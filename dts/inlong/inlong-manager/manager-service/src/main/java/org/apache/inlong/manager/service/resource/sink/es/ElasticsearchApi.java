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

package org.apache.inlong.manager.service.resource.sink.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchFieldInfo;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * elasticsearch template service
 */
@Component
public class ElasticsearchApi {

    private static final String FIELD_KEY = "properties";

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchApi.class);

    @Autowired
    private ElasticsearchConfig esConfig;

    /**
     * Search
     *
     * @param searchRequest The search request of Elasticsearch
     * @return Search reponse of Elasticsearch
     * @throws IOException The io exception may throws
     */
    public SearchResponse search(SearchRequest searchRequest) throws IOException {
        return search(searchRequest, RequestOptions.DEFAULT);
    }

    /**
     * Search
     *
     * @param searchRequest The search request of Elasticsearch
     * @param options The options of Elasticsearch
     * @return Search reponse of Elasticsearch
     * @throws IOException The io exception may throws
     */
    public SearchResponse search(SearchRequest searchRequest, RequestOptions options) throws IOException {
        LOG.info("get es search request of {}", searchRequest.source().toString());
        return getEsClient().search(searchRequest, options);
    }

    /**
     * Check index exists
     *
     * @param indexName The index name of Elasticsearch
     * @return true if exists else false
     * @throws IOException The exception may throws
     */
    public boolean indexExists(String indexName) throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(indexName);
        return getEsClient().indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    /**
     * Create index
     *
     * @param indexName The index name of Elasticsearch
     * @throws IOException The exception may throws
     */
    public void createIndex(String indexName) throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

        CreateIndexResponse createIndexResponse = getEsClient().indices()
                .create(createIndexRequest, RequestOptions.DEFAULT);
        LOG.info("create es index:{} result: {}", indexName, createIndexResponse.isAcknowledged());
    }

    /**
     * Get mapping info
     *
     * @param fieldsInfo The fields info of Elasticsearch
     * @return String list of fields translation
     * @throws IOException The exception may throws
     */
    private List<String> getMappingInfo(List<ElasticsearchFieldInfo> fieldsInfo) {
        List<String> fieldList = new ArrayList<>();
        for (ElasticsearchFieldInfo field : fieldsInfo) {
            StringBuilder fieldStr = new StringBuilder().append("        \"").append(field.getFieldName())
                    .append("\" : {\n          \"type\" : \"")
                    .append(field.getFieldType()).append("\"");
            if (field.getFieldType().equals("text")) {
                if (StringUtils.isNotEmpty(field.getAnalyzer())) {
                    fieldStr.append(",\n          \"analyzer\" : \"")
                            .append(field.getAnalyzer()).append("\"");
                }
                if (StringUtils.isNotEmpty(field.getSearchAnalyzer())) {
                    fieldStr.append(",\n          \"search_analyzer\" : \"")
                            .append(field.getSearchAnalyzer()).append("\"");
                }
            } else if (field.getFieldType().equals("date")) {
                if (StringUtils.isNotEmpty(field.getFieldFormat())) {
                    fieldStr.append(",\n          \"format\" : \"")
                            .append(field.getFieldFormat()).append("\"");
                }
            } else if (field.getFieldType().equals("scaled_float")) {
                if (StringUtils.isNotEmpty(field.getScalingFactor())) {
                    fieldStr.append(",\n          \"scaling_factor\" : \"")
                            .append(field.getScalingFactor()).append("\"");
                }
            }
            fieldStr.append("\n        }");
            fieldList.add(fieldStr.toString());
        }
        return fieldList;
    }

    /**
     * Create index and mapping
     *
     * @param indexName Index name of creating
     * @param fieldInfos Field infos
     * @throws IOException The exception may throws
     */
    public void createIndexAndMapping(String indexName,
            List<ElasticsearchFieldInfo> fieldInfos) throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        List<String> fieldList = getMappingInfo(fieldInfos);
        StringBuilder mapping = new StringBuilder().append("{\n      \"properties\" : {\n")
                .append(StringUtils.join(fieldList, ",\n")).append("\n      }\n}");
        createIndexRequest.mapping(mapping.toString(), XContentType.JSON);

        CreateIndexResponse createIndexResponse = getEsClient().indices()
                .create(createIndexRequest, RequestOptions.DEFAULT);
        LOG.info("create {}:{}", indexName, createIndexResponse.isAcknowledged());
    }

    /**
     * Get fields
     *
     * @param indexName The index name of Elasticsearch
     * @return a {@link Map} collection that contains {@link String}
     *     as key and {@link MappingMetaData} as value.
     * @throws IOException The exception may throws
     */
    public Map<String, MappingMetaData> getFields(String indexName) throws IOException {
        GetMappingsRequest request = new GetMappingsRequest().indices(indexName);
        return getEsClient().indices().getMapping(request, RequestOptions.DEFAULT).mappings();
    }

    /**
     * Add fieldss
     *
     * @param indexName The index name of Elasticsearch
     * @param fieldInfos The fields info of Elasticsearch
     * @throws IOException The exception may throws
     */
    public void addFields(String indexName, List<ElasticsearchFieldInfo> fieldInfos) throws IOException {
        if (CollectionUtils.isNotEmpty(fieldInfos)) {
            List<String> fieldList = getMappingInfo(fieldInfos);
            StringBuilder mapping = new StringBuilder().append("{\n      \"properties\" : {\n")
                    .append(StringUtils.join(fieldList, ",\n")).append("\n      }\n}");
            System.out.println(mapping.toString());
            PutMappingRequest indexRequest = new PutMappingRequest(indexName)
                    .source(mapping.toString(), XContentType.JSON);
            AcknowledgedResponse acknowledgedResponse = getEsClient().indices()
                    .putMapping(indexRequest, RequestOptions.DEFAULT);
            LOG.info("put mapping: {} result: {}", mapping.toString(), acknowledgedResponse.toString());
        }
    }

    /**
     * Add not exist fields
     *
     * @param indexName The index name of elasticsearch
     * @param fieldInfos The fields info of elasticsearch
     * @throws IOException The exception may throws
     */
    public void addNotExistFields(String indexName,
            List<ElasticsearchFieldInfo> fieldInfos) throws IOException {
        List<ElasticsearchFieldInfo> notExistFieldInfos = new ArrayList<>(fieldInfos);
        Map<String, MappingMetaData> mapping = getFields(indexName);
        Map<String, Object> filedMap = (Map<String, Object>) mapping.get(indexName).getSourceAsMap().get(FIELD_KEY);
        for (String key : filedMap.keySet()) {
            for (ElasticsearchFieldInfo field : notExistFieldInfos) {
                if (field.getFieldName().equals(key)) {
                    notExistFieldInfos.remove(field);
                    break;
                }
            }
        }
        addFields(indexName, notExistFieldInfos);
    }

    /**
     * Get Elasticsearch client
     *
     * @return RestHighLevelClient
     */
    public RestHighLevelClient getEsClient() {
        return esConfig.highLevelClient();
    }

    /**
     * Get Elasticsearch client
     *
     * @param config Elasticsearch's configuration
     */
    public void setEsConfig(ElasticsearchConfig config) {
        this.esConfig = config;
    }
}
