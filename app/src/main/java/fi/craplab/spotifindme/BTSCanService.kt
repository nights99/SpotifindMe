package fi.craplab.spotifindme

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
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
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            TODO need to only send pause when not already paused, change icon,
//             do we need to send play as well as transfer? (autoplay set to true, so shouldn't?)
            with(result.device) {
                Log.i(
                    "ScanCallback",
                    "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, rssi: ${result.rssi}"
                )
            }
            if ((result.device.name == "BLAST")) {
                if ((result.rssi > -70)) {
                    if (result.device.name != foundDevice) {
//                        TODO need to change to have service control playback
                        EventBus.getDefault().post(DeviceMsg(result.device.name, true))
                        foundDevice = result.device.name
                    }
                } else {
                    EventBus.getDefault().post(DeviceMsg(result.device.name, false))
                    foundDevice = null

                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.i(TAG, "Scan error: $errorCode")
            super.onScanFailed(errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Log.i(TAG, "Scan batch results")
            super.onBatchScanResults(results)
            results?.forEach { i ->
                Log.i(TAG, "Scan batch results: ${i.device.name}")
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
    private val filters = arrayListOf<ScanFilter>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "service onStartCommand")
//        TODO Add filter - no filter gets killed on screen off
//        For some reason, filter by name doesn't work, but MAC addr does.
//        filters.add(ScanFilter.Builder().setDeviceName("BLAST").build())
        filters.add(ScanFilter.Builder().setDeviceAddress("EC:81:93:11:C4:41").build())
        bleScanner.startScan(filters, scanSettings, scanCallback)
        return START_STICKY
    }

    data class DeviceMsg (val name: String, val found: Boolean)

}