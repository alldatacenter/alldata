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

import org.apache.seatunnel.app.dal.entity.Datasource;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface IDatasourceDao {

    boolean insertDatasource(Datasource datasource);

    Datasource selectDatasourceById(Long id);

    boolean deleteDatasourceById(Long id);

    Datasource queryDatasourceByName(String name);

    boolean updateDatasourceById(Datasource datasource);

    boolean checkDatasourceNameUnique(String dataSourceName, Long dataSourceId);

    String queryDatasourceNameById(Long id);

    List<Datasource> selectDatasourceByPluginName(String pluginName, String pluginVersion);

    IPage<Datasource> selectDatasourcePage(Page<Datasource> page);

    IPage<Datasource> selectDatasourceByParam(
            Page<Datasource> page,
            List<Long> availableDatasourceIds,
            String searchVal,
            String pluginName);

    List<Datasource> selectDatasourceByIds(List<Long> ids);

    List<Datasource> queryAll();

    List<Datasource> selectByIds(List<Long> ids);

    List<Datasource> selectDatasourceByUserId(int userId);
}
