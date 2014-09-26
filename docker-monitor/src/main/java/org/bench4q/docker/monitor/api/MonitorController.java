package org.bench4q.docker.monitor.api;

import org.bench4q.docker.monitor.api.DiskInfo;
import org.bench4q.docker.monitor.api.MemoryInfo;
import org.bench4q.docker.monitor.api.NetworkInfo;
import org.bench4q.docker.monitor.api.ProcessorInfo;
import org.bench4q.share.models.monitor.MemoryModel;
import org.bench4q.share.models.monitor.MonitorMain;
import org.bench4q.share.models.monitor.NetworkInterfaceModel;
import org.bench4q.share.models.monitor.PhysicalDiskModel;
import org.bench4q.share.models.monitor.ProcessorModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/monitor")
public class MonitorController {
	MemoryInfo memoryInfo = new MemoryInfo();
	NetworkInfo networkInfo = new NetworkInfo();
	ProcessorInfo processorInfo = new ProcessorInfo();
	DiskInfo diskInfo = new DiskInfo();
	@RequestMapping("/all")
	@ResponseBody
	public MonitorMain getMainModel(){
		MonitorMain result = new MonitorMain();
		result.setMemoryModel(getMemoryModel());
		result.setNetworkInterfaceModel(getNetworkInterfaceModel());
		result.setPhysicalDiskModel(getPhysicalDiskModel());
		result.setProcessorModel(getProcessorModel());
		return result;
	}
	
	@RequestMapping("/physicalDisk")
	@ResponseBody
	public PhysicalDiskModel getPhysicalDiskModel(){
		PhysicalDiskModel result = new PhysicalDiskModel();
		result.setDiskReadKBytesRate(diskInfo.getdiskReadRate());
		result.setDiskWriteKBytesRate(diskInfo.getdiskWriteRate());
		return result;
	}
	
	@RequestMapping("/memory")
	@ResponseBody
	public MemoryModel getMemoryModel(){
		MemoryModel result = new MemoryModel();
		result.setTotalKiloBytes(memoryInfo.getTotalKiloBytes());
		result.setMemoryUsedPercent(memoryInfo.getMemoryUsedPercent());
		return result;
	}
	@RequestMapping("/networkInterface")
	@ResponseBody
	public NetworkInterfaceModel getNetworkInterfaceModel(){
		NetworkInterfaceModel result = new NetworkInterfaceModel();
		result.setKiloBytesTotalPerSecond(networkInfo.getKiloBytesTotalPerSecond());
		result.setKiloBytesReceivedPerSecond(networkInfo.getKiloBytesReceivedPerSecond());
		result.setKiloBytesSentPerSecond(networkInfo.getKiloBytesSentPerSecond());
		return result;
	}
	@RequestMapping("/processor")
	@ResponseBody
	public ProcessorModel getProcessorModel(){
		ProcessorModel result = new ProcessorModel();
		result.setProcessorTimePercent(processorInfo.getProcessorTimePercent());
		result.setSize(processorInfo.getSize());
		return result;
	}
}
