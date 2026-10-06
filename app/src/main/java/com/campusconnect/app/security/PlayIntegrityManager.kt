package com.campusconnect.app.security

import android.content.Context
import com.campusconnect.app.data.remote.NetworkConfig
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Requests a Play Integrity token — Google's attestation that this request
 * is coming from a genuine, untampered copy of the app installed via Play,
 * running on a genuine (non-rooted/non-emulated) device.
 *
 * IMPORTANT — this is HALF a security feature without its other half:
 *   1. Android side (this class) — gets a token, ready to attach to requests.
 *   2. Backend side (NOT built) — must send the token to Google's
 *      verification API using service-account credentials tied to your
 *      GCP project, decode the verdict, and reject the request if the
 *      verdict looks bad. This environment has no real GCP project or
 *      credentials to build that against, so it isn't implemented.
 *      Google's docs: https://developer.android.com/google/play/integrity/verdict#decrypt-verify
 *
 * Until both halves exist, requesting a token and attaching it as a header
 * is a no-op from a security standpoint — nothing checks it. Gate real use
 * behind [NetworkConfig.ENABLE_PLAY_INTEGRITY] once the backend half ships.
 *
 * NOTE ON API STABILITY: Google has evolved this API's shape over time —
 * the "classic" IntegrityManager used below (requestIntegrityToken with a
 * nonce) versus the newer "Standard Integrity API"
 * (StandardIntegrityManager / PrepareIntegrityTokenRequest, which adds a
 * warm-up step that lowers per-request latency). The classic API used here
 * is the longer-established, better-documented surface; double-check
 * against the current Play Integrity docs before shipping, since exact
 * method signatures can shift between library versions.
 */
class PlayIntegrityManager(context: Context) {

    private val integrityManager = IntegrityManagerFactory.create(context)

    /**
     * @param nonce a fresh, single-use, server-provided random value (base64
     *        or hex, per Google's spec) tied to the specific request being
     *        protected — request this from the backend per-call once the
     *        verification endpoint exists. Never reuse a nonce.
     */
    suspend fun requestIntegrityToken(nonce: String): Result<String> = suspendCancellableCoroutine { cont ->
        if (!NetworkConfig.ENABLE_PLAY_INTEGRITY) {
            cont.resume(Result.failure(IllegalStateException("Play Integrity is disabled — see NetworkConfig.ENABLE_PLAY_INTEGRITY")))
            return@suspendCancellableCoroutine
        }

        val request = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(NetworkConfig.CLOUD_PROJECT_NUMBER)
            .build()

        integrityManager.requestIntegrityToken(request)
            .addOnSuccessListener { response ->
                if (cont.isActive) cont.resume(Result.success(response.token()))
            }
            .addOnFailureListener { exception ->
                if (cont.isActive) cont.resume(Result.failure(exception))
            }
    }
}
