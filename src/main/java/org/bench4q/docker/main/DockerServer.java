package org.bench4q.docker.main;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.servlet.DispatcherServlet;

public class DockerServer {
	private Server server;
	private int port;

	private Server getServer() {
		return server;
	}

	private void setServer(Server server) {
		this.server = server;
	}

	private int getPort() {
		return port;
	}

	private void setPort(int port) {
		this.port = port;
	}

	public DockerServer(int port) {
		this.setPort(port);
	}

	public boolean start() {
		try {
			this.setServer(new Server(this.getPort()));
			ServletContextHandler servletContextHandler = new ServletContextHandler();
			ServletHolder servletHolder = servletContextHandler.addServlet(
					DispatcherServlet.class, "/");
			servletHolder
					.setInitParameter("contextConfigLocation",
							"classpath*:/application-context.xml");
			servletHolder.setInitOrder(1);
			this.getServer().setHandler(servletContextHandler);
			this.getServer().start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean stop() {
		try {
			if (this.getServer() != null) {
				this.getServer().stop();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			this.setServer(null);
		}
	}
}
