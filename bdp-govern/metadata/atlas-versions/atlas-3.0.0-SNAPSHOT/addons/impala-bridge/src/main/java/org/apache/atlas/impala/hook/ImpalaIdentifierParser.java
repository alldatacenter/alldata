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

import java.util.Arrays;
import java.util.HashSet;

import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * Check if a string is a valid Impala table identifier.
 * It could be <dbName>.<tableName> or <tableName>
 */
public class ImpalaIdentifierParser {
    // http://www.cloudera.com/content/www/en-us/documentation/enterprise/latest/topics/impala_identifiers.html
    // https://github.com/apache/impala/blob/64e6719870db5602a6fa85014bc6c264080b9414/tests/common/patterns.py
    // VALID_IMPALA_IDENTIFIER_REGEX = re.compile(r'^[a-zA-Z][a-zA-Z0-9_]{,127}$')
    // add "." to allow <dbName>.<tableName>
    public static final String VALID_IMPALA_IDENTIFIER_REGEX = "^[a-zA-Z][a-zA-Z0-9_.]{0,127}$";

    public static boolean isTableNameValid(String inTableName) {
        if (StringUtils.isEmpty(inTableName)) {
            return false;
        }

        if (!inTableName.matches(VALID_IMPALA_IDENTIFIER_REGEX)) {
            return false;
        }

        String[] tokens = inTableName.split(".");
        if (tokens.length > 2) {
            // valid value should be <dbName>.<tableName> or <tableName>
            return false;
        }

        for (String token : tokens) {
            if (isReserved(token)) {
                return false;
            }
        }

        return true;
    }

    // The following is extracted from Impala code.
    // Mainly from https://github.com/apache/impala/blob/master/fe/src/main/jflex/sql-scanner.flex
    // Map from keyword string to token id.
    // We use a linked hash map because the insertion order is important.
    // for example, we want "and" to come after "&&" to make sure error reporting
    // uses "and" as a display name and not "&&".
    // Please keep the puts sorted alphabetically by keyword (where the order
    // does not affect the desired error reporting)
    static HashSet<String> keywordMap;
    // map from token id to token description
    static HashSet<String> tokenIdMap;
    // Reserved words are words that cannot be used as identifiers. It is a superset of
    // keywords.
    static Set<String> reservedWords;


