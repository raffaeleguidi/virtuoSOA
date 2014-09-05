package org.virtuosoa.proxy;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import org.virtuosoa.cache.Cache;
import org.virtuosoa.interceptors.CachingInterceptor;
import org.virtuosoa.models.Route;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class Main {
	
	private static final Logger log = Logger.getAnonymousLogger();

	static final int PORT = Integer.parseInt(System.getProperty("port", "4000"));
	static HttpRouter router = new HttpRouter();
    
    public static ServiceProxy addRoute(String source) throws IOException {
    	Route route = Route.find(source);
    	return addRoute(route);
     }
    
    public static ServiceProxy addRoute(Route route) throws IOException {
       	ServiceProxyKey key = new ServiceProxyKey(route.source, route.method, ".*", PORT); // <- should be one for GET (with a cache interceptor) and one for other methods 
    	ServiceProxy sp = new ServiceProxy(key, route.destination, route.destinationPort);
    	if (route.cache > 0) {
    		sp.getInterceptors().add(new CachingInterceptor());
    	}
		router.add(sp);
		log.info("added route on " + route.source + " for method " + route.method);
		return sp;
    }
    
    public static void loadRoutes() throws IOException {
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
			addRoute(route);
		}
		log.info("loaded " + routes.length + " routes from file");
    }
    
    public static void saveRoutes() throws IOException {
    	
    	// to be removed

    	BufferedWriter bw = new BufferedWriter(new FileWriter(new File("routes.json")));
    	Gson gson = new Gson();
    	
    	Route[] routes = new Route[]{
    			new Route("monitor.virtuoso", "10.232.132.100", "GET", 3000, 1000, 5 * Cache.MINUTES)
    	};
    	bw.write(gson.toJson(routes));
    	bw.close();
    }

	public static void main(String[] args) throws Exception {
		
		Cache.init();
		 
		loadRoutes();

		router.getTransport().setPrintStackTrace(true);
		router.init();

		log.info("membrane-proxy started");
	}
}
