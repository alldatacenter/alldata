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

package org.apache.inlong.manager.client.api.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.client.api.transform.MultiDependencyTransform;
import org.apache.inlong.manager.client.api.transform.SingleDependencyTransform;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.sort.util.StreamParseUtils;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamTransform;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;

import java.util.List;

public class StreamTransformTransfer {

    /**
     * Create request of transformation.
     */
    public static TransformRequest createTransformRequest(StreamTransform streamTransform,
            InlongStreamInfo streamInfo) {
        TransformRequest transformRequest = new TransformRequest();
        Preconditions.expectNotBlank(streamTransform.getTransformName(), ErrorCodeEnum.INVALID_PARAMETER,
                "TransformName should not be null");
        transformRequest.setTransformName(streamTransform.getTransformName());
        transformRequest.setInlongGroupId(streamInfo.getInlongGroupId());
        transformRequest.setInlongStreamId(streamInfo.getInlongStreamId());
        Preconditions.expectNotNull(streamTransform.getTransformDefinition(), "TransformDefinition should not be null");
        TransformDefinition transformDefinition = streamTransform.getTransformDefinition();
        transformRequest.setTransformType(transformDefinition.getTransformType().getType());
        transformRequest.setVersion(InlongConstants.INITIAL_VERSION);
        transformRequest.setTransformDefinition(JsonUtils.toJsonString(transformDefinition));
        if (CollectionUtils.isNotEmpty(streamTransform.getPreNodes())) {
            transformRequest.setPreNodeNames(Joiner.on(",").join(streamTransform.getPreNodes()));
        }
        if (CollectionUtils.isNotEmpty(streamTransform.getPostNodes())) {
            transformRequest.setPostNodeNames(Joiner.on(",").join(streamTransform.getPostNodes()));
        }
        if (CollectionUtils.isNotEmpty(streamTransform.getFieldList())) {
            transformRequest.setFieldList(streamTransform.getFieldList());
        }
        return transformRequest;
    }

    /**
     * Parse stream of transformation.
     */
    public static StreamTransform parseStreamTransform(TransformResponse transformResponse) {
        TransformType transformType = TransformType.forType(transformResponse.getTransformType());
        String transformDefinitionStr = transformResponse.getTransformDefinition();
        TransformDefinition transformDefinition = StreamParseUtils.parseTransformDefinition(
                transformDefinitionStr, transformType);
        String transformName = transformResponse.getTransformName();
        String preNodeNames = transformResponse.getPreNodeNames();
        List<String> preNodes = Splitter.on(",").splitToList(preNodeNames);
        StreamTransform streamTransform;
        if (preNodes.size() > 1) {
            streamTransform = new MultiDependencyTransform(transformName, transformDefinition,
                    preNodes.toArray(new String[]{}));
        } else {
            streamTransform = new SingleDependencyTransform(transformName, transformDefinition,
                    preNodes.get(0));
        }
        String postNodeNames = transformResponse.getPostNodeNames();
        if (StringUtils.isNotEmpty(postNodeNames)) {
            List<String> postNodes = Splitter.on(",").splitToList(postNodeNames);
            streamTransform.setPostNodes(Sets.newHashSet(postNodes));
        }
        if (CollectionUtils.isNotEmpty(transformResponse.getFieldList())) {
            streamTransform.setFieldList(transformResponse.getFieldList());
        }
        return streamTransform;
    }

}
