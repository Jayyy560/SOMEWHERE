package com.somewhere.app.util

import org.junit.Assert.*
import org.junit.Test

class AnchorRequestStoreTest {
    private class FakeAnchor {
        var detached = false
        var stopped = false
    }

    private class FakeRequest : AnchorRequest<FakeAnchor> {
        var result: AnchorRequestResult<FakeAnchor> = AnchorRequestResult.Pending
        var cancelled = false
        var completeOnCancel: FakeAnchor? = null
        override fun poll() = result
        override fun cancel() {
            cancelled = true
            completeOnCancel?.let { result = AnchorRequestResult.Resolved(it) }
        }
    }

    private class Fixture {
        var now = 0L
        val requests = mutableListOf<Pair<AnchorTarget, FakeRequest>>()
        val store = AnchorRequestStore<FakeAnchor>(
            nowMillis = { now },
            detach = { it.detached = true },
            isStopped = { it.stopped }
        )
        fun update(vararg targets: AnchorTarget) = store.update(targets.toList()) { target ->
            FakeRequest().also { requests += target to it }
        }
    }

    private fun target(id: String = "drop", latitude: Double = 28.6) = AnchorTarget(id, latitude, 77.2)

    @Test fun failedRequestBacksOffInsteadOfRetryingEveryFrame() {
        val f = Fixture()
        f.update(target())
        f.requests[0].second.result = AnchorRequestResult.Failed(retryable = true)
        f.update(target())
        repeat(120) { f.now += 16; f.update(target()) }
        assertEquals(1, f.requests.size)
        f.now = 2_000L
        f.update(target())
        assertEquals(2, f.requests.size)
    }

    @Test fun requestConcurrencyIsBounded() {
        val f = Fixture()
        val targets = (1..15).map { target(it.toString()) }
        f.store.update(targets) { t -> FakeRequest().also { f.requests += t to it } }
        assertEquals(4, f.requests.size)
        f.requests[0].second.result = AnchorRequestResult.Resolved(FakeAnchor())
        f.store.update(targets) { t -> FakeRequest().also { f.requests += t to it } }
        assertEquals(5, f.requests.size)
    }

    @Test fun changingCoordinatesForSameDropCancelsOldRequest() {
        val f = Fixture()
        f.update(target())
        val old = f.requests[0].second
        f.update(target(latitude = 28.7))
        assertTrue(old.cancelled)
        assertEquals(2, f.requests.size)
        old.result = AnchorRequestResult.Resolved(FakeAnchor())
        val actual = FakeAnchor()
        f.requests[1].second.result = AnchorRequestResult.Resolved(actual)
        f.update(target(latitude = 28.7))
        assertSame(actual, f.store.anchors()["drop"])
    }

    @Test fun removingLastDropDetachesAnchorAndCancelsOtherRequests() {
        val f = Fixture()
        f.update(target("one"), target("two"))
        val anchor = FakeAnchor()
        f.requests[0].second.result = AnchorRequestResult.Resolved(anchor)
        f.update(target("one"), target("two"))
        f.update()
        assertTrue(anchor.detached)
        assertTrue(f.requests[1].second.cancelled)
        assertTrue(f.store.anchors().isEmpty())
    }

    @Test fun movingResolvedDropDetachesItsPreviousPosition() {
        val f = Fixture()
        f.update(target())
        val anchor = FakeAnchor()
        f.requests[0].second.result = AnchorRequestResult.Resolved(anchor)
        f.update(target())
        f.update(target(latitude = 28.7))
        assertTrue(anchor.detached)
        assertTrue(f.store.anchors().isEmpty())
        assertEquals(2, f.requests.size)
    }

    @Test fun stoppedAnchorIsDetachedAndRecreatedAfterBackoff() {
        val f = Fixture()
        f.update(target())
        val anchor = FakeAnchor()
        f.requests[0].second.result = AnchorRequestResult.Resolved(anchor)
        f.update(target())
        anchor.stopped = true
        f.update(target())
        assertTrue(anchor.detached)
        assertTrue(f.store.anchors().isEmpty())
        assertEquals(1, f.requests.size)
        f.now = 2_000L
        f.update(target())
        assertEquals(2, f.requests.size)
    }

    @Test fun clearingScenePreventsOldResultsEnteringNewScene() {
        val f = Fixture()
        f.update(target())
        val old = f.requests[0].second
        f.store.clear()
        f.update(target())
        old.result = AnchorRequestResult.Resolved(FakeAnchor())
        f.update(target())
        assertTrue(old.cancelled)
        assertTrue(f.store.anchors().isEmpty())
        assertEquals(2, f.requests.size)
    }

    @Test fun completionRacingCancellationIsDetached() {
        val f = Fixture()
        f.update(target())
        val anchor = FakeAnchor()
        f.requests[0].second.completeOnCancel = anchor
        f.store.clear()
        assertTrue(anchor.detached)
    }

    @Test fun stalledRequestsAreCancelledAndRetryLater() {
        val f = Fixture()
        f.update(target())
        f.now = 20_000L
        f.update(target())
        assertTrue(f.requests[0].second.cancelled)
        assertEquals(1, f.requests.size)
        f.now = 22_000L
        f.update(target())
        assertEquals(2, f.requests.size)
    }

    @Test fun unsupportedLocationIsNotRequestedRepeatedly() {
        val f = Fixture()
        f.update(target())
        f.requests[0].second.result = AnchorRequestResult.Failed(retryable = false)
        f.update(target())
        f.now = 1_000_000L
        f.update(target())
        assertEquals(1, f.requests.size)
    }

    @Test fun invalidAndDuplicateTargetsDoNotConsumeRequests() {
        val f = Fixture()
        f.update(target(), target(), target("bad", Double.NaN), target("pole", 90.0))
        assertEquals(1, f.requests.size)
    }

    @Test fun replacingCloudIdAtSameCoordinatesCancelsOldResolution() {
        val f = Fixture()
        val first = target().copy(spatialAnchor = com.somewhere.app.data.model.SpatialAnchor("first", 100_000L))
        val second = first.copy(spatialAnchor = first.spatialAnchor!!.copy(cloudAnchorId = "second"))
        f.update(first)
        val oldRequest = f.requests[0].second
        f.update(second)
        assertTrue(oldRequest.cancelled)
        assertEquals(2, f.requests.size)
        assertEquals("second", f.requests.last().first.spatialAnchor?.cloudAnchorId)
    }

    @Test fun deletingVisualMetadataDetachesResolvedCloudAnchor() {
        val f = Fixture()
        val visual = target().copy(spatialAnchor = com.somewhere.app.data.model.SpatialAnchor("id", 100_000L))
        f.update(visual)
        val anchor = FakeAnchor()
        f.requests[0].second.result = AnchorRequestResult.Resolved(anchor)
        f.update(visual)
        f.update(visual.copy(spatialAnchor = null))
        assertTrue(anchor.detached)
        assertTrue(f.store.anchors().isEmpty())
    }
}
