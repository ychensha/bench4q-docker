package org.bench4q.docker.monitor.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gengpeng info of physical server
 */
public class DiskInfo {
	private double diskReadRate;
	private double diskWriteRate;
	private boolean started = false;
	private List<Integer> pidList;
	
	public DiskInfo(){
		initPidList();
		startCompute();
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
	public void startCompute(){
		if(!started){
			new Handler().start();
			started=true;
		}
	}

	public double getdiskReadRate() {
		return diskReadRate;
	}

	public void setDiskReadKRate(Double diskReadKRate) {
		this.diskReadRate = diskReadKRate;
	}

	public double getdiskWriteRate() {
		startCompute();
		return diskWriteRate;
	}

	public void setDiskWriteKRate(Double diskWriteKRate) {
		this.diskWriteRate = diskWriteKRate;
	}

	class Handler extends Thread {
		private void compute(){
			double[] read = new double[] { 0.0, 0.0 };
			double[] write = new double[] { 0.0, 0.0 };
			String url = null;
			FileReader fr = null;
			while (true) {
				read[0] = read[1];
				write[0] = write[1];
				read[1] = write[1] = 0;
				for(int pid : pidList){
					url = "/proc/" + pid + "/io";
					try {
						fr = new FileReader(url);
						BufferedReader buff = new BufferedReader(fr);
						String line;
						while ((line = buff.readLine()) != null) {
							String[] array = line.trim().split("\\s+");
							if (array[0].equals("read_bytes:")) {
								read[1] += Double.valueOf(array[1]);
							} else if (array[0].equals("write_bytes:")) {
								write[1] += Double.valueOf(array[1]);
								break;
							}
						}
						buff.close();
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
					}
				}
				diskReadRate = (read[1] - read[0]);
				diskWriteRate = (write[1] - write[0]);
				try {
					Thread.currentThread();
					Thread.sleep(1000);
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
			} 
		}
	}
}
