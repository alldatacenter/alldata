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

package com.aliyun.oss;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.comm.DefaultServiceClient;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.OSSObjectOperation;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.ObjectMetadata;

public class OSSObjectOperationTest {
    @Test
    public void testPopulateCopyObjectHeaders() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, URISyntaxException {
        CopyObjectRequest request = new CopyObjectRequest("srcbucket",
            "srckey", "destbucket", "destkey");
        request.setServerSideEncryption(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        Method[] testMethods = OSSObjectOperation.class.getDeclaredMethods();
        Method testMethod = null;
        for (Method m : testMethods) {
            if (m.getName().equals("populateCopyObjectHeaders")) {
                testMethod = m;
            }
        }
        testMethod.setAccessible(true);
        OSSObjectOperation operations = new OSSObjectOperation(
            new DefaultServiceClient(new ClientConfiguration()),
            new DefaultCredentialProvider(new DefaultCredentials("id", "key")));
        Map<String, String> headers = new HashMap<String, String>();
        Object[] params = new Object[2];
        params[0] = request;
        params[1] = headers;
        testMethod.invoke(operations, params);
        assertEquals("/srcbucket/srckey", headers.get(OSSHeaders.COPY_OBJECT_SOURCE));
        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION, headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
        assertEquals(null, headers.get(OSSHeaders.COPY_OBJECT_METADATA_DIRECTIVE));
    }
}
