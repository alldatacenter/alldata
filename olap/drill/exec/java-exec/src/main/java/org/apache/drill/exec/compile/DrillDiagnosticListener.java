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
package org.apache.drill.exec.compile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;

/* package */
final class DrillDiagnosticListener implements DiagnosticListener<JavaFileObject> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillDiagnosticListener.class);

  @Override
  public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
    if (diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
      String message = diagnostic.toString() + " (" + diagnostic.getCode() + ")";
      logger.error(message);
      Location loc = new Location( //
          diagnostic.getSource().toString(), //
          (short) diagnostic.getLineNumber(), //
          (short) diagnostic.getColumnNumber() //
      );
      // Wrap the exception in a RuntimeException, because "report()"
      // does not declare checked exceptions.
      throw new RuntimeException(new CompileException(message, loc));
    } else if (logger.isTraceEnabled()) {
      logger.trace(diagnostic.toString() + " (" + diagnostic.getCode() + ")");
    }
  }

}