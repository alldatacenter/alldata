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
package io.datavines.server.repository.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.datavines.server.api.dto.bo.datasource.ExecuteRequest;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.param.TestConnectionRequestParam;
import io.datavines.server.api.dto.bo.datasource.DataSourceCreate;
import io.datavines.server.api.dto.bo.datasource.DataSourceUpdate;
import io.datavines.server.api.dto.vo.DataSourceVO;
import io.datavines.server.repository.entity.DataSource;
import io.datavines.core.exception.DataVinesServerException;

import java.util.List;

public interface DataSourceService extends IService<DataSource> {

    boolean testConnect(TestConnectionRequestParam connectionParam);

    long insert(DataSourceCreate dataSource);

    int update(DataSourceUpdate dataSource) throws DataVinesException;

    DataSource getDataSourceById(long id);

    int delete(long id);

    List<DataSource> listByWorkSpaceId(long workspaceId);

    IPage<DataSourceVO> getDataSourcePage(String searchVal, Long workspaceId, Integer pageNumber, Integer pageSize);

    Object getDatabaseList(Long id) throws DataVinesServerException;

    Object getTableList(Long id, String database) throws DataVinesServerException;

    Object getColumnList(Long id, String database, String table) throws DataVinesServerException;

    Object executeScript(ExecuteRequest request) throws DataVinesServerException;

    String getConfigJson(String type);

    List<DataSource> listByWorkSpaceIdAndType(long workspaceId,String type);
}
