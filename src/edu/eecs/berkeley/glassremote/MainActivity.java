package edu.eecs.berkeley.glassremote;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import edu.eecs.berkeley.glassremote.R;
import android.R.color;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


// This main activity maintains the BT connection
// takes charge of the mode
// by default, the app does nothing but only display "move to select\n touch to adjust\n tap to connect"
// when onDown is detected, we switch to quasi-mode where we monitor the changes of head orientation
  // a local algorithm is performed to determine which is the next target, this information is sent back to IR through BT
  // we need a list of physical targets linked by the adjacent map data structure
// when onUp is detected, we use the current selected one
  // this could happen fairly soon after onDown, or longer, it doens't matter
  // swipe down will go back (un-select)

// main work now is how to support these in the UI

public class MainActivity extends Activity {

  // Debugging
  private static final String TAG = "GlassRemote";
  private static final boolean D = true;
  
  // Message types sent from the BluetoothChatService Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_TOAST = 5;
  
  // Key names received from the BluetoothChatService Handler
  public static final String DEVICE_NAME = "device_name";
  public static final String TOAST = "toast";
  
  // target MAC address
  private String btModuleAddress = "00:06:66:66:24:C3";
  private BluetoothAdapter mBluetoothAdapter = null;
  
  // disable sleep in the app
  protected PowerManager.WakeLock mWakeLock;

  // when toConnect is true, the Glass will connect to BT
  // otherwise, it will pretend to
  public boolean toConnect = true;
  
  //Name of the connected device
  private String mConnectedDeviceName = null;   

  // NAVIGATION CONTROL
  // TWO NAVIGATES = TRUE means switching with 2 fingers, scrolling with 1
  public boolean twoNavigates = false;

  int flingRight = 0;
  int flingLeft = 0;

  // LAYOUT
  TextView nameOfObject;
  TextView message;
  ImageView currentAlert;
  TextView valueOfVariable;
  ImageView rewindButton;
  ImageView toggleButton;
  ImageView fastForwardButton;
  int textSize = 35;
  LinearLayout currentVariableLayout;
  int paddingAmount = 30;
  int selectedColor;
  int unSelectedColor;
  int fadedColor;
  float outOfFocus;

  /**
   * STATES: LIMBO: waiting for a connection request ROOM: multiple objects
   * OBJECT: we are connected to an object
   */

  private final int LIMBO = 0;
  private final int ROOM_LEVEL = 1;
  private final int OBJECT_LEVEL = 2;
  private int level;

  // VARIABLE CONTROL
  private ArrayList<PhysicalTarget> room;   // room list out all the available targets in this room
  private HashMap<Integer, PhysicalTarget> objects;
  private PhysicalTarget currentObject;
  private Variable currentVariable;
  private int varIndex = 0;
  private int objectIndex = 0;

  // MESSAGING
  private BluetoothChatService mConnectionManager = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if(D) Log.e(TAG, "+++ ON CREATE +++");

