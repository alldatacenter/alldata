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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.services.trino.client;

import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TrinoResourceManager {
  private static final Logger LOG = LoggerFactory.getLogger(TrinoResourceManager.class);

  private static final String  CATALOG    = "catalog";
  private static final String  SCHEMA     = "schema";
  private static final String  TABLE      = "table";
  private static final String  COLUMN     = "column";


  public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) throws Exception {
    Map<String, Object> ret = null;

    if (LOG.isDebugEnabled()) {
      LOG.debug("==> TrinoResourceManager.connectionTest() ServiceName: " + serviceName + " Configs: " + configs);
    }

    try {
      ret = TrinoClient.connectionTest(serviceName, configs);
    } catch (Exception e) {
      LOG.error("<== TrinoResourceManager.connectionTest() Error: " + e);
      throw e;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("<== TrinoResourceManager.connectionTest() Result : " + ret);
    }

    return ret;
  }

  public static List<String> getTrinoResources(String serviceName, String serviceType, Map<String, String> configs, ResourceLookupContext context) throws Exception {

    String userInput = context.getUserInput();
    String resource = context.getResourceName();
    Map<String, List<String>> resourceMap = context.getResources();
    List<String> resultList = null;
    List<String> catalogList = null;
    List<String> schemaList = null;
    List<String> tableList = null;
    List<String> columnList = null;
    String catalogName = null;
    String schemaName = null;
    String tableName = null;
    String columnName = null;


    if (LOG.isDebugEnabled()) {
      LOG.debug("<== TrinoResourceMgr.getTrinoResources() UserInput: \"" + userInput + "\" resource : " + resource + " resourceMap: " + resourceMap);
    }

    if (userInput != null && resource != null) {
      if (resourceMap != null && !resourceMap.isEmpty()) {
        catalogList = resourceMap.get(CATALOG);
        schemaList = resourceMap.get(SCHEMA);
        tableList = resourceMap.get(TABLE);
        columnList = resourceMap.get(COLUMN);
      }
      switch (resource.trim().toLowerCase()) {
        case CATALOG:
          catalogName = userInput;
          break;
        case SCHEMA:
          schemaName = userInput;
        case TABLE:
          tableName = userInput;
          break;
        case COLUMN:
          columnName = userInput;
          break;
        default:
          break;
      }
    }

    if (serviceName != null && userInput != null) {
      try {

        if (LOG.isDebugEnabled()) {
          LOG.debug("==> TrinoResourceManager.getTrinoResources() UserInput: \"" + userInput + "\" configs: " + configs + " catalogList: " + catalogList + " tableList: "
            + tableList + " columnList: " + columnList);
        }

        final TrinoClient trinoClient = new TrinoConnectionManager().getTrinoConnection(serviceName, serviceType, configs);

        Callable<List<String>> callableObj = null;

        final String finalCatalogName;
        final String finalSchemaName;
        final String finalTableName;
        final String finalColumnName;

        final List<String> finalCatalogList = catalogList;
        final List<String> finalSchemaList = schemaList;
        final List<String> finalTableList = tableList;
        final List<String> finalColumnList = columnList;

        if (trinoClient != null) {
          if (catalogName != null && !catalogName.isEmpty()) {
            finalCatalogName = catalogName;
            callableObj = new Callable<List<String>>() {
              @Override
              public List<String> call() throws Exception {
                return trinoClient.getCatalogList(finalCatalogName, finalCatalogList);
              }
            };
          } else if (schemaName != null && !schemaName.isEmpty()) {
            finalSchemaName = schemaName;
            callableObj = new Callable<List<String>>() {
              @Override
              public List<String> call() throws Exception {
                return trinoClient.getSchemaList(finalSchemaName, finalCatalogList, finalSchemaList);
              }
            };
          } else if (tableName != null && !tableName.isEmpty()) {
            finalTableName = tableName;
            callableObj = new Callable<List<String>>() {
              @Override
              public List<String> call() throws Exception {
                return trinoClient.getTableList(finalTableName, finalCatalogList, finalSchemaList, finalTableList);
              }
            };
          } else if (columnName != null && !columnName.isEmpty()) {
            // Column names are matched by the wildcardmatcher
            columnName += "*";
            finalColumnName = columnName;
            callableObj = new Callable<List<String>>() {
              @Override
              public List<String> call() throws Exception {
                return trinoClient.getColumnList(finalColumnName, finalCatalogList, finalSchemaList, finalTableList, finalColumnList);
              }
            };
          }
          if (callableObj != null) {
            synchronized (trinoClient) {
              resultList = TimedEventUtil.timedTask(callableObj, 5, TimeUnit.SECONDS);
            }
          } else {
            LOG.error("Could not initiate a TrinoClient timedTask");
          }
        }
      } catch (Exception e) {
        LOG.error("Unable to get Trino resource", e);
        throw e;
      }
    }
    return resultList;
  }
}
