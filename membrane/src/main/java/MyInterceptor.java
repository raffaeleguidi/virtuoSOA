


import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import com.predic8.membrane.core.exchange.*;
import com.predic8.membrane.core.http.Header;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.interceptor.*;
import com.predic8.membrane.core.ws.relocator.Relocator;

public class MyInterceptor extends AbstractInterceptor {
	private static final Logger log = Logger.getAnonymousLogger();
	
	@Override public void handleAbort(Exchange exchange) {
		log.info("handleAbort at  " + (System.currentTimeMillis()));
		Response resp = new Response();
		resp.setBodyContent("ciao".getBytes());
		resp.setStatusCode(200);
		exchange.setResponse(resp);
	};
	
	@Override public Outcome handleResponse(Exchange exchange) throws Exception {
		log.info("handleResponse at  " + (System.currentTimeMillis()));
		return Outcome.CONTINUE;
	};

	@Override
	public Outcome handleRequest(Exchange exchange) throws MalformedURLException {
		exchange.getRequest().getHeader().add("X-Hello", "Hello World!");
		log.info("handleRequest at  " + (System.currentTimeMillis()));
		
//		Response resp = new Response();
//		resp.setBodyContent("ciao".getBytes());
//		resp.setStatusCode(200);
//		exchange.setResponse(resp);
//
//		
//		return Outcome.RETURN;
		return Outcome.CONTINUE;
	}
}