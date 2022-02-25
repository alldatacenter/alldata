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

package org.apache.ambari.view.pig.resources.scripts;

import com.google.inject.Inject;
import org.apache.ambari.view.*;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.scripts.models.PigScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Resource provider for Scripts
 */
public class ScriptResourceProvider implements ResourceProvider<PigScript> {
  @Inject
  ViewContext context;

  protected ScriptResourceManager resourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(ScriptResourceProvider.class);

  protected synchronized PersonalCRUDResourceManager<PigScript> getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new ScriptResourceManager(context);
    }
    return resourceManager;
  }

  @Override
  public PigScript getResource(String resourceId, Set<String> properties) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    try {
      return getResourceManager().read(resourceId);
    } catch (ItemNotFound itemNotFound) {
      throw new NoSuchResourceException(resourceId);
    }
  }

  @Override
  public Set<PigScript> getResources(ReadRequest readRequest) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    return new HashSet<PigScript>(getResourceManager().readAll(
        new OnlyOwnersFilteringStrategy(this.context.getUsername())));
  }

  @Override
  public void createResource(String s, Map<String, Object> stringObjectMap) throws SystemException, ResourceAlreadyExistsException, NoSuchResourceException, UnsupportedPropertyException {
    PigScript script = null;
    try {
      script = new PigScript(stringObjectMap);
    } catch (InvocationTargetException e) {
      throw new SystemException("error on creating resource", e);
    } catch (IllegalAccessException e) {
      throw new SystemException("error on creating resource", e);
    }
    getResourceManager().create(script);
  }

  @Override
  public boolean updateResource(String resourceId, Map<String, Object> stringObjectMap) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    PigScript script = null;
    try {
      script = new PigScript(stringObjectMap);
    } catch (InvocationTargetException e) {
      throw new SystemException("error on updating resource", e);
    } catch (IllegalAccessException e) {
      throw new SystemException("error on updating resource", e);
    }
    try {
      getResourceManager().update(script, resourceId);
    } catch (ItemNotFound itemNotFound) {
      throw new NoSuchResourceException(resourceId);
    }
    return true;
  }

  @Override
  public boolean deleteResource(String resourceId) throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
    try {
      getResourceManager().delete(resourceId);
    } catch (ItemNotFound itemNotFound) {
      throw new NoSuchResourceException(resourceId);
    }
    return true;
  }
}
