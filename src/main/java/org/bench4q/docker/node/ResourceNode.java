package org.bench4q.docker.node;


import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bench4q.share.master.test.resource.AgentModel;
import org.bench4q.share.master.test.resource.ResourceInfoModel;
import org.bench4q.share.master.test.resource.TestResourceModel;

public class ResourceNode {
	private DockerBlotter dockerBlotter;
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private static volatile ResourceNode instance = new ResourceNode();

	private ResourceNode() {
		cleanUp();
		dockerBlotter = new DockerBlotter();
		dockerBlotter.init();
	}

	public static ResourceNode getInstance() {
		return instance;
	}
	
	private void cleanUp(){
		DockerApi testResourceController = new DockerApi();
		List<AgentModel> runningContainerList = testResourceController
				.getContainerList();
		for(AgentModel container : runningContainerList){
			testResourceController.remove(container);
		}
	}

	public ResourceInfoModel requestResource(ResourceInfoModel resource) {
		if(resource == null)
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
