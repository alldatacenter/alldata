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

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.kms.v1.CryptoKey;
import com.google.cloud.kms.v1.CryptoKey.CryptoKeyPurpose;
import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.CryptoKeyVersion.CryptoKeyVersionAlgorithm;
import com.google.cloud.kms.v1.CryptoKeyVersionTemplate;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyRing;
import com.google.cloud.kms.v1.KeyRingName;
import com.google.cloud.kms.v1.ProtectionLevel;
import com.google.protobuf.ByteString;

public class RangerGoogleCloudHSMProvider implements RangerKMSMKI {

	private static final Logger logger = LoggerFactory.getLogger(RangerGoogleCloudHSMProvider.class);
	protected static final String GCP_KEYRING_ID = "ranger.kms.gcp.keyring.id";
	protected static final String GCP_CRED_JSON_FILE = "ranger.kms.gcp.cred.file";
	protected static final String GCP_PROJECT_ID = "ranger.kms.gcp.project.id";
	protected static final String GCP_LOCATION_ID = "ranger.kms.gcp.location.id";
	protected static final String GCP_MASTER_KEY_NAME = "ranger.kms.gcp.masterkey.name";
	private static final String GCP_CRED_ENV_VARIABLE = "GOOGLE_APPLICATION_CREDENTIALS";

	private String gcpKeyRingId;
	private String gcpAppCredFile;
	private String gcpProjectId;
	private String gcpLocationId;
	private String gcpMasterKeyName;
	private KeyManagementServiceClient client = null;
	private KeyRingName keyRingName = null;

	public RangerGoogleCloudHSMProvider(Configuration conf) throws Exception {
		this.gcpKeyRingId = conf.get(GCP_KEYRING_ID);
		this.gcpAppCredFile = conf.get(GCP_CRED_JSON_FILE);
		this.gcpLocationId = conf.get(GCP_LOCATION_ID);
		this.gcpProjectId = conf.get(GCP_PROJECT_ID);
		this.gcpMasterKeyName = conf.get(GCP_MASTER_KEY_NAME);
	}

	protected void validateGcpProps() {
		if (StringUtils.isEmpty(this.gcpAppCredFile) || !this.gcpAppCredFile.endsWith(".json")) {
			throw new RuntimeCryptoException("Error : Invalid GCP app Credential JSON file, Provided cred file : " + this.gcpAppCredFile);
		} else if (StringUtils.isEmpty(this.gcpKeyRingId)) {
			throw new RuntimeCryptoException("Error : Please provide GCP app KeyringId, Provided keyring ID : " + this.gcpKeyRingId);
		} else if (StringUtils.isEmpty(this.gcpLocationId)) {
			throw new RuntimeCryptoException("Error : Please provide the GCP app location Id, Provided location ID :" + this.gcpLocationId);
		} else if (StringUtils.isEmpty(this.gcpProjectId)) {
			throw new RuntimeCryptoException("Error : Please provide the GCP app project Id, Provided ID : " + this.gcpProjectId);
		} else if (StringUtils.isEmpty(this.gcpMasterKeyName)) {
			throw new RuntimeCryptoException("Error : Master key name must not be empty, Provided MasterKey Name : " + this.gcpMasterKeyName);
		}
	}

	@Override
	public void onInitialization() throws Exception {
		this.validateGcpProps();
		if (logger.isDebugEnabled()) {
			logger.debug("==> onInitialization() : {gcpProjectId - " + this.gcpProjectId + ", gcpLocationId - "
					+ this.gcpLocationId + ", gcpKeyRingId - " + this.gcpKeyRingId + ", gcpAppCredFile Path - "
					+ this.gcpAppCredFile + "}");
		}
		String errorMessage = null;
		client = getKeyClient(this.gcpAppCredFile);

		KeyRing keyRingResponse = null;
		if (client != null) {
			this.keyRingName = KeyRingName.of(this.gcpProjectId, this.gcpLocationId, this.gcpKeyRingId);
			if(this.keyRingName != null) {
				keyRingResponse = this.client.getKeyRing(this.keyRingName.toString());
				if (keyRingResponse == null) {
					errorMessage = "Unable to get Key Ring response for Project : " + this.gcpProjectId + " and Location : " + this.gcpLocationId;
				} else if (keyRingResponse != null && !keyRingResponse.getName().endsWith(this.gcpKeyRingId)) {
					errorMessage = "Key Ring with name : " + this.gcpKeyRingId + " does not exist for Project : " + this.gcpProjectId + " and Location : " + this.gcpLocationId;
				}
			} else {
				errorMessage = "Unable to get Key Ring response for Project : " + this.gcpProjectId + " and Location : " + this.gcpLocationId;
			}
		} else {
			errorMessage = "Unable to create client object for Google Cloud HSM. Please check the Key HSM Log file OR Verify Google App Credential JSON file.";
		}
		if (logger.isDebugEnabled()) {
			logger.debug("<== onInitialization() : {this.keyRingName - " + this.keyRingName + ", keyRingResponse - " + keyRingResponse + "}");
		}
		if (!StringUtils.isEmpty(errorMessage)) {
			throw new RuntimeCryptoException(errorMessage);
		}
	}

