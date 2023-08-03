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

package org.apache.ranger.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.apache.ranger.view.VXAsset;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXPermObj;
import org.apache.ranger.view.VXPolicy;
import org.apache.ranger.view.VXRepository;
import org.apache.ranger.view.VXResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestServiceUtil {

        @InjectMocks
        ServiceUtil serviceUtil = new ServiceUtil();

        @Mock
        ServiceDBStore svcStore;

        @Mock
        JSONUtil jsonUtil;

        @Mock
        RangerDaoManager xaDaoMgr;

        @Mock
        XXUserDao xxUserDao;

        @Mock
        XXGroupDao xxGroupDao;

        @Test
        public void testGetServiceByName() throws Exception{
                RangerService expectedRangerService = new RangerService();
                expectedRangerService.setId(1L);
                expectedRangerService.setName("hdfs");
                Mockito.when(svcStore.getServiceByName("hdfs")).thenReturn(expectedRangerService);
                RangerService actualRangerService = serviceUtil.getServiceByName("hdfs");

                Assert.assertEquals(expectedRangerService.getName(), actualRangerService.getName());
                Assert.assertEquals(expectedRangerService.getId(), actualRangerService.getId());
        }

        @Test
        public void testToRangerServiceForNull(){
                VXAsset vXAsset = null;
                RangerService actualRangerService = serviceUtil.toRangerService(vXAsset);
                Assert.assertNull(actualRangerService);

        }
        @Test
        public void testToRangerService(){
                Map<String, String> map = new HashMap<String, String>();
                RangerService expectedRangerService = new RangerService();
                expectedRangerService.setId(1L);
                expectedRangerService.setName("hive");
                expectedRangerService.setDescription("hive Description");
                map.put("config", "hiveConfig");
                VXAsset vXAsset = new VXAsset();
                vXAsset.setId(1L);
                vXAsset.setCreateDate(new Date());
                vXAsset.setUpdateDate(new Date());
                vXAsset.setOwner("ranger");
                vXAsset.setUpdatedBy("rangerAdmin");
                vXAsset.setAssetType(5);
                vXAsset.setName("hive");
                vXAsset.setDescription("hive Description");
                vXAsset.setActiveStatus(1);
                vXAsset.setConfig("{config : hiveConfig}");
                Mockito.when(jsonUtil.jsonToMap("{config : hiveConfig}")).thenReturn(map);

                RangerService actualRangerService = serviceUtil.toRangerService(vXAsset);

                Assert.assertNotNull(actualRangerService);
                Assert.assertEquals(actualRangerService.getId(), expectedRangerService.getId());
                Assert.assertEquals(actualRangerService.getName(), expectedRangerService.getName());
                Assert.assertEquals(actualRangerService.getDescription(), expectedRangerService.getDescription());
                Assert.assertTrue(actualRangerService.getIsEnabled());


        }
        @Test
        public void testToVXAssetForNull(){
                RangerService rangerService = null;
                VXAsset actualVXAsset = serviceUtil.toVXAsset(rangerService);
                Assert.assertNull(actualVXAsset);

        }

        @Test
        public void testToVXAsset(){
                RangerService rangerService = new RangerService();
                VXAsset expectedVXAssesst = new VXAsset();
                expectedVXAssesst.setId(1L);
                expectedVXAssesst.setName("hive");
                expectedVXAssesst.setDescription("hive Description");
                expectedVXAssesst.setActiveStatus(1);

                Map<String, String> map = new HashMap<String, String>();
                map.put("config", "hiveConfig");
                rangerService.setId(1L);
                rangerService.setCreateTime(new Date());
                rangerService.setUpdateTime(new Date());
                rangerService.setCreatedBy("ranger");
                rangerService.setUpdatedBy("rangerAdmin");

                rangerService.setType("hive");
                rangerService.setName("hive");
                rangerService.setDescription("hive Description");
                rangerService.setIsEnabled(true);
                rangerService.setConfigs(map);

                Mockito.when(jsonUtil.readMapToString(map)).thenReturn("{config : hiveConfig}");

                VXAsset actualVXAsset = serviceUtil.toVXAsset(rangerService);

                Assert.assertNotNull(actualVXAsset);
                Assert.assertEquals(actualVXAsset.getId(), expectedVXAssesst.getId());
                Assert.assertEquals(actualVXAsset.getName(), expectedVXAssesst.getName());
                Assert.assertEquals(actualVXAsset.getDescription(), expectedVXAssesst.getDescription());
                Assert.assertEquals(actualVXAsset.getActiveStatus(), RangerCommonEnums.STATUS_ENABLED);

        }


        @Test
        public void testToVXRepositoryForNull(){
                RangerService rangerService = null;
                VXRepository actualvXRepository = serviceUtil.toVXRepository(rangerService);
                Assert.assertNull(actualvXRepository);

        }

        @Test
        public void testToVXRepository(){
                Map<String, String> map = new HashMap<String, String>();
                map.put("config", "hiveConfig");
                VXRepository expectedVXRepository = new VXRepository();
                expectedVXRepository.setRepositoryType("hive");
                expectedVXRepository.setName("hive");
                expectedVXRepository.setDescription("hive Description");
                expectedVXRepository.setIsActive(true);
                expectedVXRepository.setVersion("3");

                RangerService rangerService = new RangerService();
                rangerService.setId(1L);
                rangerService.setCreateTime(new Date());
                rangerService.setUpdateTime(new Date());
                rangerService.setCreatedBy("ranger");
                rangerService.setUpdatedBy("rangerAdmin");
                rangerService.setType("hive");
                rangerService.setName("hive");
                rangerService.setDescription("hive Description");
                rangerService.setIsEnabled(true);
                rangerService.setConfigs(map);
                rangerService.setVersion(3L);

                Mockito.when(jsonUtil.readMapToString(map)).thenReturn("{config : hiveConfig}");

                VXRepository actualvXRepository = serviceUtil.toVXRepository(rangerService);
                Assert.assertNotNull(actualvXRepository);
                Assert.assertEquals(actualvXRepository.getRepositoryType(), expectedVXRepository.getRepositoryType());
                Assert.assertEquals(actualvXRepository.getName(), expectedVXRepository.getName());
                Assert.assertEquals(actualvXRepository.getDescription(), expectedVXRepository.getDescription());
                Assert.assertTrue(actualvXRepository.getIsActive());
                Assert.assertEquals(actualvXRepository.getVersion(), expectedVXRepository.getVersion());

        }

        @Test
        public void testToRangerPolicyForNull(){
                VXResource resource = null;
                RangerService rangerService = null;
                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);
                Assert.assertNull(actualRangerPolicy);
        }

        @Test
        public void testToRangerPolicyForResourceTypePath(){
                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("resource");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(true);
                rangerPolicyResource.setValue("/localhost/files");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("path", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setName("resource");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeTable(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("xa_service");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(true);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("xa_service");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("table", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setTables("xa_service");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());


        }

        @Test
        public void testToRangerPolicyForResourceTypeColumnFamily(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("columnFamilies");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("columnFamilies");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("column-family", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setColumnFamilies("columnFamilies");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeColumn(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("column");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(true);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("column");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("column", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setColumns("column");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeDatabase(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("databases");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("databases");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("database", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setDatabases("databases");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeUDF(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("udf");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("databases");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("udf", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setUdfs("udf");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeTopology(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("topology");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("topology");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("topology", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setTopologies("topology");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeService(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("service");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("service");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("service", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setServices("service");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForResourceTypeHiveService(){

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");

                Map<String, RangerPolicyResource> expectedMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("hiveservice");


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(false);
                rangerPolicyResource.setValue("hiveservice");
                rangerPolicyResource.setValues(valuesList);

                expectedMap.put("service", rangerPolicyResource);

                expectedRangerPolicy.setResources(expectedMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setServices("hiveservice");
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());

        }

        @Test
        public void testToRangerPolicyForPermGroup(){

                RangerPolicyItemCondition rpic = new RangerPolicyItemCondition();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("10.129.25.56");
                rpic.setType("ipaddress");
                rpic.setValues(valuesList);

                List<String> usersList = new ArrayList<String>();
                usersList.add("rangerAdmin");

                List<String> groupList = new ArrayList<String>();

                List<RangerPolicyItemCondition> listRPIC = new ArrayList<RangerPolicy.RangerPolicyItemCondition>();
                listRPIC.add(rpic);

                RangerPolicyItemAccess rpia = new RangerPolicyItemAccess();
                rpia.setIsAllowed(true);
                rpia.setType("drop");

                List<RangerPolicyItemAccess> listRPIA = new ArrayList<RangerPolicy.RangerPolicyItemAccess>();
                listRPIA.add(rpia);

                RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
                rangerPolicyItem.setConditions(listRPIC);
                rangerPolicyItem.setAccesses(listRPIA);
                rangerPolicyItem.setDelegateAdmin(false);
                rangerPolicyItem.setUsers(usersList);
                rangerPolicyItem.setGroups(groupList);

                List<RangerPolicyItem> listRangerPolicyItem = new ArrayList<RangerPolicy.RangerPolicyItem>();
                listRangerPolicyItem.add(rangerPolicyItem);

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hive Policy");
                expectedRangerPolicy.setService("hive");
                expectedRangerPolicy.setDescription("hive policy description");
                expectedRangerPolicy.setPolicyItems(listRangerPolicyItem);

                VXPermMap vXPermMap = new VXPermMap();
                vXPermMap.setId(5L);
                vXPermMap.setGroupName("myGroup");
                vXPermMap.setPermGroup("permGroup");
                vXPermMap.setUserName("rangerAdmin");
                vXPermMap.setPermType(12);
                vXPermMap.setPermFor(AppConstants.XA_PERM_FOR_USER);
                vXPermMap.setIpAddress("10.129.25.56");

                List<VXPermMap> vXPermMapList = new ArrayList<VXPermMap>();
                vXPermMapList.add(vXPermMap);


                VXAuditMap vXAuditMap = new VXAuditMap();
                vXAuditMap.setId(1L);
                vXAuditMap.setOwner("rangerAdmin");
                List<VXAuditMap> vXAuditMapList = new ArrayList<VXAuditMap>();
                vXAuditMapList.add(vXAuditMap);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");
                rangerService.setType("hive");


                VXResource resource = new VXResource();
                resource.setId(1L);
                resource.setUpdateDate(new Date());
                resource.setCreateDate(new Date());
                resource.setOwner("rangerAdmin");
                resource.setUpdatedBy("rangerAdmin");
                resource.setPolicyName("hive Policy");
                resource.setDescription("hive policy description");
                resource.setResourceStatus(RangerCommonEnums.STATUS_ENABLED);
                resource.setIsRecursive(1);
                resource.setTableType(1);
                resource.setColumnType(1);
                resource.setPermMapList(vXPermMapList);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(resource, rangerService);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getService(), actualRangerPolicy.getService());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getPolicyItems(), actualRangerPolicy.getPolicyItems());

        }

        @Test
        public void testToVXResourceForPolicyNull(){
                RangerPolicy policy = null;
                RangerService rangerService = new RangerService();
                rangerService.setName("hive");
                rangerService.setType("hive");

                VXResource vXResource = serviceUtil.toVXResource(policy, rangerService);

                Assert.assertNull(vXResource);

        }

        @Test
        public void testToVXResourceForServiceNull(){
                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("hive Policy");
                policy.setService("hive");
                policy.setDescription("hive policy description");

                RangerService rangerService = null;

                VXResource vXResource = serviceUtil.toVXResource(policy, rangerService);

                Assert.assertNull(vXResource);

        }

        @Test
        public void testToVXResourceForPath(){
                GUIDUtil guid = new GUIDUtil();
                String guidString = guid.genGUID();
                List<VXAuditMap> auditList = new ArrayList<VXAuditMap>();

                VXAuditMap vxAuditMap = new VXAuditMap();
                vxAuditMap.setResourceId(1L);
                vxAuditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);
                auditList.add(vxAuditMap);

                VXResource expectedVXResource = new VXResource();
                expectedVXResource.setName("resource");
                expectedVXResource.setGuid(guidString);
                expectedVXResource.setPolicyName("hdfs Policy");
                expectedVXResource.setDescription("hdfs policy description");
                expectedVXResource.setResourceType(1);
                expectedVXResource.setAssetName("hdfs");
                expectedVXResource.setAssetType(1);
                expectedVXResource.setAuditList(auditList);

                Map<String, RangerPolicyResource> rangerPolicyResourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("resource");

                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("hdfs Policy");
                policy.setService("hdfs");
                policy.setDescription("hdfs policy description");
                policy.setIsEnabled(true);
                policy.setGuid(guidString);
                policy.setIsAuditEnabled(true);

                RangerService rangerService = new RangerService();
                rangerService.setName("hdfs");
                rangerService.setType("hdfs");

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(true);
                rangerPolicyResource.setValue("/localhost/files");
                rangerPolicyResource.setValues(valuesList);

                rangerPolicyResourceMap.put("path", rangerPolicyResource);


                policy.setResources(rangerPolicyResourceMap);

                VXResource actualVXResource = serviceUtil.toVXResource(policy, rangerService);


                Assert.assertNotNull(actualVXResource);
                Assert.assertEquals(expectedVXResource.getName(), actualVXResource.getName());
                Assert.assertEquals(expectedVXResource.getGuid(), actualVXResource.getGuid());
                Assert.assertEquals(expectedVXResource.getPolicyName(), actualVXResource.getPolicyName());
                Assert.assertEquals(expectedVXResource.getResourceType(), actualVXResource.getResourceType());
                Assert.assertEquals(expectedVXResource.getDescription(), actualVXResource.getDescription());
                Assert.assertEquals(expectedVXResource.getAssetName(), actualVXResource.getAssetName());
                Assert.assertEquals(expectedVXResource.getAssetType(), actualVXResource.getAssetType());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getResourceId(), actualVXResource.getAuditList().get(0).getResourceId());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getAuditType(), actualVXResource.getAuditList().get(0).getAuditType());


        }

        @Test
        public void testToVXResourceForTablesColumnFamiliesAndColumn(){
                GUIDUtil guid = new GUIDUtil();
                String guidString = guid.genGUID();
                List<VXAuditMap> auditList = new ArrayList<VXAuditMap>();

                VXAuditMap vxAuditMap = new VXAuditMap();
                vxAuditMap.setResourceId(1L);
                vxAuditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);
                auditList.add(vxAuditMap);

                VXResource expectedVXResource = new VXResource();
                expectedVXResource.setName("/myTable/myColumnFamilies/myColumn");
                expectedVXResource.setTables("myTable");
                expectedVXResource.setColumnFamilies("myColumnFamilies");
                expectedVXResource.setColumns("myColumn");
                expectedVXResource.setGuid(guidString);
                expectedVXResource.setPolicyName("hbase Policy");
                expectedVXResource.setDescription("hbase policy description");
                expectedVXResource.setResourceType(1);
                expectedVXResource.setAssetName("hbase");
                expectedVXResource.setAssetType(2);
                expectedVXResource.setResourceStatus(1);
                expectedVXResource.setTableType(1);
                expectedVXResource.setColumnType(1);
                expectedVXResource.setAuditList(auditList);

                Map<String, RangerPolicyResource> rangerPolicyResourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesListForTable = new ArrayList<String>();
                valuesListForTable.add("myTable");

                List<String> valuesListForColumn = new ArrayList<String>();
                valuesListForColumn.add("myColumn");

                List<String> valuesListForColumnFamilies = new ArrayList<String>();
                valuesListForColumnFamilies.add("myColumnFamilies");

                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("hbase Policy");
                policy.setService("hbase");
                policy.setDescription("hbase policy description");
                policy.setIsEnabled(true);
                policy.setGuid(guidString);
                policy.setIsAuditEnabled(true);

                RangerService rangerService = new RangerService();
                rangerService.setName("hbase");
                rangerService.setType("hbase");

                RangerPolicyResource rangerPolicyResourceForTable = new RangerPolicyResource();
                rangerPolicyResourceForTable.setIsExcludes(true);
                rangerPolicyResourceForTable.setIsRecursive(true);
                rangerPolicyResourceForTable.setValue("table");
                rangerPolicyResourceForTable.setValues(valuesListForTable);

                rangerPolicyResourceMap.put("table", rangerPolicyResourceForTable);

                RangerPolicyResource rangerPolicyResourceForColumn = new RangerPolicyResource();
                rangerPolicyResourceForColumn.setIsExcludes(true);
                rangerPolicyResourceForColumn.setIsRecursive(true);
                rangerPolicyResourceForColumn.setValue("table");
                rangerPolicyResourceForColumn.setValues(valuesListForColumn);

                rangerPolicyResourceMap.put("column", rangerPolicyResourceForColumn);

                RangerPolicyResource rangerPolicyResourceForColumnFamilies = new RangerPolicyResource();
                rangerPolicyResourceForColumnFamilies.setIsExcludes(true);
                rangerPolicyResourceForColumnFamilies.setIsRecursive(true);
                rangerPolicyResourceForColumnFamilies.setValue("table");
                rangerPolicyResourceForColumnFamilies.setValues(valuesListForColumnFamilies);

                rangerPolicyResourceMap.put("column-family", rangerPolicyResourceForColumnFamilies);


                policy.setResources(rangerPolicyResourceMap);

                VXResource actualVXResource = serviceUtil.toVXResource(policy, rangerService);


                Assert.assertNotNull(actualVXResource);
                Assert.assertEquals(expectedVXResource.getName(), actualVXResource.getName());
                Assert.assertEquals(expectedVXResource.getGuid(), actualVXResource.getGuid());
                Assert.assertEquals(expectedVXResource.getPolicyName(), actualVXResource.getPolicyName());
                Assert.assertEquals(expectedVXResource.getResourceType(), actualVXResource.getResourceType());
                Assert.assertEquals(expectedVXResource.getDescription(), actualVXResource.getDescription());
                Assert.assertEquals(expectedVXResource.getAssetName(), actualVXResource.getAssetName());
                Assert.assertEquals(expectedVXResource.getAssetType(), actualVXResource.getAssetType());
                Assert.assertEquals(expectedVXResource.getResourceStatus(), actualVXResource.getResourceStatus());
                Assert.assertEquals(expectedVXResource.getTableType(), actualVXResource.getTableType());
                Assert.assertEquals(expectedVXResource.getColumnType(), actualVXResource.getColumnType());
                Assert.assertEquals(expectedVXResource.getTables(), actualVXResource.getTables());
                Assert.assertEquals(expectedVXResource.getColumns(), actualVXResource.getColumns());
                Assert.assertEquals(expectedVXResource.getColumnFamilies(), actualVXResource.getColumnFamilies());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getResourceId(), actualVXResource.getAuditList().get(0).getResourceId());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getAuditType(), actualVXResource.getAuditList().get(0).getAuditType());


        }

        @Test
        public void testToVXResourceForTablesColumnsAndDatabase(){
                GUIDUtil guid = new GUIDUtil();
                String guidString = guid.genGUID();
                List<VXAuditMap> auditList = new ArrayList<VXAuditMap>();

                VXAuditMap vxAuditMap = new VXAuditMap();
                vxAuditMap.setResourceId(1L);
                vxAuditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);
                auditList.add(vxAuditMap);

                VXResource expectedVXResource = new VXResource();
                expectedVXResource.setName("/myDatabase/myTable/myColumn");
                expectedVXResource.setTables("myTable");
                expectedVXResource.setDatabases("myDatabase");
                expectedVXResource.setColumns("myColumn");
                expectedVXResource.setGuid(guidString);
                expectedVXResource.setPolicyName("hive Policy");
                expectedVXResource.setDescription("hive policy description");
                expectedVXResource.setResourceType(1);
                expectedVXResource.setAssetName("hive");
                expectedVXResource.setAssetType(3);
                expectedVXResource.setResourceStatus(1);
                expectedVXResource.setTableType(1);
                expectedVXResource.setColumnType(1);
                expectedVXResource.setAuditList(auditList);

                Map<String, RangerPolicyResource> rangerPolicyResourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesListForTable = new ArrayList<String>();
                valuesListForTable.add("myTable");

                List<String> valuesListForColumn = new ArrayList<String>();
                valuesListForColumn.add("myColumn");

                List<String> valuesListForDatabase = new ArrayList<String>();
                valuesListForDatabase.add("myDatabase");

                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("hive Policy");
                policy.setService("hive");
                policy.setDescription("hive policy description");
                policy.setIsEnabled(true);
                policy.setGuid(guidString);
                policy.setIsAuditEnabled(true);

                RangerService rangerService = new RangerService();
                rangerService.setName("hive");
                rangerService.setType("hive");

                RangerPolicyResource rangerPolicyResourceForTable = new RangerPolicyResource();
                rangerPolicyResourceForTable.setIsExcludes(true);
                rangerPolicyResourceForTable.setIsRecursive(true);
                rangerPolicyResourceForTable.setValue("table");
                rangerPolicyResourceForTable.setValues(valuesListForTable);

                rangerPolicyResourceMap.put("table", rangerPolicyResourceForTable);

                RangerPolicyResource rangerPolicyResourceForColumn = new RangerPolicyResource();
                rangerPolicyResourceForColumn.setIsExcludes(true);
                rangerPolicyResourceForColumn.setIsRecursive(true);
                rangerPolicyResourceForColumn.setValue("column");
                rangerPolicyResourceForColumn.setValues(valuesListForColumn);

                rangerPolicyResourceMap.put("column", rangerPolicyResourceForColumn);

                RangerPolicyResource rangerPolicyResourceForDatabase = new RangerPolicyResource();
                rangerPolicyResourceForDatabase.setIsExcludes(true);
                rangerPolicyResourceForDatabase.setIsRecursive(true);
                rangerPolicyResourceForDatabase.setValue("database");
                rangerPolicyResourceForDatabase.setValues(valuesListForDatabase);

                rangerPolicyResourceMap.put("database", rangerPolicyResourceForDatabase);


                policy.setResources(rangerPolicyResourceMap);

                VXResource actualVXResource = serviceUtil.toVXResource(policy, rangerService);


                Assert.assertNotNull(actualVXResource);
                Assert.assertEquals(expectedVXResource.getName(), actualVXResource.getName());
                Assert.assertEquals(expectedVXResource.getGuid(), actualVXResource.getGuid());
                Assert.assertEquals(expectedVXResource.getPolicyName(), actualVXResource.getPolicyName());
                Assert.assertEquals(expectedVXResource.getResourceType(), actualVXResource.getResourceType());
                Assert.assertEquals(expectedVXResource.getDescription(), actualVXResource.getDescription());
                Assert.assertEquals(expectedVXResource.getAssetName(), actualVXResource.getAssetName());
                Assert.assertEquals(expectedVXResource.getAssetType(), actualVXResource.getAssetType());
                Assert.assertEquals(expectedVXResource.getResourceStatus(), actualVXResource.getResourceStatus());
                Assert.assertEquals(expectedVXResource.getTableType(), actualVXResource.getTableType());
                Assert.assertEquals(expectedVXResource.getColumnType(), actualVXResource.getColumnType());
                Assert.assertEquals(expectedVXResource.getTables(), actualVXResource.getTables());
                Assert.assertEquals(expectedVXResource.getColumns(), actualVXResource.getColumns());
                Assert.assertEquals(expectedVXResource.getDatabases(), actualVXResource.getDatabases());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getResourceId(), actualVXResource.getAuditList().get(0).getResourceId());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getAuditType(), actualVXResource.getAuditList().get(0).getAuditType());


        }

        @Test
        public void testToVXResourceForTopologyAndService(){
                GUIDUtil guid = new GUIDUtil();
                String guidString = guid.genGUID();
                List<VXAuditMap> auditList = new ArrayList<VXAuditMap>();

                VXAuditMap vxAuditMap = new VXAuditMap();
                vxAuditMap.setResourceId(1L);
                vxAuditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);
                auditList.add(vxAuditMap);

                VXResource expectedVXResource = new VXResource();
                expectedVXResource.setName("/myTopology/myService");
                expectedVXResource.setTopologies("myTopology");
                expectedVXResource.setServices("myService");
                expectedVXResource.setGuid(guidString);
                expectedVXResource.setPolicyName("knox Policy");
                expectedVXResource.setDescription("knox policy description");
                expectedVXResource.setResourceType(1);
                expectedVXResource.setAssetName("knox");
                expectedVXResource.setAssetType(5);
                expectedVXResource.setResourceStatus(1);
                expectedVXResource.setAuditList(auditList);

                Map<String, RangerPolicyResource> rangerPolicyResourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesListForTopology = new ArrayList<String>();
                valuesListForTopology.add("myTopology");

                List<String> valuesListForService = new ArrayList<String>();
                valuesListForService.add("myService");

                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("knox Policy");
                policy.setService("knox");
                policy.setDescription("knox policy description");
                policy.setIsEnabled(true);
                policy.setGuid(guidString);
                policy.setIsAuditEnabled(true);

                RangerService rangerService = new RangerService();
                rangerService.setName("knox");
                rangerService.setType("knox");

                RangerPolicyResource rangerPolicyResourceForTopology = new RangerPolicyResource();
                rangerPolicyResourceForTopology.setValue("topology");
                rangerPolicyResourceForTopology.setValues(valuesListForTopology);

                rangerPolicyResourceMap.put("topology", rangerPolicyResourceForTopology);

                RangerPolicyResource rangerPolicyResourceForService = new RangerPolicyResource();
                rangerPolicyResourceForService.setValue("service");
                rangerPolicyResourceForService.setValues(valuesListForService);

                rangerPolicyResourceMap.put("service", rangerPolicyResourceForService);

                policy.setResources(rangerPolicyResourceMap);

                VXResource actualVXResource = serviceUtil.toVXResource(policy, rangerService);


                Assert.assertNotNull(actualVXResource);
                Assert.assertEquals(expectedVXResource.getName(), actualVXResource.getName());
                Assert.assertEquals(expectedVXResource.getGuid(), actualVXResource.getGuid());
                Assert.assertEquals(expectedVXResource.getPolicyName(), actualVXResource.getPolicyName());
                Assert.assertEquals(expectedVXResource.getResourceType(), actualVXResource.getResourceType());
                Assert.assertEquals(expectedVXResource.getDescription(), actualVXResource.getDescription());
                Assert.assertEquals(expectedVXResource.getAssetName(), actualVXResource.getAssetName());
                Assert.assertEquals(expectedVXResource.getAssetType(), actualVXResource.getAssetType());
                Assert.assertEquals(expectedVXResource.getResourceStatus(), actualVXResource.getResourceStatus());
                Assert.assertEquals(expectedVXResource.getTopologies(), actualVXResource.getTopologies());
                Assert.assertEquals(expectedVXResource.getServices(), actualVXResource.getServices());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getResourceId(), actualVXResource.getAuditList().get(0).getResourceId());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getAuditType(), actualVXResource.getAuditList().get(0).getAuditType());


        }

        @Test
        public void testToVXResourceForStormTopologyAndVXPermMapListWithUserList(){
                GUIDUtil guid = new GUIDUtil();
                String guidString = guid.genGUID();
                XXUser xxUser = new XXUser();
                xxUser.setId(6L);
                xxUser.setName("rangerAdmin");
                List<VXAuditMap> auditList = new ArrayList<VXAuditMap>();

                VXAuditMap vxAuditMap = new VXAuditMap();
                vxAuditMap.setResourceId(1L);
                vxAuditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);
                auditList.add(vxAuditMap);

                List<VXPermMap> vXPermMapList = new ArrayList<VXPermMap>();
                VXPermMap vXPermMap1 = new VXPermMap();
                vXPermMap1.setPermFor(1);
                vXPermMap1.setUserId(6L);
                vXPermMap1.setPermType(12);
                vXPermMap1.setUserName("rangerAdmin");
                vXPermMap1.setIpAddress("10.329.85.65");

                vXPermMapList.add(vXPermMap1);

                VXPermMap vXPermMap2 = new VXPermMap();
                vXPermMap2.setPermFor(1);
                vXPermMap2.setUserId(6L);
                vXPermMap2.setPermType(6);
                vXPermMap2.setUserName("rangerAdmin");
                vXPermMap2.setIpAddress("10.329.85.65");

                vXPermMapList.add(vXPermMap2);

                VXResource expectedVXResource = new VXResource();
                expectedVXResource.setGuid(guidString);
                expectedVXResource.setName("myTopology");
                expectedVXResource.setTopologies("myTopology");
                expectedVXResource.setPolicyName("storm Policy");
                expectedVXResource.setDescription("storm policy description");
                expectedVXResource.setResourceType(1);
                expectedVXResource.setAssetName("storm");
                expectedVXResource.setAssetType(6);
                expectedVXResource.setResourceStatus(1);
                expectedVXResource.setAuditList(auditList);
                expectedVXResource.setPermMapList(vXPermMapList);

                Map<String, RangerPolicyResource> rangerPolicyResourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesListForTopology = new ArrayList<String>();
                valuesListForTopology.add("myTopology");

                RangerPolicyResource rangerPolicyResourceForTopology = new RangerPolicyResource();
                rangerPolicyResourceForTopology.setValue("topology");
                rangerPolicyResourceForTopology.setValues(valuesListForTopology);

                rangerPolicyResourceMap.put("topology", rangerPolicyResourceForTopology);


                List<String> valuesListForRangerPolicyItemCondition = new ArrayList<String>();
                valuesListForRangerPolicyItemCondition.add("10.329.85.65");

                List<String> usersList = new ArrayList<String>();
                usersList.add("rangerAdmin");

                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("storm Policy");
                policy.setService("storm");
                policy.setDescription("storm policy description");
                policy.setIsEnabled(true);
                policy.setGuid(guidString);
                policy.setIsAuditEnabled(true);

                RangerService rangerService = new RangerService();
                rangerService.setName("storm");
                rangerService.setType("storm");

                List<RangerPolicyItem> rangerPolicyItemList = new ArrayList<RangerPolicy.RangerPolicyItem>();

                RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();

                List<RangerPolicyItemCondition> rangerPolicyItemConditionList = new ArrayList<RangerPolicy.RangerPolicyItemCondition>();
                RangerPolicyItemCondition rangerPolicyItemCondition = new RangerPolicyItemCondition();
                rangerPolicyItemCondition.setType("ipaddress");
                rangerPolicyItemCondition.setValues(valuesListForRangerPolicyItemCondition);
                rangerPolicyItemConditionList.add(rangerPolicyItemCondition);

                rangerPolicyItem.setConditions(rangerPolicyItemConditionList);

                rangerPolicyItem.setUsers(usersList);

                List<RangerPolicyItemAccess> rangerPolicyItemAccessList = new ArrayList<RangerPolicy.RangerPolicyItemAccess>();
                RangerPolicyItemAccess rangerPolicyItemAccess = new RangerPolicyItemAccess();
                rangerPolicyItemAccess.setIsAllowed(true);
                rangerPolicyItemAccess.setType("drop");

                rangerPolicyItemAccessList.add(rangerPolicyItemAccess);

                rangerPolicyItem.setAccesses(rangerPolicyItemAccessList);

                rangerPolicyItem.setDelegateAdmin(true);

                rangerPolicyItemList.add(rangerPolicyItem);

                policy.setPolicyItems(rangerPolicyItemList);

                policy.setResources(rangerPolicyResourceMap);

                Mockito.when(xaDaoMgr.getXXUser()).thenReturn(xxUserDao);
                Mockito.when(xxUserDao.findByUserName("rangerAdmin")).thenReturn(xxUser);


                VXResource actualVXResource = serviceUtil.toVXResource(policy, rangerService);


                Assert.assertNotNull(actualVXResource);
                Assert.assertEquals(expectedVXResource.getName(), actualVXResource.getName());
                Assert.assertEquals(expectedVXResource.getGuid(), actualVXResource.getGuid());
                Assert.assertEquals(expectedVXResource.getPolicyName(), actualVXResource.getPolicyName());
                Assert.assertEquals(expectedVXResource.getResourceType(), actualVXResource.getResourceType());
                Assert.assertEquals(expectedVXResource.getDescription(), actualVXResource.getDescription());
                Assert.assertEquals(expectedVXResource.getAssetName(), actualVXResource.getAssetName());
                Assert.assertEquals(expectedVXResource.getAssetType(), actualVXResource.getAssetType());
                Assert.assertEquals(expectedVXResource.getResourceStatus(), actualVXResource.getResourceStatus());
                Assert.assertEquals(expectedVXResource.getTopologies(), actualVXResource.getTopologies());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getResourceId(), actualVXResource.getAuditList().get(0).getResourceId());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getAuditType(), actualVXResource.getAuditList().get(0).getAuditType());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getPermFor(), actualVXResource.getPermMapList().get(0).getPermFor());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getPermType(), actualVXResource.getPermMapList().get(0).getPermType());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getUserName(), actualVXResource.getPermMapList().get(0).getUserName());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getIpAddress(), actualVXResource.getPermMapList().get(0).getIpAddress());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getUserId(), actualVXResource.getPermMapList().get(0).getUserId());

                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getPermFor(), actualVXResource.getPermMapList().get(1).getPermFor());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getPermType(), actualVXResource.getPermMapList().get(1).getPermType());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getUserName(), actualVXResource.getPermMapList().get(1).getUserName());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getIpAddress(), actualVXResource.getPermMapList().get(1).getIpAddress());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getUserId(), actualVXResource.getPermMapList().get(1).getUserId());


        }


        @Test
        public void testToVXResourceForStormTopologyAndVXPermMapListWithGroupList(){
                GUIDUtil guid = new GUIDUtil();
                String guidString = guid.genGUID();
                XXGroup xxGroup = new XXGroup();
                xxGroup.setId(6L);
                xxGroup.setName("rangerGroup");
                List<VXAuditMap> auditList = new ArrayList<VXAuditMap>();

                VXAuditMap vxAuditMap = new VXAuditMap();
                vxAuditMap.setResourceId(1L);
                vxAuditMap.setAuditType(AppConstants.XA_AUDIT_TYPE_ALL);
                auditList.add(vxAuditMap);

                List<VXPermMap> vXPermMapList = new ArrayList<VXPermMap>();
                VXPermMap vXPermMap1 = new VXPermMap();
                vXPermMap1.setPermFor(2);
                vXPermMap1.setPermType(12);
                vXPermMap1.setGroupName("rangerGroup");
                vXPermMap1.setIpAddress("10.329.85.65");

                vXPermMapList.add(vXPermMap1);

                VXPermMap vXPermMap2 = new VXPermMap();
                vXPermMap2.setPermFor(2);
                vXPermMap2.setPermType(6);
                vXPermMap2.setGroupName("rangerGroup");
                vXPermMap2.setIpAddress("10.329.85.65");

                vXPermMapList.add(vXPermMap2);

                VXResource expectedVXResource = new VXResource();
                expectedVXResource.setGuid(guidString);
                expectedVXResource.setName("myTopology");
                expectedVXResource.setTopologies("myTopology");
                expectedVXResource.setPolicyName("storm Policy");
                expectedVXResource.setDescription("storm policy description");
                expectedVXResource.setResourceType(1);
                expectedVXResource.setAssetName("storm");
                expectedVXResource.setAssetType(6);
                expectedVXResource.setResourceStatus(1);
                expectedVXResource.setAuditList(auditList);
                expectedVXResource.setPermMapList(vXPermMapList);

                Map<String, RangerPolicyResource> rangerPolicyResourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesListForTopology = new ArrayList<String>();
                valuesListForTopology.add("myTopology");

                RangerPolicyResource rangerPolicyResourceForTopology = new RangerPolicyResource();
                rangerPolicyResourceForTopology.setValue("topology");
                rangerPolicyResourceForTopology.setValues(valuesListForTopology);

                rangerPolicyResourceMap.put("topology", rangerPolicyResourceForTopology);


                List<String> valuesListForRangerPolicyItemCondition = new ArrayList<String>();
                valuesListForRangerPolicyItemCondition.add("10.329.85.65");

                List<String> groupList = new ArrayList<String>();
                groupList.add("rangerGroup");

                RangerPolicy policy = new RangerPolicy();
                policy.setId(1L);
                policy.setName("storm Policy");
                policy.setService("storm");
                policy.setDescription("storm policy description");
                policy.setIsEnabled(true);
                policy.setGuid(guidString);
                policy.setIsAuditEnabled(true);

                RangerService rangerService = new RangerService();
                rangerService.setName("storm");
                rangerService.setType("storm");

                List<RangerPolicyItem> rangerPolicyItemList = new ArrayList<RangerPolicy.RangerPolicyItem>();

                RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();

                List<RangerPolicyItemCondition> rangerPolicyItemConditionList = new ArrayList<RangerPolicy.RangerPolicyItemCondition>();
                RangerPolicyItemCondition rangerPolicyItemCondition = new RangerPolicyItemCondition();
                rangerPolicyItemCondition.setType("ipaddress");
                rangerPolicyItemCondition.setValues(valuesListForRangerPolicyItemCondition);
                rangerPolicyItemConditionList.add(rangerPolicyItemCondition);

                rangerPolicyItem.setConditions(rangerPolicyItemConditionList);

                rangerPolicyItem.setGroups(groupList);

                List<RangerPolicyItemAccess> rangerPolicyItemAccessList = new ArrayList<RangerPolicy.RangerPolicyItemAccess>();
                RangerPolicyItemAccess rangerPolicyItemAccess = new RangerPolicyItemAccess();
                rangerPolicyItemAccess.setIsAllowed(true);
                rangerPolicyItemAccess.setType("drop");

                rangerPolicyItemAccessList.add(rangerPolicyItemAccess);

                rangerPolicyItem.setAccesses(rangerPolicyItemAccessList);

                rangerPolicyItem.setDelegateAdmin(true);

                rangerPolicyItemList.add(rangerPolicyItem);

                policy.setPolicyItems(rangerPolicyItemList);

                policy.setResources(rangerPolicyResourceMap);

                Mockito.when(xaDaoMgr.getXXGroup()).thenReturn(xxGroupDao);
                Mockito.when(xxGroupDao.findByGroupName("rangerGroup")).thenReturn(xxGroup);


                VXResource actualVXResource = serviceUtil.toVXResource(policy, rangerService);


                Assert.assertNotNull(actualVXResource);
                Assert.assertEquals(expectedVXResource.getName(), actualVXResource.getName());
                Assert.assertEquals(expectedVXResource.getGuid(), actualVXResource.getGuid());
                Assert.assertEquals(expectedVXResource.getPolicyName(), actualVXResource.getPolicyName());
                Assert.assertEquals(expectedVXResource.getResourceType(), actualVXResource.getResourceType());
                Assert.assertEquals(expectedVXResource.getDescription(), actualVXResource.getDescription());
                Assert.assertEquals(expectedVXResource.getAssetName(), actualVXResource.getAssetName());
                Assert.assertEquals(expectedVXResource.getAssetType(), actualVXResource.getAssetType());
                Assert.assertEquals(expectedVXResource.getResourceStatus(), actualVXResource.getResourceStatus());
                Assert.assertEquals(expectedVXResource.getTopologies(), actualVXResource.getTopologies());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getResourceId(), actualVXResource.getAuditList().get(0).getResourceId());
                Assert.assertEquals(expectedVXResource.getAuditList().get(0).getAuditType(), actualVXResource.getAuditList().get(0).getAuditType());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getPermFor(), actualVXResource.getPermMapList().get(0).getPermFor());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getPermType(), actualVXResource.getPermMapList().get(0).getPermType());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getUserName(), actualVXResource.getPermMapList().get(0).getUserName());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getIpAddress(), actualVXResource.getPermMapList().get(0).getIpAddress());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(0).getUserId(), actualVXResource.getPermMapList().get(0).getUserId());

                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getPermFor(), actualVXResource.getPermMapList().get(1).getPermFor());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getPermType(), actualVXResource.getPermMapList().get(1).getPermType());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getUserName(), actualVXResource.getPermMapList().get(1).getUserName());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getIpAddress(), actualVXResource.getPermMapList().get(1).getIpAddress());
                Assert.assertEquals(expectedVXResource.getPermMapList().get(1).getUserId(), actualVXResource.getPermMapList().get(1).getUserId());


        }

        @Test
        public void testPublicObjecttoVXAsset(){
                Date date = new Date();

                VXAsset expectedVXAsset = new VXAsset();
                expectedVXAsset.setId(1L);
                expectedVXAsset.setCreateDate(date);
                expectedVXAsset.setUpdateDate(date);
                expectedVXAsset.setOwner("rangerAdmin");
                expectedVXAsset.setUpdatedBy("rangerAdmin");
                expectedVXAsset.setAssetType(3);
                expectedVXAsset.setActiveStatus(RangerCommonEnums.STATUS_ENABLED);
                expectedVXAsset.setName("hive");
                expectedVXAsset.setDescription("hive description");
                expectedVXAsset.setConfig("/myConfig");

                VXRepository vXRepository = new VXRepository();
                vXRepository.setId(1L);
                vXRepository.setCreateDate(date);
                vXRepository.setUpdateDate(date);
                vXRepository.setOwner("rangerAdmin");
                vXRepository.setUpdatedBy("rangerAdmin");
                vXRepository.setRepositoryType("hive");
                vXRepository.setIsActive(true);
                vXRepository.setName("hive");
                vXRepository.setDescription("hive description");
                vXRepository.setConfig("/myConfig");

                VXAsset actualVXAsset = serviceUtil.publicObjecttoVXAsset(vXRepository);

                Assert.assertNotNull(actualVXAsset);
                Assert.assertEquals(actualVXAsset.getActiveStatus(), expectedVXAsset.getActiveStatus());
                Assert.assertEquals(actualVXAsset.getId(), expectedVXAsset.getId());
                Assert.assertEquals(actualVXAsset.getName(), expectedVXAsset.getName());
                Assert.assertEquals(actualVXAsset.getDescription(), expectedVXAsset.getDescription());
                Assert.assertEquals(actualVXAsset.getCreateDate(), expectedVXAsset.getCreateDate());
                Assert.assertEquals(actualVXAsset.getOwner(), expectedVXAsset.getOwner());
                Assert.assertEquals(actualVXAsset.getAssetType() , expectedVXAsset.getAssetType());
                Assert.assertEquals(actualVXAsset.getConfig() , expectedVXAsset.getConfig());



        }

        @Test
        public void testVXAssetToPublicObject(){

                Date date = new Date();

                VXRepository expectedVXRepository = new VXRepository();
                expectedVXRepository.setId(1L);
                expectedVXRepository.setCreateDate(date);
                expectedVXRepository.setUpdateDate(date);
                expectedVXRepository.setOwner("rangerAdmin");
                expectedVXRepository.setUpdatedBy("rangerAdmin");
                expectedVXRepository.setRepositoryType("hive");
                expectedVXRepository.setIsActive(true);
                expectedVXRepository.setName("hive");
                expectedVXRepository.setDescription("hive description");
                expectedVXRepository.setConfig("/myConfig");

                VXAsset vXAsset = new VXAsset();
                vXAsset.setId(1L);
                vXAsset.setCreateDate(date);
                vXAsset.setUpdateDate(date);
                vXAsset.setOwner("rangerAdmin");
                vXAsset.setUpdatedBy("rangerAdmin");
                vXAsset.setAssetType(3);
                vXAsset.setActiveStatus(RangerCommonEnums.STATUS_ENABLED);
                vXAsset.setName("hive");
                vXAsset.setDescription("hive description");
                vXAsset.setConfig("/myConfig");

                VXRepository actualVXRepository = serviceUtil.vXAssetToPublicObject(vXAsset);

                Assert.assertNotNull(actualVXRepository);
                Assert.assertEquals(expectedVXRepository.getId(), actualVXRepository.getId());
                Assert.assertEquals(expectedVXRepository.getName(), actualVXRepository.getName());
                Assert.assertEquals(expectedVXRepository.getDescription(), actualVXRepository.getDescription());
                Assert.assertTrue(actualVXRepository.getIsActive());
                Assert.assertEquals(expectedVXRepository.getCreateDate(), actualVXRepository.getCreateDate());
                Assert.assertEquals(expectedVXRepository.getOwner(), actualVXRepository.getOwner());
                Assert.assertEquals(expectedVXRepository.getRepositoryType() , actualVXRepository.getRepositoryType());
                Assert.assertEquals(expectedVXRepository.getConfig() , actualVXRepository.getConfig());

        }

        @Test
        public void testGetMappedSearchParams(){
                ArrayList<Integer> statusList = new ArrayList<Integer>();
                statusList.add(RangerCommonEnums.STATUS_DISABLED);
                statusList.add(RangerCommonEnums.STATUS_ENABLED);

                SearchCriteria expectedSearchCriteria = new SearchCriteria();
                expectedSearchCriteria.addParam("status", statusList);
                expectedSearchCriteria.addParam("type", 3);

                SearchCriteria sc = new SearchCriteria();
                HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

                sc.addParam("status", null);
                sc.addParam("type", "hive");

                SearchCriteria actualSearchCriteria = serviceUtil.getMappedSearchParams(request, sc);

                Assert.assertNotNull(actualSearchCriteria);
                Assert.assertEquals(expectedSearchCriteria.getParamValue("type"), actualSearchCriteria.getParamValue("type"));
                Assert.assertEquals(expectedSearchCriteria.getParamValue("status"), actualSearchCriteria.getParamValue("status"));

        }

        @Test
        public void testIsValidService() throws Exception{
                RangerService rangerService = new RangerService();
                rangerService.setId(1L);
                rangerService.setName("hiveService");
                rangerService.setIsEnabled(true);

                HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                String serviceName = "hiveService";


                Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
                boolean isValid = serviceUtil.isValidService(serviceName, request);

                Assert.assertTrue(isValid);


        }

        @Test
        public void testIsValidateHttpsAuthentication() throws Exception{

                RangerService rangerService = new RangerService();
                rangerService.setId(1L);
                rangerService.setName("hiveService");
                rangerService.setIsEnabled(true);

                HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                String serviceName = "hiveService";


                Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
                boolean isValidAuthentication = serviceUtil.isValidateHttpsAuthentication(serviceName, request);

                Assert.assertTrue(isValidAuthentication);


        }

        @Test
        public void testToGrantRevokeRequestForHive() throws Exception{
                GrantRevokeRequest expectedGrantRevokeRequest = new GrantRevokeRequest();
                expectedGrantRevokeRequest.setGrantor("rangerAdmin");
                expectedGrantRevokeRequest.setEnableAudit(true);
                expectedGrantRevokeRequest.setIsRecursive(false);
                expectedGrantRevokeRequest.setReplaceExistingPermissions(true);

                Map<String, String> mapResource = new HashMap<String, String>();
                mapResource.put("database", "myDatabase");
                mapResource.put("table", "myTable");
                mapResource.put("column", "myColumn");

                expectedGrantRevokeRequest.setResource(mapResource);

                String serviceName = "hive";

                RangerService rangerService = new RangerService();
                rangerService.setId(1L);
                rangerService.setName("hiveService");
                rangerService.setIsEnabled(true);
                rangerService.setType("hive");

                VXPolicy vXPolicy = new VXPolicy();
                vXPolicy.setRepositoryName("hive");
                vXPolicy.setGrantor("rangerAdmin");
                vXPolicy.setReplacePerm(true);
                vXPolicy.setDatabases("myDatabase");
                vXPolicy.setColumns("myColumn");
                vXPolicy.setTables("myTable");

                Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);

                GrantRevokeRequest actualGrantRevokeRequest = serviceUtil.toGrantRevokeRequest(vXPolicy);

                Assert.assertNotNull(actualGrantRevokeRequest);
                Assert.assertTrue(actualGrantRevokeRequest.getEnableAudit());
                Assert.assertFalse(actualGrantRevokeRequest.getIsRecursive());
                Assert.assertTrue(actualGrantRevokeRequest.getReplaceExistingPermissions());
                Assert.assertEquals(expectedGrantRevokeRequest.getGrantor(), actualGrantRevokeRequest.getGrantor());
                Assert.assertEquals(expectedGrantRevokeRequest.getResource(), actualGrantRevokeRequest.getResource());
        }

        @Test
        public void testToGrantRevokeRequestForHbase() throws Exception{
                GrantRevokeRequest expectedGrantRevokeRequest = new GrantRevokeRequest();
                expectedGrantRevokeRequest.setGrantor("rangerAdmin");
                expectedGrantRevokeRequest.setEnableAudit(true);
                expectedGrantRevokeRequest.setIsRecursive(false);
                expectedGrantRevokeRequest.setReplaceExistingPermissions(true);

                Map<String, String> mapResource = new HashMap<String, String>();
                mapResource.put("table", "myTable");
                mapResource.put("column", "myColumn");

                mapResource.put("column-family", "myColumnFamily");
                expectedGrantRevokeRequest.setResource(mapResource);

                String serviceName = "hbase";

                RangerService rangerService = new RangerService();
                rangerService.setId(1L);
                rangerService.setName("hbaseService");
                rangerService.setIsEnabled(true);
                rangerService.setType("hbase");

                VXPolicy vXPolicy = new VXPolicy();
                vXPolicy.setRepositoryName("hbase");
                vXPolicy.setGrantor("rangerAdmin");
                vXPolicy.setReplacePerm(true);
                vXPolicy.setColumns("myColumn");
                vXPolicy.setColumnFamilies("myColumnFamily");
                vXPolicy.setTables("myTable");

                Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);

                GrantRevokeRequest actualGrantRevokeRequest = serviceUtil.toGrantRevokeRequest(vXPolicy);

                Assert.assertNotNull(actualGrantRevokeRequest);
                Assert.assertTrue(actualGrantRevokeRequest.getEnableAudit());
                Assert.assertFalse(actualGrantRevokeRequest.getIsRecursive());
                Assert.assertTrue(actualGrantRevokeRequest.getReplaceExistingPermissions());
                Assert.assertEquals(expectedGrantRevokeRequest.getGrantor(), actualGrantRevokeRequest.getGrantor());
                Assert.assertEquals(expectedGrantRevokeRequest.getResource(), actualGrantRevokeRequest.getResource());
        }

        @Test
        public void testToGrantRevokeRequestForPermMapList() throws Exception{
                GrantRevokeRequest expectedGrantRevokeRequest = new GrantRevokeRequest();
                expectedGrantRevokeRequest.setGrantor("rangerAdmin");
                expectedGrantRevokeRequest.setEnableAudit(true);
                expectedGrantRevokeRequest.setIsRecursive(false);
                expectedGrantRevokeRequest.setReplaceExistingPermissions(true);

                List<String> userList = new ArrayList<String>();
                userList.add("rangerAdmin");

                List<String> groupList = new ArrayList<String>();
                groupList.add("rangerGroup");

                List<String> permObjList = new ArrayList<String>();
                permObjList.add("Admin");

                Map<String, String> mapResource = new HashMap<String, String>();
                mapResource.put("database", "myDatabase");
                mapResource.put("table", "myTable");
                mapResource.put("column", "myColumn");

                expectedGrantRevokeRequest.setResource(mapResource);

                List<VXPermObj> vXPermObjList = new ArrayList<VXPermObj>();
                VXPermObj vXPermObj = new VXPermObj();
                vXPermObj.setUserList(userList);
                vXPermObj.setGroupList(groupList);
                vXPermObj.setPermList(permObjList);

                vXPermObjList.add(vXPermObj);


                String serviceName = "hive";

                RangerService rangerService = new RangerService();
                rangerService.setId(1L);
                rangerService.setName("hiveService");
                rangerService.setIsEnabled(true);
                rangerService.setType("hive");

                VXPolicy vXPolicy = new VXPolicy();
                vXPolicy.setRepositoryName("hive");
                vXPolicy.setGrantor("rangerAdmin");
                vXPolicy.setReplacePerm(true);
                vXPolicy.setColumns("myColumn");
                vXPolicy.setDatabases("myDatabase");
                vXPolicy.setTables("myTable");
                vXPolicy.setPermMapList(vXPermObjList);

                Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);

                GrantRevokeRequest actualGrantRevokeRequest = serviceUtil.toGrantRevokeRequest(vXPolicy);

                Assert.assertNotNull(actualGrantRevokeRequest);
                Assert.assertTrue(actualGrantRevokeRequest.getEnableAudit());
                Assert.assertTrue(actualGrantRevokeRequest.getDelegateAdmin());
                Assert.assertFalse(actualGrantRevokeRequest.getIsRecursive());
                Assert.assertTrue(actualGrantRevokeRequest.getReplaceExistingPermissions());
                Assert.assertTrue(actualGrantRevokeRequest.getUsers().contains("rangerAdmin"));
                Assert.assertTrue(actualGrantRevokeRequest.getGroups().contains("rangerGroup"));
                Assert.assertEquals(expectedGrantRevokeRequest.getGrantor(), actualGrantRevokeRequest.getGrantor());
                Assert.assertEquals(expectedGrantRevokeRequest.getResource(), actualGrantRevokeRequest.getResource());
        }

        @Test
        public void testToRangerPolicyForNullServiceAndNullVXPolicy(){
                VXPolicy vXPolicy = null;
                RangerService service = null;

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(vXPolicy, service);
                Assert.assertNull(actualRangerPolicy);
        }

        @Test
        public void testToRangerPolicy(){
                Date date = new Date();

                List<String> userList = new ArrayList<String>();
                userList.add("rangerAdmin");

                List<String> groupList = new ArrayList<String>();
                groupList.add("rangerGroup");

                List<String> permObjList = new ArrayList<String>();
                permObjList.add("Admin");

                Map<String, RangerPolicyResource> resourceMap = new HashMap<String, RangerPolicyResource>();
                List<String> valuesList = new ArrayList<String>();
                valuesList.add("resource");

                RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
                rangerPolicyResource.setIsExcludes(false);
                rangerPolicyResource.setIsRecursive(true);
                rangerPolicyResource.setValues(valuesList);

                resourceMap.put("path", rangerPolicyResource);

                List<RangerPolicyItem> rangerPolicyItemList = new ArrayList<RangerPolicy.RangerPolicyItem>();
                RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
                rangerPolicyItem.setUsers(userList);
                rangerPolicyItem.setGroups(groupList);

                List<RangerPolicyItemCondition> rangerPolicyItemConditionList = new ArrayList<RangerPolicy.RangerPolicyItemCondition>();
                RangerPolicyItemCondition rangerPolicyItemCondition = new RangerPolicyItemCondition();
                rangerPolicyItemCondition.setType("ipaddress");
                List<String> conditionValueList = new ArrayList<String>();
                conditionValueList.add("10.129.35.86");
                rangerPolicyItemCondition.setValues(conditionValueList);
                rangerPolicyItemConditionList.add(rangerPolicyItemCondition);
                rangerPolicyItem.setConditions(rangerPolicyItemConditionList);
                rangerPolicyItem.setDelegateAdmin(true);

                rangerPolicyItemList.add(rangerPolicyItem);

                RangerPolicy expectedRangerPolicy = new RangerPolicy();
                expectedRangerPolicy.setId(1L);
                expectedRangerPolicy.setName("hdfs");
                expectedRangerPolicy.setCreatedBy("rangerAdmin");
                expectedRangerPolicy.setCreateTime(date);
                expectedRangerPolicy.setDescription("hdfs policy description");
                expectedRangerPolicy.setIsAuditEnabled(true);
                expectedRangerPolicy.setResources(resourceMap);
                expectedRangerPolicy.setPolicyItems(rangerPolicyItemList);

                VXPolicy vXPolicy = new VXPolicy();
                vXPolicy.setId(1L);
                vXPolicy.setCreateDate(date);
                vXPolicy.setUpdateDate(date);
                vXPolicy.setOwner("rangerAdmin");
                vXPolicy.setUpdatedBy("rangerAdmin");
                vXPolicy.setPolicyName("hdfs");
                vXPolicy.setDescription("hdfs policy description");
                vXPolicy.setIsEnabled(true);
                vXPolicy.setIsAuditEnabled(true);
                vXPolicy.setIsRecursive(true);
                vXPolicy.setResourceName("resource");

                RangerService service = new RangerService();
                service.setId(1L);
                service.setName("hdfsService");
                service.setType("hdfs");

                List<VXPermObj> vXPermObjList = new ArrayList<VXPermObj>();
                VXPermObj vXPermObj = new VXPermObj();
                vXPermObj.setUserList(userList);
                vXPermObj.setGroupList(groupList);
                vXPermObj.setPermList(permObjList);

                vXPermObj.setIpAddress("10.129.35.86");

                vXPermObjList.add(vXPermObj);

                vXPolicy.setPermMapList(vXPermObjList);

                RangerPolicy actualRangerPolicy = serviceUtil.toRangerPolicy(vXPolicy, service);

                Assert.assertNotNull(actualRangerPolicy);
                Assert.assertEquals(expectedRangerPolicy.getId(), actualRangerPolicy.getId());
                Assert.assertEquals(expectedRangerPolicy.getName(), actualRangerPolicy.getName());
                Assert.assertEquals(expectedRangerPolicy.getDescription(), actualRangerPolicy.getDescription());
                Assert.assertEquals(expectedRangerPolicy.getCreatedBy(), actualRangerPolicy.getCreatedBy());
                Assert.assertTrue(actualRangerPolicy.getIsAuditEnabled());
                Assert.assertEquals(expectedRangerPolicy.getResources(), actualRangerPolicy.getResources());
                Assert.assertEquals(expectedRangerPolicy.getPolicyItems(), actualRangerPolicy.getPolicyItems());
        }
}
