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

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.shaded.guava.com.google.common.base.CharMatcher;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.parquet.Strings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class QueryWrapper {

  private final String query;
  private final String queryType;
  final int autoLimitRowCount;
  final String userName;
  final String defaultSchema;
  final Map<String, String> options;

  protected QueryWrapper(String query, String queryType,
      int rowCountLimit, String userName, String defaultSchema,
      Map<String, String> options) {
    this.query = query;
    this.queryType = queryType.toUpperCase();
    this.autoLimitRowCount = rowCountLimit;
    this.userName = userName;
    this.defaultSchema = defaultSchema;
    this.options = options;
  }

  @JsonCreator
  public QueryWrapper(
    @JsonProperty("query") String query,
    @JsonProperty("queryType") String queryType,
    @JsonProperty("autoLimit") String autoLimit,
    @JsonProperty("userName") String userName,
    @JsonProperty("defaultSchema") String defaultSchema,
    @JsonProperty("options") Map<String, String> options) {
    this(query, queryType, mapCount(autoLimit), userName, defaultSchema, options);
  }

  private static int mapCount(String rowLimit) {
    if (Strings.isNullOrEmpty(rowLimit)) {
      return 0;
    }
    try {
      return Integer.parseInt(rowLimit.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public String getQuery() { return query; }

  public String getQueryType() { return queryType; }

  public String getUserName() { return userName; }

  public int getAutoLimitRowCount() { return autoLimitRowCount; }

  public String getDefaultSchema() { return defaultSchema; }

  public Map<String, String> getOptions() { return options; }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("query", query)
        .field("query type", queryType)
        .field("user name", userName)
        .field("default schema", defaultSchema)
        .field("row limit", autoLimitRowCount)
        .toString();
  }

  public static final class RestQueryBuilder {

    private String query;
    private String queryType = QueryType.SQL.name();
    private int rowLimit;
    private String userName;
    private String defaultSchema;
    private Map<String, String> options;

    public RestQueryBuilder query(String query) {
      this.query = query;
      return this;
    }

    public RestQueryBuilder queryType(String queryType) {
      this.queryType = queryType;
      return this;
    }

    public RestQueryBuilder rowLimit(int rowLimit) {
      this.rowLimit = rowLimit;
      return this;
    }

    public RestQueryBuilder rowLimit(String rowLimit) {
      this.rowLimit = mapCount(rowLimit);
      return this;
    }

    public RestQueryBuilder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public RestQueryBuilder defaultSchema(String defaultSchema) {
      this.defaultSchema = defaultSchema;
      return this;
    }

    /**
     * Optional session option values encoded as strings.
     */
    public RestQueryBuilder sessionOptions(Map<String, String> options) {
      this.options = options;
      return this;
    }

    public QueryWrapper build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(query));
      query = CharMatcher.is(';').trimTrailingFrom(query.trim());
      Preconditions.checkArgument(!query.isEmpty());
      return new QueryWrapper(query, queryType, rowLimit, userName,
          defaultSchema, options);
    }
  }
}
