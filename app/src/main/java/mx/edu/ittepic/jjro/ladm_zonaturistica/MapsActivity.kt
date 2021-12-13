package mx.edu.ittepic.jjro.ladm_zonaturistica

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.ittepic.jjro.ladm_zonaturistica.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var baseRemota = FirebaseFirestore.getInstance()
    var data2 = ArrayList<Data>()
    var latitud = 0.0
    var longitud = 0.0
    var area = ""
    lateinit var ubicacion: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var extra = intent.extras
        area=extra!!.get("dato").toString()
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        ubicacion = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        baseRemota.collection("centro")
            .whereEqualTo("nombre",area)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null){
                    Toast.makeText(this,"Error: "+firebaseFirestoreException.message, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                var resultado = ""
                data2.clear()
                for(document in querySnapshot!!){
                    var data = Data()

                    data.nombre = document.getString("nombre").toString()
                    data.posicion1 = document.getGeoPoint("posicion1")!!
                    data.posicion2 = document.getGeoPoint("posicion2")!!

                    resultado += data.toString()+"\n\n"
                    data2.add(data)
                    latitud = data.posicion1.latitude
                    longitud = data.posicion1.longitude
                }
                mMap = googleMap
                val sydney = LatLng(latitud,longitud)
                mMap.addMarker(MarkerOptions().position(sydney).title(area))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
                mMap.uiSettings.isZoomControlsEnabled=true
                mMap.uiSettings.isMyLocationButtonEnabled=true
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@addSnapshotListener
                }
                mMap.isMyLocationEnabled = true
                ubicacion.lastLocation.addOnSuccessListener {
                    if(it!=null){
                        val posicion = LatLng(latitud,longitud)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion,18f))
                    }
                }
            }
    }
}