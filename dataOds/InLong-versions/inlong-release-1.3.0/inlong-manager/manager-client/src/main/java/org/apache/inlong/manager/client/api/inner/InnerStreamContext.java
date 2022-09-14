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

package org.apache.inlong.manager.client.api.inner;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.transform.TransformRequest;

import java.util.List;
import java.util.Map;

/**
 * Inner stream context.
 */
@Data
@NoArgsConstructor
public class InnerStreamContext {

    private InlongStreamInfo streamInfo;

    private Map<String, SourceRequest> sourceRequests = Maps.newHashMap();

    private Map<String, SinkRequest> sinkRequests = Maps.newHashMap();

    private Map<String, TransformRequest> transformRequests = Maps.newHashMap();

    public InnerStreamContext(InlongStreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public void updateStreamFields(List<StreamField> fieldList) {
        streamInfo.setFieldList(fieldList);
    }

    public void setSourceRequest(SourceRequest sourceRequest) {
        this.sourceRequests.put(sourceRequest.getSourceName(), sourceRequest);
    }

    public void setSinkRequest(SinkRequest sinkRequest) {
        this.sinkRequests.put(sinkRequest.getSinkName(), sinkRequest);
    }

    public void setTransformRequest(TransformRequest transformRequest) {
        this.transformRequests.put(transformRequest.getTransformName(), transformRequest);
    }
}
