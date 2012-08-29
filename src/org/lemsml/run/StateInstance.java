package org.lemsml.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.lemsml.display.LineDisplay;
import org.lemsml.util.ContentError;
import org.lemsml.util.E;
import org.lemsml.util.RuntimeError;

public class StateInstance implements StateRunnable {

	ComponentBehavior uclass;
	String id;
	private HashMap<String, DoublePointer> varHM;
	private HashMap<String, DoublePointer> expHM;

	// TODO only use these if there is more than one;
	HashMap<String, InPort> inPortHM = new HashMap<String, InPort>();
	HashMap<String, OutPort> outPortHM = new HashMap<String, OutPort>();
	OutPort firstOut;
	InPort firstIn;
	boolean hasChildren = false;

	ArrayList<StateInstance> childA;
	HashMap<String, StateInstance> childHM;
	
	boolean hasMulti = false;
	ArrayList<MultiInstance> multiA;
	
	HashMap<String, MultiInstance> multiHM;
	boolean singleAMI = false;
	MultiInstance onlyAMI = null;
	
	boolean singleIS = false;
	InstanceSet<StateInstance> onlyIS = null;
	boolean resolvedPaths = false;
	HashMap<String, StateInstance> pathSIHM;
	HashMap<String, ArrayList<StateInstance>> pathAHM;

	HashMap<String, StateInstance> idSIHM;
	boolean hasSchemes = false;
	ArrayList<KSchemeInst> schemeA;
	HashMap<String, KSchemeInst> schemeHM;
	boolean hasRegimes = false;
	HashMap<String, RegimeStateInstance> regimeHM;

	RegimeStateInstance activeRegime = null;
	HashMap<String, InstanceSet<StateInstance>> instanceSetHM;
	HashMap<String, InstancePairSet<StateInstance>> instancePairSetHM;
	ArrayList<DestinationMap> dmaps;
	ArrayList<Builder> builders;
	StateInstance parent;
	Object work; // just a a cache - TODO not a good solution
	boolean built = false;
	boolean initialized = false;
	double currentTime = Double.NaN;

	boolean debug = false;

	EventManager eventManager;
	
	
	public StateInstance() {
		// for standalone instances that aren't related to a Behavior - see
		// PairFilter for usage
	}

	public StateInstance(ComponentBehavior uc) {
		uclass = uc;
		id = uc.getComponentID();
	}

	public String getID() {
		return id;
	}

	public void setParent(StateInstance p) {
		parent = p;
	}

