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

package org.apache.inlong.sort.standalone.sink.cls;

import com.tencentcloudapi.cls.producer.Callback;
import com.tencentcloudapi.cls.producer.Result;
import com.tencentcloudapi.cls.producer.common.Attempt;
import com.tencentcloudapi.cls.producer.common.ErrorCodes;
import org.apache.flume.Transaction;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * Implementation of CLS {@link Callback}.
 */
public class ClsCallback implements Callback {

    private static final Logger LOG = InlongLoggerFactory.getLogger(ClsCallback.class);

    private final Transaction tx;
    private final ClsSinkContext context;
    private final ProfileEvent event;
    private final String topicId;

    /**
     * Constructor.
     *
     * @param tx Transaction
     * @param context Context.
     * @param event Related event.
     */
    public ClsCallback(Transaction tx, ClsSinkContext context, ProfileEvent event) {
        this.tx = tx;
        this.context = context;
        this.event = event;
        this.topicId = event.getHeaders().get(ClsSinkContext.KEY_TOPIC_ID);
    }

    @Override
    public void onCompletion(Result result) {
        if (!result.isSuccessful()) {
            onFailed(result);
            return;
        }
        onSuccess();
    }

    /**
     * If send success.
     */
    private void onSuccess() {
        context.addSendResultMetric(event, topicId, true, System.currentTimeMillis());
        event.ack();
        tx.commit();
        tx.close();
    }

    /**
     * If send failed.
     *
     * @param result Send result.
     */
    private void onFailed(Result result) {
        if (isRetryable(result.getReservedAttempts())) {
            tx.rollback();
            tx.close();
        } else {
            event.ack();
            tx.commit();
            tx.close();
            LOG.error(result.toString());
            context.addSendResultMetric(event, topicId, false, System.currentTimeMillis());
        }
    }

    private boolean isRetryable(List<Attempt> reservedAttempts) {
        if (reservedAttempts.isEmpty()) {
            LOG.error("attempts is empty, just discards");
            return false;
        }
        for (Attempt attempt : reservedAttempts) {
            String errorCode = attempt.getErrorCode();
            if (!errorCode.equals(ErrorCodes.SpeedQuotaExceed)) {
                return false;
            }
        }
        return true;
    }
}
