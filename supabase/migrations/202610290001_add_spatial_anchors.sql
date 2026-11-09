-- Existing GPS-only Drops remain valid. The app also hydrates metadata directly
-- from drops when older nearby_drops RPCs do not yet project this field.
alter table public.drops add column if not exists spatial_anchor jsonb;

comment on column public.drops.spatial_anchor is
    'Hosted Cloud Anchor ID, expiry in epoch milliseconds, and optional WGS84 surface pose.';

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'drops_spatial_anchor_object'
          and conrelid = 'public.drops'::regclass
    ) then
        alter table public.drops add constraint drops_spatial_anchor_object
            check (spatial_anchor is null or jsonb_typeof(spatial_anchor) = 'object');
    end if;
end $$;
