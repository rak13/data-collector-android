package com.example.rakib.datacollector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public LocationManager locationManager = null;
    public LocationListener locationListener;
    public LocationListener locationListener2;

    public String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public long MIN_TIME = 1000; //
    public float MIN_DISTANCE = (float) 0.2;

    Timer timer = null;

    public String IMEI = null;
    public String DEVICEID = null;
    public String ID = null;


    public SensorManager sensorManager = null;
    public SensorEventListener sensorListener = null;

    public long prevLocTime = -1;
    public long prevTime = 0;
    public long curTime = 0;

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Ouch !!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        Log.d("Android Version", " : "+currentapiVersion);

        if (currentapiVersion > 22){
            // Do something for versions above lollipop
            getPermissions();
        }

        final Button btnStart = (Button)findViewById(R.id.button1);
        btnStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                btnStart.setEnabled(false);
                btnStart.setText("Disabled");
                Log.d("Click", "Button 1 clicked");
                Toast.makeText(MainActivity.this, "Service Started", Toast.LENGTH_SHORT).show();
                init();

                int delay = 0; // delay for 0 sec.
                int period = 7*60*1000; // repeat every 7 minutes.
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask()
                {
                    public void run()
                    {
                        myNotification();
                    }
                }, delay, period);

            }
        });

        final Button btnStop = (Button)findViewById(R.id.button2);
        btnStop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Destroy();
                final CharSequence colors[] = new CharSequence[] {"Bus", "Car", "CNG", "Motor Bike", "Leguna/Tempo", "Rikshaw", "Cycle", "Walking", "CANCEL"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select Transport Mode");
                builder.setItems(colors, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("user clicked ", ""+colors[which]);

                        String str = ""+colors[which]+"\n";

                        writeString(str);

                        btnStart.setEnabled(true);
                        btnStart.setText("Start");
                    }
                });
                builder.show();
            }
        });
    }

    public void myNotification(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final AudioManager am =
                        (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                final int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
//                Log.e("***VOLUMEVOLUME***", " "+volume_level + " *******" + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                am.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                        0);
                // Do something after 5s = 5000ms
                MediaPlayer song;
                //then play it inside your onCreate
                song = MediaPlayer.create(MainActivity.this, R.raw.pld);
                song.setVolume(1, 1);
                song.start();

                song.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                volume_level,
                                0);
                    }
                });
            }
        }, 2000);
    }

    public void writeString(final String str){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
                try {
                    File file = new File(PATH + "/gps_acc_data.txt");
                    FileOutputStream fOut = new FileOutputStream(file, true);
                    fOut.write(str.getBytes());
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("WRITING DONE", "FINISHED");
            }
        }, 500);
    }

    public void init() {
        Log.d("in init()", "Initializing");

        //Location.........

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = telephonyManager.getDeviceId();

        DEVICEID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        ID = DEVICEID + IMEI;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showSettingsAlert();
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Toast.makeText(MyServices.this, "From OnLocation change1111: \n" + location, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //Toast.makeText( MyServices.this, "Status changed",  Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                //Toast.makeText( MyServices.this, "Provider Enabled",  Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                //Toast.makeText( MyServices.this, "Provider Disabled " + Toast.LENGTH_SHORT,  Toast.LENGTH_SHORT).show();
                showSettingsAlert();
            }
        };

        locationListener2 = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Toast.makeText(MyServices.this, "From OnLocation chang2222: \n" + location, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //Toast.makeText( MyServices.this, "Status changed",  Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                //Toast.makeText( MyServices.this, "Provider Enabled",  Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                //Toast.makeText( MyServices.this, "Provider Disabled " + Toast.LENGTH_SHORT,  Toast.LENGTH_SHORT).show();
                showSettingsAlert();
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permissions 1", "PROBLEMS RETURNING");
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 101, (float) MIN_DISTANCE, locationListener);

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 101, (float) MIN_DISTANCE, locationListener2);

        //Acceleromemter
        prevTime = System.currentTimeMillis();

        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    curTime = System.currentTimeMillis();
