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

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

/**
 * Used to provide direct access to the underlying/original OSS client methods
 * free of any added cryptographic functionalities.
 */
public interface OSSDirect {
    public ClientConfiguration getInnerClientConfiguration();

    public PutObjectResult putObject(PutObjectRequest putObjectRequest);

    public OSSObject getObject(GetObjectRequest getObjectRequest);

    public void abortMultipartUpload(AbortMultipartUploadRequest request);

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request);

    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request);

    public UploadPartResult uploadPart(UploadPartRequest request);
}
