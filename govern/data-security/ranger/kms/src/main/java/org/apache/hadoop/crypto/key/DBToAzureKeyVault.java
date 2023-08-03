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

import com.microsoft.azure.keyvault.KeyVaultClient;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.entity.XXRangerKeyStore;
import org.apache.ranger.kms.dao.DaoManager;

public class DBToAzureKeyVault {
	private static final String AZURE_CLIENT_ID = "ranger.kms.azure.client.id";
    private static final String AZURE_CLIENT_SECRET = "ranger.kms.azure.client.secret";
    private static final String AZURE_MASTER_KEY_ALIAS = "ranger.kms.azure.masterkey.name";
    private static final String AZURE_KEYVAULT_CERTIFICATE_PATH = "ranger.kms.azure.keyvault.certificate.path";
    private static final String AZURE_KEYVAULT_URL = "ranger.kms.azurekeyvault.url";
    private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
    private static final String AZURE_MASTER_KEY_TYPE= "ranger.kms.azure.masterkey.type";
    private static final String ZONE_KEY_ENCRYPTION_ALGO = "ranger.kms.azure.zonekey.encryption.algorithm";
    private RangerKeyStore dbStore;
    public static void showUsage() {
        System.err
                        .println("USAGE: java "
                                        + DBToAzureKeyVault.class.getName()
                                        + " <azureMasterKeyName> <azureMasterKeyType> <zoneKeyEncryptionAlgo> <azureKeyVaultUrl> <azureClientId> <isSSLEnabled> <clientSecret / Certificate Path>");
		}
	
	public static void main(String[] args) {
		if (args.length < 7) {
			System.err.println("Invalid number of parameters found.");
			showUsage();
			System.exit(1);
		} else {
			Configuration conf = RangerKeyStoreProvider.getDBKSConf();
			String azureKeyName = args[0];
			if (azureKeyName == null || azureKeyName.trim().isEmpty()) {
				System.err.println("Azure master key name not provided.");
				showUsage();
				System.exit(1);
			}
			String azureMasterKeyType = args[1];
			if (azureMasterKeyType == null
					|| azureMasterKeyType.trim().isEmpty()) {
				System.err.println("Azure master key type not provided.");
				showUsage();
				System.exit(1);
			}
			String zoneKeyEncryptionAlgo = args[2];
			if (zoneKeyEncryptionAlgo == null
					|| zoneKeyEncryptionAlgo.trim().isEmpty()) {
				System.err
						.println("Zone Key Encryption algorithm name not provided.");
				showUsage();
				System.exit(1);
			}
			String azureKeyVaultUrl = args[3];
			if (azureKeyVaultUrl == null || azureKeyVaultUrl.trim().isEmpty()) {
				System.err.println("Azure Key Vault url not provided.");
				showUsage();
				System.exit(1);
			}
			String azureClientId = args[4];
			if (azureClientId == null || azureClientId.trim().isEmpty()) {
				System.err.println("Azure Client Id is not provided.");
				showUsage();
				System.exit(1);
			}
			String isSSLEnabled = args[5];
			if (isSSLEnabled == null || isSSLEnabled.trim().isEmpty()) {
				System.err.println("isSSLEnabled not provided.");
				showUsage();
				System.exit(1);
			}
			if (!isSSLEnabled.equalsIgnoreCase("true")
					&& !isSSLEnabled.equalsIgnoreCase("false")) {
				System.err
						.println("Please provide the valid value for isSSLEnabled");
				showUsage();
				System.exit(1);
			}

			String passwordOrCertPath = args[6];
			String certificatePassword = null;
			if (passwordOrCertPath == null
					|| passwordOrCertPath.trim().isEmpty()) {
				System.err
						.println("Please provide Azure client password of certificate password");
				showUsage();
				System.exit(1);
			}

			boolean result = false;
			boolean sslEnabled = false;
			if (isSSLEnabled.equalsIgnoreCase("true")) {
				sslEnabled = true;
				if (!passwordOrCertPath.endsWith(".pem")
						&& !passwordOrCertPath.endsWith(".pfx")) {
					System.err
							.println("Please provide valid certificate file path E.G .pem /.pfx");
					showUsage();
					System.exit(1);
				} else {
					if (args.length > 7 && !StringUtils.isEmpty(args[7])) {
						certificatePassword = args[7];
					}
				}
			}
			result = new DBToAzureKeyVault().doExportMKToAzureKeyVault(
					sslEnabled, azureKeyName, azureMasterKeyType,
					zoneKeyEncryptionAlgo, azureClientId, azureKeyVaultUrl,
					passwordOrCertPath, certificatePassword, conf);
			if (result) {
				System.out
						.println("Master Key from Ranger KMS DB has been successfully imported into Azure Key Vault.");
			} else {
				System.out
						.println("Import of Master Key from DB has been unsuccessful.");
				System.exit(1);
			}
			System.exit(0);
		}
	}
	
