package org.lemsml.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lemsml.annotation.Mat;
import org.lemsml.behavior.Behavior;
import org.lemsml.canonical.CanonicalElement;
import org.lemsml.expression.DoubleEvaluable;
import org.lemsml.expression.ParseError;
import org.lemsml.run.ComponentBehavior;
import org.lemsml.run.Constants;
import org.lemsml.run.MultiComponentBehavior;
import org.lemsml.run.RunDisplay;
import org.lemsml.run.StateRunnable;
import org.lemsml.util.ContentError;
import org.lemsml.util.E;
import org.lemsml.xml.XMLAttribute;
import org.lemsml.xml.XMLElement;


public class Component implements Attributed, IDd, Summaried, Namable, Parented  {

    public static final String THIS_COMPONENT = "this";
    public static final String PARENT_COMPONENT = "parent";
    
	@Mat(info="")
	public String id;

	// name is just used if the parent component contains <xyz type="abc".../>
	// in which case the
	// element is instantiated, called "xyz", and added to the components list
	
	@Mat(info="Name by which the component was declared - this shouldn't be accessible.")
	public String name;
 
	
	@Mat(info="")
	public String type;
	public ComponentType r_type;

	@Mat(info="")
	public String eXtends;

	public LemsCollection<Attribute> attributes = new LemsCollection<Attribute>();

	public LemsCollection<ParamValue> paramValues;

	
	public LemsCollection<Insertion> insertions = new LemsCollection<Insertion>();
	
	
	public LemsCollection<Component> components = new LemsCollection<Component>();
	
	@Mat(info="Metadata about a model can be included anywhere by wrapping it in an About element, though this " +
			"is not necessary: LEMS does not use the body text of XML elements itself, so this is free for the " +
			"modeler to include descriptive text or other markup of thier own.")
	public LemsCollection<About> abouts = new LemsCollection<About>();

	

	@Mat(info="Structured metadata can be put in Meta elements. The content is read into a generic xml data structure. " +
			"Other tools can then do their own thing with it. Each Meta element should set the context attribute, so " +
			"tools can use the getMeta(context) method to retrieve elements that match a particular context.")
	public LemsCollection<Meta> metas;
	

	
	
	
	public double xPosition;
	public double yPosition;
	
	
	HashMap<String, TextParam> textParamHM = new HashMap<String, TextParam>();

	HashMap<String, Component> childHM;

	HashMap<String, Component> refHM;

	ArrayList<String> childrenNames;
	HashMap<String, ArrayList<Component>> childrenHM;

	
	
	
	private boolean resolved = false;

	private boolean evaluatedStatic = false;

	Component r_parent;

	boolean madeCB = false;

	private ComponentBehavior componentBehavior;
 	
	
	public Component() {
	}

	public Component(String id, ComponentType componentType) {
		this.id = id;
		this.r_type = componentType;
		this.type = r_type.getName();
	}

	public void setID(String s) {
		id = s;
	}

	public void setName(String s) {
		name = s;
	}

	public String getName() {
		return name;
	}

	 
	@Override
	public String toString() {
		return "Component(id=" + id + " type=" + type + ")";
	}

	public String summary() {
		return ComponentWriter.summarize(this);
	}
	
	

	public String details(String indent) {
		return ComponentWriter.writeDetails(this, indent);
	}
	
	
	

	public void setParent(Object ob) throws ContentError {
  		if (ob instanceof Lems) {
			// we're a top level component - no parent. Could use this to set a
			// flag... TODO
		} else if (ob instanceof Component) {
			r_parent = (Component) ob;
			
		} else {
            String err = "Setting parent of [" + this + "] to [" + ob +  "] failed\n";
            err += "It should be the root LEMS element or another component. ";
			throw new ContentError(err);
		}
	}

	public void addAttribute(Attribute att) {
		attributes.add(att);
	}

	public String getID() {
		return id;
	}

	public String getUniqueID() {
		if (id!=null) return id;
        else return this.getParent().getUniqueID()+"_"+getName();
	}

	public void setType(ComponentType ct) {
		r_type = ct;
		if (type == null) {
			type = r_type.getName();
		}
	}

	public void checkResolve(Lems n, ComponentType parentType) throws ContentError, ParseError {
		// E.info("Checking resolve on "+getID());
		if (!resolved) {
			resolve(n, parentType);
		}
	}

