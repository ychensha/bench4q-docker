package org.bench4q.docker;

import java.util.List;

public class Resource {
	private List<Container> containers;
	private long memTotal;
	private long memFree;
	public long getMemTotal() {
		return memTotal;
	}
	public void setMemTotal(long memTotal) {
		this.memTotal = memTotal;
	}
	public long getMemFree() {
		return memFree;
	}
	public void setMemFree(long memFree) {
		this.memFree = memFree;
	}
}
