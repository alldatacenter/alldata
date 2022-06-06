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
package org.apache.ambari.view.proxy;

import org.apache.ambari.view.ViewContext;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * The calculator resource.
 */
public class CalculatorResource {
  /**
   * The view context.
   */
  @Inject
  ViewContext context;

  /**
   * Handles: GET /calculator Get the calculator usage.
   *
   * @param headers   http headers
   * @param ui        uri info
   *
   * @return the response including the usage of the calculator resource
   */
  @GET
  @Produces({"text/html"})
  public Response getUsage(@Context HttpHeaders headers, @Context UriInfo ui) throws IOException{

    String entity = "<h2>Usage of calculator</h2><br>" +
        "<ul>" +
        "<li><b>calculator/add/{a}/{b}</b> - add {a} to {b}</li>" +
        "<li><b>calculator/subtract/{a}/{b}</b> - subtract {a} from {b}</li>" +
        "<li><b>calculator/multiply/{a}/{b}</b> - multiply {a} with {b}</li>" +
        "<li><b>calculator/divide/{a}/{b}</b> - divide {a} by {b}</li>" +
        "</ul>"
        ;
    return Response.ok(entity).type("text/html").build();
  }

  /**
   * Handles: GET /calculator/add/{a}/{b} Get the results of a + b.
   *
   * @param a  the left side operand
   * @param b  the right side operand
   *
   * @return the response including the result of a + b
   */
  @GET
  @Path("/add/{a}/{b}")
  @Produces({"text/html"})
  public Response add(@PathParam("a") double a, @PathParam("b") double b) {
    String result = a + " + " + b + " = " + (a + b);
    return Response.ok("<b>" + result + "</b>").type("text/html").build();
  }

  /**
   * Handles: GET /calculator/subtract/{a}/{b} Get the results of a - b.
   *
   * @param a  the left side operand
   * @param b  the right side operand
   *
   * @return the response including the result of a - b
   */
  @GET
  @Path("/subtract/{a}/{b}")
  @Produces({"text/html"})
  public Response subtract(@PathParam("a") double a, @PathParam("b") double b) {
    String result =  a + " - " + b + " = " + (a - b);
    return Response.ok("<b>" + result + "</b>").type("text/html").build();
  }

  /**
   * Handles: GET /calculator/multiply/{a}/{b} Get the results of a * b.
   *
   * @param a  the left side operand
   * @param b  the right side operand
   *
   * @return the response including the result of a * b
   */
  @GET
  @Path("/multiply/{a}/{b}")
  @Produces({"text/html"})
  public Response multiply(@PathParam("a") double a, @PathParam("b") double b) {
    String result =  a + " * " + b + " = " + (a * b);
    return Response.ok("<b>" + result + "</b>").type("text/html").build();
  }

  /**
   * Handles: GET /calculator/divide/{a}/{b} Get the results of a / b.
   *
   * @param a  the left side operand
   * @param b  the right side operand
   *
   * @return the response including the result of a / b
   */
  @GET
  @Path("/divide/{a}/{b}")
  @Produces({"text/html"})
  public Response divide(@PathParam("a") double a, @PathParam("b") double b) {
    String result =  a + " / " + b + " = " + (a / b);
    return Response.ok("<b>" + result + "</b>").type("text/html").build();
  }
}
