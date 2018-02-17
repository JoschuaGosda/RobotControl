package com.example.joschua.robotcontrol;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class BluetoothService extends Service {
    public BluetoothService() {
    }

    private final IBinder myBinder = new MyLocalBinder();   //bridge that connects acitivity with service

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public String getCurrentTime(){
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
        return (df.format(new Date()));
    }

    public class MyLocalBinder extends Binder{
        //Binder class is able to bind two things togehter
        //allows to access "superclass"/Bluetoothservice from activity
        //reference to service
             BluetoothService getService(){
                 return BluetoothService.this;
             }
    }
}
