/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.inlong.audit.config.ElasticsearchConfig;
import org.apache.inlong.audit.db.entities.ESDataPo;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class ElasticsearchService implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchService.class);

    private static ScheduledExecutorService timerService = Executors.newScheduledThreadPool(1);
    private final Semaphore semaphore = new Semaphore(1);
    private List<ESDataPo> datalist = new ArrayList<>();
    @Autowired
    @Qualifier("restClient")
    private RestHighLevelClient client;

    @Autowired
    private ElasticsearchConfig esConfig;

    public void startTimerRoutine() {
        timerService.scheduleAtFixedRate((new Runnable() {
            @Override
            public void run() {
                try {
                    deleteTimeoutIndices();
                } catch (IOException e) {
                    LOG.error("deleteTimeoutIndices has err: ", e);
                }
            }
        }), 1, 1, TimeUnit.DAYS);

        timerService.scheduleWithFixedDelay((new Runnable() {
            @Override
            public void run() {
                try {
                    bulkInsert();
                } catch (IOException e) {
                    LOG.error("bulkInsert has err: ", e);
                }
            }
        }), esConfig.getBulkInterval(), esConfig.getBulkInterval(), TimeUnit.SECONDS);
    }

    public void insertData(ESDataPo data) {
        if (datalist.size() >= esConfig.getBulkThreshold()) {
            try {
                if (bulkInsert()) {
                    LOG.info("success bulk insert {} docs", esConfig.getBulkThreshold());
                } else {
                    LOG.error("failed to bulk insert");
                }
            } catch (IOException e) {
                LOG.error("bulkInsert has err: ", e);
            }
        }
        try {
            semaphore.acquire();
            datalist.add(data);
            semaphore.release();
        } catch (InterruptedException e) {
            LOG.error("datalist semaphore has err: ", e);
        }
    }

    protected boolean createIndex(String index) throws IOException {
        if (existsIndex(index)) {
            return true;
        }
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
        createIndexRequest.settings(Settings.builder().put("index.number_of_shards", esConfig.getShardsNum())
                .put("index.number_of_replicas", esConfig.getReplicaNum()));
        createIndexRequest.mapping("_doc", generateBuilder());
        CreateIndexResponse response = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        boolean res = response.isAcknowledged();
        if (res) {
            LOG.info("success creating index {}", index);
        } else {
            LOG.info("fail to create index {}", index);
        }
        return res;
    }

    protected boolean existsIndex(String index) throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(index);
        return client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    protected boolean bulkInsert() throws IOException {
        if (datalist.isEmpty()) {
            return true;
        }
        BulkRequest bulkRequest = new BulkRequest();
        try {
            semaphore.acquire();
            for (ESDataPo esDataPo : datalist) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                String index = formatter.format(esDataPo.getLogTs()) + "_" + esDataPo.getAuditId();
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .setDateFormat("yyyy-MM-dd HH:mm:ss");
                Gson gson = gsonBuilder.create();
                String esJson = gson.toJson(esDataPo);
                if (!createIndex(index)) {
                    LOG.error("fail to create index {}", index);
                    continue;
                }
                IndexRequest indexRequest;
                if (esConfig.isEnableCustomDocId()) {
                    indexRequest = new IndexRequest(index).type("_doc").id(esDataPo.getDocId())
                            .source(esJson, XContentType.JSON);
                } else {
                    indexRequest = new IndexRequest(index).type("_doc").source(esJson, XContentType.JSON);
                }
                bulkRequest.add(indexRequest);
            }
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            datalist.clear();
            semaphore.release();
            return bulkResponse.status().equals(RestStatus.OK);
        } catch (InterruptedException e) {
            LOG.error("datalist semaphore has err: ", e);
        }
        return false;
    }

    protected void deleteTimeoutIndices() throws IOException {
        List<String> auditIdList = esConfig.getAuditIdList();
        if (auditIdList.isEmpty()) {
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -esConfig.getIndexDeleteDay());
        Date deleteDay = calendar.getTime();
        String preIndex = formatter.format(deleteDay);
        for (String auditId : auditIdList) {
            String index = preIndex + "_" + auditId;
            deleteSingleIndex(index);
        }

    }

    protected boolean deleteSingleIndex(String index) throws IOException {
        if (!existsIndex(index)) {
            return true;
        }
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
        AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        boolean res = deleteIndexResponse.isAcknowledged();
        if (res) {
            LOG.info("success deleting index {}", index);
        } else {
            LOG.error("fail to delete index {}", index);
        }
        return res;
    }

    @Override
    public void close() {
        try {
            bulkInsert();
        } catch (IOException e) {
            LOG.error("bulkInsert has err: ", e);
        }
        timerService.shutdown();
    }

    protected XContentBuilder generateBuilder() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("properties");
            {
                builder.startObject("audit_id");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("inlong_group_id");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("inlong_stream_id");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("docker_id");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("thread_id");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("ip");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("log_ts");
                {
                    builder.field("type", "keyword");
                }
                builder.endObject();
            }
            {
                builder.startObject("sdk_ts");
                {
                    builder.field("type", "long");
                }
                builder.endObject();
            }
            {
                builder.startObject("count");
                {
                    builder.field("type", "long");
                }
                builder.endObject();
            }
            {
                builder.startObject("size");
                {
                    builder.field("type", "long");
                }
                builder.endObject();
            }
            {
                builder.startObject("delay");
                {
                    builder.field("type", "long");
                }
                builder.endObject();
            }
            {
                builder.startObject("packet_id");
                {
                    builder.field("type", "long");
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }
}



