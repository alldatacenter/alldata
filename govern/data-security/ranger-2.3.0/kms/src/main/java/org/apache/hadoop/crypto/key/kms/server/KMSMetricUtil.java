/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.crypto.key.kms.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.crypto.key.KeyProviderCryptoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class KMSMetricUtil {
	private static final Logger logger = LoggerFactory.getLogger(KMSMetricUtil.class);

	private static final String HSM_ENABLED = "ranger.ks.hsm.enabled";
	private static String metricType;
	
	public static void main(String[] args) {
		/* LOG4J2: TODO
		logger.getRootLogger().setLevel(Level.OFF);
		 */
		logger.info("KMSMetricUtil : main()");
		if(args.length != 2){
			System.out.println("type: Incorrect Arguments usage : For KMSMetric Usage: metric -type  hsmenabled | encryptedkey | encryptedkeybyalgorithm");
		}
		else
		{
			if(!(args[0].equalsIgnoreCase("-type")) || !(args[1].equalsIgnoreCase("hsmenabled") || args[1].equalsIgnoreCase("encryptedkey") || args[1].equalsIgnoreCase("encryptedkeybyalgorithm"))){
				System.out.println("type: Incorrect Arguments usage : For KMSMetric Usage: metric -type  hsmenabled | encryptedkey | encryptedkeybyalgorithm");	
			}
			else {
				metricType = args[1];
				if(logger.isDebugEnabled()){
					logger.debug("KMSMetric Type : " + metricType);
				}
			}
		}
		KMSMetricUtil kmsmetricutil = new KMSMetricUtil();
		kmsmetricutil.getKMSMetricCalculation(metricType);
	}

	
	@SuppressWarnings("static-access")
	private void getKMSMetricCalculation(String caseValue) {
		logger.info("Metric Type : " + caseValue);
		try {
			switch (caseValue.toLowerCase()) {
			case "hsmenabled":
				try {
					KMSConfiguration kmsConfig = new KMSConfiguration();
					if (kmsConfig != null && kmsConfig.getACLsConf() != null) {
						String hsmEnabledValue = kmsConfig.getACLsConf().get(HSM_ENABLED);
						Map<String, String> hsmEnabledMap = new HashMap<String, String>();
						if (hsmEnabledValue != null) {
							hsmEnabledMap.put("hsmEnabled", hsmEnabledValue);
							Gson gson = new GsonBuilder().create();
							final String jsonHSMEnabled = gson.toJson(hsmEnabledMap);
							System.out.println(jsonHSMEnabled);
						} else {
							hsmEnabledMap.put("hsmEnabled", "");
							Gson gson = new GsonBuilder().create();
							final String jsonHSMEnabled = gson.toJson(hsmEnabledMap);
							System.out.println(jsonHSMEnabled);
						}
					}
				} catch (Exception e) {
					logger.error("Error calculating KMSMetric for HSM enabled : " + e.getMessage());
				}
				break;
			case "encryptedkey":
				try {
					KMSWebApp kmsWebAppEncryptedKey = new KMSWebApp();
					if (kmsWebAppEncryptedKey != null) {
						kmsWebAppEncryptedKey.contextInitialized(null);
						KeyProviderCryptoExtension keyProvider = kmsWebAppEncryptedKey.getKeyProvider();
						if (keyProvider != null && keyProvider.getKeys() != null) {
							Integer encryptedKeyCount = keyProvider.getKeys().size();
							Map<String, Integer> encryptedKeyCountValueMap = new HashMap<String, Integer>();
							encryptedKeyCountValueMap.put("encryptedKeyCount", encryptedKeyCount);
							Gson gson = new GsonBuilder().create();
							final String jsonEncKeycount = gson.toJson(encryptedKeyCountValueMap);
							System.out.println(jsonEncKeycount);
						} else {
							Map<String, String> encryptedKeyCountValueMap = new HashMap<String, String>();
							encryptedKeyCountValueMap.put("encryptedKeyCount", "");
							Gson gson = new GsonBuilder().create();
							final String jsonEncKeycount = gson.toJson(encryptedKeyCountValueMap);
							System.out.println(jsonEncKeycount);
						}
						kmsWebAppEncryptedKey.contextDestroyed(null);
					}
				} catch (Exception e) {
					logger.error("Error calculating KMSMetric for encrypted key count: " + e.getMessage());
				}
				break;
			case "encryptedkeybyalgorithm":
				try {
					KMSWebApp kmsWebApp = new KMSWebApp();
					if (kmsWebApp != null) {
						kmsWebApp.contextInitialized(null);
						KeyProviderCryptoExtension keyProvider = kmsWebApp.getKeyProvider();
						Map<String, Integer> encryptedKeyByAlgorithmCountMap = new HashMap<String, Integer>();
						int count = 0;
						if (keyProvider != null && keyProvider.getKeys() != null && keyProvider.getKeys().size() > 0) {
							List<String> keyList = new ArrayList<String>();
							keyList.addAll(keyProvider.getKeys());
							if (keyList != null) {								
								for (String key : keyList) {
									String algorithmName = keyProvider.getMetadata(key).getCipher();
									if (encryptedKeyByAlgorithmCountMap.containsKey(algorithmName)) {
										count = encryptedKeyByAlgorithmCountMap.get(algorithmName);
										count += 1;
										encryptedKeyByAlgorithmCountMap.put(algorithmName, count);
									} else {
										encryptedKeyByAlgorithmCountMap.put(algorithmName, 1);
									}
								}
								Gson gson = new GsonBuilder().create();
								final String jsonEncKeyByAlgo = gson.toJson(encryptedKeyByAlgorithmCountMap);
								System.out.println(jsonEncKeyByAlgo);
							}							
						} else {
							encryptedKeyByAlgorithmCountMap.put("encryptedKeyByAlgorithm", count);
							Gson gson = new GsonBuilder().create();
							final String jsonEncKeyByAlgo = gson.toJson(encryptedKeyByAlgorithmCountMap);
							System.out.println(jsonEncKeyByAlgo);
						}
						kmsWebApp.contextDestroyed(null);
					}
				} catch (IOException e) {
					logger.error("Error calculating KMSMetric for encrypted key by algorithm : " + e.getMessage());
				}
				break;
			default:
				System.out.println("type: Incorrect Arguments usage : For KMSMetric Usage: metric -type  hsmenabled | encryptedkey | encryptedkeybyalgorithm");
				break;
			}
		} catch (Exception e) {
			logger.error("Error calculating KMSMetric : " + e.getMessage());
		}
	}
}
