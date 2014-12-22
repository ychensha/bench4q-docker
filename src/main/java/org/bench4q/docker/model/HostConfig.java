package org.bench4q.docker.model;

public class HostConfig {
	private PortBindings portBindings;
	
	public PortBindings getPortBindings() {
		return portBindings;
	}
	public void setPortBindings(PortBindings portBindings) {
		this.portBindings = portBindings;
	}
}
