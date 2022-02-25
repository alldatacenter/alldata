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

package org.apache.ambari.server.api.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.QueryResponseImpl;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.PageRequest.StartingPoint;
import org.apache.ambari.server.controller.spi.PageResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default read query.
 */
public class QueryImpl implements Query, ResourceInstance {

  /**
   * Definition for the resource type.  The definition contains all information specific to the
   * resource type.
   */
  private final ResourceDefinition resourceDefinition;

  /**
   * The cluster controller.
   */
  private final ClusterController clusterController;

  /**
   * Properties of the query which make up the select portion of the query.
   */
  private final Set<String> requestedProperties = new HashSet<>();

  /**
   * Map that associates categories with temporal data.
   */
  private final Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();

  /**
   * Map of primary and foreign key values.
   */
  private final Map<Resource.Type, String> keyValueMap = new HashMap<>();

  /**
   * Map of properties from the request.
   */
  private final Map<String, String> requestInfoProperties = new HashMap<>();

  /**
   * Set of query results.
   */
  Map<Resource, QueryResult> queryResults = new LinkedHashMap<>();

  /**
   * Set of populated query results
   */
  Map<Resource, QueryResult> populatedQueryResults = new LinkedHashMap<>();

  /**
   * Sub-resources of the resource which is being operated on.
   * Should only be added via {@link #addSubResource(String, QueryImpl)}
   */
  private final Map<String, QueryImpl> requestedSubResources = new HashMap<>();

  /**
   * Sub-resource instances of this resource.
   * Map of resource name to resource instance.
   */
  private Map<String, QueryImpl> availableSubResources;

  /**
   * Indicates that the query should include all available properties.
   */
  private boolean allProperties = false;

  /**
   * The user supplied predicate.
   */
  private Predicate userPredicate;

  /**
   * The user supplied page request information.
   */
  private PageRequest pageRequest;

  /**
   * The user supplied sorting information.
   */
  private SortRequest sortRequest;

  /**
   * The sub resource properties referenced in the user predicate.
   */
  private final Set<String> subResourcePredicateProperties = new HashSet<>();

  /**
   * Associated renderer. The default renderer is used unless
   * an alternate renderer is specified for the request. The renderer
   * is responsible for determining which properties are selected
   * as well as the overall structure of the result.
   */
  private Renderer renderer;

  /**
   * Sub-resource predicate.
   */
  private Predicate subResourcePredicate;

  /**
   * Processed predicate.
   */
  private Predicate processedPredicate;

  /**
   * The logger.
   */
  private final static Logger LOG =
      LoggerFactory.getLogger(QueryImpl.class);


  // ----- Constructor -------------------------------------------------------

  /**
   * Constructor
   *
   * @param keyValueMap         the map of key values
   * @param resourceDefinition  the resource definition
   * @param clusterController   the cluster controller
   */
  public QueryImpl(Map<Resource.Type, String> keyValueMap,
                   ResourceDefinition resourceDefinition,
                   ClusterController clusterController) {
    this.resourceDefinition = resourceDefinition;
    this.clusterController  = clusterController;
    setKeyValueMap(keyValueMap);
  }


  // ----- Query -------------------------------------------------------------

  @Override
  public void addProperty(String propertyId, TemporalInfo temporalInfo) {
    if (propertyId.equals("*")) {
      // wildcard
      addAllProperties(temporalInfo);
    } else{
      if (! addPropertyToSubResource(propertyId, temporalInfo)) {
        if (propertyId.endsWith("/*")) {
          propertyId = propertyId.substring(0, propertyId.length() - 2);
        }
        addLocalProperty(propertyId);
        if (temporalInfo != null) {
          temporalInfoMap.put(propertyId, temporalInfo);
        }
      }
    }
  }

  @Override
  public void addLocalProperty(String property) {
    requestedProperties.add(property);
  }

  @Override
  public Result execute()
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {
    queryForResources();
    return getResult(null);
  }

  @Override
  public Predicate getPredicate() {
    return createPredicate();
  }

  @Override
  public Set<String> getProperties() {
    return Collections.unmodifiableSet(requestedProperties);
  }

  @Override
  public void setUserPredicate(Predicate predicate) {
    userPredicate = predicate;
  }

  @Override
  public void setPageRequest(PageRequest pageRequest) {
    this.pageRequest = pageRequest;
  }

