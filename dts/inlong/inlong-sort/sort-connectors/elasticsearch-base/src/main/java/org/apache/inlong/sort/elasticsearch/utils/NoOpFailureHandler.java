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

package org.apache.inlong.sort.elasticsearch.utils;

import org.apache.inlong.sort.elasticsearch.ActionRequestFailureHandler;
import org.apache.inlong.sort.elasticsearch.RequestIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ActionRequestFailureHandler that simply fails the sink on any failures.
 * Copies from {@link org.apache.flink.streaming.connectors.elasticsearch.util.NoOpFailureHandler}
 *
 * @param <ActionRequest>
 */
public class NoOpFailureHandler<ActionRequest> implements ActionRequestFailureHandler<ActionRequest> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(NoOpFailureHandler.class);

    @Override
    public void onFailure(ActionRequest actionRequest, Throwable failure,
            int restStatusCode, RequestIndexer<ActionRequest> indexer) throws Throwable {
        LOG.error("Failed Elasticsearch item request: {}", failure.getMessage(), failure);
        // simply fail the sink
        throw failure;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NoOpFailureHandler;
    }

    @Override
    public int hashCode() {
        return NoOpFailureHandler.class.hashCode();
    }
}
