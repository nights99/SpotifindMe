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
import android.content.SharedPreferences;

/**
 * Spotify Auth Token Handler.
 * Stores the auth token retrieved from the Spotify API after signing in with a valid user account.
 *
 * TODO: This could probably also handle the whole token refreshing at some point..
 *
 * @see SharedPreferences
 */
public class TokenHandler {

    /** {@link SharedPreferences} name to store the token into */
    private static final String SHARED_PREFS = "spotify.token";
    /** {@link SharedPreferences} key for storing the token value */
    private static final String TOKEN = "token";
    /** {@link SharedPreferences} key for storing the token's expiration timestamp */
    private static final String EXPIRES = "expires";

    /**
     * Store the given auth token information to the {@link SharedPreferences}.
     *
     * @param context Application context
     * @param token Token value
     * @param expiresIn Token expiration value in seconds
     */
    public static void storeToken(Context context, String token, long expiresIn) {
        long expiresAt = System.currentTimeMillis() + (expiresIn * 1000);

        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(TOKEN, token)
                .putLong(EXPIRES, expiresAt)
                .apply();
    }

    /**
     * Retrieves the currently stored auth token from the {@link SharedPreferences}, provided there
     * is one stored and it's still valid.
     *
     * @param context Application context
     * @return Auth token value if available and not expired, {@code null} otherwise
     */
    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        String token = prefs.getString(TOKEN, null);
        long expires = prefs.getLong(EXPIRES, 0L);

        if (token == null || expires < System.currentTimeMillis()) {
            return null;
        }

        return token;
    }
}