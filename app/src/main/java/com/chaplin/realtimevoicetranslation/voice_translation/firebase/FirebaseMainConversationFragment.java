package com.chaplin.realtimevoicetranslation.voice_translation.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.tools.services_communication.ServiceCommunicator;
import com.chaplin.realtimevoicetranslation.tools.services_communication.ServiceCommunicatorListener;
import com.chaplin.realtimevoicetranslation.voice_translation.VoiceTranslationFragment;

import java.util.ArrayList;

public class FirebaseMainConversationFragment extends VoiceTranslationFragment {

    private static final String TAG = "FirebaseMainConversationFragment";
    private static final String ARG_TOPIC = "topic";
    private static final String ARG_USERNAME = "username";

    private Button speakBtn;

    private static FirebaseMainConversationFragment instance;
    private String topic;
    private String username;
    private TextView micInput;
    private String language;
    private Handler mHandler = new Handler();

    public FirebaseMainConversationFragment() {
        // Required empty public constructor
    }

    public static FirebaseMainConversationFragment newInstance(String topic, String username, String language) {
        FirebaseMainConversationFragment fragment = new FirebaseMainConversationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOPIC, topic);
        args.putString(ARG_USERNAME, username);
        args.putString("language", language);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        voiceTranslationServiceCommunicator = new FirebaseConversationService.ConversationServiceCommunicator(0);
        voiceTranslationServiceCallback = new VoiceTranslationServiceCallback() {
            @Override
            public void onBluetoothHeadsetConnected() {
                super.onBluetoothHeadsetConnected();
                if (getContext() != null && micInput != null) {
                    micInput.setText(getResources().getString(R.string.btHeadset));
                }
            }

            @Override
            public void onBluetoothHeadsetDisconnected() {
                super.onBluetoothHeadsetDisconnected();
                if (getContext() != null && micInput != null) {
                    micInput.setText(getResources().getString(R.string.mic));
                }
            }
        };

        if (getArguments() != null) {
            topic = getArguments().getString("topic", "topic");
            username = getArguments().getString("username", "user");
            // language = getArguments().getString("language", "en");
            Log.d(TAG, "Username received: " + username + ", language: " + language);
        }

        // Keep a static reference to this instance to update the UI from the service
        instance = this;

        // Set up a listener for new messages
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "view is inflated");
        return inflater.inflate(R.layout.fragment_firebase_conversation, container, false);
    }

    private void voiceInput() {
        //language e.g. "en" for "English", "ur" for "Urdu", "hi" for "Hindi" etc.
        SharedPreferences prefs = getActivity().getSharedPreferences("default", Context.MODE_PRIVATE);
        language = prefs.getString("language", "en");
        username = prefs.getString("user", "user");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

        try {
            voiceInputArl.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final ActivityResultLauncher<Intent> voiceInputArl = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult activityResult) {
            if (activityResult.getResultCode() == Activity.RESULT_OK) {
                ArrayList<String> result = activityResult.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String text = result.get(0);
                FirebaseConversationService.sendMessageToFirebase(text, language);
            }
        }
    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        speakBtn = view.findViewById(R.id.speakBtn);
        speakBtn.setOnClickListener(v -> {
            // Start voice input
            voiceInput();
        });

        Log.d(TAG, "text view is assigned");
        micInput = view.findViewById(R.id.inputMicType);
        microphone.setMicInput(micInput);
        // Subscribe to the topic
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Subscribed to topic: " + topic);
            } else {
                Log.e(TAG, "Failed to subscribe to topic: " + topic, task.getException());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getArguments() != null) {
            if (getArguments().getBoolean("firstStart", false)) {
                getArguments().remove("firstStart");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectToService();
                    }
                }, 300);
            } else {
                connectToService();
            }
        } else {
            connectToService();
        }
    }

    @Override
    protected void connectToService() {
        super.connectToService();
        Log.d(TAG, "connectToService reached");
        Bundle serviceData = new Bundle();
        serviceData.putString("topic", topic);
        serviceData.putString("username", username);
        Log.d(TAG, "connectToConversationService is being called");
        activity.connectToConversationService(serviceData, voiceTranslationServiceCallback, new ServiceCommunicatorListener() {
            @Override
            public void onServiceCommunicator(ServiceCommunicator serviceCommunicator) {
                Log.d(TAG, "onServiceCommunicator reached");
                voiceTranslationServiceCommunicator = (FirebaseConversationService.ConversationServiceCommunicator) serviceCommunicator;
                restoreAttributesFromService();
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                FirebaseMainConversationFragment.this.onFailureConnectingWithService(reasons, value);
            }
        });
        Log.d(TAG, "connectToConversationService was called");
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop called");
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
        activity.disconnectFromConversationService((FirebaseConversationService.ConversationServiceCommunicator) voiceTranslationServiceCommunicator);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Unsubscribed from topic: " + topic);
            } else {
                Log.e(TAG, "Failed to unsubscribe from topic: " + topic, task.getException());
            }
        });
    }
}
