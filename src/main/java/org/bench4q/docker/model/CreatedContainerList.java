package org.bench4q.docker.model;

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.bench4q.share.master.test.resource.AgentModel;

@XmlRootElement
public class CreatedContainerList {
	
	Set<AgentModel> agentModels;

	public Set<AgentModel> getAgentModels() {
		return agentModels;
	}
	@XmlElementWrapper
	@XmlElement
	public void setAgentModels(Set<AgentModel> agentModels) {
		this.agentModels = agentModels;
	}
	

}
