package org.virtuosoa.utils;

import java.io.Serializable;

public class Route implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3216678208061537415L;
	
	public String source;
	public String destination;
	public String method;
	public int destinationPort;
	public int timeout;
	public int cache;

	public Route(String source, String destination, String method, int destinationPort, int timeout, int cache) {
		this.source = source;
		this.destination = destination;
		this.method = method;
		this.destinationPort = destinationPort;
		this.timeout = timeout;
		this.cache = cache;
	}
	public Route save() {
		Cache.setGlobal("route:" + source + "$" + method, this);
		return this;
	}
	public static Route find(String sourceAndMethod) {
		return (Route) Cache.getGlobal("route:" + sourceAndMethod);
	}
}
