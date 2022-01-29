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
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.StrictMode
import android.widget.Toast
import java.lang.Exception


class BTSCanService : Service() {
    private var bcReceiver: BTSCanService.StopServerBroadcast? = null
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
        bluetoothAdapter?.bluetoothLeScanner
    }

    // From the previous section:
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        } catch (e: NullPointerException) {
            null
        }
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
        StrictMode.enableDefaults()
        Log.d(TAG, "${Build.VERSION.SDK_INT} >= ${Build.VERSION_CODES.O}")
//        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
//            NotificationChannel("channel1", "spotifind notifications", NotificationManager.IMPORTANCE_DEFAULT)
//        )
        val nfnChannel = NotificationChannel("channel1", "spotifind notifications",
            NotificationManager.IMPORTANCE_LOW)
        nfnChannel.setSound(null, null)
        getSystemService(NotificationManager::class.java).createNotificationChannel(nfnChannel)
        try {
            startForeground(
                1,
                Notification.Builder(this, "channel1")
                    .setContentTitle("SpotifindMe player")
                    .setContentText("")
//                    .setChannelId("channel1")
                    .setSmallIcon(
                        Icon.createWithResource(
                            applicationContext,
                            R.drawable.nfn_icon
                        )
                    ) //absolutely must have the above apparently...
                    .setTicker("ticker_text")
//                    .setPriority(Notification.PRIORITY_MAX)
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(applicationContext, R.drawable.nfn_icon),
                            "Exit",
                            PendingIntent.getBroadcast(
                                this, 1,
//                                Intent(this, StopServerBroadcast::class.java)
                                Intent("foo-ignored")
//                                    .setAction("foo-ignored")
                                    .putExtra(
                                    "action",
                                    "actionName"
                                )
                                ,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        ).build()
                    ).setOngoing(true)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(TAG, e)

        }

        Log.d(TAG, "service start")
        //bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "service destroy")
        this.unregisterReceiver(this.bcReceiver)
    }

    private val filters = arrayListOf<ScanFilter>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "service onStartCommand")
        this.bcReceiver = StopServerBroadcast()
        this.registerReceiver(this.bcReceiver, IntentFilter("foo-ignored"))
//        For some reason, filter by name doesn't work, but MAC address does.
//        filters.add(ScanFilter.Builder().setDeviceName("BLAST").build())
        filters.add(ScanFilter.Builder().setDeviceAddress("EC:81:93:11:C4:41").build())
        bleScanner?.startScan(filters, scanSettings, scanCallback)
        return START_STICKY
    }

    data class DeviceMsg(val name: String, val found: Boolean)

    inner class StopServerBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
            val action = intent.getStringExtra("action")
            Log.d(TAG, "Broadcast: action $action")
            if (action == "actionName") {
                this@BTSCanService.stopSelf()
            }
        }
    }
}
