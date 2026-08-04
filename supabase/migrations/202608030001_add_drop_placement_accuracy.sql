-- Persist the uncertainty of the location fix used to place each Drop.
-- Existing rows remain valid and fall back to the viewer-only uncertainty model.
alter table public.drops
    add column if not exists placement_accuracy_m real,
    add column if not exists placement_location_timestamp timestamptz,
    add column if not exists placement_location_provider text;

comment on column public.drops.placement_accuracy_m is
    'Android-reported 68% confidence radius, in metres, for the placement fix.';
comment on column public.drops.placement_location_timestamp is
    'Timestamp of the source location sample used to place the Drop.';
comment on column public.drops.placement_location_provider is
    'Android location provider associated with the selected placement sample.';

-- IMPORTANT: if nearby_drops returns an explicit TABLE column list rather than
-- SETOF drops, add these three fields to that function's return projection too.
