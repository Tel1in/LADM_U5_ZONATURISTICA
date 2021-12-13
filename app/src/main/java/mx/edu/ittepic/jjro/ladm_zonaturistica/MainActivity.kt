package mx.edu.ittepic.jjro.ladm_zonaturistica

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var baseRemota = FirebaseFirestore.getInstance()
    var posicion = ArrayList<Data>()
    var datalista = ArrayList<String>()
    lateinit var locacion : LocationManager
    var listaIds = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        cargarlista()
        locacion = getSystemService(LOCATION_SERVICE) as LocationManager
        var oyente = Oyente(this)
        locacion.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 01f, oyente)
        Busqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p: CharSequence, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p: CharSequence, p1: Int, p2: Int, p3: Int) {
                if(p.toString() == "")
                    cargarlista()
                else buscarArea(p.toString())
            }

            override fun afterTextChanged(p: Editable) {}
        })
    }
    fun buscarArea(s: String){
        baseRemota.collection("centro").orderBy("nombre").startAt(s).endAt(s+"\uf8ff")
            .addSnapshotListener { querySnapchot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null){
                    mensaje("ERROR DE CONEXION")
                    return@addSnapshotListener
                }
                datalista.clear()
                listaIds.clear()
                for(document in querySnapchot!!){
                    var resultado = document.getString("nombre")+"\n"+
                            document.getGeoPoint("posicion1")?.latitude+","+
                            document.getGeoPoint("posicion1")?.longitude+"\n"+
                            document.getGeoPoint("posicion2")?.latitude+","+
                            document.getGeoPoint("posicion2")?.longitude

                    datalista.add(resultado)
                    listaIds.add(document.id)
                }
                if(datalista.size == 0){
                    datalista.add("No se encuentra el area")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, datalista)
                lista.adapter = adaptador
            }
    }

    private fun cargarlista() {
        baseRemota.collection("centro")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null){
                    mensaje("ERROR: "+firebaseFirestoreException.message)
                    return@addSnapshotListener
                }
                var resultado = ""
                posicion.clear()
                datalista.clear()
                listaIds.clear()
                for(document in querySnapshot!!){
                    var data = Data()
                    data.nombre = document.getString("nombre").toString()
                    data.posicion1 = document.getGeoPoint("posicion1")!!
                    data.posicion2 = document.getGeoPoint("posicion2")!!

                    resultado += data.toString()+"\n\n"
                    posicion.add(data)
                    listaIds.add(document.id)
                }
                datalista.add(resultado)
                var adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, datalista)
                lista.adapter = adapter
                if(datalista.size == 0){
                    datalista.add("No hay data a mostrar")
                }
            }
        lista.setOnItemClickListener { adapterView, view, posicion, id ->
            if(listaIds.size == 0){
                return@setOnItemClickListener
            }
            AlertDialog.Builder(this).setTitle("ATENCION")
                .setMessage("Que desea hacer?")
                .setPositiveButton("Ver en mapa"){d,l->
                    Mapa(listaIds[posicion])
                }
                .setNegativeButton("Ver Detalles"){d,l->
                    //Detalles(listaIds[posicion])
                }
                .show()
        }
    }

    fun mensaje(s:String){
        Toast.makeText(this,s, Toast.LENGTH_LONG).show()
    }

    fun Mapa(idArea: String){
        baseRemota.collection("centro")
            .document(idArea)
            .get()
            .addOnSuccessListener {
                var intent = Intent(this,MapsActivity::class.java)
                intent.putExtra("id",idArea)
                intent.putExtra("dato",it.getString("nombre"))
                startActivity(intent)
            }
            .addOnFailureListener { Toast.makeText(this,"Error no hay conexion",Toast.LENGTH_LONG).show()}
    }

}


class Oyente(puntero: MainActivity) : LocationListener {
    var p= puntero
    override fun onLocationChanged(location: Location) {
        p.txtCoordenadas.setText("${location.latitude}\n${location.longitude}")
        p.txtUbicacion.setText("Area Desconocida")
        var geoPosicionGPS = GeoPoint(location.latitude, location.longitude)

        for(item in p.posicion){
            if(item.estoyEn(geoPosicionGPS)){
                p.txtUbicacion.setText("${item.nombre}")
            }
        }
    }
}