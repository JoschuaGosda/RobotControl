package com.example.joschua.robotcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class myControlActivity extends AppCompatActivity {
    private static final double VALUE_INTERVAL = 0.5;

    //////////////////////////insert/////////////////////////////////////////////////////////
    // Objects to access the layout items for Tach, Buttons, and Seek bars
    private static TextView mTachAngleText;
    private static TextView mTachSpeedText;
    private static Switch mEnableRobotSwitch;
    private static SeekBar mThrottleSeekBar;
    private static SeekBar mTurnSeekBar;
    private boolean connected = false;
    private boolean mIsBound = false;
    private boolean control_activated = false;
    private static int counter = 0;

    // This tag is used for debug messages
    private static final String TAG = myControlActivity.class.getSimpleName();

    private static String mDeviceAddress;
    private static PSoCBleRobotService mPSoCBleRobotService;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object, initialize the service, and connect.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            connected = true;
            mPSoCBleRobotService = ((PSoCBleRobotService.LocalBinder) service).getService();
            if (!mPSoCBleRobotService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the car database upon successful start-up initialization.
            mPSoCBleRobotService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mPSoCBleRobotService = null;
            connected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myactivity_control_v2);

        // Assign the various layout objects to the appropriate variables
        mTachAngleText = (TextView) findViewById(R.id.mtach_angle);
        mTachSpeedText = (TextView) findViewById(R.id.mtach_speed);
        mEnableRobotSwitch = (Switch) findViewById(R.id.menable_robot2);
        mThrottleSeekBar = (SeekBar) findViewById(R.id.throttle);
        mTurnSeekBar = (SeekBar) findViewById(R.id.turn);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(ScanActivity.EXTRAS_BLE_ADDRESS);

        // Bind to the BLE service
        Log.i(TAG, "Binding Service");
        Intent RobotServiceIntent = new Intent(this, PSoCBleRobotService.class);

        doBindService(RobotServiceIntent);

        /* This will be called when the left motor enable switch is changed */
        mEnableRobotSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableRobotSwitch(isChecked); //mod - ON/OFF beider motoren werden über einen knopf gesteuert
                control_activated = isChecked;
                //orientationData.Reset();
            }
        });

        mThrottleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //tv.setTextSize(progress*5);
                //tv_left.setText(progress + "");
                if(control_activated) {
                        mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.PITCH, progress); //LEFT wird für Winkel benutzt
                        //Log.d(TAG, "Desired PITCH chanced to:" + roundedpitch);
                    }
                }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mThrottleSeekBar.setProgress(127);
            }
        });

        mTurnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //tv.setTextSize(progress*5);
                //tv_left.setText(progress + "");
                if (control_activated) {
                    mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.ROLL, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTurnSeekBar.setProgress(127);
            }
        });
    } /* End of onCreate method */

    public void doBindService(Intent RobotServiceIntent) {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(RobotServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "Service verbunden");
    }

    public void doUnbindService() {
        if (mIsBound) {
            unbindService(mServiceConnection);
            mIsBound = false;
            mPSoCBleRobotService = null;
            Log.d(TAG, "Service ungebunden");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mRobotUpdateReceiver, makeRobotUpdateIntentFilter());
        if (mPSoCBleRobotService != null) {
            final boolean result = mPSoCBleRobotService.connect(mDeviceAddress);
            Log.i(TAG, "Connect request result=" + result);
            Log.d(TAG, "Receiver registered!!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRobotUpdateReceiver);
        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    /**
     * Enable or disable the left/right motor
     *
     * @param isChecked used to enable/disable motor
     //* @param angle is the motor to enable/disable (left or right)
     */
    private void enableRobotSwitch(boolean isChecked) {
        if (isChecked && connected && mIsBound) { // Turn on the specified motor
            mPSoCBleRobotService.setRobotState(true);
        } else if (connected && mIsBound && counter == 0) { // turn off the specified motor
            mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.PITCH, 127); // Force neutral PITCH value
            mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.ROLL, 127); // Force neutral ROLL value
            mPSoCBleRobotService.setRobotState(false);
        }
    }

    /**
     * Handle broadcasts from the Car service object. The events are:
     * ACTION_CONNECTED: connected to the car.
     * ACTION_DISCONNECTED: disconnected from the car.
     * ACTION_DATA_AVAILABLE: received data from the car.  This can be a result of a read
     * or notify operation.
     */

    ///////////
    private final BroadcastReceiver mRobotUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case PSoCBleRobotService.ACTION_CONNECTED:
                    // No need to do anything here. Service discovery is started by the service.
                    break;
                case PSoCBleRobotService.ACTION_DISCONNECTED:
                    mPSoCBleRobotService.close();
                    break;
                case PSoCBleRobotService.ACTION_DATA_AVAILABLE:
                    // This is called after a Notify completes
                    mTachAngleText.setText(String.format("%d", PSoCBleRobotService.getTach(PSoCBleRobotService.Angle.PITCH)));
                    mTachSpeedText.setText(String.format("%d", PSoCBleRobotService.getTach(PSoCBleRobotService.Angle.ROLL)));
                    break;
            }
        }
    };

    /**
     * This sets up the filter for broadcasts that we want to be notified of.
     * This needs to match the broadcast receiver cases.
     *
     * @return intentFilter
     */
    private static IntentFilter makeRobotUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PSoCBleRobotService.ACTION_CONNECTED);
        intentFilter.addAction(PSoCBleRobotService.ACTION_DISCONNECTED);
        intentFilter.addAction(PSoCBleRobotService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
