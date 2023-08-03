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

package com.netease.arctic.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
  public static final String EMPTY_ERROR_MESSAGE = "null";

  public static String getErrorMessage(Throwable t, int maxLength) {
    if (t == null) {
      return EMPTY_ERROR_MESSAGE;
    }
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    String stackTrace = sw.toString();
    if (stackTrace.length() > maxLength) {
      return stackTrace.substring(0, maxLength);
    } else if (stackTrace.length() == 0) {
      return EMPTY_ERROR_MESSAGE;
    } else {
      return stackTrace;
    }
  }
}
