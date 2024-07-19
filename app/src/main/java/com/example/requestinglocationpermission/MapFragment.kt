package com.example.requestinglocationpermission

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.requestinglocationpermission.databinding.FragmentMapBinding

private const val PERMISSION_CODE_REQUEST_LOCATION = 1

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: FragmentMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: ParkingViewModel by activityViewModels()
    private var carMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

            binding.btnParkedHere.setOnClickListener {
                Log.d("MapFragment", "I'm parked here button clicked")
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation()
                } else {
                    Log.d("MapFragment", "Location permission not granted, requesting permission")
                    requestLocationPermission()
                }
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Error in onViewCreated: ${e.message}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapFragment", "Map is ready")
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationale {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_CODE_REQUEST_LOCATION
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_CODE_REQUEST_LOCATION
            )
        }
    }

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton("OK") { _, _ -> positiveAction() }
            .create()
            .show()
    }

    private fun getLastLocation() {
        Log.d("MapFragment", "getLastLocation() called")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d("MapFragment", "Location obtained: ${location.latitude}, ${location.longitude}")
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        moveCarMarker(currentLatLng)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        viewModel.setParkingLocation(currentLatLng)
                    } else {
                        Log.d("MapFragment", "Location is null, requesting new location")
                        requestNewLocation()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MapFragment", "Error getting location: ${exception.message}")
                }
        } else {
            Log.d("MapFragment", "Location permission not granted")
        }
    }

    private fun requestNewLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d("MapFragment", "New location obtained: ${location.latitude}, ${location.longitude}")
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        moveCarMarker(currentLatLng)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        viewModel.setParkingLocation(currentLatLng)
                    } else {
                        Log.d("MapFragment", "New location is null")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MapFragment", "Error getting new location: ${exception.message}")
                }
        }
    }

    private fun moveCarMarker(location: LatLng) {
        try {
            Log.d("MapFragment", "Moving car marker to: $location")
            if (carMarker == null) {
                Log.d("MapFragment", "Creating new car marker")
                carMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title("Parked Car")
                        .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.ic_car))) // Use Bitmap
                )
                if (carMarker == null) {
                    Log.e("MapFragment", "Error creating car marker")
                } else {
                    Log.d("MapFragment", "Car marker created successfully")
                }
            } else {
                Log.d("MapFragment", "Updating existing car marker position")
                carMarker?.position = location
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Error moving car marker: ${e.message}")
        }
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable: Drawable = ContextCompat.getDrawable(requireContext(), drawableId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Log.d("MapFragment", "Location permission denied")
            }
        }
    }
}
