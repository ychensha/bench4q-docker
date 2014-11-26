package org.bench4q.docker.node;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;

import org.bench4q.share.master.test.resource.AgentModel;
import org.bench4q.share.master.test.resource.ResourceInfoModel;
import org.bench4q.share.master.test.resource.TestResourceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceNode {
	private DockerBlotter dockerBlotter;
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	@Autowired
	private DockerApi dockerApi;

	@PostConstruct
	public void init() {
		cleanUp();
		dockerBlotter = new DockerBlotter();
		dockerBlotter.init();
	}

	private void cleanUp() {
		List<AgentModel> runningContainerList = dockerApi.getAgentList();
		for (AgentModel container : runningContainerList) {
			dockerApi.remove(container);
		}
	}

	public ResourceInfoModel requestResource(ResourceInfoModel resource) {
		if (resource == null)
			return resource;

		ResourceInfoModel result = null;
		lock.writeLock().lock();
		try {
			result = dockerBlotter.requestResource(resource);
		} finally {
			lock.writeLock().unlock();
		}
		return result;
	}

	public void releaseResource(AgentModel agent) {
		lock.writeLock().lock();
		try {
			dockerBlotter.releaseResource(agent.getResourceInfo());
		} finally {
			lock.writeLock().unlock();
		}
	}

	public TestResourceModel getCurrentStatus() {
		TestResourceModel result = null;
		lock.readLock().lock();
		try {
			result = dockerBlotter.getCurrentStatus();
		} finally {
			lock.readLock().unlock();
		}
		return result;
	}
}
