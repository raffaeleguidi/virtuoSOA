package org.virtuosoa.proxy;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.virtuosoa.cache.Cache;
import org.virtuosoa.cluster.Cluster;
import org.virtuosoa.interceptors.CachingInterceptor;
import org.virtuosoa.interceptors.BaseVirtuosoInterceptor;
import org.virtuosoa.models.Route;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

import org.apache.log4j.Logger;
 
public class Main {
	
	static final Logger log = Logger.getLogger(Main.class.getCanonicalName());

	static final int PORT = Integer.parseInt(System.getProperty("port", "4000"));
	static HttpRouter router = null; // = new HttpRouter();
    
    public static ServiceProxy addRoute(String source) throws IOException {
    	Route route = Route.lookup(source);
    	return addRoute(route);
     }
    
    public static ServiceProxy addRoute(Route route) throws IOException {
       	ServiceProxyKey key = new ServiceProxyKey(route.source, route.method, route.path, PORT); // <- should be one for GET (with a cache interceptor) and one for other methods 
    	ServiceProxy sp = new ServiceProxy(key, route.destination, route.destinationPort);
    	sp.getInterceptors().add(new BaseVirtuosoInterceptor(route));
    	if (route.cache > 0) {
    		sp.getInterceptors().add(new CachingInterceptor(route));
    	}
		router.add(sp);
		log.info("added route on " + route.source + " for method " + route.method);
		return sp;
    }
    
    public static void loadRoutesFromJson() throws IOException {
    	if (!new File("routes.json").exists()) {
    		log.info("routes.json not found - skipping (you should have specified a master instance to connect to)");
    		return;
    	}
    	Gson gson = new Gson();
		JsonReader jsonReader = new JsonReader(new FileReader("routes.json"));
		Route[] routes = gson.fromJson(jsonReader, Route[].class);
		for (int i = 0; i < routes.length; i++) {
			Route route = routes[i];
			route.save();
			log.info("loaded route: " + route.source + " for method " + route.method);
		}
		log.info("loaded " + routes.length + " routes from file");
    }
    
    public static void addAllRoutes() throws Exception {
    	if (router != null) router.stop();
    	router = new HttpRouter();
    	
    	for (Entry<String, Route> entry : Cluster.getRoutes().entrySet()) {
    		addRoute(entry.getValue());
		}
		router.getTransport().setPrintStackTrace(true);
		router.init();	
		Cluster.routesChanged.set(false);
    }
    
	// to be removed
    public static void saveRoutes() throws IOException {
    	BufferedWriter bw = new BufferedWriter(new FileWriter(new File("routes.json")));
    	Gson gson = new Gson();
    	
    	Route[] routes = new Route[]{
    		new Route("monitor.virtuoso", "10.232.132.100", "GET", ".*", 3000, 1000, 5 * Cache.MINUTES)
    	};
    	bw.write(gson.toJson(routes));
    	bw.close();
    }
    
    static HouseKeeper ste = null;
    public static final MetricRegistry metrics = new MetricRegistry();
    
	public static void init() throws Exception {
		Cache.init();
		Cluster.init();
    	ste = new HouseKeeper();
	    ste.startScheduleTask();
		loadRoutesFromJson();
		addAllRoutes();
		
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
	       .convertRatesTo(TimeUnit.SECONDS)
	       .convertDurationsTo(TimeUnit.MILLISECONDS)
	       .build();
		
		reporter.start(10, TimeUnit.SECONDS);
	}

	public static void main(String[] args) throws Exception {
		init();
		log.info("membrane-proxy started");
	}
}