    // This code together with the one in onDestroy() will make the screen
    // be always on until this Activity gets destroyed.
    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "GlassRemote WakeLock Tag");
    this.mWakeLock.acquire();

    // start in limbo
    level = LIMBO;
    resetContentView();
    selectedColor = getResources().getColor(color.holo_blue_dark);
    fadedColor = getResources().getColor(R.color.white_transparent);
    outOfFocus = .5f;
    
    nameOfObject = (TextView) findViewById(R.id.name_of_object);
    message = (TextView) findViewById(R.id.message);
   
  }

  
  
  @Override
  protected void onStart() {
    super.onStart();
    if(D) Log.e(TAG, "++ ON START ++");

    // load all the targets
    initializeObjects();
//
//    if (mConnectionManager == null) 
//      setupBluetooth();
//    
//    // Get local Bluetooth adapter
//    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//    // Get the BluetoothDevice object
//    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice( btModuleAddress );
//    // Attempt to connect to the device
//    // false means insecure connection, which doesn't matter now
//    mConnectionManager.connect(device, false);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  public void resetContentView() {
    switch (level) {
    case (LIMBO):
      setContentView(R.layout.activity_main);
    break;
    case (ROOM_LEVEL):
      setContentView(R.layout.room_activity);
    break;
    case (OBJECT_LEVEL):
      setContentView(R.layout.object_activity);
    // add objects name
    if (toConnect) {
      nameOfObject.setText("connected to " + String.format("%02d", currentObject.getId()));
    }
    break;
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if(D) Log.e(TAG, "-- ON STOP --");
  }

  @Override
  protected void onPause() {
    super.onPause();
    if(D) Log.e(TAG, "- ON PAUSE -");

    if (this.mWakeLock != null && (this.mWakeLock.isHeld() == true)) {
      this.mWakeLock.release();
      this.mWakeLock = null;
    }

  }

  @Override
  protected void onResume() {
    super.onResume();
    if(D) Log.e(TAG, "+ ON RESUME +");
    
    // when on resume, we might need to acquire the lock again
    if ((this.mWakeLock != null) &&           // we have a WakeLock
	(this.mWakeLock.isHeld() == false)) {  // but we don't hold it 
      this.mWakeLock.acquire();
    }
  }

  private void setupBluetooth() {
    Log.d(TAG, "setupBluetooth()");

    // Initialize the BluetoothChatService to perform bluetooth connections
    mConnectionManager = new BluetoothChatService(this, mHandler);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if(D) Log.e(TAG, "--- ON DESTROY ---");

    if (mConnectionManager != null)
      mConnectionManager.stop();
  }
  
  /**
   * Sends a message.
   * @param message  A string of text to send.
   */
  private void sendMessage(String message) {
    // Check that we're actually connected before trying anything
    if (mConnectionManager.getState() != BluetoothChatService.STATE_CONNECTED) {
      Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
      return;
    }

    // Check that there's actually something to send
    if (message.length() > 0) {
      // Get the message bytes and tell the BluetoothChatService to write
      byte[] send = message.getBytes();
      mConnectionManager.write(send);
    }
  }


  // The Handler that gets information back from the BluetoothChatService
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case MESSAGE_STATE_CHANGE:
        if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
        switch (msg.arg1) {
        case BluetoothChatService.STATE_CONNECTED:
          break;
        case BluetoothChatService.STATE_CONNECTING:
          break;
        case BluetoothChatService.STATE_LISTEN:
        case BluetoothChatService.STATE_NONE:
          break;
        }
        break;
      case MESSAGE_WRITE:
        byte[] writeBuf = (byte[]) msg.obj;
        // construct a string from the buffer
        String writeMessage = new String(writeBuf);
        break;
      case MESSAGE_READ:
        byte[] readBuf = (byte[]) msg.obj;
        // construct a string from the valid bytes in the buffer
        String readMessage = new String(readBuf, 0, msg.arg1);
        handleBTMessage(readMessage);
        break;
      case MESSAGE_DEVICE_NAME:
        // save the connected device's name
        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
        Toast.makeText(getApplicationContext(), "Connected to "
            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
        
        break;
      case MESSAGE_TOAST:
        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
            Toast.LENGTH_SHORT).show();
        break;
      }
    }
  };
  
  private void handleBTMessage(String message) {
    Log.i(TAG, "Handling BT received message: " + message);
    if (level == LIMBO) {
      try {
        objectIndex = Integer.parseInt(message.replaceAll("(\\r|\\n)", ""));
        currentObject = objects.get(objectIndex);
        level = OBJECT_LEVEL;
      }
      catch (Exception e) {
        // no clients in room
        Log.d("debugging", "room size is 0");
        Toast toast = Toast.makeText(getApplicationContext(),
            "failed connection", Toast.LENGTH_SHORT);
        toast.show();
      }
    }
    else {
      // not in LIMBO
    }
    resetContentView();
  }
  
  public boolean getConnectingToLaptop() {
    return toConnect;
  }

  public void initializeObjects() {
    room = new ArrayList<PhysicalTarget>();
    objects = new HashMap<Integer, PhysicalTarget>();
		
    for (int i = 0; i < 20; ++i) {
      objects.put(i, new PhysicalTarget(String.format("%02d", i), i));
    }
  }

  public void refreshObjects() {
    // we should never clear the objects
    // objects.clear();
    room.clear();
  }

  public void addObjectToRoom(int key) {
    if (objects.containsKey(key)) {
      // DON'T ADD THE SAME OBJECT TWICE
      if (!room.contains(objects.get(key))) {
	room.add(objects.get(key));
	Log.i("debugging", "added " + objects.get(key).getName()
	      + " to room");
      }
    } else
      throw new NumberFormatException("key for object: " + key
				      + " was invalid");
  }

  ArrayList<LinearLayout> views = new ArrayList<LinearLayout>();
  MainActivity context = this;

  public boolean isConnectingToLaptop() {
    return toConnect;
  }

  ProgressBar variableProgressBar;

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    switch (event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      Log.e(TAG, "onDown");
      // enter quasi-mode
      break;
    case MotionEvent.ACTION_UP:
      // make confirmation
      Log.e(TAG, "onTouchUp");
      break;
    default:  
    }
    return false;
  }
  
//  @Override
//  public boolean onSingleTapUp(MotionEvent e) {
//    if (D) Log.i(TAG, "single tap up");
////    
////    if (level == LIMBO) {
////      if (toConnect) {
////        sendMessage("FF");
////        // send out message
////      }
////    }
////    resetContentView();
//    return false;
//  }

  @Override
  public void onBackPressed() {
    Log.i("myGesture", "onBackPressed");
    switch (level) {
    case (OBJECT_LEVEL):
      level = LIMBO;
      sendMessage("D");
      resetContentView();
      break;
    case (LIMBO):
      super.onBackPressed();
      return;
    }
  }

}
