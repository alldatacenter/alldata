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
package org.apache.atlas.graph;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.io.File;
import java.util.UUID;

public class GraphSandboxUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GraphSandboxUtil.class);

    public static void create(String sandboxName) {
        Configuration configuration;
        try {
            configuration = ApplicationProperties.get();
            configuration.setProperty("atlas.graph.storage.directory", getStorageDir(sandboxName, "storage"));
            configuration.setProperty("atlas.graph.index.search.directory", getStorageDir(sandboxName, "index"));


            LOG.debug("New Storage dir : {}", configuration.getProperty("atlas.graph.storage.directory"));
            LOG.debug("New Indexer dir : {}", configuration.getProperty("atlas.graph.index.search.directory"));
        } catch (AtlasException ignored) {
            throw new SkipException("Failure to setup Sandbox: " + sandboxName);
        }
    }

    private static String getStorageDir(String sandboxName, String directory) {
        return System.getProperty("atlas.data") +
                File.separatorChar + sandboxName +
                File.separatorChar + directory;
    }

    public static void create() {
        UUID uuid = UUID.randomUUID();
        create(uuid.toString());
    }

    public static boolean useLocalSolr() {
        boolean ret = false;
        
        try {
            Configuration conf     = ApplicationProperties.get();
            Object        property = conf.getProperty("atlas.graph.index.search.solr.embedded");

            if (property != null && property instanceof String) {
                ret = Boolean.valueOf((String) property);
            }
        } catch (AtlasException ignored) {
            throw new SkipException("useLocalSolr: failed! ", ignored);
        }

        return ret;
    }
}
