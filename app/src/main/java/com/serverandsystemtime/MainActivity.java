package com.serverandsystemtime;

import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;

    private static NTPUDPClient timeClient = null;
    private static InetAddress inetAddress = null;
    private static TimeInfo timeInfo = null;
    private static long returnTime;
    public static Boolean offline = true;
    private Handler hUpdate;
    private Runnable rUpdate;
    String time = "Loading...";
    int loops = 0;
    public static final String TIME_SERVER = "time-a.nist.gov";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button= findViewById(R.id.button);
        textView=findViewById(R.id.textView);

        hUpdate = new Handler();
        rUpdate = () -> {
            System.out.println("Updating UI from handler");
            textView.setText(time);
        };
        textView.setText(time);

        Thread tUpdate = new Thread() {
            public void run() {
                while(true) {
                    loops++;
                    System.out.println("Loop: " + loops);
                    hUpdate.post(rUpdate);
                    try {
                        sleep(5000);
                        System.out.println("Going to update time");
                        SimpleDateFormat sdf = new SimpleDateFormat("hh-mm-ss a");
                        time = sdf.format(getCurrentNetworkTime());
                        System.out.println("Should have updated time");
                    } catch (InterruptedException e) {
                        System.out.println("Error updating time");
                        e.printStackTrace();
                    }
                }
            }
        };
        tUpdate.start();
        button.setOnClickListener(view -> {
            if (offline) {
                if (getInternetConnection()) {
                    offline = false;
                    button.setText("Online");
                } else {
                    button.setText("No internet");
                }
            } else {
                offline = true;
                button.setText("Offline");
            }
        });




    }

    public   boolean getInternetConnection() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
        for(NetworkInfo info:networkInfos){
            if (info.getTypeName().equalsIgnoreCase("WIFI"))if (info.isConnected())connected = true;
            if (info.getTypeName().equalsIgnoreCase("MOBILE DATA"))if (info.isConnected())connected = true;
        }
        if (connected) {
            System.out.println("Have connection, device is online");
            return true;
        } else {
            System.out.println("No connection, device is offline");
            return false;
        }
    }

    public Date getCurrentNetworkTime() {
        if (timeClient == null && !offline) {
            timeClient = new NTPUDPClient();
            System.out.println("Created time client");
            try {
                inetAddress = InetAddress.getByName(TIME_SERVER);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            System.out.println("Got inet address");
        }
        int tries = 0;
        while(tries < 5) {
            tries++;
            try {
                if (offline) {
                    System.out.println("Offline");
                    return new Date(System.currentTimeMillis());
                }
                timeClient.open();
                timeClient.setSoTimeout(2000);
                System.out.println("Trying to get online time (often gets timed out, will try 5 times)");
                // This sometime gets timed out, current workaround is to just try again
                // TODO: Find a better way to handle this
                timeInfo = timeClient.getTime(inetAddress);
                System.out.println("Got time info");
                returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
                System.out.println("Got return time");
                break;
            } catch (IOException e) {
                System.out.println("Error getting time");
                if (tries == 5) {
                    returnTime = System.currentTimeMillis();
                    System.out.println("Using system time");
                }
                e.printStackTrace();
            }
        }
        return new Date(returnTime);
    }

}