package art.arc.beaconscan

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import art.arc.beaconscan.databinding.ActivityMainBinding



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
        if (bleInit()) {
            bleScan(1000L)
        } else {
            binding.perm.text = String.format("%s %s",binding.perm.text, "\n WŁĄCZ BLUETOOTH!!!")
        }
    }

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
     //   for (i in 0..9 step 1) {
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
                scanning = true
                bluetoothLeScanner.startScan(leScanCallback)
            } else {
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
   //     }
       /* if (ActivityCompat.checkSelfPermission(
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
        bluetoothLeScanner.startScan(leScanCallback)*/
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
            if (result?.device?.address == "D0:F0:18:78:03:12" ||
                result?.device?.address == "D0:F0:18:78:03:29" ||
                result?.device?.address == "D0:F0:18:78:03:13" ||
                result?.device?.address == "D0:F0:18:78:03:2A"  )
            binding.log.text = String.format("%s %s ",binding.log.text, "\n ${result.device?.address}  ${result.rssi}  ${result.device.name}  ${result.device.alias}")
            //binding.log.text = String.format("%s ","\n ${result?.device?.address}  ${result?.rssi}  ${result?.device?.name}  ${result?.device?.alias}")
        }
    }
}