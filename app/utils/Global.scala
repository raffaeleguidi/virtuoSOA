package utils

import play.api.mvc._
import models.Route
import play.cache.Cache


object Global extends WithFilters(LoggingFilter) {
//    def findRoute(source: String) = {
//    	Cache.getOrElse[Route]("route:" + source) {
//		  	Route.find.where().eq("source", source).findUnique()
//		}
//    }
//	val route = findRoute(rh.host) 
//	val cachePrefix =  rh.method + ":" + Cache.getOrElse[Double]("source:" + route.randomSeed) {
//		route.randomSeed
//	}
//	val cacheKey = cachePrefix + rh.host + rh.path + rh.rawQueryString 
//		Cache.getOrElse[play.libs.F.Promise[play.api.mvc.Result]](cacheKey) {
//			controllers.Proxy.asyncGetNoCache("ss")
//		}
	override def onRouteRequest(request: RequestHeader): Option[Handler] = {
		println("executed before every request:" + request.toString)
		super.onRouteRequest(request)
	}
}
