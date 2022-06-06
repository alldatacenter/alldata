/**
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

package org.apache.ambari.view.utils.ambari;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

/**
 * Exception during work with Ambari API
 */
public class AmbariApiException extends RuntimeException {
  public AmbariApiException(String message) {
    super(message);
  }

  public AmbariApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public Response toEntity() {
    HashMap<String, Object> respJson = new HashMap<>();
    respJson.put("trace", getCause());
    respJson.put("message", getMessage());
    respJson.put("status", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
      .entity(respJson).type(MediaType.APPLICATION_JSON).build();
  }
}
