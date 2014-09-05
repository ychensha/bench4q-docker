package org.bench4q.docker;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;


class HostPort{
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

class PortBindings{
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
class CreateContainerResponse{
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}

public class Container {
	private String ip;
	private String port;
	private String id;
	private String image;
	private String monitorPort;
	
	private HostConfig hostConfig;
	private Config config;
	
	
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

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
}

class Config{
	private String hostname;
	private String user;
	private int cpuShares;
	private int memory;
	private int memorySwap;
	private String cpuset;
	private boolean attachStdin;
	private boolean attachStdout;
	private boolean attachStderr;
	private boolean tty;
	private boolean openStdin;
	private boolean stdinOnce;
	private List<String> env;
	private List<String> cmd;
	private String dns;
	private String image;
	private Volumes volumes;
	
	private String workingDir;

	public int getCpuShares() {
		return cpuShares;
	}

	public void setCpuShares(int cpuShares) {
		this.cpuShares = cpuShares;
	}

	public String getCpuset() {
		return cpuset;
	}

	public void setCpuset(String cpuset) {
		this.cpuset = cpuset;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}
}

class HostConfig{
	private PortBindings portBindings;
	private boolean privileged;
	public PortBindings getPortBindings() {
		return portBindings;
	}
	public void setPortBindings(PortBindings portBindings) {
		this.portBindings = portBindings;
	}
}
