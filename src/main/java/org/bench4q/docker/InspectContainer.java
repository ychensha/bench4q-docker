package org.bench4q.docker;

import java.util.List;
import java.util.Map;

public class InspectContainer extends Container {
	private List<String> args;
	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}
}