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

package org.apache.ambari.view.pig.resources.scripts;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.scripts.models.PigScript;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.utils.NotFoundFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Servlet for scripts
 * API:
 * GET /:id
 *      read script
 * POST /
 *      create new script
 *      Required: title, pigScript
 * GET /
 *      get all scripts of current user
 */
public class ScriptService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected ScriptResourceManager resourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(ScriptService.class);

  protected synchronized PersonalCRUDResourceManager<PigScript> getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new ScriptResourceManager(context);
    }
    return resourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{scriptId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getScript(@PathParam("scriptId") String scriptId) {
    LOG.info("Fetching scriptId : {}", scriptId);
    try {
      PigScript script = null;
      script = getResourceManager().read(scriptId);
      JSONObject object = new JSONObject();
      object.put("script", script);
      return Response.ok(object).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Exception occurred : ", itemNotFound);
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Delete single item
   */
  @DELETE
  @Path("{scriptId}")
  public Response deleteScript(@PathParam("scriptId") String scriptId) {
    LOG.info("Deleting scriptId : {}", scriptId);
    try {
      getResourceManager().delete(scriptId);
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Exception occurred : ", itemNotFound);
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Get all scripts
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getScriptList() {
    try {
      LOG.debug("Getting all scripts");
      List allScripts = getResourceManager().readAll(
          new OnlyOwnersFilteringStrategy(this.context.getUsername()));  //TODO: move strategy to PersonalCRUDRM

      JSONObject object = new JSONObject();
      object.put("scripts", allScripts);
      return Response.ok(object).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Update item
   */
  @PUT
  @Path("{scriptId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateScript(PigScriptRequest request,
                               @PathParam("scriptId") String scriptId) {
    LOG.info("updating scriptId : {} ", scriptId);
    try {
      getResourceManager().update(request.script, scriptId);
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Exception occurred : ", itemNotFound);
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Create script
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response saveScript(PigScriptRequest request, @Context HttpServletResponse response,
                             @Context UriInfo ui) {
    LOG.info("Creating new script : {}", request);
    try {
      getResourceManager().create(request.script);

      PigScript script = null;

      script = getResourceManager().read(request.script.getId());

      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.script.getId()));

      JSONObject object = new JSONObject();
      object.put("script", script);
      return Response.ok(object).status(201).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Exception occurred : ", itemNotFound);
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class PigScriptRequest {
    public PigScript script;
  }
}
