-- Deterministic discovery, route corridors, and community-authored ordered trails.
-- Run after the existing Drop migrations. All reads retain the caller's Drop RLS policies.
begin;
create extension if not exists postgis with schema extensions;
create extension if not exists pg_trgm with schema extensions;
set local search_path = public, extensions;

alter table public.drops add column if not exists step_free_access boolean;
comment on column public.drops.step_free_access is
    'Optional author-confirmed access at the stop. NULL is unknown; does not certify the route to it.';

create index if not exists drops_wayfinder_location_idx on public.drops
    using gist ((st_setsrid(st_makepoint(longitude, latitude), 4326)::geography));
create index if not exists drops_wayfinder_words_idx on public.drops
    using gin (to_tsvector('simple', coalesce(text, '') || ' ' || coalesce(category, '')));
create index if not exists drops_wayfinder_trigram_idx on public.drops
    using gin ((lower(coalesce(text, '') || ' ' || coalesce(category, ''))) gin_trgm_ops);

-- Broader trigram candidates are rechecked with exact tokens/one-edit matching on the device.
-- Spatial + mandatory filters run before ordering, LIMIT, and OFFSET.
create or replace function public.wayfinder_drops(
    p_lat double precision, p_lon double precision, p_radius_m double precision,
    p_route jsonb default '[]', p_corridor_m double precision default 600,
    p_filter jsonb default '{}', p_limit integer default 100, p_offset integer default 0
) returns setof jsonb language plpgsql stable security invoker
set search_path = public, extensions, pg_temp as $$
declare
    center geography;
    route_line geography;
    terms tsquery;
begin
    if p_lat not between -90 and 90 or p_lon not between -180 and 180
       or p_radius_m not between 1 and 100000 or p_corridor_m not between 1 and 10000
       or jsonb_typeof(p_route) <> 'array' or jsonb_array_length(p_route) > 50000 then
        raise exception 'Invalid spatial search';
    end if;
    center := st_setsrid(st_makepoint(p_lon, p_lat), 4326)::geography;
    if jsonb_array_length(p_route) >= 2 then
        route_line := st_setsrid(st_geomfromgeojson(jsonb_build_object('type','LineString','coordinates',p_route)::text),4326)::geography;
    end if;
    select websearch_to_tsquery('simple', string_agg(words.value, ' OR ')) into terms
    from jsonb_array_elements(coalesce(p_filter->'groups','[]')) g,
         jsonb_array_elements_text(g) as words(value);
    return query
    with eligible as (
        select d.*, lower(coalesce(d.text,'') || ' ' || coalesce(d.category,'')) as doc,
            st_setsrid(st_makepoint(d.longitude,d.latitude),4326)::geography as point
        from public.drops d
        where (d.expires_at is null or d.expires_at > now())
          and d.carried_by_user_id is null
          and ((route_line is null and st_dwithin(st_setsrid(st_makepoint(d.longitude,d.latitude),4326)::geography, center, p_radius_m))
            or (route_line is not null and st_dwithin(st_setsrid(st_makepoint(d.longitude,d.latitude),4326)::geography, route_line, p_corridor_m)))
          and (not coalesce((p_filter->>'radius_required')::boolean,false)
            or st_dwithin(st_setsrid(st_makepoint(d.longitude,d.latitude),4326)::geography,center,p_radius_m))
          and (not coalesce((p_filter->>'step_free')::boolean,false) or d.step_free_access is true)
          and (nullif(p_filter->>'category','') is null or lower(d.category)=lower(p_filter->>'category'))
          and (nullif(p_filter->>'author','') is null or lower(d.author_name)=lower(p_filter->>'author'))
          and (not coalesce((p_filter->>'mine')::boolean,false) or d.user_id=auth.uid())
          and (not coalesce((p_filter->>'anonymous')::boolean,false) or d.is_anonymous)
          and not exists (select 1 from jsonb_array_elements_text(coalesce(p_filter->'excluded_ids','[]')) e where e.value=d.id::text)
          and not exists (select 1 from jsonb_array_elements_text(coalesce(p_filter->'blocked_authors','[]')) b where b.value=d.author_name or b.value=d.user_id::text)
          and not exists (
              select 1 from jsonb_array_elements_text(coalesce(p_filter->'media','[]')) m
              where (m.value='PHOTOS' and coalesce(d.image_url,'')='')
                 or (m.value='AUDIO' and coalesce(d.audio_url,'')='')
                 or (m.value='TEXT' and (coalesce(d.image_url,'')<>'' or coalesce(d.audio_url,'')<>''))
          )
    ), matched as (
        select e.* from eligible e
        where not exists (
            select 1 from jsonb_array_elements_text(coalesce(p_filter->'exclusions','[]')) x
            where to_tsvector('simple',e.doc) @@ phraseto_tsquery('simple',x.value)
        )
        and not exists (
            select 1 from jsonb_array_elements(coalesce(p_filter->'groups','[]')) g
            where not exists (
                select 1 from jsonb_array_elements_text(g) term
                where to_tsvector('simple',e.doc) @@ plainto_tsquery('simple',term.value)
                   or (length(term.value)>=4 and word_similarity(lower(term.value),e.doc)>=0.40)
            )
        )
        and not exists (
            select 1 from jsonb_array_elements_text(coalesce(p_filter->'tags','[]')) tag
            where not exists (
                select 1 from regexp_matches(lower(e.text),'#([[:alnum:]_]+)','g') hit
                where hit[1]=lower(tag.value)
            )
        )
    )
    select (to_jsonb(m)-'doc'-'point') || jsonb_build_object('distance_m',st_distance(m.point,center))
    from matched m
    order by case when coalesce((p_filter->>'recent')::boolean,false) then extract(epoch from m.created_at) else 0 end desc nulls last,
             ts_rank_cd(to_tsvector('simple',m.doc),coalesce(terms,''::tsquery)) desc,
             st_distance(m.point,coalesce(route_line,center)),m.id
    limit greatest(1,least(p_limit,100)) offset greatest(0,least(p_offset,10000));
