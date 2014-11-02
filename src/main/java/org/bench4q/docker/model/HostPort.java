package org.bench4q.docker.model;

public class HostPort {
	private String hostIp;
	private String hostPort;

	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}
	
	public HostPort(String port){
		hostPort = port;
	}
}