	public StateInstance getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return (uclass != null ? "StateInstance of " + uclass.toString() : "dummy instance");
	}

	public double getCurrentTime() {
		return currentTime;
	}
	
	
	public void setEventManager(EventManager em) {
		eventManager = em;
	}
	
	
	public EventManager getEventManager() throws ConnectionError {
		EventManager ret = null;
		if (eventManager != null) {
			ret = eventManager;
		} else if (parent != null) {
			ret = parent.getEventManager();
		}
		if (ret == null) {
			throw new ConnectionError("Can't get event manager ?" + this);
		}
		return ret;
	}
	

	public void initialize(StateRunnable parent) throws RuntimeError, ContentError {
	 
		
		currentTime = 0;
		uclass.initialize(this, parent, false);
		if (debug) {
			E.info("Post init " + this + " has vars: " + this.varHM + " and exps " + this.expHM);
		}

		if (hasChildren) {
			for (StateInstance ch : childA) {
				ch.initialize(this);
			}
		}
		if (hasMulti) {
			for (MultiInstance mi : multiA) {
				mi.initialize(this);
			}
		}

		if (hasSchemes) {
			for (KSchemeInst ksi : schemeA) {
				ksi.initialize(this);
			}
		}

		uclass.initialize(this, parent, true);
		if (debug) {
			E.info("Post CHILDREN init " + this + " has vars: " + this.varHM + " and exps " + this.expHM + "\n");
		}

		// Once more
		if (hasChildren) {
			for (StateInstance ch : childA) {
				ch.initialize(this);
			}
		}
		if (hasMulti) {
			for (MultiInstance mi : multiA) {
				mi.initialize(this);
			}
		}

		if (hasSchemes) {
			for (KSchemeInst ksi : schemeA) {
				ksi.initialize(this);
			}
		}

		uclass.initialize(this, parent, true);
		if (debug) {
			E.info("Post CHILDREN init " + this + " has vars: " + this.varHM + " and exps " + this.expHM + "\n");
		}
	}

	
	
	public void evaluate(StateRunnable parent) throws RuntimeError, ContentError {
		if (!built) {
			throw new RuntimeError("advance() called before build on " + this);
		}
		if (!initialized) {
			this.initialize(parent);
		}

		if (hasChildren) {
			for (StateInstance ch : childA) {
				ch.evaluate(this);
			}
		}
		if (hasMulti) {
			for (MultiInstance mi : multiA) {
				mi.evaluate(this);
			}
		}

		if (hasSchemes) {
			for (KSchemeInst ksi : schemeA) {
				ksi.evaluate(this);
			}
		}
		uclass.evaluate(this,  parent);

		if (hasRegimes) {
			activeRegime.evaluate(this);
		}
	}
	
	
	
	
	
	
	
	public void advance(StateRunnable parent, double t, double dt) throws RuntimeError, ContentError {
		if (!built) {
			throw new RuntimeError("advance() called before build on " + this);
		}

		currentTime = t;

		if (!initialized) {
			this.initialize(parent);
		}

		if (hasChildren) {
			for (StateInstance ch : childA) {
				ch.advance(this, t, dt);
			}
		}
		if (hasMulti) {
			for (MultiInstance mi : multiA) {
				mi.advance(this, t, dt);
			}
		}

		if (hasSchemes) {
			for (KSchemeInst ksi : schemeA) {
				ksi.advance(this, t, dt);
			}
		}

		if (RUN.method == RUN.RK4 || RUN.method == RUN.EULER) {

			if (uclass.flattened && RUN.method == RUN.RK4) {
				uclass.rk4Advance(this, parent, t, dt);
			} else {
				uclass.eulerAdvance(this, parent, t, dt);
			}
		}

		if (hasRegimes) {
			activeRegime.advance(this, t, dt);
		}
	}
	
	
	
	
	
	
	
	
	

	public void transitionTo(String rnm) throws RuntimeError {
		activeRegime = regimeHM.get(rnm);
		activeRegime.enter();
	}

	public void doneBuild() {
		built = true;
	}

	public void doneInit() {
		initialized = true;
	}

	public void initRegime() throws RuntimeError {
		if (activeRegime == null) {
			activeRegime = regimeHM.get(regimeHM.keySet().iterator().next());
			// TODO just picks random regime
		}
		activeRegime.enter();
	}

	public void setExposedVariables(HashSet<String> vars) {
		expHM = new HashMap<String, DoublePointer>();
		for (String s : vars) {
			expHM.put(s, new DoublePointer(0.));
		}
	}

	public void setVariables(ArrayList<String> vars) {
		varHM = new HashMap<String, DoublePointer>();
		for (String s : vars) {
			varHM.put(s, new DoublePointer(0.));
		}
	}

	public void setIndependents(ArrayList<String> vars) {
		for (String s : vars) {
			varHM.put(s, new DoublePointer(0.));
		}
	}

	public void setExpressionDerived(ArrayList<ExpressionDerivedVariable> exderiveds) {
		for (ExpressionDerivedVariable edv : exderiveds) {
			varHM.put(edv.getVarName(), new DoublePointer(0.));
		}
	}

	public void setFixeds(ArrayList<FixedQuantity> fqs) {
		for (FixedQuantity fq : fqs) {
			varHM.put(fq.getName(), new DoublePointer(fq.getValue()));
		}
	}

	public HashMap<String, DoublePointer> getVarHM() {
		return varHM;
	}

	public HashMap<String, DoublePointer> getExpHM() {
		return expHM;
	}

	public void addInputPort(String s, ActionBlock actionBlock) {
		InPort inp = new InPort(this, s, actionBlock);
		if (firstIn == null) {
			firstIn = inp;
		}
		inPortHM.put(s, inp);
	}

	public void checkAddInputPort(String s) {
		if (inPortHM.containsKey(s)) {
			// fine - there's an action block for it already
		} else {
			// no action block, but we still need the port: presumably for an
			// action within a regime
			addInputPort(s, null);
		}
	}

	public void addOutputPort(String s) {
		OutPort op = new OutPort(s);
		if (firstOut == null) {
			firstOut = op;
		}
		outPortHM.put(s, op);
	}

	public void sendFromPort(String sop) throws RuntimeError {
		outPortHM.get(sop).send();
	}

	public OutPort getOrMakeOutputPort(String s) {
		if (outPortHM.containsKey(s)) {
			// nothing more to do. This is called by regimes to connect to the
			// main state instance ports
		} else {
			addOutputPort(s);
		}
		return outPortHM.get(s);
	}

	public InPort getFirstInPort() throws ConnectionError {
		if (firstIn == null) {
			throw new ConnectionError("No input ports on " + this);
		}
		return firstIn;
	}

	public InPort getInPort(String portId) throws ConnectionError {
		return inPortHM.get(portId);
	}

	public String stateString() {
		return varHM.toString();
	}

	public void exportState(String pfx, double t, LineDisplay ld) {
		for (String s : varHM.keySet()) {
			ld.addPoint(pfx + s, t, varHM.get(s).get());
		}
	}

	public HashMap<String, DoublePointer> getVariables() {
		return varHM;
	}

	public StateWrapper getWrapper(String snm) {
		StateWrapper ret = null;
		if (varHM.containsKey(snm)) {
			ret = new StateWrapper(this, snm);
		}
		return ret;
	}

	public StateInstance getChild(String snm) throws ConnectionError {
		StateInstance ret = null;

		if (snm.startsWith("[") && singleAMI) {
			int idx = Integer.parseInt(snm.substring(1, snm.length() - 1));
			ret = onlyAMI.getInstance(idx);

		} else if (childHM != null && childHM.containsKey(snm)) {
			ret = childHM.get(snm);

		} else {

			// TODO this is rather adhoc for resolving paths - should be
			// external
			if (childA != null) {
				for (StateInstance si : childA) {
					if (snm.equals(si.uclass.getComponentID())) {
						ret = si;
						break;
					}
				}
			}
			if (ret == null && multiA != null) {
				for (MultiInstance mi : multiA) {
					for (StateInstance sr : mi.getStateInstances()) {
						if (sr == null) {
							throw new ConnectionError("null sr in multi instance?");

						} else if (sr.getID() == null) {
							// throw new ConnectionError("null id in sr " + sr);
							// not a problem - can have children without ids if
							// we don't ever need to access them
						} else if (sr.getID().equals(snm)) {
							ret = sr;
							break;
						}
					}
				}
			}
		}

		if (ret == null) {
			String err = "No such child element or variable " + snm + " in " + this + "\nThe children (hasChildren = "+hasChildren+") are, "
                + "childHM: " + childHM + "\nchildA: " + childA;
			throw new ConnectionError(err);
		}
		return ret;
	}

	public double getVariable(String varname) throws RuntimeError {
		// System.out.println("varHM ("+varHM+"), expHM ("+expHM+") for state: "+this.toString()+", varname: "+varname);
		double ret = Double.NaN;

		// now only exposing variables that are in expHM, not stuff in varHM.
		if (expHM != null && expHM.containsKey(varname)) {
			ret = expHM.get(varname).get();
		 
		} else {
 			if (varHM.containsKey(varname)) {
			ret = varHM.get(varname).get();
			
		} else {
			if (parent != null) {
				ret = parent.getVariable(varname);
			}
		}
		}

		if (Double.isNaN(ret)) {
			StringBuilder err = new StringBuilder("Problem getting exposed var " + varname + " in: " + this + "\n" + 
					"Exposed: " + expHM + "\n" + "Vars: " + varHM + "\n");
			if (childA != null) {
				for (StateInstance si : childA) {
					err.append("Child: " + si + ", vars: " + si.varHM + "\n");
				}
			} else {
				err.append("childA is null\n");
			}
			if (childHM != null) {
				for (String k : childHM.keySet()) {
					StateInstance si = childHM.get(k);
					err.append("Child " + k + ": " + si + ", vars: " + si.varHM + "\n");
				}
			} else {
				err.append("childHM is null\n");
			}
			throw new RuntimeError(err.toString());
		}
		return ret;
	}

	
	public void addChild(String s, StateInstance newInstance) {
		if (newInstance == null) {
			E.warning("adding a null child instance to " + this);
		} else {
			newInstance.setParent(this);

			if (!hasChildren) {
				hasChildren = true;
				childA = new ArrayList<StateInstance>();
				childHM = new HashMap<String, StateInstance>();
			}
			childA.add(newInstance);
			childHM.put(s, newInstance);
		}
	}

	public void addMultiInstance(MultiInstance mi) {
		if (!hasMulti) {
			hasMulti = true;
			multiA = new ArrayList<MultiInstance>();
			multiHM = new HashMap<String, MultiInstance>();
		}
		multiA.add(mi);
		multiHM.put(mi.getKnownAs(), mi);
		mi.setParent(this);

		if (onlyAMI == null) {
			onlyAMI = mi;
			singleAMI = true;
		}
	}

	public StateInstance getChildInstance(String snm) throws ContentError {
		// errors because we used to turn ComponentRefs into children, but we
		// don't always need that
		// now they go in refHM and don't automaticlly get instances added as
		// children
		// which to do????
		StateInstance ret = null;
		
		
		if (childHM != null && childHM.containsKey(snm)) {
			ret = childHM.get(snm);
		
		} else {
			throw new ContentError("seeking child instance " + snm + " in " + this + " but there are no children");
		}
		return ret;
	}

	public MultiInstance getMultiInstance(String snm) {
		if (multiHM == null) {
			E.info("No MultiInstances: " + snm + " in " + this);
                        return null;
		}
		MultiInstance mi = multiHM.get(snm);
		if (mi == null) {
			// E.error("Failed to get MultiInstance: " + snm + "\n" +
			// "My MultiInstances:\n" + multiHM);
		}
		return mi;

	}

	public void addPathStateInstance(String pth, StateInstance pl) {
		if (pathSIHM == null) {
			pathSIHM = new HashMap<String, StateInstance>();
		}
		pathSIHM.put(pth, pl);
	}

	public StateInstance getPathStateInstance(String pth) throws ContentError {
		if (!resolvedPaths) {
			uclass.applyPathDerived(this);
		}
		return pathSIHM.get(pth);
	}

	public StateInstance getScopeInstance(String id) {
		StateInstance ret = null;

		if (idSIHM == null) {
			makeIDSIHM();
		}

		if (idSIHM.containsKey(id)) {
			ret = idSIHM.get(id);

		} else if (parent != null) {
			ret = parent.getScopeInstance(id);
		}
		return ret;
	}

	void makeIDSIHM() {
		idSIHM = new HashMap<String, StateInstance>();
		if (childA != null) {
			for (StateInstance si : childA) {
				if (si.getID() != null) {
					idSIHM.put(si.getID(), si);
					// E.info("added child " + si.getID());
				}
			}
		}
		if (multiA != null) {
			for (MultiInstance mi : multiA) {
				for (StateInstance si : mi.instances) {
					if (si.getID() != null) {
						idSIHM.put(si.getID(), si);
						// E.info("added child " + si.getID());
					}
				}
			}
		}  
		
	}
		
		
		public String getPathStringValue(String path, double fac, double off) throws ContentError, RuntimeError {
			String ret = "";

	        StateInstance wkinst = this;
	        String[] bits = path.split("/");
	        for (int i = 0; i < bits.length - 1; i++) {
	            wkinst = wkinst.getChildInstance(bits[i]);
	        }
	        String lbit= bits[bits.length - 1];
	        
	        if (wkinst != null) {
	        	if (lbit.equals("name")) {
	        		ret = uclass.getComponentID();
	        		
	        	} else if (lbit.equals("id")) {
	        		ret = uclass.getComponentID();
	        		
	        	} else {
	        		ret= "" + Out.formatDouble(fac * wkinst.getVariable(lbit) - off);
	        	}
	        } else {
	        	ret = "(ERR:" + lbit + ")";
	        }
	        return ret;
	    }
	
	
	
	

	public void addPathStateArray(String pth, ArrayList<StateInstance> pla) throws ContentError {
		// E.info("adding psa " + pth + " " + pla);

		if (pathAHM == null) {
			pathAHM = new HashMap<String, ArrayList<StateInstance>>();
		}

		pathAHM.put(pth, pla);
		DestinationMap dm = new DestinationMap(pth, pla);
		if (dmaps == null) {
			dmaps = new ArrayList<DestinationMap>();
		}
		dmaps.add(dm);
	}

	public ArrayList<StateInstance> getPathStateArray(String pth) throws ContentError {
		if (!resolvedPaths) {
			// E.info("resolving psas in getPSA");
			uclass.applyPathDerived(this);
		}

		return pathAHM.get(pth);
	}

	public void donePaths() {
		resolvedPaths = true;
	}

	public void addAttachmentSet(String s, MultiInstance inas) {
		inas.setKnownAs(s);
		addMultiInstance(inas);

	}

	public void addAttachment(String s, StateInstance inst) throws ConnectionError, ContentError {
		String snm = s;
		MultiInstance mi = null;
		if (snm == null) {
			if (singleAMI) {
				mi = onlyAMI;
				snm = mi.getKnownAs(); // what s would have been if we'd known
				// it at the time
			} else {
				if (onlyAMI == null) {
					throw new ConnectionError("No attachments list in " + this);
				} else {
					throw new ConnectionError("Must specify destination attachments list since the target has multiple sets");
				}
			}

		} else {
			mi = multiHM.get(s);
		}
		mi.add(inst);
		inst.setParent(this);

		if (built) {
			// the child is added too late to come on with the default
			// checkBuilt
			inst.checkBuilt();

		}

		// this is fine for the advance() step, but paths that use it have
		// already been
		// populated so we need to see if this state should go in any of the
		// path state arrays

		if (dmaps != null && snm != null) {
			for (DestinationMap dm : dmaps) {
				dm.checkInsert(snm, inst);
			}
		}

	}

	public void addKSchemeInst(KSchemeInst ksi) {
		hasSchemes = true;
		if (schemeA == null) {
			schemeA = new ArrayList<KSchemeInst>();
			schemeHM = new HashMap<String, KSchemeInst>();
		}
		schemeA.add(ksi);
		schemeHM.put(ksi.getName(), ksi);

	}

	public void setVariable(String vnm, double pval) {
		varHM.get(vnm).set(pval);
	}

	// TODO this sets component level variables - should probably keep them
	// separate
	public void setNewVariable(String vnm, double pval) {
		varHM.put(vnm, new DoublePointer(pval));
	}

	public ComponentBehavior getComponentBehavior() {
		return uclass;
	}
    
	public OutPort getFirstOutPort() {
		return firstOut;
	}

	public OutPort getOutPort(String sop) {
		return outPortHM.get(sop);
	}

	public void addRegime(RegimeStateInstance rsi) {
		hasRegimes = true;
		if (regimeHM == null) {
			regimeHM = new HashMap<String, RegimeStateInstance>();
		}
		regimeHM.put(rsi.getID(), rsi);
		// E.info("state instance added regime " + rsi.getID());
		if (rsi.isInitial()) {
			activeRegime = rsi;
		}

	}

	public void receiveRegimeEvent(String name) throws RuntimeError {
		activeRegime.receiveEvent(name);
	}

	public boolean hasVariable(String s) {
		return varHM.containsKey(s);
	}

	public DoublePointer getVariablePtr(String s) {
		return varHM.get(s);
	}

	public ArrayList<StateInstance> getStateInstances(String path) throws ConnectionError, ContentError {
		//E.info("Getting instances: " + path + " relative to " + this);
		ArrayList<StateInstance> ret = quietGetStateInstances(path);
		if (ret == null) {
			throw new ConnectionError("No such set of instances " + path + " relative to " + this + " or its ancestors");
		}
		return ret;
	}

	public ArrayList<StateInstance> quietGetStateInstances(String path) throws ConnectionError, ContentError {
		ArrayList<StateInstance> ret = null;
		if (hasChildren && childHM.containsKey(path)) {
			E.info("QUERY - using path twice?");
			ret = childHM.get(path).quietGetStateInstances(path);

		} else if (hasMulti && multiHM.containsKey(path)) {
			ret = multiHM.get(path).getStateInstances();
		} else {
			if (multiA != null) {
				for (MultiInstance mi : multiA) {
					if (mi.hasID(path)) {
						ret = mi.getChildByID(path).getStateInstances();
					}
				}
			}
		}

		if (ret == null && parent != null) {
			ret = parent.quietGetStateInstances(path);
		}
		return ret;
	}

	public ArrayList<StateInstance> getStateInstances() throws ConnectionError, ContentError {
		checkBuilt();
		ArrayList<StateInstance> ret = null;
		if (singleAMI) {
			ret = onlyAMI.getStateInstances();

		} else if (singleIS) {
			ret = onlyIS.getItems();

		} else {
			throw new ConnectionError("cant get anon instances on " + this);
		}
		return ret;
	}

	public void checkBuilt() throws ConnectionError, ContentError {
 		if (!built) {
 			uclass.build(this);
		}

		if (childA != null) {
			for (StateInstance csi : childA) {
				csi.checkBuilt();
			}
		}

		if (multiA != null) {
 			for (MultiInstance mi : multiA) {
 				for (StateInstance si : mi.getStateInstances()) {
					si.checkBuilt();
				}
			}

		}
	}

	public int getMultiInstanceCount() {
		int ret = 0;
		if (instanceSetHM != null) {
			ret = instanceSetHM.size();
		}
		return ret;
	}
	
	public boolean hasSingleMI() {
		return singleAMI;
	}

	public MultiInstance getSingleMI() {
		return onlyAMI;
	}

	public void addInstanceSet(String s) {
		InstanceSet<StateInstance> newIS = new InstanceSet<StateInstance>(s, this);
		addInstanceSet(newIS);
	}

	public void addInstanceSet(InstanceSet<StateInstance> newIS) {
		if (instanceSetHM == null) {
			instanceSetHM = new HashMap<String, InstanceSet<StateInstance>>();
		}
		instanceSetHM.put(newIS.getName(), newIS);
		if (onlyIS == null) {
			onlyIS = newIS;
			singleIS = true;
		} else {
			singleIS = false;
		}
	}

	public InstanceSet<StateInstance> getInstanceSet(String col) {
		return instanceSetHM.get(col);
	}

	public void addInstancePairSet(String s) {
		InstancePairSet<StateInstance> newIS = new InstancePairSet<StateInstance>(s, this);
		addInstancePairSet(newIS);
	}

	public void addInstancePairSet(InstancePairSet<StateInstance> newIS) {
		if (instancePairSetHM == null) {
			instancePairSetHM = new HashMap<String, InstancePairSet<StateInstance>>();
		}
		instancePairSetHM.put(newIS.getName(), newIS);
	}

	public InstancePairSet<StateInstance> getInstancePairSet(String col) {
		return instancePairSetHM.get(col);
	}

	public void coCopy(StateInstance psi) {
		for (String s : psi.varHM.keySet()) {
			varHM.put(s, new DoublePointer(psi.varHM.get(s).get()));
		}

	}

	public InstanceSet<StateInstance> getUniqueInstanceSet() throws ContentError {
		InstanceSet<StateInstance> ret = null;
		if (singleIS) {
			ret = onlyIS;
		} else if (singleAMI) {
			InstanceSet<StateInstance> is = onlyAMI.getInstanceSet(this);
			addInstanceSet(is);
			ret = is;
		}
		if (ret == null) {
			throw new ContentError("Can't get single instance set from " + this);
		}
		return ret;
	}

	// used by path expressions to match a single section of the path.
	// Expressions and this method should supplant
	// all the other matching methods here
	public ArrayList<StateInstance> getPathInstances(String sel) throws ContentError, ConnectionError {
		ArrayList<StateInstance> ret = null;
		if (instanceSetHM != null && instanceSetHM.containsKey(sel)) {
			ret = instanceSetHM.get(sel).getItems();

		} else if (hasChildren && childHM.containsKey(sel)) {
			ret = new ArrayList<StateInstance>();
			ret.add(childHM.get(sel));
			// POSERR 7 mar 2011
			// ret = childHM.get(sel).getStateInstances();

		} else if (hasMulti && multiHM.containsKey(sel)) {
			ret = multiHM.get(sel).getStateInstances();

		} else if (multiA != null) {
			for (MultiInstance mi : multiA) {
				if (mi.hasID(sel)) {
					ret = mi.getChildByID(sel).getStateInstances();
					break;
				}
			}
		}
		if (ret == null && parent != null) {
			ret = parent.getPathInstances(sel);
		}

		if (ret == null) {
			throw new ContentError("cant find '" + sel + "' in " + this);
		}
		return ret;
	}

	public double getFloatProperty(String sel) throws ContentError {

		// TODO this isn't quite right: varHM contains the fixed params (from
		// uclass) as well as
		// private local variables. Here we just want the uclass ones, and any
		// instance properties

		double ret = quietGetFloatProperty(sel);
		if (Double.isNaN(ret)) {
			ret = parent.quietGetFloatProperty(sel);
		}

		if (Double.isNaN(ret)) {
			throw new ContentError("no such property " + sel + " in " + this);
		}
		return ret;
	}

	public double quietGetFloatProperty(String sel) throws ContentError {
		double ret = Double.NaN;
		if (varHM != null && varHM.containsKey(sel)) {
			ret = varHM.get(sel).get();
		}
		return ret;
	}

	public void startArray(String snm) {
		InstanceSet<StateInstance> iset = new InstanceSet<StateInstance>(snm, this);
		addInstanceSet(iset);
	}

	public void addToArray(String snm, StateInstance pc) {
		getInstanceSet(snm).add(pc);

	}

	public void setWork(String string, Object wk) {
		work = wk;
	}

	public Object getWork() {
		return work;
	}

	public String getTypeParam(String satt) throws ContentError {
		return uclass.getPropertyStringValue(satt);
	}

	public boolean hasTypeParam(String satt) {
		return uclass.hasPropertyString(satt);
	}

	public HashMap<String, MultiInstance> getMultiHM() {
		return multiHM;
	}
}
