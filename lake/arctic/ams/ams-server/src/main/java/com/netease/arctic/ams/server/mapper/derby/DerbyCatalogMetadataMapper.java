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

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.server.mapper.CatalogMetadataMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface DerbyCatalogMetadataMapper extends CatalogMetadataMapper {
  String TABLE_NAME = "catalog_metadata";

  @Insert("MERGE INTO " +
          TABLE_NAME  +
          " USING SYSIBM.SYSDUMMY1 " +
          "ON " +
          TABLE_NAME +
          ".catalog_name = #{catalogMeta.catalogName}" +
          " WHEN matched THEN update set catalog_name =#{catalogMeta.catalogName}, " +
          "catalog_type=#{catalogMeta.catalogType}, storage_configs=" +
          "#{catalogMeta.storageConfigs, typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter}, " +
          "auth_configs=#{catalogMeta.authConfigs, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter}," +
          "catalog_properties=#{catalogMeta.catalogProperties, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter}" +
          "WHEN NOT MATCHED THEN INSERT (catalog_name, catalog_type, storage_configs, auth_configs, " +
          "catalog_properties) VALUES (#{catalogMeta.catalogName}, " +
          "#{catalogMeta.catalogType}, #{catalogMeta" +
          ".storageConfigs, typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter}, #{catalogMeta" +
          ".authConfigs, typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter}, #{catalogMeta" +
          ".catalogProperties, typeHandler=com.netease.arctic.ams.server.mybatis.Map2StringConverter})")
  void insertCatalog(@Param("catalogMeta") CatalogMeta catalogMeta);
}
