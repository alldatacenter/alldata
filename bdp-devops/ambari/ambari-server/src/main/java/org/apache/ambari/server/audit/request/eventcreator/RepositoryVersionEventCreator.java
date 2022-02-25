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

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.request.AddRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.ChangeRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.audit.event.request.DeleteRepositoryVersionRequestAuditEvent;
import org.apache.ambari.server.controller.internal.OperatingSystemResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * This creator handles privilege requests
 * For resource type {@link Resource.Type#Repository}
 * and request types {@link Request.Type#POST}, {@link Request.Type#PUT} and {@link Request.Type#DELETE}
 */
public class RepositoryVersionEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = ImmutableSet.<Request.Type>builder().add(Request.Type.PUT, Request.Type.POST, Request.Type.DELETE).build();

  /**
   * Set of {@link Resource.Type}s that are handled by this plugin
   */
  private Set<Resource.Type> resourceTypes = ImmutableSet.<Resource.Type>builder().add(Resource.Type.RepositoryVersion).build();

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource.Type> getResourceTypes() {
    return resourceTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ResultStatus.STATUS> getResultStatuses() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditEvent createAuditEvent(Request request, Result result) {

    switch (request.getRequestType()) {
      case POST:
        return AddRepositoryVersionRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withStackName(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID))
          .withStackVersion(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID))
          .withDisplayName(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID))
          .withRepoVersion(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID))
          .withRepos(getRepos(request))
          .build();
      case PUT:
        return ChangeRepositoryVersionRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withStackName(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID))
          .withStackVersion(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID))
          .withDisplayName(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID))
          .withRepoVersion(RequestAuditEventCreatorHelper.getProperty(request, RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID))
          .withRepos(getRepos(request))
          .build();
      case DELETE:
        return DeleteRepositoryVersionRequestAuditEvent.builder()
          .withTimestamp(System.currentTimeMillis())
          .withRequestType(request.getRequestType())
          .withResultStatus(result.getStatus())
          .withUrl(request.getURI())
          .withRemoteIp(request.getRemoteAddress())
          .withStackName(request.getResource().getKeyValueMap().get(Resource.Type.Stack))
          .withStackVersion(request.getResource().getKeyValueMap().get(Resource.Type.StackVersion))
          .withRepoVersion(request.getResource().getKeyValueMap().get(Resource.Type.RepositoryVersion))
          .build();
      default:
        return null;
    }
  }

  /**
   * Assembles repositories from the request
   * operating system -> list of repositories where the repository is a map of properties (repo_id, repo_name, base_url)
   * @param request
   * @return a map of repositories
   */
  private SortedMap<String, List<Map<String, String>>> getRepos(Request request) {

    SortedMap<String, List<Map<String, String>>> result = new TreeMap<>();

    Map<String, Object> first = Iterables.getFirst(request.getBody().getPropertySets(), null);

    if (first != null && first.get("operating_systems") instanceof Set) {
      Set<?> set = (Set<?>) first.get("operating_systems");
      result = createResultForOperationSystems(set);
    }
    return result;
  }

  /**
   * Returns repos for the set of operating systems
   * @param set
   * @return
   */
  private SortedMap<String, List<Map<String, String>>> createResultForOperationSystems(Set<?> set) {
    SortedMap<String, List<Map<String, String>>> result = new TreeMap<>();
    for (Object entry : set) {
      if (entry instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) entry;
        String osType = (String) map.get(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID);
        if (!result.containsKey(osType)) {
          result.put(osType, new LinkedList<>());
        }
        if (map.get("repositories") instanceof Set) {
          Set<?> repos = (Set<?>) map.get("repositories");
          for (Object repo : repos) {
            if (repo instanceof Map) {
              Map<String, String> resultMap = buildResultRepo((Map<String, String>) repo);
              result.get(osType).add(resultMap);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns a map with the repository properties (repo_id, repo_name, base_url)
   * @param repo
   * @return
   */
  private Map<String, String> buildResultRepo(Map<String, String> repo) {
    Map<String, String> m = repo;
    String repoId = m.get(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID);
    String repo_name = m.get(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID);
    String baseUrl = m.get(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID);
    Map<String, String> resultMap = new HashMap<>();
    resultMap.put("repo_id", repoId);
    resultMap.put("repo_name", repo_name);
    resultMap.put("base_url", baseUrl);
    return resultMap;
  }
}