  @Override
  public void setSortRequest(SortRequest sortRequest) {
    this.sortRequest = sortRequest;
  }

  @Override
  public void setRenderer(Renderer renderer) {
    this.renderer = renderer;
    renderer.init(clusterController);
  }

  @Override
  public void setRequestInfoProps(Map<String, String> requestInfoProperties) {
    if(requestInfoProperties != null) {
      this.requestInfoProperties.putAll(requestInfoProperties);
    }
  }

  @Override
  public Map<String, String> getRequestInfoProps() {
    return this.requestInfoProperties;
  }


// ----- ResourceInstance --------------------------------------------------

  @Override
  public void setKeyValueMap(Map<Resource.Type, String> keyValueMap) {
    this.keyValueMap.putAll(keyValueMap);
  }

  @Override
  public Map<Resource.Type, String> getKeyValueMap() {
    return new HashMap<>((keyValueMap));
  }

  @Override
  public Query getQuery() {
    return this;
  }

  @Override
  public ResourceDefinition getResourceDefinition() {
    return resourceDefinition;
  }

  @Override
  public boolean isCollectionResource() {
    return getKeyValueMap().get(getResourceDefinition().getType()) == null;
  }

  @Override
  public Map<String, ResourceInstance> getSubResources() {
    return new HashMap<>(ensureSubResources());
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QueryImpl query = (QueryImpl) o;

    return clusterController.equals(query.clusterController) && !(pageRequest != null ?
        !pageRequest.equals(query.pageRequest) :
        query.pageRequest != null) && requestedProperties.equals(query.requestedProperties) &&
        resourceDefinition.equals(query.resourceDefinition) &&
        keyValueMap.equals(query.keyValueMap) && !(userPredicate != null ?
        !userPredicate.equals(query.userPredicate) :
        query.userPredicate != null);
  }

  @Override
  public int hashCode() {
    int result = resourceDefinition.hashCode();
    result = 31 * result + clusterController.hashCode();
    result = 31 * result + requestedProperties.hashCode();
    result = 31 * result + keyValueMap.hashCode();
    result = 31 * result + (userPredicate != null ? userPredicate.hashCode() : 0);
    result = 31 * result + (pageRequest != null ? pageRequest.hashCode() : 0);
    return result;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the map of sub-resources.  Lazily create the map if required.
   */
  protected Map<String, QueryImpl> ensureSubResources() {
    if (availableSubResources == null) {
      availableSubResources = new HashMap<>();
      Set<SubResourceDefinition> setSubResourceDefs =
          getResourceDefinition().getSubResourceDefinitions();

      ClusterController controller = clusterController;

      for (SubResourceDefinition subResDef : setSubResourceDefs) {
        Resource.Type type = subResDef.getType();
        Map<Resource.Type, String> valueMap = getKeyValueMap();
        QueryImpl resource =  new QueryImpl(valueMap,
            ResourceInstanceFactoryImpl.getResourceDefinition(type, valueMap),
            controller);

        String subResourceName = getSubResourceName(resource.getResourceDefinition(), subResDef);
        availableSubResources.put(subResourceName, resource);
      }
    }
    return availableSubResources;
  }

  /**
   * @param type the resource type
   * @return whether sub-resources is in processed predicate
   */
  private boolean populateResourceRequired(Resource.Type type) {
    ResourceProvider resourceProvider = clusterController.ensureResourceProvider(type);
    Set<String> unsupportedProperties =
      resourceProvider.checkPropertyIds(PredicateHelper.getPropertyIds(processedPredicate));
    return !unsupportedProperties.isEmpty() || hasSubResourcePredicate();
  }

  /**
   * Query the cluster controller for the top level resources.
   */
  private void queryForResources()
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Resource.Type resourceType    = getResourceDefinition().getType();
    Predicate     queryPredicate  = createPredicate(getKeyValueMap(), processUserPredicate(userPredicate));

    // must occur after processing user predicate and prior to creating request
    finalizeProperties();

    Request request = createRequest();

    // use linked hash sets so that we maintain insertion and traversal order
    // in the event that the resource provider already gave us a sorted set
    // back
    Set<Resource> resourceSet = new LinkedHashSet<>();
    Set<Resource> providerResourceSet = new LinkedHashSet<>();

    QueryResponse queryResponse = doQuery(resourceType, request, queryPredicate, true);

    // If there is a page request and the predicate does not contain properties
    // that need to be set
    if ((pageRequest != null || sortRequest != null ) &&
      !populateResourceRequired(resourceType)) {
      PageResponse pageResponse = clusterController.getPage(resourceType,
          queryResponse, request, queryPredicate, pageRequest, sortRequest);

      // build a new set
      for (Resource r : pageResponse.getIterable()) {
        resourceSet.add(r);
        providerResourceSet.add(r);
      }
    } else {
      resourceSet.addAll(queryResponse.getResources());
      providerResourceSet.addAll(queryResponse.getResources());
    }

    populatedQueryResults.put(null, new QueryResult(
      request, queryPredicate, userPredicate, getKeyValueMap(), new QueryResponseImpl(resourceSet)));

    queryResults.put(null, new QueryResult(
      request, queryPredicate, userPredicate, getKeyValueMap(), queryResponse));

    if (renderer.requiresPropertyProviderInput()) {
      clusterController.populateResources(resourceType, providerResourceSet, request, queryPredicate);
    }

    // Optimization:
    // Currently the steps executed when sub-resources are requested are:
    //   (1) Get *all* top-level resources
    //   (2) Populate all top-level resources
    //   (3) Query for and populate sub-resources of *all* top-level resources
    //   (4) Apply pagination and predicate on resources from above
    //
    // Though this works, it is very inefficient when either:
    //   (a) Predicate does not apply to sub-resources
    //   (b) Page request is present
    // It is inefficient because we needlessly populate sub-resources that might not get
    // used due to their top-level resources being filtered out by the predicate and paging
    //
    // The optimization is to apply the predicate and paging request on the top-level resources
    // directly if there are no sub-resources predicates.
    if ((pageRequest != null || userPredicate != null) && !hasSubResourcePredicate() && populateResourceRequired(resourceType)) {
      QueryResponse newResponse = new QueryResponseImpl(resourceSet, queryResponse.isSortedResponse(), queryResponse.isPagedResponse(),
          queryResponse.getTotalResourceCount());
      PageResponse pageResponse = clusterController.getPage(resourceType, newResponse, request, queryPredicate, pageRequest, sortRequest);
      // build a new set
      Set<Resource> newResourceSet = new LinkedHashSet<>();
      for (Resource r : pageResponse.getIterable()) {
        newResourceSet.add(r);
      }
      populatedQueryResults.put(null, new QueryResult(request, queryPredicate, userPredicate, getKeyValueMap(), new QueryResponseImpl(newResourceSet)));
    }

    queryForSubResources();
  }

