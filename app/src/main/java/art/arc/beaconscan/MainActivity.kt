package art.arc.beaconscan

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.icu.util.LocaleData
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo.Scope
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import art.arc.beaconscan.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Date


class MainActivity : AppCompatActivity() {


    private val PERMISSION_REQUEST_FINE_LOCATION = 1
    private val PERMISSION_REQUEST_BACKGROUND_LOCATION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 3
    private val PERMISSION_REQUEST_BLUETOOTH_ADMIN = 4
    private val PERMISSION_REQUEST_BLUETOOTH_SCAN = 5

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val hander = Handler(Looper.getMainLooper())
    private var czyszcenie = 0
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")



//    private val requestBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//            it ->
//        run {
//            if (it.resultCode == Activity.RESULT_OK) {
//                it.data?.dataString?.let { it1 -> Log.i("BLUETOOTH", it1) }
//            }
//        }
//    }


//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
//            isGranted: Boolean ->
//            if (isGranted) {
//                Toast.makeText(applicationContext, "GRANTED", Toast.LENGTH_SHORT).show()
//
//            }   else {
//                Toast.makeText(applicationContext, "NOT GRANTED", Toast.LENGTH_SHORT).show()
//            }
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        init()

        binding.startReceiveUDP.setOnClickListener {
            /*object: Thread() {
                override fun run() {
                    while (true) {
                        val s = String(receiveUDP(), Charsets.UTF_8)
                    }
                }
            }.start()*/
            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    val s = String(receiveUDP(), Charsets.UTF_8)
                    Log.i("UDP", s)
                    withContext(Dispatchers.Main) {
                        binding.recevedMsg.setText(s)
                    }

                }
            }
        }

        binding.sendUDP.setOnClickListener {
            val ip = binding.ipAddress.text.toString()
            val port = binding.ipPort.text.toString().toInt()
            val msg = binding.textToSend.text.toString() + '\n'
            sendViaUDP(ip, port,msg)
            Log.i("UDP", "Sending ...$ip, $port, $msg")
        }


        if (bleInit()) {
            bleScan(10000L)
            bleScan1()
        } else {
            binding.perm.text = String.format("%s %s",binding.perm.text, "\n WŁĄCZ BLUETOOTH!!!")
        }

        binding.stepCountStartStop.setOnClickListener {
            //getRandomFroemLicznikKorokow()
            sendMessage(MSG_SAY_HELLO)
            sendMessage(MSG_GET_DATA)
            sendMessage(MSG_COUNT)
        }
    }


