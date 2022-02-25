/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.commons.lang.StringUtils;

/**
 * Abstract resource provider implementation that contains helper methods for
 * retrieving data from a result set.
 */
public abstract class AbstractJDBCResourceProvider<E extends Enum<E>> extends
    AbstractResourceProvider {
  private final Map<String,E> dbFields;

  /**
   * Create a new resource provider.
   * 
   * @param propertyIds
   *          the property ids
   * @param keyPropertyIds
   *          the key property ids
   */
  protected AbstractJDBCResourceProvider(Set<String> propertyIds,
      Map<Type,String> keyPropertyIds) {
    super(propertyIds, keyPropertyIds);
    this.dbFields = getDBFieldMap();
  }

  /**
   * Gets a map from property ids to db fields.
   * 
   * @return the map from property ids to db fields.
   */
  protected abstract Map<String,E> getDBFieldMap();

  /**
   * Retrieves the db field corresponding to a property id from a result set as
   * a string and sets the resulting string as a resource property.
   * 
   * @param resource
   *          resource object to set the property on
   * @param propertyId
   *          the property id to retrieve from the result set
   * @param rs
   *          the result set
   * @param requestedIds
   *          the requested ids
   * @throws SQLException
   *           if property id cannot be retrieved from the result set
   */
  protected void setString(Resource resource, String propertyId, ResultSet rs,
      Set<String> requestedIds) throws SQLException {
    if (requestedIds.contains(propertyId))
      setResourceProperty(resource, propertyId,
          rs.getString(dbFields.get(propertyId).toString()), requestedIds);
  }

  /**
   * Retrieves the db field corresponding to a property id from a result set as
   * an int and sets the resulting int as a resource property.
   * 
   * @param resource
   *          resource object to set the property on
   * @param propertyId
   *          the property id to retrieve from the result set
   * @param rs
   *          the result set
   * @param requestedIds
   *          the requested ids
   * @throws SQLException
   *           if property id cannot be retrieved from the result set
   */
  protected void setInt(Resource resource, String propertyId, ResultSet rs,
      Set<String> requestedIds) throws SQLException {
    if (requestedIds.contains(propertyId))
      setResourceProperty(resource, propertyId,
          rs.getInt(dbFields.get(propertyId).toString()), requestedIds);
  }

  /**
   * Retrieves the db field corresponding to a property id from a result set as
   * a long and sets the resulting long as a resource property.
   * 
   * @param resource
   *          resource object to set the property on
   * @param propertyId
   *          the property id to retrieve from the result set
   * @param rs
   *          the result set
   * @param requestedIds
   *          the requested ids
   * @throws SQLException
   *           if property id cannot be retrieved from the result set
   */
  protected void setLong(Resource resource, String propertyId, ResultSet rs,
      Set<String> requestedIds) throws SQLException {
    if (requestedIds.contains(propertyId))
      setResourceProperty(resource, propertyId,
          rs.getLong(dbFields.get(propertyId).toString()), requestedIds);
  }

  /**
   * Gets a comma-separated list of db fields corresponding to set of requested
   * ids.
   * 
   * @param requestedIds
   *          the requested ids
   * @return a comma-separated list of db fields
   */
  protected String getDBFieldString(Set<String> requestedIds) {
    String[] tmp = new String[requestedIds.size()];
    int i = 0;
    for (String s : requestedIds)
      if (dbFields.containsKey(s))
        tmp[i++] = dbFields.get(s).toString();
    return StringUtils.join(tmp, ",", 0, i);
  }

  /**
   * Gets a db field corresponding to a property id.
   * 
   * @param propertyId
   *          the property id
   * @return the db field enum value
   */
  protected E getDBField(String propertyId) {
    return dbFields.get(propertyId);
  }
}
