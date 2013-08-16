package com.example.glassremote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;



public class ConnectionManager {
	//TODO: Set master in constructor 
	MainActivity master;
	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket = null;
    BluetoothDevice serverDevice;
    public ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread = null;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private boolean taskComplete = false;
    Boolean isDisconnectedOnPurpose = false;
    int REQUEST_ENABLE_BT;
    boolean registered = false;
    private final String REQUEST_OBJECTS = "FF0000000";
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final boolean D = true;
    private String connectionAddress;
    public final int LATENCY = 100;
    private long lastWriteTime= -1L;
	
    private boolean isConnected = false;
	public ConnectionManager (MainActivity master){
		this.master=master;
		//ARDUINO ADDRESS
		this.connectionAddress="00:A0:96:13:58:5E";
	}
	
	//called from mainActivity to kick off connection
	public boolean start(){
		enableBlueTooth();
		queryDevices();
		return true;
	}
	
    public void enableBlueTooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            // Device is not connected to BlueTooth
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
           master.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    
    
    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (D) {
                    Log.i("debugging", "Device is: " + device.getName());
                    Log.i("debugging",
                            "Device address:  " + device.getAddress());
                }

                // Add the name and address to an array adapter to show in a
                // ListView
                if (device.getAddress().equals(connectionAddress)) {
                    // connectToDevice(device);
                    if (D)
                        Log.i("debugging", "started the connection task");
                    serverDevice = device;
                    mBluetoothAdapter.cancelDiscovery();
                    try {
                        new ConnectThread(serverDevice).run();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            }
        }
    };



    public boolean getIsConnected (){
    	return isConnected;
    }

    

public void queryDevices() {
    Log.i("debugging", "querying devices");
    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
            .getBondedDevices();
    // If there are paired devices
    boolean found = false;
    if (pairedDevices.size() > 0) {
        // Loop through paired devices
        for (BluetoothDevice device : pairedDevices) {
            Log.i("debugging", "in for, we want device address: "+ connectionAddress);
            if (D) {
                Log.i("debugging", "Device is: " + device.getName());
                Log.i("debugging",
                        "Device address:  " + device.getAddress());
            }

            // Add the name and address to an array adapter to show in a
            // ListView
            if (device.getAddress().equals(connectionAddress)) {
                // connectToDevice(device);
                Log.i("debugging", "starting connection task");
                found = true;
                serverDevice = device;
                try {
                    new ConnectThread(serverDevice).run();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            }
        }

    }
    if (!found) {
        Log.i("debugging", "starting discovery");
        mBluetoothAdapter.startDiscovery();
    }

    // Register the BroadcastReceiver
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    master.registerReceiver(mReceiver, filter); // Don't forget to unregister

    // during onDestroy
    registered = true;

}

public void destroy(){
		if (mReceiver !=null){
		master.unregisterReceiver(mReceiver);
		}
		if (mmSocket!=null){
			try {
				mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


private class ConnectThread extends Thread {
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) throws IOException,
    InterruptedException {

        // device.fetchUuidsWithSdp();
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        if (D)
            Log.i("debugging",
                    "Instantiating connect thread to " + device.getName());
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        if (Build.VERSION.SDK_INT < 9) { // VK:
            // Build.Version_Codes.GINGERBREAD
            // is not accessible yet so
            // using raw int value
            // VK: 9 is the API Level integer value for Gingerbread
            if (D)
                Log.i("debugging", "first build");
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } else {
            // GOGGLES USE THIS
            if (D)
                Log.i("debugging", "second build");
            Method m = null;
            try {

                m = device.getClass().getMethod(
                        "createInsecureRfcommSocketToServiceRecord",
                        new Class[] { UUID.class });
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {

                tmp = (BluetoothSocket) m.invoke(device, (UUID) MY_UUID);
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        mConnectThread = this;
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection

        if (mBluetoothAdapter.isDiscovering()) {
            if (D)
                Log.i("debugging", "canceled discovery in run");
            mBluetoothAdapter.cancelDiscovery();
        }

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
            isConnected = true;
            // startConnectionThread();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.i("debugging", "unable to connect in ConnectThread.run");
            connectException.printStackTrace();
            isConnected = false;
            //Toast toast = Toast.makeText(getApplicationContext(), "the server is not available", Toast.LENGTH_SHORT);
            //toast.show();
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            }
            return;
        }
        Log.i("debugging", "SUCCESSFULLY CONNECTED");
        
        //incomingMessage.setText("connected");

        connected(mmSocket);
        // Do work to manage the connection (in a separate thread)
        // manageConnectedSocket(mmSocket);
    }


    public synchronized void connected(BluetoothSocket socket) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread = null;
        }


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        if (D)
            Log.i("debugging", "Starting connected thread");
        mConnectedThread.start();
    
        Log.i("debugging", "name of socket device is: " + mmSocket.getRemoteDevice().getName());
        //whoSays.setText(mmSocket.getRemoteDevice().getName());

    }


}


private class ConnectedThread extends Thread {
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
        mConnectedThread = this;
        mmSocket = socket;




        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.i("debugging",
                    "exception in instantiation of ConnectThread");
            e.printStackTrace();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        taskComplete = true;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        // Keep listening to the InputStream while connected
        while (true) {
            try {
                // Read from the InputStream
                Log.i("debugging", "reading from inputstream");
                bytes = mmInStream.read(buffer);

                // Send the obtained bytes to the UI Activity
                Log.i("debugging", "obtaining message from handler");
                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                .sendToTarget();
            } catch (IOException e) {
                Log.i("debugging", "disconnected", e);
                if (!isDisconnectedOnPurpose){
                restartConnection();
                }
                //make a Toast that says connection is lost 
                // connectionLost();
                // Start the service over to restart listening mode
                // BluetoothChatService.this.start();
                break;
            }
        }
    }

    public void read() {
        byte[] buffer = new byte[1024]; // buffer store for the stream
        int bytes; // bytes returned from read()

        try {
            // Read from the InputStream
            bytes = mmInStream.read(buffer);
            // Send the obtained bytes to the UI activity
            mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
            .sendToTarget();
        } catch (IOException e) {

        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        Log.i("debugging", "in connectedthread.write writing " + new String(bytes));
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        /**
         * try { mmSocket.close(); } catch (IOException e) { }
         */
    }
}
/**
 * Start the chat service. Specifically start AcceptThread to begin a
 * session in listening (server) mode. Called by the Activity onResume() */
public synchronized void restartConnection() {
    if (D) Log.i("debugging", "restarting connection ");
    //incomingMessage.setText("not connected");
    // Cancel any thread attempting to make a connection
    if (mConnectThread != null) { mConnectThread = null;}

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {mConnectedThread = null;}


    // Start the whole dance over again
    start();
}

private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

        case MESSAGE_WRITE:
            Log.i("debugging", "message write in handler");
            byte[] writeBuf = (byte[]) msg.obj;
            // construct a string from the buffer
            String writeMessage = new String(writeBuf);
            break;
        case MESSAGE_READ:
            Log.i("debugging", "message read in handler");
            byte[] readBuf = (byte[]) msg.obj;
            // construct a string from the valid bytes in the buffer
            String readMessage = new String(readBuf, 0, msg.arg1);
            master.receive(readMessage);
           Log.i("connection", "read message was " + readMessage);
           //TODO: stuff with input

            break;

        }
    }
};

