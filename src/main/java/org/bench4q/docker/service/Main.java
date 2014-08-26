package org.bench4q.docker.service;

public class Main {
	private static int port = 6566;
	
	public static void main(String[] args){
		DockerServer server = new DockerServer(port);
		server.start();
	}
}
