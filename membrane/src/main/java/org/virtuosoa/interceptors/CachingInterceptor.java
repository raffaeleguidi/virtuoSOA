package org.virtuosoa.interceptors;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.virtuosoa.cache.Cache;
import org.virtuosoa.models.Route;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Request;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.AbstractInterceptor;
import com.predic8.membrane.core.interceptor.Outcome;

public class CachingInterceptor extends AbstractInterceptor {
	private static final Logger log = Logger.getLogger(CachingInterceptor.class.getCanonicalName());
	private String routeKey;
	
	public CachingInterceptor(Route route) {
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
		exchange.getResponse().getHeader().add("X-Served-In", "" + (System.currentTimeMillis() - startedAt));
		exchange.getResponse().getHeader().add("X-Served-From", "upstream provider");
		Response rs = exchange.getResponse();
		Request rq = exchange.getRequest();
		String key = rq.getHeader().getFirstValue("Cache-Key");
		String traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
//		String routeKey = rq.getHeader().getFirstValue("Route-Key");
		Route route = Route.lookup(routeKey);
		log.info("looking in cache using route " + route);
		if (route.cache > 0) {
			log.info("saving in cache");
			log.info("Cache-Key " + key );
			log.info("Route-Key " + routeKey );
			Cache.set("sCode:" + key, new Integer(rs.getStatusCode()), route.cache);
			Cache.set("sText:" + key, rs.getStatusMessage(), route.cache);
			Cache.set("body:" + key, rs.getBodyAsStringDecoded(), route.cache);
			Cache.set("type:" + key, rs.getHeader().getContentType(), route.cache);
		}
		log.info("request " + traceId + " served in " + (System.currentTimeMillis() - startedAt) + " msecs");
		return Outcome.CONTINUE;
	};
	
	private long startedAt;
	
	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		startedAt = System.currentTimeMillis();
		String traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		
		Request rq = exchange.getRequest();
		
		String key =  rq.getMethod() + "$" + rq.getHeader().getHost() + "$" + rq.getUri();
		exchange.getRequest().getHeader().add("Cache-Key", key);
		Route route = Route.lookup(routeKey);
		log.trace("looking for routeKey " + routeKey + " I found " + route);
		if (route.cache > 0) {
			Integer code = (Integer) Cache.get("sCode:" + key);
			if (code != null) {
				Response fromCache = new Response();
				fromCache.getHeader().add("X-Served-In", "" + (System.currentTimeMillis() - startedAt));
				fromCache.getHeader().add("X-Served-From", "local cache");
				log.info("request " + traceId + " and key " + key + " served from cache");
				fromCache.setStatusCode(code.intValue());
				fromCache.setStatusMessage((String) Cache.get("sText:" + key));
				fromCache.setBodyContent(((String) Cache.get("body:" + key)).getBytes());
				fromCache.getHeader().setContentType(Cache.getAsString("type:" + key));
				// disables http cache since we are handling it in memory
				fromCache.getHeader().add("Cache-Control", "no-cache");
				fromCache.getHeader().add("Cache-Control", "no-store");
				fromCache.getHeader().add("Cache-Control", "must-revalidate");
				fromCache.getHeader().add("Pragma", "no-cache");
				fromCache.getHeader().add("Expires", "must-0");
				exchange.setResponse(fromCache);
				return Outcome.RETURN;
			} else {
				log.trace("key " + key + " not found in cache");
			}
		}

		return Outcome.CONTINUE;
	}
}