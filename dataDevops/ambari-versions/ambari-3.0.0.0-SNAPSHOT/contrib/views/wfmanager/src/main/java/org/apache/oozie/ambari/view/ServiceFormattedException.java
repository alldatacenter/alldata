/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.ambari.view;

import java.util.HashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONObject;

public class ServiceFormattedException extends WebApplicationException {
  private final static int STATUS = 500;

  public ServiceFormattedException(Throwable exception) {
    super(errorEntity(exception.getMessage(), exception));
  }

  public ServiceFormattedException(String message, Throwable exception) {
    super(errorEntity(message, exception));
  }

  protected static Response errorEntity(String message, Throwable e) {
    HashMap<String, Object> response = new HashMap<String, Object>();
    response.put("message", message);
    String trace = null;
    if (e != null) {
      trace = ExceptionUtils.getStackTrace(e);
    }
    response.put("trace", trace);
    response.put("status", STATUS);
    return Response.status(STATUS).entity(new JSONObject(response))
      .type(MediaType.APPLICATION_JSON).build();
  }
}
