package org.bench4q.docker.node;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.bench4q.docker.model.Container;
import org.bench4q.docker.model.CreateContainer;
import org.bench4q.docker.model.InspectContainer;
import org.bench4q.docker.model.StartContainer;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.bench4q.share.helper.MarshalHelper;
import org.bench4q.share.master.test.resource.AgentModel;
import org.bench4q.share.master.test.resource.ResourceInfo;
import org.bench4q.share.master.test.resource.TestResourceModel;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class DockerApi {
	private static int VETHID = 1;
	public static String IMAGE_NAME;
	private static String DOCKER_HOST_NAME;
	private static int DOCKER_HOST_PORT;
	private static String DOCKER_HOST_PASSWORD;
	private static final int CREATE_CONTAINER_SUCCESS_CODE = 201;
	private static final int START_CONTAINER_SUCCESS_CODE = 204;
	private static final int INSPECT_CONTAINER_SUCCESS_CODE = 200;
	private static final int KILL_CONTAINER_SUCCESS_CODE = 204;
	private static final int REMOVE_CONTAINER_SUCCESS_CODE = 204;

	private static int CPU_CFS_PERIOD_US; 
	private static final String LXC_CPUSET_CPUS = "lxc.cgroup.cpuset.cpus";
	private static final String LXC_MEMORY_LIMIT_IN_BYTES = "lxc.cgroup.memory.limit_in_bytes";
	private static final String LXC_NETWORK_VETH_PAIR = "lxc.network.veth.pair";
	private static final String LXC_CPU_QUOTA = "lxc.cgroup.cpu.cfs_quota_us";

	private static final String PROPERTIES_FILE_NAME = "docker-service.properties";

	private Gson gson = new GsonBuilder().setFieldNamingPolicy(
			FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	private HttpRequester httpRequester = new HttpRequester();

	public DockerApi() {
		Properties prop = new Properties();
		try {
			prop.load(DockerApi.class.getClassLoader()
					.getResourceAsStream(PROPERTIES_FILE_NAME));
			DOCKER_HOST_NAME = prop.getProperty("DOCKER_HOST_NAME", "0.0.0.0");
			DOCKER_HOST_PORT = Integer.valueOf(prop.getProperty(
					"DOCKER_HOST_PORT", "2375"));
			IMAGE_NAME = prop.getProperty("IMAGE_NAME", "chensha/docker");
			DOCKER_HOST_PASSWORD = prop.getProperty("HOST_LINUX_PASSWORD");
			VETHID = Integer.valueOf(prop.getProperty("VETHID"));
			CPU_CFS_PERIOD_US = Integer.valueOf(prop.getProperty("CPU_CFS_PERIOD_US"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return current resource status
	 */
	public TestResourceModel getCurrentResourceStatus() {
		return ResourceNode.getInstance().getCurrentStatus();
	}
	
	public AgentModel createTestContainer(ResourceInfo resource) {
		AgentModel result = new AgentModel();
		resource = ResourceNode.getInstance().requestResource(resource);
		System.out.println("get resourceNode response.");
		if (resource == null)
			return null;
		result.setId(createContainerAndSetUploadBandwidth(getCreateContainerWithSetting(resource)));
		System.out.println("create finish.");
		if (result.getId() != null) {
			if (!startContainerByIdAndSetLxcConfigWithQuota(result.getId(),
					resource))
				return null;
		}
		System.out.println("start finish.");
		setContainerDownloadBandWidth(resource);
		System.out.println("create container finish.");
		Container container = inspectContainer(result.getId());
		result.setHostName(getHostInet4Address("eth0"));
		result.setPort(Integer.valueOf(container.getPort()));
		result.setMonitorPort(Integer.valueOf(container.getMonitorPort()));
		result.setResourceInfo(resource);
		return result;
	}

	public AgentModel createContainerAndSetCpuQuota(ResourceInfo resource) {
		List<String> cmds = new ArrayList<String>();
		cmds.add("/bin/sh");
		cmds.add("-c");
		cmds.add("opt/monitor/*.sh&&java -server -jar -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -Xverify:none "
					+ "/opt/bench4q-agent-publish/*.jar");
		resource.setImageName(IMAGE_NAME);
		return createTestContainer(resource);
	}

	private String getHostInet4Address(String name) {
		String result = null;
		Enumeration<NetworkInterface> netInterfaces = null;
		try {
			netInterfaces = NetworkInterface.getNetworkInterfaces();
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface ni = netInterfaces.nextElement();
				if (!ni.getName().equals(name)) {
					continue;
				}
				Enumeration<InetAddress> ips = ni.getInetAddresses();
				while (ips.hasMoreElements()) {
					String address = ips.nextElement().getHostAddress();
					if (address.split("\\.").length == 4)
						result = address;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean startContainerByIdAndSetLxcConfigWithQuota(
			String containerId, ResourceInfo resource) {
		StartContainer startContainer = new StartContainer();
		List<String> ports = new ArrayList<String>();
		ports.add("");
		startContainer.setLxcConf(getContainerLxcConfigWithQuota(resource));
		startContainer.setPortbindings(ports);
		startContainer.setPrivileged(true);
		try {
			HttpResponse response = httpRequester.sendPostJson(DOCKER_HOST_NAME
					+ ":" + DOCKER_HOST_PORT + "/containers/" + containerId
					+ "/start", gson.toJson(startContainer), null);
			if (response.getCode() == START_CONTAINER_SUCCESS_CODE)
				return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private Map<String, String> getContainerLxcConfigWithQuota(
			ResourceInfo resource) {
		Map<String, String> result = new HashMap<String, String>();
		System.out.println(CPU_CFS_PERIOD_US * resource.getCpu() / resource.getvCpuRatio());
		result.put(LXC_CPU_QUOTA, String.valueOf(CPU_CFS_PERIOD_US * resource.getCpu() / resource.getvCpuRatio()));
		result.put(LXC_NETWORK_VETH_PAIR, "veth" + VETHID++);
		StringBuilder stringBuilder = new StringBuilder();
		for(int cpuId : resource.getCpuSet()){
			stringBuilder.append(cpuId).append(",");
		}
		result.put(LXC_CPUSET_CPUS, stringBuilder.substring(0, stringBuilder.length() - 1));
		// Properties prop = new Properties();
		// try {
		//
		// prop.load(TestResourceController.class.getClassLoader()
		// .getResourceAsStream(PROPERTIES_FILE_NAME));
		// FileOutputStream outputStream = new FileOutputStream(
		// TestResourceController.class.getClassLoader()
		// .getResource(PROPERTIES_FILE_NAME).toString());
		// prop.setProperty("VETHID", String.valueOf(VETHID));
		// prop.store(outputStream, null);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		return result;
	}

	private void setContainerDownloadBandWidth(ResourceInfo resource) {
		if (resource.getDownloadBandwidthKByte() == 0)
			return;
		String command = getTcCmd("veth" + (VETHID - 1),
				resource.getDownloadBandwidthKByte());
		if (DOCKER_HOST_PASSWORD != null) {
			String psw = "echo " + DOCKER_HOST_PASSWORD + " | ";
			command = psw + command;
		}
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getTcCmd(String device, long bandwidthLimit) {
		return "sudo tc qdisc add dev " + device + " root tbf rate "
				+ bandwidthLimit * 8
				+ "kbit latency 50ms burst 10000 mpu 64 mtu 1500";
	}

	private CreateContainer getCreateContainerWithSetting(
			ResourceInfo resource) {
		CreateContainer result = new CreateContainer();
		if (resource.getUploadBandwidthKByte() != 0) {
			String startupCmd = getTcCmd("eth0",
					resource.getUploadBandwidthKByte());
			resource.getCmds().add(startupCmd);
		}
		result.setCmd(resource.getCmds());
		result.setImage(resource.getImageName());
		result.setMemoryByte(resource.getMemoryKB() * 1024);
		return result;
	}

	private String createContainerAndSetUploadBandwidth(
			CreateContainer createContainer) {
		String result = null;
		try {
			System.out.println("starting call docker api create.");
			HttpResponse response = httpRequester.sendPostJson(DOCKER_HOST_NAME
					+ ":" + DOCKER_HOST_PORT + "/containers/create",
					gson.toJson(createContainer), null);
			System.out.println("get docker creation response");
			if (response.getCode() == CREATE_CONTAINER_SUCCESS_CODE) {
				Container createContainerResponse = gson
						.fromJson(response.getContent(),
								Container.class);
				result = createContainerResponse.getId();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @return container info
	 */
	private Container inspectContainer(String id) {
		InspectContainer inspectContainer = new InspectContainer();
		try {
			HttpResponse response = httpRequester.sendGet(DOCKER_HOST_NAME
					+ ":" + DOCKER_HOST_PORT + "/containers/" + id + "/json",
					null, null);
			if (response.getCode() == INSPECT_CONTAINER_SUCCESS_CODE) {
				inspectContainer = gson.fromJson(response.getContent(),
						InspectContainer.class);
				inspectContainer.setIp(DOCKER_HOST_NAME);
				inspectContainer.setPort(inspectContainer.getHostPort());
				inspectContainer.setMonitorPort(inspectContainer
						.getMonitorPort());
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
	 * 
	 * @param container
	 *            the container to be removed
	 * @return true if succeed
	 */
	public boolean remove(AgentModel agent) {
		guardLogDirectoryExist();
		makeContainerLogDir(agent.getId());
		try {

			Runtime.getRuntime().exec(
					"docker cp " + agent.getId() + ":/logs/log.log "
							+ "/usr/share/bench4q-docker/log/"
							+ agent.getId());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (killContainerPost(agent) == KILL_CONTAINER_SUCCESS_CODE
				| removeContainerPost(agent) == REMOVE_CONTAINER_SUCCESS_CODE) {
			System.out.println("starting remove " + agent.getId());
			if (ResourceNode.getInstance() != null)
				ResourceNode.getInstance().releaseResource(agent);
			return true;
		} else {
			return false;
		}
	}

	private void makeContainerLogDir(String id) {
		File dir = new File("/usr/share/bench4q-docker/log/" + id);
		if (dir.exists()) {
			System.out.println("container log dir existing");
			return;
		}
		dir.mkdir();
	}

	private void guardLogDirectoryExist() {
		File dir = new File("/usr/share/bench4q-docker/log");
		if (!dir.exists()) {
			if (!dir.mkdirs())
				System.out.println("make service log dir fail");
		}
	}

	private int killContainerPost(AgentModel agent) {
		HttpResponse response = null;
		try {
			response = httpRequester.sendPostJson(DOCKER_HOST_NAME + ":"
					+ DOCKER_HOST_PORT + "/containers/" + agent.getId()
					+ "/kill", null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response != null)
			return response.getCode();
		else
			return 0;
	}

	private int removeContainerPost(AgentModel agent) {
		HttpResponse response = null;
		try {
			response = httpRequester.sendDelete(DOCKER_HOST_NAME + ":"
					+ DOCKER_HOST_PORT + "/containers/" + agent.getId(),
					null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response != null)
			return response.getCode();
		else
			return 0;
	}

	public List<AgentModel> getContainerList() {
		List<AgentModel> result = new ArrayList<AgentModel>();
		try {
			HttpResponse response = httpRequester.sendGet(DOCKER_HOST_NAME
					+ ":" + DOCKER_HOST_PORT + "/containers/json", null, null);
			result = gson.fromJson(response.getContent(),
					new TypeToken<List<AgentModel>>() {
					}.getType());
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * @return the container created
	 */
	// public Container createContainer(RequestResource resource){
	// Container container = new Container();
	// String poolResponse =
	// ResourceNode.getInstance().requestResource(resource);
	//
	// if(poolResponse != null){
	// container.setId(createContainerAndSetUploadBandwidth(resource,
	// poolResponse));
	// }
	// else
	// return null;
	//
	// if(container.getId() != null){
	// if(startContainerByIdAndSetLxcConfig(container.getId(), resource,
	// poolResponse)
	// == 0)
	// return null;
	// }
	// setContainerDownloadBandWidth(resource);
	// return inspectContainer(container.getId());
	// }
	//
	// private String createContainerAndSetUploadBandwidth(RequestResource
	// resource){
	// String id = null;
	// HttpPost httpPost = new HttpPost(PROTOL_PREFIX +
	// DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT + "/containers/create");
	// CreateContainer createContainer = new CreateContainer();
	// List<String> cmds = new ArrayList<String>();
	// String startupCmd = "";
	// cmds.add("/bin/sh");
	// cmds.add("-c");
	// cmds.add("/opt/bench4q-agent-publish/startup.sh&&java -jar /opt/monitor/bench4q-docker-monitor.jar");
	// if(resource.getUploadBandwidthKBit() != 0)
	// startupCmd += ""+getTcCmd("eth0",resource.getUploadBandwidthKBit());
	// cmds.add(startupCmd);
	// createContainer.setImage(IMAGE_NAME);
	// createContainer.setCmd(cmds);
	// HttpEntity httpEntity = new StringEntity(gson.toJson(createContainer),
	// ContentType.APPLICATION_JSON);
	// httpPost.setEntity(httpEntity);
	// try {
	// CloseableHttpResponse response = httpClient.execute(httpPost);
	// if(response.getStatusLine().getStatusCode() ==
	// CREATE_CONTAINER_SUCCESS_CODE){
	// id = EntityUtils.toString(response.getEntity(), "utf-8");
	// CreateContainerResponse createContainerResponse = gson.fromJson(id,
	// CreateContainerResponse.class);
	// id = createContainerResponse.getId();
	// }
	// } catch (ClientProtocolException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// return id;
	// }

	// private Map<String, String> getContainerLxcConfig(RequestResource
	// resource, String cpuset){
	// Map<String, String> result = new HashMap<String, String>();
	// result.put(LXC_CPUSET_CPUS, cpuset);
	// result.put(LXC_MEMORY_LIMIT_IN_BYTES,
	// String.valueOf(resource.getMemoryLimitKB() * 1024));
	// result.put(LXC_NETWORK_VETH_PAIR, "veth" + VETHID++);
	// if(VETHID == 0)
	// VETHID = 1;
	// return result;
	// }

	// private int startContainerByIdAndSetLxcConfig(String id, RequestResource
	// resource, String cpuset){
	// StartContainer startContainer = new StartContainer();
	// List<String> ports = new ArrayList<String>();
	// ports.add("0");
	// startContainer.setLxcConf(getContainerLxcConfig(resource, cpuset));
	// startContainer.setPortbindings(ports);
	// startContainer.setPrivileged(true);
	//
	// HttpPost httpPost = new HttpPost(PROTOL_PREFIX +
	// DOCKER_HOST_NAME+":"+DOCKER_HOST_PORT
	// +"/containers/" + id + "/start");
	// HttpEntity httpEntity = new StringEntity(gson.toJson(startContainer),
	// ContentType.APPLICATION_JSON);
	// httpPost.setEntity(httpEntity);
	//
	// if(getResponseStatusCode(httpPost) == START_CONTAINER_SUCCESS_CODE)
	// return 1;
	// else
	// return 0;
	// }
}