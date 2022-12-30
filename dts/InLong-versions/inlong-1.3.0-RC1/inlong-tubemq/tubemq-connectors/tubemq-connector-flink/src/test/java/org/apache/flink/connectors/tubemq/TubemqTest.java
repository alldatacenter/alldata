/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.connectors.tubemq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.flink.table.descriptors.Descriptor;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.DescriptorTestBase;
import org.apache.flink.table.descriptors.DescriptorValidator;
import org.junit.Test;

/**
 * Unit tests for {@link Tubemq}.
 */
public class TubemqTest extends DescriptorTestBase {

    @Override
    protected List<Descriptor> descriptors() {
        final Descriptor descriptor1 =
            new Tubemq()
                .topic("test-topic-1")
                .master("localhost:9001")
                .group("test-group-1");

        final Descriptor descriptor2 =
            new Tubemq()
                .topic("test-topic-2")
                .master("localhost:9001")
                .group("test-group-2")
                .property("bootstrap.from.max", "true");

        final Descriptor descriptor3 =
            new Tubemq()
                .topic("test-topic-3")
                .master("localhost:9001")
                .group("test-group-3")
                .tids("test-tid-1,test-tid-2");

        return Arrays.asList(descriptor1, descriptor2, descriptor3);
    }

    @Override
    protected List<Map<String, String>> properties() {
        final Map<String, String> props1 = new HashMap<>();
        props1.put("connector.property-version", "1");
        props1.put("connector.type", "tubemq");
        props1.put("connector.master", "localhost:9001");
        props1.put("connector.topic", "test-topic-1");
        props1.put("connector.group", "test-group-1");

        final Map<String, String> props2 = new HashMap<>();
        props2.put("connector.property-version", "1");
        props2.put("connector.type", "tubemq");
        props2.put("connector.master", "localhost:9001");
        props2.put("connector.topic", "test-topic-2");
        props2.put("connector.group", "test-group-2");
        props2.put("connector.properties.bootstrap.from.max", "true");

        final Map<String, String> props3 = new HashMap<>();
        props3.put("connector.property-version", "1");
        props3.put("connector.type", "tubemq");
        props3.put("connector.master", "localhost:9001");
        props3.put("connector.topic", "test-topic-3");
        props3.put("connector.tids", "test-tid-1,test-tid-2");
        props3.put("connector.group", "test-group-3");

        return Arrays.asList(props1, props2, props3);
    }

    @Override
    protected DescriptorValidator validator() {
        return new TubemqValidator();
    }

    @Test
    public void testTubePropertiesValidator() {
        DescriptorValidator validator = this.validator();
        DescriptorProperties descriptorProperties = new DescriptorProperties();
        descriptorProperties.putProperties(properties().get(0));
        validator.validate(descriptorProperties);
    }
}
