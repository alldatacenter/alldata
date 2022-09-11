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

package org.apache.inlong.agent.plugin.utils;

import io.debezium.relational.history.DatabaseHistory;
import org.apache.inlong.agent.plugin.message.SchemaRecord;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Util to safely visit schema history {@link DatabaseHistory}
 */
public class DatabaseHistoryUtil {

    private static final Map<String, Collection<SchemaRecord>> HISTORY = new HashMap();
    private static final Map<String, Boolean> HISTORY_CLEANUP_STATUS = new HashMap();

    private DatabaseHistoryUtil() {
    }

    public static void registerHistory(String engineName, Collection<SchemaRecord> engineHistory) {
        synchronized (HISTORY) {
            HISTORY.put(engineName, engineHistory);
            HISTORY_CLEANUP_STATUS.put(engineName, false);
        }
    }

    public static void removeHistory(String engineName) {
        synchronized (HISTORY) {
            HISTORY_CLEANUP_STATUS.put(engineName, true);
            HISTORY.remove(engineName);
        }
    }

    public static Collection<SchemaRecord> retrieveHistory(String engineName) {
        synchronized (HISTORY) {
            if (Boolean.TRUE.equals(HISTORY_CLEANUP_STATUS.get(engineName))) {
                throw new IllegalStateException(String.format(
                        "Retrieve schema history failed, the schema records for engine %s has been removed, "
                                + "this might because the debezium engine has been shutdown due to other errors.",
                        engineName));
            } else {
                return (Collection) HISTORY.getOrDefault(engineName, Collections.emptyList());
            }
        }
    }
}
