package org.bench4q.docker.api;

import java.io.IOException;

import org.bench4q.docker.service.DockerService;
import org.bench4q.docker.service.ResourceNode;
import org.bench4q.docker.model.CreatedContainerList;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.models.master.AgentModel;
import org.bench4q.share.models.master.MainFrameDockerResponseModel;
import org.bench4q.share.models.master.MainFrameResponseModel;
import org.bench4q.share.models.master.ResourceInfoModel;
import org.bench4q.share.models.master.TestResourceModel;
import org.bench4q.utils.MarshalHelper;
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
	private DockerService controller;
	@Autowired
	private HttpRequester httpRequester;
	@Autowired
	private ResourceNode resourceNode;

	private CreatedContainerList createdContainerList = new CreatedContainerList();


	private String buildAgentMonitorUrl(AgentModel agent) {
		return agent.getHostName() + ":" + agent.getMonitorPort();
	}

	public void postResourInfo(AgentModel agent, ResourceInfoModel resourceInfo) {
		try {
			httpRequester.sendPostXml(
					buildAgentMonitorUrl(agent) + "/monitor/setResourceInfo",
					MarshalHelper
							.marshal(ResourceInfoModel.class, resourceInfo),
					null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/containers")
	@ResponseBody
	public CreatedContainerList getContaiers() {
		return this.createdContainerList;
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
			controller.remove(agentModel);
			return setResponseModel(false, "start agent fail", null);
		}
		postResourInfo(agentModel, resource);
		return setResponseModel(true, "", agentModel);
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
		postResourInfo(agentModel, resource);
		System.out.println("agent starts up.");
		return setResponseModel(true, "", agentModel);
	}

	@RequestMapping(value = "/remove")
	@ResponseBody
	public MainFrameResponseModel removeContainer(@RequestBody AgentModel agent) {
		MainFrameResponseModel result = new MainFrameResponseModel();
		result.setSuccess(controller.remove(agent));
		if (result.isSuccess()) {
			AgentModel agentModelToRemoved = null;
			for (AgentModel agentModel : this.getContaiers().getAgentModels()) {
				if (agentModel.getPort() == agent.getPort()) {
					agentModelToRemoved = agentModel;
				}
			}
			if (agentModelToRemoved != null) {
				this.getContaiers().getAgentModels().remove(agentModelToRemoved);
			}
		}
		return result;
	}

	private boolean isParamLegal(ResourceInfoModel model) {
		boolean result = true;
		if (model.getCpu() <= 0 || model.getCmds() == null
				|| model.getImageName() == null || model.getMemoryKB() < 0
				|| model.getDownloadBandwidthKByte() < 0
				|| model.getUploadBandwidthKByte() < 0)
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
//				response = httpRequester.sendGet(agent.getHostName() + ":"
//						+ agent.getMonitorPort(), null, null);
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
