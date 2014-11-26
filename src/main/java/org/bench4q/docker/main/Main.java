package org.bench4q.docker.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
	private static int port = 5656;
	
	public static void main(String[] args){
		Properties prop = new Properties();
		try {
			InputStream is = Main.class.getClassLoader().getResourceAsStream("docker-service.properties");
			if(is != null){
				prop.load(is);
				port = Integer.valueOf(prop.getProperty("MAINFRAME_HOST_PORT", "5656"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		DockerServer server = new DockerServer(port);
		server.start();
	}
}
