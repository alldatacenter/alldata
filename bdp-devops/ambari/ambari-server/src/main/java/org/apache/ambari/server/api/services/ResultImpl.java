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
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;


/**
 * Result implementation.
 */
public class ResultImpl implements Result {

  /**
   * Whether the request was handled synchronously.
   */
  private boolean m_synchronous;

  /**
   * Result status.
   */
  private ResultStatus m_status;

  /**
   * Result metadata.
   */
  private ResultMetadata m_resultMetadata;

  /**
   * Tree structure which holds the results
   */
  private TreeNode<Resource> m_tree = new TreeNodeImpl<>(null, null, null);


  /**
   * Constructor.
   *
   * @param synchronous true if request was handled synchronously, false otherwise
   */
  public ResultImpl(boolean synchronous) {
    m_synchronous = synchronous;
  }

  /**
   * Constructor.
   *
   * @param status  result status
   */
  public ResultImpl(ResultStatus status) {
    m_status = status;
  }

  @Override
  public TreeNode<Resource> getResultTree() {
    return m_tree;
  }

  @Override
  public boolean isSynchronous() {
    return m_synchronous;
  }

  @Override
  public ResultStatus getStatus() {
    return m_status;
  }

  @Override
  public void setResultStatus(ResultStatus status) {
    m_status = status;
  }

  @Override
  public void setResultMetadata(ResultMetadata resultMetadata) {
    m_resultMetadata = resultMetadata;
  }

  @Override
  public ResultMetadata getResultMetadata() {
    return m_resultMetadata;
  }

}

