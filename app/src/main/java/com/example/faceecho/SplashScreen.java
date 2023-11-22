package com.example.faceecho;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class SplashScreen extends AppCompatActivity {
    private TextView splashMessage;
    private boolean isInternetConnected = false;
    private ConnectivityChangeReceiver connectivityChangeReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        splashMessage = findViewById(R.id.splashMessage);

        // Check for internet connection before continuing
        if (!isInternetConnected()) {
            // Show a message to the user indicating that internet connection is required
            Toast.makeText(this, "Please turn on the internet connection", Toast.LENGTH_LONG).show();

            // You can periodically check for internet connection and proceed when available
            startConnectivityChangeReceiver();
        } else {
            // If there's an internet connection, proceed after a delay
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 2000); // 2000 milliseconds delay
    }
}

    private void startConnectivityChangeReceiver() {
        connectivityChangeReceiver = new ConnectivityChangeReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }


    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return true;
    }
    private void checkForInternetAndProceed() {
        if (isInternetConnected()) {
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Continue checking if the internet connection is not available
                    checkForInternetAndProceed();
                }
            }, 5000); // 5000 milliseconds delay
        }
        }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver to avoid memory leaks
        if (connectivityChangeReceiver != null) {
            unregisterReceiver(connectivityChangeReceiver);
        }
    }
    // Receiver to detect changes in network connectivity
    public class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInternetConnected()) {
                isInternetConnected = true;
                // If the internet connection is now available, proceed
                checkForInternetAndProceed();
            }
        }
    }
}
