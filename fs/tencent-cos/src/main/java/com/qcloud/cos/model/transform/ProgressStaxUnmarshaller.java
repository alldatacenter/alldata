/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */
package com.qcloud.cos.model.transform;

import com.qcloud.cos.model.Progress;
import com.qcloud.cos.model.Stats;
import com.qcloud.cos.model.transform.StaxUnmarshallerContext;
import com.qcloud.cos.internal.Unmarshaller;

/**
 * Unmarshaller for {@link Progress}.
 */
class ProgressStaxUnmarshaller
    implements Unmarshaller<Progress, StaxUnmarshallerContext> {

    private static final ProgressStaxUnmarshaller instance = new ProgressStaxUnmarshaller();

    public static ProgressStaxUnmarshaller getInstance() {
        return instance;
    }

    private ProgressStaxUnmarshaller() {
    }

    @Override
    public Progress unmarshall(StaxUnmarshallerContext context) throws Exception {
        // This is currently the same as the statistics. Inline the statistics if they diverge.
        Stats queryStats = StatsStaxUnmarshaller.getInstance().unmarshall(context);
        return new Progress().withBytesProcessed(queryStats.getBytesProcessed())
                             .withBytesReturned(queryStats.getBytesReturned())
                             .withBytesScanned(queryStats.getBytesScanned());
    }

}
