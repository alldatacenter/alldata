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

import org.objectweb.asm.ClassVisitor;

/**
 * An ASM ClassVisitor that allows for a late-bound delegate.
 *
 * <p>The ClassVisitor constructor takes an optional ClassVisitor argument
 * to which all calls will be delegated. However, in some circumstances, ClassVisitor
 * derivatives may not be able to specify the delegate until after they have called
 * the ClassVisitor constructor themselves. In other words, they may not be able to
 * specify the delegate until *after* they have called super(). This version of the
 * ClassVisitor will support that via the use of the {@link #setDelegate} method.
 */
public class RetargetableClassVisitor extends ClassVisitor {
  private boolean started;

  /**
   * See {@link org.objectweb.asm.ClassVisitor#ClassVisitor(int)}.
   */
  public RetargetableClassVisitor(final int api) {
    super(api);
  }

  /**
   * See {@link org.objectweb.asm.ClassVisitor#ClassVisitor(int, ClassVisitor)}.
   */
  public RetargetableClassVisitor(final int api, final ClassVisitor cv) {
    super(api, cv);
  }

  /**
   * Set the delegate for this ClassVisitor. Must be called before the visitor is
   * used (i.e., before any other methods are called), and only once.
   * @param cv the ClassVisitor to delegate calls to
   */
  protected void setDelegate(final ClassVisitor cv) {
    if (started) {
      throw new IllegalStateException("can't change delegate after visitor has been used");
    }

    // This member was only protected, so we can take advantage of that to set it here.
    this.cv = cv;
  }
}
