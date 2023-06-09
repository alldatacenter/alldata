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
package org.apache.drill.exec.expr;

import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;

/**
 * Encapsulates a Java expression, defined as anything that is
 * valid in the following code:<br>
 * <code>(<i>expr</i>)</code>
 */

public class DirectExpression extends JExpressionImpl {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectExpression.class);

  private final String source;

  private DirectExpression(final String source) {
    super();
    this.source = source;
  }

  @Override
  public void generate(JFormatter f) {
    f.p('(').p(source).p(')');
  }

  public static DirectExpression direct( final String source ) {
    return new DirectExpression(source);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DirectExpression other = (DirectExpression) obj;
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    return true;
  }
}
