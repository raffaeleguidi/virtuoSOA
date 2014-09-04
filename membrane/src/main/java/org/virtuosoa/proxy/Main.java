package org.virtuosoa.proxy;


import org.virtuosoa.interceptors.MyInterceptor;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class Main {
	
    static final int PORT = Integer.parseInt(System.getProperty("port", "4000"));

	public static void main(String[] args) throws Exception {
//		String hostname = "*";
		String method = "GET";
		String path = ".*";

		ServiceProxyKey key1 = new ServiceProxyKey("monitor.virtuoso", method, path, PORT);
		ServiceProxyKey key2 = new ServiceProxyKey("test.virtuoso", method, path, PORT);

		String targetHost = "predic8.com";
		String targetHost2 = "google.com";
		int targetPort = 80;

		ServiceProxy sp = new ServiceProxy(key1, targetHost, targetPort);
		ServiceProxy sp2 = new ServiceProxy(key2, targetHost2, targetPort);
		sp.getInterceptors().add(new MyInterceptor());
		sp2.getInterceptors().add(new MyInterceptor());

		HttpRouter router = new HttpRouter();
		router.add(sp);
		router.add(sp2);
		router.init();
	}
}
