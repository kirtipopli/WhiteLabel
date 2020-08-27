package com.app.whitelabel

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.whitelabel.adapters.PlacesListAdapter
import com.app.whitelabel.model.PlaceDetails
import com.app.whitelabel.model.ProfileDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    CompoundButton.OnCheckedChangeListener, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private val TAG: String = MapsActivity::class.java.simpleName
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    private var placesListAdapter: PlacesListAdapter? = null
    private var currentPlacesList: ArrayList<PlaceDetails>? = null

    private val API_KEY = "AIzaSyAyB7asyhW7JK6hyK90S_Ow_ai145KH14Y"

    private val PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place"

    private val TYPE_AUTOCOMPLETE = "/autocomplete"
    private val TYPE_DETAILS = "/details"
    private val TYPE_SEARCH = "/nearbysearch"
    private val OUT_JSON = "/json?"
    private val LOG_TAG = "ListRest"
    var latitude = 0.0
    var longitude = 0.0
    private val PROXIMITY_RADIUS = 10000
    var mGoogleApiClient: GoogleApiClient? = null
    var mLastLocation: Location? = null
    var mCurrLocationMarker: Marker? = null
    var mLocationRequest: LocationRequest? = null
    var adRequest: AdRequest? = null

    // The entry point to the Places API.
    private var placesClient: PlacesClient? = null

    // The entry point to the Fused Location Provider.
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(23.63936, 68.14712)
    private val DEFAULT_ZOOM = 15
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    // Keys for storing activity state.
    private val KEY_CAMERA_POSITION = "camera_position"
    private val KEY_LOCATION = "location"

    // Used for selecting the current place.
    private val M_MAX_ENTRIES = 5
    private var likelyPlaceNames: Array<String?>? = null
    private var likelyPlaceAddresses: Array<String?>? = null
    private var likelyPlaceAttributions: Array<List<*>?>? = null
    private var likelyPlaceLatLngs: Array<LatLng?>? = null

    private var profileDetails: ProfileDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main)

        getIntentData()

        rb_MapView.setOnCheckedChangeListener(this)
        rb_ListView.setOnCheckedChangeListener(this)
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)

        currentPlacesList = ArrayList()
        placesListAdapter = PlacesListAdapter(this, currentPlacesList, ::onPlaceSelected)
        recycler_placesList.adapter = placesListAdapter

        // Construct a PlacesClient
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        MobileAds.initialize(this) {

        }

        adRequest = AdRequest.Builder().build()
        ad_View.loadAd(adRequest)

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.TYPES,
                Place.Field.BUSINESS_STATUS
            )
        )

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "Place: ${place.name}, ${place.id}")
            }

            override fun onError(status: Status) {
                Log.i(TAG, "An error occurred: $status")
            }
        })

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map!!.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    private fun getIntentData() {
        if (intent.hasExtra("data"))
            profileDetails = intent.extras?.getSerializable("data")?.let {
                it as ProfileDetails
            }
    }

    override fun onMapReady(map: GoogleMap?) {
        this.map = map

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                === PackageManager.PERMISSION_GRANTED
            ) {
                buildGoogleApiClient()
                map!!.isMyLocationEnabled = true
            }
        } else {
            buildGoogleApiClient()
            map!!.isMyLocationEnabled = true
        }

        this.map!!.setInfoWindowAdapter(object : InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow: View = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    findViewById<View>(R.id.mapView) as FrameLayout, false
                )
                val title: TextView = infoWindow.findViewById(R.id.title)
                title.text = marker.title
                val snippet: TextView = infoWindow.findViewById(R.id.snippet)
                snippet.text = marker.snippet
                return infoWindow
            }
        })

        getLocationPermission()

        updateLocationUI()

        getDeviceLocation()
    }

    private fun getDeviceLocation() {

        try {
            if (locationPermissionGranted) {
                val locationResult: Task<Location> = fusedLocationProviderClient!!.lastLocation
                locationResult.addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.getResult()
                        if (lastKnownLocation != null) {
                            map!!.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation?.latitude!!,
                                        lastKnownLocation?.longitude!!
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.getException())
                        map!!.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        map!!.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (mGoogleApiClient == null) {
                buildGoogleApiClient()
            }
            map!!.isMyLocationEnabled = true

            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    @Synchronized
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient?.connect()
    }

    override fun onConnected(bundle: Bundle?) {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 1000
        mLocationRequest!!.fastestInterval = 1000
        mLocationRequest!!.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            === PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
            )
        }
    }

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }


    override fun onConnectionSuspended(i: Int) {}
    override fun onLocationChanged(location: Location) {
        Log.d("onLocationChanged", "entered")
        mLastLocation = location
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker!!.remove()
        }

        //Place current location marker
        latitude = location.latitude
        longitude = location.longitude
        val latLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("Current Position")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        mCurrLocationMarker = map!!.addMarker(markerOptions)

        //move map camera
        map!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        map!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
        Toast.makeText(this@MapsActivity, "Your Current Location", Toast.LENGTH_LONG).show()
        Log.d(
            "onLocationChanged",
            String.format("latitude:%.3f longitude:%.3f", latitude, longitude)
        )

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            Log.d("onLocationChanged", "Removing Location Updates")
        }
        Log.d("onLocationChanged", "Exit")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }


    private fun onPlaceSelected(
        placeDetails: @ParameterName(name = "placeDetails") PlaceDetails?, position: @ParameterName(
            name = "position"
        ) Int
    ) {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment?
        if (!mapFragment?.isVisible!!) {
            mapFragment?.let { supportFragmentManager.beginTransaction().show(it).commit() }
        }
        recycler_placesList?.visibility = View.GONE

        val markerLatLng = placeDetails?.latLng
        var markerSnippet = placeDetails?.snippeet

        map!!.addMarker(
            MarkerOptions()
                .title(placeDetails?.title)
                .position(markerLatLng!!)
                .snippet(markerSnippet)
        )

        // Position the map's camera at the location of the marker.
        map!!.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                markerLatLng,
                DEFAULT_ZOOM.toFloat()
            )
        )

    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map!!.isMyLocationEnabled = true
                map!!.uiSettings.isMyLocationButtonEnabled = true
            } else {
                map!!.isMyLocationEnabled = false
                map!!.uiSettings.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView?.id == rb_MapView.id) {
            recycler_placesList?.visibility = View.GONE

            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapView) as SupportMapFragment?
            mapFragment?.let { supportFragmentManager.beginTransaction().show(it).commit() }

            var Restaurant = "restaurant"
            Log.d("onClick", "Button is Clicked")
            map!!.clear()
            val url = getUrl(latitude, longitude, Restaurant)
            val DataTransfer = arrayOfNulls<Any>(2)
            DataTransfer[0] = map
            DataTransfer[1] = url
            Log.d("onClick", url)
            val getNearbyPlacesData = search(latitude, longitude, 200)
            currentPlacesList?.clear()
            if (!getNearbyPlacesData.isNullOrEmpty()) {
                for (items in getNearbyPlacesData) {
                    Log.d("MapsActivity", "Place: " + items?.title)
                    var placeDetail = PlaceDetails();
                    placeDetail.title = items?.title.toString()
                    placeDetail.snippeet = items?.snippeet.toString()
                    placeDetail.latLng = items?.latLng

                    currentPlacesList?.add(placeDetail)
                }
                placesListAdapter?.placesList = currentPlacesList
                placesListAdapter?.notifyDataSetChanged()
            }
            Toast.makeText(this@MapsActivity, "Nearby Restaurants : ", Toast.LENGTH_LONG).show()

        } else if (buttonView?.id == rb_ListView.id) {
            recycler_placesList?.visibility = View.VISIBLE
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapView) as SupportMapFragment?
            mapFragment?.let { supportFragmentManager.beginTransaction().hide(it).commit() }
