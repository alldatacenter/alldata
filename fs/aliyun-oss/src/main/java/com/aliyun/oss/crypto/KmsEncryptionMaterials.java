/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.crypto;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.kms.model.v20160120.DecryptRequest;
import com.aliyuncs.kms.model.v20160120.DecryptResponse;
import com.aliyuncs.kms.model.v20160120.EncryptRequest;
import com.aliyuncs.kms.model.v20160120.EncryptResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

/**
 * This provide a kms encryption materials for client-side encryption.
 */
public class KmsEncryptionMaterials implements EncryptionMaterials {
    private static final String KEY_WRAP_ALGORITHM = "KMS/ALICLOUD";
    private String region;
    private String cmk;
    CredentialsProvider credentialsProvider;
    
    private final Map<String, String> desc;
    private final LinkedHashMap<KmsClientSuite, Map<String, String>> kmsDescMaterials = 
                                    new LinkedHashMap<KmsClientSuite, Map<String, String>>();
    
    public KmsEncryptionMaterials(String region, String cmk) {
        assertParameterNotNull(region, "kms region");
        assertParameterNotNull(cmk, "kms cmk");
        this.region = region;
        this.cmk = cmk;
        this.desc = new HashMap<String, String>();
    }

    public KmsEncryptionMaterials(String region, String cmk, Map<String, String> desc) {
        assertParameterNotNull(region, "kms region");
        assertParameterNotNull(region, "kms cmk");
        this.region = region;
        this.cmk = cmk;
        this.desc = (desc == null) ? new HashMap<String, String>() : new HashMap<String, String>(desc);
    }
    
    private final class KmsClientSuite {
        private String region;
        private CredentialsProvider credentialsProvider;
        KmsClientSuite(String region, CredentialsProvider credentialsProvider) {
            this.region = region;
            this.credentialsProvider = credentialsProvider;
        }
    }

