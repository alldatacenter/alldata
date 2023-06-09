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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;

/**
 * Utilities commonly used with ASM.
 *
 * <p>There are several class verification utilities which use
 * CheckClassAdapter (DrillCheckClassAdapter) to ensure classes are well-formed;
 * these are packaged as boolean functions so that they can be used in assertions.
 */
public class AsmUtil {
  // This class only contains static utilities.
  private AsmUtil() {
  }

  /**
   * Check to see if a class is well-formed.
   *
   * @param logger the logger to write to if a problem is found
   * @param logTag a tag to print to the log if a problem is found
   * @param classNode the class to check
   * @return true if the class is ok, false otherwise
   */
  public static boolean isClassOk(final Logger logger, final String logTag, final ClassNode classNode) {
    final StringWriter sw = new StringWriter();
    final ClassWriter verifyWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classNode.accept(verifyWriter);
    final ClassReader ver = new ClassReader(verifyWriter.toByteArray());
    try {
      DrillCheckClassAdapter.verify(ver, false, new PrintWriter(sw));
    } catch(final Exception e) {
      logger.info("Caught exception verifying class:");
      logClass(logger, logTag, classNode);
      throw e;
    }
    final String output = sw.toString();
    if (!output.isEmpty()) {
      logger.info("Invalid class:\n" +  output);
      return false;
    }

    return true;
  }

  /**
   * Check to see if a class is well-formed.
   *
   * @param logger the logger to write to if a problem is found
   * @param logTag a tag to print to the log if a problem is found
   * @param classBytes the bytecode of the class to check
   * @return true if the class is ok, false otherwise
   */
  public static boolean isClassBytesOk(final Logger logger, final String logTag, final byte[] classBytes) {
    final ClassNode classNode = classFromBytes(classBytes, 0);
    return isClassOk(logger, logTag, classNode);
  }

  /**
   * Create a ClassNode from bytecode.
   *
   * @param classBytes the bytecode
   * @param asmReaderFlags flags for ASM; see {@link org.objectweb.asm.ClassReader#accept(org.objectweb.asm.ClassVisitor, int)}
   * @return the ClassNode
   */
  public static ClassNode classFromBytes(final byte[] classBytes, final int asmReaderFlags) {
    final ClassNode classNode = new ClassNode(CompilationConfig.ASM_API_VERSION);
    final ClassReader classReader = new ClassReader(classBytes);
    classReader.accept(classNode, asmReaderFlags);
    return classNode;
  }

  /**
   * Write a class to the log.
   *
   * <p>
   * Writes at level TRACE.
   *
   * @param logger
   *          the logger to write to
   * @param logTag
   *          a tag to print to the log
   * @param classNode
   *          the class
   */
  public static void logClass(final Logger logger, final String logTag, final ClassNode classNode) {
    if (logger.isTraceEnabled()) {
      logger.trace(logTag);
      final StringWriter stringWriter = new StringWriter();
      final PrintWriter printWriter = new PrintWriter(stringWriter);
      final TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);
      classNode.accept(traceClassVisitor);
      logger.trace(stringWriter.toString());
    }
  }

  /**
   * Write a class to the log.
   *
   * <p>Writes at level DEBUG.
   *
   * @param logTag a tag to print to the log
   * @param classBytes the class' bytecode
   * @param logger the logger to write to
   */
  public static void logClassFromBytes(final Logger logger, final String logTag, final byte[] classBytes) {
    final ClassNode classNode = classFromBytes(classBytes, 0);
    logClass(logger, logTag, classNode);
  }

  /**
   * Determine if the given opcode is a load of a constant (xCONST_y).
   *
   * @param opcode the opcode
   * @return true if the opcode is one of the constant loading ones, false otherwise
   */
  public static boolean isXconst(final int opcode) {
    switch(opcode) {
    case Opcodes.ICONST_0:
    case Opcodes.ICONST_1:
    case Opcodes.ICONST_2:
    case Opcodes.ICONST_3:
    case Opcodes.ICONST_4:
    case Opcodes.ICONST_5:
    case Opcodes.ICONST_M1:
    case Opcodes.DCONST_0:
    case Opcodes.DCONST_1:
    case Opcodes.FCONST_0:
    case Opcodes.FCONST_1:
    case Opcodes.LCONST_0:
    case Opcodes.LCONST_1:
      return true;
    }

    return false;
  }

  /**
   * Determine if the given opcode is an ADD of some kind (xADD).
   *
   * @param opcode the opcode
   * @return true if the opcode is one of the ADDs, false otherwise
   */
  public static boolean isXadd(final int opcode) {
    switch(opcode) {
    case Opcodes.IADD:
    case Opcodes.DADD:
    case Opcodes.FADD:
    case Opcodes.LADD:
      return true;
    }

    return false;
  }
}
