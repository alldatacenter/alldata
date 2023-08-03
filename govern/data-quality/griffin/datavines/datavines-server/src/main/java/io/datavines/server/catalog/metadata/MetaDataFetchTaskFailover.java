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
package io.datavines.server.catalog.metadata;

import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.NetUtils;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchTask;
import io.datavines.server.repository.service.CatalogMetaDataFetchTaskService;
import io.datavines.server.utils.SpringApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Slf4j
public class MetaDataFetchTaskFailover {

    private final CatalogMetaDataFetchTaskService metaDataFetchTaskService;

    private final CatalogMetaDataFetchTaskManager metaDataFetchTaskManager;

    public MetaDataFetchTaskFailover(CatalogMetaDataFetchTaskManager metaDataFetchTaskManager) {
        this.metaDataFetchTaskService = SpringApplicationContext.getBean(CatalogMetaDataFetchTaskService.class);
        this.metaDataFetchTaskManager = metaDataFetchTaskManager;
    }

    public void handleMetaDataFetchTaskFailover(String host) {
        List<CatalogMetaDataFetchTask> needFailoverTaskList = metaDataFetchTaskService.listNeedFailover(host);
        innerHandleMetaDataFetchTaskFailover(needFailoverTaskList);
    }

    private void innerHandleMetaDataFetchTaskFailover(List<CatalogMetaDataFetchTask> needFailover) {
        if (CollectionUtils.isNotEmpty(needFailover)) {
            needFailover.forEach(task -> {
                task.setExecuteHost(NetUtils.getAddr(
                        CommonPropertyUtils.getInt(CommonPropertyUtils.SERVER_PORT, CommonPropertyUtils.SERVER_PORT_DEFAULT)));
                metaDataFetchTaskService.updateById(task);

                try {
                    metaDataFetchTaskManager.putCatalogTask(task);
                } catch (Exception e) {
                    log.error("put the task need failover into manager error : ", e);
                }
            });
        }
    }

    public void handleMetaDataFetchTaskFailover(List<String> hostList) {
        List<CatalogMetaDataFetchTask> needFailoverTaskList = metaDataFetchTaskService.listTaskNotInServerList(hostList);
        innerHandleMetaDataFetchTaskFailover(needFailoverTaskList);
    }
}
