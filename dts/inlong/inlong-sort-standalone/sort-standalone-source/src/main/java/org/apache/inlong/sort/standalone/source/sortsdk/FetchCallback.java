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

package org.apache.inlong.sort.standalone.source.sortsdk;

import com.google.common.base.Preconditions;

import org.apache.flume.channel.ChannelProcessor;
import org.apache.inlong.sdk.sort.api.ReadCallback;
import org.apache.inlong.sdk.sort.api.SortClient;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sort.standalone.channel.CacheMessageRecord;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Implementation of {@link ReadCallback}.
 *
 * The ACKer will be used to <b>ACK</b> upstream after that the downstream <b>ACKed</b> sort-standalone.
 * This process seems like <b>transaction</b> of the whole sort-standalone, and which
 * ensure <b>At Least One</b> semantics.
 */
public class FetchCallback implements ReadCallback {

    // Logger of {@link FetchCallback}.
    private static final Logger LOG = LoggerFactory.getLogger(FetchCallback.class);

    // SortId of fetch message.
    private final String sortTaskName;

    // ChannelProcessor that put message in specific channel.
    private final ChannelProcessor channelProcessor;

    // Context of source, used to report fetch results.
    private final SortSdkSourceContext context;

    // Temporary usage for ACK. The {@link SortClient} and Callback should not circular reference each other.
    private SortClient client;

    /**
     * Private constructor of {@link FetchCallback}.
     * <p> The construction of FetchCallback should be initiated by {@link FetchCallback.Factory}.</p>
     *
     * @param sortTaskName SortId of fetch message.
     * @param channelProcessor ChannelProcessor that message put in.
     * @param context The context to report fetch results.
     */
    private FetchCallback(
            final String sortTaskName,
            final ChannelProcessor channelProcessor,
            final SortSdkSourceContext context) {
        this.sortTaskName = sortTaskName;
        this.channelProcessor = channelProcessor;
        this.context = context;
    }

    /**
     * Set client for ack.
     *
     * @param client client for ack.
     */
    public void setClient(@NotNull SortClient client) {
        this.client = client;
    }

    /**
     * The callback function that SortSDK invoke when fetch messages.
     *
     * <p> In order to ACK the fetched msg, {@link FetchCallback} has to hold the {@link SortClient} which results in
     * <b>Cycle Reference</b>. The {@link SortClient} should deliver one object to ACK after invoke the callback method
     * {@link ReadCallback#onFinished(MessageRecord)}. </p>
     *
     * @param messageRecord message
     */
    @Override
    public void onFinished(final MessageRecord messageRecord) {
        try {
            Preconditions.checkState(messageRecord != null, "Fetched msg is null.");
            CacheMessageRecord cacheRecord = new CacheMessageRecord(messageRecord, client,
                    CommonPropertiesHolder.getAckPolicy());
            for (InLongMessage inLongMessage : messageRecord.getMsgs()) {
                final ProfileEvent profileEvent = new ProfileEvent(inLongMessage, cacheRecord);
                channelProcessor.processEvent(profileEvent);
                context.reportToMetric(profileEvent, sortTaskName, "-", SortSdkSourceContext.FetchResult.SUCCESS);
            }
        } catch (NullPointerException npe) {
            LOG.error("Got a null pointer exception for sort task " + sortTaskName, npe);
            context.reportToMetric(null, sortTaskName, "-", SortSdkSourceContext.FetchResult.FAILURE);
        } catch (Exception e) {
            LOG.error("Got exception of sort task " + sortTaskName, e);
            context.reportToMetric(null, sortTaskName, "-", SortSdkSourceContext.FetchResult.FAILURE);
        }
    }

    /**
     * The callback function that SortSDK invoke when fetch messages batch
     *
     * @param messageRecordList List
     */
    @Override
    public void onFinishedBatch(List<MessageRecord> messageRecordList) {
        messageRecordList.forEach(this::onFinished);
    }

    /**
     * Factory of {@link FetchCallback}
     */
    public static class Factory {

        /**
         * Create one {@link FetchCallback}.
         * <p> Validate sortId and channelProcessor before the construction of FetchCallback.</p>
         *
         * @param sortId The sortId of fetched message.
         * @param channelProcessor The channelProcessor that put message in specific channel.
         * @param context The context to report fetch results.
         * @return One FetchCallback.
         */
        public static FetchCallback create(
                @NotBlank(message = "sortId should not be null or empty.") final String sortId,
                @NotNull(message = "channelProcessor should not be null.") final ChannelProcessor channelProcessor,
                @NotNull(message = "context should not be null") final SortSdkSourceContext context) {
            return new FetchCallback(sortId, channelProcessor, context);
        }
    }
}
