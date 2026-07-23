package com.somewhere.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class NetworkStatus {
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

@Composable
fun rememberNetworkStatus(): NetworkStatus {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var status by remember {
        mutableStateOf(
            if (connectivityManager.isOnline()) NetworkStatus.AVAILABLE
            else NetworkStatus.UNAVAILABLE
        )
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                status = NetworkStatus.AVAILABLE
            }

            override fun onLost(network: Network) {
                status = if (connectivityManager.isOnline()) {
                    NetworkStatus.AVAILABLE
                } else {
                    NetworkStatus.UNAVAILABLE
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                status = if (
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                ) {
                    NetworkStatus.AVAILABLE
                } else {
                    NetworkStatus.UNAVAILABLE
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    return status
}

private fun ConnectivityManager.isOnline(): Boolean {
    val network = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
