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

import com.obs.services.model.AppendObjectRequest;
import com.obs.services.model.HttpMethodEnum;

/**
 * Parameters in a request for writing data to a file
 *
 */
public class WriteFileRequest extends AppendObjectRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    public WriteFileRequest() {
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     */
    public WriteFileRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
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
    public WriteFileRequest(String bucketName, String objectKey, File file) {
        this(bucketName, objectKey);
        this.file = file;
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
     * @param position
     *            Start position for writing data to a file
     */
    public WriteFileRequest(String bucketName, String objectKey, File file, long position) {
        this(bucketName, objectKey, file);
        this.position = position;
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
    public WriteFileRequest(String bucketName, String objectKey, InputStream input) {
        this(bucketName, objectKey);
        this.input = input;
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
     * @param position
     *            Start position for writing data to a file
     */
    public WriteFileRequest(String bucketName, String objectKey, InputStream input, long position) {
        this(bucketName, objectKey, input);
        this.position = position;
    }
}
