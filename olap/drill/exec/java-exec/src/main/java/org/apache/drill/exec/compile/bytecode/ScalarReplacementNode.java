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
package org.apache.drill.exec.compile.bytecode;

import java.util.LinkedList;

import org.apache.drill.exec.compile.CheckMethodVisitorFsm;
import org.apache.drill.exec.compile.CompilationConfig;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class ScalarReplacementNode extends MethodNode {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScalarReplacementNode.class);
  private final boolean verifyBytecode;

  private final String className;
  private final String[] exceptionsArr;
  private final MethodVisitor inner;

  public ScalarReplacementNode(final String className, final int access, final String name,
      final String desc, final String signature, final String[] exceptions, final MethodVisitor inner,
      final boolean verifyBytecode) {
    super(CompilationConfig.ASM_API_VERSION, access, name, desc, signature, exceptions);
    this.className = className;
    this.exceptionsArr = exceptions;
    this.inner = inner;
    this.verifyBytecode = verifyBytecode;
  }

  @Override
  public void visitEnd() {
    /*
     * Note this is a MethodNode, not a MethodVisitor. As a result, calls to the various visitX()
     * methods will be building up a method. Then, once we analyze it, we use accept() to visit that
     * method and transform it with the InstructionModifier at the bottom.
     */
    super.visitEnd();

    final LinkedList<ReplacingBasicValue> valueList = new LinkedList<>();
    final MethodAnalyzer<BasicValue> analyzer =
        new MethodAnalyzer<>(new ReplacingInterpreter(className, valueList));
    Frame<BasicValue>[] frames;
    try {
      frames = analyzer.analyze(className, this);
    } catch (final AnalyzerException e) {
      throw new IllegalStateException(e);
    }

    if (logger.isTraceEnabled()) {
      final StringBuilder sb = new StringBuilder();
      sb.append("ReplacingBasicValues for " + className + "\n");
      for(final ReplacingBasicValue value : valueList) {
        value.dump(sb, 2);
        sb.append('\n');
      }
      logger.debug(sb.toString());
    }

    // wrap the instruction handler so that we can do additional things
    final TrackingInstructionList list = new TrackingInstructionList(frames, this.instructions);
    this.instructions = list;

    MethodVisitor methodVisitor = inner;
    if (verifyBytecode) {
      methodVisitor = new CheckMethodVisitorFsm(CompilationConfig.ASM_API_VERSION, methodVisitor);
    }
    final InstructionModifier holderV = new InstructionModifier(this.access, this.name, this.desc,
        this.signature, this.exceptionsArr, list, methodVisitor);
    accept(holderV);
  }
}
