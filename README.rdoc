=GlassGestures 
====listeners for 1, 2 and 3 finger tapping and scrolling on Android (built for Google Glass)



==Getting started

1) import "MultiTouchDetector.java" and "MultiTouchListener.java".
  
2) In your activity, implement GestureDetector.OnGestureListener and MultiTouchListener
  public class MainActivity extends Activity implements GestureDetector.OnGestureListener, MultiTouchListener{
        ...
      }
  
3) declare a MultiTouchDetector and a GestureDetector 
      MultiTouchDetector multiTouchDetector;
      GestureDetector gestureDetector;
  
4)  instantiate the detectors in onCreate
      gestureDetector = new GestureDetector(this, this);
      multiTouchDetector = new MultiTouchDetector(this);
  
5) send events to the multiTouchDetector in onGenericMotionEvent
    
      @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
          	multiTouchDetector.onTouchEvent(event);
    		    return true;
        }
  
6) add unimplemented methods for the onGestureListener and MultiTouchListener. The following methods will be called when a tap or scroll
has been detected, with numFingers indicating the number of fingers in the event.
 
      public void onTapUp(int numFingers);	
      public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, int numFingers);
      
    
