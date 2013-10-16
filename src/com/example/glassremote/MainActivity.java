

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.R.color;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.Bundle;
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


public boolean connectingToLaptop = true;

//NAVIGATION CONTROL
//TWO NAVIGATES = TRUE means switching with 2 fingers, scrolling with 1 
public boolean twoNavigates = false;
int fingersToNavigate;
int fingersToScroll;
int fingersToToggle;
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

private final int SELECTION = 3;
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
private final int MODE_IR = 1;		//get clients who received IR signal
private final int MODE_LIST = 2;	//get all clients and shown in list
private final int MODE_HOME = 3;	//smart home scenario
private int exp_mode = MODE_IR;
private boolean double_clients = false;



ControlledObject fan;
ControlledObject smartTV;
ControlledObject lamp;
ControlledObject music;
ControlledObject slide;

ControlledObject target1;
ControlledObject target2;
ControlledObject target3;
ControlledObject target4;
ControlledObject target5;
ControlledObject target6;
ControlledObject target7;
ControlledObject target8;
ControlledObject target9;
ControlledObject target10;

//*** 2nd pair
ControlledObject target11;
ControlledObject target12;
ControlledObject target13;
ControlledObject target14;
ControlledObject target15;
ControlledObject target16;
ControlledObject target17;
ControlledObject target18;
ControlledObject target19;
ControlledObject target20;

