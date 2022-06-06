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
package org.apache.ambari.view.property;

import org.apache.ambari.view.ViewContext;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Map;
import java.util.Iterator;

/**
 * The PropertyValidator service.
 */
public class PropertyValidatorService {

  /**
   * The view context.
   */
  @Inject
  ViewContext context;
  
  /**
   * Handles: GET /.
   *
   * @param headers   http headers
   * @param ui        uri info
   *
   * @return value JSON representation
   */
  @GET
  @Path("/")
  @Produces({"text/plain", "application/json"})
  public Response getValue(@Context HttpHeaders headers, @Context UriInfo ui) {

    Map props = context.getProperties();;

    Iterator it = props.entrySet().iterator();
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    buf.append("[");
    while(it.hasNext()) {
        Map.Entry pairs = (Map.Entry)it.next();
        if (first == false)
            buf.append(",\n");
        buf.append("{\"");
        buf.append(pairs.getKey());
        buf.append("\" : \"");
        buf.append(pairs.getValue());
        buf.append("\"}");
        first = false;
    }
    buf.append("]");

    return Response.ok(buf.toString()).build();
  }

} // end PropertyService
