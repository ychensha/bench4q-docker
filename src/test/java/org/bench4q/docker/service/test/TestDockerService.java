package org.bench4q.docker.service.test;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.bench4q.docker.communication.DockerDaemonMessenger;
import org.bench4q.docker.model.CreateContainer;
import org.bench4q.docker.model.InspectContainer;
import org.bench4q.docker.model.StartContainer;
import org.bench4q.docker.service.DockerService;
import org.bench4q.docker.service.ResourceNode;
import org.bench4q.share.master.test.resource.AgentModel;
import org.bench4q.share.master.test.resource.ResourceInfoModel;
import org.bench4q.share.master.test.resource.TestResourceModel;

import static org.easymock.EasyMock.*;
import junit.framework.TestCase;

public class TestDockerService extends TestCase{
	private ResourceNode mockResourceNode;
	private DockerDaemonMessenger mockDaemonMessenger;
	private InspectContainer mockInspectContainer;
	private DockerService dockerService;
	private ResourceInfoModel requestResource;
	
	public void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
		dockerService = new DockerService();
		mockResourceNode = createMock(ResourceNode.class);
		TestResourceModel currentStatus = new TestResourceModel();
		//4core 4GB memory
		currentStatus.setFreeCpu(12);
		currentStatus.setTotalCpu(12);
		currentStatus.setFreeMemory((long)3.5 * 1024 * 1024);
		currentStatus.setTotalMemory(4 * 1024 * 1024);
		currentStatus.setTotalDownloadBandwidthKB(100 * 1000);
		currentStatus.setTotalUploadBandwidthKB(100 * 1000);
		currentStatus.setFreeDownloadBandwidthKB(100 * 1000);
		currentStatus.setFreeUploadBandwidthKB(100 * 1000);
		expect(mockResourceNode.getCurrentStatus()).andReturn(currentStatus);
		
		requestResource = new ResourceInfoModel();
		Integer[] cpu = new Integer[] {0,1,2,3};
		requestResource.setCpuSet(Arrays.asList(cpu));
		requestResource.setMemoryKB(512 * 1024);
		requestResource.setImageName("chensha/docker");
		requestResource.setvCpuRatio(3);
		expect(mockResourceNode.requestResource(isA(ResourceInfoModel.class))).andReturn(requestResource);
		replay(mockResourceNode);
		Field resourceNodeField = DockerService.class.getDeclaredField("resourceNode");
		resourceNodeField.setAccessible(true);
		resourceNodeField.set(dockerService, mockResourceNode);
		
		mockInspectContainer = createMock(InspectContainer.class);
		expect(mockInspectContainer.getAgentPort()).andReturn("8080");
		expect(mockInspectContainer.getMonitorPort()).andReturn("8081");
		replay(mockInspectContainer);
		
		mockDaemonMessenger = createMock(DockerDaemonMessenger.class);
		expect(mockDaemonMessenger.createContainer(isA(CreateContainer.class))).andReturn("container1");
		expect(mockDaemonMessenger.startContainer(isA(StartContainer.class))).andReturn(true);
		expect(mockDaemonMessenger.inspectContainer("container1")).andReturn(mockInspectContainer);
		replay(mockDaemonMessenger);
		Field messengerField = DockerService.class.getDeclaredField("dockerDaemonMessenger");
		messengerField.setAccessible(true);
		messengerField.set(dockerService, mockDaemonMessenger);
	}
	
	public void testCreateTestContainer(){
		AgentModel agent = dockerService.createTestContainer(requestResource);
		assertNotNull(agent);
		assertEquals("container1", agent.getId());
		assertEquals("8080", agent.getPort());
		assertEquals("8081", agent.getMonitorPort());
	}
	
	public void testRemove(){
	}
}
