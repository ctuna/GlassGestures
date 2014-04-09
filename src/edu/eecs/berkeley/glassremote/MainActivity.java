package edu.eecs.berkeley.glassremote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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

public class MainActivity extends Activity implements GestureDetector.OnGestureListener, OnTouchListener {

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
    
    gestureDetector = new GestureDetector(this, this);

  }
  public HorizontalScrollView scroller;
  public LinearLayout holder;
  
  @Override
  protected void onStart() {
    super.onStart();
    if(D) Log.e(TAG, "++ ON START ++");

    // load all the targets
    initializeObjects();

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
    Log.i(TAG, "Handling BT received message: " + message);
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
    String[] replies = BTMessage.split(",");
    for (String r:replies) {
      String[] data = r.split(":");

      try {
        int objectId = Integer.parseInt(data[0].replaceAll("( |\\r|\\n)", ""));
        int objectValue = Integer.parseInt(data[1].replaceAll("( |\\r|\\n)", ""));
        addTargetToPotentialList(objectId, objectValue);
        Log.i(TAG, Integer.toString(objectId) + ":" + Integer.toString(objectValue));
      }
      catch (Exception e) {
        Log.d("debugging", "error in parsing");
      }
    }    
    handlePotentialTargets();
  }
  
  private void handlePotentialTargets() {
    Collections.sort(potentialTargets, new IntensityCompare());
    Log.i(TAG, potentialTargets.toString());
    
    if (potentialTargets.size() > 1) {
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
      Log.i(TAG, "get one target");
      mainMessage = (TextView) findViewById(R.id.mainMessage);
      mainMessage.setText(String.format("%02d", potentialTargets.get(0).getId()));
      isConnected = true;
      sendMessage("C" + String.format("%02d", potentialTargets.get(0).getId()));
      sendMessage("C" + String.format("%02d", potentialTargets.get(0).getId()));
    }
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

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    gestureDetector.onTouchEvent(event);
    switch (event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      // in single IR mode, we send out message and query the pending device
      // inflate the menu and then open the Menu Options
      // this part is easy
      Log.e(TAG, "onDown");
      // enter quasi-mode
      break;
    case MotionEvent.ACTION_UP:
      // make confirmation
      Log.e(TAG, "onTouchUp");
      break;
    default:  
    }
    return super.onGenericMotionEvent(event);
  }
  
  
  @Override
  public void onBackPressed() {
    Log.i("myGesture", "onBackPressed");
    if (isConnected || level == ROOM_LEVEL) {      
      setContentView(R.layout.activity_main);
      isConnected = false;
      level = LIMBO;
      mainMessage = (TextView) findViewById(R.id.mainMessage);
      mainMessage.setText("tap to connect");
      sendMessage("D");
      clearPotentialTargets();
      return;
    }
    else {
      super.onBackPressed();
    } 
  }

  @Override
  public boolean onDown(MotionEvent arg0) {
    // TODO Auto-generated method stub
    Log.i(TAG, "onDown event triggered!");
    return false;
  }

  @Override
  public boolean onFling(MotionEvent arg0, MotionEvent arg1, 
      float velocityX, float velocityY) {
    // TODO Auto-generated method stub
    Log.i(TAG, "onFling");
    
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
  public void onContentChanged () {
    Log.i(TAG, "onContentChanged");
  }

  @Override
  public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
      float arg3) {
    Log.i(TAG, "onScroll");
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void onShowPress(MotionEvent arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean onSingleTapUp(MotionEvent arg0) {
    if (D) Log.i(TAG, "single tap up");
    if (toConnect && !isConnected && level != ROOM_LEVEL) {
      // send out message
      sendMessage("FF00000");
    }
    if (level == ROOM_LEVEL && potentialTargets.size() > currentIndex) {
      if (D) Log.i(TAG, "Room level " + potentialTargets.toString() + "current: " + currentIndex);

      setContentView(R.layout.activity_main);
      isConnected = true;
      mainMessage = (TextView) findViewById(R.id.mainMessage);
      mainMessage.setText(String.format("%02d", potentialTargets.get(currentIndex).getId()));
      sendMessage("C" + String.format("%02d", potentialTargets.get(currentIndex).getId()));
      
      sendMessage("C" + String.format("%02d", potentialTargets.get(currentIndex).getId()));
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
}
