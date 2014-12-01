package org.bench4q.docker.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.bench4q.docker.communication.DockerDaemonMessenger;
import org.bench4q.docker.model.Container;
import org.bench4q.docker.model.CreateContainer;
import org.bench4q.docker.model.InspectContainer;
import org.bench4q.docker.model.StartContainer;
import org.bench4q.share.master.test.resource.AgentModel;
import org.bench4q.share.master.test.resource.ResourceInfoModel;
import org.bench4q.share.master.test.resource.TestResourceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DockerService {
	private Logger logger = Logger.getLogger(DockerService.class);

	private static int VETHID = 1;
	private static String IMAGE_NAME;
	private static String DOCKER_HOST_PASSWORD;

	private static int CPU_CFS_PERIOD_US;
	private static final String LXC_CPUSET_CPUS = "lxc.cgroup.cpuset.cpus";
	private static final String LXC_NETWORK_VETH_PAIR = "lxc.network.veth.pair";
	private static final String LXC_CPU_QUOTA = "lxc.cgroup.cpu.cfs_quota_us";
	private static final String LXC_MEMORY_LIMIT = "lxc.cgroup.memory.limit_in_bytes";

	private static final String PROPERTIES_FILE_NAME = "docker-service.properties";

	private Map<String, ResourceInfoModel> containerInfoMap;

	@Autowired
	private DockerDaemonMessenger dockerDaemonMessenger;
	@Autowired
	private ResourceNode resourceNode;

	public DockerService() {
		Properties prop = new Properties();
		containerInfoMap = new HashMap<String, ResourceInfoModel>();
		try {
			InputStream inputStream = DockerService.class.getClassLoader()
					.getResourceAsStream(PROPERTIES_FILE_NAME);
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				logger.fatal("docker-service property file not exist");
			}
			IMAGE_NAME = prop.getProperty("IMAGE_NAME", "chensha/docker");
			DOCKER_HOST_PASSWORD = prop.getProperty("HOST_LINUX_PASSWORD");
			CPU_CFS_PERIOD_US = Integer.valueOf(prop
					.getProperty("CPU_CFS_PERIOD_US"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return current resource status
	 */
	public TestResourceModel getCurrentResourceStatus() {
		return resourceNode.getCurrentStatus();
	}

	public AgentModel createTestContainer(ResourceInfoModel resource) {
		AgentModel result = new AgentModel();
		resource = resourceNode.requestResource(resource);
		System.out.println("get resourceNode response.");
		if (resource == null) {
			return null;
		}
		CreateContainer createContainer = getCreateContainerFromResourceInfo(resource);
		String id = dockerDaemonMessenger.createContainer(createContainer);
		result.setId(id);
		System.out.println("create finish.");
		if (result.getId() != null) {
			if (!dockerDaemonMessenger.startContainer(getStartContainer(
					resource, id)))
				return null;
		}
		System.out.println("start finish.");
		setContainerDownloadBandWidth(resource);
		System.out.println("create container finish.");
		InspectContainer container = dockerDaemonMessenger
				.inspectContainer(result.getId());
		try {
			result.setHostName(getHostInet4Address("eth0"));
			result.setPort(Integer.valueOf(container.getAgentPort()));
			result.setMonitorPort(Integer.valueOf(container.getMonitorPort()));
			result.setResourceInfo(resource);
		} catch (Exception e) {
			logger.error("inspect container get wrong info.");
			remove(result);
			return null;
		}
		containerInfoMap.put(id, resource);
		return result;
	}

	public AgentModel createContainer(ResourceInfoModel resource) {
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

	private StartContainer getStartContainer(ResourceInfoModel resource,
			String id) {
		StartContainer startContainer = new StartContainer();
		List<String> ports = new ArrayList<String>();
		ports.add("");
		startContainer.setLxcConf(getLxcConf(resource));
		startContainer.setPortbindings(ports);
		startContainer.setPrivileged(true);
		startContainer.setId(id);
		return startContainer;
	}

	private Map<String, String> getLxcConf(ResourceInfoModel resource) {
		Map<String, String> result = new HashMap<String, String>();
		result.put(
				LXC_CPU_QUOTA,
				String.valueOf(CPU_CFS_PERIOD_US * resource.getCpu()
						/ resource.getvCpuRatio()));
		result.put(LXC_NETWORK_VETH_PAIR, "veth" + VETHID++);
		result.put(LXC_MEMORY_LIMIT,
				String.valueOf(resource.getMemoryKB() * 1024));
		StringBuilder stringBuilder = new StringBuilder();
		for (int cpuId : resource.getCpuSet()) {
			stringBuilder.append(cpuId).append(",");
		}
		result.put(LXC_CPUSET_CPUS,
				stringBuilder.substring(0, stringBuilder.length() - 1));
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

	private void setContainerDownloadBandWidth(ResourceInfoModel resource) {
		if (resource.getDownloadBandwidthKByte() == 0)
			return;
		String command = getTcCmd("veth" + (VETHID - 1),
				resource.getDownloadBandwidthKByte());
		if (DOCKER_HOST_PASSWORD != null) {
			String psw = "echo " + DOCKER_HOST_PASSWORD + "|";
			command = psw + command;
		}
		try {
			List<String> cmds = new ArrayList<String>();
			cmds.add("/bin/sh");
			cmds.add("-c");
			cmds.add(command);
			Runtime.getRuntime().exec(cmds.toArray(new String[cmds.size()]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getTcCmd(String device, long bandwidthLimit) {
		return "sudo -S tc qdisc add dev " + device + " root tbf rate "
				+ bandwidthLimit
				+ "kbps latency 50ms burst 50000 mpu 64 mtu 1500";
	}

	private CreateContainer getCreateContainerFromResourceInfo(
			ResourceInfoModel resource) {
		CreateContainer result = new CreateContainer();
		if (resource.getUploadBandwidthKByte() != 0) {
			String startupCmd = getTcCmd("eth0",
					resource.getUploadBandwidthKByte());
			// "/opt/monitor/*.sh&&tc qdisc add dev eth0 root tbf rate 200kbps
			// latency 50ms burst 50000 mpu 64 mtu 1500&&
			// \java -server -jar /opt/bench4q-agent-publish/*.jar"
			// now it is the only way to set upload bandwidth
			startupCmd = startupCmd + "&&"
					+ resource.getCmds().get(resource.getCmds().size() - 1);
			resource.getCmds().remove(resource.getCmds().size() - 1);
			resource.getCmds().add(startupCmd);
		}
		result.setCmd(resource.getCmds());
		result.setImage(resource.getImageName());
		return result;
	}

	/**
	 * Remove Given Container
	 * 
	 * @param container
	 *            the container to be removed
	 * @return true if succeed
	 */
	public boolean remove(AgentModel agent) {
		boolean result = false;
		String logFilePath = "./AgentLog/"
				+ new Date().toString().replace(':', '_');
		makeContainerLogDir(logFilePath);
		try {
			Runtime.getRuntime().exec(
					"docker cp " + agent.getId() + ":/logs/log.log "
							+ logFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (dockerDaemonMessenger.killContainer(agent.getId())
				& dockerDaemonMessenger.removeContainer(agent.getId())) {
			System.out.println("removed " + agent.getId());
			if(resourceNode != null)
				resourceNode.releaseResource(agent.getResourceInfo());
			result = true;
			containerInfoMap.remove(agent.getId());
		}
		return result;
	}

	private void makeContainerLogDir(String logDirName) {
		File dir = new File(logDirName);
		if (dir.exists()) {
			System.out.println("container log dir existing");
			return;
		}
		dir.mkdirs();
	}

	public List<AgentModel> getAgentList() {
		List<AgentModel> result = new ArrayList<AgentModel>();
		List<Container> containers = dockerDaemonMessenger.listContainer();
		for (Container container : containers) {
			if (container.getImage().contains("agent")) {
				AgentModel agent = new AgentModel();
				agent.setId(container.getId());
				result.add(agent);
			}
		}
		return result;
	}
}
