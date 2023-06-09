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

import org.apache.drill.exec.compile.sig.SignatureHolder;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DrillInitMethodVisitor extends MethodVisitor {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillInitMethodVisitor.class);

  final String className;

  public DrillInitMethodVisitor(String className, MethodVisitor mv){
    super(CompilationConfig.ASM_API_VERSION, mv);
    this.className = className;
  }

  @Override
  public void visitInsn(int opcode) {
    if(opcode == Opcodes.RETURN) {
      // Load this.
      super.visitVarInsn(Opcodes.ALOAD, 0);

      // Execute drill init.
      super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, SignatureHolder.DRILL_INIT_METHOD, "()V", false);
    }
    super.visitInsn(opcode);
  }
}
