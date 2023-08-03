/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io.writer;

import com.netease.arctic.data.ChangeAction;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.types.Types;

import java.util.Map;

public class RecordWithAction implements Record {

  private Record record;

  private ChangeAction action;

  public RecordWithAction(Record record, ChangeAction action) {
    this.record = record;
    this.action = action;
  }

  public Record getRecord() {
    return record;
  }

  public ChangeAction getAction() {
    return action;
  }

  @Override
  public Types.StructType struct() {
    return record.struct();
  }

  @Override
  public Object getField(String name) {
    return record.getField(name);
  }

  @Override
  public void setField(String name, Object value) {
    record.setField(name, value);
  }

  @Override
  public Object get(int pos) {
    return record.get(pos);
  }

  @Override
  public <T> T get(int pos, Class<T> javaClass) {
    return record.get(pos, javaClass);
  }

  @Override
  public Record copy() {
    return new RecordWithAction(record.copy(), action);
  }

  @Override
  public Record copy(Map<String, Object> overwriteValues) {
    return new RecordWithAction(record.copy(overwriteValues), action);
  }

  @Override
  public Record copy(String field, Object value) {
    return new RecordWithAction(record.copy(field, value), action);
  }

  @Override
  public Record copy(String field1, Object value1, String field2, Object value2) {
    return new RecordWithAction(record.copy(field1, value1, field2, value2), action);
  }

  @Override
  public Record copy(String field1, Object value1, String field2, Object value2, String field3, Object value3) {
    return new RecordWithAction(record.copy(field1, value1, field2, value2, field3, value3), action);
  }

  @Override
  public int size() {
    return record.size();
  }

  @Override
  public <T> void set(int pos, T value) {
    record.set(pos, value);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RecordWithAction{");
    sb.append("record=").append(record);
    sb.append(", action=").append(action);
    sb.append('}');
    return sb.toString();
  }
}
