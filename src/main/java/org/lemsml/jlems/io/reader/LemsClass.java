package org.lemsml.jlems.io.reader;

public class LemsClass {

	public Class<?> jclass;
	
	public String section;
	
 	
	public LemsClass(Class<?> c, String sec) {
		jclass = c;
		section = sec;
	}
	
	
	public String getName() {
		String ret = jclass.getSimpleName();
		return ret;
	}
	
	
}