end $$;

create or replace function public.wayfinder_stops(p_ids text[])
returns setof jsonb language sql stable security invoker
set search_path = public, extensions, pg_temp as $$
    select to_jsonb(d) from public.drops d
    where d.id::text=any(p_ids) and (d.expires_at is null or d.expires_at>now()) and d.carried_by_user_id is null
    order by array_position(p_ids,d.id::text)
    limit 24;
$$;

create table if not exists public.wayfinder_trails (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null check (char_length(title) between 1 and 80),
    description text not null default '' check (char_length(description)<=500),
    stop_ids text[] not null check (cardinality(stop_ids) between 1 and 8),
    travel_mode text not null check (travel_mode in ('walking','bicycling','driving')),
    return_to_start boolean not null default false,
    start_lat double precision not null check (start_lat between -90 and 90),
    start_lng double precision not null check (start_lng between -180 and 180),
    created_at timestamptz not null default now()
);
alter table public.wayfinder_trails enable row level security;
drop policy if exists wayfinder_trails_read on public.wayfinder_trails;
create policy wayfinder_trails_read on public.wayfinder_trails for select to authenticated using (true);
-- All access uses caller RLS, including publishing. The creator may remove their own trail.
drop policy if exists wayfinder_trails_insert on public.wayfinder_trails;
create policy wayfinder_trails_insert on public.wayfinder_trails for insert to authenticated with check (auth.uid()=user_id);
drop policy if exists wayfinder_trails_delete on public.wayfinder_trails;
create policy wayfinder_trails_delete on public.wayfinder_trails for delete to authenticated using (auth.uid()=user_id);
create index if not exists wayfinder_trails_location_idx on public.wayfinder_trails
    using gist ((st_setsrid(st_makepoint(start_lng,start_lat),4326)::geography));

create or replace function public.wayfinder_trails_near(p_lat double precision,p_lon double precision,p_radius_m double precision)
returns setof public.wayfinder_trails language sql stable security invoker
set search_path=public,extensions,pg_temp as $$
    select t.* from public.wayfinder_trails t
    where st_dwithin(st_setsrid(st_makepoint(t.start_lng,t.start_lat),4326)::geography,
                     st_setsrid(st_makepoint(p_lon,p_lat),4326)::geography,least(greatest(p_radius_m,1),100000))
    order by st_distance(st_setsrid(st_makepoint(t.start_lng,t.start_lat),4326)::geography,
                         st_setsrid(st_makepoint(p_lon,p_lat),4326)::geography),t.created_at desc
    limit 50;
