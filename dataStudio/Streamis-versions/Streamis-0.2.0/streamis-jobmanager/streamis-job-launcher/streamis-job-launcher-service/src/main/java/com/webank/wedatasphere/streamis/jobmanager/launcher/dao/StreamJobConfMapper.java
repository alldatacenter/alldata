/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.dao;

import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.JobConfDefinition;
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.JobConfValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Operate the job configuration
 */
public interface StreamJobConfMapper {

    /**
     * Select all config definitions
     * @return list
     */
    List<JobConfDefinition> loadAllDefinitions();

    /**
     * Get raw value
     * @param jobId job id
     * @param key key
     * @return
     */
    String getRawConfValue(@Param("jobId")Long jobId, @Param("key")String key);
    /**
     * Get config values by job id
     * @param jobId job id
     * @return
     */
    List<JobConfValue> getConfValuesByJobId(@Param("jobId")Long jobId);

    /**
     * Delete values by job id
     * @param jobId job id
     */
    int deleteConfValuesByJobId(@Param("jobId")Long jobId);

    /**
     * Delete temporary config value
     * @param jobId job id
     * @return affect rows
     */
    int deleteTemporaryConfValue(@Param("jobId")Long jobId);
    /**
     * Batch insert
     * @param values values
     */
    void batchInsertValues(@Param("values")List<JobConfValue> values);
}
