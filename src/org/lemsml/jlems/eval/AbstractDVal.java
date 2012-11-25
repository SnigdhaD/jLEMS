package org.lemsml.jlems.eval;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class AbstractDVal {

	public abstract double eval();

	public abstract void recAdd(ArrayList<DVar> val);
     
	public abstract AbstractDVal makeCopy();

	public abstract AbstractDVal makePrefixedCopy(String pfx, HashSet<String> stetHS);

	public abstract void substituteVariableWith(String vnm, String pth);

	public abstract boolean variablesIn(HashSet<String> known);

	public abstract String toExpression();
	 
}