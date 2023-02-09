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

import com.qcloud.cos.transfer.TransferProgress;

/**
 * TransferProgressUpdatingListener for multiple-file transfer. In addition
 * to updating the TransferProgress, it also sends out ByteTrasnferred
 * events to a ProgressListenerChain.
 */
public final class MultipleFileTransferProgressUpdatingListener extends
        TransferProgressUpdatingListener implements DeliveryMode {
    private final ProgressListenerChain progressListenerChain;

    public MultipleFileTransferProgressUpdatingListener(
            TransferProgress transferProgress,
            ProgressListenerChain progressListenerChain) {
        super(transferProgress);
        this.progressListenerChain = progressListenerChain;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        super.progressChanged(progressEvent);
        progressListenerChain.progressChanged(progressEvent);
    }

    @Override
    public boolean isSyncCallSafe() {
        return progressListenerChain == null
            || progressListenerChain.isSyncCallSafe();
    }
}