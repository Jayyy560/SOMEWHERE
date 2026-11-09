package com.somewhere.app.util

internal data class AnchorTarget(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val spatialAnchor: com.somewhere.app.data.model.SpatialAnchor? = null
) {
    val isValid: Boolean
        get() = id.isNotBlank() && latitude.isFinite() && longitude.isFinite() &&
            latitude in -89.9..89.9 && longitude in -180.0..180.0

    val hasEarthPlacement: Boolean
        get() = spatialAnchor == null || spatialAnchor.geospatialPose?.isValid == true
}

internal sealed interface AnchorRequestResult<out A> {
    data object Pending : AnchorRequestResult<Nothing>
    data class Resolved<A>(val anchor: A) : AnchorRequestResult<A>
    data class Failed(val retryable: Boolean) : AnchorRequestResult<Nothing>
}

internal interface AnchorRequest<A> {
    fun poll(): AnchorRequestResult<A>
    fun cancel()
}

/** Owned by one AR scene; all calls happen on the scene's main-thread frame callback. */
internal class AnchorRequestStore<A : Any>(
    private val nowMillis: () -> Long,
    private val detach: (A) -> Unit,
    private val isStopped: (A) -> Boolean,
    private val maxConcurrentRequests: Int = 4,
    private val requestTimeoutMillis: Long = 20_000L
) {
    private class Entry<A>(val target: AnchorTarget) {
        var anchor: A? = null
        var request: AnchorRequest<A>? = null
        var startedAt = 0L
        var retryAt = 0L
        var failures = 0
        var permanentFailure = false
    }

    private val entries = linkedMapOf<String, Entry<A>>()

    fun update(
        targets: List<AnchorTarget>,
        resolve: (AnchorTarget) -> AnchorRequest<A>
    ) {
        val desired = targets.filter { it.isValid }.distinctBy { it.id }.take(50)
            .associateBy { it.id }
        entries.keys.toList().forEach { id ->
            if (entries.getValue(id).target != desired[id]) {
                dispose(entries.remove(id)!!)
            }
        }
        desired.forEach { (id, target) -> entries.getOrPut(id) { Entry(target) } }

        val now = nowMillis()
        entries.values.forEach { entry ->
            entry.anchor?.takeIf(isStopped)?.let { anchor ->
                runCatching { detach(anchor) }
                entry.anchor = null
                scheduleRetry(entry, now)
            }
            entry.request?.let { request ->
                val result = runCatching { request.poll() }
                    .getOrElse { AnchorRequestResult.Failed(retryable = true) }
                when (result) {
                    AnchorRequestResult.Pending -> if (now - entry.startedAt >= requestTimeoutMillis) {
                        cancel(request)
                        entry.request = null
                        scheduleRetry(entry, now)
                    }
                    is AnchorRequestResult.Resolved -> {
                        entry.request = null
                        entry.anchor = result.anchor
                        entry.failures = 0
                    }
                    is AnchorRequestResult.Failed -> {
                        cancel(request)
                        entry.request = null
                        entry.permanentFailure = !result.retryable
                        scheduleRetry(entry, now)
                    }
                }
            }
        }

        var active = entries.values.count { it.request != null }
        entries.values.forEach { entry ->
            if (active < maxConcurrentRequests && entry.anchor == null && entry.request == null &&
                !entry.permanentFailure && now >= entry.retryAt
            ) {
                try {
                    entry.request = resolve(entry.target)
                    entry.startedAt = now
                    active++
                } catch (_: Exception) {
                    scheduleRetry(entry, now)
                }
            }
        }
    }

    fun anchors(): Map<String, A> = entries.mapNotNull { (id, entry) ->
        entry.anchor?.let { id to it }
    }.toMap()

    fun clear() {
        entries.values.forEach(::dispose)
        entries.clear()
    }

    private fun scheduleRetry(entry: Entry<A>, now: Long) {
        entry.failures++
        val delay = (2_000L shl (entry.failures - 1).coerceAtMost(4)).coerceAtMost(30_000L)
        entry.retryAt = now + delay
    }

    private fun dispose(entry: Entry<A>) {
        entry.request?.let(::cancel)
        entry.anchor?.let { anchor -> runCatching { detach(anchor) } }
    }

    private fun cancel(request: AnchorRequest<A>) {
        runCatching { request.cancel() }
        // A completion can race cancellation. Adopt and detach that result here rather
        // than allowing an orphaned anchor to survive the scene that requested it.
        val result = runCatching { request.poll() }.getOrNull()
        if (result is AnchorRequestResult.Resolved) runCatching { detach(result.anchor) }
    }
}
