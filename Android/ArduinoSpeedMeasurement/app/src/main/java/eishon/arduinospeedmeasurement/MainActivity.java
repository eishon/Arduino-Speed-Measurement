package eishon.arduinospeedmeasurement;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{

    private static final String TAG = "SPEED_MEASUREMENT";

    Button btn_start,btn_stop;
    static TextView txt_sensor_dist,txt_time_dif,txt_speed;

    private static final int REQUEST_ENABLE_BT = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private InputStream inStream=null;
    private OutputStream outStream = null;
    private static final UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "98:D3:31:FD:0A:E5";
    //private static String address = "74:A5:28:5E:18:3B";

    String sensor_dist="0";
    String speed="0";
    String time_dif="0";

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    boolean flag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "In onCreate()");

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        btn_start= (Button) findViewById(R.id.btn_start);
        btn_stop= (Button) findViewById(R.id.btn_stop);

        txt_sensor_dist= (TextView) findViewById(R.id.textView_sensor_dist);
        txt_time_dif= (TextView) findViewById(R.id.textView_time_dif);
        txt_speed= (TextView) findViewById(R.id.textview_speed);

        btAdapter=BluetoothAdapter.getDefaultAdapter();

        checkBT();

        btn_start.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                flag=true;
                //dataThread=new DataThread(btSocket,"10");
                //dataThread.start();
                //report("flag- "+dataThread.threadFlag);
                report("Started");
                sendData("Started");
            }
        });
        btn_stop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                flag=false;
                //dataThread.stopDataThread();
                //report("flag- "+dataThread.threadFlag);
                report("Stopped");
                sendData("Stopped");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        connectBT();
    }


    @Override
    protected void onDestroy() {
        closeBT();
        super.onDestroy();
    }


    private void checkBT() {
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                report("Bluetooth On");
                Log.d(TAG, "...Bluetooth enabled...");
            } else {
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void connectBT(){
        Log.d(TAG, "...In connectBT() : Attempting client connect...");

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            report("Socket-"+MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In connectBT() : socket create failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "...Connecting to Remote...");

        try {
            btSocket.connect();
            report("Connected to - "+address);
            Log.d(TAG, "...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
                report("Socket Closed due to IO");
            } catch (IOException e2) {
                errorExit("Fatal Error", "In connectBT() : unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        Log.d(TAG, "...Creating Socket...");



        try {
            inStream=btSocket.getInputStream();
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In connectBT() : output stream creation failed:" + e.getMessage() + ".");
        }

        receive_data();

    }

    private void closeBT(){
        Log.d(TAG, "...In closeBT()...");

        if (inStream != null) {
            try {
                inStream.close();
                report("Instream Closed");
            } catch (IOException e) {
                errorExit("Fatal Error", "In closeBT() and failed to close input stream: " + e.getMessage() + ".");
            }
        }

        if (outStream != null) {
            try {
                outStream.flush();
                report("Outstream Flushed");
            } catch (IOException e) {
                errorExit("Fatal Error", "In closeBT() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
            report("Socket Closed");
        } catch (IOException e2) {
            errorExit("Fatal Error", "In closeBT() and failed to close socket." + e2.getMessage() + ".");
        }
    }


    private void errorExit(String title, String message){
        Toast msg = Toast.makeText(getBaseContext(),
                title + " - " + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    private void report(String message){
        Toast msg = Toast.makeText(getBaseContext(),
                message, Toast.LENGTH_SHORT);
        msg.show();
    }

    void receive_data()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            //txt_speed.setText(data);
                                            if(flag){
                                                data_processing(data);
                                                txt_speed.setText(speed);
                                                txt_sensor_dist.setText(sensor_dist);
                                                txt_time_dif.setText(time_dif);
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void data_processing(String message){
        String[] split_msg=message.split("\\s+");
        sensor_dist=split_msg[0];
        time_dif=split_msg[1];
        speed=split_msg[2];
    }

    private void sendData(String message){
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Sending data: " + message + "...");
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In sendData() and an exception occurred during write: " + e.getMessage();
            errorExit("Fatal Error", msg);
        }
    }

}