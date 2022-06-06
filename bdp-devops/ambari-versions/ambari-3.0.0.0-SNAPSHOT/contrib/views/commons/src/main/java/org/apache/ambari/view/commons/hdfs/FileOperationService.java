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

package org.apache.ambari.view.commons.hdfs;

import com.google.common.base.Strings;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.exceptions.NotFoundFormattedException;
import org.apache.ambari.view.commons.exceptions.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.DirListInfo;
import org.apache.ambari.view.utils.hdfs.DirStatus;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * File operations service
 */
public class FileOperationService extends HdfsService {
  private final static Logger LOG =
      LoggerFactory.getLogger(FileOperationService.class);


  private static final String FILES_VIEW_MAX_FILE_PER_PAGE = "views.files.max.files.per.page";
  private static final int DEFAULT_FILES_VIEW_MAX_FILE_PER_PAGE = 5000;

  private Integer maxFilesPerPage = DEFAULT_FILES_VIEW_MAX_FILE_PER_PAGE;

  /**
   * Constructor
   * @param context View Context instance
   */
  public FileOperationService(ViewContext context) {
    super(context);
    setMaxFilesPerPage(context);
  }

  private void setMaxFilesPerPage(ViewContext context) {
    String maxFilesPerPageProperty = context.getAmbariProperty(FILES_VIEW_MAX_FILE_PER_PAGE);
    LOG.info("maxFilesPerPageProperty = {}", maxFilesPerPageProperty);
    if(!Strings.isNullOrEmpty(maxFilesPerPageProperty)){
      try {
        maxFilesPerPage = Integer.parseInt(maxFilesPerPageProperty);
      }catch(Exception e){
        LOG.error("{} should be integer, but it is {}, using default value of {}", FILES_VIEW_MAX_FILE_PER_PAGE , maxFilesPerPageProperty, DEFAULT_FILES_VIEW_MAX_FILE_PER_PAGE);
      }
    }
  }

  /**
   * Constructor
   * @param context View Context instance
   */
  public FileOperationService(ViewContext context, Map<String, String> customProperties) {
    super(context, customProperties);
    this.setMaxFilesPerPage(context);
  }

