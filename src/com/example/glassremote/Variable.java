package com.example.glassremote;

import android.util.Log;

public class Variable{
		private boolean isBoolean;
		private boolean isContinuous;
		private int continuousValue;
		private int percentage;
		private int min;
		private int max;
		private boolean on;
		private String name;
		private String abbreviation;
		private MainActivity activity;
		double conversionFactor; 
		String currentValue;
		
		
		public Variable(String name, boolean isBoolean, boolean isContinuous, int min, int max, MainActivity activity){
			this.name=name;
			this.abbreviation= name.substring(0, 3).toUpperCase();
			this.isBoolean=isBoolean;
			this.isContinuous=isContinuous;
			this.activity=activity;
			if (isContinuous){
			this.min=min;
			this.max=max;
			conversionFactor = (double) 100/(Math.abs(max - min));
			}
			
			//TODO: GET DEFAULT VALUE FROM THE OBJECT 
			continuousValue = 20;
			on=false;
		}
	
		
		
		/**
		 * TODO: domain of a variable. how far up/down it can go 
		 * 		ControlledObject lamp = new ControlledObject("lamp", new Variable("light", true, false, this));
		ControlledObject laptop = new ControlledObject("laptop", new Variable("sound", true, true, this), 
				new Variable("brightness", true, true, this), new Variable("scroll position", false, true, this), 
				new Variable("text size", false, true, this));
		 *defaults to "on"/ "off"
		 *v
		 * @param on
		 * @param off
		 * 
		 * 
		 */
		public String getCurrentValue(){
			String toReturn = "";
			//DEFAULT CASE
			if (isBoolean){
				if (on){
					toReturn = "on";
				}
				else toReturn = "off";
			}
			if (isContinuous) 
				{
				toReturn = Integer.toString(getPercentage());
				if (isBoolean){
					if (!on){
						toReturn = "off";
					}
				}
				}
		
			//SPECIAL CASES 
			if (this.name.equals("sound")){
				if (!on) toReturn = "muted";
			}
			return toReturn;
		}


		
		public String getName(){
			return name;
		}
		
		public boolean hasBoolean(){
			return isBoolean;
		}
		public boolean hasContinuous(){
			return isContinuous;
		}
		public void setBoolean(boolean newVal){
			if (isBoolean){
				this.on=newVal;
			}
			else {
				Log.i("myGesture", "Variable does not have boolean value.");
			}
			
			
		}
		
		public boolean getBoolean(){
			if (isBoolean){
			return on;
			}
			else {
				throw new UnsupportedOperationException("Variable does not have boolean value.");
			}
		}
		
		public void setContinuous(int newVal){
			
			
			if (isContinuous){
				if (isBoolean){
					on=true;
				}
				if ((min + (int) (newVal/conversionFactor) >= min) && (min + (int) (newVal/conversionFactor) <= max)){
				this.continuousValue= min + (int) (newVal/conversionFactor);
				}
				else if (min + (int) (newVal/conversionFactor) >= max){
					this.continuousValue= max;
				}
				else if (min + (int) (newVal/conversionFactor) <= min){
					this.continuousValue= min;
				}
				else Log.i("myGesture", "too big or small");
			}
			else {
				throw new UnsupportedOperationException("Variable does not have continuous value.");
			}
			
		}
		
		public int getPercentage(){
			if (isContinuous){
				return (int) ((continuousValue-min)*conversionFactor);
			}
			else {
				throw new UnsupportedOperationException("Variable does not have continuous value.");
			}
		}
		
		public int getContinuous(){
			if (isContinuous){
				return continuousValue;
			}
			else {
				throw new UnsupportedOperationException("Variable does not have continuous value.");
			}
		}
		
		public String getAbbreviation(){
			return this.abbreviation;
		}
		public String toString(){
			return name;
		}
	}