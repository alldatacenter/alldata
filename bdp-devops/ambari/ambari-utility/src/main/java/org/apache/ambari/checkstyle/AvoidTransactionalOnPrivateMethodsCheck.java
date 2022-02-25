/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Detects private methods annotated as <code>Transactional</code>.
 * See https://github.com/google/guice/wiki/Transactions for why this should be
 * avoided.
 */
public class AvoidTransactionalOnPrivateMethodsCheck extends AbstractCheck {

  private static final String ANNOTATION_NAME = "Transactional";
  public static final String MSG_TRANSACTIONAL_ON_PRIVATE_METHOD = "@" + ANNOTATION_NAME + " should not be used on private methods";
  private static final int[] TOKENS = new int[] { TokenTypes.METHOD_DEF };

  @Override
  public int[] getAcceptableTokens() {
    return TOKENS;
  }

  @Override
  public int[] getDefaultTokens() {
    return TOKENS;
  }

  @Override
  public int[] getRequiredTokens() {
    return TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers.findFirstToken(TokenTypes.LITERAL_PRIVATE) != null) {
      DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
      while (annotation != null) {
        DetailAST name = annotation.findFirstToken(TokenTypes.IDENT);
        if (name != null && ANNOTATION_NAME.equals(name.getText())) {
          log(ast.getLineNo(), MSG_TRANSACTIONAL_ON_PRIVATE_METHOD);
          break;
        }
        annotation = annotation.getNextSibling();
      }
    }
  }

}
