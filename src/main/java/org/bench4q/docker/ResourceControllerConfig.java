package org.bench4q.docker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.thoughtworks.xstream.XStream;

public class ResourceControllerConfig {
	private String dockerHostIp;
	private String dockerHostPort;
	private int vcpuRatio;
	private String imageName;
	
	public String getDockerHostPort() {
		return dockerHostPort;
	}

	public void setDockerHostPort(String dockerHostPort) {
		this.dockerHostPort = dockerHostPort;
	}

	public String getDockerHostIp() {
		return dockerHostIp;
	}

	public void setDockerHostIp(String dockerHostIp) {
		this.dockerHostIp = dockerHostIp;
	}

	public int getVcpuRatio() {
		return vcpuRatio;
	}

	public void setVcpuRatio(int vcpuRatio) {
		this.vcpuRatio = vcpuRatio;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public static void main(String[] args){
		XStream xStream = new XStream();
		ResourceControllerConfig config = new ResourceControllerConfig();
		OutputStream out = null;
		config.setDockerHostIp("133.133.134.74");
		config.setDockerHostPort("2375");
		config.setImageName("chensha/bench4q-agent-test");
		config.setVcpuRatio(3);
		
		try {
			out = new FileOutputStream("./config.xml");
			xStream.toXML(config, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
