package com.chaplin.realtimevoicetranslation.voice_translation.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.chaplin.realtimevoicetranslation.LoadingActivity;
import com.chaplin.realtimevoicetranslation.R;

public class FragmentFirebaseSignInActivity extends AppCompatActivity {
    String TAG = "EMAIL_UI_AUTH";
    private EditText inputEmail, inputPassword;
    private ProgressBar progressBar;
    private ImageView passwordToggle;
    private boolean isPasswordVisible = false;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_firebase_sign_in);

        // Initialize views after setContentView
        inputEmail = findViewById(R.id.email);
        inputPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        passwordToggle = findViewById(R.id.passwordToggle);
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        passwordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide Password
                    inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    passwordToggle.setImageResource(R.drawable.ic_eye); // Use your "eye" icon
                    isPasswordVisible = false;
                } else {
                    // Show Password
                    inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    passwordToggle.setImageResource(R.drawable.ic_eye_off); // Use your "eye off" icon
                    isPasswordVisible = true;
                }
                inputPassword.setSelection(inputPassword.getText().length());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // if user logged in, go to sign-in screen
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, LoadingActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    public void loginButtonClicked(View view) {
        String email = inputEmail.getText().toString();
        final String password = inputPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (password.length() < 6) {
            inputPassword.setError(getString(R.string.minimum_password));
            progressBar.setVisibility(View.GONE);
        } else {
            //authenticate user
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(FragmentFirebaseSignInActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        // there was an error
                        Toast.makeText(FragmentFirebaseSignInActivity.this, "Authentication failed." + task.getException(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, task.getException().toString());
                        progressBar.setVisibility(View.GONE);
                    } else {
                        Intent intent = new Intent(FragmentFirebaseSignInActivity.this, LoadingActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }
}
