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
//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import com.google.common.collect.Lists;
//import com.opensymphony.xwork2.ActionContext;
//import com.qlangtech.tis.manage.common.*;
//import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
//import com.qlangtech.tis.runtime.module.action.IParamGetter;
//import com.qlangtech.tis.runtime.module.screen.IndexQuery.QueryRequestContext;
//import com.qlangtech.tis.solrdao.SolrFieldsParser;
//import com.qlangtech.tis.solrdao.impl.ParseResult;
//import com.qlangtech.tis.solrdao.SolrFieldsParser.SchemaFields;
//import com.qlangtech.tis.solrdao.pojo.PSchemaField;
//import junit.framework.Assert;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang.StringUtils;
//import org.apache.commons.lang3.StringEscapeUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.SolrRequest.METHOD;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.apache.solr.common.SolrDocument;
//import org.apache.solr.common.SolrDocumentList;
//import org.apache.solr.common.params.CommonParams;
//import org.apache.solr.common.params.FacetParams;
//import org.apache.solr.common.params.SolrParams;
//import org.apache.solr.common.util.SimpleOrderedMap;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.*;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2012-4-13
// */
//public class QueryIndexServlet extends BasicServlet {
//
//    private static final long serialVersionUID = 1L;
//
//    public static final String FIELD_SHARD = "[shard]";
//
//    public static final Pattern PATTERN_SHARD_INFO = Pattern.compile("http://(.+?):(.+?)_shard(\\d+?)_.+?");
//
//    private static final ExecutorService threadPool = java.util.concurrent.Executors.newCachedThreadPool();
//
//    private static final Logger log = LoggerFactory.getLogger(QueryIndexServlet.class);
//
//    // private ZkStateReader zkStateReader;
//    // private SolrZkClient solrZkClient;
//    private Cache<String, SchemaFields> /* collection name */
//    schemaFieldsCache;
//
//    public QueryIndexServlet() {
//        super();
//    }
//
//    @Override
//    public void init() throws ServletException {
//        this.schemaFieldsCache = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
//    }
//
//    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        List<String> emptylist = Collections.emptyList();
//        doQuery(req, resp, CheckAppDomainExistValve.getAppDomain(req, this.getContext()), emptylist);
//    }
//
//    // private static final GeneralXMLResponseParser RESPONSE_PARSE = new
//    // GeneralXMLResponseParser();
//    @Override
//    protected void doPost(final HttpServletRequest request, final HttpServletResponse resp) throws ServletException, IOException {
//        String[] sfields = request.getParameterValues("sfields");
//        doQuery(request, resp, CheckAppDomainExistValve.getAppDomain(request, this.getContext()), Arrays.asList(sfields));
//    }
//
//    public void doQuery(final HttpServletRequest request, final HttpServletResponse resp, AppDomainInfo domain, List<String> sfields) throws ServletException, IOException {
//        request.setAttribute("selectedFields", sfields);
//        request.setCharacterEncoding(TisUTF8.getName());
//        final String query = StringUtils.defaultIfEmpty((request.getParameter("q")).replaceAll("\r|\n", StringUtils.EMPTY), "*:*");
//        Integer shownum = null;
//        try {
//            shownum = Integer.parseInt(request.getParameter("shownum"));
//        } catch (Throwable e2) {
//        }
//        QueryRequestContext requestContext = new QueryRequestContext(request);
//        final String sort = request.getParameter("sort");
//        final String[] fqs = request.getParameterValues("fq");
//        if (domain instanceof com.qlangtech.tis.pubhook.common.Nullable) {
//            throw new IllegalStateException("domain can not be nullable ");
//        }
//        final QueryResutStrategy queryResutStrategy = createQueryResutStrategy(domain, request, resp, this.getContext());
//        final List<ServerJoinGroup> serverlist = queryResutStrategy.queryProcess();
//
//        execuetQuery(null, domain, requestContext, this.getContext(), queryResutStrategy, serverlist, query, sort, fqs, shownum, sfields);
//    // }
//    }
//
//    private void setSfields(final HttpServletRequest request, AppDomainInfo domain, QueryRequestContext requestContext, final QueryResutStrategy queryResutStrategy, final List<ServerJoinGroup> serverlist) throws ServletException {
//        List<PSchemaField> fieldList = null;
//        fieldList = this.schemaFieldsCache.getIfPresent(domain.getAppName());
//        try {
//            if (fieldList == null) {
//                fieldList = this.schemaFieldsCache.get(domain.getAppName(), () -> {
//                    getSchemaFrom1Server(domain, requestContext, queryResutStrategy, serverlist);
//                    return requestContext.schema.dFields;
//                });
//            }
//        } catch (ExecutionException e1) {
//            throw new ServletException(e1);
//        }
//        request.setAttribute("sfields", fieldList);
//    }
//
//    /**
//     * @param domain
//     * @param requestContext
//     * @param queryResutStrategy
//     * @param serverlist
//     * @throws ServletException
//     */
//    private void getSchemaFrom1Server(AppDomainInfo domain, QueryRequestContext requestContext
//      , final QueryResutStrategy queryResutStrategy, final List<ServerJoinGroup> serverlist) throws ServletException {
//        // boolean isSuccessGet = false;
//        for (ServerJoinGroup server : serverlist) {
//            try {
//                requestContext.schema = processSchema(// http://http://10.1.4.145:8080/solr/search4shop_shard1_replica1/:0/solr/search4shopadmin/file/?file=schema.xml
//                queryResutStrategy.getRequest(), "http://" + server.getIp() + ":8080/solr/" + domain.getAppName());
//                // isSuccessGet = true;
//                return;
//            } catch (Exception e) {
//                log.warn(e.getMessage(), e);
//            }
//        }
//        requestContext.schema = new ParseResult(false);
//    // StringBuffer servers = new StringBuffer();
//    // for (ServerJoinGroup server : serverlist) {
//    // servers.append("[").append(server.getIpAddress()).append("]");
//    //
//    // }
//    // throw new ServletException("remote server faild,remote servers:" +
//    // servers.toString());
//    }
//
//    public static // , boolean queryResultAware
//    QueryResutStrategy createQueryResutStrategy(// , boolean queryResultAware
//    AppDomainInfo domain, // , boolean queryResultAware
//    final HttpServletRequest request, // , boolean queryResultAware
//    final HttpServletResponse resp, RunContext runContext) {
//        final SolrQueryModuleCreatorAdapter creatorAdapter = new SolrQueryModuleCreatorAdapter() {
//
//            @Override
//            public boolean schemaAware() {
//                return false;
//            }
//
//            @Override
//            public SolrParams build(IParamGetter params, final String querystr, final String sort, final String[] fqs, final Integer shownumf, final List<String> showFields) {
//                SolrQuery query = new SolrQuery();
//                // 增加排序字段
//                if (StringUtils.isNotBlank(sort)) {
//                    query.add(CommonParams.SORT, sort);
//                }
//                // query.add(CommonParams.Q, querystr);
//                query.setQuery(querystr);
//                if (fqs != null) {
//                    for (String fq : fqs) {
//                        query.add(CommonParams.FQ, fq);
//                    }
//                }
//                boolean facet = params.getBoolean(FacetParams.FACET);
//                if (facet) {
//                    String facetField = params.getString(FacetParams.FACET_FIELD);
//                    if (StringUtils.isNotEmpty(facetField)) {
//                        query.addFacetField(facetField);
//                    }
//                    String facetQuery = params.getString(FacetParams.FACET_QUERY);
//                    if (StringUtils.isNotEmpty(facetQuery)) {
//                        query.addFacetQuery(facetQuery);
//                    }
//                    String facetPrefix = params.getString(FacetParams.FACET_PREFIX);
//                    if (StringUtils.isNotEmpty(facetPrefix)) {
//                        query.setFacetPrefix(facetPrefix);
//                    }
//                }
//                query.add(CommonParams.START, params.getString(CommonParams.START, "0"));
//                // 默认显示前三行
//                query.setRows(shownumf);
//                // query.add(CommonParams.ROWS, String.valueOf(shownumf));
//                query.add(CommonParams.VERSION, "2.2");
//                query.add(CommonParams.WT, "xml");
//                boolean distrib = params.getBoolean(CommonParams.DISTRIB);
//                query.add(CommonParams.DISTRIB, String.valueOf(distrib));
//                if (showFields.isEmpty()) {
//                    if (distrib) {
//                        query.setFields("*", FIELD_SHARD);
//                    }
//                } else {
//                    for (String field : showFields) {
//                        query.addField(field);
//                    }
//                    if (distrib) {
//                        query.addField(FIELD_SHARD);
//                    }
//                }
//                query.setShowDebugInfo(params.getBoolean(CommonParams.DEBUG));
//                return query;
//            }
//
//            @Override
//            public ParseResult processSchema(InputStream schemaStream) {
//                try {
//                    // SolrFieldsParser schemaParser = new SolrFieldsParser();
//                    ParseResult parseResult = SolrFieldsParser.parse(() -> IOUtils.toByteArray(schemaStream)).getSchemaParseResult();
//                    return parseResult;
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            @Override
//            public String[] getParameterValues(String keyname) {
//                return request.getParameterValues(keyname);
//            }
//
//            @Override
//            public void handleError(String url, ServerJoinGroup server, long allcount, Exception e) {
//                synchronized (resp) {
//                    try {
//                        String serverInfo = "[" + server.getGroupIndex() + "]" + server.getIp();
//                        flushResult(resp, request, Arrays.asList(convert2Html("url:" + url + "<br/>" + getErrorContent(e), serverInfo)), allcount);
//                    } catch (IOException e1) {
//                    }
//                }
//            }
//
//            @Override
//            public void processResult(QueryRequestContext qrequest, QueryResponse result, ServerJoinGroup server) throws Exception {
//                flushResult(resp, qrequest.request, convert2Html(qrequest, result, server, false), qrequest.resultCount.get());
//            }
//
//            @Override
//            public void processResult(QueryRequestContext qrequest, List<Row> rows) throws Exception {
//                flushResult(resp, qrequest.request, rows, qrequest.resultCount.get());
//            }
//
//            @Override
//            public void setQuerySelectServerCandiate(Map<String, List<ServerJoinGroup>> servers) {
//                request.setAttribute("querySelectServerCandiate", servers);
//            }
//        };
//        final QueryResutStrategy queryResutStrategy = SolrCloudQueryResutStrategy.create(domain, creatorAdapter, runContext);
//        return queryResutStrategy;
//    }
//
//    public static String getErrorContent(Throwable e) {
//        StringWriter reader = new StringWriter();
//        PrintWriter errprint = null;
//        try {
//            errprint = new PrintWriter(reader);
//            e.printStackTrace(errprint);
//            return processContent2Json(reader.toString());
//        // StringUtils.trimToEmpty(
//        // StringUtils.replace(reader.toString(), "\"", "'"))
//        // .replaceAll("(\r|\n|\t)+", "<br/>");
//        } finally {
//            IOUtils.closeQuietly(errprint);
//        }
//    }
//
//    /**
//     * 执行查询逻辑和使用的场景无关
//     *
//     * @param domain
//     * @param
//     * @param getContext
//     */
//    public static void execuetQuery(IParamGetter params, final AppDomainInfo domain, final QueryRequestContext requestContext
//      , RunContext getContext, final QueryResutStrategy queryResutStrategy, final List<ServerJoinGroup> serverlist
//      , final String querystr, final String sort, final String[] fqs, final Integer shownumf, final List<String> showFields) {
//
//        Assert.assertNotNull("param SolrQueryModuleCreator can not be null", queryResutStrategy.getRequest());
//        Assert.assertNotNull(queryResutStrategy);
//        Assert.assertNotNull(serverlist);
//        final ActionContext context = ActionContext.getContext();
//        final boolean distrib = params.getBoolean(CommonParams.DISTRIB);
//        final CountDownLatch lock = new CountDownLatch(distrib ? 1 : serverlist.size());
//        final AtomicBoolean hasGetSchema = new AtomicBoolean(false);
//        List<Row> rows = Lists.newArrayList();
//        synchronized (lock) {
//            for (final ServerJoinGroup server : serverlist) {
//                threadPool.execute(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        ActionContext.setContext(context);
//                        // 组装url
//                        final String url = server.getIpAddress();
//                        try {
//                            QueryCloudSolrClient solrClient = new QueryCloudSolrClient(url);
//                            QueryResponse result = solrClient.query(domain.getAppName(), queryResutStrategy.getRequest().build(params, querystr, sort, fqs, shownumf, showFields), METHOD.POST);
//                            solrClient.close();
//                            long c = result.getResults().getNumFound();
//                            if (c < 1) {
//                                return;
//                            }
//                            // count.add(c);
//                            requestContext.add(c);
//                            rows.addAll(convert2Html(requestContext, result, server, distrib));
//                        // queryResutStrategy.getRequest().processResult(requestContext, result, server);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            queryResutStrategy.getRequest().handleError(url, server, requestContext.resultCount.get(), e);
//                        } finally {
//                            lock.countDown();
//                        }
//                    }
//                });
//                if (distrib) {
//                    break;
//                }
//            }
//        }
//        try {
//            lock.await();
//            queryResutStrategy.getRequest().processResult(requestContext, rows);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private static ParseResult processSchema(final SolrQueryModuleCreator creator, final String url) throws MalformedURLException {
//        return ConfigFileContext.processContent(new URL(url + "/admin/file/?file=schema.xml"), new StreamProcess<ParseResult>() {
//
//            @Override
//            public ParseResult p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//                return creator.processSchema(stream);
//            }
//        });
//    }
//
//    public static interface SolrQueryModuleCreator {
//
//        public SolrParams build(IParamGetter params, final String querystr, final String sort, final String[] fqs, final Integer shownumf, final List<String> showField);
//
//        public void processResult(QueryRequestContext qrequest, QueryResponse result, ServerJoinGroup server) throws Exception;
//
//        public void processResult(QueryRequestContext qrequest, List<Row> rows) throws Exception;
//
//        public void handleError(String url, ServerJoinGroup server, long allcount, Exception e);
//
//        // public void selectedCanidateServers(final Collection<String> selectedCanidateServers);
//        public void setQuerySelectServerCandiate(Map<String, List<ServerJoinGroup>> servers);
//
//        /**
//         * 处理schema流
//         *
//         * @param schemaStream
//         */
//        public ParseResult processSchema(InputStream schemaStream);
//
//        public boolean schemaAware();
//
//        // public boolean queryResultAware();
//        /**
//         * 取得查询参数
//         *
//         * @param keyname
//         * @return
//         */
//        public String[] getParameterValues(String keyname);
//    }
//
//    public static void flushResult(final HttpServletResponse resp, final HttpServletRequest req, List<Row> rowlist, long allcount) throws IOException {
//        Assert.assertNotNull(req);
//        String callback = req.getParameter("callback");
//        Assert.assertNotNull("param callback can not be null", callback);
//        // rowlist = new ArrayList<Row>(rowlist);
//        synchronized (resp) {
//            PrintWriter writer = resp.getWriter();
//            writer.write(callback + "(");
//            JSONObject j = new JSONObject();
//            JSONArray rows = new JSONArray();
//            JSONObject rr = null;
//            for (Row r : rowlist) {
//                rr = new JSONObject();
//                rr.put("server", r.getServer());
//                rr.put("rowContent", r.getRowContent());
//                rows.put(rr);
//            }
//            j.put("result", rows);
//            j.put("rownum", allcount);
//            writer.write(j.toString(1));
//            writer.write(");\n");
//        }
//    }
//
//    private static class ResultCount {
//
//        private AtomicLong value = new AtomicLong();
//
//        public void add(long v) {
//            // this.value += v;
//            value.addAndGet(v);
//        }
//    }
//
//    @SuppressWarnings("all")
//    private static List<Row> convert2Html(QueryRequestContext requestContext, QueryResponse response, ServerJoinGroup server, boolean distrib) {
//        // StringBuffer result = new StringBuffer();
//        //
//        SolrDocumentList solrDocumentList = response.getResults();
//        String uniqueKey = null;
//        List<Row> result = new ArrayList<Row>();
//        Row record = null;
//        String shardVal = null;
//        SimpleOrderedMap explain = null;
//        Matcher shardMatcher = null;
//        if (requestContext.queryDebug) {
//            explain = (SimpleOrderedMap) response.getDebugMap().get("explain");
//        }
//        for (SolrDocument document : solrDocumentList) {
//            StringBuffer temp = new StringBuffer();
//            for (String key : document.getFieldNames()) {
//                if (FIELD_SHARD.equals(key)) {
//                    continue;
//                }
//                temp.append("<strong>").append(key).append("</strong>");
//                temp.append(":").append(processContent2Json(String.valueOf(document.get(key)))).append(" ");
//            }
//            String serverInfo = "[" + server.getGroupIndex() + "]" + server.getIp();
//            if (distrib) {
//                shardVal = String.valueOf(document.getFirstValue(FIELD_SHARD));
//                shardMatcher = QueryIndexServlet.PATTERN_SHARD_INFO.matcher(shardVal);
//                if (shardMatcher.matches()) {
//                    serverInfo = "[" + shardMatcher.group(3) + "]" + shardMatcher.group(1);
//                }
//            }
//            record = convert2Html(temp.toString(), serverInfo);
//            if (requestContext.queryDebug) {
//                uniqueKey = getUniqueKey(requestContext, document);
//                record.setPk(uniqueKey);
//                record.explain = processContent2Json(String.valueOf(explain.get(uniqueKey)));
//            }
//            result.add(record);
//        }
//        return result;
//    }
//
//    public static String processContent2Json(String content) {
//        return StringUtils.trimToEmpty(StringEscapeUtils.escapeHtml4(content)).replaceAll("(\r|\n)+", "<br/>").replaceAll("\\s|\t", "&nbsp;");
//    }
//
//    private static String getUniqueKey(QueryRequestContext request, SolrDocument document) {
//        Assert.assertNotNull("request.schema can not be null", request.schema);
//        return String.valueOf(document.get(request.schema.getUniqueKey()));
//    }
//
//    public static Row convert2Html(String rightcell, String serverInfo) {
//        Row result = new Row();
//        result.setRowContent(rightcell);
//        result.setServer(serverInfo);
//        return result;
//    }
//
//    public static class Row {
//
//        private String server;
//
//        private String rowContent;
//
//        private String explain;
//
//        private String pk;
//
//        String getExplain() {
//            return explain;
//        }
//
//        void setExplain(String explain) {
//            this.explain = explain;
//        }
//
//        public String getServer() {
//            return server;
//        }
//
//        public void setServer(String server) {
//            this.server = server;
//        }
//
//        public String getRowContent() {
//            return rowContent;
//        }
//
//        private String getPk() {
//            return pk;
//        }
//
//        private void setPk(String pk) {
//            this.pk = pk;
//        }
//
//        public void setRowContent(String rowContent) {
//            this.rowContent = rowContent;
//        }
//    }
//}
