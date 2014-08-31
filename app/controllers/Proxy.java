package controllers;

import play.*;
import play.mvc.*;
import play.mvc.Http.Response;
import views.html.*;
import models.ResponseCache;
import models.Route;
import play.data.Form;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import play.db.ebean.Model;
import static play.libs.Json.*;
import play.libs.ws.*;
import play.libs.F;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.api.mvc.Request;
import play.cache.Cache;
import scala.Function0;
import scala.concurrent.impl.Future;

public class Proxy extends Controller {
	
	public static Promise<Result> asyncGetNoCache(String url) {
		final long beforeRequest = System.currentTimeMillis();
	    final Promise<Result> resultPromise = WS.url(url).get().map(
            new Function<WSResponse, Result>() {
                public Result apply(WSResponse response) {
                	response().setContentType(response.getHeader("Content-Type"));
                	response().setHeader("UpstreamResponse-Time", "" + (System.currentTimeMillis()-beforeRequest));
                	return status(response.getStatus(), response.getBody());
                }
            }
	    );
	    return resultPromise;
	}

//	public static Promise<Result> asyncGetWithCache(String url, final Route route, final String cacheKey) {
//		ResponseCache responseCache = (ResponseCache)Cache.get(cacheKey);
//		if (responseCache == null) {
//		    final Promise<Result> resultPromise = WS.url(url).get().map(
//	            new Function<WSResponse, Result>() {
//	                public Result apply(WSResponse response) {
//	                	response().setContentType(response.getHeader("Content-Type"));
//	                   	final ResponseCache responseCache = new ResponseCache();
//	            		responseCache.body = response.getBody();
//	                	responseCache.contentType = response.getHeader("Content-Type");
//	                	responseCache.headers = response.getAllHeaders().entrySet();
//	                	responseCache.status = response.getStatus();
//	                	copyResponseHeaders(responseCache);
//	                	Cache.set(cacheKey, responseCache, route.timeout);
//	                	return status(responseCache.status, responseCache.body);
//	                }
//	            }
//		    );
//		    return resultPromise;
//		} else {	
//			return new F.Promise<Result>((scala.concurrent.Future<Result>) new SimpleResult(responseCache.status, responseCache.body));
//			
//		}
//	}
	
	
	public static Result getWithCache(String url, final Route route, final String cacheKey) {
		ResponseCache responseCache = (ResponseCache)Cache.get(cacheKey);
		if (responseCache == null) {
	    	WSResponse response = createHolder(route).get().get(route.timeout);;
	    	response().setContentType(response.getHeader("Content-Type"));
	       	responseCache = new ResponseCache();
			responseCache.body = response.getBody();
	    	responseCache.contentType = response.getHeader("Content-Type");
	    	responseCache.headers = response.getAllHeaders().entrySet();
	    	responseCache.status = response.getStatus();
	    	copyResponseHeaders(responseCache);
	    	Cache.set(cacheKey, responseCache, route.timeout);
		}
    	return status(responseCache.status, responseCache.body);
	}

    public static Result handleGet(String path) throws Exception {
    	
    	final Route route = findRoute(request().host());
    	
		
		final String cacheKey = "GET:" + Cache.getOrElse("source:" + route.randomSeed, new java.util.concurrent.Callable<String>() {
			@Override
			public String call() throws Exception {
				Logger.trace("saved in cache randomSeed " + route.randomSeed + " for route " + route.source);
				return "" + route.randomSeed;
			}
		}, 60) + request().host() + request().uri();
		
		
		final String upstreamUrl = "http://" + route.destination + request().uri().replaceAll("%20", "+");
		
		if (route.cache == 0)
			return asyncGetNoCache(upstreamUrl).get(route.timeout);
		else {
			return getWithCache(upstreamUrl, route, cacheKey);
		}
//		Logger.trace("cacheKey=" + cacheKey);
//		ResponseCache responseCache = (ResponseCache)Cache.get(cacheKey);
//		if (responseCache == null) {
//        	Logger.trace("sending get to downstream server " + beforeRequest);
//        	downstreamResponse = holder.get().get(route.timeout);
//        	responseCache = getResponse(downstreamResponse);
//        	Logger.trace("received response from downstream server in " + (System.currentTimeMillis() - beforeRequest) +"ms " + System.currentTimeMillis());
//        	if (route.cache > 0) {
//        		Cache.set(cacheKey, responseCache, route.cache);
//        		Logger.trace("saved in cache for " + route.cache + "s " + System.currentTimeMillis() );
//        	}
//		} else {
//        	Logger.trace("got response from cache " + System.currentTimeMillis() );
//		}
//
//    	copyResponseHeaders(responseCache);
//    	
//        return play.mvc.Results.status(responseCache.status, responseCache.body);
    }

	private static Route findRoute(String source) {
		Route route = (Route)Cache.get("route:" + source);
		if (route == null) {
			Logger.trace("looking for " + source);
			route = Route.find.where().eq("source", source).findUnique();
			if (null != route.timeout) {
				route.timeout = 10000;
			}
			Cache.set("route:" + source, route, 60); // in cache per 60 secondi
		}
		Logger.trace("got route with seed " + route.randomSeed);
		return route;
	}

	private static ResponseCache getResponse(WSResponse downstreamResponse) {
       	final ResponseCache responseCache = new ResponseCache();
		responseCache.body = downstreamResponse.getBody();
    	responseCache.contentType = downstreamResponse.getHeader("Content-Type");
    	responseCache.headers = downstreamResponse.getAllHeaders().entrySet();
    	responseCache.status = downstreamResponse.getStatus();
    	return responseCache;
	}
    
	private static void copyResponseHeaders(ResponseCache responseCache) {
		response().setContentType(responseCache.contentType);
		
		for (Entry<String, List<String>> header : responseCache.headers) {
			response().setHeader(header.getKey(), header.getValue().get(0));
			if (Logger.isTraceEnabled()) Logger.trace("response cache headers: " + header.getKey() + "=" + header.getValue().get(0));
		}
		
		if (Logger.isTraceEnabled()) {
			for (Entry<String, String> header: response().getHeaders().entrySet()) {
				Logger.trace("response headers: " + header.getKey() + "=" + header.getValue());
			}
			
	    	Logger.trace("added headers to upstream request " + System.currentTimeMillis());
		}
	}

	private static WSRequestHolder createHolder(Route route) {
		WSRequestHolder holder = WS.url("http://" + route.destination + request().uri().replaceAll("%20", "+"));
		if (Logger.isTraceEnabled()) Logger.trace("creating request " + request().getHeader("traceId") + " " + System.currentTimeMillis());
		for (Entry<String, String[]> header : request().headers().entrySet()) {
			holder.setHeader(header.getKey(), header.getValue()[0]);
			if (Logger.isTraceEnabled()) Logger.trace("request headers: " + header.getKey() + "=" + header.getValue()[0]);
		}
		return holder;
	}
    
    public static Result handlePost(String path) {
    	
    	Route route = findRoute(request().host());
    	
    	WSResponse downstreamResponse;
		WSRequestHolder holder = createHolder(route);
		long beforeRequest = System.currentTimeMillis();
		
		ResponseCache responseCache = null;

    	Logger.trace("sending post to downstream server " + beforeRequest);
		downstreamResponse = holder.post(request().body().asText()).get(route.timeout);
    	responseCache = getResponse(downstreamResponse);

    	Logger.trace("received response from downstream server in " + (System.currentTimeMillis() - beforeRequest) +"ms " + System.currentTimeMillis());
    	copyResponseHeaders(responseCache);
    	
        return play.mvc.Results.status(responseCache.status, responseCache.body);
    }
}