	public String getPathParameterPath(String paramName) throws ContentError {
		String ret = null;

		// MUSTDO - all a bit adhoc
		if (attributes.hasName(paramName)) {
			Attribute att = attributes.getByName(paramName);
			ret = (att.getValue());
		} else if (paramName.indexOf("../") == 0) {
			ret = getParent().getPathParameterPath(paramName.substring(3, paramName.length()));
		}
		
		if (ret == null) {
			throw new ContentError("no value specified for parameter " + paramName);
		}

		return ret;
	}
	public void resolve(Lems lems, ComponentType parentType) throws ContentError, ParseError {
		resolve(lems, parentType, true);
	}
	
	public void resolve(Lems lems, ComponentType parentType, boolean bwarn) throws ContentError, ParseError {
		if (eXtends != null) {
			if (lems.hasComponent(eXtends)) {
				Component cp = lems.getComponent(eXtends);
				for (Insertion ins : cp.insertions) {
					insertions.add(ins.makeCopy());
				}
			}
		}
			
			
		for (Insertion ins : insertions) {
			Component cpt = lems.getComponent(ins.component);
			if (cpt != null) {
				components.add(cpt);
			}
		}
		
		
		for (Attribute att : attributes) {
			att.clearFlag();
		}

		if (paramValues == null) {
			paramValues = new LemsCollection<ParamValue>();
		}
		if (childHM == null) {
			childHM = new HashMap<String, Component>();
		}
		if (refHM == null) {
			refHM = new HashMap<String, Component>();
		}
		if (childrenHM == null) {
			childrenNames = new ArrayList<String>();
			childrenHM = new HashMap<String, ArrayList<Component>>();
		} else {
			for (String children : childrenNames) {
				for (Component comp : childrenHM.get(children)) {
					comp.checkResolve(lems, parentType);
				}
			}
		}

		// attributes.size());

		Component pinst = null;

		
		if ((eXtends != null && type == null) || (type != null && type.equals("Component"))) {			
			// an Instance without a "class" attribute - it must have a "proto"
			// attribute instead or its an error
			if (eXtends != null) {
				if (lems.hasComponent(eXtends)) {
					pinst = lems.getComponent(eXtends);
					pinst.checkResolve(lems, parentType);
					r_type = pinst.getComponentType();

					 
					
				} else {
					throw new ContentError("no such component " + eXtends + " (needed for proto of " + id);
				}

			} else {
				throw new ContentError("Component " + id + " must set 'type' or 'extends' attributes");
			}
		} else {
			if (type == null && name != null) {
				if (parentType != null) {
					type = parentType.getChildType(name);
				}
				if (type == null) {
					type = name;
				}
			}
			if (type == null) {
				throw new ContentError("No type for " + this);
			}
			
			r_type = lems.getComponentTypeByName(type);
			
			if (eXtends != null) {
				if (lems.hasComponent(eXtends)) {
					pinst = lems.getComponent(eXtends);
					pinst.checkResolve(lems, parentType);
					r_type = pinst.getComponentType();
					if (r_type.getName().equals(type)) {
						// as it should be
					} else {
						throw new ContentError("extension of " + id + " from " + pinst + " tries to change type");
					}
					
				} else {
					throw new ContentError("no such component " + eXtends + " (needed for proto of " + id);
				}
			}
		}

		
		
		if (r_type == null) {
			throw new ContentError("no type found for " + id);
		} else {
			r_type.addCpt(this);
		}

		for (FinalParam dp : r_type.getFinalParams()) {
	 		
			ParamValue pv = new ParamValue(dp);
			paramValues.add(pv);
			String pvn = pv.getName();
			String atval = null;

			boolean gotFromProto = false;
			if (pinst != null) {
				ParamValue protoPV = pinst.getParamValue(pv.getName());
				if (protoPV != null) {
					pv.copyFrom(protoPV);
					gotFromProto = true;
				} else {
					E.info("proto params " + pinst.stringParams());
					E.error("proto " + eXtends + " provides no pv for " + pv.getName());
				}
			}

			if (attributes.hasName(pvn)) {
				Attribute att = attributes.getByName(pvn);
				atval = (att.getValue());
				att.setFlag();

			} else if (gotFromProto) {
				// OK - the prototype supplied a value

			} else if (dp.hasSValue()) {
				atval = dp.getSValue();

			} else if (dp instanceof DerivedFinalParam) {
				// will populate it later

			} else {
				E.simpleError("no value supplied for parameter: " + pvn + " in " + id + " (" + type + ")");
			}
					
			if (atval != null) {
				pv.setValue(atval, lems.getUnits());
			}
		}

		for (ComponentReference cr : r_type.getComponentRefs()) {
			String crn = cr.getName();
			if (attributes.hasName(crn)) {
				Attribute att = attributes.getByName(crn);
				String attval = att.getValue();
				att.setFlag();
				Component cpt = lems.getComponent(attval);

				if (cpt != null) {
					refHM.put(crn, cpt);

				} else {
					String err = "No such component: " + crn + " " + attval + ", existing:";
					for (Component comp : lems.components) {
						err = err + "\n   " + comp;
					}
					E.error(err);
				}
			} else {
				// can be OK to resolve with dangling refs as long as we will resolve them again
				// before trying to run it
				if (bwarn) {
					E.warning("component reference " + crn + " missing in " + this + " type " + r_type);
				} 
			}
		}

		for (Link lin : r_type.getLinks()) {
			String crn = lin.getName();
			if (attributes.hasName(crn)) {
				Attribute att = attributes.getByName(crn);
				String attval = att.getValue();
				att.setFlag();
				Component cpt = null;

				/*
				 * PathEvaluator pe = new PathEvaluator(); cpt =
				 * pe.getComponent(r_parent, attval);
				 */

				cpt = r_parent.getLocalByID(attval);

				if (cpt != null) {
					refHM.put(crn, cpt);
				} else {
					E.error("The path " + attval + " for attribute '" + crn
					        + "' does not match any component relative to my ("+this+") parent:\n" + this.getParent().details(""));
				}
			} else {
				if (bwarn) {
					throw new ContentError("Component " + this + " must supply a value for link '" + crn + "'");
				}
			}
		}

		for (Component cpt : components) {		
			cpt.checkResolve(lems, r_type);
			String scb = cpt.getName();
			boolean done = false;
			if (scb != null) {
				if (r_type.hasChild(scb)) {
					childHM.put(scb, cpt);
					done = true;
				}
			}

			if (!done) {
				Children children = r_type.getChildren(cpt.getComponentType());
				if (children != null) {
					String st = children.getName();

					if (st == null) {
						throw new ContentError("anon children array " + children);
					}

					if (childrenHM.containsKey(st)) {
						childrenHM.get(st).add(cpt);

					} else {
						ArrayList<Component> acpt = new ArrayList<Component>();
						acpt.add(cpt);
						childrenNames.add(st);
						childrenHM.put(st, acpt);
					}
					done = true;
				}
			}
			if (!done) {
				throw new ContentError("no such child allowed: " + scb);
			}
		}

		for (Path p : r_type.getPaths()) {
			flagAttribute(p.getName());
		}

		for (ComponentTypeRef ctr : r_type.getComponentTypeRefs()) {
			flagAttribute(ctr.getName());
		}

		for (PathParameter pp : r_type.getPathParameters()) {
			flagAttribute(pp.getName());
		}

		for (Text t : r_type.getTexts()) {
			String tnm = t.getName();
			flagAttribute(t.getName());
			if (attributes.hasName(tnm)) {
				textParamHM.put(tnm, new TextParam(tnm, attributes.getByName(tnm).getValue()));
			}
		}

		for (Attribute att : attributes) {
			if (att.flagged()) {
				// fine - we've used it
			} else {
				E.shortWarning("Unused attribute in " + this + ": " + att);
			}
		}

		resolved = true;

		// E.info("--------    "+this.getID()+ ": childrenHM: "+childrenHM+
		// ": paramValues: "+paramValues+ ": textParamHM: "+textParamHM+
		// ": attributes: "+attributes);
	}

