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

package org.apache.inlong.sort.protocol.node.load;

import java.util.Collections;
import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

/**
 * test for elasticserach load node
 */
public class ElasticSearchNodeTest extends SerializeBaseTest<Node> {

    public ElasticsearchLoadNode getTestObject() {
        return new ElasticsearchLoadNode(
                "1", "test",
                Collections.singletonList(new FieldInfo("id", new StringFormatInfo())),
                Collections.singletonList(
                        new FieldRelation(new FieldInfo("id", new StringFormatInfo()),
                                new FieldInfo("id", new StringFormatInfo()))),
                null, null, 1, null,
                "index", "hosts", "username",
                "password", "documentType", "age", 7);
    }
}
