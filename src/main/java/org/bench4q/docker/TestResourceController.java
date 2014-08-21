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
	private Queue<Cpu> processor;
	private int avalCpu = 0;
	private ResourceControllerConfig config;
	private static final String APIPROTOCOL_STRING = "http://";
	private static final String PROCFS_MEMINFO = "/proc/meminfo";
	private static final String MEMTOTAL_STRING = "MemTotal";
	private static final String MEMFREE_STRING = "MemFree";
	private static final Pattern PROCFS_MEMFILE_FORMAT = Pattern.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB");
	private static final int CREATE_CONTAINER_SUCCESS_CODE = 201;
	private static final int START_CONTAINER_SUCCESS_CODE =204;
	private static final int INSPECT_CONTAINER_SUCCESS_CODE = 200;
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public static void main(String[] args){
		TestResourceController testResourceController = new TestResourceController();
//		String config = testResourceController.config.getDockerHostPort()+
//				testResourceController.config.getImageName()+
//				testResourceController.config.getVcpuRatio();
//		System.out.println(config);
		TestResource testResource = new TestResource();
		Container container = null;
		testResource.setCpuNumber(1);
		//testResource.setMemoryLimit(256*1024*);
		int requestCount = 7;
		for(int i = 0; i < requestCount; ++i){
			container = testResourceController.createContainer(testResource);
			if(container != null){
				System.out.println("success");
				System.out.println("id: "+container.getId());
				System.out.println("port: "+container.getPort());
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
		avalCpu *= config.getVcpuRatio();
	}
	public Queue<Cpu> getProcessor() {
		return processor;
	}

	public void setProcessor(Queue<Cpu> processor) {
		this.processor = processor;
	}

	/**
	 * 
	 * @return current resource status
	 */
	public Resource getCurrentResourceStatus(){
		Resource resource = new Resource();
		BufferedReader bufferedReader = null;
		Matcher mat = null;
		
		//read meminfo
		try {
			bufferedReader = new BufferedReader(new FileReader(PROCFS_MEMINFO));
			String line = null;
			while((line = bufferedReader.readLine()) != null){
				mat = PROCFS_MEMFILE_FORMAT.matcher(line);
				if(mat.find()){
					if(mat.group(1).equals(MEMTOTAL_STRING))
						resource.setMemFree(Long.parseLong(mat.group(2)));
					else if(mat.group(1).equals(MEMFREE_STRING))
						resource.setMemFree(Long.parseLong(mat.group(2)));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//close the reader
		try {
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resource;
	}
	
	/**
	 * @return the container created
	 */
	public Container createContainer(TestResource resource){
		Container container = new Container();
		HttpEntity httpEntity = null;
		HttpPost httpPost = null;
		//to set the config of the container
		String poolResponse = ResourcePool.getInstance().requestResource(resource);
		if(poolResponse != null){
			httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/create");
			CreateContainer createContainer = new CreateContainer();
			createContainer.setImage(config.getImageName());
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
			System.out.println(startResponse);
			if(startResponse==0)
				return null;
		}
		
		//set container's port
		container.setPort(inspectContainer(container.getId()).getPort());
		return container;
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
	private Container inspectContainer(String id){
		HttpGet httpGet = new HttpGet(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+id+"/json");
		String result = null;
		Container container = new Container();
		InspectContainer inspectContainer = new InspectContainer();
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == INSPECT_CONTAINER_SUCCESS_CODE){
				result = EntityUtils.toString(response.getEntity(), "utf-8");
				inspectContainer = gson.fromJson(result, InspectContainer.class);
				container.setId(inspectContainer.getId());
				container.setPort(config.getDockerHostIp()+":"+inspectContainer.getHostConfig().getPortBindings().getHostPortList().get(0).getHostPort());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return container;
	}
	/**
	 * Remove Given Container
	 * @param container the container to be removed
	 * @return operation code
	 */
	public int remove(Container container){
		return 0;
	}
}
