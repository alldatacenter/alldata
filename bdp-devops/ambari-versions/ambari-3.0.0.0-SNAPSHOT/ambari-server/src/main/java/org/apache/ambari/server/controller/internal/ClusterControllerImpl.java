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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.ExtendedResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.PageResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourcePredicateEvaluator;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SortRequestProperty;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default cluster controller implementation.
 */
public class ClusterControllerImpl implements ClusterController {
  private final static Logger LOG =
      LoggerFactory.getLogger(ClusterControllerImpl.class);

  /**
   * Module of providers for this controller.
   */
  private final ProviderModule providerModule;

  /**
   * Map of resource providers keyed by resource type.
   */
  private final Map<Resource.Type, ExtendedResourceProviderWrapper> resourceProviders =
    new HashMap<>();

  /**
   * Map of property provider lists keyed by resource type.
   */
  private final Map<Resource.Type, List<PropertyProvider>> propertyProviders =
    new HashMap<>();

  /**
   * Map of schemas keyed by resource type.
   */
  private final Map<Resource.Type, Schema> schemas =
    new HashMap<>();

  /**
   * Resource comparator.
   */
  private final ResourceComparator comparator = new ResourceComparator();

  /**
   * Predicate evaluator
   */
  private static final DefaultResourcePredicateEvaluator
    DEFAULT_RESOURCE_PREDICATE_EVALUATOR =
    new DefaultResourcePredicateEvaluator();

  // ----- Constructors ------------------------------------------------------

  public ClusterControllerImpl(ProviderModule providerModule) {
    this.providerModule = providerModule;
  }



  // ----- ClusterController -------------------------------------------------

  @Override
  public Predicate getAmendedPredicate(Type type, Predicate predicate) {
    ExtendedResourceProviderWrapper provider = ensureResourceProviderWrapper(type);
    ensurePropertyProviders(type);

    return provider.getAmendedPredicate(predicate);
  }

  @Override
  public QueryResponse getResources(Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException, NoSuchResourceException,
             NoSuchParentResourceException, SystemException {
    QueryResponse queryResponse = null;

    ExtendedResourceProviderWrapper provider = ensureResourceProviderWrapper(type);
    ensurePropertyProviders(type);

    if (provider != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Using resource provider {} for request type {}", provider.getClass().getName(), type);
      }
      // make sure that the providers can satisfy the request
      checkProperties(type, request, predicate);

      // get the resources
      queryResponse = provider.queryForResources(request, predicate);
    }
    return queryResponse == null ? new QueryResponseImpl(Collections.emptySet()) : queryResponse;
  }

  @Override
  public Set<Resource> populateResources(Type type,
                                         Set<Resource> resources,
                                         Request request,
                                         Predicate predicate) throws SystemException {
    Set<Resource> keepers = resources;
    List<PropertyProvider> propertyProviders = ensurePropertyProviders(type);
    for (PropertyProvider propertyProvider : propertyProviders) {
      if (providesRequestProperties(propertyProvider, request, predicate)) {
        keepers = propertyProvider.populateResources(keepers, request, predicate);
      }
    }
    return keepers;
  }

  @Override
  public Iterable<Resource> getIterable(Type type, QueryResponse queryResponse,
                                        Request request, Predicate predicate,
                                        PageRequest pageRequest,
                                        SortRequest sortRequest)
      throws NoSuchParentResourceException, UnsupportedPropertyException, NoSuchResourceException, SystemException {
    return getPage(type, queryResponse, request, predicate, pageRequest, sortRequest).getIterable();
  }

  @Override
  public PageResponse getPage(Type type, QueryResponse queryResponse,
                              Request request, Predicate predicate,
                              PageRequest pageRequest, SortRequest sortRequest)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Resource> providerResources = queryResponse.getResources();

    ExtendedResourceProviderWrapper provider  = ensureResourceProviderWrapper(type);

    int totalCount = 0;
    Set<Resource> resources = providerResources;

