/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode(of = {"name", "type"})
@NoArgsConstructor
@Getter
@Setter
@ToString(of = {"name", "type", "comment"})
public class ColumnInfo implements Serializable {
  private String name;
  private String type;
  private String comment;
  private Object defaultValue;
  private String properties;

  @Builder
  public ColumnInfo(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public ColumnInfo(String name,
                    String type,
                    String comment) {
    this(name, type);
    this.comment = comment;
  }

  public ColumnInfo(String name,
                    String type,
                    String comment,
                    Object defaultValue) {
    this(name, type, comment);
    this.defaultValue = defaultValue;
  }

  public String getComment() {
    return comment;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

}
