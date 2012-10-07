package org.lemsml.jlemsio.doc;


import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

import org.lemsml.jlems.annotation.ModelProperty;
import org.lemsml.jlems.annotation.ModelElement;
import org.lemsml.jlems.logging.E;
import org.lemsml.jlems.type.Component;
import org.lemsml.jlems.type.LemsCollection;
import org.lemsml.jlems.xml.XMLElement;
import org.lemsml.jlemsio.logging.DefaultLogger;
import org.lemsml.jlemsio.reader.LemsClass;
import org.lemsml.jlemsio.reader.LemsClasses;
import org.lemsml.jlemsio.util.FileUtil;



public class DocExtractor {

 	
	
	public static void main(String[] argv) {
		DefaultLogger.initialize();
		
		DocExtractor de = new DocExtractor();
		de.extract(argv);
	}
	
	 
	
	
	public void extract(String[] argv) {
		
		ArrayList<DocItem> items = new ArrayList<DocItem>();
		
		for (LemsClass lc : LemsClasses.getInstance().getClasses()) {
			items.add(makeDocItem(lc));
		}
		items.add(makeDocItem(new LemsClass(Component.class, "components")));
		
		XMLElement root = new XMLElement("ElementTypes");
		for (DocItem di : items) {
			root.add(di.makeXMLElement());
		}
		
		String stxt = root.serialize();
		// E.info("stxt is " + stxt);
		if (argv.length > 0) {
			String sf = argv[0];
			File f = new File(sf);
			try {
				FileUtil.writeStringToFile(stxt, f);
				E.info("Written file " + f.getAbsolutePath());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			E.info("Got docs xml: " + stxt);
		}
			
	}
	
	
	private DocItem makeDocItem(LemsClass lc) {
	 
			DocItem di = new DocItem(lc.jclass);
	 		
			di.setSection(lc.section);
			
			Annotation[] caa = (Annotation[]) lc.jclass.getAnnotations();
			if (caa != null) {
				for (Annotation a : caa) {
					if (a instanceof ModelElement) {
						ModelElement mel = (ModelElement)a;
						di.setInfo(mel.info());
					}
				}
			}
			
			
			for (Field fld : lc.jclass.getFields()) {
				if (Modifier.isPublic(fld.getModifiers())) {
				
					String fnm = fld.getName();
				 
					
					if (fnm.startsWith("r_") || fnm.startsWith("p_")) {
						// naming convention for semi private fields
					
					} else if (fld.getType().equals(LemsCollection.class)) {
							ParameterizedType t = (ParameterizedType)fld.getGenericType();
 
							Class<?> c =  (Class<?>)(t.getActualTypeArguments()[0]);
						 
							di.addListAttribute(fld.getName(), shortName(c), "");
								 
							
					
					} else {
						Annotation[] aa = fld.getAnnotations();
						if (aa != null) {
							for (Annotation a : aa) {
								if (a instanceof ModelProperty) {
									ModelProperty mat = (ModelProperty)a;
									di.addAttribute(fld.getName(), shortName(fld.getType()), mat.info());
								}
							}
						}
					}
							
				}
				
			}
			return di;
	
	}
	
	
	private String shortName(Class<?> cls) {
		String ret = cls.getName();
		ret = ret.substring(ret.lastIndexOf(".") + 1, ret.length());
		return ret;
	}

	
	
}
