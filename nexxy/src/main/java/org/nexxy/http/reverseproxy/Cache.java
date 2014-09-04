package org.nexxy.http.reverseproxy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.ning.http.client.AsyncHttpClient;

public class Cache {
	private static AsyncHttpClient client = null;
    private static Map<String, Object> map = new HashMap<String, Object>();
    private static Map<String, Serializable> globalCache = null;
	
	public static Serializable getGlobal(String key) {
		return globalCache.get(key);
	}	
    public static void setGlobal(String key, Serializable value) {
    	globalCache.put(key, value);
    }
	public static Object get(String key) {
		return map.get(key);
	}	
    public static void set(String key, Object value) {
    	map.put(key, value);
    }
    public static String getAsString(String key) {
    	return (String) map.get(key);
    }
    public static byte[] getAsByteArray(String key) {
    	return (byte[]) map.get(key);
    }
    public static Integer getAsInt(String key) {
    	return (Integer) map.get(key);
    }
	public static int size() {
		return map.size();
	}
	
    static final int HC_PORT = Integer.parseInt(System.getProperty("hcPort", "5701"));
    static final String HC_MASTER = System.getProperty("hcMaster", "127.0.0.1:5701");

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
        globalCache = instance.getMap("nexxy-routes");
 
        System.out.println(" ***** Cache contains: "+ map.size() + " items");

		new Route("monitor.virtuoso", "10.232.132.100:3000", 1000, 300).save();
		new Route("test.virtuoso", "10.232.132.100:3000", 1000, 0).save();
	}

	public static AsyncHttpClient getClient() {
    	if (client == null) { 
		    client = new AsyncHttpClient();
		}
    	return client;
    }
}
