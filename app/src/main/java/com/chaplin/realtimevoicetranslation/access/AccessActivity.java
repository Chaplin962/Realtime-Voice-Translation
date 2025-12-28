package com.chaplin.realtimevoicetranslation.access;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.chaplin.realtimevoicetranslation.GeneralActivity;
import com.chaplin.realtimevoicetranslation.Global;
import com.chaplin.realtimevoicetranslation.LoadingActivity;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode.PairingFragment;

public class AccessActivity extends GeneralActivity {
    public static final int USER_DATA_FRAGMENT = 0;
    public static final int PAIRING_FRAGMENT = 2;
    private Fragment fragment;
    private AccessActivity activity;
    private Global global;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access);
        activity = this;
        global = (Global) activity.getApplication();
        if (savedInstanceState != null) {
            //Restore the fragment's instance
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "fragment_inizialization");
        } else {
            final SharedPreferences sharedPreferences = this.getSharedPreferences("default", Context.MODE_PRIVATE);
            String savedUserName = sharedPreferences.getString("name", "");
            if (savedUserName.length() > 0) {
                startFragment(PAIRING_FRAGMENT, null);
            } else {
                startFragment(USER_DATA_FRAGMENT, null);
            }
        }
    }

    @Override
    protected void onStart() {
        Global global = (Global) getApplication();
        if (global != null) {
            global.setAccessActivity(this);
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Global global = (Global) getApplication();
        if (global != null) {
            global.setAccessActivity(null);
        }
    }

    public void startFragment(int action, Bundle bundle) {
        switch (action) {
            case USER_DATA_FRAGMENT: {
                UserDataFragment userDataFragment = new UserDataFragment();
                if (bundle != null) {
                    userDataFragment.setArguments(bundle);
                }
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.replace(R.id.fragment_initialization_container, userDataFragment);
                transaction.commit();
                fragment = userDataFragment;
                break;
            }
            case PAIRING_FRAGMENT: {
                startRTranslator();
                break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, "fragment_inizialization", fragment);
    }


    private void startRTranslator() {
        if (activity != null) {
            //modification of the firstStart
            global.setFirstStart(false);
            //start activity
            Intent intent = new Intent(activity, LoadingActivity.class);
            intent.putExtra("activity", "download");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            activity.finish();
        }
    }
}


