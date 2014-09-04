package utils

import play.api.mvc._
import models._
import play.cache.Cache
import play.api.Logger
import play.libs.F.Promise
import play.api.GlobalSettings


object Global extends WithFilters(LoggingFilter) {
//    def findRoute(source: String) = {
//    	val route = Cache.get("route:" + source).asInstanceOf[Route]
//    	if (route  != null) {
//		  	route
//		} else {
//		  val route = Route.find.where().eq("source", source).findUnique()
//		  route.cache = 10
//		  Cache.set("route:" + source, route)
//		  route
//		}
//    }
    
//    def cachePrefix(route: Route): Double = {
//    	val cachePrefix = Cache.get(route.id).asInstanceOf[Double]
//    	if (cachePrefix != 0) {
//    	  cachePrefix 
//    	} else {
//    	  val cachePrefix = route.randomSeed
//    	  Cache.set(route.id, route.randomSeed, 60)
//    	  route.randomSeed
//    	}
//    }

//	override def onRouteRequest(rh: RequestHeader): Option[Handler] = {
//    	val route = findRoute(rh.host)
//    	Logger.info("********************* " + route.destination + rh.path + rh.rawQueryString)
//	  (rh.method) match {
//	    case ("GET") => {
//	    	if (route.cache > 0) {
//		    	val cacheKey = cachePrefix(route).toString + rh.host + rh.path + rh.rawQueryString
//		    	Logger.info("********************* " + cacheKey)
//		    	val cachedResponse = Cache.get(cacheKey)
//		    	if (cachedResponse != null) {
//		    	  Some(cachedResponse)
//		    	} else {
//		    		Logger.info("da zero")
//			    	val res = controllers.Proxy.syncGet("http://" + route.destination + rh.path + rh.rawQueryString, route.timeout.toLong)
//			    	Logger.info("da zero e 1")
//			    	Cache.set(cacheKey, res, route.cache)
//			    	Logger.info("da zero e 2")
//			    	Some(res)
//		    	}
//	    	} else {
//	    	  Logger.info("no cache")
//	    	  Some(controllers.Proxy.asyncGetNoCache("http://" + route.destination + rh.path + rh.rawQueryString))
//	    	}
//	    }
//	    case _ => None
//	  }		
//	  Logger.info("executed before every request:" + rh.toString)
//	  super.onRouteRequest(rh)
//	}
}
