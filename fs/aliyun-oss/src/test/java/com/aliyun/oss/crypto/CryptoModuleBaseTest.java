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

import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CryptoModuleBaseTest {

    private class CryptoModuleBaseWrap extends CryptoModuleBase {
        CryptoModuleBaseWrap(OSSDirect OSS,
                           EncryptionMaterials encryptionMaterials,
                           CryptoConfiguration cryptoConfig) {
            super(OSS, encryptionMaterials, cryptoConfig);
        }

        @Override
        byte[] generateIV() {
            return new byte[0];
        }

        @Override
        CryptoCipher createCryptoCipherFromContentMaterial(ContentCryptoMaterial cekMaterial, int cipherMode, long[] cryptoRange, long skipBlock) {
            return null;
        }

        ContentCryptoMaterial TestCreateContentMaterialFromMetadata(ObjectMetadata meta)
        {
            return createContentMaterialFromMetadata(meta);
        }

        Map<String, String> TestGetDescFromJsonString(String jsonString)
        {
            return getDescFromJsonString(jsonString);
        }

        long[] TestGetAdjustedCryptoRange(long[] range)
        {
            return getAdjustedCryptoRange(range);
        }
    }

    private class OSSDirectImplWarp implements OSSDirect {
        private OSSClient client;
        OSSDirectImplWarp()
        {
            client = new OSSClient("http://endpoint", null, new ClientConfiguration());
        }

        @Override
        public ClientConfiguration getInnerClientConfiguration() {
            return client.getClientConfiguration();
        }

        @Override
        public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
            return client.putObject(putObjectRequest);
        }

        @Override
        public OSSObject getObject(GetObjectRequest getObjectRequest) {
            return client.getObject(getObjectRequest);
        }

        @Override
        public void abortMultipartUpload(AbortMultipartUploadRequest request) {
            client.abortMultipartUpload(request);
        }

        @Override
        public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
            return client.completeMultipartUpload(request);
        }

        @Override
        public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
            return client.initiateMultipartUpload(request);
        }

        @Override
        public UploadPartResult uploadPart(UploadPartRequest request) {
            return client.uploadPart(request);
        }
    }

    @Test
    public void testHasEncryptionInfo() {

        ObjectMetadata metadata = new ObjectMetadata();
        Assert.assertEquals(CryptoModuleBase.hasEncryptionInfo(metadata), false);

        metadata.addUserMetadata(CryptoHeaders.CRYPTO_KEY, "key");
        Assert.assertEquals(CryptoModuleBase.hasEncryptionInfo(metadata), false);

        metadata.addUserMetadata(CryptoHeaders.CRYPTO_IV, "iv");
        Assert.assertEquals(CryptoModuleBase.hasEncryptionInfo(metadata), true);
    }

    @Test
    public void testCryptoModuleBaseWrap() {
        Map<String, String> matDesc = new HashMap<String, String>();
        matDesc.put("<yourDescriptionKey>", "<yourDescriptionValue>");

        KmsEncryptionMaterials encryptionMaterials = new KmsEncryptionMaterials("region", "cmk", matDesc);
        CryptoConfiguration cryptoConfig = new CryptoConfiguration();

        CryptoModuleBaseWrap wrap = new CryptoModuleBaseWrap(new OSSDirectImplWarp(), encryptionMaterials, cryptoConfig);

        ObjectMetadata meta = new ObjectMetadata();

        try {
            wrap.TestCreateContentMaterialFromMetadata(meta);
            Assert.assertTrue(false);
        }
        catch (ClientException e) {
            Assert.assertTrue(true);
        }

        try {
            meta.addUserMetadata(CryptoHeaders.CRYPTO_KEY, "key");
            wrap.TestCreateContentMaterialFromMetadata(meta);
            Assert.assertTrue(false);
        }
        catch (ClientException e) {
            Assert.assertTrue(true);
        }

        try {
            meta.addUserMetadata(CryptoHeaders.CRYPTO_IV, "iv");
            wrap.TestCreateContentMaterialFromMetadata(meta);
            Assert.assertTrue(false);
        }
        catch (ClientException e) {
            Assert.assertTrue(true);
        }

        try {
            meta.addUserMetadata(CryptoHeaders.CRYPTO_WRAP_ALG, "alg");
            wrap.TestCreateContentMaterialFromMetadata(meta);
            Assert.assertTrue(false);
        }
        catch (ClientException e) {
            Assert.assertTrue(true);
        }


        //getDescFromJsonString
        wrap.TestGetDescFromJsonString(null);
        Assert.assertTrue(true);

        try {
            wrap.TestGetDescFromJsonString("xxx");
            Assert.assertTrue(false);
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }


        //getAdjustedCryptoRange
        try {
            long[] range = new long[2];
            range[0] = 1;
            range[1] = 0;
            wrap.TestGetAdjustedCryptoRange(range);
            Assert.assertTrue(false);
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            long[] range = new long[2];
            range[0] = -1;
            range[1] = 0;
            wrap.TestGetAdjustedCryptoRange(range);
            Assert.assertTrue(false);
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            long[] range = new long[2];
            range[0] = 0;
            range[1] = 0;
            wrap.TestGetAdjustedCryptoRange(range);
            Assert.assertTrue(false);
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}
