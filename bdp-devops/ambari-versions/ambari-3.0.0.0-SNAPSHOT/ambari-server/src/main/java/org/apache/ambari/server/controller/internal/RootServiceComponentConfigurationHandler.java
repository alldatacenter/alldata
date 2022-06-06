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

package org.apache.ambari.server.controller.internal;

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.RootServiceComponentConfiguration;
import org.apache.ambari.server.controller.spi.SystemException;

/**
 * RootServiceComponentConfigurationHandler is an interface to be implemented to handle the different
 * types of root-level configurations.
 */
abstract class RootServiceComponentConfigurationHandler {
  /**
   * Retrieve the requested component configurations.
   *
   * @param categoryName the category name (or <code>null</code> for all)
   * @return a map of category names to properties (name/value pairs).
   */
  public abstract Map<String, RootServiceComponentConfiguration> getComponentConfigurations(String categoryName);

  /**
   * Delete the requested configuration.
   *
   * @param categoryName the category name
   */
  public abstract void removeComponentConfiguration(String categoryName);

  /**
   * Set or update a configuration category with the specified properties.
   * <p>
   * If <code>removePropertiesIfNotSpecified</code> is <code>true</code>, the persisted category is to include only the specified properties.
   * <p>
   * If <code>removePropertiesIfNotSpecified</code> is <code>false</code>, the persisted category is to include the union of the existing and specified properties.
   * <p>
   * In any case, existing property values will be overwritten by the one specified in the property map.
   *
   * @param categoryName                   the category name
   * @param properties                     a map of properties to set
   * @param removePropertiesIfNotSpecified <code>true</code> to ensure the set of properties are only those that have be explicitly specified;
   *                                       <code>false</code> to update the set of existing properties with the specified set of properties, adding missing properties but not removing any properties
   * @throws AmbariException in case an error occurred while updating category's properties
   */
  public abstract void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws AmbariException;

  /**
   * Preform some operation on the set of data for a category.
   * <p>
   * If <code>mergeExistingProperties</code> is <code>false</code>, the operation is to include the union of the existing and specified properties.
   * <p>
   * If <code>mergeExistingProperties</code> is <code>false</code>, the operation is to include only the specified properties.
   *
   * @param categoryName            the category name
   * @param properties              a map of properties to set
   * @param mergeExistingProperties <code>true</code> to use the the set of existing properties along with the specified set of properties;
   *                                <code>false</code>  to use set of specified properties only
   * @param operation               the operation to perform
   * @param operationParameters     parameters to supply the name operation
   * @return an {@link OperationResult}
   * @throws SystemException if an error occurs while handling the request
   */
  public abstract OperationResult performOperation(String categoryName, Map<String, String> properties,
                                                   boolean mergeExistingProperties, String operation,
                                                   Map<String, Object> operationParameters) throws SystemException;

  class OperationResult {
    private final String id;
    private final boolean success;
    private final String message;
    private final Object response;

    OperationResult(String id, boolean success, String message, Object response) {
      this.id = id;
      this.success = success;
      this.message = message;
      this.response = response;
    }

    public String getId() {
      return id;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public Object getResponse() {
      return response;
    }

  }
}
