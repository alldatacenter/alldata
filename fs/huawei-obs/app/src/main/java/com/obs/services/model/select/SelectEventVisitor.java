/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.select;

import java.nio.ByteBuffer;

/**
 * Interface for a visitor over the events of the input stream
 * 
 * Those events not overriden do nothing.
 */
public class SelectEventVisitor {
    /**
     * Informs of the next chunk to be processed
     * 
     * The content of the chunk is only known by the wire format input serialization format.
     * The purpose of this method is for monitoring of the chunk sizes or for backup of the
     * input data.
     * 
     * @param payload
     *      Buffer with the next chunk of the input stream
     */
    public void visitRecordsEvent(ByteBuffer payload) {
    }

    /**
     * Informs that the server is still processing the Select request.
     * 
     * Used internally to maintain the connection alive. Optionally, the the receiver can abort
     * after some user-defined timeout
     */
    public void visitContinuationEvent() {
    }

    /**
     * Periodic progress information
     * 
     * @param bytesScanned
     *      Current number of input bytes scanned by the server
     * 
     * @param bytesProcessed
     *      Current number of input bytes processed by the server, less than or equal than the
     *      scanned bytes.
     * 
     * @param bytesReturned
     *      Current number of bytes returned as chunks by the server
     */
    public void visitProgressEvent(long bytesScanned, long bytesProcessed, long bytesReturned) {
    }

    /**
     * Final statistics when the request has finished successfully
     * 
     * @param bytesScanned
     *      Total number of input bytes scanned by the server
     * 
     * @param bytesProcessed
     *      Total number of input bytes processed by the server, less than or equal than the
     *      scanned bytes.
     * 
     * @param bytesReturned
     *      Total number of bytes returned as chunks by the server
     */
    public void visitStatsEvent(long bytesScanned, long bytesProcessed, long bytesReturned) {
    }

    /**
     * Informs than the request has finished successfully.
     */
    public void visitEndEvent() {
    }
}
