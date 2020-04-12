package com.quantumcoders.travelpool;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.utility.UserInfo;
import com.quantumcoders.travelpool.utility.Util;

public class MainActivity extends AppCompatActivity {
    TextInputEditText emailInput =null, pwdInput =null, phoneInput = null;
    CheckBox registerCheck = null;
    FirebaseAuth auth = null;
    boolean signUpTask = false;
    Button logSignButton = null;
    Handler hnd = null;

    String userEmail,userPwd,userPhone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.email);
        pwdInput = findViewById(R.id.password);
        phoneInput = findViewById(R.id.phoneNum);
        registerCheck = findViewById(R.id.registerCheck);
        phoneInput.setVisibility(View.GONE);
        logSignButton = findViewById(R.id.button);
        hnd = new Handler();

        checkAllPermissions();
        initListeners();

        //autologin is performed in onrequestpermissionresult
    }

    int customPermReqCode=101;
    public void checkAllPermissions(){
        boolean ret = false;
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE};

        ActivityCompat.requestPermissions(this, permissions ,this.customPermReqCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==this.customPermReqCode) {
            boolean flag=true;
            for(int x : grantResults){
                if(x!=PackageManager.PERMISSION_GRANTED) flag=false;
            }
            if(!flag) finish();
            else {
                autoLogin();
            }
        }
    }

    public void autoLogin(){
        String s_email = Util.sprefGetString(this, AppConstants.EMAIL_KEY);
        String s_pwd = Util.sprefGetString(this,AppConstants.PWD_KEY);
        String s_phone = Util.sprefGetString(this,AppConstants.PHONE_KEY);


        if(s_email!=null && s_pwd!=null && s_phone != null){
            //direct login
            System.out.println("Direct Login");
            emailInput.setText(s_email);
            pwdInput.setText(s_pwd);
            registerCheck.setChecked(false);
            logSignButton.performClick();
        }
    }

    public void initListeners(){
        registerCheck.setOnClickListener((v)->{
            phoneInput.setVisibility((((CheckBox)v).isChecked()) ? View.VISIBLE : View.GONE);
        });


        logSignButton.setOnClickListener((v)->{
            v.setEnabled(false);
            userEmail = emailInput.getText().toString();
            userPwd = pwdInput.getText().toString();
            userPhone = phoneInput.getText().toString();

            if(userEmail.isEmpty() || userPwd.isEmpty() || (registerCheck.isChecked() && userPhone.isEmpty())
                    || !userEmail.matches(Patterns.EMAIL_ADDRESS.pattern())){
                Util.shortToast(this,"Invalid Details");
                return;
            }

            if(registerCheck.isChecked()){
                System.out.println("Signing Up");
                signUpTask = true;
                auth.createUserWithEmailAndPassword(userEmail,userPwd).addOnCompleteListener(this, this::loginSignupComplete);
            } else {
                System.out.println("Logging In");
                auth.signInWithEmailAndPassword(userEmail,userPwd).addOnCompleteListener(this, this::loginSignupComplete);
            }

        });
    }

    public void loginSignupComplete(@NonNull Task<AuthResult> task) {
        logSignButton.setEnabled(true);
        if(task.isSuccessful()){

            //save session
            Util.sprefSetString(this,AppConstants.EMAIL_KEY,userEmail);
            Util.sprefSetString(this,AppConstants.PWD_KEY,userPwd);

            //add user phone number to firebase
            if(signUpTask){
                Util.sprefSetString(this,AppConstants.PHONE_KEY,userPhone);
                UserInfo user = new UserInfo();
                user.setEmail(userEmail);
                user.setPhoneNo(userPhone);
                user.setBookedRide("0");
                user.setOfferedRide("0");
                user.setState(AppConstants.STATE_IDLE);
                FirebaseDatabase.getInstance().getReference().child(AppConstants.DOC_USERDATA)
                        .child(Util.encodeEmail(userEmail)).setValue(user)
                        .addOnCompleteListener(task1 -> {
                            Log.d("PhoneTask",""+ task1.isSuccessful());
                            System.out.println(task1.getException());
                        });
            } else {    //login task. fetch phone number
                FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA)
                        .child(Util.encodeEmail(userEmail)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        System.out.println("Phone Obtained " + dataSnapshot.getValue(UserInfo.class).getPhoneNo());
                        Util.sprefSetString(MainActivity.this,AppConstants.PHONE_KEY,dataSnapshot.getValue(UserInfo.class).phoneNo);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            //start home activity
            startActivity(new Intent(MainActivity.this,HomeActivity.class));
            Util.shortToast(MainActivity.this,"Successful Login");
            finish();
        } else {
            //login failed
            System.out.println(task.getException());
            Util.shortToast(MainActivity.this,"Something went wrong!");
        }
    }

}
