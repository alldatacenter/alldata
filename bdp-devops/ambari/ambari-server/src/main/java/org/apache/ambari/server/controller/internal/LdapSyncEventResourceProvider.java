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

package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.OperationNotSupportedException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.LdapSyncRequest;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.LdapSyncEventEntity;
import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for ldap sync events.
 */
public class LdapSyncEventResourceProvider extends AbstractControllerResourceProvider {

  /**
   * Thread pool
   */
  private static ExecutorService executorService;

  // TODO : Do we need to expose the thread pool params in configuration?
  private final static int  THREAD_POOL_CORE_SIZE = 2;
  private final static int  THREAD_POOL_MAX_SIZE  = 5;
  private final static long THREAD_POOL_TIMEOUT   = 1000L;

  /**
   * Event property id constants.
   */
  public static final String EVENT_ID_PROPERTY_ID            = "Event/id";
  public static final String EVENT_STATUS_PROPERTY_ID        = "Event/status";
  public static final String EVENT_STATUS_DETAIL_PROPERTY_ID = "Event/status_detail";
  public static final String EVENT_START_TIME_PROPERTY_ID    = "Event/sync_time/start";
  public static final String EVENT_END_TIME_PROPERTY_ID      = "Event/sync_time/end";
  public static final String USERS_CREATED_PROPERTY_ID       = "Event/summary/users/created";
  public static final String USERS_UPDATED_PROPERTY_ID       = "Event/summary/users/updated";
  public static final String USERS_REMOVED_PROPERTY_ID       = "Event/summary/users/removed";
  public static final String USERS_SKIPPED_PROPERTY_ID       = "Event/summary/users/skipped";
  public static final String GROUPS_CREATED_PROPERTY_ID      = "Event/summary/groups/created";
  public static final String GROUPS_UPDATED_PROPERTY_ID      = "Event/summary/groups/updated";
  public static final String GROUPS_REMOVED_PROPERTY_ID      = "Event/summary/groups/removed";
  public static final String MEMBERSHIPS_CREATED_PROPERTY_ID = "Event/summary/memberships/created";
  public static final String MEMBERSHIPS_REMOVED_PROPERTY_ID = "Event/summary/memberships/removed";
  public static final String EVENT_SPECS_PROPERTY_ID         = "Event/specs";

  /**
   * The key property ids for a event resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.LdapSyncEvent, EVENT_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a event resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      EVENT_ID_PROPERTY_ID,
      EVENT_STATUS_PROPERTY_ID,
      EVENT_STATUS_DETAIL_PROPERTY_ID,
      EVENT_START_TIME_PROPERTY_ID,
      EVENT_END_TIME_PROPERTY_ID,
      USERS_CREATED_PROPERTY_ID,
      USERS_UPDATED_PROPERTY_ID,
      USERS_REMOVED_PROPERTY_ID,
      USERS_SKIPPED_PROPERTY_ID,
      GROUPS_CREATED_PROPERTY_ID,
      GROUPS_UPDATED_PROPERTY_ID,
      GROUPS_REMOVED_PROPERTY_ID,
      MEMBERSHIPS_CREATED_PROPERTY_ID,
      MEMBERSHIPS_REMOVED_PROPERTY_ID,
      EVENT_SPECS_PROPERTY_ID);

  /**
   * Spec property keys.
   */
  private static final String PRINCIPAL_TYPE_SPEC_KEY = "principal_type";
  private static final String SYNC_TYPE_SPEC_KEY      = "sync_type";
  private static final String POST_PROCESS_EXISTING_USERS_SPEC_KEY = "post_process_existing_users";
  private static final String NAMES_SPEC_KEY          = "names";

  /**
   * Map of all sync events.
   */
  private final Map<Long, LdapSyncEventEntity> events = new ConcurrentSkipListMap<>();

  /**
   * The queue of events to be processed.
   */
  private final Queue<LdapSyncEventEntity> eventQueue = new LinkedList<>();

  /**
   * Indicates whether or not the events are currently being processed.
   */
  private volatile boolean processingEvents = false;

  /**
   * The next event id.
   */
  private AtomicLong nextEventId = new AtomicLong(1L);

