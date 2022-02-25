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

package org.apache.ambari.server.controller.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.BaseProvider;
import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitorAcceptor;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic JDBC based resource provider.
 * TODO : Not used. Move to Test for API integration testing.
 */
public class JDBCResourceProvider extends BaseProvider implements ResourceProvider {

    private final Resource.Type type;

  private final ConnectionFactory connectionFactory;

    /**
     * The schema for this provider's resource type.
     */
    private final Map<Resource.Type, String> keyPropertyIds;

    /**
     * Key mappings used for joins.
     */
    private final Map<String, Map<String, String>> importedKeys = new HashMap<>();

    private static final Logger LOG =
            LoggerFactory.getLogger(JDBCResourceProvider.class);

    public JDBCResourceProvider(ConnectionFactory connectionFactory,
                                Resource.Type type,
                                Set<String> propertyIds,
                                Map<Resource.Type, String> keyPropertyIds) {
      super(propertyIds);
      this.connectionFactory = connectionFactory;
      this.type = type;
      this.keyPropertyIds = keyPropertyIds;
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

        Set<Resource> resources = new HashSet<>();
        Set<String> propertyIds = getRequestPropertyIds(request, predicate);

        // Can't allow these properties with the old schema...
        propertyIds.remove(PropertyHelper.getPropertyId("Clusters", "cluster_id"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "disk_info"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "public_host_name"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "last_registration_time"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "host_state"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "last_heartbeat_time"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "host_health_report"));
        propertyIds.remove(PropertyHelper.getPropertyId("Hosts", "host_status"));
        propertyIds.remove(PropertyHelper.getPropertyId("ServiceInfo", "desired_configs"));
        propertyIds.remove(PropertyHelper.getPropertyId("ServiceComponentInfo", "desired_configs"));
        propertyIds.remove(PropertyHelper.getPropertyId("HostRoles", "configs"));
        propertyIds.remove(PropertyHelper.getPropertyId("HostRoles", "desired_configs"));

        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = connectionFactory.getConnection();


            for (String table : getTables(propertyIds)) {
                getImportedKeys(connection, table);
            }

            String sql = getSelectSQL(propertyIds, predicate);
            statement = connection.createStatement();

            rs = statement.executeQuery(sql);

            while (rs.next()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                final ResourceImpl resource = new ResourceImpl(type);
                for (int i = 1; i <= columnCount; ++i) {
                    String propertyId = PropertyHelper.getPropertyId(metaData.getTableName(i), metaData.getColumnName(i));
                    if (propertyIds.contains(propertyId)) {
                        resource.setProperty(propertyId, rs.getString(i));
                    }
                }
                resources.add(resource);
            }
            statement.close();

        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Caught exception getting resource.", e);
            }
            return Collections.emptySet();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error("Exception while closing ResultSet", e);
            }

            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                LOG.error("Exception while closing statment", e);
            }

            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                LOG.error("Exception while closing statment", e);
            }

        }


        return resources;
    }

    @Override
    public RequestStatus createResources(Request request)
        throws SystemException,
               UnsupportedPropertyException,
               ResourceAlreadyExistsException,
               NoSuchParentResourceException {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            Statement statement = null;
            try {
                Set<Map<String, Object>> propertySet = request.getProperties();
                statement = connection.createStatement(); 
                for (Map<String, Object> properties : propertySet) {
                    String sql = getInsertSQL(properties);
                    statement.execute(sql);
                }
            } finally {
              if (statement != null) {
                statement.close();
              }  
            }

        } catch (SQLException e) {
            throw new IllegalStateException("DB error : ", e);
        } finally {
          if (connection != null) {
            try {
              connection.close();
            } catch (SQLException ex) {
              throw new IllegalStateException("DB error : ", ex);
            }
          }
        }

        return getRequestStatus();
    }

    @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Connection connection = null;
    try {
      connection = connectionFactory.getConnection();
      Statement statement = null;
      try {
        Set<Map<String, Object>> propertySet = request.getProperties();

        Map<String, Object> properties = propertySet.iterator().next();

        String sql = getUpdateSQL(properties, predicate);

        statement = connection.createStatement();

        statement.execute(sql);

      } finally {
        if (statement != null) {
          statement.close();
        }
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ex) {
          throw new IllegalStateException("DB error : ", ex);
        }
      }
    }

    return getRequestStatus();
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Connection connection = null;
    try {
      connection = connectionFactory.getConnection();
      Statement statement = null;
      try {
        String sql = getDeleteSQL(predicate);
        statement = connection.createStatement();
        statement.execute(sql);
      } finally {
        if (statement != null) {
          statement.close();
        }
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ex) {
          throw new IllegalStateException("DB error : ", ex);
        }
      }
    }

    return getRequestStatus();
  }


    private String getInsertSQL(Map<String, Object> properties) {

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        String table = null;


        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyId = entry.getKey();
            Object propertyValue = entry.getValue();

            table = PropertyHelper.getPropertyCategory(propertyId);


            if (columns.length() > 0) {
                columns.append(", ");
            }
            columns.append(PropertyHelper.getPropertyName(propertyId));

            if (values.length() > 0) {
                values.append(", ");
            }
            values.append("'");
            values.append(propertyValue);
            values.append("'");
        }

        return "insert into " + table + " (" +
                columns + ") values (" + values + ")";
    }

    private String getSelectSQL(Set<String> propertyIds, Predicate predicate) {

        StringBuilder columns = new StringBuilder();
        Set<String> tableSet = new HashSet<>();

        for (String propertyId : propertyIds) {
            if (columns.length() > 0) {
                columns.append(", ");
            }
          String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);
          columns.append(propertyCategory).append(".").append(PropertyHelper.getPropertyName(propertyId));
            tableSet.add(propertyCategory);
        }


        boolean haveWhereClause = false;
        StringBuilder whereClause = new StringBuilder();
        if (predicate != null &&
                propertyIds.containsAll(PredicateHelper.getPropertyIds(predicate)) &&
                predicate instanceof PredicateVisitorAcceptor) {

            SQLPredicateVisitor visitor = new SQLPredicateVisitor();
            ((PredicateVisitorAcceptor) predicate).accept(visitor);
            whereClause.append(visitor.getSQL());
            haveWhereClause = true;
        }

        StringBuilder joinClause = new StringBuilder();

        if (tableSet.size() > 1) {

            for (String table : tableSet) {
                Map<String, String> joinKeys = importedKeys.get(table);
                if (joinKeys != null) {
                    for (Map.Entry<String, String> entry : joinKeys.entrySet()) {
                        String category1 = PropertyHelper.getPropertyCategory(entry.getKey());
                        String category2 = PropertyHelper.getPropertyCategory(entry.getValue());
                        if (tableSet.contains(category1) && tableSet.contains(category2)) {
                            if (haveWhereClause) {
                                joinClause.append(" AND ");
                            }
                            joinClause.append(category1).append(".").append(PropertyHelper.getPropertyName(entry.getKey()));
                            joinClause.append(" = ");
                            joinClause.append(category2).append(".").append(PropertyHelper.getPropertyName(entry.getValue()));
                            tableSet.add(category1);
                            tableSet.add(category2);

                            haveWhereClause = true;
                        }
                    }
                }
            }
        }

        StringBuilder tables = new StringBuilder();

        for (String table : tableSet) {
            if (tables.length() > 0) {
                tables.append(", ");
            }
            tables.append(table);
        }

        String sql = "select " + columns + " from " + tables;

        if (haveWhereClause) {
            sql = sql + " where " + whereClause + joinClause;
        }

        return sql;
    }

    private String getDeleteSQL(Predicate predicate) {

        StringBuilder whereClause = new StringBuilder();
        if (predicate instanceof BasePredicate) {

            BasePredicate basePredicate = (BasePredicate) predicate;

            SQLPredicateVisitor visitor = new SQLPredicateVisitor();
            basePredicate.accept(visitor);
            whereClause.append(visitor.getSQL());

            String table = PropertyHelper.getPropertyCategory(basePredicate.getPropertyIds().iterator().next());

            return "delete from " + table + " where " + whereClause;
        }
        throw new IllegalStateException("Can't generate SQL.");
    }

    private String getUpdateSQL(Map<String, Object> properties, Predicate predicate) {

        if (predicate instanceof BasePredicate) {

            StringBuilder whereClause = new StringBuilder();

            BasePredicate basePredicate = (BasePredicate) predicate;

            SQLPredicateVisitor visitor = new SQLPredicateVisitor();
            basePredicate.accept(visitor);
            whereClause.append(visitor.getSQL());

            String table = PropertyHelper.getPropertyCategory(basePredicate.getPropertyIds().iterator().next());


            StringBuilder setClause = new StringBuilder();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {

                if (setClause.length() > 0) {
                    setClause.append(", ");
                }
                setClause.append(PropertyHelper.getPropertyName(entry.getKey()));
                setClause.append(" = ");
                setClause.append("'");
                setClause.append(entry.getValue());
                setClause.append("'");
            }

            return "update " + table + " set " + setClause + " where " + whereClause;
        }
        throw new IllegalStateException("Can't generate SQL.");
    }

    @Override
    public Map<Resource.Type, String> getKeyPropertyIds() {
        return keyPropertyIds;
    }

    /**
     * Lazily populate the imported key mappings for the given table.
     *
     * @param connection the connection to use to obtain the database meta data
     * @param table      the table
     * @throws SQLException thrown if the meta data for the given connection cannot be obtained
     */
  private void getImportedKeys(Connection connection, String table) throws SQLException {
    if (!this.importedKeys.containsKey(table)) {

      Map<String, String> importedKeys = new HashMap<>();
      this.importedKeys.put(table, importedKeys);

      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet rs = null;
      try {
        rs = metaData.getImportedKeys(connection.getCatalog(), null, table);

        while (rs.next()) {

          String pkPropertyId = PropertyHelper.getPropertyId(
                  rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"));

          String fkPropertyId = PropertyHelper.getPropertyId(
                  rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"));

          importedKeys.put(pkPropertyId, fkPropertyId);
        }
      } finally {
        if (rs != null) {
          rs.close();
        }
      }
    }
  }

    /**
     * Get a request status
     *
     * @return the request status
     */
    private RequestStatus getRequestStatus() {
        return new RequestStatusImpl(null);
    }

    /**
     * Get the set of tables associated with the given property ids.
     *
     * @param propertyIds the property ids
     * @return the set of tables
     */
    private static Set<String> getTables(Set<String> propertyIds) {
        Set<String> tables = new HashSet<>();
        for (String propertyId : propertyIds) {
            tables.add(PropertyHelper.getPropertyCategory(propertyId));
        }
        return tables;
    }
}
