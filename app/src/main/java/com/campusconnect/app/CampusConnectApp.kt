package com.campusconnect.app

import android.app.Application
import com.campusconnect.app.di.AppContainer
import com.campusconnect.app.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CampusConnectApp : Application() {

    /** App-lifetime coroutine scope — outlives any single Activity/Composable. */
    val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)

        // Drive the WebSocket connection from auth state: connect once
        // logged in, disconnect (and let it go idle) on logout — rather
        // than each screen managing the socket lifecycle independently.
        applicationScope.launch {
            container.tokenManager.isLoggedInFlow.collect { isLoggedIn ->
                if (isLoggedIn) {
                    container.realtimeService.connect()
                } else {
                    container.realtimeService.disconnect()
                }
            }
        }
    }
}
