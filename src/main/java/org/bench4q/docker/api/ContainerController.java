package org.bench4q.docker.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.bench4q.docker.Container;
import org.bench4q.docker.RequestResource;
import org.bench4q.docker.Resource;
import org.bench4q.docker.TestResourceController;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.helper.MarshalHelper;
import org.bench4q.share.master.test.resource.*;
import org.bench4q.share.models.mainframe.MainFrameDockerResponseModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/docker")
public class ContainerController {
	private static final TestResourceController controller = new TestResourceController();
	private HttpRequester httpRequester = new HttpRequester();

	private String buildBaseUrl() {
		return "133.133.134.153:5656/docker";
	}

	public void removeContainers(List<AgentModel> agents) {
		if (agents != null) {
			for (AgentModel agent : agents) {
				try {
					HttpResponse response = httpRequester.sendPostXml(
							buildBaseUrl() + "/remove",
							MarshalHelper.marshal(AgentModel.class, agent), null);
					MainFrameResponseModel model = (MainFrameResponseModel) MarshalHelper
							.unmarshal(MainFrameResponseModel.class,
									response.getContent());
					if (model.isSuccess()) {
						System.out.println("remove " + agent.getId());
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JAXBException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public List<AgentModel> createContainers(List<ResourceInfo> resourceInfoList){
		List<AgentModel> result = new ArrayList<AgentModel>();
		if(resourceInfoList != null){
			for(ResourceInfo resourceInfo : resourceInfoList){
				HttpResponse response;
				try {
					response = httpRequester
							.sendPostXml(
									buildBaseUrl() + "/createTestContainer",
									MarshalHelper.marshal(ResourceInfo.class,
											resourceInfo), null);
					MainFrameDockerResponseModel dockerResponse = (MainFrameDockerResponseModel) MarshalHelper
							.unmarshal(MainFrameDockerResponseModel.class,
									response.getContent());
					if (dockerResponse.isSuccess()) {
						AgentModel agent = dockerResponse.getAgentModel();
						result.add(agent);
						System.out.println(agent.getId());
						System.out.println(agent.getPort());
						System.out.println(agent.getMonitorPort());
					} else {
						System.out.println("create fail");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JAXBException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public static void main(String[] args) {
		ContainerController testController = new ContainerController();
		
		ResourceInfo resourceInfo = new ResourceInfo();
		resourceInfo.setCpu(4);
		resourceInfo.setMemoryKB(512 * 1024);
		resourceInfo.setDownloadBandwidthKByte(0);
		resourceInfo.setUploadBandwidthKByte(0);
		resourceInfo.setImageName("chensha/docker");
		List<String> cmds = new ArrayList<String>();
		cmds.add("/bin/sh");
		cmds.add("-c");
		cmds.add("/opt/monitor/*.sh&&java -jar -server /opt/bench4q-agent-publish/*.java");
//		cmds.add("java -jar /opt/monitor/bench4q-docker-monitor.jar");
//		cmds.add("java -jar -server -Xms1024M -Xmx1024M /opt/bench4q-agent-publish/*.jav&&java -jar /opt/monitor/bench4q-docker-monitor.jar");
		// cmds.add("/opt/empty-jetty/start.sh");
		// cmds.add("java -jar /opt/empty-jetty/empty-jetty-server.jar");
		resourceInfo.setCmds(cmds);
		
		List<ResourceInfo> resourceInfoList = new ArrayList<ResourceInfo>();
		for(int i = 0; i < 1; i++){
			resourceInfoList.add(resourceInfo);
		}
		List<AgentModel> agents = testController.createContainers(resourceInfoList);
		testController.removeContainers(agents);
	}

	@RequestMapping(value = "/createTestContainer", method = RequestMethod.POST)
	@ResponseBody
	public MainFrameDockerResponseModel createTestContainer(
			@RequestBody ResourceInfo resource) {
		System.out.println(MarshalHelper.tryMarshal(resource));
		AgentModel agentModel = setAgentCreated(controller.createTestContainer(
				setRequestResource(resource), resource.getImageName(),
				resource.getCmds()), resource);
		if (agentModel == null) {
			return setResponseModel(false, "docker create container fail", null);
		}
		int response = checkAgent(agentModel);
		if (response == 0) {
			System.out.println("test container is not aval.");
			controller.remove(getContainerByAgent(agentModel));
			return setResponseModel(false, "start agent fail", null);
		}
		System.out.println("test container starts up.");
		return setResponseModel(true, null, agentModel);
	}

	@RequestMapping(value = "/currentresource", method = RequestMethod.GET)
	@ResponseBody
	public TestResourceModel getCurrentResourceStatus() {
		return setTestResource(controller.getCurrentResourceStatus());
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public MainFrameDockerResponseModel createContainer(
			@RequestBody ResourceInfo resource) {
		System.out.println(MarshalHelper.tryMarshal(resource));
		AgentModel agentModel = setAgentCreated(
				controller
						.createContainerAndSetCpuQuota(setRequestResource(resource)),
				resource);
		if (agentModel == null) {
			return setResponseModel(false, "docker create container fail", null);
		}
		int response = checkAgent(agentModel);
		if (response == 0) {
			System.out.println("agent is not aval.");
			controller.remove(getContainerByAgent(agentModel));
			return setResponseModel(false, "start agent fail", null);
		}
		System.out.println("agent starts up.");
		return setResponseModel(true, null, agentModel);
	}

	@RequestMapping(value = "/remove")
	@ResponseBody
	public MainFrameResponseModel removeContainer(@RequestBody AgentModel agent) {
		MainFrameResponseModel result = new MainFrameResponseModel();
		result.setSuccess(controller.remove(getContainerByAgent(agent)));
		return result;
	}

	private MainFrameDockerResponseModel setResponseModel(boolean isSuccess,
			String failCauseString, AgentModel agentModel) {
		MainFrameDockerResponseModel responseModel = new MainFrameDockerResponseModel();
		responseModel.setSuccess(isSuccess);
		responseModel.setFailCauseString(failCauseString);
		responseModel.setAgentModel(agentModel);
		return responseModel;
	}

	private int checkAgent(AgentModel agent) {
		HttpResponse response = null;
		int checkCount = 0;
		long startTime = System.currentTimeMillis();
		int result = 0;
		try {
			while (result == 0) {
				if (checkCount > 30)
					break;
				response = httpRequester.sendGet(agent.getHostName() + ":"
						+ agent.getPort(), null, null);
				// checkCount++;
				// if(response.getCode() != 0)
				// return response.getCode();
				result = response.getCode();
				Thread.currentThread();
				Thread.sleep(1000);
				checkCount++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("check the container takes: "
				+ (float) (endTime - startTime) / 1000 + " s.");
		return result;
	}

	private TestResourceModel setTestResource(Resource resource) {
		TestResourceModel testResource = new TestResourceModel();
		testResource.setTotalCpu(resource.getTotalCpu());
		testResource.setFreeCpu(resource.getFreeCpu());
		testResource.setUsedCpu(resource.getUsedCpu());
		testResource.setTotalMemory(resource.getTotalMemeory());
		testResource.setFreeMemory(resource.getFreeMemory());
		testResource.setUsedMemory(resource.getUsedMemory());
		return testResource;
	}

	private RequestResource setRequestResource(ResourceInfo resource) {
		RequestResource requestResource = new RequestResource();
		requestResource.setCpuNumber(resource.getCpu());
		requestResource.setMemoryLimitKB(resource.getMemoryKB());
		requestResource.setDownloadBandwidthKByte(resource
				.getDownloadBandwidthKByte());
		requestResource.setUploadBandwidthKByte(resource
				.getUploadBandwidthKByte());
		return requestResource;
	}

	private AgentModel setAgentCreated(Container container,
			ResourceInfo resource) {
		if (container == null)
			return null;
		AgentModel agent = new AgentModel();
		agent.setHostName(container.getIp());
		agent.setPort(Integer.valueOf(container.getPort()));
		agent.setMonitorPort(Integer.valueOf(container.getMonitorPort()));
		agent.setId(container.getId());
		agent.setResourceInfo(resource);
		return agent;
	}

	private Container getContainerByAgent(AgentModel agent) {
		return controller.inspectContainer(agent.getId());
	}
}
