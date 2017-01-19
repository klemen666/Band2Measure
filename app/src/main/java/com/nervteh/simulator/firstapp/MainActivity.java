package com.nervteh.simulator.firstapp;

import android.content.Context;
import android.content.DialogInterface;
import android.icu.math.BigDecimal;
import android.net.wifi.WifiManager;
import android.os.BadParcelableException;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.jar.Attributes;

import static android.R.attr.data;
import static android.R.attr.logo;
import static android.R.attr.name;

public class MainActivity extends AppCompatActivity {

    private BandClient client = null;

    TextView heartRateText;
    TextView heartRateQuality;
    TextView gsrText;
    TextView skinTempText;
    TextView intervalRRText;
    File outputFileHR;
    File outputFileGSR;
    File outputFileRR;
    File outputFileTemp;
    EditText hostText;
    EditText portText;
    EditText userInfo;

    DatagramSocket socket = null;
    int port = 8888;
    InetAddress host;

    // naslov in port, ki ga dobimo iz java aplikacije
    InetAddress PChost;

    static boolean serverRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heartRateText = (TextView) findViewById(R.id.heartRateText);
        heartRateQuality = (TextView) findViewById(R.id.heartRateQuality);
        gsrText = (TextView) findViewById(R.id.gsrText);
        skinTempText = (TextView) findViewById(R.id.skinTempText);
        intervalRRText = (TextView) findViewById(R.id.intervalRRText);
        hostText = (EditText) findViewById(R.id.ipAddress);
        portText = (EditText) findViewById(R.id.hostPort);
        userInfo = (EditText) findViewById(R.id.userInfo);

        serverRunning = true;

        try {
            new BandConnectTask().execute();
        }catch (Exception e){
            e.printStackTrace();
            Log.d("Band", "Can't connect to band!");
        }
        connectUDP();
        //new ListenUDPAsyncTask().execute();

