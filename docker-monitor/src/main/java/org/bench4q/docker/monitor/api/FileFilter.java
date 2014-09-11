package org.bench4q.docker.monitor.api;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class FileFilter implements FilenameFilter{
	private Pattern pattern;
	public FileFilter(String regExp){
		pattern = Pattern.compile(regExp);
	}
	public boolean accept(File dir, String name) {
		return pattern.matcher(name).find();
	}
	
	public static void main(String[] args){
		File dir = new File("C:/Users/Chen/Documents/GitHub/docker-monitor/target");
		FileFilter filter = new FileFilter("^[0-9]*$");
		File[] numberNameFile = dir.listFiles(filter);
		for(File file : numberNameFile){
			System.out.println(file.getName());
		}
	}
}
