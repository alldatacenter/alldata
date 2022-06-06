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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;
import org.apache.ambari.server.controller.internal.BaseClusterRequest;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.ScaleClusterRequest;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.ClusterConfigFinishedEvent;
import org.apache.ambari.server.events.ClusterProvisionStartedEvent;
import org.apache.ambari.server.events.ClusterProvisionedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.RequestFinishedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.SettingDAO;
import org.apache.ambari.server.orm.entities.SettingEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfile;
import org.apache.ambari.server.topology.addservice.ResourceProviderAdapter;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTask;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.ambari.server.topology.validators.TopologyValidatorService;
import org.apache.ambari.server.utils.ManagedThreadPoolExecutor;
import org.apache.ambari.server.utils.RetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Manages all cluster provisioning actions on the cluster topology.
 */
//todo: cluster isolation
@Singleton
public class TopologyManager {

  /**
   * internal token for topology related async tasks
   */
  public static final String INTERNAL_AUTH_TOKEN = "internal_topology_token";

  public static final String INITIAL_CONFIG_TAG = "INITIAL";
  public static final String TOPOLOGY_RESOLVED_TAG = "TOPOLOGY_RESOLVED";
  public static final String KDC_ADMIN_CREDENTIAL = "kdc.admin.credential";

  private PersistedState persistedState;

  /**
   * Single threaded executor to execute async tasks. At the moment it's only used to execute ConfigureClusterTask.
   */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /**
   * Thread pool size for topology task executors.
   */
  private int topologyTaskExecutorThreadPoolSize;
  /**
   * There is one ExecutorService for each cluster to execute TopologyTasks.
   * TopologyTasks are submitted into ExecutorService for each cluster,
   * however the ExecutorService is started only after cluster configuration is finished.
   */
  private final Map<Long, ManagedThreadPoolExecutor> topologyTaskExecutorServiceMap = new HashMap<>();

  private Collection<String> hostsToIgnore = new HashSet<>();
  private final List<HostImpl> availableHosts = new LinkedList<>();
  private final Map<String, LogicalRequest> reservedHosts = new HashMap<>();
  private final Map<Long, LogicalRequest> allRequests = new HashMap<>();
  // priority is given to oldest outstanding requests
  private final Collection<LogicalRequest> outstandingRequests = new ArrayList<>();
  //todo: currently only support a single cluster
  private Map<Long, ClusterTopology> clusterTopologyMap = new HashMap<>();

  @Inject
  private StackAdvisorBlueprintProcessor stackAdvisorBlueprintProcessor;

  @Inject
  private LogicalRequestFactory logicalRequestFactory;

  @Inject
  private AmbariContext ambariContext;

  private final Object initializationLock = new Object();

  @Inject
  private SecurityConfigurationFactory securityConfigurationFactory;

  @Inject
  private ConfigureClusterTaskFactory configureClusterTaskFactory;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private SettingDAO settingDAO;

  @Inject
  private TopologyValidatorService topologyValidatorService;

  /**
   * A boolean not cached thread-local (volatile) to prevent double-checked
   * locking on the synchronized keyword.
   */
  private volatile boolean isInitialized;

  private final static Logger LOG = LoggerFactory.getLogger(TopologyManager.class);

  /**
   * Stores request that belongs to blueprint creation
   */
  private Map<Long, LogicalRequest> clusterProvisionWithBlueprintCreateRequests = new HashMap<>();
  /**
   * Flag to show whether blueprint is already finished or not. It is used for shortcuts.
   */
  private Map<Long, Boolean> clusterProvisionWithBlueprintCreationFinished = new HashMap<>();

  public TopologyManager() {
    topologyTaskExecutorThreadPoolSize = 1;
  }

  @Inject
  public TopologyManager(Configuration configuration) {
    topologyTaskExecutorThreadPoolSize = configuration.getParallelTopologyTaskCreationThreadPoolSize();
    if (!configuration.isParallelTopologyTaskCreationEnabled()) {
      topologyTaskExecutorThreadPoolSize = 1;
    }
  }

  // executed by the IoC framework after creating the object (guice)
  @Inject
  private void register() {
    ambariEventPublisher.register(this);
  }

  @Inject
  private void setPersistedState() {
    persistedState = ambariContext.getPersistedTopologyState();
  }

  //todo: can't call in constructor.
  //todo: Very important that this occurs prior to any usage
  private void ensureInitialized() {
    if (!isInitialized) {
      synchronized (initializationLock) {
        if (!isInitialized) {
          replayRequests(persistedState.getAllRequests());
          // ensure KERBEROS_CLIENT is present in each hostgroup even if it's not in original BP
          for(ClusterTopology clusterTopology : clusterTopologyMap.values()) {
            if (clusterTopology.isClusterKerberosEnabled()) {
              addKerberosClient(clusterTopology);
            }
          }
          isInitialized = true;
        }

      }
    }
  }

