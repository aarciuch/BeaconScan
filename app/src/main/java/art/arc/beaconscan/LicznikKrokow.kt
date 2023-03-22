package art.arc.beaconscan

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*

const val MSG_SAY_HELLO = 1
const val MSG_GET_DATA = 2
const val MSG_COUNT = 3

class LicznikKrokow : Service() {

    //private var binder: IBinder = LicznikKrokowBinder()
    private lateinit var mMessenger : Messenger

    private val mGenerator = java.util.Random()

    val randomNumber : Int
        get() = mGenerator.nextInt(100)

    override fun onBind(intent: Intent): IBinder {
        Log.i("SERVIVE", "onBind")
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
}