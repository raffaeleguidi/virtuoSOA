package org.virtuosoa.cache;

public class Expiring {
	public long expiresAt;
	public Object payload;
	public Expiring(Object payload, long expiresIn) {
		this.payload = payload;
		this.expiresAt = expiresIn ;
	}
	public static Expiring in(Object payload, long expiresIn) {
		return new Expiring(payload, expiresIn + System.currentTimeMillis());
	}
	public static Expiring at(Object payload, long expiresAt) {
		return new Expiring(payload, expiresAt);
	}
}