    public static void init() {
        // initilize keywords
        keywordMap = new HashSet<>();
        keywordMap.add("&&");
        keywordMap.add("add");
        keywordMap.add("aggregate");
        keywordMap.add("all");
        keywordMap.add("alter");
        keywordMap.add("analytic");
        keywordMap.add("and");
        keywordMap.add("anti");
        keywordMap.add("api_version");
        keywordMap.add("array");
        keywordMap.add("as");
        keywordMap.add("asc");
        keywordMap.add("authorization");
        keywordMap.add("avro");
        keywordMap.add("between");
        keywordMap.add("bigint");
        keywordMap.add("binary");
        keywordMap.add("block_size");
        keywordMap.add("boolean");
        keywordMap.add("by");
        keywordMap.add("cached");
        keywordMap.add("case");
        keywordMap.add("cascade");
        keywordMap.add("cast");
        keywordMap.add("change");
        keywordMap.add("char");
        keywordMap.add("class");
        keywordMap.add("close_fn");
        keywordMap.add("column");
        keywordMap.add("columns");
        keywordMap.add("comment");
        keywordMap.add("compression");
        keywordMap.add("compute");
        keywordMap.add("copy");
        keywordMap.add("create");
        keywordMap.add("cross");
        keywordMap.add("current");
        keywordMap.add("data");
        keywordMap.add("database");
        keywordMap.add("databases");
        keywordMap.add("date");
        keywordMap.add("datetime");
        keywordMap.add("decimal");
        //keywordMap.add("default"); "default" can be database or table name
        keywordMap.add("delete");
        keywordMap.add("delimited");
        keywordMap.add("desc");
        keywordMap.add("describe");
        keywordMap.add("distinct");
        keywordMap.add("div");
        keywordMap.add("double");
        keywordMap.add("drop");
        keywordMap.add("else");
        keywordMap.add("encoding");
        keywordMap.add("end");
        keywordMap.add("escaped");
        keywordMap.add("exists");
        keywordMap.add("explain");
        keywordMap.add("extended");
        keywordMap.add("external");
        keywordMap.add("false");
        keywordMap.add("fields");
        keywordMap.add("fileformat");
        keywordMap.add("files");
        keywordMap.add("finalize_fn");
        keywordMap.add("first");
        keywordMap.add("float");
        keywordMap.add("following");
        keywordMap.add("for");
        keywordMap.add("format");
        keywordMap.add("formatted");
        keywordMap.add("from");
        keywordMap.add("full");
        keywordMap.add("function");
        keywordMap.add("functions");
        keywordMap.add("grant");
        keywordMap.add("group");
        keywordMap.add("hash");
        keywordMap.add("having");
        keywordMap.add("if");
        keywordMap.add("ilike");
        keywordMap.add("ignore");
        keywordMap.add("in");
        keywordMap.add("incremental");
        keywordMap.add("init_fn");
        keywordMap.add("inner");
        keywordMap.add("inpath");
        keywordMap.add("insert");
        keywordMap.add("int");
        keywordMap.add("integer");
        keywordMap.add("intermediate");
        keywordMap.add("interval");
        keywordMap.add("into");
        keywordMap.add("invalidate");
        keywordMap.add("iregexp");
        keywordMap.add("is");
        keywordMap.add("join");
        keywordMap.add("kudu");
        keywordMap.add("last");
        keywordMap.add("left");
        keywordMap.add("like");
        keywordMap.add("limit");
        keywordMap.add("lines");
        keywordMap.add("load");
        keywordMap.add("location");
        keywordMap.add("map");
        keywordMap.add("merge_fn");
        keywordMap.add("metadata");
        keywordMap.add("not");
        keywordMap.add("null");
        keywordMap.add("nulls");
        keywordMap.add("offset");
        keywordMap.add("on");
        keywordMap.add("||");
        keywordMap.add("or");
        keywordMap.add("orc");
        keywordMap.add("order");
        keywordMap.add("outer");
        keywordMap.add("over");
        keywordMap.add("overwrite");
        keywordMap.add("parquet");
        keywordMap.add("parquetfile");
        keywordMap.add("partition");
        keywordMap.add("partitioned");
        keywordMap.add("partitions");
        keywordMap.add("preceding");
        keywordMap.add("prepare_fn");
        keywordMap.add("primary");
        keywordMap.add("produced");
        keywordMap.add("purge");
        keywordMap.add("range");
        keywordMap.add("rcfile");
        keywordMap.add("real");
        keywordMap.add("recover");
        keywordMap.add("refresh");
        keywordMap.add("regexp");
        keywordMap.add("rename");
        keywordMap.add("repeatable");
        keywordMap.add("replace");
        keywordMap.add("replication");
        keywordMap.add("restrict");
        keywordMap.add("returns");
        keywordMap.add("revoke");
        keywordMap.add("right");
        keywordMap.add("rlike");
        keywordMap.add("role");
        keywordMap.add("roles");
        keywordMap.add("row");
        keywordMap.add("rows");
        keywordMap.add("schema");
        keywordMap.add("schemas");
        keywordMap.add("select");
        keywordMap.add("semi");
        keywordMap.add("sequencefile");
        keywordMap.add("serdeproperties");
        keywordMap.add("serialize_fn");
        keywordMap.add("set");
        keywordMap.add("show");
        keywordMap.add("smallint");
        keywordMap.add("sort");
        keywordMap.add("stats");
        keywordMap.add("stored");
        keywordMap.add("straight_join");
        keywordMap.add("string");
        keywordMap.add("struct");
        keywordMap.add("symbol");
        keywordMap.add("table");
        keywordMap.add("tables");
        keywordMap.add("tablesample");
        keywordMap.add("tblproperties");
        keywordMap.add("terminated");
        keywordMap.add("textfile");
        keywordMap.add("then");
        keywordMap.add("timestamp");
        keywordMap.add("tinyint");
        keywordMap.add("to");
        keywordMap.add("true");
        keywordMap.add("truncate");
        keywordMap.add("unbounded");
        keywordMap.add("uncached");
        keywordMap.add("union");
        keywordMap.add("unknown");
        keywordMap.add("update");
        keywordMap.add("update_fn");
        keywordMap.add("upsert");
        keywordMap.add("use");
        keywordMap.add("using");
        keywordMap.add("values");
        keywordMap.add("varchar");
        keywordMap.add("view");
        keywordMap.add("when");
        keywordMap.add("where");
        keywordMap.add("with");

        // Initilize tokenIdMap for error reporting
        tokenIdMap = new HashSet<>(keywordMap);

        // add non-keyword tokens. Please keep this in the same order as they are used in this
        // file.
        tokenIdMap.add("EOF");
        tokenIdMap.add("...");
        tokenIdMap.add(":");
        tokenIdMap.add(";");
        tokenIdMap.add("COMMA");
        tokenIdMap.add(".");
        tokenIdMap.add("*");
        tokenIdMap.add("(");
        tokenIdMap.add(")");
        tokenIdMap.add("[");
        tokenIdMap.add("]");
        tokenIdMap.add("/");
        tokenIdMap.add("%");
        tokenIdMap.add("+");
        tokenIdMap.add("-");
        tokenIdMap.add("&");
        tokenIdMap.add("|");
        tokenIdMap.add("^");
        tokenIdMap.add("~");
        tokenIdMap.add("=");
        tokenIdMap.add("!");
        tokenIdMap.add("<");
        tokenIdMap.add(">");
        tokenIdMap.add("UNMATCHED STRING LITERAL");
        tokenIdMap.add("!=");
        tokenIdMap.add("INTEGER LITERAL");
        tokenIdMap.add("NUMERIC OVERFLOW");
        tokenIdMap.add("DECIMAL LITERAL");
        tokenIdMap.add("EMPTY IDENTIFIER");
        tokenIdMap.add("IDENTIFIER");
        tokenIdMap.add("STRING LITERAL");
        tokenIdMap.add("COMMENTED_PLAN_HINT_START");
        tokenIdMap.add("COMMENTED_PLAN_HINT_END");
        tokenIdMap.add("Unexpected character");


        // For impala 3.0, reserved words = keywords + sql16ReservedWords - builtinFunctions
        // - whitelist
        // unused reserved words = reserved words - keywords. These words are reserved for
        // forward compatibility purposes.
        reservedWords = new HashSet<>(keywordMap);
        // Add SQL:2016 reserved words
        reservedWords.addAll(Arrays.asList(new String[] {
            "abs", "acos", "allocate", "any", "are", "array_agg", "array_max_cardinality",
            "asensitive", "asin", "asymmetric", "at", "atan", "atomic", "avg", "begin",
            "begin_frame", "begin_partition", "blob", "both", "call", "called", "cardinality",
            "cascaded", "ceil", "ceiling", "char_length", "character", "character_length",
            "check", "classifier", "clob", "close", "coalesce", "collate", "collect",
            "commit", "condition", "connect", "constraint", "contains", "convert", "copy",
            "corr", "corresponding", "cos", "cosh", "count", "covar_pop", "covar_samp",
            "cube", "cume_dist", "current_catalog", "current_date",
            "current_default_transform_group", "current_path", "current_path", "current_role",
            "current_role", "current_row", "current_schema", "current_time",
            "current_timestamp", "current_transform_group_for_type", "current_user", "cursor",
            "cycle", "day", "deallocate", "dec", "decfloat", "declare", "define",
            "dense_rank", "deref", "deterministic", "disconnect", "dynamic", "each",
            "element", "empty", "end-exec", "end_frame", "end_partition", "equals", "escape",
            "every", "except", "exec", "execute", "exp", "extract", "fetch", "filter",
            "first_value", "floor", "foreign", "frame_row", "free", "fusion", "get", "global",
            "grouping", "groups", "hold", "hour", "identity", "indicator", "initial", "inout",
            "insensitive", "integer", "intersect", "intersection", "json_array",
            "json_arrayagg", "json_exists", "json_object", "json_objectagg", "json_query",
            "json_table", "json_table_primitive", "json_value", "lag", "language", "large",
            "last_value", "lateral", "lead", "leading", "like_regex", "listagg", "ln",
            "local", "localtime", "localtimestamp", "log", "log10 ", "lower", "match",
            "match_number", "match_recognize", "matches", "max", "member", "merge", "method",
            "min", "minute", "mod", "modifies", "module", "month", "multiset", "national",
            "natural", "nchar", "nclob", "new", "no", "none", "normalize", "nth_value",
            "ntile", "nullif", "numeric", "occurrences_regex", "octet_length", "of", "old",
            "omit", "one", "only", "open", "out", "overlaps", "overlay", "parameter",
            "pattern", "per", "percent", "percent_rank", "percentile_cont", "percentile_disc",
            "period", "portion", "position", "position_regex", "power", "precedes",
            "precision", "prepare", "procedure", "ptf", "rank", "reads", "real", "recursive",
            "ref", "references", "referencing", "regr_avgx", "regr_avgy", "regr_count",
            "regr_intercept", "regr_r2", "regr_slope", "regr_sxx", "regr_sxy", "regr_syy",
            "release", "result", "return", "rollback", "rollup", "row_number", "running",
            "savepoint", "scope", "scroll", "search", "second", "seek", "sensitive",
            "session_user", "similar", "sin", "sinh", "skip", "some", "specific",
            "specifictype", "sql", "sqlexception", "sqlstate", "sqlwarning", "sqrt", "start",
            "static", "stddev_pop", "stddev_samp", "submultiset", "subset", "substring",
            "substring_regex", "succeeds", "sum", "symmetric", "system", "system_time",
            "system_user", "tan", "tanh", "time", "timezone_hour", "timezone_minute",
            "trailing", "translate", "translate_regex", "translation", "treat", "trigger",
            "trim", "trim_array", "uescape", "unique", "unknown", "unnest", "update  ",
            "upper", "user", "value", "value_of", "var_pop", "var_samp", "varbinary",
            "varying", "versioning", "whenever", "width_bucket", "window", "within",
            "without", "year"}));
        // TODO: Remove impala builtin function names. Need to find content of
        // BuiltinsDb.getInstance().getAllFunctions()
        //reservedWords.removeAll(BuiltinsDb.getInstance().getAllFunctions().keySet());

        // Remove whitelist words. These words might be heavily used in production, and
        // impala is unlikely to implement SQL features around these words in the near future.
        reservedWords.removeAll(Arrays.asList(new String[] {
            // time units
            "year", "month", "day", "hour", "minute", "second",
            "begin", "call", "check", "classifier", "close", "identity", "language",
            "localtime", "member", "module", "new", "nullif", "old", "open", "parameter",
            "period", "result", "return", "sql", "start", "system", "time", "user", "value"
        }));
    }

    static {
        init();
    }

    static boolean isReserved(String token) {
        return token != null && reservedWords.contains(token.toLowerCase());
    }
}
