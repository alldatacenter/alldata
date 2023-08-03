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

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.kms.v20190118.KmsClient;
import com.tencentcloudapi.kms.v20190118.models.*;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

public class RangerTencentKMSProvider implements RangerKMSMKI {

	static final Logger logger = LoggerFactory.getLogger(RangerTencentKMSProvider.class);
	static final String TENCENT_MASTER_KEY_ID = "ranger.kms.tencent.masterkey.id";
	static final String TENCENT_CLIENT_ID = "ranger.kms.tencent.client.id";
    static final String TENCENT_CLIENT_SECRET = "ranger.kms.tencent.client.secret";
    static final String TENCENT_CLIENT_REGION = "ranger.kms.tencent.client.region";
    private String masterKeyId;
	private KeyMetadata masterKeyMetadata;
	private KmsClient keyVaultClient;

	protected RangerTencentKMSProvider(Configuration conf,
									KmsClient client) {
		this.masterKeyId = conf.get(TENCENT_MASTER_KEY_ID);
		this.keyVaultClient = client;
	}

	public RangerTencentKMSProvider(Configuration conf) throws Exception {
		this(conf, createKMSClient(conf));
	}

    public static KmsClient createKMSClient(Configuration conf) throws Exception {
		String tencentClientId = conf.get(TENCENT_CLIENT_ID);
		if (StringUtils.isEmpty(tencentClientId)) {
			throw new Exception(
					"Tencent KMS is enabled, but client id is not configured");
		}
		String tencentClientSecret = conf.get(TENCENT_CLIENT_SECRET);
		String tencentClientRegion = conf.get(TENCENT_CLIENT_REGION);
		return new KmsClient(
				new Credential(tencentClientId, tencentClientSecret),
				tencentClientRegion);
	}

    @Override
	public boolean generateMasterKey(String password) throws Exception {
		if (keyVaultClient == null) {
			throw new Exception(
					"Key Vault Client is null. Please check the azure related configuration.");
		}
		try {

			DescribeKeyRequest desckey_req = new DescribeKeyRequest();
			desckey_req.setKeyId(masterKeyId);
			DescribeKeyResponse desckey_resp = keyVaultClient.DescribeKey(desckey_req);
			if (desckey_resp == null || !desckey_resp.getKeyMetadata().getKeyId().equals(masterKeyId)) {
				throw new Exception("KetMetadata is invalid");
			}
			masterKeyMetadata = desckey_resp.getKeyMetadata();
		} catch (TencentCloudSDKException ex) {
			throw new Exception(
					"Error while getting existing master key from Tencent.  Master Key Id : "
							+ masterKeyId + " . Error : " + ex.getMessage());
		}
		if (masterKeyMetadata == null) {
			throw new NoSuchMethodException("generateMasterKey is not implemented for Tencent KMS");
		} else {
			logger.info("Tencent Master key exist with KeyId :" + masterKeyId
					+ " with Alias: " + masterKeyMetadata.getAlias()
					+ " with Description : " + masterKeyMetadata.getDescription()
					+ " with ResourceId : " + masterKeyMetadata.getResourceId());
			return true;
		}
	}

	@Override
	public byte[] encryptZoneKey(Key zoneKey) throws Exception {
		try {
			EncryptRequest req = new EncryptRequest();
			req.setKeyId(this.masterKeyId);
			req.setPlaintext(Base64.getEncoder().encodeToString(zoneKey.getEncoded()));
			EncryptResponse resp = keyVaultClient.Encrypt(req);
			// resp.getCiphertextBlob() returns something looks like base64 encoded.
			// It is actually a concatenation of several base64 encoded fragments.
			// Maybe Tencent KMS will use the separation information of fragments.
			// So we MUST NOT decode base64 here.
			return resp.getCiphertextBlob().getBytes(StandardCharsets.US_ASCII);
		} catch (TencentCloudSDKException e) {
			throw (Exception)new Exception("Error while encrypting zone key.").initCause(e);
		}
	}

	@Override
	public byte[] decryptZoneKey(byte[] encryptedByte) throws Exception {
		try {
			DecryptRequest req = new DecryptRequest();
			req.setCiphertextBlob(new String(encryptedByte, StandardCharsets.US_ASCII));
			DecryptResponse resp = keyVaultClient.Decrypt(req);
			return Base64.getDecoder().decode(resp.getPlaintext());
		} catch (TencentCloudSDKException e) {
			throw (Exception)new Exception("Error while decrypting zone key.").initCause(e);
		}
	}

	@Override
	public String getMasterKey(String masterKeySecretName) {
		/*
		 * This method is not require for Tencent KMS because we can't get
		 * key outside of KMS
		 */
		return null;
	}
}
