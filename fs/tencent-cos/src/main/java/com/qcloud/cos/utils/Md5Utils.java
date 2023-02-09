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


package com.qcloud.cos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class Md5Utils {
    public static byte[] computeMD5Hash(InputStream is) throws IOException {
        return DigestUtils.md5(is);
    }

    /**
     * Returns the MD5 in base64 for the data from the given input stream. Note this method closes
     * the given input stream upon completion.
     */
    public static String md5AsBase64(InputStream is) throws IOException {
        return Base64.encodeAsString(computeMD5Hash(is));
    }

    /**
     * Computes the MD5 hash of the given data and returns it as an array of bytes.
     */
    public static byte[] computeMD5Hash(byte[] input) {
        return DigestUtils.md5(input);
    }

    /**
     * Returns the MD5 in base64 for the given byte array.
     */
    public static String md5AsBase64(byte[] input) {
        return Base64.encodeAsString(computeMD5Hash(input));
    }

    public static String md5Hex(File file) throws FileNotFoundException, IOException {
        return Hex.encodeHexString(computeMD5Hash(file));
    }

    public static String md5Hex(String utf8Content) {
        return Hex.encodeHexString(computeMD5Hash(utf8Content.getBytes(StringUtils.UTF8)));
    }
    

    public static String md5Hex(byte[] input) {
        return Hex.encodeHexString(computeMD5Hash(input));
    }

    /**
     * Computes the MD5 of the given file.
     */
    public static byte[] computeMD5Hash(File file) throws FileNotFoundException, IOException {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            return computeMD5Hash(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Returns the MD5 in base64 for the given file.
     */
    public static String md5AsBase64(File file) throws FileNotFoundException, IOException {
        return Base64.encodeAsString(computeMD5Hash(file));
    }

}
