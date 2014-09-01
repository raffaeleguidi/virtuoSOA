package utils

import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EmptyFilter extends Filter {

	def apply(nextFilter: (RequestHeader) => Future[Result])
				(requestHeader: RequestHeader): Future[Result] = {	  
		nextFilter(requestHeader).map { result =>
		  
		  Logger.info(" ******************************** " + requestHeader.method)
		  result
	    }
	  }
}
