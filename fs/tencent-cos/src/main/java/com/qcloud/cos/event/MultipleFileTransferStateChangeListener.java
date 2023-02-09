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


package com.qcloud.cos.event;

import java.util.concurrent.CountDownLatch;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.transfer.MultipleFileTransfer;
import com.qcloud.cos.transfer.Transfer;
import com.qcloud.cos.transfer.Transfer.TransferState;

public final class MultipleFileTransferStateChangeListener implements TransferStateChangeListener {
    private final CountDownLatch latch;
    private final MultipleFileTransfer<?> multipleFileTransfer;

    public MultipleFileTransferStateChangeListener(CountDownLatch latch,
            MultipleFileTransfer<?> multipleFileTransfer) {
        this.latch = latch;
        this.multipleFileTransfer = multipleFileTransfer;
    }

    @Override
    public void transferStateChanged(Transfer upload, TransferState state) {
        // There's a race here: we can't start monitoring the state of
        // individual transfers until we have added all the transfers to the
        // list, or we may incorrectly report completion.
        try {
            latch.await();
        } catch ( InterruptedException e ) {
            throw new CosClientException("Couldn't wait for all downloads to be queued");
        }

        synchronized (multipleFileTransfer) {
            if ( multipleFileTransfer.getState() == state || multipleFileTransfer.isDone() )
                return;

            /*
             * If we're not already in a terminal state, allow a transition
             * to a non-waiting state. Mark completed if this download is
             * completed and the monitor says all of the rest are as well.
             */
            if ( state == TransferState.InProgress ) {
                multipleFileTransfer.setState(state);
            } else if ( multipleFileTransfer.getMonitor().isDone() ) {
                multipleFileTransfer.collateFinalState();
            } else {
                multipleFileTransfer.setState(TransferState.InProgress);
            }
        }
    }
}