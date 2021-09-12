package fi.craplab.spotifindme

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import org.greenrobot.eventbus.EventBus

class BTSCanService : Service() {
    private var foundDevice: Any? = null
    @Suppress("PrivatePropertyName")
    private val Any.TAG: String
        get() {
            val tag = javaClass.simpleName
            return if (tag.length <= 23) tag else tag.substring(0, 23)
        }



    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "service onBind")
        TODO("Return the communication channel to the service.")
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // From the previous section:
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // TODO change to low power
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            TODO need to only send pause when not already paused, change icon, do we need to send play as well as transfer?
            with(result.device) {
                Log.i(
                    "ScanCallback",
                    "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, rssi: ${result.rssi}"
                )
            }
            if ((result.device.name == "BLAST")) {
                if ((result.rssi > -70)) {
                    if (result.device.name != foundDevice) {
                        EventBus.getDefault().post(DeviceMsg(result.device.name, true))
                        foundDevice = result.device.name
                    }
                } else {
                    EventBus.getDefault().post(DeviceMsg(result.device.name, false))
                    foundDevice = null

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel("channel1", "spotifind notifications", NotificationManager.IMPORTANCE_DEFAULT)
        )
        startForeground(1,
            Notification.Builder(this, "foo")
                .setContentTitle("title")
                .setContentText("context")
                .setChannelId("channel1")
                .setTicker("ticker_text")
            .build())
//        TODO add exit button
        Log.d(TAG, "service start")
        //bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "service destroy")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "service onStartCommand")
//        TODO Add filter - no filter gets killed on screen off
        bleScanner.startScan(null, scanSettings, scanCallback)
        return START_STICKY
    }

    data class DeviceMsg (val name: String, val found: Boolean)

}