	public void addToChildren(String childrenName, Component c) throws ContentError {

		if (childrenHM == null)
        {
			childrenNames = new ArrayList<String>();
			childrenHM = new HashMap<String, ArrayList<Component>>();
        }

		if (childrenHM.containsKey(childrenName)) {
			childrenHM.get(childrenName).add(c);

		} else {
			ArrayList<Component> acpt = new ArrayList<Component>();
			acpt.add(c);
			childrenNames.add(childrenName);
			childrenHM.put(childrenName, acpt);
		}
		// E.info(this.getID()+ ": childrenHM: "+childrenHM);
	}

	public void evaluateStatic(Lems lems) throws ContentError, ParseError {
		if (evaluatedStatic) {
			return;
		}
		HashMap<String, Double> valHM = new HashMap<String, Double>();

		for (ParamValue pv : paramValues) {
			valHM.put(pv.getName(), pv.getDoubleValue());
		}

		for (FinalParam fp : r_type.getFinalParams()) {
			if (fp instanceof DerivedFinalParam) {
				DerivedFinalParam dfp = (DerivedFinalParam) fp;

				double qv = 0.;
				if (dfp.isSelect()) {
					String sel = dfp.getSelect();
					qv = PathEvaluator.getValue(lems, this, sel);

				} else if (dfp.isValue()) {
					// TODO this can be done in the class, not here
					DoubleEvaluable evaluable = lems.getParser().parseExpression(dfp.getValueString());

					qv = evaluable.evalD(valHM);
				}

				if (paramValues.hasName(dfp.getName())) {
					paramValues.getByName(dfp.getName()).setDoubleValue(qv);
				} else {
					paramValues.add(new ParamValue(dfp, qv));
				}
				valHM.put(dfp.getName(), qv);

			}
		}
		evaluatedStatic = true;
		for (Component cpt : components) {
			cpt.evaluateStatic(lems);
		}
	}

