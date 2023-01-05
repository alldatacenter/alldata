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
package com.qlangtech.tis.runtime.module.screen;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.common.AppDomainInfo;
import com.qlangtech.tis.manage.common.SnapshotDomain;
import com.qlangtech.tis.manage.servlet.DownloadServlet;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.solrdao.IBuilderContext;
import com.qlangtech.tis.solrdao.SolrPojoBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ViewPojo {

  private static final long serialVersionUID = 1L;

  public static interface ResourcePrep {
    public void prepare(IBuilderContext builderContext);
  }

  public static boolean downloadResource(Context context, final AppDomainInfo appDomainInfo, BasicModule module, final Writer writer) throws Exception {
    return downloadResource(context, appDomainInfo, module, writer, new ResourcePrep() {
      @Override
      public void prepare(IBuilderContext builderContext) {
      }
    });
  }

  public static SnapshotDomain getSnapshotDoamin(BasicModule module, final AppDomainInfo appDomainInfo) {
    ServerGroup group = DownloadServlet.getServerGroup(appDomainInfo.getAppid(), (short) 0
      , appDomainInfo.getRunEnvironment().getId(), module.getServerGroupDAO());
    if (group == null) {
      //module.addErrorMessage(context, "您还没有为该应用配置Snapshot");
      // return false;
      throw new IllegalStateException("application:" + appDomainInfo + " can not get relevant serverGroup");
    }
    final SnapshotDomain snapshot = module.getSnapshotViewDAO().getView(group.getPublishSnapshotId());
    return snapshot;
  }

  public static boolean downloadResource(Context context, final AppDomainInfo appDomainInfo
    , BasicModule module, final Writer writer, ResourcePrep prepare) throws Exception {

    final SnapshotDomain snapshot = getSnapshotDoamin(module, appDomainInfo);
    // final StringWriter writer = new StringWriter();
    final IBuilderContext builderContext = new IBuilderContext() {

      @Override
      public void closeWriter(PrintWriter writer) {
      }

      @Override
      public Writer getOutputStream() throws Exception {
        return writer;
      }

      @Override
      public String getPojoName() {
        return StringUtils.capitalize(StringUtils.substringAfter(appDomainInfo.getAppName(), "search4"));
      }

      @Override
      public byte[] getResourceInputStream() {
        return snapshot.getSolrSchema().getContent();
      }

      @Override
      public String getTargetNameSpace() {
        return "com.qlangtech.tis";
      }
    };
    prepare.prepare(builderContext);
    SolrPojoBuilder builder = new SolrPojoBuilder(builderContext);
    builder.create();
    // return writer;
    return true;
  }
}
