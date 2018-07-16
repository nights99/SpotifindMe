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
package fi.craplab.spotifindme.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Playback Transfer API Model.
 *
 * @see <a href="https://developer.spotify.com/documentation/web-api/reference/player/transfer-a-users-playback/">
 *     <em>Transfer User's Playback</em> API description</a>
 */
public class PlaybackTransfer {
    @SerializedName("device_ids")
    public List<String> deviceIds;

    @SerializedName("play")
    public boolean autoPlayOnTransfer;

    public PlaybackTransfer(String deviceId) {
        this(deviceId, true);
    }

    public PlaybackTransfer(String deviceId, boolean autoPlay) {
        this.deviceIds = new ArrayList<>(1);
        this.deviceIds.add(deviceId);
        this.autoPlayOnTransfer = autoPlay;
    }
}