	private KeyManagementServiceClient getKeyClient(String credentialFileName) {
		try {
			if (StringUtils.isEmpty(System.getenv(GCP_CRED_ENV_VARIABLE))) {
				updateEnv(GCP_CRED_ENV_VARIABLE, credentialFileName);
			}
			KeyManagementServiceClient client = KeyManagementServiceClient.create();
			return client;
		} catch (Exception ex) {
			logger.error("Unable to create Google Cloud KMS Client, Error : ", ex);
		}
		return null;
	}

	@Override
	public boolean generateMasterKey(String unused_password) throws Throwable {
		//The ENCRYPT_DECRYPT key purpose enables symmetric encryption.
		//All keys with key purpose ENCRYPT_DECRYPT use the GOOGLE_SYMMETRIC_ENCRYPTION algorithm.
		//No parameters are used with this algorithm.
		CryptoKey key = CryptoKey.newBuilder()
					.setPurpose(CryptoKeyPurpose.ENCRYPT_DECRYPT)
					.setVersionTemplate(CryptoKeyVersionTemplate.newBuilder()
					.setProtectionLevel(ProtectionLevel.HSM)
					.setAlgorithm(CryptoKeyVersionAlgorithm.GOOGLE_SYMMETRIC_ENCRYPTION))
					.build();

		// Create the key.
		CryptoKey createdKey = null;
		try {
			createdKey = client.createCryptoKey(this.keyRingName, this.gcpMasterKeyName, key);
		} catch (Exception e) {
			if (e instanceof AlreadyExistsException) {
				logger.info("MasterKey with the name '" + this.gcpMasterKeyName + "' already exist.");
				return true;
			} else {
				throw new RuntimeCryptoException("Failed to create master key with name '" + this.gcpMasterKeyName + "', Error - " + e.getMessage());
			}
		}

		if (createdKey == null) {
			logger.info("Failed to create master key : " + this.gcpMasterKeyName);
			return false;
		}
		logger.info("Master Key Created Successfully On Google Cloud HSM : " + this.gcpMasterKeyName);
		return true;
	}

	@Override
	public String getMasterKey(String password) throws Throwable {
		// Not Allowed to get master key out side of the Google Cloud HSM i.e very similar to Azure Key Vault
		return null;
	}

	@Override
	public byte[] encryptZoneKey(Key zoneKey) throws Exception {
		if(logger.isDebugEnabled()) {
			logger.debug("==> GCP encryptZoneKey()");
		}
		byte[] primaryEncodedZoneKey = zoneKey.getEncoded(); // Data to encrypt i.e a zoneKey
		CryptoKeyName keyName = CryptoKeyName.of(this.gcpProjectId, this.gcpLocationId, this.gcpKeyRingId, this.gcpMasterKeyName);

		EncryptResponse encryptResponse = this.client.encrypt(keyName, ByteString.copyFrom(primaryEncodedZoneKey));
		if (encryptResponse == null) {
			throw new RuntimeCryptoException("Got null response for encrypt zone key operation, Please reverify/check configs!");
		}
		if(logger.isDebugEnabled()) {
			logger.debug("<== GCP encryptZoneKey() : EncryptResponse - { " + encryptResponse + " }" );
		}
		return encryptResponse.getCiphertext().toByteArray();
	}

	@Override
	public byte[] decryptZoneKey(byte[] encryptedByte) throws Exception {
		CryptoKeyName keyName = CryptoKeyName.of(this.gcpProjectId, this.gcpLocationId, this.gcpKeyRingId, this.gcpMasterKeyName);
		if(logger.isDebugEnabled()) {
			logger.debug("==> GCP decryptZoneKey() : CryptoKeyName - { " + keyName + " }");
		}

		DecryptResponse response = client.decrypt(keyName, ByteString.copyFrom(encryptedByte));
		if(response == null) {
			throw new RuntimeCryptoException("Got null response for decrypt zone key operation!");
		} else if(response.getPlaintext() == null || StringUtils.isEmpty(response.getPlaintext().toString())) {
			throw new RuntimeCryptoException("Error - Received null or empty decrypted zone key : " + response.getPlaintext());
		}
		if(logger.isDebugEnabled()) {
			logger.debug("<== GCP decryptZoneKey() : DecryptResponse - { " + response + " }" );
		}
		return response.getPlaintext().toByteArray();
	}

	@SuppressWarnings("unchecked")
	private static void updateEnv(String name, String val) throws ReflectiveOperationException {
		Map<String, String> env = System.getenv();
		Field field = env.getClass().getDeclaredField("m");
		field.setAccessible(true);

		Map<String, String> writeAbleEnvMap = (Map<String, String>) field.get(env);
		writeAbleEnvMap.put(name, val);
	}
}
