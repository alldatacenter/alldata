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
//package com.qlangtech.tis.runtime.module.screen;
//
//import com.alibaba.citrus.turbine.Context;
//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import com.qlangtech.tis.manage.common.ConfigFileContext;
//import com.qlangtech.tis.manage.servlet.QueryIndexServlet;
//import com.qlangtech.tis.manage.servlet.QueryResutStrategy;
//import com.qlangtech.tis.manage.servlet.ServerJoinGroup;
//import com.qlangtech.tis.solrdao.SolrFieldsParser;
//import com.qlangtech.tis.solrdao.impl.ParseResult;
//import com.qlangtech.tis.solrdao.pojo.PSchemaField;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletRequestWrapper;
//import java.io.InputStream;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Callable;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/09/25
// */
//public class IndexQuery {
//
//  //
//  // private static final long serialVersionUID = 1L;
//  //
//  private static final Logger logger = LoggerFactory.getLogger(IndexQuery.class);
//
//  //
//  private static final Cache<String, SolrFieldsParser.SchemaFields> /* collection name */
//    schemaFieldsCache;
//
//  //
//  static {
//    schemaFieldsCache = CacheBuilder.newBuilder().expireAfterWrite(6, TimeUnit.MINUTES).build();
//  }
//
//  //
//  // public IndexQuery() {
//  // super();
//  // }
//  //
//  public static List<PSchemaField> getSfields(HttpServletRequest request, QueryResutStrategy queryStrategy, List<ServerJoinGroup> nodes) throws Exception {
//    // return getRequest().getParameterValues("sfields");
//    final String collection = queryStrategy.domain.getAppName();
//    List<PSchemaField> fieldList = null;
//    fieldList = schemaFieldsCache.getIfPresent(collection);
//    if (fieldList == null) {
//      fieldList = schemaFieldsCache.get(collection, new Callable<SolrFieldsParser.SchemaFields>() {
//
//        @Override
//        public SolrFieldsParser.SchemaFields call() throws Exception {
//          QueryRequestContext queryContext = new QueryRequestContext(request);
//          getSchemaFrom1Server(collection, queryContext, queryStrategy, nodes);
//          return queryContext.schema.dFields;
//        }
//      });
//    }
//    return fieldList;
//  }
//
//  //
//  public static class QueryRequestWrapper extends HttpServletRequestWrapper {
//
//    private final Context context;
//
//    public QueryRequestWrapper(HttpServletRequest request, Context context) {
//      super(request);
//      this.context = context;
//    }
//
//    @Override
//    public void setAttribute(String name, Object o) {
//      context.put(name, o);
//    }
//  }
//
//  //
//  private static void getSchemaFrom1Server(String collection, QueryRequestContext requestContext
//    , final QueryResutStrategy queryResutStrategy, final List<ServerJoinGroup> serverlist) throws ServletException {
//
//    for (ServerJoinGroup server : serverlist) {
//      try {
//        requestContext.schema = processSchema(
//          queryResutStrategy.getRequest(), "http://" + server.getIp() + ":8080/solr/" + collection);
//        // isSuccessGet = true;
//        return;
//      } catch (Exception e) {
//        logger.warn(e.getMessage(), e);
//      }
//    }
//    requestContext.schema = new ParseResult(false);
//  }
//
//  //
//  public static class QueryRequestContext {
//
//    // final ResultCount count = new ResultCount();
//    public AtomicLong resultCount = new AtomicLong();
//
//    public final HttpServletRequest request;
//
//    public ParseResult schema;
//
//    public QueryRequestContext(HttpServletRequest request) {
//      super();
//      this.request = request;
//    }
//
//    public void add(long value) {
//      this.resultCount.addAndGet(value);
//    }
//
//    public final boolean queryDebug = false;
//  }
//
//  //
//  private static ParseResult processSchema(final QueryIndexServlet.SolrQueryModuleCreator creator, final String url) throws MalformedURLException {
//    return ConfigFileContext.processContent(new URL(url + "/admin/file/?file=schema.xml"), new ConfigFileContext.StreamProcess<ParseResult>() {
//
//      @Override
//      public ParseResult p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//        return creator.processSchema(stream);
//      }
//    });
//  }
//}
