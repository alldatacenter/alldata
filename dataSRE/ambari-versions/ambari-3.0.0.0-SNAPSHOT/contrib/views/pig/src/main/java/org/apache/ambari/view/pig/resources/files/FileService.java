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

package org.apache.ambari.view.pig.resources.files;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.utils.BadRequestFormattedException;
import org.apache.ambari.view.pig.utils.FilePaginator;
import org.apache.ambari.view.pig.utils.NotFoundFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.ambari.view.commons.hdfs.UserService;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.apache.hadoop.fs.FileStatus;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * File access resource
 * API:
 * GET /:path
 *      read entire file
 * POST /
 *      create new file
 *      Required: filePath
 *      file should not already exists
 * PUT /:path
 *      update file content
 */
public class FileService extends BaseService {
  public static final String VIEW_CONF_KEYVALUES = "view.conf.keyvalues";

  @Inject
  ViewResourceHandler handler;

  protected final static Logger LOG =
      LoggerFactory.getLogger(FileService.class);

  /**
   * Get single item
   */
  @GET
  @Path("{filePath:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFile(@PathParam("filePath") String filePath,
                          @QueryParam("page") Long page,
                          @QueryParam("action") String action) throws IOException, InterruptedException {
    try {
      filePath = sanitizeFilePath(filePath);
      if (action != null && action.equals("ls")) {
        LOG.debug("List directory {}", filePath);
        List<String> ls = new LinkedList<String>();
        for (FileStatus fs : getHdfsApi().listdir(filePath)) {
          ls.add(fs.getPath().toString());
        }
        JSONObject object = new JSONObject();
        object.put("ls", ls);
        return Response.ok(object).status(200).build();
      }
      LOG.debug("Reading file {}", filePath);
      FilePaginator paginator = new FilePaginator(filePath, context);

      if (page == null)
        page = 0L;

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
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (FileNotFoundException ex) {
      LOG.error("Error occurred : ", ex);
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (IllegalArgumentException ex) {
      LOG.error("Error occurred : ", ex);
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Delete single item
   */
  @DELETE
  @Path("{filePath:.*}")
  public Response deleteFile(@PathParam("filePath") String filePath) throws IOException, InterruptedException {
    try {

      filePath = sanitizeFilePath(filePath);

      LOG.info("Deleting file {}", filePath);
      if (getHdfsApi().delete(filePath, false)) {
        return Response.status(204).build();
      }
      throw new NotFoundFormattedException("FileSystem.delete returned false", null);
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Update item
   */
  @PUT
  @Path("{filePath:.*}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateFile(FileResourceRequest request,
                             @PathParam("filePath") String filePath) throws IOException, InterruptedException {
    try {
      filePath = sanitizeFilePath(filePath);
      LOG.info("Rewriting file {}", filePath);
      FSDataOutputStream output = getHdfsApi().create(filePath, true);
      output.write(request.file.getFileContent().getBytes("UTF-8"));
      output.close();
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Create script
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createFile(FileResourceRequest request,
                             @Context HttpServletResponse response, @Context UriInfo ui)
      throws IOException, InterruptedException {
    try {
      LOG.info("Creating file {}", request.file.getFilePath());
      try {
        FSDataOutputStream output = getHdfsApi().create(request.file.getFilePath(), false);
        if (request.file.getFileContent() != null) {
          output.write(request.file.getFileContent().getBytes("UTF-8"));
        }
        output.close();
      } catch (FileAlreadyExistsException ex) {
        throw new ServiceFormattedException(ex.getMessage(), ex, 400);
      }
      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.file.getFilePath()));
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Checks connection to HDFS
   * @param context View Context
   */
  public static void hdfsSmokeTest(ViewContext context) {
    try {
      Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
      HdfsApi api;
      if(props.isPresent()){
        api = HdfsUtil.connectToHDFSApi(context, props.get());
      }else{
        api = HdfsUtil.connectToHDFSApi(context);
      }

      api.getStatus();
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }


  /**
   * Checks connection to User HomeDirectory
   * @param context View Context
   */
  public static void userhomeSmokeTest(ViewContext context) {
    try {
      UserService  userservice = new UserService(context, getViewConfigs(context));
      userservice.homeDir();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class FileResourceRequest {
    public FileResource file;
  }

  private String sanitizeFilePath(String filePath){
    if (!filePath.startsWith("/") && !filePath.startsWith(".")) {
      filePath = "/" + filePath;  // some servers strip double slashes in URL
    }
    return filePath;
  }

  private static Map<String,String> getViewConfigs(ViewContext context) {
    Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
    return props.isPresent()? props.get() : new HashMap<String, String>();
  }
}
