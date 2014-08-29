package org.bench4q.docker;

public class RequestResource {
	private long cpuNumber;
	private long memoryLimit;	//measured by Byte 
	private long uploadBandWidthKBits = 40;
	private long downloadBandWidthKBits = 80;
	public long getUploadBandWidthKBits() {
		return uploadBandWidthKBits;
	}
	public void setUploadBandWidthKBits(long uploadBandWidthKBits) {
		this.uploadBandWidthKBits = uploadBandWidthKBits;
	}
	public long getDownloadBandWidthKBits() {
		return downloadBandWidthKBits;
	}
	public void setDownloadBandWidthKBits(long downloadBandWidthKBits) {
		this.downloadBandWidthKBits = downloadBandWidthKBits;
	}
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
