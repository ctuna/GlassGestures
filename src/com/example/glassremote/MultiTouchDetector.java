package com.example.glassremote;

import java.util.Calendar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class MultiTouchDetector implements GestureDetector.OnGestureListener {
	
    boolean[] hasBegun = new boolean[3];
    FingerThread[] fingerThread = new FingerThread[3];
    int fingersDown=0;
    TapThread tThread;
    GestureDetector gestureDetector;
    
   @SuppressWarnings("deprecation")
   public MultiTouchDetector(){
	   gestureDetector = new GestureDetector(this);
   }
    
    public void onTouchEvent(MotionEvent event){
    	gestureDetector.onTouchEvent(event);
    	//Log.i("myGesture", fingersDown + " DOWN at: " + event.getEventTime());
		int numFingers = event.getPointerCount();
		int fingerIndex = numFingers-1;
		
    	switch (event.getActionIndex()){
    		case (MotionEvent.ACTION_UP):
    			
    		case (MotionEvent.ACTION_DOWN):
    			FingerThread currentThread = fingerThread[fingerIndex];
    			if (currentThread !=null){
    				currentThread.setStartTime(Calendar.getInstance().getTimeInMillis());
    			}
    			//IF THERE WERE PREVIOUSLY NO FINGERS, LISTEN FOR TAP
    			if (!hasFingersDown()){
    				tThread = new TapThread(numFingers);
    				tThread.start();
    			}
    			else if (tThread!=null){
    				if (numFingers>tThread.getNumFingers()){
    					tThread.setNumFingers(numFingers);
    				}
    			}
    			
    			if (!hasBegun[fingerIndex]){
    				hasBegun[fingerIndex]=true;
    				if (numFingers > fingersDown){
    					fingersDown = numFingers;
    				}
    				currentThread = new FingerThread(numFingers);
    				currentThread.start();
    			}
    			
    		
    		case (MotionEvent.ACTION_MOVE):
    			//Log.i("myGesture", "move with "+ event.getPointerCount());
    		case (MotionEvent.ACTION_OUTSIDE):
    			//Log.i("myGesture", "outside with "+ event.getPointerCount());
    		case (MotionEvent.ACTION_POINTER_DOWN):
    			//Log.i("myGesture", "pointer down with "+ event.getPointerCount());	
    		case (MotionEvent.ACTION_POINTER_UP):
    			//Log.i("myGesture", "pointer up with "+ event.getPointerCount());
    		case (MotionEvent.ACTION_SCROLL):
    			//if (event.getX(0) != event.getHistoricalX(0))
    	
}
    	
}
    
    public synchronized boolean hasFingersDown(){
    	for (boolean b: hasBegun){
    		if (b) return true;
		}
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
			
			int fingersIndex = numFingers-1;
			hasBegun[fingersIndex]=false;
			//Log.i("myGesture", numFingers + " FINGERS LIFT hasFingers is: " + hasFingersDown());
			
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
    	
    boolean notScrolling=true;
    private class TapThread extends Thread{
    	//MAGIC NUMBER
		private final long TIMEOUT= 700L;
		private final long NOFINGERTIMEOUT = 300L;
		private long startTime;
		private int numFingers;
		private long timeRunning=0;
		
		public TapThread(int numFingers){
			this.numFingers=numFingers;
			startTime = Calendar.getInstance().getTimeInMillis();
			notScrolling=true;
		}
		
		public void setNumFingers(int newNumFingers){
			this.numFingers=newNumFingers;
		}
		
		public int getNumFingers(){
			return numFingers;
		}
		
		
		@Override
		 synchronized public void run(){
			//WHILE THERE ARE FINGERS AND WE HAVEN'T TIMED OUT
			while (hasFingersDown() && Calendar.getInstance().getTimeInMillis()-startTime < this.TIMEOUT ){
				timeRunning =Calendar.getInstance().getTimeInMillis()-startTime; //SPIN
			}
			//IF THE FINGERS ARE LIFTED BEFORE TIMEOUT, TAP 
			if (!hasFingersDown()){
				long noFingersStart = Calendar.getInstance().getTimeInMillis();
				while (Calendar.getInstance().getTimeInMillis()- noFingersStart < NOFINGERTIMEOUT){
					if (hasFingersDown()){
						return;
						}
						
					}	
				}
				//MAKE SURE FINGERS STAY OFF FOR A FEW MILIS
				if (!hasFingersDown() && notScrolling){
					Log.i("myGesture", "*****" + this.numFingers + " FINGER TAP******");
				}
			
			
		
		
		
	}  	
    }


	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
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
		final float THRESHOLD = 20;
		if (distanceX > THRESHOLD){
			notScrolling=false;
			Log.i("myGesture", "scrolling back with "+ fingersDown);
		}
		if (distanceX < -1*THRESHOLD){
			notScrolling=false;
			Log.i("myGesture", "scrolling forward with "+ fingersDown);
		}
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
}


    
    
	
	
	
	
	

