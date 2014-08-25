package org.bench4q.docker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.xstream.XStream;

class Cpu{
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

public class ResourcePool {
	private static final ResourcePool instance = new ResourcePool();
	private int totalCpu;
	private int freeCpu;
	private long totalMemory;
	private long freeMemory;
	private Queue<Cpu> processorQueue;
	private List<Cpu> processorList;
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private static final String PROCFS_MEMINFO = "/proc/meminfo";
	private static final String PROCFS_CPUINFO = "/proc/cpuinfo";
	private static final String MEMTOTAL_STRING = "MemTotal";
	private static final String MEMFREE_STRING = "MemFree";
	private static final Pattern PROCFS_MEMFILE_FORMAT = Pattern.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB");
	private static final Pattern PROCFS_CPUFILE_FORMAT = Pattern.compile("^(processor*):[ \t]*([0-9]*)[ \t]");
	
	public static void main(String[] args){
		Queue<Cpu> queue = new PriorityQueue<Cpu>(2, new Comparator<Cpu>() {
			public int compare(Cpu c1, Cpu c2){
				return c1.usage - c2.usage;
			}
		});
		List<Cpu> list = new ArrayList<Cpu>();
		
		for(int i = 0; i < 2; ++i){
			Cpu cpu = new Cpu();
			cpu.setCpuId(i);
			cpu.setUsage(0);
			list.add(cpu);
			queue.add(cpu);
		}
		
		Cpu cpu = queue.poll();
		cpu.setUsage(cpu.getUsage() + 1);
		queue.add(cpu);
		
		for(int i = 0; i < 2; ++i)
			System.out.println(list.get(i).getUsage());
		
		list.get(1).setUsage(list.get(1).getUsage() + 2);
		
		cpu = queue.poll();
		System.out.println(cpu.getUsage());
		System.out.println(queue.poll().getUsage());
	}
	
	private ResourcePool(){
//		freeCpu = Runtime.getRuntime().availableProcessors();
		ResourceControllerConfig config = new ResourceControllerConfig();
		freeCpu = totalCpu = 0;
		XStream xStream = new XStream();
		InputStream in = null;
		try {
			in = new FileInputStream("./config.xml");
			config = (ResourceControllerConfig) xStream.fromXML(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader bufferedReader = null;
		Matcher mat = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(PROCFS_MEMINFO));
			String line = null;
			while((line = bufferedReader.readLine()) != null){
				mat = PROCFS_MEMFILE_FORMAT.matcher(line);
				if(mat.find()){
					if(mat.group(1).equals(MEMTOTAL_STRING))
						totalMemory = Long.parseLong(mat.group(2));
					else if(mat.group(1).equals(MEMFREE_STRING))
						freeMemory = Long.parseLong(mat.group(2));
				}
			}
			try {
				bufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			bufferedReader = new BufferedReader(new FileReader(PROCFS_CPUINFO));
			while((line = bufferedReader.readLine()) != null){
				mat = PROCFS_CPUFILE_FORMAT.matcher(line);
				if(mat.find())
					freeCpu++;
			}
			
			try {
				bufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// may be it's not Linux, suppose we get 2GB memory and 2 CPU
			freeMemory = totalMemory = 2*1024*1024;
			freeCpu = totalCpu = 2;
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		processorQueue = new PriorityQueue<Cpu>(freeCpu, new Comparator<Cpu>(){
			public int compare(Cpu c1, Cpu c2){
				return c1.getUsage() - c2.getUsage();
			}
		});
		processorList = new ArrayList<Cpu>();
		for(int i = 0; i < freeCpu; ++i){
			Cpu cpu = new Cpu();
			cpu.setCpuId(i);
			cpu.setUsage(0);
			processorQueue.add(cpu);
			processorList.add(cpu);
		}
		freeCpu *= config.getVcpuRatio();
		totalCpu = freeCpu;
		
		TestResourceController testResourceController = new TestResourceController();
		List<Container> runningContainerList = testResourceController.getContainerList();
		for (Container container : runningContainerList) {
			container = testResourceController.inspectContainer(container.getId());
			String[] cpus = container.getConfig().getCpuset().split(",");
			for(int i = 0; i < cpus.length; ++i){
				if(cpus[i].equals("")){
					for (Cpu cpu : processorList) {
						cpu.setUsage(cpu.getUsage() + 1);
					}
					freeCpu -= processorList.size();
				}
				else{
					Cpu cpu = processorList.get(Integer.valueOf(cpus[i]));
					cpu.setUsage(cpu.getUsage() + 1);
					freeCpu--;
				}
			}
			//update freeMem
			freeMemory -= container.getConfig().getMemory();
			//update the Priority Queue
			int size = processorList.size();
			for(int i = 0; i < size; ++i){
				Cpu cpu = processorQueue.poll();
				processorQueue.add(cpu);
			}
		}
		
		System.out.println("resource pool init finished:\n" + "free cpu: "+freeCpu+"\nfree memory: "+freeMemory);
	}
	
	/**
	 * 
	 * @param resource request certain resource used to test
	 * @return NOW is CPU setting
	 */
	public String requestResource(RequestResource resource){
		long testCpu = resource.getCpuNumber();
		long testMem = resource.getMemoryLimit();
		String response = null;
		lock.writeLock().lock();
		try{
			StringBuilder builder = new StringBuilder();
			if(freeCpu >= testCpu && freeMemory >= testMem){
				for(int i = 0; i < testCpu; ++i){
					Cpu cpu = processorQueue.poll();
					cpu.setUsage(cpu.getUsage() + 1);
					builder.append(cpu.getCpuId()+",");
					processorQueue.add(cpu);
				}
				builder.deleteCharAt(builder.length() - 1);
				freeCpu -= testCpu;
				response = builder.toString();
			}
		} finally{
			lock.writeLock().unlock();
		}
		
		return response;
	}
	
	public void releaseResource(Container container){
		String[] cpus = container.getConfig().getCpuset().split(",");
		lock.writeLock().lock();
		try{
			//update avalCpu
			for(int i = 0; i < cpus.length; ++i){
				Cpu cpu = processorList.get(Integer.valueOf(cpus[i]));
				cpu.setUsage(cpu.getUsage() - 1);
				freeCpu++;
			}
			//update freeMem
			freeMemory += container.getConfig().getMemory();
			//update the Priority Queue
			int size = processorList.size();
			for(int i = 0; i < size; ++i){
				Cpu cpu = processorQueue.poll();
				processorQueue.add(cpu);
			}
		} finally{
			lock.writeLock().unlock();
		}
	}
	
	public Resource getCurrentStatus(){
		Resource resource = new Resource();
		lock.readLock().lock();
		try{
			resource.setFreeCpu(freeCpu);
			resource.setTotalCpu(totalCpu);
			resource.setUsedCpu(totalCpu - freeCpu);
			resource.setTotalMemeory(totalMemory);
			resource.setFreeMemory(freeMemory);
			resource.setUsedMemory(totalMemory - freeMemory);
		} finally{
			lock.readLock().unlock();
		}
		return resource;
	}
	
	public static ResourcePool getInstance(){
		return instance;
	}
}
