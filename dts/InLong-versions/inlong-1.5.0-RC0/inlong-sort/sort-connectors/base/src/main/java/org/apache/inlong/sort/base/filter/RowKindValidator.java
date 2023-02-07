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

package org.apache.inlong.sort.base.filter;

import static org.apache.inlong.sort.base.Constants.DELIMITER;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;

/**
 * row kind validator, only specified row kinds can be valid
 * supported row kinds are
 *
 * "+I" represents INSERT.
 * "-U" represents UPDATE_BEFORE.
 * "+U" represents UPDATE_AFTER.
 * "-D" represents DELETE.
 *
 */
public class RowKindValidator implements RowValidator {

    private final Set<RowKind> rowKindsFiltered = new HashSet<>();

    private static final String pattern = "(\\+I|\\+U|-U|-D)(&(\\+I|\\+U|-U|-D))*";

    public RowKindValidator(List<String> rowKinds) {
        Preconditions.checkArgument(!rowKinds.isEmpty(),
                "rowKinds should not be empty");
        for (String rowKind : rowKinds) {
            Arrays.stream(RowKind.values()).filter(value -> value.shortString().equals(rowKind))
                    .findFirst().ifPresent(rowKindsFiltered::add);
        }
    }

    public RowKindValidator(String rowKinds) {
        Preconditions.checkArgument(Pattern.matches(pattern, rowKinds),
                String.format("rowKinds is not valid, should match the pattern %s,"
                        + " the input value is %s", pattern, rowKinds));
        for (String rowKind : rowKinds.split(DELIMITER)) {
            Arrays.stream(RowKind.values()).filter(value -> value.shortString().equals(rowKind))
                    .findFirst().ifPresent(rowKindsFiltered::add);
        }
    }

    @Override
    public boolean validate(RowKind rowKind) {
        return rowKindsFiltered.contains(rowKind);
    }
}
