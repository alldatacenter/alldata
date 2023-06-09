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

import com.netease.arctic.ams.server.model.Container;
import com.netease.arctic.ams.server.mybatis.Map2StringConverter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ContainerMetadataMapper {

  String TABLE_NAME = "container_metadata";

  @Select("select name, type, properties from " + TABLE_NAME)
  @Results({
          @Result(property = "name", column = "name"),
          @Result(property = "type", column = "type"),
          @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class)
  })
  List<Container> getContainers();

  @Select("select name, type, properties from " + TABLE_NAME + " where name = #{name}")
  @Results({
          @Result(property = "name", column = "name"),
          @Result(property = "type", column = "type"),
          @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class)
  })
  Container getContainer(@Param("name") String name);

  @Select("select type from " + TABLE_NAME + " where name = #{name}")
  String getType(String name);

  @Insert("replace into " + TABLE_NAME + " (name, type, properties) values (#{container.name}, #{container.type}, " +
          "#{container.properties, typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter})")
  void insertContainer(@Param("container") Container container);
}
