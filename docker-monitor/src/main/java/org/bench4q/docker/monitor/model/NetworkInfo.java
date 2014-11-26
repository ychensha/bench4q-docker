package org.bench4q.docker.monitor.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author gengpeng info of physical server
 */
public class NetworkInfo {
	private int pid;
	private double kiloBytesTotalPerSecond;
	private double kiloBytesReceivedPerSecond;
	private double kiloBytesSentPerSecond;
	private boolean started = false;
	private Logger logger = Logger.getLogger(NetworkInfo.class);
	
	public NetworkInfo(String name){
		init(name);
		startCompute();
	}
	
	private void init(String name) {
		try {
			Process p = Runtime.getRuntime().exec("jps");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while((line = reader.readLine()) != null){
				if(line.contains(name)){
					String[] pid = line.split(" ");
					this.pid = Integer.valueOf(pid[0]);
				}
			}
			if(pid == 0){
				logger.fatal("Do not catch pid");
			}
		} catch (IOException e) {
			e.printStackTrace();
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
		return kiloBytesReceivedPerSecond;
	}

	public void setKiloBytesReceivedPerSecond(double kiloBytesReceivedPerSecond) {
		this.kiloBytesReceivedPerSecond = kiloBytesReceivedPerSecond;
	}

	public double getKiloBytesSentPerSecond() {
		return kiloBytesSentPerSecond;
	}

	public void setKiloBytesSentPerSecond(double kiloBytesSentPerSecond) {
		this.kiloBytesSentPerSecond = kiloBytesSentPerSecond;
	}

	class Handler extends Thread {
		private void compute() throws NumberFormatException {
			double[] receive = new double[] { 0, 0 };
			double[] send = new double[] { 0, 0 };
			String path = null;
			FileReader fr = null;
			while (true) {
				send[0] = send[1];
				receive[0] = receive[1];
				send[1] = receive[1] = 0;
				path = "/proc/" + pid + "/net/dev";
				try {
					fr = new FileReader(path);
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
