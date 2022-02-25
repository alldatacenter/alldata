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

package org.apache.ambari.view.pig.resources.jobs;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.pig.resources.files.FileResource;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.utils.BadRequestFormattedException;
import org.apache.ambari.view.pig.utils.FilePaginator;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Servlet for Pig Jobs
 * API:
 * GET /:id
 *      read job info
 * POST /
 *      create new job
 *      Required: scriptId
 *      Optional: params
 * GET /
 *      get all jobs of current user
 * GET /:id/notify
 *      callback from Templeton
 */
public class JobService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected final static Logger LOG =
      LoggerFactory.getLogger(JobService.class);

  protected JobResourceManager resourceManager = null;

  /**
   * Get resource manager object
   * @return resource manager object
   */
  public synchronized JobResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new JobResourceManager(context);
    }
    return resourceManager;
  }

  /**
   * Set resource manager object
   * @param resourceManager resource manager object
   */
  public synchronized void setResourceManager(JobResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJob(@PathParam("jobId") String jobId) {
    LOG.info("Fetching job with id : {}", jobId);
    try {
      PigJob job = null;
      try {
        job = getResourceManager().read(jobId);
      } catch (ItemNotFound itemNotFound) {
        LOG.error("Exception occurred : ", itemNotFound);
        throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
      }
      getResourceManager().retrieveJobStatus(job);
      JSONObject object = new JSONObject();
      object.put("job", job);
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
   * Get single item
   */
  @DELETE
  @Path("{jobId}")
  public Response killJob(@PathParam("jobId") String jobId,
                          @QueryParam("remove") final String remove) throws IOException {
    LOG.info("killing job : {}, remove : {}", jobId, remove);
    try {
      PigJob job = null;
      try {
        job = getResourceManager().read(jobId);
      } catch (ItemNotFound itemNotFound) {
        LOG.error("Exception occurred : ", itemNotFound);
        throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
      }
      getResourceManager().killJob(job);
      if (remove != null && remove.compareTo("true") == 0) {
        getResourceManager().delete(jobId);
      }
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Callback from templeton
   */
  @GET
  @Path("{jobId}/notify")
  public Response jobCompletionNotification(@Context HttpHeaders headers,
                                            @Context UriInfo ui,
                                            @PathParam("jobId") final String jobId) {
    try {
      PigJob job = null;
      job = getResourceManager().ignorePermissions(new Callable<PigJob>() {
        public PigJob call() throws Exception {
          PigJob job = null;
          try {
            job = getResourceManager().read(jobId);
          } catch (ItemNotFound itemNotFound) {
            LOG.error("Exception occurred : ", itemNotFound);
            return null;
          }
          return job;
        }
      });
      if (job == null)
        throw new NotFoundFormattedException("Job with id '" + jobId + "' not found", null);

      getResourceManager().retrieveJobStatus(job);
      return Response.ok().build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  @GET
  @Path("{jobId}/results/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response jobExitCode(@Context HttpHeaders headers,
                              @Context UriInfo ui,
                              @PathParam("jobId") String jobId,
                              @PathParam("fileName") String fileName,
                              @QueryParam("page") Long page) {
    LOG.info("fetching results in fileName {} ", fileName);
    try {
      PigJob job = null;
      try {
        job = getResourceManager().read(jobId);
      } catch (ItemNotFound itemNotFound) {
        LOG.error("Exception occurred : ", itemNotFound);
        throw new NotFoundFormattedException("Job with id '" + jobId + "' not found", null);
      }
      String filePath = job.getStatusDir() + "/" + fileName;
      LOG.debug("Reading file {}", filePath);
      FilePaginator paginator = new FilePaginator(filePath, context);

      if (page == null) {
        page = 0L;
      }

      FileResource file = new FileResource();
      file.setFilePath(filePath);
      file.setFileContent(paginator.readPage(page));
      file.setHasNext(paginator.pageCount() > page + 1);
      file.setPage(page);
      file.setPageCount(paginator.pageCount());

      JSONObject object = new JSONObject();
      object.put("file", file);
      return Response.ok(object).status(200).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (IOException ex) {
      LOG.error("Exception occurred : ", ex);
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      LOG.error("Exception occurred : ", ex);
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Get all jobs
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobList(@QueryParam("scriptId") final String scriptId) {
    LOG.info("Fechting scriptId : {} ", scriptId);
    try {
      List allJobs = getResourceManager().readAll(
          new OnlyOwnersFilteringStrategy(this.context.getUsername()) {
            @Override
            public boolean isConform(Indexed item) {
              if (scriptId == null)
                return super.isConform(item);
              else {
                PigJob job = (PigJob) item;
                return (job.getScriptId() != null && scriptId.compareTo(job.getScriptId()) == 0 && super.isConform(item));
              }
            }
          });  //TODO: move strategy to PersonalCRUDRM

      JSONObject object = new JSONObject();
      object.put("jobs", allJobs);
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
   * Create job
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response runJob(PigJobRequest request, @Context HttpServletResponse response,
                         @Context UriInfo ui) {
    LOG.info("Creating new job : {} ", request);
    try {
      request.validatePOST();
      getResourceManager().create(request.job);

      PigJob job = null;

      try {
        job = getResourceManager().read(request.job.getId());
      } catch (ItemNotFound itemNotFound) {
        throw new NotFoundFormattedException("Job not found", null);
      }

      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.job.getId()));

      JSONObject object = new JSONObject();
      object.put("job", job);
      return Response.ok(object).status(201).build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception occurred : ", ex);
      throw ex;
    } catch (IllegalArgumentException ex) {
      LOG.error("Exception occurred : ", ex);
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      LOG.error("Exception occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class PigJobRequest {
    public PigJob job;

    public String explainPOST() {
      StringBuilder result = new StringBuilder();
      if ((job.getPigScript() == null || job.getPigScript().isEmpty()) &&
          (job.getForcedContent() == null || job.getForcedContent().isEmpty()))
        result.append("No pigScript file or forcedContent specifed;");
      if (job.getTitle() == null || job.getTitle().isEmpty())
        result.append("No title specifed;");
      if (job.getId() != null && !job.getTitle().isEmpty())
        result.append("ID should not exists in creation request;");
      return result.toString();
    }

    public void validatePOST() {
      if (!explainPOST().isEmpty()) {
        throw new BadRequestFormattedException(explainPOST(), null);
      }
    }

    @Override
    public String toString() {
      return new StringBuilder("PigJobRequest{")
        .append("job=").append(job)
        .append('}').toString();
    }
  }
}
