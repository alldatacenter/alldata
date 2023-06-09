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
package org.apache.drill.exec.work.foreman;

import org.apache.drill.exec.exception.UnsupportedOperatorCollector;

public abstract class SqlUnsupportedException extends ForemanSetupException {
  public static enum ExceptionType {
    NONE("NONE"),
    RELATIONAL(UnsupportedRelOperatorException.class.getSimpleName()),
    DATA_TYPE(UnsupportedDataTypeException.class.getSimpleName()),
    FUNCTION(UnsupportedFunctionException.class.getSimpleName());

    private String exceptionType;
    ExceptionType(String exceptionType) {
      this.exceptionType = exceptionType;
    }

    @Override
    public String toString() {
      return exceptionType;
    }
  }

  public SqlUnsupportedException(String jiraNumber, String disabledFunctionality) {
    super(disabledFunctionality + " is not supported\n" +
        "See Apache Drill JIRA: DRILL-" + jiraNumber);
  }

  public SqlUnsupportedException(String errorMessage) {
    super(errorMessage);
  }

  public static void errorClassNameToException(String errorClassName) throws SqlUnsupportedException {
    UnsupportedOperatorCollector collector = new UnsupportedOperatorCollector();
    for(ExceptionType ex : ExceptionType.values()) {
      if(errorClassName.endsWith(ex.toString())) {
        collector.setException(ex);
        collector.convertException();
        collector.clean();
      }
    }
  }
}


