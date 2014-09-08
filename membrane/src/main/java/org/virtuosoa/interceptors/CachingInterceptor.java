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
	
	@Override public void handleAbort(Exchange exchange) {
		log.info("handleAbort at  " + (System.currentTimeMillis()));
		Response resp = new Response();
		resp.setBodyContent("transaction aborted".getBytes());
		resp.setStatusCode(200);
		exchange.setResponse(resp);
	};
	
	@Override public Outcome handleResponse(Exchange exchange) throws Exception {
		exchange.getResponse().getHeader().add("X-Served-In", "" + (System.currentTimeMillis() - startedAt));
		exchange.getResponse().getHeader().add("X-Served-From", "upstream provider");
		Response rs = exchange.getResponse();
		Request rq = exchange.getRequest();
		String key = rq.getHeader().getFirstValue("Cache-Key");
		String routeKey = rq.getHeader().getFirstValue("Route-Key");
		Route route = Route.find(routeKey);
		log.info("looking in cache");
		if (route.cache > 0) {
			log.info("saving in cache");
			log.info("Cache-Key " + key );
			log.info("Route-Key " + routeKey );
			Cache.set("sCode:" + key, new Integer(rs.getStatusCode()), route.cache);
			Cache.set("sText:" + key, rs.getStatusMessage(), route.cache);
			Cache.set("body:" + key, rs.getBodyAsStringDecoded(), route.cache);
			Cache.set("type:" + key, rs.getHeader().getContentType(), route.cache);
		}
		log.info("request served in " + (System.currentTimeMillis() - startedAt) + " msecs");
		return Outcome.CONTINUE;
	};
	
	private long startedAt;
	
	private String stripPort(String host) {
		return host.substring(0, host.indexOf(":"));
	}

	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		startedAt = System.currentTimeMillis();
		
		Request rq = exchange.getRequest();
		
		String routeKey = stripPort(rq.getHeader().getHost()) + "$" + rq.getMethod();
		exchange.getRequest().getHeader().add("Route-Key", routeKey);
		
		String key =  rq.getMethod() + "$" + rq.getHeader().getHost() + "$" + rq.getUri();
		exchange.getRequest().getHeader().add("Cache-Key", key);

		// this can work only on explicit verbs
		// have to try it in two steps - first explicit and 2nd with *
		// better move it in a AbstractRoutingInterceptor for reuse
		Route route = Route.find(routeKey);
		log.info("looking for routeKey " + routeKey + " I found " + route);
		if (route.cache > 0) {
			Integer code = (Integer) Cache.get("sCode:" + key);
			log.debug("code for " + key + " is " + code);
			if (code != null) {
				Response fromCache = new Response();
				fromCache.getHeader().add("X-Served-In", "" + (System.currentTimeMillis() - startedAt));
				fromCache.getHeader().add("X-Served-From", "local cache");
				log.info("key " + key + " served from cache");
				fromCache.setStatusCode(code.intValue());
				fromCache.setStatusMessage((String) Cache.get("sText:" + key));
				fromCache.setBodyContent(((String) Cache.get("body:" + key)).getBytes());
				fromCache.getHeader().setContentType(Cache.getAsString("type:" + key));
				exchange.setResponse(fromCache);
				return Outcome.RETURN;
			} else {
				log.info("key " + key + " not found in cache");
			}
		}

		return Outcome.CONTINUE;
	}
}