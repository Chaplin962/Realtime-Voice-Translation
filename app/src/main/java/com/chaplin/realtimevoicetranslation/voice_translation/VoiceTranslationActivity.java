

package com.chaplin.realtimevoicetranslation.voice_translation;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.chaplin.realtimevoicetranslation.GeneralActivity;
import com.chaplin.realtimevoicetranslation.Global;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.bluetooth.BluetoothCommunicator;
import com.chaplin.realtimevoicetranslation.bluetooth.Peer;
import com.chaplin.realtimevoicetranslation.settings.SettingsActivity;
import com.chaplin.realtimevoicetranslation.tools.CustomLocale;
import com.chaplin.realtimevoicetranslation.tools.CustomServiceConnection;
import com.chaplin.realtimevoicetranslation.tools.Tools;
import com.chaplin.realtimevoicetranslation.tools.gui.peers.GuiPeer;
import com.chaplin.realtimevoicetranslation.tools.services_communication.ServiceCommunicatorListener;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode.PairingFragment;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.ConversationFragment;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.ConversationService;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.main.ConversationMainFragment;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode.communication.ConversationBluetoothCommunicator;
import com.chaplin.realtimevoicetranslation.voice_translation.firebase.FirebaseConversationFragment;
import com.chaplin.realtimevoicetranslation.voice_translation.firebase.FirebaseConversationService;
import com.chaplin.realtimevoicetranslation.voice_translation.firebase.FirebaseMainConversationFragment;
import com.chaplin.realtimevoicetranslation.voice_translation.firebase.FirebaseRoomFragment;

import java.util.ArrayList;
import java.util.List;

public class VoiceTranslationActivity extends GeneralActivity {
    private String TAG = "VoiceTranslationActivity";

