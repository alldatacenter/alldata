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

package com.obs.services.model.fs;

import java.io.File;
import java.io.InputStream;

import com.obs.services.model.PutObjectRequest;

/**
 * Parameters in a file creation request
 *
 */
public class NewFileRequest extends PutObjectRequest {

    public NewFileRequest() {
    }

    public NewFileRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     * @param file
     *            Local path to the file
     */
    public NewFileRequest(String bucketName, String objectKey, File file) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.file = file;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     * @param input
     *            Data stream to be uploaded
     */
    public NewFileRequest(String bucketName, String objectKey, InputStream input) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.input = input;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     */
    public NewFileRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

}
