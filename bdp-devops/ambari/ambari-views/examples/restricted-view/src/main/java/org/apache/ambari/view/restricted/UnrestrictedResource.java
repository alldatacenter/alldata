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
package org.apache.ambari.view.restricted;


import org.apache.ambari.view.ViewContext;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * The unrestricted resource.
 */
public class UnrestrictedResource {
  /**
   * The view context.
   */
  @Inject
  ViewContext context;

  /**
   * Handles: GET /unrestricted.
   *
   * @return the response
   */
  @GET
  @Produces({"text/html"})
  public Response getUnrestricted() throws IOException{
    return Response.ok("<b>You have accessed an unrestricted resource.</b>").type("text/html").build();
  }
}