  /**
   * Called when heartbeat processing finishes
   * @param event
   */
  @Subscribe
  public void onRequestFinished(RequestFinishedEvent event) {
    if(event.getType() != AmbariEvent.AmbariEventType.REQUEST_FINISHED
            || clusterProvisionWithBlueprintCreateRequests.isEmpty()
            || Boolean.TRUE.equals(clusterProvisionWithBlueprintCreationFinished.get(event.getClusterId()))) {
      return;
    }

    if(isClusterProvisionWithBlueprintFinished(event.getClusterId())) {
      clusterProvisionWithBlueprintCreationFinished.put(event.getClusterId(), Boolean.TRUE);
      LogicalRequest provisionRequest = clusterProvisionWithBlueprintCreateRequests.get(event.getClusterId());
      if(isLogicalRequestSuccessful(provisionRequest)) {
        LOG.info("Cluster creation request id={} using Blueprint {} successfully completed for cluster id={}",
                clusterProvisionWithBlueprintCreateRequests.get(event.getClusterId()).getRequestId(),
                clusterTopologyMap.get(event.getClusterId()).getBlueprint().getName(),
                event.getClusterId());
        ambariEventPublisher.publish(new ClusterProvisionedEvent(event.getClusterId()));
      } else {
        LOG.info("Cluster creation request id={} using Blueprint {} failed for cluster id={}",
                clusterProvisionWithBlueprintCreateRequests.get(event.getClusterId()).getRequestId(),
                clusterTopologyMap.get(event.getClusterId()).getBlueprint().getName(),
                event.getClusterId());
      }
    }
  }

  /**
   * Returns if provision request for a cluster is tracked
   * @param clusterId
   * @return
   */
  public boolean isClusterProvisionWithBlueprintTracked(long clusterId) {
    return clusterProvisionWithBlueprintCreateRequests.containsKey(clusterId);
  }

  /**
   * Returns if the provision request for a cluster is finished.
   * Note that this method returns false if the request is not tracked.
   * See {@link TopologyManager#isClusterProvisionWithBlueprintTracked(long)}
   * @param clusterId
   * @return
   */
  public boolean isClusterProvisionWithBlueprintFinished(long clusterId) {
    if(!isClusterProvisionWithBlueprintTracked(clusterId)) {
      return false; // no blueprint request is running
    }
    // shortcut
    if(clusterProvisionWithBlueprintCreationFinished.containsKey(clusterId) && clusterProvisionWithBlueprintCreationFinished.get(clusterId)) {
      return true;
    }
    return isLogicalRequestFinished(clusterProvisionWithBlueprintCreateRequests.get(clusterId));
  }

  public RequestStatusResponse provisionCluster(final ProvisionClusterRequest request) throws InvalidTopologyException, AmbariException {
    ensureInitialized();

    final ClusterTopology topology = new ClusterTopologyImpl(ambariContext, request);
    final String clusterName = request.getClusterName();
    final Stack stack = topology.getBlueprint().getStack();
    final String repoVersion = request.getRepositoryVersion();
    final Long repoVersionID = request.getRepositoryVersionId();

    // get the id prior to creating ambari resources which increments the counter
    final Long provisionId = ambariContext.getNextRequestId();

    SecurityType securityType = null;
    Credential credential = null;

    SecurityConfiguration securityConfiguration = processSecurityConfiguration(request);

    if (securityConfiguration != null && securityConfiguration.getType() == SecurityType.KERBEROS) {
      securityType = SecurityType.KERBEROS;
      addKerberosClient(topology);

      // refresh default stack config after adding KERBEROS_CLIENT component to topology
      topology.getBlueprint().getConfiguration().setParentConfiguration(stack.getConfiguration(topology.getBlueprint().getServices()));

      credential = request.getCredentialsMap().get(KDC_ADMIN_CREDENTIAL);
      if (credential == null) {
        throw new InvalidTopologyException(KDC_ADMIN_CREDENTIAL + " is missing from request.");
      }
    }

    topologyValidatorService.validateTopologyConfiguration(topology);


    // create resources
    ambariContext.createAmbariResources(topology, clusterName, securityType, repoVersion, repoVersionID);

    if (securityConfiguration != null) {
      securityConfiguration.getDescriptor().ifPresent(descriptor -> submitKerberosDescriptorAsArtifact(clusterName, descriptor));
    }

    if (credential != null) {
      submitCredential(clusterName, credential);
    }

    long clusterId = ambariContext.getClusterId(clusterName);
    topology.setClusterId(clusterId);
    request.setClusterId(clusterId);
    // set recommendation strategy
    topology.setConfigRecommendationStrategy(request.getConfigRecommendationStrategy());
    // set provision action requested
    topology.setProvisionAction(request.getProvisionAction());


    // create task executor for TopologyTasks
    getOrCreateTopologyTaskExecutor(clusterId);

    // persist request
    LogicalRequest logicalRequest = RetryHelper.executeWithRetry(new Callable<LogicalRequest>() {
        @Override
        public LogicalRequest call() throws Exception {
          LogicalRequest logicalRequest = processAndPersistProvisionClusterTopologyRequest(request, topology, provisionId);
          return logicalRequest;
        }
      }
    );

    clusterTopologyMap.put(clusterId, topology);

    addClusterConfigRequest(logicalRequest, topology, new ClusterConfigurationRequest(ambariContext, topology, true,
      stackAdvisorBlueprintProcessor, securityType == SecurityType.KERBEROS));

    // Process the logical request
    processRequest(request, topology, logicalRequest);

    //todo: this should be invoked as part of a generic lifecycle event which could possibly
    //todo: be tied to cluster state

    ambariContext.persistInstallStateForUI(clusterName, stack.getName(), stack.getVersion());
    clusterProvisionWithBlueprintCreateRequests.put(clusterId, logicalRequest);
    ambariEventPublisher.publish(new ClusterProvisionStartedEvent(clusterId));
    return getRequestStatus(logicalRequest.getRequestId());
  }

