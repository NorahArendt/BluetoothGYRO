package com.example.nathan.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import java.io.*;

import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Switch Swh_blt;
    private Button btn_clear;
    private TextView mBluetoothStatus;
    private TextView show_gsX, show_gsY, show_gsZ, show_ItvX, show_ItvY, show_ItvZ, show_maxItvX, show_maxItvY, show_maxItvZ;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;

    private Handler mHandler; // Our main handler that will receive callback notifications

    private String adress_bt = null;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    private final String TAG = MainActivity.class.getSimpleName();

    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data

    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private final int SETTING_0 = Menu.FIRST;
    private final int SETTING_1 = Menu.FIRST + 1;


    private float x_max = 0, x_min = 0, y_max = 0, y_min = 0, z_max = 0, z_min = 0, resultCount = 0, x_max_interval = 0, y_max_interval = 0, z_max_interval = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        setTitle("Bluetooth gyroscope");

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);

        Swh_blt = (Switch) findViewById(R.id.blt_switch);

        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = "";

                    byte[] a = (byte[]) msg.obj;

                    for(int i = 0; i < 199; i++)
                        if (String.format("%02x", a[i]).equals("55") & String.format("%02x", a[i+1]).equals("52")) {
                            resultCount = resultCount + 1;
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("%02x", a[i+0]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+1]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+2]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+3]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+4]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+5]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+6]));
                            sb.append(" ");
                            sb.append(String.format("%02x", a[i+7]));
                            String result = sb.toString();
                            mBluetoothStatus.setText("HEX values:" + result);

                            int GS_X = Integer.parseInt(String.format("%02x", a[i+3]) + String.format("%02x", a[i+2]), 16);
                            int GS_Y = Integer.parseInt(String.format("%02x", a[i+5]) + String.format("%02x", a[i+4]), 16);
                            int GS_Z = Integer.parseInt(String.format("%02x", a[i+7]) + String.format("%02x", a[i+6]), 16);


                            if(GS_X >= 32768)
                                GS_X = -(65536 - GS_X);
                            if(GS_Y >= 32768)
                                GS_Y = -(65536 - GS_Y);
                            if(GS_Z >= 32768)
                                GS_Z = -(65536 - GS_Z);

                            float resultX = ((float)GS_X / 32768)*2000;
                            float resultY = ((float)GS_Y / 32768)*2000;
                            float resultZ = ((float)GS_Z / 32768)*2000;

                            if (resultX > x_max)
                                x_max = resultX;
                            if (resultX < x_min)
                                x_min = resultX;

                            if (resultY > y_max)
                                y_max = resultY;
                            if (resultY < y_min)
                                y_min = resultY;

                            if (resultZ > z_max)
                                z_max = resultZ;
                            if (resultZ < z_min)
                                z_min = resultZ;

                            if (x_max - x_min > x_max_interval) {
                                x_max_interval = x_max - x_min;
                                show_maxItvX.setText("X max interval:\n" + String.format("%.6f", x_max_interval));
                            }
                            if (y_max - y_min > y_max_interval) {
                                y_max_interval = y_max - y_min;
                                show_maxItvY.setText("Y max interval:\n" + String.format("%.6f", y_max_interval));
                            }
                            if (z_max - z_min > z_max_interval) {
                                z_max_interval = z_max - z_min;
                                show_maxItvZ.setText("Z max interval:\n" + String.format("%.6f", z_max_interval));
                            }

                            if(resultCount > 12) {
                                show_ItvX.setText("X max: " + String.format("%.6f", x_max) + "\nX min: " + String.format("%.6f", x_min) + "\nX Interval: " + String.format("%.6f", x_max - x_min));
                                show_ItvY.setText("Y max: " + String.format("%.6f", y_max) + "\nY min: " + String.format("%.6f", y_min) + "\nY Interval: " + String.format("%.6f", y_max - y_min));
                                show_ItvZ.setText("Z max: " + String.format("%.6f", z_max) + "\nZ min: " + String.format("%.6f", z_min) + "\nZ Interval: " + String.format("%.6f", z_max - z_min));

                                // initialize values
                                resultCount = 0;
                                x_max = 0;
                                x_min = 0;
                                y_max = 0;
                                y_min = 0;
                                z_max = 0;
                                z_min = 0;
                            }


                            show_gsX.setText("X axis: " + String.format("%.6f", resultX ) );
                            show_gsY.setText("Y axis: " + String.format("%.6f", resultY ) );
                            show_gsZ.setText("Z axis: " + String.format("%.6f", resultZ ) );

                        }
                    /*
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }*/

                    //mBluetoothStatus.setText(result);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };


        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);

        show_gsX = (TextView)findViewById(R.id.txt_gsX);
        show_gsY = (TextView)findViewById(R.id.txt_gsY);
        show_gsZ = (TextView)findViewById(R.id.txt_gsZ);
        show_ItvX = (TextView)findViewById(R.id.Itv_dataX);
        show_ItvY = (TextView)findViewById(R.id.Itv_dataY);
        show_ItvZ = (TextView)findViewById(R.id.Itv_dataZ);
        show_maxItvX = (TextView)findViewById(R.id.Itv_maxDataX);
        show_maxItvY = (TextView)findViewById(R.id.Itv_maxDataY);
        show_maxItvZ = (TextView)findViewById(R.id.Itv_maxDataZ);

        btn_clear = (Button)findViewById(R.id.btn_clearItv);

        mDevicesListView = (ListView)findViewById(R.id.listView1);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        Swh_blt.setOnClickListener(new buttonOnClickListener());
        btn_clear.setOnClickListener(new buttonOnClickListener());
        this.registerReceiver(mReceiver, makeFilter());

        if(mBTAdapter.isEnabled()) {
            Swh_blt.setChecked(true);
            listPairedDevices(this.getCurrentFocus());
            mBluetoothStatus.setText("Bluetooth is Enabled");
        }
        else {
            Swh_blt.setChecked(false);
            mBluetoothStatus.setText("Bluetooth is Disabled");
        }
    }


    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                Swh_blt.setChecked(true);
            }
            else
                Swh_blt.setChecked(false);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }

            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.e("TAG", "TURNING_ON");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Swh_blt.setChecked(true);
                            mBluetoothStatus.setText("Bluetooth is Enabled");
                            listPairedDevices( getWindow().getDecorView().findViewById(android.R.id.content) );
                            Log.e("TAG", "STATE_ON");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e("TAG", "STATE_TURNING_OFF");
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Swh_blt.setChecked(false);
                            mBluetoothStatus.setText("Bluetooth is Disabled");

                            mBTArrayAdapter.clear();
                            mDevicesListView.deferNotifyDataSetChanged();
                            Log.e("TAG", "STATE_OFF");
                            break;
                    }
                    break;
            }
        }
    };

    public class buttonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.blt_switch:
                    if(Swh_blt.isChecked())
                        bluetoothOn(v);
                    else
                        bluetoothOff(v);
                    break;
                case R.id.btn_clearItv:
                    x_max_interval = 0;
                    y_max_interval = 0;
                    z_max_interval = 0;
                    show_maxItvX.setText("X max interval:\n" + String.format("%.6f", x_max_interval));
                    show_maxItvY.setText("Y max interval:\n" + String.format("%.6f", y_max_interval));
                    show_maxItvZ.setText("Z max interval:\n" + String.format("%.6f", z_max_interval));
                    break;
            }
        }
    }


    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off

    }
    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices) {
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }



    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        buffer = new byte[200];
                        //SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, SETTING_0, 0, "Close connected");
        menu.add(0, SETTING_1, 1, "About ..");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case SETTING_0:  // Close connected
                if(mConnectedThread != null)
                    mConnectedThread.cancel();
                return true;

            case SETTING_1:  // About..
                new AlertDialog.Builder(this)
                        .setTitle("關於APP .. (v1.1)")
                        .setMessage("用於測試抓取藍牙陀螺儀(維特智能 型號：BWT61PCL)\n\n" +
                                "主要多出計算並顯示最大、最小、與其(最大)區間值(可清除)，在APP下螢幕保持清醒。\n\n" +
                                "修正螢幕翻轉時程式不閃跳。")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // do something
                            }
                        }).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // if user keydown the back, request user want to leave this APP or not
        if(keyCode == KeyEvent.KEYCODE_BACK){
            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth gyroscope")
                    .setMessage("Do you leave the APP?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    }).show();
            return true;
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }

}
