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

package com.netease.arctic.ams.server.mapper.derby;

import com.netease.arctic.ams.server.mapper.ContainerMetadataMapper;
import com.netease.arctic.ams.server.model.Container;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface DerbyContainerMetadataMapper extends ContainerMetadataMapper {

  String TABLE_NAME = "container_metadata";

  @Insert("MERGE INTO " +
          TABLE_NAME  +
          " USING SYSIBM.SYSDUMMY1 " +
          "ON APP.container_metadata.name = #{container.name} " +
          "WHEN matched THEN update set type =#{container.type}, properties =#{container.properties, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter} " +
          "WHEN NOT MATCHED THEN INSERT (name,type,properties) VALUES (#{container.name}, #{container.type}, " +
          "#{container.properties, typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter})")
  void insertContainer(@Param("container") Container container);

}
