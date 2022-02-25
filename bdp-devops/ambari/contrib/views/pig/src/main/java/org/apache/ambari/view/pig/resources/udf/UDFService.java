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

package org.apache.ambari.view.pig.resources.udf;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.udf.models.UDF;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.utils.NotFoundFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

/**
 * Servlet for UDFs
 * API:
 * GET /
 *      get all UDFs
 * GET /:id
 *      get one UDF
 * PUT /:id
 *      update UDF
 * POST /
 *      create new UDF
 *      Required: path, name
 */
public class UDFService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected UDFResourceManager resourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(UDFService.class);

  protected synchronized PersonalCRUDResourceManager<UDF> getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new UDFResourceManager(context);
    }
    return resourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{udfId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUDF(@PathParam("udfId") String udfId) {
    try {
      UDF udf = getResourceManager().read(udfId);
      JSONObject object = new JSONObject();
      object.put("udf", udf);
      return Response.ok(object).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Delete single item
   */
  @DELETE
  @Path("{udfId}")
  public Response deleteUDF(@PathParam("udfId") String udfId) {
    try {
      getResourceManager().delete(udfId);
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Get all UDFs
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUDFList(@Context UriInfo ui) {
    try {
      LOG.debug("Getting all UDFs");
      List allUDFs = getResourceManager().readAll(
          new OnlyOwnersFilteringStrategy(this.context.getUsername()));

      JSONObject object = new JSONObject();
      object.put("udfs", allUDFs);
      return Response.ok(object).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Update item
   */
  @PUT
  @Path("{udfId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateUDF(UDFRequest request,
                            @PathParam("udfId") String udfId) {
    try {
      getResourceManager().update(request.udf, udfId);
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Create UDF
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createUDF(UDFRequest request, @Context HttpServletResponse response,
                            @Context UriInfo ui) {
    try {
      getResourceManager().create(request.udf);

      UDF udf = getResourceManager().read(request.udf.getId());

      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.udf.getId()));

      JSONObject object = new JSONObject();
      object.put("udf", udf);
      return Response.ok(object).status(201).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class UDFRequest {
    public UDF udf;
  }
}
