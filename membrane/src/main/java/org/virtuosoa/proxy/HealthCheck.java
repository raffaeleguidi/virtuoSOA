package org.virtuosoa.proxy;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;
import org.virtuosoa.models.Route;

import com.codahale.metrics.Meter;

public class HealthCheck {
	private static final Logger log = Logger.getLogger(HealthCheck.class.getCanonicalName());
	
	public static void run(Route route) {
		final Meter errors = Main.metrics.meter("healthcheckerrors:" + route.key());
		try {
			HttpResponse response = Request.Head("http://" + route.destination + ":" + route.destinationPort)
				.execute()
				.returnResponse();
			route.setHealth(response.getStatusLine().getStatusCode() == 200);
			log.trace("check of " + route.destination + ":" + route.destinationPort + " response code is " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
		} catch (IOException e) {
			log.trace("route " + route.key() + " " + e.getMessage());
			route.setHealth(false);
		}
		if (!route.healthy) {
			errors.mark();
		}
    	log.info("route " + route.key() + " is " + (route.healthy ? "healthy" : "unhealthy"));
	}
	public static void markAsUnhealthy(Route route) {
		route.setHealth(false);
		final Meter errors = Main.metrics.meter("healthcheckerrors:" + route.key());
		errors.mark();
	}
}
