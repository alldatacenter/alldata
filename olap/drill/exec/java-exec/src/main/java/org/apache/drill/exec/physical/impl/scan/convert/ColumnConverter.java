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
package org.apache.drill.exec.physical.impl.scan.convert;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;

/**
 * Base class for any kind of column converter. Extend this class if
 * your columns work with reader-specific types, such as having to
 * call a type-specific method on some row object. In this case,
 * all your subclasses would implement the same method, say
 * {@code void convertValue(MyRow row)}. Subclases might know their
 * column indexes within the row object, extract the value using
 * type-specific methods, then pass the value onto the scalar writer.
 */
public class ColumnConverter {
  protected final ScalarWriter baseWriter;

  public ColumnConverter(ScalarWriter colWriter) {
    this.baseWriter = colWriter;
  }

  public ScalarWriter writer() { return baseWriter; }
  public ColumnMetadata schema() { return baseWriter.schema(); }

  protected UnsupportedConversionError conversionError(String javaType) {
    return UnsupportedConversionError.writeError(schema(), javaType);
  }
}
