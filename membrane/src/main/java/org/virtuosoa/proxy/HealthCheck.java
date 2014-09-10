package org.virtuosoa.proxy;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;
import org.virtuosoa.cache.Cache;
import org.virtuosoa.models.CheckResult;
import org.virtuosoa.models.Route;

import com.codahale.metrics.Meter;

public class HealthCheck {
	private static final Logger log = Logger.getLogger(HealthCheck.class.getCanonicalName());
	
	public static void run(Route route) {
		try {
			HttpResponse response = Request.Head("http://" + route.destination + ":" + route.destinationPort)
				.execute()
				.returnResponse();
			
			boolean is200 = response.getStatusLine().getStatusCode() == 200;
			if (is200) {
				markAsHealthy(route);
				return;
			} else {
				markAsUnhealthy(
					route, 
					"HEAD request returned " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase()
				);
			} 
			
		} catch (IOException e) {
	    	log.warn("route " + route.destination + ":" + route.destinationPort + " has a technical problem - " + e.getMessage());
			markAsUnhealthy(route, "technical problem - " + e.getMessage());
		}
 	}

	public static CheckResult markAsUnhealthy(Route route, String reason) {
		CheckResult check = new CheckResult(false);
		Cache.set("health:" + route.destination + "$" + route.destinationPort, check, Cache.MINUTES * 5);
		final Meter errors = Main.metrics.meter("healthcheckerrors:" + route.destination + "$" + route.destinationPort);
		errors.mark();
	   	log.warn("route " + route.destination + ":" + route.destinationPort + " is unhealthy");
		return check;
	}
	
	public static void markAsHealthy(Route route) {
		Cache.remove("health:" + route.destination + "$" + route.destinationPort);
    	log.info("route " + route.destination + ":" + route.destinationPort + " is healthy");
	}
	
	public static CheckResult check(Route route) {
		CheckResult check = (CheckResult) Cache.get("health:" + route.destination + "$" + route.destinationPort);
		if (check == null) check = new CheckResult(true);
		return check;
	}
	
}
