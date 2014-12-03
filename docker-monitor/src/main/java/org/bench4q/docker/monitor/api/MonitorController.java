package org.bench4q.docker.monitor.api;

import org.bench4q.docker.monitor.model.DiskInfo;
import org.bench4q.docker.monitor.model.JvmInfo;
import org.bench4q.docker.monitor.model.MemoryInfo;
import org.bench4q.docker.monitor.model.NetworkInfo;
import org.bench4q.docker.monitor.model.ProcessorInfo;
import org.bench4q.share.helper.ExceptionLog;
import org.bench4q.share.helper.MarshalHelper;
import org.bench4q.share.master.test.resource.ResourceInfoModel;
import org.bench4q.share.models.monitor.HealthModel;
import org.bench4q.share.models.monitor.JvmModel;
import org.bench4q.share.models.monitor.MemoryModel;
import org.bench4q.share.models.monitor.MonitorMain;
import org.bench4q.share.models.monitor.NetworkInterfaceModel;
import org.bench4q.share.models.monitor.PhysicalDiskModel;
import org.bench4q.share.models.monitor.ProcessorModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/monitor")
public class MonitorController {
	MemoryInfo memoryInfo;
	ProcessorInfo processorInfo;
	DiskInfo diskInfo = new DiskInfo();
	JvmInfo jvmInfo = new JvmInfo();
	private NetworkInfo networkInfo = new NetworkInfo();
	private ResourceInfoModel resourceInfo;

	@RequestMapping("/all")
	@ResponseBody
	public MonitorMain getMainModel() {
		try {
			MonitorMain result = new MonitorMain();
			result.setMemoryModel(getMemoryModel());
			result.setPhysicalDiskModel(getPhysicalDiskModel());
			result.setProcessorModel(getProcessorModel());
			result.setJvmModel(getJvmModel());
			result.setHealthModel(new HealthModel(resourceInfo
					.getHealthThreshold()));
			result.checkHealth();
			result.setNetworkInterfaceModel(getNetworkInterfaceModel());
			return result;
		} catch (Exception e) {
			ExceptionLog.getStackTrace(e);
			return null;
		}
	}

	@RequestMapping("/networkInterface")
	@ResponseBody
	public NetworkInterfaceModel getNetworkInterfaceModel() {
		NetworkInterfaceModel result = new NetworkInterfaceModel();
		result.setKiloBytesReceivedPerSecond(networkInfo
				.getKiloBytesReceivedPerSecond());
		result.setKiloBytesSentPerSecond(networkInfo
				.getKiloBytesSentPerSecond());
		result.setKiloBytesTotalPerSecond(networkInfo
				.getKiloBytesTotalPerSecond());
		return result;
	}

	@RequestMapping("/physicalDisk")
	@ResponseBody
	public PhysicalDiskModel getPhysicalDiskModel() {
		PhysicalDiskModel result = new PhysicalDiskModel();
		result.setDiskReadKBytesRate(diskInfo.getdiskReadRate());
		result.setDiskWriteKBytesRate(diskInfo.getdiskWriteRate());
		return result;
	}

	@RequestMapping("/memory")
	@ResponseBody
	public MemoryModel getMemoryModel() {
		MemoryModel result = new MemoryModel();
		result.setTotalKiloBytes(memoryInfo.getTotalKiloBytes());
		result.setMemoryUsedPercent(memoryInfo.getMemoryUsedPercent());
		return result;
	}

	@RequestMapping("/processor")
	@ResponseBody
	public ProcessorModel getProcessorModel() {
		ProcessorModel result = new ProcessorModel();
		result.setProcessorTimePercent(processorInfo.getProcessorTimePercent());
		result.setSize(processorInfo.getSize());
		return result;
	}

	@RequestMapping("/jvm")
	@ResponseBody
	public JvmModel getJvmModel() {
		return jvmInfo.getJvmInfo();
	}

	@RequestMapping(value = "/setResourceInfo", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	synchronized public void setResourceInfo(
			@RequestBody ResourceInfoModel resourceInfo) {
		System.out.println(MarshalHelper.tryMarshal(resourceInfo));
		this.resourceInfo = resourceInfo;
		this.processorInfo = new ProcessorInfo(resourceInfo);
		this.memoryInfo = new MemoryInfo(resourceInfo);
	}
}
