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

public class UrlComponentUtils {
    public static void validateComponent(String componentName, String componentValue)
            throws IllegalArgumentException {
        if (componentValue == null) {
            throw new IllegalArgumentException(componentName + " cannot be null");
        }

        for (int i = 0; i < componentValue.length(); ++i) {
            char next = componentValue.charAt(i);
            if (i == 0 && next == '-') {
                throw new IllegalArgumentException(componentName + " can not start with -");
            }
            if (next == '-' || next == '.')
                continue;
            if (next >= 'a' && next <= 'z')
                continue;

            if (next >= '0' && next <= '9')
                continue;

            if (next >= 'A' && next <= 'Z') {
                throw new IllegalArgumentException(
                        componentName + " should not contain uppercase characters");
            }

            if (next == ' ' || next == '\t' || next == '\r' || next == '\n') {
                throw new IllegalArgumentException(
                        componentName + " should not contain whitespace");
            }

            throw new IllegalArgumentException(
                    componentName + " only should contain lowercase characters, num, . and -");
        }
    }
    
    public static void validateRegionName(String regionName) throws IllegalArgumentException {
        validateComponent("region name", regionName);
    }
    
    public static void validateEndPointSuffix(String endPointSuffix) throws IllegalArgumentException {
        validateComponent("endpoint suffix", endPointSuffix);
    }
    
    public static void validateSrcEndPointSuffix(String srcEndPointSuffix) throws IllegalArgumentException {
        validateComponent("srcEndpoint suffix", srcEndPointSuffix);
    }
}
