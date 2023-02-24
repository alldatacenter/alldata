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

package org.apache.atlas.web.params;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;

/**
 * A parameter encapsulating boolean values. If the query parameter value is {@code "true"},
 * regardless of case, the returned value is {@link Boolean#TRUE}. If the query parameter value is
 * {@code "false"}, regardless of case, the returned value is {@link Boolean#FALSE}. All other
 * values will return a {@code 400 Bad Request} response.
 */
public class BooleanParam extends AbstractParam<Boolean> {

    public BooleanParam(String input) {
        super(input);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return '"' + input + "\" must be \"true\" or \"false\".";
    }

    @Override
    protected Boolean parse(String input) throws AtlasBaseException {
        if ("true".equalsIgnoreCase(input)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(input)) {
            return Boolean.FALSE;
        }
        throw new AtlasBaseException(AtlasErrorCode.PARAMETER_PARSING_FAILED, "Boolean.parse: input=" + input);
    }
}