

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class Main {
	public static void main(String[] args) throws Exception {
		String hostname = "*";
		String method = "GET";
		String path = ".*";
		int listenPort = 4000;

		ServiceProxyKey key1 = new ServiceProxyKey("monitor.virtuoso", method, path, listenPort);
		ServiceProxyKey key2 = new ServiceProxyKey("test.virtuoso", method, path, listenPort);

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
