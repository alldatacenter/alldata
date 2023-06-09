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
package org.apache.drill.exec.store.mapr;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.slf4j.Logger;

public final class PluginErrorHandler {

  public static UserException unsupportedError(Logger logger, String format, Object... args) {
    return UserException.unsupportedError()
        .message(String.format(format, args))
        .build(logger);
  }

  public static UserException dataReadError(Logger logger, Throwable t) {
    return dataReadError(logger, t, null);
  }

  public static UserException dataReadError(Logger logger, String format, Object... args) {
    return dataReadError(null, format, args);
  }

  public static UserException dataReadError(Logger logger, Throwable t, String format, Object... args) {
    return UserException.dataReadError(t)
        .message(format == null ? null : String.format(format, args))
        .build(logger);
  }

  public static SchemaChangeException schemaChangeException(Logger logger, Throwable t, String format, Object... args) {
    return new SchemaChangeException(format, t, args);
  }
}
