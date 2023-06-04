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

package org.apache.inlong.sort.elasticsearch7;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.elasticsearch.table.ElasticsearchSinkFunctionBase;
import org.apache.inlong.sort.elasticsearch.table.IndexGenerator;
import org.apache.inlong.sort.elasticsearch.table.RequestFactory;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.common.xcontent.XContentType;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * ElasticsearchSinkFunction for elasticsearch7
 */
public class RowElasticsearchSinkFunction extends ElasticsearchSinkFunctionBase<DocWriteRequest<?>, XContentType> {

    private static final long serialVersionUID = 1L;

    public RowElasticsearchSinkFunction(
            IndexGenerator indexGenerator,
            @Nullable String docType, // this is deprecated in es 7+
            SerializationSchema<RowData> serializationSchema,
            XContentType contentType,
            RequestFactory<DocWriteRequest<?>, XContentType> requestFactory,
            Function<RowData, String> createKey,
            @Nullable Function<RowData, String> createRouting,
            DirtySinkHelper<Object> dirtySinkHelper) {
        super(indexGenerator, docType, serializationSchema, contentType,
                requestFactory, createKey, createRouting, dirtySinkHelper);
    }

    @Override
    public void handleRouting(DocWriteRequest<?> request, String routing) {
        request.routing(routing);
    }
}
