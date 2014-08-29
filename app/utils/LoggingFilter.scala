package utils

import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object LoggingFilter extends Filter {
  
	def createUuid() = {
		java.util.UUID.randomUUID.toString
	}


	def apply(nextFilter: (RequestHeader) => Future[Result])
				(requestHeader: RequestHeader): Future[Result] = {
	    val startTime = System.currentTimeMillis
	    val uuid = createUuid()
//	    val uuid = requestHeader.id.toString
//	    val rhWithTraceId = requestHeader.copy(tags = requestHeader.tags + ("traceId" -> uuid))
	    
	    val rhWithTraceId = requestHeader.copy(
		  headers =
		    new Headers{
		      val data : Seq[(String, Seq[String])]  = 
		         (requestHeader.headers.toMap.toSeq  ++ Seq("traceId" -> Seq(uuid))).toSeq
		    }
		)
		
	    Logger.info(s"request ${uuid} started at ${startTime}")
	    nextFilter(rhWithTraceId).map { result =>
	      val endTime = System.currentTimeMillis
	      val requestTime = endTime - startTime
	    
	      Logger.info(
	        s"request ${uuid} ${requestHeader.method} ${requestHeader.uri} " +
		    s"took ${requestTime}ms and returned code HTTP_${result.header.status}"
		  )
		  result
		  .withHeaders(
		      "Cache-Control" -> "no-cache, no-store, must-revalidate",
		      "Pragma" -> "no-cache",
		      "Expires" -> "0"
		  )
//		  .withHeaders("Request-Time" -> requestTime.toString, "traceId" -> uuid)
	    }
	  }
}
