/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceConfigMapDao;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.db.XXServiceVersionInfoDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigMap;

import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.entity.XXServiceWithAssignedId;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.ranger.plugin.model.RangerService;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerServiceWithAssignedIdService {

        @InjectMocks
        RangerServiceWithAssignedIdService rangerServiceWithAssignedIdService;

        @Mock
        RangerDaoManager daoMgr;
        @Mock
        XXServiceConfigMapDao xXServiceConfigMapDao;

        @Mock
        org.apache.ranger.entity.XXServiceDef xXServiceDef;

        @Mock
        org.apache.ranger.db.XXServiceDefDao xXServiceDefDao;

        @Mock
        XXServiceDao xXServiceDao;

        @Mock
        XXService xXService;

        @Mock
        RangerService RangerService;

        @Mock
        org.apache.ranger.db.XXPortalUserDao xXPortalUserDao;

        @Mock
        XXServiceVersionInfoDao xXServiceVersionInfoDao;

        @Mock
        XXServiceVersionInfo xXServiceVersionInfo;

        @Test
        public void test1GetPopulatedViewObject() {
                XXServiceWithAssignedId xXServiceWithAssignedId = new XXServiceWithAssignedId();
                xXServiceWithAssignedId.setId(1L);
                Date date = new Date();
                xXServiceWithAssignedId.setIsEnabled(true);
                xXServiceWithAssignedId.setName("testService");
                xXServiceWithAssignedId.setPolicyVersion(1L);
                xXServiceWithAssignedId.setVersion(1L);
                xXServiceWithAssignedId.setCreateTime(date);
                xXServiceWithAssignedId.setTagService(1L);
                xXServiceWithAssignedId.setTagVersion(1L);
                xXServiceWithAssignedId.setUpdateTime(date);
                xXServiceWithAssignedId.setUpdatedByUserId(1L);
                xXServiceWithAssignedId.setAddedByUserId(1L);
                xXServiceWithAssignedId.setType(1L);
                List<XXServiceConfigMap> svcConfigMapList = new ArrayList<XXServiceConfigMap>();
                Mockito.when(daoMgr.getXXPortalUser()).thenReturn(xXPortalUserDao);
                Mockito.when(daoMgr.getXXServiceDef()).thenReturn(xXServiceDefDao);
                Mockito.when(xXServiceDefDao.getById(1L)).thenReturn(xXServiceDef);
                Mockito.when(daoMgr.getXXService()).thenReturn(xXServiceDao);
                Mockito.when(xXServiceDao.getById(1L)).thenReturn(xXService);
                Mockito.when(daoMgr.getXXServiceVersionInfo()).thenReturn(xXServiceVersionInfoDao);
                Mockito.when(xXServiceVersionInfoDao.findByServiceId(1L)).thenReturn(xXServiceVersionInfo);
                Mockito.when(daoMgr.getXXServiceConfigMap()).thenReturn(xXServiceConfigMapDao);
                Mockito.when(xXServiceConfigMapDao.findByServiceId(1L)).thenReturn(svcConfigMapList);
                rangerServiceWithAssignedIdService.getPopulatedViewObject(xXServiceWithAssignedId);
        }
}
