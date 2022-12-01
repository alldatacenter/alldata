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

package org.apache.atlas.repository.graph;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.repository.RepositoryException;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.GraphDatabase;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides access to the AtlasGraph
 *
 */
@Configuration
public class AtlasGraphProvider implements IAtlasGraphProvider {

    private static volatile GraphDatabase<?,?> graphDb_;

    private static final Logger  LOG                              = LoggerFactory.getLogger(AtlasGraphProvider.class);
    private static final Integer MAX_RETRY_COUNT                  = getMaxRetryCount();
    private static final Long    RETRY_SLEEP_TIME_MS              = getRetrySleepTime();
    private static final String  GRAPH_REPOSITORY_MAX_RETRIES     = "atlas.graph.repository.max.retries";
    private static final String  GRAPH_REPOSITORY_RETRY_SLEEPTIME = "atlas.graph.repository.retry.sleeptime.ms";

    private static org.apache.commons.configuration.Configuration APPLICATION_PROPERTIES = null;

    public static <V, E> AtlasGraph<V, E> getGraphInstance() {
        GraphDatabase<?,?> db = getGraphDatabase();      
        AtlasGraph<?, ?> graph = db.getGraph();
        return (AtlasGraph<V, E>) graph;

    }

    private static <V, E> GraphDatabase<?,?> getGraphDatabase() {

        try {
            if (graphDb_ == null) {
                synchronized(AtlasGraphProvider.class) {
                    if(graphDb_ == null) {
                        Class implClass = AtlasRepositoryConfiguration.getGraphDatabaseImpl();
                        graphDb_ = (GraphDatabase<V, E>) implClass.newInstance();
                    }
                }
            }         
            return graphDb_;
        }
        catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error initializing graph database", e);
        }
    }

    public AtlasGraph getBulkLoading() {
        try {
            GraphDatabase<?, ?> graphDB = null;
            synchronized (AtlasGraphProvider.class) {
                Class implClass = AtlasRepositoryConfiguration.getGraphDatabaseImpl();
                graphDB = (GraphDatabase<?, ?>) implClass.newInstance();
            }

            return graphDB.getGraphBulkLoading();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error initializing graph database", e);
        }
    }

    @VisibleForTesting
    public static void cleanup() {
        getGraphDatabase().cleanup();
    }

    @Override
    @Bean(destroyMethod = "")
    public AtlasGraph get() throws RepositoryException{
        try {
            return getGraphInstance();
        } catch (Exception ex) {
            LOG.info("Failed to obtain graph instance, retrying " + MAX_RETRY_COUNT + " times, error: " + ex);

            return retry();
        }
    }

    private AtlasGraph retry() throws RepositoryException {
        int retryCounter = 0;

        while (retryCounter < MAX_RETRY_COUNT) {
            try {
                // Retry after 30 sec to get graph instance
                Thread.sleep(RETRY_SLEEP_TIME_MS);

                return getGraphInstance();
            } catch (Exception ex) {
                retryCounter++;

                LOG.warn("Failed to obtain graph instance on attempt " + retryCounter + " of " + MAX_RETRY_COUNT, ex);

                if (retryCounter >= MAX_RETRY_COUNT) {
                    LOG.info("Max retries exceeded.");
                    break;
                }
            }
        }

        throw new RepositoryException("Max retries exceeded. Failed to obtain graph instance after " + MAX_RETRY_COUNT + " retries");
    }

    private static Integer getMaxRetryCount() {
        initApplicationProperties();

        return (APPLICATION_PROPERTIES == null) ? 3 : APPLICATION_PROPERTIES.getInt(GRAPH_REPOSITORY_MAX_RETRIES, 3);
    }

    private static Long getRetrySleepTime() {
        initApplicationProperties();

        return (APPLICATION_PROPERTIES == null) ? 30000 : APPLICATION_PROPERTIES.getLong(GRAPH_REPOSITORY_RETRY_SLEEPTIME, 30000);
    }

    private static void initApplicationProperties() {
        if (APPLICATION_PROPERTIES == null) {
            try {
                APPLICATION_PROPERTIES = ApplicationProperties.get();
            } catch (AtlasException ex) {
                // ignore
            }
        }
    }
}
