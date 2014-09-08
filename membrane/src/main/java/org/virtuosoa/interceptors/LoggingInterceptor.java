package org.virtuosoa.interceptors;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.AbstractInterceptor;
import com.predic8.membrane.core.interceptor.Outcome;

public class LoggingInterceptor extends AbstractInterceptor {
	private static final Logger log = Logger.getLogger(LoggingInterceptor.class.getCanonicalName());
	
	@Override public void handleAbort(Exchange exchange) {
		log.info("handleAbort at  " + (System.currentTimeMillis()));
		Response resp = new Response();
		resp.setBodyContent("transaction aborted".getBytes());
		resp.setStatusCode(200);
		exchange.setResponse(resp);
	};
	
	@Override public Outcome handleResponse(Exchange exchange) throws Exception {
		String traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		log.info("Request " + exchange.getRequest().getUri() + " with Trace-Id " + traceId + " served in " + (System.currentTimeMillis() - startedAt) + " msecs");
		return Outcome.CONTINUE;
	};
	
	private long startedAt;
	
	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		startedAt = System.currentTimeMillis();
		String traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		log.debug("Trace-Id " + traceId + " request received");
		return Outcome.CONTINUE;
	}
}