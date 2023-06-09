/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.scanner.persistence;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * a field set in an annotation
 * (to simplify we have a list of string representations of the values)
 */
public final class AttributeDescriptor {
  private final String name;
  private final List<String> values;

  @JsonCreator public AttributeDescriptor(
      @JsonProperty("name") String name,
      @JsonProperty("values") List<String> values) {
    this.name = name;
    this.values = values;
  }

  /**
   * @return field name
   */
  public String getName() {
    return name;
  }

  public List<String> getValues() {
    return values;
  }

  @Override
  public String toString() {
    return "Attribute[" + name + "=" + values + "]";
  }
}