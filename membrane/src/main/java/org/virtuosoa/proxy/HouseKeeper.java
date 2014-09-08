package org.virtuosoa.proxy;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.virtuosoa.cache.Cache;
import org.virtuosoa.cluster.Cluster;

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
                    cleanupCache();
                }catch(Exception ex) {
                    ex.printStackTrace(); //or loggger would be better
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private void cleanupCache() throws Exception {
        log.info("starting cache cleanup");
    	Cache.cleanUp();
    	Cache.stats();
    	if (Cluster.routesChanged.get()) 
    		Main.addAllRoutes();
        log.info("ended cache cleanup");
    }
}