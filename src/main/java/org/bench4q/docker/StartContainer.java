package org.bench4q.docker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;


public class StartContainer {
	private PortBindings portBindings;
	private List<LxcConf> lxcConf;
	private boolean privileged;
	
	public StartContainer(){
		lxcConf = new ArrayList<LxcConf>();
	}
	
	public PortBindings getPortBindings() {
		return portBindings;
	}

	public void setPortbindings(List<String> ports){
		portBindings = new PortBindings(ports);
	}

	public List<LxcConf> getLxcConf() {
		return lxcConf;
	}

	public void setLxcConf(Map<String, String> map) {
		Set<Map.Entry<String, String>> set = map.entrySet();
		for(Iterator<Map.Entry<String, String>> it = set.iterator(); it.hasNext();){
			LxcConf opts = new LxcConf();
			Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
			opts.setKey(entry.getKey());
			opts.setValue(entry.getValue());
			
			lxcConf.add(opts);
		}
	}

	public boolean isPrivileged() {
		return privileged;
	}

	public void setPrivileged(boolean privileged) {
		this.privileged = privileged;
	}
}

class LxcConf{
	private String key;
	private String value;
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}