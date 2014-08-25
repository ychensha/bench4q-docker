package org.bench4q.docker;

import java.util.List;
import java.util.Map;

public class InspectContainer extends Container {
	private List<String> args;
	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}
	
	public String getHostPort(){
		try{
			return getHostConfig().getPortBindings().getHostPortList().get(0).getHostPort();
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
	private String ipAddress;
	private int ipPrefixLen;
	private String gateway;
	private String bridge;
	private Map<String, String> portMapping;
}

