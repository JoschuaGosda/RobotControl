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
import android.net.Uri;
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

    private boolean ble_activated;
    private boolean location_activated;
    Context context;
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myactivity_main__menu);

        context = MainMenuActivity.this;

        CircleMenu circleMenu = (CircleMenu)findViewById(R.id.circle_menu);
        circleMenu.setMainMenu(Color.parseColor("#E0E0E0"),R.drawable.add, R.drawable.remove)
                .addSubMenu(Color.parseColor("#009688"),R.drawable.bluetooth)
                .addSubMenu(Color.parseColor("#FFEB3B"),R.drawable.location)
                .addSubMenu(Color.parseColor("#8BC34A"),R.drawable.settings)
                .addSubMenu(Color.parseColor("#2196F3"),R.drawable.playbutton_2)
                .addSubMenu(Color.parseColor("#FF9800"),R.drawable.android)
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
                                    ble_activated = true;
                                }
                                else{
                                    mBluetoothAdapter.disable();
                                    Toast.makeText(MainMenuActivity.this, "Bluetooth is disabled",
                                            Toast.LENGTH_SHORT).show();
                                    ble_activated = false;
                                }

                                break;
                            case 1:
                                boolean mlocation = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        /*LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                                        if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                            //All location services are disabled
                                            location_activated = false;
                                        }
                                        else{
                                            location_activated = true;
                                        }*/

                                        /*if(location_activated) {
                                            Toast.makeText(MainMenuActivity.this, "location activated",
                                                    Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(MainMenuActivity.this, "location deactivated",
                                                    Toast.LENGTH_SHORT).show();
                                        }*/

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
                                        if(isLocationEnabled() && isBluetoothEnabled()) {
                                            Intent mplay = new Intent(getApplicationContext(), ScanActivity.class);
                                            startActivity(mplay);
                                        }
                                        else{
                                            String mMessage = new String();
                                            if(isBluetoothEnabled() && !isLocationEnabled()){
                                                mMessage = "Please enable Location Services";
                                            }
                                            else if(isLocationEnabled() && !isBluetoothEnabled()){
                                                mMessage = "Please enable Bluetooth";
                                            }
                                            else{
                                                mMessage = "Please enable Bluetooth and Location Services";
                                            }
                                            AlertDialog.Builder mbuilder = new AlertDialog.Builder(context);
                                            mbuilder.setMessage(mMessage)
                                                    .setCancelable(true)
                                                    .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.cancel();
                                                        }
                                                    });
                                            AlertDialog alert = mbuilder.create();
                                            alert.setTitle("Notification");
                                            alert.show();
                                        }
                                    }
                                }, 1100);
                                break;
                            case 4:
                                boolean mgit = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        String mcode = "https://github.com/913791373/RobotControl/tree/master/app/src/main";
                                        Uri webadress = Uri.parse(mcode);

                                        Intent gotoCode = new Intent(Intent.ACTION_VIEW, webadress);
                                        if (gotoCode.resolveActivity(getPackageManager()) != null) {
                                            startActivity(gotoCode);
                                        }
                                    }
                                }, 1100);

                        }
                    }
                });
    }
    protected boolean isLocationEnabled(){
        String le = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) getSystemService(le);
        if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            return false;
        } else {
            return true;
        }
    }

    public boolean isBluetoothEnabled()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();

    }
}
