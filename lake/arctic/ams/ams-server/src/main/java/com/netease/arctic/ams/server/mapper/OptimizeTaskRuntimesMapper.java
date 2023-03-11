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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.mapper;

import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface OptimizeTaskRuntimesMapper {
  String TABLE_NAME = "optimize_task";

  @Select("select trace_id, optimize_type, status, pending_time, execute_time, prepared_time, report_time," +
      " commit_time, job_type, job_id, retry, attempt_id, fail_reason, fail_time, new_file_size," +
      " new_file_cnt, cost_time from " + TABLE_NAME)
  @Results({
      @Result(property = "optimizeTaskId.traceId", column = "trace_id"),
      @Result(property = "optimizeTaskId.type", column = "optimize_type"),
      @Result(property = "status", column = "status"),
      @Result(property = "pendingTime", column = "pending_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "executeTime", column = "execute_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "preparedTime", column = "prepared_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "reportTime", column = "report_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "commitTime", column = "commit_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "costTime", column = "cost_time"),
      @Result(property = "jobId.type", column = "job_type"),
      @Result(property = "jobId.id", column = "job_id"),
      @Result(property = "retry", column = "retry"),
      @Result(property = "newFileSize", column = "new_file_size"),
      @Result(property = "newFileCnt", column = "new_file_cnt"),
      @Result(property = "attemptId", column = "attempt_id"),
      @Result(property = "errorMessage.failTime", column = "fail_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "errorMessage.failReason", column = "fail_reason")
  })
  List<OptimizeTaskRuntime> selectAllOptimizeTaskRuntimes();

  @Update("update " + TABLE_NAME + " set" +
      " status = #{optimizeTaskRuntime.status}," +
      " pending_time = #{optimizeTaskRuntime.pendingTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " execute_time = #{optimizeTaskRuntime.executeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " prepared_time = #{optimizeTaskRuntime.preparedTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " report_time = #{optimizeTaskRuntime.reportTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " commit_time = #{optimizeTaskRuntime.commitTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " job_type = #{optimizeTaskRuntime.jobId.type, jdbcType=VARCHAR}," +
      " job_id = #{optimizeTaskRuntime.jobId.id, jdbcType=VARCHAR}," +
      " attempt_id = #{optimizeTaskRuntime.attemptId, jdbcType=VARCHAR}," +
      " retry = #{optimizeTaskRuntime.retry}," +
      " fail_reason = #{optimizeTaskRuntime.failReason, jdbcType=VARCHAR}," +
      " fail_time = #{optimizeTaskRuntime.failTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " new_file_size = #{optimizeTaskRuntime.newFileSize}," +
      " new_file_cnt = #{optimizeTaskRuntime.newFileCnt}," +
      " cost_time = #{optimizeTaskRuntime.costTime}" +
      " where trace_id = #{optimizeTaskRuntime.optimizeTaskId.traceId}")
  void updateOptimizeTaskRuntime(
      @Param("optimizeTaskRuntime") OptimizeTaskRuntime optimizeTaskRuntime);
}
