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

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.entity.XXRangerKeyStore;
import org.apache.ranger.kms.dao.DaoManager;

public class MigrateDBMKeyToGCP {
	private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
	private static RangerGoogleCloudHSMProvider rangerGcpProvider;
	private RangerKeyStore dbStore;

	public static void main(String[] args) throws Exception {
	    if (args == null || args.length != 5) {
	        System.err.println("Invalid number of parameters found.");
	        showUsage();
	        System.exit(1);
	    } else {
	        Configuration conf = RangerKeyStoreProvider.getDBKSConf();
	        final String gcpMasterKeyName     = args[0];
	        final String gcpProjectId         = args[1];
	        final String gcpKeyRingId         = args[2];
	        final String gcpKeyRingLocationId = args[3];
	        final String pathOfJsonCredFile   = args[4];

            if (conf != null) {
                conf.set(RangerGoogleCloudHSMProvider.GCP_MASTER_KEY_NAME, gcpMasterKeyName);
                conf.set(RangerGoogleCloudHSMProvider.GCP_PROJECT_ID, gcpProjectId);
                conf.set(RangerGoogleCloudHSMProvider.GCP_KEYRING_ID, gcpKeyRingId);
                conf.set(RangerGoogleCloudHSMProvider.GCP_LOCATION_ID, gcpKeyRingLocationId);
                conf.set(RangerGoogleCloudHSMProvider.GCP_CRED_JSON_FILE, pathOfJsonCredFile);

                rangerGcpProvider = new RangerGoogleCloudHSMProvider(conf);
                rangerGcpProvider.onInitialization();
                boolean result = new MigrateDBMKeyToGCP().doExportMKToGcp(conf, gcpMasterKeyName);
                if (result) {
                    System.out.println("Master Key from Ranger KMS DB has been successfully migrated to GCP.");
                    System.exit(0);
                } else {
                    System.out.println("Migration of Master Key from Ranger KMS DB to GCP has been unsuccessful.");
                    System.exit(1);
                }
            } else {
                System.out.println("Migration of Master Key from Ranger KMS DB to GCP failed, Error - Configuration is null.");
                System.exit(1);
            }
	    }
	}

	private boolean doExportMKToGcp(Configuration conf, final String masterKeyName) {
		try {
			String mKeyPass = conf.get(ENCRYPTION_KEY);
			if (mKeyPass == null || mKeyPass.trim().equals("") || mKeyPass.trim().equals("_")
					|| mKeyPass.trim().equals("crypted")) {
				throw new IOException("Master Key Jceks does not exists");
			}

			RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);
			DaoManager daoManager = rangerkmsDb.getDaoManager();

			System.out.println("Creating masterkey with the name - " + masterKeyName);
			boolean gcpMKSuccess = rangerGcpProvider.generateMasterKey(null);
			if (gcpMKSuccess) {
			    System.out.println("Masterkey with the name '" + masterKeyName +"' created successfully on Google Cloud KMS.");
				dbStore = new RangerKeyStore(daoManager, false, rangerGcpProvider);
				// Get Master Key from Ranger DB
				RangerMasterKey rangerMasterKey = new RangerMasterKey(daoManager);
				char[] mkey = rangerMasterKey.getMasterKey(mKeyPass).toCharArray();
				List<XXRangerKeyStore> rangerKeyStoreList = new ArrayList<XXRangerKeyStore>();
				dbStore.engineLoad(null, mkey);
				Enumeration<String> e = dbStore.engineAliases();
				Key key;
				String alias = null;
				while (e.hasMoreElements()) {
					alias = e.nextElement();
					key = dbStore.engineGetKey(alias, mkey);
					XXRangerKeyStore xxRangerKeyStore = dbStore.convertKeysBetweenRangerKMSAndGCP(alias, key, rangerGcpProvider);
					rangerKeyStoreList.add(xxRangerKeyStore);
				}
				if (rangerKeyStoreList != null && !rangerKeyStoreList.isEmpty()) {
					for (XXRangerKeyStore rangerKeyStore : rangerKeyStoreList) {
						dbStore.dbOperationStore(rangerKeyStore);
					}
				}
				return true;
			}
			return false;
		} catch (Throwable t) {
			throw new RuntimeException("Unable to migrate Master key from Ranger KMS DB to GCP ", t);
		}
	}

	private static void showUsage() {
	    System.err.println("USAGE: java " + MigrateDBMKeyToGCP.class.getName() + " <gcpMasterKeyName> <gcpProjectName> <gcpKeyRingName> <gcpKeyRingLocationName> <pathOfJsonCredFile> ");
	}
}
