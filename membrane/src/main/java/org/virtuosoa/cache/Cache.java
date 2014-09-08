package org.virtuosoa.cache;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.virtuosoa.models.Route;
import org.virtuosoa.proxy.Main;

import com.google.common.collect.MapMaker;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.topic.TopicService;

public class Cache {
	private static final Logger log = Logger.getAnonymousLogger();

	static final long DEFAULT_EXPIRATION = Long.parseLong(System.getProperty("defaultExpiration", "300")); // 5 minutes
    static final int HC_PORT = Integer.parseInt(System.getProperty("hcPort", "5701"));
    static final String HC_MASTER = System.getProperty("hcMaster", "127.0.0.1:5701");

    public static long SECONDS = 1000;
    public static long MINUTES = SECONDS * 60;
    
//    private static Map<String, Expiring> map = new MapMaker().concurrencyLevel(10).makeMap();
    
    private static Map<String, Expiring> map = new ConcurrentHashMap<String, Expiring>();
    private static Map<String, Route> routesCache = null;
    
    public static Map<String, Route> getRoutes() {
    	return routesCache;
    }
    	
	public static Serializable getRoute(String key) {
		return routesCache.get(key);
	}	
    public static void setRoute(String key, Route value) {
    	routesCache.put(key, value);
    	routesChanged.set(true);
    }
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
	
    static BackgroundCacheCleanup ste = null;
    
    public static void stats() {
    	log.info(" ***** map size: " + map.size());
    }
    
    private static HazelcastInstance instance = null;
    
	public static void init() throws ExecutionException, InterruptedException {
	
        Config cfg = new Config();
        
        NetworkConfig network = cfg.getNetworkConfig();
        network.setPort(HC_PORT);
        network.setPublicAddress("127.0.0.1:" + HC_PORT);
        JoinConfig join = network.getJoin();
        join.getTcpIpConfig().setEnabled(true);
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().addMember(HC_MASTER);

        instance = Hazelcast.newHazelcastInstance(cfg);
        routesCache = instance.getMap("virtuoSOA-routes");
 
        log.info(" ***** Cache contains: "+ map.size() + " items");

        ITopic<Command> topic = instance.getTopic("commands");
        topic.addMessageListener(listener);

    	ste = new BackgroundCacheCleanup();
	    ste.startScheduleTask();
	}
	
	
	private static CommandListener listener = new CommandListener();
	
    public static void broadcast(Command command) throws ExecutionException, InterruptedException {
        ITopic<Command> topic = instance.getTopic("commands");
        topic.publish(command);
		log.info(" xxxxxxxxxxxxxxxxxxx published " + command.text);
    }

    static class CommandListener implements MessageListener<Command> {
		@Override
		public void onMessage(Message<Command> message) {
			log.info(" xxxxxxxxxxxxxxxxxxx received " + message.getMessageObject().text);
			switch (message.getMessageObject().code) {
				case RELOAD_ROUTES:
					try {
						Main.addAllRoutes();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case PING: break;
			}
		}
    }
	
    static class Command implements Serializable {
		private static final long serialVersionUID = -5612641101914170529L;
		public String text;
		public codes code;
		
		public static enum codes {
			RELOAD_ROUTES,
			PING
		}
		
		public Command() {
			
		}
		
		public Command(codes code, String text) {
			this.code = code;
			this.text = text;
		}
		
		public static Command reloadRoutes() {
			return new Command(Command.codes.RELOAD_ROUTES, "reload routes");
		}
		
		public static Command ping() {
			return new Command(Command.codes.PING, "piiiing!!!");
		}
		
    }
	
    public static AtomicBoolean routesChanged = new AtomicBoolean(false);
	
	public static void cleanUp() throws ExecutionException, InterruptedException {
		if (routesChanged.get()) {
			broadcast(Command.reloadRoutes());
		}
    	for (Entry<String, Expiring> entry: map.entrySet()) {
    		if (entry.getValue().expired()){
        		log.finest("removing " + entry.getKey());
    			map.remove(entry.getKey());
        		log.finest("removed " + entry.getKey());
    		}
    	}
	}
}