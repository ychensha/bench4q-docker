package org.bench4q.docker.node;

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

import org.bench4q.share.master.test.resource.ResourceInfo;
import org.bench4q.share.master.test.resource.TestResourceModel;

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
	private int vCpuRatio;
	private double containerHealthThreshold;
	private long totalUploadBandwidthKB = 10 * 1000;
	private long freeUploadBandwidthKB;
	private long totalDownloadBandwidthKB = 10 * 1000;
	private long freeDownloadBandwidthKB; 
	private int physicalCpu;
	private int totalVCpu;
	private int freeVCpu;
	private long totalMemoryKB;
	private long freeMemoryKB;

	private static int CPU_CFS_PERIOD_US = 100000; 
	private static final String PROCFS_MEMINFO = "/proc/meminfo";
	private static final String PROCFS_CPUINFO = "/proc/cpuinfo";
	private static final String MEMTOTAL_STRING = "MemTotal";
	private static final String MEMFREE_STRING = "MemFree";
	private static final Pattern PROCFS_MEMFILE_FORMAT = Pattern
			.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB");
	private static final Pattern PROCFS_CPUFILE_FORMAT = Pattern
			.compile("processor");
	
	private Queue<Cpu> processorQueue;
	private List<Cpu> processorList;
	
	private void readSystemInfo() {
		BufferedReader bufferedReader = null;
		Matcher mat = PROCFS_MEMFILE_FORMAT.matcher("one line ");
		try {
			bufferedReader = new BufferedReader(new FileReader(PROCFS_MEMINFO));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				mat = PROCFS_MEMFILE_FORMAT.matcher(line);
				if (mat.find()) {
					if (mat.group(1).equals(MEMTOTAL_STRING))
						totalMemoryKB = Long.parseLong(mat.group(2));
					else if (mat.group(1).equals(MEMFREE_STRING))
						freeMemoryKB = Long.parseLong(mat.group(2));
				}
			}
			bufferedReader.close();

			bufferedReader = new BufferedReader(new FileReader(PROCFS_CPUINFO));
			while ((line = bufferedReader.readLine()) != null) {
				mat = PROCFS_CPUFILE_FORMAT.matcher(line);
				if (mat.find())
					physicalCpu++;
			}
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			// may be it's not Linux, suppose we get 2GB memory and 2 CPU
			freeMemoryKB = totalMemoryKB = 2 * 1024 * 1024;
			physicalCpu = 2;
			e.printStackTrace();
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
			totalDownloadBandwidthKB = Long.valueOf(prop.getProperty("DOWNLOADBANDWIDTH", "10000"));
			totalUploadBandwidthKB = Long.valueOf(prop.getProperty("UPLOADBANDWIDTH", "10000"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void init() {
		readSystemInfo();
		readProperties();
		freeUploadBandwidthKB = totalUploadBandwidthKB;
		freeDownloadBandwidthKB = totalDownloadBandwidthKB;
		freeVCpu = totalVCpu = vCpuRatio * physicalCpu;
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

	public int getvCpuRatio() {
		return vCpuRatio;
	}

	public void setvCpuRatio(int vCpuRatio) {
		this.vCpuRatio = vCpuRatio;
	}

	public double getContainerHealthThreshold() {
		return containerHealthThreshold;
	}

	public void setContainerHealthThreshold(double containerHealthThreshold) {
		this.containerHealthThreshold = containerHealthThreshold;
	}

	public long getUploadBandwidthKB() {
		return totalUploadBandwidthKB;
	}

	public void setUploadBandwidthKB(long uploadBandwidth) {
		this.totalUploadBandwidthKB = uploadBandwidth;
	}

	public long getDownloadBandwidthKB() {
		return totalDownloadBandwidthKB;
	}

	public void setDownloadBandwidthKB(long downloadBandwidth) {
		this.totalDownloadBandwidthKB = downloadBandwidth;
	}

	private boolean isEnough(ResourceInfo resourceInfo) {
		boolean result = false;
		if (resourceInfo.getCpu() <= getFreeVCpu()
				&& resourceInfo.getMemoryKB() <= getFreeMemoryKB()
				&& resourceInfo.getDownloadBandwidthKByte() <= getDownloadBandwidthKB()
				&& resourceInfo.getUploadBandwidthKByte() <= getUploadBandwidthKB())
			result = true;
		return result;
	}
	
	/**
	 * 
	 * @param resourceInfo resource requested
	 * @return the format that docker-api requires
	 */
	public ResourceInfo requestResource(ResourceInfo resourceInfo) {
		if(!isEnough(resourceInfo))
			return null;
		
		List<Integer> cpuSet = new ArrayList<Integer>();
		for (int i = 0; i < resourceInfo.getCpu(); ++i) {
			Cpu cpu = processorQueue.poll();
			cpu.setUsage(cpu.getUsage() + 1);
			cpuSet.add(cpu.getCpuId());
			processorQueue.add(cpu);
		}
		resourceInfo.setCpuSet(cpuSet);
		freeVCpu -= resourceInfo.getCpu();
		freeMemoryKB -= resourceInfo.getMemoryKB();
		freeDownloadBandwidthKB -= resourceInfo.getDownloadBandwidthKByte();
		freeUploadBandwidthKB -= resourceInfo.getUploadBandwidthKByte();
		
		resourceInfo.setHealthThreshold(containerHealthThreshold);
		resourceInfo.setvCpuRatio(vCpuRatio);
		return resourceInfo;
	}
	
	public void releaseResource(ResourceInfo resourceInfo){
		// update avalCpu
		for (int cpuId : resourceInfo.getCpuSet()) {
			Cpu cpu = processorList.get(cpuId);
			cpu.setUsage(cpu.getUsage() - 1);
			freeVCpu++;
		}
		freeMemoryKB += resourceInfo.getMemoryKB();
		freeDownloadBandwidthKB += resourceInfo.getDownloadBandwidthKByte();
		freeUploadBandwidthKB += resourceInfo.getUploadBandwidthKByte();
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
		TestResourceModel result = new TestResourceModel();
		result.setFreeCpu(freeVCpu);
		result.setTotalCpu(totalVCpu);
		result.setUsedCpu(totalVCpu - freeVCpu);
		result.setTotalMemory(totalMemoryKB);
		result.setFreeMemory(freeMemoryKB);
		result.setUsedMemory(totalMemoryKB - freeMemoryKB);
		result.setTotalDownloadBandwidthKB(totalDownloadBandwidthKB);
		result.setFreeDownloadBandwidthKB(freeDownloadBandwidthKB);
		result.setUsedDownloadBandwidthKB(totalDownloadBandwidthKB - freeDownloadBandwidthKB);
		result.setTotalUploadBandwidthKB(totalUploadBandwidthKB);
		result.setFreeUploadBandwidthKB(freeUploadBandwidthKB);
		result.setUsedUploadBandwidthKB(totalUploadBandwidthKB - freeUploadBandwidthKB);
		return result;
	}

	private long getFreeMemoryKB() {
		return freeMemoryKB;
	}

	private long getTotalMemoryKB() {
		return totalMemoryKB;
	}

	private int getFreeVCpu() {
		return freeVCpu;
	}

	private int getTotalVCpu() {
		return totalVCpu;
	}
}
