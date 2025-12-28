

package com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.main;


import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.tools.services_communication.ServiceCommunicator;
import com.chaplin.realtimevoicetranslation.tools.services_communication.ServiceCommunicatorListener;
import com.chaplin.realtimevoicetranslation.voice_translation.VoiceTranslationFragment;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.ConversationService;


public class ConversationMainFragment extends VoiceTranslationFragment {
    private TextView micInput;
    private Handler mHandler = new Handler();

    public ConversationMainFragment() {
        //an empty constructor is always needed for fragments
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        voiceTranslationServiceCommunicator = new ConversationService.ConversationServiceCommunicator(0);
        voiceTranslationServiceCallback = new VoiceTranslationServiceCallback() {
            @Override
            public void onBluetoothHeadsetConnected() {
                super.onBluetoothHeadsetConnected();
                if(getContext() != null && micInput != null) {
                    micInput.setText(getResources().getString(R.string.btHeadset));
                }
            }

            @Override
            public void onBluetoothHeadsetDisconnected() {
                super.onBluetoothHeadsetDisconnected();
                if(getContext() != null && micInput != null) {
                    micInput.setText(getResources().getString(R.string.mic));
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_voice_translation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        micInput = view.findViewById(R.id.inputMicType);
        microphone.setMicInput(micInput);
    }

    /*@Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState!=null){
            //si riapre l' editText se era aperto al momento della distuzione dell' activity per motivi di sistema (es. rotazione o multi-windows)
            isEditTextOpen=savedInstanceState.getBoolean("isEditTextOpen");
            if(isEditTextOpen){
                keyboard.generateEditText(activity,microphone,editText,false);
            }
        }
    }*/

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
        activity.connectToConversationService(voiceTranslationServiceCallback, new ServiceCommunicatorListener() {
            @Override
            public void onServiceCommunicator(ServiceCommunicator serviceCommunicator) {
                voiceTranslationServiceCommunicator = (ConversationService.ConversationServiceCommunicator) serviceCommunicator;
                restoreAttributesFromService();
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                ConversationMainFragment.this.onFailureConnectingWithService(reasons, value);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
        activity.disconnectFromConversationService((ConversationService.ConversationServiceCommunicator) voiceTranslationServiceCommunicator);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isEditTextOpen",isEditTextOpen);
    }*/


}