//                showCurrentPlace()


        }
    }

    fun search(lat: Double, lng: Double, radius: Int): ArrayList<PlaceDetails?>? {
        var resultList: ArrayList<PlaceDetails?>? = null
        var conn: HttpURLConnection? = null
        val jsonResults = java.lang.StringBuilder()
        try {
            val sb: java.lang.StringBuilder = java.lang.StringBuilder(PLACES_API_BASE)
            sb.append(TYPE_SEARCH)
            sb.append(OUT_JSON)
            sb.append("location=$lat,$lng")
            sb.append("&radius=$radius")
            sb.append("&type=restaurant")
            sb.append("&key=$API_KEY")
            val url = URL(sb.toString())
            conn = url.openConnection() as HttpURLConnection
            val `in` = InputStreamReader(conn.getInputStream())
            var read: Int
            val buff = CharArray(1024)
            while (`in`.read(buff).also { read = it } != -1) {
                jsonResults.append(buff, 0, read)
            }
        } catch (e: MalformedURLException) {
            Log.e(LOG_TAG, "Error processing Places API URL", e)
            return resultList
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error connecting to Places API", e)
            return resultList
        } finally {
            if (conn != null) {
                conn.disconnect()
            }
        }
        try {
            // Create a JSON object hierarchy from the results
            val jsonObj = JSONObject(jsonResults.toString())
            val predsJsonArray: JSONArray = jsonObj.getJSONArray("results")

            // Extract the descriptions from the results
            resultList = ArrayList()
            for (i in 0 until predsJsonArray.length()) {
                val place = PlaceDetails()
                place.snippeet = predsJsonArray.getJSONObject(i).getString("vicinity")
                place.title = predsJsonArray.getJSONObject(i).getString("name")
                var lat = predsJsonArray.getJSONObject(i).getJSONObject("geometry")
                    .getJSONObject("location").getDouble("lat")
                var lon = predsJsonArray.getJSONObject(i).getJSONObject("geometry")
                    .getJSONObject("location").getDouble("lng")
                place.latLng = LatLng(lat, lon)
                resultList!!.add(place)
            }
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "Error processing JSON results", e)
        }
        return resultList
    }

    private fun getUrl(
        latitude: Double,
        longitude: Double,
        nearbyPlace: String
    ): String {
        val googlePlacesUrl =
            StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
        googlePlacesUrl.append("location=$latitude,$longitude")
        googlePlacesUrl.append("&radius=$PROXIMITY_RADIUS")
        googlePlacesUrl.append("&type=$nearbyPlace")
        googlePlacesUrl.append("&sensor=true")
        googlePlacesUrl.append("&key=" + "AIzaSyAyB7asyhW7JK6hyK90S_Ow_ai145KH14Y")
        Log.d("getUrl", googlePlacesUrl.toString())
        return googlePlacesUrl.toString()
    }

}