  @Subscribe
  public void onClusterConfigFinishedEvent(ClusterConfigFinishedEvent event) {
    ManagedThreadPoolExecutor taskExecutor = topologyTaskExecutorServiceMap.get(event.getClusterId());
    if (taskExecutor == null) {
      LOG.error("Can't find executor service taskQueue not found for cluster: {} ", event.getClusterName());
    } else {
      LOG.info("Starting topology task ExecutorService for cluster: {}", event.getClusterName());
      taskExecutor.start();
    }
  }


  /**
   * Saves the quick links profile to the DB as an Ambari setting. Creates a new setting entity or updates the existing
   * one.
   * @param quickLinksProfileJson the quicklinks profile in Json format
   */
  void saveOrUpdateQuickLinksProfile(String quickLinksProfileJson) {
    SettingEntity settingEntity = settingDAO.findByName(QuickLinksProfile.SETTING_NAME_QUICKLINKS_PROFILE);
    // create new
    if (null == settingEntity) {
      settingEntity = new SettingEntity();
      settingEntity.setName(QuickLinksProfile.SETTING_NAME_QUICKLINKS_PROFILE);
      settingEntity.setSettingType(QuickLinksProfile.SETTING_TYPE_AMBARI_SERVER);
      settingEntity.setContent(quickLinksProfileJson);
      settingEntity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
      settingEntity.setUpdateTimestamp(System.currentTimeMillis());
      settingDAO.create(settingEntity);
    }
    // update existing
    else {
      settingEntity.setContent(quickLinksProfileJson);
      settingEntity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
      settingEntity.setUpdateTimestamp(System.currentTimeMillis());
      settingDAO.merge(settingEntity);
    }
  }

  private static void submitCredential(String clusterName, Credential credential) {
    ResourceProvider provider = AmbariContext.getClusterController().ensureResourceProvider(Resource.Type.Credential);
    Map<String, Object> credentialProperties = ResourceProviderAdapter.createCredentialRequestProperties(clusterName, credential);
    Request request = new RequestImpl(ImmutableSet.of(), ImmutableSet.of(credentialProperties), ImmutableMap.of(), null);
    String baseMessage = String.format("Failed to add credential %s to cluster %s", credential.getAlias(), clusterName);
    try {
      RequestStatus status = provider.createResources(request);
      if (status.getStatus() != RequestStatus.Status.Complete) {
        String msg = String.format("%s, received status: %s", baseMessage, status.getStatus());
        LOG.error(msg);
        throw new RuntimeException(msg);
      }
    } catch (ResourceAlreadyExistsException | SystemException | UnsupportedPropertyException | NoSuchParentResourceException e) {
      String msg = String.format("%s, %s", baseMessage, e);
      LOG.error(msg);
      throw new RuntimeException(msg, e);
    }
  }

  /**
   * Retrieve security info from Blueprint if missing from Cluster Template request.
   *
   * @param request
   * @return
   */
  private SecurityConfiguration processSecurityConfiguration(ProvisionClusterRequest request) {
    LOG.debug("Getting security configuration from the request ...");
    SecurityConfiguration securityConfiguration = request.getSecurityConfiguration();

    if (securityConfiguration == null) {
      // todo - perform this logic at request creation instead!
      LOG.debug("There's no security configuration in the request, retrieving it from the associated blueprint");
      securityConfiguration = request.getBlueprint().getSecurity();
      if (securityConfiguration != null && securityConfiguration.getType() == SecurityType.KERBEROS &&
          securityConfiguration.getDescriptorReference() != null) {
        securityConfiguration = securityConfigurationFactory.loadSecurityConfigurationByReference
          (securityConfiguration.getDescriptorReference());
      }
    }
    return securityConfiguration;
  }

  private void submitKerberosDescriptorAsArtifact(String clusterName, Map<?,?> descriptor) {

    ResourceProvider artifactProvider =
        ambariContext.getClusterController().ensureResourceProvider(Resource.Type.Artifact);

    Map<String, Object> properties = ResourceProviderAdapter.createKerberosDescriptorRequestProperties(clusterName);
    Map<String, String> requestInfoProps = ImmutableMap.of(
      Request.REQUEST_INFO_BODY_PROPERTY, ArtifactResourceProvider.toArtifactDataJson(descriptor)
    );
    Request request = new RequestImpl(Collections.emptySet(), Collections.singleton(properties), requestInfoProps, null);

    try {
      RequestStatus status = artifactProvider.createResources(request);
      try {
        while (status.getStatus() != RequestStatus.Status.Complete) {
          LOG.info("Waiting for kerberos_descriptor artifact creation.");
          Thread.sleep(100);
        }
      } catch (InterruptedException e) {
        LOG.info("Wait for resource creation interrupted!");
      }

      if (status.getStatus() != RequestStatus.Status.Complete) {
        throw new RuntimeException("Failed to attach kerberos_descriptor artifact to cluster!");
      }
    } catch (SystemException | UnsupportedPropertyException | NoSuchParentResourceException e) {
      throw new RuntimeException("Failed to attach kerberos_descriptor artifact to cluster: " + e);
    } catch (ResourceAlreadyExistsException e) {
      throw new RuntimeException("Failed to attach kerberos_descriptor artifact to cluster as resource already exists.");
    }

  }

