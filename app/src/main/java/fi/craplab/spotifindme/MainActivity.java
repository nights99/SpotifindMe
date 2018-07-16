/*
 * SpotifindMe - Anywhere you go, Spotify will know
 *
 * Copyright (c) 2018 Sven Gregori <sven@craplab.fi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.craplab.spotifindme;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.IOException;
import java.util.Collection;

import fi.craplab.spotifindme.model.Device;
import fi.craplab.spotifindme.model.Devices;
import fi.craplab.spotifindme.model.PlaybackTransfer;
import fi.craplab.spotifindme.model.UserProfile;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Main Activity, doing ..everything I guess.
 * This could probably benefit from some refactoring and splitting it up into several files.
 */
public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    /**
     * Set to {@code true} if you want to get beacon information in the logcat output as it arrives.
     * Prints the beacon's id, rssi and distance.
     * Can be useful for debugging and curiosity, but is also rather noisy in the log
     */
    private static final boolean PRINT_BEACON_INFO = false;

    /** {@link Log} Tag */
    private static final String TAG = MainActivity.class.getSimpleName();

    /** Enable Bluetooth Request id used in {@link #onActivityResult(int, int, Intent)} */
    private static final int REQUEST_ENABLE_BT = 0x10;
    /** Location Permission Request id used in {@link #onActivityResult(int, int, Intent)} */
    private static final int REQUEST_LOCATION_PERMISSION = 0x20;
    /** Key for Spotify auth token in the calling {@link Intent}'s extras */
    static final String EXTRA_TOKEN = "EXTRA_TOKEN";

    /**
     * Spotify playback device names. See {@link #checkSituation()}
     * Currently supports only two devices.
     */
    private static final String[] myDevices = new String[] {
            "Laptop", // change to one of your Spotify device's name
            "Desktop" // change to another Spotify device's name
    };

    /**
     * Available Bluetooth beacon IDs, value is the {@code ID3} field in the beacon advertising
     * data, retrieved by {@link Beacon#getId3()}.
     * Currently support only two beacons.
     */
    private static final int[] myBeacons = new int[] {
            1,
            2
    };

    private String accessToken;
    private SpotifyRestApi spotifyRestApi;
    private Devices devices;
    private DeviceItemAdapter deviceItemAdapter;
    private String currentDevice;
    private boolean transferOngoing;
    private BeaconManager beaconManager;
    private Region beaconRegion;
    private Collection<Beacon> beacons;


    /**
     * {@link Interceptor} to modify each request by adding the auth token to the header.
     */
    private class AuthInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            return chain.proceed(request);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        accessToken = intent.getStringExtra(EXTRA_TOKEN);

        final OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new AuthInterceptor())
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        spotifyRestApi = retrofit.create(SpotifyRestApi.class);

        deviceItemAdapter = new DeviceItemAdapter(this);
        ListView listView = findViewById(R.id.device_list);
        listView.setFocusable(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device device = (Device) parent.getAdapter().getItem(position);
                if (device.isActive) {
                    Log.d(TAG, "Device " + device.deviceName + " already active");
                } else {
                    Log.d(TAG, "Transferring playback to " + device.deviceName);
                    transferPlayback(device);
                }
            }
        });
        listView.setAdapter(deviceItemAdapter);
        initBeaconView();

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "You got no Bluetooth");
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            setupBluetooth();
        }
    }

    /**
     * on Activity resume callback.
     *
     * Checks if we still have a token and goes back to the {@link LoginActivity} if not. If we're
     * all good with the token, we get the user profile and device information from the Spotify API.
     */
    @Override
    protected void onResume() {
        super.onResume();
        String token = TokenHandler.getToken(this);
        if (token == null) {
            Toast.makeText(this, "Auth token expired", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            getUserProfile();
            getUserDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (beaconManager != null) {
            try {
                beaconManager.stopRangingBeaconsInRegion(beaconRegion);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            beaconManager.unbind(this);
            beaconRegion = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Bluetooth enabled now");
                setupBluetooth();
            } else {
                Log.e(TAG, "Bluetooth still not enabled");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted now");
            } else {
                Log.e(TAG, "Location permission still not granted");
            }
            setupBluetooth(); // keep on doing it, we need this permission to work
        }
    }

    /**
     * Set up Bluetooth by either requesting permission for coarse location (cause can't have
     * Bluetooth without that) or by invoking the {@link BeaconManager}. If location permission was
     * requested, {@link #onRequestPermissionsResult(int, String[], int[])} will call this method
     * again, whether permission was granted or not, and the cycle continues.
     */
    private void setupBluetooth() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.bind(this);
        }
    }

    /**
     * Retrieve user profile from the Spotify API through {@link Retrofit}.
     * When data is retrieved, {@link #setUserNameView(UserProfile)} is called with the new data.
     */
    private void getUserProfile() {
        Call<UserProfile> call = spotifyRestApi.getUserProfile();

        call.enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(@NonNull Call<UserProfile> call,
                                   @NonNull Response<UserProfile> response) {
                Log.d(TAG, "got user profile: " + response.toString());
                UserProfile userProfile = response.body();
                if (userProfile != null) {
                    setUserNameView(userProfile);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                Log.e(TAG, "getting user profile failed", t);
            }
        });
    }

    /**
     * Retrieve user devices from the Spotify API through {@link Retrofit}.
     * When data is retrieved, {@link DeviceItemAdapter#updateDevices(Devices)} and
     * {@link #setDevices(Devices)} are called with the new data.
     */
    private void getUserDevices() {
        Call<Devices> call = spotifyRestApi.getDevices();

        call.enqueue(new Callback<Devices>() {
            @Override
            public void onResponse(@NonNull Call<Devices> call,
                                   @NonNull Response<Devices> response) {
                Log.d(TAG, "got devices: " + response.toString());
                Devices devices = response.body();
                if (devices != null) {
                    setDevices(devices);
                    deviceItemAdapter.updateDevices(devices);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Devices> call, @NonNull Throwable t) {
                Log.e(TAG, "getting user devices failed", t);
            }
        });
    }

    /**
     * Transfers playback to the device with the given {@code deviceName} through the Spotify API.
     * Searches all available devices for a device with the given {@code deviceName} as name and
     * calls {@link #transferPlayback(Device)} if it was found. If it wasn't found, nothing will
     * happen. Could throw some exception I guess.
     *
     * @param deviceName Name of the device to transfer playback to
     */
    private void transferPlayback(String deviceName) {
        for (Device device : devices.devices) {
            if (device.deviceName.equals(deviceName)) {
                transferPlayback(device);
                break;
            }
        }
    }

    /**
     * Transfers playback to the given {@code device} through the Spotify API. On success, the
     * updated list of devices is requested via {@link #getUserDevices()} one second after the
     * transfer request's response was received (avoiding some timing issues)
     *
     * @param device Device object to transfer playback to
     */
    private void transferPlayback(Device device) {
        if (transferOngoing) {
            return;
        }
        transferOngoing = true;

        PlaybackTransfer playbackTransfer = new PlaybackTransfer(device.deviceId);
        Call<ResponseBody> call = spotifyRestApi.transferPlayback(playbackTransfer);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull final Response<ResponseBody> response) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "got transfer response: " + response.toString());
                        transferOngoing = false;
                        getUserDevices();
                    }
                }, 1000);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "transferring playback failed", t);
            }
        });
    }

    /**
     * Sets the user name banner to one of the available names from the given {@code userProfile}.
     *
     * @param userProfile Spotify user profile
     */
    private void setUserNameView(UserProfile userProfile) {
        TextView userNameView = findViewById(R.id.user_name);

        if (userProfile.displayName != null) {
            userNameView.setText(getString(R.string.user_name, userProfile.displayName));
        } else if (userProfile.userId != null) {
            userNameView.setText(getString(R.string.user_name, userProfile.userId));
        } else {
            // probably shouldn't happen [TM]
            userNameView.setText(getString(R.string.user_name_unknown));
        }
    }

    /**
     * Internally stores the given {@code devices} in {@link #devices}, and check for an active
     * device, and update {@link #currentDevice} accordingly.
     *
     * @param devices Newly received devices list
     */
    private void setDevices(Devices devices) {
        this.devices = devices;
        for (Device device : devices.devices) {
            if (device.isActive) {
                currentDevice = device.deviceName;
                return;
            }
        }
        currentDevice = null;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconRegion = new Region("someRegion", null, null, null);
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (PRINT_BEACON_INFO) {
                    for (Beacon beacon : beacons) {
                        Log.d(TAG, "found id " + beacon.getId3()
                                + " rssi " + beacon.getRssi()
                                + " distance " + beacon.getDistance());
                    }
                }
                updateBeacons(beacons);
                setBeaconInfo();
                checkSituation();
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(beaconRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the beacon view by setting the beacon IDs.
     *
     * @see #myBeacons
     */
    private void initBeaconView() {
        LinearLayout layout = findViewById(R.id.beacon_info_1);
        TextView textView = layout.findViewById(R.id.beacon_id);
        textView.setText(getString(R.string.beacon_id, myBeacons[0]));

        layout = findViewById(R.id.beacon_info_2);
        textView = layout.findViewById(R.id.beacon_id);
        textView.setText(getString(R.string.beacon_id, myBeacons[1]));
    }

    /**
     * Sets the newly retrieved beacon information from the {@link BeaconManager} internally.
     *
     * @param beacons List of retrieved beacons
     */
    private void updateBeacons(Collection<Beacon> beacons) {
        /*
         * TODO copy data to this.beacons by comparing old values and only update, not overwrite all
         */
        this.beacons = beacons;
    }

    /**
     * Set the new info of previously retrieved {@link Beacon}s in the beacon view.
     * Mainly just sets the distance for each beacon
     */
    private void setBeaconInfo() {
        for (Beacon beacon : beacons) {
            int resId;
            int beaconId = beacon.getId3().toInt();

            if (beaconId == myBeacons[0]) {
                resId = R.id.beacon_info_1;
            } else if (beaconId == myBeacons[1]) {
                resId = R.id.beacon_info_2;
            } else {
                continue;
            }

            LinearLayout layout = findViewById(resId);
            TextView distanceView = layout.findViewById(R.id.beacon_distance);
            distanceView.setText(getString(R.string.beacon_distance, beacon.getDistance()));
        }
    }

    /**
     * Checks the general situation with the beacons and playback and transfers if necessary the
     * playback on the other device.
     *
     * This is a very basic implementation at this point. Follows very simple rules based on
     * not so properly calibrated, arbitrarily located beacons. Transitioning is therefore not the
     * smoothest with this. But it works, and this is proof-of-concept. Concept proven.
     *
     * @see #myBeacons
     * @see #myDevices
     */
    private void checkSituation() {
        Beacon id1 = null; // laptop, if far, switch to myDevices[0]
        Beacon id2 = null; // rpi

        if (currentDevice == null) {
            return;
        }

        for (Beacon beacon : beacons) {
            int beaconId = beacon.getId3().toInt();

            if (beaconId == myBeacons[0]) {
                id1 = beacon;
            } else if (beaconId == myBeacons[1]) {
                id2 = beacon;
            }
        }

        if (id1 == null && id2 == null) {
            Log.w(TAG, "no beacons");
            return;
        }

        if (id1 == null || id2 == null) {
            /*
             * FIXME if only one beacon is sent, it might kill the playback.
             * should keep track of the last state and use that (with expiration)
             * TODO updateBeacons() should set the beacon data accordingly
             */
            return;
        }

        double d1 = id1.getDistance();
        double d2 = id2.getDistance();

        /*
         * FIXME this basically "just works", at least sometimes.
         * Needs a lot more tweaking for smoother transitioning
         */

        if (d1 > 2.0 && d2 < 5.0 && currentDevice.equals(myDevices[0])) {
            transferPlayback(myDevices[1]);
        } else if (d1 < 1.5 && currentDevice.equals(myDevices[1])) {
            transferPlayback(myDevices[0]);
        }
    }
}
