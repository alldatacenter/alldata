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

// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlCommand.g4 by ANTLR 4.7
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
public class ArcticSqlCommandParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, KEY=12, MIGRATE=13, ARCTIC=14, ADD=15, AFTER=16, ALL=17, 
		ALTER=18, ANALYZE=19, AND=20, ANTI=21, ANY=22, ARCHIVE=23, ARRAY=24, AS=25, 
		ASC=26, AT=27, AUTHORIZATION=28, BETWEEN=29, BOTH=30, BUCKET=31, BUCKETS=32, 
		BY=33, CACHE=34, CASCADE=35, CASE=36, CAST=37, CHANGE=38, CHECK=39, CLEAR=40, 
		CLUSTER=41, CLUSTERED=42, CODEGEN=43, COLLATE=44, COLLECTION=45, COLUMN=46, 
		COLUMNS=47, COMMENT=48, COMMIT=49, COMPACT=50, COMPACTIONS=51, COMPUTE=52, 
		CONCATENATE=53, CONSTRAINT=54, COST=55, CREATE=56, CROSS=57, CUBE=58, 
		CURRENT=59, CURRENT_DATE=60, CURRENT_TIME=61, CURRENT_TIMESTAMP=62, CURRENT_USER=63, 
		DAY=64, DATA=65, DATABASE=66, DATABASES=67, DBPROPERTIES=68, DEFINED=69, 
		DELETE=70, DELIMITED=71, DESC=72, DESCRIBE=73, DFS=74, DIRECTORIES=75, 
		DIRECTORY=76, DISTINCT=77, DISTRIBUTE=78, DIV=79, DROP=80, ELSE=81, END=82, 
		ESCAPE=83, ESCAPED=84, EXCEPT=85, EXCHANGE=86, EXISTS=87, EXPLAIN=88, 
		EXPORT=89, EXTENDED=90, EXTERNAL=91, EXTRACT=92, FALSE=93, FETCH=94, FIELDS=95, 
		FILTER=96, FILEFORMAT=97, FIRST=98, FOLLOWING=99, FOR=100, FOREIGN=101, 
		FORMAT=102, FORMATTED=103, FROM=104, FULL=105, FUNCTION=106, FUNCTIONS=107, 
		GLOBAL=108, GRANT=109, GROUP=110, GROUPING=111, HAVING=112, HOUR=113, 
		IF=114, IGNORE=115, IMPORT=116, IN=117, INDEX=118, INDEXES=119, INNER=120, 
		INPATH=121, INPUTFORMAT=122, INSERT=123, INTERSECT=124, INTERVAL=125, 
		INTO=126, IS=127, ITEMS=128, JOIN=129, KEYS=130, LAST=131, LATERAL=132, 
		LAZY=133, LEADING=134, LEFT=135, LIKE=136, LIMIT=137, LINES=138, LIST=139, 
		LOAD=140, LOCAL=141, LOCATION=142, LOCK=143, LOCKS=144, LOGICAL=145, MACRO=146, 
		MAP=147, MATCHED=148, MERGE=149, MINUTE=150, MONTH=151, MSCK=152, NAMESPACE=153, 
		NAMESPACES=154, NATURAL=155, NO=156, NOT=157, NULL=158, NULLS=159, OF=160, 
		ON=161, ONLY=162, OPTION=163, OPTIONS=164, OR=165, ORDER=166, OUT=167, 
		OUTER=168, OUTPUTFORMAT=169, OVER=170, OVERLAPS=171, OVERLAY=172, OVERWRITE=173, 
		PARTITION=174, PARTITIONED=175, PARTITIONS=176, PERCENTLIT=177, PIVOT=178, 
		PLACING=179, POSITION=180, PRECEDING=181, PRIMARY=182, PRINCIPALS=183, 
		PROPERTIES=184, PURGE=185, QUERY=186, RANGE=187, RECORDREADER=188, RECORDWRITER=189, 
		RECOVER=190, REDUCE=191, REFERENCES=192, REFRESH=193, RENAME=194, REPAIR=195, 
		REPLACE=196, RESET=197, RESPECT=198, RESTRICT=199, REVOKE=200, RIGHT=201, 
		RLIKE=202, ROLE=203, ROLES=204, ROLLBACK=205, ROLLUP=206, ROW=207, ROWS=208, 
		SECOND=209, SCHEMA=210, SELECT=211, SEMI=212, SEPARATED=213, SERDE=214, 
		SERDEPROPERTIES=215, SESSION_USER=216, SET=217, SETMINUS=218, SETS=219, 
		SHOW=220, SKEWED=221, SOME=222, SORT=223, SORTED=224, START=225, STATISTICS=226, 
		STORED=227, STRATIFY=228, STRUCT=229, SUBSTR=230, SUBSTRING=231, SYNC=232, 
		TABLE=233, TABLES=234, TABLESAMPLE=235, TBLPROPERTIES=236, TEMPORARY=237, 
		TERMINATED=238, THEN=239, TIME=240, TO=241, TOUCH=242, TRAILING=243, TRANSACTION=244, 
		TRANSACTIONS=245, TRANSFORM=246, TRIM=247, TRUE=248, TRUNCATE=249, TRY_CAST=250, 
		TYPE=251, UNARCHIVE=252, UNBOUNDED=253, UNCACHE=254, UNION=255, UNIQUE=256, 
		UNKNOWN=257, UNLOCK=258, UNSET=259, UPDATE=260, USE=261, USER=262, USING=263, 
		VALUES=264, VIEW=265, VIEWS=266, WHEN=267, WHERE=268, WINDOW=269, WITH=270, 
		YEAR=271, ZONE=272, EQ=273, NSEQ=274, NEQ=275, NEQJ=276, LT=277, LTE=278, 
		GT=279, GTE=280, PLUS=281, MINUS=282, ASTERISK=283, SLASH=284, PERCENT=285, 
		TILDE=286, AMPERSAND=287, PIPE=288, CONCAT_PIPE=289, HAT=290, STRING=291, 
		BIGINT_LITERAL=292, SMALLINT_LITERAL=293, TINYINT_LITERAL=294, INTEGER_VALUE=295, 
		EXPONENT_VALUE=296, DECIMAL_VALUE=297, FLOAT_LITERAL=298, DOUBLE_LITERAL=299, 
		BIGDECIMAL_LITERAL=300, IDENTIFIER=301, BACKQUOTED_IDENTIFIER=302, SIMPLE_COMMENT=303, 
		BRACKETED_COMMENT=304, WS=305, UNRECOGNIZED=306;
	public static final int
		RULE_arcticCommand = 0, RULE_arcticStatement = 1, RULE_singleStatement = 2, 
		RULE_singleExpression = 3, RULE_singleTableIdentifier = 4, RULE_singleMultipartIdentifier = 5, 
		RULE_singleFunctionIdentifier = 6, RULE_singleDataType = 7, RULE_singleTableSchema = 8, 
		RULE_statement = 9, RULE_configKey = 10, RULE_configValue = 11, RULE_unsupportedHiveNativeCommands = 12, 
		RULE_createTableHeader = 13, RULE_replaceTableHeader = 14, RULE_bucketSpec = 15, 
		RULE_skewSpec = 16, RULE_locationSpec = 17, RULE_commentSpec = 18, RULE_query = 19, 
		RULE_insertInto = 20, RULE_partitionSpecLocation = 21, RULE_partitionSpec = 22, 
		RULE_partitionVal = 23, RULE_namespace = 24, RULE_describeFuncName = 25, 
		RULE_describeColName = 26, RULE_ctes = 27, RULE_namedQuery = 28, RULE_tableProvider = 29, 
		RULE_createTableClauses = 30, RULE_tablePropertyList = 31, RULE_tableProperty = 32, 
		RULE_tablePropertyKey = 33, RULE_tablePropertyValue = 34, RULE_constantList = 35, 
		RULE_nestedConstantList = 36, RULE_createFileFormat = 37, RULE_fileFormat = 38, 
		RULE_storageHandler = 39, RULE_resource = 40, RULE_dmlStatementNoWith = 41, 
		RULE_queryOrganization = 42, RULE_multiInsertQueryBody = 43, RULE_queryTerm = 44, 
		RULE_queryPrimary = 45, RULE_sortItem = 46, RULE_fromStatement = 47, RULE_fromStatementBody = 48, 
		RULE_querySpecification = 49, RULE_transformClause = 50, RULE_selectClause = 51, 
		RULE_setClause = 52, RULE_matchedClause = 53, RULE_notMatchedClause = 54, 
		RULE_matchedAction = 55, RULE_notMatchedAction = 56, RULE_assignmentList = 57, 
		RULE_assignment = 58, RULE_whereClause = 59, RULE_havingClause = 60, RULE_hint = 61, 
		RULE_hintStatement = 62, RULE_fromClause = 63, RULE_aggregationClause = 64, 
		RULE_groupByClause = 65, RULE_groupingAnalytics = 66, RULE_groupingElement = 67, 
		RULE_groupingSet = 68, RULE_pivotClause = 69, RULE_pivotColumn = 70, RULE_pivotValue = 71, 
		RULE_lateralView = 72, RULE_setQuantifier = 73, RULE_relation = 74, RULE_joinRelation = 75, 
		RULE_joinType = 76, RULE_joinCriteria = 77, RULE_sample = 78, RULE_sampleMethod = 79, 
		RULE_identifierList = 80, RULE_identifierSeq = 81, RULE_orderedIdentifierList = 82, 
		RULE_orderedIdentifier = 83, RULE_identifierCommentList = 84, RULE_identifierComment = 85, 
		RULE_relationPrimary = 86, RULE_inlineTable = 87, RULE_functionTable = 88, 
		RULE_tableAlias = 89, RULE_rowFormat = 90, RULE_multipartIdentifierList = 91, 
		RULE_multipartIdentifier = 92, RULE_tableIdentifier = 93, RULE_functionIdentifier = 94, 
		RULE_namedExpression = 95, RULE_namedExpressionSeq = 96, RULE_partitionFieldList = 97, 
		RULE_partitionField = 98, RULE_transform = 99, RULE_transformArgument = 100, 
		RULE_expression = 101, RULE_expressionSeq = 102, RULE_booleanExpression = 103, 
		RULE_predicate = 104, RULE_valueExpression = 105, RULE_primaryExpression = 106, 
		RULE_constant = 107, RULE_comparisonOperator = 108, RULE_arithmeticOperator = 109, 
		RULE_predicateOperator = 110, RULE_booleanValue = 111, RULE_interval = 112, 
		RULE_errorCapturingMultiUnitsInterval = 113, RULE_multiUnitsInterval = 114, 
		RULE_errorCapturingUnitToUnitInterval = 115, RULE_unitToUnitInterval = 116, 
		RULE_intervalValue = 117, RULE_colPosition = 118, RULE_dataType = 119, 
		RULE_qualifiedColTypeWithPositionList = 120, RULE_qualifiedColTypeWithPosition = 121, 
		RULE_colTypeList = 122, RULE_colType = 123, RULE_complexColTypeList = 124, 
		RULE_complexColType = 125, RULE_whenClause = 126, RULE_windowClause = 127, 
		RULE_namedWindow = 128, RULE_windowSpec = 129, RULE_windowFrame = 130, 
		RULE_frameBound = 131, RULE_qualifiedNameList = 132, RULE_functionName = 133, 
		RULE_qualifiedName = 134, RULE_errorCapturingIdentifier = 135, RULE_errorCapturingIdentifierExtra = 136, 
		RULE_identifier = 137, RULE_strictIdentifier = 138, RULE_quotedIdentifier = 139, 
		RULE_number = 140, RULE_alterColumnAction = 141, RULE_ansiNonReserved = 142, 
		RULE_strictNonReserved = 143, RULE_nonReserved = 144;
	public static final String[] ruleNames = {
		"arcticCommand", "arcticStatement", "singleStatement", "singleExpression", 
		"singleTableIdentifier", "singleMultipartIdentifier", "singleFunctionIdentifier", 
		"singleDataType", "singleTableSchema", "statement", "configKey", "configValue", 
		"unsupportedHiveNativeCommands", "createTableHeader", "replaceTableHeader", 
		"bucketSpec", "skewSpec", "locationSpec", "commentSpec", "query", "insertInto", 
		"partitionSpecLocation", "partitionSpec", "partitionVal", "namespace", 
		"describeFuncName", "describeColName", "ctes", "namedQuery", "tableProvider", 
		"createTableClauses", "tablePropertyList", "tableProperty", "tablePropertyKey", 
		"tablePropertyValue", "constantList", "nestedConstantList", "createFileFormat", 
		"fileFormat", "storageHandler", "resource", "dmlStatementNoWith", "queryOrganization", 
		"multiInsertQueryBody", "queryTerm", "queryPrimary", "sortItem", "fromStatement", 
		"fromStatementBody", "querySpecification", "transformClause", "selectClause", 
		"setClause", "matchedClause", "notMatchedClause", "matchedAction", "notMatchedAction", 
		"assignmentList", "assignment", "whereClause", "havingClause", "hint", 
		"hintStatement", "fromClause", "aggregationClause", "groupByClause", "groupingAnalytics", 
		"groupingElement", "groupingSet", "pivotClause", "pivotColumn", "pivotValue", 
		"lateralView", "setQuantifier", "relation", "joinRelation", "joinType", 
		"joinCriteria", "sample", "sampleMethod", "identifierList", "identifierSeq", 
		"orderedIdentifierList", "orderedIdentifier", "identifierCommentList", 
		"identifierComment", "relationPrimary", "inlineTable", "functionTable", 
		"tableAlias", "rowFormat", "multipartIdentifierList", "multipartIdentifier", 
		"tableIdentifier", "functionIdentifier", "namedExpression", "namedExpressionSeq", 
		"partitionFieldList", "partitionField", "transform", "transformArgument", 
		"expression", "expressionSeq", "booleanExpression", "predicate", "valueExpression", 
		"primaryExpression", "constant", "comparisonOperator", "arithmeticOperator", 
		"predicateOperator", "booleanValue", "interval", "errorCapturingMultiUnitsInterval", 
		"multiUnitsInterval", "errorCapturingUnitToUnitInterval", "unitToUnitInterval", 
		"intervalValue", "colPosition", "dataType", "qualifiedColTypeWithPositionList", 
		"qualifiedColTypeWithPosition", "colTypeList", "colType", "complexColTypeList", 
		"complexColType", "whenClause", "windowClause", "namedWindow", "windowSpec", 
		"windowFrame", "frameBound", "qualifiedNameList", "functionName", "qualifiedName", 
		"errorCapturingIdentifier", "errorCapturingIdentifierExtra", "identifier", 
		"strictIdentifier", "quotedIdentifier", "number", "alterColumnAction", 
		"ansiNonReserved", "strictNonReserved", "nonReserved"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'('", "')'", "','", "'.'", "'/*+'", "'*/'", "'->'", "'['", 
		"']'", "':'", "'KEY'", "'MIGRATE'", "'ARCTIC'", "'ADD'", "'AFTER'", "'ALL'", 
		"'ALTER'", "'ANALYZE'", "'AND'", "'ANTI'", "'ANY'", "'ARCHIVE'", "'ARRAY'", 
		"'AS'", "'ASC'", "'AT'", "'AUTHORIZATION'", "'BETWEEN'", "'BOTH'", "'BUCKET'", 
		"'BUCKETS'", "'BY'", "'CACHE'", "'CASCADE'", "'CASE'", "'CAST'", "'CHANGE'", 
		"'CHECK'", "'CLEAR'", "'CLUSTER'", "'CLUSTERED'", "'CODEGEN'", "'COLLATE'", 
		"'COLLECTION'", "'COLUMN'", "'COLUMNS'", "'COMMENT'", "'COMMIT'", "'COMPACT'", 
		"'COMPACTIONS'", "'COMPUTE'", "'CONCATENATE'", "'CONSTRAINT'", "'COST'", 
		"'CREATE'", "'CROSS'", "'CUBE'", "'CURRENT'", "'CURRENT_DATE'", "'CURRENT_TIME'", 
		"'CURRENT_TIMESTAMP'", "'CURRENT_USER'", "'DAY'", "'DATA'", "'DATABASE'", 
		null, "'DBPROPERTIES'", "'DEFINED'", "'DELETE'", "'DELIMITED'", "'DESC'", 
		"'DESCRIBE'", "'DFS'", "'DIRECTORIES'", "'DIRECTORY'", "'DISTINCT'", "'DISTRIBUTE'", 
		"'DIV'", "'DROP'", "'ELSE'", "'END'", "'ESCAPE'", "'ESCAPED'", "'EXCEPT'", 
		"'EXCHANGE'", "'EXISTS'", "'EXPLAIN'", "'EXPORT'", "'EXTENDED'", "'EXTERNAL'", 
		"'EXTRACT'", "'FALSE'", "'FETCH'", "'FIELDS'", "'FILTER'", "'FILEFORMAT'", 
		"'FIRST'", "'FOLLOWING'", "'FOR'", "'FOREIGN'", "'FORMAT'", "'FORMATTED'", 
		"'FROM'", "'FULL'", "'FUNCTION'", "'FUNCTIONS'", "'GLOBAL'", "'GRANT'", 
		"'GROUP'", "'GROUPING'", "'HAVING'", "'HOUR'", "'IF'", "'IGNORE'", "'IMPORT'", 
		"'IN'", "'INDEX'", "'INDEXES'", "'INNER'", "'INPATH'", "'INPUTFORMAT'", 
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
		"'!='", "'<'", null, "'>'", null, "'+'", "'-'", "'*'", "'/'", "'%'", "'~'", 
		"'&'", "'|'", "'||'", "'^'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		"KEY", "MIGRATE", "ARCTIC", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", 
		"AND", "ANTI", "ANY", "ARCHIVE", "ARRAY", "AS", "ASC", "AT", "AUTHORIZATION", 
		"BETWEEN", "BOTH", "BUCKET", "BUCKETS", "BY", "CACHE", "CASCADE", "CASE", 
		"CAST", "CHANGE", "CHECK", "CLEAR", "CLUSTER", "CLUSTERED", "CODEGEN", 
		"COLLATE", "COLLECTION", "COLUMN", "COLUMNS", "COMMENT", "COMMIT", "COMPACT", 
		"COMPACTIONS", "COMPUTE", "CONCATENATE", "CONSTRAINT", "COST", "CREATE", 
		"CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", 
		"CURRENT_USER", "DAY", "DATA", "DATABASE", "DATABASES", "DBPROPERTIES", 
		"DEFINED", "DELETE", "DELIMITED", "DESC", "DESCRIBE", "DFS", "DIRECTORIES", 
		"DIRECTORY", "DISTINCT", "DISTRIBUTE", "DIV", "DROP", "ELSE", "END", "ESCAPE", 
		"ESCAPED", "EXCEPT", "EXCHANGE", "EXISTS", "EXPLAIN", "EXPORT", "EXTENDED", 
		"EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FIELDS", "FILTER", "FILEFORMAT", 
		"FIRST", "FOLLOWING", "FOR", "FOREIGN", "FORMAT", "FORMATTED", "FROM", 
		"FULL", "FUNCTION", "FUNCTIONS", "GLOBAL", "GRANT", "GROUP", "GROUPING", 
		"HAVING", "HOUR", "IF", "IGNORE", "IMPORT", "IN", "INDEX", "INDEXES", 
		"INNER", "INPATH", "INPUTFORMAT", "INSERT", "INTERSECT", "INTERVAL", "INTO", 
		"IS", "ITEMS", "JOIN", "KEYS", "LAST", "LATERAL", "LAZY", "LEADING", "LEFT", 
		"LIKE", "LIMIT", "LINES", "LIST", "LOAD", "LOCAL", "LOCATION", "LOCK", 
		"LOCKS", "LOGICAL", "MACRO", "MAP", "MATCHED", "MERGE", "MINUTE", "MONTH", 
		"MSCK", "NAMESPACE", "NAMESPACES", "NATURAL", "NO", "NOT", "NULL", "NULLS", 
		"OF", "ON", "ONLY", "OPTION", "OPTIONS", "OR", "ORDER", "OUT", "OUTER", 
		"OUTPUTFORMAT", "OVER", "OVERLAPS", "OVERLAY", "OVERWRITE", "PARTITION", 
		"PARTITIONED", "PARTITIONS", "PERCENTLIT", "PIVOT", "PLACING", "POSITION", 
		"PRECEDING", "PRIMARY", "PRINCIPALS", "PROPERTIES", "PURGE", "QUERY", 
		"RANGE", "RECORDREADER", "RECORDWRITER", "RECOVER", "REDUCE", "REFERENCES", 
		"REFRESH", "RENAME", "REPAIR", "REPLACE", "RESET", "RESPECT", "RESTRICT", 
		"REVOKE", "RIGHT", "RLIKE", "ROLE", "ROLES", "ROLLBACK", "ROLLUP", "ROW", 
		"ROWS", "SECOND", "SCHEMA", "SELECT", "SEMI", "SEPARATED", "SERDE", "SERDEPROPERTIES", 
		"SESSION_USER", "SET", "SETMINUS", "SETS", "SHOW", "SKEWED", "SOME", "SORT", 
		"SORTED", "START", "STATISTICS", "STORED", "STRATIFY", "STRUCT", "SUBSTR", 
		"SUBSTRING", "SYNC", "TABLE", "TABLES", "TABLESAMPLE", "TBLPROPERTIES", 
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
	public String getGrammarFileName() { return "ArcticSqlCommand.g4"; }

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

	public ArcticSqlCommandParser(TokenStream input) {
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterArcticCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitArcticCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitArcticCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArcticCommandContext arcticCommand() throws RecognitionException {
		ArcticCommandContext _localctx = new ArcticCommandContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_arcticCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(290);
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
	public static class MigrateStatementContext extends ArcticStatementContext {
		public MultipartIdentifierContext source;
		public MultipartIdentifierContext target;
		public TerminalNode MIGRATE() { return getToken(ArcticSqlCommandParser.MIGRATE, 0); }
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
		public TerminalNode ARCTIC() { return getToken(ArcticSqlCommandParser.ARCTIC, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public MigrateStatementContext(ArcticStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMigrateStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMigrateStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMigrateStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArcticStatementContext arcticStatement() throws RecognitionException {
		ArcticStatementContext _localctx = new ArcticStatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_arcticStatement);
		try {
			_localctx = new MigrateStatementContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(292);
			match(MIGRATE);
			setState(293);
			((MigrateStatementContext)_localctx).source = multipartIdentifier();
			setState(294);
			match(TO);
			setState(295);
			match(ARCTIC);
			setState(296);
			((MigrateStatementContext)_localctx).target = multipartIdentifier();
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleStatementContext singleStatement() throws RecognitionException {
		SingleStatementContext _localctx = new SingleStatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_singleStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			statement();
			setState(302);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(299);
				match(T__0);
				}
				}
				setState(304);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(305);
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleExpressionContext singleExpression() throws RecognitionException {
		SingleExpressionContext _localctx = new SingleExpressionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_singleExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(307);
			namedExpression();
			setState(308);
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleTableIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleTableIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleTableIdentifierContext singleTableIdentifier() throws RecognitionException {
		SingleTableIdentifierContext _localctx = new SingleTableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_singleTableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			tableIdentifier();
			setState(311);
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleMultipartIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleMultipartIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleMultipartIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleMultipartIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleMultipartIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleMultipartIdentifierContext singleMultipartIdentifier() throws RecognitionException {
		SingleMultipartIdentifierContext _localctx = new SingleMultipartIdentifierContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_singleMultipartIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(313);
			multipartIdentifier();
			setState(314);
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleFunctionIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleFunctionIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleFunctionIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleFunctionIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleFunctionIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleFunctionIdentifierContext singleFunctionIdentifier() throws RecognitionException {
		SingleFunctionIdentifierContext _localctx = new SingleFunctionIdentifierContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_singleFunctionIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(316);
			functionIdentifier();
			setState(317);
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleDataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleDataType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleDataTypeContext singleDataType() throws RecognitionException {
		SingleDataTypeContext _localctx = new SingleDataTypeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_singleDataType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			dataType();
			setState(320);
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
		public TerminalNode EOF() { return getToken(ArcticSqlCommandParser.EOF, 0); }
		public SingleTableSchemaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleTableSchema; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleTableSchema(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleTableSchema(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleTableSchema(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleTableSchemaContext singleTableSchema() throws RecognitionException {
		SingleTableSchemaContext _localctx = new SingleTableSchemaContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_singleTableSchema);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(322);
			colTypeList();
			setState(323);
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
		public TerminalNode EXPLAIN() { return getToken(ArcticSqlCommandParser.EXPLAIN, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode LOGICAL() { return getToken(ArcticSqlCommandParser.LOGICAL, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlCommandParser.FORMATTED, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSqlCommandParser.CODEGEN, 0); }
		public TerminalNode COST() { return getToken(ArcticSqlCommandParser.COST, 0); }
		public ExplainContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterExplain(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitExplain(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitExplain(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ResetConfigurationContext extends StatementContext {
		public TerminalNode RESET() { return getToken(ArcticSqlCommandParser.RESET, 0); }
		public ResetConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterResetConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitResetConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitResetConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AlterViewQueryContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public AlterViewQueryContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAlterViewQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAlterViewQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAlterViewQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UseContext extends StatementContext {
		public TerminalNode USE() { return getToken(ArcticSqlCommandParser.USE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlCommandParser.NAMESPACE, 0); }
		public UseContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUse(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropNamespaceContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticSqlCommandParser.RESTRICT, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticSqlCommandParser.CASCADE, 0); }
		public DropNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDropNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDropNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDropNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateTempViewUsingContext extends StatementContext {
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticSqlCommandParser.GLOBAL, 0); }
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TerminalNode OPTIONS() { return getToken(ArcticSqlCommandParser.OPTIONS, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public CreateTempViewUsingContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateTempViewUsing(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateTempViewUsing(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateTempViewUsing(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RenameTableContext extends StatementContext {
		public MultipartIdentifierContext from;
		public MultipartIdentifierContext to;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSqlCommandParser.RENAME, 0); }
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public RenameTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRenameTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRenameTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRenameTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FailNativeCommandContext extends StatementContext {
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSqlCommandParser.ROLE, 0); }
		public UnsupportedHiveNativeCommandsContext unsupportedHiveNativeCommands() {
			return getRuleContext(UnsupportedHiveNativeCommandsContext.class,0);
		}
		public FailNativeCommandContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFailNativeCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFailNativeCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFailNativeCommand(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ClearCacheContext extends StatementContext {
		public TerminalNode CLEAR() { return getToken(ArcticSqlCommandParser.CLEAR, 0); }
		public TerminalNode CACHE() { return getToken(ArcticSqlCommandParser.CACHE, 0); }
		public ClearCacheContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterClearCache(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitClearCache(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitClearCache(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropViewContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public DropViewContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDropView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDropView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDropView(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowTablesContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSqlCommandParser.TABLES, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public ShowTablesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowTables(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowTables(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowTables(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RecoverPartitionsContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode RECOVER() { return getToken(ArcticSqlCommandParser.RECOVER, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlCommandParser.PARTITIONS, 0); }
		public RecoverPartitionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRecoverPartitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRecoverPartitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRecoverPartitions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowCurrentNamespaceContext extends StatementContext {
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlCommandParser.CURRENT, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlCommandParser.NAMESPACE, 0); }
		public ShowCurrentNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowCurrentNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowCurrentNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowCurrentNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RenameTablePartitionContext extends StatementContext {
		public PartitionSpecContext from;
		public PartitionSpecContext to;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode RENAME() { return getToken(ArcticSqlCommandParser.RENAME, 0); }
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
		public List<PartitionSpecContext> partitionSpec() {
			return getRuleContexts(PartitionSpecContext.class);
		}
		public PartitionSpecContext partitionSpec(int i) {
			return getRuleContext(PartitionSpecContext.class,i);
		}
		public RenameTablePartitionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRenameTablePartition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRenameTablePartition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRenameTablePartition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RepairTableContext extends StatementContext {
		public Token option;
		public TerminalNode MSCK() { return getToken(ArcticSqlCommandParser.MSCK, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticSqlCommandParser.REPAIR, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlCommandParser.PARTITIONS, 0); }
		public TerminalNode ADD() { return getToken(ArcticSqlCommandParser.ADD, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode SYNC() { return getToken(ArcticSqlCommandParser.SYNC, 0); }
		public RepairTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRepairTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRepairTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRepairTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RefreshResourceContext extends StatementContext {
		public TerminalNode REFRESH() { return getToken(ArcticSqlCommandParser.REFRESH, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public RefreshResourceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRefreshResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRefreshResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRefreshResource(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowCreateTableContext extends StatementContext {
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlCommandParser.SERDE, 0); }
		public ShowCreateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowCreateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowCreateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowCreateTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowNamespacesContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticSqlCommandParser.DATABASES, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticSqlCommandParser.NAMESPACES, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public ShowNamespacesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowNamespaces(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowNamespaces(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowNamespaces(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowColumnsContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext ns;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public List<TerminalNode> FROM() { return getTokens(ArcticSqlCommandParser.FROM); }
		public TerminalNode FROM(int i) {
			return getToken(ArcticSqlCommandParser.FROM, i);
		}
		public List<TerminalNode> IN() { return getTokens(ArcticSqlCommandParser.IN); }
		public TerminalNode IN(int i) {
			return getToken(ArcticSqlCommandParser.IN, i);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowColumns(this);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public ReplaceTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterReplaceTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitReplaceTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitReplaceTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnalyzeTablesContext extends StatementContext {
		public TerminalNode ANALYZE() { return getToken(ArcticSqlCommandParser.ANALYZE, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSqlCommandParser.TABLES, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticSqlCommandParser.COMPUTE, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSqlCommandParser.STATISTICS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public AnalyzeTablesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAnalyzeTables(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAnalyzeTables(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAnalyzeTables(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AddTablePartitionContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode ADD() { return getToken(ArcticSqlCommandParser.ADD, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public List<PartitionSpecLocationContext> partitionSpecLocation() {
			return getRuleContexts(PartitionSpecLocationContext.class);
		}
		public PartitionSpecLocationContext partitionSpecLocation(int i) {
			return getRuleContext(PartitionSpecLocationContext.class,i);
		}
		public AddTablePartitionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAddTablePartition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAddTablePartition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAddTablePartition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetNamespaceLocationContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public LocationSpecContext locationSpec() {
			return getRuleContext(LocationSpecContext.class,0);
		}
		public SetNamespaceLocationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetNamespaceLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetNamespaceLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetNamespaceLocation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RefreshTableContext extends StatementContext {
		public TerminalNode REFRESH() { return getToken(ArcticSqlCommandParser.REFRESH, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public RefreshTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRefreshTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRefreshTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRefreshTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetNamespacePropertiesContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode DBPROPERTIES() { return getToken(ArcticSqlCommandParser.DBPROPERTIES, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticSqlCommandParser.PROPERTIES, 0); }
		public SetNamespacePropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetNamespaceProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetNamespaceProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetNamespaceProperties(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ManageResourceContext extends StatementContext {
		public Token op;
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ADD() { return getToken(ArcticSqlCommandParser.ADD, 0); }
		public TerminalNode LIST() { return getToken(ArcticSqlCommandParser.LIST, 0); }
		public ManageResourceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterManageResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitManageResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitManageResource(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetQuotedConfigurationContext extends StatementContext {
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public ConfigKeyContext configKey() {
			return getRuleContext(ConfigKeyContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticSqlCommandParser.EQ, 0); }
		public ConfigValueContext configValue() {
			return getRuleContext(ConfigValueContext.class,0);
		}
		public SetQuotedConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetQuotedConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetQuotedConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetQuotedConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnalyzeContext extends StatementContext {
		public TerminalNode ANALYZE() { return getToken(ArcticSqlCommandParser.ANALYZE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode COMPUTE() { return getToken(ArcticSqlCommandParser.COMPUTE, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSqlCommandParser.STATISTICS, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode FOR() { return getToken(ArcticSqlCommandParser.FOR, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public TerminalNode ALL() { return getToken(ArcticSqlCommandParser.ALL, 0); }
		public AnalyzeContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAnalyze(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAnalyze(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAnalyze(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateFunctionContext extends StatementContext {
		public Token className;
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlCommandParser.FUNCTION, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public TerminalNode USING() { return getToken(ArcticSqlCommandParser.USING, 0); }
		public List<ResourceContext> resource() {
			return getRuleContexts(ResourceContext.class);
		}
		public ResourceContext resource(int i) {
			return getRuleContext(ResourceContext.class,i);
		}
		public CreateFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HiveReplaceColumnsContext extends StatementContext {
		public MultipartIdentifierContext table;
		public QualifiedColTypeWithPositionListContext columns;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterHiveReplaceColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitHiveReplaceColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitHiveReplaceColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CommentNamespaceContext extends StatementContext {
		public Token comment;
		public TerminalNode COMMENT() { return getToken(ArcticSqlCommandParser.COMMENT, 0); }
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IS() { return getToken(ArcticSqlCommandParser.IS, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public CommentNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCommentNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCommentNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCommentNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ResetQuotedConfigurationContext extends StatementContext {
		public TerminalNode RESET() { return getToken(ArcticSqlCommandParser.RESET, 0); }
		public ConfigKeyContext configKey() {
			return getRuleContext(ConfigKeyContext.class,0);
		}
		public ResetQuotedConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterResetQuotedConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitResetQuotedConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitResetQuotedConfiguration(this);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public CreateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateTable(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDmlStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDmlStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDmlStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateTableLikeContext extends StatementContext {
		public TableIdentifierContext target;
		public TableIdentifierContext source;
		public TablePropertyListContext tableProps;
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public List<TableIdentifierContext> tableIdentifier() {
			return getRuleContexts(TableIdentifierContext.class);
		}
		public TableIdentifierContext tableIdentifier(int i) {
			return getRuleContext(TableIdentifierContext.class,i);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
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
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticSqlCommandParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticSqlCommandParser.TBLPROPERTIES, i);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateTableLike(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateTableLike(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateTableLike(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UncacheTableContext extends StatementContext {
		public TerminalNode UNCACHE() { return getToken(ArcticSqlCommandParser.UNCACHE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public UncacheTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUncacheTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUncacheTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUncacheTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropFunctionContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlCommandParser.FUNCTION, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public DropFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDropFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDropFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDropFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeRelationContext extends StatementContext {
		public Token option;
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlCommandParser.DESCRIBE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public DescribeColNameContext describeColName() {
			return getRuleContext(DescribeColNameContext.class,0);
		}
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlCommandParser.FORMATTED, 0); }
		public DescribeRelationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDescribeRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDescribeRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDescribeRelation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LoadDataContext extends StatementContext {
		public Token path;
		public TerminalNode LOAD() { return getToken(ArcticSqlCommandParser.LOAD, 0); }
		public TerminalNode DATA() { return getToken(ArcticSqlCommandParser.DATA, 0); }
		public TerminalNode INPATH() { return getToken(ArcticSqlCommandParser.INPATH, 0); }
		public TerminalNode INTO() { return getToken(ArcticSqlCommandParser.INTO, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlCommandParser.LOCAL, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlCommandParser.OVERWRITE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public LoadDataContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLoadData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLoadData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLoadData(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowPartitionsContext extends StatementContext {
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlCommandParser.PARTITIONS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public ShowPartitionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowPartitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowPartitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowPartitions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeFunctionContext extends StatementContext {
		public TerminalNode FUNCTION() { return getToken(ArcticSqlCommandParser.FUNCTION, 0); }
		public DescribeFuncNameContext describeFuncName() {
			return getRuleContext(DescribeFuncNameContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlCommandParser.DESCRIBE, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public DescribeFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDescribeFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDescribeFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDescribeFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RenameTableColumnContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext from;
		public ErrorCapturingIdentifierContext to;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSqlCommandParser.RENAME, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSqlCommandParser.COLUMN, 0); }
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRenameTableColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRenameTableColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRenameTableColumn(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterStatementDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitStatementDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitStatementDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HiveChangeColumnContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext colName;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticSqlCommandParser.CHANGE, 0); }
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
		public TerminalNode COLUMN() { return getToken(ArcticSqlCommandParser.COLUMN, 0); }
		public ColPositionContext colPosition() {
			return getRuleContext(ColPositionContext.class,0);
		}
		public HiveChangeColumnContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterHiveChangeColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitHiveChangeColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitHiveChangeColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTimeZoneContext extends StatementContext {
		public Token timezone;
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode TIME() { return getToken(ArcticSqlCommandParser.TIME, 0); }
		public TerminalNode ZONE() { return getToken(ArcticSqlCommandParser.ZONE, 0); }
		public IntervalContext interval() {
			return getRuleContext(IntervalContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlCommandParser.LOCAL, 0); }
		public SetTimeZoneContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetTimeZone(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetTimeZone(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetTimeZone(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescribeQueryContext extends StatementContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlCommandParser.DESCRIBE, 0); }
		public TerminalNode QUERY() { return getToken(ArcticSqlCommandParser.QUERY, 0); }
		public DescribeQueryContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDescribeQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDescribeQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDescribeQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TruncateTableContext extends StatementContext {
		public TerminalNode TRUNCATE() { return getToken(ArcticSqlCommandParser.TRUNCATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TruncateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTruncateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTruncateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTruncateTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTableSerDeContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlCommandParser.SERDE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode WITH() { return getToken(ArcticSqlCommandParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlCommandParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public SetTableSerDeContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetTableSerDe(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetTableSerDe(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetTableSerDe(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateViewContext extends StatementContext {
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public IdentifierCommentListContext identifierCommentList() {
			return getRuleContext(IdentifierCommentListContext.class,0);
		}
		public List<CommentSpecContext> commentSpec() {
			return getRuleContexts(CommentSpecContext.class);
		}
		public CommentSpecContext commentSpec(int i) {
			return getRuleContext(CommentSpecContext.class,i);
		}
		public List<TerminalNode> PARTITIONED() { return getTokens(ArcticSqlCommandParser.PARTITIONED); }
		public TerminalNode PARTITIONED(int i) {
			return getToken(ArcticSqlCommandParser.PARTITIONED, i);
		}
		public List<TerminalNode> ON() { return getTokens(ArcticSqlCommandParser.ON); }
		public TerminalNode ON(int i) {
			return getToken(ArcticSqlCommandParser.ON, i);
		}
		public List<IdentifierListContext> identifierList() {
			return getRuleContexts(IdentifierListContext.class);
		}
		public IdentifierListContext identifierList(int i) {
			return getRuleContext(IdentifierListContext.class,i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticSqlCommandParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticSqlCommandParser.TBLPROPERTIES, i);
		}
		public List<TablePropertyListContext> tablePropertyList() {
			return getRuleContexts(TablePropertyListContext.class);
		}
		public TablePropertyListContext tablePropertyList(int i) {
			return getRuleContext(TablePropertyListContext.class,i);
		}
		public TerminalNode GLOBAL() { return getToken(ArcticSqlCommandParser.GLOBAL, 0); }
		public CreateViewContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateView(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropTablePartitionsContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public List<PartitionSpecContext> partitionSpec() {
			return getRuleContexts(PartitionSpecContext.class);
		}
		public PartitionSpecContext partitionSpec(int i) {
			return getRuleContext(PartitionSpecContext.class,i);
		}
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSqlCommandParser.PURGE, 0); }
		public DropTablePartitionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDropTablePartitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDropTablePartitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDropTablePartitions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetConfigurationContext extends StatementContext {
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public ConfigKeyContext configKey() {
			return getRuleContext(ConfigKeyContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticSqlCommandParser.EQ, 0); }
		public SetConfigurationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetConfiguration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetConfiguration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetConfiguration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropTableContext extends StatementContext {
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSqlCommandParser.PURGE, 0); }
		public DropTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDropTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDropTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDropTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowTableExtendedContext extends StatementContext {
		public MultipartIdentifierContext ns;
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public ShowTableExtendedContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowTableExtended(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowTableExtended(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowTableExtended(this);
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
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlCommandParser.DESCRIBE, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public DescribeNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDescribeNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDescribeNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDescribeNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AlterTableAlterColumnContext extends StatementContext {
		public MultipartIdentifierContext table;
		public MultipartIdentifierContext column;
		public List<TerminalNode> ALTER() { return getTokens(ArcticSqlCommandParser.ALTER); }
		public TerminalNode ALTER(int i) {
			return getToken(ArcticSqlCommandParser.ALTER, i);
		}
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public TerminalNode CHANGE() { return getToken(ArcticSqlCommandParser.CHANGE, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSqlCommandParser.COLUMN, 0); }
		public AlterColumnActionContext alterColumnAction() {
			return getRuleContext(AlterColumnActionContext.class,0);
		}
		public AlterTableAlterColumnContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAlterTableAlterColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAlterTableAlterColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAlterTableAlterColumn(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RefreshFunctionContext extends StatementContext {
		public TerminalNode REFRESH() { return getToken(ArcticSqlCommandParser.REFRESH, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlCommandParser.FUNCTION, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public RefreshFunctionContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRefreshFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRefreshFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRefreshFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CommentTableContext extends StatementContext {
		public Token comment;
		public TerminalNode COMMENT() { return getToken(ArcticSqlCommandParser.COMMENT, 0); }
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IS() { return getToken(ArcticSqlCommandParser.IS, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public CommentTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCommentTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCommentTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCommentTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateNamespaceContext extends StatementContext {
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
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
		public List<TerminalNode> WITH() { return getTokens(ArcticSqlCommandParser.WITH); }
		public TerminalNode WITH(int i) {
			return getToken(ArcticSqlCommandParser.WITH, i);
		}
		public List<TablePropertyListContext> tablePropertyList() {
			return getRuleContexts(TablePropertyListContext.class);
		}
		public TablePropertyListContext tablePropertyList(int i) {
			return getRuleContext(TablePropertyListContext.class,i);
		}
		public List<TerminalNode> DBPROPERTIES() { return getTokens(ArcticSqlCommandParser.DBPROPERTIES); }
		public TerminalNode DBPROPERTIES(int i) {
			return getToken(ArcticSqlCommandParser.DBPROPERTIES, i);
		}
		public List<TerminalNode> PROPERTIES() { return getTokens(ArcticSqlCommandParser.PROPERTIES); }
		public TerminalNode PROPERTIES(int i) {
			return getToken(ArcticSqlCommandParser.PROPERTIES, i);
		}
		public CreateNamespaceContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateNamespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowTblPropertiesContext extends StatementContext {
		public MultipartIdentifierContext table;
		public TablePropertyKeyContext key;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlCommandParser.TBLPROPERTIES, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TablePropertyKeyContext tablePropertyKey() {
			return getRuleContext(TablePropertyKeyContext.class,0);
		}
		public ShowTblPropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowTblProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowTblProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowTblProperties(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnsetTablePropertiesContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode UNSET() { return getToken(ArcticSqlCommandParser.UNSET, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlCommandParser.TBLPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public UnsetTablePropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUnsetTableProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUnsetTableProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUnsetTableProperties(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTableLocationContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public LocationSpecContext locationSpec() {
			return getRuleContext(LocationSpecContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public SetTableLocationContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetTableLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetTableLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetTableLocation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DropTableColumnsContext extends StatementContext {
		public MultipartIdentifierListContext columns;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSqlCommandParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public MultipartIdentifierListContext multipartIdentifierList() {
			return getRuleContext(MultipartIdentifierListContext.class,0);
		}
		public DropTableColumnsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDropTableColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDropTableColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDropTableColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowViewsContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticSqlCommandParser.VIEWS, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public ShowViewsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowViews(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowViews(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowViews(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ShowFunctionsContext extends StatementContext {
		public Token pattern;
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticSqlCommandParser.FUNCTIONS, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public ShowFunctionsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterShowFunctions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitShowFunctions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitShowFunctions(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CacheTableContext extends StatementContext {
		public TablePropertyListContext options;
		public TerminalNode CACHE() { return getToken(ArcticSqlCommandParser.CACHE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode LAZY() { return getToken(ArcticSqlCommandParser.LAZY, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSqlCommandParser.OPTIONS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public CacheTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCacheTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCacheTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCacheTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AddTableColumnsContext extends StatementContext {
		public QualifiedColTypeWithPositionListContext columns;
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode ADD() { return getToken(ArcticSqlCommandParser.ADD, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSqlCommandParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public QualifiedColTypeWithPositionListContext qualifiedColTypeWithPositionList() {
			return getRuleContext(QualifiedColTypeWithPositionListContext.class,0);
		}
		public AddTableColumnsContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAddTableColumns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAddTableColumns(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAddTableColumns(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetTablePropertiesContext extends StatementContext {
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlCommandParser.TBLPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public SetTablePropertiesContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetTableProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetTableProperties(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetTableProperties(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_statement);
		int _la;
		try {
			int _alt;
			setState(1062);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				_localctx = new StatementDefaultContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(325);
				query();
				}
				break;
			case 2:
				_localctx = new DmlStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(327);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WITH) {
					{
					setState(326);
					ctes();
					}
				}

				setState(329);
				dmlStatementNoWith();
				}
				break;
			case 3:
				_localctx = new UseContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(330);
				match(USE);
				setState(332);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
				case 1:
					{
					setState(331);
					match(NAMESPACE);
					}
					break;
				}
				setState(334);
				multipartIdentifier();
				}
				break;
			case 4:
				_localctx = new CreateNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(335);
				match(CREATE);
				setState(336);
				namespace();
				setState(340);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(337);
					match(IF);
					setState(338);
					match(NOT);
					setState(339);
					match(EXISTS);
					}
					break;
				}
				setState(342);
				multipartIdentifier();
				setState(350);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMENT || _la==LOCATION || _la==WITH) {
					{
					setState(348);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case COMMENT:
						{
						setState(343);
						commentSpec();
						}
						break;
					case LOCATION:
						{
						setState(344);
						locationSpec();
						}
						break;
					case WITH:
						{
						{
						setState(345);
						match(WITH);
						setState(346);
						_la = _input.LA(1);
						if ( !(_la==DBPROPERTIES || _la==PROPERTIES) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(347);
						tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(352);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 5:
				_localctx = new SetNamespacePropertiesContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(353);
				match(ALTER);
				setState(354);
				namespace();
				setState(355);
				multipartIdentifier();
				setState(356);
				match(SET);
				setState(357);
				_la = _input.LA(1);
				if ( !(_la==DBPROPERTIES || _la==PROPERTIES) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(358);
				tablePropertyList();
				}
				break;
			case 6:
				_localctx = new SetNamespaceLocationContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(360);
				match(ALTER);
				setState(361);
				namespace();
				setState(362);
				multipartIdentifier();
				setState(363);
				match(SET);
				setState(364);
				locationSpec();
				}
				break;
			case 7:
				_localctx = new DropNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(366);
				match(DROP);
				setState(367);
				namespace();
				setState(370);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
				case 1:
					{
					setState(368);
					match(IF);
					setState(369);
					match(EXISTS);
					}
					break;
				}
				setState(372);
				multipartIdentifier();
				setState(374);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CASCADE || _la==RESTRICT) {
					{
					setState(373);
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
				setState(376);
				match(SHOW);
				setState(377);
				_la = _input.LA(1);
				if ( !(_la==DATABASES || _la==NAMESPACES) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(380);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(378);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(379);
					multipartIdentifier();
					}
				}

				setState(386);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIKE || _la==STRING) {
					{
					setState(383);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LIKE) {
						{
						setState(382);
						match(LIKE);
						}
					}

					setState(385);
					((ShowNamespacesContext)_localctx).pattern = match(STRING);
					}
				}

				}
				break;
			case 9:
				_localctx = new CreateTableContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(388);
				createTableHeader();
				setState(393);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
				case 1:
					{
					setState(389);
					match(T__1);
					setState(390);
					colTypeList();
					setState(391);
					match(T__2);
					}
					break;
				}
				setState(396);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(395);
					tableProvider();
					}
				}

				setState(398);
				createTableClauses();
				setState(403);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1 || _la==AS || _la==FROM || _la==MAP || ((((_la - 191)) & ~0x3f) == 0 && ((1L << (_la - 191)) & ((1L << (REDUCE - 191)) | (1L << (SELECT - 191)) | (1L << (TABLE - 191)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(400);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(399);
						match(AS);
						}
					}

					setState(402);
					query();
					}
				}

				}
				break;
			case 10:
				_localctx = new CreateTableLikeContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(405);
				match(CREATE);
				setState(406);
				match(TABLE);
				setState(410);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
				case 1:
					{
					setState(407);
					match(IF);
					setState(408);
					match(NOT);
					setState(409);
					match(EXISTS);
					}
					break;
				}
				setState(412);
				((CreateTableLikeContext)_localctx).target = tableIdentifier();
				setState(413);
				match(LIKE);
				setState(414);
				((CreateTableLikeContext)_localctx).source = tableIdentifier();
				setState(423);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LOCATION || ((((_la - 207)) & ~0x3f) == 0 && ((1L << (_la - 207)) & ((1L << (ROW - 207)) | (1L << (STORED - 207)) | (1L << (TBLPROPERTIES - 207)) | (1L << (USING - 207)))) != 0)) {
					{
					setState(421);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case USING:
						{
						setState(415);
						tableProvider();
						}
						break;
					case ROW:
						{
						setState(416);
						rowFormat();
						}
						break;
					case STORED:
						{
						setState(417);
						createFileFormat();
						}
						break;
					case LOCATION:
						{
						setState(418);
						locationSpec();
						}
						break;
					case TBLPROPERTIES:
						{
						{
						setState(419);
						match(TBLPROPERTIES);
						setState(420);
						((CreateTableLikeContext)_localctx).tableProps = tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(425);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 11:
				_localctx = new ReplaceTableContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(426);
				replaceTableHeader();
				setState(431);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
				case 1:
					{
					setState(427);
					match(T__1);
					setState(428);
					colTypeList();
					setState(429);
					match(T__2);
					}
					break;
				}
				setState(434);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(433);
					tableProvider();
					}
				}

				setState(436);
				createTableClauses();
				setState(441);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1 || _la==AS || _la==FROM || _la==MAP || ((((_la - 191)) & ~0x3f) == 0 && ((1L << (_la - 191)) & ((1L << (REDUCE - 191)) | (1L << (SELECT - 191)) | (1L << (TABLE - 191)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(438);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(437);
						match(AS);
						}
					}

					setState(440);
					query();
					}
				}

				}
				break;
			case 12:
				_localctx = new AnalyzeContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(443);
				match(ANALYZE);
				setState(444);
				match(TABLE);
				setState(445);
				multipartIdentifier();
				setState(447);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(446);
					partitionSpec();
					}
				}

				setState(449);
				match(COMPUTE);
				setState(450);
				match(STATISTICS);
				setState(458);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
				case 1:
					{
					setState(451);
					identifier();
					}
					break;
				case 2:
					{
					setState(452);
					match(FOR);
					setState(453);
					match(COLUMNS);
					setState(454);
					identifierSeq();
					}
					break;
				case 3:
					{
					setState(455);
					match(FOR);
					setState(456);
					match(ALL);
					setState(457);
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
				setState(460);
				match(ANALYZE);
				setState(461);
				match(TABLES);
				setState(464);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(462);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(463);
					multipartIdentifier();
					}
				}

				setState(466);
				match(COMPUTE);
				setState(467);
				match(STATISTICS);
				setState(469);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
				case 1:
					{
					setState(468);
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
				setState(471);
				match(ALTER);
				setState(472);
				match(TABLE);
				setState(473);
				multipartIdentifier();
				setState(474);
				match(ADD);
				setState(475);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(476);
				((AddTableColumnsContext)_localctx).columns = qualifiedColTypeWithPositionList();
				}
				break;
			case 15:
				_localctx = new AddTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(478);
				match(ALTER);
				setState(479);
				match(TABLE);
				setState(480);
				multipartIdentifier();
				setState(481);
				match(ADD);
				setState(482);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(483);
				match(T__1);
				setState(484);
				((AddTableColumnsContext)_localctx).columns = qualifiedColTypeWithPositionList();
				setState(485);
				match(T__2);
				}
				break;
			case 16:
				_localctx = new RenameTableColumnContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(487);
				match(ALTER);
				setState(488);
				match(TABLE);
				setState(489);
				((RenameTableColumnContext)_localctx).table = multipartIdentifier();
				setState(490);
				match(RENAME);
				setState(491);
				match(COLUMN);
				setState(492);
				((RenameTableColumnContext)_localctx).from = multipartIdentifier();
				setState(493);
				match(TO);
				setState(494);
				((RenameTableColumnContext)_localctx).to = errorCapturingIdentifier();
				}
				break;
			case 17:
				_localctx = new DropTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(496);
				match(ALTER);
				setState(497);
				match(TABLE);
				setState(498);
				multipartIdentifier();
				setState(499);
				match(DROP);
				setState(500);
				_la = _input.LA(1);
				if ( !(_la==COLUMN || _la==COLUMNS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(501);
				match(T__1);
				setState(502);
				((DropTableColumnsContext)_localctx).columns = multipartIdentifierList();
				setState(503);
				match(T__2);
				}
				break;
			case 18:
				_localctx = new DropTableColumnsContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(505);
				match(ALTER);
				setState(506);
				match(TABLE);
				setState(507);
				multipartIdentifier();
				setState(508);
				match(DROP);
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
				((DropTableColumnsContext)_localctx).columns = multipartIdentifierList();
				}
				break;
			case 19:
				_localctx = new RenameTableContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(512);
				match(ALTER);
				setState(513);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(514);
				((RenameTableContext)_localctx).from = multipartIdentifier();
				setState(515);
				match(RENAME);
				setState(516);
				match(TO);
				setState(517);
				((RenameTableContext)_localctx).to = multipartIdentifier();
				}
				break;
			case 20:
				_localctx = new SetTablePropertiesContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(519);
				match(ALTER);
				setState(520);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(521);
				multipartIdentifier();
				setState(522);
				match(SET);
				setState(523);
				match(TBLPROPERTIES);
				setState(524);
				tablePropertyList();
				}
				break;
			case 21:
				_localctx = new UnsetTablePropertiesContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(526);
				match(ALTER);
				setState(527);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(528);
				multipartIdentifier();
				setState(529);
				match(UNSET);
				setState(530);
				match(TBLPROPERTIES);
				setState(533);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(531);
					match(IF);
					setState(532);
					match(EXISTS);
					}
				}

				setState(535);
				tablePropertyList();
				}
				break;
			case 22:
				_localctx = new AlterTableAlterColumnContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(537);
				match(ALTER);
				setState(538);
				match(TABLE);
				setState(539);
				((AlterTableAlterColumnContext)_localctx).table = multipartIdentifier();
				setState(540);
				_la = _input.LA(1);
				if ( !(_la==ALTER || _la==CHANGE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(542);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
				case 1:
					{
					setState(541);
					match(COLUMN);
					}
					break;
				}
				setState(544);
				((AlterTableAlterColumnContext)_localctx).column = multipartIdentifier();
				setState(546);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AFTER || _la==COMMENT || _la==DROP || _la==FIRST || _la==SET || _la==TYPE) {
					{
					setState(545);
					alterColumnAction();
					}
				}

				}
				break;
			case 23:
				_localctx = new HiveChangeColumnContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(548);
				match(ALTER);
				setState(549);
				match(TABLE);
				setState(550);
				((HiveChangeColumnContext)_localctx).table = multipartIdentifier();
				setState(552);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(551);
					partitionSpec();
					}
				}

				setState(554);
				match(CHANGE);
				setState(556);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
				case 1:
					{
					setState(555);
					match(COLUMN);
					}
					break;
				}
				setState(558);
				((HiveChangeColumnContext)_localctx).colName = multipartIdentifier();
				setState(559);
				colType();
				setState(561);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AFTER || _la==FIRST) {
					{
					setState(560);
					colPosition();
					}
				}

				}
				break;
			case 24:
				_localctx = new HiveReplaceColumnsContext(_localctx);
				enterOuterAlt(_localctx, 24);
				{
				setState(563);
				match(ALTER);
				setState(564);
				match(TABLE);
				setState(565);
				((HiveReplaceColumnsContext)_localctx).table = multipartIdentifier();
				setState(567);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(566);
					partitionSpec();
					}
				}

				setState(569);
				match(REPLACE);
				setState(570);
				match(COLUMNS);
				setState(571);
				match(T__1);
				setState(572);
				((HiveReplaceColumnsContext)_localctx).columns = qualifiedColTypeWithPositionList();
				setState(573);
				match(T__2);
				}
				break;
			case 25:
				_localctx = new SetTableSerDeContext(_localctx);
				enterOuterAlt(_localctx, 25);
				{
				setState(575);
				match(ALTER);
				setState(576);
				match(TABLE);
				setState(577);
				multipartIdentifier();
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
				match(SET);
				setState(582);
				match(SERDE);
				setState(583);
				match(STRING);
				setState(587);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WITH) {
					{
					setState(584);
					match(WITH);
					setState(585);
					match(SERDEPROPERTIES);
					setState(586);
					tablePropertyList();
					}
				}

				}
				break;
			case 26:
				_localctx = new SetTableSerDeContext(_localctx);
				enterOuterAlt(_localctx, 26);
				{
				setState(589);
				match(ALTER);
				setState(590);
				match(TABLE);
				setState(591);
				multipartIdentifier();
				setState(593);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(592);
					partitionSpec();
					}
				}

				setState(595);
				match(SET);
				setState(596);
				match(SERDEPROPERTIES);
				setState(597);
				tablePropertyList();
				}
				break;
			case 27:
				_localctx = new AddTablePartitionContext(_localctx);
				enterOuterAlt(_localctx, 27);
				{
				setState(599);
				match(ALTER);
				setState(600);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(601);
				multipartIdentifier();
				setState(602);
				match(ADD);
				setState(606);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(603);
					match(IF);
					setState(604);
					match(NOT);
					setState(605);
					match(EXISTS);
					}
				}

				setState(609); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(608);
					partitionSpecLocation();
					}
					}
					setState(611); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==PARTITION );
				}
				break;
			case 28:
				_localctx = new RenameTablePartitionContext(_localctx);
				enterOuterAlt(_localctx, 28);
				{
				setState(613);
				match(ALTER);
				setState(614);
				match(TABLE);
				setState(615);
				multipartIdentifier();
				setState(616);
				((RenameTablePartitionContext)_localctx).from = partitionSpec();
				setState(617);
				match(RENAME);
				setState(618);
				match(TO);
				setState(619);
				((RenameTablePartitionContext)_localctx).to = partitionSpec();
				}
				break;
			case 29:
				_localctx = new DropTablePartitionsContext(_localctx);
				enterOuterAlt(_localctx, 29);
				{
				setState(621);
				match(ALTER);
				setState(622);
				_la = _input.LA(1);
				if ( !(_la==TABLE || _la==VIEW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(623);
				multipartIdentifier();
				setState(624);
				match(DROP);
				setState(627);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(625);
					match(IF);
					setState(626);
					match(EXISTS);
					}
				}

				setState(629);
				partitionSpec();
				setState(634);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(630);
					match(T__3);
					setState(631);
					partitionSpec();
					}
					}
					setState(636);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(638);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PURGE) {
					{
					setState(637);
					match(PURGE);
					}
				}

				}
				break;
			case 30:
				_localctx = new SetTableLocationContext(_localctx);
				enterOuterAlt(_localctx, 30);
				{
				setState(640);
				match(ALTER);
				setState(641);
				match(TABLE);
				setState(642);
				multipartIdentifier();
				setState(644);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(643);
					partitionSpec();
					}
				}

				setState(646);
				match(SET);
				setState(647);
				locationSpec();
				}
				break;
			case 31:
				_localctx = new RecoverPartitionsContext(_localctx);
				enterOuterAlt(_localctx, 31);
				{
				setState(649);
				match(ALTER);
				setState(650);
				match(TABLE);
				setState(651);
				multipartIdentifier();
				setState(652);
				match(RECOVER);
				setState(653);
				match(PARTITIONS);
				}
				break;
			case 32:
				_localctx = new DropTableContext(_localctx);
				enterOuterAlt(_localctx, 32);
				{
				setState(655);
				match(DROP);
				setState(656);
				match(TABLE);
				setState(659);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
				case 1:
					{
					setState(657);
					match(IF);
					setState(658);
					match(EXISTS);
					}
					break;
				}
				setState(661);
				multipartIdentifier();
				setState(663);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PURGE) {
					{
					setState(662);
					match(PURGE);
					}
				}

				}
				break;
			case 33:
				_localctx = new DropViewContext(_localctx);
				enterOuterAlt(_localctx, 33);
				{
				setState(665);
				match(DROP);
				setState(666);
				match(VIEW);
				setState(669);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
				case 1:
					{
					setState(667);
					match(IF);
					setState(668);
					match(EXISTS);
					}
					break;
				}
				setState(671);
				multipartIdentifier();
				}
				break;
			case 34:
				_localctx = new CreateViewContext(_localctx);
				enterOuterAlt(_localctx, 34);
				{
				setState(672);
				match(CREATE);
				setState(675);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OR) {
					{
					setState(673);
					match(OR);
					setState(674);
					match(REPLACE);
					}
				}

				setState(681);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GLOBAL || _la==TEMPORARY) {
					{
					setState(678);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==GLOBAL) {
						{
						setState(677);
						match(GLOBAL);
						}
					}

					setState(680);
					match(TEMPORARY);
					}
				}

				setState(683);
				match(VIEW);
				setState(687);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
				case 1:
					{
					setState(684);
					match(IF);
					setState(685);
					match(NOT);
					setState(686);
					match(EXISTS);
					}
					break;
				}
				setState(689);
				multipartIdentifier();
				setState(691);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(690);
					identifierCommentList();
					}
				}

				setState(701);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMENT || _la==PARTITIONED || _la==TBLPROPERTIES) {
					{
					setState(699);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case COMMENT:
						{
						setState(693);
						commentSpec();
						}
						break;
					case PARTITIONED:
						{
						{
						setState(694);
						match(PARTITIONED);
						setState(695);
						match(ON);
						setState(696);
						identifierList();
						}
						}
						break;
					case TBLPROPERTIES:
						{
						{
						setState(697);
						match(TBLPROPERTIES);
						setState(698);
						tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(703);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(704);
				match(AS);
				setState(705);
				query();
				}
				break;
			case 35:
				_localctx = new CreateTempViewUsingContext(_localctx);
				enterOuterAlt(_localctx, 35);
				{
				setState(707);
				match(CREATE);
				setState(710);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OR) {
					{
					setState(708);
					match(OR);
					setState(709);
					match(REPLACE);
					}
				}

				setState(713);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GLOBAL) {
					{
					setState(712);
					match(GLOBAL);
					}
				}

				setState(715);
				match(TEMPORARY);
				setState(716);
				match(VIEW);
				setState(717);
				tableIdentifier();
				setState(722);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(718);
					match(T__1);
					setState(719);
					colTypeList();
					setState(720);
					match(T__2);
					}
				}

				setState(724);
				tableProvider();
				setState(727);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(725);
					match(OPTIONS);
					setState(726);
					tablePropertyList();
					}
				}

				}
				break;
			case 36:
				_localctx = new AlterViewQueryContext(_localctx);
				enterOuterAlt(_localctx, 36);
				{
				setState(729);
				match(ALTER);
				setState(730);
				match(VIEW);
				setState(731);
				multipartIdentifier();
				setState(733);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(732);
					match(AS);
					}
				}

				setState(735);
				query();
				}
				break;
			case 37:
				_localctx = new CreateFunctionContext(_localctx);
				enterOuterAlt(_localctx, 37);
				{
				setState(737);
				match(CREATE);
				setState(740);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OR) {
					{
					setState(738);
					match(OR);
					setState(739);
					match(REPLACE);
					}
				}

				setState(743);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==TEMPORARY) {
					{
					setState(742);
					match(TEMPORARY);
					}
				}

				setState(745);
				match(FUNCTION);
				setState(749);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,59,_ctx) ) {
				case 1:
					{
					setState(746);
					match(IF);
					setState(747);
					match(NOT);
					setState(748);
					match(EXISTS);
					}
					break;
				}
				setState(751);
				multipartIdentifier();
				setState(752);
				match(AS);
				setState(753);
				((CreateFunctionContext)_localctx).className = match(STRING);
				setState(763);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(754);
					match(USING);
					setState(755);
					resource();
					setState(760);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(756);
						match(T__3);
						setState(757);
						resource();
						}
						}
						setState(762);
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
				setState(765);
				match(DROP);
				setState(767);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==TEMPORARY) {
					{
					setState(766);
					match(TEMPORARY);
					}
				}

				setState(769);
				match(FUNCTION);
				setState(772);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
				case 1:
					{
					setState(770);
					match(IF);
					setState(771);
					match(EXISTS);
					}
					break;
				}
				setState(774);
				multipartIdentifier();
				}
				break;
			case 39:
				_localctx = new ExplainContext(_localctx);
				enterOuterAlt(_localctx, 39);
				{
				setState(775);
				match(EXPLAIN);
				setState(777);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CODEGEN || _la==COST || ((((_la - 90)) & ~0x3f) == 0 && ((1L << (_la - 90)) & ((1L << (EXTENDED - 90)) | (1L << (FORMATTED - 90)) | (1L << (LOGICAL - 90)))) != 0)) {
					{
					setState(776);
					_la = _input.LA(1);
					if ( !(_la==CODEGEN || _la==COST || ((((_la - 90)) & ~0x3f) == 0 && ((1L << (_la - 90)) & ((1L << (EXTENDED - 90)) | (1L << (FORMATTED - 90)) | (1L << (LOGICAL - 90)))) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(779);
				statement();
				}
				break;
			case 40:
				_localctx = new ShowTablesContext(_localctx);
				enterOuterAlt(_localctx, 40);
				{
				setState(780);
				match(SHOW);
				setState(781);
				match(TABLES);
				setState(784);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(782);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(783);
					multipartIdentifier();
					}
				}

				setState(790);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIKE || _la==STRING) {
					{
					setState(787);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LIKE) {
						{
						setState(786);
						match(LIKE);
						}
					}

					setState(789);
					((ShowTablesContext)_localctx).pattern = match(STRING);
					}
				}

				}
				break;
			case 41:
				_localctx = new ShowTableExtendedContext(_localctx);
				enterOuterAlt(_localctx, 41);
				{
				setState(792);
				match(SHOW);
				setState(793);
				match(TABLE);
				setState(794);
				match(EXTENDED);
				setState(797);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(795);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(796);
					((ShowTableExtendedContext)_localctx).ns = multipartIdentifier();
					}
				}

				setState(799);
				match(LIKE);
				setState(800);
				((ShowTableExtendedContext)_localctx).pattern = match(STRING);
				setState(802);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(801);
					partitionSpec();
					}
				}

				}
				break;
			case 42:
				_localctx = new ShowTblPropertiesContext(_localctx);
				enterOuterAlt(_localctx, 42);
				{
				setState(804);
				match(SHOW);
				setState(805);
				match(TBLPROPERTIES);
				setState(806);
				((ShowTblPropertiesContext)_localctx).table = multipartIdentifier();
				setState(811);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(807);
					match(T__1);
					setState(808);
					((ShowTblPropertiesContext)_localctx).key = tablePropertyKey();
					setState(809);
					match(T__2);
					}
				}

				}
				break;
			case 43:
				_localctx = new ShowColumnsContext(_localctx);
				enterOuterAlt(_localctx, 43);
				{
				setState(813);
				match(SHOW);
				setState(814);
				match(COLUMNS);
				setState(815);
				_la = _input.LA(1);
				if ( !(_la==FROM || _la==IN) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(816);
				((ShowColumnsContext)_localctx).table = multipartIdentifier();
				setState(819);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(817);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(818);
					((ShowColumnsContext)_localctx).ns = multipartIdentifier();
					}
				}

				}
				break;
			case 44:
				_localctx = new ShowViewsContext(_localctx);
				enterOuterAlt(_localctx, 44);
				{
				setState(821);
				match(SHOW);
				setState(822);
				match(VIEWS);
				setState(825);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FROM || _la==IN) {
					{
					setState(823);
					_la = _input.LA(1);
					if ( !(_la==FROM || _la==IN) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(824);
					multipartIdentifier();
					}
				}

				setState(831);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIKE || _la==STRING) {
					{
					setState(828);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LIKE) {
						{
						setState(827);
						match(LIKE);
						}
					}

					setState(830);
					((ShowViewsContext)_localctx).pattern = match(STRING);
					}
				}

				}
				break;
			case 45:
				_localctx = new ShowPartitionsContext(_localctx);
				enterOuterAlt(_localctx, 45);
				{
				setState(833);
				match(SHOW);
				setState(834);
				match(PARTITIONS);
				setState(835);
				multipartIdentifier();
				setState(837);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(836);
					partitionSpec();
					}
				}

				}
				break;
			case 46:
				_localctx = new ShowFunctionsContext(_localctx);
				enterOuterAlt(_localctx, 46);
				{
				setState(839);
				match(SHOW);
				setState(841);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,76,_ctx) ) {
				case 1:
					{
					setState(840);
					identifier();
					}
					break;
				}
				setState(843);
				match(FUNCTIONS);
				setState(851);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
				case 1:
					{
					setState(845);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,77,_ctx) ) {
					case 1:
						{
						setState(844);
						match(LIKE);
						}
						break;
					}
					setState(849);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
					case 1:
						{
						setState(847);
						multipartIdentifier();
						}
						break;
					case 2:
						{
						setState(848);
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
				setState(853);
				match(SHOW);
				setState(854);
				match(CREATE);
				setState(855);
				match(TABLE);
				setState(856);
				multipartIdentifier();
				setState(859);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(857);
					match(AS);
					setState(858);
					match(SERDE);
					}
				}

				}
				break;
			case 48:
				_localctx = new ShowCurrentNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 48);
				{
				setState(861);
				match(SHOW);
				setState(862);
				match(CURRENT);
				setState(863);
				match(NAMESPACE);
				}
				break;
			case 49:
				_localctx = new DescribeFunctionContext(_localctx);
				enterOuterAlt(_localctx, 49);
				{
				setState(864);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(865);
				match(FUNCTION);
				setState(867);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
				case 1:
					{
					setState(866);
					match(EXTENDED);
					}
					break;
				}
				setState(869);
				describeFuncName();
				}
				break;
			case 50:
				_localctx = new DescribeNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 50);
				{
				setState(870);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(871);
				namespace();
				setState(873);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
				case 1:
					{
					setState(872);
					match(EXTENDED);
					}
					break;
				}
				setState(875);
				multipartIdentifier();
				}
				break;
			case 51:
				_localctx = new DescribeRelationContext(_localctx);
				enterOuterAlt(_localctx, 51);
				{
				setState(877);
				_la = _input.LA(1);
				if ( !(_la==DESC || _la==DESCRIBE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(879);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,83,_ctx) ) {
				case 1:
					{
					setState(878);
					match(TABLE);
					}
					break;
				}
				setState(882);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
				case 1:
					{
					setState(881);
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
				setState(884);
				multipartIdentifier();
				setState(886);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
				case 1:
					{
					setState(885);
					partitionSpec();
					}
					break;
				}
				setState(889);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,86,_ctx) ) {
				case 1:
					{
					setState(888);
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
				setState(893);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==QUERY) {
					{
					setState(892);
					match(QUERY);
					}
				}

				setState(895);
				query();
				}
				break;
			case 53:
				_localctx = new CommentNamespaceContext(_localctx);
				enterOuterAlt(_localctx, 53);
				{
				setState(896);
				match(COMMENT);
				setState(897);
				match(ON);
				setState(898);
				namespace();
				setState(899);
				multipartIdentifier();
				setState(900);
				match(IS);
				setState(901);
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
				setState(903);
				match(COMMENT);
				setState(904);
				match(ON);
				setState(905);
				match(TABLE);
				setState(906);
				multipartIdentifier();
				setState(907);
				match(IS);
				setState(908);
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
				setState(910);
				match(REFRESH);
				setState(911);
				match(TABLE);
				setState(912);
				multipartIdentifier();
				}
				break;
			case 56:
				_localctx = new RefreshFunctionContext(_localctx);
				enterOuterAlt(_localctx, 56);
				{
				setState(913);
				match(REFRESH);
				setState(914);
				match(FUNCTION);
				setState(915);
				multipartIdentifier();
				}
				break;
			case 57:
				_localctx = new RefreshResourceContext(_localctx);
				enterOuterAlt(_localctx, 57);
				{
				setState(916);
				match(REFRESH);
				setState(924);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,89,_ctx) ) {
				case 1:
					{
					setState(917);
					match(STRING);
					}
					break;
				case 2:
					{
					setState(921);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
					while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
						if ( _alt==1+1 ) {
							{
							{
							setState(918);
							matchWildcard();
							}
							} 
						}
						setState(923);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
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
				setState(926);
				match(CACHE);
				setState(928);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LAZY) {
					{
					setState(927);
					match(LAZY);
					}
				}

				setState(930);
				match(TABLE);
				setState(931);
				multipartIdentifier();
				setState(934);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(932);
					match(OPTIONS);
					setState(933);
					((CacheTableContext)_localctx).options = tablePropertyList();
					}
				}

				setState(940);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1 || _la==AS || _la==FROM || _la==MAP || ((((_la - 191)) & ~0x3f) == 0 && ((1L << (_la - 191)) & ((1L << (REDUCE - 191)) | (1L << (SELECT - 191)) | (1L << (TABLE - 191)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(937);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(936);
						match(AS);
						}
					}

					setState(939);
					query();
					}
				}

				}
				break;
			case 59:
				_localctx = new UncacheTableContext(_localctx);
				enterOuterAlt(_localctx, 59);
				{
				setState(942);
				match(UNCACHE);
				setState(943);
				match(TABLE);
				setState(946);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
				case 1:
					{
					setState(944);
					match(IF);
					setState(945);
					match(EXISTS);
					}
					break;
				}
				setState(948);
				multipartIdentifier();
				}
				break;
			case 60:
				_localctx = new ClearCacheContext(_localctx);
				enterOuterAlt(_localctx, 60);
				{
				setState(949);
				match(CLEAR);
				setState(950);
				match(CACHE);
				}
				break;
			case 61:
				_localctx = new LoadDataContext(_localctx);
				enterOuterAlt(_localctx, 61);
				{
				setState(951);
				match(LOAD);
				setState(952);
				match(DATA);
				setState(954);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(953);
					match(LOCAL);
					}
				}

				setState(956);
				match(INPATH);
				setState(957);
				((LoadDataContext)_localctx).path = match(STRING);
				setState(959);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OVERWRITE) {
					{
					setState(958);
					match(OVERWRITE);
					}
				}

				setState(961);
				match(INTO);
				setState(962);
				match(TABLE);
				setState(963);
				multipartIdentifier();
				setState(965);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(964);
					partitionSpec();
					}
				}

				}
				break;
			case 62:
				_localctx = new TruncateTableContext(_localctx);
				enterOuterAlt(_localctx, 62);
				{
				setState(967);
				match(TRUNCATE);
				setState(968);
				match(TABLE);
				setState(969);
				multipartIdentifier();
				setState(971);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(970);
					partitionSpec();
					}
				}

				}
				break;
			case 63:
				_localctx = new RepairTableContext(_localctx);
				enterOuterAlt(_localctx, 63);
				{
				setState(973);
				match(MSCK);
				setState(974);
				match(REPAIR);
				setState(975);
				match(TABLE);
				setState(976);
				multipartIdentifier();
				setState(979);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ADD || _la==DROP || _la==SYNC) {
					{
					setState(977);
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
					setState(978);
					match(PARTITIONS);
					}
				}

				}
				break;
			case 64:
				_localctx = new ManageResourceContext(_localctx);
				enterOuterAlt(_localctx, 64);
				{
				setState(981);
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
				setState(982);
				identifier();
				setState(986);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,100,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(983);
						matchWildcard();
						}
						} 
					}
					setState(988);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,100,_ctx);
				}
				}
				break;
			case 65:
				_localctx = new FailNativeCommandContext(_localctx);
				enterOuterAlt(_localctx, 65);
				{
				setState(989);
				match(SET);
				setState(990);
				match(ROLE);
				setState(994);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,101,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(991);
						matchWildcard();
						}
						} 
					}
					setState(996);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,101,_ctx);
				}
				}
				break;
			case 66:
				_localctx = new SetTimeZoneContext(_localctx);
				enterOuterAlt(_localctx, 66);
				{
				setState(997);
				match(SET);
				setState(998);
				match(TIME);
				setState(999);
				match(ZONE);
				setState(1000);
				interval();
				}
				break;
			case 67:
				_localctx = new SetTimeZoneContext(_localctx);
				enterOuterAlt(_localctx, 67);
				{
				setState(1001);
				match(SET);
				setState(1002);
				match(TIME);
				setState(1003);
				match(ZONE);
				setState(1004);
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
				setState(1005);
				match(SET);
				setState(1006);
				match(TIME);
				setState(1007);
				match(ZONE);
				setState(1011);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1008);
						matchWildcard();
						}
						} 
					}
					setState(1013);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
				}
				}
				break;
			case 69:
				_localctx = new SetQuotedConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 69);
				{
				setState(1014);
				match(SET);
				setState(1015);
				configKey();
				setState(1016);
				match(EQ);
				setState(1017);
				configValue();
				}
				break;
			case 70:
				_localctx = new SetConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 70);
				{
				setState(1019);
				match(SET);
				setState(1020);
				configKey();
				setState(1028);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(1021);
					match(EQ);
					setState(1025);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,103,_ctx);
					while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
						if ( _alt==1+1 ) {
							{
							{
							setState(1022);
							matchWildcard();
							}
							} 
						}
						setState(1027);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,103,_ctx);
					}
					}
				}

				}
				break;
			case 71:
				_localctx = new SetQuotedConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 71);
				{
				setState(1030);
				match(SET);
				setState(1034);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1031);
						matchWildcard();
						}
						} 
					}
					setState(1036);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				}
				setState(1037);
				match(EQ);
				setState(1038);
				configValue();
				}
				break;
			case 72:
				_localctx = new SetConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 72);
				{
				setState(1039);
				match(SET);
				setState(1043);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,106,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1040);
						matchWildcard();
						}
						} 
					}
					setState(1045);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,106,_ctx);
				}
				}
				break;
			case 73:
				_localctx = new ResetQuotedConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 73);
				{
				setState(1046);
				match(RESET);
				setState(1047);
				configKey();
				}
				break;
			case 74:
				_localctx = new ResetConfigurationContext(_localctx);
				enterOuterAlt(_localctx, 74);
				{
				setState(1048);
				match(RESET);
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
				break;
			case 75:
				_localctx = new FailNativeCommandContext(_localctx);
				enterOuterAlt(_localctx, 75);
				{
				setState(1055);
				unsupportedHiveNativeCommands();
				setState(1059);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(1056);
						matchWildcard();
						}
						} 
					}
					setState(1061);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterConfigKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitConfigKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitConfigKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConfigKeyContext configKey() throws RecognitionException {
		ConfigKeyContext _localctx = new ConfigKeyContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_configKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1064);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterConfigValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitConfigValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitConfigValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConfigValueContext configValue() throws RecognitionException {
		ConfigValueContext _localctx = new ConfigValueContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_configValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1066);
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
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSqlCommandParser.ROLE, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode GRANT() { return getToken(ArcticSqlCommandParser.GRANT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSqlCommandParser.REVOKE, 0); }
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSqlCommandParser.PRINCIPALS, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSqlCommandParser.ROLES, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlCommandParser.CURRENT, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSqlCommandParser.EXPORT, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSqlCommandParser.IMPORT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSqlCommandParser.COMPACTIONS, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSqlCommandParser.TRANSACTIONS, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSqlCommandParser.INDEXES, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSqlCommandParser.LOCKS, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSqlCommandParser.INDEX, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSqlCommandParser.LOCK, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSqlCommandParser.DATABASE, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSqlCommandParser.UNLOCK, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSqlCommandParser.MACRO, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlCommandParser.CLUSTERED, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlCommandParser.BY, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlCommandParser.SORTED, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSqlCommandParser.SKEWED, 0); }
		public TerminalNode STORED() { return getToken(ArcticSqlCommandParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlCommandParser.DIRECTORIES, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSqlCommandParser.LOCATION, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSqlCommandParser.EXCHANGE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSqlCommandParser.PARTITION, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSqlCommandParser.ARCHIVE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSqlCommandParser.UNARCHIVE, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSqlCommandParser.TOUCH, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSqlCommandParser.COMPACT, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode CONCATENATE() { return getToken(ArcticSqlCommandParser.CONCATENATE, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSqlCommandParser.FILEFORMAT, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public TerminalNode START() { return getToken(ArcticSqlCommandParser.START, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSqlCommandParser.TRANSACTION, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSqlCommandParser.COMMIT, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSqlCommandParser.ROLLBACK, 0); }
		public TerminalNode DFS() { return getToken(ArcticSqlCommandParser.DFS, 0); }
		public UnsupportedHiveNativeCommandsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unsupportedHiveNativeCommands; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUnsupportedHiveNativeCommands(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUnsupportedHiveNativeCommands(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUnsupportedHiveNativeCommands(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnsupportedHiveNativeCommandsContext unsupportedHiveNativeCommands() throws RecognitionException {
		UnsupportedHiveNativeCommandsContext _localctx = new UnsupportedHiveNativeCommandsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_unsupportedHiveNativeCommands);
		int _la;
		try {
			setState(1236);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,117,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1068);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(1069);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1070);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(1071);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1072);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(GRANT);
				setState(1074);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,110,_ctx) ) {
				case 1:
					{
					setState(1073);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
					}
					break;
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1076);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(REVOKE);
				setState(1078);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,111,_ctx) ) {
				case 1:
					{
					setState(1077);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
					}
					break;
				}
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1080);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1081);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(GRANT);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1082);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1083);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				setState(1085);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
				case 1:
					{
					setState(1084);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(GRANT);
					}
					break;
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1087);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1088);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(PRINCIPALS);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1089);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1090);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLES);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1091);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1092);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(CURRENT);
				setState(1093);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(ROLES);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1094);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(EXPORT);
				setState(1095);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1096);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(IMPORT);
				setState(1097);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1098);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1099);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(COMPACTIONS);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1100);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1101);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(CREATE);
				setState(1102);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(TABLE);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1103);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1104);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TRANSACTIONS);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1105);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1106);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEXES);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1107);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(1108);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(LOCKS);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1109);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(1110);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(1111);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(1112);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(1113);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1114);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(1115);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(LOCK);
				setState(1116);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(1117);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(LOCK);
				setState(1118);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(DATABASE);
				}
				break;
			case 22:
				enterOuterAlt(_localctx, 22);
				{
				setState(1119);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(UNLOCK);
				setState(1120);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 23:
				enterOuterAlt(_localctx, 23);
				{
				setState(1121);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(UNLOCK);
				setState(1122);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(DATABASE);
				}
				break;
			case 24:
				enterOuterAlt(_localctx, 24);
				{
				setState(1123);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(1124);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TEMPORARY);
				setState(1125);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(MACRO);
				}
				break;
			case 25:
				enterOuterAlt(_localctx, 25);
				{
				setState(1126);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(1127);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TEMPORARY);
				setState(1128);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(MACRO);
				}
				break;
			case 26:
				enterOuterAlt(_localctx, 26);
				{
				setState(1129);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1130);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1131);
				tableIdentifier();
				setState(1132);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1133);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(CLUSTERED);
				}
				break;
			case 27:
				enterOuterAlt(_localctx, 27);
				{
				setState(1135);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1136);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1137);
				tableIdentifier();
				setState(1138);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(CLUSTERED);
				setState(1139);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(BY);
				}
				break;
			case 28:
				enterOuterAlt(_localctx, 28);
				{
				setState(1141);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1142);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1143);
				tableIdentifier();
				setState(1144);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1145);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SORTED);
				}
				break;
			case 29:
				enterOuterAlt(_localctx, 29);
				{
				setState(1147);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1148);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1149);
				tableIdentifier();
				setState(1150);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SKEWED);
				setState(1151);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(BY);
				}
				break;
			case 30:
				enterOuterAlt(_localctx, 30);
				{
				setState(1153);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1154);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1155);
				tableIdentifier();
				setState(1156);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1157);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SKEWED);
				}
				break;
			case 31:
				enterOuterAlt(_localctx, 31);
				{
				setState(1159);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1160);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1161);
				tableIdentifier();
				setState(1162);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(1163);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(STORED);
				setState(1164);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw5 = match(AS);
				setState(1165);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw6 = match(DIRECTORIES);
				}
				break;
			case 32:
				enterOuterAlt(_localctx, 32);
				{
				setState(1167);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1168);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1169);
				tableIdentifier();
				setState(1170);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SET);
				setState(1171);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SKEWED);
				setState(1172);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw5 = match(LOCATION);
				}
				break;
			case 33:
				enterOuterAlt(_localctx, 33);
				{
				setState(1174);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1175);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1176);
				tableIdentifier();
				setState(1177);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(EXCHANGE);
				setState(1178);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 34:
				enterOuterAlt(_localctx, 34);
				{
				setState(1180);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1181);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1182);
				tableIdentifier();
				setState(1183);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(ARCHIVE);
				setState(1184);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 35:
				enterOuterAlt(_localctx, 35);
				{
				setState(1186);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1187);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1188);
				tableIdentifier();
				setState(1189);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(UNARCHIVE);
				setState(1190);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 36:
				enterOuterAlt(_localctx, 36);
				{
				setState(1192);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1193);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1194);
				tableIdentifier();
				setState(1195);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(TOUCH);
				}
				break;
			case 37:
				enterOuterAlt(_localctx, 37);
				{
				setState(1197);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1198);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1199);
				tableIdentifier();
				setState(1201);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1200);
					partitionSpec();
					}
				}

				setState(1203);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(COMPACT);
				}
				break;
			case 38:
				enterOuterAlt(_localctx, 38);
				{
				setState(1205);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1206);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1207);
				tableIdentifier();
				setState(1209);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1208);
					partitionSpec();
					}
				}

				setState(1211);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(CONCATENATE);
				}
				break;
			case 39:
				enterOuterAlt(_localctx, 39);
				{
				setState(1213);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1214);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1215);
				tableIdentifier();
				setState(1217);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1216);
					partitionSpec();
					}
				}

				setState(1219);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SET);
				setState(1220);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(FILEFORMAT);
				}
				break;
			case 40:
				enterOuterAlt(_localctx, 40);
				{
				setState(1222);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(1223);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(1224);
				tableIdentifier();
				setState(1226);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1225);
					partitionSpec();
					}
				}

				setState(1228);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(REPLACE);
				setState(1229);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(COLUMNS);
				}
				break;
			case 41:
				enterOuterAlt(_localctx, 41);
				{
				setState(1231);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(START);
				setState(1232);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TRANSACTION);
				}
				break;
			case 42:
				enterOuterAlt(_localctx, 42);
				{
				setState(1233);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(COMMIT);
				}
				break;
			case 43:
				enterOuterAlt(_localctx, 43);
				{
				setState(1234);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ROLLBACK);
				}
				break;
			case 44:
				enterOuterAlt(_localctx, 44);
				{
				setState(1235);
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
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSqlCommandParser.EXTERNAL, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public CreateTableHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateTableHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateTableHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateTableHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableHeaderContext createTableHeader() throws RecognitionException {
		CreateTableHeaderContext _localctx = new CreateTableHeaderContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_createTableHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1238);
			match(CREATE);
			setState(1240);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TEMPORARY) {
				{
				setState(1239);
				match(TEMPORARY);
				}
			}

			setState(1243);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTERNAL) {
				{
				setState(1242);
				match(EXTERNAL);
				}
			}

			setState(1245);
			match(TABLE);
			setState(1249);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,120,_ctx) ) {
			case 1:
				{
				setState(1246);
				match(IF);
				setState(1247);
				match(NOT);
				setState(1248);
				match(EXISTS);
				}
				break;
			}
			setState(1251);
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
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public ReplaceTableHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceTableHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterReplaceTableHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitReplaceTableHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitReplaceTableHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceTableHeaderContext replaceTableHeader() throws RecognitionException {
		ReplaceTableHeaderContext _localctx = new ReplaceTableHeaderContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_replaceTableHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1255);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CREATE) {
				{
				setState(1253);
				match(CREATE);
				setState(1254);
				match(OR);
				}
			}

			setState(1257);
			match(REPLACE);
			setState(1258);
			match(TABLE);
			setState(1259);
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
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlCommandParser.CLUSTERED, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSqlCommandParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlCommandParser.BY, i);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode INTO() { return getToken(ArcticSqlCommandParser.INTO, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlCommandParser.INTEGER_VALUE, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSqlCommandParser.BUCKETS, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlCommandParser.SORTED, 0); }
		public OrderedIdentifierListContext orderedIdentifierList() {
			return getRuleContext(OrderedIdentifierListContext.class,0);
		}
		public BucketSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bucketSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterBucketSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitBucketSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitBucketSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BucketSpecContext bucketSpec() throws RecognitionException {
		BucketSpecContext _localctx = new BucketSpecContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_bucketSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1261);
			match(CLUSTERED);
			setState(1262);
			match(BY);
			setState(1263);
			identifierList();
			setState(1267);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SORTED) {
				{
				setState(1264);
				match(SORTED);
				setState(1265);
				match(BY);
				setState(1266);
				orderedIdentifierList();
				}
			}

			setState(1269);
			match(INTO);
			setState(1270);
			match(INTEGER_VALUE);
			setState(1271);
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
		public TerminalNode SKEWED() { return getToken(ArcticSqlCommandParser.SKEWED, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlCommandParser.BY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
		public ConstantListContext constantList() {
			return getRuleContext(ConstantListContext.class,0);
		}
		public NestedConstantListContext nestedConstantList() {
			return getRuleContext(NestedConstantListContext.class,0);
		}
		public TerminalNode STORED() { return getToken(ArcticSqlCommandParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlCommandParser.DIRECTORIES, 0); }
		public SkewSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skewSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSkewSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSkewSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSkewSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SkewSpecContext skewSpec() throws RecognitionException {
		SkewSpecContext _localctx = new SkewSpecContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_skewSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1273);
			match(SKEWED);
			setState(1274);
			match(BY);
			setState(1275);
			identifierList();
			setState(1276);
			match(ON);
			setState(1279);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,123,_ctx) ) {
			case 1:
				{
				setState(1277);
				constantList();
				}
				break;
			case 2:
				{
				setState(1278);
				nestedConstantList();
				}
				break;
			}
			setState(1284);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,124,_ctx) ) {
			case 1:
				{
				setState(1281);
				match(STORED);
				setState(1282);
				match(AS);
				setState(1283);
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
		public TerminalNode LOCATION() { return getToken(ArcticSqlCommandParser.LOCATION, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public LocationSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_locationSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLocationSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLocationSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLocationSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocationSpecContext locationSpec() throws RecognitionException {
		LocationSpecContext _localctx = new LocationSpecContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_locationSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1286);
			match(LOCATION);
			setState(1287);
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
		public TerminalNode COMMENT() { return getToken(ArcticSqlCommandParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public CommentSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commentSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCommentSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCommentSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCommentSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommentSpecContext commentSpec() throws RecognitionException {
		CommentSpecContext _localctx = new CommentSpecContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_commentSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1289);
			match(COMMENT);
			setState(1290);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1293);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(1292);
				ctes();
				}
			}

			setState(1295);
			queryTerm(0);
			setState(1296);
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
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlCommandParser.OVERWRITE, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSqlCommandParser.DIRECTORY, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlCommandParser.LOCAL, 0); }
		public RowFormatContext rowFormat() {
			return getRuleContext(RowFormatContext.class,0);
		}
		public CreateFileFormatContext createFileFormat() {
			return getRuleContext(CreateFileFormatContext.class,0);
		}
		public InsertOverwriteHiveDirContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInsertOverwriteHiveDir(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInsertOverwriteHiveDir(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInsertOverwriteHiveDir(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertOverwriteDirContext extends InsertIntoContext {
		public Token path;
		public TablePropertyListContext options;
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlCommandParser.OVERWRITE, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSqlCommandParser.DIRECTORY, 0); }
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public TerminalNode LOCAL() { return getToken(ArcticSqlCommandParser.LOCAL, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSqlCommandParser.OPTIONS, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public InsertOverwriteDirContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInsertOverwriteDir(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInsertOverwriteDir(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInsertOverwriteDir(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertOverwriteTableContext extends InsertIntoContext {
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlCommandParser.OVERWRITE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public InsertOverwriteTableContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInsertOverwriteTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInsertOverwriteTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInsertOverwriteTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertIntoTableContext extends InsertIntoContext {
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode INTO() { return getToken(ArcticSqlCommandParser.INTO, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public InsertIntoTableContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInsertIntoTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInsertIntoTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInsertIntoTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertIntoContext insertInto() throws RecognitionException {
		InsertIntoContext _localctx = new InsertIntoContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_insertInto);
		int _la;
		try {
			setState(1359);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,140,_ctx) ) {
			case 1:
				_localctx = new InsertOverwriteTableContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1298);
				match(INSERT);
				setState(1299);
				match(OVERWRITE);
				setState(1301);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,126,_ctx) ) {
				case 1:
					{
					setState(1300);
					match(TABLE);
					}
					break;
				}
				setState(1303);
				multipartIdentifier();
				setState(1310);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1304);
					partitionSpec();
					setState(1308);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==IF) {
						{
						setState(1305);
						match(IF);
						setState(1306);
						match(NOT);
						setState(1307);
						match(EXISTS);
						}
					}

					}
				}

				setState(1313);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,129,_ctx) ) {
				case 1:
					{
					setState(1312);
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
				setState(1315);
				match(INSERT);
				setState(1316);
				match(INTO);
				setState(1318);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,130,_ctx) ) {
				case 1:
					{
					setState(1317);
					match(TABLE);
					}
					break;
				}
				setState(1320);
				multipartIdentifier();
				setState(1322);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(1321);
					partitionSpec();
					}
				}

				setState(1327);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IF) {
					{
					setState(1324);
					match(IF);
					setState(1325);
					match(NOT);
					setState(1326);
					match(EXISTS);
					}
				}

				setState(1330);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,133,_ctx) ) {
				case 1:
					{
					setState(1329);
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
				setState(1332);
				match(INSERT);
				setState(1333);
				match(OVERWRITE);
				setState(1335);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(1334);
					match(LOCAL);
					}
				}

				setState(1337);
				match(DIRECTORY);
				setState(1338);
				((InsertOverwriteHiveDirContext)_localctx).path = match(STRING);
				setState(1340);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ROW) {
					{
					setState(1339);
					rowFormat();
					}
				}

				setState(1343);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STORED) {
					{
					setState(1342);
					createFileFormat();
					}
				}

				}
				break;
			case 4:
				_localctx = new InsertOverwriteDirContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1345);
				match(INSERT);
				setState(1346);
				match(OVERWRITE);
				setState(1348);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(1347);
					match(LOCAL);
					}
				}

				setState(1350);
				match(DIRECTORY);
				setState(1352);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STRING) {
					{
					setState(1351);
					((InsertOverwriteDirContext)_localctx).path = match(STRING);
					}
				}

				setState(1354);
				tableProvider();
				setState(1357);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(1355);
					match(OPTIONS);
					setState(1356);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPartitionSpecLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPartitionSpecLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPartitionSpecLocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionSpecLocationContext partitionSpecLocation() throws RecognitionException {
		PartitionSpecLocationContext _localctx = new PartitionSpecLocationContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_partitionSpecLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1361);
			partitionSpec();
			setState(1363);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LOCATION) {
				{
				setState(1362);
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
		public TerminalNode PARTITION() { return getToken(ArcticSqlCommandParser.PARTITION, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPartitionSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPartitionSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPartitionSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionSpecContext partitionSpec() throws RecognitionException {
		PartitionSpecContext _localctx = new PartitionSpecContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_partitionSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1365);
			match(PARTITION);
			setState(1366);
			match(T__1);
			setState(1367);
			partitionVal();
			setState(1372);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(1368);
				match(T__3);
				setState(1369);
				partitionVal();
				}
				}
				setState(1374);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1375);
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
		public TerminalNode EQ() { return getToken(ArcticSqlCommandParser.EQ, 0); }
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public PartitionValContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionVal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPartitionVal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPartitionVal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPartitionVal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionValContext partitionVal() throws RecognitionException {
		PartitionValContext _localctx = new PartitionValContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_partitionVal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1377);
			identifier();
			setState(1380);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EQ) {
				{
				setState(1378);
				match(EQ);
				setState(1379);
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
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlCommandParser.NAMESPACE, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSqlCommandParser.DATABASE, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticSqlCommandParser.SCHEMA, 0); }
		public NamespaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNamespace(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamespaceContext namespace() throws RecognitionException {
		NamespaceContext _localctx = new NamespaceContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_namespace);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1382);
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
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDescribeFuncName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDescribeFuncName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDescribeFuncName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeFuncNameContext describeFuncName() throws RecognitionException {
		DescribeFuncNameContext _localctx = new DescribeFuncNameContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_describeFuncName);
		try {
			setState(1389);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1384);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1385);
				match(STRING);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1386);
				comparisonOperator();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1387);
				arithmeticOperator();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1388);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDescribeColName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDescribeColName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDescribeColName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeColNameContext describeColName() throws RecognitionException {
		DescribeColNameContext _localctx = new DescribeColNameContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_describeColName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1391);
			((DescribeColNameContext)_localctx).identifier = identifier();
			((DescribeColNameContext)_localctx).nameParts.add(((DescribeColNameContext)_localctx).identifier);
			setState(1396);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__4) {
				{
				{
				setState(1392);
				match(T__4);
				setState(1393);
				((DescribeColNameContext)_localctx).identifier = identifier();
				((DescribeColNameContext)_localctx).nameParts.add(((DescribeColNameContext)_localctx).identifier);
				}
				}
				setState(1398);
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
		public TerminalNode WITH() { return getToken(ArcticSqlCommandParser.WITH, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCtes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCtes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCtes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtesContext ctes() throws RecognitionException {
		CtesContext _localctx = new CtesContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_ctes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1399);
			match(WITH);
			setState(1400);
			namedQuery();
			setState(1405);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(1401);
				match(T__3);
				setState(1402);
				namedQuery();
				}
				}
				setState(1407);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public NamedQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNamedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNamedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNamedQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedQueryContext namedQuery() throws RecognitionException {
		NamedQueryContext _localctx = new NamedQueryContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_namedQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1408);
			((NamedQueryContext)_localctx).name = errorCapturingIdentifier();
			setState(1410);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
			case 1:
				{
				setState(1409);
				((NamedQueryContext)_localctx).columnAliases = identifierList();
				}
				break;
			}
			setState(1413);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(1412);
				match(AS);
				}
			}

			setState(1415);
			match(T__1);
			setState(1416);
			query();
			setState(1417);
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
		public TerminalNode USING() { return getToken(ArcticSqlCommandParser.USING, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableProviderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProvider; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableProvider(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableProvider(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableProvider(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableProviderContext tableProvider() throws RecognitionException {
		TableProviderContext _localctx = new TableProviderContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_tableProvider);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1419);
			match(USING);
			setState(1420);
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
		public List<TerminalNode> OPTIONS() { return getTokens(ArcticSqlCommandParser.OPTIONS); }
		public TerminalNode OPTIONS(int i) {
			return getToken(ArcticSqlCommandParser.OPTIONS, i);
		}
		public List<TerminalNode> PARTITIONED() { return getTokens(ArcticSqlCommandParser.PARTITIONED); }
		public TerminalNode PARTITIONED(int i) {
			return getToken(ArcticSqlCommandParser.PARTITIONED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticSqlCommandParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlCommandParser.BY, i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticSqlCommandParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticSqlCommandParser.TBLPROPERTIES, i);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateTableClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateTableClauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateTableClauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableClausesContext createTableClauses() throws RecognitionException {
		CreateTableClausesContext _localctx = new CreateTableClausesContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_createTableClauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1437);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==CLUSTERED || _la==COMMENT || ((((_la - 142)) & ~0x3f) == 0 && ((1L << (_la - 142)) & ((1L << (LOCATION - 142)) | (1L << (OPTIONS - 142)) | (1L << (PARTITIONED - 142)))) != 0) || ((((_la - 207)) & ~0x3f) == 0 && ((1L << (_la - 207)) & ((1L << (ROW - 207)) | (1L << (SKEWED - 207)) | (1L << (STORED - 207)) | (1L << (TBLPROPERTIES - 207)))) != 0)) {
				{
				setState(1435);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTIONS:
					{
					{
					setState(1422);
					match(OPTIONS);
					setState(1423);
					((CreateTableClausesContext)_localctx).options = tablePropertyList();
					}
					}
					break;
				case PARTITIONED:
					{
					{
					setState(1424);
					match(PARTITIONED);
					setState(1425);
					match(BY);
					setState(1426);
					((CreateTableClausesContext)_localctx).partitioning = partitionFieldList();
					}
					}
					break;
				case SKEWED:
					{
					setState(1427);
					skewSpec();
					}
					break;
				case CLUSTERED:
					{
					setState(1428);
					bucketSpec();
					}
					break;
				case ROW:
					{
					setState(1429);
					rowFormat();
					}
					break;
				case STORED:
					{
					setState(1430);
					createFileFormat();
					}
					break;
				case LOCATION:
					{
					setState(1431);
					locationSpec();
					}
					break;
				case COMMENT:
					{
					setState(1432);
					commentSpec();
					}
					break;
				case TBLPROPERTIES:
					{
					{
					setState(1433);
					match(TBLPROPERTIES);
					setState(1434);
					((CreateTableClausesContext)_localctx).tableProps = tablePropertyList();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(1439);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTablePropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTablePropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTablePropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyListContext tablePropertyList() throws RecognitionException {
		TablePropertyListContext _localctx = new TablePropertyListContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_tablePropertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1440);
			match(T__1);
			setState(1441);
			tableProperty();
			setState(1446);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(1442);
				match(T__3);
				setState(1443);
				tableProperty();
				}
				}
				setState(1448);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1449);
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
		public TerminalNode EQ() { return getToken(ArcticSqlCommandParser.EQ, 0); }
		public TablePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyContext tableProperty() throws RecognitionException {
		TablePropertyContext _localctx = new TablePropertyContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_tableProperty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1451);
			((TablePropertyContext)_localctx).key = tablePropertyKey();
			setState(1456);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FALSE || ((((_la - 248)) & ~0x3f) == 0 && ((1L << (_la - 248)) & ((1L << (TRUE - 248)) | (1L << (EQ - 248)) | (1L << (STRING - 248)) | (1L << (INTEGER_VALUE - 248)) | (1L << (DECIMAL_VALUE - 248)))) != 0)) {
				{
				setState(1453);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(1452);
					match(EQ);
					}
				}

				setState(1455);
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
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TablePropertyKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTablePropertyKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTablePropertyKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTablePropertyKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyKeyContext tablePropertyKey() throws RecognitionException {
		TablePropertyKeyContext _localctx = new TablePropertyKeyContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_tablePropertyKey);
		int _la;
		try {
			setState(1467);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,155,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1458);
				identifier();
				setState(1463);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__4) {
					{
					{
					setState(1459);
					match(T__4);
					setState(1460);
					identifier();
					}
					}
					setState(1465);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1466);
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
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlCommandParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlCommandParser.DECIMAL_VALUE, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TablePropertyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTablePropertyValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTablePropertyValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTablePropertyValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyValueContext tablePropertyValue() throws RecognitionException {
		TablePropertyValueContext _localctx = new TablePropertyValueContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_tablePropertyValue);
		try {
			setState(1473);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1469);
				match(INTEGER_VALUE);
				}
				break;
			case DECIMAL_VALUE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1470);
				match(DECIMAL_VALUE);
				}
				break;
			case FALSE:
			case TRUE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1471);
				booleanValue();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(1472);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantListContext constantList() throws RecognitionException {
		ConstantListContext _localctx = new ConstantListContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_constantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1475);
			match(T__1);
			setState(1476);
			constant();
			setState(1481);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(1477);
				match(T__3);
				setState(1478);
				constant();
				}
				}
				setState(1483);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1484);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNestedConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNestedConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNestedConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NestedConstantListContext nestedConstantList() throws RecognitionException {
		NestedConstantListContext _localctx = new NestedConstantListContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_nestedConstantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1486);
			match(T__1);
			setState(1487);
			constantList();
			setState(1492);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(1488);
				match(T__3);
				setState(1489);
				constantList();
				}
				}
				setState(1494);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1495);
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
		public TerminalNode STORED() { return getToken(ArcticSqlCommandParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public FileFormatContext fileFormat() {
			return getRuleContext(FileFormatContext.class,0);
		}
		public TerminalNode BY() { return getToken(ArcticSqlCommandParser.BY, 0); }
		public StorageHandlerContext storageHandler() {
			return getRuleContext(StorageHandlerContext.class,0);
		}
		public CreateFileFormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createFileFormat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCreateFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCreateFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCreateFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateFileFormatContext createFileFormat() throws RecognitionException {
		CreateFileFormatContext _localctx = new CreateFileFormatContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_createFileFormat);
		try {
			setState(1503);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,159,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1497);
				match(STORED);
				setState(1498);
				match(AS);
				setState(1499);
				fileFormat();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1500);
				match(STORED);
				setState(1501);
				match(BY);
				setState(1502);
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
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSqlCommandParser.INPUTFORMAT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSqlCommandParser.OUTPUTFORMAT, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlCommandParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlCommandParser.STRING, i);
		}
		public TableFileFormatContext(FileFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableFileFormat(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterGenericFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitGenericFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitGenericFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileFormatContext fileFormat() throws RecognitionException {
		FileFormatContext _localctx = new FileFormatContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_fileFormat);
		try {
			setState(1510);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,160,_ctx) ) {
			case 1:
				_localctx = new TableFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1505);
				match(INPUTFORMAT);
				setState(1506);
				((TableFileFormatContext)_localctx).inFmt = match(STRING);
				setState(1507);
				match(OUTPUTFORMAT);
				setState(1508);
				((TableFileFormatContext)_localctx).outFmt = match(STRING);
				}
				break;
			case 2:
				_localctx = new GenericFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1509);
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
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticSqlCommandParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlCommandParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public StorageHandlerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storageHandler; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterStorageHandler(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitStorageHandler(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitStorageHandler(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StorageHandlerContext storageHandler() throws RecognitionException {
		StorageHandlerContext _localctx = new StorageHandlerContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_storageHandler);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1512);
			match(STRING);
			setState(1516);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,161,_ctx) ) {
			case 1:
				{
				setState(1513);
				match(WITH);
				setState(1514);
				match(SERDEPROPERTIES);
				setState(1515);
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
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public ResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitResource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceContext resource() throws RecognitionException {
		ResourceContext _localctx = new ResourceContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_resource);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1518);
			identifier();
			setState(1519);
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
		public TerminalNode DELETE() { return getToken(ArcticSqlCommandParser.DELETE, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDeleteFromTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDeleteFromTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDeleteFromTable(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSingleInsertQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSingleInsertQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSingleInsertQuery(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMultiInsertQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMultiInsertQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMultiInsertQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UpdateTableContext extends DmlStatementNoWithContext {
		public TerminalNode UPDATE() { return getToken(ArcticSqlCommandParser.UPDATE, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUpdateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUpdateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUpdateTable(this);
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
		public TerminalNode MERGE() { return getToken(ArcticSqlCommandParser.MERGE, 0); }
		public TerminalNode INTO() { return getToken(ArcticSqlCommandParser.INTO, 0); }
		public TerminalNode USING() { return getToken(ArcticSqlCommandParser.USING, 0); }
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMergeIntoTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMergeIntoTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMergeIntoTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DmlStatementNoWithContext dmlStatementNoWith() throws RecognitionException {
		DmlStatementNoWithContext _localctx = new DmlStatementNoWithContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_dmlStatementNoWith);
		int _la;
		try {
			int _alt;
			setState(1572);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INSERT:
				_localctx = new SingleInsertQueryContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1521);
				insertInto();
				setState(1522);
				queryTerm(0);
				setState(1523);
				queryOrganization();
				}
				break;
			case FROM:
				_localctx = new MultiInsertQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1525);
				fromClause();
				setState(1527); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1526);
					multiInsertQueryBody();
					}
					}
					setState(1529); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==INSERT );
				}
				break;
			case DELETE:
				_localctx = new DeleteFromTableContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1531);
				match(DELETE);
				setState(1532);
				match(FROM);
				setState(1533);
				multipartIdentifier();
				setState(1534);
				tableAlias();
				setState(1536);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1535);
					whereClause();
					}
				}

				}
				break;
			case UPDATE:
				_localctx = new UpdateTableContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1538);
				match(UPDATE);
				setState(1539);
				multipartIdentifier();
				setState(1540);
				tableAlias();
				setState(1541);
				setClause();
				setState(1543);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1542);
					whereClause();
					}
				}

				}
				break;
			case MERGE:
				_localctx = new MergeIntoTableContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1545);
				match(MERGE);
				setState(1546);
				match(INTO);
				setState(1547);
				((MergeIntoTableContext)_localctx).target = multipartIdentifier();
				setState(1548);
				((MergeIntoTableContext)_localctx).targetAlias = tableAlias();
				setState(1549);
				match(USING);
				setState(1555);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,165,_ctx) ) {
				case 1:
					{
					setState(1550);
					((MergeIntoTableContext)_localctx).source = multipartIdentifier();
					}
					break;
				case 2:
					{
					setState(1551);
					match(T__1);
					setState(1552);
					((MergeIntoTableContext)_localctx).sourceQuery = query();
					setState(1553);
					match(T__2);
					}
					break;
				}
				setState(1557);
				((MergeIntoTableContext)_localctx).sourceAlias = tableAlias();
				setState(1558);
				match(ON);
				setState(1559);
				((MergeIntoTableContext)_localctx).mergeCondition = booleanExpression(0);
				setState(1563);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,166,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1560);
						matchedClause();
						}
						} 
					}
					setState(1565);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,166,_ctx);
				}
				setState(1569);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==WHEN) {
					{
					{
					setState(1566);
					notMatchedClause();
					}
					}
					setState(1571);
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
		public TerminalNode ORDER() { return getToken(ArcticSqlCommandParser.ORDER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSqlCommandParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlCommandParser.BY, i);
		}
		public TerminalNode CLUSTER() { return getToken(ArcticSqlCommandParser.CLUSTER, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlCommandParser.DISTRIBUTE, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlCommandParser.SORT, 0); }
		public WindowClauseContext windowClause() {
			return getRuleContext(WindowClauseContext.class,0);
		}
		public TerminalNode LIMIT() { return getToken(ArcticSqlCommandParser.LIMIT, 0); }
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
		public TerminalNode ALL() { return getToken(ArcticSqlCommandParser.ALL, 0); }
		public QueryOrganizationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryOrganization; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQueryOrganization(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQueryOrganization(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQueryOrganization(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryOrganizationContext queryOrganization() throws RecognitionException {
		QueryOrganizationContext _localctx = new QueryOrganizationContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_queryOrganization);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1584);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,170,_ctx) ) {
			case 1:
				{
				setState(1574);
				match(ORDER);
				setState(1575);
				match(BY);
				setState(1576);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(1581);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,169,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1577);
						match(T__3);
						setState(1578);
						((QueryOrganizationContext)_localctx).sortItem = sortItem();
						((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
						}
						} 
					}
					setState(1583);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,169,_ctx);
				}
				}
				break;
			}
			setState(1596);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,172,_ctx) ) {
			case 1:
				{
				setState(1586);
				match(CLUSTER);
				setState(1587);
				match(BY);
				setState(1588);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(1593);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,171,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1589);
						match(T__3);
						setState(1590);
						((QueryOrganizationContext)_localctx).expression = expression();
						((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
						}
						} 
					}
					setState(1595);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,171,_ctx);
				}
				}
				break;
			}
			setState(1608);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,174,_ctx) ) {
			case 1:
				{
				setState(1598);
				match(DISTRIBUTE);
				setState(1599);
				match(BY);
				setState(1600);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(1605);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,173,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1601);
						match(T__3);
						setState(1602);
						((QueryOrganizationContext)_localctx).expression = expression();
						((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
						}
						} 
					}
					setState(1607);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,173,_ctx);
				}
				}
				break;
			}
			setState(1620);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,176,_ctx) ) {
			case 1:
				{
				setState(1610);
				match(SORT);
				setState(1611);
				match(BY);
				setState(1612);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(1617);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,175,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1613);
						match(T__3);
						setState(1614);
						((QueryOrganizationContext)_localctx).sortItem = sortItem();
						((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
						}
						} 
					}
					setState(1619);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,175,_ctx);
				}
				}
				break;
			}
			setState(1623);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,177,_ctx) ) {
			case 1:
				{
				setState(1622);
				windowClause();
				}
				break;
			}
			setState(1630);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,179,_ctx) ) {
			case 1:
				{
				setState(1625);
				match(LIMIT);
				setState(1628);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,178,_ctx) ) {
				case 1:
					{
					setState(1626);
					match(ALL);
					}
					break;
				case 2:
					{
					setState(1627);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMultiInsertQueryBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMultiInsertQueryBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMultiInsertQueryBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiInsertQueryBodyContext multiInsertQueryBody() throws RecognitionException {
		MultiInsertQueryBodyContext _localctx = new MultiInsertQueryBodyContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_multiInsertQueryBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1632);
			insertInto();
			setState(1633);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQueryTermDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQueryTermDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQueryTermDefault(this);
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
		public TerminalNode INTERSECT() { return getToken(ArcticSqlCommandParser.INTERSECT, 0); }
		public TerminalNode UNION() { return getToken(ArcticSqlCommandParser.UNION, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticSqlCommandParser.EXCEPT, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSqlCommandParser.SETMINUS, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public SetOperationContext(QueryTermContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetOperation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetOperation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetOperation(this);
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
		int _startState = 88;
		enterRecursionRule(_localctx, 88, RULE_queryTerm, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new QueryTermDefaultContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(1636);
			queryPrimary();
			}
			_ctx.stop = _input.LT(-1);
			setState(1661);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,184,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1659);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,183,_ctx) ) {
					case 1:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(1638);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1639);
						if (!(legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "legacy_setops_precedence_enabled");
						setState(1640);
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
						setState(1642);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(1641);
							setQuantifier();
							}
						}

						setState(1644);
						((SetOperationContext)_localctx).right = queryTerm(4);
						}
						break;
					case 2:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(1645);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1646);
						if (!(!legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "!legacy_setops_precedence_enabled");
						setState(1647);
						((SetOperationContext)_localctx).operator = match(INTERSECT);
						setState(1649);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(1648);
							setQuantifier();
							}
						}

						setState(1651);
						((SetOperationContext)_localctx).right = queryTerm(3);
						}
						break;
					case 3:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(1652);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1653);
						if (!(!legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "!legacy_setops_precedence_enabled");
						setState(1654);
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
						setState(1656);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(1655);
							setQuantifier();
							}
						}

						setState(1658);
						((SetOperationContext)_localctx).right = queryTerm(2);
						}
						break;
					}
					} 
				}
				setState(1663);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,184,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSubquery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSubquery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSubquery(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQueryPrimaryDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQueryPrimaryDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQueryPrimaryDefault(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInlineTableDefault1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInlineTableDefault1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInlineTableDefault1(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFromStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFromStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFromStmt(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TableContext extends QueryPrimaryContext {
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryPrimaryContext queryPrimary() throws RecognitionException {
		QueryPrimaryContext _localctx = new QueryPrimaryContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_queryPrimary);
		try {
			setState(1673);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MAP:
			case REDUCE:
			case SELECT:
				_localctx = new QueryPrimaryDefaultContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1664);
				querySpecification();
				}
				break;
			case FROM:
				_localctx = new FromStmtContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1665);
				fromStatement();
				}
				break;
			case TABLE:
				_localctx = new TableContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1666);
				match(TABLE);
				setState(1667);
				multipartIdentifier();
				}
				break;
			case VALUES:
				_localctx = new InlineTableDefault1Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1668);
				inlineTable();
				}
				break;
			case T__1:
				_localctx = new SubqueryContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1669);
				match(T__1);
				setState(1670);
				query();
				setState(1671);
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
		public TerminalNode NULLS() { return getToken(ArcticSqlCommandParser.NULLS, 0); }
		public TerminalNode ASC() { return getToken(ArcticSqlCommandParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode LAST() { return getToken(ArcticSqlCommandParser.LAST, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSqlCommandParser.FIRST, 0); }
		public SortItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sortItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSortItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSortItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSortItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SortItemContext sortItem() throws RecognitionException {
		SortItemContext _localctx = new SortItemContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_sortItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1675);
			expression();
			setState(1677);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,186,_ctx) ) {
			case 1:
				{
				setState(1676);
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
			setState(1681);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,187,_ctx) ) {
			case 1:
				{
				setState(1679);
				match(NULLS);
				setState(1680);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFromStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFromStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFromStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromStatementContext fromStatement() throws RecognitionException {
		FromStatementContext _localctx = new FromStatementContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_fromStatement);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1683);
			fromClause();
			setState(1685); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1684);
					fromStatementBody();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1687); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,188,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFromStatementBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFromStatementBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFromStatementBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromStatementBodyContext fromStatementBody() throws RecognitionException {
		FromStatementBodyContext _localctx = new FromStatementBodyContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_fromStatementBody);
		try {
			int _alt;
			setState(1716);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,195,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1689);
				transformClause();
				setState(1691);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,189,_ctx) ) {
				case 1:
					{
					setState(1690);
					whereClause();
					}
					break;
				}
				setState(1693);
				queryOrganization();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1695);
				selectClause();
				setState(1699);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,190,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1696);
						lateralView();
						}
						} 
					}
					setState(1701);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,190,_ctx);
				}
				setState(1703);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,191,_ctx) ) {
				case 1:
					{
					setState(1702);
					whereClause();
					}
					break;
				}
				setState(1706);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,192,_ctx) ) {
				case 1:
					{
					setState(1705);
					aggregationClause();
					}
					break;
				}
				setState(1709);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,193,_ctx) ) {
				case 1:
					{
					setState(1708);
					havingClause();
					}
					break;
				}
				setState(1712);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,194,_ctx) ) {
				case 1:
					{
					setState(1711);
					windowClause();
					}
					break;
				}
				setState(1714);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRegularQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRegularQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRegularQuerySpecification(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTransformQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTransformQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTransformQuerySpecification(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuerySpecificationContext querySpecification() throws RecognitionException {
		QuerySpecificationContext _localctx = new QuerySpecificationContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_querySpecification);
		try {
			int _alt;
			setState(1762);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,208,_ctx) ) {
			case 1:
				_localctx = new TransformQuerySpecificationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1718);
				transformClause();
				setState(1720);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,196,_ctx) ) {
				case 1:
					{
					setState(1719);
					fromClause();
					}
					break;
				}
				setState(1725);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,197,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1722);
						lateralView();
						}
						} 
					}
					setState(1727);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,197,_ctx);
				}
				setState(1729);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,198,_ctx) ) {
				case 1:
					{
					setState(1728);
					whereClause();
					}
					break;
				}
				setState(1732);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,199,_ctx) ) {
				case 1:
					{
					setState(1731);
					aggregationClause();
					}
					break;
				}
				setState(1735);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,200,_ctx) ) {
				case 1:
					{
					setState(1734);
					havingClause();
					}
					break;
				}
				setState(1738);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,201,_ctx) ) {
				case 1:
					{
					setState(1737);
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
				setState(1740);
				selectClause();
				setState(1742);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,202,_ctx) ) {
				case 1:
					{
					setState(1741);
					fromClause();
					}
					break;
				}
				setState(1747);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,203,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1744);
						lateralView();
						}
						} 
					}
					setState(1749);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,203,_ctx);
				}
				setState(1751);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,204,_ctx) ) {
				case 1:
					{
					setState(1750);
					whereClause();
					}
					break;
				}
				setState(1754);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,205,_ctx) ) {
				case 1:
					{
					setState(1753);
					aggregationClause();
					}
					break;
				}
				setState(1757);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,206,_ctx) ) {
				case 1:
					{
					setState(1756);
					havingClause();
					}
					break;
				}
				setState(1760);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,207,_ctx) ) {
				case 1:
					{
					setState(1759);
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
		public TerminalNode USING() { return getToken(ArcticSqlCommandParser.USING, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlCommandParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlCommandParser.STRING, i);
		}
		public TerminalNode SELECT() { return getToken(ArcticSqlCommandParser.SELECT, 0); }
		public ExpressionSeqContext expressionSeq() {
			return getRuleContext(ExpressionSeqContext.class,0);
		}
		public TerminalNode TRANSFORM() { return getToken(ArcticSqlCommandParser.TRANSFORM, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlCommandParser.MAP, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSqlCommandParser.REDUCE, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSqlCommandParser.RECORDWRITER, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSqlCommandParser.RECORDREADER, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTransformClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTransformClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTransformClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformClauseContext transformClause() throws RecognitionException {
		TransformClauseContext _localctx = new TransformClauseContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_transformClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1783);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
				{
				setState(1764);
				match(SELECT);
				setState(1765);
				((TransformClauseContext)_localctx).kind = match(TRANSFORM);
				setState(1766);
				match(T__1);
				setState(1768);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,209,_ctx) ) {
				case 1:
					{
					setState(1767);
					setQuantifier();
					}
					break;
				}
				setState(1770);
				expressionSeq();
				setState(1771);
				match(T__2);
				}
				break;
			case MAP:
				{
				setState(1773);
				((TransformClauseContext)_localctx).kind = match(MAP);
				setState(1775);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,210,_ctx) ) {
				case 1:
					{
					setState(1774);
					setQuantifier();
					}
					break;
				}
				setState(1777);
				expressionSeq();
				}
				break;
			case REDUCE:
				{
				setState(1778);
				((TransformClauseContext)_localctx).kind = match(REDUCE);
				setState(1780);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,211,_ctx) ) {
				case 1:
					{
					setState(1779);
					setQuantifier();
					}
					break;
				}
				setState(1782);
				expressionSeq();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1786);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ROW) {
				{
				setState(1785);
				((TransformClauseContext)_localctx).inRowFormat = rowFormat();
				}
			}

			setState(1790);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RECORDWRITER) {
				{
				setState(1788);
				match(RECORDWRITER);
				setState(1789);
				((TransformClauseContext)_localctx).recordWriter = match(STRING);
				}
			}

			setState(1792);
			match(USING);
			setState(1793);
			((TransformClauseContext)_localctx).script = match(STRING);
			setState(1806);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,217,_ctx) ) {
			case 1:
				{
				setState(1794);
				match(AS);
				setState(1804);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,216,_ctx) ) {
				case 1:
					{
					setState(1795);
					identifierSeq();
					}
					break;
				case 2:
					{
					setState(1796);
					colTypeList();
					}
					break;
				case 3:
					{
					{
					setState(1797);
					match(T__1);
					setState(1800);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,215,_ctx) ) {
					case 1:
						{
						setState(1798);
						identifierSeq();
						}
						break;
					case 2:
						{
						setState(1799);
						colTypeList();
						}
						break;
					}
					setState(1802);
					match(T__2);
					}
					}
					break;
				}
				}
				break;
			}
			setState(1809);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,218,_ctx) ) {
			case 1:
				{
				setState(1808);
				((TransformClauseContext)_localctx).outRowFormat = rowFormat();
				}
				break;
			}
			setState(1813);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,219,_ctx) ) {
			case 1:
				{
				setState(1811);
				match(RECORDREADER);
				setState(1812);
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
		public TerminalNode SELECT() { return getToken(ArcticSqlCommandParser.SELECT, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSelectClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSelectClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSelectClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectClauseContext selectClause() throws RecognitionException {
		SelectClauseContext _localctx = new SelectClauseContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_selectClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1815);
			match(SELECT);
			setState(1819);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,220,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1816);
					((SelectClauseContext)_localctx).hint = hint();
					((SelectClauseContext)_localctx).hints.add(((SelectClauseContext)_localctx).hint);
					}
					} 
				}
				setState(1821);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,220,_ctx);
			}
			setState(1823);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,221,_ctx) ) {
			case 1:
				{
				setState(1822);
				setQuantifier();
				}
				break;
			}
			setState(1825);
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
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public AssignmentListContext assignmentList() {
			return getRuleContext(AssignmentListContext.class,0);
		}
		public SetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetClauseContext setClause() throws RecognitionException {
		SetClauseContext _localctx = new SetClauseContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_setClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1827);
			match(SET);
			setState(1828);
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
		public TerminalNode WHEN() { return getToken(ArcticSqlCommandParser.WHEN, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticSqlCommandParser.MATCHED, 0); }
		public TerminalNode THEN() { return getToken(ArcticSqlCommandParser.THEN, 0); }
		public MatchedActionContext matchedAction() {
			return getRuleContext(MatchedActionContext.class,0);
		}
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public MatchedClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchedClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMatchedClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMatchedClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMatchedClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchedClauseContext matchedClause() throws RecognitionException {
		MatchedClauseContext _localctx = new MatchedClauseContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_matchedClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1830);
			match(WHEN);
			setState(1831);
			match(MATCHED);
			setState(1834);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AND) {
				{
				setState(1832);
				match(AND);
				setState(1833);
				((MatchedClauseContext)_localctx).matchedCond = booleanExpression(0);
				}
			}

			setState(1836);
			match(THEN);
			setState(1837);
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
		public TerminalNode WHEN() { return getToken(ArcticSqlCommandParser.WHEN, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticSqlCommandParser.MATCHED, 0); }
		public TerminalNode THEN() { return getToken(ArcticSqlCommandParser.THEN, 0); }
		public NotMatchedActionContext notMatchedAction() {
			return getRuleContext(NotMatchedActionContext.class,0);
		}
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public NotMatchedClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notMatchedClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNotMatchedClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNotMatchedClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNotMatchedClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotMatchedClauseContext notMatchedClause() throws RecognitionException {
		NotMatchedClauseContext _localctx = new NotMatchedClauseContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_notMatchedClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1839);
			match(WHEN);
			setState(1840);
			match(NOT);
			setState(1841);
			match(MATCHED);
			setState(1844);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AND) {
				{
				setState(1842);
				match(AND);
				setState(1843);
				((NotMatchedClauseContext)_localctx).notMatchedCond = booleanExpression(0);
				}
			}

			setState(1846);
			match(THEN);
			setState(1847);
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
		public TerminalNode DELETE() { return getToken(ArcticSqlCommandParser.DELETE, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticSqlCommandParser.UPDATE, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticSqlCommandParser.ASTERISK, 0); }
		public AssignmentListContext assignmentList() {
			return getRuleContext(AssignmentListContext.class,0);
		}
		public MatchedActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchedAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMatchedAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMatchedAction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMatchedAction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchedActionContext matchedAction() throws RecognitionException {
		MatchedActionContext _localctx = new MatchedActionContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_matchedAction);
		try {
			setState(1856);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,224,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1849);
				match(DELETE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1850);
				match(UPDATE);
				setState(1851);
				match(SET);
				setState(1852);
				match(ASTERISK);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1853);
				match(UPDATE);
				setState(1854);
				match(SET);
				setState(1855);
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
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticSqlCommandParser.ASTERISK, 0); }
		public TerminalNode VALUES() { return getToken(ArcticSqlCommandParser.VALUES, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNotMatchedAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNotMatchedAction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNotMatchedAction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NotMatchedActionContext notMatchedAction() throws RecognitionException {
		NotMatchedActionContext _localctx = new NotMatchedActionContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_notMatchedAction);
		int _la;
		try {
			setState(1876);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,226,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1858);
				match(INSERT);
				setState(1859);
				match(ASTERISK);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1860);
				match(INSERT);
				setState(1861);
				match(T__1);
				setState(1862);
				((NotMatchedActionContext)_localctx).columns = multipartIdentifierList();
				setState(1863);
				match(T__2);
				setState(1864);
				match(VALUES);
				setState(1865);
				match(T__1);
				setState(1866);
				expression();
				setState(1871);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(1867);
					match(T__3);
					setState(1868);
					expression();
					}
					}
					setState(1873);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1874);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAssignmentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAssignmentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAssignmentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentListContext assignmentList() throws RecognitionException {
		AssignmentListContext _localctx = new AssignmentListContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_assignmentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1878);
			assignment();
			setState(1883);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(1879);
				match(T__3);
				setState(1880);
				assignment();
				}
				}
				setState(1885);
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
		public TerminalNode EQ() { return getToken(ArcticSqlCommandParser.EQ, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1886);
			((AssignmentContext)_localctx).key = multipartIdentifier();
			setState(1887);
			match(EQ);
			setState(1888);
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
		public TerminalNode WHERE() { return getToken(ArcticSqlCommandParser.WHERE, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public WhereClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterWhereClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitWhereClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitWhereClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_whereClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1890);
			match(WHERE);
			setState(1891);
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
		public TerminalNode HAVING() { return getToken(ArcticSqlCommandParser.HAVING, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public HavingClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_havingClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterHavingClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitHavingClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitHavingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingClauseContext havingClause() throws RecognitionException {
		HavingClauseContext _localctx = new HavingClauseContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_havingClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1893);
			match(HAVING);
			setState(1894);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterHint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitHint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitHint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintContext hint() throws RecognitionException {
		HintContext _localctx = new HintContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_hint);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1896);
			match(T__5);
			setState(1897);
			((HintContext)_localctx).hintStatement = hintStatement();
			((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
			setState(1904);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,229,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1899);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,228,_ctx) ) {
					case 1:
						{
						setState(1898);
						match(T__3);
						}
						break;
					}
					setState(1901);
					((HintContext)_localctx).hintStatement = hintStatement();
					((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
					}
					} 
				}
				setState(1906);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,229,_ctx);
			}
			setState(1907);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterHintStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitHintStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitHintStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintStatementContext hintStatement() throws RecognitionException {
		HintStatementContext _localctx = new HintStatementContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_hintStatement);
		int _la;
		try {
			setState(1922);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,231,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1909);
				((HintStatementContext)_localctx).hintName = identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1910);
				((HintStatementContext)_localctx).hintName = identifier();
				setState(1911);
				match(T__1);
				setState(1912);
				((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
				((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
				setState(1917);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(1913);
					match(T__3);
					setState(1914);
					((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
					((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
					}
					}
					setState(1919);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1920);
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
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFromClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFromClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFromClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromClauseContext fromClause() throws RecognitionException {
		FromClauseContext _localctx = new FromClauseContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_fromClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1924);
			match(FROM);
			setState(1925);
			relation();
			setState(1930);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,232,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1926);
					match(T__3);
					setState(1927);
					relation();
					}
					} 
				}
				setState(1932);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,232,_ctx);
			}
			setState(1936);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,233,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1933);
					lateralView();
					}
					} 
				}
				setState(1938);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,233,_ctx);
			}
			setState(1940);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,234,_ctx) ) {
			case 1:
				{
				setState(1939);
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
		public TerminalNode GROUP() { return getToken(ArcticSqlCommandParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlCommandParser.BY, 0); }
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
		public TerminalNode WITH() { return getToken(ArcticSqlCommandParser.WITH, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlCommandParser.SETS, 0); }
		public List<GroupingSetContext> groupingSet() {
			return getRuleContexts(GroupingSetContext.class);
		}
		public GroupingSetContext groupingSet(int i) {
			return getRuleContext(GroupingSetContext.class,i);
		}
		public TerminalNode ROLLUP() { return getToken(ArcticSqlCommandParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlCommandParser.CUBE, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlCommandParser.GROUPING, 0); }
		public AggregationClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregationClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAggregationClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAggregationClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAggregationClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregationClauseContext aggregationClause() throws RecognitionException {
		AggregationClauseContext _localctx = new AggregationClauseContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_aggregationClause);
		int _la;
		try {
			int _alt;
			setState(1981);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,239,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1942);
				match(GROUP);
				setState(1943);
				match(BY);
				setState(1944);
				((AggregationClauseContext)_localctx).groupByClause = groupByClause();
				((AggregationClauseContext)_localctx).groupingExpressionsWithGroupingAnalytics.add(((AggregationClauseContext)_localctx).groupByClause);
				setState(1949);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,235,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1945);
						match(T__3);
						setState(1946);
						((AggregationClauseContext)_localctx).groupByClause = groupByClause();
						((AggregationClauseContext)_localctx).groupingExpressionsWithGroupingAnalytics.add(((AggregationClauseContext)_localctx).groupByClause);
						}
						} 
					}
					setState(1951);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,235,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1952);
				match(GROUP);
				setState(1953);
				match(BY);
				setState(1954);
				((AggregationClauseContext)_localctx).expression = expression();
				((AggregationClauseContext)_localctx).groupingExpressions.add(((AggregationClauseContext)_localctx).expression);
				setState(1959);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,236,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1955);
						match(T__3);
						setState(1956);
						((AggregationClauseContext)_localctx).expression = expression();
						((AggregationClauseContext)_localctx).groupingExpressions.add(((AggregationClauseContext)_localctx).expression);
						}
						} 
					}
					setState(1961);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,236,_ctx);
				}
				setState(1979);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,238,_ctx) ) {
				case 1:
					{
					setState(1962);
					match(WITH);
					setState(1963);
					((AggregationClauseContext)_localctx).kind = match(ROLLUP);
					}
					break;
				case 2:
					{
					setState(1964);
					match(WITH);
					setState(1965);
					((AggregationClauseContext)_localctx).kind = match(CUBE);
					}
					break;
				case 3:
					{
					setState(1966);
					((AggregationClauseContext)_localctx).kind = match(GROUPING);
					setState(1967);
					match(SETS);
					setState(1968);
					match(T__1);
					setState(1969);
					groupingSet();
					setState(1974);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(1970);
						match(T__3);
						setState(1971);
						groupingSet();
						}
						}
						setState(1976);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(1977);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterGroupByClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitGroupByClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitGroupByClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupByClauseContext groupByClause() throws RecognitionException {
		GroupByClauseContext _localctx = new GroupByClauseContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_groupByClause);
		try {
			setState(1985);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,240,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1983);
				groupingAnalytics();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1984);
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
		public TerminalNode ROLLUP() { return getToken(ArcticSqlCommandParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlCommandParser.CUBE, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlCommandParser.GROUPING, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlCommandParser.SETS, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterGroupingAnalytics(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitGroupingAnalytics(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitGroupingAnalytics(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingAnalyticsContext groupingAnalytics() throws RecognitionException {
		GroupingAnalyticsContext _localctx = new GroupingAnalyticsContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_groupingAnalytics);
		int _la;
		try {
			setState(2012);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CUBE:
			case ROLLUP:
				enterOuterAlt(_localctx, 1);
				{
				setState(1987);
				_la = _input.LA(1);
				if ( !(_la==CUBE || _la==ROLLUP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1988);
				match(T__1);
				setState(1989);
				groupingSet();
				setState(1994);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(1990);
					match(T__3);
					setState(1991);
					groupingSet();
					}
					}
					setState(1996);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1997);
				match(T__2);
				}
				break;
			case GROUPING:
				enterOuterAlt(_localctx, 2);
				{
				setState(1999);
				match(GROUPING);
				setState(2000);
				match(SETS);
				setState(2001);
				match(T__1);
				setState(2002);
				groupingElement();
				setState(2007);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(2003);
					match(T__3);
					setState(2004);
					groupingElement();
					}
					}
					setState(2009);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2010);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterGroupingElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitGroupingElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitGroupingElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingElementContext groupingElement() throws RecognitionException {
		GroupingElementContext _localctx = new GroupingElementContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_groupingElement);
		try {
			setState(2016);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,244,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2014);
				groupingAnalytics();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2015);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterGroupingSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitGroupingSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitGroupingSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingSetContext groupingSet() throws RecognitionException {
		GroupingSetContext _localctx = new GroupingSetContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_groupingSet);
		int _la;
		try {
			setState(2031);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,247,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2018);
				match(T__1);
				setState(2027);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,246,_ctx) ) {
				case 1:
					{
					setState(2019);
					expression();
					setState(2024);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(2020);
						match(T__3);
						setState(2021);
						expression();
						}
						}
						setState(2026);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(2029);
				match(T__2);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2030);
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
		public TerminalNode PIVOT() { return getToken(ArcticSqlCommandParser.PIVOT, 0); }
		public TerminalNode FOR() { return getToken(ArcticSqlCommandParser.FOR, 0); }
		public PivotColumnContext pivotColumn() {
			return getRuleContext(PivotColumnContext.class,0);
		}
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPivotClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPivotClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPivotClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotClauseContext pivotClause() throws RecognitionException {
		PivotClauseContext _localctx = new PivotClauseContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_pivotClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2033);
			match(PIVOT);
			setState(2034);
			match(T__1);
			setState(2035);
			((PivotClauseContext)_localctx).aggregates = namedExpressionSeq();
			setState(2036);
			match(FOR);
			setState(2037);
			pivotColumn();
			setState(2038);
			match(IN);
			setState(2039);
			match(T__1);
			setState(2040);
			((PivotClauseContext)_localctx).pivotValue = pivotValue();
			((PivotClauseContext)_localctx).pivotValues.add(((PivotClauseContext)_localctx).pivotValue);
			setState(2045);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2041);
				match(T__3);
				setState(2042);
				((PivotClauseContext)_localctx).pivotValue = pivotValue();
				((PivotClauseContext)_localctx).pivotValues.add(((PivotClauseContext)_localctx).pivotValue);
				}
				}
				setState(2047);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2048);
			match(T__2);
			setState(2049);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPivotColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPivotColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPivotColumn(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotColumnContext pivotColumn() throws RecognitionException {
		PivotColumnContext _localctx = new PivotColumnContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_pivotColumn);
		int _la;
		try {
			setState(2063);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,250,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2051);
				((PivotColumnContext)_localctx).identifier = identifier();
				((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2052);
				match(T__1);
				setState(2053);
				((PivotColumnContext)_localctx).identifier = identifier();
				((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
				setState(2058);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(2054);
					match(T__3);
					setState(2055);
					((PivotColumnContext)_localctx).identifier = identifier();
					((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
					}
					}
					setState(2060);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2061);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public PivotValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPivotValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPivotValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPivotValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotValueContext pivotValue() throws RecognitionException {
		PivotValueContext _localctx = new PivotValueContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_pivotValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2065);
			expression();
			setState(2070);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,252,_ctx) ) {
			case 1:
				{
				setState(2067);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,251,_ctx) ) {
				case 1:
					{
					setState(2066);
					match(AS);
					}
					break;
				}
				setState(2069);
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
		public TerminalNode LATERAL() { return getToken(ArcticSqlCommandParser.LATERAL, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode OUTER() { return getToken(ArcticSqlCommandParser.OUTER, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public LateralViewContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lateralView; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLateralView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLateralView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLateralView(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LateralViewContext lateralView() throws RecognitionException {
		LateralViewContext _localctx = new LateralViewContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_lateralView);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2072);
			match(LATERAL);
			setState(2073);
			match(VIEW);
			setState(2075);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,253,_ctx) ) {
			case 1:
				{
				setState(2074);
				match(OUTER);
				}
				break;
			}
			setState(2077);
			qualifiedName();
			setState(2078);
			match(T__1);
			setState(2087);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,255,_ctx) ) {
			case 1:
				{
				setState(2079);
				expression();
				setState(2084);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(2080);
					match(T__3);
					setState(2081);
					expression();
					}
					}
					setState(2086);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
			setState(2089);
			match(T__2);
			setState(2090);
			((LateralViewContext)_localctx).tblName = identifier();
			setState(2102);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,258,_ctx) ) {
			case 1:
				{
				setState(2092);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,256,_ctx) ) {
				case 1:
					{
					setState(2091);
					match(AS);
					}
					break;
				}
				setState(2094);
				((LateralViewContext)_localctx).identifier = identifier();
				((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
				setState(2099);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,257,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2095);
						match(T__3);
						setState(2096);
						((LateralViewContext)_localctx).identifier = identifier();
						((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
						}
						} 
					}
					setState(2101);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,257,_ctx);
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
		public TerminalNode DISTINCT() { return getToken(ArcticSqlCommandParser.DISTINCT, 0); }
		public TerminalNode ALL() { return getToken(ArcticSqlCommandParser.ALL, 0); }
		public SetQuantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setQuantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSetQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSetQuantifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSetQuantifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetQuantifierContext setQuantifier() throws RecognitionException {
		SetQuantifierContext _localctx = new SetQuantifierContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_setQuantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2104);
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
		public TerminalNode LATERAL() { return getToken(ArcticSqlCommandParser.LATERAL, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationContext relation() throws RecognitionException {
		RelationContext _localctx = new RelationContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_relation);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2107);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,259,_ctx) ) {
			case 1:
				{
				setState(2106);
				match(LATERAL);
				}
				break;
			}
			setState(2109);
			relationPrimary();
			setState(2113);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,260,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2110);
					joinRelation();
					}
					} 
				}
				setState(2115);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,260,_ctx);
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
		public TerminalNode JOIN() { return getToken(ArcticSqlCommandParser.JOIN, 0); }
		public RelationPrimaryContext relationPrimary() {
			return getRuleContext(RelationPrimaryContext.class,0);
		}
		public JoinTypeContext joinType() {
			return getRuleContext(JoinTypeContext.class,0);
		}
		public TerminalNode LATERAL() { return getToken(ArcticSqlCommandParser.LATERAL, 0); }
		public JoinCriteriaContext joinCriteria() {
			return getRuleContext(JoinCriteriaContext.class,0);
		}
		public TerminalNode NATURAL() { return getToken(ArcticSqlCommandParser.NATURAL, 0); }
		public JoinRelationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinRelation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterJoinRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitJoinRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitJoinRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinRelationContext joinRelation() throws RecognitionException {
		JoinRelationContext _localctx = new JoinRelationContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_joinRelation);
		try {
			setState(2133);
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
				setState(2116);
				joinType();
				}
				setState(2117);
				match(JOIN);
				setState(2119);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,261,_ctx) ) {
				case 1:
					{
					setState(2118);
					match(LATERAL);
					}
					break;
				}
				setState(2121);
				((JoinRelationContext)_localctx).right = relationPrimary();
				setState(2123);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,262,_ctx) ) {
				case 1:
					{
					setState(2122);
					joinCriteria();
					}
					break;
				}
				}
				break;
			case NATURAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(2125);
				match(NATURAL);
				setState(2126);
				joinType();
				setState(2127);
				match(JOIN);
				setState(2129);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,263,_ctx) ) {
				case 1:
					{
					setState(2128);
					match(LATERAL);
					}
					break;
				}
				setState(2131);
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
		public TerminalNode INNER() { return getToken(ArcticSqlCommandParser.INNER, 0); }
		public TerminalNode CROSS() { return getToken(ArcticSqlCommandParser.CROSS, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSqlCommandParser.LEFT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticSqlCommandParser.OUTER, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSqlCommandParser.SEMI, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSqlCommandParser.RIGHT, 0); }
		public TerminalNode FULL() { return getToken(ArcticSqlCommandParser.FULL, 0); }
		public TerminalNode ANTI() { return getToken(ArcticSqlCommandParser.ANTI, 0); }
		public JoinTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterJoinType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitJoinType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitJoinType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinTypeContext joinType() throws RecognitionException {
		JoinTypeContext _localctx = new JoinTypeContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_joinType);
		int _la;
		try {
			setState(2159);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,271,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2136);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INNER) {
					{
					setState(2135);
					match(INNER);
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2138);
				match(CROSS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2139);
				match(LEFT);
				setState(2141);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(2140);
					match(OUTER);
					}
				}

				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(2144);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(2143);
					match(LEFT);
					}
				}

				setState(2146);
				match(SEMI);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(2147);
				match(RIGHT);
				setState(2149);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(2148);
					match(OUTER);
					}
				}

				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(2151);
				match(FULL);
				setState(2153);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(2152);
					match(OUTER);
					}
				}

				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(2156);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(2155);
					match(LEFT);
					}
				}

				setState(2158);
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
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public TerminalNode USING() { return getToken(ArcticSqlCommandParser.USING, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public JoinCriteriaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinCriteria; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterJoinCriteria(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitJoinCriteria(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitJoinCriteria(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinCriteriaContext joinCriteria() throws RecognitionException {
		JoinCriteriaContext _localctx = new JoinCriteriaContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_joinCriteria);
		try {
			setState(2165);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ON:
				enterOuterAlt(_localctx, 1);
				{
				setState(2161);
				match(ON);
				setState(2162);
				booleanExpression(0);
				}
				break;
			case USING:
				enterOuterAlt(_localctx, 2);
				{
				setState(2163);
				match(USING);
				setState(2164);
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
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSqlCommandParser.TABLESAMPLE, 0); }
		public SampleMethodContext sampleMethod() {
			return getRuleContext(SampleMethodContext.class,0);
		}
		public SampleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sample; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSample(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSample(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSample(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleContext sample() throws RecognitionException {
		SampleContext _localctx = new SampleContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_sample);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2167);
			match(TABLESAMPLE);
			setState(2168);
			match(T__1);
			setState(2170);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,273,_ctx) ) {
			case 1:
				{
				setState(2169);
				sampleMethod();
				}
				break;
			}
			setState(2172);
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
		public TerminalNode ROWS() { return getToken(ArcticSqlCommandParser.ROWS, 0); }
		public SampleByRowsContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSampleByRows(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSampleByRows(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSampleByRows(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByPercentileContext extends SampleMethodContext {
		public Token negativeSign;
		public Token percentage;
		public TerminalNode PERCENTLIT() { return getToken(ArcticSqlCommandParser.PERCENTLIT, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlCommandParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlCommandParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public SampleByPercentileContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSampleByPercentile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSampleByPercentile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSampleByPercentile(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByBucketContext extends SampleMethodContext {
		public Token sampleType;
		public Token numerator;
		public Token denominator;
		public TerminalNode OUT() { return getToken(ArcticSqlCommandParser.OUT, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlCommandParser.OF, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSqlCommandParser.BUCKET, 0); }
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticSqlCommandParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticSqlCommandParser.INTEGER_VALUE, i);
		}
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public SampleByBucketContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSampleByBucket(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSampleByBucket(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSampleByBucket(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSampleByBytes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSampleByBytes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSampleByBytes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleMethodContext sampleMethod() throws RecognitionException {
		SampleMethodContext _localctx = new SampleMethodContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_sampleMethod);
		int _la;
		try {
			setState(2198);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,277,_ctx) ) {
			case 1:
				_localctx = new SampleByPercentileContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2175);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(2174);
					((SampleByPercentileContext)_localctx).negativeSign = match(MINUS);
					}
				}

				setState(2177);
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
				setState(2178);
				match(PERCENTLIT);
				}
				break;
			case 2:
				_localctx = new SampleByRowsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2179);
				expression();
				setState(2180);
				match(ROWS);
				}
				break;
			case 3:
				_localctx = new SampleByBucketContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2182);
				((SampleByBucketContext)_localctx).sampleType = match(BUCKET);
				setState(2183);
				((SampleByBucketContext)_localctx).numerator = match(INTEGER_VALUE);
				setState(2184);
				match(OUT);
				setState(2185);
				match(OF);
				setState(2186);
				((SampleByBucketContext)_localctx).denominator = match(INTEGER_VALUE);
				setState(2195);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ON) {
					{
					setState(2187);
					match(ON);
					setState(2193);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,275,_ctx) ) {
					case 1:
						{
						setState(2188);
						identifier();
						}
						break;
					case 2:
						{
						setState(2189);
						qualifiedName();
						setState(2190);
						match(T__1);
						setState(2191);
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
				setState(2197);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierListContext identifierList() throws RecognitionException {
		IdentifierListContext _localctx = new IdentifierListContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_identifierList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2200);
			match(T__1);
			setState(2201);
			identifierSeq();
			setState(2202);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIdentifierSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIdentifierSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIdentifierSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierSeqContext identifierSeq() throws RecognitionException {
		IdentifierSeqContext _localctx = new IdentifierSeqContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_identifierSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2204);
			((IdentifierSeqContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
			((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).errorCapturingIdentifier);
			setState(2209);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,278,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2205);
					match(T__3);
					setState(2206);
					((IdentifierSeqContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
					((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).errorCapturingIdentifier);
					}
					} 
				}
				setState(2211);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,278,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterOrderedIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitOrderedIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitOrderedIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierListContext orderedIdentifierList() throws RecognitionException {
		OrderedIdentifierListContext _localctx = new OrderedIdentifierListContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_orderedIdentifierList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2212);
			match(T__1);
			setState(2213);
			orderedIdentifier();
			setState(2218);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2214);
				match(T__3);
				setState(2215);
				orderedIdentifier();
				}
				}
				setState(2220);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2221);
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
		public TerminalNode ASC() { return getToken(ArcticSqlCommandParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public OrderedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterOrderedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitOrderedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitOrderedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierContext orderedIdentifier() throws RecognitionException {
		OrderedIdentifierContext _localctx = new OrderedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_orderedIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2223);
			((OrderedIdentifierContext)_localctx).ident = errorCapturingIdentifier();
			setState(2225);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(2224);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIdentifierCommentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIdentifierCommentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIdentifierCommentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierCommentListContext identifierCommentList() throws RecognitionException {
		IdentifierCommentListContext _localctx = new IdentifierCommentListContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_identifierCommentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2227);
			match(T__1);
			setState(2228);
			identifierComment();
			setState(2233);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2229);
				match(T__3);
				setState(2230);
				identifierComment();
				}
				}
				setState(2235);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2236);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIdentifierComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIdentifierComment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIdentifierComment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierCommentContext identifierComment() throws RecognitionException {
		IdentifierCommentContext _localctx = new IdentifierCommentContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_identifierComment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2238);
			identifier();
			setState(2240);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(2239);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableValuedFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableValuedFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableValuedFunction(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInlineTableDefault2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInlineTableDefault2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInlineTableDefault2(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAliasedRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAliasedRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAliasedRelation(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAliasedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAliasedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAliasedQuery(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationPrimaryContext relationPrimary() throws RecognitionException {
		RelationPrimaryContext _localctx = new RelationPrimaryContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_relationPrimary);
		try {
			setState(2266);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,286,_ctx) ) {
			case 1:
				_localctx = new TableNameContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2242);
				multipartIdentifier();
				setState(2244);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,283,_ctx) ) {
				case 1:
					{
					setState(2243);
					sample();
					}
					break;
				}
				setState(2246);
				tableAlias();
				}
				break;
			case 2:
				_localctx = new AliasedQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2248);
				match(T__1);
				setState(2249);
				query();
				setState(2250);
				match(T__2);
				setState(2252);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,284,_ctx) ) {
				case 1:
					{
					setState(2251);
					sample();
					}
					break;
				}
				setState(2254);
				tableAlias();
				}
				break;
			case 3:
				_localctx = new AliasedRelationContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2256);
				match(T__1);
				setState(2257);
				relation();
				setState(2258);
				match(T__2);
				setState(2260);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,285,_ctx) ) {
				case 1:
					{
					setState(2259);
					sample();
					}
					break;
				}
				setState(2262);
				tableAlias();
				}
				break;
			case 4:
				_localctx = new InlineTableDefault2Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2264);
				inlineTable();
				}
				break;
			case 5:
				_localctx = new TableValuedFunctionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(2265);
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
		public TerminalNode VALUES() { return getToken(ArcticSqlCommandParser.VALUES, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInlineTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInlineTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInlineTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableContext inlineTable() throws RecognitionException {
		InlineTableContext _localctx = new InlineTableContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_inlineTable);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2268);
			match(VALUES);
			setState(2269);
			expression();
			setState(2274);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,287,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2270);
					match(T__3);
					setState(2271);
					expression();
					}
					} 
				}
				setState(2276);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,287,_ctx);
			}
			setState(2277);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFunctionTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFunctionTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFunctionTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionTableContext functionTable() throws RecognitionException {
		FunctionTableContext _localctx = new FunctionTableContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_functionTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2279);
			((FunctionTableContext)_localctx).funcName = functionName();
			setState(2280);
			match(T__1);
			setState(2289);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,289,_ctx) ) {
			case 1:
				{
				setState(2281);
				expression();
				setState(2286);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(2282);
					match(T__3);
					setState(2283);
					expression();
					}
					}
					setState(2288);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
			setState(2291);
			match(T__2);
			setState(2292);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TableAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableAlias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableAlias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableAliasContext tableAlias() throws RecognitionException {
		TableAliasContext _localctx = new TableAliasContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_tableAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2301);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,292,_ctx) ) {
			case 1:
				{
				setState(2295);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,290,_ctx) ) {
				case 1:
					{
					setState(2294);
					match(AS);
					}
					break;
				}
				setState(2297);
				strictIdentifier();
				setState(2299);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,291,_ctx) ) {
				case 1:
					{
					setState(2298);
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
		public TerminalNode ROW() { return getToken(ArcticSqlCommandParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlCommandParser.FORMAT, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlCommandParser.SERDE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticSqlCommandParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlCommandParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public RowFormatSerdeContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRowFormatSerde(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRowFormatSerde(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRowFormatSerde(this);
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
		public TerminalNode ROW() { return getToken(ArcticSqlCommandParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlCommandParser.FORMAT, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSqlCommandParser.DELIMITED, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSqlCommandParser.FIELDS, 0); }
		public List<TerminalNode> TERMINATED() { return getTokens(ArcticSqlCommandParser.TERMINATED); }
		public TerminalNode TERMINATED(int i) {
			return getToken(ArcticSqlCommandParser.TERMINATED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticSqlCommandParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlCommandParser.BY, i);
		}
		public TerminalNode COLLECTION() { return getToken(ArcticSqlCommandParser.COLLECTION, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSqlCommandParser.ITEMS, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlCommandParser.MAP, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSqlCommandParser.KEYS, 0); }
		public TerminalNode LINES() { return getToken(ArcticSqlCommandParser.LINES, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSqlCommandParser.DEFINED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlCommandParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlCommandParser.STRING, i);
		}
		public TerminalNode ESCAPED() { return getToken(ArcticSqlCommandParser.ESCAPED, 0); }
		public RowFormatDelimitedContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRowFormatDelimited(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRowFormatDelimited(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRowFormatDelimited(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RowFormatContext rowFormat() throws RecognitionException {
		RowFormatContext _localctx = new RowFormatContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_rowFormat);
		try {
			setState(2352);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,300,_ctx) ) {
			case 1:
				_localctx = new RowFormatSerdeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2303);
				match(ROW);
				setState(2304);
				match(FORMAT);
				setState(2305);
				match(SERDE);
				setState(2306);
				((RowFormatSerdeContext)_localctx).name = match(STRING);
				setState(2310);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,293,_ctx) ) {
				case 1:
					{
					setState(2307);
					match(WITH);
					setState(2308);
					match(SERDEPROPERTIES);
					setState(2309);
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
				setState(2312);
				match(ROW);
				setState(2313);
				match(FORMAT);
				setState(2314);
				match(DELIMITED);
				setState(2324);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,295,_ctx) ) {
				case 1:
					{
					setState(2315);
					match(FIELDS);
					setState(2316);
					match(TERMINATED);
					setState(2317);
					match(BY);
					setState(2318);
					((RowFormatDelimitedContext)_localctx).fieldsTerminatedBy = match(STRING);
					setState(2322);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,294,_ctx) ) {
					case 1:
						{
						setState(2319);
						match(ESCAPED);
						setState(2320);
						match(BY);
						setState(2321);
						((RowFormatDelimitedContext)_localctx).escapedBy = match(STRING);
						}
						break;
					}
					}
					break;
				}
				setState(2331);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,296,_ctx) ) {
				case 1:
					{
					setState(2326);
					match(COLLECTION);
					setState(2327);
					match(ITEMS);
					setState(2328);
					match(TERMINATED);
					setState(2329);
					match(BY);
					setState(2330);
					((RowFormatDelimitedContext)_localctx).collectionItemsTerminatedBy = match(STRING);
					}
					break;
				}
				setState(2338);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,297,_ctx) ) {
				case 1:
					{
					setState(2333);
					match(MAP);
					setState(2334);
					match(KEYS);
					setState(2335);
					match(TERMINATED);
					setState(2336);
					match(BY);
					setState(2337);
					((RowFormatDelimitedContext)_localctx).keysTerminatedBy = match(STRING);
					}
					break;
				}
				setState(2344);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,298,_ctx) ) {
				case 1:
					{
					setState(2340);
					match(LINES);
					setState(2341);
					match(TERMINATED);
					setState(2342);
					match(BY);
					setState(2343);
					((RowFormatDelimitedContext)_localctx).linesSeparatedBy = match(STRING);
					}
					break;
				}
				setState(2350);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,299,_ctx) ) {
				case 1:
					{
					setState(2346);
					match(NULL);
					setState(2347);
					match(DEFINED);
					setState(2348);
					match(AS);
					setState(2349);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMultipartIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMultipartIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMultipartIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultipartIdentifierListContext multipartIdentifierList() throws RecognitionException {
		MultipartIdentifierListContext _localctx = new MultipartIdentifierListContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_multipartIdentifierList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2354);
			multipartIdentifier();
			setState(2359);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2355);
				match(T__3);
				setState(2356);
				multipartIdentifier();
				}
				}
				setState(2361);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMultipartIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMultipartIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMultipartIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultipartIdentifierContext multipartIdentifier() throws RecognitionException {
		MultipartIdentifierContext _localctx = new MultipartIdentifierContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_multipartIdentifier);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2362);
			((MultipartIdentifierContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
			((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).errorCapturingIdentifier);
			setState(2367);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,302,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2363);
					match(T__4);
					setState(2364);
					((MultipartIdentifierContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
					((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).errorCapturingIdentifier);
					}
					} 
				}
				setState(2369);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,302,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableIdentifierContext tableIdentifier() throws RecognitionException {
		TableIdentifierContext _localctx = new TableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_tableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2373);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,303,_ctx) ) {
			case 1:
				{
				setState(2370);
				((TableIdentifierContext)_localctx).db = errorCapturingIdentifier();
				setState(2371);
				match(T__4);
				}
				break;
			}
			setState(2375);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFunctionIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFunctionIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFunctionIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionIdentifierContext functionIdentifier() throws RecognitionException {
		FunctionIdentifierContext _localctx = new FunctionIdentifierContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_functionIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2380);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,304,_ctx) ) {
			case 1:
				{
				setState(2377);
				((FunctionIdentifierContext)_localctx).db = errorCapturingIdentifier();
				setState(2378);
				match(T__4);
				}
				break;
			}
			setState(2382);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public NamedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNamedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNamedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNamedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionContext namedExpression() throws RecognitionException {
		NamedExpressionContext _localctx = new NamedExpressionContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_namedExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2384);
			expression();
			setState(2392);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,307,_ctx) ) {
			case 1:
				{
				setState(2386);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,305,_ctx) ) {
				case 1:
					{
					setState(2385);
					match(AS);
					}
					break;
				}
				setState(2390);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,306,_ctx) ) {
				case 1:
					{
					setState(2388);
					((NamedExpressionContext)_localctx).name = errorCapturingIdentifier();
					}
					break;
				case 2:
					{
					setState(2389);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNamedExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNamedExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNamedExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionSeqContext namedExpressionSeq() throws RecognitionException {
		NamedExpressionSeqContext _localctx = new NamedExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_namedExpressionSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2394);
			namedExpression();
			setState(2399);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,308,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2395);
					match(T__3);
					setState(2396);
					namedExpression();
					}
					} 
				}
				setState(2401);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,308,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPartitionFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPartitionFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPartitionFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldListContext partitionFieldList() throws RecognitionException {
		PartitionFieldListContext _localctx = new PartitionFieldListContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_partitionFieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2402);
			match(T__1);
			setState(2403);
			((PartitionFieldListContext)_localctx).partitionField = partitionField();
			((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
			setState(2408);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2404);
				match(T__3);
				setState(2405);
				((PartitionFieldListContext)_localctx).partitionField = partitionField();
				((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
				}
				}
				setState(2410);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2411);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPartitionColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPartitionColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPartitionColumn(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPartitionTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPartitionTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPartitionTransform(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldContext partitionField() throws RecognitionException {
		PartitionFieldContext _localctx = new PartitionFieldContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_partitionField);
		try {
			setState(2415);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,310,_ctx) ) {
			case 1:
				_localctx = new PartitionTransformContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2413);
				transform();
				}
				break;
			case 2:
				_localctx = new PartitionColumnContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2414);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIdentityTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIdentityTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIdentityTransform(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterApplyTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitApplyTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitApplyTransform(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformContext transform() throws RecognitionException {
		TransformContext _localctx = new TransformContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_transform);
		int _la;
		try {
			setState(2430);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,312,_ctx) ) {
			case 1:
				_localctx = new IdentityTransformContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2417);
				qualifiedName();
				}
				break;
			case 2:
				_localctx = new ApplyTransformContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2418);
				((ApplyTransformContext)_localctx).transformName = identifier();
				setState(2419);
				match(T__1);
				setState(2420);
				((ApplyTransformContext)_localctx).transformArgument = transformArgument();
				((ApplyTransformContext)_localctx).argument.add(((ApplyTransformContext)_localctx).transformArgument);
				setState(2425);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(2421);
					match(T__3);
					setState(2422);
					((ApplyTransformContext)_localctx).transformArgument = transformArgument();
					((ApplyTransformContext)_localctx).argument.add(((ApplyTransformContext)_localctx).transformArgument);
					}
					}
					setState(2427);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2428);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTransformArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTransformArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTransformArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformArgumentContext transformArgument() throws RecognitionException {
		TransformArgumentContext _localctx = new TransformArgumentContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_transformArgument);
		try {
			setState(2434);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,313,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2432);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2433);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2436);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionSeqContext expressionSeq() throws RecognitionException {
		ExpressionSeqContext _localctx = new ExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_expressionSeq);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2438);
			expression();
			setState(2443);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2439);
				match(T__3);
				setState(2440);
				expression();
				}
				}
				setState(2445);
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
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public LogicalNotContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLogicalNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLogicalNot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLogicalNot(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPredicated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPredicated(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPredicated(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExistsContext extends BooleanExpressionContext {
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public ExistsContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterExists(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitExists(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitExists(this);
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
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public LogicalBinaryContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLogicalBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLogicalBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLogicalBinary(this);
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
		int _startState = 206;
		enterRecursionRule(_localctx, 206, RULE_booleanExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2458);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,316,_ctx) ) {
			case 1:
				{
				_localctx = new LogicalNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(2447);
				match(NOT);
				setState(2448);
				booleanExpression(5);
				}
				break;
			case 2:
				{
				_localctx = new ExistsContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2449);
				match(EXISTS);
				setState(2450);
				match(T__1);
				setState(2451);
				query();
				setState(2452);
				match(T__2);
				}
				break;
			case 3:
				{
				_localctx = new PredicatedContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2454);
				valueExpression(0);
				setState(2456);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,315,_ctx) ) {
				case 1:
					{
					setState(2455);
					predicate();
					}
					break;
				}
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(2468);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,318,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(2466);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,317,_ctx) ) {
					case 1:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(2460);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(2461);
						((LogicalBinaryContext)_localctx).operator = match(AND);
						setState(2462);
						((LogicalBinaryContext)_localctx).right = booleanExpression(3);
						}
						break;
					case 2:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(2463);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(2464);
						((LogicalBinaryContext)_localctx).operator = match(OR);
						setState(2465);
						((LogicalBinaryContext)_localctx).right = booleanExpression(2);
						}
						break;
					}
					} 
				}
				setState(2470);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,318,_ctx);
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
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlCommandParser.BETWEEN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RLIKE() { return getToken(ArcticSqlCommandParser.RLIKE, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public TerminalNode ANY() { return getToken(ArcticSqlCommandParser.ANY, 0); }
		public TerminalNode SOME() { return getToken(ArcticSqlCommandParser.SOME, 0); }
		public TerminalNode ALL() { return getToken(ArcticSqlCommandParser.ALL, 0); }
		public TerminalNode ESCAPE() { return getToken(ArcticSqlCommandParser.ESCAPE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode IS() { return getToken(ArcticSqlCommandParser.IS, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSqlCommandParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSqlCommandParser.FALSE, 0); }
		public TerminalNode UNKNOWN() { return getToken(ArcticSqlCommandParser.UNKNOWN, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticSqlCommandParser.DISTINCT, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_predicate);
		int _la;
		try {
			setState(2553);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,332,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2472);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2471);
					match(NOT);
					}
				}

				setState(2474);
				((PredicateContext)_localctx).kind = match(BETWEEN);
				setState(2475);
				((PredicateContext)_localctx).lower = valueExpression(0);
				setState(2476);
				match(AND);
				setState(2477);
				((PredicateContext)_localctx).upper = valueExpression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2480);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2479);
					match(NOT);
					}
				}

				setState(2482);
				((PredicateContext)_localctx).kind = match(IN);
				setState(2483);
				match(T__1);
				setState(2484);
				expression();
				setState(2489);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(2485);
					match(T__3);
					setState(2486);
					expression();
					}
					}
					setState(2491);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2492);
				match(T__2);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2495);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2494);
					match(NOT);
					}
				}

				setState(2497);
				((PredicateContext)_localctx).kind = match(IN);
				setState(2498);
				match(T__1);
				setState(2499);
				query();
				setState(2500);
				match(T__2);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(2503);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2502);
					match(NOT);
					}
				}

				setState(2505);
				((PredicateContext)_localctx).kind = match(RLIKE);
				setState(2506);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(2508);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2507);
					match(NOT);
					}
				}

				setState(2510);
				((PredicateContext)_localctx).kind = match(LIKE);
				setState(2511);
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
				setState(2525);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,326,_ctx) ) {
				case 1:
					{
					setState(2512);
					match(T__1);
					setState(2513);
					match(T__2);
					}
					break;
				case 2:
					{
					setState(2514);
					match(T__1);
					setState(2515);
					expression();
					setState(2520);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(2516);
						match(T__3);
						setState(2517);
						expression();
						}
						}
						setState(2522);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(2523);
					match(T__2);
					}
					break;
				}
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(2528);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2527);
					match(NOT);
					}
				}

				setState(2530);
				((PredicateContext)_localctx).kind = match(LIKE);
				setState(2531);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				setState(2534);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,328,_ctx) ) {
				case 1:
					{
					setState(2532);
					match(ESCAPE);
					setState(2533);
					((PredicateContext)_localctx).escapeChar = match(STRING);
					}
					break;
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(2536);
				match(IS);
				setState(2538);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2537);
					match(NOT);
					}
				}

				setState(2540);
				((PredicateContext)_localctx).kind = match(NULL);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(2541);
				match(IS);
				setState(2543);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2542);
					match(NOT);
					}
				}

				setState(2545);
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
				setState(2546);
				match(IS);
				setState(2548);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(2547);
					match(NOT);
					}
				}

				setState(2550);
				((PredicateContext)_localctx).kind = match(DISTINCT);
				setState(2551);
				match(FROM);
				setState(2552);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterValueExpressionDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitValueExpressionDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitValueExpressionDefault(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitComparison(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitComparison(this);
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
		public TerminalNode ASTERISK() { return getToken(ArcticSqlCommandParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticSqlCommandParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticSqlCommandParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlCommandParser.DIV, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSqlCommandParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticSqlCommandParser.CONCAT_PIPE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticSqlCommandParser.AMPERSAND, 0); }
		public TerminalNode HAT() { return getToken(ArcticSqlCommandParser.HAT, 0); }
		public TerminalNode PIPE() { return getToken(ArcticSqlCommandParser.PIPE, 0); }
		public ArithmeticBinaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterArithmeticBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitArithmeticBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitArithmeticBinary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ArithmeticUnaryContext extends ValueExpressionContext {
		public Token operator;
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSqlCommandParser.PLUS, 0); }
		public TerminalNode TILDE() { return getToken(ArcticSqlCommandParser.TILDE, 0); }
		public ArithmeticUnaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterArithmeticUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitArithmeticUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitArithmeticUnary(this);
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
		int _startState = 210;
		enterRecursionRule(_localctx, 210, RULE_valueExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2559);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,333,_ctx) ) {
			case 1:
				{
				_localctx = new ValueExpressionDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(2556);
				primaryExpression(0);
				}
				break;
			case 2:
				{
				_localctx = new ArithmeticUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2557);
				((ArithmeticUnaryContext)_localctx).operator = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 281)) & ~0x3f) == 0 && ((1L << (_la - 281)) & ((1L << (PLUS - 281)) | (1L << (MINUS - 281)) | (1L << (TILDE - 281)))) != 0)) ) {
					((ArithmeticUnaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2558);
				valueExpression(7);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(2582);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,335,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(2580);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,334,_ctx) ) {
					case 1:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2561);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(2562);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==DIV || ((((_la - 283)) & ~0x3f) == 0 && ((1L << (_la - 283)) & ((1L << (ASTERISK - 283)) | (1L << (SLASH - 283)) | (1L << (PERCENT - 283)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(2563);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(7);
						}
						break;
					case 2:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2564);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(2565);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 281)) & ~0x3f) == 0 && ((1L << (_la - 281)) & ((1L << (PLUS - 281)) | (1L << (MINUS - 281)) | (1L << (CONCAT_PIPE - 281)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(2566);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(6);
						}
						break;
					case 3:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2567);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(2568);
						((ArithmeticBinaryContext)_localctx).operator = match(AMPERSAND);
						setState(2569);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(5);
						}
						break;
					case 4:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2570);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(2571);
						((ArithmeticBinaryContext)_localctx).operator = match(HAT);
						setState(2572);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(4);
						}
						break;
					case 5:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2573);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(2574);
						((ArithmeticBinaryContext)_localctx).operator = match(PIPE);
						setState(2575);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(3);
						}
						break;
					case 6:
						{
						_localctx = new ComparisonContext(new ValueExpressionContext(_parentctx, _parentState));
						((ComparisonContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(2576);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(2577);
						comparisonOperator();
						setState(2578);
						((ComparisonContext)_localctx).right = valueExpression(2);
						}
						break;
					}
					} 
				}
				setState(2584);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,335,_ctx);
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
		public TerminalNode STRUCT() { return getToken(ArcticSqlCommandParser.STRUCT, 0); }
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public StructContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterStruct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitStruct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitStruct(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDereference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDereference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDereference(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleCaseContext extends PrimaryExpressionContext {
		public ExpressionContext value;
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticSqlCommandParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticSqlCommandParser.END, 0); }
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
		public TerminalNode ELSE() { return getToken(ArcticSqlCommandParser.ELSE, 0); }
		public SimpleCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSimpleCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSimpleCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSimpleCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CurrentLikeContext extends PrimaryExpressionContext {
		public Token name;
		public TerminalNode CURRENT_DATE() { return getToken(ArcticSqlCommandParser.CURRENT_DATE, 0); }
		public TerminalNode CURRENT_TIMESTAMP() { return getToken(ArcticSqlCommandParser.CURRENT_TIMESTAMP, 0); }
		public TerminalNode CURRENT_USER() { return getToken(ArcticSqlCommandParser.CURRENT_USER, 0); }
		public CurrentLikeContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCurrentLike(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCurrentLike(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCurrentLike(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterColumnReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitColumnReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitColumnReference(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRowConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRowConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRowConstructor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LastContext extends PrimaryExpressionContext {
		public TerminalNode LAST() { return getToken(ArcticSqlCommandParser.LAST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticSqlCommandParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlCommandParser.NULLS, 0); }
		public LastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLast(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StarContext extends PrimaryExpressionContext {
		public TerminalNode ASTERISK() { return getToken(ArcticSqlCommandParser.ASTERISK, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public StarContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterStar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitStar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitStar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OverlayContext extends PrimaryExpressionContext {
		public ValueExpressionContext input;
		public ValueExpressionContext replace;
		public ValueExpressionContext position;
		public ValueExpressionContext length;
		public TerminalNode OVERLAY() { return getToken(ArcticSqlCommandParser.OVERLAY, 0); }
		public TerminalNode PLACING() { return getToken(ArcticSqlCommandParser.PLACING, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode FOR() { return getToken(ArcticSqlCommandParser.FOR, 0); }
		public OverlayContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterOverlay(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitOverlay(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitOverlay(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSubscript(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSubscript(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSubscript(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSubqueryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSubqueryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSubqueryExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubstringContext extends PrimaryExpressionContext {
		public ValueExpressionContext str;
		public ValueExpressionContext pos;
		public ValueExpressionContext len;
		public TerminalNode SUBSTR() { return getToken(ArcticSqlCommandParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticSqlCommandParser.SUBSTRING, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode FOR() { return getToken(ArcticSqlCommandParser.FOR, 0); }
		public SubstringContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSubstring(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSubstring(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSubstring(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CastContext extends PrimaryExpressionContext {
		public Token name;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode CAST() { return getToken(ArcticSqlCommandParser.CAST, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticSqlCommandParser.TRY_CAST, 0); }
		public CastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitCast(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterConstantDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitConstantDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitConstantDefault(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLambda(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLambda(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLambda(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterParenthesizedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitParenthesizedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitParenthesizedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExtractContext extends PrimaryExpressionContext {
		public IdentifierContext field;
		public ValueExpressionContext source;
		public TerminalNode EXTRACT() { return getToken(ArcticSqlCommandParser.EXTRACT, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public ExtractContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterExtract(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitExtract(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitExtract(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TrimContext extends PrimaryExpressionContext {
		public Token trimOption;
		public ValueExpressionContext trimStr;
		public ValueExpressionContext srcStr;
		public TerminalNode TRIM() { return getToken(ArcticSqlCommandParser.TRIM, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode BOTH() { return getToken(ArcticSqlCommandParser.BOTH, 0); }
		public TerminalNode LEADING() { return getToken(ArcticSqlCommandParser.LEADING, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticSqlCommandParser.TRAILING, 0); }
		public TrimContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTrim(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTrim(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTrim(this);
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
		public TerminalNode FILTER() { return getToken(ArcticSqlCommandParser.FILTER, 0); }
		public TerminalNode WHERE() { return getToken(ArcticSqlCommandParser.WHERE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlCommandParser.NULLS, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlCommandParser.OVER, 0); }
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
		public TerminalNode IGNORE() { return getToken(ArcticSqlCommandParser.IGNORE, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticSqlCommandParser.RESPECT, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public FunctionCallContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SearchedCaseContext extends PrimaryExpressionContext {
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticSqlCommandParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticSqlCommandParser.END, 0); }
		public List<WhenClauseContext> whenClause() {
			return getRuleContexts(WhenClauseContext.class);
		}
		public WhenClauseContext whenClause(int i) {
			return getRuleContext(WhenClauseContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(ArcticSqlCommandParser.ELSE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SearchedCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSearchedCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSearchedCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSearchedCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PositionContext extends PrimaryExpressionContext {
		public ValueExpressionContext substr;
		public ValueExpressionContext str;
		public TerminalNode POSITION() { return getToken(ArcticSqlCommandParser.POSITION, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public PositionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPosition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FirstContext extends PrimaryExpressionContext {
		public TerminalNode FIRST() { return getToken(ArcticSqlCommandParser.FIRST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticSqlCommandParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlCommandParser.NULLS, 0); }
		public FirstContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFirst(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFirst(this);
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
		int _startState = 212;
		enterRecursionRule(_localctx, 212, RULE_primaryExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2773);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,356,_ctx) ) {
			case 1:
				{
				_localctx = new CurrentLikeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(2586);
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
				setState(2587);
				match(CASE);
				setState(2589); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2588);
					whenClause();
					}
					}
					setState(2591); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(2595);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(2593);
					match(ELSE);
					setState(2594);
					((SearchedCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(2597);
				match(END);
				}
				break;
			case 3:
				{
				_localctx = new SimpleCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2599);
				match(CASE);
				setState(2600);
				((SimpleCaseContext)_localctx).value = expression();
				setState(2602); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2601);
					whenClause();
					}
					}
					setState(2604); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(2608);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(2606);
					match(ELSE);
					setState(2607);
					((SimpleCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(2610);
				match(END);
				}
				break;
			case 4:
				{
				_localctx = new CastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2612);
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
				setState(2613);
				match(T__1);
				setState(2614);
				expression();
				setState(2615);
				match(AS);
				setState(2616);
				dataType();
				setState(2617);
				match(T__2);
				}
				break;
			case 5:
				{
				_localctx = new StructContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2619);
				match(STRUCT);
				setState(2620);
				match(T__1);
				setState(2629);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,341,_ctx) ) {
				case 1:
					{
					setState(2621);
					((StructContext)_localctx).namedExpression = namedExpression();
					((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
					setState(2626);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(2622);
						match(T__3);
						setState(2623);
						((StructContext)_localctx).namedExpression = namedExpression();
						((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
						}
						}
						setState(2628);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(2631);
				match(T__2);
				}
				break;
			case 6:
				{
				_localctx = new FirstContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2632);
				match(FIRST);
				setState(2633);
				match(T__1);
				setState(2634);
				expression();
				setState(2637);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(2635);
					match(IGNORE);
					setState(2636);
					match(NULLS);
					}
				}

				setState(2639);
				match(T__2);
				}
				break;
			case 7:
				{
				_localctx = new LastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2641);
				match(LAST);
				setState(2642);
				match(T__1);
				setState(2643);
				expression();
				setState(2646);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(2644);
					match(IGNORE);
					setState(2645);
					match(NULLS);
					}
				}

				setState(2648);
				match(T__2);
				}
				break;
			case 8:
				{
				_localctx = new PositionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2650);
				match(POSITION);
				setState(2651);
				match(T__1);
				setState(2652);
				((PositionContext)_localctx).substr = valueExpression(0);
				setState(2653);
				match(IN);
				setState(2654);
				((PositionContext)_localctx).str = valueExpression(0);
				setState(2655);
				match(T__2);
				}
				break;
			case 9:
				{
				_localctx = new ConstantDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2657);
				constant();
				}
				break;
			case 10:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2658);
				match(ASTERISK);
				}
				break;
			case 11:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2659);
				qualifiedName();
				setState(2660);
				match(T__4);
				setState(2661);
				match(ASTERISK);
				}
				break;
			case 12:
				{
				_localctx = new RowConstructorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2663);
				match(T__1);
				setState(2664);
				namedExpression();
				setState(2667); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2665);
					match(T__3);
					setState(2666);
					namedExpression();
					}
					}
					setState(2669); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__3 );
				setState(2671);
				match(T__2);
				}
				break;
			case 13:
				{
				_localctx = new SubqueryExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2673);
				match(T__1);
				setState(2674);
				query();
				setState(2675);
				match(T__2);
				}
				break;
			case 14:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2677);
				functionName();
				setState(2678);
				match(T__1);
				setState(2690);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,347,_ctx) ) {
				case 1:
					{
					setState(2680);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,345,_ctx) ) {
					case 1:
						{
						setState(2679);
						setQuantifier();
						}
						break;
					}
					setState(2682);
					((FunctionCallContext)_localctx).expression = expression();
					((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
					setState(2687);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(2683);
						match(T__3);
						setState(2684);
						((FunctionCallContext)_localctx).expression = expression();
						((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
						}
						}
						setState(2689);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(2692);
				match(T__2);
				setState(2699);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,348,_ctx) ) {
				case 1:
					{
					setState(2693);
					match(FILTER);
					setState(2694);
					match(T__1);
					setState(2695);
					match(WHERE);
					setState(2696);
					((FunctionCallContext)_localctx).where = booleanExpression(0);
					setState(2697);
					match(T__2);
					}
					break;
				}
				setState(2703);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,349,_ctx) ) {
				case 1:
					{
					setState(2701);
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
					setState(2702);
					match(NULLS);
					}
					break;
				}
				setState(2707);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,350,_ctx) ) {
				case 1:
					{
					setState(2705);
					match(OVER);
					setState(2706);
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
				setState(2709);
				identifier();
				setState(2710);
				match(T__7);
				setState(2711);
				expression();
				}
				break;
			case 16:
				{
				_localctx = new LambdaContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2713);
				match(T__1);
				setState(2714);
				identifier();
				setState(2717); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2715);
					match(T__3);
					setState(2716);
					identifier();
					}
					}
					setState(2719); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__3 );
				setState(2721);
				match(T__2);
				setState(2722);
				match(T__7);
				setState(2723);
				expression();
				}
				break;
			case 17:
				{
				_localctx = new ColumnReferenceContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2725);
				identifier();
				}
				break;
			case 18:
				{
				_localctx = new ParenthesizedExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2726);
				match(T__1);
				setState(2727);
				expression();
				setState(2728);
				match(T__2);
				}
				break;
			case 19:
				{
				_localctx = new ExtractContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2730);
				match(EXTRACT);
				setState(2731);
				match(T__1);
				setState(2732);
				((ExtractContext)_localctx).field = identifier();
				setState(2733);
				match(FROM);
				setState(2734);
				((ExtractContext)_localctx).source = valueExpression(0);
				setState(2735);
				match(T__2);
				}
				break;
			case 20:
				{
				_localctx = new SubstringContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2737);
				_la = _input.LA(1);
				if ( !(_la==SUBSTR || _la==SUBSTRING) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2738);
				match(T__1);
				setState(2739);
				((SubstringContext)_localctx).str = valueExpression(0);
				setState(2740);
				_la = _input.LA(1);
				if ( !(_la==T__3 || _la==FROM) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2741);
				((SubstringContext)_localctx).pos = valueExpression(0);
				setState(2744);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__3 || _la==FOR) {
					{
					setState(2742);
					_la = _input.LA(1);
					if ( !(_la==T__3 || _la==FOR) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(2743);
					((SubstringContext)_localctx).len = valueExpression(0);
					}
				}

				setState(2746);
				match(T__2);
				}
				break;
			case 21:
				{
				_localctx = new TrimContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2748);
				match(TRIM);
				setState(2749);
				match(T__1);
				setState(2751);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,353,_ctx) ) {
				case 1:
					{
					setState(2750);
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
				setState(2754);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,354,_ctx) ) {
				case 1:
					{
					setState(2753);
					((TrimContext)_localctx).trimStr = valueExpression(0);
					}
					break;
				}
				setState(2756);
				match(FROM);
				setState(2757);
				((TrimContext)_localctx).srcStr = valueExpression(0);
				setState(2758);
				match(T__2);
				}
				break;
			case 22:
				{
				_localctx = new OverlayContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(2760);
				match(OVERLAY);
				setState(2761);
				match(T__1);
				setState(2762);
				((OverlayContext)_localctx).input = valueExpression(0);
				setState(2763);
				match(PLACING);
				setState(2764);
				((OverlayContext)_localctx).replace = valueExpression(0);
				setState(2765);
				match(FROM);
				setState(2766);
				((OverlayContext)_localctx).position = valueExpression(0);
				setState(2769);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FOR) {
					{
					setState(2767);
					match(FOR);
					setState(2768);
					((OverlayContext)_localctx).length = valueExpression(0);
					}
				}

				setState(2771);
				match(T__2);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(2785);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,358,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(2783);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,357,_ctx) ) {
					case 1:
						{
						_localctx = new SubscriptContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((SubscriptContext)_localctx).value = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(2775);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(2776);
						match(T__8);
						setState(2777);
						((SubscriptContext)_localctx).index = valueExpression(0);
						setState(2778);
						match(T__9);
						}
						break;
					case 2:
						{
						_localctx = new DereferenceContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((DereferenceContext)_localctx).base = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(2780);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(2781);
						match(T__4);
						setState(2782);
						((DereferenceContext)_localctx).fieldName = identifier();
						}
						break;
					}
					} 
				}
				setState(2787);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,358,_ctx);
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
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public NullLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNullLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNullLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNullLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ConstantContext {
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlCommandParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlCommandParser.STRING, i);
		}
		public StringLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TypeConstructorContext extends ConstantContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TypeConstructorContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTypeConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTypeConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTypeConstructor(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIntervalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIntervalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIntervalLiteral(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNumericLiteral(this);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_constant);
		try {
			int _alt;
			setState(2800);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,360,_ctx) ) {
			case 1:
				_localctx = new NullLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2788);
				match(NULL);
				}
				break;
			case 2:
				_localctx = new IntervalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2789);
				interval();
				}
				break;
			case 3:
				_localctx = new TypeConstructorContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2790);
				identifier();
				setState(2791);
				match(STRING);
				}
				break;
			case 4:
				_localctx = new NumericLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2793);
				number();
				}
				break;
			case 5:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(2794);
				booleanValue();
				}
				break;
			case 6:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(2796); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(2795);
						match(STRING);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(2798); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,359,_ctx);
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
		public TerminalNode EQ() { return getToken(ArcticSqlCommandParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(ArcticSqlCommandParser.NEQ, 0); }
		public TerminalNode NEQJ() { return getToken(ArcticSqlCommandParser.NEQJ, 0); }
		public TerminalNode LT() { return getToken(ArcticSqlCommandParser.LT, 0); }
		public TerminalNode LTE() { return getToken(ArcticSqlCommandParser.LTE, 0); }
		public TerminalNode GT() { return getToken(ArcticSqlCommandParser.GT, 0); }
		public TerminalNode GTE() { return getToken(ArcticSqlCommandParser.GTE, 0); }
		public TerminalNode NSEQ() { return getToken(ArcticSqlCommandParser.NSEQ, 0); }
		public ComparisonOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterComparisonOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitComparisonOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitComparisonOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOperatorContext comparisonOperator() throws RecognitionException {
		ComparisonOperatorContext _localctx = new ComparisonOperatorContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_comparisonOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2802);
			_la = _input.LA(1);
			if ( !(((((_la - 273)) & ~0x3f) == 0 && ((1L << (_la - 273)) & ((1L << (EQ - 273)) | (1L << (NSEQ - 273)) | (1L << (NEQ - 273)) | (1L << (NEQJ - 273)) | (1L << (LT - 273)) | (1L << (LTE - 273)) | (1L << (GT - 273)) | (1L << (GTE - 273)))) != 0)) ) {
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
		public TerminalNode PLUS() { return getToken(ArcticSqlCommandParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticSqlCommandParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticSqlCommandParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticSqlCommandParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlCommandParser.DIV, 0); }
		public TerminalNode TILDE() { return getToken(ArcticSqlCommandParser.TILDE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticSqlCommandParser.AMPERSAND, 0); }
		public TerminalNode PIPE() { return getToken(ArcticSqlCommandParser.PIPE, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticSqlCommandParser.CONCAT_PIPE, 0); }
		public TerminalNode HAT() { return getToken(ArcticSqlCommandParser.HAT, 0); }
		public ArithmeticOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterArithmeticOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitArithmeticOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitArithmeticOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticOperatorContext arithmeticOperator() throws RecognitionException {
		ArithmeticOperatorContext _localctx = new ArithmeticOperatorContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_arithmeticOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2804);
			_la = _input.LA(1);
			if ( !(_la==DIV || ((((_la - 281)) & ~0x3f) == 0 && ((1L << (_la - 281)) & ((1L << (PLUS - 281)) | (1L << (MINUS - 281)) | (1L << (ASTERISK - 281)) | (1L << (SLASH - 281)) | (1L << (PERCENT - 281)) | (1L << (TILDE - 281)) | (1L << (AMPERSAND - 281)) | (1L << (PIPE - 281)) | (1L << (CONCAT_PIPE - 281)) | (1L << (HAT - 281)))) != 0)) ) {
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
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public PredicateOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicateOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPredicateOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPredicateOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPredicateOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateOperatorContext predicateOperator() throws RecognitionException {
		PredicateOperatorContext _localctx = new PredicateOperatorContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_predicateOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2806);
			_la = _input.LA(1);
			if ( !(_la==AND || ((((_la - 117)) & ~0x3f) == 0 && ((1L << (_la - 117)) & ((1L << (IN - 117)) | (1L << (NOT - 117)) | (1L << (OR - 117)))) != 0)) ) {
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
		public TerminalNode TRUE() { return getToken(ArcticSqlCommandParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSqlCommandParser.FALSE, 0); }
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2808);
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
		public TerminalNode INTERVAL() { return getToken(ArcticSqlCommandParser.INTERVAL, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalContext interval() throws RecognitionException {
		IntervalContext _localctx = new IntervalContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_interval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2810);
			match(INTERVAL);
			setState(2813);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,361,_ctx) ) {
			case 1:
				{
				setState(2811);
				errorCapturingMultiUnitsInterval();
				}
				break;
			case 2:
				{
				setState(2812);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterErrorCapturingMultiUnitsInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitErrorCapturingMultiUnitsInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitErrorCapturingMultiUnitsInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingMultiUnitsIntervalContext errorCapturingMultiUnitsInterval() throws RecognitionException {
		ErrorCapturingMultiUnitsIntervalContext _localctx = new ErrorCapturingMultiUnitsIntervalContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_errorCapturingMultiUnitsInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2815);
			((ErrorCapturingMultiUnitsIntervalContext)_localctx).body = multiUnitsInterval();
			setState(2817);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,362,_ctx) ) {
			case 1:
				{
				setState(2816);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterMultiUnitsInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitMultiUnitsInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitMultiUnitsInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiUnitsIntervalContext multiUnitsInterval() throws RecognitionException {
		MultiUnitsIntervalContext _localctx = new MultiUnitsIntervalContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_multiUnitsInterval);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2822); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(2819);
					intervalValue();
					setState(2820);
					((MultiUnitsIntervalContext)_localctx).identifier = identifier();
					((MultiUnitsIntervalContext)_localctx).unit.add(((MultiUnitsIntervalContext)_localctx).identifier);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(2824); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,363,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterErrorCapturingUnitToUnitInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitErrorCapturingUnitToUnitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitErrorCapturingUnitToUnitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingUnitToUnitIntervalContext errorCapturingUnitToUnitInterval() throws RecognitionException {
		ErrorCapturingUnitToUnitIntervalContext _localctx = new ErrorCapturingUnitToUnitIntervalContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_errorCapturingUnitToUnitInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2826);
			((ErrorCapturingUnitToUnitIntervalContext)_localctx).body = unitToUnitInterval();
			setState(2829);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,364,_ctx) ) {
			case 1:
				{
				setState(2827);
				((ErrorCapturingUnitToUnitIntervalContext)_localctx).error1 = multiUnitsInterval();
				}
				break;
			case 2:
				{
				setState(2828);
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
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUnitToUnitInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUnitToUnitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUnitToUnitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitToUnitIntervalContext unitToUnitInterval() throws RecognitionException {
		UnitToUnitIntervalContext _localctx = new UnitToUnitIntervalContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_unitToUnitInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2831);
			((UnitToUnitIntervalContext)_localctx).value = intervalValue();
			setState(2832);
			((UnitToUnitIntervalContext)_localctx).from = identifier();
			setState(2833);
			match(TO);
			setState(2834);
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
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlCommandParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlCommandParser.DECIMAL_VALUE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlCommandParser.STRING, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSqlCommandParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public IntervalValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intervalValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIntervalValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIntervalValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIntervalValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalValueContext intervalValue() throws RecognitionException {
		IntervalValueContext _localctx = new IntervalValueContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_intervalValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2837);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PLUS || _la==MINUS) {
				{
				setState(2836);
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

			setState(2839);
			_la = _input.LA(1);
			if ( !(((((_la - 291)) & ~0x3f) == 0 && ((1L << (_la - 291)) & ((1L << (STRING - 291)) | (1L << (INTEGER_VALUE - 291)) | (1L << (DECIMAL_VALUE - 291)))) != 0)) ) {
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
		public TerminalNode FIRST() { return getToken(ArcticSqlCommandParser.FIRST, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSqlCommandParser.AFTER, 0); }
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public ColPositionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colPosition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterColPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitColPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitColPosition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColPositionContext colPosition() throws RecognitionException {
		ColPositionContext _localctx = new ColPositionContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_colPosition);
		try {
			setState(2844);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FIRST:
				enterOuterAlt(_localctx, 1);
				{
				setState(2841);
				((ColPositionContext)_localctx).position = match(FIRST);
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2842);
				((ColPositionContext)_localctx).position = match(AFTER);
				setState(2843);
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
		public List<DataTypeContext> dataType() {
			return getRuleContexts(DataTypeContext.class);
		}
		public DataTypeContext dataType(int i) {
			return getRuleContext(DataTypeContext.class,i);
		}
		public TerminalNode ARRAY() { return getToken(ArcticSqlCommandParser.ARRAY, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlCommandParser.MAP, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSqlCommandParser.STRUCT, 0); }
		public TerminalNode NEQ() { return getToken(ArcticSqlCommandParser.NEQ, 0); }
		public ComplexColTypeListContext complexColTypeList() {
			return getRuleContext(ComplexColTypeListContext.class,0);
		}
		public ComplexDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterComplexDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitComplexDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitComplexDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class YearMonthIntervalDataTypeContext extends DataTypeContext {
		public Token from;
		public Token to;
		public TerminalNode INTERVAL() { return getToken(ArcticSqlCommandParser.INTERVAL, 0); }
		public TerminalNode YEAR() { return getToken(ArcticSqlCommandParser.YEAR, 0); }
		public List<TerminalNode> MONTH() { return getTokens(ArcticSqlCommandParser.MONTH); }
		public TerminalNode MONTH(int i) {
			return getToken(ArcticSqlCommandParser.MONTH, i);
		}
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
		public YearMonthIntervalDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterYearMonthIntervalDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitYearMonthIntervalDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitYearMonthIntervalDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DayTimeIntervalDataTypeContext extends DataTypeContext {
		public Token from;
		public Token to;
		public TerminalNode INTERVAL() { return getToken(ArcticSqlCommandParser.INTERVAL, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlCommandParser.DAY, 0); }
		public List<TerminalNode> HOUR() { return getTokens(ArcticSqlCommandParser.HOUR); }
		public TerminalNode HOUR(int i) {
			return getToken(ArcticSqlCommandParser.HOUR, i);
		}
		public List<TerminalNode> MINUTE() { return getTokens(ArcticSqlCommandParser.MINUTE); }
		public TerminalNode MINUTE(int i) {
			return getToken(ArcticSqlCommandParser.MINUTE, i);
		}
		public List<TerminalNode> SECOND() { return getTokens(ArcticSqlCommandParser.SECOND); }
		public TerminalNode SECOND(int i) {
			return getToken(ArcticSqlCommandParser.SECOND, i);
		}
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
		public DayTimeIntervalDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDayTimeIntervalDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDayTimeIntervalDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDayTimeIntervalDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrimitiveDataTypeContext extends DataTypeContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticSqlCommandParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticSqlCommandParser.INTEGER_VALUE, i);
		}
		public PrimitiveDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterPrimitiveDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitPrimitiveDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitPrimitiveDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_dataType);
		int _la;
		try {
			setState(2892);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,373,_ctx) ) {
			case 1:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2846);
				((ComplexDataTypeContext)_localctx).complex = match(ARRAY);
				setState(2847);
				match(LT);
				setState(2848);
				dataType();
				setState(2849);
				match(GT);
				}
				break;
			case 2:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2851);
				((ComplexDataTypeContext)_localctx).complex = match(MAP);
				setState(2852);
				match(LT);
				setState(2853);
				dataType();
				setState(2854);
				match(T__3);
				setState(2855);
				dataType();
				setState(2856);
				match(GT);
				}
				break;
			case 3:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2858);
				((ComplexDataTypeContext)_localctx).complex = match(STRUCT);
				setState(2865);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LT:
					{
					setState(2859);
					match(LT);
					setState(2861);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,367,_ctx) ) {
					case 1:
						{
						setState(2860);
						complexColTypeList();
						}
						break;
					}
					setState(2863);
					match(GT);
					}
					break;
				case NEQ:
					{
					setState(2864);
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
				setState(2867);
				match(INTERVAL);
				setState(2868);
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
				setState(2871);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,369,_ctx) ) {
				case 1:
					{
					setState(2869);
					match(TO);
					setState(2870);
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
				setState(2873);
				match(INTERVAL);
				setState(2874);
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
				setState(2877);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,370,_ctx) ) {
				case 1:
					{
					setState(2875);
					match(TO);
					setState(2876);
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
				setState(2879);
				identifier();
				setState(2890);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,372,_ctx) ) {
				case 1:
					{
					setState(2880);
					match(T__1);
					setState(2881);
					match(INTEGER_VALUE);
					setState(2886);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(2882);
						match(T__3);
						setState(2883);
						match(INTEGER_VALUE);
						}
						}
						setState(2888);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(2889);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQualifiedColTypeWithPositionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQualifiedColTypeWithPositionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQualifiedColTypeWithPositionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedColTypeWithPositionListContext qualifiedColTypeWithPositionList() throws RecognitionException {
		QualifiedColTypeWithPositionListContext _localctx = new QualifiedColTypeWithPositionListContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_qualifiedColTypeWithPositionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2894);
			qualifiedColTypeWithPosition();
			setState(2899);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2895);
				match(T__3);
				setState(2896);
				qualifiedColTypeWithPosition();
				}
				}
				setState(2901);
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
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQualifiedColTypeWithPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQualifiedColTypeWithPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQualifiedColTypeWithPosition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedColTypeWithPositionContext qualifiedColTypeWithPosition() throws RecognitionException {
		QualifiedColTypeWithPositionContext _localctx = new QualifiedColTypeWithPositionContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_qualifiedColTypeWithPosition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2902);
			((QualifiedColTypeWithPositionContext)_localctx).name = multipartIdentifier();
			setState(2903);
			dataType();
			setState(2906);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(2904);
				match(NOT);
				setState(2905);
				match(NULL);
				}
			}

			setState(2909);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(2908);
				commentSpec();
				}
			}

			setState(2912);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AFTER || _la==FIRST) {
				{
				setState(2911);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeListContext colTypeList() throws RecognitionException {
		ColTypeListContext _localctx = new ColTypeListContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_colTypeList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2914);
			colType();
			setState(2919);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,378,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2915);
					match(T__3);
					setState(2916);
					colType();
					}
					} 
				}
				setState(2921);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,378,_ctx);
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
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeContext colType() throws RecognitionException {
		ColTypeContext _localctx = new ColTypeContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_colType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2922);
			((ColTypeContext)_localctx).colName = errorCapturingIdentifier();
			setState(2923);
			dataType();
			setState(2926);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,379,_ctx) ) {
			case 1:
				{
				setState(2924);
				match(NOT);
				setState(2925);
				match(NULL);
				}
				break;
			}
			setState(2929);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,380,_ctx) ) {
			case 1:
				{
				setState(2928);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterComplexColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitComplexColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitComplexColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexColTypeListContext complexColTypeList() throws RecognitionException {
		ComplexColTypeListContext _localctx = new ComplexColTypeListContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_complexColTypeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2931);
			complexColType();
			setState(2936);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(2932);
				match(T__3);
				setState(2933);
				complexColType();
				}
				}
				setState(2938);
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
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ComplexColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexColType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterComplexColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitComplexColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitComplexColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexColTypeContext complexColType() throws RecognitionException {
		ComplexColTypeContext _localctx = new ComplexColTypeContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_complexColType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2939);
			identifier();
			setState(2941);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,382,_ctx) ) {
			case 1:
				{
				setState(2940);
				match(T__10);
				}
				break;
			}
			setState(2943);
			dataType();
			setState(2946);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(2944);
				match(NOT);
				setState(2945);
				match(NULL);
				}
			}

			setState(2949);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(2948);
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
		public TerminalNode WHEN() { return getToken(ArcticSqlCommandParser.WHEN, 0); }
		public TerminalNode THEN() { return getToken(ArcticSqlCommandParser.THEN, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterWhenClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitWhenClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitWhenClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhenClauseContext whenClause() throws RecognitionException {
		WhenClauseContext _localctx = new WhenClauseContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_whenClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2951);
			match(WHEN);
			setState(2952);
			((WhenClauseContext)_localctx).condition = expression();
			setState(2953);
			match(THEN);
			setState(2954);
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
		public TerminalNode WINDOW() { return getToken(ArcticSqlCommandParser.WINDOW, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterWindowClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitWindowClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitWindowClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowClauseContext windowClause() throws RecognitionException {
		WindowClauseContext _localctx = new WindowClauseContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_windowClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2956);
			match(WINDOW);
			setState(2957);
			namedWindow();
			setState(2962);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,385,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2958);
					match(T__3);
					setState(2959);
					namedWindow();
					}
					} 
				}
				setState(2964);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,385,_ctx);
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
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNamedWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNamedWindow(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNamedWindow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedWindowContext namedWindow() throws RecognitionException {
		NamedWindowContext _localctx = new NamedWindowContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_namedWindow);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2965);
			((NamedWindowContext)_localctx).name = errorCapturingIdentifier();
			setState(2966);
			match(AS);
			setState(2967);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterWindowRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitWindowRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitWindowRef(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WindowDefContext extends WindowSpecContext {
		public ExpressionContext expression;
		public List<ExpressionContext> partition = new ArrayList<ExpressionContext>();
		public TerminalNode CLUSTER() { return getToken(ArcticSqlCommandParser.CLUSTER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSqlCommandParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlCommandParser.BY, i);
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
		public TerminalNode PARTITION() { return getToken(ArcticSqlCommandParser.PARTITION, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlCommandParser.DISTRIBUTE, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSqlCommandParser.ORDER, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlCommandParser.SORT, 0); }
		public WindowDefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterWindowDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitWindowDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitWindowDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowSpecContext windowSpec() throws RecognitionException {
		WindowSpecContext _localctx = new WindowSpecContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_windowSpec);
		int _la;
		try {
			setState(3015);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,393,_ctx) ) {
			case 1:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2969);
				((WindowRefContext)_localctx).name = errorCapturingIdentifier();
				}
				break;
			case 2:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2970);
				match(T__1);
				setState(2971);
				((WindowRefContext)_localctx).name = errorCapturingIdentifier();
				setState(2972);
				match(T__2);
				}
				break;
			case 3:
				_localctx = new WindowDefContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2974);
				match(T__1);
				setState(3009);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case CLUSTER:
					{
					setState(2975);
					match(CLUSTER);
					setState(2976);
					match(BY);
					setState(2977);
					((WindowDefContext)_localctx).expression = expression();
					((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
					setState(2982);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__3) {
						{
						{
						setState(2978);
						match(T__3);
						setState(2979);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						}
						}
						setState(2984);
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
					setState(2995);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==DISTRIBUTE || _la==PARTITION) {
						{
						setState(2985);
						_la = _input.LA(1);
						if ( !(_la==DISTRIBUTE || _la==PARTITION) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(2986);
						match(BY);
						setState(2987);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						setState(2992);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__3) {
							{
							{
							setState(2988);
							match(T__3);
							setState(2989);
							((WindowDefContext)_localctx).expression = expression();
							((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
							}
							}
							setState(2994);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(3007);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ORDER || _la==SORT) {
						{
						setState(2997);
						_la = _input.LA(1);
						if ( !(_la==ORDER || _la==SORT) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(2998);
						match(BY);
						setState(2999);
						sortItem();
						setState(3004);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__3) {
							{
							{
							setState(3000);
							match(T__3);
							setState(3001);
							sortItem();
							}
							}
							setState(3006);
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
				setState(3012);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==RANGE || _la==ROWS) {
					{
					setState(3011);
					windowFrame();
					}
				}

				setState(3014);
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
		public TerminalNode RANGE() { return getToken(ArcticSqlCommandParser.RANGE, 0); }
		public List<FrameBoundContext> frameBound() {
			return getRuleContexts(FrameBoundContext.class);
		}
		public FrameBoundContext frameBound(int i) {
			return getRuleContext(FrameBoundContext.class,i);
		}
		public TerminalNode ROWS() { return getToken(ArcticSqlCommandParser.ROWS, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlCommandParser.BETWEEN, 0); }
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public WindowFrameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowFrame; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterWindowFrame(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitWindowFrame(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitWindowFrame(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowFrameContext windowFrame() throws RecognitionException {
		WindowFrameContext _localctx = new WindowFrameContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_windowFrame);
		try {
			setState(3033);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,394,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3017);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(3018);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3019);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(3020);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(3021);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(3022);
				match(BETWEEN);
				setState(3023);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(3024);
				match(AND);
				setState(3025);
				((WindowFrameContext)_localctx).end = frameBound();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(3027);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(3028);
				match(BETWEEN);
				setState(3029);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(3030);
				match(AND);
				setState(3031);
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
		public TerminalNode UNBOUNDED() { return getToken(ArcticSqlCommandParser.UNBOUNDED, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSqlCommandParser.PRECEDING, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSqlCommandParser.FOLLOWING, 0); }
		public TerminalNode ROW() { return getToken(ArcticSqlCommandParser.ROW, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlCommandParser.CURRENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FrameBoundContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_frameBound; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFrameBound(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFrameBound(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFrameBound(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FrameBoundContext frameBound() throws RecognitionException {
		FrameBoundContext _localctx = new FrameBoundContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_frameBound);
		int _la;
		try {
			setState(3042);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,395,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3035);
				match(UNBOUNDED);
				setState(3036);
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
				setState(3037);
				((FrameBoundContext)_localctx).boundType = match(CURRENT);
				setState(3038);
				match(ROW);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(3039);
				expression();
				setState(3040);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQualifiedNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQualifiedNameList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQualifiedNameList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameListContext qualifiedNameList() throws RecognitionException {
		QualifiedNameListContext _localctx = new QualifiedNameListContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_qualifiedNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3044);
			qualifiedName();
			setState(3049);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(3045);
				match(T__3);
				setState(3046);
				qualifiedName();
				}
				}
				setState(3051);
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
		public TerminalNode FILTER() { return getToken(ArcticSqlCommandParser.FILTER, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSqlCommandParser.LEFT, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSqlCommandParser.RIGHT, 0); }
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFunctionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFunctionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_functionName);
		try {
			setState(3056);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,397,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3052);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3053);
				match(FILTER);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(3054);
				match(LEFT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(3055);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(3058);
			identifier();
			setState(3063);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,398,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(3059);
					match(T__4);
					setState(3060);
					identifier();
					}
					} 
				}
				setState(3065);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,398,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterErrorCapturingIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitErrorCapturingIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitErrorCapturingIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingIdentifierContext errorCapturingIdentifier() throws RecognitionException {
		ErrorCapturingIdentifierContext _localctx = new ErrorCapturingIdentifierContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_errorCapturingIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3066);
			identifier();
			setState(3067);
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
		public List<TerminalNode> MINUS() { return getTokens(ArcticSqlCommandParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(ArcticSqlCommandParser.MINUS, i);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterErrorIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitErrorIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitErrorIdent(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RealIdentContext extends ErrorCapturingIdentifierExtraContext {
		public RealIdentContext(ErrorCapturingIdentifierExtraContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterRealIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitRealIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitRealIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingIdentifierExtraContext errorCapturingIdentifierExtra() throws RecognitionException {
		ErrorCapturingIdentifierExtraContext _localctx = new ErrorCapturingIdentifierExtraContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_errorCapturingIdentifierExtra);
		try {
			int _alt;
			setState(3076);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,400,_ctx) ) {
			case 1:
				_localctx = new ErrorIdentContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(3071); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(3069);
						match(MINUS);
						setState(3070);
						identifier();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(3073); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,399,_ctx);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_identifier);
		try {
			setState(3081);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,401,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(3078);
				strictIdentifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(3079);
				if (!(!SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "!SQL_standard_keyword_behavior");
				setState(3080);
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
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQuotedIdentifierAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQuotedIdentifierAlternative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQuotedIdentifierAlternative(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnquotedIdentifierContext extends StrictIdentifierContext {
		public TerminalNode IDENTIFIER() { return getToken(ArcticSqlCommandParser.IDENTIFIER, 0); }
		public AnsiNonReservedContext ansiNonReserved() {
			return getRuleContext(AnsiNonReservedContext.class,0);
		}
		public NonReservedContext nonReserved() {
			return getRuleContext(NonReservedContext.class,0);
		}
		public UnquotedIdentifierContext(StrictIdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterUnquotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitUnquotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitUnquotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictIdentifierContext strictIdentifier() throws RecognitionException {
		StrictIdentifierContext _localctx = new StrictIdentifierContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_strictIdentifier);
		try {
			setState(3089);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,402,_ctx) ) {
			case 1:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(3083);
				match(IDENTIFIER);
				}
				break;
			case 2:
				_localctx = new QuotedIdentifierAlternativeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(3084);
				quotedIdentifier();
				}
				break;
			case 3:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(3085);
				if (!(SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "SQL_standard_keyword_behavior");
				setState(3086);
				ansiNonReserved();
				}
				break;
			case 4:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(3087);
				if (!(!SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "!SQL_standard_keyword_behavior");
				setState(3088);
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
		public TerminalNode BACKQUOTED_IDENTIFIER() { return getToken(ArcticSqlCommandParser.BACKQUOTED_IDENTIFIER, 0); }
		public QuotedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quotedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterQuotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitQuotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitQuotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuotedIdentifierContext quotedIdentifier() throws RecognitionException {
		QuotedIdentifierContext _localctx = new QuotedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_quotedIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3091);
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
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlCommandParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public DecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigIntLiteralContext extends NumberContext {
		public TerminalNode BIGINT_LITERAL() { return getToken(ArcticSqlCommandParser.BIGINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public BigIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterBigIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitBigIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitBigIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TinyIntLiteralContext extends NumberContext {
		public TerminalNode TINYINT_LITERAL() { return getToken(ArcticSqlCommandParser.TINYINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public TinyIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterTinyIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitTinyIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitTinyIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LegacyDecimalLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(ArcticSqlCommandParser.EXPONENT_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlCommandParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public LegacyDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterLegacyDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitLegacyDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitLegacyDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigDecimalLiteralContext extends NumberContext {
		public TerminalNode BIGDECIMAL_LITERAL() { return getToken(ArcticSqlCommandParser.BIGDECIMAL_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public BigDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterBigDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitBigDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitBigDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExponentLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(ArcticSqlCommandParser.EXPONENT_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public ExponentLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterExponentLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitExponentLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitExponentLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DoubleLiteralContext extends NumberContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(ArcticSqlCommandParser.DOUBLE_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public DoubleLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitDoubleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitDoubleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntegerLiteralContext extends NumberContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlCommandParser.INTEGER_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public IntegerLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitIntegerLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FloatLiteralContext extends NumberContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(ArcticSqlCommandParser.FLOAT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public FloatLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterFloatLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitFloatLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitFloatLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SmallIntLiteralContext extends NumberContext {
		public TerminalNode SMALLINT_LITERAL() { return getToken(ArcticSqlCommandParser.SMALLINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlCommandParser.MINUS, 0); }
		public SmallIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterSmallIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitSmallIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitSmallIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_number);
		int _la;
		try {
			setState(3136);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,413,_ctx) ) {
			case 1:
				_localctx = new ExponentLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(3093);
				if (!(!legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "!legacy_exponent_literal_as_decimal_enabled");
				setState(3095);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3094);
					match(MINUS);
					}
				}

				setState(3097);
				match(EXPONENT_VALUE);
				}
				break;
			case 2:
				_localctx = new DecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(3098);
				if (!(!legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "!legacy_exponent_literal_as_decimal_enabled");
				setState(3100);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3099);
					match(MINUS);
					}
				}

				setState(3102);
				match(DECIMAL_VALUE);
				}
				break;
			case 3:
				_localctx = new LegacyDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(3103);
				if (!(legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "legacy_exponent_literal_as_decimal_enabled");
				setState(3105);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3104);
					match(MINUS);
					}
				}

				setState(3107);
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
				setState(3109);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3108);
					match(MINUS);
					}
				}

				setState(3111);
				match(INTEGER_VALUE);
				}
				break;
			case 5:
				_localctx = new BigIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(3113);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3112);
					match(MINUS);
					}
				}

				setState(3115);
				match(BIGINT_LITERAL);
				}
				break;
			case 6:
				_localctx = new SmallIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(3117);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3116);
					match(MINUS);
					}
				}

				setState(3119);
				match(SMALLINT_LITERAL);
				}
				break;
			case 7:
				_localctx = new TinyIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(3121);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3120);
					match(MINUS);
					}
				}

				setState(3123);
				match(TINYINT_LITERAL);
				}
				break;
			case 8:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(3125);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3124);
					match(MINUS);
					}
				}

				setState(3127);
				match(DOUBLE_LITERAL);
				}
				break;
			case 9:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(3129);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3128);
					match(MINUS);
					}
				}

				setState(3131);
				match(FLOAT_LITERAL);
				}
				break;
			case 10:
				_localctx = new BigDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(3133);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(3132);
					match(MINUS);
					}
				}

				setState(3135);
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
		public TerminalNode TYPE() { return getToken(ArcticSqlCommandParser.TYPE, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ColPositionContext colPosition() {
			return getRuleContext(ColPositionContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public AlterColumnActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterColumnAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAlterColumnAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAlterColumnAction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAlterColumnAction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AlterColumnActionContext alterColumnAction() throws RecognitionException {
		AlterColumnActionContext _localctx = new AlterColumnActionContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_alterColumnAction);
		int _la;
		try {
			setState(3145);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TYPE:
				enterOuterAlt(_localctx, 1);
				{
				setState(3138);
				match(TYPE);
				setState(3139);
				dataType();
				}
				break;
			case COMMENT:
				enterOuterAlt(_localctx, 2);
				{
				setState(3140);
				commentSpec();
				}
				break;
			case AFTER:
			case FIRST:
				enterOuterAlt(_localctx, 3);
				{
				setState(3141);
				colPosition();
				}
				break;
			case DROP:
			case SET:
				enterOuterAlt(_localctx, 4);
				{
				setState(3142);
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
				setState(3143);
				match(NOT);
				setState(3144);
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
		public TerminalNode ADD() { return getToken(ArcticSqlCommandParser.ADD, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSqlCommandParser.AFTER, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticSqlCommandParser.ANALYZE, 0); }
		public TerminalNode ANTI() { return getToken(ArcticSqlCommandParser.ANTI, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSqlCommandParser.ARCHIVE, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSqlCommandParser.ARRAY, 0); }
		public TerminalNode ASC() { return getToken(ArcticSqlCommandParser.ASC, 0); }
		public TerminalNode AT() { return getToken(ArcticSqlCommandParser.AT, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlCommandParser.BETWEEN, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSqlCommandParser.BUCKET, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSqlCommandParser.BUCKETS, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlCommandParser.BY, 0); }
		public TerminalNode CACHE() { return getToken(ArcticSqlCommandParser.CACHE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticSqlCommandParser.CASCADE, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticSqlCommandParser.CHANGE, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticSqlCommandParser.CLEAR, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticSqlCommandParser.CLUSTER, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlCommandParser.CLUSTERED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSqlCommandParser.CODEGEN, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticSqlCommandParser.COLLECTION, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticSqlCommandParser.COMMENT, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSqlCommandParser.COMMIT, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSqlCommandParser.COMPACT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSqlCommandParser.COMPACTIONS, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticSqlCommandParser.COMPUTE, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticSqlCommandParser.CONCATENATE, 0); }
		public TerminalNode COST() { return getToken(ArcticSqlCommandParser.COST, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlCommandParser.CUBE, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlCommandParser.CURRENT, 0); }
		public TerminalNode DATA() { return getToken(ArcticSqlCommandParser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSqlCommandParser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticSqlCommandParser.DATABASES, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlCommandParser.DAY, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticSqlCommandParser.DBPROPERTIES, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSqlCommandParser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(ArcticSqlCommandParser.DELETE, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSqlCommandParser.DELIMITED, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlCommandParser.DESCRIBE, 0); }
		public TerminalNode DFS() { return getToken(ArcticSqlCommandParser.DFS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlCommandParser.DIRECTORIES, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSqlCommandParser.DIRECTORY, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlCommandParser.DISTRIBUTE, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlCommandParser.DIV, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticSqlCommandParser.ESCAPED, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSqlCommandParser.EXCHANGE, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticSqlCommandParser.EXPLAIN, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSqlCommandParser.EXPORT, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSqlCommandParser.EXTERNAL, 0); }
		public TerminalNode EXTRACT() { return getToken(ArcticSqlCommandParser.EXTRACT, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSqlCommandParser.FIELDS, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSqlCommandParser.FILEFORMAT, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSqlCommandParser.FIRST, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSqlCommandParser.FOLLOWING, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlCommandParser.FORMAT, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlCommandParser.FORMATTED, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlCommandParser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticSqlCommandParser.FUNCTIONS, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticSqlCommandParser.GLOBAL, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlCommandParser.GROUPING, 0); }
		public TerminalNode HOUR() { return getToken(ArcticSqlCommandParser.HOUR, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSqlCommandParser.IGNORE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSqlCommandParser.IMPORT, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSqlCommandParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSqlCommandParser.INDEXES, 0); }
		public TerminalNode INPATH() { return getToken(ArcticSqlCommandParser.INPATH, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSqlCommandParser.INPUTFORMAT, 0); }
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticSqlCommandParser.INTERVAL, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSqlCommandParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSqlCommandParser.KEYS, 0); }
		public TerminalNode LAST() { return getToken(ArcticSqlCommandParser.LAST, 0); }
		public TerminalNode LAZY() { return getToken(ArcticSqlCommandParser.LAZY, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticSqlCommandParser.LIMIT, 0); }
		public TerminalNode LINES() { return getToken(ArcticSqlCommandParser.LINES, 0); }
		public TerminalNode LIST() { return getToken(ArcticSqlCommandParser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(ArcticSqlCommandParser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlCommandParser.LOCAL, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSqlCommandParser.LOCATION, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSqlCommandParser.LOCK, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSqlCommandParser.LOCKS, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticSqlCommandParser.LOGICAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSqlCommandParser.MACRO, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlCommandParser.MAP, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticSqlCommandParser.MATCHED, 0); }
		public TerminalNode MERGE() { return getToken(ArcticSqlCommandParser.MERGE, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticSqlCommandParser.MINUTE, 0); }
		public TerminalNode MONTH() { return getToken(ArcticSqlCommandParser.MONTH, 0); }
		public TerminalNode MSCK() { return getToken(ArcticSqlCommandParser.MSCK, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlCommandParser.NAMESPACE, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticSqlCommandParser.NAMESPACES, 0); }
		public TerminalNode NO() { return getToken(ArcticSqlCommandParser.NO, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlCommandParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlCommandParser.OF, 0); }
		public TerminalNode OPTION() { return getToken(ArcticSqlCommandParser.OPTION, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSqlCommandParser.OPTIONS, 0); }
		public TerminalNode OUT() { return getToken(ArcticSqlCommandParser.OUT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSqlCommandParser.OUTPUTFORMAT, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlCommandParser.OVER, 0); }
		public TerminalNode OVERLAY() { return getToken(ArcticSqlCommandParser.OVERLAY, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlCommandParser.OVERWRITE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSqlCommandParser.PARTITION, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticSqlCommandParser.PARTITIONED, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlCommandParser.PARTITIONS, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticSqlCommandParser.PERCENTLIT, 0); }
		public TerminalNode PIVOT() { return getToken(ArcticSqlCommandParser.PIVOT, 0); }
		public TerminalNode PLACING() { return getToken(ArcticSqlCommandParser.PLACING, 0); }
		public TerminalNode POSITION() { return getToken(ArcticSqlCommandParser.POSITION, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSqlCommandParser.PRECEDING, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSqlCommandParser.PRINCIPALS, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticSqlCommandParser.PROPERTIES, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSqlCommandParser.PURGE, 0); }
		public TerminalNode QUERY() { return getToken(ArcticSqlCommandParser.QUERY, 0); }
		public TerminalNode RANGE() { return getToken(ArcticSqlCommandParser.RANGE, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSqlCommandParser.RECORDREADER, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSqlCommandParser.RECORDWRITER, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticSqlCommandParser.RECOVER, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSqlCommandParser.REDUCE, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticSqlCommandParser.REFRESH, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSqlCommandParser.RENAME, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticSqlCommandParser.REPAIR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode RESET() { return getToken(ArcticSqlCommandParser.RESET, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticSqlCommandParser.RESPECT, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticSqlCommandParser.RESTRICT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSqlCommandParser.REVOKE, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticSqlCommandParser.RLIKE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSqlCommandParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSqlCommandParser.ROLES, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSqlCommandParser.ROLLBACK, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSqlCommandParser.ROLLUP, 0); }
		public TerminalNode ROW() { return getToken(ArcticSqlCommandParser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(ArcticSqlCommandParser.ROWS, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticSqlCommandParser.SCHEMA, 0); }
		public TerminalNode SECOND() { return getToken(ArcticSqlCommandParser.SECOND, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSqlCommandParser.SEMI, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticSqlCommandParser.SEPARATED, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlCommandParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlCommandParser.SERDEPROPERTIES, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSqlCommandParser.SETMINUS, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlCommandParser.SETS, 0); }
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSqlCommandParser.SKEWED, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlCommandParser.SORT, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlCommandParser.SORTED, 0); }
		public TerminalNode START() { return getToken(ArcticSqlCommandParser.START, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSqlCommandParser.STATISTICS, 0); }
		public TerminalNode STORED() { return getToken(ArcticSqlCommandParser.STORED, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticSqlCommandParser.STRATIFY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSqlCommandParser.STRUCT, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticSqlCommandParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticSqlCommandParser.SUBSTRING, 0); }
		public TerminalNode SYNC() { return getToken(ArcticSqlCommandParser.SYNC, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSqlCommandParser.TABLES, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSqlCommandParser.TABLESAMPLE, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlCommandParser.TBLPROPERTIES, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticSqlCommandParser.TERMINATED, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSqlCommandParser.TOUCH, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSqlCommandParser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSqlCommandParser.TRANSACTIONS, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticSqlCommandParser.TRANSFORM, 0); }
		public TerminalNode TRIM() { return getToken(ArcticSqlCommandParser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSqlCommandParser.TRUE, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticSqlCommandParser.TRUNCATE, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticSqlCommandParser.TRY_CAST, 0); }
		public TerminalNode TYPE() { return getToken(ArcticSqlCommandParser.TYPE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSqlCommandParser.UNARCHIVE, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticSqlCommandParser.UNBOUNDED, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticSqlCommandParser.UNCACHE, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSqlCommandParser.UNLOCK, 0); }
		public TerminalNode UNSET() { return getToken(ArcticSqlCommandParser.UNSET, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticSqlCommandParser.UPDATE, 0); }
		public TerminalNode USE() { return getToken(ArcticSqlCommandParser.USE, 0); }
		public TerminalNode VALUES() { return getToken(ArcticSqlCommandParser.VALUES, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticSqlCommandParser.VIEWS, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticSqlCommandParser.WINDOW, 0); }
		public TerminalNode YEAR() { return getToken(ArcticSqlCommandParser.YEAR, 0); }
		public TerminalNode ZONE() { return getToken(ArcticSqlCommandParser.ZONE, 0); }
		public AnsiNonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ansiNonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterAnsiNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitAnsiNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitAnsiNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnsiNonReservedContext ansiNonReserved() throws RecognitionException {
		AnsiNonReservedContext _localctx = new AnsiNonReservedContext(_ctx, getState());
		enterRule(_localctx, 284, RULE_ansiNonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3147);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ADD) | (1L << AFTER) | (1L << ALTER) | (1L << ANALYZE) | (1L << ANTI) | (1L << ARCHIVE) | (1L << ARRAY) | (1L << ASC) | (1L << AT) | (1L << BETWEEN) | (1L << BUCKET) | (1L << BUCKETS) | (1L << BY) | (1L << CACHE) | (1L << CASCADE) | (1L << CHANGE) | (1L << CLEAR) | (1L << CLUSTER) | (1L << CLUSTERED) | (1L << CODEGEN) | (1L << COLLECTION) | (1L << COLUMNS) | (1L << COMMENT) | (1L << COMMIT) | (1L << COMPACT) | (1L << COMPACTIONS) | (1L << COMPUTE) | (1L << CONCATENATE) | (1L << COST) | (1L << CUBE) | (1L << CURRENT))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (DAY - 64)) | (1L << (DATA - 64)) | (1L << (DATABASE - 64)) | (1L << (DATABASES - 64)) | (1L << (DBPROPERTIES - 64)) | (1L << (DEFINED - 64)) | (1L << (DELETE - 64)) | (1L << (DELIMITED - 64)) | (1L << (DESC - 64)) | (1L << (DESCRIBE - 64)) | (1L << (DFS - 64)) | (1L << (DIRECTORIES - 64)) | (1L << (DIRECTORY - 64)) | (1L << (DISTRIBUTE - 64)) | (1L << (DIV - 64)) | (1L << (DROP - 64)) | (1L << (ESCAPED - 64)) | (1L << (EXCHANGE - 64)) | (1L << (EXISTS - 64)) | (1L << (EXPLAIN - 64)) | (1L << (EXPORT - 64)) | (1L << (EXTENDED - 64)) | (1L << (EXTERNAL - 64)) | (1L << (EXTRACT - 64)) | (1L << (FIELDS - 64)) | (1L << (FILEFORMAT - 64)) | (1L << (FIRST - 64)) | (1L << (FOLLOWING - 64)) | (1L << (FORMAT - 64)) | (1L << (FORMATTED - 64)) | (1L << (FUNCTION - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (GLOBAL - 64)) | (1L << (GROUPING - 64)) | (1L << (HOUR - 64)) | (1L << (IF - 64)) | (1L << (IGNORE - 64)) | (1L << (IMPORT - 64)) | (1L << (INDEX - 64)) | (1L << (INDEXES - 64)) | (1L << (INPATH - 64)) | (1L << (INPUTFORMAT - 64)) | (1L << (INSERT - 64)) | (1L << (INTERVAL - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (ITEMS - 128)) | (1L << (KEYS - 128)) | (1L << (LAST - 128)) | (1L << (LAZY - 128)) | (1L << (LIKE - 128)) | (1L << (LIMIT - 128)) | (1L << (LINES - 128)) | (1L << (LIST - 128)) | (1L << (LOAD - 128)) | (1L << (LOCAL - 128)) | (1L << (LOCATION - 128)) | (1L << (LOCK - 128)) | (1L << (LOCKS - 128)) | (1L << (LOGICAL - 128)) | (1L << (MACRO - 128)) | (1L << (MAP - 128)) | (1L << (MATCHED - 128)) | (1L << (MERGE - 128)) | (1L << (MINUTE - 128)) | (1L << (MONTH - 128)) | (1L << (MSCK - 128)) | (1L << (NAMESPACE - 128)) | (1L << (NAMESPACES - 128)) | (1L << (NO - 128)) | (1L << (NULLS - 128)) | (1L << (OF - 128)) | (1L << (OPTION - 128)) | (1L << (OPTIONS - 128)) | (1L << (OUT - 128)) | (1L << (OUTPUTFORMAT - 128)) | (1L << (OVER - 128)) | (1L << (OVERLAY - 128)) | (1L << (OVERWRITE - 128)) | (1L << (PARTITION - 128)) | (1L << (PARTITIONED - 128)) | (1L << (PARTITIONS - 128)) | (1L << (PERCENTLIT - 128)) | (1L << (PIVOT - 128)) | (1L << (PLACING - 128)) | (1L << (POSITION - 128)) | (1L << (PRECEDING - 128)) | (1L << (PRINCIPALS - 128)) | (1L << (PROPERTIES - 128)) | (1L << (PURGE - 128)) | (1L << (QUERY - 128)) | (1L << (RANGE - 128)) | (1L << (RECORDREADER - 128)) | (1L << (RECORDWRITER - 128)) | (1L << (RECOVER - 128)) | (1L << (REDUCE - 128)))) != 0) || ((((_la - 193)) & ~0x3f) == 0 && ((1L << (_la - 193)) & ((1L << (REFRESH - 193)) | (1L << (RENAME - 193)) | (1L << (REPAIR - 193)) | (1L << (REPLACE - 193)) | (1L << (RESET - 193)) | (1L << (RESPECT - 193)) | (1L << (RESTRICT - 193)) | (1L << (REVOKE - 193)) | (1L << (RLIKE - 193)) | (1L << (ROLE - 193)) | (1L << (ROLES - 193)) | (1L << (ROLLBACK - 193)) | (1L << (ROLLUP - 193)) | (1L << (ROW - 193)) | (1L << (ROWS - 193)) | (1L << (SECOND - 193)) | (1L << (SCHEMA - 193)) | (1L << (SEMI - 193)) | (1L << (SEPARATED - 193)) | (1L << (SERDE - 193)) | (1L << (SERDEPROPERTIES - 193)) | (1L << (SET - 193)) | (1L << (SETMINUS - 193)) | (1L << (SETS - 193)) | (1L << (SHOW - 193)) | (1L << (SKEWED - 193)) | (1L << (SORT - 193)) | (1L << (SORTED - 193)) | (1L << (START - 193)) | (1L << (STATISTICS - 193)) | (1L << (STORED - 193)) | (1L << (STRATIFY - 193)) | (1L << (STRUCT - 193)) | (1L << (SUBSTR - 193)) | (1L << (SUBSTRING - 193)) | (1L << (SYNC - 193)) | (1L << (TABLES - 193)) | (1L << (TABLESAMPLE - 193)) | (1L << (TBLPROPERTIES - 193)) | (1L << (TEMPORARY - 193)) | (1L << (TERMINATED - 193)) | (1L << (TOUCH - 193)) | (1L << (TRANSACTION - 193)) | (1L << (TRANSACTIONS - 193)) | (1L << (TRANSFORM - 193)) | (1L << (TRIM - 193)) | (1L << (TRUE - 193)) | (1L << (TRUNCATE - 193)) | (1L << (TRY_CAST - 193)) | (1L << (TYPE - 193)) | (1L << (UNARCHIVE - 193)) | (1L << (UNBOUNDED - 193)) | (1L << (UNCACHE - 193)))) != 0) || ((((_la - 258)) & ~0x3f) == 0 && ((1L << (_la - 258)) & ((1L << (UNLOCK - 258)) | (1L << (UNSET - 258)) | (1L << (UPDATE - 258)) | (1L << (USE - 258)) | (1L << (VALUES - 258)) | (1L << (VIEW - 258)) | (1L << (VIEWS - 258)) | (1L << (WINDOW - 258)) | (1L << (YEAR - 258)) | (1L << (ZONE - 258)))) != 0)) ) {
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
		public TerminalNode ANTI() { return getToken(ArcticSqlCommandParser.ANTI, 0); }
		public TerminalNode CROSS() { return getToken(ArcticSqlCommandParser.CROSS, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticSqlCommandParser.EXCEPT, 0); }
		public TerminalNode FULL() { return getToken(ArcticSqlCommandParser.FULL, 0); }
		public TerminalNode INNER() { return getToken(ArcticSqlCommandParser.INNER, 0); }
		public TerminalNode INTERSECT() { return getToken(ArcticSqlCommandParser.INTERSECT, 0); }
		public TerminalNode JOIN() { return getToken(ArcticSqlCommandParser.JOIN, 0); }
		public TerminalNode LATERAL() { return getToken(ArcticSqlCommandParser.LATERAL, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSqlCommandParser.LEFT, 0); }
		public TerminalNode NATURAL() { return getToken(ArcticSqlCommandParser.NATURAL, 0); }
		public TerminalNode ON() { return getToken(ArcticSqlCommandParser.ON, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSqlCommandParser.RIGHT, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSqlCommandParser.SEMI, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSqlCommandParser.SETMINUS, 0); }
		public TerminalNode UNION() { return getToken(ArcticSqlCommandParser.UNION, 0); }
		public TerminalNode USING() { return getToken(ArcticSqlCommandParser.USING, 0); }
		public StrictNonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_strictNonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterStrictNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitStrictNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitStrictNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictNonReservedContext strictNonReserved() throws RecognitionException {
		StrictNonReservedContext _localctx = new StrictNonReservedContext(_ctx, getState());
		enterRule(_localctx, 286, RULE_strictNonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3149);
			_la = _input.LA(1);
			if ( !(_la==ANTI || _la==CROSS || ((((_la - 85)) & ~0x3f) == 0 && ((1L << (_la - 85)) & ((1L << (EXCEPT - 85)) | (1L << (FULL - 85)) | (1L << (INNER - 85)) | (1L << (INTERSECT - 85)) | (1L << (JOIN - 85)) | (1L << (LATERAL - 85)) | (1L << (LEFT - 85)))) != 0) || ((((_la - 155)) & ~0x3f) == 0 && ((1L << (_la - 155)) & ((1L << (NATURAL - 155)) | (1L << (ON - 155)) | (1L << (RIGHT - 155)) | (1L << (SEMI - 155)) | (1L << (SETMINUS - 155)))) != 0) || _la==UNION || _la==USING) ) {
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
		public TerminalNode ADD() { return getToken(ArcticSqlCommandParser.ADD, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSqlCommandParser.AFTER, 0); }
		public TerminalNode ALL() { return getToken(ArcticSqlCommandParser.ALL, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSqlCommandParser.ALTER, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticSqlCommandParser.ANALYZE, 0); }
		public TerminalNode AND() { return getToken(ArcticSqlCommandParser.AND, 0); }
		public TerminalNode ANY() { return getToken(ArcticSqlCommandParser.ANY, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSqlCommandParser.ARCHIVE, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSqlCommandParser.ARRAY, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlCommandParser.AS, 0); }
		public TerminalNode ASC() { return getToken(ArcticSqlCommandParser.ASC, 0); }
		public TerminalNode AT() { return getToken(ArcticSqlCommandParser.AT, 0); }
		public TerminalNode AUTHORIZATION() { return getToken(ArcticSqlCommandParser.AUTHORIZATION, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlCommandParser.BETWEEN, 0); }
		public TerminalNode BOTH() { return getToken(ArcticSqlCommandParser.BOTH, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSqlCommandParser.BUCKET, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSqlCommandParser.BUCKETS, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlCommandParser.BY, 0); }
		public TerminalNode CACHE() { return getToken(ArcticSqlCommandParser.CACHE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticSqlCommandParser.CASCADE, 0); }
		public TerminalNode CASE() { return getToken(ArcticSqlCommandParser.CASE, 0); }
		public TerminalNode CAST() { return getToken(ArcticSqlCommandParser.CAST, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticSqlCommandParser.CHANGE, 0); }
		public TerminalNode CHECK() { return getToken(ArcticSqlCommandParser.CHECK, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticSqlCommandParser.CLEAR, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticSqlCommandParser.CLUSTER, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlCommandParser.CLUSTERED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSqlCommandParser.CODEGEN, 0); }
		public TerminalNode COLLATE() { return getToken(ArcticSqlCommandParser.COLLATE, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticSqlCommandParser.COLLECTION, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSqlCommandParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlCommandParser.COLUMNS, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticSqlCommandParser.COMMENT, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSqlCommandParser.COMMIT, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSqlCommandParser.COMPACT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSqlCommandParser.COMPACTIONS, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticSqlCommandParser.COMPUTE, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticSqlCommandParser.CONCATENATE, 0); }
		public TerminalNode CONSTRAINT() { return getToken(ArcticSqlCommandParser.CONSTRAINT, 0); }
		public TerminalNode COST() { return getToken(ArcticSqlCommandParser.COST, 0); }
		public TerminalNode CREATE() { return getToken(ArcticSqlCommandParser.CREATE, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlCommandParser.CUBE, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlCommandParser.CURRENT, 0); }
		public TerminalNode CURRENT_DATE() { return getToken(ArcticSqlCommandParser.CURRENT_DATE, 0); }
		public TerminalNode CURRENT_TIME() { return getToken(ArcticSqlCommandParser.CURRENT_TIME, 0); }
		public TerminalNode CURRENT_TIMESTAMP() { return getToken(ArcticSqlCommandParser.CURRENT_TIMESTAMP, 0); }
		public TerminalNode CURRENT_USER() { return getToken(ArcticSqlCommandParser.CURRENT_USER, 0); }
		public TerminalNode DATA() { return getToken(ArcticSqlCommandParser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSqlCommandParser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticSqlCommandParser.DATABASES, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlCommandParser.DAY, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticSqlCommandParser.DBPROPERTIES, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSqlCommandParser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(ArcticSqlCommandParser.DELETE, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSqlCommandParser.DELIMITED, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlCommandParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlCommandParser.DESCRIBE, 0); }
		public TerminalNode DFS() { return getToken(ArcticSqlCommandParser.DFS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlCommandParser.DIRECTORIES, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSqlCommandParser.DIRECTORY, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticSqlCommandParser.DISTINCT, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlCommandParser.DISTRIBUTE, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlCommandParser.DIV, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlCommandParser.DROP, 0); }
		public TerminalNode ELSE() { return getToken(ArcticSqlCommandParser.ELSE, 0); }
		public TerminalNode END() { return getToken(ArcticSqlCommandParser.END, 0); }
		public TerminalNode ESCAPE() { return getToken(ArcticSqlCommandParser.ESCAPE, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticSqlCommandParser.ESCAPED, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSqlCommandParser.EXCHANGE, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlCommandParser.EXISTS, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticSqlCommandParser.EXPLAIN, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSqlCommandParser.EXPORT, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlCommandParser.EXTENDED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSqlCommandParser.EXTERNAL, 0); }
		public TerminalNode EXTRACT() { return getToken(ArcticSqlCommandParser.EXTRACT, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSqlCommandParser.FALSE, 0); }
		public TerminalNode FETCH() { return getToken(ArcticSqlCommandParser.FETCH, 0); }
		public TerminalNode FILTER() { return getToken(ArcticSqlCommandParser.FILTER, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSqlCommandParser.FIELDS, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSqlCommandParser.FILEFORMAT, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSqlCommandParser.FIRST, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSqlCommandParser.FOLLOWING, 0); }
		public TerminalNode FOR() { return getToken(ArcticSqlCommandParser.FOR, 0); }
		public TerminalNode FOREIGN() { return getToken(ArcticSqlCommandParser.FOREIGN, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlCommandParser.FORMAT, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlCommandParser.FORMATTED, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlCommandParser.FROM, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlCommandParser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticSqlCommandParser.FUNCTIONS, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticSqlCommandParser.GLOBAL, 0); }
		public TerminalNode GRANT() { return getToken(ArcticSqlCommandParser.GRANT, 0); }
		public TerminalNode GROUP() { return getToken(ArcticSqlCommandParser.GROUP, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlCommandParser.GROUPING, 0); }
		public TerminalNode HAVING() { return getToken(ArcticSqlCommandParser.HAVING, 0); }
		public TerminalNode HOUR() { return getToken(ArcticSqlCommandParser.HOUR, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlCommandParser.IF, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSqlCommandParser.IGNORE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSqlCommandParser.IMPORT, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlCommandParser.IN, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSqlCommandParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSqlCommandParser.INDEXES, 0); }
		public TerminalNode INPATH() { return getToken(ArcticSqlCommandParser.INPATH, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSqlCommandParser.INPUTFORMAT, 0); }
		public TerminalNode INSERT() { return getToken(ArcticSqlCommandParser.INSERT, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticSqlCommandParser.INTERVAL, 0); }
		public TerminalNode INTO() { return getToken(ArcticSqlCommandParser.INTO, 0); }
		public TerminalNode IS() { return getToken(ArcticSqlCommandParser.IS, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSqlCommandParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSqlCommandParser.KEYS, 0); }
		public TerminalNode LAST() { return getToken(ArcticSqlCommandParser.LAST, 0); }
		public TerminalNode LAZY() { return getToken(ArcticSqlCommandParser.LAZY, 0); }
		public TerminalNode LEADING() { return getToken(ArcticSqlCommandParser.LEADING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlCommandParser.LIKE, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticSqlCommandParser.LIMIT, 0); }
		public TerminalNode LINES() { return getToken(ArcticSqlCommandParser.LINES, 0); }
		public TerminalNode LIST() { return getToken(ArcticSqlCommandParser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(ArcticSqlCommandParser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlCommandParser.LOCAL, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSqlCommandParser.LOCATION, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSqlCommandParser.LOCK, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSqlCommandParser.LOCKS, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticSqlCommandParser.LOGICAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSqlCommandParser.MACRO, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlCommandParser.MAP, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticSqlCommandParser.MATCHED, 0); }
		public TerminalNode MERGE() { return getToken(ArcticSqlCommandParser.MERGE, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticSqlCommandParser.MINUTE, 0); }
		public TerminalNode MONTH() { return getToken(ArcticSqlCommandParser.MONTH, 0); }
		public TerminalNode MSCK() { return getToken(ArcticSqlCommandParser.MSCK, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlCommandParser.NAMESPACE, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticSqlCommandParser.NAMESPACES, 0); }
		public TerminalNode NO() { return getToken(ArcticSqlCommandParser.NO, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlCommandParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlCommandParser.NULL, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlCommandParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlCommandParser.OF, 0); }
		public TerminalNode ONLY() { return getToken(ArcticSqlCommandParser.ONLY, 0); }
		public TerminalNode OPTION() { return getToken(ArcticSqlCommandParser.OPTION, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSqlCommandParser.OPTIONS, 0); }
		public TerminalNode OR() { return getToken(ArcticSqlCommandParser.OR, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSqlCommandParser.ORDER, 0); }
		public TerminalNode OUT() { return getToken(ArcticSqlCommandParser.OUT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticSqlCommandParser.OUTER, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSqlCommandParser.OUTPUTFORMAT, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlCommandParser.OVER, 0); }
		public TerminalNode OVERLAPS() { return getToken(ArcticSqlCommandParser.OVERLAPS, 0); }
		public TerminalNode OVERLAY() { return getToken(ArcticSqlCommandParser.OVERLAY, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlCommandParser.OVERWRITE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSqlCommandParser.PARTITION, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticSqlCommandParser.PARTITIONED, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlCommandParser.PARTITIONS, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticSqlCommandParser.PERCENTLIT, 0); }
		public TerminalNode PIVOT() { return getToken(ArcticSqlCommandParser.PIVOT, 0); }
		public TerminalNode PLACING() { return getToken(ArcticSqlCommandParser.PLACING, 0); }
		public TerminalNode POSITION() { return getToken(ArcticSqlCommandParser.POSITION, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSqlCommandParser.PRECEDING, 0); }
		public TerminalNode PRIMARY() { return getToken(ArcticSqlCommandParser.PRIMARY, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSqlCommandParser.PRINCIPALS, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticSqlCommandParser.PROPERTIES, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSqlCommandParser.PURGE, 0); }
		public TerminalNode QUERY() { return getToken(ArcticSqlCommandParser.QUERY, 0); }
		public TerminalNode RANGE() { return getToken(ArcticSqlCommandParser.RANGE, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSqlCommandParser.RECORDREADER, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSqlCommandParser.RECORDWRITER, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticSqlCommandParser.RECOVER, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSqlCommandParser.REDUCE, 0); }
		public TerminalNode REFERENCES() { return getToken(ArcticSqlCommandParser.REFERENCES, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticSqlCommandParser.REFRESH, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSqlCommandParser.RENAME, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticSqlCommandParser.REPAIR, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlCommandParser.REPLACE, 0); }
		public TerminalNode RESET() { return getToken(ArcticSqlCommandParser.RESET, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticSqlCommandParser.RESPECT, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticSqlCommandParser.RESTRICT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSqlCommandParser.REVOKE, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticSqlCommandParser.RLIKE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSqlCommandParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSqlCommandParser.ROLES, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSqlCommandParser.ROLLBACK, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSqlCommandParser.ROLLUP, 0); }
		public TerminalNode ROW() { return getToken(ArcticSqlCommandParser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(ArcticSqlCommandParser.ROWS, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticSqlCommandParser.SCHEMA, 0); }
		public TerminalNode SECOND() { return getToken(ArcticSqlCommandParser.SECOND, 0); }
		public TerminalNode SELECT() { return getToken(ArcticSqlCommandParser.SELECT, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticSqlCommandParser.SEPARATED, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlCommandParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlCommandParser.SERDEPROPERTIES, 0); }
		public TerminalNode SESSION_USER() { return getToken(ArcticSqlCommandParser.SESSION_USER, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlCommandParser.SET, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlCommandParser.SETS, 0); }
		public TerminalNode SHOW() { return getToken(ArcticSqlCommandParser.SHOW, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSqlCommandParser.SKEWED, 0); }
		public TerminalNode SOME() { return getToken(ArcticSqlCommandParser.SOME, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlCommandParser.SORT, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlCommandParser.SORTED, 0); }
		public TerminalNode START() { return getToken(ArcticSqlCommandParser.START, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSqlCommandParser.STATISTICS, 0); }
		public TerminalNode STORED() { return getToken(ArcticSqlCommandParser.STORED, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticSqlCommandParser.STRATIFY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSqlCommandParser.STRUCT, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticSqlCommandParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticSqlCommandParser.SUBSTRING, 0); }
		public TerminalNode SYNC() { return getToken(ArcticSqlCommandParser.SYNC, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlCommandParser.TABLE, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSqlCommandParser.TABLES, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSqlCommandParser.TABLESAMPLE, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlCommandParser.TBLPROPERTIES, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlCommandParser.TEMPORARY, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticSqlCommandParser.TERMINATED, 0); }
		public TerminalNode THEN() { return getToken(ArcticSqlCommandParser.THEN, 0); }
		public TerminalNode TIME() { return getToken(ArcticSqlCommandParser.TIME, 0); }
		public TerminalNode TO() { return getToken(ArcticSqlCommandParser.TO, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSqlCommandParser.TOUCH, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticSqlCommandParser.TRAILING, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSqlCommandParser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSqlCommandParser.TRANSACTIONS, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticSqlCommandParser.TRANSFORM, 0); }
		public TerminalNode TRIM() { return getToken(ArcticSqlCommandParser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSqlCommandParser.TRUE, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticSqlCommandParser.TRUNCATE, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticSqlCommandParser.TRY_CAST, 0); }
		public TerminalNode TYPE() { return getToken(ArcticSqlCommandParser.TYPE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSqlCommandParser.UNARCHIVE, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticSqlCommandParser.UNBOUNDED, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticSqlCommandParser.UNCACHE, 0); }
		public TerminalNode UNIQUE() { return getToken(ArcticSqlCommandParser.UNIQUE, 0); }
		public TerminalNode UNKNOWN() { return getToken(ArcticSqlCommandParser.UNKNOWN, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSqlCommandParser.UNLOCK, 0); }
		public TerminalNode UNSET() { return getToken(ArcticSqlCommandParser.UNSET, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticSqlCommandParser.UPDATE, 0); }
		public TerminalNode USE() { return getToken(ArcticSqlCommandParser.USE, 0); }
		public TerminalNode USER() { return getToken(ArcticSqlCommandParser.USER, 0); }
		public TerminalNode VALUES() { return getToken(ArcticSqlCommandParser.VALUES, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlCommandParser.VIEW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticSqlCommandParser.VIEWS, 0); }
		public TerminalNode WHEN() { return getToken(ArcticSqlCommandParser.WHEN, 0); }
		public TerminalNode WHERE() { return getToken(ArcticSqlCommandParser.WHERE, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticSqlCommandParser.WINDOW, 0); }
		public TerminalNode WITH() { return getToken(ArcticSqlCommandParser.WITH, 0); }
		public TerminalNode YEAR() { return getToken(ArcticSqlCommandParser.YEAR, 0); }
		public TerminalNode ZONE() { return getToken(ArcticSqlCommandParser.ZONE, 0); }
		public NonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).enterNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlCommandListener ) ((ArcticSqlCommandListener)listener).exitNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlCommandVisitor ) return ((ArcticSqlCommandVisitor<? extends T>)visitor).visitNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonReservedContext nonReserved() throws RecognitionException {
		NonReservedContext _localctx = new NonReservedContext(_ctx, getState());
		enterRule(_localctx, 288, RULE_nonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(3151);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ADD) | (1L << AFTER) | (1L << ALL) | (1L << ALTER) | (1L << ANALYZE) | (1L << AND) | (1L << ANY) | (1L << ARCHIVE) | (1L << ARRAY) | (1L << AS) | (1L << ASC) | (1L << AT) | (1L << AUTHORIZATION) | (1L << BETWEEN) | (1L << BOTH) | (1L << BUCKET) | (1L << BUCKETS) | (1L << BY) | (1L << CACHE) | (1L << CASCADE) | (1L << CASE) | (1L << CAST) | (1L << CHANGE) | (1L << CHECK) | (1L << CLEAR) | (1L << CLUSTER) | (1L << CLUSTERED) | (1L << CODEGEN) | (1L << COLLATE) | (1L << COLLECTION) | (1L << COLUMN) | (1L << COLUMNS) | (1L << COMMENT) | (1L << COMMIT) | (1L << COMPACT) | (1L << COMPACTIONS) | (1L << COMPUTE) | (1L << CONCATENATE) | (1L << CONSTRAINT) | (1L << COST) | (1L << CREATE) | (1L << CUBE) | (1L << CURRENT) | (1L << CURRENT_DATE) | (1L << CURRENT_TIME) | (1L << CURRENT_TIMESTAMP) | (1L << CURRENT_USER))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (DAY - 64)) | (1L << (DATA - 64)) | (1L << (DATABASE - 64)) | (1L << (DATABASES - 64)) | (1L << (DBPROPERTIES - 64)) | (1L << (DEFINED - 64)) | (1L << (DELETE - 64)) | (1L << (DELIMITED - 64)) | (1L << (DESC - 64)) | (1L << (DESCRIBE - 64)) | (1L << (DFS - 64)) | (1L << (DIRECTORIES - 64)) | (1L << (DIRECTORY - 64)) | (1L << (DISTINCT - 64)) | (1L << (DISTRIBUTE - 64)) | (1L << (DIV - 64)) | (1L << (DROP - 64)) | (1L << (ELSE - 64)) | (1L << (END - 64)) | (1L << (ESCAPE - 64)) | (1L << (ESCAPED - 64)) | (1L << (EXCHANGE - 64)) | (1L << (EXISTS - 64)) | (1L << (EXPLAIN - 64)) | (1L << (EXPORT - 64)) | (1L << (EXTENDED - 64)) | (1L << (EXTERNAL - 64)) | (1L << (EXTRACT - 64)) | (1L << (FALSE - 64)) | (1L << (FETCH - 64)) | (1L << (FIELDS - 64)) | (1L << (FILTER - 64)) | (1L << (FILEFORMAT - 64)) | (1L << (FIRST - 64)) | (1L << (FOLLOWING - 64)) | (1L << (FOR - 64)) | (1L << (FOREIGN - 64)) | (1L << (FORMAT - 64)) | (1L << (FORMATTED - 64)) | (1L << (FROM - 64)) | (1L << (FUNCTION - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (GLOBAL - 64)) | (1L << (GRANT - 64)) | (1L << (GROUP - 64)) | (1L << (GROUPING - 64)) | (1L << (HAVING - 64)) | (1L << (HOUR - 64)) | (1L << (IF - 64)) | (1L << (IGNORE - 64)) | (1L << (IMPORT - 64)) | (1L << (IN - 64)) | (1L << (INDEX - 64)) | (1L << (INDEXES - 64)) | (1L << (INPATH - 64)) | (1L << (INPUTFORMAT - 64)) | (1L << (INSERT - 64)) | (1L << (INTERVAL - 64)) | (1L << (INTO - 64)) | (1L << (IS - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (ITEMS - 128)) | (1L << (KEYS - 128)) | (1L << (LAST - 128)) | (1L << (LAZY - 128)) | (1L << (LEADING - 128)) | (1L << (LIKE - 128)) | (1L << (LIMIT - 128)) | (1L << (LINES - 128)) | (1L << (LIST - 128)) | (1L << (LOAD - 128)) | (1L << (LOCAL - 128)) | (1L << (LOCATION - 128)) | (1L << (LOCK - 128)) | (1L << (LOCKS - 128)) | (1L << (LOGICAL - 128)) | (1L << (MACRO - 128)) | (1L << (MAP - 128)) | (1L << (MATCHED - 128)) | (1L << (MERGE - 128)) | (1L << (MINUTE - 128)) | (1L << (MONTH - 128)) | (1L << (MSCK - 128)) | (1L << (NAMESPACE - 128)) | (1L << (NAMESPACES - 128)) | (1L << (NO - 128)) | (1L << (NOT - 128)) | (1L << (NULL - 128)) | (1L << (NULLS - 128)) | (1L << (OF - 128)) | (1L << (ONLY - 128)) | (1L << (OPTION - 128)) | (1L << (OPTIONS - 128)) | (1L << (OR - 128)) | (1L << (ORDER - 128)) | (1L << (OUT - 128)) | (1L << (OUTER - 128)) | (1L << (OUTPUTFORMAT - 128)) | (1L << (OVER - 128)) | (1L << (OVERLAPS - 128)) | (1L << (OVERLAY - 128)) | (1L << (OVERWRITE - 128)) | (1L << (PARTITION - 128)) | (1L << (PARTITIONED - 128)) | (1L << (PARTITIONS - 128)) | (1L << (PERCENTLIT - 128)) | (1L << (PIVOT - 128)) | (1L << (PLACING - 128)) | (1L << (POSITION - 128)) | (1L << (PRECEDING - 128)) | (1L << (PRIMARY - 128)) | (1L << (PRINCIPALS - 128)) | (1L << (PROPERTIES - 128)) | (1L << (PURGE - 128)) | (1L << (QUERY - 128)) | (1L << (RANGE - 128)) | (1L << (RECORDREADER - 128)) | (1L << (RECORDWRITER - 128)) | (1L << (RECOVER - 128)) | (1L << (REDUCE - 128)))) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & ((1L << (REFERENCES - 192)) | (1L << (REFRESH - 192)) | (1L << (RENAME - 192)) | (1L << (REPAIR - 192)) | (1L << (REPLACE - 192)) | (1L << (RESET - 192)) | (1L << (RESPECT - 192)) | (1L << (RESTRICT - 192)) | (1L << (REVOKE - 192)) | (1L << (RLIKE - 192)) | (1L << (ROLE - 192)) | (1L << (ROLES - 192)) | (1L << (ROLLBACK - 192)) | (1L << (ROLLUP - 192)) | (1L << (ROW - 192)) | (1L << (ROWS - 192)) | (1L << (SECOND - 192)) | (1L << (SCHEMA - 192)) | (1L << (SELECT - 192)) | (1L << (SEPARATED - 192)) | (1L << (SERDE - 192)) | (1L << (SERDEPROPERTIES - 192)) | (1L << (SESSION_USER - 192)) | (1L << (SET - 192)) | (1L << (SETS - 192)) | (1L << (SHOW - 192)) | (1L << (SKEWED - 192)) | (1L << (SOME - 192)) | (1L << (SORT - 192)) | (1L << (SORTED - 192)) | (1L << (START - 192)) | (1L << (STATISTICS - 192)) | (1L << (STORED - 192)) | (1L << (STRATIFY - 192)) | (1L << (STRUCT - 192)) | (1L << (SUBSTR - 192)) | (1L << (SUBSTRING - 192)) | (1L << (SYNC - 192)) | (1L << (TABLE - 192)) | (1L << (TABLES - 192)) | (1L << (TABLESAMPLE - 192)) | (1L << (TBLPROPERTIES - 192)) | (1L << (TEMPORARY - 192)) | (1L << (TERMINATED - 192)) | (1L << (THEN - 192)) | (1L << (TIME - 192)) | (1L << (TO - 192)) | (1L << (TOUCH - 192)) | (1L << (TRAILING - 192)) | (1L << (TRANSACTION - 192)) | (1L << (TRANSACTIONS - 192)) | (1L << (TRANSFORM - 192)) | (1L << (TRIM - 192)) | (1L << (TRUE - 192)) | (1L << (TRUNCATE - 192)) | (1L << (TRY_CAST - 192)) | (1L << (TYPE - 192)) | (1L << (UNARCHIVE - 192)) | (1L << (UNBOUNDED - 192)) | (1L << (UNCACHE - 192)))) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & ((1L << (UNIQUE - 256)) | (1L << (UNKNOWN - 256)) | (1L << (UNLOCK - 256)) | (1L << (UNSET - 256)) | (1L << (UPDATE - 256)) | (1L << (USE - 256)) | (1L << (USER - 256)) | (1L << (VALUES - 256)) | (1L << (VIEW - 256)) | (1L << (VIEWS - 256)) | (1L << (WHEN - 256)) | (1L << (WHERE - 256)) | (1L << (WINDOW - 256)) | (1L << (WITH - 256)) | (1L << (YEAR - 256)) | (1L << (ZONE - 256)))) != 0)) ) {
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
		case 44:
			return queryTerm_sempred((QueryTermContext)_localctx, predIndex);
		case 103:
			return booleanExpression_sempred((BooleanExpressionContext)_localctx, predIndex);
		case 105:
			return valueExpression_sempred((ValueExpressionContext)_localctx, predIndex);
		case 106:
			return primaryExpression_sempred((PrimaryExpressionContext)_localctx, predIndex);
		case 137:
			return identifier_sempred((IdentifierContext)_localctx, predIndex);
		case 138:
			return strictIdentifier_sempred((StrictIdentifierContext)_localctx, predIndex);
		case 140:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\u0134\u0c54\4\2\t"+
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
		"\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\7\4\u012f\n\4\f\4\16\4\u0132"+
		"\13\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t"+
		"\3\t\3\n\3\n\3\n\3\13\3\13\5\13\u014a\n\13\3\13\3\13\3\13\5\13\u014f\n"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u0157\n\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\7\13\u015f\n\13\f\13\16\13\u0162\13\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13"+
		"\u0175\n\13\3\13\3\13\5\13\u0179\n\13\3\13\3\13\3\13\3\13\5\13\u017f\n"+
		"\13\3\13\5\13\u0182\n\13\3\13\5\13\u0185\n\13\3\13\3\13\3\13\3\13\3\13"+
		"\5\13\u018c\n\13\3\13\5\13\u018f\n\13\3\13\3\13\5\13\u0193\n\13\3\13\5"+
		"\13\u0196\n\13\3\13\3\13\3\13\3\13\3\13\5\13\u019d\n\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u01a8\n\13\f\13\16\13\u01ab\13\13"+
		"\3\13\3\13\3\13\3\13\3\13\5\13\u01b2\n\13\3\13\5\13\u01b5\n\13\3\13\3"+
		"\13\5\13\u01b9\n\13\3\13\5\13\u01bc\n\13\3\13\3\13\3\13\3\13\5\13\u01c2"+
		"\n\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u01cd\n\13\3\13"+
		"\3\13\3\13\3\13\5\13\u01d3\n\13\3\13\3\13\3\13\5\13\u01d8\n\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\5\13\u0218\n\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\5\13\u0221\n\13\3\13\3\13\5\13\u0225\n\13\3\13\3\13\3\13\3\13\5"+
		"\13\u022b\n\13\3\13\3\13\5\13\u022f\n\13\3\13\3\13\3\13\5\13\u0234\n\13"+
		"\3\13\3\13\3\13\3\13\5\13\u023a\n\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\5\13\u0246\n\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u024e"+
		"\n\13\3\13\3\13\3\13\3\13\5\13\u0254\n\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\5\13\u0261\n\13\3\13\6\13\u0264\n\13\r\13\16"+
		"\13\u0265\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\5\13\u0276\n\13\3\13\3\13\3\13\7\13\u027b\n\13\f\13\16\13\u027e"+
		"\13\13\3\13\5\13\u0281\n\13\3\13\3\13\3\13\3\13\5\13\u0287\n\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u0296"+
		"\n\13\3\13\3\13\5\13\u029a\n\13\3\13\3\13\3\13\3\13\5\13\u02a0\n\13\3"+
		"\13\3\13\3\13\3\13\5\13\u02a6\n\13\3\13\5\13\u02a9\n\13\3\13\5\13\u02ac"+
		"\n\13\3\13\3\13\3\13\3\13\5\13\u02b2\n\13\3\13\3\13\5\13\u02b6\n\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\7\13\u02be\n\13\f\13\16\13\u02c1\13\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\5\13\u02c9\n\13\3\13\5\13\u02cc\n\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u02d5\n\13\3\13\3\13\3\13\5\13\u02da"+
		"\n\13\3\13\3\13\3\13\3\13\5\13\u02e0\n\13\3\13\3\13\3\13\3\13\3\13\5\13"+
		"\u02e7\n\13\3\13\5\13\u02ea\n\13\3\13\3\13\3\13\3\13\5\13\u02f0\n\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u02f9\n\13\f\13\16\13\u02fc\13"+
		"\13\5\13\u02fe\n\13\3\13\3\13\5\13\u0302\n\13\3\13\3\13\3\13\5\13\u0307"+
		"\n\13\3\13\3\13\3\13\5\13\u030c\n\13\3\13\3\13\3\13\3\13\3\13\5\13\u0313"+
		"\n\13\3\13\5\13\u0316\n\13\3\13\5\13\u0319\n\13\3\13\3\13\3\13\3\13\3"+
		"\13\5\13\u0320\n\13\3\13\3\13\3\13\5\13\u0325\n\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\5\13\u032e\n\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u0336"+
		"\n\13\3\13\3\13\3\13\3\13\5\13\u033c\n\13\3\13\5\13\u033f\n\13\3\13\5"+
		"\13\u0342\n\13\3\13\3\13\3\13\3\13\5\13\u0348\n\13\3\13\3\13\5\13\u034c"+
		"\n\13\3\13\3\13\5\13\u0350\n\13\3\13\3\13\5\13\u0354\n\13\5\13\u0356\n"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u035e\n\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\5\13\u0366\n\13\3\13\3\13\3\13\3\13\5\13\u036c\n\13\3\13\3"+
		"\13\3\13\3\13\5\13\u0372\n\13\3\13\5\13\u0375\n\13\3\13\3\13\5\13\u0379"+
		"\n\13\3\13\5\13\u037c\n\13\3\13\3\13\5\13\u0380\n\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u039a\n\13\f\13\16\13\u039d\13"+
		"\13\5\13\u039f\n\13\3\13\3\13\5\13\u03a3\n\13\3\13\3\13\3\13\3\13\5\13"+
		"\u03a9\n\13\3\13\5\13\u03ac\n\13\3\13\5\13\u03af\n\13\3\13\3\13\3\13\3"+
		"\13\5\13\u03b5\n\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u03bd\n\13\3\13"+
		"\3\13\3\13\5\13\u03c2\n\13\3\13\3\13\3\13\3\13\5\13\u03c8\n\13\3\13\3"+
		"\13\3\13\3\13\5\13\u03ce\n\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u03d6"+
		"\n\13\3\13\3\13\3\13\7\13\u03db\n\13\f\13\16\13\u03de\13\13\3\13\3\13"+
		"\3\13\7\13\u03e3\n\13\f\13\16\13\u03e6\13\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u03f4\n\13\f\13\16\13\u03f7\13"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u0402\n\13\f\13"+
		"\16\13\u0405\13\13\5\13\u0407\n\13\3\13\3\13\7\13\u040b\n\13\f\13\16\13"+
		"\u040e\13\13\3\13\3\13\3\13\3\13\7\13\u0414\n\13\f\13\16\13\u0417\13\13"+
		"\3\13\3\13\3\13\3\13\7\13\u041d\n\13\f\13\16\13\u0420\13\13\3\13\3\13"+
		"\7\13\u0424\n\13\f\13\16\13\u0427\13\13\5\13\u0429\n\13\3\f\3\f\3\r\3"+
		"\r\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0435\n\16\3\16\3\16\5\16\u0439"+
		"\n\16\3\16\3\16\3\16\3\16\3\16\5\16\u0440\n\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u04b4\n\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u04bc\n\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\5\16\u04c4\n\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u04cd\n"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u04d7\n\16\3\17\3\17"+
		"\5\17\u04db\n\17\3\17\5\17\u04de\n\17\3\17\3\17\3\17\3\17\5\17\u04e4\n"+
		"\17\3\17\3\17\3\20\3\20\5\20\u04ea\n\20\3\20\3\20\3\20\3\20\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\5\21\u04f6\n\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22"+
		"\3\22\3\22\3\22\5\22\u0502\n\22\3\22\3\22\3\22\5\22\u0507\n\22\3\23\3"+
		"\23\3\23\3\24\3\24\3\24\3\25\5\25\u0510\n\25\3\25\3\25\3\25\3\26\3\26"+
		"\3\26\5\26\u0518\n\26\3\26\3\26\3\26\3\26\3\26\5\26\u051f\n\26\5\26\u0521"+
		"\n\26\3\26\5\26\u0524\n\26\3\26\3\26\3\26\5\26\u0529\n\26\3\26\3\26\5"+
		"\26\u052d\n\26\3\26\3\26\3\26\5\26\u0532\n\26\3\26\5\26\u0535\n\26\3\26"+
		"\3\26\3\26\5\26\u053a\n\26\3\26\3\26\3\26\5\26\u053f\n\26\3\26\5\26\u0542"+
		"\n\26\3\26\3\26\3\26\5\26\u0547\n\26\3\26\3\26\5\26\u054b\n\26\3\26\3"+
		"\26\3\26\5\26\u0550\n\26\5\26\u0552\n\26\3\27\3\27\5\27\u0556\n\27\3\30"+
		"\3\30\3\30\3\30\3\30\7\30\u055d\n\30\f\30\16\30\u0560\13\30\3\30\3\30"+
		"\3\31\3\31\3\31\5\31\u0567\n\31\3\32\3\32\3\33\3\33\3\33\3\33\3\33\5\33"+
		"\u0570\n\33\3\34\3\34\3\34\7\34\u0575\n\34\f\34\16\34\u0578\13\34\3\35"+
		"\3\35\3\35\3\35\7\35\u057e\n\35\f\35\16\35\u0581\13\35\3\36\3\36\5\36"+
		"\u0585\n\36\3\36\5\36\u0588\n\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3"+
		" \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \7 \u059e\n \f \16 \u05a1\13 \3!"+
		"\3!\3!\3!\7!\u05a7\n!\f!\16!\u05aa\13!\3!\3!\3\"\3\"\5\"\u05b0\n\"\3\""+
		"\5\"\u05b3\n\"\3#\3#\3#\7#\u05b8\n#\f#\16#\u05bb\13#\3#\5#\u05be\n#\3"+
		"$\3$\3$\3$\5$\u05c4\n$\3%\3%\3%\3%\7%\u05ca\n%\f%\16%\u05cd\13%\3%\3%"+
		"\3&\3&\3&\3&\7&\u05d5\n&\f&\16&\u05d8\13&\3&\3&\3\'\3\'\3\'\3\'\3\'\3"+
		"\'\5\'\u05e2\n\'\3(\3(\3(\3(\3(\5(\u05e9\n(\3)\3)\3)\3)\5)\u05ef\n)\3"+
		"*\3*\3*\3+\3+\3+\3+\3+\3+\6+\u05fa\n+\r+\16+\u05fb\3+\3+\3+\3+\3+\5+\u0603"+
		"\n+\3+\3+\3+\3+\3+\5+\u060a\n+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\5+\u0616"+
		"\n+\3+\3+\3+\3+\7+\u061c\n+\f+\16+\u061f\13+\3+\7+\u0622\n+\f+\16+\u0625"+
		"\13+\5+\u0627\n+\3,\3,\3,\3,\3,\7,\u062e\n,\f,\16,\u0631\13,\5,\u0633"+
		"\n,\3,\3,\3,\3,\3,\7,\u063a\n,\f,\16,\u063d\13,\5,\u063f\n,\3,\3,\3,\3"+
		",\3,\7,\u0646\n,\f,\16,\u0649\13,\5,\u064b\n,\3,\3,\3,\3,\3,\7,\u0652"+
		"\n,\f,\16,\u0655\13,\5,\u0657\n,\3,\5,\u065a\n,\3,\3,\3,\5,\u065f\n,\5"+
		",\u0661\n,\3-\3-\3-\3.\3.\3.\3.\3.\3.\3.\5.\u066d\n.\3.\3.\3.\3.\3.\5"+
		".\u0674\n.\3.\3.\3.\3.\3.\5.\u067b\n.\3.\7.\u067e\n.\f.\16.\u0681\13."+
		"\3/\3/\3/\3/\3/\3/\3/\3/\3/\5/\u068c\n/\3\60\3\60\5\60\u0690\n\60\3\60"+
		"\3\60\5\60\u0694\n\60\3\61\3\61\6\61\u0698\n\61\r\61\16\61\u0699\3\62"+
		"\3\62\5\62\u069e\n\62\3\62\3\62\3\62\3\62\7\62\u06a4\n\62\f\62\16\62\u06a7"+
		"\13\62\3\62\5\62\u06aa\n\62\3\62\5\62\u06ad\n\62\3\62\5\62\u06b0\n\62"+
		"\3\62\5\62\u06b3\n\62\3\62\3\62\5\62\u06b7\n\62\3\63\3\63\5\63\u06bb\n"+
		"\63\3\63\7\63\u06be\n\63\f\63\16\63\u06c1\13\63\3\63\5\63\u06c4\n\63\3"+
		"\63\5\63\u06c7\n\63\3\63\5\63\u06ca\n\63\3\63\5\63\u06cd\n\63\3\63\3\63"+
		"\5\63\u06d1\n\63\3\63\7\63\u06d4\n\63\f\63\16\63\u06d7\13\63\3\63\5\63"+
		"\u06da\n\63\3\63\5\63\u06dd\n\63\3\63\5\63\u06e0\n\63\3\63\5\63\u06e3"+
		"\n\63\5\63\u06e5\n\63\3\64\3\64\3\64\3\64\5\64\u06eb\n\64\3\64\3\64\3"+
		"\64\3\64\3\64\5\64\u06f2\n\64\3\64\3\64\3\64\5\64\u06f7\n\64\3\64\5\64"+
		"\u06fa\n\64\3\64\5\64\u06fd\n\64\3\64\3\64\5\64\u0701\n\64\3\64\3\64\3"+
		"\64\3\64\3\64\3\64\3\64\3\64\5\64\u070b\n\64\3\64\3\64\5\64\u070f\n\64"+
		"\5\64\u0711\n\64\3\64\5\64\u0714\n\64\3\64\3\64\5\64\u0718\n\64\3\65\3"+
		"\65\7\65\u071c\n\65\f\65\16\65\u071f\13\65\3\65\5\65\u0722\n\65\3\65\3"+
		"\65\3\66\3\66\3\66\3\67\3\67\3\67\3\67\5\67\u072d\n\67\3\67\3\67\3\67"+
		"\38\38\38\38\38\58\u0737\n8\38\38\38\39\39\39\39\39\39\39\59\u0743\n9"+
		"\3:\3:\3:\3:\3:\3:\3:\3:\3:\3:\3:\7:\u0750\n:\f:\16:\u0753\13:\3:\3:\5"+
		":\u0757\n:\3;\3;\3;\7;\u075c\n;\f;\16;\u075f\13;\3<\3<\3<\3<\3=\3=\3="+
		"\3>\3>\3>\3?\3?\3?\5?\u076e\n?\3?\7?\u0771\n?\f?\16?\u0774\13?\3?\3?\3"+
		"@\3@\3@\3@\3@\3@\7@\u077e\n@\f@\16@\u0781\13@\3@\3@\5@\u0785\n@\3A\3A"+
		"\3A\3A\7A\u078b\nA\fA\16A\u078e\13A\3A\7A\u0791\nA\fA\16A\u0794\13A\3"+
		"A\5A\u0797\nA\3B\3B\3B\3B\3B\7B\u079e\nB\fB\16B\u07a1\13B\3B\3B\3B\3B"+
		"\3B\7B\u07a8\nB\fB\16B\u07ab\13B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\7B\u07b7"+
		"\nB\fB\16B\u07ba\13B\3B\3B\5B\u07be\nB\5B\u07c0\nB\3C\3C\5C\u07c4\nC\3"+
		"D\3D\3D\3D\3D\7D\u07cb\nD\fD\16D\u07ce\13D\3D\3D\3D\3D\3D\3D\3D\3D\7D"+
		"\u07d8\nD\fD\16D\u07db\13D\3D\3D\5D\u07df\nD\3E\3E\5E\u07e3\nE\3F\3F\3"+
		"F\3F\7F\u07e9\nF\fF\16F\u07ec\13F\5F\u07ee\nF\3F\3F\5F\u07f2\nF\3G\3G"+
		"\3G\3G\3G\3G\3G\3G\3G\3G\7G\u07fe\nG\fG\16G\u0801\13G\3G\3G\3G\3H\3H\3"+
		"H\3H\3H\7H\u080b\nH\fH\16H\u080e\13H\3H\3H\5H\u0812\nH\3I\3I\5I\u0816"+
		"\nI\3I\5I\u0819\nI\3J\3J\3J\5J\u081e\nJ\3J\3J\3J\3J\3J\7J\u0825\nJ\fJ"+
		"\16J\u0828\13J\5J\u082a\nJ\3J\3J\3J\5J\u082f\nJ\3J\3J\3J\7J\u0834\nJ\f"+
		"J\16J\u0837\13J\5J\u0839\nJ\3K\3K\3L\5L\u083e\nL\3L\3L\7L\u0842\nL\fL"+
		"\16L\u0845\13L\3M\3M\3M\5M\u084a\nM\3M\3M\5M\u084e\nM\3M\3M\3M\3M\5M\u0854"+
		"\nM\3M\3M\5M\u0858\nM\3N\5N\u085b\nN\3N\3N\3N\5N\u0860\nN\3N\5N\u0863"+
		"\nN\3N\3N\3N\5N\u0868\nN\3N\3N\5N\u086c\nN\3N\5N\u086f\nN\3N\5N\u0872"+
		"\nN\3O\3O\3O\3O\5O\u0878\nO\3P\3P\3P\5P\u087d\nP\3P\3P\3Q\5Q\u0882\nQ"+
		"\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\5Q\u0894\nQ\5Q\u0896"+
		"\nQ\3Q\5Q\u0899\nQ\3R\3R\3R\3R\3S\3S\3S\7S\u08a2\nS\fS\16S\u08a5\13S\3"+
		"T\3T\3T\3T\7T\u08ab\nT\fT\16T\u08ae\13T\3T\3T\3U\3U\5U\u08b4\nU\3V\3V"+
		"\3V\3V\7V\u08ba\nV\fV\16V\u08bd\13V\3V\3V\3W\3W\5W\u08c3\nW\3X\3X\5X\u08c7"+
		"\nX\3X\3X\3X\3X\3X\3X\5X\u08cf\nX\3X\3X\3X\3X\3X\3X\5X\u08d7\nX\3X\3X"+
		"\3X\3X\5X\u08dd\nX\3Y\3Y\3Y\3Y\7Y\u08e3\nY\fY\16Y\u08e6\13Y\3Y\3Y\3Z\3"+
		"Z\3Z\3Z\3Z\7Z\u08ef\nZ\fZ\16Z\u08f2\13Z\5Z\u08f4\nZ\3Z\3Z\3Z\3[\5[\u08fa"+
		"\n[\3[\3[\5[\u08fe\n[\5[\u0900\n[\3\\\3\\\3\\\3\\\3\\\3\\\3\\\5\\\u0909"+
		"\n\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\5\\\u0915\n\\\5\\\u0917\n"+
		"\\\3\\\3\\\3\\\3\\\3\\\5\\\u091e\n\\\3\\\3\\\3\\\3\\\3\\\5\\\u0925\n\\"+
		"\3\\\3\\\3\\\3\\\5\\\u092b\n\\\3\\\3\\\3\\\3\\\5\\\u0931\n\\\5\\\u0933"+
		"\n\\\3]\3]\3]\7]\u0938\n]\f]\16]\u093b\13]\3^\3^\3^\7^\u0940\n^\f^\16"+
		"^\u0943\13^\3_\3_\3_\5_\u0948\n_\3_\3_\3`\3`\3`\5`\u094f\n`\3`\3`\3a\3"+
		"a\5a\u0955\na\3a\3a\5a\u0959\na\5a\u095b\na\3b\3b\3b\7b\u0960\nb\fb\16"+
		"b\u0963\13b\3c\3c\3c\3c\7c\u0969\nc\fc\16c\u096c\13c\3c\3c\3d\3d\5d\u0972"+
		"\nd\3e\3e\3e\3e\3e\3e\7e\u097a\ne\fe\16e\u097d\13e\3e\3e\5e\u0981\ne\3"+
		"f\3f\5f\u0985\nf\3g\3g\3h\3h\3h\7h\u098c\nh\fh\16h\u098f\13h\3i\3i\3i"+
		"\3i\3i\3i\3i\3i\3i\3i\5i\u099b\ni\5i\u099d\ni\3i\3i\3i\3i\3i\3i\7i\u09a5"+
		"\ni\fi\16i\u09a8\13i\3j\5j\u09ab\nj\3j\3j\3j\3j\3j\3j\5j\u09b3\nj\3j\3"+
		"j\3j\3j\3j\7j\u09ba\nj\fj\16j\u09bd\13j\3j\3j\3j\5j\u09c2\nj\3j\3j\3j"+
		"\3j\3j\3j\5j\u09ca\nj\3j\3j\3j\5j\u09cf\nj\3j\3j\3j\3j\3j\3j\3j\3j\7j"+
		"\u09d9\nj\fj\16j\u09dc\13j\3j\3j\5j\u09e0\nj\3j\5j\u09e3\nj\3j\3j\3j\3"+
		"j\5j\u09e9\nj\3j\3j\5j\u09ed\nj\3j\3j\3j\5j\u09f2\nj\3j\3j\3j\5j\u09f7"+
		"\nj\3j\3j\3j\5j\u09fc\nj\3k\3k\3k\3k\5k\u0a02\nk\3k\3k\3k\3k\3k\3k\3k"+
		"\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\7k\u0a17\nk\fk\16k\u0a1a\13k\3l\3"+
		"l\3l\3l\6l\u0a20\nl\rl\16l\u0a21\3l\3l\5l\u0a26\nl\3l\3l\3l\3l\3l\6l\u0a2d"+
		"\nl\rl\16l\u0a2e\3l\3l\5l\u0a33\nl\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3"+
		"l\3l\3l\7l\u0a43\nl\fl\16l\u0a46\13l\5l\u0a48\nl\3l\3l\3l\3l\3l\3l\5l"+
		"\u0a50\nl\3l\3l\3l\3l\3l\3l\3l\5l\u0a59\nl\3l\3l\3l\3l\3l\3l\3l\3l\3l"+
		"\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\6l\u0a6e\nl\rl\16l\u0a6f\3l\3l\3l\3l\3"+
		"l\3l\3l\3l\3l\5l\u0a7b\nl\3l\3l\3l\7l\u0a80\nl\fl\16l\u0a83\13l\5l\u0a85"+
		"\nl\3l\3l\3l\3l\3l\3l\3l\5l\u0a8e\nl\3l\3l\5l\u0a92\nl\3l\3l\5l\u0a96"+
		"\nl\3l\3l\3l\3l\3l\3l\3l\3l\6l\u0aa0\nl\rl\16l\u0aa1\3l\3l\3l\3l\3l\3"+
		"l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\5l\u0abb\nl\3l\3"+
		"l\3l\3l\3l\5l\u0ac2\nl\3l\5l\u0ac5\nl\3l\3l\3l\3l\3l\3l\3l\3l\3l\3l\3"+
		"l\3l\3l\5l\u0ad4\nl\3l\3l\5l\u0ad8\nl\3l\3l\3l\3l\3l\3l\3l\3l\7l\u0ae2"+
		"\nl\fl\16l\u0ae5\13l\3m\3m\3m\3m\3m\3m\3m\3m\6m\u0aef\nm\rm\16m\u0af0"+
		"\5m\u0af3\nm\3n\3n\3o\3o\3p\3p\3q\3q\3r\3r\3r\5r\u0b00\nr\3s\3s\5s\u0b04"+
		"\ns\3t\3t\3t\6t\u0b09\nt\rt\16t\u0b0a\3u\3u\3u\5u\u0b10\nu\3v\3v\3v\3"+
		"v\3v\3w\5w\u0b18\nw\3w\3w\3x\3x\3x\5x\u0b1f\nx\3y\3y\3y\3y\3y\3y\3y\3"+
		"y\3y\3y\3y\3y\3y\3y\3y\5y\u0b30\ny\3y\3y\5y\u0b34\ny\3y\3y\3y\3y\5y\u0b3a"+
		"\ny\3y\3y\3y\3y\5y\u0b40\ny\3y\3y\3y\3y\3y\7y\u0b47\ny\fy\16y\u0b4a\13"+
		"y\3y\5y\u0b4d\ny\5y\u0b4f\ny\3z\3z\3z\7z\u0b54\nz\fz\16z\u0b57\13z\3{"+
		"\3{\3{\3{\5{\u0b5d\n{\3{\5{\u0b60\n{\3{\5{\u0b63\n{\3|\3|\3|\7|\u0b68"+
		"\n|\f|\16|\u0b6b\13|\3}\3}\3}\3}\5}\u0b71\n}\3}\5}\u0b74\n}\3~\3~\3~\7"+
		"~\u0b79\n~\f~\16~\u0b7c\13~\3\177\3\177\5\177\u0b80\n\177\3\177\3\177"+
		"\3\177\5\177\u0b85\n\177\3\177\5\177\u0b88\n\177\3\u0080\3\u0080\3\u0080"+
		"\3\u0080\3\u0080\3\u0081\3\u0081\3\u0081\3\u0081\7\u0081\u0b93\n\u0081"+
		"\f\u0081\16\u0081\u0b96\13\u0081\3\u0082\3\u0082\3\u0082\3\u0082\3\u0083"+
		"\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083\3\u0083"+
		"\3\u0083\7\u0083\u0ba7\n\u0083\f\u0083\16\u0083\u0baa\13\u0083\3\u0083"+
		"\3\u0083\3\u0083\3\u0083\3\u0083\7\u0083\u0bb1\n\u0083\f\u0083\16\u0083"+
		"\u0bb4\13\u0083\5\u0083\u0bb6\n\u0083\3\u0083\3\u0083\3\u0083\3\u0083"+
		"\3\u0083\7\u0083\u0bbd\n\u0083\f\u0083\16\u0083\u0bc0\13\u0083\5\u0083"+
		"\u0bc2\n\u0083\5\u0083\u0bc4\n\u0083\3\u0083\5\u0083\u0bc7\n\u0083\3\u0083"+
		"\5\u0083\u0bca\n\u0083\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084"+
		"\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084\3\u0084"+
		"\3\u0084\5\u0084\u0bdc\n\u0084\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085"+
		"\3\u0085\3\u0085\5\u0085\u0be5\n\u0085\3\u0086\3\u0086\3\u0086\7\u0086"+
		"\u0bea\n\u0086\f\u0086\16\u0086\u0bed\13\u0086\3\u0087\3\u0087\3\u0087"+
		"\3\u0087\5\u0087\u0bf3\n\u0087\3\u0088\3\u0088\3\u0088\7\u0088\u0bf8\n"+
		"\u0088\f\u0088\16\u0088\u0bfb\13\u0088\3\u0089\3\u0089\3\u0089\3\u008a"+
		"\3\u008a\6\u008a\u0c02\n\u008a\r\u008a\16\u008a\u0c03\3\u008a\5\u008a"+
		"\u0c07\n\u008a\3\u008b\3\u008b\3\u008b\5\u008b\u0c0c\n\u008b\3\u008c\3"+
		"\u008c\3\u008c\3\u008c\3\u008c\3\u008c\5\u008c\u0c14\n\u008c\3\u008d\3"+
		"\u008d\3\u008e\3\u008e\5\u008e\u0c1a\n\u008e\3\u008e\3\u008e\3\u008e\5"+
		"\u008e\u0c1f\n\u008e\3\u008e\3\u008e\3\u008e\5\u008e\u0c24\n\u008e\3\u008e"+
		"\3\u008e\5\u008e\u0c28\n\u008e\3\u008e\3\u008e\5\u008e\u0c2c\n\u008e\3"+
		"\u008e\3\u008e\5\u008e\u0c30\n\u008e\3\u008e\3\u008e\5\u008e\u0c34\n\u008e"+
		"\3\u008e\3\u008e\5\u008e\u0c38\n\u008e\3\u008e\3\u008e\5\u008e\u0c3c\n"+
		"\u008e\3\u008e\3\u008e\5\u008e\u0c40\n\u008e\3\u008e\5\u008e\u0c43\n\u008e"+
		"\3\u008f\3\u008f\3\u008f\3\u008f\3\u008f\3\u008f\3\u008f\5\u008f\u0c4c"+
		"\n\u008f\3\u0090\3\u0090\3\u0091\3\u0091\3\u0092\3\u0092\3\u0092\13\u039b"+
		"\u03dc\u03e4\u03f5\u0403\u040c\u0415\u041e\u0425\6Z\u00d0\u00d4\u00d6"+
		"\u0093\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<"+
		">@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a"+
		"\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2"+
		"\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba"+
		"\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2"+
		"\u00d4\u00d6\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea"+
		"\u00ec\u00ee\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102"+
		"\u0104\u0106\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a"+
		"\u011c\u011e\u0120\u0122\2\65\4\2FF\u00ba\u00ba\4\2%%\u00c9\u00c9\4\2"+
		"EE\u009c\u009c\4\2jjww\3\2\60\61\4\2\u00eb\u00eb\u010b\u010b\4\2\24\24"+
		"((\7\2--99\\\\ii\u0093\u0093\3\2JK\4\2\\\\ii\4\2\u00a0\u00a0\u0125\u0125"+
		"\5\2\21\21RR\u00ea\u00ea\4\2\21\21\u008d\u008d\4\2\u008f\u008f\u0125\u0125"+
		"\5\2DD\u009b\u009b\u00d4\u00d4\6\2WW~~\u00dc\u00dc\u0101\u0101\5\2WW\u00dc"+
		"\u00dc\u0101\u0101\4\2\34\34JJ\4\2dd\u0085\u0085\4\2<<\u00d0\u00d0\4\2"+
		"\23\23OO\4\2\u0129\u0129\u012b\u012b\5\2\23\23\30\30\u00e0\u00e0\5\2_"+
		"_\u00fa\u00fa\u0103\u0103\4\2\u011b\u011c\u0120\u0120\4\2QQ\u011d\u011f"+
		"\4\2\u011b\u011c\u0123\u0123\4\2>>@A\4\2\'\'\u00fc\u00fc\4\2uu\u00c8\u00c8"+
		"\3\2\u00e8\u00e9\4\2\6\6jj\4\2\6\6ff\5\2  \u0088\u0088\u00f5\u00f5\3\2"+
		"\u0113\u011a\4\2QQ\u011b\u0124\6\2\26\26ww\u009f\u009f\u00a7\u00a7\4\2"+
		"__\u00fa\u00fa\3\2\u011b\u011c\5\2\u0125\u0125\u0129\u0129\u012b\u012b"+
		"\4\2\u0099\u0099\u0111\u0111\6\2BBss\u0098\u0098\u00d3\u00d3\5\2ss\u0098"+
		"\u0098\u00d3\u00d3\4\2PP\u00b0\u00b0\4\2\u00a8\u00a8\u00e1\u00e1\4\2e"+
		"e\u00b7\u00b7\3\2\u012a\u012b\4\2RR\u00db\u00db\63\2\21\22\24\25\27\27"+
		"\31\32\34\35\37\37!%((*-//\61\6799<=BNPRVVX^aacehilnqqsvxy{}\177\177\u0082"+
		"\u0082\u0084\u0085\u0087\u0087\u008a\u009c\u009e\u009e\u00a1\u00a2\u00a5"+
		"\u00a6\u00a9\u00a9\u00ab\u00ac\u00ae\u00b7\u00b9\u00c1\u00c3\u00ca\u00cc"+
		"\u00d4\u00d6\u00d9\u00db\u00df\u00e1\u00ea\u00ec\u00f0\u00f4\u00f4\u00f6"+
		"\u0100\u0104\u0107\u010a\u010c\u010f\u010f\u0111\u0112\22\2\27\27;;WW"+
		"kkzz~~\u0083\u0083\u0086\u0086\u0089\u0089\u009d\u009d\u00a3\u00a3\u00cb"+
		"\u00cb\u00d6\u00d6\u00dc\u00dc\u0101\u0101\u0109\u0109\23\2\21\26\30:"+
		"<VXjly{}\177\u0082\u0084\u0085\u0087\u0088\u008a\u009c\u009e\u00a2\u00a4"+
		"\u00ca\u00cc\u00d5\u00d7\u00db\u00dd\u0100\u0102\u0108\u010a\u0112\2\u0e3b"+
		"\2\u0124\3\2\2\2\4\u0126\3\2\2\2\6\u012c\3\2\2\2\b\u0135\3\2\2\2\n\u0138"+
		"\3\2\2\2\f\u013b\3\2\2\2\16\u013e\3\2\2\2\20\u0141\3\2\2\2\22\u0144\3"+
		"\2\2\2\24\u0428\3\2\2\2\26\u042a\3\2\2\2\30\u042c\3\2\2\2\32\u04d6\3\2"+
		"\2\2\34\u04d8\3\2\2\2\36\u04e9\3\2\2\2 \u04ef\3\2\2\2\"\u04fb\3\2\2\2"+
		"$\u0508\3\2\2\2&\u050b\3\2\2\2(\u050f\3\2\2\2*\u0551\3\2\2\2,\u0553\3"+
		"\2\2\2.\u0557\3\2\2\2\60\u0563\3\2\2\2\62\u0568\3\2\2\2\64\u056f\3\2\2"+
		"\2\66\u0571\3\2\2\28\u0579\3\2\2\2:\u0582\3\2\2\2<\u058d\3\2\2\2>\u059f"+
		"\3\2\2\2@\u05a2\3\2\2\2B\u05ad\3\2\2\2D\u05bd\3\2\2\2F\u05c3\3\2\2\2H"+
		"\u05c5\3\2\2\2J\u05d0\3\2\2\2L\u05e1\3\2\2\2N\u05e8\3\2\2\2P\u05ea\3\2"+
		"\2\2R\u05f0\3\2\2\2T\u0626\3\2\2\2V\u0632\3\2\2\2X\u0662\3\2\2\2Z\u0665"+
		"\3\2\2\2\\\u068b\3\2\2\2^\u068d\3\2\2\2`\u0695\3\2\2\2b\u06b6\3\2\2\2"+
		"d\u06e4\3\2\2\2f\u06f9\3\2\2\2h\u0719\3\2\2\2j\u0725\3\2\2\2l\u0728\3"+
		"\2\2\2n\u0731\3\2\2\2p\u0742\3\2\2\2r\u0756\3\2\2\2t\u0758\3\2\2\2v\u0760"+
		"\3\2\2\2x\u0764\3\2\2\2z\u0767\3\2\2\2|\u076a\3\2\2\2~\u0784\3\2\2\2\u0080"+
		"\u0786\3\2\2\2\u0082\u07bf\3\2\2\2\u0084\u07c3\3\2\2\2\u0086\u07de\3\2"+
		"\2\2\u0088\u07e2\3\2\2\2\u008a\u07f1\3\2\2\2\u008c\u07f3\3\2\2\2\u008e"+
		"\u0811\3\2\2\2\u0090\u0813\3\2\2\2\u0092\u081a\3\2\2\2\u0094\u083a\3\2"+
		"\2\2\u0096\u083d\3\2\2\2\u0098\u0857\3\2\2\2\u009a\u0871\3\2\2\2\u009c"+
		"\u0877\3\2\2\2\u009e\u0879\3\2\2\2\u00a0\u0898\3\2\2\2\u00a2\u089a\3\2"+
		"\2\2\u00a4\u089e\3\2\2\2\u00a6\u08a6\3\2\2\2\u00a8\u08b1\3\2\2\2\u00aa"+
		"\u08b5\3\2\2\2\u00ac\u08c0\3\2\2\2\u00ae\u08dc\3\2\2\2\u00b0\u08de\3\2"+
		"\2\2\u00b2\u08e9\3\2\2\2\u00b4\u08ff\3\2\2\2\u00b6\u0932\3\2\2\2\u00b8"+
		"\u0934\3\2\2\2\u00ba\u093c\3\2\2\2\u00bc\u0947\3\2\2\2\u00be\u094e\3\2"+
		"\2\2\u00c0\u0952\3\2\2\2\u00c2\u095c\3\2\2\2\u00c4\u0964\3\2\2\2\u00c6"+
		"\u0971\3\2\2\2\u00c8\u0980\3\2\2\2\u00ca\u0984\3\2\2\2\u00cc\u0986\3\2"+
		"\2\2\u00ce\u0988\3\2\2\2\u00d0\u099c\3\2\2\2\u00d2\u09fb\3\2\2\2\u00d4"+
		"\u0a01\3\2\2\2\u00d6\u0ad7\3\2\2\2\u00d8\u0af2\3\2\2\2\u00da\u0af4\3\2"+
		"\2\2\u00dc\u0af6\3\2\2\2\u00de\u0af8\3\2\2\2\u00e0\u0afa\3\2\2\2\u00e2"+
		"\u0afc\3\2\2\2\u00e4\u0b01\3\2\2\2\u00e6\u0b08\3\2\2\2\u00e8\u0b0c\3\2"+
		"\2\2\u00ea\u0b11\3\2\2\2\u00ec\u0b17\3\2\2\2\u00ee\u0b1e\3\2\2\2\u00f0"+
		"\u0b4e\3\2\2\2\u00f2\u0b50\3\2\2\2\u00f4\u0b58\3\2\2\2\u00f6\u0b64\3\2"+
		"\2\2\u00f8\u0b6c\3\2\2\2\u00fa\u0b75\3\2\2\2\u00fc\u0b7d\3\2\2\2\u00fe"+
		"\u0b89\3\2\2\2\u0100\u0b8e\3\2\2\2\u0102\u0b97\3\2\2\2\u0104\u0bc9\3\2"+
		"\2\2\u0106\u0bdb\3\2\2\2\u0108\u0be4\3\2\2\2\u010a\u0be6\3\2\2\2\u010c"+
		"\u0bf2\3\2\2\2\u010e\u0bf4\3\2\2\2\u0110\u0bfc\3\2\2\2\u0112\u0c06\3\2"+
		"\2\2\u0114\u0c0b\3\2\2\2\u0116\u0c13\3\2\2\2\u0118\u0c15\3\2\2\2\u011a"+
		"\u0c42\3\2\2\2\u011c\u0c4b\3\2\2\2\u011e\u0c4d\3\2\2\2\u0120\u0c4f\3\2"+
		"\2\2\u0122\u0c51\3\2\2\2\u0124\u0125\5\4\3\2\u0125\3\3\2\2\2\u0126\u0127"+
		"\7\17\2\2\u0127\u0128\5\u00ba^\2\u0128\u0129\7\u00f3\2\2\u0129\u012a\7"+
		"\20\2\2\u012a\u012b\5\u00ba^\2\u012b\5\3\2\2\2\u012c\u0130\5\24\13\2\u012d"+
		"\u012f\7\3\2\2\u012e\u012d\3\2\2\2\u012f\u0132\3\2\2\2\u0130\u012e\3\2"+
		"\2\2\u0130\u0131\3\2\2\2\u0131\u0133\3\2\2\2\u0132\u0130\3\2\2\2\u0133"+
		"\u0134\7\2\2\3\u0134\7\3\2\2\2\u0135\u0136\5\u00c0a\2\u0136\u0137\7\2"+
		"\2\3\u0137\t\3\2\2\2\u0138\u0139\5\u00bc_\2\u0139\u013a\7\2\2\3\u013a"+
		"\13\3\2\2\2\u013b\u013c\5\u00ba^\2\u013c\u013d\7\2\2\3\u013d\r\3\2\2\2"+
		"\u013e\u013f\5\u00be`\2\u013f\u0140\7\2\2\3\u0140\17\3\2\2\2\u0141\u0142"+
		"\5\u00f0y\2\u0142\u0143\7\2\2\3\u0143\21\3\2\2\2\u0144\u0145\5\u00f6|"+
		"\2\u0145\u0146\7\2\2\3\u0146\23\3\2\2\2\u0147\u0429\5(\25\2\u0148\u014a"+
		"\58\35\2\u0149\u0148\3\2\2\2\u0149\u014a\3\2\2\2\u014a\u014b\3\2\2\2\u014b"+
		"\u0429\5T+\2\u014c\u014e\7\u0107\2\2\u014d\u014f\7\u009b\2\2\u014e\u014d"+
		"\3\2\2\2\u014e\u014f\3\2\2\2\u014f\u0150\3\2\2\2\u0150\u0429\5\u00ba^"+
		"\2\u0151\u0152\7:\2\2\u0152\u0156\5\62\32\2\u0153\u0154\7t\2\2\u0154\u0155"+
		"\7\u009f\2\2\u0155\u0157\7Y\2\2\u0156\u0153\3\2\2\2\u0156\u0157\3\2\2"+
		"\2\u0157\u0158\3\2\2\2\u0158\u0160\5\u00ba^\2\u0159\u015f\5&\24\2\u015a"+
		"\u015f\5$\23\2\u015b\u015c\7\u0110\2\2\u015c\u015d\t\2\2\2\u015d\u015f"+
		"\5@!\2\u015e\u0159\3\2\2\2\u015e\u015a\3\2\2\2\u015e\u015b\3\2\2\2\u015f"+
		"\u0162\3\2\2\2\u0160\u015e\3\2\2\2\u0160\u0161\3\2\2\2\u0161\u0429\3\2"+
		"\2\2\u0162\u0160\3\2\2\2\u0163\u0164\7\24\2\2\u0164\u0165\5\62\32\2\u0165"+
		"\u0166\5\u00ba^\2\u0166\u0167\7\u00db\2\2\u0167\u0168\t\2\2\2\u0168\u0169"+
		"\5@!\2\u0169\u0429\3\2\2\2\u016a\u016b\7\24\2\2\u016b\u016c\5\62\32\2"+
		"\u016c\u016d\5\u00ba^\2\u016d\u016e\7\u00db\2\2\u016e\u016f\5$\23\2\u016f"+
		"\u0429\3\2\2\2\u0170\u0171\7R\2\2\u0171\u0174\5\62\32\2\u0172\u0173\7"+
		"t\2\2\u0173\u0175\7Y\2\2\u0174\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175"+
		"\u0176\3\2\2\2\u0176\u0178\5\u00ba^\2\u0177\u0179\t\3\2\2\u0178\u0177"+
		"\3\2\2\2\u0178\u0179\3\2\2\2\u0179\u0429\3\2\2\2\u017a\u017b\7\u00de\2"+
		"\2\u017b\u017e\t\4\2\2\u017c\u017d\t\5\2\2\u017d\u017f\5\u00ba^\2\u017e"+
		"\u017c\3\2\2\2\u017e\u017f\3\2\2\2\u017f\u0184\3\2\2\2\u0180\u0182\7\u008a"+
		"\2\2\u0181\u0180\3\2\2\2\u0181\u0182\3\2\2\2\u0182\u0183\3\2\2\2\u0183"+
		"\u0185\7\u0125\2\2\u0184\u0181\3\2\2\2\u0184\u0185\3\2\2\2\u0185\u0429"+
		"\3\2\2\2\u0186\u018b\5\34\17\2\u0187\u0188\7\4\2\2\u0188\u0189\5\u00f6"+
		"|\2\u0189\u018a\7\5\2\2\u018a\u018c\3\2\2\2\u018b\u0187\3\2\2\2\u018b"+
		"\u018c\3\2\2\2\u018c\u018e\3\2\2\2\u018d\u018f\5<\37\2\u018e\u018d\3\2"+
		"\2\2\u018e\u018f\3\2\2\2\u018f\u0190\3\2\2\2\u0190\u0195\5> \2\u0191\u0193"+
		"\7\33\2\2\u0192\u0191\3\2\2\2\u0192\u0193\3\2\2\2\u0193\u0194\3\2\2\2"+
		"\u0194\u0196\5(\25\2\u0195\u0192\3\2\2\2\u0195\u0196\3\2\2\2\u0196\u0429"+
		"\3\2\2\2\u0197\u0198\7:\2\2\u0198\u019c\7\u00eb\2\2\u0199\u019a\7t\2\2"+
		"\u019a\u019b\7\u009f\2\2\u019b\u019d\7Y\2\2\u019c\u0199\3\2\2\2\u019c"+
		"\u019d\3\2\2\2\u019d\u019e\3\2\2\2\u019e\u019f\5\u00bc_\2\u019f\u01a0"+
		"\7\u008a\2\2\u01a0\u01a9\5\u00bc_\2\u01a1\u01a8\5<\37\2\u01a2\u01a8\5"+
		"\u00b6\\\2\u01a3\u01a8\5L\'\2\u01a4\u01a8\5$\23\2\u01a5\u01a6\7\u00ee"+
		"\2\2\u01a6\u01a8\5@!\2\u01a7\u01a1\3\2\2\2\u01a7\u01a2\3\2\2\2\u01a7\u01a3"+
		"\3\2\2\2\u01a7\u01a4\3\2\2\2\u01a7\u01a5\3\2\2\2\u01a8\u01ab\3\2\2\2\u01a9"+
		"\u01a7\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u0429\3\2\2\2\u01ab\u01a9\3\2"+
		"\2\2\u01ac\u01b1\5\36\20\2\u01ad\u01ae\7\4\2\2\u01ae\u01af\5\u00f6|\2"+
		"\u01af\u01b0\7\5\2\2\u01b0\u01b2\3\2\2\2\u01b1\u01ad\3\2\2\2\u01b1\u01b2"+
		"\3\2\2\2\u01b2\u01b4\3\2\2\2\u01b3\u01b5\5<\37\2\u01b4\u01b3\3\2\2\2\u01b4"+
		"\u01b5\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6\u01bb\5> \2\u01b7\u01b9\7\33"+
		"\2\2\u01b8\u01b7\3\2\2\2\u01b8\u01b9\3\2\2\2\u01b9\u01ba\3\2\2\2\u01ba"+
		"\u01bc\5(\25\2\u01bb\u01b8\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u0429\3\2"+
		"\2\2\u01bd\u01be\7\25\2\2\u01be\u01bf\7\u00eb\2\2\u01bf\u01c1\5\u00ba"+
		"^\2\u01c0\u01c2\5.\30\2\u01c1\u01c0\3\2\2\2\u01c1\u01c2\3\2\2\2\u01c2"+
		"\u01c3\3\2\2\2\u01c3\u01c4\7\66\2\2\u01c4\u01cc\7\u00e4\2\2\u01c5\u01cd"+
		"\5\u0114\u008b\2\u01c6\u01c7\7f\2\2\u01c7\u01c8\7\61\2\2\u01c8\u01cd\5"+
		"\u00a4S\2\u01c9\u01ca\7f\2\2\u01ca\u01cb\7\23\2\2\u01cb\u01cd\7\61\2\2"+
		"\u01cc\u01c5\3\2\2\2\u01cc\u01c6\3\2\2\2\u01cc\u01c9\3\2\2\2\u01cc\u01cd"+
		"\3\2\2\2\u01cd\u0429\3\2\2\2\u01ce\u01cf\7\25\2\2\u01cf\u01d2\7\u00ec"+
		"\2\2\u01d0\u01d1\t\5\2\2\u01d1\u01d3\5\u00ba^\2\u01d2\u01d0\3\2\2\2\u01d2"+
		"\u01d3\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d5\7\66\2\2\u01d5\u01d7\7"+
		"\u00e4\2\2\u01d6\u01d8\5\u0114\u008b\2\u01d7\u01d6\3\2\2\2\u01d7\u01d8"+
		"\3\2\2\2\u01d8\u0429\3\2\2\2\u01d9\u01da\7\24\2\2\u01da\u01db\7\u00eb"+
		"\2\2\u01db\u01dc\5\u00ba^\2\u01dc\u01dd\7\21\2\2\u01dd\u01de\t\6\2\2\u01de"+
		"\u01df\5\u00f2z\2\u01df\u0429\3\2\2\2\u01e0\u01e1\7\24\2\2\u01e1\u01e2"+
		"\7\u00eb\2\2\u01e2\u01e3\5\u00ba^\2\u01e3\u01e4\7\21\2\2\u01e4\u01e5\t"+
		"\6\2\2\u01e5\u01e6\7\4\2\2\u01e6\u01e7\5\u00f2z\2\u01e7\u01e8\7\5\2\2"+
		"\u01e8\u0429\3\2\2\2\u01e9\u01ea\7\24\2\2\u01ea\u01eb\7\u00eb\2\2\u01eb"+
		"\u01ec\5\u00ba^\2\u01ec\u01ed\7\u00c4\2\2\u01ed\u01ee\7\60\2\2\u01ee\u01ef"+
		"\5\u00ba^\2\u01ef\u01f0\7\u00f3\2\2\u01f0\u01f1\5\u0110\u0089\2\u01f1"+
		"\u0429\3\2\2\2\u01f2\u01f3\7\24\2\2\u01f3\u01f4\7\u00eb\2\2\u01f4\u01f5"+
		"\5\u00ba^\2\u01f5\u01f6\7R\2\2\u01f6\u01f7\t\6\2\2\u01f7\u01f8\7\4\2\2"+
		"\u01f8\u01f9\5\u00b8]\2\u01f9\u01fa\7\5\2\2\u01fa\u0429\3\2\2\2\u01fb"+
		"\u01fc\7\24\2\2\u01fc\u01fd\7\u00eb\2\2\u01fd\u01fe\5\u00ba^\2\u01fe\u01ff"+
		"\7R\2\2\u01ff\u0200\t\6\2\2\u0200\u0201\5\u00b8]\2\u0201\u0429\3\2\2\2"+
		"\u0202\u0203\7\24\2\2\u0203\u0204\t\7\2\2\u0204\u0205\5\u00ba^\2\u0205"+
		"\u0206\7\u00c4\2\2\u0206\u0207\7\u00f3\2\2\u0207\u0208\5\u00ba^\2\u0208"+
		"\u0429\3\2\2\2\u0209\u020a\7\24\2\2\u020a\u020b\t\7\2\2\u020b\u020c\5"+
		"\u00ba^\2\u020c\u020d\7\u00db\2\2\u020d\u020e\7\u00ee\2\2\u020e\u020f"+
		"\5@!\2\u020f\u0429\3\2\2\2\u0210\u0211\7\24\2\2\u0211\u0212\t\7\2\2\u0212"+
		"\u0213\5\u00ba^\2\u0213\u0214\7\u0105\2\2\u0214\u0217\7\u00ee\2\2\u0215"+
		"\u0216\7t\2\2\u0216\u0218\7Y\2\2\u0217\u0215\3\2\2\2\u0217\u0218\3\2\2"+
		"\2\u0218\u0219\3\2\2\2\u0219\u021a\5@!\2\u021a\u0429\3\2\2\2\u021b\u021c"+
		"\7\24\2\2\u021c\u021d\7\u00eb\2\2\u021d\u021e\5\u00ba^\2\u021e\u0220\t"+
		"\b\2\2\u021f\u0221\7\60\2\2\u0220\u021f\3\2\2\2\u0220\u0221\3\2\2\2\u0221"+
		"\u0222\3\2\2\2\u0222\u0224\5\u00ba^\2\u0223\u0225\5\u011c\u008f\2\u0224"+
		"\u0223\3\2\2\2\u0224\u0225\3\2\2\2\u0225\u0429\3\2\2\2\u0226\u0227\7\24"+
		"\2\2\u0227\u0228\7\u00eb\2\2\u0228\u022a\5\u00ba^\2\u0229\u022b\5.\30"+
		"\2\u022a\u0229\3\2\2\2\u022a\u022b\3\2\2\2\u022b\u022c\3\2\2\2\u022c\u022e"+
		"\7(\2\2\u022d\u022f\7\60\2\2\u022e\u022d\3\2\2\2\u022e\u022f\3\2\2\2\u022f"+
		"\u0230\3\2\2\2\u0230\u0231\5\u00ba^\2\u0231\u0233\5\u00f8}\2\u0232\u0234"+
		"\5\u00eex\2\u0233\u0232\3\2\2\2\u0233\u0234\3\2\2\2\u0234\u0429\3\2\2"+
		"\2\u0235\u0236\7\24\2\2\u0236\u0237\7\u00eb\2\2\u0237\u0239\5\u00ba^\2"+
		"\u0238\u023a\5.\30\2\u0239\u0238\3\2\2\2\u0239\u023a\3\2\2\2\u023a\u023b"+
		"\3\2\2\2\u023b\u023c\7\u00c6\2\2\u023c\u023d\7\61\2\2\u023d\u023e\7\4"+
		"\2\2\u023e\u023f\5\u00f2z\2\u023f\u0240\7\5\2\2\u0240\u0429\3\2\2\2\u0241"+
		"\u0242\7\24\2\2\u0242\u0243\7\u00eb\2\2\u0243\u0245\5\u00ba^\2\u0244\u0246"+
		"\5.\30\2\u0245\u0244\3\2\2\2\u0245\u0246\3\2\2\2\u0246\u0247\3\2\2\2\u0247"+
		"\u0248\7\u00db\2\2\u0248\u0249\7\u00d8\2\2\u0249\u024d\7\u0125\2\2\u024a"+
		"\u024b\7\u0110\2\2\u024b\u024c\7\u00d9\2\2\u024c\u024e\5@!\2\u024d\u024a"+
		"\3\2\2\2\u024d\u024e\3\2\2\2\u024e\u0429\3\2\2\2\u024f\u0250\7\24\2\2"+
		"\u0250\u0251\7\u00eb\2\2\u0251\u0253\5\u00ba^\2\u0252\u0254\5.\30\2\u0253"+
		"\u0252\3\2\2\2\u0253\u0254\3\2\2\2\u0254\u0255\3\2\2\2\u0255\u0256\7\u00db"+
		"\2\2\u0256\u0257\7\u00d9\2\2\u0257\u0258\5@!\2\u0258\u0429\3\2\2\2\u0259"+
		"\u025a\7\24\2\2\u025a\u025b\t\7\2\2\u025b\u025c\5\u00ba^\2\u025c\u0260"+
		"\7\21\2\2\u025d\u025e\7t\2\2\u025e\u025f\7\u009f\2\2\u025f\u0261\7Y\2"+
		"\2\u0260\u025d\3\2\2\2\u0260\u0261\3\2\2\2\u0261\u0263\3\2\2\2\u0262\u0264"+
		"\5,\27\2\u0263\u0262\3\2\2\2\u0264\u0265\3\2\2\2\u0265\u0263\3\2\2\2\u0265"+
		"\u0266\3\2\2\2\u0266\u0429\3\2\2\2\u0267\u0268\7\24\2\2\u0268\u0269\7"+
		"\u00eb\2\2\u0269\u026a\5\u00ba^\2\u026a\u026b\5.\30\2\u026b\u026c\7\u00c4"+
		"\2\2\u026c\u026d\7\u00f3\2\2\u026d\u026e\5.\30\2\u026e\u0429\3\2\2\2\u026f"+
		"\u0270\7\24\2\2\u0270\u0271\t\7\2\2\u0271\u0272\5\u00ba^\2\u0272\u0275"+
		"\7R\2\2\u0273\u0274\7t\2\2\u0274\u0276\7Y\2\2\u0275\u0273\3\2\2\2\u0275"+
		"\u0276\3\2\2\2\u0276\u0277\3\2\2\2\u0277\u027c\5.\30\2\u0278\u0279\7\6"+
		"\2\2\u0279\u027b\5.\30\2\u027a\u0278\3\2\2\2\u027b\u027e\3\2\2\2\u027c"+
		"\u027a\3\2\2\2\u027c\u027d\3\2\2\2\u027d\u0280\3\2\2\2\u027e\u027c\3\2"+
		"\2\2\u027f\u0281\7\u00bb\2\2\u0280\u027f\3\2\2\2\u0280\u0281\3\2\2\2\u0281"+
		"\u0429\3\2\2\2\u0282\u0283\7\24\2\2\u0283\u0284\7\u00eb\2\2\u0284\u0286"+
		"\5\u00ba^\2\u0285\u0287\5.\30\2\u0286\u0285\3\2\2\2\u0286\u0287\3\2\2"+
		"\2\u0287\u0288\3\2\2\2\u0288\u0289\7\u00db\2\2\u0289\u028a\5$\23\2\u028a"+
		"\u0429\3\2\2\2\u028b\u028c\7\24\2\2\u028c\u028d\7\u00eb\2\2\u028d\u028e"+
		"\5\u00ba^\2\u028e\u028f\7\u00c0\2\2\u028f\u0290\7\u00b2\2\2\u0290\u0429"+
		"\3\2\2\2\u0291\u0292\7R\2\2\u0292\u0295\7\u00eb\2\2\u0293\u0294\7t\2\2"+
		"\u0294\u0296\7Y\2\2\u0295\u0293\3\2\2\2\u0295\u0296\3\2\2\2\u0296\u0297"+
		"\3\2\2\2\u0297\u0299\5\u00ba^\2\u0298\u029a\7\u00bb\2\2\u0299\u0298\3"+
		"\2\2\2\u0299\u029a\3\2\2\2\u029a\u0429\3\2\2\2\u029b\u029c\7R\2\2\u029c"+
		"\u029f\7\u010b\2\2\u029d\u029e\7t\2\2\u029e\u02a0\7Y\2\2\u029f\u029d\3"+
		"\2\2\2\u029f\u02a0\3\2\2\2\u02a0\u02a1\3\2\2\2\u02a1\u0429\5\u00ba^\2"+
		"\u02a2\u02a5\7:\2\2\u02a3\u02a4\7\u00a7\2\2\u02a4\u02a6\7\u00c6\2\2\u02a5"+
		"\u02a3\3\2\2\2\u02a5\u02a6\3\2\2\2\u02a6\u02ab\3\2\2\2\u02a7\u02a9\7n"+
		"\2\2\u02a8\u02a7\3\2\2\2\u02a8\u02a9\3\2\2\2\u02a9\u02aa\3\2\2\2\u02aa"+
		"\u02ac\7\u00ef\2\2\u02ab\u02a8\3\2\2\2\u02ab\u02ac\3\2\2\2\u02ac\u02ad"+
		"\3\2\2\2\u02ad\u02b1\7\u010b\2\2\u02ae\u02af\7t\2\2\u02af\u02b0\7\u009f"+
		"\2\2\u02b0\u02b2\7Y\2\2\u02b1\u02ae\3\2\2\2\u02b1\u02b2\3\2\2\2\u02b2"+
		"\u02b3\3\2\2\2\u02b3\u02b5\5\u00ba^\2\u02b4\u02b6\5\u00aaV\2\u02b5\u02b4"+
		"\3\2\2\2\u02b5\u02b6\3\2\2\2\u02b6\u02bf\3\2\2\2\u02b7\u02be\5&\24\2\u02b8"+
		"\u02b9\7\u00b1\2\2\u02b9\u02ba\7\u00a3\2\2\u02ba\u02be\5\u00a2R\2\u02bb"+
		"\u02bc\7\u00ee\2\2\u02bc\u02be\5@!\2\u02bd\u02b7\3\2\2\2\u02bd\u02b8\3"+
		"\2\2\2\u02bd\u02bb\3\2\2\2\u02be\u02c1\3\2\2\2\u02bf\u02bd\3\2\2\2\u02bf"+
		"\u02c0\3\2\2\2\u02c0\u02c2\3\2\2\2\u02c1\u02bf\3\2\2\2\u02c2\u02c3\7\33"+
		"\2\2\u02c3\u02c4\5(\25\2\u02c4\u0429\3\2\2\2\u02c5\u02c8\7:\2\2\u02c6"+
		"\u02c7\7\u00a7\2\2\u02c7\u02c9\7\u00c6\2\2\u02c8\u02c6\3\2\2\2\u02c8\u02c9"+
		"\3\2\2\2\u02c9\u02cb\3\2\2\2\u02ca\u02cc\7n\2\2\u02cb\u02ca\3\2\2\2\u02cb"+
		"\u02cc\3\2\2\2\u02cc\u02cd\3\2\2\2\u02cd\u02ce\7\u00ef\2\2\u02ce\u02cf"+
		"\7\u010b\2\2\u02cf\u02d4\5\u00bc_\2\u02d0\u02d1\7\4\2\2\u02d1\u02d2\5"+
		"\u00f6|\2\u02d2\u02d3\7\5\2\2\u02d3\u02d5\3\2\2\2\u02d4\u02d0\3\2\2\2"+
		"\u02d4\u02d5\3\2\2\2\u02d5\u02d6\3\2\2\2\u02d6\u02d9\5<\37\2\u02d7\u02d8"+
		"\7\u00a6\2\2\u02d8\u02da\5@!\2\u02d9\u02d7\3\2\2\2\u02d9\u02da\3\2\2\2"+
		"\u02da\u0429\3\2\2\2\u02db\u02dc\7\24\2\2\u02dc\u02dd\7\u010b\2\2\u02dd"+
		"\u02df\5\u00ba^\2\u02de\u02e0\7\33\2\2\u02df\u02de\3\2\2\2\u02df\u02e0"+
		"\3\2\2\2\u02e0\u02e1\3\2\2\2\u02e1\u02e2\5(\25\2\u02e2\u0429\3\2\2\2\u02e3"+
		"\u02e6\7:\2\2\u02e4\u02e5\7\u00a7\2\2\u02e5\u02e7\7\u00c6\2\2\u02e6\u02e4"+
		"\3\2\2\2\u02e6\u02e7\3\2\2\2\u02e7\u02e9\3\2\2\2\u02e8\u02ea\7\u00ef\2"+
		"\2\u02e9\u02e8\3\2\2\2\u02e9\u02ea\3\2\2\2\u02ea\u02eb\3\2\2\2\u02eb\u02ef"+
		"\7l\2\2\u02ec\u02ed\7t\2\2\u02ed\u02ee\7\u009f\2\2\u02ee\u02f0\7Y\2\2"+
		"\u02ef\u02ec\3\2\2\2\u02ef\u02f0\3\2\2\2\u02f0\u02f1\3\2\2\2\u02f1\u02f2"+
		"\5\u00ba^\2\u02f2\u02f3\7\33\2\2\u02f3\u02fd\7\u0125\2\2\u02f4\u02f5\7"+
		"\u0109\2\2\u02f5\u02fa\5R*\2\u02f6\u02f7\7\6\2\2\u02f7\u02f9\5R*\2\u02f8"+
		"\u02f6\3\2\2\2\u02f9\u02fc\3\2\2\2\u02fa\u02f8\3\2\2\2\u02fa\u02fb\3\2"+
		"\2\2\u02fb\u02fe\3\2\2\2\u02fc\u02fa\3\2\2\2\u02fd\u02f4\3\2\2\2\u02fd"+
		"\u02fe\3\2\2\2\u02fe\u0429\3\2\2\2\u02ff\u0301\7R\2\2\u0300\u0302\7\u00ef"+
		"\2\2\u0301\u0300\3\2\2\2\u0301\u0302\3\2\2\2\u0302\u0303\3\2\2\2\u0303"+
		"\u0306\7l\2\2\u0304\u0305\7t\2\2\u0305\u0307\7Y\2\2\u0306\u0304\3\2\2"+
		"\2\u0306\u0307\3\2\2\2\u0307\u0308\3\2\2\2\u0308\u0429\5\u00ba^\2\u0309"+
		"\u030b\7Z\2\2\u030a\u030c\t\t\2\2\u030b\u030a\3\2\2\2\u030b\u030c\3\2"+
		"\2\2\u030c\u030d\3\2\2\2\u030d\u0429\5\24\13\2\u030e\u030f\7\u00de\2\2"+
		"\u030f\u0312\7\u00ec\2\2\u0310\u0311\t\5\2\2\u0311\u0313\5\u00ba^\2\u0312"+
		"\u0310\3\2\2\2\u0312\u0313\3\2\2\2\u0313\u0318\3\2\2\2\u0314\u0316\7\u008a"+
		"\2\2\u0315\u0314\3\2\2\2\u0315\u0316\3\2\2\2\u0316\u0317\3\2\2\2\u0317"+
		"\u0319\7\u0125\2\2\u0318\u0315\3\2\2\2\u0318\u0319\3\2\2\2\u0319\u0429"+
		"\3\2\2\2\u031a\u031b\7\u00de\2\2\u031b\u031c\7\u00eb\2\2\u031c\u031f\7"+
		"\\\2\2\u031d\u031e\t\5\2\2\u031e\u0320\5\u00ba^\2\u031f\u031d\3\2\2\2"+
		"\u031f\u0320\3\2\2\2\u0320\u0321\3\2\2\2\u0321\u0322\7\u008a\2\2\u0322"+
		"\u0324\7\u0125\2\2\u0323\u0325\5.\30\2\u0324\u0323\3\2\2\2\u0324\u0325"+
		"\3\2\2\2\u0325\u0429\3\2\2\2\u0326\u0327\7\u00de\2\2\u0327\u0328\7\u00ee"+
		"\2\2\u0328\u032d\5\u00ba^\2\u0329\u032a\7\4\2\2\u032a\u032b\5D#\2\u032b"+
		"\u032c\7\5\2\2\u032c\u032e\3\2\2\2\u032d\u0329\3\2\2\2\u032d\u032e\3\2"+
		"\2\2\u032e\u0429\3\2\2\2\u032f\u0330\7\u00de\2\2\u0330\u0331\7\61\2\2"+
		"\u0331\u0332\t\5\2\2\u0332\u0335\5\u00ba^\2\u0333\u0334\t\5\2\2\u0334"+
		"\u0336\5\u00ba^\2\u0335\u0333\3\2\2\2\u0335\u0336\3\2\2\2\u0336\u0429"+
		"\3\2\2\2\u0337\u0338\7\u00de\2\2\u0338\u033b\7\u010c\2\2\u0339\u033a\t"+
		"\5\2\2\u033a\u033c\5\u00ba^\2\u033b\u0339\3\2\2\2\u033b\u033c\3\2\2\2"+
		"\u033c\u0341\3\2\2\2\u033d\u033f\7\u008a\2\2\u033e\u033d\3\2\2\2\u033e"+
		"\u033f\3\2\2\2\u033f\u0340\3\2\2\2\u0340\u0342\7\u0125\2\2\u0341\u033e"+
		"\3\2\2\2\u0341\u0342\3\2\2\2\u0342\u0429\3\2\2\2\u0343\u0344\7\u00de\2"+
		"\2\u0344\u0345\7\u00b2\2\2\u0345\u0347\5\u00ba^\2\u0346\u0348\5.\30\2"+
		"\u0347\u0346\3\2\2\2\u0347\u0348\3\2\2\2\u0348\u0429\3\2\2\2\u0349\u034b"+
		"\7\u00de\2\2\u034a\u034c\5\u0114\u008b\2\u034b\u034a\3\2\2\2\u034b\u034c"+
		"\3\2\2\2\u034c\u034d\3\2\2\2\u034d\u0355\7m\2\2\u034e\u0350\7\u008a\2"+
		"\2\u034f\u034e\3\2\2\2\u034f\u0350\3\2\2\2\u0350\u0353\3\2\2\2\u0351\u0354"+
		"\5\u00ba^\2\u0352\u0354\7\u0125\2\2\u0353\u0351\3\2\2\2\u0353\u0352\3"+
		"\2\2\2\u0354\u0356\3\2\2\2\u0355\u034f\3\2\2\2\u0355\u0356\3\2\2\2\u0356"+
		"\u0429\3\2\2\2\u0357\u0358\7\u00de\2\2\u0358\u0359\7:\2\2\u0359\u035a"+
		"\7\u00eb\2\2\u035a\u035d\5\u00ba^\2\u035b\u035c\7\33\2\2\u035c\u035e\7"+
		"\u00d8\2\2\u035d\u035b\3\2\2\2\u035d\u035e\3\2\2\2\u035e\u0429\3\2\2\2"+
		"\u035f\u0360\7\u00de\2\2\u0360\u0361\7=\2\2\u0361\u0429\7\u009b\2\2\u0362"+
		"\u0363\t\n\2\2\u0363\u0365\7l\2\2\u0364\u0366\7\\\2\2\u0365\u0364\3\2"+
		"\2\2\u0365\u0366\3\2\2\2\u0366\u0367\3\2\2\2\u0367\u0429\5\64\33\2\u0368"+
		"\u0369\t\n\2\2\u0369\u036b\5\62\32\2\u036a\u036c\7\\\2\2\u036b\u036a\3"+
		"\2\2\2\u036b\u036c\3\2\2\2\u036c\u036d\3\2\2\2\u036d\u036e\5\u00ba^\2"+
		"\u036e\u0429\3\2\2\2\u036f\u0371\t\n\2\2\u0370\u0372\7\u00eb\2\2\u0371"+
		"\u0370\3\2\2\2\u0371\u0372\3\2\2\2\u0372\u0374\3\2\2\2\u0373\u0375\t\13"+
		"\2\2\u0374\u0373\3\2\2\2\u0374\u0375\3\2\2\2\u0375\u0376\3\2\2\2\u0376"+
		"\u0378\5\u00ba^\2\u0377\u0379\5.\30\2\u0378\u0377\3\2\2\2\u0378\u0379"+
		"\3\2\2\2\u0379\u037b\3\2\2\2\u037a\u037c\5\66\34\2\u037b\u037a\3\2\2\2"+
		"\u037b\u037c\3\2\2\2\u037c\u0429\3\2\2\2\u037d\u037f\t\n\2\2\u037e\u0380"+
		"\7\u00bc\2\2\u037f\u037e\3\2\2\2\u037f\u0380\3\2\2\2\u0380\u0381\3\2\2"+
		"\2\u0381\u0429\5(\25\2\u0382\u0383\7\62\2\2\u0383\u0384\7\u00a3\2\2\u0384"+
		"\u0385\5\62\32\2\u0385\u0386\5\u00ba^\2\u0386\u0387\7\u0081\2\2\u0387"+
		"\u0388\t\f\2\2\u0388\u0429\3\2\2\2\u0389\u038a\7\62\2\2\u038a\u038b\7"+
		"\u00a3\2\2\u038b\u038c\7\u00eb\2\2\u038c\u038d\5\u00ba^\2\u038d\u038e"+
		"\7\u0081\2\2\u038e\u038f\t\f\2\2\u038f\u0429\3\2\2\2\u0390\u0391\7\u00c3"+
		"\2\2\u0391\u0392\7\u00eb\2\2\u0392\u0429\5\u00ba^\2\u0393\u0394\7\u00c3"+
		"\2\2\u0394\u0395\7l\2\2\u0395\u0429\5\u00ba^\2\u0396\u039e\7\u00c3\2\2"+
		"\u0397\u039f\7\u0125\2\2\u0398\u039a\13\2\2\2\u0399\u0398\3\2\2\2\u039a"+
		"\u039d\3\2\2\2\u039b\u039c\3\2\2\2\u039b\u0399\3\2\2\2\u039c\u039f\3\2"+
		"\2\2\u039d\u039b\3\2\2\2\u039e\u0397\3\2\2\2\u039e\u039b\3\2\2\2\u039f"+
		"\u0429\3\2\2\2\u03a0\u03a2\7$\2\2\u03a1\u03a3\7\u0087\2\2\u03a2\u03a1"+
		"\3\2\2\2\u03a2\u03a3\3\2\2\2\u03a3\u03a4\3\2\2\2\u03a4\u03a5\7\u00eb\2"+
		"\2\u03a5\u03a8\5\u00ba^\2\u03a6\u03a7\7\u00a6\2\2\u03a7\u03a9\5@!\2\u03a8"+
		"\u03a6\3\2\2\2\u03a8\u03a9\3\2\2\2\u03a9\u03ae\3\2\2\2\u03aa\u03ac\7\33"+
		"\2\2\u03ab\u03aa\3\2\2\2\u03ab\u03ac\3\2\2\2\u03ac\u03ad\3\2\2\2\u03ad"+
		"\u03af\5(\25\2\u03ae\u03ab\3\2\2\2\u03ae\u03af\3\2\2\2\u03af\u0429\3\2"+
		"\2\2\u03b0\u03b1\7\u0100\2\2\u03b1\u03b4\7\u00eb\2\2\u03b2\u03b3\7t\2"+
		"\2\u03b3\u03b5\7Y\2\2\u03b4\u03b2\3\2\2\2\u03b4\u03b5\3\2\2\2\u03b5\u03b6"+
		"\3\2\2\2\u03b6\u0429\5\u00ba^\2\u03b7\u03b8\7*\2\2\u03b8\u0429\7$\2\2"+
		"\u03b9\u03ba\7\u008e\2\2\u03ba\u03bc\7C\2\2\u03bb\u03bd\7\u008f\2\2\u03bc"+
		"\u03bb\3\2\2\2\u03bc\u03bd\3\2\2\2\u03bd\u03be\3\2\2\2\u03be\u03bf\7{"+
		"\2\2\u03bf\u03c1\7\u0125\2\2\u03c0\u03c2\7\u00af\2\2\u03c1\u03c0\3\2\2"+
		"\2\u03c1\u03c2\3\2\2\2\u03c2\u03c3\3\2\2\2\u03c3\u03c4\7\u0080\2\2\u03c4"+
		"\u03c5\7\u00eb\2\2\u03c5\u03c7\5\u00ba^\2\u03c6\u03c8\5.\30\2\u03c7\u03c6"+
		"\3\2\2\2\u03c7\u03c8\3\2\2\2\u03c8\u0429\3\2\2\2\u03c9\u03ca\7\u00fb\2"+
		"\2\u03ca\u03cb\7\u00eb\2\2\u03cb\u03cd\5\u00ba^\2\u03cc\u03ce\5.\30\2"+
		"\u03cd\u03cc\3\2\2\2\u03cd\u03ce\3\2\2\2\u03ce\u0429\3\2\2\2\u03cf\u03d0"+
		"\7\u009a\2\2\u03d0\u03d1\7\u00c5\2\2\u03d1\u03d2\7\u00eb\2\2\u03d2\u03d5"+
		"\5\u00ba^\2\u03d3\u03d4\t\r\2\2\u03d4\u03d6\7\u00b2\2\2\u03d5\u03d3\3"+
		"\2\2\2\u03d5\u03d6\3\2\2\2\u03d6\u0429\3\2\2\2\u03d7\u03d8\t\16\2\2\u03d8"+
		"\u03dc\5\u0114\u008b\2\u03d9\u03db\13\2\2\2\u03da\u03d9\3\2\2\2\u03db"+
		"\u03de\3\2\2\2\u03dc\u03dd\3\2\2\2\u03dc\u03da\3\2\2\2\u03dd\u0429\3\2"+
		"\2\2\u03de\u03dc\3\2\2\2\u03df\u03e0\7\u00db\2\2\u03e0\u03e4\7\u00cd\2"+
		"\2\u03e1\u03e3\13\2\2\2\u03e2\u03e1\3\2\2\2\u03e3\u03e6\3\2\2\2\u03e4"+
		"\u03e5\3\2\2\2\u03e4\u03e2\3\2\2\2\u03e5\u0429\3\2\2\2\u03e6\u03e4\3\2"+
		"\2\2\u03e7\u03e8\7\u00db\2\2\u03e8\u03e9\7\u00f2\2\2\u03e9\u03ea\7\u0112"+
		"\2\2\u03ea\u0429\5\u00e2r\2\u03eb\u03ec\7\u00db\2\2\u03ec\u03ed\7\u00f2"+
		"\2\2\u03ed\u03ee\7\u0112\2\2\u03ee\u0429\t\17\2\2\u03ef\u03f0\7\u00db"+
		"\2\2\u03f0\u03f1\7\u00f2\2\2\u03f1\u03f5\7\u0112\2\2\u03f2\u03f4\13\2"+
		"\2\2\u03f3\u03f2\3\2\2\2\u03f4\u03f7\3\2\2\2\u03f5\u03f6\3\2\2\2\u03f5"+
		"\u03f3\3\2\2\2\u03f6\u0429\3\2\2\2\u03f7\u03f5\3\2\2\2\u03f8\u03f9\7\u00db"+
		"\2\2\u03f9\u03fa\5\26\f\2\u03fa\u03fb\7\u0113\2\2\u03fb\u03fc\5\30\r\2"+
		"\u03fc\u0429\3\2\2\2\u03fd\u03fe\7\u00db\2\2\u03fe\u0406\5\26\f\2\u03ff"+
		"\u0403\7\u0113\2\2\u0400\u0402\13\2\2\2\u0401\u0400\3\2\2\2\u0402\u0405"+
		"\3\2\2\2\u0403\u0404\3\2\2\2\u0403\u0401\3\2\2\2\u0404\u0407\3\2\2\2\u0405"+
		"\u0403\3\2\2\2\u0406\u03ff\3\2\2\2\u0406\u0407\3\2\2\2\u0407\u0429\3\2"+
		"\2\2\u0408\u040c\7\u00db\2\2\u0409\u040b\13\2\2\2\u040a\u0409\3\2\2\2"+
		"\u040b\u040e\3\2\2\2\u040c\u040d\3\2\2\2\u040c\u040a\3\2\2\2\u040d\u040f"+
		"\3\2\2\2\u040e\u040c\3\2\2\2\u040f\u0410\7\u0113\2\2\u0410\u0429\5\30"+
		"\r\2\u0411\u0415\7\u00db\2\2\u0412\u0414\13\2\2\2\u0413\u0412\3\2\2\2"+
		"\u0414\u0417\3\2\2\2\u0415\u0416\3\2\2\2\u0415\u0413\3\2\2\2\u0416\u0429"+
		"\3\2\2\2\u0417\u0415\3\2\2\2\u0418\u0419\7\u00c7\2\2\u0419\u0429\5\26"+
		"\f\2\u041a\u041e\7\u00c7\2\2\u041b\u041d\13\2\2\2\u041c\u041b\3\2\2\2"+
		"\u041d\u0420\3\2\2\2\u041e\u041f\3\2\2\2\u041e\u041c\3\2\2\2\u041f\u0429"+
		"\3\2\2\2\u0420\u041e\3\2\2\2\u0421\u0425\5\32\16\2\u0422\u0424\13\2\2"+
		"\2\u0423\u0422\3\2\2\2\u0424\u0427\3\2\2\2\u0425\u0426\3\2\2\2\u0425\u0423"+
		"\3\2\2\2\u0426\u0429\3\2\2\2\u0427\u0425\3\2\2\2\u0428\u0147\3\2\2\2\u0428"+
		"\u0149\3\2\2\2\u0428\u014c\3\2\2\2\u0428\u0151\3\2\2\2\u0428\u0163\3\2"+
		"\2\2\u0428\u016a\3\2\2\2\u0428\u0170\3\2\2\2\u0428\u017a\3\2\2\2\u0428"+
		"\u0186\3\2\2\2\u0428\u0197\3\2\2\2\u0428\u01ac\3\2\2\2\u0428\u01bd\3\2"+
		"\2\2\u0428\u01ce\3\2\2\2\u0428\u01d9\3\2\2\2\u0428\u01e0\3\2\2\2\u0428"+
		"\u01e9\3\2\2\2\u0428\u01f2\3\2\2\2\u0428\u01fb\3\2\2\2\u0428\u0202\3\2"+
		"\2\2\u0428\u0209\3\2\2\2\u0428\u0210\3\2\2\2\u0428\u021b\3\2\2\2\u0428"+
		"\u0226\3\2\2\2\u0428\u0235\3\2\2\2\u0428\u0241\3\2\2\2\u0428\u024f\3\2"+
		"\2\2\u0428\u0259\3\2\2\2\u0428\u0267\3\2\2\2\u0428\u026f\3\2\2\2\u0428"+
		"\u0282\3\2\2\2\u0428\u028b\3\2\2\2\u0428\u0291\3\2\2\2\u0428\u029b\3\2"+
		"\2\2\u0428\u02a2\3\2\2\2\u0428\u02c5\3\2\2\2\u0428\u02db\3\2\2\2\u0428"+
		"\u02e3\3\2\2\2\u0428\u02ff\3\2\2\2\u0428\u0309\3\2\2\2\u0428\u030e\3\2"+
		"\2\2\u0428\u031a\3\2\2\2\u0428\u0326\3\2\2\2\u0428\u032f\3\2\2\2\u0428"+
		"\u0337\3\2\2\2\u0428\u0343\3\2\2\2\u0428\u0349\3\2\2\2\u0428\u0357\3\2"+
		"\2\2\u0428\u035f\3\2\2\2\u0428\u0362\3\2\2\2\u0428\u0368\3\2\2\2\u0428"+
		"\u036f\3\2\2\2\u0428\u037d\3\2\2\2\u0428\u0382\3\2\2\2\u0428\u0389\3\2"+
		"\2\2\u0428\u0390\3\2\2\2\u0428\u0393\3\2\2\2\u0428\u0396\3\2\2\2\u0428"+
		"\u03a0\3\2\2\2\u0428\u03b0\3\2\2\2\u0428\u03b7\3\2\2\2\u0428\u03b9\3\2"+
		"\2\2\u0428\u03c9\3\2\2\2\u0428\u03cf\3\2\2\2\u0428\u03d7\3\2\2\2\u0428"+
		"\u03df\3\2\2\2\u0428\u03e7\3\2\2\2\u0428\u03eb\3\2\2\2\u0428\u03ef\3\2"+
		"\2\2\u0428\u03f8\3\2\2\2\u0428\u03fd\3\2\2\2\u0428\u0408\3\2\2\2\u0428"+
		"\u0411\3\2\2\2\u0428\u0418\3\2\2\2\u0428\u041a\3\2\2\2\u0428\u0421\3\2"+
		"\2\2\u0429\25\3\2\2\2\u042a\u042b\5\u0118\u008d\2\u042b\27\3\2\2\2\u042c"+
		"\u042d\5\u0118\u008d\2\u042d\31\3\2\2\2\u042e\u042f\7:\2\2\u042f\u04d7"+
		"\7\u00cd\2\2\u0430\u0431\7R\2\2\u0431\u04d7\7\u00cd\2\2\u0432\u0434\7"+
		"o\2\2\u0433\u0435\7\u00cd\2\2\u0434\u0433\3\2\2\2\u0434\u0435\3\2\2\2"+
		"\u0435\u04d7\3\2\2\2\u0436\u0438\7\u00ca\2\2\u0437\u0439\7\u00cd\2\2\u0438"+
		"\u0437\3\2\2\2\u0438\u0439\3\2\2\2\u0439\u04d7\3\2\2\2\u043a\u043b\7\u00de"+
		"\2\2\u043b\u04d7\7o\2\2\u043c\u043d\7\u00de\2\2\u043d\u043f\7\u00cd\2"+
		"\2\u043e\u0440\7o\2\2\u043f\u043e\3\2\2\2\u043f\u0440\3\2\2\2\u0440\u04d7"+
		"\3\2\2\2\u0441\u0442\7\u00de\2\2\u0442\u04d7\7\u00b9\2\2\u0443\u0444\7"+
		"\u00de\2\2\u0444\u04d7\7\u00ce\2\2\u0445\u0446\7\u00de\2\2\u0446\u0447"+
		"\7=\2\2\u0447\u04d7\7\u00ce\2\2\u0448\u0449\7[\2\2\u0449\u04d7\7\u00eb"+
		"\2\2\u044a\u044b\7v\2\2\u044b\u04d7\7\u00eb\2\2\u044c\u044d\7\u00de\2"+
		"\2\u044d\u04d7\7\65\2\2\u044e\u044f\7\u00de\2\2\u044f\u0450\7:\2\2\u0450"+
		"\u04d7\7\u00eb\2\2\u0451\u0452\7\u00de\2\2\u0452\u04d7\7\u00f7\2\2\u0453"+
		"\u0454\7\u00de\2\2\u0454\u04d7\7y\2\2\u0455\u0456\7\u00de\2\2\u0456\u04d7"+
		"\7\u0092\2\2\u0457\u0458\7:\2\2\u0458\u04d7\7x\2\2\u0459\u045a\7R\2\2"+
		"\u045a\u04d7\7x\2\2\u045b\u045c\7\24\2\2\u045c\u04d7\7x\2\2\u045d\u045e"+
		"\7\u0091\2\2\u045e\u04d7\7\u00eb\2\2\u045f\u0460\7\u0091\2\2\u0460\u04d7"+
		"\7D\2\2\u0461\u0462\7\u0104\2\2\u0462\u04d7\7\u00eb\2\2\u0463\u0464\7"+
		"\u0104\2\2\u0464\u04d7\7D\2\2\u0465\u0466\7:\2\2\u0466\u0467\7\u00ef\2"+
		"\2\u0467\u04d7\7\u0094\2\2\u0468\u0469\7R\2\2\u0469\u046a\7\u00ef\2\2"+
		"\u046a\u04d7\7\u0094\2\2\u046b\u046c\7\24\2\2\u046c\u046d\7\u00eb\2\2"+
		"\u046d\u046e\5\u00bc_\2\u046e\u046f\7\u009f\2\2\u046f\u0470\7,\2\2\u0470"+
		"\u04d7\3\2\2\2\u0471\u0472\7\24\2\2\u0472\u0473\7\u00eb\2\2\u0473\u0474"+
		"\5\u00bc_\2\u0474\u0475\7,\2\2\u0475\u0476\7#\2\2\u0476\u04d7\3\2\2\2"+
		"\u0477\u0478\7\24\2\2\u0478\u0479\7\u00eb\2\2\u0479\u047a\5\u00bc_\2\u047a"+
		"\u047b\7\u009f\2\2\u047b\u047c\7\u00e2\2\2\u047c\u04d7\3\2\2\2\u047d\u047e"+
		"\7\24\2\2\u047e\u047f\7\u00eb\2\2\u047f\u0480\5\u00bc_\2\u0480\u0481\7"+
		"\u00df\2\2\u0481\u0482\7#\2\2\u0482\u04d7\3\2\2\2\u0483\u0484\7\24\2\2"+
		"\u0484\u0485\7\u00eb\2\2\u0485\u0486\5\u00bc_\2\u0486\u0487\7\u009f\2"+
		"\2\u0487\u0488\7\u00df\2\2\u0488\u04d7\3\2\2\2\u0489\u048a\7\24\2\2\u048a"+
		"\u048b\7\u00eb\2\2\u048b\u048c\5\u00bc_\2\u048c\u048d\7\u009f\2\2\u048d"+
		"\u048e\7\u00e5\2\2\u048e\u048f\7\33\2\2\u048f\u0490\7M\2\2\u0490\u04d7"+
		"\3\2\2\2\u0491\u0492\7\24\2\2\u0492\u0493\7\u00eb\2\2\u0493\u0494\5\u00bc"+
		"_\2\u0494\u0495\7\u00db\2\2\u0495\u0496\7\u00df\2\2\u0496\u0497\7\u0090"+
		"\2\2\u0497\u04d7\3\2\2\2\u0498\u0499\7\24\2\2\u0499\u049a\7\u00eb\2\2"+
		"\u049a\u049b\5\u00bc_\2\u049b\u049c\7X\2\2\u049c\u049d\7\u00b0\2\2\u049d"+
		"\u04d7\3\2\2\2\u049e\u049f\7\24\2\2\u049f\u04a0\7\u00eb\2\2\u04a0\u04a1"+
		"\5\u00bc_\2\u04a1\u04a2\7\31\2\2\u04a2\u04a3\7\u00b0\2\2\u04a3\u04d7\3"+
		"\2\2\2\u04a4\u04a5\7\24\2\2\u04a5\u04a6\7\u00eb\2\2\u04a6\u04a7\5\u00bc"+
		"_\2\u04a7\u04a8\7\u00fe\2\2\u04a8\u04a9\7\u00b0\2\2\u04a9\u04d7\3\2\2"+
		"\2\u04aa\u04ab\7\24\2\2\u04ab\u04ac\7\u00eb\2\2\u04ac\u04ad\5\u00bc_\2"+
		"\u04ad\u04ae\7\u00f4\2\2\u04ae\u04d7\3\2\2\2\u04af\u04b0\7\24\2\2\u04b0"+
		"\u04b1\7\u00eb\2\2\u04b1\u04b3\5\u00bc_\2\u04b2\u04b4\5.\30\2\u04b3\u04b2"+
		"\3\2\2\2\u04b3\u04b4\3\2\2\2\u04b4\u04b5\3\2\2\2\u04b5\u04b6\7\64\2\2"+
		"\u04b6\u04d7\3\2\2\2\u04b7\u04b8\7\24\2\2\u04b8\u04b9\7\u00eb\2\2\u04b9"+
		"\u04bb\5\u00bc_\2\u04ba\u04bc\5.\30\2\u04bb\u04ba\3\2\2\2\u04bb\u04bc"+
		"\3\2\2\2\u04bc\u04bd\3\2\2\2\u04bd\u04be\7\67\2\2\u04be\u04d7\3\2\2\2"+
		"\u04bf\u04c0\7\24\2\2\u04c0\u04c1\7\u00eb\2\2\u04c1\u04c3\5\u00bc_\2\u04c2"+
		"\u04c4\5.\30\2\u04c3\u04c2\3\2\2\2\u04c3\u04c4\3\2\2\2\u04c4\u04c5\3\2"+
		"\2\2\u04c5\u04c6\7\u00db\2\2\u04c6\u04c7\7c\2\2\u04c7\u04d7\3\2\2\2\u04c8"+
		"\u04c9\7\24\2\2\u04c9\u04ca\7\u00eb\2\2\u04ca\u04cc\5\u00bc_\2\u04cb\u04cd"+
		"\5.\30\2\u04cc\u04cb\3\2\2\2\u04cc\u04cd\3\2\2\2\u04cd\u04ce\3\2\2\2\u04ce"+
		"\u04cf\7\u00c6\2\2\u04cf\u04d0\7\61\2\2\u04d0\u04d7\3\2\2\2\u04d1\u04d2"+
		"\7\u00e3\2\2\u04d2\u04d7\7\u00f6\2\2\u04d3\u04d7\7\63\2\2\u04d4\u04d7"+
		"\7\u00cf\2\2\u04d5\u04d7\7L\2\2\u04d6\u042e\3\2\2\2\u04d6\u0430\3\2\2"+
		"\2\u04d6\u0432\3\2\2\2\u04d6\u0436\3\2\2\2\u04d6\u043a\3\2\2\2\u04d6\u043c"+
		"\3\2\2\2\u04d6\u0441\3\2\2\2\u04d6\u0443\3\2\2\2\u04d6\u0445\3\2\2\2\u04d6"+
		"\u0448\3\2\2\2\u04d6\u044a\3\2\2\2\u04d6\u044c\3\2\2\2\u04d6\u044e\3\2"+
		"\2\2\u04d6\u0451\3\2\2\2\u04d6\u0453\3\2\2\2\u04d6\u0455\3\2\2\2\u04d6"+
		"\u0457\3\2\2\2\u04d6\u0459\3\2\2\2\u04d6\u045b\3\2\2\2\u04d6\u045d\3\2"+
		"\2\2\u04d6\u045f\3\2\2\2\u04d6\u0461\3\2\2\2\u04d6\u0463\3\2\2\2\u04d6"+
		"\u0465\3\2\2\2\u04d6\u0468\3\2\2\2\u04d6\u046b\3\2\2\2\u04d6\u0471\3\2"+
		"\2\2\u04d6\u0477\3\2\2\2\u04d6\u047d\3\2\2\2\u04d6\u0483\3\2\2\2\u04d6"+
		"\u0489\3\2\2\2\u04d6\u0491\3\2\2\2\u04d6\u0498\3\2\2\2\u04d6\u049e\3\2"+
		"\2\2\u04d6\u04a4\3\2\2\2\u04d6\u04aa\3\2\2\2\u04d6\u04af\3\2\2\2\u04d6"+
		"\u04b7\3\2\2\2\u04d6\u04bf\3\2\2\2\u04d6\u04c8\3\2\2\2\u04d6\u04d1\3\2"+
		"\2\2\u04d6\u04d3\3\2\2\2\u04d6\u04d4\3\2\2\2\u04d6\u04d5\3\2\2\2\u04d7"+
		"\33\3\2\2\2\u04d8\u04da\7:\2\2\u04d9\u04db\7\u00ef\2\2\u04da\u04d9\3\2"+
		"\2\2\u04da\u04db\3\2\2\2\u04db\u04dd\3\2\2\2\u04dc\u04de\7]\2\2\u04dd"+
		"\u04dc\3\2\2\2\u04dd\u04de\3\2\2\2\u04de\u04df\3\2\2\2\u04df\u04e3\7\u00eb"+
		"\2\2\u04e0\u04e1\7t\2\2\u04e1\u04e2\7\u009f\2\2\u04e2\u04e4\7Y\2\2\u04e3"+
		"\u04e0\3\2\2\2\u04e3\u04e4\3\2\2\2\u04e4\u04e5\3\2\2\2\u04e5\u04e6\5\u00ba"+
		"^\2\u04e6\35\3\2\2\2\u04e7\u04e8\7:\2\2\u04e8\u04ea\7\u00a7\2\2\u04e9"+
		"\u04e7\3\2\2\2\u04e9\u04ea\3\2\2\2\u04ea\u04eb\3\2\2\2\u04eb\u04ec\7\u00c6"+
		"\2\2\u04ec\u04ed\7\u00eb\2\2\u04ed\u04ee\5\u00ba^\2\u04ee\37\3\2\2\2\u04ef"+
		"\u04f0\7,\2\2\u04f0\u04f1\7#\2\2\u04f1\u04f5\5\u00a2R\2\u04f2\u04f3\7"+
		"\u00e2\2\2\u04f3\u04f4\7#\2\2\u04f4\u04f6\5\u00a6T\2\u04f5\u04f2\3\2\2"+
		"\2\u04f5\u04f6\3\2\2\2\u04f6\u04f7\3\2\2\2\u04f7\u04f8\7\u0080\2\2\u04f8"+
		"\u04f9\7\u0129\2\2\u04f9\u04fa\7\"\2\2\u04fa!\3\2\2\2\u04fb\u04fc\7\u00df"+
		"\2\2\u04fc\u04fd\7#\2\2\u04fd\u04fe\5\u00a2R\2\u04fe\u0501\7\u00a3\2\2"+
		"\u04ff\u0502\5H%\2\u0500\u0502\5J&\2\u0501\u04ff\3\2\2\2\u0501\u0500\3"+
		"\2\2\2\u0502\u0506\3\2\2\2\u0503\u0504\7\u00e5\2\2\u0504\u0505\7\33\2"+
		"\2\u0505\u0507\7M\2\2\u0506\u0503\3\2\2\2\u0506\u0507\3\2\2\2\u0507#\3"+
		"\2\2\2\u0508\u0509\7\u0090\2\2\u0509\u050a\7\u0125\2\2\u050a%\3\2\2\2"+
		"\u050b\u050c\7\62\2\2\u050c\u050d\7\u0125\2\2\u050d\'\3\2\2\2\u050e\u0510"+
		"\58\35\2\u050f\u050e\3\2\2\2\u050f\u0510\3\2\2\2\u0510\u0511\3\2\2\2\u0511"+
		"\u0512\5Z.\2\u0512\u0513\5V,\2\u0513)\3\2\2\2\u0514\u0515\7}\2\2\u0515"+
		"\u0517\7\u00af\2\2\u0516\u0518\7\u00eb\2\2\u0517\u0516\3\2\2\2\u0517\u0518"+
		"\3\2\2\2\u0518\u0519\3\2\2\2\u0519\u0520\5\u00ba^\2\u051a\u051e\5.\30"+
		"\2\u051b\u051c\7t\2\2\u051c\u051d\7\u009f\2\2\u051d\u051f\7Y\2\2\u051e"+
		"\u051b\3\2\2\2\u051e\u051f\3\2\2\2\u051f\u0521\3\2\2\2\u0520\u051a\3\2"+
		"\2\2\u0520\u0521\3\2\2\2\u0521\u0523\3\2\2\2\u0522\u0524\5\u00a2R\2\u0523"+
		"\u0522\3\2\2\2\u0523\u0524\3\2\2\2\u0524\u0552\3\2\2\2\u0525\u0526\7}"+
		"\2\2\u0526\u0528\7\u0080\2\2\u0527\u0529\7\u00eb\2\2\u0528\u0527\3\2\2"+
		"\2\u0528\u0529\3\2\2\2\u0529\u052a\3\2\2\2\u052a\u052c\5\u00ba^\2\u052b"+
		"\u052d\5.\30\2\u052c\u052b\3\2\2\2\u052c\u052d\3\2\2\2\u052d\u0531\3\2"+
		"\2\2\u052e\u052f\7t\2\2\u052f\u0530\7\u009f\2\2\u0530\u0532\7Y\2\2\u0531"+
		"\u052e\3\2\2\2\u0531\u0532\3\2\2\2\u0532\u0534\3\2\2\2\u0533\u0535\5\u00a2"+
		"R\2\u0534\u0533\3\2\2\2\u0534\u0535\3\2\2\2\u0535\u0552\3\2\2\2\u0536"+
		"\u0537\7}\2\2\u0537\u0539\7\u00af\2\2\u0538\u053a\7\u008f\2\2\u0539\u0538"+
		"\3\2\2\2\u0539\u053a\3\2\2\2\u053a\u053b\3\2\2\2\u053b\u053c\7N\2\2\u053c"+
		"\u053e\7\u0125\2\2\u053d\u053f\5\u00b6\\\2\u053e\u053d\3\2\2\2\u053e\u053f"+
		"\3\2\2\2\u053f\u0541\3\2\2\2\u0540\u0542\5L\'\2\u0541\u0540\3\2\2\2\u0541"+
		"\u0542\3\2\2\2\u0542\u0552\3\2\2\2\u0543\u0544\7}\2\2\u0544\u0546\7\u00af"+
		"\2\2\u0545\u0547\7\u008f\2\2\u0546\u0545\3\2\2\2\u0546\u0547\3\2\2\2\u0547"+
		"\u0548\3\2\2\2\u0548\u054a\7N\2\2\u0549\u054b\7\u0125\2\2\u054a\u0549"+
		"\3\2\2\2\u054a\u054b\3\2\2\2\u054b\u054c\3\2\2\2\u054c\u054f\5<\37\2\u054d"+
		"\u054e\7\u00a6\2\2\u054e\u0550\5@!\2\u054f\u054d\3\2\2\2\u054f\u0550\3"+
		"\2\2\2\u0550\u0552\3\2\2\2\u0551\u0514\3\2\2\2\u0551\u0525\3\2\2\2\u0551"+
		"\u0536\3\2\2\2\u0551\u0543\3\2\2\2\u0552+\3\2\2\2\u0553\u0555\5.\30\2"+
		"\u0554\u0556\5$\23\2\u0555\u0554\3\2\2\2\u0555\u0556\3\2\2\2\u0556-\3"+
		"\2\2\2\u0557\u0558\7\u00b0\2\2\u0558\u0559\7\4\2\2\u0559\u055e\5\60\31"+
		"\2\u055a\u055b\7\6\2\2\u055b\u055d\5\60\31\2\u055c\u055a\3\2\2\2\u055d"+
		"\u0560\3\2\2\2\u055e\u055c\3\2\2\2\u055e\u055f\3\2\2\2\u055f\u0561\3\2"+
		"\2\2\u0560\u055e\3\2\2\2\u0561\u0562\7\5\2\2\u0562/\3\2\2\2\u0563\u0566"+
		"\5\u0114\u008b\2\u0564\u0565\7\u0113\2\2\u0565\u0567\5\u00d8m\2\u0566"+
		"\u0564\3\2\2\2\u0566\u0567\3\2\2\2\u0567\61\3\2\2\2\u0568\u0569\t\20\2"+
		"\2\u0569\63\3\2\2\2\u056a\u0570\5\u010e\u0088\2\u056b\u0570\7\u0125\2"+
		"\2\u056c\u0570\5\u00dan\2\u056d\u0570\5\u00dco\2\u056e\u0570\5\u00dep"+
		"\2\u056f\u056a\3\2\2\2\u056f\u056b\3\2\2\2\u056f\u056c\3\2\2\2\u056f\u056d"+
		"\3\2\2\2\u056f\u056e\3\2\2\2\u0570\65\3\2\2\2\u0571\u0576\5\u0114\u008b"+
		"\2\u0572\u0573\7\7\2\2\u0573\u0575\5\u0114\u008b\2\u0574\u0572\3\2\2\2"+
		"\u0575\u0578\3\2\2\2\u0576\u0574\3\2\2\2\u0576\u0577\3\2\2\2\u0577\67"+
		"\3\2\2\2\u0578\u0576\3\2\2\2\u0579\u057a\7\u0110\2\2\u057a\u057f\5:\36"+
		"\2\u057b\u057c\7\6\2\2\u057c\u057e\5:\36\2\u057d\u057b\3\2\2\2\u057e\u0581"+
		"\3\2\2\2\u057f\u057d\3\2\2\2\u057f\u0580\3\2\2\2\u05809\3\2\2\2\u0581"+
		"\u057f\3\2\2\2\u0582\u0584\5\u0110\u0089\2\u0583\u0585\5\u00a2R\2\u0584"+
		"\u0583\3\2\2\2\u0584\u0585\3\2\2\2\u0585\u0587\3\2\2\2\u0586\u0588\7\33"+
		"\2\2\u0587\u0586\3\2\2\2\u0587\u0588\3\2\2\2\u0588\u0589\3\2\2\2\u0589"+
		"\u058a\7\4\2\2\u058a\u058b\5(\25\2\u058b\u058c\7\5\2\2\u058c;\3\2\2\2"+
		"\u058d\u058e\7\u0109\2\2\u058e\u058f\5\u00ba^\2\u058f=\3\2\2\2\u0590\u0591"+
		"\7\u00a6\2\2\u0591\u059e\5@!\2\u0592\u0593\7\u00b1\2\2\u0593\u0594\7#"+
		"\2\2\u0594\u059e\5\u00c4c\2\u0595\u059e\5\"\22\2\u0596\u059e\5 \21\2\u0597"+
		"\u059e\5\u00b6\\\2\u0598\u059e\5L\'\2\u0599\u059e\5$\23\2\u059a\u059e"+
		"\5&\24\2\u059b\u059c\7\u00ee\2\2\u059c\u059e\5@!\2\u059d\u0590\3\2\2\2"+
		"\u059d\u0592\3\2\2\2\u059d\u0595\3\2\2\2\u059d\u0596\3\2\2\2\u059d\u0597"+
		"\3\2\2\2\u059d\u0598\3\2\2\2\u059d\u0599\3\2\2\2\u059d\u059a\3\2\2\2\u059d"+
		"\u059b\3\2\2\2\u059e\u05a1\3\2\2\2\u059f\u059d\3\2\2\2\u059f\u05a0\3\2"+
		"\2\2\u05a0?\3\2\2\2\u05a1\u059f\3\2\2\2\u05a2\u05a3\7\4\2\2\u05a3\u05a8"+
		"\5B\"\2\u05a4\u05a5\7\6\2\2\u05a5\u05a7\5B\"\2\u05a6\u05a4\3\2\2\2\u05a7"+
		"\u05aa\3\2\2\2\u05a8\u05a6\3\2\2\2\u05a8\u05a9\3\2\2\2\u05a9\u05ab\3\2"+
		"\2\2\u05aa\u05a8\3\2\2\2\u05ab\u05ac\7\5\2\2\u05acA\3\2\2\2\u05ad\u05b2"+
		"\5D#\2\u05ae\u05b0\7\u0113\2\2\u05af\u05ae\3\2\2\2\u05af\u05b0\3\2\2\2"+
		"\u05b0\u05b1\3\2\2\2\u05b1\u05b3\5F$\2\u05b2\u05af\3\2\2\2\u05b2\u05b3"+
		"\3\2\2\2\u05b3C\3\2\2\2\u05b4\u05b9\5\u0114\u008b\2\u05b5\u05b6\7\7\2"+
		"\2\u05b6\u05b8\5\u0114\u008b\2\u05b7\u05b5\3\2\2\2\u05b8\u05bb\3\2\2\2"+
		"\u05b9\u05b7\3\2\2\2\u05b9\u05ba\3\2\2\2\u05ba\u05be\3\2\2\2\u05bb\u05b9"+
		"\3\2\2\2\u05bc\u05be\7\u0125\2\2\u05bd\u05b4\3\2\2\2\u05bd\u05bc\3\2\2"+
		"\2\u05beE\3\2\2\2\u05bf\u05c4\7\u0129\2\2\u05c0\u05c4\7\u012b\2\2\u05c1"+
		"\u05c4\5\u00e0q\2\u05c2\u05c4\7\u0125\2\2\u05c3\u05bf\3\2\2\2\u05c3\u05c0"+
		"\3\2\2\2\u05c3\u05c1\3\2\2\2\u05c3\u05c2\3\2\2\2\u05c4G\3\2\2\2\u05c5"+
		"\u05c6\7\4\2\2\u05c6\u05cb\5\u00d8m\2\u05c7\u05c8\7\6\2\2\u05c8\u05ca"+
		"\5\u00d8m\2\u05c9\u05c7\3\2\2\2\u05ca\u05cd\3\2\2\2\u05cb\u05c9\3\2\2"+
		"\2\u05cb\u05cc\3\2\2\2\u05cc\u05ce\3\2\2\2\u05cd\u05cb\3\2\2\2\u05ce\u05cf"+
		"\7\5\2\2\u05cfI\3\2\2\2\u05d0\u05d1\7\4\2\2\u05d1\u05d6\5H%\2\u05d2\u05d3"+
		"\7\6\2\2\u05d3\u05d5\5H%\2\u05d4\u05d2\3\2\2\2\u05d5\u05d8\3\2\2\2\u05d6"+
		"\u05d4\3\2\2\2\u05d6\u05d7\3\2\2\2\u05d7\u05d9\3\2\2\2\u05d8\u05d6\3\2"+
		"\2\2\u05d9\u05da\7\5\2\2\u05daK\3\2\2\2\u05db\u05dc\7\u00e5\2\2\u05dc"+
		"\u05dd\7\33\2\2\u05dd\u05e2\5N(\2\u05de\u05df\7\u00e5\2\2\u05df\u05e0"+
		"\7#\2\2\u05e0\u05e2\5P)\2\u05e1\u05db\3\2\2\2\u05e1\u05de\3\2\2\2\u05e2"+
		"M\3\2\2\2\u05e3\u05e4\7|\2\2\u05e4\u05e5\7\u0125\2\2\u05e5\u05e6\7\u00ab"+
		"\2\2\u05e6\u05e9\7\u0125\2\2\u05e7\u05e9\5\u0114\u008b\2\u05e8\u05e3\3"+
		"\2\2\2\u05e8\u05e7\3\2\2\2\u05e9O\3\2\2\2\u05ea\u05ee\7\u0125\2\2\u05eb"+
		"\u05ec\7\u0110\2\2\u05ec\u05ed\7\u00d9\2\2\u05ed\u05ef\5@!\2\u05ee\u05eb"+
		"\3\2\2\2\u05ee\u05ef\3\2\2\2\u05efQ\3\2\2\2\u05f0\u05f1\5\u0114\u008b"+
		"\2\u05f1\u05f2\7\u0125\2\2\u05f2S\3\2\2\2\u05f3\u05f4\5*\26\2\u05f4\u05f5"+
		"\5Z.\2\u05f5\u05f6\5V,\2\u05f6\u0627\3\2\2\2\u05f7\u05f9\5\u0080A\2\u05f8"+
		"\u05fa\5X-\2\u05f9\u05f8\3\2\2\2\u05fa\u05fb\3\2\2\2\u05fb\u05f9\3\2\2"+
		"\2\u05fb\u05fc\3\2\2\2\u05fc\u0627\3\2\2\2\u05fd\u05fe\7H\2\2\u05fe\u05ff"+
		"\7j\2\2\u05ff\u0600\5\u00ba^\2\u0600\u0602\5\u00b4[\2\u0601\u0603\5x="+
		"\2\u0602\u0601\3\2\2\2\u0602\u0603\3\2\2\2\u0603\u0627\3\2\2\2\u0604\u0605"+
		"\7\u0106\2\2\u0605\u0606\5\u00ba^\2\u0606\u0607\5\u00b4[\2\u0607\u0609"+
		"\5j\66\2\u0608\u060a\5x=\2\u0609\u0608\3\2\2\2\u0609\u060a\3\2\2\2\u060a"+
		"\u0627\3\2\2\2\u060b\u060c\7\u0097\2\2\u060c\u060d\7\u0080\2\2\u060d\u060e"+
		"\5\u00ba^\2\u060e\u060f\5\u00b4[\2\u060f\u0615\7\u0109\2\2\u0610\u0616"+
		"\5\u00ba^\2\u0611\u0612\7\4\2\2\u0612\u0613\5(\25\2\u0613\u0614\7\5\2"+
		"\2\u0614\u0616\3\2\2\2\u0615\u0610\3\2\2\2\u0615\u0611\3\2\2\2\u0616\u0617"+
		"\3\2\2\2\u0617\u0618\5\u00b4[\2\u0618\u0619\7\u00a3\2\2\u0619\u061d\5"+
		"\u00d0i\2\u061a\u061c\5l\67\2\u061b\u061a\3\2\2\2\u061c\u061f\3\2\2\2"+
		"\u061d\u061b\3\2\2\2\u061d\u061e\3\2\2\2\u061e\u0623\3\2\2\2\u061f\u061d"+
		"\3\2\2\2\u0620\u0622\5n8\2\u0621\u0620\3\2\2\2\u0622\u0625\3\2\2\2\u0623"+
		"\u0621\3\2\2\2\u0623\u0624\3\2\2\2\u0624\u0627\3\2\2\2\u0625\u0623\3\2"+
		"\2\2\u0626\u05f3\3\2\2\2\u0626\u05f7\3\2\2\2\u0626\u05fd\3\2\2\2\u0626"+
		"\u0604\3\2\2\2\u0626\u060b\3\2\2\2\u0627U\3\2\2\2\u0628\u0629\7\u00a8"+
		"\2\2\u0629\u062a\7#\2\2\u062a\u062f\5^\60\2\u062b\u062c\7\6\2\2\u062c"+
		"\u062e\5^\60\2\u062d\u062b\3\2\2\2\u062e\u0631\3\2\2\2\u062f\u062d\3\2"+
		"\2\2\u062f\u0630\3\2\2\2\u0630\u0633\3\2\2\2\u0631\u062f\3\2\2\2\u0632"+
		"\u0628\3\2\2\2\u0632\u0633\3\2\2\2\u0633\u063e\3\2\2\2\u0634\u0635\7+"+
		"\2\2\u0635\u0636\7#\2\2\u0636\u063b\5\u00ccg\2\u0637\u0638\7\6\2\2\u0638"+
		"\u063a\5\u00ccg\2\u0639\u0637\3\2\2\2\u063a\u063d\3\2\2\2\u063b\u0639"+
		"\3\2\2\2\u063b\u063c\3\2\2\2\u063c\u063f\3\2\2\2\u063d\u063b\3\2\2\2\u063e"+
		"\u0634\3\2\2\2\u063e\u063f\3\2\2\2\u063f\u064a\3\2\2\2\u0640\u0641\7P"+
		"\2\2\u0641\u0642\7#\2\2\u0642\u0647\5\u00ccg\2\u0643\u0644\7\6\2\2\u0644"+
		"\u0646\5\u00ccg\2\u0645\u0643\3\2\2\2\u0646\u0649\3\2\2\2\u0647\u0645"+
		"\3\2\2\2\u0647\u0648\3\2\2\2\u0648\u064b\3\2\2\2\u0649\u0647\3\2\2\2\u064a"+
		"\u0640\3\2\2\2\u064a\u064b\3\2\2\2\u064b\u0656\3\2\2\2\u064c\u064d\7\u00e1"+
		"\2\2\u064d\u064e\7#\2\2\u064e\u0653\5^\60\2\u064f\u0650\7\6\2\2\u0650"+
		"\u0652\5^\60\2\u0651\u064f\3\2\2\2\u0652\u0655\3\2\2\2\u0653\u0651\3\2"+
		"\2\2\u0653\u0654\3\2\2\2\u0654\u0657\3\2\2\2\u0655\u0653\3\2\2\2\u0656"+
		"\u064c\3\2\2\2\u0656\u0657\3\2\2\2\u0657\u0659\3\2\2\2\u0658\u065a\5\u0100"+
		"\u0081\2\u0659\u0658\3\2\2\2\u0659\u065a\3\2\2\2\u065a\u0660\3\2\2\2\u065b"+
		"\u065e\7\u008b\2\2\u065c\u065f\7\23\2\2\u065d\u065f\5\u00ccg\2\u065e\u065c"+
		"\3\2\2\2\u065e\u065d\3\2\2\2\u065f\u0661\3\2\2\2\u0660\u065b\3\2\2\2\u0660"+
		"\u0661\3\2\2\2\u0661W\3\2\2\2\u0662\u0663\5*\26\2\u0663\u0664\5b\62\2"+
		"\u0664Y\3\2\2\2\u0665\u0666\b.\1\2\u0666\u0667\5\\/\2\u0667\u067f\3\2"+
		"\2\2\u0668\u0669\f\5\2\2\u0669\u066a\6.\3\2\u066a\u066c\t\21\2\2\u066b"+
		"\u066d\5\u0094K\2\u066c\u066b\3\2\2\2\u066c\u066d\3\2\2\2\u066d\u066e"+
		"\3\2\2\2\u066e\u067e\5Z.\6\u066f\u0670\f\4\2\2\u0670\u0671\6.\5\2\u0671"+
		"\u0673\7~\2\2\u0672\u0674\5\u0094K\2\u0673\u0672\3\2\2\2\u0673\u0674\3"+
		"\2\2\2\u0674\u0675\3\2\2\2\u0675\u067e\5Z.\5\u0676\u0677\f\3\2\2\u0677"+
		"\u0678\6.\7\2\u0678\u067a\t\22\2\2\u0679\u067b\5\u0094K\2\u067a\u0679"+
		"\3\2\2\2\u067a\u067b\3\2\2\2\u067b\u067c\3\2\2\2\u067c\u067e\5Z.\4\u067d"+
		"\u0668\3\2\2\2\u067d\u066f\3\2\2\2\u067d\u0676\3\2\2\2\u067e\u0681\3\2"+
		"\2\2\u067f\u067d\3\2\2\2\u067f\u0680\3\2\2\2\u0680[\3\2\2\2\u0681\u067f"+
		"\3\2\2\2\u0682\u068c\5d\63\2\u0683\u068c\5`\61\2\u0684\u0685\7\u00eb\2"+
		"\2\u0685\u068c\5\u00ba^\2\u0686\u068c\5\u00b0Y\2\u0687\u0688\7\4\2\2\u0688"+
		"\u0689\5(\25\2\u0689\u068a\7\5\2\2\u068a\u068c\3\2\2\2\u068b\u0682\3\2"+
		"\2\2\u068b\u0683\3\2\2\2\u068b\u0684\3\2\2\2\u068b\u0686\3\2\2\2\u068b"+
		"\u0687\3\2\2\2\u068c]\3\2\2\2\u068d\u068f\5\u00ccg\2\u068e\u0690\t\23"+
		"\2\2\u068f\u068e\3\2\2\2\u068f\u0690\3\2\2\2\u0690\u0693\3\2\2\2\u0691"+
		"\u0692\7\u00a1\2\2\u0692\u0694\t\24\2\2\u0693\u0691\3\2\2\2\u0693\u0694"+
		"\3\2\2\2\u0694_\3\2\2\2\u0695\u0697\5\u0080A\2\u0696\u0698\5b\62\2\u0697"+
		"\u0696\3\2\2\2\u0698\u0699\3\2\2\2\u0699\u0697\3\2\2\2\u0699\u069a\3\2"+
		"\2\2\u069aa\3\2\2\2\u069b\u069d\5f\64\2\u069c\u069e\5x=\2\u069d\u069c"+
		"\3\2\2\2\u069d\u069e\3\2\2\2\u069e\u069f\3\2\2\2\u069f\u06a0\5V,\2\u06a0"+
		"\u06b7\3\2\2\2\u06a1\u06a5\5h\65\2\u06a2\u06a4\5\u0092J\2\u06a3\u06a2"+
		"\3\2\2\2\u06a4\u06a7\3\2\2\2\u06a5\u06a3\3\2\2\2\u06a5\u06a6\3\2\2\2\u06a6"+
		"\u06a9\3\2\2\2\u06a7\u06a5\3\2\2\2\u06a8\u06aa\5x=\2\u06a9\u06a8\3\2\2"+
		"\2\u06a9\u06aa\3\2\2\2\u06aa\u06ac\3\2\2\2\u06ab\u06ad\5\u0082B\2\u06ac"+
		"\u06ab\3\2\2\2\u06ac\u06ad\3\2\2\2\u06ad\u06af\3\2\2\2\u06ae\u06b0\5z"+
		">\2\u06af\u06ae\3\2\2\2\u06af\u06b0\3\2\2\2\u06b0\u06b2\3\2\2\2\u06b1"+
		"\u06b3\5\u0100\u0081\2\u06b2\u06b1\3\2\2\2\u06b2\u06b3\3\2\2\2\u06b3\u06b4"+
		"\3\2\2\2\u06b4\u06b5\5V,\2\u06b5\u06b7\3\2\2\2\u06b6\u069b\3\2\2\2\u06b6"+
		"\u06a1\3\2\2\2\u06b7c\3\2\2\2\u06b8\u06ba\5f\64\2\u06b9\u06bb\5\u0080"+
		"A\2\u06ba\u06b9\3\2\2\2\u06ba\u06bb\3\2\2\2\u06bb\u06bf\3\2\2\2\u06bc"+
		"\u06be\5\u0092J\2\u06bd\u06bc\3\2\2\2\u06be\u06c1\3\2\2\2\u06bf\u06bd"+
		"\3\2\2\2\u06bf\u06c0\3\2\2\2\u06c0\u06c3\3\2\2\2\u06c1\u06bf\3\2\2\2\u06c2"+
		"\u06c4\5x=\2\u06c3\u06c2\3\2\2\2\u06c3\u06c4\3\2\2\2\u06c4\u06c6\3\2\2"+
		"\2\u06c5\u06c7\5\u0082B\2\u06c6\u06c5\3\2\2\2\u06c6\u06c7\3\2\2\2\u06c7"+
		"\u06c9\3\2\2\2\u06c8\u06ca\5z>\2\u06c9\u06c8\3\2\2\2\u06c9\u06ca\3\2\2"+
		"\2\u06ca\u06cc\3\2\2\2\u06cb\u06cd\5\u0100\u0081\2\u06cc\u06cb\3\2\2\2"+
		"\u06cc\u06cd\3\2\2\2\u06cd\u06e5\3\2\2\2\u06ce\u06d0\5h\65\2\u06cf\u06d1"+
		"\5\u0080A\2\u06d0\u06cf\3\2\2\2\u06d0\u06d1\3\2\2\2\u06d1\u06d5\3\2\2"+
		"\2\u06d2\u06d4\5\u0092J\2\u06d3\u06d2\3\2\2\2\u06d4\u06d7\3\2\2\2\u06d5"+
		"\u06d3\3\2\2\2\u06d5\u06d6\3\2\2\2\u06d6\u06d9\3\2\2\2\u06d7\u06d5\3\2"+
		"\2\2\u06d8\u06da\5x=\2\u06d9\u06d8\3\2\2\2\u06d9\u06da\3\2\2\2\u06da\u06dc"+
		"\3\2\2\2\u06db\u06dd\5\u0082B\2\u06dc\u06db\3\2\2\2\u06dc\u06dd\3\2\2"+
		"\2\u06dd\u06df\3\2\2\2\u06de\u06e0\5z>\2\u06df\u06de\3\2\2\2\u06df\u06e0"+
		"\3\2\2\2\u06e0\u06e2\3\2\2\2\u06e1\u06e3\5\u0100\u0081\2\u06e2\u06e1\3"+
		"\2\2\2\u06e2\u06e3\3\2\2\2\u06e3\u06e5\3\2\2\2\u06e4\u06b8\3\2\2\2\u06e4"+
		"\u06ce\3\2\2\2\u06e5e\3\2\2\2\u06e6\u06e7\7\u00d5\2\2\u06e7\u06e8\7\u00f8"+
		"\2\2\u06e8\u06ea\7\4\2\2\u06e9\u06eb\5\u0094K\2\u06ea\u06e9\3\2\2\2\u06ea"+
		"\u06eb\3\2\2\2\u06eb\u06ec\3\2\2\2\u06ec\u06ed\5\u00ceh\2\u06ed\u06ee"+
		"\7\5\2\2\u06ee\u06fa\3\2\2\2\u06ef\u06f1\7\u0095\2\2\u06f0\u06f2\5\u0094"+
		"K\2\u06f1\u06f0\3\2\2\2\u06f1\u06f2\3\2\2\2\u06f2\u06f3\3\2\2\2\u06f3"+
		"\u06fa\5\u00ceh\2\u06f4\u06f6\7\u00c1\2\2\u06f5\u06f7\5\u0094K\2\u06f6"+
		"\u06f5\3\2\2\2\u06f6\u06f7\3\2\2\2\u06f7\u06f8\3\2\2\2\u06f8\u06fa\5\u00ce"+
		"h\2\u06f9\u06e6\3\2\2\2\u06f9\u06ef\3\2\2\2\u06f9\u06f4\3\2\2\2\u06fa"+
		"\u06fc\3\2\2\2\u06fb\u06fd\5\u00b6\\\2\u06fc\u06fb\3\2\2\2\u06fc\u06fd"+
		"\3\2\2\2\u06fd\u0700\3\2\2\2\u06fe\u06ff\7\u00bf\2\2\u06ff\u0701\7\u0125"+
		"\2\2\u0700\u06fe\3\2\2\2\u0700\u0701\3\2\2\2\u0701\u0702\3\2\2\2\u0702"+
		"\u0703\7\u0109\2\2\u0703\u0710\7\u0125\2\2\u0704\u070e\7\33\2\2\u0705"+
		"\u070f\5\u00a4S\2\u0706\u070f\5\u00f6|\2\u0707\u070a\7\4\2\2\u0708\u070b"+
		"\5\u00a4S\2\u0709\u070b\5\u00f6|\2\u070a\u0708\3\2\2\2\u070a\u0709\3\2"+
		"\2\2\u070b\u070c\3\2\2\2\u070c\u070d\7\5\2\2\u070d\u070f\3\2\2\2\u070e"+
		"\u0705\3\2\2\2\u070e\u0706\3\2\2\2\u070e\u0707\3\2\2\2\u070f\u0711\3\2"+
		"\2\2\u0710\u0704\3\2\2\2\u0710\u0711\3\2\2\2\u0711\u0713\3\2\2\2\u0712"+
		"\u0714\5\u00b6\\\2\u0713\u0712\3\2\2\2\u0713\u0714\3\2\2\2\u0714\u0717"+
		"\3\2\2\2\u0715\u0716\7\u00be\2\2\u0716\u0718\7\u0125\2\2\u0717\u0715\3"+
		"\2\2\2\u0717\u0718\3\2\2\2\u0718g\3\2\2\2\u0719\u071d\7\u00d5\2\2\u071a"+
		"\u071c\5|?\2\u071b\u071a\3\2\2\2\u071c\u071f\3\2\2\2\u071d\u071b\3\2\2"+
		"\2\u071d\u071e\3\2\2\2\u071e\u0721\3\2\2\2\u071f\u071d\3\2\2\2\u0720\u0722"+
		"\5\u0094K\2\u0721\u0720\3\2\2\2\u0721\u0722\3\2\2\2\u0722\u0723\3\2\2"+
		"\2\u0723\u0724\5\u00c2b\2\u0724i\3\2\2\2\u0725\u0726\7\u00db\2\2\u0726"+
		"\u0727\5t;\2\u0727k\3\2\2\2\u0728\u0729\7\u010d\2\2\u0729\u072c\7\u0096"+
		"\2\2\u072a\u072b\7\26\2\2\u072b\u072d\5\u00d0i\2\u072c\u072a\3\2\2\2\u072c"+
		"\u072d\3\2\2\2\u072d\u072e\3\2\2\2\u072e\u072f\7\u00f1\2\2\u072f\u0730"+
		"\5p9\2\u0730m\3\2\2\2\u0731\u0732\7\u010d\2\2\u0732\u0733\7\u009f\2\2"+
		"\u0733\u0736\7\u0096\2\2\u0734\u0735\7\26\2\2\u0735\u0737\5\u00d0i\2\u0736"+
		"\u0734\3\2\2\2\u0736\u0737\3\2\2\2\u0737\u0738\3\2\2\2\u0738\u0739\7\u00f1"+
		"\2\2\u0739\u073a\5r:\2\u073ao\3\2\2\2\u073b\u0743\7H\2\2\u073c\u073d\7"+
		"\u0106\2\2\u073d\u073e\7\u00db\2\2\u073e\u0743\7\u011d\2\2\u073f\u0740"+
		"\7\u0106\2\2\u0740\u0741\7\u00db\2\2\u0741\u0743\5t;\2\u0742\u073b\3\2"+
		"\2\2\u0742\u073c\3\2\2\2\u0742\u073f\3\2\2\2\u0743q\3\2\2\2\u0744\u0745"+
		"\7}\2\2\u0745\u0757\7\u011d\2\2\u0746\u0747\7}\2\2\u0747\u0748\7\4\2\2"+
		"\u0748\u0749\5\u00b8]\2\u0749\u074a\7\5\2\2\u074a\u074b\7\u010a\2\2\u074b"+
		"\u074c\7\4\2\2\u074c\u0751\5\u00ccg\2\u074d\u074e\7\6\2\2\u074e\u0750"+
		"\5\u00ccg\2\u074f\u074d\3\2\2\2\u0750\u0753\3\2\2\2\u0751\u074f\3\2\2"+
		"\2\u0751\u0752\3\2\2\2\u0752\u0754\3\2\2\2\u0753\u0751\3\2\2\2\u0754\u0755"+
		"\7\5\2\2\u0755\u0757\3\2\2\2\u0756\u0744\3\2\2\2\u0756\u0746\3\2\2\2\u0757"+
		"s\3\2\2\2\u0758\u075d\5v<\2\u0759\u075a\7\6\2\2\u075a\u075c\5v<\2\u075b"+
		"\u0759\3\2\2\2\u075c\u075f\3\2\2\2\u075d\u075b\3\2\2\2\u075d\u075e\3\2"+
		"\2\2\u075eu\3\2\2\2\u075f\u075d\3\2\2\2\u0760\u0761\5\u00ba^\2\u0761\u0762"+
		"\7\u0113\2\2\u0762\u0763\5\u00ccg\2\u0763w\3\2\2\2\u0764\u0765\7\u010e"+
		"\2\2\u0765\u0766\5\u00d0i\2\u0766y\3\2\2\2\u0767\u0768\7r\2\2\u0768\u0769"+
		"\5\u00d0i\2\u0769{\3\2\2\2\u076a\u076b\7\b\2\2\u076b\u0772\5~@\2\u076c"+
		"\u076e\7\6\2\2\u076d\u076c\3\2\2\2\u076d\u076e\3\2\2\2\u076e\u076f\3\2"+
		"\2\2\u076f\u0771\5~@\2\u0770\u076d\3\2\2\2\u0771\u0774\3\2\2\2\u0772\u0770"+
		"\3\2\2\2\u0772\u0773\3\2\2\2\u0773\u0775\3\2\2\2\u0774\u0772\3\2\2\2\u0775"+
		"\u0776\7\t\2\2\u0776}\3\2\2\2\u0777\u0785\5\u0114\u008b\2\u0778\u0779"+
		"\5\u0114\u008b\2\u0779\u077a\7\4\2\2\u077a\u077f\5\u00d6l\2\u077b\u077c"+
		"\7\6\2\2\u077c\u077e\5\u00d6l\2\u077d\u077b\3\2\2\2\u077e\u0781\3\2\2"+
		"\2\u077f\u077d\3\2\2\2\u077f\u0780\3\2\2\2\u0780\u0782\3\2\2\2\u0781\u077f"+
		"\3\2\2\2\u0782\u0783\7\5\2\2\u0783\u0785\3\2\2\2\u0784\u0777\3\2\2\2\u0784"+
		"\u0778\3\2\2\2\u0785\177\3\2\2\2\u0786\u0787\7j\2\2\u0787\u078c\5\u0096"+
		"L\2\u0788\u0789\7\6\2\2\u0789\u078b\5\u0096L\2\u078a\u0788\3\2\2\2\u078b"+
		"\u078e\3\2\2\2\u078c\u078a\3\2\2\2\u078c\u078d\3\2\2\2\u078d\u0792\3\2"+
		"\2\2\u078e\u078c\3\2\2\2\u078f\u0791\5\u0092J\2\u0790\u078f\3\2\2\2\u0791"+
		"\u0794\3\2\2\2\u0792\u0790\3\2\2\2\u0792\u0793\3\2\2\2\u0793\u0796\3\2"+
		"\2\2\u0794\u0792\3\2\2\2\u0795\u0797\5\u008cG\2\u0796\u0795\3\2\2\2\u0796"+
		"\u0797\3\2\2\2\u0797\u0081\3\2\2\2\u0798\u0799\7p\2\2\u0799\u079a\7#\2"+
		"\2\u079a\u079f\5\u0084C\2\u079b\u079c\7\6\2\2\u079c\u079e\5\u0084C\2\u079d"+
		"\u079b\3\2\2\2\u079e\u07a1\3\2\2\2\u079f\u079d\3\2\2\2\u079f\u07a0\3\2"+
		"\2\2\u07a0\u07c0\3\2\2\2\u07a1\u079f\3\2\2\2\u07a2\u07a3\7p\2\2\u07a3"+
		"\u07a4\7#\2\2\u07a4\u07a9\5\u00ccg\2\u07a5\u07a6\7\6\2\2\u07a6\u07a8\5"+
		"\u00ccg\2\u07a7\u07a5\3\2\2\2\u07a8\u07ab\3\2\2\2\u07a9\u07a7\3\2\2\2"+
		"\u07a9\u07aa\3\2\2\2\u07aa\u07bd\3\2\2\2\u07ab\u07a9\3\2\2\2\u07ac\u07ad"+
		"\7\u0110\2\2\u07ad\u07be\7\u00d0\2\2\u07ae\u07af\7\u0110\2\2\u07af\u07be"+
		"\7<\2\2\u07b0\u07b1\7q\2\2\u07b1\u07b2\7\u00dd\2\2\u07b2\u07b3\7\4\2\2"+
		"\u07b3\u07b8\5\u008aF\2\u07b4\u07b5\7\6\2\2\u07b5\u07b7\5\u008aF\2\u07b6"+
		"\u07b4\3\2\2\2\u07b7\u07ba\3\2\2\2\u07b8\u07b6\3\2\2\2\u07b8\u07b9\3\2"+
		"\2\2\u07b9\u07bb\3\2\2\2\u07ba\u07b8\3\2\2\2\u07bb\u07bc\7\5\2\2\u07bc"+
		"\u07be\3\2\2\2\u07bd\u07ac\3\2\2\2\u07bd\u07ae\3\2\2\2\u07bd\u07b0\3\2"+
		"\2\2\u07bd\u07be\3\2\2\2\u07be\u07c0\3\2\2\2\u07bf\u0798\3\2\2\2\u07bf"+
		"\u07a2\3\2\2\2\u07c0\u0083\3\2\2\2\u07c1\u07c4\5\u0086D\2\u07c2\u07c4"+
		"\5\u00ccg\2\u07c3\u07c1\3\2\2\2\u07c3\u07c2\3\2\2\2\u07c4\u0085\3\2\2"+
		"\2\u07c5\u07c6\t\25\2\2\u07c6\u07c7\7\4\2\2\u07c7\u07cc\5\u008aF\2\u07c8"+
		"\u07c9\7\6\2\2\u07c9\u07cb\5\u008aF\2\u07ca\u07c8\3\2\2\2\u07cb\u07ce"+
		"\3\2\2\2\u07cc\u07ca\3\2\2\2\u07cc\u07cd\3\2\2\2\u07cd\u07cf\3\2\2\2\u07ce"+
		"\u07cc\3\2\2\2\u07cf\u07d0\7\5\2\2\u07d0\u07df\3\2\2\2\u07d1\u07d2\7q"+
		"\2\2\u07d2\u07d3\7\u00dd\2\2\u07d3\u07d4\7\4\2\2\u07d4\u07d9\5\u0088E"+
		"\2\u07d5\u07d6\7\6\2\2\u07d6\u07d8\5\u0088E\2\u07d7\u07d5\3\2\2\2\u07d8"+
		"\u07db\3\2\2\2\u07d9\u07d7\3\2\2\2\u07d9\u07da\3\2\2\2\u07da\u07dc\3\2"+
		"\2\2\u07db\u07d9\3\2\2\2\u07dc\u07dd\7\5\2\2\u07dd\u07df\3\2\2\2\u07de"+
		"\u07c5\3\2\2\2\u07de\u07d1\3\2\2\2\u07df\u0087\3\2\2\2\u07e0\u07e3\5\u0086"+
		"D\2\u07e1\u07e3\5\u008aF\2\u07e2\u07e0\3\2\2\2\u07e2\u07e1\3\2\2\2\u07e3"+
		"\u0089\3\2\2\2\u07e4\u07ed\7\4\2\2\u07e5\u07ea\5\u00ccg\2\u07e6\u07e7"+
		"\7\6\2\2\u07e7\u07e9\5\u00ccg\2\u07e8\u07e6\3\2\2\2\u07e9\u07ec\3\2\2"+
		"\2\u07ea\u07e8\3\2\2\2\u07ea\u07eb\3\2\2\2\u07eb\u07ee\3\2\2\2\u07ec\u07ea"+
		"\3\2\2\2\u07ed\u07e5\3\2\2\2\u07ed\u07ee\3\2\2\2\u07ee\u07ef\3\2\2\2\u07ef"+
		"\u07f2\7\5\2\2\u07f0\u07f2\5\u00ccg\2\u07f1\u07e4\3\2\2\2\u07f1\u07f0"+
		"\3\2\2\2\u07f2\u008b\3\2\2\2\u07f3\u07f4\7\u00b4\2\2\u07f4\u07f5\7\4\2"+
		"\2\u07f5\u07f6\5\u00c2b\2\u07f6\u07f7\7f\2\2\u07f7\u07f8\5\u008eH\2\u07f8"+
		"\u07f9\7w\2\2\u07f9\u07fa\7\4\2\2\u07fa\u07ff\5\u0090I\2\u07fb\u07fc\7"+
		"\6\2\2\u07fc\u07fe\5\u0090I\2\u07fd\u07fb\3\2\2\2\u07fe\u0801\3\2\2\2"+
		"\u07ff\u07fd\3\2\2\2\u07ff\u0800\3\2\2\2\u0800\u0802\3\2\2\2\u0801\u07ff"+
		"\3\2\2\2\u0802\u0803\7\5\2\2\u0803\u0804\7\5\2\2\u0804\u008d\3\2\2\2\u0805"+
		"\u0812\5\u0114\u008b\2\u0806\u0807\7\4\2\2\u0807\u080c\5\u0114\u008b\2"+
		"\u0808\u0809\7\6\2\2\u0809\u080b\5\u0114\u008b\2\u080a\u0808\3\2\2\2\u080b"+
		"\u080e\3\2\2\2\u080c\u080a\3\2\2\2\u080c\u080d\3\2\2\2\u080d\u080f\3\2"+
		"\2\2\u080e\u080c\3\2\2\2\u080f\u0810\7\5\2\2\u0810\u0812\3\2\2\2\u0811"+
		"\u0805\3\2\2\2\u0811\u0806\3\2\2\2\u0812\u008f\3\2\2\2\u0813\u0818\5\u00cc"+
		"g\2\u0814\u0816\7\33\2\2\u0815\u0814\3\2\2\2\u0815\u0816\3\2\2\2\u0816"+
		"\u0817\3\2\2\2\u0817\u0819\5\u0114\u008b\2\u0818\u0815\3\2\2\2\u0818\u0819"+
		"\3\2\2\2\u0819\u0091\3\2\2\2\u081a\u081b\7\u0086\2\2\u081b\u081d\7\u010b"+
		"\2\2\u081c\u081e\7\u00aa\2\2\u081d\u081c\3\2\2\2\u081d\u081e\3\2\2\2\u081e"+
		"\u081f\3\2\2\2\u081f\u0820\5\u010e\u0088\2\u0820\u0829\7\4\2\2\u0821\u0826"+
		"\5\u00ccg\2\u0822\u0823\7\6\2\2\u0823\u0825\5\u00ccg\2\u0824\u0822\3\2"+
		"\2\2\u0825\u0828\3\2\2\2\u0826\u0824\3\2\2\2\u0826\u0827\3\2\2\2\u0827"+
		"\u082a\3\2\2\2\u0828\u0826\3\2\2\2\u0829\u0821\3\2\2\2\u0829\u082a\3\2"+
		"\2\2\u082a\u082b\3\2\2\2\u082b\u082c\7\5\2\2\u082c\u0838\5\u0114\u008b"+
		"\2\u082d\u082f\7\33\2\2\u082e\u082d\3\2\2\2\u082e\u082f\3\2\2\2\u082f"+
		"\u0830\3\2\2\2\u0830\u0835\5\u0114\u008b\2\u0831\u0832\7\6\2\2\u0832\u0834"+
		"\5\u0114\u008b\2\u0833\u0831\3\2\2\2\u0834\u0837\3\2\2\2\u0835\u0833\3"+
		"\2\2\2\u0835\u0836\3\2\2\2\u0836\u0839\3\2\2\2\u0837\u0835\3\2\2\2\u0838"+
		"\u082e\3\2\2\2\u0838\u0839\3\2\2\2\u0839\u0093\3\2\2\2\u083a\u083b\t\26"+
		"\2\2\u083b\u0095\3\2\2\2\u083c\u083e\7\u0086\2\2\u083d\u083c\3\2\2\2\u083d"+
		"\u083e\3\2\2\2\u083e\u083f\3\2\2\2\u083f\u0843\5\u00aeX\2\u0840\u0842"+
		"\5\u0098M\2\u0841\u0840\3\2\2\2\u0842\u0845\3\2\2\2\u0843\u0841\3\2\2"+
		"\2\u0843\u0844\3\2\2\2\u0844\u0097\3\2\2\2\u0845\u0843\3\2\2\2\u0846\u0847"+
		"\5\u009aN\2\u0847\u0849\7\u0083\2\2\u0848\u084a\7\u0086\2\2\u0849\u0848"+
		"\3\2\2\2\u0849\u084a\3\2\2\2\u084a\u084b\3\2\2\2\u084b\u084d\5\u00aeX"+
		"\2\u084c\u084e\5\u009cO\2\u084d\u084c\3\2\2\2\u084d\u084e\3\2\2\2\u084e"+
		"\u0858\3\2\2\2\u084f\u0850\7\u009d\2\2\u0850\u0851\5\u009aN\2\u0851\u0853"+
		"\7\u0083\2\2\u0852\u0854\7\u0086\2\2\u0853\u0852\3\2\2\2\u0853\u0854\3"+
		"\2\2\2\u0854\u0855";
	private static final String _serializedATNSegment1 =
		"\3\2\2\2\u0855\u0856\5\u00aeX\2\u0856\u0858\3\2\2\2\u0857\u0846\3\2\2"+
		"\2\u0857\u084f\3\2\2\2\u0858\u0099\3\2\2\2\u0859\u085b\7z\2\2\u085a\u0859"+
		"\3\2\2\2\u085a\u085b\3\2\2\2\u085b\u0872\3\2\2\2\u085c\u0872\7;\2\2\u085d"+
		"\u085f\7\u0089\2\2\u085e\u0860\7\u00aa\2\2\u085f\u085e\3\2\2\2\u085f\u0860"+
		"\3\2\2\2\u0860\u0872\3\2\2\2\u0861\u0863\7\u0089\2\2\u0862\u0861\3\2\2"+
		"\2\u0862\u0863\3\2\2\2\u0863\u0864\3\2\2\2\u0864\u0872\7\u00d6\2\2\u0865"+
		"\u0867\7\u00cb\2\2\u0866\u0868\7\u00aa\2\2\u0867\u0866\3\2\2\2\u0867\u0868"+
		"\3\2\2\2\u0868\u0872\3\2\2\2\u0869\u086b\7k\2\2\u086a\u086c\7\u00aa\2"+
		"\2\u086b\u086a\3\2\2\2\u086b\u086c\3\2\2\2\u086c\u0872\3\2\2\2\u086d\u086f"+
		"\7\u0089\2\2\u086e\u086d\3\2\2\2\u086e\u086f\3\2\2\2\u086f\u0870\3\2\2"+
		"\2\u0870\u0872\7\27\2\2\u0871\u085a\3\2\2\2\u0871\u085c\3\2\2\2\u0871"+
		"\u085d\3\2\2\2\u0871\u0862\3\2\2\2\u0871\u0865\3\2\2\2\u0871\u0869\3\2"+
		"\2\2\u0871\u086e\3\2\2\2\u0872\u009b\3\2\2\2\u0873\u0874\7\u00a3\2\2\u0874"+
		"\u0878\5\u00d0i\2\u0875\u0876\7\u0109\2\2\u0876\u0878\5\u00a2R\2\u0877"+
		"\u0873\3\2\2\2\u0877\u0875\3\2\2\2\u0878\u009d\3\2\2\2\u0879\u087a\7\u00ed"+
		"\2\2\u087a\u087c\7\4\2\2\u087b\u087d\5\u00a0Q\2\u087c\u087b\3\2\2\2\u087c"+
		"\u087d\3\2\2\2\u087d\u087e\3\2\2\2\u087e\u087f\7\5\2\2\u087f\u009f\3\2"+
		"\2\2\u0880\u0882\7\u011c\2\2\u0881\u0880\3\2\2\2\u0881\u0882\3\2\2\2\u0882"+
		"\u0883\3\2\2\2\u0883\u0884\t\27\2\2\u0884\u0899\7\u00b3\2\2\u0885\u0886"+
		"\5\u00ccg\2\u0886\u0887\7\u00d2\2\2\u0887\u0899\3\2\2\2\u0888\u0889\7"+
		"!\2\2\u0889\u088a\7\u0129\2\2\u088a\u088b\7\u00a9\2\2\u088b\u088c\7\u00a2"+
		"\2\2\u088c\u0895\7\u0129\2\2\u088d\u0893\7\u00a3\2\2\u088e\u0894\5\u0114"+
		"\u008b\2\u088f\u0890\5\u010e\u0088\2\u0890\u0891\7\4\2\2\u0891\u0892\7"+
		"\5\2\2\u0892\u0894\3\2\2\2\u0893\u088e\3\2\2\2\u0893\u088f\3\2\2\2\u0894"+
		"\u0896\3\2\2\2\u0895\u088d\3\2\2\2\u0895\u0896\3\2\2\2\u0896\u0899\3\2"+
		"\2\2\u0897\u0899\5\u00ccg\2\u0898\u0881\3\2\2\2\u0898\u0885\3\2\2\2\u0898"+
		"\u0888\3\2\2\2\u0898\u0897\3\2\2\2\u0899\u00a1\3\2\2\2\u089a\u089b\7\4"+
		"\2\2\u089b\u089c\5\u00a4S\2\u089c\u089d\7\5\2\2\u089d\u00a3\3\2\2\2\u089e"+
		"\u08a3\5\u0110\u0089\2\u089f\u08a0\7\6\2\2\u08a0\u08a2\5\u0110\u0089\2"+
		"\u08a1\u089f\3\2\2\2\u08a2\u08a5\3\2\2\2\u08a3\u08a1\3\2\2\2\u08a3\u08a4"+
		"\3\2\2\2\u08a4\u00a5\3\2\2\2\u08a5\u08a3\3\2\2\2\u08a6\u08a7\7\4\2\2\u08a7"+
		"\u08ac\5\u00a8U\2\u08a8\u08a9\7\6\2\2\u08a9\u08ab\5\u00a8U\2\u08aa\u08a8"+
		"\3\2\2\2\u08ab\u08ae\3\2\2\2\u08ac\u08aa\3\2\2\2\u08ac\u08ad\3\2\2\2\u08ad"+
		"\u08af\3\2\2\2\u08ae\u08ac\3\2\2\2\u08af\u08b0\7\5\2\2\u08b0\u00a7\3\2"+
		"\2\2\u08b1\u08b3\5\u0110\u0089\2\u08b2\u08b4\t\23\2\2\u08b3\u08b2\3\2"+
		"\2\2\u08b3\u08b4\3\2\2\2\u08b4\u00a9\3\2\2\2\u08b5\u08b6\7\4\2\2\u08b6"+
		"\u08bb\5\u00acW\2\u08b7\u08b8\7\6\2\2\u08b8\u08ba\5\u00acW\2\u08b9\u08b7"+
		"\3\2\2\2\u08ba\u08bd\3\2\2\2\u08bb\u08b9\3\2\2\2\u08bb\u08bc\3\2\2\2\u08bc"+
		"\u08be\3\2\2\2\u08bd\u08bb\3\2\2\2\u08be\u08bf\7\5\2\2\u08bf\u00ab\3\2"+
		"\2\2\u08c0\u08c2\5\u0114\u008b\2\u08c1\u08c3\5&\24\2\u08c2\u08c1\3\2\2"+
		"\2\u08c2\u08c3\3\2\2\2\u08c3\u00ad\3\2\2\2\u08c4\u08c6\5\u00ba^\2\u08c5"+
		"\u08c7\5\u009eP\2\u08c6\u08c5\3\2\2\2\u08c6\u08c7\3\2\2\2\u08c7\u08c8"+
		"\3\2\2\2\u08c8\u08c9\5\u00b4[\2\u08c9\u08dd\3\2\2\2\u08ca\u08cb\7\4\2"+
		"\2\u08cb\u08cc\5(\25\2\u08cc\u08ce\7\5\2\2\u08cd\u08cf\5\u009eP\2\u08ce"+
		"\u08cd\3\2\2\2\u08ce\u08cf\3\2\2\2\u08cf\u08d0\3\2\2\2\u08d0\u08d1\5\u00b4"+
		"[\2\u08d1\u08dd\3\2\2\2\u08d2\u08d3\7\4\2\2\u08d3\u08d4\5\u0096L\2\u08d4"+
		"\u08d6\7\5\2\2\u08d5\u08d7\5\u009eP\2\u08d6\u08d5\3\2\2\2\u08d6\u08d7"+
		"\3\2\2\2\u08d7\u08d8\3\2\2\2\u08d8\u08d9\5\u00b4[\2\u08d9\u08dd\3\2\2"+
		"\2\u08da\u08dd\5\u00b0Y\2\u08db\u08dd\5\u00b2Z\2\u08dc\u08c4\3\2\2\2\u08dc"+
		"\u08ca\3\2\2\2\u08dc\u08d2\3\2\2\2\u08dc\u08da\3\2\2\2\u08dc\u08db\3\2"+
		"\2\2\u08dd\u00af\3\2\2\2\u08de\u08df\7\u010a\2\2\u08df\u08e4\5\u00ccg"+
		"\2\u08e0\u08e1\7\6\2\2\u08e1\u08e3\5\u00ccg\2\u08e2\u08e0\3\2\2\2\u08e3"+
		"\u08e6\3\2\2\2\u08e4\u08e2\3\2\2\2\u08e4\u08e5\3\2\2\2\u08e5\u08e7\3\2"+
		"\2\2\u08e6\u08e4\3\2\2\2\u08e7\u08e8\5\u00b4[\2\u08e8\u00b1\3\2\2\2\u08e9"+
		"\u08ea\5\u010c\u0087\2\u08ea\u08f3\7\4\2\2\u08eb\u08f0\5\u00ccg\2\u08ec"+
		"\u08ed\7\6\2\2\u08ed\u08ef\5\u00ccg\2\u08ee\u08ec\3\2\2\2\u08ef\u08f2"+
		"\3\2\2\2\u08f0\u08ee\3\2\2\2\u08f0\u08f1\3\2\2\2\u08f1\u08f4\3\2\2\2\u08f2"+
		"\u08f0\3\2\2\2\u08f3\u08eb\3\2\2\2\u08f3\u08f4\3\2\2\2\u08f4\u08f5\3\2"+
		"\2\2\u08f5\u08f6\7\5\2\2\u08f6\u08f7\5\u00b4[\2\u08f7\u00b3\3\2\2\2\u08f8"+
		"\u08fa\7\33\2\2\u08f9\u08f8\3\2\2\2\u08f9\u08fa\3\2\2\2\u08fa\u08fb\3"+
		"\2\2\2\u08fb\u08fd\5\u0116\u008c\2\u08fc\u08fe\5\u00a2R\2\u08fd\u08fc"+
		"\3\2\2\2\u08fd\u08fe\3\2\2\2\u08fe\u0900\3\2\2\2\u08ff\u08f9\3\2\2\2\u08ff"+
		"\u0900\3\2\2\2\u0900\u00b5\3\2\2\2\u0901\u0902\7\u00d1\2\2\u0902\u0903"+
		"\7h\2\2\u0903\u0904\7\u00d8\2\2\u0904\u0908\7\u0125\2\2\u0905\u0906\7"+
		"\u0110\2\2\u0906\u0907\7\u00d9\2\2\u0907\u0909\5@!\2\u0908\u0905\3\2\2"+
		"\2\u0908\u0909\3\2\2\2\u0909\u0933\3\2\2\2\u090a\u090b\7\u00d1\2\2\u090b"+
		"\u090c\7h\2\2\u090c\u0916\7I\2\2\u090d\u090e\7a\2\2\u090e\u090f\7\u00f0"+
		"\2\2\u090f\u0910\7#\2\2\u0910\u0914\7\u0125\2\2\u0911\u0912\7V\2\2\u0912"+
		"\u0913\7#\2\2\u0913\u0915\7\u0125\2\2\u0914\u0911\3\2\2\2\u0914\u0915"+
		"\3\2\2\2\u0915\u0917\3\2\2\2\u0916\u090d\3\2\2\2\u0916\u0917\3\2\2\2\u0917"+
		"\u091d\3\2\2\2\u0918\u0919\7/\2\2\u0919\u091a\7\u0082\2\2\u091a\u091b"+
		"\7\u00f0\2\2\u091b\u091c\7#\2\2\u091c\u091e\7\u0125\2\2\u091d\u0918\3"+
		"\2\2\2\u091d\u091e\3\2\2\2\u091e\u0924\3\2\2\2\u091f\u0920\7\u0095\2\2"+
		"\u0920\u0921\7\u0084\2\2\u0921\u0922\7\u00f0\2\2\u0922\u0923\7#\2\2\u0923"+
		"\u0925\7\u0125\2\2\u0924\u091f\3\2\2\2\u0924\u0925\3\2\2\2\u0925\u092a"+
		"\3\2\2\2\u0926\u0927\7\u008c\2\2\u0927\u0928\7\u00f0\2\2\u0928\u0929\7"+
		"#\2\2\u0929\u092b\7\u0125\2\2\u092a\u0926\3\2\2\2\u092a\u092b\3\2\2\2"+
		"\u092b\u0930\3\2\2\2\u092c\u092d\7\u00a0\2\2\u092d\u092e\7G\2\2\u092e"+
		"\u092f\7\33\2\2\u092f\u0931\7\u0125\2\2\u0930\u092c\3\2\2\2\u0930\u0931"+
		"\3\2\2\2\u0931\u0933\3\2\2\2\u0932\u0901\3\2\2\2\u0932\u090a\3\2\2\2\u0933"+
		"\u00b7\3\2\2\2\u0934\u0939\5\u00ba^\2\u0935\u0936\7\6\2\2\u0936\u0938"+
		"\5\u00ba^\2\u0937\u0935\3\2\2\2\u0938\u093b\3\2\2\2\u0939\u0937\3\2\2"+
		"\2\u0939\u093a\3\2\2\2\u093a\u00b9\3\2\2\2\u093b\u0939\3\2\2\2\u093c\u0941"+
		"\5\u0110\u0089\2\u093d\u093e\7\7\2\2\u093e\u0940\5\u0110\u0089\2\u093f"+
		"\u093d\3\2\2\2\u0940\u0943\3\2\2\2\u0941\u093f\3\2\2\2\u0941\u0942\3\2"+
		"\2\2\u0942\u00bb\3\2\2\2\u0943\u0941\3\2\2\2\u0944\u0945\5\u0110\u0089"+
		"\2\u0945\u0946\7\7\2\2\u0946\u0948\3\2\2\2\u0947\u0944\3\2\2\2\u0947\u0948"+
		"\3\2\2\2\u0948\u0949\3\2\2\2\u0949\u094a\5\u0110\u0089\2\u094a\u00bd\3"+
		"\2\2\2\u094b\u094c\5\u0110\u0089\2\u094c\u094d\7\7\2\2\u094d\u094f\3\2"+
		"\2\2\u094e\u094b\3\2\2\2\u094e\u094f\3\2\2\2\u094f\u0950\3\2\2\2\u0950"+
		"\u0951\5\u0110\u0089\2\u0951\u00bf\3\2\2\2\u0952\u095a\5\u00ccg\2\u0953"+
		"\u0955\7\33\2\2\u0954\u0953\3\2\2\2\u0954\u0955\3\2\2\2\u0955\u0958\3"+
		"\2\2\2\u0956\u0959\5\u0110\u0089\2\u0957\u0959\5\u00a2R\2\u0958\u0956"+
		"\3\2\2\2\u0958\u0957\3\2\2\2\u0959\u095b\3\2\2\2\u095a\u0954\3\2\2\2\u095a"+
		"\u095b\3\2\2\2\u095b\u00c1\3\2\2\2\u095c\u0961\5\u00c0a\2\u095d\u095e"+
		"\7\6\2\2\u095e\u0960\5\u00c0a\2\u095f\u095d\3\2\2\2\u0960\u0963\3\2\2"+
		"\2\u0961\u095f\3\2\2\2\u0961\u0962\3\2\2\2\u0962\u00c3\3\2\2\2\u0963\u0961"+
		"\3\2\2\2\u0964\u0965\7\4\2\2\u0965\u096a\5\u00c6d\2\u0966\u0967\7\6\2"+
		"\2\u0967\u0969\5\u00c6d\2\u0968\u0966\3\2\2\2\u0969\u096c\3\2\2\2\u096a"+
		"\u0968\3\2\2\2\u096a\u096b\3\2\2\2\u096b\u096d\3\2\2\2\u096c\u096a\3\2"+
		"\2\2\u096d\u096e\7\5\2\2\u096e\u00c5\3\2\2\2\u096f\u0972\5\u00c8e\2\u0970"+
		"\u0972\5\u00f8}\2\u0971\u096f\3\2\2\2\u0971\u0970\3\2\2\2\u0972\u00c7"+
		"\3\2\2\2\u0973\u0981\5\u010e\u0088\2\u0974\u0975\5\u0114\u008b\2\u0975"+
		"\u0976\7\4\2\2\u0976\u097b\5\u00caf\2\u0977\u0978\7\6\2\2\u0978\u097a"+
		"\5\u00caf\2\u0979\u0977\3\2\2\2\u097a\u097d\3\2\2\2\u097b\u0979\3\2\2"+
		"\2\u097b\u097c\3\2\2\2\u097c\u097e\3\2\2\2\u097d\u097b\3\2\2\2\u097e\u097f"+
		"\7\5\2\2\u097f\u0981\3\2\2\2\u0980\u0973\3\2\2\2\u0980\u0974\3\2\2\2\u0981"+
		"\u00c9\3\2\2\2\u0982\u0985\5\u010e\u0088\2\u0983\u0985\5\u00d8m\2\u0984"+
		"\u0982\3\2\2\2\u0984\u0983\3\2\2\2\u0985\u00cb\3\2\2\2\u0986\u0987\5\u00d0"+
		"i\2\u0987\u00cd\3\2\2\2\u0988\u098d\5\u00ccg\2\u0989\u098a\7\6\2\2\u098a"+
		"\u098c\5\u00ccg\2\u098b\u0989\3\2\2\2\u098c\u098f\3\2\2\2\u098d\u098b"+
		"\3\2\2\2\u098d\u098e\3\2\2\2\u098e\u00cf\3\2\2\2\u098f\u098d\3\2\2\2\u0990"+
		"\u0991\bi\1\2\u0991\u0992\7\u009f\2\2\u0992\u099d\5\u00d0i\7\u0993\u0994"+
		"\7Y\2\2\u0994\u0995\7\4\2\2\u0995\u0996\5(\25\2\u0996\u0997\7\5\2\2\u0997"+
		"\u099d\3\2\2\2\u0998\u099a\5\u00d4k\2\u0999\u099b\5\u00d2j\2\u099a\u0999"+
		"\3\2\2\2\u099a\u099b\3\2\2\2\u099b\u099d\3\2\2\2\u099c\u0990\3\2\2\2\u099c"+
		"\u0993\3\2\2\2\u099c\u0998\3\2\2\2\u099d\u09a6\3\2\2\2\u099e\u099f\f\4"+
		"\2\2\u099f\u09a0\7\26\2\2\u09a0\u09a5\5\u00d0i\5\u09a1\u09a2\f\3\2\2\u09a2"+
		"\u09a3\7\u00a7\2\2\u09a3\u09a5\5\u00d0i\4\u09a4\u099e\3\2\2\2\u09a4\u09a1"+
		"\3\2\2\2\u09a5\u09a8\3\2\2\2\u09a6\u09a4\3\2\2\2\u09a6\u09a7\3\2\2\2\u09a7"+
		"\u00d1\3\2\2\2\u09a8\u09a6\3\2\2\2\u09a9\u09ab\7\u009f\2\2\u09aa\u09a9"+
		"\3\2\2\2\u09aa\u09ab\3\2\2\2\u09ab\u09ac\3\2\2\2\u09ac\u09ad\7\37\2\2"+
		"\u09ad\u09ae\5\u00d4k\2\u09ae\u09af\7\26\2\2\u09af\u09b0\5\u00d4k\2\u09b0"+
		"\u09fc\3\2\2\2\u09b1\u09b3\7\u009f\2\2\u09b2\u09b1\3\2\2\2\u09b2\u09b3"+
		"\3\2\2\2\u09b3\u09b4\3\2\2\2\u09b4\u09b5\7w\2\2\u09b5\u09b6\7\4\2\2\u09b6"+
		"\u09bb\5\u00ccg\2\u09b7\u09b8\7\6\2\2\u09b8\u09ba\5\u00ccg\2\u09b9\u09b7"+
		"\3\2\2\2\u09ba\u09bd\3\2\2\2\u09bb\u09b9\3\2\2\2\u09bb\u09bc\3\2\2\2\u09bc"+
		"\u09be\3\2\2\2\u09bd\u09bb\3\2\2\2\u09be\u09bf\7\5\2\2\u09bf\u09fc\3\2"+
		"\2\2\u09c0\u09c2\7\u009f\2\2\u09c1\u09c0\3\2\2\2\u09c1\u09c2\3\2\2\2\u09c2"+
		"\u09c3\3\2\2\2\u09c3\u09c4\7w\2\2\u09c4\u09c5\7\4\2\2\u09c5\u09c6\5(\25"+
		"\2\u09c6\u09c7\7\5\2\2\u09c7\u09fc\3\2\2\2\u09c8\u09ca\7\u009f\2\2\u09c9"+
		"\u09c8\3\2\2\2\u09c9\u09ca\3\2\2\2\u09ca\u09cb\3\2\2\2\u09cb\u09cc\7\u00cc"+
		"\2\2\u09cc\u09fc\5\u00d4k\2\u09cd\u09cf\7\u009f\2\2\u09ce\u09cd\3\2\2"+
		"\2\u09ce\u09cf\3\2\2\2\u09cf\u09d0\3\2\2\2\u09d0\u09d1\7\u008a\2\2\u09d1"+
		"\u09df\t\30\2\2\u09d2\u09d3\7\4\2\2\u09d3\u09e0\7\5\2\2\u09d4\u09d5\7"+
		"\4\2\2\u09d5\u09da\5\u00ccg\2\u09d6\u09d7\7\6\2\2\u09d7\u09d9\5\u00cc"+
		"g\2\u09d8\u09d6\3\2\2\2\u09d9\u09dc\3\2\2\2\u09da\u09d8\3\2\2\2\u09da"+
		"\u09db\3\2\2\2\u09db\u09dd\3\2\2\2\u09dc\u09da\3\2\2\2\u09dd\u09de\7\5"+
		"\2\2\u09de\u09e0\3\2\2\2\u09df\u09d2\3\2\2\2\u09df\u09d4\3\2\2\2\u09e0"+
		"\u09fc\3\2\2\2\u09e1\u09e3\7\u009f\2\2\u09e2\u09e1\3\2\2\2\u09e2\u09e3"+
		"\3\2\2\2\u09e3\u09e4\3\2\2\2\u09e4\u09e5\7\u008a\2\2\u09e5\u09e8\5\u00d4"+
		"k\2\u09e6\u09e7\7U\2\2\u09e7\u09e9\7\u0125\2\2\u09e8\u09e6\3\2\2\2\u09e8"+
		"\u09e9\3\2\2\2\u09e9\u09fc\3\2\2\2\u09ea\u09ec\7\u0081\2\2\u09eb\u09ed"+
		"\7\u009f\2\2\u09ec\u09eb\3\2\2\2\u09ec\u09ed\3\2\2\2\u09ed\u09ee\3\2\2"+
		"\2\u09ee\u09fc\7\u00a0\2\2\u09ef\u09f1\7\u0081\2\2\u09f0\u09f2\7\u009f"+
		"\2\2\u09f1\u09f0\3\2\2\2\u09f1\u09f2\3\2\2\2\u09f2\u09f3\3\2\2\2\u09f3"+
		"\u09fc\t\31\2\2\u09f4\u09f6\7\u0081\2\2\u09f5\u09f7\7\u009f\2\2\u09f6"+
		"\u09f5\3\2\2\2\u09f6\u09f7\3\2\2\2\u09f7\u09f8\3\2\2\2\u09f8\u09f9\7O"+
		"\2\2\u09f9\u09fa\7j\2\2\u09fa\u09fc\5\u00d4k\2\u09fb\u09aa\3\2\2\2\u09fb"+
		"\u09b2\3\2\2\2\u09fb\u09c1\3\2\2\2\u09fb\u09c9\3\2\2\2\u09fb\u09ce\3\2"+
		"\2\2\u09fb\u09e2\3\2\2\2\u09fb\u09ea\3\2\2\2\u09fb\u09ef\3\2\2\2\u09fb"+
		"\u09f4\3\2\2\2\u09fc\u00d3\3\2\2\2\u09fd\u09fe\bk\1\2\u09fe\u0a02\5\u00d6"+
		"l\2\u09ff\u0a00\t\32\2\2\u0a00\u0a02\5\u00d4k\t\u0a01\u09fd\3\2\2\2\u0a01"+
		"\u09ff\3\2\2\2\u0a02\u0a18\3\2\2\2\u0a03\u0a04\f\b\2\2\u0a04\u0a05\t\33"+
		"\2\2\u0a05\u0a17\5\u00d4k\t\u0a06\u0a07\f\7\2\2\u0a07\u0a08\t\34\2\2\u0a08"+
		"\u0a17\5\u00d4k\b\u0a09\u0a0a\f\6\2\2\u0a0a\u0a0b\7\u0121\2\2\u0a0b\u0a17"+
		"\5\u00d4k\7\u0a0c\u0a0d\f\5\2\2\u0a0d\u0a0e\7\u0124\2\2\u0a0e\u0a17\5"+
		"\u00d4k\6\u0a0f\u0a10\f\4\2\2\u0a10\u0a11\7\u0122\2\2\u0a11\u0a17\5\u00d4"+
		"k\5\u0a12\u0a13\f\3\2\2\u0a13\u0a14\5\u00dan\2\u0a14\u0a15\5\u00d4k\4"+
		"\u0a15\u0a17\3\2\2\2\u0a16\u0a03\3\2\2\2\u0a16\u0a06\3\2\2\2\u0a16\u0a09"+
		"\3\2\2\2\u0a16\u0a0c\3\2\2\2\u0a16\u0a0f\3\2\2\2\u0a16\u0a12\3\2\2\2\u0a17"+
		"\u0a1a\3\2\2\2\u0a18\u0a16\3\2\2\2\u0a18\u0a19\3\2\2\2\u0a19\u00d5\3\2"+
		"\2\2\u0a1a\u0a18\3\2\2\2\u0a1b\u0a1c\bl\1\2\u0a1c\u0ad8\t\35\2\2\u0a1d"+
		"\u0a1f\7&\2\2\u0a1e\u0a20\5\u00fe\u0080\2\u0a1f\u0a1e\3\2\2\2\u0a20\u0a21"+
		"\3\2\2\2\u0a21\u0a1f\3\2\2\2\u0a21\u0a22\3\2\2\2\u0a22\u0a25\3\2\2\2\u0a23"+
		"\u0a24\7S\2\2\u0a24\u0a26\5\u00ccg\2\u0a25\u0a23\3\2\2\2\u0a25\u0a26\3"+
		"\2\2\2\u0a26\u0a27\3\2\2\2\u0a27\u0a28\7T\2\2\u0a28\u0ad8\3\2\2\2\u0a29"+
		"\u0a2a\7&\2\2\u0a2a\u0a2c\5\u00ccg\2\u0a2b\u0a2d\5\u00fe\u0080\2\u0a2c"+
		"\u0a2b\3\2\2\2\u0a2d\u0a2e\3\2\2\2\u0a2e\u0a2c\3\2\2\2\u0a2e\u0a2f\3\2"+
		"\2\2\u0a2f\u0a32\3\2\2\2\u0a30\u0a31\7S\2\2\u0a31\u0a33\5\u00ccg\2\u0a32"+
		"\u0a30\3\2\2\2\u0a32\u0a33\3\2\2\2\u0a33\u0a34\3\2\2\2\u0a34\u0a35\7T"+
		"\2\2\u0a35\u0ad8\3\2\2\2\u0a36\u0a37\t\36\2\2\u0a37\u0a38\7\4\2\2\u0a38"+
		"\u0a39\5\u00ccg\2\u0a39\u0a3a\7\33\2\2\u0a3a\u0a3b\5\u00f0y\2\u0a3b\u0a3c"+
		"\7\5\2\2\u0a3c\u0ad8\3\2\2\2\u0a3d\u0a3e\7\u00e7\2\2\u0a3e\u0a47\7\4\2"+
		"\2\u0a3f\u0a44\5\u00c0a\2\u0a40\u0a41\7\6\2\2\u0a41\u0a43\5\u00c0a\2\u0a42"+
		"\u0a40\3\2\2\2\u0a43\u0a46\3\2\2\2\u0a44\u0a42\3\2\2\2\u0a44\u0a45\3\2"+
		"\2\2\u0a45\u0a48\3\2\2\2\u0a46\u0a44\3\2\2\2\u0a47\u0a3f\3\2\2\2\u0a47"+
		"\u0a48\3\2\2\2\u0a48\u0a49\3\2\2\2\u0a49\u0ad8\7\5\2\2\u0a4a\u0a4b\7d"+
		"\2\2\u0a4b\u0a4c\7\4\2\2\u0a4c\u0a4f\5\u00ccg\2\u0a4d\u0a4e\7u\2\2\u0a4e"+
		"\u0a50\7\u00a1\2\2\u0a4f\u0a4d\3\2\2\2\u0a4f\u0a50\3\2\2\2\u0a50\u0a51"+
		"\3\2\2\2\u0a51\u0a52\7\5\2\2\u0a52\u0ad8\3\2\2\2\u0a53\u0a54\7\u0085\2"+
		"\2\u0a54\u0a55\7\4\2\2\u0a55\u0a58\5\u00ccg\2\u0a56\u0a57\7u\2\2\u0a57"+
		"\u0a59\7\u00a1\2\2\u0a58\u0a56\3\2\2\2\u0a58\u0a59\3\2\2\2\u0a59\u0a5a"+
		"\3\2\2\2\u0a5a\u0a5b\7\5\2\2\u0a5b\u0ad8\3\2\2\2\u0a5c\u0a5d\7\u00b6\2"+
		"\2\u0a5d\u0a5e\7\4\2\2\u0a5e\u0a5f\5\u00d4k\2\u0a5f\u0a60\7w\2\2\u0a60"+
		"\u0a61\5\u00d4k\2\u0a61\u0a62\7\5\2\2\u0a62\u0ad8\3\2\2\2\u0a63\u0ad8"+
		"\5\u00d8m\2\u0a64\u0ad8\7\u011d\2\2\u0a65\u0a66\5\u010e\u0088\2\u0a66"+
		"\u0a67\7\7\2\2\u0a67\u0a68\7\u011d\2\2\u0a68\u0ad8\3\2\2\2\u0a69\u0a6a"+
		"\7\4\2\2\u0a6a\u0a6d\5\u00c0a\2\u0a6b\u0a6c\7\6\2\2\u0a6c\u0a6e\5\u00c0"+
		"a\2\u0a6d\u0a6b\3\2\2\2\u0a6e\u0a6f\3\2\2\2\u0a6f\u0a6d\3\2\2\2\u0a6f"+
		"\u0a70\3\2\2\2\u0a70\u0a71\3\2\2\2\u0a71\u0a72\7\5\2\2\u0a72\u0ad8\3\2"+
		"\2\2\u0a73\u0a74\7\4\2\2\u0a74\u0a75\5(\25\2\u0a75\u0a76\7\5\2\2\u0a76"+
		"\u0ad8\3\2\2\2\u0a77\u0a78\5\u010c\u0087\2\u0a78\u0a84\7\4\2\2\u0a79\u0a7b"+
		"\5\u0094K\2\u0a7a\u0a79\3\2\2\2\u0a7a\u0a7b\3\2\2\2\u0a7b\u0a7c\3\2\2"+
		"\2\u0a7c\u0a81\5\u00ccg\2\u0a7d\u0a7e\7\6\2\2\u0a7e\u0a80\5\u00ccg\2\u0a7f"+
		"\u0a7d\3\2\2\2\u0a80\u0a83\3\2\2\2\u0a81\u0a7f\3\2\2\2\u0a81\u0a82\3\2"+
		"\2\2\u0a82\u0a85\3\2\2\2\u0a83\u0a81\3\2\2\2\u0a84\u0a7a\3\2\2\2\u0a84"+
		"\u0a85\3\2\2\2\u0a85\u0a86\3\2\2\2\u0a86\u0a8d\7\5\2\2\u0a87\u0a88\7b"+
		"\2\2\u0a88\u0a89\7\4\2\2\u0a89\u0a8a\7\u010e\2\2\u0a8a\u0a8b\5\u00d0i"+
		"\2\u0a8b\u0a8c\7\5\2\2\u0a8c\u0a8e\3\2\2\2\u0a8d\u0a87\3\2\2\2\u0a8d\u0a8e"+
		"\3\2\2\2\u0a8e\u0a91\3\2\2\2\u0a8f\u0a90\t\37\2\2\u0a90\u0a92\7\u00a1"+
		"\2\2\u0a91\u0a8f\3\2\2\2\u0a91\u0a92\3\2\2\2\u0a92\u0a95\3\2\2\2\u0a93"+
		"\u0a94\7\u00ac\2\2\u0a94\u0a96\5\u0104\u0083\2\u0a95\u0a93\3\2\2\2\u0a95"+
		"\u0a96\3\2\2\2\u0a96\u0ad8\3\2\2\2\u0a97\u0a98\5\u0114\u008b\2\u0a98\u0a99"+
		"\7\n\2\2\u0a99\u0a9a\5\u00ccg\2\u0a9a\u0ad8\3\2\2\2\u0a9b\u0a9c\7\4\2"+
		"\2\u0a9c\u0a9f\5\u0114\u008b\2\u0a9d\u0a9e\7\6\2\2\u0a9e\u0aa0\5\u0114"+
		"\u008b\2\u0a9f\u0a9d\3\2\2\2\u0aa0\u0aa1\3\2\2\2\u0aa1\u0a9f\3\2\2\2\u0aa1"+
		"\u0aa2\3\2\2\2\u0aa2\u0aa3\3\2\2\2\u0aa3\u0aa4\7\5\2\2\u0aa4\u0aa5\7\n"+
		"\2\2\u0aa5\u0aa6\5\u00ccg\2\u0aa6\u0ad8\3\2\2\2\u0aa7\u0ad8\5\u0114\u008b"+
		"\2\u0aa8\u0aa9\7\4\2\2\u0aa9\u0aaa\5\u00ccg\2\u0aaa\u0aab\7\5\2\2\u0aab"+
		"\u0ad8\3\2\2\2\u0aac\u0aad\7^\2\2\u0aad\u0aae\7\4\2\2\u0aae\u0aaf\5\u0114"+
		"\u008b\2\u0aaf\u0ab0\7j\2\2\u0ab0\u0ab1\5\u00d4k\2\u0ab1\u0ab2\7\5\2\2"+
		"\u0ab2\u0ad8\3\2\2\2\u0ab3\u0ab4\t \2\2\u0ab4\u0ab5\7\4\2\2\u0ab5\u0ab6"+
		"\5\u00d4k\2\u0ab6\u0ab7\t!\2\2\u0ab7\u0aba\5\u00d4k\2\u0ab8\u0ab9\t\""+
		"\2\2\u0ab9\u0abb\5\u00d4k\2\u0aba\u0ab8\3\2\2\2\u0aba\u0abb\3\2\2\2\u0abb"+
		"\u0abc\3\2\2\2\u0abc\u0abd\7\5\2\2\u0abd\u0ad8\3\2\2\2\u0abe\u0abf\7\u00f9"+
		"\2\2\u0abf\u0ac1\7\4\2\2\u0ac0\u0ac2\t#\2\2\u0ac1\u0ac0\3\2\2\2\u0ac1"+
		"\u0ac2\3\2\2\2\u0ac2\u0ac4\3\2\2\2\u0ac3\u0ac5\5\u00d4k\2\u0ac4\u0ac3"+
		"\3\2\2\2\u0ac4\u0ac5\3\2\2\2\u0ac5\u0ac6\3\2\2\2\u0ac6\u0ac7\7j\2\2\u0ac7"+
		"\u0ac8\5\u00d4k\2\u0ac8\u0ac9\7\5\2\2\u0ac9\u0ad8\3\2\2\2\u0aca\u0acb"+
		"\7\u00ae\2\2\u0acb\u0acc\7\4\2\2\u0acc\u0acd\5\u00d4k\2\u0acd\u0ace\7"+
		"\u00b5\2\2\u0ace\u0acf\5\u00d4k\2\u0acf\u0ad0\7j\2\2\u0ad0\u0ad3\5\u00d4"+
		"k\2\u0ad1\u0ad2\7f\2\2\u0ad2\u0ad4\5\u00d4k\2\u0ad3\u0ad1\3\2\2\2\u0ad3"+
		"\u0ad4\3\2\2\2\u0ad4\u0ad5\3\2\2\2\u0ad5\u0ad6\7\5\2\2\u0ad6\u0ad8\3\2"+
		"\2\2\u0ad7\u0a1b\3\2\2\2\u0ad7\u0a1d\3\2\2\2\u0ad7\u0a29\3\2\2\2\u0ad7"+
		"\u0a36\3\2\2\2\u0ad7\u0a3d\3\2\2\2\u0ad7\u0a4a\3\2\2\2\u0ad7\u0a53\3\2"+
		"\2\2\u0ad7\u0a5c\3\2\2\2\u0ad7\u0a63\3\2\2\2\u0ad7\u0a64\3\2\2\2\u0ad7"+
		"\u0a65\3\2\2\2\u0ad7\u0a69\3\2\2\2\u0ad7\u0a73\3\2\2\2\u0ad7\u0a77\3\2"+
		"\2\2\u0ad7\u0a97\3\2\2\2\u0ad7\u0a9b\3\2\2\2\u0ad7\u0aa7\3\2\2\2\u0ad7"+
		"\u0aa8\3\2\2\2\u0ad7\u0aac\3\2\2\2\u0ad7\u0ab3\3\2\2\2\u0ad7\u0abe\3\2"+
		"\2\2\u0ad7\u0aca\3\2\2\2\u0ad8\u0ae3\3\2\2\2\u0ad9\u0ada\f\n\2\2\u0ada"+
		"\u0adb\7\13\2\2\u0adb\u0adc\5\u00d4k\2\u0adc\u0add\7\f\2\2\u0add\u0ae2"+
		"\3\2\2\2\u0ade\u0adf\f\b\2\2\u0adf\u0ae0\7\7\2\2\u0ae0\u0ae2\5\u0114\u008b"+
		"\2\u0ae1\u0ad9\3\2\2\2\u0ae1\u0ade\3\2\2\2\u0ae2\u0ae5\3\2\2\2\u0ae3\u0ae1"+
		"\3\2\2\2\u0ae3\u0ae4\3\2\2\2\u0ae4\u00d7\3\2\2\2\u0ae5\u0ae3\3\2\2\2\u0ae6"+
		"\u0af3\7\u00a0\2\2\u0ae7\u0af3\5\u00e2r\2\u0ae8\u0ae9\5\u0114\u008b\2"+
		"\u0ae9\u0aea\7\u0125\2\2\u0aea\u0af3\3\2\2\2\u0aeb\u0af3\5\u011a\u008e"+
		"\2\u0aec\u0af3\5\u00e0q\2\u0aed\u0aef\7\u0125\2\2\u0aee\u0aed\3\2\2\2"+
		"\u0aef\u0af0\3\2\2\2\u0af0\u0aee\3\2\2\2\u0af0\u0af1\3\2\2\2\u0af1\u0af3"+
		"\3\2\2\2\u0af2\u0ae6\3\2\2\2\u0af2\u0ae7\3\2\2\2\u0af2\u0ae8\3\2\2\2\u0af2"+
		"\u0aeb\3\2\2\2\u0af2\u0aec\3\2\2\2\u0af2\u0aee\3\2\2\2\u0af3\u00d9\3\2"+
		"\2\2\u0af4\u0af5\t$\2\2\u0af5\u00db\3\2\2\2\u0af6\u0af7\t%\2\2\u0af7\u00dd"+
		"\3\2\2\2\u0af8\u0af9\t&\2\2\u0af9\u00df\3\2\2\2\u0afa\u0afb\t\'\2\2\u0afb"+
		"\u00e1\3\2\2\2\u0afc\u0aff\7\177\2\2\u0afd\u0b00\5\u00e4s\2\u0afe\u0b00"+
		"\5\u00e8u\2\u0aff\u0afd\3\2\2\2\u0aff\u0afe\3\2\2\2\u0aff\u0b00\3\2\2"+
		"\2\u0b00\u00e3\3\2\2\2\u0b01\u0b03\5\u00e6t\2\u0b02\u0b04\5\u00eav\2\u0b03"+
		"\u0b02\3\2\2\2\u0b03\u0b04\3\2\2\2\u0b04\u00e5\3\2\2\2\u0b05\u0b06\5\u00ec"+
		"w\2\u0b06\u0b07\5\u0114\u008b\2\u0b07\u0b09\3\2\2\2\u0b08\u0b05\3\2\2"+
		"\2\u0b09\u0b0a\3\2\2\2\u0b0a\u0b08\3\2\2\2\u0b0a\u0b0b\3\2\2\2\u0b0b\u00e7"+
		"\3\2\2\2\u0b0c\u0b0f\5\u00eav\2\u0b0d\u0b10\5\u00e6t\2\u0b0e\u0b10\5\u00ea"+
		"v\2\u0b0f\u0b0d\3\2\2\2\u0b0f\u0b0e\3\2\2\2\u0b0f\u0b10\3\2\2\2\u0b10"+
		"\u00e9\3\2\2\2\u0b11\u0b12\5\u00ecw\2\u0b12\u0b13\5\u0114\u008b\2\u0b13"+
		"\u0b14\7\u00f3\2\2\u0b14\u0b15\5\u0114\u008b\2\u0b15\u00eb\3\2\2\2\u0b16"+
		"\u0b18\t(\2\2\u0b17\u0b16\3\2\2\2\u0b17\u0b18\3\2\2\2\u0b18\u0b19\3\2"+
		"\2\2\u0b19\u0b1a\t)\2\2\u0b1a\u00ed\3\2\2\2\u0b1b\u0b1f\7d\2\2\u0b1c\u0b1d"+
		"\7\22\2\2\u0b1d\u0b1f\5\u0110\u0089\2\u0b1e\u0b1b\3\2\2\2\u0b1e\u0b1c"+
		"\3\2\2\2\u0b1f\u00ef\3\2\2\2\u0b20\u0b21\7\32\2\2\u0b21\u0b22\7\u0117"+
		"\2\2\u0b22\u0b23\5\u00f0y\2\u0b23\u0b24\7\u0119\2\2\u0b24\u0b4f\3\2\2"+
		"\2\u0b25\u0b26\7\u0095\2\2\u0b26\u0b27\7\u0117\2\2\u0b27\u0b28\5\u00f0"+
		"y\2\u0b28\u0b29\7\6\2\2\u0b29\u0b2a\5\u00f0y\2\u0b2a\u0b2b\7\u0119\2\2"+
		"\u0b2b\u0b4f\3\2\2\2\u0b2c\u0b33\7\u00e7\2\2\u0b2d\u0b2f\7\u0117\2\2\u0b2e"+
		"\u0b30\5\u00fa~\2\u0b2f\u0b2e\3\2\2\2\u0b2f\u0b30\3\2\2\2\u0b30\u0b31"+
		"\3\2\2\2\u0b31\u0b34\7\u0119\2\2\u0b32\u0b34\7\u0115\2\2\u0b33\u0b2d\3"+
		"\2\2\2\u0b33\u0b32\3\2\2\2\u0b34\u0b4f\3\2\2\2\u0b35\u0b36\7\177\2\2\u0b36"+
		"\u0b39\t*\2\2\u0b37\u0b38\7\u00f3\2\2\u0b38\u0b3a\7\u0099\2\2\u0b39\u0b37"+
		"\3\2\2\2\u0b39\u0b3a\3\2\2\2\u0b3a\u0b4f\3\2\2\2\u0b3b\u0b3c\7\177\2\2"+
		"\u0b3c\u0b3f\t+\2\2\u0b3d\u0b3e\7\u00f3\2\2\u0b3e\u0b40\t,\2\2\u0b3f\u0b3d"+
		"\3\2\2\2\u0b3f\u0b40\3\2\2\2\u0b40\u0b4f\3\2\2\2\u0b41\u0b4c\5\u0114\u008b"+
		"\2\u0b42\u0b43\7\4\2\2\u0b43\u0b48\7\u0129\2\2\u0b44\u0b45\7\6\2\2\u0b45"+
		"\u0b47\7\u0129\2\2\u0b46\u0b44\3\2\2\2\u0b47\u0b4a\3\2\2\2\u0b48\u0b46"+
		"\3\2\2\2\u0b48\u0b49\3\2\2\2\u0b49\u0b4b\3\2\2\2\u0b4a\u0b48\3\2\2\2\u0b4b"+
		"\u0b4d\7\5\2\2\u0b4c\u0b42\3\2\2\2\u0b4c\u0b4d\3\2\2\2\u0b4d\u0b4f\3\2"+
		"\2\2\u0b4e\u0b20\3\2\2\2\u0b4e\u0b25\3\2\2\2\u0b4e\u0b2c\3\2\2\2\u0b4e"+
		"\u0b35\3\2\2\2\u0b4e\u0b3b\3\2\2\2\u0b4e\u0b41\3\2\2\2\u0b4f\u00f1\3\2"+
		"\2\2\u0b50\u0b55\5\u00f4{\2\u0b51\u0b52\7\6\2\2\u0b52\u0b54\5\u00f4{\2"+
		"\u0b53\u0b51\3\2\2\2\u0b54\u0b57\3\2\2\2\u0b55\u0b53\3\2\2\2\u0b55\u0b56"+
		"\3\2\2\2\u0b56\u00f3\3\2\2\2\u0b57\u0b55\3\2\2\2\u0b58\u0b59\5\u00ba^"+
		"\2\u0b59\u0b5c\5\u00f0y\2\u0b5a\u0b5b\7\u009f\2\2\u0b5b\u0b5d\7\u00a0"+
		"\2\2\u0b5c\u0b5a\3\2\2\2\u0b5c\u0b5d\3\2\2\2\u0b5d\u0b5f\3\2\2\2\u0b5e"+
		"\u0b60\5&\24\2\u0b5f\u0b5e\3\2\2\2\u0b5f\u0b60\3\2\2\2\u0b60\u0b62\3\2"+
		"\2\2\u0b61\u0b63\5\u00eex\2\u0b62\u0b61\3\2\2\2\u0b62\u0b63\3\2\2\2\u0b63"+
		"\u00f5\3\2\2\2\u0b64\u0b69\5\u00f8}\2\u0b65\u0b66\7\6\2\2\u0b66\u0b68"+
		"\5\u00f8}\2\u0b67\u0b65\3\2\2\2\u0b68\u0b6b\3\2\2\2\u0b69\u0b67\3\2\2"+
		"\2\u0b69\u0b6a\3\2\2\2\u0b6a\u00f7\3\2\2\2\u0b6b\u0b69\3\2\2\2\u0b6c\u0b6d"+
		"\5\u0110\u0089\2\u0b6d\u0b70\5\u00f0y\2\u0b6e\u0b6f\7\u009f\2\2\u0b6f"+
		"\u0b71\7\u00a0\2\2\u0b70\u0b6e\3\2\2\2\u0b70\u0b71\3\2\2\2\u0b71\u0b73"+
		"\3\2\2\2\u0b72\u0b74\5&\24\2\u0b73\u0b72\3\2\2\2\u0b73\u0b74\3\2\2\2\u0b74"+
		"\u00f9\3\2\2\2\u0b75\u0b7a\5\u00fc\177\2\u0b76\u0b77\7\6\2\2\u0b77\u0b79"+
		"\5\u00fc\177\2\u0b78\u0b76\3\2\2\2\u0b79\u0b7c\3\2\2\2\u0b7a\u0b78\3\2"+
		"\2\2\u0b7a\u0b7b\3\2\2\2\u0b7b\u00fb\3\2\2\2\u0b7c\u0b7a\3\2\2\2\u0b7d"+
		"\u0b7f\5\u0114\u008b\2\u0b7e\u0b80\7\r\2\2\u0b7f\u0b7e\3\2\2\2\u0b7f\u0b80"+
		"\3\2\2\2\u0b80\u0b81\3\2\2\2\u0b81\u0b84\5\u00f0y\2\u0b82\u0b83\7\u009f"+
		"\2\2\u0b83\u0b85\7\u00a0\2\2\u0b84\u0b82\3\2\2\2\u0b84\u0b85\3\2\2\2\u0b85"+
		"\u0b87\3\2\2\2\u0b86\u0b88\5&\24\2\u0b87\u0b86\3\2\2\2\u0b87\u0b88\3\2"+
		"\2\2\u0b88\u00fd\3\2\2\2\u0b89\u0b8a\7\u010d\2\2\u0b8a\u0b8b\5\u00ccg"+
		"\2\u0b8b\u0b8c\7\u00f1\2\2\u0b8c\u0b8d\5\u00ccg\2\u0b8d\u00ff\3\2\2\2"+
		"\u0b8e\u0b8f\7\u010f\2\2\u0b8f\u0b94\5\u0102\u0082\2\u0b90\u0b91\7\6\2"+
		"\2\u0b91\u0b93\5\u0102\u0082\2\u0b92\u0b90\3\2\2\2\u0b93\u0b96\3\2\2\2"+
		"\u0b94\u0b92\3\2\2\2\u0b94\u0b95\3\2\2\2\u0b95\u0101\3\2\2\2\u0b96\u0b94"+
		"\3\2\2\2\u0b97\u0b98\5\u0110\u0089\2\u0b98\u0b99\7\33\2\2\u0b99\u0b9a"+
		"\5\u0104\u0083\2\u0b9a\u0103\3\2\2\2\u0b9b\u0bca\5\u0110\u0089\2\u0b9c"+
		"\u0b9d\7\4\2\2\u0b9d\u0b9e\5\u0110\u0089\2\u0b9e\u0b9f\7\5\2\2\u0b9f\u0bca"+
		"\3\2\2\2\u0ba0\u0bc3\7\4\2\2\u0ba1\u0ba2\7+\2\2\u0ba2\u0ba3\7#\2\2\u0ba3"+
		"\u0ba8\5\u00ccg\2\u0ba4\u0ba5\7\6\2\2\u0ba5\u0ba7\5\u00ccg\2\u0ba6\u0ba4"+
		"\3\2\2\2\u0ba7\u0baa\3\2\2\2\u0ba8\u0ba6\3\2\2\2\u0ba8\u0ba9\3\2\2\2\u0ba9"+
		"\u0bc4\3\2\2\2\u0baa\u0ba8\3\2\2\2\u0bab\u0bac\t-\2\2\u0bac\u0bad\7#\2"+
		"\2\u0bad\u0bb2\5\u00ccg\2\u0bae\u0baf\7\6\2\2\u0baf\u0bb1\5\u00ccg\2\u0bb0"+
		"\u0bae\3\2\2\2\u0bb1\u0bb4\3\2\2\2\u0bb2\u0bb0\3\2\2\2\u0bb2\u0bb3\3\2"+
		"\2\2\u0bb3\u0bb6\3\2\2\2\u0bb4\u0bb2\3\2\2\2\u0bb5\u0bab\3\2\2\2\u0bb5"+
		"\u0bb6\3\2\2\2\u0bb6\u0bc1\3\2\2\2\u0bb7\u0bb8\t.\2\2\u0bb8\u0bb9\7#\2"+
		"\2\u0bb9\u0bbe\5^\60\2\u0bba\u0bbb\7\6\2\2\u0bbb\u0bbd\5^\60\2\u0bbc\u0bba"+
		"\3\2\2\2\u0bbd\u0bc0\3\2\2\2\u0bbe\u0bbc\3\2\2\2\u0bbe\u0bbf\3\2\2\2\u0bbf"+
		"\u0bc2\3\2\2\2\u0bc0\u0bbe\3\2\2\2\u0bc1\u0bb7\3\2\2\2\u0bc1\u0bc2\3\2"+
		"\2\2\u0bc2\u0bc4\3\2\2\2\u0bc3\u0ba1\3\2\2\2\u0bc3\u0bb5\3\2\2\2\u0bc4"+
		"\u0bc6\3\2\2\2\u0bc5\u0bc7\5\u0106\u0084\2\u0bc6\u0bc5\3\2\2\2\u0bc6\u0bc7"+
		"\3\2\2\2\u0bc7\u0bc8\3\2\2\2\u0bc8\u0bca\7\5\2\2\u0bc9\u0b9b\3\2\2\2\u0bc9"+
		"\u0b9c\3\2\2\2\u0bc9\u0ba0\3\2\2\2\u0bca\u0105\3\2\2\2\u0bcb\u0bcc\7\u00bd"+
		"\2\2\u0bcc\u0bdc\5\u0108\u0085\2\u0bcd\u0bce\7\u00d2\2\2\u0bce\u0bdc\5"+
		"\u0108\u0085\2\u0bcf\u0bd0\7\u00bd\2\2\u0bd0\u0bd1\7\37\2\2\u0bd1\u0bd2"+
		"\5\u0108\u0085\2\u0bd2\u0bd3\7\26\2\2\u0bd3\u0bd4\5\u0108\u0085\2\u0bd4"+
		"\u0bdc\3\2\2\2\u0bd5\u0bd6\7\u00d2\2\2\u0bd6\u0bd7\7\37\2\2\u0bd7\u0bd8"+
		"\5\u0108\u0085\2\u0bd8\u0bd9\7\26\2\2\u0bd9\u0bda\5\u0108\u0085\2\u0bda"+
		"\u0bdc\3\2\2\2\u0bdb\u0bcb\3\2\2\2\u0bdb\u0bcd\3\2\2\2\u0bdb\u0bcf\3\2"+
		"\2\2\u0bdb\u0bd5\3\2\2\2\u0bdc\u0107\3\2\2\2\u0bdd\u0bde\7\u00ff\2\2\u0bde"+
		"\u0be5\t/\2\2\u0bdf\u0be0\7=\2\2\u0be0\u0be5\7\u00d1\2\2\u0be1\u0be2\5"+
		"\u00ccg\2\u0be2\u0be3\t/\2\2\u0be3\u0be5\3\2\2\2\u0be4\u0bdd\3\2\2\2\u0be4"+
		"\u0bdf\3\2\2\2\u0be4\u0be1\3\2\2\2\u0be5\u0109\3\2\2\2\u0be6\u0beb\5\u010e"+
		"\u0088\2\u0be7\u0be8\7\6\2\2\u0be8\u0bea\5\u010e\u0088\2\u0be9\u0be7\3"+
		"\2\2\2\u0bea\u0bed\3\2\2\2\u0beb\u0be9\3\2\2\2\u0beb\u0bec\3\2\2\2\u0bec"+
		"\u010b\3\2\2\2\u0bed\u0beb\3\2\2\2\u0bee\u0bf3\5\u010e\u0088\2\u0bef\u0bf3"+
		"\7b\2\2\u0bf0\u0bf3\7\u0089\2\2\u0bf1\u0bf3\7\u00cb\2\2\u0bf2\u0bee\3"+
		"\2\2\2\u0bf2\u0bef\3\2\2\2\u0bf2\u0bf0\3\2\2\2\u0bf2\u0bf1\3\2\2\2\u0bf3"+
		"\u010d\3\2\2\2\u0bf4\u0bf9\5\u0114\u008b\2\u0bf5\u0bf6\7\7\2\2\u0bf6\u0bf8"+
		"\5\u0114\u008b\2\u0bf7\u0bf5\3\2\2\2\u0bf8\u0bfb\3\2\2\2\u0bf9\u0bf7\3"+
		"\2\2\2\u0bf9\u0bfa\3\2\2\2\u0bfa\u010f\3\2\2\2\u0bfb\u0bf9\3\2\2\2\u0bfc"+
		"\u0bfd\5\u0114\u008b\2\u0bfd\u0bfe\5\u0112\u008a\2\u0bfe\u0111\3\2\2\2"+
		"\u0bff\u0c00\7\u011c\2\2\u0c00\u0c02\5\u0114\u008b\2\u0c01\u0bff\3\2\2"+
		"\2\u0c02\u0c03\3\2\2\2\u0c03\u0c01\3\2\2\2\u0c03\u0c04\3\2\2\2\u0c04\u0c07"+
		"\3\2\2\2\u0c05\u0c07\3\2\2\2\u0c06\u0c01\3\2\2\2\u0c06\u0c05\3\2\2\2\u0c07"+
		"\u0113\3\2\2\2\u0c08\u0c0c\5\u0116\u008c\2\u0c09\u0c0a\6\u008b\22\2\u0c0a"+
		"\u0c0c\5\u0120\u0091\2\u0c0b\u0c08\3\2\2\2\u0c0b\u0c09\3\2\2\2\u0c0c\u0115"+
		"\3\2\2\2\u0c0d\u0c14\7\u012f\2\2\u0c0e\u0c14\5\u0118\u008d\2\u0c0f\u0c10"+
		"\6\u008c\23\2\u0c10\u0c14\5\u011e\u0090\2\u0c11\u0c12\6\u008c\24\2\u0c12"+
		"\u0c14\5\u0122\u0092\2\u0c13\u0c0d\3\2\2\2\u0c13\u0c0e\3\2\2\2\u0c13\u0c0f"+
		"\3\2\2\2\u0c13\u0c11\3\2\2\2\u0c14\u0117\3\2\2\2\u0c15\u0c16\7\u0130\2"+
		"\2\u0c16\u0119\3\2\2\2\u0c17\u0c19\6\u008e\25\2\u0c18\u0c1a\7\u011c\2"+
		"\2\u0c19\u0c18\3\2\2\2\u0c19\u0c1a\3\2\2\2\u0c1a\u0c1b\3\2\2\2\u0c1b\u0c43"+
		"\7\u012a\2\2\u0c1c\u0c1e\6\u008e\26\2\u0c1d\u0c1f\7\u011c\2\2\u0c1e\u0c1d"+
		"\3\2\2\2\u0c1e\u0c1f\3\2\2\2\u0c1f\u0c20\3\2\2\2\u0c20\u0c43\7\u012b\2"+
		"\2\u0c21\u0c23\6\u008e\27\2\u0c22\u0c24\7\u011c\2\2\u0c23\u0c22\3\2\2"+
		"\2\u0c23\u0c24\3\2\2\2\u0c24\u0c25\3\2\2\2\u0c25\u0c43\t\60\2\2\u0c26"+
		"\u0c28\7\u011c\2\2\u0c27\u0c26\3\2\2\2\u0c27\u0c28\3\2\2\2\u0c28\u0c29"+
		"\3\2\2\2\u0c29\u0c43\7\u0129\2\2\u0c2a\u0c2c\7\u011c\2\2\u0c2b\u0c2a\3"+
		"\2\2\2\u0c2b\u0c2c\3\2\2\2\u0c2c\u0c2d\3\2\2\2\u0c2d\u0c43\7\u0126\2\2"+
		"\u0c2e\u0c30\7\u011c\2\2\u0c2f\u0c2e\3\2\2\2\u0c2f\u0c30\3\2\2\2\u0c30"+
		"\u0c31\3\2\2\2\u0c31\u0c43\7\u0127\2\2\u0c32\u0c34\7\u011c\2\2\u0c33\u0c32"+
		"\3\2\2\2\u0c33\u0c34\3\2\2\2\u0c34\u0c35\3\2\2\2\u0c35\u0c43\7\u0128\2"+
		"\2\u0c36\u0c38\7\u011c\2\2\u0c37\u0c36\3\2\2\2\u0c37\u0c38\3\2\2\2\u0c38"+
		"\u0c39\3\2\2\2\u0c39\u0c43\7\u012d\2\2\u0c3a\u0c3c\7\u011c\2\2\u0c3b\u0c3a"+
		"\3\2\2\2\u0c3b\u0c3c\3\2\2\2\u0c3c\u0c3d\3\2\2\2\u0c3d\u0c43\7\u012c\2"+
		"\2\u0c3e\u0c40\7\u011c\2\2\u0c3f\u0c3e\3\2\2\2\u0c3f\u0c40\3\2\2\2\u0c40"+
		"\u0c41\3\2\2\2\u0c41\u0c43\7\u012e\2\2\u0c42\u0c17\3\2\2\2\u0c42\u0c1c"+
		"\3\2\2\2\u0c42\u0c21\3\2\2\2\u0c42\u0c27\3\2\2\2\u0c42\u0c2b\3\2\2\2\u0c42"+
		"\u0c2f\3\2\2\2\u0c42\u0c33\3\2\2\2\u0c42\u0c37\3\2\2\2\u0c42\u0c3b\3\2"+
		"\2\2\u0c42\u0c3f\3\2\2\2\u0c43\u011b\3\2\2\2\u0c44\u0c45\7\u00fd\2\2\u0c45"+
		"\u0c4c\5\u00f0y\2\u0c46\u0c4c\5&\24\2\u0c47\u0c4c\5\u00eex\2\u0c48\u0c49"+
		"\t\61\2\2\u0c49\u0c4a\7\u009f\2\2\u0c4a\u0c4c\7\u00a0\2\2\u0c4b\u0c44"+
		"\3\2\2\2\u0c4b\u0c46\3\2\2\2\u0c4b\u0c47\3\2\2\2\u0c4b\u0c48\3\2\2\2\u0c4c"+
		"\u011d\3\2\2\2\u0c4d\u0c4e\t\62\2\2\u0c4e\u011f\3\2\2\2\u0c4f\u0c50\t"+
		"\63\2\2\u0c50\u0121\3\2\2\2\u0c51\u0c52\t\64\2\2\u0c52\u0123\3\2\2\2\u01a1"+
		"\u0130\u0149\u014e\u0156\u015e\u0160\u0174\u0178\u017e\u0181\u0184\u018b"+
		"\u018e\u0192\u0195\u019c\u01a7\u01a9\u01b1\u01b4\u01b8\u01bb\u01c1\u01cc"+
		"\u01d2\u01d7\u0217\u0220\u0224\u022a\u022e\u0233\u0239\u0245\u024d\u0253"+
		"\u0260\u0265\u0275\u027c\u0280\u0286\u0295\u0299\u029f\u02a5\u02a8\u02ab"+
		"\u02b1\u02b5\u02bd\u02bf\u02c8\u02cb\u02d4\u02d9\u02df\u02e6\u02e9\u02ef"+
		"\u02fa\u02fd\u0301\u0306\u030b\u0312\u0315\u0318\u031f\u0324\u032d\u0335"+
		"\u033b\u033e\u0341\u0347\u034b\u034f\u0353\u0355\u035d\u0365\u036b\u0371"+
		"\u0374\u0378\u037b\u037f\u039b\u039e\u03a2\u03a8\u03ab\u03ae\u03b4\u03bc"+
		"\u03c1\u03c7\u03cd\u03d5\u03dc\u03e4\u03f5\u0403\u0406\u040c\u0415\u041e"+
		"\u0425\u0428\u0434\u0438\u043f\u04b3\u04bb\u04c3\u04cc\u04d6\u04da\u04dd"+
		"\u04e3\u04e9\u04f5\u0501\u0506\u050f\u0517\u051e\u0520\u0523\u0528\u052c"+
		"\u0531\u0534\u0539\u053e\u0541\u0546\u054a\u054f\u0551\u0555\u055e\u0566"+
		"\u056f\u0576\u057f\u0584\u0587\u059d\u059f\u05a8\u05af\u05b2\u05b9\u05bd"+
		"\u05c3\u05cb\u05d6\u05e1\u05e8\u05ee\u05fb\u0602\u0609\u0615\u061d\u0623"+
		"\u0626\u062f\u0632\u063b\u063e\u0647\u064a\u0653\u0656\u0659\u065e\u0660"+
		"\u066c\u0673\u067a\u067d\u067f\u068b\u068f\u0693\u0699\u069d\u06a5\u06a9"+
		"\u06ac\u06af\u06b2\u06b6\u06ba\u06bf\u06c3\u06c6\u06c9\u06cc\u06d0\u06d5"+
		"\u06d9\u06dc\u06df\u06e2\u06e4\u06ea\u06f1\u06f6\u06f9\u06fc\u0700\u070a"+
		"\u070e\u0710\u0713\u0717\u071d\u0721\u072c\u0736\u0742\u0751\u0756\u075d"+
		"\u076d\u0772\u077f\u0784\u078c\u0792\u0796\u079f\u07a9\u07b8\u07bd\u07bf"+
		"\u07c3\u07cc\u07d9\u07de\u07e2\u07ea\u07ed\u07f1\u07ff\u080c\u0811\u0815"+
		"\u0818\u081d\u0826\u0829\u082e\u0835\u0838\u083d\u0843\u0849\u084d\u0853"+
		"\u0857\u085a\u085f\u0862\u0867\u086b\u086e\u0871\u0877\u087c\u0881\u0893"+
		"\u0895\u0898\u08a3\u08ac\u08b3\u08bb\u08c2\u08c6\u08ce\u08d6\u08dc\u08e4"+
		"\u08f0\u08f3\u08f9\u08fd\u08ff\u0908\u0914\u0916\u091d\u0924\u092a\u0930"+
		"\u0932\u0939\u0941\u0947\u094e\u0954\u0958\u095a\u0961\u096a\u0971\u097b"+
		"\u0980\u0984\u098d\u099a\u099c\u09a4\u09a6\u09aa\u09b2\u09bb\u09c1\u09c9"+
		"\u09ce\u09da\u09df\u09e2\u09e8\u09ec\u09f1\u09f6\u09fb\u0a01\u0a16\u0a18"+
		"\u0a21\u0a25\u0a2e\u0a32\u0a44\u0a47\u0a4f\u0a58\u0a6f\u0a7a\u0a81\u0a84"+
		"\u0a8d\u0a91\u0a95\u0aa1\u0aba\u0ac1\u0ac4\u0ad3\u0ad7\u0ae1\u0ae3\u0af0"+
		"\u0af2\u0aff\u0b03\u0b0a\u0b0f\u0b17\u0b1e\u0b2f\u0b33\u0b39\u0b3f\u0b48"+
		"\u0b4c\u0b4e\u0b55\u0b5c\u0b5f\u0b62\u0b69\u0b70\u0b73\u0b7a\u0b7f\u0b84"+
		"\u0b87\u0b94\u0ba8\u0bb2\u0bb5\u0bbe\u0bc1\u0bc3\u0bc6\u0bc9\u0bdb\u0be4"+
		"\u0beb\u0bf2\u0bf9\u0c03\u0c06\u0c0b\u0c13\u0c19\u0c1e\u0c23\u0c27\u0c2b"+
		"\u0c2f\u0c33\u0c37\u0c3b\u0c3f\u0c42\u0c4b";
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