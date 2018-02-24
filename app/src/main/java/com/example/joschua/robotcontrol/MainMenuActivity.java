package com.example.joschua.robotcontrol;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;

public class MainMenuActivity extends AppCompatActivity {

    String arrayName[] = {"Settings",
                            "myControlActivity",
                            "BluetoothActivity"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myactivity_main__menu);

        CircleMenu circleMenu = (CircleMenu)findViewById(R.id.circle_menu);
        circleMenu.setMainMenu(Color.parseColor("#E0E0E0"),R.drawable.add, R.drawable.remove)
                .addSubMenu(Color.parseColor("#009688"),R.drawable.bluetooth)
                .addSubMenu(Color.parseColor("#FFEB3B"),R.drawable.location)
                .addSubMenu(Color.parseColor("#8BC34A"),R.drawable.settings)
                .addSubMenu(Color.parseColor("#2196F3"),R.drawable.playbutton_2)
                .addSubMenu(Color.parseColor("#FF9800"),R.drawable.playbutton_1)
                .setOnMenuSelectedListener(new OnMenuSelectedListener() {
                    @Override
                    public void onMenuSelected(int index) {
                        switch (index){
                            case 0:
                                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                if (!mBluetoothAdapter.isEnabled()){
                                    mBluetoothAdapter.enable();
                                    Toast.makeText(MainMenuActivity.this, "Bluetooth is enabled",
                                            Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    mBluetoothAdapter.disable();
                                    Toast.makeText(MainMenuActivity.this, "Bluetooth is disabled",
                                            Toast.LENGTH_SHORT).show();
                                }

                                break;
                            case 1:
                                boolean mlocation = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent mlocation = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(mlocation);
                                    }
                                }, 1100);
                                break;
                            case 2:
                                boolean msettings = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent msettings = new Intent(getApplicationContext(), TestActivity.class);
                                        startActivity(msettings);
                                    }
                                }, 1100);
                                break;
                            case 3:
                                boolean mplay = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent mplay = new Intent(getApplicationContext(), ScanActivity.class);
                                        startActivity(mplay);
                                    }
                                }, 1100);
                        }
                    }
                });
    }
}
