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
package org.apache.drill.exec.server.rest;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.rest.DrillRestServer.UserAuthEnabled;
import org.apache.drill.exec.server.rest.QueryWrapper.RestQueryBuilder;
import org.apache.drill.exec.server.rest.RestQueryRunner.QueryResult;
import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal;
import org.apache.drill.exec.server.rest.stream.QueryRunner;
import org.apache.drill.exec.work.WorkManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
@RolesAllowed(DrillUserPrincipal.AUTHENTICATED_ROLE)
public class QueryResources {
   private static final Logger logger = LoggerFactory.getLogger(QueryResources.class);

  @Inject
  UserAuthEnabled authEnabled;

  @Inject
  WorkManager work;

  @Inject
  SecurityContext sc;

  @Inject
  WebUserConnection webUserConnection;

  @Inject
  HttpServletRequest request;

  @Inject
  StorageResources sr;

  @GET
  @Path("/query")
  @Produces(MediaType.TEXT_HTML)
  public Viewable getQuery() {
    List<StorageResources.StoragePluginModel> enabledPlugins = sr.getConfigsFor("enabled")
      .stream()
      .map(plugin -> new StorageResources.StoragePluginModel(plugin, request))
      .collect(Collectors.toList());
    return ViewableWithPermissions.create(
        authEnabled.get(), "/rest/query/query.ftl",
        sc, new QueryPage(work, enabledPlugins, request));
  }

  @POST
  @Path("/query.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public StreamingOutput submitQueryJSON(QueryWrapper query) throws Exception {

    /*
    Prior to Drill 1.18, REST queries would batch the entire result set in memory,
    limiting query size. In Drill 1.19 and later, results are streamed from the
    executor, to the JSON writer and to the HTTP connection with no buffering.

    Starting with Drill 1.19, the "metadata" property specifying the result column
    data types is placed *before* the data itself. One drawback of doing so is that
    the schema will report that of the first batch: Drill allows schema to change
    across batches and thus the schema of the JSON-encoded data would change. This
    is more a bug with how Drill handles schemas than a JSON issue. (ODBC and JDBC
    have the same issues.)
    */
    QueryRunner runner = new QueryRunner(work, webUserConnection);
    try {
      runner.start(query);
    } catch (Exception e) {
      throw new WebApplicationException("Query submission failed", e);
    }
    return new StreamingOutput() {
      @Override
      public void write(OutputStream output)
          throws IOException, WebApplicationException {
        try {
          runner.sendResults(output);
        } catch (IOException e) {
          throw e;
        } catch (Exception e) {
          throw new WebApplicationException("JSON query failed", e);
        }
      }
    };
  }

