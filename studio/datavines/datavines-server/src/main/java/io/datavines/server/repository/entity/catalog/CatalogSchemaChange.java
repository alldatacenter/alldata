/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.repository.entity.catalog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.datavines.server.enums.SchemaChangeType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dv_catalog_schema_change")
public class CatalogSchemaChange implements Serializable {

    private static final long serialVersionUID = -1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "parent_uuid")
    private String parentUuid;

    @TableField(value = "entity_uuid")
    private String entityUuid;

    @TableField(value = "change_type")
    private SchemaChangeType changeType;

    @TableField(value = "database_name")
    private String databaseName;

    @TableField(value = "table_name")
    private String tableName;

    @TableField(value = "column_name")
    private String columnName;

    @TableField(value = "change_before")
    private String changeBefore;

    @TableField(value = "change_after")
    private String changeAfter;

    @TableField(value = "update_by")
    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
