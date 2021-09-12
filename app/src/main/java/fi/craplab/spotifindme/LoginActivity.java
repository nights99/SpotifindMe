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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

/**
 * Initial activity, checks if we have a valid Spotify auth token stored and either proceeds to the
 * {@link MainActivity} or waits for the user to sign up with their Spotify account to retrieve a
 * new auth token (and then proceeds to the {@link MainActivity}).
 */
public class LoginActivity extends AppCompatActivity {
    /** {@link Log} Tag */
    private static final String TAG = LoginActivity.class.getSimpleName();

    /** SpotifindMe client ID, used to identify the application with Spotify itself */
    private static final String CLIENT_ID = "b8b6373e77414ae181cd6446db6b595d";
    /** Spotify auth redirect URL */
    private static final String REDIRECT_URI = "spotifindme://redirect";
    /** Spotify Auth Request identifier used in {@link #onActivityResult(int, int, Intent)} */
    private static final int REQUEST_SPOTIFY_AUTH = 0x10;
    /**
     * List of Spotify auth scopes to have access to the required user data
     * Required scopes for this application are
     * <ul>
     *     <li>{@code user-read-playback-state} to get the available Spotify devices</li>
     *     <li>{@code user-modify-playback-state} to transfer the playback to another device</li>
     * </ul>
     * @see <a href="https://developer.spotify.com/documentation/general/guides/scopes/">
     *     Spotify Authorization Scopes</a>
     */
    private static final String[] authScopes = new String[] {
            "user-read-playback-state",
            "user-modify-playback-state"
    };

    /**
     * {@inheritDoc}
     * <p>
     * Checks if we have a valid token and either proceeds straight to the {@link MainActivity}, or
     * waits for the user to sign up with their Spotify account.
     * </p>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String token = TokenHandler.getToken(this);

        if (token == null) {
            setContentView(R.layout.activity_login);
        } else {
            Toast.makeText(this, R.string.spotify_token_found, Toast.LENGTH_SHORT).show();
            startMainActivity(token);
        }
    }

    /**
     * Spotify login button onClick handler, sends the authentication request to Spotify.
     *
     * @param view The Button associated with this onClick handler,
     *             see {@code activity_login.xml} layout file
     */
    public void onLoginButtonClicked(View view) {
        System.out.println("View == " + view.toString() + " tag " + view.getTag() + " id " + view.getId());
        final AuthorizationRequest request = new AuthorizationRequest
                .Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(authScopes)
                .build();

        AuthorizationClient.openLoginActivity(this, REQUEST_SPOTIFY_AUTH, request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves and handles the result from the Spotify auth request initiated in
     * {@link #onLoginButtonClicked(View)}. Best case scenario, we have a valid auth token now which
     * we pass to our {@link TokenHandler} for storage, and proceed to the {@link MainActivity}.
     * </p>
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode != REQUEST_SPOTIFY_AUTH) {
            // nothing to do here
            return;
        }

        AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

        switch (response.getType()) {
            case TOKEN:
                Log.d(TAG, "Auth success: " + response.getAccessToken());
                TokenHandler.storeToken(this,
                        response.getAccessToken(),
                        response.getExpiresIn());

                startMainActivity(response.getAccessToken());
                break;

            case ERROR:
                Toast.makeText(this,
                        getString(R.string.spotify_auth_error, response.getError()),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Auth error: " + response.getError());
                break;

            default:
                Log.w(TAG, "Auth response: " + response.getType());
        }
    }

    /**
     * Switch to the {@link MainActivity} and pass the Spotify auth token to it.
     *
     * @param token Auth token received from Spotify
     */
    private void startMainActivity(String token) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TOKEN, token);
        startActivity(intent);
        finish();
    }
}
