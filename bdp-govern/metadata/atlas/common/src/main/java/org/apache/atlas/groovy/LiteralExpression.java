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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a literal value.
 */
public class LiteralExpression extends AbstractGroovyExpression {

    public static final LiteralExpression TRUE = new LiteralExpression(true);
    public static final LiteralExpression FALSE = new LiteralExpression(false);
    public static final LiteralExpression NULL = new LiteralExpression(null);

    private Object value;
    private boolean translateToParameter = true;
    private boolean addTypeSuffix = false;

    public LiteralExpression(Object value, boolean addTypeSuffix) {
        this.value = value;
        this.translateToParameter = value instanceof String;
        this.addTypeSuffix = addTypeSuffix;
    }

    public LiteralExpression(Object value, boolean addTypeSuffix, boolean translateToParameter) {
        this.value = value;
        this.translateToParameter = translateToParameter;
        this.addTypeSuffix = addTypeSuffix;
    }

    public LiteralExpression(Object value) {
        this.value = value;
        this.translateToParameter = value instanceof String;
    }

    private String getTypeSuffix() {
        if (!addTypeSuffix) {
            return "";
        }
        if (value.getClass() == Long.class) {
            return "L";
        }

        if (value.getClass() == Float.class) {
            return "f";
        }

        if (value.getClass() == Double.class) {
            return "d";
        }

        return "";
    }

    @Override
    public void generateGroovy(GroovyGenerationContext context) {

        if (translateToParameter) {
            GroovyExpression result = context.addParameter(value);
            result.generateGroovy(context);
            return;
        }

        if (value instanceof String) {
            String escapedValue = getEscapedValue();
            context.append("'");
            context.append(escapedValue);
            context.append("'");

        } else {
            context.append(String.valueOf(value));
            context.append(getTypeSuffix());
        }

    }

    public Object getValue() {
        return value;
    }

    private String getEscapedValue() {
        String escapedValue = (String)value;
        escapedValue = escapedValue.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\"));
        escapedValue = escapedValue.replaceAll(Pattern.quote("'"), Matcher.quoteReplacement("\\'"));
        return escapedValue;
    }

    public void setTranslateToParameter(boolean translateToParameter) {
        this.translateToParameter = translateToParameter;
    }

    @Override
    public List<GroovyExpression> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public GroovyExpression copy(List<GroovyExpression> newChildren) {
        assert newChildren.size() == 0;
        return new LiteralExpression(value, addTypeSuffix, translateToParameter);
    }
}
