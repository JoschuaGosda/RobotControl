package com.example.joschua.robotcontrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout mrelativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams mrelativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        mrelativeLayout.setLayoutParams(mrelativeParams);
        setContentView(mrelativeLayout);

        TextView speedView = new TextView(this);
        RelativeLayout.LayoutParams speedViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        speedViewParams.addRule((RelativeLayout.ALIGN_PARENT_TOP));
        speedViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        speedViewParams.setMargins(0, 100, 100, 0);
        speedView.setLayoutParams(speedViewParams);
        speedView.setText(R.string.app_name);
        speedView.setPadding(0,200,0,0);

        mrelativeLayout.addView(speedView);

        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.test, mrelativeLayout, true);

        /*LayoutInflater mInflator = getLayoutInflater();
        Switch mSwitch = (Switch) mInflator.inflate(R.layout.test, mrelativeLayout);
        //mrelativeLayout.addView(mSwitch);

        Button activateButton = new Button(this);
        RelativeLayout.LayoutParams activateButtonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        activateButtonParams.addRule(RelativeLayout.ALIGN_BOTTOM);
        activateButtonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        activateButton.setLayoutParams(activateButtonParams);
        activateButton.setPadding(0,100,0,0);*/
    }
}
