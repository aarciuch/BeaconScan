package art.arc.beaconscan

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

const val MSG_SAY_HELLO = 1
const val MSG_GET_DATA = 2
const val MSG_COUNT = 3

class LicznikKrokow : Service(), SensorEventListener {

    //private var binder: IBinder = LicznikKrokowBinder()
    private lateinit var mMessenger : Messenger
    private lateinit var sensorManager: SensorManager
    private lateinit var licznik: Sensor

    private val mGenerator = java.util.Random()

    val randomNumber : Int
        get() = mGenerator.nextInt(100)

    override fun onBind(intent: Intent): IBinder {
        Log.i("SERVIVE", "onBind")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            licznik = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            Toast.makeText(applicationContext, "JEST", Toast.LENGTH_LONG).show()
            sensorManager.registerListener(this, licznik, SensorManager.SENSOR_DELAY_GAME)
        }
        mMessenger = Messenger(IncommingHandler(this))

        return mMessenger.binder
    }

    override fun onCreate() {
        Log.i("SERVIVE", "onCreate")
        super.onCreate()



    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("SERVIVE", "onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("SERVIVE", "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        Log.i("SERVIVE", "onRebind")
        super.onRebind(intent)
    }

    override fun onDestroy() {
        Log.i("SERVIVE", "onDestroy")
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    /*inner class LicznikKrokowBinder : Binder() {
        fun getService() : LicznikKrokow = this@LicznikKrokow
    }*/

    internal class IncommingHandler(
        context: Context,
        private val applicationContext: Context = context.applicationContext) : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_SAY_HELLO ->
                    Toast.makeText(applicationContext, "hello!", Toast.LENGTH_SHORT).show()
                MSG_GET_DATA ->
                     {
                        var intent: Intent = Intent("LICZNIK_KROKOW")
                        intent.putExtra("CZAS", Date().toString())
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    }
                MSG_COUNT ->

                        GlobalScope.launch {
                            delay(1000L)
                            for (i in 1..10) {
                                var intent: Intent = Intent("LICZNIK_KROKOW")
                                intent.putExtra("K", i.toString())
                                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                                delay(500L)
                            }
                        }

                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0 != null) {
           // Toast.makeText(applicationContext, "${p0.values[0]}", Toast.LENGTH_LONG).show()
            Log.i("STEP COUNTER", p0.values[0].toString())
            var intent: Intent = Intent("LICZNIK_KROKOW")
            intent.putExtra("L", p0.values[0].toString())
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }
}