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
package org.apache.drill.exec.expr.fn;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.janino.Java;
import org.codehaus.janino.Java.CompilationUnit.SingleStaticImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.SingleTypeImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.StaticImportOnDemandDeclaration;
import org.codehaus.janino.Java.CompilationUnit.TypeImportOnDemandDeclaration;
import org.codehaus.janino.util.AbstractTraverser;

public class ImportGrabber {

  private final List<String> imports = new ArrayList<>();
  private final ImportFinder importFinder = new ImportFinder();

  private ImportGrabber() {
  }

  /**
   * Creates list of imports that are present in compilation unit.
   * For example:
   * [import io.netty.buffer.DrillBuf;, import org.apache.drill.exec.expr.DrillSimpleFunc;]
   *
   * @param compilationUnit compilation unit
   * @return list of imports
   */
  public static List<String> getImports(Java.CompilationUnit compilationUnit) {
    ImportGrabber visitor = new ImportGrabber();
    compilationUnit.importDeclarations.forEach(visitor.importFinder::visitImportDeclaration);

    return visitor.imports;
  }

  public class ImportFinder extends AbstractTraverser<RuntimeException> {

    @Override
    public void traverseSingleTypeImportDeclaration(SingleTypeImportDeclaration stid) {
      imports.add(stid.toString());
    }

    @Override
    public void traverseSingleStaticImportDeclaration(SingleStaticImportDeclaration stid) {
      imports.add(stid.toString());
    }

    @Override
    public void traverseTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd) {
      imports.add(tiodd.toString());
    }

    @Override
    public void traverseStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) {
      imports.add(siodd.toString());
    }
  }

}
