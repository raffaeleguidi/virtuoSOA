package org.virtuosoa.models;

import java.io.Serializable;

import org.virtuosoa.cluster.Cluster;

public class Route implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3216678208061537415L;
	
	public String source;
	public String destination;
	public String method;
	public String path;
	public int destinationPort;
	public int timeout;
	public long cache;

	public Route(String source, String destination, String method, String path, int destinationPort, int timeout, long cache) {
		this.source = source;
		this.destination = destination;
		this.method = method;
		this.destinationPort = destinationPort;
		this.timeout = timeout;
		this.cache = cache;
	}
	public Route save() {
		Cluster.setRoute(key(), this);
		return this;
	}
	public static Route lookup(String key) {
		// see http://hazelcast.org/docs/latest/manual/html/query.html
		// to query the map with sql or criteria query
		return (Route) Cluster.getRoute(key);
	}
	public String key() {
		return "route:" + this.source +"$" + this.method + "$" + this.path; 
	}
 }
