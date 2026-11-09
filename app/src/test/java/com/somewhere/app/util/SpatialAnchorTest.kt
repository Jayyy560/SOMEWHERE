package com.somewhere.app.util

import com.google.ar.core.Anchor
import com.somewhere.app.data.local.SpatialAnchorConverter
import com.somewhere.app.data.model.*
import com.somewhere.app.data.remote.DropInsert
import com.somewhere.app.data.remote.NearbyDrop
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SpatialAnchorTest {
    private val pose = GeospatialAnchorPose(28.6, 77.2, 210.0, 0f, 0f, 0f, 1f)
    private val anchor = SpatialAnchor("cloud-id", 10_000L, pose)
    private val placement = VisualPlacement(28.6, 77.2, 8f, 1_000L, "arcore-cloud-gps", anchor)

    @Test fun cloudAnchorExpiryIsStrictAndBlankIdsCannotResolve() {
        assertTrue(anchor.canResolveAt(9_999L))
        assertFalse(anchor.canResolveAt(10_000L))
        assertFalse(anchor.copy(cloudAnchorId = " ").canResolveAt(1L))
    }

    @Test fun persistedDraftRoundTripsWithSurfacePose() {
        assertEquals(placement, Json.decodeFromString<VisualPlacement>(Json.encodeToString(placement)))
    }

    @Test fun roomConverterPreservesCloudAndEarthMetadata() {
        val converter = SpatialAnchorConverter()
        assertEquals(anchor, converter.decode(converter.encode(anchor)))
        assertNull(converter.encode(null))
        assertNull(converter.decode(null))
        assertNull(converter.decode("not json"))
    }

    @Test fun roomConverterAcceptsFutureOptionalFields() {
        val json = """{"cloudAnchorId":"cloud-id","expiresAtMillis":10000,"futureField":true}"""
        assertEquals(anchor.copy(geospatialPose = null), SpatialAnchorConverter().decode(json))
    }

    @Test fun oldRemoteRowsRemainCompatible() {
        val old = """{"id":"old","text":"hello","image_url":"https://example.test/image","latitude":28.6,"longitude":77.2}"""
        assertNull(Json.decodeFromString<NearbyDrop>(old).spatialAnchor)
    }

    @Test fun remotePublicationAndRetrievalPreserveTheSameCloudId() {
        val insert = DropInsert(id = "drop", text = "hello", imageUrl = "https://example.test/image",
            latitude = 28.6, longitude = 77.2, spatialAnchor = anchor)
        val encoded = Json.encodeToString(insert)
        assertTrue(Json.parseToJsonElement(encoded).jsonObject.containsKey("spatial_anchor"))
        assertEquals(anchor, Json.decodeFromString<NearbyDrop>(encoded).spatialAnchor)
    }

    @Test fun expiredOrInvalidDraftCannotBePublishedAsAVisualPlacement() {
        assertTrue(placement.isUsableAt(2_000L))
        assertFalse(placement.isUsableAt(10_000L))
        assertFalse(placement.copy(latitude = Double.NaN).isUsableAt(2_000L))
        assertFalse(placement.copy(longitude = 181.0).isUsableAt(2_000L))
        assertFalse(placement.copy(accuracyMeters = Float.NaN).isUsableAt(2_000L))
        assertFalse(placement.copy(accuracyMeters = 0f).isUsableAt(2_000L))
    }

    @Test fun earthPoseRejectsNonFiniteValuesAndInvalidRotation() {
        assertTrue(pose.isValid)
        assertFalse(pose.copy(altitudeMeters = Double.POSITIVE_INFINITY).isValid)
        assertFalse(pose.copy(qw = 0f).isValid)
        assertFalse(pose.copy(qx = Float.NaN).isValid)
        assertFalse(pose.copy(latitude = 90.0).isValid)
    }

    @Test fun indoorVisualAnchorNeverFallsBackToOutdoorTerrain() {
        val indoor = AnchorTarget("drop", 28.6, 77.2, anchor.copy(geospatialPose = null))
        assertFalse(indoor.hasEarthPlacement)
        assertTrue(indoor.spatialAnchor!!.canResolveAt(1_000L))
        assertTrue(indoor.copy(spatialAnchor = anchor).hasEarthPlacement)
        assertTrue(indoor.copy(spatialAnchor = null).hasEarthPlacement)
        assertFalse(indoor.copy(spatialAnchor = anchor.copy(geospatialPose = pose.copy(qw = 0f))).hasEarthPlacement)
    }

    @Test fun authorizationAndInvalidIdsDoNotRetryButNetworkAndNoMatchDo() {
        assertFalse(cloudFailureIsRetryable(Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED))
        assertFalse(cloudFailureIsRetryable(Anchor.CloudAnchorState.ERROR_CLOUD_ID_NOT_FOUND))
        assertFalse(cloudFailureIsRetryable(Anchor.CloudAnchorState.ERROR_RESOLVING_SDK_VERSION_TOO_OLD))
        assertTrue(cloudFailureIsRetryable(Anchor.CloudAnchorState.ERROR_SERVICE_UNAVAILABLE))
        assertTrue(cloudFailureIsRetryable(Anchor.CloudAnchorState.ERROR_RESOLVING_LOCALIZATION_NO_MATCH))
    }
}
