package org.bench4q.docker.model;

import org.springframework.stereotype.Component;

@Component
public class DockerDaemonPort {
	private String hostIp;
	private String hostPort;

	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}
}
