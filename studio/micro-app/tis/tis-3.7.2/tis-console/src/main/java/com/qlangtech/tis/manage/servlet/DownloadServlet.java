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

import com.qlangtech.tis.manage.biz.dal.dao.IServerGroupDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroupCriteria;
import com.qlangtech.tis.manage.common.AppDomainInfo;
import com.qlangtech.tis.manage.common.ManageUtils;
import com.qlangtech.tis.manage.common.SnapshotDomain;
import com.qlangtech.tis.pubhook.common.ConfigConstant;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-19
 */
public class DownloadServlet extends BasicServlet {

  private static final Pattern download_pattern = Pattern.compile(".+?/download/publish/(\\d+)/(\\d+)/group(\\d+)/r(\\d+)/("
    + replace(ConfigConstant.FILE_SCHEMA + "|" + ConfigConstant.FILE_SOLR) + "|" + DownloadResource.JAR_NAME + ")");


  public static String replace(String name) {
    return name.replace(".", "\\.");
  }

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    try {
      Matcher matcher = null;
      if (!(matcher = getURLPattern().matcher(request.getRequestURL())).matches()) {
        throw new ServletException("has not match dowload url pattern:" + request.getRequestURL());
      }
      DownloadResource downloadRes = getDownloadResource(matcher);

      StringBuffer writer = getAppendInfo(request, downloadRes);
      ServletOutputStream output = response.getOutputStream();
      response.setContentType(downloadRes.getContentType());
      setDownloadName(response, downloadRes.getFileName());
      response.addHeader("filemd5", downloadRes.getMd5CodeValue());
      response.setContentLength(downloadRes.getFileLength() + writer.length());
      if (writer.length() > 0) {
        output.write(writer.toString().getBytes());
      }
      IOUtils.write(downloadRes.read(), output);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static void setDownloadName(final HttpServletResponse response, String fileName) {
    response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
  }

  protected StringBuffer getAppendInfo(final HttpServletRequest request, DownloadResource downloadRes) throws IOException {
    StringBuffer writer = new StringBuffer();
    writer.append("\n<!--\n");
    writer.append("download date:" + ManageUtils.formatDateYYYYMMdd(new Date())).append("\n");
    writer.append("app:" + downloadRes.getApplication().getProjectName()).append("\n");
    writer.append("request:" + request.getRequestURL()).append("\n");
    writer.append("-->");
    return writer;
  }

  protected Pattern getURLPattern() {
    return download_pattern;
  }

  protected DownloadResource getDownloadResource(Matcher matcher) {
    Integer bizid = Integer.parseInt(matcher.group(1));
    Integer appid = Integer.parseInt(matcher.group(2));
    Short groupIndex = Short.parseShort(matcher.group(3));
    // 开发环境 线上 线下。。。。
    Short runtime = Short.parseShort(matcher.group(4));
    final String resourceName = matcher.group(5);
    final ServerGroup group = getServerGroup(appid, groupIndex, runtime, this.getContext().getServerGroupDAO());
    if (group == null) {
      throw new IllegalStateException("appid:" + appid + ",groupIndex:" + groupIndex + " runtime:" + AppDomainInfo.getRunEnvir(runtime) + " can not find a group");
    }
    if (group.getPublishSnapshotId() == null) {
      throw new IllegalStateException("group id:" + group.getGid() + " group have not set publishSnapshotId");
    }
    Application app = this.getContext().getApplicationDAO().selectByPrimaryKey(appid);
    if (bizid.intValue() != app.getDptId()) {
      throw new IllegalArgumentException("bizid.intValue()" + bizid.intValue() + " != app.getBizId()" + app.getDptId());
    }
    final SnapshotDomain snapshot = this.getContext().getSnapshotViewDAO().getView(group.getPublishSnapshotId());
    // Snapshot snapshot, String resourceName
    return new DownloadResource(app, snapshot, resourceName);
  }

  public static ServerGroup getServerGroup(Integer appid, Short groupIndex, Short runtime, IServerGroupDAO serverGroupDAO) {
    ServerGroupCriteria gcriteria = new ServerGroupCriteria();
    gcriteria.createCriteria().andGroupIndexEqualTo(groupIndex).andRuntEnvironmentEqualTo(runtime).andAppIdEqualTo(appid);
    List<ServerGroup> groupList = serverGroupDAO.selectByExample(gcriteria);
    for (ServerGroup g : groupList) {
      return g;
    }
    return null;
  }
}