  /**
   * Query the cluster controller for the sub-resources associated with
   * this query object.
   */
  private void queryForSubResources()
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    for (Map.Entry<String, QueryImpl> entry : requestedSubResources.entrySet()) {
      QueryImpl     subResource         = entry.getValue();
      Resource.Type resourceType        = subResource.getResourceDefinition().getType();
      Request       request             = subResource.createRequest();
      Set<Resource> providerResourceSet = new HashSet<>();

      for (QueryResult queryResult : populatedQueryResults.values()) {
        for (Resource resource : queryResult.getQueryResponse().getResources()) {
          Map<Resource.Type, String> map = getKeyValueMap(resource, queryResult.getKeyValueMap());

          Predicate     queryPredicate = subResource.createPredicate(map, subResource.processedPredicate);
          Set<Resource> resourceSet    = new LinkedHashSet<>();

          try {
            Set<Resource> queryResources =
                subResource.doQuery(resourceType, request, queryPredicate, false).getResources();

            providerResourceSet.addAll(queryResources);
            resourceSet.addAll(queryResources);
          } catch (NoSuchResourceException e) {
            // do nothing ...
          } catch (AuthorizationException e) {
            // do nothing, since the user does not have access to the data ...
            LOG.debug("User does not have authorization to get {} resources. The data will not be added to the response.", resourceType.name());
          }
          subResource.queryResults.put(resource,
              new QueryResult(request, queryPredicate, subResourcePredicate, map, new QueryResponseImpl(resourceSet)));
          subResource.populatedQueryResults.put(resource,
            new QueryResult(request, queryPredicate, subResourcePredicate, map, new QueryResponseImpl(resourceSet)));
        }
      }

      if (renderer.requiresPropertyProviderInput()) {
        clusterController.populateResources(resourceType, providerResourceSet, request, subResourcePredicate);
      }

      subResource.queryForSubResources();
    }
  }

  /**
   * Query the cluster controller for the resources.
   *
   * @param type                the resource type
   * @param request             the request information
   * @param predicate           the predicate
   * @param checkEmptyResponse  true if an empty query response can trigger a NoSuchResourceException
   *
   * @return the result of the cluster controller query
   *
   * @throws NoSuchResourceException if a specific resource was asked for and not found and checkEmptyResponse == true
   */
  private QueryResponse doQuery(Resource.Type type, Request request, Predicate predicate, boolean checkEmptyResponse)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing resource query: {} where {}", request, predicate);
    }

    QueryResponse queryResponse = clusterController.getResources(type, request, predicate);

    if (checkEmptyResponse && queryResponse.getResources().isEmpty()) {

      // If this is not a collection request then we must throw
      // NoSuchResourceException (404 response) for an empty query result
      if(!isCollectionResource()) {
        throw new NoSuchResourceException(
            "The requested resource doesn't exist: " + type + " not found where " + predicate + ".");
      }
    }
    return queryResponse;
  }

  /**
   * Get a map of property sets keyed by the resources associated with this
   * query. The property sets should contain the joined sets of all of the
   * requested properties from each resource's sub-resources.
   *
   * For example, if this query is associated with the resources AResource1,
   * AResource1 and AResource3 as follows ...
   *
   * <pre>
   * a_resources
   * │
   * └──AResource1 ─────────────AResource1 ─────────────AResource3
   *      │                       │                       │
   *      ├── b_resources         ├── b_resources         ├── BResources
   *      │   ├── BResource1      │   ├── BResource3      │    └── BResource5
   *      │   │     p1:1          │   │     p1:3          │          p1:5
   *      │   │     p2:5          │   │     p2:5          │          p2:5
   *      │   │                   │   │                   │
   *      │   └── BResource2      │   └── BResource4      └── c_resources
   *      │         p1:2          │         p1:4              └── CResource4
   *      │         p2:0          │         p2:0                    p3:4
   *      │                       │
   *      └── c_resources         └── c_resources
   *          ├── CResource1          └── CResource3
   *          │     p3:1                    p3:3
   *          │
   *          └── CResource2
   *                p3:2
   *
   * Given the following query ...
   *
   *     api/v1/a_resources?b_resources/p1>3&b_resources/p2=5&c_resources/p3=1
   *
   * The caller should pass the following property ids ...
   *
   *     b_resources/p1
   *     b_resources/p2
   *     c_resources/p3
   *
   * getJoinedResourceProperties should produce the following map of property sets
   * by making recursive calls on the sub-resources of each of this query's resources,
   * joining the resulting property sets, and adding them to the map keyed by the
   * resource ...
   *
   *  {
   *    AResource1=[{b_resources/p1=1, b_resources/p2=5, c_resources/p3=1},
   *                {b_resources/p1=2, b_resources/p2=0, c_resources/p3=1},
   *                {b_resources/p1=1, b_resources/p2=5, c_resources/p3=2},
   *                {b_resources/p1=2, b_resources/p2=0, c_resources/p3=2}],
   *    AResource2=[{b_resources/p1=3, b_resources/p2=5, c_resources/p3=3},
   *                {b_resources/p1=4, b_resources/p2=0, c_resources/p3=3}],
   *    AResource3=[{b_resources/p1=5, b_resources/p2=5, c_resources/p3=4}],
   *  }
   * </pre>
   *
   * @param propertyIds
   *          the requested properties
   * @param parentResource
   *          the parent resource; may be null
   * @param category
   *          the sub-resource category; may be null
   *
   * @return a map of property sets keyed by the resources associated with this
   *         query
   */
  protected Map<Resource, Set<Map<String, Object>>> getJoinedResourceProperties(Set<String> propertyIds,
                                                                                Resource parentResource,
                                                                                String category)
      throws SystemException, UnsupportedPropertyException, NoSuchParentResourceException, NoSuchResourceException {

    Map<Resource, Set<Map<String, Object>>> resourcePropertyMaps =
      new HashMap<>();

    Map<String, String> categoryPropertyIdMap =
        getPropertyIdsForCategory(propertyIds, category);

    for (Map.Entry<Resource, QueryResult> queryResultEntry : populatedQueryResults.entrySet()) {
      QueryResult queryResult         = queryResultEntry.getValue();
      Resource    queryParentResource = queryResultEntry.getKey();

      // for each resource for the given parent ...
      if (queryParentResource == parentResource) {

        Iterable<Resource> iterResource = clusterController.getIterable(
            resourceDefinition.getType(), queryResult.getQueryResponse(),
            queryResult.getRequest(), queryResult.getPredicate(), null, null);

        for (Resource resource : iterResource) {
          // get the resource properties
          Map<String, Object> resourcePropertyMap = new HashMap<>();
          for (Map.Entry<String, String> categoryPropertyIdEntry : categoryPropertyIdMap.entrySet()) {
            Object value = resource.getPropertyValue(categoryPropertyIdEntry.getValue());
            if (value != null) {
              resourcePropertyMap.put(categoryPropertyIdEntry.getKey(), value);
            }
          }

          Set<Map<String, Object>> propertyMaps = new HashSet<>();

          // For each sub category get the property maps for the sub resources
          for (Map.Entry<String, QueryImpl> entry : requestedSubResources.entrySet()) {
            String subResourceCategory = category == null ? entry.getKey() : category + "/" + entry.getKey();

            QueryImpl subResource = entry.getValue();

            Map<Resource, Set<Map<String, Object>>> subResourcePropertyMaps =
                subResource.getJoinedResourceProperties(propertyIds, resource, subResourceCategory);

            Set<Map<String, Object>> combinedSubResourcePropertyMaps = new HashSet<>();
            for (Set<Map<String, Object>> maps : subResourcePropertyMaps.values()) {
              combinedSubResourcePropertyMaps.addAll(maps);
            }
            propertyMaps = joinPropertyMaps(propertyMaps, combinedSubResourcePropertyMaps);
          }
          // add parent resource properties to joinedResources
          if (!resourcePropertyMap.isEmpty()) {
            if (propertyMaps.isEmpty()) {
              propertyMaps.add(resourcePropertyMap);
            } else {
              for (Map<String, Object> propertyMap : propertyMaps) {
                propertyMap.putAll(resourcePropertyMap);
              }
            }
          }
          resourcePropertyMaps.put(resource, propertyMaps);
        }
      }
    }
    return resourcePropertyMaps;
  }

  /**
   * Finalize properties for entire query tree before executing query.
   */
  private void finalizeProperties() {
    ResourceDefinition rootDefinition = resourceDefinition;

    QueryInfo rootQueryInfo = new QueryInfo(rootDefinition, requestedProperties);
    TreeNode<QueryInfo> rootNode = new TreeNodeImpl<>(
      null, rootQueryInfo, rootDefinition.getType().name());

    TreeNode<QueryInfo> requestedPropertyTree = buildQueryPropertyTree(this, rootNode);

    mergeFinalizedProperties(renderer.finalizeProperties(
        requestedPropertyTree, isCollectionResource()), this);
  }

  /**
   * Recursively build a tree of query information.
   *
   * @param query  query to process
   * @param node   tree node associated with the query
   *
   * @return query info tree
   */
  private TreeNode<QueryInfo> buildQueryPropertyTree(QueryImpl query, TreeNode<QueryInfo> node) {
    for (QueryImpl subQuery : query.requestedSubResources.values()) {
      ResourceDefinition childResource = subQuery.resourceDefinition;

      QueryInfo queryInfo = new QueryInfo(childResource, subQuery.requestedProperties);
      TreeNode<QueryInfo> childNode = node.addChild(queryInfo, childResource.getType().name());
      buildQueryPropertyTree(subQuery, childNode);
    }
    return node;
  }

  /**
   * Merge the tree of query properties returned by the renderer with properties in
   * the query tree.
   *
   * @param node   property tree node
   * @param query  query associated with the property tree node
   */
  private void mergeFinalizedProperties(TreeNode<Set<String>> node, QueryImpl query) {

    Set<String> finalizedProperties = node.getObject();
    query.requestedProperties.clear();
    // currently not exposing temporal information to renderer
    query.requestedProperties.addAll(finalizedProperties);

    for (TreeNode<Set<String>> child : node.getChildren()) {
      Resource.Type childType = Resource.Type.valueOf(child.getName());
      ResourceDefinition parentResource = query.resourceDefinition;
      Set<SubResourceDefinition> subResources = parentResource.getSubResourceDefinitions();
      String subResourceName = null;
      for (SubResourceDefinition subResource : subResources) {
        if (subResource.getType() == childType) {
          ResourceDefinition resource = ResourceInstanceFactoryImpl.getResourceDefinition(
              subResource.getType(), query.keyValueMap);
          subResourceName = getSubResourceName(resource, subResource);
          break;
        }
      }
      QueryImpl subQuery = query.requestedSubResources.get(subResourceName);
      if (subQuery == null) {
        query.addProperty(subResourceName, null);
        subQuery = query.requestedSubResources.get(subResourceName);
      }
      mergeFinalizedProperties(child, subQuery);
    }
  }

  // Map the given set of property ids to corresponding property ids in the
  // given sub-resource category.
  private Map<String, String> getPropertyIdsForCategory(Set<String> propertyIds, String category) {
    Map<String, String> map = new HashMap<>();

    for (String propertyId : propertyIds) {
      if (category == null || propertyId.startsWith(category)) {
        map.put(propertyId, category==null ? propertyId : propertyId.substring(category.length() + 1));
      }
    }
    return map;
  }

  // Join two sets of property maps into one.
  private static Set<Map<String, Object>> joinPropertyMaps(Set<Map<String, Object>> propertyMaps1,
                                                           Set<Map<String, Object>> propertyMaps2) {
    Set<Map<String, Object>> propertyMaps = new HashSet<>();

    if (propertyMaps1.isEmpty()) {
      return propertyMaps2;
    }
    if (propertyMaps2.isEmpty()) {
      return propertyMaps1;
    }

    for (Map<String, Object> map1 : propertyMaps1) {
      for (Map<String, Object> map2 : propertyMaps2) {
        Map<String, Object> joinedMap = new HashMap<>(map1);
        joinedMap.putAll(map2);
        propertyMaps.add(joinedMap);
      }
    }
    return propertyMaps;
  }

   // Get a result from this query.
  private Result getResult(Resource parentResource)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    Result result = new ResultImpl(true);
    Resource.Type resourceType = getResourceDefinition().getType();
    TreeNode<Resource> tree = result.getResultTree();

    if (isCollectionResource()) {
      tree.setProperty("isCollection", "true");
    }

    QueryResult queryResult = queryResults.get(parentResource);

    if (queryResult != null) {
      Predicate queryPredicate     = queryResult.getPredicate();
      Predicate queryUserPredicate = queryResult.getUserPredicate();
      Request   queryRequest       = queryResult.getRequest();

      QueryResponse queryResponse = queryResult.getQueryResponse();

      if (hasSubResourcePredicate() && queryUserPredicate != null) {
        queryPredicate = getExtendedPredicate(parentResource, queryUserPredicate);
      }

      Iterable<Resource> iterResource;

      if (pageRequest == null) {
        iterResource = clusterController.getIterable(
          resourceType, queryResponse, queryRequest, queryPredicate,
          null, sortRequest
        );
      } else {
        PageResponse pageResponse = clusterController.getPage(
          resourceType, queryResponse, queryRequest, queryPredicate,
          pageRequest, sortRequest
        );
        iterResource = pageResponse.getIterable();
        tree.setProperty("count", pageResponse.getTotalResourceCount().toString());
      }

      int count = 1;
      for (Resource resource : iterResource) {

        // add a child node for the resource and provide a unique name.  The name is never used.
        TreeNode<Resource> node = tree.addChild(
            resource, resource.getType() + ":" + count++);

        for (Map.Entry<String, QueryImpl> entry : requestedSubResources.entrySet()) {
          String    subResCategory = entry.getKey();
          QueryImpl subResource    = entry.getValue();

          TreeNode<Resource> childResult = subResource.getResult(resource).getResultTree();
          childResult.setName(subResCategory);
          childResult.setProperty("isCollection", "false");
          node.addChild(childResult);
        }
      }
    }
    return renderer.finalizeResult(result);
  }

  // Indicates whether or not this query has sub-resource elements
  // in its predicate.
  private boolean hasSubResourcePredicate() {
    return !subResourcePredicateProperties.isEmpty();
  }

  // Alter the given predicate so that the resources referenced by
  // the predicate will be extended to include the joined properties
  // of their sub-resources.
  private Predicate getExtendedPredicate(Resource parentResource,
                                         Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchParentResourceException,
             NoSuchResourceException {

    Map<Resource, Set<Map<String, Object>>> joinedResources =
        getJoinedResourceProperties(subResourcePredicateProperties, parentResource, null);

    ExtendedResourcePredicateVisitor visitor =
        new ExtendedResourcePredicateVisitor(joinedResources);

    PredicateHelper.visit(predicate, visitor);
    return visitor.getExtendedPredicate();
  }

  private void addAllProperties(TemporalInfo temporalInfo) {
    allProperties = true;
    if (temporalInfo != null) {
      temporalInfoMap.put(null, temporalInfo);
    }

    for (Map.Entry<String, QueryImpl> entry : ensureSubResources().entrySet()) {
      String name = entry.getKey();
      if (! requestedSubResources.containsKey(name)) {
        addSubResource(name, entry.getValue());
      }
    }
  }

  private boolean addPropertyToSubResource(String propertyId, TemporalInfo temporalInfo) {
    int    index    = propertyId.indexOf("/");
    String category = index == -1 ? propertyId : propertyId.substring(0, index);

    Map<String, QueryImpl> subResources = ensureSubResources();

    QueryImpl subResource = subResources.get(category);
    if (subResource != null) {
      addSubResource(category, subResource);

      //only add if a sub property is set or if a sub category is specified
      if (index != -1) {
        subResource.addProperty(propertyId.substring(index + 1), temporalInfo);
      }
      return true;
    }
    return false;
  }

  private Predicate createInternalPredicate(Map<Resource.Type, String> mapResourceIds) {
    Resource.Type resourceType = getResourceDefinition().getType();
    Schema schema = clusterController.getSchema(resourceType);

    Set<Predicate> setPredicates = new HashSet<>();
    for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
      if (entry.getValue() != null) {
        String keyPropertyId = schema.getKeyPropertyId(entry.getKey());
        if (keyPropertyId != null) {
          setPredicates.add(new EqualsPredicate<>(keyPropertyId, entry.getValue()));
        }
      }
    }

    Predicate p = null;

    if (setPredicates.size() == 1) {
      p = setPredicates.iterator().next();
    } else if (setPredicates.size() > 1) {
      p = new AndPredicate(setPredicates.toArray(new Predicate[setPredicates.size()]));
    } else {
      return null;
    }

    Resource.Type type = getResourceDefinition().getType();
    Predicate override = clusterController.getAmendedPredicate(type, p);
    if (null != override) {
      p = override;
    }

    return p;
  }

  private Predicate createPredicate() {
    return createPredicate(getKeyValueMap(), userPredicate);
  }

  private Predicate createPredicate(Map<Resource.Type, String> keyValueMap, Predicate predicate) {
    Predicate internalPredicate = createInternalPredicate(keyValueMap);

    if (internalPredicate == null) {
        return predicate;
    }
    return (predicate == null ? internalPredicate :
          new AndPredicate(predicate, internalPredicate));
  }

  // Get a sub-resource predicate from the given predicate.
  private Predicate getSubResourcePredicate(Predicate predicate, String category) {
    if (predicate == null) {
      return null;
    }

    SubResourcePredicateVisitor visitor = new SubResourcePredicateVisitor(category);
    PredicateHelper.visit(predicate, visitor);
    return visitor.getSubResourcePredicate();
  }

  // Process the given predicate to remove sub-resource elements.
  private Predicate processUserPredicate(Predicate predicate) {
    if (predicate == null) {
      return null;
    }
    ProcessingPredicateVisitor visitor = new ProcessingPredicateVisitor(this);
    PredicateHelper.visit(predicate, visitor);

    // add the sub-resource to the request
    Set<String> categories = visitor.getSubResourceCategories();
    for (String category : categories) {
      addPropertyToSubResource(category, null);
    }
    // record the sub-resource properties on this query
    subResourcePredicateProperties.addAll(visitor.getSubResourceProperties());

    if (hasSubResourcePredicate()) {
      for (Map.Entry<String, QueryImpl> entry : requestedSubResources.entrySet()) {
        subResourcePredicate = getSubResourcePredicate(predicate, entry.getKey());
        entry.getValue().processUserPredicate(subResourcePredicate);
      }
    }

    processedPredicate = visitor.getProcessedPredicate();
    return processedPredicate;
  }

  private Request createRequest() {
    // Initiate this request's requestInfoProperties with the ones set from the original request
    Map<String, String> requestInfoProperties = new HashMap<>(this.requestInfoProperties);

    if (pageRequest != null) {
      requestInfoProperties.put(BaseRequest.PAGE_SIZE_PROPERTY_KEY,
          Integer.toString(pageRequest.getPageSize() + pageRequest.getOffset()));

      requestInfoProperties.put(
        BaseRequest.ASC_ORDER_PROPERTY_KEY,
          Boolean.toString(pageRequest.getStartingPoint() == StartingPoint.Beginning
              || pageRequest.getStartingPoint() == StartingPoint.OffsetStart));
    }

    if (allProperties) {
      return PropertyHelper.getReadRequest(Collections.emptySet(),
          requestInfoProperties, null, pageRequest, sortRequest);
    }

    Map<String, TemporalInfo> mapTemporalInfo    = new HashMap<>();
    TemporalInfo              globalTemporalInfo = temporalInfoMap.get(null);

    Set<String> setProperties = new HashSet<>();
    setProperties.addAll(requestedProperties);
    for (String propertyId : setProperties) {
      TemporalInfo temporalInfo = temporalInfoMap.get(propertyId);
      if (temporalInfo != null) {
        mapTemporalInfo.put(propertyId, temporalInfo);
      } else if (globalTemporalInfo != null) {
        mapTemporalInfo.put(propertyId, globalTemporalInfo);
      }
    }

    return PropertyHelper.getReadRequest(setProperties, requestInfoProperties,
        mapTemporalInfo, pageRequest, sortRequest);
  }


  // Get a key value map based on the given resource and an existing key value map
  private Map<Resource.Type, String> getKeyValueMap(Resource resource,
                                                    Map<Resource.Type, String> keyValueMap) {
    Map<Resource.Type, String> resourceKeyValueMap = new HashMap<>(keyValueMap.size());
    for (Map.Entry<Resource.Type, String> resourceIdEntry : keyValueMap.entrySet()) {
      Resource.Type type = resourceIdEntry.getKey();
      String value = resourceIdEntry.getValue();

      if (value == null) {
        Object o = resource.getPropertyValue(clusterController.getSchema(type).getKeyPropertyId(type));
        value = o == null ? null : o.toString();
      }
      if (value != null) {
        resourceKeyValueMap.put(type, value);
      }
    }
    Schema schema = clusterController.getSchema(resource.getType());
    Set<Resource.Type> types = schema.getKeyTypes();

    for (Resource.Type type : types) {
      String resourceKeyProp = schema.getKeyPropertyId(type);
      Object resourceValue = resource.getPropertyValue(resourceKeyProp);
      if (null != resourceValue) {
        resourceKeyValueMap.put(type, resourceValue.toString());
      }
    }
    return resourceKeyValueMap;
  }

  /**
   * Add a sub query with the renderer set.
   *
   * @param name   name of sub resource
   * @param query  sub resource
   */
  private void addSubResource(String name, QueryImpl query) {
    // renderer specified for request only applies to top level query
    query.setRenderer(new DefaultRenderer());
    requestedSubResources.put(name, query);
  }

  /**
   * Obtain the name of a sub-resource.
   *
   * @param resource     parent resource
   * @param subResource  sub-resource
   *
   * @return either the plural or singular sub-resource name based on whether the sub-resource is
   *         included as a collection
   */
  private String getSubResourceName(ResourceDefinition resource, SubResourceDefinition subResource) {
    return subResource.isCollection() ?
        resource.getPluralName() :
        resource.getSingularName();
  }

  // ----- inner class : QueryResult -----------------------------------------

  /**
   * Maintain information about an individual query and its result.
   */
  private static class QueryResult {
    private final Request request;
    private final Predicate predicate;
    private final Predicate userPredicate;
    private final Map<Resource.Type, String> keyValueMap;
    private final QueryResponse queryResponse;

    // ----- Constructor -----------------------------------------------------

    private  QueryResult(Request request, Predicate predicate, Predicate userPredicate,
                         Map<Resource.Type, String> keyValueMap,
                         QueryResponse queryResponse) {
      this.request        = request;
      this.predicate      = predicate;
      this.userPredicate  = userPredicate;
      this.keyValueMap    = keyValueMap;
      this.queryResponse  = queryResponse;
    }

    // ----- accessors -------------------------------------------------------

    public Request getRequest() {
      return request;
    }

    public Predicate getPredicate() {
      return predicate;
    }

    public Predicate getUserPredicate() {
      return userPredicate;
    }

    public Map<Resource.Type, String> getKeyValueMap() {
      return keyValueMap;
    }

    public QueryResponse getQueryResponse() {
      return queryResponse;
    }
  }
}
