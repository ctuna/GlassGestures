package com.example.glassremote;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.R.color;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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


//LAYOUT STUFF
TextView nameOfObject;

TextView valueOfVariable;
private int level = 3;
private final int LIMBO = 3;
private final int ROOM_LEVEL = 0;
private final int OBJECT_LEVEL = 1;
private final int VARIABLE_LEVEL = 2;
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
		connectionManager= new ConnectionManager(this);
		if (connectingToLaptop){
			connectionManager.start();
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
		}
		else {
			fingersToScroll=2;
			fingersToNavigate=1;
		}
	}
	
	@Override
	protected void onDestroy(){
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
				new Variable("brightness", true, false, 0, 100, this));
		ControlledObject laptop = new ControlledObject("laptop", 1,
				new Variable("brightness", true, true, 0, 100, this),
				new Variable("volume", true, true,0, 100, this));
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
	
	public void receive(String message){
		Log.i("debugging", "received string:  " + message + " with length " + message.length());
		if (level == LIMBO){

			
			String currentSubstring= message;
				while (currentSubstring.length()>=2){
					//CUT OFF ":" 
					if (currentSubstring.startsWith(":")){
						currentSubstring.substring(1);
					}
					try {
						addObjectToRoom(Integer.parseInt(currentSubstring.substring(0, 2)));
						level = ROOM_LEVEL;
					}
					catch (NumberFormatException e){
						e.printStackTrace();
						
					}
					currentSubstring=currentSubstring.substring(2);
					
					Log.i("debugging", "current"+ "current substring is: " + currentSubstring + " with length "+ currentSubstring.length());
					
					
				}
				
				resetLayout();
			//PARSE IDS
		}
			
		
		else{
			try {
			String id = message.substring(0, 2);
			String fn = message.substring(2, 3);
			String variable = message.substring(3, 6);
			String value = message.substring(6, 9);
			}
			catch (StringIndexOutOfBoundsException e) {
				Log.i("debugging", "badly formatted message: " + message);
			}
			//PARSE NORMAL MESSAGE
		}
	}
	
	public void onValueChanged(){
		
	}
	
	public void addObjectToRoom(int key){
		if (objects.containsKey(key)){
			room.add(objects.get(key));
			Log.i("debugging", "added "+ objects.get(key).getName() + " to room");
		}
		else Log.i("debugging", "key " + key + " was invalid");	
	}
	
	public void switchMode(boolean forward){
		int length;
		LinearLayout holder;
		HorizontalScrollView scroller = (HorizontalScrollView)findViewById (R.id.scroller);
		switch (level){
		case (ROOM_LEVEL):
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
			
		
			currentObject=room.get(objectIndex);
			varIndex=0;
			holder = (LinearLayout) findViewById(R.id.list_holder);
			for (int i = 0 ; i < room.size(); i ++){
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
			for (int i = 0 ; i < currentObject.getVariables().size(); i ++){
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
		Log.i("myGesture", "resetLayout");
		
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
						setContentView(R.layout.object_activity);
						
						holder = (LinearLayout) findViewById(R.id.list_holder);
						holder.removeAllViews();
						nameOfObject = (TextView) findViewById(R.id.name_of_object);
						nameOfObject.setText(currentObject.getName());
						nameOfObject.setTextColor(fadedColor);
						views.clear();
						//POPULATE DEVICES
						for (Variable v : currentObject.getVariables()){
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
							
							
							ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
							progressBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
							
							String currentValue = v.getCurrentValue();
							if (currentValue.equals("off")){
								progressBar.setProgress(0);
							}
							else if (currentValue.equals("on")){
								//ON/OFF CASE
								progressBar.setProgress(100);
							}
							else progressBar.setProgress(Integer.parseInt(currentValue));
						
							
							
							l.addView(t);
							l.addView(progressBar);
							
							
							holder.addView(l);
							if (currentName.equals(currentVariable.getName())) {
								currentVariableLayout=l;
							}
							
							views.add(l);
							t=null;		
						}
						HorizontalScrollView scroller = (HorizontalScrollView)findViewById (R.id.scroller);
					
						for (int i = 0; i < varIndex; i++){
							Log.i("myGesture", "doing it");
							scroller.pageScroll(ScrollView.FOCUS_RIGHT);
						}
						break;
					case (VARIABLE_LEVEL):
						//GET VARIABLE, SCALE TO / 100
						setContentView(R.layout.variable_activity);
						TextView nameOfVariable=(TextView) findViewById(R.id.name_of_variable);
						nameOfObject = (TextView) findViewById(R.id.name_of_object);
						nameOfObject.setText(currentObject.getName());
						nameOfVariable.setTextColor(selectedColor);
						nameOfVariable.setText(varName);
						ProgressBar valueOfVariable = (ProgressBar) findViewById(R.id.variable_progress);
						String currentValue = currentVariable.getCurrentValue();
						
						if (currentValue.equals("off")){
							valueOfVariable.setProgress(0);
							
						}
						else if (currentValue.equals("on")){
							//ON/OFF CASE
							valueOfVariable.setProgress(100);
						}
						else valueOfVariable.setProgress(Integer.parseInt(currentValue));
					
						connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, currentValue));
						
						break;
				
				
				case (LIMBO):
					
				}
		     
		     
		     }
		});
		
	}	
    
	public boolean isConnectingToLaptop(){
		return connectingToLaptop;
	}
	
	public void updateValue(){
		ProgressBar valueOfVariable = null;
		if (level == VARIABLE_LEVEL){
			valueOfVariable = (ProgressBar) findViewById(R.id.variable_progress);
		}
		if (level == OBJECT_LEVEL){
			valueOfVariable = (ProgressBar)currentVariableLayout.getChildAt(1);
		}
		if (level == OBJECT_LEVEL || level == VARIABLE_LEVEL)
			{
			String currentValue = currentVariable.getCurrentValue();
			if (currentValue.equals("off")){
				valueOfVariable.setProgress(0);
			}
			else if (currentValue.equals("on")){
			//ON/OFF CASE
				valueOfVariable.setProgress(100);
			}
			else valueOfVariable.setProgress(Integer.parseInt(currentValue));
			
			connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, currentValue));
			
			
			}
		
		//FORMAT MESSAGE
		
		}
	
	public void updateValue(int oldVal){
		ProgressBar valueOfVariable = null;
		int THRESHOLD = 5;
		if (level == VARIABLE_LEVEL){
			valueOfVariable = (ProgressBar) findViewById(R.id.variable_progress);
		}
		if (level == OBJECT_LEVEL){
			valueOfVariable = (ProgressBar)currentVariableLayout.getChildAt(1);
		}
		if (level == OBJECT_LEVEL || level == VARIABLE_LEVEL)
			{
			String currentValue = currentVariable.getCurrentValue();
			if (currentValue.equals("off")){
				valueOfVariable.setProgress(0);
			}
			else if (currentValue.equals("on")){
			//ON/OFF CASE
				valueOfVariable.setProgress(100);
			}
			else {
				valueOfVariable.setProgress(Integer.parseInt(currentValue));
				if (Math.abs(Integer.parseInt(currentValue) - oldVal)< THRESHOLD){
				
					//don't send if hasn't changed enough 
					return;
				}
			}
			connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, currentValue));
			
			}
		
		//FORMAT MESSAGE
		
		}
	
	
	
	
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

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		//BEHAVIOR FOR TAP 
		if (level==LIMBO){
			connectionManager.initialMessage();
		
		}
		Log.i("debugging", "sent init message");
		switch (level){
			case (ROOM_LEVEL):
				
				
				level=OBJECT_LEVEL;
				break;
			case (OBJECT_LEVEL):
				//TODO: SELECT VARIABLE
				if (currentVariable.hasBoolean())currentVariable.setBoolean(!currentVariable.getBoolean());
				//level=VARIABLE_LEVEL;
				break;
			case (VARIABLE_LEVEL):
				
				break;
		}
		resetLayout();
	return false;
	}




	@Override
	public void onTapUp(int numFingers) {
		
		if (numFingers == 2){
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
	
	
		
		}
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
					newVal = (int) (currentVariable.getPercentage()-(distanceX/6));
					oldVal = currentVariable.getPercentage();
					currentVariable.setContinuous(newVal);
					
					updateValue(oldVal);
					
					}
					}
			else{
				//TODO: TURN IT UP
				if (currentVariable.hasContinuous()){    
					Log.i("var", "turning it up");
					oldVal = currentVariable.getPercentage();
					newVal = (int)(currentVariable.getPercentage()-(distanceX/6));
					currentVariable.setContinuous(newVal);
					updateValue(oldVal);
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
	    	onDestroy();
	    	super.onBackPressed();
	    	return;
	    case (OBJECT_LEVEL):
	    	level = ROOM_LEVEL;
	    	break;
	    case (VARIABLE_LEVEL):
	    	level=OBJECT_LEVEL;
	    	break;
	    
	    case (LIMBO):
	    	super.onBackPressed();
	    	return;
	    }
	    resetLayout();
	    
	}
	
	
	

}

