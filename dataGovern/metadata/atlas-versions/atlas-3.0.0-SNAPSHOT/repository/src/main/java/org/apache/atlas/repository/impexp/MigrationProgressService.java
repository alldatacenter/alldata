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

package org.apache.atlas.repository.impexp;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graphdb.GraphDBMigrator;
import org.apache.atlas.repository.migration.DataMigrationStatusService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.apache.atlas.AtlasConstants.ATLAS_MIGRATION_MODE_FILENAME;

@AtlasService
@Singleton
public class MigrationProgressService {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationProgressService.class);
    private static final String FILE_EXTENSION_ZIP = ".zip";

    public static final String MIGRATION_QUERY_CACHE_TTL = "atlas.migration.query.cache.ttlInSecs";

    @VisibleForTesting
    static long DEFAULT_CACHE_TTL_IN_SECS = 120 * 1000; // 30 secs

    private final long            cacheValidity;
    private final GraphDBMigrator migrator;
    private       MigrationStatus cachedStatus;
    private       long            cacheExpirationTime = 0;
    private DataMigrationStatusService dataMigrationStatusService;
    private boolean zipFileBasedMigrationImport;

    @Inject
    public MigrationProgressService(Configuration configuration, GraphDBMigrator migrator) {
        this.migrator      = migrator;
        this.cacheValidity = (configuration != null) ? configuration.getLong(MIGRATION_QUERY_CACHE_TTL, DEFAULT_CACHE_TTL_IN_SECS) : DEFAULT_CACHE_TTL_IN_SECS;

        this.zipFileBasedMigrationImport = isZipFileBasedMigrationEnabled();
        initConditionallyZipFileBasedMigrator();
    }

    private void initConditionallyZipFileBasedMigrator() {
        if (!zipFileBasedMigrationImport) {
            return;
        }

        dataMigrationStatusService = new DataMigrationStatusService(AtlasGraphProvider.getGraphInstance());
        dataMigrationStatusService.init(getFileNameFromMigrationProperty());
    }

    private boolean isZipFileBasedMigrationEnabled() {
        return StringUtils.endsWithIgnoreCase(getFileNameFromMigrationProperty(), FILE_EXTENSION_ZIP);
    }

    public MigrationStatus getStatus() {
        return fetchStatus();
    }

    private MigrationStatus fetchStatus() {
        long currentTime = System.currentTimeMillis();

        if(resetCache(currentTime)) {
            if (this.zipFileBasedMigrationImport) {
                cachedStatus = dataMigrationStatusService.getStatus();
            } else {
                cachedStatus = migrator.getMigrationStatus();
            }
        }

        return cachedStatus;
    }

    private boolean resetCache(long currentTime) {
        boolean ret = cachedStatus == null || currentTime > cacheExpirationTime;

        if(ret) {
            cacheExpirationTime = currentTime + cacheValidity;
        }

        return ret;
    }

    public String getFileNameFromMigrationProperty() {
        try {
            return ApplicationProperties.get().getString(ATLAS_MIGRATION_MODE_FILENAME, StringUtils.EMPTY);
        } catch (AtlasException e) {
            return StringUtils.EMPTY;
        }
    }
}
