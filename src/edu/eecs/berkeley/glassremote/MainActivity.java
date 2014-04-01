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

import android.view.Menu;
import android.view.MotionEvent;
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

public class MainActivity extends Activity implements
					     GestureDetector.OnGestureListener, MultiTouchListener {

  // Debugging
  private static final String TAG = "GlassRemote";
  private static final boolean D = true;
  
  GestureDetector gestureDetector;
  GestureDetector.OnDoubleTapListener doubleGestureDetector;

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
  private ArrayList<ControlledObject> room;   // room list out all the available targets in this room
  private HashMap<Integer, ControlledObject> objects;
  private ControlledObject currentObject;
  private Variable currentVariable;
  private int varIndex = 0;
  private int objectIndex = 0;

  // MESSAGING
  private BluetoothChatService mConnectionManager = null;

  // EXPERIMENT
  // default set to IR mode

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
    
    // setup gesture detector
    gestureDetector = new GestureDetector(this, this);

  }

  @Override
  protected void onStart() {
    super.onStart();
    if(D) Log.e(TAG, "++ ON START ++");

    // load all the targets
    initializeObjects();

    if (mConnectionManager == null) 
      setupBluetooth();
    
    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    // Get the BluetoothDevice object
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice( btModuleAddress );
    // Attempt to connect to the device
    // false means insecure connection, which doesn't matter now
    mConnectionManager.connect(device, false);
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
      TextView nameOfObject = (TextView) findViewById(R.id.name_of_object);
      if (toConnect) {
	nameOfObject.setText(String.format("%02d", currentObject.getId()));
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

    if (this.mWakeLock != null &&           
	(this.mWakeLock.isHeld() == true)) {
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

    if (this.mWakeLock != null) {
      this.mWakeLock.release();
      this.mWakeLock = null; 
    }
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

        Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
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
  

  public boolean getConnectingToLaptop() {
    return toConnect;
  }

  public void initializeObjects() {
    room = new ArrayList<ControlledObject>();
    objects = new HashMap<Integer, ControlledObject>();
		
    for (int i = 0; i < 20; ++i) {
      objects.put(i, new ControlledObject(String.format("%02d", i), i, new Variable("selection", true, true, 0, 100, this))); 
    }
  }

  public void refreshObjects() {
    // we should never clear the objects
    // objects.clear();
    room.clear();
  }

  public void receive(String message) {
    Log.i("FUNCTION", "receive");
    Log.i("MESSAGE", "received message " + message);

    if (level == LIMBO) {
      // PARSE RESPONSE TO FF000000
      // message is a single target id
      try {
	objectIndex = Integer.parseInt(message.replaceAll("(\\r|\\n)", ""));
	currentObject = objects.get(objectIndex);
	Variable var_sel = getVariable(currentObject, "selection");
	level = OBJECT_LEVEL;
	objectIndex = 0;
      } catch (Exception e) {
	// no clients in room
	Log.d("debugging", "room size is 0");
	Toast toast = Toast.makeText(getApplicationContext(),
				     "failed connection", Toast.LENGTH_SHORT);
	toast.show();
      }
    }

    else { // leve != LIMBO
      // NOT INITIAL MESSAGE
      try {
	String id = message.substring(0, 2);
	String fn = message.substring(2, 3);
	String variable = message.substring(3, 6);
	String value = message.substring(6, 9);
	// PARSE NORMAL MESSAGE
	for (Variable currentVar : currentObject.getVariables()) {
	  if (currentVar.getAbbreviation().equals(variable)) {
	    if (fn.equals("A")) {
	      // IF IT'S AN ACK, SET VALUE
	      boolean on = !value.equals("OFF");
	      if (currentVar.hasBoolean()
		  && !currentVar.hasContinuous()) {
		currentVar.setBoolean(on);
	      }
	      // IF OFF, SET TO OFF, IF ON, SET TO CONTINUOUS
	      // VALUE
	      if (currentVar.hasBoolean()
		  && currentVar.hasContinuous()) {
		if (!on) {
		  currentVar.setBoolean(!on);
		} else {
		  currentVar.setContinuous(Integer
					   .parseInt(value));
		}
	      }

	    }
	  }
	}
      } catch (StringIndexOutOfBoundsException e) {
	Log.i("debugging", "badly formatted message: " + message);
      }

    }
    resetContentView();
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


  // FORMAT MESSAGE
  MultiTouchDetector multiTouchDetector = new MultiTouchDetector(this);

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    gestureDetector.onTouchEvent(event);
    multiTouchDetector.onTouchEvent(event);

    return false;
  }

  @Override
  public boolean onDown(MotionEvent e) {
    Log.i("myGesture", "onDown with pointer count: " + e.getPointerCount());
    return true;
  }

  @Override
  public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			 float arg3) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void onLongPress(MotionEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			  float distanceY) {

    return false;
  }

  @Override
  public void onShowPress(MotionEvent e) {
    // TODO Auto-generated method stub

  }

  public void refreshRoom() {
    Log.i("FUNCTION", "refreshRoom");

    if (toConnect) {
      room.clear();
      // send initial message
    }
    varIndex = 0;
    objectIndex = 0;
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    switch(e.getAction())
    {
      case MotionEvent.ACTION_DOWN:
        Log.i(TAG, "onDown with pointer count: " + e.getPointerCount());
        sendMessage("FF00000");
        break;
      case MotionEvent.ACTION_MOVE:
        break;
      case MotionEvent.ACTION_UP:
        break;
    }
    return super.onTouchEvent(e);
  }
  
  @Override
  public boolean onSingleTapUp(MotionEvent e) {
    if (D) Log.i(TAG, "single tap up");
    
    if (level == LIMBO) {
      if (toConnect) {
        sendMessage("FF00000");
        // send out message
        refreshRoom();
      } else {
        level = OBJECT_LEVEL;
        // TODO: change level to OBJECT LEVEL
      }
    }

    resetContentView();
    return false;
  }

  @Override
  public void onTapUp(int numFingers) {

  }

  @Override
  public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
		       float distanceY, int numFingers) {
	

  }

  @Override
  public void onFling(MotionEvent e1, MotionEvent e2, float velocityX,
		      float velocityY, int numFingers) {

  }

  @Override
  public void onBackPressed() {
    Log.i("myGesture", "onBackPressed");
    switch (level) {

    case (OBJECT_LEVEL):
      level = LIMBO;
      // turnOffLights();
      break;
    case (LIMBO):
      super.onBackPressed();
      return;
    }
    resetContentView();
  }
	
  @Override
  public void onScrollEnded(int numFingers) {

  }

  public Variable getVariable(ControlledObject obj, String var_name) {
    Variable var = null;
    for (Variable v : obj.getVariables()) {
      if (v.getName().equals(var_name)) {
	return v;
      }
    }
    return var;
  }
}
