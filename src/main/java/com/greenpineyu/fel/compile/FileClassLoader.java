package com.greenpineyu.fel.compile;

import com.greenpineyu.fel.util.FelSwitcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileClassLoader extends ClassLoader {

	private static final Logger log = LoggerFactory.getLogger(FileClassLoader.class);

	private String dir;
	public static final String fileType = ".class";

	public FileClassLoader(ClassLoader arg0, String dir) {
		super(arg0);
		this.dir = dir;
	}

	public Class<?> findClass(String name) {
		byte[] data = loadClassData(name);
		return defineClass(name, data, 0, data.length);
	}

	public byte[] loadClassData(String name) {
		name = name.replaceAll("\\.", "/");
		FileInputStream fis = null;
		byte[] data = null;
		try {
			fis = new FileInputStream(new File(dir + name + fileType));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int ch;
			while ((ch = fis.read()) != -1) {
				baos.write(ch);
			}
			data = baos.toByteArray();
		} catch (IOException e) {
			if (FelSwitcher.errorLog) {
				log.error("loadClassData error, className:" + name, e);
			}
		}finally{
			if(fis!= null){
				try {
					fis.close();
				} catch (IOException e) {
					if (FelSwitcher.errorLog) {
						log.error("loadClassData error, className:" + name, e);
					}
				}
			}
		}
		return data;
	}
}
