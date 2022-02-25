/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.Request;

import com.google.common.collect.Iterables;

/**
 * The purpose of this class is to retrieve information from {@link Request} objects.
 * This information can be a single value or a list of values.
 */
public class RequestAuditEventCreatorHelper {

  /**
   * Returns a named property from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static String getNamedProperty(Request request, String propertyName) {
    NamedPropertySet first = Iterables.getFirst(request.getBody().getNamedPropertySets(), null);
    if (first != null && first.getProperties().get(propertyName) instanceof String) {
      return String.valueOf(first.getProperties().get(propertyName));
    }
    return null;
  }

  /**
   * Returns a list of named properties from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static List<String> getNamedPropertyList(Request request, String propertyName) {
    NamedPropertySet first = Iterables.getFirst(request.getBody().getNamedPropertySets(), null);
    if (first != null && first.getProperties().get(propertyName) instanceof List) {
      List<String> list = (List<String>) first.getProperties().get(propertyName);
      if (list != null) {
        return list;
      }
    }
    return Collections.emptyList();
  }

  /**
   * Returns a property from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static String getProperty(Request request, String propertyName) {
    List<String> list = getPropertyList(request, propertyName);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Returns a list of properties from a request
   * @param request
   * @param propertyName
   * @return
   */
  public static List<String> getPropertyList(Request request, String propertyName) {
    List<String> list = new LinkedList<>();
    for (Map<String, Object> propertyMap : request.getBody().getPropertySets()) {
      if (propertyMap.containsKey(propertyName)) {
        list.add(String.valueOf(propertyMap.get(propertyName)));
      }
    }
    return list;
  }
}
