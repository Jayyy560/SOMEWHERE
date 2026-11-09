package com.somewhere.app.data.location

import com.somewhere.app.util.LocationSample
import org.junit.Assert.*
import org.junit.Test

class TripNavigationTrackerTest {
    private val line = listOf(TripPoint(0.0,0.0),TripPoint(0.0,.1))
    private fun sample(lat: Double,lng: Double,time: Long,accuracy: Float = 5f) = LocationSample(lat,lng,accuracy,time)
    @Test fun rejectsStaleFixes() {
        assertNull(TripNavigationTracker(TravelMode.WALKING).update(sample(0.0,0.0,1000),15000,line))
    }
    @Test fun noisyFixesDoNotTriggerRerouting() {
        val tracker = TripNavigationTracker(TravelMode.WALKING)
        for (time in 0L..30000L step 3000) assertFalse(tracker.update(sample(.001,0.0,time,120f),time,line)!!.shouldReroute)
    }
    @Test fun sustainedReliableDeviationTriggersOneRerouteThenCooldown() {
        val tracker = TripNavigationTracker(TravelMode.WALKING)
        val triggered = (0L..24000L step 3000).count { time -> tracker.update(sample(.002,0.0,time),time,line)!!.shouldReroute }
        assertEquals(1,triggered)
    }
    @Test fun loopsDoNotJumpFromStartToReturnLeg() {
        val tracker = TripNavigationTracker(TravelMode.WALKING)
        val loop = listOf(TripPoint(0.0,0.0),TripPoint(0.0,.01),TripPoint(.01,.01),TripPoint(.01,0.0),TripPoint(0.0,0.0))
        val update = tracker.update(sample(0.0,.00001,0),0,loop)!!
        assertTrue(update.progressMeters < 10)
    }
    @Test fun drivingFilterAcceptsNormalDrivingSpeed() {
        val tracker = TripNavigationTracker(TravelMode.DRIVING)
        tracker.update(sample(0.0,0.0,0),0,line)
        val update = tracker.update(sample(0.0,.0009,3000),3000,line)!!
        assertTrue(update.progressMeters > 70)
    }
    @Test fun visitRequiresAccurateDwellNotJustEnteringGeofence() {
        val tracker = TripNavigationTracker(TravelMode.WALKING)
        assertFalse(tracker.isVisit("one",20.0,5f,0))
        assertFalse(tracker.isVisit("one",20.0,5f,4000))
        assertTrue(tracker.isVisit("one",20.0,5f,8000))
        assertFalse(tracker.isVisit("two",20.0,60f,8000))
    }
    @Test fun projectionMeasuresSegmentsRatherThanSampleCircles() {
        val point = TripPoint(.001,.05)
        val projection = TripGeometry.project(point,line)
        assertEquals(111.2,projection.distanceMeters,1.0)
        assertEquals(TripGeometry.length(line)/2,projection.progressMeters,1.0)
    }
    @Test fun dateLineProjectionUsesShortLongitudeDifference() {
        val projection = TripGeometry.project(TripPoint(.001,180.0),listOf(TripPoint(0.0,179.99),TripPoint(0.0,-179.99)))
        assertEquals(111.2,projection.distanceMeters,1.0)
    }
}
