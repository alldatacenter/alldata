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

package org.apache.ambari.server.stack;


import java.util.Map;

import org.apache.ambari.server.AmbariException;

/**
 * Stack Definition Module.
 * Represents a module within a stack definition tree.  For each stack version specified in
 * a stack definition a tree will exist with a stack module being the root node.  Each module
 * may have a parent as well as child modules.  Each module has an associated "info" object
 * which contains the underlying state that is being wrapped by the module.
 */
public interface StackDefinitionModule <T, I> {
  /**
   * Resolve the module state with the specified parent.
   *
   * @param parent          the parent that this module will be merged with
   * @param allStacks       collection of all stack modules in the tree
   * @param commonServices  collection of all common service modules in the tree
   * @param extensions  collection of all extension modules in the tree
   *
   * @throws AmbariException if resolution fails
   */
  void resolve(T parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions) throws AmbariException;

  /**
   * Obtain the associated module information.
   *
   * @return associated module information
   */
  I getModuleInfo();

  /**
   * Determine whether the module has been marked for deletion.
   *
   * @return true if the module is marked for deletion; otherwise false
   */
  boolean isDeleted();

  /**
   * Obtain the id of the module.
   *
   * @return module id
   */
  String getId();

  /**
   * Lifecycle even which is called when the associated stack has been fully resolved.
   */
  void finalizeModule();

  /**
   * Module state.
   * Initial state is INIT.
   * When resolve is called state is set to VISITED.
   * When resolve completes, state is set to RESOLVED.
   *
   * @return the module state
   */
  ModuleState getModuleState();
  
  /**
   * 
   * @return valid module flag
   */
  boolean isValid();

  /**
   * 
   * @param valid set validity flag
   */
  void setValid(boolean valid);
}
