package org.bench4q.docker.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class PortBindings{
	@SerializedName("6565/tcp")
	private List<HostPort> hostPortList;
	
	@SerializedName("5556/tcp")
	private List<HostPort> monitorPort;
	
	public PortBindings(List<String> ports){
		hostPortList = new ArrayList<HostPort>();
		monitorPort = new ArrayList<HostPort>();
		for (String port : ports) {
			hostPortList.add(new HostPort(port));
			monitorPort.add(new HostPort(port));
		}
	}

	public List<HostPort> getHostPortList() {
		return hostPortList;
	}

	public void setHostPortList(List<HostPort> hostPortList) {
		this.hostPortList = hostPortList;
	}

	public List<HostPort> getMonitorPort() {
		return monitorPort;
	}

	public void setMonitorPort(List<HostPort> monitorPort) {
		this.monitorPort = monitorPort;
	}
}