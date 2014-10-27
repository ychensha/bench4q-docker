package org.bench4q.docker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		for(Map.Entry<String, String> entry : map.entrySet()){
			LxcConf opts = new LxcConf();
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