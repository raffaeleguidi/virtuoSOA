package org.virtuosoa.interceptors;

import java.net.MalformedURLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.virtuosoa.models.Route;
import org.virtuosoa.proxy.Main;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.AbstractInterceptor;
import com.predic8.membrane.core.interceptor.Outcome;

public class BaseVirtuosoInterceptor extends AbstractInterceptor {
	private static final Logger log = Logger.getLogger(BaseVirtuosoInterceptor.class.getCanonicalName());
	private final Meter requests = Main.metrics.meter("requests");
	private final Meter responses = Main.metrics.meter("responses");
	private final Histogram responseTime = Main.metrics.histogram("responseTime");
	
	private String routeKey;
	
	public BaseVirtuosoInterceptor(Route route) {
		routeKey = route.key();
	}

	@Override public void handleAbort(Exchange exchange) {
		log.info("handleAbort at  " + (System.currentTimeMillis()));
		Response resp = new Response();
		resp.setBodyContent("transaction aborted".getBytes());
		resp.setStatusCode(500);
		exchange.setResponse(resp);
	};
	
	@Override public Outcome handleResponse(Exchange exchange) throws Exception {
		responses.mark();
		responseTime.update(System.currentTimeMillis() - startedAt);
		exchange.getResponse().getHeader().add("Trace-Id", traceId);
		exchange.getResponse().getHeader().add("Route-Key", routeKey);
		log.info("Response to '" + exchange.getRequest().getUri() + "' with Trace-Id " + traceId + " served in " + (System.currentTimeMillis() - startedAt) + " msecs");
		return Outcome.CONTINUE;
	};
	
	private long startedAt;
	String traceId;
	
	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		requests.mark();
		
		if (!Route.lookup(routeKey).healthy) {
			// TBD
			return Outcome.ABORT;
		}
		
		startedAt = System.currentTimeMillis();
		traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
			exchange.getRequest().getHeader().add("Trace-Id", traceId);
		}
		exchange.getRequest().getHeader().add("Route-Key", routeKey);
		log.info("Request '" + exchange.getRequest().getUri() + "' with Trace-Id " + traceId + " received");
		return Outcome.CONTINUE;
	}
}