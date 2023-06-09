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

import java.io.IOException;
import java.util.Map;

import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.compile.ClassTransformer.ClassNames;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.codehaus.commons.compiler.CompileException;

@SuppressWarnings("unused")
public abstract class AbstractClassCompiler {
  protected boolean debug = false;

  AbstractClassCompiler(boolean debug) {
    this.debug = debug;
  }

  public byte[][] getClassByteCode(ClassNames className, String sourceCode)
      throws CompileException, IOException, ClassNotFoundException, ClassTransformationException {
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("Compiling (source size={}):\n{}", DrillStringUtils.readable(sourceCode.length()), prefixLineNumbers(sourceCode));

/* uncomment this to get a dump of the generated source in /tmp
      // This can be used to write out the generated operator classes for debugging purposes
      // TODO: should these be put into a directory named with the query id and/or fragment id
      final int lastSlash = className.slash.lastIndexOf('/');
      final File dir = new File("/tmp", className.slash.substring(0, lastSlash));
      dir.mkdirs();
      final File file = new File(dir, className.slash.substring(lastSlash + 1) + ".java");
      final FileWriter writer = new FileWriter(file);
      writer.write(sourceCode);
      writer.close();
*/
    }
    return getByteCode(className, sourceCode);
  }

  protected String prefixLineNumbers(String code) {
    if (!debug) {
      return code;
    }

    StringBuilder out = new StringBuilder();
    int i = 1;
    for (String line : code.split("\n")) {
      int start = out.length();
      out.append(i++);
      int numLength = out.length() - start;
      out.append(":");
      for (int spaces = 0; spaces < 7 - numLength; ++spaces) {
        out.append(" ");
      }
      out.append(line);
      out.append('\n');
    }
    return out.toString();
  }

  protected abstract byte[][] getByteCode(final ClassNames className, final String sourcecode)
      throws CompileException, IOException, ClassNotFoundException, ClassTransformationException;
  public abstract Map<String,byte[]> compile(final ClassNames className, final String sourceCode)
      throws CompileException, IOException, ClassNotFoundException;
  protected abstract org.slf4j.Logger getLogger();

}
