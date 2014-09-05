package org.virtuosoa.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.virtuosoa.utils.BackgroundCacheCleanup;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Cache {
    private static Map<String, Expiring> map = new HashMap<String, Expiring>();
    private static Map<String, Serializable> globalCache = null;
    public static long SECONDS = 1000;
    public static long MINUTES = SECONDS * 60;
    
    public static long defaultExpiresIn = 10 * SECONDS; // 10 seconds
	
	public static Serializable getGlobal(String key) {
		return globalCache.get(key);
	}	
    public static void setGlobal(String key, Serializable value) {
    	globalCache.put(key, value);
    }
	public static Object get(String key) {
		Expiring expiring = map.get(key);
		if (expiring != null && expiring.expiresAt >= System.currentTimeMillis()) {
			return expiring.payload;
		} else {
			return null;
		}
	}	
    public static void set(String key, Object value, long expiresIn ) {
    	map.put(key, Expiring.in(value, expiresIn));
    }
    public static void set(String key, Object value) {
    	set(key, value, defaultExpiresIn);
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
	
    static final int HC_PORT = Integer.parseInt(System.getProperty("hcPort", "5701"));
    static final String HC_MASTER = System.getProperty("hcMaster", "127.0.0.1:5701");
   	
    static BackgroundCacheCleanup ste = null;
    
    public static void stats() {
    	System.out.println(" ***** map size: " + map.size());
    	
    	for (Entry entry: map.entrySet()) {
    		System.out.println(entry.getKey() + "=" + entry.getValue());
    	}
    }
    
	public static void init() {
	
        Config cfg = new Config();
        
        NetworkConfig network = cfg.getNetworkConfig();
        network.setPort(HC_PORT);
        network.setPublicAddress("127.0.0.1:" + HC_PORT);
        JoinConfig join = network.getJoin();
        join.getTcpIpConfig().setEnabled(true);
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().addMember(HC_MASTER);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        globalCache = instance.getMap("virtuoSOA-routes");
 
        System.out.println(" ***** Cache contains: "+ map.size() + " items");

    	ste = new BackgroundCacheCleanup();
	    ste.startScheduleTask();
	}
}