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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.credentialapi.CredentialReader;
import org.apache.ranger.kms.dao.DaoManager;

public class Ranger2JKSUtil {

	private static final String AZURE_KEYVAULT_ENABLED = "ranger.kms.azurekeyvault.enabled";
	private static final String AZURE_KEYVAULT_SSL_ENABLED = "ranger.kms.azure.keyvault.ssl.enabled";
	private static final String AZURE_CLIENT_ID = "ranger.kms.azure.client.id";
	private static final String AZURE_CLIENT_SECRET = "ranger.kms.azure.client.secret";
	private static final String AZURE_CLIENT_SECRET_ALIAS = "ranger.kms.azure.client.secret.alias";
	private static final String AZURE_KEYVAULT_CERTIFICATE_PATH = "ranger.kms.azure.keyvault.certificate.path";
	private static final String AZURE_KEYVAULT_CERTIFICATE_PASSWORD = "ranger.kms.azure.keyvault.certificate.password";
	private static final String CREDENTIAL_PATH = "ranger.ks.jpa.jdbc.credential.provider.path";
	private static final String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();
	private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
	private static final String KEYSECURE_ENABLED = "ranger.kms.keysecure.enabled";
	private static final String KEYSECURE_USERNAME = "ranger.kms.keysecure.login.username";
	private static final String KEYSECURE_PASSWORD = "ranger.kms.keysecure.login.password";
	private static final String KEYSECURE_PASSWORD_ALIAS = "ranger.kms.keysecure.login.password.alias";
	private static final String KEYSECURE_LOGIN = "ranger.kms.keysecure.login";

