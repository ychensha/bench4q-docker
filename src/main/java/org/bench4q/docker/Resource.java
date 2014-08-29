package org.bench4q.docker;


public class Resource {
	private int totalCpu;
	private int freeCpu;
	private int usedCpu;
	private long totalMemeory;
	private long freeMemory;
	private long usedMemory;
	public int getTotalCpu() {
		return totalCpu;
	}
	public void setTotalCpu(int totalCpu) {
		this.totalCpu = totalCpu;
	}
	public int getFreeCpu() {
		return freeCpu;
	}
	public void setFreeCpu(int freeCpu) {
		this.freeCpu = freeCpu;
	}
	public int getUsedCpu() {
		return usedCpu;
	}
	public void setUsedCpu(int usedCpu) {
		this.usedCpu = usedCpu;
	}
	public long getTotalMemeory() {
		return totalMemeory;
	}
	public void setTotalMemeory(long totalMemeory) {
		this.totalMemeory = totalMemeory;
	}
	public long getFreeMemory() {
		return freeMemory;
	}
	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}
	public long getUsedMemory() {
		return usedMemory;
	}
	public void setUsedMemory(long usedMemory) {
		this.usedMemory = usedMemory;
	}
}
