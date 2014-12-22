package org.bench4q.docker.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bench4q.share.models.master.ResourceInfoModel;
import org.bench4q.share.models.master.TestResourceModel;

class Cpu {
	int usage;
	int cpuId;

	public int getUsage() {
		return usage;
	}

	public void setUsage(int usage) {
		this.usage = usage;
	}

	public int getCpuId() {
		return cpuId;
	}

	public void setCpuId(int cpuId) {
		this.cpuId = cpuId;
	}
}

public class DockerBlotter {
	private Logger logger = Logger.getLogger(DockerBlotter.class);
	
	private int vCpuRatio;
	private double containerHealthThreshold;
	private int physicalCpu;
	
	private TestResourceModel testResourceModel;

	private static final String PROCFS_MEMINFO = "/proc/meminfo";
	private static final String PROCFS_CPUINFO = "/proc/cpuinfo";
	private static final String MEMTOTAL_STRING = "MemTotal";
	private static final String MEMFREE_STRING = "MemFree";
	private static final String MEMCACHED_STRING = "Cached";
	private static final Pattern PROCFS_MEMFILE_FORMAT = Pattern
			.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB");
	private static final Pattern PROCFS_CPUFILE_FORMAT = Pattern
			.compile("processor");
	
	private Queue<Cpu> processorQueue;
	private List<Cpu> processorList;
	
