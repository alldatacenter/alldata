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


package com.qcloud.cos.internal;

public enum BucketNameUtils {
    ;
    private static final int MIN_BUCKET_NAME_LENGTH = 1;

    public static void validateBucketName(final String bucketName) throws IllegalArgumentException {
        if (bucketName == null) {
            throw new IllegalArgumentException("Bucket Name cannot be null");
        }
        String bucketNameNotContainAppid = bucketName;
        if (bucketName.contains("-") && bucketName.lastIndexOf("-") != 0) {
            bucketNameNotContainAppid = bucketName.substring(0, bucketName.lastIndexOf("-"));
        }
        if (bucketNameNotContainAppid.length() < MIN_BUCKET_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "bucketName length must at least 1 character long");
        }

        for (int i = 0; i < bucketNameNotContainAppid.length(); ++i) {
            char next = bucketNameNotContainAppid.charAt(i);
            if (i == 0 && next == '-') { 
                throw new IllegalArgumentException("Bucket name can not start with -");
            }
            if (next == '-') 
                continue;
            if (next >= 'a' && next <= 'z')
                continue;

            if (next >= '0' && next <= '9')
                continue;

            if (next >= 'A' && next <= 'Z') {
                throw new IllegalArgumentException(
                        "Bucket name should not contain uppercase characters");
            }

            if (next == ' ' || next == '\t' || next == '\r' || next == '\n') {
                throw new IllegalArgumentException("Bucket name should not contain whitespace");
            }
            
            throw new IllegalArgumentException(
                    "Bucket name only should contain lowercase characters, num and -");
        }
    }
}
