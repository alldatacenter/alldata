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

package org.apache.inlong.sort.formats.base;

/**
 * Common constants used in various formats.
 */
public class TableFormatConstants {

    public static final String FORMAT_SCHEMA = "format.schema";
    public static final String FORMAT_DELIMITER = "format.delimiter";
    public static final String FORMAT_ENTRY_DELIMITER = "format.entry-delimiter";
    public static final String FORMAT_KV_DELIMITER = "format.kv-delimiter";
    public static final String FORMAT_NULL_LITERAL = "format.null-literal";
    public static final String FORMAT_ESCAPE_CHARACTER = "format.escape-character";
    public static final String FORMAT_QUOTE_CHARACTER = "format.quote-character";
    public static final String FORMAT_IGNORE_ERRORS = "format.ignore-errors";
    public static final String FORMAT_CHARSET = "format.charset";

    public static final char DEFAULT_DELIMITER = ',';
    public static final char DEFAULT_ENTRY_DELIMITER = '&';
    public static final char DEFAULT_KV_DELIMITER = '=';
    public static final boolean DEFAULT_IGNORE_ERRORS = false;
    public static final String DEFAULT_CHARSET = "UTF-8";
}
