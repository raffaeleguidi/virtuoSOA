package org.virtuosoa.utils;

import java.io.Serializable;

public class Route implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3216678208061537415L;
	
	public String source;
	public String destination;
	public int timeout;
	public int cache;
	public Route(String source, String destination, int timeout, int cache) {
		this.source = source;
		this.destination = destination;
		this.timeout = timeout;
		this.cache = cache;
	}
	public void save() {
		Cache.setGlobal("route:" + source, this);
	}
	public static Route findBySource(String source) {
		return (Route) Cache.getGlobal("route:" + source);
	}
}
