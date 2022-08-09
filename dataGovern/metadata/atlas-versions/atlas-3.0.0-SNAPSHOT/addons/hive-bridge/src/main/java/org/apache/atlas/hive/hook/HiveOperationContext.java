/**
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
package org.apache.atlas.hive.hook;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.events.ListenerEvent;
import org.apache.hadoop.hive.ql.plan.HiveOperation;

public class HiveOperationContext {
    HiveOperation operation;
    ListenerEvent event;
    FieldSchema   columnOld;
    FieldSchema   columnNew;

    public HiveOperationContext(ListenerEvent event) {
        this(null, event);
    }

    public HiveOperationContext(HiveOperation operation, ListenerEvent event) {
        setOperation(operation);
        setEvent(event);
        setColumnOld(null);
        setColumnNew(null);
    }

    public ListenerEvent getEvent() {
        return event;
    }

    public void setEvent(ListenerEvent event) {
        this.event = event;
    }

    public HiveOperation getOperation() {
        return operation;
    }

    public void setOperation(HiveOperation operation) {
        this.operation = operation;
    }

    public FieldSchema getColumnOld() {
        return columnOld;
    }

    public void setColumnOld(FieldSchema columnOld) {
        this.columnOld = columnOld;
    }

    public FieldSchema getColumnNew() {
        return columnNew;
    }

    public void setColumnNew(FieldSchema columnNew) {
        this.columnNew = columnNew;
    }
}
