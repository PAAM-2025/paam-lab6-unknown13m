package upt.paam.lab6

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import upt.paam.lab6.location.LocationHandler
import upt.paam.lab6.ui.theme.LAB6Theme

class MainActivity : ComponentActivity() {

    private val latLngState =
        mutableStateOf(LatLng(45.74745411062225, 21.226260284261446))

    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isLocationPermissionGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_ID
            )
        }

        enableEdgeToEdge()

        setContent {
            LAB6Theme {
                Column(modifier = Modifier.fillMaxSize()) {

                    Row(modifier = Modifier.weight(8f)) {
                        MapComposable()
                    }

                    Row(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LocationComposable()
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { getCurrentLocation() }) {
                                Text("Get current location")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MapComposable() {
        var mapLoaded by remember { mutableStateOf(false) }

        val markerState = remember { MarkerState(latLngState.value) }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(latLngState.value, 10f)
        }

        LaunchedEffect(latLngState.value) {
            if (!mapLoaded) return@LaunchedEffect

            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition(latLngState.value, 15f, 0f, 0f)
                ),
                durationMs = 1000
            )

            markerState.position = latLngState.value
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true }
        ) {
            Marker(
                state = markerState,
                title = "Current location"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setupLocation()
        LocationHandler(this).registerLocationListener(locationCallback!!)
    }

    private fun setupLocation() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    latLngState.value = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationPermissionGranted) return

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    latLngState.value = LatLng(it.latitude, it.longitude)
                }
            }
            .addOnFailureListener {
                Log.e("Location", "Failed to get current location", it)
            }
    }

    @Composable
    private fun LocationComposable() {
        Text(
            text = "Lat = ${latLngState.value.latitude}, Lng = ${latLngState.value.longitude}"
        )
    }


    private val isLocationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_ID = 111
    }
}