  /**
   * List dir
   * @param path path
   * @param nameFilter : name on which filter is applied
   * @return response with dir content
   */
  @GET
  @Path("/listdir")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listdir(@QueryParam("path") String path, @QueryParam("nameFilter") String nameFilter) {
    try {
      JSONObject response = new JSONObject();
      Map<String, Object> parentInfo = getApi().fileStatusToJSON(getApi().getFileStatus(path));
      DirStatus dirStatus = getApi().listdir(path, nameFilter, maxFilesPerPage);
      DirListInfo dirListInfo = dirStatus.getDirListInfo();
      parentInfo.put("originalSize", dirListInfo.getOriginalSize());
      parentInfo.put("truncated", dirListInfo.isTruncated());
      parentInfo.put("finalSize", dirListInfo.getFinalSize());
      parentInfo.put("nameFilter", dirListInfo.getNameFilter());
      response.put("files", getApi().fileStatusToJSON(dirStatus.getFileStatuses()));
      response.put("meta", parentInfo);
      return Response.ok(response).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (FileNotFoundException ex) {
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Rename
   * @param request rename request
   * @return response with success
   */
  @POST
  @Path("/rename")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response rename(final SrcDstFileRequest request) {
    try {
      HdfsApi api = getApi();
      ResponseBuilder result;
      if (api.rename(request.src, request.dst)) {
        result = Response.ok(getApi().fileStatusToJSON(api
            .getFileStatus(request.dst)));
      } else {
        result = Response.ok(new FileOperationResult(false, "Can't move '" + request.src + "' to '" + request.dst + "'")).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Chmod
   * @param request chmod request
   * @return response with success
   */
  @POST
  @Path("/chmod")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response chmod(final ChmodRequest request) {
    try {
      HdfsApi api = getApi();
      ResponseBuilder result;
      if (api.chmod(request.path, request.mode)) {
        result = Response.ok(getApi().fileStatusToJSON(api
            .getFileStatus(request.path)));
      } else {
        result = Response.ok(new FileOperationResult(false, "Can't chmod '" + request.path + "'")).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Copy file
   * @param request source and destination request
   * @return response with success
   */
  @POST
  @Path("/move")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response move(final MultiSrcDstFileRequest request,
                       @Context HttpHeaders headers, @Context UriInfo ui) {
    try {
      HdfsApi api = getApi();
      ResponseBuilder result;
      String message = "";

      List<String> sources = request.sourcePaths;
      String destination = request.destinationPath;
      if(sources.isEmpty()) {
        result = Response.ok(new FileOperationResult(false, "Can't move 0 file/folder to '" + destination + "'")).
          status(422);
        return result.build();
      }

      int index = 0;
      for (String src : sources) {
        String fileName = getFileName(src);
        String finalDestination = getDestination(destination, fileName);
        try {
          if (api.rename(src, finalDestination)) {
            index ++;
          } else {
            message = "Failed to move '" + src + "' to '" + finalDestination + "'";
            break;
          }
        } catch (IOException exception) {
          message = exception.getMessage();
          logger.error("Failed to move '{}' to '{}'. Exception: {}", src, finalDestination,
            exception.getMessage());
          break;
        }
      }
      if (index == sources.size()) {
        result = Response.ok(new FileOperationResult(true)).status(200);
      } else {
        FileOperationResult errorResult = getFailureFileOperationResult(sources, index, message);
        result = Response.ok(errorResult).status(422);
      }
      return result.build();
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Copy file
   * @param request source and destination request
   * @return response with success
   */
  @POST
  @Path("/copy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response copy(final MultiSrcDstFileRequest request,
                       @Context HttpHeaders headers, @Context UriInfo ui) {
    try {
      HdfsApi api = getApi();
      ResponseBuilder result;
      String message = "";

      List<String> sources = request.sourcePaths;
      String destination = request.destinationPath;
      if(sources.isEmpty()) {
        result = Response.ok(new FileOperationResult(false, "Can't copy 0 file/folder to '" + destination + "'")).
          status(422);
        return result.build();
      }

      int index = 0;
      for (String src : sources) {
        String fileName = getFileName(src);
        String finalDestination = getDestination(destination, fileName);
        try {
          api.copy(src, finalDestination);
          index ++;
        } catch (IOException|HdfsApiException exception) {
          message = exception.getMessage();
          logger.error("Failed to copy '{}' to '{}'. Exception: {}", src, finalDestination,
            exception.getMessage());
          break;
        }
      }
      if (index == sources.size()) {
        result = Response.ok(new FileOperationResult(true)).status(200);
      } else {
        FileOperationResult errorResult = getFailureFileOperationResult(sources, index, message);
        result = Response.ok(errorResult).status(422);
      }
      return result.build();
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Make directory
   * @param request make directory request
   * @return response with success
   */
  @PUT
  @Path("/mkdir")
  @Produces(MediaType.APPLICATION_JSON)
  public Response mkdir(final MkdirRequest request) {
    try{
      HdfsApi api = getApi();
      ResponseBuilder result;
      if (api.mkdir(request.path)) {
        result = Response.ok(getApi().fileStatusToJSON(api.getFileStatus(request.path)));
      } else {
        result = Response.ok(new FileOperationResult(false, "Can't create dir '" + request.path + "'")).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Empty trash
   * @return response with success
   */
  @DELETE
  @Path("/trash/emptyTrash")
  @Produces(MediaType.APPLICATION_JSON)
  public Response emptyTrash() {
    try {
      HdfsApi api = getApi();
      api.emptyTrash();
      return Response.ok(new FileOperationResult(true)).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Move to trash
   * @param request remove request
   * @return response with success
   */
  @POST
  @Path("/moveToTrash")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response moveToTrash(MultiRemoveRequest request) {
    try {
      ResponseBuilder result;
      HdfsApi api = getApi();
      String trash = api.getTrashDirPath();
      String message = "";

      if (request.paths.size() == 0) {
        result = Response.ok(new FileOperationResult(false, "No path entries provided.")).status(422);
      } else {
        if (!api.exists(trash)) {
          if (!api.mkdir(trash)) {
            result = Response.ok(new FileOperationResult(false, "Trash dir does not exists. Can't create dir for " +
              "trash '" + trash + "'")).status(422);
            return result.build();
          }
        }

        int index = 0;
        for (MultiRemoveRequest.PathEntry entry : request.paths) {
          String trashFilePath = api.getTrashDirPath(entry.path);
          try {
            if (api.rename(entry.path, trashFilePath)) {
              index ++;
            } else {
              message = "Failed to move '" + entry.path + "' to '" + trashFilePath + "'";
              break;
            }
          } catch (IOException exception) {
            message = exception.getMessage();
            logger.error("Failed to move '{}' to '{}'. Exception: {}", entry.path, trashFilePath,
              exception.getMessage());
            break;
          }
        }
        if (index == request.paths.size()) {
          result = Response.ok(new FileOperationResult(true)).status(200);
        } else {
          FileOperationResult errorResult = getFailureFileOperationResult(getPathsFromPathsEntries(request.paths), index, message);
          result = Response.ok(errorResult).status(422);
        }
      }
      return result.build();
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Remove
   * @param request remove request
   * @return response with success
   */
  @POST
  @Path("/remove")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response remove(MultiRemoveRequest request, @Context HttpHeaders headers,
                         @Context UriInfo ui) {
    try {
      HdfsApi api = getApi();
      ResponseBuilder result;
      String message = "";
      if(request.paths.size() == 0) {
        result = Response.ok(new FileOperationResult(false, "No path entries provided."));
      } else {
        int index = 0;
        for (MultiRemoveRequest.PathEntry entry : request.paths) {
          try {
            if (api.delete(entry.path, entry.recursive)) {
              index++;
            } else {
              message = "Failed to remove '" + entry.path + "'";
              break;
            }
          } catch (IOException exception) {
            message = exception.getMessage();
            logger.error("Failed to remove '{}'. Exception: {}", entry.path, exception.getMessage());
            break;
          }

        }
        if (index == request.paths.size()) {
          result = Response.ok(new FileOperationResult(true)).status(200);
        } else {
          FileOperationResult errorResult = getFailureFileOperationResult(getPathsFromPathsEntries(request.paths), index, message);
          result = Response.ok(errorResult).status(422);
        }
      }
      return result.build();
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private List<String> getPathsFromPathsEntries(List<MultiRemoveRequest.PathEntry> paths) {
    List<String> entries = new ArrayList<>();
    for(MultiRemoveRequest.PathEntry path: paths) {
      entries.add(path.path);
    }
    return entries;
  }

  private FileOperationResult getFailureFileOperationResult(List<String> paths, int failedIndex, String message) {
    List<String> succeeded = new ArrayList<>();
    List<String> unprocessed = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    ListIterator<String> iter = paths.listIterator();
    while (iter.hasNext()) {
      int index = iter.nextIndex();
      String path = iter.next();
      if (index < failedIndex) {
        succeeded.add(path);
      } else if (index == failedIndex) {
        failed.add(path);
      } else {
        unprocessed.add(path);
      }
    }
    return new FileOperationResult(false, message, succeeded, failed, unprocessed);
  }

  private String getDestination(String baseDestination, String fileName) {
    if(baseDestination.endsWith("/")) {
      return baseDestination + fileName;
    } else {
      return baseDestination + "/" + fileName;
    }
  }

  private String getFileName(String srcPath) {
    return srcPath.substring(srcPath.lastIndexOf('/') + 1);
  }

  /**
   * Wrapper for json mapping of mkdir request
   */
  @XmlRootElement
  public static class MkdirRequest {
    @XmlElement(nillable = false, required = true)
    public String path;
  }

  /**
   * Wrapper for json mapping of chmod request
   */
  @XmlRootElement
  public static class ChmodRequest {
    @XmlElement(nillable = false, required = true)
    public String path;
    @XmlElement(nillable = false, required = true)
    public String mode;
  }

  /**
   * Wrapper for json mapping of request with
   * source and destination
   */
  @XmlRootElement
  public static class SrcDstFileRequest {
    @XmlElement(nillable = false, required = true)
    public String src;
    @XmlElement(nillable = false, required = true)
    public String dst;
  }

  /**
   * Wrapper for json mapping of request with multiple
   * source and destination
   */
  @XmlRootElement
  public static class MultiSrcDstFileRequest {
    @XmlElement(nillable = false, required = true)
    public List<String> sourcePaths = new ArrayList<>();
    @XmlElement(nillable = false, required = true)
    public String destinationPath;
  }

  /**
   * Wrapper for json mapping of remove request
   */
  @XmlRootElement
  public static class MultiRemoveRequest {
    @XmlElement(nillable = false, required = true)
    public List<PathEntry> paths = new ArrayList<>();
    public static class PathEntry {
      @XmlElement(nillable = false, required = true)
      public String path;
      public boolean recursive;
    }

  }
}
