package com.platform.spark.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class MockRealTimeData extends Thread {
	
	private static final Random random = new Random();
	private static final String[] provinces = new String[]{"Jiangsu", "Hubei", "Hunan", "Henan", "Hebei"};  
	private static final Map<String, String[]> provinceCityMap = new HashMap<String, String[]>();
	
	private Producer<Integer, String> producer;
	
	public MockRealTimeData() {
		provinceCityMap.put("Jiangsu", new String[] {"Nanjing", "Suzhou"});  
		provinceCityMap.put("Hubei", new String[] {"Wuhan", "Jingzhou"});
		provinceCityMap.put("Hunan", new String[] {"Changsha", "Xiangtan"});
		provinceCityMap.put("Henan", new String[] {"Zhengzhou", "Luoyang"});
		provinceCityMap.put("Hebei", new String[] {"Shijiazhuang", "Tangshan"});  
		
		producer = new Producer<Integer, String>(createProducerConfig());  
	}
	
	private ProducerConfig createProducerConfig() {
		Properties props = new Properties();
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		props.put("metadata.broker.list", "120.77.155.220:9092");
		return new ProducerConfig(props);
	}
	
	public void run() {
		while(true) {	
			String province = provinces[random.nextInt(5)];  
			String city = provinceCityMap.get(province)[random.nextInt(2)];
			
			String log = new Date().getTime() + " " + province + " " + city + " " 
					+ random.nextInt(1000) + " " + random.nextInt(10);  
			producer.send(new KeyedMessage<Integer, String>("AdRealTimeLog", log));  
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}  
		}
	}
	
	/**
	 * 启动Kafka Producer
	 * @param args
	 */
	public static void main(String[] args) {
		MockRealTimeData producer = new MockRealTimeData();
		producer.start();
	}
	
}
