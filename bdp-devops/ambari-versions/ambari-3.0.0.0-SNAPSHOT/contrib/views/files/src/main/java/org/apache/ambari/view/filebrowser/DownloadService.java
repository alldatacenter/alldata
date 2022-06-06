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

package org.apache.ambari.view.filebrowser;

import com.google.gson.Gson;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.exceptions.MisconfigurationFormattedException;
import org.apache.ambari.view.commons.exceptions.NotFoundFormattedException;
import org.apache.ambari.view.commons.exceptions.ServiceFormattedException;
import org.apache.ambari.view.commons.hdfs.HdfsService;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.security.AccessControlException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for download and aggregate files
 */
public class DownloadService extends HdfsService {

  protected static final Logger LOG = LoggerFactory.getLogger(DownloadService.class);

  public DownloadService(ViewContext context) {
    super(context);
  }

  /**
   * @param context
   * @param customProperties : extra properties that need to be included into config
   */
  public DownloadService(ViewContext context, Map<String, String> customProperties) {
    super(context, customProperties);
  }

  /**
   * Download entire file
   * @param path path to file
   * @param download download as octet strem or as file mime type
   * @param checkperm used to check if the file can be downloaded. Takes precedence when both download and checkperm
   *                  is set.
   * @param headers http headers
   * @param ui uri info
   * @return response with file
   */
  @GET
  @Path("/browse")
  @Produces(MediaType.TEXT_PLAIN)
  public Response browse(@QueryParam("path") String path, @QueryParam("download") boolean download,
                         @QueryParam("checkperm") boolean checkperm,
                         @Context HttpHeaders headers, @Context UriInfo ui) {
    LOG.debug("browsing path : {} with download : {}", path, download);
    try {
      HdfsApi api = getApi();
      FileStatus status = api.getFileStatus(path);
      FSDataInputStream fs = api.open(path);
      if(checkperm) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("allowed", true);
        return Response.ok(jsonObject)
          .header("Content-Type", MediaType.APPLICATION_JSON)
          .build();
      }
      ResponseBuilder result = Response.ok(fs);
      if (download) {
        result.header("Content-Disposition",
          "attachment; filename=\"" + status.getPath().getName() + "\"").type(MediaType.APPLICATION_OCTET_STREAM);
      } else {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String mimeType = fileNameMap.getContentTypeFor(status.getPath().getName());
        result.header("Content-Disposition",
          "filename=\"" + status.getPath().getName() + "\"").type(mimeType);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      LOG.error("Exception while browsing : {}", path ,ex);
      throw ex;
    } catch (FileNotFoundException ex) {
      LOG.error("File not found while browsing : {}", path ,ex);
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      LOG.error("Exception while browsing : {}", path ,ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private void zipFile(ZipOutputStream zip, String path) {
    try {
      FSDataInputStream in = getApi().open(path);
      zip.putNextEntry(new ZipEntry(path.substring(1)));
      byte[] chunk = new byte[1024];

      int readLen = 0;
      while(readLen != -1) {
        zip.write(chunk, 0, readLen);
        readLen = in.read(chunk);
      }
    } catch (IOException ex) {
      LOG.error("Error zipping file {}  (file ignored): ", path, ex);
    } catch (InterruptedException ex) {
      LOG.error("Error zipping file {} (file ignored): ", path, ex);
    } finally {
      try {
        zip.closeEntry();
      } catch (IOException ex) {
        LOG.error("Error closing entry {} (file ignored): ", path, ex);
      }
    }
  }

  private void zipDirectory(ZipOutputStream zip, String path) {
    try {
      zip.putNextEntry(new ZipEntry(path.substring(1) + "/"));
    } catch (IOException ex) {
      LOG.error("Error zipping directory {} (directory ignored).", path, ex);
    } finally {
      try {
        zip.closeEntry();
      } catch (IOException ex) {
        LOG.error("Error zipping directory {} (directory ignored).", path, ex);
      }
    }
  }

  /**
   * Download ZIP of passed file list
   * @param request download request
   * @return response with zip
   */
  @POST
  @Path("/zip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadGZip(final DownloadRequest request) {
    LOG.debug("downloadGZip requested for : {} ", request.entries );
    try {
      String name = "hdfs.zip";
      if(request.entries.length == 1 ){
        name = new File(request.entries[0]).getName() + ".zip";
      }

      StreamingOutput result = new StreamingOutput() {
        public void write(OutputStream output) throws IOException,
            ServiceFormattedException {
          ZipOutputStream zip = new ZipOutputStream(output);
          try {
            HdfsApi api = getApi();
            Queue<String> files = new LinkedList<String>();
            for (String file : request.entries) {
              files.add(file);
            }
            while (!files.isEmpty()) {
              String path = files.poll();
              FileStatus status = api.getFileStatus(path);
              if (status.isDirectory()) {
                FileStatus[] subdir;
                try {
                  subdir = api.listdir(path);
                } catch (AccessControlException ex) {
                  LOG.error("Error zipping directory {}/ (directory ignored) : ", path.substring(1), ex);
                  continue;
                }
                for (FileStatus file : subdir) {
                  files.add(org.apache.hadoop.fs.Path
                      .getPathWithoutSchemeAndAuthority(file.getPath())
                      .toString());
                }
                zipDirectory(zip, path);
              } else {
                zipFile(zip, path);
              }
            }
          } catch (Exception ex) {
            LOG.error("Error occurred: " ,ex);
            throw new ServiceFormattedException(ex.getMessage(), ex);
          } finally {
            zip.close();
          }
        }
      };
      return Response.ok(result)
          .header("Content-Disposition", "inline; filename=\"" + name +"\"").build();
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ",ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Concatenate files
   * @param request download request
   * @return response with all files concatenated
   */
  @POST
  @Path("/concat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response concat(final DownloadRequest request) {
    LOG.info("Starting concat files.");
    try {
      StreamingOutput result = new StreamingOutput() {
        public void write(OutputStream output) throws IOException,
            ServiceFormattedException {
          FSDataInputStream in = null;
          for (String path : request.entries) {
            try {
              try {
                in = getApi().open(path);
              } catch (AccessControlException ex) {
                LOG.error("Error in opening file {}. Ignoring concat of this files.", path.substring(1), ex);
                continue;
              }

              long bytesCopied = IOUtils.copyLarge(in, output);
              LOG.info("concated file : {}, total bytes added = {}", path, bytesCopied);
            } catch (Exception ex) {
              LOG.error("Error occurred : ", ex);
              throw new ServiceFormattedException(ex.getMessage(), ex);
            } finally {
              if (in != null)
                in.close();
            }
          }
        }
      };
      ResponseBuilder response = Response.ok(result);
      if (request.download) {
        response.header("Content-Disposition", "attachment; filename=\"concatResult.txt\"").type(MediaType.APPLICATION_OCTET_STREAM);
      } else {
        response.header("Content-Disposition", "filename=\"concatResult.txt\"").type(MediaType.TEXT_PLAIN);
      }
      return response.build();
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  // ===============================
  // Download files by unique link

  /**
   * Download zip by unique link
   * @param requestId id of request
   * @return response with zip
   */
  @GET
  @Path("/zip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces("application/zip")
  public Response zipByRequestId(@QueryParam("requestId") String requestId) {
    LOG.info("Starting zip download requestId : {}", requestId);
    try {
      DownloadRequest request = getDownloadRequest(requestId);
      return downloadGZip(request);
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Generate link for zip
   * @param request download request
   * @return response wth request id
   * @see #zipByRequestId(String)
   */
  @POST
  @Path("/zip/generate-link")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response zipGenerateLink(final DownloadRequest request) {
    LOG.info("starting generate-link");
    return generateLink(request);
  }

  /**
   * Concatenate files by unique link
   * @param requestId id of request
   * @return response with concatenated files
   */
  @GET
  @Path("/concat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response concatByRequestId(@QueryParam("requestId") String requestId) {
    LOG.info("Starting concat for requestId : {}", requestId);
    try {
      DownloadRequest request = getDownloadRequest(requestId);
      return concat(request);
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred : ", ex);
      throw ex;
    } catch (Exception ex) {
      LOG.error("Error occurred : ", ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Generate link for concat
   * @param request download request
   * @return response wth request id
   * @see #concatByRequestId(String)
   */
  @POST
  @Path("/concat/generate-link")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response concatGenerateLink(final DownloadRequest request) {
    LOG.info("Starting link generation for concat");
    return generateLink(request);
  }

  private Response generateLink(DownloadRequest request) {
    try {
      String requestId = generateUniqueIdentifer(request);
      LOG.info("returning generated requestId : {}", requestId);
      JSONObject json = new JSONObject();
      json.put("requestId", requestId);
      return Response.ok(json).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private DownloadRequest getDownloadRequest(String requestId) throws HdfsApiException, IOException, InterruptedException {
    String fileName = getFileNameForRequestData(requestId);
    String json = HdfsUtil.readFile(getApi(), fileName);
    DownloadRequest request = gson.fromJson(json, DownloadRequest.class);

    deleteFileFromHdfs(fileName);
    return request;
  }

  private Gson gson = new Gson();

  private String generateUniqueIdentifer(DownloadRequest request) {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    String json = gson.toJson(request);
    writeToHdfs(uuid, json);
    return uuid;
  }

  private void writeToHdfs(String uuid, String json) {
    String fileName = getFileNameForRequestData(uuid);
    try {
      HdfsUtil.putStringToFile(getApi(), fileName, json);
    } catch (HdfsApiException e) {
      LOG.error("Failed to write request data to HDFS", e);
      throw new ServiceFormattedException("Failed to write request data to HDFS", e);
    }
  }

  private String getFileNameForRequestData(String uuid) {
    String tmpPath = context.getProperties().get("tmp.dir");
    if (tmpPath == null) {
      LOG.error("tmp.dir is not configured!");
      throw new MisconfigurationFormattedException("tmp.dir");
    }
    return String.format(tmpPath + "/%s.json", uuid);
  }

  private void deleteFileFromHdfs(String fileName) throws IOException, InterruptedException {
    getApi().delete(fileName, true);
  }


  /**
   * Wrapper for json mapping of download request
   */
  public static class DownloadRequest {
    @XmlElement(nillable = false, required = true)
    public String[] entries;
    @XmlElement(required = false)
    public boolean download;
  }
}
