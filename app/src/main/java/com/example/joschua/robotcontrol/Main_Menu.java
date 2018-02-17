package com.example.joschua.robotcontrol;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;

public class Main_Menu extends AppCompatActivity {

    String arrayName[] = {"Settings",
                            "Control",
                            "Bluetooth"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main__menu);

        CircleMenu circleMenu = (CircleMenu)findViewById(R.id.circle_menu);
        circleMenu.setMainMenu(Color.parseColor("#E0E0E0"),R.drawable.add, R.drawable.remove)
                .addSubMenu(Color.parseColor("#009688"),R.drawable.settings)
                .addSubMenu(Color.parseColor("#8BC34A"),R.drawable.playbutton_1)
                .addSubMenu(Color.parseColor("#2196F3"),R.drawable.bluetooth)
                .setOnMenuSelectedListener(new OnMenuSelectedListener() {
                    @Override
                    public void onMenuSelected(int index) {
                        switch (index){
                            case 0:
                                Toast.makeText(Main_Menu.this, "This may work in the future",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                boolean mcontrol = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent mcontrol = new Intent(getApplicationContext(), Control.class);
                                        startActivity(mcontrol);
                                    }
                                }, 1100);
                                break;
                            case 2:
                                boolean mbluetooth = new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent mbluetooth = new Intent(getApplicationContext(), Bluetooth.class);
                                        startActivity(mbluetooth);
                                    }
                                }, 1100);
                        }
                    }
                });
    }
}
