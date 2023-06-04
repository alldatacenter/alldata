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
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dirty sink helper, it helps dirty data sink for {@link DirtySink}
 * @param <T>
 */
public class DirtySinkHelper<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DirtySinkHelper.class);
    static final Pattern REGEX_PATTERN = Pattern.compile("\\$\\{\\s*([\\w.-]+)\\s*}", Pattern.CASE_INSENSITIVE);
    private static final String DIRTY_TYPE_KEY = "DIRTY_TYPE";
    private static final String DIRTY_MESSAGE_KEY = "DIRTY_MESSAGE";
    private static final String SYSTEM_TIME_KEY = "SYSTEM_TIME";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
     *
     * @param dirtyData The dirty data
     * @param dirtyType The dirty type {@link DirtyType}
     * @param e The cause of dirty data
     */
    public void invoke(T dirtyData, DirtyType dirtyType, Throwable e) {
        invoke(dirtyData, dirtyType, dirtyOptions.getLabels(), dirtyOptions.getLogTag(), dirtyOptions.getIdentifier(),
                e);
    }

    /**
     * Dirty data sink
     *
     * @param dirtyData The dirty data
     * @param dirtyType The dirty type {@link DirtyType}
     * @param label The dirty label
     * @param logTag The dirty logTag
     * @param identifier The dirty identifier
     * @param e The cause of dirty data
     */
    public void invoke(T dirtyData, DirtyType dirtyType, String label, String logTag, String identifier, Throwable e) {
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
                        .setLabels(label)
                        .setLogTag(logTag)
                        .setDirtyMessage(e.getMessage())
                        .setIdentifier(identifier);
                dirtySink.invoke(builder.build());
            } catch (Exception ex) {
                if (!dirtyOptions.ignoreSideOutputErrors()) {
                    throw new RuntimeException(ex);
                }
                LOGGER.warn("Dirty sink failed", ex);
            }
        }
    }

    /**
     * replace ${SYSTEM_TIME} with real time
     *
     * @param pattern
     * @return
     */
    public static String regexReplace(String pattern, DirtyType dirtyType, String dirtyMessage) {
        if (pattern == null) {
            return null;
        }

        Map<String, String> paramMap = new HashMap<>(6);
        paramMap.put(SYSTEM_TIME_KEY, DATE_TIME_FORMAT.format(LocalDateTime.now()));
        paramMap.put(DIRTY_TYPE_KEY, dirtyType.format());
        paramMap.put(DIRTY_MESSAGE_KEY, dirtyMessage);

        Matcher matcher = REGEX_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String keyText = matcher.group(1);
            String replacement = paramMap.get(keyText);
            if (replacement == null) {
                continue;
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * replace ${database} ${table} etc. Used in cases where jsonDynamicFormat.parse() is not usable.
     */
    public static String regexReplace(String pattern, DirtyType dirtyType, String dirtyMessage, String database,
            String table, String schema) {
        if (pattern == null) {
            return null;
        }

        Map<String, String> paramMap = new HashMap<>(6);
        paramMap.put(SYSTEM_TIME_KEY, DATE_TIME_FORMAT.format(LocalDateTime.now()));
        paramMap.put(DIRTY_TYPE_KEY, dirtyType.format());
        paramMap.put(DIRTY_MESSAGE_KEY, dirtyMessage);
        paramMap.put("source.database", database);
        paramMap.put("database", database);
        paramMap.put("source.table", table);
        paramMap.put("table", table);
        if (schema != null) {
            paramMap.put("source.schema", schema);
            paramMap.put("schema", schema);
        }

        Matcher matcher = REGEX_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String keyText = matcher.group(1);
            String replacement = paramMap.get(keyText);
            if (replacement == null) {
                continue;
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
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
