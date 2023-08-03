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

import com.baomidou.mybatisplus.extension.service.IService;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.catalog.profile.CatalogProfileScheduleCreateOrUpdate;
import io.datavines.server.api.dto.bo.job.schedule.JobScheduleCreateOrUpdate;
import io.datavines.server.api.dto.vo.DataTime2ValueItem;
import io.datavines.server.repository.entity.JobSchedule;
import io.datavines.server.repository.entity.catalog.CatalogEntityProfile;

import java.util.List;

public interface CatalogEntityProfileService extends IService<CatalogEntityProfile> {

    List<CatalogEntityProfile> getEntityProfileByUUID(String uuid, String dataDate);

    List<CatalogEntityProfile> getEntityProfileByUUIDAndMetric(String uuid, String metricName, String dataDate);

    DataTime2ValueItem getCurrentTableRecords(String uuid);

    Double getColumnUniqueCount(String uuid, String dataDate);

    List<DataTime2ValueItem> listTableRecords(String uuid, String starTime, String endTime);

    JobSchedule createOrUpdate(CatalogProfileScheduleCreateOrUpdate createOrUpdate) throws DataVinesServerException;

    JobSchedule getById(long id);

    JobSchedule getByEntityUUID(String entityUUID);
}