    if (!providerResources.isEmpty()) {
      // determine if the provider has already paged & sorted the results
      boolean providerAlreadyPaged  = queryResponse.isPagedResponse();
      boolean providerAlreadySorted = queryResponse.isSortedResponse();

      // conditionally create a comparator if there is a sort
      Comparator<Resource> resourceComparator = comparator;
      if (null != sortRequest) {
        checkSortRequestProperties(sortRequest, type, provider);
        resourceComparator = new ResourceComparator(sortRequest);
      }

      // if the provider did not already sort the set, then sort it based
      // on the comparator
      if (!providerAlreadySorted) {
        TreeSet<Resource> sortedResources = new TreeSet<>(
          resourceComparator);

        sortedResources.addAll(providerResources);
        resources = sortedResources;
      }

      // start out assuming that the results are not paged and that
      // the total count is the size of the provider resources
      totalCount = resources.size();

      // conditionally page the results
      if (null != pageRequest && !providerAlreadyPaged) {
        switch (pageRequest.getStartingPoint()) {
          case Beginning:
            return getPageFromOffset(pageRequest.getPageSize(), 0, resources,
                predicate, provider);
          case End:
            return getPageToOffset(pageRequest.getPageSize(), -1, resources,
                predicate, provider);
          case OffsetStart:
            return getPageFromOffset(pageRequest.getPageSize(),
                pageRequest.getOffset(), resources, predicate, provider);
          case OffsetEnd:
            return getPageToOffset(pageRequest.getPageSize(),
                pageRequest.getOffset(), resources, predicate, provider);
          case PredicateStart:
          case PredicateEnd:
            // TODO : need to support the following cases for pagination
            break;
          default:
            break;
        }
      } else if (providerAlreadyPaged) {
        totalCount = queryResponse.getTotalResourceCount();
      }
    }

