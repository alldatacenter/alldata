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

import java.io.IOException;
import java.io.InputStream;

/**
 * Result of a Select request
 * 
 * The evaluation is lazy and it only will start when the first row
 * of the iterator is required.
 */
public class SelectObjectResult {
    private InputStream input;

    /**
     * Prepares a resultset based on an input stream of binary data
     * 
     * @param input
     *      Binary input stream
     */
    public SelectObjectResult(InputStream input) {
        this.input = input;
    }

    /**
     * Closes the inut stream
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        input.close();
    }

    /**
     * Returns a row iterator over the input data
     * 
     * @return The row iterator
     */
    public SelectInputStream getInputStream() {
        return new SelectInputStream(input, null);
    }

    /**
     * Returns a row iterator over the input data with a visitor of the stream events
     * 
     * @param listener
     *          The event visitor
     * 
     * @return The row iterator
     */
    public SelectInputStream getInputStream(SelectEventVisitor listener) {
        return new SelectInputStream(input, listener);
    }
}
