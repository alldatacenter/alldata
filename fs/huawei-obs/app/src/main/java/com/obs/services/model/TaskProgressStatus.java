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

public interface TaskProgressStatus {

    /**
     * Obtain the upload progress in percentage.
     * 
     * @return Upload progress in percentage
     */
    public int getExecPercentage();

    /**
     * Obtain the number of objects being uploaded.
     * 
     * @return Number of objects being uploaded
     */
    public int getExecTaskNum();

    /**
     * Obtain the number of objects that have been successfully uploaded.
     * 
     * @return Number of objects that have been successfully uploaded
     */
    public int getSucceedTaskNum();

    /**
     * Obtain the number of objects that fail to be uploaded.
     * 
     * @return Number of objects that fail to be uploaded
     */
    public int getFailTaskNum();

    /**
     * Obtain the total number of objects in the upload task.
     * 
     * @return Total number of objects in the upload task
     */
    public int getTotalTaskNum();
}
