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

import com.netease.arctic.ams.server.mapper.TableMetadataMapper;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.mybatis.Map2StringConverter;
import com.netease.arctic.table.TableIdentifier;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface DerbyTableMetadataMapper extends TableMetadataMapper {
  String TABLE_NAME = "table_metadata";

  @Select("select table_name, db_name, catalog_name, primary_key, " +
      "table_location, base_location, delta_location, meta_store_site, hdfs_site, core_site, " +
      "auth_method, hadoop_username, krb_keytab, krb_conf, krb_principal, properties, current_tx_id from " +
      TABLE_NAME +
      " where table_name = #{tableIdentifier.tableName} and db_name = #{tableIdentifier.database} " +
      "and catalog_name = #{tableIdentifier.catalog}")
  @Results({
      @Result(property = "tableIdentifier.tableName", column = "table_name"),
      @Result(property = "tableIdentifier.database", column = "db_name"),
      @Result(property = "tableIdentifier.catalog", column = "catalog_name"),
      @Result(property = "primaryKey", column = "primary_key"),
      @Result(property = "tableLocation", column = "table_location"),
      @Result(property = "baseLocation", column = "base_location"),
      @Result(property = "changeLocation", column = "delta_location"),
      @Result(property = "metaStoreSite", column = "meta_store_site"),
      @Result(property = "hdfsSite", column = "hdfs_site"),
      @Result(property = "coreSite", column = "core_site"),
      @Result(property = "authMethod", column = "auth_method"),
      @Result(property = "hadoopUsername", column = "hadoop_username"),
      @Result(property = "krbKeyteb", column = "krb_keytab"),
      @Result(property = "krbConf", column = "krb_conf"),
      @Result(property = "krbPrincipal", column = "krb_principal"),
      @Result(property = "properties", column = "properties",
          typeHandler = Map2StringConverter.class),
      @Result(property = "currentTxId", column = "current_tx_id")
  })
  TableMetadata loadTableMetaInLock(@Param("tableIdentifier") TableIdentifier tableIdentifier);
}