    /**
     * Sets the credentials provider.
     *
     * @param credentialsProvider
     *            The {@link CredentialsProvider} instance.
     */
    public void setKmsCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        kmsDescMaterials.put(new KmsClientSuite(region, credentialsProvider), desc);
    }

    /**
     * Create a new kms client.
     */
    private DefaultAcsClient createKmsClient(String region, CredentialsProvider credentialsPorvider) {
        Credentials credentials = credentialsPorvider.getCredentials();
        IClientProfile profile = DefaultProfile.getProfile(region, credentials.getAccessKeyId(), 
                credentials.getSecretAccessKey(), credentials.getSecurityToken());
        return new DefaultAcsClient(profile);
    }

    /**
     * Encrypt the plain text to cipherBlob.
     */
    private EncryptResponse encryptPlainText(String keyId, String plainText) throws ClientException {
        DefaultAcsClient kmsClient = createKmsClient(region, credentialsProvider);
        final EncryptRequest encReq = new EncryptRequest();
        encReq.setSysProtocol(ProtocolType.HTTPS);
        encReq.setAcceptFormat(FormatType.JSON);
        encReq.setSysMethod(MethodType.POST);
        encReq.setKeyId(keyId);
        encReq.setPlaintext(plainText);

        final EncryptResponse encResponse;
        try {
            encResponse = kmsClient.getAcsResponse(encReq);
        } catch (Exception e) {
            throw new ClientException("the kms client encrypt data failed." + e.getMessage(), e);
        }
        return encResponse;
    }

    /**
     * Decrypt the cipherBlob to palin text.
     */
    private DecryptResponse decryptCipherBlob(KmsClientSuite kmsClientSuite, String cipherBlob) 
            throws ClientException {
        final DefaultAcsClient kmsClient = createKmsClient(kmsClientSuite.region, kmsClientSuite.credentialsProvider);
        final DecryptRequest decReq = new DecryptRequest();
        decReq.setSysProtocol(ProtocolType.HTTPS);
        decReq.setAcceptFormat(FormatType.JSON);
        decReq.setSysMethod(MethodType.POST);
        decReq.setCiphertextBlob(cipherBlob);

        final DecryptResponse decResponse;
        try {
            decResponse = kmsClient.getAcsResponse(decReq);
        } catch (Exception e) {
            throw new ClientException("The kms client decrypt data faild." + e.getMessage(), e);
        }
        return decResponse;
    }

    /**
     * Add other kms region and descrption materials used for decryption.
     * 
     * @param region
     *            region.
     * @param description
     *            The descripton of encryption materails.
     */
    public void addKmsDescMaterial(String region, Map<String, String> description) {
        addKmsDescMaterial(region, credentialsProvider, description);
    }

    /**
     * Add other kms region credentialsProvider and descrption materials used for decryption.
     * 
     * @param region
     *            region.
     * @param credentialsProvider
     *            The credential provider.
     * @param description
     *            The descripton of encryption materails.
     */
    public synchronized void addKmsDescMaterial(String region, CredentialsProvider credentialsProvider, Map<String, String> description) {
        assertParameterNotNull(region, "region");
        assertParameterNotNull(credentialsProvider, "credentialsProvider");
        KmsClientSuite kmsClientSuite = new KmsClientSuite(region, credentialsProvider);
        if (description != null) {
            kmsDescMaterials.put(kmsClientSuite, new HashMap<String, String>(description));
        } else {
            kmsDescMaterials.put(kmsClientSuite, new HashMap<String, String>());
        }
    }

    /**
     * Find the specifed cmk region info for decrypting by the specifed descrption.
     * 
     * @param desc
     *            The encryption description.  
     * @return the specifed region if it was be found, otherwise return null.
     */
    private KmsClientSuite findKmsClientSuiteByDescription(Map<String, String> desc) {
        if (desc == null) {
            return null;
        }
        for (Map.Entry<KmsClientSuite, Map<String, String>> entry : kmsDescMaterials.entrySet()) {
            if (desc.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the lastest key-value in the LinedHashMap.
     */
    private <K, V> Entry<K, V> getTailByReflection(LinkedHashMap<K, V> map)
            throws NoSuchFieldException, IllegalAccessException {
        Field tail = map.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Entry<K, V>) tail.get(map);
    }

    /**
     * Encrypt the content encryption key(cek) and iv, and put the result into
     * {@link ContentCryptoMaterialRW}.
     * 
     * @param contentMaterialRW
     *            The materials that contans all content crypto info, 
     *            it must be constructed on outside and filled with the iv and cek.
     *            Then it will be builded with the encrypted cek ,encrypted iv, key wrap algorithm 
     *            and encryption materials description by this method.
     */
    @Override
    public void encryptCEK(ContentCryptoMaterialRW contentMaterialRW) {
        try {
            assertParameterNotNull(contentMaterialRW, "contentMaterialRW");
            assertParameterNotNull(contentMaterialRW.getIV(), "contentMaterialRW#getIV");
            assertParameterNotNull(contentMaterialRW.getCEK(), "contentMaterialRW#getCEK");

            byte[] iv = contentMaterialRW.getIV();
            EncryptResponse encryptresponse = encryptPlainText(cmk, BinaryUtil.toBase64String(iv));
            byte[] encryptedIV = BinaryUtil.fromBase64String(encryptresponse.getCiphertextBlob());

            SecretKey cek = contentMaterialRW.getCEK();
            encryptresponse = encryptPlainText(cmk, BinaryUtil.toBase64String(cek.getEncoded()));
            byte[] encryptedCEK = BinaryUtil.fromBase64String(encryptresponse.getCiphertextBlob());

            contentMaterialRW.setEncryptedCEK(encryptedCEK);
            contentMaterialRW.setEncryptedIV(encryptedIV);
            contentMaterialRW.setKeyWrapAlgorithm(KEY_WRAP_ALGORITHM);
            contentMaterialRW.setMaterialsDescription(desc);
        } catch (Exception e) {
            throw new ClientException("Kms encrypt CEK IV error. "
                    + "Please check your cmk, region, accessKeyId and accessSecretId." + e.getMessage(), e);
        }
    }

    /**
     * Decrypt the encrypted content encryption key(cek) and encrypted iv and put
     * the result into {@link ContentCryptoMaterialRW}.
     * 
     * @param contentMaterialRW
     *            The materials that contans all content crypto info, 
     *            it must be constructed on outside and filled with the encrypted cek ,encrypted iv, 
     *            key wrap algorithm, encryption materials description and cek generator 
     *            algothrim. Then it will be builded with the cek iv parameters by this method.
     */
    @Override
    public void decryptCEK(ContentCryptoMaterialRW contentMaterialRW) {
        assertParameterNotNull(contentMaterialRW, "ContentCryptoMaterialRW");
        assertParameterNotNull(contentMaterialRW.getEncryptedCEK(), "ContentCryptoMaterialRW#getEncryptedCEK");
        assertParameterNotNull(contentMaterialRW.getEncryptedIV(), "ContentCryptoMaterialRW#getEncryptedIV");
        assertParameterNotNull(contentMaterialRW.getKeyWrapAlgorithm(), "ContentCryptoMaterialRW#getKeyWrapAlgorithm");

        if (!contentMaterialRW.getKeyWrapAlgorithm().toLowerCase().equals(KEY_WRAP_ALGORITHM.toLowerCase())) {
            throw new ClientException(
                    "Unrecognize your object key wrap algorithm: " + contentMaterialRW.getKeyWrapAlgorithm());
        }

        try {
            KmsClientSuite kmsClientSuite = findKmsClientSuiteByDescription(contentMaterialRW.getMaterialsDescription());
            if (kmsClientSuite == null) {
                Entry<KmsClientSuite, Map<String, String>> entry = getTailByReflection(kmsDescMaterials);
                kmsClientSuite = entry.getKey();
            }

            DecryptResponse decryptIvResp = decryptCipherBlob(kmsClientSuite,
                    BinaryUtil.toBase64String(contentMaterialRW.getEncryptedIV()));
            byte[] iv = BinaryUtil.fromBase64String(decryptIvResp.getPlaintext());

            DecryptResponse decryptCEKResp = decryptCipherBlob(kmsClientSuite,
                    BinaryUtil.toBase64String(contentMaterialRW.getEncryptedCEK()));
            byte[] cekBytes = BinaryUtil.fromBase64String(decryptCEKResp.getPlaintext());
            SecretKey cek = new SecretKeySpec(cekBytes, "");

            contentMaterialRW.setCEK(cek);
            contentMaterialRW.setIV(iv);
        } catch (Exception e) {
            throw new ClientException("Unable to decrypt content secured key and iv. "
                    + "Please check your kms region and materails description." + e.getMessage(), e);
        }
    }

    private void assertParameterNotNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }
}