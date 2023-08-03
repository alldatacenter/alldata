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

package org.apache.ranger.plugin.contextenricher;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.service.RangerAuthContext;
import org.apache.ranger.plugin.util.DownloaderTask;
import org.apache.ranger.plugin.util.DownloadTrigger;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import java.io.File;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerUserStoreEnricher extends RangerAbstractContextEnricher {
    private static final Logger LOG                    = LoggerFactory.getLogger(RangerUserStoreEnricher.class);
    private static final Logger PERF_SET_USERSTORE_LOG = RangerPerfTracer.getPerfLogger("userstoreenricher.setuserstore");

    public static final String USERSTORE_REFRESHER_POLLINGINTERVAL_OPTION = "userStoreRefresherPollingInterval";
    public static final String USERSTORE_RETRIEVER_CLASSNAME_OPTION       = "userStoreRetrieverClassName";

    private       RangerUserStoreRefresher       userStoreRefresher;
    private       RangerUserStoreRetriever       userStoreRetriever;
    private       RangerUserStore                rangerUserStore;
    private       boolean                        disableCacheIfServiceNotFound = true;
    private       boolean                        dedupStrings                  = true;
    private final BlockingQueue<DownloadTrigger> userStoreDownloadQueue = new LinkedBlockingQueue<>();
    private       Timer                          userStoreDownloadTimer;

    @Override
    public void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerUserStoreEnricher.init()");
        }

        super.init();

        String propertyPrefix              = getPropertyPrefix();
        String userStoreRetrieverClassName = getOption(USERSTORE_RETRIEVER_CLASSNAME_OPTION);
        long   pollingIntervalMs           = getLongOption(USERSTORE_REFRESHER_POLLINGINTERVAL_OPTION, 3600 * 1000);

        dedupStrings = getBooleanConfig(propertyPrefix + ".dedup.strings", true);

        if (StringUtils.isNotBlank(userStoreRetrieverClassName)) {

            try {
                @SuppressWarnings("unchecked")
                Class<RangerUserStoreRetriever> userStoreRetriverClass = (Class<RangerUserStoreRetriever>) Class.forName(userStoreRetrieverClassName);

                userStoreRetriever = userStoreRetriverClass.newInstance();

            } catch (ClassNotFoundException exception) {
                LOG.error("Class " + userStoreRetrieverClassName + " not found, exception=" + exception);
            } catch (ClassCastException exception) {
                LOG.error("Class " + userStoreRetrieverClassName + " is not a type of RangerUserStoreRetriever, exception=" + exception);
            } catch (IllegalAccessException exception) {
                LOG.error("Class " + userStoreRetrieverClassName + " illegally accessed, exception=" + exception);
            } catch (InstantiationException exception) {
                LOG.error("Class " + userStoreRetrieverClassName + " could not be instantiated, exception=" + exception);
            }

            if (userStoreRetriever != null) {
                disableCacheIfServiceNotFound = getBooleanConfig(propertyPrefix + ".disable.cache.if.servicenotfound", true);
                String cacheDir      = getConfig(propertyPrefix + ".policy.cache.dir", null);
                String cacheFilename = String.format("%s_%s_userstore.json", appId, serviceName);

                cacheFilename = cacheFilename.replace(File.separatorChar,  '_');
                cacheFilename = cacheFilename.replace(File.pathSeparatorChar,  '_');

                String cacheFile = cacheDir == null ? null : (cacheDir + File.separator + cacheFilename);

                userStoreRetriever.setServiceName(serviceName);
                userStoreRetriever.setServiceDef(serviceDef);
                userStoreRetriever.setAppId(appId);
                userStoreRetriever.setPluginConfig(getPluginConfig());
                userStoreRetriever.setPluginContext(getPluginContext());
                userStoreRetriever.init(enricherDef.getEnricherOptions());

                userStoreRefresher = new RangerUserStoreRefresher(userStoreRetriever, this, null, -1L, userStoreDownloadQueue, cacheFile);
                LOG.info("Created Thread(RangerUserStoreRefresher(" + getName() + ")");

                try {
                    userStoreRefresher.populateUserStoreInfo();
                } catch (Throwable exception) {
                    LOG.error("Exception when retrieving userstore information for this enricher", exception);
                }

                userStoreRefresher.setDaemon(true);
                userStoreRefresher.startRefresher();

                userStoreDownloadTimer = new Timer("userStoreDownloadTimer", true);

                try {
                    userStoreDownloadTimer.schedule(new DownloaderTask(userStoreDownloadQueue), pollingIntervalMs, pollingIntervalMs);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Scheduled userStoreDownloadRefresher to download userstore every " + pollingIntervalMs + " milliseconds");
                    }
                } catch (IllegalStateException exception) {
                    LOG.error("Error scheduling userStoreDownloadTimer:", exception);
                    LOG.error("*** UserStore information will NOT be downloaded every " + pollingIntervalMs + " milliseconds ***");
                    userStoreDownloadTimer = null;
                }
            }
        } else {
            LOG.error("No value specified for " + USERSTORE_RETRIEVER_CLASSNAME_OPTION + " in the RangerUserStoreEnricher options");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerUserStoreEnricher.init()");
        }
    }

    @Override
    public void enrich(RangerAccessRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerUserStoreEnricher.enrich(" + request + ")");
        }

        enrich(request, null);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerUserStoreEnricher.enrich(" + request + ")");
        }
    }

    @Override
    public void enrich(RangerAccessRequest request, Object dataStore) {

        // Unused by Solr plugin as document level authorization gets RangerUserStore from AuthContext
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerUserStoreEnricher.enrich(" + request + ") with dataStore:[" + dataStore + "]");
        }
        final RangerUserStore rangerUserStore;

        if (dataStore instanceof RangerUserStore) {
            rangerUserStore = (RangerUserStore) dataStore;
        } else {
            rangerUserStore = this.rangerUserStore;

            if (dataStore != null) {
                LOG.warn("Incorrect type of dataStore :[" + dataStore.getClass().getName() + "], falling back to original enrich");
            }
        }

        RangerAccessRequestUtil.setRequestUserStoreInContext(request.getContext(), rangerUserStore);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerUserStoreEnricher.enrich(" + request + ") with dataStore:[" + dataStore + "])");
        }
    }

    public boolean isDisableCacheIfServiceNotFound() {
        return disableCacheIfServiceNotFound;
    }

    public RangerUserStore getRangerUserStore() {return this.rangerUserStore;}

    public void setRangerUserStore(final RangerUserStore rangerUserStore) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerUserStoreEnricher.setRangerUserStore(rangerUserStore=" + rangerUserStore + ")");
        }

        if (rangerUserStore == null) {
            LOG.info("UserStore information is null for service " + serviceName);
            this.rangerUserStore = null;
        } else  {
            RangerPerfTracer perf = null;

            if(RangerPerfTracer.isPerfTraceEnabled(PERF_SET_USERSTORE_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_SET_USERSTORE_LOG, "RangerUserStoreEnricher.setRangerUserStore(newUserStoreVersion=" + rangerUserStore.getUserStoreVersion() + ")");
            }

            if (dedupStrings) {
                rangerUserStore.dedupStrings();
            }

            this.rangerUserStore = rangerUserStore;

            RangerPerfTracer.logAlways(perf);
        }

        setRangerUserStoreInPlugin();
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerUserStoreEnricher.setRangerUserStore(rangerUserStore=" + rangerUserStore + ")");
        }

    }

    public Long getUserStoreVersion() {
        RangerUserStore localUserStore = this.rangerUserStore;

        return localUserStore != null ? localUserStore.getUserStoreVersion() : null;
    }

    @Override
    public boolean preCleanup() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerUserStoreEnricher.preCleanup()");
        }

        super.preCleanup();

        if (userStoreDownloadTimer != null) {
            userStoreDownloadTimer.cancel();
            userStoreDownloadTimer = null;
        }

        if (userStoreRefresher != null) {
            userStoreRefresher.cleanup();
            userStoreRefresher = null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerUserStoreEnricher.preCleanup() : result=" + true);
        }
        return true;
    }

    private void setRangerUserStoreInPlugin() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> setRangerUserStoreInPlugin()");
        }

        RangerAuthContext authContext = getAuthContext();

        if (authContext != null) {
            authContext.addOrReplaceRequestContextEnricher(this, rangerUserStore);

            notifyAuthContextChanged();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== setRangerUserStoreInPlugin()");
        }
    }

}
