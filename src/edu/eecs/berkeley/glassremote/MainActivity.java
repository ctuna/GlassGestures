package edu.eecs.berkeley.glassremote;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import edu.eecs.berkeley.glassremote.R;

import android.R.color;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
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

	GestureDetector gestureDetector;
	GestureDetector.OnDoubleTapListener doubleGestureDetector;

	// disable sleep in the app
	protected PowerManager.WakeLock mWakeLock;

	// when toConnect is true, the Glass will connect to BT
	// otherwise, it will pretend to
	public boolean toConnect = true;

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
	private ArrayList<ControlledObject> room;
	private HashMap<Integer, ControlledObject> objects;
	private ControlledObject currentObject;
	private Variable currentVariable;
	private int varIndex = 0;
	private int objectIndex = 0;

	// MESSAGING
	ConnectionManager connectionManager;

	// EXPERIMENT
	// default set to IR mode

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		/*
		 * This code together with the one in onDestroy() will make the screen
		 * be always on until this Activity gets destroyed.
		 */
		super.onCreate(savedInstanceState);
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "GlassRemote WakeLock Tag");
		this.mWakeLock.acquire();

		// start in limbo
		level = LIMBO;
		connectionManager = new ConnectionManager(this, toConnect);
		resetContentView();
		connectionManager.start();
		initializeObjects();

		gestureDetector = new GestureDetector(this, this);
		selectedColor = getResources().getColor(color.holo_blue_dark);
		fadedColor = getResources().getColor(R.color.white_transparent);
		outOfFocus = .5f;

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
				nameOfObject.setText(currentObject.getId());
			}
			break;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// try{
		turnOffLights();
		connectionManager.destroy();
		this.mWakeLock.release();
		super.onDestroy();

	}

	public boolean getConnectingToLaptop() {
		return toConnect;
	}

	public void initializeObjects() {

		room = new ArrayList<ControlledObject>();
		objects = new HashMap<Integer, ControlledObject>();

	}

	public void refreshObjects() {

		objects.clear();
		room.clear();

	}

	public void receive(String message) {
		Log.i("FUNCTION", "receive");
		Log.i("MESSAGE", "received message " + message);

		if (level == LIMBO) {
			// PARSE RESPONSE TO FF000000
			String currentSubstring = message;
			String nextSubstring = "";
			String[] halves = new String[2];

			while (currentSubstring.length() >= 3) { // one or more clients
														// responded
				if (currentSubstring.contains(":")) {
					halves = currentSubstring.split(":", 2);
					currentSubstring = halves[0];
					nextSubstring = halves[1];
				} else {
					nextSubstring = "";
				}

				while (currentSubstring.length() > 0) {
					if (!Helper.isNum(currentSubstring.charAt(0))) {
						currentSubstring = currentSubstring.substring(1);
					} else
						break;
				}
				String currentSubstringTrimmed = "";
				// TRIM OUT EXTRA SPACES
				for (int i = 0; i < currentSubstring.length(); i++) {
					if (Helper.isNum(currentSubstring.charAt(i))) {
						currentSubstringTrimmed += currentSubstring.charAt(i);
					}
				}

				try {
					addObjectToRoom(Integer.parseInt(currentSubstringTrimmed));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				currentSubstring = nextSubstring;

			}

			if (room.size() >= 1) {
				currentObject = room.get(objectIndex);
				Variable var_sel = getVariable(currentObject, "selection");

				if (room.size() == 1) {
					level = OBJECT_LEVEL;
					objectIndex = 0;
					// "AON" (auto on) means only 1 client responded and is auto
					// selected
					// "ON" means selected manully among multiple targets by the
					// user
					connectionManager.write(connectionManager.formatMessage(
							currentObject, var_sel, 'C', "aon"));
				} else if (room.size() > 1) {
					level = ROOM_LEVEL;
					// get the first ID and ask to blink fast
					connectionManager.write(connectionManager.formatMessage(
							currentObject, var_sel, 'C', "1st"));
				}
			} else {
				// no clients in room
				Log.d("debugging", "room size is 0");
				Toast toast = Toast.makeText(getApplicationContext(),
						"no appliances were found", Toast.LENGTH_SHORT);
				toast.show();
				// meaning no clients responded after the broadcast
				connectionManager.write("00CSEL NA\n");
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
			connectionManager.initialMessage();
		}
		varIndex = 0;
		objectIndex = 0;
	}

	public void turnOffLights() {
		if (toConnect) {
			currentObject = room.get(objectIndex);
			Variable var_sel = getVariable(currentObject, "selection");
			connectionManager.write(connectionManager.formatMessage(
					currentObject, var_sel, 'C', "off"));
		}
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {

		if (level == LIMBO) {
			if (toConnect) {
				// TODO: CHECK ON THIS
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
			turnOffLights();
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
