package com.campusconnect.app.data.remote

/**
 * Central place to point the app at a backend and to toggle security
 * features that depend on infrastructure not yet in place.
 */
object NetworkConfig {

    /**
     * 10.0.2.2 is the Android emulator's alias for the host machine's
     * localhost — correct for running the backend locally while developing
     * against an emulator. For a PHYSICAL device on the same network,
     * replace with your machine's LAN IP (e.g. "http://192.168.1.42:3000/api/v1/").
     * For production, this must be an HTTPS URL on a real domain — plain
     * HTTP is only permitted for the hosts listed in
     * res/xml/network_security_config.xml, and only in this dev config.
     */
    const val BASE_URL = "http://10.0.2.2:3000/api/v1/"

    const val WEBSOCKET_URL = "http://10.0.2.2:3000/ws"

    /**
     * Certificate pinning (OkHttp CertificatePinner) — OFF by default because
     * there is no production TLS certificate to pin against yet (the backend
     * has no HTTPS listener configured in dev). Turn this on and fill in real
     * pins once a production domain + certificate exist.
     *
     * To generate a real pin once you have one:
     *   openssl s_client -connect api.campusconnect.app:443 -servername api.campusconnect.app </dev/null 2>/dev/null | \
     *     openssl x509 -pubkey -noout | \
     *     openssl pkey -pubin -outform der | \
     *     openssl dgst -sha256 -binary | \
     *     openssl enc -base64
     *
     * Pin BOTH the current leaf certificate's key AND your CA/intermediate's
     * key (or a backup leaf key) — pinning only the leaf means a routine
     * cert renewal locks every installed copy of the app out of the API
     * until an update ships.
     */
    const val ENABLE_CERTIFICATE_PINNING = false

    /** hostname -> list of "sha256/<base64>" pins. Empty until real certs exist. */
    val CERTIFICATE_PINS: Map<String, List<String>> = emptyMap()

    /**
     * Play Integrity — verifies requests come from a genuine, untampered
     * app install via Google Play (anti-emulator-fraud / anti-tampering).
     * Requires:
     *   1. A Google Cloud project linked to your Play Console listing
     *   2. That project's CLOUD_PROJECT_NUMBER below
     *   3. A backend endpoint that decodes the integrity token via Google's
     *      verification API using service-account credentials (NOT built —
     *      needs real GCP credentials this environment doesn't have; see
     *      PlayIntegrityManager.kt for the Android-side token request, which
     *      IS fully wired and ready to send once the backend endpoint exists)
     */
    const val CLOUD_PROJECT_NUMBER = 0L // TODO: replace with your real GCP project number
    const val ENABLE_PLAY_INTEGRITY = false
}
