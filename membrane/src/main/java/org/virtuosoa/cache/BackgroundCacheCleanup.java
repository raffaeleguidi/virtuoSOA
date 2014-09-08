package org.virtuosoa.cache;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BackgroundCacheCleanup {
	private static final Logger log = Logger.getAnonymousLogger();

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
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void cleanupCache() throws ExecutionException, InterruptedException {
        log.info("starting cache cleanup");
    	Cache.cleanUp();
    	Cache.stats();
        log.info("ended cache cleanup");
    }
}