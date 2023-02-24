/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.patches;

import org.apache.atlas.AtlasException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.service.Service;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Order(3)
public class AtlasPatchService implements Service, ActiveStateChangeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPatchService.class);

    private final Configuration     configuration;
    private final AtlasPatchManager patchManager;

    @Inject
    public AtlasPatchService(Configuration configuration, AtlasPatchManager patchManager) {
        this.configuration = configuration;
        this.patchManager  = patchManager;
    }

    @Override
    public void start() throws AtlasException {
        LOG.info("==> AtlasPatchService.start()");

        if (!HAConfiguration.isHAEnabled(configuration)) {
            startInternal();
        } else {
            LOG.info("AtlasPatchService.start(): deferring patches until instance activation");
        }

        LOG.info("<== AtlasPatchService.start()");
    }

    @Override
    public void stop() {
        LOG.info("AtlasPatchService.stop(): stopped");
    }

    @Override
    public void instanceIsActive() {
        LOG.info("==> AtlasPatchService.instanceIsActive()");

        startInternal();

        LOG.info("<== AtlasPatchService.instanceIsActive()");
    }

    @Override
    public void instanceIsPassive() {
        LOG.info("AtlasPatchService.instanceIsPassive(): no action needed");
    }

    @Override
    public int getHandlerOrder() {
        return HandlerOrder.ATLAS_PATCH_SERVICE.getOrder();
    }

    void startInternal() {
        try {
            LOG.info("AtlasPatchService: applying patches...");

            patchManager.applyAll();
        } catch (Exception ex) {
            LOG.error("AtlasPatchService: failed in applying patches", ex);
        }
    }
}
