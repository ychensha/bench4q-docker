package org.bench4q.docker.communication.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bench4q.docker.communication.DockerDaemonMessenger;
import org.bench4q.docker.model.Container;
import org.bench4q.docker.model.CreateContainer;
import org.bench4q.docker.model.DockerDaemonPort;
import org.bench4q.docker.model.DockerStatusCode;
import org.bench4q.docker.model.InspectContainer;
import org.bench4q.docker.model.StartContainer;
import org.bench4q.share.communication.HttpRequester;
import org.bench4q.share.communication.HttpRequester.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

@Component
public class DockerDaemonMessengerImpl implements DockerDaemonMessenger {
	private Logger logger = Logger.getLogger(DockerDaemonMessengerImpl.class);
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(
			FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	@Autowired
	private HttpRequester httpRequester;
	@Autowired
	private DockerDaemonPort dockerDaemonPort;

	private String buildBaseUrl() {
		return dockerDaemonPort.getHostIp() + ":"
				+ dockerDaemonPort.getHostPort();
	}

	/**
	 * @return the id of container, the container is not running
	 */
	@Override
	public String createContainer(CreateContainer createContainer) {
		String result = null;
		try {
			HttpResponse response = httpRequester.sendPostJson(buildBaseUrl()
					+ "/containers/create", gson.toJson(createContainer), null);
			if (response.getCode() == DockerStatusCode.CREATE_CONTAINER_SUCCESS_CODE) {
				return gson.fromJson(response.getContent(), Container.class)
						.getId();
			}
		} catch (IOException e) {

		}
		return result;
	}

	@Override
	public boolean startContainer(StartContainer startContainer) {
		boolean result = false;
		try {
			HttpResponse response = httpRequester.sendPostJson(buildBaseUrl()
					+ "/containers/" + startContainer.getId() + "/start",
					gson.toJson(startContainer), null);
			if (response.getCode() == DockerStatusCode.START_CONTAINER_SUCCESS_CODE) {
				result = true;
			}
		} catch (IOException e) {
			logger.error(e.getStackTrace());
		}
		return result;
	}

	@Override
	public boolean killContainer(String id) {
		boolean result = false;
		try {
			HttpResponse response = httpRequester.sendPostJson(buildBaseUrl()
					+ "/containers/" + id + "/kill", null, null);
			if (response.getCode() == DockerStatusCode.KILL_CONTAINER_SUCCESS_CODE)
				result = true;
		} catch (IOException e) {
			logger.error(e.getStackTrace());
		}
		return result;
	}

	@Override
	public boolean removeContainer(String id) {
		boolean result = false;
		try {
			HttpResponse response = httpRequester.sendDelete(buildBaseUrl()
					+ "/containers/" + id, null, null);
			if (response.getCode() == DockerStatusCode.REMOVE_CONTAINER_SUCCESS_CODE)
				result = true;
		} catch (IOException e) {
			logger.error(e.getStackTrace());
		}
		return result;
	}

	@Override
	public InspectContainer inspectContainer(String id) {
		HttpResponse httpResponse = null;
		InspectContainer container = null;
		try {
			httpResponse = httpRequester.sendGet(buildBaseUrl()
					+ "/containers/" + id + "/json", null, null);
			container = gson.fromJson(httpResponse.getContent(),
					InspectContainer.class);
		} catch (Exception e) {
			logger.error(e.getStackTrace());
		}
		return container;
	}

	@Override
	public List<Container> listContainer() {
		List<Container> containers = new ArrayList<Container>();
		try {
			HttpResponse response = httpRequester.sendGet(buildBaseUrl()
					+ "/containers/json", null, null);
			containers = gson.fromJson(response.getContent(),
					new TypeToken<List<Container>>() {
					}.getType());
		} catch (Exception e) {
			logger.error(e.getStackTrace());
		}
		return containers;
	}

	public DockerDaemonPort getDockerDaemonPort() {
		return dockerDaemonPort;
	}

	public void setDockerDaemonPort(DockerDaemonPort dockerDaemonPort) {
		this.dockerDaemonPort = dockerDaemonPort;
	}
}