//                    Log.d("Time = ", "" + (curTime - prevTime) + " > " + MIN_TIME);

                    if (curTime - prevTime > MIN_TIME) {
                        writeToFile(event);
                        prevTime = curTime;
                    }

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

        Log.d("in init()", "Initialized");
    }

    public void showSettingsAlert() {
        Toast.makeText(this, "Please Enable GPS", Toast.LENGTH_LONG).show();
    }


    public void writeToFile(SensorEvent event) {
        double ax, ay, az;
        ax = event.values[0];
        ay = event.values[1];
        az = event.values[2];


        String str = ID + " " + System.currentTimeMillis() + " " + ax + " " + ay + " " + az;
        String acc = "Acc Readings - \nax: " + ax + "\nay: " + ay + "\naz: " + az + "\n";

        Location loc[] = new Location[3];

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d("Permissions", "RETURNING");
            return;
        }
        loc[1] = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        loc[2] = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);


        Location pass1 = null, pass2 = null, location = null;

        if (loc[1] != null && loc[1].getTime() > prevLocTime) pass1 = loc[1];
        if (loc[2] != null && loc[2].getTime() > prevLocTime) pass2 = loc[2];


//
        if (pass1 != null && pass2 != null) {
            if (pass1.getAccuracy() < pass2.getAccuracy()) {
                location = pass1;
            } else location = pass2;
        } else {
            if (pass1 != null) location = pass1;
            else location = pass2;
        }
        if (location != null) {
//            Toast.makeText(MyServices.this, acc + "\n\n" + location, Toast.LENGTH_LONG).show();
//            Log.d("Acc + Locations", acc + "\n\n" + location);
            str = str + " " + location.getLatitude() + " " + location.getLongitude() + " "
                    + location.getBearing() + " " + location.getTime() + " " +location.getAltitude()+" "+location.getSpeed() + " "
                    + location.getAccuracy() + " " + location.getProvider() + " " + location.getExtras() + "\n";

            prevLocTime = location.getTime();
        } else {
//            Toast.makeText(MyServices.this, acc + "\n\nNO GPS DATA", Toast.LENGTH_LONG).show();
            str = str + "\n";
        }
        try {
            File file = new File(PATH + "/gps_acc_data.txt");
            FileOutputStream fOut = new FileOutputStream(file, true);
            fOut.write(str.getBytes());
            fOut.close();
            Log.d("Acc + Locations", acc + "\n\n" + location);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void Destroy() {
        Log.d("in Destroy()", "Stopped....");
        if(timer != null) timer.cancel();
        if(sensorListener != null) {
            sensorManager.unregisterListener(sensorListener);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d("Permissions 2", "PROBLEMS RETURNING");
            return;
        }
        if(locationManager != null) {
            if (locationListener != null) locationManager.removeUpdates(locationListener);
            if (locationListener2 != null) locationManager.removeUpdates(locationListener2);
        }
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }


    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    @TargetApi(Build.VERSION_CODES.M)
    private void getPermissions() {
        Log.e("GETPERMISSIONS", "INSIDE GETPERMISSIONS");
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("FINE_LOCATION");
        if (!addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE))
            permissionsNeeded.add("READ_PHONE_STATE");
        if (!addPermission(permissionsList, Manifest.permission.INTERNET))
            permissionsNeeded.add("INTERNET");
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("WRITE_EXTERNAL_STORAGE");


        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                Toast.makeText(this, message,  Toast.LENGTH_LONG).show();
                Log.d("Message", message);

                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.INTERNET, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "All Permissions Granted");
                    // All Permissions Granted
                }
                else {
                    // Permission Denied
                    Log.d("Permissions", "Some Permission is Denied");
                    for (int i = 0; i < permissions.length; i++){
                        if(grantResults[i]!=PackageManager.PERMISSION_GRANTED){
                            Log.d("No permssion : ", permissions[i]);
                        }

                    }
                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
