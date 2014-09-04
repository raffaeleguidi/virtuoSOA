package org.virtuosoa.utils;

import java.io.Serializable;

public class Route implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3216678208061537415L;
	
	public String source;
	public String destination;
	public int destinationPort;
	
	public int timeout;
	public int cache;
	public Route(String source, String destination, int destinationPort, int timeout, int cache) {
		this.source = source;
		this.destination = destination;
		this.destinationPort = destinationPort;
		this.timeout = timeout;
		this.cache = cache;
	}
	public Route save() {
		Cache.setGlobal("route:" + source, this);
		return this;
	}
	public static Route findBySource(String source) {
		return (Route) Cache.getGlobal("route:" + source);
	}
}
