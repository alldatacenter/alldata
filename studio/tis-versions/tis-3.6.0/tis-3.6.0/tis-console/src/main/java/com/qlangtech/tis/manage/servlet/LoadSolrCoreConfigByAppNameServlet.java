///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.manage.servlet;
//
//import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
//import com.qlangtech.tis.manage.common.*;
//import com.qlangtech.tis.manage.util.LRUCache;
//import com.qlangtech.tis.openapi.SnapshotNotFindException;
//import com.qlangtech.tis.openapi.impl.AppKey;
//import com.qlangtech.tis.openapi.impl.SnapshotDomainGetter;
//import com.qlangtech.tis.pubhook.common.ConfigConstant;
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * http://127.0.0.1/download/appconfig/appname/0/$runtimeEnvironment/schema.xml/solrconfig.xml
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2012-5-2
// */
//public class LoadSolrCoreConfigByAppNameServlet extends BasicServlet {
//
//  private static final long serialVersionUID = 1L;
//
//  private static final Logger log = LoggerFactory.getLogger(LoadSolrCoreConfigByAppNameServlet.class);
//
//  private static final Pattern resourcePattern = Pattern.compile(DownloadServlet.replace(ConfigConstant.FILE_SCHEMA + "|" + ConfigConstant.FILE_SOLR));
//
//  public static final String pattern_runtime = "/(" + RunEnvironment.DAILY.getKeyName() + "|" + RunEnvironment.ONLINE.getKeyName() + ")";
//
//  private static final Pattern pattern = Pattern.compile(".+?/download/appconfig/(.+?)/(\\d{1,})" + pattern_runtime + "((/(" + resourcePattern.pattern() + "))+)");
//
//  protected String getResources(HttpServletRequest request) throws ServletException {
//    return this.getMatcher(request).group(4);
//  }
//
//  protected final Matcher getMatcher(HttpServletRequest request) throws ServletException {
//    Matcher matcher = null;
//    if (!(matcher = getUrlPattern().matcher(request.getRequestURL())).matches()) {
//      throw new ServletException("has not match dowload url pattern:" + request.getRequestURL());
//    }
//    return matcher;
//  }
//
//  protected final Pattern getUrlPattern() {
//    return pattern;
//  }
//
//  protected final boolean isFindAll() {
//    return false;
//  }
//
//  private static final LRUCache<Integer, SnapshotDomain> resourceCache = new LRUCache<Integer, SnapshotDomain>();
//
//  static {
//    Map<String, String> args = new HashMap<String, String>();
//    args.put("name", "resourceCache");
//    args.put("size", "200");
//    resourceCache.init(args, null);
//  }
//
//  @Override
//  protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
//    Matcher matcher = getMatcher(request);
//    final AppKey appKey = new AppKey(matcher.group(1), /* appName ========== */
//      Short.parseShort(matcher.group(2)), /* groupIndex */
//      RunEnvironment.getEnum(matcher.group(3)), "true".equals(request.getParameter(HttpConfigFileReader.unmergeglobalparams)));
//    setSnapshotId(request, appKey);
//    log.warn("getres " + appKey.appName + ",unmerge:" + appKey.unmergeglobalparams + ",client ip:" + request.getRemoteAddr() + "=>" + request.getRequestURI());
//    List<PropteryGetter> needRes = new ArrayList<PropteryGetter>();
//    matcher = resourcePattern.matcher(getResources(request));
//    while ((matcher).find()) {
//      needRes.add(ConfigFileReader.createPropertyGetter(matcher.group()));
//    }
//    appKey.setFromCache(true);
//    SnapshotDomain colon = getSnapshotDomain(needRes, appKey, this.getContext());
//    this.wirteXml2Client(resp, colon);
//  }
//
//  /**
//   * @param request
//   * @param appKey
//   */
//  private void setSnapshotId(HttpServletRequest request, final AppKey appKey) {
//    Long targetSnapshotId = null;
//    try {
//      targetSnapshotId = Long.parseLong(request.getParameter("snapshotid"));
//      if (targetSnapshotId > 0) {
//        appKey.setTargetSnapshotId(targetSnapshotId);
//      }
//    } catch (Throwable e) {
//    }
//  }
//
//  /**
//   * @param
//   * @param appKey
//   * @return
//   * @throws ServletException
//   */
//  public static SnapshotDomain getSnapshotDomain(
//    List<PropteryGetter> needRes, final AppKey appKey, RunContext runContext) throws ServletException {
//    SnapshotDomain snapshot = null;
//    snapshot = resourceCache.get(appKey.hashCode());
//    try {
//      if (!appKey.isFromCache() || snapshot == null) {
//        log.info("key relevant snapshot is null,key:" + appKey.toString());
//        synchronized (resourceCache) {
//          snapshot = resourceCache.get(appKey.hashCode());
//          if (!appKey.isFromCache() || snapshot == null) {
//            SnapshotDomainGetter snapshotDomainGetter = new SnapshotDomainGetter(runContext);
//            snapshot = snapshotDomainGetter.getSnapshot(appKey);
//            snapshot = getSnapshot(true, needRes, snapshot);
//            resourceCache.put(appKey.hashCode(), snapshot);
//          }
//        }
//      } else {
//        log.info("key relevant snapshot not null,key:" + appKey.toString());
//      }
//    } catch (SnapshotNotFindException e) {
//      throw new ServletException(e);
//    }
//    SnapshotDomain colon = getSnapshot(false, needRes, snapshot);
//    return colon;
//  }
//
//  private static SnapshotDomain getSnapshot(boolean isFindAll, List<PropteryGetter> needRes, SnapshotDomain snapshot) throws ServletException {
//    SnapshotDomain colon = new SnapshotDomain(snapshot.getSnapshot());
//    for (PropteryGetter getter : needRes) {
//      if (isFindAll || ConfigConstant.FILE_SCHEMA.equals(getter.getFileName())) {
//        colon.setSolrSchema(snapshot.getSolrSchema());
//        debugResContent(snapshot.getSolrSchema());
//      }
//      if (isFindAll || ConfigConstant.FILE_SOLR.equals(getter.getFileName())) {
//        colon.setSolrConfig(snapshot.getSolrConfig());
//        debugResContent(snapshot.getSolrConfig());
//      }
//      if (isFindAll) {
//        break;
//      }
//    }
//    return colon;
//  }
//
//  private static void debugResContent(UploadResource res) {
//  }
//
//  public static void main(String[] arg) {
//  }
//}
