package com.example.joschua.robotcontrol;

/**
 * Created by josch on 09.02.2018.
 */

public  class ConstantsClass {

    private static  float LOW_PASS_SENSITIVITY = 0.1f;
    private static int CIRCLE_SMALL_RADIUS = 90;
    //public static final int CIRCLE_STROKE_1 = 90;
    //public static final int CIRCLE_STROKE_2 = 88;
    private static final int CIRCLE_BIG_RADIUS = 400;
    private static final int CIRCLE_SENSITIVITY = 1100;


    private  static String THEME = "#FAFAFA";
    private  static String COLOR_SMALL_CIRCLE = "#8BC34A";
    private  static String COLOR_BIG_CIRCLE = "#CDDC39";
    //public static final String STROKE1_SMALL_CIRCLE = "#689F38";
    //public static final String STROKE2_SMALL_CIRCLE = "#7CB342";

    private static final double MAX_SENSOR_VALUES = 0.5;

    public static int getCircleSmallRadius() {
        return CIRCLE_SMALL_RADIUS;
    }

    public static void setCircleSmallRadius(int circleSmallRadius) {
        CIRCLE_SMALL_RADIUS = circleSmallRadius;
    }

    public static String getColorSmallCircle() {
        return COLOR_SMALL_CIRCLE;
    }

    public static void setColorSmallCircle(String colorSmallCircle) {
        COLOR_SMALL_CIRCLE = colorSmallCircle;
    }

    public static void setColorBigCircle(String colorBigCircle) {
        COLOR_BIG_CIRCLE = colorBigCircle;
    }

    public static float getLowPassSensitivity() {
        return LOW_PASS_SENSITIVITY;
    }

    public static int getCircleBigRadius() {
        return CIRCLE_BIG_RADIUS;
    }

    public static int getCircleSensitivity() {
        return CIRCLE_SENSITIVITY;
    }

    public static String getColorBigCircle() {
        return COLOR_BIG_CIRCLE;
    }

    public static double getMaxSensorValues() {
        return MAX_SENSOR_VALUES;
    }

    /*
    public void setColorSmallCircle(String color){
        COLOR_SMALL_CIRCLE = color;
    }

    public void setColorBigCircle(String color){
        COLOR_BIG_CIRCLE = color;
    }

    public void setLowPassSensitivity(float sensitivity){
        LOW_PASS_SENSITIVITY = sensitivity;
    }

    public void setTheme (String Theme){
        THEME = Theme;
    }

    public void setBallSize(int size){
        CIRCLE_SMALL_RADIUS = size;
    }*/
}
