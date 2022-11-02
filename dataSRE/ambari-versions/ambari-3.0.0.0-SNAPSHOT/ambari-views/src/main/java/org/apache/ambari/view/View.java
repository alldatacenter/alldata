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
 * Interface for main view class.
 */
public interface View {

  /**
   * Called by the view framework when the given view is being deployed.
   *
   * @param definition  the view definition
   */
  public void onDeploy(ViewDefinition definition);

  /**
   * Called by the view framework when the given instance is being created.
   *
   * @param definition  the view instance definition
   */
  public void onCreate(ViewInstanceDefinition definition);

  /**
   * Called by the view framework when the given instance is being destroyed.
   *
   * @param definition  the view instance definition
   */
  public void onDestroy(ViewInstanceDefinition definition);

  /**
   * Called by the view framework when the given instance is being updated
   *
   * @param definition
   */
  public void onUpdate(ViewInstanceDefinition definition);
}
