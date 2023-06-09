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
package org.apache.drill.exec.store.table.function;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes table and parameters that can be used during table initialization and usage.
 * Common parameters are those that are common for all tables (ex: schema).
 * Specific parameters are those that are specific to the schema table belongs to.
 */
public final class TableSignature {

  private final String name;
  private final List<TableParamDef> commonParams;
  private final List<TableParamDef> specificParams;
  private final List<TableParamDef> params;

  public static TableSignature of(String name) {
    return new TableSignature(name, Collections.emptyList(), Collections.emptyList());
  }

  public static TableSignature of(String name, List<TableParamDef> commonParams) {
    return new TableSignature(name, commonParams, Collections.emptyList());
  }

  public static TableSignature of(String name, List<TableParamDef> commonParams, List<TableParamDef> specificParams) {
    return new TableSignature(name, commonParams, specificParams);
  }

  private TableSignature(String name, List<TableParamDef> commonParams, List<TableParamDef> specificParams) {
    this.name = name;
    this.commonParams = Collections.unmodifiableList(commonParams);
    this.specificParams = Collections.unmodifiableList(specificParams);
    this.params = Stream.of(specificParams, commonParams)
      .flatMap(Collection::stream)
      .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  public String getName() {
    return name;
  }

  public List<TableParamDef> getParams() {
    return params;
  }

  public List<TableParamDef> getCommonParams() {
    return commonParams;
  }

  public List<TableParamDef> getSpecificParams() { return specificParams; }

  @Override
  public int hashCode() {
    return Objects.hash(name, params);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableSignature that = (TableSignature) o;
    return name.equals(that.name) && params.equals(that.params);
  }

  @Override
  public String toString() {
    return "TableSignature{" +
      "name='" + name + '\'' +
      ", commonParams=" + commonParams +
      ", specificParams=" + specificParams + '}';
  }
}
