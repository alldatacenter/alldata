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

package com.netease.arctic.server.persistence.mapper;

import com.netease.arctic.server.persistence.converter.Long2TsConverter;
import com.netease.arctic.server.persistence.converter.Map2StringConverter;
import com.netease.arctic.server.resource.OptimizerInstance;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * optimize mapper.
 */
@Mapper
public interface OptimizerMapper {

  @Insert("INSERT INTO optimizer (token, resource_id, group_name,container_name, start_time, touch_time, " +
      "thread_count, total_memory, properties) VALUES (#{optimizer.token}, #{optimizer.resourceId, jdbcType=VARCHAR}," +
      " #{optimizer.groupName}, #{optimizer.containerName}," +
      " #{optimizer.startTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      " #{optimizer.touchTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      " #{optimizer.threadCount}, #{optimizer.memoryMb}," +
      " #{optimizer.properties, typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter})")
  void insertOptimizer(@Param("optimizer") OptimizerInstance optimizer);

  @Update("UPDATE optimizer SET touch_time = CURRENT_TIMESTAMP WHERE token = #{token}")
  void updateTouchTime(@Param("token") String token);

  @Delete("DELETE FROM optimizer WHERE token = #{token}")
  void deleteOptimizer(@Param("token") String token);

  @Select("SELECT token, resource_id, group_name, container_name, start_time, touch_time," +
      "thread_count, total_memory, properties FROM optimizer")
  @Results({
      @Result(property = "token", column = "token"),
      @Result(property = "resourceId", column = "resource_id"),
      @Result(property = "groupName", column = "group_name"),
      @Result(property = "containerName", column = "container_name"),
      @Result(property = "startTime", column = "start_time", typeHandler = Long2TsConverter.class),
      @Result(property = "touchTime", column = "touch_time", typeHandler = Long2TsConverter.class),
      @Result(property = "threadCount", column = "thread_count"),
      @Result(property = "memoryMb", column = "total_memory"),
      @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class),
  })
  List<OptimizerInstance> selectAll();

  @Select("SELECT token, resource_id, group_name, container_name, start_time, touch_time," +
      "thread_count, total_memory, properties FROM optimizer WHERE resource_id = #{resourceId}")
  @Results({
      @Result(property = "token", column = "token"),
      @Result(property = "resourceId", column = "resource_id"),
      @Result(property = "groupName", column = "group_name"),
      @Result(property = "containerName", column = "container_name"),
      @Result(property = "startTime", column = "start_time", typeHandler = Long2TsConverter.class),
      @Result(property = "touchTime", column = "touch_time", typeHandler = Long2TsConverter.class),
      @Result(property = "threadCount", column = "thread_count"),
      @Result(property = "memoryMb", column = "total_memory"),
      @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class),
  })
  List<OptimizerInstance> selectByResourceId(@Param("resourceId") String resourceId);
}

