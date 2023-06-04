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

package org.apache.inlong.manager.client.api.inner.client;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.StreamSinkApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.sink.ParseFieldRequest;
import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;

import java.util.List;

import static org.apache.inlong.manager.common.consts.InlongConstants.STATEMENT_TYPE_JSON;
import static org.apache.inlong.manager.common.consts.InlongConstants.STATEMENT_TYPE_SQL;

/**
 * Client for {@link StreamSinkApi}.
 */
public class StreamSinkClient {

    private final StreamSinkApi streamSinkApi;

    public StreamSinkClient(ClientConfiguration configuration) {
        streamSinkApi = ClientUtils.createRetrofit(configuration).create(StreamSinkApi.class);
    }

    public Integer createSink(SinkRequest sinkRequest) {
        Response<Integer> response = ClientUtils.executeHttpCall(streamSinkApi.save(sinkRequest));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete stream sink info by ID.
     */
    public boolean deleteSink(int id) {
        Preconditions.expectTrue(id > 0, "sinkId is illegal");
        Response<Boolean> response = ClientUtils.executeHttpCall(streamSinkApi.deleteById(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete stream sink by key
     */
    public boolean deleteSinkByKey(String groupId, String streamId, String name) {
        Response<Boolean> response = ClientUtils.executeHttpCall(streamSinkApi.deleteByKey(groupId, streamId, name));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * List stream sinks by the given groupId and streamId.
     */
    public List<StreamSink> listSinks(String groupId, String streamId) {
        return listSinks(groupId, streamId, null);
    }

    /**
     * List stream sinks by the specified sink type.
     */
    public List<StreamSink> listSinks(String groupId, String streamId, String sinkType) {
        SinkPageRequest pageRequest = new SinkPageRequest();
        pageRequest.setInlongGroupId(groupId);
        pageRequest.setInlongStreamId(streamId);
        pageRequest.setSinkType(sinkType);
        Response<PageResult<StreamSink>> response = ClientUtils.executeHttpCall(streamSinkApi.list(pageRequest));
        ClientUtils.assertRespSuccess(response);
        return response.getData().getList();
    }

    /**
     * Paging query stream sink info based on conditions.
     */
    public List<StreamSink> listSinks(SinkPageRequest pageRequest) {
        Response<PageResult<StreamSink>> response = ClientUtils.executeHttpCall(streamSinkApi.list(pageRequest));
        ClientUtils.assertRespSuccess(response);
        return response.getData().getList();
    }

    /**
     * Update the stream sink info.
     */
    public Pair<Boolean, String> updateSink(SinkRequest sinkRequest) {
        Response<Boolean> response = ClientUtils.executeHttpCall(streamSinkApi.updateById(sinkRequest));
        ClientUtils.assertRespSuccess(response);

        if (response.getData() != null) {
            return Pair.of(response.getData(), response.getErrMsg());
        } else {
            return Pair.of(false, response.getErrMsg());
        }
    }

    /**
     * Update the stream sink by key
     */
    public Pair<UpdateResult, String> updateSinkByKey(SinkRequest sinkRequest) {
        Response<UpdateResult> response = ClientUtils.executeHttpCall(streamSinkApi.updateByKey(sinkRequest));
        ClientUtils.assertRespSuccess(response);

        if (response.getData() != null) {
            return Pair.of(response.getData(), response.getErrMsg());
        } else {
            return Pair.of(new UpdateResult(), response.getErrMsg());
        }
    }

    /**
     * Get detail information of data sink.
     */
    public StreamSink getSinkInfo(Integer sinkId) {
        Response<StreamSink> response = ClientUtils.executeHttpCall(streamSinkApi.get(sinkId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Converts a json string to a sinkFields
     *
     * @param parseFieldRequest the request for the field information
     * @return list of sink field
     */
    public List<SinkField> parseFields(ParseFieldRequest parseFieldRequest) {
        Response<List<SinkField>> response = ClientUtils.executeHttpCall(streamSinkApi.parseFields(parseFieldRequest));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Converts a json string to a streamFields
     *     @param method the method for the field information: json or sql
     * @param statement the statement for the field information
     * @return list of stream field
     */
    public List<SinkField> parseFields(String method, String statement) {
        Preconditions.expectTrue(STATEMENT_TYPE_JSON.equals(method) || STATEMENT_TYPE_SQL.equals(method),
                "Unsupported parse field method: '" + method + "'");
        Preconditions.expectNotBlank(statement, "The statement must not empty");
        ParseFieldRequest request = ParseFieldRequest.builder().method(method).statement(statement).build();
        return parseFields(request);
    }
}