    //flags
    public static final int NORMAL_START = 0;
    public static final int FIRST_START = 1;
    //costants
    public static final int PAIRING_FRAGMENT = 0;
    public static final int CONVERSATION_FRAGMENT = 1;
    public static final int FIREBASE_ROOM_FRAGMENT = 2;
    public static final int FIREBASE_CONVERSATION_FRAGMENT = 3;
    public static final int DEFAULT_FRAGMENT = FIREBASE_ROOM_FRAGMENT;
    public static final int NO_PERMISSIONS = -10;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;
    public static String[] REQUIRED_PERMISSIONS;
    //objects
    private Global global;
    private Fragment fragment;
    private CoordinatorLayout fragmentContainer;
    private int currentFragment = -1;
    private boolean startingPairing = false;   //used to start Conversation Mode after bluetooth permissions are granted
    private ArrayList<Callback> clientsCallbacks = new ArrayList<>();
    private ArrayList<CustomServiceConnection> conversationServiceConnections = new ArrayList<>();
    private Handler mainHandler;  // handler that can be used to post to the main thread
    //variables
    private int connectionId = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        global = (Global) getApplication();
        mainHandler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            REQUIRED_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION,};
        } else {
            REQUIRED_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }

        // Clean fragments (only if the app is recreated (When user disable permission))
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // Remove previous fragments (case of the app was restarted after changed permission on android 6 and higher)
        List<Fragment> fragmentList = fragmentManager.getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        fragmentContainer = findViewById(R.id.fragment_container);

        /*if (savedInstanceState != null) {
            //Restore the fragment's instance
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "myFragmentName");
        }*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        // when we return to the app's gui based on the service that was saved in the last closure we choose which fragment to start
        SharedPreferences sharedPreferences = this.getSharedPreferences("default", Context.MODE_PRIVATE);
        setFragment(sharedPreferences.getInt("fragment", DEFAULT_FRAGMENT));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void setFragment(int fragmentName) {
        switch (fragmentName) {
            case PAIRING_FRAGMENT: {
                // possible stop of the Conversation and WalkieTalkie Service
                stopConversationService();
                // possible setting of the fragment
                if (getCurrentFragment() != PAIRING_FRAGMENT) {
                    if (Tools.hasPermissions(this, REQUIRED_PERMISSIONS)) {
                        global.initializeBluetoothCommunicator();
                        if (global.getBluetoothCommunicator() != null && global.getBluetoothCommunicator().isBluetoothLeSupported()) {
                            PairingFragment paringFragment = new PairingFragment();
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            Bundle bundle = new Bundle();
                            paringFragment.setArguments(bundle);
                            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                            transaction.replace(R.id.fragment_container, paringFragment);
                            transaction.commit();
                            currentFragment = PAIRING_FRAGMENT;
                            saveFragment();
                            //fragment=paringFragment;
                        } else if (global.getBluetoothCommunicator().isBluetoothLeSupported()) {
                            Toast.makeText(global, "Error with Bluetooth, please restart the app", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(global, R.string.error_missing_bluetooth_le, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        startingPairing = true;
                        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
                    }
                }
                break;
            }
            case CONVERSATION_FRAGMENT: {
                // possible setting of the fragment
                if (getCurrentFragment() != CONVERSATION_FRAGMENT && global.getBluetoothCommunicator() != null) {
                    ConversationFragment conversationFragment = new ConversationFragment();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("firstStart", true);
                    conversationFragment.setArguments(bundle);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, conversationFragment);
                    transaction.commit();
                    currentFragment = CONVERSATION_FRAGMENT;
                    saveFragment();
                    //fragment= conversationFragment;
                } else if (global.getBluetoothCommunicator() == null) {
                    setFragment(DEFAULT_FRAGMENT);
                }
                break;
            }
            case FIREBASE_CONVERSATION_FRAGMENT: {
                if (getCurrentFragment() != FIREBASE_CONVERSATION_FRAGMENT) {
                    FirebaseConversationFragment firebaseConversationFragment = new FirebaseConversationFragment();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("firstStart", true);
                    firebaseConversationFragment.setArguments(bundle);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, firebaseConversationFragment);
                    transaction.commit();
                    currentFragment = FIREBASE_CONVERSATION_FRAGMENT;
                    saveFragment();
                }
                break;
            }
            case FIREBASE_ROOM_FRAGMENT: {
                stopConversationService();
                if (getCurrentFragment() != FIREBASE_ROOM_FRAGMENT) {
                    FirebaseRoomFragment firebaseRoomFragment = new FirebaseRoomFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    SharedPreferences sharedPreferences = VoiceTranslationActivity.this.getSharedPreferences("default", Context.MODE_PRIVATE);
                    bundle.putString("username", sharedPreferences.getString("name", "user"));
                    bundle.putString("language", sharedPreferences.getString("language", "en"));

                    firebaseRoomFragment.setArguments(bundle);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    transaction.replace(R.id.fragment_container, firebaseRoomFragment);
                    transaction.commit();
                    currentFragment = FIREBASE_ROOM_FRAGMENT;
                    saveFragment();
                }
                break;
            }
        }
    }

    public void saveFragment() {
        new Thread("saveFragment") {
            @Override
            public void run() {
                super.run();
                //save fragment
                SharedPreferences sharedPreferences = VoiceTranslationActivity.this.getSharedPreferences("default", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("fragment", getCurrentFragment());
                editor.apply();
            }
        }.start();
    }

    public int getCurrentFragment() {
        if (currentFragment != -1) {
            return currentFragment;
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                if (currentFragment.getClass().equals(PairingFragment.class)) {
                    return PAIRING_FRAGMENT;
                }
                if (currentFragment.getClass().equals(ConversationFragment.class)) {
                    return CONVERSATION_FRAGMENT;
                }

                if (currentFragment.getClass().equals(FirebaseRoomFragment.class)) {
                    return FIREBASE_ROOM_FRAGMENT;
                }
                if (currentFragment.getClass().equals(FirebaseConversationFragment.class)) {
                    return FIREBASE_CONVERSATION_FRAGMENT;
                }
            }
        }
        return -1;
    }

    public int startSearch() {
        if (global.getBluetoothCommunicator() != null) {
            return global.getBluetoothCommunicator().startSearch();
        } else {
            return BluetoothCommunicator.ERROR;
        }
    }

    public int stopSearch(boolean tryRestoreBluetoothStatus) {
        if (global.getBluetoothCommunicator() != null) {
            return global.getBluetoothCommunicator().stopSearch(tryRestoreBluetoothStatus);
        } else {
            return BluetoothCommunicator.ERROR;
        }
    }

    public boolean isSearching() {
        if (global.getBluetoothCommunicator() != null) {
            return global.getBluetoothCommunicator().isSearching();
        } else {
            return false;
        }
    }

    public void connect(Peer peer) {
        stopSearch(false);
        if (global.getBluetoothCommunicator() != null) {
            global.getBluetoothCommunicator().connect(peer);
        }
    }

    public void acceptConnection(Peer peer) {
        if (global.getBluetoothCommunicator() != null) {
            global.getBluetoothCommunicator().acceptConnection(peer);
        }
    }

    public void rejectConnection(Peer peer) {
        if (global.getBluetoothCommunicator() != null) {
            global.getBluetoothCommunicator().rejectConnection(peer);
        }
    }

    public ArrayList<GuiPeer> getConnectedPeersList() {
        if (global.getBluetoothCommunicator() != null) {
            return global.getBluetoothCommunicator().getConnectedPeersList();
        } else {
            return new ArrayList<GuiPeer>();
        }
    }

    public ArrayList<Peer> getConnectingPeersList() {
        if (global.getBluetoothCommunicator() != null) {
            return global.getBluetoothCommunicator().getConnectingPeers();
        } else {
            return new ArrayList<Peer>();
        }
    }

    public void disconnect(Peer peer) {
        if (global.getBluetoothCommunicator() != null) {
            global.getBluetoothCommunicator().disconnect(peer);
        }
    }



    /*@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, "myFragmentName", fragment);
    }*/

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                //notifyMissingSearchPermission();
                Toast.makeText(global, R.string.error_missing_location_permissions, Toast.LENGTH_LONG).show();
                startingPairing = false;
                return;
            }
        }
        //bluetooth permissions are granted
        if (startingPairing) {
            startingPairing = false;
            if (currentFragment != FIREBASE_CONVERSATION_FRAGMENT) {
                setFragment(PAIRING_FRAGMENT);
            }
        }
        //notifySearchPermissionGranted();

        /*if (!Tools.hasPermissions(this, REQUIRED_PERMISSIONS)) {

        }else{

        }*/
        //recreate();   // was called only if the grantResults were of length 0 or were neither PERMISSIONS_GRANTED nor PERMISSION_DENIED (I don't know what it is for anyway)
    }

    @Override
    public void onBackPressed() {
        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitFromVoiceTranslation();
            }
        };

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            if (fragment instanceof ConversationFragment) {
                Fragment currentChildFragment = ((ConversationFragment) fragment).getCurrentFragment();
                if (currentChildFragment instanceof ConversationMainFragment) {
                    ConversationMainFragment conversationMainFragment = (ConversationMainFragment) currentChildFragment;
                    if (conversationMainFragment.isInputActive()) {
                        if (conversationMainFragment.isEditTextOpen()) {
                            conversationMainFragment.deleteEditText();
                        } else {
                            showConfirmExitDialog(confirmExitListener);
                        }
                    }
                } else {
                    showConfirmExitDialog(confirmExitListener);
                }
            } else if (fragment instanceof PairingFragment) {
                setFragment(DEFAULT_FRAGMENT);
            } else if (fragment instanceof FirebaseConversationFragment) {
                Fragment currentChildFragment = ((FirebaseConversationFragment) fragment).getCurrentFragment();
                if (currentChildFragment instanceof FirebaseMainConversationFragment) {
                    FirebaseMainConversationFragment firebaseMainConversationFragment = (FirebaseMainConversationFragment) currentChildFragment;
                    if (firebaseMainConversationFragment.isInputActive()) {
                        if (firebaseMainConversationFragment.isEditTextOpen()) {
                            firebaseMainConversationFragment.deleteEditText();
                        } else {
                            Log.d(TAG, "onBackPressed 1");
                            showConfirmExitDialog(confirmExitListener);
                        }
                    }
                } else {
                    Log.d(TAG, "onBackPressed 2");
                    showConfirmExitDialog(confirmExitListener);
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void exitFromVoiceTranslation() {
        Log.d(TAG, "exitFromVoiceTranslation, currentFragment = " + currentFragment);
        if (currentFragment != FIREBASE_CONVERSATION_FRAGMENT) {
            if (getConnectedPeersList().size() > 0 && global.getBluetoothCommunicator() != null) {
                global.getBluetoothCommunicator().disconnectFromAll();
            } else if (global.getBluetoothCommunicator() != null) {
                setFragment(VoiceTranslationActivity.PAIRING_FRAGMENT);
            } else {  //if global.getBluetoothCommunicator() == null
                setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
            }
        } else {
            setFragment(VoiceTranslationActivity.FIREBASE_ROOM_FRAGMENT);
        }
    }

    // services management
    public void startConversationService(final Notification notification, final Global.ResponseListener responseListener) {
        final Intent intent = new Intent(this, ConversationService.class);
        global.getLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {
                if (NotificationManagerCompat.from(VoiceTranslationActivity.this).areNotificationsEnabled()) {
                    intent.putExtra("notification", notification);
                } else {
                    Toast.makeText(VoiceTranslationActivity.this, getResources().getString(R.string.toast_missing_notification_permission), Toast.LENGTH_LONG).show();
                }
                startService(intent);
                responseListener.onSuccess();
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });

    }

    // services management firebase
    public void startConversationService(final Bundle serviceData, final Notification notification, final Global.ResponseListener responseListener) {
        Log.d(TAG, "startConversationService is being called");

        final Intent intent = new Intent(this, FirebaseConversationService.class);
        intent.putExtras(serviceData);
        global.getLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {
                if (NotificationManagerCompat.from(VoiceTranslationActivity.this).areNotificationsEnabled()) {
                    intent.putExtra("notification", notification);
                } else {
                    Toast.makeText(VoiceTranslationActivity.this, getResources().getString(R.string.toast_missing_notification_permission), Toast.LENGTH_LONG).show();
                }
                startService(intent);
                Log.d(TAG, "startService was called");
                responseListener.onSuccess();
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });

    }

    public synchronized void connectToConversationService(final VoiceTranslationService.VoiceTranslationServiceCallback callback, final ServiceCommunicatorListener responseListener) {
        // possible start of ConversationService
        startConversationService(buildNotification(CONVERSATION_FRAGMENT), new Global.ResponseListener() {
            @Override
            public void onSuccess() {
                CustomServiceConnection conversationServiceConnection = new CustomServiceConnection(new ConversationService.ConversationServiceCommunicator(connectionId));
                connectionId++;
                conversationServiceConnection.addCallbacks(callback, responseListener);
                conversationServiceConnections.add(conversationServiceConnection);
                bindService(new Intent(VoiceTranslationActivity.this, ConversationService.class), conversationServiceConnection, BIND_ABOVE_CLIENT);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public synchronized void connectToConversationService(final Bundle serviceData, final VoiceTranslationService.VoiceTranslationServiceCallback callback, final ServiceCommunicatorListener responseListener) {
        // Start the FirebaseConversationService explicitly
        Log.d(TAG, "connectToConversationService is being called");
        startConversationService(serviceData, buildNotification(FIREBASE_CONVERSATION_FRAGMENT), new Global.ResponseListener() {
            @Override
            public void onSuccess() {
                CustomServiceConnection conversationServiceConnection = new CustomServiceConnection(new FirebaseConversationService.ConversationServiceCommunicator(connectionId));
                connectionId++;
                conversationServiceConnection.addCallbacks(callback, responseListener);
                conversationServiceConnections.add(conversationServiceConnection);
                Log.d(TAG, "bindService is being called");
                bindService(new Intent(VoiceTranslationActivity.this, FirebaseConversationService.class), conversationServiceConnection, BIND_ABOVE_CLIENT);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public void disconnectFromConversationService(ConversationService.ConversationServiceCommunicator conversationServiceCommunicator) {
        int index = -1;
        boolean found = false;
        for (int i = 0; i < conversationServiceConnections.size() && !found; i++) {
            if (conversationServiceConnections.get(i).getServiceCommunicator().equals(conversationServiceCommunicator)) {
                index = i;
                found = true;
            }
        }
        if (index != -1) {
            CustomServiceConnection serviceConnection = conversationServiceConnections.remove(index);
            unbindService(serviceConnection);
            serviceConnection.onServiceDisconnected();
        }
    }

    public void disconnectFromConversationService(FirebaseConversationService.ConversationServiceCommunicator conversationServiceCommunicator) {
        int index = -1;
        boolean found = false;
        for (int i = 0; i < conversationServiceConnections.size() && !found; i++) {
            if (conversationServiceConnections.get(i).getServiceCommunicator().equals(conversationServiceCommunicator)) {
                index = i;
                found = true;
            }
        }
        if (index != -1) {
            CustomServiceConnection serviceConnection = conversationServiceConnections.remove(index);
            unbindService(serviceConnection);
            serviceConnection.onServiceDisconnected();
        }
    }

    public void stopConversationService() {
        stopService(new Intent(this, ConversationService.class));
    }

    //notification
    private Notification buildNotification(int clickAction) {
        String channelID = "service_background_notification";
        // creation of the click on the notification
        Intent resultIntent = new Intent(this, VoiceTranslationActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        // creation of the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        if (clickAction == CONVERSATION_FRAGMENT) {
            builder.setContentTitle(getString(R.string.title_fragment_conversation)).setContentText(getString(R.string.conversation_mode_running)).setContentIntent(resultPendingIntent).setSmallIcon(R.drawable.mic_icon).setOngoing(true).setChannelId(channelID).build();
        }
        return builder.build();
    }


    public void addCallback(Callback callback) {
        // in this way the listener will listen to both this activity and the communicator
        if (global.getBluetoothCommunicator() != null) {
            global.getBluetoothCommunicator().addCallback(callback);
        }
        clientsCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        if (global.getBluetoothCommunicator() != null) {
            global.getBluetoothCommunicator().removeCallback(callback);
        }
        clientsCallbacks.remove(callback);
    }

    private void notifyMissingSearchPermission() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < clientsCallbacks.size(); i++) {
                    clientsCallbacks.get(i).onMissingSearchPermission();
                }
            }
        });
    }

    private void notifySearchPermissionGranted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < clientsCallbacks.size(); i++) {
                    clientsCallbacks.get(i).onSearchPermissionGranted();
                }
            }
        });
    }

    public CoordinatorLayout getFragmentContainer() {
        return fragmentContainer;
    }

    public static class Callback extends ConversationBluetoothCommunicator.Callback {
        public void onMissingSearchPermission() {
        }

        public void onSearchPermissionGranted() {
        }
    }
}
