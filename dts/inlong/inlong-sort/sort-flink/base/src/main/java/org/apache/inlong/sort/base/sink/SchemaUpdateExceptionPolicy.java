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

package org.apache.inlong.sort.base.sink;

/**
 * Multiple sink scenes will meet different table data.
 * Maybe one table data have different schema, once it's schema mismatch with catalog schema, how to handle
 * this table data. For example schema mismatch:
 *
 * <pre>
 * data : {a : int, b : string, c : date}
 * catalog : {a : string, b : timestamp}
 * </pre>
 */
public enum SchemaUpdateExceptionPolicy {

    TRY_IT_BEST("Try it best to handle schema update, if can not handle it, just ignore it."),
    LOG_WITH_IGNORE("Ignore schema update and log it."),
    ALERT_WITH_IGNORE("Ignore schema update and alert it."),
    STOP_PARTIAL("Only stop abnormal sink table, other tables writes normally."),
    THROW_WITH_STOP("Throw exception to stop flink job when meet schema update.");

    private String description;

    SchemaUpdateExceptionPolicy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
