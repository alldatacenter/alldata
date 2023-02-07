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
package org.apache.drill.exec.vector.accessor;

import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Raised when a conversion from one type to another is supported at
 * setup type, but a value provided at runtime is not valid for that
 * conversion. Example: trying to convert the string "foo" to an INT
 * column.
 *
 * @see {UnsupportedConversionError} for a setup tie exception where the conversion
 * is not supported for any values.
 */

public class InvalidConversionError extends UnsupportedOperationException {

  private static final long serialVersionUID = 1L;

  public InvalidConversionError(String message) {
    super(message);
  }

  public InvalidConversionError(String message, Exception e) {
    super(message, e);
  }

  public static InvalidConversionError writeError(ColumnMetadata schema, Object value) {
    return writeError(schema, value, null);
  }

  public static InvalidConversionError writeError(ColumnMetadata schema, Object value, Exception e) {
    return new InvalidConversionError(
        String.format("Illegal conversion: Column `%s` of type %s, Illegal value `%s`",
            schema.name(), schema.type().name(),
            value == null ? "null" : value.toString()),
        e);
  }
}
