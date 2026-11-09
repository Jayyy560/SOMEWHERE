// Isolated Postgres/PostGIS integration checks. See docs/wayfinder.md for runtime setup.
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
const require = createRequire(path.join(process.argv[2] || '/tmp/wayfinder-sql-test', 'package.json'));
const { PGlite } = require('@electric-sql/pglite');
const { postgis } = require('@electric-sql/pglite-postgis');
const { pg_trgm } = require('@electric-sql/pglite/contrib/pg_trgm');
const db = new PGlite({ extensions: { postgis, pg_trgm } });
try {
  await db.exec(`
    create schema extensions; create schema auth; create role authenticated nologin; create role anon nologin;
    alter default privileges in schema public grant execute on functions to anon;
    alter default privileges in schema public grant all on tables to anon, authenticated;
    create table auth.users(id uuid primary key);
    create function auth.uid() returns uuid language sql stable as $$ select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid $$;
    create table public.drops (
      id uuid primary key default gen_random_uuid(), text text not null, category text,
      image_url text not null default '', audio_url text, latitude double precision, longitude double precision,
      user_id uuid references auth.users(id), author_name text, author_avatar_url text,
      is_anonymous boolean not null default false, is_public boolean not null default true,
      created_at timestamptz not null default now(), expires_at timestamptz, carried_by_user_id uuid
    );
    alter table drops enable row level security;
    create policy visible_drops on drops for select to authenticated using(is_public or user_id=auth.uid());
    create policy own_drop_access on drops for update to authenticated using(user_id=auth.uid()) with check(user_id=auth.uid());
    grant usage on schema public,auth,extensions to authenticated;
    grant select,update on drops to authenticated;
    insert into auth.users values ('00000000-0000-0000-0000-000000000001'),('00000000-0000-0000-0000-000000000002');
  `);
  const migration = await readFile(new URL('../supabase/migrations/202611050001_wayfinder_planner.sql',import.meta.url),'utf8');
  await db.exec(migration);
  await db.exec(migration); // safe to reapply
  const access = (await db.query(`select
    has_function_privilege('anon', 'public.wayfinder_stops(text[])', 'execute') as anon_rpc,
    has_table_privilege('anon', 'public.wayfinder_trails', 'select') as anon_table,
    has_table_privilege('authenticated', 'public.wayfinder_trails', 'update') as authenticated_update
  `)).rows[0];
  assert.deepEqual(access, { anon_rpc:false, anon_table:false, authenticated_update:false });
  await db.exec(`
    insert into drops(text,category,latitude,longitude,user_id,author_name)
    select 'Food nearby '||g,'Food',0,g*0.00001,'00000000-0000-0000-0000-000000000002','Other' from generate_series(1,150)g;
    insert into drops(id,text,category,latitude,longitude,user_id,author_name,step_free_access) values
      ('10000000-0000-0000-0000-000000000001','A mural #art','Photography',0,0.01,'00000000-0000-0000-0000-000000000001','Me',true),
      ('10000000-0000-0000-0000-000000000002','Along the route','Photography',0.001,0.03,'00000000-0000-0000-0000-000000000002','Other',null),
      ('10000000-0000-0000-0000-000000000003','Outside corridor','Photography',0.02,0.02,'00000000-0000-0000-0000-000000000002','Other',false),
      ('10000000-0000-0000-0000-000000000004','Secret private memory','Photography',0,0.002,'00000000-0000-0000-0000-000000000002','Other',true);
    update drops set is_public=false where id='10000000-0000-0000-0000-000000000004';
    set request.jwt.claim.sub='00000000-0000-0000-0000-000000000001'; set role authenticated;
  `);
  let count = 1;
  const search = async (filter={}, route=[], radius=2000, corridor=200, offset=0) => {
    const result = await db.query('select * from wayfinder_drops($1,$2,$3,$4::jsonb,$5,$6::jsonb,100,$7)',[0,0,radius,JSON.stringify(route),corridor,JSON.stringify(filter),offset]);
    return result.rows.map(row=>row.wayfinder_drops);
  };
  const photography = {groups:[['photography']]};
  let rows = await search(photography);
  assert.deepEqual(rows.map(row=>row.id),['10000000-0000-0000-0000-000000000001']); count++;
  rows = await search(photography,[[0,0],[0.04,0]]);
  assert.deepEqual(new Set(rows.map(row=>row.id)),new Set(['10000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002'])); count++;
  assert.equal((await search({...photography,radius_required:true},[[0,0],[0.04,0]],1000)).length,0); count++;
  assert.equal((await search({...photography,step_free:true},[[0,0],[0.04,0]])).length,1); count++;
  assert.equal((await search({...photography,exclusions:['mural']})).length,0); count++;
  assert.equal((await search({...photography,blocked_authors:['Me']})).length,0); count++;
  assert.equal((await search({groups:[['food']]})).length,100);
  assert.equal((await search({groups:[['food']]},[],2000,200,100)).length,50); count++;
  assert.equal((await search({tags:['art']})).length,1);
  assert.equal((await search({tags:['artist']})).length,0); count++;
  assert.equal((await search({...photography,media:['PHOTOS']})).length,0); count++;
  const ids=['10000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000001'];
  const published=await db.query('select publish_wayfinder_trail($1,$2,$3::text[],$4,$5) as id',['Photo walk','Two ordered stops',ids,'walking',false]);
  const trail=(await db.query('select * from wayfinder_trails where id=$1',[published.rows[0].id])).rows[0];
  assert.deepEqual(trail.stop_ids,ids); assert.equal(trail.start_lng,0.03); assert.equal(trail.start_lat,0.001); count++;
  await assert.rejects(db.query('select publish_wayfinder_trail($1,$2,$3::text[],$4,$5)',['Private','',['10000000-0000-0000-0000-000000000004'],'walking',false])); count++;
  await db.query('select set_wayfinder_stop_access($1,$2)',['10000000-0000-0000-0000-000000000001',false]);
  assert.equal((await search({...photography,step_free:true})).length,0); count++;
  await db.query('select set_wayfinder_stop_access($1,$2)',['10000000-0000-0000-0000-000000000002',true]);
  assert.equal((await db.query('select step_free_access from drops where id=$1',[ids[0]])).rows[0].step_free_access,null); count++;
  console.log(`${count} Postgres/PostGIS integration scenarios passed; migration reapplied successfully.`);
} catch (error) { console.error(error.message); if (error.internalQuery) console.error(error.internalQuery); process.exitCode=1; } finally { await db.close(); }
