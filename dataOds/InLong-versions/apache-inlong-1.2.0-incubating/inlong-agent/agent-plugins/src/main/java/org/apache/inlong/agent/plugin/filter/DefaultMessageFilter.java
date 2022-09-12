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

package org.apache.inlong.agent.plugin.filter;

import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.utils.ByteUtil;

/**
 * filter message to get stream id
 * use the first word to set stream id string
 */
public class DefaultMessageFilter implements MessageFilter {

    public static final int STREAM_INDEX = 0;
    public static final int FIELDS_LIMIT = 2;

    @Override
    public String filterStreamId(Message message, byte[] fieldSplitter) {
        byte[] body = message.getBody();
        return new String(ByteUtil.split(body, fieldSplitter, FIELDS_LIMIT)[STREAM_INDEX]);
    }
}
