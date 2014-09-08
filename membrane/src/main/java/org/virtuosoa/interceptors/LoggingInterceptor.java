package org.virtuosoa.interceptors;

import java.net.MalformedURLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.virtuosoa.models.Route;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.AbstractInterceptor;
import com.predic8.membrane.core.interceptor.Outcome;

public class LoggingInterceptor extends AbstractInterceptor {
	private static final Logger log = Logger.getLogger(LoggingInterceptor.class.getCanonicalName());
	
	private String stripPort(String host) {
		return host.substring(0, host.indexOf(":"));
	}
	
	
	@Override public void handleAbort(Exchange exchange) {
		log.info("handleAbort at  " + (System.currentTimeMillis()));
		Response resp = new Response();
		resp.setBodyContent("transaction aborted".getBytes());
		resp.setStatusCode(200);
		exchange.setResponse(resp);
	};
	
	@Override public Outcome handleResponse(Exchange exchange) throws Exception {
		//String traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		exchange.getResponse().getHeader().add("Trace-Id", traceId);
		log.info("Request " + exchange.getRequest().getUri() + " with Trace-Id " + traceId + " served in " + (System.currentTimeMillis() - startedAt) + " msecs");
		return Outcome.CONTINUE;
	};
	
	private long startedAt;
	String traceId;
	
	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		startedAt = System.currentTimeMillis();
		traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
			exchange.getRequest().getHeader().add("Trace-Id", traceId);
		}
		String routeKey = stripPort(exchange.getRequest().getHeader().getHost()) + "$" + exchange.getRequest().getMethod();
		Route route = Route.find(routeKey);
		if (route == null) {
			routeKey = stripPort(exchange.getRequest().getHeader().getHost()) + "$*";
			route = Route.find(routeKey);
		}
		exchange.getRequest().getHeader().add("Route-Key", routeKey);
		
		log.debug("Trace-Id " + traceId + " request received");
		return Outcome.CONTINUE;
	}
}