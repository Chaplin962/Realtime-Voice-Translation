package com.chaplin.realtimevoicetranslation.voice_translation.firebase;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.voice_translation.VoiceTranslationActivity;

public class FirebaseRoomFragment extends Fragment {

    private static final String TAG = "FirebaseRoomFragment";
    private FloatingActionButton firebaseButton;

    private EditText topicEditText;
    private Button joinRoomButton;
    private String username;
    private String language;
    protected VoiceTranslationActivity activity;

    public FirebaseRoomFragment() {
        // Required empty public constructor
    }

    public static FirebaseRoomFragment newInstance() {
        return new FirebaseRoomFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_firebase_room, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString("username", "user");
            language = getArguments().getString("language", "en");

            Log.d(TAG, "Username Received: " + username);
            Log.d(TAG, "language Received: " + language);

        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        topicEditText = view.findViewById(R.id.topicEditText);
        joinRoomButton = view.findViewById(R.id.joinRoomButton);
        firebaseButton = view.findViewById(R.id.button3);

        joinRoomButton.setOnClickListener(v -> {
            String topic = topicEditText.getText().toString().trim();
            if (!topic.isEmpty()) {
                subscribeToTopic(topic);
            } else {
                Toast.makeText(getContext(), "Please enter a room ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (VoiceTranslationActivity) requireActivity();
        firebaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.setFragment(VoiceTranslationActivity.PAIRING_FRAGMENT);
            }
        });

    }

    private void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Subscribed to topic: " + topic);
                Toast.makeText(getContext(), "Joined room: " + topic, Toast.LENGTH_SHORT).show();

                // Create the FirebaseConversationFragment and pass the username and topic
                Bundle args = new Bundle();
                args.putString("username", username);  // Add username to the bundle
                args.putString("topic", topic);        // Add topic to the bundle
                args.putString("language", language);        // Add language to the bundle

                FirebaseConversationFragment firebaseConversationFragment = new FirebaseConversationFragment();
                firebaseConversationFragment.setArguments(args);

                // Replace the current fragment with the conversation fragment
                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, firebaseConversationFragment).addToBackStack(null).commit();

            } else {
                Log.e(TAG, "Failed to subscribe to topic", task.getException());
                Toast.makeText(getContext(), "Failed to join room", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
