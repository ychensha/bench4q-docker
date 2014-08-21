package org.bench4q.docker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;


public class StartContainer {
	private PortBindings portBindings;
	private Map<String, String> lxcConf;
	
	public StartContainer(){
		lxcConf = new HashMap<String, String>();
	}
	
	public PortBindings getPortBindings() {
		return portBindings;
	}

	public void setPortbindings(List<String> ports){
		portBindings = new PortBindings(ports);
	}

	public Map<String, String> getLxcConf() {
		return lxcConf;
	}

	public void setLxcConf(Map<String, String> lxcConf) {
		this.lxcConf = lxcConf;
	}

}

class LxcConf{
	@SerializedName("lxc.cgroup.cpuset.cpus")
	private String lxc_cgroup_cpuset_cpus;
	@SerializedName("lxc.cgroup.memory.limit_in_bytes")
	private long lxc_cgroup_memory_limit_in_bytes;
	
	public String getLxc_cgroup_cpuset_cpus() {
		return lxc_cgroup_cpuset_cpus;
	}
	public void setLxc_cgroup_cpuset_cpus(String lxc_cgroup_cpuset_cpus) {
		this.lxc_cgroup_cpuset_cpus = lxc_cgroup_cpuset_cpus;
	}
	public long getLxc_cgroup_memory_limit_in_bytes() {
		return lxc_cgroup_memory_limit_in_bytes;
	}
	public void setLxc_cgroup_memory_limit_in_bytes(
			long lxc_cgroup_memory_limit_in_bytes) {
		this.lxc_cgroup_memory_limit_in_bytes = lxc_cgroup_memory_limit_in_bytes;
	}
}