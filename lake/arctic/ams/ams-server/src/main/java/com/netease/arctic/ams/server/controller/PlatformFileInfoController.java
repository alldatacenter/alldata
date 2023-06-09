/*
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

package com.netease.arctic.ams.server.controller;

import com.netease.arctic.ams.server.controller.response.ErrorResponse;
import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.PlatformFileInfoService;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class PlatformFileInfoController extends RestBaseController {

  private static PlatformFileInfoService platformFileInfoService = ServiceContainer.getPlatformFileInfoService();

  /**
   * uplaod file
   *
   * @param ctx
   */
  public static void uploadFile(Context ctx) {
    try {
      InputStream bodyAsInputStream = ctx.uploadedFile("file").getContent();
      //todo get file name
      String name = ctx.uploadedFile("file").getFilename();
      byte[] bytes = IOUtils.toByteArray(bodyAsInputStream);
      String content = Base64.getEncoder().encodeToString(bytes);
      Integer fid = platformFileInfoService.addFile(name, content);
      Map<String, String> result  = new HashMap<>();
      result.put("id", String.valueOf(fid));
      result.put("url", "/ams/v1/files/" + fid);
      ctx.json(OkResponse.of(result));
    } catch (IOException e) {
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Failed to upload file", null));
    }
  }

  /**
   * download file
   *
   * @param ctx
   */
  public static void downloadFile(Context ctx) {
    String fileId = ctx.pathParam("fileId");
    if (!StringUtils.isNumeric(fileId)) {
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Invalid file id", null));
      return;
    }
    String content = platformFileInfoService.getFileContentById(Integer.valueOf(fileId));
    ctx.result(content);
  }
}