	public static void showUsage() {
		System.err.println("USAGE: java " + Ranger2JKSUtil.class.getName()
				+ " <KMS_FileName> [KeyStoreType]");
		System.err
				.println(" If KeyStoreType is not provided, it will be considered as "
						+ DEFAULT_KEYSTORE_TYPE);
		System.err
				.println(" When execution of this utility, it will prompt for both keystore password and key password.");
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.err.println("Invalid number of parameters found.");
			showUsage();
			System.exit(1);
		} else {
			String keyStoreFileName = args[0];
			File f = new File(keyStoreFileName);
			if (!f.exists()) {
				boolean ret = f.createNewFile();
				if (!ret) {
					System.err
							.println("Error creating new keystore file. fileName="
									+ args[0]);
				}
			}
			String keyStoreType = (args.length == 2 ? args[1]
					: DEFAULT_KEYSTORE_TYPE);
			try {
				KeyStore.getInstance(keyStoreType);
			} catch (KeyStoreException e) {
				System.err
						.println("ERROR: Unable to get valid keystore for the type ["
								+ keyStoreType + "]");
				showUsage();
				System.exit(1);
			}
			new Ranger2JKSUtil().doExportKeysFromJKS(keyStoreFileName,
					keyStoreType);
			System.out
					.println("Keys from Ranger KMS Database has been successfully exported into "
							+ keyStoreFileName);
			System.exit(0);
		}
	}

	private void doExportKeysFromJKS(String keyStoreFileName,
			String keyStoreType) {
		char[] keyStorePassword = null;
		char[] keyPassword = null;
		try {
			keyStorePassword = ConsoleUtil
					.getPasswordFromConsole("Enter Password for the keystore FILE :");
			keyPassword = ConsoleUtil
					.getPasswordFromConsole("Enter Password for the KEY(s) stored in the keystore:");
			Configuration conf = RangerKeyStoreProvider.getDBKSConf();
			RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);
			DaoManager daoManager = rangerkmsDb.getDaoManager();
			RangerKeyStore dbStore;
			char[] masterKey = null;
			String password = conf.get(ENCRYPTION_KEY);
			if (conf != null
					&& StringUtils.isNotEmpty(conf.get(KEYSECURE_ENABLED))
					&& conf.get(KEYSECURE_ENABLED).equalsIgnoreCase("true")) {

				getFromJceks(conf, CREDENTIAL_PATH, KEYSECURE_PASSWORD_ALIAS,
						KEYSECURE_PASSWORD);
				String keySecureLoginCred = conf.get(KEYSECURE_USERNAME).trim()
						+ ":" + conf.get(KEYSECURE_PASSWORD);
				conf.set(KEYSECURE_LOGIN, keySecureLoginCred);

				RangerSafenetKeySecure rangerSafenetKeySecure = new RangerSafenetKeySecure(
						conf);
				masterKey = rangerSafenetKeySecure.getMasterKey(password)
						.toCharArray();
				dbStore = new RangerKeyStore(daoManager);

			} else if (conf != null
					&& StringUtils.isNotEmpty(conf.get(AZURE_KEYVAULT_ENABLED))
					&& conf.get(AZURE_KEYVAULT_ENABLED)
							.equalsIgnoreCase("true")) {
				getFromJceks(conf, CREDENTIAL_PATH, AZURE_CLIENT_SECRET_ALIAS,
						AZURE_CLIENT_SECRET);
				String azureClientId = conf.get(AZURE_CLIENT_ID);
				if (StringUtils.isEmpty(azureClientId)) {
					throw new Exception(
							"Azure Key Vault is enabled and client id is not configured");
				}
				String azureClientSecret = conf.get(AZURE_CLIENT_SECRET);
				dbStore = new RangerKeyStore(daoManager);
				AzureKeyVaultClientAuthenticator azureKVClientAuthenticator;
				KeyVaultClient kvClient = null;
				if (conf != null
						&& StringUtils.isNotEmpty(conf.get(AZURE_KEYVAULT_SSL_ENABLED))
						&& conf.get(AZURE_KEYVAULT_SSL_ENABLED).equalsIgnoreCase("false")) {
					try {
						azureKVClientAuthenticator = new AzureKeyVaultClientAuthenticator(
								azureClientId, azureClientSecret);
						kvClient = new KeyVaultClient(
								azureKVClientAuthenticator);
					} catch (Exception ex) {
						throw new Exception(
								"Error while getting key vault client object with client id and client secret : "
										+ ex);
					}
				} else {
					try {
						azureKVClientAuthenticator = new AzureKeyVaultClientAuthenticator(
								azureClientId);
						String keyVaultCertPath = conf
								.get(AZURE_KEYVAULT_CERTIFICATE_PATH);
						if (StringUtils.isEmpty(keyVaultCertPath)) {
							throw new Exception(
									"Azure Key Vault is enabled. Please provide client secret or certificate path for authentication.");
						}
						String keyVaultCertPassword = conf
								.get(AZURE_KEYVAULT_CERTIFICATE_PASSWORD);

						kvClient = !StringUtils.isEmpty(keyVaultCertPassword) ? azureKVClientAuthenticator
								.getAuthentication(keyVaultCertPath,
										keyVaultCertPassword)
								: azureKVClientAuthenticator.getAuthentication(
										keyVaultCertPath, "");
					} catch (Exception ex) {
						throw new Exception(
								"Error while getting key vault client object with client id and certificate. Error :  : "
										+ ex);
					}
				}
				if (kvClient != null) {
					masterKey = null;
					dbStore = new RangerKeyStore(daoManager, conf, kvClient);
				}
			} else {
				RangerMasterKey rangerMasterKey = new RangerMasterKey(
						daoManager);
				masterKey = rangerMasterKey.getMasterKey(password)
						.toCharArray();
				dbStore = new RangerKeyStore(daoManager);
			}
			OutputStream out = null;
			try {
				out = new FileOutputStream(new File(keyStoreFileName));
				dbStore.engineLoadToKeyStoreFile(out, keyStorePassword,
						keyPassword, masterKey, keyStoreType);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception e) {
						throw new RuntimeException(
								"ERROR:  Unable to close file stream for ["
										+ keyStoreFileName + "]", e);
					}
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException("Unable to export keys to ["
					+ keyStoreFileName + "] due to exception.", t);
		} finally {
			Arrays.fill(keyStorePassword, ' ');
			Arrays.fill(keyPassword, ' ');
		}
	}

	private static void getFromJceks(Configuration conf, String path,
			String alias, String key) {
		// update credential from keystore
		if (conf != null) {
			String pathValue = conf.get(path);
			String aliasValue = conf.get(alias);
			if (pathValue != null && aliasValue != null) {
				String xaDBPassword = CredentialReader.getDecryptedString(
						pathValue.trim(), aliasValue.trim(), KeyStore.getDefaultType());
				if (xaDBPassword != null && !xaDBPassword.trim().isEmpty()
						&& !xaDBPassword.trim().equalsIgnoreCase("none")) {
					conf.set(key, xaDBPassword);
				}
			}
		}
	}
}
