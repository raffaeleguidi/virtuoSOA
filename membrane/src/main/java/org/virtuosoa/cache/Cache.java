package org.virtuosoa.cache;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.virtuosoa.proxy.HouseKeeper;

public class Cache {
	private static final Logger log = Logger.getLogger(Cache.class.getSimpleName());

	static final long DEFAULT_EXPIRATION = Long.parseLong(System.getProperty("defaultExpiration", "300")); // 5 minutes

    public static long SECONDS = 1000;
    public static long MINUTES = SECONDS * 60;
    
    private static Map<String, Expiring> map = new ConcurrentHashMap<String, Expiring>();

    public static Object get(String key) {
		Expiring expiring = map.get(key);
		if (expiring != null && !expiring.expired()) {
			return expiring.payload;
		} else {
			return null;
		}
	}	
    public static void set(String key, Object value, long expiresIn ) {
    	map.put(key, Expiring.in(value, expiresIn));
    }
    public static void set(String key, Object value) {
    	set(key, value, DEFAULT_EXPIRATION);
    }
    public static String getAsString(String key) {
    	return (String) get(key);
    }
    public static byte[] getAsByteArray(String key) {
    	return (byte[]) get(key);
    }
    public static Integer getAsInt(String key) {
    	return (Integer) get(key);
    }
	public static int size() {
		return map.size();
	}
	
    static HouseKeeper ste = null;
    
    public static void stats() {
    	log.info(" ***** map size: " + map.size());
    }
    
	public static void init() throws ExecutionException, InterruptedException {
    	ste = new HouseKeeper();
	    ste.startScheduleTask();
	}
	
	public static void cleanUp() throws ExecutionException, InterruptedException {
    	for (Entry<String, Expiring> entry: map.entrySet()) {
    		if (entry.getValue().expired()){
        		log.finest("removing " + entry.getKey());
    			map.remove(entry.getKey());
        		log.finest("removed " + entry.getKey());
    		}
    	}
	}
}