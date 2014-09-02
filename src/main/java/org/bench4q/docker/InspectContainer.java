package org.bench4q.docker;

import java.util.Map;

public class InspectContainer extends Container {
	private NetworkSettings networkSettings;
	public String getHostPort(){
		try{
			return networkSettings.getPorts().getHostPortList().get(0).getHostPort();
		} catch(NullPointerException e){
			return "";
		}
	}
}

class Volumes{
	
}

class State{
	private boolean running;
	private int pid;
	private int exitCode;
	private String startedAt;
	private boolean ghost;
}

class NetworkSettings{
	private PortBindings ports;

	public PortBindings getPorts() {
		return ports;
	}

	public void setPorts(PortBindings ports) {
		this.ports = ports;
	}
}

