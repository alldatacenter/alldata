/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.hive.hook.utils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.hive.hook.HiveHook;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.apache.atlas.hive.hook.HiveHook.HOOK_HIVE_IGNORE_DDL_OPERATIONS;

public class ActiveEntityFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveEntityFilter.class);

    private static EntityFilter entityFilter;

    public static void init(Configuration configuration) {
        boolean skipDdlOperations = configuration.getBoolean(HOOK_HIVE_IGNORE_DDL_OPERATIONS, false);
        List<String> additionalTypesToRetain = getConfiguredTypesToRetainForDDLEntityFilter(configuration);

        init(skipDdlOperations, additionalTypesToRetain);
        LOG.info("atlas.hook.hive.ignore.ddl.operations={} - {}", skipDdlOperations, entityFilter.getClass().getSimpleName());
    }

    @VisibleForTesting
    static void init(boolean lineageOnlyFilter, List<String> additionalTypesToRetain) {
        entityFilter = lineageOnlyFilter ? new HiveDDLEntityFilter(additionalTypesToRetain) : new PassthroughFilter();
    }

    public static List<HookNotification> apply(List<HookNotification> incoming) {
        return entityFilter.apply(incoming);
    }

    private static List<String> getConfiguredTypesToRetainForDDLEntityFilter(Configuration configuration) {
        try {
            if (configuration.containsKey(HiveHook.HOOK_HIVE_FILTER_ENTITY_ADDITIONAL_TYPES_TO_RETAIN)) {
                String[] configuredTypes = configuration.getStringArray(HiveHook.HOOK_HIVE_FILTER_ENTITY_ADDITIONAL_TYPES_TO_RETAIN);

                return Arrays.asList(configuredTypes);
            }
        } catch (Exception e) {
            LOG.error("Failed to load application properties", e);
        }

        return null;
    }
}
