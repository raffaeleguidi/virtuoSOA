package org.virtuosoa.cluster;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.virtuosoa.models.Route;
import org.virtuosoa.proxy.Main;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import org.apache.log4j.Logger;
 
public class Cluster {
	private static final Logger log = Logger.getLogger(Cluster.class.getCanonicalName());

    private static HazelcastInstance instance = null;
	
    public static AtomicBoolean routesChanged = new AtomicBoolean(false);

    static final int HC_PORT = Integer.parseInt(System.getProperty("hcPort", "5701"));
    static final String HC_MASTER = System.getProperty("hcMaster", "127.0.0.1:5701");

    private static Map<String, Route> routesCache = null;
    
    
	public static void init() throws ExecutionException, InterruptedException {
	
        Config cfg = new Config();
        cfg.setProperty("hazelcast.logging.type", "log4j");
        
        NetworkConfig network = cfg.getNetworkConfig();
        network.setPort(HC_PORT);
        network.setPublicAddress("127.0.0.1:" + HC_PORT);
        JoinConfig join = network.getJoin();
        join.getTcpIpConfig().setEnabled(true);
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().addMember(HC_MASTER);

        instance = Hazelcast.newHazelcastInstance(cfg);
        routesCache = instance.getMap("virtuoSOA-routes");
 
        ITopic<Command> topic = instance.getTopic("commands");
        topic.addMessageListener(listener);
	}
	
	public static void cleanUp() throws ExecutionException, InterruptedException {
		if (routesChanged.get()) {
			broadcast(Command.reloadRoutes());
		}
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

    public static Map<String, Route> getRoutes() {
    	return routesCache;
    }
    
	public static Serializable getRoute(String key) {
		return routesCache.get(key);
	}	
    public static void setRoute(String key, Route value) {
    	routesCache.remove(key);
    	routesCache.put(key, value);
    	routesChanged.set(true);
    }    
}