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

/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;

public class ClassBodyBuilder {

  private String[] optionalDefaultImports = null;
  private String className = IClassBodyEvaluator.DEFAULT_CLASS_NAME;
  private Class<?> optionalExtendedType = null;
  private Class<?>[] implementedTypes = new Class[0];
  private String[] imports = {};
  private String body;
  private boolean used = false;

  public static ClassBodyBuilder newBuilder(){
    return new ClassBodyBuilder();
  }

  private ClassBodyBuilder(){
  }

  public ClassBodyBuilder setClassName(String className) {
    assertNotCooked();
    this.className = className;
    return this;
  }

  public ClassBodyBuilder setDefaultImports(String... optionalDefaultImports) {
    assertNotCooked();
    this.optionalDefaultImports = optionalDefaultImports;
    return this;
  }

  public ClassBodyBuilder setExtendedClass(Class<?> optionalExtendedType) {
    assertNotCooked();
    this.optionalExtendedType = optionalExtendedType;
    return this;
  }

  public ClassBodyBuilder setImplementedInterfaces(Class<?>... implementedTypes) {
    assertNotCooked();
    this.implementedTypes = implementedTypes;
    return this;
  }

  private void assertNotCooked() {
    assert !used;
  }

  public ClassBodyBuilder setImports(String[] imports) {
    assertNotCooked();
    this.imports = imports;
    return this;
  }

  public ClassBodyBuilder setBody(String body) {
    assertNotCooked();
    this.body = body;
    return this;
  }

  public String build() throws CompileException, IOException {
    used = true;
    // Wrap the class body in a compilation unit.
    {
      StringWriter sw1 = new StringWriter();
      {
        PrintWriter pw = new PrintWriter(sw1);

        // Break the class name up into package name and simple class name.
        String packageName; // null means default package.
        String simpleClassName;
        {
          int idx = this.className.lastIndexOf('.');
          if (idx == -1) {
            packageName = "";
            simpleClassName = this.className;
          } else {
            packageName = this.className.substring(0, idx);
            simpleClassName = this.className.substring(idx + 1);
          }
        }

        // Print PACKAGE directive.
        if (!packageName.isEmpty()) {
          pw.print("package ");
          pw.print(packageName);
          pw.println(";");
        }

        // Print default imports.
        if (this.optionalDefaultImports != null) {
          for (String defaultImport : this.optionalDefaultImports) {
            pw.print("import ");
            pw.print(defaultImport);
            pw.println(";");
          }
        }

        // Print imports as declared in the document.
        for (String imporT : imports) {
          pw.print("import ");
          pw.print(imporT);
          pw.println(";");
        }

        // Print the class declaration.
        pw.print("public class ");
        pw.print(simpleClassName);
        if (this.optionalExtendedType != null) {
          pw.print(" extends ");
          pw.print(this.optionalExtendedType.getCanonicalName());
        }
        if (this.implementedTypes.length > 0) {
          pw.print(" implements ");
          pw.print(this.implementedTypes[0].getName());
          for (int i = 1; i < this.implementedTypes.length; ++i) {
            pw.print(", ");
            pw.print(this.implementedTypes[i].getName());
          }
        }
        pw.println(" {");
        pw.close();
      }

      StringWriter sw2 = new StringWriter();
      {
        PrintWriter pw = new PrintWriter(sw2);
        pw.println("}");
        pw.close();
      }

      return sw1.toString() + body + sw2.toString();

    }

  }

//  /**
//   * Heuristically parse IMPORT declarations at the beginning of the character stream produced by the given
//   * {@link Reader}. After this method returns, all characters up to and including that last IMPORT declaration have
//   * been read from the {@link Reader}.
//   * <p>
//   * This method does not handle comments and string literals correctly, i.e. if a pattern that looks like an IMPORT
//   * declaration appears within a comment or a string literal, it will be taken as an IMPORT declaration.
//   *
//   * @param r
//   *          A {@link Reader} that supports MARK, e.g. a {@link BufferedReader}
//   * @return The parsed imports, e.g. {@code "java.util.*", "static java.util.Map.Entry" }
//   */
//  protected static String[] parseImportDeclarations(Reader r) throws IOException {
//    final CharBuffer cb = CharBuffer.allocate(10000);
//    r.mark(cb.limit());
//    r.read(cb);
//    cb.rewind();
//
//    List<String> imports = new ArrayList<String>();
//    int afterLastImport = 0;
//    for (Matcher matcher = IMPORT_STATEMENT_PATTERN.matcher(cb); matcher.find();) {
//      imports.add(matcher.group(1));
//      afterLastImport = matcher.end();
//    }
//    r.reset();
//    r.skip(afterLastImport);
//    return imports.toArray(new String[imports.size()]);
//  }
//
//  private static final Pattern IMPORT_STATEMENT_PATTERN = Pattern.compile("\\bimport\\s+" + "(" + "(?:static\\s+)?"
//      + "[\\p{javaLowerCase}\\p{javaUpperCase}_\\$][\\p{javaLowerCase}\\p{javaUpperCase}\\d_\\$]*"
//      + "(?:\\.[\\p{javaLowerCase}\\p{javaUpperCase}_\\$][\\p{javaLowerCase}\\p{javaUpperCase}\\d_\\$]*)*"
//      + "(?:\\.\\*)?" + ");");

//  @Override
//  public Object createInstance(Reader reader) throws CompileException, IOException {
//    this.cook(reader);
//    try {
//      return this.getClazz().newInstance();
//    } catch (InstantiationException ie) {
//      CompileException ce = new CompileException(
//          ("Class is abstract, an interface, an array class, a primitive type, or void; "
//              + "or has no zero-parameter constructor"), null);
//      ce.initCause(ie);
//      throw ce;
//    } catch (IllegalAccessException iae) {
//      CompileException ce = new CompileException("The class or its zero-parameter constructor is not accessible", null);
//      ce.initCause(iae);
//      throw ce;
//    }
//  }

}
