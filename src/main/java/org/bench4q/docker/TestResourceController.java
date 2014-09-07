package org.bench4q.docker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


public class TestResourceController {
	/**/
	private static int CLASSID = 1;
	public static String IMAGE_NAME;
	private static String DOCKER_HOST_NAME;
	private static int DOCKER_HOST_PORT;
	private static String DOCKER_HOST_PASSWORD;
	private static final String PROTOL_PREFIX = "http://";
	private static final int CREATE_CONTAINER_SUCCESS_CODE = 201;
	private static final int START_CONTAINER_SUCCESS_CODE =204;
	private static final int INSPECT_CONTAINER_SUCCESS_CODE = 200;
	private static final int KILL_CONTAINER_SUCCESS_CODE = 204;
	private static final int REMOVE_CONTAINER_SUCCESS_CODE = 204;
	
	private static final String LXC_CPUSET_CPUS = "lxc.cgroup.cpuset.cpus";
	private static final String LXC_MEMORY_LIMIT_IN_BYTES = "lxc.cgroup.memory.limit_in_bytes";
	private static final String LXC_NETWORK_VETH_PAIR = "lxc.network.veth.pair";
	private static final String LXC_CPU_QUOTA = "lxc.cgroup.cpu.cfs_quota_us";
	
	private static final String PROPERTIES_FILE_NAME = "docker-service.properties";
	
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public static void main(String[] args){
		TestResourceController controller = new TestResourceController();
		RequestResource resource = new RequestResource();
		resource.setCpuNumber(1);
		resource.setMemoryLimitKB(256000);
		resource.setUploadBandwidthKBit(200000);
		
		System.out.println(ResourceNode.getInstance().getCurrentStatus().getTotalCpu());
		Container container = controller.createContainerAndSetCpuQuota(resource);
		if(container != null){
			System.out.println(container.getId());
		}
		else {
			System.out.println("create fail");
		}
	}
	
