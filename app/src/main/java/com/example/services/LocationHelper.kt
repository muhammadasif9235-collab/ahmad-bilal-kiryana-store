package com.example.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedLocation(
    val latitude: Double,
    val longitude: Double,
    val addressLine: String,
    val mapsUrl: String
)

object LocationHelper {

    fun buildGoogleMapsUrl(latitude: Double, longitude: Double): String {
        val latStr = String.format(Locale.US, "%.6f", latitude)
        val lngStr = String.format(Locale.US, "%.6f", longitude)
        return "https://maps.google.com/?q=$latStr,$lngStr"
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            runCatching {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }.getOrDefault(false)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(context: Context): Result<DetectedLocation> {
        if (!hasLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.failure(IllegalStateException("Location service unavailable"))

        val location = withContext(Dispatchers.Main) {
            // Try a fresh fix from enabled providers (Network/Fused first for fast indoor lock, then GPS)
            val providersToTry = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    runCatching { lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false)
                ) {
                    add(LocationManager.FUSED_PROVIDER)
                }
                if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.GPS_PROVIDER)
                }
                if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.NETWORK_PROVIDER)
                }
            }

            var freshFix: Location? = null
            for (provider in providersToTry) {
                freshFix = requestSingleLocationFix(context, lm, provider, timeoutMs = 6500L)
                if (freshFix != null) break
            }

            freshFix ?: getBestLastKnownLocation(lm)
        } ?: return Result.failure(
            IllegalStateException(
                if (!isLocationEnabled(context)) {
                    "GPS is turned off. Please turn on device Location / GPS."
                } else {
                    "Unable to lock GPS signal right now. Try moving near a window or enter address manually."
                }
            )
        )

        val readableAddress = reverseGeocodeAddress(context, location.latitude, location.longitude)
        val mapsUrl = buildGoogleMapsUrl(location.latitude, location.longitude)
        return Result.success(
            DetectedLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                addressLine = readableAddress,
                mapsUrl = mapsUrl
            )
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocationFix(
        context: Context,
        lm: LocationManager,
        provider: String,
        timeoutMs: Long
    ): Location? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    try {
                        lm.getCurrentLocation(
                            provider,
                            signal,
                            ContextCompat.getMainExecutor(context)
                        ) { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                    } catch (_: Throwable) {
                        if (cont.isActive) cont.resume(null)
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            runCatching { lm.removeUpdates(this) }
                            if (cont.isActive) cont.resume(location)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                        override fun onProviderEnabled(provider: String) = Unit

                        override fun onProviderDisabled(provider: String) = Unit
                    }
                    cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
                    try {
                        @Suppress("DEPRECATION")
                        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    } catch (_: Throwable) {
                        runCatching { lm.removeUpdates(listener) }
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(lm: LocationManager): Location? {
        val providers = runCatching { lm.getProviders(true) }.getOrNull().orEmpty() +
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (provider in providers.distinct()) {
            val candidate = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || candidate.time > best.time ||
                (candidate.time == best.time && candidate.accuracy < best.accuracy)
            ) {
                best = candidate
            }
        }
        return best
    }

    private suspend fun reverseGeocodeAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext formatCoordinatesFallback(latitude, longitude)
        }
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(4500L) {
                    suspendCancellableCoroutine { cont ->
                        try {
                            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(addresses.firstOrNull())
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            })
                        } catch (_: Throwable) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }

            if (address != null) {
                val fullLine = address.getAddressLine(0)?.trim().orEmpty()
                if (fullLine.isNotBlank()) return@withContext fullLine
                val parts = listOfNotNull(
                    address.featureName,
                    address.thoroughfare,
                    address.subLocality,
                    address.locality,
                    address.subAdminArea,
                    address.adminArea
                ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
                if (parts.isNotEmpty()) return@withContext parts.joinToString(", ")
            }
            formatCoordinatesFallback(latitude, longitude)
        } catch (_: Throwable) {
            formatCoordinatesFallback(latitude, longitude)
        }
    }

    private fun formatCoordinatesFallback(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "GPS: %.5f, %.5f", latitude, longitude)
    }
}
