package com.example.joschua.robotcontrol;

import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class TestActivity extends AppCompatActivity {

    private static RadioGroup groupTheme;
    private static RadioButton radioButton;
    private static Button button_submit;
    private static RadioButton lightTheme, darkTheme, green, red_orange, red_blue, indigo_pink, small, medium, big;
    private String[] Theme = {"#FAFAFA", "#424242"};
    private String[] Color = {"#8BC34A", "#CDDC39", //small - big / green
                                "#F44336", "#FF9800", //red - orange
                                "#2196F3", "#B71C1C", //blue - red
                                "#E91E63", "#3F51B5"}; //pink - indigo
    private int[] Size = {60, 90, 120};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        onClickListenerButton();
    }

    public void onClickListenerButton() {
        //groupTheme = (RadioGroup)findViewById(R.id.group_theme);
        button_submit = (Button)findViewById(R.id.btn_submit);
        //lightTheme = (RadioButton)findViewById(R.id.lighttheme);
        //darkTheme = (RadioButton)findViewById(R.id.darktheme);
        green = (RadioButton)findViewById(R.id.green);
        red_orange = (RadioButton)findViewById(R.id.redorange);
        red_blue = (RadioButton)findViewById(R.id.redblue);
        indigo_pink = (RadioButton)findViewById(R.id.indigopink);
        small = (RadioButton)findViewById(R.id.small);
        medium = (RadioButton)findViewById(R.id.medium);
        big = (RadioButton)findViewById(R.id.big);

        button_submit.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //int selected_id = groupTheme.getCheckedRadioButtonId();
                        //radioButton = (RadioButton)findViewById(selected_id);

                        /*if(lightTheme.isChecked()){
                            ConstantsClass.THEME = Theme[0];
                        }
                        if(darkTheme.isChecked()){
                            ConstantsClass.THEME = Theme[1];
                        }*/
                        if(green.isChecked()){
                            ConstantsClass.setColorSmallCircle(Color[0]);
                            ConstantsClass.setColorBigCircle(Color[1]);
                        }
                        if(red_orange.isChecked()){
                            ConstantsClass.setColorSmallCircle(Color[2]);
                            ConstantsClass.setColorBigCircle(Color[3]);
                        }
                        if(red_blue.isChecked()){
                            ConstantsClass.setColorSmallCircle(Color[4]);
                            ConstantsClass.setColorBigCircle(Color[5]);
                        }
                        if(indigo_pink.isChecked()){
                            ConstantsClass.setColorSmallCircle(Color[6]);
                            ConstantsClass.setColorBigCircle(Color[7]);
                        }
                        if(small.isChecked()){
                            ConstantsClass.setCircleSmallRadius(Size[0]);
                        }
                        if(medium.isChecked()){
                            ConstantsClass.setCircleSmallRadius(Size[1]);
                        }
                        if(big.isChecked()){
                            ConstantsClass.setCircleSmallRadius(Size[2]);
                        }


                        //Toast.makeText(MainActivity.this,
                                //radioButton.getText().toString(),Toast.LENGTH_SHORT ).show();
                    }
                }
        );
    }
}
