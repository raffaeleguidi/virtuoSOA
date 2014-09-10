package org.virtuosoa.models;

public class CheckResult {
	public boolean healthy = true;
	public long lastHealthChecked;
	public String reason;

	public CheckResult(boolean healthy) {
		this.lastHealthChecked = System.currentTimeMillis();
		this.healthy =  healthy;
	}
	public CheckResult(boolean healthy, String reason) {
		this.lastHealthChecked = System.currentTimeMillis();
		this.healthy =  healthy;
	}
}