	private void readSystemInfo() {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(PROCFS_MEMINFO));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				Matcher mat = PROCFS_MEMFILE_FORMAT.matcher(line);
				if (mat.find()) {
					if (mat.group(1).equals(MEMTOTAL_STRING))
						testResourceModel.setTotalMemory(Long.parseLong(mat.group(2)));
					else if (mat.group(1).equals(MEMFREE_STRING))
						testResourceModel.setFreeMemory(Long.parseLong(mat.group(2)));
					else if (mat.group(1).equals(MEMCACHED_STRING))
						testResourceModel.setFreeMemory(testResourceModel.getFreeMemory() + Long.parseLong(mat.group(2)));
				}
			}
			bufferedReader.close();

			bufferedReader = new BufferedReader(new FileReader(PROCFS_CPUINFO));
			while ((line = bufferedReader.readLine()) != null) {
				Matcher mat = PROCFS_CPUFILE_FORMAT.matcher(line);
				if (mat.find())
					physicalCpu++;
			}
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			// may be it's not Linux, suppose we get 2GB memory and 2 CPU
			testResourceModel.setFreeMemory(2 * 1024 * 1024);
			testResourceModel.setTotalMemory(2 * 1024 * 1024);
			testResourceModel.setTotalCpu(4);
			logger.warn("can not find procfs");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readProperties(){
		Properties prop = new Properties();
		try {
			prop.load(ResourceNode.class.getClassLoader().getResourceAsStream(
					"docker-service.properties"));
			vCpuRatio = Integer.valueOf(prop.getProperty("VCPU_RATIO", "3"));
			containerHealthThreshold = Double.valueOf(prop.getProperty("CONTAINER_HEALTH_THRESHOLD", "0.85"));
			testResourceModel.setTotalDownloadBandwidthKB(Long.valueOf(prop.getProperty("DOWNLOADBANDWIDTH", "10000")));
			testResourceModel.setFreeDownloadBandwidthKB(testResourceModel.getTotalDownloadBandwidthKB());
			testResourceModel.setTotalUploadBandwidthKB(Long.valueOf(prop.getProperty("UPLOADBANDWIDTH", "10000")));
			testResourceModel.setFreeUploadBandwidthKB(testResourceModel.getTotalUploadBandwidthKB());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void init() {
		testResourceModel = new TestResourceModel();
		readSystemInfo();
		readProperties();
		testResourceModel.setTotalCpu(physicalCpu * vCpuRatio);
		testResourceModel.setFreeCpu(testResourceModel.getTotalCpu());
		processorQueue = new PriorityQueue<Cpu>(physicalCpu, new Comparator<Cpu>() {
			public int compare(Cpu c1, Cpu c2) {
				return c1.getUsage() - c2.getUsage();
			}
		});
		processorList = new ArrayList<Cpu>();
		for (int i = 0; i < physicalCpu; ++i) {
			Cpu cpu = new Cpu();
			cpu.setCpuId(i);
			cpu.setUsage(0);
			processorQueue.add(cpu);
			processorList.add(cpu);
		}
	}

	private boolean isEnough(ResourceInfoModel resourceInfo) {
		boolean result = false;
		if (resourceInfo.getCpu() <= testResourceModel.getFreeCpu()
				&& resourceInfo.getMemoryKB() <= testResourceModel.getFreeMemory()
				&& resourceInfo.getDownloadBandwidthKByte() <= testResourceModel.getFreeDownloadBandwidthKB()
				&& resourceInfo.getUploadBandwidthKByte() <= testResourceModel.getFreeUploadBandwidthKB())
			result = true;
		return result;
	}
	
	/**
	 * 
	 * @param resourceInfo resource requested
	 * @return the format that docker-api requires
	 */
	public ResourceInfoModel requestResource(ResourceInfoModel resourceInfo) {
		if(!isEnough(resourceInfo)){
			logger.warn("the system resource is not enough.");
			return null;
		}
		
		List<Integer> cpuSet = new ArrayList<Integer>();
		for (int i = 0; i < resourceInfo.getCpu(); ++i) {
			Cpu cpu = processorQueue.poll();
			cpu.setUsage(cpu.getUsage() + 1);
			cpuSet.add(cpu.getCpuId());
			processorQueue.add(cpu);
		}
		resourceInfo.setCpuSet(cpuSet);
		testResourceModel.setFreeCpu(testResourceModel.getFreeCpu() - resourceInfo.getCpu());
		testResourceModel.setFreeMemory(testResourceModel.getFreeMemory() - resourceInfo.getMemoryKB());
		testResourceModel.setFreeUploadBandwidthKB(testResourceModel.getFreeUploadBandwidthKB() - resourceInfo.getUploadBandwidthKByte());
		testResourceModel.setFreeDownloadBandwidthKB(testResourceModel.getFreeDownloadBandwidthKB() - resourceInfo.getDownloadBandwidthKByte());
		
		resourceInfo.setHealthThreshold(containerHealthThreshold);
		resourceInfo.setvCpuRatio(vCpuRatio);
		return resourceInfo;
	}
	
	public void releaseResource(ResourceInfoModel resourceInfo){
		// update avalCpu
		if(resourceInfo == null){
			logger.warn("release resource for null");
			return;
		}
		for (int cpuId : resourceInfo.getCpuSet()) {
			Cpu cpu = processorList.get(cpuId);
			cpu.setUsage(cpu.getUsage() - 1);
		}
		testResourceModel.setFreeCpu(testResourceModel.getFreeCpu() + resourceInfo.getCpuSet().size());
		testResourceModel.setFreeMemory(testResourceModel.getFreeMemory() + resourceInfo.getMemoryKB());
		testResourceModel.setFreeUploadBandwidthKB(testResourceModel.getFreeUploadBandwidthKB() + resourceInfo.getUploadBandwidthKByte());
		testResourceModel.setFreeDownloadBandwidthKB(testResourceModel.getFreeDownloadBandwidthKB() + resourceInfo.getDownloadBandwidthKByte());
		// update the Priority Queue
		int size = processorList.size();
		for (int i = 0; i < size; ++i) {
			Cpu cpu = processorQueue.poll();
			processorQueue.add(cpu);
		}
	}
	
	/**
	 * query for current status, including free vcpu, free memory, etc.
	 */
	public TestResourceModel getCurrentStatus(){
		testResourceModel.setUsedCpu(testResourceModel.getTotalCpu() - testResourceModel.getFreeCpu());
		testResourceModel.setUsedMemory(testResourceModel.getTotalMemory() - testResourceModel.getFreeMemory());
		testResourceModel.setUsedUploadBandwidthKB(testResourceModel.getTotalUploadBandwidthKB() - testResourceModel.getFreeUploadBandwidthKB());
		testResourceModel.setUsedDownloadBandwidthKB(testResourceModel.getTotalDownloadBandwidthKB() - testResourceModel.getFreeDownloadBandwidthKB());
		return testResourceModel;
	}
}
