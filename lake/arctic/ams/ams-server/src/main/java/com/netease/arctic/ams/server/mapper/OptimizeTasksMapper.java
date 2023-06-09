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

import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.ams.server.mybatis.ListOfTreeNode2StringConverter;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import com.netease.arctic.ams.server.mybatis.Map2StringConverter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OptimizeTasksMapper {
  String TABLE_NAME = "optimize_task";

  @Select("select trace_id, optimize_type, catalog_name, db_name, table_name, `partition`," +
      " task_commit_group, task_plan_group, to_sequence, from_sequence, " +
      " source_nodes, create_time, properties, queue_id," +
      " insert_file_size, delete_file_size, base_file_size, pos_delete_file_size," +
      " insert_files, delete_files, base_files, pos_delete_files" +
      " from " + TABLE_NAME)
  @Results({
      @Result(property = "tableIdentifier.catalog", column = "catalog_name"),
      @Result(property = "tableIdentifier.database", column = "db_name"),
      @Result(property = "tableIdentifier.tableName", column = "table_name"),
      @Result(property = "taskId.traceId", column = "trace_id"),
      @Result(property = "taskId.type", column = "optimize_type"),
      @Result(property = "partition", column = "partition"),
      @Result(property = "taskCommitGroup", column = "task_commit_group"),
      @Result(property = "taskPlanGroup", column = "task_plan_group"),
      @Result(property = "toSequence", column = "to_sequence"),
      @Result(property = "fromSequence", column = "from_sequence"),
      @Result(property = "queueId", column = "queue_id"),
      @Result(property = "insertFileSize", column = "insert_file_size"),
      @Result(property = "deleteFileSize", column = "delete_file_size"),
      @Result(property = "baseFileSize", column = "base_file_size"),
      @Result(property = "posDeleteFileSize", column = "pos_delete_file_size"),
      @Result(property = "insertFileCnt", column = "insert_files"),
      @Result(property = "deleteFileCnt", column = "delete_files"),
      @Result(property = "baseFileCnt", column = "base_files"),
      @Result(property = "posDeleteFileCnt", column = "pos_delete_files"),
      @Result(property = "createTime", column = "create_time",
          typeHandler = Long2TsConvertor.class),
      @Result(property = "properties", column = "properties",
          typeHandler = Map2StringConverter.class),
      @Result(property = "sourceNodes", column = "source_nodes",
          typeHandler = ListOfTreeNode2StringConverter.class)
  })
  List<BasicOptimizeTask> selectAllOptimizeTasks();

  @Insert("insert into " + TABLE_NAME + " (" +
      " trace_id, optimize_type, catalog_name, db_name, table_name, `partition`," +
      " task_commit_group, task_plan_group, to_sequence, from_sequence," +
      " source_nodes, create_time, properties, queue_id," +
      " insert_file_size, delete_file_size, base_file_size, pos_delete_file_size," +
      " insert_files, delete_files, base_files, pos_delete_files," +
      " status, pending_time, execute_time," +
      " prepared_time, report_time, commit_time, job_type, job_id, attempt_id, retry, fail_reason," +
      " fail_time, new_file_size, new_file_cnt, cost_time)" +
      " values(" +
      " #{optimizeTask.taskId.traceId}," +
      " #{optimizeTask.taskId.type}," +
      " #{optimizeTask.tableIdentifier.catalog}," +
      " #{optimizeTask.tableIdentifier.database}," +
      " #{optimizeTask.tableIdentifier.tableName}," +
      " #{optimizeTask.partition}," +
      " #{optimizeTask.taskCommitGroup}," +
      " #{optimizeTask.taskPlanGroup}," +
      " #{optimizeTask.toSequence}," +
      " #{optimizeTask.fromSequence}," +
      " #{optimizeTask.sourceNodes, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.ListOfTreeNode2StringConverter}," +
      " #{optimizeTask.createTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTask.properties, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter}," +
      " #{optimizeTask.queueId}," +
      " #{optimizeTask.insertFileSize}," +
      " #{optimizeTask.deleteFileSize}," +
      " #{optimizeTask.baseFileSize}," +
      " #{optimizeTask.posDeleteFileSize}," +
      " #{optimizeTask.insertFileCnt}," +
      " #{optimizeTask.deleteFileCnt}," +
      " #{optimizeTask.baseFileCnt}," +
      " #{optimizeTask.posDeleteFileCnt}," +
      " #{optimizeTaskRuntime.status}, " +
      " #{optimizeTaskRuntime.pendingTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTaskRuntime.executeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTaskRuntime.preparedTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTaskRuntime.reportTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTaskRuntime.commitTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTaskRuntime.jobId.type}," +
      " #{optimizeTaskRuntime.jobId.id}," +
      " #{optimizeTaskRuntime.attemptId}," +
      " #{optimizeTaskRuntime.retry}," +
      " #{optimizeTaskRuntime.failReason}," +
      " #{optimizeTaskRuntime.failTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      " #{optimizeTaskRuntime.newFileSize}," +
      " #{optimizeTaskRuntime.newFileCnt}," +
      " #{optimizeTaskRuntime.costTime}" +
      " )")
  void insertOptimizeTask(@Param("optimizeTask") BasicOptimizeTask optimizeTask,
                          @Param("optimizeTaskRuntime") OptimizeTaskRuntime optimizeTaskRuntime);

  @Delete("delete from " + TABLE_NAME + " where trace_id = #{traceId}")
  void deleteOptimizeTask(@Param("traceId") String traceId);
}
