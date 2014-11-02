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

import org.bench4q.share.master.test.resource.ResourceInfo;

/**
 * @author gengpeng info of each container
 */
public class ProcessorInfo {
	private double processorTimePercent;
	private boolean started = false;
	private List<Integer> pidList;
	private static long sleepTime = 1000;
	private ResourceInfo resourceInfo;

	public ProcessorInfo(ResourceInfo resourceInfo) {
		initPidList();
		this.resourceInfo = resourceInfo;
		startCompute();
	}

	public int getSize() {
		return resourceInfo.getCpu();
	}

	private void initPidList() {
		pidList = new ArrayList<Integer>();
		File procRootDir = new File("/proc/");
		FileFilter pidFilter = new FileFilter("^[0-9]*$");
		File[] pidFiles = procRootDir.listFiles(pidFilter);
		for (File file : pidFiles) {
			pidList.add(Integer.valueOf(file.getName()));
		}
	}

	public void startCompute() {
		if (!started) {
			new Handler().start();
			started = true;
		}
	}

	class Handler extends Thread {
		private void compute() throws NumberFormatException {
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
				for (int pid : pidList) {
					url = "/proc/" + pid + "/stat";
					FileReader fr = null;
					try {
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
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// pay attention to the unit of uTime[], it's jiffies = 10ms
				processorTimePercent = (uTime[1] + sTime[1] + cuTime[1]
						+ csTime[1] - uTime[0] - sTime[0] - cuTime[0] - csTime[0])
						* 10
						* resourceInfo.getvCpuRatio()
						/ (sleepTime * resourceInfo.getCpu());
				processorTimePercent = Math.min(1.0, processorTimePercent);
				try {
					Thread.currentThread();
					Thread.sleep(sleepTime);
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

	public double getProcessorTimePercent() {
		return processorTimePercent;
	}

	public void setUserTimePercent(double processorTimePercent) {
		this.processorTimePercent = processorTimePercent;
	}
}
