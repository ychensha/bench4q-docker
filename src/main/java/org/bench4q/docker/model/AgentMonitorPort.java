package org.bench4q.docker.model;

public enum AgentMonitorPort {
	AGENT_PORT("6565/tcp"), MONITOR_PORT("5556/tcp");
	private String port;
	private AgentMonitorPort(String port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		return this.port;
	}
}
