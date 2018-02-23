package com.example.joschua.robotcontrol;

/**
 * Created by josch on 09.02.2018.
 */

public class ConstantsClass {
    public static  float LOW_PASS_SENSITIVITY = 0.1f;
    public static final int CIRCLE_SMALL_RADIUS = 90;
    public static final int CIRCLE_STROKE_1 = 90;
    public static final int CIRCLE_STROKE_2 = 88;
    public static final int CIRCLE_BIG_RADIUS = 400;
    public static final int CIRCLE_SENSITIVITY = 1100;

    public static String COLOR_SMALL_CIRCLE = "#8BC34A";
    public static  String COLOR_BIG_CIRCLE = "#CDDC39";
    public static final String STROKE1_SMALL_CIRCLE = "#689F38";
    public static final String STROKE2_SMALL_CIRCLE = "#7CB342";

    public static final double MAX_SENSOR_VALUES = 0.5;


    public void setColorSmallCircle(String color){
        COLOR_SMALL_CIRCLE = color;
    }

    public void setColorBigCircle(String color){
        COLOR_BIG_CIRCLE = color;
    }

    public void setLowPassSensitivity(float sensitivity){
        LOW_PASS_SENSITIVITY = sensitivity;
    }
}
