/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.repository.mapper;

import io.datavines.server.repository.entity.JobExecutionResult;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JobExecutionResultMapper extends BaseMapper<JobExecutionResult>  {

    @Select("SELECT * from dv_job_execution_result where job_execution_id = #{jobExecutionId} limit 1 ")
    JobExecutionResult getOne(Long jobExecutionId);

    @Select("SELECT * from dv_job_execution_result where job_execution_id in (select id from dv_job_execution where job_id = #{jobId} and start_time >= #{startTime} and start_time <= #{endTime}) order by create_time")
    List<JobExecutionResult> listByJobIdAndTimeRange(@Param("jobId") Long jobId,
                                                     @Param("startTime")String startTime,
                                                     @Param("endTime")String endTime);
}
