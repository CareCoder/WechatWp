package com.yq.wechat.web.utils;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class FileUtil {
	
	/**
	 * 读取配置文件
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> loadConfigFile(String path) throws Exception{
		Reader read = new FileReader(path);
		Properties pro = new Properties();
		pro.load(read);
		read.close();
		
		Map<String, String> map = new HashMap<String, String>();
		for(Entry<Object, Object> entry : pro.entrySet()){
			map.put(String.valueOf(entry.getKey()).trim(), String.valueOf(entry.getValue()).trim());
		}
		return map;
	}
	
}