	// TODO evaluate phase for components
	/*
	 * for (ExternalQuantity equan : externalQuantitys) { String qn =
	 * equan.getName(); double qv = PathEvaluator.getValue(cpt,
	 * equan.getPath()); ret.addFixed(qn, qv); fixedHM.put(qn, qv); // MUSTDO we
	 * don't need both of these: // either Ext quans shouldn't say they are
	 * fixed and we should use ret.addFixed, or it should // and use
	 * fixedHM.put. }
	 */

	private void flagAttribute(String pn) throws ContentError {
		if (attributes.hasName(pn)) {
			Attribute att = attributes.getByName(pn);
			att.setFlag();
		}
	}

	public LemsCollection<ParamValue> getParamValues() {
		return paramValues;
	}

	private Component getLocalByID(String sid) {
		Component ret = null;
		for (Component c : components) {
			String cid = c.getID();
			if (cid != null && cid.equals(sid)) {
				ret = c;
				break;
			}
		}
		return ret;
	}

	private String stringParams() {
		return paramValues.toString();
	}

	public ParamValue getParamValue(String pvn) throws ContentError {
		// paramValues.getByName(pvn);

		ParamValue ret = null;
		if (pvn.indexOf("/") >= 0) {
			ret = getPathParamValue(pvn.split("/"));
		} else {
			ret = paramValues.getByName(pvn);
		}
		if (ret == null) {
			String warn = "No such param: " + pvn + " on " + getID() + ", existing params:";
			for (ParamValue pv : paramValues) {
				warn = warn + "\n" + pv;
			}
			E.warning(warn + "\n");
		}
		return ret;
	}

	public ComponentType getComponentType() {
		return r_type;
	}

	public void setDeclaredName(String snm) {
		name = snm;
	}
	
	
	public void setTypeName(String scl) {
		type = scl;
	}

	

	public String getTextParam(String pnm) {
		String ret = null;
		if (textParamHM.containsKey(pnm)) {
			ret = textParamHM.get(pnm).getText();
		} else {
			return null;
		}
		return ret;
	}

