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
package org.apache.drill.metastore.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class that specifies metadata type and metadata information
 * which will be used for obtaining specific metadata from metastore.
 *
 * For example:
 * <li>For table metadata:
 * {@code MetadataInfo[MetadataType.TABLE, MetadataInfo.GENERAL_INFO_KEY, MetadataInfo.GENERAL_INFO_KEY]}</li>
 * <li>For default segment metadata:
 * {@code MetadataInfo[MetadataType.SEGMENT, MetadataInfo.DEFAULT_SEGMENT_KEY, MetadataInfo.DEFAULT_SEGMENT_KEY]}</li>
 * <li>For top-level segment metadata:
 * {@code MetadataInfo[MetadataType.SEGMENT, "1994", "1994"]}</li>
 * <li>For nested segment metadata:
 * {@code MetadataInfo[MetadataType.SEGMENT, "1994", "1994/Q1"]}</li>
 * <li>For file metadata:
 * {@code MetadataInfo[MetadataType.FILE, "1994", "1994/Q1/0_0_0.parquet"]}</li>
 * <li>For row group metadata:
 * {@code MetadataInfo[MetadataType.ROW_GROUP, "1994", "1994/Q1/0_0_0.parquet/1"]}</li>
 * <li>For partition metadata:
 * {@code MetadataInfo[MetadataType.PARTITION, "1994", "1994/Q1/01"]}</li>
 */
@JsonTypeName("metadataInfo")
@JsonDeserialize(builder = MetadataInfo.MetadataInfoBuilder.class)
public class MetadataInfo {

  public static final String GENERAL_INFO_KEY = "GENERAL_INFO";
  public static final String DEFAULT_SEGMENT_KEY = "DEFAULT_SEGMENT";
  public static final String DEFAULT_COLUMN_PREFIX = "_$SEGMENT_";

  private static final String UNDEFINED_KEY = "UNDEFINED_KEY";

  private final MetadataType type;
  private final String key;
  private final String identifier;

  private MetadataInfo(MetadataInfoBuilder builder) {
    this.type = builder.type;
    this.key = builder.key;
    this.identifier = builder.identifier;
  }

  @JsonProperty
  public MetadataType type() {
    return type;
  }

  @JsonProperty
  public String key() {
    return key;
  }

  @JsonProperty
  public String identifier() {
    return identifier;
  }

  public void toMetadataUnitBuilder(TableMetadataUnit.Builder builder) {
    builder.metadataType(type.name());
    builder.metadataKey(key);
    builder.metadataIdentifier(identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, key, identifier);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataInfo that = (MetadataInfo) o;
    return type == that.type
      && Objects.equals(key, that.key)
      && Objects.equals(identifier, that.identifier);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MetadataInfo.class.getSimpleName() + "[", "]")
      .add("type=" + type)
      .add("key=" + key)
      .add("identifier=" + identifier)
      .toString();
  }

  public static MetadataInfoBuilder builder() {
    return new MetadataInfoBuilder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class MetadataInfoBuilder {
    private MetadataType type;
    private String key;
    private String identifier;

    public MetadataInfoBuilder type(MetadataType type) {
      this.type = type;
      return this;
    }

    public MetadataInfoBuilder key(String key) {
      this.key = key;
      return this;
    }

    public MetadataInfoBuilder identifier(String identifier) {
      this.identifier = identifier;
      return this;
    }

    public MetadataInfoBuilder metadataUnit(TableMetadataUnit unit) {
      type(MetadataType.fromValue(unit.metadataType()));
      key(unit.metadataKey());
      identifier(unit.metadataIdentifier());
      return this;
    }

    public MetadataInfo build() {
      Objects.requireNonNull(type, "type was not set");
      key = key == null ? UNDEFINED_KEY : key;
      identifier = identifier == null ? key : identifier;
      return new MetadataInfo(this);
    }
  }
}
