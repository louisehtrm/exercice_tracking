package com.example.ass3;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    Button mainButton;

    float averageSpeed;
    float totalDistance ;
    int timeTaken;
    double minAltitude;
    double maxAltitude;

    TextView averageS;
    TextView distanceT;
    TextView timeT;
    TextView minA;
    TextView maxA;

    String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mainButton = findViewById(R.id.mainButton);

        averageS = findViewById(R.id.averageSpeedV);
        distanceT = findViewById(R.id.totalDistanceV);
        timeT = findViewById(R.id.timeTakenV);
        minA = findViewById(R.id.minAltitudeV);
        maxA = findViewById(R.id.maxAltitudeV);

        //retrieve the data
        Intent intent = getIntent();
        totalDistance = intent.getFloatExtra("totD", 0);
        minAltitude = intent.getDoubleExtra("minA", 0);
        maxAltitude = intent.getDoubleExtra("maxA", 0);
        timeTaken = intent.getIntExtra("sec", 0);

        //the average speed
        averageSpeed = 0;
        int i = 0;
        for(; i < MainActivity.points.size(); i++){
            averageSpeed += MainActivity.points.get(i);
        }
        averageSpeed /= i;

        //get only two number after the decimal points
        averageSpeed = (float)((int)(averageSpeed*100))/100;
        averageS.setText(averageSpeed + "");

        //the time
        if(timeTaken / 3600 >= 1)
            time = (int)timeTaken/3600 + ":" + (int)timeTaken % 3600 / 60 + ":" + timeTaken % 60;
        else if(timeTaken / 60 >= 1)
            time = (int)timeTaken / 60 + ":" + timeTaken % 60;
        else
            time = timeTaken % 60 + "s";

        timeT.setText(time);

        //the distance
        totalDistance = (float) ((int)(totalDistance))/1000;
        distanceT.setText(totalDistance + "");

        //the altitude
        minA.setText(minAltitude + "");
        maxA.setText(maxAltitude + "");

        //popup message
        if(MainActivity.setGoal) {
            final AlertDialog.Builder myGoalPopup = new AlertDialog.Builder(ReportActivity.this);

            //Success of the goal
            if (timeTaken > MainActivity.secondsGoal) {
                myGoalPopup.setTitle("Success of your goal");
                myGoalPopup.setMessage("Well done ! You have achieved your daily goal !");
                myGoalPopup.setIcon(R.drawable.fireworks);
            }

            //fail of the goal
            else {
                myGoalPopup.setTitle("Failure of your goal");
                myGoalPopup.setMessage("You did not reach your goal !\nBut courage you will improve yourself !");
                myGoalPopup.setIcon(R.drawable.fail);
            }

            AlertDialog dialog = myGoalPopup.create();

            //display the dialog
            dialog.show();
        }
    }

    //recording button to go to MainActivity
    public void ReturnRecording(View view) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("lastA", averageSpeed);
        intent.putExtra("lastT", time);
        intent.putExtra("lastK", totalDistance);
        startActivity(intent);
        finish();
    }


}