package hcmute.edu.vn.thanh0456.zaloclone;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import hcmute.edu.vn.thanh0456.zaloclone.activities.SignInActivity;


public class interface1 extends AppCompatActivity  {
     Timer timer ;
    private static final int REQUEST_CODE= 101010;
   BiometricPrompt biometricPrompt;
   BiometricPrompt.PromptInfo promptInfo;
   ConstraintLayout mMainLayout ;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interface1);
        mMainLayout=findViewById(R.id.main_Layout);
        BiometricManager biometricManager = BiometricManager.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            switch (biometricManager.canAuthenticate()){

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Device Doesn't not have fingerprint", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Not working", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // Prompts the user to create credentials that your app accepts.
                Toast.makeText(this, "No FingerPrint assigned", Toast.LENGTH_SHORT).show();
                break;
        }
        }
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(interface1.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                                "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                mMainLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(" login for my chat")
                .setSubtitle("Use FingerPrint to login ")
                .setDeviceCredentialAllowed(true)
                .build();

        // Prompt appears when user clicks "Log in".
        // Consider integrating with the keystore to unlock cryptographic operations,
        // if needed by your app.

            biometricPrompt.authenticate(promptInfo);




        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(interface1.this, SignInActivity.class);
                startActivity(intent);

            }
        }, 5000);



    }
}
