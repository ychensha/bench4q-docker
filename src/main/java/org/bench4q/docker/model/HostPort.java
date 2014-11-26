package org.bench4q.docker.model;

public class HostPort {
	private String hostIp;
	private String hostPort;

	public HostPort(){
		
	}
	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}
	
	public HostPort(String port){
		hostPort = port;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}
}