        Thread threadListenUDP = new Thread(new ListenUDPThread());
        threadListenUDP.start();

    }

    private void connectUDP(){

        try {

            socket = new DatagramSocket(port);

            /*
            if (socket == null) {
                socket = new DatagramSocket(port);
                socket.setBroadcast(true);
            }

            // usvarimo novo nit, kjer poslušamo ukaze iz java aplikacije
            //listenUDPTask(this);

            String hostIp = "";

            // če ne dobimo naslova iz java aplikacije
            if (host == null) {

                hostIp = String.valueOf(hostText.getText());

                if (portText.getText().toString().length() > 0) {
                    port = Integer.parseInt(portText.getText().toString());
                } else {
                    Toast.makeText(this, "Vnesite številko vrat!", Toast.LENGTH_LONG).show();
                }

                Log.d("UDP", hostIp);
                Log.d("UDP", String.valueOf(port));

                if (hostIp == "" || hostIp == null) {
                    Toast.makeText(this, "Vnesite IP naslov strežnika!", Toast.LENGTH_LONG).show();
                } else {
                    socket = new DatagramSocket();
                    host = InetAddress.getByName(hostIp);
                }


            }
            */

            }   catch(SocketException e){
                e.printStackTrace();
            }


        /*catch(UnknownHostException e){
                e.printStackTrace();
            }
       */
        }


    public void toStart() {
        if(client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED){
            client.getSensorManager().requestHeartRateConsent(MainActivity.this, new HeartRateConsentListener() {
                @Override
                public void userAccepted(boolean consentGiven) {
                    Log.d("Band", "User permission granted!");
                    connectUDP();
                    createFiles();
                    startReceivingData();
                }
            });
        }
        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
            Log.d("Band", "User permission has already been granted!");
            connectUDP();
            createFiles();
            startReceivingData();
        }
    }

    public void start(View view) {
        if(client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED){
            client.getSensorManager().requestHeartRateConsent(MainActivity.this, new HeartRateConsentListener() {
                @Override
                public void userAccepted(boolean consentGiven) {
                    Log.d("Band", "User permission granted!");
                    connectUDP();
                    startReceivingData();
                    createFiles();
                }
            });
        }

        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
            Log.d("Band", "User permission has already been granted!");
            connectUDP();
            startReceivingData();
            createFiles();
        }
    }

    public void stop(View view) {
        Log.d("Band", "Stop receiving data!");
        heartRateText.setText("");
        heartRateQuality.setText("QUALITY: ");
        gsrText.setText("");
        skinTempText.setText("");
        intervalRRText.setText("");
        try {
            client.getSensorManager().unregisterHeartRateEventListener(heartRateListener);
            client.getSensorManager().unregisterGsrEventListener(gsrListener);
            client.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureListener);
            client.getSensorManager().unregisterRRIntervalEventListener(bandRRIntervalListener);

        } catch (BandIOException ex) {
            ex.printStackTrace();
        }
    }

    public void toStop() {
        Log.d("Band", "Stop receiving data!");
        /*
        heartRateText.setText("");
        heartRateQuality.setText("QUALITY: ");
        gsrText.setText("");
        skinTempText.setText("");
        intervalRRText.setText("");
        */

        try {
            client.getSensorManager().unregisterHeartRateEventListener(heartRateListener);
            client.getSensorManager().unregisterGsrEventListener(gsrListener);
            client.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureListener);
            client.getSensorManager().unregisterRRIntervalEventListener(bandRRIntervalListener);


        } catch (BandIOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        connectUDP();
        new BandConnectTask().execute();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        connectUDP();
        new BandConnectTask().execute();
    }

    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d("Band", "Stop receiving data!");
        heartRateText.setText("");
        heartRateQuality.setText("QUALITY: ");
        gsrText.setText("");
        skinTempText.setText("");
        intervalRRText.setText("");

        try {
            client.getSensorManager().unregisterHeartRateEventListener(heartRateListener);
            client.getSensorManager().unregisterGsrEventListener(gsrListener);
            client.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureListener);
            client.getSensorManager().unregisterRRIntervalEventListener(bandRRIntervalListener);

        } catch (BandIOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("Band", "Stop receiving data!");
        heartRateText.setText("");
        heartRateQuality.setText("QUALITY: ");
        gsrText.setText("");
        skinTempText.setText("");
        intervalRRText.setText("");
        try {
            client.getSensorManager().unregisterHeartRateEventListener(heartRateListener);
            client.getSensorManager().unregisterGsrEventListener(gsrListener);
            client.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureListener);
            client.getSensorManager().unregisterRRIntervalEventListener(bandRRIntervalListener);

        } catch (BandIOException ex) {
            ex.printStackTrace();
        }
    }

    public void createFiles(){
        if (isExternalStorageWritable()){
            Log.d("Storage", "Writable");

            if (userInfo != null ){

                String userInfoString = String.valueOf(userInfo.getText());

                final String path = Environment.getExternalStorageDirectory().toString();
                String timestampFile = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
                File dir = new File(path, "/measurements/" + timestampFile + "-" + userInfoString);
                String fileNameEnd = ".txt";

                if (!dir.exists()){
                    dir.mkdirs();
                }

                outputFileHR = new File(dir, "HRData-" + userInfoString + "-" + timestampFile + fileNameEnd);
                outputFileGSR = new File(dir, "GSRData-" + userInfoString + "-" + timestampFile + fileNameEnd);
                outputFileRR = new File(dir, "RRData-" + userInfoString + "-" + timestampFile + fileNameEnd);
                outputFileTemp = new File(dir, "TempData-" + userInfoString + "-" + timestampFile + fileNameEnd);

                try {
                    outputFileHR.createNewFile();
                    outputFileGSR.createNewFile();
                    outputFileRR.createNewFile();
                    outputFileTemp.createNewFile();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else{
                Toast.makeText(this, "Vnesite ime testa", Toast.LENGTH_LONG).show();
                return;
            }



        }
    }

    private void startReceivingData() {
        Log.d("Band", "Start receiving data!");
        try {
            client.getSensorManager().registerHeartRateEventListener(heartRateListener);
            client.getSensorManager().registerGsrEventListener(gsrListener);
            client.getSensorManager().registerSkinTemperatureEventListener(skinTemperatureListener);
            client.getSensorManager().registerRRIntervalEventListener(bandRRIntervalListener);

            //writeDataToFile();
        } catch (BandException e) {
            e.printStackTrace();
        } catch (InvalidBandVersionException e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private class BandConnectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {

                BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
                client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);

                ConnectionState state = client.connect().await();
                if (state == ConnectionState.CONNECTED) {
                    Log.d("Band", "Band connected!");
                }
                else {
                    Log.d("Band", "Band Connection refused!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BandException e) {
                e.printStackTrace();
            }catch (Exception e){
                e.printStackTrace();
                Log.d("Band", "Can't connect to band!");
                //Toast.makeText(getApplicationContext(), "Please connect to Band", Toast.LENGTH_LONG).show();
            }
            return null;
        }
    }

    final BandHeartRateEventListener heartRateListener = new BandHeartRateEventListener() {

        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    heartRateText.setText(String.valueOf(event.getHeartRate()));
                    heartRateQuality.setText("Quality: " + String.valueOf(event.getQuality()));
                }
            });

            //Log.d("HeartRateChanged", "HR: " + String.valueOf(event.getHeartRate()));
            try {
                String s = String.valueOf(event.getTimestamp()) + "," +  "HR," + String.valueOf(event.getHeartRate()) + "," + String.valueOf(event.getQuality());
                sendUDPData(s);

                FileOutputStream os = new FileOutputStream(outputFileHR, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(event.getTimestamp() + ","));
                outWriter.append(String.valueOf(event.getHeartRate() + ","));
                outWriter.append(String.valueOf(event.getQuality() + "\n"));
                outWriter.close();

                os.flush();
                os.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    final BandGsrEventListener gsrListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent bandGsrEvent) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gsrText.setText(String.valueOf(bandGsrEvent.getResistance()));
                }
            });

            try {

                String s = String.valueOf(bandGsrEvent.getTimestamp()) + "," +  "GSR," + String.valueOf(bandGsrEvent.getResistance());
                sendUDPData(s);

                FileOutputStream os = new FileOutputStream(outputFileGSR, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(bandGsrEvent.getTimestamp()) + ",");
                outWriter.append(String.valueOf(bandGsrEvent.getResistance()) + "\n");
                outWriter.close();

                os.flush();
                os.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    final BandSkinTemperatureEventListener skinTemperatureListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent bandSkinTemperatureEvent) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    skinTempText.setText(String.valueOf(bandSkinTemperatureEvent.getTemperature()));
                }
            });

            try {

                String s = String.valueOf(bandSkinTemperatureEvent.getTimestamp()) + "," +  "Temp," + String.valueOf(bandSkinTemperatureEvent.getTemperature());
                sendUDPData(s);

                FileOutputStream os = new FileOutputStream(outputFileTemp, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(bandSkinTemperatureEvent.getTimestamp()) + ",");
                outWriter.append(String.valueOf(bandSkinTemperatureEvent.getTemperature()) + "\n");
                outWriter.close();

                os.flush();
                os.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    final BandRRIntervalEventListener bandRRIntervalListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent bandRRIntervalEvent) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    intervalRRText.setText(String.valueOf(bandRRIntervalEvent.getInterval() * 1000));
                }
            });

            try {

                String s = String.valueOf(bandRRIntervalEvent.getTimestamp()) + "," +  "RR," + String.valueOf(bandRRIntervalEvent.getInterval());
                sendUDPData(s);

                FileOutputStream os = new FileOutputStream(outputFileRR, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(bandRRIntervalEvent.getTimestamp()) + ",");
                outWriter.append(String.valueOf(bandRRIntervalEvent.getInterval() * 1000 + "\n"));
                outWriter.close();

                os.flush();
                os.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    // Funkcija, ki pošilja UDP Datagrame s določenim tekstom
    public void sendUDPData(String s){

        byte[] b = s.getBytes();
        DatagramPacket dp = new DatagramPacket(b, b.length, host, port);
        try {
            socket.send(dp);
            Log.d("SEND", s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //funkcija, ki posluša na UDP portu za "START" in "STOP" ukaze
    public class ListenUDPThread implements Runnable {

        String message = "";

        public void run() {

            while(serverRunning) {

                Log.d("UDP", "Tread start");


                byte[] buffer = new byte[65536];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    message = new String(buffer, 0, packet.getLength());
                    Log.d("UDP", message);

                    if (message.contains("START")){
                        Log.d("UDP", "Dobil sem START!");
                        host = packet.getAddress();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                userInfo.setText(message.substring(6));
                                Log.d("UDP", "User info: " + String.valueOf(userInfo.getText()));
                            }
                        });

                        toStart();

                    }

                    if (message.contains("STOP")){
                        Log.d("UDP", "Dobil sem STOP!");

                        toStop();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                heartRateText.setText("");
                                heartRateQuality.setText("QUALITY: ");
                                gsrText.setText("");
                                skinTempText.setText("");
                                intervalRRText.setText("");
                            }
                        });

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /*
        //funkcija, ki posluša na UDP portu za "START" in "STOP" ukaze
        private void listenUDPTask(final MainActivity mainActivity) {

            new Thread(new Runnable() {

                @Override
                public void run() {
                    Log.d("UDP", "Tread start");
                    String message = "";
                    byte[] buffer = new byte[65536];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {

                        while (serverRunning){
                            Log.d("UDP", "Listening....");

                            socket.receive(packet);

                            message = new String(buffer, 0, packet.getLength());
                            Log.d("UDP", message);

                            if (message.contains("START")){
                                Log.d("UDP", "Dobil sem START!");
                                host = packet.getAddress();
                                mainActivity.toStart();
                            }

                            if (message.contains("STOP")){
                                Log.d("UDP", "Dobil sem STOP!");
                                toStop();

                                  runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toStop();
                                        }
                                });

                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }

    */
    private class ListenUDPAsyncTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            Log.d("UDP", "AsyncTask started");
            String message = "";
            byte[] buffer = new byte[65536];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (serverRunning) {
                try {
                    Log.d("UDP", "Listening....");
                    socket.receive(packet);
                    message = new String(buffer, 0, packet.getLength());
                    //message = new String(packet.getData());
                    Log.d("UDP", "Message is: " + message);

                    /*
                    if (message.contains("START")){
                        Log.d("UDP", "Dobil sem START!");
                        host = packet.getAddress();
                        toStart();
                    }

                    if (message.contains("STOP")){
                        Log.d("UDP", "Dobil sem STOP!");
                        toStop();

                             /*   runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        toStop();
                                    }


                    }
                   */

                } catch (Exception e) {
                    e.printStackTrace();
                }

                }

            return message;
            }

        @Override
        protected void onPostExecute(String message) {
            if (message != null) {
                //print text
                Log.d("UDP", "PostExecute: " + message);
            }
        }
    }


}
