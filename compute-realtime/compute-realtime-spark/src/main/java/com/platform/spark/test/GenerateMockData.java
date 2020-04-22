package com.platform.spark.test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.UUID;

import com.platform.spark.util.DateUtils;
import com.platform.spark.util.StringUtils;

/**
 * 模拟数据程序
 * @author wulinhao
 *
 */
public class GenerateMockData {

	/**
	 * 模拟数据
	 */
	public static void main(String[] args) { 
		BufferedWriter bw = null;
		try {
			String file = "C://Users//Administrator//Desktop//user_visit_action.txt";
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true)));
			
			String[] searchKeywords = new String[] {"火锅", "蛋糕", "重庆辣子鸡", "重庆小面",
					"呷哺呷哺", "新辣道鱼火锅", "国贸大厦", "太古商场", "日本料理", "温泉"};
			String date = DateUtils.getTodayDate();
			String[] actions = new String[]{"search", "click", "order", "pay"};
			Random random = new Random();
			
			for(int i = 0; i < 100; i++) {
				long userid = random.nextInt(100);    
				
				for(int j = 0; j < 10; j++) {
					String sessionid = UUID.randomUUID().toString().replace("-", "");  
					String baseActionTime = date + " " + random.nextInt(23);
					
					Long clickCategoryId = null;
					  
					for(int k = 0; k < random.nextInt(100); k++) {
						long pageid = random.nextInt(10);    
						String actionTime = baseActionTime + ":" + StringUtils.fulfuill(String.valueOf(random.nextInt(59))) + ":" + StringUtils.fulfuill(String.valueOf(random.nextInt(59)));
						String searchKeyword = null;
						Long clickProductId = null;
						String orderCategoryIds = null;
						String orderProductIds = null;
						String payCategoryIds = null;
						String payProductIds = null;
						
						String action = actions[random.nextInt(4)];
						if("search".equals(action)) {
							searchKeyword = searchKeywords[random.nextInt(10)];   
						} else if("click".equals(action)) {
							if(clickCategoryId == null) {
								clickCategoryId = Long.valueOf(String.valueOf(random.nextInt(100)));    
							}
							clickProductId = Long.valueOf(String.valueOf(random.nextInt(100)));  
						} else if("order".equals(action)) {
							orderCategoryIds = String.valueOf(random.nextInt(100));  
							orderProductIds = String.valueOf(random.nextInt(100));
						} else if("pay".equals(action)) {
							payCategoryIds = String.valueOf(random.nextInt(100));  
							payProductIds = String.valueOf(random.nextInt(100));
						}
						
						bw.write(date + "" + userid + "" + sessionid +
								"" + pageid + "" + actionTime + "" + searchKeyword +
								"" + clickCategoryId + "" + clickProductId +
								"" + orderCategoryIds + "" + orderProductIds +
								"" + payCategoryIds + "" + payProductIds + 
								"" + random.nextInt(10) + "\n");  
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		}
		
		/**
		 * ==================================================================
		 */
		try {
			String file = "C://Users//Administrator//Desktop//user_info.txt";
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true)));
		
			Random random = new Random();
			String[] sexes = new String[]{"male", "female"};
			for(int i = 0; i < 100; i ++) {
				long userid = i;
				String username = "user" + i;
				String name = "name" + i;
				int age = random.nextInt(60);
				String professional = "professional" + random.nextInt(100);
				String city = "city" + random.nextInt(100);
				String sex = sexes[random.nextInt(2)];
				
				bw.write(userid + "" + username + "" + name + "" + age +
						"" + professional + "" + city + "" + sex + "\n");  
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * ==================================================================
		 */
		
		try {
			String file = "C://Users//Administrator//Desktop//product_info.txt";
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true)));
		
			Random random = new Random();
			int[] productStatuses = new int[]{0, 1};
			for(int i = 0; i < 100; i ++) {
				long productid = i;
				String productName = "product" + i;
				int productStatus = productStatuses[random.nextInt(2)];
				String extendInfo = "{\"product_status\":" + productStatus + "}";
				bw.write(productid + "" + productName + "" + extendInfo + "\n");  
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
