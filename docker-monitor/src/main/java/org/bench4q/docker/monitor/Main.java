package org.bench4q.docker.monitor;

import java.io.IOException;
import java.util.Properties;


public class Main {
	private static int port = 5556;
	
	public static void main(String[] args){
		Properties prop = new Properties();
		try {
			prop.load(Main.class.getClassLoader().getResourceAsStream("docker-monitor.properties"));
			if(prop != null){
				port = Integer.valueOf(prop.getProperty("MONITOR_HOST_PORT", "5556"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		MonitorServer server = new MonitorServer(port);
		server.start();
	}
}
