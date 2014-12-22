package org.bench4q.docker.api.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.models.master.AgentModel;
import org.bench4q.share.models.master.MainFrameDockerResponseModel;
import org.bench4q.share.models.master.MainFrameResponseModel;
import org.bench4q.share.models.master.ResourceInfoModel;
import org.bench4q.utils.MarshalHelper;

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
		resourceInfo.setUploadBandwidthKByte(200);
		List<String> cmds = new ArrayList<String>();
		cmds.add("/bin/sh");
		cmds.add("-c");
//		cmds.add("/opt/monitor/*.sh&&java -server -jar /opt/bench4q-agent-publish/*.jar&&sudo -S tc qdisc add dev eth0 root tbf rate 200kbps latency 50ms burst 50000 mpu 64 mtu 1500");
		cmds.add("/opt/monitor/*.sh&&java -server -jar /opt/bench4q-agent-publish/*.jar");
//		cmds.add("tc qdisc add dev eth0 root tbf rate 200kbps latency 50ms burst 50000 mpu 64 mtu 1500");
		resourceInfo.setCmds(cmds);
		resourceInfo.setImageName("bench4q-agent");
		HttpResponse response = null;
		try {
			response = httpRequester.sendPostXml(buildBaseUrl()
					+ "/createTestContainer", MarshalHelper.marshal(
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