	public TestResourceController(){
		Properties prop = new Properties();
		try {
			prop.load(TestResourceController.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME));
			DOCKER_HOST_NAME = prop.getProperty("DOCKER_HOST_NAME", "0.0.0.0");
			DOCKER_HOST_PORT = Integer.valueOf(prop.getProperty("DOCKER_HOST_PORT", "2375"));
			IMAGE_NAME = prop.getProperty("IMAGE_NAME", "chensha/bench4q-agent-test");
			DOCKER_HOST_PASSWORD = prop.getProperty("HOST_LINUX_PASSWORD");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @return current resource status
	 */
	public Resource getCurrentResourceStatus(){
		return ResourceNode.getInstance().getCurrentStatus();
	}
	
	/**
	 * @return the container created
	 */
	public Container createContainer(RequestResource resource){
		Container container = new Container();
		String poolResponse = ResourceNode.getInstance().requestResource(resource);
		
		if(poolResponse != null){
			container.setId(createContainerAndSetUploadBandwidth(resource, poolResponse));
		}
		else
			return null;
		
		if(container.getId() != null){
			if(startContainerByIdAndSetLxcConfig(container.getId(), resource, poolResponse) 
					== 0)
				return null;
		}
		setContainerDownloadBandWidth(resource);
		return inspectContainer(container.getId());
	}
	
	public Container createContainerAndSetCpuQuota(RequestResource resource){
		Container container = new Container();
		String poolResponse = ResourceNode.getInstance().requestResource(resource);
		if(poolResponse != null){
			String[] cpus = poolResponse.split(",");
			container.setId(createContainerAndSetUploadBandwidth(resource, poolResponse));
			resource.setCpuNumber(ResourceNode.getInstance().getCpuQuota(poolResponse));
		}
		else {
			return null;
		}
		if(container.getId() != null){
			if(startContainerByIdAndSetLxcConfigWithQuota(
					container.getId(), resource) == 0){
				return null;
			}
		}
		setContainerDownloadBandWidth(resource);
		return inspectContainer(container.getId());
	}
//	
//	private String createContainerAndSetUploadBandwidth(RequestResource resource){
//		String id = null;
//		HttpPost httpPost = new HttpPost(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT + "/containers/create");
//		CreateContainer createContainer = new CreateContainer();
//		List<String> cmds = new ArrayList<String>();
//		String startupCmd = "";
//		cmds.add("/bin/sh");
//		cmds.add("-c");
//		cmds.add("/opt/bench4q-agent-publish/startup.sh&&java -jar /opt/monitor/bench4q-docker-monitor.jar");
//		if(resource.getUploadBandwidthKBit() != 0)
//			startupCmd += ""+getTcCmd("eth0",resource.getUploadBandwidthKBit());
//		cmds.add(startupCmd);
//		createContainer.setImage(IMAGE_NAME);
//		createContainer.setCmd(cmds);
//		HttpEntity httpEntity = new StringEntity(gson.toJson(createContainer), ContentType.APPLICATION_JSON);
//		httpPost.setEntity(httpEntity);
//		try {
//			CloseableHttpResponse response = httpClient.execute(httpPost);
//			if(response.getStatusLine().getStatusCode() == CREATE_CONTAINER_SUCCESS_CODE){
//				id = EntityUtils.toString(response.getEntity(), "utf-8");
//				CreateContainerResponse createContainerResponse = gson.fromJson(id, CreateContainerResponse.class);
//				id = createContainerResponse.getId();
//			}
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return id;
//	}
	
	private int startContainerByIdAndSetLxcConfigWithQuota(String containerId, RequestResource resource){
		StartContainer startContainer = new StartContainer();
		List<String> ports = new ArrayList<String>();
		ports.add("");
		startContainer.setLxcConf(getContainerLxcConfigWithQuota(resource));
		startContainer.setPortbindings(ports);
		startContainer.setPrivileged(true);
		
		HttpPost httpPost = new HttpPost(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT
				+"/containers/" + containerId + "/start");
		HttpEntity httpEntity = new StringEntity(gson.toJson(startContainer), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		
		if(getResponseStatusCode(httpPost) == START_CONTAINER_SUCCESS_CODE)
			return 1;
		else
			return 0;
	}
	
	private Map<String, String> getContainerLxcConfigWithQuota(RequestResource resource){
		Map<String, String> result = new HashMap<String, String>();
		result.put(LXC_CPU_QUOTA, String.valueOf(resource.getCpuNumber()));
		//result.put(LXC_MEMORY_LIMIT_IN_BYTES, String.valueOf(resource.getMemoryLimitKB() * 1024));
		//the way to name is not good enough
		result.put(LXC_NETWORK_VETH_PAIR, "veth" + CLASSID++);
		if(CLASSID == 0)
			CLASSID = 1;
		return result;
	} 
	
	private Map<String, String> getContainerLxcConfig(RequestResource resource, String cpuset){
		Map<String, String> result = new HashMap<String, String>();
		result.put(LXC_CPUSET_CPUS, cpuset);
		result.put(LXC_MEMORY_LIMIT_IN_BYTES, String.valueOf(resource.getMemoryLimitKB() * 1024));
		//the way to name is not good enough
		result.put(LXC_NETWORK_VETH_PAIR, "veth" + CLASSID++);
		if(CLASSID == 0)
			CLASSID = 1;
		return result;
	}
	
	private void setContainerDownloadBandWidth(RequestResource resource){
		if(resource.getDownloadBandwidthKBit() == 0)
			return;
		String command = getTcCmd("veth" + (CLASSID - 1), resource.getDownloadBandwidthKBit());
		if(DOCKER_HOST_PASSWORD != null){
			String psw = "echo " + DOCKER_HOST_PASSWORD +" | ";
			command += psw;
		}
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int startContainerByIdAndSetLxcConfig(String id, RequestResource resource, String cpuset){
		StartContainer startContainer = new StartContainer();
		List<String> ports = new ArrayList<String>();
		ports.add("0");
		startContainer.setLxcConf(getContainerLxcConfig(resource, cpuset));
		startContainer.setPortbindings(ports);
		startContainer.setPrivileged(true);
		
		HttpPost httpPost = new HttpPost(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT
				+"/containers/" + id + "/start");
		HttpEntity httpEntity = new StringEntity(gson.toJson(startContainer), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		
		if(getResponseStatusCode(httpPost) == START_CONTAINER_SUCCESS_CODE)
			return 1;
		else
			return 0;
	}
	
	private String getTcCmd(String device, long bandwidthLimit){
		return "sudo tc qdisc add dev "
				+device+" root tbf rate "
				+ bandwidthLimit + "kbit latency 50ms burst 10000 mpu 64 mtu 1500";
	}
	
	private String createContainerAndSetUploadBandwidth(RequestResource resource, String cpuset){
		String id = null;
		HttpPost httpPost = new HttpPost(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT + "/containers/create");
		CreateContainer createContainer = new CreateContainer();
		List<String> cmds = new ArrayList<String>();
		String startupCmd = "";
		cmds.add("/bin/sh");
		cmds.add("-c");
		cmds.add("/opt/bench4q-agent-publish/startup.sh&&java -jar /opt/monitor/bench4q-docker-monitor.jar");
		if(resource.getUploadBandwidthKBit() != 0)
			startupCmd += ""+getTcCmd("eth0",resource.getUploadBandwidthKBit());
		cmds.add(startupCmd);
		createContainer.setCmd(cmds);
		createContainer.setImage(IMAGE_NAME);
		createContainer.setCpuset(cpuset);
		createContainer.setMemory(resource.getMemoryLimitKB() * 1024);
		HttpEntity httpEntity = new StringEntity(gson.toJson(createContainer), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == CREATE_CONTAINER_SUCCESS_CODE){
				id = EntityUtils.toString(response.getEntity(), "utf-8");
				CreateContainerResponse createContainerResponse = gson.fromJson(id, CreateContainerResponse.class);
				id = createContainerResponse.getId();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return id;
	}
	/**
	 * 
	 * @return container info
	 */
	public Container inspectContainer(String id){
		HttpGet httpGet = new HttpGet(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT+"/containers/"+id+"/json");
		InspectContainer inspectContainer = new InspectContainer();
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == INSPECT_CONTAINER_SUCCESS_CODE){
				inspectContainer = gson.fromJson(EntityUtils.toString(response.getEntity(), "utf-8"), 
						InspectContainer.class);
				inspectContainer.setIp(DOCKER_HOST_NAME);
				inspectContainer.setPort(inspectContainer.getHostPort());
				inspectContainer.setMonitorPort(inspectContainer.getMonitorPort());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return inspectContainer;
	}
	/**
	 * Remove Given Container
	 * @param container the container to be removed
	 * @return true if succeed
	 */
	public boolean remove(Container container){
		if(killContainerPost(container) == KILL_CONTAINER_SUCCESS_CODE
				&& removeContainerPost(container) == REMOVE_CONTAINER_SUCCESS_CODE){
			ResourceNode.getInstance().releaseResource(container);
			return true;
		}
		else {
			return false;
		}
	}
	private int killContainerPost(Container container){
		return getResponseStatusCode(new HttpPost(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT+"/containers/"+
					container.getId()+"/kill"));
	}
	
	private int removeContainerPost(Container container){
		return getResponseStatusCode(new HttpDelete(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT+"/containers/"+
					container.getId()));
	}
	
	private int getResponseStatusCode(HttpUriRequest request){
		try {
			return httpClient.execute(request).getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public List<Container> getContainerList(){
		List<Container> result = new ArrayList<Container>();
		HttpGet httpGet = new HttpGet(PROTOL_PREFIX + DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT+"/containers/json");
		try {
			String entity = EntityUtils.toString(httpClient.execute(httpGet).getEntity(), "utf-8");
			result = gson.fromJson(entity, new TypeToken<List<Container>>(){}.getType());
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}