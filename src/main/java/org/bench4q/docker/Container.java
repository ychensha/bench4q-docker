package org.bench4q.docker;

import java.util.List;
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

public class Container {
	private String id;
	private String port;
	
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

	public static void main(String[] args){
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
