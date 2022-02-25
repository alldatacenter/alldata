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

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.exceptions.NotFoundFormattedException;
import org.apache.ambari.view.commons.exceptions.ServiceFormattedException;
import org.apache.ambari.view.commons.hdfs.HdfsService;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * File Preview Service
 */
public class FilePreviewService extends HdfsService {
  protected static final Logger LOG = LoggerFactory.getLogger(FilePreviewService.class);

  private CompressionCodecFactory compressionCodecFactory;

  public FilePreviewService(ViewContext context) {
    super(context);

    initCompressionCodecFactory();
  }

  private void initCompressionCodecFactory() {
    Configuration conf = new Configuration();
    conf.set("io.compression.codecs","org.apache.hadoop.io.compress.GzipCodec," +
      "org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec," +
      "org.apache.hadoop.io.compress.BZip2Codec");

    compressionCodecFactory = new CompressionCodecFactory(conf);
  }

  /**
   * @param context
   * @param viewConfigs : extra properties that needs to be included into configs
   */
  public FilePreviewService(ViewContext context, Map<String, String> viewConfigs) {
    super(context, viewConfigs);
    initCompressionCodecFactory();
  }

  @GET
  @Path("/file")
  @Produces(MediaType.APPLICATION_JSON)
  public Response previewFile(@QueryParam("path") String path, @QueryParam("start") int start, @QueryParam("end") int end) {
    LOG.info("previewing file {}, from start {}, till end {}", path, start, end);
    try {
      HdfsApi api = getApi();
      FileStatus status = api.getFileStatus(path);

      CompressionCodec codec = compressionCodecFactory.getCodec(status.getPath());

      // check if we have a compression codec we need to use
      InputStream stream = (codec != null) ? codec.createInputStream(api.open(path)) : api.open(path);

      int length = end - start;
      byte[] bytes = new byte[length];

      if (start != 0) IOUtils.skip(stream, start);
      int readBytes = IOUtils.read(stream, bytes);
      boolean isFileEnd = false;

      if (readBytes < length) isFileEnd = true;

      JSONObject response = new JSONObject();
      response.put("data", new String(bytes));
      response.put("readbytes", readBytes);
      response.put("isFileEnd", isFileEnd);

      return Response.ok(response).build();
    } catch (WebApplicationException ex) {
      LOG.error("Error occurred while previewing {} : ", path, ex);
      throw ex;
    } catch (FileNotFoundException ex) {
      LOG.error("Error occurred while previewing {} : ", path, ex);
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      LOG.error("Error occurred while previewing {} : ", path, ex);
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }
}
