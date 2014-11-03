package org.bench4q.docker.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.bench4q.docker.node.DockerApi;
import org.bench4q.docker.node.ResourceNode;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.helper.MarshalHelper;
import org.bench4q.share.master.test.resource.*;
import org.bench4q.share.models.mainframe.MainFrameDockerResponseModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/docker")
public class ContainerController {
	private static final DockerApi controller = new DockerApi();
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
							MarshalHelper.marshal(AgentModel.class, agent),
							null);
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

	public List<AgentModel> createContainers(List<ResourceInfoModel> resourceInfoList) {
		List<AgentModel> result = new ArrayList<AgentModel>();
		if (resourceInfoList != null) {
			for (ResourceInfoModel resourceInfo : resourceInfoList) {
				HttpResponse response;
				try {
					response = httpRequester.sendPostXml(buildBaseUrl()
							+ "/createTestContainer", MarshalHelper.marshal(
							ResourceInfoModel.class, resourceInfo), null);
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
		ResourceInfoModel resourceInfo = new ResourceInfoModel();
		resourceInfo.setCpu(4);
		resourceInfo.setMemoryKB(768 * 1024);
		resourceInfo.setDownloadBandwidthKByte(0);
		resourceInfo.setUploadBandwidthKByte(0);
		resourceInfo.setImageName("chensha/docker");
		List<String> cmds = new ArrayList<String>();
		cmds.add("/bin/sh");
		cmds.add("-c");
		cmds.add("/opt/monitor/*.sh&&java -server -jar -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -Xverify:none "
				+ "-XX:PermSize=64m -XX:MaxPermSize=64m -Xms500m -Xms500m -Xmn200m /opt/bench4q-agent-publish/*.jar");
		resourceInfo.setCmds(cmds);
		resourceInfo.setImageName("chensha/docker");
		// cmds.add("java -jar /opt/monitor/bench4q-docker-monitor.jar");
		// cmds.add("java -jar -server -Xms1024M -Xmx1024M /opt/bench4q-agent-publish/*.jav&&java -jar /opt/monitor/bench4q-docker-monitor.jar");
		// cmds.add("/opt/empty-jetty/start.sh");
		// cmds.add("java -jar /opt/empty-jetty/empty-jetty-server.jar");
		resourceInfo.setCmds(cmds);

		List<ResourceInfoModel> resourceInfoList = new ArrayList<ResourceInfoModel>();
		for (int i = 0; i < 1; i++) {
			resourceInfoList.add(resourceInfo);
		}
		List<AgentModel> agents = testController
				.createContainers(resourceInfoList);
		testController.removeContainers(agents);
	}

	private String buildAgentMonitorUrl(AgentModel agent) {
		return agent.getHostName() + ":" + agent.getMonitorPort();
	}

	public void postResourInfo(AgentModel agent, ResourceInfoModel resourceInfo) {
		try {
			System.out.println(buildAgentMonitorUrl(agent)
					+ "/monitor/setResourceInfo");
			HttpResponse response = httpRequester.sendPostXml(
					buildAgentMonitorUrl(agent) + "/monitor/setResourceInfo",
					MarshalHelper.marshal(ResourceInfoModel.class, resourceInfo),
					null);
			System.out.println(response.getCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/createTestContainer", method = RequestMethod.POST)
	@ResponseBody
	public MainFrameDockerResponseModel createTestContainer(
			@RequestBody ResourceInfoModel resource) {
		AgentModel agentModel = controller.createTestContainer(resource);
		if (agentModel == null) {
			return setResponseModel(false, "docker create container fail", null);
		}
		int response = checkAgent(agentModel);
		if (response == 0) {
			System.out.println("test container is not aval.");
			controller.remove(agentModel);
			System.out.println("remove failed container");
			return setResponseModel(false, "start agent fail", null);
		}
		postResourInfo(agentModel, resource);
		System.out.println("test container starts up.");
		return setResponseModel(true, null, agentModel);
	}

	@RequestMapping(value = "/currentresource", method = RequestMethod.GET)
	@ResponseBody
	public TestResourceModel getCurrentResourceStatus() {
		return ResourceNode.getInstance().getCurrentStatus();
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public MainFrameDockerResponseModel createContainer(
			@RequestBody ResourceInfoModel resource) {
		System.out.println(MarshalHelper.tryMarshal(resource));
		AgentModel agentModel = controller
				.createContainerAndSetCpuQuota(resource);
		if (agentModel == null) {
			return setResponseModel(false, "docker create container fail", null);
		}
		int response = checkAgent(agentModel);
		if (response == 0) {
			System.out.println("agent is not aval.");
			controller.remove(agentModel);
			return setResponseModel(false, "start agent fail", null);
		}
		System.out.println("agent starts up.");
		return setResponseModel(true, null, agentModel);
	}

	@RequestMapping(value = "/remove")
	@ResponseBody
	public MainFrameResponseModel removeContainer(@RequestBody AgentModel agent) {
		MainFrameResponseModel result = new MainFrameResponseModel();
		result.setSuccess(controller.remove(agent));
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
}
