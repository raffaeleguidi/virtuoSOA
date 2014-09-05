package org.virtuosoa.proxy;


import java.io.IOException;

import org.virtuosoa.cache.Cache;
import org.virtuosoa.interceptors.CachingInterceptor;
import org.virtuosoa.utils.Route;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class Main {
	
    static final int PORT = Integer.parseInt(System.getProperty("port", "4000"));
	static HttpRouter router = new HttpRouter();
    
    public static ServiceProxy addRoute(String source) throws IOException {
    	Route route = Route.find(source);
    	return addRoute(route);
     }
    
    public static ServiceProxy addRoute(Route route) throws IOException {
       	ServiceProxyKey key = new ServiceProxyKey(route.source, route.method, ".*", PORT); // <- should be one for GET (with a cache interceptor) and one for other methods 
    	ServiceProxy sp = new ServiceProxy(key, route.destination, route.destinationPort);
		router.add(sp);
		return sp;
    }

	public static void main(String[] args) throws Exception {

		Cache.init();
		 
		addRoute(
				new Route("monitor.virtuoso", "8rmw00004738", "GET", 3000, 1000, 5 * Cache.MINUTES).save())
					.getInterceptors().add(new CachingInterceptor());
		addRoute(new Route("monitor.virtuoso", "8rmw00004738", "*", 3000, 1000, 0).save());
		addRoute(new Route("test.virtuoso", "8rmw00004738", "*", 3000, 1000, 0).save());
		addRoute(new Route("telefoni.virtuoso", "telefoni", "*", 80, 1000, 0).save());
		addRoute(new Route("google.virtuoso", "google.com", "*", 80, 1000, 0).save());

		router.getTransport().setPrintStackTrace(true);
		router.init();
	}
}
