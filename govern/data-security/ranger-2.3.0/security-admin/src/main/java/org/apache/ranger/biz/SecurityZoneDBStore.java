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

package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXSecurityZone;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerSecurityZoneHeaderInfo;
import org.apache.ranger.plugin.model.RangerServiceHeaderInfo;
import org.apache.ranger.plugin.store.AbstractPredicateUtil;
import org.apache.ranger.plugin.store.SecurityZonePredicateUtil;
import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerSecurityZoneServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class SecurityZoneDBStore implements SecurityZoneStore {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityZoneDBStore.class);
    private static final String RANGER_GLOBAL_STATE_NAME = "RangerSecurityZone";

    @Autowired
    RangerSecurityZoneServiceService securityZoneService;

    @Autowired
    RangerDaoManager daoMgr;

    @Autowired
    RESTErrorUtil restErrorUtil;

    @Autowired
	SecurityZoneRefUpdater securityZoneRefUpdater;

    @Autowired
    RangerBizUtil bizUtil;

    AbstractPredicateUtil predicateUtil = null;

    public void init() throws Exception {}

    @PostConstruct
    public void initStore() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SecurityZoneDBStore.initStore()");
        }

        predicateUtil = new SecurityZonePredicateUtil();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== SecurityZoneDBStore.initStore()");
        }
    }

    @Override
    public RangerSecurityZone createSecurityZone(RangerSecurityZone securityZone) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SecurityZoneDBStore.createSecurityZone()");
        }

        XXSecurityZone xxSecurityZone = daoMgr.getXXSecurityZoneDao().findByZoneName(securityZone.getName());

        if (xxSecurityZone != null) {
            throw restErrorUtil.createRESTException("security-zone with name: " + securityZone.getName() + " already exists", MessageEnums.ERROR_DUPLICATE_OBJECT);
        }

        daoMgr.getXXGlobalState().onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);

        RangerSecurityZone createdSecurityZone = securityZoneService.create(securityZone);
        if (createdSecurityZone == null) {
            throw new Exception("Cannot create security zone:[" + securityZone + "]");
        }
        securityZoneRefUpdater.createNewZoneMappingForRefTable(createdSecurityZone);
        List<XXTrxLog> trxLogList = securityZoneService.getTransactionLog(createdSecurityZone, null, "create");
        bizUtil.createTrxLog(trxLogList);
        return createdSecurityZone;
    }

    @Override
    public RangerSecurityZone updateSecurityZoneById(RangerSecurityZone securityZone) throws Exception {
        XXSecurityZone xxSecurityZone = daoMgr.getXXSecurityZoneDao().findByZoneId(securityZone.getId());
        if (xxSecurityZone == null) {
            throw restErrorUtil.createRESTException("security-zone with id: " + securityZone.getId() + " does not exist");
        }

        Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").create();
        RangerSecurityZone oldSecurityZone = gsonBuilder.fromJson(xxSecurityZone.getJsonData(), RangerSecurityZone.class);

        daoMgr.getXXGlobalState().onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);

        RangerSecurityZone updatedSecurityZone = securityZoneService.update(securityZone);
        if (updatedSecurityZone == null) {
            throw new Exception("Cannot update security zone:[" + securityZone + "]");
        }
        securityZoneRefUpdater.createNewZoneMappingForRefTable(updatedSecurityZone);
        List<XXTrxLog> trxLogList = securityZoneService.getTransactionLog(updatedSecurityZone, oldSecurityZone, "update");
        bizUtil.createTrxLog(trxLogList);
        return securityZone;
    }

    @Override
    public void deleteSecurityZoneByName(String zoneName) throws Exception {
        XXSecurityZone xxSecurityZone = daoMgr.getXXSecurityZoneDao().findByZoneName(zoneName);
        if (xxSecurityZone == null) {
            throw restErrorUtil.createRESTException("security-zone with name: " + zoneName + " does not exist");
        }
        RangerSecurityZone securityZone = securityZoneService.read(xxSecurityZone.getId());

        daoMgr.getXXGlobalState().onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);

        securityZoneRefUpdater.cleanupRefTables(securityZone);

        securityZoneService.delete(securityZone);
        List<XXTrxLog> trxLogList = securityZoneService.getTransactionLog(securityZone, null, "delete");
        bizUtil.createTrxLog(trxLogList);
        }

    @Override
    public void deleteSecurityZoneById(Long zoneId) throws Exception {
        RangerSecurityZone securityZone = securityZoneService.read(zoneId);

        daoMgr.getXXGlobalState().onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);

        securityZoneRefUpdater.cleanupRefTables(securityZone);

        securityZoneService.delete(securityZone);
        List<XXTrxLog> trxLogList = securityZoneService.getTransactionLog(securityZone, null, "delete");
        bizUtil.createTrxLog(trxLogList);
    }

    @Override
    public RangerSecurityZone getSecurityZone(Long id) throws Exception {
        return securityZoneService.read(id);
    }

    @Override
    public RangerSecurityZone getSecurityZoneByName(String name) throws Exception {
        XXSecurityZone xxSecurityZone = daoMgr.getXXSecurityZoneDao().findByZoneName(name);
        if (xxSecurityZone == null) {
            throw restErrorUtil.createRESTException("security-zone with name: " + name + " does not exist");
        }
        return securityZoneService.read(xxSecurityZone.getId());
    }

    @Override
    public List<RangerSecurityZone> getSecurityZones(SearchFilter filter) throws Exception {
        List<RangerSecurityZone> ret = new ArrayList<>();

        List<XXSecurityZone> xxSecurityZones = daoMgr.getXXSecurityZoneDao().getAll();

        for (XXSecurityZone xxSecurityZone : xxSecurityZones) {
            if (!xxSecurityZone.getId().equals(RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID)) {
                ret.add(securityZoneService.read(xxSecurityZone.getId()));
            }
        }

        if (CollectionUtils.isNotEmpty(ret) && filter != null && !filter.isEmpty()) {
            List<RangerSecurityZone> copy = new ArrayList<>(ret);

            predicateUtil.applyFilter(copy, filter);
            ret = copy;
        }

        return ret;
    }

    @Override
    public Map<String, RangerSecurityZone.RangerSecurityZoneService> getSecurityZonesForService(String serviceName) {
        Map<String, RangerSecurityZone.RangerSecurityZoneService> ret = null;

        SearchFilter filter = new SearchFilter();
        filter.setParam(SearchFilter.SERVICE_NAME, serviceName);

        try {
            List<RangerSecurityZone> matchingZones = getSecurityZones(filter);

            if (CollectionUtils.isNotEmpty(matchingZones)) {
                ret = new HashMap<>();

                for (RangerSecurityZone matchingZone : matchingZones) {
                    ret.put(matchingZone.getName(), matchingZone.getServices().get(serviceName));
                }
            }
        } catch (Exception excp) {
            LOG.error("Failed to get security zones for service:[" + serviceName + "]", excp);
        }

        return ret;
    }

    public List<RangerSecurityZoneHeaderInfo> getSecurityZoneHeaderInfoList() {
        return daoMgr.getXXSecurityZoneDao().findAllZoneHeaderInfos();
    }

    public List<RangerServiceHeaderInfo> getServiceHeaderInfoListByZoneId(Long zoneId) {
        List<RangerServiceHeaderInfo> services    = daoMgr.getXXSecurityZoneRefService().findServiceHeaderInfosByZoneId(zoneId);
        List<RangerServiceHeaderInfo> tagServices = daoMgr.getXXSecurityZoneRefTagService().findServiceHeaderInfosByZoneId(zoneId);
        services.addAll(tagServices);

        return services;
    }
}
