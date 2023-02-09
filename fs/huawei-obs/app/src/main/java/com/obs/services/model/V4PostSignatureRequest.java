/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.util.Date;


public class V4PostSignatureRequest extends PostSignatureRequest {

    public V4PostSignatureRequest() {

    }

    public V4PostSignatureRequest(long expires, String bucketName, String objectKey) {
        super(expires, bucketName, objectKey);
    }

    public V4PostSignatureRequest(Date expiryDate, String bucketName, String objectKey) {
        super(expiryDate, bucketName, objectKey);
    }

    public V4PostSignatureRequest(long expires, Date requestDate, String bucketName, String objectKey) {
        super(expires, requestDate, bucketName, objectKey);
    }

    public V4PostSignatureRequest(Date expiryDate, Date requestDate, String bucketName, String objectKey) {
        super(expiryDate, requestDate, bucketName, objectKey);
    }

}