  public RequestStatusResponse scaleHosts(final ScaleClusterRequest request)
      throws InvalidTopologyException, AmbariException {

    ensureInitialized();
    LOG.info("TopologyManager.scaleHosts: Entering");
    String clusterName = request.getClusterName();
    long clusterId = ambariContext.getClusterId(clusterName);
    final ClusterTopology topology = clusterTopologyMap.get(clusterId);
    if (topology == null) {
      throw new InvalidTopologyException("Unable to retrieve cluster topology for cluster. This is most likely a " +
          "result of trying to scale a cluster via the API which was created using " +
          "the Ambari UI. At this time only clusters created via the API using a " +
          "blueprint can be scaled with this API.  If the cluster was originally created " +
          "via the API as described above, please file a Jira for this matter.");
    }

    hostNameCheck(request, topology);
    request.setClusterId(clusterId);
    if (ambariContext.isTopologyResolved(clusterId)) {
      getOrCreateTopologyTaskExecutor(clusterId).start();
    }

    // this registers/updates all request host groups
    topology.update(request);

    final Long requestId = ambariContext.getNextRequestId();
    LogicalRequest logicalRequest = RetryHelper.executeWithRetry(new Callable<LogicalRequest>() {
         @Override
         public LogicalRequest call() throws Exception {
           LogicalRequest logicalRequest = processAndPersistTopologyRequest(request, topology, requestId);

           return logicalRequest;
         }
       }
    );
    processRequest(request, topology, logicalRequest);
    return getRequestStatus(logicalRequest.getRequestId());
  }

  public void removePendingHostRequests(String clusterName, long requestId) {
    ensureInitialized();
    LOG.info("TopologyManager.removePendingHostRequests: Entering");

    long clusterId = 0;
    try {
      clusterId = ambariContext.getClusterId(clusterName);
    } catch (AmbariException e) {
      LOG.error("Unable to retrieve clusterId", e);
      throw new IllegalArgumentException("Unable to retrieve clusterId");
    }
    ClusterTopology topology = clusterTopologyMap.get(clusterId);
    if (topology == null) {
      throw new IllegalArgumentException("Unable to retrieve cluster topology for cluster");
    }

    LogicalRequest logicalRequest = allRequests.get(requestId);
    if (logicalRequest == null) {
      throw new IllegalArgumentException("No Logical Request found for requestId: " + requestId);
    }

    Collection<HostRequest> pendingHostRequests = logicalRequest.removePendingHostRequests(null);

    if (!logicalRequest.hasPendingHostRequests()) {
      outstandingRequests.remove(logicalRequest);
    }
    if (logicalRequest.getHostRequests().isEmpty()) {
      allRequests.remove(requestId);
    }

    persistedState.removeHostRequests(requestId, pendingHostRequests);

    // set current host count to number of currently connected hosts
    for (HostGroupInfo currentHostGroupInfo : topology.getHostGroupInfo().values()) {
      currentHostGroupInfo.setRequestedCount(currentHostGroupInfo.getHostNames().size());
    }

    LOG.info("TopologyManager.removePendingHostRequests: Exit");
  }

  /**
   * Removes topology host requests matched to the given host.  If the parent
   * request has no more child host requests, then it is also removed.
   * This is used when hosts are deleted from the cluster.
   *
   * @param hostName the host name for which requests should be removed
   */
  public void removeHostRequests(String hostName) {
    ensureInitialized();

    for (Iterator<LogicalRequest> iter = allRequests.values().iterator(); iter.hasNext(); ) {
      LogicalRequest logicalRequest = iter.next();
      Collection<HostRequest> removed = logicalRequest.removeHostRequestByHostName(hostName);
      if (!logicalRequest.hasPendingHostRequests()) {
        outstandingRequests.remove(logicalRequest);
      }
      if (logicalRequest.getHostRequests().isEmpty()) {
        iter.remove();
      }
      if (!removed.isEmpty()) {
        persistedState.removeHostRequests(logicalRequest.getRequestId(), removed);
      }
    }
  }

  /**
   * Creates and persists a {@see PersistedTopologyRequest} and a {@see LogicalRequest} for the provided
   * provision cluster request and topology.
   * @param request Provision cluster request to create a logical request for.
   * @param topology Cluster topology
   * @param logicalRequestId The Id for the created logical request
   * @return Logical request created.
   */
  @Transactional
  protected LogicalRequest processAndPersistProvisionClusterTopologyRequest(ProvisionClusterRequest request, ClusterTopology topology, Long logicalRequestId)
    throws InvalidTopologyException, AmbariException {

    if (null != request.getQuickLinksProfileJson()) {
      saveOrUpdateQuickLinksProfile(request.getQuickLinksProfileJson());
    }

    LogicalRequest logicalRequest = processAndPersistTopologyRequest(request, topology, logicalRequestId);

    return logicalRequest;

  }


  /**
   * Creates and persists a {@see PersistedTopologyRequest} and a {@see LogicalRequest} for the provided request and topology.
   * @param request {@see ProvisionClusterRequest} or {@see ScaleClusterRequest} to create a logical request for.
   * @param topology Cluster topology
   * @param logicalRequestId The Id for the created logical request
   * @return Logical request created.
   */
  @Transactional
  protected  LogicalRequest processAndPersistTopologyRequest(BaseClusterRequest request, ClusterTopology topology, Long logicalRequestId)
    throws InvalidTopologyException, AmbariException {
    PersistedTopologyRequest persistedRequest = persistedState.persistTopologyRequest(request);

    LogicalRequest logicalRequest = createLogicalRequest(persistedRequest, topology, logicalRequestId);

    return logicalRequest;
  }

