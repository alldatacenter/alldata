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

package org.apache.ambari.view.pig.resources;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.PersonalResource;

import java.util.concurrent.Callable;

/**
 * Resource manager that returns only user owned elements from DB
 * @param <T> Data type with ID and Owner
 */
public class PersonalCRUDResourceManager<T extends PersonalResource> extends CRUDResourceManager<T> {
  protected ViewContext context;
  protected boolean ignorePermissions = false;

  /**
   * Constructor
   * @param responseClass model class
   * @param context View Context instance
   */
  public PersonalCRUDResourceManager(Class<T> responseClass, ViewContext context) {
    super(responseClass);
    this.context = context;
  }

  @Override
  public T update(T newObject, String id) throws ItemNotFound {
    T object = getPigStorage().load(this.resourceClass, Integer.parseInt(id));
    if (object.getOwner().compareTo(this.context.getUsername()) != 0) {
      throw new ItemNotFound();
    }

    newObject.setOwner(this.context.getUsername());
    return super.update(newObject, id);
  }

  @Override
  public T save(T object) {
    if (!ignorePermissions) {
      // in threads permissions should be ignored,
      // because context.getUsername doesn't work. See BUG-27093.
      object.setOwner(this.context.getUsername());
    }
    return super.save(object);
  }

  @Override
  protected boolean checkPermissions(T object) {
    if (ignorePermissions)
      return true;
    return object.getOwner().compareTo(this.context.getUsername()) == 0;
  }

  @Override
  public ViewContext getContext() {
    return context;
  }

  /**
   * Execute action ignoring objects owner
   * @param actions callable to execute
   * @return value returned from actions
   * @throws Exception
   */
  public <T> T ignorePermissions(Callable<T> actions) throws Exception {
    ignorePermissions = true;
    T result;
    try {
      result = actions.call();
    } finally {
      ignorePermissions = false;
    }
    return result;
  }

  protected static String getUsername(ViewContext context) {
    String userName = context.getProperties().get("dataworker.username");
    if (userName == null || userName.compareTo("null") == 0 || userName.compareTo("") == 0)
      userName = context.getUsername();
    return userName;
  }

  protected String getUsername() {
    return getUsername(context);
  }
}
