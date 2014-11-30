package org.bench4q.docker.communication;

import java.util.List;

import org.bench4q.docker.model.Container;
import org.bench4q.docker.model.CreateContainer;
import org.bench4q.docker.model.DockerDaemonPort;
import org.bench4q.docker.model.InspectContainer;
import org.bench4q.docker.model.StartContainer;

public interface DockerDaemonMessenger {
	public void setDockerDaemonPort(DockerDaemonPort daemonHostPort);
	public String createContainer(CreateContainer createContainer);
	public boolean startContainer(StartContainer startContainer);
	public boolean killContainer(String id);
	public boolean removeContainer(String id);
	public InspectContainer inspectContainer(String id);
	public List<Container> listContainer();
}
