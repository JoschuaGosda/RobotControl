package com.example.joschua.robotcontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Control extends AppCompatActivity {
    private static final double VALUE_INTERVAL = 0.5;
    Button mybutton;
    TextView roll, pitch;
    OrientationData orientationData;
    update mupdate;

    private DrawingBall mBall = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        //mybutton = (Button) findViewById(R.id.myBtn);
        //roll = (TextView) findViewById(R.id.roll_value);
        //pitch = (TextView) findViewById(R.id.pitch_value);
        mupdate = new update();
        mupdate.start();
        mBall = new DrawingBall(this);
        setContentView(mBall);
    }

    public class update  {
        public update(){}
        public void start()
        {
            //final OrientationData orientationData = new OrientationData(this);
            orientationData = new OrientationData(Control.this, update.this);
            orientationData.register();
        }
        public void sendData(){
            /*mybutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    orientationData.newGame();
                }
            });*/

            if ((orientationData.getOrientation() != null) && (orientationData.getStartOrientation() != null)) {
                float mpitch = orientationData.getOrientation()[1] - orientationData.getStartOrientation()[1];
                if(mpitch > Constants.MAX_SENSOR_VALUES){
                    mpitch = (float) Constants.MAX_SENSOR_VALUES; //Begrenzung für die Steuerung auf Arduino
                }
                if(mpitch < -Constants.MAX_SENSOR_VALUES){
                    mpitch = (float) -Constants.MAX_SENSOR_VALUES;
                }

                float mroll = orientationData.getOrientation()[2] - orientationData.getStartOrientation()[2];
                if(mroll > Constants.MAX_SENSOR_VALUES){
                    mroll = (float) Constants.MAX_SENSOR_VALUES;
                }
                if(mroll < -Constants.MAX_SENSOR_VALUES){
                    mroll = (float) -Constants.MAX_SENSOR_VALUES;
                }
                //String mytextroll = Float.toString(mroll);
                //roll.setText(mytextroll);
                //String mytextpitch = Float.toString(mpitch);
                //pitch.setText(mytextpitch);

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
            mPaint_small.setColor(Color.parseColor(Constants.COLOR_SMALL_CIRCLE));
            mPaint_small.setStyle(Paint.Style.FILL_AND_STROKE);

            stroke1 = new Paint();
            stroke1.setStyle(Paint.Style.STROKE);
            stroke1.setStrokeWidth(1);
            stroke1.setColor(Color.parseColor(Constants.STROKE1_SMALL_CIRCLE));

            stroke2 = new Paint();
            stroke2.setStyle(Paint.Style.STROKE);
            stroke2.setStrokeWidth(1);
            stroke2.setColor(Color.parseColor(Constants.STROKE2_SMALL_CIRCLE));

            mPaint_big = new Paint();
            mPaint_big.setColor(Color.parseColor(Constants.COLOR_BIG_CIRCLE));
            mPaint_big.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            viewWidth = w;
            viewHeight = h;
        }

        public void setCordinates (float x_data, float y_data) {
            int x = (int) (y_data*Constants.CIRCLE_SENSITIVITY);
            int y = (int) (x_data*Constants.CIRCLE_SENSITIVITY);
            float radius = (float) Math.hypot(x, y);
            float radius_max = Constants.CIRCLE_BIG_RADIUS - Constants.CIRCLE_SMALL_RADIUS;
            float theta = (float) Math.atan2(y, x);

            if(radius > radius_max){
                radius = radius_max;
            }
            x_small = (int) (radius * Math.cos(theta));
            y_small = (int) (radius * Math.sin(theta));

            x_small_final = viewWidth/2 + x_small; //Werte bis jetzt nur in der Darstellung richtig
            y_small_final = viewHeight/2 - y_small; //für Bluetooth muss noch übertragen werden
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawCircle(viewWidth/2, viewHeight/2, Constants.CIRCLE_BIG_RADIUS, mPaint_big);
            canvas.drawCircle(x_small_final, y_small_final, Constants.CIRCLE_SMALL_RADIUS, mPaint_small);
            canvas.drawCircle(x_small_final, y_small_final, Constants.CIRCLE_STROKE_1, stroke1);
            canvas.drawCircle(x_small_final, y_small_final, Constants.CIRCLE_STROKE_2, stroke2);
            invalidate();
        }
    }}
