package com.chaplin.realtimevoicetranslation;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import com.chaplin.realtimevoicetranslation.access.AccessActivity;
import com.chaplin.realtimevoicetranslation.tools.CustomLocale;
import com.chaplin.realtimevoicetranslation.tools.ErrorCodes;
import com.chaplin.realtimevoicetranslation.voice_translation.VoiceTranslationActivity;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.NeuralNetworkApi;
import com.chaplin.realtimevoicetranslation.voice_translation.neural_networks.translation.Translator;

public class LoadingActivity extends GeneralActivity {
    private Handler mainHandler;
    private boolean isVisible = false;
    private Global global;
    private boolean startingActivity = false;
    private boolean showingError = false;

    public LoadingActivity() {
        // Required empty public constructor
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String previousActivity = getIntent().getStringExtra("activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void onResume() {
        super.onResume();
        isVisible = true;
        global = (Global) getApplication();
        if (global.isFirstStart()) {
            Intent intent = new Intent(this, AccessActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } else if (global.getTranslator() != null && global.getSpeechRecognizer() != null) {
            startVoiceTranslationActivity();
        } else {
            initializeApp(false);
            //onFailure(new int[]{ErrorCodes.GOOGLE_TTS_ERROR}, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    private void initializeApp(boolean ignoreTTSError) {
        global.getLanguages(false, ignoreTTSError, new Global.GetLocalesListListener() {
            @Override
            public void onSuccess(ArrayList<CustomLocale> result) {
                global.initializeTranslator(new Translator.InitListener() {
                    @Override
                    public void onInitializationFinished() {
                        global.initializeSpeechRecognizer(new NeuralNetworkApi.InitListener() {
                            @Override
                            public void onInitializationFinished() {
                                if (isVisible) {
                                    startVoiceTranslationActivity();
                                }
                            }

                            @Override
                            public void onError(int[] reasons, long value) {
                                global.deleteSpeechRecognizer();  //we do this to ensure the restart of the loading of models when the app is restarted
                                LoadingActivity.this.onFailure(reasons, value);
                            }
                        });
                    }

                    @Override
                    public void onError(int[] reasons, long value) {
                        global.deleteTranslator();   //we do this to ensure the restart of the loading of models when the app is restarted
                        LoadingActivity.this.onFailure(reasons, value);
                    }
                });
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                LoadingActivity.this.onFailure(reasons, value);
            }
        });
    }

    private void startVoiceTranslationActivity() {
        startingActivity = true;
        Intent intent = new Intent(LoadingActivity.this, VoiceTranslationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void notifyGoogleTTSErrorDialog() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                showGoogleTTSErrorDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        initializeApp(true);
                    }
                });
            }
        });
    }

    public void notifyInternetLack() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isVisible) {
                    // creation of the dialog.
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                    //builder.setCancelable(true);
                    builder.setMessage(R.string.error_internet_lack_loading);
                    builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            initializeApp(false);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                }
            }
        });
    }

    public void notifyModelsLoadingError() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isVisible) {
                    // creation of the dialog.
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                    //builder.setCancelable(true);
                    builder.setMessage(R.string.error_models_loading);
                    builder.setPositiveButton(R.string.fix, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(global != null){
                                //restartDownload();
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                }
            }
        });
    }

    private void notifyMissingGoogleTTSDialog() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isVisible) {
                    showMissingGoogleTTSDialog(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            initializeApp(true);
                        }
                    });
                }
            }
        });
    }

    private void onFailure(int[] reasons, long value) {
        for (int aReason : reasons) {
            switch (aReason) {
                case ErrorCodes.ERROR_LOADING_MODEL:
                    showingError = true;
                    notifyModelsLoadingError();
                    break;
                case ErrorCodes.SAFETY_NET_EXCEPTION:
                case ErrorCodes.MISSED_CONNECTION:
                    showingError = true;
                    notifyInternetLack();
                    break;
                case ErrorCodes.MISSING_GOOGLE_TTS:
                    showingError = true;
                    notifyMissingGoogleTTSDialog();
                    break;
                case ErrorCodes.GOOGLE_TTS_ERROR:
                    showingError = true;
                    notifyGoogleTTSErrorDialog();
                    break;
                default:
                    onError(aReason, value);
                    break;
            }
        }
    }
}
