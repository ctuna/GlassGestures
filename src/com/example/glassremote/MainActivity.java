package com.example.glassremote;

import java.util.Calendar;
import java.util.concurrent.locks.Lock;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;

public class MainActivity extends Activity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{
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

/**
 * STATES FOR TWO FINGER TAP
 */
public enum State {
    NONEDOWN, ONEDOWN, TWODOWN;
}
//boolean double_tap_pointer_up= false;

@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gestureDetector = new GestureDetector(this, this);
		State currentState= State.NONEDOWN;
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	//GESTURES
    
    WaitThread waitThread = null;
    boolean hasHadTwoDown= false;
    boolean hasHadThreeDown=false;
    public long startOfEvent = 0L;
    public boolean seenThree = false;
    ThreeFingerThread thread3 = null;
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
    		
    		int numPointers = event.getPointerCount();
        	switch (event.getActionIndex()){
        		case (MotionEvent.ACTION_UP):
        			//Log.i("myGesture", event.getEventTime() + ": action up with " + numPointers);
        			if (numPointers==2){
        			}
        		case (MotionEvent.ACTION_DOWN):
        			Log.i("myGesture", "down with "+ event.getPointerCount());
        			//if (waitThread != null){
        				//stateChange=true;
        				//Log.i("myGesture", "state changed");
        			
        				
        			//}
        			if (event.getPointerCount()==3){
        				//STOP OLD THREAD
        				if (thread3!=null){
        					thread3.setStartTime(Calendar.getInstance().getTimeInMillis());
        				}
        				
        				if (!hasHadThreeDown){
        					hasHadThreeDown=true;
        					thread3 = new ThreeFingerThread();
        					thread3.start();
        					Log.i("myGesture3", "3 FINGERS DOWN");
        				}
        				
        			}
        			if (event.getPointerCount()==2){
        				if (!hasHadTwoDown){
        					hasHadTwoDown=true;
        					Log.i("myGesture", "2 FINGERS DOWN");
        				}
        			}
        			if (event.getPointerCount()==1){
        				//FIRE A THREAD THAT WAITS
        				if (hasHadTwoDown){
        					//Log.i("myGesture", "fired a new thread");
        					//waitThread = new WaitThread();
        					//waitThread.start();
        				}
        			}
        		
        		case (MotionEvent.ACTION_MOVE):
        			//Log.i("myGesture", "move with "+ event.getPointerCount());
        		case (MotionEvent.ACTION_OUTSIDE):
        			//Log.i("myGesture", "outside with "+ event.getPointerCount());
        		case (MotionEvent.ACTION_POINTER_DOWN):
        			//Log.i("myGesture", "pointer down with "+ event.getPointerCount());
        			if (event.getPointerCount()==2 && double_tap_down){
        				
        				double_tap_pointer_down=true;
        				//Log.i("myGesture", "step 2");
        				
        			}
        		case (MotionEvent.ACTION_POINTER_UP):
        			//Log.i("myGesture", "pointer up with "+ event.getPointerCount());
        			if ((event.getPointerCount()==2) && double_tap_pointer_down){
        				//Log.i("myGesture", "double tap!");
        				//start over again
        				double_tap_down=false;
            			double_tap_pointer_down=false;
            			coordinatesSet=false;
        			}
        			
        		case (MotionEvent.ACTION_SCROLL):
        			//if (event.getX(0) != event.getHistoricalX(0)){

    				float currentX = event.getX(0);
        			float currentY = event.getY(0);
        			if (!coordinatesSet){
        				oldX=currentX;
        				oldY=currentY;
        				coordinatesSet=true;
        			}
        			if (event.getPointerCount()==2){
        				
        			
        			if ((Math.abs(currentX - oldX))>10f){
        				//Log.i("myGesture", "current: "+ currentX + "old: " + oldX);
        				//start again
        				//Log.i("myGesture", "double scrolling");
        				double_tap_down=false;
            			double_tap_pointer_down=false;
        			}
        			oldX=currentX;
        			oldY=currentY;
        	}
        	//else Log.i("myGesture", "Action is index: " + event.getActionIndex());
        
        	gestureDetector.onTouchEvent(event);
        
    }
        	return false;
    }
    float oldX;
    float oldY;
    boolean coordinatesSet=false;
	  @Override
	    public boolean onDown(MotionEvent e) {
	        //Log.i("myGesture", "onDown with pointer count: "+ e.getPointerCount());
	        return true;

	    }
	
	  
	  @Override
	  public boolean onTouchEvent(MotionEvent event) {
		  Log.i("myGesture", "hello");
		    // Get the pointer ID
		    int mActivePointerId = event.getPointerId(0);

		    // ... Many touch events later...

		    // Use the pointer ID to find the index of the active pointer 
		    // and fetch its position
		    int pointerIndex = event.findPointerIndex(mActivePointerId);
		    // Get the pointer's current position
		    float x = event.getX(pointerIndex);
		    float y = event.getY(pointerIndex);
		    
		    
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
		//Log.i("myGesture", "scrollin");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		 Log.i("myGesture", "on single tap up");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent arg0) {
		Log.i("Double", "double tap detected by listener");
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent arg0) {
		Log.i("Double", "double tap detected by listener");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent arg0) {
		Log.i("Double", "double tap detected by listener");
		// TODO Auto-generated method stub
		return false;
	}

	
public long currentDoubleClick= 0L;
public long previousDoubleClick= 0L;


public boolean stateChange;


	private class ThreeFingerThread extends Thread{
		private final long TIMEOUT= 300L;
		private int index;
		long startTime;
		
		public ThreeFingerThread(){
			hasHadThreeDown=true;
			seenThree=false;
			i+=1;
			index=i;
			startTime = Calendar.getInstance().getTimeInMillis();
		}
		
		public void setStartTime(long newTime){
			Log.i("myGesture3", "setting new time to: "+ newTime);
			startTime=newTime;
		}
		
		@Override
		 synchronized public void run(){
			
			//IF WE DONT DETECT A 3 FOR 500 MS, 3 IS OVER
			while (Calendar.getInstance().getTimeInMillis()-startTime < TIMEOUT ){
				//SPIN
			}
			
			Log.i("myGesture3", "3 FINGERS LIFT");
			hasHadThreeDown=false;
			
		}
	}
	
int i = 0;
	
    private class WaitThread extends Thread {
  

		private int timeOfLastOne;
		
		public void setTimeOfLastOne(int t){
			timeOfLastOne=t;
		}
		
		
		public WaitThread(){
			stateChange=false;
		}
	
		@Override
		synchronized public void run(){
			
			Log.i("myGesture", "thread is running");
			long startTime = Calendar.getInstance().getTimeInMillis();
			
			
			    while(!stateChange && Calendar.getInstance().getTimeInMillis() - startTime < 100){
			        
			    
			    }
				if (stateChange){
					waitThread=null;
					return;
				}
			    
				
			
			//state
			
			currentDoubleClick=Calendar.getInstance().getTimeInMillis();			
			if (previousDoubleClick!=0L && currentDoubleClick!=0L){
			
				if (currentDoubleClick - previousDoubleClick<150L){
					//TOO CLOSE TOGETHER
					return;
				}
			}
			Log.i("myGesture", "2 FINGERS UP");
			previousDoubleClick=currentDoubleClick;
			hasHadTwoDown=false;
			waitThread=null;
		}
		
	}
}

