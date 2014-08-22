package org.bench4q.docker;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bench4q.docker.CreateContainer;


/*
 * define inner class HostPort 
 */
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
	
	public PortBindings(List<String> ports){
		hostPortList = new ArrayList<HostPort>();
		for (String port : ports) {
			hostPortList.add(new HostPort(port));
		}
	}

	public List<HostPort> getHostPortList() {
		return hostPortList;
	}

	public void setHostPortList(List<HostPort> hostPortList) {
		this.hostPortList = hostPortList;
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
	private String port;
	private Config config;
	private String created;
	private String hostnamePath;
	private String hostsPath;
	private String id;
	private String path;
	private State state;
	private String image;
	private NetworkSettings networkSettings;
	private HostConfig hostConfig;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
	private Map<String, String> portSpecs;
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
	private String volumesFrom;
	public PortBindings getPortBindings() {
		return portBindings;
	}
	public void setPortBindings(PortBindings portBindings) {
		this.portBindings = portBindings;
	}
}
