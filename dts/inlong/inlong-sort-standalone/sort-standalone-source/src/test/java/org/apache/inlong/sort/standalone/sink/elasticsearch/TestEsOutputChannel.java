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

import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.sink.SinkContext;
import org.apache.inlong.sort.standalone.utils.BufferQueue;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;

/**
 * 
 * TestEsOutputChannel
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({EsSinkFactory.class, RestHighLevelClient.class, ClusterClient.class, IndicesClient.class,
        MetricRegister.class})
public class TestEsOutputChannel {

    private RestHighLevelClient esClient;

    /**
     * mock
     *
     * @throws Exception
     */
    public static EsOutputChannel mock() throws Exception {
        EsOutputChannel output = PowerMockito.mock(EsOutputChannel.class);
        PowerMockito.when(EsSinkFactory.class, "createEsOutputChannel", any()).thenReturn(output);
        PowerMockito.doNothing().when(output, "close");
        //
        BulkProcessor bulkProcessor = PowerMockito.mock(BulkProcessor.class);
        PowerMockito.when(bulkProcessor, "add", any()).thenReturn(bulkProcessor);
        PowerMockito.when(output, "getBulkProcessor").thenReturn(bulkProcessor);
        PowerMockito.when(output, "initEsclient").thenReturn(true);
        return output;
    }

    /**
     * before
     * 
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Throwable {
        PowerMockito.mockStatic(EsSinkFactory.class);
        this.esClient = PowerMockito.mock(RestHighLevelClient.class);
        PowerMockito.mockStatic(RestHighLevelClient.class);
        PowerMockito.when(EsSinkFactory.createRestHighLevelClient(any())).thenReturn(esClient);
        PowerMockito.doNothing().when(esClient).close();
        PowerMockito.doNothing().when(esClient).bulkAsync(any(BulkRequest.class), any(RequestOptions.class),
                any(ActionListener.class));
        PowerMockito.when(esClient.ping(any(RequestOptions.class))).thenReturn(false);

        // ClusterClient
        ClusterClient clusterClient = PowerMockito.mock(ClusterClient.class);
        PowerMockito.when(esClient, "cluster").thenReturn(clusterClient);
        ClusterUpdateSettingsResponse clusterResponse = PowerMockito.mock(ClusterUpdateSettingsResponse.class);
        PowerMockito.when(clusterClient.putSettings(any(ClusterUpdateSettingsRequest.class),
                any(RequestOptions.class))).thenReturn(clusterResponse);
        // IndicesClient
        IndicesClient indicesClient = PowerMockito.mock(IndicesClient.class);
        PowerMockito.when(esClient, "indices").thenReturn(indicesClient);
        AcknowledgedResponse indicesResponse = PowerMockito.mock(AcknowledgedResponse.class);
        PowerMockito.when(indicesClient.putSettings(any(UpdateSettingsRequest.class),
                any(RequestOptions.class))).thenReturn(indicesResponse);
    }

    /**
     * test
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        BufferQueue<EsIndexRequest> dispatchQueue = SinkContext.createBufferQueue();
        EsSinkContext context = TestEsSinkContext.mock(dispatchQueue);
        EsOutputChannel output = new EsOutputChannel(context);
        ProfileEvent event = TestEsSinkContext.mockProfileEvent();
        EsIndexRequest indexRequest = context.createIndexRequestHandler().parse(context, event);
        dispatchQueue.offer(indexRequest);
        output.init();
        output.send();
        output.close();
    }

}