  @POST
  @Path("/query")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_HTML)
  public Viewable submitQuery(@FormParam("query") String query,
                              @FormParam("queryType") String queryType,
                              @FormParam("autoLimit") String autoLimit,
                              @FormParam("userName") String userName,
                              @FormParam("defaultSchema") String defaultSchema,
                              Form form) throws Exception {
    try {
      // Run the query and wrap the result sets in a model to be
      // transformed to HTML. This can be memory-intensive for larger
      // queries.
      QueryWrapper wrapper = new RestQueryBuilder()
          .query(query)
          .queryType(queryType)
          .rowLimit(autoLimit)
          .userName(userName)
          .defaultSchema(defaultSchema)
          .sessionOptions(readOptionsFromForm(form))
          .build();
      final QueryResult result = new RestQueryRunner(wrapper,
              work, webUserConnection)
        .run();
      List<Integer> rowsPerPageValues = work.getContext().getConfig().getIntList(
          ExecConstants.HTTP_WEB_CLIENT_RESULTSET_ROWS_PER_PAGE_VALUES);
      Collections.sort(rowsPerPageValues);
      final String rowsPerPageValuesAsStr = Joiner.on(",").join(rowsPerPageValues);
      return ViewableWithPermissions.create(authEnabled.get(), "/rest/query/result.ftl", sc, new TabularResult(result, rowsPerPageValuesAsStr));
    } catch (Exception | Error e) {
      logger.error("Query from Web UI Failed: {}", e);
      return ViewableWithPermissions.create(authEnabled.get(), "/rest/errorMessage.ftl", sc, e);
    } finally {
      // no-op for authenticated user
      webUserConnection.cleanupSession();
    }
  }

  /**
   * Convert the form to a map. The form allows multiple values per key;
   * discard the entry if empty, throw an error if more than one value.
   */
  private Map<String, String> readOptionsFromForm(Form form) {
    Map<String, String> options = new HashMap<>();
    for (Map.Entry<String, List<String>> pair : form.asMap().entrySet()) {
      List<String> values = pair.getValue();
       if (values.isEmpty()) {
        continue;
      }
      if (values.size() > 1) {
        throw new BadRequestException(String.format(
            "Multiple values given for option '%s'", pair.getKey()));
      }

      options.put(pair.getKey(), values.get(0));
    }
    return options;
  }

  /**
   * Model class for Query page
   */
  public static class QueryPage {
    private final boolean onlyImpersonationEnabled;
    private final boolean autoLimitEnabled;
    private final int defaultRowsAutoLimited;
    private final List<StorageResources.StoragePluginModel> enabledPlugins;
    private final String csrfToken;

    public QueryPage(WorkManager work, List<StorageResources.StoragePluginModel> enabledPlugins, HttpServletRequest request) {
      DrillConfig config = work.getContext().getConfig();
      this.enabledPlugins = enabledPlugins;
      //if impersonation is enabled without authentication, will provide mechanism to add user name to request header from Web UI
      onlyImpersonationEnabled = WebServer.isOnlyImpersonationEnabled(config);
      autoLimitEnabled = config.getBoolean(ExecConstants.HTTP_WEB_CLIENT_RESULTSET_AUTOLIMIT_CHECKED);
      defaultRowsAutoLimited = config.getInt(ExecConstants.HTTP_WEB_CLIENT_RESULTSET_AUTOLIMIT_ROWS);
      csrfToken = WebUtils.getCsrfTokenFromHttpRequest(request);
    }

    public boolean isOnlyImpersonationEnabled() {
      return onlyImpersonationEnabled;
    }

    public boolean isAutoLimitEnabled() {
      return autoLimitEnabled;
    }

    public int getDefaultRowsAutoLimited() {
      return defaultRowsAutoLimited;
    }

    public List<StorageResources.StoragePluginModel> getEnabledPlugins() {
      return enabledPlugins;
    }

    public String getCsrfToken() {
      return csrfToken;
    }
  }

  /**
   * Model class for Results page
   */
  public static class TabularResult {
    private final List<String> columns;
    private final List<List<String>> rows;
    private final String queryId;
    private final String rowsPerPageValues;
    private final String queryState;
    private final int autoLimitedRowCount;

    public TabularResult(QueryResult result, String rowsPerPageValuesAsStr) {
      rowsPerPageValues = rowsPerPageValuesAsStr;
      queryId = result.getQueryId();
      final List<List<String>> rows = Lists.newArrayList();
      for (Map<String, String> rowMap:result.rows) {
        final List<String> row = Lists.newArrayList();
        for (String col:result.columns) {
          row.add(rowMap.get(col));
        }
        rows.add(row);
      }

      this.columns = ImmutableList.copyOf(result.columns);
      this.rows = rows;
      this.queryState = result.queryState;
      this.autoLimitedRowCount = result.attemptedAutoLimit;
    }

    public boolean isEmpty() {
      return columns.isEmpty();
    }

    public String getQueryId() {
      return queryId;
    }

    public List<String> getColumns() {
      return columns;
    }

    public List<List<String>> getRows() {
      return rows;
    }

    // Used by results.ftl to render default number of pages per row
    public String getRowsPerPageValues() {
      return rowsPerPageValues;
    }

    public String getQueryState() {
      return queryState;
    }

    // Used by results.ftl to indicate autoLimited resultset
    public boolean isResultSetAutoLimited() {
      return autoLimitedRowCount > 0 && rows.size() == autoLimitedRowCount;
    }

    // Used by results.ftl to indicate autoLimited resultset size
    public int getAutoLimitedRowCount() {
      return autoLimitedRowCount;
    }
  }
}
