# Visual placement and persistent Cloud Anchors

## What was implemented

After taking a photo and entering the message, open **Place → Scan and place in AR**.
The photo and text remain in the composer while a separate ARCore camera scans the
environment. Tap a detected plane, oriented feature point, or depth surface between
0.2 and 8 metres away. A green marker follows the chosen physical spot. Hosting is
enabled only after ARCore reports GOOD visual-map quality, with the selected spot
visible and tracked continuously for at least 750 ms. Tap **Use this spot**, wait for
hosting, then publish normally. The Cloud Anchor is hosted during the scan, before
publication, so a failed upload can be retried without repeating a successful scan.

The saved Drop contains an optional `spatial_anchor` JSON object:

```json
{
  "cloudAnchorId": "ARCore-issued ID",
  "expiresAtMillis": 1800000000000,
  "geospatialPose": {
    "latitude": 28.6,
    "longitude": 77.2,
    "altitudeMeters": 210.0,
    "qx": 0.0, "qy": 0.0, "qz": 0.0, "qw": 1.0
  }
}
```

`geospatialPose` is optional: it is recorded only when Earth is tracking with
horizontal accuracy ≤10 m, vertical accuracy ≤5 m, and yaw accuracy ≤15°. Its WGS84
position and east-up-south orientation describe the tapped surface, not the phone.
Otherwise the geographic coordinate remains a coarse search location; the hosted
visual map stores the precise local placement. Its coarse accuracy includes the
distance from the scan's starting camera position to the selected anchor. The scan
can accept a fused network/GPS fix up to 500 m accuracy; ordinary GPS-only creation
still defaults to the existing 100 m collection limit. A rough location is necessary
for the app's geographic nearby search. Cloud matching does not require precise GPS.

Discovery resolves nearby Cloud IDs using the local camera even if Earth/VPS is not
available, including indoors. A resolved Cloud Anchor takes precedence over a
geographic fallback. Visual-only indoor anchors never receive an invented outdoor
terrain height. A recognized anchor within the existing 30 m unlock radius can
unlock the Drop using physical AR distance rather than noisy GPS distance. The
camera overlay also uses AR distance for visual placements.

Legacy Drops continue using terrain anchors outdoors and the compass fallback.
Changing a composer map pin explicitly clears the hosted placement; otherwise the
map and saved visual location could disagree. Hosted drafts survive saved-state
restoration. Relocating a Hitchhiker clears the previous location's anchor metadata.
Cloud failures, expiry, timeout, backgrounding and camera release are handled without
publishing a fake successful visual placement.

## Required deployment setup — not performed by the local build

1. Apply `supabase/migrations/202610290001_add_spatial_anchors.sql` to the same Supabase
   project used by the app. It adds a nullable JSONB column and preserves existing
   Drops, RLS policies and RPC signatures. Existing `nearby_drops` RPCs remain
   compatible because the repository hydrates placement metadata directly from the
   `drops` table. Do not publish the new app before applying the migration.
2. Enable the **ARCore API** in your Google Cloud project. Configure **keyless
   Android authorization**: create an OAuth 2.0 Android client with package
   `com.somewhere.app` and the SHA-1 of the certificate signing the installed build.
   Obtain local fingerprints with `./gradlew signingReport`. Register debug and
   production/Play App Signing certificates separately where appropriate.
3. Keep Maps API configuration separate. The manifest retains the Maps key but no
   longer passes it as `com.google.android.ar.API_KEY`. Cloud/Geospatial sessions use
   package/certificate authorization, and the build includes the required Play
   Services auth dependency and release keep rules. No OAuth client secret belongs
   in this app, and no new secret needs to be pasted into `local.properties`.
4. The configured Cloud Anchor TTL is **365 days**, not permanent. API-key-only
   authorization permits at most 24 hours; the longer TTL requires keyless
   authorization. Expired anchors are not resolved. Automatic renewal or editing a
   published Drop to rehost it is not implemented; recreate/rehost content before
   expiry if it must last longer. Discarded hosted drafts expire naturally; Android
   does not delete their server-side visual maps when a local anchor is detached.
5. Use compatible Android phones with current Google Play Services for AR, camera,
   precise location permissions, and internet. The app displays Google's sensor-data
   disclosure before activating Cloud/Geospatial APIs, with a Learn more link.

Neither live Google Cloud authorization nor the live Supabase migration has been
verified or applied by these local code changes.

## Physical-device acceptance test

1. Install the same authorized build on two ARCore-supported phones using different
   accounts. First test the composer camera, photo capture, and scan handoff; no black
   preview or CameraX/ARCore camera-in-use error should occur.
2. Place an indoor Drop on a stationary, textured object. Move around it until hosting
   becomes available; host and publish. Verify the row's Cloud ID/expiry and that
   geographic coordinates are present. A successful hosting response is required;
   an authorization failure must leave the draft intact and must not report success.
3. Force-close the creator app and reopen discovery. On the second phone, approach
   the same area and scan the same surfaces from multiple angles. Verify that the
   Drop appears at the selected surface and floor without waiting for Earth/VPS.
   Measure and record physical offset; no centimetre-accuracy guarantee is assumed.
4. Repeat from different directions, on different days and in different lighting.
   Change/move the scanned object to check honest recognition failure instead of a
   jump to a ground-level GPS marker. Cloud Anchors depend on recognizable scenery.
5. Host outdoors where VPS is available. Verify the stored surface pose has altitude
   and quaternion, and that Cloud resolution supersedes geographic placement.
6. Disconnect internet during hosting/resolving. Check the 60 s hosting timeout,
   discovery retry/compass fallback, useful errors and intact draft. Disable API
   authorization temporarily only in a test project to check the authorization error.
7. Cancel a scan, change the selected surface, retake the photo, edit the map pin,
   rotate/recreate the activity after hosting, and background/foreground repeatedly.
   Confirm no old Cloud ID is attached to a different location or reused photo.
8. Test an expired Cloud ID and a legacy GPS-only Drop. The former must not resolve
   or prevent nearby search; the latter should retain the original discovery path.
9. Upgrade an installed version-9 database with existing Drops to version 10. Verify
   the nullable Room field is added without deleting existing rows.

## Verification scope

Local JVM tests cover metadata serialization, Room converters, legacy JSON,
expiry/invalid values, visual-vs-terrain routing, quality gating, request replacement,
cancellation, timeout and retry behavior. They cannot run ARCore visual localization.
Run `./gradlew testDebugUnitTest lintDebug assembleDebug` for local checks, then run
the physical acceptance test above. No Android device was attached during development.

## Primary references

- [Cloud Anchors developer guide](https://developers.google.com/ar/develop/java/cloud-anchors/developer-guide)
- [ARCore Android authorization](https://developers.google.com/ar/develop/authorization?platform=android)
- [Feature-map quality API](https://developers.google.com/ar/reference/java/com/google/ar/core/Session#estimateFeatureMapQualityForHosting-com.google.ar.core.Pose-)
- [Geospatial session setup](https://developers.google.com/ar/develop/java/geospatial/enable)
- [ARCore privacy requirements](https://developers.google.com/ar/develop/privacy-requirements)
