package com.app.whitelabel.model

import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

data class PlaceDetails(
    var title: String = "",
    var snippeet: String = "",
    var latLng: LatLng? = null
) : Serializable {
}