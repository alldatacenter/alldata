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

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.MongoDBReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * MongoDBSource : mongo source, split mongo source job into multi readers
 */
public class MongoDBSource extends AbstractSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBSource.class);

    @Override
    public List<Reader> split(JobProfile conf) {
        super.init(conf);
        List<Reader> readerList = Collections.singletonList(new MongoDBReader());
        sourceMetric.sourceSuccessCount.incrementAndGet();
        return readerList;
    }
}
