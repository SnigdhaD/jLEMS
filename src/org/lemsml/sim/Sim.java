package org.lemsml.sim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.lemsml.display.ComponentBehaviorWriter;
import org.lemsml.display.DataViewer;
import org.lemsml.display.DataViewerFactory;
import org.lemsml.expression.ParseError;
import org.lemsml.run.ComponentBehavior;
import org.lemsml.run.ConnectionError;
import org.lemsml.run.EventManager;
import org.lemsml.run.RunConfig;
import org.lemsml.run.RunDisplay;
import org.lemsml.run.RuntimeOutput;
import org.lemsml.run.RuntimeRecorder;
import org.lemsml.run.StateInstance;
import org.lemsml.type.Component;
import org.lemsml.type.Lems;
import org.lemsml.type.Target;
import org.lemsml.util.ContentError;
import org.lemsml.util.E;
import org.lemsml.util.FileUtil;
import org.lemsml.util.RuntimeError;


public class Sim extends LemsProcess {

   
    ComponentBehavior rootBehavior;
    ComponentBehavior targetBehavior;
    
     
    HashMap<String, DataViewer> dvHM;
    
    ArrayList<RuntimeRecorder> runtimeRecorders;
    
    ArrayList<RunConfig> runConfigs;
    
    EventManager eventManager;
    
    
    public Sim(Class<?> c, String fnm) {
    	super(c, fnm);
    }

    public Sim(File file) {
       super(file);
     
    }

    public Sim(String srcStr) {
    	super(srcStr);
    }

    public Sim(Lems lems) {
       super(lems);
    }

    
    	
    	
    public void build() throws ContentError, ConnectionError, ParseError {
    	
	    Target dr = lems.getTarget();
	
	    Component simCpt = dr.getComponent();
	
	    if (simCpt == null) {
	        E.error("No such component: " + dr.component + " as referred to by default simulation.");
	        E.error(lems.textSummary());
	        throw new ContentError("No such component " + dr.component);
	    }
	
	    
	    E.info("Simulation component: " + simCpt);
	
	    rootBehavior = simCpt.getComponentBehavior();
	    
	   
	    // collect everything in the ComponentBehavior tree that makes a display
	    ArrayList<RuntimeOutput> runtimeOutputs = new ArrayList<RuntimeOutput>();
	    OutputCollector oc = new OutputCollector(runtimeOutputs);
	    rootBehavior.visitAll(oc);
	   
	    // build the displays and keep them in dvHM
	    dvHM = new HashMap<String, DataViewer>();
	    for (RuntimeOutput ro : runtimeOutputs) {
	    	dvHM.put(ro.getID(), DataViewerFactory.getFactory().newDataViewer(ro.getTitle()));
	    }
	     
	    runtimeRecorders = new ArrayList<RuntimeRecorder>();
	    RecorderCollector rc = new RecorderCollector(runtimeRecorders);
	    rootBehavior.visitAll(rc);
	    
	    
	    
	    runConfigs = new ArrayList<RunConfig>();
	    RunConfigCollector rcc = new RunConfigCollector(runConfigs);
	    rootBehavior.visitAll(rcc);
	    
	    
	    
	    eventManager = new EventManager();
	     
	}


   
    public void run() throws ConnectionError, ContentError, RuntimeError, IOException, ParseError {
    	
    	for (RunConfig rc : runConfigs) {
    		run(rc);
    	}
        E.info("Done");
    }

  
    
    
    public void run(RunConfig rc) throws ConnectionError, ContentError, RuntimeError, IOException, ParseError {

    	Component runCpt = rc.getTarget();
  	    	
  		targetBehavior = runCpt.getComponentBehavior();
  	 
  		
  	    StateInstance rootState = lems.build(targetBehavior, eventManager);
  	
  	    RunnableAccessor ra = new RunnableAccessor(rootState);
  	    
  	    
  	    for (RuntimeRecorder rr : runtimeRecorders) {
  	    	rr.connectRunnable(ra, dvHM.get(rr.getDisplay()));
  	    }
  	    

        double dt = rc.getTimestep();
        int nstep = (int) Math.round(rc.getRuntime() / dt);

        E.info("Running for " + nstep + " steps");

      
        StringBuilder info = new StringBuilder("#Report of running simulation with LEMS Interpreter\n");
        StringBuilder times = new StringBuilder();
 
        long start = System.currentTimeMillis();
  
        double t = 0;
        
       
        rootState.initialize(null);  
        EventManager eventManager = rootState.getEventManager();
        
        for (int istep = 0; istep < nstep; istep++) {
        	if (istep > 0) {
        		eventManager.advance(t);
                rootState.advance(null, t, dt);
        	}
        	
        	for (RuntimeRecorder rr : runtimeRecorders) {
        		rr.appendState(t);
        	}
           
            times.append((float) (t * 1000)).append("\n");
            t += dt;
        }
        E.info("Finished " + nstep + " steps");

        
        long end = System.currentTimeMillis();
        info.append("RealSimulationTime=" + ((end - start) / 1000.0) + "\n");
       }

    
	public void printCB() {
		ComponentBehaviorWriter cbw = new ComponentBehaviorWriter();
		cbw.print(targetBehavior);
		
	}
}
