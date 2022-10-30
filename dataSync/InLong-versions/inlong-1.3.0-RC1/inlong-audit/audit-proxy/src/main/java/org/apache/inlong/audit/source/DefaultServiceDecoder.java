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

package org.apache.inlong.audit.source;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.inlong.audit.consts.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.inlong.audit.protocol.AuditApi.BaseCommand;

public class DefaultServiceDecoder implements ServiceDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultServiceDecoder.class);

    @Override
    public BaseCommand extractData(ByteBuf cb, Channel channel) throws Exception {
        /*[cmd size] | [cmd]*/
        if (null == cb) {
            LOG.error("cb == null");
            return null;
        }
        int totalLen = cb.readableBytes();
        if (ConfigConstants.MSG_MAX_LENGTH_BYTES < totalLen) {
            throw new Exception(new Throwable("err msg, ConfigConstants.MSG_MAX_LENGTH_BYTES "
                    + "< totalLen, and  totalLen=" + totalLen));
        }
        cb.markReaderIndex();
        BaseCommand cmd = null;
        BaseCommand.Builder cmdBuilder = BaseCommand.newBuilder();
        int cmdSize = cb.readInt();
        if (cmdSize <= totalLen) {
            byte[] bodyBytes = new byte[cmdSize];
            cb.readBytes(bodyBytes);
            LOG.debug("msg totalDataLen = {}, cmdSize = {}", totalLen, cmdSize);
            cmd = cmdBuilder.mergeFrom(bodyBytes).build();
        } else {
            // reset index.
            cb.resetReaderIndex();
        }
        return cmd;
    }
}
