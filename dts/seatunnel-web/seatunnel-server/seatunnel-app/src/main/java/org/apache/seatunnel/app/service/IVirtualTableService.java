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

package org.apache.seatunnel.app.service;

import org.apache.seatunnel.app.domain.request.datasource.VirtualTableReq;
import org.apache.seatunnel.app.domain.response.PageInfo;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableRes;
import org.apache.seatunnel.server.common.CodeGenerateUtils;

import java.util.List;

public interface IVirtualTableService {

    String createVirtualTable(Integer userId, VirtualTableReq req)
            throws CodeGenerateUtils.CodeGenerateException;

    Boolean updateVirtualTable(Integer userId, String tableId, VirtualTableReq req);

    Boolean deleteVirtualTable(Integer userId, String tableId);

    Boolean checkVirtualTableValid(VirtualTableReq req);

    VirtualTableDetailRes queryVirtualTable(String tableId);

    VirtualTableDetailRes queryVirtualTableByTableName(String tableName);

    boolean containsVirtualTableByTableName(String tableName);

    String queryTableDynamicTable(String pluginName);

    PageInfo<VirtualTableRes> getVirtualTableList(
            String pluginName, String datasourceName, Integer pageNo, Integer pageSize);

    List<String> getVirtualTableNames(String databaseName, String datasourceId);

    List<String> getVirtualDatabaseNames(String datasourceId);
}