  private void hostNameCheck(ScaleClusterRequest request, ClusterTopology topology) throws InvalidTopologyException {
    Set<String> hostNames = new HashSet<>();
    for(Map.Entry<String, HostGroupInfo> entry : request.getHostGroupInfo().entrySet()) {
      hostNames.addAll(entry.getValue().getHostNames());
    }
    for(String hostName : hostNames) {
      // check if host exists already
      if(topology.getHostGroupForHost(hostName) != null) {
        throw new InvalidTopologyException("Host " + hostName + " cannot be added, because it is already in the cluster");
      }
    }
  }

  public void onHostRegistered(HostImpl host, boolean associatedWithCluster) {
    ensureInitialized();
    LOG.info("TopologyManager.onHostRegistered: Entering");
    if (associatedWithCluster || isHostIgnored(host.getHostName())) {
      LOG.info("TopologyManager.onHostRegistered: host = {} is already associated with the cluster or is currently being processed", host.getHostName());
      return;
    }

    boolean matchedToRequest = false;
    String hostName = host.getHostName();
    // The lock ordering in this method must always be the same ordering as TopologyManager.processRequest
    // TODO: Locking strategies for TopologyManager should be reviewed and possibly rewritten in a future release
    synchronized (availableHosts) {
      synchronized (reservedHosts) {
        if (reservedHosts.containsKey(hostName)) {
          LogicalRequest request = reservedHosts.remove(hostName);
          HostOfferResponse response = request.offer(host);
          if (response.getAnswer() != HostOfferResponse.Answer.ACCEPTED) {
            throw new RuntimeException("LogicalRequest declined host offer of explicitly requested host: " + hostName);
          }

          LOG.info("TopologyManager.onHostRegistered: processing accepted host offer for reserved host = {}", hostName);
          processAcceptedHostOffer(getClusterTopology(request.getClusterId()), response, host);
          matchedToRequest = true;
        }
      }

      // can be true if host was reserved
      if (!matchedToRequest) {
        synchronized (outstandingRequests) {
          Iterator<LogicalRequest> outstandingRequestIterator = outstandingRequests.iterator();
          while (!matchedToRequest && outstandingRequestIterator.hasNext()) {
            LogicalRequest request = outstandingRequestIterator.next();
            HostOfferResponse hostOfferResponse = request.offer(host);
            switch (hostOfferResponse.getAnswer()) {
              case ACCEPTED:
                matchedToRequest = true;
                LOG.info("TopologyManager.onHostRegistered: processing accepted host offer for matched host = {}", hostName);
                processAcceptedHostOffer(getClusterTopology(request.getClusterId()), hostOfferResponse, host);
                break;
              case DECLINED_DONE:
                LOG.info("TopologyManager.onHostRegistered: DECLINED_DONE received for host = {}", hostName);
                outstandingRequestIterator.remove();
                break;
              case DECLINED_PREDICATE:
                LOG.info("TopologyManager.onHostRegistered: DECLINED_PREDICATE received for host = {}", hostName);
                break;
            }
          }
        }
      }

      if (!matchedToRequest) {
        boolean addToAvailableList = true;
        for (HostImpl registered : availableHosts) {
          if (Objects.equals(registered.getHostId(), host.getHostId())) {
            LOG.info("Host {} re-registered, will not be added to the available hosts list", hostName);
            addToAvailableList = false;
            break;
          }
        }

        if (addToAvailableList) {
          LOG.info("TopologyManager: Queueing available host {}", hostName);
          availableHosts.add(host);
        }
      }
    }
  }

  /**
   * Through this method {@see TopologyManager} gets notified when a connection to a host in the cluster is lost.
   * The passed host will be excluded from scheduling any tasks onto it as it can't be reached.
   * @param host
   */
  public void onHostHeartBeatLost(Host host) {
    if (AmbariServer.getController() == null) {
      return;
    }
    ensureInitialized();
    synchronized (availableHosts) {
      LOG.info("Hearbeat for host {} lost thus removing it from available hosts.", host.getHostName());
      availableHosts.remove(host);
    }
  }

  public LogicalRequest getRequest(long requestId) {
    ensureInitialized();
    return allRequests.get(requestId);
  }

  public Collection<LogicalRequest> getRequests(Collection<Long> requestIds) {
    ensureInitialized();
    if (requestIds.isEmpty()) {
      return allRequests.values();
    } else {
      Collection<LogicalRequest> matchingRequests = new ArrayList<>();
      for (long id : requestIds) {
        LogicalRequest request = allRequests.get(id);
        if (request != null) {
          matchingRequests.add(request);
        }
      }
      return matchingRequests;
    }
  }

  /**
   * Currently we are just returning all stages for all requests
   * and relying on the StageResourceProvider to convert each to a resource and do a predicate eval on each.
   */
  public Collection<StageEntity> getStages() {
    ensureInitialized();
    Collection<StageEntity> stages = new ArrayList<>();
    for (LogicalRequest logicalRequest : allRequests.values()) {
      stages.addAll(logicalRequest.getStageEntities());
    }
    return stages;
  }

  public Collection<HostRoleCommand> getTasks(long requestId) {
    ensureInitialized();
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? Collections.emptyList() : request.getCommands();
  }

  public Collection<HostRoleCommand> getTasks(Collection<Long> requestIds) {
    ensureInitialized();
    Collection<HostRoleCommand> tasks = new ArrayList<>();
    for (long id : requestIds) {
      tasks.addAll(getTasks(id));
    }

    return tasks;
  }

