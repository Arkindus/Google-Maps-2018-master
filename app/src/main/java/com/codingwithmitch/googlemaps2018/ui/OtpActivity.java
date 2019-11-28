package com.codingwithmitch.googlemaps2018.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.codingwithmitch.googlemaps2018.R;
import com.codingwithmitch.googlemaps2018.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.concurrent.TimeUnit;

import static android.text.TextUtils.isEmpty;
import static com.codingwithmitch.googlemaps2018.util.Check.doStringsMatch;

public class OtpActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "OtpActivity";

    //widgets
    private String mUsername, mEmail, mPassword, mConfirmPassword;
    private ProgressBar mProgressBar;

    //vars
    private FirebaseFirestore mDb;

    //otp widgets
    private EditText mPhoneNumber, mOTP;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private FirebaseAuth mAuth;
    private String mVerificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        Intent intent = getIntent();
        mUsername = intent.getStringExtra("Username");
        mEmail = intent.getStringExtra("Email");
        mPassword = intent.getStringExtra("Password");
        mConfirmPassword = intent.getStringExtra("ConfirmPassword");

        mProgressBar = findViewById(R.id.progressBar);

        mPhoneNumber = findViewById(R.id.et_phone_number);
        mOTP = findViewById(R.id.et_otp);

        findViewById(R.id.btn_generate_otp).setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();
        initFireBaseCallbacks();
        findViewById(R.id.btn_resend_otp).setOnClickListener(this);
        findViewById(R.id.btn_register).setOnClickListener(this);

        mDb = FirebaseFirestore.getInstance();

        hideSoftKeyboard();
    }

    public void registerNewEmail(final String email, String password){

        showDialog();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: AuthState: " + FirebaseAuth.getInstance().getCurrentUser().getUid());

                            //insert some default data
                            User user = new User();
                            //user.setEmail(email);
                            user.setEmail(mEmail);
                            user.setUsername(mUsername);
                            //user.setUsername(email.substring(0, email.indexOf("@")));
                            user.setUser_id(FirebaseAuth.getInstance().getUid());

                            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                                    .setTimestampsInSnapshotsEnabled(true)
                                    .build();
                            mDb.setFirestoreSettings(settings);

                            DocumentReference newUserRef = mDb
                                    .collection(getString(R.string.collection_users))
                                    .document(FirebaseAuth.getInstance().getUid());

                            newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    hideDialog();

                                    if(task.isSuccessful()){
                                        redirectLoginScreen();
                                    }else{
                                        Toast.makeText(OtpActivity.this,"Something went wrong",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        }
                        else {
                            Toast.makeText(OtpActivity.this,"Something went wrong",Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }

                        // ...
                    }
                });
    }

    /**
     * Redirects the user to the login screen
     */
    private void redirectLoginScreen(){
        Log.d(TAG, "redirectLoginScreen: redirecting to verification screen.");

        Intent intent = new Intent(OtpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideDialog(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void initFireBaseCallbacks() {

        mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Toast.makeText(OtpActivity.this, "Verified & Registered Successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(OtpActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                Toast.makeText(OtpActivity.this, "Code Sent", Toast.LENGTH_SHORT).show();
                mVerificationCode = verificationId;
                mResendToken = token;
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.btn_generate_otp:{
                Log.d(TAG, "onClick: attempting to verify.");

                if(!isEmpty(mPhoneNumber.getText().toString())){

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            mPhoneNumber.getText().toString(),                     // Phone number to verify
                            30,                           // Timeout duration
                            TimeUnit.SECONDS,                // Unit of timeout
                            this,        // Activity (for callback binding)
                            mCallback);                      // OnVerificationStateChangedCallbacks

                }else{
                    Toast.makeText(OtpActivity.this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.btn_resend_otp: {
                Log.d(TAG, "onClick: attempting to verify.");

                if(!isEmpty(mPhoneNumber.getText().toString())) {
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            mPhoneNumber.getText().toString(),        // Phone number to verify
                            30,               // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            this,               // Activity (for callback binding)
                            mCallback,         // OnVerificationStateChangedCallbacks
                            mResendToken);             // Force Resending Token from callbacks
                }else{
                    Toast.makeText(OtpActivity.this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.btn_register:{
                Log.d(TAG, "onClick: attempting to register.");

                //check for null valued EditText fields
                if(!isEmpty(mPhoneNumber.getText().toString())
                        && !isEmpty(mOTP.getText().toString())){

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationCode, mOTP.getText().toString());
                    mAuth.signInWithCredential(credential)
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(OtpActivity.this, "Verification Success", Toast.LENGTH_SHORT).show();
                                        registerNewEmail(mEmail, mPassword);
                                    } else {
                                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                            Toast.makeText(OtpActivity.this, "Verification Failed, Invalid credentials", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                }else{
                    Toast.makeText(OtpActivity.this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
}