package org.virtuosoa.proxy;


import java.io.IOException;

import org.virtuosoa.interceptors.MyInterceptor;
import org.virtuosoa.utils.Cache;
import org.virtuosoa.utils.Route;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class Main {
	
    static final int PORT = Integer.parseInt(System.getProperty("port", "4000"));
	static HttpRouter router = new HttpRouter();
    
    public static ServiceProxy addRoute(String source) throws IOException {
    	Route route = Route.findBySource(source);
    	return addRoute(route);
     }
    
    public static ServiceProxy addRoute(Route route) throws IOException {
       	ServiceProxyKey key = new ServiceProxyKey(route.source, "*", ".*", PORT); // <- should be one for GET (with a cache interceptor) and one for other methods 
    	ServiceProxy sp = new ServiceProxy(key, route.destination, route.destinationPort);
    	sp.getInterceptors().add(new MyInterceptor());
		router.add(sp);
		return sp;
    }

	public static void main(String[] args) throws Exception {

		Cache.init();
		 
		addRoute(new Route("monitor.virtuoso", "10.232.132.100", 3000, 1000, 300).save());
		addRoute(new Route("test.virtuoso", "10.232.132.100", 3000, 1000, 0).save());
		addRoute(new Route("telefoni.virtuoso", "telefoni", 80, 1000, 0).save());
		addRoute(new Route("google.virtuoso", "google.com", 80, 1000, 0).save());

		router.getTransport().setPrintStackTrace(true);
		router.init();
	}
}
