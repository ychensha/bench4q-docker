package org.bench4q.docker.monitor.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.bench4q.share.models.monitor.JvmModel;

public class JvmInfo {
	private int agentPid;
	
	public JvmInfo(){
		try {
			Process p = Runtime.getRuntime().exec("jps");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = reader.readLine();//jps line
			System.out.println(line);
			line = reader.readLine();//monitor line
			System.out.println(line);
			line = reader.readLine();//agent line
			System.out.println(line);
			if(line != null){
				String[] pid = line.split(" ");
				agentPid = Integer.valueOf(pid[0]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JvmModel getJvmInfo(){
		JvmModel result = new JvmModel();
		try {
			Process p = Runtime.getRuntime().exec(getGcCommand());
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = reader.readLine();//title line
			line = reader.readLine();
			if(line != null){
				String[] parts = line.split(" +");
				if(parts.length < 0)
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private String getGcCommand(){
		return "jstat -gc " + agentPid;
	}
}
