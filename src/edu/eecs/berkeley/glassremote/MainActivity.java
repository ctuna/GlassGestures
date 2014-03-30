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

public class MainActivity extends Activity implements GestureDetector.OnGestureListener, MultiTouchListener{
	
  GestureDetector gestureDetector;
  GestureDetector.OnDoubleTapListener doubleGestureDetector;

  // disable sleep in the app
  protected PowerManager.WakeLock mWakeLock;

  //when connecting is false, glass doesn't connect to devices but pretends to
  public boolean connecting = false;

  //NAVIGATION CONTROL
  //TWO NAVIGATES = TRUE means switching with 2 fingers, scrolling with 1 
  public boolean twoNavigates = false;

  int flingRight = 0;
  int flingLeft = 0;

  //LAYOUT 
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
   * STATES:
   * Limbo: waiting for a connection request
   * Room: lists all objects 
   * Objects: list all variables of an object
   */


  private final int LIMBO = 0;
  private final int ROOM_LEVEL = 1;
  private final int OBJECT_LEVEL = 2;
  private int level; 



  //VARIABLE CONTROL
  private ArrayList<ControlledObject> room;
  private HashMap<Integer, ControlledObject> objects;
  private ControlledObject currentObject;
  private Variable currentVariable;
  private int varIndex = 0;
  private int objectIndex = 0;


  //MESSAGING
  ConnectionManager connectionManager;

  //EXPERIMENT
  //default set to IR mode



  ControlledObject fan;
  ControlledObject smartTV;
  ControlledObject lamp;
  ControlledObject music;
  ControlledObject slide;



  

  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    
    /* This code together with the one in onDestroy() 
     * will make the screen be always on until this Activity gets destroyed. */
	super.onCreate(savedInstanceState);
    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
    this.mWakeLock.acquire();  
    
    //start in limbo
    level = LIMBO;
    connectionManager= new ConnectionManager(this);
    resetContentView();

    
    if (connecting){
      connectionManager.start();
    }
    
    initializeObjects();

