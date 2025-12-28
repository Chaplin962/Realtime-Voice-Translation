

package com.chaplin.realtimevoicetranslation.access;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.gallery.imageselector.GalleryImageSelector;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import com.chaplin.realtimevoicetranslation.Global;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.bluetooth.tools.BluetoothTools;


public class UserDataFragment extends Fragment {
    private ImageView imageView;
    private Button buttonConfirm;
    private EditText inputName;
    private TextInputLayout inputNameLayout;
    private AccessActivity activity;
    private Global global;
    private GalleryImageSelector userImageContainer;

    public UserDataFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imageView = view.findViewById(R.id.user_image_initialization);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        inputName = view.findViewById(R.id.input_name);
        inputNameLayout = view.findViewById(R.id.input_name_layout);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AccessActivity) requireActivity();
        global = (Global) activity.getApplication();
        userImageContainer = new GalleryImageSelector(imageView, activity, this, R.drawable.user_icon, "com.gallery.RealtimeVoiceTranslator.1.0.provider");

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputNameLayout.setErrorEnabled(false);
                inputNameLayout.setErrorEnabled(true);
                boolean error = false;
                String name = inputName.getText().toString();
                if (name.length() == 0) {
                    error = true;
                    inputNameLayout.setError(getResources().getString(R.string.error_missing_username));
                } else {
                    //compatibily check for supported characters
                    ArrayList<Character> supportedCharacters = BluetoothTools.getSupportedUTFCharacters(global);
                    boolean equals = true;
                    for (int i = 0; i < name.length() && equals; i++) {
                        if (!supportedCharacters.contains(Character.valueOf(name.charAt(i)))) {
                            equals = false;
                        }
                    }

                    if (!equals) {
                        error = true;
                        inputNameLayout.setError(getResources().getString(R.string.error_wrong_username) + BluetoothTools.getSupportedNameCharactersString(global));
                    }
                }
                if (!error) {
                    //save name
                    global.setName(inputName.getText().toString());
                    //save image
                    userImageContainer.saveImage();
                    activity.startFragment(AccessActivity.PAIRING_FRAGMENT, null);
                }
            }
        });
    }

    private void showPrivacyTermsError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        builder.setMessage(R.string.error_privacy_unchecked);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        userImageContainer.onActivityResult(requestCode, resultCode, data, false);
    }
}