public void write(String s){
	if (master.getConnectingToLaptop()){
		long initialTime;
		long threshold = 300;

		if (s.substring(3, 6).equals("SEL")){
		//DELAY
			initialTime = Calendar.getInstance().getTimeInMillis();
			threshold = 100;
			while (Calendar.getInstance().getTimeInMillis() - initialTime < threshold){
				//SPIN
			}
		}

		long currentTime = Calendar.getInstance().getTimeInMillis();
		byte[] send = s.getBytes();
		try {
			mConnectedThread.write(send);
			lastWriteTime = currentTime;
			}
		catch (NullPointerException e){
			Log.i("debugging", "couldn't write message because unconnected");
			}
	
		if (s.substring(3, 6).equals("SEL")){
			initialTime = Calendar.getInstance().getTimeInMillis();
			while (Calendar.getInstance().getTimeInMillis() - initialTime < threshold){
		//SPIN
			}
		}
	}
	else {
	
		Log.i("debugging", "sent message: " + s);
		
	}
}



public void initialMessage(){
		//lastWriteTime = Calendar.getInstance().getTimeInMillis();
        write(REQUEST_OBJECTS);
        
}
public String formatMessage(ControlledObject object, Variable variable, char function, String... currentValue){
	String id = "" + object.getId();
	String var = variable.getAbbreviation();
	
	String val="XXX";
	if (id.length()==1){
		id = "0"+ id;
	}

	
	if (function != 'R'){
		

		
		
		if (currentValue[0].equals("off")){
			
			function = 'C';
			val = "OFF";
		}
		else if (variable.getName().equals("video")) {
			
			
			if (!currentValue[0].equals("INC") && !currentValue[0].equals("DEC") ){
				val = " ON";
				function = 'C';
			}
			else {
				function = 'C';
				val = currentValue[0];
			}
		
		}
		else if (currentValue[0].equals("on")){
			function = 'C';
			val = " ON";
		}	
		else {
			function = 'S';
			val = currentValue[0];
			Log.i("Debugging", "in write value is " + val);
			if (currentValue[0].length()==1){
				val = "00"+currentValue[0];
			}
			if (currentValue[0].length()==2){
				val = "0"+currentValue[0];
			}
		
	}
	
	

	}
	return id + function + var  + val+"\n";
	
}
}

