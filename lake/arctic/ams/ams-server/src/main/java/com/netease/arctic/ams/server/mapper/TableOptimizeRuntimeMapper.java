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

import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import com.netease.arctic.ams.server.mybatis.MapLong2StringConverter;
import com.netease.arctic.table.TableIdentifier;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TableOptimizeRuntimeMapper {
  String TABLE_NAME = "optimize_table_runtime";

  @Select("select catalog_name, db_name, table_name, current_snapshot_id, current_change_snapshotId," +
      " latest_major_optimize_time, latest_full_optimize_time, latest_minor_optimize_time, latest_task_plan_group," +
      " optimize_status, optimize_status_start_time " +
      " from " + TABLE_NAME)
  @ConstructorArgs({
      @Arg(column = "catalog_name", javaType = String.class),
      @Arg(column = "db_name", javaType = String.class),
      @Arg(column = "table_name", javaType = String.class)
  })
  @Results({
      @Result(property = "currentSnapshotId", column = "current_snapshot_id"),
      @Result(property = "currentChangeSnapshotId", column = "current_change_snapshotId"),
      @Result(property = "latestMajorOptimizeTime", column = "latest_major_optimize_time",
          typeHandler = MapLong2StringConverter.class),
      @Result(property = "latestFullOptimizeTime", column = "latest_full_optimize_time",
          typeHandler = MapLong2StringConverter.class),
      @Result(property = "latestMinorOptimizeTime", column = "latest_minor_optimize_time",
          typeHandler = MapLong2StringConverter.class),
      @Result(property = "latestTaskPlanGroup", column = "latest_task_plan_group"),
      @Result(property = "optimizeStatus", column = "optimize_status"),
      @Result(property = "optimizeStatusStartTime", column = "optimize_status_start_time",
          typeHandler = Long2TsConvertor.class)
  })
  List<TableOptimizeRuntime> selectTableOptimizeRuntimes();

  @Update("update " + TABLE_NAME + " set " +
      "current_snapshot_id = #{runtime.currentSnapshotId}, " +
      "current_change_snapshotId = #{runtime.currentChangeSnapshotId}, " +
      "latest_major_optimize_time = #{runtime.latestMajorOptimizeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.MapLong2StringConverter}, " +
      "latest_full_optimize_time = #{runtime.latestFullOptimizeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.MapLong2StringConverter}, " +
      "latest_minor_optimize_time = #{runtime.latestMinorOptimizeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.MapLong2StringConverter}, " +
      "latest_task_plan_group = #{runtime.latestTaskPlanGroup, jdbcType=VARCHAR}, " +
      "optimize_status = #{runtime.optimizeStatus}, " +
      "optimize_status_start_time = #{runtime.optimizeStatusStartTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor} " +
      "where " +
      "catalog_name = #{runtime.tableIdentifier.catalog} and " +
      "db_name = #{runtime.tableIdentifier.database} and " +
      "table_name = #{runtime.tableIdentifier.tableName}")
  void updateTableOptimizeRuntime(@Param("runtime") TableOptimizeRuntime runtime);

  @Delete("delete from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and " +
      "db_name = #{tableIdentifier.database} and " +
      "table_name = #{tableIdentifier.tableName}")
  void deleteTableOptimizeRuntime(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Insert("insert into " + TABLE_NAME + " (catalog_name, db_name, table_name, " +
      "current_snapshot_id, current_change_snapshotId, latest_major_optimize_time, latest_full_optimize_time, " +
      "latest_minor_optimize_time, latest_task_plan_group, optimize_status, optimize_status_start_time) values ( " +
      "#{runtime.tableIdentifier.catalog}, " +
      "#{runtime.tableIdentifier.database}, " +
      "#{runtime.tableIdentifier.tableName}, " +
      "#{runtime.currentSnapshotId}, " +
      "#{runtime.currentChangeSnapshotId}, " +
      "#{runtime.latestMajorOptimizeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.MapLong2StringConverter}," +
      "#{runtime.latestFullOptimizeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.MapLong2StringConverter}," +
      "#{runtime.latestMinorOptimizeTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.MapLong2StringConverter}," +
      "#{runtime.latestTaskPlanGroup, jdbcType=VARCHAR}," +
      "#{runtime.optimizeStatus}," +
      "#{runtime.optimizeStatusStartTime," +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}" +
      ") ")
  void insertTableOptimizeRuntime(@Param("runtime") TableOptimizeRuntime runtime);
}
