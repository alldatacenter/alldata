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

import com.obs.services.model.BaseObjectRequest;
import com.obs.services.model.HttpMethodEnum;

/**
 * Parameters in a request for renaming a file or folder
 *
 */
public class RenameRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.POST;
    }

    private String newObjectKey;

    public RenameRequest() {

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File or folder name
     * @param newObjectKey
     *            New file or folder name
     */
    public RenameRequest(String bucketName, String objectKey, String newObjectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.newObjectKey = newObjectKey;
    }

    /**
     * Obtain the new file or folder name.
     * 
     * @return New file or folder name
     */
    public String getNewObjectKey() {
        return newObjectKey;
    }

    /**
     * Set the new file or folder name.
     * 
     * @param newObjectKey
     *            New file or folder name
     */
    public void setNewObjectKey(String newObjectKey) {
        this.newObjectKey = newObjectKey;
    }

}