//**************** service Licznik kroków start ******************************//


    //private lateinit var mLicznikKrokow: LicznikKrokow
    private var mLicznikKrokowBound: Boolean = false
    private var mLicznikKrokow2 : Messenger? = null
    private var broadcastReceiver : MessageReceiver = MessageReceiver()


    inner class MessageReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
           val s = p1?.getStringExtra("CZAS")
           binding.stepCountStatus.text = s.toString()
        }

    }

    private val mConnection2LicznikKrokow = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
          //  val binder = p1 as LicznikKrokow.LicznikKrokowBinder
           //mLicznikKrokow = binder.getService()
            mLicznikKrokow2 = Messenger(p1)
            mLicznikKrokowBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            //mLicznikKrokowBound = false
            mLicznikKrokow2 = null
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, LicznikKrokow::class.java).also {
            bindService(it, mConnection2LicznikKrokow, Context.BIND_AUTO_CREATE)
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver,
            IntentFilter("LICZNIK_KROKOW"))
    }

    override fun onStop() {
        super.onStop()
        if (mLicznikKrokowBound) {
            unbindService(mConnection2LicznikKrokow)
            mLicznikKrokowBound = false
        }
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver)
    }

   /* private fun getRandomFroemLicznikKorokow() {
        if (mLicznikKrokowBound) {
            val num: Int = mLicznikKrokow.randomNumber
            binding.stepCountStatus.text = num.toString()
        }
    }*/

    fun sendMessage(a: Int) {
        if (!mLicznikKrokowBound) return
        var msg : Message? = null
        when (a) {

        MSG_SAY_HELLO ->
                {
                    msg = Message.obtain(null, MSG_SAY_HELLO, 0, 0)
                }
        MSG_GET_DATA ->
            {
                msg = Message.obtain(null, MSG_GET_DATA, 0, 0)
            }
        MSG_COUNT ->
            {
                 msg = Message.obtain(null, MSG_COUNT, 0, 0)
            }
        }
        try {
            mLicznikKrokow2?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

//**************** service Licznik kroków stop ******************************//

    /****************************** UPRAWNIENIA START ******************************************/
    private fun init() {
        binding.perm.text = String.format("%s %s %d",binding.perm.text, "\n API = ", Build.VERSION.SDK_INT)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("BLUETOOTH", "ACCESS_FINE_LOCATION GRANTED")
            binding.perm.text = String.format("%s %s",binding.perm.text, "\n ACCESS_FINE_LOCATION GRANTED")
        }
        else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_FINE_LOCATION)
        //    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            Log.i("BLUETOOTH", "ACCESS_FINE_LOCATION AFTER REQUEST")

        }

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("BLUETOOTH", "ACCESS_BACKGROUND_LOCATION GRANTED")
            binding.perm.text = String.format("%s %s",binding.perm.text, "\n ACCESS_BACKGROUND_LOCATION GRANTED")
        }
        else {


            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                            PERMISSION_REQUEST_BACKGROUND_LOCATION)
        //    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            Log.i("BLUETOOTH", "ACCESS_BACKGROUND_LOCATION AFTER REQUEST")


        }

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("BLUETOOTH", "BLUETOOTH GRANTED")
            binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH GRANTED")
        }
        else {


            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH),
                PERMISSION_REQUEST_BLUETOOTH)
        //    requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH)
            Log.i("BLUETOOTH", "BLUETOOTH AFTER REQUEST")

        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("BLUETOOTH", "BLUETOOTH_ADMIN GRANTED")
            binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH_ADMIN GRANTED")
        }
        else {


            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_ADMIN),
                PERMISSION_REQUEST_BLUETOOTH_ADMIN)
        //    requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_ADMIN)
            Log.i("BLUETOOTH", "BLUETOOTH_ADMIN AFTER REQUEST")

        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("BLUETOOTH", "BLUETOOTH_SCAN GRANTED")
            binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH_SCAN GRANTED")
        }
        else {

            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_REQUEST_BLUETOOTH_SCAN)
          //  requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_SCAN)
            Log.i("BLUETOOTH", "BLUETOOTH_SCAN AFTER REQUEST")

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.perm.text = String.format("%s %s",binding.perm.text, "\n ACCESS_FINE_LOCATION AFTER REQUEST -> GRANTED")
                }
            }
            PERMISSION_REQUEST_BACKGROUND_LOCATION -> {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.perm.text = String.format("%s %s",binding.perm.text,"\n ACCESS_BACKGROUND_LOCATION AFTER REQUEST -> GRANTED")
                }
            }
            PERMISSION_REQUEST_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH AFTER REQUEST -> GRANTED")
                }
            }
            PERMISSION_REQUEST_BLUETOOTH_ADMIN -> {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH_ADMIN AFTER REQUEST -> GRANTED")
                }
            }
            PERMISSION_REQUEST_BLUETOOTH_SCAN -> {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH_SCAN AFTER REQUEST -> GRANTED")
                }
            }
            else -> {

            }
        }
    }

    val launchBluetoothAdapterIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        it ->
            if (it.resultCode == Activity.RESULT_OK) {
                binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH ON")
            }
    }

    private fun bleInit() : Boolean {
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            launchBluetoothAdapterIntent.launch(intent)
        } else {
            binding.perm.text = String.format("%s %s",binding.perm.text,"\n BLUETOOTH ON")

        }
        return bluetoothAdapter.isEnabled
    }

    private fun bleScan(period: Long) {
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        var scanning = false
        if (!scanning) {
                hander.postDelayed({
                    scanning = false
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        //return
                    }
                //   bluetoothLeScanner.stopScan(leScanCallback)
                }, period)
                scanning = false
                bluetoothLeScanner.startScan(leScanCallback)
            } else {
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
    }

    private fun bleScan1() {
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(
               applicationContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        }
        bluetoothLeScanner.startScan(leScanCallback)
    }
    private val leScanCallback : ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
               // return
            }
            czyszcenie ++
            if (czyszcenie == 100) {
                binding.log.text = ""
                czyszcenie = 0
            }
            if (binding.wszystkie.isChecked) {
                binding.log.text = String.format( "%s %s ",binding.log.text,"\n ${result?.device?.address}  ${result?.rssi}  ${result?.device?.name}  ${result?.device?.alias}")
            }
            if (result?.device?.address == "D0:F0:18:78:03:12")
                 binding.log1.text = String.format("%s ","${result.device?.address}  ${result.rssi}  ${result.device.name}  ${result.device.alias}  ${LocalDateTime.now().format(formatter)}")
            if (result?.device?.address == "D0:F0:18:78:03:29")
                binding.log2.text = String.format("%s ","${result.device?.address}  ${result.rssi}  ${result.device.name}  ${result.device.alias}  ${LocalDateTime.now().format(formatter)}")
            if (result?.device?.address == "D0:F0:18:78:03:13")
                binding.log3.text = String.format("%s ","${result.device?.address}  ${result.rssi}  ${result.device.name}  ${result.device.alias}  ${LocalDateTime.now().format(formatter)}")
            if (  result?.device?.address == "D0:F0:18:78:03:2A" )
                binding.log4.text = String.format("%s ","${result.device?.address}  ${result.rssi}  ${result.device.name}  ${result.device.alias}  ${LocalDateTime.now().format(formatter)}")

                //binding.log.text = String.format("%s ","\n ${result?.device?.address}  ${result?.rssi}  ${result?.device?.name}  ${result?.device?.alias}")
        }
    }

    private fun sendViaUDP(ip: String, port: Int, data: String) {
        try {
            val socket = DatagramSocket()
            val socket_MATLAB = DatagramSocket()
            socket.broadcast = true
            val sendData = data.toByteArray(Charsets.UTF_8)
            val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getLoopbackAddress(), port)
            val sendPacket_MATLAB = DatagramPacket(sendData, sendData.size, InetAddress.getByName(ip), port)
            GlobalScope.launch {
                Log.i("UDP", "${sendData.size}")
                socket.send(sendPacket)
                socket_MATLAB.send(sendPacket_MATLAB)
            }

        } catch (e: Exception) {
            e.message?.let { Log.e("UDP", it) }
        }
    }

    fun receiveUDP(): ByteArray {
        var ret = ByteArray(24)
        var socket: DatagramSocket? = null
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = DatagramSocket(8888)
           // socket.broadcast = true
            val Buffer = ByteArray(24)
            val packet = DatagramPacket(Buffer, Buffer.size)
            Log.i("UDP", "Receiving ...")
            socket.receive(packet)
            ret = packet.data
            Log.i("UDP", "${packet.address} : ${String(packet.data, Charsets.UTF_8)}")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        return ret
    }
}



