	private boolean doExportMKToAzureKeyVault(boolean sslEnabled, String masterKeyName, String masterKeyType, String zoneKeyEncryptionAlgo,
			String azureClientId, String azureKeyVaultUrl,
			String passwordOrCertPath, String certificatePassword, Configuration conf) {
		try {
			String mKeyPass = conf.get(ENCRYPTION_KEY);
			if (mKeyPass == null
					|| mKeyPass.trim().equals("")
					|| mKeyPass.trim().equals("_")
					|| mKeyPass.trim().equals("crypted")) {
				throw new IOException("Master Key Jceks does not exists");
			}
			conf.set(AZURE_MASTER_KEY_TYPE, masterKeyType);
			conf.set(ZONE_KEY_ENCRYPTION_ALGO, zoneKeyEncryptionAlgo);
			conf.set(AZURE_MASTER_KEY_ALIAS, masterKeyName);
			conf.set(AZURE_CLIENT_ID, azureClientId);
			conf.set(AZURE_KEYVAULT_URL, azureKeyVaultUrl);
			RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);
			DaoManager daoManager = rangerkmsDb.getDaoManager();
			KeyVaultClient kvClient = null;
			if(sslEnabled){
				conf.set(AZURE_KEYVAULT_CERTIFICATE_PATH, passwordOrCertPath);
				AzureKeyVaultClientAuthenticator azureKVClientAuthenticator = new AzureKeyVaultClientAuthenticator(
						azureClientId);
				kvClient = !StringUtils.isEmpty(certificatePassword) ? azureKVClientAuthenticator
						.getAuthentication(passwordOrCertPath, certificatePassword)
						: azureKVClientAuthenticator.getAuthentication(passwordOrCertPath, "");
				
			}else{
				conf.set(AZURE_CLIENT_SECRET, passwordOrCertPath);
				AzureKeyVaultClientAuthenticator azureKVClientAuthenticator = new AzureKeyVaultClientAuthenticator(
						azureClientId, passwordOrCertPath);
				kvClient = new KeyVaultClient(
						azureKVClientAuthenticator);
			}
			if(kvClient == null){
				System.err.println("Key Vault is null. Please check the azure related configs.");
				System.exit(1);
			}
			RangerKMSMKI rangerKVKeyGenerator = new RangerAzureKeyVaultKeyGenerator(
					conf, kvClient);
			boolean azureMKSuccess = rangerKVKeyGenerator.generateMasterKey(mKeyPass);
			if (azureMKSuccess) {
				dbStore = new RangerKeyStore(daoManager, conf, kvClient);
				// Get Master Key from Ranger DB
				RangerMasterKey rangerMasterKey = new RangerMasterKey(
						daoManager);
				char[] mkey = rangerMasterKey.getMasterKey(mKeyPass)
						.toCharArray();
				List<XXRangerKeyStore> rangerKeyStoreList = new ArrayList<XXRangerKeyStore>();
				dbStore.engineLoad(null, mkey);
				Enumeration<String> e = dbStore.engineAliases();
				Key key;
				String alias = null;
				while (e.hasMoreElements()) {
					alias = e.nextElement();
					key = dbStore.engineGetKey(alias, mkey);
					XXRangerKeyStore xxRangerKeyStore = dbStore
							.convertKeysBetweenRangerKMSAndAzureKeyVault(alias,
									key, rangerKVKeyGenerator);
					rangerKeyStoreList.add(xxRangerKeyStore);
				}
				if (rangerKeyStoreList != null && !rangerKeyStoreList.isEmpty()) {
					for(XXRangerKeyStore rangerKeyStore : rangerKeyStoreList){
						dbStore.dbOperationStore(rangerKeyStore);
					}
				}
				return true;
			}
			return false;
		}catch(Throwable t){
			throw new RuntimeException(
                    "Unable to import Master key from Ranger DB to Azure Key Vault ",
                    t);
		}

	}
	
}
