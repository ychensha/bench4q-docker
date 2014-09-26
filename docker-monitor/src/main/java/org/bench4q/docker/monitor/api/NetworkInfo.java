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
public class NetworkInfo {
	private double kiloBytesTotalPerSecond;
	private double kiloBytesReceivedPerSecond;
	private double kiloBytesSentPerSecond;
	private boolean started = false;
	private List<Integer> pidList;
	
	public NetworkInfo(){
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
	private void startCompute(){
		if(!started){
			new Handler().start();
			started = true;
		}
	}

	public double getKiloBytesTotalPerSecond() {
		return kiloBytesTotalPerSecond;
	}

	public void setKiloBytesTotalPerSecond(double kiloBytesTotalPerSecond) {
		this.kiloBytesTotalPerSecond = kiloBytesTotalPerSecond;
	}

	public double getKiloBytesReceivedPerSecond() {
		startCompute();
		return kiloBytesReceivedPerSecond;
	}

	public void setKiloBytesReceivedPerSecond(double kiloBytesReceivedPerSecond) {
		this.kiloBytesReceivedPerSecond = kiloBytesReceivedPerSecond;
	}

	public double getKiloBytesSentPerSecond() {
		startCompute();
		return kiloBytesSentPerSecond;
	}

	public void setKiloBytesSentPerSecond(double kiloBytesSentPerSecond) {
		this.kiloBytesSentPerSecond = kiloBytesSentPerSecond;
	}

	class Handler extends Thread {
		private void compute() throws NumberFormatException {
			double[] receive = new double[] { 0, 0 };
			double[] send = new double[] { 0, 0 };
			String url = null;
			FileReader fr = null;
			while (true) {
				send[0] = send[1];
				receive[0] = receive[1];
				send[1] = receive[1] = 0;
				for(int pid : pidList){
					url = "/proc/" + pid + "/net/dev";
					try {
						fr = new FileReader(url);
						BufferedReader buff = new BufferedReader(fr);
						String line;
						while ((line = buff.readLine()) != null) {
							String[] array = line.trim().split("\\s+");
							if (array[0].equals("eth0:")) {
								receive[1] += Double.valueOf(array[1]);
								send[1] += Double.valueOf(array[9]);
								break;
							}
						}
						buff.close();
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				kiloBytesReceivedPerSecond = (receive[1] - receive[0]) / 1000;
				kiloBytesSentPerSecond = (send[1] - send[0]) / 1000;
				kiloBytesTotalPerSecond = (kiloBytesReceivedPerSecond
						+ kiloBytesSentPerSecond);
				try {
					Thread.currentThread();
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		public void run() {
			try {
				compute();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} 
		}
	}
}
