package edu.eecs.berkeley.glassremote;

import java.util.ArrayList;

public class ControlledObject {

	private String name;
	private ArrayList<Variable> variables;
	private int id;
	public ControlledObject(String name, int id, Variable... vars){
		this.name=name;
		this.id=id;
		variables = new ArrayList<Variable>();
		for (Variable v: vars){
			variables.add(v);
		}
	}
	
	public ArrayList<Variable> getVariables(){
		return variables;
	}
	
	public String getName(){
		return name;
	}
	
	public String toString(){
		return name;
	}
	
	public int getId(){
		return id;
	}
	
}
