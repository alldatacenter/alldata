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

package org.apache.ambari.server.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.UserName;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class OrmTestHelper {

  private static final Logger LOG = LoggerFactory.getLogger(OrmTestHelper.class);

  private AtomicInteger uniqueCounter = new AtomicInteger();

  @Inject
  public Provider<EntityManager> entityManagerProvider;

  @Inject
  public Injector injector;

  @Inject
  public UserDAO userDAO;

  @Inject
  public AlertDefinitionDAO alertDefinitionDAO;

  @Inject
  public AlertDispatchDAO alertDispatchDAO;

  @Inject
  public AlertsDAO alertsDAO;

  @Inject
  public RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  public HostVersionDAO hostVersionDAO;

  @Inject
  public HostDAO hostDAO;

  @Inject
  private StackDAO stackDAO;

  public static final String CLUSTER_NAME = "test_cluster1";

  public EntityManager getEntityManager() {
    return entityManagerProvider.get();
  }

  /**
   * creates some test data
   */
  @Transactional
  public void createDefaultData() {
    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    ResourceTypeEntity resourceTypeEntity =  new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
    resourceTypeEntity.setName(ResourceType.CLUSTER.name());

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setDesiredStack(stackEntity);

    HostEntity host1 = new HostEntity();
    HostEntity host2 = new HostEntity();
    HostEntity host3 = new HostEntity();

    host1.setHostName("test_host1");
    host2.setHostName("test_host2");
    host3.setHostName("test_host3");
    host1.setIpv4("192.168.0.1");
    host2.setIpv4("192.168.0.2");
    host3.setIpv4("192.168.0.3");

    List<HostEntity> hostEntities = new ArrayList<>();
    hostEntities.add(host1);
    hostEntities.add(host2);

    clusterEntity.setHostEntities(hostEntities);

    // both sides of relation should be set when modifying in runtime
    host1.setClusterEntities(Arrays.asList(clusterEntity));
    host2.setClusterEntities(Arrays.asList(clusterEntity));

    HostStateEntity hostStateEntity1 = new HostStateEntity();
    hostStateEntity1.setCurrentState(HostState.HEARTBEAT_LOST);
    hostStateEntity1.setHostEntity(host1);
    HostStateEntity hostStateEntity2 = new HostStateEntity();
    hostStateEntity2.setCurrentState(HostState.HEALTHY);
    hostStateEntity2.setHostEntity(host2);
    host1.setHostStateEntity(hostStateEntity1);
    host2.setHostStateEntity(hostStateEntity2);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setServiceName("HDFS");
    clusterServiceEntity.setClusterEntity(clusterEntity);
    List<ClusterServiceEntity> clusterServiceEntities = new ArrayList<>();
    clusterServiceEntities.add(clusterServiceEntity);
    clusterEntity.setClusterServiceEntities(clusterServiceEntities);

    getEntityManager().persist(host1);
    getEntityManager().persist(host2);
    getEntityManager().persist(resourceTypeEntity);
    getEntityManager().persist(resourceEntity);
    getEntityManager().persist(clusterEntity);
    getEntityManager().persist(hostStateEntity1);
    getEntityManager().persist(hostStateEntity2);
    getEntityManager().persist(clusterServiceEntity);
  }

  @Transactional
  public void createTestUsers() {
    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setName(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME);
    getEntityManager().persist(principalTypeEntity);

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);

    getEntityManager().persist(principalEntity);

    PasswordEncoder encoder = injector.getInstance(PasswordEncoder.class);

    UserEntity admin = new UserEntity();
    admin.setUserName(UserName.fromString("administrator").toString());
    admin.setPrincipal(principalEntity);

    Set<UserEntity> users = new HashSet<>();

    users.add(admin);

    userDAO.create(admin);

    principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    getEntityManager().persist(principalEntity);

    UserEntity userWithoutRoles = new UserEntity();
    userWithoutRoles.setUserName(UserName.fromString("userWithoutRoles").toString());
    userWithoutRoles.setPrincipal(principalEntity);
    userDAO.create(userWithoutRoles);
  }

  @Transactional
  public void performTransactionMarkedForRollback() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterDAO.removeByName("test_cluster1");
    getEntityManager().getTransaction().setRollbackOnly();
  }

  @Transactional
  public void createStageCommands() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(1L);
    requestEntity.setClusterId(clusterDAO.findByName("test_cluster1").getClusterId());

    StageEntity stageEntity = new StageEntity();
    stageEntity.setRequest(requestEntity);
    stageEntity.setClusterId(clusterDAO.findByName("test_cluster1").getClusterId());
    stageEntity.setRequestId(1L);
    stageEntity.setStageId(1L);

    requestEntity.setStages(Collections.singletonList(stageEntity));

    HostRoleCommandEntity commandEntity = new HostRoleCommandEntity();
    HostRoleCommandEntity commandEntity2 = new HostRoleCommandEntity();
    HostRoleCommandEntity commandEntity3 = new HostRoleCommandEntity();
    HostEntity host1 = hostDAO.findByName("test_host1");
    HostEntity host2 = hostDAO.findByName("test_host2");
    commandEntity.setHostEntity(host1);
    host1.getHostRoleCommandEntities().add(commandEntity);
    commandEntity.setRoleCommand(RoleCommand.INSTALL);
    commandEntity.setStatus(HostRoleStatus.QUEUED);
    commandEntity.setRole(Role.DATANODE);
    commandEntity2.setHostEntity(host2);
    host2.getHostRoleCommandEntities().add(commandEntity2);
    commandEntity2.setRoleCommand(RoleCommand.EXECUTE);
    commandEntity2.setRole(Role.NAMENODE);
    commandEntity2.setStatus(HostRoleStatus.COMPLETED);
    commandEntity3.setHostEntity(host1);
    host1.getHostRoleCommandEntities().add(commandEntity3);
    commandEntity3.setRoleCommand(RoleCommand.START);
    commandEntity3.setRole(Role.SECONDARY_NAMENODE);
    commandEntity3.setStatus(HostRoleStatus.IN_PROGRESS);
    commandEntity.setStage(stageEntity);
    commandEntity2.setStage(stageEntity);
    commandEntity3.setStage(stageEntity);

    stageEntity.setHostRoleCommands(new ArrayList<>());
    stageEntity.getHostRoleCommands().add(commandEntity);
    stageEntity.getHostRoleCommands().add(commandEntity2);
    stageEntity.getHostRoleCommands().add(commandEntity3);

    requestDAO.create(requestEntity);
    stageDAO.create(stageEntity);
    hostRoleCommandDAO.create(commandEntity3);
    hostRoleCommandDAO.create(commandEntity);
    hostRoleCommandDAO.create(commandEntity2);
    hostDAO.merge(host1);
    hostDAO.merge(host2);
  }

  @Transactional
  public StackEntity createStack(StackId stackId) throws AmbariException {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    if (null == stackEntity) {
      stackEntity = new StackEntity();
      stackEntity.setStackName(stackId.getStackName());
      stackEntity.setStackVersion(stackId.getStackVersion());
      stackDAO.create(stackEntity);
    }

    return stackEntity;
  }

  /**
   * Creates an empty cluster with an ID.
   *
   * @return the cluster ID.
   */
  @Transactional
  public Long createCluster() throws Exception {
    return createCluster(CLUSTER_NAME);
  }

  /**
   * Creates an empty cluster with an ID using the specified cluster name.
   *
   * @return the cluster ID.
   */
  @Transactional
  public Long createCluster(String clusterName) throws Exception {
    // required to populate the database with stacks
    injector.getInstance(AmbariMetaInfo.class);

    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);

    ResourceTypeEntity resourceTypeEntity =  new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
    resourceTypeEntity.setName(ResourceType.CLUSTER.name());
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    StackDAO stackDAO = injector.getInstance(StackDAO.class);

    StackEntity stackEntity = stackDAO.find("HDP", "2.0.6");
    assertNotNull(stackEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(clusterName);
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    ClusterStateEntity clusterStateEntity = new ClusterStateEntity();
    clusterStateEntity.setCurrentStack(stackEntity);
    clusterStateEntity.setClusterEntity(clusterEntity);
    getEntityManager().persist(clusterStateEntity);

    clusterEntity = clusterDAO.findByName(clusterEntity.getClusterName());
    assertNotNull(clusterEntity);
    assertTrue(clusterEntity.getClusterId() > 0);

    clusterEntity.setClusterStateEntity(clusterStateEntity);
    clusterDAO.merge(clusterEntity);

    // because this test method goes around the Clusters business object, we
    // forcefully will refresh the internal state so that any tests which
    // incorrect use Clusters after calling this won't be affected
    Clusters clusters = injector.getInstance(Clusters.class);
    Method method = ClustersImpl.class.getDeclaredMethod("loadClustersAndHosts");
    method.setAccessible(true);
    method.invoke(clusters);

    return clusterEntity.getClusterId();
  }

  public Cluster buildNewCluster(Clusters clusters,
      ServiceFactory serviceFactory, ServiceComponentFactory componentFactory,
      ServiceComponentHostFactory schFactory, String hostName) throws Exception {
    String clusterName = "cluster-" + System.currentTimeMillis();
    StackId stackId = new StackId("HDP", "2.0.6");

    createStack(stackId);

    clusters.addCluster(clusterName, stackId);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster = initializeClusterWithStack(cluster);

    addHost(clusters, cluster, hostName);

    installHdfsService(cluster, serviceFactory, componentFactory, schFactory, hostName);
    installYarnService(cluster, serviceFactory, componentFactory, schFactory,
        hostName);
    return cluster;
  }

  public Cluster initializeClusterWithStack(Cluster cluster) throws Exception {
    StackId stackId = new StackId("HDP", "2.0.6");
    cluster.setDesiredStackVersion(stackId);
    getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    return cluster;
  }

  /**
   * @throws Exception
   */
  public void addHost(Clusters clusters, Cluster cluster, String hostName)
      throws Exception {
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);

    clusters.mapAndPublishHostsToCluster(Collections.singleton(hostName), cluster.getClusterName());
  }

  public void addHostComponent(Cluster cluster, String hostName, String serviceName, String componentName) throws AmbariException {
    Service service = cluster.getService(serviceName);
    ServiceComponent serviceComponent = service.getServiceComponent(componentName);
    ServiceComponentHost serviceComponentHost = serviceComponent.addServiceComponentHost(hostName);
    serviceComponentHost.setDesiredState(State.INSTALLED);
  }

  public void installHdfsService(Cluster cluster,
      ServiceFactory serviceFactory, ServiceComponentFactory componentFactory,
      ServiceComponentHostFactory schFactory, String hostName) throws Exception {

    RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(cluster.getDesiredStackVersion(),
        cluster.getDesiredStackVersion().getStackVersion());

    String serviceName = "HDFS";
    Service service = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    assertNotNull(cluster.getService(serviceName));

    ServiceComponent datanode = componentFactory.createNew(service, "DATANODE");

    service.addServiceComponent(datanode);
    datanode.setDesiredState(State.INSTALLED);

    ServiceComponentHost sch = schFactory.createNew(datanode, hostName);

    datanode.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);

    ServiceComponent namenode = componentFactory.createNew(service, "NAMENODE");

    service.addServiceComponent(namenode);
    namenode.setDesiredState(State.INSTALLED);

    sch = schFactory.createNew(namenode, hostName);
    namenode.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
  }

  public void installYarnService(Cluster cluster,
      ServiceFactory serviceFactory, ServiceComponentFactory componentFactory,
      ServiceComponentHostFactory schFactory, String hostName) throws Exception {

    RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(cluster.getDesiredStackVersion(),
        cluster.getDesiredStackVersion().getStackVersion());

    String serviceName = "YARN";
    Service service = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    assertNotNull(cluster.getService(serviceName));

    ServiceComponent resourceManager = componentFactory.createNew(service,
        "RESOURCEMANAGER");

    service.addServiceComponent(resourceManager);
    resourceManager.setDesiredState(State.INSTALLED);

    ServiceComponentHost sch = schFactory.createNew(resourceManager, hostName);

    resourceManager.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
  }

  /**
   * Creates an alert target.
   *
   * @return
   */
  public AlertTargetEntity createAlertTarget() throws Exception {
    AlertTargetEntity target = new AlertTargetEntity();
    target.setDescription("Target Description");
    target.setNotificationType("EMAIL");
    target.setProperties("Target Properties");
    target.setTargetName("Target Name " + System.currentTimeMillis());

    alertDispatchDAO.create(target);
    return target;
  }

  /**
   * Creates a global alert target.
   *
   * @return
   */
  public AlertTargetEntity createGlobalAlertTarget() throws Exception {
    AlertTargetEntity target = new AlertTargetEntity();
    target.setDescription("Target Description");
    target.setNotificationType("EMAIL");
    target.setProperties("Target Properties");
    target.setTargetName("Target Name " + System.currentTimeMillis());
    target.setGlobal(true);

    alertDispatchDAO.create(target);
    return target;
  }

  /**
   * Creates an alert definition.
   *
   * @param clusterId
   * @return
   * @throws Exception
   */
  public AlertDefinitionEntity createAlertDefinition(long clusterId)
      throws Exception {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName("Alert Definition "
        + System.currentTimeMillis());
    definition.setServiceName("AMBARI");
    definition.setComponentName(null);
    definition.setClusterId(clusterId);
    definition.setHash(UUID.randomUUID().toString());
    definition.setScheduleInterval(60);
    definition.setScope(Scope.SERVICE);
    definition.setSource("{\"type\" : \"SCRIPT\"}");
    definition.setSourceType(SourceType.SCRIPT);

    alertDefinitionDAO.create(definition);
    return alertDefinitionDAO.findById(definition.getDefinitionId());
  }

  /**
   * Creates an alert group.
   *
   * @param clusterId
   * @param targets
   * @return
   * @throws Exception
   */
  public AlertGroupEntity createAlertGroup(long clusterId,
      Set<AlertTargetEntity> targets) throws Exception {
    AlertGroupEntity group = new AlertGroupEntity();
    group.setDefault(false);
    group.setGroupName("Group Name " + System.currentTimeMillis() + uniqueCounter.incrementAndGet());
    group.setClusterId(clusterId);
    group.setAlertTargets(targets);

    alertDispatchDAO.create(group);
    return group;
  }

  /**
   * Creates some default alert groups for various services used in the tests.
   *
   * @param clusterId
   * @return
   * @throws Exception
   */
  public List<AlertGroupEntity> createDefaultAlertGroups(long clusterId)
      throws Exception {
    AlertGroupEntity hdfsGroup = new AlertGroupEntity();
    hdfsGroup.setDefault(true);
    hdfsGroup.setClusterId(clusterId);
    hdfsGroup.setGroupName("HDFS");
    hdfsGroup.setServiceName("HDFS");

    AlertGroupEntity oozieGroup = new AlertGroupEntity();
    oozieGroup.setDefault(true);
    oozieGroup.setClusterId(clusterId);
    oozieGroup.setGroupName("OOZIE");
    oozieGroup.setServiceName("OOZIE");

    alertDispatchDAO.create(hdfsGroup);
    alertDispatchDAO.create(oozieGroup);

    List<AlertGroupEntity> defaultGroups = alertDispatchDAO.findAllGroups(clusterId);
    assertEquals(2, defaultGroups.size());
    assertNotNull(alertDispatchDAO.findDefaultServiceGroup(clusterId, "HDFS"));
    assertNotNull(alertDispatchDAO.findDefaultServiceGroup(clusterId, "OOZIE"));

    return defaultGroups;
  }

  /**
   * Convenient method to create or to get repository version for given cluster.  The repository
   * version string is based on the cluster's stack version.
   *
   * @return repository version
   */
  public RepositoryVersionEntity getOrCreateRepositoryVersion(Cluster cluster) {
    StackId stackId = cluster.getCurrentStackVersion();
    String version = stackId.getStackVersion() + ".1";

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    assertNotNull(stackEntity);

    RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(
        stackId, version);

    if (repositoryVersion == null) {
      try {
        List<RepoOsEntity> operatingSystems = new ArrayList<>();
        RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
        repoDefinitionEntity1.setRepoID("HDP");
        repoDefinitionEntity1.setBaseUrl("");
        repoDefinitionEntity1.setRepoName("HDP");
        RepoDefinitionEntity repoDefinitionEntity2 = new RepoDefinitionEntity();
        repoDefinitionEntity2.setRepoID("HDP-UTILS");
        repoDefinitionEntity2.setBaseUrl("");
        repoDefinitionEntity2.setRepoName("HDP-UTILS");
        RepoOsEntity repoOsEntityRedHat6 = new RepoOsEntity();
        repoOsEntityRedHat6.setFamily("redhat6");
        repoOsEntityRedHat6.setAmbariManaged(true);
        repoOsEntityRedHat6.addRepoDefinition(repoDefinitionEntity1);
        repoOsEntityRedHat6.addRepoDefinition(repoDefinitionEntity2);
        RepoOsEntity repoOsEntityRedHat5 = new RepoOsEntity();
        repoOsEntityRedHat5.setFamily("redhat5");
        repoOsEntityRedHat5.setAmbariManaged(true);
        repoOsEntityRedHat5.addRepoDefinition(repoDefinitionEntity1);
        repoOsEntityRedHat5.addRepoDefinition(repoDefinitionEntity2);
        operatingSystems.add(repoOsEntityRedHat6);
        operatingSystems.add(repoOsEntityRedHat5);

        repositoryVersion = repositoryVersionDAO.create(stackEntity, version,
            String.valueOf(System.currentTimeMillis()) + uniqueCounter.incrementAndGet(),
            operatingSystems);
      } catch (Exception ex) {
        LOG.error("Caught exception", ex);
        ex.printStackTrace();
        Assert.fail(MessageFormat.format("Unable to create Repo Version for Stack {0} and version {1}",
            stackEntity.getStackName() + "-" + stackEntity.getStackVersion(), version));
      }
    }
    return repositoryVersion;
  }

  /**
   * Convenient method to create or to get repository version for given stack.
   *
   * @param stackId stack object
   * @param version stack version
   * @return repository version
   */
  public RepositoryVersionEntity getOrCreateRepositoryVersion(StackId stackId,
      String version) {
    StackEntity stackEntity = null;
    try {
      stackEntity = createStack(stackId);
    } catch (Exception e) {
      LOG.error("Expected successful repository", e);
    }

    assertNotNull(stackEntity);

    RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(
        stackId, version);

    if (repositoryVersion == null) {
      try {
        List<RepoOsEntity> operatingSystems = new ArrayList<>();
        RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
        repoDefinitionEntity1.setRepoID("HDP");
        repoDefinitionEntity1.setBaseUrl("");
        repoDefinitionEntity1.setRepoName("HDP");
        RepoDefinitionEntity repoDefinitionEntity2 = new RepoDefinitionEntity();
        repoDefinitionEntity2.setRepoID("HDP-UTILS");
        repoDefinitionEntity2.setBaseUrl("");
        repoDefinitionEntity2.setRepoName("HDP-UTILS");
        RepoOsEntity repoOsEntityRedHat6 = new RepoOsEntity();
        repoOsEntityRedHat6.setFamily("redhat6");
        repoOsEntityRedHat6.setAmbariManaged(true);
        repoOsEntityRedHat6.addRepoDefinition(repoDefinitionEntity1);
        repoOsEntityRedHat6.addRepoDefinition(repoDefinitionEntity2);
        RepoOsEntity repoOsEntityRedHat5 = new RepoOsEntity();
        repoOsEntityRedHat5.setFamily("redhat5");
        repoOsEntityRedHat5.setAmbariManaged(true);
        repoOsEntityRedHat5.addRepoDefinition(repoDefinitionEntity1);
        repoOsEntityRedHat5.addRepoDefinition(repoDefinitionEntity2);
        operatingSystems.add(repoOsEntityRedHat6);
        operatingSystems.add(repoOsEntityRedHat5);


        repositoryVersion = repositoryVersionDAO.create(stackEntity, version,
            String.valueOf(System.currentTimeMillis()) + uniqueCounter.incrementAndGet(), operatingSystems);

        repositoryVersion.setResolved(true);
        repositoryVersion = repositoryVersionDAO.merge(repositoryVersion);
      } catch (Exception ex) {
        LOG.error("Caught exception", ex);

        Assert.fail(MessageFormat.format("Unable to create Repo Version for Stack {0} and version {1}",
            stackEntity.getStackName() + "-" + stackEntity.getStackVersion(), version));
      }
    }
    return repositoryVersion;
  }

  /**
   * Convenient method to create host version for given stack.
   */
  public HostVersionEntity createHostVersion(String hostName, RepositoryVersionEntity repositoryVersionEntity,
      RepositoryVersionState repositoryVersionState) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    HostVersionEntity hostVersionEntity = new HostVersionEntity(hostEntity, repositoryVersionEntity, repositoryVersionState);
    hostVersionEntity.setHostId(hostEntity.getHostId());
    hostVersionDAO.create(hostVersionEntity);

    hostEntity.getHostVersionEntities().add(hostVersionEntity);
    hostDAO.merge(hostEntity);

    return hostVersionEntity;
  }
}
