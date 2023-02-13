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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import java.util.List;

import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.slf4j.Logger;

/**
 * EsCallbackListener
 *
 */
public class EsCallbackListener implements BulkProcessor.Listener {

    public static final Logger LOG = InlongLoggerFactory.getLogger(EsCallbackListener.class);

    private EsSinkContext context;

    /**
     * Constructor
     * 
     * @param context
     */
    public EsCallbackListener(EsSinkContext context) {
        this.context = context;
    }

    /**
     * beforeBulk
     * 
     * @param executionId
     * @param request
     */
    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
        LOG.debug("beforeBulk,executionId:{},request:{}", executionId, request);
    }

    /**
     * afterBulk
     * 
     * @param executionId
     * @param request
     * @param response
     */
    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        LOG.debug("afterBulk,executionId,executionId:{},request:{},response:{}", executionId, request, response);
        BulkItemResponse[] itemResponses = response.getItems();
        List<DocWriteRequest<?>> requests = request.requests();
        if (itemResponses.length != requests.size()) {
            LOG.error("BulkItemResponse size is not equal to IndexRequest size:requestSize:{},responseSize:{}",
                    requests.size(), itemResponses.length);
        }
        for (int i = 0; i < itemResponses.length; i++) {
            // parameter
            EsIndexRequest requestItem = (EsIndexRequest) requests.get(i);
            BulkItemResponse responseItem = itemResponses[i];
            ProfileEvent event = requestItem.getEvent();
            long sendTime = requestItem.getSendTime();

            // is fail
            if (responseItem.isFailed()) {
                context.addSendResultMetric(event, context.getTaskName(), false, sendTime);
                context.backDispatchQueue(requestItem);
            } else {
                context.addSendResultMetric(event, context.getTaskName(), true, sendTime);
                context.releaseDispatchQueue(requestItem);
                event.ack();
            }
        }
    }

    /**
     * afterBulk
     * 
     * @param executionId
     * @param request
     * @param failure
     */
    @Override
    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        LOG.error("afterBulk,executionId:" + executionId + ",failure:" + failure, failure);
        String errorMsg = String.format("EsSenderError,whole bulk,errorMsg:%s,count:%d", failure.getMessage(),
                request.numberOfActions());
        LOG.error(errorMsg, failure);

        // monitor
        List<DocWriteRequest<?>> requests = request.requests();
        for (int i = 0; i < requests.size(); i++) {
            EsIndexRequest requestItem = (EsIndexRequest) requests.get(i);
            ProfileEvent event = requestItem.getEvent();
            long sendTime = requestItem.getSendTime();
            context.addSendResultMetric(event, context.getTaskName(), false, sendTime);
            context.backDispatchQueue(requestItem);
        }
    }

}
