package org.bench4q.docker;

public class RequestResource {
	private long cpuNumber;
	private long memoryLimitKB;	//measured by Byte 
	private long uploadBandwidthKByte = 0;
	private long downloadBandwidthKByte = 0;
	public long getUploadBandwidthKByte() {
		return uploadBandwidthKByte;
	}
	public void setUploadBandwidthKByte(long uploadBandwidthKByte) {
		this.uploadBandwidthKByte = uploadBandwidthKByte;
	}
	public long getDownloadBandwidthKByte() {
		return downloadBandwidthKByte;
	}
	public void setDownloadBandwidthKByte(long downloadBandwidthKByte) {
		this.downloadBandwidthKByte = downloadBandwidthKByte;
	}
	public long getMemoryLimitKB() {
		return memoryLimitKB;
	}
	public void setMemoryLimitKB(long memoryLimit) {
		this.memoryLimitKB = memoryLimit;
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