@Override
	protected void onCreate(Bundle savedInstanceState) {
		//begin in limbo
		level = SELECTION;
		resetContentView();
		super.onCreate(savedInstanceState);
		setupNavigation();
		connectionManager= new ConnectionManager(this);
		if (connectingToLaptop){
			//MAKE TEXT DISPLAY LOADING UNTIL CONNECTED SUCCESSFULLY 

				connectionManager.start();
		}


		initializeObjects();


		//see if an error happens, if so take out reset layout
		resetLayout();
		gestureDetector = new GestureDetector(this, this);
		selectedColor = getResources().getColor(color.holo_blue_dark);
		fadedColor = getResources().getColor(R.color.white_transparent);
		outOfFocus=.5f;

		/**
		if(exp_mode == MODE_IR) {
			//tell glass arduino and clients that we are in list mode
			Log.d("debugging", "sending cmd: exp IR mode");
			connectionManager.write("00SMOD001\n");
		}else{
			Log.d("debugging", "sending cmd: exp list mode");
			connectionManager.write("00SMOD002\n");
		}*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void resetContentView(){
		switch (level){
			case(SELECTION):
				setContentView(R.layout.mode_selection_activity);	
				break;
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
		//try{
			turnOffLights();
			connectionManager.destroy();
			super.onStop();
	//	}
		//catch 

	}
	//GESTURES
    
	public boolean getConnectingToLaptop(){
		return connectingToLaptop;
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


		target1 = new ControlledObject("01", 1,
				new Variable("selection", true, true, 0, 100, this));
		target2 = new ControlledObject("02", 2,
				new Variable("selection", true, true, 0, 100, this));
		target3 = new ControlledObject("03", 3,
				new Variable("selection", true, true, 0, 100, this));
		target4 = new ControlledObject("F", 4,
				new Variable("selection", true, true, 0, 100, this));
		target5 = new ControlledObject("J", 5,
				new Variable("selection", true, true, 0, 100, this));
		target6 = new ControlledObject("S", 6,
				new Variable("selection", true, true, 0, 100, this));
		target7 = new ControlledObject("Y", 7,
				new Variable("selection", true, true, 0, 100, this));
		target8 = new ControlledObject("R", 8,
				new Variable("selection", true, true, 0, 100, this));
		target9 = new ControlledObject("A", 9,
				new Variable("selection", true, true, 0, 100, this));
		target10 = new ControlledObject("M", 10,
				new Variable("selection", true, true, 0, 100, this));

		//*** 2nd pair
		if(double_clients){
			target11 = new ControlledObject("G", 11,
					new Variable("selection", true, true, 0, 100, this));
			target12 = new ControlledObject("O", 12,
					new Variable("selection", true, true, 0, 100, this));
			target13 = new ControlledObject("W", 13,
					new Variable("selection", true, true, 0, 100, this));
			target14 = new ControlledObject("F", 14,
					new Variable("selection", true, true, 0, 100, this));
			target15 = new ControlledObject("J", 15,
					new Variable("selection", true, true, 0, 100, this));
			target16 = new ControlledObject("S", 16,
					new Variable("selection", true, true, 0, 100, this));
			target17 = new ControlledObject("Y", 17,
					new Variable("selection", true, true, 0, 100, this));
			target18 = new ControlledObject("R", 18,
					new Variable("selection", true, true, 0, 100, this));
			target19 = new ControlledObject("A", 19,
					new Variable("selection", true, true, 0, 100, this));
			target20 = new ControlledObject("M", 20,
					new Variable("selection", true, true, 0, 100, this));
		}




//		objects.put(fridge.getId(), fridge);


	}

	public void refreshObjects() {

		objects.clear();
		room.clear();

		if(exp_mode == MODE_HOME) {
			objects.put(fan.getId(), fan);
//			objects.put(music.getId(), music);
			objects.put(slide.getId(), slide);
			objects.put(lamp.getId(), lamp);

		} else {
			objects.put(target9.getId(), target9);
			objects.put(target4.getId(), target4);
			objects.put(target1.getId(), target1);
			objects.put(target5.getId(), target5);
			objects.put(target10.getId(), target10);
			objects.put(target2.getId(), target2);
			objects.put(target8.getId(), target8);
			objects.put(target6.getId(), target6);
			objects.put(target3.getId(), target3);
			objects.put(target7.getId(), target7);

			//*** 2nd pair
			if(double_clients){
				objects.put(target19.getId(), target19);
				objects.put(target14.getId(), target14);
				objects.put(target11.getId(), target11);
				objects.put(target15.getId(), target15);
				objects.put(target20.getId(), target20);
				objects.put(target12.getId(), target12);
				objects.put(target18.getId(), target18);
				objects.put(target16.getId(), target16);
				objects.put(target13.getId(), target13);
				objects.put(target17.getId(), target17);
			}


			if(exp_mode == MODE_LIST) {
				//add all clients to room
//				Iterator<Entry<Integer, ControlledObject>> it = objects.entrySet().iterator();
//			    while (it.hasNext()) {
//			        HashMap.Entry pairs = (HashMap.Entry)it.next();
//			        System.out.println(pairs.getKey() + " = " + pairs.getValue());
//			        Log.d("debugging", "adding object " + pairs.getValue() + " to room" );
//			        addObjectToRoom((Integer) pairs.getKey());
//			        it.remove(); // avoids a ConcurrentModificationException
//			    }

				//using alphabetic order instead of id (numeric)
				addObjectToRoom(9);
				addObjectToRoom(4);
				addObjectToRoom(1);
				addObjectToRoom(5);
				addObjectToRoom(10);
				addObjectToRoom(2);
				addObjectToRoom(8);
				addObjectToRoom(6);
				addObjectToRoom(3);
				addObjectToRoom(7);

				//*** 2nd pair
				if(double_clients){
					addObjectToRoom(19);
					addObjectToRoom(14);
					addObjectToRoom(11);
					addObjectToRoom(15);
					addObjectToRoom(20);
					addObjectToRoom(12);
					addObjectToRoom(18);
					addObjectToRoom(16);
					addObjectToRoom(13);
					addObjectToRoom(17);					
				}

			}

		}

		Log.d("debugging", "finishing adding " + objects.size() + " objects");
		Log.d("debugging", "room size: " + room.size() );


		if (!connectingToLaptop){
		//ADD DUMMY OBJECTS
			room.add(smartTV);
			room.add(lamp);
//			currentObject=laptop;
//			currentVariable = laptop.getVariables().get(varIndex);
		}
	}

	public void receive(String message){
		Log.i("debugging", "received string:  " + message);
		Log.d("debugging", "objects size: " + objects.size());
		Log.d("debugging", "level is:" + level);

		if (level == LIMBO ){
			//PARSE RESPONSE TO FF000000
			String currentSubstring= message;
			String nextSubstring="";
			String[] halves = new String[2];

			Log.d("debugging", "string length:"+currentSubstring.length());
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
		resetLayout();
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
		Log.i("debugging", "onfling called on level SELECTION? " + (level == SELECTION) );
		int length;
		LinearLayout holder;
		HorizontalScrollView scroller = (HorizontalScrollView)findViewById (R.id.scroller);
		switch (level){

		case (SELECTION):
			Log.i("debugging", "inside switchmode/selection");

//			Log.d("debugging", scroller.toString());
//			if (forward){
//				exp_mode = MODE_IR;
//			}
//			else {
//				exp_mode = MODE_LIST;
//			}
			Log.d("debugging", "exp_mode: " + exp_mode);
			if(forward && exp_mode != MODE_HOME) {
				exp_mode++;
			} else if(!forward && exp_mode != MODE_IR) {
				exp_mode--;
			}
			resetLayout();
			break;
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
			if(exp_mode == MODE_HOME) {
				//no object level switch in IR and List mode

				scroller = (HorizontalScrollView)findViewById (R.id.scroller);
				length = currentObject.getVariables().size();
				//- 2 SO LED IS HIDDEN
				if (forward && varIndex != length - 2) {
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
						current.setAlpha(1f);
						currentVariableLayout = current;
					}
					else {
						currentText.setTextColor(unSelectedColor);
						current.setAlpha(outOfFocus);
					}
				}

			}

		//resetLayout();
	}
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
				resetContentView();
				switch (level){
					case (SELECTION):
						//LIST MODE
						TextView listText = (TextView) findViewById(R.id.list_text);
						TextView irText = (TextView) findViewById(R.id.ir_text);
						TextView homeText = (TextView) findViewById(R.id.home_text);
						Log.d("debugging", "change display to mode:" + exp_mode);
						if(exp_mode == MODE_IR) {

							listText.setTextColor(unSelectedColor);
							irText.setTextColor(selectedColor);
							homeText.setTextColor(unSelectedColor);
						}
						else if (exp_mode == MODE_LIST){
							listText.setTextColor(selectedColor);
							irText.setTextColor(unSelectedColor);
							homeText.setTextColor(unSelectedColor);
						}
						else {
							//Home MODE
							listText.setTextColor(unSelectedColor);
							irText.setTextColor(unSelectedColor);
							homeText.setTextColor(selectedColor);
						}

						break;
					case (ROOM_LEVEL):
						holder = (LinearLayout) findViewById(R.id.list_holder);
						holder.removeAllViews();



						//POPULATE DEVICES
						for (ControlledObject ob : room){
							t= new TextView(context);
							currentName = ob.getName();
							t.setText(currentName);
							t.setId(ob.getId());
							t.setTextSize(textSize);
							t.setPadding(0,  0 , 60, 0);
							if (currentName.equals(currentObject.getName())) {
								t.setTextColor(selectedColor);
								t.setAlpha(1f);
							}
							else{
								t.setAlpha(outOfFocus);
							}
							holder.addView(t);
							t=null;		
						}



						break;
					case (OBJECT_LEVEL):


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
								l.setAlpha(1f);
							}
							else {l.setAlpha(outOfFocus);}
							t.setText(currentName);
							t.setPadding(0, 0, 20, 0);
							t.setTextSize(textSize);

							LinearLayout relative = new LinearLayout(context);

							if (v.hasContinuous()){
								if (v.getName().equals("video") || v.getName().equals("music")){
									relative.setOrientation(LinearLayout.HORIZONTAL);
									//VIDEO CASE
									//DEFAULT TO PAUSED FIRST
									v.setBoolean(false);

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
								else if (v.getName().equals("position")){
										relative.setOrientation(LinearLayout.HORIZONTAL);
									//VIDEO CASE
									//DEFAULT TO PAUSED FIRST
									v.setBoolean(false);
									rewindButton = new ImageView(context);
									rewindButton.setImageDrawable(getResources().getDrawable(R.drawable.rewindsmall));
									fastForwardButton = new ImageView(context);
									fastForwardButton.setAlpha(.4f);
									rewindButton.setAlpha(.4f);
									fastForwardButton.setImageDrawable(getResources().getDrawable(R.drawable.fastforwardsmall));
									relative.addView(rewindButton);
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
						
						HorizontalScrollView scroller = (HorizontalScrollView)findViewById (R.id.scroller);

						for (int i = 0; i < varIndex; i++){
							Log.i("myGesture", "doing it");
							scroller.pageScroll(ScrollView.FOCUS_RIGHT);
						}
						break;

				case (LIMBO):			
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
				if (connectingToLaptop) connectionManager.write(connectionManager.formatMessage(currentObject, currentVariable, 'C', currentValue));
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
		if (level == OBJECT_LEVEL ){
			Log.i("cure", "currentvariable percent is"+ currentVariable.getPercentage());
				if (increase){
					if (connectingToLaptop) {
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
			Log.d("debugging", "objects size"+objects.size());
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
			currentObject=room.get(objectIndex);
			Variable var_sel = getVariable(currentObject, "selection");
			connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "off"));
		}
	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		//BEHAVIOR FOR TAP 

//		Log.i("debugging", "sent init message");
		Log.i("debugging", "single tap up");
		switch (level){
			case (SELECTION):


				if(exp_mode == MODE_IR) {
					//tell glass arduino and clients that we are in list mode
					Log.d("debugging", "sending cmd: exp IR mode");
					connectionManager.write("00SMOD001\n");
					level = LIMBO;
				}else if(exp_mode == MODE_LIST){
					//MODE LIST
					Log.d("debugging", "sending cmd: exp list mode");
					connectionManager.write("00SMOD002\n");
					level = LIMBO;

				}else if(exp_mode == MODE_HOME){
					Log.d("debugging", "sending cmd: exp HOME mode");
					connectionManager.write("00SMOD001\n");
					//TODO: need to add mode 003 and glass should change channel accordingly
					level = LIMBO;
				}
				refreshObjects();
				break;
			case (LIMBO):

				if(exp_mode == MODE_IR || exp_mode == MODE_HOME) {
					refreshRoom();
				} else if(exp_mode == MODE_LIST) {
					level = ROOM_LEVEL;

					objectIndex = 0;
					currentObject=room.get(objectIndex);
					Variable var_sel = getVariable(currentObject, "selection");
					connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "1st"));
					resetLayout();
				}

				//Log.i("debugging", "in limbo, room size is: " + room.size());

				break;
			case (ROOM_LEVEL):



				level=OBJECT_LEVEL;

				//an object is selected, query initial status of each variables 

				//send out corresponding led commands
				//selected client turn led on, others off
				Variable var_sel = getVariable(currentObject, "selection");
				if (connectingToLaptop){
					connectionManager.write(connectionManager.formatMessage(currentObject, var_sel, 'C', "on"));

					//removing this msg and have client auto reply status once it's selected
//					for (Variable v: currentObject.getVariables()){
//						if (!v.getName().equals("selection"))connectionManager.write(connectionManager.formatMessage(currentObject, v, 'R'));
//						
//						
//					}
				}
				break;
			case (OBJECT_LEVEL):
				//TODO: SELECT VARIABLE
				if (currentVariable.hasBoolean()){
					currentVariable.setBoolean(!currentVariable.getBoolean());

				}
				if(exp_mode == MODE_HOME){
					updateValue();
					//skip if it's target acquisition test (IR or List)
				}

				return false;
				//level=VARIABLE_LEVEL;

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
	if (numFingers ==fingersToScroll){
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
		if (level == OBJECT_LEVEL || level == ROOM_LEVEL || level == SELECTION){

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
		else if (numFingers == fingersToScroll && level == OBJECT_LEVEL && currentVariable.getName().equals("position")){
			//POSITION PRESS FORWARD
				//forward
				if (velocityX>0){
					updateValue(true);

				}
				//backward
				else {
					updateValue(false);

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
	    	level = LIMBO;

	    	break;
	    case (OBJECT_LEVEL):
//	    	if (exp_mode == MODE_IR) level = LIMBO;
//	    	else  level = ROOM_LEVEL;
	    	level = LIMBO;
	    	//send led off msg to previously connected client
	    	turnOffLights();
	    	break;


	    case (LIMBO):
	    	level = SELECTION;
	    	break;
	    	//onDestroy();
	    	//super.onBackPressed();
	    	//return;

	    case (SELECTION):
	    	onDestroy();
	    	super.onBackPressed();
	    	return;
	    }
	    resetLayout();

	}

	@Override
	public void onScrollEnded(int numFingers) {
		Log.i("debugging", "in on scroll ended");
		//ONLY IF DONE CONNCETING/DONT SEND ERROR
		if (exp_mode == MODE_HOME && level == OBJECT_LEVEL){

			if (numFingers == fingersToToggle && !currentVariable.getName().equals("video") && !currentVariable.getName().equals("position") && !currentVariable.getName().equals("music")){

				updateValue();
			}
			try{	
				if (currentObject.getName().equals("TV")){
				runOnUiThread(new Runnable() {

				     public void run() {
				    	 Log.i("debugging", "in where i want to be");
				    	 if (fastForwardButton !=null){

				    	 fastForwardButton.setAlpha(.4f);
				    	 rewindButton.setAlpha(.4f);}
				     }
				});
			}
		}
		catch (NullPointerException e){
			e.printStackTrace();
		}
		try{	
			if (currentObject.getName().equals("Slide")){
				runOnUiThread(new Runnable() {

					public void run() {
				    	 	if (fastForwardButton !=null && rewindButton !=null){
				    	 		fastForwardButton.setAlpha(.4f);
				    			 rewindButton.setAlpha(.4f);}
				     }
				});
			}
		}
		catch (NullPointerException e){
			e.printStackTrace();
		}
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

