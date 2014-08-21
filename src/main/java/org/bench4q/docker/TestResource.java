package org.bench4q.docker;

public class TestResource {
	private long cpuNumber;
	private long memoryLimit;	//measured by Byte 
	public long getMemoryLimit() {
		return memoryLimit;
	}
	public void setMemoryLimit(long memoryLimit) {
		this.memoryLimit = memoryLimit;
	}
	public long getCpuNumber() {
		return cpuNumber;
	}
	public void setCpuNumber(long cpuNumber) {
		this.cpuNumber = cpuNumber;
	}
	
	public static void main(String[] args){
		StringBuilder builder = new StringBuilder();
		builder.append(1 + ",");
		//builder.deleteCharAt(builder.length() - 1);
		System.out.println(builder.toString());
	}
}
