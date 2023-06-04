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

package org.apache.inlong.sort.elasticsearch6.utils;

import org.apache.flink.annotation.Internal;
import org.apache.flink.util.ExceptionUtils;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.elasticsearch.ActionRequestFailureHandler;
import org.apache.inlong.sort.elasticsearch.RequestIndexer;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Dirty data sink when failure happens
 */
@Internal
public class DirtySinkFailureHandler implements ActionRequestFailureHandler<DocWriteRequest<?>> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DirtySinkFailureHandler.class);

    private transient DirtySinkHelper<Object> dirtySinkHelper;
    private transient SinkMetricData sinkMetricData;

    @Override
    public void onFailure(DocWriteRequest<?> request, Throwable failure, int restStatusCode,
            RequestIndexer<DocWriteRequest<?>> indexer) throws Throwable {
        // We think that only when ElasticsearchException occurs, it is considered dirty data
        if (ExceptionUtils.findThrowable(failure, ElasticsearchException.class).isPresent()) {
            String dirtyData;
            if (request instanceof UpdateRequest) {
                dirtyData = ((UpdateRequest) request).doc().source().utf8ToString();
            } else if (request instanceof IndexRequest) {
                dirtyData = ((IndexRequest) request).source().utf8ToString();
            } else {
                dirtyData = request.id();
            }
            LOGGER.error("Action request handle error", failure);
            LOGGER.debug("The dirty data: {}", dirtyData);
            dirtySinkHelper.invoke(dirtyData, DirtyType.DOCUMENT_PARSE_ERROR, failure);
            if (sinkMetricData != null) {
                sinkMetricData.invokeDirty(1, dirtyData.getBytes(StandardCharsets.UTF_8).length);
            }
        } else {
            throw failure;
        }
    }

    public void setDirtySinkHelper(DirtySinkHelper<Object> dirtySinkHelper) {
        this.dirtySinkHelper = dirtySinkHelper;
    }

    public void setSinkMetricData(SinkMetricData sinkMetricData) {
        this.sinkMetricData = sinkMetricData;
    }
}
