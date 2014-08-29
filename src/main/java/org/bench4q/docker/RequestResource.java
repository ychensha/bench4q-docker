package org.bench4q.docker;

public class RequestResource {
	private long cpuNumber;
	private long memoryLimitKB;	//measured by Byte 
	private long uploadBandwidthKbit = 0;
	private long downloadBandwidthKBit = 0;
	public long getUploadBandwidthKBit() {
		return uploadBandwidthKbit;
	}
	public void setUploadBandwidthKBit(long uploadBandwidthKbit) {
		this.uploadBandwidthKbit = uploadBandwidthKbit;
	}
	public long getDownloadBandwidthKBit() {
		return downloadBandwidthKBit;
	}
	public void setDownloadBandwidthKBit(long downloadBandwidthKBit) {
		this.downloadBandwidthKBit = downloadBandwidthKBit;
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
