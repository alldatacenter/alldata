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
package org.apache.ambari.view.favorite;

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
import java.util.HashMap;

/**
 * The Favorite service.
 */
public class FavoriteService {

  private   static  final   String  PROPERTY_QUESTION = "what.is.the.question";
  private   static  final   String  PROPERTY_DONOTKNOW = "i.do.not.know";

  private   static  final   String  INSTANCE_ITEM = "favorite.item";
  
  /**
   * The view context.
   */
  @Inject
  ViewContext context;

  /**
   * Handles: POST /item/{thing}.
   *
   * @param headers   http headers
   * @param ui        uri info
   *
   * @return favorite item representation
   */
  @POST
  @Path("/item/{thing}")
  @Consumes({"text/plain", "application/json"})
  @Produces({"text/plain", "application/json"})
  public Response setFavoriteItem(@Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("thing") String thing) {
                            
    context.putInstanceData(INSTANCE_ITEM, thing);
    
    StringBuffer buf = new StringBuffer();
    buf.append("{\"item\" : \"");
    buf.append(thing);
    buf.append("\"}");

    return Response.ok(buf.toString()).build();
  }
  
  /**
   * Handles: GET /item.
   *
   * @param headers   http headers
   * @param ui        uri info
   *
   * @return favorite item representation
   */
  @GET
  @Path("/item")
  @Produces({"text/plain", "application/json"})
  public Response getFavoriteItem(@Context HttpHeaders headers, @Context UriInfo ui) {
    System.out.println(" context = "+context);

    String item = context.getInstanceData(INSTANCE_ITEM);
    if (item == null || item.isEmpty())
        item = context.getProperties().get(PROPERTY_DONOTKNOW);

    StringBuffer buf = new StringBuffer();
    buf.append("{\"item\" : \"");
    buf.append(item);
    buf.append("\"}");

    return Response.ok(buf.toString()).build();
  }

  /**
   * Handles: GET /question.
   *
   * @param headers   http headers
   * @param ui        uri info
   *
   * @return favorite question representation
   */
  @GET
  @Path("/question")
  @Produces({"text/plain", "application/json"})
  public Response getQuestion(@Context HttpHeaders headers, @Context UriInfo ui) {

    String  question = context.getProperties().get(PROPERTY_QUESTION);
    String  username = context.getUsername();

    StringBuffer buf = new StringBuffer();
    buf.append("{\"question\" : \"");
    buf.append(question);
    buf.append("\", \"username\" : \"");
    buf.append(username);
    buf.append("\"}");
        
    return Response.ok(buf.toString()).build();
  }


}
