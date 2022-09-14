/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.inlongmsg;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.inlong.common.msg.InLongMsg;

/**
 * The body deserialized from {@link InLongMsg}.
 */
public class InLongMsgBody implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The body of the record.
     */
    private final byte[] data;

    /**
     * The interface of the record.
     */
    private final String tid;

    /**
     * The fields extracted from the body.
     */
    private final List<String> fields;

    /**
     * The entries extracted from the body.
     */
    private final Map<String, String> entries;

    public InLongMsgBody(
            byte[] data,
            String tid,
            List<String> fields,
            Map<String, String> entries
    ) {
        this.data = data;
        this.tid = tid;
        this.fields = fields;
        this.entries = entries;
    }

    public byte[] getData() {
        return data;
    }

    public String getTid() {
        return tid;
    }

    public List<String> getFields() {
        return fields;
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InLongMsgBody inLongMsgBody = (InLongMsgBody) o;
        return Arrays.equals(data, inLongMsgBody.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "InLongMsgBody{" + "data=" + Arrays.toString(data) + ", tid='" + tid + '\''
               + ", fields=" + fields + ", entries=" + entries + '}';
    }
}
