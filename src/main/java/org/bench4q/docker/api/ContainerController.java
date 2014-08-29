package org.bench4q.docker.api;
import org.bench4q.docker.Container;
import org.bench4q.docker.RequestResource;
import org.bench4q.docker.Resource;
import org.bench4q.docker.TestResourceController;
import org.bench4q.share.master.test.resource.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/docker")
public class ContainerController {
	private static final TestResourceController controller = new TestResourceController();
	
	public static void main(String[] args){
//		ContainerController containerController = new ContainerController();
//		RequiredResource requiredResource = new RequiredResource();
//		requiredResource.setCpu(1);
//		requiredResource.setMemroyKB(256*1024);//256MB
//		AgentCreated agent = containerController.createContainer(requiredResource);
//		System.out.println(agent.getId());
//		System.out.println(agent.getIp());
//		System.out.println(agent.getPort());
	}
	
	@RequestMapping(value = "/currentresource", method = RequestMethod.GET)
	@ResponseBody
	public TestResource getCurrentResourceStatus(){
		return setTestResource(controller.getCurrentResourceStatus());
	}
	
	@RequestMapping(value="/create")
	public AgentCreated createContainer(RequiredResource resource){
		return setAgentCreated(controller.createContainer(setRequestResource(resource)));
	}
	
	@RequestMapping(value="/remove")
	public void removeContainer(AgentCreated agent){
		controller.remove(getContainerByAgent(agent));
	}
	
	private TestResource setTestResource(Resource resource){
		TestResource testResource = new TestResource();
		testResource.setTotalCpu(resource.getTotalCpu());
		testResource.setFreeCpu(resource.getFreeCpu());
		testResource.setUsedCpu(resource.getUsedCpu());
		testResource.setTotalMemeory(resource.getTotalMemeory());
		testResource.setFreeMemory(resource.getFreeMemory());
		testResource.setUsedMemory(resource.getUsedMemory());
		return testResource;
	}
	
	private RequestResource setRequestResource(RequiredResource resource){
		RequestResource requestResource = new RequestResource();
		requestResource.setCpuNumber(resource.getCpu());
		requestResource.setMemoryLimit(resource.getMemroyKB());
		requestResource.setDownloadBandwidthKBit(resource.getDownloadBandwidthKbit());
		requestResource.setUploadBandwidthKBit(resource.getUploadBandwidthKbit());
		return requestResource;
	}
	
	private AgentCreated setAgentCreated(Container container){
		AgentCreated agent = new AgentCreated();
		agent.setIp(container.getIp());
		agent.setPort(Integer.valueOf(container.getPort()));
		agent.setId(container.getId());
		return agent;
	}
	
	private Container getContainerByAgent(AgentCreated agent){
		return controller.inspectContainer(agent.getId());
	}
}
