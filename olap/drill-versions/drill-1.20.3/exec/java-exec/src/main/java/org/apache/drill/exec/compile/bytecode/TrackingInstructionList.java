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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class TrackingInstructionList extends InsnList {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TrackingInstructionList.class);

  Frame<BasicValue> currentFrame;
  Frame<BasicValue> nextFrame;
  AbstractInsnNode currentInsn;
  private int index = 0;
  private final Frame<BasicValue>[] frames;
  private final InsnList inner;

  public TrackingInstructionList(final Frame<BasicValue>[] frames, final InsnList inner) {
    this.frames = frames;
    this.inner = inner;
  }

  @Override
  public void accept(final MethodVisitor mv) {
    currentInsn = inner.getFirst();
    while (currentInsn != null) {
        currentFrame = frames[index];
        nextFrame = index + 1 < frames.length ? frames[index + 1] : null;
        currentInsn.accept(mv);

        currentInsn = currentInsn.getNext();
        index++;
    }
  }

  @Override
  public int size() {
    return inner.size();
  }
}
