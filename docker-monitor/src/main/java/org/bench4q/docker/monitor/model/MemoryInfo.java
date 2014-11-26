package org.bench4q.docker.monitor.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bench4q.share.master.test.resource.ResourceInfoModel;


/**
 * @author gengpeng
 *info of each container
 */
public class MemoryInfo {
	private long usedMemory;
	private List<Integer> pidList;
	private ResourceInfoModel resourceInfo;

	public MemoryInfo(ResourceInfoModel resourceInfo){
		this.resourceInfo = resourceInfo;
		initPidList();
	}
	
	private void initPidList(){
		pidList = new ArrayList<Integer>();
		File procRootDir = new File("/proc/");
		FileFilter pidFilter = new FileFilter("^[0-9]*$");
		File[] pidFiles = procRootDir.listFiles(pidFilter);
		for(File file : pidFiles){
			pidList.add(Integer.valueOf(file.getName()));
		}
	}
	
	public long getTotalKiloBytes(){
		return resourceInfo.getMemoryKB();
	}
	
	private long getProcessUsedMemory(int pId){
		FileReader fr = null;
		String url = "/proc/" + pId + "/status";
		try {
			fr = new FileReader(url);
			BufferedReader buff = new BufferedReader(fr);
			String line;
			while((line=buff.readLine()) != null){
				String[] array = line.split("\\s+");
				if(array[0].equals("VmRSS:")){
					this.usedMemory=Long.valueOf(array[1]);
			        break;
				}
			}
			buff.close();
		} catch (FileNotFoundException e) {
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return usedMemory;
	}
	
	public long getUsedMemory(){
		long result = 0;
		for(int pid : pidList){
			result += getProcessUsedMemory(pid);
		}
		return result;
	}
	
	public double getMemoryUsedPercent(){
		return (double)this.getUsedMemory()/this.getTotalKiloBytes();
	} 
}
