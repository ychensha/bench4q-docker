package org.bench4q.docker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;


public class TestResourceController {
	/*test attribute*/
	private long freeMemory = 2*1024*1024;
	/**/
	private ResourceControllerConfig config;
	private static final String APIPROTOCOL_STRING = "http://";
	private static final int CREATE_CONTAINER_SUCCESS_CODE = 201;
	private static final int START_CONTAINER_SUCCESS_CODE =204;
	private static final int INSPECT_CONTAINER_SUCCESS_CODE = 200;
	private static final int KILL_CONTAINER_SUCCESS_CODE = 204;
	private static final int REMOVE_CONTAINER_SUCCESS_CODE = 204;
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public static void main(String[] args){
		TestResourceController testResourceController = new TestResourceController();
		RequestResource testResource = new RequestResource();
		Container container = null;
		testResource.setCpuNumber(1);
		//testResource.setMemoryLimit(256*1024*);
		int requestCount = 8;
		for(int i = 0; i < requestCount; ++i){
			container = testResourceController.createContainer(testResource);
			if(container != null){
				System.out.println("success");
				System.out.println("id: "+container.getId());
				System.out.println("port: "+container.getPort());
				testResourceController.remove(container);
			}
			else {
				System.out.println("fail");
			}
		}
	}
	
	public TestResourceController(){
		XStream xStream = new XStream();
		InputStream in = null;
		try {
			in = new FileInputStream("./config.xml");
			config = (ResourceControllerConfig) xStream.fromXML(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @return current resource status
	 */
	public Resource getCurrentResourceStatus(){
		return ResourcePool.getInstance().getCurrentStatus();
	}
	
	/**
	 * @return the container created
	 */
	public Container createContainer(RequestResource resource){
		Container container = new Container();
		HttpEntity httpEntity = null;
		HttpPost httpPost = null;
		//to set the config of the container
		String poolResponse = ResourcePool.getInstance().requestResource(resource);
		if(poolResponse != null){
			httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/create");
			CreateContainer createContainer = new CreateContainer();
			createContainer.setImage(config.getImageName());
			createContainer.setCpuset(poolResponse);
			httpEntity = new StringEntity(gson.toJson(createContainer), ContentType.APPLICATION_JSON);
			httpPost.setEntity(httpEntity);
			container.setId(createContainerPost(httpPost));
		}
		else
			return null;
		
		//to configure a container and start it
		if(container.getId() != null){
			StartContainer startContainer = new StartContainer();
			List<String> ports = new ArrayList<String>();
			ports.add("0");
			startContainer.setPortbindings(ports);
			httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/" + container.getId() + "/start");
			httpEntity = new StringEntity(gson.toJson(startContainer), ContentType.APPLICATION_JSON);
			httpPost.setEntity(httpEntity);
			int startResponse = startContainerPost(httpPost);
			if(startResponse == 0)
				return null;
		}
		
		//set container's port
		return inspectContainer(container.getId());
	}
	
	/**
	 * 
	 * @param httpPost passed from createContainer
	 * @return container id, null if failed
	 */
	private String createContainerPost(HttpPost httpPost){
		String id = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == CREATE_CONTAINER_SUCCESS_CODE){
				id = EntityUtils.toString(response.getEntity(), "utf-8");
				CreateContainerResponse createContainerResponse = gson.fromJson(id, CreateContainerResponse.class);
				id = createContainerResponse.getId();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return id;
	}
	
	private int startContainerPost(HttpPost httpPost){
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			if(response.getStatusLine().getStatusCode() == START_CONTAINER_SUCCESS_CODE){
				return 1;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	/**
	 * 
	 * @return container info
	 */
	public Container inspectContainer(String id){
		HttpGet httpGet = new HttpGet(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+id+"/json");
		String result = null;
		InspectContainer inspectContainer = new InspectContainer();
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == INSPECT_CONTAINER_SUCCESS_CODE){
				result = EntityUtils.toString(response.getEntity(), "utf-8");
				inspectContainer = gson.fromJson(result, InspectContainer.class);
				inspectContainer.setIp(config.getDockerHostIp());
				inspectContainer.setPort(inspectContainer.getHostConfig().getPortBindings().getHostPortList().get(0).getHostPort());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return inspectContainer;
	}
	/**
	 * Remove Given Container
	 * @param container the container to be removed
	 * @return true if succeed
	 */
	public boolean remove(Container container){
		if(killContainerPost(container) == KILL_CONTAINER_SUCCESS_CODE
				&& removeContainerPost(container) == REMOVE_CONTAINER_SUCCESS_CODE){
			ResourcePool.getInstance().releaseResource(container);
			return true;
		}
		else {
			return false;
		}
	}
	
	private int killContainerPost(Container container){
		HttpPost httpPost = new HttpPost(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+
				container.getId()+"/kill");
		int statusCode = 0;
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			statusCode = response.getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return statusCode;
	}
	
	private int removeContainerPost(Container container){
		HttpDelete httpDelete = new HttpDelete(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+
				container.getId());
		int statusCode = 0;
		try {
			CloseableHttpResponse response = httpClient.execute(httpDelete);
			statusCode = response.getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return statusCode;
	}
}
