/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Represents an HTTP request to another server for properties to be used to populate an Ambari resource.
 * The server response is expected to be JSON that can be deserialized into a <code>Map<String, Object>></code>
 * instance.
 */
public abstract class JsonHttpPropertyRequest extends HttpPropertyProvider.HttpPropertyRequest {
  private static final Logger LOG = LoggerFactory.getLogger(JsonHttpPropertyRequest.class);

  private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

  private static final Gson GSON = new Gson();


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a property request.
   *
   * @param propertyMappings  the property name mapping
   */
  public JsonHttpPropertyRequest(Map<String, String> propertyMappings) {
    super(propertyMappings);
  }


  // ----- PropertyRequest -------------------------------------------------

  @Override
  public void populateResource(Resource resource, InputStream inputStream) throws SystemException {

    try {
      Map<String, Object> responseMap = GSON.fromJson(IOUtils.toString(inputStream, "UTF-8"), MAP_TYPE);
      if (responseMap == null){
        LOG.error("Properties map from HTTP response is null");
      }
      for (Map.Entry<String, String> entry : getPropertyMappings().entrySet()) {
        Object propertyValueToSet = getPropertyValue(responseMap, entry.getKey());
        resource.setProperty(entry.getValue(), propertyValueToSet);
      }
    } catch (IOException e) {
      throw new SystemException("Error setting properties.", e);
    }
  }


  // ----- helper methods --------------------------------------------------

  // get the property value from the response map for the given property name
  private Object getPropertyValue(Map<String, Object> responseMap, String property) throws SystemException {
    if (property == null || responseMap == null) {
      return null;
    }

    Object result = responseMap;

    try {
      for (String key : property.split("/")) {
        result = ((Map) result).get(key);
      }
    } catch (ClassCastException e) {
      String msg = String.format("Error getting property value for %s.", property);
      LOG.error(msg, e);
      throw new SystemException(msg, e);
    }
    return result;
  }
}
