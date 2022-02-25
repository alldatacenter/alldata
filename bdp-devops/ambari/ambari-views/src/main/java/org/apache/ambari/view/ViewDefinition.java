/**
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

package org.apache.ambari.view;

/**
 * The view definition.
 */
public interface ViewDefinition {

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName();

  /**
   * Get the view label (display name).
   *
   * @return the view label
   */
  public String getLabel();

  /**
   * Get the view description.
   *
   * @return the description
   */
  public String getDescription();

  /**
   * Get the view version.
   *
   * @return the version
   */
  public String getVersion();

  /**
   * Get the view build number.
   *
   * @return the build number
   */
  public String getBuild();

  /**
   * Get the mask class name.
   *
   * @return the mask class name.
   */
  public String getMask();

  /**
   * Get the view status.
   *
   * @return the view status
   */
  public ViewStatus getStatus();

  /**
   * Get the view status detail.
   *
   * @return the view status detail
   */
  public String getStatusDetail();


  // ----- ViewStatus enum ---------------------------------------------------

  /**
   * View status
   */
  public enum ViewStatus {
    PENDING,   // view has been created but not loaded from the archive
    DEPLOYING, // view is in the process of being deployed from the archive
    DEPLOYED,  // view is completely deployed and ready to use
    ERROR      // an error occurred deploying the view
  }
}
