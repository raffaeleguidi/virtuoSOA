package org.virtuosoa.proxy;


import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.virtuosoa.cache.Cache;
import org.virtuosoa.cluster.Cluster;
import org.virtuosoa.models.Route;
import org.apache.log4j.Logger;
 
public class HouseKeeper {
	private static final Logger log = Logger.getLogger(HouseKeeper.class.getCanonicalName());

	private final ScheduledExecutorService scheduler = Executors
        .newScheduledThreadPool(1);

    public void startScheduleTask() {
    /**
    * not using the taskHandle returned here, but it can be used to cancel
    * the task, or check if it's done (for recurring tasks, that's not
    * going to be very useful)
    */
    @SuppressWarnings("unused")
	final ScheduledFuture<?> taskHandle = scheduler.scheduleAtFixedRate(
        new Runnable() {
            public void run() {
                try {
                    doStuff();
                }catch(Exception ex) {
                    ex.printStackTrace(); //or loggger would be better
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void cleanupCache() throws Exception {
    	Cache.cleanUp();
    	Cache.stats();
    	if (Cluster.routesChanged.get()) 
    		Main.addAllRoutes();
    }
    
    private void doHealthCheck() throws Exception {
    	for (Entry<String, Route> entry : Cluster.getRoutes().entrySet()) {
        	log.debug("healthcheck on " + entry.getValue().key() + " started");
			HealthCheck.run(entry.getValue());
		} 
    }
    
    
    private void doStuff() throws Exception {
       	log.info("housekeeping thread started");
       	cleanupCache();
    	doHealthCheck();
    	log.info("housekeeping thread ended");
    }
}