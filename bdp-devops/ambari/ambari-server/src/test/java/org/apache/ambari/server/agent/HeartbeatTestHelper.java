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
package org.apache.ambari.server.agent;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyClusterId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOSRelease;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOs;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyRepositoryVersion;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyStackId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HBASE;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.easymock.EasyMock;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;

import junit.framework.Assert;

@Singleton
public class HeartbeatTestHelper {

  @Inject
  Clusters clusters;

  @Inject
  Injector injector;

  @Inject
  AmbariMetaInfo metaInfo;

  @Inject
  ActionDBAccessor actionDBAccessor;

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  StackDAO stackDAO;

  @Inject
  UnitOfWork unitOfWork;

  @Inject
  ResourceTypeDAO resourceTypeDAO;

  @Inject
  OrmTestHelper helper;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private StageFactory stageFactory;

  public final static StackId HDP_22_STACK = new StackId("HDP", "2.2.0");

  public static InMemoryDefaultTestModule getTestModule() {
    return new InMemoryDefaultTestModule(){

      @Override
      protected void configure() {
        super.configure();
        binder().bind(STOMPUpdatePublisher.class).toInstance(EasyMock.createNiceMock(STOMPUpdatePublisher.class));
      }
    };
  }

  public HeartBeatHandler getHeartBeatHandler(ActionManager am)
      throws InvalidStateTransitionException, AmbariException {
    HeartBeatHandler handler = new HeartBeatHandler(clusters, am, Encryptor.NONE, injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);
    return handler;
  }

  public Cluster getDummyCluster()
      throws Exception {
    Map<String, String> configProperties = new HashMap<String, String>() {{
      put(RecoveryConfigHelper.RECOVERY_ENABLED_KEY, "true");
      put(RecoveryConfigHelper.RECOVERY_TYPE_KEY, "AUTO_START");
      put(RecoveryConfigHelper.RECOVERY_MAX_COUNT_KEY, "4");
      put(RecoveryConfigHelper.RECOVERY_LIFETIME_MAX_COUNT_KEY, "10");
      put(RecoveryConfigHelper.RECOVERY_WINDOW_IN_MIN_KEY, "23");
      put(RecoveryConfigHelper.RECOVERY_RETRY_GAP_KEY, "2");
    }};

    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};

    return getDummyCluster(DummyCluster, new Long(DummyClusterId), new StackId(DummyStackId), DummyRepositoryVersion,
        configProperties, hostNames);
  }

  public Cluster getDummyCluster(String clusterName, Long clusterId, StackId stackId, String repositoryVersion,
      Map<String, String> configProperties, Set<String> hostNames)
      throws Exception {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    org.junit.Assert.assertNotNull(stackEntity);

    // Create the cluster
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
    resourceTypeEntity.setName(ResourceType.CLUSTER.name());
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(clusterName);
    clusterEntity.setClusterId(clusterId);
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    // because this test method goes around the Clusters business object, we
    // forcefully will refresh the internal state so that any tests which
    // incorrect use Clusters after calling this won't be affected
    Clusters clusters = injector.getInstance(Clusters.class);
    Method method = ClustersImpl.class.getDeclaredMethod("safelyLoadClustersAndHosts");
    method.setAccessible(true);
    method.invoke(clusters);

    Cluster cluster = clusters.getCluster(clusterName);

    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(cluster, "cluster-env", "version1", configProperties, new HashMap<>());
    cluster.addDesiredConfig("user", Collections.singleton(config));


    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");

    List<HostEntity> hostEntities = new ArrayList<>();
    for(String hostName : hostNames) {
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);
      host.setHostAttributes(hostAttributes);

      HostEntity hostEntity = hostDAO.findByName(hostName);
      Assert.assertNotNull(hostEntity);
      hostEntities.add(hostEntity);
    }

    clusterEntity.setHostEntities(hostEntities);
    clusters.mapAndPublishHostsToCluster(hostNames, clusterName);

    return cluster;
  }

  public void populateActionDB(ActionDBAccessor db, String DummyHostname1, long requestId, long stageId) throws AmbariException {
    Stage s = stageFactory.createNew(requestId, "/a/b", DummyCluster, 1L, "heartbeat handler test",
        "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    String filename = null;
    s.addHostRoleExecutionCommand(DummyHostname1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            DummyHostname1, System.currentTimeMillis()), DummyCluster, HBASE, false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "clusterHostInfo", clusters);
    db.persistActions(request);
  }

}
