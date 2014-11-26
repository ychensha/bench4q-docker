package org.bench4q.docker.monitor.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.bench4q.share.models.monitor.JvmModel;

public class JvmInfo {
	private int agentPid;
	private Logger logger = Logger.getLogger(JvmInfo.class);
	
	public JvmInfo(){
		try {
			Process p = Runtime.getRuntime().exec("jps");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while((line = reader.readLine()) != null){
				if(line.contains("agent")){
					String[] pid = line.split(" ");
					agentPid = Integer.valueOf(pid[0]);
				}
			}
			if(agentPid == 0){
				logger.fatal("Do not catch agent pid");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JvmModel getJvmInfo(){
		JvmModel result = new JvmModel();
		try {
			Process p = Runtime.getRuntime().exec(getCommand("gc"));
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = reader.readLine();//title line
			line = reader.readLine();
			if(line != null){
				String[] parts = line.split("\\s+");
				if(parts.length < 15)
					return null;
				result.setS0CapacityKB(Float.valueOf(parts[0]));
				result.setS1CapacityKB(Float.valueOf(parts[1]));
				result.setS0UsedKB(Float.valueOf(parts[2]));
				result.setS1UsedKB(Float.valueOf(parts[3]));
				result.setEdenCapacityKB(Float.valueOf(parts[4]));
				result.setEdenUsedKB(Float.valueOf(parts[5]));
				result.setOldCapacityKB(Float.valueOf(parts[6]));
				result.setOldUsedKB(Float.valueOf(parts[7]));
				result.setPermCapacityKB(Float.valueOf(parts[8]));
				result.setPermUsedKB(Float.valueOf(parts[9]));
				result.setYoungGcCount(Long.valueOf(parts[10]));
				result.setYoungGcTime(Float.valueOf(parts[11]));
				result.setFullGcCount(Long.valueOf(parts[12]));
				result.setFullGcTime(Float.valueOf(parts[13]));
				result.setTotalGcTime(Float.valueOf(parts[14]));
			}
			
			p = Runtime.getRuntime().exec(getCommand("compiler"));
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			line = reader.readLine();//title line
			line = reader.readLine();
			if(line != null){
				String[] parts = line.split("\\s+");
				if(parts.length < 5)
					return null;
				result.setCompiledClass(Long.valueOf(parts[1]));
				result.setCompileFailedClass(Long.valueOf(parts[2]));
				result.setCompileTime(Float.valueOf(parts[4]));
			}
			
			p = Runtime.getRuntime().exec(getCommand("class"));
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			line = reader.readLine();//title line
			line = reader.readLine();
			if(line != null){
				String[] parts = line.split("\\s+");
				if(parts.length < 5)
					return null;
				result.setLoadedClass(Long.valueOf(parts[1]));
				result.setUnloadedClass(Long.valueOf(parts[3]));
				result.setLoadTime(Float.valueOf(parts[5]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private String getCommand(String options){
		return "jstat -" + options + " " + agentPid;
	}
}
