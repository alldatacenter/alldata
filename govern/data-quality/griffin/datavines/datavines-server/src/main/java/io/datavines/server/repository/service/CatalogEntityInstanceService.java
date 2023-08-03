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
import io.datavines.server.api.dto.bo.catalog.OptionItem;
import io.datavines.server.api.dto.bo.catalog.profile.RunProfileRequest;
import io.datavines.server.api.dto.bo.job.JobCreateWithEntityUuid;
import io.datavines.server.api.dto.vo.DataTime2ValueItem;
import io.datavines.server.api.dto.vo.JobExecutionVO;
import io.datavines.server.api.dto.vo.catalog.*;
import io.datavines.server.repository.entity.catalog.CatalogEntityInstance;

import java.util.List;

public interface CatalogEntityInstanceService extends IService<CatalogEntityInstance> {

    String create(CatalogEntityInstance entityInstance);

    CatalogEntityInstance getByTypeAndFQN(String type, String fqn);

    CatalogEntityInstance getByDataSourceAndFQN(Long dataSourceId, String fqn);

    IPage<CatalogEntityInstance> getEntityPage(String upstreamId, Integer pageNumber, Integer pageSize, String whetherMark);

    boolean updateStatus(String entityUUID, String status);

    List<OptionItem> getEntityList(String upstreamId);

    boolean deleteEntityByUUID(String entityUUID);

    boolean deleteEntityByDataSourceAndFQN(Long dataSourceId, String fqn);

    boolean softDeleteEntityByDataSourceAndFQN(Long dataSourceId, String fqn);

    List<CatalogColumnDetailVO> getCatalogColumnWithDetailList(String upstreamId);

    List<CatalogTableDetailVO> getCatalogTableWithDetailList(String upstreamId);

    IPage<CatalogColumnDetailVO> getCatalogColumnWithDetailPage(String upstreamId, String name, Integer pageNumber, Integer pageSize);

    IPage<CatalogTableDetailVO> getCatalogTableWithDetailPage(String upstreamId, String name, Integer pageNumber, Integer pageSize);

    CatalogDatabaseDetailVO getDatabaseEntityDetail(String uuid);

    CatalogTableDetailVO getTableEntityDetail(String uuid);

    CatalogColumnDetailVO getColumnEntityDetail(String uuid);

    CatalogTableProfileVO getTableEntityProfile(String uuid);

    Object getColumnEntityProfile(String uuid);

    long entityAddMetric(JobCreateWithEntityUuid jobCreateWithEntityUuid);

    CatalogEntityMetricParameter getEntityMetricParameter(String uuid);

    IPage<CatalogEntityMetricVO> getEntityMetricList(String uuid, Integer pageNumber, Integer pageSize);

    long executeDataProfileJob(RunProfileRequest runProfileRequest);

    long executeDataProfileJob(RunProfileRequest runProfileRequest, int runningNow);

    List<DataTime2ValueItem> listTableRecords(String uuid, String starTime, String endTime);

    IPage<CatalogEntityIssueVO> getEntityIssueList(String uuid, Integer pageNumber, Integer pageSize);

    List<String> getProfileJobSelectedColumns(String uuid);

    boolean deleteInstanceByDataSourceId(Long datasourceId);

    IPage<JobExecutionVO> profileJobExecutionPage(String uuid, Integer pageNumber, Integer pageSize);
}