    return new PageResponseImpl(new ResourceIterable(resources, predicate,
        provider), 0, null, null, totalCount);
  }

  /**
   * Check whether properties specified with a @SortRequest are supported by
   * the @ResourceProvider.
   *
   * @param sortRequest @SortRequest
   * @param type @Type
   * @param provider @ResourceProvider
   * @throws UnsupportedPropertyException
   */
  private void checkSortRequestProperties(SortRequest sortRequest, Type type,
                                          ResourceProvider provider) throws UnsupportedPropertyException {
    Set<String> requestPropertyIds = provider.checkPropertyIds(
      new HashSet<>(sortRequest.getPropertyIds()));

    if (requestPropertyIds.size() > 0) {
      List<PropertyProvider> propertyProviders = ensurePropertyProviders(type);
      for (PropertyProvider propertyProvider : propertyProviders) {
        requestPropertyIds = propertyProvider.checkPropertyIds(requestPropertyIds);
        if (requestPropertyIds.size() == 0) {
          return;
        }
      }
      throw new UnsupportedPropertyException(type, requestPropertyIds);
    }
  }

  @Override
  public Schema getSchema(Type type) {
    Schema schema = schemas.get(type);

    if (schema == null) {
      synchronized (schemas) {
        schema = schemas.get(type);
        if (schema == null) {
          schema = new SchemaImpl(ensureResourceProvider(type));
          schemas.put(type, schema);
        }
      }
    }

    return schema;
  }

  @Override
  public RequestStatus createResources(Type type, Request request)
      throws UnsupportedPropertyException,
             SystemException,
    ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    ResourceProvider provider = ensureResourceProvider(type);
    if (provider != null) {

      checkProperties(type, request, null);

      return provider.createResources(request);
    }
    return null;
  }

  @Override
  public RequestStatus updateResources(Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    ResourceProvider provider = ensureResourceProvider(type);
    if (provider != null) {

      if (!checkProperties(type, request, predicate)) {
        predicate = resolvePredicate(type, predicate);
        if (predicate == null) {
          return null;
        }
      }
      return provider.updateResources(request, predicate);
    }
    return null;
  }

  @Override
  public RequestStatus deleteResources(Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    ResourceProvider provider = ensureResourceProvider(type);
    if (provider != null) {
      if (!checkProperties(type, null, predicate)) {
        predicate = resolvePredicate(type, predicate);
        if (predicate == null) {
          return null;
        }
      }
      return provider.deleteResources(request, predicate);
    }
    return null;
  }


  /**
   * Provides a non-wrapped resource provider..
   *
   * @param type  type of resource provider to obtain
   * @return a non-wrapped resource provider
   */
  @Override
  public ResourceProvider ensureResourceProvider(Type type) {
    //todo: in some cases it is necessary to down cast the returned resource provider
    //todo: to a concrete type.  Perhaps we can provided a 'T getDelegate()' method
    //todo: on the wrapper so no casting would be necessary.
    ExtendedResourceProviderWrapper providerWrapper = ensureResourceProviderWrapper(type);
    return providerWrapper == null ? null : providerWrapper.resourceProvider;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the extended resource provider for the given type, creating it if required.
   *
   * @param type  the resource type
   *
   * @return the resource provider
   */
  private ExtendedResourceProviderWrapper ensureResourceProviderWrapper(Type type) {
    synchronized (resourceProviders) {
      if (!resourceProviders.containsKey(type)) {
        resourceProviders.put(type, new ExtendedResourceProviderWrapper(providerModule.getResourceProvider(type)));
      }
    }
    return resourceProviders.get(type);
  }

  /**
   * Get an iterable set of resources filtered by the given request and
   * predicate objects.
   *
   * @param type       type of resources
   * @param request    the request
   * @param predicate  the predicate object which filters which resources are returned
   *
   * @return a page response representing the requested page of resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  protected Iterable<Resource> getResourceIterable(Type type, Request request,
                                                   Predicate predicate)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchParentResourceException,
      NoSuchResourceException {
    return getResources(type, request, predicate, null, null).getIterable();
  }

  /**
   * Get a page of resources filtered by the given request, predicate objects and
   * page request.
   *
   * @param type         type of resources
   * @param request      the request
   * @param predicate    the predicate object which filters which resources are returned
   * @param pageRequest  the page request for a paginated response
   *
   * @return a page response representing the requested page of resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  protected  PageResponse getResources(Type type, Request request, Predicate predicate,
                                       PageRequest pageRequest, SortRequest sortRequest)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    QueryResponse queryResponse = getResources(type, request, predicate);
    populateResources(type, queryResponse.getResources(), request, predicate);
    return getPage(type, queryResponse, request, predicate, pageRequest, sortRequest);
  }

  /**
   * Check to make sure that all the property ids specified in the given request and
   * predicate are supported by the resource provider or property providers for the
   * given type.
   *
   * @param type       the resource type
   * @param request    the request
   * @param predicate  the predicate
   *
   * @return true if all of the properties specified in the request and predicate are supported by
   *         the resource provider for the given type; false if any of the properties specified in
   *         the request and predicate are not supported by the resource provider but are supported
   *         by a property provider for the given type.
   *
   * @throws UnsupportedPropertyException thrown if any of the properties specified in the request
   *                                      and predicate are not supported by either the resource
   *                                      provider or a property provider for the given type
   */
  private boolean checkProperties(Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException {
    Set<String> requestPropertyIds = request == null ? new HashSet<>() :
        PropertyHelper.getAssociatedPropertyIds(request);

    if (predicate != null) {
      requestPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }

    if (requestPropertyIds.size() > 0) {
      ResourceProvider provider = ensureResourceProvider(type);
      requestPropertyIds = provider.checkPropertyIds(requestPropertyIds);

      if (requestPropertyIds.size() > 0) {
        List<PropertyProvider> propertyProviders = ensurePropertyProviders(type);
        for (PropertyProvider propertyProvider : propertyProviders) {
          requestPropertyIds = propertyProvider.checkPropertyIds(requestPropertyIds);
          if (requestPropertyIds.size() == 0) {
            return false;
          }
        }
        throw new UnsupportedPropertyException(type, requestPropertyIds);
      }
    }
    return true;
  }

  /**
   * Check to see if any of the property ids specified in the given request and
   * predicate are handled by an associated property provider.  if so, then use
   * the given predicate to obtain a new predicate that can be completely
   * processed by an update or delete operation on a resource provider for
   * the given resource type.  This means that the new predicate should only
   * reference the key property ids for this type.
   *
   * @param type       the resource type
   * @param predicate  the predicate
   *
   * @return the given predicate if a new one is not required; a new predicate if required
   *
   * @throws UnsupportedPropertyException thrown if any of the properties specified in the request
   *                                      and predicate are not supported by either the resource
   *                                      provider or a property provider for the given type
   *
   * @throws SystemException thrown for internal exceptions
   * @throws NoSuchResourceException if the resource that is requested doesn't exist
   * @throws NoSuchParentResourceException if a parent resource of the requested resource doesn't exist
   */
  private Predicate resolvePredicate(Type type, Predicate predicate)
    throws UnsupportedPropertyException,
        SystemException,
        NoSuchResourceException,
        NoSuchParentResourceException{

    ResourceProvider provider = ensureResourceProvider(type);

    Set<String>  keyPropertyIds = new HashSet<>(provider.getKeyPropertyIds().values());
    Request      readRequest    = PropertyHelper.getReadRequest(keyPropertyIds);

    Iterable<Resource> resources = getResourceIterable(type, readRequest, predicate);

    PredicateBuilder pb = new PredicateBuilder();
    PredicateBuilder.PredicateBuilderWithPredicate pbWithPredicate = null;

    for (Resource resource : resources) {
      if (pbWithPredicate != null) {
        pb = pbWithPredicate.or();
      }

      pb              = pb.begin();
      pbWithPredicate = null;

      for (String keyPropertyId : keyPropertyIds) {
        if (pbWithPredicate != null) {
          pb = pbWithPredicate.and();
        }
        pbWithPredicate =
            pb.property(keyPropertyId).equals((Comparable) resource.getPropertyValue(keyPropertyId));
      }
      if (pbWithPredicate != null) {
        pbWithPredicate = pbWithPredicate.end();
      }
    }
    return pbWithPredicate == null ? null : pbWithPredicate.toPredicate();
  }

  /**
   * Indicates whether or not the given property provider can service the given request.
   *
   * @param provider   the property provider
   * @param request    the request
   * @param predicate  the predicate
   *
   * @return true if the given provider can service the request
   */
  private boolean providesRequestProperties(PropertyProvider provider, Request request, Predicate predicate) {
    Set<String> requestPropertyIds = new HashSet<>(request.getPropertyIds());

    if (requestPropertyIds.size() == 0) {
      return true;
    }
    requestPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));

    int size = requestPropertyIds.size();

    return size > provider.checkPropertyIds(requestPropertyIds).size();
  }

  /**
   * Get the list of property providers for the given type.
   *
   * @param type  the resource type
   *
   * @return the list of property providers
   */
  private List<PropertyProvider> ensurePropertyProviders(Type type) {
    synchronized (propertyProviders) {
      if (!propertyProviders.containsKey(type)) {
        List<PropertyProvider> providers = providerModule.getPropertyProviders(type);
        propertyProviders.put(type,
            providers == null ? Collections.emptyList() : providers);
      }
    }
    return propertyProviders.get(type);
  }

  /**
   * Evaluate the predicate and create a list of filtered resources
   *
   * @param resourceIterable @ResourceIterable
   * @return @LinkedList of filtered resources
   */
  private LinkedList<Resource> getEvaluatedResources(ResourceIterable
                                              resourceIterable) {
    LinkedList<Resource> resources = new LinkedList<>();
    if (resourceIterable != null) {
      for (Resource resource : resourceIterable) {
        resources.add(resource);
      }
    }
    return resources;
  }

  /**
   * Get one page of resources from the given set of resources starting at the given offset.
   *
   * @param pageSize   the page size
   * @param offset     the offset
   * @param resources  the set of resources
   * @param predicate  the predicate
   *
   * @return a page response containing a page of resources
   */
  private PageResponse getPageFromOffset(int pageSize, int offset,
      Set<Resource> resources,
                                         Predicate predicate,
                                         ResourcePredicateEvaluator evaluator) {

    int currentOffset = 0;
    Resource previous      = null;
    Set<Resource> pageResources = new LinkedHashSet<>();
    LinkedList<Resource> filteredResources =
      getEvaluatedResources(new ResourceIterable(resources, predicate, evaluator));
    Iterator<Resource> iterator = filteredResources.iterator();

    // skip till offset
    while (currentOffset < offset && iterator.hasNext()) {
      previous = iterator.next();
      ++currentOffset;
    }

    // get a page worth of resources
    for (int i = 0; i < pageSize && iterator.hasNext(); ++i) {
      pageResources.add(iterator.next());
    }

    return new PageResponseImpl(pageResources,
        currentOffset,
        previous,
        iterator.hasNext() ? iterator.next() : null,
        filteredResources.size()
      );
  }

  /**
   * Get one page of resources from the given set of resources ending at the given offset.
   *
   * @param pageSize   the page size
   * @param offset     the offset; -1 indicates the end of the resource set
   * @param resources  the set of resources
   * @param predicate  the predicate
   *
   * @return a page response containing a page of resources
   */
  private PageResponse getPageToOffset(int pageSize, int offset,
      Set<Resource> resources,
                                       Predicate predicate,
                                       ResourcePredicateEvaluator evaluator) {

    int                currentOffset = resources.size() - 1;
    Resource           next          = null;
    List<Resource>     pageResources = new LinkedList<>();
    LinkedList<Resource> filteredResources =
      getEvaluatedResources(new ResourceIterable(resources, predicate, evaluator));
    Iterator<Resource> iterator = filteredResources.descendingIterator();

    if (offset != -1) {
      // skip till offset
      while (currentOffset > offset && iterator.hasNext()) {
        next = iterator.next();
        --currentOffset;
      }
    }

    // get a page worth of resources
    for (int i = 0; i < pageSize && iterator.hasNext(); ++i) {
      pageResources.add(0, iterator.next());
      --currentOffset;
    }

    return new PageResponseImpl(pageResources,
        currentOffset + 1,
        iterator.hasNext() ? iterator.next() : null,
        next,
        filteredResources.size()
      );
  }

  /**
   * Get the associated resource comparator.
   *
   * @return the resource comparator
   */
  protected Comparator<Resource> getComparator() {
    return comparator;
  }


  // ----- ResourceIterable inner class --------------------------------------

  private static class ResourceIterable implements Iterable<Resource> {

    /**
     * The resources to iterate over.
     */
    private final Set<Resource> resources;

    /**
     * The predicate used to filter the set.
     */
    private final Predicate predicate;

    /**
     * The predicate evaluator.
     */
    private final ResourcePredicateEvaluator evaluator;


    // ----- Constructors ----------------------------------------------------

    /**
     * Create a ResourceIterable.
     *
     * @param resources  the set of resources to iterate over
     * @param predicate  the predicate used to filter the set of resources
     * @param evaluator  the evaluator used to evaluate with the given predicate
     */
    private ResourceIterable(Set<Resource> resources, Predicate predicate,
                             ResourcePredicateEvaluator evaluator) {
      this.resources = resources;
      this.predicate = predicate;
      this.evaluator = evaluator;
    }

    // ----- Iterable --------------------------------------------------------

    @Override
    public Iterator<Resource> iterator() {
      return new ResourceIterator(resources, predicate, evaluator);
    }
  }


  // ----- ResourceIterator inner class --------------------------------------

  private static class ResourceIterator implements Iterator<Resource> {

    /**
     * The underlying iterator.
     */
    private final Iterator<Resource> iterator;

    /**
     * The predicate used to filter the resource being iterated over.
     */
    private final Predicate predicate;

    /**
     * The next resource.
     */
    private Resource nextResource;

    /**
     * The predicate evaluator.
     */
    private final ResourcePredicateEvaluator evaluator;

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a new ResourceIterator.
     *
     * @param resources  the set of resources to iterate over
     * @param predicate  the predicate used to filter the set of resources
     * @param evaluator  the evaluator used to evaluate with the given predicate
     */
    private ResourceIterator(Set<Resource> resources, Predicate predicate,
                             ResourcePredicateEvaluator evaluator) {
      iterator     = resources.iterator();
      this.predicate    = predicate;
      this.evaluator    = evaluator;
      nextResource = getNextResource();
    }

    // ----- Iterator --------------------------------------------------------

    @Override
    public boolean hasNext() {
      return nextResource != null;
    }

    @Override
    public Resource next() {
      if (nextResource == null) {
        throw new NoSuchElementException("Iterator has no more elements.");
      }

      Resource currentResource = nextResource;
      nextResource = getNextResource();

      return currentResource;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported.");
    }

    // ----- helper methods --------------------------------------------------

    /**
     * Get the next resource.
     *
     * @return the next resource.
     */
    private Resource getNextResource() {
      while (iterator.hasNext()) {
        Resource next = iterator.next();
        if (predicate == null || evaluator.evaluate(predicate, next)) {
          return next;
        }
      }
      return null;
    }
  }

  // ----- ResourceComparator inner class ------------------------------------

  protected class ResourceComparator implements Comparator<Resource> {
    SortRequest sortRequest;

    /**
     * Default comparator
     */
    protected ResourceComparator() {
    }

    /**
     * Sort by properties.
     * @param sortRequest @SortRequest to sort by.
     */
    protected ResourceComparator(SortRequest sortRequest) {
      this.sortRequest = sortRequest;
    }

    @Override
    public int compare(Resource resource1, Resource resource2) {
      Type resourceType = resource1.getType();

      // compare based on resource type
      int compVal = resourceType.compareTo(resource2.getType());

      // compare based on requested properties
      if (compVal == 0 && sortRequest != null) {
        for (SortRequestProperty property : sortRequest.getProperties()) {
          compVal = compareValues(
            resource1.getPropertyValue(property.getPropertyId()),
            resource2.getPropertyValue(property.getPropertyId()),
            property.getOrder());

          if (compVal != 0) {
            return compVal;
          }
        }
      }

      // compare based on resource key properties
      if (compVal == 0) {
        Schema schema = getSchema(resourceType);

        for (Type type : schema.getKeyTypes()) {
          String keyPropertyId = schema.getKeyPropertyId(type);
          if (keyPropertyId != null) {
            compVal = compareValues(resource1.getPropertyValue(keyPropertyId),
                                    resource2.getPropertyValue(keyPropertyId));
            if (compVal != 0 ) {
              return compVal;
            }
          }
        }
      }

      // compare based on the resource strings
      return resource1.toString().compareTo(resource2.toString());
    }

    // compare two values and account for null
    @SuppressWarnings("unchecked")
    private int compareValues(Object val1, Object val2) {

      if (val1 == null || val2 == null) {
        return val1 == null && val2 == null ? 0 : val1 == null ? -1 : 1;
      }

      if (val1 instanceof Comparable) {
        try {
          return ((Comparable) val1).compareTo(val2);
        } catch (ClassCastException e) {
          return 0;
        }
      }
      return 0;
    }

    // compare two values respecting order specifier
    private int compareValues(Object val1, Object val2, SortRequest.Order order) {
      if (order == SortRequest.Order.ASC) {
        return compareValues(val1, val2);
      } else {
        return -1 * compareValues(val1, val2);
      }
    }
  }


  // ----- inner class : ExtendedResourceProviderWrapper ---------------------

  /**
   * Wrapper class that allows the cluster controller to treat all resource providers the same.
   */
  private static class ExtendedResourceProviderWrapper implements ExtendedResourceProvider, ResourcePredicateEvaluator {

    /**
     * The delegate resource provider.
     */
    private final ResourceProvider resourceProvider;

    /**
     * The delegate predicate evaluator. {@code null} if the given delegate resource provider is not an
     * instance of {@link ResourcePredicateEvaluator}
     */
    private final ResourcePredicateEvaluator evaluator;

    /**
     * The delegate extended resource provider.  {@code null} if the given delegate resource provider is not an
     * instance of {@link ExtendedResourceProvider}
     */
    private final ExtendedResourceProvider extendedResourceProvider;


    // ----- Constructors ----------------------------------------------------

    /**
     * Constructor.
     *
     * @param resourceProvider  the delegate resource provider
     */
    public ExtendedResourceProviderWrapper(ResourceProvider resourceProvider) {
      this.resourceProvider = resourceProvider;

      extendedResourceProvider = resourceProvider instanceof ExtendedResourceProvider ?
          (ExtendedResourceProvider) resourceProvider : null;

      evaluator = resourceProvider instanceof ResourcePredicateEvaluator ?
          (ResourcePredicateEvaluator) resourceProvider : DEFAULT_RESOURCE_PREDICATE_EVALUATOR;
    }


    /**
     * @return the amended predicate, or {@code null} to use the provided one
     */
    public Predicate getAmendedPredicate(Predicate predicate) {
      if (ReadOnlyResourceProvider.class.isInstance(resourceProvider)) {
        return ((ReadOnlyResourceProvider) resourceProvider).amendPredicate(predicate);
      } else {
        return null;
      }
    }


    // ----- ExtendedResourceProvider ----------------------------------------

    @Override
    public QueryResponse queryForResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return extendedResourceProvider == null ?
          new QueryResponseImpl(resourceProvider.getResources(request, predicate)) :
          extendedResourceProvider.queryForResources(request, predicate);
    }


    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request)
        throws SystemException, UnsupportedPropertyException,
               ResourceAlreadyExistsException, NoSuchParentResourceException {
      return resourceProvider.createResources(request);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return resourceProvider.getResources(request, predicate);
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return resourceProvider.updateResources(request, predicate);
    }

    @Override
    public RequestStatus deleteResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return resourceProvider.deleteResources(request, predicate);
    }

    @Override
    public Map<Type, String> getKeyPropertyIds() {
      return resourceProvider.getKeyPropertyIds();
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return resourceProvider.checkPropertyIds(propertyIds);
    }


    // ----- ResourcePredicateEvaluator --------------------------------------

    @Override
    public boolean evaluate(Predicate predicate, Resource resource) {
      return evaluator.evaluate(predicate, resource);
    }
  }
}
