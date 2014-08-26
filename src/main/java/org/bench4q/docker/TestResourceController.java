package org.bench4q.docker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.XStream;

class ContainerConfig{
	String cpuSet;
	long memoryKB;
	public String getCpuSet() {
		return cpuSet;
	}
	public void setCpuSet(String cpuSet) {
		this.cpuSet = cpuSet;
	}
	public long getMemoryKB() {
		return memoryKB;
	}
	public void setMemoryKB(long memoryKB) {
		this.memoryKB = memoryKB;
	}
}

public class TestResourceController {
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
		
		testResourceController.getCurrentResourceStatus();
		System.out.println("container number: " + testResourceController.getContainerList().size());
		//testResource.setMemoryLimit(256*1024*);
//		int requestCount = 8;
//		for(int i = 0; i < requestCount; ++i){
//			container = testResourceController.createContainer(testResource);
//			if(container != null){
//				System.out.println("success");
//				System.out.println("id: "+container.getId());
//				System.out.println("port: "+container.getPort());
//				testResourceController.remove(container);
//			}
//			else {
//				System.out.println("fail");
//			}
//		}
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
		return ResourceNode.getInstance().getCurrentStatus();
	}
	
	/**
	 * @return the container created
	 */
	public Container createContainer(RequestResource resource){
		Container container = new Container();
		String poolResponse = ResourceNode.getInstance().requestResource(resource);
		if(poolResponse != null){
			ContainerConfig containerConfig = new ContainerConfig();
			containerConfig.setCpuSet(poolResponse);
			containerConfig.setMemoryKB(resource.getMemoryLimit());
			container.setId(createContainerWithConfig(containerConfig));
		}
		else
			return null;
		
		if(container.getId() != null){
			if(startContainerById(container.getId()) == 0)
				return null;
		}
		
		return inspectContainer(container.getId());
	}
	
	private int startContainerById(String id){
		StartContainer startContainer = new StartContainer();
		List<String> ports = new ArrayList<String>();
		ports.add("0");
		startContainer.setPortbindings(ports);
		HttpPost httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/" + id + "/start");
		HttpEntity httpEntity = new StringEntity(gson.toJson(startContainer), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		
		if(getResponseStatusCode(httpPost) == START_CONTAINER_SUCCESS_CODE)
			return 1;
		else
			return 0;
	}
	
	private String createContainerWithConfig(ContainerConfig containerConfig){
		String id = null;
		HttpPost httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/create");
		CreateContainer createContainer = new CreateContainer();
		createContainer.setImage(config.getImageName());
		createContainer.setCpuset(containerConfig.getCpuSet());
		createContainer.setMemory(containerConfig.getMemoryKB()*1024);
		HttpEntity httpEntity = new StringEntity(gson.toJson(createContainer), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		
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
	/**
	 * 
	 * @return container info
	 */
	public Container inspectContainer(String id){
		HttpGet httpGet = new HttpGet(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+id+"/json");
		InspectContainer inspectContainer = new InspectContainer();
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == INSPECT_CONTAINER_SUCCESS_CODE){
				inspectContainer = gson.fromJson(EntityUtils.toString(response.getEntity(), "utf-8"), 
						InspectContainer.class);
				inspectContainer.setIp(config.getDockerHostIp());
				inspectContainer.setPort(inspectContainer.getHostPort());
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
			ResourceNode.getInstance().releaseResource(container);
			return true;
		}
		else {
			return false;
		}
	}
	private int killContainerPost(Container container){
		return getResponseStatusCode(new HttpPost(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+
					container.getId()+"/kill"));
	}
	
	private int removeContainerPost(Container container){
		return getResponseStatusCode(new HttpDelete(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+
					container.getId()));
	}
	
	private int getResponseStatusCode(HttpUriRequest request){
		try {
			return httpClient.execute(request).getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public List<Container> getContainerList(){
		List<Container> result = new ArrayList<Container>();
		HttpGet httpGet = new HttpGet(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/json");
		try {
			String entity = EntityUtils.toString(httpClient.execute(httpGet).getEntity(), "utf-8");
			result = gson.fromJson(entity, new TypeToken<List<Container>>(){}.getType());
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
//	public boolean remove(String id){
//		if(killContainerPost(id) == KILL_CONTAINER_SUCCESS_CODE
//				&& removeContainerPost(id) == REMOVE_CONTAINER_SUCCESS_CODE){
//			ResourcePool.getInstance().releaseResource(id);
//			return true;
//		}
//		else {
//			return false;
//		}
//	}
//	
//	private int killContainerPost(String id){
//		HttpPost httpPost = new HttpPost(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+
//				id+"/kill");
//		int statusCode = 0;
//		try {
//			CloseableHttpResponse response = httpClient.execute(httpPost);
//			statusCode = response.getStatusLine().getStatusCode();
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return statusCode;
//	}
//	
//	private int removeContainerPost(String id){
//		HttpDelete httpDelete = new HttpDelete(APIPROTOCOL_STRING+config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+
//				id);
//		int statusCode = 0;
//		try {
//			CloseableHttpResponse response = httpClient.execute(httpDelete);
//			statusCode = response.getStatusLine().getStatusCode();
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return statusCode;
//	}
	

}
