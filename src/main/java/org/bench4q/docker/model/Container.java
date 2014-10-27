package org.bench4q.docker.model;

public class Container {
	private String ip;
	private String port;
	private String id;
	private String image;
	private String monitorPort;
	
	private HostConfig hostConfig;
	private ContainerConfig config;
	
	
	public String getMonitorPort() {
		return monitorPort;
	}

	public void setMonitorPort(String monitorPort) {
		this.monitorPort = monitorPort;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public HostConfig getHostConfig() {
		return hostConfig;
	}

	public void setHostConfig(HostConfig hostConfig) {
		this.hostConfig = hostConfig;
	}

	public ContainerConfig getConfig() {
		return config;
	}

	public void setConfig(ContainerConfig config) {
		this.config = config;
	}
}
