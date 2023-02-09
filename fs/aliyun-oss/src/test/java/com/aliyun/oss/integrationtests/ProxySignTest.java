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

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Ignore;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.auth.ServiceSignature;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.internal.SignUtils;

/**
 * Test proxy sign
 */
public class ProxySignTest {
    
    private static String HEADER_PROXY_TYPE = "x-oss-proxy-tunnel-type";
    private static String HEADER_PROXY_USER = "x-oss-proxy-user";
    private static String HEADER_PROXY_DEST = "x-oss-proxy-destination";
    private static String HEADER_PROXY_DEST_REGION = "x-oss-proxy-destination-region";
    private static String HEADER_PROXY_REAL_HOST = "x-oss-proxy-realhost";
    
    private static String HEADER_PROXY_AUTH = "x-drs-proxy-authorization";
    
    class ProxyCredentials implements Credentials {

        public ProxyCredentials(String securityToken) {
            this.securityToken = securityToken;
        }

        @Override
        public String getAccessKeyId() {
            return null;
        }

        @Override
        public String getSecretAccessKey() {
            return null;
        }

        @Override
        public String getSecurityToken() {
            return securityToken;
        }

        @Override
        public boolean useSecurityToken() {
            return true;
        }
        
        private String securityToken;
    }
    
    
    class ProxyRequestSigner implements RequestSigner {
        
        public ProxyRequestSigner(Credentials creds) {
            this.creds = creds;
        }

        @Override
        public void sign(RequestMessage request) throws ClientException {
            String authToken = creds.getSecurityToken();
            String canonicalString = SignUtils.buildCanonicalString(request.getMethod().toString(),
                    buildResourcePath(request), request, null);
            String signature = ServiceSignature.create().computeSignature(authToken, canonicalString);
            request.addHeader(HEADER_PROXY_AUTH, signature);
        }
        
        private String buildResourcePath(RequestMessage request) {
            String bucketName = request.getBucket();
            String key = request.getKey();
            String resourcePath = "/" + ((bucketName != null) ? bucketName + "/" : "") + ((key != null ? key : ""));
            return resourcePath;
        }
        
        private Credentials creds;
    }

    @Ignore
    public void testProxyAuth() {
        String bucketName = "sdk-test-md-1";
        String key = "mingdi/test.txt";
        String content = "Hello OSS.";
        String proxyHost = "";
        String endpoint = "";
        String accessKeyId = "";
        String secretAccessKey = "";
        
        try {
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            conf.setProxyHost(proxyHost);
            conf.setProxyPort(8080);
            
            Credentials credentials = new ProxyCredentials("mingditest");
            ProxyRequestSigner proxySigner= new ProxyRequestSigner(credentials);
            List<RequestSigner> signerHandlers = new LinkedList<RequestSigner>();
            signerHandlers.add(proxySigner);
            conf.setSignerHandlers(signerHandlers);
            
            Map<String, String> proxyHeaders = new LinkedHashMap<String, String>();
            proxyHeaders.put(HEADER_PROXY_TYPE, "default");
            proxyHeaders.put(HEADER_PROXY_USER, "diff_bucket_sync_test_user_1");
            proxyHeaders.put(HEADER_PROXY_DEST, "cn-qingdao");
            proxyHeaders.put(HEADER_PROXY_DEST_REGION, "cn-qingdao");
            proxyHeaders.put(HEADER_PROXY_REAL_HOST, endpoint);
            conf.setDefaultHeaders(proxyHeaders);

            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, secretAccessKey, conf);

            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.getObject(bucketName, key);
            ossClient.deleteObject(bucketName, key);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
}
