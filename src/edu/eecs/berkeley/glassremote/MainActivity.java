package edu.eecs.berkeley.glassremote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import com.achaldave.myapplication2.app.Location;
import com.achaldave.myapplication2.app.LocationMap;
import com.achaldave.myapplication2.app.Orientation;
import com.achaldave.myapplication2.app.OrientationHistory;

import android.R.color;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

public class MainActivity extends Activity 
  implements GestureDetector.OnGestureListener, OnTouchListener {
// commented out SensorEventListener for reliability
  
  // Glass sensors  
//  private SensorManager mSensorManager;
//  private Sensor mSensor;
//  
  /* Not proud of this global, but hard to make it cleaner. */
//  private Orientation orientation;
//  private OrientationHistory previousOrientations;
//  /* Sorted list of rotation matrices at which something was tagged. */
//  private LocationMap locations;
//  /* Next id of device. */
//  private int nextId = 0;


//  private boolean quasiMode = false;
  GestureDetector gestureDetector;

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
  private boolean toConnect = true;
  private boolean isConnected = false;

  //Name of the connected device
  private String mConnectedDeviceName = null;   

  // NAVIGATION CONTROL
  // TWO NAVIGATES = TRUE means switching with 2 fingers, scrolling with 1
  public boolean twoNavigates = false;

  int flingRight = 0;
  int flingLeft = 0;

  // LAYOUT
  TextView nameOfObject;
  TextView mainMessage;
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
  private ArrayList<PhysicalTarget> potentialTargets;   // room list out all the available targets in this room
  private HashMap<Integer, PhysicalTarget> targets;
  private PhysicalTarget currentTarget;
  private int currentIndex;
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
    mainMessage = (TextView) findViewById(R.id.mainMessage);
    mainMessage.setText("Loading");
    
    gestureDetector = new GestureDetector(this, this);
    
    // orientation
//    locations = new LocationMap();
//    orientation = new Orientation();
//    // locations.loadMap(this);
//    previousOrientations = new OrientationHistory(10);

  }
  public HorizontalScrollView scroller;
  public LinearLayout holder;
  
  @Override
  protected void onStart() {
    super.onStart();
    if(D) Log.e(TAG, "++ ON START ++");

    ((TextView) findViewById(R.id.mainMessage)).setText("Loading");
    
    // load all the targets
    initializeObjects();

    
//    // Get an instance of the SensorManager
//    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
//
//    // Get updates every 10ms.
//    mSensorManager.registerListener(this, mSensor, 10000);

    if (toConnect) {
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
    
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }
  
  /**
   * Gets called every time the user presses the menu button.
   * Use if your menu is dynamic.
   */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
      menu.clear();
      for (PhysicalTarget t:potentialTargets) {
        menu.add(0, Menu.FIRST + potentialTargets.indexOf(t), Menu.NONE, t.getName());
      }
      return super.onPrepareOptionsMenu(menu);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      super.onOptionsItemSelected(item);
      Log.i(TAG, potentialTargets.get(item.getItemId()).getName());
      return false;
  }
  
  @Override
  public void onOptionsMenuClosed(Menu menu) {
    mainMessage.setAlpha(1);
    sendMessage("D");
    clearPotentialTargets();
    super.onOptionsMenuClosed(menu);  
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
        nameOfObject.setText("connected to " + String.format("%02d", currentTarget.getId()));
      }
    break;
    }
  }

  @Override
  protected void onStop() {
    sendMessage("D");
    // mSensorManager.unregisterListener(this);
    if(D) Log.e(TAG, "-- ON STOP --");
//    locations.saveMap(this);
    super.onStop();
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
  
  // this works for tablets and phones
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
  protected void onResume() {
    super.onResume();
    if(D) Log.e(TAG, "+ ON RESUME +");
    
    // when on resume, we might need to acquire the lock again
    if ((this.mWakeLock != null) &&           // we have a WakeLock
	(this.mWakeLock.isHeld() == false)) {  // but we don't hold it 
      this.mWakeLock.acquire();
    }
  }

  private int previousMatch = 0;
//  public void onSensorChanged(SensorEvent event) {
//    if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
//      float[] tmp = new float[16];
//      float[] remapped = new float[16];
//      float[] rawOrientation = new float[3];
//      SensorManager.getRotationMatrixFromVector(tmp, event.values);
//      SensorManager.remapCoordinateSystem(tmp, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped);
//      SensorManager.getOrientation(remapped, rawOrientation);
//
//      orientation.setOrientation(rawOrientation[0], rawOrientation[1]);
//      previousOrientations.add(new Orientation(orientation));
//      int match = locations.getMatch(previousOrientations);
//      if (match > 0) {
//        if (previousMatch != match && this.quasiMode == true) {
//          // this is where we found something
//          sendMessage( "H" + String.format("%02d", match));
//          previousMatch = match;
//          Log.i(TAG, "Found id: " + previousMatch);
//          // Location found = locations.getById(match);
//          // match is the id
//          sendMessage( "H" + String.format("%02d", match));
//        }
//      }
//    }
//  }
  
  public void updateMap(Location updated) {
      int id = updated.id;
      Log.d("Finder", "Updating location for " + Integer.toString(id));
     //  locations.update(updated);
  }
  /**
   * Update the location of @ids such that they are centered around @orientation
   *
   * TODO: Ideally, we get the center point of @ids, and update all the devices
   *       such that the center would be @orientation.
   *
   * @param ids              The devices to update
   * @param orientation      Where the user is looking
   */
//  public void updateMap(ArrayList<Integer> ids, Orientation orientation) {
//      locations.update(ids, orientation);
//  }
  
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
  
  // The Handler that gets information back from the BluetoothChatService
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case MESSAGE_STATE_CHANGE:
        if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
        switch (msg.arg1) {
        case BluetoothChatService.STATE_CONNECTED:
          mainMessage.setText("tap to connect");
          MainActivity.this.sendMessage("D");
          break;
        case BluetoothChatService.STATE_CONNECTING:
          break;
        case BluetoothChatService.STATE_LISTEN:
        case BluetoothChatService.STATE_NONE:
          break;
        }
        break;
      case MESSAGE_WRITE:
        // byte[] writeBuf = (byte[]) msg.obj;
        // construct a string from the buffer
        // String writeMessage = new String(writeBuf);
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
  
  private boolean firstHalfBT = false;
  private String BTMessage = null;
  private MainActivity context = this;
  
  private void handleBTMessage(String message) {
    Log.e("GlasSees", "time: " + Calendar.getInstance().getTimeInMillis() + " BT message level: " + message);
    if (firstHalfBT) {
      BTMessage += message;
      if (!message.contains("\n")) 
        return;
      else 
        firstHalfBT = false;
    }
    if (! message.contains("\n") ) {
      BTMessage = message;
      firstHalfBT = true;
      return;
    }
    // BTMessage examples: "5:123, 12:231,"
    String[] replies = null;
    try {
      replies = BTMessage.split(",");

      for (String r:replies) {
        String[] data = r.split(":");

        int objectId = Integer.parseInt(data[0].replaceAll("( |\\r|\\n)", ""));
        int objectValue = Integer.parseInt(data[1].replaceAll("( |\\r|\\n)", ""));
        addTargetToPotentialList(objectId, objectValue);
        Log.i(TAG, Integer.toString(objectId) + ":" + Integer.toString(objectValue));
      }
    }
    catch (Exception e) {
      Log.d("debugging", "error in parsing");
    }
    handlePotentialTargets();
  }
  
  private void handlePotentialTargets() {
    // Collections.sort(potentialTargets, new IntensityCompare());
    
    Log.i(TAG, potentialTargets.toString());
    
    if (potentialTargets.size() > 1) {
      Log.e("GlasSees", "time: " + Calendar.getInstance().getTimeInMillis() + " Room level: " + potentialTargets.toString() + " current: " + potentialTargets.get(0).getId());
      currentIndex = 0;
      sendMessage( "H" + String.format("%02d", potentialTargets.get(0).getId()));
      sendMessage( "H" + String.format("%02d", potentialTargets.get(0).getId()));
      // mainMessage.setAlpha((float) 0);
      // the menu options now doesn't work (for fling gesture), we don't open then
      // openOptionsMenu();
      
      // we enter into room activity
      level = ROOM_LEVEL;
      setContentView(R.layout.room_activity);
      
      holder = (LinearLayout) findViewById(R.id.list_holder);
      holder.removeAllViews();
      for (PhysicalTarget pt:potentialTargets){
        TextView t = new TextView(context);
        t.setText(pt.getName());
        t.setId(pt.getId());
        t.setTextSize(textSize);
        t.setPadding(0, 0, 60, 0);
        if ( potentialTargets.indexOf(pt) == 0) {
          t.setTextColor(selectedColor);
          t.setAlpha(1f);
        }
        else{
          t.setTextColor(fadedColor);
          t.setAlpha(outOfFocus);
        }
        holder.addView(t);
      }
    }
    
    else if (potentialTargets.size() == 1) {
      connectToTarget(potentialTargets.get(0).getId());
      // updateMap(new Location(potentialTargets.get(0).getId(), orientation));
      Log.i(TAG, "get one target");
    }
  }
 
  private void connectToTarget(int id) {
    sendMessage("C" + String.format("%02d", id));
    ((TextView) findViewById(R.id.mainMessage)).setText("connected to " + String.format("%02d", id));
    isConnected = true;
    Log.e("GlasSees", "time: " + Calendar.getInstance().getTimeInMillis() + " Connected to " + id);
    sendMessage("C" + String.format("%02d", id));
  }
  
  public boolean getConnectingToLaptop() {
    return toConnect;
  }

  public void initializeObjects() {
    potentialTargets = new ArrayList<PhysicalTarget>();
    targets = new HashMap<Integer, PhysicalTarget>();

    for (int i = 0; i < 20; ++i) {
      targets.put(i, new PhysicalTarget(String.format("%02d", i), i));
    }
  }

  public void clearPotentialTargets() {
    potentialTargets.clear();
  }

  public void addTargetToPotentialList(int key, int intensity) {
    if (targets.containsKey(key)) {
      // DON'T ADD THE SAME OBJECT TWICE
      PhysicalTarget current = targets.get(key);
      if ( !potentialTargets.contains(current) ) {
        current.setIntensity(intensity);
        potentialTargets.add(current);
        Log.i("debugging", "added " + current.getName() + " to room");
      }
      else {
        current.setIntensity(intensity);
      }
    } else {
      throw new NumberFormatException("key for object: " + key + " was invalid");
    }
  }

//  final Runnable enterQuasi = new Runnable()
//  {
//    public void run() 
//    {
//      quasiMode = true;
//      Log.i(TAG, "entering Quasimode");
//    }
//  };

  Handler mDelayHandler = new Handler();

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    gestureDetector.onTouchEvent(event);
    switch (event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      // make sure we are not in quasi-mode
      // this.quasiMode = false;
      // previousMatch = 0;
      // mDelayHandler.postDelayed(enterQuasi, 200);
      break;
    case MotionEvent.ACTION_MOVE:
      break;
    case MotionEvent.ACTION_UP:
//      mDelayHandler.removeCallbacks(enterQuasi);
//      // make confirmation
//      Log.e(TAG, "onTouchUp");
//      if (previousMatch != 0) {
//        connectToTarget(previousMatch);
//      }
      //this.quasiMode = false;

      break;
    default:  
    }
    return super.onGenericMotionEvent(event);
  }
  
  
  @Override
  public void onBackPressed() {
    // mDelayHandler.removeCallbacks(enterQuasi);
    Log.i("myGesture", "onBackPressed");
    if (isConnected || level == ROOM_LEVEL) {      
      setContentView(R.layout.activity_main);
      isConnected = false;
      sendMessage("D");
      level = LIMBO;
      mainMessage = (TextView) findViewById(R.id.mainMessage);
      mainMessage.setText("tap to connect");
      sendMessage("D");
      Log.e("GlasSees", "time: " + Calendar.getInstance().getTimeInMillis() + " Disconnected");

      clearPotentialTargets();
      return;
    }
    else {
      // super.onBackPressed();
    } 
  }

  @Override
  public boolean onDown(MotionEvent arg0) {
    // TODO Auto-generated method stub
    // Log.i(TAG, "onDown event triggered!");
    return false;
  }

  @Override
  public boolean onFling(MotionEvent arg0, MotionEvent arg1, 
      float velocityX, float velocityY) {
    // TODO Auto-generated method stub
    // Log.i(TAG, "onFling");
    
    if (level == ROOM_LEVEL) {
      if (velocityX>0){
        if (currentIndex < potentialTargets.size() - 1) {
          currentIndex++;
        }
      }
      else {
        if (currentIndex > 0) {
          currentIndex--;
        }
      }
      for (int i = 0 ; i < potentialTargets.size(); i ++){
        TextView currentText = (TextView) holder.getChildAt(i);
        if (currentText.getText().equals(potentialTargets.get(currentIndex).getName())){
          currentText.setTextColor(selectedColor);
          currentText.setAlpha(1f);

          Log.e("GlasSees", "time: " + Calendar.getInstance().getTimeInMillis() + " Change to: " + potentialTargets.get(currentIndex).getId());

          sendMessage( "H" + String.format("%02d", potentialTargets.get(currentIndex).getId()));
          sendMessage( "H" + String.format("%02d", potentialTargets.get(currentIndex).getId()));

        }
        else {
          currentText.setTextColor(fadedColor);
          currentText.setAlpha(outOfFocus);
        }
      }
    }
    return false;
  }



  @Override
  public void onLongPress(MotionEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
      float arg3) {
    // Log.i(TAG, "onScroll");
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void onShowPress(MotionEvent arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean onSingleTapUp(MotionEvent arg0) {
    // this.quasiMode = false;

    if (D) Log.i(TAG, "single tap up");
    if (toConnect && !isConnected && level != ROOM_LEVEL) {
      // send out message
      sendMessage("FF");
      Log.e("GlasSees", "time: " + Calendar.getInstance().getTimeInMillis() + " Tapped to connect");
      sendMessage("FF");
    }
    if (level == ROOM_LEVEL && potentialTargets.size() > currentIndex) {
      
      setContentView(R.layout.activity_main);
      connectToTarget(potentialTargets.get(currentIndex).getId());
      // updateMap(new Location(potentialTargets.get(currentIndex).getId(), orientation));
    }
    return false;
  }  
  
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


  @Override
  public boolean onTouch(View arg0, MotionEvent arg1) {
    // TODO Auto-generated method stub
    Log.i(TAG, "onTouch");
    return false;
  }

//  @Override
//  public void onAccuracyChanged(Sensor sensor, int accuracy) {
//    // TODO Auto-generated method stub
//    
//  }
}
