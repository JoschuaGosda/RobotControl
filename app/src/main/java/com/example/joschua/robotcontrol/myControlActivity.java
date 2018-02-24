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
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class myControlActivity extends AppCompatActivity {
    private static final double VALUE_INTERVAL = 0.5;
    OrientationDataClass orientationData;
    update mupdate;

    private DrawingBall mBall = null;
    //////////////////////////insert/////////////////////////////////////////////////////////
    // Objects to access the layout items for Tach, Buttons, and Seek bars
    private static TextView mTachAngleText;
    private static TextView mTachSpeedText;
    private static SeekBar mSpeedLeftSeekBar;
    private static SeekBar mSpeedRightSeekBar;
    private static Switch mEnableRobotSwitch;
    private static Switch mEnableRightSwitch;
    private boolean connected = false;
    private boolean mIsBound = false;

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

    //////////////////////////end///////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myactivity_control);



        //mybutton = (Button) findViewById(R.id.myBtn);
        //roll = (TextView) findViewById(R.id.roll_value);
        //pitch = (TextView) findViewById(R.id.pitch_value);




        //setContentView(mBall);
        ////////////////////////insert/////////////////////////////////////
        //setContentView(R.layout.activity_control);

        // Assign the various layout objects to the appropriate variables
        mTachAngleText = (TextView) findViewById(R.id.mtach_angle);
        mTachSpeedText = (TextView) findViewById(R.id.mtach_speed);
        mEnableRobotSwitch = (Switch) findViewById(R.id.menable_robot);
        // mEnableRightSwitch = (Switch) findViewById(R.id.enable_right);
        mSpeedLeftSeekBar = (SeekBar) findViewById(R.id.speed_left);
        mSpeedRightSeekBar = (SeekBar) findViewById(R.id.speed_right);

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
            }
        });

        mupdate = new update();
        mupdate.start();
        mBall = new DrawingBall(this);

        /* This will be called when the right motor enable switch is changed
        mEnableRightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableMotorSwitch(isChecked, PSoCBleRobotService.Motor.RIGHT);
            }
        });*/

        /* This will be called when the left speed seekbar is moved */
        /*mSpeedLeftSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int speed, boolean fromUser) {
                 Scale the speed from what the seek bar provides to what the PSoC FW expects
                speed = scaleSpeed(speed);
                mPSoCBleRobotService.setMotorSpeed(PSoCBleRobotService.Motor.LEFT, speed);
                Log.d(TAG, "Left Speed Change to:" + speed);
            }
        });

        /* This will be called when the right speed seekbar is moved */
       /* mSpeedRightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int speed, boolean fromUser) {
                 Scale the speed from what the seek bar provides to what the PSoC FW expects
                speed = scaleSpeed(speed);
                mPSoCBleRobotService.setMotorSpeed(PSoCBleRobotService.Motor.RIGHT, speed);
                Log.d(TAG, "Right Speed Change to:" + speed);
            }
        });*/

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

   /* @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRobotUpdateReceiver, makeRobotUpdateIntentFilter());
        if (mPSoCBleRobotService != null) {
            final boolean result = mPSoCBleRobotService.connect(mDeviceAddress);
            Log.i(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        registerReceiver(mRobotUpdateReceiver, makeRobotUpdateIntentFilter());
        if (mPSoCBleRobotService != null) {
            final boolean result = mPSoCBleRobotService.connect(mDeviceAddress);
            Log.i(TAG, "Connect request result=" + result);
        }
    }
*/

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
        //unbindService(mServiceConnection);     //mod
        //mPSoCBleRobotService = null;           //mod
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        //unbindService(mServiceConnection);
        //mPSoCBleRobotService = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
        //unbindService(mServiceConnection);
        //mPSoCBleRobotService = null;
    }

    /**
     * Scale the speed read from the slider (0 to 20) to
     * what the car object expects (-100 to +100).
     *
     * @param speed Input speed from the slider
     * @return scaled value of the speed
     */
    private int scaleSpeed(int speed) {
        final int SCALE = 10;
        final int OFFSET = 100;

        return ((speed * SCALE) - OFFSET);
    }

    private int scaleAngle(float angle) {
        final int SCALE = 255; //werte von Sensor zwischen 0,5 und -0,5 --> Winkel von 20 bis -20°
        final float OFFSET = (float) 0.5;
        int result = (int) Math.round((angle + OFFSET)* SCALE) ;
        return result;
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
            //Log.d(TAG, (angle == PSoCBleRobotService.Angle.PITCH ? "Left" : "Right") + " Motor On");
        } else if (connected && mIsBound) { // turn off the specified motor
            mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.PITCH, 127); // Force motor off
            mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.ROLL, 127); // Force motor off
            mPSoCBleRobotService.setRobotState(false);
            /*if(angle == PSoCBleRobotService.Angle.PITCH) {
                mSpeedLeftSeekBar.setProgress(10); // Move slider to middle position
            } else {
                mSpeedRightSeekBar.setProgress(10); // Move slider to middle position
            }
            Log.d(TAG, (angle == PSoCBleRobotService.Angle.PITCH ? "Left" : "Right") + " Motor Off");*/
        }

    }

    /**
     * Handle broadcasts from the Car service object. The events are:
     * ACTION_CONNECTED: connected to the car.
     * ACTION_DISCONNECTED: disconnected from the car.
     * ACTION_DATA_AVAILABLE: received data from the car.  This can be a result of a read
     * or notify operation.
     */
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

    //////////////////////////end///////////////////////////////////////////////////////


    public class update  {

        float mpitch, mroll;
        int roundedpitch, roundedroll;
        int roundedpitch_old, roundedroll_old;

        public update(){}

        public void start()
        {
            //final OrientationDataClass orientationData = new OrientationDataClass(this);
            orientationData = new OrientationDataClass(myControlActivity.this, update.this);
            orientationData.register();
        }
        public void sendData(){
            /*mybutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    orientationData.newGame();
                }
            });*/
            //Log.d(TAG, "Sensordaten kommen an");
            if ((orientationData.getOrientation() != null) && (orientationData.getStartOrientation() != null)) {
                 mpitch = orientationData.getOrientation()[1] - orientationData.getStartOrientation()[1];
                if(mpitch > ConstantsClass.MAX_SENSOR_VALUES){
                    mpitch = (float) ConstantsClass.MAX_SENSOR_VALUES; //Begrenzung für die Steuerung auf Arduino
                }
                if(mpitch < -ConstantsClass.MAX_SENSOR_VALUES){
                    mpitch = (float) -ConstantsClass.MAX_SENSOR_VALUES;
                }

                mroll = orientationData.getOrientation()[2] - orientationData.getStartOrientation()[2];
                if(mroll > ConstantsClass.MAX_SENSOR_VALUES){
                    mroll = (float) ConstantsClass.MAX_SENSOR_VALUES;
                }
                if(mroll < -ConstantsClass.MAX_SENSOR_VALUES){
                    mroll = (float) -ConstantsClass.MAX_SENSOR_VALUES;
                }
                ///////////////////////////modstart//////////////////////////////////////////

                roundedpitch = scaleAngle(mpitch);
                roundedroll = scaleAngle(mroll);

                //Log.d(TAG, "roundedpitch:" + roundedpitch);
                //Log.d(TAG, "roundedroll:" + roundedroll);

                /* Scale the speed from what the seek bar provides to what the PSoC FW expects */
                if((roundedpitch != roundedpitch_old) && connected && mIsBound){
                    mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.PITCH, roundedpitch); //LEFT wird für Winkel benutzt
                    //Log.d(TAG, "Desired PITCH chanced to:" + roundedpitch);
                    roundedpitch_old = roundedpitch;
                }
                if((roundedroll != roundedroll_old) && connected && mIsBound){
                    mPSoCBleRobotService.setAngle(PSoCBleRobotService.Angle.ROLL, roundedroll);
                    //Log.d(TAG, "Desired ROLL chanced to:" + roundedroll);
                    roundedroll_old = roundedroll;
                }


                /////////////////////////modend//////////////////////////////////////////

                mBall.setCordinates(mpitch, mroll);
            }
        }
    }

    public class DrawingBall extends View{

        //private static final int CIRCLE_SMALL_RADIUS = 90; //pixels
        //private static final int CIRCLE_STROKE_1 = 90; //pixels
        //private static final int CIRCLE_STROKE_2 = 88; //pixels
        //private static final int CIRCLE_BIG_RADIUS = 400; //pixels
        //private static final int SENSITIVITY = 1100;

        private Paint mPaint_small;
        private Paint stroke1;
        private Paint stroke2;
        private Paint mPaint_big;
        private int x_small;
        private int y_small;
        private int x_small_final;
        private int y_small_final;
        private int viewWidth;
        private int viewHeight;

        public DrawingBall (Context context){
            super(context);
            mPaint_small = new Paint();
            mPaint_small.setColor(Color.parseColor(ConstantsClass.COLOR_SMALL_CIRCLE));
            mPaint_small.setStyle(Paint.Style.FILL_AND_STROKE);

            stroke1 = new Paint();
            stroke1.setStyle(Paint.Style.STROKE);
            stroke1.setStrokeWidth(1);
            stroke1.setColor(Color.parseColor(ConstantsClass.STROKE1_SMALL_CIRCLE));

            stroke2 = new Paint();
            stroke2.setStyle(Paint.Style.STROKE);
            stroke2.setStrokeWidth(1);
            stroke2.setColor(Color.parseColor(ConstantsClass.STROKE2_SMALL_CIRCLE));

            mPaint_big = new Paint();
            mPaint_big.setColor(Color.parseColor(ConstantsClass.COLOR_BIG_CIRCLE));
            mPaint_big.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            viewWidth = w;
            viewHeight = h;
        }

        public void setCordinates (float x_data, float y_data) {
            int x = (int) (y_data* ConstantsClass.CIRCLE_SENSITIVITY);
            int y = (int) (x_data* ConstantsClass.CIRCLE_SENSITIVITY);
            float radius = (float) Math.hypot(x, y);
            float radius_max = ConstantsClass.CIRCLE_BIG_RADIUS - ConstantsClass.CIRCLE_SMALL_RADIUS;
            float theta = (float) Math.atan2(y, x);

            if(radius > radius_max){
                radius = radius_max;
            }
            x_small = (int) (radius * Math.cos(theta));
            y_small = (int) (radius * Math.sin(theta));

            x_small_final = viewWidth/2 + x_small; //Werte bis jetzt nur in der Darstellung richtig
            y_small_final = viewHeight/2 - y_small; //für BluetoothActivity muss noch übertragen werden
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawCircle(viewWidth/2, viewHeight/2, ConstantsClass.CIRCLE_BIG_RADIUS, mPaint_big);
            canvas.drawCircle(x_small_final, y_small_final, ConstantsClass.CIRCLE_SMALL_RADIUS, mPaint_small);
            canvas.drawCircle(x_small_final, y_small_final, ConstantsClass.CIRCLE_STROKE_1, stroke1);
            canvas.drawCircle(x_small_final, y_small_final, ConstantsClass.CIRCLE_STROKE_2, stroke2);
            invalidate();
        }
    }
}
