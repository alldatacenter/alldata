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
import org.apache.inlong.manager.common.pojo.stream.StreamTransform;
import org.apache.inlong.manager.common.pojo.transform.TransformDefinition;
import org.apache.inlong.manager.common.util.AssertUtils;

/**
 * StreamTransform with one pre stream node, such as filter, splitter, etc.
 */
@ApiModel("StreamTransform with one pre stream node, such as filter, splitter, etc")
public class SingleDependencyTransform extends StreamTransform {

    /**
     * Constructor of SingleDependencyTransform
     *
     * @param transformName transform name
     * @param transformDefinition definition info
     * @param preNode name of pre streamNode, if pre streamNode is streamSource, then preNode is sourceName
     *         if pre streamNode is streamTransform, preNode is transformName
     */
    public SingleDependencyTransform(String transformName, TransformDefinition transformDefinition, String preNode) {
        AssertUtils.notNull(transformDefinition, "TransformDefinition should not be null");
        this.transformDefinition = transformDefinition;
        AssertUtils.notNull(transformName, "TransformName should not be empty");
        this.transformName = transformName;
        AssertUtils.notNull(preNode, "Pre streamNode should not be null");
        this.addPre(preNode);
    }

    /**
     * Constructor of SingleDependencyTransform
     *
     * @param transformName transform name
     * @param transformDefinition definition info
     * @param preNode name of pre streamNode, if pre streamNode is streamSource, then preNode is sourceName
     *         if pre streamNode is streamTransform, preNode is transformName
     * @param postNodes name of post streamNode, if post streamNode is streamSource, then postNode is sourceName
     *         if post streamNode is streamTransform, postNode is transformName
     */
    public SingleDependencyTransform(String transformName, TransformDefinition transformDefinition, String preNode,
            String... postNodes) {
        this(transformName, transformDefinition, preNode);
        if (postNodes != null) {
            for (String postNode : postNodes) {
                this.addPost(postNode);
            }
        }
    }

}
