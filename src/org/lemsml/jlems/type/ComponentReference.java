package org.lemsml.jlems.type;

import org.lemsml.jlems.annotation.ModelProperty;
import org.lemsml.jlems.annotation.ModelElement;
 
import org.lemsml.jlems.expression.ParseError;
import org.lemsml.jlems.expression.Parser;
import org.lemsml.jlems.logging.E;
import org.lemsml.jlems.sim.ContentError;


@ModelElement(info="A reference to another component. The target component can be accessed with path expressions in the " +
		"same way as a child component, but can be defined independently")
public class ComponentReference implements Named  {

	@ModelProperty(info="")
	public String name;
	@ModelProperty(info="Target type")
	public String type;
	
	public ComponentType r_type;
	
	public boolean isAny = false;
	
	public String compClass;
	
	
	public ComponentReference() {
		
	}
	
	
	public ComponentReference(String sn, String st, ComponentType t) {
		name = sn;
		type = st;
		r_type = t;
	}


   
	
	
	
	public void resolve(Lems lems, Parser p) throws ContentError, ParseError {
		LemsCollection<ComponentType> types = lems.getComponentTypes();
		
		if (type == null && compClass != null) {
			type = compClass;
		}
		
		if (type == null) {
			E.error("no type specified in component ref " + name);
		} else if (type.equals("Component")) {
			isAny = true;
			
		} else {
			ComponentType t = types.getByName(type);
		if (t != null) {
			r_type = t;
			r_type.checkResolve(lems, p);
			
		} else {
			throw new ContentError("ComponentRef: No such typer: " + type + ", used by " + getName());
		}
		}
		
	}


	public String getName() {
		return name;
	}


	public ComponentType getComponentType() {
		return r_type;
	}
	
	
	public String getTargetType() {
		return type;
	}
	
	


	public ComponentReference makeCopy() {
		 return new ComponentReference(name, type, r_type);
	}


	public boolean isLocal() {
		return false;
	}


 
}
