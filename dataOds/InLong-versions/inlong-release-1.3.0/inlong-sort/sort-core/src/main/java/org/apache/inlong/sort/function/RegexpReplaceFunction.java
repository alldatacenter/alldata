/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.function;

import org.apache.flink.table.functions.ScalarFunction;

/**
 * RegexpReplaceFirstFunction class. It is a custom function, used to replace the value in the string.
 * Specifically, given a string to be replaced, a regular expression to be replaced, and the replaced target string,
 * the regular expression will be satisfied replace the all element of the formula with the target string.
 */
public class RegexpReplaceFunction extends ScalarFunction {

    private static final long serialVersionUID = -7185622027483662395L;

    /**
     * eval is String replacement execution method
     *
     * @param field is the field to be replaced
     * @return replaced value
     */
    public String eval(String field, String regex, String replacement) {
        if (field != null) {
            return field.replaceAll(regex, replacement);
        }
        return null;
    }

}