	public ComponentBehavior makeComponentBehavior() throws ContentError, ParseError {
	
		if (madeCB) {
			throw new ContentError("remaking a component behavior that is already made " + id + " " + r_type);
		}

		HashMap<String, Double> fixedHM = new HashMap<String, Double>();

		HashMap<String, Double> chm = Constants.getConstantsHM();
		if (chm != null) {
			fixedHM.putAll(chm);
		}
		
		for (ParamValue pv : paramValues) {
			fixedHM.put(pv.getName(), pv.getDoubleValue());
		}

		ComponentBehavior ret = null;
		if (r_type.hasBehavior()) {
			Behavior bv = r_type.getBehavior();
			ret = bv.makeComponentBehavior(this, fixedHM);
		} else {
 			ret = new ComponentBehavior(getID(), getComponentType().getName());
			for (ParamValue pv : getParamValues()) {
				 String qn = pv.getName();
				 double qv = pv.getDoubleValue();
				 ret.addFixed(qn, qv);
			}

		}
		for (Property p : getComponentType().getPropertys()) {
			String pnm = p.getName();
			ret.addExposureMapping(pnm, pnm);
		}

		for (Text text : r_type.getTexts()) {
			String tnm = text.getName();
			if (attributes.hasName(tnm)) {
				ret.addTextParam(tnm, attributes.getByName(tnm).getValue());
			}
		}

		for (String s : refHM.keySet()) {
			Component ch = refHM.get(s);
			ComponentBehavior chb = ch.getComponentBehavior();
			ret.addRefComponentBehavior(s, chb);
		}

		for (String s : childHM.keySet()) {
			Component ch = childHM.get(s);
			ComponentBehavior chb = ch.getComponentBehavior();
			ret.addChildComponentBehavior(s, chb);
		}

		for (String s : childrenNames) {
			ArrayList<Component> cpts = childrenHM.get(s);
			ArrayList<ComponentBehavior> cba = new ArrayList<ComponentBehavior>();
			for (Component c : cpts) {
				cba.add(c.getComponentBehavior());
			}
			ret.addMultiComponentBehavior(s, new MultiComponentBehavior(cba));
		}

		for (Attachments ats : r_type.getAttachmentss()) {
			ret.addAttachmentSet(ats.getName(), ats.getComponentType().getName());
		}

		for (Collection c : r_type.getCollections()) {
			ret.addInstanceSet(c.getName());
		}

		for (PairCollection c : r_type.getPairCollections()) {
			ret.addInstancePairSet(c.getName());
		}
		
		madeCB = true;
		
	  
		componentBehavior = ret; // TODO maybe delete this ref later?
		return ret;
	}
	
	
	public ComponentBehavior makeConsolidatedCoponentBehavior(String knownas) throws ContentError, ParseError {
		ComponentBehavior cb = getComponentBehavior();
	    ComponentBehavior ret = cb.makeConsolidatedBehavior(knownas);
 	    return ret;
	}
	
	

	public ComponentBehavior getComponentBehavior() throws ContentError, ParseError {
		ComponentBehavior ret = null;
		 
		if (componentBehavior == null) {
			makeComponentBehavior();
		}
		ret = componentBehavior;
		return ret;
	}

	
	public boolean hasChildrenAL(String s) {
		boolean ret = false;
		if (childrenHM.containsKey(s)) {
			ret = true;
		}
		return ret;
	}

	public ArrayList<Component> getChildrenAL(String s) {

		ArrayList<Component> children = childrenHM.get(s);
		if (children == null) {
			//E.info("No children of class: " + s + " in " + this.getID() + ", valid values: " + childrenHM.keySet());
			return new ArrayList<Component>();
		}
		return children;

	}
	
	
	public HashMap<String, Component> getChildHM() {
		return childHM;
	}
	
	public HashMap<String, Component> getRefHM() {
 		return refHM;
	}

	
	
	public ArrayList<Component> getStrictChildren() {
		ArrayList<Component> comps = new ArrayList<Component>();
		for (ArrayList<Component> compList : childrenHM.values()) {
			comps.addAll(compList);
		}
		return comps;
	}
	
	public HashMap<String, Component> getRefComponents() {
		return refHM;
	}

	

	public ArrayList<Component> getAllChildren() {
		ArrayList<Component> comps = new ArrayList<Component>();
		comps.addAll(childHM.values());
		for (ArrayList<Component> compList : childrenHM.values()) {
			comps.addAll(compList);
		}
		comps.addAll(refHM.values());
		return comps;
	}

	public boolean hasAttribute(String s) throws ContentError {
		boolean ret = false;
		if (attributes.hasName(s)) {
			ret = true;
		}
		return ret;
	}

	public String getStringValue(String sn) throws ContentError {
		String ret = null;

        if (sn.equals(THIS_COMPONENT))
            return THIS_COMPONENT;

        if (sn.equals(PARENT_COMPONENT))
            return PARENT_COMPONENT;


		if (refHM.containsKey(sn)) {
			ret = refHM.get(sn).getID();

		} else if (childHM.containsKey(sn)) {
			ret = childHM.get(sn).getID();

		} else if (attributes.getByName(sn) != null) {
			ret = attributes.getByName(sn).getValue();

		} else {
			throw new ContentError("No such field '"
                    + sn + "' in " + this + "\n"+details(""));
		}

		// which should be the same as attval(sn);
		Attribute att = attributes.getByName(sn);
		if (att != null && att.getValue().equals(ret)) {
			// all well
		} else {
			throw new ContentError("Get string value ("+sn+") mismatch on component ref "+this);
		}
		return ret;
	}

