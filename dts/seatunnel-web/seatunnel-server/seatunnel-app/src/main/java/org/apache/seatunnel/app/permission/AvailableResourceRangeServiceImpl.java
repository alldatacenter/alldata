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

package org.apache.seatunnel.app.permission;

import org.apache.seatunnel.app.dal.dao.IDatasourceDao;
import org.apache.seatunnel.app.dal.entity.Datasource;
import org.apache.seatunnel.app.permission.enums.SeatunnelResourcePermissionModuleEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AvailableResourceRangeServiceImpl
        implements AvailableResourceRangeService, ApplicationContextAware {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AvailableResourceRangeServiceImpl.class);

    private final Map<String, ResourcePermissionQuery> resourceQueryMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, ResourcePermissionQuery> beansOfType =
                applicationContext.getBeansOfType(ResourcePermissionQuery.class);
        beansOfType.forEach(
                (key, value) -> {
                    List typeList = value.accessTypes();
                    if (typeList == null || typeList.isEmpty()) {
                        return;
                    }
                    typeList.forEach(
                            accessType -> resourceQueryMap.put(String.valueOf(accessType), value));
                });
    }

    @Override
    public List queryAvailableResourceRangeBySourceType(String resourceType, int userId) {
        ResourcePermissionQuery resourcePermissionQuery = resourceQueryMap.get(resourceType);
        if (resourcePermissionQuery == null) {
            LOGGER.warn("resource type {} query handle not init", resourceType);
            return Collections.emptyList();
        }
        return resourcePermissionQuery.queryByResourceType(userId);
    }

    @Component
    public static class DataSourceQuery implements ResourcePermissionQuery<Long> {

        @Autowired
        @Qualifier("datasourceDaoImpl") private IDatasourceDao iDatasourceDao;

        @Override
        public List<String> accessTypes() {
            return Collections.singletonList(
                    SeatunnelResourcePermissionModuleEnum.DATASOURCE.name());
        }

        @Override
        public List<Long> queryByResourceType(int userId) {
            List<Datasource> datasourceList = iDatasourceDao.selectDatasourceByUserId(userId);
            return datasourceList == null || datasourceList.isEmpty()
                    ? Collections.emptyList()
                    : datasourceList.stream().map(Datasource::getId).collect(Collectors.toList());
        }
    }

    interface ResourcePermissionQuery<T> {

        /** resource type */
        List<String> accessTypes();

        /** query by resource type */
        List<T> queryByResourceType(int userId);
    }
}
