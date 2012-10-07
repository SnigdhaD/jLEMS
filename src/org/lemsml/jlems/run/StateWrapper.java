package org.lemsml.jlems.run;

import org.lemsml.jlems.display.LineDisplay;
import org.lemsml.jlems.logging.E;
import org.lemsml.jlems.sim.ContentError;


// TODO this shouldn't implement StateRunnable 
public class StateWrapper {

	
	StateInstance unitInstance = null;
	RegimeStateInstance regimeInstance = null;
	
	String varname;

	
	
	public StateWrapper(StateInstance ui, String snm) {
		unitInstance = ui;
		varname = snm;
	}

	public StateWrapper(RegimeStateInstance ui, String snm) {
		regimeInstance = ui;
		varname = snm;
	}

	
	
	public StateRunnable getChild(String snm) {
		E.missing();
		return null;
	}

	
	public double getValue() throws ContentError, RuntimeError {
		double ret = Double.NaN;
		if (unitInstance != null) {
 			ret = unitInstance.getVariable(varname);
			
		} else if (regimeInstance != null) {
			ret = regimeInstance.getVariable(varname);
		} else {
			throw new ContentError("State wrapper empty");
		}
		return ret;
	}

 
	public void advance(StateInstance parent, double t, double dt) {
	 
	}
 
	public void exportState(String pfx, double t, LineDisplay ld) {
	 
	}
	
	
}
