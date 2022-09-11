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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.sort.formats.base.TableFormatDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base for all inlongmsg format deserializers.
 */
public abstract class AbstractInLongMsgFormatDeserializer implements TableFormatDeserializer {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInLongMsgFormatDeserializer.class);

    /**
     * True if ignore errors in the deserialization.
     */
    @Nonnull
    protected final Boolean ignoreErrors;

    public AbstractInLongMsgFormatDeserializer(@Nonnull Boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    /**
     * Parses the head of the inlongmsg record.
     */
    protected abstract InLongMsgHead parseHead(String attr) throws Exception;

    /**
     * Parses the body of the inlongmsg record.
     */
    protected abstract InLongMsgBody parseBody(byte[] bytes) throws Exception;

    /**
     * Converts the inlongmsg record into a row.
     */
    protected abstract Row convertRow(InLongMsgHead head, InLongMsgBody body) throws Exception;

    @Override
    public void flatMap(
            byte[] bytes,
            Collector<Row> collector
    ) throws Exception {
        InLongMsg inLongMsg = InLongMsg.parseFrom(bytes);

        for (String attr : inLongMsg.getAttrs()) {
            Iterator<byte[]> iterator = inLongMsg.getIterator(attr);
            if (iterator == null) {
                continue;
            }

            InLongMsgHead head;
            try {
                head = parseHead(attr);
            } catch (Exception e) {
                if (ignoreErrors) {
                    LOG.warn("Cannot properly parse the head {}.", attr, e);
                    continue;
                } else {
                    throw e;
                }
            }

            while (iterator.hasNext()) {

                byte[] bodyBytes = iterator.next();
                if (bodyBytes == null || bodyBytes.length == 0) {
                    continue;
                }

                InLongMsgBody body;
                try {
                    body = parseBody(bodyBytes);
                } catch (Exception e) {
                    if (ignoreErrors) {
                        LOG.warn("Cannot properly parse the body {}.",
                                Arrays.toString(bodyBytes), e);
                        continue;
                    } else {
                        throw e;
                    }
                }

                Row row;
                try {
                    row = convertRow(head, body);
                } catch (Exception e) {
                    if (ignoreErrors) {
                        LOG.warn("Cannot properly convert the inlongmsg ({}, {}) " + "to row.", head, body, e);
                        continue;
                    } else {
                        throw e;
                    }
                }

                if (row != null) {
                    collector.collect(row);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractInLongMsgFormatDeserializer that = (AbstractInLongMsgFormatDeserializer) o;
        return ignoreErrors.equals(that.ignoreErrors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ignoreErrors);
    }
}
