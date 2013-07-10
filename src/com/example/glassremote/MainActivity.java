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
    
    
    boolean[] hasBegun = new boolean[3];
    FingerThread[] fingerThread = new FingerThread[3];
    int fingersDown=0;
    
    //NEED THIS LINE
    MultiTouchDetector multiTouchDetector = new MultiTouchDetector();
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
    		multiTouchDetector.onTouchEvent(event);
    		return true;
    }	  @Override
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



	private class FingerThread extends Thread{
		private final long TIMEOUT= 500L;
		private long startTime;
		private int numFingers;
		
		public FingerThread(int numFingers){
			this.numFingers=numFingers;
			startTime = Calendar.getInstance().getTimeInMillis();
		}
		
		public void setStartTime(long newTime){
			startTime=newTime;
		}
		
		@Override
		 synchronized public void run(){
			//IF WE DONT DETECT A 3 FOR TIMEOUT MS, 3 IS OVER
			while (Calendar.getInstance().getTimeInMillis()-startTime < TIMEOUT ){
				//SPIN
			}
			Log.i("myGesture", numFingers + " FINGERS LIFT");
			int fingersIndex = numFingers-1;
			hasBegun[fingersIndex]=false;
			
			for (int i = 0; i<3; i++){
				//FIND GREATEST FINGER DOWN
				if (hasBegun[2-i]){
					fingersDown = 2-i+1;
					break;
				}
				fingersDown=0;
			}
			
		}
	}
	

}

