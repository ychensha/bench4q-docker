package org.bench4q.docker.monitor.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


/**
 * @author gengpeng info of physical server
 */
public class NetworkInfo {
	private double kiloBytesTotalPerSecond;
	private double kiloBytesReceivedPerSecond;
	private double kiloBytesSentPerSecond;
	private boolean started = false;

	public NetworkInfo() {
		startCompute();
	}


	private void startCompute() {
		if (!started) {
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
			String path =" /proc/net/dev";;
			FileReader fr = null;
			long lastTime = -1;
			while (true) {
				send[0] = send[1];
				receive[0] = receive[1];
				send[1] = receive[1] = 0;
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
				long timeRange = (System.currentTimeMillis() - lastTime) / 1000L;
				kiloBytesReceivedPerSecond = (receive[1] - receive[0]) / 1024
						/ timeRange;
				kiloBytesSentPerSecond = (send[1] - send[0]) / 1024 / timeRange;
				kiloBytesTotalPerSecond = (kiloBytesReceivedPerSecond + kiloBytesSentPerSecond);
				lastTime = System.currentTimeMillis();
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
