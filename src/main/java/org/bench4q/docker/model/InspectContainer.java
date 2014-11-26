package org.bench4q.docker.model;

import java.util.List;
import java.util.Map;



public class InspectContainer{
	private NetworkSettings networkSettings;
	public String getAgentPort(){
		try{
			return networkSettings.getPorts().get(AgentMonitorPort.AGENT_PORT.toString()).get(0).getHostPort();
		} catch(NullPointerException e){
			return "";
		}
	}
	
	public String getMonitorPort(){
		try{
			return networkSettings.getPorts().get(AgentMonitorPort.MONITOR_PORT.toString()).get(0).getHostPort();
		} catch(NullPointerException e){
			return "";
		}
	}
}

class State{
	private boolean running;
	private int pid;
	private int exitCode;
	private String startedAt;
	private boolean ghost;
}

class NetworkSettings{
	private Map<String, List<HostPort>> ports;

	public Map<String, List<HostPort>> getPorts() {
		return ports;
	}

	public void setPorts(Map<String, List<HostPort>> ports) {
		this.ports = ports;
	}
}

