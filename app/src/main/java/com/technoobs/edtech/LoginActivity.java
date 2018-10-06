package com.technoobs.edtech;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "Login Activity";
    private static final int LOGIN_VIEW = 1;
    private static final int CODE_VERIFICATION_VIEW = 2;

    private LinearLayout llVerificationLayout;
    private EditText etMobileNumber;
    private EditText etVerificationField;
    private CountryCodePicker ccp;


    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private boolean mVerificationInProgress = false;
    private Button btnSignUp;
    private Button btnVerifyCode;
    private LinearLayout llSignupLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewByIds();

        mAuth = FirebaseAuth.getInstance();

        btnSignUp.setOnClickListener(this);
        btnVerifyCode.setOnClickListener(this);

        //Attach Carrier Number Edit text
        ccp.registerCarrierNumberEditText(etMobileNumber);

        //Phone Auth
        initializePhoneAuth();
    }

    @Override
    public void onStart() {
        super.onStart();

        //check verification is in process and user entered number or not and continue process
        if (mVerificationInProgress && ccp.isValidFullNumber()) {
            startPhoneNumberVerification(ccp.getFullNumberWithPlus());
        }
    }


    private void startPhoneNumberVerification(String phoneNumber) {
        try {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    mCallbacks);        // OnVerificationStateChangedCallbacks

            mVerificationInProgress = true;
        } catch (Exception e) {
            Log.v(TAG, e.getMessage());
            Toast.makeText(this, "Oops!", Toast.LENGTH_SHORT).show();
        }
    }


    private void initializePhoneAuth() {
        // Initialize phone auth callbacks
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                mVerificationInProgress = false; //Change state of verification progress
                signInWithPhoneAuthCredential(credential); //On Verification complete sing In with That credential
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);

                mVerificationInProgress = false; //Change state of verification progress

                //if number is invalid set error
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    etMobileNumber.setError("Invalid phone number.");
                }

                changeView(LOGIN_VIEW, true); //changeView to Login page
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {

                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                changeButtonStage(false);
                changeView(CODE_VERIFICATION_VIEW, false); //Change view to code verification page
            }
        };
    }

    private void findViewByIds() {
        ccp = findViewById(R.id.ccPicker);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        btnSignUp = findViewById(R.id.btnSignUp);
        etVerificationField = findViewById(R.id.etVerificationCode);
        btnVerifyCode = findViewById(R.id.btnVerification);
        llVerificationLayout = findViewById(R.id.llCodeVerificationLayout);
        llSignupLayout = findViewById(R.id.llMobile);
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        //Verify With Code
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);

        //Check Verified or Not
        signInWithPhoneAuthCredential(credential);
    }

    // sign in with Mobile Number
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {

        //try to sing in with provided credentials
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            changeActivity(); // redirect to welcome activity
                        } else {
                            // Sign in failed, display a message
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                etVerificationField.setError("Invalid code.");
                            }
                            Toast.makeText(LoginActivity.this, "Oops!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSignUp:
                Log.v(TAG, "Signup Called");
                if (ccp.isValidFullNumber()) { //Validate number and verify that number
                    etMobileNumber.setError(null);
                    changeButtonStage(false);

                    //Verify Mobile Number
                    startPhoneNumberVerification(ccp.getFullNumberWithPlus());
                } else {
                    // set error message
                    etMobileNumber.setError(getString(R.string.invalid_phone_number));
                }
                break;
            case R.id.btnVerification:
                Log.v(TAG, "Verification Called");
                changeButtonStage(false);
                String code = etVerificationField.getText().toString();
                //Verify Phone Number Using Verification Code
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;
        }
    }

    private void changeButtonStage(boolean state) {
        //Toggle progress bar
        btnSignUp.setClickable(state);
        btnVerifyCode.setClickable(state);
    }

    private void changeView(int loginView, boolean isError) {
        if (isError) {
            changeButtonStage(true);
            Toast.makeText(this, "No Internet!", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (loginView) {
            case LOGIN_VIEW:
                llVerificationLayout.setVisibility(View.GONE);
                btnVerifyCode.setVisibility(View.GONE);
                llSignupLayout.setVisibility(View.VISIBLE);
                btnSignUp.setVisibility(View.VISIBLE);
                btnVerifyCode.setClickable(false);
                btnSignUp.setClickable(true);
                break;
            case CODE_VERIFICATION_VIEW:
                llSignupLayout.setVisibility(View.GONE);
                btnSignUp.setVisibility(View.GONE);
                llVerificationLayout.setVisibility(View.VISIBLE);
                btnVerifyCode.setVisibility(View.VISIBLE);
                btnVerifyCode.setClickable(true);
                btnSignUp.setClickable(false);
                break;
        }
    }

    //Redirect user to main activity
    private void changeActivity() {
        finish();
        Intent welcomeIntent = new Intent(this, MainActivity.class);
        startActivity(welcomeIntent);
    }


}
