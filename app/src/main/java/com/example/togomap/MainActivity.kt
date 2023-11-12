package com.example.togomap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.android.gms.maps.model.Marker


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationMarker: Marker? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Luodaan päälayout
        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Lisäämme karttafragmentin layoutiin
        val mapFragment = SupportMapFragment.newInstance()
        mapFragment.getMapAsync(this)

        // Käytetään satunnainen tunniste
        val containerId = View.generateViewId()
        mainLayout.id = containerId
        supportFragmentManager.beginTransaction().replace(containerId, mapFragment).commit()

        // Pyydä sijaintilupa tarvittaessa
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Luo FusedLocationProviderClient
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }

        // Luo LayoutParams Buttonille
        val buttonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonLayoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL


        // Lisää Button kartan yläpuolelle
        val zoomToLocationButton = Button(this)
        zoomToLocationButton.layoutParams = buttonLayoutParams
        zoomToLocationButton.text = "Find my location"
        zoomToLocationButton.setOnClickListener {
            zoomToCurrentLocation()
        }
        mainLayout.addView(zoomToLocationButton)


        // Luo Spinner-elementti eli dropdown valikko
        val mapTypeSpinner = Spinner(this)

        // Luo adapteri valintojen määritykselle (esimerkiksi karttatyypeille)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.map_types, // res/values/arrays.xml: <string-array name="map_types">...</string-array>
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Aseta adapteri Spinneriin
        mapTypeSpinner.adapter = adapter

        // Lisää kuuntelija Spinnerille
        mapTypeSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                // Aseta karttatyyppi valinnan perusteella
                when (position) {
                    0 -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    1 -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    2 -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    3 -> googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Ei mitään erityistä tehtävissä, jos mitään ei ole valittu
            }
        })

        // Lisää Spinner päälayoutiin
        mainLayout.addView(mapTypeSpinner)

        // Aseta päälayout näytölle
        setContentView(mainLayout)

    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap

        // Lisää oletussijaintiin merkki
        val location = LatLng(0.0, 0.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
    }

    private fun zoomToCurrentLocation() {
        // Tarkista sijaintilupa
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000 // 10 seconds
                fastestInterval = 5000 // 5 seconds
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            // Pyydä nykyinen sijainti
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        val location = locationResult.lastLocation
                        if (location != null) {
                            Log.d("Location", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            currentLocationMarker?.remove()
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                            // Voit lopettaa sijaintipäivitykset, jos haluat
                            fusedLocationClient.removeLocationUpdates(this)
                            val title = "You're here (${location.latitude}, ${location.longitude})"
                            googleMap.addMarker(MarkerOptions().position(currentLatLng).title(title))
                        } else {
                            Log.e("Location", "Current location is null")
                        }
                    }
                },
                Looper.getMainLooper()
            )
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

