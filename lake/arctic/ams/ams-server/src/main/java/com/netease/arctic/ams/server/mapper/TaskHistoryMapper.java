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

import com.netease.arctic.ams.server.model.TableTaskHistory;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import com.netease.arctic.table.TableIdentifier;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TaskHistoryMapper {
  String TABLE_NAME = "optimize_task_history";

  @Select("select task_trace_id, retry, catalog_name, db_name, table_name, task_plan_group, " +
      "start_time, end_time, cost_time, queue_id from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName} and task_plan_group = #{taskPlanGroup}")
  @Results({
      @Result(column = "task_trace_id", property = "taskTraceId"),
      @Result(column = "retry", property = "retry"),
      @Result(column = "catalog_name", property = "tableIdentifier.catalog"),
      @Result(column = "db_name", property = "tableIdentifier.database"),
      @Result(column = "table_name", property = "tableIdentifier.tableName"),
      @Result(column = "task_plan_group", property = "taskPlanGroup"),
      @Result(column = "start_time", property = "startTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "end_time", property = "endTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "cost_time", property = "costTime"),
      @Result(column = "queue_id", property = "queueId")
  })
  List<TableTaskHistory> selectTaskHistory(@Param("tableIdentifier") TableIdentifier tableIdentifier,
                                           @Param("taskPlanGroup") String taskPlanGroup);

  @Select("select task_trace_id, retry, catalog_name, db_name, table_name, task_plan_group, " +
      "start_time, end_time, cost_time, queue_id from " + TABLE_NAME + " where " +
      "task_trace_id = #{taskTraceId} order by retry desc")
  @Results({
      @Result(column = "task_trace_id", property = "taskTraceId"),
      @Result(column = "retry", property = "retry"),
      @Result(column = "catalog_name", property = "tableIdentifier.catalog"),
      @Result(column = "db_name", property = "tableIdentifier.database"),
      @Result(column = "table_name", property = "tableIdentifier.tableName"),
      @Result(column = "task_plan_group", property = "taskPlanGroup"),
      @Result(column = "start_time", property = "startTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "end_time", property = "endTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "cost_time", property = "costTime"),
      @Result(column = "queue_id", property = "queueId")
  })
  List<TableTaskHistory> selectTaskHistoryByTraceId(@Param("taskTraceId") String taskTraceId);

  @Insert("insert into " + TABLE_NAME + "(task_trace_id, retry, catalog_name, db_name, table_name, " +
      "task_plan_group, start_time, end_time, cost_time, queue_id) values ( " +
      "#{taskHistory.taskTraceId}, " +
      "#{taskHistory.retry}, " +
      "#{taskHistory.tableIdentifier.catalog}, " +
      "#{taskHistory.tableIdentifier.database}, " +
      "#{taskHistory.tableIdentifier.tableName}, " +
      "#{taskHistory.taskPlanGroup}, " +
      "#{taskHistory.startTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      "#{taskHistory.endTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}," +
      "#{taskHistory.costTime}, " +
      "#{taskHistory.queueId}) ")
  void insertTaskHistory(@Param("taskHistory") TableTaskHistory taskHistory);

  @Update("update " + TABLE_NAME + " set " +
      "start_time = #{taskHistory.startTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
      "end_time = #{taskHistory.endTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
      "cost_time = #{taskHistory.costTime} " +
      "where " +
      "task_trace_id = #{taskHistory.taskTraceId} and retry = #{taskHistory.retry}")
  void updateTaskHistory(@Param("taskHistory") TableTaskHistory taskHistory);

  @Select("select task_trace_id, retry, catalog_name, db_name, table_name, task_plan_group, " +
      "start_time, end_time, cost_time, queue_id from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and " +
      "db_name = #{tableIdentifier.database} and " +
      "table_name = #{tableIdentifier.tableName} " +
      "and (end_time > #{startTime, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor} " +
      "or end_time is null)" +
      "and start_time < #{endTime, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor} ")
  @Results({
      @Result(column = "task_trace_id", property = "taskTraceId"),
      @Result(column = "retry", property = "retry"),
      @Result(column = "catalog_name", property = "tableIdentifier.catalog"),
      @Result(column = "db_name", property = "tableIdentifier.database"),
      @Result(column = "table_name", property = "tableIdentifier.tableName"),
      @Result(column = "task_plan_group", property = "taskPlanGroup"),
      @Result(column = "start_time", property = "startTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "end_time", property = "endTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "cost_time", property = "costTime"),
      @Result(column = "queue_id", property = "queueId")
  })
  List<TableTaskHistory> selectTaskHistoryByTableIdAndTime(@Param("tableIdentifier") TableIdentifier tableIdentifier,
                                                           @Param("startTime") long startTime,
                                                           @Param("endTime") long endTime);

  @Delete("delete from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName}")
  void deleteTaskHistory(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Delete("delete from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName} and task_plan_group = #{taskPlanGroup}")
  void deleteTaskHistoryWithPlanGroup(@Param("tableIdentifier") TableIdentifier tableIdentifier,
                                      @Param("taskPlanGroup") String taskPlanGroup);

  @Delete("delete from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName}" +
      "and end_time < #{expireTime, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}" +
      "and task_plan_group != #{latestTaskPlanGroup}")
  void expireTaskHistory(@Param("tableIdentifier") TableIdentifier identifier,
                         @Param("latestTaskPlanGroup") String latestTaskPlanGroup,
                         @Param("expireTime") long expireTime);
}
