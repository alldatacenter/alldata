/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.internal.crypto;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.qcloud.cos.Headers;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectId;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.utils.StringUtils;

public class COSObjectWrapper implements Closeable {
    private final COSObject cosobj;
    private final COSObjectId id;

    COSObjectWrapper(COSObject cosobj, COSObjectId id) {
        if (cosobj == null)
            throw new IllegalArgumentException();
        this.cosobj = cosobj;
        this.id = id;
    }

    public COSObjectId getCOSObjectId() {
        return id;
    }

    ObjectMetadata getObjectMetadata() {
        return cosobj.getObjectMetadata();
    }

    void setObjectMetadata(ObjectMetadata metadata) {
        cosobj.setObjectMetadata(metadata);
    }

    COSObjectInputStream getObjectContent() {
        return cosobj.getObjectContent();
    }

    void setObjectContent(COSObjectInputStream objectContent) {
        cosobj.setObjectContent(objectContent);
    }

    void setObjectContent(InputStream objectContent) {
        cosobj.setObjectContent(objectContent);
    }

    String getBucketName() {
        return cosobj.getBucketName();
    }

    void setBucketName(String bucketName) {
        cosobj.setBucketName(bucketName);
    }

    String getKey() {
        return cosobj.getKey();
    }

    void setKey(String key) {
        cosobj.setKey(key);
    }

    @Override
    public String toString() {
        return cosobj.toString();
    }

    /**
     * Returns true if this COS object has the encryption information stored as user meta data; false
     * otherwise.
     */
    final boolean hasEncryptionInfo() {
        ObjectMetadata metadata = cosobj.getObjectMetadata();
        Map<String, String> userMeta = metadata.getUserMetadata();
        return userMeta != null
                && (userMeta.containsKey(Headers.CRYPTO_IV)
                        || userMeta.containsKey(Headers.ENCRYPTION_START))
                && (userMeta.containsKey(Headers.CRYPTO_KEY_V2)
                        || userMeta.containsKey(Headers.CRYPTO_KEY))
                        || userMeta.containsKey(Headers.ENCRYPTION_KEY);
    }

    /**
     * Converts and return the underlying COS object as a json string.
     * 
     * @throws CosClientException if failed in JSON conversion.
     */
    String toJsonString() {
        try {
            return from(cosobj.getObjectContent());
        } catch (Exception e) {
            throw new CosClientException("Error parsing JSON: " + e.getMessage());
        }
    }

    private static String from(InputStream is) throws IOException {
        if (is == null)
            return "";
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StringUtils.UTF8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            is.close();
        }
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        cosobj.close();
    }

    COSObject getCOSObject() {
        return cosobj;
    }

    /**
     * Returns the original crypto scheme used for encryption, which may differ from the crypto
     * scheme used for decryption during, for example, a range-get operation.
     * 
     * @param instructionFile the instruction file of the cos object; or null if there is none.
     */
    ContentCryptoScheme encryptionSchemeOf(Map<String, String> instructionFile) {
        if (instructionFile != null) {
            String cekAlgo = instructionFile.get(Headers.CRYPTO_CEK_ALGORITHM);
            return ContentCryptoScheme.fromCEKAlgo(cekAlgo);
        }
        ObjectMetadata meta = cosobj.getObjectMetadata();
        Map<String, String> userMeta = meta.getUserMetadata();
        String cekAlgo = userMeta.get(Headers.CRYPTO_CEK_ALGORITHM);
        return ContentCryptoScheme.fromCEKAlgo(cekAlgo);
    }
}
