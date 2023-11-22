 package com.example.faceecho;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.annotation.NonNull;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

 public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE=200;
     private static final String PREFS_NAME = "MyPrefsFile";
     private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private Button GETSTARTEDBTN;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!preferences.getBoolean(IS_FIRST_TIME_LAUNCH, true)) {
            openNextScreen();
            finish();
        }
        setContentView(R.layout.activity_main);
        GETSTARTEDBTN= findViewById(R.id.GETSTARTED);
        GETSTARTEDBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)
                !=PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST_CODE);
                }else{
                    Toast.makeText(MainActivity.this,"CAMERA Permission Already Granted",Toast.LENGTH_SHORT).show();
                openNextScreen();
                finish();
                }
            }
        });
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, false);
        editor.apply();
    }
    @Override
     public void onRequestPermissionsResult(int requestcode, @NonNull String[]permissions, @NonNull int[]grantResults) {
        super.onRequestPermissionsResult(requestcode, permissions, grantResults);
        if (requestcode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
                openNextScreen();
                finish();
            } else {
                Toast.makeText(this, "Please Allow Camera Access To Proceed", Toast.LENGTH_SHORT).show();
            }
        }
    }
     private void openNextScreen() {
         Intent intent = new Intent(MainActivity.this, MainHome.class);
         startActivity(intent);
     }
}