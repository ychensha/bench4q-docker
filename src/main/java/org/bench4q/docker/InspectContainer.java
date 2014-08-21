package org.bench4q.docker;

import java.util.List;
import java.util.Map;

public class InspectContainer {
	private List<String> args;
	private Config config;
	private String created;
	private String driver;
	private String execDriver;
	private String hostnamePath;
	private String hostsPath;
	private String id;
//	private String path;
//	private State state;
//	private String image;
//	private NetworkSettings networkSettings;
//	private String sysInitPath;
//	private String resolvConfPath;
//	private Volumes volumes;
	private HostConfig hostConfig;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public HostConfig getHostConfig() {
		return hostConfig;
	}
	public void setHostConfig(HostConfig hostConfig) {
		this.hostConfig = hostConfig;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}
}

class Config{
	private String hostname;
	private String user;
	private int memory;
	private int memorySwap;
	private boolean attachStdin;
	private boolean attachStdout;
	private boolean attachStderr;
	//private Map<String, String> portSpecs;
	private boolean tty;
	private boolean openStdin;
	private boolean stdinOnce;
	//private List<String> env;
	//private List<String> cmd;
	private String dns;
	private String image;
	private Volumes volumes;
	
	private String workingDir;
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
	//private Map<String, String> portMapping;
}

class HostConfig{
	private List<String> binds;
	private String containerIDFile;
	private List<String> dns;
	private List<String> dnsSearch;
	private List<String> links;
	private List<String> lxcConf;
	private String networkMode;
	private PortBindings portBindings;
	private boolean privileged;
//	private String volumesFrom;
	public PortBindings getPortBindings() {
		return portBindings;
	}
	public void setPortBindings(PortBindings portBindings) {
		this.portBindings = portBindings;
	}
}