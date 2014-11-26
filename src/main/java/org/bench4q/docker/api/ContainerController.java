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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/docker")
public class ContainerController {
	@Autowired
	private DockerApi controller;
	@Autowired
	private HttpRequester httpRequester;
	@Autowired
	private ResourceNode resourceNode;

	private String buildAgentMonitorUrl(AgentModel agent) {
		return agent.getHostName() + ":" + agent.getMonitorPort();
	}

	public void postResourInfo(AgentModel agent, ResourceInfoModel resourceInfo) {
		try {
			HttpResponse response = httpRequester.sendPostXml(
					buildAgentMonitorUrl(agent) + "/monitor/setResourceInfo",
					MarshalHelper
							.marshal(ResourceInfoModel.class, resourceInfo),
					null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/createTestContainer", method = RequestMethod.POST)
	@ResponseBody
	public MainFrameDockerResponseModel createTestContainer(
			@RequestBody ResourceInfoModel resource) {
		if (!isParamLegal(resource)) {
			return setResponseModel(false, "param illegal", null);
		}
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
		return resourceNode.getCurrentStatus();
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public MainFrameDockerResponseModel createContainer(
			@RequestBody ResourceInfoModel resource) {
		if (!isParamLegal(resource)) {
			return setResponseModel(false, "param illegal", null);
		}
		AgentModel agentModel = controller.createContainer(resource);
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

	private boolean isParamLegal(ResourceInfoModel model) {
		boolean result = true;
		if (model.getCpu() <= 0 || model.getCmds() == null
				|| model.getImageName() == null || model.getMemoryKB() <= 0
				|| model.getDownloadBandwidthKByte() <= 0
				|| model.getUploadBandwidthKByte() <= 0)
			result = false;
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
