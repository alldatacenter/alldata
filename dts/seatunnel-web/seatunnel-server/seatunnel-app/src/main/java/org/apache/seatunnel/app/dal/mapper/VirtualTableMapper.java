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

package org.apache.seatunnel.app.dal.mapper;

import org.apache.seatunnel.app.dal.entity.VirtualTable;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface VirtualTableMapper extends BaseMapper<VirtualTable> {

    IPage<VirtualTable> selectPage(IPage<VirtualTable> page);

    IPage<VirtualTable> selectVirtualTablePageByParam(
            IPage<VirtualTable> page,
            @Param("pluginName") String pluginName,
            @Param("datasourceName") String datasourceName);

    int checkVirtualTableNameUnique(
            @Param("tableId") Long tableId,
            @Param("virtualDatabaseName") String databaseName,
            @Param("virtualTableName") String virtualTableName);
}
