package org.bench4q.docker;

import java.util.List;

public class Resource {
	private long avalCpu;
	private List<Container> containers;
	private long totalMem;
	private long freeMem;

	public long getTotalMem() {
		return totalMem;
	}
	public void setTotalMem(long totalMem) {
		this.totalMem = totalMem;
	}
	public long getFreeMem() {
		return freeMem;
	}
	public void setFreeMem(long freeMem) {
		this.freeMem = freeMem;
	}
	public long getAvalCpu() {
		return avalCpu;
	}
	public void setAvalCpu(long avalCpu) {
		this.avalCpu = avalCpu;
	}
}
