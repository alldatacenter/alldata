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
package org.apache.drill.exec.store.openTSDB.client.query;

import org.apache.drill.common.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * DBQuery is an abstraction of an openTSDB query,
 * that used for extracting data from the storage system by POST request to DB.
 * <p>
 * An OpenTSDB query requires at least one sub query,
 * a means of selecting which time series should be included in the result set.
 */
public class DBQuery {

  private static final Logger log =
          LoggerFactory.getLogger(DBQuery.class);
  /**
   * The start time for the query. This can be a relative or absolute timestamp.
   */
  private String start;
  /**
   * An end time for the query. If not supplied, the TSD will assume the local system time on the server.
   * This may be a relative or absolute timestamp. This param is optional, and if it isn't specified we will send null
   * to the db in this field, but in this case db will assume the local system time on the server.
   */
  private String end;
  /**
   * One or more sub subQueries used to select the time series to return.
   */
  private Set<Query> queries;

  private DBQuery(Builder builder) {
    this.start = builder.start;
    this.end = builder.end;
    this.queries = builder.queries;
  }

  public String getStart() {
    return start;
  }

  public String getEnd() {
    return end;
  }

  public Set<Query> getQueries() {
    return queries;
  }

  public static class Builder {

    private String start;
    private String end;
    private Set<Query> queries = new HashSet<>();

    public Builder() {
    }

    public Builder setStartTime(String startTime) {
      if (startTime == null) {
        throw UserException.validationError()
                .message("start param must be specified")
                .build(log);
      }
      this.start = startTime;
      return this;
    }

    public Builder setEndTime(String endTime) {
      this.end = endTime;
      return this;
    }

    public Builder setQueries(Set<Query> queries) {
      if (queries.isEmpty()) {
        throw UserException.validationError()
                .message("Required params such as metric, aggregator weren't specified. " +
                        "Add these params to the query")
                .build(log);
      }
      this.queries = queries;
      return this;
    }

    public DBQuery build() {
      return new DBQuery(this);
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DBQuery dbQuery = (DBQuery) o;

    if (!start.equals(dbQuery.start)) {
      return false;
    }
    if (!end.equals(dbQuery.end)) {
      return false;
    }
    return queries.equals(dbQuery.queries);
  }

  @Override
  public int hashCode() {
    int result = start.hashCode();
    result = 31 * result + end.hashCode();
    result = 31 * result + queries.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "DBQuery{" +
            "start='" + start + '\'' +
            ", end='" + end + '\'' +
            ", queries=" + queries +
            '}';
  }
}