  /**
   * The logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(LdapSyncEventResourceProvider.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a event resource provider.
   */
  public LdapSyncEventResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.LdapSyncEvent, propertyIds, keyPropertyIds, managementController);

    EnumSet<RoleAuthorization> roleAuthorizations =
        EnumSet.of(RoleAuthorization.AMBARI_MANAGE_GROUPS, RoleAuthorization.AMBARI_MANAGE_USERS);

    setRequiredCreateAuthorizations(roleAuthorizations);
    setRequiredDeleteAuthorizations(roleAuthorizations);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResourcesAuthorized(Request event)
      throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {
    Set<LdapSyncEventEntity> newEvents = new HashSet<>();

    for (Map<String, Object> properties : event.getProperties()) {
      newEvents.add(createResources(getCreateCommand(properties)));
    }
    notifyCreate(Resource.Type.ViewInstance, event);

    Set<Resource> associatedResources = new HashSet<>();
    for (LdapSyncEventEntity eventEntity : newEvents) {
      Resource resource = new ResourceImpl(Resource.Type.LdapSyncEvent);
      resource.setProperty(EVENT_ID_PROPERTY_ID, eventEntity.getId());
      associatedResources.add(resource);
      synchronized (eventQueue) {
        eventQueue.offer(eventEntity);
      }
    }

    ensureEventProcessor();

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResources(Request event, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources    = new HashSet<>();
    Set<String>   requestedIds = getRequestPropertyIds(event, predicate);

    for (LdapSyncEventEntity eventEntity : events.values()) {
      resources.add(toResource(eventEntity, requestedIds));
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request event, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    modifyResources(getDeleteCommand(predicate));
    notifyDelete(Resource.Type.ViewInstance, predicate);
    return getRequestStatus(null);
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Ensure that sync events are being processed.
   */
  protected void ensureEventProcessor() {

    if (!processingEvents) {
      synchronized (eventQueue) {
        if (!processingEvents) {
          processingEvents = true;
          getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
              processSyncEvents();
            }
          });
        }
      }
    }
  }

  // create a resource from the given event entity
  private Resource toResource(LdapSyncEventEntity eventEntity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.LdapSyncEvent);

    setResourceProperty(resource, EVENT_ID_PROPERTY_ID, eventEntity.getId(), requestedIds);
    setResourceProperty(resource, EVENT_STATUS_PROPERTY_ID, eventEntity.getStatus().toString().toUpperCase(), requestedIds);
    setResourceProperty(resource, EVENT_STATUS_DETAIL_PROPERTY_ID, eventEntity.getStatusDetail(), requestedIds);
    setResourceProperty(resource, USERS_CREATED_PROPERTY_ID, eventEntity.getUsersCreated(), requestedIds);
    setResourceProperty(resource, USERS_UPDATED_PROPERTY_ID, eventEntity.getUsersUpdated(), requestedIds);
    setResourceProperty(resource, USERS_REMOVED_PROPERTY_ID, eventEntity.getUsersRemoved(), requestedIds);
    setResourceProperty(resource, USERS_SKIPPED_PROPERTY_ID, eventEntity.getUsersSkipped(), requestedIds);
    setResourceProperty(resource, GROUPS_CREATED_PROPERTY_ID, eventEntity.getGroupsCreated(), requestedIds);
    setResourceProperty(resource, GROUPS_UPDATED_PROPERTY_ID, eventEntity.getGroupsUpdated(), requestedIds);
    setResourceProperty(resource, GROUPS_REMOVED_PROPERTY_ID, eventEntity.getGroupsRemoved(), requestedIds);
    setResourceProperty(resource, MEMBERSHIPS_CREATED_PROPERTY_ID, eventEntity.getMembershipsCreated(), requestedIds);
    setResourceProperty(resource, MEMBERSHIPS_REMOVED_PROPERTY_ID, eventEntity.getMembershipsRemoved(), requestedIds);

    Set<Map<String, String>> specs = new HashSet<>();

    List<LdapSyncSpecEntity> specList = eventEntity.getSpecs();

    for (LdapSyncSpecEntity spec : specList) {

      Map<String, String> specMap = new HashMap<>();

      specMap.put(PRINCIPAL_TYPE_SPEC_KEY, spec.getPrincipalType().toString().toLowerCase());
      specMap.put(SYNC_TYPE_SPEC_KEY, spec.getSyncType().toString().toLowerCase());

      List<String> names = spec.getPrincipalNames();

      if (!names.isEmpty()) {
        specMap.put(NAMES_SPEC_KEY, names.toString().replace("[", "").replace("]", "").replace(", ", ","));
      }
      specs.add(specMap);
    }
    setResourceProperty(resource, EVENT_SPECS_PROPERTY_ID, specs, requestedIds);

    setResourceProperty(resource, EVENT_START_TIME_PROPERTY_ID, eventEntity.getStartTime(), requestedIds);
    setResourceProperty(resource, EVENT_END_TIME_PROPERTY_ID, eventEntity.getEndTime(), requestedIds);

    return resource;
  }

  // create a event entity from the given set of properties
  private LdapSyncEventEntity toEntity(Map<String, Object> properties) {
    LdapSyncEventEntity      entity   = new LdapSyncEventEntity(getNextEventId());
    List<LdapSyncSpecEntity> specList = new LinkedList<>();

    Set<Map<String, String>> specs = (Set<Map<String, String>>) properties.get(EVENT_SPECS_PROPERTY_ID);

    for (Map<String, String> specMap : specs) {

      LdapSyncSpecEntity.SyncType      syncType      = null;
      LdapSyncSpecEntity.PrincipalType principalType = null;
      boolean postProcessExistingUsers = false;

      List<String> principalNames = Collections.emptyList();

      for (Map.Entry<String, String> entry : specMap.entrySet()) {
        String key = entry.getKey();
        if (key.equalsIgnoreCase(PRINCIPAL_TYPE_SPEC_KEY)) {
          principalType = LdapSyncSpecEntity.PrincipalType.valueOfIgnoreCase(entry.getValue());

        } else if (key.equalsIgnoreCase(SYNC_TYPE_SPEC_KEY)) {
          syncType = LdapSyncSpecEntity.SyncType.valueOfIgnoreCase(entry.getValue());

        } else if (key.equalsIgnoreCase(NAMES_SPEC_KEY)) {
          String names = entry.getValue();
          principalNames = Arrays.asList(names.split("\\s*,\\s*"));

        } else if (key.equalsIgnoreCase(POST_PROCESS_EXISTING_USERS_SPEC_KEY)) {
          postProcessExistingUsers = "true".equalsIgnoreCase(entry.getValue());

        } else {
          throw new IllegalArgumentException("Unknown spec key " + key + ".");
        }
      }

      if (syncType == null || principalType == null) {
        throw new IllegalArgumentException("LDAP event spec must include both sync-type and principal-type.");
      }

      LdapSyncSpecEntity spec = new LdapSyncSpecEntity(principalType, syncType, principalNames, postProcessExistingUsers);
      specList.add(spec);
    }
    entity.setSpecs(specList);

    return entity;
  }

  // get the next event id
  private long getNextEventId() {
    return nextEventId.getAndIncrement();
  }

  // Create a create command with all properties set
  private Command<LdapSyncEventEntity> getCreateCommand(final Map<String, Object> properties) {
    return new Command<LdapSyncEventEntity>() {
      @Override
      public LdapSyncEventEntity invoke() throws AmbariException, AuthorizationException {

        LdapSyncEventEntity eventEntity = toEntity(properties);

        for (LdapSyncSpecEntity ldapSyncSpecEntity : eventEntity.getSpecs()) {
          if (ldapSyncSpecEntity.getPrincipalType() == LdapSyncSpecEntity.PrincipalType.USERS) {
            if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_USERS)) {
              throw new AuthorizationException("The uthenticated user is not authorized to syng LDAP users");
            }
          } else {
            if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_GROUPS)) {
              throw new AuthorizationException("The uthenticated user is not authorized to syng LDAP groups");
            }
          }
        }

        events.put(eventEntity.getId(), eventEntity);

        return eventEntity;
      }
    };
  }

  // Create a delete command with the given predicate
  private Command<Void> getDeleteCommand(final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        Set<String>  requestedIds = getRequestPropertyIds(PropertyHelper.getReadRequest(), predicate);

        Set<LdapSyncEventEntity> entities = new HashSet<>();

        for (LdapSyncEventEntity entity : events.values()){
              Resource resource = toResource(entity, requestedIds);
              if (predicate == null || predicate.evaluate(resource)) {
                entities.add(entity);
              }
        }
        for (LdapSyncEventEntity entity : entities) {
          events.remove(entity.getId());
        }
        return null;
      }
    };
  }

  // Get the ldap sync thread pool
  private static synchronized ExecutorService getExecutorService() {
    if (executorService == null) {
      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
          THREAD_POOL_CORE_SIZE,
          THREAD_POOL_MAX_SIZE,
          THREAD_POOL_TIMEOUT,
          TimeUnit.MILLISECONDS,
          queue);

      threadPoolExecutor.allowCoreThreadTimeOut(true);
      executorService = threadPoolExecutor;
    }
    return executorService;
  }

  // Process any queued up sync events
  private void processSyncEvents() {

    while (processingEvents) {
      LdapSyncEventEntity event;
      synchronized (eventQueue) {
        if (processingEvents) {
          event = eventQueue.poll();
          if (event == null) {
            processingEvents = false;
            return;
          }
        } else {
          break;
        }
      }

      event.setStatus(LdapSyncEventEntity.Status.RUNNING);
      event.setStatusDetail("Running LDAP sync.");
      event.setStartTime(System.currentTimeMillis());

      try {

        populateLdapSyncEvent(event, syncLdap(event));

        event.setStatus(LdapSyncEventEntity.Status.COMPLETE);
        event.setStatusDetail("Completed LDAP sync.");
      } catch (Exception e) {
        event.setStatus(LdapSyncEventEntity.Status.ERROR);
        String msg = "Caught exception running LDAP sync. ";
        if (e.getCause() instanceof OperationNotSupportedException) {
          msg += "LDAP server may not support search results pagination. " +
            "Try to turn the pagination off.";
        }
        event.setStatusDetail(msg + e.getMessage());
        LOG.error(msg, e);
      } finally {
        event.setEndTime(System.currentTimeMillis());
      }
    }
  }

  /**
   * Sync the users and groups specified in the given sync event with ldap.
   *
   * @param event  the sync event
   *
   * @return the results of the sync
   *
   * @throws AmbariException if the sync could not be completed
   */
  private LdapBatchDto syncLdap(LdapSyncEventEntity event) throws AmbariException {
    LdapSyncRequest userRequest  = null;
    LdapSyncRequest groupRequest = null;

    for (LdapSyncSpecEntity spec : event.getSpecs()) {
      switch (spec.getPrincipalType()) {
        case USERS:
          userRequest = getLdapRequest(userRequest, spec);
          break;
        case GROUPS:
          groupRequest = getLdapRequest(groupRequest, spec);
          break;
      }
    }
    return getManagementController().synchronizeLdapUsersAndGroups(userRequest, groupRequest);
  }

  /**
   * Update the given request with the given ldap event spec.
   *
   * @param request  the sync request; may be null
   * @param spec     the specification of what to sync
   *
   * @return the updated sync request or a new sync request if the given request is null
   */
  private LdapSyncRequest getLdapRequest(LdapSyncRequest request, LdapSyncSpecEntity spec) {

    switch (spec.getSyncType()) {
      case ALL:
        return new LdapSyncRequest(LdapSyncSpecEntity.SyncType.ALL, spec.getPostProcessExistingUsers());
      case EXISTING:
        return new LdapSyncRequest(LdapSyncSpecEntity.SyncType.EXISTING, spec.getPostProcessExistingUsers());
      case SPECIFIC:
        Set<String> principalNames = new HashSet<>(spec.getPrincipalNames());
        if (request == null ) {
          request = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC, principalNames, spec.getPostProcessExistingUsers());
        } else {
          request.addPrincipalNames(principalNames);
        }
    }
    return request;
  }

  /**
   * Populate the given ldap sync event with the results of an ldap sync.
   *
   * @param event     the sync event
   * @param syncInfo  the sync results
   */
  private void populateLdapSyncEvent(LdapSyncEventEntity event, LdapBatchDto syncInfo) {
    event.setUsersCreated(syncInfo.getUsersToBeCreated().size());
    event.setUsersUpdated(syncInfo.getUsersToBecomeLdap().size());
    event.setUsersRemoved(syncInfo.getUsersToBeRemoved().size());
    event.setUsersSkipped(syncInfo.getUsersSkipped().size());
    event.setGroupsCreated(syncInfo.getGroupsToBeCreated().size());
    event.setGroupsUpdated(syncInfo.getGroupsToBecomeLdap().size());
    event.setGroupsRemoved(syncInfo.getGroupsToBeRemoved().size());
    event.setMembershipsCreated(syncInfo.getMembershipToAdd().size());
    event.setMembershipsRemoved(syncInfo.getMembershipToRemove().size());
  }
}
