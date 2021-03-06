package com.nervteh.simulator.firstapp;

import android.content.DialogInterface;
import android.icu.math.BigDecimal;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.jar.Attributes;

import static android.R.attr.data;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heartRateText = (TextView) findViewById(R.id.heartRateText);
        heartRateQuality = (TextView) findViewById(R.id.heartRateQuality);
        gsrText = (TextView) findViewById(R.id.gsrText);
        skinTempText = (TextView) findViewById(R.id.skinTempText);
        intervalRRText = (TextView) findViewById(R.id.intervalRRText);

        new BandConnectTask().execute();


    }


    public void start(View view) {
        if(client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED){
            client.getSensorManager().requestHeartRateConsent(MainActivity.this, new HeartRateConsentListener() {
                @Override
                public void userAccepted(boolean consentGiven) {
                    Log.d("Band", "User permission granted!");
                    startReceivingData();
                    createFiles();

                }
            });
        }
        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
            Log.d("Band", "User permission has already been granted!");
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

    protected void onPause() {
        super.onPause();
        /*Log.d("Band", "Stop receiving data!");
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
        }*/
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
            final String path = Environment.getExternalStorageDirectory().toString();
            String timestampFile = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
            File dir = new File(path, "/measurements/" + timestampFile);
            String fileNameEnd = ".txt";

            if (!dir.exists()){
                dir.mkdirs();
            }

            outputFileHR = new File(dir, "HRData-" + timestampFile + fileNameEnd);
            outputFileGSR = new File(dir, "GSRData-" + timestampFile + fileNameEnd);
            outputFileRR = new File(dir, "RRData-" + timestampFile + fileNameEnd);
            outputFileTemp = new File(dir, "TempData-" + timestampFile + fileNameEnd);

                try {

                    outputFileHR.createNewFile();
                    outputFileGSR.createNewFile();
                    outputFileRR.createNewFile();
                    outputFileTemp.createNewFile();

                } catch (IOException e) {
                    e.printStackTrace();
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
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);

            try {
                ConnectionState state = client.connect().await();
                if (state == ConnectionState.CONNECTED) {
                    Log.d("Band", "Band connected!");

                }
                else {
                    Log.d("Band", "Coonection refused!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BandException e) {
                e.printStackTrace();
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

                FileOutputStream os = new FileOutputStream(outputFileHR, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(event.getTimestamp() + " - "));
                outWriter.append(String.valueOf(event.getHeartRate() + " - "));
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

                FileOutputStream os = new FileOutputStream(outputFileGSR, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(bandGsrEvent.getTimestamp()) + " - ");
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

                FileOutputStream os = new FileOutputStream(outputFileTemp, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(bandSkinTemperatureEvent.getTimestamp()) + " - ");
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

                FileOutputStream os = new FileOutputStream(outputFileRR, true);
                OutputStreamWriter outWriter = new OutputStreamWriter(os);
                outWriter.append(String.valueOf(bandRRIntervalEvent.getTimestamp()) + " - ");
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


}
