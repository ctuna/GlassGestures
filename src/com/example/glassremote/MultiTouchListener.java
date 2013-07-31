package com.example.glassremote;


import android.util.Log;
import android.view.MotionEvent;

interface MultiTouchListener {
	
	public void onTapUp(int numFingers);
	
	public void onScrollEnded(int numFingers);
	
	public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY, int numFingers);
	
	public void onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY, int numFingers);
	/**
	 * USEFUL FOR GLASS TRACKPAD 
	 * if (distanceX > 0){
			Log.i("myGesture", "scrolling back with "+ fingersDown);
				}
		else{
			Log.i("myGesture", "scrolling forward with "+ fingersDown);
				}
		
	 */
	
}
