/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.config.module.screen;

import com.alibaba.citrus.turbine.Context;
import com.opensymphony.xwork2.Action;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.DefaultFilter;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.runtime.module.action.HdfsAction;
import com.qlangtech.tis.runtime.module.screen.BasicScreen;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * http://localhost:8080/tjs/config/stream_script_repo.action?path=/streamscript/search4totalpay/20190820171040/mq_meta/config.yaml
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StreamScriptRepo extends BasicScreen {

  private static final Logger logger = LoggerFactory.getLogger(StreamScriptRepo.class);

  @Override
  @Func(value = PermissionConstant.PERMISSION_PLUGIN_GET_CONFIG, sideEffect = false)
  public void execute(Context context) throws Exception {
    File rootDir = Config.getDataDir();
    String path = this.getString("path");
    if (StringUtils.isEmpty(path)) {
      throw new IllegalArgumentException("illegal argument 'path'");
    }
    HttpServletResponse response = (HttpServletResponse) DefaultFilter.getRespone();
    File targetFile = new File(rootDir, path);
    if (!targetFile.exists()) {
      // throw new IllegalStateException("target file not exist:" + targetFile.getAbsolutePath());
      response.addHeader(ConfigFileContext.KEY_HEAD_FILE_NOT_EXIST
        , String.valueOf(Boolean.TRUE.booleanValue()));
      return;
    }
    boolean getMeta = Boolean.parseBoolean(this.getRequest().getHeader(ConfigFileContext.StreamProcess.HEADER_KEY_GET_FILE_META));
    logger.info("path:{},getChildren:{},local file exist:{},getMeta:{}", path, !targetFile.isFile(), targetFile.exists(), getMeta);
    if (targetFile.isFile()) {
      // 是否取文件meta信息
      response = HdfsAction.getDownloadResponse(targetFile, !getMeta);
      if (!getMeta) {
        try (InputStream input = FileUtils.openInputStream(targetFile)) {
          IOUtils.copyLarge(input, response.getOutputStream());
        }
      }
    } else {

      response.addHeader(ConfigFileContext.KEY_HEAD_FILE_DOWNLOAD, String.valueOf(false));
      List<String> subs = new ArrayList<>();
      File sub = null;
      for (String d : targetFile.list((d, fn) -> !StringUtils.endsWith(fn, CenterResource.KEY_LAST_MODIFIED_EXTENDION))) {
        sub = new File(targetFile, d);
        subs.add(d + ":" + (sub.isDirectory() ? "d" : "f"));
      }
      response.addHeader(ConfigFileContext.KEY_HEAD_FILES, subs.stream().collect(Collectors.joining(",")));


      IOUtils.write(com.qlangtech.tis.extension.impl.IOUtils.writeZip(targetFile), response.getOutputStream());
      // 将目录中的内容全部写到zip流中去
//      try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
//        ZipOutputStream zipOut = new ZipOutputStream(output, TisUTF8.get());
//        iterateAllFile(targetFile, StringUtils.EMPTY, (childFile, subPath) -> {
//          zipOut.putNextEntry(new ZipEntry(subPath));
//          zipOut.write(FileUtils.readFileToByteArray(childFile));
//          zipOut.closeEntry();
//        });
//        zipOut.flush();
//        IOUtils.write(output.toByteArray(), response.getOutputStream());
//      }
    }
  }


  @Override
  protected String getReturnCode() {
    return Action.NONE;
  }
}
