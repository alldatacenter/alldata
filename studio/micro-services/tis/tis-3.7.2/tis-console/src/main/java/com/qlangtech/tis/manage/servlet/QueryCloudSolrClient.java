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
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.client.solrj.SolrRequest;
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.impl.*;
//import org.apache.solr.common.util.NamedList;
//import java.io.IOException;
//import java.util.Collections;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2015年10月7日 下午10:29:30
// */
//public class QueryCloudSolrClient extends SolrClient {
//
//    private static final long serialVersionUID = 1L;
//
//    private static final LBHttpSolrClient lbClient;
//
//    static {
//        // CloseableHttpClient myClient = HttpClientUtil.createClient(null);
//        // lbClient = new LBHttpSolrClient(myClient);
//        // lbClient.setRequestWriter(new BinaryRequestWriter());
//        // lbClient.setParser(new BinaryResponseParser());
//        CloseableHttpClient myClient = HttpClientUtil.createClient(null);
//        LBHttpSolrClient.Builder clientBuilder = new LBHttpSolrClient.Builder();
//        clientBuilder.withHttpClient(myClient);
//        clientBuilder.withResponseParser(new BinaryResponseParser());
//        clientBuilder.withSocketTimeout(40000);
//        lbClient = clientBuilder.build();
//        lbClient.setRequestWriter(new BinaryRequestWriter());
//    }
//
//    private final String applyUrl;
//
//    public QueryCloudSolrClient(final String applyUrl) {
//        super();
//        this.applyUrl = applyUrl;
//    }
//
//    @Override
//    public NamedList<Object> request(SolrRequest request, String collection) throws SolrServerException, IOException {
//        LBSolrClient.Req req = new LBSolrClient.Req(request, Collections.singletonList(applyUrl));
//        // LBHttpSolrClient.Req req = new LBHttpSolrClient.Req(request, Collections.singletonList(applyUrl));
//        LBSolrClient.Rsp rsp = lbClient.request(req);
//        return rsp.getResponse();
//    }
//
//    @Override
//    public void close() throws IOException {
//    }
//}
