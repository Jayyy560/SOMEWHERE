package com.somewhere.app.util

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class ARUtilsTest {
    private val identity = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

    @Test fun centerProjectsUsingTheActualViewport() {
        val point = ARUtils.projectToScreen(floatArrayOf(0f, 0f, 0f), identity, identity, 400, 800)!!
        assertEquals(200f, point.first, 0f)
        assertEquals(400f, point.second, 0f)
    }

    @Test fun nearFarClippingAndInvalidCoordinatesAreRejected() {
        assertNull(ARUtils.projectToScreen(floatArrayOf(0f, 0f, 2f), identity, identity, 400, 800))
        assertNull(ARUtils.projectToScreen(floatArrayOf(0f, 0f, -2f), identity, identity, 400, 800))
        assertNull(ARUtils.projectToScreen(floatArrayOf(Float.NaN, 0f, 0f), identity, identity, 400, 800))
        assertNull(ARUtils.projectToScreen(floatArrayOf(0f, 0f, 0f), identity, identity, 0, 800))
    }

    @Test fun behindCameraIsRejected() {
        val projection = identity.copyOf().apply { this[15] = -1f }
        assertNull(ARUtils.projectToScreen(floatArrayOf(0f, 0f, 0f), identity, projection, 400, 800))
    }

    @Test fun discoveryRangeExtendsPastTheOldHundredMetreFarPlane() {
        val near = 0.1f
        val far = LocationUtils.DISCOVERY_RADIUS + LocationUtils.HYSTERESIS_MARGIN + 50f
        val projection = FloatArray(16).apply {
            this[0] = 1f
            this[5] = 1f
            this[10] = -(far + near) / (far - near)
            this[11] = -1f
            this[14] = -2f * far * near / (far - near)
        }
        assertNotNull(ARUtils.projectToScreen(floatArrayOf(0f, 0f, -200f), identity, projection, 400, 800))
        assertNull(ARUtils.projectToScreen(floatArrayOf(0f, 0f, -600f), identity, projection, 400, 800))
    }

    @Test fun quaternionCompassBearingsAndVerticalCamera() {
        val half = sqrt(0.5f)
        assertEquals(0f, ARUtils.headingFromQuaternion(floatArrayOf(0f, 0f, 0f, 1f))!!, 0.001f)
        assertEquals(90f, ARUtils.headingFromQuaternion(floatArrayOf(0f, -half, 0f, half))!!, 0.001f)
        assertEquals(180f, ARUtils.headingFromQuaternion(floatArrayOf(0f, 1f, 0f, 0f))!!, 0.001f)
        assertEquals(270f, ARUtils.headingFromQuaternion(floatArrayOf(0f, half, 0f, half))!!, 0.001f)
        assertNull(ARUtils.headingFromQuaternion(floatArrayOf(half, 0f, 0f, half)))
    }
}
