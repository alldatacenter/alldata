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
package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.NetUtils;
import io.datavines.server.api.dto.bo.catalog.CatalogRefresh;
import io.datavines.server.registry.RegistryHolder;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchCommand;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchTask;
import io.datavines.server.repository.mapper.CatalogMetaDataFetchTaskMapper;
import io.datavines.server.repository.service.CatalogMetaDataFetchCommandService;
import io.datavines.server.repository.service.CatalogMetaDataFetchTaskService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service("catalogMetaDataFetcherTaskService")
public class CatalogMetaDataFetchTaskServiceImpl
        extends ServiceImpl<CatalogMetaDataFetchTaskMapper, CatalogMetaDataFetchTask>
        implements CatalogMetaDataFetchTaskService {

    @Autowired
    private CatalogMetaDataFetchCommandService catalogMetaDataFetchCommandService;

    @Autowired
    private RegistryHolder registryHolder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long refreshCatalog(CatalogRefresh catalogRefresh) {

        Long taskId = 0L;
        registryHolder.blockUtilAcquireLock("1028");
        List<CatalogMetaDataFetchTask> oldTaskList =
                baseMapper.selectList(new QueryWrapper<CatalogMetaDataFetchTask>()
                        .eq("status",0)
                        .eq("datasource_id", catalogRefresh.getDatasourceId())
                        .eq("parameter", JSONUtils.toJsonString(catalogRefresh)));
        if (CollectionUtils.isNotEmpty(oldTaskList)) {
            registryHolder.release("1028");
            return 0L;
        }
        //生成任务之前需要检查是否有相同的任务在执行
        LocalDateTime now = LocalDateTime.now();
        CatalogMetaDataFetchTask catalogMetaDataFetchTask = new CatalogMetaDataFetchTask();
        catalogMetaDataFetchTask.setParameter(JSONUtils.toJsonString(catalogRefresh));
        catalogMetaDataFetchTask.setDataSourceId(catalogRefresh.getDatasourceId());
        catalogMetaDataFetchTask.setStatus(0);
        catalogMetaDataFetchTask.setExecuteHost(NetUtils.getAddr(
                CommonPropertyUtils.getInt(CommonPropertyUtils.SERVER_PORT, CommonPropertyUtils.SERVER_PORT_DEFAULT)));
        catalogMetaDataFetchTask.setSubmitTime(now);
        catalogMetaDataFetchTask.setCreateTime(now);
        catalogMetaDataFetchTask.setUpdateTime(now);

        baseMapper.insert(catalogMetaDataFetchTask);

        CatalogMetaDataFetchCommand catalogMetaDataFetchCommand = new CatalogMetaDataFetchCommand();
        catalogMetaDataFetchCommand.setTaskId(catalogMetaDataFetchTask.getId());
        catalogMetaDataFetchCommand.setCreateTime(now);
        catalogMetaDataFetchCommand.setUpdateTime(now);
        catalogMetaDataFetchCommandService.create(catalogMetaDataFetchCommand);
        taskId = catalogMetaDataFetchTask.getId();
        registryHolder.release("1028");

        return taskId;
    }

    @Override
    public int update(CatalogMetaDataFetchTask catalogMetaDataFetchTask) {
        return baseMapper.updateById(catalogMetaDataFetchTask);
    }

    @Override
    public CatalogMetaDataFetchTask getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Long killCatalogTask(Long catalogTaskId) {
        return null;
    }

    @Override
    public List<CatalogMetaDataFetchTask> listNeedFailover(String host) {
        return baseMapper.selectList(new QueryWrapper<CatalogMetaDataFetchTask>()
                .eq("execute_host", host)
                .in("status", ExecutionStatus.RUNNING_EXECUTION.getCode(), ExecutionStatus.SUBMITTED_SUCCESS.getCode()));
    }

    @Override
    public List<CatalogMetaDataFetchTask> listTaskNotInServerList(List<String> hostList) {
        return baseMapper.selectList(new QueryWrapper<CatalogMetaDataFetchTask>()
                .notIn("execute_host", hostList)
                .in("status",ExecutionStatus.RUNNING_EXECUTION.getCode(), ExecutionStatus.SUBMITTED_SUCCESS.getCode()));
    }

    @Override
    public String getTaskExecuteHost(Long catalogTaskId) {
        return null;
    }
}
