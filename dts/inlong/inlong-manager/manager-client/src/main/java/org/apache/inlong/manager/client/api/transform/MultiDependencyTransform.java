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

package org.apache.inlong.manager.client.api.transform;

import io.swagger.annotations.ApiModel;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.stream.StreamTransform;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;

import java.util.List;

/**
 * StreamTransform with multiple pre stream nodes, such as join.
 */
@NoArgsConstructor
@ApiModel("StreamTransform with multiple pre stream nodes, such as join")
public class MultiDependencyTransform extends StreamTransform {

    /**
     * Constructor of MultiDependencyTransform
     *
     * @param transformName transform name
     * @param transformDefinition definition info
     * @param preNodes name of pre streamNodes, if pre streamNode is streamSource, then preNode is sourceName
     *         if pre streamNode is streamTransform, preNode is transformName
     */
    public MultiDependencyTransform(String transformName, TransformDefinition transformDefinition, String... preNodes) {
        Preconditions.expectNotNull(transformDefinition, "transform definition cannot be null");
        this.transformDefinition = transformDefinition;
        Preconditions.expectNotBlank(transformName, ErrorCodeEnum.INVALID_PARAMETER, "transform name cannot be empty");
        this.transformName = transformName;
        Preconditions.expectNoNullElements(preNodes, "pre nodes cannot be null");
        for (String preNode : preNodes) {
            this.addPre(preNode);
        }
    }

    /**
     * Constructor of MultiDependencyTransform
     *
     * @param transformName transform name
     * @param transformDefinition definition info
     * @param preNodes name of pre streamNodes, if pre streamNode is streamSource, then preNode is sourceName
     *         if pre streamNode is streamTransform, preNode is transformName
     * @param postNodes postNodes name of post streamNode, if post streamNode is streamSource, then postNode is
     *         sourceName, if post streamNode is streamTransform, postNode is transformName
     */
    public MultiDependencyTransform(String transformName, TransformDefinition transformDefinition,
            List<String> preNodes, List<String> postNodes) {
        this(transformName, transformDefinition, preNodes.toArray(new String[0]));
        if (postNodes != null) {
            for (String postNode : postNodes) {
                this.addPost(postNode);
            }
        }
    }
}
