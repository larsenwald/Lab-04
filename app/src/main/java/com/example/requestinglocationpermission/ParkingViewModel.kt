package com.example.requestinglocationpermission

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class ParkingViewModel : ViewModel() {
    private val _parkingLocation = MutableLiveData<LatLng>()
    val parkingLocation: LiveData<LatLng> get() = _parkingLocation

    fun setParkingLocation(location: LatLng) {
        _parkingLocation.value = location
    }
}
