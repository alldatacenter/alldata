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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtility;

/**
 * REST API operations should either be documented, or marked to be ignored.
 */
public class UndocumentedRestApiOperationCheck extends AbstractCheck {

  private static final Set<String> API_ANNOTATIONS = ImmutableSet.of("DELETE", "GET", "HEAD", "OPTIONS", "PUT", "POST");
  private static final String API_OPERATION = "ApiOperation";
  private static final String API_IGNORE = "ApiIgnore";
  public static final String MESSAGE = "REST API operation should be documented";
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
    if (isApiOperation(ast) && !isDocumentedApiOperation(ast) && !isIgnoredApi(ast)) {
      log(ast.getLineNo(), MESSAGE);
    }
  }

  private static boolean isApiOperation(DetailAST ast) {
    DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
    if (modifiers.findFirstToken(TokenTypes.LITERAL_PRIVATE) != null) {
      return false;
    }

    DetailAST annotation = modifiers.findFirstToken(TokenTypes.ANNOTATION);
    while (annotation != null) {
      DetailAST name = annotation.findFirstToken(TokenTypes.IDENT);
      if (name != null && API_ANNOTATIONS.contains(name.getText())) {
        return true;
      }
      annotation = annotation.getNextSibling();
    }

    return false;
  }

  private static boolean isDocumentedApiOperation(DetailAST ast) {
    return AnnotationUtility.containsAnnotation(ast, API_OPERATION);
  }

  private static boolean isIgnoredApi(DetailAST ast) {
    return AnnotationUtility.containsAnnotation(ast, API_IGNORE);
  }

}
