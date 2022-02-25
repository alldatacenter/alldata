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


package org.apache.ambari.server.api.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.security.authorization.AuthorizationException;

/**
 * Handler for creates that are applied to the results of a query.
 */
public class QueryCreateHandler extends BaseManagementHandler {

  private RequestHandler m_readHandler = new ReadHandler();

  @Override
  public Result handleRequest(Request request) {
    Result queryResult = getReadHandler().handleRequest(request);
    if (queryResult.getStatus().isErrorState() ||
        queryResult.getResultTree().getChildren().isEmpty()) {

      // if query result has error state or contains no resources just return it.
      // currently returns 200 for case where query returns no rows
      return queryResult;
    }

    Map<Resource.Type, Set<Map<String, Object>>> mapProperties;
    try {
      mapProperties = buildCreateSet(request, queryResult);
    } catch (IllegalArgumentException e) {
      return createInvalidRequestResult(e.getMessage());
    }

    if (mapProperties.size() != 1) {
      return createInvalidRequestResult(mapProperties.size() == 0 ?
          "A minimum of one sub-resource must be specified for creation." :
          "Multiple sub-resource types may not be created in the same request.");
    }

    // only get first element because we currently only support creation of a single sub-resource type
    final Map.Entry<Resource.Type, Set<Map<String, Object>>> entry = mapProperties.entrySet().iterator().next();
    ResourceInstance createResource = getResourceFactory().createResource(
        entry.getKey(), request.getResource().getKeyValueMap());

    RequestBody requestBody = new RequestBody();
    requestBody.setBody(request.getBody().getBody());
    for (Map<String, Object> map : entry.getValue()) {
      requestBody.addPropertySet(new NamedPropertySet("", map));
    }

    return persist(createResource,
        requestBody
      );
  }

  /**
   * Build the property set for all sub-resource to be created.
   * This includes determining the sub-resource type and creating a property set for each matching parent.
   *
   * @param request      the current request
   * @param queryResult  the result of the query for matching parents
   *
   * @return a map of sub-resource types to be created and their associated properties
   *
   * @throws IllegalArgumentException  if no sub-resource type was specified or it is not a valid
   *                                   sub-resource of the parent.
   */
  private Map<Resource.Type, Set<Map<String, Object>>> buildCreateSet(Request request, Result queryResult)
    throws IllegalArgumentException {

    Set<NamedPropertySet> setRequestProps = request.getBody().getNamedPropertySets();

    HashMap<Resource.Type, Set<Map<String, Object>>> mapProps =
      new HashMap<>();

    ResourceInstance  resource            = request.getResource();
    Resource.Type     type                = resource.getResourceDefinition().getType();
    ClusterController controller          = getClusterController();
    String            resourceKeyProperty = controller.getSchema(type).getKeyPropertyId(type);

    TreeNode<Resource> tree = queryResult.getResultTree();
    Collection<TreeNode<Resource>> treeChildren = tree.getChildren();
    for (TreeNode<Resource> node : treeChildren) {
      Resource r = node.getObject();
      Object keyVal = r.getPropertyValue(resourceKeyProperty);

      for (NamedPropertySet namedProps : setRequestProps) {
        for (Map.Entry<String, Object> entry : namedProps.getProperties().entrySet()) {
          Set<Map<String, Object>> set = (Set<Map<String, Object>>) entry.getValue();
          for (Map<String, Object> map : set) {
            Map<String, Object> mapResourceProps = new HashMap<>(map);
            Resource.Type       createType       = getCreateType(resource, entry.getKey());
            mapResourceProps.put(controller.getSchema(createType).
                getKeyPropertyId(resource.getResourceDefinition().getType()), keyVal);
            Set<Map<String, Object>> setCreateProps = mapProps.get(createType);
            if (setCreateProps == null) {
              setCreateProps = new HashSet<>();
              mapProps.put(createType, setCreateProps);
            }
            setCreateProps.add(mapResourceProps);
          }
        }
      }
    }
    return mapProps;
  }

  /**
   * Determine the sub-resource type(s) to be created.
   *
   * @param resource         the requests resource instance
   * @param subResourceName  the name of the sub-resource to be created
   * @return  the resource type
   *
   * @throws IllegalArgumentException  if the specified sub-resource name is empty or it is not a valid
   *                                   sub-resource of the parent.
   */
  private Resource.Type getCreateType(ResourceInstance resource, String subResourceName) throws IllegalArgumentException{
    if (subResourceName == null || subResourceName.equals("")) {
      throw new IllegalArgumentException("A sub-resource name must be supplied.");
    }
    ResourceInstance res = resource.getSubResources().get(subResourceName);

    if (res == null) {
      throw new IllegalArgumentException("The specified sub-resource name is not valid: '" + subResourceName + "'.");
    }

    return res.getResourceDefinition().getType();
  }

  /**
   * Convenience method to create a result for invalid requests.
   *
   * @param msg  message indicating why the request is invalid
   *
   * @return  a request with a 400 status and msg set
   */
  private Result createInvalidRequestResult(String msg) {
    return new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, "Invalid Request: " + msg));
  }

  @Override
  protected Result persist(ResourceInstance resource, RequestBody body) {
    Result result;
    try {
      RequestStatus status = getPersistenceManager().create(resource, body);
      result = createResult(status);

      if (result.isSynchronous()) {
        if (resource.getResourceDefinition().isCreatable()) {
          result.setResultStatus(new ResultStatus(ResultStatus.STATUS.CREATED));
        } else {
          result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
        }
      } else {
        result.setResultStatus(new ResultStatus(ResultStatus.STATUS.ACCEPTED));
      }
    } catch (AuthorizationException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.FORBIDDEN, e.getMessage()));
    } catch (UnsupportedPropertyException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e));
    } catch (ResourceAlreadyExistsException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.CONFLICT, e));
    } catch (NoSuchParentResourceException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e));
    } catch (SystemException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e));
    }

    return result;
  }

  @Override
  protected ResultMetadata convert(RequestStatusMetaData requestStatusMetaData) {
    if (requestStatusMetaData == null) {
      return null;
    }

    throw new UnsupportedOperationException();
  }

  /**
   * Get the resource factory instance.
   * @return  a factory for creating resource instances
   */
  protected ResourceInstanceFactory getResourceFactory() {
    //todo: inject
    return new ResourceInstanceFactoryImpl();
  }

  /**
   * Read handler instance.  Used for obtaining matching parents which match the query.
   *
   * @return  read handler instance
   */
  protected RequestHandler getReadHandler() {
    return m_readHandler;
  }
}
