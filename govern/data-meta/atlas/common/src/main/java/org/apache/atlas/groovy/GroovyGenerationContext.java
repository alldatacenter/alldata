/**
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

package org.apache.atlas.groovy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context used when generating Groovy queries.  Keeps track of query parameters
 * that are created during the process as well as the portion of the query that
 * has been generated so far.
 *
 */
public class GroovyGenerationContext {

    private boolean parametersAllowed = true;
    private int parameterCount = 0;
    private Map<String, Object> parameterValues = new HashMap<>();

    //used to build up the groovy script
    private StringBuilder generatedQuery = new StringBuilder();

    public void setParametersAllowed(boolean value) {
        this.parametersAllowed = value;
    }

    public GroovyExpression addParameter(Object value) {
        if (this.parametersAllowed) {
            String parameterName = "p" + (++parameterCount);
            this.parameterValues.put(parameterName, value);
            return new IdentifierExpression(parameterName);
        } else {
            LiteralExpression expr = new LiteralExpression(value);
            expr.setTranslateToParameter(false);
            return expr;
        }
    }

    public StringBuilder append(String gremlin) {
        this.generatedQuery.append(gremlin);
        return generatedQuery;
    }

    public String getQuery() {
        return generatedQuery.toString();
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameterValues);
    }
}
