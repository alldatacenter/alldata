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
package org.apache.drill.exec.exception;

import org.apache.drill.common.exceptions.DrillRuntimeException;

/**
 * Metadata runtime exception to indicate issues connected with table metadata.
 */
public class MetadataException extends DrillRuntimeException {
  private final MetadataExceptionType exceptionType;

  private MetadataException(MetadataExceptionType exceptionType) {
    super(exceptionType.message);
    this.exceptionType = exceptionType;
  }

  private MetadataException(MetadataExceptionType exceptionType, Throwable cause) {
    super(exceptionType.message, cause);
    this.exceptionType = exceptionType;
  }

  public MetadataExceptionType getExceptionType() {
    return exceptionType;
  }

  public static MetadataException of(MetadataExceptionType exceptionType) {
    return new MetadataException(exceptionType);
  }

  public static MetadataException of(MetadataExceptionType exceptionType, Throwable cause) {
    return new MetadataException(exceptionType, cause);
  }

  public enum MetadataExceptionType {
    OUTDATED_METADATA("Metastore metadata is outdated."),

    INCONSISTENT_METADATA("Inconsistent Metastore metadata. " +
        "Metadata was refreshed after it was fetched from the Metastore."),

    INCOMPLETE_METADATA("Metastore does not have metadata for row groups " +
        "and `metastore.metadata.fallback_to_file_metadata` is disabled. " +
        "Please either execute ANALYZE with 'ROW_GROUP' level " +
        "for the querying table or enable `metastore.metadata.fallback_to_file_metadata` to allow " +
        "using metadata taken from the file metadata cache or table files."),

    ABSENT_SCHEMA("Table schema wasn't provided " +
        "and `metastore.metadata.use_schema` is disabled. " +
        "Please either provide table schema for [%s] table " +
        "(using table function or creating schema file) or enable " +
        "`metastore.metadata.use_schema`."),

    FALLBACK_EXCEPTION("Exception happened when was attempting to use fallback metadata.");

    private final String message;

    MetadataExceptionType(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }
}
