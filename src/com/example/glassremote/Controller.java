package com.example.glassremote;

public class Controller {

	
	/**
	 * if (connectingToLaptop == false)
	 * 		the system runs without message relay/IR and has dummy objects
	 */
	public boolean connectingToLaptop = true;
	/**
	 * if (twoNavigates==true)
	 * 		2-finger swiping navigates between select-able objects
	 * 		1-finger swiping navigates within the selected object
	 * else 
	 * 		1-finger swiping navigates between select-able objects
	 * 		2-finger swiping navigates within the selected object
	 */
	public boolean twoNavigates = false;
	
	
	
	
	public boolean getConnectingToLaptop(){
		return connectingToLaptop;
	}
	
	public boolean getTwoNavigates(){
		return twoNavigates;
	}
}
