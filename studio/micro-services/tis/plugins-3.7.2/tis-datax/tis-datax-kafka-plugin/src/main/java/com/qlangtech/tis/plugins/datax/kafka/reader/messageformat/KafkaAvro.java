/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.datax.kafka.reader.messageformat;

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;

public class KafkaAvro extends KafkaMessageFormat {

    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = {})
    public String deserializationStrategy;

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {})
    public String schemaRegistryUsername;


    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {})
    public String schemaRegistryPassword;

    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {})
    public String schemaRegistryUrl;

    @Override
    public String getDeserializationType() {
        return "AVRO";
    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<KafkaMessageFormat> {
        @Override
        public String getDisplayName() {
            return "AVRO";
        }
    }
}
