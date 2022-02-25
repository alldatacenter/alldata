/*
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

package org.apache.ambari.funtest.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.ambari.funtest.server.ConnectionParams;
import org.apache.ambari.funtest.server.WebResponse;
import org.apache.ambari.funtest.server.api.cluster.DeleteClusterWebRequest;
import org.apache.ambari.funtest.server.api.service.DeleteServiceWebRequest;
import org.apache.ambari.funtest.server.api.service.GetServiceWebRequest;
import org.apache.ambari.funtest.server.api.service.StopServiceWebRequest;
import org.apache.ambari.funtest.server.utils.ClusterUtils;
import org.apache.ambari.funtest.server.utils.RestApiUtils;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;
import org.apache.ambari.server.state.State;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;



/**
 * Simple test that starts the local ambari server,
 * tests it's status and shuts down the server.
 */
@Ignore
public class DeleteServiceTest extends ServerTestBase {

    private static Log LOG = LogFactory.getLog(DeleteServiceTest.class);

    /**
     * Set up a test cluster with a service, a host and a few components.
     * Attempt to delete the service. Verify the state of the DB.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteService() throws Exception {
        String clusterName = "c1";
        String serviceName = "HDFS";
        ConnectionParams params = new ConnectionParams();

        params.setServerName("localhost");
        params.setServerApiPort(serverPort);
        params.setServerAgentPort(serverAgentPort);
        params.setUserName("admin");
        params.setPassword("admin");

        ClusterUtils clusterUtils = injector.getInstance(ClusterUtils.class);
        clusterUtils.createSampleCluster(params);

        /**
         * Verify the status of the service
         */
        JsonElement jsonResponse = RestApiUtils.executeRequest(new GetServiceWebRequest(params, clusterName, serviceName));
        assertTrue(!jsonResponse.isJsonNull());
        JsonObject jsonServiceInfoObj = jsonResponse.getAsJsonObject().get("ServiceInfo").getAsJsonObject();
        String cluster_name = jsonServiceInfoObj.get("cluster_name").getAsString();
        assertEquals(cluster_name, clusterName);

        String service_name = jsonServiceInfoObj.get("service_name").getAsString();
        assertEquals(service_name, serviceName);

        /**
         * Check the following:
         * ClusterServiceDAO
         * ServiceDesiredStateDAO
         * ServiceComponentDesiredStateDAO
         * HostComponentStateDAO
         * HostComponentDesiredStateDAO
         */

        /**
         * Stop the service
         */

        jsonResponse = RestApiUtils.executeRequest(new StopServiceWebRequest(params, clusterName, serviceName));

        /**
         * clusterservice table
         */
        ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
        List<ClusterServiceEntity> clusterServiceEntities = clusterServiceDAO.findAll();
        assertEquals(clusterServiceEntities.size(), 1); // Only one service in the sample cluster (HDFS)
        assertEquals(clusterServiceEntities.get(0).getServiceName(), serviceName); // Verify the only service name

        ClusterServiceEntity clusterServiceEntity = clusterServiceEntities.get(0);
        long clusterId = clusterServiceEntity.getClusterId();

        /**
         * servicedesiredstate table
         */
        ServiceDesiredStateDAO serviceDesiredStateDAO = injector.getInstance(ServiceDesiredStateDAO.class);
        List<ServiceDesiredStateEntity> serviceDesiredStateEntities = serviceDesiredStateDAO.findAll();
        assertEquals(serviceDesiredStateEntities.size(), 1);
        ServiceDesiredStateEntity serviceDesiredStateEntity = serviceDesiredStateEntities.get(0);
        assertEquals(serviceDesiredStateEntity.getServiceName(), serviceName);
        assertEquals(serviceDesiredStateEntity.getDesiredState(), State.INSTALLED);

        /**
         * servicecomponentdesiredstate table
         */
        ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
        List<ServiceComponentDesiredStateEntity>  serviceComponentDesiredStateEntities =  serviceComponentDesiredStateDAO.findAll();
        assertEquals(serviceComponentDesiredStateEntities.size(), 3); // NAMENODE, SECONDARY_NAMENODE, DATANODE.
        for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity : serviceComponentDesiredStateEntities) {
            assertEquals(serviceComponentDesiredStateEntity.getDesiredState(), State.INSTALLED);
        }

        /**
         * hostcomponentstate table
         */
        HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
        List<HostComponentStateEntity> hostComponentStateEntities = hostComponentStateDAO.findAll();
        assertEquals(hostComponentStateEntities.size(), 3);

        /**
         * hostcomponentdesiredstate table
         */
        HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
        List<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities = hostComponentDesiredStateDAO.findAll();
        assertEquals(hostComponentDesiredStateEntities.size(), 3);

        /**
         * Delete the service
         */
        jsonResponse = RestApiUtils.executeRequest(new DeleteServiceWebRequest(params, clusterName, serviceName));

        WebResponse webResponse = new GetServiceWebRequest(params, clusterName, serviceName).getResponse();
        assertEquals(webResponse.getStatusCode(), HttpStatus.SC_NOT_FOUND);

        /**
         * ClusterServiceDAO - the service entry should have been removed.
         */
        clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);
        assertTrue(clusterServiceEntity == null);

        /**
         * ServiceDesiredStateDAO - the service entry should have been removed.
         */
        ServiceDesiredStateEntityPK serviceDesiredStateEntityPK = injector.getInstance(ServiceDesiredStateEntityPK.class);
        serviceDesiredStateEntityPK.setClusterId(clusterId);
        serviceDesiredStateEntityPK.setServiceName(serviceName);
        serviceDesiredStateEntity =  serviceDesiredStateDAO.findByPK(serviceDesiredStateEntityPK);
        assertTrue(serviceDesiredStateEntity == null);

        /**
         * ServiceComponentDesiredStateDAO
         */
        ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findById(0L);
        assertTrue(serviceComponentDesiredStateEntity == null);

        /**
         * HostComponentStateDAO
         */
        hostComponentStateEntities = hostComponentStateDAO.findByService(serviceName);
        assertEquals(hostComponentStateEntities.size(), 0);


        /**
         * HostComponentDesiredStateDAO
         */
        hostComponentDesiredStateEntities = hostComponentDesiredStateDAO.findAll();
        assertEquals(hostComponentDesiredStateEntities.size(), 0);

        jsonResponse = RestApiUtils.executeRequest(new DeleteClusterWebRequest(params, clusterName));

        LOG.info(jsonResponse);
    }
}