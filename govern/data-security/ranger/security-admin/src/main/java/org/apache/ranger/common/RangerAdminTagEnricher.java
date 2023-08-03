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

import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.plugin.contextenricher.RangerTagEnricher;

import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.store.TagStore;
import org.apache.ranger.plugin.util.RangerReadWriteLock;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAdminTagEnricher extends RangerTagEnricher {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAdminTagEnricher.class);

    private static TagStore         tagStore   = null;
    private static RangerDaoManager daoManager = null;

    private static boolean ADMIN_TAG_ENRICHER_SUPPORTS_TAG_DELTAS_INITIALIZED = false;
    private static boolean ADMIN_TAG_ENRICHER_SUPPORTS_TAG_DELTAS;

    private Long serviceId;

    public static void setTagStore(TagStore tagStore) {
        RangerAdminTagEnricher.tagStore   = tagStore;
    }

    public static void setDaoManager(RangerDaoManager daoManager) {
        RangerAdminTagEnricher.daoManager = daoManager;
    }

    @Override
    public void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAdminTagEnricher.init()");
        }
        super.init();

        if (!ADMIN_TAG_ENRICHER_SUPPORTS_TAG_DELTAS_INITIALIZED) {
            RangerAdminConfig config = RangerAdminConfig.getInstance();

            ADMIN_TAG_ENRICHER_SUPPORTS_TAG_DELTAS = config.getBoolean("ranger.admin.tag.enricher.supports.tag.deltas", true);

            ADMIN_TAG_ENRICHER_SUPPORTS_TAG_DELTAS_INITIALIZED = true;
        }

        ServiceStore svcStore = tagStore != null ? tagStore.getServiceStore() : null;

        if (tagStore == null || svcStore == null) {
            LOG.error("ServiceDBStore/TagDBStore is not initialized!! Internal Error!");
        } else {
            super.init();
            try {
                RangerService service = svcStore.getServiceByName(serviceName);
                serviceId = service.getId();
                createLock();
            } catch (Exception e) {
                LOG.error("Cannot find service with name:[" + serviceName + "]", e);
                LOG.error("This will cause tag-enricher in Ranger-Admin to fail!!");
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAdminTagEnricher.init()");
        }
    }

    @Override
    protected RangerReadWriteLock createLock() {
        boolean useReadWriteLock = tagStore != null && tagStore.isInPlaceTagUpdateSupported();

        LOG.info("Policy-Engine will" + (useReadWriteLock ? " " : " not ") + "use read-write locking to update tags in place when tag-deltas are provided");

        return new RangerReadWriteLock(useReadWriteLock);
    }

    @Override
    public void enrich(RangerAccessRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAdminTagEnricher.enrich(" + request + ")");
        }

        refreshTagsIfNeeded();
        super.enrich(request);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAdminTagEnricher.enrich(" + request + ")");
        }
    }

    private void refreshTagsIfNeeded() {

        final Long        enrichedServiceTagsVersion = getServiceTagsVersion();
        final Long        resourceTrieVersion        = getResourceTrieVersion();
        ServiceTags       serviceTags                = null;

        try {

            boolean needsBackwardCompatibility = !ADMIN_TAG_ENRICHER_SUPPORTS_TAG_DELTAS || enrichedServiceTagsVersion == -1L;

            XXServiceVersionInfo serviceVersionInfoDbObj = daoManager.getXXServiceVersionInfo().findByServiceName(serviceName);

            if (serviceVersionInfoDbObj == null) {
                LOG.warn("serviceVersionInfo does not exist. name=" + serviceName);
            }

            if (serviceVersionInfoDbObj == null || serviceVersionInfoDbObj.getTagVersion() == null || !enrichedServiceTagsVersion.equals(serviceVersionInfoDbObj.getTagVersion())) {
                serviceTags = RangerServiceTagsCache.getInstance().getServiceTags(serviceName, serviceId, enrichedServiceTagsVersion, needsBackwardCompatibility, tagStore);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Have the latest tag version already. Only need to check if it needs to be rebuilt");
                }
                if (!enrichedServiceTagsVersion.equals(resourceTrieVersion)) {
                    serviceTags = RangerServiceTagsCache.getInstance().getServiceTags(serviceName, serviceId, resourceTrieVersion, needsBackwardCompatibility, tagStore);
                }
            }

        } catch (Exception e) {
            LOG.error("Could not get cached service-tags, continue to use old ones..", e);
            serviceTags = null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Received serviceTags:[" + serviceTags + "]");
        }

        if (serviceTags != null) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("enrichedServiceTagsVersion=" + enrichedServiceTagsVersion + ", serviceTags-version=" + serviceTags.getTagVersion());
            }

            if (!enrichedServiceTagsVersion.equals(serviceTags.getTagVersion()) || !resourceTrieVersion.equals(serviceTags.getTagVersion())) {

                synchronized(this) {

                    if (serviceTags.getIsDelta()) {
                        // Avoid rebuilding service-tags - applyDelta may not work correctly if called twice
                        boolean rebuildOnlyIndex = true;
                        setServiceTags(serviceTags, rebuildOnlyIndex);
                    } else {
                        setServiceTags(serviceTags);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RangerAdminTagEnricher={serviceName=").append(serviceName).append(", ");
        sb.append("serviceId=").append(serviceId).append("}");
        return sb.toString();
    }
}
