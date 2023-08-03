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

package org.apache.seatunnel.app.dal.dao;

import org.apache.seatunnel.app.dal.entity.VirtualTable;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface IVirtualTableDao {

    boolean insertVirtualTable(VirtualTable virtualTable);

    boolean updateVirtualTable(VirtualTable virtualTable);

    boolean deleteVirtualTable(Long id);

    VirtualTable selectVirtualTableById(Long id);

    VirtualTable selectVirtualTableByTableName(String tableName);

    boolean checkVirtualTableNameUnique(String virtualTableName, String databaseName, Long tableId);

    IPage<VirtualTable> selectVirtualTablePage(
            Page<VirtualTable> page, String pluginName, String datasourceName);

    IPage<VirtualTable> selectDatasourceByParam(Page<VirtualTable> page, Long datasourceId);

    List<String> getVirtualTableNames(String databaseName, Long datasourceId);

    List<String> getVirtualDatabaseNames(Long datasourceId);

    boolean checkHasVirtualTable(Long datasourceId);
}
