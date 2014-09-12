package org.virtuosoa.interceptors;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.virtuosoa.cache.Cache;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Request;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.Outcome;

public class CachingInterceptor extends AbstractVirtuosoInterceptor {
	private static final Logger log = Logger.getLogger(CachingInterceptor.class.getCanonicalName());
	
	protected long expiration;
	
	public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
	
	public CachingInterceptor() {
		// noop
	}
	public CachingInterceptor(String routeId, long cacheFor) {
		this.setId(routeId);
		this.setExpiration(cacheFor);
	}
	
	@Override public void handleAbort(Exchange exchange) {
		log.info("handleAbort at  " + (System.currentTimeMillis()));
		Response resp = new Response();
		resp.setBodyContent("transaction aborted".getBytes());
		resp.setStatusCode(500);
		exchange.setResponse(resp);
	};
	
	byte[] streamToArray(java.io.InputStream in) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		try {
			while ((nRead = in.read(data, 0, data.length)) != -1) {
			  buffer.write(data, 0, nRead);
			}
			buffer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer.toByteArray();
	}
	
	@Override public Outcome handleResponse(Exchange exchange) throws Exception {
		exchange.getResponse().getHeader().add("X-Served-In", "" + (System.currentTimeMillis() - startedAt));
		exchange.getResponse().getHeader().add("X-Served-From", "upstream provider");
		Response rs = exchange.getResponse();
		Request rq = exchange.getRequest();
		String key = rq.getHeader().getFirstValue("Cache-Key");
		String traceId = exchange.getRequest().getHeader().getFirstValue("Trace-Id");
		log.info("looking in cache using route " + getId());
		if (expiration > 0 && rs.getStatusCode() == 200) {
			log.info("saving in cache");
			log.info("Cache-Key " + key );
			log.info("Route-Key " + getId() );
			Cache.set("sCode:" + key, new Integer(rs.getStatusCode()), expiration);
			Cache.set("sText:" + key, rs.getStatusMessage(), expiration);
			Cache.set("body:" + key, streamToArray(rs.getBodyAsStream()), expiration);
			//System.out.print(" *************** " + rs.getBodyAsStringDecoded());
			Cache.set("type:" + key, rs.getHeader().getContentType(), expiration);
			
			rs.getHeader().removeFields("Cache-Control");
			rs.getHeader().removeFields("Pragma");
			rs.getHeader().removeFields("Expires");
			
			rs.getHeader().add("Cache-Control", "no-cache");
			rs.getHeader().add("Cache-Control", "no-store");
			rs.getHeader().add("Cache-Control", "must-revalidate");
			rs.getHeader().add("Pragma", "no-cache");
			rs.getHeader().add("Expires", "0");
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

//		exchange.getResponse().getHeader().add("Cache-Control", "no-cache");
//		exchange.getResponse().getHeader().add("Cache-Control", "no-store");
//		exchange.getResponse().getHeader().add("Cache-Control", "must-revalidate");
//		exchange.getResponse().getHeader().add("Pragma", "no-cache");
//		exchange.getResponse().getHeader().add("Expires", "0");
//		
		if (expiration > 0) {
			Integer code = (Integer) Cache.get("sCode:" + key);
			if (code != null) {
				Response fromCache = new Response();
				fromCache.getHeader().add("X-Served-In", "" + (System.currentTimeMillis() - startedAt));
				fromCache.getHeader().add("X-Served-From", "local cache");
				log.info("request " + traceId + " and key " + key + " served from cache");
				fromCache.setStatusCode(code.intValue());
				fromCache.setStatusMessage((String) Cache.get("sText:" + key));
				fromCache.setBodyContent((byte[])Cache.get("body:" + key));
				fromCache.getHeader().setContentType(Cache.getAsString("type:" + key));
				// disables http cache since we are handling it in memory
				fromCache.getHeader().add("Cache-Control", "no-cache");
				fromCache.getHeader().add("Cache-Control", "no-store");
				fromCache.getHeader().add("Cache-Control", "must-revalidate");
				fromCache.getHeader().add("Pragma", "no-cache");
				fromCache.getHeader().add("Expires", "0");

				exchange.setResponse(fromCache);
				return Outcome.RETURN;
			} else {
				log.trace("key " + key + " not found in cache");
			}
		}

		return Outcome.CONTINUE;
	}
}