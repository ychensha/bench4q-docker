package org.bench4q.docker.model;

import java.util.List;

public class ContainerConfig{
	private String hostname;
	private String user;
	private int cpuShares;
	private long memory;
	private long memorySwap;
	private String cpuset;
	private List<String> cmd;
	private String dns;
	private String image;
	
	private String workingDir;

	public int getCpuShares() {
		return cpuShares;
	}

	public void setCpuShares(int cpuShares) {
		this.cpuShares = cpuShares;
	}

	public String getCpuset() {
		return cpuset;
	}

	public void setCpuset(String cpuset) {
		this.cpuset = cpuset;
	}

	public long getMemory() {
		return memory;
	}

	public void setMemory(long memory) {
		this.memory = memory;
	}
}