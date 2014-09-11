package org.bench4q.docker.monitor.api;

import org.bench4q.docker.monitor.api.DiskInfo;
import org.bench4q.docker.monitor.api.MemoryInfo;
import org.bench4q.docker.monitor.api.NetworkInfo;
import org.bench4q.docker.monitor.api.ProcessorInfo;
import org.bench4q.share.models.monitor.MemoryModel;
import org.bench4q.share.models.monitor.NetworkInterfaceModel;
import org.bench4q.share.models.monitor.PhysicalDiskModel;
import org.bench4q.share.models.monitor.ProcessorModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/Monitor")
public class MonitorController {

	@RequestMapping("/PhysicalDisk")
	@ResponseBody
	public PhysicalDiskModel getDiskInfoFromPid(){
		DiskInfo diskInfo = new DiskInfo();
		PhysicalDiskModel result = new PhysicalDiskModel();
		result.setDiskReadKBytesRate(diskInfo.getdiskReadRate());
		result.setDiskWriteKBytesRate(diskInfo.getdiskWriteRate());
		return result;
	}
	
	@RequestMapping("/Memory")
	@ResponseBody
	public MemoryModel getMemoryInfoFromPid(){
		MemoryModel result = new MemoryModel();
		MemoryInfo memoryInfo = new MemoryInfo();
		result.setTotalKiloBytes(memoryInfo.getTotalKiloBytes());
		result.setMemoryUsedPercent(memoryInfo.getMemoryUsedPercent());
		return result;
	}
	@RequestMapping("/NetworkInterface")
	@ResponseBody
	public NetworkInterfaceModel getNetworkInfoFromPid(){
		NetworkInterfaceModel result = new NetworkInterfaceModel();
		NetworkInfo networkInfo = new NetworkInfo();
		result.setKiloBytesTotalPerSecond(networkInfo.getKiloBytesTotalPerSecond());
		result.setKiloBytesReceivedPerSecond(networkInfo.getKiloBytesReceivedPerSecond());
		result.setKiloBytesSentPerSecond(networkInfo.getKiloBytesSentPerSecond());
		return result;
	}
	@RequestMapping("/Processor")
	@ResponseBody
	public ProcessorModel getProcessorInfoFromPid(){
		ProcessorModel result = new ProcessorModel();
		ProcessorInfo processorInfo = new ProcessorInfo();
		result.setProcessorTimePercent(processorInfo.getProcessorTimePercent());
		return result;
	}
}
