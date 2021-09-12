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

import fi.craplab.spotifindme.model.Devices;
import fi.craplab.spotifindme.model.PlaybackTransfer;
import fi.craplab.spotifindme.model.UserProfile;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.Call;
import retrofit2.http.PUT;

/**
 * Retrofit REST API description
 *
 * @see <a href="http://square.github.io/retrofit/">Retrofit website</a>
 */
public interface SpotifyRestApi {
    /**
     * {@code GET} request to retrieve the current user's profile information.
     *
     * @return {@link UserProfile} retrieved from the Spotify API
     * @see <a href="https://developer.spotify.com/documentation/web-api/reference/users-profile/get-current-users-profile/">
     *     <em>Get Current User's Profile</em> API description</a>
     */
    @GET("/v1/me")
    Call<UserProfile> getUserProfile();

    /**
     * {@code GET} request to retrieve the user's available devices
     *
     * @return {@link Devices} retrieved from the Spotify API
     * @see <a href="https://developer.spotify.com/documentation/web-api/reference/player/get-a-users-available-devices/">
     *     <em>Get a User's available devices</em> API description</a>
     */
    @GET("/v1/me/player/devices")
    Call<Devices> getDevices();

    /**
     * {@code PUT} request to transfer the playback to another device
     *
     * @param body {@link PlaybackTransfer} information
     * @return {@link ResponseBody} retrieved from the Spotify API call
     * @see <a href="https://developer.spotify.com/documentation/web-api/reference/player/transfer-a-users-playback/">
     *     <em>Transfer a User's Playback</em> API description</a>
     */
    @PUT("/v1/me/player")
    Call<ResponseBody> transferPlayback(@Body PlaybackTransfer body);

    @PUT("/v1/me/player/pause")
    Call<ResponseBody> pausePlayback(@Body String device_id);
}
