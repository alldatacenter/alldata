/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.connector.elasticsearch7;


import com.google.common.collect.Sets;
import com.qlangtech.org.apache.http.HttpHost;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.impl.ESTableAlias;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.AuthToken;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.aliyun.NoneToken;
import com.qlangtech.tis.plugin.aliyun.UsernamePassword;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.DataXElasticsearchWriter;
import com.qlangtech.tis.plugin.datax.elastic.ElasticEndpoint;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import com.qlangtech.tis.realtime.BasicTISSinkFactory;
import com.qlangtech.tis.realtime.TabSinkFunc;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.annotation.Public;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


/**
 * https://ci.apache.org/projects/flink/flink-docs-master/zh/docs/connectors/datastream/elasticsearch/
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-28 19:45
 **/
@Public
public class ElasticSearchSinkFactory extends BasicTISSinkFactory<RowData> {
    public static final String DISPLAY_NAME_FLINK_CDC_SINK = "Flink-ElasticSearch-Sink";
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchSinkFactory.class);
    private static final int DEFAULT_PARALLELISM = 1;// parallelism
    // bulk.flush.max.actions
    @FormField(ordinal = 0, type = FormFieldType.INT_NUMBER, validate = Validator.integer)
    public Integer bulkFlushMaxActions;

    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = Validator.integer)
    public Integer bulkFlushMaxSizeMb;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer bulkFlushIntervalMs;


    @Override
    public Map<TableAlias, TabSinkFunc<RowData>> createSinkFunction(IDataxProcessor dataxProcessor) {

        DataXElasticsearchWriter dataXWriter = (DataXElasticsearchWriter) dataxProcessor.getWriter(null);


        Objects.requireNonNull(dataXWriter, "dataXWriter can not be null");
        ElasticEndpoint token = dataXWriter.getToken();

        ESTableAlias esSchema = null;
        Optional<TableAlias> first = dataxProcessor.getTabAlias(null).findFirst();
        if (first.isPresent()) {
            TableAlias value = first.get();
            if (!(value instanceof ESTableAlias)) {
                throw new IllegalStateException("value must be type of 'ESTableAlias',but now is :" + value.getClass());
            }
            esSchema = (ESTableAlias) value;
        }
//        for (Map.Entry<String, TableAlias> e : dataxProcessor.getTabAlias().entrySet()) {
//            TableAlias value = e.getValue();
//            if (!(value instanceof ESTableAlias)) {
//                throw new IllegalStateException("value must be type of 'ESTableAlias',but now is :" + value.getClass());
//            }
//            esSchema = (ESTableAlias) value;
//            break;
//        }
        Objects.requireNonNull(esSchema, "esSchema can not be null");
        List<CMeta> cols = esSchema.getSourceCols();
        if (CollectionUtils.isEmpty(cols)) {
            throw new IllegalStateException("cols can not be null");
        }
        Optional<CMeta> firstPK = cols.stream().filter((c) -> c.isPk()).findFirst();
        if (!firstPK.isPresent()) {
            throw new IllegalStateException("has not set PK col");
        }

        /********************************************************
         * 初始化索引Schema
         *******************************************************/
        dataXWriter.initialIndex(esSchema);
        List<HttpHost> transportAddresses = new ArrayList<>();
        transportAddresses.add(HttpHost.create(token.getEndpoint()));

        ISelectedTab tab = null;
        IDataxReader reader = dataxProcessor.getReader(null);
        for (ISelectedTab selectedTab : reader.getSelectedTabs()) {
            tab = selectedTab;
            break;
        }
        Objects.requireNonNull(tab, "tab ca not be null");

        final List<FlinkCol> colsMeta = AbstractRowDataMapper.getAllTabColsMeta(tab.getCols());
        ElasticsearchSink.Builder<RowData> sinkBuilder
                = new ElasticsearchSink.Builder<>(transportAddresses
                , new DefaultElasticsearchSinkFunction(
                cols.stream().map((c) -> c.getName()).collect(Collectors.toSet())
                , colsMeta
                , firstPK.get().getName()
                , dataXWriter.getIndexName()));

        if (this.bulkFlushMaxActions != null) {
            sinkBuilder.setBulkFlushMaxActions(this.bulkFlushMaxActions);
        }

        if (this.bulkFlushMaxSizeMb != null) {
            sinkBuilder.setBulkFlushMaxSizeMb(bulkFlushMaxSizeMb);
        }

        if (this.bulkFlushIntervalMs != null) {
            sinkBuilder.setBulkFlushInterval(this.bulkFlushIntervalMs);
        }

        sinkBuilder.setFailureHandler(new DefaultActionRequestFailureHandler());

        token.accept(new AuthToken.Visitor<Void>() {
            @Override
            public Void visit(NoneToken noneToken) {
                return null;
            }

            @Override
            public Void visit(UsernamePassword accessKey) {
                sinkBuilder.setRestClientFactory(new TISElasticRestClientFactory(accessKey.userName, accessKey.password));
                return null;
            }
        });

//        if (StringUtils.isNotEmpty(token.getAccessKeyId())
//                || StringUtils.isNotEmpty(token.getAccessKeySecret())) {
//            // 如果用户设置了accessKey 或者accessSecret
//            sinkBuilder.setRestClientFactory(new TISElasticRestClientFactory(token.getAccessKeyId(), token.getAccessKeySecret()));
//        }


        return Collections.singletonMap(esSchema
                , new RowDataSinkFunc(esSchema, sinkBuilder.build()
                        , colsMeta
                        , true, DEFAULT_PARALLELISM));
    }

    private static class DefaultActionRequestFailureHandler implements ActionRequestFailureHandler, Serializable {
        @Override
        public void onFailure(ActionRequest actionRequest, Throwable throwable, int restStatusCode, RequestIndexer requestIndexer) throws Throwable {
            //throwable.printStackTrace();
            logger.error(throwable.getMessage(), throwable);
        }
    }

    @Override
    public ICompileAndPackage getCompileAndPackageManager() {
        return new CompileAndPackage(Sets.newHashSet(ElasticSearchSinkFactory.class));
    }

    private static class DefaultElasticsearchSinkFunction implements ElasticsearchSinkFunction<RowData>, Serializable {
        private final Set<String> cols;
        private final String pkName;
        private final String targetIndexName;
        private final List<FlinkCol> vgetters;

        public DefaultElasticsearchSinkFunction(Set<String> cols
                , List<FlinkCol> vgetters, String pkName, String targetIndexName) {
            this.vgetters = vgetters;
            this.cols = cols;
            this.pkName = pkName;
            this.targetIndexName = targetIndexName;
            if (StringUtils.isEmpty(targetIndexName)) {
                throw new IllegalArgumentException("param targetIndexName can not be null");
            }
        }

        private IndexRequest createIndexRequest(RowData element) {
            Map<String, Object> json = new HashMap<>();

            Object val = null;
            for (FlinkCol get : vgetters) {
                if (!cols.contains(get.name)) {
                    continue;
                }
                val = get.getRowDataVal(element);
                if (val instanceof DecimalData) {
                    val = ((DecimalData) val).toBigDecimal();
                } else if (val instanceof StringData) {
                    val = ((StringData) val).toString();
                }
                json.put(get.name, val);
            }
            Object pkVal = json.get(this.pkName);
            IndexRequest request = Requests.indexRequest()
                    .index(this.targetIndexName)
                    //.type("my-type")
                    .source(json);
            if (pkVal != null) {
                request.id(String.valueOf(pkVal));
            }
            return request;
        }

        @Override
        public void process(RowData element, RuntimeContext ctx, RequestIndexer indexer) {
            indexer.add(createIndexRequest(element));
        }
    }


    @TISExtension
    public static class DefaultSinkFunctionDescriptor extends BaseSinkFunctionDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME_FLINK_CDC_SINK;
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }

        @Override
        protected IEndTypeGetter.EndType getTargetType() {
            return IEndTypeGetter.EndType.ElasticSearch;
        }
    }
}
