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

package org.apache.seatunnel.app.dal.dao.impl;

import org.apache.seatunnel.app.dal.dao.IVirtualTableDao;
import org.apache.seatunnel.app.dal.entity.VirtualTable;
import org.apache.seatunnel.app.dal.mapper.VirtualTableMapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class VirtualTableDaoImpl implements IVirtualTableDao {

    @Resource private VirtualTableMapper virtualTableMapper;

    @Override
    public boolean insertVirtualTable(VirtualTable virtualTable) {
        return virtualTableMapper.insert(virtualTable) > 0;
    }

    @Override
    public boolean updateVirtualTable(VirtualTable virtualTable) {
        return virtualTableMapper.updateById(virtualTable) > 0;
    }

    @Override
    public boolean deleteVirtualTable(Long id) {
        return virtualTableMapper.deleteById(id) > 0;
    }

    @Override
    public VirtualTable selectVirtualTableById(Long id) {
        return virtualTableMapper.selectById(id);
    }

    @Override
    public VirtualTable selectVirtualTableByTableName(String tableName) {
        return virtualTableMapper.selectOne(
                new QueryWrapper<VirtualTable>().eq("virtual_table_name", tableName));
    }

    @Override
    public boolean checkVirtualTableNameUnique(
            String virtualTableName, String databaseName, Long tableId) {
        return virtualTableMapper.checkVirtualTableNameUnique(
                        tableId, databaseName, virtualTableName)
                <= 0;
    }

    @Override
    public IPage<VirtualTable> selectVirtualTablePage(
            Page<VirtualTable> page, String pluginName, String datasourceName) {
        log.debug(
                "======================pluginName:{}, datasourceName:{}",
                pluginName,
                datasourceName);
        if (StringUtils.isBlank(pluginName) && StringUtils.isBlank(datasourceName)) {
            return virtualTableMapper.selectPage(
                    page, new QueryWrapper<VirtualTable>().orderByDesc("create_time"));
        }
        return virtualTableMapper.selectVirtualTablePageByParam(page, pluginName, datasourceName);
    }

    @Override
    public IPage<VirtualTable> selectDatasourceByParam(Page<VirtualTable> page, Long datasourceId) {
        return virtualTableMapper.selectPage(
                page,
                new QueryWrapper<VirtualTable>()
                        .eq("datasource_id", datasourceId)
                        .orderByDesc("create_time"));
    }

    @Override
    public List<String> getVirtualTableNames(String databaseName, Long datasourceId) {

        List<VirtualTable> result =
                virtualTableMapper.selectList(
                        new QueryWrapper<VirtualTable>()
                                .select("virtual_table_name")
                                .eq("datasource_id", datasourceId)
                                .eq("virtual_database_name", databaseName));
        if (CollectionUtils.isEmpty(result)) {
            return new ArrayList<>();
        }
        return result.stream().map(VirtualTable::getVirtualTableName).collect(Collectors.toList());
    }

    @Override
    public List<String> getVirtualDatabaseNames(Long datasourceId) {
        List<VirtualTable> result =
                virtualTableMapper.selectList(
                        new QueryWrapper<VirtualTable>()
                                .select("virtual_database_name")
                                .eq("datasource_id", datasourceId));
        if (CollectionUtils.isEmpty(result)) {
            return new ArrayList<>();
        }
        return result.stream()
                .map(VirtualTable::getVirtualDatabaseName)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkHasVirtualTable(Long datasourceId) {
        return virtualTableMapper.selectCount(
                        new QueryWrapper<VirtualTable>().eq("datasource_id", datasourceId))
                > 0;
    }
}
