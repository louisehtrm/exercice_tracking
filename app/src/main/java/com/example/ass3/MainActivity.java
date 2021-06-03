package com.example.ass3;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    ImageButton recordButton;
    ImageButton pauseButton;
    ImageButton resetButton;
    TextView timeView;

    TextView lastAverage;
    TextView lastTime;
    TextView lastKilo;

    float totalDistance;
    double minAltitude;
    double maxAltitude;

    TextView day;
    TextView month;

    TextView dayGoal;

    //state of the stopwatch
    private boolean running;
    private boolean wasRunning;

    //number of seconds displayed on the stopwatch
    int seconds = 0;
    int lastS = 0;

    //GPX file
    File myExternalFile;

    //location of the user
    LocationListener listener;
    LocationManager locMan;
    double latitude = 0, longitude = 0;

    //list of different speed
    static List<Float> points;

    //for the recent activity
    float lastAverageV;
    float lastKiloV;
    String lastTimeV = "00:00";

    private MainActivity activity;

    //for the goal of the day
    static int secondsGoal;
    static boolean setGoal = false;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //retrieve the different view
        recordButton = findViewById(R.id.recordButton);
        pauseButton = findViewById(R.id.pauseButton);
        resetButton = findViewById(R.id.resetButton);
        timeView = findViewById(R.id.time_view);

        day = findViewById(R.id.dayV);
        month = findViewById(R.id.month);

        lastAverage = findViewById(R.id.lastAverage);
        lastKilo = findViewById(R.id.kilometers);
        lastTime = findViewById(R.id.time);

        dayGoal = findViewById(R.id.dayGoal);

        //retrieve the data from the other activity
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Intent intent = getIntent();
            lastAverageV = intent.getFloatExtra("lastA", 0);
            lastTimeV = intent.getStringExtra("lastT");
            lastKiloV = intent.getFloatExtra("lastK", 0);
        }

        //set the text of the recent activity
        lastAverage.setText(lastAverageV + "");
        lastTime.setText(lastTimeV);
        lastKilo.setText(lastKiloV  + "");

        this.activity = this;

        //start the stopwatch checking
        runTimer();

        locMan = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //disabled the button if can't store the GPX file in external storage
        if(!isExternalStorageAvailableForRW())
            recordButton.setEnabled(false);

        //initialize values
        totalDistance = 0 ;
        minAltitude = 0;
        maxAltitude = 0;
        totalDistance = 0;

        //getting the current day
        Calendar calendar = Calendar.getInstance();
        String dayD = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.getTime().getTime());

        //getting the current date value
        LocalDate currentDate = LocalDate.now();
        int currentDay = currentDate.getDayOfMonth();
        Month currentMonth = currentDate.getMonth();
        int currentYear = currentDate.getYear();

        day.setText(dayD + ",");
        month.setText(currentMonth + " " + currentDay + ", " + currentYear);
    }

    //start and stop the tracking
    public void StartRecording(View view) {
        //stop the recording
        if(running) {

            //stop the stopwatch
            running = false;

            //stop the tracking
            locMan.removeUpdates(listener);

            endFile();

            //change of activity
            Intent intent = new Intent(getApplicationContext(), ReportActivity.class);
            intent.putExtra("sec", seconds);
            intent.putExtra("totD", totalDistance);
            intent.putExtra("minA", minAltitude);
            intent.putExtra("maxA", maxAltitude);
            startActivity(intent);
            finish();
        }

        //start the recording
        else {

            //start the stopwatch
            running = true;

            //change the start button to the stop button
            recordButton.setImageResource(R.drawable.stopbutton);

            //create an arrayList to store the different points
            points = new ArrayList<>();

            //create the GPX file with the header
            createGPXFile();

            //start the tracking
            setListener();
            locationListener();
        }
    }

    //reset the stopwatch
    public void ResetRecording(View view) {
        //reset the variables
        running = false;
        seconds = 0;

        //change the image to the play button
        recordButton.setImageResource(R.drawable.playbutton);
    }

    //set the tracking to pause or play
    public void PauseRecording(View view) {
        //set pause
        if(running) {
            wasRunning = true;
            running = false;
            pauseButton.setImageResource(R.drawable.playbutton2);
        }
        //set resume
        else if(wasRunning){
            running = true;
            pauseButton.setImageResource(R.drawable.pausebutton);
        }
    }

    //updates the seconds variable and displays it on the screen
    private void runTimer() {

        //new handler
        Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {

                //if running, increment the seconds variable
                if(running)
                    seconds++;

                //split the seconds
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int sec = seconds % 60;

                //format the second into hours, minutes and seconds
                String time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, sec);

                //set the text view
                timeView.setText(time);

                //delays to 1 seconds
                handler.postDelayed(this, 1000);
            }
        });
    }

    //check if possible to store in external storage
    private boolean isExternalStorageAvailableForRW() {
        String extStorageState = Environment.getExternalStorageState();
        return extStorageState.equals(Environment.MEDIA_MOUNTED);
    }

    //create a GPX file
    public void createGPXFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        myExternalFile = new File(getExternalFilesDir("GPStracks"), sdf.format(new Date()) + ".gpx");

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\nxsi:schemaLocation=\"http://www.topografix.com/GPX/1/1\nhttp://www.topografix.com/GPX/1/1/gpx.xsd\">\n<trk>\n";
        String name = "<name>" + sdf.format(new Date()) + "</name>\n<trkseg>\n";

        try {
            FileWriter writer = new FileWriter(myExternalFile, true);
            writer.append(header);
            writer.append(name);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //add a point in the GPX file
    public void addPoints(String point) {
        try {
            FileWriter writer = new FileWriter(myExternalFile, true);
            writer.append(point);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //complete the end the GPX file
    public void endFile() {
        String footer = "</trkseg>\n</trk>\n</gpx>";

        try {
            FileWriter writer = new FileWriter(myExternalFile, true);
            writer.append(footer);
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    //create a new listener
    public void setListener() {

        try {
            listener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    float[] results = {0};
                    if (latitude != 0 && running) {

                        //get the distance between two points
                        android.location.Location.distanceBetween(latitude, longitude, location.getLatitude(), location.getLongitude(), results);

                        //add the distance to the total distance
                        totalDistance += results[0];

                        //add the speed between both points in the points table
                        float speed = (float) (results[0] / (seconds - lastS) * 3.6);

                        //if higher than 10, set the speed to 10 to not be out of the graph
                        if (speed > 10) points.add((float) 10);
                        else points.add(speed);

                        Log.d("Debug", totalDistance+"tot dist");
                        Log.d("Debug", seconds - lastS +" seconds");
                    }

                    //keep the last time to know the time between two points
                    lastS = seconds;

                    //get the current location
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    //get the min and maw altitude
                    if (maxAltitude == 0) {
                        minAltitude = location.getAltitude();
                        maxAltitude = location.getAltitude();
                    } else if (minAltitude > location.getAltitude())
                        minAltitude = location.getAltitude();
                    else if (maxAltitude < location.getAltitude())
                        maxAltitude = location.getAltitude();

                    //add the point in the GPX file
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    String point = "<trkpt lat=\"" + latitude + "\" lon=\"" + longitude + "\">\n<time>" + df.format(new Date()) + "</time>\n</trkpt>\n";

                    addPoints(point);
                }

                //if location not enable
                @Override
                public void onProviderDisabled(String provider) {
                    if (provider != LocationManager.GPS_PROVIDER) {
                        Toast.makeText(getApplicationContext(), "Please enable location services", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Location location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (provider == LocationManager.GPS_PROVIDER) {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
            };

        }catch(SecurityException se) {
            se.printStackTrace();
        }
    }

    private void locationListener() {
        try {
            locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);

        }catch(SecurityException se) {
            se.printStackTrace();
        }
    }

    //set the goal of the day
    public void SetGoal(View view) {

        //create a Dialog
        final AlertDialog.Builder myGoalPopup = new AlertDialog.Builder(activity);
        myGoalPopup.setTitle("What is your goal of the day ?");
        myGoalPopup.setIcon(R.drawable.goal);

        //the different choices
        String[] choices = new String[]{
                "15m",
                "30m",
                "45m",
                "1h",
                "1h15",
                "1h30",
                "1h45",
                "2h"};


        //the chosen choice
        final int[] checkedTime = {0};

        //the seconds associated
        final int[] secondsTime = new int[]{
                15,
                30,
                45,
                60,
                75,
                90,
                105,
                120};

        //set the choice and save it into checkTime variable
        myGoalPopup.setSingleChoiceItems(choices, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedTime[0] = which;
            }
        });


        //if okay, save the choice and display the right message
        myGoalPopup.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //change the message
                String goalText = "Your goal of the day is to make " + choices[checkedTime[0]] + " of training !\nYou can do it :)";
                dayGoal.setText(goalText);

                //retrieve the associated seconds
                secondsGoal = secondsTime[checkedTime[0]];
                setGoal = true;
            }
        });

        //if no, no goal set for the day, display the old message
        myGoalPopup.setNegativeButton("Set my goal later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setGoal = false;
                dayGoal.setText("Set your goal of the day");
            }
        });

        //display the dialog
        AlertDialog dialog = myGoalPopup.create();
        dialog.show();
    }
}