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

import com.netease.arctic.ams.server.model.Optimizer;
import com.netease.arctic.ams.server.model.OptimizerResourceInfo;
import com.netease.arctic.ams.server.model.TableTaskStatus;
import com.netease.arctic.ams.server.mybatis.Map2StringConverter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * optimize mapper.
 */
@Mapper
public interface OptimizerMapper {
  String TABLE_NAME = "optimizer";

  @Select("select optimizer_id as jobId," +
      "queue_name as groupName," +
      "queue_id as queueId," +
      "optimizer_status as jobStatus," +
      "core_number as coreNumber," +
      "memory as memory," +
      "parallelism as parallelism," +
      "container as container," +
      "jobmanager_url as jobmanagerUrl from " + TABLE_NAME +
      " where queue_name = #{groupName} and (optimizer_status = " +
      "'RUNNING' or optimizer_status = 'STARTING')")
  List<Optimizer> selectOptimizersByGroupName(String groupName);

  @Select("select optimizer_id as jobId," +
      "queue_name as groupName," +
      "queue_id as queueId," +
      "optimizer_status as jobStatus," +
      "core_number as coreNumber," +
      "memory as memory," +
      "parallelism as parallelism," +
      "container as container," +
      "jobmanager_url as jobmanagerUrl," +
      "update_time as updateTime from " + TABLE_NAME +
      " where optimizer_status = 'RUNNING' or " +
      "optimizer_status = 'STARTING'")
  List<Optimizer> selectOptimizers();

  @Select("select sum(core_number) as occupationCore," +
      "sum(memory) as occupationMemory from " + TABLE_NAME + " where optimizer_status = 'RUNNING' or " +
      "optimizer_status = 'STARTING'")
  OptimizerResourceInfo selectOptimizerGroupResourceInfo();

  @Select("select sum(core_number) as occupationCore," +
      "sum(memory) as occupationMemory from " + TABLE_NAME +
      " where queue_name = #{groupName} " +
      "and (optimizer_status = 'RUNNING' or " +
      "optimizer_status = 'STARTING')")
  OptimizerResourceInfo selectOptimizerGroupResourceInfoByGroupName(String groupName);

  @Insert("insert into " + TABLE_NAME + "(optimizer_name, queue_id, queue_name, " +
      "optimizer_status, optimizer_start_time, core_number, memory, parallelism, container) values (" +
      "#{param1}, #{param2}, #{param3}, #{param4}, #{param5}, #{param6}, #{param7}, #{param8}, #{param9})")
  void insertOptimizer(String optimizerName, int queueId, String queueName, TableTaskStatus status, String startTime,
                       int coreNumber, long memory, int parallelism, String containerType);

  @Select("select optimizer_id from " + TABLE_NAME + " where optimizer_name = #{param1}")
  String selectOptimizerIdByOptimizerName(String optimizerName);

  @Delete("delete from " + TABLE_NAME + " where optimizer_name = #{optimizerName}")
  void deleteOptimizerByName(String optimizerName);

  @Select("select optimizer_id, queue_name, queue_id, optimizer_status, core_number, " +
      "memory, parallelism, container, jobmanager_url, optimizer_instance, optimizer_state_info ,update_time " +
      "from " + TABLE_NAME + " where optimizer_id = #{optimizerId}")
  @Results({
          @Result(property = "jobId", column = "optimizer_id"),
          @Result(property = "groupName", column = "queue_name"),
          @Result(property = "queueId", column = "queue_id"),
          @Result(property = "jobStatus", column = "optimizer_status"),
          @Result(property = "coreNumber", column = "core_number"),
          @Result(property = "memory", column = "memory"),
          @Result(property = "parallelism", column = "parallelism"),
          @Result(property = "container", column = "container"),
          @Result(property = "jobmanagerUrl", column = "jobmanager_url"),
          @Result(property = "instance", column = "optimizer_instance"),
          @Result(property = "stateInfo", column = "optimizer_state_info", typeHandler = Map2StringConverter.class),
          @Result(property = "updateTime", column = "update_time")
  })
  Optimizer selectOptimizer(@Param("optimizerId") Long optimizerId);

  @Select("select optimizer_id, queue_name, queue_id, optimizer_status, core_number, " +
      "memory, parallelism, container, jobmanager_url, optimizer_instance, optimizer_state_info ,update_time " +
      "from " + TABLE_NAME + " where optimizer_name = #{optimizerName}")
  @Results({
      @Result(property = "jobId", column = "optimizer_id"),
      @Result(property = "groupName", column = "queue_name"),
      @Result(property = "queueId", column = "queue_id"),
      @Result(property = "jobStatus", column = "optimizer_status"),
      @Result(property = "coreNumber", column = "core_number"),
      @Result(property = "memory", column = "memory"),
      @Result(property = "parallelism", column = "parallelism"),
      @Result(property = "container", column = "container"),
      @Result(property = "jobmanagerUrl", column = "jobmanager_url"),
      @Result(property = "instance", column = "optimizer_instance"),
      @Result(property = "stateInfo", column = "optimizer_state_info", typeHandler = Map2StringConverter.class),
      @Result(property = "updateTime", column = "update_time")
  })
  Optimizer selectOptimizerByName(@Param("optimizerName") String optimizerName);


  @Update("update " + TABLE_NAME + " set" +
      " optimizer_instance = #{instance}, optimizer_state_info = #{state,typeHandler=com.netease.arctic.ams" +
      ".server.mybatis.Map2StringConverter}, optimizer_status = #{status}, update_time = CURRENT_TIMESTAMP" +
      " where optimizer_id = #{optimizerId}")
  void updateOptimizerState(
      @Param("optimizerId") Long optimizerId,
      @Param("instance") byte[] instance, @Param("state") Map state, @Param("status") String status);

  @Update("update " + TABLE_NAME + " set optimizer_status = #{status} where optimizer_id = #{optimizerId}")
  void updateOptimizerStatus(@Param("optimizerId") Long optimizerId, @Param("status") String status);

  @Delete("delete from " + TABLE_NAME + " where optimizer_id = #{optimizerId}")
  void deleteOptimizer(@Param("optimizerId") Long optimizerId);

  @Update("update " + TABLE_NAME + " set optimizer_instance = #{instance} where optimizer_id = #{optimizerId}")
  void addOptimizerInstance(@Param("optimizerId") Long optimizerId, @Param("instance") byte[] instance);
}

