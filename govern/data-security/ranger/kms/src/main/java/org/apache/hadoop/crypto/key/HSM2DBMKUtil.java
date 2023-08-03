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
package org.apache.hadoop.crypto.key;

import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.kms.dao.DaoManager;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class HSM2DBMKUtil {

	private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
	private static final String PARTITION_PASSWORD = "ranger.ks.hsm.partition.password";
	private static final String PARTITION_NAME = "ranger.ks.hsm.partition.name";
	private static final String HSM_TYPE = "ranger.ks.hsm.type";
	
	public static void showUsage() {
		System.err.println("USAGE: java " + HSM2DBMKUtil.class.getName() + " <HSMType> <partitionName>");
	}
	

	public static void main(String[] args) {
			if (args.length < 2) {
				System.err.println("Invalid number of parameters found.");
				showUsage();
				System.exit(1);
			}
			else {				
				String hsmType = args[0];
				if (hsmType == null || hsmType.trim().isEmpty()) {
					System.err.println("HSM Type does not exists.");
					showUsage();
					System.exit(1);
				}
				
				String partitionName = args[1];
				if (partitionName == null || partitionName.trim().isEmpty()) {
					System.err.println("Partition name does not exists.");
					showUsage();
					System.exit(1);
				}
				
				new HSM2DBMKUtil().doImportMKFromHSM(hsmType, partitionName);
				
				System.out.println("Master Key from HSM has been successfully imported into Ranger KMS DB.");
				
				System.exit(0);
				
			}
	}
	
	private void doImportMKFromHSM(String hsmType, String partitionName) {
		char[] partitionPassword=null;
		try {
			partitionPassword = ConsoleUtil.getPasswordFromConsole("Enter Password for the Partition "+partitionName+" : ");
			Configuration conf = RangerKeyStoreProvider.getDBKSConf();
			conf.set(HSM_TYPE, hsmType);
			conf.set(PARTITION_NAME, partitionName);
			conf.set(PARTITION_PASSWORD, String.valueOf(partitionPassword));
			
			RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);		
			DaoManager daoManager = rangerkmsDb.getDaoManager();
			String password = conf.get(ENCRYPTION_KEY);
			
			// Get Master Key from HSM
			RangerHSM rangerHSM = new RangerHSM(conf);
			String mKey = rangerHSM.getMasterKey(password);
			byte[] key = Base64.decode(mKey);
			
			// Put Master Key in Ranger DB			
			RangerMasterKey rangerMasterKey = new RangerMasterKey(daoManager);
			rangerMasterKey.generateMKFromHSMMK(password, key);		
		}
		catch(Throwable t) {
			throw new RuntimeException("Unable to import Master key from HSM to Ranger DB", t);
		}
		finally{
			Arrays.fill(partitionPassword, ' ');
		}
	}
}
