package org.bench4q.docker.api.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.helper.MarshalHelper;
import org.bench4q.share.master.test.resource.AgentModel;
import org.bench4q.share.master.test.resource.MainFrameResponseModel;
import org.bench4q.share.master.test.resource.ResourceInfoModel;
import org.bench4q.share.models.mainframe.MainFrameDockerResponseModel;

import junit.framework.TestCase;

public class TestContainerController extends TestCase{
	private HttpRequester httpRequester = new HttpRequester();
	private AgentModel agent;
	private String buildBaseUrl() {
		return "133.133.134.153:5656/docker";
	}
	
	public void testCreateTestContainer(){
		ResourceInfoModel resourceInfo = new ResourceInfoModel();
		resourceInfo.setCpu(4);
		resourceInfo.setMemoryKB(512 * 1024);
		resourceInfo.setDownloadBandwidthKByte(300);
		resourceInfo.setUploadBandwidthKByte(500);
		List<String> cmds = new ArrayList<String>();
		cmds.add("/bin/sh");
		cmds.add("-c");
		cmds.add("/opt/monitor/*.sh&&java -server -jar /opt/bench4q-agent-publish/*.jar");
		resourceInfo.setCmds(cmds);
		resourceInfo.setImageName("chensha/docker");
		HttpResponse response = null;
		try {
			response = httpRequester.sendPostXml(buildBaseUrl()
					+ "/create", MarshalHelper.marshal(
					ResourceInfoModel.class, resourceInfo), null);
			MainFrameDockerResponseModel dockerResponse = (MainFrameDockerResponseModel) MarshalHelper
					.unmarshal(MainFrameDockerResponseModel.class,
							response.getContent());
			System.out.println("fail cause: " + dockerResponse.getFailCauseString());
			System.out.println("content: " + response.getContent());
			if (dockerResponse.isSuccess()) {
				agent = dockerResponse.getAgentModel();
				System.out.println(agent.getId());
				System.out.println(agent.getPort());
				System.out.println(agent.getMonitorPort());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(agent);
		assertTrue(removeContainer(agent));
	}
	
	private boolean removeContainer(AgentModel agent) {
		HttpResponse response = null;
		MainFrameResponseModel dockerResponse = null;
		try {
			response = httpRequester.sendPostXml(buildBaseUrl() + "/remove", MarshalHelper.tryMarshal(agent), null);
			dockerResponse = (MainFrameResponseModel) MarshalHelper
					.unmarshal(MainFrameResponseModel.class,
							response.getContent());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return dockerResponse.isSuccess();
	}
}
