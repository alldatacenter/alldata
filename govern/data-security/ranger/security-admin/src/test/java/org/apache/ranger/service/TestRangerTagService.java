/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.service;

import org.apache.ranger.db.XXServiceResourceDao;
import org.apache.ranger.entity.XXServiceResource;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXServiceVersionInfoDao;
import org.apache.ranger.db.XXTagAttributeDao;
import org.apache.ranger.db.XXTagDao;
import org.apache.ranger.db.XXTagDefDao;
import org.apache.ranger.entity.XXTag;
import org.apache.ranger.entity.XXTagDef;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerTagService {

        @InjectMocks
        RangerTagService rangerTagService;

        @Mock
        XXTag xXTag;

        @Mock
        RangerDaoManager daoMgr;

        @Mock
        XXPortalUserDao xXPortalUserDao;

        @Mock
        XXServiceVersionInfoDao xXServiceVersionInfoDao;

        @Mock
        XXTagDefDao xXTagDefDao;

        @Mock
        XXTagDef xXTagDef;

        @Mock
        XXTagAttributeDao xXTagAttributeDao;

        @Mock
        XXTagDao xXTagDao;

        @Mock
    XXServiceResourceDao xxServiceResourceDao;

        @Mock
        XXServiceResource xxServiceResource;

        @Test
        public void test1postUpdate() {
                Mockito.when(daoMgr.getXXPortalUser()).thenReturn(xXPortalUserDao);
                Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
                Mockito.when(xXTagDefDao.getById(xXTag.getType())).thenReturn(xXTagDef);
                Mockito.when(daoMgr.getXXServiceVersionInfo()).thenReturn(xXServiceVersionInfoDao);
                rangerTagService.postUpdate(xXTag);

        }

        @Test
        public void test2GetPopulatedViewObject() {
                Mockito.when(daoMgr.getXXPortalUser()).thenReturn(xXPortalUserDao);
                Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
                XXTag xXTag = createXXTag();
                Mockito.when(xXTagDefDao.getById(1L)).thenReturn(xXTagDef);
                rangerTagService.getPopulatedViewObject(xXTag);

        }

        @Test
        public void test3GetTagByGuid() {
                Mockito.when(daoMgr.getXXTag()).thenReturn(xXTagDao);
                rangerTagService.getTagByGuid("1");

        }

        @Test
        public void test4GetTagsByType() {
                Mockito.when(daoMgr.getXXTag()).thenReturn(xXTagDao);
                rangerTagService.getTagsByType("testTagName");

        }

        @Test
        public void test5GetTagsForResourceId() {
                Mockito.when(daoMgr.getXXServiceResource()).thenReturn(xxServiceResourceDao);
                Mockito.when(xxServiceResourceDao.getById(1L)).thenReturn(xxServiceResource);

            rangerTagService.getTagsForResourceId(1L);

        }

        @Test
        public void test6GetTagsForResourceGuid() {
            Mockito.when(daoMgr.getXXServiceResource()).thenReturn(xxServiceResourceDao);
            Mockito.when(xxServiceResourceDao.findByGuid("1")).thenReturn(xxServiceResource);
                rangerTagService.getTagsForResourceGuid("1");

        }

        @Test
        public void test7getTagsByServiceId() {
                Mockito.when(daoMgr.getXXTag()).thenReturn(xXTagDao);
                rangerTagService.getTagsByServiceId(1L);
        }

        private XXTag createXXTag() {
                XXTag xXTag = new XXTag();
                xXTag.setAddedByUserId(1L);
                Date date = new Date();
                xXTag.setCreateTime(date);
                xXTag.setGuid("1");
                xXTag.setType(1L);
                xXTag.setUpdatedByUserId(1L);
                xXTag.setVersion(1L);
                xXTag.setId(1L);
                return xXTag;
        }
}
