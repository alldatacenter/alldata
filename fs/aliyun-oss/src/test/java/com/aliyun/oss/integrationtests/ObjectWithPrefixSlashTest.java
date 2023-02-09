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

package com.aliyun.oss.integrationtests;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.ServiceException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import junit.framework.Assert;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_OBJECT_CONTENT_TYPE;


public class ObjectWithPrefixSlashTest extends TestBase {

    @Test
    public void testPutObject() {
        String objectName = "/abc中/put-object-slash-prefix/";
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream("123".getBytes()));
        } catch (ServiceException e) {
            Assert.assertEquals(e.getErrorCode(), "InvalidObjectName");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutObjectWithSignedUrl() {
        final String key = "/abc中/put-object-prefix-slash-by-urlSignature";

        final String expirationString = "Sun, 12 Apr 2025 12:00:00 GMT";
        final long inputStreamLength = 1024;

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
        try {
            Date expiration = DateUtil.parseRfc822Date(expirationString);
            request.setExpiration(expiration);
            request.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            request.addHeader("x-oss-head1", "value");
            request.addHeader("abc", "value");
            request.addQueryParameter("param1", "value1");
            URL signedUrl = ossClient.generatePresignedUrl(request);

            Map<String, String> requestHeaders = new HashMap<String, String>();

            requestHeaders.put("x-oss-head1", "value");
            requestHeaders.put("abc", "value");
            requestHeaders.put(HttpHeaders.CONTENT_TYPE, DEFAULT_OBJECT_CONTENT_TYPE);
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            // Using url signature & chunked encoding to upload specified inputstream.
            ossClient.putObject(signedUrl, instream, -1, requestHeaders, true);
        } catch (ServiceException e) {
            Assert.assertEquals(e.getErrorCode(), "InvalidObjectName");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
