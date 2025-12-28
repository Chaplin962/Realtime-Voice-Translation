package com.chaplin.realtimevoicetranslation.voice_translation.firebase;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        // Log the updated InstanceID token.
        Log.d(TAG, "Refreshed token: " + token);

        // Send the Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Implement this method to send the token to your app server.
        Log.d(TAG, "Sending token to server: " + token);
    }

}
