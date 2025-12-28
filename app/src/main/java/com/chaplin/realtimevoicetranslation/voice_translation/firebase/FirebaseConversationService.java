package com.chaplin.realtimevoicetranslation.voice_translation.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.chaplin.realtimevoicetranslation.Global;
import com.chaplin.realtimevoicetranslation.bluetooth.Peer;
import com.chaplin.realtimevoicetranslation.tools.CustomLocale;
import com.chaplin.realtimevoicetranslation.tools.Tools;
import com.chaplin.realtimevoicetranslation.voice_translation.VoiceTranslationService;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.ConversationMessage;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.NeuralNetworkApiText;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.translation.Translator;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.voice.Recognizer;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.voice.RecognizerListener;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.voice.Recorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirebaseConversationService extends VoiceTranslationService {
    public static final int SPEECH_BEAM_SIZE = 4;
    public static final int TRANSLATOR_BEAM_SIZE = 1;

    private String textRecognized = "";
    private Translator translator;
    private Recognizer mVoiceRecognizer;
    private RecognizerListener mVoiceRecognizerCallback;
    private Global global;
    private static DatabaseReference mRootReference;
    private static DatabaseReference mMessagesReference;
    private static String topic;
    private static String username;
    private static Handler mHandler = new Handler();
    private Handler mainHandler;
    static String TAG = "FirebaseConversationService";
    private static ChildEventListener mMessagesListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        if (intent != null && intent.getExtras() != null) {
            topic = intent.getStringExtra("topic");
            username = intent.getStringExtra("username");
            Log.d(TAG, "Received topic: " + topic + ", username: " + username);
            mRootReference = FirebaseDatabase.getInstance().getReference();

            if (topic != null) {
                mMessagesReference = mRootReference.child("messages").child(topic);

                // Initialize Firebase Listener for receiving messages
                initializeFirebaseListener();
                mMessagesReference.addChildEventListener(mMessagesListener);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        global = (Global) getApplication();
        mainHandler = new Handler(Looper.getMainLooper());

        mVoiceCallback = new Recorder.Callback() {
            @Override
            public void onVoiceStart() {
                if (mVoiceRecognizer != null) {
                    super.onVoiceStart();
                    Log.e("recorder", "onVoiceStart");
                    //we notify the client
                    FirebaseConversationService.super.notifyVoiceStart();
                }
            }

            @Override
            public void onVoice(@NonNull float[] data, int size) {
                if (mVoiceRecognizer != null) {
                    super.onVoice(data, size);
                    global.getLanguage(true, new Global.GetLocaleListener() {
                        @Override
                        public void onSuccess(CustomLocale result) {
                            int sampleRate = getVoiceRecorderSampleRate();
                            if (sampleRate != 0) {
                                mVoiceRecognizer.recognize(data, SPEECH_BEAM_SIZE, result.getCode());
                            }
                        }

                        @Override
                        public void onFailure(int[] reasons, long value) {
                            FirebaseConversationService.super.notifyError(reasons, value);
                        }
                    });
                }
            }

            @Override
            public void onVoiceEnd() {
                if (mVoiceRecognizer != null) {
                    super.onVoiceEnd();
                    Log.e("recorder", "onVoiceEnd");
                    // if the textRecognizer is not empty then it means that we have a result that has not been correctly recognized as final
                    if (!textRecognized.equals("")) {
                        textRecognized = "";
                    }
                    // the client is notified
                    FirebaseConversationService.super.notifyVoiceEnd();
                }
            }
        };

        clientHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(final android.os.Message message) {
                int command = message.getData().getInt("command", -1);
                final String text = message.getData().getString("text");
                if (command != -1) {
                    if (!FirebaseConversationService.super.executeCommand(command, message.getData())) {
                        switch (command) {
                            case RECEIVE_TEXT:
                                global.getLanguage(true, new Global.GetLocaleListener() {
                                    @Override
                                    public void onSuccess(CustomLocale language) {
                                        if (text != null) {
                                            // send the message
                                            Log.d(TAG, "handleMessage.onSuccess is called");

                                            //sendMessage(new ConversationMessage(new NeuralNetworkApiText(text, language)));
                                        }
                                    }

                                    @Override
                                    public void onFailure(int[] reasons, long value) {
                                        FirebaseConversationService.super.notifyError(reasons, value);
                                    }
                                });
                                break;
                        }
                    }
                }
                return false;
            }
        });

        // Speech recognition and translation initialization
        translator = global.getTranslator();
        mVoiceRecognizer = global.getSpeechRecognizer();
        mVoiceRecognizerCallback = new VoiceTranslationServiceRecognizerListener() {
            @Override
            public void onSpeechRecognizedResult(String text, String languageCode, double confidenceScore, boolean isFinal) {
                Log.d(TAG, "onSpeechRecognizedResult is called");
                if (text != null && languageCode != null && !text.equals("") && !isMetaText(text)) {
                    CustomLocale language = CustomLocale.getInstance(languageCode);
                    if (isFinal) {
                        textRecognized = "";  // to ensure that we continue to listen since in this case the result is automatically extracted
                        // send the message
                        sendMessage(new ConversationMessage(new NeuralNetworkApiText(text, language)));
                    } else {
                        textRecognized = text;  // if it equals something then when calling voiceEnd we stop recognition
                    }
                }
            }

            @Override
            public void onError(int[] reasons, long value) {
                FirebaseConversationService.super.notifyError(reasons, value);
            }
        };

        if (global == null) {
            Log.e(TAG, "global is null");
        }

        if (mVoiceRecognizer != null) {
            mVoiceRecognizer.addCallback(mVoiceRecognizerCallback);
        } else {
            Log.e(TAG, "mVoiceRecognizer is null, cannot add callback");
        }
        initializeVoiceRecorder();
    }

    private void sendMessage(ConversationMessage conversationMessage) {
        Log.d(TAG, "sendMessage called");
        String languageCode = conversationMessage.getPayload().getLanguage().getCode();
        sendMessageToFirebase(conversationMessage.getPayload().getText(), languageCode);
    }

    @Override
    public void initializeVoiceRecorder() {
        Log.d(TAG, "initializeVoiceRecorder");
        if (Tools.hasPermissions(this, REQUIRED_PERMISSIONS)) {
            //voice recorder initialization
            Log.d(TAG, "initializeVoiceRecorder has permissions");
            super.mVoiceRecorder = new Recorder((Global) getApplication(), false, mVoiceCallback, new FirebaseConversationService.BluetoothHeadsetCallback());
        }
    }

    public class BluetoothHeadsetCallback {

        public void onHeadsetConnected() {
        }

        public void onScoAudioConnected() {
            Bundle bundle = new Bundle();
            bundle.putInt("callback", ON_CONNECTED_BLUETOOTH_HEADSET);
            notifyToClient(bundle);
        }

        public void onScoAudioDisconnected() {
            Bundle bundle = new Bundle();
            bundle.putInt("callback", ON_DISCONNECTED_BLUETOOTH_HEADSET);
            notifyToClient(bundle);
        }

        public void onHeadsetDisconnected() {
        }
    }

    private String lastMessage;

    private void initializeFirebaseListener() {
        mMessagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildAdded: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildChanged: " + dataSnapshot.getKey());

                if (!username.equals(dataSnapshot.getKey())) {
                    Long latestTime = Long.MIN_VALUE;
                    DataSnapshot latestMessageSnapshot = null;

                    // Directly retrieve the message from the snapshot
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        Long time = messageSnapshot.child("time").getValue(Long.class);
                        if (time != null && time > latestTime) {
                            latestTime = time;
                            latestMessageSnapshot = messageSnapshot;
                        }
                    }

                    // Check if a latest message was found and handle it
                    if (latestMessageSnapshot != null) {
                        String message = latestMessageSnapshot.child("message").getValue(String.class);
                        String language = latestMessageSnapshot.child("language").getValue(String.class);
                        String from = latestMessageSnapshot.child("from").getValue(String.class);

                        if (message != null && language != null) {
                            if (!message.equals(lastMessage)) {
                                lastMessage = message;
                                Toast.makeText(getApplicationContext(), "Currently Speaking: " + from, Toast.LENGTH_SHORT).show();
                                handleReceivedMessage(message, language);
                            }
                        } else {
                            Log.d(TAG, "No valid message found");
                        }
                    } else {
                        Log.d(TAG, "No messages found.");
                    }
                } else {
                    Log.d(TAG, "Current user's message received");
                }

            }


            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved: " + dataSnapshot.getKey());
                // Handle message removal if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildMoved: " + dataSnapshot.getKey());
                // Handle message moves if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        };
    }

    public static void sendMessageToFirebase(String message, String languageCode) {
        Log.d(TAG, "sendMessageToFirebase called");
        if (!message.isEmpty()) {
            String topicRef = "messages/" + topic + "/" + username;

            DatabaseReference userMessagePush = mRootReference.child("messages").child(username).child(topic).push();

            String pushId = userMessagePush.getKey();

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", username);
            messageMap.put("language", languageCode);

            Map<String, Object> messageUserMap = new HashMap<>();
            messageUserMap.put(topicRef + "/" + pushId, messageMap);

            mRootReference.updateChildren(messageUserMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Sent message successfully");

                } else {
                    Log.e(TAG, "Failed to send message", task.getException());
                }
            });
        }
        mMessagesReference.addChildEventListener(mMessagesListener);
    }

    private void handleReceivedMessage(String message, String language) {
        Log.d("FirebaseConversationService", "Received message: " + message);
        global.getLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {

                ConversationMessage conversationMessage = new ConversationMessage(null, new NeuralNetworkApiText(message, CustomLocale.getInstance(language)));
                translator.translateMessage(conversationMessage, result, TRANSLATOR_BEAM_SIZE, new Translator.TranslateMessageListener() {
                    @Override
                    public void onTranslatedMessage(ConversationMessage conversationMessage, long messageID, boolean isFinal) {
                        global.getTTSLanguages(true, new Global.GetLocalesListListener() {
                            @Override
                            public void onSuccess(ArrayList<CustomLocale> ttsLanguages) {
                                if (isFinal && CustomLocale.containsLanguage(ttsLanguages, conversationMessage.getPayload().getLanguage())) { // check if the language can be speak
                                    speak(conversationMessage.getPayload().getText(), conversationMessage.getPayload().getLanguage());
                                }
                            }

                            @Override
                            public void onFailure(int[] reasons, long value) {
                                //never called in this case
                            }
                        });
                    }

                    @Override
                    public void onFailure(int[] reasons, long value) {
                        FirebaseConversationService.super.notifyError(reasons, value);
                    }
                });
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                FirebaseConversationService.super.notifyError(reasons, value);
            }
        });

        // Process and notify UI
        // You can use Global's translator to handle translations here if needed
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        if (mVoiceRecognizer != null) mVoiceRecognizer.removeCallback(mVoiceRecognizerCallback);
        mVoiceRecognizer = null;
        super.onDestroy();
    }

    @Override
    protected boolean shouldStopMicDuringTTS() {
        return true;  // No Bluetooth headset, so always continue with TTS
    }

    @Override
    protected boolean isBluetoothHeadsetConnected() {
        return false;  // Bluetooth functionality removed
    }

    public static class ConversationServiceCommunicator extends VoiceTranslationServiceCommunicator {
        public ConversationServiceCommunicator(int id) {
            super(id);
            super.serviceHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(android.os.Message msg) {
                    msg.getData().setClassLoader(Peer.class.getClassLoader());
                    int callbackMessage = msg.getData().getInt("callback", -1);
                    Bundle data = msg.getData();
                    executeCallback(callbackMessage, data);
                    return true;
                }
            });
        }
    }

}
