package edu.eecs.berkeley.glassremote;

import java.util.ArrayList;

import android.util.Log;

public class PhysicalTarget {

  private final String TAG = "PhysicalTarget";
  private String name;

  public final int DIRECTION_LEFT = 1;
  public final int DIRECTION_RIGHT = 2;
  public final int DIRECTION_UP = 3;
  public final int DIRECTION_DOWN = 4;

  private float pitch;  // up-down orientation
  private float yaw;    // left-right orientation
  
  @SuppressWarnings("unused")
  private float roll;   // not used in this case

  public PhysicalTarget left;
  public PhysicalTarget right;
  public PhysicalTarget up;
  public PhysicalTarget down;

  private int id;
  public PhysicalTarget(String name, int id){
    this.name=name;
    this.id=id;
    this.left = null;
    this.right = null;
    this.up = null;
    this.down = null;
  }

  // a list of update functions
  public void updateOrientation(float pitch, float yaw, float roll) {
    this.setPitch(pitch);
    this.setYaw(yaw);
    this.roll = roll;
  }

  // for now we only have four neighbors as the implementation, we might need to update this later
  // not urgent to have 8
  public void updateNeighbor(PhysicalTarget neighbor, int direction) {
    switch (direction) {
    case DIRECTION_LEFT: 
      this.left = neighbor;
      break;
    case DIRECTION_RIGHT: 
      this.right = neighbor;
      break;
    case DIRECTION_UP: 
      this.up = neighbor;
      break;
    case DIRECTION_DOWN: 
      this.down = neighbor;
      break;
    default: 
      Log.i(TAG, "updating neighbor - WRONG");
    }
  }
  
  public PhysicalTarget getNeighbor(int direction) {
    switch (direction) {
    case DIRECTION_LEFT: 
      return this.left;
    case DIRECTION_RIGHT: 
      return this.right;
    case DIRECTION_UP: 
      return this.up;
    case DIRECTION_DOWN: 
      return this.down;
    default: 
      Log.i(TAG, "getting neighbor - WRONG");
    }
    return null;
  }

  // a list of get functions
  public String getName(){
    return name;
  }

  public int getId(){
    return id;
  }

  public String toString(){
    return name;
  }

  public float getPitch() {
    return pitch;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  public float getYaw() {
    return yaw;
  }

  public void setYaw(float yaw) {
    this.yaw = yaw;
  }

}
