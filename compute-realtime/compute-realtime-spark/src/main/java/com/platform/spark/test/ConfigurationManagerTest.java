package com.platform.spark.test;

import com.platform.spark.conf.ConfigurationManager;

/**
 * 配置管理组件测试类
 * @author wulinhao
 *
 */
public class ConfigurationManagerTest {

	public static void main(String[] args) {
		String testkey1 = ConfigurationManager.getProperty("testkey1");
		String testkey2 = ConfigurationManager.getProperty("testkey2");  
		System.out.println(testkey1);  
		System.out.println(testkey2);  
	}
	
}
