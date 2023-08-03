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

package org.apache.seatunnel.app.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_st_job_task")
public class JobTask {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("version_id")
    private Long versionId;

    @TableField("plugin_id")
    private String pluginId;

    @TableField("connector_type")
    private String connectorType;

    @TableField("datasource_id")
    private Long dataSourceId;

    /** { value */
    @TableField("datasource_option")
    private String dataSourceOption;

    /** { value */
    @TableField("select_table_fields")
    private String selectTableFields;

    @TableField("scene_mode")
    private String sceneMode;

    /** value */
    @TableField("transform_options")
    private String transformOptions;

    /** {@link java.util.List} value */
    @TableField("output_schema")
    private String outputSchema;

    @TableField private String config;

    /** {@link org.apache.seatunnel.common.constants.PluginType} values, lowercase */
    @TableField private String type;

    @TableField private String name;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
