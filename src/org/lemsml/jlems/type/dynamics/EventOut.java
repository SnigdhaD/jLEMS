package org.lemsml.jlems.type.dynamics;

import org.lemsml.jlems.sim.ContentError;
import org.lemsml.jlems.type.ComponentType;
import org.lemsml.jlems.type.EventPort;

public class EventOut {

	public String port;
	
	public EventPort r_eventPort;

	
	public void resolve(ComponentType base) throws ContentError {
		r_eventPort = base.getOutEventPort(port);
		
	}


	public String getPortName() {
		return r_eventPort.getName();
	}
	
}