    gestureDetector = new GestureDetector(this, this);
    selectedColor = getResources().getColor(color.holo_blue_dark);
    fadedColor = getResources().getColor(R.color.white_transparent);
    outOfFocus=.5f;


  }

  
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  
  public void resetContentView(){
	Log.i("LAYOUT ", "changing view to: " + level);
    switch (level){

    case(LIMBO):
      setContentView(R.layout.activity_main);
    break;
    case(ROOM_LEVEL):
      setContentView(R.layout.room_activity);
    break;
    case(OBJECT_LEVEL):
      setContentView(R.layout.object_activity);	
    break;
    }
  }

  


  @Override
  protected void onStop() {
    super.onStop();
  }
  
  @Override
  protected void onDestroy(){
    //try{
    turnOffLights();
    connectionManager.destroy();
    this.mWakeLock.release();
    super.onDestroy();


    //	}
    //catch 

  }
  //GESTURES

  public boolean getConnectingToLaptop(){
    return connecting;
  }
  /**
   * lamp->light	on/off	continuous
   * laptop-> sound (on/off/continuous)
				brightness(on/off/continuous)
				scroll position (continuous)
				text size (continuous)
   */

  public void initializeObjects(){

    room = new ArrayList<ControlledObject>();
    objects = new HashMap<Integer, ControlledObject>();
    //DECLARE OBJECTS

    fan = new ControlledObject("Fan", 14,
        new Variable("power", true, false, 0, 100, this),
        new Variable("selection", true, true, 0, 100, this));
    /**smartTV = new ControlledObject("TV", 12,
				new Variable("video", true, true, 0, 1000, this),
				new Variable("volume", true, true,0, 100, this),
				new Variable("selection", true, true, 0, 100, this));*/
    //		music = new ControlledObject("Music", 11,
    //				new Variable("music", true, true, 0, 1000, this),
    //				new Variable("volume", true, true,0, 100, this),
    //				new Variable("selection", true, true, 0, 100, this));
    lamp = new ControlledObject("Lamp", 11,
        new Variable("power", true, false, 0, 100, this),
        new Variable("selection", true, true, 0, 100, this));
    slide = new ControlledObject("Slide", 12,
        new Variable("position", true, true, 0, 100, this),
        new Variable("selection", true, true, 0, 100, this));






    //		objects.put(fridge.getId(), fridge);


  }

  public void refreshObjects() {

    objects.clear();
    room.clear();
    //TODO: add list from IR 
 
    if (!connecting){
      //ADD DUMMY OBJECTS
      room.add(smartTV);
      room.add(lamp);
    }
  }

  public void receive(String message){
	Log.i("FUNCTION", "receive");
	Log.i("MESSAGE", "received message " + message);


    if (level == LIMBO ){
      //PARSE RESPONSE TO FF000000
      String currentSubstring= message;
      String nextSubstring="";
      String[] halves = new String[2];

      while (currentSubstring.length()>=3){	//one or more clients responded
        if (currentSubstring.contains(":")){
          halves = currentSubstring.split(":", 2);
          currentSubstring = halves[0];
          nextSubstring = halves[1];
        }
        else{
          nextSubstring="";
        }

        while (currentSubstring.length()>0){
          if (!Helper.isNum(currentSubstring.charAt(0))) {
            currentSubstring = currentSubstring.substring(1);
          }
          else break;
        }
        String currentSubstringTrimmed="";
        //TRIM OUT EXTRA SPACES
        for (int i = 0; i < currentSubstring.length(); i++){
          if (Helper.isNum(currentSubstring.charAt(i))){
            currentSubstringTrimmed+=currentSubstring.charAt(i);
          }
        }

        try {					
          addObjectToRoom(Integer.parseInt(currentSubstringTrimmed));
        }
        catch (NumberFormatException e){
          e.printStackTrace();
        }
        currentSubstring=nextSubstring;

      }


      if (room.size()>=1){
        currentObject=room.get(objectIndex);
        Variable var_sel = getVariable(currentObject, "selection");

        if (room.size() == 1){
          level = OBJECT_LEVEL;
          objectIndex = 0;
          //"AON" (auto on) means only 1 client responded and is auto selected
          //"ON" means selected manully among multiple targets by the user
          connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "aon"));
        }
        else if (room.size()>1){
          level = ROOM_LEVEL;
          //get the first ID and ask to blink fast
          connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "1st"));
        }
      } else {
        //no clients in room
        Log.d("debugging", "room size is 0");
        Toast toast = Toast.makeText(getApplicationContext(), "no appliances were found", Toast.LENGTH_SHORT);
        toast.show();
        //meaning no clients responded after the broadcast
        connectionManager.write("00CSEL NA\n");
      }

    }


    else{	//leve != LIMBO
      //NOT INITIAL MESSAGE
      try {
        String id = message.substring(0, 2);
        String fn = message.substring(2, 3);
        String variable = message.substring(3, 6);
        String value = message.substring(6, 9);
        //PARSE NORMAL MESSAGE
        for (Variable currentVar : currentObject.getVariables()){
          if (currentVar.getAbbreviation().equals(variable)){
            if (fn.equals("A")){
              //IF IT'S AN ACK, SET VALUE
              boolean on = !value.equals("OFF");
              if (currentVar.hasBoolean() && !currentVar.hasContinuous()){
                currentVar.setBoolean(on);
              }
              //IF OFF, SET TO OFF, IF ON, SET TO CONTINUOUS VALUE 
              if (currentVar.hasBoolean() && currentVar.hasContinuous()){
                if (!on){
                  currentVar.setBoolean(!on);
                }
                else {
                  currentVar.setContinuous(Integer.parseInt(value));
                }
              }

            }
          }
        }
      }
      catch (StringIndexOutOfBoundsException e) {
        Log.i("debugging", "badly formatted message: " + message);
      }

    }
    resetContentView();
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
      //				if (flingRight > 1){
      if (objectIndex == 6){
        flingRight = 0;
        scroller.pageScroll(ScrollView.FOCUS_RIGHT);

      }

    }
    else if (!forward && objectIndex != 0) {
      objectIndex--;	
      //				if (flingLeft > 1){
      if (objectIndex == 4){
        scroller.pageScroll(ScrollView.FOCUS_LEFT);
        flingLeft = 0;
      }
    }

    //update the led for hovered client candidate
    //set the previously hovering one to blink slow

    currentObject=room.get(objectIndex);
    //set the current hovering one to blink fast
    Variable var_sel = getVariable(currentObject, "selection");
    connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'S', "80"));

    varIndex=0;
    holder = (LinearLayout) findViewById(R.id.list_holder);
    for (int i = 0 ; i < room.size(); i ++){

      TextView currentText = (TextView) holder.getChildAt(i);
      if (currentText.getText().equals(currentObject.getName())){
        currentText.setTextColor(selectedColor);
        currentText.setAlpha(1f);
      }
      else {
        currentText.setTextColor(unSelectedColor);
        currentText.setAlpha(outOfFocus);
      }
    }


    break;
    case (OBJECT_LEVEL):


    //resetLayout();
    }
  }
  
  ArrayList<LinearLayout> views = new ArrayList<LinearLayout>();
  MainActivity context = this;






  public boolean isConnectingToLaptop(){
    return connecting;
  }
  ProgressBar variableProgressBar;

  public void updateValue(){
    currentVariable = currentObject.getVariables().get(varIndex);
    CheckBox variableCheckBox=null;
    int THRESHOLD = 3;

    if (level == OBJECT_LEVEL){
      LinearLayout rel = (LinearLayout) currentVariableLayout.getChildAt(1);

      if (!currentVariable.getName().equals("video") && !currentVariable.getName().equals("music")&& !currentVariable.getName().equals("slide")){
        if (currentVariable.hasContinuous()) variableProgressBar = (ProgressBar) rel.getChildAt(0);	
        else variableCheckBox = (CheckBox) rel.getChildAt(0);
      }
    }
    if (level == OBJECT_LEVEL)
    {
      String currentValue = currentVariable.getCurrentValue();
      if (currentVariable.getName().equals("video")|| currentVariable.getName().equals("music")|| currentVariable.getName().equals("slide")){
        runOnUiThread(new Runnable() {
          public void run() {
            if (!currentVariable.getName().equals("slide")){
              if (!currentVariable.getBoolean()){
                toggleButton.setImageDrawable(getResources().getDrawable((R.drawable.playsmall)));
              }
              else toggleButton.setImageDrawable(getResources().getDrawable((R.drawable.pausesmall)));
            }
          }
        });
        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));
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
        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));

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
        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));

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

        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'S', currentValue));

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
    if (level == OBJECT_LEVEL)
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
        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));

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
        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));

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
        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'S', currentValue));

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
    if (level == OBJECT_LEVEL ){
      Log.i("cure", "currentvariable percent is"+ currentVariable.getPercentage());
      if (increase){
        if (connecting) {
          connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', "INC"));
        }
        //ADD 10 MILLIS TO POSITION

        runOnUiThread(new Runnable() {
          public void run() {
            long startTime = Calendar.getInstance().getTimeInMillis();
            rewindButton.setAlpha(.4f);
            fastForwardButton.setAlpha(1f);

          }
        });
      }
      else 	{

        if (connecting) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', "DEC"));
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
  }	  
  
  @Override
  public boolean onDown(MotionEvent e) {
    Log.i("myGesture", "onDown with pointer count: "+ e.getPointerCount());
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
    Log.i("FUNCTION", "refreshRoom");

    if (connecting) {
      room.clear();
      connectionManager.initialMessage();
    }
    varIndex=0;
    objectIndex=0;
  }

  public void turnOffLights(){
    if (connecting){
      currentObject=room.get(objectIndex);
      Variable var_sel = getVariable(currentObject, "selection");
      connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "off"));
    }
  }
  @Override
  public boolean onSingleTapUp(MotionEvent e) {
  
    if (level == LIMBO){
    	if (connecting){
    		//TODO: CHECK ON THIS
    		refreshRoom();
    	}
    	else{
    		level = OBJECT_LEVEL; 
    		//TODO: change level to OBJECT LEVEL
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
    int newVal;
    int oldVal;
    if (numFingers == 1){
      if (distanceY>-10){
        //GET RID OF FALSE ALARM FROM DOWN STROKE
        if (level ==OBJECT_LEVEL && !currentVariable.getName().equals("position")){

          if (distanceX > 0){
            Log.i("var", "turning it down");
            //TODO: TURN IT DOWN
            if (currentVariable.hasContinuous()){
              if (currentVariable.getName().equals("video") || currentVariable.getName().equals("music")){
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
              if (currentVariable.getName().equals("video")|| currentVariable.getName().equals("music")){
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
   
		


  }

  @Override
  public void onFling(MotionEvent e1, MotionEvent e2, float velocityX,
      float velocityY, int numFingers) {
    if (level == OBJECT_LEVEL || level == ROOM_LEVEL ){

      if (numFingers==1){

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
      //cancel from multiple selection
      //will also trigger turn off lights (executed on client side)
      connectionManager.write("00CSELCAN\n");
    	onDestroy();
    	super.onBackPressed();
    	return;
    	
    case (OBJECT_LEVEL):
      //	    	if (exp_mode == MODE_IR) level = LIMBO;
      //	    	else  level = ROOM_LEVEL;
      level = LIMBO;
    //send led off msg to previously connected client
    turnOffLights();
    break;


    case (LIMBO):
      level = ROOM_LEVEL;
      break;
    //onDestroy();
    //super.onBackPressed();
    //return;

    }
    resetContentView();

  }

  @Override
  public void onScrollEnded(int numFingers) {

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

