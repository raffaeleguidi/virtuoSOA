package utils

import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Route
import play.api.cache.Cache
import play.api.Play.current

object CachingFilter extends Filter {
  


	
	def apply(nextFilter: (RequestHeader) => Future[Result])
			(rh: RequestHeader): Future[Result] = {	  
		
		nextFilter(rh).map { result =>
			result
	    }

	}
}
