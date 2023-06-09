/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.dorisdb.connector.datax.plugin.writer.doriswriter.manager;

import java.util.List;

public class DorisFlushTuple {

    private final String label;
    public final WriterBuffer buffer;

    public DorisFlushTuple(String label, WriterBuffer buffer) {
        this.label = label;
        this.buffer = buffer;
    }

    public String getLabel() {
        return this.label;
    }


    public List<byte[]> getRows() {
        return buffer.buffer;
    }

    public long getBytes() {
        return buffer.size;
    }
}