  public Map<Long, HostRoleCommandStatusSummaryDTO> getStageSummaries(Long requestId) {
    ensureInitialized();
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? Collections.emptyMap() :
        request.getStageSummaries();
  }

  public RequestStatusResponse getRequestStatus(long requestId) {
    ensureInitialized();
    LogicalRequest request = allRequests.get(requestId);
    return request == null ? null : request.getRequestStatus();
  }

  public Collection<RequestStatusResponse> getRequestStatus(Collection<Long> ids) {
    ensureInitialized();
    List<RequestStatusResponse> requestStatusResponses = new ArrayList<>();
    for (long id : ids) {
      RequestStatusResponse response = getRequestStatus(id);
      if (response != null) {
        requestStatusResponses.add(response);
      }
    }

    return requestStatusResponses;
  }

  public ClusterTopology getClusterTopology(Long clusterId) {
    ensureInitialized();
    return clusterTopologyMap.get(clusterId);
  }

  /**
   * Gets a map of components keyed by host which have operations in the
   * {@link HostRoleStatus#PENDING} state. This could either be because hosts
   * have not registered or becuase the operations are actually waiting to be
   * queued.
   *
   * @return a mapping of host with pending components.
   */
  public Map<String, Collection<String>> getPendingHostComponents() {
    ensureInitialized();
    Map<String, Collection<String>> hostComponentMap = new HashMap<>();

    for (LogicalRequest logicalRequest : allRequests.values()) {
      Map<Long, HostRoleCommandStatusSummaryDTO> summary = logicalRequest.getStageSummaries();
      final CalculatedStatus status = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());

      // either use the calculated status of the stage or the fact that there
      // are no tasks and the request has no end time to determine if the
      // request is still in progress
      boolean logicalRequestInProgress = false;
      if (status.getStatus().isInProgress() || (summary.isEmpty() && logicalRequest.getEndTime() <= 0) ) {
        logicalRequestInProgress = true;
      }

      if (logicalRequestInProgress) {
        Map<String, Collection<String>> requestTopology = logicalRequest.getProjectedTopology();
        for (Map.Entry<String, Collection<String>> entry : requestTopology.entrySet()) {
          String host = entry.getKey();
          Collection<String> hostComponents = hostComponentMap.get(host);
          if (hostComponents == null) {
            hostComponents = new HashSet<>();
            hostComponentMap.put(host, hostComponents);
          }
          hostComponents.addAll(entry.getValue());
        }
      }
    }
    return hostComponentMap;
  }

  private void processRequest(TopologyRequest request, ClusterTopology topology, final LogicalRequest logicalRequest)
    throws AmbariException {

    LOG.info("TopologyManager.processRequest: Entering");

    finalizeTopology(request, topology);

    boolean requestHostComplete = false;
    //todo: overall synchronization. Currently we have nested synchronization here

    // The lock ordering in this method must always be the same ordering as TopologyManager.onHostRegistered
    // TODO: Locking strategies for TopologyManager should be reviewed and possibly rewritten in a future release
    synchronized (availableHosts) {
      Iterator<HostImpl> hostIterator = availableHosts.iterator();
      while (!requestHostComplete && hostIterator.hasNext()) {
        HostImpl host = hostIterator.next();
        synchronized (reservedHosts) {
          String hostname = host.getHostName();
          if (reservedHosts.containsKey(hostname)) {
            if (logicalRequest.equals(reservedHosts.get(hostname))) {
              // host is registered to this request, remove it from reserved map
              LOG.info("TopologyManager.processRequest: host name = {} is mapped to LogicalRequest ID = {} and will be removed from the reserved hosts.",
                  hostname, logicalRequest.getRequestId());
              reservedHosts.remove(hostname);
            } else {
              // host is registered with another request, don't offer
              //todo: clean up logic
              LOG.info("TopologyManager.processRequest: host name = {} is registered with another request, and will not be offered to LogicalRequest ID = {}",
                  hostname, logicalRequest.getRequestId());
              continue;
            }
          }
        }

        LOG.info("TopologyManager.processRequest: offering host name = {} to LogicalRequest ID = {}",
            host.getHostName(), logicalRequest.getRequestId());
        HostOfferResponse response = logicalRequest.offer(host);
        switch (response.getAnswer()) {
          case ACCEPTED:
            //todo: when host matches last host it returns ACCEPTED so we don't know that logical request is no
            //todo: longer outstanding until we call offer again.  This is really only an issue if we need to
            //todo: deal specifically with outstanding hosts other than calling offer.  Also, failure handling
            //todo: may affect this behavior??
            hostIterator.remove();
            LOG.info("TopologyManager.processRequest: host name = {} was ACCEPTED by LogicalRequest ID = {} , host has been removed from available hosts.",
                host.getHostName(), logicalRequest.getRequestId());
            processAcceptedHostOffer(getClusterTopology(logicalRequest.getClusterId()), response, host);
            break;
          case DECLINED_DONE:
            requestHostComplete = true;
            LOG.info("TopologyManager.processRequest: host name = {} was DECLINED_DONE by LogicalRequest ID = {}",
                host.getHostName(), logicalRequest.getRequestId());
            break;
          case DECLINED_PREDICATE:
            LOG.info("TopologyManager.processRequest: host name = {} was DECLINED_PREDICATE by LogicalRequest ID = {}",
                host.getHostName(), logicalRequest.getRequestId());
            break;
        }
      }

      if (!requestHostComplete) {
        // not all required hosts have been matched (see earlier comment regarding outstanding logical requests)
        LOG.info("TopologyManager.processRequest: not all required hosts have been matched, so adding LogicalRequest ID = {} to outstanding requests",
            logicalRequest.getRequestId());
        synchronized (outstandingRequests) {
          outstandingRequests.add(logicalRequest);
        }
      }
    }
  }

  @Transactional
  protected LogicalRequest createLogicalRequest(final PersistedTopologyRequest request, ClusterTopology topology, Long requestId)
      throws AmbariException {

    final LogicalRequest logicalRequest = logicalRequestFactory.createRequest(
        requestId, request.getRequest(), topology);

    persistedState.persistLogicalRequest(logicalRequest, request.getId());

    allRequests.put(logicalRequest.getRequestId(), logicalRequest);
    LOG.info("TopologyManager.createLogicalRequest: created LogicalRequest with ID = {} and completed persistence of this request.",
        logicalRequest.getRequestId());
    synchronized (reservedHosts) {
      for (String host : logicalRequest.getReservedHosts()) {
        reservedHosts.put(host, logicalRequest);
      }
    }
    return logicalRequest;
  }

  private void processAcceptedHostOffer(final ClusterTopology topology, final HostOfferResponse response, final HostImpl host) {
    final String hostName = host.getHostName();
    try {
      topology.addHostToTopology(response.getHostGroupName(), hostName);

      // update the host with the rack info if applicable
      updateHostWithRackInfo(topology, response, host);

    } catch (InvalidTopologyException | NoSuchHostGroupException e) {
      // host already registered
      throw new RuntimeException("An internal error occurred while performing request host registration: " + e, e);
    }

    // persist the host request -> hostName association
    try {
      RetryHelper.executeWithRetry(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          persistTopologyHostRegistration(response.getHostRequestId(), host);
          return null;
        }
      });
    } catch (AmbariException e) {
      LOG.error("Exception ocurred while registering host name", e);
      throw new RuntimeException(e);
    }

    LOG.info("TopologyManager.processAcceptedHostOffer: queue tasks for host = {} which responded {}", hostName, response.getAnswer());
    queueHostTasks(topology, response, hostName);

  }

  @Transactional
  protected void persistTopologyHostRegistration(long hostRequestId, final HostImpl host) {
    persistedState.registerHostName(hostRequestId, host.getHostName());
    persistedState.registerInTopologyHostInfo(host);
  }

  private ManagedThreadPoolExecutor getOrCreateTopologyTaskExecutor(Long clusterId) {
    ManagedThreadPoolExecutor topologyTaskExecutor = this.topologyTaskExecutorServiceMap.get(clusterId);
    if (topologyTaskExecutor == null) {
      LOG.info("Creating TopologyTaskExecutorService for clusterId: {}", clusterId);

      topologyTaskExecutor = new ManagedThreadPoolExecutor(topologyTaskExecutorThreadPoolSize,
              topologyTaskExecutorThreadPoolSize, 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>());
      topologyTaskExecutorServiceMap.put(clusterId, topologyTaskExecutor);
    }
    return topologyTaskExecutor;
  }

  private void queueHostTasks(ClusterTopology topology, HostOfferResponse response, String hostName) {
    LOG.info("TopologyManager.processAcceptedHostOffer: queueing tasks for host = {}", hostName);
    ExecutorService executorService = getOrCreateTopologyTaskExecutor(topology.getClusterId());
    response.executeTasks(executorService, hostName, topology, ambariContext);
  }

  private void updateHostWithRackInfo(ClusterTopology topology, HostOfferResponse response, HostImpl host) {
    // the rack info from the cluster creation template
    String rackInfoFromTemplate = topology.getHostGroupInfo().get(response.getHostGroupName()).getHostRackInfo().get
        (host.getHostName());

    if (null != rackInfoFromTemplate) {
      host.setRackInfo(rackInfoFromTemplate);
      try {
        // todo: do we need this in case of blueprints?
        ambariContext.getController().registerRackChange(ambariContext.getClusterName(topology.getClusterId()));
      } catch (AmbariException e) {
        LOG.error("Could not register rack change for cluster id {}", topology.getClusterId());
        LOG.error("Exception during rack change: ", e);
      }
    }
  }

  private void replayRequests(Map<ClusterTopology, List<LogicalRequest>> persistedRequests) {
    LOG.info("TopologyManager.replayRequests: Entering");
    boolean configChecked = false;
    for (Map.Entry<ClusterTopology, List<LogicalRequest>> requestEntry : persistedRequests.entrySet()) {
      ClusterTopology topology = requestEntry.getKey();
      clusterTopologyMap.put(topology.getClusterId(), topology);
      // update provision request cache
      LogicalRequest provisionRequest = persistedState.getProvisionRequest(topology.getClusterId());
      if(provisionRequest != null) {
        clusterProvisionWithBlueprintCreateRequests.put(topology.getClusterId(), provisionRequest);
        clusterProvisionWithBlueprintCreationFinished.put(topology.getClusterId(),
                isLogicalRequestFinished(clusterProvisionWithBlueprintCreateRequests.get(topology.getClusterId())));
      }

      for (LogicalRequest logicalRequest : requestEntry.getValue()) {
        allRequests.put(logicalRequest.getRequestId(), logicalRequest);
        if (logicalRequest.hasPendingHostRequests()) {
          outstandingRequests.add(logicalRequest);
          for (String reservedHost : logicalRequest.getReservedHosts()) {
            reservedHosts.put(reservedHost, logicalRequest);
          }
          // completed host requests are host requests which have been mapped to a host
          // and the host has ben added to the cluster
          for (HostRequest hostRequest : logicalRequest.getCompletedHostRequests()) {
            try {
              String hostName = hostRequest.getHostName();
              topology.addHostToTopology(hostRequest.getHostgroupName(), hostName);
              hostsToIgnore.add(hostName);
              LOG.info("TopologyManager.replayRequests: host name = {} has been added to cluster and to ignore list.", hostName);
            } catch (InvalidTopologyException e) {
              LOG.warn("Attempted to add host to multiple host groups while replaying requests: " + e, e);
            } catch (NoSuchHostGroupException e) {
              LOG.warn("Failed to add host to topology while replaying requests: " + e, e);
            }
          }
        }
      }

      if (!configChecked) {
        configChecked = true;
        if (!ambariContext.isTopologyResolved(topology.getClusterId())) {
          if (provisionRequest == null) {
            LOG.info("TopologyManager.replayRequests: no config with TOPOLOGY_RESOLVED found, but provision request missing, skipping cluster config request");
          } else if (provisionRequest.isFinished()) {
            LOG.info("TopologyManager.replayRequests: no config with TOPOLOGY_RESOLVED found, but provision request is finished, skipping cluster config request");
          } else {
            LOG.info("TopologyManager.replayRequests: no config with TOPOLOGY_RESOLVED found, adding cluster config request");
            ClusterConfigurationRequest configRequest = new ClusterConfigurationRequest(ambariContext, topology, false, stackAdvisorBlueprintProcessor);
            addClusterConfigRequest(provisionRequest, topology, configRequest);
          }
        } else {
          getOrCreateTopologyTaskExecutor(topology.getClusterId()).start();
        }
      }
    }
    LOG.info("TopologyManager.replayRequests: Exit");
  }

  /**
   * @return true if all the tasks in the logical request are in completed state, false otherwise
   */
  private boolean isLogicalRequestFinished(LogicalRequest logicalRequest) {
    return logicalRequest != null && logicalRequest.isFinished();
  }

  /**
   * Returns if all the tasks in the logical request have completed state.
   */
  private boolean isLogicalRequestSuccessful(LogicalRequest logicalRequest) {
    return logicalRequest != null && logicalRequest.isSuccessful();
  }

  //todo: this should invoke a callback on each 'service' in the topology
  private void finalizeTopology(TopologyRequest request, ClusterTopology topology) {
  }

  private boolean isHostIgnored(String host) {
    return hostsToIgnore.remove(host);
  }

  /**
   * Add the kerberos client to groups if kerberos is enabled for the cluster.
   *
   * @param topology  cluster topology
   */
  private void addKerberosClient(ClusterTopology topology) {
    for (HostGroup group : topology.getBlueprint().getHostGroups().values()) {
      group.addComponent("KERBEROS_CLIENT");
    }
  }

  /**
   * Register the configuration task which is responsible for configuration topology resolution
   * and setting the updated configuration on the cluster.  This task needs to be submitted to the
   * executor before any host requests to ensure that no install or start tasks are executed prior
   * to configuration being set on the cluster.
   *
   * @param topology              cluster topology
   * @param configurationRequest  configuration request to be executed
   */
  private void addClusterConfigRequest(final LogicalRequest logicalRequest, ClusterTopology topology, ClusterConfigurationRequest configurationRequest) {
    ConfigureClusterTask task = configureClusterTaskFactory.createConfigureClusterTask(topology, configurationRequest, ambariEventPublisher);
    executor.submit(new AsyncCallableService<>(task, task.getTimeout(), task.getRepeatDelay(),"ConfigureClusterTask", throwable -> {
      HostRoleStatus status = throwable instanceof TimeoutException ? HostRoleStatus.TIMEDOUT : HostRoleStatus.FAILED;
      LOG.info("ConfigureClusterTask failed, marking host requests {}", status);
      for (HostRequest hostRequest : logicalRequest.getHostRequests()) {
        hostRequest.markHostRequestFailed(status, throwable, persistedState);
      }
    }));
  }

  /**
   *
   * Removes a host from the available hosts when the host gets deleted.
   * @param hostsRemovedEvent the event containing the hostname
   */
  @Subscribe
  public void processHostRemovedEvent(HostsRemovedEvent hostsRemovedEvent) {

    if (hostsRemovedEvent.getHostNames().isEmpty()) {
      LOG.warn("Missing host name from host removed event [{}] !", hostsRemovedEvent);
      return;
    }

    LOG.info("Removing hosts [{}] from available hosts on hosts removed event.", hostsRemovedEvent.getHostNames());
    Set<HostImpl> toBeRemoved = new HashSet<>();

    // synchronization is required here as the list may be modified concurrently. See comments in this whole class.
    synchronized (availableHosts) {
      for (HostImpl hostImpl : availableHosts) {
        for (String hostName : hostsRemovedEvent.getHostNames()) {
          if (hostName.equals(hostImpl.getHostName())) {
            toBeRemoved.add(hostImpl);
            break;
          }
        }
      }

      if (!toBeRemoved.isEmpty()) {
        for (HostImpl host : toBeRemoved) {
          availableHosts.remove(host);
          LOG.info("Removed host: [{}] from available hosts", host.getHostName());
        }
      } else {
        LOG.debug("No any host [{}] found in available hosts", hostsRemovedEvent.getHostNames());
      }
    }
  }

}
