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

import com.netease.arctic.ams.server.model.OptimizeQueueMeta;
import com.netease.arctic.ams.server.mybatis.Map2StringConverter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface OptimizeQueueMapper {
  String TABLE_NAME = "optimize_group";

  @Select("select group_id, name, scheduling_policy, properties, container from " + TABLE_NAME)
  @Results({
      @Result(property = "queueId", column = "group_id"),
      @Result(property = "name", column = "name"),
      @Result(property = "schedulingPolicy", column = "scheduling_policy"),
      @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class),
      @Result(property = "container", column = "container")
  })
  List<OptimizeQueueMeta> selectOptimizeQueues();

  @Select("select group_id, name, scheduling_policy, properties, container from " + TABLE_NAME + " where name = " +
      "#{queueName}")
  @Results({
      @Result(property = "queueId", column = "group_id"),
      @Result(property = "name", column = "name"),
      @Result(property = "schedulingPolicy", column = "scheduling_policy"),
      @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class),
      @Result(property = "container", column = "container")
  })
  OptimizeQueueMeta selectOptimizeQueue(@Param("queueName") String queueName);

  @Insert("insert into " + TABLE_NAME + " (name,scheduling_policy,properties,container) values " +
      "(#{optimizeQueue.name}, #{optimizeQueue.schedulingPolicy}, " +
      "#{optimizeQueue.properties, typeHandler=com.netease.arctic" +
      ".ams.server.mybatis.Map2StringConverter}, #{optimizeQueue.container})")
  void insertQueue(@Param("optimizeQueue") OptimizeQueueMeta optimizeQueue);

  @Delete("delete from " + TABLE_NAME + " where group_id = #{queueId}")
  void deleteQueue(int queueId);


  @Delete("delete from " + TABLE_NAME)
  void deleteAllQueue();

  @Update("update " + TABLE_NAME + " set" +
      " properties = #{optimizeQueue.properties, typeHandler=com.netease.arctic.ams.server.mybatis" +
      ".Map2StringConverter}," +
      " scheduling_policy = #{optimizeQueue.schedulingPolicy}," +
      " container = #{optimizeQueue.container}" +
      " where group_id = #{optimizeQueue.queueId}")
  void updateQueue(@Param("optimizeQueue") OptimizeQueueMeta optimizeQueue);
}