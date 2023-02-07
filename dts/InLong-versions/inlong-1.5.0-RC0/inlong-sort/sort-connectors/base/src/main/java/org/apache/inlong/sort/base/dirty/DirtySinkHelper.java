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

package org.apache.inlong.sort.base.dirty;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Dirty sink helper, it helps dirty data sink for {@link DirtySink}
 * @param <T>
 */
public class DirtySinkHelper<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DirtySinkHelper.class);

    private DirtyOptions dirtyOptions;
    private final @Nullable DirtySink<T> dirtySink;

    public DirtySinkHelper(DirtyOptions dirtyOptions, @Nullable DirtySink<T> dirtySink) {
        this.dirtyOptions = Preconditions.checkNotNull(dirtyOptions, "dirtyOptions is null");
        this.dirtySink = dirtySink;
    }

    /**
     * Open for dirty sink
     *
     * @param configuration The configuration that is used for dirty sink
     */
    public void open(Configuration configuration) {
        if (dirtySink != null) {
            try {
                dirtySink.open(configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Dirty data sink
     * @param dirtyData The dirty data
     * @param dirtyType The dirty type {@link DirtyType}
     * @param e The cause of dirty data
     */
    public void invoke(T dirtyData, DirtyType dirtyType, Throwable e) {
        if (!dirtyOptions.ignoreDirty()) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        if (dirtySink != null) {
            DirtyData.Builder<T> builder = DirtyData.builder();
            try {
                builder.setData(dirtyData)
                        .setDirtyType(dirtyType)
                        .setLabels(dirtyOptions.getLabels())
                        .setLogTag(dirtyOptions.getLogTag())
                        .setDirtyMessage(e.getMessage())
                        .setIdentifier(dirtyOptions.getIdentifier());
                dirtySink.invoke(builder.build());
            } catch (Exception ex) {
                if (!dirtyOptions.ignoreSideOutputErrors()) {
                    throw new RuntimeException(ex);
                }
                LOGGER.warn("Dirty sink failed", ex);
            }
        }
    }

    public void invokeMultiple(T dirtyData, DirtyType dirtyType, Throwable e,
            String sinkMultipleFormat) {
        JsonDynamicSchemaFormat jsonDynamicSchemaFormat =
                (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
        if (!dirtyOptions.ignoreDirty()) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        if (dirtySink != null) {
            JsonNode rootNode;
            DirtyData.Builder<T> builder = DirtyData.builder();
            try {
                rootNode = jsonDynamicSchemaFormat.deserialize(((RowData) dirtyData).getBinary(0));
            } catch (Exception ex) {
                invoke(dirtyData, DirtyType.DESERIALIZE_ERROR, e);
                return;
            }
            try {
                builder.setData(dirtyData)
                        .setDirtyType(dirtyType)
                        .setLabels(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getLabels()))
                        .setLogTag(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getLogTag()))
                        .setDirtyMessage(e.getMessage())
                        .setIdentifier(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getIdentifier()));
                dirtySink.invoke(builder.build());
            } catch (Exception ex) {
                if (!dirtyOptions.ignoreSideOutputErrors()) {
                    throw new RuntimeException(ex);
                }
                LOGGER.warn("Dirty sink failed", ex);
            }
        }
    }

    public void setDirtyOptions(DirtyOptions dirtyOptions) {
        this.dirtyOptions = dirtyOptions;
    }

    public DirtyOptions getDirtyOptions() {
        return dirtyOptions;
    }

    @Nullable
    public DirtySink<T> getDirtySink() {
        return dirtySink;
    }
}
