package org.virtuosoa.interceptors;



import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.virtuosoa.utils.Cache;

import com.predic8.membrane.core.exchange.*;
import com.predic8.membrane.core.http.Header;
import com.predic8.membrane.core.http.Request;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.*;
import com.predic8.membrane.core.ws.relocator.Relocator;

public class CachingInterceptor extends AbstractInterceptor {
	private static final Logger log = Logger.getAnonymousLogger();
	
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
		log.info("Cache-Key " + key );
		Cache.set("sCode:" + key, new Integer(rs.getStatusCode()));
		Cache.set("sText:" + key, rs.getStatusMessage());
		Cache.set("body:" + key, rs.getBodyAsStringDecoded());
		Cache.set("type:" + key, rs.getHeader().getContentType());
		return Outcome.CONTINUE;
	};
	
	private static long startedAt;

	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		startedAt = System.currentTimeMillis();
		
		Response rs = exchange.getResponse();
		Request rq = exchange.getRequest();
		
//		Cache.stats();

		String key =  rq.getMethod() + "$" + rq.getHeader().getHost() + "$" + rq.getUri();
		exchange.getRequest().getHeader().add("Cache-Key", key);

		Integer code = (Integer) Cache.get("sCode:" + key);
		log.info("code for " + key + " is " + code);
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

		return Outcome.CONTINUE;
	}
}