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
package com.qlangtech.async.message.client.util;

import org.apache.rocketmq.common.message.MessageExt;

/*
 * @Date 2017/3/21
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MsgUtils {

    public static final String MESSAGE_TRACE = "message_trace";

    public static final <T> boolean isAllNull(T... objs) {
        for (T t : objs) {
            if (t != null)
                return false;
        }
        return true;
    }

    public static String getOriginMsgId(MessageExt messageExt) {
        if (messageExt.getReconsumeTimes() > 0) {
            String msgId = messageExt.getProperty("ORIGIN_MESSAGE_ID");
            // }
            return msgId;
        }
        return messageExt.getMsgId();
    }
}
