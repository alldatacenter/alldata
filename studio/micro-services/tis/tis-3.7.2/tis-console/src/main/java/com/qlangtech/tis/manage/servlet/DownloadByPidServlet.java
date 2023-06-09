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
package com.qlangtech.tis.manage.servlet;

import com.qlangtech.tis.manage.common.SnapshotDomain;
import com.qlangtech.tis.pubhook.common.ConfigConstant;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过包和snapshot来下载资源包
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-30
 */
public class DownloadByPidServlet extends DownloadServlet {

  private static final long serialVersionUID = -8824528411218625478L;

  // private static final Pattern download_pattern = Pattern
  // .compile(".+?/download/bypid/(\\d+)/snid/(\\d+)/("
  // + replace(ConfigConstant.FILE_APPLICATION + "|"
  // + ConfigConstant.FILE_DATA_SOURCE + "|"
  // + ConfigConstant.FILE_SCHEMA + "|"
  // + ConfigConstant.FILE_SOLOR) + "|"
  // + DownloadResource.JAR_NAME + ")");
  private static final Pattern download_pattern = Pattern.compile(".+?/download/snid/(\\d+)/(" + replace(ConfigConstant.FILE_SCHEMA + "|" + ConfigConstant.FILE_SOLR) + "|" + DownloadResource.JAR_NAME + ")");

  // @Override
  // protected void doGet(HttpServletRequest reqeust,
  // HttpServletResponse response) throws ServletException, IOException {
  //
  // }
  @Override
  protected DownloadResource getDownloadResource(Matcher matcher) {
    // AppPackage pack = this.getContext().getAppPackageDAO()
    // .selectByPrimaryKey(Integer.parseInt(matcher.group(1)));
    SnapshotDomain snapshot = this.getContext().getSnapshotViewDAO().getView(Integer.parseInt(matcher.group(1)));
    // Snapshot snapshot, String resourceName
    return new DownloadResource(this.getContext().getApplicationDAO().selectByPrimaryKey(snapshot.getAppId()), snapshot, matcher.group(2));
    // return super.getDownloadResource(matcher);
    // return resource;
  }

  @Override
  protected StringBuffer getAppendInfo(HttpServletRequest request, DownloadResource downloadRes) throws IOException {
    return new StringBuffer();
  }

  @Override
  protected Pattern getURLPattern() {
    return download_pattern;
  }
}
