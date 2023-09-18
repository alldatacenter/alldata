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
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dv_catalog_entity_instance")
public class CatalogEntityInstance implements Serializable {

    private static final long serialVersionUID = -1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "uuid")
    private String uuid;

    @TableField(value = "datasource_id")
    private Long datasourceId;

    @TableField(value = "type")
    private String type;

    @TableField(value = "fully_qualified_name")
    private String fullyQualifiedName;

    @TableField(value = "display_name")
    private String displayName;

    @TableField(value = "description")
    private String description;

    @TableField(value = "properties")
    private String properties;

    @TableField(value = "owner")
    private String owner;

    @TableField(value = "version")
    private String version;

    @TableField(value = "status")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
