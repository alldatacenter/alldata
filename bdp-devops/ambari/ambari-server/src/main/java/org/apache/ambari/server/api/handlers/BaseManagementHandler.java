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

import java.util.Set;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.api.services.persistence.PersistenceManager;
import org.apache.ambari.server.api.services.persistence.PersistenceManagerImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

/**
 * Base handler for operations that persist state to the back-end.
 */
public abstract class BaseManagementHandler implements RequestHandler {

  public static final String RESOURCES_NODE_NAME = "resources";

  /**
   * PersistenceManager implementation.
   */
  PersistenceManager m_pm = new PersistenceManagerImpl(getClusterController());

  /**
   * Constructor.
   */
  protected BaseManagementHandler() {
  }


  @Override
  public Result handleRequest(Request request) {
    Query query = request.getResource().getQuery();
    Predicate queryPredicate = request.getQueryPredicate();

    query.setRenderer(request.getRenderer());
    if (queryPredicate != null) {
      query.setUserPredicate(queryPredicate);
    }
    return persist(request.getResource(), request.getBody());
  }

  /**
   * Create a result from a request status.
   *
   * @param requestStatus  the request status to build the result from.
   *
   * @return  a Result instance for the provided request status
   */
  protected Result createResult(RequestStatus requestStatus) {

    boolean            isSynchronous = requestStatus.getStatus() == RequestStatus.Status.Complete;
    Result             result        = new ResultImpl(isSynchronous);
    TreeNode<Resource> tree          = result.getResultTree();

    if (! isSynchronous) {
      tree.addChild(requestStatus.getRequestResource(), "request");
    }

    Set<Resource> setResources = requestStatus.getAssociatedResources();
    if (! setResources.isEmpty()) {
      TreeNode<Resource> resourcesNode = tree.addChild(null, RESOURCES_NODE_NAME);
      resourcesNode.setProperty("isCollection", "true");

      int count = 1;
      for (Resource resource : setResources) {
        //todo: provide a more meaningful node name
        resourcesNode.addChild(resource, resource.getType() + ":" + count++);
      }
    }
    result.setResultMetadata(convert(requestStatus.getStatusMetadata()));
    return result;
  }


  //todo: inject ClusterController, PersistenceManager

  /**
   * Get the cluster controller instance.
   *
   * @return cluster controller
   */
  protected ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  /**
   * Get the persistence manager instance.
   *
   * @return persistence manager
   */
  protected PersistenceManager getPersistenceManager() {
    return m_pm;
  }

  /**
   * Persist the operation to the back end.
   *
   * @param resource  associated resource
   * @param body      associated request body
   *
   * @return the result of the persist operation
   */
  protected abstract Result persist(ResourceInstance resource, RequestBody body);

  /**
   * Convert {@link RequestStatusMetaData} object to {@link ResultMetadata} which will be
   * included in {@link Result} object.
   * @param requestStatusMetaData request status details
   * @return result details
   */
  protected abstract ResultMetadata convert(RequestStatusMetaData requestStatusMetaData);
}
