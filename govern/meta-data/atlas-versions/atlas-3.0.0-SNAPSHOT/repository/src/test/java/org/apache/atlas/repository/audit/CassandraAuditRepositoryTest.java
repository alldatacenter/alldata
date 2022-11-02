/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.audit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.atlas.AtlasException;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CassandraAuditRepositoryTest extends AuditRepositoryTestBase {
    private static final int MAX_RETRIES    = 9;
    private final String CLUSTER_HOST       = "localhost";
    private final String CLUSTER_NAME_TEST  = "Test Cluster";
    private final int CLUSTER_PORT          = 9042;

    @BeforeClass
    public void setup() {
        try {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra_test.yml");
            eventRepository = new CassandraBasedAuditRepository();
            Configuration atlasConf = new MapConfiguration(getClusterProperties());
            ((CassandraBasedAuditRepository) eventRepository).setApplicationProperties(atlasConf);
            ((CassandraBasedAuditRepository) eventRepository).start();

            ensureClusterCreation();
        }
        catch (Exception ex) {
            throw new SkipException("setup: failed!", ex);
        }
    }

    private Map<String, Object> getClusterProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(CassandraBasedAuditRepository.MANAGE_EMBEDDED_CASSANDRA, Boolean.TRUE);
        props.put(CassandraBasedAuditRepository.CASSANDRA_CLUSTERNAME_PROPERTY, CLUSTER_NAME_TEST);
        props.put(CassandraBasedAuditRepository.CASSANDRA_HOSTNAME_PROPERTY, CLUSTER_HOST);
        props.put(CassandraBasedAuditRepository.CASSANDRA_PORT_PROPERTY, CLUSTER_PORT);
        return props;
    }

    private void ensureClusterCreation() throws InterruptedException {
        // Retry the connection until we either connect or timeout
        Cluster.Builder cassandraClusterBuilder = Cluster.builder();
        Cluster cluster =
                cassandraClusterBuilder.addContactPoint(CLUSTER_HOST).withClusterName(CLUSTER_NAME_TEST).withPort(CLUSTER_PORT)
                        .build();
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                Session cassSession = cluster.connect();
                if (cassSession.getState().getConnectedHosts().size() > 0) {
                    cassSession.close();
                    return;
                }
            } catch (Exception e) {
                Thread.sleep(1000);
            }
            retryCount++;
        }

        throw new SkipException("Unable to connect to embedded Cassandra after " + MAX_RETRIES + " seconds.");
    }
}
