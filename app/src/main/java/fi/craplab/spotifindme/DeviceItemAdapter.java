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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.joanzapata.iconify.widget.IconTextView;

import fi.craplab.spotifindme.model.Device;
import fi.craplab.spotifindme.model.Devices;

/**
 * Device Item Adapter to display all available Spotify playback devices.
 */
public class DeviceItemAdapter extends BaseAdapter {
    /** Application context */
    private final Context context;
    /** List of available devices */
    private Devices devices;

    /**
     * Internal item holder object, holding all individual views of the list item layout.
     * See {@code item_device.xml} layout file.
     */
    private static class ItemHolder {
        IconTextView type;
        TextView name;
        IconTextView active;
    }

    /**
     * Creates a new {@code {@link DeviceItemAdapter} with the given application context.
     *
     * @param context Application context
     */
    public DeviceItemAdapter(Context context) {
        this.context = context;
    }

    /**
     * Returns the number of available devices to display
     *
     * @return Number of items to display
     */
    @Override
    public int getCount() {
        if (devices == null || devices.devices == null) {
            return 0;
        }
        return devices.devices.size();
    }

    /**
     * Get the device data for the given position.
     *
     * @param position Item position
     * @return {@link Device} object at the given position if found, {@code null} otherwise
     */
    @Override
    public Object getItem(int position) {
        if (devices == null || devices.devices == null) {
            return null;
        }

        return devices.devices.get(position);
    }

    /**
     * Return the id of the item at the given position. This is just going to be the index of the
     * device in the {@link Devices} list itself.
     *
     * @param position Item position
     * @return Item id
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Creates and returns the {@link View} displaying the device information of the device at the
     * given position. Maps all required data from the {@link Device} at the given position to an
     * internal {@link ItemHolder}, which is then attached to the given {@code convertView}.
     *
     * @param position Item position
     * @param convertView {@link View} to attach the device information to
     * @param parent Parent view
     * @return {@link View} populated with the device data on success, {@code null} otherwise
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (devices == null || devices.devices == null) {
            return null;
        }

        ItemHolder itemHolder;
        Device device = devices.devices.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false);
            itemHolder = new ItemHolder();
            itemHolder.type = convertView.findViewById(R.id.device_type);
            itemHolder.name = convertView.findViewById(R.id.device_name);
            itemHolder.active = convertView.findViewById(R.id.device_active);
            convertView.setTag(itemHolder);
        } else {
            itemHolder = (ItemHolder) convertView.getTag();
        }

        itemHolder.type.setText(getTypeString(device.deviceType));
        itemHolder.name.setText(device.deviceName);

        if (device.isActive) {
            itemHolder.active.setVisibility(View.VISIBLE);
            convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            itemHolder.active.setVisibility(View.INVISIBLE);
            convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        return convertView;
    }

    /**
     * Update the list of devices and notify that data has changed to redraw the view.
     *
     * @param devices List of new devices retrieved from the Spotify API
     * @see MainActivity#getUserDevices()
     */
    public void updateDevices(@NonNull Devices devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    /**
     * Map the device type retrieved from the Spotify API to a matching FontAwesome icon.
     *
     * @param type Device type retrieved from the Spotify API
     * @return Matching FontAwesome icon name
     * @see <a href="https://developer.spotify.com/documentation/web-api/reference/player/get-a-users-available-devices/">
     *     <em>Device Object</em> API description</a>
     * @see <a href="https://fontawesome.com/cheatsheet">List of FontAwesome icons</a>
     */
    private String getTypeString(String type) {
        switch (type) {
            case "Computer":
                return "{fa-desktop}";
            case "Smartphone":
                return "{fa-tablet}";
            case "Speaker":
                return "{fa-headphones}";
        }
        return "{fa-question-circle}";
    }
}
