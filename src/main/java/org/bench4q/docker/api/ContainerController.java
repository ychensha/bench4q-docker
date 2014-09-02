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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/docker")
public class ContainerController {
	private static final TestResourceController controller = new TestResourceController();

	public static void main(String[] args) {
		RequiredResource requiredResource = new RequiredResource();
		requiredResource.setCpu(2);
		requiredResource.setMemroyKB(512 * 1024);// 256MB
		HttpRequester httpRequester = new HttpRequester();
		try {
			HttpResponse response = httpRequester.sendPostXml(
					"localhost:5656/docker/create", MarshalHelper.marshal(
							RequiredResource.class, requiredResource), null);
			Agent agent = (Agent) MarshalHelper.unmarshal(Agent.class,
					response.getContent());
			if (agent != null) {
				System.out.println(agent.getId());
				System.out.println(agent.getHostName());
				System.out.println(agent.getPort());
			} else {
				System.out.println("fail");
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/currentresource", method = RequestMethod.GET)
	@ResponseBody
	public TestResource getCurrentResourceStatus() {
		return setTestResource(controller.getCurrentResourceStatus());
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public Agent createContainer(@RequestBody RequiredResource resource) {
		return setAgentCreated(controller
				.createContainer(setRequestResource(resource)));
	}

	@RequestMapping(value = "/remove")
	public void removeContainer(@RequestParam Agent agent) {
		controller.remove(getContainerByAgent(agent));
	}

	private TestResource setTestResource(Resource resource) {
		TestResource testResource = new TestResource();
		testResource.setTotalCpu(resource.getTotalCpu());
		testResource.setFreeCpu(resource.getFreeCpu());
		testResource.setUsedCpu(resource.getUsedCpu());
		testResource.setTotalMemeory(resource.getTotalMemeory());
		testResource.setFreeMemory(resource.getFreeMemory());
		testResource.setUsedMemory(resource.getUsedMemory());
		return testResource;
	}

	private RequestResource setRequestResource(RequiredResource resource) {
		RequestResource requestResource = new RequestResource();
		requestResource.setCpuNumber(resource.getCpu());
		requestResource.setMemoryLimitKB(resource.getMemroyKB());
		requestResource.setDownloadBandwidthKBit(resource
				.getDownloadBandwidthKbit());
		requestResource.setUploadBandwidthKBit(resource
				.getUploadBandwidthKbit());
		return requestResource;
	}

	private Agent setAgentCreated(Container container) {
		if(container == null)
			return null;
		
		Agent agent = new Agent();
		agent.setHostName(container.getIp());
		agent.setPort(Integer.valueOf(container.getPort()));
		agent.setId(container.getId());
		return agent;
	}

	private Container getContainerByAgent(Agent agent) {
		return controller.inspectContainer(agent.getId());
	}
}
