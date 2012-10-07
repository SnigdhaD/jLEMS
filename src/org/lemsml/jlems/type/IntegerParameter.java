package org.lemsml.jlems.type;

import org.lemsml.jlems.sim.ContentError;

public class IntegerParameter extends Parameter {
	
	
	@Override
	public void resolve(final LemsCollection<Dimension> dimensions) throws ContentError {
		    
	        if (dimension != null) {
	           throw new ContentError("Integer paramters must be dimensionless: " + name);
	            
	        }  
	        r_dimension = new Dimension();
	    }
}
