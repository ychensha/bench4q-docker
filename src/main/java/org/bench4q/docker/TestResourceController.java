package org.bench4q.docker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.w3c.dom.ProcessingInstruction;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

class Cpu{
	int usage;
	int cpuId;
	public int getUsage() {
		return usage;
	}
	public void setUsage(int usage) {
		this.usage = usage;
	}
	public int getCpuId() {
		return cpuId;
	}
	public void setCpuId(int cpuId) {
		this.cpuId = cpuId;
	}
}

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
	
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public static void main(String[] args){
		TestResourceController testResourceController = new TestResourceController();
//		String config = testResourceController.config.getDockerHostPort()+
//				testResourceController.config.getImageName()+
//				testResourceController.config.getVcpuRatio();
//		System.out.println(config);
		TestResource testResource = new TestResource();
		testResource.setCpuNumber(1);
		//testResource.setMemoryLimit(256*1024*);
//		int requestCount = 1;
//		for(int i = 0; i < requestCount; ++i){
//			if(testResourceController.createContainer(testResource) != null)
//				System.out.println("success");
//			else {
//				System.out.println("fail");
//			}
//		}
		Container container = testResourceController.inspectContainer("4cac0759f940");
		System.out.println(container.getId());
		System.out.println(container.getPort());
		//System.out.println(testResourceController.getProcessor().size());
		//System.out.println(testResourceController.getCurrentResourceStatus().getMemFree());
		//System.out.println(testResourceController.createContainer(null).getContaiderId());
	}
	
	public TestResourceController(){
		//avalCpu = Runtime.getRuntime().availableProcessors();
		avalCpu = 2;
		processor = new PriorityQueue<Cpu>(avalCpu, new Comparator<Cpu>() {
			public int compare(Cpu c1, Cpu c2){
				return c1.getUsage() - c2.getUsage();
			}
		});
		for(int i = 0; i < avalCpu; ++i){
			Cpu cpu = new Cpu();
			cpu.setCpuId(i);
			cpu.setUsage(0);
			processor.add(cpu);
		}
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
		HttpPost httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/create");
		//to set the config of the container
		CreateContainer createContainer = new CreateContainer();
		StringBuilder stringBuilder = new StringBuilder();
		createContainer.setImage(config.getImageName());
		
		long testCpuNumber = resource.getCpuNumber();
		long testMemoryLimit = resource.getMemoryLimit();
		//getCurrentResourceStatus().getMemFree()*1024 >= testMemoryLimit
		if(avalCpu >= testCpuNumber &&
				true){
			for(int i = 0; i < testCpuNumber; ++i){
				Cpu cpu = processor.poll();
				cpu.setUsage(cpu.getUsage() + 1);
				stringBuilder.append(cpu.getCpuId()+",");
				processor.add(cpu);
			}
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			avalCpu -= resource.getCpuNumber();
			createContainer.setCpuset(stringBuilder.toString());
		}
		else {
			return null;
		}
		HttpEntity httpEntity = new StringEntity(gson.toJson(createContainer), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		String result = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			result = EntityUtils.toString(response.getEntity(), "utf-8");
			System.out.println(response.getStatusLine().getStatusCode());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//get container id
		CreateContainerResponse createContainerResponse;
		try {
			createContainerResponse = gson.fromJson(result, CreateContainerResponse.class);
			result = createContainerResponse.getId();
			container.setId(result);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		//to configure a container and start it
		StartContainer startContainer = new StartContainer();
		List<String> ports = new ArrayList<String>();
		ports.add("0");
		startContainer.setPortbindings(ports);
		httpEntity = new StringEntity(gson.toJson(startContainer), ContentType.APPLICATION_JSON);
		httpPost = new HttpPost(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort() + "/containers/" + container.getId() + "/start");
		httpPost.setEntity(httpEntity);
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			System.out.println(response.getStatusLine().getStatusCode());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return container;
	}
	/**
	 * 
	 * @return container info
	 */
	private Container inspectContainer(String id){
		HttpGet httpGet = new HttpGet(APIPROTOCOL_STRING + config.getDockerHostIp()+":"+config.getDockerHostPort()+"/containers/"+id+"/json");
		String result = null;
		Container container = null;
		InspectContainer inspectContainer = new InspectContainer();
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			if(response.getStatusLine().getStatusCode() == 200){
				result = EntityUtils.toString(response.getEntity(), "utf-8");
				System.out.println(result);
				inspectContainer = gson.fromJson(result, InspectContainer.class);
				System.out.println(inspectContainer.getArgs().get(0));
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
