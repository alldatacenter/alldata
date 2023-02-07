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

import org.apache.drill.exec.compile.sig.RuntimeOverridden;
import org.apache.drill.exec.exception.SchemaChangeException;

/**
 * Test case that also illustrates the proper construction of templates
 * with nested classes.
 */
public abstract class ExampleTemplateWithInner implements ExampleInner{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExampleTemplateWithInner.class);

  /**
   * Outer class methods can be abstract. The generated methods
   * replace (code merge) or override (plain-old Java) this method.
   */

  @Override
  public abstract void doOutside() throws SchemaChangeException;

  /**
   * Nested classes can be static or non-static "inner" classes.
   * Inner classes can access fields in the outer class - a
   * feature not demonstrated here.
   * <p>
   * TODO: Test that use case here.
   */

  public class TheInnerClass {

    /**
     * Because of how Drill does byte-code merging, the methods
     * on the inner class cannot be abstract; they must have an
     * empty implementation which is discarded and replaced with the
     * generated implementation. In plain-old Java, the generated
     * method overrides this one.
     * @throws SchemaChangeException all methods that Drill generates
     * throw this exception. This does not matter for byte-code merge,
     * but plain-old Java requires that the overridden method declare
     * any exceptions thrown by the overriding method.
     */

    @RuntimeOverridden
    public void doInside() throws SchemaChangeException {};

    /**
     * Not overridden. Must pass along (or handle) the SchemaChangeException
     * thrown by the generated method.
     *
     * @throws SchemaChangeException
     */
    public void doDouble() throws SchemaChangeException {
      DoubleInner di = newDoubleInner();
      di.doDouble();
    }

    protected DoubleInner newDoubleInner() {
      return this.injectMembers(new DoubleInner());
    }

    protected DoubleInner injectMembers(DoubleInner doubleInner) {
      return doubleInner;
    }

    public class DoubleInner {
      @RuntimeOverridden
      public void doDouble() throws SchemaChangeException {};
    }
  }

  @Override
  public void doInsideOutside() throws SchemaChangeException {
    TheInnerClass inner = newTheInnerClass();
    inner.doInside();
    inner.doDouble();
  }

  /**
   * The byte-code merge mechanism will replace in-line calls to
   * <tt>new TheInnerClass</tt> with a call to create the generated
   * inner class. But, plain-old Java can only override methods. The
   * code generator will create a method of the form
   * <tt>new<i>InnerClassName</tt> to create the generated inner
   * class, which is subclass of the template inner class. The
   * byte-code transform technique rewrites this method to create the
   * generated inner class directly
   * @return an instance of the inner class, at runtime the generated
   * subclass (or replacement) of the template inner class
   */
  protected TheInnerClass newTheInnerClass( ) {
    return this.injectMembers(new TheInnerClass());
  }

  protected TheInnerClass injectMembers(TheInnerClass theInnerClass) {
    return theInnerClass;
  }
}
