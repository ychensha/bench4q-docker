package org.bench4q.docker.service;

import java.io.IOException;
import java.util.Properties;

public class Main {
	private static int port = 6566;
	
	public static void main(String[] args){
		Properties prop = new Properties();
		try {
			prop.load(Main.class.getClassLoader().getResourceAsStream("docker-service.properties"));
			port = Integer.valueOf(prop.getProperty("MAINFRAME_HOST_PORT", "5656"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		DockerServer server = new DockerServer(port);
		server.start();
	}
}
