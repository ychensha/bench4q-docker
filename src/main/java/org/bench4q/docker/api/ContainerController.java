package org.bench4q.docker.api;


import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.bench4q.docker.Container;
import org.bench4q.docker.RequestResource;
import org.bench4q.docker.Resource;
import org.bench4q.docker.TestResourceController;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.helper.MarshalHelper;
import org.bench4q.share.master.test.resource.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/docker")
public class ContainerController {
	private static final TestResourceController controller = new TestResourceController();

	public static void main(String[] args) {
		ResourceInfo requiredResource = new ResourceInfo();
		requiredResource.setCpu(2);
		requiredResource.setMemroyKB(512 * 1024);// 256MB
		HttpRequester httpRequester = new HttpRequester();
		HttpResponse response;
		try {
			response = httpRequester.sendPostXml(
					"localhost:6666/docker/create", MarshalHelper.marshal(
							ResourceInfo.class, requiredResource), null);
			AgentModel agent = (AgentModel) MarshalHelper.unmarshal(AgentModel.class,
					response.getContent());
			if (agent != null) {
				System.out.println(agent.getId());
				System.out.println(agent.getHostName());
				System.out.println(agent.getPort());
				System.out.println(agent.getMonitorPort());
			} else {
				System.out.println("fail");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/currentresource", method = RequestMethod.GET)
	@ResponseBody
	public TestResourceModel getCurrentResourceStatus() {
		return setTestResource(controller.getCurrentResourceStatus());
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public AgentModel createContainer(@RequestBody ResourceInfo resource) {
		AgentModel result = setAgentCreated(controller
				.createContainerAndSetCpuQuota(setRequestResource(resource)));
		if(result != null)
			result.setResourceInfo(resource);
		return result;
	}

	@RequestMapping(value = "/remove")
	@ResponseBody
	public MainFrameResponseModel removeContainer(@RequestBody AgentModel agent) {
		MainFrameResponseModel result = new MainFrameResponseModel();
		result.setSuccess(controller.remove(getContainerByAgent(agent)));
		
		return result;
	}

	private TestResourceModel setTestResource(Resource resource) {
		TestResourceModel testResource = new TestResourceModel();
		testResource.setTotalCpu(resource.getTotalCpu());
		testResource.setFreeCpu(resource.getFreeCpu());
		testResource.setUsedCpu(resource.getUsedCpu());
		testResource.setTotalMemeory(resource.getTotalMemeory());
		testResource.setFreeMemory(resource.getFreeMemory());
		testResource.setUsedMemory(resource.getUsedMemory());
		return testResource;
	}

	private RequestResource setRequestResource(ResourceInfo resource) {
		RequestResource requestResource = new RequestResource();
		requestResource.setCpuNumber(resource.getCpu());
		requestResource.setMemoryLimitKB(resource.getMemroyKB());
		requestResource.setDownloadBandwidthKBit(resource
				.getDownloadBandwidthKbit());
		requestResource.setUploadBandwidthKBit(resource
				.getUploadBandwidthKbit());
		return requestResource;
	}

	private AgentModel setAgentCreated(Container container) {
		if(container == null)
			return null;
		
		AgentModel agent = new AgentModel();
		agent.setHostName(container.getIp());
		if(Integer.valueOf(container.getPort()).equals(""))
			agent.setPort(0);
		else {
			agent.setPort(Integer.valueOf(container.getPort()));
		}
		if(Integer.valueOf(container.getMonitorPort()).equals(""))
			agent.setMonitorPort(0);
		else {
			agent.setMonitorPort(Integer.valueOf(container.getMonitorPort()));
		}
		agent.setId(container.getId());
		return agent;
	}

	private Container getContainerByAgent(AgentModel agent) {
		return controller.inspectContainer(agent.getId());
	}
}
