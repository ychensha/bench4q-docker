package org.bench4q.docker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
	private long avalCpu;
	private long maxAvalCpu;
	private long totalMem;
	private long freeMem;
	private long maxFreeMem;
	private Queue<Cpu> processorQueue;
	private List<Cpu> processorList;
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private static final String PROCFS_MEMINFO = "/proc/meminfo";
	private static final String MEMTOTAL_STRING = "MemTotal";
	private static final String MEMFREE_STRING = "MemFree";
	private static final Pattern PROCFS_MEMFILE_FORMAT = Pattern.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB");
	private static final int REQUEST_APPROVE = 1;
	private static final int REQUEST_REJECT = 0;
//	public synchronized 
	
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
//		avalCpu = Runtime.getRuntime().availableProcessors();
		System.out.println("resource pool init...");
		avalCpu = 2;
		ResourceControllerConfig config = new ResourceControllerConfig();
		processorQueue = new PriorityQueue<Cpu>((int)avalCpu, new Comparator<Cpu>(){
			public int compare(Cpu c1, Cpu c2){
				return c1.getUsage() - c2.getUsage();
			}
		});
		processorList = new ArrayList<Cpu>();
		for(int i = 0; i < avalCpu; ++i){
			Cpu cpu = new Cpu();
			cpu.setCpuId(i);
			cpu.setUsage(0);
			processorQueue.add(cpu);
			processorList.add(cpu);
		}
		XStream xStream = new XStream();
		InputStream in = null;
		try {
			in = new FileInputStream("./config.xml");
			config = (ResourceControllerConfig) xStream.fromXML(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		avalCpu *= config.getVcpuRatio();
		maxAvalCpu = avalCpu;
		BufferedReader bufferedReader = null;
		Matcher mat = null;
//		try {
//			bufferedReader = new BufferedReader(new FileReader(PROCFS_MEMINFO));
//			String line = null;
//			while((line = bufferedReader.readLine()) != null){
//				mat = PROCFS_MEMFILE_FORMAT.matcher(line);
//				if(mat.find()){
//					if(mat.group(1).equals(MEMTOTAL_STRING))
//						totalMem = Long.parseLong(mat.group(2))*1024;
//					else if(mat.group(1).equals(MEMFREE_STRING))
//						maxFreeMem = Long.parseLong(mat.group(2))*1024;
//				}
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		try {
//			bufferedReader.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	/**
	 * 
	 * @param resource request certain resource used to test
	 * @return NOW is CPU setting
	 */
	public String requestResource(TestResource resource){
		long testCpu = resource.getCpuNumber();
		long testMem = resource.getMemoryLimit();
		String response = null;
		lock.writeLock().lock();
		try{
			StringBuilder builder = new StringBuilder();
			if(avalCpu >= testCpu){
				for(int i = 0; i < testCpu; ++i){
					Cpu cpu = processorQueue.poll();
					cpu.setUsage(cpu.getUsage() + 1);
					builder.append(cpu.getCpuId()+",");
					processorQueue.add(cpu);
				}
				builder.deleteCharAt(builder.length() - 1);
				avalCpu -= testCpu;
				response = builder.toString();
			}
		} finally{
			lock.writeLock().unlock();
		}
		
		System.out.println("after request, avalCpu: " + avalCpu);
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
				avalCpu++;
			}
			//update freeMem
			freeMem += container.getConfig().getMemory();
			//update the Priority Queue
			int size = processorList.size();
			for(int i = 0; i < size; ++i){
				Cpu cpu = processorQueue.poll();
				processorQueue.add(cpu);
			}
		} finally{
			lock.writeLock().unlock();
		}
		
		System.out.println("after release, avalCpu: " + avalCpu);
	}
	
	public Resource requestCurrentStatus(){
		Resource resource = new Resource();
		lock.readLock().lock();
		try{
			resource.setAvalCpu(avalCpu);
			resource.setFreeMem(freeMem);
		} finally{
			lock.readLock().unlock();
		}
		return resource;
	}
	
	public static ResourcePool getInstance(){
		return instance;
	}

	public long getAvalCpu() {
		return avalCpu;
	}

	public void setAvalCpu(long avalCpu) {
		this.avalCpu = avalCpu;
	}

	public long getMaxAvalCpu() {
		return maxAvalCpu;
	}

	public void setMaxAvalCpu(long maxAvalCpu) {
		this.maxAvalCpu = maxAvalCpu;
	}

	public long getFreeMem() {
		return freeMem;
	}

	public void setFreeMem(long freeMem) {
		this.freeMem = freeMem;
	}

	public long getMaxFreeMem() {
		return maxFreeMem;
	}

	public void setMaxFreeMem(long maxFreeMem) {
		this.maxFreeMem = maxFreeMem;
	}
}
