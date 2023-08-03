/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.hook;

import org.apache.atlas.impala.model.ImpalaOperationType;
import org.apache.commons.lang.StringUtils;
import java.util.regex.Pattern;

/**
 * Parse an Impala query text and output the impala operation type
 */
public class ImpalaOperationParser {

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern CREATE_VIEW_PATTERN =
            Pattern.compile("^[ ]*\\bcreate\\b.*\\bview\\b.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern CREATE_TABLE_AS_SELECT_PATTERN =
            Pattern.compile("^[ ]*\\bcreate\\b.*\\btable\\b.*\\bas\\b.*\\bselect\\b.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern ALTER_VIEW_AS_SELECT_PATTERN =
            Pattern.compile("^[ ]*\\balter\\b.*\\bview\\b.*\\bas.*\\bselect\\b.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern INSERT_SELECT_FROM_PATTERN =
            Pattern.compile("^[ ]*\\binsert\\b.*\\b(into|overwrite)\\b.*\\bselect\\b.*\\bfrom\\b.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public ImpalaOperationParser() {
    }

    public static ImpalaOperationType getImpalaOperationType(String queryText) {
        // Impala does no generate lineage record for command "LOAD DATA IN PATH"
        String queryTextWithNoComments = COMMENT_PATTERN.matcher(queryText).replaceAll("");
        if (doesMatch(queryTextWithNoComments, CREATE_VIEW_PATTERN)) {
            return ImpalaOperationType.CREATEVIEW;
        } else if (doesMatch(queryTextWithNoComments, CREATE_TABLE_AS_SELECT_PATTERN)) {
            return ImpalaOperationType.CREATETABLE_AS_SELECT;
        } else if (doesMatch(queryTextWithNoComments, ALTER_VIEW_AS_SELECT_PATTERN)) {
            return ImpalaOperationType.ALTERVIEW_AS;
        } else if (doesMatch(queryTextWithNoComments, INSERT_SELECT_FROM_PATTERN)) {
            return ImpalaOperationType.QUERY;
        }

        return ImpalaOperationType.UNKNOWN;
    }

    public static ImpalaOperationType getImpalaOperationSubType(ImpalaOperationType operationType, String queryText) {
        if (operationType == ImpalaOperationType.QUERY) {
            if (StringUtils.containsIgnoreCase(queryText, "insert into")) {
                return ImpalaOperationType.INSERT;
            } else if (StringUtils.containsIgnoreCase(queryText, "insert overwrite")) {
                return ImpalaOperationType.INSERT_OVERWRITE;
            }
        }

        return ImpalaOperationType.UNKNOWN;
    }

    private static boolean doesMatch(final String queryText, final Pattern pattern) {
        return pattern.matcher(queryText).matches();
    }

}