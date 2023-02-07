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
package org.apache.drill.common.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@link TestRule} to repeat a test for a specified number of times. If "count" <= 0, the test is not run.
 * For example,
 * <pre>
 * {@code
 * @Test
 * @Repeat(count = 20) // repeats the test 20 times
 * public void unitTest() {
 *   // code
 * }
 * }
 * </pre>
 * @deprecated It was created for Junit4. Junit5 has its own @RepeatedTest(num). And looks like it is not used now, but
 * it can be used for manual testing
 */
@Deprecated
public class RepeatTestRule implements TestRule {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface Repeat {
    int count();
  }

  private static class RepeatStatement extends Statement {
    private final Statement statement;
    private final int count;

    private RepeatStatement(final Statement statement, final int count) {
      this.statement = statement;
      this.count = count;
    }

    @Override
    public void evaluate() throws Throwable {
      for (int i = 0; i < count; ++i) {
        statement.evaluate();
      }
    }
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    final Repeat repeat = description.getAnnotation(Repeat.class);
    return repeat != null ? new RepeatStatement(base, repeat.count()) : base;
  }
}
