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

import com.netease.arctic.ams.server.model.OptimizerGroup;
import com.netease.arctic.ams.server.model.OptimizerGroupInfo;
import com.netease.arctic.ams.server.mybatis.Map2StringConverter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * optimizer group mapper
 */
@Mapper
public interface OptimizerGroupMapper {
  String TABLE_NAME = "optimize_group";

  @Select("select group_id as optimizerGroupId, name as optimizerGroupName from " + TABLE_NAME)
  List<OptimizerGroup> selectOptimzerGroups();

  @Select("select group_id as id, name, properties, container from " + TABLE_NAME +
          " where name = #{groupName}")
  @Results({
          @Result(property = "id", column = "id"),
          @Result(property = "name", column = "name"),
          @Result(property = "container", column = "container"),
          @Result(property = "properties", column = "properties",
                  typeHandler = Map2StringConverter.class)
  })
  OptimizerGroupInfo selectOptimizerGroupInfo(String groupName);

  @Select("select group_id as id, name, properties, container from " + TABLE_NAME)
  @Results({
          @Result(property = "id", column = "id"),
          @Result(property = "name", column = "name"),
          @Result(property = "container", column = "container"),
          @Result(property = "properties", column = "properties",
                  typeHandler = Map2StringConverter.class)
  })
  List<OptimizerGroupInfo> selectAllOptimizerGroupInfo();

}
