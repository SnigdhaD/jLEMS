package org.lemsml.jlems.type.dynamics;

import org.lemsml.jlems.annotation.ModelElement;
import org.lemsml.jlems.type.Dimension;

 
@ModelElement(info="Has a variable and a value. The value is the rate of change of the variable.")
public class TimeDerivative extends StateChange {

	
	public TimeDerivative() {
		super();
	}
	
    public TimeDerivative(String vnm) {
    	super(vnm);
    }

    
    public TimeDerivative(String vnm, String value) {
        this(vnm);
        this.value = value;
    }


	 
	public Dimension getStateVariableDimensionMultiplier() {
		 Dimension ret = new Dimension("per_time");
		 ret.setT(-1);
		 return ret;
	}

	
}
