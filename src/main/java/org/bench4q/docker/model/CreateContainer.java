package org.bench4q.docker.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;


public class CreateContainer {
//	private long memory;
//	private long memorySwap;
	private String cpuset;
	private boolean attachStdin = false;
	private boolean attachStdout = false;
	private boolean attachStderr = false;
	private ExposedPort exposedPorts;
	private String image;
	private List<String> cmd;
	
	public List<String> getCmd() {
		return cmd;
	}

	public void setCmd(List<String> cmd) {
		this.cmd = cmd;
	}

	public String getCpuset() {
		return cpuset;
	}

	public void setCpuset(String cpuset) {
		this.cpuset = cpuset;
	}

	public void setImage(String image) {
		this.image = image;
	}
	
	public String getImage(){
		return image;
	}

//	public long getMemoryByte() {
//		return memory;
//	}
//
//	public void setMemoryByte(long memory) {
//		this.memory = memory;
//	}
//
//	public long getMemorySwap() {
//		return memorySwap;
//	}
//
//	public void setMemorySwap(long memorySwap) {
//		this.memorySwap = memorySwap;
//	}

	public boolean isAttachStdin() {
		return attachStdin;
	}

	public void setAttachStdin(boolean attachStdin) {
		this.attachStdin = attachStdin;
	}

	public boolean isAttachStdout() {
		return attachStdout;
	}

	public void setAttachStdout(boolean attachStdout) {
		this.attachStdout = attachStdout;
	}

	public boolean isAttachStderr() {
		return attachStderr;
	}

	public void setAttachStderr(boolean attachStderr) {
		this.attachStderr = attachStderr;
	}

	public ExposedPort getExposedPorts() {
		return exposedPorts;
	}

	public void setExposedPorts(List<String> exposedPorts) {
		this.exposedPorts = new ExposedPort(exposedPorts);
	}

	public static class ExposedPort{
		@SerializedName("6565/tcp")
		private List<HostPort> hostPortList;

		
		public List<HostPort> getHostPortList() {
			return hostPortList;
		}

		public void setHostPortList(List<HostPort> hostPortList) {
			this.hostPortList = hostPortList;
		}

		public ExposedPort(List<String> ports){
			hostPortList = new ArrayList<HostPort>();
			for (String port : ports) {
				hostPortList.add(new HostPort(port));
			}
		}
	}
}