	public Component getParent() {
		return r_parent;
	}

	public Component quietGetChild(String rp) {
		Component ret = null;
		if (childHM.containsKey(rp)) {
			ret = childHM.get(rp);
		} else if (refHM.containsKey(rp)) {
			ret = refHM.get(rp);
		}
		return ret;
	}

	public Component getChild(String rp) throws ContentError {
		Component ret = quietGetChild(rp);
		if (ret == null) {
			String info = "no such child " + rp + " in " + this;
			info = info + "\n - Child: " + childHM;
			info = info + "\n - Children: " + childrenHM;
			info = info + "\n - Refs: " + refHM;
			throw new ContentError(info);
		}
		return ret;
	}

	public CanonicalElement makeCanonical() {
		CanonicalElement ret = new CanonicalElement("Component");
		// MUSTDO
		ret.add(new CanonicalElement("id", id));
		if (type.equals("Component")) {
			// leave it out
		} else {
			ret.add(new CanonicalElement("class", type));
		}

		if (eXtends != null) {
			ret.add(new CanonicalElement("extends", eXtends));
		}
		for (ParamValue pv : paramValues) {
			ret.add(pv.makeCanonicalElement());
		}

		for (Component c : components) {
			ret.add(c.makeCanonical());
		}

		return ret;
	}

	public ParamValue getPathParamValue(String[] bits) throws ContentError {
		ParamValue ret = null;
		if (bits.length == 1) {
			ret = getParamValue(bits[0]);
		} else {
			String[] rbits = new String[bits.length - 1];
			for (int i = 0; i < bits.length - 1; i++) {
				rbits[i] = bits[i + 1];
			}

			Component cpt = getRelativeComponent(bits[0]);
			return cpt.getPathParamValue(rbits);
		}
		return ret;
	}

	private Component getRelativeComponent(String nm) throws ContentError {
		Component ret = null;
		if (childHM.containsKey(nm)) {
			ret = childHM.get(nm);

		} else if (refHM.containsKey(nm)) {
			ret = refHM.get(nm);
		} else {
			throw new ContentError("no such relative component: " + nm + " rel to " + this);
		}
		return ret;
	}

	// TODO more general
	public Component getScopeComponent(String at) throws ContentError {
		Component p = this;
		Component ret = null;
		for (int nu = 0; nu < 3; nu++) {
			if (p != null && p.quietGetChild(at) != null) {
				ret = p.quietGetChild(at);
				break;
			} else if (p != null) {
				p = p.getParent();
			} else {
				break;
			}
		}
		if (ret == null) {
			throw new ContentError("Can't locate '" + at + "' relative to " + this);
		}
		return ret;
	}

	public void setParameter(String sa, String sv) {
		attributes.add(new XMLAttribute(sa, sv));
	}

	public String getAbout() {
		String ret = "";
		for (About about : abouts) {
			ret += about.getText();
		}
		return ret;
	}

	public void clear() {
		attributes.clear();
	}

	public HashMap<String, String> getTextParamMap() {
		HashMap<String, String> ret = new HashMap<String, String>();
		for (String s : textParamHM.keySet()) {
			ret.put(s, textParamHM.get(s).getText());
		}
		return ret;
    }

	public String getTypeName() {
		String ret = null;
		if (r_type != null) {
			ret = r_type.getName();
		} else if (type != null) {
			ret = type;
		}  
		return ret;
	}
	
	public String getExtendsName() {
		String ret = null;
		if (eXtends != null) {
			ret = eXtends;
		}
		return ret;
	}

	public LemsCollection<Attribute> getAttributes() {
		return attributes;
	}

	public void setPosition(double x, double y) {
		 xPosition = x;
		 yPosition = y;
	}

	public XMLElement getContextMeta(String ctxt) {
		XMLElement ret = null;
		for (Meta m : metas) {
			if (m.context != null && m.context.equals(ctxt)) {
				ret = m.getXMLValue();
			}
		}
		return ret;
	}
	public void removeChild(Component c) {
		E.missing("Need to remove " + c + " from parent component");
		
	}

	public LemsCollection<Component> getComponents() {
		return components;
	}


	 
}
