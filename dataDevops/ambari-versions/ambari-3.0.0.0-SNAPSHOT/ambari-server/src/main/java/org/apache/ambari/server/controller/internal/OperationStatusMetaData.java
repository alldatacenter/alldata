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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;

/**
 * OperationStatusMetaData is data to return as the status of an operation invoked on a set of resources
 * while processing a REST API request.
 * <p>
 * Zero or more results may be set as part of the data.
 */
public class OperationStatusMetaData implements RequestStatusMetaData, ResultMetadata {

  private final Map<String, Result> results = new HashMap<>();

  public void addResult(String id, boolean success, String message, Object response) {
    results.put(id, new Result(id, success, message, response));
  }

  public Set<String> getResultIds() {
    return results.keySet();
  }

  public Result getResult(String id) {
    if (results.containsKey(id)) {
      return results.get(id);
    }

    throw new NoSuchElementException();
  }

  public List<Result> getResults() {
    return new ArrayList<>(results.values());
  }

  public class Result {
    private final String id;
    private final boolean success;
    private final String message;
    private final Object response;

    Result(String id, boolean success, String message, Object response) {
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
