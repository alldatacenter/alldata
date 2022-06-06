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

package org.apache.ambari.server.api.services;


import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Represents a result from a request handler invocation.
 */
public interface Result {

  /**
   * Obtain the results of the request invocation as a Tree structure.
   *
   * @return the results of the request as a Tree structure
   */
  TreeNode<Resource> getResultTree();

  /**
   * Determine whether the request was handled synchronously.
   * If the request is synchronous, all work was completed prior to returning.
   *
   * @return true if the request was synchronous, false if it was asynchronous
   */
  boolean isSynchronous();

  ResultStatus getStatus();

  void setResultStatus(ResultStatus status);

  void setResultMetadata(ResultMetadata resultMetadata);

  ResultMetadata getResultMetadata();
}
