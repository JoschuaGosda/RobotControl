package com.example.joschua.robotcontrol;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by josch on 07.02.2018.
 */

public class OrientationData implements SensorEventListener {
    //static final float ALPHA = 0.01f;
    private SensorManager manager;
    private Sensor accelerometer;
    private Sensor magnometer;
    private Context mContext;
    private Control.update mContext2;
    private float[] accelOutput;
    private float[] magOutput;
    private float[] orientation = new float[3];
    private float[] startOrientation = null;

    public float[] getOrientation() {
        return orientation;
    }

    public float[] getStartOrientation() {
        return startOrientation;
    }

    public void newGame() { startOrientation = null;}

    public OrientationData(Context mContext, Control.update mContext2) {
        this.mContext = mContext;
        this.mContext2 = mContext2;
        manager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void register() {
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, magnometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void pause() {
        manager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accelOutput = lowPass(event.values.clone(),accelOutput);
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magOutput = lowPass(event.values.clone(), magOutput);


        if(accelOutput != null && magOutput != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, accelOutput, magOutput);
            if(success) {
                SensorManager.getOrientation(R, orientation);
                if(startOrientation == null) {
                    startOrientation = new float[orientation.length];
                    System.arraycopy(orientation, 0, startOrientation, 0, orientation.length);
                }
            }
        }
        mContext2.sendData();
    }

    protected float[] lowPass(float[] input, float[] output) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + Constants.LOW_PASS_SENSITIVITY * (input[i] - output[i]);
        }
        return output;
    }
}