$$;

-- This function intentionally runs as the caller; Drop visibility policies apply to every stop.
create or replace function public.validate_wayfinder_trail()
returns trigger language plpgsql security invoker set search_path=public,pg_temp as $$
declare visible_count integer;
begin
    select count(distinct d.id) into visible_count from public.drops d
    where d.id::text=any(new.stop_ids) and (d.expires_at is null or d.expires_at>now()) and d.carried_by_user_id is null;
    if visible_count <> cardinality(new.stop_ids) then raise exception 'Some trail stops are unavailable'; end if;
    select latitude,longitude into new.start_lat,new.start_lng from public.drops where id::text=new.stop_ids[1];
    return new;
end $$;
drop trigger if exists validate_wayfinder_trail on public.wayfinder_trails;
create trigger validate_wayfinder_trail before insert on public.wayfinder_trails
    for each row execute function public.validate_wayfinder_trail();

create or replace function public.publish_wayfinder_trail(
    p_title text,p_description text,p_stop_ids text[],p_travel_mode text,p_return_to_start boolean
) returns uuid language plpgsql security invoker
set search_path=public,extensions,pg_temp as $$
declare
    owner_id uuid := auth.uid();
    first_lat double precision;
    first_lng double precision;
    valid_count integer;
begin
    if owner_id is null then raise exception 'Sign in to publish a trail'; end if;
    if cardinality(p_stop_ids) not between 1 and 8 or p_stop_ids is null
       or char_length(trim(p_title)) not between 1 and 80 or p_title is null
       or char_length(coalesce(p_description,''))>500 or p_travel_mode not in ('walking','bicycling','driving') then
        raise exception 'Invalid trail';
    end if;
    -- Require each supplied stop to be visible under caller RLS and currently available.
    select count(distinct d.id) into valid_count from public.drops d
    where d.id::text=any(p_stop_ids) and (d.expires_at is null or d.expires_at>now())
      and d.carried_by_user_id is null;
    if valid_count <> cardinality(p_stop_ids) then raise exception 'Some stops are unavailable or repeated'; end if;
    select latitude,longitude into first_lat,first_lng from public.drops where id::text=p_stop_ids[1];
    -- Start is the first public stop, never the creator's private trip origin.
    insert into public.wayfinder_trails(user_id,title,description,stop_ids,travel_mode,return_to_start,start_lat,start_lng)
    values(owner_id,trim(p_title),coalesce(p_description,''),p_stop_ids,p_travel_mode,coalesce(p_return_to_start,false),first_lat,first_lng)
    returning id into owner_id;
    return owner_id;
end $$;

create or replace function public.set_wayfinder_stop_access(p_drop_id text,p_accessible boolean)
returns void language sql security invoker set search_path=public,pg_temp as $$
    update public.drops set step_free_access=p_accessible where id::text=p_drop_id and user_id=auth.uid();
$$;

-- Older projects may grant privileges directly to API roles by default.
revoke all on function public.wayfinder_drops(double precision,double precision,double precision,jsonb,double precision,jsonb,integer,integer) from public, anon;
revoke all on function public.wayfinder_stops(text[]) from public, anon;
revoke all on function public.wayfinder_trails_near(double precision,double precision,double precision) from public, anon;
revoke all on function public.publish_wayfinder_trail(text,text,text[],text,boolean) from public, anon;
revoke all on function public.set_wayfinder_stop_access(text,boolean) from public, anon;
revoke all on table public.wayfinder_trails from public, anon, authenticated;
grant execute on function public.wayfinder_drops(double precision,double precision,double precision,jsonb,double precision,jsonb,integer,integer) to authenticated;
grant execute on function public.wayfinder_stops(text[]) to authenticated;
grant execute on function public.wayfinder_trails_near(double precision,double precision,double precision) to authenticated;
grant execute on function public.publish_wayfinder_trail(text,text,text[],text,boolean) to authenticated;
grant execute on function public.set_wayfinder_stop_access(text,boolean) to authenticated;
grant select,insert,delete on public.wayfinder_trails to authenticated;
commit;
