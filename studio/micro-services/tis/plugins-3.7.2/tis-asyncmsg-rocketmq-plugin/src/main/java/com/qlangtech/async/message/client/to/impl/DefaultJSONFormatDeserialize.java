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
package com.qlangtech.async.message.client.to.impl;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.async.message.client.consumer.impl.AbstractAsyncMsgDeserialize;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.realtime.transfer.DTO;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@Public
public class DefaultJSONFormatDeserialize extends AbstractAsyncMsgDeserialize {
    // CharsetDecoder 有线程安全问题
    private static final ThreadLocal<CharsetDecoder> utf8CharsetDecoder = new ThreadLocal<CharsetDecoder>() {
        @Override
        protected CharsetDecoder initialValue() {
            return TisUTF8.get().newDecoder();
        }
    };

    @Override
    public final DTO deserialize(byte[] content) throws IOException {
        return com.alibaba.fastjson.JSONObject.parseObject(content, 0, content.length, utf8CharsetDecoder.get(), DTO.class);
    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<AbstractAsyncMsgDeserialize> {

        @Override
        public String getDisplayName() {
            return "defaultJson";
        }
    }
}
