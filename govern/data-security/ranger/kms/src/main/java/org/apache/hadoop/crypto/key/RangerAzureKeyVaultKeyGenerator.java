/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.crypto.key;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.Attributes;
import com.microsoft.azure.keyvault.models.KeyAttributes;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.models.custom.KeyBundle;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;

import java.security.Key;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAzureKeyVaultKeyGenerator implements RangerKMSMKI {

	static final Logger logger = LoggerFactory
			.getLogger(RangerAzureKeyVaultKeyGenerator.class);
	static final String AZURE_KEYVAULT_URL = "ranger.kms.azurekeyvault.url";
	static final String AZURE_MASTER_KEY_ALIAS = "ranger.kms.azure.masterkey.name";
	static final String AZURE_MASTER_KEY_TYPE = "ranger.kms.azure.masterkey.type";
	static final String ZONE_KEY_ENCRYPTION_ALGO = "ranger.kms.azure.zonekey.encryption.algorithm";
	static final String AZURE_KEYVAULT_SSL_ENABLED = "ranger.kms.azure.keyvault.ssl.enabled";
	static final String AZURE_CLIENT_ID = "ranger.kms.azure.client.id";
	static final String AZURE_CLIENT_SECRET = "ranger.kms.azure.client.secret";
	static final String AZURE_KEYVAULT_CERTIFICATE_PATH = "ranger.kms.azure.keyvault.certificate.path";
	static final String AZURE_KEYVAULT_CERTIFICATE_PASSWORD = "ranger.kms.azure.keyvault.certificate.password";
	private String keyVaultURL;
	private String azureMasterKey;
	private String azureMasterKeyType;
	private String zoneKeyEncryptionAlgo;
	private KeyVaultClient keyVaultClient;
	private KeyBundle masterKeyBundle;

	public RangerAzureKeyVaultKeyGenerator(Configuration conf,
										   KeyVaultClient kvClient) {
		this.keyVaultURL = conf.get(AZURE_KEYVAULT_URL);
		this.azureMasterKey = conf.get(AZURE_MASTER_KEY_ALIAS);
		this.azureMasterKeyType = conf.get(AZURE_MASTER_KEY_TYPE);
		this.zoneKeyEncryptionAlgo = conf.get(ZONE_KEY_ENCRYPTION_ALGO);
		this.keyVaultClient = kvClient;
	}

	public RangerAzureKeyVaultKeyGenerator(Configuration conf) throws Exception {
		this(conf, createKeyVaultClient(conf));
	}

	public static KeyVaultClient createKeyVaultClient(Configuration conf) throws Exception {
		AzureKeyVaultClientAuthenticator azureKVClientAuthenticator;
		String azureClientId = conf.get(AZURE_CLIENT_ID);
		if (StringUtils.isEmpty(azureClientId)) {
			throw new Exception(
					"Azure Key Vault is enabled and client id is not configured");
		}
		String azureClientSecret = conf.get(AZURE_CLIENT_SECRET);
		KeyVaultClient kvClient;
		if (conf != null
				&& StringUtils.isNotEmpty(conf.get(AZURE_KEYVAULT_SSL_ENABLED))
				&& conf.get(AZURE_KEYVAULT_SSL_ENABLED).equalsIgnoreCase("false")) {
			try {
				if (StringUtils.isEmpty(azureClientSecret)) {
					throw new Exception(
							"Azure Key Vault is enabled in non SSL mode and client password/secret is not configured");
				}
				azureKVClientAuthenticator = new AzureKeyVaultClientAuthenticator(
						azureClientId, azureClientSecret);
				kvClient = new KeyVaultClient(azureKVClientAuthenticator);
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
							"Azure Key Vault is enabled in SSL mode. Please provide certificate path for authentication.");
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
		return kvClient;
	}

	@Override
	public boolean generateMasterKey(String password) throws Exception {
		if (keyVaultClient == null) {
			throw new Exception(
					"Key Vault Client is null. Please check the azure related configuration.");
		}
		try {
			masterKeyBundle = keyVaultClient
					.getKey(keyVaultURL, azureMasterKey);

		} catch (Exception ex) {
			throw new Exception(
					"Error while getting existing master key from Azure.  Master Key Name : "
							+ azureMasterKey + " . Key Vault URL : "
							+ keyVaultURL + " . Error : " + ex.getMessage());
		}
		if (masterKeyBundle == null) {
			try {
				JsonWebKeyType keyType;
				switch (azureMasterKeyType) {
				case "RSA":
					keyType = JsonWebKeyType.RSA;
					break;

				case "RSA_HSM":
					keyType = JsonWebKeyType.RSA_HSM;
					break;

				case "EC":
					keyType = JsonWebKeyType.EC;
					break;

				case "EC_HSM":
					keyType = JsonWebKeyType.EC_HSM;
					break;

				case "OCT":
					keyType = JsonWebKeyType.OCT;
					break;

				default:
					keyType = JsonWebKeyType.RSA;
				}

				Attributes masterKeyattribute = new KeyAttributes()
						.withEnabled(true).withNotBefore(new DateTime());

				CreateKeyRequest createKeyRequest = new CreateKeyRequest.Builder(
						keyVaultURL, azureMasterKey, keyType).withAttributes(
						masterKeyattribute).build();
				masterKeyBundle = keyVaultClient.createKeyAsync(
						createKeyRequest, null).get();
				return true;
			} catch (Exception ex) {
				throw new Exception("Error while creating master key  : "
						+ ex.getMessage());
			}
		} else {
			logger.info("Azure Master key exist with name :" + azureMasterKey
					+ " with key identifier " + masterKeyBundle.key().kid());
			return true;
		}
	}

	@Override
	public byte[] encryptZoneKey(Key zoneKey) throws Exception {
		JsonWebKeyEncryptionAlgorithm keyEncryptionAlgo = getZoneKeyEncryptionAlgo();
		KeyOperationResult encryptResult = null;
		
		if (masterKeyBundle == null) {
			masterKeyBundle = keyVaultClient
					.getKey(keyVaultURL, azureMasterKey);
		}
		try {
			encryptResult = keyVaultClient.encryptAsync(
					masterKeyBundle.key().kid(), keyEncryptionAlgo,
					zoneKey.getEncoded(), null).get();

		} catch (Exception e) {
			throw new Exception("Error while encrypting zone key." + e);
		}
		return encryptResult.result();
	}

	@Override
	public byte[] decryptZoneKey(byte[] encryptedByte) throws Exception {
		JsonWebKeyEncryptionAlgorithm keyEncryptionAlgo = getZoneKeyEncryptionAlgo();
		if (masterKeyBundle == null) {
			masterKeyBundle = keyVaultClient
					.getKey(keyVaultURL, azureMasterKey);
		}
		KeyOperationResult decryptResult = null;
		try {
			decryptResult = keyVaultClient.decryptAsync(
					masterKeyBundle.key().kid(), keyEncryptionAlgo,
					encryptedByte, null).get();

		} catch (Exception e) {
			throw new Exception("Error while decrypting zone key." + e);
		}
		return decryptResult.result();
	}

	private JsonWebKeyEncryptionAlgorithm getZoneKeyEncryptionAlgo() {
		JsonWebKeyEncryptionAlgorithm keyEncryptionAlgo;
		switch (zoneKeyEncryptionAlgo) {
		case "RSA_OAEP":
			keyEncryptionAlgo = JsonWebKeyEncryptionAlgorithm.RSA_OAEP;
			break;

		case "RSA_OAEP_256":
			keyEncryptionAlgo = JsonWebKeyEncryptionAlgorithm.RSA_OAEP_256;
			break;

		case "RSA1_5":
			keyEncryptionAlgo = JsonWebKeyEncryptionAlgorithm.RSA1_5;
			break;

		default:
			keyEncryptionAlgo = JsonWebKeyEncryptionAlgorithm.RSA_OAEP;
		}
		return keyEncryptionAlgo;
	}

	@Override
	public String getMasterKey(String masterKeySecretName) {
		/*
		 * This method is not require for Azure Key Vault because we can't get
		 * key outside of key vault
		 */
		return null;
	}
}
