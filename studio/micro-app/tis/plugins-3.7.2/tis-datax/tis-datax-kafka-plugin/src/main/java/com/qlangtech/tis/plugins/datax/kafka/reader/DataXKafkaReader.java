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

package com.qlangtech.tis.plugins.datax.kafka.reader;

import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugins.datax.kafka.reader.messageformat.KafkaMessageFormat;
import com.qlangtech.tis.plugins.datax.kafka.reader.subscriptionmethod.KafkaSubscriptionMethod;
import com.qlangtech.tis.plugins.datax.kafka.writer.protocol.KafkaProtocol;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-16 22:25
 **/
public class DataXKafkaReader extends DataxReader {

    @FormField(ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String bootstrapServers;
    @FormField(ordinal = 1, validate = {})
    public KafkaMessageFormat MessageFormat;

    @FormField(ordinal = 2, validate = {Validator.require})
    public KafkaProtocol protocol;

    @FormField(ordinal = 3, validate = {Validator.require})
    public KafkaSubscriptionMethod subscription;

    @FormField(ordinal = 6, type = FormFieldType.ENUM, validate = {})
    public Boolean enableAutoCommit;


    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer maxRecordsProcess;

    @FormField(ordinal = 5, type = FormFieldType.ENUM, validate = {})
    public String clientDnsLookup;

    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer requestTimeoutMs;


    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, validate = {})
    public String clientId;

    @FormField(ordinal = 8, type = FormFieldType.INT_NUMBER, validate = {Validator.integer}, advance = true)
    public Integer pollingTime;

    @FormField(ordinal = 9, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer retryBackoffMs;

    @FormField(ordinal = 10, type = FormFieldType.INT_NUMBER, validate = {Validator.integer}, advance = true)
    public Integer repeatedCalls;


    @FormField(ordinal = 12, type = FormFieldType.INT_NUMBER, validate = {Validator.integer}, advance = true)
    public Integer autoCommitIntervalMs;

    @FormField(ordinal = 13, type = FormFieldType.ENUM, validate = {}, advance = true)
    public String autoOffsetReset;

    @FormField(ordinal = 14, type = FormFieldType.INPUTTEXT, validate = {}, advance = true)
    public String groupId;

    @FormField(ordinal = 15, type = FormFieldType.INPUTTEXT, validate = {}, advance = true)
    public String testTopic;

    @FormField(ordinal = 16, type = FormFieldType.INT_NUMBER, validate = {Validator.integer}, advance = true)
    public Integer maxPollRecords;

    @FormField(ordinal = 17, type = FormFieldType.INT_NUMBER, validate = {Validator.integer}, advance = true)
    public Integer receiveBufferBytes;

    @Override
    public <T extends ISelectedTab> List<T> getSelectedTabs() {
        return null;
    }

    @Override
    public IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter) {
        return null;
    }

    @Override
    public String getTemplate() {
        return null;
    }

   // @TISExtension
    public static class DefaultDescriptor extends BaseDataxReaderDescriptor {

        @Override
        public boolean isRdbms() {
            return false;
        }

        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public boolean isSupportBatch() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return IEndTypeGetter.EndType.Kafka;
        }

        @Override
        public String getDisplayName() {
            return getEndType().name();
        }
    }
}
