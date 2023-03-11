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

package com.netease.arctic.ams.server.model;

import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;

/**
 * AMS server columninfo.
 */
public class AMSColumnInfo {
  String field;
  String type;
  String comment;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public AMSColumnInfo() {
  }

  public AMSColumnInfo(String field, String type, String comment) {
    this.field = field;
    this.type = type;
    this.comment = comment;
  }

  /**
   * @return
   */
  public static AMSColumnInfo buildFromNestedField(Types.NestedField field) {
    if (field == null) {
      return null;
    }
    return new Builder()
            .field(field.name())
            .type(field.type().toString())
            .comment(field.doc())
            .build();
  }

  /**
   * Construct ColumnInfo based on schema and primarykeyfield.
   * @param schema schema
   * @param pkf pkf
   * @return
   */
  public static AMSColumnInfo buildFromPartitionSpec(Schema schema, PrimaryKeySpec.PrimaryKeyField pkf) {
    return buildFromNestedField(schema.findField(pkf.fieldName()));

  }


  public static class Builder {
    String field;
    String type;
    String comment;


    public Builder field(String field) {
      this.field = field;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder comment(String comment) {
      this.comment = comment;
      return this;
    }

    public AMSColumnInfo build() {
      return new AMSColumnInfo(field, type, comment);
    }
  }
}
