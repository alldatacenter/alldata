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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.tools.SimpleJavaFileObject;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

/* package */
final class DrillJavaFileObject extends SimpleJavaFileObject {
  private final String sourceCode;

  private final ByteArrayOutputStream outputStream;

  private Map<String, DrillJavaFileObject> outputFiles;

  private final String className;

  public DrillJavaFileObject(final String className, final String sourceCode) {
    super(makeURI(className), Kind.SOURCE);
    this.className = className;
    this.sourceCode = sourceCode;
    this.outputStream = null;
  }

  private DrillJavaFileObject(final String name, final Kind kind) {
    super(makeURI(name), kind);
    this.className = name;
    this.outputStream = new ByteArrayOutputStream();
    this.sourceCode = null;
  }

  public boolean isCompiled() {
    return (outputFiles != null);
  }

  public byte[][] getByteCode() {
    if (!isCompiled()) {
      return null;
    } else {
      int index = 0;
      byte[][] byteCode = new byte[outputFiles.size()][];
      for(DrillJavaFileObject outputFile : outputFiles.values()) {
        byteCode[index++] = outputFile.outputStream.toByteArray();
      }
      return byteCode;
    }
  }

  /**
   * Return the byte codes for the main class and any nested
   * classes.
   *
   * @return map of fully-qualified class names to byte codes
   * for the class
   */

  public Map<String,byte[]> getClassByteCodes() {
    Map<String,byte[]> results = new HashMap<>();
    for(DrillJavaFileObject outputFile : outputFiles.values()) {
      results.put(outputFile.className, outputFile.outputStream.toByteArray());
    }
    return results;
  }

  public DrillJavaFileObject addOutputJavaFile(String className) {
    if (outputFiles == null) {
      outputFiles = Maps.newLinkedHashMap();
    }
    DrillJavaFileObject outputFile = new DrillJavaFileObject(className, Kind.CLASS);
    outputFiles.put(className, outputFile);
    return outputFile;
  }

  @Override
  public Reader openReader(final boolean ignoreEncodingErrors) throws IOException {
    return new StringReader(sourceCode);
  }

  @Override
  public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
    if (sourceCode == null) {
      throw new UnsupportedOperationException("This instance of DrillJavaFileObject is not an input object.");
    }
    return sourceCode;
  }

  @Override
  public OutputStream openOutputStream() {
    if (outputStream == null) {
      throw new UnsupportedOperationException("This instance of DrillJavaFileObject is not an output object.");
    }
    return outputStream;
  }

  private static URI makeURI(final String canonicalClassName) {
    final int dotPos = canonicalClassName.lastIndexOf('.');
    final String simpleClassName = dotPos == -1 ? canonicalClassName : canonicalClassName.substring(dotPos + 1);
    try {
      return new URI(simpleClassName + Kind.SOURCE.extension);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
