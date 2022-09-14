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

package com.webank.wedatasphere.streamis.jobmanager.manager.dao;

import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface StreamTaskMapper {

    void insertTask(StreamTask streamTask);

    void updateTask(StreamTask streamTask);

    /**
     * Update task which in specific status
     * @param streamTask stream task
     * @param status status
     */
    int updateTaskInStatus(@Param("task")StreamTask streamTask, @Param("status")Integer status);

    List<StreamTask> getByJobVersionId(@Param("jobVersionId") Long jobVersionId, @Param("version") String version);

    /**
     * Get latest task by job version id
     * @param jobVersionId job version id
     * @param version version number
     * @return stream task
     */
    StreamTask getLatestByJobVersionId(@Param("jobVersionId") Long jobVersionId, @Param("version") String version);

    /**
     * Get the latest task by job id
     * @param jobId job id
     * @return stream task
     */
    StreamTask getLatestByJobId(@Param("jobId") Long jobId);

    /**
     * Get the latest task(launched) by job id
     * @param jobId job id
     * @return stream task
     */
    StreamTask getLatestLaunchedById(@Param("jobId") Long jobId);
    /**
     * Get earlier task list by job id
     * @param jobId job id
     * @param count the max number of task
     * @return
     */
    List<StreamTask> getEarlierByJobId(@Param("jobId") Long jobId, @Param("count") Integer count);

    StreamTask getRunningTaskByJobId(@Param("jobId") Long jobId);

    StreamTask getTaskById(@Param("id") Long id);

    List<StreamTask> getTasksByJobIdAndJobVersionId(@Param("jobId") Long jobId, @Param("jobVersionId") Long jobVersionId);

    List<StreamTask> getTasksByStatus(List<Integer> status);

    String getTask(@Param("jobId") Long jobId, @Param("version") String version);

    /**
     * Get status info of tasks by job ids
     * @param jobIds job ids
     * @return list
     */
    List<StreamTask> getStatusInfoByJobIds(@Param("jobIds")List<Long> jobIds);

}
