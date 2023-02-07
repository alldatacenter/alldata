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

package org.apache.inlong.sdk.sort.impl;

import org.apache.inlong.sdk.sort.api.Cleanable;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.api.SortClient;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.api.TopicManager;
import org.apache.inlong.sdk.sort.exception.NotExistException;
import org.apache.inlong.sdk.sort.api.InlongTopicManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * New version of sort client.
 */
public class SortClientImpl extends SortClient {

    private final String logPrefix = "[" + SortClientImpl.class.getSimpleName() + "] ";
    private final Logger logger = LoggerFactory.getLogger(SortClientImpl.class);

    private final SortClientConfig sortClientConfig;

    private final ClientContext context;

    private final TopicManager inLongTopicManager;

    /**
     * SortClient Constructor
     *
     * @param sortClientConfig SortClientConfig
     */
    public SortClientImpl(SortClientConfig sortClientConfig) {
        try {
            this.sortClientConfig = sortClientConfig;
            this.context = new ClientContextImpl(this.sortClientConfig);

            this.inLongTopicManager = InlongTopicManagerFactory
                    .createInLongTopicManager(sortClientConfig.getTopicType(),
                            context, new QueryConsumeConfigImpl(context));
        } catch (Exception e) {
            this.close();
            throw e;
        }
    }

    /**
     * SortClient Constructor with user defined QueryConsumeConfig,MetricReporter and ManagerReportHandler
     *
     * @param sortClientConfig SortClientConfig
     * @param queryConsumeConfig QueryConsumeConfig
     */
    public SortClientImpl(SortClientConfig sortClientConfig, QueryConsumeConfig queryConsumeConfig) {
        try {
            this.sortClientConfig = sortClientConfig;
            this.context = new ClientContextImpl(this.sortClientConfig);
            queryConsumeConfig.configure(context);
            this.inLongTopicManager = InlongTopicManagerFactory
                    .createInLongTopicManager(sortClientConfig.getTopicType(),
                            context, queryConsumeConfig);
        } catch (Exception e) {
            e.printStackTrace();
            this.close();
            throw e;
        }
    }

    /**
     * init SortClient
     *
     * @return true/false
     * @throws Throwable
     */
    @Override
    public boolean init() throws Throwable {
        logger.info(logPrefix + "init|" + sortClientConfig);
        return true;
    }

    /**
     * ack offset to msgKey
     *
     * @param msgKey String
     * @param msgOffset String
     * @throws Exception
     */
    @Override
    public void ack(String msgKey, String msgOffset)
            throws Exception {
        logger.debug("ack:{} offset:{}", msgKey, msgOffset);
        TopicFetcher topicFetcher = getFetcher(msgKey);
        topicFetcher.ack(msgOffset);
    }

    /**
     * close SortClient
     *
     * @return true/false
     */
    @Override
    public boolean close() {
        boolean cleanInLongTopicManager = doClose(inLongTopicManager);
        boolean cleanContext = doClose(context);

        logger.info(logPrefix

                + "|cleanInLongTopicManager=" + cleanInLongTopicManager
                + "|cleanContext=" + cleanContext);
        return (cleanInLongTopicManager && cleanContext);
    }

    @Override
    public SortClientConfig getConfig() {
        return this.sortClientConfig;
    }

    private TopicFetcher getFetcher(String msgKey) throws NotExistException {
        TopicFetcher topicFetcher = inLongTopicManager.getFetcher(msgKey);
        if (topicFetcher == null) {
            throw new NotExistException(msgKey + " not exist.");
        }
        return topicFetcher;
    }

    private boolean doClose(Cleanable c) {
        try {
            if (c != null) {
                return c.clean();
            }
            return true;
        } catch (Throwable th) {
            logger.error(logPrefix + "clean error.", th);
            return false;
        }
    }
}
