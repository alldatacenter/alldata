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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;

/**
 * Abstract base service definition module.
 * Provides functionality that is common across multiple modules.
 */
public abstract class BaseModule<T, I> implements StackDefinitionModule<T, I> {

  /**
   * Module visitation state
   */
  protected ModuleState moduleState = ModuleState.INIT;

  /**
   * Module state.
   * Initial state is INIT.
   * When resolve is called state is set to VISITED.
   * When resolve completes, state is set to RESOLVED.
   *
   * @return the module's state
   */
  @Override
  public ModuleState getModuleState() {
    return moduleState;
  }

  /**
   * Merges child modules with the corresponding parent modules.
   *
   * @param allStacks      collection of all stack module in stack definition
   * @param commonServices collection of all common service module in stack definition
   * @param modules        child modules of this module that are to be merged
   * @param parentModules  parent modules which the modules are to be merged with
   *
   * @return collection of the merged modules
   */
  protected <T extends StackDefinitionModule<T, ?>> Collection<T> mergeChildModules(
      Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions, Map<String, T> modules, Map<String, T> parentModules)
        throws AmbariException {
    Set<String> addedModules = new HashSet<>();
    Collection<T> mergedModules = new HashSet<>();

    for (T module : modules.values()) {
      String id = module.getId();
      addedModules.add(id);
      if (!module.isDeleted()) {
        if (parentModules.containsKey(id)) {
          module.resolve(parentModules.get(id), allStacks, commonServices, extensions);
        }
      }
      mergedModules.add(module);
    }

    // add non-overlapping parent modules
    for (T parentModule : parentModules.values()) {
      String id = parentModule.getId();
      if (!addedModules.contains(id)) {
        mergedModules.add(parentModule);
      }
    }
    return mergedModules;
  }

  /**
   * Finalize a modules child components.
   * Any child module marked as deleted will be removed from this module after finalizing
   * the child.
   *
   * @param modules  child modules to finalize
   */
  protected void finalizeChildModules(Collection<? extends StackDefinitionModule> modules) {
    Iterator<? extends StackDefinitionModule> iter = modules.iterator();
    while (iter.hasNext()) {
      StackDefinitionModule module = iter.next();
      module.finalizeModule();
      if (module.isDeleted()) {
        iter.remove();
      }
    }
  }

  @Override
  public void finalizeModule() {
    // do nothing by default
  }
}
