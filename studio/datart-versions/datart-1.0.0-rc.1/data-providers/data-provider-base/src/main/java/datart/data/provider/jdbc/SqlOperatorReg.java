/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.data.provider.jdbc;

import lombok.Getter;

import java.util.regex.Pattern;

public enum SqlOperatorReg {

    GT(Pattern.compile("[^\\s<]+(\\s*>\\s*){1}[^\\s=]+"), ">", Pattern.compile("\\s*>\\s*")),

    GTE(Pattern.compile("[^\\s]+(\\s*>=\\s*){1}[^\\s]+"), ">=", Pattern.compile("\\s*>=\\s*")),

    LT(Pattern.compile("[^\\s]+(\\s*<\\s*){1}[^\\s=>]+"), "<", Pattern.compile("\\s*<\\s*")),

    LTE(Pattern.compile("[^\\s]+(\\s*<=\\s*){1}[^\\s]+"), "<=", Pattern.compile("\\s*<=\\s*")),

    EQ(Pattern.compile("[^\\s=<>]+(\\s*=\\s*){1}[^\\s]+"), "=", Pattern.compile("\\s*=\\s*")),

    NEQ(Pattern.compile("[^\\s]+(\\s*<\\s*>\\s*){1}[^\\s]+"), "<>", Pattern.compile("\\s*<\\s*>\\s*")),

    LIKE(Pattern.compile("[^\\s(not)]+(\\s+like\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "like", Pattern.compile("\\s*like\\s*", Pattern.CASE_INSENSITIVE)),

    UNLIKE(Pattern.compile("[^\\s]+(\\s+not\\s+like\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "not like", Pattern.compile("\\s*not\\s*like\\s*", Pattern.CASE_INSENSITIVE)),

    IN(Pattern.compile("[^\\s(not)]+(\\s+in\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "in", Pattern.compile("\\s*in\\s*", Pattern.CASE_INSENSITIVE)),

    NIN(Pattern.compile("[^\\s]+(\\s+not\\s+in\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "not in", Pattern.compile("\\s*not\\s*in\\s*", Pattern.CASE_INSENSITIVE));

    SqlOperatorReg(Pattern pattern, String operator, Pattern replace) {
        this.pattern = pattern;
        this.operator = operator;
        this.replace = replace;
    }

    @Getter
    private final Pattern pattern;

    @Getter
    private final String operator;

    @Getter
    private final Pattern replace;

}
