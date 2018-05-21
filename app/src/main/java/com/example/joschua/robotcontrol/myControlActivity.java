package com.example.joschua.robotcontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class myControlActivity extends AppCompatActivity {


    // Objects to access the layout items for Tach, Buttons, and Seek bars
    private static TextView mTachAngleText;
    private static TextView mTachSpeedText;
    private static Switch mEnableRobotSwitch;
    private static SeekBar mThrottleSeekBar;
    private static SeekBar mTurnSeekBar;
    private boolean connected = false;  //turns true when service is conntected - false if disconnected
    private boolean bound = false;      //turns true when activity is bound to service
    private boolean online = false;     //turns true when enableSwitch is checked

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
            mPSoCBleRobotService.connect(mDeviceAddress); //by function connect the start values of THROTTLE and TURN are automatically set to 127 (==0)
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
        setContentView(R.layout.activity_control);

        // Assign the various layout objects to the appropriate variables
        mTachAngleText = (TextView) findViewById(R.id.mtach_angle);
        mTachSpeedText = (TextView) findViewById(R.id.mtach_speed);
        mEnableRobotSwitch = (Switch) findViewById(R.id.enableRobot);
        mThrottleSeekBar = (SeekBar) findViewById(R.id.throttle);
        mTurnSeekBar = (SeekBar) findViewById(R.id.turn);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(ScanActivity.EXTRAS_BLE_ADDRESS);

        // Bind to the BLE service
        Log.i(TAG, "Binding Service");
        Intent RobotServiceIntent = new Intent(this, PSoCBleRobotService.class);

        doBindService(RobotServiceIntent);

        /* This will be called when the  robot enable switch is changed */
        mEnableRobotSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableRobotSwitch(isChecked); //updates the stateFlag for sharing data
                online = isChecked;
            }
        });

        mThrottleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(online) {
                        mPSoCBleRobotService.setVelocity(PSoCBleRobotService.Velocity.THROTTLE, progress); //LEFT wird für Winkel benutzt
                        //Log.d(TAG, "Desired THROTTLE chanced to:" + roundedTHROTTLE);
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
                if (online) {
                    mPSoCBleRobotService.setVelocity(PSoCBleRobotService.Velocity.TURN, progress);
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
        bound = true;
        Log.d(TAG, "Service verbunden");
    }

    public void doUnbindService() {
        if (bound) {
            unbindService(mServiceConnection);
            bound = false;
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
        //doBindService(RobotServiceIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        doUnbindService();
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
        if (isChecked && connected && bound) {
            mPSoCBleRobotService.setRobotState(true);
            //mPSoCBleRobotService.setVelocity(PSoCBleRobotService.Velocity.THROTTLE, 127);
            //mPSoCBleRobotService.setVelocity(PSoCBleRobotService.Velocity.TURN, 127);
        }
        else if(connected && bound){
            mPSoCBleRobotService.setRobotState(false);
        }
    }

    /**
     * Handle broadcasts from the Car service object. The events are:
     * ACTION_CONNECTED: connected to the segway.
     * ACTION_DISCONNECTED: disconnected from the segway.
     * ACTION_DATA_AVAILABLE: received data from the segway.  This can be a result of a read
     * or notify operation.
     */

    private final BroadcastReceiver mRobotUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case PSoCBleRobotService.ACTION_CONNECTED:
                    //Service discovery is started by the service.
                    //Toast conntected = Toast.makeText(myControlActivity.this, "succesfully connected", Toast.LENGTH_LONG);
                    //conntected.show();
                    break;
                case PSoCBleRobotService.ACTION_DISCONNECTED:
                    Toast disconntected = Toast.makeText(myControlActivity.this, "disconnected - Service will be closed", Toast.LENGTH_LONG);
                    disconntected.show();
                    mPSoCBleRobotService.close();
                    break;
                case PSoCBleRobotService.ACTION_DATA_AVAILABLE:
                    // This is called after a Notify completes
                    mTachAngleText.setText(String.format("%.2f", PSoCBleRobotService.getTach(PSoCBleRobotService.Velocity.THROTTLE)));
                    mTachSpeedText.setText(String.format("%.2f", PSoCBleRobotService.getTach(PSoCBleRobotService.Velocity.TURN)));
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
