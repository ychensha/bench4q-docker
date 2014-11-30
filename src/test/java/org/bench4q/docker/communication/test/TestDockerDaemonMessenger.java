package org.bench4q.docker.communication.test;

import java.lang.reflect.Field;

import org.bench4q.docker.communication.DockerDaemonMessenger;
import org.bench4q.docker.communication.impl.DockerDaemonMessengerImpl;
import org.bench4q.docker.model.CreateContainer;
import org.bench4q.docker.model.DockerDaemonPort;
import org.bench4q.docker.model.InspectContainer;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;

public class TestDockerDaemonMessenger extends TestCase{
	private DockerDaemonMessenger messenger;
	private HttpResponse mockCreateContainerResponse;
	private HttpResponse mockStartContainerResponse;
	private HttpResponse mockKillContainerResponse;
	private HttpResponse mockRemoveContainerResponse;
	
	private void mockCreateResponse(HttpRequester httpRequester){
		
	}
	
	public void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
		messenger = new DockerDaemonMessengerImpl();
		DockerDaemonPort daemonPort = new DockerDaemonPort();
		daemonPort.setHostIp("133.133.134.153");
		daemonPort.setHostPort("2375");
		messenger.setDockerDaemonPort(daemonPort);
		
		HttpRequester httpRequester = new HttpRequester();
		Field messengerRequester = DockerDaemonMessengerImpl.class.getDeclaredField("httpRequester");
		messengerRequester.setAccessible(true);
		messengerRequester.set(messenger, httpRequester);
	}
	
	public void StestCreateAndInspectContainer(){
		CreateContainer createContainer = new CreateContainer();
		createContainer.setImage("chensha/docker");
		String id = messenger.createContainer(createContainer);
		assertNotNull(id);
		System.out.println(id);
		InspectContainer inspectContainer = messenger.inspectContainer(id);
		assertNotNull(inspectContainer);
	}
	
	public void testRemoveContainer(){
		boolean result = messenger.removeContainer("3751");
		assertTrue(result);
	}
	
	public void testStartContainer(){
		
	}
	
	public void testListContainer(){
	}
}
