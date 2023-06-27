package hcmute.edu.vn.thanh0456.zaloclone.activities;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import hcmute.edu.vn.thanh0456.zaloclone.R;

public  class ForgetPassword extends AppCompatActivity implements View.OnClickListener {
    private EditText email;
    private Button btn;
    private TextView backtosignin;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        email = findViewById(R.id.TextEmail);
        btn = findViewById(R.id.button);
        btn.setOnClickListener(this);
        backtosignin =  findViewById(R.id.textView5);
        backtosignin.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textView5:
                startActivity(new Intent(this, SignInActivity.class));
                break;
            case R.id.button:
                String useremail=email.getText().toString().trim();
                if(TextUtils.isEmpty(useremail)){
                    Toast.makeText(ForgetPassword.this , "please Enter the email", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.sendPasswordResetEmail(useremail).addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Toast.makeText(ForgetPassword.this,"Please check your mail account and rest your password",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), SignInActivity.class)); }
                        else {
                            String message= Objects.requireNonNull(task.getException()).getMessage();
                            Toast.makeText(ForgetPassword.this,"Error Occured:"+message,Toast.LENGTH_SHORT).show();
                        }
                    });
                }

        }

    }

}
