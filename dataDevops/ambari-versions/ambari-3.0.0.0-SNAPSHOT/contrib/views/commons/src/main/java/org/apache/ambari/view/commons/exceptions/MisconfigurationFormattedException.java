/**
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

package org.apache.ambari.view.commons.exceptions;

import org.json.simple.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

public class MisconfigurationFormattedException extends WebApplicationException {
  private final static int STATUS = 500;
  private final static String message = "Parameter \"%s\" is set to null";

  public MisconfigurationFormattedException(String name) {
    super(errorEntity(name));
  }

  protected static Response errorEntity(String name) {
    HashMap<String, Object> response = new HashMap<String, Object>();
    response.put("message", String.format(message, name));
    response.put("trace", null);
    response.put("status", STATUS);
    return Response.status(STATUS).entity(new JSONObject(response)).type(MediaType.APPLICATION_JSON).build();
  }
}
