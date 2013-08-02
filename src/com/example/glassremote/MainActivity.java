package com.example.glassremote;

import java.util.ArrayList;
import java.util.HashMap;

import android.R.color;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends Activity implements GestureDetector.OnGestureListener, MultiTouchListener{
GestureDetector gestureDetector;
GestureDetector.OnDoubleTapListener doubleGestureDetector;

/**
 * for detecting double tap, these three must happen in order
 * down with 1 pointer
 * pointer down with 2 pointers
 * pointer up with 1 pointer
 */
boolean double_tap_down = false;
boolean double_tap_pointer_down=false;

public boolean connectingToLaptop = false;

//TWO NAVIGATES = TRUE means switching with 2 fingers, scrolling with 1 
public boolean twoNavigates = true;
int fingersToNavigate;
int fingersToScroll;
int fingersToToggle;

int videoLengthInMillis = 1000;
//LAYOUT STUFF
TextView nameOfObject;
ImageView currentAlert;
TextView valueOfVariable;
ImageView rewindButton;
ImageView toggleButton;
ImageView fastForwardButton; 
//STATES

private final int ROOM_LEVEL = 0;
private final int OBJECT_LEVEL = 1;
private final int VARIABLE_LEVEL = 2;
private final int LIMBO = 3;
private int level = LIMBO;


int paddingAmount = 30;
int selectedColor;
int unSelectedColor;
int fadedColor;
int textSize = 35;
LinearLayout currentVariableLayout;
int flingRight = 0;
int flingLeft = 0;

//VARIABLE CONTROL
private ArrayList<ControlledObject> room;
private HashMap<Integer, ControlledObject> objects;
private ControlledObject currentObject;
private Variable currentVariable;
private int varIndex = 0;
private int objectIndex = 0;


//MESSAGING
ConnectionManager connectionManager;
private boolean waitingOnIds=false;
private final String REQUEST_OBJECTS = "FF0000000";
private BroadcastReceiver mReceiver=null;
/**
 * STATES FOR TWO FINGER TAP
 */
public enum State {
    NONEDOWN, ONEDOWN, TWODOWN;
}
//boolean double_tap_pointer_up= false;

@Override
	protected void onCreate(Bundle savedInstanceState) {
	
		setContentView(R.layout.activity_main);
			super.onCreate(savedInstanceState);
		setupNavigation();
		
		if (connectingToLaptop){
			//MAKE TEXT DISPLAY LOADING UNTIL CONNECTED SUCCESSFULLY 
			((TextView)findViewById(R.id.message)).setText("loading...");
			if(connectionManager == null) {
				connectionManager= new ConnectionManager(this);
				connectionManager.start();
			}
			while (!connectionManager.getIsConnected()){
				Log.i("debugging", "trying to connect");
				
				
			}
			((TextView)findViewById(R.id.message)).setText("tap to connect");
		}
		else{
			level=ROOM_LEVEL;
		}
		//ASK FOR OBJECTS
	
	
		initializeObjects();
		resetLayout();
		gestureDetector = new GestureDetector(this, this);
		multiTouchDetector = new MultiTouchDetector(this);
		selectedColor = getResources().getColor(color.holo_blue_dark);
		fadedColor = getResources().getColor(R.color.white_transparent);
		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	public void setupNavigation(){
		if (twoNavigates){
			fingersToNavigate=2;
			fingersToScroll=1;
			fingersToToggle = 1;
		}
		else {
			fingersToScroll=2;
			fingersToNavigate=1;
			fingersToToggle=1;
		}
	}
	
	@Override
	protected void onDestroy(){
			turnOffLights();
		
			connectionManager.destroy();
		
		 super.onStop();
	}
	//GESTURES
    
	public void setBroadcastReceiver(BroadcastReceiver b){
		this.mReceiver=b;
	}
    /**
     * lamp->light	on/off	continuous
     * laptop-> sound (on/off/continuous)
				brightness(on/off/continuous)
				scroll position (continuous)
				text size (continuous)
     */
	
	public void initializeObjects(){
		//DECLARE OBJECTS
		//TODO: have objects send ID upon connection

		ControlledObject lamp = new ControlledObject("lamp", 2,
				new Variable("brightness", true, false, 0, 100, this),
				new Variable("selection", true, true, 0, 100, this));
		ControlledObject laptop = new ControlledObject("laptop", 1,
				new Variable("video", true, true, 0, 1000, this),
				new Variable("volume", true, true,0, 100, this),
				new Variable("selection", true, true, 0, 100, this));
		objects = new HashMap<Integer, ControlledObject>();
		objects.put(laptop.getId(), laptop);
		objects.put(lamp.getId(), lamp);
		room = new ArrayList<ControlledObject>();
		
		if (!connectingToLaptop){
		//ADD DUMMY OBJECTS
			room.add(laptop);
			room.add(lamp);
			currentObject=laptop;
			currentVariable = laptop.getVariables().get(varIndex);
		}
	}
	
	public boolean isNum(char c){
		return (c == '0' || c == '1' || c =='2' ||
				c == '3' || c == '4' || c =='5' ||
				c == '6' || c == '7' || c =='8' ||
				c == '9');
	}
	
	public void receive(String message){
		Log.i("debugging", "received string:  " + message + " with length " + message.length());
		if (level == LIMBO || level == ROOM_LEVEL){

			boolean hasNext=false;
			String currentSubstring= message;
			String nextSubstring="";
			String[] halves = new String[2];
			
			while (currentSubstring.length()>=2){	//multiple clients responded
				if (currentSubstring.contains(":")){
					
					halves = currentSubstring.split(":", 2);
					currentSubstring = halves[0];
					nextSubstring = halves[1];
					
				}
				else{
					nextSubstring="";
				}
			
				while (currentSubstring.length()>0){
					if (!isNum(currentSubstring.charAt(0))) {
				
						currentSubstring = currentSubstring.substring(1);
					}
					else break;
				}
				String currentSubstringTrimmed="";
				//TRIM OUT EXTRA SPACES
				for (int i = 0; i < currentSubstring.length(); i++){
					if (isNum(currentSubstring.charAt(i))){
						currentSubstringTrimmed+=currentSubstring.charAt(i);
					}
				}
				
				try {
					
					addObjectToRoom(Integer.parseInt(currentSubstringTrimmed));
					
					//level = ROOM_LEVEL;
				}
				catch (NumberFormatException e){
					e.printStackTrace();
					
				}
				
				currentSubstring=nextSubstring;
				
						
			}
			
			
			if (room.size() == 1){
				level = OBJECT_LEVEL;
				objectIndex = 0;
				currentObject=room.get(objectIndex);
				Variable var_sel = getVariable(currentObject, "selection");
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "on"));
			}
			else if (room.size()>1){
				level = ROOM_LEVEL;
				currentObject = room.get(objectIndex);
				Variable var_sel = getVariable(currentObject, "selection");
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'S', "80"));
				
			}
			
			
			//PARSE IDS
		}
			
		
		else{	//Object level -> returns of READ msg
			try {
			String id = message.substring(0, 2);
			String fn = message.substring(2, 3);
			String variable = message.substring(3, 6);
			String value = message.substring(6, 9);
			
			for (Variable currentVar : currentObject.getVariables()){
				if (currentVar.getAbbreviation().equals(variable)){
					//JUST BOOLEAN
					boolean on = !value.equals("OFF");
					if (currentVar.hasBoolean() && !currentVar.hasContinuous()){
						currentVar.setBoolean(on);
					}
					if (currentVar.hasBoolean() && currentVar.hasContinuous()){
						if (!on){
							currentVar.setBoolean(!on);
						}
						else {
							currentVar.setContinuous(Integer.parseInt(value));
							}
					}
					//BOOL/CONTINUOUS
				}
			}
			}
			catch (StringIndexOutOfBoundsException e) {
				Log.i("debugging", "badly formatted message: " + message);
			}
			//PARSE NORMAL MESSAGE
		}
		resetLayout();
	}
	
	public void onValueChanged(){
		
	}
	
	public void addObjectToRoom(int key){
		if (objects.containsKey(key)){
			//DON'T ADD THE SAME OBJECT TWICE
			if (!room.contains(objects.get(key))){
				room.add(objects.get(key));
				Log.i("debugging", "added "+ objects.get(key).getName() + " to room");
			}
		}
		else throw new NumberFormatException("key for object: " + key + " was invalid"); 
			
	}
	
	public void switchMode(boolean forward){
		int length;
		LinearLayout holder;
		HorizontalScrollView scroller = (HorizontalScrollView)findViewById (R.id.scroller);
		switch (level){
		case (ROOM_LEVEL):
			//switch to a different client candidate
			
			scroller = (HorizontalScrollView)findViewById (R.id.scroller);
			length = room.size();
			if (forward && objectIndex != length - 1) 
				{
				objectIndex++;
				if (flingRight > 1){
					flingRight = 0;
					scroller.pageScroll(ScrollView.FOCUS_RIGHT);
				}
				
				}
			else if (!forward && objectIndex != 0) {
				objectIndex--;	
				if (flingLeft > 1){
					scroller.pageScroll(ScrollView.FOCUS_LEFT);
					flingLeft = 0;
				}
			}
			
			//update the led for hovered client candidate
			//set the previously hovering one to blink slow
	    
			currentObject=room.get(objectIndex);
			//set the current hovering one to blink fast
			Variable var_sel = getVariable(currentObject, "selection");
			if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'S', "80"));
	    
			//for objects in room, if current object blink fast else blink slow
			varIndex=0;
			holder = (LinearLayout) findViewById(R.id.list_holder);
			for (int i = 0 ; i < room.size(); i ++){
				//TODO: send corresponding to clients for blinking speed
				
				TextView currentText = (TextView) holder.getChildAt(i);
				if (currentText.getText().equals(currentObject.getName())){
					currentText.setTextColor(selectedColor);
				}
				else currentText.setTextColor(unSelectedColor);
			}
			
			
			break;
		case (OBJECT_LEVEL):
			scroller = (HorizontalScrollView)findViewById (R.id.scroller);
			length = currentObject.getVariables().size();
			if (forward && varIndex != length - 1) {
				varIndex++;
				if (flingRight > 1){
					flingRight = 0;
					scroller.pageScroll(ScrollView.FOCUS_RIGHT);
				}
				
				
			}
			else if (!forward && varIndex != 0) {
				varIndex--;
				if (flingLeft > 1){
					scroller.pageScroll(ScrollView.FOCUS_LEFT);
					flingLeft = 0;
				}
				
			}
			currentVariable = currentObject.getVariables().get(varIndex);
			
			holder = (LinearLayout) findViewById(R.id.list_holder);
			for (int i = 0 ; i < currentObject.getVariables().size()-1; i ++){
				LinearLayout current = (LinearLayout) holder.getChildAt(i);
				
				TextView currentText = (TextView) current.getChildAt(0);
				if (currentText.getText().equals(currentVariable.getName())){
					currentText.setTextColor(selectedColor);
					currentVariableLayout = current;
				}
				else currentText.setTextColor(unSelectedColor);
			}
			
			break;
		case (VARIABLE_LEVEL):
			break;	
		}
		//resetLayout();
	}
	

	
	ArrayList<LinearLayout> views = new ArrayList<LinearLayout>();
	MainActivity context = this;
	
	public void resetLayout(){

		
		runOnUiThread(new Runnable() {
		     public void run() {
		    	 TextView nameOfObject;
		    	selectedColor = getResources().getColor(color.holo_blue_dark);
		    	unSelectedColor = getResources().getColor(color.secondary_text_dark);
		    	//fadedColor = getResources().getColor(color.darker_gray);
				Log.i("myGesture", "level is : " + level);
				String varName="";
				if (room.size()>0){
					//IF THE ROOM HAS OBJECTS
					currentObject = room.get(objectIndex);
					
					currentVariable = currentObject.getVariables().get(varIndex);
					varName = currentVariable.getName();
					String varValue = currentVariable.getCurrentValue();
				}
				LinearLayout holder;
				TextView t;
				String currentName;
				switch (level){
					case (ROOM_LEVEL):

						
						
						
						Log.i("myGesture", "resetLayout to ROOM LEVEL");
						
						setContentView(R.layout.room_activity);
						holder = (LinearLayout) findViewById(R.id.list_holder);
						holder.removeAllViews();
						
						
						
						//POPULATE DEVICES
						for (ControlledObject ob : room){
							t= new TextView(context);
							currentName = ob.getName();
							t.setText(currentName);
							t.setTextSize(textSize);
							t.setPadding(0,  0 , 20, 0);
							if (currentName.equals(currentObject.getName())) {
								t.setTextColor(selectedColor);
							}
							holder.addView(t);
							t=null;		
						}
					
						
						
						break;
					case (OBJECT_LEVEL):
						Log.i("myGesture", "resetLayout to OBJECT LEVEL");
						//TODO: SEND READ FOR CURRENT OBJECT
						setContentView(R.layout.object_activity);
						
						holder = (LinearLayout) findViewById(R.id.list_holder);
						holder.removeAllViews();
						nameOfObject = (TextView) findViewById(R.id.name_of_object);
						nameOfObject.setText(currentObject.getName());
						nameOfObject.setTextColor(fadedColor);
						views.clear();
						//POPULATE DEVICES
						for (Variable v : currentObject.getVariables()){
							if (!v.getName().equals("selection")){
							LinearLayout l = new LinearLayout(context);
							l.setOrientation(LinearLayout.VERTICAL);
							LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
							l.setLayoutParams(params);
							//l.setPadding(paddingAmount, 0 , paddingAmount, 0);
							l.setPadding(0, 0 , paddingAmount, 0);
							
							
							t= new TextView(context);
							currentName = v.getName();
							if (currentName.equals(currentVariable.getName())) {
								t.setTextColor(selectedColor);
							}
							t.setText(currentName);
							t.setPadding(0, 0, 20, 0);
							t.setTextSize(textSize);
							
							LinearLayout relative = new LinearLayout(context);
						
							if (v.hasContinuous()){
								if (v.getName().equals("video")){
									relative.setOrientation(LinearLayout.HORIZONTAL);
									//VIDEO CASE

									rewindButton = new ImageView(context);
									rewindButton.setImageDrawable(getResources().getDrawable(R.drawable.rewindsmall));
									toggleButton = new ImageView(context);
									
									toggleButton.setImageDrawable(getResources().getDrawable(R.drawable.playsmall));
									fastForwardButton = new ImageView(context);
									fastForwardButton.setAlpha(.4f);
									rewindButton.setAlpha(.4f);
									toggleButton.setAlpha(1f);
									fastForwardButton.setImageDrawable(getResources().getDrawable(R.drawable.fastforwardsmall));
									relative.addView(rewindButton);
									relative.addView(toggleButton);
									relative.addView(fastForwardButton);
									}
								else{
									
								
								SeekBar progressBar = new SeekBar(context);
								//ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyle);
								//RelativeLayout.LayoutParams progressParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
								
								progressBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
								ImageView image = new ImageView(context);
								image.setAlpha(0f);
								//TODO: aesthe
								if (currentName.equals(currentVariable.getName())) {
									currentAlert = image;
								}
								
							
								//image.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
								relative.addView(progressBar);
								//relative.addView(image);
								//image.setLayoutParams(progressParams);
								String currentValue = v.getCurrentValue();
								if (currentValue.equals("off")){
									progressBar.setProgress(0);
								}
								else if (currentValue.equals("on")){
								//ON/OFF CASE
									progressBar.setProgress(100);
								}
								else progressBar.setProgress(Integer.parseInt(currentValue));
								}
							}
							else {
								//DOES NOT HAVE CONTINUOUS, ONLY HAS BOOLEAN
								CheckBox checkBox = new CheckBox(context);
								checkBox.setChecked(v.getBoolean());
								relative.addView(checkBox);
								
								
							}
							
							
							l.addView(t);
							l.addView(relative);
							
							
							holder.addView(l);
							if (currentName.equals(currentVariable.getName())) {
								currentVariableLayout=l;
							}
							
							views.add(l);
							t=null;		
							}
						}
						HorizontalScrollView scroller = (HorizontalScrollView)findViewById (R.id.scroller);
					
						for (int i = 0; i < varIndex; i++){
							Log.i("myGesture", "doing it");
							scroller.pageScroll(ScrollView.FOCUS_RIGHT);
						}
						break;
				case (VARIABLE_LEVEL):
						
				
				case (LIMBO):
					Log.i("myGesture", "resetLayout to LIMBO");
					setContentView(R.layout.activity_main);
				
					//TODO: TURN LEDS OFF
			
					break;
				}
		     
		     
		     }
		});
		
	}	
    
	public boolean isConnectingToLaptop(){
		return connectingToLaptop;
	}
	ProgressBar variableProgressBar;
	
	public void updateValue(){
		currentVariable = currentObject.getVariables().get(varIndex);
		CheckBox variableCheckBox=null;
		int THRESHOLD = 3;

		if (level == OBJECT_LEVEL){
			LinearLayout rel = (LinearLayout) currentVariableLayout.getChildAt(1);
			
			if (!currentVariable.getName().equals("video")){
					if (currentVariable.hasContinuous()) variableProgressBar = (ProgressBar) rel.getChildAt(0);	
					else variableCheckBox = (CheckBox) rel.getChildAt(0);
					}
			}
		if (level == OBJECT_LEVEL || level == VARIABLE_LEVEL)
			{
			String currentValue = currentVariable.getCurrentValue();
			if (currentVariable.getName().equals("video")){
				runOnUiThread(new Runnable() {
				     public void run() {
			
				    	 if (!currentVariable.getBoolean()){
				    		 toggleButton.setImageDrawable(getResources().getDrawable((R.drawable.playsmall)));
				    		
				    	 }
				    	 else toggleButton.setImageDrawable(getResources().getDrawable((R.drawable.pausesmall)));
				     }
				});
			}
			else if (currentValue.equals("off")){
				
				if (currentVariable.hasContinuous() ) {
					runOnUiThread(new Runnable() {
					     public void run() {
					    	 variableProgressBar.setAlpha(.4f);
					     }
					});
				}
				else variableCheckBox.setChecked(false);
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));
				
			}
			else if (currentValue.equals("on")){
			//ON/OFF CASE
				if (currentVariable.hasContinuous()){
					runOnUiThread(new Runnable() {
					     public void run() {
					    	 variableProgressBar.setAlpha(1f);
					     }
					});
				}
				else variableCheckBox.setChecked(true);
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));
				
			}
			else {
				if (currentVariable.hasContinuous()) {
					variableProgressBar.setProgress(Integer.parseInt(currentValue));
					runOnUiThread(new Runnable() {
					     public void run() {
					    	 variableProgressBar.setAlpha(1f);
					     }
					});
				}
				
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'S', currentValue));
				
			}
			Log.i("bt_message", "send out value: " + currentValue);
			}
		
		//FORMAT MESSAGE
		
		}
	
			
		
		//FORMAT MESSAGE
		
		
	
	public void updateValue(int oldVal){
		
		CheckBox variableCheckBox=null;
		int THRESHOLD = 3;
		Variable currentVariable = currentObject.getVariables().get(varIndex);
		Log.i("debugging", "in updateValue current variable is: " + currentVariable.getName());
		if (level == OBJECT_LEVEL){
			LinearLayout rel = (LinearLayout) currentVariableLayout.getChildAt(1);
			
			if (currentVariable.hasContinuous())variableProgressBar = (ProgressBar) rel.getChildAt(0);	
			else variableCheckBox = (CheckBox) rel.getChildAt(0);
					}
		if (level == OBJECT_LEVEL || level == VARIABLE_LEVEL)
			{
			String currentValue = currentVariable.getCurrentValue();
			if (currentValue.equals("off")){
				
				if (currentVariable.hasContinuous()) {
					runOnUiThread(new Runnable() {
					     public void run() {
					    	
					    	 variableProgressBar.setAlpha(.4f);
					     }
					});
				}
				else variableCheckBox.setChecked(false);
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));
				
			}
			else if (currentValue.equals("on")){
			//ON/OFF CASE
				if (currentVariable.hasContinuous()){
					runOnUiThread(new Runnable() {
					     public void run() {
					    	
					    	 variableProgressBar.setAlpha(1f);
					     }
					});
				}
				else variableCheckBox.setChecked(true);
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));
				
			}
			else {
				if (currentVariable.hasContinuous()) {
					variableProgressBar.setProgress(Integer.parseInt(currentValue));
					runOnUiThread(new Runnable() {
					     public void run() {
					    	
					    	 variableProgressBar.setAlpha(1f);
					     }
					});
				}
				if (Math.abs(Integer.parseInt(currentValue) - oldVal)< THRESHOLD){
				
					//don't send if hasn't changed enough 
					return;
				}
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'S', currentValue));
				
			}
			Log.i("bt_message", "send out value: " + currentValue);
			}
		
		//FORMAT MESSAGE
		
		}
	
	public void updateValue(boolean increase){
		//VIDEO CASE 
		ProgressBar valueOfVariable = null;
		int THRESHOLD = 3;
		
		if (level == OBJECT_LEVEL){
			//LinearLayout rel = (LinearLayout) currentVariableLayout.getChildAt(1);
			//valueOfVariable = (ProgressBar) rel.getChildAt(0);
		}
		if (level == OBJECT_LEVEL || level == VARIABLE_LEVEL){
			Log.i("cure", "currentvariable percent is"+ currentVariable.getPercentage());
				if (increase){
					if (connectingToLaptop) {
						connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', "INC"));
					}
					//ADD 10 MILLIS TO POSITION
					
					runOnUiThread(new Runnable() {
					     public void run() {
					    	 rewindButton.setAlpha(.4f);
					    	 fastForwardButton.setAlpha(1f);
					    	 Log.i("debugging", "setting alpha");
					     }
					});
					}
				else 	{
					
					if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', "DEC"));
					//SUBTRACT 10 MILLIS FROM POSITION
					runOnUiThread(new Runnable() {
					     public void run() {
					    	 rewindButton.setAlpha(1f);
					    	 fastForwardButton.setAlpha(.4f);
					     }
					});
				}
		}
			
			
	}
		
		//FORMAT MESSAGE
		
		
	
	
	
    MultiTouchDetector multiTouchDetector = new MultiTouchDetector(this);
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
    		gestureDetector.onTouchEvent(event);
    		multiTouchDetector.onTouchEvent(event);

    		return false;
    }	  @Override
	    public boolean onDown(MotionEvent e) {
	        //Log.i("myGesture", "onDown with pointer count: "+ e.getPointerCount());
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
	
	public void refreshRoom(){
		Log.i("debugging", "refreshing room");
		
		if (connectingToLaptop) {
			room.clear();
			connectionManager.initialMessage();
		}
		else {
			level = ROOM_LEVEL;
		}
		varIndex=0;
		objectIndex=0;
	}
	
	public void turnOffLights(){
		if (connectingToLaptop){
			for (ControlledObject object : room){
				//TURN OFF LIGHTS WHEN SHUT DOWN
				for (Variable v : object.getVariables()){
					if (v.getName().equals("selection")){
						if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(object, v, 'C', "off"));
				}
			}
		
		
	}
		}
	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		//BEHAVIOR FOR TAP 
	
		Log.i("debugging", "sent init message");
		Log.i("debugging", "single tap up");
		switch (level){
			case (LIMBO):
				
				refreshRoom();
				Log.i("debugging", "in limbo, room size is: " + room.size());
			
				break;
			case (ROOM_LEVEL):
				
				
			
				level=OBJECT_LEVEL;
				
				//an object is selected, query initial status of each variables 
			
				//send out corresponding led commands
				//selected client turn led on, others off
				Variable var_sel = getVariable(currentObject, "selection");
				if (connectingToLaptop){
				connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "on"));
			
				for (Variable v: currentObject.getVariables()){
					if (!v.getName().equals("selection"))connectionManager.write(connectionManager.formatMessage(currentObject, v, 'R'));
					
					
				}
				}
				break;
			case (OBJECT_LEVEL):
				//TODO: SELECT VARIABLE
				if (currentVariable.hasBoolean()){
					currentVariable.setBoolean(!currentVariable.getBoolean());
					
				}
				updateValue();
				return false;
				//level=VARIABLE_LEVEL;
				
			case (VARIABLE_LEVEL):
				
				break;
		}
		resetLayout();
	return false;
	}




	@Override
	public void onTapUp(int numFingers) {
		/**
		if (numFingers == 1){
			Log.i("myGesture", "tap registered at: "+ Calendar.getInstance().getTimeInMillis());
			switch (level){
			
			case (ROOM_LEVEL):
				//TODO: 
				
				break;
			case (OBJECT_LEVEL):
				//TODO: MUTE VARIABLE
				if (currentVariable.hasBoolean()){
					currentVariable.setBoolean(!currentVariable.getBoolean());
					updateValue();
				}
				break;
			case (VARIABLE_LEVEL):
				
				break;
		}
	
	
		
		}*/
		}
	
	
	@Override
	public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY, int numFingers) {
		int newVal;
		int oldVal;
	if (numFingers ==fingersToScroll ){
		if (distanceY>-10){
			//GET RID OF FALSE ALARM FROM DOWN STROKE
		if (level ==OBJECT_LEVEL){
			
			if (distanceX > 0){
				Log.i("var", "turning it down");
				//TODO: TURN IT DOWN
				if (currentVariable.hasContinuous()){
					if (currentVariable.getName().equals("video")){
						updateValue(false);
					}
					else {
						newVal = (int) (currentVariable.getPercentage()-(distanceX/6));
						oldVal = currentVariable.getPercentage();
						if (currentVariable.hasContinuous()){
							currentVariable.setContinuous(newVal);
							updateValue(oldVal);
						}
					}
				
					
					}
					}
			else{
				Log.i("var", "turning it up");
				//TODO: TURN IT UP
				if (currentVariable.hasContinuous()){ 
					if (currentVariable.getName().equals("video")){
						updateValue(true);
					}
					else {
						Log.i("var", "turning it up");
						oldVal = currentVariable.getPercentage();
						newVal = (int)(currentVariable.getPercentage()-(distanceX/6));
						if (currentVariable.hasContinuous()){
							currentVariable.setContinuous(newVal);
							updateValue(oldVal);
						}
						
					}
					}
					}
		}
		}
	}
	/**
	if (numFingers == 2){
		if (distanceY>-10){
			//GET RID OF FALSE ALARM FROM DOWN STROKE
		
		if (level ==OBJECT_LEVEL){
			if (distanceX > 0){
				//TODO: TURN IT DOWN
				if (currentVariable.hasContinuous()){
					currentVariable.setContinuous((int) (currentVariable.getPercentage()-(distanceX/6)));
					
					updateValue();
					
					}
					}
			else{
				//TODO: TURN IT UP
				if (currentVariable.hasContinuous()){                                                  
					currentVariable.setContinuous((int)(currentVariable.getPercentage()-(distanceX/6)));
					
					updateValue();
					}
					}
			
		}
		}
	}
		

		
		
		/**
		
		if (numFingers ==2){
			if (distanceX > 0){
				//TODO: TURN IT DOWN
				if (currentVariable.hasContinuous()){
					//TOGGLE
					currentVariable.setContinuous(currentVariable.getContinuous()-1);
					resetLayout();
					}
					//Log.i("myGesture", "scrolling back with "+ numFingers);
					}
			else{
				//TODO: TURN IT UP
				if (currentVariable.hasContinuous()){
					//TOGGLE
					currentVariable.setContinuous(currentVariable.getContinuous()+1);
					resetLayout();
					}
					//Log.i("myGesture", "scrolling forward with "+ numFingers);
					}
		
		}*/
		
		
		
	}

	@Override
	public void onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY, int numFingers) {
		
		if (level == OBJECT_LEVEL || level == ROOM_LEVEL){
			if (numFingers==fingersToNavigate){
			
				
				if (velocityX>0){
					flingLeft = 0;
					flingRight++;
						switchMode(true);
					
					
				}
				
				else {
					flingRight = 0;
					flingLeft++;
					switchMode(false);
				}
			
			//TODO: CHANGE MODE
			}
		}
	}

		
		
	
	@Override
	public void onBackPressed() {
	    Log.i("myGesture", "onBackPressed");
	    switch (level){
	    case (ROOM_LEVEL):
	    	turnOffLights();
	    	level = LIMBO;
	    	break;
	    case (OBJECT_LEVEL):
	    	level = LIMBO;
	    	//send led off msg to previously connected client
	    	turnOffLights();
	    	break;
	    
	    case (LIMBO):
	    	onDestroy();
	    	super.onBackPressed();
	    	return;
	    }
	    resetLayout();
	    
	}

	@Override
	public void onScrollEnded(int numFingers) {
		if (numFingers == fingersToToggle && !currentVariable.getName().equals("video")){
			
			updateValue();
		}
		if (numFingers == fingersToToggle && currentVariable.getName().equals("video")){
			runOnUiThread(new Runnable() {
			     public void run() {
			    	 fastForwardButton.setAlpha(.4f);
			    	 rewindButton.setAlpha(.4f);
			     }
			});
		}
		// TODO Auto-generated method stub
		
	}
	
	
	public Variable getVariable(ControlledObject obj, String var_name){
		 Variable var = null;
			for (Variable v: obj.getVariables()){
				if (v.getName().equals(var_name)){
					return v;
				}
			}
			return var;
	}

}

