# Wayfinder

Wayfinder uses an explicit prompt grammar, spatial SQL, lexical search, and a bounded route-insertion heuristic. It makes no model or embedding requests.

## Setup

1. Apply `supabase/migrations/202611050001_wayfinder_planner.sql` to the project's Supabase database, after the existing Drop migrations. The migration adds the search RPC, spatial/text indexes, optional stop-access metadata, and ordered community trails. It is safe to reapply. Existing Drop row-level security continues to govern discovery and trail publishing.
2. Enable Google Routes API for the project associated with `GOOGLE_MAPS_API_KEY`. Existing projects with Directions API enabled can use the compatibility fallback when Routes returns HTTP 403/404. Travel modes are preserved in both providers. Place autocomplete continues to use the existing Places service.
3. Build the Android app with JDK 17 or the configured Android Studio JBR: `./gradlew testDebugUnitTest assembleDebug`.

The migration is supplied in the repository; applying it to a live database requires that database's migration credentials. No live database changes are made by the local integration tests.

## Using it

Walking is the default. Choose Walk, Cycle, or Drive beside search. Trip options control exploration radius, total time, extra route travel, stop count, time at stops, return trips, unvisited filtering, category, media, and accessibility preferences. An explicit value in the request overrides the matching option.

Examples:

- `photography nearby`
- `photos history by @sam within 2km without "street art"`
- `Find three photography Drops on a 30-minute walk and bring me back here`
- `music to India Gate with 5 minutes detour driving`
- `hidden gems near Connaught Place`
- `from here to "Walk of Fame" cycling`
- `from Delhi to Jaipur via Agra and Bharatpur`
- `surprise me within 1km without food`

Quote place names containing command words. Multiple explicit waypoints use `and` or `;`. `then` connects destination stages; discovery followed by a destination chooses a matching first Drop and continues to the endpoint. This is a deliberately bounded grammar, not unrestricted natural-language interpretation.

Nearby searches show ranked results without requesting a route for every Drop. Press Start trip to route to the selected result. Explicit stop counts, time budgets, return trips, and Surprise me build an itinerary immediately. On an existing destination route, discovery candidates are checked using provider-calculated extra travel time. Stops can be added or removed in **Stops** before navigation starts.

**Save** stores a private bookmark. **Saved & trails** contains bookmarks, ended trips and notes, and nearby community trails. A trip and each stop can have a private note. Journals can also hold up to 12 private photos selected through Android's photo picker; files are copied into account-scoped app storage and removed when the journal or photo is deleted. No extra camera or storage permission is required. A navigation session left open when the process ends is available for resuming; routes are recalculated on opening, using the current location. A loop retains its original return point. Ended trips can be opened as new journeys.

Publishing is an explicit action under **More → Publish trail**. It shares the chosen ordered Drop IDs, title, description, and travel mode. The public trail's starting coordinate is derived on the server from its first visible Drop. Private trip origins, visit history, and journals are never included in that payload.

List navigation has the same trip actions without requiring the map or camera. Proximity alerts support haptics and screen-reader announcements. The **Step-free stops** filter includes only author-confirmed stop metadata, excluding unknown access. Authors can update this metadata under **Stops**. It does not certify wheelchair access, step-free connections, or the accessibility of the routing provider's path.

## Algorithm and limits

- Mandatory constraints (visibility, expiry, carried state, category, media, exclusions, visit status, access metadata, and any explicit radius) precede ordering and pagination in SQL. The client repeats matching before ranking.
- Text search uses complete words, a curated synonym dictionary, and at most one edit/transposition for longer words. Combined terms and media/author/tag filters are conjunctive. Hashtags and excluded phrases match complete tokens.
- PostGIS `ST_DWithin` queries the actual route LineString using a GiST expression index. Route corridors are broad-phase candidates; provider route checks account for barriers and travel mode.
- `TripSearch.Weights` exposes the manually chosen relevance, detour, novelty, and freshness weights. Recent preference increases the freshness weight. No learned weights are involved.
- At most 1,000 eligible candidates are fetched in pages of 100. Destination-route discovery checks the best 24 candidates. Itinerary construction evaluates batches of six candidates at each insertion (trying the next batch if none fits), with at most three simultaneous requests and a cap of 40 candidate route evaluations and eight stops. The planner may return fewer stops when none of the remaining checked candidates fits; it does not claim global optimality or exhaustive reachability.
- Total time includes provider travel time and configured dwell time. Extra-travel limits exclude dwell time. Candidate selection can use geometry to choose an insertion position, but acceptance always uses network travel time. A checked reversal may shorten an unordered itinerary; published curated order is preserved.
- Coordinates, resolved endpoints, ordered stop IDs, filters, and the return point survive rerouting. Reroutes validate discoveries again and warn when elapsed time plus the new remaining itinerary exceeds the original budget.
- Navigation retains reported accuracy and monotonic timestamps. Stale fixes and isolated jumps do not cause reroutes. Reliable deviation must persist for at least eight seconds and three samples, with a reroute cooldown. Progress is bounded to avoid jumping to the return segment at a loop crossing.
- The foreground service and screen share one location subscription. Tracking continues during navigation and stops when neither navigation nor the Wayfinder screen needs it. Proximity notification cooldowns, dismissals, and visit history are shared by foreground and geofence paths. Up to 90 upcoming geofences leave room below Android's app limit; foreground tracking supplies alerts if background geofencing permission is unavailable.
- A visit is recorded after eight seconds within 30 m with reported accuracy at most 25 m, or when the user marks it manually. Visit history is a journal feature and does not unlock Drop content.
- Bookmarks, journals, and visit history are account-scoped local data. Only user-authored itinerary data is persisted; map-provider route responses are kept in memory.

## Verification

Android tests:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug --offline
```

Isolated PostgreSQL/PostGIS checks (no live service required):

```sh
npm install --prefix /tmp/wayfinder-sql-test --ignore-scripts @electric-sql/pglite @electric-sql/pglite-postgis
node scripts/wayfinder-sql-test.mjs /tmp/wayfinder-sql-test
```

The SQL checks execute the migration twice against real PostgreSQL/PostGIS in WASM and verify filtering before pagination, exact corridors, explicit radius constraints, access metadata, media, exclusions, tags, ordered publishing, and RLS. Routing unit tests use controlled network costs to test a nearby-but-unreachable candidate, dwell-inclusive budgets, insertion limits, curated order, and cancellation. Navigation tests replay stale, noisy, off-route, driving, loop, and dateline cases. Provider responses are tested for multi-leg totals and malformed geometry. Field performance, actual map-service authorization, and device GPS behavior still need testing on a physical phone after deployment.
