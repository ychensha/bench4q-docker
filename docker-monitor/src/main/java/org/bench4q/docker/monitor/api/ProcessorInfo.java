package org.bench4q.docker.monitor.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gengpeng info of each container
 */
public class ProcessorInfo {
	private int size;
	private double processorTimePercent;
	private boolean started = false;
	private List<Integer> pidList;
	private static final Pattern PROCFS_CPUFILE_FORMAT = Pattern
			.compile("processor");
	private int processorCount;
	private static int vcpuRatio;
	
	public ProcessorInfo(){
		initPidList();
		processorCount = getProcessorCount();
		vcpuRatio = getVCpuRatio();
	}
	
	private int getVCpuRatio(){
		Properties prop = new Properties();
		int result = 0;
		try {
			prop.load(ProcessorInfo.class.getClassLoader().getResourceAsStream("docker-monitor.properties"));
			result = Integer.valueOf(prop.getProperty("VCPU_RATIO", "3"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	private int getProcessorCount(){
		int result = 0;
		FileReader fr;
		try {
			fr = new FileReader("/proc/cpuinfo");
			BufferedReader br = new BufferedReader(fr);
			String line;
			Matcher mat;
			while ((line = br.readLine()) != null) {
				mat = PROCFS_CPUFILE_FORMAT.matcher(line);
				if (mat.find())
					result++;
			}
			br.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
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
	
	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() throws IOException {
		FileReader fr = null;
		int pSize = 0;
		fr = new FileReader("/proc//cpuinfo");
		BufferedReader buff = new BufferedReader(fr);
		String line;
		while ((line = buff.readLine()) != null) {
			String[] array = line.split("\\s+");
			if (array[0].equals("processor"))
				pSize++;
		}
		buff.close();
		return pSize;
	}
	
	public void startCompute(){
		if(!started){
			new Handler().start();
			started=true;
		}
	}
	
	class Handler extends Thread{
		private void compute() throws NumberFormatException, IOException{
			double[] uTime = new double[] { 0, 0 };
			double[] sTime = new double[] { 0, 0 };
			double[] cuTime = new double[] { 0, 0 };
			double[] csTime = new double[] { 0, 0 };
			String url = null;
			while (true) {
				uTime[0] = uTime[1];
				sTime[0] = sTime[1];
				cuTime[0] = cuTime[1];
				csTime[0] = csTime[1];
				uTime[1] = sTime[1] = cuTime[1] = csTime[1] = 0;
				for(int pid : pidList){
					url = "/proc/"+pid+"/stat";
					FileReader fr = null;
					fr = new FileReader(url);
					BufferedReader buff = new BufferedReader(fr);
					String line;
					while ((line = buff.readLine()) != null) {
						String[] array = line.trim().split("\\s+");
						uTime[1] += Double.valueOf(array[13]);
						sTime[1] += Double.valueOf(array[14]);
						cuTime[1] += Double.valueOf(array[15]);
						csTime[1] += Double.valueOf(array[16]);
						break;
					}
					buff.close();
				}
				processorTimePercent = (uTime[1] + sTime[1] + cuTime[1]
						+ csTime[1] - uTime[0] - sTime[0] - cuTime[0] - csTime[0]) / 200;
				processorTimePercent = Math.min(1.0, 
						processorTimePercent * processorCount * vcpuRatio);
				try {
					Thread.currentThread();
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void run(){
			try {
				compute();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public double getProcessorTimePercent(){
		startCompute();
		return processorTimePercent;
	}

	public void setUserTimePercent(double processorTimePercent) {
		this.processorTimePercent = processorTimePercent;
	}
}
