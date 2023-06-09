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

// Generated from com/netease/arctic/spark/sql/parser/ArcticExtendSparkSql.g4 by ANTLR 4.7
package com.netease.arctic.spark.sql.parser;

import org.antlr.v4.runtime.FailedPredicateException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ArcticExtendSparkSqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, KEY=12, MIGRATE=13, ADD=14, AFTER=15, ALL=16, ALTER=17, 
		ANALYZE=18, AND=19, ANTI=20, ANY=21, ARCHIVE=22, ARRAY=23, AS=24, ASC=25, 
		AT=26, AUTHORIZATION=27, BETWEEN=28, BOTH=29, BUCKET=30, BUCKETS=31, BY=32, 
		CACHE=33, CASCADE=34, CASE=35, CAST=36, CHANGE=37, CHECK=38, CLEAR=39, 
		CLUSTER=40, CLUSTERED=41, CODEGEN=42, COLLATE=43, COLLECTION=44, COLUMN=45, 
		COLUMNS=46, COMMENT=47, COMMIT=48, COMPACT=49, COMPACTIONS=50, COMPUTE=51, 
		CONCATENATE=52, CONSTRAINT=53, COST=54, CREATE=55, CROSS=56, CUBE=57, 
		CURRENT=58, CURRENT_DATE=59, CURRENT_TIME=60, CURRENT_TIMESTAMP=61, CURRENT_USER=62, 
		DAY=63, DATA=64, DATABASE=65, DATABASES=66, DBPROPERTIES=67, DEFINED=68, 
		DELETE=69, DELIMITED=70, DESC=71, DESCRIBE=72, DFS=73, DIRECTORIES=74, 
		DIRECTORY=75, DISTINCT=76, DISTRIBUTE=77, DIV=78, DROP=79, ELSE=80, END=81, 
		ESCAPE=82, ESCAPED=83, EXCEPT=84, EXCHANGE=85, EXISTS=86, EXPLAIN=87, 
		EXPORT=88, EXTENDED=89, EXTERNAL=90, EXTRACT=91, FALSE=92, FETCH=93, FIELDS=94, 
		FILTER=95, FILEFORMAT=96, FIRST=97, FOLLOWING=98, FOR=99, FOREIGN=100, 
		FORMAT=101, FORMATTED=102, FROM=103, FULL=104, FUNCTION=105, FUNCTIONS=106, 
		GLOBAL=107, GRANT=108, GROUP=109, GROUPING=110, HAVING=111, HOUR=112, 
		IF=113, IGNORE=114, IMPORT=115, IN=116, INDEX=117, INDEXES=118, INNER=119, 
		INPATH=120, INPUTFORMAT=121, INSERT=122, INTERSECT=123, INTERVAL=124, 
		INTO=125, IS=126, ITEMS=127, JOIN=128, KEYS=129, LAST=130, LATERAL=131, 
		LAZY=132, LEADING=133, LEFT=134, LIKE=135, LIMIT=136, LINES=137, LIST=138, 
		LOAD=139, LOCAL=140, LOCATION=141, LOCK=142, LOCKS=143, LOGICAL=144, MACRO=145, 
		MAP=146, MATCHED=147, MERGE=148, MINUTE=149, MONTH=150, MSCK=151, NAMESPACE=152, 
		NAMESPACES=153, NATURAL=154, NO=155, NOT=156, NULL=157, NULLS=158, OF=159, 
		ON=160, ONLY=161, OPTION=162, OPTIONS=163, OR=164, ORDER=165, OUT=166, 
		OUTER=167, OUTPUTFORMAT=168, OVER=169, OVERLAPS=170, OVERLAY=171, OVERWRITE=172, 
		PARTITION=173, PARTITIONED=174, PARTITIONS=175, PERCENTLIT=176, PIVOT=177, 
		PLACING=178, POSITION=179, PRECEDING=180, PRIMARY=181, PRINCIPALS=182, 
		PROPERTIES=183, PURGE=184, QUERY=185, RANGE=186, RECORDREADER=187, RECORDWRITER=188, 
		RECOVER=189, REDUCE=190, REFERENCES=191, REFRESH=192, RENAME=193, REPAIR=194, 
		REPLACE=195, RESET=196, RESPECT=197, RESTRICT=198, REVOKE=199, RIGHT=200, 
		RLIKE=201, ROLE=202, ROLES=203, ROLLBACK=204, ROLLUP=205, ROW=206, ROWS=207, 
		SECOND=208, SCHEMA=209, SELECT=210, SEMI=211, SEPARATED=212, SERDE=213, 
		SERDEPROPERTIES=214, SESSION_USER=215, SET=216, SETMINUS=217, SETS=218, 
		SHOW=219, SKEWED=220, SOME=221, SORT=222, SORTED=223, START=224, STATISTICS=225, 
		STORED=226, STRATIFY=227, STRUCT=228, SUBSTR=229, SUBSTRING=230, SYNC=231, 
		TABLE=232, TABLES=233, TABLESAMPLE=234, TBLPROPERTIES=235, TEMPORARY=236, 
		TERMINATED=237, THEN=238, TIME=239, TO=240, TOUCH=241, TRAILING=242, TRANSACTION=243, 
		TRANSACTIONS=244, TRANSFORM=245, TRIM=246, TRUE=247, TRUNCATE=248, TRY_CAST=249, 
		TYPE=250, UNARCHIVE=251, UNBOUNDED=252, UNCACHE=253, UNION=254, UNIQUE=255, 
		UNKNOWN=256, UNLOCK=257, UNSET=258, UPDATE=259, USE=260, USER=261, USING=262, 
		VALUES=263, VIEW=264, VIEWS=265, WHEN=266, WHERE=267, WINDOW=268, WITH=269, 
		YEAR=270, ZONE=271, EQ=272, NSEQ=273, NEQ=274, NEQJ=275, LT=276, LTE=277, 
		GT=278, GTE=279, PLUS=280, MINUS=281, ASTERISK=282, SLASH=283, PERCENT=284, 
		TILDE=285, AMPERSAND=286, PIPE=287, CONCAT_PIPE=288, HAT=289, STRING=290, 
		BIGINT_LITERAL=291, SMALLINT_LITERAL=292, TINYINT_LITERAL=293, INTEGER_VALUE=294, 
		EXPONENT_VALUE=295, DECIMAL_VALUE=296, FLOAT_LITERAL=297, DOUBLE_LITERAL=298, 
		BIGDECIMAL_LITERAL=299, IDENTIFIER=300, BACKQUOTED_IDENTIFIER=301, SIMPLE_COMMENT=302, 
		BRACKETED_COMMENT=303, WS=304, UNRECOGNIZED=305;
	public static final int
		RULE_arcticCommand = 0, RULE_arcticStatement = 1, RULE_createTableWithPrimaryKey = 2, 
		RULE_primarySpec = 3, RULE_colListAndPk = 4, RULE_singleStatement = 5, 
		RULE_singleExpression = 6, RULE_singleTableIdentifier = 7, RULE_singleMultipartIdentifier = 8, 
		RULE_singleFunctionIdentifier = 9, RULE_singleDataType = 10, RULE_singleTableSchema = 11, 
		RULE_statement = 12, RULE_configKey = 13, RULE_configValue = 14, RULE_unsupportedHiveNativeCommands = 15, 
		RULE_createTableHeader = 16, RULE_replaceTableHeader = 17, RULE_bucketSpec = 18, 
		RULE_skewSpec = 19, RULE_locationSpec = 20, RULE_commentSpec = 21, RULE_query = 22, 
		RULE_insertInto = 23, RULE_partitionSpecLocation = 24, RULE_partitionSpec = 25, 
		RULE_partitionVal = 26, RULE_namespace = 27, RULE_describeFuncName = 28, 
		RULE_describeColName = 29, RULE_ctes = 30, RULE_namedQuery = 31, RULE_tableProvider = 32, 
		RULE_createTableClauses = 33, RULE_tablePropertyList = 34, RULE_tableProperty = 35, 
		RULE_tablePropertyKey = 36, RULE_tablePropertyValue = 37, RULE_constantList = 38, 
		RULE_nestedConstantList = 39, RULE_createFileFormat = 40, RULE_fileFormat = 41, 
		RULE_storageHandler = 42, RULE_resource = 43, RULE_dmlStatementNoWith = 44, 
		RULE_queryOrganization = 45, RULE_multiInsertQueryBody = 46, RULE_queryTerm = 47, 
		RULE_queryPrimary = 48, RULE_sortItem = 49, RULE_fromStatement = 50, RULE_fromStatementBody = 51, 
		RULE_querySpecification = 52, RULE_transformClause = 53, RULE_selectClause = 54, 
		RULE_setClause = 55, RULE_matchedClause = 56, RULE_notMatchedClause = 57, 
		RULE_matchedAction = 58, RULE_notMatchedAction = 59, RULE_assignmentList = 60, 
		RULE_assignment = 61, RULE_whereClause = 62, RULE_havingClause = 63, RULE_hint = 64, 
		RULE_hintStatement = 65, RULE_fromClause = 66, RULE_aggregationClause = 67, 
		RULE_groupByClause = 68, RULE_groupingAnalytics = 69, RULE_groupingElement = 70, 
		RULE_groupingSet = 71, RULE_pivotClause = 72, RULE_pivotColumn = 73, RULE_pivotValue = 74, 
		RULE_lateralView = 75, RULE_setQuantifier = 76, RULE_relation = 77, RULE_joinRelation = 78, 
		RULE_joinType = 79, RULE_joinCriteria = 80, RULE_sample = 81, RULE_sampleMethod = 82, 
		RULE_identifierList = 83, RULE_identifierSeq = 84, RULE_orderedIdentifierList = 85, 
		RULE_orderedIdentifier = 86, RULE_identifierCommentList = 87, RULE_identifierComment = 88, 
		RULE_relationPrimary = 89, RULE_inlineTable = 90, RULE_functionTable = 91, 
		RULE_tableAlias = 92, RULE_rowFormat = 93, RULE_multipartIdentifierList = 94, 
		RULE_multipartIdentifier = 95, RULE_tableIdentifier = 96, RULE_functionIdentifier = 97, 
		RULE_namedExpression = 98, RULE_namedExpressionSeq = 99, RULE_partitionFieldList = 100, 
		RULE_partitionField = 101, RULE_transform = 102, RULE_transformArgument = 103, 
		RULE_expression = 104, RULE_expressionSeq = 105, RULE_booleanExpression = 106, 
		RULE_predicate = 107, RULE_valueExpression = 108, RULE_primaryExpression = 109, 
		RULE_constant = 110, RULE_comparisonOperator = 111, RULE_arithmeticOperator = 112, 
		RULE_predicateOperator = 113, RULE_booleanValue = 114, RULE_interval = 115, 
		RULE_errorCapturingMultiUnitsInterval = 116, RULE_multiUnitsInterval = 117, 
		RULE_errorCapturingUnitToUnitInterval = 118, RULE_unitToUnitInterval = 119, 
		RULE_intervalValue = 120, RULE_colPosition = 121, RULE_dataType = 122, 
		RULE_qualifiedColTypeWithPositionList = 123, RULE_qualifiedColTypeWithPosition = 124, 
		RULE_colTypeList = 125, RULE_colType = 126, RULE_complexColTypeList = 127, 
		RULE_complexColType = 128, RULE_whenClause = 129, RULE_windowClause = 130, 
		RULE_namedWindow = 131, RULE_windowSpec = 132, RULE_windowFrame = 133, 
		RULE_frameBound = 134, RULE_qualifiedNameList = 135, RULE_functionName = 136, 
		RULE_qualifiedName = 137, RULE_errorCapturingIdentifier = 138, RULE_errorCapturingIdentifierExtra = 139, 
		RULE_identifier = 140, RULE_strictIdentifier = 141, RULE_quotedIdentifier = 142, 
		RULE_number = 143, RULE_alterColumnAction = 144, RULE_ansiNonReserved = 145, 
		RULE_strictNonReserved = 146, RULE_nonReserved = 147;
	private static String[] makeRuleNames() {
		return new String[] {
			"arcticCommand", "arcticStatement", "createTableWithPrimaryKey", "primarySpec", 
			"colListAndPk", "singleStatement", "singleExpression", "singleTableIdentifier", 
			"singleMultipartIdentifier", "singleFunctionIdentifier", "singleDataType", 
			"singleTableSchema", "statement", "configKey", "configValue", "unsupportedHiveNativeCommands", 
			"createTableHeader", "replaceTableHeader", "bucketSpec", "skewSpec", 
			"locationSpec", "commentSpec", "query", "insertInto", "partitionSpecLocation", 
			"partitionSpec", "partitionVal", "namespace", "describeFuncName", "describeColName", 
			"ctes", "namedQuery", "tableProvider", "createTableClauses", "tablePropertyList", 
			"tableProperty", "tablePropertyKey", "tablePropertyValue", "constantList", 
			"nestedConstantList", "createFileFormat", "fileFormat", "storageHandler", 
			"resource", "dmlStatementNoWith", "queryOrganization", "multiInsertQueryBody", 
			"queryTerm", "queryPrimary", "sortItem", "fromStatement", "fromStatementBody", 
			"querySpecification", "transformClause", "selectClause", "setClause", 
			"matchedClause", "notMatchedClause", "matchedAction", "notMatchedAction", 
			"assignmentList", "assignment", "whereClause", "havingClause", "hint", 
			"hintStatement", "fromClause", "aggregationClause", "groupByClause", 
			"groupingAnalytics", "groupingElement", "groupingSet", "pivotClause", 
			"pivotColumn", "pivotValue", "lateralView", "setQuantifier", "relation", 
			"joinRelation", "joinType", "joinCriteria", "sample", "sampleMethod", 
			"identifierList", "identifierSeq", "orderedIdentifierList", "orderedIdentifier", 
			"identifierCommentList", "identifierComment", "relationPrimary", "inlineTable", 
			"functionTable", "tableAlias", "rowFormat", "multipartIdentifierList", 
			"multipartIdentifier", "tableIdentifier", "functionIdentifier", "namedExpression", 
			"namedExpressionSeq", "partitionFieldList", "partitionField", "transform", 
			"transformArgument", "expression", "expressionSeq", "booleanExpression", 
			"predicate", "valueExpression", "primaryExpression", "constant", "comparisonOperator", 
			"arithmeticOperator", "predicateOperator", "booleanValue", "interval", 
			"errorCapturingMultiUnitsInterval", "multiUnitsInterval", "errorCapturingUnitToUnitInterval", 
			"unitToUnitInterval", "intervalValue", "colPosition", "dataType", "qualifiedColTypeWithPositionList", 
			"qualifiedColTypeWithPosition", "colTypeList", "colType", "complexColTypeList", 
			"complexColType", "whenClause", "windowClause", "namedWindow", "windowSpec", 
			"windowFrame", "frameBound", "qualifiedNameList", "functionName", "qualifiedName", 
			"errorCapturingIdentifier", "errorCapturingIdentifierExtra", "identifier", 
			"strictIdentifier", "quotedIdentifier", "number", "alterColumnAction", 
			"ansiNonReserved", "strictNonReserved", "nonReserved"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "','", "')'", "';'", "'.'", "'/*+'", "'*/'", "'->'", "'['", 
			"']'", "':'", "'KEY'", "'MIGRATE'", "'ADD'", "'AFTER'", "'ALL'", "'ALTER'", 
			"'ANALYZE'", "'AND'", "'ANTI'", "'ANY'", "'ARCHIVE'", "'ARRAY'", "'AS'", 
			"'ASC'", "'AT'", "'AUTHORIZATION'", "'BETWEEN'", "'BOTH'", "'BUCKET'", 
			"'BUCKETS'", "'BY'", "'CACHE'", "'CASCADE'", "'CASE'", "'CAST'", "'CHANGE'", 
			"'CHECK'", "'CLEAR'", "'CLUSTER'", "'CLUSTERED'", "'CODEGEN'", "'COLLATE'", 
			"'COLLECTION'", "'COLUMN'", "'COLUMNS'", "'COMMENT'", "'COMMIT'", "'COMPACT'", 
			"'COMPACTIONS'", "'COMPUTE'", "'CONCATENATE'", "'CONSTRAINT'", "'COST'", 
			"'CREATE'", "'CROSS'", "'CUBE'", "'CURRENT'", "'CURRENT_DATE'", "'CURRENT_TIME'", 
			"'CURRENT_TIMESTAMP'", "'CURRENT_USER'", "'DAY'", "'DATA'", "'DATABASE'", 
			null, "'DBPROPERTIES'", "'DEFINED'", "'DELETE'", "'DELIMITED'", "'DESC'", 
			"'DESCRIBE'", "'DFS'", "'DIRECTORIES'", "'DIRECTORY'", "'DISTINCT'", 
			"'DISTRIBUTE'", "'DIV'", "'DROP'", "'ELSE'", "'END'", "'ESCAPE'", "'ESCAPED'", 
			"'EXCEPT'", "'EXCHANGE'", "'EXISTS'", "'EXPLAIN'", "'EXPORT'", "'EXTENDED'", 
			"'EXTERNAL'", "'EXTRACT'", "'FALSE'", "'FETCH'", "'FIELDS'", "'FILTER'", 
			"'FILEFORMAT'", "'FIRST'", "'FOLLOWING'", "'FOR'", "'FOREIGN'", "'FORMAT'", 
			"'FORMATTED'", "'FROM'", "'FULL'", "'FUNCTION'", "'FUNCTIONS'", "'GLOBAL'", 
			"'GRANT'", "'GROUP'", "'GROUPING'", "'HAVING'", "'HOUR'", "'IF'", "'IGNORE'", 
			"'IMPORT'", "'IN'", "'INDEX'", "'INDEXES'", "'INNER'", "'INPATH'", "'INPUTFORMAT'", 
			"'INSERT'", "'INTERSECT'", "'INTERVAL'", "'INTO'", "'IS'", "'ITEMS'", 
			"'JOIN'", "'KEYS'", "'LAST'", "'LATERAL'", "'LAZY'", "'LEADING'", "'LEFT'", 
			"'LIKE'", "'LIMIT'", "'LINES'", "'LIST'", "'LOAD'", "'LOCAL'", "'LOCATION'", 
			"'LOCK'", "'LOCKS'", "'LOGICAL'", "'MACRO'", "'MAP'", "'MATCHED'", "'MERGE'", 
			"'MINUTE'", "'MONTH'", "'MSCK'", "'NAMESPACE'", "'NAMESPACES'", "'NATURAL'", 
			"'NO'", null, "'NULL'", "'NULLS'", "'OF'", "'ON'", "'ONLY'", "'OPTION'", 
			"'OPTIONS'", "'OR'", "'ORDER'", "'OUT'", "'OUTER'", "'OUTPUTFORMAT'", 
			"'OVER'", "'OVERLAPS'", "'OVERLAY'", "'OVERWRITE'", "'PARTITION'", "'PARTITIONED'", 
			"'PARTITIONS'", "'PERCENT'", "'PIVOT'", "'PLACING'", "'POSITION'", "'PRECEDING'", 
			"'PRIMARY'", "'PRINCIPALS'", "'PROPERTIES'", "'PURGE'", "'QUERY'", "'RANGE'", 
			"'RECORDREADER'", "'RECORDWRITER'", "'RECOVER'", "'REDUCE'", "'REFERENCES'", 
			"'REFRESH'", "'RENAME'", "'REPAIR'", "'REPLACE'", "'RESET'", "'RESPECT'", 
			"'RESTRICT'", "'REVOKE'", "'RIGHT'", null, "'ROLE'", "'ROLES'", "'ROLLBACK'", 
			"'ROLLUP'", "'ROW'", "'ROWS'", "'SECOND'", "'SCHEMA'", "'SELECT'", "'SEMI'", 
			"'SEPARATED'", "'SERDE'", "'SERDEPROPERTIES'", "'SESSION_USER'", "'SET'", 
			"'MINUS'", "'SETS'", "'SHOW'", "'SKEWED'", "'SOME'", "'SORT'", "'SORTED'", 
			"'START'", "'STATISTICS'", "'STORED'", "'STRATIFY'", "'STRUCT'", "'SUBSTR'", 
			"'SUBSTRING'", "'SYNC'", "'TABLE'", "'TABLES'", "'TABLESAMPLE'", "'TBLPROPERTIES'", 
			null, "'TERMINATED'", "'THEN'", "'TIME'", "'TO'", "'TOUCH'", "'TRAILING'", 
			"'TRANSACTION'", "'TRANSACTIONS'", "'TRANSFORM'", "'TRIM'", "'TRUE'", 
			"'TRUNCATE'", "'TRY_CAST'", "'TYPE'", "'UNARCHIVE'", "'UNBOUNDED'", "'UNCACHE'", 
			"'UNION'", "'UNIQUE'", "'UNKNOWN'", "'UNLOCK'", "'UNSET'", "'UPDATE'", 
			"'USE'", "'USER'", "'USING'", "'VALUES'", "'VIEW'", "'VIEWS'", "'WHEN'", 
			"'WHERE'", "'WINDOW'", "'WITH'", "'YEAR'", "'ZONE'", null, "'<=>'", "'<>'", 
			"'!='", "'<'", null, "'>'", null, "'+'", "'-'", "'*'", "'/'", "'%'", 
			"'~'", "'&'", "'|'", "'||'", "'^'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"KEY", "MIGRATE", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "ANTI", 
			"ANY", "ARCHIVE", "ARRAY", "AS", "ASC", "AT", "AUTHORIZATION", "BETWEEN", 
			"BOTH", "BUCKET", "BUCKETS", "BY", "CACHE", "CASCADE", "CASE", "CAST", 
			"CHANGE", "CHECK", "CLEAR", "CLUSTER", "CLUSTERED", "CODEGEN", "COLLATE", 
			"COLLECTION", "COLUMN", "COLUMNS", "COMMENT", "COMMIT", "COMPACT", "COMPACTIONS", 
			"COMPUTE", "CONCATENATE", "CONSTRAINT", "COST", "CREATE", "CROSS", "CUBE", 
			"CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", 
			"DAY", "DATA", "DATABASE", "DATABASES", "DBPROPERTIES", "DEFINED", "DELETE", 
			"DELIMITED", "DESC", "DESCRIBE", "DFS", "DIRECTORIES", "DIRECTORY", "DISTINCT", 
			"DISTRIBUTE", "DIV", "DROP", "ELSE", "END", "ESCAPE", "ESCAPED", "EXCEPT", 
			"EXCHANGE", "EXISTS", "EXPLAIN", "EXPORT", "EXTENDED", "EXTERNAL", "EXTRACT", 
			"FALSE", "FETCH", "FIELDS", "FILTER", "FILEFORMAT", "FIRST", "FOLLOWING", 
			"FOR", "FOREIGN", "FORMAT", "FORMATTED", "FROM", "FULL", "FUNCTION", 
			"FUNCTIONS", "GLOBAL", "GRANT", "GROUP", "GROUPING", "HAVING", "HOUR", 
			"IF", "IGNORE", "IMPORT", "IN", "INDEX", "INDEXES", "INNER", "INPATH", 
			"INPUTFORMAT", "INSERT", "INTERSECT", "INTERVAL", "INTO", "IS", "ITEMS", 
			"JOIN", "KEYS", "LAST", "LATERAL", "LAZY", "LEADING", "LEFT", "LIKE", 
			"LIMIT", "LINES", "LIST", "LOAD", "LOCAL", "LOCATION", "LOCK", "LOCKS", 
			"LOGICAL", "MACRO", "MAP", "MATCHED", "MERGE", "MINUTE", "MONTH", "MSCK", 
			"NAMESPACE", "NAMESPACES", "NATURAL", "NO", "NOT", "NULL", "NULLS", "OF", 
			"ON", "ONLY", "OPTION", "OPTIONS", "OR", "ORDER", "OUT", "OUTER", "OUTPUTFORMAT", 
			"OVER", "OVERLAPS", "OVERLAY", "OVERWRITE", "PARTITION", "PARTITIONED", 
			"PARTITIONS", "PERCENTLIT", "PIVOT", "PLACING", "POSITION", "PRECEDING", 
			"PRIMARY", "PRINCIPALS", "PROPERTIES", "PURGE", "QUERY", "RANGE", "RECORDREADER", 
			"RECORDWRITER", "RECOVER", "REDUCE", "REFERENCES", "REFRESH", "RENAME", 
			"REPAIR", "REPLACE", "RESET", "RESPECT", "RESTRICT", "REVOKE", "RIGHT", 
			"RLIKE", "ROLE", "ROLES", "ROLLBACK", "ROLLUP", "ROW", "ROWS", "SECOND", 
			"SCHEMA", "SELECT", "SEMI", "SEPARATED", "SERDE", "SERDEPROPERTIES", 
			"SESSION_USER", "SET", "SETMINUS", "SETS", "SHOW", "SKEWED", "SOME", 
			"SORT", "SORTED", "START", "STATISTICS", "STORED", "STRATIFY", "STRUCT", 
			"SUBSTR", "SUBSTRING", "SYNC", "TABLE", "TABLES", "TABLESAMPLE", "TBLPROPERTIES", 
			"TEMPORARY", "TERMINATED", "THEN", "TIME", "TO", "TOUCH", "TRAILING", 
			"TRANSACTION", "TRANSACTIONS", "TRANSFORM", "TRIM", "TRUE", "TRUNCATE", 
			"TRY_CAST", "TYPE", "UNARCHIVE", "UNBOUNDED", "UNCACHE", "UNION", "UNIQUE", 
			"UNKNOWN", "UNLOCK", "UNSET", "UPDATE", "USE", "USER", "USING", "VALUES", 
			"VIEW", "VIEWS", "WHEN", "WHERE", "WINDOW", "WITH", "YEAR", "ZONE", "EQ", 
			"NSEQ", "NEQ", "NEQJ", "LT", "LTE", "GT", "GTE", "PLUS", "MINUS", "ASTERISK", 
			"SLASH", "PERCENT", "TILDE", "AMPERSAND", "PIPE", "CONCAT_PIPE", "HAT", 
			"STRING", "BIGINT_LITERAL", "SMALLINT_LITERAL", "TINYINT_LITERAL", "INTEGER_VALUE", 
			"EXPONENT_VALUE", "DECIMAL_VALUE", "FLOAT_LITERAL", "DOUBLE_LITERAL", 
			"BIGDECIMAL_LITERAL", "IDENTIFIER", "BACKQUOTED_IDENTIFIER", "SIMPLE_COMMENT", 
			"BRACKETED_COMMENT", "WS", "UNRECOGNIZED"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "ArcticExtendSparkSql.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


	  /**
	   * When false, INTERSECT is given the greater precedence over the other set
	   * operations (UNION, EXCEPT and MINUS) as per the SQL standard.
	   */
	  public boolean legacy_setops_precedence_enabled = false;

	  /**
	   * When false, a literal with an exponent would be converted into
	   * double type rather than decimal type.
	   */
	  public boolean legacy_exponent_literal_as_decimal_enabled = false;

	  /**
	   * When true, the behavior of keywords follows ANSI SQL standard.
	   */
	  public boolean SQL_standard_keyword_behavior = false;

	public ArcticExtendSparkSqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ArcticCommandContext extends ParserRuleContext {
		public ArcticStatementContext arcticStatement() {
			return getRuleContext(ArcticStatementContext.class,0);
		}
		public ArcticCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arcticCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterArcticCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitArcticCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitArcticCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArcticCommandContext arcticCommand() throws RecognitionException {
		ArcticCommandContext _localctx = new ArcticCommandContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_arcticCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			arcticStatement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArcticStatementContext extends ParserRuleContext {
		public ArcticStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arcticStatement; }
	 
		public ArcticStatementContext() { }
		public void copyFrom(ArcticStatementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class CreateTableWithPkContext extends ArcticStatementContext {
		public CreateTableWithPrimaryKeyContext createTableWithPrimaryKey() {
			return getRuleContext(CreateTableWithPrimaryKeyContext.class,0);
		}
		public CreateTableWithPkContext(ArcticStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTableWithPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTableWithPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTableWithPk(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArcticStatementContext arcticStatement() throws RecognitionException {
		ArcticStatementContext _localctx = new ArcticStatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_arcticStatement);
		try {
			_localctx = new CreateTableWithPkContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			createTableWithPrimaryKey();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreateTableWithPrimaryKeyContext extends ParserRuleContext {
		public CreateTableHeaderContext createTableHeader() {
			return getRuleContext(CreateTableHeaderContext.class,0);
		}
		public ColListAndPkContext colListAndPk() {
			return getRuleContext(ColListAndPkContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public CreateTableClausesContext createTableClauses() {
			return getRuleContext(CreateTableClausesContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public CreateTableWithPrimaryKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableWithPrimaryKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTableWithPrimaryKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTableWithPrimaryKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTableWithPrimaryKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableWithPrimaryKeyContext createTableWithPrimaryKey() throws RecognitionException {
		CreateTableWithPrimaryKeyContext _localctx = new CreateTableWithPrimaryKeyContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_createTableWithPrimaryKey);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(300);
			createTableHeader();
			setState(301);
			colListAndPk();
			setState(302);
			tableProvider();
			setState(303);
			createTableClauses();
			setState(308);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0 || _la==AS || _la==FROM || _la==MAP || ((((_la - 190)) & ~0x3f) == 0 && ((1L << (_la - 190)) & ((1L << (REDUCE - 190)) | (1L << (SELECT - 190)) | (1L << (TABLE - 190)))) != 0) || _la==VALUES || _la==WITH) {
				{
				setState(305);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(304);
					match(AS);
					}
				}

				setState(307);
				query();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrimarySpecContext extends ParserRuleContext {
		public TerminalNode PRIMARY() { return getToken(ArcticExtendSparkSqlParser.PRIMARY, 0); }
		public TerminalNode KEY() { return getToken(ArcticExtendSparkSqlParser.KEY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public PrimarySpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primarySpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPrimarySpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPrimarySpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPrimarySpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimarySpecContext primarySpec() throws RecognitionException {
		PrimarySpecContext _localctx = new PrimarySpecContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_primarySpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			match(PRIMARY);
			setState(311);
			match(KEY);
			setState(312);
			identifierList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ColListAndPkContext extends ParserRuleContext {
		public ColListAndPkContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colListAndPk; }
	 
		public ColListAndPkContext() { }
		public void copyFrom(ColListAndPkContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ColListOnlyPkContext extends ColListAndPkContext {
		public PrimarySpecContext primarySpec() {
			return getRuleContext(PrimarySpecContext.class,0);
		}
		public ColListOnlyPkContext(ColListAndPkContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterColListOnlyPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitColListOnlyPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitColListOnlyPk(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ColListWithPkContext extends ColListAndPkContext {
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public PrimarySpecContext primarySpec() {
			return getRuleContext(PrimarySpecContext.class,0);
		}
		public ColListWithPkContext(ColListAndPkContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterColListWithPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitColListWithPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitColListWithPk(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColListAndPkContext colListAndPk() throws RecognitionException {
		ColListAndPkContext _localctx = new ColListAndPkContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_colListAndPk);
		int _la;
		try {
			setState(323);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				_localctx = new ColListWithPkContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(314);
				match(T__0);
				setState(315);
				colTypeList();
				setState(318);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(316);
					match(T__1);
					setState(317);
					primarySpec();
					}
				}

				setState(320);
				match(T__2);
				}
				break;
			case PRIMARY:
				_localctx = new ColListOnlyPkContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(322);
				primarySpec();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleStatementContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleStatementContext singleStatement() throws RecognitionException {
		SingleStatementContext _localctx = new SingleStatementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_singleStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			statement();
			setState(329);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(326);
				match(T__3);
				}
				}
				setState(331);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(332);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleExpressionContext extends ParserRuleContext {
		public NamedExpressionContext namedExpression() {
			return getRuleContext(NamedExpressionContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleExpressionContext singleExpression() throws RecognitionException {
		SingleExpressionContext _localctx = new SingleExpressionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_singleExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(334);
			namedExpression();
			setState(335);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleTableIdentifierContext extends ParserRuleContext {
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleTableIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleTableIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleTableIdentifierContext singleTableIdentifier() throws RecognitionException {
		SingleTableIdentifierContext _localctx = new SingleTableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_singleTableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(337);
			tableIdentifier();
			setState(338);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleMultipartIdentifierContext extends ParserRuleContext {
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleMultipartIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleMultipartIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleMultipartIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleMultipartIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleMultipartIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleMultipartIdentifierContext singleMultipartIdentifier() throws RecognitionException {
		SingleMultipartIdentifierContext _localctx = new SingleMultipartIdentifierContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_singleMultipartIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			multipartIdentifier();
			setState(341);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleFunctionIdentifierContext extends ParserRuleContext {
		public FunctionIdentifierContext functionIdentifier() {
			return getRuleContext(FunctionIdentifierContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleFunctionIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleFunctionIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleFunctionIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleFunctionIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleFunctionIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleFunctionIdentifierContext singleFunctionIdentifier() throws RecognitionException {
		SingleFunctionIdentifierContext _localctx = new SingleFunctionIdentifierContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_singleFunctionIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(343);
			functionIdentifier();
			setState(344);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleDataTypeContext extends ParserRuleContext {
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleDataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleDataType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleDataTypeContext singleDataType() throws RecognitionException {
		SingleDataTypeContext _localctx = new SingleDataTypeContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_singleDataType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			dataType();
			setState(347);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SingleTableSchemaContext extends ParserRuleContext {
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticExtendSparkSqlParser.EOF, 0); }
		public SingleTableSchemaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleTableSchema; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleTableSchema(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleTableSchema(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleTableSchema(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleTableSchemaContext singleTableSchema() throws RecognitionException {
		SingleTableSchemaContext _localctx = new SingleTableSchemaContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_singleTableSchema);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(349);
			colTypeList();
			setState(350);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
	 
		public StatementContext() { }
		public void copyFrom(StatementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ExplainContext extends StatementContext {
		public TerminalNode EXPLAIN() { return getToken(ArcticExtendSparkSqlParser.EXPLAIN, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode LOGICAL() { return getToken(ArcticExtendSparkSqlParser.LOGICAL, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticExtendSparkSqlParser.FORMATTED, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticExtendSparkSqlParser.CODEGEN, 0); }
		public TerminalNode COST() { return getToken(ArcticExtendSparkSqlParser.COST, 0); }
		public ExplainContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterExplain(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitExplain(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitExplain(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ResetConfigurationContext extends StatementContext {
		public TerminalNode RESET() { return getToken(ArcticExtendSparkSqlParser.RESET, 0); }
		public ResetConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterResetConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitResetConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitResetConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AlterViewQueryContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public AlterViewQueryContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAlterViewQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAlterViewQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAlterViewQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UseContext extends StatementContext {
		public TerminalNode USE() { return getToken(ArcticExtendSparkSqlParser.USE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode NAMESPACE() { return getToken(ArcticExtendSparkSqlParser.NAMESPACE, 0); }
		public UseContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUse(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropNamespaceContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticExtendSparkSqlParser.RESTRICT, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticExtendSparkSqlParser.CASCADE, 0); }
		public DropNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDropNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDropNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDropNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateTempViewUsingContext extends StatementContext {
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticExtendSparkSqlParser.GLOBAL, 0); }
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TerminalNode OPTIONS() { return getToken(ArcticExtendSparkSqlParser.OPTIONS, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public CreateTempViewUsingContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTempViewUsing(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTempViewUsing(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTempViewUsing(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RenameTableContext extends StatementContext {
		public MultipartIdentifierContext from;
		public MultipartIdentifierContext to;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode RENAME() { return getToken(ArcticExtendSparkSqlParser.RENAME, 0); }
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public RenameTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRenameTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRenameTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRenameTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FailNativeCommandContext extends StatementContext {
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode ROLE() { return getToken(ArcticExtendSparkSqlParser.ROLE, 0); }
		public UnsupportedHiveNativeCommandsContext unsupportedHiveNativeCommands() {
			return getRuleContext(UnsupportedHiveNativeCommandsContext.class,0);
		}
		public FailNativeCommandContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFailNativeCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFailNativeCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFailNativeCommand(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ClearCacheContext extends StatementContext {
		public TerminalNode CLEAR() { return getToken(ArcticExtendSparkSqlParser.CLEAR, 0); }
		public TerminalNode CACHE() { return getToken(ArcticExtendSparkSqlParser.CACHE, 0); }
		public ClearCacheContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterClearCache(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitClearCache(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitClearCache(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropViewContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public DropViewContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDropView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDropView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDropView(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowTablesContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode TABLES() { return getToken(ArcticExtendSparkSqlParser.TABLES, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public ShowTablesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowTables(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowTables(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowTables(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RecoverPartitionsContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode RECOVER() { return getToken(ArcticExtendSparkSqlParser.RECOVER, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticExtendSparkSqlParser.PARTITIONS, 0); }
		public RecoverPartitionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRecoverPartitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRecoverPartitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRecoverPartitions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowCurrentNamespaceContext extends StatementContext {
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticExtendSparkSqlParser.CURRENT, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticExtendSparkSqlParser.NAMESPACE, 0); }
		public ShowCurrentNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowCurrentNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowCurrentNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowCurrentNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RenameTablePartitionContext extends StatementContext {
		public PartitionSpecContext from;
		public PartitionSpecContext to;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode RENAME() { return getToken(ArcticExtendSparkSqlParser.RENAME, 0); }
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public List<PartitionSpecContext> partitionSpec() {
			return getRuleContexts(PartitionSpecContext.class);
		}
		public PartitionSpecContext partitionSpec(int i) {
			return getRuleContext(PartitionSpecContext.class,i);
		}
		public RenameTablePartitionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRenameTablePartition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRenameTablePartition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRenameTablePartition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RepairTableContext extends StatementContext {
		public Token option;
		public TerminalNode MSCK() { return getToken(ArcticExtendSparkSqlParser.MSCK, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticExtendSparkSqlParser.REPAIR, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode PARTITIONS() { return getToken(ArcticExtendSparkSqlParser.PARTITIONS, 0); }
		public TerminalNode ADD() { return getToken(ArcticExtendSparkSqlParser.ADD, 0); }
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode SYNC() { return getToken(ArcticExtendSparkSqlParser.SYNC, 0); }
		public RepairTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRepairTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRepairTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRepairTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RefreshResourceContext extends StatementContext {
		public TerminalNode REFRESH() { return getToken(ArcticExtendSparkSqlParser.REFRESH, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public RefreshResourceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRefreshResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRefreshResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRefreshResource(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowCreateTableContext extends StatementContext {
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public TerminalNode SERDE() { return getToken(ArcticExtendSparkSqlParser.SERDE, 0); }
		public ShowCreateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowCreateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowCreateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowCreateTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowNamespacesContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticExtendSparkSqlParser.DATABASES, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticExtendSparkSqlParser.NAMESPACES, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public ShowNamespacesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowNamespaces(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowNamespaces(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowNamespaces(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowColumnsContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext ns;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public List<TerminalNode> FROM() { return getTokens(ArcticExtendSparkSqlParser.FROM); }
		public TerminalNode FROM(int i) {
			return getToken(ArcticExtendSparkSqlParser.FROM, i);
		}
		public List<TerminalNode> IN() { return getTokens(ArcticExtendSparkSqlParser.IN); }
		public TerminalNode IN(int i) {
			return getToken(ArcticExtendSparkSqlParser.IN, i);
		}
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public ShowColumnsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ReplaceTableContext extends StatementContext {
		public ReplaceTableHeaderContext replaceTableHeader() {
			return getRuleContext(ReplaceTableHeaderContext.class,0);
		}
		public CreateTableClausesContext createTableClauses() {
			return getRuleContext(CreateTableClausesContext.class,0);
		}
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public ReplaceTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterReplaceTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitReplaceTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitReplaceTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnalyzeTablesContext extends StatementContext {
		public TerminalNode ANALYZE() { return getToken(ArcticExtendSparkSqlParser.ANALYZE, 0); }
		public TerminalNode TABLES() { return getToken(ArcticExtendSparkSqlParser.TABLES, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticExtendSparkSqlParser.COMPUTE, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticExtendSparkSqlParser.STATISTICS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public AnalyzeTablesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAnalyzeTables(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAnalyzeTables(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAnalyzeTables(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AddTablePartitionContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode ADD() { return getToken(ArcticExtendSparkSqlParser.ADD, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public List<PartitionSpecLocationContext> partitionSpecLocation() {
			return getRuleContexts(PartitionSpecLocationContext.class);
		}
		public PartitionSpecLocationContext partitionSpecLocation(int i) {
			return getRuleContext(PartitionSpecLocationContext.class,i);
		}
		public AddTablePartitionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAddTablePartition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAddTablePartition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAddTablePartition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetNamespaceLocationContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public LocationSpecContext locationSpec() {
			return getRuleContext(LocationSpecContext.class,0);
		}
		public SetNamespaceLocationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetNamespaceLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetNamespaceLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetNamespaceLocation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RefreshTableContext extends StatementContext {
		public TerminalNode REFRESH() { return getToken(ArcticExtendSparkSqlParser.REFRESH, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public RefreshTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRefreshTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRefreshTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRefreshTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetNamespacePropertiesContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode DBPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.DBPROPERTIES, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticExtendSparkSqlParser.PROPERTIES, 0); }
		public SetNamespacePropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetNamespaceProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetNamespaceProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetNamespaceProperties(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ManageResourceContext extends StatementContext {
		public Token op;
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ADD() { return getToken(ArcticExtendSparkSqlParser.ADD, 0); }
		public TerminalNode LIST() { return getToken(ArcticExtendSparkSqlParser.LIST, 0); }
		public ManageResourceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterManageResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitManageResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitManageResource(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetQuotedConfigurationContext extends StatementContext {
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public ConfigKeyContext configKey() {
			return getRuleContext(ConfigKeyContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticExtendSparkSqlParser.EQ, 0); }
		public ConfigValueContext configValue() {
			return getRuleContext(ConfigValueContext.class,0);
		}
		public SetQuotedConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetQuotedConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetQuotedConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetQuotedConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnalyzeContext extends StatementContext {
		public TerminalNode ANALYZE() { return getToken(ArcticExtendSparkSqlParser.ANALYZE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode COMPUTE() { return getToken(ArcticExtendSparkSqlParser.COMPUTE, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticExtendSparkSqlParser.STATISTICS, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode FOR() { return getToken(ArcticExtendSparkSqlParser.FOR, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public TerminalNode ALL() { return getToken(ArcticExtendSparkSqlParser.ALL, 0); }
		public AnalyzeContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAnalyze(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAnalyze(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAnalyze(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateFunctionContext extends StatementContext {
		public Token className;
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticExtendSparkSqlParser.FUNCTION, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public TerminalNode USING() { return getToken(ArcticExtendSparkSqlParser.USING, 0); }
		public List<ResourceContext> resource() {
			return getRuleContexts(ResourceContext.class);
		}
		public ResourceContext resource(int i) {
			return getRuleContext(ResourceContext.class,i);
		}
		public CreateFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HiveReplaceColumnsContext extends StatementContext {
		public MultipartIdentifierContext table;
		public QualifiedColTypeWithPositionListContext columns;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public QualifiedColTypeWithPositionListContext qualifiedColTypeWithPositionList() {
			return getRuleContext(QualifiedColTypeWithPositionListContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public HiveReplaceColumnsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterHiveReplaceColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitHiveReplaceColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitHiveReplaceColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CommentNamespaceContext extends StatementContext {
		public Token comment;
		public TerminalNode COMMENT() { return getToken(ArcticExtendSparkSqlParser.COMMENT, 0); }
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IS() { return getToken(ArcticExtendSparkSqlParser.IS, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public CommentNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCommentNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCommentNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCommentNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ResetQuotedConfigurationContext extends StatementContext {
		public TerminalNode RESET() { return getToken(ArcticExtendSparkSqlParser.RESET, 0); }
		public ConfigKeyContext configKey() {
			return getRuleContext(ConfigKeyContext.class,0);
		}
		public ResetQuotedConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterResetQuotedConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitResetQuotedConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitResetQuotedConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateTableContext extends StatementContext {
		public CreateTableHeaderContext createTableHeader() {
			return getRuleContext(CreateTableHeaderContext.class,0);
		}
		public CreateTableClausesContext createTableClauses() {
			return getRuleContext(CreateTableClausesContext.class,0);
		}
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public CreateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DmlStatementContext extends StatementContext {
		public DmlStatementNoWithContext dmlStatementNoWith() {
			return getRuleContext(DmlStatementNoWithContext.class,0);
		}
		public CtesContext ctes() {
			return getRuleContext(CtesContext.class,0);
		}
		public DmlStatementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDmlStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDmlStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDmlStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateTableLikeContext extends StatementContext {
		public TableIdentifierContext target;
		public TableIdentifierContext source;
		public TablePropertyListContext tableProps;
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public List<TableIdentifierContext> tableIdentifier() {
			return getRuleContexts(TableIdentifierContext.class);
		}
		public TableIdentifierContext tableIdentifier(int i) {
			return getRuleContext(TableIdentifierContext.class,i);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public List<TableProviderContext> tableProvider() {
			return getRuleContexts(TableProviderContext.class);
		}
		public TableProviderContext tableProvider(int i) {
			return getRuleContext(TableProviderContext.class,i);
		}
		public List<RowFormatContext> rowFormat() {
			return getRuleContexts(RowFormatContext.class);
		}
		public RowFormatContext rowFormat(int i) {
			return getRuleContext(RowFormatContext.class,i);
		}
		public List<CreateFileFormatContext> createFileFormat() {
			return getRuleContexts(CreateFileFormatContext.class);
		}
		public CreateFileFormatContext createFileFormat(int i) {
			return getRuleContext(CreateFileFormatContext.class,i);
		}
		public List<LocationSpecContext> locationSpec() {
			return getRuleContexts(LocationSpecContext.class);
		}
		public LocationSpecContext locationSpec(int i) {
			return getRuleContext(LocationSpecContext.class,i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticExtendSparkSqlParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, i);
		}
		public List<TablePropertyListContext> tablePropertyList() {
			return getRuleContexts(TablePropertyListContext.class);
		}
		public TablePropertyListContext tablePropertyList(int i) {
			return getRuleContext(TablePropertyListContext.class,i);
		}
		public CreateTableLikeContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTableLike(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTableLike(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTableLike(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UncacheTableContext extends StatementContext {
		public TerminalNode UNCACHE() { return getToken(ArcticExtendSparkSqlParser.UNCACHE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public UncacheTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUncacheTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUncacheTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUncacheTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropFunctionContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticExtendSparkSqlParser.FUNCTION, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public DropFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDropFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDropFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDropFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeRelationContext extends StatementContext {
		public Token option;
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticExtendSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public DescribeColNameContext describeColName() {
			return getRuleContext(DescribeColNameContext.class,0);
		}
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticExtendSparkSqlParser.FORMATTED, 0); }
		public DescribeRelationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDescribeRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDescribeRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDescribeRelation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LoadDataContext extends StatementContext {
		public Token path;
		public TerminalNode LOAD() { return getToken(ArcticExtendSparkSqlParser.LOAD, 0); }
		public TerminalNode DATA() { return getToken(ArcticExtendSparkSqlParser.DATA, 0); }
		public TerminalNode INPATH() { return getToken(ArcticExtendSparkSqlParser.INPATH, 0); }
		public TerminalNode INTO() { return getToken(ArcticExtendSparkSqlParser.INTO, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticExtendSparkSqlParser.LOCAL, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticExtendSparkSqlParser.OVERWRITE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public LoadDataContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLoadData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLoadData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLoadData(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowPartitionsContext extends StatementContext {
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticExtendSparkSqlParser.PARTITIONS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public ShowPartitionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowPartitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowPartitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowPartitions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeFunctionContext extends StatementContext {
		public TerminalNode FUNCTION() { return getToken(ArcticExtendSparkSqlParser.FUNCTION, 0); }
		public DescribeFuncNameContext describeFuncName() {
			return getRuleContext(DescribeFuncNameContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticExtendSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public DescribeFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDescribeFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDescribeFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDescribeFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RenameTableColumnContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext from;
		public ErrorCapturingIdentifierContext to;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode RENAME() { return getToken(ArcticExtendSparkSqlParser.RENAME, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticExtendSparkSqlParser.COLUMN, 0); }
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public RenameTableColumnContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRenameTableColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRenameTableColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRenameTableColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StatementDefaultContext extends StatementContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public StatementDefaultContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterStatementDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitStatementDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitStatementDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HiveChangeColumnContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext colName;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticExtendSparkSqlParser.CHANGE, 0); }
		public ColTypeContext colType() {
			return getRuleContext(ColTypeContext.class,0);
		}
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode COLUMN() { return getToken(ArcticExtendSparkSqlParser.COLUMN, 0); }
		public ColPositionContext colPosition() {
			return getRuleContext(ColPositionContext.class,0);
		}
		public HiveChangeColumnContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterHiveChangeColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitHiveChangeColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitHiveChangeColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTimeZoneContext extends StatementContext {
		public Token timezone;
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode TIME() { return getToken(ArcticExtendSparkSqlParser.TIME, 0); }
		public TerminalNode ZONE() { return getToken(ArcticExtendSparkSqlParser.ZONE, 0); }
		public IntervalContext interval() {
			return getRuleContext(IntervalContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticExtendSparkSqlParser.LOCAL, 0); }
		public SetTimeZoneContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetTimeZone(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetTimeZone(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetTimeZone(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeQueryContext extends StatementContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticExtendSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode QUERY() { return getToken(ArcticExtendSparkSqlParser.QUERY, 0); }
		public DescribeQueryContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDescribeQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDescribeQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDescribeQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TruncateTableContext extends StatementContext {
		public TerminalNode TRUNCATE() { return getToken(ArcticExtendSparkSqlParser.TRUNCATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TruncateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTruncateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTruncateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTruncateTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTableSerDeContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode SERDE() { return getToken(ArcticExtendSparkSqlParser.SERDE, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode WITH() { return getToken(ArcticExtendSparkSqlParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public SetTableSerDeContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetTableSerDe(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetTableSerDe(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetTableSerDe(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateViewContext extends StatementContext {
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public IdentifierCommentListContext identifierCommentList() {
			return getRuleContext(IdentifierCommentListContext.class,0);
		}
		public List<CommentSpecContext> commentSpec() {
			return getRuleContexts(CommentSpecContext.class);
		}
		public CommentSpecContext commentSpec(int i) {
			return getRuleContext(CommentSpecContext.class,i);
		}
		public List<TerminalNode> PARTITIONED() { return getTokens(ArcticExtendSparkSqlParser.PARTITIONED); }
		public TerminalNode PARTITIONED(int i) {
			return getToken(ArcticExtendSparkSqlParser.PARTITIONED, i);
		}
		public List<TerminalNode> ON() { return getTokens(ArcticExtendSparkSqlParser.ON); }
		public TerminalNode ON(int i) {
			return getToken(ArcticExtendSparkSqlParser.ON, i);
		}
		public List<IdentifierListContext> identifierList() {
			return getRuleContexts(IdentifierListContext.class);
		}
		public IdentifierListContext identifierList(int i) {
			return getRuleContext(IdentifierListContext.class,i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticExtendSparkSqlParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, i);
		}
		public List<TablePropertyListContext> tablePropertyList() {
			return getRuleContexts(TablePropertyListContext.class);
		}
		public TablePropertyListContext tablePropertyList(int i) {
			return getRuleContext(TablePropertyListContext.class,i);
		}
		public TerminalNode GLOBAL() { return getToken(ArcticExtendSparkSqlParser.GLOBAL, 0); }
		public CreateViewContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateView(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropTablePartitionsContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public List<PartitionSpecContext> partitionSpec() {
			return getRuleContexts(PartitionSpecContext.class);
		}
		public PartitionSpecContext partitionSpec(int i) {
			return getRuleContext(PartitionSpecContext.class,i);
		}
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public TerminalNode PURGE() { return getToken(ArcticExtendSparkSqlParser.PURGE, 0); }
		public DropTablePartitionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDropTablePartitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDropTablePartitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDropTablePartitions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetConfigurationContext extends StatementContext {
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public ConfigKeyContext configKey() {
			return getRuleContext(ConfigKeyContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticExtendSparkSqlParser.EQ, 0); }
		public SetConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropTableContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public TerminalNode PURGE() { return getToken(ArcticExtendSparkSqlParser.PURGE, 0); }
		public DropTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDropTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDropTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDropTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowTableExtendedContext extends StatementContext {
		public MultipartIdentifierContext ns;
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public ShowTableExtendedContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowTableExtended(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowTableExtended(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowTableExtended(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeNamespaceContext extends StatementContext {
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticExtendSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public DescribeNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDescribeNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDescribeNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDescribeNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AlterTableAlterColumnContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext column;
		public List<TerminalNode> ALTER() { return getTokens(ArcticExtendSparkSqlParser.ALTER); }
		public TerminalNode ALTER(int i) {
			return getToken(ArcticExtendSparkSqlParser.ALTER, i);
		}
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public TerminalNode CHANGE() { return getToken(ArcticExtendSparkSqlParser.CHANGE, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticExtendSparkSqlParser.COLUMN, 0); }
		public AlterColumnActionContext alterColumnAction() {
			return getRuleContext(AlterColumnActionContext.class,0);
		}
		public AlterTableAlterColumnContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAlterTableAlterColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAlterTableAlterColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAlterTableAlterColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RefreshFunctionContext extends StatementContext {
		public TerminalNode REFRESH() { return getToken(ArcticExtendSparkSqlParser.REFRESH, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticExtendSparkSqlParser.FUNCTION, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public RefreshFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRefreshFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRefreshFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRefreshFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CommentTableContext extends StatementContext {
		public Token comment;
		public TerminalNode COMMENT() { return getToken(ArcticExtendSparkSqlParser.COMMENT, 0); }
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IS() { return getToken(ArcticExtendSparkSqlParser.IS, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public CommentTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCommentTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCommentTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCommentTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateNamespaceContext extends StatementContext {
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public List<CommentSpecContext> commentSpec() {
			return getRuleContexts(CommentSpecContext.class);
		}
		public CommentSpecContext commentSpec(int i) {
			return getRuleContext(CommentSpecContext.class,i);
		}
		public List<LocationSpecContext> locationSpec() {
			return getRuleContexts(LocationSpecContext.class);
		}
		public LocationSpecContext locationSpec(int i) {
			return getRuleContext(LocationSpecContext.class,i);
		}
		public List<TerminalNode> WITH() { return getTokens(ArcticExtendSparkSqlParser.WITH); }
		public TerminalNode WITH(int i) {
			return getToken(ArcticExtendSparkSqlParser.WITH, i);
		}
		public List<TablePropertyListContext> tablePropertyList() {
			return getRuleContexts(TablePropertyListContext.class);
		}
		public TablePropertyListContext tablePropertyList(int i) {
			return getRuleContext(TablePropertyListContext.class,i);
		}
		public List<TerminalNode> DBPROPERTIES() { return getTokens(ArcticExtendSparkSqlParser.DBPROPERTIES); }
		public TerminalNode DBPROPERTIES(int i) {
			return getToken(ArcticExtendSparkSqlParser.DBPROPERTIES, i);
		}
		public List<TerminalNode> PROPERTIES() { return getTokens(ArcticExtendSparkSqlParser.PROPERTIES); }
		public TerminalNode PROPERTIES(int i) {
			return getToken(ArcticExtendSparkSqlParser.PROPERTIES, i);
		}
		public CreateNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowTblPropertiesContext extends StatementContext {
		public MultipartIdentifierContext table;
		public TablePropertyKeyContext key;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TablePropertyKeyContext tablePropertyKey() {
			return getRuleContext(TablePropertyKeyContext.class,0);
		}
		public ShowTblPropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowTblProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowTblProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowTblProperties(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnsetTablePropertiesContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode UNSET() { return getToken(ArcticExtendSparkSqlParser.UNSET, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public UnsetTablePropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUnsetTableProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUnsetTableProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUnsetTableProperties(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTableLocationContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public LocationSpecContext locationSpec() {
			return getRuleContext(LocationSpecContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public SetTableLocationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetTableLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetTableLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetTableLocation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropTableColumnsContext extends StatementContext {
		public MultipartIdentifierListContext columns;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticExtendSparkSqlParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public MultipartIdentifierListContext multipartIdentifierList() {
			return getRuleContext(MultipartIdentifierListContext.class,0);
		}
		public DropTableColumnsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDropTableColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDropTableColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDropTableColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowViewsContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticExtendSparkSqlParser.VIEWS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public ShowViewsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowViews(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowViews(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowViews(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowFunctionsContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticExtendSparkSqlParser.FUNCTIONS, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public ShowFunctionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterShowFunctions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitShowFunctions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitShowFunctions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CacheTableContext extends StatementContext {
		public TablePropertyListContext options;
		public TerminalNode CACHE() { return getToken(ArcticExtendSparkSqlParser.CACHE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode LAZY() { return getToken(ArcticExtendSparkSqlParser.LAZY, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticExtendSparkSqlParser.OPTIONS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public CacheTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCacheTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCacheTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCacheTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AddTableColumnsContext extends StatementContext {
		public QualifiedColTypeWithPositionListContext columns;
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode ADD() { return getToken(ArcticExtendSparkSqlParser.ADD, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticExtendSparkSqlParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public QualifiedColTypeWithPositionListContext qualifiedColTypeWithPositionList() {
			return getRuleContext(QualifiedColTypeWithPositionListContext.class,0);
		}
		public AddTableColumnsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAddTableColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAddTableColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAddTableColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTablePropertiesContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public SetTablePropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetTableProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetTableProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetTableProperties(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_statement);
		int _la;
		try {
			int _alt;
			setState(1089);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
			case 1:
				_localctx = new StatementDefaultContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(352);
				query();
				}
				break;
			case 2:
				_localctx = new DmlStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(354);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WITH) {
					{
					setState(353);
					ctes();
					}
				}

				setState(356);
				dmlStatementNoWith();
				}
				break;
			case 3:
				_localctx = new UseContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(357);
				match(USE);
				setState(359);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
				case 1:
					{
					setState(358);
					match(NAMESPACE);
					}
					break;
				}
				setState(361);
				multipartIdentifier();
				}
				break;
			case 4:
				_localctx = new CreateNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(362);
				match(CREATE);
				setState(363);
				namespace();
				setState(367);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
				case 1:
					{
					setState(364);
					match(IF);
					setState(365);
					match(NOT);
					setState(366);
					match(EXISTS);
					}
					break;
				}
				setState(369);
				multipartIdentifier();
				setState(377);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMENT || _la==LOCATION || _la==WITH) {
					{
					setState(375);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case COMMENT:
						{
						setState(370);
						commentSpec();
						}
						break;
					case LOCATION:
						{
						setState(371);
						locationSpec();
						}
						break;
					case WITH:
						{
						{
						setState(372);
						match(WITH);
						setState(373);
						_la = _input.LA(1);
						if ( !(_la==DBPROPERTIES || _la==PROPERTIES) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(374);
						tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(379);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 5:
				_localctx = new SetNamespacePropertiesContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(380);
				match(ALTER);
				setState(381);
				namespace();
				setState(382);
				multipartIdentifier();
				setState(383);
				match(SET);
				setState(384);
				_la = _input.LA(1);
				if ( !(_la==DBPROPERTIES || _la==PROPERTIES) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(385);
				tablePropertyList();
				}
				break;
			case 6:
				_localctx = new SetNamespaceLocationContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(387);
				match(ALTER);
				setState(388);
				namespace();
				setState(389);
				multipartIdentifier();
				setState(390);
				match(SET);
				setState(391);
				locationSpec();
				}
				break;
			case 7:
				_localctx = new DropNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(393);
				match(DROP);
				setState(394);
				namespace();
				setState(397);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(395);
					match(IF);
					setState(396);
					match(EXISTS);
					}
					break;
				}
				setState(399);
				multipartIdentifier();
				setState(401);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CASCADE || _la==RESTRICT) {
					{
					setState(400);
					_la = _input.LA(1);
					if ( !(_la==CASCADE || _la==RESTRICT) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				}
				break;
			case 8:
				_localctx = new ShowNamespacesContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(403);
				match(SHOW);
				setState(404);
				_la = _input.LA(1);
				if ( !(_la==DATABASES || _la==NAMESPACES) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(407);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(405);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(406);
					multipartIdentifier();
					}
				}

				setState(413);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIKE || _la==STRING) {
					{
					setState(410);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LIKE) {
						{
						setState(409);
						match(LIKE);
						}
					}

					setState(412);
					((ShowNamespacesContext)_localctx).pattern = match(STRING);
					}
				}

				}
				break;
			case 9:
				_localctx = new CreateTableContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(415);
				createTableHeader();
				setState(420);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
				case 1:
					{
					setState(416);
					match(T__0);
					setState(417);
					colTypeList();
					setState(418);
					match(T__2);
					}
					break;
				}
				setState(423);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(422);
					tableProvider();
					}
				}

				setState(425);
				createTableClauses();
				setState(430);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0 || _la==AS || _la==FROM || _la==MAP || ((((_la - 190)) & ~0x3f) == 0 && ((1L << (_la - 190)) & ((1L << (REDUCE - 190)) | (1L << (SELECT - 190)) | (1L << (TABLE - 190)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(427);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(426);
						match(AS);
						}
					}

					setState(429);
					query();
					}
				}

				}
				break;
			case 10:
				_localctx = new CreateTableLikeContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(432);
				match(CREATE);
				setState(433);
				match(TABLE);
				setState(437);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(434);
					match(IF);
					setState(435);
					match(NOT);
					setState(436);
					match(EXISTS);
					}
					break;
				}
				setState(439);
				((CreateTableLikeContext)_localctx).target = tableIdentifier();
				setState(440);
				match(LIKE);
				setState(441);
				((CreateTableLikeContext)_localctx).source = tableIdentifier();
				setState(450);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LOCATION || ((((_la - 206)) & ~0x3f) == 0 && ((1L << (_la - 206)) & ((1L << (ROW - 206)) | (1L << (STORED - 206)) | (1L << (TBLPROPERTIES - 206)) | (1L << (USING - 206)))) != 0)) {
					{
					setState(448);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case USING:
						{
						setState(442);
						tableProvider();
						}
						break;
					case ROW:
						{
						setState(443);
						rowFormat();
						}
						break;
					case STORED:
						{
						setState(444);
						createFileFormat();
						}
						break;
					case LOCATION:
						{
						setState(445);
						locationSpec();
						}
						break;
					case TBLPROPERTIES:
						{
						{
						setState(446);
						match(TBLPROPERTIES);
						setState(447);
						((CreateTableLikeContext)_localctx).tableProps = tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(452);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 11:
				_localctx = new ReplaceTableContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(453);
				replaceTableHeader();
				setState(458);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
				case 1:
					{
					setState(454);
					match(T__0);
					setState(455);
					colTypeList();
					setState(456);
					match(T__2);
					}
					break;
				}
				setState(461);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(460);
					tableProvider();
					}
				}

				setState(463);
				createTableClauses();
				setState(468);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0 || _la==AS || _la==FROM || _la==MAP || ((((_la - 190)) & ~0x3f) == 0 && ((1L << (_la - 190)) & ((1L << (REDUCE - 190)) | (1L << (SELECT - 190)) | (1L << (TABLE - 190)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(465);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(464);
						match(AS);
						}
					}

					setState(467);
					query();
					}
				}

				}
				break;
			case 12:
				_localctx = new AnalyzeContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(470);
				match(ANALYZE);
				setState(471);
				match(TABLE);
				setState(472);
				multipartIdentifier();
				setState(474);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(473);
					partitionSpec();
					}
				}

				setState(476);
				match(COMPUTE);
				setState(477);
				match(STATISTICS);
				setState(485);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
				case 1:
					{
					setState(478);
					identifier();
					}
					break;
				case 2:
					{
					setState(479);
					match(FOR);
					setState(480);
					match(COLUMNS);
					setState(481);
					identifierSeq();
					}
					break;
				case 3:
					{
					setState(482);
					match(FOR);
					setState(483);
					match(ALL);
					setState(484);
					match(COLUMNS);
					}
					break;
				}
				}
				break;
			case 13:
				_localctx = new AnalyzeTablesContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(487);
				match(ANALYZE);
				setState(488);
				match(TABLES);
				setState(491);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(489);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(490);
					multipartIdentifier();
					}
				}

				setState(493);
				match(COMPUTE);
				setState(494);
				match(STATISTICS);
				setState(496);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
				case 1:
					{
					setState(495);
					identifier();
					}
					break;
				}
				}
				break;
			case 14:
				_localctx = new AddTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(498);
				match(ALTER);
				setState(499);
				match(TABLE);
				setState(500);
				multipartIdentifier();
				setState(501);
				match(ADD);
				setState(502);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(503);
				((AddTableColumnsContext)_localctx).columns = qualifiedColTypeWithPositionList();
				}
				break;
			case 15:
				_localctx = new AddTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(505);
				match(ALTER);
				setState(506);
				match(TABLE);
				setState(507);
				multipartIdentifier();
				setState(508);
				match(ADD);
				setState(509);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(510);
				match(T__0);
				setState(511);
				((AddTableColumnsContext)_localctx).columns = qualifiedColTypeWithPositionList();
				setState(512);
				match(T__2);
				}
				break;
			case 16:
				_localctx = new RenameTableColumnContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(514);
				match(ALTER);
				setState(515);
				match(TABLE);
				setState(516);
				((RenameTableColumnContext)_localctx).table = multipartIdentifier();
				setState(517);
				match(RENAME);
				setState(518);
				match(COLUMN);
				setState(519);
				((RenameTableColumnContext)_localctx).from = multipartIdentifier();
				setState(520);
				match(TO);
				setState(521);
				((RenameTableColumnContext)_localctx).to = errorCapturingIdentifier();
				}
				break;
			case 17:
				_localctx = new DropTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(523);
				match(ALTER);
				setState(524);
				match(TABLE);
				setState(525);
				multipartIdentifier();
				setState(526);
				match(DROP);
				setState(527);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(528);
				match(T__0);
				setState(529);
				((DropTableColumnsContext)_localctx).columns = multipartIdentifierList();
				setState(530);
				match(T__2);
				}
				break;
			case 18:
				_localctx = new DropTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(532);
				match(ALTER);
				setState(533);
				match(TABLE);
				setState(534);
				multipartIdentifier();
				setState(535);
				match(DROP);
				setState(536);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(537);
				((DropTableColumnsContext)_localctx).columns = multipartIdentifierList();
				}
				break;
			case 19:
				_localctx = new RenameTableContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(539);
				match(ALTER);
				setState(540);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(541);
				((RenameTableContext)_localctx).from = multipartIdentifier();
				setState(542);
				match(RENAME);
				setState(543);
				match(TO);
				setState(544);
				((RenameTableContext)_localctx).to = multipartIdentifier();
				}
				break;
			case 20:
				_localctx = new SetTablePropertiesContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(546);
				match(ALTER);
				setState(547);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(548);
				multipartIdentifier();
				setState(549);
				match(SET);
				setState(550);
				match(TBLPROPERTIES);
				setState(551);
				tablePropertyList();
				}
				break;
			case 21:
				_localctx = new UnsetTablePropertiesContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(553);
				match(ALTER);
				setState(554);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(555);
				multipartIdentifier();
				setState(556);
				match(UNSET);
				setState(557);
				match(TBLPROPERTIES);
				setState(560);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(558);
					match(IF);
					setState(559);
					match(EXISTS);
					}
				}

				setState(562);
				tablePropertyList();
				}
				break;
			case 22:
				_localctx = new AlterTableAlterColumnContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(564);
				match(ALTER);
				setState(565);
				match(TABLE);
				setState(566);
				((AlterTableAlterColumnContext)_localctx).table = multipartIdentifier();
				setState(567);
				_la = _input.LA(1);
				if ( !(_la==ALTER || _la==CHANGE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(569);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
				case 1:
					{
					setState(568);
					match(COLUMN);
					}
					break;
				}
				setState(571);
				((AlterTableAlterColumnContext)_localctx).column = multipartIdentifier();
				setState(573);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AFTER || _la==COMMENT || _la==DROP || _la==FIRST || _la==SET || _la==TYPE) {
					{
					setState(572);
					alterColumnAction();
					}
				}

				}
				break;
			case 23:
				_localctx = new HiveChangeColumnContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(575);
				match(ALTER);
				setState(576);
				match(TABLE);
				setState(577);
				((HiveChangeColumnContext)_localctx).table = multipartIdentifier();
				setState(579);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(578);
					partitionSpec();
					}
				}

				setState(581);
				match(CHANGE);
				setState(583);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
				case 1:
					{
					setState(582);
					match(COLUMN);
					}
					break;
				}
				setState(585);
				((HiveChangeColumnContext)_localctx).colName = multipartIdentifier();
				setState(586);
				colType();
				setState(588);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AFTER || _la==FIRST) {
					{
					setState(587);
					colPosition();
					}
				}

				}
				break;
			case 24:
				_localctx = new HiveReplaceColumnsContext(_localctx);
				enterOuterAlt(_localctx, 24);
				{
				setState(590);
				match(ALTER);
				setState(591);
				match(TABLE);
				setState(592);
				((HiveReplaceColumnsContext)_localctx).table = multipartIdentifier();
				setState(594);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(593);
					partitionSpec();
					}
				}

				setState(596);
				match(REPLACE);
				setState(597);
				match(COLUMNS);
				setState(598);
				match(T__0);
				setState(599);
				((HiveReplaceColumnsContext)_localctx).columns = qualifiedColTypeWithPositionList();
				setState(600);
				match(T__2);
				}
				break;
			case 25:
				_localctx = new SetTableSerDeContext(_localctx);
				enterOuterAlt(_localctx, 25);
				{
				setState(602);
				match(ALTER);
				setState(603);
				match(TABLE);
				setState(604);
				multipartIdentifier();
				setState(606);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(605);
					partitionSpec();
					}
				}

				setState(608);
				match(SET);
				setState(609);
				match(SERDE);
				setState(610);
				match(STRING);
				setState(614);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WITH) {
					{
					setState(611);
					match(WITH);
					setState(612);
					match(SERDEPROPERTIES);
					setState(613);
					tablePropertyList();
					}
				}

				}
				break;
			case 26:
				_localctx = new SetTableSerDeContext(_localctx);
				enterOuterAlt(_localctx, 26);
				{
				setState(616);
				match(ALTER);
				setState(617);
				match(TABLE);
				setState(618);
				multipartIdentifier();
				setState(620);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(619);
					partitionSpec();
					}
				}

				setState(622);
				match(SET);
				setState(623);
				match(SERDEPROPERTIES);
				setState(624);
				tablePropertyList();
				}
				break;
			case 27:
				_localctx = new AddTablePartitionContext(_localctx);
				enterOuterAlt(_localctx, 27);
				{
				setState(626);
				match(ALTER);
				setState(627);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(628);
				multipartIdentifier();
				setState(629);
				match(ADD);
				setState(633);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(630);
					match(IF);
					setState(631);
					match(NOT);
					setState(632);
					match(EXISTS);
					}
				}

				setState(636); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(635);
					partitionSpecLocation();
					}
					}
					setState(638); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==PARTITION );
				}
				break;
			case 28:
				_localctx = new RenameTablePartitionContext(_localctx);
				enterOuterAlt(_localctx, 28);
				{
				setState(640);
				match(ALTER);
				setState(641);
				match(TABLE);
				setState(642);
				multipartIdentifier();
				setState(643);
				((RenameTablePartitionContext)_localctx).from = partitionSpec();
				setState(644);
				match(RENAME);
				setState(645);
				match(TO);
				setState(646);
				((RenameTablePartitionContext)_localctx).to = partitionSpec();
				}
				break;
			case 29:
				_localctx = new DropTablePartitionsContext(_localctx);
				enterOuterAlt(_localctx, 29);
				{
				setState(648);
				match(ALTER);
				setState(649);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(650);
				multipartIdentifier();
				setState(651);
				match(DROP);
				setState(654);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(652);
					match(IF);
					setState(653);
					match(EXISTS);
					}
				}

				setState(656);
				partitionSpec();
				setState(661);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(657);
					match(T__1);
					setState(658);
					partitionSpec();
					}
					}
					setState(663);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(665);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PURGE) {
					{
					setState(664);
					match(PURGE);
					}
				}

				}
				break;
			case 30:
				_localctx = new SetTableLocationContext(_localctx);
				enterOuterAlt(_localctx, 30);
				{
				setState(667);
				match(ALTER);
				setState(668);
				match(TABLE);
				setState(669);
				multipartIdentifier();
				setState(671);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(670);
					partitionSpec();
					}
				}

				setState(673);
				match(SET);
				setState(674);
				locationSpec();
				}
				break;
			case 31:
				_localctx = new RecoverPartitionsContext(_localctx);
				enterOuterAlt(_localctx, 31);
				{
				setState(676);
				match(ALTER);
				setState(677);
				match(TABLE);
				setState(678);
				multipartIdentifier();
				setState(679);
				match(RECOVER);
				setState(680);
				match(PARTITIONS);
				}
				break;
			case 32:
				_localctx = new DropTableContext(_localctx);
				enterOuterAlt(_localctx, 32);
				{
				setState(682);
				match(DROP);
				setState(683);
				match(TABLE);
				setState(686);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,46,_ctx) ) {
				case 1:
					{
					setState(684);
					match(IF);
					setState(685);
					match(EXISTS);
					}
					break;
				}
				setState(688);
				multipartIdentifier();
				setState(690);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PURGE) {
					{
					setState(689);
					match(PURGE);
					}
				}

				}
				break;
			case 33:
				_localctx = new DropViewContext(_localctx);
				enterOuterAlt(_localctx, 33);
				{
				setState(692);
				match(DROP);
				setState(693);
				match(VIEW);
				setState(696);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
				case 1:
					{
					setState(694);
					match(IF);
					setState(695);
					match(EXISTS);
					}
					break;
				}
				setState(698);
				multipartIdentifier();
				}
				break;
			case 34:
				_localctx = new CreateViewContext(_localctx);
				enterOuterAlt(_localctx, 34);
				{
				setState(699);
				match(CREATE);
				setState(702);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OR) {
					{
					setState(700);
					match(OR);
					setState(701);
					match(REPLACE);
					}
				}

				setState(708);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GLOBAL || _la==TEMPORARY) {
					{
					setState(705);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==GLOBAL) {
						{
						setState(704);
						match(GLOBAL);
						}
					}

					setState(707);
					match(TEMPORARY);
					}
				}

				setState(710);
				match(VIEW);
				setState(714);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,52,_ctx) ) {
				case 1:
					{
					setState(711);
					match(IF);
					setState(712);
					match(NOT);
					setState(713);
					match(EXISTS);
					}
					break;
				}
				setState(716);
				multipartIdentifier();
				setState(718);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(717);
					identifierCommentList();
					}
				}

				setState(728);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMENT || _la==PARTITIONED || _la==TBLPROPERTIES) {
					{
					setState(726);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case COMMENT:
						{
						setState(720);
						commentSpec();
						}
						break;
					case PARTITIONED:
						{
						{
						setState(721);
						match(PARTITIONED);
						setState(722);
						match(ON);
						setState(723);
						identifierList();
						}
						}
						break;
					case TBLPROPERTIES:
						{
						{
						setState(724);
						match(TBLPROPERTIES);
						setState(725);
						tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(730);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(731);
				match(AS);
				setState(732);
				query();
				}
				break;
			case 35:
				_localctx = new CreateTempViewUsingContext(_localctx);
				enterOuterAlt(_localctx, 35);
				{
				setState(734);
				match(CREATE);
				setState(737);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OR) {
					{
					setState(735);
					match(OR);
					setState(736);
					match(REPLACE);
					}
				}

				setState(740);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GLOBAL) {
					{
					setState(739);
					match(GLOBAL);
					}
				}

				setState(742);
				match(TEMPORARY);
				setState(743);
				match(VIEW);
				setState(744);
				tableIdentifier();
				setState(749);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(745);
					match(T__0);
					setState(746);
					colTypeList();
					setState(747);
					match(T__2);
					}
				}

				setState(751);
				tableProvider();
				setState(754);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(752);
					match(OPTIONS);
					setState(753);
					tablePropertyList();
					}
				}

				}
				break;
			case 36:
				_localctx = new AlterViewQueryContext(_localctx);
				enterOuterAlt(_localctx, 36);
				{
				setState(756);
				match(ALTER);
				setState(757);
				match(VIEW);
				setState(758);
				multipartIdentifier();
				setState(760);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(759);
					match(AS);
					}
				}

				setState(762);
				query();
				}
				break;
			case 37:
				_localctx = new CreateFunctionContext(_localctx);
				enterOuterAlt(_localctx, 37);
				{
				setState(764);
				match(CREATE);
				setState(767);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OR) {
					{
					setState(765);
					match(OR);
					setState(766);
					match(REPLACE);
					}
				}

				setState(770);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==TEMPORARY) {
					{
					setState(769);
					match(TEMPORARY);
					}
				}

				setState(772);
				match(FUNCTION);
				setState(776);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
				case 1:
					{
					setState(773);
					match(IF);
					setState(774);
					match(NOT);
					setState(775);
					match(EXISTS);
					}
					break;
				}
				setState(778);
				multipartIdentifier();
				setState(779);
				match(AS);
				setState(780);
				((CreateFunctionContext)_localctx).className = match(STRING);
				setState(790);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(781);
					match(USING);
					setState(782);
					resource();
					setState(787);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(783);
						match(T__1);
						setState(784);
						resource();
						}
						}
						setState(789);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				}
				break;
			case 38:
				_localctx = new DropFunctionContext(_localctx);
				enterOuterAlt(_localctx, 38);
				{
				setState(792);
				match(DROP);
				setState(794);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==TEMPORARY) {
					{
					setState(793);
					match(TEMPORARY);
					}
				}

				setState(796);
				match(FUNCTION);
				setState(799);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
				case 1:
					{
					setState(797);
					match(IF);
					setState(798);
					match(EXISTS);
					}
					break;
				}
				setState(801);
				multipartIdentifier();
				}
				break;
			case 39:
				_localctx = new ExplainContext(_localctx);
				enterOuterAlt(_localctx, 39);
				{
				setState(802);
				match(EXPLAIN);
				setState(804);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CODEGEN || _la==COST || ((((_la - 89)) & ~0x3f) == 0 && ((1L << (_la - 89)) & ((1L << (EXTENDED - 89)) | (1L << (FORMATTED - 89)) | (1L << (LOGICAL - 89)))) != 0)) {
					{
					setState(803);
					_la = _input.LA(1);
					if ( !(_la==CODEGEN || _la==COST || ((((_la - 89)) & ~0x3f) == 0 && ((1L << (_la - 89)) & ((1L << (EXTENDED - 89)) | (1L << (FORMATTED - 89)) | (1L << (LOGICAL - 89)))) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(806);
				statement();
				}
				break;
			case 40:
				_localctx = new ShowTablesContext(_localctx);
				enterOuterAlt(_localctx, 40);
				{
				setState(807);
				match(SHOW);
				setState(808);
				match(TABLES);
				setState(811);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(809);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(810);
					multipartIdentifier();
					}
				}

				setState(817);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIKE || _la==STRING) {
					{
					setState(814);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LIKE) {
						{
						setState(813);
						match(LIKE);
						}
					}

					setState(816);
					((ShowTablesContext)_localctx).pattern = match(STRING);
					}
				}

				}
				break;
			case 41:
				_localctx = new ShowTableExtendedContext(_localctx);
				enterOuterAlt(_localctx, 41);
				{
				setState(819);
				match(SHOW);
				setState(820);
				match(TABLE);
				setState(821);
				match(EXTENDED);
				setState(824);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(822);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(823);
					((ShowTableExtendedContext)_localctx).ns = multipartIdentifier();
					}
				}

				setState(826);
				match(LIKE);
				setState(827);
				((ShowTableExtendedContext)_localctx).pattern = match(STRING);
				setState(829);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(828);
					partitionSpec();
					}
				}

				}
				break;
			case 42:
				_localctx = new ShowTblPropertiesContext(_localctx);
				enterOuterAlt(_localctx, 42);
				{
				setState(831);
				match(SHOW);
				setState(832);
				match(TBLPROPERTIES);
				setState(833);
				((ShowTblPropertiesContext)_localctx).table = multipartIdentifier();
				setState(838);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(834);
					match(T__0);
					setState(835);
					((ShowTblPropertiesContext)_localctx).key = tablePropertyKey();
					setState(836);
					match(T__2);
					}
				}

				}
				break;
			case 43:
				_localctx = new ShowColumnsContext(_localctx);
				enterOuterAlt(_localctx, 43);
				{
				setState(840);
				match(SHOW);
				setState(841);
				match(COLUMNS);
				setState(842);
				_la = _input.LA(1);
				if ( !(_la==FROM || _la==IN) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(843);
				((ShowColumnsContext)_localctx).table = multipartIdentifier();
				setState(846);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(844);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(845);
					((ShowColumnsContext)_localctx).ns = multipartIdentifier();
					}
				}

				}
				break;
			case 44:
				_localctx = new ShowViewsContext(_localctx);
				enterOuterAlt(_localctx, 44);
				{
				setState(848);
				match(SHOW);
				setState(849);
				match(VIEWS);
				setState(852);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(850);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(851);
					multipartIdentifier();
					}
				}

				setState(858);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIKE || _la==STRING) {
					{
					setState(855);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LIKE) {
						{
						setState(854);
						match(LIKE);
						}
					}

					setState(857);
					((ShowViewsContext)_localctx).pattern = match(STRING);
					}
				}

				}
				break;
			case 45:
				_localctx = new ShowPartitionsContext(_localctx);
				enterOuterAlt(_localctx, 45);
				{
				setState(860);
				match(SHOW);
				setState(861);
				match(PARTITIONS);
				setState(862);
				multipartIdentifier();
				setState(864);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(863);
					partitionSpec();
					}
				}

				}
				break;
			case 46:
				_localctx = new ShowFunctionsContext(_localctx);
				enterOuterAlt(_localctx, 46);
				{
				setState(866);
				match(SHOW);
				setState(868);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
				case 1:
					{
					setState(867);
					identifier();
					}
					break;
				}
				setState(870);
				match(FUNCTIONS);
				setState(878);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,83,_ctx) ) {
				case 1:
					{
					setState(872);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
					case 1:
						{
						setState(871);
						match(LIKE);
						}
						break;
					}
					setState(876);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
					case 1:
						{
						setState(874);
						multipartIdentifier();
						}
						break;
					case 2:
						{
						setState(875);
						((ShowFunctionsContext)_localctx).pattern = match(STRING);
						}
						break;
					}
					}
					break;
				}
				}
				break;
			case 47:
				_localctx = new ShowCreateTableContext(_localctx);
				enterOuterAlt(_localctx, 47);
				{
				setState(880);
				match(SHOW);
				setState(881);
				match(CREATE);
				setState(882);
				match(TABLE);
				setState(883);
				multipartIdentifier();
				setState(886);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(884);
					match(AS);
					setState(885);
					match(SERDE);
					}
				}

				}
				break;
			case 48:
				_localctx = new ShowCurrentNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 48);
				{
				setState(888);
				match(SHOW);
				setState(889);
				match(CURRENT);
				setState(890);
				match(NAMESPACE);
				}
				break;
			case 49:
				_localctx = new DescribeFunctionContext(_localctx);
				enterOuterAlt(_localctx, 49);
				{
				setState(891);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(892);
				match(FUNCTION);
				setState(894);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
				case 1:
					{
					setState(893);
					match(EXTENDED);
					}
					break;
				}
				setState(896);
				describeFuncName();
				}
				break;
			case 50:
				_localctx = new DescribeNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 50);
				{
				setState(897);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(898);
				namespace();
				setState(900);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,86,_ctx) ) {
				case 1:
					{
					setState(899);
					match(EXTENDED);
					}
					break;
				}
				setState(902);
				multipartIdentifier();
				}
				break;
			case 51:
				_localctx = new DescribeRelationContext(_localctx);
				enterOuterAlt(_localctx, 51);
				{
				setState(904);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(906);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
				case 1:
					{
					setState(905);
					match(TABLE);
					}
					break;
				}
				setState(909);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
				case 1:
					{
					setState(908);
					((DescribeRelationContext)_localctx).option = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==EXTENDED || _la==FORMATTED) ) {
						((DescribeRelationContext)_localctx).option = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				}
				setState(911);
				multipartIdentifier();
				setState(913);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,89,_ctx) ) {
				case 1:
					{
					setState(912);
					partitionSpec();
					}
					break;
				}
				setState(916);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
				case 1:
					{
					setState(915);
					describeColName();
					}
					break;
				}
				}
				break;
			case 52:
				_localctx = new DescribeQueryContext(_localctx);
				enterOuterAlt(_localctx, 52);
				{
				setState(918);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(920);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==QUERY) {
					{
					setState(919);
					match(QUERY);
					}
				}

				setState(922);
				query();
				}
				break;
			case 53:
				_localctx = new CommentNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 53);
				{
				setState(923);
				match(COMMENT);
				setState(924);
				match(ON);
				setState(925);
				namespace();
				setState(926);
				multipartIdentifier();
				setState(927);
				match(IS);
				setState(928);
				((CommentNamespaceContext)_localctx).comment = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==NULL || _la==STRING) ) {
					((CommentNamespaceContext)_localctx).comment = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 54:
				_localctx = new CommentTableContext(_localctx);
				enterOuterAlt(_localctx, 54);
				{
				setState(930);
				match(COMMENT);
				setState(931);
				match(ON);
				setState(932);
				match(TABLE);
				setState(933);
				multipartIdentifier();
				setState(934);
				match(IS);
				setState(935);
				((CommentTableContext)_localctx).comment = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==NULL || _la==STRING) ) {
					((CommentTableContext)_localctx).comment = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 55:
				_localctx = new RefreshTableContext(_localctx);
				enterOuterAlt(_localctx, 55);
				{
				setState(937);
				match(REFRESH);
				setState(938);
				match(TABLE);
				setState(939);
				multipartIdentifier();
				}
				break;
			case 56:
				_localctx = new RefreshFunctionContext(_localctx);
				enterOuterAlt(_localctx, 56);
				{
				setState(940);
				match(REFRESH);
				setState(941);
				match(FUNCTION);
				setState(942);
				multipartIdentifier();
				}
				break;
			case 57:
				_localctx = new RefreshResourceContext(_localctx);
				enterOuterAlt(_localctx, 57);
				{
				setState(943);
				match(REFRESH);
				setState(951);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,93,_ctx) ) {
				case 1:
					{
					setState(944);
					match(STRING);
					}
					break;
				case 2:
					{
					setState(948);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,92,_ctx);
					while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
						if ( _alt==1+1 ) {
							{
							{
							setState(945);
							matchWildcard();
							}
							} 
						}
						setState(950);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,92,_ctx);
					}
					}
					break;
				}
				}
				break;
			case 58:
				_localctx = new CacheTableContext(_localctx);
				enterOuterAlt(_localctx, 58);
				{
				setState(953);
				match(CACHE);
				setState(955);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LAZY) {
					{
					setState(954);
					match(LAZY);
					}
				}

				setState(957);
				match(TABLE);
				setState(958);
				multipartIdentifier();
				setState(961);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(959);
					match(OPTIONS);
					setState(960);
					((CacheTableContext)_localctx).options = tablePropertyList();
					}
				}

				setState(967);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0 || _la==AS || _la==FROM || _la==MAP || ((((_la - 190)) & ~0x3f) == 0 && ((1L << (_la - 190)) & ((1L << (REDUCE - 190)) | (1L << (SELECT - 190)) | (1L << (TABLE - 190)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(964);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(963);
						match(AS);
						}
					}

					setState(966);
					query();
					}
				}

				}
				break;
			case 59:
				_localctx = new UncacheTableContext(_localctx);
				enterOuterAlt(_localctx, 59);
				{
				setState(969);
				match(UNCACHE);
				setState(970);
				match(TABLE);
				setState(973);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
				case 1:
					{
					setState(971);
					match(IF);
					setState(972);
					match(EXISTS);
					}
					break;
				}
				setState(975);
				multipartIdentifier();
				}
				break;
			case 60:
				_localctx = new ClearCacheContext(_localctx);
				enterOuterAlt(_localctx, 60);
				{
				setState(976);
				match(CLEAR);
				setState(977);
				match(CACHE);
				}
				break;
			case 61:
				_localctx = new LoadDataContext(_localctx);
				enterOuterAlt(_localctx, 61);
				{
				setState(978);
				match(LOAD);
				setState(979);
				match(DATA);
				setState(981);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(980);
					match(LOCAL);
					}
				}

				setState(983);
				match(INPATH);
				setState(984);
				((LoadDataContext)_localctx).path = match(STRING);
				setState(986);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OVERWRITE) {
					{
					setState(985);
					match(OVERWRITE);
					}
				}

				setState(988);
				match(INTO);
				setState(989);
				match(TABLE);
				setState(990);
				multipartIdentifier();
				setState(992);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(991);
					partitionSpec();
					}
				}

				}
				break;
			case 62:
				_localctx = new TruncateTableContext(_localctx);
				enterOuterAlt(_localctx, 62);
				{
				setState(994);
				match(TRUNCATE);
				setState(995);
				match(TABLE);
				setState(996);
				multipartIdentifier();
				setState(998);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(997);
					partitionSpec();
					}
				}

				}
				break;
			case 63:
				_localctx = new RepairTableContext(_localctx);
				enterOuterAlt(_localctx, 63);
				{
				setState(1000);
				match(MSCK);
				setState(1001);
				match(REPAIR);
				setState(1002);
				match(TABLE);
				setState(1003);
				multipartIdentifier();
				setState(1006);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ADD || _la==DROP || _la==SYNC) {
					{
					setState(1004);
					((RepairTableContext)_localctx).option = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==ADD || _la==DROP || _la==SYNC) ) {
						((RepairTableContext)_localctx).option = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(1005);
					match(PARTITIONS);
					}
				}

				}
				break;
			case 64:
				_localctx = new ManageResourceContext(_localctx);
				enterOuterAlt(_localctx, 64);
				{
				setState(1008);
				((ManageResourceContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ADD || _la==LIST) ) {
					((ManageResourceContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1009);
				identifier();
				setState(1013);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,104,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1010);
						matchWildcard();
						}
						} 
					}
					setState(1015);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,104,_ctx);
				}
				}
				break;
			case 65:
				_localctx = new FailNativeCommandContext(_localctx);
				enterOuterAlt(_localctx, 65);
				{
				setState(1016);
				match(SET);
				setState(1017);
				match(ROLE);
				setState(1021);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1018);
						matchWildcard();
						}
						} 
					}
					setState(1023);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				}
				}
				break;
			case 66:
				_localctx = new SetTimeZoneContext(_localctx);
				enterOuterAlt(_localctx, 66);
				{
				setState(1024);
				match(SET);
				setState(1025);
				match(TIME);
				setState(1026);
				match(ZONE);
				setState(1027);
				interval();
				}
				break;
			case 67:
				_localctx = new SetTimeZoneContext(_localctx);
				enterOuterAlt(_localctx, 67);
				{
				setState(1028);
				match(SET);
				setState(1029);
				match(TIME);
				setState(1030);
				match(ZONE);
				setState(1031);
				((SetTimeZoneContext)_localctx).timezone = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==LOCAL || _la==STRING) ) {
					((SetTimeZoneContext)_localctx).timezone = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 68:
				_localctx = new SetTimeZoneContext(_localctx);
				enterOuterAlt(_localctx, 68);
				{
				setState(1032);
				match(SET);
				setState(1033);
				match(TIME);
				setState(1034);
				match(ZONE);
				setState(1038);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,106,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1035);
						matchWildcard();
						}
						} 
					}
					setState(1040);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,106,_ctx);
				}
				}
				break;
			case 69:
				_localctx = new SetQuotedConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 69);
				{
				setState(1041);
				match(SET);
				setState(1042);
				configKey();
				setState(1043);
				match(EQ);
				setState(1044);
				configValue();
				}
				break;
			case 70:
				_localctx = new SetConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 70);
				{
				setState(1046);
				match(SET);
				setState(1047);
				configKey();
				setState(1055);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(1048);
					match(EQ);
					setState(1052);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,107,_ctx);
					while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
						if ( _alt==1+1 ) {
							{
							{
							setState(1049);
							matchWildcard();
							}
							} 
						}
						setState(1054);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,107,_ctx);
					}
					}
				}

				}
				break;
			case 71:
				_localctx = new SetQuotedConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 71);
				{
				setState(1057);
				match(SET);
				setState(1061);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1058);
						matchWildcard();
						}
						} 
					}
					setState(1063);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
				}
				setState(1064);
				match(EQ);
				setState(1065);
				configValue();
				}
				break;
			case 72:
				_localctx = new SetConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 72);
				{
				setState(1066);
				match(SET);
				setState(1070);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,110,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1067);
						matchWildcard();
						}
						} 
					}
					setState(1072);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,110,_ctx);
				}
				}
				break;
			case 73:
				_localctx = new ResetQuotedConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 73);
				{
				setState(1073);
				match(RESET);
				setState(1074);
				configKey();
				}
				break;
			case 74:
				_localctx = new ResetConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 74);
				{
				setState(1075);
				match(RESET);
				setState(1079);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,111,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1076);
						matchWildcard();
						}
						} 
					}
					setState(1081);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,111,_ctx);
				}
				}
				break;
			case 75:
				_localctx = new FailNativeCommandContext(_localctx);
				enterOuterAlt(_localctx, 75);
				{
				setState(1082);
				unsupportedHiveNativeCommands();
				setState(1086);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,112,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1083);
						matchWildcard();
						}
						} 
					}
					setState(1088);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,112,_ctx);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConfigKeyContext extends ParserRuleContext {
		public QuotedIdentifierContext quotedIdentifier() {
			return getRuleContext(QuotedIdentifierContext.class,0);
		}
		public ConfigKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_configKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterConfigKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitConfigKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitConfigKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConfigKeyContext configKey() throws RecognitionException {
		ConfigKeyContext _localctx = new ConfigKeyContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_configKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1091);
			quotedIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConfigValueContext extends ParserRuleContext {
		public QuotedIdentifierContext quotedIdentifier() {
			return getRuleContext(QuotedIdentifierContext.class,0);
		}
		public ConfigValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_configValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterConfigValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitConfigValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitConfigValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConfigValueContext configValue() throws RecognitionException {
		ConfigValueContext _localctx = new ConfigValueContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_configValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1093);
			quotedIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnsupportedHiveNativeCommandsContext extends ParserRuleContext {
		public Token kw1;
		public Token kw2;
		public Token kw3;
		public Token kw4;
		public Token kw5;
		public Token kw6;
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticExtendSparkSqlParser.ROLE, 0); }
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode GRANT() { return getToken(ArcticExtendSparkSqlParser.GRANT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticExtendSparkSqlParser.REVOKE, 0); }
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticExtendSparkSqlParser.PRINCIPALS, 0); }
		public TerminalNode ROLES() { return getToken(ArcticExtendSparkSqlParser.ROLES, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticExtendSparkSqlParser.CURRENT, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticExtendSparkSqlParser.EXPORT, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticExtendSparkSqlParser.IMPORT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticExtendSparkSqlParser.COMPACTIONS, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticExtendSparkSqlParser.TRANSACTIONS, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticExtendSparkSqlParser.INDEXES, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticExtendSparkSqlParser.LOCKS, 0); }
		public TerminalNode INDEX() { return getToken(ArcticExtendSparkSqlParser.INDEX, 0); }
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode LOCK() { return getToken(ArcticExtendSparkSqlParser.LOCK, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticExtendSparkSqlParser.DATABASE, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticExtendSparkSqlParser.UNLOCK, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode MACRO() { return getToken(ArcticExtendSparkSqlParser.MACRO, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticExtendSparkSqlParser.CLUSTERED, 0); }
		public TerminalNode BY() { return getToken(ArcticExtendSparkSqlParser.BY, 0); }
		public TerminalNode SORTED() { return getToken(ArcticExtendSparkSqlParser.SORTED, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticExtendSparkSqlParser.SKEWED, 0); }
		public TerminalNode STORED() { return getToken(ArcticExtendSparkSqlParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticExtendSparkSqlParser.DIRECTORIES, 0); }
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticExtendSparkSqlParser.LOCATION, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticExtendSparkSqlParser.EXCHANGE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticExtendSparkSqlParser.PARTITION, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticExtendSparkSqlParser.ARCHIVE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticExtendSparkSqlParser.UNARCHIVE, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticExtendSparkSqlParser.TOUCH, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticExtendSparkSqlParser.COMPACT, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode CONCATENATE() { return getToken(ArcticExtendSparkSqlParser.CONCATENATE, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticExtendSparkSqlParser.FILEFORMAT, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public TerminalNode START() { return getToken(ArcticExtendSparkSqlParser.START, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticExtendSparkSqlParser.TRANSACTION, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticExtendSparkSqlParser.COMMIT, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticExtendSparkSqlParser.ROLLBACK, 0); }
		public TerminalNode DFS() { return getToken(ArcticExtendSparkSqlParser.DFS, 0); }
		public UnsupportedHiveNativeCommandsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unsupportedHiveNativeCommands; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUnsupportedHiveNativeCommands(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUnsupportedHiveNativeCommands(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUnsupportedHiveNativeCommands(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnsupportedHiveNativeCommandsContext unsupportedHiveNativeCommands() throws RecognitionException {
		UnsupportedHiveNativeCommandsContext _localctx = new UnsupportedHiveNativeCommandsContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_unsupportedHiveNativeCommands);
		int _la;
		try {
			setState(1263);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,121,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1095);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(1096);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1097);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(1098);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1099);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(GRANT);
				setState(1101);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,114,_ctx) ) {
				case 1:
					{
					setState(1100);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
					}
					break;
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1103);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(REVOKE);
				setState(1105);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
				case 1:
					{
					setState(1104);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
					}
					break;
				}
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1107);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1108);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(GRANT);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1109);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1110);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				setState(1112);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,116,_ctx) ) {
				case 1:
					{
					setState(1111);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(GRANT);
					}
					break;
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1114);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1115);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(PRINCIPALS);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1116);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1117);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLES);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1118);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1119);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(CURRENT);
				setState(1120);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(ROLES);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1121);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(EXPORT);
				setState(1122);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1123);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(IMPORT);
				setState(1124);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1125);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1126);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(COMPACTIONS);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1127);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1128);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(CREATE);
				setState(1129);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(TABLE);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1130);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1131);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TRANSACTIONS);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1132);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1133);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEXES);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1134);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1135);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(LOCKS);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1136);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(1137);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(1138);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(1139);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(1140);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1141);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(1142);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(LOCK);
				setState(1143);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(1144);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(LOCK);
				setState(1145);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(DATABASE);
				}
				break;
			case 22:
				enterOuterAlt(_localctx, 22);
				{
				setState(1146);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(UNLOCK);
				setState(1147);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 23:
				enterOuterAlt(_localctx, 23);
				{
				setState(1148);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(UNLOCK);
				setState(1149);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(DATABASE);
				}
				break;
			case 24:
				enterOuterAlt(_localctx, 24);
				{
				setState(1150);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(1151);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TEMPORARY);
				setState(1152);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(MACRO);
				}
				break;
			case 25:
				enterOuterAlt(_localctx, 25);
				{
				setState(1153);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(1154);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TEMPORARY);
				setState(1155);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(MACRO);
				}
				break;
			case 26:
				enterOuterAlt(_localctx, 26);
				{
				setState(1156);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1157);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1158);
				tableIdentifier();
				setState(1159);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1160);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(CLUSTERED);
				}
				break;
			case 27:
				enterOuterAlt(_localctx, 27);
				{
				setState(1162);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1163);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1164);
				tableIdentifier();
				setState(1165);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(CLUSTERED);
				setState(1166);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(BY);
				}
				break;
			case 28:
				enterOuterAlt(_localctx, 28);
				{
				setState(1168);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1169);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1170);
				tableIdentifier();
				setState(1171);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1172);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SORTED);
				}
				break;
			case 29:
				enterOuterAlt(_localctx, 29);
				{
				setState(1174);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1175);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1176);
				tableIdentifier();
				setState(1177);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SKEWED);
				setState(1178);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(BY);
				}
				break;
			case 30:
				enterOuterAlt(_localctx, 30);
				{
				setState(1180);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1181);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1182);
				tableIdentifier();
				setState(1183);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1184);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SKEWED);
				}
				break;
			case 31:
				enterOuterAlt(_localctx, 31);
				{
				setState(1186);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1187);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1188);
				tableIdentifier();
				setState(1189);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1190);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(STORED);
				setState(1191);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw5 = match(AS);
				setState(1192);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw6 = match(DIRECTORIES);
				}
				break;
			case 32:
				enterOuterAlt(_localctx, 32);
				{
				setState(1194);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1195);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1196);
				tableIdentifier();
				setState(1197);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SET);
				setState(1198);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SKEWED);
				setState(1199);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw5 = match(LOCATION);
				}
				break;
			case 33:
				enterOuterAlt(_localctx, 33);
				{
				setState(1201);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1202);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1203);
				tableIdentifier();
				setState(1204);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(EXCHANGE);
				setState(1205);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 34:
				enterOuterAlt(_localctx, 34);
				{
				setState(1207);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1208);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1209);
				tableIdentifier();
				setState(1210);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(ARCHIVE);
				setState(1211);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 35:
				enterOuterAlt(_localctx, 35);
				{
				setState(1213);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1214);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1215);
				tableIdentifier();
				setState(1216);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(UNARCHIVE);
				setState(1217);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 36:
				enterOuterAlt(_localctx, 36);
				{
				setState(1219);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1220);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1221);
				tableIdentifier();
				setState(1222);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(TOUCH);
				}
				break;
			case 37:
				enterOuterAlt(_localctx, 37);
				{
				setState(1224);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1225);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1226);
				tableIdentifier();
				setState(1228);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1227);
					partitionSpec();
					}
				}

				setState(1230);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(COMPACT);
				}
				break;
			case 38:
				enterOuterAlt(_localctx, 38);
				{
				setState(1232);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1233);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1234);
				tableIdentifier();
				setState(1236);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1235);
					partitionSpec();
					}
				}

				setState(1238);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(CONCATENATE);
				}
				break;
			case 39:
				enterOuterAlt(_localctx, 39);
				{
				setState(1240);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1241);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1242);
				tableIdentifier();
				setState(1244);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1243);
					partitionSpec();
					}
				}

				setState(1246);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SET);
				setState(1247);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(FILEFORMAT);
				}
				break;
			case 40:
				enterOuterAlt(_localctx, 40);
				{
				setState(1249);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1250);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1251);
				tableIdentifier();
				setState(1253);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1252);
					partitionSpec();
					}
				}

				setState(1255);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(REPLACE);
				setState(1256);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(COLUMNS);
				}
				break;
			case 41:
				enterOuterAlt(_localctx, 41);
				{
				setState(1258);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(START);
				setState(1259);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TRANSACTION);
				}
				break;
			case 42:
				enterOuterAlt(_localctx, 42);
				{
				setState(1260);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(COMMIT);
				}
				break;
			case 43:
				enterOuterAlt(_localctx, 43);
				{
				setState(1261);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ROLLBACK);
				}
				break;
			case 44:
				enterOuterAlt(_localctx, 44);
				{
				setState(1262);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DFS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreateTableHeaderContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticExtendSparkSqlParser.EXTERNAL, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public CreateTableHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTableHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTableHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTableHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableHeaderContext createTableHeader() throws RecognitionException {
		CreateTableHeaderContext _localctx = new CreateTableHeaderContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_createTableHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1265);
			match(CREATE);
			setState(1267);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TEMPORARY) {
				{
				setState(1266);
				match(TEMPORARY);
				}
			}

			setState(1270);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTERNAL) {
				{
				setState(1269);
				match(EXTERNAL);
				}
			}

			setState(1272);
			match(TABLE);
			setState(1276);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,124,_ctx) ) {
			case 1:
				{
				setState(1273);
				match(IF);
				setState(1274);
				match(NOT);
				setState(1275);
				match(EXISTS);
				}
				break;
			}
			setState(1278);
			multipartIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReplaceTableHeaderContext extends ParserRuleContext {
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public ReplaceTableHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceTableHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterReplaceTableHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitReplaceTableHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitReplaceTableHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceTableHeaderContext replaceTableHeader() throws RecognitionException {
		ReplaceTableHeaderContext _localctx = new ReplaceTableHeaderContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_replaceTableHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1282);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CREATE) {
				{
				setState(1280);
				match(CREATE);
				setState(1281);
				match(OR);
				}
			}

			setState(1284);
			match(REPLACE);
			setState(1285);
			match(TABLE);
			setState(1286);
			multipartIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BucketSpecContext extends ParserRuleContext {
		public TerminalNode CLUSTERED() { return getToken(ArcticExtendSparkSqlParser.CLUSTERED, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticExtendSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticExtendSparkSqlParser.BY, i);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode INTO() { return getToken(ArcticExtendSparkSqlParser.INTO, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticExtendSparkSqlParser.BUCKETS, 0); }
		public TerminalNode SORTED() { return getToken(ArcticExtendSparkSqlParser.SORTED, 0); }
		public OrderedIdentifierListContext orderedIdentifierList() {
			return getRuleContext(OrderedIdentifierListContext.class,0);
		}
		public BucketSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bucketSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterBucketSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitBucketSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitBucketSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BucketSpecContext bucketSpec() throws RecognitionException {
		BucketSpecContext _localctx = new BucketSpecContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_bucketSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1288);
			match(CLUSTERED);
			setState(1289);
			match(BY);
			setState(1290);
			identifierList();
			setState(1294);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SORTED) {
				{
				setState(1291);
				match(SORTED);
				setState(1292);
				match(BY);
				setState(1293);
				orderedIdentifierList();
				}
			}

			setState(1296);
			match(INTO);
			setState(1297);
			match(INTEGER_VALUE);
			setState(1298);
			match(BUCKETS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SkewSpecContext extends ParserRuleContext {
		public TerminalNode SKEWED() { return getToken(ArcticExtendSparkSqlParser.SKEWED, 0); }
		public TerminalNode BY() { return getToken(ArcticExtendSparkSqlParser.BY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public ConstantListContext constantList() {
			return getRuleContext(ConstantListContext.class,0);
		}
		public NestedConstantListContext nestedConstantList() {
			return getRuleContext(NestedConstantListContext.class,0);
		}
		public TerminalNode STORED() { return getToken(ArcticExtendSparkSqlParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticExtendSparkSqlParser.DIRECTORIES, 0); }
		public SkewSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skewSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSkewSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSkewSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSkewSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SkewSpecContext skewSpec() throws RecognitionException {
		SkewSpecContext _localctx = new SkewSpecContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_skewSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1300);
			match(SKEWED);
			setState(1301);
			match(BY);
			setState(1302);
			identifierList();
			setState(1303);
			match(ON);
			setState(1306);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,127,_ctx) ) {
			case 1:
				{
				setState(1304);
				constantList();
				}
				break;
			case 2:
				{
				setState(1305);
				nestedConstantList();
				}
				break;
			}
			setState(1311);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,128,_ctx) ) {
			case 1:
				{
				setState(1308);
				match(STORED);
				setState(1309);
				match(AS);
				setState(1310);
				match(DIRECTORIES);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LocationSpecContext extends ParserRuleContext {
		public TerminalNode LOCATION() { return getToken(ArcticExtendSparkSqlParser.LOCATION, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public LocationSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_locationSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLocationSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLocationSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLocationSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocationSpecContext locationSpec() throws RecognitionException {
		LocationSpecContext _localctx = new LocationSpecContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_locationSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1313);
			match(LOCATION);
			setState(1314);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CommentSpecContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(ArcticExtendSparkSqlParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public CommentSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commentSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCommentSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCommentSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCommentSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommentSpecContext commentSpec() throws RecognitionException {
		CommentSpecContext _localctx = new CommentSpecContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_commentSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1316);
			match(COMMENT);
			setState(1317);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryContext extends ParserRuleContext {
		public QueryTermContext queryTerm() {
			return getRuleContext(QueryTermContext.class,0);
		}
		public QueryOrganizationContext queryOrganization() {
			return getRuleContext(QueryOrganizationContext.class,0);
		}
		public CtesContext ctes() {
			return getRuleContext(CtesContext.class,0);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1320);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(1319);
				ctes();
				}
			}

			setState(1322);
			queryTerm(0);
			setState(1323);
			queryOrganization();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InsertIntoContext extends ParserRuleContext {
		public InsertIntoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertInto; }
	 
		public InsertIntoContext() { }
		public void copyFrom(InsertIntoContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class InsertOverwriteHiveDirContext extends InsertIntoContext {
		public Token path;
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticExtendSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticExtendSparkSqlParser.DIRECTORY, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticExtendSparkSqlParser.LOCAL, 0); }
		public RowFormatContext rowFormat() {
			return getRuleContext(RowFormatContext.class,0);
		}
		public CreateFileFormatContext createFileFormat() {
			return getRuleContext(CreateFileFormatContext.class,0);
		}
		public InsertOverwriteHiveDirContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInsertOverwriteHiveDir(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInsertOverwriteHiveDir(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInsertOverwriteHiveDir(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertOverwriteDirContext extends InsertIntoContext {
		public Token path;
		public TablePropertyListContext options;
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticExtendSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticExtendSparkSqlParser.DIRECTORY, 0); }
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public TerminalNode LOCAL() { return getToken(ArcticExtendSparkSqlParser.LOCAL, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticExtendSparkSqlParser.OPTIONS, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public InsertOverwriteDirContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInsertOverwriteDir(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInsertOverwriteDir(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInsertOverwriteDir(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertOverwriteTableContext extends InsertIntoContext {
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticExtendSparkSqlParser.OVERWRITE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public InsertOverwriteTableContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInsertOverwriteTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInsertOverwriteTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInsertOverwriteTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertIntoTableContext extends InsertIntoContext {
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode INTO() { return getToken(ArcticExtendSparkSqlParser.INTO, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public InsertIntoTableContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInsertIntoTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInsertIntoTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInsertIntoTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertIntoContext insertInto() throws RecognitionException {
		InsertIntoContext _localctx = new InsertIntoContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_insertInto);
		int _la;
		try {
			setState(1386);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
			case 1:
				_localctx = new InsertOverwriteTableContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1325);
				match(INSERT);
				setState(1326);
				match(OVERWRITE);
				setState(1328);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,130,_ctx) ) {
				case 1:
					{
					setState(1327);
					match(TABLE);
					}
					break;
				}
				setState(1330);
				multipartIdentifier();
				setState(1337);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1331);
					partitionSpec();
					setState(1335);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==IF) {
						{
						setState(1332);
						match(IF);
						setState(1333);
						match(NOT);
						setState(1334);
						match(EXISTS);
						}
					}

					}
				}

				setState(1340);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,133,_ctx) ) {
				case 1:
					{
					setState(1339);
					identifierList();
					}
					break;
				}
				}
				break;
			case 2:
				_localctx = new InsertIntoTableContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1342);
				match(INSERT);
				setState(1343);
				match(INTO);
				setState(1345);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,134,_ctx) ) {
				case 1:
					{
					setState(1344);
					match(TABLE);
					}
					break;
				}
				setState(1347);
				multipartIdentifier();
				setState(1349);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1348);
					partitionSpec();
					}
				}

				setState(1354);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(1351);
					match(IF);
					setState(1352);
					match(NOT);
					setState(1353);
					match(EXISTS);
					}
				}

				setState(1357);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,137,_ctx) ) {
				case 1:
					{
					setState(1356);
					identifierList();
					}
					break;
				}
				}
				break;
			case 3:
				_localctx = new InsertOverwriteHiveDirContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1359);
				match(INSERT);
				setState(1360);
				match(OVERWRITE);
				setState(1362);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(1361);
					match(LOCAL);
					}
				}

				setState(1364);
				match(DIRECTORY);
				setState(1365);
				((InsertOverwriteHiveDirContext)_localctx).path = match(STRING);
				setState(1367);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ROW) {
					{
					setState(1366);
					rowFormat();
					}
				}

				setState(1370);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STORED) {
					{
					setState(1369);
					createFileFormat();
					}
				}

				}
				break;
			case 4:
				_localctx = new InsertOverwriteDirContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1372);
				match(INSERT);
				setState(1373);
				match(OVERWRITE);
				setState(1375);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(1374);
					match(LOCAL);
					}
				}

				setState(1377);
				match(DIRECTORY);
				setState(1379);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STRING) {
					{
					setState(1378);
					((InsertOverwriteDirContext)_localctx).path = match(STRING);
					}
				}

				setState(1381);
				tableProvider();
				setState(1384);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(1382);
					match(OPTIONS);
					setState(1383);
					((InsertOverwriteDirContext)_localctx).options = tablePropertyList();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PartitionSpecLocationContext extends ParserRuleContext {
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public LocationSpecContext locationSpec() {
			return getRuleContext(LocationSpecContext.class,0);
		}
		public PartitionSpecLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionSpecLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPartitionSpecLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPartitionSpecLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPartitionSpecLocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionSpecLocationContext partitionSpecLocation() throws RecognitionException {
		PartitionSpecLocationContext _localctx = new PartitionSpecLocationContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_partitionSpecLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1388);
			partitionSpec();
			setState(1390);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LOCATION) {
				{
				setState(1389);
				locationSpec();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PartitionSpecContext extends ParserRuleContext {
		public TerminalNode PARTITION() { return getToken(ArcticExtendSparkSqlParser.PARTITION, 0); }
		public List<PartitionValContext> partitionVal() {
			return getRuleContexts(PartitionValContext.class);
		}
		public PartitionValContext partitionVal(int i) {
			return getRuleContext(PartitionValContext.class,i);
		}
		public PartitionSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPartitionSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPartitionSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPartitionSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionSpecContext partitionSpec() throws RecognitionException {
		PartitionSpecContext _localctx = new PartitionSpecContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_partitionSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1392);
			match(PARTITION);
			setState(1393);
			match(T__0);
			setState(1394);
			partitionVal();
			setState(1399);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1395);
				match(T__1);
				setState(1396);
				partitionVal();
				}
				}
				setState(1401);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1402);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PartitionValContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticExtendSparkSqlParser.EQ, 0); }
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public PartitionValContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionVal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPartitionVal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPartitionVal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPartitionVal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionValContext partitionVal() throws RecognitionException {
		PartitionValContext _localctx = new PartitionValContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_partitionVal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1404);
			identifier();
			setState(1407);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EQ) {
				{
				setState(1405);
				match(EQ);
				setState(1406);
				constant();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamespaceContext extends ParserRuleContext {
		public TerminalNode NAMESPACE() { return getToken(ArcticExtendSparkSqlParser.NAMESPACE, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticExtendSparkSqlParser.DATABASE, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticExtendSparkSqlParser.SCHEMA, 0); }
		public NamespaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNamespace(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamespaceContext namespace() throws RecognitionException {
		NamespaceContext _localctx = new NamespaceContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_namespace);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1409);
			_la = _input.LA(1);
			if ( !(_la==DATABASE || _la==NAMESPACE || _la==SCHEMA) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DescribeFuncNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public ComparisonOperatorContext comparisonOperator() {
			return getRuleContext(ComparisonOperatorContext.class,0);
		}
		public ArithmeticOperatorContext arithmeticOperator() {
			return getRuleContext(ArithmeticOperatorContext.class,0);
		}
		public PredicateOperatorContext predicateOperator() {
			return getRuleContext(PredicateOperatorContext.class,0);
		}
		public DescribeFuncNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_describeFuncName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDescribeFuncName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDescribeFuncName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDescribeFuncName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeFuncNameContext describeFuncName() throws RecognitionException {
		DescribeFuncNameContext _localctx = new DescribeFuncNameContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_describeFuncName);
		try {
			setState(1416);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,148,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1411);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1412);
				match(STRING);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1413);
				comparisonOperator();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1414);
				arithmeticOperator();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1415);
				predicateOperator();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DescribeColNameContext extends ParserRuleContext {
		public IdentifierContext identifier;
		public List<IdentifierContext> nameParts = new ArrayList<IdentifierContext>();
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public DescribeColNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_describeColName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDescribeColName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDescribeColName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDescribeColName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeColNameContext describeColName() throws RecognitionException {
		DescribeColNameContext _localctx = new DescribeColNameContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_describeColName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1418);
			((DescribeColNameContext)_localctx).identifier = identifier();
			((DescribeColNameContext)_localctx).nameParts.add(((DescribeColNameContext)_localctx).identifier);
			setState(1423);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__4) {
				{
				{
				setState(1419);
				match(T__4);
				setState(1420);
				((DescribeColNameContext)_localctx).identifier = identifier();
				((DescribeColNameContext)_localctx).nameParts.add(((DescribeColNameContext)_localctx).identifier);
				}
				}
				setState(1425);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CtesContext extends ParserRuleContext {
		public TerminalNode WITH() { return getToken(ArcticExtendSparkSqlParser.WITH, 0); }
		public List<NamedQueryContext> namedQuery() {
			return getRuleContexts(NamedQueryContext.class);
		}
		public NamedQueryContext namedQuery(int i) {
			return getRuleContext(NamedQueryContext.class,i);
		}
		public CtesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ctes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCtes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCtes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCtes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtesContext ctes() throws RecognitionException {
		CtesContext _localctx = new CtesContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_ctes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1426);
			match(WITH);
			setState(1427);
			namedQuery();
			setState(1432);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1428);
				match(T__1);
				setState(1429);
				namedQuery();
				}
				}
				setState(1434);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedQueryContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext name;
		public IdentifierListContext columnAliases;
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public NamedQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNamedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNamedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNamedQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedQueryContext namedQuery() throws RecognitionException {
		NamedQueryContext _localctx = new NamedQueryContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_namedQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1435);
			((NamedQueryContext)_localctx).name = errorCapturingIdentifier();
			setState(1437);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,151,_ctx) ) {
			case 1:
				{
				setState(1436);
				((NamedQueryContext)_localctx).columnAliases = identifierList();
				}
				break;
			}
			setState(1440);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(1439);
				match(AS);
				}
			}

			setState(1442);
			match(T__0);
			setState(1443);
			query();
			setState(1444);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TableProviderContext extends ParserRuleContext {
		public TerminalNode USING() { return getToken(ArcticExtendSparkSqlParser.USING, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableProviderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProvider; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableProvider(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableProvider(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableProvider(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableProviderContext tableProvider() throws RecognitionException {
		TableProviderContext _localctx = new TableProviderContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_tableProvider);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1446);
			match(USING);
			setState(1447);
			multipartIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreateTableClausesContext extends ParserRuleContext {
		public TablePropertyListContext options;
		public PartitionFieldListContext partitioning;
		public TablePropertyListContext tableProps;
		public List<SkewSpecContext> skewSpec() {
			return getRuleContexts(SkewSpecContext.class);
		}
		public SkewSpecContext skewSpec(int i) {
			return getRuleContext(SkewSpecContext.class,i);
		}
		public List<BucketSpecContext> bucketSpec() {
			return getRuleContexts(BucketSpecContext.class);
		}
		public BucketSpecContext bucketSpec(int i) {
			return getRuleContext(BucketSpecContext.class,i);
		}
		public List<RowFormatContext> rowFormat() {
			return getRuleContexts(RowFormatContext.class);
		}
		public RowFormatContext rowFormat(int i) {
			return getRuleContext(RowFormatContext.class,i);
		}
		public List<CreateFileFormatContext> createFileFormat() {
			return getRuleContexts(CreateFileFormatContext.class);
		}
		public CreateFileFormatContext createFileFormat(int i) {
			return getRuleContext(CreateFileFormatContext.class,i);
		}
		public List<LocationSpecContext> locationSpec() {
			return getRuleContexts(LocationSpecContext.class);
		}
		public LocationSpecContext locationSpec(int i) {
			return getRuleContext(LocationSpecContext.class,i);
		}
		public List<CommentSpecContext> commentSpec() {
			return getRuleContexts(CommentSpecContext.class);
		}
		public CommentSpecContext commentSpec(int i) {
			return getRuleContext(CommentSpecContext.class,i);
		}
		public List<TerminalNode> OPTIONS() { return getTokens(ArcticExtendSparkSqlParser.OPTIONS); }
		public TerminalNode OPTIONS(int i) {
			return getToken(ArcticExtendSparkSqlParser.OPTIONS, i);
		}
		public List<TerminalNode> PARTITIONED() { return getTokens(ArcticExtendSparkSqlParser.PARTITIONED); }
		public TerminalNode PARTITIONED(int i) {
			return getToken(ArcticExtendSparkSqlParser.PARTITIONED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticExtendSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticExtendSparkSqlParser.BY, i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticExtendSparkSqlParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, i);
		}
		public List<TablePropertyListContext> tablePropertyList() {
			return getRuleContexts(TablePropertyListContext.class);
		}
		public TablePropertyListContext tablePropertyList(int i) {
			return getRuleContext(TablePropertyListContext.class,i);
		}
		public List<PartitionFieldListContext> partitionFieldList() {
			return getRuleContexts(PartitionFieldListContext.class);
		}
		public PartitionFieldListContext partitionFieldList(int i) {
			return getRuleContext(PartitionFieldListContext.class,i);
		}
		public CreateTableClausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableClauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateTableClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateTableClauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateTableClauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableClausesContext createTableClauses() throws RecognitionException {
		CreateTableClausesContext _localctx = new CreateTableClausesContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_createTableClauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==CLUSTERED || _la==COMMENT || ((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (LOCATION - 141)) | (1L << (OPTIONS - 141)) | (1L << (PARTITIONED - 141)))) != 0) || ((((_la - 206)) & ~0x3f) == 0 && ((1L << (_la - 206)) & ((1L << (ROW - 206)) | (1L << (SKEWED - 206)) | (1L << (STORED - 206)) | (1L << (TBLPROPERTIES - 206)))) != 0)) {
				{
				setState(1462);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTIONS:
					{
					{
					setState(1449);
					match(OPTIONS);
					setState(1450);
					((CreateTableClausesContext)_localctx).options = tablePropertyList();
					}
					}
					break;
				case PARTITIONED:
					{
					{
					setState(1451);
					match(PARTITIONED);
					setState(1452);
					match(BY);
					setState(1453);
					((CreateTableClausesContext)_localctx).partitioning = partitionFieldList();
					}
					}
					break;
				case SKEWED:
					{
					setState(1454);
					skewSpec();
					}
					break;
				case CLUSTERED:
					{
					setState(1455);
					bucketSpec();
					}
					break;
				case ROW:
					{
					setState(1456);
					rowFormat();
					}
					break;
				case STORED:
					{
					setState(1457);
					createFileFormat();
					}
					break;
				case LOCATION:
					{
					setState(1458);
					locationSpec();
					}
					break;
				case COMMENT:
					{
					setState(1459);
					commentSpec();
					}
					break;
				case TBLPROPERTIES:
					{
					{
					setState(1460);
					match(TBLPROPERTIES);
					setState(1461);
					((CreateTableClausesContext)_localctx).tableProps = tablePropertyList();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(1466);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TablePropertyListContext extends ParserRuleContext {
		public List<TablePropertyContext> tableProperty() {
			return getRuleContexts(TablePropertyContext.class);
		}
		public TablePropertyContext tableProperty(int i) {
			return getRuleContext(TablePropertyContext.class,i);
		}
		public TablePropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTablePropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTablePropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTablePropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyListContext tablePropertyList() throws RecognitionException {
		TablePropertyListContext _localctx = new TablePropertyListContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_tablePropertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1467);
			match(T__0);
			setState(1468);
			tableProperty();
			setState(1473);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1469);
				match(T__1);
				setState(1470);
				tableProperty();
				}
				}
				setState(1475);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1476);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TablePropertyContext extends ParserRuleContext {
		public TablePropertyKeyContext key;
		public TablePropertyValueContext value;
		public TablePropertyKeyContext tablePropertyKey() {
			return getRuleContext(TablePropertyKeyContext.class,0);
		}
		public TablePropertyValueContext tablePropertyValue() {
			return getRuleContext(TablePropertyValueContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticExtendSparkSqlParser.EQ, 0); }
		public TablePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyContext tableProperty() throws RecognitionException {
		TablePropertyContext _localctx = new TablePropertyContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_tableProperty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1478);
			((TablePropertyContext)_localctx).key = tablePropertyKey();
			setState(1483);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FALSE || ((((_la - 247)) & ~0x3f) == 0 && ((1L << (_la - 247)) & ((1L << (TRUE - 247)) | (1L << (EQ - 247)) | (1L << (STRING - 247)) | (1L << (INTEGER_VALUE - 247)) | (1L << (DECIMAL_VALUE - 247)))) != 0)) {
				{
				setState(1480);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(1479);
					match(EQ);
					}
				}

				setState(1482);
				((TablePropertyContext)_localctx).value = tablePropertyValue();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TablePropertyKeyContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TablePropertyKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTablePropertyKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTablePropertyKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTablePropertyKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyKeyContext tablePropertyKey() throws RecognitionException {
		TablePropertyKeyContext _localctx = new TablePropertyKeyContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_tablePropertyKey);
		int _la;
		try {
			setState(1494);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,159,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1485);
				identifier();
				setState(1490);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__4) {
					{
					{
					setState(1486);
					match(T__4);
					setState(1487);
					identifier();
					}
					}
					setState(1492);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1493);
				match(STRING);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TablePropertyValueContext extends ParserRuleContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticExtendSparkSqlParser.DECIMAL_VALUE, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TablePropertyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTablePropertyValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTablePropertyValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTablePropertyValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyValueContext tablePropertyValue() throws RecognitionException {
		TablePropertyValueContext _localctx = new TablePropertyValueContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_tablePropertyValue);
		try {
			setState(1500);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1496);
				match(INTEGER_VALUE);
				}
				break;
			case DECIMAL_VALUE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1497);
				match(DECIMAL_VALUE);
				}
				break;
			case FALSE:
			case TRUE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1498);
				booleanValue();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(1499);
				match(STRING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstantListContext extends ParserRuleContext {
		public List<ConstantContext> constant() {
			return getRuleContexts(ConstantContext.class);
		}
		public ConstantContext constant(int i) {
			return getRuleContext(ConstantContext.class,i);
		}
		public ConstantListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantListContext constantList() throws RecognitionException {
		ConstantListContext _localctx = new ConstantListContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_constantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1502);
			match(T__0);
			setState(1503);
			constant();
			setState(1508);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1504);
				match(T__1);
				setState(1505);
				constant();
				}
				}
				setState(1510);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1511);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NestedConstantListContext extends ParserRuleContext {
		public List<ConstantListContext> constantList() {
			return getRuleContexts(ConstantListContext.class);
		}
		public ConstantListContext constantList(int i) {
			return getRuleContext(ConstantListContext.class,i);
		}
		public NestedConstantListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nestedConstantList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNestedConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNestedConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNestedConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NestedConstantListContext nestedConstantList() throws RecognitionException {
		NestedConstantListContext _localctx = new NestedConstantListContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_nestedConstantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1513);
			match(T__0);
			setState(1514);
			constantList();
			setState(1519);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1515);
				match(T__1);
				setState(1516);
				constantList();
				}
				}
				setState(1521);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1522);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreateFileFormatContext extends ParserRuleContext {
		public TerminalNode STORED() { return getToken(ArcticExtendSparkSqlParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public FileFormatContext fileFormat() {
			return getRuleContext(FileFormatContext.class,0);
		}
		public TerminalNode BY() { return getToken(ArcticExtendSparkSqlParser.BY, 0); }
		public StorageHandlerContext storageHandler() {
			return getRuleContext(StorageHandlerContext.class,0);
		}
		public CreateFileFormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createFileFormat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCreateFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCreateFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCreateFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateFileFormatContext createFileFormat() throws RecognitionException {
		CreateFileFormatContext _localctx = new CreateFileFormatContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_createFileFormat);
		try {
			setState(1530);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1524);
				match(STORED);
				setState(1525);
				match(AS);
				setState(1526);
				fileFormat();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1527);
				match(STORED);
				setState(1528);
				match(BY);
				setState(1529);
				storageHandler();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FileFormatContext extends ParserRuleContext {
		public FileFormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fileFormat; }
	 
		public FileFormatContext() { }
		public void copyFrom(FileFormatContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TableFileFormatContext extends FileFormatContext {
		public Token inFmt;
		public Token outFmt;
		public TerminalNode INPUTFORMAT() { return getToken(ArcticExtendSparkSqlParser.INPUTFORMAT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticExtendSparkSqlParser.OUTPUTFORMAT, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticExtendSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticExtendSparkSqlParser.STRING, i);
		}
		public TableFileFormatContext(FileFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GenericFileFormatContext extends FileFormatContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public GenericFileFormatContext(FileFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterGenericFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitGenericFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitGenericFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileFormatContext fileFormat() throws RecognitionException {
		FileFormatContext _localctx = new FileFormatContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_fileFormat);
		try {
			setState(1537);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,164,_ctx) ) {
			case 1:
				_localctx = new TableFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1532);
				match(INPUTFORMAT);
				setState(1533);
				((TableFileFormatContext)_localctx).inFmt = match(STRING);
				setState(1534);
				match(OUTPUTFORMAT);
				setState(1535);
				((TableFileFormatContext)_localctx).outFmt = match(STRING);
				}
				break;
			case 2:
				_localctx = new GenericFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1536);
				identifier();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StorageHandlerContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticExtendSparkSqlParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public StorageHandlerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storageHandler; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterStorageHandler(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitStorageHandler(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitStorageHandler(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StorageHandlerContext storageHandler() throws RecognitionException {
		StorageHandlerContext _localctx = new StorageHandlerContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_storageHandler);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1539);
			match(STRING);
			setState(1543);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,165,_ctx) ) {
			case 1:
				{
				setState(1540);
				match(WITH);
				setState(1541);
				match(SERDEPROPERTIES);
				setState(1542);
				tablePropertyList();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ResourceContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public ResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitResource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceContext resource() throws RecognitionException {
		ResourceContext _localctx = new ResourceContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_resource);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1545);
			identifier();
			setState(1546);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DmlStatementNoWithContext extends ParserRuleContext {
		public DmlStatementNoWithContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dmlStatementNoWith; }
	 
		public DmlStatementNoWithContext() { }
		public void copyFrom(DmlStatementNoWithContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DeleteFromTableContext extends DmlStatementNoWithContext {
		public TerminalNode DELETE() { return getToken(ArcticExtendSparkSqlParser.DELETE, 0); }
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public DeleteFromTableContext(DmlStatementNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDeleteFromTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDeleteFromTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDeleteFromTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SingleInsertQueryContext extends DmlStatementNoWithContext {
		public InsertIntoContext insertInto() {
			return getRuleContext(InsertIntoContext.class,0);
		}
		public QueryTermContext queryTerm() {
			return getRuleContext(QueryTermContext.class,0);
		}
		public QueryOrganizationContext queryOrganization() {
			return getRuleContext(QueryOrganizationContext.class,0);
		}
		public SingleInsertQueryContext(DmlStatementNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSingleInsertQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSingleInsertQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSingleInsertQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MultiInsertQueryContext extends DmlStatementNoWithContext {
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public List<MultiInsertQueryBodyContext> multiInsertQueryBody() {
			return getRuleContexts(MultiInsertQueryBodyContext.class);
		}
		public MultiInsertQueryBodyContext multiInsertQueryBody(int i) {
			return getRuleContext(MultiInsertQueryBodyContext.class,i);
		}
		public MultiInsertQueryContext(DmlStatementNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMultiInsertQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMultiInsertQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMultiInsertQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UpdateTableContext extends DmlStatementNoWithContext {
		public TerminalNode UPDATE() { return getToken(ArcticExtendSparkSqlParser.UPDATE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public SetClauseContext setClause() {
			return getRuleContext(SetClauseContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public UpdateTableContext(DmlStatementNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUpdateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUpdateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUpdateTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MergeIntoTableContext extends DmlStatementNoWithContext {
		public MultipartIdentifierContext target;
		public TableAliasContext targetAlias;
		public MultipartIdentifierContext source;
		public QueryContext sourceQuery;
		public TableAliasContext sourceAlias;
		public BooleanExpressionContext mergeCondition;
		public TerminalNode MERGE() { return getToken(ArcticExtendSparkSqlParser.MERGE, 0); }
		public TerminalNode INTO() { return getToken(ArcticExtendSparkSqlParser.INTO, 0); }
		public TerminalNode USING() { return getToken(ArcticExtendSparkSqlParser.USING, 0); }
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public List<TableAliasContext> tableAlias() {
			return getRuleContexts(TableAliasContext.class);
		}
		public TableAliasContext tableAlias(int i) {
			return getRuleContext(TableAliasContext.class,i);
		}
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public List<MatchedClauseContext> matchedClause() {
			return getRuleContexts(MatchedClauseContext.class);
		}
		public MatchedClauseContext matchedClause(int i) {
			return getRuleContext(MatchedClauseContext.class,i);
		}
		public List<NotMatchedClauseContext> notMatchedClause() {
			return getRuleContexts(NotMatchedClauseContext.class);
		}
		public NotMatchedClauseContext notMatchedClause(int i) {
			return getRuleContext(NotMatchedClauseContext.class,i);
		}
		public MergeIntoTableContext(DmlStatementNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMergeIntoTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMergeIntoTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMergeIntoTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DmlStatementNoWithContext dmlStatementNoWith() throws RecognitionException {
		DmlStatementNoWithContext _localctx = new DmlStatementNoWithContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_dmlStatementNoWith);
		int _la;
		try {
			int _alt;
			setState(1599);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INSERT:
				_localctx = new SingleInsertQueryContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1548);
				insertInto();
				setState(1549);
				queryTerm(0);
				setState(1550);
				queryOrganization();
				}
				break;
			case FROM:
				_localctx = new MultiInsertQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1552);
				fromClause();
				setState(1554); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1553);
					multiInsertQueryBody();
					}
					}
					setState(1556); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==INSERT );
				}
				break;
			case DELETE:
				_localctx = new DeleteFromTableContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1558);
				match(DELETE);
				setState(1559);
				match(FROM);
				setState(1560);
				multipartIdentifier();
				setState(1561);
				tableAlias();
				setState(1563);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1562);
					whereClause();
					}
				}

				}
				break;
			case UPDATE:
				_localctx = new UpdateTableContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1565);
				match(UPDATE);
				setState(1566);
				multipartIdentifier();
				setState(1567);
				tableAlias();
				setState(1568);
				setClause();
				setState(1570);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1569);
					whereClause();
					}
				}

				}
				break;
			case MERGE:
				_localctx = new MergeIntoTableContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1572);
				match(MERGE);
				setState(1573);
				match(INTO);
				setState(1574);
				((MergeIntoTableContext)_localctx).target = multipartIdentifier();
				setState(1575);
				((MergeIntoTableContext)_localctx).targetAlias = tableAlias();
				setState(1576);
				match(USING);
				setState(1582);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,169,_ctx) ) {
				case 1:
					{
					setState(1577);
					((MergeIntoTableContext)_localctx).source = multipartIdentifier();
					}
					break;
				case 2:
					{
					setState(1578);
					match(T__0);
					setState(1579);
					((MergeIntoTableContext)_localctx).sourceQuery = query();
					setState(1580);
					match(T__2);
					}
					break;
				}
				setState(1584);
				((MergeIntoTableContext)_localctx).sourceAlias = tableAlias();
				setState(1585);
				match(ON);
				setState(1586);
				((MergeIntoTableContext)_localctx).mergeCondition = booleanExpression(0);
				setState(1590);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,170,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1587);
						matchedClause();
						}
						} 
					}
					setState(1592);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,170,_ctx);
				}
				setState(1596);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==WHEN) {
					{
					{
					setState(1593);
					notMatchedClause();
					}
					}
					setState(1598);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryOrganizationContext extends ParserRuleContext {
		public SortItemContext sortItem;
		public List<SortItemContext> order = new ArrayList<SortItemContext>();
		public ExpressionContext expression;
		public List<ExpressionContext> clusterBy = new ArrayList<ExpressionContext>();
		public List<ExpressionContext> distributeBy = new ArrayList<ExpressionContext>();
		public List<SortItemContext> sort = new ArrayList<SortItemContext>();
		public ExpressionContext limit;
		public TerminalNode ORDER() { return getToken(ArcticExtendSparkSqlParser.ORDER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticExtendSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticExtendSparkSqlParser.BY, i);
		}
		public TerminalNode CLUSTER() { return getToken(ArcticExtendSparkSqlParser.CLUSTER, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticExtendSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode SORT() { return getToken(ArcticExtendSparkSqlParser.SORT, 0); }
		public WindowClauseContext windowClause() {
			return getRuleContext(WindowClauseContext.class,0);
		}
		public TerminalNode LIMIT() { return getToken(ArcticExtendSparkSqlParser.LIMIT, 0); }
		public List<SortItemContext> sortItem() {
			return getRuleContexts(SortItemContext.class);
		}
		public SortItemContext sortItem(int i) {
			return getRuleContext(SortItemContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode ALL() { return getToken(ArcticExtendSparkSqlParser.ALL, 0); }
		public QueryOrganizationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryOrganization; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQueryOrganization(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQueryOrganization(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQueryOrganization(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryOrganizationContext queryOrganization() throws RecognitionException {
		QueryOrganizationContext _localctx = new QueryOrganizationContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_queryOrganization);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1611);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,174,_ctx) ) {
			case 1:
				{
				setState(1601);
				match(ORDER);
				setState(1602);
				match(BY);
				setState(1603);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(1608);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,173,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1604);
						match(T__1);
						setState(1605);
						((QueryOrganizationContext)_localctx).sortItem = sortItem();
						((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
						}
						} 
					}
					setState(1610);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,173,_ctx);
				}
				}
				break;
			}
			setState(1623);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,176,_ctx) ) {
			case 1:
				{
				setState(1613);
				match(CLUSTER);
				setState(1614);
				match(BY);
				setState(1615);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(1620);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,175,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1616);
						match(T__1);
						setState(1617);
						((QueryOrganizationContext)_localctx).expression = expression();
						((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
						}
						} 
					}
					setState(1622);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,175,_ctx);
				}
				}
				break;
			}
			setState(1635);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,178,_ctx) ) {
			case 1:
				{
				setState(1625);
				match(DISTRIBUTE);
				setState(1626);
				match(BY);
				setState(1627);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(1632);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,177,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1628);
						match(T__1);
						setState(1629);
						((QueryOrganizationContext)_localctx).expression = expression();
						((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
						}
						} 
					}
					setState(1634);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,177,_ctx);
				}
				}
				break;
			}
			setState(1647);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,180,_ctx) ) {
			case 1:
				{
				setState(1637);
				match(SORT);
				setState(1638);
				match(BY);
				setState(1639);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(1644);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,179,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1640);
						match(T__1);
						setState(1641);
						((QueryOrganizationContext)_localctx).sortItem = sortItem();
						((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
						}
						} 
					}
					setState(1646);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,179,_ctx);
				}
				}
				break;
			}
			setState(1650);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,181,_ctx) ) {
			case 1:
				{
				setState(1649);
				windowClause();
				}
				break;
			}
			setState(1657);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,183,_ctx) ) {
			case 1:
				{
				setState(1652);
				match(LIMIT);
				setState(1655);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,182,_ctx) ) {
				case 1:
					{
					setState(1653);
					match(ALL);
					}
					break;
				case 2:
					{
					setState(1654);
					((QueryOrganizationContext)_localctx).limit = expression();
					}
					break;
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiInsertQueryBodyContext extends ParserRuleContext {
		public InsertIntoContext insertInto() {
			return getRuleContext(InsertIntoContext.class,0);
		}
		public FromStatementBodyContext fromStatementBody() {
			return getRuleContext(FromStatementBodyContext.class,0);
		}
		public MultiInsertQueryBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiInsertQueryBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMultiInsertQueryBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMultiInsertQueryBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMultiInsertQueryBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiInsertQueryBodyContext multiInsertQueryBody() throws RecognitionException {
		MultiInsertQueryBodyContext _localctx = new MultiInsertQueryBodyContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_multiInsertQueryBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1659);
			insertInto();
			setState(1660);
			fromStatementBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryTermContext extends ParserRuleContext {
		public QueryTermContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryTerm; }
	 
		public QueryTermContext() { }
		public void copyFrom(QueryTermContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class QueryTermDefaultContext extends QueryTermContext {
		public QueryPrimaryContext queryPrimary() {
			return getRuleContext(QueryPrimaryContext.class,0);
		}
		public QueryTermDefaultContext(QueryTermContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQueryTermDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQueryTermDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQueryTermDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetOperationContext extends QueryTermContext {
		public QueryTermContext left;
		public Token operator;
		public QueryTermContext right;
		public List<QueryTermContext> queryTerm() {
			return getRuleContexts(QueryTermContext.class);
		}
		public QueryTermContext queryTerm(int i) {
			return getRuleContext(QueryTermContext.class,i);
		}
		public TerminalNode INTERSECT() { return getToken(ArcticExtendSparkSqlParser.INTERSECT, 0); }
		public TerminalNode UNION() { return getToken(ArcticExtendSparkSqlParser.UNION, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticExtendSparkSqlParser.EXCEPT, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticExtendSparkSqlParser.SETMINUS, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public SetOperationContext(QueryTermContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetOperation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetOperation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetOperation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryTermContext queryTerm() throws RecognitionException {
		return queryTerm(0);
	}

	private QueryTermContext queryTerm(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		QueryTermContext _localctx = new QueryTermContext(_ctx, _parentState);
		QueryTermContext _prevctx = _localctx;
		int _startState = 94;
		enterRecursionRule(_localctx, 94, RULE_queryTerm, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new QueryTermDefaultContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(1663);
			queryPrimary();
			}
			_ctx.stop = _input.LT(-1);
			setState(1688);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,188,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1686);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,187,_ctx) ) {
					case 1:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(1665);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1666);
						if (!(legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "legacy_setops_precedence_enabled");
						setState(1667);
						((SetOperationContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==EXCEPT || _la==INTERSECT || _la==SETMINUS || _la==UNION) ) {
							((SetOperationContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1669);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(1668);
							setQuantifier();
							}
						}

						setState(1671);
						((SetOperationContext)_localctx).right = queryTerm(4);
						}
						break;
					case 2:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(1672);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1673);
						if (!(!legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "!legacy_setops_precedence_enabled");
						setState(1674);
						((SetOperationContext)_localctx).operator = match(INTERSECT);
						setState(1676);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(1675);
							setQuantifier();
							}
						}

						setState(1678);
						((SetOperationContext)_localctx).right = queryTerm(3);
						}
						break;
					case 3:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(1679);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1680);
						if (!(!legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "!legacy_setops_precedence_enabled");
						setState(1681);
						((SetOperationContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==EXCEPT || _la==SETMINUS || _la==UNION) ) {
							((SetOperationContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1683);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(1682);
							setQuantifier();
							}
						}

						setState(1685);
						((SetOperationContext)_localctx).right = queryTerm(2);
						}
						break;
					}
					} 
				}
				setState(1690);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,188,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class QueryPrimaryContext extends ParserRuleContext {
		public QueryPrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryPrimary; }
	 
		public QueryPrimaryContext() { }
		public void copyFrom(QueryPrimaryContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SubqueryContext extends QueryPrimaryContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public SubqueryContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSubquery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSubquery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSubquery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class QueryPrimaryDefaultContext extends QueryPrimaryContext {
		public QuerySpecificationContext querySpecification() {
			return getRuleContext(QuerySpecificationContext.class,0);
		}
		public QueryPrimaryDefaultContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQueryPrimaryDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQueryPrimaryDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQueryPrimaryDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InlineTableDefault1Context extends QueryPrimaryContext {
		public InlineTableContext inlineTable() {
			return getRuleContext(InlineTableContext.class,0);
		}
		public InlineTableDefault1Context(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInlineTableDefault1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInlineTableDefault1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInlineTableDefault1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FromStmtContext extends QueryPrimaryContext {
		public FromStatementContext fromStatement() {
			return getRuleContext(FromStatementContext.class,0);
		}
		public FromStmtContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFromStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFromStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFromStmt(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TableContext extends QueryPrimaryContext {
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryPrimaryContext queryPrimary() throws RecognitionException {
		QueryPrimaryContext _localctx = new QueryPrimaryContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_queryPrimary);
		try {
			setState(1700);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MAP:
			case REDUCE:
			case SELECT:
				_localctx = new QueryPrimaryDefaultContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1691);
				querySpecification();
				}
				break;
			case FROM:
				_localctx = new FromStmtContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1692);
				fromStatement();
				}
				break;
			case TABLE:
				_localctx = new TableContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1693);
				match(TABLE);
				setState(1694);
				multipartIdentifier();
				}
				break;
			case VALUES:
				_localctx = new InlineTableDefault1Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1695);
				inlineTable();
				}
				break;
			case T__0:
				_localctx = new SubqueryContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1696);
				match(T__0);
				setState(1697);
				query();
				setState(1698);
				match(T__2);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SortItemContext extends ParserRuleContext {
		public Token ordering;
		public Token nullOrder;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode NULLS() { return getToken(ArcticExtendSparkSqlParser.NULLS, 0); }
		public TerminalNode ASC() { return getToken(ArcticExtendSparkSqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode LAST() { return getToken(ArcticExtendSparkSqlParser.LAST, 0); }
		public TerminalNode FIRST() { return getToken(ArcticExtendSparkSqlParser.FIRST, 0); }
		public SortItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sortItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSortItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSortItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSortItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SortItemContext sortItem() throws RecognitionException {
		SortItemContext _localctx = new SortItemContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_sortItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1702);
			expression();
			setState(1704);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,190,_ctx) ) {
			case 1:
				{
				setState(1703);
				((SortItemContext)_localctx).ordering = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
					((SortItemContext)_localctx).ordering = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
			setState(1708);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,191,_ctx) ) {
			case 1:
				{
				setState(1706);
				match(NULLS);
				setState(1707);
				((SortItemContext)_localctx).nullOrder = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==FIRST || _la==LAST) ) {
					((SortItemContext)_localctx).nullOrder = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FromStatementContext extends ParserRuleContext {
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public List<FromStatementBodyContext> fromStatementBody() {
			return getRuleContexts(FromStatementBodyContext.class);
		}
		public FromStatementBodyContext fromStatementBody(int i) {
			return getRuleContext(FromStatementBodyContext.class,i);
		}
		public FromStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFromStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFromStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFromStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromStatementContext fromStatement() throws RecognitionException {
		FromStatementContext _localctx = new FromStatementContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_fromStatement);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1710);
			fromClause();
			setState(1712); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1711);
					fromStatementBody();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1714); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,192,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FromStatementBodyContext extends ParserRuleContext {
		public TransformClauseContext transformClause() {
			return getRuleContext(TransformClauseContext.class,0);
		}
		public QueryOrganizationContext queryOrganization() {
			return getRuleContext(QueryOrganizationContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public SelectClauseContext selectClause() {
			return getRuleContext(SelectClauseContext.class,0);
		}
		public List<LateralViewContext> lateralView() {
			return getRuleContexts(LateralViewContext.class);
		}
		public LateralViewContext lateralView(int i) {
			return getRuleContext(LateralViewContext.class,i);
		}
		public AggregationClauseContext aggregationClause() {
			return getRuleContext(AggregationClauseContext.class,0);
		}
		public HavingClauseContext havingClause() {
			return getRuleContext(HavingClauseContext.class,0);
		}
		public WindowClauseContext windowClause() {
			return getRuleContext(WindowClauseContext.class,0);
		}
		public FromStatementBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromStatementBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFromStatementBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFromStatementBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFromStatementBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromStatementBodyContext fromStatementBody() throws RecognitionException {
		FromStatementBodyContext _localctx = new FromStatementBodyContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_fromStatementBody);
		try {
			int _alt;
			setState(1743);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,199,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1716);
				transformClause();
				setState(1718);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,193,_ctx) ) {
				case 1:
					{
					setState(1717);
					whereClause();
					}
					break;
				}
				setState(1720);
				queryOrganization();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1722);
				selectClause();
				setState(1726);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,194,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1723);
						lateralView();
						}
						} 
					}
					setState(1728);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,194,_ctx);
				}
				setState(1730);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,195,_ctx) ) {
				case 1:
					{
					setState(1729);
					whereClause();
					}
					break;
				}
				setState(1733);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,196,_ctx) ) {
				case 1:
					{
					setState(1732);
					aggregationClause();
					}
					break;
				}
				setState(1736);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,197,_ctx) ) {
				case 1:
					{
					setState(1735);
					havingClause();
					}
					break;
				}
				setState(1739);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,198,_ctx) ) {
				case 1:
					{
					setState(1738);
					windowClause();
					}
					break;
				}
				setState(1741);
				queryOrganization();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuerySpecificationContext extends ParserRuleContext {
		public QuerySpecificationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_querySpecification; }
	 
		public QuerySpecificationContext() { }
		public void copyFrom(QuerySpecificationContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class RegularQuerySpecificationContext extends QuerySpecificationContext {
		public SelectClauseContext selectClause() {
			return getRuleContext(SelectClauseContext.class,0);
		}
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public List<LateralViewContext> lateralView() {
			return getRuleContexts(LateralViewContext.class);
		}
		public LateralViewContext lateralView(int i) {
			return getRuleContext(LateralViewContext.class,i);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public AggregationClauseContext aggregationClause() {
			return getRuleContext(AggregationClauseContext.class,0);
		}
		public HavingClauseContext havingClause() {
			return getRuleContext(HavingClauseContext.class,0);
		}
		public WindowClauseContext windowClause() {
			return getRuleContext(WindowClauseContext.class,0);
		}
		public RegularQuerySpecificationContext(QuerySpecificationContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRegularQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRegularQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRegularQuerySpecification(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TransformQuerySpecificationContext extends QuerySpecificationContext {
		public TransformClauseContext transformClause() {
			return getRuleContext(TransformClauseContext.class,0);
		}
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public List<LateralViewContext> lateralView() {
			return getRuleContexts(LateralViewContext.class);
		}
		public LateralViewContext lateralView(int i) {
			return getRuleContext(LateralViewContext.class,i);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public AggregationClauseContext aggregationClause() {
			return getRuleContext(AggregationClauseContext.class,0);
		}
		public HavingClauseContext havingClause() {
			return getRuleContext(HavingClauseContext.class,0);
		}
		public WindowClauseContext windowClause() {
			return getRuleContext(WindowClauseContext.class,0);
		}
		public TransformQuerySpecificationContext(QuerySpecificationContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTransformQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTransformQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTransformQuerySpecification(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuerySpecificationContext querySpecification() throws RecognitionException {
		QuerySpecificationContext _localctx = new QuerySpecificationContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_querySpecification);
		try {
			int _alt;
			setState(1789);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,212,_ctx) ) {
			case 1:
				_localctx = new TransformQuerySpecificationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1745);
				transformClause();
				setState(1747);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,200,_ctx) ) {
				case 1:
					{
					setState(1746);
					fromClause();
					}
					break;
				}
				setState(1752);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,201,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1749);
						lateralView();
						}
						} 
					}
					setState(1754);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,201,_ctx);
				}
				setState(1756);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,202,_ctx) ) {
				case 1:
					{
					setState(1755);
					whereClause();
					}
					break;
				}
				setState(1759);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,203,_ctx) ) {
				case 1:
					{
					setState(1758);
					aggregationClause();
					}
					break;
				}
				setState(1762);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,204,_ctx) ) {
				case 1:
					{
					setState(1761);
					havingClause();
					}
					break;
				}
				setState(1765);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,205,_ctx) ) {
				case 1:
					{
					setState(1764);
					windowClause();
					}
					break;
				}
				}
				break;
			case 2:
				_localctx = new RegularQuerySpecificationContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1767);
				selectClause();
				setState(1769);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,206,_ctx) ) {
				case 1:
					{
					setState(1768);
					fromClause();
					}
					break;
				}
				setState(1774);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,207,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1771);
						lateralView();
						}
						} 
					}
					setState(1776);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,207,_ctx);
				}
				setState(1778);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,208,_ctx) ) {
				case 1:
					{
					setState(1777);
					whereClause();
					}
					break;
				}
				setState(1781);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,209,_ctx) ) {
				case 1:
					{
					setState(1780);
					aggregationClause();
					}
					break;
				}
				setState(1784);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,210,_ctx) ) {
				case 1:
					{
					setState(1783);
					havingClause();
					}
					break;
				}
				setState(1787);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,211,_ctx) ) {
				case 1:
					{
					setState(1786);
					windowClause();
					}
					break;
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TransformClauseContext extends ParserRuleContext {
		public Token kind;
		public RowFormatContext inRowFormat;
		public Token recordWriter;
		public Token script;
		public RowFormatContext outRowFormat;
		public Token recordReader;
		public TerminalNode USING() { return getToken(ArcticExtendSparkSqlParser.USING, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticExtendSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticExtendSparkSqlParser.STRING, i);
		}
		public TerminalNode SELECT() { return getToken(ArcticExtendSparkSqlParser.SELECT, 0); }
		public ExpressionSeqContext expressionSeq() {
			return getRuleContext(ExpressionSeqContext.class,0);
		}
		public TerminalNode TRANSFORM() { return getToken(ArcticExtendSparkSqlParser.TRANSFORM, 0); }
		public TerminalNode MAP() { return getToken(ArcticExtendSparkSqlParser.MAP, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticExtendSparkSqlParser.REDUCE, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticExtendSparkSqlParser.RECORDWRITER, 0); }
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticExtendSparkSqlParser.RECORDREADER, 0); }
		public List<RowFormatContext> rowFormat() {
			return getRuleContexts(RowFormatContext.class);
		}
		public RowFormatContext rowFormat(int i) {
			return getRuleContext(RowFormatContext.class,i);
		}
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TransformClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transformClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTransformClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTransformClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTransformClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformClauseContext transformClause() throws RecognitionException {
		TransformClauseContext _localctx = new TransformClauseContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_transformClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1810);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
				{
				setState(1791);
				match(SELECT);
				setState(1792);
				((TransformClauseContext)_localctx).kind = match(TRANSFORM);
				setState(1793);
				match(T__0);
				setState(1795);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,213,_ctx) ) {
				case 1:
					{
					setState(1794);
					setQuantifier();
					}
					break;
				}
				setState(1797);
				expressionSeq();
				setState(1798);
				match(T__2);
				}
				break;
			case MAP:
				{
				setState(1800);
				((TransformClauseContext)_localctx).kind = match(MAP);
				setState(1802);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,214,_ctx) ) {
				case 1:
					{
					setState(1801);
					setQuantifier();
					}
					break;
				}
				setState(1804);
				expressionSeq();
				}
				break;
			case REDUCE:
				{
				setState(1805);
				((TransformClauseContext)_localctx).kind = match(REDUCE);
				setState(1807);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,215,_ctx) ) {
				case 1:
					{
					setState(1806);
					setQuantifier();
					}
					break;
				}
				setState(1809);
				expressionSeq();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1813);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ROW) {
				{
				setState(1812);
				((TransformClauseContext)_localctx).inRowFormat = rowFormat();
				}
			}

			setState(1817);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RECORDWRITER) {
				{
				setState(1815);
				match(RECORDWRITER);
				setState(1816);
				((TransformClauseContext)_localctx).recordWriter = match(STRING);
				}
			}

			setState(1819);
			match(USING);
			setState(1820);
			((TransformClauseContext)_localctx).script = match(STRING);
			setState(1833);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,221,_ctx) ) {
			case 1:
				{
				setState(1821);
				match(AS);
				setState(1831);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,220,_ctx) ) {
				case 1:
					{
					setState(1822);
					identifierSeq();
					}
					break;
				case 2:
					{
					setState(1823);
					colTypeList();
					}
					break;
				case 3:
					{
					{
					setState(1824);
					match(T__0);
					setState(1827);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,219,_ctx) ) {
					case 1:
						{
						setState(1825);
						identifierSeq();
						}
						break;
					case 2:
						{
						setState(1826);
						colTypeList();
						}
						break;
					}
					setState(1829);
					match(T__2);
					}
					}
					break;
				}
				}
				break;
			}
			setState(1836);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,222,_ctx) ) {
			case 1:
				{
				setState(1835);
				((TransformClauseContext)_localctx).outRowFormat = rowFormat();
				}
				break;
			}
			setState(1840);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,223,_ctx) ) {
			case 1:
				{
				setState(1838);
				match(RECORDREADER);
				setState(1839);
				((TransformClauseContext)_localctx).recordReader = match(STRING);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectClauseContext extends ParserRuleContext {
		public HintContext hint;
		public List<HintContext> hints = new ArrayList<HintContext>();
		public TerminalNode SELECT() { return getToken(ArcticExtendSparkSqlParser.SELECT, 0); }
		public NamedExpressionSeqContext namedExpressionSeq() {
			return getRuleContext(NamedExpressionSeqContext.class,0);
		}
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public List<HintContext> hint() {
			return getRuleContexts(HintContext.class);
		}
		public HintContext hint(int i) {
			return getRuleContext(HintContext.class,i);
		}
		public SelectClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSelectClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSelectClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSelectClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectClauseContext selectClause() throws RecognitionException {
		SelectClauseContext _localctx = new SelectClauseContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_selectClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1842);
			match(SELECT);
			setState(1846);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,224,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1843);
					((SelectClauseContext)_localctx).hint = hint();
					((SelectClauseContext)_localctx).hints.add(((SelectClauseContext)_localctx).hint);
					}
					} 
				}
				setState(1848);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,224,_ctx);
			}
			setState(1850);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,225,_ctx) ) {
			case 1:
				{
				setState(1849);
				setQuantifier();
				}
				break;
			}
			setState(1852);
			namedExpressionSeq();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SetClauseContext extends ParserRuleContext {
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public AssignmentListContext assignmentList() {
			return getRuleContext(AssignmentListContext.class,0);
		}
		public SetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetClauseContext setClause() throws RecognitionException {
		SetClauseContext _localctx = new SetClauseContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_setClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1854);
			match(SET);
			setState(1855);
			assignmentList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MatchedClauseContext extends ParserRuleContext {
		public BooleanExpressionContext matchedCond;
		public TerminalNode WHEN() { return getToken(ArcticExtendSparkSqlParser.WHEN, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticExtendSparkSqlParser.MATCHED, 0); }
		public TerminalNode THEN() { return getToken(ArcticExtendSparkSqlParser.THEN, 0); }
		public MatchedActionContext matchedAction() {
			return getRuleContext(MatchedActionContext.class,0);
		}
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public MatchedClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchedClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMatchedClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMatchedClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMatchedClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchedClauseContext matchedClause() throws RecognitionException {
		MatchedClauseContext _localctx = new MatchedClauseContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_matchedClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1857);
			match(WHEN);
			setState(1858);
			match(MATCHED);
			setState(1861);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AND) {
				{
				setState(1859);
				match(AND);
				setState(1860);
				((MatchedClauseContext)_localctx).matchedCond = booleanExpression(0);
				}
			}

			setState(1863);
			match(THEN);
			setState(1864);
			matchedAction();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NotMatchedClauseContext extends ParserRuleContext {
		public BooleanExpressionContext notMatchedCond;
		public TerminalNode WHEN() { return getToken(ArcticExtendSparkSqlParser.WHEN, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticExtendSparkSqlParser.MATCHED, 0); }
		public TerminalNode THEN() { return getToken(ArcticExtendSparkSqlParser.THEN, 0); }
		public NotMatchedActionContext notMatchedAction() {
			return getRuleContext(NotMatchedActionContext.class,0);
		}
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public NotMatchedClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notMatchedClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNotMatchedClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNotMatchedClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNotMatchedClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotMatchedClauseContext notMatchedClause() throws RecognitionException {
		NotMatchedClauseContext _localctx = new NotMatchedClauseContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_notMatchedClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1866);
			match(WHEN);
			setState(1867);
			match(NOT);
			setState(1868);
			match(MATCHED);
			setState(1871);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AND) {
				{
				setState(1869);
				match(AND);
				setState(1870);
				((NotMatchedClauseContext)_localctx).notMatchedCond = booleanExpression(0);
				}
			}

			setState(1873);
			match(THEN);
			setState(1874);
			notMatchedAction();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MatchedActionContext extends ParserRuleContext {
		public TerminalNode DELETE() { return getToken(ArcticExtendSparkSqlParser.DELETE, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticExtendSparkSqlParser.UPDATE, 0); }
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticExtendSparkSqlParser.ASTERISK, 0); }
		public AssignmentListContext assignmentList() {
			return getRuleContext(AssignmentListContext.class,0);
		}
		public MatchedActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchedAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMatchedAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMatchedAction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMatchedAction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchedActionContext matchedAction() throws RecognitionException {
		MatchedActionContext _localctx = new MatchedActionContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_matchedAction);
		try {
			setState(1883);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,228,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1876);
				match(DELETE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1877);
				match(UPDATE);
				setState(1878);
				match(SET);
				setState(1879);
				match(ASTERISK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1880);
				match(UPDATE);
				setState(1881);
				match(SET);
				setState(1882);
				assignmentList();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NotMatchedActionContext extends ParserRuleContext {
		public MultipartIdentifierListContext columns;
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticExtendSparkSqlParser.ASTERISK, 0); }
		public TerminalNode VALUES() { return getToken(ArcticExtendSparkSqlParser.VALUES, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public MultipartIdentifierListContext multipartIdentifierList() {
			return getRuleContext(MultipartIdentifierListContext.class,0);
		}
		public NotMatchedActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notMatchedAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNotMatchedAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNotMatchedAction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNotMatchedAction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotMatchedActionContext notMatchedAction() throws RecognitionException {
		NotMatchedActionContext _localctx = new NotMatchedActionContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_notMatchedAction);
		int _la;
		try {
			setState(1903);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,230,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1885);
				match(INSERT);
				setState(1886);
				match(ASTERISK);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1887);
				match(INSERT);
				setState(1888);
				match(T__0);
				setState(1889);
				((NotMatchedActionContext)_localctx).columns = multipartIdentifierList();
				setState(1890);
				match(T__2);
				setState(1891);
				match(VALUES);
				setState(1892);
				match(T__0);
				setState(1893);
				expression();
				setState(1898);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(1894);
					match(T__1);
					setState(1895);
					expression();
					}
					}
					setState(1900);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1901);
				match(T__2);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignmentListContext extends ParserRuleContext {
		public List<AssignmentContext> assignment() {
			return getRuleContexts(AssignmentContext.class);
		}
		public AssignmentContext assignment(int i) {
			return getRuleContext(AssignmentContext.class,i);
		}
		public AssignmentListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignmentList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAssignmentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAssignmentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAssignmentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentListContext assignmentList() throws RecognitionException {
		AssignmentListContext _localctx = new AssignmentListContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_assignmentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1905);
			assignment();
			setState(1910);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1906);
				match(T__1);
				setState(1907);
				assignment();
				}
				}
				setState(1912);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignmentContext extends ParserRuleContext {
		public MultipartIdentifierContext key;
		public ExpressionContext value;
		public TerminalNode EQ() { return getToken(ArcticExtendSparkSqlParser.EQ, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1913);
			((AssignmentContext)_localctx).key = multipartIdentifier();
			setState(1914);
			match(EQ);
			setState(1915);
			((AssignmentContext)_localctx).value = expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WhereClauseContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(ArcticExtendSparkSqlParser.WHERE, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public WhereClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterWhereClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitWhereClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitWhereClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_whereClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1917);
			match(WHERE);
			setState(1918);
			booleanExpression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HavingClauseContext extends ParserRuleContext {
		public TerminalNode HAVING() { return getToken(ArcticExtendSparkSqlParser.HAVING, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public HavingClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_havingClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterHavingClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitHavingClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitHavingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingClauseContext havingClause() throws RecognitionException {
		HavingClauseContext _localctx = new HavingClauseContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_havingClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1920);
			match(HAVING);
			setState(1921);
			booleanExpression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HintContext extends ParserRuleContext {
		public HintStatementContext hintStatement;
		public List<HintStatementContext> hintStatements = new ArrayList<HintStatementContext>();
		public List<HintStatementContext> hintStatement() {
			return getRuleContexts(HintStatementContext.class);
		}
		public HintStatementContext hintStatement(int i) {
			return getRuleContext(HintStatementContext.class,i);
		}
		public HintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterHint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitHint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitHint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintContext hint() throws RecognitionException {
		HintContext _localctx = new HintContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_hint);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1923);
			match(T__5);
			setState(1924);
			((HintContext)_localctx).hintStatement = hintStatement();
			((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
			setState(1931);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,233,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1926);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,232,_ctx) ) {
					case 1:
						{
						setState(1925);
						match(T__1);
						}
						break;
					}
					setState(1928);
					((HintContext)_localctx).hintStatement = hintStatement();
					((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
					}
					} 
				}
				setState(1933);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,233,_ctx);
			}
			setState(1934);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HintStatementContext extends ParserRuleContext {
		public IdentifierContext hintName;
		public PrimaryExpressionContext primaryExpression;
		public List<PrimaryExpressionContext> parameters = new ArrayList<PrimaryExpressionContext>();
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<PrimaryExpressionContext> primaryExpression() {
			return getRuleContexts(PrimaryExpressionContext.class);
		}
		public PrimaryExpressionContext primaryExpression(int i) {
			return getRuleContext(PrimaryExpressionContext.class,i);
		}
		public HintStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hintStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterHintStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitHintStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitHintStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintStatementContext hintStatement() throws RecognitionException {
		HintStatementContext _localctx = new HintStatementContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_hintStatement);
		int _la;
		try {
			setState(1949);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,235,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1936);
				((HintStatementContext)_localctx).hintName = identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1937);
				((HintStatementContext)_localctx).hintName = identifier();
				setState(1938);
				match(T__0);
				setState(1939);
				((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
				((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
				setState(1944);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(1940);
					match(T__1);
					setState(1941);
					((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
					((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
					}
					}
					setState(1946);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1947);
				match(T__2);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FromClauseContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public List<RelationContext> relation() {
			return getRuleContexts(RelationContext.class);
		}
		public RelationContext relation(int i) {
			return getRuleContext(RelationContext.class,i);
		}
		public List<LateralViewContext> lateralView() {
			return getRuleContexts(LateralViewContext.class);
		}
		public LateralViewContext lateralView(int i) {
			return getRuleContext(LateralViewContext.class,i);
		}
		public PivotClauseContext pivotClause() {
			return getRuleContext(PivotClauseContext.class,0);
		}
		public FromClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFromClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFromClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFromClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromClauseContext fromClause() throws RecognitionException {
		FromClauseContext _localctx = new FromClauseContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_fromClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1951);
			match(FROM);
			setState(1952);
			relation();
			setState(1957);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,236,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1953);
					match(T__1);
					setState(1954);
					relation();
					}
					} 
				}
				setState(1959);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,236,_ctx);
			}
			setState(1963);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,237,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1960);
					lateralView();
					}
					} 
				}
				setState(1965);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,237,_ctx);
			}
			setState(1967);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,238,_ctx) ) {
			case 1:
				{
				setState(1966);
				pivotClause();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AggregationClauseContext extends ParserRuleContext {
		public GroupByClauseContext groupByClause;
		public List<GroupByClauseContext> groupingExpressionsWithGroupingAnalytics = new ArrayList<GroupByClauseContext>();
		public ExpressionContext expression;
		public List<ExpressionContext> groupingExpressions = new ArrayList<ExpressionContext>();
		public Token kind;
		public TerminalNode GROUP() { return getToken(ArcticExtendSparkSqlParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(ArcticExtendSparkSqlParser.BY, 0); }
		public List<GroupByClauseContext> groupByClause() {
			return getRuleContexts(GroupByClauseContext.class);
		}
		public GroupByClauseContext groupByClause(int i) {
			return getRuleContext(GroupByClauseContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode WITH() { return getToken(ArcticExtendSparkSqlParser.WITH, 0); }
		public TerminalNode SETS() { return getToken(ArcticExtendSparkSqlParser.SETS, 0); }
		public List<GroupingSetContext> groupingSet() {
			return getRuleContexts(GroupingSetContext.class);
		}
		public GroupingSetContext groupingSet(int i) {
			return getRuleContext(GroupingSetContext.class,i);
		}
		public TerminalNode ROLLUP() { return getToken(ArcticExtendSparkSqlParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticExtendSparkSqlParser.CUBE, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticExtendSparkSqlParser.GROUPING, 0); }
		public AggregationClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregationClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAggregationClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAggregationClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAggregationClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregationClauseContext aggregationClause() throws RecognitionException {
		AggregationClauseContext _localctx = new AggregationClauseContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_aggregationClause);
		int _la;
		try {
			int _alt;
			setState(2008);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,243,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1969);
				match(GROUP);
				setState(1970);
				match(BY);
				setState(1971);
				((AggregationClauseContext)_localctx).groupByClause = groupByClause();
				((AggregationClauseContext)_localctx).groupingExpressionsWithGroupingAnalytics.add(((AggregationClauseContext)_localctx).groupByClause);
				setState(1976);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,239,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1972);
						match(T__1);
						setState(1973);
						((AggregationClauseContext)_localctx).groupByClause = groupByClause();
						((AggregationClauseContext)_localctx).groupingExpressionsWithGroupingAnalytics.add(((AggregationClauseContext)_localctx).groupByClause);
						}
						} 
					}
					setState(1978);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,239,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1979);
				match(GROUP);
				setState(1980);
				match(BY);
				setState(1981);
				((AggregationClauseContext)_localctx).expression = expression();
				((AggregationClauseContext)_localctx).groupingExpressions.add(((AggregationClauseContext)_localctx).expression);
				setState(1986);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,240,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1982);
						match(T__1);
						setState(1983);
						((AggregationClauseContext)_localctx).expression = expression();
						((AggregationClauseContext)_localctx).groupingExpressions.add(((AggregationClauseContext)_localctx).expression);
						}
						} 
					}
					setState(1988);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,240,_ctx);
				}
				setState(2006);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,242,_ctx) ) {
				case 1:
					{
					setState(1989);
					match(WITH);
					setState(1990);
					((AggregationClauseContext)_localctx).kind = match(ROLLUP);
					}
					break;
				case 2:
					{
					setState(1991);
					match(WITH);
					setState(1992);
					((AggregationClauseContext)_localctx).kind = match(CUBE);
					}
					break;
				case 3:
					{
					setState(1993);
					((AggregationClauseContext)_localctx).kind = match(GROUPING);
					setState(1994);
					match(SETS);
					setState(1995);
					match(T__0);
					setState(1996);
					groupingSet();
					setState(2001);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(1997);
						match(T__1);
						setState(1998);
						groupingSet();
						}
						}
						setState(2003);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(2004);
					match(T__2);
					}
					break;
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupByClauseContext extends ParserRuleContext {
		public GroupingAnalyticsContext groupingAnalytics() {
			return getRuleContext(GroupingAnalyticsContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public GroupByClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupByClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterGroupByClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitGroupByClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitGroupByClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupByClauseContext groupByClause() throws RecognitionException {
		GroupByClauseContext _localctx = new GroupByClauseContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_groupByClause);
		try {
			setState(2012);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,244,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2010);
				groupingAnalytics();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2011);
				expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupingAnalyticsContext extends ParserRuleContext {
		public List<GroupingSetContext> groupingSet() {
			return getRuleContexts(GroupingSetContext.class);
		}
		public GroupingSetContext groupingSet(int i) {
			return getRuleContext(GroupingSetContext.class,i);
		}
		public TerminalNode ROLLUP() { return getToken(ArcticExtendSparkSqlParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticExtendSparkSqlParser.CUBE, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticExtendSparkSqlParser.GROUPING, 0); }
		public TerminalNode SETS() { return getToken(ArcticExtendSparkSqlParser.SETS, 0); }
		public List<GroupingElementContext> groupingElement() {
			return getRuleContexts(GroupingElementContext.class);
		}
		public GroupingElementContext groupingElement(int i) {
			return getRuleContext(GroupingElementContext.class,i);
		}
		public GroupingAnalyticsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupingAnalytics; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterGroupingAnalytics(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitGroupingAnalytics(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitGroupingAnalytics(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingAnalyticsContext groupingAnalytics() throws RecognitionException {
		GroupingAnalyticsContext _localctx = new GroupingAnalyticsContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_groupingAnalytics);
		int _la;
		try {
			setState(2039);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CUBE:
			case ROLLUP:
				enterOuterAlt(_localctx, 1);
				{
				setState(2014);
				_la = _input.LA(1);
				if ( !(_la==CUBE || _la==ROLLUP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2015);
				match(T__0);
				setState(2016);
				groupingSet();
				setState(2021);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2017);
					match(T__1);
					setState(2018);
					groupingSet();
					}
					}
					setState(2023);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2024);
				match(T__2);
				}
				break;
			case GROUPING:
				enterOuterAlt(_localctx, 2);
				{
				setState(2026);
				match(GROUPING);
				setState(2027);
				match(SETS);
				setState(2028);
				match(T__0);
				setState(2029);
				groupingElement();
				setState(2034);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2030);
					match(T__1);
					setState(2031);
					groupingElement();
					}
					}
					setState(2036);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2037);
				match(T__2);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupingElementContext extends ParserRuleContext {
		public GroupingAnalyticsContext groupingAnalytics() {
			return getRuleContext(GroupingAnalyticsContext.class,0);
		}
		public GroupingSetContext groupingSet() {
			return getRuleContext(GroupingSetContext.class,0);
		}
		public GroupingElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupingElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterGroupingElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitGroupingElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitGroupingElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingElementContext groupingElement() throws RecognitionException {
		GroupingElementContext _localctx = new GroupingElementContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_groupingElement);
		try {
			setState(2043);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,248,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2041);
				groupingAnalytics();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2042);
				groupingSet();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupingSetContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public GroupingSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupingSet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterGroupingSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitGroupingSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitGroupingSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingSetContext groupingSet() throws RecognitionException {
		GroupingSetContext _localctx = new GroupingSetContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_groupingSet);
		int _la;
		try {
			setState(2058);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,251,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2045);
				match(T__0);
				setState(2054);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,250,_ctx) ) {
				case 1:
					{
					setState(2046);
					expression();
					setState(2051);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(2047);
						match(T__1);
						setState(2048);
						expression();
						}
						}
						setState(2053);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(2056);
				match(T__2);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2057);
				expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PivotClauseContext extends ParserRuleContext {
		public NamedExpressionSeqContext aggregates;
		public PivotValueContext pivotValue;
		public List<PivotValueContext> pivotValues = new ArrayList<PivotValueContext>();
		public TerminalNode PIVOT() { return getToken(ArcticExtendSparkSqlParser.PIVOT, 0); }
		public TerminalNode FOR() { return getToken(ArcticExtendSparkSqlParser.FOR, 0); }
		public PivotColumnContext pivotColumn() {
			return getRuleContext(PivotColumnContext.class,0);
		}
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public NamedExpressionSeqContext namedExpressionSeq() {
			return getRuleContext(NamedExpressionSeqContext.class,0);
		}
		public List<PivotValueContext> pivotValue() {
			return getRuleContexts(PivotValueContext.class);
		}
		public PivotValueContext pivotValue(int i) {
			return getRuleContext(PivotValueContext.class,i);
		}
		public PivotClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPivotClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPivotClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPivotClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotClauseContext pivotClause() throws RecognitionException {
		PivotClauseContext _localctx = new PivotClauseContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_pivotClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2060);
			match(PIVOT);
			setState(2061);
			match(T__0);
			setState(2062);
			((PivotClauseContext)_localctx).aggregates = namedExpressionSeq();
			setState(2063);
			match(FOR);
			setState(2064);
			pivotColumn();
			setState(2065);
			match(IN);
			setState(2066);
			match(T__0);
			setState(2067);
			((PivotClauseContext)_localctx).pivotValue = pivotValue();
			((PivotClauseContext)_localctx).pivotValues.add(((PivotClauseContext)_localctx).pivotValue);
			setState(2072);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2068);
				match(T__1);
				setState(2069);
				((PivotClauseContext)_localctx).pivotValue = pivotValue();
				((PivotClauseContext)_localctx).pivotValues.add(((PivotClauseContext)_localctx).pivotValue);
				}
				}
				setState(2074);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2075);
			match(T__2);
			setState(2076);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PivotColumnContext extends ParserRuleContext {
		public IdentifierContext identifier;
		public List<IdentifierContext> identifiers = new ArrayList<IdentifierContext>();
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public PivotColumnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotColumn; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPivotColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPivotColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPivotColumn(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotColumnContext pivotColumn() throws RecognitionException {
		PivotColumnContext _localctx = new PivotColumnContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_pivotColumn);
		int _la;
		try {
			setState(2090);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,254,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2078);
				((PivotColumnContext)_localctx).identifier = identifier();
				((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2079);
				match(T__0);
				setState(2080);
				((PivotColumnContext)_localctx).identifier = identifier();
				((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
				setState(2085);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2081);
					match(T__1);
					setState(2082);
					((PivotColumnContext)_localctx).identifier = identifier();
					((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
					}
					}
					setState(2087);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2088);
				match(T__2);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PivotValueContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public PivotValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPivotValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPivotValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPivotValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotValueContext pivotValue() throws RecognitionException {
		PivotValueContext _localctx = new PivotValueContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_pivotValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2092);
			expression();
			setState(2097);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,256,_ctx) ) {
			case 1:
				{
				setState(2094);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,255,_ctx) ) {
				case 1:
					{
					setState(2093);
					match(AS);
					}
					break;
				}
				setState(2096);
				identifier();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LateralViewContext extends ParserRuleContext {
		public IdentifierContext tblName;
		public IdentifierContext identifier;
		public List<IdentifierContext> colName = new ArrayList<IdentifierContext>();
		public TerminalNode LATERAL() { return getToken(ArcticExtendSparkSqlParser.LATERAL, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode OUTER() { return getToken(ArcticExtendSparkSqlParser.OUTER, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public LateralViewContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lateralView; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLateralView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLateralView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLateralView(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LateralViewContext lateralView() throws RecognitionException {
		LateralViewContext _localctx = new LateralViewContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_lateralView);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2099);
			match(LATERAL);
			setState(2100);
			match(VIEW);
			setState(2102);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,257,_ctx) ) {
			case 1:
				{
				setState(2101);
				match(OUTER);
				}
				break;
			}
			setState(2104);
			qualifiedName();
			setState(2105);
			match(T__0);
			setState(2114);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,259,_ctx) ) {
			case 1:
				{
				setState(2106);
				expression();
				setState(2111);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2107);
					match(T__1);
					setState(2108);
					expression();
					}
					}
					setState(2113);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
			setState(2116);
			match(T__2);
			setState(2117);
			((LateralViewContext)_localctx).tblName = identifier();
			setState(2129);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,262,_ctx) ) {
			case 1:
				{
				setState(2119);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,260,_ctx) ) {
				case 1:
					{
					setState(2118);
					match(AS);
					}
					break;
				}
				setState(2121);
				((LateralViewContext)_localctx).identifier = identifier();
				((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
				setState(2126);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,261,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2122);
						match(T__1);
						setState(2123);
						((LateralViewContext)_localctx).identifier = identifier();
						((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
						}
						} 
					}
					setState(2128);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,261,_ctx);
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SetQuantifierContext extends ParserRuleContext {
		public TerminalNode DISTINCT() { return getToken(ArcticExtendSparkSqlParser.DISTINCT, 0); }
		public TerminalNode ALL() { return getToken(ArcticExtendSparkSqlParser.ALL, 0); }
		public SetQuantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setQuantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSetQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSetQuantifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSetQuantifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetQuantifierContext setQuantifier() throws RecognitionException {
		SetQuantifierContext _localctx = new SetQuantifierContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_setQuantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2131);
			_la = _input.LA(1);
			if ( !(_la==ALL || _la==DISTINCT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RelationContext extends ParserRuleContext {
		public RelationPrimaryContext relationPrimary() {
			return getRuleContext(RelationPrimaryContext.class,0);
		}
		public TerminalNode LATERAL() { return getToken(ArcticExtendSparkSqlParser.LATERAL, 0); }
		public List<JoinRelationContext> joinRelation() {
			return getRuleContexts(JoinRelationContext.class);
		}
		public JoinRelationContext joinRelation(int i) {
			return getRuleContext(JoinRelationContext.class,i);
		}
		public RelationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationContext relation() throws RecognitionException {
		RelationContext _localctx = new RelationContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_relation);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2134);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,263,_ctx) ) {
			case 1:
				{
				setState(2133);
				match(LATERAL);
				}
				break;
			}
			setState(2136);
			relationPrimary();
			setState(2140);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,264,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2137);
					joinRelation();
					}
					} 
				}
				setState(2142);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,264,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinRelationContext extends ParserRuleContext {
		public RelationPrimaryContext right;
		public TerminalNode JOIN() { return getToken(ArcticExtendSparkSqlParser.JOIN, 0); }
		public RelationPrimaryContext relationPrimary() {
			return getRuleContext(RelationPrimaryContext.class,0);
		}
		public JoinTypeContext joinType() {
			return getRuleContext(JoinTypeContext.class,0);
		}
		public TerminalNode LATERAL() { return getToken(ArcticExtendSparkSqlParser.LATERAL, 0); }
		public JoinCriteriaContext joinCriteria() {
			return getRuleContext(JoinCriteriaContext.class,0);
		}
		public TerminalNode NATURAL() { return getToken(ArcticExtendSparkSqlParser.NATURAL, 0); }
		public JoinRelationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinRelation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterJoinRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitJoinRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitJoinRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinRelationContext joinRelation() throws RecognitionException {
		JoinRelationContext _localctx = new JoinRelationContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_joinRelation);
		try {
			setState(2160);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ANTI:
			case CROSS:
			case FULL:
			case INNER:
			case JOIN:
			case LEFT:
			case RIGHT:
			case SEMI:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(2143);
				joinType();
				}
				setState(2144);
				match(JOIN);
				setState(2146);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,265,_ctx) ) {
				case 1:
					{
					setState(2145);
					match(LATERAL);
					}
					break;
				}
				setState(2148);
				((JoinRelationContext)_localctx).right = relationPrimary();
				setState(2150);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,266,_ctx) ) {
				case 1:
					{
					setState(2149);
					joinCriteria();
					}
					break;
				}
				}
				break;
			case NATURAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(2152);
				match(NATURAL);
				setState(2153);
				joinType();
				setState(2154);
				match(JOIN);
				setState(2156);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,267,_ctx) ) {
				case 1:
					{
					setState(2155);
					match(LATERAL);
					}
					break;
				}
				setState(2158);
				((JoinRelationContext)_localctx).right = relationPrimary();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinTypeContext extends ParserRuleContext {
		public TerminalNode INNER() { return getToken(ArcticExtendSparkSqlParser.INNER, 0); }
		public TerminalNode CROSS() { return getToken(ArcticExtendSparkSqlParser.CROSS, 0); }
		public TerminalNode LEFT() { return getToken(ArcticExtendSparkSqlParser.LEFT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticExtendSparkSqlParser.OUTER, 0); }
		public TerminalNode SEMI() { return getToken(ArcticExtendSparkSqlParser.SEMI, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticExtendSparkSqlParser.RIGHT, 0); }
		public TerminalNode FULL() { return getToken(ArcticExtendSparkSqlParser.FULL, 0); }
		public TerminalNode ANTI() { return getToken(ArcticExtendSparkSqlParser.ANTI, 0); }
		public JoinTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterJoinType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitJoinType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitJoinType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinTypeContext joinType() throws RecognitionException {
		JoinTypeContext _localctx = new JoinTypeContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_joinType);
		int _la;
		try {
			setState(2186);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,275,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2163);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INNER) {
					{
					setState(2162);
					match(INNER);
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2165);
				match(CROSS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2166);
				match(LEFT);
				setState(2168);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(2167);
					match(OUTER);
					}
				}

				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(2171);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(2170);
					match(LEFT);
					}
				}

				setState(2173);
				match(SEMI);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(2174);
				match(RIGHT);
				setState(2176);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(2175);
					match(OUTER);
					}
				}

				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(2178);
				match(FULL);
				setState(2180);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(2179);
					match(OUTER);
					}
				}

				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(2183);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(2182);
					match(LEFT);
					}
				}

				setState(2185);
				match(ANTI);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JoinCriteriaContext extends ParserRuleContext {
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public TerminalNode USING() { return getToken(ArcticExtendSparkSqlParser.USING, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public JoinCriteriaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinCriteria; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterJoinCriteria(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitJoinCriteria(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitJoinCriteria(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinCriteriaContext joinCriteria() throws RecognitionException {
		JoinCriteriaContext _localctx = new JoinCriteriaContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_joinCriteria);
		try {
			setState(2192);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ON:
				enterOuterAlt(_localctx, 1);
				{
				setState(2188);
				match(ON);
				setState(2189);
				booleanExpression(0);
				}
				break;
			case USING:
				enterOuterAlt(_localctx, 2);
				{
				setState(2190);
				match(USING);
				setState(2191);
				identifierList();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SampleContext extends ParserRuleContext {
		public TerminalNode TABLESAMPLE() { return getToken(ArcticExtendSparkSqlParser.TABLESAMPLE, 0); }
		public SampleMethodContext sampleMethod() {
			return getRuleContext(SampleMethodContext.class,0);
		}
		public SampleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sample; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSample(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSample(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSample(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleContext sample() throws RecognitionException {
		SampleContext _localctx = new SampleContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_sample);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2194);
			match(TABLESAMPLE);
			setState(2195);
			match(T__0);
			setState(2197);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,277,_ctx) ) {
			case 1:
				{
				setState(2196);
				sampleMethod();
				}
				break;
			}
			setState(2199);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SampleMethodContext extends ParserRuleContext {
		public SampleMethodContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sampleMethod; }
	 
		public SampleMethodContext() { }
		public void copyFrom(SampleMethodContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SampleByRowsContext extends SampleMethodContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode ROWS() { return getToken(ArcticExtendSparkSqlParser.ROWS, 0); }
		public SampleByRowsContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSampleByRows(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSampleByRows(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSampleByRows(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByPercentileContext extends SampleMethodContext {
		public Token negativeSign;
		public Token percentage;
		public TerminalNode PERCENTLIT() { return getToken(ArcticExtendSparkSqlParser.PERCENTLIT, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticExtendSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public SampleByPercentileContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSampleByPercentile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSampleByPercentile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSampleByPercentile(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByBucketContext extends SampleMethodContext {
		public Token sampleType;
		public Token numerator;
		public Token denominator;
		public TerminalNode OUT() { return getToken(ArcticExtendSparkSqlParser.OUT, 0); }
		public TerminalNode OF() { return getToken(ArcticExtendSparkSqlParser.OF, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticExtendSparkSqlParser.BUCKET, 0); }
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticExtendSparkSqlParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, i);
		}
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public SampleByBucketContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSampleByBucket(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSampleByBucket(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSampleByBucket(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByBytesContext extends SampleMethodContext {
		public ExpressionContext bytes;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SampleByBytesContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSampleByBytes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSampleByBytes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSampleByBytes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleMethodContext sampleMethod() throws RecognitionException {
		SampleMethodContext _localctx = new SampleMethodContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_sampleMethod);
		int _la;
		try {
			setState(2225);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,281,_ctx) ) {
			case 1:
				_localctx = new SampleByPercentileContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2202);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(2201);
					((SampleByPercentileContext)_localctx).negativeSign = match(MINUS);
					}
				}

				setState(2204);
				((SampleByPercentileContext)_localctx).percentage = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==INTEGER_VALUE || _la==DECIMAL_VALUE) ) {
					((SampleByPercentileContext)_localctx).percentage = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2205);
				match(PERCENTLIT);
				}
				break;
			case 2:
				_localctx = new SampleByRowsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2206);
				expression();
				setState(2207);
				match(ROWS);
				}
				break;
			case 3:
				_localctx = new SampleByBucketContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2209);
				((SampleByBucketContext)_localctx).sampleType = match(BUCKET);
				setState(2210);
				((SampleByBucketContext)_localctx).numerator = match(INTEGER_VALUE);
				setState(2211);
				match(OUT);
				setState(2212);
				match(OF);
				setState(2213);
				((SampleByBucketContext)_localctx).denominator = match(INTEGER_VALUE);
				setState(2222);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ON) {
					{
					setState(2214);
					match(ON);
					setState(2220);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,279,_ctx) ) {
					case 1:
						{
						setState(2215);
						identifier();
						}
						break;
					case 2:
						{
						setState(2216);
						qualifiedName();
						setState(2217);
						match(T__0);
						setState(2218);
						match(T__2);
						}
						break;
					}
					}
				}

				}
				break;
			case 4:
				_localctx = new SampleByBytesContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2224);
				((SampleByBytesContext)_localctx).bytes = expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierListContext extends ParserRuleContext {
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public IdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierListContext identifierList() throws RecognitionException {
		IdentifierListContext _localctx = new IdentifierListContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_identifierList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2227);
			match(T__0);
			setState(2228);
			identifierSeq();
			setState(2229);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierSeqContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext errorCapturingIdentifier;
		public List<ErrorCapturingIdentifierContext> ident = new ArrayList<ErrorCapturingIdentifierContext>();
		public List<ErrorCapturingIdentifierContext> errorCapturingIdentifier() {
			return getRuleContexts(ErrorCapturingIdentifierContext.class);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier(int i) {
			return getRuleContext(ErrorCapturingIdentifierContext.class,i);
		}
		public IdentifierSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIdentifierSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIdentifierSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIdentifierSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierSeqContext identifierSeq() throws RecognitionException {
		IdentifierSeqContext _localctx = new IdentifierSeqContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_identifierSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2231);
			((IdentifierSeqContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
			((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).errorCapturingIdentifier);
			setState(2236);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,282,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2232);
					match(T__1);
					setState(2233);
					((IdentifierSeqContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
					((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).errorCapturingIdentifier);
					}
					} 
				}
				setState(2238);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,282,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderedIdentifierListContext extends ParserRuleContext {
		public List<OrderedIdentifierContext> orderedIdentifier() {
			return getRuleContexts(OrderedIdentifierContext.class);
		}
		public OrderedIdentifierContext orderedIdentifier(int i) {
			return getRuleContext(OrderedIdentifierContext.class,i);
		}
		public OrderedIdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderedIdentifierList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterOrderedIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitOrderedIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitOrderedIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierListContext orderedIdentifierList() throws RecognitionException {
		OrderedIdentifierListContext _localctx = new OrderedIdentifierListContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_orderedIdentifierList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2239);
			match(T__0);
			setState(2240);
			orderedIdentifier();
			setState(2245);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2241);
				match(T__1);
				setState(2242);
				orderedIdentifier();
				}
				}
				setState(2247);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2248);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderedIdentifierContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext ident;
		public Token ordering;
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public TerminalNode ASC() { return getToken(ArcticExtendSparkSqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public OrderedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterOrderedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitOrderedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitOrderedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierContext orderedIdentifier() throws RecognitionException {
		OrderedIdentifierContext _localctx = new OrderedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_orderedIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2250);
			((OrderedIdentifierContext)_localctx).ident = errorCapturingIdentifier();
			setState(2252);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(2251);
				((OrderedIdentifierContext)_localctx).ordering = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
					((OrderedIdentifierContext)_localctx).ordering = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierCommentListContext extends ParserRuleContext {
		public List<IdentifierCommentContext> identifierComment() {
			return getRuleContexts(IdentifierCommentContext.class);
		}
		public IdentifierCommentContext identifierComment(int i) {
			return getRuleContext(IdentifierCommentContext.class,i);
		}
		public IdentifierCommentListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierCommentList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIdentifierCommentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIdentifierCommentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIdentifierCommentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierCommentListContext identifierCommentList() throws RecognitionException {
		IdentifierCommentListContext _localctx = new IdentifierCommentListContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_identifierCommentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2254);
			match(T__0);
			setState(2255);
			identifierComment();
			setState(2260);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2256);
				match(T__1);
				setState(2257);
				identifierComment();
				}
				}
				setState(2262);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2263);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierCommentContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public IdentifierCommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierComment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIdentifierComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIdentifierComment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIdentifierComment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierCommentContext identifierComment() throws RecognitionException {
		IdentifierCommentContext _localctx = new IdentifierCommentContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_identifierComment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2265);
			identifier();
			setState(2267);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(2266);
				commentSpec();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RelationPrimaryContext extends ParserRuleContext {
		public RelationPrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relationPrimary; }
	 
		public RelationPrimaryContext() { }
		public void copyFrom(RelationPrimaryContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TableValuedFunctionContext extends RelationPrimaryContext {
		public FunctionTableContext functionTable() {
			return getRuleContext(FunctionTableContext.class,0);
		}
		public TableValuedFunctionContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableValuedFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableValuedFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableValuedFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InlineTableDefault2Context extends RelationPrimaryContext {
		public InlineTableContext inlineTable() {
			return getRuleContext(InlineTableContext.class,0);
		}
		public InlineTableDefault2Context(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInlineTableDefault2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInlineTableDefault2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInlineTableDefault2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AliasedRelationContext extends RelationPrimaryContext {
		public RelationContext relation() {
			return getRuleContext(RelationContext.class,0);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public SampleContext sample() {
			return getRuleContext(SampleContext.class,0);
		}
		public AliasedRelationContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAliasedRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAliasedRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAliasedRelation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AliasedQueryContext extends RelationPrimaryContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public SampleContext sample() {
			return getRuleContext(SampleContext.class,0);
		}
		public AliasedQueryContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAliasedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAliasedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAliasedQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TableNameContext extends RelationPrimaryContext {
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public SampleContext sample() {
			return getRuleContext(SampleContext.class,0);
		}
		public TableNameContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationPrimaryContext relationPrimary() throws RecognitionException {
		RelationPrimaryContext _localctx = new RelationPrimaryContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_relationPrimary);
		try {
			setState(2293);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,290,_ctx) ) {
			case 1:
				_localctx = new TableNameContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2269);
				multipartIdentifier();
				setState(2271);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,287,_ctx) ) {
				case 1:
					{
					setState(2270);
					sample();
					}
					break;
				}
				setState(2273);
				tableAlias();
				}
				break;
			case 2:
				_localctx = new AliasedQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2275);
				match(T__0);
				setState(2276);
				query();
				setState(2277);
				match(T__2);
				setState(2279);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,288,_ctx) ) {
				case 1:
					{
					setState(2278);
					sample();
					}
					break;
				}
				setState(2281);
				tableAlias();
				}
				break;
			case 3:
				_localctx = new AliasedRelationContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2283);
				match(T__0);
				setState(2284);
				relation();
				setState(2285);
				match(T__2);
				setState(2287);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,289,_ctx) ) {
				case 1:
					{
					setState(2286);
					sample();
					}
					break;
				}
				setState(2289);
				tableAlias();
				}
				break;
			case 4:
				_localctx = new InlineTableDefault2Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2291);
				inlineTable();
				}
				break;
			case 5:
				_localctx = new TableValuedFunctionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(2292);
				functionTable();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineTableContext extends ParserRuleContext {
		public TerminalNode VALUES() { return getToken(ArcticExtendSparkSqlParser.VALUES, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public InlineTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInlineTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInlineTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInlineTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableContext inlineTable() throws RecognitionException {
		InlineTableContext _localctx = new InlineTableContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_inlineTable);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2295);
			match(VALUES);
			setState(2296);
			expression();
			setState(2301);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,291,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2297);
					match(T__1);
					setState(2298);
					expression();
					}
					} 
				}
				setState(2303);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,291,_ctx);
			}
			setState(2304);
			tableAlias();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionTableContext extends ParserRuleContext {
		public FunctionNameContext funcName;
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public FunctionTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFunctionTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFunctionTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFunctionTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionTableContext functionTable() throws RecognitionException {
		FunctionTableContext _localctx = new FunctionTableContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_functionTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2306);
			((FunctionTableContext)_localctx).funcName = functionName();
			setState(2307);
			match(T__0);
			setState(2316);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,293,_ctx) ) {
			case 1:
				{
				setState(2308);
				expression();
				setState(2313);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2309);
					match(T__1);
					setState(2310);
					expression();
					}
					}
					setState(2315);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
			setState(2318);
			match(T__2);
			setState(2319);
			tableAlias();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TableAliasContext extends ParserRuleContext {
		public StrictIdentifierContext strictIdentifier() {
			return getRuleContext(StrictIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TableAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableAlias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableAlias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableAliasContext tableAlias() throws RecognitionException {
		TableAliasContext _localctx = new TableAliasContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_tableAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2328);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,296,_ctx) ) {
			case 1:
				{
				setState(2322);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,294,_ctx) ) {
				case 1:
					{
					setState(2321);
					match(AS);
					}
					break;
				}
				setState(2324);
				strictIdentifier();
				setState(2326);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,295,_ctx) ) {
				case 1:
					{
					setState(2325);
					identifierList();
					}
					break;
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RowFormatContext extends ParserRuleContext {
		public RowFormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rowFormat; }
	 
		public RowFormatContext() { }
		public void copyFrom(RowFormatContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class RowFormatSerdeContext extends RowFormatContext {
		public Token name;
		public TablePropertyListContext props;
		public TerminalNode ROW() { return getToken(ArcticExtendSparkSqlParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticExtendSparkSqlParser.FORMAT, 0); }
		public TerminalNode SERDE() { return getToken(ArcticExtendSparkSqlParser.SERDE, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticExtendSparkSqlParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public RowFormatSerdeContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRowFormatSerde(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRowFormatSerde(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRowFormatSerde(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RowFormatDelimitedContext extends RowFormatContext {
		public Token fieldsTerminatedBy;
		public Token escapedBy;
		public Token collectionItemsTerminatedBy;
		public Token keysTerminatedBy;
		public Token linesSeparatedBy;
		public Token nullDefinedAs;
		public TerminalNode ROW() { return getToken(ArcticExtendSparkSqlParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticExtendSparkSqlParser.FORMAT, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticExtendSparkSqlParser.DELIMITED, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticExtendSparkSqlParser.FIELDS, 0); }
		public List<TerminalNode> TERMINATED() { return getTokens(ArcticExtendSparkSqlParser.TERMINATED); }
		public TerminalNode TERMINATED(int i) {
			return getToken(ArcticExtendSparkSqlParser.TERMINATED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticExtendSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticExtendSparkSqlParser.BY, i);
		}
		public TerminalNode COLLECTION() { return getToken(ArcticExtendSparkSqlParser.COLLECTION, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticExtendSparkSqlParser.ITEMS, 0); }
		public TerminalNode MAP() { return getToken(ArcticExtendSparkSqlParser.MAP, 0); }
		public TerminalNode KEYS() { return getToken(ArcticExtendSparkSqlParser.KEYS, 0); }
		public TerminalNode LINES() { return getToken(ArcticExtendSparkSqlParser.LINES, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticExtendSparkSqlParser.DEFINED, 0); }
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticExtendSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticExtendSparkSqlParser.STRING, i);
		}
		public TerminalNode ESCAPED() { return getToken(ArcticExtendSparkSqlParser.ESCAPED, 0); }
		public RowFormatDelimitedContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRowFormatDelimited(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRowFormatDelimited(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRowFormatDelimited(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RowFormatContext rowFormat() throws RecognitionException {
		RowFormatContext _localctx = new RowFormatContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_rowFormat);
		try {
			setState(2379);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,304,_ctx) ) {
			case 1:
				_localctx = new RowFormatSerdeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2330);
				match(ROW);
				setState(2331);
				match(FORMAT);
				setState(2332);
				match(SERDE);
				setState(2333);
				((RowFormatSerdeContext)_localctx).name = match(STRING);
				setState(2337);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,297,_ctx) ) {
				case 1:
					{
					setState(2334);
					match(WITH);
					setState(2335);
					match(SERDEPROPERTIES);
					setState(2336);
					((RowFormatSerdeContext)_localctx).props = tablePropertyList();
					}
					break;
				}
				}
				break;
			case 2:
				_localctx = new RowFormatDelimitedContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2339);
				match(ROW);
				setState(2340);
				match(FORMAT);
				setState(2341);
				match(DELIMITED);
				setState(2351);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,299,_ctx) ) {
				case 1:
					{
					setState(2342);
					match(FIELDS);
					setState(2343);
					match(TERMINATED);
					setState(2344);
					match(BY);
					setState(2345);
					((RowFormatDelimitedContext)_localctx).fieldsTerminatedBy = match(STRING);
					setState(2349);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,298,_ctx) ) {
					case 1:
						{
						setState(2346);
						match(ESCAPED);
						setState(2347);
						match(BY);
						setState(2348);
						((RowFormatDelimitedContext)_localctx).escapedBy = match(STRING);
						}
						break;
					}
					}
					break;
				}
				setState(2358);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,300,_ctx) ) {
				case 1:
					{
					setState(2353);
					match(COLLECTION);
					setState(2354);
					match(ITEMS);
					setState(2355);
					match(TERMINATED);
					setState(2356);
					match(BY);
					setState(2357);
					((RowFormatDelimitedContext)_localctx).collectionItemsTerminatedBy = match(STRING);
					}
					break;
				}
				setState(2365);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,301,_ctx) ) {
				case 1:
					{
					setState(2360);
					match(MAP);
					setState(2361);
					match(KEYS);
					setState(2362);
					match(TERMINATED);
					setState(2363);
					match(BY);
					setState(2364);
					((RowFormatDelimitedContext)_localctx).keysTerminatedBy = match(STRING);
					}
					break;
				}
				setState(2371);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,302,_ctx) ) {
				case 1:
					{
					setState(2367);
					match(LINES);
					setState(2368);
					match(TERMINATED);
					setState(2369);
					match(BY);
					setState(2370);
					((RowFormatDelimitedContext)_localctx).linesSeparatedBy = match(STRING);
					}
					break;
				}
				setState(2377);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,303,_ctx) ) {
				case 1:
					{
					setState(2373);
					match(NULL);
					setState(2374);
					match(DEFINED);
					setState(2375);
					match(AS);
					setState(2376);
					((RowFormatDelimitedContext)_localctx).nullDefinedAs = match(STRING);
					}
					break;
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultipartIdentifierListContext extends ParserRuleContext {
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public MultipartIdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multipartIdentifierList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMultipartIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMultipartIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMultipartIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultipartIdentifierListContext multipartIdentifierList() throws RecognitionException {
		MultipartIdentifierListContext _localctx = new MultipartIdentifierListContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_multipartIdentifierList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2381);
			multipartIdentifier();
			setState(2386);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2382);
				match(T__1);
				setState(2383);
				multipartIdentifier();
				}
				}
				setState(2388);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultipartIdentifierContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext errorCapturingIdentifier;
		public List<ErrorCapturingIdentifierContext> parts = new ArrayList<ErrorCapturingIdentifierContext>();
		public List<ErrorCapturingIdentifierContext> errorCapturingIdentifier() {
			return getRuleContexts(ErrorCapturingIdentifierContext.class);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier(int i) {
			return getRuleContext(ErrorCapturingIdentifierContext.class,i);
		}
		public MultipartIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multipartIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMultipartIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMultipartIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMultipartIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultipartIdentifierContext multipartIdentifier() throws RecognitionException {
		MultipartIdentifierContext _localctx = new MultipartIdentifierContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_multipartIdentifier);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2389);
			((MultipartIdentifierContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
			((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).errorCapturingIdentifier);
			setState(2394);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,306,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2390);
					match(T__4);
					setState(2391);
					((MultipartIdentifierContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
					((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).errorCapturingIdentifier);
					}
					} 
				}
				setState(2396);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,306,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TableIdentifierContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext db;
		public ErrorCapturingIdentifierContext table;
		public List<ErrorCapturingIdentifierContext> errorCapturingIdentifier() {
			return getRuleContexts(ErrorCapturingIdentifierContext.class);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier(int i) {
			return getRuleContext(ErrorCapturingIdentifierContext.class,i);
		}
		public TableIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableIdentifierContext tableIdentifier() throws RecognitionException {
		TableIdentifierContext _localctx = new TableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_tableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2400);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,307,_ctx) ) {
			case 1:
				{
				setState(2397);
				((TableIdentifierContext)_localctx).db = errorCapturingIdentifier();
				setState(2398);
				match(T__4);
				}
				break;
			}
			setState(2402);
			((TableIdentifierContext)_localctx).table = errorCapturingIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionIdentifierContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext db;
		public ErrorCapturingIdentifierContext function;
		public List<ErrorCapturingIdentifierContext> errorCapturingIdentifier() {
			return getRuleContexts(ErrorCapturingIdentifierContext.class);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier(int i) {
			return getRuleContext(ErrorCapturingIdentifierContext.class,i);
		}
		public FunctionIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFunctionIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFunctionIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFunctionIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionIdentifierContext functionIdentifier() throws RecognitionException {
		FunctionIdentifierContext _localctx = new FunctionIdentifierContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_functionIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2407);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,308,_ctx) ) {
			case 1:
				{
				setState(2404);
				((FunctionIdentifierContext)_localctx).db = errorCapturingIdentifier();
				setState(2405);
				match(T__4);
				}
				break;
			}
			setState(2409);
			((FunctionIdentifierContext)_localctx).function = errorCapturingIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedExpressionContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext name;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public NamedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNamedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNamedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNamedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionContext namedExpression() throws RecognitionException {
		NamedExpressionContext _localctx = new NamedExpressionContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_namedExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2411);
			expression();
			setState(2419);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,311,_ctx) ) {
			case 1:
				{
				setState(2413);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,309,_ctx) ) {
				case 1:
					{
					setState(2412);
					match(AS);
					}
					break;
				}
				setState(2417);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,310,_ctx) ) {
				case 1:
					{
					setState(2415);
					((NamedExpressionContext)_localctx).name = errorCapturingIdentifier();
					}
					break;
				case 2:
					{
					setState(2416);
					identifierList();
					}
					break;
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedExpressionSeqContext extends ParserRuleContext {
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public NamedExpressionSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedExpressionSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNamedExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNamedExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNamedExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionSeqContext namedExpressionSeq() throws RecognitionException {
		NamedExpressionSeqContext _localctx = new NamedExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_namedExpressionSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2421);
			namedExpression();
			setState(2426);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,312,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2422);
					match(T__1);
					setState(2423);
					namedExpression();
					}
					} 
				}
				setState(2428);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,312,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PartitionFieldListContext extends ParserRuleContext {
		public PartitionFieldContext partitionField;
		public List<PartitionFieldContext> fields = new ArrayList<PartitionFieldContext>();
		public List<PartitionFieldContext> partitionField() {
			return getRuleContexts(PartitionFieldContext.class);
		}
		public PartitionFieldContext partitionField(int i) {
			return getRuleContext(PartitionFieldContext.class,i);
		}
		public PartitionFieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionFieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPartitionFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPartitionFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPartitionFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldListContext partitionFieldList() throws RecognitionException {
		PartitionFieldListContext _localctx = new PartitionFieldListContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_partitionFieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2429);
			match(T__0);
			setState(2430);
			((PartitionFieldListContext)_localctx).partitionField = partitionField();
			((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
			setState(2435);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2431);
				match(T__1);
				setState(2432);
				((PartitionFieldListContext)_localctx).partitionField = partitionField();
				((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
				}
				}
				setState(2437);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2438);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PartitionFieldContext extends ParserRuleContext {
		public PartitionFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionField; }
	 
		public PartitionFieldContext() { }
		public void copyFrom(PartitionFieldContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class PartitionColumnContext extends PartitionFieldContext {
		public ColTypeContext colType() {
			return getRuleContext(ColTypeContext.class,0);
		}
		public PartitionColumnContext(PartitionFieldContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPartitionColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPartitionColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPartitionColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PartitionTransformContext extends PartitionFieldContext {
		public TransformContext transform() {
			return getRuleContext(TransformContext.class,0);
		}
		public PartitionTransformContext(PartitionFieldContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPartitionTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPartitionTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPartitionTransform(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldContext partitionField() throws RecognitionException {
		PartitionFieldContext _localctx = new PartitionFieldContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_partitionField);
		try {
			setState(2442);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,314,_ctx) ) {
			case 1:
				_localctx = new PartitionTransformContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2440);
				transform();
				}
				break;
			case 2:
				_localctx = new PartitionColumnContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2441);
				colType();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TransformContext extends ParserRuleContext {
		public TransformContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transform; }
	 
		public TransformContext() { }
		public void copyFrom(TransformContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class IdentityTransformContext extends TransformContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public IdentityTransformContext(TransformContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIdentityTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIdentityTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIdentityTransform(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ApplyTransformContext extends TransformContext {
		public IdentifierContext transformName;
		public TransformArgumentContext transformArgument;
		public List<TransformArgumentContext> argument = new ArrayList<TransformArgumentContext>();
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<TransformArgumentContext> transformArgument() {
			return getRuleContexts(TransformArgumentContext.class);
		}
		public TransformArgumentContext transformArgument(int i) {
			return getRuleContext(TransformArgumentContext.class,i);
		}
		public ApplyTransformContext(TransformContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterApplyTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitApplyTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitApplyTransform(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformContext transform() throws RecognitionException {
		TransformContext _localctx = new TransformContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_transform);
		int _la;
		try {
			setState(2457);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,316,_ctx) ) {
			case 1:
				_localctx = new IdentityTransformContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2444);
				qualifiedName();
				}
				break;
			case 2:
				_localctx = new ApplyTransformContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2445);
				((ApplyTransformContext)_localctx).transformName = identifier();
				setState(2446);
				match(T__0);
				setState(2447);
				((ApplyTransformContext)_localctx).transformArgument = transformArgument();
				((ApplyTransformContext)_localctx).argument.add(((ApplyTransformContext)_localctx).transformArgument);
				setState(2452);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2448);
					match(T__1);
					setState(2449);
					((ApplyTransformContext)_localctx).transformArgument = transformArgument();
					((ApplyTransformContext)_localctx).argument.add(((ApplyTransformContext)_localctx).transformArgument);
					}
					}
					setState(2454);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2455);
				match(T__2);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TransformArgumentContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public TransformArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transformArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTransformArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTransformArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTransformArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformArgumentContext transformArgument() throws RecognitionException {
		TransformArgumentContext _localctx = new TransformArgumentContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_transformArgument);
		try {
			setState(2461);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,317,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2459);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2460);
				constant();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2463);
			booleanExpression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionSeqContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ExpressionSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionSeqContext expressionSeq() throws RecognitionException {
		ExpressionSeqContext _localctx = new ExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_expressionSeq);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2465);
			expression();
			setState(2470);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2466);
				match(T__1);
				setState(2467);
				expression();
				}
				}
				setState(2472);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BooleanExpressionContext extends ParserRuleContext {
		public BooleanExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanExpression; }
	 
		public BooleanExpressionContext() { }
		public void copyFrom(BooleanExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LogicalNotContext extends BooleanExpressionContext {
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public LogicalNotContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLogicalNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLogicalNot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLogicalNot(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PredicatedContext extends BooleanExpressionContext {
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public PredicateContext predicate() {
			return getRuleContext(PredicateContext.class,0);
		}
		public PredicatedContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPredicated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPredicated(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPredicated(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExistsContext extends BooleanExpressionContext {
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public ExistsContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterExists(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitExists(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitExists(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LogicalBinaryContext extends BooleanExpressionContext {
		public BooleanExpressionContext left;
		public Token operator;
		public BooleanExpressionContext right;
		public List<BooleanExpressionContext> booleanExpression() {
			return getRuleContexts(BooleanExpressionContext.class);
		}
		public BooleanExpressionContext booleanExpression(int i) {
			return getRuleContext(BooleanExpressionContext.class,i);
		}
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public LogicalBinaryContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLogicalBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLogicalBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLogicalBinary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanExpressionContext booleanExpression() throws RecognitionException {
		return booleanExpression(0);
	}

	private BooleanExpressionContext booleanExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		BooleanExpressionContext _localctx = new BooleanExpressionContext(_ctx, _parentState);
		BooleanExpressionContext _prevctx = _localctx;
		int _startState = 212;
		enterRecursionRule(_localctx, 212, RULE_booleanExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2485);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,320,_ctx) ) {
			case 1:
				{
				_localctx = new LogicalNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(2474);
				match(NOT);
				setState(2475);
				booleanExpression(5);
				}
				break;
			case 2:
				{
				_localctx = new ExistsContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2476);
				match(EXISTS);
				setState(2477);
				match(T__0);
				setState(2478);
				query();
				setState(2479);
				match(T__2);
				}
				break;
			case 3:
				{
				_localctx = new PredicatedContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2481);
				valueExpression(0);
				setState(2483);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,319,_ctx) ) {
				case 1:
					{
					setState(2482);
					predicate();
					}
					break;
				}
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(2495);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,322,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(2493);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,321,_ctx) ) {
					case 1:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(2487);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(2488);
						((LogicalBinaryContext)_localctx).operator = match(AND);
						setState(2489);
						((LogicalBinaryContext)_localctx).right = booleanExpression(3);
						}
						break;
					case 2:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(2490);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(2491);
						((LogicalBinaryContext)_localctx).operator = match(OR);
						setState(2492);
						((LogicalBinaryContext)_localctx).right = booleanExpression(2);
						}
						break;
					}
					} 
				}
				setState(2497);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,322,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class PredicateContext extends ParserRuleContext {
		public Token kind;
		public ValueExpressionContext lower;
		public ValueExpressionContext upper;
		public ValueExpressionContext pattern;
		public Token quantifier;
		public Token escapeChar;
		public ValueExpressionContext right;
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticExtendSparkSqlParser.BETWEEN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RLIKE() { return getToken(ArcticExtendSparkSqlParser.RLIKE, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public TerminalNode ANY() { return getToken(ArcticExtendSparkSqlParser.ANY, 0); }
		public TerminalNode SOME() { return getToken(ArcticExtendSparkSqlParser.SOME, 0); }
		public TerminalNode ALL() { return getToken(ArcticExtendSparkSqlParser.ALL, 0); }
		public TerminalNode ESCAPE() { return getToken(ArcticExtendSparkSqlParser.ESCAPE, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode IS() { return getToken(ArcticExtendSparkSqlParser.IS, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public TerminalNode TRUE() { return getToken(ArcticExtendSparkSqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticExtendSparkSqlParser.FALSE, 0); }
		public TerminalNode UNKNOWN() { return getToken(ArcticExtendSparkSqlParser.UNKNOWN, 0); }
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticExtendSparkSqlParser.DISTINCT, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_predicate);
		int _la;
		try {
			setState(2580);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,336,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2499);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2498);
					match(NOT);
					}
				}

				setState(2501);
				((PredicateContext)_localctx).kind = match(BETWEEN);
				setState(2502);
				((PredicateContext)_localctx).lower = valueExpression(0);
				setState(2503);
				match(AND);
				setState(2504);
				((PredicateContext)_localctx).upper = valueExpression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2507);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2506);
					match(NOT);
					}
				}

				setState(2509);
				((PredicateContext)_localctx).kind = match(IN);
				setState(2510);
				match(T__0);
				setState(2511);
				expression();
				setState(2516);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(2512);
					match(T__1);
					setState(2513);
					expression();
					}
					}
					setState(2518);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2519);
				match(T__2);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2522);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2521);
					match(NOT);
					}
				}

				setState(2524);
				((PredicateContext)_localctx).kind = match(IN);
				setState(2525);
				match(T__0);
				setState(2526);
				query();
				setState(2527);
				match(T__2);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(2530);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2529);
					match(NOT);
					}
				}

				setState(2532);
				((PredicateContext)_localctx).kind = match(RLIKE);
				setState(2533);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(2535);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2534);
					match(NOT);
					}
				}

				setState(2537);
				((PredicateContext)_localctx).kind = match(LIKE);
				setState(2538);
				((PredicateContext)_localctx).quantifier = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ALL || _la==ANY || _la==SOME) ) {
					((PredicateContext)_localctx).quantifier = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2552);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,330,_ctx) ) {
				case 1:
					{
					setState(2539);
					match(T__0);
					setState(2540);
					match(T__2);
					}
					break;
				case 2:
					{
					setState(2541);
					match(T__0);
					setState(2542);
					expression();
					setState(2547);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(2543);
						match(T__1);
						setState(2544);
						expression();
						}
						}
						setState(2549);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(2550);
					match(T__2);
					}
					break;
				}
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(2555);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2554);
					match(NOT);
					}
				}

				setState(2557);
				((PredicateContext)_localctx).kind = match(LIKE);
				setState(2558);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				setState(2561);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,332,_ctx) ) {
				case 1:
					{
					setState(2559);
					match(ESCAPE);
					setState(2560);
					((PredicateContext)_localctx).escapeChar = match(STRING);
					}
					break;
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(2563);
				match(IS);
				setState(2565);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2564);
					match(NOT);
					}
				}

				setState(2567);
				((PredicateContext)_localctx).kind = match(NULL);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(2568);
				match(IS);
				setState(2570);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2569);
					match(NOT);
					}
				}

				setState(2572);
				((PredicateContext)_localctx).kind = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==FALSE || _la==TRUE || _la==UNKNOWN) ) {
					((PredicateContext)_localctx).kind = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(2573);
				match(IS);
				setState(2575);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2574);
					match(NOT);
					}
				}

				setState(2577);
				((PredicateContext)_localctx).kind = match(DISTINCT);
				setState(2578);
				match(FROM);
				setState(2579);
				((PredicateContext)_localctx).right = valueExpression(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueExpressionContext extends ParserRuleContext {
		public ValueExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueExpression; }
	 
		public ValueExpressionContext() { }
		public void copyFrom(ValueExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ValueExpressionDefaultContext extends ValueExpressionContext {
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public ValueExpressionDefaultContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterValueExpressionDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitValueExpressionDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitValueExpressionDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ComparisonContext extends ValueExpressionContext {
		public ValueExpressionContext left;
		public ValueExpressionContext right;
		public ComparisonOperatorContext comparisonOperator() {
			return getRuleContext(ComparisonOperatorContext.class,0);
		}
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public ComparisonContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitComparison(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitComparison(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ArithmeticBinaryContext extends ValueExpressionContext {
		public ValueExpressionContext left;
		public Token operator;
		public ValueExpressionContext right;
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode ASTERISK() { return getToken(ArcticExtendSparkSqlParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticExtendSparkSqlParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticExtendSparkSqlParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticExtendSparkSqlParser.DIV, 0); }
		public TerminalNode PLUS() { return getToken(ArcticExtendSparkSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticExtendSparkSqlParser.CONCAT_PIPE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticExtendSparkSqlParser.AMPERSAND, 0); }
		public TerminalNode HAT() { return getToken(ArcticExtendSparkSqlParser.HAT, 0); }
		public TerminalNode PIPE() { return getToken(ArcticExtendSparkSqlParser.PIPE, 0); }
		public ArithmeticBinaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterArithmeticBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitArithmeticBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitArithmeticBinary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ArithmeticUnaryContext extends ValueExpressionContext {
		public Token operator;
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(ArcticExtendSparkSqlParser.PLUS, 0); }
		public TerminalNode TILDE() { return getToken(ArcticExtendSparkSqlParser.TILDE, 0); }
		public ArithmeticUnaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterArithmeticUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitArithmeticUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitArithmeticUnary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueExpressionContext valueExpression() throws RecognitionException {
		return valueExpression(0);
	}

	private ValueExpressionContext valueExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ValueExpressionContext _localctx = new ValueExpressionContext(_ctx, _parentState);
		ValueExpressionContext _prevctx = _localctx;
		int _startState = 216;
		enterRecursionRule(_localctx, 216, RULE_valueExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2586);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,337,_ctx) ) {
			case 1:
				{
				_localctx = new ValueExpressionDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(2583);
				primaryExpression(0);
				}
				break;
			case 2:
				{
				_localctx = new ArithmeticUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2584);
				((ArithmeticUnaryContext)_localctx).operator = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 280)) & ~0x3f) == 0 && ((1L << (_la - 280)) & ((1L << (PLUS - 280)) | (1L << (MINUS - 280)) | (1L << (TILDE - 280)))) != 0)) ) {
					((ArithmeticUnaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2585);
				valueExpression(7);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(2609);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,339,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(2607);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,338,_ctx) ) {
					case 1:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2588);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(2589);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==DIV || ((((_la - 282)) & ~0x3f) == 0 && ((1L << (_la - 282)) & ((1L << (ASTERISK - 282)) | (1L << (SLASH - 282)) | (1L << (PERCENT - 282)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(2590);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(7);
						}
						break;
					case 2:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2591);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(2592);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 280)) & ~0x3f) == 0 && ((1L << (_la - 280)) & ((1L << (PLUS - 280)) | (1L << (MINUS - 280)) | (1L << (CONCAT_PIPE - 280)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(2593);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(6);
						}
						break;
					case 3:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2594);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(2595);
						((ArithmeticBinaryContext)_localctx).operator = match(AMPERSAND);
						setState(2596);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(5);
						}
						break;
					case 4:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2597);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(2598);
						((ArithmeticBinaryContext)_localctx).operator = match(HAT);
						setState(2599);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(4);
						}
						break;
					case 5:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2600);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(2601);
						((ArithmeticBinaryContext)_localctx).operator = match(PIPE);
						setState(2602);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(3);
						}
						break;
					case 6:
						{
						_localctx = new ComparisonContext(new ValueExpressionContext(_parentctx, _parentState));
						((ComparisonContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2603);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(2604);
						comparisonOperator();
						setState(2605);
						((ComparisonContext)_localctx).right = valueExpression(2);
						}
						break;
					}
					} 
				}
				setState(2611);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,339,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class PrimaryExpressionContext extends ParserRuleContext {
		public PrimaryExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryExpression; }
	 
		public PrimaryExpressionContext() { }
		public void copyFrom(PrimaryExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class StructContext extends PrimaryExpressionContext {
		public NamedExpressionContext namedExpression;
		public List<NamedExpressionContext> argument = new ArrayList<NamedExpressionContext>();
		public TerminalNode STRUCT() { return getToken(ArcticExtendSparkSqlParser.STRUCT, 0); }
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public StructContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterStruct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitStruct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitStruct(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DereferenceContext extends PrimaryExpressionContext {
		public PrimaryExpressionContext base;
		public IdentifierContext fieldName;
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public DereferenceContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDereference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDereference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDereference(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleCaseContext extends PrimaryExpressionContext {
		public ExpressionContext value;
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticExtendSparkSqlParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticExtendSparkSqlParser.END, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<WhenClauseContext> whenClause() {
			return getRuleContexts(WhenClauseContext.class);
		}
		public WhenClauseContext whenClause(int i) {
			return getRuleContext(WhenClauseContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(ArcticExtendSparkSqlParser.ELSE, 0); }
		public SimpleCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSimpleCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSimpleCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSimpleCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CurrentLikeContext extends PrimaryExpressionContext {
		public Token name;
		public TerminalNode CURRENT_DATE() { return getToken(ArcticExtendSparkSqlParser.CURRENT_DATE, 0); }
		public TerminalNode CURRENT_TIMESTAMP() { return getToken(ArcticExtendSparkSqlParser.CURRENT_TIMESTAMP, 0); }
		public TerminalNode CURRENT_USER() { return getToken(ArcticExtendSparkSqlParser.CURRENT_USER, 0); }
		public CurrentLikeContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCurrentLike(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCurrentLike(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCurrentLike(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ColumnReferenceContext extends PrimaryExpressionContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ColumnReferenceContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterColumnReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitColumnReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitColumnReference(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RowConstructorContext extends PrimaryExpressionContext {
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public RowConstructorContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRowConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRowConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRowConstructor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LastContext extends PrimaryExpressionContext {
		public TerminalNode LAST() { return getToken(ArcticExtendSparkSqlParser.LAST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticExtendSparkSqlParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticExtendSparkSqlParser.NULLS, 0); }
		public LastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLast(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StarContext extends PrimaryExpressionContext {
		public TerminalNode ASTERISK() { return getToken(ArcticExtendSparkSqlParser.ASTERISK, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public StarContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterStar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitStar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitStar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OverlayContext extends PrimaryExpressionContext {
		public ValueExpressionContext input;
		public ValueExpressionContext replace;
		public ValueExpressionContext position;
		public ValueExpressionContext length;
		public TerminalNode OVERLAY() { return getToken(ArcticExtendSparkSqlParser.OVERLAY, 0); }
		public TerminalNode PLACING() { return getToken(ArcticExtendSparkSqlParser.PLACING, 0); }
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode FOR() { return getToken(ArcticExtendSparkSqlParser.FOR, 0); }
		public OverlayContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterOverlay(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitOverlay(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitOverlay(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubscriptContext extends PrimaryExpressionContext {
		public PrimaryExpressionContext value;
		public ValueExpressionContext index;
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public SubscriptContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSubscript(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSubscript(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSubscript(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubqueryExpressionContext extends PrimaryExpressionContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public SubqueryExpressionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSubqueryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSubqueryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSubqueryExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubstringContext extends PrimaryExpressionContext {
		public ValueExpressionContext str;
		public ValueExpressionContext pos;
		public ValueExpressionContext len;
		public TerminalNode SUBSTR() { return getToken(ArcticExtendSparkSqlParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticExtendSparkSqlParser.SUBSTRING, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode FOR() { return getToken(ArcticExtendSparkSqlParser.FOR, 0); }
		public SubstringContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSubstring(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSubstring(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSubstring(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CastContext extends PrimaryExpressionContext {
		public Token name;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode CAST() { return getToken(ArcticExtendSparkSqlParser.CAST, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticExtendSparkSqlParser.TRY_CAST, 0); }
		public CastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitCast(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ConstantDefaultContext extends PrimaryExpressionContext {
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public ConstantDefaultContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterConstantDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitConstantDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitConstantDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LambdaContext extends PrimaryExpressionContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public LambdaContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLambda(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLambda(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLambda(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParenthesizedExpressionContext extends PrimaryExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ParenthesizedExpressionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterParenthesizedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitParenthesizedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitParenthesizedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExtractContext extends PrimaryExpressionContext {
		public IdentifierContext field;
		public ValueExpressionContext source;
		public TerminalNode EXTRACT() { return getToken(ArcticExtendSparkSqlParser.EXTRACT, 0); }
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public ExtractContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterExtract(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitExtract(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitExtract(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TrimContext extends PrimaryExpressionContext {
		public Token trimOption;
		public ValueExpressionContext trimStr;
		public ValueExpressionContext srcStr;
		public TerminalNode TRIM() { return getToken(ArcticExtendSparkSqlParser.TRIM, 0); }
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode BOTH() { return getToken(ArcticExtendSparkSqlParser.BOTH, 0); }
		public TerminalNode LEADING() { return getToken(ArcticExtendSparkSqlParser.LEADING, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticExtendSparkSqlParser.TRAILING, 0); }
		public TrimContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTrim(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTrim(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTrim(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FunctionCallContext extends PrimaryExpressionContext {
		public ExpressionContext expression;
		public List<ExpressionContext> argument = new ArrayList<ExpressionContext>();
		public BooleanExpressionContext where;
		public Token nullsOption;
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public TerminalNode FILTER() { return getToken(ArcticExtendSparkSqlParser.FILTER, 0); }
		public TerminalNode WHERE() { return getToken(ArcticExtendSparkSqlParser.WHERE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticExtendSparkSqlParser.NULLS, 0); }
		public TerminalNode OVER() { return getToken(ArcticExtendSparkSqlParser.OVER, 0); }
		public WindowSpecContext windowSpec() {
			return getRuleContext(WindowSpecContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticExtendSparkSqlParser.IGNORE, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticExtendSparkSqlParser.RESPECT, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public FunctionCallContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SearchedCaseContext extends PrimaryExpressionContext {
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticExtendSparkSqlParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticExtendSparkSqlParser.END, 0); }
		public List<WhenClauseContext> whenClause() {
			return getRuleContexts(WhenClauseContext.class);
		}
		public WhenClauseContext whenClause(int i) {
			return getRuleContext(WhenClauseContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(ArcticExtendSparkSqlParser.ELSE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SearchedCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSearchedCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSearchedCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSearchedCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PositionContext extends PrimaryExpressionContext {
		public ValueExpressionContext substr;
		public ValueExpressionContext str;
		public TerminalNode POSITION() { return getToken(ArcticExtendSparkSqlParser.POSITION, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public PositionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPosition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FirstContext extends PrimaryExpressionContext {
		public TerminalNode FIRST() { return getToken(ArcticExtendSparkSqlParser.FIRST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticExtendSparkSqlParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticExtendSparkSqlParser.NULLS, 0); }
		public FirstContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFirst(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFirst(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExpressionContext primaryExpression() throws RecognitionException {
		return primaryExpression(0);
	}

	private PrimaryExpressionContext primaryExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		PrimaryExpressionContext _localctx = new PrimaryExpressionContext(_ctx, _parentState);
		PrimaryExpressionContext _prevctx = _localctx;
		int _startState = 218;
		enterRecursionRule(_localctx, 218, RULE_primaryExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2800);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,360,_ctx) ) {
			case 1:
				{
				_localctx = new CurrentLikeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(2613);
				((CurrentLikeContext)_localctx).name = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << CURRENT_DATE) | (1L << CURRENT_TIMESTAMP) | (1L << CURRENT_USER))) != 0)) ) {
					((CurrentLikeContext)_localctx).name = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 2:
				{
				_localctx = new SearchedCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2614);
				match(CASE);
				setState(2616); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2615);
					whenClause();
					}
					}
					setState(2618); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(2622);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(2620);
					match(ELSE);
					setState(2621);
					((SearchedCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(2624);
				match(END);
				}
				break;
			case 3:
				{
				_localctx = new SimpleCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2626);
				match(CASE);
				setState(2627);
				((SimpleCaseContext)_localctx).value = expression();
				setState(2629); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2628);
					whenClause();
					}
					}
					setState(2631); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(2635);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(2633);
					match(ELSE);
					setState(2634);
					((SimpleCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(2637);
				match(END);
				}
				break;
			case 4:
				{
				_localctx = new CastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2639);
				((CastContext)_localctx).name = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==CAST || _la==TRY_CAST) ) {
					((CastContext)_localctx).name = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2640);
				match(T__0);
				setState(2641);
				expression();
				setState(2642);
				match(AS);
				setState(2643);
				dataType();
				setState(2644);
				match(T__2);
				}
				break;
			case 5:
				{
				_localctx = new StructContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2646);
				match(STRUCT);
				setState(2647);
				match(T__0);
				setState(2656);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,345,_ctx) ) {
				case 1:
					{
					setState(2648);
					((StructContext)_localctx).namedExpression = namedExpression();
					((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
					setState(2653);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(2649);
						match(T__1);
						setState(2650);
						((StructContext)_localctx).namedExpression = namedExpression();
						((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
						}
						}
						setState(2655);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(2658);
				match(T__2);
				}
				break;
			case 6:
				{
				_localctx = new FirstContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2659);
				match(FIRST);
				setState(2660);
				match(T__0);
				setState(2661);
				expression();
				setState(2664);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(2662);
					match(IGNORE);
					setState(2663);
					match(NULLS);
					}
				}

				setState(2666);
				match(T__2);
				}
				break;
			case 7:
				{
				_localctx = new LastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2668);
				match(LAST);
				setState(2669);
				match(T__0);
				setState(2670);
				expression();
				setState(2673);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(2671);
					match(IGNORE);
					setState(2672);
					match(NULLS);
					}
				}

				setState(2675);
				match(T__2);
				}
				break;
			case 8:
				{
				_localctx = new PositionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2677);
				match(POSITION);
				setState(2678);
				match(T__0);
				setState(2679);
				((PositionContext)_localctx).substr = valueExpression(0);
				setState(2680);
				match(IN);
				setState(2681);
				((PositionContext)_localctx).str = valueExpression(0);
				setState(2682);
				match(T__2);
				}
				break;
			case 9:
				{
				_localctx = new ConstantDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2684);
				constant();
				}
				break;
			case 10:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2685);
				match(ASTERISK);
				}
				break;
			case 11:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2686);
				qualifiedName();
				setState(2687);
				match(T__4);
				setState(2688);
				match(ASTERISK);
				}
				break;
			case 12:
				{
				_localctx = new RowConstructorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2690);
				match(T__0);
				setState(2691);
				namedExpression();
				setState(2694); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2692);
					match(T__1);
					setState(2693);
					namedExpression();
					}
					}
					setState(2696); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__1 );
				setState(2698);
				match(T__2);
				}
				break;
			case 13:
				{
				_localctx = new SubqueryExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2700);
				match(T__0);
				setState(2701);
				query();
				setState(2702);
				match(T__2);
				}
				break;
			case 14:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2704);
				functionName();
				setState(2705);
				match(T__0);
				setState(2717);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,351,_ctx) ) {
				case 1:
					{
					setState(2707);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,349,_ctx) ) {
					case 1:
						{
						setState(2706);
						setQuantifier();
						}
						break;
					}
					setState(2709);
					((FunctionCallContext)_localctx).expression = expression();
					((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
					setState(2714);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(2710);
						match(T__1);
						setState(2711);
						((FunctionCallContext)_localctx).expression = expression();
						((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
						}
						}
						setState(2716);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(2719);
				match(T__2);
				setState(2726);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,352,_ctx) ) {
				case 1:
					{
					setState(2720);
					match(FILTER);
					setState(2721);
					match(T__0);
					setState(2722);
					match(WHERE);
					setState(2723);
					((FunctionCallContext)_localctx).where = booleanExpression(0);
					setState(2724);
					match(T__2);
					}
					break;
				}
				setState(2730);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,353,_ctx) ) {
				case 1:
					{
					setState(2728);
					((FunctionCallContext)_localctx).nullsOption = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==IGNORE || _la==RESPECT) ) {
						((FunctionCallContext)_localctx).nullsOption = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(2729);
					match(NULLS);
					}
					break;
				}
				setState(2734);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,354,_ctx) ) {
				case 1:
					{
					setState(2732);
					match(OVER);
					setState(2733);
					windowSpec();
					}
					break;
				}
				}
				break;
			case 15:
				{
				_localctx = new LambdaContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2736);
				identifier();
				setState(2737);
				match(T__7);
				setState(2738);
				expression();
				}
				break;
			case 16:
				{
				_localctx = new LambdaContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2740);
				match(T__0);
				setState(2741);
				identifier();
				setState(2744); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2742);
					match(T__1);
					setState(2743);
					identifier();
					}
					}
					setState(2746); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__1 );
				setState(2748);
				match(T__2);
				setState(2749);
				match(T__7);
				setState(2750);
				expression();
				}
				break;
			case 17:
				{
				_localctx = new ColumnReferenceContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2752);
				identifier();
				}
				break;
			case 18:
				{
				_localctx = new ParenthesizedExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2753);
				match(T__0);
				setState(2754);
				expression();
				setState(2755);
				match(T__2);
				}
				break;
			case 19:
				{
				_localctx = new ExtractContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2757);
				match(EXTRACT);
				setState(2758);
				match(T__0);
				setState(2759);
				((ExtractContext)_localctx).field = identifier();
				setState(2760);
				match(FROM);
				setState(2761);
				((ExtractContext)_localctx).source = valueExpression(0);
				setState(2762);
				match(T__2);
				}
				break;
			case 20:
				{
				_localctx = new SubstringContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2764);
				_la = _input.LA(1);
				if ( !(_la==SUBSTR || _la==SUBSTRING) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2765);
				match(T__0);
				setState(2766);
				((SubstringContext)_localctx).str = valueExpression(0);
				setState(2767);
				_la = _input.LA(1);
				if ( !(_la==T__1 || _la==FROM) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2768);
				((SubstringContext)_localctx).pos = valueExpression(0);
				setState(2771);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1 || _la==FOR) {
					{
					setState(2769);
					_la = _input.LA(1);
					if ( !(_la==T__1 || _la==FOR) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(2770);
					((SubstringContext)_localctx).len = valueExpression(0);
					}
				}

				setState(2773);
				match(T__2);
				}
				break;
			case 21:
				{
				_localctx = new TrimContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2775);
				match(TRIM);
				setState(2776);
				match(T__0);
				setState(2778);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,357,_ctx) ) {
				case 1:
					{
					setState(2777);
					((TrimContext)_localctx).trimOption = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==BOTH || _la==LEADING || _la==TRAILING) ) {
						((TrimContext)_localctx).trimOption = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				}
				setState(2781);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,358,_ctx) ) {
				case 1:
					{
					setState(2780);
					((TrimContext)_localctx).trimStr = valueExpression(0);
					}
					break;
				}
				setState(2783);
				match(FROM);
				setState(2784);
				((TrimContext)_localctx).srcStr = valueExpression(0);
				setState(2785);
				match(T__2);
				}
				break;
			case 22:
				{
				_localctx = new OverlayContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2787);
				match(OVERLAY);
				setState(2788);
				match(T__0);
				setState(2789);
				((OverlayContext)_localctx).input = valueExpression(0);
				setState(2790);
				match(PLACING);
				setState(2791);
				((OverlayContext)_localctx).replace = valueExpression(0);
				setState(2792);
				match(FROM);
				setState(2793);
				((OverlayContext)_localctx).position = valueExpression(0);
				setState(2796);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FOR) {
					{
					setState(2794);
					match(FOR);
					setState(2795);
					((OverlayContext)_localctx).length = valueExpression(0);
					}
				}

				setState(2798);
				match(T__2);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(2812);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,362,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(2810);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,361,_ctx) ) {
					case 1:
						{
						_localctx = new SubscriptContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((SubscriptContext)_localctx).value = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(2802);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(2803);
						match(T__8);
						setState(2804);
						((SubscriptContext)_localctx).index = valueExpression(0);
						setState(2805);
						match(T__9);
						}
						break;
					case 2:
						{
						_localctx = new DereferenceContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((DereferenceContext)_localctx).base = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(2807);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(2808);
						match(T__4);
						setState(2809);
						((DereferenceContext)_localctx).fieldName = identifier();
						}
						break;
					}
					} 
				}
				setState(2814);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,362,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class ConstantContext extends ParserRuleContext {
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
	 
		public ConstantContext() { }
		public void copyFrom(ConstantContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NullLiteralContext extends ConstantContext {
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public NullLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNullLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNullLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNullLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ConstantContext {
		public List<TerminalNode> STRING() { return getTokens(ArcticExtendSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticExtendSparkSqlParser.STRING, i);
		}
		public StringLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TypeConstructorContext extends ConstantContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TypeConstructorContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTypeConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTypeConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTypeConstructor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntervalLiteralContext extends ConstantContext {
		public IntervalContext interval() {
			return getRuleContext(IntervalContext.class,0);
		}
		public IntervalLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIntervalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIntervalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIntervalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NumericLiteralContext extends ConstantContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public NumericLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNumericLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BooleanLiteralContext extends ConstantContext {
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public BooleanLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_constant);
		try {
			int _alt;
			setState(2827);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,364,_ctx) ) {
			case 1:
				_localctx = new NullLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2815);
				match(NULL);
				}
				break;
			case 2:
				_localctx = new IntervalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2816);
				interval();
				}
				break;
			case 3:
				_localctx = new TypeConstructorContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2817);
				identifier();
				setState(2818);
				match(STRING);
				}
				break;
			case 4:
				_localctx = new NumericLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2820);
				number();
				}
				break;
			case 5:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(2821);
				booleanValue();
				}
				break;
			case 6:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(2823); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(2822);
						match(STRING);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(2825); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,363,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComparisonOperatorContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(ArcticExtendSparkSqlParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(ArcticExtendSparkSqlParser.NEQ, 0); }
		public TerminalNode NEQJ() { return getToken(ArcticExtendSparkSqlParser.NEQJ, 0); }
		public TerminalNode LT() { return getToken(ArcticExtendSparkSqlParser.LT, 0); }
		public TerminalNode LTE() { return getToken(ArcticExtendSparkSqlParser.LTE, 0); }
		public TerminalNode GT() { return getToken(ArcticExtendSparkSqlParser.GT, 0); }
		public TerminalNode GTE() { return getToken(ArcticExtendSparkSqlParser.GTE, 0); }
		public TerminalNode NSEQ() { return getToken(ArcticExtendSparkSqlParser.NSEQ, 0); }
		public ComparisonOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterComparisonOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitComparisonOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitComparisonOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOperatorContext comparisonOperator() throws RecognitionException {
		ComparisonOperatorContext _localctx = new ComparisonOperatorContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_comparisonOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2829);
			_la = _input.LA(1);
			if ( !(((((_la - 272)) & ~0x3f) == 0 && ((1L << (_la - 272)) & ((1L << (EQ - 272)) | (1L << (NSEQ - 272)) | (1L << (NEQ - 272)) | (1L << (NEQJ - 272)) | (1L << (LT - 272)) | (1L << (LTE - 272)) | (1L << (GT - 272)) | (1L << (GTE - 272)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArithmeticOperatorContext extends ParserRuleContext {
		public TerminalNode PLUS() { return getToken(ArcticExtendSparkSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticExtendSparkSqlParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticExtendSparkSqlParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticExtendSparkSqlParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticExtendSparkSqlParser.DIV, 0); }
		public TerminalNode TILDE() { return getToken(ArcticExtendSparkSqlParser.TILDE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticExtendSparkSqlParser.AMPERSAND, 0); }
		public TerminalNode PIPE() { return getToken(ArcticExtendSparkSqlParser.PIPE, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticExtendSparkSqlParser.CONCAT_PIPE, 0); }
		public TerminalNode HAT() { return getToken(ArcticExtendSparkSqlParser.HAT, 0); }
		public ArithmeticOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterArithmeticOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitArithmeticOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitArithmeticOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticOperatorContext arithmeticOperator() throws RecognitionException {
		ArithmeticOperatorContext _localctx = new ArithmeticOperatorContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_arithmeticOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2831);
			_la = _input.LA(1);
			if ( !(_la==DIV || ((((_la - 280)) & ~0x3f) == 0 && ((1L << (_la - 280)) & ((1L << (PLUS - 280)) | (1L << (MINUS - 280)) | (1L << (ASTERISK - 280)) | (1L << (SLASH - 280)) | (1L << (PERCENT - 280)) | (1L << (TILDE - 280)) | (1L << (AMPERSAND - 280)) | (1L << (PIPE - 280)) | (1L << (CONCAT_PIPE - 280)) | (1L << (HAT - 280)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PredicateOperatorContext extends ParserRuleContext {
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public PredicateOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicateOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPredicateOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPredicateOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPredicateOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateOperatorContext predicateOperator() throws RecognitionException {
		PredicateOperatorContext _localctx = new PredicateOperatorContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_predicateOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2833);
			_la = _input.LA(1);
			if ( !(_la==AND || ((((_la - 116)) & ~0x3f) == 0 && ((1L << (_la - 116)) & ((1L << (IN - 116)) | (1L << (NOT - 116)) | (1L << (OR - 116)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BooleanValueContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(ArcticExtendSparkSqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticExtendSparkSqlParser.FALSE, 0); }
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2835);
			_la = _input.LA(1);
			if ( !(_la==FALSE || _la==TRUE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IntervalContext extends ParserRuleContext {
		public TerminalNode INTERVAL() { return getToken(ArcticExtendSparkSqlParser.INTERVAL, 0); }
		public ErrorCapturingMultiUnitsIntervalContext errorCapturingMultiUnitsInterval() {
			return getRuleContext(ErrorCapturingMultiUnitsIntervalContext.class,0);
		}
		public ErrorCapturingUnitToUnitIntervalContext errorCapturingUnitToUnitInterval() {
			return getRuleContext(ErrorCapturingUnitToUnitIntervalContext.class,0);
		}
		public IntervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalContext interval() throws RecognitionException {
		IntervalContext _localctx = new IntervalContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_interval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2837);
			match(INTERVAL);
			setState(2840);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,365,_ctx) ) {
			case 1:
				{
				setState(2838);
				errorCapturingMultiUnitsInterval();
				}
				break;
			case 2:
				{
				setState(2839);
				errorCapturingUnitToUnitInterval();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ErrorCapturingMultiUnitsIntervalContext extends ParserRuleContext {
		public MultiUnitsIntervalContext body;
		public MultiUnitsIntervalContext multiUnitsInterval() {
			return getRuleContext(MultiUnitsIntervalContext.class,0);
		}
		public UnitToUnitIntervalContext unitToUnitInterval() {
			return getRuleContext(UnitToUnitIntervalContext.class,0);
		}
		public ErrorCapturingMultiUnitsIntervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_errorCapturingMultiUnitsInterval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterErrorCapturingMultiUnitsInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitErrorCapturingMultiUnitsInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitErrorCapturingMultiUnitsInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingMultiUnitsIntervalContext errorCapturingMultiUnitsInterval() throws RecognitionException {
		ErrorCapturingMultiUnitsIntervalContext _localctx = new ErrorCapturingMultiUnitsIntervalContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_errorCapturingMultiUnitsInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2842);
			((ErrorCapturingMultiUnitsIntervalContext)_localctx).body = multiUnitsInterval();
			setState(2844);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,366,_ctx) ) {
			case 1:
				{
				setState(2843);
				unitToUnitInterval();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiUnitsIntervalContext extends ParserRuleContext {
		public IdentifierContext identifier;
		public List<IdentifierContext> unit = new ArrayList<IdentifierContext>();
		public List<IntervalValueContext> intervalValue() {
			return getRuleContexts(IntervalValueContext.class);
		}
		public IntervalValueContext intervalValue(int i) {
			return getRuleContext(IntervalValueContext.class,i);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public MultiUnitsIntervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiUnitsInterval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterMultiUnitsInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitMultiUnitsInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitMultiUnitsInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiUnitsIntervalContext multiUnitsInterval() throws RecognitionException {
		MultiUnitsIntervalContext _localctx = new MultiUnitsIntervalContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_multiUnitsInterval);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2849); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(2846);
					intervalValue();
					setState(2847);
					((MultiUnitsIntervalContext)_localctx).identifier = identifier();
					((MultiUnitsIntervalContext)_localctx).unit.add(((MultiUnitsIntervalContext)_localctx).identifier);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(2851); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,367,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ErrorCapturingUnitToUnitIntervalContext extends ParserRuleContext {
		public UnitToUnitIntervalContext body;
		public MultiUnitsIntervalContext error1;
		public UnitToUnitIntervalContext error2;
		public List<UnitToUnitIntervalContext> unitToUnitInterval() {
			return getRuleContexts(UnitToUnitIntervalContext.class);
		}
		public UnitToUnitIntervalContext unitToUnitInterval(int i) {
			return getRuleContext(UnitToUnitIntervalContext.class,i);
		}
		public MultiUnitsIntervalContext multiUnitsInterval() {
			return getRuleContext(MultiUnitsIntervalContext.class,0);
		}
		public ErrorCapturingUnitToUnitIntervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_errorCapturingUnitToUnitInterval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterErrorCapturingUnitToUnitInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitErrorCapturingUnitToUnitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitErrorCapturingUnitToUnitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingUnitToUnitIntervalContext errorCapturingUnitToUnitInterval() throws RecognitionException {
		ErrorCapturingUnitToUnitIntervalContext _localctx = new ErrorCapturingUnitToUnitIntervalContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_errorCapturingUnitToUnitInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2853);
			((ErrorCapturingUnitToUnitIntervalContext)_localctx).body = unitToUnitInterval();
			setState(2856);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,368,_ctx) ) {
			case 1:
				{
				setState(2854);
				((ErrorCapturingUnitToUnitIntervalContext)_localctx).error1 = multiUnitsInterval();
				}
				break;
			case 2:
				{
				setState(2855);
				((ErrorCapturingUnitToUnitIntervalContext)_localctx).error2 = unitToUnitInterval();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnitToUnitIntervalContext extends ParserRuleContext {
		public IntervalValueContext value;
		public IdentifierContext from;
		public IdentifierContext to;
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public IntervalValueContext intervalValue() {
			return getRuleContext(IntervalValueContext.class,0);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public UnitToUnitIntervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unitToUnitInterval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUnitToUnitInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUnitToUnitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUnitToUnitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitToUnitIntervalContext unitToUnitInterval() throws RecognitionException {
		UnitToUnitIntervalContext _localctx = new UnitToUnitIntervalContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_unitToUnitInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2858);
			((UnitToUnitIntervalContext)_localctx).value = intervalValue();
			setState(2859);
			((UnitToUnitIntervalContext)_localctx).from = identifier();
			setState(2860);
			match(TO);
			setState(2861);
			((UnitToUnitIntervalContext)_localctx).to = identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IntervalValueContext extends ParserRuleContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticExtendSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode STRING() { return getToken(ArcticExtendSparkSqlParser.STRING, 0); }
		public TerminalNode PLUS() { return getToken(ArcticExtendSparkSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public IntervalValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intervalValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIntervalValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIntervalValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIntervalValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalValueContext intervalValue() throws RecognitionException {
		IntervalValueContext _localctx = new IntervalValueContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_intervalValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2864);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PLUS || _la==MINUS) {
				{
				setState(2863);
				_la = _input.LA(1);
				if ( !(_la==PLUS || _la==MINUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(2866);
			_la = _input.LA(1);
			if ( !(((((_la - 290)) & ~0x3f) == 0 && ((1L << (_la - 290)) & ((1L << (STRING - 290)) | (1L << (INTEGER_VALUE - 290)) | (1L << (DECIMAL_VALUE - 290)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ColPositionContext extends ParserRuleContext {
		public Token position;
		public ErrorCapturingIdentifierContext afterCol;
		public TerminalNode FIRST() { return getToken(ArcticExtendSparkSqlParser.FIRST, 0); }
		public TerminalNode AFTER() { return getToken(ArcticExtendSparkSqlParser.AFTER, 0); }
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public ColPositionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colPosition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterColPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitColPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitColPosition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColPositionContext colPosition() throws RecognitionException {
		ColPositionContext _localctx = new ColPositionContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_colPosition);
		try {
			setState(2871);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FIRST:
				enterOuterAlt(_localctx, 1);
				{
				setState(2868);
				((ColPositionContext)_localctx).position = match(FIRST);
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2869);
				((ColPositionContext)_localctx).position = match(AFTER);
				setState(2870);
				((ColPositionContext)_localctx).afterCol = errorCapturingIdentifier();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataTypeContext extends ParserRuleContext {
		public DataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataType; }
	 
		public DataTypeContext() { }
		public void copyFrom(DataTypeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ComplexDataTypeContext extends DataTypeContext {
		public Token complex;
		public TerminalNode LT() { return getToken(ArcticExtendSparkSqlParser.LT, 0); }
		public List<DataTypeContext> dataType() {
			return getRuleContexts(DataTypeContext.class);
		}
		public DataTypeContext dataType(int i) {
			return getRuleContext(DataTypeContext.class,i);
		}
		public TerminalNode GT() { return getToken(ArcticExtendSparkSqlParser.GT, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticExtendSparkSqlParser.ARRAY, 0); }
		public TerminalNode MAP() { return getToken(ArcticExtendSparkSqlParser.MAP, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticExtendSparkSqlParser.STRUCT, 0); }
		public TerminalNode NEQ() { return getToken(ArcticExtendSparkSqlParser.NEQ, 0); }
		public ComplexColTypeListContext complexColTypeList() {
			return getRuleContext(ComplexColTypeListContext.class,0);
		}
		public ComplexDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterComplexDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitComplexDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitComplexDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class YearMonthIntervalDataTypeContext extends DataTypeContext {
		public Token from;
		public Token to;
		public TerminalNode INTERVAL() { return getToken(ArcticExtendSparkSqlParser.INTERVAL, 0); }
		public TerminalNode YEAR() { return getToken(ArcticExtendSparkSqlParser.YEAR, 0); }
		public List<TerminalNode> MONTH() { return getTokens(ArcticExtendSparkSqlParser.MONTH); }
		public TerminalNode MONTH(int i) {
			return getToken(ArcticExtendSparkSqlParser.MONTH, i);
		}
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public YearMonthIntervalDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterYearMonthIntervalDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitYearMonthIntervalDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitYearMonthIntervalDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DayTimeIntervalDataTypeContext extends DataTypeContext {
		public Token from;
		public Token to;
		public TerminalNode INTERVAL() { return getToken(ArcticExtendSparkSqlParser.INTERVAL, 0); }
		public TerminalNode DAY() { return getToken(ArcticExtendSparkSqlParser.DAY, 0); }
		public List<TerminalNode> HOUR() { return getTokens(ArcticExtendSparkSqlParser.HOUR); }
		public TerminalNode HOUR(int i) {
			return getToken(ArcticExtendSparkSqlParser.HOUR, i);
		}
		public List<TerminalNode> MINUTE() { return getTokens(ArcticExtendSparkSqlParser.MINUTE); }
		public TerminalNode MINUTE(int i) {
			return getToken(ArcticExtendSparkSqlParser.MINUTE, i);
		}
		public List<TerminalNode> SECOND() { return getTokens(ArcticExtendSparkSqlParser.SECOND); }
		public TerminalNode SECOND(int i) {
			return getToken(ArcticExtendSparkSqlParser.SECOND, i);
		}
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public DayTimeIntervalDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDayTimeIntervalDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDayTimeIntervalDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDayTimeIntervalDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrimitiveDataTypeContext extends DataTypeContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticExtendSparkSqlParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, i);
		}
		public PrimitiveDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterPrimitiveDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitPrimitiveDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitPrimitiveDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_dataType);
		int _la;
		try {
			setState(2919);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,377,_ctx) ) {
			case 1:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2873);
				((ComplexDataTypeContext)_localctx).complex = match(ARRAY);
				setState(2874);
				match(LT);
				setState(2875);
				dataType();
				setState(2876);
				match(GT);
				}
				break;
			case 2:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2878);
				((ComplexDataTypeContext)_localctx).complex = match(MAP);
				setState(2879);
				match(LT);
				setState(2880);
				dataType();
				setState(2881);
				match(T__1);
				setState(2882);
				dataType();
				setState(2883);
				match(GT);
				}
				break;
			case 3:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2885);
				((ComplexDataTypeContext)_localctx).complex = match(STRUCT);
				setState(2892);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LT:
					{
					setState(2886);
					match(LT);
					setState(2888);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,371,_ctx) ) {
					case 1:
						{
						setState(2887);
						complexColTypeList();
						}
						break;
					}
					setState(2890);
					match(GT);
					}
					break;
				case NEQ:
					{
					setState(2891);
					match(NEQ);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 4:
				_localctx = new YearMonthIntervalDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2894);
				match(INTERVAL);
				setState(2895);
				((YearMonthIntervalDataTypeContext)_localctx).from = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==MONTH || _la==YEAR) ) {
					((YearMonthIntervalDataTypeContext)_localctx).from = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2898);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,373,_ctx) ) {
				case 1:
					{
					setState(2896);
					match(TO);
					setState(2897);
					((YearMonthIntervalDataTypeContext)_localctx).to = match(MONTH);
					}
					break;
				}
				}
				break;
			case 5:
				_localctx = new DayTimeIntervalDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(2900);
				match(INTERVAL);
				setState(2901);
				((DayTimeIntervalDataTypeContext)_localctx).from = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==DAY || _la==HOUR || _la==MINUTE || _la==SECOND) ) {
					((DayTimeIntervalDataTypeContext)_localctx).from = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2904);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,374,_ctx) ) {
				case 1:
					{
					setState(2902);
					match(TO);
					setState(2903);
					((DayTimeIntervalDataTypeContext)_localctx).to = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==HOUR || _la==MINUTE || _la==SECOND) ) {
						((DayTimeIntervalDataTypeContext)_localctx).to = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				}
				}
				break;
			case 6:
				_localctx = new PrimitiveDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(2906);
				identifier();
				setState(2917);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,376,_ctx) ) {
				case 1:
					{
					setState(2907);
					match(T__0);
					setState(2908);
					match(INTEGER_VALUE);
					setState(2913);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(2909);
						match(T__1);
						setState(2910);
						match(INTEGER_VALUE);
						}
						}
						setState(2915);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(2916);
					match(T__2);
					}
					break;
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedColTypeWithPositionListContext extends ParserRuleContext {
		public List<QualifiedColTypeWithPositionContext> qualifiedColTypeWithPosition() {
			return getRuleContexts(QualifiedColTypeWithPositionContext.class);
		}
		public QualifiedColTypeWithPositionContext qualifiedColTypeWithPosition(int i) {
			return getRuleContext(QualifiedColTypeWithPositionContext.class,i);
		}
		public QualifiedColTypeWithPositionListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedColTypeWithPositionList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQualifiedColTypeWithPositionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQualifiedColTypeWithPositionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQualifiedColTypeWithPositionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedColTypeWithPositionListContext qualifiedColTypeWithPositionList() throws RecognitionException {
		QualifiedColTypeWithPositionListContext _localctx = new QualifiedColTypeWithPositionListContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_qualifiedColTypeWithPositionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2921);
			qualifiedColTypeWithPosition();
			setState(2926);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2922);
				match(T__1);
				setState(2923);
				qualifiedColTypeWithPosition();
				}
				}
				setState(2928);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedColTypeWithPositionContext extends ParserRuleContext {
		public MultipartIdentifierContext name;
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ColPositionContext colPosition() {
			return getRuleContext(ColPositionContext.class,0);
		}
		public QualifiedColTypeWithPositionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedColTypeWithPosition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQualifiedColTypeWithPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQualifiedColTypeWithPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQualifiedColTypeWithPosition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedColTypeWithPositionContext qualifiedColTypeWithPosition() throws RecognitionException {
		QualifiedColTypeWithPositionContext _localctx = new QualifiedColTypeWithPositionContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_qualifiedColTypeWithPosition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2929);
			((QualifiedColTypeWithPositionContext)_localctx).name = multipartIdentifier();
			setState(2930);
			dataType();
			setState(2933);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(2931);
				match(NOT);
				setState(2932);
				match(NULL);
				}
			}

			setState(2936);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(2935);
				commentSpec();
				}
			}

			setState(2939);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AFTER || _la==FIRST) {
				{
				setState(2938);
				colPosition();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ColTypeListContext extends ParserRuleContext {
		public List<ColTypeContext> colType() {
			return getRuleContexts(ColTypeContext.class);
		}
		public ColTypeContext colType(int i) {
			return getRuleContext(ColTypeContext.class,i);
		}
		public ColTypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colTypeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeListContext colTypeList() throws RecognitionException {
		ColTypeListContext _localctx = new ColTypeListContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_colTypeList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2941);
			colType();
			setState(2946);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,382,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2942);
					match(T__1);
					setState(2943);
					colType();
					}
					} 
				}
				setState(2948);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,382,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ColTypeContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext colName;
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeContext colType() throws RecognitionException {
		ColTypeContext _localctx = new ColTypeContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_colType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2949);
			((ColTypeContext)_localctx).colName = errorCapturingIdentifier();
			setState(2950);
			dataType();
			setState(2953);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,383,_ctx) ) {
			case 1:
				{
				setState(2951);
				match(NOT);
				setState(2952);
				match(NULL);
				}
				break;
			}
			setState(2956);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,384,_ctx) ) {
			case 1:
				{
				setState(2955);
				commentSpec();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComplexColTypeListContext extends ParserRuleContext {
		public List<ComplexColTypeContext> complexColType() {
			return getRuleContexts(ComplexColTypeContext.class);
		}
		public ComplexColTypeContext complexColType(int i) {
			return getRuleContext(ComplexColTypeContext.class,i);
		}
		public ComplexColTypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexColTypeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterComplexColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitComplexColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitComplexColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexColTypeListContext complexColTypeList() throws RecognitionException {
		ComplexColTypeListContext _localctx = new ComplexColTypeListContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_complexColTypeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2958);
			complexColType();
			setState(2963);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(2959);
				match(T__1);
				setState(2960);
				complexColType();
				}
				}
				setState(2965);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComplexColTypeContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ComplexColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexColType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterComplexColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitComplexColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitComplexColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexColTypeContext complexColType() throws RecognitionException {
		ComplexColTypeContext _localctx = new ComplexColTypeContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_complexColType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2966);
			identifier();
			setState(2968);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,386,_ctx) ) {
			case 1:
				{
				setState(2967);
				match(T__10);
				}
				break;
			}
			setState(2970);
			dataType();
			setState(2973);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(2971);
				match(NOT);
				setState(2972);
				match(NULL);
				}
			}

			setState(2976);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(2975);
				commentSpec();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WhenClauseContext extends ParserRuleContext {
		public ExpressionContext condition;
		public ExpressionContext result;
		public TerminalNode WHEN() { return getToken(ArcticExtendSparkSqlParser.WHEN, 0); }
		public TerminalNode THEN() { return getToken(ArcticExtendSparkSqlParser.THEN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public WhenClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whenClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterWhenClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitWhenClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitWhenClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhenClauseContext whenClause() throws RecognitionException {
		WhenClauseContext _localctx = new WhenClauseContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_whenClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2978);
			match(WHEN);
			setState(2979);
			((WhenClauseContext)_localctx).condition = expression();
			setState(2980);
			match(THEN);
			setState(2981);
			((WhenClauseContext)_localctx).result = expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WindowClauseContext extends ParserRuleContext {
		public TerminalNode WINDOW() { return getToken(ArcticExtendSparkSqlParser.WINDOW, 0); }
		public List<NamedWindowContext> namedWindow() {
			return getRuleContexts(NamedWindowContext.class);
		}
		public NamedWindowContext namedWindow(int i) {
			return getRuleContext(NamedWindowContext.class,i);
		}
		public WindowClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterWindowClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitWindowClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitWindowClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowClauseContext windowClause() throws RecognitionException {
		WindowClauseContext _localctx = new WindowClauseContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_windowClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2983);
			match(WINDOW);
			setState(2984);
			namedWindow();
			setState(2989);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,389,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2985);
					match(T__1);
					setState(2986);
					namedWindow();
					}
					} 
				}
				setState(2991);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,389,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedWindowContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext name;
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public WindowSpecContext windowSpec() {
			return getRuleContext(WindowSpecContext.class,0);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public NamedWindowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedWindow; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNamedWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNamedWindow(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNamedWindow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedWindowContext namedWindow() throws RecognitionException {
		NamedWindowContext _localctx = new NamedWindowContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_namedWindow);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2992);
			((NamedWindowContext)_localctx).name = errorCapturingIdentifier();
			setState(2993);
			match(AS);
			setState(2994);
			windowSpec();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WindowSpecContext extends ParserRuleContext {
		public WindowSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowSpec; }
	 
		public WindowSpecContext() { }
		public void copyFrom(WindowSpecContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class WindowRefContext extends WindowSpecContext {
		public ErrorCapturingIdentifierContext name;
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public WindowRefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterWindowRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitWindowRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitWindowRef(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WindowDefContext extends WindowSpecContext {
		public ExpressionContext expression;
		public List<ExpressionContext> partition = new ArrayList<ExpressionContext>();
		public TerminalNode CLUSTER() { return getToken(ArcticExtendSparkSqlParser.CLUSTER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticExtendSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticExtendSparkSqlParser.BY, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public WindowFrameContext windowFrame() {
			return getRuleContext(WindowFrameContext.class,0);
		}
		public List<SortItemContext> sortItem() {
			return getRuleContexts(SortItemContext.class);
		}
		public SortItemContext sortItem(int i) {
			return getRuleContext(SortItemContext.class,i);
		}
		public TerminalNode PARTITION() { return getToken(ArcticExtendSparkSqlParser.PARTITION, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticExtendSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode ORDER() { return getToken(ArcticExtendSparkSqlParser.ORDER, 0); }
		public TerminalNode SORT() { return getToken(ArcticExtendSparkSqlParser.SORT, 0); }
		public WindowDefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterWindowDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitWindowDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitWindowDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowSpecContext windowSpec() throws RecognitionException {
		WindowSpecContext _localctx = new WindowSpecContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_windowSpec);
		int _la;
		try {
			setState(3042);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,397,_ctx) ) {
			case 1:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2996);
				((WindowRefContext)_localctx).name = errorCapturingIdentifier();
				}
				break;
			case 2:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2997);
				match(T__0);
				setState(2998);
				((WindowRefContext)_localctx).name = errorCapturingIdentifier();
				setState(2999);
				match(T__2);
				}
				break;
			case 3:
				_localctx = new WindowDefContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(3001);
				match(T__0);
				setState(3036);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case CLUSTER:
					{
					setState(3002);
					match(CLUSTER);
					setState(3003);
					match(BY);
					setState(3004);
					((WindowDefContext)_localctx).expression = expression();
					((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
					setState(3009);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(3005);
						match(T__1);
						setState(3006);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						}
						}
						setState(3011);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				case T__2:
				case DISTRIBUTE:
				case ORDER:
				case PARTITION:
				case RANGE:
				case ROWS:
				case SORT:
					{
					setState(3022);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==DISTRIBUTE || _la==PARTITION) {
						{
						setState(3012);
						_la = _input.LA(1);
						if ( !(_la==DISTRIBUTE || _la==PARTITION) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(3013);
						match(BY);
						setState(3014);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						setState(3019);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__1) {
							{
							{
							setState(3015);
							match(T__1);
							setState(3016);
							((WindowDefContext)_localctx).expression = expression();
							((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
							}
							}
							setState(3021);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(3034);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ORDER || _la==SORT) {
						{
						setState(3024);
						_la = _input.LA(1);
						if ( !(_la==ORDER || _la==SORT) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(3025);
						match(BY);
						setState(3026);
						sortItem();
						setState(3031);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__1) {
							{
							{
							setState(3027);
							match(T__1);
							setState(3028);
							sortItem();
							}
							}
							setState(3033);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(3039);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==RANGE || _la==ROWS) {
					{
					setState(3038);
					windowFrame();
					}
				}

				setState(3041);
				match(T__2);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WindowFrameContext extends ParserRuleContext {
		public Token frameType;
		public FrameBoundContext start;
		public FrameBoundContext end;
		public TerminalNode RANGE() { return getToken(ArcticExtendSparkSqlParser.RANGE, 0); }
		public List<FrameBoundContext> frameBound() {
			return getRuleContexts(FrameBoundContext.class);
		}
		public FrameBoundContext frameBound(int i) {
			return getRuleContext(FrameBoundContext.class,i);
		}
		public TerminalNode ROWS() { return getToken(ArcticExtendSparkSqlParser.ROWS, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticExtendSparkSqlParser.BETWEEN, 0); }
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public WindowFrameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowFrame; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterWindowFrame(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitWindowFrame(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitWindowFrame(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowFrameContext windowFrame() throws RecognitionException {
		WindowFrameContext _localctx = new WindowFrameContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_windowFrame);
		try {
			setState(3060);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,398,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3044);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(3045);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3046);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(3047);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(3048);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(3049);
				match(BETWEEN);
				setState(3050);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(3051);
				match(AND);
				setState(3052);
				((WindowFrameContext)_localctx).end = frameBound();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(3054);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(3055);
				match(BETWEEN);
				setState(3056);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(3057);
				match(AND);
				setState(3058);
				((WindowFrameContext)_localctx).end = frameBound();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FrameBoundContext extends ParserRuleContext {
		public Token boundType;
		public TerminalNode UNBOUNDED() { return getToken(ArcticExtendSparkSqlParser.UNBOUNDED, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticExtendSparkSqlParser.PRECEDING, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticExtendSparkSqlParser.FOLLOWING, 0); }
		public TerminalNode ROW() { return getToken(ArcticExtendSparkSqlParser.ROW, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticExtendSparkSqlParser.CURRENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FrameBoundContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_frameBound; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFrameBound(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFrameBound(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFrameBound(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FrameBoundContext frameBound() throws RecognitionException {
		FrameBoundContext _localctx = new FrameBoundContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_frameBound);
		int _la;
		try {
			setState(3069);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,399,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3062);
				match(UNBOUNDED);
				setState(3063);
				((FrameBoundContext)_localctx).boundType = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==FOLLOWING || _la==PRECEDING) ) {
					((FrameBoundContext)_localctx).boundType = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3064);
				((FrameBoundContext)_localctx).boundType = match(CURRENT);
				setState(3065);
				match(ROW);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(3066);
				expression();
				setState(3067);
				((FrameBoundContext)_localctx).boundType = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==FOLLOWING || _la==PRECEDING) ) {
					((FrameBoundContext)_localctx).boundType = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedNameListContext extends ParserRuleContext {
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public QualifiedNameListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedNameList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQualifiedNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQualifiedNameList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQualifiedNameList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameListContext qualifiedNameList() throws RecognitionException {
		QualifiedNameListContext _localctx = new QualifiedNameListContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_qualifiedNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3071);
			qualifiedName();
			setState(3076);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(3072);
				match(T__1);
				setState(3073);
				qualifiedName();
				}
				}
				setState(3078);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode FILTER() { return getToken(ArcticExtendSparkSqlParser.FILTER, 0); }
		public TerminalNode LEFT() { return getToken(ArcticExtendSparkSqlParser.LEFT, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticExtendSparkSqlParser.RIGHT, 0); }
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFunctionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFunctionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_functionName);
		try {
			setState(3083);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,401,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3079);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3080);
				match(FILTER);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(3081);
				match(LEFT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(3082);
				match(RIGHT);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedNameContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(3085);
			identifier();
			setState(3090);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,402,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(3086);
					match(T__4);
					setState(3087);
					identifier();
					}
					} 
				}
				setState(3092);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,402,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ErrorCapturingIdentifierContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ErrorCapturingIdentifierExtraContext errorCapturingIdentifierExtra() {
			return getRuleContext(ErrorCapturingIdentifierExtraContext.class,0);
		}
		public ErrorCapturingIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_errorCapturingIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterErrorCapturingIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitErrorCapturingIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitErrorCapturingIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingIdentifierContext errorCapturingIdentifier() throws RecognitionException {
		ErrorCapturingIdentifierContext _localctx = new ErrorCapturingIdentifierContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_errorCapturingIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3093);
			identifier();
			setState(3094);
			errorCapturingIdentifierExtra();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ErrorCapturingIdentifierExtraContext extends ParserRuleContext {
		public ErrorCapturingIdentifierExtraContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_errorCapturingIdentifierExtra; }
	 
		public ErrorCapturingIdentifierExtraContext() { }
		public void copyFrom(ErrorCapturingIdentifierExtraContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ErrorIdentContext extends ErrorCapturingIdentifierExtraContext {
		public List<TerminalNode> MINUS() { return getTokens(ArcticExtendSparkSqlParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(ArcticExtendSparkSqlParser.MINUS, i);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public ErrorIdentContext(ErrorCapturingIdentifierExtraContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterErrorIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitErrorIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitErrorIdent(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RealIdentContext extends ErrorCapturingIdentifierExtraContext {
		public RealIdentContext(ErrorCapturingIdentifierExtraContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterRealIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitRealIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitRealIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingIdentifierExtraContext errorCapturingIdentifierExtra() throws RecognitionException {
		ErrorCapturingIdentifierExtraContext _localctx = new ErrorCapturingIdentifierExtraContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_errorCapturingIdentifierExtra);
		try {
			int _alt;
			setState(3103);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,404,_ctx) ) {
			case 1:
				_localctx = new ErrorIdentContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(3098); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(3096);
						match(MINUS);
						setState(3097);
						identifier();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(3100); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,403,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case 2:
				_localctx = new RealIdentContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierContext extends ParserRuleContext {
		public StrictIdentifierContext strictIdentifier() {
			return getRuleContext(StrictIdentifierContext.class,0);
		}
		public StrictNonReservedContext strictNonReserved() {
			return getRuleContext(StrictNonReservedContext.class,0);
		}
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_identifier);
		try {
			setState(3108);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,405,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3105);
				strictIdentifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3106);
				if (!(!SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "!SQL_standard_keyword_behavior");
				setState(3107);
				strictNonReserved();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StrictIdentifierContext extends ParserRuleContext {
		public StrictIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_strictIdentifier; }
	 
		public StrictIdentifierContext() { }
		public void copyFrom(StrictIdentifierContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class QuotedIdentifierAlternativeContext extends StrictIdentifierContext {
		public QuotedIdentifierContext quotedIdentifier() {
			return getRuleContext(QuotedIdentifierContext.class,0);
		}
		public QuotedIdentifierAlternativeContext(StrictIdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQuotedIdentifierAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQuotedIdentifierAlternative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQuotedIdentifierAlternative(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnquotedIdentifierContext extends StrictIdentifierContext {
		public TerminalNode IDENTIFIER() { return getToken(ArcticExtendSparkSqlParser.IDENTIFIER, 0); }
		public AnsiNonReservedContext ansiNonReserved() {
			return getRuleContext(AnsiNonReservedContext.class,0);
		}
		public NonReservedContext nonReserved() {
			return getRuleContext(NonReservedContext.class,0);
		}
		public UnquotedIdentifierContext(StrictIdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterUnquotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitUnquotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitUnquotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictIdentifierContext strictIdentifier() throws RecognitionException {
		StrictIdentifierContext _localctx = new StrictIdentifierContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_strictIdentifier);
		try {
			setState(3116);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,406,_ctx) ) {
			case 1:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(3110);
				match(IDENTIFIER);
				}
				break;
			case 2:
				_localctx = new QuotedIdentifierAlternativeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(3111);
				quotedIdentifier();
				}
				break;
			case 3:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(3112);
				if (!(SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "SQL_standard_keyword_behavior");
				setState(3113);
				ansiNonReserved();
				}
				break;
			case 4:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(3114);
				if (!(!SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "!SQL_standard_keyword_behavior");
				setState(3115);
				nonReserved();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QuotedIdentifierContext extends ParserRuleContext {
		public TerminalNode BACKQUOTED_IDENTIFIER() { return getToken(ArcticExtendSparkSqlParser.BACKQUOTED_IDENTIFIER, 0); }
		public QuotedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quotedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterQuotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitQuotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitQuotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuotedIdentifierContext quotedIdentifier() throws RecognitionException {
		QuotedIdentifierContext _localctx = new QuotedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 284, RULE_quotedIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3118);
			match(BACKQUOTED_IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumberContext extends ParserRuleContext {
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
	 
		public NumberContext() { }
		public void copyFrom(NumberContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DecimalLiteralContext extends NumberContext {
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticExtendSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public DecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigIntLiteralContext extends NumberContext {
		public TerminalNode BIGINT_LITERAL() { return getToken(ArcticExtendSparkSqlParser.BIGINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public BigIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterBigIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitBigIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitBigIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TinyIntLiteralContext extends NumberContext {
		public TerminalNode TINYINT_LITERAL() { return getToken(ArcticExtendSparkSqlParser.TINYINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public TinyIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterTinyIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitTinyIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitTinyIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LegacyDecimalLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(ArcticExtendSparkSqlParser.EXPONENT_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticExtendSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public LegacyDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterLegacyDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitLegacyDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitLegacyDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigDecimalLiteralContext extends NumberContext {
		public TerminalNode BIGDECIMAL_LITERAL() { return getToken(ArcticExtendSparkSqlParser.BIGDECIMAL_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public BigDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterBigDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitBigDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitBigDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExponentLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(ArcticExtendSparkSqlParser.EXPONENT_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public ExponentLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterExponentLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitExponentLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitExponentLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DoubleLiteralContext extends NumberContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(ArcticExtendSparkSqlParser.DOUBLE_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public DoubleLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitDoubleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitDoubleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntegerLiteralContext extends NumberContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticExtendSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public IntegerLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitIntegerLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FloatLiteralContext extends NumberContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(ArcticExtendSparkSqlParser.FLOAT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public FloatLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterFloatLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitFloatLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitFloatLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SmallIntLiteralContext extends NumberContext {
		public TerminalNode SMALLINT_LITERAL() { return getToken(ArcticExtendSparkSqlParser.SMALLINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticExtendSparkSqlParser.MINUS, 0); }
		public SmallIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterSmallIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitSmallIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitSmallIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 286, RULE_number);
		int _la;
		try {
			setState(3163);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,417,_ctx) ) {
			case 1:
				_localctx = new ExponentLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(3120);
				if (!(!legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "!legacy_exponent_literal_as_decimal_enabled");
				setState(3122);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3121);
					match(MINUS);
					}
				}

				setState(3124);
				match(EXPONENT_VALUE);
				}
				break;
			case 2:
				_localctx = new DecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(3125);
				if (!(!legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "!legacy_exponent_literal_as_decimal_enabled");
				setState(3127);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3126);
					match(MINUS);
					}
				}

				setState(3129);
				match(DECIMAL_VALUE);
				}
				break;
			case 3:
				_localctx = new LegacyDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(3130);
				if (!(legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "legacy_exponent_literal_as_decimal_enabled");
				setState(3132);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3131);
					match(MINUS);
					}
				}

				setState(3134);
				_la = _input.LA(1);
				if ( !(_la==EXPONENT_VALUE || _la==DECIMAL_VALUE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 4:
				_localctx = new IntegerLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(3136);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3135);
					match(MINUS);
					}
				}

				setState(3138);
				match(INTEGER_VALUE);
				}
				break;
			case 5:
				_localctx = new BigIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(3140);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3139);
					match(MINUS);
					}
				}

				setState(3142);
				match(BIGINT_LITERAL);
				}
				break;
			case 6:
				_localctx = new SmallIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(3144);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3143);
					match(MINUS);
					}
				}

				setState(3146);
				match(SMALLINT_LITERAL);
				}
				break;
			case 7:
				_localctx = new TinyIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(3148);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3147);
					match(MINUS);
					}
				}

				setState(3150);
				match(TINYINT_LITERAL);
				}
				break;
			case 8:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(3152);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3151);
					match(MINUS);
					}
				}

				setState(3154);
				match(DOUBLE_LITERAL);
				}
				break;
			case 9:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(3156);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3155);
					match(MINUS);
					}
				}

				setState(3158);
				match(FLOAT_LITERAL);
				}
				break;
			case 10:
				_localctx = new BigDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(3160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3159);
					match(MINUS);
					}
				}

				setState(3162);
				match(BIGDECIMAL_LITERAL);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AlterColumnActionContext extends ParserRuleContext {
		public Token setOrDrop;
		public TerminalNode TYPE() { return getToken(ArcticExtendSparkSqlParser.TYPE, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ColPositionContext colPosition() {
			return getRuleContext(ColPositionContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public AlterColumnActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterColumnAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAlterColumnAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAlterColumnAction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAlterColumnAction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AlterColumnActionContext alterColumnAction() throws RecognitionException {
		AlterColumnActionContext _localctx = new AlterColumnActionContext(_ctx, getState());
		enterRule(_localctx, 288, RULE_alterColumnAction);
		int _la;
		try {
			setState(3172);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TYPE:
				enterOuterAlt(_localctx, 1);
				{
				setState(3165);
				match(TYPE);
				setState(3166);
				dataType();
				}
				break;
			case COMMENT:
				enterOuterAlt(_localctx, 2);
				{
				setState(3167);
				commentSpec();
				}
				break;
			case AFTER:
			case FIRST:
				enterOuterAlt(_localctx, 3);
				{
				setState(3168);
				colPosition();
				}
				break;
			case DROP:
			case SET:
				enterOuterAlt(_localctx, 4);
				{
				setState(3169);
				((AlterColumnActionContext)_localctx).setOrDrop = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==DROP || _la==SET) ) {
					((AlterColumnActionContext)_localctx).setOrDrop = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(3170);
				match(NOT);
				setState(3171);
				match(NULL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnsiNonReservedContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(ArcticExtendSparkSqlParser.ADD, 0); }
		public TerminalNode AFTER() { return getToken(ArcticExtendSparkSqlParser.AFTER, 0); }
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticExtendSparkSqlParser.ANALYZE, 0); }
		public TerminalNode ANTI() { return getToken(ArcticExtendSparkSqlParser.ANTI, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticExtendSparkSqlParser.ARCHIVE, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticExtendSparkSqlParser.ARRAY, 0); }
		public TerminalNode ASC() { return getToken(ArcticExtendSparkSqlParser.ASC, 0); }
		public TerminalNode AT() { return getToken(ArcticExtendSparkSqlParser.AT, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticExtendSparkSqlParser.BETWEEN, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticExtendSparkSqlParser.BUCKET, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticExtendSparkSqlParser.BUCKETS, 0); }
		public TerminalNode BY() { return getToken(ArcticExtendSparkSqlParser.BY, 0); }
		public TerminalNode CACHE() { return getToken(ArcticExtendSparkSqlParser.CACHE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticExtendSparkSqlParser.CASCADE, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticExtendSparkSqlParser.CHANGE, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticExtendSparkSqlParser.CLEAR, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticExtendSparkSqlParser.CLUSTER, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticExtendSparkSqlParser.CLUSTERED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticExtendSparkSqlParser.CODEGEN, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticExtendSparkSqlParser.COLLECTION, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticExtendSparkSqlParser.COMMENT, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticExtendSparkSqlParser.COMMIT, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticExtendSparkSqlParser.COMPACT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticExtendSparkSqlParser.COMPACTIONS, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticExtendSparkSqlParser.COMPUTE, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticExtendSparkSqlParser.CONCATENATE, 0); }
		public TerminalNode COST() { return getToken(ArcticExtendSparkSqlParser.COST, 0); }
		public TerminalNode CUBE() { return getToken(ArcticExtendSparkSqlParser.CUBE, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticExtendSparkSqlParser.CURRENT, 0); }
		public TerminalNode DATA() { return getToken(ArcticExtendSparkSqlParser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticExtendSparkSqlParser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticExtendSparkSqlParser.DATABASES, 0); }
		public TerminalNode DAY() { return getToken(ArcticExtendSparkSqlParser.DAY, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.DBPROPERTIES, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticExtendSparkSqlParser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(ArcticExtendSparkSqlParser.DELETE, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticExtendSparkSqlParser.DELIMITED, 0); }
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticExtendSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode DFS() { return getToken(ArcticExtendSparkSqlParser.DFS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticExtendSparkSqlParser.DIRECTORIES, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticExtendSparkSqlParser.DIRECTORY, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticExtendSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode DIV() { return getToken(ArcticExtendSparkSqlParser.DIV, 0); }
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticExtendSparkSqlParser.ESCAPED, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticExtendSparkSqlParser.EXCHANGE, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticExtendSparkSqlParser.EXPLAIN, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticExtendSparkSqlParser.EXPORT, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticExtendSparkSqlParser.EXTERNAL, 0); }
		public TerminalNode EXTRACT() { return getToken(ArcticExtendSparkSqlParser.EXTRACT, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticExtendSparkSqlParser.FIELDS, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticExtendSparkSqlParser.FILEFORMAT, 0); }
		public TerminalNode FIRST() { return getToken(ArcticExtendSparkSqlParser.FIRST, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticExtendSparkSqlParser.FOLLOWING, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticExtendSparkSqlParser.FORMAT, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticExtendSparkSqlParser.FORMATTED, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticExtendSparkSqlParser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticExtendSparkSqlParser.FUNCTIONS, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticExtendSparkSqlParser.GLOBAL, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticExtendSparkSqlParser.GROUPING, 0); }
		public TerminalNode HOUR() { return getToken(ArcticExtendSparkSqlParser.HOUR, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticExtendSparkSqlParser.IGNORE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticExtendSparkSqlParser.IMPORT, 0); }
		public TerminalNode INDEX() { return getToken(ArcticExtendSparkSqlParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticExtendSparkSqlParser.INDEXES, 0); }
		public TerminalNode INPATH() { return getToken(ArcticExtendSparkSqlParser.INPATH, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticExtendSparkSqlParser.INPUTFORMAT, 0); }
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticExtendSparkSqlParser.INTERVAL, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticExtendSparkSqlParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticExtendSparkSqlParser.KEYS, 0); }
		public TerminalNode LAST() { return getToken(ArcticExtendSparkSqlParser.LAST, 0); }
		public TerminalNode LAZY() { return getToken(ArcticExtendSparkSqlParser.LAZY, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticExtendSparkSqlParser.LIMIT, 0); }
		public TerminalNode LINES() { return getToken(ArcticExtendSparkSqlParser.LINES, 0); }
		public TerminalNode LIST() { return getToken(ArcticExtendSparkSqlParser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(ArcticExtendSparkSqlParser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticExtendSparkSqlParser.LOCAL, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticExtendSparkSqlParser.LOCATION, 0); }
		public TerminalNode LOCK() { return getToken(ArcticExtendSparkSqlParser.LOCK, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticExtendSparkSqlParser.LOCKS, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticExtendSparkSqlParser.LOGICAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticExtendSparkSqlParser.MACRO, 0); }
		public TerminalNode MAP() { return getToken(ArcticExtendSparkSqlParser.MAP, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticExtendSparkSqlParser.MATCHED, 0); }
		public TerminalNode MERGE() { return getToken(ArcticExtendSparkSqlParser.MERGE, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticExtendSparkSqlParser.MINUTE, 0); }
		public TerminalNode MONTH() { return getToken(ArcticExtendSparkSqlParser.MONTH, 0); }
		public TerminalNode MSCK() { return getToken(ArcticExtendSparkSqlParser.MSCK, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticExtendSparkSqlParser.NAMESPACE, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticExtendSparkSqlParser.NAMESPACES, 0); }
		public TerminalNode NO() { return getToken(ArcticExtendSparkSqlParser.NO, 0); }
		public TerminalNode NULLS() { return getToken(ArcticExtendSparkSqlParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(ArcticExtendSparkSqlParser.OF, 0); }
		public TerminalNode OPTION() { return getToken(ArcticExtendSparkSqlParser.OPTION, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticExtendSparkSqlParser.OPTIONS, 0); }
		public TerminalNode OUT() { return getToken(ArcticExtendSparkSqlParser.OUT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticExtendSparkSqlParser.OUTPUTFORMAT, 0); }
		public TerminalNode OVER() { return getToken(ArcticExtendSparkSqlParser.OVER, 0); }
		public TerminalNode OVERLAY() { return getToken(ArcticExtendSparkSqlParser.OVERLAY, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticExtendSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticExtendSparkSqlParser.PARTITION, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticExtendSparkSqlParser.PARTITIONED, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticExtendSparkSqlParser.PARTITIONS, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticExtendSparkSqlParser.PERCENTLIT, 0); }
		public TerminalNode PIVOT() { return getToken(ArcticExtendSparkSqlParser.PIVOT, 0); }
		public TerminalNode PLACING() { return getToken(ArcticExtendSparkSqlParser.PLACING, 0); }
		public TerminalNode POSITION() { return getToken(ArcticExtendSparkSqlParser.POSITION, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticExtendSparkSqlParser.PRECEDING, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticExtendSparkSqlParser.PRINCIPALS, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticExtendSparkSqlParser.PROPERTIES, 0); }
		public TerminalNode PURGE() { return getToken(ArcticExtendSparkSqlParser.PURGE, 0); }
		public TerminalNode QUERY() { return getToken(ArcticExtendSparkSqlParser.QUERY, 0); }
		public TerminalNode RANGE() { return getToken(ArcticExtendSparkSqlParser.RANGE, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticExtendSparkSqlParser.RECORDREADER, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticExtendSparkSqlParser.RECORDWRITER, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticExtendSparkSqlParser.RECOVER, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticExtendSparkSqlParser.REDUCE, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticExtendSparkSqlParser.REFRESH, 0); }
		public TerminalNode RENAME() { return getToken(ArcticExtendSparkSqlParser.RENAME, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticExtendSparkSqlParser.REPAIR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode RESET() { return getToken(ArcticExtendSparkSqlParser.RESET, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticExtendSparkSqlParser.RESPECT, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticExtendSparkSqlParser.RESTRICT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticExtendSparkSqlParser.REVOKE, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticExtendSparkSqlParser.RLIKE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticExtendSparkSqlParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticExtendSparkSqlParser.ROLES, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticExtendSparkSqlParser.ROLLBACK, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticExtendSparkSqlParser.ROLLUP, 0); }
		public TerminalNode ROW() { return getToken(ArcticExtendSparkSqlParser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(ArcticExtendSparkSqlParser.ROWS, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticExtendSparkSqlParser.SCHEMA, 0); }
		public TerminalNode SECOND() { return getToken(ArcticExtendSparkSqlParser.SECOND, 0); }
		public TerminalNode SEMI() { return getToken(ArcticExtendSparkSqlParser.SEMI, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticExtendSparkSqlParser.SEPARATED, 0); }
		public TerminalNode SERDE() { return getToken(ArcticExtendSparkSqlParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.SERDEPROPERTIES, 0); }
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticExtendSparkSqlParser.SETMINUS, 0); }
		public TerminalNode SETS() { return getToken(ArcticExtendSparkSqlParser.SETS, 0); }
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticExtendSparkSqlParser.SKEWED, 0); }
		public TerminalNode SORT() { return getToken(ArcticExtendSparkSqlParser.SORT, 0); }
		public TerminalNode SORTED() { return getToken(ArcticExtendSparkSqlParser.SORTED, 0); }
		public TerminalNode START() { return getToken(ArcticExtendSparkSqlParser.START, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticExtendSparkSqlParser.STATISTICS, 0); }
		public TerminalNode STORED() { return getToken(ArcticExtendSparkSqlParser.STORED, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticExtendSparkSqlParser.STRATIFY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticExtendSparkSqlParser.STRUCT, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticExtendSparkSqlParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticExtendSparkSqlParser.SUBSTRING, 0); }
		public TerminalNode SYNC() { return getToken(ArcticExtendSparkSqlParser.SYNC, 0); }
		public TerminalNode TABLES() { return getToken(ArcticExtendSparkSqlParser.TABLES, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticExtendSparkSqlParser.TABLESAMPLE, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticExtendSparkSqlParser.TERMINATED, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticExtendSparkSqlParser.TOUCH, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticExtendSparkSqlParser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticExtendSparkSqlParser.TRANSACTIONS, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticExtendSparkSqlParser.TRANSFORM, 0); }
		public TerminalNode TRIM() { return getToken(ArcticExtendSparkSqlParser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(ArcticExtendSparkSqlParser.TRUE, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticExtendSparkSqlParser.TRUNCATE, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticExtendSparkSqlParser.TRY_CAST, 0); }
		public TerminalNode TYPE() { return getToken(ArcticExtendSparkSqlParser.TYPE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticExtendSparkSqlParser.UNARCHIVE, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticExtendSparkSqlParser.UNBOUNDED, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticExtendSparkSqlParser.UNCACHE, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticExtendSparkSqlParser.UNLOCK, 0); }
		public TerminalNode UNSET() { return getToken(ArcticExtendSparkSqlParser.UNSET, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticExtendSparkSqlParser.UPDATE, 0); }
		public TerminalNode USE() { return getToken(ArcticExtendSparkSqlParser.USE, 0); }
		public TerminalNode VALUES() { return getToken(ArcticExtendSparkSqlParser.VALUES, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticExtendSparkSqlParser.VIEWS, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticExtendSparkSqlParser.WINDOW, 0); }
		public TerminalNode YEAR() { return getToken(ArcticExtendSparkSqlParser.YEAR, 0); }
		public TerminalNode ZONE() { return getToken(ArcticExtendSparkSqlParser.ZONE, 0); }
		public AnsiNonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ansiNonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterAnsiNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitAnsiNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitAnsiNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnsiNonReservedContext ansiNonReserved() throws RecognitionException {
		AnsiNonReservedContext _localctx = new AnsiNonReservedContext(_ctx, getState());
		enterRule(_localctx, 290, RULE_ansiNonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3174);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ADD) | (1L << AFTER) | (1L << ALTER) | (1L << ANALYZE) | (1L << ANTI) | (1L << ARCHIVE) | (1L << ARRAY) | (1L << ASC) | (1L << AT) | (1L << BETWEEN) | (1L << BUCKET) | (1L << BUCKETS) | (1L << BY) | (1L << CACHE) | (1L << CASCADE) | (1L << CHANGE) | (1L << CLEAR) | (1L << CLUSTER) | (1L << CLUSTERED) | (1L << CODEGEN) | (1L << COLLECTION) | (1L << COLUMNS) | (1L << COMMENT) | (1L << COMMIT) | (1L << COMPACT) | (1L << COMPACTIONS) | (1L << COMPUTE) | (1L << CONCATENATE) | (1L << COST) | (1L << CUBE) | (1L << CURRENT) | (1L << DAY))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (DATA - 64)) | (1L << (DATABASE - 64)) | (1L << (DATABASES - 64)) | (1L << (DBPROPERTIES - 64)) | (1L << (DEFINED - 64)) | (1L << (DELETE - 64)) | (1L << (DELIMITED - 64)) | (1L << (DESC - 64)) | (1L << (DESCRIBE - 64)) | (1L << (DFS - 64)) | (1L << (DIRECTORIES - 64)) | (1L << (DIRECTORY - 64)) | (1L << (DISTRIBUTE - 64)) | (1L << (DIV - 64)) | (1L << (DROP - 64)) | (1L << (ESCAPED - 64)) | (1L << (EXCHANGE - 64)) | (1L << (EXISTS - 64)) | (1L << (EXPLAIN - 64)) | (1L << (EXPORT - 64)) | (1L << (EXTENDED - 64)) | (1L << (EXTERNAL - 64)) | (1L << (EXTRACT - 64)) | (1L << (FIELDS - 64)) | (1L << (FILEFORMAT - 64)) | (1L << (FIRST - 64)) | (1L << (FOLLOWING - 64)) | (1L << (FORMAT - 64)) | (1L << (FORMATTED - 64)) | (1L << (FUNCTION - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (GLOBAL - 64)) | (1L << (GROUPING - 64)) | (1L << (HOUR - 64)) | (1L << (IF - 64)) | (1L << (IGNORE - 64)) | (1L << (IMPORT - 64)) | (1L << (INDEX - 64)) | (1L << (INDEXES - 64)) | (1L << (INPATH - 64)) | (1L << (INPUTFORMAT - 64)) | (1L << (INSERT - 64)) | (1L << (INTERVAL - 64)) | (1L << (ITEMS - 64)))) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & ((1L << (KEYS - 129)) | (1L << (LAST - 129)) | (1L << (LAZY - 129)) | (1L << (LIKE - 129)) | (1L << (LIMIT - 129)) | (1L << (LINES - 129)) | (1L << (LIST - 129)) | (1L << (LOAD - 129)) | (1L << (LOCAL - 129)) | (1L << (LOCATION - 129)) | (1L << (LOCK - 129)) | (1L << (LOCKS - 129)) | (1L << (LOGICAL - 129)) | (1L << (MACRO - 129)) | (1L << (MAP - 129)) | (1L << (MATCHED - 129)) | (1L << (MERGE - 129)) | (1L << (MINUTE - 129)) | (1L << (MONTH - 129)) | (1L << (MSCK - 129)) | (1L << (NAMESPACE - 129)) | (1L << (NAMESPACES - 129)) | (1L << (NO - 129)) | (1L << (NULLS - 129)) | (1L << (OF - 129)) | (1L << (OPTION - 129)) | (1L << (OPTIONS - 129)) | (1L << (OUT - 129)) | (1L << (OUTPUTFORMAT - 129)) | (1L << (OVER - 129)) | (1L << (OVERLAY - 129)) | (1L << (OVERWRITE - 129)) | (1L << (PARTITION - 129)) | (1L << (PARTITIONED - 129)) | (1L << (PARTITIONS - 129)) | (1L << (PERCENTLIT - 129)) | (1L << (PIVOT - 129)) | (1L << (PLACING - 129)) | (1L << (POSITION - 129)) | (1L << (PRECEDING - 129)) | (1L << (PRINCIPALS - 129)) | (1L << (PROPERTIES - 129)) | (1L << (PURGE - 129)) | (1L << (QUERY - 129)) | (1L << (RANGE - 129)) | (1L << (RECORDREADER - 129)) | (1L << (RECORDWRITER - 129)) | (1L << (RECOVER - 129)) | (1L << (REDUCE - 129)) | (1L << (REFRESH - 129)))) != 0) || ((((_la - 193)) & ~0x3f) == 0 && ((1L << (_la - 193)) & ((1L << (RENAME - 193)) | (1L << (REPAIR - 193)) | (1L << (REPLACE - 193)) | (1L << (RESET - 193)) | (1L << (RESPECT - 193)) | (1L << (RESTRICT - 193)) | (1L << (REVOKE - 193)) | (1L << (RLIKE - 193)) | (1L << (ROLE - 193)) | (1L << (ROLES - 193)) | (1L << (ROLLBACK - 193)) | (1L << (ROLLUP - 193)) | (1L << (ROW - 193)) | (1L << (ROWS - 193)) | (1L << (SECOND - 193)) | (1L << (SCHEMA - 193)) | (1L << (SEMI - 193)) | (1L << (SEPARATED - 193)) | (1L << (SERDE - 193)) | (1L << (SERDEPROPERTIES - 193)) | (1L << (SET - 193)) | (1L << (SETMINUS - 193)) | (1L << (SETS - 193)) | (1L << (SHOW - 193)) | (1L << (SKEWED - 193)) | (1L << (SORT - 193)) | (1L << (SORTED - 193)) | (1L << (START - 193)) | (1L << (STATISTICS - 193)) | (1L << (STORED - 193)) | (1L << (STRATIFY - 193)) | (1L << (STRUCT - 193)) | (1L << (SUBSTR - 193)) | (1L << (SUBSTRING - 193)) | (1L << (SYNC - 193)) | (1L << (TABLES - 193)) | (1L << (TABLESAMPLE - 193)) | (1L << (TBLPROPERTIES - 193)) | (1L << (TEMPORARY - 193)) | (1L << (TERMINATED - 193)) | (1L << (TOUCH - 193)) | (1L << (TRANSACTION - 193)) | (1L << (TRANSACTIONS - 193)) | (1L << (TRANSFORM - 193)) | (1L << (TRIM - 193)) | (1L << (TRUE - 193)) | (1L << (TRUNCATE - 193)) | (1L << (TRY_CAST - 193)) | (1L << (TYPE - 193)) | (1L << (UNARCHIVE - 193)) | (1L << (UNBOUNDED - 193)) | (1L << (UNCACHE - 193)))) != 0) || ((((_la - 257)) & ~0x3f) == 0 && ((1L << (_la - 257)) & ((1L << (UNLOCK - 257)) | (1L << (UNSET - 257)) | (1L << (UPDATE - 257)) | (1L << (USE - 257)) | (1L << (VALUES - 257)) | (1L << (VIEW - 257)) | (1L << (VIEWS - 257)) | (1L << (WINDOW - 257)) | (1L << (YEAR - 257)) | (1L << (ZONE - 257)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StrictNonReservedContext extends ParserRuleContext {
		public TerminalNode ANTI() { return getToken(ArcticExtendSparkSqlParser.ANTI, 0); }
		public TerminalNode CROSS() { return getToken(ArcticExtendSparkSqlParser.CROSS, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticExtendSparkSqlParser.EXCEPT, 0); }
		public TerminalNode FULL() { return getToken(ArcticExtendSparkSqlParser.FULL, 0); }
		public TerminalNode INNER() { return getToken(ArcticExtendSparkSqlParser.INNER, 0); }
		public TerminalNode INTERSECT() { return getToken(ArcticExtendSparkSqlParser.INTERSECT, 0); }
		public TerminalNode JOIN() { return getToken(ArcticExtendSparkSqlParser.JOIN, 0); }
		public TerminalNode LATERAL() { return getToken(ArcticExtendSparkSqlParser.LATERAL, 0); }
		public TerminalNode LEFT() { return getToken(ArcticExtendSparkSqlParser.LEFT, 0); }
		public TerminalNode NATURAL() { return getToken(ArcticExtendSparkSqlParser.NATURAL, 0); }
		public TerminalNode ON() { return getToken(ArcticExtendSparkSqlParser.ON, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticExtendSparkSqlParser.RIGHT, 0); }
		public TerminalNode SEMI() { return getToken(ArcticExtendSparkSqlParser.SEMI, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticExtendSparkSqlParser.SETMINUS, 0); }
		public TerminalNode UNION() { return getToken(ArcticExtendSparkSqlParser.UNION, 0); }
		public TerminalNode USING() { return getToken(ArcticExtendSparkSqlParser.USING, 0); }
		public StrictNonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_strictNonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterStrictNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitStrictNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitStrictNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictNonReservedContext strictNonReserved() throws RecognitionException {
		StrictNonReservedContext _localctx = new StrictNonReservedContext(_ctx, getState());
		enterRule(_localctx, 292, RULE_strictNonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3176);
			_la = _input.LA(1);
			if ( !(_la==ANTI || _la==CROSS || ((((_la - 84)) & ~0x3f) == 0 && ((1L << (_la - 84)) & ((1L << (EXCEPT - 84)) | (1L << (FULL - 84)) | (1L << (INNER - 84)) | (1L << (INTERSECT - 84)) | (1L << (JOIN - 84)) | (1L << (LATERAL - 84)) | (1L << (LEFT - 84)))) != 0) || ((((_la - 154)) & ~0x3f) == 0 && ((1L << (_la - 154)) & ((1L << (NATURAL - 154)) | (1L << (ON - 154)) | (1L << (RIGHT - 154)) | (1L << (SEMI - 154)) | (1L << (SETMINUS - 154)))) != 0) || _la==UNION || _la==USING) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NonReservedContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(ArcticExtendSparkSqlParser.ADD, 0); }
		public TerminalNode AFTER() { return getToken(ArcticExtendSparkSqlParser.AFTER, 0); }
		public TerminalNode ALL() { return getToken(ArcticExtendSparkSqlParser.ALL, 0); }
		public TerminalNode ALTER() { return getToken(ArcticExtendSparkSqlParser.ALTER, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticExtendSparkSqlParser.ANALYZE, 0); }
		public TerminalNode AND() { return getToken(ArcticExtendSparkSqlParser.AND, 0); }
		public TerminalNode ANY() { return getToken(ArcticExtendSparkSqlParser.ANY, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticExtendSparkSqlParser.ARCHIVE, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticExtendSparkSqlParser.ARRAY, 0); }
		public TerminalNode AS() { return getToken(ArcticExtendSparkSqlParser.AS, 0); }
		public TerminalNode ASC() { return getToken(ArcticExtendSparkSqlParser.ASC, 0); }
		public TerminalNode AT() { return getToken(ArcticExtendSparkSqlParser.AT, 0); }
		public TerminalNode AUTHORIZATION() { return getToken(ArcticExtendSparkSqlParser.AUTHORIZATION, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticExtendSparkSqlParser.BETWEEN, 0); }
		public TerminalNode BOTH() { return getToken(ArcticExtendSparkSqlParser.BOTH, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticExtendSparkSqlParser.BUCKET, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticExtendSparkSqlParser.BUCKETS, 0); }
		public TerminalNode BY() { return getToken(ArcticExtendSparkSqlParser.BY, 0); }
		public TerminalNode CACHE() { return getToken(ArcticExtendSparkSqlParser.CACHE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticExtendSparkSqlParser.CASCADE, 0); }
		public TerminalNode CASE() { return getToken(ArcticExtendSparkSqlParser.CASE, 0); }
		public TerminalNode CAST() { return getToken(ArcticExtendSparkSqlParser.CAST, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticExtendSparkSqlParser.CHANGE, 0); }
		public TerminalNode CHECK() { return getToken(ArcticExtendSparkSqlParser.CHECK, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticExtendSparkSqlParser.CLEAR, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticExtendSparkSqlParser.CLUSTER, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticExtendSparkSqlParser.CLUSTERED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticExtendSparkSqlParser.CODEGEN, 0); }
		public TerminalNode COLLATE() { return getToken(ArcticExtendSparkSqlParser.COLLATE, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticExtendSparkSqlParser.COLLECTION, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticExtendSparkSqlParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticExtendSparkSqlParser.COLUMNS, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticExtendSparkSqlParser.COMMENT, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticExtendSparkSqlParser.COMMIT, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticExtendSparkSqlParser.COMPACT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticExtendSparkSqlParser.COMPACTIONS, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticExtendSparkSqlParser.COMPUTE, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticExtendSparkSqlParser.CONCATENATE, 0); }
		public TerminalNode CONSTRAINT() { return getToken(ArcticExtendSparkSqlParser.CONSTRAINT, 0); }
		public TerminalNode COST() { return getToken(ArcticExtendSparkSqlParser.COST, 0); }
		public TerminalNode CREATE() { return getToken(ArcticExtendSparkSqlParser.CREATE, 0); }
		public TerminalNode CUBE() { return getToken(ArcticExtendSparkSqlParser.CUBE, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticExtendSparkSqlParser.CURRENT, 0); }
		public TerminalNode CURRENT_DATE() { return getToken(ArcticExtendSparkSqlParser.CURRENT_DATE, 0); }
		public TerminalNode CURRENT_TIME() { return getToken(ArcticExtendSparkSqlParser.CURRENT_TIME, 0); }
		public TerminalNode CURRENT_TIMESTAMP() { return getToken(ArcticExtendSparkSqlParser.CURRENT_TIMESTAMP, 0); }
		public TerminalNode CURRENT_USER() { return getToken(ArcticExtendSparkSqlParser.CURRENT_USER, 0); }
		public TerminalNode DATA() { return getToken(ArcticExtendSparkSqlParser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticExtendSparkSqlParser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticExtendSparkSqlParser.DATABASES, 0); }
		public TerminalNode DAY() { return getToken(ArcticExtendSparkSqlParser.DAY, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.DBPROPERTIES, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticExtendSparkSqlParser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(ArcticExtendSparkSqlParser.DELETE, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticExtendSparkSqlParser.DELIMITED, 0); }
		public TerminalNode DESC() { return getToken(ArcticExtendSparkSqlParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticExtendSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode DFS() { return getToken(ArcticExtendSparkSqlParser.DFS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticExtendSparkSqlParser.DIRECTORIES, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticExtendSparkSqlParser.DIRECTORY, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticExtendSparkSqlParser.DISTINCT, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticExtendSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode DIV() { return getToken(ArcticExtendSparkSqlParser.DIV, 0); }
		public TerminalNode DROP() { return getToken(ArcticExtendSparkSqlParser.DROP, 0); }
		public TerminalNode ELSE() { return getToken(ArcticExtendSparkSqlParser.ELSE, 0); }
		public TerminalNode END() { return getToken(ArcticExtendSparkSqlParser.END, 0); }
		public TerminalNode ESCAPE() { return getToken(ArcticExtendSparkSqlParser.ESCAPE, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticExtendSparkSqlParser.ESCAPED, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticExtendSparkSqlParser.EXCHANGE, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticExtendSparkSqlParser.EXISTS, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticExtendSparkSqlParser.EXPLAIN, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticExtendSparkSqlParser.EXPORT, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticExtendSparkSqlParser.EXTENDED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticExtendSparkSqlParser.EXTERNAL, 0); }
		public TerminalNode EXTRACT() { return getToken(ArcticExtendSparkSqlParser.EXTRACT, 0); }
		public TerminalNode FALSE() { return getToken(ArcticExtendSparkSqlParser.FALSE, 0); }
		public TerminalNode FETCH() { return getToken(ArcticExtendSparkSqlParser.FETCH, 0); }
		public TerminalNode FILTER() { return getToken(ArcticExtendSparkSqlParser.FILTER, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticExtendSparkSqlParser.FIELDS, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticExtendSparkSqlParser.FILEFORMAT, 0); }
		public TerminalNode FIRST() { return getToken(ArcticExtendSparkSqlParser.FIRST, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticExtendSparkSqlParser.FOLLOWING, 0); }
		public TerminalNode FOR() { return getToken(ArcticExtendSparkSqlParser.FOR, 0); }
		public TerminalNode FOREIGN() { return getToken(ArcticExtendSparkSqlParser.FOREIGN, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticExtendSparkSqlParser.FORMAT, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticExtendSparkSqlParser.FORMATTED, 0); }
		public TerminalNode FROM() { return getToken(ArcticExtendSparkSqlParser.FROM, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticExtendSparkSqlParser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticExtendSparkSqlParser.FUNCTIONS, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticExtendSparkSqlParser.GLOBAL, 0); }
		public TerminalNode GRANT() { return getToken(ArcticExtendSparkSqlParser.GRANT, 0); }
		public TerminalNode GROUP() { return getToken(ArcticExtendSparkSqlParser.GROUP, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticExtendSparkSqlParser.GROUPING, 0); }
		public TerminalNode HAVING() { return getToken(ArcticExtendSparkSqlParser.HAVING, 0); }
		public TerminalNode HOUR() { return getToken(ArcticExtendSparkSqlParser.HOUR, 0); }
		public TerminalNode IF() { return getToken(ArcticExtendSparkSqlParser.IF, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticExtendSparkSqlParser.IGNORE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticExtendSparkSqlParser.IMPORT, 0); }
		public TerminalNode IN() { return getToken(ArcticExtendSparkSqlParser.IN, 0); }
		public TerminalNode INDEX() { return getToken(ArcticExtendSparkSqlParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticExtendSparkSqlParser.INDEXES, 0); }
		public TerminalNode INPATH() { return getToken(ArcticExtendSparkSqlParser.INPATH, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticExtendSparkSqlParser.INPUTFORMAT, 0); }
		public TerminalNode INSERT() { return getToken(ArcticExtendSparkSqlParser.INSERT, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticExtendSparkSqlParser.INTERVAL, 0); }
		public TerminalNode INTO() { return getToken(ArcticExtendSparkSqlParser.INTO, 0); }
		public TerminalNode IS() { return getToken(ArcticExtendSparkSqlParser.IS, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticExtendSparkSqlParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticExtendSparkSqlParser.KEYS, 0); }
		public TerminalNode LAST() { return getToken(ArcticExtendSparkSqlParser.LAST, 0); }
		public TerminalNode LAZY() { return getToken(ArcticExtendSparkSqlParser.LAZY, 0); }
		public TerminalNode LEADING() { return getToken(ArcticExtendSparkSqlParser.LEADING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticExtendSparkSqlParser.LIKE, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticExtendSparkSqlParser.LIMIT, 0); }
		public TerminalNode LINES() { return getToken(ArcticExtendSparkSqlParser.LINES, 0); }
		public TerminalNode LIST() { return getToken(ArcticExtendSparkSqlParser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(ArcticExtendSparkSqlParser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticExtendSparkSqlParser.LOCAL, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticExtendSparkSqlParser.LOCATION, 0); }
		public TerminalNode LOCK() { return getToken(ArcticExtendSparkSqlParser.LOCK, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticExtendSparkSqlParser.LOCKS, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticExtendSparkSqlParser.LOGICAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticExtendSparkSqlParser.MACRO, 0); }
		public TerminalNode MAP() { return getToken(ArcticExtendSparkSqlParser.MAP, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticExtendSparkSqlParser.MATCHED, 0); }
		public TerminalNode MERGE() { return getToken(ArcticExtendSparkSqlParser.MERGE, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticExtendSparkSqlParser.MINUTE, 0); }
		public TerminalNode MONTH() { return getToken(ArcticExtendSparkSqlParser.MONTH, 0); }
		public TerminalNode MSCK() { return getToken(ArcticExtendSparkSqlParser.MSCK, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticExtendSparkSqlParser.NAMESPACE, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticExtendSparkSqlParser.NAMESPACES, 0); }
		public TerminalNode NO() { return getToken(ArcticExtendSparkSqlParser.NO, 0); }
		public TerminalNode NOT() { return getToken(ArcticExtendSparkSqlParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticExtendSparkSqlParser.NULL, 0); }
		public TerminalNode NULLS() { return getToken(ArcticExtendSparkSqlParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(ArcticExtendSparkSqlParser.OF, 0); }
		public TerminalNode ONLY() { return getToken(ArcticExtendSparkSqlParser.ONLY, 0); }
		public TerminalNode OPTION() { return getToken(ArcticExtendSparkSqlParser.OPTION, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticExtendSparkSqlParser.OPTIONS, 0); }
		public TerminalNode OR() { return getToken(ArcticExtendSparkSqlParser.OR, 0); }
		public TerminalNode ORDER() { return getToken(ArcticExtendSparkSqlParser.ORDER, 0); }
		public TerminalNode OUT() { return getToken(ArcticExtendSparkSqlParser.OUT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticExtendSparkSqlParser.OUTER, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticExtendSparkSqlParser.OUTPUTFORMAT, 0); }
		public TerminalNode OVER() { return getToken(ArcticExtendSparkSqlParser.OVER, 0); }
		public TerminalNode OVERLAPS() { return getToken(ArcticExtendSparkSqlParser.OVERLAPS, 0); }
		public TerminalNode OVERLAY() { return getToken(ArcticExtendSparkSqlParser.OVERLAY, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticExtendSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticExtendSparkSqlParser.PARTITION, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticExtendSparkSqlParser.PARTITIONED, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticExtendSparkSqlParser.PARTITIONS, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticExtendSparkSqlParser.PERCENTLIT, 0); }
		public TerminalNode PIVOT() { return getToken(ArcticExtendSparkSqlParser.PIVOT, 0); }
		public TerminalNode PLACING() { return getToken(ArcticExtendSparkSqlParser.PLACING, 0); }
		public TerminalNode POSITION() { return getToken(ArcticExtendSparkSqlParser.POSITION, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticExtendSparkSqlParser.PRECEDING, 0); }
		public TerminalNode PRIMARY() { return getToken(ArcticExtendSparkSqlParser.PRIMARY, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticExtendSparkSqlParser.PRINCIPALS, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticExtendSparkSqlParser.PROPERTIES, 0); }
		public TerminalNode PURGE() { return getToken(ArcticExtendSparkSqlParser.PURGE, 0); }
		public TerminalNode QUERY() { return getToken(ArcticExtendSparkSqlParser.QUERY, 0); }
		public TerminalNode RANGE() { return getToken(ArcticExtendSparkSqlParser.RANGE, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticExtendSparkSqlParser.RECORDREADER, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticExtendSparkSqlParser.RECORDWRITER, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticExtendSparkSqlParser.RECOVER, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticExtendSparkSqlParser.REDUCE, 0); }
		public TerminalNode REFERENCES() { return getToken(ArcticExtendSparkSqlParser.REFERENCES, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticExtendSparkSqlParser.REFRESH, 0); }
		public TerminalNode RENAME() { return getToken(ArcticExtendSparkSqlParser.RENAME, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticExtendSparkSqlParser.REPAIR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticExtendSparkSqlParser.REPLACE, 0); }
		public TerminalNode RESET() { return getToken(ArcticExtendSparkSqlParser.RESET, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticExtendSparkSqlParser.RESPECT, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticExtendSparkSqlParser.RESTRICT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticExtendSparkSqlParser.REVOKE, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticExtendSparkSqlParser.RLIKE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticExtendSparkSqlParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticExtendSparkSqlParser.ROLES, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticExtendSparkSqlParser.ROLLBACK, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticExtendSparkSqlParser.ROLLUP, 0); }
		public TerminalNode ROW() { return getToken(ArcticExtendSparkSqlParser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(ArcticExtendSparkSqlParser.ROWS, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticExtendSparkSqlParser.SCHEMA, 0); }
		public TerminalNode SECOND() { return getToken(ArcticExtendSparkSqlParser.SECOND, 0); }
		public TerminalNode SELECT() { return getToken(ArcticExtendSparkSqlParser.SELECT, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticExtendSparkSqlParser.SEPARATED, 0); }
		public TerminalNode SERDE() { return getToken(ArcticExtendSparkSqlParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.SERDEPROPERTIES, 0); }
		public TerminalNode SESSION_USER() { return getToken(ArcticExtendSparkSqlParser.SESSION_USER, 0); }
		public TerminalNode SET() { return getToken(ArcticExtendSparkSqlParser.SET, 0); }
		public TerminalNode SETS() { return getToken(ArcticExtendSparkSqlParser.SETS, 0); }
		public TerminalNode SHOW() { return getToken(ArcticExtendSparkSqlParser.SHOW, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticExtendSparkSqlParser.SKEWED, 0); }
		public TerminalNode SOME() { return getToken(ArcticExtendSparkSqlParser.SOME, 0); }
		public TerminalNode SORT() { return getToken(ArcticExtendSparkSqlParser.SORT, 0); }
		public TerminalNode SORTED() { return getToken(ArcticExtendSparkSqlParser.SORTED, 0); }
		public TerminalNode START() { return getToken(ArcticExtendSparkSqlParser.START, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticExtendSparkSqlParser.STATISTICS, 0); }
		public TerminalNode STORED() { return getToken(ArcticExtendSparkSqlParser.STORED, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticExtendSparkSqlParser.STRATIFY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticExtendSparkSqlParser.STRUCT, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticExtendSparkSqlParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticExtendSparkSqlParser.SUBSTRING, 0); }
		public TerminalNode SYNC() { return getToken(ArcticExtendSparkSqlParser.SYNC, 0); }
		public TerminalNode TABLE() { return getToken(ArcticExtendSparkSqlParser.TABLE, 0); }
		public TerminalNode TABLES() { return getToken(ArcticExtendSparkSqlParser.TABLES, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticExtendSparkSqlParser.TABLESAMPLE, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticExtendSparkSqlParser.TBLPROPERTIES, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticExtendSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticExtendSparkSqlParser.TERMINATED, 0); }
		public TerminalNode THEN() { return getToken(ArcticExtendSparkSqlParser.THEN, 0); }
		public TerminalNode TIME() { return getToken(ArcticExtendSparkSqlParser.TIME, 0); }
		public TerminalNode TO() { return getToken(ArcticExtendSparkSqlParser.TO, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticExtendSparkSqlParser.TOUCH, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticExtendSparkSqlParser.TRAILING, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticExtendSparkSqlParser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticExtendSparkSqlParser.TRANSACTIONS, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticExtendSparkSqlParser.TRANSFORM, 0); }
		public TerminalNode TRIM() { return getToken(ArcticExtendSparkSqlParser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(ArcticExtendSparkSqlParser.TRUE, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticExtendSparkSqlParser.TRUNCATE, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticExtendSparkSqlParser.TRY_CAST, 0); }
		public TerminalNode TYPE() { return getToken(ArcticExtendSparkSqlParser.TYPE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticExtendSparkSqlParser.UNARCHIVE, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticExtendSparkSqlParser.UNBOUNDED, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticExtendSparkSqlParser.UNCACHE, 0); }
		public TerminalNode UNIQUE() { return getToken(ArcticExtendSparkSqlParser.UNIQUE, 0); }
		public TerminalNode UNKNOWN() { return getToken(ArcticExtendSparkSqlParser.UNKNOWN, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticExtendSparkSqlParser.UNLOCK, 0); }
		public TerminalNode UNSET() { return getToken(ArcticExtendSparkSqlParser.UNSET, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticExtendSparkSqlParser.UPDATE, 0); }
		public TerminalNode USE() { return getToken(ArcticExtendSparkSqlParser.USE, 0); }
		public TerminalNode USER() { return getToken(ArcticExtendSparkSqlParser.USER, 0); }
		public TerminalNode VALUES() { return getToken(ArcticExtendSparkSqlParser.VALUES, 0); }
		public TerminalNode VIEW() { return getToken(ArcticExtendSparkSqlParser.VIEW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticExtendSparkSqlParser.VIEWS, 0); }
		public TerminalNode WHEN() { return getToken(ArcticExtendSparkSqlParser.WHEN, 0); }
		public TerminalNode WHERE() { return getToken(ArcticExtendSparkSqlParser.WHERE, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticExtendSparkSqlParser.WINDOW, 0); }
		public TerminalNode WITH() { return getToken(ArcticExtendSparkSqlParser.WITH, 0); }
		public TerminalNode YEAR() { return getToken(ArcticExtendSparkSqlParser.YEAR, 0); }
		public TerminalNode ZONE() { return getToken(ArcticExtendSparkSqlParser.ZONE, 0); }
		public NonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).enterNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticExtendSparkSqlListener ) ((ArcticExtendSparkSqlListener)listener).exitNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticExtendSparkSqlVisitor ) return ((ArcticExtendSparkSqlVisitor<? extends T>)visitor).visitNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonReservedContext nonReserved() throws RecognitionException {
		NonReservedContext _localctx = new NonReservedContext(_ctx, getState());
		enterRule(_localctx, 294, RULE_nonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3178);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ADD) | (1L << AFTER) | (1L << ALL) | (1L << ALTER) | (1L << ANALYZE) | (1L << AND) | (1L << ANY) | (1L << ARCHIVE) | (1L << ARRAY) | (1L << AS) | (1L << ASC) | (1L << AT) | (1L << AUTHORIZATION) | (1L << BETWEEN) | (1L << BOTH) | (1L << BUCKET) | (1L << BUCKETS) | (1L << BY) | (1L << CACHE) | (1L << CASCADE) | (1L << CASE) | (1L << CAST) | (1L << CHANGE) | (1L << CHECK) | (1L << CLEAR) | (1L << CLUSTER) | (1L << CLUSTERED) | (1L << CODEGEN) | (1L << COLLATE) | (1L << COLLECTION) | (1L << COLUMN) | (1L << COLUMNS) | (1L << COMMENT) | (1L << COMMIT) | (1L << COMPACT) | (1L << COMPACTIONS) | (1L << COMPUTE) | (1L << CONCATENATE) | (1L << CONSTRAINT) | (1L << COST) | (1L << CREATE) | (1L << CUBE) | (1L << CURRENT) | (1L << CURRENT_DATE) | (1L << CURRENT_TIME) | (1L << CURRENT_TIMESTAMP) | (1L << CURRENT_USER) | (1L << DAY))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (DATA - 64)) | (1L << (DATABASE - 64)) | (1L << (DATABASES - 64)) | (1L << (DBPROPERTIES - 64)) | (1L << (DEFINED - 64)) | (1L << (DELETE - 64)) | (1L << (DELIMITED - 64)) | (1L << (DESC - 64)) | (1L << (DESCRIBE - 64)) | (1L << (DFS - 64)) | (1L << (DIRECTORIES - 64)) | (1L << (DIRECTORY - 64)) | (1L << (DISTINCT - 64)) | (1L << (DISTRIBUTE - 64)) | (1L << (DIV - 64)) | (1L << (DROP - 64)) | (1L << (ELSE - 64)) | (1L << (END - 64)) | (1L << (ESCAPE - 64)) | (1L << (ESCAPED - 64)) | (1L << (EXCHANGE - 64)) | (1L << (EXISTS - 64)) | (1L << (EXPLAIN - 64)) | (1L << (EXPORT - 64)) | (1L << (EXTENDED - 64)) | (1L << (EXTERNAL - 64)) | (1L << (EXTRACT - 64)) | (1L << (FALSE - 64)) | (1L << (FETCH - 64)) | (1L << (FIELDS - 64)) | (1L << (FILTER - 64)) | (1L << (FILEFORMAT - 64)) | (1L << (FIRST - 64)) | (1L << (FOLLOWING - 64)) | (1L << (FOR - 64)) | (1L << (FOREIGN - 64)) | (1L << (FORMAT - 64)) | (1L << (FORMATTED - 64)) | (1L << (FROM - 64)) | (1L << (FUNCTION - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (GLOBAL - 64)) | (1L << (GRANT - 64)) | (1L << (GROUP - 64)) | (1L << (GROUPING - 64)) | (1L << (HAVING - 64)) | (1L << (HOUR - 64)) | (1L << (IF - 64)) | (1L << (IGNORE - 64)) | (1L << (IMPORT - 64)) | (1L << (IN - 64)) | (1L << (INDEX - 64)) | (1L << (INDEXES - 64)) | (1L << (INPATH - 64)) | (1L << (INPUTFORMAT - 64)) | (1L << (INSERT - 64)) | (1L << (INTERVAL - 64)) | (1L << (INTO - 64)) | (1L << (IS - 64)) | (1L << (ITEMS - 64)))) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & ((1L << (KEYS - 129)) | (1L << (LAST - 129)) | (1L << (LAZY - 129)) | (1L << (LEADING - 129)) | (1L << (LIKE - 129)) | (1L << (LIMIT - 129)) | (1L << (LINES - 129)) | (1L << (LIST - 129)) | (1L << (LOAD - 129)) | (1L << (LOCAL - 129)) | (1L << (LOCATION - 129)) | (1L << (LOCK - 129)) | (1L << (LOCKS - 129)) | (1L << (LOGICAL - 129)) | (1L << (MACRO - 129)) | (1L << (MAP - 129)) | (1L << (MATCHED - 129)) | (1L << (MERGE - 129)) | (1L << (MINUTE - 129)) | (1L << (MONTH - 129)) | (1L << (MSCK - 129)) | (1L << (NAMESPACE - 129)) | (1L << (NAMESPACES - 129)) | (1L << (NO - 129)) | (1L << (NOT - 129)) | (1L << (NULL - 129)) | (1L << (NULLS - 129)) | (1L << (OF - 129)) | (1L << (ONLY - 129)) | (1L << (OPTION - 129)) | (1L << (OPTIONS - 129)) | (1L << (OR - 129)) | (1L << (ORDER - 129)) | (1L << (OUT - 129)) | (1L << (OUTER - 129)) | (1L << (OUTPUTFORMAT - 129)) | (1L << (OVER - 129)) | (1L << (OVERLAPS - 129)) | (1L << (OVERLAY - 129)) | (1L << (OVERWRITE - 129)) | (1L << (PARTITION - 129)) | (1L << (PARTITIONED - 129)) | (1L << (PARTITIONS - 129)) | (1L << (PERCENTLIT - 129)) | (1L << (PIVOT - 129)) | (1L << (PLACING - 129)) | (1L << (POSITION - 129)) | (1L << (PRECEDING - 129)) | (1L << (PRIMARY - 129)) | (1L << (PRINCIPALS - 129)) | (1L << (PROPERTIES - 129)) | (1L << (PURGE - 129)) | (1L << (QUERY - 129)) | (1L << (RANGE - 129)) | (1L << (RECORDREADER - 129)) | (1L << (RECORDWRITER - 129)) | (1L << (RECOVER - 129)) | (1L << (REDUCE - 129)) | (1L << (REFERENCES - 129)) | (1L << (REFRESH - 129)))) != 0) || ((((_la - 193)) & ~0x3f) == 0 && ((1L << (_la - 193)) & ((1L << (RENAME - 193)) | (1L << (REPAIR - 193)) | (1L << (REPLACE - 193)) | (1L << (RESET - 193)) | (1L << (RESPECT - 193)) | (1L << (RESTRICT - 193)) | (1L << (REVOKE - 193)) | (1L << (RLIKE - 193)) | (1L << (ROLE - 193)) | (1L << (ROLES - 193)) | (1L << (ROLLBACK - 193)) | (1L << (ROLLUP - 193)) | (1L << (ROW - 193)) | (1L << (ROWS - 193)) | (1L << (SECOND - 193)) | (1L << (SCHEMA - 193)) | (1L << (SELECT - 193)) | (1L << (SEPARATED - 193)) | (1L << (SERDE - 193)) | (1L << (SERDEPROPERTIES - 193)) | (1L << (SESSION_USER - 193)) | (1L << (SET - 193)) | (1L << (SETS - 193)) | (1L << (SHOW - 193)) | (1L << (SKEWED - 193)) | (1L << (SOME - 193)) | (1L << (SORT - 193)) | (1L << (SORTED - 193)) | (1L << (START - 193)) | (1L << (STATISTICS - 193)) | (1L << (STORED - 193)) | (1L << (STRATIFY - 193)) | (1L << (STRUCT - 193)) | (1L << (SUBSTR - 193)) | (1L << (SUBSTRING - 193)) | (1L << (SYNC - 193)) | (1L << (TABLE - 193)) | (1L << (TABLES - 193)) | (1L << (TABLESAMPLE - 193)) | (1L << (TBLPROPERTIES - 193)) | (1L << (TEMPORARY - 193)) | (1L << (TERMINATED - 193)) | (1L << (THEN - 193)) | (1L << (TIME - 193)) | (1L << (TO - 193)) | (1L << (TOUCH - 193)) | (1L << (TRAILING - 193)) | (1L << (TRANSACTION - 193)) | (1L << (TRANSACTIONS - 193)) | (1L << (TRANSFORM - 193)) | (1L << (TRIM - 193)) | (1L << (TRUE - 193)) | (1L << (TRUNCATE - 193)) | (1L << (TRY_CAST - 193)) | (1L << (TYPE - 193)) | (1L << (UNARCHIVE - 193)) | (1L << (UNBOUNDED - 193)) | (1L << (UNCACHE - 193)) | (1L << (UNIQUE - 193)) | (1L << (UNKNOWN - 193)))) != 0) || ((((_la - 257)) & ~0x3f) == 0 && ((1L << (_la - 257)) & ((1L << (UNLOCK - 257)) | (1L << (UNSET - 257)) | (1L << (UPDATE - 257)) | (1L << (USE - 257)) | (1L << (USER - 257)) | (1L << (VALUES - 257)) | (1L << (VIEW - 257)) | (1L << (VIEWS - 257)) | (1L << (WHEN - 257)) | (1L << (WHERE - 257)) | (1L << (WINDOW - 257)) | (1L << (WITH - 257)) | (1L << (YEAR - 257)) | (1L << (ZONE - 257)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 47:
			return queryTerm_sempred((QueryTermContext)_localctx, predIndex);
		case 106:
			return booleanExpression_sempred((BooleanExpressionContext)_localctx, predIndex);
		case 108:
			return valueExpression_sempred((ValueExpressionContext)_localctx, predIndex);
		case 109:
			return primaryExpression_sempred((PrimaryExpressionContext)_localctx, predIndex);
		case 140:
			return identifier_sempred((IdentifierContext)_localctx, predIndex);
		case 141:
			return strictIdentifier_sempred((StrictIdentifierContext)_localctx, predIndex);
		case 143:
			return number_sempred((NumberContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean queryTerm_sempred(QueryTermContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return legacy_setops_precedence_enabled;
		case 2:
			return precpred(_ctx, 2);
		case 3:
			return !legacy_setops_precedence_enabled;
		case 4:
			return precpred(_ctx, 1);
		case 5:
			return !legacy_setops_precedence_enabled;
		}
		return true;
	}
	private boolean booleanExpression_sempred(BooleanExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 6:
			return precpred(_ctx, 2);
		case 7:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean valueExpression_sempred(ValueExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 8:
			return precpred(_ctx, 6);
		case 9:
			return precpred(_ctx, 5);
		case 10:
			return precpred(_ctx, 4);
		case 11:
			return precpred(_ctx, 3);
		case 12:
			return precpred(_ctx, 2);
		case 13:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean primaryExpression_sempred(PrimaryExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 14:
			return precpred(_ctx, 8);
		case 15:
			return precpred(_ctx, 6);
		}
		return true;
	}
	private boolean identifier_sempred(IdentifierContext _localctx, int predIndex) {
		switch (predIndex) {
		case 16:
			return !SQL_standard_keyword_behavior;
		}
		return true;
	}
	private boolean strictIdentifier_sempred(StrictIdentifierContext _localctx, int predIndex) {
		switch (predIndex) {
		case 17:
			return SQL_standard_keyword_behavior;
		case 18:
			return !SQL_standard_keyword_behavior;
		}
		return true;
	}
	private boolean number_sempred(NumberContext _localctx, int predIndex) {
		switch (predIndex) {
		case 19:
			return !legacy_exponent_literal_as_decimal_enabled;
		case 20:
			return !legacy_exponent_literal_as_decimal_enabled;
		case 21:
			return legacy_exponent_literal_as_decimal_enabled;
		}
		return true;
	}

	private static final int _serializedATNSegments = 2;
	private static final String _serializedATNSegment0 =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\u0133\u0c6f\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k\t"+
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv\4"+
		"w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t\u0080"+
		"\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084\4\u0085"+
		"\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089\t\u0089"+
		"\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d\4\u008e"+
		"\t\u008e\4\u008f\t\u008f\4\u0090\t\u0090\4\u0091\t\u0091\4\u0092\t\u0092"+
		"\4\u0093\t\u0093\4\u0094\t\u0094\4\u0095\t\u0095\3\2\3\2\3\3\3\3\3\4\3"+
		"\4\3\4\3\4\3\4\5\4\u0134\n\4\3\4\5\4\u0137\n\4\3\5\3\5\3\5\3\5\3\6\3\6"+
		"\3\6\3\6\5\6\u0141\n\6\3\6\3\6\3\6\5\6\u0146\n\6\3\7\3\7\7\7\u014a\n\7"+
		"\f\7\16\7\u014d\13\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13"+
		"\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\5\16\u0165\n\16\3\16\3\16"+
		"\3\16\5\16\u016a\n\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0172\n\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\7\16\u017a\n\16\f\16\16\16\u017d\13\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3"+
		"\16\3\16\3\16\5\16\u0190\n\16\3\16\3\16\5\16\u0194\n\16\3\16\3\16\3\16"+
		"\3\16\5\16\u019a\n\16\3\16\5\16\u019d\n\16\3\16\5\16\u01a0\n\16\3\16\3"+
		"\16\3\16\3\16\3\16\5\16\u01a7\n\16\3\16\5\16\u01aa\n\16\3\16\3\16\5\16"+
		"\u01ae\n\16\3\16\5\16\u01b1\n\16\3\16\3\16\3\16\3\16\3\16\5\16\u01b8\n"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\7\16\u01c3\n\16\f\16"+
		"\16\16\u01c6\13\16\3\16\3\16\3\16\3\16\3\16\5\16\u01cd\n\16\3\16\5\16"+
		"\u01d0\n\16\3\16\3\16\5\16\u01d4\n\16\3\16\5\16\u01d7\n\16\3\16\3\16\3"+
		"\16\3\16\5\16\u01dd\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\5\16\u01e8\n\16\3\16\3\16\3\16\3\16\5\16\u01ee\n\16\3\16\3\16\3\16\5"+
		"\16\u01f3\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0233\n\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\5\16\u023c\n\16\3\16\3\16\5\16\u0240\n\16\3"+
		"\16\3\16\3\16\3\16\5\16\u0246\n\16\3\16\3\16\5\16\u024a\n\16\3\16\3\16"+
		"\3\16\5\16\u024f\n\16\3\16\3\16\3\16\3\16\5\16\u0255\n\16\3\16\3\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0261\n\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\5\16\u0269\n\16\3\16\3\16\3\16\3\16\5\16\u026f\n\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u027c\n\16"+
		"\3\16\6\16\u027f\n\16\r\16\16\16\u0280\3\16\3\16\3\16\3\16\3\16\3\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0291\n\16\3\16\3\16\3\16"+
		"\7\16\u0296\n\16\f\16\16\16\u0299\13\16\3\16\5\16\u029c\n\16\3\16\3\16"+
		"\3\16\3\16\5\16\u02a2\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\5\16\u02b1\n\16\3\16\3\16\5\16\u02b5\n\16\3\16\3"+
		"\16\3\16\3\16\5\16\u02bb\n\16\3\16\3\16\3\16\3\16\5\16\u02c1\n\16\3\16"+
		"\5\16\u02c4\n\16\3\16\5\16\u02c7\n\16\3\16\3\16\3\16\3\16\5\16\u02cd\n"+
		"\16\3\16\3\16\5\16\u02d1\n\16\3\16\3\16\3\16\3\16\3\16\3\16\7\16\u02d9"+
		"\n\16\f\16\16\16\u02dc\13\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u02e4"+
		"\n\16\3\16\5\16\u02e7\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u02f0"+
		"\n\16\3\16\3\16\3\16\5\16\u02f5\n\16\3\16\3\16\3\16\3\16\5\16\u02fb\n"+
		"\16\3\16\3\16\3\16\3\16\3\16\5\16\u0302\n\16\3\16\5\16\u0305\n\16\3\16"+
		"\3\16\3\16\3\16\5\16\u030b\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\7\16"+
		"\u0314\n\16\f\16\16\16\u0317\13\16\5\16\u0319\n\16\3\16\3\16\5\16\u031d"+
		"\n\16\3\16\3\16\3\16\5\16\u0322\n\16\3\16\3\16\3\16\5\16\u0327\n\16\3"+
		"\16\3\16\3\16\3\16\3\16\5\16\u032e\n\16\3\16\5\16\u0331\n\16\3\16\5\16"+
		"\u0334\n\16\3\16\3\16\3\16\3\16\3\16\5\16\u033b\n\16\3\16\3\16\3\16\5"+
		"\16\u0340\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0349\n\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\5\16\u0351\n\16\3\16\3\16\3\16\3\16\5\16\u0357"+
		"\n\16\3\16\5\16\u035a\n\16\3\16\5\16\u035d\n\16\3\16\3\16\3\16\3\16\5"+
		"\16\u0363\n\16\3\16\3\16\5\16\u0367\n\16\3\16\3\16\5\16\u036b\n\16\3\16"+
		"\3\16\5\16\u036f\n\16\5\16\u0371\n\16\3\16\3\16\3\16\3\16\3\16\3\16\5"+
		"\16\u0379\n\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0381\n\16\3\16\3\16"+
		"\3\16\3\16\5\16\u0387\n\16\3\16\3\16\3\16\3\16\5\16\u038d\n\16\3\16\5"+
		"\16\u0390\n\16\3\16\3\16\5\16\u0394\n\16\3\16\5\16\u0397\n\16\3\16\3\16"+
		"\5\16\u039b\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\7\16"+
		"\u03b5\n\16\f\16\16\16\u03b8\13\16\5\16\u03ba\n\16\3\16\3\16\5\16\u03be"+
		"\n\16\3\16\3\16\3\16\3\16\5\16\u03c4\n\16\3\16\5\16\u03c7\n\16\3\16\5"+
		"\16\u03ca\n\16\3\16\3\16\3\16\3\16\5\16\u03d0\n\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\5\16\u03d8\n\16\3\16\3\16\3\16\5\16\u03dd\n\16\3\16\3\16\3"+
		"\16\3\16\5\16\u03e3\n\16\3\16\3\16\3\16\3\16\5\16\u03e9\n\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\5\16\u03f1\n\16\3\16\3\16\3\16\7\16\u03f6\n\16\f"+
		"\16\16\16\u03f9\13\16\3\16\3\16\3\16\7\16\u03fe\n\16\f\16\16\16\u0401"+
		"\13\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\7\16"+
		"\u040f\n\16\f\16\16\16\u0412\13\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\7\16\u041d\n\16\f\16\16\16\u0420\13\16\5\16\u0422\n\16\3\16"+
		"\3\16\7\16\u0426\n\16\f\16\16\16\u0429\13\16\3\16\3\16\3\16\3\16\7\16"+
		"\u042f\n\16\f\16\16\16\u0432\13\16\3\16\3\16\3\16\3\16\7\16\u0438\n\16"+
		"\f\16\16\16\u043b\13\16\3\16\3\16\7\16\u043f\n\16\f\16\16\16\u0442\13"+
		"\16\5\16\u0444\n\16\3\17\3\17\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\5\21\u0450\n\21\3\21\3\21\5\21\u0454\n\21\3\21\3\21\3\21\3\21\3\21\5"+
		"\21\u045b\n\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\5\21\u04cf\n\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\5\21\u04d7\n\21\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u04df\n\21\3\21\3"+
		"\21\3\21\3\21\3\21\3\21\3\21\5\21\u04e8\n\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\5\21\u04f2\n\21\3\22\3\22\5\22\u04f6\n\22\3\22\5\22\u04f9"+
		"\n\22\3\22\3\22\3\22\3\22\5\22\u04ff\n\22\3\22\3\22\3\23\3\23\5\23\u0505"+
		"\n\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u0511\n\24"+
		"\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\5\25\u051d\n\25\3\25"+
		"\3\25\3\25\5\25\u0522\n\25\3\26\3\26\3\26\3\27\3\27\3\27\3\30\5\30\u052b"+
		"\n\30\3\30\3\30\3\30\3\31\3\31\3\31\5\31\u0533\n\31\3\31\3\31\3\31\3\31"+
		"\3\31\5\31\u053a\n\31\5\31\u053c\n\31\3\31\5\31\u053f\n\31\3\31\3\31\3"+
		"\31\5\31\u0544\n\31\3\31\3\31\5\31\u0548\n\31\3\31\3\31\3\31\5\31\u054d"+
		"\n\31\3\31\5\31\u0550\n\31\3\31\3\31\3\31\5\31\u0555\n\31\3\31\3\31\3"+
		"\31\5\31\u055a\n\31\3\31\5\31\u055d\n\31\3\31\3\31\3\31\5\31\u0562\n\31"+
		"\3\31\3\31\5\31\u0566\n\31\3\31\3\31\3\31\5\31\u056b\n\31\5\31\u056d\n"+
		"\31\3\32\3\32\5\32\u0571\n\32\3\33\3\33\3\33\3\33\3\33\7\33\u0578\n\33"+
		"\f\33\16\33\u057b\13\33\3\33\3\33\3\34\3\34\3\34\5\34\u0582\n\34\3\35"+
		"\3\35\3\36\3\36\3\36\3\36\3\36\5\36\u058b\n\36\3\37\3\37\3\37\7\37\u0590"+
		"\n\37\f\37\16\37\u0593\13\37\3 \3 \3 \3 \7 \u0599\n \f \16 \u059c\13 "+
		"\3!\3!\5!\u05a0\n!\3!\5!\u05a3\n!\3!\3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#"+
		"\3#\3#\3#\3#\3#\3#\3#\3#\3#\7#\u05b9\n#\f#\16#\u05bc\13#\3$\3$\3$\3$\7"+
		"$\u05c2\n$\f$\16$\u05c5\13$\3$\3$\3%\3%\5%\u05cb\n%\3%\5%\u05ce\n%\3&"+
		"\3&\3&\7&\u05d3\n&\f&\16&\u05d6\13&\3&\5&\u05d9\n&\3\'\3\'\3\'\3\'\5\'"+
		"\u05df\n\'\3(\3(\3(\3(\7(\u05e5\n(\f(\16(\u05e8\13(\3(\3(\3)\3)\3)\3)"+
		"\7)\u05f0\n)\f)\16)\u05f3\13)\3)\3)\3*\3*\3*\3*\3*\3*\5*\u05fd\n*\3+\3"+
		"+\3+\3+\3+\5+\u0604\n+\3,\3,\3,\3,\5,\u060a\n,\3-\3-\3-\3.\3.\3.\3.\3"+
		".\3.\6.\u0615\n.\r.\16.\u0616\3.\3.\3.\3.\3.\5.\u061e\n.\3.\3.\3.\3.\3"+
		".\5.\u0625\n.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\5.\u0631\n.\3.\3.\3.\3.\7"+
		".\u0637\n.\f.\16.\u063a\13.\3.\7.\u063d\n.\f.\16.\u0640\13.\5.\u0642\n"+
		".\3/\3/\3/\3/\3/\7/\u0649\n/\f/\16/\u064c\13/\5/\u064e\n/\3/\3/\3/\3/"+
		"\3/\7/\u0655\n/\f/\16/\u0658\13/\5/\u065a\n/\3/\3/\3/\3/\3/\7/\u0661\n"+
		"/\f/\16/\u0664\13/\5/\u0666\n/\3/\3/\3/\3/\3/\7/\u066d\n/\f/\16/\u0670"+
		"\13/\5/\u0672\n/\3/\5/\u0675\n/\3/\3/\3/\5/\u067a\n/\5/\u067c\n/\3\60"+
		"\3\60\3\60\3\61\3\61\3\61\3\61\3\61\3\61\3\61\5\61\u0688\n\61\3\61\3\61"+
		"\3\61\3\61\3\61\5\61\u068f\n\61\3\61\3\61\3\61\3\61\3\61\5\61\u0696\n"+
		"\61\3\61\7\61\u0699\n\61\f\61\16\61\u069c\13\61\3\62\3\62\3\62\3\62\3"+
		"\62\3\62\3\62\3\62\3\62\5\62\u06a7\n\62\3\63\3\63\5\63\u06ab\n\63\3\63"+
		"\3\63\5\63\u06af\n\63\3\64\3\64\6\64\u06b3\n\64\r\64\16\64\u06b4\3\65"+
		"\3\65\5\65\u06b9\n\65\3\65\3\65\3\65\3\65\7\65\u06bf\n\65\f\65\16\65\u06c2"+
		"\13\65\3\65\5\65\u06c5\n\65\3\65\5\65\u06c8\n\65\3\65\5\65\u06cb\n\65"+
		"\3\65\5\65\u06ce\n\65\3\65\3\65\5\65\u06d2\n\65\3\66\3\66\5\66\u06d6\n"+
		"\66\3\66\7\66\u06d9\n\66\f\66\16\66\u06dc\13\66\3\66\5\66\u06df\n\66\3"+
		"\66\5\66\u06e2\n\66\3\66\5\66\u06e5\n\66\3\66\5\66\u06e8\n\66\3\66\3\66"+
		"\5\66\u06ec\n\66\3\66\7\66\u06ef\n\66\f\66\16\66\u06f2\13\66\3\66\5\66"+
		"\u06f5\n\66\3\66\5\66\u06f8\n\66\3\66\5\66\u06fb\n\66\3\66\5\66\u06fe"+
		"\n\66\5\66\u0700\n\66\3\67\3\67\3\67\3\67\5\67\u0706\n\67\3\67\3\67\3"+
		"\67\3\67\3\67\5\67\u070d\n\67\3\67\3\67\3\67\5\67\u0712\n\67\3\67\5\67"+
		"\u0715\n\67\3\67\5\67\u0718\n\67\3\67\3\67\5\67\u071c\n\67\3\67\3\67\3"+
		"\67\3\67\3\67\3\67\3\67\3\67\5\67\u0726\n\67\3\67\3\67\5\67\u072a\n\67"+
		"\5\67\u072c\n\67\3\67\5\67\u072f\n\67\3\67\3\67\5\67\u0733\n\67\38\38"+
		"\78\u0737\n8\f8\168\u073a\138\38\58\u073d\n8\38\38\39\39\39\3:\3:\3:\3"+
		":\5:\u0748\n:\3:\3:\3:\3;\3;\3;\3;\3;\5;\u0752\n;\3;\3;\3;\3<\3<\3<\3"+
		"<\3<\3<\3<\5<\u075e\n<\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\7=\u076b\n=\f"+
		"=\16=\u076e\13=\3=\3=\5=\u0772\n=\3>\3>\3>\7>\u0777\n>\f>\16>\u077a\13"+
		">\3?\3?\3?\3?\3@\3@\3@\3A\3A\3A\3B\3B\3B\5B\u0789\nB\3B\7B\u078c\nB\f"+
		"B\16B\u078f\13B\3B\3B\3C\3C\3C\3C\3C\3C\7C\u0799\nC\fC\16C\u079c\13C\3"+
		"C\3C\5C\u07a0\nC\3D\3D\3D\3D\7D\u07a6\nD\fD\16D\u07a9\13D\3D\7D\u07ac"+
		"\nD\fD\16D\u07af\13D\3D\5D\u07b2\nD\3E\3E\3E\3E\3E\7E\u07b9\nE\fE\16E"+
		"\u07bc\13E\3E\3E\3E\3E\3E\7E\u07c3\nE\fE\16E\u07c6\13E\3E\3E\3E\3E\3E"+
		"\3E\3E\3E\3E\3E\7E\u07d2\nE\fE\16E\u07d5\13E\3E\3E\5E\u07d9\nE\5E\u07db"+
		"\nE\3F\3F\5F\u07df\nF\3G\3G\3G\3G\3G\7G\u07e6\nG\fG\16G\u07e9\13G\3G\3"+
		"G\3G\3G\3G\3G\3G\3G\7G\u07f3\nG\fG\16G\u07f6\13G\3G\3G\5G\u07fa\nG\3H"+
		"\3H\5H\u07fe\nH\3I\3I\3I\3I\7I\u0804\nI\fI\16I\u0807\13I\5I\u0809\nI\3"+
		"I\3I\5I\u080d\nI\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\7J\u0819\nJ\fJ\16J\u081c"+
		"\13J\3J\3J\3J\3K\3K\3K\3K\3K\7K\u0826\nK\fK\16K\u0829\13K\3K\3K\5K\u082d"+
		"\nK\3L\3L\5L\u0831\nL\3L\5L\u0834\nL\3M\3M\3M\5M\u0839\nM\3M\3M\3M\3M"+
		"\3M\7M\u0840\nM\fM\16M\u0843\13M\5M\u0845\nM\3M\3M\3M\5M\u084a\nM\3M\3"+
		"M\3M\7M\u084f\nM\fM\16M\u0852\13M\5M\u0854\nM\3N\3N\3O\5O\u0859\nO\3O"+
		"\3O\7O\u085d\nO\fO\16O\u0860\13O\3P\3P\3P\5P\u0865\nP\3P\3P\5P\u0869\n"+
		"P\3P\3P\3P\3P\5P\u086f\nP\3P\3P\5P\u0873\nP\3Q\5Q\u0876\nQ\3Q\3Q\3Q\5"+
		"Q\u087b\nQ\3Q\5Q\u087e\nQ\3Q\3Q\3Q\5Q\u0883\nQ\3Q\3Q\5Q\u0887\nQ\3Q\5"+
		"Q\u088a\nQ\3Q\5Q\u088d\nQ\3R\3R\3R\3R\5R\u0893\nR\3S\3S\3S\5S\u0898\n"+
		"S\3S\3S\3T\5T\u089d\nT\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3"+
		"T\5T\u08af\nT\5T\u08b1\nT\3T\5T\u08b4\nT\3U\3U\3U\3U\3V\3V\3V\7V\u08bd"+
		"\nV\fV\16V\u08c0\13V\3W\3W\3W\3W\7W\u08c6\nW\fW\16W\u08c9\13W\3W\3W\3"+
		"X\3X\5X\u08cf\nX\3Y\3Y\3Y\3Y\7Y\u08d5\nY\fY\16Y\u08d8\13Y\3Y\3Y\3Z\3Z"+
		"\5Z\u08de\nZ\3[\3[\5[\u08e2\n[\3[\3[\3[\3[\3[\3[\5[\u08ea\n[\3[\3[\3["+
		"\3[\3[\3[\5[\u08f2\n[\3[\3[\3[\3[\5[\u08f8\n[\3\\\3\\\3\\\3\\\7\\\u08fe"+
		"\n\\\f\\\16\\\u0901\13\\\3\\\3\\\3]\3]\3]\3]\3]\7]\u090a\n]\f]\16]\u090d"+
		"\13]\5]\u090f\n]\3]\3]\3]\3^\5^\u0915\n^\3^\3^\5^\u0919\n^\5^\u091b\n"+
		"^\3_\3_\3_\3_\3_\3_\3_\5_\u0924\n_\3_\3_\3_\3_\3_\3_\3_\3_\3_\3_\5_\u0930"+
		"\n_\5_\u0932\n_\3_\3_\3_\3_\3_\5_\u0939\n_\3_\3_\3_\3_\3_\5_\u0940\n_"+
		"\3_\3_\3_\3_\5_\u0946\n_\3_\3_\3_\3_\5_\u094c\n_\5_\u094e\n_\3`\3`\3`"+
		"\7`\u0953\n`\f`\16`\u0956\13`\3a\3a\3a\7a\u095b\na\fa\16a\u095e\13a\3"+
		"b\3b\3b\5b\u0963\nb\3b\3b\3c\3c\3c\5c\u096a\nc\3c\3c\3d\3d\5d\u0970\n"+
		"d\3d\3d\5d\u0974\nd\5d\u0976\nd\3e\3e\3e\7e\u097b\ne\fe\16e\u097e\13e"+
		"\3f\3f\3f\3f\7f\u0984\nf\ff\16f\u0987\13f\3f\3f\3g\3g\5g\u098d\ng\3h\3"+
		"h\3h\3h\3h\3h\7h\u0995\nh\fh\16h\u0998\13h\3h\3h\5h\u099c\nh\3i\3i\5i"+
		"\u09a0\ni\3j\3j\3k\3k\3k\7k\u09a7\nk\fk\16k\u09aa\13k\3l\3l\3l\3l\3l\3"+
		"l\3l\3l\3l\3l\5l\u09b6\nl\5l\u09b8\nl\3l\3l\3l\3l\3l\3l\7l\u09c0\nl\f"+
		"l\16l\u09c3\13l\3m\5m\u09c6\nm\3m\3m\3m\3m\3m\3m\5m\u09ce\nm\3m\3m\3m"+
		"\3m\3m\7m\u09d5\nm\fm\16m\u09d8\13m\3m\3m\3m\5m\u09dd\nm\3m\3m\3m\3m\3"+
		"m\3m\5m\u09e5\nm\3m\3m\3m\5m\u09ea\nm\3m\3m\3m\3m\3m\3m\3m\3m\7m\u09f4"+
		"\nm\fm\16m\u09f7\13m\3m\3m\5m\u09fb\nm\3m\5m\u09fe\nm\3m\3m\3m\3m\5m\u0a04"+
		"\nm\3m\3m\5m\u0a08\nm\3m\3m\3m\5m\u0a0d\nm\3m\3m\3m\5m\u0a12\nm\3m\3m"+
		"\3m\5m\u0a17\nm\3n\3n\3n\3n\5n\u0a1d\nn\3n\3n\3n\3n\3n\3n\3n\3n\3n\3n"+
		"\3n\3n\3n\3n\3n\3n\3n\3n\3n\7n\u0a32\nn\fn\16n\u0a35\13n\3o\3o\3o\3o\6"+
		"o\u0a3b\no\ro\16o\u0a3c\3o\3o\5o\u0a41\no\3o\3o\3o\3o\3o\6o\u0a48\no\r"+
		"o\16o\u0a49\3o\3o\5o\u0a4e\no\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3"+
		"o\7o\u0a5e\no\fo\16o\u0a61\13o\5o\u0a63\no\3o\3o\3o\3o\3o\3o\5o\u0a6b"+
		"\no\3o\3o\3o\3o\3o\3o\3o\5o\u0a74\no\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o"+
		"\3o\3o\3o\3o\3o\3o\3o\3o\6o\u0a89\no\ro\16o\u0a8a\3o\3o\3o\3o\3o\3o\3"+
		"o\3o\3o\5o\u0a96\no\3o\3o\3o\7o\u0a9b\no\fo\16o\u0a9e\13o\5o\u0aa0\no"+
		"\3o\3o\3o\3o\3o\3o\3o\5o\u0aa9\no\3o\3o\5o\u0aad\no\3o\3o\5o\u0ab1\no"+
		"\3o\3o\3o\3o\3o\3o\3o\3o\6o\u0abb\no\ro\16o\u0abc\3o\3o\3o\3o\3o\3o\3"+
		"o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\5o\u0ad6\no\3o\3o\3"+
		"o\3o\3o\5o\u0add\no\3o\5o\u0ae0\no\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3o\3"+
		"o\3o\5o\u0aef\no\3o\3o\5o\u0af3\no\3o\3o\3o\3o\3o\3o\3o\3o\7o\u0afd\n"+
		"o\fo\16o\u0b00\13o\3p\3p\3p\3p\3p\3p\3p\3p\6p\u0b0a\np\rp\16p\u0b0b\5"+
		"p\u0b0e\np\3q\3q\3r\3r\3s\3s\3t\3t\3u\3u\3u\5u\u0b1b\nu\3v\3v\5v\u0b1f"+
		"\nv\3w\3w\3w\6w\u0b24\nw\rw\16w\u0b25\3x\3x\3x\5x\u0b2b\nx\3y\3y\3y\3"+
		"y\3y\3z\5z\u0b33\nz\3z\3z\3{\3{\3{\5{\u0b3a\n{\3|\3|\3|\3|\3|\3|\3|\3"+
		"|\3|\3|\3|\3|\3|\3|\3|\5|\u0b4b\n|\3|\3|\5|\u0b4f\n|\3|\3|\3|\3|\5|\u0b55"+
		"\n|\3|\3|\3|\3|\5|\u0b5b\n|\3|\3|\3|\3|\3|\7|\u0b62\n|\f|\16|\u0b65\13"+
		"|\3|\5|\u0b68\n|\5|\u0b6a\n|\3}\3}\3}\7}\u0b6f\n}\f}\16}\u0b72\13}\3~"+
		"\3~\3~\3~\5~\u0b78\n~\3~\5~\u0b7b\n~\3~\5~\u0b7e\n~\3\177\3\177\3\177"+
		"\7\177\u0b83\n\177\f\177\16\177\u0b86\13\177\3\u0080\3\u0080\3\u0080\3"+
		"\u0080\5\u0080\u0b8c\n\u0080\3\u0080\5\u0080\u0b8f\n\u0080\3\u0081\3\u0081"+
		"\3\u0081\7\u0081\u0b94\n\u0081\f\u0081\16\u0081\u0b97\13\u0081\3\u0082"+
		"\3\u0082\5\u0082\u0b9b\n\u0082\3\u0082\3\u0082\3\u0082\5\u0082\u0ba0\n"+
		"\u0082\3\u0082\5\u0082\u0ba3\n\u0082\3\u0083\3\u0083\3\u0083\3\u0083\3"+
		"\u0083\3\u0084\3\u0084\3\u0084\3\u0084\7\u0084\u0bae\n\u0084\f\u0084\16"+
		"\u0084\u0bb1\13\u0084\3\u0085\3\u0085\3\u0085\3\u0085\3\u0086\3\u0086"+
		"\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086"+
		"\7\u0086\u0bc2\n\u0086\f\u0086\16\u0086\u0bc5\13\u0086\3\u0086\3\u0086"+
		"\3\u0086\3\u0086\3\u0086\7\u0086\u0bcc\n\u0086\f\u0086\16\u0086\u0bcf"+
		"\13\u0086\5\u0086\u0bd1\n\u0086\3\u0086\3\u0086\3\u0086\3\u0086\3\u0086"+
		"\7\u0086\u0bd8\n\u0086\f\u0086\16\u0086\u0bdb\13\u0086\5\u0086\u0bdd\n"+
		"\u0086\5\u0086\u0bdf\n\u0086\3\u0086\5\u0086\u0be2\n\u0086\3\u0086\5\u0086"+
		"\u0be5\n\u0086\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087"+
		"\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087\3\u0087"+
		"\5\u0087\u0bf7\n\u0087\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088\3\u0088"+
		"\3\u0088\5\u0088\u0c00\n\u0088\3\u0089\3\u0089\3\u0089\7\u0089\u0c05\n"+
		"\u0089\f\u0089\16\u0089\u0c08\13\u0089\3\u008a\3\u008a\3\u008a\3\u008a"+
		"\5\u008a\u0c0e\n\u008a\3\u008b\3\u008b\3\u008b\7\u008b\u0c13\n\u008b\f"+
		"\u008b\16\u008b\u0c16\13\u008b\3\u008c\3\u008c\3\u008c\3\u008d\3\u008d"+
		"\6\u008d\u0c1d\n\u008d\r\u008d\16\u008d\u0c1e\3\u008d\5\u008d\u0c22\n"+
		"\u008d\3\u008e\3\u008e\3\u008e\5\u008e\u0c27\n\u008e\3\u008f\3\u008f\3"+
		"\u008f\3\u008f\3\u008f\3\u008f\5\u008f\u0c2f\n\u008f\3\u0090\3\u0090\3"+
		"\u0091\3\u0091\5\u0091\u0c35\n\u0091\3\u0091\3\u0091\3\u0091\5\u0091\u0c3a"+
		"\n\u0091\3\u0091\3\u0091\3\u0091\5\u0091\u0c3f\n\u0091\3\u0091\3\u0091"+
		"\5\u0091\u0c43\n\u0091\3\u0091\3\u0091\5\u0091\u0c47\n\u0091\3\u0091\3"+
		"\u0091\5\u0091\u0c4b\n\u0091\3\u0091\3\u0091\5\u0091\u0c4f\n\u0091\3\u0091"+
		"\3\u0091\5\u0091\u0c53\n\u0091\3\u0091\3\u0091\5\u0091\u0c57\n\u0091\3"+
		"\u0091\3\u0091\5\u0091\u0c5b\n\u0091\3\u0091\5\u0091\u0c5e\n\u0091\3\u0092"+
		"\3\u0092\3\u0092\3\u0092\3\u0092\3\u0092\3\u0092\5\u0092\u0c67\n\u0092"+
		"\3\u0093\3\u0093\3\u0094\3\u0094\3\u0095\3\u0095\3\u0095\13\u03b6\u03f7"+
		"\u03ff\u0410\u041e\u0427\u0430\u0439\u0440\6`\u00d6\u00da\u00dc\u0096"+
		"\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFH"+
		"JLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c"+
		"\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4"+
		"\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc"+
		"\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4"+
		"\u00d6\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec"+
		"\u00ee\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104"+
		"\u0106\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a\u011c"+
		"\u011e\u0120\u0122\u0124\u0126\u0128\2\65\4\2EE\u00b9\u00b9\4\2$$\u00c8"+
		"\u00c8\4\2DD\u009b\u009b\4\2iivv\3\2/\60\4\2\u00ea\u00ea\u010a\u010a\4"+
		"\2\23\23\'\'\7\2,,88[[hh\u0092\u0092\3\2IJ\4\2[[hh\4\2\u009f\u009f\u0124"+
		"\u0124\5\2\20\20QQ\u00e9\u00e9\4\2\20\20\u008c\u008c\4\2\u008e\u008e\u0124"+
		"\u0124\5\2CC\u009a\u009a\u00d3\u00d3\6\2VV}}\u00db\u00db\u0100\u0100\5"+
		"\2VV\u00db\u00db\u0100\u0100\4\2\33\33II\4\2cc\u0084\u0084\4\2;;\u00cf"+
		"\u00cf\4\2\22\22NN\4\2\u0128\u0128\u012a\u012a\5\2\22\22\27\27\u00df\u00df"+
		"\5\2^^\u00f9\u00f9\u0102\u0102\4\2\u011a\u011b\u011f\u011f\4\2PP\u011c"+
		"\u011e\4\2\u011a\u011b\u0122\u0122\4\2==?@\4\2&&\u00fb\u00fb\4\2tt\u00c7"+
		"\u00c7\3\2\u00e7\u00e8\4\2\4\4ii\4\2\4\4ee\5\2\37\37\u0087\u0087\u00f4"+
		"\u00f4\3\2\u0112\u0119\4\2PP\u011a\u0123\6\2\25\25vv\u009e\u009e\u00a6"+
		"\u00a6\4\2^^\u00f9\u00f9\3\2\u011a\u011b\5\2\u0124\u0124\u0128\u0128\u012a"+
		"\u012a\4\2\u0098\u0098\u0110\u0110\6\2AArr\u0097\u0097\u00d2\u00d2\5\2"+
		"rr\u0097\u0097\u00d2\u00d2\4\2OO\u00af\u00af\4\2\u00a7\u00a7\u00e0\u00e0"+
		"\4\2dd\u00b6\u00b6\3\2\u0129\u012a\4\2QQ\u00da\u00da\63\2\20\21\23\24"+
		"\26\26\30\31\33\34\36\36 $\'\'),..\60\6688;<AMOQUUW]``bdghkmppruwxz|~"+
		"~\u0081\u0081\u0083\u0084\u0086\u0086\u0089\u009b\u009d\u009d\u00a0\u00a1"+
		"\u00a4\u00a5\u00a8\u00a8\u00aa\u00ab\u00ad\u00b6\u00b8\u00c0\u00c2\u00c9"+
		"\u00cb\u00d3\u00d5\u00d8\u00da\u00de\u00e0\u00e9\u00eb\u00ef\u00f3\u00f3"+
		"\u00f5\u00ff\u0103\u0106\u0109\u010b\u010e\u010e\u0110\u0111\22\2\26\26"+
		"::VVjjyy}}\u0082\u0082\u0085\u0085\u0088\u0088\u009c\u009c\u00a2\u00a2"+
		"\u00ca\u00ca\u00d5\u00d5\u00db\u00db\u0100\u0100\u0108\u0108\23\2\20\25"+
		"\279;UWikxz|~\u0081\u0083\u0084\u0086\u0087\u0089\u009b\u009d\u00a1\u00a3"+
		"\u00c9\u00cb\u00d4\u00d6\u00da\u00dc\u00ff\u0101\u0107\u0109\u0111\2\u0e57"+
		"\2\u012a\3\2\2\2\4\u012c\3\2\2\2\6\u012e\3\2\2\2\b\u0138\3\2\2\2\n\u0145"+
		"\3\2\2\2\f\u0147\3\2\2\2\16\u0150\3\2\2\2\20\u0153\3\2\2\2\22\u0156\3"+
		"\2\2\2\24\u0159\3\2\2\2\26\u015c\3\2\2\2\30\u015f\3\2\2\2\32\u0443\3\2"+
		"\2\2\34\u0445\3\2\2\2\36\u0447\3\2\2\2 \u04f1\3\2\2\2\"\u04f3\3\2\2\2"+
		"$\u0504\3\2\2\2&\u050a\3\2\2\2(\u0516\3\2\2\2*\u0523\3\2\2\2,\u0526\3"+
		"\2\2\2.\u052a\3\2\2\2\60\u056c\3\2\2\2\62\u056e\3\2\2\2\64\u0572\3\2\2"+
		"\2\66\u057e\3\2\2\28\u0583\3\2\2\2:\u058a\3\2\2\2<\u058c\3\2\2\2>\u0594"+
		"\3\2\2\2@\u059d\3\2\2\2B\u05a8\3\2\2\2D\u05ba\3\2\2\2F\u05bd\3\2\2\2H"+
		"\u05c8\3\2\2\2J\u05d8\3\2\2\2L\u05de\3\2\2\2N\u05e0\3\2\2\2P\u05eb\3\2"+
		"\2\2R\u05fc\3\2\2\2T\u0603\3\2\2\2V\u0605\3\2\2\2X\u060b\3\2\2\2Z\u0641"+
		"\3\2\2\2\\\u064d\3\2\2\2^\u067d\3\2\2\2`\u0680\3\2\2\2b\u06a6\3\2\2\2"+
		"d\u06a8\3\2\2\2f\u06b0\3\2\2\2h\u06d1\3\2\2\2j\u06ff\3\2\2\2l\u0714\3"+
		"\2\2\2n\u0734\3\2\2\2p\u0740\3\2\2\2r\u0743\3\2\2\2t\u074c\3\2\2\2v\u075d"+
		"\3\2\2\2x\u0771\3\2\2\2z\u0773\3\2\2\2|\u077b\3\2\2\2~\u077f\3\2\2\2\u0080"+
		"\u0782\3\2\2\2\u0082\u0785\3\2\2\2\u0084\u079f\3\2\2\2\u0086\u07a1\3\2"+
		"\2\2\u0088\u07da\3\2\2\2\u008a\u07de\3\2\2\2\u008c\u07f9\3\2\2\2\u008e"+
		"\u07fd\3\2\2\2\u0090\u080c\3\2\2\2\u0092\u080e\3\2\2\2\u0094\u082c\3\2"+
		"\2\2\u0096\u082e\3\2\2\2\u0098\u0835\3\2\2\2\u009a\u0855\3\2\2\2\u009c"+
		"\u0858\3\2\2\2\u009e\u0872\3\2\2\2\u00a0\u088c\3\2\2\2\u00a2\u0892\3\2"+
		"\2\2\u00a4\u0894\3\2\2\2\u00a6\u08b3\3\2\2\2\u00a8\u08b5\3\2\2\2\u00aa"+
		"\u08b9\3\2\2\2\u00ac\u08c1\3\2\2\2\u00ae\u08cc\3\2\2\2\u00b0\u08d0\3\2"+
		"\2\2\u00b2\u08db\3\2\2\2\u00b4\u08f7\3\2\2\2\u00b6\u08f9\3\2\2\2\u00b8"+
		"\u0904\3\2\2\2\u00ba\u091a\3\2\2\2\u00bc\u094d\3\2\2\2\u00be\u094f\3\2"+
		"\2\2\u00c0\u0957\3\2\2\2\u00c2\u0962\3\2\2\2\u00c4\u0969\3\2\2\2\u00c6"+
		"\u096d\3\2\2\2\u00c8\u0977\3\2\2\2\u00ca\u097f\3\2\2\2\u00cc\u098c\3\2"+
		"\2\2\u00ce\u099b\3\2\2\2\u00d0\u099f\3\2\2\2\u00d2\u09a1\3\2\2\2\u00d4"+
		"\u09a3\3\2\2\2\u00d6\u09b7\3\2\2\2\u00d8\u0a16\3\2\2\2\u00da\u0a1c\3\2"+
		"\2\2\u00dc\u0af2\3\2\2\2\u00de\u0b0d\3\2\2\2\u00e0\u0b0f\3\2\2\2\u00e2"+
		"\u0b11\3\2\2\2\u00e4\u0b13\3\2\2\2\u00e6\u0b15\3\2\2\2\u00e8\u0b17\3\2"+
		"\2\2\u00ea\u0b1c\3\2\2\2\u00ec\u0b23\3\2\2\2\u00ee\u0b27\3\2\2\2\u00f0"+
		"\u0b2c\3\2\2\2\u00f2\u0b32\3\2\2\2\u00f4\u0b39\3\2\2\2\u00f6\u0b69\3\2"+
		"\2\2\u00f8\u0b6b\3\2\2\2\u00fa\u0b73\3\2\2\2\u00fc\u0b7f\3\2\2\2\u00fe"+
		"\u0b87\3\2\2\2\u0100\u0b90\3\2\2\2\u0102\u0b98\3\2\2\2\u0104\u0ba4\3\2"+
		"\2\2\u0106\u0ba9\3\2\2\2\u0108\u0bb2\3\2\2\2\u010a\u0be4\3\2\2\2\u010c"+
		"\u0bf6\3\2\2\2\u010e\u0bff\3\2\2\2\u0110\u0c01\3\2\2\2\u0112\u0c0d\3\2"+
		"\2\2\u0114\u0c0f\3\2\2\2\u0116\u0c17\3\2\2\2\u0118\u0c21\3\2\2\2\u011a"+
		"\u0c26\3\2\2\2\u011c\u0c2e\3\2\2\2\u011e\u0c30\3\2\2\2\u0120\u0c5d\3\2"+
		"\2\2\u0122\u0c66\3\2\2\2\u0124\u0c68\3\2\2\2\u0126\u0c6a\3\2\2\2\u0128"+
		"\u0c6c\3\2\2\2\u012a\u012b\5\4\3\2\u012b\3\3\2\2\2\u012c\u012d\5\6\4\2"+
		"\u012d\5\3\2\2\2\u012e\u012f\5\"\22\2\u012f\u0130\5\n\6\2\u0130\u0131"+
		"\5B\"\2\u0131\u0136\5D#\2\u0132\u0134\7\32\2\2\u0133\u0132\3\2\2\2\u0133"+
		"\u0134\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0137\5.\30\2\u0136\u0133\3\2"+
		"\2\2\u0136\u0137\3\2\2\2\u0137\7\3\2\2\2\u0138\u0139\7\u00b7\2\2\u0139"+
		"\u013a\7\16\2\2\u013a\u013b\5\u00a8U\2\u013b\t\3\2\2\2\u013c\u013d\7\3"+
		"\2\2\u013d\u0140\5\u00fc\177\2\u013e\u013f\7\4\2\2\u013f\u0141\5\b\5\2"+
		"\u0140\u013e\3\2\2\2\u0140\u0141\3\2\2\2\u0141\u0142\3\2\2\2\u0142\u0143"+
		"\7\5\2\2\u0143\u0146\3\2\2\2\u0144\u0146\5\b\5\2\u0145\u013c\3\2\2\2\u0145"+
		"\u0144\3\2\2\2\u0146\13\3\2\2\2\u0147\u014b\5\32\16\2\u0148\u014a\7\6"+
		"\2\2\u0149\u0148\3\2\2\2\u014a\u014d\3\2\2\2\u014b\u0149\3\2\2\2\u014b"+
		"\u014c\3\2\2\2\u014c\u014e\3\2\2\2\u014d\u014b\3\2\2\2\u014e\u014f\7\2"+
		"\2\3\u014f\r\3\2\2\2\u0150\u0151\5\u00c6d\2\u0151\u0152\7\2\2\3\u0152"+
		"\17\3\2\2\2\u0153\u0154\5\u00c2b\2\u0154\u0155\7\2\2\3\u0155\21\3\2\2"+
		"\2\u0156\u0157\5\u00c0a\2\u0157\u0158\7\2\2\3\u0158\23\3\2\2\2\u0159\u015a"+
		"\5\u00c4c\2\u015a\u015b\7\2\2\3\u015b\25\3\2\2\2\u015c\u015d\5\u00f6|"+
		"\2\u015d\u015e\7\2\2\3\u015e\27\3\2\2\2\u015f\u0160\5\u00fc\177\2\u0160"+
		"\u0161\7\2\2\3\u0161\31\3\2\2\2\u0162\u0444\5.\30\2\u0163\u0165\5> \2"+
		"\u0164\u0163\3\2\2\2\u0164\u0165\3\2\2\2\u0165\u0166\3\2\2\2\u0166\u0444"+
		"\5Z.\2\u0167\u0169\7\u0106\2\2\u0168\u016a\7\u009a\2\2\u0169\u0168\3\2"+
		"\2\2\u0169\u016a\3\2\2\2\u016a\u016b\3\2\2\2\u016b\u0444\5\u00c0a\2\u016c"+
		"\u016d\79\2\2\u016d\u0171\58\35\2\u016e\u016f\7s\2\2\u016f\u0170\7\u009e"+
		"\2\2\u0170\u0172\7X\2\2\u0171\u016e\3\2\2\2\u0171\u0172\3\2\2\2\u0172"+
		"\u0173\3\2\2\2\u0173\u017b\5\u00c0a\2\u0174\u017a\5,\27\2\u0175\u017a"+
		"\5*\26\2\u0176\u0177\7\u010f\2\2\u0177\u0178\t\2\2\2\u0178\u017a\5F$\2"+
		"\u0179\u0174\3\2\2\2\u0179\u0175\3\2\2\2\u0179\u0176\3\2\2\2\u017a\u017d"+
		"\3\2\2\2\u017b\u0179\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u0444\3\2\2\2\u017d"+
		"\u017b\3\2\2\2\u017e\u017f\7\23\2\2\u017f\u0180\58\35\2\u0180\u0181\5"+
		"\u00c0a\2\u0181\u0182\7\u00da\2\2\u0182\u0183\t\2\2\2\u0183\u0184\5F$"+
		"\2\u0184\u0444\3\2\2\2\u0185\u0186\7\23\2\2\u0186\u0187\58\35\2\u0187"+
		"\u0188\5\u00c0a\2\u0188\u0189\7\u00da\2\2\u0189\u018a\5*\26\2\u018a\u0444"+
		"\3\2\2\2\u018b\u018c\7Q\2\2\u018c\u018f\58\35\2\u018d\u018e\7s\2\2\u018e"+
		"\u0190\7X\2\2\u018f\u018d\3\2\2\2\u018f\u0190\3\2\2\2\u0190\u0191\3\2"+
		"\2\2\u0191\u0193\5\u00c0a\2\u0192\u0194\t\3\2\2\u0193\u0192\3\2\2\2\u0193"+
		"\u0194\3\2\2\2\u0194\u0444\3\2\2\2\u0195\u0196\7\u00dd\2\2\u0196\u0199"+
		"\t\4\2\2\u0197\u0198\t\5\2\2\u0198\u019a\5\u00c0a\2\u0199\u0197\3\2\2"+
		"\2\u0199\u019a\3\2\2\2\u019a\u019f\3\2\2\2\u019b\u019d\7\u0089\2\2\u019c"+
		"\u019b\3\2\2\2\u019c\u019d\3\2\2\2\u019d\u019e\3\2\2\2\u019e\u01a0\7\u0124"+
		"\2\2\u019f\u019c\3\2\2\2\u019f\u01a0\3\2\2\2\u01a0\u0444\3\2\2\2\u01a1"+
		"\u01a6\5\"\22\2\u01a2\u01a3\7\3\2\2\u01a3\u01a4\5\u00fc\177\2\u01a4\u01a5"+
		"\7\5\2\2\u01a5\u01a7\3\2\2\2\u01a6\u01a2\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7"+
		"\u01a9\3\2\2\2\u01a8\u01aa\5B\"\2\u01a9\u01a8\3\2\2\2\u01a9\u01aa\3\2"+
		"\2\2\u01aa\u01ab\3\2\2\2\u01ab\u01b0\5D#\2\u01ac\u01ae\7\32\2\2\u01ad"+
		"\u01ac\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01af\3\2\2\2\u01af\u01b1\5."+
		"\30\2\u01b0\u01ad\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b1\u0444\3\2\2\2\u01b2"+
		"\u01b3\79\2\2\u01b3\u01b7\7\u00ea\2\2\u01b4\u01b5\7s\2\2\u01b5\u01b6\7"+
		"\u009e\2\2\u01b6\u01b8\7X\2\2\u01b7\u01b4\3\2\2\2\u01b7\u01b8\3\2\2\2"+
		"\u01b8\u01b9\3\2\2\2\u01b9\u01ba\5\u00c2b\2\u01ba\u01bb\7\u0089\2\2\u01bb"+
		"\u01c4\5\u00c2b\2\u01bc\u01c3\5B\"\2\u01bd\u01c3\5\u00bc_\2\u01be\u01c3"+
		"\5R*\2\u01bf\u01c3\5*\26\2\u01c0\u01c1\7\u00ed\2\2\u01c1\u01c3\5F$\2\u01c2"+
		"\u01bc\3\2\2\2\u01c2\u01bd\3\2\2\2\u01c2\u01be\3\2\2\2\u01c2\u01bf\3\2"+
		"\2\2\u01c2\u01c0\3\2\2\2\u01c3\u01c6\3\2\2\2\u01c4\u01c2\3\2\2\2\u01c4"+
		"\u01c5\3\2\2\2\u01c5\u0444\3\2\2\2\u01c6\u01c4\3\2\2\2\u01c7\u01cc\5$"+
		"\23\2\u01c8\u01c9\7\3\2\2\u01c9\u01ca\5\u00fc\177\2\u01ca\u01cb\7\5\2"+
		"\2\u01cb\u01cd\3\2\2\2\u01cc\u01c8\3\2\2\2\u01cc\u01cd\3\2\2\2\u01cd\u01cf"+
		"\3\2\2\2\u01ce\u01d0\5B\"\2\u01cf\u01ce\3\2\2\2\u01cf\u01d0\3\2\2\2\u01d0"+
		"\u01d1\3\2\2\2\u01d1\u01d6\5D#\2\u01d2\u01d4\7\32\2\2\u01d3\u01d2\3\2"+
		"\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d5\u01d7\5.\30\2\u01d6"+
		"\u01d3\3\2\2\2\u01d6\u01d7\3\2\2\2\u01d7\u0444\3\2\2\2\u01d8\u01d9\7\24"+
		"\2\2\u01d9\u01da\7\u00ea\2\2\u01da\u01dc\5\u00c0a\2\u01db\u01dd\5\64\33"+
		"\2\u01dc\u01db\3\2\2\2\u01dc\u01dd\3\2\2\2\u01dd\u01de\3\2\2\2\u01de\u01df"+
		"\7\65\2\2\u01df\u01e7\7\u00e3\2\2\u01e0\u01e8\5\u011a\u008e\2\u01e1\u01e2"+
		"\7e\2\2\u01e2\u01e3\7\60\2\2\u01e3\u01e8\5\u00aaV\2\u01e4\u01e5\7e\2\2"+
		"\u01e5\u01e6\7\22\2\2\u01e6\u01e8\7\60\2\2\u01e7\u01e0\3\2\2\2\u01e7\u01e1"+
		"\3\2\2\2\u01e7\u01e4\3\2\2\2\u01e7\u01e8\3\2\2\2\u01e8\u0444\3\2\2\2\u01e9"+
		"\u01ea\7\24\2\2\u01ea\u01ed\7\u00eb\2\2\u01eb\u01ec\t\5\2\2\u01ec\u01ee"+
		"\5\u00c0a\2\u01ed\u01eb\3\2\2\2\u01ed\u01ee\3\2\2\2\u01ee\u01ef\3\2\2"+
		"\2\u01ef\u01f0\7\65\2\2\u01f0\u01f2\7\u00e3\2\2\u01f1\u01f3\5\u011a\u008e"+
		"\2\u01f2\u01f1\3\2\2\2\u01f2\u01f3\3\2\2\2\u01f3\u0444\3\2\2\2\u01f4\u01f5"+
		"\7\23\2\2\u01f5\u01f6\7\u00ea\2\2\u01f6\u01f7\5\u00c0a\2\u01f7\u01f8\7"+
		"\20\2\2\u01f8\u01f9\t\6\2\2\u01f9\u01fa\5\u00f8}\2\u01fa\u0444\3\2\2\2"+
		"\u01fb\u01fc\7\23\2\2\u01fc\u01fd\7\u00ea\2\2\u01fd\u01fe\5\u00c0a\2\u01fe"+
		"\u01ff\7\20\2\2\u01ff\u0200\t\6\2\2\u0200\u0201\7\3\2\2\u0201\u0202\5"+
		"\u00f8}\2\u0202\u0203\7\5\2\2\u0203\u0444\3\2\2\2\u0204\u0205\7\23\2\2"+
		"\u0205\u0206\7\u00ea\2\2\u0206\u0207\5\u00c0a\2\u0207\u0208\7\u00c3\2"+
		"\2\u0208\u0209\7/\2\2\u0209\u020a\5\u00c0a\2\u020a\u020b\7\u00f2\2\2\u020b"+
		"\u020c\5\u0116\u008c\2\u020c\u0444\3\2\2\2\u020d\u020e\7\23\2\2\u020e"+
		"\u020f\7\u00ea\2\2\u020f\u0210\5\u00c0a\2\u0210\u0211\7Q\2\2\u0211\u0212"+
		"\t\6\2\2\u0212\u0213\7\3\2\2\u0213\u0214\5\u00be`\2\u0214\u0215\7\5\2"+
		"\2\u0215\u0444\3\2\2\2\u0216\u0217\7\23\2\2\u0217\u0218\7\u00ea\2\2\u0218"+
		"\u0219\5\u00c0a\2\u0219\u021a\7Q\2\2\u021a\u021b\t\6\2\2\u021b\u021c\5"+
		"\u00be`\2\u021c\u0444\3\2\2\2\u021d\u021e\7\23\2\2\u021e\u021f\t\7\2\2"+
		"\u021f\u0220\5\u00c0a\2\u0220\u0221\7\u00c3\2\2\u0221\u0222\7\u00f2\2"+
		"\2\u0222\u0223\5\u00c0a\2\u0223\u0444\3\2\2\2\u0224\u0225\7\23\2\2\u0225"+
		"\u0226\t\7\2\2\u0226\u0227\5\u00c0a\2\u0227\u0228\7\u00da\2\2\u0228\u0229"+
		"\7\u00ed\2\2\u0229\u022a\5F$\2\u022a\u0444\3\2\2\2\u022b\u022c\7\23\2"+
		"\2\u022c\u022d\t\7\2\2\u022d\u022e\5\u00c0a\2\u022e\u022f\7\u0104\2\2"+
		"\u022f\u0232\7\u00ed\2\2\u0230\u0231\7s\2\2\u0231\u0233\7X\2\2\u0232\u0230"+
		"\3\2\2\2\u0232\u0233\3\2\2\2\u0233\u0234\3\2\2\2\u0234\u0235\5F$\2\u0235"+
		"\u0444\3\2\2\2\u0236\u0237\7\23\2\2\u0237\u0238\7\u00ea\2\2\u0238\u0239"+
		"\5\u00c0a\2\u0239\u023b\t\b\2\2\u023a\u023c\7/\2\2\u023b\u023a\3\2\2\2"+
		"\u023b\u023c\3\2\2\2\u023c\u023d\3\2\2\2\u023d\u023f\5\u00c0a\2\u023e"+
		"\u0240\5\u0122\u0092\2\u023f\u023e\3\2\2\2\u023f\u0240\3\2\2\2\u0240\u0444"+
		"\3\2\2\2\u0241\u0242\7\23\2\2\u0242\u0243\7\u00ea\2\2\u0243\u0245\5\u00c0"+
		"a\2\u0244\u0246\5\64\33\2\u0245\u0244\3\2\2\2\u0245\u0246\3\2\2\2\u0246"+
		"\u0247\3\2\2\2\u0247\u0249\7\'\2\2\u0248\u024a\7/\2\2\u0249\u0248\3\2"+
		"\2\2\u0249\u024a\3\2\2\2\u024a\u024b\3\2\2\2\u024b\u024c\5\u00c0a\2\u024c"+
		"\u024e\5\u00fe\u0080\2\u024d\u024f\5\u00f4{\2\u024e\u024d\3\2\2\2\u024e"+
		"\u024f\3\2\2\2\u024f\u0444\3\2\2\2\u0250\u0251\7\23\2\2\u0251\u0252\7"+
		"\u00ea\2\2\u0252\u0254\5\u00c0a\2\u0253\u0255\5\64\33\2\u0254\u0253\3"+
		"\2\2\2\u0254\u0255\3\2\2\2\u0255\u0256\3\2\2\2\u0256\u0257\7\u00c5\2\2"+
		"\u0257\u0258\7\60\2\2\u0258\u0259\7\3\2\2\u0259\u025a\5\u00f8}\2\u025a"+
		"\u025b\7\5\2\2\u025b\u0444\3\2\2\2\u025c\u025d\7\23\2\2\u025d\u025e\7"+
		"\u00ea\2\2\u025e\u0260\5\u00c0a\2\u025f\u0261\5\64\33\2\u0260\u025f\3"+
		"\2\2\2\u0260\u0261\3\2\2\2\u0261\u0262\3\2\2\2\u0262\u0263\7\u00da\2\2"+
		"\u0263\u0264\7\u00d7\2\2\u0264\u0268\7\u0124\2\2\u0265\u0266\7\u010f\2"+
		"\2\u0266\u0267\7\u00d8\2\2\u0267\u0269\5F$\2\u0268\u0265\3\2\2\2\u0268"+
		"\u0269\3\2\2\2\u0269\u0444\3\2\2\2\u026a\u026b\7\23\2\2\u026b\u026c\7"+
		"\u00ea\2\2\u026c\u026e\5\u00c0a\2\u026d\u026f\5\64\33\2\u026e\u026d\3"+
		"\2\2\2\u026e\u026f\3\2\2\2\u026f\u0270\3\2\2\2\u0270\u0271\7\u00da\2\2"+
		"\u0271\u0272\7\u00d8\2\2\u0272\u0273\5F$\2\u0273\u0444\3\2\2\2\u0274\u0275"+
		"\7\23\2\2\u0275\u0276\t\7\2\2\u0276\u0277\5\u00c0a\2\u0277\u027b\7\20"+
		"\2\2\u0278\u0279\7s\2\2\u0279\u027a\7\u009e\2\2\u027a\u027c\7X\2\2\u027b"+
		"\u0278\3\2\2\2\u027b\u027c\3\2\2\2\u027c\u027e\3\2\2\2\u027d\u027f\5\62"+
		"\32\2\u027e\u027d\3\2\2\2\u027f\u0280\3\2\2\2\u0280\u027e\3\2\2\2\u0280"+
		"\u0281\3\2\2\2\u0281\u0444\3\2\2\2\u0282\u0283\7\23\2\2\u0283\u0284\7"+
		"\u00ea\2\2\u0284\u0285\5\u00c0a\2\u0285\u0286\5\64\33\2\u0286\u0287\7"+
		"\u00c3\2\2\u0287\u0288\7\u00f2\2\2\u0288\u0289\5\64\33\2\u0289\u0444\3"+
		"\2\2\2\u028a\u028b\7\23\2\2\u028b\u028c\t\7\2\2\u028c\u028d\5\u00c0a\2"+
		"\u028d\u0290\7Q\2\2\u028e\u028f\7s\2\2\u028f\u0291\7X\2\2\u0290\u028e"+
		"\3\2\2\2\u0290\u0291\3\2\2\2\u0291\u0292\3\2\2\2\u0292\u0297\5\64\33\2"+
		"\u0293\u0294\7\4\2\2\u0294\u0296\5\64\33\2\u0295\u0293\3\2\2\2\u0296\u0299"+
		"\3\2\2\2\u0297\u0295\3\2\2\2\u0297\u0298\3\2\2\2\u0298\u029b\3\2\2\2\u0299"+
		"\u0297\3\2\2\2\u029a\u029c\7\u00ba\2\2\u029b\u029a\3\2\2\2\u029b\u029c"+
		"\3\2\2\2\u029c\u0444\3\2\2\2\u029d\u029e\7\23\2\2\u029e\u029f\7\u00ea"+
		"\2\2\u029f\u02a1\5\u00c0a\2\u02a0\u02a2\5\64\33\2\u02a1\u02a0\3\2\2\2"+
		"\u02a1\u02a2\3\2\2\2\u02a2\u02a3\3\2\2\2\u02a3\u02a4\7\u00da\2\2\u02a4"+
		"\u02a5\5*\26\2\u02a5\u0444\3\2\2\2\u02a6\u02a7\7\23\2\2\u02a7\u02a8\7"+
		"\u00ea\2\2\u02a8\u02a9\5\u00c0a\2\u02a9\u02aa\7\u00bf\2\2\u02aa\u02ab"+
		"\7\u00b1\2\2\u02ab\u0444\3\2\2\2\u02ac\u02ad\7Q\2\2\u02ad\u02b0\7\u00ea"+
		"\2\2\u02ae\u02af\7s\2\2\u02af\u02b1\7X\2\2\u02b0\u02ae\3\2\2\2\u02b0\u02b1"+
		"\3\2\2\2\u02b1\u02b2\3\2\2\2\u02b2\u02b4\5\u00c0a\2\u02b3\u02b5\7\u00ba"+
		"\2\2\u02b4\u02b3\3\2\2\2\u02b4\u02b5\3\2\2\2\u02b5\u0444\3\2\2\2\u02b6"+
		"\u02b7\7Q\2\2\u02b7\u02ba\7\u010a\2\2\u02b8\u02b9\7s\2\2\u02b9\u02bb\7"+
		"X\2\2\u02ba\u02b8\3\2\2\2\u02ba\u02bb\3\2\2\2\u02bb\u02bc\3\2\2\2\u02bc"+
		"\u0444\5\u00c0a\2\u02bd\u02c0\79\2\2\u02be\u02bf\7\u00a6\2\2\u02bf\u02c1"+
		"\7\u00c5\2\2\u02c0\u02be\3\2\2\2\u02c0\u02c1\3\2\2\2\u02c1\u02c6\3\2\2"+
		"\2\u02c2\u02c4\7m\2\2\u02c3\u02c2\3\2\2\2\u02c3\u02c4\3\2\2\2\u02c4\u02c5"+
		"\3\2\2\2\u02c5\u02c7\7\u00ee\2\2\u02c6\u02c3\3\2\2\2\u02c6\u02c7\3\2\2"+
		"\2\u02c7\u02c8\3\2\2\2\u02c8\u02cc\7\u010a\2\2\u02c9\u02ca\7s\2\2\u02ca"+
		"\u02cb\7\u009e\2\2\u02cb\u02cd\7X\2\2\u02cc\u02c9\3\2\2\2\u02cc\u02cd"+
		"\3\2\2\2\u02cd\u02ce\3\2\2\2\u02ce\u02d0\5\u00c0a\2\u02cf\u02d1\5\u00b0"+
		"Y\2\u02d0\u02cf\3\2\2\2\u02d0\u02d1\3\2\2\2\u02d1\u02da\3\2\2\2\u02d2"+
		"\u02d9\5,\27\2\u02d3\u02d4\7\u00b0\2\2\u02d4\u02d5\7\u00a2\2\2\u02d5\u02d9"+
		"\5\u00a8U\2\u02d6\u02d7\7\u00ed\2\2\u02d7\u02d9\5F$\2\u02d8\u02d2\3\2"+
		"\2\2\u02d8\u02d3\3\2\2\2\u02d8\u02d6\3\2\2\2\u02d9\u02dc\3\2\2\2\u02da"+
		"\u02d8\3\2\2\2\u02da\u02db\3\2\2\2\u02db\u02dd\3\2\2\2\u02dc\u02da\3\2"+
		"\2\2\u02dd\u02de\7\32\2\2\u02de\u02df\5.\30\2\u02df\u0444\3\2\2\2\u02e0"+
		"\u02e3\79\2\2\u02e1\u02e2\7\u00a6\2\2\u02e2\u02e4\7\u00c5\2\2\u02e3\u02e1"+
		"\3\2\2\2\u02e3\u02e4\3\2\2\2\u02e4\u02e6\3\2\2\2\u02e5\u02e7\7m\2\2\u02e6"+
		"\u02e5\3\2\2\2\u02e6\u02e7\3\2\2\2\u02e7\u02e8\3\2\2\2\u02e8\u02e9\7\u00ee"+
		"\2\2\u02e9\u02ea\7\u010a\2\2\u02ea\u02ef\5\u00c2b\2\u02eb\u02ec\7\3\2"+
		"\2\u02ec\u02ed\5\u00fc\177\2\u02ed\u02ee\7\5\2\2\u02ee\u02f0\3\2\2\2\u02ef"+
		"\u02eb\3\2\2\2\u02ef\u02f0\3\2\2\2\u02f0\u02f1\3\2\2\2\u02f1\u02f4\5B"+
		"\"\2\u02f2\u02f3\7\u00a5\2\2\u02f3\u02f5\5F$\2\u02f4\u02f2\3\2\2\2\u02f4"+
		"\u02f5\3\2\2\2\u02f5\u0444\3\2\2\2\u02f6\u02f7\7\23\2\2\u02f7\u02f8\7"+
		"\u010a\2\2\u02f8\u02fa\5\u00c0a\2\u02f9\u02fb\7\32\2\2\u02fa\u02f9\3\2"+
		"\2\2\u02fa\u02fb\3\2\2\2\u02fb\u02fc\3\2\2\2\u02fc\u02fd\5.\30\2\u02fd"+
		"\u0444\3\2\2\2\u02fe\u0301\79\2\2\u02ff\u0300\7\u00a6\2\2\u0300\u0302"+
		"\7\u00c5\2\2\u0301\u02ff\3\2\2\2\u0301\u0302\3\2\2\2\u0302\u0304\3\2\2"+
		"\2\u0303\u0305\7\u00ee\2\2\u0304\u0303\3\2\2\2\u0304\u0305\3\2\2\2\u0305"+
		"\u0306\3\2\2\2\u0306\u030a\7k\2\2\u0307\u0308\7s\2\2\u0308\u0309\7\u009e"+
		"\2\2\u0309\u030b\7X\2\2\u030a\u0307\3\2\2\2\u030a\u030b\3\2\2\2\u030b"+
		"\u030c\3\2\2\2\u030c\u030d\5\u00c0a\2\u030d\u030e\7\32\2\2\u030e\u0318"+
		"\7\u0124\2\2\u030f\u0310\7\u0108\2\2\u0310\u0315\5X-\2\u0311\u0312\7\4"+
		"\2\2\u0312\u0314\5X-\2\u0313\u0311\3\2\2\2\u0314\u0317\3\2\2\2\u0315\u0313"+
		"\3\2\2\2\u0315\u0316\3\2\2\2\u0316\u0319\3\2\2\2\u0317\u0315\3\2\2\2\u0318"+
		"\u030f\3\2\2\2\u0318\u0319\3\2\2\2\u0319\u0444\3\2\2\2\u031a\u031c\7Q"+
		"\2\2\u031b\u031d\7\u00ee\2\2\u031c\u031b\3\2\2\2\u031c\u031d\3\2\2\2\u031d"+
		"\u031e\3\2\2\2\u031e\u0321\7k\2\2\u031f\u0320\7s\2\2\u0320\u0322\7X\2"+
		"\2\u0321\u031f\3\2\2\2\u0321\u0322\3\2\2\2\u0322\u0323\3\2\2\2\u0323\u0444"+
		"\5\u00c0a\2\u0324\u0326\7Y\2\2\u0325\u0327\t\t\2\2\u0326\u0325\3\2\2\2"+
		"\u0326\u0327\3\2\2\2\u0327\u0328\3\2\2\2\u0328\u0444\5\32\16\2\u0329\u032a"+
		"\7\u00dd\2\2\u032a\u032d\7\u00eb\2\2\u032b\u032c\t\5\2\2\u032c\u032e\5"+
		"\u00c0a\2\u032d\u032b\3\2\2\2\u032d\u032e\3\2\2\2\u032e\u0333\3\2\2\2"+
		"\u032f\u0331\7\u0089\2\2\u0330\u032f\3\2\2\2\u0330\u0331\3\2\2\2\u0331"+
		"\u0332\3\2\2\2\u0332\u0334\7\u0124\2\2\u0333\u0330\3\2\2\2\u0333\u0334"+
		"\3\2\2\2\u0334\u0444\3\2\2\2\u0335\u0336\7\u00dd\2\2\u0336\u0337\7\u00ea"+
		"\2\2\u0337\u033a\7[\2\2\u0338\u0339\t\5\2\2\u0339\u033b\5\u00c0a\2\u033a"+
		"\u0338\3\2\2\2\u033a\u033b\3\2\2\2\u033b\u033c\3\2\2\2\u033c\u033d\7\u0089"+
		"\2\2\u033d\u033f\7\u0124\2\2\u033e\u0340\5\64\33\2\u033f\u033e\3\2\2\2"+
		"\u033f\u0340\3\2\2\2\u0340\u0444\3\2\2\2\u0341\u0342\7\u00dd\2\2\u0342"+
		"\u0343\7\u00ed\2\2\u0343\u0348\5\u00c0a\2\u0344\u0345\7\3\2\2\u0345\u0346"+
		"\5J&\2\u0346\u0347\7\5\2\2\u0347\u0349\3\2\2\2\u0348\u0344\3\2\2\2\u0348"+
		"\u0349\3\2\2\2\u0349\u0444\3\2\2\2\u034a\u034b\7\u00dd\2\2\u034b\u034c"+
		"\7\60\2\2\u034c\u034d\t\5\2\2\u034d\u0350\5\u00c0a\2\u034e\u034f\t\5\2"+
		"\2\u034f\u0351\5\u00c0a\2\u0350\u034e\3\2\2\2\u0350\u0351\3\2\2\2\u0351"+
		"\u0444\3\2\2\2\u0352\u0353\7\u00dd\2\2\u0353\u0356\7\u010b\2\2\u0354\u0355"+
		"\t\5\2\2\u0355\u0357\5\u00c0a\2\u0356\u0354\3\2\2\2\u0356\u0357\3\2\2"+
		"\2\u0357\u035c\3\2\2\2\u0358\u035a\7\u0089\2\2\u0359\u0358\3\2\2\2\u0359"+
		"\u035a\3\2\2\2\u035a\u035b\3\2\2\2\u035b\u035d\7\u0124\2\2\u035c\u0359"+
		"\3\2\2\2\u035c\u035d\3\2\2\2\u035d\u0444\3\2\2\2\u035e\u035f\7\u00dd\2"+
		"\2\u035f\u0360\7\u00b1\2\2\u0360\u0362\5\u00c0a\2\u0361\u0363\5\64\33"+
		"\2\u0362\u0361\3\2\2\2\u0362\u0363\3\2\2\2\u0363\u0444\3\2\2\2\u0364\u0366"+
		"\7\u00dd\2\2\u0365\u0367\5\u011a\u008e\2\u0366\u0365\3\2\2\2\u0366\u0367"+
		"\3\2\2\2\u0367\u0368\3\2\2\2\u0368\u0370\7l\2\2\u0369\u036b\7\u0089\2"+
		"\2\u036a\u0369\3\2\2\2\u036a\u036b\3\2\2\2\u036b\u036e\3\2\2\2\u036c\u036f"+
		"\5\u00c0a\2\u036d\u036f\7\u0124\2\2\u036e\u036c\3\2\2\2\u036e\u036d\3"+
		"\2\2\2\u036f\u0371\3\2\2\2\u0370\u036a\3\2\2\2\u0370\u0371\3\2\2\2\u0371"+
		"\u0444\3\2\2\2\u0372\u0373\7\u00dd\2\2\u0373\u0374\79\2\2\u0374\u0375"+
		"\7\u00ea\2\2\u0375\u0378\5\u00c0a\2\u0376\u0377\7\32\2\2\u0377\u0379\7"+
		"\u00d7\2\2\u0378\u0376\3\2\2\2\u0378\u0379\3\2\2\2\u0379\u0444\3\2\2\2"+
		"\u037a\u037b\7\u00dd\2\2\u037b\u037c\7<\2\2\u037c\u0444\7\u009a\2\2\u037d"+
		"\u037e\t\n\2\2\u037e\u0380\7k\2\2\u037f\u0381\7[\2\2\u0380\u037f\3\2\2"+
		"\2\u0380\u0381\3\2\2\2\u0381\u0382\3\2\2\2\u0382\u0444\5:\36\2\u0383\u0384"+
		"\t\n\2\2\u0384\u0386\58\35\2\u0385\u0387\7[\2\2\u0386\u0385\3\2\2\2\u0386"+
		"\u0387\3\2\2\2\u0387\u0388\3\2\2\2\u0388\u0389\5\u00c0a\2\u0389\u0444"+
		"\3\2\2\2\u038a\u038c\t\n\2\2\u038b\u038d\7\u00ea\2\2\u038c\u038b\3\2\2"+
		"\2\u038c\u038d\3\2\2\2\u038d\u038f\3\2\2\2\u038e\u0390\t\13\2\2\u038f"+
		"\u038e\3\2\2\2\u038f\u0390\3\2\2\2\u0390\u0391\3\2\2\2\u0391\u0393\5\u00c0"+
		"a\2\u0392\u0394\5\64\33\2\u0393\u0392\3\2\2\2\u0393\u0394\3\2\2\2\u0394"+
		"\u0396\3\2\2\2\u0395\u0397\5<\37\2\u0396\u0395\3\2\2\2\u0396\u0397\3\2"+
		"\2\2\u0397\u0444\3\2\2\2\u0398\u039a\t\n\2\2\u0399\u039b\7\u00bb\2\2\u039a"+
		"\u0399\3\2\2\2\u039a\u039b\3\2\2\2\u039b\u039c\3\2\2\2\u039c\u0444\5."+
		"\30\2\u039d\u039e\7\61\2\2\u039e\u039f\7\u00a2\2\2\u039f\u03a0\58\35\2"+
		"\u03a0\u03a1\5\u00c0a\2\u03a1\u03a2\7\u0080\2\2\u03a2\u03a3\t\f\2\2\u03a3"+
		"\u0444\3\2\2\2\u03a4\u03a5\7\61\2\2\u03a5\u03a6\7\u00a2\2\2\u03a6\u03a7"+
		"\7\u00ea\2\2\u03a7\u03a8\5\u00c0a\2\u03a8\u03a9\7\u0080\2\2\u03a9\u03aa"+
		"\t\f\2\2\u03aa\u0444\3\2\2\2\u03ab\u03ac\7\u00c2\2\2\u03ac\u03ad\7\u00ea"+
		"\2\2\u03ad\u0444\5\u00c0a\2\u03ae\u03af\7\u00c2\2\2\u03af\u03b0\7k\2\2"+
		"\u03b0\u0444\5\u00c0a\2\u03b1\u03b9\7\u00c2\2\2\u03b2\u03ba\7\u0124\2"+
		"\2\u03b3\u03b5\13\2\2\2\u03b4\u03b3\3\2\2\2\u03b5\u03b8\3\2\2\2\u03b6"+
		"\u03b7\3\2\2\2\u03b6\u03b4\3\2\2\2\u03b7\u03ba\3\2\2\2\u03b8\u03b6\3\2"+
		"\2\2\u03b9\u03b2\3\2\2\2\u03b9\u03b6\3\2\2\2\u03ba\u0444\3\2\2\2\u03bb"+
		"\u03bd\7#\2\2\u03bc\u03be\7\u0086\2\2\u03bd\u03bc\3\2\2\2\u03bd\u03be"+
		"\3\2\2\2\u03be\u03bf\3\2\2\2\u03bf\u03c0\7\u00ea\2\2\u03c0\u03c3\5\u00c0"+
		"a\2\u03c1\u03c2\7\u00a5\2\2\u03c2\u03c4\5F$\2\u03c3\u03c1\3\2\2\2\u03c3"+
		"\u03c4\3\2\2\2\u03c4\u03c9\3\2\2\2\u03c5\u03c7\7\32\2\2\u03c6\u03c5\3"+
		"\2\2\2\u03c6\u03c7\3\2\2\2\u03c7\u03c8\3\2\2\2\u03c8\u03ca\5.\30\2\u03c9"+
		"\u03c6\3\2\2\2\u03c9\u03ca\3\2\2\2\u03ca\u0444\3\2\2\2\u03cb\u03cc\7\u00ff"+
		"\2\2\u03cc\u03cf\7\u00ea\2\2\u03cd\u03ce\7s\2\2\u03ce\u03d0\7X\2\2\u03cf"+
		"\u03cd\3\2\2\2\u03cf\u03d0\3\2\2\2\u03d0\u03d1\3\2\2\2\u03d1\u0444\5\u00c0"+
		"a\2\u03d2\u03d3\7)\2\2\u03d3\u0444\7#\2\2\u03d4\u03d5\7\u008d\2\2\u03d5"+
		"\u03d7\7B\2\2\u03d6\u03d8\7\u008e\2\2\u03d7\u03d6\3\2\2\2\u03d7\u03d8"+
		"\3\2\2\2\u03d8\u03d9\3\2\2\2\u03d9\u03da\7z\2\2\u03da\u03dc\7\u0124\2"+
		"\2\u03db\u03dd\7\u00ae\2\2\u03dc\u03db\3\2\2\2\u03dc\u03dd\3\2\2\2\u03dd"+
		"\u03de\3\2\2\2\u03de\u03df\7\177\2\2\u03df\u03e0\7\u00ea\2\2\u03e0\u03e2"+
		"\5\u00c0a\2\u03e1\u03e3\5\64\33\2\u03e2\u03e1\3\2\2\2\u03e2\u03e3\3\2"+
		"\2\2\u03e3\u0444\3\2\2\2\u03e4\u03e5\7\u00fa\2\2\u03e5\u03e6\7\u00ea\2"+
		"\2\u03e6\u03e8\5\u00c0a\2\u03e7\u03e9\5\64\33\2\u03e8\u03e7\3\2\2\2\u03e8"+
		"\u03e9\3\2\2\2\u03e9\u0444\3\2\2\2\u03ea\u03eb\7\u0099\2\2\u03eb\u03ec"+
		"\7\u00c4\2\2\u03ec\u03ed\7\u00ea\2\2\u03ed\u03f0\5\u00c0a\2\u03ee\u03ef"+
		"\t\r\2\2\u03ef\u03f1\7\u00b1\2\2\u03f0\u03ee\3\2\2\2\u03f0\u03f1\3\2\2"+
		"\2\u03f1\u0444\3\2\2\2\u03f2\u03f3\t\16\2\2\u03f3\u03f7\5\u011a\u008e"+
		"\2\u03f4\u03f6\13\2\2\2\u03f5\u03f4\3\2\2\2\u03f6\u03f9\3\2\2\2\u03f7"+
		"\u03f8\3\2\2\2\u03f7\u03f5\3\2\2\2\u03f8\u0444\3\2\2\2\u03f9\u03f7\3\2"+
		"\2\2\u03fa\u03fb\7\u00da\2\2\u03fb\u03ff\7\u00cc\2\2\u03fc\u03fe\13\2"+
		"\2\2\u03fd\u03fc\3\2\2\2\u03fe\u0401\3\2\2\2\u03ff\u0400\3\2\2\2\u03ff"+
		"\u03fd\3\2\2\2\u0400\u0444\3\2\2\2\u0401\u03ff\3\2\2\2\u0402\u0403\7\u00da"+
		"\2\2\u0403\u0404\7\u00f1\2\2\u0404\u0405\7\u0111\2\2\u0405\u0444\5\u00e8"+
		"u\2\u0406\u0407\7\u00da\2\2\u0407\u0408\7\u00f1\2\2\u0408\u0409\7\u0111"+
		"\2\2\u0409\u0444\t\17\2\2\u040a\u040b\7\u00da\2\2\u040b\u040c\7\u00f1"+
		"\2\2\u040c\u0410\7\u0111\2\2\u040d\u040f\13\2\2\2\u040e\u040d\3\2\2\2"+
		"\u040f\u0412\3\2\2\2\u0410\u0411\3\2\2\2\u0410\u040e\3\2\2\2\u0411\u0444"+
		"\3\2\2\2\u0412\u0410\3\2\2\2\u0413\u0414\7\u00da\2\2\u0414\u0415\5\34"+
		"\17\2\u0415\u0416\7\u0112\2\2\u0416\u0417\5\36\20\2\u0417\u0444\3\2\2"+
		"\2\u0418\u0419\7\u00da\2\2\u0419\u0421\5\34\17\2\u041a\u041e\7\u0112\2"+
		"\2\u041b\u041d\13\2\2\2\u041c\u041b\3\2\2\2\u041d\u0420\3\2\2\2\u041e"+
		"\u041f\3\2\2\2\u041e\u041c\3\2\2\2\u041f\u0422\3\2\2\2\u0420\u041e\3\2"+
		"\2\2\u0421\u041a\3\2\2\2\u0421\u0422\3\2\2\2\u0422\u0444\3\2\2\2\u0423"+
		"\u0427\7\u00da\2\2\u0424\u0426\13\2\2\2\u0425\u0424\3\2\2\2\u0426\u0429"+
		"\3\2\2\2\u0427\u0428\3\2\2\2\u0427\u0425\3\2\2\2\u0428\u042a\3\2\2\2\u0429"+
		"\u0427\3\2\2\2\u042a\u042b\7\u0112\2\2\u042b\u0444\5\36\20\2\u042c\u0430"+
		"\7\u00da\2\2\u042d\u042f\13\2\2\2\u042e\u042d\3\2\2\2\u042f\u0432\3\2"+
		"\2\2\u0430\u0431\3\2\2\2\u0430\u042e\3\2\2\2\u0431\u0444\3\2\2\2\u0432"+
		"\u0430\3\2\2\2\u0433\u0434\7\u00c6\2\2\u0434\u0444\5\34\17\2\u0435\u0439"+
		"\7\u00c6\2\2\u0436\u0438\13\2\2\2\u0437\u0436\3\2\2\2\u0438\u043b\3\2"+
		"\2\2\u0439\u043a\3\2\2\2\u0439\u0437\3\2\2\2\u043a\u0444\3\2\2\2\u043b"+
		"\u0439\3\2\2\2\u043c\u0440\5 \21\2\u043d\u043f\13\2\2\2\u043e\u043d\3"+
		"\2\2\2\u043f\u0442\3\2\2\2\u0440\u0441\3\2\2\2\u0440\u043e\3\2\2\2\u0441"+
		"\u0444\3\2\2\2\u0442\u0440\3\2\2\2\u0443\u0162\3\2\2\2\u0443\u0164\3\2"+
		"\2\2\u0443\u0167\3\2\2\2\u0443\u016c\3\2\2\2\u0443\u017e\3\2\2\2\u0443"+
		"\u0185\3\2\2\2\u0443\u018b\3\2\2\2\u0443\u0195\3\2\2\2\u0443\u01a1\3\2"+
		"\2\2\u0443\u01b2\3\2\2\2\u0443\u01c7\3\2\2\2\u0443\u01d8\3\2\2\2\u0443"+
		"\u01e9\3\2\2\2\u0443\u01f4\3\2\2\2\u0443\u01fb\3\2\2\2\u0443\u0204\3\2"+
		"\2\2\u0443\u020d\3\2\2\2\u0443\u0216\3\2\2\2\u0443\u021d\3\2\2\2\u0443"+
		"\u0224\3\2\2\2\u0443\u022b\3\2\2\2\u0443\u0236\3\2\2\2\u0443\u0241\3\2"+
		"\2\2\u0443\u0250\3\2\2\2\u0443\u025c\3\2\2\2\u0443\u026a\3\2\2\2\u0443"+
		"\u0274\3\2\2\2\u0443\u0282\3\2\2\2\u0443\u028a\3\2\2\2\u0443\u029d\3\2"+
		"\2\2\u0443\u02a6\3\2\2\2\u0443\u02ac\3\2\2\2\u0443\u02b6\3\2\2\2\u0443"+
		"\u02bd\3\2\2\2\u0443\u02e0\3\2\2\2\u0443\u02f6\3\2\2\2\u0443\u02fe\3\2"+
		"\2\2\u0443\u031a\3\2\2\2\u0443\u0324\3\2\2\2\u0443\u0329\3\2\2\2\u0443"+
		"\u0335\3\2\2\2\u0443\u0341\3\2\2\2\u0443\u034a\3\2\2\2\u0443\u0352\3\2"+
		"\2\2\u0443\u035e\3\2\2\2\u0443\u0364\3\2\2\2\u0443\u0372\3\2\2\2\u0443"+
		"\u037a\3\2\2\2\u0443\u037d\3\2\2\2\u0443\u0383\3\2\2\2\u0443\u038a\3\2"+
		"\2\2\u0443\u0398\3\2\2\2\u0443\u039d\3\2\2\2\u0443\u03a4\3\2\2\2\u0443"+
		"\u03ab\3\2\2\2\u0443\u03ae\3\2\2\2\u0443\u03b1\3\2\2\2\u0443\u03bb\3\2"+
		"\2\2\u0443\u03cb\3\2\2\2\u0443\u03d2\3\2\2\2\u0443\u03d4\3\2\2\2\u0443"+
		"\u03e4\3\2\2\2\u0443\u03ea\3\2\2\2\u0443\u03f2\3\2\2\2\u0443\u03fa\3\2"+
		"\2\2\u0443\u0402\3\2\2\2\u0443\u0406\3\2\2\2\u0443\u040a\3\2\2\2\u0443"+
		"\u0413\3\2\2\2\u0443\u0418\3\2\2\2\u0443\u0423\3\2\2\2\u0443\u042c\3\2"+
		"\2\2\u0443\u0433\3\2\2\2\u0443\u0435\3\2\2\2\u0443\u043c\3\2\2\2\u0444"+
		"\33\3\2\2\2\u0445\u0446\5\u011e\u0090\2\u0446\35\3\2\2\2\u0447\u0448\5"+
		"\u011e\u0090\2\u0448\37\3\2\2\2\u0449\u044a\79\2\2\u044a\u04f2\7\u00cc"+
		"\2\2\u044b\u044c\7Q\2\2\u044c\u04f2\7\u00cc\2\2\u044d\u044f\7n\2\2\u044e"+
		"\u0450\7\u00cc\2\2\u044f\u044e\3\2\2\2\u044f\u0450\3\2\2\2\u0450\u04f2"+
		"\3\2\2\2\u0451\u0453\7\u00c9\2\2\u0452\u0454\7\u00cc\2\2\u0453\u0452\3"+
		"\2\2\2\u0453\u0454\3\2\2\2\u0454\u04f2\3\2\2\2\u0455\u0456\7\u00dd\2\2"+
		"\u0456\u04f2\7n\2\2\u0457\u0458\7\u00dd\2\2\u0458\u045a\7\u00cc\2\2\u0459"+
		"\u045b\7n\2\2\u045a\u0459\3\2\2\2\u045a\u045b\3\2\2\2\u045b\u04f2\3\2"+
		"\2\2\u045c\u045d\7\u00dd\2\2\u045d\u04f2\7\u00b8\2\2\u045e\u045f\7\u00dd"+
		"\2\2\u045f\u04f2\7\u00cd\2\2\u0460\u0461\7\u00dd\2\2\u0461\u0462\7<\2"+
		"\2\u0462\u04f2\7\u00cd\2\2\u0463\u0464\7Z\2\2\u0464\u04f2\7\u00ea\2\2"+
		"\u0465\u0466\7u\2\2\u0466\u04f2\7\u00ea\2\2\u0467\u0468\7\u00dd\2\2\u0468"+
		"\u04f2\7\64\2\2\u0469\u046a\7\u00dd\2\2\u046a\u046b\79\2\2\u046b\u04f2"+
		"\7\u00ea\2\2\u046c\u046d\7\u00dd\2\2\u046d\u04f2\7\u00f6\2\2\u046e\u046f"+
		"\7\u00dd\2\2\u046f\u04f2\7x\2\2\u0470\u0471\7\u00dd\2\2\u0471\u04f2\7"+
		"\u0091\2\2\u0472\u0473\79\2\2\u0473\u04f2\7w\2\2\u0474\u0475\7Q\2\2\u0475"+
		"\u04f2\7w\2\2\u0476\u0477\7\23\2\2\u0477\u04f2\7w\2\2\u0478\u0479\7\u0090"+
		"\2\2\u0479\u04f2\7\u00ea\2\2\u047a\u047b\7\u0090\2\2\u047b\u04f2\7C\2"+
		"\2\u047c\u047d\7\u0103\2\2\u047d\u04f2\7\u00ea\2\2\u047e\u047f\7\u0103"+
		"\2\2\u047f\u04f2\7C\2\2\u0480\u0481\79\2\2\u0481\u0482\7\u00ee\2\2\u0482"+
		"\u04f2\7\u0093\2\2\u0483\u0484\7Q\2\2\u0484\u0485\7\u00ee\2\2\u0485\u04f2"+
		"\7\u0093\2\2\u0486\u0487\7\23\2\2\u0487\u0488\7\u00ea\2\2\u0488\u0489"+
		"\5\u00c2b\2\u0489\u048a\7\u009e\2\2\u048a\u048b\7+\2\2\u048b\u04f2\3\2"+
		"\2\2\u048c\u048d\7\23\2\2\u048d\u048e\7\u00ea\2\2\u048e\u048f\5\u00c2"+
		"b\2\u048f\u0490\7+\2\2\u0490\u0491\7\"\2\2\u0491\u04f2\3\2\2\2\u0492\u0493"+
		"\7\23\2\2\u0493\u0494\7\u00ea\2\2\u0494\u0495\5\u00c2b\2\u0495\u0496\7"+
		"\u009e\2\2\u0496\u0497\7\u00e1\2\2\u0497\u04f2\3\2\2\2\u0498\u0499\7\23"+
		"\2\2\u0499\u049a\7\u00ea\2\2\u049a\u049b\5\u00c2b\2\u049b\u049c\7\u00de"+
		"\2\2\u049c\u049d\7\"\2\2\u049d\u04f2\3\2\2\2\u049e\u049f\7\23\2\2\u049f"+
		"\u04a0\7\u00ea\2\2\u04a0\u04a1\5\u00c2b\2\u04a1\u04a2\7\u009e\2\2\u04a2"+
		"\u04a3\7\u00de\2\2\u04a3\u04f2\3\2\2\2\u04a4\u04a5\7\23\2\2\u04a5\u04a6"+
		"\7\u00ea\2\2\u04a6\u04a7\5\u00c2b\2\u04a7\u04a8\7\u009e\2\2\u04a8\u04a9"+
		"\7\u00e4\2\2\u04a9\u04aa\7\32\2\2\u04aa\u04ab\7L\2\2\u04ab\u04f2\3\2\2"+
		"\2\u04ac\u04ad\7\23\2\2\u04ad\u04ae\7\u00ea\2\2\u04ae\u04af\5\u00c2b\2"+
		"\u04af\u04b0\7\u00da\2\2\u04b0\u04b1\7\u00de\2\2\u04b1\u04b2\7\u008f\2"+
		"\2\u04b2\u04f2\3\2\2\2\u04b3\u04b4\7\23\2\2\u04b4\u04b5\7\u00ea\2\2\u04b5"+
		"\u04b6\5\u00c2b\2\u04b6\u04b7\7W\2\2\u04b7\u04b8\7\u00af\2\2\u04b8\u04f2"+
		"\3\2\2\2\u04b9\u04ba\7\23\2\2\u04ba\u04bb\7\u00ea\2\2\u04bb\u04bc\5\u00c2"+
		"b\2\u04bc\u04bd\7\30\2\2\u04bd\u04be\7\u00af\2\2\u04be\u04f2\3\2\2\2\u04bf"+
		"\u04c0\7\23\2\2\u04c0\u04c1\7\u00ea\2\2\u04c1\u04c2\5\u00c2b\2\u04c2\u04c3"+
		"\7\u00fd\2\2\u04c3\u04c4\7\u00af\2\2\u04c4\u04f2\3\2\2\2\u04c5\u04c6\7"+
		"\23\2\2\u04c6\u04c7\7\u00ea\2\2\u04c7\u04c8\5\u00c2b\2\u04c8\u04c9\7\u00f3"+
		"\2\2\u04c9\u04f2\3\2\2\2\u04ca\u04cb\7\23\2\2\u04cb\u04cc\7\u00ea\2\2"+
		"\u04cc\u04ce\5\u00c2b\2\u04cd\u04cf\5\64\33\2\u04ce\u04cd\3\2\2\2\u04ce"+
		"\u04cf\3\2\2\2\u04cf\u04d0\3\2\2\2\u04d0\u04d1\7\63\2\2\u04d1\u04f2\3"+
		"\2\2\2\u04d2\u04d3\7\23\2\2\u04d3\u04d4\7\u00ea\2\2\u04d4\u04d6\5\u00c2"+
		"b\2\u04d5\u04d7\5\64\33\2\u04d6\u04d5\3\2\2\2\u04d6\u04d7\3\2\2\2\u04d7"+
		"\u04d8\3\2\2\2\u04d8\u04d9\7\66\2\2\u04d9\u04f2\3\2\2\2\u04da\u04db\7"+
		"\23\2\2\u04db\u04dc\7\u00ea\2\2\u04dc\u04de\5\u00c2b\2\u04dd\u04df\5\64"+
		"\33\2\u04de\u04dd\3\2\2\2\u04de\u04df\3\2\2\2\u04df\u04e0\3\2\2\2\u04e0"+
		"\u04e1\7\u00da\2\2\u04e1\u04e2\7b\2\2\u04e2\u04f2\3\2\2\2\u04e3\u04e4"+
		"\7\23\2\2\u04e4\u04e5\7\u00ea\2\2\u04e5\u04e7\5\u00c2b\2\u04e6\u04e8\5"+
		"\64\33\2\u04e7\u04e6\3\2\2\2\u04e7\u04e8\3\2\2\2\u04e8\u04e9\3\2\2\2\u04e9"+
		"\u04ea\7\u00c5\2\2\u04ea\u04eb\7\60\2\2\u04eb\u04f2\3\2\2\2\u04ec\u04ed"+
		"\7\u00e2\2\2\u04ed\u04f2\7\u00f5\2\2\u04ee\u04f2\7\62\2\2\u04ef\u04f2"+
		"\7\u00ce\2\2\u04f0\u04f2\7K\2\2\u04f1\u0449\3\2\2\2\u04f1\u044b\3\2\2"+
		"\2\u04f1\u044d\3\2\2\2\u04f1\u0451\3\2\2\2\u04f1\u0455\3\2\2\2\u04f1\u0457"+
		"\3\2\2\2\u04f1\u045c\3\2\2\2\u04f1\u045e\3\2\2\2\u04f1\u0460\3\2\2\2\u04f1"+
		"\u0463\3\2\2\2\u04f1\u0465\3\2\2\2\u04f1\u0467\3\2\2\2\u04f1\u0469\3\2"+
		"\2\2\u04f1\u046c\3\2\2\2\u04f1\u046e\3\2\2\2\u04f1\u0470\3\2\2\2\u04f1"+
		"\u0472\3\2\2\2\u04f1\u0474\3\2\2\2\u04f1\u0476\3\2\2\2\u04f1\u0478\3\2"+
		"\2\2\u04f1\u047a\3\2\2\2\u04f1\u047c\3\2\2\2\u04f1\u047e\3\2\2\2\u04f1"+
		"\u0480\3\2\2\2\u04f1\u0483\3\2\2\2\u04f1\u0486\3\2\2\2\u04f1\u048c\3\2"+
		"\2\2\u04f1\u0492\3\2\2\2\u04f1\u0498\3\2\2\2\u04f1\u049e\3\2\2\2\u04f1"+
		"\u04a4\3\2\2\2\u04f1\u04ac\3\2\2\2\u04f1\u04b3\3\2\2\2\u04f1\u04b9\3\2"+
		"\2\2\u04f1\u04bf\3\2\2\2\u04f1\u04c5\3\2\2\2\u04f1\u04ca\3\2\2\2\u04f1"+
		"\u04d2\3\2\2\2\u04f1\u04da\3\2\2\2\u04f1\u04e3\3\2\2\2\u04f1\u04ec\3\2"+
		"\2\2\u04f1\u04ee\3\2\2\2\u04f1\u04ef\3\2\2\2\u04f1\u04f0\3\2\2\2\u04f2"+
		"!\3\2\2\2\u04f3\u04f5\79\2\2\u04f4\u04f6\7\u00ee\2\2\u04f5\u04f4\3\2\2"+
		"\2\u04f5\u04f6\3\2\2\2\u04f6\u04f8\3\2\2\2\u04f7\u04f9\7\\\2\2\u04f8\u04f7"+
		"\3\2\2\2\u04f8\u04f9\3\2\2\2\u04f9\u04fa\3\2\2\2\u04fa\u04fe\7\u00ea\2"+
		"\2\u04fb\u04fc\7s\2\2\u04fc\u04fd\7\u009e\2\2\u04fd\u04ff\7X\2\2\u04fe"+
		"\u04fb\3\2\2\2\u04fe\u04ff\3\2\2\2\u04ff\u0500\3\2\2\2\u0500\u0501\5\u00c0"+
		"a\2\u0501#\3\2\2\2\u0502\u0503\79\2\2\u0503\u0505\7\u00a6\2\2\u0504\u0502"+
		"\3\2\2\2\u0504\u0505\3\2\2\2\u0505\u0506\3\2\2\2\u0506\u0507\7\u00c5\2"+
		"\2\u0507\u0508\7\u00ea\2\2\u0508\u0509\5\u00c0a\2\u0509%\3\2\2\2\u050a"+
		"\u050b\7+\2\2\u050b\u050c\7\"\2\2\u050c\u0510\5\u00a8U\2\u050d\u050e\7"+
		"\u00e1\2\2\u050e\u050f\7\"\2\2\u050f\u0511\5\u00acW\2\u0510\u050d\3\2"+
		"\2\2\u0510\u0511\3\2\2\2\u0511\u0512\3\2\2\2\u0512\u0513\7\177\2\2\u0513"+
		"\u0514\7\u0128\2\2\u0514\u0515\7!\2\2\u0515\'\3\2\2\2\u0516\u0517\7\u00de"+
		"\2\2\u0517\u0518\7\"\2\2\u0518\u0519\5\u00a8U\2\u0519\u051c\7\u00a2\2"+
		"\2\u051a\u051d\5N(\2\u051b\u051d\5P)\2\u051c\u051a\3\2\2\2\u051c\u051b"+
		"\3\2\2\2\u051d\u0521\3\2\2\2\u051e\u051f\7\u00e4\2\2\u051f\u0520\7\32"+
		"\2\2\u0520\u0522\7L\2\2\u0521\u051e\3\2\2\2\u0521\u0522\3\2\2\2\u0522"+
		")\3\2\2\2\u0523\u0524\7\u008f\2\2\u0524\u0525\7\u0124\2\2\u0525+\3\2\2"+
		"\2\u0526\u0527\7\61\2\2\u0527\u0528\7\u0124\2\2\u0528-\3\2\2\2\u0529\u052b"+
		"\5> \2\u052a\u0529\3\2\2\2\u052a\u052b\3\2\2\2\u052b\u052c\3\2\2\2\u052c"+
		"\u052d\5`\61\2\u052d\u052e\5\\/\2\u052e/\3\2\2\2\u052f\u0530\7|\2\2\u0530"+
		"\u0532\7\u00ae\2\2\u0531\u0533\7\u00ea\2\2\u0532\u0531\3\2\2\2\u0532\u0533"+
		"\3\2\2\2\u0533\u0534\3\2\2\2\u0534\u053b\5\u00c0a\2\u0535\u0539\5\64\33"+
		"\2\u0536\u0537\7s\2\2\u0537\u0538\7\u009e\2\2\u0538\u053a\7X\2\2\u0539"+
		"\u0536\3\2\2\2\u0539\u053a\3\2\2\2\u053a\u053c\3\2\2\2\u053b\u0535\3\2"+
		"\2\2\u053b\u053c\3\2\2\2\u053c\u053e\3\2\2\2\u053d\u053f\5\u00a8U\2\u053e"+
		"\u053d\3\2\2\2\u053e\u053f\3\2\2\2\u053f\u056d\3\2\2\2\u0540\u0541\7|"+
		"\2\2\u0541\u0543\7\177\2\2\u0542\u0544\7\u00ea\2\2\u0543\u0542\3\2\2\2"+
		"\u0543\u0544\3\2\2\2\u0544\u0545\3\2\2\2\u0545\u0547\5\u00c0a\2\u0546"+
		"\u0548\5\64\33\2\u0547\u0546\3\2\2\2\u0547\u0548\3\2\2\2\u0548\u054c\3"+
		"\2\2\2\u0549\u054a\7s\2\2\u054a\u054b\7\u009e\2\2\u054b\u054d\7X\2\2\u054c"+
		"\u0549\3\2\2\2\u054c\u054d\3\2\2\2\u054d\u054f\3\2\2\2\u054e\u0550\5\u00a8"+
		"U\2\u054f\u054e\3\2\2\2\u054f\u0550\3\2\2\2\u0550\u056d\3\2\2\2\u0551"+
		"\u0552\7|\2\2\u0552\u0554\7\u00ae\2\2\u0553\u0555\7\u008e\2\2\u0554\u0553"+
		"\3\2\2\2\u0554\u0555\3\2\2\2\u0555\u0556\3\2\2\2\u0556\u0557\7M\2\2\u0557"+
		"\u0559\7\u0124\2\2\u0558\u055a\5\u00bc_\2\u0559\u0558\3\2\2\2\u0559\u055a"+
		"\3\2\2\2\u055a\u055c\3\2\2\2\u055b\u055d\5R*\2\u055c\u055b\3\2\2\2\u055c"+
		"\u055d\3\2\2\2\u055d\u056d\3\2\2\2\u055e\u055f\7|\2\2\u055f\u0561\7\u00ae"+
		"\2\2\u0560\u0562\7\u008e\2\2\u0561\u0560\3\2\2\2\u0561\u0562\3\2\2\2\u0562"+
		"\u0563\3\2\2\2\u0563\u0565\7M\2\2\u0564\u0566\7\u0124\2\2\u0565\u0564"+
		"\3\2\2\2\u0565\u0566\3\2\2\2\u0566\u0567\3\2\2\2\u0567\u056a\5B\"\2\u0568"+
		"\u0569\7\u00a5\2\2\u0569\u056b\5F$\2\u056a\u0568\3\2\2\2\u056a\u056b\3"+
		"\2\2\2\u056b\u056d\3\2\2\2\u056c\u052f\3\2\2\2\u056c\u0540\3\2\2\2\u056c"+
		"\u0551\3\2\2\2\u056c\u055e\3\2\2\2\u056d\61\3\2\2\2\u056e\u0570\5\64\33"+
		"\2\u056f\u0571\5*\26\2\u0570\u056f\3\2\2\2\u0570\u0571\3\2\2\2\u0571\63"+
		"\3\2\2\2\u0572\u0573\7\u00af\2\2\u0573\u0574\7\3\2\2\u0574\u0579\5\66"+
		"\34\2\u0575\u0576\7\4\2\2\u0576\u0578\5\66\34\2\u0577\u0575\3\2\2\2\u0578"+
		"\u057b\3\2\2\2\u0579\u0577\3\2\2\2\u0579\u057a\3\2\2\2\u057a\u057c\3\2"+
		"\2\2\u057b\u0579\3\2\2\2\u057c\u057d\7\5\2\2\u057d\65\3\2\2\2\u057e\u0581"+
		"\5\u011a\u008e\2\u057f\u0580\7\u0112\2\2\u0580\u0582\5\u00dep\2\u0581"+
		"\u057f\3\2\2\2\u0581\u0582\3\2\2\2\u0582\67\3\2\2\2\u0583\u0584\t\20\2"+
		"\2\u05849\3\2\2\2\u0585\u058b\5\u0114\u008b\2\u0586\u058b\7\u0124\2\2"+
		"\u0587\u058b\5\u00e0q\2\u0588\u058b\5\u00e2r\2\u0589\u058b\5\u00e4s\2"+
		"\u058a\u0585\3\2\2\2\u058a\u0586\3\2\2\2\u058a\u0587\3\2\2\2\u058a\u0588"+
		"\3\2\2\2\u058a\u0589\3\2\2\2\u058b;\3\2\2\2\u058c\u0591\5\u011a\u008e"+
		"\2\u058d\u058e\7\7\2\2\u058e\u0590\5\u011a\u008e\2\u058f\u058d\3\2\2\2"+
		"\u0590\u0593\3\2\2\2\u0591\u058f\3\2\2\2\u0591\u0592\3\2\2\2\u0592=\3"+
		"\2\2\2\u0593\u0591\3\2\2\2\u0594\u0595\7\u010f\2\2\u0595\u059a\5@!\2\u0596"+
		"\u0597\7\4\2\2\u0597\u0599\5@!\2\u0598\u0596\3\2\2\2\u0599\u059c\3\2\2"+
		"\2\u059a\u0598\3\2\2\2\u059a\u059b\3\2\2\2\u059b?\3\2\2\2\u059c\u059a"+
		"\3\2\2\2\u059d\u059f\5\u0116\u008c\2\u059e\u05a0\5\u00a8U\2\u059f\u059e"+
		"\3\2\2\2\u059f\u05a0\3\2\2\2\u05a0\u05a2\3\2\2\2\u05a1\u05a3\7\32\2\2"+
		"\u05a2\u05a1\3\2\2\2\u05a2\u05a3\3\2\2\2\u05a3\u05a4\3\2\2\2\u05a4\u05a5"+
		"\7\3\2\2\u05a5\u05a6\5.\30\2\u05a6\u05a7\7\5\2\2\u05a7A\3\2\2\2\u05a8"+
		"\u05a9\7\u0108\2\2\u05a9\u05aa\5\u00c0a\2\u05aaC\3\2\2\2\u05ab\u05ac\7"+
		"\u00a5\2\2\u05ac\u05b9\5F$\2\u05ad\u05ae\7\u00b0\2\2\u05ae\u05af\7\"\2"+
		"\2\u05af\u05b9\5\u00caf\2\u05b0\u05b9\5(\25\2\u05b1\u05b9\5&\24\2\u05b2"+
		"\u05b9\5\u00bc_\2\u05b3\u05b9\5R*\2\u05b4\u05b9\5*\26\2\u05b5\u05b9\5"+
		",\27\2\u05b6\u05b7\7\u00ed\2\2\u05b7\u05b9\5F$\2\u05b8\u05ab\3\2\2\2\u05b8"+
		"\u05ad\3\2\2\2\u05b8\u05b0\3\2\2\2\u05b8\u05b1\3\2\2\2\u05b8\u05b2\3\2"+
		"\2\2\u05b8\u05b3\3\2\2\2\u05b8\u05b4\3\2\2\2\u05b8\u05b5\3\2\2\2\u05b8"+
		"\u05b6\3\2\2\2\u05b9\u05bc\3\2\2\2\u05ba\u05b8\3\2\2\2\u05ba\u05bb\3\2"+
		"\2\2\u05bbE\3\2\2\2\u05bc\u05ba\3\2\2\2\u05bd\u05be\7\3\2\2\u05be\u05c3"+
		"\5H%\2\u05bf\u05c0\7\4\2\2\u05c0\u05c2\5H%\2\u05c1\u05bf\3\2\2\2\u05c2"+
		"\u05c5\3\2\2\2\u05c3\u05c1\3\2\2\2\u05c3\u05c4\3\2\2\2\u05c4\u05c6\3\2"+
		"\2\2\u05c5\u05c3\3\2\2\2\u05c6\u05c7\7\5\2\2\u05c7G\3\2\2\2\u05c8\u05cd"+
		"\5J&\2\u05c9\u05cb\7\u0112\2\2\u05ca\u05c9\3\2\2\2\u05ca\u05cb\3\2\2\2"+
		"\u05cb\u05cc\3\2\2\2\u05cc\u05ce\5L\'\2\u05cd\u05ca\3\2\2\2\u05cd\u05ce"+
		"\3\2\2\2\u05ceI\3\2\2\2\u05cf\u05d4\5\u011a\u008e\2\u05d0\u05d1\7\7\2"+
		"\2\u05d1\u05d3\5\u011a\u008e\2\u05d2\u05d0\3\2\2\2\u05d3\u05d6\3\2\2\2"+
		"\u05d4\u05d2\3\2\2\2\u05d4\u05d5\3\2\2\2\u05d5\u05d9\3\2\2\2\u05d6\u05d4"+
		"\3\2\2\2\u05d7\u05d9\7\u0124\2\2\u05d8\u05cf\3\2\2\2\u05d8\u05d7\3\2\2"+
		"\2\u05d9K\3\2\2\2\u05da\u05df\7\u0128\2\2\u05db\u05df\7\u012a\2\2\u05dc"+
		"\u05df\5\u00e6t\2\u05dd\u05df\7\u0124\2\2\u05de\u05da\3\2\2\2\u05de\u05db"+
		"\3\2\2\2\u05de\u05dc\3\2\2\2\u05de\u05dd\3\2\2\2\u05dfM\3\2\2\2\u05e0"+
		"\u05e1\7\3\2\2\u05e1\u05e6\5\u00dep\2\u05e2\u05e3\7\4\2\2\u05e3\u05e5"+
		"\5\u00dep\2\u05e4\u05e2\3\2\2\2\u05e5\u05e8\3\2\2\2\u05e6\u05e4\3\2\2"+
		"\2\u05e6\u05e7\3\2\2\2\u05e7\u05e9\3\2\2\2\u05e8\u05e6\3\2\2\2\u05e9\u05ea"+
		"\7\5\2\2\u05eaO\3\2\2\2\u05eb\u05ec\7\3\2\2\u05ec\u05f1\5N(\2\u05ed\u05ee"+
		"\7\4\2\2\u05ee\u05f0\5N(\2\u05ef\u05ed\3\2\2\2\u05f0\u05f3\3\2\2\2\u05f1"+
		"\u05ef\3\2\2\2\u05f1\u05f2\3\2\2\2\u05f2\u05f4\3\2\2\2\u05f3\u05f1\3\2"+
		"\2\2\u05f4\u05f5\7\5\2\2\u05f5Q\3\2\2\2\u05f6\u05f7\7\u00e4\2\2\u05f7"+
		"\u05f8\7\32\2\2\u05f8\u05fd\5T+\2\u05f9\u05fa\7\u00e4\2\2\u05fa\u05fb"+
		"\7\"\2\2\u05fb\u05fd\5V,\2\u05fc\u05f6\3\2\2\2\u05fc\u05f9\3\2\2\2\u05fd"+
		"S\3\2\2\2\u05fe\u05ff\7{\2\2\u05ff\u0600\7\u0124\2\2\u0600\u0601\7\u00aa"+
		"\2\2\u0601\u0604\7\u0124\2\2\u0602\u0604\5\u011a\u008e\2\u0603\u05fe\3"+
		"\2\2\2\u0603\u0602\3\2\2\2\u0604U\3\2\2\2\u0605\u0609\7\u0124\2\2\u0606"+
		"\u0607\7\u010f\2\2\u0607\u0608\7\u00d8\2\2\u0608\u060a\5F$\2\u0609\u0606"+
		"\3\2\2\2\u0609\u060a\3\2\2\2\u060aW\3\2\2\2\u060b\u060c\5\u011a\u008e"+
		"\2\u060c\u060d\7\u0124\2\2\u060dY\3\2\2\2\u060e\u060f\5\60\31\2\u060f"+
		"\u0610\5`\61\2\u0610\u0611\5\\/\2\u0611\u0642\3\2\2\2\u0612\u0614\5\u0086"+
		"D\2\u0613\u0615\5^\60\2\u0614\u0613\3\2\2\2\u0615\u0616\3\2\2\2\u0616"+
		"\u0614\3\2\2\2\u0616\u0617\3\2\2\2\u0617\u0642\3\2\2\2\u0618\u0619\7G"+
		"\2\2\u0619\u061a\7i\2\2\u061a\u061b\5\u00c0a\2\u061b\u061d\5\u00ba^\2"+
		"\u061c\u061e\5~@\2\u061d\u061c\3\2\2\2\u061d\u061e\3\2\2\2\u061e\u0642"+
		"\3\2\2\2\u061f\u0620\7\u0105\2\2\u0620\u0621\5\u00c0a\2\u0621\u0622\5"+
		"\u00ba^\2\u0622\u0624\5p9\2\u0623\u0625\5~@\2\u0624\u0623\3\2\2\2\u0624"+
		"\u0625\3\2\2\2\u0625\u0642\3\2\2\2\u0626\u0627\7\u0096\2\2\u0627\u0628"+
		"\7\177\2\2\u0628\u0629\5\u00c0a\2\u0629\u062a\5\u00ba^\2\u062a\u0630\7"+
		"\u0108\2\2\u062b\u0631\5\u00c0a\2\u062c\u062d\7\3\2\2\u062d\u062e\5.\30"+
		"\2\u062e\u062f\7\5\2\2\u062f\u0631\3\2\2\2\u0630\u062b\3\2\2\2\u0630\u062c"+
		"\3\2\2\2\u0631\u0632\3\2\2\2\u0632\u0633\5\u00ba^\2\u0633\u0634\7\u00a2"+
		"\2\2\u0634\u0638\5\u00d6l\2\u0635\u0637\5r:\2\u0636\u0635\3\2\2\2\u0637"+
		"\u063a\3\2\2\2\u0638\u0636\3\2\2\2\u0638\u0639\3\2\2\2\u0639\u063e\3\2"+
		"\2\2\u063a\u0638\3\2\2\2\u063b\u063d\5t;\2\u063c\u063b\3\2\2\2\u063d\u0640"+
		"\3\2\2\2\u063e\u063c\3\2\2\2\u063e\u063f\3\2\2\2\u063f\u0642\3\2\2\2\u0640"+
		"\u063e\3\2\2\2\u0641\u060e\3\2\2\2\u0641\u0612\3\2\2\2\u0641\u0618\3\2"+
		"\2\2\u0641\u061f\3\2\2\2\u0641\u0626\3\2\2\2\u0642[\3\2\2\2\u0643\u0644"+
		"\7\u00a7\2\2\u0644\u0645\7\"\2\2\u0645\u064a\5d\63\2\u0646\u0647\7\4\2"+
		"\2\u0647\u0649\5d\63\2\u0648\u0646\3\2\2\2\u0649\u064c\3\2\2\2\u064a\u0648"+
		"\3\2\2\2\u064a\u064b\3\2\2\2\u064b\u064e\3\2\2\2\u064c\u064a\3\2\2\2\u064d"+
		"\u0643\3\2\2\2\u064d\u064e\3\2\2\2\u064e\u0659\3\2\2\2\u064f\u0650\7*"+
		"\2\2\u0650\u0651\7\"\2\2\u0651\u0656\5\u00d2j\2\u0652\u0653\7\4\2\2\u0653"+
		"\u0655\5\u00d2j\2\u0654\u0652\3\2\2\2\u0655\u0658\3\2\2\2\u0656\u0654"+
		"\3\2\2\2\u0656\u0657\3\2\2\2\u0657\u065a\3\2\2\2\u0658\u0656\3\2\2\2\u0659"+
		"\u064f\3\2\2\2\u0659\u065a\3\2\2\2\u065a\u0665\3\2\2\2\u065b\u065c\7O"+
		"\2\2\u065c\u065d\7\"\2\2\u065d\u0662\5\u00d2j\2\u065e\u065f\7\4\2\2\u065f"+
		"\u0661\5\u00d2j\2\u0660\u065e\3\2\2\2\u0661\u0664\3\2\2\2\u0662\u0660"+
		"\3\2\2\2\u0662\u0663\3\2\2\2\u0663\u0666\3\2\2\2\u0664\u0662\3\2\2\2\u0665"+
		"\u065b\3\2\2\2\u0665\u0666\3\2\2\2\u0666\u0671\3\2\2\2\u0667\u0668\7\u00e0"+
		"\2\2\u0668\u0669\7\"\2\2\u0669\u066e\5d\63\2\u066a\u066b\7\4\2\2\u066b"+
		"\u066d\5d\63\2\u066c\u066a\3\2\2\2\u066d\u0670\3\2\2\2\u066e\u066c\3\2"+
		"\2\2\u066e\u066f\3\2\2\2\u066f\u0672\3\2\2\2\u0670\u066e\3\2\2\2\u0671"+
		"\u0667\3\2\2\2\u0671\u0672\3\2\2\2\u0672\u0674\3\2\2\2\u0673\u0675\5\u0106"+
		"\u0084\2\u0674\u0673\3\2\2\2\u0674\u0675\3\2\2\2\u0675\u067b\3\2\2\2\u0676"+
		"\u0679\7\u008a\2\2\u0677\u067a\7\22\2\2\u0678\u067a\5\u00d2j\2\u0679\u0677"+
		"\3\2\2\2\u0679\u0678\3\2\2\2\u067a\u067c\3\2\2\2\u067b\u0676\3\2\2\2\u067b"+
		"\u067c\3\2\2\2\u067c]\3\2\2\2\u067d\u067e\5\60\31\2\u067e\u067f\5h\65"+
		"\2\u067f_\3\2\2\2\u0680\u0681\b\61\1\2\u0681\u0682\5b\62\2\u0682\u069a"+
		"\3\2\2\2\u0683\u0684\f\5\2\2\u0684\u0685\6\61\3\2\u0685\u0687\t\21\2\2"+
		"\u0686\u0688\5\u009aN\2\u0687\u0686\3\2\2\2\u0687\u0688\3\2\2\2\u0688"+
		"\u0689\3\2\2\2\u0689\u0699\5`\61\6\u068a\u068b\f\4\2\2\u068b\u068c\6\61"+
		"\5\2\u068c\u068e\7}\2\2\u068d\u068f\5\u009aN\2\u068e\u068d\3\2\2\2\u068e"+
		"\u068f\3\2\2\2\u068f\u0690\3\2\2\2\u0690\u0699\5`\61\5\u0691\u0692\f\3"+
		"\2\2\u0692\u0693\6\61\7\2\u0693\u0695\t\22\2\2\u0694\u0696\5\u009aN\2"+
		"\u0695\u0694\3\2\2\2\u0695\u0696\3\2\2\2\u0696\u0697\3\2\2\2\u0697\u0699"+
		"\5`\61\4\u0698\u0683\3\2\2\2\u0698\u068a\3\2\2\2\u0698\u0691\3\2\2\2\u0699"+
		"\u069c\3\2\2\2\u069a\u0698\3\2\2\2\u069a\u069b\3\2\2\2\u069ba\3\2\2\2"+
		"\u069c\u069a\3\2\2\2\u069d\u06a7\5j\66\2\u069e\u06a7\5f\64\2\u069f\u06a0"+
		"\7\u00ea\2\2\u06a0\u06a7\5\u00c0a\2\u06a1\u06a7\5\u00b6\\\2\u06a2\u06a3"+
		"\7\3\2\2\u06a3\u06a4\5.\30\2\u06a4\u06a5\7\5\2\2\u06a5\u06a7\3\2\2\2\u06a6"+
		"\u069d\3\2\2\2\u06a6\u069e\3\2\2\2\u06a6\u069f\3\2\2\2\u06a6\u06a1\3\2"+
		"\2\2\u06a6\u06a2\3\2\2\2\u06a7c\3\2\2\2\u06a8\u06aa\5\u00d2j\2\u06a9\u06ab"+
		"\t\23\2\2\u06aa\u06a9\3\2\2\2\u06aa\u06ab\3\2\2\2\u06ab\u06ae\3\2\2\2"+
		"\u06ac\u06ad\7\u00a0\2\2\u06ad\u06af\t\24\2\2\u06ae\u06ac\3\2\2\2\u06ae"+
		"\u06af\3\2\2\2\u06afe\3\2\2\2\u06b0\u06b2\5\u0086D\2\u06b1\u06b3\5h\65"+
		"\2\u06b2\u06b1\3\2\2\2\u06b3\u06b4\3\2\2\2\u06b4\u06b2\3\2\2\2\u06b4\u06b5"+
		"\3\2\2\2\u06b5g\3\2\2\2\u06b6\u06b8\5l\67\2\u06b7\u06b9\5~@\2\u06b8\u06b7"+
		"\3\2\2\2\u06b8\u06b9\3\2\2\2\u06b9\u06ba\3\2\2\2\u06ba\u06bb\5\\/\2\u06bb"+
		"\u06d2\3\2\2\2\u06bc\u06c0\5n8\2\u06bd\u06bf\5\u0098M\2\u06be\u06bd\3"+
		"\2\2\2\u06bf\u06c2\3\2\2\2\u06c0\u06be\3\2\2\2\u06c0\u06c1\3\2\2\2\u06c1"+
		"\u06c4\3\2\2\2\u06c2\u06c0\3\2\2\2\u06c3\u06c5\5~@\2\u06c4\u06c3\3\2\2"+
		"\2\u06c4\u06c5\3\2\2\2\u06c5\u06c7\3\2\2\2\u06c6\u06c8\5\u0088E\2\u06c7"+
		"\u06c6\3\2\2\2\u06c7\u06c8\3\2\2\2\u06c8\u06ca\3\2\2\2\u06c9\u06cb\5\u0080"+
		"A\2\u06ca\u06c9\3\2\2\2\u06ca\u06cb\3\2\2\2\u06cb\u06cd\3\2\2\2\u06cc"+
		"\u06ce\5\u0106\u0084\2\u06cd\u06cc\3\2\2\2\u06cd\u06ce\3\2\2\2\u06ce\u06cf"+
		"\3\2\2\2\u06cf\u06d0\5\\/\2\u06d0\u06d2\3\2\2\2\u06d1\u06b6\3\2\2\2\u06d1"+
		"\u06bc\3\2\2\2\u06d2i\3\2\2\2\u06d3\u06d5\5l\67\2\u06d4\u06d6\5\u0086"+
		"D\2\u06d5\u06d4\3\2\2\2\u06d5\u06d6\3\2\2\2\u06d6\u06da\3\2\2\2\u06d7"+
		"\u06d9\5\u0098M\2\u06d8\u06d7\3\2\2\2\u06d9\u06dc\3\2\2\2\u06da\u06d8"+
		"\3\2\2\2\u06da\u06db\3\2\2\2\u06db\u06de\3\2\2\2\u06dc\u06da\3\2\2\2\u06dd"+
		"\u06df\5~@\2\u06de\u06dd\3\2\2\2\u06de\u06df\3\2\2\2\u06df\u06e1\3\2\2"+
		"\2\u06e0\u06e2\5\u0088E\2\u06e1\u06e0\3\2\2\2\u06e1\u06e2\3\2\2\2\u06e2"+
		"\u06e4\3\2\2\2\u06e3\u06e5\5\u0080A\2\u06e4\u06e3\3\2\2\2\u06e4\u06e5"+
		"\3\2\2\2\u06e5\u06e7\3\2\2\2\u06e6\u06e8\5\u0106\u0084\2\u06e7\u06e6\3"+
		"\2\2\2\u06e7\u06e8\3\2\2\2\u06e8\u0700\3\2\2\2\u06e9\u06eb\5n8\2\u06ea"+
		"\u06ec\5\u0086D\2\u06eb\u06ea\3\2\2\2\u06eb\u06ec\3\2\2\2\u06ec\u06f0"+
		"\3\2\2\2\u06ed\u06ef\5\u0098M\2\u06ee\u06ed\3\2\2\2\u06ef\u06f2\3\2\2"+
		"\2\u06f0\u06ee\3\2\2\2\u06f0\u06f1\3\2\2\2\u06f1\u06f4\3\2\2\2\u06f2\u06f0"+
		"\3\2\2\2\u06f3\u06f5\5~@\2\u06f4\u06f3\3\2\2\2\u06f4\u06f5\3\2\2\2\u06f5"+
		"\u06f7\3\2\2\2\u06f6\u06f8\5\u0088E\2\u06f7\u06f6\3\2\2\2\u06f7\u06f8"+
		"\3\2\2\2\u06f8\u06fa\3\2\2\2\u06f9\u06fb\5\u0080A\2\u06fa\u06f9\3\2\2"+
		"\2\u06fa\u06fb\3\2\2\2\u06fb\u06fd\3\2\2\2\u06fc\u06fe\5\u0106\u0084\2"+
		"\u06fd\u06fc\3\2\2\2\u06fd\u06fe\3\2\2\2\u06fe\u0700\3\2\2\2\u06ff\u06d3"+
		"\3\2\2\2\u06ff\u06e9\3\2\2\2\u0700k\3\2\2\2\u0701\u0702\7\u00d4\2\2\u0702"+
		"\u0703\7\u00f7\2\2\u0703\u0705\7\3\2\2\u0704\u0706\5\u009aN\2\u0705\u0704"+
		"\3\2\2\2\u0705\u0706\3\2\2\2\u0706\u0707\3\2\2\2\u0707\u0708\5\u00d4k"+
		"\2\u0708\u0709\7\5\2\2\u0709\u0715\3\2\2\2\u070a\u070c\7\u0094\2\2\u070b"+
		"\u070d\5\u009aN\2\u070c\u070b\3\2\2\2\u070c\u070d\3\2\2\2\u070d\u070e"+
		"\3\2\2\2\u070e\u0715\5\u00d4k\2\u070f\u0711\7\u00c0\2\2\u0710\u0712\5"+
		"\u009aN\2\u0711\u0710\3\2\2\2\u0711\u0712\3\2\2\2\u0712\u0713\3\2\2\2"+
		"\u0713\u0715\5\u00d4k\2\u0714\u0701\3\2\2\2\u0714\u070a\3\2\2\2\u0714"+
		"\u070f\3\2\2\2\u0715\u0717\3\2\2\2\u0716\u0718\5\u00bc_\2\u0717\u0716"+
		"\3\2\2\2\u0717\u0718\3\2\2\2\u0718\u071b\3\2\2\2\u0719\u071a\7\u00be\2"+
		"\2\u071a\u071c\7\u0124\2\2\u071b\u0719\3\2\2\2\u071b\u071c\3\2\2\2\u071c"+
		"\u071d\3\2\2\2\u071d\u071e\7\u0108\2\2\u071e\u072b\7\u0124\2\2\u071f\u0729"+
		"\7\32\2\2\u0720\u072a\5\u00aaV\2\u0721\u072a\5\u00fc\177\2\u0722\u0725"+
		"\7\3\2\2\u0723\u0726\5\u00aaV\2\u0724\u0726\5\u00fc\177\2\u0725\u0723"+
		"\3\2\2\2\u0725\u0724\3\2\2\2\u0726\u0727\3\2\2\2\u0727\u0728\7\5\2\2\u0728"+
		"\u072a\3\2\2\2\u0729\u0720\3\2\2\2\u0729\u0721\3\2\2\2\u0729\u0722\3\2"+
		"\2\2\u072a\u072c\3\2\2\2\u072b\u071f\3\2\2\2\u072b\u072c\3\2\2\2\u072c"+
		"\u072e\3\2\2\2\u072d\u072f\5\u00bc_\2\u072e\u072d\3\2\2\2\u072e\u072f"+
		"\3\2\2\2\u072f\u0732\3\2\2\2\u0730\u0731\7\u00bd\2\2\u0731\u0733\7\u0124"+
		"\2\2\u0732\u0730\3\2\2\2\u0732\u0733\3\2\2\2\u0733m\3\2\2\2\u0734\u0738"+
		"\7\u00d4\2\2\u0735\u0737\5\u0082B\2\u0736\u0735\3\2\2\2\u0737\u073a\3"+
		"\2\2\2\u0738\u0736\3\2\2\2\u0738\u0739\3\2\2\2\u0739\u073c\3\2\2\2\u073a"+
		"\u0738\3\2\2\2\u073b\u073d\5\u009aN\2\u073c\u073b\3\2\2\2\u073c\u073d"+
		"\3\2\2\2\u073d\u073e\3\2\2\2\u073e\u073f\5\u00c8e\2\u073fo\3\2\2\2\u0740"+
		"\u0741\7\u00da\2\2\u0741\u0742\5z>\2\u0742q\3\2\2\2\u0743\u0744\7\u010c"+
		"\2\2\u0744\u0747\7\u0095\2\2\u0745\u0746\7\25\2\2\u0746\u0748\5\u00d6"+
		"l\2\u0747\u0745\3\2\2\2\u0747\u0748\3\2\2\2\u0748\u0749\3\2\2\2\u0749"+
		"\u074a\7\u00f0\2\2\u074a\u074b\5v<\2\u074bs\3\2\2\2\u074c\u074d\7\u010c"+
		"\2\2\u074d\u074e\7\u009e\2\2\u074e\u0751\7\u0095\2\2\u074f\u0750\7\25"+
		"\2\2\u0750\u0752\5\u00d6l\2\u0751\u074f\3\2\2\2\u0751\u0752\3\2\2\2\u0752"+
		"\u0753\3\2\2\2\u0753\u0754\7\u00f0\2\2\u0754\u0755\5x=\2\u0755u\3\2\2"+
		"\2\u0756\u075e\7G\2\2\u0757\u0758\7\u0105\2\2\u0758\u0759\7\u00da\2\2"+
		"\u0759\u075e\7\u011c\2\2\u075a\u075b\7\u0105\2\2\u075b\u075c\7\u00da\2"+
		"\2\u075c\u075e\5z>\2\u075d\u0756\3\2\2\2\u075d\u0757\3\2\2\2\u075d\u075a"+
		"\3\2\2\2\u075ew\3\2\2\2\u075f\u0760\7|\2\2\u0760\u0772\7\u011c\2\2\u0761"+
		"\u0762\7|\2\2\u0762\u0763\7\3\2\2\u0763\u0764\5\u00be`\2\u0764\u0765\7"+
		"\5\2\2\u0765\u0766\7\u0109\2\2\u0766\u0767\7\3\2\2\u0767\u076c\5\u00d2"+
		"j\2\u0768\u0769\7\4\2\2\u0769\u076b\5\u00d2j\2\u076a\u0768\3\2\2\2\u076b"+
		"\u076e\3\2\2\2\u076c\u076a\3\2\2\2\u076c\u076d\3\2\2\2\u076d\u076f\3\2"+
		"\2\2\u076e\u076c\3\2\2\2\u076f\u0770\7\5\2\2\u0770\u0772\3\2\2\2\u0771"+
		"\u075f\3\2\2\2\u0771\u0761\3\2\2\2\u0772y\3\2\2\2\u0773\u0778\5|?\2\u0774"+
		"\u0775\7\4\2\2\u0775\u0777\5|?\2\u0776\u0774\3\2\2\2\u0777\u077a\3\2\2"+
		"\2\u0778\u0776\3\2\2\2\u0778\u0779\3\2\2\2\u0779{\3\2\2\2\u077a\u0778"+
		"\3\2\2\2\u077b\u077c\5\u00c0a\2\u077c\u077d\7\u0112\2\2\u077d\u077e\5"+
		"\u00d2j\2\u077e}\3\2\2\2\u077f\u0780\7\u010d\2\2\u0780\u0781\5\u00d6l"+
		"\2\u0781\177\3\2\2\2\u0782\u0783\7q\2\2\u0783\u0784\5\u00d6l\2\u0784\u0081"+
		"\3\2\2\2\u0785\u0786\7\b\2\2\u0786\u078d\5\u0084C\2\u0787\u0789\7\4\2"+
		"\2\u0788\u0787\3\2\2\2\u0788\u0789\3\2\2\2\u0789\u078a\3\2\2\2\u078a\u078c"+
		"\5\u0084C\2\u078b\u0788\3\2\2\2\u078c\u078f\3\2\2\2\u078d\u078b\3\2\2"+
		"\2\u078d\u078e\3\2\2\2\u078e\u0790\3\2\2\2\u078f\u078d\3\2\2\2\u0790\u0791"+
		"\7\t\2\2\u0791\u0083\3\2\2\2\u0792\u07a0\5\u011a\u008e\2\u0793\u0794\5"+
		"\u011a\u008e\2\u0794\u0795\7\3\2\2\u0795\u079a\5\u00dco\2\u0796\u0797"+
		"\7\4\2\2\u0797\u0799\5\u00dco\2\u0798\u0796\3\2\2\2\u0799\u079c\3\2\2"+
		"\2\u079a\u0798\3\2\2\2\u079a\u079b\3\2\2\2\u079b\u079d\3\2\2\2\u079c\u079a"+
		"\3\2\2\2\u079d\u079e\7\5\2\2\u079e\u07a0\3\2\2\2\u079f\u0792\3\2\2\2\u079f"+
		"\u0793\3\2\2\2\u07a0\u0085\3\2\2\2\u07a1\u07a2\7i\2\2\u07a2\u07a7\5\u009c"+
		"O\2\u07a3\u07a4\7\4\2\2\u07a4\u07a6\5\u009cO\2\u07a5\u07a3\3\2\2\2\u07a6"+
		"\u07a9\3\2\2\2\u07a7\u07a5\3\2\2\2\u07a7\u07a8\3\2\2\2\u07a8\u07ad\3\2"+
		"\2\2\u07a9\u07a7\3\2\2\2\u07aa\u07ac\5\u0098M\2\u07ab\u07aa\3\2\2\2\u07ac"+
		"\u07af\3\2\2\2\u07ad\u07ab\3\2\2\2\u07ad\u07ae\3\2\2\2\u07ae\u07b1\3\2"+
		"\2\2\u07af\u07ad\3\2\2\2\u07b0\u07b2\5\u0092J\2\u07b1\u07b0\3\2\2\2\u07b1"+
		"\u07b2\3\2\2\2\u07b2\u0087\3\2\2\2\u07b3\u07b4\7o\2\2\u07b4\u07b5\7\""+
		"\2\2\u07b5\u07ba\5\u008aF\2\u07b6\u07b7\7\4\2\2\u07b7\u07b9\5\u008aF\2"+
		"\u07b8\u07b6\3\2\2\2\u07b9\u07bc\3\2\2\2\u07ba\u07b8\3\2\2\2\u07ba\u07bb"+
		"\3\2\2\2\u07bb\u07db\3\2\2\2\u07bc\u07ba\3\2\2\2\u07bd\u07be\7o\2\2\u07be"+
		"\u07bf\7\"\2\2\u07bf\u07c4\5\u00d2j\2\u07c0\u07c1\7\4\2\2\u07c1\u07c3"+
		"\5\u00d2j\2\u07c2\u07c0\3\2\2\2\u07c3\u07c6\3\2\2\2\u07c4\u07c2\3\2\2"+
		"\2\u07c4\u07c5\3\2\2\2\u07c5\u07d8\3\2\2\2\u07c6\u07c4\3\2\2\2\u07c7\u07c8"+
		"\7\u010f\2\2\u07c8\u07d9\7\u00cf\2\2\u07c9\u07ca\7\u010f\2\2\u07ca\u07d9"+
		"\7;\2\2\u07cb\u07cc\7p\2\2\u07cc\u07cd\7\u00dc\2\2\u07cd\u07ce\7\3\2\2"+
		"\u07ce\u07d3\5\u0090I\2\u07cf\u07d0\7\4\2\2\u07d0\u07d2\5\u0090I\2\u07d1"+
		"\u07cf\3\2\2\2\u07d2\u07d5\3\2\2\2\u07d3\u07d1\3\2\2\2\u07d3\u07d4\3\2"+
		"\2\2\u07d4\u07d6\3\2\2\2\u07d5\u07d3\3\2\2\2\u07d6\u07d7\7\5\2\2\u07d7"+
		"\u07d9\3\2\2\2\u07d8\u07c7\3\2\2\2\u07d8\u07c9\3\2\2\2\u07d8\u07cb\3\2"+
		"\2\2\u07d8\u07d9\3\2\2\2\u07d9\u07db\3\2\2\2\u07da\u07b3\3\2\2\2\u07da"+
		"\u07bd\3\2\2\2\u07db\u0089\3\2\2\2\u07dc\u07df\5\u008cG\2\u07dd\u07df"+
		"\5\u00d2j\2\u07de\u07dc\3\2\2\2\u07de\u07dd\3\2\2\2\u07df\u008b\3\2\2"+
		"\2\u07e0\u07e1\t\25\2\2\u07e1\u07e2\7\3\2\2\u07e2\u07e7\5\u0090I\2\u07e3"+
		"\u07e4\7\4\2\2\u07e4\u07e6\5\u0090I\2\u07e5\u07e3\3\2\2\2\u07e6\u07e9"+
		"\3\2\2\2\u07e7\u07e5\3\2\2\2\u07e7\u07e8\3\2\2\2\u07e8\u07ea\3\2\2\2\u07e9"+
		"\u07e7\3\2\2\2\u07ea\u07eb\7\5\2\2\u07eb\u07fa\3\2\2\2\u07ec\u07ed\7p"+
		"\2\2\u07ed\u07ee\7\u00dc\2\2\u07ee\u07ef\7\3\2\2\u07ef\u07f4\5\u008eH"+
		"\2\u07f0\u07f1\7\4\2\2\u07f1\u07f3\5\u008eH\2\u07f2\u07f0\3\2\2\2\u07f3"+
		"\u07f6\3\2\2\2\u07f4\u07f2\3\2\2\2\u07f4\u07f5\3\2\2\2\u07f5\u07f7\3\2"+
		"\2\2\u07f6\u07f4\3\2\2\2\u07f7\u07f8\7\5\2\2\u07f8\u07fa\3\2\2\2\u07f9"+
		"\u07e0\3\2\2\2\u07f9\u07ec\3\2\2\2\u07fa\u008d\3\2\2\2\u07fb\u07fe\5\u008c"+
		"G\2\u07fc\u07fe\5\u0090I\2\u07fd\u07fb\3\2\2\2\u07fd\u07fc\3\2\2\2\u07fe"+
		"\u008f\3\2\2\2\u07ff\u0808\7\3\2\2\u0800\u0805\5\u00d2j\2\u0801\u0802"+
		"\7\4\2\2\u0802\u0804\5\u00d2j\2\u0803\u0801\3\2\2\2\u0804\u0807\3\2\2"+
		"\2\u0805\u0803\3\2\2\2\u0805\u0806\3\2\2\2\u0806\u0809\3\2\2\2\u0807\u0805"+
		"\3\2\2\2\u0808\u0800\3\2\2\2\u0808\u0809\3\2\2\2\u0809\u080a\3\2\2\2\u080a"+
		"\u080d\7\5\2\2\u080b\u080d\5\u00d2j\2\u080c\u07ff\3\2\2\2\u080c\u080b"+
		"\3\2\2\2\u080d\u0091\3\2\2\2\u080e\u080f\7\u00b3\2\2\u080f\u0810\7\3\2"+
		"\2\u0810\u0811\5\u00c8e\2\u0811\u0812\7e\2\2\u0812\u0813\5\u0094K\2\u0813"+
		"\u0814\7v\2\2\u0814\u0815\7\3\2\2\u0815\u081a\5\u0096L\2\u0816\u0817\7"+
		"\4\2\2\u0817\u0819\5\u0096L\2\u0818\u0816\3\2\2\2\u0819\u081c\3\2\2\2"+
		"\u081a\u0818\3\2\2\2\u081a\u081b\3\2\2\2\u081b\u081d\3\2\2\2\u081c\u081a"+
		"\3\2\2\2\u081d\u081e\7\5\2\2\u081e\u081f\7\5\2\2\u081f\u0093\3\2\2\2\u0820"+
		"\u082d\5\u011a\u008e\2\u0821\u0822\7\3\2\2\u0822\u0827\5\u011a\u008e\2"+
		"\u0823\u0824\7\4\2\2\u0824\u0826\5\u011a\u008e\2\u0825\u0823\3\2\2\2\u0826"+
		"\u0829\3\2\2\2\u0827\u0825\3\2\2\2\u0827\u0828\3\2\2\2\u0828\u082a\3\2"+
		"\2\2\u0829\u0827\3\2\2\2\u082a\u082b\7\5\2\2\u082b\u082d\3\2\2\2\u082c"+
		"\u0820\3\2\2\2\u082c\u0821\3\2\2\2\u082d\u0095\3\2\2\2\u082e\u0833\5\u00d2"+
		"j\2\u082f\u0831\7\32\2\2\u0830\u082f\3\2\2\2\u0830\u0831\3\2\2\2\u0831"+
		"\u0832\3\2\2\2\u0832\u0834\5\u011a\u008e\2\u0833\u0830\3\2\2\2\u0833\u0834"+
		"\3\2\2\2\u0834\u0097\3\2\2\2\u0835\u0836\7\u0085\2\2\u0836\u0838\7\u010a"+
		"\2\2\u0837\u0839\7\u00a9\2\2\u0838\u0837\3\2\2\2\u0838\u0839\3\2\2\2\u0839"+
		"\u083a\3\2\2\2\u083a\u083b\5\u0114\u008b\2\u083b\u0844\7\3\2\2\u083c\u0841"+
		"\5\u00d2j\2\u083d\u083e\7\4\2\2\u083e\u0840\5\u00d2j\2\u083f\u083d\3\2"+
		"\2\2\u0840\u0843\3\2\2\2\u0841\u083f\3\2\2\2\u0841\u0842\3\2\2\2\u0842"+
		"\u0845\3\2\2\2\u0843\u0841\3\2\2\2\u0844\u083c\3\2\2\2\u0844\u0845\3\2"+
		"\2\2\u0845\u0846\3\2\2\2\u0846\u0847\7\5\2\2\u0847\u0853\5\u011a\u008e"+
		"\2\u0848\u084a\7\32\2\2\u0849\u0848\3\2\2\2\u0849\u084a\3\2\2\2\u084a"+
		"\u084b\3\2\2\2\u084b\u0850\5\u011a\u008e\2\u084c\u084d\7\4\2\2\u084d\u084f"+
		"\5\u011a\u008e\2\u084e\u084c\3\2\2\2\u084f\u0852\3\2\2\2\u0850";
	private static final String _serializedATNSegment1 =
		"\u084e\3\2\2\2\u0850\u0851\3\2\2\2\u0851\u0854\3\2\2\2\u0852\u0850\3\2"+
		"\2\2\u0853\u0849\3\2\2\2\u0853\u0854\3\2\2\2\u0854\u0099\3\2\2\2\u0855"+
		"\u0856\t\26\2\2\u0856\u009b\3\2\2\2\u0857\u0859\7\u0085\2\2\u0858\u0857"+
		"\3\2\2\2\u0858\u0859\3\2\2\2\u0859\u085a\3\2\2\2\u085a\u085e\5\u00b4["+
		"\2\u085b\u085d\5\u009eP\2\u085c\u085b\3\2\2\2\u085d\u0860\3\2\2\2\u085e"+
		"\u085c\3\2\2\2\u085e\u085f\3\2\2\2\u085f\u009d\3\2\2\2\u0860\u085e\3\2"+
		"\2\2\u0861\u0862\5\u00a0Q\2\u0862\u0864\7\u0082\2\2\u0863\u0865\7\u0085"+
		"\2\2\u0864\u0863\3\2\2\2\u0864\u0865\3\2\2\2\u0865\u0866\3\2\2\2\u0866"+
		"\u0868\5\u00b4[\2\u0867\u0869\5\u00a2R\2\u0868\u0867\3\2\2\2\u0868\u0869"+
		"\3\2\2\2\u0869\u0873\3\2\2\2\u086a\u086b\7\u009c\2\2\u086b\u086c\5\u00a0"+
		"Q\2\u086c\u086e\7\u0082\2\2\u086d\u086f\7\u0085\2\2\u086e\u086d\3\2\2"+
		"\2\u086e\u086f\3\2\2\2\u086f\u0870\3\2\2\2\u0870\u0871\5\u00b4[\2\u0871"+
		"\u0873\3\2\2\2\u0872\u0861\3\2\2\2\u0872\u086a\3\2\2\2\u0873\u009f\3\2"+
		"\2\2\u0874\u0876\7y\2\2\u0875\u0874\3\2\2\2\u0875\u0876\3\2\2\2\u0876"+
		"\u088d\3\2\2\2\u0877\u088d\7:\2\2\u0878\u087a\7\u0088\2\2\u0879\u087b"+
		"\7\u00a9\2\2\u087a\u0879\3\2\2\2\u087a\u087b\3\2\2\2\u087b\u088d\3\2\2"+
		"\2\u087c\u087e\7\u0088\2\2\u087d\u087c\3\2\2\2\u087d\u087e\3\2\2\2\u087e"+
		"\u087f\3\2\2\2\u087f\u088d\7\u00d5\2\2\u0880\u0882\7\u00ca\2\2\u0881\u0883"+
		"\7\u00a9\2\2\u0882\u0881\3\2\2\2\u0882\u0883\3\2\2\2\u0883\u088d\3\2\2"+
		"\2\u0884\u0886\7j\2\2\u0885\u0887\7\u00a9\2\2\u0886\u0885\3\2\2\2\u0886"+
		"\u0887\3\2\2\2\u0887\u088d\3\2\2\2\u0888\u088a\7\u0088\2\2\u0889\u0888"+
		"\3\2\2\2\u0889\u088a\3\2\2\2\u088a\u088b\3\2\2\2\u088b\u088d\7\26\2\2"+
		"\u088c\u0875\3\2\2\2\u088c\u0877\3\2\2\2\u088c\u0878\3\2\2\2\u088c\u087d"+
		"\3\2\2\2\u088c\u0880\3\2\2\2\u088c\u0884\3\2\2\2\u088c\u0889\3\2\2\2\u088d"+
		"\u00a1\3\2\2\2\u088e\u088f\7\u00a2\2\2\u088f\u0893\5\u00d6l\2\u0890\u0891"+
		"\7\u0108\2\2\u0891\u0893\5\u00a8U\2\u0892\u088e\3\2\2\2\u0892\u0890\3"+
		"\2\2\2\u0893\u00a3\3\2\2\2\u0894\u0895\7\u00ec\2\2\u0895\u0897\7\3\2\2"+
		"\u0896\u0898\5\u00a6T\2\u0897\u0896\3\2\2\2\u0897\u0898\3\2\2\2\u0898"+
		"\u0899\3\2\2\2\u0899\u089a\7\5\2\2\u089a\u00a5\3\2\2\2\u089b\u089d\7\u011b"+
		"\2\2\u089c\u089b\3\2\2\2\u089c\u089d\3\2\2\2\u089d\u089e\3\2\2\2\u089e"+
		"\u089f\t\27\2\2\u089f\u08b4\7\u00b2\2\2\u08a0\u08a1\5\u00d2j\2\u08a1\u08a2"+
		"\7\u00d1\2\2\u08a2\u08b4\3\2\2\2\u08a3\u08a4\7 \2\2\u08a4\u08a5\7\u0128"+
		"\2\2\u08a5\u08a6\7\u00a8\2\2\u08a6\u08a7\7\u00a1\2\2\u08a7\u08b0\7\u0128"+
		"\2\2\u08a8\u08ae\7\u00a2\2\2\u08a9\u08af\5\u011a\u008e\2\u08aa\u08ab\5"+
		"\u0114\u008b\2\u08ab\u08ac\7\3\2\2\u08ac\u08ad\7\5\2\2\u08ad\u08af\3\2"+
		"\2\2\u08ae\u08a9\3\2\2\2\u08ae\u08aa\3\2\2\2\u08af\u08b1\3\2\2\2\u08b0"+
		"\u08a8\3\2\2\2\u08b0\u08b1\3\2\2\2\u08b1\u08b4\3\2\2\2\u08b2\u08b4\5\u00d2"+
		"j\2\u08b3\u089c\3\2\2\2\u08b3\u08a0\3\2\2\2\u08b3\u08a3\3\2\2\2\u08b3"+
		"\u08b2\3\2\2\2\u08b4\u00a7\3\2\2\2\u08b5\u08b6\7\3\2\2\u08b6\u08b7\5\u00aa"+
		"V\2\u08b7\u08b8\7\5\2\2\u08b8\u00a9\3\2\2\2\u08b9\u08be\5\u0116\u008c"+
		"\2\u08ba\u08bb\7\4\2\2\u08bb\u08bd\5\u0116\u008c\2\u08bc\u08ba\3\2\2\2"+
		"\u08bd\u08c0\3\2\2\2\u08be\u08bc\3\2\2\2\u08be\u08bf\3\2\2\2\u08bf\u00ab"+
		"\3\2\2\2\u08c0\u08be\3\2\2\2\u08c1\u08c2\7\3\2\2\u08c2\u08c7\5\u00aeX"+
		"\2\u08c3\u08c4\7\4\2\2\u08c4\u08c6\5\u00aeX\2\u08c5\u08c3\3\2\2\2\u08c6"+
		"\u08c9\3\2\2\2\u08c7\u08c5\3\2\2\2\u08c7\u08c8\3\2\2\2\u08c8\u08ca\3\2"+
		"\2\2\u08c9\u08c7\3\2\2\2\u08ca\u08cb\7\5\2\2\u08cb\u00ad\3\2\2\2\u08cc"+
		"\u08ce\5\u0116\u008c\2\u08cd\u08cf\t\23\2\2\u08ce\u08cd\3\2\2\2\u08ce"+
		"\u08cf\3\2\2\2\u08cf\u00af\3\2\2\2\u08d0\u08d1\7\3\2\2\u08d1\u08d6\5\u00b2"+
		"Z\2\u08d2\u08d3\7\4\2\2\u08d3\u08d5\5\u00b2Z\2\u08d4\u08d2\3\2\2\2\u08d5"+
		"\u08d8\3\2\2\2\u08d6\u08d4\3\2\2\2\u08d6\u08d7\3\2\2\2\u08d7\u08d9\3\2"+
		"\2\2\u08d8\u08d6\3\2\2\2\u08d9\u08da\7\5\2\2\u08da\u00b1\3\2\2\2\u08db"+
		"\u08dd\5\u011a\u008e\2\u08dc\u08de\5,\27\2\u08dd\u08dc\3\2\2\2\u08dd\u08de"+
		"\3\2\2\2\u08de\u00b3\3\2\2\2\u08df\u08e1\5\u00c0a\2\u08e0\u08e2\5\u00a4"+
		"S\2\u08e1\u08e0\3\2\2\2\u08e1\u08e2\3\2\2\2\u08e2\u08e3\3\2\2\2\u08e3"+
		"\u08e4\5\u00ba^\2\u08e4\u08f8\3\2\2\2\u08e5\u08e6\7\3\2\2\u08e6\u08e7"+
		"\5.\30\2\u08e7\u08e9\7\5\2\2\u08e8\u08ea\5\u00a4S\2\u08e9\u08e8\3\2\2"+
		"\2\u08e9\u08ea\3\2\2\2\u08ea\u08eb\3\2\2\2\u08eb\u08ec\5\u00ba^\2\u08ec"+
		"\u08f8\3\2\2\2\u08ed\u08ee\7\3\2\2\u08ee\u08ef\5\u009cO\2\u08ef\u08f1"+
		"\7\5\2\2\u08f0\u08f2\5\u00a4S\2\u08f1\u08f0\3\2\2\2\u08f1\u08f2\3\2\2"+
		"\2\u08f2\u08f3\3\2\2\2\u08f3\u08f4\5\u00ba^\2\u08f4\u08f8\3\2\2\2\u08f5"+
		"\u08f8\5\u00b6\\\2\u08f6\u08f8\5\u00b8]\2\u08f7\u08df\3\2\2\2\u08f7\u08e5"+
		"\3\2\2\2\u08f7\u08ed\3\2\2\2\u08f7\u08f5\3\2\2\2\u08f7\u08f6\3\2\2\2\u08f8"+
		"\u00b5\3\2\2\2\u08f9\u08fa\7\u0109\2\2\u08fa\u08ff\5\u00d2j\2\u08fb\u08fc"+
		"\7\4\2\2\u08fc\u08fe\5\u00d2j\2\u08fd\u08fb\3\2\2\2\u08fe\u0901\3\2\2"+
		"\2\u08ff\u08fd\3\2\2\2\u08ff\u0900\3\2\2\2\u0900\u0902\3\2\2\2\u0901\u08ff"+
		"\3\2\2\2\u0902\u0903\5\u00ba^\2\u0903\u00b7\3\2\2\2\u0904\u0905\5\u0112"+
		"\u008a\2\u0905\u090e\7\3\2\2\u0906\u090b\5\u00d2j\2\u0907\u0908\7\4\2"+
		"\2\u0908\u090a\5\u00d2j\2\u0909\u0907\3\2\2\2\u090a\u090d\3\2\2\2\u090b"+
		"\u0909\3\2\2\2\u090b\u090c\3\2\2\2\u090c\u090f\3\2\2\2\u090d\u090b\3\2"+
		"\2\2\u090e\u0906\3\2\2\2\u090e\u090f\3\2\2\2\u090f\u0910\3\2\2\2\u0910"+
		"\u0911\7\5\2\2\u0911\u0912\5\u00ba^\2\u0912\u00b9\3\2\2\2\u0913\u0915"+
		"\7\32\2\2\u0914\u0913\3\2\2\2\u0914\u0915\3\2\2\2\u0915\u0916\3\2\2\2"+
		"\u0916\u0918\5\u011c\u008f\2\u0917\u0919\5\u00a8U\2\u0918\u0917\3\2\2"+
		"\2\u0918\u0919\3\2\2\2\u0919\u091b\3\2\2\2\u091a\u0914\3\2\2\2\u091a\u091b"+
		"\3\2\2\2\u091b\u00bb\3\2\2\2\u091c\u091d\7\u00d0\2\2\u091d\u091e\7g\2"+
		"\2\u091e\u091f\7\u00d7\2\2\u091f\u0923\7\u0124\2\2\u0920\u0921\7\u010f"+
		"\2\2\u0921\u0922\7\u00d8\2\2\u0922\u0924\5F$\2\u0923\u0920\3\2\2\2\u0923"+
		"\u0924\3\2\2\2\u0924\u094e\3\2\2\2\u0925\u0926\7\u00d0\2\2\u0926\u0927"+
		"\7g\2\2\u0927\u0931\7H\2\2\u0928\u0929\7`\2\2\u0929\u092a\7\u00ef\2\2"+
		"\u092a\u092b\7\"\2\2\u092b\u092f\7\u0124\2\2\u092c\u092d\7U\2\2\u092d"+
		"\u092e\7\"\2\2\u092e\u0930\7\u0124\2\2\u092f\u092c\3\2\2\2\u092f\u0930"+
		"\3\2\2\2\u0930\u0932\3\2\2\2\u0931\u0928\3\2\2\2\u0931\u0932\3\2\2\2\u0932"+
		"\u0938\3\2\2\2\u0933\u0934\7.\2\2\u0934\u0935\7\u0081\2\2\u0935\u0936"+
		"\7\u00ef\2\2\u0936\u0937\7\"\2\2\u0937\u0939\7\u0124\2\2\u0938\u0933\3"+
		"\2\2\2\u0938\u0939\3\2\2\2\u0939\u093f\3\2\2\2\u093a\u093b\7\u0094\2\2"+
		"\u093b\u093c\7\u0083\2\2\u093c\u093d\7\u00ef\2\2\u093d\u093e\7\"\2\2\u093e"+
		"\u0940\7\u0124\2\2\u093f\u093a\3\2\2\2\u093f\u0940\3\2\2\2\u0940\u0945"+
		"\3\2\2\2\u0941\u0942\7\u008b\2\2\u0942\u0943\7\u00ef\2\2\u0943\u0944\7"+
		"\"\2\2\u0944\u0946\7\u0124\2\2\u0945\u0941\3\2\2\2\u0945\u0946\3\2\2\2"+
		"\u0946\u094b\3\2\2\2\u0947\u0948\7\u009f\2\2\u0948\u0949\7F\2\2\u0949"+
		"\u094a\7\32\2\2\u094a\u094c\7\u0124\2\2\u094b\u0947\3\2\2\2\u094b\u094c"+
		"\3\2\2\2\u094c\u094e\3\2\2\2\u094d\u091c\3\2\2\2\u094d\u0925\3\2\2\2\u094e"+
		"\u00bd\3\2\2\2\u094f\u0954\5\u00c0a\2\u0950\u0951\7\4\2\2\u0951\u0953"+
		"\5\u00c0a\2\u0952\u0950\3\2\2\2\u0953\u0956\3\2\2\2\u0954\u0952\3\2\2"+
		"\2\u0954\u0955\3\2\2\2\u0955\u00bf\3\2\2\2\u0956\u0954\3\2\2\2\u0957\u095c"+
		"\5\u0116\u008c\2\u0958\u0959\7\7\2\2\u0959\u095b\5\u0116\u008c\2\u095a"+
		"\u0958\3\2\2\2\u095b\u095e\3\2\2\2\u095c\u095a\3\2\2\2\u095c\u095d\3\2"+
		"\2\2\u095d\u00c1\3\2\2\2\u095e\u095c\3\2\2\2\u095f\u0960\5\u0116\u008c"+
		"\2\u0960\u0961\7\7\2\2\u0961\u0963\3\2\2\2\u0962\u095f\3\2\2\2\u0962\u0963"+
		"\3\2\2\2\u0963\u0964\3\2\2\2\u0964\u0965\5\u0116\u008c\2\u0965\u00c3\3"+
		"\2\2\2\u0966\u0967\5\u0116\u008c\2\u0967\u0968\7\7\2\2\u0968\u096a\3\2"+
		"\2\2\u0969\u0966\3\2\2\2\u0969\u096a\3\2\2\2\u096a\u096b\3\2\2\2\u096b"+
		"\u096c\5\u0116\u008c\2\u096c\u00c5\3\2\2\2\u096d\u0975\5\u00d2j\2\u096e"+
		"\u0970\7\32\2\2\u096f\u096e\3\2\2\2\u096f\u0970\3\2\2\2\u0970\u0973\3"+
		"\2\2\2\u0971\u0974\5\u0116\u008c\2\u0972\u0974\5\u00a8U\2\u0973\u0971"+
		"\3\2\2\2\u0973\u0972\3\2\2\2\u0974\u0976\3\2\2\2\u0975\u096f\3\2\2\2\u0975"+
		"\u0976\3\2\2\2\u0976\u00c7\3\2\2\2\u0977\u097c\5\u00c6d\2\u0978\u0979"+
		"\7\4\2\2\u0979\u097b\5\u00c6d\2\u097a\u0978\3\2\2\2\u097b\u097e\3\2\2"+
		"\2\u097c\u097a\3\2\2\2\u097c\u097d\3\2\2\2\u097d\u00c9\3\2\2\2\u097e\u097c"+
		"\3\2\2\2\u097f\u0980\7\3\2\2\u0980\u0985\5\u00ccg\2\u0981\u0982\7\4\2"+
		"\2\u0982\u0984\5\u00ccg\2\u0983\u0981\3\2\2\2\u0984\u0987\3\2\2\2\u0985"+
		"\u0983\3\2\2\2\u0985\u0986\3\2\2\2\u0986\u0988\3\2\2\2\u0987\u0985\3\2"+
		"\2\2\u0988\u0989\7\5\2\2\u0989\u00cb\3\2\2\2\u098a\u098d\5\u00ceh\2\u098b"+
		"\u098d\5\u00fe\u0080\2\u098c\u098a\3\2\2\2\u098c\u098b\3\2\2\2\u098d\u00cd"+
		"\3\2\2\2\u098e\u099c\5\u0114\u008b\2\u098f\u0990\5\u011a\u008e\2\u0990"+
		"\u0991\7\3\2\2\u0991\u0996\5\u00d0i\2\u0992\u0993\7\4\2\2\u0993\u0995"+
		"\5\u00d0i\2\u0994\u0992\3\2\2\2\u0995\u0998\3\2\2\2\u0996\u0994\3\2\2"+
		"\2\u0996\u0997\3\2\2\2\u0997\u0999\3\2\2\2\u0998\u0996\3\2\2\2\u0999\u099a"+
		"\7\5\2\2\u099a\u099c\3\2\2\2\u099b\u098e\3\2\2\2\u099b\u098f\3\2\2\2\u099c"+
		"\u00cf\3\2\2\2\u099d\u09a0\5\u0114\u008b\2\u099e\u09a0\5\u00dep\2\u099f"+
		"\u099d\3\2\2\2\u099f\u099e\3\2\2\2\u09a0\u00d1\3\2\2\2\u09a1\u09a2\5\u00d6"+
		"l\2\u09a2\u00d3\3\2\2\2\u09a3\u09a8\5\u00d2j\2\u09a4\u09a5\7\4\2\2\u09a5"+
		"\u09a7\5\u00d2j\2\u09a6\u09a4\3\2\2\2\u09a7\u09aa\3\2\2\2\u09a8\u09a6"+
		"\3\2\2\2\u09a8\u09a9\3\2\2\2\u09a9\u00d5\3\2\2\2\u09aa\u09a8\3\2\2\2\u09ab"+
		"\u09ac\bl\1\2\u09ac\u09ad\7\u009e\2\2\u09ad\u09b8\5\u00d6l\7\u09ae\u09af"+
		"\7X\2\2\u09af\u09b0\7\3\2\2\u09b0\u09b1\5.\30\2\u09b1\u09b2\7\5\2\2\u09b2"+
		"\u09b8\3\2\2\2\u09b3\u09b5\5\u00dan\2\u09b4\u09b6\5\u00d8m\2\u09b5\u09b4"+
		"\3\2\2\2\u09b5\u09b6\3\2\2\2\u09b6\u09b8\3\2\2\2\u09b7\u09ab\3\2\2\2\u09b7"+
		"\u09ae\3\2\2\2\u09b7\u09b3\3\2\2\2\u09b8\u09c1\3\2\2\2\u09b9\u09ba\f\4"+
		"\2\2\u09ba\u09bb\7\25\2\2\u09bb\u09c0\5\u00d6l\5\u09bc\u09bd\f\3\2\2\u09bd"+
		"\u09be\7\u00a6\2\2\u09be\u09c0\5\u00d6l\4\u09bf\u09b9\3\2\2\2\u09bf\u09bc"+
		"\3\2\2\2\u09c0\u09c3\3\2\2\2\u09c1\u09bf\3\2\2\2\u09c1\u09c2\3\2\2\2\u09c2"+
		"\u00d7\3\2\2\2\u09c3\u09c1\3\2\2\2\u09c4\u09c6\7\u009e\2\2\u09c5\u09c4"+
		"\3\2\2\2\u09c5\u09c6\3\2\2\2\u09c6\u09c7\3\2\2\2\u09c7\u09c8\7\36\2\2"+
		"\u09c8\u09c9\5\u00dan\2\u09c9\u09ca\7\25\2\2\u09ca\u09cb\5\u00dan\2\u09cb"+
		"\u0a17\3\2\2\2\u09cc\u09ce\7\u009e\2\2\u09cd\u09cc\3\2\2\2\u09cd\u09ce"+
		"\3\2\2\2\u09ce\u09cf\3\2\2\2\u09cf\u09d0\7v\2\2\u09d0\u09d1\7\3\2\2\u09d1"+
		"\u09d6\5\u00d2j\2\u09d2\u09d3\7\4\2\2\u09d3\u09d5\5\u00d2j\2\u09d4\u09d2"+
		"\3\2\2\2\u09d5\u09d8\3\2\2\2\u09d6\u09d4\3\2\2\2\u09d6\u09d7\3\2\2\2\u09d7"+
		"\u09d9\3\2\2\2\u09d8\u09d6\3\2\2\2\u09d9\u09da\7\5\2\2\u09da\u0a17\3\2"+
		"\2\2\u09db\u09dd\7\u009e\2\2\u09dc\u09db\3\2\2\2\u09dc\u09dd\3\2\2\2\u09dd"+
		"\u09de\3\2\2\2\u09de\u09df\7v\2\2\u09df\u09e0\7\3\2\2\u09e0\u09e1\5.\30"+
		"\2\u09e1\u09e2\7\5\2\2\u09e2\u0a17\3\2\2\2\u09e3\u09e5\7\u009e\2\2\u09e4"+
		"\u09e3\3\2\2\2\u09e4\u09e5\3\2\2\2\u09e5\u09e6\3\2\2\2\u09e6\u09e7\7\u00cb"+
		"\2\2\u09e7\u0a17\5\u00dan\2\u09e8\u09ea\7\u009e\2\2\u09e9\u09e8\3\2\2"+
		"\2\u09e9\u09ea\3\2\2\2\u09ea\u09eb\3\2\2\2\u09eb\u09ec\7\u0089\2\2\u09ec"+
		"\u09fa\t\30\2\2\u09ed\u09ee\7\3\2\2\u09ee\u09fb\7\5\2\2\u09ef\u09f0\7"+
		"\3\2\2\u09f0\u09f5\5\u00d2j\2\u09f1\u09f2\7\4\2\2\u09f2\u09f4\5\u00d2"+
		"j\2\u09f3\u09f1\3\2\2\2\u09f4\u09f7\3\2\2\2\u09f5\u09f3\3\2\2\2\u09f5"+
		"\u09f6\3\2\2\2\u09f6\u09f8\3\2\2\2\u09f7\u09f5\3\2\2\2\u09f8\u09f9\7\5"+
		"\2\2\u09f9\u09fb\3\2\2\2\u09fa\u09ed\3\2\2\2\u09fa\u09ef\3\2\2\2\u09fb"+
		"\u0a17\3\2\2\2\u09fc\u09fe\7\u009e\2\2\u09fd\u09fc\3\2\2\2\u09fd\u09fe"+
		"\3\2\2\2\u09fe\u09ff\3\2\2\2\u09ff\u0a00\7\u0089\2\2\u0a00\u0a03\5\u00da"+
		"n\2\u0a01\u0a02\7T\2\2\u0a02\u0a04\7\u0124\2\2\u0a03\u0a01\3\2\2\2\u0a03"+
		"\u0a04\3\2\2\2\u0a04\u0a17\3\2\2\2\u0a05\u0a07\7\u0080\2\2\u0a06\u0a08"+
		"\7\u009e\2\2\u0a07\u0a06\3\2\2\2\u0a07\u0a08\3\2\2\2\u0a08\u0a09\3\2\2"+
		"\2\u0a09\u0a17\7\u009f\2\2\u0a0a\u0a0c\7\u0080\2\2\u0a0b\u0a0d\7\u009e"+
		"\2\2\u0a0c\u0a0b\3\2\2\2\u0a0c\u0a0d\3\2\2\2\u0a0d\u0a0e\3\2\2\2\u0a0e"+
		"\u0a17\t\31\2\2\u0a0f\u0a11\7\u0080\2\2\u0a10\u0a12\7\u009e\2\2\u0a11"+
		"\u0a10\3\2\2\2\u0a11\u0a12\3\2\2\2\u0a12\u0a13\3\2\2\2\u0a13\u0a14\7N"+
		"\2\2\u0a14\u0a15\7i\2\2\u0a15\u0a17\5\u00dan\2\u0a16\u09c5\3\2\2\2\u0a16"+
		"\u09cd\3\2\2\2\u0a16\u09dc\3\2\2\2\u0a16\u09e4\3\2\2\2\u0a16\u09e9\3\2"+
		"\2\2\u0a16\u09fd\3\2\2\2\u0a16\u0a05\3\2\2\2\u0a16\u0a0a\3\2\2\2\u0a16"+
		"\u0a0f\3\2\2\2\u0a17\u00d9\3\2\2\2\u0a18\u0a19\bn\1\2\u0a19\u0a1d\5\u00dc"+
		"o\2\u0a1a\u0a1b\t\32\2\2\u0a1b\u0a1d\5\u00dan\t\u0a1c\u0a18\3\2\2\2\u0a1c"+
		"\u0a1a\3\2\2\2\u0a1d\u0a33\3\2\2\2\u0a1e\u0a1f\f\b\2\2\u0a1f\u0a20\t\33"+
		"\2\2\u0a20\u0a32\5\u00dan\t\u0a21\u0a22\f\7\2\2\u0a22\u0a23\t\34\2\2\u0a23"+
		"\u0a32\5\u00dan\b\u0a24\u0a25\f\6\2\2\u0a25\u0a26\7\u0120\2\2\u0a26\u0a32"+
		"\5\u00dan\7\u0a27\u0a28\f\5\2\2\u0a28\u0a29\7\u0123\2\2\u0a29\u0a32\5"+
		"\u00dan\6\u0a2a\u0a2b\f\4\2\2\u0a2b\u0a2c\7\u0121\2\2\u0a2c\u0a32\5\u00da"+
		"n\5\u0a2d\u0a2e\f\3\2\2\u0a2e\u0a2f\5\u00e0q\2\u0a2f\u0a30\5\u00dan\4"+
		"\u0a30\u0a32\3\2\2\2\u0a31\u0a1e\3\2\2\2\u0a31\u0a21\3\2\2\2\u0a31\u0a24"+
		"\3\2\2\2\u0a31\u0a27\3\2\2\2\u0a31\u0a2a\3\2\2\2\u0a31\u0a2d\3\2\2\2\u0a32"+
		"\u0a35\3\2\2\2\u0a33\u0a31\3\2\2\2\u0a33\u0a34\3\2\2\2\u0a34\u00db\3\2"+
		"\2\2\u0a35\u0a33\3\2\2\2\u0a36\u0a37\bo\1\2\u0a37\u0af3\t\35\2\2\u0a38"+
		"\u0a3a\7%\2\2\u0a39\u0a3b\5\u0104\u0083\2\u0a3a\u0a39\3\2\2\2\u0a3b\u0a3c"+
		"\3\2\2\2\u0a3c\u0a3a\3\2\2\2\u0a3c\u0a3d\3\2\2\2\u0a3d\u0a40\3\2\2\2\u0a3e"+
		"\u0a3f\7R\2\2\u0a3f\u0a41\5\u00d2j\2\u0a40\u0a3e\3\2\2\2\u0a40\u0a41\3"+
		"\2\2\2\u0a41\u0a42\3\2\2\2\u0a42\u0a43\7S\2\2\u0a43\u0af3\3\2\2\2\u0a44"+
		"\u0a45\7%\2\2\u0a45\u0a47\5\u00d2j\2\u0a46\u0a48\5\u0104\u0083\2\u0a47"+
		"\u0a46\3\2\2\2\u0a48\u0a49\3\2\2\2\u0a49\u0a47\3\2\2\2\u0a49\u0a4a\3\2"+
		"\2\2\u0a4a\u0a4d\3\2\2\2\u0a4b\u0a4c\7R\2\2\u0a4c\u0a4e\5\u00d2j\2\u0a4d"+
		"\u0a4b\3\2\2\2\u0a4d\u0a4e\3\2\2\2\u0a4e\u0a4f\3\2\2\2\u0a4f\u0a50\7S"+
		"\2\2\u0a50\u0af3\3\2\2\2\u0a51\u0a52\t\36\2\2\u0a52\u0a53\7\3\2\2\u0a53"+
		"\u0a54\5\u00d2j\2\u0a54\u0a55\7\32\2\2\u0a55\u0a56\5\u00f6|\2\u0a56\u0a57"+
		"\7\5\2\2\u0a57\u0af3\3\2\2\2\u0a58\u0a59\7\u00e6\2\2\u0a59\u0a62\7\3\2"+
		"\2\u0a5a\u0a5f\5\u00c6d\2\u0a5b\u0a5c\7\4\2\2\u0a5c\u0a5e\5\u00c6d\2\u0a5d"+
		"\u0a5b\3\2\2\2\u0a5e\u0a61\3\2\2\2\u0a5f\u0a5d\3\2\2\2\u0a5f\u0a60\3\2"+
		"\2\2\u0a60\u0a63\3\2\2\2\u0a61\u0a5f\3\2\2\2\u0a62\u0a5a\3\2\2\2\u0a62"+
		"\u0a63\3\2\2\2\u0a63\u0a64\3\2\2\2\u0a64\u0af3\7\5\2\2\u0a65\u0a66\7c"+
		"\2\2\u0a66\u0a67\7\3\2\2\u0a67\u0a6a\5\u00d2j\2\u0a68\u0a69\7t\2\2\u0a69"+
		"\u0a6b\7\u00a0\2\2\u0a6a\u0a68\3\2\2\2\u0a6a\u0a6b\3\2\2\2\u0a6b\u0a6c"+
		"\3\2\2\2\u0a6c\u0a6d\7\5\2\2\u0a6d\u0af3\3\2\2\2\u0a6e\u0a6f\7\u0084\2"+
		"\2\u0a6f\u0a70\7\3\2\2\u0a70\u0a73\5\u00d2j\2\u0a71\u0a72\7t\2\2\u0a72"+
		"\u0a74\7\u00a0\2\2\u0a73\u0a71\3\2\2\2\u0a73\u0a74\3\2\2\2\u0a74\u0a75"+
		"\3\2\2\2\u0a75\u0a76\7\5\2\2\u0a76\u0af3\3\2\2\2\u0a77\u0a78\7\u00b5\2"+
		"\2\u0a78\u0a79\7\3\2\2\u0a79\u0a7a\5\u00dan\2\u0a7a\u0a7b\7v\2\2\u0a7b"+
		"\u0a7c\5\u00dan\2\u0a7c\u0a7d\7\5\2\2\u0a7d\u0af3\3\2\2\2\u0a7e\u0af3"+
		"\5\u00dep\2\u0a7f\u0af3\7\u011c\2\2\u0a80\u0a81\5\u0114\u008b\2\u0a81"+
		"\u0a82\7\7\2\2\u0a82\u0a83\7\u011c\2\2\u0a83\u0af3\3\2\2\2\u0a84\u0a85"+
		"\7\3\2\2\u0a85\u0a88\5\u00c6d\2\u0a86\u0a87\7\4\2\2\u0a87\u0a89\5\u00c6"+
		"d\2\u0a88\u0a86\3\2\2\2\u0a89\u0a8a\3\2\2\2\u0a8a\u0a88\3\2\2\2\u0a8a"+
		"\u0a8b\3\2\2\2\u0a8b\u0a8c\3\2\2\2\u0a8c\u0a8d\7\5\2\2\u0a8d\u0af3\3\2"+
		"\2\2\u0a8e\u0a8f\7\3\2\2\u0a8f\u0a90\5.\30\2\u0a90\u0a91\7\5\2\2\u0a91"+
		"\u0af3\3\2\2\2\u0a92\u0a93\5\u0112\u008a\2\u0a93\u0a9f\7\3\2\2\u0a94\u0a96"+
		"\5\u009aN\2\u0a95\u0a94\3\2\2\2\u0a95\u0a96\3\2\2\2\u0a96\u0a97\3\2\2"+
		"\2\u0a97\u0a9c\5\u00d2j\2\u0a98\u0a99\7\4\2\2\u0a99\u0a9b\5\u00d2j\2\u0a9a"+
		"\u0a98\3\2\2\2\u0a9b\u0a9e\3\2\2\2\u0a9c\u0a9a\3\2\2\2\u0a9c\u0a9d\3\2"+
		"\2\2\u0a9d\u0aa0\3\2\2\2\u0a9e\u0a9c\3\2\2\2\u0a9f\u0a95\3\2\2\2\u0a9f"+
		"\u0aa0\3\2\2\2\u0aa0\u0aa1\3\2\2\2\u0aa1\u0aa8\7\5\2\2\u0aa2\u0aa3\7a"+
		"\2\2\u0aa3\u0aa4\7\3\2\2\u0aa4\u0aa5\7\u010d\2\2\u0aa5\u0aa6\5\u00d6l"+
		"\2\u0aa6\u0aa7\7\5\2\2\u0aa7\u0aa9\3\2\2\2\u0aa8\u0aa2\3\2\2\2\u0aa8\u0aa9"+
		"\3\2\2\2\u0aa9\u0aac\3\2\2\2\u0aaa\u0aab\t\37\2\2\u0aab\u0aad\7\u00a0"+
		"\2\2\u0aac\u0aaa\3\2\2\2\u0aac\u0aad\3\2\2\2\u0aad\u0ab0\3\2\2\2\u0aae"+
		"\u0aaf\7\u00ab\2\2\u0aaf\u0ab1\5\u010a\u0086\2\u0ab0\u0aae\3\2\2\2\u0ab0"+
		"\u0ab1\3\2\2\2\u0ab1\u0af3\3\2\2\2\u0ab2\u0ab3\5\u011a\u008e\2\u0ab3\u0ab4"+
		"\7\n\2\2\u0ab4\u0ab5\5\u00d2j\2\u0ab5\u0af3\3\2\2\2\u0ab6\u0ab7\7\3\2"+
		"\2\u0ab7\u0aba\5\u011a\u008e\2\u0ab8\u0ab9\7\4\2\2\u0ab9\u0abb\5\u011a"+
		"\u008e\2\u0aba\u0ab8\3\2\2\2\u0abb\u0abc\3\2\2\2\u0abc\u0aba\3\2\2\2\u0abc"+
		"\u0abd\3\2\2\2\u0abd\u0abe\3\2\2\2\u0abe\u0abf\7\5\2\2\u0abf\u0ac0\7\n"+
		"\2\2\u0ac0\u0ac1\5\u00d2j\2\u0ac1\u0af3\3\2\2\2\u0ac2\u0af3\5\u011a\u008e"+
		"\2\u0ac3\u0ac4\7\3\2\2\u0ac4\u0ac5\5\u00d2j\2\u0ac5\u0ac6\7\5\2\2\u0ac6"+
		"\u0af3\3\2\2\2\u0ac7\u0ac8\7]\2\2\u0ac8\u0ac9\7\3\2\2\u0ac9\u0aca\5\u011a"+
		"\u008e\2\u0aca\u0acb\7i\2\2\u0acb\u0acc\5\u00dan\2\u0acc\u0acd\7\5\2\2"+
		"\u0acd\u0af3\3\2\2\2\u0ace\u0acf\t \2\2\u0acf\u0ad0\7\3\2\2\u0ad0\u0ad1"+
		"\5\u00dan\2\u0ad1\u0ad2\t!\2\2\u0ad2\u0ad5\5\u00dan\2\u0ad3\u0ad4\t\""+
		"\2\2\u0ad4\u0ad6\5\u00dan\2\u0ad5\u0ad3\3\2\2\2\u0ad5\u0ad6\3\2\2\2\u0ad6"+
		"\u0ad7\3\2\2\2\u0ad7\u0ad8\7\5\2\2\u0ad8\u0af3\3\2\2\2\u0ad9\u0ada\7\u00f8"+
		"\2\2\u0ada\u0adc\7\3\2\2\u0adb\u0add\t#\2\2\u0adc\u0adb\3\2\2\2\u0adc"+
		"\u0add\3\2\2\2\u0add\u0adf\3\2\2\2\u0ade\u0ae0\5\u00dan\2\u0adf\u0ade"+
		"\3\2\2\2\u0adf\u0ae0\3\2\2\2\u0ae0\u0ae1\3\2\2\2\u0ae1\u0ae2\7i\2\2\u0ae2"+
		"\u0ae3\5\u00dan\2\u0ae3\u0ae4\7\5\2\2\u0ae4\u0af3\3\2\2\2\u0ae5\u0ae6"+
		"\7\u00ad\2\2\u0ae6\u0ae7\7\3\2\2\u0ae7\u0ae8\5\u00dan\2\u0ae8\u0ae9\7"+
		"\u00b4\2\2\u0ae9\u0aea\5\u00dan\2\u0aea\u0aeb\7i\2\2\u0aeb\u0aee\5\u00da"+
		"n\2\u0aec\u0aed\7e\2\2\u0aed\u0aef\5\u00dan\2\u0aee\u0aec\3\2\2\2\u0aee"+
		"\u0aef\3\2\2\2\u0aef\u0af0\3\2\2\2\u0af0\u0af1\7\5\2\2\u0af1\u0af3\3\2"+
		"\2\2\u0af2\u0a36\3\2\2\2\u0af2\u0a38\3\2\2\2\u0af2\u0a44\3\2\2\2\u0af2"+
		"\u0a51\3\2\2\2\u0af2\u0a58\3\2\2\2\u0af2\u0a65\3\2\2\2\u0af2\u0a6e\3\2"+
		"\2\2\u0af2\u0a77\3\2\2\2\u0af2\u0a7e\3\2\2\2\u0af2\u0a7f\3\2\2\2\u0af2"+
		"\u0a80\3\2\2\2\u0af2\u0a84\3\2\2\2\u0af2\u0a8e\3\2\2\2\u0af2\u0a92\3\2"+
		"\2\2\u0af2\u0ab2\3\2\2\2\u0af2\u0ab6\3\2\2\2\u0af2\u0ac2\3\2\2\2\u0af2"+
		"\u0ac3\3\2\2\2\u0af2\u0ac7\3\2\2\2\u0af2\u0ace\3\2\2\2\u0af2\u0ad9\3\2"+
		"\2\2\u0af2\u0ae5\3\2\2\2\u0af3\u0afe\3\2\2\2\u0af4\u0af5\f\n\2\2\u0af5"+
		"\u0af6\7\13\2\2\u0af6\u0af7\5\u00dan\2\u0af7\u0af8\7\f\2\2\u0af8\u0afd"+
		"\3\2\2\2\u0af9\u0afa\f\b\2\2\u0afa\u0afb\7\7\2\2\u0afb\u0afd\5\u011a\u008e"+
		"\2\u0afc\u0af4\3\2\2\2\u0afc\u0af9\3\2\2\2\u0afd\u0b00\3\2\2\2\u0afe\u0afc"+
		"\3\2\2\2\u0afe\u0aff\3\2\2\2\u0aff\u00dd\3\2\2\2\u0b00\u0afe\3\2\2\2\u0b01"+
		"\u0b0e\7\u009f\2\2\u0b02\u0b0e\5\u00e8u\2\u0b03\u0b04\5\u011a\u008e\2"+
		"\u0b04\u0b05\7\u0124\2\2\u0b05\u0b0e\3\2\2\2\u0b06\u0b0e\5\u0120\u0091"+
		"\2\u0b07\u0b0e\5\u00e6t\2\u0b08\u0b0a\7\u0124\2\2\u0b09\u0b08\3\2\2\2"+
		"\u0b0a\u0b0b\3\2\2\2\u0b0b\u0b09\3\2\2\2\u0b0b\u0b0c\3\2\2\2\u0b0c\u0b0e"+
		"\3\2\2\2\u0b0d\u0b01\3\2\2\2\u0b0d\u0b02\3\2\2\2\u0b0d\u0b03\3\2\2\2\u0b0d"+
		"\u0b06\3\2\2\2\u0b0d\u0b07\3\2\2\2\u0b0d\u0b09\3\2\2\2\u0b0e\u00df\3\2"+
		"\2\2\u0b0f\u0b10\t$\2\2\u0b10\u00e1\3\2\2\2\u0b11\u0b12\t%\2\2\u0b12\u00e3"+
		"\3\2\2\2\u0b13\u0b14\t&\2\2\u0b14\u00e5\3\2\2\2\u0b15\u0b16\t\'\2\2\u0b16"+
		"\u00e7\3\2\2\2\u0b17\u0b1a\7~\2\2\u0b18\u0b1b\5\u00eav\2\u0b19\u0b1b\5"+
		"\u00eex\2\u0b1a\u0b18\3\2\2\2\u0b1a\u0b19\3\2\2\2\u0b1a\u0b1b\3\2\2\2"+
		"\u0b1b\u00e9\3\2\2\2\u0b1c\u0b1e\5\u00ecw\2\u0b1d\u0b1f\5\u00f0y\2\u0b1e"+
		"\u0b1d\3\2\2\2\u0b1e\u0b1f\3\2\2\2\u0b1f\u00eb\3\2\2\2\u0b20\u0b21\5\u00f2"+
		"z\2\u0b21\u0b22\5\u011a\u008e\2\u0b22\u0b24\3\2\2\2\u0b23\u0b20\3\2\2"+
		"\2\u0b24\u0b25\3\2\2\2\u0b25\u0b23\3\2\2\2\u0b25\u0b26\3\2\2\2\u0b26\u00ed"+
		"\3\2\2\2\u0b27\u0b2a\5\u00f0y\2\u0b28\u0b2b\5\u00ecw\2\u0b29\u0b2b\5\u00f0"+
		"y\2\u0b2a\u0b28\3\2\2\2\u0b2a\u0b29\3\2\2\2\u0b2a\u0b2b\3\2\2\2\u0b2b"+
		"\u00ef\3\2\2\2\u0b2c\u0b2d\5\u00f2z\2\u0b2d\u0b2e\5\u011a\u008e\2\u0b2e"+
		"\u0b2f\7\u00f2\2\2\u0b2f\u0b30\5\u011a\u008e\2\u0b30\u00f1\3\2\2\2\u0b31"+
		"\u0b33\t(\2\2\u0b32\u0b31\3\2\2\2\u0b32\u0b33\3\2\2\2\u0b33\u0b34\3\2"+
		"\2\2\u0b34\u0b35\t)\2\2\u0b35\u00f3\3\2\2\2\u0b36\u0b3a\7c\2\2\u0b37\u0b38"+
		"\7\21\2\2\u0b38\u0b3a\5\u0116\u008c\2\u0b39\u0b36\3\2\2\2\u0b39\u0b37"+
		"\3\2\2\2\u0b3a\u00f5\3\2\2\2\u0b3b\u0b3c\7\31\2\2\u0b3c\u0b3d\7\u0116"+
		"\2\2\u0b3d\u0b3e\5\u00f6|\2\u0b3e\u0b3f\7\u0118\2\2\u0b3f\u0b6a\3\2\2"+
		"\2\u0b40\u0b41\7\u0094\2\2\u0b41\u0b42\7\u0116\2\2\u0b42\u0b43\5\u00f6"+
		"|\2\u0b43\u0b44\7\4\2\2\u0b44\u0b45\5\u00f6|\2\u0b45\u0b46\7\u0118\2\2"+
		"\u0b46\u0b6a\3\2\2\2\u0b47\u0b4e\7\u00e6\2\2\u0b48\u0b4a\7\u0116\2\2\u0b49"+
		"\u0b4b\5\u0100\u0081\2\u0b4a\u0b49\3\2\2\2\u0b4a\u0b4b\3\2\2\2\u0b4b\u0b4c"+
		"\3\2\2\2\u0b4c\u0b4f\7\u0118\2\2\u0b4d\u0b4f\7\u0114\2\2\u0b4e\u0b48\3"+
		"\2\2\2\u0b4e\u0b4d\3\2\2\2\u0b4f\u0b6a\3\2\2\2\u0b50\u0b51\7~\2\2\u0b51"+
		"\u0b54\t*\2\2\u0b52\u0b53\7\u00f2\2\2\u0b53\u0b55\7\u0098\2\2\u0b54\u0b52"+
		"\3\2\2\2\u0b54\u0b55\3\2\2\2\u0b55\u0b6a\3\2\2\2\u0b56\u0b57\7~\2\2\u0b57"+
		"\u0b5a\t+\2\2\u0b58\u0b59\7\u00f2\2\2\u0b59\u0b5b\t,\2\2\u0b5a\u0b58\3"+
		"\2\2\2\u0b5a\u0b5b\3\2\2\2\u0b5b\u0b6a\3\2\2\2\u0b5c\u0b67\5\u011a\u008e"+
		"\2\u0b5d\u0b5e\7\3\2\2\u0b5e\u0b63\7\u0128\2\2\u0b5f\u0b60\7\4\2\2\u0b60"+
		"\u0b62\7\u0128\2\2\u0b61\u0b5f\3\2\2\2\u0b62\u0b65\3\2\2\2\u0b63\u0b61"+
		"\3\2\2\2\u0b63\u0b64\3\2\2\2\u0b64\u0b66\3\2\2\2\u0b65\u0b63\3\2\2\2\u0b66"+
		"\u0b68\7\5\2\2\u0b67\u0b5d\3\2\2\2\u0b67\u0b68\3\2\2\2\u0b68\u0b6a\3\2"+
		"\2\2\u0b69\u0b3b\3\2\2\2\u0b69\u0b40\3\2\2\2\u0b69\u0b47\3\2\2\2\u0b69"+
		"\u0b50\3\2\2\2\u0b69\u0b56\3\2\2\2\u0b69\u0b5c\3\2\2\2\u0b6a\u00f7\3\2"+
		"\2\2\u0b6b\u0b70\5\u00fa~\2\u0b6c\u0b6d\7\4\2\2\u0b6d\u0b6f\5\u00fa~\2"+
		"\u0b6e\u0b6c\3\2\2\2\u0b6f\u0b72\3\2\2\2\u0b70\u0b6e\3\2\2\2\u0b70\u0b71"+
		"\3\2\2\2\u0b71\u00f9\3\2\2\2\u0b72\u0b70\3\2\2\2\u0b73\u0b74\5\u00c0a"+
		"\2\u0b74\u0b77\5\u00f6|\2\u0b75\u0b76\7\u009e\2\2\u0b76\u0b78\7\u009f"+
		"\2\2\u0b77\u0b75\3\2\2\2\u0b77\u0b78\3\2\2\2\u0b78\u0b7a\3\2\2\2\u0b79"+
		"\u0b7b\5,\27\2\u0b7a\u0b79\3\2\2\2\u0b7a\u0b7b\3\2\2\2\u0b7b\u0b7d\3\2"+
		"\2\2\u0b7c\u0b7e\5\u00f4{\2\u0b7d\u0b7c\3\2\2\2\u0b7d\u0b7e\3\2\2\2\u0b7e"+
		"\u00fb\3\2\2\2\u0b7f\u0b84\5\u00fe\u0080\2\u0b80\u0b81\7\4\2\2\u0b81\u0b83"+
		"\5\u00fe\u0080\2\u0b82\u0b80\3\2\2\2\u0b83\u0b86\3\2\2\2\u0b84\u0b82\3"+
		"\2\2\2\u0b84\u0b85\3\2\2\2\u0b85\u00fd\3\2\2\2\u0b86\u0b84\3\2\2\2\u0b87"+
		"\u0b88\5\u0116\u008c\2\u0b88\u0b8b\5\u00f6|\2\u0b89\u0b8a\7\u009e\2\2"+
		"\u0b8a\u0b8c\7\u009f\2\2\u0b8b\u0b89\3\2\2\2\u0b8b\u0b8c\3\2\2\2\u0b8c"+
		"\u0b8e\3\2\2\2\u0b8d\u0b8f\5,\27\2\u0b8e\u0b8d\3\2\2\2\u0b8e\u0b8f\3\2"+
		"\2\2\u0b8f\u00ff\3\2\2\2\u0b90\u0b95\5\u0102\u0082\2\u0b91\u0b92\7\4\2"+
		"\2\u0b92\u0b94\5\u0102\u0082\2\u0b93\u0b91\3\2\2\2\u0b94\u0b97\3\2\2\2"+
		"\u0b95\u0b93\3\2\2\2\u0b95\u0b96\3\2\2\2\u0b96\u0101\3\2\2\2\u0b97\u0b95"+
		"\3\2\2\2\u0b98\u0b9a\5\u011a\u008e\2\u0b99\u0b9b\7\r\2\2\u0b9a\u0b99\3"+
		"\2\2\2\u0b9a\u0b9b\3\2\2\2\u0b9b\u0b9c\3\2\2\2\u0b9c\u0b9f\5\u00f6|\2"+
		"\u0b9d\u0b9e\7\u009e\2\2\u0b9e\u0ba0\7\u009f\2\2\u0b9f\u0b9d\3\2\2\2\u0b9f"+
		"\u0ba0\3\2\2\2\u0ba0\u0ba2\3\2\2\2\u0ba1\u0ba3\5,\27\2\u0ba2\u0ba1\3\2"+
		"\2\2\u0ba2\u0ba3\3\2\2\2\u0ba3\u0103\3\2\2\2\u0ba4\u0ba5\7\u010c\2\2\u0ba5"+
		"\u0ba6\5\u00d2j\2\u0ba6\u0ba7\7\u00f0\2\2\u0ba7\u0ba8\5\u00d2j\2\u0ba8"+
		"\u0105\3\2\2\2\u0ba9\u0baa\7\u010e\2\2\u0baa\u0baf\5\u0108\u0085\2\u0bab"+
		"\u0bac\7\4\2\2\u0bac\u0bae\5\u0108\u0085\2\u0bad\u0bab\3\2\2\2\u0bae\u0bb1"+
		"\3\2\2\2\u0baf\u0bad\3\2\2\2\u0baf\u0bb0\3\2\2\2\u0bb0\u0107\3\2\2\2\u0bb1"+
		"\u0baf\3\2\2\2\u0bb2\u0bb3\5\u0116\u008c\2\u0bb3\u0bb4\7\32\2\2\u0bb4"+
		"\u0bb5\5\u010a\u0086\2\u0bb5\u0109\3\2\2\2\u0bb6\u0be5\5\u0116\u008c\2"+
		"\u0bb7\u0bb8\7\3\2\2\u0bb8\u0bb9\5\u0116\u008c\2\u0bb9\u0bba\7\5\2\2\u0bba"+
		"\u0be5\3\2\2\2\u0bbb\u0bde\7\3\2\2\u0bbc\u0bbd\7*\2\2\u0bbd\u0bbe\7\""+
		"\2\2\u0bbe\u0bc3\5\u00d2j\2\u0bbf\u0bc0\7\4\2\2\u0bc0\u0bc2\5\u00d2j\2"+
		"\u0bc1\u0bbf\3\2\2\2\u0bc2\u0bc5\3\2\2\2\u0bc3\u0bc1\3\2\2\2\u0bc3\u0bc4"+
		"\3\2\2\2\u0bc4\u0bdf\3\2\2\2\u0bc5\u0bc3\3\2\2\2\u0bc6\u0bc7\t-\2\2\u0bc7"+
		"\u0bc8\7\"\2\2\u0bc8\u0bcd\5\u00d2j\2\u0bc9\u0bca\7\4\2\2\u0bca\u0bcc"+
		"\5\u00d2j\2\u0bcb\u0bc9\3\2\2\2\u0bcc\u0bcf\3\2\2\2\u0bcd\u0bcb\3\2\2"+
		"\2\u0bcd\u0bce\3\2\2\2\u0bce\u0bd1\3\2\2\2\u0bcf\u0bcd\3\2\2\2\u0bd0\u0bc6"+
		"\3\2\2\2\u0bd0\u0bd1\3\2\2\2\u0bd1\u0bdc\3\2\2\2\u0bd2\u0bd3\t.\2\2\u0bd3"+
		"\u0bd4\7\"\2\2\u0bd4\u0bd9\5d\63\2\u0bd5\u0bd6\7\4\2\2\u0bd6\u0bd8\5d"+
		"\63\2\u0bd7\u0bd5\3\2\2\2\u0bd8\u0bdb\3\2\2\2\u0bd9\u0bd7\3\2\2\2\u0bd9"+
		"\u0bda\3\2\2\2\u0bda\u0bdd\3\2\2\2\u0bdb\u0bd9\3\2\2\2\u0bdc\u0bd2\3\2"+
		"\2\2\u0bdc\u0bdd\3\2\2\2\u0bdd\u0bdf\3\2\2\2\u0bde\u0bbc\3\2\2\2\u0bde"+
		"\u0bd0\3\2\2\2\u0bdf\u0be1\3\2\2\2\u0be0\u0be2\5\u010c\u0087\2\u0be1\u0be0"+
		"\3\2\2\2\u0be1\u0be2\3\2\2\2\u0be2\u0be3\3\2\2\2\u0be3\u0be5\7\5\2\2\u0be4"+
		"\u0bb6\3\2\2\2\u0be4\u0bb7\3\2\2\2\u0be4\u0bbb\3\2\2\2\u0be5\u010b\3\2"+
		"\2\2\u0be6\u0be7\7\u00bc\2\2\u0be7\u0bf7\5\u010e\u0088\2\u0be8\u0be9\7"+
		"\u00d1\2\2\u0be9\u0bf7\5\u010e\u0088\2\u0bea\u0beb\7\u00bc\2\2\u0beb\u0bec"+
		"\7\36\2\2\u0bec\u0bed\5\u010e\u0088\2\u0bed\u0bee\7\25\2\2\u0bee\u0bef"+
		"\5\u010e\u0088\2\u0bef\u0bf7\3\2\2\2\u0bf0\u0bf1\7\u00d1\2\2\u0bf1\u0bf2"+
		"\7\36\2\2\u0bf2\u0bf3\5\u010e\u0088\2\u0bf3\u0bf4\7\25\2\2\u0bf4\u0bf5"+
		"\5\u010e\u0088\2\u0bf5\u0bf7\3\2\2\2\u0bf6\u0be6\3\2\2\2\u0bf6\u0be8\3"+
		"\2\2\2\u0bf6\u0bea\3\2\2\2\u0bf6\u0bf0\3\2\2\2\u0bf7\u010d\3\2\2\2\u0bf8"+
		"\u0bf9\7\u00fe\2\2\u0bf9\u0c00\t/\2\2\u0bfa\u0bfb\7<\2\2\u0bfb\u0c00\7"+
		"\u00d0\2\2\u0bfc\u0bfd\5\u00d2j\2\u0bfd\u0bfe\t/\2\2\u0bfe\u0c00\3\2\2"+
		"\2\u0bff\u0bf8\3\2\2\2\u0bff\u0bfa\3\2\2\2\u0bff\u0bfc\3\2\2\2\u0c00\u010f"+
		"\3\2\2\2\u0c01\u0c06\5\u0114\u008b\2\u0c02\u0c03\7\4\2\2\u0c03\u0c05\5"+
		"\u0114\u008b\2\u0c04\u0c02\3\2\2\2\u0c05\u0c08\3\2\2\2\u0c06\u0c04\3\2"+
		"\2\2\u0c06\u0c07\3\2\2\2\u0c07\u0111\3\2\2\2\u0c08\u0c06\3\2\2\2\u0c09"+
		"\u0c0e\5\u0114\u008b\2\u0c0a\u0c0e\7a\2\2\u0c0b\u0c0e\7\u0088\2\2\u0c0c"+
		"\u0c0e\7\u00ca\2\2\u0c0d\u0c09\3\2\2\2\u0c0d\u0c0a\3\2\2\2\u0c0d\u0c0b"+
		"\3\2\2\2\u0c0d\u0c0c\3\2\2\2\u0c0e\u0113\3\2\2\2\u0c0f\u0c14\5\u011a\u008e"+
		"\2\u0c10\u0c11\7\7\2\2\u0c11\u0c13\5\u011a\u008e\2\u0c12\u0c10\3\2\2\2"+
		"\u0c13\u0c16\3\2\2\2\u0c14\u0c12\3\2\2\2\u0c14\u0c15\3\2\2\2\u0c15\u0115"+
		"\3\2\2\2\u0c16\u0c14\3\2\2\2\u0c17\u0c18\5\u011a\u008e\2\u0c18\u0c19\5"+
		"\u0118\u008d\2\u0c19\u0117\3\2\2\2\u0c1a\u0c1b\7\u011b\2\2\u0c1b\u0c1d"+
		"\5\u011a\u008e\2\u0c1c\u0c1a\3\2\2\2\u0c1d\u0c1e\3\2\2\2\u0c1e\u0c1c\3"+
		"\2\2\2\u0c1e\u0c1f\3\2\2\2\u0c1f\u0c22\3\2\2\2\u0c20\u0c22\3\2\2\2\u0c21"+
		"\u0c1c\3\2\2\2\u0c21\u0c20\3\2\2\2\u0c22\u0119\3\2\2\2\u0c23\u0c27\5\u011c"+
		"\u008f\2\u0c24\u0c25\6\u008e\22\2\u0c25\u0c27\5\u0126\u0094\2\u0c26\u0c23"+
		"\3\2\2\2\u0c26\u0c24\3\2\2\2\u0c27\u011b\3\2\2\2\u0c28\u0c2f\7\u012e\2"+
		"\2\u0c29\u0c2f\5\u011e\u0090\2\u0c2a\u0c2b\6\u008f\23\2\u0c2b\u0c2f\5"+
		"\u0124\u0093\2\u0c2c\u0c2d\6\u008f\24\2\u0c2d\u0c2f\5\u0128\u0095\2\u0c2e"+
		"\u0c28\3\2\2\2\u0c2e\u0c29\3\2\2\2\u0c2e\u0c2a\3\2\2\2\u0c2e\u0c2c\3\2"+
		"\2\2\u0c2f\u011d\3\2\2\2\u0c30\u0c31\7\u012f\2\2\u0c31\u011f\3\2\2\2\u0c32"+
		"\u0c34\6\u0091\25\2\u0c33\u0c35\7\u011b\2\2\u0c34\u0c33\3\2\2\2\u0c34"+
		"\u0c35\3\2\2\2\u0c35\u0c36\3\2\2\2\u0c36\u0c5e\7\u0129\2\2\u0c37\u0c39"+
		"\6\u0091\26\2\u0c38\u0c3a\7\u011b\2\2\u0c39\u0c38\3\2\2\2\u0c39\u0c3a"+
		"\3\2\2\2\u0c3a\u0c3b\3\2\2\2\u0c3b\u0c5e\7\u012a\2\2\u0c3c\u0c3e\6\u0091"+
		"\27\2\u0c3d\u0c3f\7\u011b\2\2\u0c3e\u0c3d\3\2\2\2\u0c3e\u0c3f\3\2\2\2"+
		"\u0c3f\u0c40\3\2\2\2\u0c40\u0c5e\t\60\2\2\u0c41\u0c43\7\u011b\2\2\u0c42"+
		"\u0c41\3\2\2\2\u0c42\u0c43\3\2\2\2\u0c43\u0c44\3\2\2\2\u0c44\u0c5e\7\u0128"+
		"\2\2\u0c45\u0c47\7\u011b\2\2\u0c46\u0c45\3\2\2\2\u0c46\u0c47\3\2\2\2\u0c47"+
		"\u0c48\3\2\2\2\u0c48\u0c5e\7\u0125\2\2\u0c49\u0c4b\7\u011b\2\2\u0c4a\u0c49"+
		"\3\2\2\2\u0c4a\u0c4b\3\2\2\2\u0c4b\u0c4c\3\2\2\2\u0c4c\u0c5e\7\u0126\2"+
		"\2\u0c4d\u0c4f\7\u011b\2\2\u0c4e\u0c4d\3\2\2\2\u0c4e\u0c4f\3\2\2\2\u0c4f"+
		"\u0c50\3\2\2\2\u0c50\u0c5e\7\u0127\2\2\u0c51\u0c53\7\u011b\2\2\u0c52\u0c51"+
		"\3\2\2\2\u0c52\u0c53\3\2\2\2\u0c53\u0c54\3\2\2\2\u0c54\u0c5e\7\u012c\2"+
		"\2\u0c55\u0c57\7\u011b\2\2\u0c56\u0c55\3\2\2\2\u0c56\u0c57\3\2\2\2\u0c57"+
		"\u0c58\3\2\2\2\u0c58\u0c5e\7\u012b\2\2\u0c59\u0c5b\7\u011b\2\2\u0c5a\u0c59"+
		"\3\2\2\2\u0c5a\u0c5b\3\2\2\2\u0c5b\u0c5c\3\2\2\2\u0c5c\u0c5e\7\u012d\2"+
		"\2\u0c5d\u0c32\3\2\2\2\u0c5d\u0c37\3\2\2\2\u0c5d\u0c3c\3\2\2\2\u0c5d\u0c42"+
		"\3\2\2\2\u0c5d\u0c46\3\2\2\2\u0c5d\u0c4a\3\2\2\2\u0c5d\u0c4e\3\2\2\2\u0c5d"+
		"\u0c52\3\2\2\2\u0c5d\u0c56\3\2\2\2\u0c5d\u0c5a\3\2\2\2\u0c5e\u0121\3\2"+
		"\2\2\u0c5f\u0c60\7\u00fc\2\2\u0c60\u0c67\5\u00f6|\2\u0c61\u0c67\5,\27"+
		"\2\u0c62\u0c67\5\u00f4{\2\u0c63\u0c64\t\61\2\2\u0c64\u0c65\7\u009e\2\2"+
		"\u0c65\u0c67\7\u009f\2\2\u0c66\u0c5f\3\2\2\2\u0c66\u0c61\3\2\2\2\u0c66"+
		"\u0c62\3\2\2\2\u0c66\u0c63\3\2\2\2\u0c67\u0123\3\2\2\2\u0c68\u0c69\t\62"+
		"\2\2\u0c69\u0125\3\2\2\2\u0c6a\u0c6b\t\63\2\2\u0c6b\u0127\3\2\2\2\u0c6c"+
		"\u0c6d\t\64\2\2\u0c6d\u0129\3\2\2\2\u01a5\u0133\u0136\u0140\u0145\u014b"+
		"\u0164\u0169\u0171\u0179\u017b\u018f\u0193\u0199\u019c\u019f\u01a6\u01a9"+
		"\u01ad\u01b0\u01b7\u01c2\u01c4\u01cc\u01cf\u01d3\u01d6\u01dc\u01e7\u01ed"+
		"\u01f2\u0232\u023b\u023f\u0245\u0249\u024e\u0254\u0260\u0268\u026e\u027b"+
		"\u0280\u0290\u0297\u029b\u02a1\u02b0\u02b4\u02ba\u02c0\u02c3\u02c6\u02cc"+
		"\u02d0\u02d8\u02da\u02e3\u02e6\u02ef\u02f4\u02fa\u0301\u0304\u030a\u0315"+
		"\u0318\u031c\u0321\u0326\u032d\u0330\u0333\u033a\u033f\u0348\u0350\u0356"+
		"\u0359\u035c\u0362\u0366\u036a\u036e\u0370\u0378\u0380\u0386\u038c\u038f"+
		"\u0393\u0396\u039a\u03b6\u03b9\u03bd\u03c3\u03c6\u03c9\u03cf\u03d7\u03dc"+
		"\u03e2\u03e8\u03f0\u03f7\u03ff\u0410\u041e\u0421\u0427\u0430\u0439\u0440"+
		"\u0443\u044f\u0453\u045a\u04ce\u04d6\u04de\u04e7\u04f1\u04f5\u04f8\u04fe"+
		"\u0504\u0510\u051c\u0521\u052a\u0532\u0539\u053b\u053e\u0543\u0547\u054c"+
		"\u054f\u0554\u0559\u055c\u0561\u0565\u056a\u056c\u0570\u0579\u0581\u058a"+
		"\u0591\u059a\u059f\u05a2\u05b8\u05ba\u05c3\u05ca\u05cd\u05d4\u05d8\u05de"+
		"\u05e6\u05f1\u05fc\u0603\u0609\u0616\u061d\u0624\u0630\u0638\u063e\u0641"+
		"\u064a\u064d\u0656\u0659\u0662\u0665\u066e\u0671\u0674\u0679\u067b\u0687"+
		"\u068e\u0695\u0698\u069a\u06a6\u06aa\u06ae\u06b4\u06b8\u06c0\u06c4\u06c7"+
		"\u06ca\u06cd\u06d1\u06d5\u06da\u06de\u06e1\u06e4\u06e7\u06eb\u06f0\u06f4"+
		"\u06f7\u06fa\u06fd\u06ff\u0705\u070c\u0711\u0714\u0717\u071b\u0725\u0729"+
		"\u072b\u072e\u0732\u0738\u073c\u0747\u0751\u075d\u076c\u0771\u0778\u0788"+
		"\u078d\u079a\u079f\u07a7\u07ad\u07b1\u07ba\u07c4\u07d3\u07d8\u07da\u07de"+
		"\u07e7\u07f4\u07f9\u07fd\u0805\u0808\u080c\u081a\u0827\u082c\u0830\u0833"+
		"\u0838\u0841\u0844\u0849\u0850\u0853\u0858\u085e\u0864\u0868\u086e\u0872"+
		"\u0875\u087a\u087d\u0882\u0886\u0889\u088c\u0892\u0897\u089c\u08ae\u08b0"+
		"\u08b3\u08be\u08c7\u08ce\u08d6\u08dd\u08e1\u08e9\u08f1\u08f7\u08ff\u090b"+
		"\u090e\u0914\u0918\u091a\u0923\u092f\u0931\u0938\u093f\u0945\u094b\u094d"+
		"\u0954\u095c\u0962\u0969\u096f\u0973\u0975\u097c\u0985\u098c\u0996\u099b"+
		"\u099f\u09a8\u09b5\u09b7\u09bf\u09c1\u09c5\u09cd\u09d6\u09dc\u09e4\u09e9"+
		"\u09f5\u09fa\u09fd\u0a03\u0a07\u0a0c\u0a11\u0a16\u0a1c\u0a31\u0a33\u0a3c"+
		"\u0a40\u0a49\u0a4d\u0a5f\u0a62\u0a6a\u0a73\u0a8a\u0a95\u0a9c\u0a9f\u0aa8"+
		"\u0aac\u0ab0\u0abc\u0ad5\u0adc\u0adf\u0aee\u0af2\u0afc\u0afe\u0b0b\u0b0d"+
		"\u0b1a\u0b1e\u0b25\u0b2a\u0b32\u0b39\u0b4a\u0b4e\u0b54\u0b5a\u0b63\u0b67"+
		"\u0b69\u0b70\u0b77\u0b7a\u0b7d\u0b84\u0b8b\u0b8e\u0b95\u0b9a\u0b9f\u0ba2"+
		"\u0baf\u0bc3\u0bcd\u0bd0\u0bd9\u0bdc\u0bde\u0be1\u0be4\u0bf6\u0bff\u0c06"+
		"\u0c0d\u0c14\u0c1e\u0c21\u0c26\u0c2e\u0c34\u0c39\u0c3e\u0c42\u0c46\u0c4a"+
		"\u0c4e\u0c52\u0c56\u0c5a\u0c5d\u0c66";
	public static final String _serializedATN = Utils.join(
		new String[] {
			_serializedATNSegment0,
			_serializedATNSegment1
		},
		""
	);
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}