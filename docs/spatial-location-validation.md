# Spatial placement: implementation and device verification

## Current behavior

- Nearby search starts with Android fused location. Once ARCore localizes, it uses
  the Geospatial camera position, refreshing position at most once per second.
- Localization requires horizontal accuracy at or below 10 metres and yaw accuracy
  at or below 15 degrees for one second. After localization, exit thresholds widen
  to 15 metres and 20 degrees to avoid flicker. Tracking loss hides AR labels immediately.
- Legacy terrain anchors use each Drop's saved latitude/longitude and a label height of
  1.35 metres above terrain. Coordinates are never compressed into a short camera-relative range.
- Each camera scene owns its anchor requests. Four can resolve concurrently. Pending
  requests time out after 20 seconds. Transient failures retry after 2 seconds, then
  exponentially up to 30 seconds. Unsupported locations are not retried in that scene.
- Leaving the camera or pausing clears requests and detaches anchors. Matrices,
  world positions, and viewport dimensions used for projection come from the same frame.
- Cloud Anchors resolve independently of Earth localization. Discovery falls back to
  the compass camera after 15 seconds without local camera tracking or 60 seconds
  with no resolved markers. Geospatial service errors do not disable indoor Cloud matching.
- A map pin is stored separately from GPS readings. A chosen pin survives sensor
  updates, back/next navigation, and saved-state restoration. Its provider is
  `manual-map`, its timestamp is the selection time, and its measured accuracy is
  null: the original GPS radius does not measure a different point selected on a map.

## Setup

Use an ARCore-supported device with precise location and camera access. Enable the
ARCore API for the Google Cloud project authorizing the app. ARCore now uses keyless
package/signing-certificate authorization; the Maps key is used only for Maps. See
[Cloud Anchor setup](cloud-anchor-setup.md) for the required Google Cloud authorization
and Supabase migration. A working internet connection is required for Cloud/Terrain
anchor operations. These settings have
not been verified against the live Google Cloud project by local unit tests.

## Physical-device checks

1. Place a Drop at a fixed outdoor landmark, then close/reopen discovery and revisit
   it from several directions. Repeat on a second compatible phone. Record horizontal
   offset from the landmark, localization time, and resolution failures.
2. Inspect Drops about 10, 50, and 200 metres away. Their bearings should agree with
   the real landmarks; the farther points must not behave as if they are 10 metres away.
3. Cover the camera or move into poor lighting. Labels should disappear on tracking
   loss, the compass should resume, and AR should return only after localization stabilizes.
4. Alternate camera/list view and background/foreground repeatedly. Check that the
   camera reopens and that markers or pending requests do not carry over from a closed view.
5. Disable connectivity or test outside VPS coverage. Verify the timed compass fallback
   and that standing still still refreshes nearby content.
6. Select a map pin, move back a creation step, then return. Recreate the activity and
   verify the selection is retained. Publish and verify the saved coordinate, source,
   selection timestamp, and null manual-pin accuracy.

## Visual placement and remaining accuracy work

The composer now supports surface-tap visual placement and Cloud Anchor hosting.
Discovery recognizes those visual maps in later sessions and on other devices,
without requiring Earth tracking. When Earth quality allows it, authoring also saves
the tapped surface's geographic pose. See the setup guide's physical acceptance test;
these paths still require authorized devices and real-environment validation.

GPS-only Drops still use a geographic coordinate: resolving it as a terrain anchor
does not correct an incorrect authoring coordinate or encode an indoor floor. Visual
placement must be used for repeatable wall/floor-specific placement. Cloud Anchors
expire and depend on stable, recognizable surroundings; automatic renewal is not implemented.

Cards are still screen-space billboards. Anchor correctness does not itself add
3D geometry, physical-size rendering, or depth occlusion. No numerical real-world
accuracy guarantee has been validated by these local tests.
