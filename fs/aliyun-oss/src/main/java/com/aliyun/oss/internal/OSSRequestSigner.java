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

package com.aliyun.oss.internal;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.SignVersion;

public class OSSRequestSigner implements RequestSigner {

    private String httpMethod;

    /* Note that resource path should not have been url-encoded. */
    private String resourcePath;

    private Credentials creds;

    private SignVersion signatureVersion;

    public OSSRequestSigner(String httpMethod, String resourcePath, Credentials creds, SignVersion signatureVersion) {
        this.httpMethod = httpMethod;
        this.resourcePath = resourcePath;
        this.creds = creds;
        this.signatureVersion = signatureVersion;
    }

    @Override
    public void sign(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
            String signature;

            if (signatureVersion == SignVersion.V2) {
                signature = SignV2Utils.buildSignature(secretAccessKey, httpMethod, resourcePath, request);
                request.addHeader(OSSHeaders.AUTHORIZATION, SignV2Utils.composeRequestAuthorization(accessKeyId,signature, request));
            } else {
                signature = SignUtils.buildSignature(secretAccessKey, httpMethod, resourcePath, request);
                request.addHeader(OSSHeaders.AUTHORIZATION, SignUtils.composeRequestAuthorization(accessKeyId, signature));
            }
        }
    }
}
