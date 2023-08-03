package com.netease.arctic.spark.sql.parser;// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlExtend.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ArcticSqlExtendParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEMICOLON=1, LEFT_PAREN=2, RIGHT_PAREN=3, COMMA=4, DOT=5, LEFT_BRACKET=6, 
		RIGHT_BRACKET=7, ADD=8, AFTER=9, ALL=10, ALTER=11, ANALYZE=12, AND=13, 
		ANTI=14, ANY=15, ARCHIVE=16, ARRAY=17, AS=18, ASC=19, AT=20, AUTHORIZATION=21, 
		BETWEEN=22, BOTH=23, BUCKET=24, BUCKETS=25, BY=26, CACHE=27, CASCADE=28, 
		CASE=29, CAST=30, CATALOG=31, CATALOGS=32, CHANGE=33, CHECK=34, CLEAR=35, 
		CLUSTER=36, CLUSTERED=37, CODEGEN=38, COLLATE=39, COLLECTION=40, COLUMN=41, 
		COLUMNS=42, COMMENT=43, COMMIT=44, COMPACT=45, COMPACTIONS=46, COMPUTE=47, 
		CONCATENATE=48, CONSTRAINT=49, COST=50, CREATE=51, CROSS=52, CUBE=53, 
		CURRENT=54, CURRENT_DATE=55, CURRENT_TIME=56, CURRENT_TIMESTAMP=57, CURRENT_USER=58, 
		DAY=59, DAYOFYEAR=60, DATA=61, DATABASE=62, DATABASES=63, DATEADD=64, 
		DATEDIFF=65, DBPROPERTIES=66, DEFINED=67, DELETE=68, DELIMITED=69, DESC=70, 
		DESCRIBE=71, DFS=72, DIRECTORIES=73, DIRECTORY=74, DISTINCT=75, DISTRIBUTE=76, 
		DIV=77, DROP=78, ELSE=79, END=80, ESCAPE=81, ESCAPED=82, EXCEPT=83, EXCHANGE=84, 
		EXISTS=85, EXPLAIN=86, EXPORT=87, EXTENDED=88, EXTERNAL=89, EXTRACT=90, 
		FALSE=91, FETCH=92, FIELDS=93, FILTER=94, FILEFORMAT=95, FIRST=96, FOLLOWING=97, 
		FOR=98, FOREIGN=99, FORMAT=100, FORMATTED=101, FROM=102, FULL=103, FUNCTION=104, 
		FUNCTIONS=105, GLOBAL=106, GRANT=107, GROUP=108, GROUPING=109, HAVING=110, 
		HOUR=111, IF=112, IGNORE=113, IMPORT=114, IN=115, INDEX=116, INDEXES=117, 
		INNER=118, INPATH=119, INPUTFORMAT=120, INSERT=121, INTERSECT=122, INTERVAL=123, 
		INTO=124, IS=125, ITEMS=126, JOIN=127, KEYS=128, LAST=129, LATERAL=130, 
		LAZY=131, LEADING=132, LEFT=133, LIKE=134, ILIKE=135, LIMIT=136, LINES=137, 
		LIST=138, LOAD=139, LOCAL=140, LOCATION=141, LOCK=142, LOCKS=143, LOGICAL=144, 
		MACRO=145, MAP=146, MATCHED=147, MERGE=148, MICROSECOND=149, MILLISECOND=150, 
		MINUTE=151, MONTH=152, MSCK=153, NAMESPACE=154, NAMESPACES=155, NATURAL=156, 
		NO=157, NOT=158, NULL=159, NULLS=160, OF=161, ON=162, ONLY=163, OPTION=164, 
		OPTIONS=165, OR=166, ORDER=167, OUT=168, OUTER=169, OUTPUTFORMAT=170, 
		OVER=171, OVERLAPS=172, OVERLAY=173, OVERWRITE=174, PARTITION=175, PARTITIONED=176, 
		PARTITIONS=177, PERCENTILE_CONT=178, PERCENTILE_DISC=179, PERCENTLIT=180, 
		PIVOT=181, PLACING=182, POSITION=183, PRECEDING=184, PRIMARY=185, PRINCIPALS=186, 
		PROPERTIES=187, PURGE=188, QUARTER=189, QUERY=190, RANGE=191, RECORDREADER=192, 
		RECORDWRITER=193, RECOVER=194, REDUCE=195, REFERENCES=196, REFRESH=197, 
		RENAME=198, REPAIR=199, REPEATABLE=200, REPLACE=201, RESET=202, RESPECT=203, 
		RESTRICT=204, REVOKE=205, RIGHT=206, RLIKE=207, ROLE=208, ROLES=209, ROLLBACK=210, 
		ROLLUP=211, ROW=212, ROWS=213, SECOND=214, SCHEMA=215, SCHEMAS=216, SELECT=217, 
		SEMI=218, SEPARATED=219, SERDE=220, SERDEPROPERTIES=221, SESSION_USER=222, 
		SET=223, SETMINUS=224, SETS=225, SHOW=226, SKEWED=227, SOME=228, SORT=229, 
		SORTED=230, START=231, STATISTICS=232, STORED=233, STRATIFY=234, STRUCT=235, 
		SUBSTR=236, SUBSTRING=237, SYNC=238, SYSTEM_TIME=239, SYSTEM_VERSION=240, 
		TABLE=241, TABLES=242, TABLESAMPLE=243, TBLPROPERTIES=244, TEMPORARY=245, 
		TERMINATED=246, THEN=247, TIME=248, TIMESTAMP=249, TIMESTAMPADD=250, TIMESTAMPDIFF=251, 
		TO=252, TOUCH=253, TRAILING=254, TRANSACTION=255, TRANSACTIONS=256, TRANSFORM=257, 
		TRIM=258, TRUE=259, TRUNCATE=260, TRY_CAST=261, TYPE=262, UNARCHIVE=263, 
		UNBOUNDED=264, UNCACHE=265, UNION=266, UNIQUE=267, UNKNOWN=268, UNLOCK=269, 
		UNSET=270, UPDATE=271, USE=272, USER=273, USING=274, VALUES=275, VERSION=276, 
		VIEW=277, VIEWS=278, WEEK=279, WHEN=280, WHERE=281, WINDOW=282, WITH=283, 
		WITHIN=284, YEAR=285, ZONE=286, KEY=287, EQ=288, NSEQ=289, NEQ=290, NEQJ=291, 
		LT=292, LTE=293, GT=294, GTE=295, PLUS=296, MINUS=297, ASTERISK=298, SLASH=299, 
		PERCENT=300, TILDE=301, AMPERSAND=302, PIPE=303, CONCAT_PIPE=304, HAT=305, 
		COLON=306, ARROW=307, HENT_START=308, HENT_END=309, STRING=310, BIGINT_LITERAL=311, 
		SMALLINT_LITERAL=312, TINYINT_LITERAL=313, INTEGER_VALUE=314, EXPONENT_VALUE=315, 
		DECIMAL_VALUE=316, FLOAT_LITERAL=317, DOUBLE_LITERAL=318, BIGDECIMAL_LITERAL=319, 
		IDENTIFIER=320, BACKQUOTED_IDENTIFIER=321, SIMPLE_COMMENT=322, BRACKETED_COMMENT=323, 
		WS=324, UNRECOGNIZED=325;
	public static final int
		RULE_extendStatement = 0, RULE_statement = 1, RULE_createTableHeader = 2, 
		RULE_colListAndPk = 3, RULE_primarySpec = 4, RULE_bucketSpec = 5, RULE_skewSpec = 6, 
		RULE_locationSpec = 7, RULE_commentSpec = 8, RULE_query = 9, RULE_ctes = 10, 
		RULE_namedQuery = 11, RULE_tableProvider = 12, RULE_createTableClauses = 13, 
		RULE_propertyList = 14, RULE_property = 15, RULE_propertyKey = 16, RULE_propertyValue = 17, 
		RULE_constantList = 18, RULE_nestedConstantList = 19, RULE_createFileFormat = 20, 
		RULE_fileFormat = 21, RULE_storageHandler = 22, RULE_queryOrganization = 23, 
		RULE_queryTerm = 24, RULE_queryPrimary = 25, RULE_sortItem = 26, RULE_fromStatement = 27, 
		RULE_fromStatementBody = 28, RULE_querySpecification = 29, RULE_transformClause = 30, 
		RULE_selectClause = 31, RULE_whereClause = 32, RULE_havingClause = 33, 
		RULE_hint = 34, RULE_hintStatement = 35, RULE_fromClause = 36, RULE_temporalClause = 37, 
		RULE_aggregationClause = 38, RULE_groupByClause = 39, RULE_groupingAnalytics = 40, 
		RULE_groupingElement = 41, RULE_groupingSet = 42, RULE_pivotClause = 43, 
		RULE_pivotColumn = 44, RULE_pivotValue = 45, RULE_lateralView = 46, RULE_setQuantifier = 47, 
		RULE_relation = 48, RULE_joinRelation = 49, RULE_joinType = 50, RULE_joinCriteria = 51, 
		RULE_sample = 52, RULE_sampleMethod = 53, RULE_identifierList = 54, RULE_identifierSeq = 55, 
		RULE_orderedIdentifierList = 56, RULE_orderedIdentifier = 57, RULE_relationPrimary = 58, 
		RULE_inlineTable = 59, RULE_functionTable = 60, RULE_tableAlias = 61, 
		RULE_rowFormat = 62, RULE_multipartIdentifier = 63, RULE_namedExpression = 64, 
		RULE_namedExpressionSeq = 65, RULE_partitionFieldList = 66, RULE_partitionField = 67, 
		RULE_transform = 68, RULE_transformArgument = 69, RULE_expression = 70, 
		RULE_expressionSeq = 71, RULE_booleanExpression = 72, RULE_predicate = 73, 
		RULE_valueExpression = 74, RULE_datetimeUnit = 75, RULE_primaryExpression = 76, 
		RULE_constant = 77, RULE_comparisonOperator = 78, RULE_booleanValue = 79, 
		RULE_interval = 80, RULE_errorCapturingMultiUnitsInterval = 81, RULE_multiUnitsInterval = 82, 
		RULE_errorCapturingUnitToUnitInterval = 83, RULE_unitToUnitInterval = 84, 
		RULE_intervalValue = 85, RULE_dataType = 86, RULE_colTypeList = 87, RULE_colType = 88, 
		RULE_complexColTypeList = 89, RULE_complexColType = 90, RULE_whenClause = 91, 
		RULE_windowClause = 92, RULE_namedWindow = 93, RULE_windowSpec = 94, RULE_windowFrame = 95, 
		RULE_frameBound = 96, RULE_functionName = 97, RULE_qualifiedName = 98, 
		RULE_errorCapturingIdentifier = 99, RULE_errorCapturingIdentifierExtra = 100, 
		RULE_identifier = 101, RULE_strictIdentifier = 102, RULE_quotedIdentifier = 103, 
		RULE_number = 104, RULE_ansiNonReserved = 105, RULE_strictNonReserved = 106, 
		RULE_nonReserved = 107;
	private static String[] makeRuleNames() {
		return new String[] {
			"extendStatement", "statement", "createTableHeader", "colListAndPk", 
			"primarySpec", "bucketSpec", "skewSpec", "locationSpec", "commentSpec", 
			"query", "ctes", "namedQuery", "tableProvider", "createTableClauses", 
			"propertyList", "property", "propertyKey", "propertyValue", "constantList", 
			"nestedConstantList", "createFileFormat", "fileFormat", "storageHandler", 
			"queryOrganization", "queryTerm", "queryPrimary", "sortItem", "fromStatement", 
			"fromStatementBody", "querySpecification", "transformClause", "selectClause", 
			"whereClause", "havingClause", "hint", "hintStatement", "fromClause", 
			"temporalClause", "aggregationClause", "groupByClause", "groupingAnalytics", 
			"groupingElement", "groupingSet", "pivotClause", "pivotColumn", "pivotValue", 
			"lateralView", "setQuantifier", "relation", "joinRelation", "joinType", 
			"joinCriteria", "sample", "sampleMethod", "identifierList", "identifierSeq", 
			"orderedIdentifierList", "orderedIdentifier", "relationPrimary", "inlineTable", 
			"functionTable", "tableAlias", "rowFormat", "multipartIdentifier", "namedExpression", 
			"namedExpressionSeq", "partitionFieldList", "partitionField", "transform", 
			"transformArgument", "expression", "expressionSeq", "booleanExpression", 
			"predicate", "valueExpression", "datetimeUnit", "primaryExpression", 
			"constant", "comparisonOperator", "booleanValue", "interval", "errorCapturingMultiUnitsInterval", 
			"multiUnitsInterval", "errorCapturingUnitToUnitInterval", "unitToUnitInterval", 
			"intervalValue", "dataType", "colTypeList", "colType", "complexColTypeList", 
			"complexColType", "whenClause", "windowClause", "namedWindow", "windowSpec", 
			"windowFrame", "frameBound", "functionName", "qualifiedName", "errorCapturingIdentifier", 
			"errorCapturingIdentifierExtra", "identifier", "strictIdentifier", "quotedIdentifier", 
			"number", "ansiNonReserved", "strictNonReserved", "nonReserved"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'('", "')'", "','", "'.'", "'['", "']'", "'ADD'", "'AFTER'", 
			"'ALL'", "'ALTER'", "'ANALYZE'", "'AND'", "'ANTI'", "'ANY'", "'ARCHIVE'", 
			"'ARRAY'", "'AS'", "'ASC'", "'AT'", "'AUTHORIZATION'", "'BETWEEN'", "'BOTH'", 
			"'BUCKET'", "'BUCKETS'", "'BY'", "'CACHE'", "'CASCADE'", "'CASE'", "'CAST'", 
			"'CATALOG'", "'CATALOGS'", "'CHANGE'", "'CHECK'", "'CLEAR'", "'CLUSTER'", 
			"'CLUSTERED'", "'CODEGEN'", "'COLLATE'", "'COLLECTION'", "'COLUMN'", 
			"'COLUMNS'", "'COMMENT'", "'COMMIT'", "'COMPACT'", "'COMPACTIONS'", "'COMPUTE'", 
			"'CONCATENATE'", "'CONSTRAINT'", "'COST'", "'CREATE'", "'CROSS'", "'CUBE'", 
			"'CURRENT'", "'CURRENT_DATE'", "'CURRENT_TIME'", "'CURRENT_TIMESTAMP'", 
			"'CURRENT_USER'", "'DAY'", "'DAYOFYEAR'", "'DATA'", "'DATABASE'", "'DATABASES'", 
			"'DATEADD'", "'DATEDIFF'", "'DBPROPERTIES'", "'DEFINED'", "'DELETE'", 
			"'DELIMITED'", "'DESC'", "'DESCRIBE'", "'DFS'", "'DIRECTORIES'", "'DIRECTORY'", 
			"'DISTINCT'", "'DISTRIBUTE'", "'DIV'", "'DROP'", "'ELSE'", "'END'", "'ESCAPE'", 
			"'ESCAPED'", "'EXCEPT'", "'EXCHANGE'", "'EXISTS'", "'EXPLAIN'", "'EXPORT'", 
			"'EXTENDED'", "'EXTERNAL'", "'EXTRACT'", "'FALSE'", "'FETCH'", "'FIELDS'", 
			"'FILTER'", "'FILEFORMAT'", "'FIRST'", "'FOLLOWING'", "'FOR'", "'FOREIGN'", 
			"'FORMAT'", "'FORMATTED'", "'FROM'", "'FULL'", "'FUNCTION'", "'FUNCTIONS'", 
			"'GLOBAL'", "'GRANT'", "'GROUP'", "'GROUPING'", "'HAVING'", "'HOUR'", 
			"'IF'", "'IGNORE'", "'IMPORT'", "'IN'", "'INDEX'", "'INDEXES'", "'INNER'", 
			"'INPATH'", "'INPUTFORMAT'", "'INSERT'", "'INTERSECT'", "'INTERVAL'", 
			"'INTO'", "'IS'", "'ITEMS'", "'JOIN'", "'KEYS'", "'LAST'", "'LATERAL'", 
			"'LAZY'", "'LEADING'", "'LEFT'", "'LIKE'", "'ILIKE'", "'LIMIT'", "'LINES'", 
			"'LIST'", "'LOAD'", "'LOCAL'", "'LOCATION'", "'LOCK'", "'LOCKS'", "'LOGICAL'", 
			"'MACRO'", "'MAP'", "'MATCHED'", "'MERGE'", "'MICROSECOND'", "'MILLISECOND'", 
			"'MINUTE'", "'MONTH'", "'MSCK'", "'NAMESPACE'", "'NAMESPACES'", "'NATURAL'", 
			"'NO'", null, "'NULL'", "'NULLS'", "'OF'", "'ON'", "'ONLY'", "'OPTION'", 
			"'OPTIONS'", "'OR'", "'ORDER'", "'OUT'", "'OUTER'", "'OUTPUTFORMAT'", 
			"'OVER'", "'OVERLAPS'", "'OVERLAY'", "'OVERWRITE'", "'PARTITION'", "'PARTITIONED'", 
			"'PARTITIONS'", "'PERCENTILE_CONT'", "'PERCENTILE_DISC'", "'PERCENT'", 
			"'PIVOT'", "'PLACING'", "'POSITION'", "'PRECEDING'", "'PRIMARY'", "'PRINCIPALS'", 
			"'PROPERTIES'", "'PURGE'", "'QUARTER'", "'QUERY'", "'RANGE'", "'RECORDREADER'", 
			"'RECORDWRITER'", "'RECOVER'", "'REDUCE'", "'REFERENCES'", "'REFRESH'", 
			"'RENAME'", "'REPAIR'", "'REPEATABLE'", "'REPLACE'", "'RESET'", "'RESPECT'", 
			"'RESTRICT'", "'REVOKE'", "'RIGHT'", null, "'ROLE'", "'ROLES'", "'ROLLBACK'", 
			"'ROLLUP'", "'ROW'", "'ROWS'", "'SECOND'", "'SCHEMA'", "'SCHEMAS'", "'SELECT'", 
			"'SEMI'", "'SEPARATED'", "'SERDE'", "'SERDEPROPERTIES'", "'SESSION_USER'", 
			"'SET'", "'MINUS'", "'SETS'", "'SHOW'", "'SKEWED'", "'SOME'", "'SORT'", 
			"'SORTED'", "'START'", "'STATISTICS'", "'STORED'", "'STRATIFY'", "'STRUCT'", 
			"'SUBSTR'", "'SUBSTRING'", "'SYNC'", "'SYSTEM_TIME'", "'SYSTEM_VERSION'", 
			"'TABLE'", "'TABLES'", "'TABLESAMPLE'", "'TBLPROPERTIES'", null, "'TERMINATED'", 
			"'THEN'", "'TIME'", "'TIMESTAMP'", "'TIMESTAMPADD'", "'TIMESTAMPDIFF'", 
			"'TO'", "'TOUCH'", "'TRAILING'", "'TRANSACTION'", "'TRANSACTIONS'", "'TRANSFORM'", 
			"'TRIM'", "'TRUE'", "'TRUNCATE'", "'TRY_CAST'", "'TYPE'", "'UNARCHIVE'", 
			"'UNBOUNDED'", "'UNCACHE'", "'UNION'", "'UNIQUE'", "'UNKNOWN'", "'UNLOCK'", 
			"'UNSET'", "'UPDATE'", "'USE'", "'USER'", "'USING'", "'VALUES'", "'VERSION'", 
			"'VIEW'", "'VIEWS'", "'WEEK'", "'WHEN'", "'WHERE'", "'WINDOW'", "'WITH'", 
			"'WITHIN'", "'YEAR'", "'ZONE'", "'KEY'", null, "'<=>'", "'<>'", "'!='", 
			"'<'", null, "'>'", null, "'+'", "'-'", "'*'", "'/'", "'%'", "'~'", "'&'", 
			"'|'", "'||'", "'^'", "':'", "'->'", "'/*+'", "'*/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEMICOLON", "LEFT_PAREN", "RIGHT_PAREN", "COMMA", "DOT", "LEFT_BRACKET", 
			"RIGHT_BRACKET", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "ANTI", 
			"ANY", "ARCHIVE", "ARRAY", "AS", "ASC", "AT", "AUTHORIZATION", "BETWEEN", 
			"BOTH", "BUCKET", "BUCKETS", "BY", "CACHE", "CASCADE", "CASE", "CAST", 
			"CATALOG", "CATALOGS", "CHANGE", "CHECK", "CLEAR", "CLUSTER", "CLUSTERED", 
			"CODEGEN", "COLLATE", "COLLECTION", "COLUMN", "COLUMNS", "COMMENT", "COMMIT", 
			"COMPACT", "COMPACTIONS", "COMPUTE", "CONCATENATE", "CONSTRAINT", "COST", 
			"CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", 
			"CURRENT_TIMESTAMP", "CURRENT_USER", "DAY", "DAYOFYEAR", "DATA", "DATABASE", 
			"DATABASES", "DATEADD", "DATEDIFF", "DBPROPERTIES", "DEFINED", "DELETE", 
			"DELIMITED", "DESC", "DESCRIBE", "DFS", "DIRECTORIES", "DIRECTORY", "DISTINCT", 
			"DISTRIBUTE", "DIV", "DROP", "ELSE", "END", "ESCAPE", "ESCAPED", "EXCEPT", 
			"EXCHANGE", "EXISTS", "EXPLAIN", "EXPORT", "EXTENDED", "EXTERNAL", "EXTRACT", 
			"FALSE", "FETCH", "FIELDS", "FILTER", "FILEFORMAT", "FIRST", "FOLLOWING", 
			"FOR", "FOREIGN", "FORMAT", "FORMATTED", "FROM", "FULL", "FUNCTION", 
			"FUNCTIONS", "GLOBAL", "GRANT", "GROUP", "GROUPING", "HAVING", "HOUR", 
			"IF", "IGNORE", "IMPORT", "IN", "INDEX", "INDEXES", "INNER", "INPATH", 
			"INPUTFORMAT", "INSERT", "INTERSECT", "INTERVAL", "INTO", "IS", "ITEMS", 
			"JOIN", "KEYS", "LAST", "LATERAL", "LAZY", "LEADING", "LEFT", "LIKE", 
			"ILIKE", "LIMIT", "LINES", "LIST", "LOAD", "LOCAL", "LOCATION", "LOCK", 
			"LOCKS", "LOGICAL", "MACRO", "MAP", "MATCHED", "MERGE", "MICROSECOND", 
			"MILLISECOND", "MINUTE", "MONTH", "MSCK", "NAMESPACE", "NAMESPACES", 
			"NATURAL", "NO", "NOT", "NULL", "NULLS", "OF", "ON", "ONLY", "OPTION", 
			"OPTIONS", "OR", "ORDER", "OUT", "OUTER", "OUTPUTFORMAT", "OVER", "OVERLAPS", 
			"OVERLAY", "OVERWRITE", "PARTITION", "PARTITIONED", "PARTITIONS", "PERCENTILE_CONT", 
			"PERCENTILE_DISC", "PERCENTLIT", "PIVOT", "PLACING", "POSITION", "PRECEDING", 
			"PRIMARY", "PRINCIPALS", "PROPERTIES", "PURGE", "QUARTER", "QUERY", "RANGE", 
			"RECORDREADER", "RECORDWRITER", "RECOVER", "REDUCE", "REFERENCES", "REFRESH", 
			"RENAME", "REPAIR", "REPEATABLE", "REPLACE", "RESET", "RESPECT", "RESTRICT", 
			"REVOKE", "RIGHT", "RLIKE", "ROLE", "ROLES", "ROLLBACK", "ROLLUP", "ROW", 
			"ROWS", "SECOND", "SCHEMA", "SCHEMAS", "SELECT", "SEMI", "SEPARATED", 
			"SERDE", "SERDEPROPERTIES", "SESSION_USER", "SET", "SETMINUS", "SETS", 
			"SHOW", "SKEWED", "SOME", "SORT", "SORTED", "START", "STATISTICS", "STORED", 
			"STRATIFY", "STRUCT", "SUBSTR", "SUBSTRING", "SYNC", "SYSTEM_TIME", "SYSTEM_VERSION", 
			"TABLE", "TABLES", "TABLESAMPLE", "TBLPROPERTIES", "TEMPORARY", "TERMINATED", 
			"THEN", "TIME", "TIMESTAMP", "TIMESTAMPADD", "TIMESTAMPDIFF", "TO", "TOUCH", 
			"TRAILING", "TRANSACTION", "TRANSACTIONS", "TRANSFORM", "TRIM", "TRUE", 
			"TRUNCATE", "TRY_CAST", "TYPE", "UNARCHIVE", "UNBOUNDED", "UNCACHE", 
			"UNION", "UNIQUE", "UNKNOWN", "UNLOCK", "UNSET", "UPDATE", "USE", "USER", 
			"USING", "VALUES", "VERSION", "VIEW", "VIEWS", "WEEK", "WHEN", "WHERE", 
			"WINDOW", "WITH", "WITHIN", "YEAR", "ZONE", "KEY", "EQ", "NSEQ", "NEQ", 
			"NEQJ", "LT", "LTE", "GT", "GTE", "PLUS", "MINUS", "ASTERISK", "SLASH", 
			"PERCENT", "TILDE", "AMPERSAND", "PIPE", "CONCAT_PIPE", "HAT", "COLON", 
			"ARROW", "HENT_START", "HENT_END", "STRING", "BIGINT_LITERAL", "SMALLINT_LITERAL", 
			"TINYINT_LITERAL", "INTEGER_VALUE", "EXPONENT_VALUE", "DECIMAL_VALUE", 
			"FLOAT_LITERAL", "DOUBLE_LITERAL", "BIGDECIMAL_LITERAL", "IDENTIFIER", 
			"BACKQUOTED_IDENTIFIER", "SIMPLE_COMMENT", "BRACKETED_COMMENT", "WS", 
			"UNRECOGNIZED"
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
	public String getGrammarFileName() { return "ArcticSqlExtend.g4"; }

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

	public ArcticSqlExtendParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ExtendStatementContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticSqlExtendParser.EOF, 0); }
		public List<TerminalNode> SEMICOLON() { return getTokens(ArcticSqlExtendParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(ArcticSqlExtendParser.SEMICOLON, i);
		}
		public ExtendStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extendStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExtendStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExtendStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExtendStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtendStatementContext extendStatement() throws RecognitionException {
		ExtendStatementContext _localctx = new ExtendStatementContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_extendStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
			statement();
			setState(220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(217);
				match(SEMICOLON);
				}
				}
				setState(222);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(223);
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
		public TerminalNode EXPLAIN() { return getToken(ArcticSqlExtendParser.EXPLAIN, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode LOGICAL() { return getToken(ArcticSqlExtendParser.LOGICAL, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlExtendParser.FORMATTED, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlExtendParser.EXTENDED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSqlExtendParser.CODEGEN, 0); }
		public TerminalNode COST() { return getToken(ArcticSqlExtendParser.COST, 0); }
		public ExplainContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExplain(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExplain(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExplain(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateTableWithPkContext extends StatementContext {
		public CreateTableHeaderContext createTableHeader() {
			return getRuleContext(CreateTableHeaderContext.class,0);
		}
		public ColListAndPkContext colListAndPk() {
			return getRuleContext(ColListAndPkContext.class,0);
		}
		public CreateTableClausesContext createTableClauses() {
			return getRuleContext(CreateTableClausesContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public CreateTableWithPkContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCreateTableWithPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCreateTableWithPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCreateTableWithPk(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		int _la;
		try {
			setState(242);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CREATE:
				_localctx = new CreateTableWithPkContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(225);
				createTableHeader();
				setState(226);
				colListAndPk();
				setState(228);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==USING) {
					{
					setState(227);
					tableProvider();
					}
				}

				setState(230);
				createTableClauses();
				setState(235);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT_PAREN || _la==AS || _la==FROM || _la==MAP || ((((_la - 195)) & ~0x3f) == 0 && ((1L << (_la - 195)) & ((1L << (REDUCE - 195)) | (1L << (SELECT - 195)) | (1L << (TABLE - 195)))) != 0) || _la==VALUES || _la==WITH) {
					{
					setState(232);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(231);
						match(AS);
						}
					}

					setState(234);
					query();
					}
				}

				}
				break;
			case EXPLAIN:
				_localctx = new ExplainContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(237);
				match(EXPLAIN);
				setState(239);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CODEGEN || _la==COST || ((((_la - 88)) & ~0x3f) == 0 && ((1L << (_la - 88)) & ((1L << (EXTENDED - 88)) | (1L << (FORMATTED - 88)) | (1L << (LOGICAL - 88)))) != 0)) {
					{
					setState(238);
					_la = _input.LA(1);
					if ( !(_la==CODEGEN || _la==COST || ((((_la - 88)) & ~0x3f) == 0 && ((1L << (_la - 88)) & ((1L << (EXTENDED - 88)) | (1L << (FORMATTED - 88)) | (1L << (LOGICAL - 88)))) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(241);
				statement();
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

	public static class CreateTableHeaderContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(ArcticSqlExtendParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlExtendParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlExtendParser.TEMPORARY, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSqlExtendParser.EXTERNAL, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlExtendParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlExtendParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlExtendParser.EXISTS, 0); }
		public CreateTableHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCreateTableHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCreateTableHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCreateTableHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableHeaderContext createTableHeader() throws RecognitionException {
		CreateTableHeaderContext _localctx = new CreateTableHeaderContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_createTableHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(CREATE);
			setState(246);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TEMPORARY) {
				{
				setState(245);
				match(TEMPORARY);
				}
			}

			setState(249);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTERNAL) {
				{
				setState(248);
				match(EXTERNAL);
				}
			}

			setState(251);
			match(TABLE);
			setState(255);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(252);
				match(IF);
				setState(253);
				match(NOT);
				setState(254);
				match(EXISTS);
				}
				break;
			}
			setState(257);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterColListOnlyPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitColListOnlyPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitColListOnlyPk(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ColListWithPkContext extends ColListAndPkContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode COMMA() { return getToken(ArcticSqlExtendParser.COMMA, 0); }
		public PrimarySpecContext primarySpec() {
			return getRuleContext(PrimarySpecContext.class,0);
		}
		public ColListWithPkContext(ColListAndPkContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterColListWithPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitColListWithPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitColListWithPk(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColListAndPkContext colListAndPk() throws RecognitionException {
		ColListAndPkContext _localctx = new ColListAndPkContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_colListAndPk);
		int _la;
		try {
			setState(268);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LEFT_PAREN:
				_localctx = new ColListWithPkContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(259);
				match(LEFT_PAREN);
				setState(260);
				colTypeList();
				setState(263);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(261);
					match(COMMA);
					setState(262);
					primarySpec();
					}
				}

				setState(265);
				match(RIGHT_PAREN);
				}
				break;
			case PRIMARY:
				_localctx = new ColListOnlyPkContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(267);
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

	public static class PrimarySpecContext extends ParserRuleContext {
		public TerminalNode PRIMARY() { return getToken(ArcticSqlExtendParser.PRIMARY, 0); }
		public TerminalNode KEY() { return getToken(ArcticSqlExtendParser.KEY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public PrimarySpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primarySpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPrimarySpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPrimarySpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPrimarySpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimarySpecContext primarySpec() throws RecognitionException {
		PrimarySpecContext _localctx = new PrimarySpecContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_primarySpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(270);
			match(PRIMARY);
			setState(271);
			match(KEY);
			setState(272);
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

	public static class BucketSpecContext extends ParserRuleContext {
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlExtendParser.CLUSTERED, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSqlExtendParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlExtendParser.BY, i);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode INTO() { return getToken(ArcticSqlExtendParser.INTO, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSqlExtendParser.BUCKETS, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlExtendParser.SORTED, 0); }
		public OrderedIdentifierListContext orderedIdentifierList() {
			return getRuleContext(OrderedIdentifierListContext.class,0);
		}
		public BucketSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bucketSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterBucketSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitBucketSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitBucketSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BucketSpecContext bucketSpec() throws RecognitionException {
		BucketSpecContext _localctx = new BucketSpecContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_bucketSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(274);
			match(CLUSTERED);
			setState(275);
			match(BY);
			setState(276);
			identifierList();
			setState(280);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SORTED) {
				{
				setState(277);
				match(SORTED);
				setState(278);
				match(BY);
				setState(279);
				orderedIdentifierList();
				}
			}

			setState(282);
			match(INTO);
			setState(283);
			match(INTEGER_VALUE);
			setState(284);
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
		public TerminalNode SKEWED() { return getToken(ArcticSqlExtendParser.SKEWED, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlExtendParser.BY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode ON() { return getToken(ArcticSqlExtendParser.ON, 0); }
		public ConstantListContext constantList() {
			return getRuleContext(ConstantListContext.class,0);
		}
		public NestedConstantListContext nestedConstantList() {
			return getRuleContext(NestedConstantListContext.class,0);
		}
		public TerminalNode STORED() { return getToken(ArcticSqlExtendParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlExtendParser.DIRECTORIES, 0); }
		public SkewSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skewSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSkewSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSkewSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSkewSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SkewSpecContext skewSpec() throws RecognitionException {
		SkewSpecContext _localctx = new SkewSpecContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_skewSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			match(SKEWED);
			setState(287);
			match(BY);
			setState(288);
			identifierList();
			setState(289);
			match(ON);
			setState(292);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				{
				setState(290);
				constantList();
				}
				break;
			case 2:
				{
				setState(291);
				nestedConstantList();
				}
				break;
			}
			setState(297);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				{
				setState(294);
				match(STORED);
				setState(295);
				match(AS);
				setState(296);
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
		public TerminalNode LOCATION() { return getToken(ArcticSqlExtendParser.LOCATION, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public LocationSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_locationSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLocationSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLocationSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLocationSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocationSpecContext locationSpec() throws RecognitionException {
		LocationSpecContext _localctx = new LocationSpecContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_locationSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(299);
			match(LOCATION);
			setState(300);
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
		public TerminalNode COMMENT() { return getToken(ArcticSqlExtendParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public CommentSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commentSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCommentSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCommentSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCommentSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommentSpecContext commentSpec() throws RecognitionException {
		CommentSpecContext _localctx = new CommentSpecContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_commentSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(302);
			match(COMMENT);
			setState(303);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(306);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(305);
				ctes();
				}
			}

			setState(308);
			queryTerm(0);
			setState(309);
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

	public static class CtesContext extends ParserRuleContext {
		public TerminalNode WITH() { return getToken(ArcticSqlExtendParser.WITH, 0); }
		public List<NamedQueryContext> namedQuery() {
			return getRuleContexts(NamedQueryContext.class);
		}
		public NamedQueryContext namedQuery(int i) {
			return getRuleContext(NamedQueryContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public CtesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ctes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCtes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCtes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCtes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtesContext ctes() throws RecognitionException {
		CtesContext _localctx = new CtesContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_ctes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(311);
			match(WITH);
			setState(312);
			namedQuery();
			setState(317);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(313);
				match(COMMA);
				setState(314);
				namedQuery();
				}
				}
				setState(319);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public NamedQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNamedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNamedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNamedQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedQueryContext namedQuery() throws RecognitionException {
		NamedQueryContext _localctx = new NamedQueryContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_namedQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(320);
			((NamedQueryContext)_localctx).name = errorCapturingIdentifier();
			setState(322);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(321);
				((NamedQueryContext)_localctx).columnAliases = identifierList();
				}
				break;
			}
			setState(325);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(324);
				match(AS);
				}
			}

			setState(327);
			match(LEFT_PAREN);
			setState(328);
			query();
			setState(329);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode USING() { return getToken(ArcticSqlExtendParser.USING, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableProviderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProvider; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTableProvider(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTableProvider(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTableProvider(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableProviderContext tableProvider() throws RecognitionException {
		TableProviderContext _localctx = new TableProviderContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_tableProvider);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(331);
			match(USING);
			setState(332);
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
		public PropertyListContext options;
		public PartitionFieldListContext partitioning;
		public PropertyListContext tableProps;
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
		public List<TerminalNode> OPTIONS() { return getTokens(ArcticSqlExtendParser.OPTIONS); }
		public TerminalNode OPTIONS(int i) {
			return getToken(ArcticSqlExtendParser.OPTIONS, i);
		}
		public List<TerminalNode> PARTITIONED() { return getTokens(ArcticSqlExtendParser.PARTITIONED); }
		public TerminalNode PARTITIONED(int i) {
			return getToken(ArcticSqlExtendParser.PARTITIONED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticSqlExtendParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlExtendParser.BY, i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticSqlExtendParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticSqlExtendParser.TBLPROPERTIES, i);
		}
		public List<PropertyListContext> propertyList() {
			return getRuleContexts(PropertyListContext.class);
		}
		public PropertyListContext propertyList(int i) {
			return getRuleContext(PropertyListContext.class,i);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCreateTableClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCreateTableClauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCreateTableClauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableClausesContext createTableClauses() throws RecognitionException {
		CreateTableClausesContext _localctx = new CreateTableClausesContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_createTableClauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(349);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==CLUSTERED || _la==COMMENT || ((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (LOCATION - 141)) | (1L << (OPTIONS - 141)) | (1L << (PARTITIONED - 141)))) != 0) || ((((_la - 212)) & ~0x3f) == 0 && ((1L << (_la - 212)) & ((1L << (ROW - 212)) | (1L << (SKEWED - 212)) | (1L << (STORED - 212)) | (1L << (TBLPROPERTIES - 212)))) != 0)) {
				{
				setState(347);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTIONS:
					{
					{
					setState(334);
					match(OPTIONS);
					setState(335);
					((CreateTableClausesContext)_localctx).options = propertyList();
					}
					}
					break;
				case PARTITIONED:
					{
					{
					setState(336);
					match(PARTITIONED);
					setState(337);
					match(BY);
					setState(338);
					((CreateTableClausesContext)_localctx).partitioning = partitionFieldList();
					}
					}
					break;
				case SKEWED:
					{
					setState(339);
					skewSpec();
					}
					break;
				case CLUSTERED:
					{
					setState(340);
					bucketSpec();
					}
					break;
				case ROW:
					{
					setState(341);
					rowFormat();
					}
					break;
				case STORED:
					{
					setState(342);
					createFileFormat();
					}
					break;
				case LOCATION:
					{
					setState(343);
					locationSpec();
					}
					break;
				case COMMENT:
					{
					setState(344);
					commentSpec();
					}
					break;
				case TBLPROPERTIES:
					{
					{
					setState(345);
					match(TBLPROPERTIES);
					setState(346);
					((CreateTableClausesContext)_localctx).tableProps = propertyList();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(351);
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

	public static class PropertyListContext extends ParserRuleContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<PropertyContext> property() {
			return getRuleContexts(PropertyContext.class);
		}
		public PropertyContext property(int i) {
			return getRuleContext(PropertyContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public PropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListContext propertyList() throws RecognitionException {
		PropertyListContext _localctx = new PropertyListContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_propertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(352);
			match(LEFT_PAREN);
			setState(353);
			property();
			setState(358);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(354);
				match(COMMA);
				setState(355);
				property();
				}
				}
				setState(360);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(361);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PropertyContext extends ParserRuleContext {
		public PropertyKeyContext key;
		public PropertyValueContext value;
		public PropertyKeyContext propertyKey() {
			return getRuleContext(PropertyKeyContext.class,0);
		}
		public PropertyValueContext propertyValue() {
			return getRuleContext(PropertyValueContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ArcticSqlExtendParser.EQ, 0); }
		public PropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_property; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyContext property() throws RecognitionException {
		PropertyContext _localctx = new PropertyContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_property);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(363);
			((PropertyContext)_localctx).key = propertyKey();
			setState(368);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FALSE || ((((_la - 259)) & ~0x3f) == 0 && ((1L << (_la - 259)) & ((1L << (TRUE - 259)) | (1L << (EQ - 259)) | (1L << (STRING - 259)) | (1L << (INTEGER_VALUE - 259)) | (1L << (DECIMAL_VALUE - 259)))) != 0)) {
				{
				setState(365);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(364);
					match(EQ);
					}
				}

				setState(367);
				((PropertyContext)_localctx).value = propertyValue();
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

	public static class PropertyKeyContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(ArcticSqlExtendParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(ArcticSqlExtendParser.DOT, i);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public PropertyKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPropertyKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPropertyKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPropertyKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyKeyContext propertyKey() throws RecognitionException {
		PropertyKeyContext _localctx = new PropertyKeyContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_propertyKey);
		int _la;
		try {
			setState(379);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(370);
				identifier();
				setState(375);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(371);
					match(DOT);
					setState(372);
					identifier();
					}
					}
					setState(377);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(378);
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

	public static class PropertyValueContext extends ParserRuleContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlExtendParser.DECIMAL_VALUE, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public PropertyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPropertyValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPropertyValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPropertyValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyValueContext propertyValue() throws RecognitionException {
		PropertyValueContext _localctx = new PropertyValueContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_propertyValue);
		try {
			setState(385);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(381);
				match(INTEGER_VALUE);
				}
				break;
			case DECIMAL_VALUE:
				enterOuterAlt(_localctx, 2);
				{
				setState(382);
				match(DECIMAL_VALUE);
				}
				break;
			case FALSE:
			case TRUE:
				enterOuterAlt(_localctx, 3);
				{
				setState(383);
				booleanValue();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(384);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<ConstantContext> constant() {
			return getRuleContexts(ConstantContext.class);
		}
		public ConstantContext constant(int i) {
			return getRuleContext(ConstantContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public ConstantListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantListContext constantList() throws RecognitionException {
		ConstantListContext _localctx = new ConstantListContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_constantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			match(LEFT_PAREN);
			setState(388);
			constant();
			setState(393);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(389);
				match(COMMA);
				setState(390);
				constant();
				}
				}
				setState(395);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(396);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<ConstantListContext> constantList() {
			return getRuleContexts(ConstantListContext.class);
		}
		public ConstantListContext constantList(int i) {
			return getRuleContext(ConstantListContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public NestedConstantListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nestedConstantList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNestedConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNestedConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNestedConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NestedConstantListContext nestedConstantList() throws RecognitionException {
		NestedConstantListContext _localctx = new NestedConstantListContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_nestedConstantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(398);
			match(LEFT_PAREN);
			setState(399);
			constantList();
			setState(404);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(400);
				match(COMMA);
				setState(401);
				constantList();
				}
				}
				setState(406);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(407);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode STORED() { return getToken(ArcticSqlExtendParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public FileFormatContext fileFormat() {
			return getRuleContext(FileFormatContext.class,0);
		}
		public TerminalNode BY() { return getToken(ArcticSqlExtendParser.BY, 0); }
		public StorageHandlerContext storageHandler() {
			return getRuleContext(StorageHandlerContext.class,0);
		}
		public CreateFileFormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createFileFormat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCreateFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCreateFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCreateFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateFileFormatContext createFileFormat() throws RecognitionException {
		CreateFileFormatContext _localctx = new CreateFileFormatContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_createFileFormat);
		try {
			setState(415);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(409);
				match(STORED);
				setState(410);
				match(AS);
				setState(411);
				fileFormat();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(412);
				match(STORED);
				setState(413);
				match(BY);
				setState(414);
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
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSqlExtendParser.INPUTFORMAT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSqlExtendParser.OUTPUTFORMAT, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlExtendParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlExtendParser.STRING, i);
		}
		public TableFileFormatContext(FileFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTableFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTableFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTableFileFormat(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterGenericFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitGenericFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitGenericFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileFormatContext fileFormat() throws RecognitionException {
		FileFormatContext _localctx = new FileFormatContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_fileFormat);
		try {
			setState(422);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				_localctx = new TableFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(417);
				match(INPUTFORMAT);
				setState(418);
				((TableFileFormatContext)_localctx).inFmt = match(STRING);
				setState(419);
				match(OUTPUTFORMAT);
				setState(420);
				((TableFileFormatContext)_localctx).outFmt = match(STRING);
				}
				break;
			case 2:
				_localctx = new GenericFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(421);
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
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticSqlExtendParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlExtendParser.SERDEPROPERTIES, 0); }
		public PropertyListContext propertyList() {
			return getRuleContext(PropertyListContext.class,0);
		}
		public StorageHandlerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storageHandler; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterStorageHandler(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitStorageHandler(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitStorageHandler(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StorageHandlerContext storageHandler() throws RecognitionException {
		StorageHandlerContext _localctx = new StorageHandlerContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_storageHandler);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(424);
			match(STRING);
			setState(428);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				{
				setState(425);
				match(WITH);
				setState(426);
				match(SERDEPROPERTIES);
				setState(427);
				propertyList();
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

	public static class QueryOrganizationContext extends ParserRuleContext {
		public SortItemContext sortItem;
		public List<SortItemContext> order = new ArrayList<SortItemContext>();
		public ExpressionContext expression;
		public List<ExpressionContext> clusterBy = new ArrayList<ExpressionContext>();
		public List<ExpressionContext> distributeBy = new ArrayList<ExpressionContext>();
		public List<SortItemContext> sort = new ArrayList<SortItemContext>();
		public ExpressionContext limit;
		public TerminalNode ORDER() { return getToken(ArcticSqlExtendParser.ORDER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSqlExtendParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlExtendParser.BY, i);
		}
		public TerminalNode CLUSTER() { return getToken(ArcticSqlExtendParser.CLUSTER, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlExtendParser.DISTRIBUTE, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlExtendParser.SORT, 0); }
		public WindowClauseContext windowClause() {
			return getRuleContext(WindowClauseContext.class,0);
		}
		public TerminalNode LIMIT() { return getToken(ArcticSqlExtendParser.LIMIT, 0); }
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
		public TerminalNode ALL() { return getToken(ArcticSqlExtendParser.ALL, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public QueryOrganizationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryOrganization; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQueryOrganization(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQueryOrganization(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQueryOrganization(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryOrganizationContext queryOrganization() throws RecognitionException {
		QueryOrganizationContext _localctx = new QueryOrganizationContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_queryOrganization);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(440);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				{
				setState(430);
				match(ORDER);
				setState(431);
				match(BY);
				setState(432);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(437);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(433);
						match(COMMA);
						setState(434);
						((QueryOrganizationContext)_localctx).sortItem = sortItem();
						((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
						}
						} 
					}
					setState(439);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
				}
				}
				break;
			}
			setState(452);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				{
				setState(442);
				match(CLUSTER);
				setState(443);
				match(BY);
				setState(444);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(449);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(445);
						match(COMMA);
						setState(446);
						((QueryOrganizationContext)_localctx).expression = expression();
						((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
						}
						} 
					}
					setState(451);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
				}
				}
				break;
			}
			setState(464);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				{
				setState(454);
				match(DISTRIBUTE);
				setState(455);
				match(BY);
				setState(456);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(461);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(457);
						match(COMMA);
						setState(458);
						((QueryOrganizationContext)_localctx).expression = expression();
						((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
						}
						} 
					}
					setState(463);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
				}
				}
				break;
			}
			setState(476);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				{
				setState(466);
				match(SORT);
				setState(467);
				match(BY);
				setState(468);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(473);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(469);
						match(COMMA);
						setState(470);
						((QueryOrganizationContext)_localctx).sortItem = sortItem();
						((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
						}
						} 
					}
					setState(475);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
				}
				}
				break;
			}
			setState(479);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				{
				setState(478);
				windowClause();
				}
				break;
			}
			setState(486);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				{
				setState(481);
				match(LIMIT);
				setState(484);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
				case 1:
					{
					setState(482);
					match(ALL);
					}
					break;
				case 2:
					{
					setState(483);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQueryTermDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQueryTermDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQueryTermDefault(this);
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
		public TerminalNode INTERSECT() { return getToken(ArcticSqlExtendParser.INTERSECT, 0); }
		public TerminalNode UNION() { return getToken(ArcticSqlExtendParser.UNION, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticSqlExtendParser.EXCEPT, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSqlExtendParser.SETMINUS, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public SetOperationContext(QueryTermContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSetOperation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSetOperation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSetOperation(this);
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
		int _startState = 48;
		enterRecursionRule(_localctx, 48, RULE_queryTerm, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new QueryTermDefaultContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(489);
			queryPrimary();
			}
			_ctx.stop = _input.LT(-1);
			setState(514);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,46,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(512);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
					case 1:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(491);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(492);
						if (!(legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "legacy_setops_precedence_enabled");
						setState(493);
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
						setState(495);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(494);
							setQuantifier();
							}
						}

						setState(497);
						((SetOperationContext)_localctx).right = queryTerm(4);
						}
						break;
					case 2:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(498);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(499);
						if (!(!legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "!legacy_setops_precedence_enabled");
						setState(500);
						((SetOperationContext)_localctx).operator = match(INTERSECT);
						setState(502);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(501);
							setQuantifier();
							}
						}

						setState(504);
						((SetOperationContext)_localctx).right = queryTerm(3);
						}
						break;
					case 3:
						{
						_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
						((SetOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
						setState(505);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(506);
						if (!(!legacy_setops_precedence_enabled)) throw new FailedPredicateException(this, "!legacy_setops_precedence_enabled");
						setState(507);
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
						setState(509);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ALL || _la==DISTINCT) {
							{
							setState(508);
							setQuantifier();
							}
						}

						setState(511);
						((SetOperationContext)_localctx).right = queryTerm(2);
						}
						break;
					}
					} 
				}
				setState(516);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,46,_ctx);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public SubqueryContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSubquery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSubquery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSubquery(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQueryPrimaryDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQueryPrimaryDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQueryPrimaryDefault(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterInlineTableDefault1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitInlineTableDefault1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitInlineTableDefault1(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFromStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFromStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFromStmt(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TableContext extends QueryPrimaryContext {
		public TerminalNode TABLE() { return getToken(ArcticSqlExtendParser.TABLE, 0); }
		public MultipartIdentifierContext multipartIdentifier() {
			return getRuleContext(MultipartIdentifierContext.class,0);
		}
		public TableContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryPrimaryContext queryPrimary() throws RecognitionException {
		QueryPrimaryContext _localctx = new QueryPrimaryContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_queryPrimary);
		try {
			setState(526);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MAP:
			case REDUCE:
			case SELECT:
				_localctx = new QueryPrimaryDefaultContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(517);
				querySpecification();
				}
				break;
			case FROM:
				_localctx = new FromStmtContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(518);
				fromStatement();
				}
				break;
			case TABLE:
				_localctx = new TableContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(519);
				match(TABLE);
				setState(520);
				multipartIdentifier();
				}
				break;
			case VALUES:
				_localctx = new InlineTableDefault1Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(521);
				inlineTable();
				}
				break;
			case LEFT_PAREN:
				_localctx = new SubqueryContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(522);
				match(LEFT_PAREN);
				setState(523);
				query();
				setState(524);
				match(RIGHT_PAREN);
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
		public TerminalNode NULLS() { return getToken(ArcticSqlExtendParser.NULLS, 0); }
		public TerminalNode ASC() { return getToken(ArcticSqlExtendParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlExtendParser.DESC, 0); }
		public TerminalNode LAST() { return getToken(ArcticSqlExtendParser.LAST, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSqlExtendParser.FIRST, 0); }
		public SortItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sortItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSortItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSortItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSortItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SortItemContext sortItem() throws RecognitionException {
		SortItemContext _localctx = new SortItemContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_sortItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(528);
			expression();
			setState(530);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
			case 1:
				{
				setState(529);
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
			setState(534);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,49,_ctx) ) {
			case 1:
				{
				setState(532);
				match(NULLS);
				setState(533);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFromStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFromStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFromStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromStatementContext fromStatement() throws RecognitionException {
		FromStatementContext _localctx = new FromStatementContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_fromStatement);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(536);
			fromClause();
			setState(538); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(537);
					fromStatementBody();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(540); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFromStatementBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFromStatementBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFromStatementBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromStatementBodyContext fromStatementBody() throws RecognitionException {
		FromStatementBodyContext _localctx = new FromStatementBodyContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_fromStatementBody);
		try {
			int _alt;
			setState(569);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(542);
				transformClause();
				setState(544);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
				case 1:
					{
					setState(543);
					whereClause();
					}
					break;
				}
				setState(546);
				queryOrganization();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(548);
				selectClause();
				setState(552);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(549);
						lateralView();
						}
						} 
					}
					setState(554);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
				}
				setState(556);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,53,_ctx) ) {
				case 1:
					{
					setState(555);
					whereClause();
					}
					break;
				}
				setState(559);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
				case 1:
					{
					setState(558);
					aggregationClause();
					}
					break;
				}
				setState(562);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
				case 1:
					{
					setState(561);
					havingClause();
					}
					break;
				}
				setState(565);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
				case 1:
					{
					setState(564);
					windowClause();
					}
					break;
				}
				setState(567);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterRegularQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitRegularQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitRegularQuerySpecification(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTransformQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTransformQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTransformQuerySpecification(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuerySpecificationContext querySpecification() throws RecognitionException {
		QuerySpecificationContext _localctx = new QuerySpecificationContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_querySpecification);
		try {
			int _alt;
			setState(615);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,70,_ctx) ) {
			case 1:
				_localctx = new TransformQuerySpecificationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(571);
				transformClause();
				setState(573);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,58,_ctx) ) {
				case 1:
					{
					setState(572);
					fromClause();
					}
					break;
				}
				setState(578);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,59,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(575);
						lateralView();
						}
						} 
					}
					setState(580);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,59,_ctx);
				}
				setState(582);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
				case 1:
					{
					setState(581);
					whereClause();
					}
					break;
				}
				setState(585);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
				case 1:
					{
					setState(584);
					aggregationClause();
					}
					break;
				}
				setState(588);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
				case 1:
					{
					setState(587);
					havingClause();
					}
					break;
				}
				setState(591);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
				case 1:
					{
					setState(590);
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
				setState(593);
				selectClause();
				setState(595);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
				case 1:
					{
					setState(594);
					fromClause();
					}
					break;
				}
				setState(600);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,65,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(597);
						lateralView();
						}
						} 
					}
					setState(602);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,65,_ctx);
				}
				setState(604);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
				case 1:
					{
					setState(603);
					whereClause();
					}
					break;
				}
				setState(607);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
				case 1:
					{
					setState(606);
					aggregationClause();
					}
					break;
				}
				setState(610);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
				case 1:
					{
					setState(609);
					havingClause();
					}
					break;
				}
				setState(613);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,69,_ctx) ) {
				case 1:
					{
					setState(612);
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
		public TerminalNode USING() { return getToken(ArcticSqlExtendParser.USING, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlExtendParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlExtendParser.STRING, i);
		}
		public TerminalNode SELECT() { return getToken(ArcticSqlExtendParser.SELECT, 0); }
		public List<TerminalNode> LEFT_PAREN() { return getTokens(ArcticSqlExtendParser.LEFT_PAREN); }
		public TerminalNode LEFT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.LEFT_PAREN, i);
		}
		public ExpressionSeqContext expressionSeq() {
			return getRuleContext(ExpressionSeqContext.class,0);
		}
		public List<TerminalNode> RIGHT_PAREN() { return getTokens(ArcticSqlExtendParser.RIGHT_PAREN); }
		public TerminalNode RIGHT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.RIGHT_PAREN, i);
		}
		public TerminalNode TRANSFORM() { return getToken(ArcticSqlExtendParser.TRANSFORM, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlExtendParser.MAP, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSqlExtendParser.REDUCE, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSqlExtendParser.RECORDWRITER, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSqlExtendParser.RECORDREADER, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTransformClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTransformClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTransformClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformClauseContext transformClause() throws RecognitionException {
		TransformClauseContext _localctx = new TransformClauseContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_transformClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(636);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
				{
				setState(617);
				match(SELECT);
				setState(618);
				((TransformClauseContext)_localctx).kind = match(TRANSFORM);
				setState(619);
				match(LEFT_PAREN);
				setState(621);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
				case 1:
					{
					setState(620);
					setQuantifier();
					}
					break;
				}
				setState(623);
				expressionSeq();
				setState(624);
				match(RIGHT_PAREN);
				}
				break;
			case MAP:
				{
				setState(626);
				((TransformClauseContext)_localctx).kind = match(MAP);
				setState(628);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
				case 1:
					{
					setState(627);
					setQuantifier();
					}
					break;
				}
				setState(630);
				expressionSeq();
				}
				break;
			case REDUCE:
				{
				setState(631);
				((TransformClauseContext)_localctx).kind = match(REDUCE);
				setState(633);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,73,_ctx) ) {
				case 1:
					{
					setState(632);
					setQuantifier();
					}
					break;
				}
				setState(635);
				expressionSeq();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(639);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ROW) {
				{
				setState(638);
				((TransformClauseContext)_localctx).inRowFormat = rowFormat();
				}
			}

			setState(643);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RECORDWRITER) {
				{
				setState(641);
				match(RECORDWRITER);
				setState(642);
				((TransformClauseContext)_localctx).recordWriter = match(STRING);
				}
			}

			setState(645);
			match(USING);
			setState(646);
			((TransformClauseContext)_localctx).script = match(STRING);
			setState(659);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
			case 1:
				{
				setState(647);
				match(AS);
				setState(657);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
				case 1:
					{
					setState(648);
					identifierSeq();
					}
					break;
				case 2:
					{
					setState(649);
					colTypeList();
					}
					break;
				case 3:
					{
					{
					setState(650);
					match(LEFT_PAREN);
					setState(653);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,77,_ctx) ) {
					case 1:
						{
						setState(651);
						identifierSeq();
						}
						break;
					case 2:
						{
						setState(652);
						colTypeList();
						}
						break;
					}
					setState(655);
					match(RIGHT_PAREN);
					}
					}
					break;
				}
				}
				break;
			}
			setState(662);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				{
				setState(661);
				((TransformClauseContext)_localctx).outRowFormat = rowFormat();
				}
				break;
			}
			setState(666);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
			case 1:
				{
				setState(664);
				match(RECORDREADER);
				setState(665);
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
		public TerminalNode SELECT() { return getToken(ArcticSqlExtendParser.SELECT, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSelectClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSelectClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSelectClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectClauseContext selectClause() throws RecognitionException {
		SelectClauseContext _localctx = new SelectClauseContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_selectClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(668);
			match(SELECT);
			setState(672);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(669);
					((SelectClauseContext)_localctx).hint = hint();
					((SelectClauseContext)_localctx).hints.add(((SelectClauseContext)_localctx).hint);
					}
					} 
				}
				setState(674);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			}
			setState(676);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,83,_ctx) ) {
			case 1:
				{
				setState(675);
				setQuantifier();
				}
				break;
			}
			setState(678);
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

	public static class WhereClauseContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(ArcticSqlExtendParser.WHERE, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public WhereClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterWhereClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitWhereClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitWhereClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_whereClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(680);
			match(WHERE);
			setState(681);
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
		public TerminalNode HAVING() { return getToken(ArcticSqlExtendParser.HAVING, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public HavingClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_havingClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterHavingClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitHavingClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitHavingClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HavingClauseContext havingClause() throws RecognitionException {
		HavingClauseContext _localctx = new HavingClauseContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_havingClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(683);
			match(HAVING);
			setState(684);
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
		public TerminalNode HENT_START() { return getToken(ArcticSqlExtendParser.HENT_START, 0); }
		public TerminalNode HENT_END() { return getToken(ArcticSqlExtendParser.HENT_END, 0); }
		public List<HintStatementContext> hintStatement() {
			return getRuleContexts(HintStatementContext.class);
		}
		public HintStatementContext hintStatement(int i) {
			return getRuleContext(HintStatementContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public HintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterHint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitHint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitHint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintContext hint() throws RecognitionException {
		HintContext _localctx = new HintContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_hint);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(686);
			match(HENT_START);
			setState(687);
			((HintContext)_localctx).hintStatement = hintStatement();
			((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
			setState(694);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(689);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
					case 1:
						{
						setState(688);
						match(COMMA);
						}
						break;
					}
					setState(691);
					((HintContext)_localctx).hintStatement = hintStatement();
					((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
					}
					} 
				}
				setState(696);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			}
			setState(697);
			match(HENT_END);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<PrimaryExpressionContext> primaryExpression() {
			return getRuleContexts(PrimaryExpressionContext.class);
		}
		public PrimaryExpressionContext primaryExpression(int i) {
			return getRuleContext(PrimaryExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public HintStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hintStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterHintStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitHintStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitHintStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintStatementContext hintStatement() throws RecognitionException {
		HintStatementContext _localctx = new HintStatementContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_hintStatement);
		int _la;
		try {
			setState(712);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(699);
				((HintStatementContext)_localctx).hintName = identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(700);
				((HintStatementContext)_localctx).hintName = identifier();
				setState(701);
				match(LEFT_PAREN);
				setState(702);
				((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
				((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
				setState(707);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(703);
					match(COMMA);
					setState(704);
					((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
					((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
					}
					}
					setState(709);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(710);
				match(RIGHT_PAREN);
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
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public List<RelationContext> relation() {
			return getRuleContexts(RelationContext.class);
		}
		public RelationContext relation(int i) {
			return getRuleContext(RelationContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFromClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFromClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFromClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromClauseContext fromClause() throws RecognitionException {
		FromClauseContext _localctx = new FromClauseContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_fromClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(714);
			match(FROM);
			setState(715);
			relation();
			setState(720);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(716);
					match(COMMA);
					setState(717);
					relation();
					}
					} 
				}
				setState(722);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
			}
			setState(726);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(723);
					lateralView();
					}
					} 
				}
				setState(728);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
			}
			setState(730);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
			case 1:
				{
				setState(729);
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

	public static class TemporalClauseContext extends ParserRuleContext {
		public Token version;
		public ValueExpressionContext timestamp;
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlExtendParser.OF, 0); }
		public TerminalNode SYSTEM_VERSION() { return getToken(ArcticSqlExtendParser.SYSTEM_VERSION, 0); }
		public TerminalNode VERSION() { return getToken(ArcticSqlExtendParser.VERSION, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public TerminalNode FOR() { return getToken(ArcticSqlExtendParser.FOR, 0); }
		public TerminalNode SYSTEM_TIME() { return getToken(ArcticSqlExtendParser.SYSTEM_TIME, 0); }
		public TerminalNode TIMESTAMP() { return getToken(ArcticSqlExtendParser.TIMESTAMP, 0); }
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TemporalClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_temporalClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTemporalClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTemporalClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTemporalClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TemporalClauseContext temporalClause() throws RecognitionException {
		TemporalClauseContext _localctx = new TemporalClauseContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_temporalClause);
		int _la;
		try {
			setState(746);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,93,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(733);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FOR) {
					{
					setState(732);
					match(FOR);
					}
				}

				setState(735);
				_la = _input.LA(1);
				if ( !(_la==SYSTEM_VERSION || _la==VERSION) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(736);
				match(AS);
				setState(737);
				match(OF);
				setState(738);
				((TemporalClauseContext)_localctx).version = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==STRING || _la==INTEGER_VALUE) ) {
					((TemporalClauseContext)_localctx).version = (Token)_errHandler.recoverInline(this);
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
				setState(740);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FOR) {
					{
					setState(739);
					match(FOR);
					}
				}

				setState(742);
				_la = _input.LA(1);
				if ( !(_la==SYSTEM_TIME || _la==TIMESTAMP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(743);
				match(AS);
				setState(744);
				match(OF);
				setState(745);
				((TemporalClauseContext)_localctx).timestamp = valueExpression(0);
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

	public static class AggregationClauseContext extends ParserRuleContext {
		public GroupByClauseContext groupByClause;
		public List<GroupByClauseContext> groupingExpressionsWithGroupingAnalytics = new ArrayList<GroupByClauseContext>();
		public ExpressionContext expression;
		public List<ExpressionContext> groupingExpressions = new ArrayList<ExpressionContext>();
		public Token kind;
		public TerminalNode GROUP() { return getToken(ArcticSqlExtendParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlExtendParser.BY, 0); }
		public List<GroupByClauseContext> groupByClause() {
			return getRuleContexts(GroupByClauseContext.class);
		}
		public GroupByClauseContext groupByClause(int i) {
			return getRuleContext(GroupByClauseContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode WITH() { return getToken(ArcticSqlExtendParser.WITH, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlExtendParser.SETS, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<GroupingSetContext> groupingSet() {
			return getRuleContexts(GroupingSetContext.class);
		}
		public GroupingSetContext groupingSet(int i) {
			return getRuleContext(GroupingSetContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSqlExtendParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlExtendParser.CUBE, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlExtendParser.GROUPING, 0); }
		public AggregationClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregationClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterAggregationClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitAggregationClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitAggregationClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregationClauseContext aggregationClause() throws RecognitionException {
		AggregationClauseContext _localctx = new AggregationClauseContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_aggregationClause);
		int _la;
		try {
			int _alt;
			setState(787);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(748);
				match(GROUP);
				setState(749);
				match(BY);
				setState(750);
				((AggregationClauseContext)_localctx).groupByClause = groupByClause();
				((AggregationClauseContext)_localctx).groupingExpressionsWithGroupingAnalytics.add(((AggregationClauseContext)_localctx).groupByClause);
				setState(755);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(751);
						match(COMMA);
						setState(752);
						((AggregationClauseContext)_localctx).groupByClause = groupByClause();
						((AggregationClauseContext)_localctx).groupingExpressionsWithGroupingAnalytics.add(((AggregationClauseContext)_localctx).groupByClause);
						}
						} 
					}
					setState(757);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(758);
				match(GROUP);
				setState(759);
				match(BY);
				setState(760);
				((AggregationClauseContext)_localctx).expression = expression();
				((AggregationClauseContext)_localctx).groupingExpressions.add(((AggregationClauseContext)_localctx).expression);
				setState(765);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,95,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(761);
						match(COMMA);
						setState(762);
						((AggregationClauseContext)_localctx).expression = expression();
						((AggregationClauseContext)_localctx).groupingExpressions.add(((AggregationClauseContext)_localctx).expression);
						}
						} 
					}
					setState(767);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,95,_ctx);
				}
				setState(785);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,97,_ctx) ) {
				case 1:
					{
					setState(768);
					match(WITH);
					setState(769);
					((AggregationClauseContext)_localctx).kind = match(ROLLUP);
					}
					break;
				case 2:
					{
					setState(770);
					match(WITH);
					setState(771);
					((AggregationClauseContext)_localctx).kind = match(CUBE);
					}
					break;
				case 3:
					{
					setState(772);
					((AggregationClauseContext)_localctx).kind = match(GROUPING);
					setState(773);
					match(SETS);
					setState(774);
					match(LEFT_PAREN);
					setState(775);
					groupingSet();
					setState(780);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(776);
						match(COMMA);
						setState(777);
						groupingSet();
						}
						}
						setState(782);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(783);
					match(RIGHT_PAREN);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterGroupByClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitGroupByClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitGroupByClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupByClauseContext groupByClause() throws RecognitionException {
		GroupByClauseContext _localctx = new GroupByClauseContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_groupByClause);
		try {
			setState(791);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,99,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(789);
				groupingAnalytics();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(790);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<GroupingSetContext> groupingSet() {
			return getRuleContexts(GroupingSetContext.class);
		}
		public GroupingSetContext groupingSet(int i) {
			return getRuleContext(GroupingSetContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSqlExtendParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlExtendParser.CUBE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public TerminalNode GROUPING() { return getToken(ArcticSqlExtendParser.GROUPING, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlExtendParser.SETS, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterGroupingAnalytics(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitGroupingAnalytics(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitGroupingAnalytics(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingAnalyticsContext groupingAnalytics() throws RecognitionException {
		GroupingAnalyticsContext _localctx = new GroupingAnalyticsContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_groupingAnalytics);
		int _la;
		try {
			setState(818);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CUBE:
			case ROLLUP:
				enterOuterAlt(_localctx, 1);
				{
				setState(793);
				_la = _input.LA(1);
				if ( !(_la==CUBE || _la==ROLLUP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(794);
				match(LEFT_PAREN);
				setState(795);
				groupingSet();
				setState(800);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(796);
					match(COMMA);
					setState(797);
					groupingSet();
					}
					}
					setState(802);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(803);
				match(RIGHT_PAREN);
				}
				break;
			case GROUPING:
				enterOuterAlt(_localctx, 2);
				{
				setState(805);
				match(GROUPING);
				setState(806);
				match(SETS);
				setState(807);
				match(LEFT_PAREN);
				setState(808);
				groupingElement();
				setState(813);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(809);
					match(COMMA);
					setState(810);
					groupingElement();
					}
					}
					setState(815);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(816);
				match(RIGHT_PAREN);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterGroupingElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitGroupingElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitGroupingElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingElementContext groupingElement() throws RecognitionException {
		GroupingElementContext _localctx = new GroupingElementContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_groupingElement);
		try {
			setState(822);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,103,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(820);
				groupingAnalytics();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(821);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public GroupingSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupingSet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterGroupingSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitGroupingSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitGroupingSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingSetContext groupingSet() throws RecognitionException {
		GroupingSetContext _localctx = new GroupingSetContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_groupingSet);
		int _la;
		try {
			setState(837);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,106,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(824);
				match(LEFT_PAREN);
				setState(833);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
				case 1:
					{
					setState(825);
					expression();
					setState(830);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(826);
						match(COMMA);
						setState(827);
						expression();
						}
						}
						setState(832);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(835);
				match(RIGHT_PAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(836);
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
		public TerminalNode PIVOT() { return getToken(ArcticSqlExtendParser.PIVOT, 0); }
		public List<TerminalNode> LEFT_PAREN() { return getTokens(ArcticSqlExtendParser.LEFT_PAREN); }
		public TerminalNode LEFT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.LEFT_PAREN, i);
		}
		public TerminalNode FOR() { return getToken(ArcticSqlExtendParser.FOR, 0); }
		public PivotColumnContext pivotColumn() {
			return getRuleContext(PivotColumnContext.class,0);
		}
		public TerminalNode IN() { return getToken(ArcticSqlExtendParser.IN, 0); }
		public List<TerminalNode> RIGHT_PAREN() { return getTokens(ArcticSqlExtendParser.RIGHT_PAREN); }
		public TerminalNode RIGHT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.RIGHT_PAREN, i);
		}
		public NamedExpressionSeqContext namedExpressionSeq() {
			return getRuleContext(NamedExpressionSeqContext.class,0);
		}
		public List<PivotValueContext> pivotValue() {
			return getRuleContexts(PivotValueContext.class);
		}
		public PivotValueContext pivotValue(int i) {
			return getRuleContext(PivotValueContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public PivotClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPivotClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPivotClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPivotClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotClauseContext pivotClause() throws RecognitionException {
		PivotClauseContext _localctx = new PivotClauseContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_pivotClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(839);
			match(PIVOT);
			setState(840);
			match(LEFT_PAREN);
			setState(841);
			((PivotClauseContext)_localctx).aggregates = namedExpressionSeq();
			setState(842);
			match(FOR);
			setState(843);
			pivotColumn();
			setState(844);
			match(IN);
			setState(845);
			match(LEFT_PAREN);
			setState(846);
			((PivotClauseContext)_localctx).pivotValue = pivotValue();
			((PivotClauseContext)_localctx).pivotValues.add(((PivotClauseContext)_localctx).pivotValue);
			setState(851);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(847);
				match(COMMA);
				setState(848);
				((PivotClauseContext)_localctx).pivotValue = pivotValue();
				((PivotClauseContext)_localctx).pivotValues.add(((PivotClauseContext)_localctx).pivotValue);
				}
				}
				setState(853);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(854);
			match(RIGHT_PAREN);
			setState(855);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public PivotColumnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotColumn; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPivotColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPivotColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPivotColumn(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotColumnContext pivotColumn() throws RecognitionException {
		PivotColumnContext _localctx = new PivotColumnContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_pivotColumn);
		int _la;
		try {
			setState(869);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(857);
				((PivotColumnContext)_localctx).identifier = identifier();
				((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(858);
				match(LEFT_PAREN);
				setState(859);
				((PivotColumnContext)_localctx).identifier = identifier();
				((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
				setState(864);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(860);
					match(COMMA);
					setState(861);
					((PivotColumnContext)_localctx).identifier = identifier();
					((PivotColumnContext)_localctx).identifiers.add(((PivotColumnContext)_localctx).identifier);
					}
					}
					setState(866);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(867);
				match(RIGHT_PAREN);
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
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public PivotValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pivotValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPivotValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPivotValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPivotValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PivotValueContext pivotValue() throws RecognitionException {
		PivotValueContext _localctx = new PivotValueContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_pivotValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
			expression();
			setState(876);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,111,_ctx) ) {
			case 1:
				{
				setState(873);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,110,_ctx) ) {
				case 1:
					{
					setState(872);
					match(AS);
					}
					break;
				}
				setState(875);
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
		public TerminalNode LATERAL() { return getToken(ArcticSqlExtendParser.LATERAL, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlExtendParser.VIEW, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode OUTER() { return getToken(ArcticSqlExtendParser.OUTER, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public LateralViewContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lateralView; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLateralView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLateralView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLateralView(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LateralViewContext lateralView() throws RecognitionException {
		LateralViewContext _localctx = new LateralViewContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_lateralView);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(878);
			match(LATERAL);
			setState(879);
			match(VIEW);
			setState(881);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
			case 1:
				{
				setState(880);
				match(OUTER);
				}
				break;
			}
			setState(883);
			qualifiedName();
			setState(884);
			match(LEFT_PAREN);
			setState(893);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,114,_ctx) ) {
			case 1:
				{
				setState(885);
				expression();
				setState(890);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(886);
					match(COMMA);
					setState(887);
					expression();
					}
					}
					setState(892);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
			setState(895);
			match(RIGHT_PAREN);
			setState(896);
			((LateralViewContext)_localctx).tblName = identifier();
			setState(908);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,117,_ctx) ) {
			case 1:
				{
				setState(898);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
				case 1:
					{
					setState(897);
					match(AS);
					}
					break;
				}
				setState(900);
				((LateralViewContext)_localctx).identifier = identifier();
				((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
				setState(905);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,116,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(901);
						match(COMMA);
						setState(902);
						((LateralViewContext)_localctx).identifier = identifier();
						((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
						}
						} 
					}
					setState(907);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,116,_ctx);
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
		public TerminalNode DISTINCT() { return getToken(ArcticSqlExtendParser.DISTINCT, 0); }
		public TerminalNode ALL() { return getToken(ArcticSqlExtendParser.ALL, 0); }
		public SetQuantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setQuantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSetQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSetQuantifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSetQuantifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetQuantifierContext setQuantifier() throws RecognitionException {
		SetQuantifierContext _localctx = new SetQuantifierContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_setQuantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(910);
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
		public TerminalNode LATERAL() { return getToken(ArcticSqlExtendParser.LATERAL, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationContext relation() throws RecognitionException {
		RelationContext _localctx = new RelationContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_relation);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(913);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,118,_ctx) ) {
			case 1:
				{
				setState(912);
				match(LATERAL);
				}
				break;
			}
			setState(915);
			relationPrimary();
			setState(919);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,119,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(916);
					joinRelation();
					}
					} 
				}
				setState(921);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,119,_ctx);
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
		public TerminalNode JOIN() { return getToken(ArcticSqlExtendParser.JOIN, 0); }
		public RelationPrimaryContext relationPrimary() {
			return getRuleContext(RelationPrimaryContext.class,0);
		}
		public JoinTypeContext joinType() {
			return getRuleContext(JoinTypeContext.class,0);
		}
		public TerminalNode LATERAL() { return getToken(ArcticSqlExtendParser.LATERAL, 0); }
		public JoinCriteriaContext joinCriteria() {
			return getRuleContext(JoinCriteriaContext.class,0);
		}
		public TerminalNode NATURAL() { return getToken(ArcticSqlExtendParser.NATURAL, 0); }
		public JoinRelationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinRelation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterJoinRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitJoinRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitJoinRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinRelationContext joinRelation() throws RecognitionException {
		JoinRelationContext _localctx = new JoinRelationContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_joinRelation);
		try {
			setState(939);
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
				setState(922);
				joinType();
				}
				setState(923);
				match(JOIN);
				setState(925);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,120,_ctx) ) {
				case 1:
					{
					setState(924);
					match(LATERAL);
					}
					break;
				}
				setState(927);
				((JoinRelationContext)_localctx).right = relationPrimary();
				setState(929);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,121,_ctx) ) {
				case 1:
					{
					setState(928);
					joinCriteria();
					}
					break;
				}
				}
				break;
			case NATURAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(931);
				match(NATURAL);
				setState(932);
				joinType();
				setState(933);
				match(JOIN);
				setState(935);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,122,_ctx) ) {
				case 1:
					{
					setState(934);
					match(LATERAL);
					}
					break;
				}
				setState(937);
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
		public TerminalNode INNER() { return getToken(ArcticSqlExtendParser.INNER, 0); }
		public TerminalNode CROSS() { return getToken(ArcticSqlExtendParser.CROSS, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSqlExtendParser.LEFT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticSqlExtendParser.OUTER, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSqlExtendParser.SEMI, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSqlExtendParser.RIGHT, 0); }
		public TerminalNode FULL() { return getToken(ArcticSqlExtendParser.FULL, 0); }
		public TerminalNode ANTI() { return getToken(ArcticSqlExtendParser.ANTI, 0); }
		public JoinTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterJoinType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitJoinType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitJoinType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinTypeContext joinType() throws RecognitionException {
		JoinTypeContext _localctx = new JoinTypeContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_joinType);
		int _la;
		try {
			setState(965);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,130,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(942);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INNER) {
					{
					setState(941);
					match(INNER);
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(944);
				match(CROSS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(945);
				match(LEFT);
				setState(947);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(946);
					match(OUTER);
					}
				}

				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(950);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(949);
					match(LEFT);
					}
				}

				setState(952);
				match(SEMI);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(953);
				match(RIGHT);
				setState(955);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(954);
					match(OUTER);
					}
				}

				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(957);
				match(FULL);
				setState(959);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(958);
					match(OUTER);
					}
				}

				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(962);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(961);
					match(LEFT);
					}
				}

				setState(964);
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
		public TerminalNode ON() { return getToken(ArcticSqlExtendParser.ON, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public TerminalNode USING() { return getToken(ArcticSqlExtendParser.USING, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public JoinCriteriaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinCriteria; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterJoinCriteria(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitJoinCriteria(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitJoinCriteria(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinCriteriaContext joinCriteria() throws RecognitionException {
		JoinCriteriaContext _localctx = new JoinCriteriaContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_joinCriteria);
		try {
			setState(971);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ON:
				enterOuterAlt(_localctx, 1);
				{
				setState(967);
				match(ON);
				setState(968);
				booleanExpression(0);
				}
				break;
			case USING:
				enterOuterAlt(_localctx, 2);
				{
				setState(969);
				match(USING);
				setState(970);
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
		public Token seed;
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSqlExtendParser.TABLESAMPLE, 0); }
		public List<TerminalNode> LEFT_PAREN() { return getTokens(ArcticSqlExtendParser.LEFT_PAREN); }
		public TerminalNode LEFT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.LEFT_PAREN, i);
		}
		public List<TerminalNode> RIGHT_PAREN() { return getTokens(ArcticSqlExtendParser.RIGHT_PAREN); }
		public TerminalNode RIGHT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.RIGHT_PAREN, i);
		}
		public SampleMethodContext sampleMethod() {
			return getRuleContext(SampleMethodContext.class,0);
		}
		public TerminalNode REPEATABLE() { return getToken(ArcticSqlExtendParser.REPEATABLE, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public SampleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sample; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSample(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSample(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSample(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleContext sample() throws RecognitionException {
		SampleContext _localctx = new SampleContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_sample);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(973);
			match(TABLESAMPLE);
			setState(974);
			match(LEFT_PAREN);
			setState(976);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,132,_ctx) ) {
			case 1:
				{
				setState(975);
				sampleMethod();
				}
				break;
			}
			setState(978);
			match(RIGHT_PAREN);
			setState(983);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,133,_ctx) ) {
			case 1:
				{
				setState(979);
				match(REPEATABLE);
				setState(980);
				match(LEFT_PAREN);
				setState(981);
				((SampleContext)_localctx).seed = match(INTEGER_VALUE);
				setState(982);
				match(RIGHT_PAREN);
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
		public TerminalNode ROWS() { return getToken(ArcticSqlExtendParser.ROWS, 0); }
		public SampleByRowsContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSampleByRows(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSampleByRows(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSampleByRows(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByPercentileContext extends SampleMethodContext {
		public Token negativeSign;
		public Token percentage;
		public TerminalNode PERCENTLIT() { return getToken(ArcticSqlExtendParser.PERCENTLIT, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlExtendParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public SampleByPercentileContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSampleByPercentile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSampleByPercentile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSampleByPercentile(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByBucketContext extends SampleMethodContext {
		public Token sampleType;
		public Token numerator;
		public Token denominator;
		public TerminalNode OUT() { return getToken(ArcticSqlExtendParser.OUT, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlExtendParser.OF, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSqlExtendParser.BUCKET, 0); }
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticSqlExtendParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticSqlExtendParser.INTEGER_VALUE, i);
		}
		public TerminalNode ON() { return getToken(ArcticSqlExtendParser.ON, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public SampleByBucketContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSampleByBucket(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSampleByBucket(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSampleByBucket(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSampleByBytes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSampleByBytes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSampleByBytes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleMethodContext sampleMethod() throws RecognitionException {
		SampleMethodContext _localctx = new SampleMethodContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_sampleMethod);
		int _la;
		try {
			setState(1009);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,137,_ctx) ) {
			case 1:
				_localctx = new SampleByPercentileContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(986);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(985);
					((SampleByPercentileContext)_localctx).negativeSign = match(MINUS);
					}
				}

				setState(988);
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
				setState(989);
				match(PERCENTLIT);
				}
				break;
			case 2:
				_localctx = new SampleByRowsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(990);
				expression();
				setState(991);
				match(ROWS);
				}
				break;
			case 3:
				_localctx = new SampleByBucketContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(993);
				((SampleByBucketContext)_localctx).sampleType = match(BUCKET);
				setState(994);
				((SampleByBucketContext)_localctx).numerator = match(INTEGER_VALUE);
				setState(995);
				match(OUT);
				setState(996);
				match(OF);
				setState(997);
				((SampleByBucketContext)_localctx).denominator = match(INTEGER_VALUE);
				setState(1006);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ON) {
					{
					setState(998);
					match(ON);
					setState(1004);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,135,_ctx) ) {
					case 1:
						{
						setState(999);
						identifier();
						}
						break;
					case 2:
						{
						setState(1000);
						qualifiedName();
						setState(1001);
						match(LEFT_PAREN);
						setState(1002);
						match(RIGHT_PAREN);
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
				setState(1008);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public IdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierListContext identifierList() throws RecognitionException {
		IdentifierListContext _localctx = new IdentifierListContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_identifierList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1011);
			match(LEFT_PAREN);
			setState(1012);
			identifierSeq();
			setState(1013);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public IdentifierSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIdentifierSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIdentifierSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIdentifierSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierSeqContext identifierSeq() throws RecognitionException {
		IdentifierSeqContext _localctx = new IdentifierSeqContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_identifierSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1015);
			((IdentifierSeqContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
			((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).errorCapturingIdentifier);
			setState(1020);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,138,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1016);
					match(COMMA);
					setState(1017);
					((IdentifierSeqContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
					((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).errorCapturingIdentifier);
					}
					} 
				}
				setState(1022);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,138,_ctx);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<OrderedIdentifierContext> orderedIdentifier() {
			return getRuleContexts(OrderedIdentifierContext.class);
		}
		public OrderedIdentifierContext orderedIdentifier(int i) {
			return getRuleContext(OrderedIdentifierContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public OrderedIdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderedIdentifierList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterOrderedIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitOrderedIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitOrderedIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierListContext orderedIdentifierList() throws RecognitionException {
		OrderedIdentifierListContext _localctx = new OrderedIdentifierListContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_orderedIdentifierList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1023);
			match(LEFT_PAREN);
			setState(1024);
			orderedIdentifier();
			setState(1029);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1025);
				match(COMMA);
				setState(1026);
				orderedIdentifier();
				}
				}
				setState(1031);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1032);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode ASC() { return getToken(ArcticSqlExtendParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlExtendParser.DESC, 0); }
		public OrderedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterOrderedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitOrderedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitOrderedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierContext orderedIdentifier() throws RecognitionException {
		OrderedIdentifierContext _localctx = new OrderedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_orderedIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1034);
			((OrderedIdentifierContext)_localctx).ident = errorCapturingIdentifier();
			setState(1036);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(1035);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTableValuedFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTableValuedFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTableValuedFunction(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterInlineTableDefault2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitInlineTableDefault2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitInlineTableDefault2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AliasedRelationContext extends RelationPrimaryContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public RelationContext relation() {
			return getRuleContext(RelationContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public SampleContext sample() {
			return getRuleContext(SampleContext.class,0);
		}
		public AliasedRelationContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterAliasedRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitAliasedRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitAliasedRelation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AliasedQueryContext extends RelationPrimaryContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public SampleContext sample() {
			return getRuleContext(SampleContext.class,0);
		}
		public AliasedQueryContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterAliasedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitAliasedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitAliasedQuery(this);
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
		public TemporalClauseContext temporalClause() {
			return getRuleContext(TemporalClauseContext.class,0);
		}
		public SampleContext sample() {
			return getRuleContext(SampleContext.class,0);
		}
		public TableNameContext(RelationPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTableName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTableName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationPrimaryContext relationPrimary() throws RecognitionException {
		RelationPrimaryContext _localctx = new RelationPrimaryContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_relationPrimary);
		try {
			setState(1065);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,145,_ctx) ) {
			case 1:
				_localctx = new TableNameContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1038);
				multipartIdentifier();
				setState(1040);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,141,_ctx) ) {
				case 1:
					{
					setState(1039);
					temporalClause();
					}
					break;
				}
				setState(1043);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,142,_ctx) ) {
				case 1:
					{
					setState(1042);
					sample();
					}
					break;
				}
				setState(1045);
				tableAlias();
				}
				break;
			case 2:
				_localctx = new AliasedQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1047);
				match(LEFT_PAREN);
				setState(1048);
				query();
				setState(1049);
				match(RIGHT_PAREN);
				setState(1051);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
				case 1:
					{
					setState(1050);
					sample();
					}
					break;
				}
				setState(1053);
				tableAlias();
				}
				break;
			case 3:
				_localctx = new AliasedRelationContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1055);
				match(LEFT_PAREN);
				setState(1056);
				relation();
				setState(1057);
				match(RIGHT_PAREN);
				setState(1059);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
				case 1:
					{
					setState(1058);
					sample();
					}
					break;
				}
				setState(1061);
				tableAlias();
				}
				break;
			case 4:
				_localctx = new InlineTableDefault2Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1063);
				inlineTable();
				}
				break;
			case 5:
				_localctx = new TableValuedFunctionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1064);
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
		public TerminalNode VALUES() { return getToken(ArcticSqlExtendParser.VALUES, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public InlineTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterInlineTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitInlineTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitInlineTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableContext inlineTable() throws RecognitionException {
		InlineTableContext _localctx = new InlineTableContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_inlineTable);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1067);
			match(VALUES);
			setState(1068);
			expression();
			setState(1073);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,146,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1069);
					match(COMMA);
					setState(1070);
					expression();
					}
					} 
				}
				setState(1075);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,146,_ctx);
			}
			setState(1076);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
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
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public FunctionTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFunctionTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFunctionTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFunctionTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionTableContext functionTable() throws RecognitionException {
		FunctionTableContext _localctx = new FunctionTableContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_functionTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1078);
			((FunctionTableContext)_localctx).funcName = functionName();
			setState(1079);
			match(LEFT_PAREN);
			setState(1088);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,148,_ctx) ) {
			case 1:
				{
				setState(1080);
				expression();
				setState(1085);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1081);
					match(COMMA);
					setState(1082);
					expression();
					}
					}
					setState(1087);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
			setState(1090);
			match(RIGHT_PAREN);
			setState(1091);
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
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TableAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTableAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTableAlias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTableAlias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableAliasContext tableAlias() throws RecognitionException {
		TableAliasContext _localctx = new TableAliasContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_tableAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1100);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,151,_ctx) ) {
			case 1:
				{
				setState(1094);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,149,_ctx) ) {
				case 1:
					{
					setState(1093);
					match(AS);
					}
					break;
				}
				setState(1096);
				strictIdentifier();
				setState(1098);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,150,_ctx) ) {
				case 1:
					{
					setState(1097);
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
		public PropertyListContext props;
		public TerminalNode ROW() { return getToken(ArcticSqlExtendParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlExtendParser.FORMAT, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlExtendParser.SERDE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticSqlExtendParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlExtendParser.SERDEPROPERTIES, 0); }
		public PropertyListContext propertyList() {
			return getRuleContext(PropertyListContext.class,0);
		}
		public RowFormatSerdeContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterRowFormatSerde(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitRowFormatSerde(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitRowFormatSerde(this);
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
		public TerminalNode ROW() { return getToken(ArcticSqlExtendParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlExtendParser.FORMAT, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSqlExtendParser.DELIMITED, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSqlExtendParser.FIELDS, 0); }
		public List<TerminalNode> TERMINATED() { return getTokens(ArcticSqlExtendParser.TERMINATED); }
		public TerminalNode TERMINATED(int i) {
			return getToken(ArcticSqlExtendParser.TERMINATED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticSqlExtendParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlExtendParser.BY, i);
		}
		public TerminalNode COLLECTION() { return getToken(ArcticSqlExtendParser.COLLECTION, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSqlExtendParser.ITEMS, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlExtendParser.MAP, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSqlExtendParser.KEYS, 0); }
		public TerminalNode LINES() { return getToken(ArcticSqlExtendParser.LINES, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlExtendParser.NULL, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSqlExtendParser.DEFINED, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlExtendParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlExtendParser.STRING, i);
		}
		public TerminalNode ESCAPED() { return getToken(ArcticSqlExtendParser.ESCAPED, 0); }
		public RowFormatDelimitedContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterRowFormatDelimited(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitRowFormatDelimited(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitRowFormatDelimited(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RowFormatContext rowFormat() throws RecognitionException {
		RowFormatContext _localctx = new RowFormatContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_rowFormat);
		try {
			setState(1151);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,159,_ctx) ) {
			case 1:
				_localctx = new RowFormatSerdeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1102);
				match(ROW);
				setState(1103);
				match(FORMAT);
				setState(1104);
				match(SERDE);
				setState(1105);
				((RowFormatSerdeContext)_localctx).name = match(STRING);
				setState(1109);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,152,_ctx) ) {
				case 1:
					{
					setState(1106);
					match(WITH);
					setState(1107);
					match(SERDEPROPERTIES);
					setState(1108);
					((RowFormatSerdeContext)_localctx).props = propertyList();
					}
					break;
				}
				}
				break;
			case 2:
				_localctx = new RowFormatDelimitedContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1111);
				match(ROW);
				setState(1112);
				match(FORMAT);
				setState(1113);
				match(DELIMITED);
				setState(1123);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,154,_ctx) ) {
				case 1:
					{
					setState(1114);
					match(FIELDS);
					setState(1115);
					match(TERMINATED);
					setState(1116);
					match(BY);
					setState(1117);
					((RowFormatDelimitedContext)_localctx).fieldsTerminatedBy = match(STRING);
					setState(1121);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,153,_ctx) ) {
					case 1:
						{
						setState(1118);
						match(ESCAPED);
						setState(1119);
						match(BY);
						setState(1120);
						((RowFormatDelimitedContext)_localctx).escapedBy = match(STRING);
						}
						break;
					}
					}
					break;
				}
				setState(1130);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,155,_ctx) ) {
				case 1:
					{
					setState(1125);
					match(COLLECTION);
					setState(1126);
					match(ITEMS);
					setState(1127);
					match(TERMINATED);
					setState(1128);
					match(BY);
					setState(1129);
					((RowFormatDelimitedContext)_localctx).collectionItemsTerminatedBy = match(STRING);
					}
					break;
				}
				setState(1137);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,156,_ctx) ) {
				case 1:
					{
					setState(1132);
					match(MAP);
					setState(1133);
					match(KEYS);
					setState(1134);
					match(TERMINATED);
					setState(1135);
					match(BY);
					setState(1136);
					((RowFormatDelimitedContext)_localctx).keysTerminatedBy = match(STRING);
					}
					break;
				}
				setState(1143);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,157,_ctx) ) {
				case 1:
					{
					setState(1139);
					match(LINES);
					setState(1140);
					match(TERMINATED);
					setState(1141);
					match(BY);
					setState(1142);
					((RowFormatDelimitedContext)_localctx).linesSeparatedBy = match(STRING);
					}
					break;
				}
				setState(1149);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,158,_ctx) ) {
				case 1:
					{
					setState(1145);
					match(NULL);
					setState(1146);
					match(DEFINED);
					setState(1147);
					match(AS);
					setState(1148);
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

	public static class MultipartIdentifierContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext errorCapturingIdentifier;
		public List<ErrorCapturingIdentifierContext> parts = new ArrayList<ErrorCapturingIdentifierContext>();
		public List<ErrorCapturingIdentifierContext> errorCapturingIdentifier() {
			return getRuleContexts(ErrorCapturingIdentifierContext.class);
		}
		public ErrorCapturingIdentifierContext errorCapturingIdentifier(int i) {
			return getRuleContext(ErrorCapturingIdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(ArcticSqlExtendParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(ArcticSqlExtendParser.DOT, i);
		}
		public MultipartIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multipartIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterMultipartIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitMultipartIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitMultipartIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultipartIdentifierContext multipartIdentifier() throws RecognitionException {
		MultipartIdentifierContext _localctx = new MultipartIdentifierContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_multipartIdentifier);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1153);
			((MultipartIdentifierContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
			((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).errorCapturingIdentifier);
			setState(1158);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,160,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1154);
					match(DOT);
					setState(1155);
					((MultipartIdentifierContext)_localctx).errorCapturingIdentifier = errorCapturingIdentifier();
					((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).errorCapturingIdentifier);
					}
					} 
				}
				setState(1160);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,160,_ctx);
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

	public static class NamedExpressionContext extends ParserRuleContext {
		public ErrorCapturingIdentifierContext name;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public ErrorCapturingIdentifierContext errorCapturingIdentifier() {
			return getRuleContext(ErrorCapturingIdentifierContext.class,0);
		}
		public NamedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNamedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNamedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNamedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionContext namedExpression() throws RecognitionException {
		NamedExpressionContext _localctx = new NamedExpressionContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_namedExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1161);
			expression();
			setState(1169);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				{
				setState(1163);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,161,_ctx) ) {
				case 1:
					{
					setState(1162);
					match(AS);
					}
					break;
				}
				setState(1167);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,162,_ctx) ) {
				case 1:
					{
					setState(1165);
					((NamedExpressionContext)_localctx).name = errorCapturingIdentifier();
					}
					break;
				case 2:
					{
					setState(1166);
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
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public NamedExpressionSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedExpressionSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNamedExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNamedExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNamedExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionSeqContext namedExpressionSeq() throws RecognitionException {
		NamedExpressionSeqContext _localctx = new NamedExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_namedExpressionSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1171);
			namedExpression();
			setState(1176);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,164,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1172);
					match(COMMA);
					setState(1173);
					namedExpression();
					}
					} 
				}
				setState(1178);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,164,_ctx);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<PartitionFieldContext> partitionField() {
			return getRuleContexts(PartitionFieldContext.class);
		}
		public PartitionFieldContext partitionField(int i) {
			return getRuleContext(PartitionFieldContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public PartitionFieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionFieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPartitionFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPartitionFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPartitionFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldListContext partitionFieldList() throws RecognitionException {
		PartitionFieldListContext _localctx = new PartitionFieldListContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_partitionFieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1179);
			match(LEFT_PAREN);
			setState(1180);
			((PartitionFieldListContext)_localctx).partitionField = partitionField();
			((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
			setState(1185);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1181);
				match(COMMA);
				setState(1182);
				((PartitionFieldListContext)_localctx).partitionField = partitionField();
				((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
				}
				}
				setState(1187);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1188);
			match(RIGHT_PAREN);
			}
		}
		catch (RecognitionException re) {
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPartitionColumn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPartitionColumn(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPartitionColumn(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPartitionTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPartitionTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPartitionTransform(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldContext partitionField() throws RecognitionException {
		PartitionFieldContext _localctx = new PartitionFieldContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_partitionField);
		try {
			setState(1192);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,166,_ctx) ) {
			case 1:
				_localctx = new PartitionTransformContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1190);
				transform();
				}
				break;
			case 2:
				_localctx = new PartitionColumnContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1191);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIdentityTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIdentityTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIdentityTransform(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ApplyTransformContext extends TransformContext {
		public IdentifierContext transformName;
		public TransformArgumentContext transformArgument;
		public List<TransformArgumentContext> argument = new ArrayList<TransformArgumentContext>();
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<TransformArgumentContext> transformArgument() {
			return getRuleContexts(TransformArgumentContext.class);
		}
		public TransformArgumentContext transformArgument(int i) {
			return getRuleContext(TransformArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public ApplyTransformContext(TransformContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterApplyTransform(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitApplyTransform(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitApplyTransform(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformContext transform() throws RecognitionException {
		TransformContext _localctx = new TransformContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_transform);
		int _la;
		try {
			setState(1207);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,168,_ctx) ) {
			case 1:
				_localctx = new IdentityTransformContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1194);
				qualifiedName();
				}
				break;
			case 2:
				_localctx = new ApplyTransformContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1195);
				((ApplyTransformContext)_localctx).transformName = identifier();
				setState(1196);
				match(LEFT_PAREN);
				setState(1197);
				((ApplyTransformContext)_localctx).transformArgument = transformArgument();
				((ApplyTransformContext)_localctx).argument.add(((ApplyTransformContext)_localctx).transformArgument);
				setState(1202);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1198);
					match(COMMA);
					setState(1199);
					((ApplyTransformContext)_localctx).transformArgument = transformArgument();
					((ApplyTransformContext)_localctx).argument.add(((ApplyTransformContext)_localctx).transformArgument);
					}
					}
					setState(1204);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1205);
				match(RIGHT_PAREN);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTransformArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTransformArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTransformArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TransformArgumentContext transformArgument() throws RecognitionException {
		TransformArgumentContext _localctx = new TransformArgumentContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_transformArgument);
		try {
			setState(1211);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,169,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1209);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1210);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1213);
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
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public ExpressionSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionSeqContext expressionSeq() throws RecognitionException {
		ExpressionSeqContext _localctx = new ExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_expressionSeq);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1215);
			expression();
			setState(1220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1216);
				match(COMMA);
				setState(1217);
				expression();
				}
				}
				setState(1222);
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
		public TerminalNode NOT() { return getToken(ArcticSqlExtendParser.NOT, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public LogicalNotContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLogicalNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLogicalNot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLogicalNot(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPredicated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPredicated(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPredicated(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExistsContext extends BooleanExpressionContext {
		public TerminalNode EXISTS() { return getToken(ArcticSqlExtendParser.EXISTS, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public ExistsContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExists(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExists(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExists(this);
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
		public TerminalNode AND() { return getToken(ArcticSqlExtendParser.AND, 0); }
		public TerminalNode OR() { return getToken(ArcticSqlExtendParser.OR, 0); }
		public LogicalBinaryContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLogicalBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLogicalBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLogicalBinary(this);
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
		int _startState = 144;
		enterRecursionRule(_localctx, 144, RULE_booleanExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1235);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,172,_ctx) ) {
			case 1:
				{
				_localctx = new LogicalNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1224);
				match(NOT);
				setState(1225);
				booleanExpression(5);
				}
				break;
			case 2:
				{
				_localctx = new ExistsContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1226);
				match(EXISTS);
				setState(1227);
				match(LEFT_PAREN);
				setState(1228);
				query();
				setState(1229);
				match(RIGHT_PAREN);
				}
				break;
			case 3:
				{
				_localctx = new PredicatedContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1231);
				valueExpression(0);
				setState(1233);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,171,_ctx) ) {
				case 1:
					{
					setState(1232);
					predicate();
					}
					break;
				}
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1245);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,174,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1243);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,173,_ctx) ) {
					case 1:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(1237);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1238);
						((LogicalBinaryContext)_localctx).operator = match(AND);
						setState(1239);
						((LogicalBinaryContext)_localctx).right = booleanExpression(3);
						}
						break;
					case 2:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(1240);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1241);
						((LogicalBinaryContext)_localctx).operator = match(OR);
						setState(1242);
						((LogicalBinaryContext)_localctx).right = booleanExpression(2);
						}
						break;
					}
					} 
				}
				setState(1247);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,174,_ctx);
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
		public TerminalNode AND() { return getToken(ArcticSqlExtendParser.AND, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlExtendParser.BETWEEN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode NOT() { return getToken(ArcticSqlExtendParser.NOT, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlExtendParser.IN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RLIKE() { return getToken(ArcticSqlExtendParser.RLIKE, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlExtendParser.LIKE, 0); }
		public TerminalNode ILIKE() { return getToken(ArcticSqlExtendParser.ILIKE, 0); }
		public TerminalNode ANY() { return getToken(ArcticSqlExtendParser.ANY, 0); }
		public TerminalNode SOME() { return getToken(ArcticSqlExtendParser.SOME, 0); }
		public TerminalNode ALL() { return getToken(ArcticSqlExtendParser.ALL, 0); }
		public TerminalNode ESCAPE() { return getToken(ArcticSqlExtendParser.ESCAPE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public TerminalNode IS() { return getToken(ArcticSqlExtendParser.IS, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlExtendParser.NULL, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSqlExtendParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSqlExtendParser.FALSE, 0); }
		public TerminalNode UNKNOWN() { return getToken(ArcticSqlExtendParser.UNKNOWN, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticSqlExtendParser.DISTINCT, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_predicate);
		int _la;
		try {
			setState(1330);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,188,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1249);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1248);
					match(NOT);
					}
				}

				setState(1251);
				((PredicateContext)_localctx).kind = match(BETWEEN);
				setState(1252);
				((PredicateContext)_localctx).lower = valueExpression(0);
				setState(1253);
				match(AND);
				setState(1254);
				((PredicateContext)_localctx).upper = valueExpression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1257);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1256);
					match(NOT);
					}
				}

				setState(1259);
				((PredicateContext)_localctx).kind = match(IN);
				setState(1260);
				match(LEFT_PAREN);
				setState(1261);
				expression();
				setState(1266);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1262);
					match(COMMA);
					setState(1263);
					expression();
					}
					}
					setState(1268);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1269);
				match(RIGHT_PAREN);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1272);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1271);
					match(NOT);
					}
				}

				setState(1274);
				((PredicateContext)_localctx).kind = match(IN);
				setState(1275);
				match(LEFT_PAREN);
				setState(1276);
				query();
				setState(1277);
				match(RIGHT_PAREN);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1280);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1279);
					match(NOT);
					}
				}

				setState(1282);
				((PredicateContext)_localctx).kind = match(RLIKE);
				setState(1283);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1285);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1284);
					match(NOT);
					}
				}

				setState(1287);
				((PredicateContext)_localctx).kind = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==LIKE || _la==ILIKE) ) {
					((PredicateContext)_localctx).kind = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1288);
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
				setState(1302);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,182,_ctx) ) {
				case 1:
					{
					setState(1289);
					match(LEFT_PAREN);
					setState(1290);
					match(RIGHT_PAREN);
					}
					break;
				case 2:
					{
					setState(1291);
					match(LEFT_PAREN);
					setState(1292);
					expression();
					setState(1297);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1293);
						match(COMMA);
						setState(1294);
						expression();
						}
						}
						setState(1299);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(1300);
					match(RIGHT_PAREN);
					}
					break;
				}
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1305);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1304);
					match(NOT);
					}
				}

				setState(1307);
				((PredicateContext)_localctx).kind = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==LIKE || _la==ILIKE) ) {
					((PredicateContext)_localctx).kind = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1308);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				setState(1311);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,184,_ctx) ) {
				case 1:
					{
					setState(1309);
					match(ESCAPE);
					setState(1310);
					((PredicateContext)_localctx).escapeChar = match(STRING);
					}
					break;
				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1313);
				match(IS);
				setState(1315);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1314);
					match(NOT);
					}
				}

				setState(1317);
				((PredicateContext)_localctx).kind = match(NULL);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1318);
				match(IS);
				setState(1320);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1319);
					match(NOT);
					}
				}

				setState(1322);
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
				setState(1323);
				match(IS);
				setState(1325);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1324);
					match(NOT);
					}
				}

				setState(1327);
				((PredicateContext)_localctx).kind = match(DISTINCT);
				setState(1328);
				match(FROM);
				setState(1329);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterValueExpressionDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitValueExpressionDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitValueExpressionDefault(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitComparison(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitComparison(this);
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
		public TerminalNode ASTERISK() { return getToken(ArcticSqlExtendParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticSqlExtendParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticSqlExtendParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlExtendParser.DIV, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSqlExtendParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticSqlExtendParser.CONCAT_PIPE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticSqlExtendParser.AMPERSAND, 0); }
		public TerminalNode HAT() { return getToken(ArcticSqlExtendParser.HAT, 0); }
		public TerminalNode PIPE() { return getToken(ArcticSqlExtendParser.PIPE, 0); }
		public ArithmeticBinaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterArithmeticBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitArithmeticBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitArithmeticBinary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ArithmeticUnaryContext extends ValueExpressionContext {
		public Token operator;
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSqlExtendParser.PLUS, 0); }
		public TerminalNode TILDE() { return getToken(ArcticSqlExtendParser.TILDE, 0); }
		public ArithmeticUnaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterArithmeticUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitArithmeticUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitArithmeticUnary(this);
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
		int _startState = 148;
		enterRecursionRule(_localctx, 148, RULE_valueExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1336);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,189,_ctx) ) {
			case 1:
				{
				_localctx = new ValueExpressionDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1333);
				primaryExpression(0);
				}
				break;
			case 2:
				{
				_localctx = new ArithmeticUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1334);
				((ArithmeticUnaryContext)_localctx).operator = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 296)) & ~0x3f) == 0 && ((1L << (_la - 296)) & ((1L << (PLUS - 296)) | (1L << (MINUS - 296)) | (1L << (TILDE - 296)))) != 0)) ) {
					((ArithmeticUnaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1335);
				valueExpression(7);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1359);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,191,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1357);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,190,_ctx) ) {
					case 1:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1338);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1339);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==DIV || ((((_la - 298)) & ~0x3f) == 0 && ((1L << (_la - 298)) & ((1L << (ASTERISK - 298)) | (1L << (SLASH - 298)) | (1L << (PERCENT - 298)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1340);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(7);
						}
						break;
					case 2:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1341);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1342);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 296)) & ~0x3f) == 0 && ((1L << (_la - 296)) & ((1L << (PLUS - 296)) | (1L << (MINUS - 296)) | (1L << (CONCAT_PIPE - 296)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1343);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(6);
						}
						break;
					case 3:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1344);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1345);
						((ArithmeticBinaryContext)_localctx).operator = match(AMPERSAND);
						setState(1346);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(5);
						}
						break;
					case 4:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1347);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1348);
						((ArithmeticBinaryContext)_localctx).operator = match(HAT);
						setState(1349);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(4);
						}
						break;
					case 5:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1350);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1351);
						((ArithmeticBinaryContext)_localctx).operator = match(PIPE);
						setState(1352);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(3);
						}
						break;
					case 6:
						{
						_localctx = new ComparisonContext(new ValueExpressionContext(_parentctx, _parentState));
						((ComparisonContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1353);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1354);
						comparisonOperator();
						setState(1355);
						((ComparisonContext)_localctx).right = valueExpression(2);
						}
						break;
					}
					} 
				}
				setState(1361);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,191,_ctx);
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

	public static class DatetimeUnitContext extends ParserRuleContext {
		public TerminalNode YEAR() { return getToken(ArcticSqlExtendParser.YEAR, 0); }
		public TerminalNode QUARTER() { return getToken(ArcticSqlExtendParser.QUARTER, 0); }
		public TerminalNode MONTH() { return getToken(ArcticSqlExtendParser.MONTH, 0); }
		public TerminalNode WEEK() { return getToken(ArcticSqlExtendParser.WEEK, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlExtendParser.DAY, 0); }
		public TerminalNode DAYOFYEAR() { return getToken(ArcticSqlExtendParser.DAYOFYEAR, 0); }
		public TerminalNode HOUR() { return getToken(ArcticSqlExtendParser.HOUR, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticSqlExtendParser.MINUTE, 0); }
		public TerminalNode SECOND() { return getToken(ArcticSqlExtendParser.SECOND, 0); }
		public TerminalNode MILLISECOND() { return getToken(ArcticSqlExtendParser.MILLISECOND, 0); }
		public TerminalNode MICROSECOND() { return getToken(ArcticSqlExtendParser.MICROSECOND, 0); }
		public DatetimeUnitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_datetimeUnit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterDatetimeUnit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitDatetimeUnit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitDatetimeUnit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DatetimeUnitContext datetimeUnit() throws RecognitionException {
		DatetimeUnitContext _localctx = new DatetimeUnitContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_datetimeUnit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1362);
			_la = _input.LA(1);
			if ( !(_la==DAY || _la==DAYOFYEAR || ((((_la - 111)) & ~0x3f) == 0 && ((1L << (_la - 111)) & ((1L << (HOUR - 111)) | (1L << (MICROSECOND - 111)) | (1L << (MILLISECOND - 111)) | (1L << (MINUTE - 111)) | (1L << (MONTH - 111)))) != 0) || _la==QUARTER || _la==SECOND || _la==WEEK || _la==YEAR) ) {
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
		public TerminalNode STRUCT() { return getToken(ArcticSqlExtendParser.STRUCT, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public StructContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterStruct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitStruct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitStruct(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DereferenceContext extends PrimaryExpressionContext {
		public PrimaryExpressionContext base;
		public IdentifierContext fieldName;
		public TerminalNode DOT() { return getToken(ArcticSqlExtendParser.DOT, 0); }
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public DereferenceContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterDereference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitDereference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitDereference(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TimestampaddContext extends PrimaryExpressionContext {
		public Token name;
		public DatetimeUnitContext unit;
		public ValueExpressionContext unitsAmount;
		public ValueExpressionContext timestamp;
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public DatetimeUnitContext datetimeUnit() {
			return getRuleContext(DatetimeUnitContext.class,0);
		}
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode TIMESTAMPADD() { return getToken(ArcticSqlExtendParser.TIMESTAMPADD, 0); }
		public TerminalNode DATEADD() { return getToken(ArcticSqlExtendParser.DATEADD, 0); }
		public TimestampaddContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTimestampadd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTimestampadd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTimestampadd(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubstringContext extends PrimaryExpressionContext {
		public ValueExpressionContext str;
		public ValueExpressionContext pos;
		public ValueExpressionContext len;
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticSqlExtendParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticSqlExtendParser.SUBSTRING, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public TerminalNode FOR() { return getToken(ArcticSqlExtendParser.FOR, 0); }
		public SubstringContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSubstring(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSubstring(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSubstring(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CastContext extends PrimaryExpressionContext {
		public Token name;
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode CAST() { return getToken(ArcticSqlExtendParser.CAST, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticSqlExtendParser.TRY_CAST, 0); }
		public CastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCast(this);
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
		public TerminalNode ARROW() { return getToken(ArcticSqlExtendParser.ARROW, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public LambdaContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLambda(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLambda(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLambda(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParenthesizedExpressionContext extends PrimaryExpressionContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public ParenthesizedExpressionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterParenthesizedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitParenthesizedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitParenthesizedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TrimContext extends PrimaryExpressionContext {
		public Token trimOption;
		public ValueExpressionContext trimStr;
		public ValueExpressionContext srcStr;
		public TerminalNode TRIM() { return getToken(ArcticSqlExtendParser.TRIM, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode BOTH() { return getToken(ArcticSqlExtendParser.BOTH, 0); }
		public TerminalNode LEADING() { return getToken(ArcticSqlExtendParser.LEADING, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticSqlExtendParser.TRAILING, 0); }
		public TrimContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTrim(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTrim(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTrim(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleCaseContext extends PrimaryExpressionContext {
		public ExpressionContext value;
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticSqlExtendParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticSqlExtendParser.END, 0); }
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
		public TerminalNode ELSE() { return getToken(ArcticSqlExtendParser.ELSE, 0); }
		public SimpleCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSimpleCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSimpleCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSimpleCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CurrentLikeContext extends PrimaryExpressionContext {
		public Token name;
		public TerminalNode CURRENT_DATE() { return getToken(ArcticSqlExtendParser.CURRENT_DATE, 0); }
		public TerminalNode CURRENT_TIMESTAMP() { return getToken(ArcticSqlExtendParser.CURRENT_TIMESTAMP, 0); }
		public TerminalNode CURRENT_USER() { return getToken(ArcticSqlExtendParser.CURRENT_USER, 0); }
		public CurrentLikeContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterCurrentLike(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitCurrentLike(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitCurrentLike(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterColumnReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitColumnReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitColumnReference(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RowConstructorContext extends PrimaryExpressionContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public RowConstructorContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterRowConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitRowConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitRowConstructor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LastContext extends PrimaryExpressionContext {
		public TerminalNode LAST() { return getToken(ArcticSqlExtendParser.LAST, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSqlExtendParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlExtendParser.NULLS, 0); }
		public LastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLast(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StarContext extends PrimaryExpressionContext {
		public TerminalNode ASTERISK() { return getToken(ArcticSqlExtendParser.ASTERISK, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(ArcticSqlExtendParser.DOT, 0); }
		public StarContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterStar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitStar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitStar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OverlayContext extends PrimaryExpressionContext {
		public ValueExpressionContext input;
		public ValueExpressionContext replace;
		public ValueExpressionContext position;
		public ValueExpressionContext length;
		public TerminalNode OVERLAY() { return getToken(ArcticSqlExtendParser.OVERLAY, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode PLACING() { return getToken(ArcticSqlExtendParser.PLACING, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode FOR() { return getToken(ArcticSqlExtendParser.FOR, 0); }
		public OverlayContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterOverlay(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitOverlay(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitOverlay(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubscriptContext extends PrimaryExpressionContext {
		public PrimaryExpressionContext value;
		public ValueExpressionContext index;
		public TerminalNode LEFT_BRACKET() { return getToken(ArcticSqlExtendParser.LEFT_BRACKET, 0); }
		public TerminalNode RIGHT_BRACKET() { return getToken(ArcticSqlExtendParser.RIGHT_BRACKET, 0); }
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public SubscriptContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSubscript(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSubscript(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSubscript(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TimestampdiffContext extends PrimaryExpressionContext {
		public Token name;
		public DatetimeUnitContext unit;
		public ValueExpressionContext startTimestamp;
		public ValueExpressionContext endTimestamp;
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public DatetimeUnitContext datetimeUnit() {
			return getRuleContext(DatetimeUnitContext.class,0);
		}
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode TIMESTAMPDIFF() { return getToken(ArcticSqlExtendParser.TIMESTAMPDIFF, 0); }
		public TerminalNode DATEDIFF() { return getToken(ArcticSqlExtendParser.DATEDIFF, 0); }
		public TimestampdiffContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTimestampdiff(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTimestampdiff(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTimestampdiff(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SubqueryExpressionContext extends PrimaryExpressionContext {
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public SubqueryExpressionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSubqueryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSubqueryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSubqueryExpression(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterConstantDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitConstantDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitConstantDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExtractContext extends PrimaryExpressionContext {
		public IdentifierContext field;
		public ValueExpressionContext source;
		public TerminalNode EXTRACT() { return getToken(ArcticSqlExtendParser.EXTRACT, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public ExtractContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExtract(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExtract(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExtract(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PercentileContext extends PrimaryExpressionContext {
		public Token name;
		public ValueExpressionContext percentage;
		public List<TerminalNode> LEFT_PAREN() { return getTokens(ArcticSqlExtendParser.LEFT_PAREN); }
		public TerminalNode LEFT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.LEFT_PAREN, i);
		}
		public List<TerminalNode> RIGHT_PAREN() { return getTokens(ArcticSqlExtendParser.RIGHT_PAREN); }
		public TerminalNode RIGHT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.RIGHT_PAREN, i);
		}
		public TerminalNode WITHIN() { return getToken(ArcticSqlExtendParser.WITHIN, 0); }
		public TerminalNode GROUP() { return getToken(ArcticSqlExtendParser.GROUP, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSqlExtendParser.ORDER, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlExtendParser.BY, 0); }
		public SortItemContext sortItem() {
			return getRuleContext(SortItemContext.class,0);
		}
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TerminalNode PERCENTILE_CONT() { return getToken(ArcticSqlExtendParser.PERCENTILE_CONT, 0); }
		public TerminalNode PERCENTILE_DISC() { return getToken(ArcticSqlExtendParser.PERCENTILE_DISC, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlExtendParser.OVER, 0); }
		public WindowSpecContext windowSpec() {
			return getRuleContext(WindowSpecContext.class,0);
		}
		public PercentileContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPercentile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPercentile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPercentile(this);
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
		public List<TerminalNode> LEFT_PAREN() { return getTokens(ArcticSqlExtendParser.LEFT_PAREN); }
		public TerminalNode LEFT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.LEFT_PAREN, i);
		}
		public List<TerminalNode> RIGHT_PAREN() { return getTokens(ArcticSqlExtendParser.RIGHT_PAREN); }
		public TerminalNode RIGHT_PAREN(int i) {
			return getToken(ArcticSqlExtendParser.RIGHT_PAREN, i);
		}
		public TerminalNode FILTER() { return getToken(ArcticSqlExtendParser.FILTER, 0); }
		public TerminalNode WHERE() { return getToken(ArcticSqlExtendParser.WHERE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlExtendParser.NULLS, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlExtendParser.OVER, 0); }
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
		public TerminalNode IGNORE() { return getToken(ArcticSqlExtendParser.IGNORE, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticSqlExtendParser.RESPECT, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public FunctionCallContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SearchedCaseContext extends PrimaryExpressionContext {
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticSqlExtendParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticSqlExtendParser.END, 0); }
		public List<WhenClauseContext> whenClause() {
			return getRuleContexts(WhenClauseContext.class);
		}
		public WhenClauseContext whenClause(int i) {
			return getRuleContext(WhenClauseContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(ArcticSqlExtendParser.ELSE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SearchedCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSearchedCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSearchedCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSearchedCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PositionContext extends PrimaryExpressionContext {
		public ValueExpressionContext substr;
		public ValueExpressionContext str;
		public TerminalNode POSITION() { return getToken(ArcticSqlExtendParser.POSITION, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlExtendParser.IN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public PositionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPosition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FirstContext extends PrimaryExpressionContext {
		public TerminalNode FIRST() { return getToken(ArcticSqlExtendParser.FIRST, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSqlExtendParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlExtendParser.NULLS, 0); }
		public FirstContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFirst(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFirst(this);
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
		int _startState = 152;
		enterRecursionRule(_localctx, 152, RULE_primaryExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1585);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,213,_ctx) ) {
			case 1:
				{
				_localctx = new CurrentLikeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1365);
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
				_localctx = new TimestampaddContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1366);
				((TimestampaddContext)_localctx).name = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==DATEADD || _la==TIMESTAMPADD) ) {
					((TimestampaddContext)_localctx).name = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1367);
				match(LEFT_PAREN);
				setState(1368);
				((TimestampaddContext)_localctx).unit = datetimeUnit();
				setState(1369);
				match(COMMA);
				setState(1370);
				((TimestampaddContext)_localctx).unitsAmount = valueExpression(0);
				setState(1371);
				match(COMMA);
				setState(1372);
				((TimestampaddContext)_localctx).timestamp = valueExpression(0);
				setState(1373);
				match(RIGHT_PAREN);
				}
				break;
			case 3:
				{
				_localctx = new TimestampdiffContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1375);
				((TimestampdiffContext)_localctx).name = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==DATEDIFF || _la==TIMESTAMPDIFF) ) {
					((TimestampdiffContext)_localctx).name = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1376);
				match(LEFT_PAREN);
				setState(1377);
				((TimestampdiffContext)_localctx).unit = datetimeUnit();
				setState(1378);
				match(COMMA);
				setState(1379);
				((TimestampdiffContext)_localctx).startTimestamp = valueExpression(0);
				setState(1380);
				match(COMMA);
				setState(1381);
				((TimestampdiffContext)_localctx).endTimestamp = valueExpression(0);
				setState(1382);
				match(RIGHT_PAREN);
				}
				break;
			case 4:
				{
				_localctx = new SearchedCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1384);
				match(CASE);
				setState(1386); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1385);
					whenClause();
					}
					}
					setState(1388); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(1392);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(1390);
					match(ELSE);
					setState(1391);
					((SearchedCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(1394);
				match(END);
				}
				break;
			case 5:
				{
				_localctx = new SimpleCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1396);
				match(CASE);
				setState(1397);
				((SimpleCaseContext)_localctx).value = expression();
				setState(1399); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1398);
					whenClause();
					}
					}
					setState(1401); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(1405);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(1403);
					match(ELSE);
					setState(1404);
					((SimpleCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(1407);
				match(END);
				}
				break;
			case 6:
				{
				_localctx = new CastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1409);
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
				setState(1410);
				match(LEFT_PAREN);
				setState(1411);
				expression();
				setState(1412);
				match(AS);
				setState(1413);
				dataType();
				setState(1414);
				match(RIGHT_PAREN);
				}
				break;
			case 7:
				{
				_localctx = new StructContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1416);
				match(STRUCT);
				setState(1417);
				match(LEFT_PAREN);
				setState(1426);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,197,_ctx) ) {
				case 1:
					{
					setState(1418);
					((StructContext)_localctx).namedExpression = namedExpression();
					((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
					setState(1423);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1419);
						match(COMMA);
						setState(1420);
						((StructContext)_localctx).namedExpression = namedExpression();
						((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
						}
						}
						setState(1425);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(1428);
				match(RIGHT_PAREN);
				}
				break;
			case 8:
				{
				_localctx = new FirstContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1429);
				match(FIRST);
				setState(1430);
				match(LEFT_PAREN);
				setState(1431);
				expression();
				setState(1434);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(1432);
					match(IGNORE);
					setState(1433);
					match(NULLS);
					}
				}

				setState(1436);
				match(RIGHT_PAREN);
				}
				break;
			case 9:
				{
				_localctx = new LastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1438);
				match(LAST);
				setState(1439);
				match(LEFT_PAREN);
				setState(1440);
				expression();
				setState(1443);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(1441);
					match(IGNORE);
					setState(1442);
					match(NULLS);
					}
				}

				setState(1445);
				match(RIGHT_PAREN);
				}
				break;
			case 10:
				{
				_localctx = new PositionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1447);
				match(POSITION);
				setState(1448);
				match(LEFT_PAREN);
				setState(1449);
				((PositionContext)_localctx).substr = valueExpression(0);
				setState(1450);
				match(IN);
				setState(1451);
				((PositionContext)_localctx).str = valueExpression(0);
				setState(1452);
				match(RIGHT_PAREN);
				}
				break;
			case 11:
				{
				_localctx = new ConstantDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1454);
				constant();
				}
				break;
			case 12:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1455);
				match(ASTERISK);
				}
				break;
			case 13:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1456);
				qualifiedName();
				setState(1457);
				match(DOT);
				setState(1458);
				match(ASTERISK);
				}
				break;
			case 14:
				{
				_localctx = new RowConstructorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1460);
				match(LEFT_PAREN);
				setState(1461);
				namedExpression();
				setState(1464); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1462);
					match(COMMA);
					setState(1463);
					namedExpression();
					}
					}
					setState(1466); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				setState(1468);
				match(RIGHT_PAREN);
				}
				break;
			case 15:
				{
				_localctx = new SubqueryExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1470);
				match(LEFT_PAREN);
				setState(1471);
				query();
				setState(1472);
				match(RIGHT_PAREN);
				}
				break;
			case 16:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1474);
				functionName();
				setState(1475);
				match(LEFT_PAREN);
				setState(1487);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,203,_ctx) ) {
				case 1:
					{
					setState(1477);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,201,_ctx) ) {
					case 1:
						{
						setState(1476);
						setQuantifier();
						}
						break;
					}
					setState(1479);
					((FunctionCallContext)_localctx).expression = expression();
					((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
					setState(1484);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1480);
						match(COMMA);
						setState(1481);
						((FunctionCallContext)_localctx).expression = expression();
						((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
						}
						}
						setState(1486);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				setState(1489);
				match(RIGHT_PAREN);
				setState(1496);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,204,_ctx) ) {
				case 1:
					{
					setState(1490);
					match(FILTER);
					setState(1491);
					match(LEFT_PAREN);
					setState(1492);
					match(WHERE);
					setState(1493);
					((FunctionCallContext)_localctx).where = booleanExpression(0);
					setState(1494);
					match(RIGHT_PAREN);
					}
					break;
				}
				setState(1500);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,205,_ctx) ) {
				case 1:
					{
					setState(1498);
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
					setState(1499);
					match(NULLS);
					}
					break;
				}
				setState(1504);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,206,_ctx) ) {
				case 1:
					{
					setState(1502);
					match(OVER);
					setState(1503);
					windowSpec();
					}
					break;
				}
				}
				break;
			case 17:
				{
				_localctx = new LambdaContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1506);
				identifier();
				setState(1507);
				match(ARROW);
				setState(1508);
				expression();
				}
				break;
			case 18:
				{
				_localctx = new LambdaContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1510);
				match(LEFT_PAREN);
				setState(1511);
				identifier();
				setState(1514); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1512);
					match(COMMA);
					setState(1513);
					identifier();
					}
					}
					setState(1516); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				setState(1518);
				match(RIGHT_PAREN);
				setState(1519);
				match(ARROW);
				setState(1520);
				expression();
				}
				break;
			case 19:
				{
				_localctx = new ColumnReferenceContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1522);
				identifier();
				}
				break;
			case 20:
				{
				_localctx = new ParenthesizedExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1523);
				match(LEFT_PAREN);
				setState(1524);
				expression();
				setState(1525);
				match(RIGHT_PAREN);
				}
				break;
			case 21:
				{
				_localctx = new ExtractContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1527);
				match(EXTRACT);
				setState(1528);
				match(LEFT_PAREN);
				setState(1529);
				((ExtractContext)_localctx).field = identifier();
				setState(1530);
				match(FROM);
				setState(1531);
				((ExtractContext)_localctx).source = valueExpression(0);
				setState(1532);
				match(RIGHT_PAREN);
				}
				break;
			case 22:
				{
				_localctx = new SubstringContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1534);
				_la = _input.LA(1);
				if ( !(_la==SUBSTR || _la==SUBSTRING) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1535);
				match(LEFT_PAREN);
				setState(1536);
				((SubstringContext)_localctx).str = valueExpression(0);
				setState(1537);
				_la = _input.LA(1);
				if ( !(_la==COMMA || _la==FROM) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1538);
				((SubstringContext)_localctx).pos = valueExpression(0);
				setState(1541);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA || _la==FOR) {
					{
					setState(1539);
					_la = _input.LA(1);
					if ( !(_la==COMMA || _la==FOR) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(1540);
					((SubstringContext)_localctx).len = valueExpression(0);
					}
				}

				setState(1543);
				match(RIGHT_PAREN);
				}
				break;
			case 23:
				{
				_localctx = new TrimContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1545);
				match(TRIM);
				setState(1546);
				match(LEFT_PAREN);
				setState(1548);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,209,_ctx) ) {
				case 1:
					{
					setState(1547);
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
				setState(1551);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,210,_ctx) ) {
				case 1:
					{
					setState(1550);
					((TrimContext)_localctx).trimStr = valueExpression(0);
					}
					break;
				}
				setState(1553);
				match(FROM);
				setState(1554);
				((TrimContext)_localctx).srcStr = valueExpression(0);
				setState(1555);
				match(RIGHT_PAREN);
				}
				break;
			case 24:
				{
				_localctx = new OverlayContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1557);
				match(OVERLAY);
				setState(1558);
				match(LEFT_PAREN);
				setState(1559);
				((OverlayContext)_localctx).input = valueExpression(0);
				setState(1560);
				match(PLACING);
				setState(1561);
				((OverlayContext)_localctx).replace = valueExpression(0);
				setState(1562);
				match(FROM);
				setState(1563);
				((OverlayContext)_localctx).position = valueExpression(0);
				setState(1566);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FOR) {
					{
					setState(1564);
					match(FOR);
					setState(1565);
					((OverlayContext)_localctx).length = valueExpression(0);
					}
				}

				setState(1568);
				match(RIGHT_PAREN);
				}
				break;
			case 25:
				{
				_localctx = new PercentileContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1570);
				((PercentileContext)_localctx).name = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PERCENTILE_CONT || _la==PERCENTILE_DISC) ) {
					((PercentileContext)_localctx).name = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1571);
				match(LEFT_PAREN);
				setState(1572);
				((PercentileContext)_localctx).percentage = valueExpression(0);
				setState(1573);
				match(RIGHT_PAREN);
				setState(1574);
				match(WITHIN);
				setState(1575);
				match(GROUP);
				setState(1576);
				match(LEFT_PAREN);
				setState(1577);
				match(ORDER);
				setState(1578);
				match(BY);
				setState(1579);
				sortItem();
				setState(1580);
				match(RIGHT_PAREN);
				setState(1583);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,212,_ctx) ) {
				case 1:
					{
					setState(1581);
					match(OVER);
					setState(1582);
					windowSpec();
					}
					break;
				}
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1597);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,215,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1595);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,214,_ctx) ) {
					case 1:
						{
						_localctx = new SubscriptContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((SubscriptContext)_localctx).value = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(1587);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1588);
						match(LEFT_BRACKET);
						setState(1589);
						((SubscriptContext)_localctx).index = valueExpression(0);
						setState(1590);
						match(RIGHT_BRACKET);
						}
						break;
					case 2:
						{
						_localctx = new DereferenceContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((DereferenceContext)_localctx).base = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(1592);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1593);
						match(DOT);
						setState(1594);
						((DereferenceContext)_localctx).fieldName = identifier();
						}
						break;
					}
					} 
				}
				setState(1599);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,215,_ctx);
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
		public TerminalNode NULL() { return getToken(ArcticSqlExtendParser.NULL, 0); }
		public NullLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNullLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNullLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNullLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ConstantContext {
		public List<TerminalNode> STRING() { return getTokens(ArcticSqlExtendParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSqlExtendParser.STRING, i);
		}
		public StringLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TypeConstructorContext extends ConstantContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public TypeConstructorContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTypeConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTypeConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTypeConstructor(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIntervalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIntervalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIntervalLiteral(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNumericLiteral(this);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_constant);
		try {
			int _alt;
			setState(1612);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,217,_ctx) ) {
			case 1:
				_localctx = new NullLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1600);
				match(NULL);
				}
				break;
			case 2:
				_localctx = new IntervalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1601);
				interval();
				}
				break;
			case 3:
				_localctx = new TypeConstructorContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1602);
				identifier();
				setState(1603);
				match(STRING);
				}
				break;
			case 4:
				_localctx = new NumericLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1605);
				number();
				}
				break;
			case 5:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1606);
				booleanValue();
				}
				break;
			case 6:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1608); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(1607);
						match(STRING);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(1610); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,216,_ctx);
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
		public TerminalNode EQ() { return getToken(ArcticSqlExtendParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(ArcticSqlExtendParser.NEQ, 0); }
		public TerminalNode NEQJ() { return getToken(ArcticSqlExtendParser.NEQJ, 0); }
		public TerminalNode LT() { return getToken(ArcticSqlExtendParser.LT, 0); }
		public TerminalNode LTE() { return getToken(ArcticSqlExtendParser.LTE, 0); }
		public TerminalNode GT() { return getToken(ArcticSqlExtendParser.GT, 0); }
		public TerminalNode GTE() { return getToken(ArcticSqlExtendParser.GTE, 0); }
		public TerminalNode NSEQ() { return getToken(ArcticSqlExtendParser.NSEQ, 0); }
		public ComparisonOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterComparisonOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitComparisonOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitComparisonOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOperatorContext comparisonOperator() throws RecognitionException {
		ComparisonOperatorContext _localctx = new ComparisonOperatorContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_comparisonOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1614);
			_la = _input.LA(1);
			if ( !(((((_la - 288)) & ~0x3f) == 0 && ((1L << (_la - 288)) & ((1L << (EQ - 288)) | (1L << (NSEQ - 288)) | (1L << (NEQ - 288)) | (1L << (NEQJ - 288)) | (1L << (LT - 288)) | (1L << (LTE - 288)) | (1L << (GT - 288)) | (1L << (GTE - 288)))) != 0)) ) {
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
		public TerminalNode TRUE() { return getToken(ArcticSqlExtendParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSqlExtendParser.FALSE, 0); }
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1616);
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
		public TerminalNode INTERVAL() { return getToken(ArcticSqlExtendParser.INTERVAL, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalContext interval() throws RecognitionException {
		IntervalContext _localctx = new IntervalContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_interval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1618);
			match(INTERVAL);
			setState(1621);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,218,_ctx) ) {
			case 1:
				{
				setState(1619);
				errorCapturingMultiUnitsInterval();
				}
				break;
			case 2:
				{
				setState(1620);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterErrorCapturingMultiUnitsInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitErrorCapturingMultiUnitsInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitErrorCapturingMultiUnitsInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingMultiUnitsIntervalContext errorCapturingMultiUnitsInterval() throws RecognitionException {
		ErrorCapturingMultiUnitsIntervalContext _localctx = new ErrorCapturingMultiUnitsIntervalContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_errorCapturingMultiUnitsInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1623);
			((ErrorCapturingMultiUnitsIntervalContext)_localctx).body = multiUnitsInterval();
			setState(1625);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,219,_ctx) ) {
			case 1:
				{
				setState(1624);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterMultiUnitsInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitMultiUnitsInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitMultiUnitsInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiUnitsIntervalContext multiUnitsInterval() throws RecognitionException {
		MultiUnitsIntervalContext _localctx = new MultiUnitsIntervalContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_multiUnitsInterval);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1630); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1627);
					intervalValue();
					setState(1628);
					((MultiUnitsIntervalContext)_localctx).identifier = identifier();
					((MultiUnitsIntervalContext)_localctx).unit.add(((MultiUnitsIntervalContext)_localctx).identifier);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1632); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,220,_ctx);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterErrorCapturingUnitToUnitInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitErrorCapturingUnitToUnitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitErrorCapturingUnitToUnitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingUnitToUnitIntervalContext errorCapturingUnitToUnitInterval() throws RecognitionException {
		ErrorCapturingUnitToUnitIntervalContext _localctx = new ErrorCapturingUnitToUnitIntervalContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_errorCapturingUnitToUnitInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1634);
			((ErrorCapturingUnitToUnitIntervalContext)_localctx).body = unitToUnitInterval();
			setState(1637);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,221,_ctx) ) {
			case 1:
				{
				setState(1635);
				((ErrorCapturingUnitToUnitIntervalContext)_localctx).error1 = multiUnitsInterval();
				}
				break;
			case 2:
				{
				setState(1636);
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
		public TerminalNode TO() { return getToken(ArcticSqlExtendParser.TO, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterUnitToUnitInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitUnitToUnitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitUnitToUnitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitToUnitIntervalContext unitToUnitInterval() throws RecognitionException {
		UnitToUnitIntervalContext _localctx = new UnitToUnitIntervalContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_unitToUnitInterval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1639);
			((UnitToUnitIntervalContext)_localctx).value = intervalValue();
			setState(1640);
			((UnitToUnitIntervalContext)_localctx).from = identifier();
			setState(1641);
			match(TO);
			setState(1642);
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
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlExtendParser.DECIMAL_VALUE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSqlExtendParser.STRING, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSqlExtendParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public IntervalValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intervalValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIntervalValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIntervalValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIntervalValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalValueContext intervalValue() throws RecognitionException {
		IntervalValueContext _localctx = new IntervalValueContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_intervalValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1645);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PLUS || _la==MINUS) {
				{
				setState(1644);
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

			setState(1647);
			_la = _input.LA(1);
			if ( !(((((_la - 310)) & ~0x3f) == 0 && ((1L << (_la - 310)) & ((1L << (STRING - 310)) | (1L << (INTEGER_VALUE - 310)) | (1L << (DECIMAL_VALUE - 310)))) != 0)) ) {
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
		public TerminalNode LT() { return getToken(ArcticSqlExtendParser.LT, 0); }
		public List<DataTypeContext> dataType() {
			return getRuleContexts(DataTypeContext.class);
		}
		public DataTypeContext dataType(int i) {
			return getRuleContext(DataTypeContext.class,i);
		}
		public TerminalNode GT() { return getToken(ArcticSqlExtendParser.GT, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSqlExtendParser.ARRAY, 0); }
		public TerminalNode COMMA() { return getToken(ArcticSqlExtendParser.COMMA, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlExtendParser.MAP, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSqlExtendParser.STRUCT, 0); }
		public TerminalNode NEQ() { return getToken(ArcticSqlExtendParser.NEQ, 0); }
		public ComplexColTypeListContext complexColTypeList() {
			return getRuleContext(ComplexColTypeListContext.class,0);
		}
		public ComplexDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterComplexDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitComplexDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitComplexDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class YearMonthIntervalDataTypeContext extends DataTypeContext {
		public Token from;
		public Token to;
		public TerminalNode INTERVAL() { return getToken(ArcticSqlExtendParser.INTERVAL, 0); }
		public TerminalNode YEAR() { return getToken(ArcticSqlExtendParser.YEAR, 0); }
		public List<TerminalNode> MONTH() { return getTokens(ArcticSqlExtendParser.MONTH); }
		public TerminalNode MONTH(int i) {
			return getToken(ArcticSqlExtendParser.MONTH, i);
		}
		public TerminalNode TO() { return getToken(ArcticSqlExtendParser.TO, 0); }
		public YearMonthIntervalDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterYearMonthIntervalDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitYearMonthIntervalDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitYearMonthIntervalDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DayTimeIntervalDataTypeContext extends DataTypeContext {
		public Token from;
		public Token to;
		public TerminalNode INTERVAL() { return getToken(ArcticSqlExtendParser.INTERVAL, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlExtendParser.DAY, 0); }
		public List<TerminalNode> HOUR() { return getTokens(ArcticSqlExtendParser.HOUR); }
		public TerminalNode HOUR(int i) {
			return getToken(ArcticSqlExtendParser.HOUR, i);
		}
		public List<TerminalNode> MINUTE() { return getTokens(ArcticSqlExtendParser.MINUTE); }
		public TerminalNode MINUTE(int i) {
			return getToken(ArcticSqlExtendParser.MINUTE, i);
		}
		public List<TerminalNode> SECOND() { return getTokens(ArcticSqlExtendParser.SECOND); }
		public TerminalNode SECOND(int i) {
			return getToken(ArcticSqlExtendParser.SECOND, i);
		}
		public TerminalNode TO() { return getToken(ArcticSqlExtendParser.TO, 0); }
		public DayTimeIntervalDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterDayTimeIntervalDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitDayTimeIntervalDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitDayTimeIntervalDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrimitiveDataTypeContext extends DataTypeContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticSqlExtendParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticSqlExtendParser.INTEGER_VALUE, i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public PrimitiveDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterPrimitiveDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitPrimitiveDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitPrimitiveDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_dataType);
		int _la;
		try {
			setState(1695);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,229,_ctx) ) {
			case 1:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1649);
				((ComplexDataTypeContext)_localctx).complex = match(ARRAY);
				setState(1650);
				match(LT);
				setState(1651);
				dataType();
				setState(1652);
				match(GT);
				}
				break;
			case 2:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1654);
				((ComplexDataTypeContext)_localctx).complex = match(MAP);
				setState(1655);
				match(LT);
				setState(1656);
				dataType();
				setState(1657);
				match(COMMA);
				setState(1658);
				dataType();
				setState(1659);
				match(GT);
				}
				break;
			case 3:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1661);
				((ComplexDataTypeContext)_localctx).complex = match(STRUCT);
				setState(1668);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LT:
					{
					setState(1662);
					match(LT);
					setState(1664);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,223,_ctx) ) {
					case 1:
						{
						setState(1663);
						complexColTypeList();
						}
						break;
					}
					setState(1666);
					match(GT);
					}
					break;
				case NEQ:
					{
					setState(1667);
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
				setState(1670);
				match(INTERVAL);
				setState(1671);
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
				setState(1674);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,225,_ctx) ) {
				case 1:
					{
					setState(1672);
					match(TO);
					setState(1673);
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
				setState(1676);
				match(INTERVAL);
				setState(1677);
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
				setState(1680);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,226,_ctx) ) {
				case 1:
					{
					setState(1678);
					match(TO);
					setState(1679);
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
				setState(1682);
				identifier();
				setState(1693);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,228,_ctx) ) {
				case 1:
					{
					setState(1683);
					match(LEFT_PAREN);
					setState(1684);
					match(INTEGER_VALUE);
					setState(1689);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1685);
						match(COMMA);
						setState(1686);
						match(INTEGER_VALUE);
						}
						}
						setState(1691);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(1692);
					match(RIGHT_PAREN);
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

	public static class ColTypeListContext extends ParserRuleContext {
		public List<ColTypeContext> colType() {
			return getRuleContexts(ColTypeContext.class);
		}
		public ColTypeContext colType(int i) {
			return getRuleContext(ColTypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public ColTypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colTypeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeListContext colTypeList() throws RecognitionException {
		ColTypeListContext _localctx = new ColTypeListContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_colTypeList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1697);
			colType();
			setState(1702);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,230,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1698);
					match(COMMA);
					setState(1699);
					colType();
					}
					} 
				}
				setState(1704);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,230,_ctx);
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
		public TerminalNode NOT() { return getToken(ArcticSqlExtendParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlExtendParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeContext colType() throws RecognitionException {
		ColTypeContext _localctx = new ColTypeContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_colType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1705);
			((ColTypeContext)_localctx).colName = errorCapturingIdentifier();
			setState(1706);
			dataType();
			setState(1709);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,231,_ctx) ) {
			case 1:
				{
				setState(1707);
				match(NOT);
				setState(1708);
				match(NULL);
				}
				break;
			}
			setState(1712);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,232,_ctx) ) {
			case 1:
				{
				setState(1711);
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
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public ComplexColTypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexColTypeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterComplexColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitComplexColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitComplexColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexColTypeListContext complexColTypeList() throws RecognitionException {
		ComplexColTypeListContext _localctx = new ComplexColTypeListContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_complexColTypeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1714);
			complexColType();
			setState(1719);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1715);
				match(COMMA);
				setState(1716);
				complexColType();
				}
				}
				setState(1721);
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
		public TerminalNode COLON() { return getToken(ArcticSqlExtendParser.COLON, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlExtendParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlExtendParser.NULL, 0); }
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public ComplexColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexColType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterComplexColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitComplexColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitComplexColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexColTypeContext complexColType() throws RecognitionException {
		ComplexColTypeContext _localctx = new ComplexColTypeContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_complexColType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1722);
			identifier();
			setState(1724);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,234,_ctx) ) {
			case 1:
				{
				setState(1723);
				match(COLON);
				}
				break;
			}
			setState(1726);
			dataType();
			setState(1729);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(1727);
				match(NOT);
				setState(1728);
				match(NULL);
				}
			}

			setState(1732);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(1731);
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
		public TerminalNode WHEN() { return getToken(ArcticSqlExtendParser.WHEN, 0); }
		public TerminalNode THEN() { return getToken(ArcticSqlExtendParser.THEN, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterWhenClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitWhenClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitWhenClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhenClauseContext whenClause() throws RecognitionException {
		WhenClauseContext _localctx = new WhenClauseContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_whenClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1734);
			match(WHEN);
			setState(1735);
			((WhenClauseContext)_localctx).condition = expression();
			setState(1736);
			match(THEN);
			setState(1737);
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
		public TerminalNode WINDOW() { return getToken(ArcticSqlExtendParser.WINDOW, 0); }
		public List<NamedWindowContext> namedWindow() {
			return getRuleContexts(NamedWindowContext.class);
		}
		public NamedWindowContext namedWindow(int i) {
			return getRuleContext(NamedWindowContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public WindowClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterWindowClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitWindowClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitWindowClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowClauseContext windowClause() throws RecognitionException {
		WindowClauseContext _localctx = new WindowClauseContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_windowClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1739);
			match(WINDOW);
			setState(1740);
			namedWindow();
			setState(1745);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,237,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1741);
					match(COMMA);
					setState(1742);
					namedWindow();
					}
					} 
				}
				setState(1747);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,237,_ctx);
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
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNamedWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNamedWindow(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNamedWindow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedWindowContext namedWindow() throws RecognitionException {
		NamedWindowContext _localctx = new NamedWindowContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_namedWindow);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1748);
			((NamedWindowContext)_localctx).name = errorCapturingIdentifier();
			setState(1749);
			match(AS);
			setState(1750);
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
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public WindowRefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterWindowRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitWindowRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitWindowRef(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WindowDefContext extends WindowSpecContext {
		public ExpressionContext expression;
		public List<ExpressionContext> partition = new ArrayList<ExpressionContext>();
		public TerminalNode LEFT_PAREN() { return getToken(ArcticSqlExtendParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(ArcticSqlExtendParser.RIGHT_PAREN, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticSqlExtendParser.CLUSTER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSqlExtendParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSqlExtendParser.BY, i);
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
		public List<TerminalNode> COMMA() { return getTokens(ArcticSqlExtendParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ArcticSqlExtendParser.COMMA, i);
		}
		public List<SortItemContext> sortItem() {
			return getRuleContexts(SortItemContext.class);
		}
		public SortItemContext sortItem(int i) {
			return getRuleContext(SortItemContext.class,i);
		}
		public TerminalNode PARTITION() { return getToken(ArcticSqlExtendParser.PARTITION, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlExtendParser.DISTRIBUTE, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSqlExtendParser.ORDER, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlExtendParser.SORT, 0); }
		public WindowDefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterWindowDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitWindowDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitWindowDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowSpecContext windowSpec() throws RecognitionException {
		WindowSpecContext _localctx = new WindowSpecContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_windowSpec);
		int _la;
		try {
			setState(1798);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,245,_ctx) ) {
			case 1:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1752);
				((WindowRefContext)_localctx).name = errorCapturingIdentifier();
				}
				break;
			case 2:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1753);
				match(LEFT_PAREN);
				setState(1754);
				((WindowRefContext)_localctx).name = errorCapturingIdentifier();
				setState(1755);
				match(RIGHT_PAREN);
				}
				break;
			case 3:
				_localctx = new WindowDefContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1757);
				match(LEFT_PAREN);
				setState(1792);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case CLUSTER:
					{
					setState(1758);
					match(CLUSTER);
					setState(1759);
					match(BY);
					setState(1760);
					((WindowDefContext)_localctx).expression = expression();
					((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
					setState(1765);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(1761);
						match(COMMA);
						setState(1762);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						}
						}
						setState(1767);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				case RIGHT_PAREN:
				case DISTRIBUTE:
				case ORDER:
				case PARTITION:
				case RANGE:
				case ROWS:
				case SORT:
					{
					setState(1778);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==DISTRIBUTE || _la==PARTITION) {
						{
						setState(1768);
						_la = _input.LA(1);
						if ( !(_la==DISTRIBUTE || _la==PARTITION) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1769);
						match(BY);
						setState(1770);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						setState(1775);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==COMMA) {
							{
							{
							setState(1771);
							match(COMMA);
							setState(1772);
							((WindowDefContext)_localctx).expression = expression();
							((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
							}
							}
							setState(1777);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(1790);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ORDER || _la==SORT) {
						{
						setState(1780);
						_la = _input.LA(1);
						if ( !(_la==ORDER || _la==SORT) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1781);
						match(BY);
						setState(1782);
						sortItem();
						setState(1787);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==COMMA) {
							{
							{
							setState(1783);
							match(COMMA);
							setState(1784);
							sortItem();
							}
							}
							setState(1789);
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
				setState(1795);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==RANGE || _la==ROWS) {
					{
					setState(1794);
					windowFrame();
					}
				}

				setState(1797);
				match(RIGHT_PAREN);
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
		public TerminalNode RANGE() { return getToken(ArcticSqlExtendParser.RANGE, 0); }
		public List<FrameBoundContext> frameBound() {
			return getRuleContexts(FrameBoundContext.class);
		}
		public FrameBoundContext frameBound(int i) {
			return getRuleContext(FrameBoundContext.class,i);
		}
		public TerminalNode ROWS() { return getToken(ArcticSqlExtendParser.ROWS, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlExtendParser.BETWEEN, 0); }
		public TerminalNode AND() { return getToken(ArcticSqlExtendParser.AND, 0); }
		public WindowFrameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowFrame; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterWindowFrame(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitWindowFrame(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitWindowFrame(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowFrameContext windowFrame() throws RecognitionException {
		WindowFrameContext _localctx = new WindowFrameContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_windowFrame);
		try {
			setState(1816);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,246,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1800);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(1801);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1802);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(1803);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1804);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(1805);
				match(BETWEEN);
				setState(1806);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(1807);
				match(AND);
				setState(1808);
				((WindowFrameContext)_localctx).end = frameBound();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1810);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(1811);
				match(BETWEEN);
				setState(1812);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(1813);
				match(AND);
				setState(1814);
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
		public TerminalNode UNBOUNDED() { return getToken(ArcticSqlExtendParser.UNBOUNDED, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSqlExtendParser.PRECEDING, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSqlExtendParser.FOLLOWING, 0); }
		public TerminalNode ROW() { return getToken(ArcticSqlExtendParser.ROW, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlExtendParser.CURRENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FrameBoundContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_frameBound; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFrameBound(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFrameBound(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFrameBound(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FrameBoundContext frameBound() throws RecognitionException {
		FrameBoundContext _localctx = new FrameBoundContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_frameBound);
		int _la;
		try {
			setState(1825);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,247,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1818);
				match(UNBOUNDED);
				setState(1819);
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
				setState(1820);
				((FrameBoundContext)_localctx).boundType = match(CURRENT);
				setState(1821);
				match(ROW);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1822);
				expression();
				setState(1823);
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

	public static class FunctionNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode FILTER() { return getToken(ArcticSqlExtendParser.FILTER, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSqlExtendParser.LEFT, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSqlExtendParser.RIGHT, 0); }
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFunctionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFunctionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_functionName);
		try {
			setState(1831);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,248,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1827);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1828);
				match(FILTER);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1829);
				match(LEFT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1830);
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
		public List<TerminalNode> DOT() { return getTokens(ArcticSqlExtendParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(ArcticSqlExtendParser.DOT, i);
		}
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1833);
			identifier();
			setState(1838);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,249,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1834);
					match(DOT);
					setState(1835);
					identifier();
					}
					} 
				}
				setState(1840);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,249,_ctx);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterErrorCapturingIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitErrorCapturingIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitErrorCapturingIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingIdentifierContext errorCapturingIdentifier() throws RecognitionException {
		ErrorCapturingIdentifierContext _localctx = new ErrorCapturingIdentifierContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_errorCapturingIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1841);
			identifier();
			setState(1842);
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
		public List<TerminalNode> MINUS() { return getTokens(ArcticSqlExtendParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(ArcticSqlExtendParser.MINUS, i);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterErrorIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitErrorIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitErrorIdent(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RealIdentContext extends ErrorCapturingIdentifierExtraContext {
		public RealIdentContext(ErrorCapturingIdentifierExtraContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterRealIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitRealIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitRealIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ErrorCapturingIdentifierExtraContext errorCapturingIdentifierExtra() throws RecognitionException {
		ErrorCapturingIdentifierExtraContext _localctx = new ErrorCapturingIdentifierExtraContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_errorCapturingIdentifierExtra);
		try {
			int _alt;
			setState(1851);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,251,_ctx) ) {
			case 1:
				_localctx = new ErrorIdentContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1846); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(1844);
						match(MINUS);
						setState(1845);
						identifier();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(1848); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,250,_ctx);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_identifier);
		try {
			setState(1856);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,252,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1853);
				strictIdentifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1854);
				if (!(!SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "!SQL_standard_keyword_behavior");
				setState(1855);
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
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQuotedIdentifierAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQuotedIdentifierAlternative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQuotedIdentifierAlternative(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnquotedIdentifierContext extends StrictIdentifierContext {
		public TerminalNode IDENTIFIER() { return getToken(ArcticSqlExtendParser.IDENTIFIER, 0); }
		public AnsiNonReservedContext ansiNonReserved() {
			return getRuleContext(AnsiNonReservedContext.class,0);
		}
		public NonReservedContext nonReserved() {
			return getRuleContext(NonReservedContext.class,0);
		}
		public UnquotedIdentifierContext(StrictIdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterUnquotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitUnquotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitUnquotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictIdentifierContext strictIdentifier() throws RecognitionException {
		StrictIdentifierContext _localctx = new StrictIdentifierContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_strictIdentifier);
		try {
			setState(1864);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,253,_ctx) ) {
			case 1:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1858);
				match(IDENTIFIER);
				}
				break;
			case 2:
				_localctx = new QuotedIdentifierAlternativeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1859);
				quotedIdentifier();
				}
				break;
			case 3:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1860);
				if (!(SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "SQL_standard_keyword_behavior");
				setState(1861);
				ansiNonReserved();
				}
				break;
			case 4:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1862);
				if (!(!SQL_standard_keyword_behavior)) throw new FailedPredicateException(this, "!SQL_standard_keyword_behavior");
				setState(1863);
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
		public TerminalNode BACKQUOTED_IDENTIFIER() { return getToken(ArcticSqlExtendParser.BACKQUOTED_IDENTIFIER, 0); }
		public QuotedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quotedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterQuotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitQuotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitQuotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuotedIdentifierContext quotedIdentifier() throws RecognitionException {
		QuotedIdentifierContext _localctx = new QuotedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_quotedIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1866);
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
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlExtendParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public DecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigIntLiteralContext extends NumberContext {
		public TerminalNode BIGINT_LITERAL() { return getToken(ArcticSqlExtendParser.BIGINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public BigIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterBigIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitBigIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitBigIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TinyIntLiteralContext extends NumberContext {
		public TerminalNode TINYINT_LITERAL() { return getToken(ArcticSqlExtendParser.TINYINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public TinyIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterTinyIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitTinyIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitTinyIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LegacyDecimalLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(ArcticSqlExtendParser.EXPONENT_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSqlExtendParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public LegacyDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterLegacyDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitLegacyDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitLegacyDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigDecimalLiteralContext extends NumberContext {
		public TerminalNode BIGDECIMAL_LITERAL() { return getToken(ArcticSqlExtendParser.BIGDECIMAL_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public BigDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterBigDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitBigDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitBigDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExponentLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(ArcticSqlExtendParser.EXPONENT_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public ExponentLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterExponentLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitExponentLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitExponentLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DoubleLiteralContext extends NumberContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(ArcticSqlExtendParser.DOUBLE_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public DoubleLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitDoubleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitDoubleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntegerLiteralContext extends NumberContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSqlExtendParser.INTEGER_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public IntegerLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitIntegerLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FloatLiteralContext extends NumberContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(ArcticSqlExtendParser.FLOAT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public FloatLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterFloatLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitFloatLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitFloatLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SmallIntLiteralContext extends NumberContext {
		public TerminalNode SMALLINT_LITERAL() { return getToken(ArcticSqlExtendParser.SMALLINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSqlExtendParser.MINUS, 0); }
		public SmallIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterSmallIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitSmallIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitSmallIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_number);
		int _la;
		try {
			setState(1911);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,264,_ctx) ) {
			case 1:
				_localctx = new ExponentLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1868);
				if (!(!legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "!legacy_exponent_literal_as_decimal_enabled");
				setState(1870);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1869);
					match(MINUS);
					}
				}

				setState(1872);
				match(EXPONENT_VALUE);
				}
				break;
			case 2:
				_localctx = new DecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1873);
				if (!(!legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "!legacy_exponent_literal_as_decimal_enabled");
				setState(1875);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1874);
					match(MINUS);
					}
				}

				setState(1877);
				match(DECIMAL_VALUE);
				}
				break;
			case 3:
				_localctx = new LegacyDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1878);
				if (!(legacy_exponent_literal_as_decimal_enabled)) throw new FailedPredicateException(this, "legacy_exponent_literal_as_decimal_enabled");
				setState(1880);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1879);
					match(MINUS);
					}
				}

				setState(1882);
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
				setState(1884);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1883);
					match(MINUS);
					}
				}

				setState(1886);
				match(INTEGER_VALUE);
				}
				break;
			case 5:
				_localctx = new BigIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1888);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1887);
					match(MINUS);
					}
				}

				setState(1890);
				match(BIGINT_LITERAL);
				}
				break;
			case 6:
				_localctx = new SmallIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1892);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1891);
					match(MINUS);
					}
				}

				setState(1894);
				match(SMALLINT_LITERAL);
				}
				break;
			case 7:
				_localctx = new TinyIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(1896);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1895);
					match(MINUS);
					}
				}

				setState(1898);
				match(TINYINT_LITERAL);
				}
				break;
			case 8:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(1900);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1899);
					match(MINUS);
					}
				}

				setState(1902);
				match(DOUBLE_LITERAL);
				}
				break;
			case 9:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(1904);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1903);
					match(MINUS);
					}
				}

				setState(1906);
				match(FLOAT_LITERAL);
				}
				break;
			case 10:
				_localctx = new BigDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(1908);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1907);
					match(MINUS);
					}
				}

				setState(1910);
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

	public static class AnsiNonReservedContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(ArcticSqlExtendParser.ADD, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSqlExtendParser.AFTER, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSqlExtendParser.ALTER, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticSqlExtendParser.ANALYZE, 0); }
		public TerminalNode ANTI() { return getToken(ArcticSqlExtendParser.ANTI, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSqlExtendParser.ARCHIVE, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSqlExtendParser.ARRAY, 0); }
		public TerminalNode ASC() { return getToken(ArcticSqlExtendParser.ASC, 0); }
		public TerminalNode AT() { return getToken(ArcticSqlExtendParser.AT, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlExtendParser.BETWEEN, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSqlExtendParser.BUCKET, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSqlExtendParser.BUCKETS, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlExtendParser.BY, 0); }
		public TerminalNode CACHE() { return getToken(ArcticSqlExtendParser.CACHE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticSqlExtendParser.CASCADE, 0); }
		public TerminalNode CATALOG() { return getToken(ArcticSqlExtendParser.CATALOG, 0); }
		public TerminalNode CATALOGS() { return getToken(ArcticSqlExtendParser.CATALOGS, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticSqlExtendParser.CHANGE, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticSqlExtendParser.CLEAR, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticSqlExtendParser.CLUSTER, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlExtendParser.CLUSTERED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSqlExtendParser.CODEGEN, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticSqlExtendParser.COLLECTION, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlExtendParser.COLUMNS, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticSqlExtendParser.COMMENT, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSqlExtendParser.COMMIT, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSqlExtendParser.COMPACT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSqlExtendParser.COMPACTIONS, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticSqlExtendParser.COMPUTE, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticSqlExtendParser.CONCATENATE, 0); }
		public TerminalNode COST() { return getToken(ArcticSqlExtendParser.COST, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlExtendParser.CUBE, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlExtendParser.CURRENT, 0); }
		public TerminalNode DATA() { return getToken(ArcticSqlExtendParser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSqlExtendParser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticSqlExtendParser.DATABASES, 0); }
		public TerminalNode DATEADD() { return getToken(ArcticSqlExtendParser.DATEADD, 0); }
		public TerminalNode DATEDIFF() { return getToken(ArcticSqlExtendParser.DATEDIFF, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlExtendParser.DAY, 0); }
		public TerminalNode DAYOFYEAR() { return getToken(ArcticSqlExtendParser.DAYOFYEAR, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticSqlExtendParser.DBPROPERTIES, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSqlExtendParser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(ArcticSqlExtendParser.DELETE, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSqlExtendParser.DELIMITED, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlExtendParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlExtendParser.DESCRIBE, 0); }
		public TerminalNode DFS() { return getToken(ArcticSqlExtendParser.DFS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlExtendParser.DIRECTORIES, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSqlExtendParser.DIRECTORY, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlExtendParser.DISTRIBUTE, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlExtendParser.DIV, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlExtendParser.DROP, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticSqlExtendParser.ESCAPED, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSqlExtendParser.EXCHANGE, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlExtendParser.EXISTS, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticSqlExtendParser.EXPLAIN, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSqlExtendParser.EXPORT, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlExtendParser.EXTENDED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSqlExtendParser.EXTERNAL, 0); }
		public TerminalNode EXTRACT() { return getToken(ArcticSqlExtendParser.EXTRACT, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSqlExtendParser.FIELDS, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSqlExtendParser.FILEFORMAT, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSqlExtendParser.FIRST, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSqlExtendParser.FOLLOWING, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlExtendParser.FORMAT, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlExtendParser.FORMATTED, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlExtendParser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticSqlExtendParser.FUNCTIONS, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticSqlExtendParser.GLOBAL, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlExtendParser.GROUPING, 0); }
		public TerminalNode HOUR() { return getToken(ArcticSqlExtendParser.HOUR, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlExtendParser.IF, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSqlExtendParser.IGNORE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSqlExtendParser.IMPORT, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSqlExtendParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSqlExtendParser.INDEXES, 0); }
		public TerminalNode INPATH() { return getToken(ArcticSqlExtendParser.INPATH, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSqlExtendParser.INPUTFORMAT, 0); }
		public TerminalNode INSERT() { return getToken(ArcticSqlExtendParser.INSERT, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticSqlExtendParser.INTERVAL, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSqlExtendParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSqlExtendParser.KEYS, 0); }
		public TerminalNode LAST() { return getToken(ArcticSqlExtendParser.LAST, 0); }
		public TerminalNode LAZY() { return getToken(ArcticSqlExtendParser.LAZY, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlExtendParser.LIKE, 0); }
		public TerminalNode ILIKE() { return getToken(ArcticSqlExtendParser.ILIKE, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticSqlExtendParser.LIMIT, 0); }
		public TerminalNode LINES() { return getToken(ArcticSqlExtendParser.LINES, 0); }
		public TerminalNode LIST() { return getToken(ArcticSqlExtendParser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(ArcticSqlExtendParser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlExtendParser.LOCAL, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSqlExtendParser.LOCATION, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSqlExtendParser.LOCK, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSqlExtendParser.LOCKS, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticSqlExtendParser.LOGICAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSqlExtendParser.MACRO, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlExtendParser.MAP, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticSqlExtendParser.MATCHED, 0); }
		public TerminalNode MERGE() { return getToken(ArcticSqlExtendParser.MERGE, 0); }
		public TerminalNode MICROSECOND() { return getToken(ArcticSqlExtendParser.MICROSECOND, 0); }
		public TerminalNode MILLISECOND() { return getToken(ArcticSqlExtendParser.MILLISECOND, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticSqlExtendParser.MINUTE, 0); }
		public TerminalNode MONTH() { return getToken(ArcticSqlExtendParser.MONTH, 0); }
		public TerminalNode MSCK() { return getToken(ArcticSqlExtendParser.MSCK, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlExtendParser.NAMESPACE, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticSqlExtendParser.NAMESPACES, 0); }
		public TerminalNode NO() { return getToken(ArcticSqlExtendParser.NO, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlExtendParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlExtendParser.OF, 0); }
		public TerminalNode OPTION() { return getToken(ArcticSqlExtendParser.OPTION, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSqlExtendParser.OPTIONS, 0); }
		public TerminalNode OUT() { return getToken(ArcticSqlExtendParser.OUT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSqlExtendParser.OUTPUTFORMAT, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlExtendParser.OVER, 0); }
		public TerminalNode OVERLAY() { return getToken(ArcticSqlExtendParser.OVERLAY, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlExtendParser.OVERWRITE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSqlExtendParser.PARTITION, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticSqlExtendParser.PARTITIONED, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlExtendParser.PARTITIONS, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticSqlExtendParser.PERCENTLIT, 0); }
		public TerminalNode PIVOT() { return getToken(ArcticSqlExtendParser.PIVOT, 0); }
		public TerminalNode PLACING() { return getToken(ArcticSqlExtendParser.PLACING, 0); }
		public TerminalNode POSITION() { return getToken(ArcticSqlExtendParser.POSITION, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSqlExtendParser.PRECEDING, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSqlExtendParser.PRINCIPALS, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticSqlExtendParser.PROPERTIES, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSqlExtendParser.PURGE, 0); }
		public TerminalNode QUARTER() { return getToken(ArcticSqlExtendParser.QUARTER, 0); }
		public TerminalNode QUERY() { return getToken(ArcticSqlExtendParser.QUERY, 0); }
		public TerminalNode RANGE() { return getToken(ArcticSqlExtendParser.RANGE, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSqlExtendParser.RECORDREADER, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSqlExtendParser.RECORDWRITER, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticSqlExtendParser.RECOVER, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSqlExtendParser.REDUCE, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticSqlExtendParser.REFRESH, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSqlExtendParser.RENAME, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticSqlExtendParser.REPAIR, 0); }
		public TerminalNode REPEATABLE() { return getToken(ArcticSqlExtendParser.REPEATABLE, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlExtendParser.REPLACE, 0); }
		public TerminalNode RESET() { return getToken(ArcticSqlExtendParser.RESET, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticSqlExtendParser.RESPECT, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticSqlExtendParser.RESTRICT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSqlExtendParser.REVOKE, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticSqlExtendParser.RLIKE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSqlExtendParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSqlExtendParser.ROLES, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSqlExtendParser.ROLLBACK, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSqlExtendParser.ROLLUP, 0); }
		public TerminalNode ROW() { return getToken(ArcticSqlExtendParser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(ArcticSqlExtendParser.ROWS, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticSqlExtendParser.SCHEMA, 0); }
		public TerminalNode SCHEMAS() { return getToken(ArcticSqlExtendParser.SCHEMAS, 0); }
		public TerminalNode SECOND() { return getToken(ArcticSqlExtendParser.SECOND, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSqlExtendParser.SEMI, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticSqlExtendParser.SEPARATED, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlExtendParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlExtendParser.SERDEPROPERTIES, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlExtendParser.SET, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSqlExtendParser.SETMINUS, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlExtendParser.SETS, 0); }
		public TerminalNode SHOW() { return getToken(ArcticSqlExtendParser.SHOW, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSqlExtendParser.SKEWED, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlExtendParser.SORT, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlExtendParser.SORTED, 0); }
		public TerminalNode START() { return getToken(ArcticSqlExtendParser.START, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSqlExtendParser.STATISTICS, 0); }
		public TerminalNode STORED() { return getToken(ArcticSqlExtendParser.STORED, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticSqlExtendParser.STRATIFY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSqlExtendParser.STRUCT, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticSqlExtendParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticSqlExtendParser.SUBSTRING, 0); }
		public TerminalNode SYNC() { return getToken(ArcticSqlExtendParser.SYNC, 0); }
		public TerminalNode SYSTEM_TIME() { return getToken(ArcticSqlExtendParser.SYSTEM_TIME, 0); }
		public TerminalNode SYSTEM_VERSION() { return getToken(ArcticSqlExtendParser.SYSTEM_VERSION, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSqlExtendParser.TABLES, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSqlExtendParser.TABLESAMPLE, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlExtendParser.TBLPROPERTIES, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlExtendParser.TEMPORARY, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticSqlExtendParser.TERMINATED, 0); }
		public TerminalNode TIMESTAMP() { return getToken(ArcticSqlExtendParser.TIMESTAMP, 0); }
		public TerminalNode TIMESTAMPADD() { return getToken(ArcticSqlExtendParser.TIMESTAMPADD, 0); }
		public TerminalNode TIMESTAMPDIFF() { return getToken(ArcticSqlExtendParser.TIMESTAMPDIFF, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSqlExtendParser.TOUCH, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSqlExtendParser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSqlExtendParser.TRANSACTIONS, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticSqlExtendParser.TRANSFORM, 0); }
		public TerminalNode TRIM() { return getToken(ArcticSqlExtendParser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSqlExtendParser.TRUE, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticSqlExtendParser.TRUNCATE, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticSqlExtendParser.TRY_CAST, 0); }
		public TerminalNode TYPE() { return getToken(ArcticSqlExtendParser.TYPE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSqlExtendParser.UNARCHIVE, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticSqlExtendParser.UNBOUNDED, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticSqlExtendParser.UNCACHE, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSqlExtendParser.UNLOCK, 0); }
		public TerminalNode UNSET() { return getToken(ArcticSqlExtendParser.UNSET, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticSqlExtendParser.UPDATE, 0); }
		public TerminalNode USE() { return getToken(ArcticSqlExtendParser.USE, 0); }
		public TerminalNode VALUES() { return getToken(ArcticSqlExtendParser.VALUES, 0); }
		public TerminalNode VERSION() { return getToken(ArcticSqlExtendParser.VERSION, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlExtendParser.VIEW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticSqlExtendParser.VIEWS, 0); }
		public TerminalNode WEEK() { return getToken(ArcticSqlExtendParser.WEEK, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticSqlExtendParser.WINDOW, 0); }
		public TerminalNode YEAR() { return getToken(ArcticSqlExtendParser.YEAR, 0); }
		public TerminalNode ZONE() { return getToken(ArcticSqlExtendParser.ZONE, 0); }
		public AnsiNonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ansiNonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterAnsiNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitAnsiNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitAnsiNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnsiNonReservedContext ansiNonReserved() throws RecognitionException {
		AnsiNonReservedContext _localctx = new AnsiNonReservedContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_ansiNonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1913);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ADD) | (1L << AFTER) | (1L << ALTER) | (1L << ANALYZE) | (1L << ANTI) | (1L << ARCHIVE) | (1L << ARRAY) | (1L << ASC) | (1L << AT) | (1L << BETWEEN) | (1L << BUCKET) | (1L << BUCKETS) | (1L << BY) | (1L << CACHE) | (1L << CASCADE) | (1L << CATALOG) | (1L << CATALOGS) | (1L << CHANGE) | (1L << CLEAR) | (1L << CLUSTER) | (1L << CLUSTERED) | (1L << CODEGEN) | (1L << COLLECTION) | (1L << COLUMNS) | (1L << COMMENT) | (1L << COMMIT) | (1L << COMPACT) | (1L << COMPACTIONS) | (1L << COMPUTE) | (1L << CONCATENATE) | (1L << COST) | (1L << CUBE) | (1L << CURRENT) | (1L << DAY) | (1L << DAYOFYEAR) | (1L << DATA) | (1L << DATABASE) | (1L << DATABASES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (DATEADD - 64)) | (1L << (DATEDIFF - 64)) | (1L << (DBPROPERTIES - 64)) | (1L << (DEFINED - 64)) | (1L << (DELETE - 64)) | (1L << (DELIMITED - 64)) | (1L << (DESC - 64)) | (1L << (DESCRIBE - 64)) | (1L << (DFS - 64)) | (1L << (DIRECTORIES - 64)) | (1L << (DIRECTORY - 64)) | (1L << (DISTRIBUTE - 64)) | (1L << (DIV - 64)) | (1L << (DROP - 64)) | (1L << (ESCAPED - 64)) | (1L << (EXCHANGE - 64)) | (1L << (EXISTS - 64)) | (1L << (EXPLAIN - 64)) | (1L << (EXPORT - 64)) | (1L << (EXTENDED - 64)) | (1L << (EXTERNAL - 64)) | (1L << (EXTRACT - 64)) | (1L << (FIELDS - 64)) | (1L << (FILEFORMAT - 64)) | (1L << (FIRST - 64)) | (1L << (FOLLOWING - 64)) | (1L << (FORMAT - 64)) | (1L << (FORMATTED - 64)) | (1L << (FUNCTION - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (GLOBAL - 64)) | (1L << (GROUPING - 64)) | (1L << (HOUR - 64)) | (1L << (IF - 64)) | (1L << (IGNORE - 64)) | (1L << (IMPORT - 64)) | (1L << (INDEX - 64)) | (1L << (INDEXES - 64)) | (1L << (INPATH - 64)) | (1L << (INPUTFORMAT - 64)) | (1L << (INSERT - 64)) | (1L << (INTERVAL - 64)) | (1L << (ITEMS - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (KEYS - 128)) | (1L << (LAST - 128)) | (1L << (LAZY - 128)) | (1L << (LIKE - 128)) | (1L << (ILIKE - 128)) | (1L << (LIMIT - 128)) | (1L << (LINES - 128)) | (1L << (LIST - 128)) | (1L << (LOAD - 128)) | (1L << (LOCAL - 128)) | (1L << (LOCATION - 128)) | (1L << (LOCK - 128)) | (1L << (LOCKS - 128)) | (1L << (LOGICAL - 128)) | (1L << (MACRO - 128)) | (1L << (MAP - 128)) | (1L << (MATCHED - 128)) | (1L << (MERGE - 128)) | (1L << (MICROSECOND - 128)) | (1L << (MILLISECOND - 128)) | (1L << (MINUTE - 128)) | (1L << (MONTH - 128)) | (1L << (MSCK - 128)) | (1L << (NAMESPACE - 128)) | (1L << (NAMESPACES - 128)) | (1L << (NO - 128)) | (1L << (NULLS - 128)) | (1L << (OF - 128)) | (1L << (OPTION - 128)) | (1L << (OPTIONS - 128)) | (1L << (OUT - 128)) | (1L << (OUTPUTFORMAT - 128)) | (1L << (OVER - 128)) | (1L << (OVERLAY - 128)) | (1L << (OVERWRITE - 128)) | (1L << (PARTITION - 128)) | (1L << (PARTITIONED - 128)) | (1L << (PARTITIONS - 128)) | (1L << (PERCENTLIT - 128)) | (1L << (PIVOT - 128)) | (1L << (PLACING - 128)) | (1L << (POSITION - 128)) | (1L << (PRECEDING - 128)) | (1L << (PRINCIPALS - 128)) | (1L << (PROPERTIES - 128)) | (1L << (PURGE - 128)) | (1L << (QUARTER - 128)) | (1L << (QUERY - 128)) | (1L << (RANGE - 128)))) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & ((1L << (RECORDREADER - 192)) | (1L << (RECORDWRITER - 192)) | (1L << (RECOVER - 192)) | (1L << (REDUCE - 192)) | (1L << (REFRESH - 192)) | (1L << (RENAME - 192)) | (1L << (REPAIR - 192)) | (1L << (REPEATABLE - 192)) | (1L << (REPLACE - 192)) | (1L << (RESET - 192)) | (1L << (RESPECT - 192)) | (1L << (RESTRICT - 192)) | (1L << (REVOKE - 192)) | (1L << (RLIKE - 192)) | (1L << (ROLE - 192)) | (1L << (ROLES - 192)) | (1L << (ROLLBACK - 192)) | (1L << (ROLLUP - 192)) | (1L << (ROW - 192)) | (1L << (ROWS - 192)) | (1L << (SECOND - 192)) | (1L << (SCHEMA - 192)) | (1L << (SCHEMAS - 192)) | (1L << (SEMI - 192)) | (1L << (SEPARATED - 192)) | (1L << (SERDE - 192)) | (1L << (SERDEPROPERTIES - 192)) | (1L << (SET - 192)) | (1L << (SETMINUS - 192)) | (1L << (SETS - 192)) | (1L << (SHOW - 192)) | (1L << (SKEWED - 192)) | (1L << (SORT - 192)) | (1L << (SORTED - 192)) | (1L << (START - 192)) | (1L << (STATISTICS - 192)) | (1L << (STORED - 192)) | (1L << (STRATIFY - 192)) | (1L << (STRUCT - 192)) | (1L << (SUBSTR - 192)) | (1L << (SUBSTRING - 192)) | (1L << (SYNC - 192)) | (1L << (SYSTEM_TIME - 192)) | (1L << (SYSTEM_VERSION - 192)) | (1L << (TABLES - 192)) | (1L << (TABLESAMPLE - 192)) | (1L << (TBLPROPERTIES - 192)) | (1L << (TEMPORARY - 192)) | (1L << (TERMINATED - 192)) | (1L << (TIMESTAMP - 192)) | (1L << (TIMESTAMPADD - 192)) | (1L << (TIMESTAMPDIFF - 192)) | (1L << (TOUCH - 192)) | (1L << (TRANSACTION - 192)))) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & ((1L << (TRANSACTIONS - 256)) | (1L << (TRANSFORM - 256)) | (1L << (TRIM - 256)) | (1L << (TRUE - 256)) | (1L << (TRUNCATE - 256)) | (1L << (TRY_CAST - 256)) | (1L << (TYPE - 256)) | (1L << (UNARCHIVE - 256)) | (1L << (UNBOUNDED - 256)) | (1L << (UNCACHE - 256)) | (1L << (UNLOCK - 256)) | (1L << (UNSET - 256)) | (1L << (UPDATE - 256)) | (1L << (USE - 256)) | (1L << (VALUES - 256)) | (1L << (VERSION - 256)) | (1L << (VIEW - 256)) | (1L << (VIEWS - 256)) | (1L << (WEEK - 256)) | (1L << (WINDOW - 256)) | (1L << (YEAR - 256)) | (1L << (ZONE - 256)))) != 0)) ) {
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
		public TerminalNode ANTI() { return getToken(ArcticSqlExtendParser.ANTI, 0); }
		public TerminalNode CROSS() { return getToken(ArcticSqlExtendParser.CROSS, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticSqlExtendParser.EXCEPT, 0); }
		public TerminalNode FULL() { return getToken(ArcticSqlExtendParser.FULL, 0); }
		public TerminalNode INNER() { return getToken(ArcticSqlExtendParser.INNER, 0); }
		public TerminalNode INTERSECT() { return getToken(ArcticSqlExtendParser.INTERSECT, 0); }
		public TerminalNode JOIN() { return getToken(ArcticSqlExtendParser.JOIN, 0); }
		public TerminalNode LATERAL() { return getToken(ArcticSqlExtendParser.LATERAL, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSqlExtendParser.LEFT, 0); }
		public TerminalNode NATURAL() { return getToken(ArcticSqlExtendParser.NATURAL, 0); }
		public TerminalNode ON() { return getToken(ArcticSqlExtendParser.ON, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSqlExtendParser.RIGHT, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSqlExtendParser.SEMI, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSqlExtendParser.SETMINUS, 0); }
		public TerminalNode UNION() { return getToken(ArcticSqlExtendParser.UNION, 0); }
		public TerminalNode USING() { return getToken(ArcticSqlExtendParser.USING, 0); }
		public StrictNonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_strictNonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterStrictNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitStrictNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitStrictNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictNonReservedContext strictNonReserved() throws RecognitionException {
		StrictNonReservedContext _localctx = new StrictNonReservedContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_strictNonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1915);
			_la = _input.LA(1);
			if ( !(_la==ANTI || _la==CROSS || ((((_la - 83)) & ~0x3f) == 0 && ((1L << (_la - 83)) & ((1L << (EXCEPT - 83)) | (1L << (FULL - 83)) | (1L << (INNER - 83)) | (1L << (INTERSECT - 83)) | (1L << (JOIN - 83)) | (1L << (LATERAL - 83)) | (1L << (LEFT - 83)))) != 0) || ((((_la - 156)) & ~0x3f) == 0 && ((1L << (_la - 156)) & ((1L << (NATURAL - 156)) | (1L << (ON - 156)) | (1L << (RIGHT - 156)) | (1L << (SEMI - 156)))) != 0) || ((((_la - 224)) & ~0x3f) == 0 && ((1L << (_la - 224)) & ((1L << (SETMINUS - 224)) | (1L << (UNION - 224)) | (1L << (USING - 224)))) != 0)) ) {
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
		public TerminalNode ADD() { return getToken(ArcticSqlExtendParser.ADD, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSqlExtendParser.AFTER, 0); }
		public TerminalNode ALL() { return getToken(ArcticSqlExtendParser.ALL, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSqlExtendParser.ALTER, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticSqlExtendParser.ANALYZE, 0); }
		public TerminalNode AND() { return getToken(ArcticSqlExtendParser.AND, 0); }
		public TerminalNode ANY() { return getToken(ArcticSqlExtendParser.ANY, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSqlExtendParser.ARCHIVE, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSqlExtendParser.ARRAY, 0); }
		public TerminalNode AS() { return getToken(ArcticSqlExtendParser.AS, 0); }
		public TerminalNode ASC() { return getToken(ArcticSqlExtendParser.ASC, 0); }
		public TerminalNode AT() { return getToken(ArcticSqlExtendParser.AT, 0); }
		public TerminalNode AUTHORIZATION() { return getToken(ArcticSqlExtendParser.AUTHORIZATION, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSqlExtendParser.BETWEEN, 0); }
		public TerminalNode BOTH() { return getToken(ArcticSqlExtendParser.BOTH, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSqlExtendParser.BUCKET, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSqlExtendParser.BUCKETS, 0); }
		public TerminalNode BY() { return getToken(ArcticSqlExtendParser.BY, 0); }
		public TerminalNode CACHE() { return getToken(ArcticSqlExtendParser.CACHE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticSqlExtendParser.CASCADE, 0); }
		public TerminalNode CASE() { return getToken(ArcticSqlExtendParser.CASE, 0); }
		public TerminalNode CAST() { return getToken(ArcticSqlExtendParser.CAST, 0); }
		public TerminalNode CATALOG() { return getToken(ArcticSqlExtendParser.CATALOG, 0); }
		public TerminalNode CATALOGS() { return getToken(ArcticSqlExtendParser.CATALOGS, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticSqlExtendParser.CHANGE, 0); }
		public TerminalNode CHECK() { return getToken(ArcticSqlExtendParser.CHECK, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticSqlExtendParser.CLEAR, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticSqlExtendParser.CLUSTER, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSqlExtendParser.CLUSTERED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSqlExtendParser.CODEGEN, 0); }
		public TerminalNode COLLATE() { return getToken(ArcticSqlExtendParser.COLLATE, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticSqlExtendParser.COLLECTION, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSqlExtendParser.COLUMN, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSqlExtendParser.COLUMNS, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticSqlExtendParser.COMMENT, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSqlExtendParser.COMMIT, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSqlExtendParser.COMPACT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSqlExtendParser.COMPACTIONS, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticSqlExtendParser.COMPUTE, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticSqlExtendParser.CONCATENATE, 0); }
		public TerminalNode CONSTRAINT() { return getToken(ArcticSqlExtendParser.CONSTRAINT, 0); }
		public TerminalNode COST() { return getToken(ArcticSqlExtendParser.COST, 0); }
		public TerminalNode CREATE() { return getToken(ArcticSqlExtendParser.CREATE, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSqlExtendParser.CUBE, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSqlExtendParser.CURRENT, 0); }
		public TerminalNode CURRENT_DATE() { return getToken(ArcticSqlExtendParser.CURRENT_DATE, 0); }
		public TerminalNode CURRENT_TIME() { return getToken(ArcticSqlExtendParser.CURRENT_TIME, 0); }
		public TerminalNode CURRENT_TIMESTAMP() { return getToken(ArcticSqlExtendParser.CURRENT_TIMESTAMP, 0); }
		public TerminalNode CURRENT_USER() { return getToken(ArcticSqlExtendParser.CURRENT_USER, 0); }
		public TerminalNode DATA() { return getToken(ArcticSqlExtendParser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSqlExtendParser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticSqlExtendParser.DATABASES, 0); }
		public TerminalNode DATEADD() { return getToken(ArcticSqlExtendParser.DATEADD, 0); }
		public TerminalNode DATEDIFF() { return getToken(ArcticSqlExtendParser.DATEDIFF, 0); }
		public TerminalNode DAY() { return getToken(ArcticSqlExtendParser.DAY, 0); }
		public TerminalNode DAYOFYEAR() { return getToken(ArcticSqlExtendParser.DAYOFYEAR, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticSqlExtendParser.DBPROPERTIES, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSqlExtendParser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(ArcticSqlExtendParser.DELETE, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSqlExtendParser.DELIMITED, 0); }
		public TerminalNode DESC() { return getToken(ArcticSqlExtendParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSqlExtendParser.DESCRIBE, 0); }
		public TerminalNode DFS() { return getToken(ArcticSqlExtendParser.DFS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSqlExtendParser.DIRECTORIES, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSqlExtendParser.DIRECTORY, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticSqlExtendParser.DISTINCT, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSqlExtendParser.DISTRIBUTE, 0); }
		public TerminalNode DIV() { return getToken(ArcticSqlExtendParser.DIV, 0); }
		public TerminalNode DROP() { return getToken(ArcticSqlExtendParser.DROP, 0); }
		public TerminalNode ELSE() { return getToken(ArcticSqlExtendParser.ELSE, 0); }
		public TerminalNode END() { return getToken(ArcticSqlExtendParser.END, 0); }
		public TerminalNode ESCAPE() { return getToken(ArcticSqlExtendParser.ESCAPE, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticSqlExtendParser.ESCAPED, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSqlExtendParser.EXCHANGE, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSqlExtendParser.EXISTS, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticSqlExtendParser.EXPLAIN, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSqlExtendParser.EXPORT, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSqlExtendParser.EXTENDED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSqlExtendParser.EXTERNAL, 0); }
		public TerminalNode EXTRACT() { return getToken(ArcticSqlExtendParser.EXTRACT, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSqlExtendParser.FALSE, 0); }
		public TerminalNode FETCH() { return getToken(ArcticSqlExtendParser.FETCH, 0); }
		public TerminalNode FILTER() { return getToken(ArcticSqlExtendParser.FILTER, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSqlExtendParser.FIELDS, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSqlExtendParser.FILEFORMAT, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSqlExtendParser.FIRST, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSqlExtendParser.FOLLOWING, 0); }
		public TerminalNode FOR() { return getToken(ArcticSqlExtendParser.FOR, 0); }
		public TerminalNode FOREIGN() { return getToken(ArcticSqlExtendParser.FOREIGN, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSqlExtendParser.FORMAT, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSqlExtendParser.FORMATTED, 0); }
		public TerminalNode FROM() { return getToken(ArcticSqlExtendParser.FROM, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSqlExtendParser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticSqlExtendParser.FUNCTIONS, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticSqlExtendParser.GLOBAL, 0); }
		public TerminalNode GRANT() { return getToken(ArcticSqlExtendParser.GRANT, 0); }
		public TerminalNode GROUP() { return getToken(ArcticSqlExtendParser.GROUP, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSqlExtendParser.GROUPING, 0); }
		public TerminalNode HAVING() { return getToken(ArcticSqlExtendParser.HAVING, 0); }
		public TerminalNode HOUR() { return getToken(ArcticSqlExtendParser.HOUR, 0); }
		public TerminalNode IF() { return getToken(ArcticSqlExtendParser.IF, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSqlExtendParser.IGNORE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSqlExtendParser.IMPORT, 0); }
		public TerminalNode IN() { return getToken(ArcticSqlExtendParser.IN, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSqlExtendParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSqlExtendParser.INDEXES, 0); }
		public TerminalNode INPATH() { return getToken(ArcticSqlExtendParser.INPATH, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSqlExtendParser.INPUTFORMAT, 0); }
		public TerminalNode INSERT() { return getToken(ArcticSqlExtendParser.INSERT, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticSqlExtendParser.INTERVAL, 0); }
		public TerminalNode INTO() { return getToken(ArcticSqlExtendParser.INTO, 0); }
		public TerminalNode IS() { return getToken(ArcticSqlExtendParser.IS, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSqlExtendParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSqlExtendParser.KEYS, 0); }
		public TerminalNode LAST() { return getToken(ArcticSqlExtendParser.LAST, 0); }
		public TerminalNode LAZY() { return getToken(ArcticSqlExtendParser.LAZY, 0); }
		public TerminalNode LEADING() { return getToken(ArcticSqlExtendParser.LEADING, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSqlExtendParser.LIKE, 0); }
		public TerminalNode ILIKE() { return getToken(ArcticSqlExtendParser.ILIKE, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticSqlExtendParser.LIMIT, 0); }
		public TerminalNode LINES() { return getToken(ArcticSqlExtendParser.LINES, 0); }
		public TerminalNode LIST() { return getToken(ArcticSqlExtendParser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(ArcticSqlExtendParser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSqlExtendParser.LOCAL, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSqlExtendParser.LOCATION, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSqlExtendParser.LOCK, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSqlExtendParser.LOCKS, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticSqlExtendParser.LOGICAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSqlExtendParser.MACRO, 0); }
		public TerminalNode MAP() { return getToken(ArcticSqlExtendParser.MAP, 0); }
		public TerminalNode MATCHED() { return getToken(ArcticSqlExtendParser.MATCHED, 0); }
		public TerminalNode MERGE() { return getToken(ArcticSqlExtendParser.MERGE, 0); }
		public TerminalNode MICROSECOND() { return getToken(ArcticSqlExtendParser.MICROSECOND, 0); }
		public TerminalNode MILLISECOND() { return getToken(ArcticSqlExtendParser.MILLISECOND, 0); }
		public TerminalNode MINUTE() { return getToken(ArcticSqlExtendParser.MINUTE, 0); }
		public TerminalNode MONTH() { return getToken(ArcticSqlExtendParser.MONTH, 0); }
		public TerminalNode MSCK() { return getToken(ArcticSqlExtendParser.MSCK, 0); }
		public TerminalNode NAMESPACE() { return getToken(ArcticSqlExtendParser.NAMESPACE, 0); }
		public TerminalNode NAMESPACES() { return getToken(ArcticSqlExtendParser.NAMESPACES, 0); }
		public TerminalNode NO() { return getToken(ArcticSqlExtendParser.NO, 0); }
		public TerminalNode NOT() { return getToken(ArcticSqlExtendParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(ArcticSqlExtendParser.NULL, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSqlExtendParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(ArcticSqlExtendParser.OF, 0); }
		public TerminalNode ONLY() { return getToken(ArcticSqlExtendParser.ONLY, 0); }
		public TerminalNode OPTION() { return getToken(ArcticSqlExtendParser.OPTION, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSqlExtendParser.OPTIONS, 0); }
		public TerminalNode OR() { return getToken(ArcticSqlExtendParser.OR, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSqlExtendParser.ORDER, 0); }
		public TerminalNode OUT() { return getToken(ArcticSqlExtendParser.OUT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticSqlExtendParser.OUTER, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSqlExtendParser.OUTPUTFORMAT, 0); }
		public TerminalNode OVER() { return getToken(ArcticSqlExtendParser.OVER, 0); }
		public TerminalNode OVERLAPS() { return getToken(ArcticSqlExtendParser.OVERLAPS, 0); }
		public TerminalNode OVERLAY() { return getToken(ArcticSqlExtendParser.OVERLAY, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSqlExtendParser.OVERWRITE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSqlExtendParser.PARTITION, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticSqlExtendParser.PARTITIONED, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSqlExtendParser.PARTITIONS, 0); }
		public TerminalNode PERCENTILE_CONT() { return getToken(ArcticSqlExtendParser.PERCENTILE_CONT, 0); }
		public TerminalNode PERCENTILE_DISC() { return getToken(ArcticSqlExtendParser.PERCENTILE_DISC, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticSqlExtendParser.PERCENTLIT, 0); }
		public TerminalNode PIVOT() { return getToken(ArcticSqlExtendParser.PIVOT, 0); }
		public TerminalNode PLACING() { return getToken(ArcticSqlExtendParser.PLACING, 0); }
		public TerminalNode POSITION() { return getToken(ArcticSqlExtendParser.POSITION, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSqlExtendParser.PRECEDING, 0); }
		public TerminalNode PRIMARY() { return getToken(ArcticSqlExtendParser.PRIMARY, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSqlExtendParser.PRINCIPALS, 0); }
		public TerminalNode PROPERTIES() { return getToken(ArcticSqlExtendParser.PROPERTIES, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSqlExtendParser.PURGE, 0); }
		public TerminalNode QUARTER() { return getToken(ArcticSqlExtendParser.QUARTER, 0); }
		public TerminalNode QUERY() { return getToken(ArcticSqlExtendParser.QUERY, 0); }
		public TerminalNode RANGE() { return getToken(ArcticSqlExtendParser.RANGE, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSqlExtendParser.RECORDREADER, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSqlExtendParser.RECORDWRITER, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticSqlExtendParser.RECOVER, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSqlExtendParser.REDUCE, 0); }
		public TerminalNode REFERENCES() { return getToken(ArcticSqlExtendParser.REFERENCES, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticSqlExtendParser.REFRESH, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSqlExtendParser.RENAME, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticSqlExtendParser.REPAIR, 0); }
		public TerminalNode REPEATABLE() { return getToken(ArcticSqlExtendParser.REPEATABLE, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSqlExtendParser.REPLACE, 0); }
		public TerminalNode RESET() { return getToken(ArcticSqlExtendParser.RESET, 0); }
		public TerminalNode RESPECT() { return getToken(ArcticSqlExtendParser.RESPECT, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticSqlExtendParser.RESTRICT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSqlExtendParser.REVOKE, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticSqlExtendParser.RLIKE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSqlExtendParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSqlExtendParser.ROLES, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSqlExtendParser.ROLLBACK, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSqlExtendParser.ROLLUP, 0); }
		public TerminalNode ROW() { return getToken(ArcticSqlExtendParser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(ArcticSqlExtendParser.ROWS, 0); }
		public TerminalNode SCHEMA() { return getToken(ArcticSqlExtendParser.SCHEMA, 0); }
		public TerminalNode SCHEMAS() { return getToken(ArcticSqlExtendParser.SCHEMAS, 0); }
		public TerminalNode SECOND() { return getToken(ArcticSqlExtendParser.SECOND, 0); }
		public TerminalNode SELECT() { return getToken(ArcticSqlExtendParser.SELECT, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticSqlExtendParser.SEPARATED, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSqlExtendParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSqlExtendParser.SERDEPROPERTIES, 0); }
		public TerminalNode SESSION_USER() { return getToken(ArcticSqlExtendParser.SESSION_USER, 0); }
		public TerminalNode SET() { return getToken(ArcticSqlExtendParser.SET, 0); }
		public TerminalNode SETS() { return getToken(ArcticSqlExtendParser.SETS, 0); }
		public TerminalNode SHOW() { return getToken(ArcticSqlExtendParser.SHOW, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSqlExtendParser.SKEWED, 0); }
		public TerminalNode SOME() { return getToken(ArcticSqlExtendParser.SOME, 0); }
		public TerminalNode SORT() { return getToken(ArcticSqlExtendParser.SORT, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSqlExtendParser.SORTED, 0); }
		public TerminalNode START() { return getToken(ArcticSqlExtendParser.START, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSqlExtendParser.STATISTICS, 0); }
		public TerminalNode STORED() { return getToken(ArcticSqlExtendParser.STORED, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticSqlExtendParser.STRATIFY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSqlExtendParser.STRUCT, 0); }
		public TerminalNode SUBSTR() { return getToken(ArcticSqlExtendParser.SUBSTR, 0); }
		public TerminalNode SUBSTRING() { return getToken(ArcticSqlExtendParser.SUBSTRING, 0); }
		public TerminalNode SYNC() { return getToken(ArcticSqlExtendParser.SYNC, 0); }
		public TerminalNode SYSTEM_TIME() { return getToken(ArcticSqlExtendParser.SYSTEM_TIME, 0); }
		public TerminalNode SYSTEM_VERSION() { return getToken(ArcticSqlExtendParser.SYSTEM_VERSION, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSqlExtendParser.TABLE, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSqlExtendParser.TABLES, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSqlExtendParser.TABLESAMPLE, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSqlExtendParser.TBLPROPERTIES, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSqlExtendParser.TEMPORARY, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticSqlExtendParser.TERMINATED, 0); }
		public TerminalNode THEN() { return getToken(ArcticSqlExtendParser.THEN, 0); }
		public TerminalNode TIME() { return getToken(ArcticSqlExtendParser.TIME, 0); }
		public TerminalNode TIMESTAMP() { return getToken(ArcticSqlExtendParser.TIMESTAMP, 0); }
		public TerminalNode TIMESTAMPADD() { return getToken(ArcticSqlExtendParser.TIMESTAMPADD, 0); }
		public TerminalNode TIMESTAMPDIFF() { return getToken(ArcticSqlExtendParser.TIMESTAMPDIFF, 0); }
		public TerminalNode TO() { return getToken(ArcticSqlExtendParser.TO, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSqlExtendParser.TOUCH, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticSqlExtendParser.TRAILING, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSqlExtendParser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSqlExtendParser.TRANSACTIONS, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticSqlExtendParser.TRANSFORM, 0); }
		public TerminalNode TRIM() { return getToken(ArcticSqlExtendParser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSqlExtendParser.TRUE, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticSqlExtendParser.TRUNCATE, 0); }
		public TerminalNode TRY_CAST() { return getToken(ArcticSqlExtendParser.TRY_CAST, 0); }
		public TerminalNode TYPE() { return getToken(ArcticSqlExtendParser.TYPE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSqlExtendParser.UNARCHIVE, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticSqlExtendParser.UNBOUNDED, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticSqlExtendParser.UNCACHE, 0); }
		public TerminalNode UNIQUE() { return getToken(ArcticSqlExtendParser.UNIQUE, 0); }
		public TerminalNode UNKNOWN() { return getToken(ArcticSqlExtendParser.UNKNOWN, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSqlExtendParser.UNLOCK, 0); }
		public TerminalNode UNSET() { return getToken(ArcticSqlExtendParser.UNSET, 0); }
		public TerminalNode UPDATE() { return getToken(ArcticSqlExtendParser.UPDATE, 0); }
		public TerminalNode USE() { return getToken(ArcticSqlExtendParser.USE, 0); }
		public TerminalNode USER() { return getToken(ArcticSqlExtendParser.USER, 0); }
		public TerminalNode VALUES() { return getToken(ArcticSqlExtendParser.VALUES, 0); }
		public TerminalNode VERSION() { return getToken(ArcticSqlExtendParser.VERSION, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSqlExtendParser.VIEW, 0); }
		public TerminalNode VIEWS() { return getToken(ArcticSqlExtendParser.VIEWS, 0); }
		public TerminalNode WEEK() { return getToken(ArcticSqlExtendParser.WEEK, 0); }
		public TerminalNode WHEN() { return getToken(ArcticSqlExtendParser.WHEN, 0); }
		public TerminalNode WHERE() { return getToken(ArcticSqlExtendParser.WHERE, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticSqlExtendParser.WINDOW, 0); }
		public TerminalNode WITH() { return getToken(ArcticSqlExtendParser.WITH, 0); }
		public TerminalNode WITHIN() { return getToken(ArcticSqlExtendParser.WITHIN, 0); }
		public TerminalNode YEAR() { return getToken(ArcticSqlExtendParser.YEAR, 0); }
		public TerminalNode ZONE() { return getToken(ArcticSqlExtendParser.ZONE, 0); }
		public NonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).enterNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSqlExtendListener ) ((ArcticSqlExtendListener)listener).exitNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSqlExtendVisitor ) return ((ArcticSqlExtendVisitor<? extends T>)visitor).visitNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonReservedContext nonReserved() throws RecognitionException {
		NonReservedContext _localctx = new NonReservedContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_nonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1917);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ADD) | (1L << AFTER) | (1L << ALL) | (1L << ALTER) | (1L << ANALYZE) | (1L << AND) | (1L << ANY) | (1L << ARCHIVE) | (1L << ARRAY) | (1L << AS) | (1L << ASC) | (1L << AT) | (1L << AUTHORIZATION) | (1L << BETWEEN) | (1L << BOTH) | (1L << BUCKET) | (1L << BUCKETS) | (1L << BY) | (1L << CACHE) | (1L << CASCADE) | (1L << CASE) | (1L << CAST) | (1L << CATALOG) | (1L << CATALOGS) | (1L << CHANGE) | (1L << CHECK) | (1L << CLEAR) | (1L << CLUSTER) | (1L << CLUSTERED) | (1L << CODEGEN) | (1L << COLLATE) | (1L << COLLECTION) | (1L << COLUMN) | (1L << COLUMNS) | (1L << COMMENT) | (1L << COMMIT) | (1L << COMPACT) | (1L << COMPACTIONS) | (1L << COMPUTE) | (1L << CONCATENATE) | (1L << CONSTRAINT) | (1L << COST) | (1L << CREATE) | (1L << CUBE) | (1L << CURRENT) | (1L << CURRENT_DATE) | (1L << CURRENT_TIME) | (1L << CURRENT_TIMESTAMP) | (1L << CURRENT_USER) | (1L << DAY) | (1L << DAYOFYEAR) | (1L << DATA) | (1L << DATABASE) | (1L << DATABASES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (DATEADD - 64)) | (1L << (DATEDIFF - 64)) | (1L << (DBPROPERTIES - 64)) | (1L << (DEFINED - 64)) | (1L << (DELETE - 64)) | (1L << (DELIMITED - 64)) | (1L << (DESC - 64)) | (1L << (DESCRIBE - 64)) | (1L << (DFS - 64)) | (1L << (DIRECTORIES - 64)) | (1L << (DIRECTORY - 64)) | (1L << (DISTINCT - 64)) | (1L << (DISTRIBUTE - 64)) | (1L << (DIV - 64)) | (1L << (DROP - 64)) | (1L << (ELSE - 64)) | (1L << (END - 64)) | (1L << (ESCAPE - 64)) | (1L << (ESCAPED - 64)) | (1L << (EXCHANGE - 64)) | (1L << (EXISTS - 64)) | (1L << (EXPLAIN - 64)) | (1L << (EXPORT - 64)) | (1L << (EXTENDED - 64)) | (1L << (EXTERNAL - 64)) | (1L << (EXTRACT - 64)) | (1L << (FALSE - 64)) | (1L << (FETCH - 64)) | (1L << (FIELDS - 64)) | (1L << (FILTER - 64)) | (1L << (FILEFORMAT - 64)) | (1L << (FIRST - 64)) | (1L << (FOLLOWING - 64)) | (1L << (FOR - 64)) | (1L << (FOREIGN - 64)) | (1L << (FORMAT - 64)) | (1L << (FORMATTED - 64)) | (1L << (FROM - 64)) | (1L << (FUNCTION - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (GLOBAL - 64)) | (1L << (GRANT - 64)) | (1L << (GROUP - 64)) | (1L << (GROUPING - 64)) | (1L << (HAVING - 64)) | (1L << (HOUR - 64)) | (1L << (IF - 64)) | (1L << (IGNORE - 64)) | (1L << (IMPORT - 64)) | (1L << (IN - 64)) | (1L << (INDEX - 64)) | (1L << (INDEXES - 64)) | (1L << (INPATH - 64)) | (1L << (INPUTFORMAT - 64)) | (1L << (INSERT - 64)) | (1L << (INTERVAL - 64)) | (1L << (INTO - 64)) | (1L << (IS - 64)) | (1L << (ITEMS - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (KEYS - 128)) | (1L << (LAST - 128)) | (1L << (LAZY - 128)) | (1L << (LEADING - 128)) | (1L << (LIKE - 128)) | (1L << (ILIKE - 128)) | (1L << (LIMIT - 128)) | (1L << (LINES - 128)) | (1L << (LIST - 128)) | (1L << (LOAD - 128)) | (1L << (LOCAL - 128)) | (1L << (LOCATION - 128)) | (1L << (LOCK - 128)) | (1L << (LOCKS - 128)) | (1L << (LOGICAL - 128)) | (1L << (MACRO - 128)) | (1L << (MAP - 128)) | (1L << (MATCHED - 128)) | (1L << (MERGE - 128)) | (1L << (MICROSECOND - 128)) | (1L << (MILLISECOND - 128)) | (1L << (MINUTE - 128)) | (1L << (MONTH - 128)) | (1L << (MSCK - 128)) | (1L << (NAMESPACE - 128)) | (1L << (NAMESPACES - 128)) | (1L << (NO - 128)) | (1L << (NOT - 128)) | (1L << (NULL - 128)) | (1L << (NULLS - 128)) | (1L << (OF - 128)) | (1L << (ONLY - 128)) | (1L << (OPTION - 128)) | (1L << (OPTIONS - 128)) | (1L << (OR - 128)) | (1L << (ORDER - 128)) | (1L << (OUT - 128)) | (1L << (OUTER - 128)) | (1L << (OUTPUTFORMAT - 128)) | (1L << (OVER - 128)) | (1L << (OVERLAPS - 128)) | (1L << (OVERLAY - 128)) | (1L << (OVERWRITE - 128)) | (1L << (PARTITION - 128)) | (1L << (PARTITIONED - 128)) | (1L << (PARTITIONS - 128)) | (1L << (PERCENTILE_CONT - 128)) | (1L << (PERCENTILE_DISC - 128)) | (1L << (PERCENTLIT - 128)) | (1L << (PIVOT - 128)) | (1L << (PLACING - 128)) | (1L << (POSITION - 128)) | (1L << (PRECEDING - 128)) | (1L << (PRIMARY - 128)) | (1L << (PRINCIPALS - 128)) | (1L << (PROPERTIES - 128)) | (1L << (PURGE - 128)) | (1L << (QUARTER - 128)) | (1L << (QUERY - 128)) | (1L << (RANGE - 128)))) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & ((1L << (RECORDREADER - 192)) | (1L << (RECORDWRITER - 192)) | (1L << (RECOVER - 192)) | (1L << (REDUCE - 192)) | (1L << (REFERENCES - 192)) | (1L << (REFRESH - 192)) | (1L << (RENAME - 192)) | (1L << (REPAIR - 192)) | (1L << (REPEATABLE - 192)) | (1L << (REPLACE - 192)) | (1L << (RESET - 192)) | (1L << (RESPECT - 192)) | (1L << (RESTRICT - 192)) | (1L << (REVOKE - 192)) | (1L << (RLIKE - 192)) | (1L << (ROLE - 192)) | (1L << (ROLES - 192)) | (1L << (ROLLBACK - 192)) | (1L << (ROLLUP - 192)) | (1L << (ROW - 192)) | (1L << (ROWS - 192)) | (1L << (SECOND - 192)) | (1L << (SCHEMA - 192)) | (1L << (SCHEMAS - 192)) | (1L << (SELECT - 192)) | (1L << (SEPARATED - 192)) | (1L << (SERDE - 192)) | (1L << (SERDEPROPERTIES - 192)) | (1L << (SESSION_USER - 192)) | (1L << (SET - 192)) | (1L << (SETS - 192)) | (1L << (SHOW - 192)) | (1L << (SKEWED - 192)) | (1L << (SOME - 192)) | (1L << (SORT - 192)) | (1L << (SORTED - 192)) | (1L << (START - 192)) | (1L << (STATISTICS - 192)) | (1L << (STORED - 192)) | (1L << (STRATIFY - 192)) | (1L << (STRUCT - 192)) | (1L << (SUBSTR - 192)) | (1L << (SUBSTRING - 192)) | (1L << (SYNC - 192)) | (1L << (SYSTEM_TIME - 192)) | (1L << (SYSTEM_VERSION - 192)) | (1L << (TABLE - 192)) | (1L << (TABLES - 192)) | (1L << (TABLESAMPLE - 192)) | (1L << (TBLPROPERTIES - 192)) | (1L << (TEMPORARY - 192)) | (1L << (TERMINATED - 192)) | (1L << (THEN - 192)) | (1L << (TIME - 192)) | (1L << (TIMESTAMP - 192)) | (1L << (TIMESTAMPADD - 192)) | (1L << (TIMESTAMPDIFF - 192)) | (1L << (TO - 192)) | (1L << (TOUCH - 192)) | (1L << (TRAILING - 192)) | (1L << (TRANSACTION - 192)))) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & ((1L << (TRANSACTIONS - 256)) | (1L << (TRANSFORM - 256)) | (1L << (TRIM - 256)) | (1L << (TRUE - 256)) | (1L << (TRUNCATE - 256)) | (1L << (TRY_CAST - 256)) | (1L << (TYPE - 256)) | (1L << (UNARCHIVE - 256)) | (1L << (UNBOUNDED - 256)) | (1L << (UNCACHE - 256)) | (1L << (UNIQUE - 256)) | (1L << (UNKNOWN - 256)) | (1L << (UNLOCK - 256)) | (1L << (UNSET - 256)) | (1L << (UPDATE - 256)) | (1L << (USE - 256)) | (1L << (USER - 256)) | (1L << (VALUES - 256)) | (1L << (VERSION - 256)) | (1L << (VIEW - 256)) | (1L << (VIEWS - 256)) | (1L << (WEEK - 256)) | (1L << (WHEN - 256)) | (1L << (WHERE - 256)) | (1L << (WINDOW - 256)) | (1L << (WITH - 256)) | (1L << (WITHIN - 256)) | (1L << (YEAR - 256)) | (1L << (ZONE - 256)))) != 0)) ) {
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
		case 24:
			return queryTerm_sempred((QueryTermContext)_localctx, predIndex);
		case 72:
			return booleanExpression_sempred((BooleanExpressionContext)_localctx, predIndex);
		case 74:
			return valueExpression_sempred((ValueExpressionContext)_localctx, predIndex);
		case 76:
			return primaryExpression_sempred((PrimaryExpressionContext)_localctx, predIndex);
		case 101:
			return identifier_sempred((IdentifierContext)_localctx, predIndex);
		case 102:
			return strictIdentifier_sempred((StrictIdentifierContext)_localctx, predIndex);
		case 104:
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
			return precpred(_ctx, 9);
		case 15:
			return precpred(_ctx, 7);
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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\u0147\u0782\4\2\t"+
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
		"k\4l\tl\4m\tm\3\2\3\2\7\2\u00dd\n\2\f\2\16\2\u00e0\13\2\3\2\3\2\3\3\3"+
		"\3\3\3\5\3\u00e7\n\3\3\3\3\3\5\3\u00eb\n\3\3\3\5\3\u00ee\n\3\3\3\3\3\5"+
		"\3\u00f2\n\3\3\3\5\3\u00f5\n\3\3\4\3\4\5\4\u00f9\n\4\3\4\5\4\u00fc\n\4"+
		"\3\4\3\4\3\4\3\4\5\4\u0102\n\4\3\4\3\4\3\5\3\5\3\5\3\5\5\5\u010a\n\5\3"+
		"\5\3\5\3\5\5\5\u010f\n\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u011b"+
		"\n\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u0127\n\b\3\b\3\b\3\b"+
		"\5\b\u012c\n\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13\5\13\u0135\n\13\3\13\3\13"+
		"\3\13\3\f\3\f\3\f\3\f\7\f\u013e\n\f\f\f\16\f\u0141\13\f\3\r\3\r\5\r\u0145"+
		"\n\r\3\r\5\r\u0148\n\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\17\3"+
		"\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\7\17\u015e\n\17\f\17"+
		"\16\17\u0161\13\17\3\20\3\20\3\20\3\20\7\20\u0167\n\20\f\20\16\20\u016a"+
		"\13\20\3\20\3\20\3\21\3\21\5\21\u0170\n\21\3\21\5\21\u0173\n\21\3\22\3"+
		"\22\3\22\7\22\u0178\n\22\f\22\16\22\u017b\13\22\3\22\5\22\u017e\n\22\3"+
		"\23\3\23\3\23\3\23\5\23\u0184\n\23\3\24\3\24\3\24\3\24\7\24\u018a\n\24"+
		"\f\24\16\24\u018d\13\24\3\24\3\24\3\25\3\25\3\25\3\25\7\25\u0195\n\25"+
		"\f\25\16\25\u0198\13\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u01a2"+
		"\n\26\3\27\3\27\3\27\3\27\3\27\5\27\u01a9\n\27\3\30\3\30\3\30\3\30\5\30"+
		"\u01af\n\30\3\31\3\31\3\31\3\31\3\31\7\31\u01b6\n\31\f\31\16\31\u01b9"+
		"\13\31\5\31\u01bb\n\31\3\31\3\31\3\31\3\31\3\31\7\31\u01c2\n\31\f\31\16"+
		"\31\u01c5\13\31\5\31\u01c7\n\31\3\31\3\31\3\31\3\31\3\31\7\31\u01ce\n"+
		"\31\f\31\16\31\u01d1\13\31\5\31\u01d3\n\31\3\31\3\31\3\31\3\31\3\31\7"+
		"\31\u01da\n\31\f\31\16\31\u01dd\13\31\5\31\u01df\n\31\3\31\5\31\u01e2"+
		"\n\31\3\31\3\31\3\31\5\31\u01e7\n\31\5\31\u01e9\n\31\3\32\3\32\3\32\3"+
		"\32\3\32\3\32\3\32\5\32\u01f2\n\32\3\32\3\32\3\32\3\32\3\32\5\32\u01f9"+
		"\n\32\3\32\3\32\3\32\3\32\3\32\5\32\u0200\n\32\3\32\7\32\u0203\n\32\f"+
		"\32\16\32\u0206\13\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\5\33"+
		"\u0211\n\33\3\34\3\34\5\34\u0215\n\34\3\34\3\34\5\34\u0219\n\34\3\35\3"+
		"\35\6\35\u021d\n\35\r\35\16\35\u021e\3\36\3\36\5\36\u0223\n\36\3\36\3"+
		"\36\3\36\3\36\7\36\u0229\n\36\f\36\16\36\u022c\13\36\3\36\5\36\u022f\n"+
		"\36\3\36\5\36\u0232\n\36\3\36\5\36\u0235\n\36\3\36\5\36\u0238\n\36\3\36"+
		"\3\36\5\36\u023c\n\36\3\37\3\37\5\37\u0240\n\37\3\37\7\37\u0243\n\37\f"+
		"\37\16\37\u0246\13\37\3\37\5\37\u0249\n\37\3\37\5\37\u024c\n\37\3\37\5"+
		"\37\u024f\n\37\3\37\5\37\u0252\n\37\3\37\3\37\5\37\u0256\n\37\3\37\7\37"+
		"\u0259\n\37\f\37\16\37\u025c\13\37\3\37\5\37\u025f\n\37\3\37\5\37\u0262"+
		"\n\37\3\37\5\37\u0265\n\37\3\37\5\37\u0268\n\37\5\37\u026a\n\37\3 \3 "+
		"\3 \3 \5 \u0270\n \3 \3 \3 \3 \3 \5 \u0277\n \3 \3 \3 \5 \u027c\n \3 "+
		"\5 \u027f\n \3 \5 \u0282\n \3 \3 \5 \u0286\n \3 \3 \3 \3 \3 \3 \3 \3 "+
		"\5 \u0290\n \3 \3 \5 \u0294\n \5 \u0296\n \3 \5 \u0299\n \3 \3 \5 \u029d"+
		"\n \3!\3!\7!\u02a1\n!\f!\16!\u02a4\13!\3!\5!\u02a7\n!\3!\3!\3\"\3\"\3"+
		"\"\3#\3#\3#\3$\3$\3$\5$\u02b4\n$\3$\7$\u02b7\n$\f$\16$\u02ba\13$\3$\3"+
		"$\3%\3%\3%\3%\3%\3%\7%\u02c4\n%\f%\16%\u02c7\13%\3%\3%\5%\u02cb\n%\3&"+
		"\3&\3&\3&\7&\u02d1\n&\f&\16&\u02d4\13&\3&\7&\u02d7\n&\f&\16&\u02da\13"+
		"&\3&\5&\u02dd\n&\3\'\5\'\u02e0\n\'\3\'\3\'\3\'\3\'\3\'\5\'\u02e7\n\'\3"+
		"\'\3\'\3\'\3\'\5\'\u02ed\n\'\3(\3(\3(\3(\3(\7(\u02f4\n(\f(\16(\u02f7\13"+
		"(\3(\3(\3(\3(\3(\7(\u02fe\n(\f(\16(\u0301\13(\3(\3(\3(\3(\3(\3(\3(\3("+
		"\3(\3(\7(\u030d\n(\f(\16(\u0310\13(\3(\3(\5(\u0314\n(\5(\u0316\n(\3)\3"+
		")\5)\u031a\n)\3*\3*\3*\3*\3*\7*\u0321\n*\f*\16*\u0324\13*\3*\3*\3*\3*"+
		"\3*\3*\3*\3*\7*\u032e\n*\f*\16*\u0331\13*\3*\3*\5*\u0335\n*\3+\3+\5+\u0339"+
		"\n+\3,\3,\3,\3,\7,\u033f\n,\f,\16,\u0342\13,\5,\u0344\n,\3,\3,\5,\u0348"+
		"\n,\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\7-\u0354\n-\f-\16-\u0357\13-\3-\3-\3"+
		"-\3.\3.\3.\3.\3.\7.\u0361\n.\f.\16.\u0364\13.\3.\3.\5.\u0368\n.\3/\3/"+
		"\5/\u036c\n/\3/\5/\u036f\n/\3\60\3\60\3\60\5\60\u0374\n\60\3\60\3\60\3"+
		"\60\3\60\3\60\7\60\u037b\n\60\f\60\16\60\u037e\13\60\5\60\u0380\n\60\3"+
		"\60\3\60\3\60\5\60\u0385\n\60\3\60\3\60\3\60\7\60\u038a\n\60\f\60\16\60"+
		"\u038d\13\60\5\60\u038f\n\60\3\61\3\61\3\62\5\62\u0394\n\62\3\62\3\62"+
		"\7\62\u0398\n\62\f\62\16\62\u039b\13\62\3\63\3\63\3\63\5\63\u03a0\n\63"+
		"\3\63\3\63\5\63\u03a4\n\63\3\63\3\63\3\63\3\63\5\63\u03aa\n\63\3\63\3"+
		"\63\5\63\u03ae\n\63\3\64\5\64\u03b1\n\64\3\64\3\64\3\64\5\64\u03b6\n\64"+
		"\3\64\5\64\u03b9\n\64\3\64\3\64\3\64\5\64\u03be\n\64\3\64\3\64\5\64\u03c2"+
		"\n\64\3\64\5\64\u03c5\n\64\3\64\5\64\u03c8\n\64\3\65\3\65\3\65\3\65\5"+
		"\65\u03ce\n\65\3\66\3\66\3\66\5\66\u03d3\n\66\3\66\3\66\3\66\3\66\3\66"+
		"\5\66\u03da\n\66\3\67\5\67\u03dd\n\67\3\67\3\67\3\67\3\67\3\67\3\67\3"+
		"\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\5\67\u03ef\n\67\5\67"+
		"\u03f1\n\67\3\67\5\67\u03f4\n\67\38\38\38\38\39\39\39\79\u03fd\n9\f9\16"+
		"9\u0400\139\3:\3:\3:\3:\7:\u0406\n:\f:\16:\u0409\13:\3:\3:\3;\3;\5;\u040f"+
		"\n;\3<\3<\5<\u0413\n<\3<\5<\u0416\n<\3<\3<\3<\3<\3<\3<\5<\u041e\n<\3<"+
		"\3<\3<\3<\3<\3<\5<\u0426\n<\3<\3<\3<\3<\5<\u042c\n<\3=\3=\3=\3=\7=\u0432"+
		"\n=\f=\16=\u0435\13=\3=\3=\3>\3>\3>\3>\3>\7>\u043e\n>\f>\16>\u0441\13"+
		">\5>\u0443\n>\3>\3>\3>\3?\5?\u0449\n?\3?\3?\5?\u044d\n?\5?\u044f\n?\3"+
		"@\3@\3@\3@\3@\3@\3@\5@\u0458\n@\3@\3@\3@\3@\3@\3@\3@\3@\3@\3@\5@\u0464"+
		"\n@\5@\u0466\n@\3@\3@\3@\3@\3@\5@\u046d\n@\3@\3@\3@\3@\3@\5@\u0474\n@"+
		"\3@\3@\3@\3@\5@\u047a\n@\3@\3@\3@\3@\5@\u0480\n@\5@\u0482\n@\3A\3A\3A"+
		"\7A\u0487\nA\fA\16A\u048a\13A\3B\3B\5B\u048e\nB\3B\3B\5B\u0492\nB\5B\u0494"+
		"\nB\3C\3C\3C\7C\u0499\nC\fC\16C\u049c\13C\3D\3D\3D\3D\7D\u04a2\nD\fD\16"+
		"D\u04a5\13D\3D\3D\3E\3E\5E\u04ab\nE\3F\3F\3F\3F\3F\3F\7F\u04b3\nF\fF\16"+
		"F\u04b6\13F\3F\3F\5F\u04ba\nF\3G\3G\5G\u04be\nG\3H\3H\3I\3I\3I\7I\u04c5"+
		"\nI\fI\16I\u04c8\13I\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\5J\u04d4\nJ\5J\u04d6"+
		"\nJ\3J\3J\3J\3J\3J\3J\7J\u04de\nJ\fJ\16J\u04e1\13J\3K\5K\u04e4\nK\3K\3"+
		"K\3K\3K\3K\3K\5K\u04ec\nK\3K\3K\3K\3K\3K\7K\u04f3\nK\fK\16K\u04f6\13K"+
		"\3K\3K\3K\5K\u04fb\nK\3K\3K\3K\3K\3K\3K\5K\u0503\nK\3K\3K\3K\5K\u0508"+
		"\nK\3K\3K\3K\3K\3K\3K\3K\3K\7K\u0512\nK\fK\16K\u0515\13K\3K\3K\5K\u0519"+
		"\nK\3K\5K\u051c\nK\3K\3K\3K\3K\5K\u0522\nK\3K\3K\5K\u0526\nK\3K\3K\3K"+
		"\5K\u052b\nK\3K\3K\3K\5K\u0530\nK\3K\3K\3K\5K\u0535\nK\3L\3L\3L\3L\5L"+
		"\u053b\nL\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\7L"+
		"\u0550\nL\fL\16L\u0553\13L\3M\3M\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3"+
		"N\3N\3N\3N\3N\3N\3N\3N\3N\3N\6N\u056d\nN\rN\16N\u056e\3N\3N\5N\u0573\n"+
		"N\3N\3N\3N\3N\3N\6N\u057a\nN\rN\16N\u057b\3N\3N\5N\u0580\nN\3N\3N\3N\3"+
		"N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\7N\u0590\nN\fN\16N\u0593\13N\5N\u0595"+
		"\nN\3N\3N\3N\3N\3N\3N\5N\u059d\nN\3N\3N\3N\3N\3N\3N\3N\5N\u05a6\nN\3N"+
		"\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\6N\u05bb\nN\rN"+
		"\16N\u05bc\3N\3N\3N\3N\3N\3N\3N\3N\3N\5N\u05c8\nN\3N\3N\3N\7N\u05cd\n"+
		"N\fN\16N\u05d0\13N\5N\u05d2\nN\3N\3N\3N\3N\3N\3N\3N\5N\u05db\nN\3N\3N"+
		"\5N\u05df\nN\3N\3N\5N\u05e3\nN\3N\3N\3N\3N\3N\3N\3N\3N\6N\u05ed\nN\rN"+
		"\16N\u05ee\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3"+
		"N\3N\3N\3N\5N\u0608\nN\3N\3N\3N\3N\3N\5N\u060f\nN\3N\5N\u0612\nN\3N\3"+
		"N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\5N\u0621\nN\3N\3N\3N\3N\3N\3N\3N\3"+
		"N\3N\3N\3N\3N\3N\3N\3N\5N\u0632\nN\5N\u0634\nN\3N\3N\3N\3N\3N\3N\3N\3"+
		"N\7N\u063e\nN\fN\16N\u0641\13N\3O\3O\3O\3O\3O\3O\3O\3O\6O\u064b\nO\rO"+
		"\16O\u064c\5O\u064f\nO\3P\3P\3Q\3Q\3R\3R\3R\5R\u0658\nR\3S\3S\5S\u065c"+
		"\nS\3T\3T\3T\6T\u0661\nT\rT\16T\u0662\3U\3U\3U\5U\u0668\nU\3V\3V\3V\3"+
		"V\3V\3W\5W\u0670\nW\3W\3W\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3"+
		"X\5X\u0683\nX\3X\3X\5X\u0687\nX\3X\3X\3X\3X\5X\u068d\nX\3X\3X\3X\3X\5"+
		"X\u0693\nX\3X\3X\3X\3X\3X\7X\u069a\nX\fX\16X\u069d\13X\3X\5X\u06a0\nX"+
		"\5X\u06a2\nX\3Y\3Y\3Y\7Y\u06a7\nY\fY\16Y\u06aa\13Y\3Z\3Z\3Z\3Z\5Z\u06b0"+
		"\nZ\3Z\5Z\u06b3\nZ\3[\3[\3[\7[\u06b8\n[\f[\16[\u06bb\13[\3\\\3\\\5\\\u06bf"+
		"\n\\\3\\\3\\\3\\\5\\\u06c4\n\\\3\\\5\\\u06c7\n\\\3]\3]\3]\3]\3]\3^\3^"+
		"\3^\3^\7^\u06d2\n^\f^\16^\u06d5\13^\3_\3_\3_\3_\3`\3`\3`\3`\3`\3`\3`\3"+
		"`\3`\3`\3`\7`\u06e6\n`\f`\16`\u06e9\13`\3`\3`\3`\3`\3`\7`\u06f0\n`\f`"+
		"\16`\u06f3\13`\5`\u06f5\n`\3`\3`\3`\3`\3`\7`\u06fc\n`\f`\16`\u06ff\13"+
		"`\5`\u0701\n`\5`\u0703\n`\3`\5`\u0706\n`\3`\5`\u0709\n`\3a\3a\3a\3a\3"+
		"a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\5a\u071b\na\3b\3b\3b\3b\3b\3b\3b\5"+
		"b\u0724\nb\3c\3c\3c\3c\5c\u072a\nc\3d\3d\3d\7d\u072f\nd\fd\16d\u0732\13"+
		"d\3e\3e\3e\3f\3f\6f\u0739\nf\rf\16f\u073a\3f\5f\u073e\nf\3g\3g\3g\5g\u0743"+
		"\ng\3h\3h\3h\3h\3h\3h\5h\u074b\nh\3i\3i\3j\3j\5j\u0751\nj\3j\3j\3j\5j"+
		"\u0756\nj\3j\3j\3j\5j\u075b\nj\3j\3j\5j\u075f\nj\3j\3j\5j\u0763\nj\3j"+
		"\3j\5j\u0767\nj\3j\3j\5j\u076b\nj\3j\3j\5j\u076f\nj\3j\3j\5j\u0773\nj"+
		"\3j\3j\5j\u0777\nj\3j\5j\u077a\nj\3k\3k\3l\3l\3m\3m\3m\2\6\62\u0092\u0096"+
		"\u009an\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:"+
		"<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a"+
		"\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2"+
		"\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba"+
		"\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2"+
		"\u00d4\u00d6\u00d8\2,\7\2((\64\64ZZgg\u0092\u0092\6\2UU||\u00e2\u00e2"+
		"\u010c\u010c\5\2UU\u00e2\u00e2\u010c\u010c\4\2\25\25HH\4\2bb\u0083\u0083"+
		"\4\2\u00f2\u00f2\u0116\u0116\4\2\u0138\u0138\u013c\u013c\4\2\u00f1\u00f1"+
		"\u00fb\u00fb\4\2\67\67\u00d5\u00d5\4\2\f\fMM\4\2\u013c\u013c\u013e\u013e"+
		"\3\2\u0088\u0089\5\2\f\f\21\21\u00e6\u00e6\5\2]]\u0105\u0105\u010e\u010e"+
		"\4\2\u012a\u012b\u012f\u012f\4\2OO\u012c\u012e\4\2\u012a\u012b\u0132\u0132"+
		"\t\2=>qq\u0097\u009a\u00bf\u00bf\u00d8\u00d8\u0119\u0119\u011f\u011f\4"+
		"\299;<\4\2BB\u00fc\u00fc\4\2CC\u00fd\u00fd\4\2  \u0107\u0107\4\2ss\u00cd"+
		"\u00cd\3\2\u00ee\u00ef\4\2\6\6hh\4\2\6\6dd\5\2\31\31\u0086\u0086\u0100"+
		"\u0100\3\2\u00b4\u00b5\3\2\u0122\u0129\4\2]]\u0105\u0105\3\2\u012a\u012b"+
		"\5\2\u0138\u0138\u013c\u013c\u013e\u013e\4\2\u009a\u009a\u011f\u011f\6"+
		"\2==qq\u0099\u0099\u00d8\u00d8\5\2qq\u0099\u0099\u00d8\u00d8\4\2NN\u00b1"+
		"\u00b1\4\2\u00a9\u00a9\u00e7\u00e7\4\2cc\u00ba\u00ba\3\2\u013d\u013e\65"+
		"\2\n\13\r\16\20\20\22\23\25\26\30\30\32\36!#%(**,\62\64\64\678=LNPTTV"+
		"\\__acfgjlooqtvwy{}}\u0080\u0080\u0082\u0083\u0085\u0085\u0088\u009d\u009f"+
		"\u009f\u00a2\u00a3\u00a6\u00a7\u00aa\u00aa\u00ac\u00ad\u00af\u00b3\u00b6"+
		"\u00ba\u00bc\u00c5\u00c7\u00cf\u00d1\u00da\u00dc\u00df\u00e1\u00e5\u00e7"+
		"\u00f2\u00f4\u00f8\u00fb\u00fd\u00ff\u00ff\u0101\u010b\u010f\u0112\u0115"+
		"\u0119\u011c\u011c\u011f\u0120\22\2\20\20\66\66UUiixx||\u0081\u0081\u0084"+
		"\u0084\u0087\u0087\u009e\u009e\u00a4\u00a4\u00d0\u00d0\u00dc\u00dc\u00e2"+
		"\u00e2\u010c\u010c\u0114\u0114\23\2\n\17\21\65\67TVhjwy{}\u0080\u0082"+
		"\u0083\u0085\u0086\u0088\u009d\u009f\u00a3\u00a5\u00cf\u00d1\u00db\u00dd"+
		"\u00e1\u00e3\u010b\u010d\u0113\u0115\u0120\2\u0876\2\u00da\3\2\2\2\4\u00f4"+
		"\3\2\2\2\6\u00f6\3\2\2\2\b\u010e\3\2\2\2\n\u0110\3\2\2\2\f\u0114\3\2\2"+
		"\2\16\u0120\3\2\2\2\20\u012d\3\2\2\2\22\u0130\3\2\2\2\24\u0134\3\2\2\2"+
		"\26\u0139\3\2\2\2\30\u0142\3\2\2\2\32\u014d\3\2\2\2\34\u015f\3\2\2\2\36"+
		"\u0162\3\2\2\2 \u016d\3\2\2\2\"\u017d\3\2\2\2$\u0183\3\2\2\2&\u0185\3"+
		"\2\2\2(\u0190\3\2\2\2*\u01a1\3\2\2\2,\u01a8\3\2\2\2.\u01aa\3\2\2\2\60"+
		"\u01ba\3\2\2\2\62\u01ea\3\2\2\2\64\u0210\3\2\2\2\66\u0212\3\2\2\28\u021a"+
		"\3\2\2\2:\u023b\3\2\2\2<\u0269\3\2\2\2>\u027e\3\2\2\2@\u029e\3\2\2\2B"+
		"\u02aa\3\2\2\2D\u02ad\3\2\2\2F\u02b0\3\2\2\2H\u02ca\3\2\2\2J\u02cc\3\2"+
		"\2\2L\u02ec\3\2\2\2N\u0315\3\2\2\2P\u0319\3\2\2\2R\u0334\3\2\2\2T\u0338"+
		"\3\2\2\2V\u0347\3\2\2\2X\u0349\3\2\2\2Z\u0367\3\2\2\2\\\u0369\3\2\2\2"+
		"^\u0370\3\2\2\2`\u0390\3\2\2\2b\u0393\3\2\2\2d\u03ad\3\2\2\2f\u03c7\3"+
		"\2\2\2h\u03cd\3\2\2\2j\u03cf\3\2\2\2l\u03f3\3\2\2\2n\u03f5\3\2\2\2p\u03f9"+
		"\3\2\2\2r\u0401\3\2\2\2t\u040c\3\2\2\2v\u042b\3\2\2\2x\u042d\3\2\2\2z"+
		"\u0438\3\2\2\2|\u044e\3\2\2\2~\u0481\3\2\2\2\u0080\u0483\3\2\2\2\u0082"+
		"\u048b\3\2\2\2\u0084\u0495\3\2\2\2\u0086\u049d\3\2\2\2\u0088\u04aa\3\2"+
		"\2\2\u008a\u04b9\3\2\2\2\u008c\u04bd\3\2\2\2\u008e\u04bf\3\2\2\2\u0090"+
		"\u04c1\3\2\2\2\u0092\u04d5\3\2\2\2\u0094\u0534\3\2\2\2\u0096\u053a\3\2"+
		"\2\2\u0098\u0554\3\2\2\2\u009a\u0633\3\2\2\2\u009c\u064e\3\2\2\2\u009e"+
		"\u0650\3\2\2\2\u00a0\u0652\3\2\2\2\u00a2\u0654\3\2\2\2\u00a4\u0659\3\2"+
		"\2\2\u00a6\u0660\3\2\2\2\u00a8\u0664\3\2\2\2\u00aa\u0669\3\2\2\2\u00ac"+
		"\u066f\3\2\2\2\u00ae\u06a1\3\2\2\2\u00b0\u06a3\3\2\2\2\u00b2\u06ab\3\2"+
		"\2\2\u00b4\u06b4\3\2\2\2\u00b6\u06bc\3\2\2\2\u00b8\u06c8\3\2\2\2\u00ba"+
		"\u06cd\3\2\2\2\u00bc\u06d6\3\2\2\2\u00be\u0708\3\2\2\2\u00c0\u071a\3\2"+
		"\2\2\u00c2\u0723\3\2\2\2\u00c4\u0729\3\2\2\2\u00c6\u072b\3\2\2\2\u00c8"+
		"\u0733\3\2\2\2\u00ca\u073d\3\2\2\2\u00cc\u0742\3\2\2\2\u00ce\u074a\3\2"+
		"\2\2\u00d0\u074c\3\2\2\2\u00d2\u0779\3\2\2\2\u00d4\u077b\3\2\2\2\u00d6"+
		"\u077d\3\2\2\2\u00d8\u077f\3\2\2\2\u00da\u00de\5\4\3\2\u00db\u00dd\7\3"+
		"\2\2\u00dc\u00db\3\2\2\2\u00dd\u00e0\3\2\2\2\u00de\u00dc\3\2\2\2\u00de"+
		"\u00df\3\2\2\2\u00df\u00e1\3\2\2\2\u00e0\u00de\3\2\2\2\u00e1\u00e2\7\2"+
		"\2\3\u00e2\3\3\2\2\2\u00e3\u00e4\5\6\4\2\u00e4\u00e6\5\b\5\2\u00e5\u00e7"+
		"\5\32\16\2\u00e6\u00e5\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7\u00e8\3\2\2\2"+
		"\u00e8\u00ed\5\34\17\2\u00e9\u00eb\7\24\2\2\u00ea\u00e9\3\2\2\2\u00ea"+
		"\u00eb\3\2\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00ee\5\24\13\2\u00ed\u00ea\3"+
		"\2\2\2\u00ed\u00ee\3\2\2\2\u00ee\u00f5\3\2\2\2\u00ef\u00f1\7X\2\2\u00f0"+
		"\u00f2\t\2\2\2\u00f1\u00f0\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f3\3\2"+
		"\2\2\u00f3\u00f5\5\4\3\2\u00f4\u00e3\3\2\2\2\u00f4\u00ef\3\2\2\2\u00f5"+
		"\5\3\2\2\2\u00f6\u00f8\7\65\2\2\u00f7\u00f9\7\u00f7\2\2\u00f8\u00f7\3"+
		"\2\2\2\u00f8\u00f9\3\2\2\2\u00f9\u00fb\3\2\2\2\u00fa\u00fc\7[\2\2\u00fb"+
		"\u00fa\3\2\2\2\u00fb\u00fc\3\2\2\2\u00fc\u00fd\3\2\2\2\u00fd\u0101\7\u00f3"+
		"\2\2\u00fe\u00ff\7r\2\2\u00ff\u0100\7\u00a0\2\2\u0100\u0102\7W\2\2\u0101"+
		"\u00fe\3\2\2\2\u0101\u0102\3\2\2\2\u0102\u0103\3\2\2\2\u0103\u0104\5\u0080"+
		"A\2\u0104\7\3\2\2\2\u0105\u0106\7\4\2\2\u0106\u0109\5\u00b0Y\2\u0107\u0108"+
		"\7\6\2\2\u0108\u010a\5\n\6\2\u0109\u0107\3\2\2\2\u0109\u010a\3\2\2\2\u010a"+
		"\u010b\3\2\2\2\u010b\u010c\7\5\2\2\u010c\u010f\3\2\2\2\u010d\u010f\5\n"+
		"\6\2\u010e\u0105\3\2\2\2\u010e\u010d\3\2\2\2\u010f\t\3\2\2\2\u0110\u0111"+
		"\7\u00bb\2\2\u0111\u0112\7\u0121\2\2\u0112\u0113\5n8\2\u0113\13\3\2\2"+
		"\2\u0114\u0115\7\'\2\2\u0115\u0116\7\34\2\2\u0116\u011a\5n8\2\u0117\u0118"+
		"\7\u00e8\2\2\u0118\u0119\7\34\2\2\u0119\u011b\5r:\2\u011a\u0117\3\2\2"+
		"\2\u011a\u011b\3\2\2\2\u011b\u011c\3\2\2\2\u011c\u011d\7~\2\2\u011d\u011e"+
		"\7\u013c\2\2\u011e\u011f\7\33\2\2\u011f\r\3\2\2\2\u0120\u0121\7\u00e5"+
		"\2\2\u0121\u0122\7\34\2\2\u0122\u0123\5n8\2\u0123\u0126\7\u00a4\2\2\u0124"+
		"\u0127\5&\24\2\u0125\u0127\5(\25\2\u0126\u0124\3\2\2\2\u0126\u0125\3\2"+
		"\2\2\u0127\u012b\3\2\2\2\u0128\u0129\7\u00eb\2\2\u0129\u012a\7\24\2\2"+
		"\u012a\u012c\7K\2\2\u012b\u0128\3\2\2\2\u012b\u012c\3\2\2\2\u012c\17\3"+
		"\2\2\2\u012d\u012e\7\u008f\2\2\u012e\u012f\7\u0138\2\2\u012f\21\3\2\2"+
		"\2\u0130\u0131\7-\2\2\u0131\u0132\7\u0138\2\2\u0132\23\3\2\2\2\u0133\u0135"+
		"\5\26\f\2\u0134\u0133\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0136\3\2\2\2"+
		"\u0136\u0137\5\62\32\2\u0137\u0138\5\60\31\2\u0138\25\3\2\2\2\u0139\u013a"+
		"\7\u011d\2\2\u013a\u013f\5\30\r\2\u013b\u013c\7\6\2\2\u013c\u013e\5\30"+
		"\r\2\u013d\u013b\3\2\2\2\u013e\u0141\3\2\2\2\u013f\u013d\3\2\2\2\u013f"+
		"\u0140\3\2\2\2\u0140\27\3\2\2\2\u0141\u013f\3\2\2\2\u0142\u0144\5\u00c8"+
		"e\2\u0143\u0145\5n8\2\u0144\u0143\3\2\2\2\u0144\u0145\3\2\2\2\u0145\u0147"+
		"\3\2\2\2\u0146\u0148\7\24\2\2\u0147\u0146\3\2\2\2\u0147\u0148\3\2\2\2"+
		"\u0148\u0149\3\2\2\2\u0149\u014a\7\4\2\2\u014a\u014b\5\24\13\2\u014b\u014c"+
		"\7\5\2\2\u014c\31\3\2\2\2\u014d\u014e\7\u0114\2\2\u014e\u014f\5\u0080"+
		"A\2\u014f\33\3\2\2\2\u0150\u0151\7\u00a7\2\2\u0151\u015e\5\36\20\2\u0152"+
		"\u0153\7\u00b2\2\2\u0153\u0154\7\34\2\2\u0154\u015e\5\u0086D\2\u0155\u015e"+
		"\5\16\b\2\u0156\u015e\5\f\7\2\u0157\u015e\5~@\2\u0158\u015e\5*\26\2\u0159"+
		"\u015e\5\20\t\2\u015a\u015e\5\22\n\2\u015b\u015c\7\u00f6\2\2\u015c\u015e"+
		"\5\36\20\2\u015d\u0150\3\2\2\2\u015d\u0152\3\2\2\2\u015d\u0155\3\2\2\2"+
		"\u015d\u0156\3\2\2\2\u015d\u0157\3\2\2\2\u015d\u0158\3\2\2\2\u015d\u0159"+
		"\3\2\2\2\u015d\u015a\3\2\2\2\u015d\u015b\3\2\2\2\u015e\u0161\3\2\2\2\u015f"+
		"\u015d\3\2\2\2\u015f\u0160\3\2\2\2\u0160\35\3\2\2\2\u0161\u015f\3\2\2"+
		"\2\u0162\u0163\7\4\2\2\u0163\u0168\5 \21\2\u0164\u0165\7\6\2\2\u0165\u0167"+
		"\5 \21\2\u0166\u0164\3\2\2\2\u0167\u016a\3\2\2\2\u0168\u0166\3\2\2\2\u0168"+
		"\u0169\3\2\2\2\u0169\u016b\3\2\2\2\u016a\u0168\3\2\2\2\u016b\u016c\7\5"+
		"\2\2\u016c\37\3\2\2\2\u016d\u0172\5\"\22\2\u016e\u0170\7\u0122\2\2\u016f"+
		"\u016e\3\2\2\2\u016f\u0170\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u0173\5$"+
		"\23\2\u0172\u016f\3\2\2\2\u0172\u0173\3\2\2\2\u0173!\3\2\2\2\u0174\u0179"+
		"\5\u00ccg\2\u0175\u0176\7\7\2\2\u0176\u0178\5\u00ccg\2\u0177\u0175\3\2"+
		"\2\2\u0178\u017b\3\2\2\2\u0179\u0177\3\2\2\2\u0179\u017a\3\2\2\2\u017a"+
		"\u017e\3\2\2\2\u017b\u0179\3\2\2\2\u017c\u017e\7\u0138\2\2\u017d\u0174"+
		"\3\2\2\2\u017d\u017c\3\2\2\2\u017e#\3\2\2\2\u017f\u0184\7\u013c\2\2\u0180"+
		"\u0184\7\u013e\2\2\u0181\u0184\5\u00a0Q\2\u0182\u0184\7\u0138\2\2\u0183"+
		"\u017f\3\2\2\2\u0183\u0180\3\2\2\2\u0183\u0181\3\2\2\2\u0183\u0182\3\2"+
		"\2\2\u0184%\3\2\2\2\u0185\u0186\7\4\2\2\u0186\u018b\5\u009cO\2\u0187\u0188"+
		"\7\6\2\2\u0188\u018a\5\u009cO\2\u0189\u0187\3\2\2\2\u018a\u018d\3\2\2"+
		"\2\u018b\u0189\3\2\2\2\u018b\u018c\3\2\2\2\u018c\u018e\3\2\2\2\u018d\u018b"+
		"\3\2\2\2\u018e\u018f\7\5\2\2\u018f\'\3\2\2\2\u0190\u0191\7\4\2\2\u0191"+
		"\u0196\5&\24\2\u0192\u0193\7\6\2\2\u0193\u0195\5&\24\2\u0194\u0192\3\2"+
		"\2\2\u0195\u0198\3\2\2\2\u0196\u0194\3\2\2\2\u0196\u0197\3\2\2\2\u0197"+
		"\u0199\3\2\2\2\u0198\u0196\3\2\2\2\u0199\u019a\7\5\2\2\u019a)\3\2\2\2"+
		"\u019b\u019c\7\u00eb\2\2\u019c\u019d\7\24\2\2\u019d\u01a2\5,\27\2\u019e"+
		"\u019f\7\u00eb\2\2\u019f\u01a0\7\34\2\2\u01a0\u01a2\5.\30\2\u01a1\u019b"+
		"\3\2\2\2\u01a1\u019e\3\2\2\2\u01a2+\3\2\2\2\u01a3\u01a4\7z\2\2\u01a4\u01a5"+
		"\7\u0138\2\2\u01a5\u01a6\7\u00ac\2\2\u01a6\u01a9\7\u0138\2\2\u01a7\u01a9"+
		"\5\u00ccg\2\u01a8\u01a3\3\2\2\2\u01a8\u01a7\3\2\2\2\u01a9-\3\2\2\2\u01aa"+
		"\u01ae\7\u0138\2\2\u01ab\u01ac\7\u011d\2\2\u01ac\u01ad\7\u00df\2\2\u01ad"+
		"\u01af\5\36\20\2\u01ae\u01ab\3\2\2\2\u01ae\u01af\3\2\2\2\u01af/\3\2\2"+
		"\2\u01b0\u01b1\7\u00a9\2\2\u01b1\u01b2\7\34\2\2\u01b2\u01b7\5\66\34\2"+
		"\u01b3\u01b4\7\6\2\2\u01b4\u01b6\5\66\34\2\u01b5\u01b3\3\2\2\2\u01b6\u01b9"+
		"\3\2\2\2\u01b7\u01b5\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8\u01bb\3\2\2\2\u01b9"+
		"\u01b7\3\2\2\2\u01ba\u01b0\3\2\2\2\u01ba\u01bb\3\2\2\2\u01bb\u01c6\3\2"+
		"\2\2\u01bc\u01bd\7&\2\2\u01bd\u01be\7\34\2\2\u01be\u01c3\5\u008eH\2\u01bf"+
		"\u01c0\7\6\2\2\u01c0\u01c2\5\u008eH\2\u01c1\u01bf\3\2\2\2\u01c2\u01c5"+
		"\3\2\2\2\u01c3\u01c1\3\2\2\2\u01c3\u01c4\3\2\2\2\u01c4\u01c7\3\2\2\2\u01c5"+
		"\u01c3\3\2\2\2\u01c6\u01bc\3\2\2\2\u01c6\u01c7\3\2\2\2\u01c7\u01d2\3\2"+
		"\2\2\u01c8\u01c9\7N\2\2\u01c9\u01ca\7\34\2\2\u01ca\u01cf\5\u008eH\2\u01cb"+
		"\u01cc\7\6\2\2\u01cc\u01ce\5\u008eH\2\u01cd\u01cb\3\2\2\2\u01ce\u01d1"+
		"\3\2\2\2\u01cf\u01cd\3\2\2\2\u01cf\u01d0\3\2\2\2\u01d0\u01d3\3\2\2\2\u01d1"+
		"\u01cf\3\2\2\2\u01d2\u01c8\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3\u01de\3\2"+
		"\2\2\u01d4\u01d5\7\u00e7\2\2\u01d5\u01d6\7\34\2\2\u01d6\u01db\5\66\34"+
		"\2\u01d7\u01d8\7\6\2\2\u01d8\u01da\5\66\34\2\u01d9\u01d7\3\2\2\2\u01da"+
		"\u01dd\3\2\2\2\u01db\u01d9\3\2\2\2\u01db\u01dc\3\2\2\2\u01dc\u01df\3\2"+
		"\2\2\u01dd\u01db\3\2\2\2\u01de\u01d4\3\2\2\2\u01de\u01df\3\2\2\2\u01df"+
		"\u01e1\3\2\2\2\u01e0\u01e2\5\u00ba^\2\u01e1\u01e0\3\2\2\2\u01e1\u01e2"+
		"\3\2\2\2\u01e2\u01e8\3\2\2\2\u01e3\u01e6\7\u008a\2\2\u01e4\u01e7\7\f\2"+
		"\2\u01e5\u01e7\5\u008eH\2\u01e6\u01e4\3\2\2\2\u01e6\u01e5\3\2\2\2\u01e7"+
		"\u01e9\3\2\2\2\u01e8\u01e3\3\2\2\2\u01e8\u01e9\3\2\2\2\u01e9\61\3\2\2"+
		"\2\u01ea\u01eb\b\32\1\2\u01eb\u01ec\5\64\33\2\u01ec\u0204\3\2\2\2\u01ed"+
		"\u01ee\f\5\2\2\u01ee\u01ef\6\32\3\2\u01ef\u01f1\t\3\2\2\u01f0\u01f2\5"+
		"`\61\2\u01f1\u01f0\3\2\2\2\u01f1\u01f2\3\2\2\2\u01f2\u01f3\3\2\2\2\u01f3"+
		"\u0203\5\62\32\6\u01f4\u01f5\f\4\2\2\u01f5\u01f6\6\32\5\2\u01f6\u01f8"+
		"\7|\2\2\u01f7\u01f9\5`\61\2\u01f8\u01f7\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9"+
		"\u01fa\3\2\2\2\u01fa\u0203\5\62\32\5\u01fb\u01fc\f\3\2\2\u01fc\u01fd\6"+
		"\32\7\2\u01fd\u01ff\t\4\2\2\u01fe\u0200\5`\61\2\u01ff\u01fe\3\2\2\2\u01ff"+
		"\u0200\3\2\2\2\u0200\u0201\3\2\2\2\u0201\u0203\5\62\32\4\u0202\u01ed\3"+
		"\2\2\2\u0202\u01f4\3\2\2\2\u0202\u01fb\3\2\2\2\u0203\u0206\3\2\2\2\u0204"+
		"\u0202\3\2\2\2\u0204\u0205\3\2\2\2\u0205\63\3\2\2\2\u0206\u0204\3\2\2"+
		"\2\u0207\u0211\5<\37\2\u0208\u0211\58\35\2\u0209\u020a\7\u00f3\2\2\u020a"+
		"\u0211\5\u0080A\2\u020b\u0211\5x=\2\u020c\u020d\7\4\2\2\u020d\u020e\5"+
		"\24\13\2\u020e\u020f\7\5\2\2\u020f\u0211\3\2\2\2\u0210\u0207\3\2\2\2\u0210"+
		"\u0208\3\2\2\2\u0210\u0209\3\2\2\2\u0210\u020b\3\2\2\2\u0210\u020c\3\2"+
		"\2\2\u0211\65\3\2\2\2\u0212\u0214\5\u008eH\2\u0213\u0215\t\5\2\2\u0214"+
		"\u0213\3\2\2\2\u0214\u0215\3\2\2\2\u0215\u0218\3\2\2\2\u0216\u0217\7\u00a2"+
		"\2\2\u0217\u0219\t\6\2\2\u0218\u0216\3\2\2\2\u0218\u0219\3\2\2\2\u0219"+
		"\67\3\2\2\2\u021a\u021c\5J&\2\u021b\u021d\5:\36\2\u021c\u021b\3\2\2\2"+
		"\u021d\u021e\3\2\2\2\u021e\u021c\3\2\2\2\u021e\u021f\3\2\2\2\u021f9\3"+
		"\2\2\2\u0220\u0222\5> \2\u0221\u0223\5B\"\2\u0222\u0221\3\2\2\2\u0222"+
		"\u0223\3\2\2\2\u0223\u0224\3\2\2\2\u0224\u0225\5\60\31\2\u0225\u023c\3"+
		"\2\2\2\u0226\u022a\5@!\2\u0227\u0229\5^\60\2\u0228\u0227\3\2\2\2\u0229"+
		"\u022c\3\2\2\2\u022a\u0228\3\2\2\2\u022a\u022b\3\2\2\2\u022b\u022e\3\2"+
		"\2\2\u022c\u022a\3\2\2\2\u022d\u022f\5B\"\2\u022e\u022d\3\2\2\2\u022e"+
		"\u022f\3\2\2\2\u022f\u0231\3\2\2\2\u0230\u0232\5N(\2\u0231\u0230\3\2\2"+
		"\2\u0231\u0232\3\2\2\2\u0232\u0234\3\2\2\2\u0233\u0235\5D#\2\u0234\u0233"+
		"\3\2\2\2\u0234\u0235\3\2\2\2\u0235\u0237\3\2\2\2\u0236\u0238\5\u00ba^"+
		"\2\u0237\u0236\3\2\2\2\u0237\u0238\3\2\2\2\u0238\u0239\3\2\2\2\u0239\u023a"+
		"\5\60\31\2\u023a\u023c\3\2\2\2\u023b\u0220\3\2\2\2\u023b\u0226\3\2\2\2"+
		"\u023c;\3\2\2\2\u023d\u023f\5> \2\u023e\u0240\5J&\2\u023f\u023e\3\2\2"+
		"\2\u023f\u0240\3\2\2\2\u0240\u0244\3\2\2\2\u0241\u0243\5^\60\2\u0242\u0241"+
		"\3\2\2\2\u0243\u0246\3\2\2\2\u0244\u0242\3\2\2\2\u0244\u0245\3\2\2\2\u0245"+
		"\u0248\3\2\2\2\u0246\u0244\3\2\2\2\u0247\u0249\5B\"\2\u0248\u0247\3\2"+
		"\2\2\u0248\u0249\3\2\2\2\u0249\u024b\3\2\2\2\u024a\u024c\5N(\2\u024b\u024a"+
		"\3\2\2\2\u024b\u024c\3\2\2\2\u024c\u024e\3\2\2\2\u024d\u024f\5D#\2\u024e"+
		"\u024d\3\2\2\2\u024e\u024f\3\2\2\2\u024f\u0251\3\2\2\2\u0250\u0252\5\u00ba"+
		"^\2\u0251\u0250\3\2\2\2\u0251\u0252\3\2\2\2\u0252\u026a\3\2\2\2\u0253"+
		"\u0255\5@!\2\u0254\u0256\5J&\2\u0255\u0254\3\2\2\2\u0255\u0256\3\2\2\2"+
		"\u0256\u025a\3\2\2\2\u0257\u0259\5^\60\2\u0258\u0257\3\2\2\2\u0259\u025c"+
		"\3\2\2\2\u025a\u0258\3\2\2\2\u025a\u025b\3\2\2\2\u025b\u025e\3\2\2\2\u025c"+
		"\u025a\3\2\2\2\u025d\u025f\5B\"\2\u025e\u025d\3\2\2\2\u025e\u025f\3\2"+
		"\2\2\u025f\u0261\3\2\2\2\u0260\u0262\5N(\2\u0261\u0260\3\2\2\2\u0261\u0262"+
		"\3\2\2\2\u0262\u0264\3\2\2\2\u0263\u0265\5D#\2\u0264\u0263\3\2\2\2\u0264"+
		"\u0265\3\2\2\2\u0265\u0267\3\2\2\2\u0266\u0268\5\u00ba^\2\u0267\u0266"+
		"\3\2\2\2\u0267\u0268\3\2\2\2\u0268\u026a\3\2\2\2\u0269\u023d\3\2\2\2\u0269"+
		"\u0253\3\2\2\2\u026a=\3\2\2\2\u026b\u026c\7\u00db\2\2\u026c\u026d\7\u0103"+
		"\2\2\u026d\u026f\7\4\2\2\u026e\u0270\5`\61\2\u026f\u026e\3\2\2\2\u026f"+
		"\u0270\3\2\2\2\u0270\u0271\3\2\2\2\u0271\u0272\5\u0090I\2\u0272\u0273"+
		"\7\5\2\2\u0273\u027f\3\2\2\2\u0274\u0276\7\u0094\2\2\u0275\u0277\5`\61"+
		"\2\u0276\u0275\3\2\2\2\u0276\u0277\3\2\2\2\u0277\u0278\3\2\2\2\u0278\u027f"+
		"\5\u0090I\2\u0279\u027b\7\u00c5\2\2\u027a\u027c\5`\61\2\u027b\u027a\3"+
		"\2\2\2\u027b\u027c\3\2\2\2\u027c\u027d\3\2\2\2\u027d\u027f\5\u0090I\2"+
		"\u027e\u026b\3\2\2\2\u027e\u0274\3\2\2\2\u027e\u0279\3\2\2\2\u027f\u0281"+
		"\3\2\2\2\u0280\u0282\5~@\2\u0281\u0280\3\2\2\2\u0281\u0282\3\2\2\2\u0282"+
		"\u0285\3\2\2\2\u0283\u0284\7\u00c3\2\2\u0284\u0286\7\u0138\2\2\u0285\u0283"+
		"\3\2\2\2\u0285\u0286\3\2\2\2\u0286\u0287\3\2\2\2\u0287\u0288\7\u0114\2"+
		"\2\u0288\u0295\7\u0138\2\2\u0289\u0293\7\24\2\2\u028a\u0294\5p9\2\u028b"+
		"\u0294\5\u00b0Y\2\u028c\u028f\7\4\2\2\u028d\u0290\5p9\2\u028e\u0290\5"+
		"\u00b0Y\2\u028f\u028d\3\2\2\2\u028f\u028e\3\2\2\2\u0290\u0291\3\2\2\2"+
		"\u0291\u0292\7\5\2\2\u0292\u0294\3\2\2\2\u0293\u028a\3\2\2\2\u0293\u028b"+
		"\3\2\2\2\u0293\u028c\3\2\2\2\u0294\u0296\3\2\2\2\u0295\u0289\3\2\2\2\u0295"+
		"\u0296\3\2\2\2\u0296\u0298\3\2\2\2\u0297\u0299\5~@\2\u0298\u0297\3\2\2"+
		"\2\u0298\u0299\3\2\2\2\u0299\u029c\3\2\2\2\u029a\u029b\7\u00c2\2\2\u029b"+
		"\u029d\7\u0138\2\2\u029c\u029a\3\2\2\2\u029c\u029d\3\2\2\2\u029d?\3\2"+
		"\2\2\u029e\u02a2\7\u00db\2\2\u029f\u02a1\5F$\2\u02a0\u029f\3\2\2\2\u02a1"+
		"\u02a4\3\2\2\2\u02a2\u02a0\3\2\2\2\u02a2\u02a3\3\2\2\2\u02a3\u02a6\3\2"+
		"\2\2\u02a4\u02a2\3\2\2\2\u02a5\u02a7\5`\61\2\u02a6\u02a5\3\2\2\2\u02a6"+
		"\u02a7\3\2\2\2\u02a7\u02a8\3\2\2\2\u02a8\u02a9\5\u0084C\2\u02a9A\3\2\2"+
		"\2\u02aa\u02ab\7\u011b\2\2\u02ab\u02ac\5\u0092J\2\u02acC\3\2\2\2\u02ad"+
		"\u02ae\7p\2\2\u02ae\u02af\5\u0092J\2\u02afE\3\2\2\2\u02b0\u02b1\7\u0136"+
		"\2\2\u02b1\u02b8\5H%\2\u02b2\u02b4\7\6\2\2\u02b3\u02b2\3\2\2\2\u02b3\u02b4"+
		"\3\2\2\2\u02b4\u02b5\3\2\2\2\u02b5\u02b7\5H%\2\u02b6\u02b3\3\2\2\2\u02b7"+
		"\u02ba\3\2\2\2\u02b8\u02b6\3\2\2\2\u02b8\u02b9\3\2\2\2\u02b9\u02bb\3\2"+
		"\2\2\u02ba\u02b8\3\2\2\2\u02bb\u02bc\7\u0137\2\2\u02bcG\3\2\2\2\u02bd"+
		"\u02cb\5\u00ccg\2\u02be\u02bf\5\u00ccg\2\u02bf\u02c0\7\4\2\2\u02c0\u02c5"+
		"\5\u009aN\2\u02c1\u02c2\7\6\2\2\u02c2\u02c4\5\u009aN\2\u02c3\u02c1\3\2"+
		"\2\2\u02c4\u02c7\3\2\2\2\u02c5\u02c3\3\2\2\2\u02c5\u02c6\3\2\2\2\u02c6"+
		"\u02c8\3\2\2\2\u02c7\u02c5\3\2\2\2\u02c8\u02c9\7\5\2\2\u02c9\u02cb\3\2"+
		"\2\2\u02ca\u02bd\3\2\2\2\u02ca\u02be\3\2\2\2\u02cbI\3\2\2\2\u02cc\u02cd"+
		"\7h\2\2\u02cd\u02d2\5b\62\2\u02ce\u02cf\7\6\2\2\u02cf\u02d1\5b\62\2\u02d0"+
		"\u02ce\3\2\2\2\u02d1\u02d4\3\2\2\2\u02d2\u02d0\3\2\2\2\u02d2\u02d3\3\2"+
		"\2\2\u02d3\u02d8\3\2\2\2\u02d4\u02d2\3\2\2\2\u02d5\u02d7\5^\60\2\u02d6"+
		"\u02d5\3\2\2\2\u02d7\u02da\3\2\2\2\u02d8\u02d6\3\2\2\2\u02d8\u02d9\3\2"+
		"\2\2\u02d9\u02dc\3\2\2\2\u02da\u02d8\3\2\2\2\u02db\u02dd\5X-\2\u02dc\u02db"+
		"\3\2\2\2\u02dc\u02dd\3\2\2\2\u02ddK\3\2\2\2\u02de\u02e0\7d\2\2\u02df\u02de"+
		"\3\2\2\2\u02df\u02e0\3\2\2\2\u02e0\u02e1\3\2\2\2\u02e1\u02e2\t\7\2\2\u02e2"+
		"\u02e3\7\24\2\2\u02e3\u02e4\7\u00a3\2\2\u02e4\u02ed\t\b\2\2\u02e5\u02e7"+
		"\7d\2\2\u02e6\u02e5\3\2\2\2\u02e6\u02e7\3\2\2\2\u02e7\u02e8\3\2\2\2\u02e8"+
		"\u02e9\t\t\2\2\u02e9\u02ea\7\24\2\2\u02ea\u02eb\7\u00a3\2\2\u02eb\u02ed"+
		"\5\u0096L\2\u02ec\u02df\3\2\2\2\u02ec\u02e6\3\2\2\2\u02edM\3\2\2\2\u02ee"+
		"\u02ef\7n\2\2\u02ef\u02f0\7\34\2\2\u02f0\u02f5\5P)\2\u02f1\u02f2\7\6\2"+
		"\2\u02f2\u02f4\5P)\2\u02f3\u02f1\3\2\2\2\u02f4\u02f7\3\2\2\2\u02f5\u02f3"+
		"\3\2\2\2\u02f5\u02f6\3\2\2\2\u02f6\u0316\3\2\2\2\u02f7\u02f5\3\2\2\2\u02f8"+
		"\u02f9\7n\2\2\u02f9\u02fa\7\34\2\2\u02fa\u02ff\5\u008eH\2\u02fb\u02fc"+
		"\7\6\2\2\u02fc\u02fe\5\u008eH\2\u02fd\u02fb\3\2\2\2\u02fe\u0301\3\2\2"+
		"\2\u02ff\u02fd\3\2\2\2\u02ff\u0300\3\2\2\2\u0300\u0313\3\2\2\2\u0301\u02ff"+
		"\3\2\2\2\u0302\u0303\7\u011d\2\2\u0303\u0314\7\u00d5\2\2\u0304\u0305\7"+
		"\u011d\2\2\u0305\u0314\7\67\2\2\u0306\u0307\7o\2\2\u0307\u0308\7\u00e3"+
		"\2\2\u0308\u0309\7\4\2\2\u0309\u030e\5V,\2\u030a\u030b\7\6\2\2\u030b\u030d"+
		"\5V,\2\u030c\u030a\3\2\2\2\u030d\u0310\3\2\2\2\u030e\u030c\3\2\2\2\u030e"+
		"\u030f\3\2\2\2\u030f\u0311\3\2\2\2\u0310\u030e\3\2\2\2\u0311\u0312\7\5"+
		"\2\2\u0312\u0314\3\2\2\2\u0313\u0302\3\2\2\2\u0313\u0304\3\2\2\2\u0313"+
		"\u0306\3\2\2\2\u0313\u0314\3\2\2\2\u0314\u0316\3\2\2\2\u0315\u02ee\3\2"+
		"\2\2\u0315\u02f8\3\2\2\2\u0316O\3\2\2\2\u0317\u031a\5R*\2\u0318\u031a"+
		"\5\u008eH\2\u0319\u0317\3\2\2\2\u0319\u0318\3\2\2\2\u031aQ\3\2\2\2\u031b"+
		"\u031c\t\n\2\2\u031c\u031d\7\4\2\2\u031d\u0322\5V,\2\u031e\u031f\7\6\2"+
		"\2\u031f\u0321\5V,\2\u0320\u031e\3\2\2\2\u0321\u0324\3\2\2\2\u0322\u0320"+
		"\3\2\2\2\u0322\u0323\3\2\2\2\u0323\u0325\3\2\2\2\u0324\u0322\3\2\2\2\u0325"+
		"\u0326\7\5\2\2\u0326\u0335\3\2\2\2\u0327\u0328\7o\2\2\u0328\u0329\7\u00e3"+
		"\2\2\u0329\u032a\7\4\2\2\u032a\u032f\5T+\2\u032b\u032c\7\6\2\2\u032c\u032e"+
		"\5T+\2\u032d\u032b\3\2\2\2\u032e\u0331\3\2\2\2\u032f\u032d\3\2\2\2\u032f"+
		"\u0330\3\2\2\2\u0330\u0332\3\2\2\2\u0331\u032f\3\2\2\2\u0332\u0333\7\5"+
		"\2\2\u0333\u0335\3\2\2\2\u0334\u031b\3\2\2\2\u0334\u0327\3\2\2\2\u0335"+
		"S\3\2\2\2\u0336\u0339\5R*\2\u0337\u0339\5V,\2\u0338\u0336\3\2\2\2\u0338"+
		"\u0337\3\2\2\2\u0339U\3\2\2\2\u033a\u0343\7\4\2\2\u033b\u0340\5\u008e"+
		"H\2\u033c\u033d\7\6\2\2\u033d\u033f\5\u008eH\2\u033e\u033c\3\2\2\2\u033f"+
		"\u0342\3\2\2\2\u0340\u033e\3\2\2\2\u0340\u0341\3\2\2\2\u0341\u0344\3\2"+
		"\2\2\u0342\u0340\3\2\2\2\u0343\u033b\3\2\2\2\u0343\u0344\3\2\2\2\u0344"+
		"\u0345\3\2\2\2\u0345\u0348\7\5\2\2\u0346\u0348\5\u008eH\2\u0347\u033a"+
		"\3\2\2\2\u0347\u0346\3\2\2\2\u0348W\3\2\2\2\u0349\u034a\7\u00b7\2\2\u034a"+
		"\u034b\7\4\2\2\u034b\u034c\5\u0084C\2\u034c\u034d\7d\2\2\u034d\u034e\5"+
		"Z.\2\u034e\u034f\7u\2\2\u034f\u0350\7\4\2\2\u0350\u0355\5\\/\2\u0351\u0352"+
		"\7\6\2\2\u0352\u0354\5\\/\2\u0353\u0351\3\2\2\2\u0354\u0357\3\2\2\2\u0355"+
		"\u0353\3\2\2\2\u0355\u0356\3\2\2\2\u0356\u0358\3\2\2\2\u0357\u0355\3\2"+
		"\2\2\u0358\u0359\7\5\2\2\u0359\u035a\7\5\2\2\u035aY\3\2\2\2\u035b\u0368"+
		"\5\u00ccg\2\u035c\u035d\7\4\2\2\u035d\u0362\5\u00ccg\2\u035e\u035f\7\6"+
		"\2\2\u035f\u0361\5\u00ccg\2\u0360\u035e\3\2\2\2\u0361\u0364\3\2\2\2\u0362"+
		"\u0360\3\2\2\2\u0362\u0363\3\2\2\2\u0363\u0365\3\2\2\2\u0364\u0362\3\2"+
		"\2\2\u0365\u0366\7\5\2\2\u0366\u0368\3\2\2\2\u0367\u035b\3\2\2\2\u0367"+
		"\u035c\3\2\2\2\u0368[\3\2\2\2\u0369\u036e\5\u008eH\2\u036a\u036c\7\24"+
		"\2\2\u036b\u036a\3\2\2\2\u036b\u036c\3\2\2\2\u036c\u036d\3\2\2\2\u036d"+
		"\u036f\5\u00ccg\2\u036e\u036b\3\2\2\2\u036e\u036f\3\2\2\2\u036f]\3\2\2"+
		"\2\u0370\u0371\7\u0084\2\2\u0371\u0373\7\u0117\2\2\u0372\u0374\7\u00ab"+
		"\2\2\u0373\u0372\3\2\2\2\u0373\u0374\3\2\2\2\u0374\u0375\3\2\2\2\u0375"+
		"\u0376\5\u00c6d\2\u0376\u037f\7\4\2\2\u0377\u037c\5\u008eH\2\u0378\u0379"+
		"\7\6\2\2\u0379\u037b\5\u008eH\2\u037a\u0378\3\2\2\2\u037b\u037e\3\2\2"+
		"\2\u037c\u037a\3\2\2\2\u037c\u037d\3\2\2\2\u037d\u0380\3\2\2\2\u037e\u037c"+
		"\3\2\2\2\u037f\u0377\3\2\2\2\u037f\u0380\3\2\2\2\u0380\u0381\3\2\2\2\u0381"+
		"\u0382\7\5\2\2\u0382\u038e\5\u00ccg\2\u0383\u0385\7\24\2\2\u0384\u0383"+
		"\3\2\2\2\u0384\u0385\3\2\2\2\u0385\u0386\3\2\2\2\u0386\u038b\5\u00ccg"+
		"\2\u0387\u0388\7\6\2\2\u0388\u038a\5\u00ccg\2\u0389\u0387\3\2\2\2\u038a"+
		"\u038d\3\2\2\2\u038b\u0389\3\2\2\2\u038b\u038c\3\2\2\2\u038c\u038f\3\2"+
		"\2\2\u038d\u038b\3\2\2\2\u038e\u0384\3\2\2\2\u038e\u038f\3\2\2\2\u038f"+
		"_\3\2\2\2\u0390\u0391\t\13\2\2\u0391a\3\2\2\2\u0392\u0394\7\u0084\2\2"+
		"\u0393\u0392\3\2\2\2\u0393\u0394\3\2\2\2\u0394\u0395\3\2\2\2\u0395\u0399"+
		"\5v<\2\u0396\u0398\5d\63\2\u0397\u0396\3\2\2\2\u0398\u039b\3\2\2\2\u0399"+
		"\u0397\3\2\2\2\u0399\u039a\3\2\2\2\u039ac\3\2\2\2\u039b\u0399\3\2\2\2"+
		"\u039c\u039d\5f\64\2\u039d\u039f\7\u0081\2\2\u039e\u03a0\7\u0084\2\2\u039f"+
		"\u039e\3\2\2\2\u039f\u03a0\3\2\2\2\u03a0\u03a1\3\2\2\2\u03a1\u03a3\5v"+
		"<\2\u03a2\u03a4\5h\65\2\u03a3\u03a2\3\2\2\2\u03a3\u03a4\3\2\2\2\u03a4"+
		"\u03ae\3\2\2\2\u03a5\u03a6\7\u009e\2\2\u03a6\u03a7\5f\64\2\u03a7\u03a9"+
		"\7\u0081\2\2\u03a8\u03aa\7\u0084\2\2\u03a9\u03a8\3\2\2\2\u03a9\u03aa\3"+
		"\2\2\2\u03aa\u03ab\3\2\2\2\u03ab\u03ac\5v<\2\u03ac\u03ae\3\2\2\2\u03ad"+
		"\u039c\3\2\2\2\u03ad\u03a5\3\2\2\2\u03aee\3\2\2\2\u03af\u03b1\7x\2\2\u03b0"+
		"\u03af\3\2\2\2\u03b0\u03b1\3\2\2\2\u03b1\u03c8\3\2\2\2\u03b2\u03c8\7\66"+
		"\2\2\u03b3\u03b5\7\u0087\2\2\u03b4\u03b6\7\u00ab\2\2\u03b5\u03b4\3\2\2"+
		"\2\u03b5\u03b6\3\2\2\2\u03b6\u03c8\3\2\2\2\u03b7\u03b9\7\u0087\2\2\u03b8"+
		"\u03b7\3\2\2\2\u03b8\u03b9\3\2\2\2\u03b9\u03ba\3\2\2\2\u03ba\u03c8\7\u00dc"+
		"\2\2\u03bb\u03bd\7\u00d0\2\2\u03bc\u03be\7\u00ab\2\2\u03bd\u03bc\3\2\2"+
		"\2\u03bd\u03be\3\2\2\2\u03be\u03c8\3\2\2\2\u03bf\u03c1\7i\2\2\u03c0\u03c2"+
		"\7\u00ab\2\2\u03c1\u03c0\3\2\2\2\u03c1\u03c2\3\2\2\2\u03c2\u03c8\3\2\2"+
		"\2\u03c3\u03c5\7\u0087\2\2\u03c4\u03c3\3\2\2\2\u03c4\u03c5\3\2\2\2\u03c5"+
		"\u03c6\3\2\2\2\u03c6\u03c8\7\20\2\2\u03c7\u03b0\3\2\2\2\u03c7\u03b2\3"+
		"\2\2\2\u03c7\u03b3\3\2\2\2\u03c7\u03b8\3\2\2\2\u03c7\u03bb\3\2\2\2\u03c7"+
		"\u03bf\3\2\2\2\u03c7\u03c4\3\2\2\2\u03c8g\3\2\2\2\u03c9\u03ca\7\u00a4"+
		"\2\2\u03ca\u03ce\5\u0092J\2\u03cb\u03cc\7\u0114\2\2\u03cc\u03ce\5n8\2"+
		"\u03cd\u03c9\3\2\2\2\u03cd\u03cb\3\2\2\2\u03cei\3\2\2\2\u03cf\u03d0\7"+
		"\u00f5\2\2\u03d0\u03d2\7\4\2\2\u03d1\u03d3\5l\67\2\u03d2\u03d1\3\2\2\2"+
		"\u03d2\u03d3\3\2\2\2\u03d3\u03d4\3\2\2\2\u03d4\u03d9\7\5\2\2\u03d5\u03d6"+
		"\7\u00ca\2\2\u03d6\u03d7\7\4\2\2\u03d7\u03d8\7\u013c\2\2\u03d8\u03da\7"+
		"\5\2\2\u03d9\u03d5\3\2\2\2\u03d9\u03da\3\2\2\2\u03dak\3\2\2\2\u03db\u03dd"+
		"\7\u012b\2\2\u03dc\u03db\3\2\2\2\u03dc\u03dd\3\2\2\2\u03dd\u03de\3\2\2"+
		"\2\u03de\u03df\t\f\2\2\u03df\u03f4\7\u00b6\2\2\u03e0\u03e1\5\u008eH\2"+
		"\u03e1\u03e2\7\u00d7\2\2\u03e2\u03f4\3\2\2\2\u03e3\u03e4\7\32\2\2\u03e4"+
		"\u03e5\7\u013c\2\2\u03e5\u03e6\7\u00aa\2\2\u03e6\u03e7\7\u00a3\2\2\u03e7"+
		"\u03f0\7\u013c\2\2\u03e8\u03ee\7\u00a4\2\2\u03e9\u03ef\5\u00ccg\2\u03ea"+
		"\u03eb\5\u00c6d\2\u03eb\u03ec\7\4\2\2\u03ec\u03ed\7\5\2\2\u03ed\u03ef"+
		"\3\2\2\2\u03ee\u03e9\3\2\2\2\u03ee\u03ea\3\2\2\2\u03ef\u03f1\3\2\2\2\u03f0"+
		"\u03e8\3\2\2\2\u03f0\u03f1\3\2\2\2\u03f1\u03f4\3\2\2\2\u03f2\u03f4\5\u008e"+
		"H\2\u03f3\u03dc\3\2\2\2\u03f3\u03e0\3\2\2\2\u03f3\u03e3\3\2\2\2\u03f3"+
		"\u03f2\3\2\2\2\u03f4m\3\2\2\2\u03f5\u03f6\7\4\2\2\u03f6\u03f7\5p9\2\u03f7"+
		"\u03f8\7\5\2\2\u03f8o\3\2\2\2\u03f9\u03fe\5\u00c8e\2\u03fa\u03fb\7\6\2"+
		"\2\u03fb\u03fd\5\u00c8e\2\u03fc\u03fa\3\2\2\2\u03fd\u0400\3\2\2\2\u03fe"+
		"\u03fc\3\2\2\2\u03fe\u03ff\3\2\2\2\u03ffq\3\2\2\2\u0400\u03fe\3\2\2\2"+
		"\u0401\u0402\7\4\2\2\u0402\u0407\5t;\2\u0403\u0404\7\6\2\2\u0404\u0406"+
		"\5t;\2\u0405\u0403\3\2\2\2\u0406\u0409\3\2\2\2\u0407\u0405\3\2\2\2\u0407"+
		"\u0408\3\2\2\2\u0408\u040a\3\2\2\2\u0409\u0407\3\2\2\2\u040a\u040b\7\5"+
		"\2\2\u040bs\3\2\2\2\u040c\u040e\5\u00c8e\2\u040d\u040f\t\5\2\2\u040e\u040d"+
		"\3\2\2\2\u040e\u040f\3\2\2\2\u040fu\3\2\2\2\u0410\u0412\5\u0080A\2\u0411"+
		"\u0413\5L\'\2\u0412\u0411\3\2\2\2\u0412\u0413\3\2\2\2\u0413\u0415\3\2"+
		"\2\2\u0414\u0416\5j\66\2\u0415\u0414\3\2\2\2\u0415\u0416\3\2\2\2\u0416"+
		"\u0417\3\2\2\2\u0417\u0418\5|?\2\u0418\u042c\3\2\2\2\u0419\u041a\7\4\2"+
		"\2\u041a\u041b\5\24\13\2\u041b\u041d\7\5\2\2\u041c\u041e\5j\66\2\u041d"+
		"\u041c\3\2\2\2\u041d\u041e\3\2\2\2\u041e\u041f\3\2\2\2\u041f\u0420\5|"+
		"?\2\u0420\u042c\3\2\2\2\u0421\u0422\7\4\2\2\u0422\u0423\5b\62\2\u0423"+
		"\u0425\7\5\2\2\u0424\u0426\5j\66\2\u0425\u0424\3\2\2\2\u0425\u0426\3\2"+
		"\2\2\u0426\u0427\3\2\2\2\u0427\u0428\5|?\2\u0428\u042c\3\2\2\2\u0429\u042c"+
		"\5x=\2\u042a\u042c\5z>\2\u042b\u0410\3\2\2\2\u042b\u0419\3\2\2\2\u042b"+
		"\u0421\3\2\2\2\u042b\u0429\3\2\2\2\u042b\u042a\3\2\2\2\u042cw\3\2\2\2"+
		"\u042d\u042e\7\u0115\2\2\u042e\u0433\5\u008eH\2\u042f\u0430\7\6\2\2\u0430"+
		"\u0432\5\u008eH\2\u0431\u042f\3\2\2\2\u0432\u0435\3\2\2\2\u0433\u0431"+
		"\3\2\2\2\u0433\u0434\3\2\2\2\u0434\u0436\3\2\2\2\u0435\u0433\3\2\2\2\u0436"+
		"\u0437\5|?\2\u0437y\3\2\2\2\u0438\u0439\5\u00c4c\2\u0439\u0442\7\4\2\2"+
		"\u043a\u043f\5\u008eH\2\u043b\u043c\7\6\2\2\u043c\u043e\5\u008eH\2\u043d"+
		"\u043b\3\2\2\2\u043e\u0441\3\2\2\2\u043f\u043d\3\2\2\2\u043f\u0440\3\2"+
		"\2\2\u0440\u0443\3\2\2\2\u0441\u043f\3\2\2\2\u0442\u043a\3\2\2\2\u0442"+
		"\u0443\3\2\2\2\u0443\u0444\3\2\2\2\u0444\u0445\7\5\2\2\u0445\u0446\5|"+
		"?\2\u0446{\3\2\2\2\u0447\u0449\7\24\2\2\u0448\u0447\3\2\2\2\u0448\u0449"+
		"\3\2\2\2\u0449\u044a\3\2\2\2\u044a\u044c\5\u00ceh\2\u044b\u044d\5n8\2"+
		"\u044c\u044b\3\2\2\2\u044c\u044d\3\2\2\2\u044d\u044f\3\2\2\2\u044e\u0448"+
		"\3\2\2\2\u044e\u044f\3\2\2\2\u044f}\3\2\2\2\u0450\u0451\7\u00d6\2\2\u0451"+
		"\u0452\7f\2\2\u0452\u0453\7\u00de\2\2\u0453\u0457\7\u0138\2\2\u0454\u0455"+
		"\7\u011d\2\2\u0455\u0456\7\u00df\2\2\u0456\u0458\5\36\20\2\u0457\u0454"+
		"\3\2\2\2\u0457\u0458\3\2\2\2\u0458\u0482\3\2\2\2\u0459\u045a\7\u00d6\2"+
		"\2\u045a\u045b\7f\2\2\u045b\u0465\7G\2\2\u045c\u045d\7_\2\2\u045d\u045e"+
		"\7\u00f8\2\2\u045e\u045f\7\34\2\2\u045f\u0463\7\u0138\2\2\u0460\u0461"+
		"\7T\2\2\u0461\u0462\7\34\2\2\u0462\u0464\7\u0138\2\2\u0463\u0460\3\2\2"+
		"\2\u0463\u0464\3\2\2\2\u0464\u0466\3\2\2\2\u0465\u045c\3\2\2\2\u0465\u0466"+
		"\3\2\2\2\u0466\u046c\3\2\2\2\u0467\u0468\7*\2\2\u0468\u0469\7\u0080\2"+
		"\2\u0469\u046a\7\u00f8\2\2\u046a\u046b\7\34\2\2\u046b\u046d\7\u0138\2"+
		"\2\u046c\u0467\3\2\2\2\u046c\u046d\3\2\2\2\u046d\u0473\3\2\2\2\u046e\u046f"+
		"\7\u0094\2\2\u046f\u0470\7\u0082\2\2\u0470\u0471\7\u00f8\2\2\u0471\u0472"+
		"\7\34\2\2\u0472\u0474\7\u0138\2\2\u0473\u046e\3\2\2\2\u0473\u0474\3\2"+
		"\2\2\u0474\u0479\3\2\2\2\u0475\u0476\7\u008b\2\2\u0476\u0477\7\u00f8\2"+
		"\2\u0477\u0478\7\34\2\2\u0478\u047a\7\u0138\2\2\u0479\u0475\3\2\2\2\u0479"+
		"\u047a\3\2\2\2\u047a\u047f\3\2\2\2\u047b\u047c\7\u00a1\2\2\u047c\u047d"+
		"\7E\2\2\u047d\u047e\7\24\2\2\u047e\u0480\7\u0138\2\2\u047f\u047b\3\2\2"+
		"\2\u047f\u0480\3\2\2\2\u0480\u0482\3\2\2\2\u0481\u0450\3\2\2\2\u0481\u0459"+
		"\3\2\2\2\u0482\177\3\2\2\2\u0483\u0488\5\u00c8e\2\u0484\u0485\7\7\2\2"+
		"\u0485\u0487\5\u00c8e\2\u0486\u0484\3\2\2\2\u0487\u048a\3\2\2\2\u0488"+
		"\u0486\3\2\2\2\u0488\u0489\3\2\2\2\u0489\u0081\3\2\2\2\u048a\u0488\3\2"+
		"\2\2\u048b\u0493\5\u008eH\2\u048c\u048e\7\24\2\2\u048d\u048c\3\2\2\2\u048d"+
		"\u048e\3\2\2\2\u048e\u0491\3\2\2\2\u048f\u0492\5\u00c8e\2\u0490\u0492"+
		"\5n8\2\u0491\u048f\3\2\2\2\u0491\u0490\3\2\2\2\u0492\u0494\3\2\2\2\u0493"+
		"\u048d\3\2\2\2\u0493\u0494\3\2\2\2\u0494\u0083\3\2\2\2\u0495\u049a\5\u0082"+
		"B\2\u0496\u0497\7\6\2\2\u0497\u0499\5\u0082B\2\u0498\u0496\3\2\2\2\u0499"+
		"\u049c\3\2\2\2\u049a\u0498\3\2\2\2\u049a\u049b\3\2\2\2\u049b\u0085\3\2"+
		"\2\2\u049c\u049a\3\2\2\2\u049d\u049e\7\4\2\2\u049e\u04a3\5\u0088E\2\u049f"+
		"\u04a0\7\6\2\2\u04a0\u04a2\5\u0088E\2\u04a1\u049f\3\2\2\2\u04a2\u04a5"+
		"\3\2\2\2\u04a3\u04a1\3\2\2\2\u04a3\u04a4\3\2\2\2\u04a4\u04a6\3\2\2\2\u04a5"+
		"\u04a3\3\2\2\2\u04a6\u04a7\7\5\2\2\u04a7\u0087\3\2\2\2\u04a8\u04ab\5\u008a"+
		"F\2\u04a9\u04ab\5\u00b2Z\2\u04aa\u04a8\3\2\2\2\u04aa\u04a9\3\2\2\2\u04ab"+
		"\u0089\3\2\2\2\u04ac\u04ba\5\u00c6d\2\u04ad\u04ae\5\u00ccg\2\u04ae\u04af"+
		"\7\4\2\2\u04af\u04b4\5\u008cG\2\u04b0\u04b1\7\6\2\2\u04b1\u04b3\5\u008c"+
		"G\2\u04b2\u04b0\3\2\2\2\u04b3\u04b6\3\2\2\2\u04b4\u04b2\3\2\2\2\u04b4"+
		"\u04b5\3\2\2\2\u04b5\u04b7\3\2\2\2\u04b6\u04b4\3\2\2\2\u04b7\u04b8\7\5"+
		"\2\2\u04b8\u04ba\3\2\2\2\u04b9\u04ac\3\2\2\2\u04b9\u04ad\3\2\2\2\u04ba"+
		"\u008b\3\2\2\2\u04bb\u04be\5\u00c6d\2\u04bc\u04be\5\u009cO\2\u04bd\u04bb"+
		"\3\2\2\2\u04bd\u04bc\3\2\2\2\u04be\u008d\3\2\2\2\u04bf\u04c0\5\u0092J"+
		"\2\u04c0\u008f\3\2\2\2\u04c1\u04c6\5\u008eH\2\u04c2\u04c3\7\6\2\2\u04c3"+
		"\u04c5\5\u008eH\2\u04c4\u04c2\3\2\2\2\u04c5\u04c8\3\2\2\2\u04c6\u04c4"+
		"\3\2\2\2\u04c6\u04c7\3\2\2\2\u04c7\u0091\3\2\2\2\u04c8\u04c6\3\2\2\2\u04c9"+
		"\u04ca\bJ\1\2\u04ca\u04cb\7\u00a0\2\2\u04cb\u04d6\5\u0092J\7\u04cc\u04cd"+
		"\7W\2\2\u04cd\u04ce\7\4\2\2\u04ce\u04cf\5\24\13\2\u04cf\u04d0\7\5\2\2"+
		"\u04d0\u04d6\3\2\2\2\u04d1\u04d3\5\u0096L\2\u04d2\u04d4\5\u0094K\2\u04d3"+
		"\u04d2\3\2\2\2\u04d3\u04d4\3\2\2\2\u04d4\u04d6\3\2\2\2\u04d5\u04c9\3\2"+
		"\2\2\u04d5\u04cc\3\2\2\2\u04d5\u04d1\3\2\2\2\u04d6\u04df\3\2\2\2\u04d7"+
		"\u04d8\f\4\2\2\u04d8\u04d9\7\17\2\2\u04d9\u04de\5\u0092J\5\u04da\u04db"+
		"\f\3\2\2\u04db\u04dc\7\u00a8\2\2\u04dc\u04de\5\u0092J\4\u04dd\u04d7\3"+
		"\2\2\2\u04dd\u04da\3\2\2\2\u04de\u04e1\3\2\2\2\u04df\u04dd\3\2\2\2\u04df"+
		"\u04e0\3\2\2\2\u04e0\u0093\3\2\2\2\u04e1\u04df\3\2\2\2\u04e2\u04e4\7\u00a0"+
		"\2\2\u04e3\u04e2\3\2\2\2\u04e3\u04e4\3\2\2\2\u04e4\u04e5\3\2\2\2\u04e5"+
		"\u04e6\7\30\2\2\u04e6\u04e7\5\u0096L\2\u04e7\u04e8\7\17\2\2\u04e8\u04e9"+
		"\5\u0096L\2\u04e9\u0535\3\2\2\2\u04ea\u04ec\7\u00a0\2\2\u04eb\u04ea\3"+
		"\2\2\2\u04eb\u04ec\3\2\2\2\u04ec\u04ed\3\2\2\2\u04ed\u04ee\7u\2\2\u04ee"+
		"\u04ef\7\4\2\2\u04ef\u04f4\5\u008eH\2\u04f0\u04f1\7\6\2\2\u04f1\u04f3"+
		"\5\u008eH\2\u04f2\u04f0\3\2\2\2\u04f3\u04f6\3\2\2\2\u04f4\u04f2\3\2\2"+
		"\2\u04f4\u04f5\3\2\2\2\u04f5\u04f7\3\2\2\2\u04f6\u04f4\3\2\2\2\u04f7\u04f8"+
		"\7\5\2\2\u04f8\u0535\3\2\2\2\u04f9\u04fb\7\u00a0\2\2\u04fa\u04f9\3\2\2"+
		"\2\u04fa\u04fb\3\2\2\2\u04fb\u04fc\3\2\2\2\u04fc\u04fd\7u\2\2\u04fd\u04fe"+
		"\7\4\2\2\u04fe\u04ff\5\24\13\2\u04ff\u0500\7\5\2\2\u0500\u0535\3\2\2\2"+
		"\u0501\u0503\7\u00a0\2\2\u0502\u0501\3\2\2\2\u0502\u0503\3\2\2\2\u0503"+
		"\u0504\3\2\2\2\u0504\u0505\7\u00d1\2\2\u0505\u0535\5\u0096L\2\u0506\u0508"+
		"\7\u00a0\2\2\u0507\u0506\3\2\2\2\u0507\u0508\3\2\2\2\u0508\u0509\3\2\2"+
		"\2\u0509\u050a\t\r\2\2\u050a\u0518\t\16\2\2\u050b\u050c\7\4\2\2\u050c"+
		"\u0519\7\5\2\2\u050d\u050e\7\4\2\2\u050e\u0513\5\u008eH\2\u050f\u0510"+
		"\7\6\2\2\u0510\u0512\5\u008eH\2\u0511\u050f\3\2\2\2\u0512\u0515\3\2\2"+
		"\2\u0513\u0511\3\2\2\2\u0513\u0514\3\2\2\2\u0514\u0516\3\2\2\2\u0515\u0513"+
		"\3\2\2\2\u0516\u0517\7\5\2\2\u0517\u0519\3\2\2\2\u0518\u050b\3\2\2\2\u0518"+
		"\u050d\3\2\2\2\u0519\u0535\3\2\2\2\u051a\u051c\7\u00a0\2\2\u051b\u051a"+
		"\3\2\2\2\u051b\u051c\3\2\2\2\u051c\u051d\3\2\2\2\u051d\u051e\t\r\2\2\u051e"+
		"\u0521\5\u0096L\2\u051f\u0520\7S\2\2\u0520\u0522\7\u0138\2\2\u0521\u051f"+
		"\3\2\2\2\u0521\u0522\3\2\2\2\u0522\u0535\3\2\2\2\u0523\u0525\7\177\2\2"+
		"\u0524\u0526\7\u00a0\2\2\u0525\u0524\3\2\2\2\u0525\u0526\3\2\2\2\u0526"+
		"\u0527\3\2\2\2\u0527\u0535\7\u00a1\2\2\u0528\u052a\7\177\2\2\u0529\u052b"+
		"\7\u00a0\2\2\u052a\u0529\3\2\2\2\u052a\u052b\3\2\2\2\u052b\u052c\3\2\2"+
		"\2\u052c\u0535\t\17\2\2\u052d\u052f\7\177\2\2\u052e\u0530\7\u00a0\2\2"+
		"\u052f\u052e\3\2\2\2\u052f\u0530\3\2\2\2\u0530\u0531\3\2\2\2\u0531\u0532"+
		"\7M\2\2\u0532\u0533\7h\2\2\u0533\u0535\5\u0096L\2\u0534\u04e3\3\2\2\2"+
		"\u0534\u04eb\3\2\2\2\u0534\u04fa\3\2\2\2\u0534\u0502\3\2\2\2\u0534\u0507"+
		"\3\2\2\2\u0534\u051b\3\2\2\2\u0534\u0523\3\2\2\2\u0534\u0528\3\2\2\2\u0534"+
		"\u052d\3\2\2\2\u0535\u0095\3\2\2\2\u0536\u0537\bL\1\2\u0537\u053b\5\u009a"+
		"N\2\u0538\u0539\t\20\2\2\u0539\u053b\5\u0096L\t\u053a\u0536\3\2\2\2\u053a"+
		"\u0538\3\2\2\2\u053b\u0551\3\2\2\2\u053c\u053d\f\b\2\2\u053d\u053e\t\21"+
		"\2\2\u053e\u0550\5\u0096L\t\u053f\u0540\f\7\2\2\u0540\u0541\t\22\2\2\u0541"+
		"\u0550\5\u0096L\b\u0542\u0543\f\6\2\2\u0543\u0544\7\u0130\2\2\u0544\u0550"+
		"\5\u0096L\7\u0545\u0546\f\5\2\2\u0546\u0547\7\u0133\2\2\u0547\u0550\5"+
		"\u0096L\6\u0548\u0549\f\4\2\2\u0549\u054a\7\u0131\2\2\u054a\u0550\5\u0096"+
		"L\5\u054b\u054c\f\3\2\2\u054c\u054d\5\u009eP\2\u054d\u054e\5\u0096L\4"+
		"\u054e\u0550\3\2\2\2\u054f\u053c\3\2\2\2\u054f\u053f\3\2\2\2\u054f\u0542"+
		"\3\2\2\2\u054f\u0545\3\2\2\2\u054f\u0548\3\2\2\2\u054f\u054b\3\2\2\2\u0550"+
		"\u0553\3\2\2\2\u0551\u054f\3\2\2\2\u0551\u0552\3\2\2\2\u0552\u0097\3\2"+
		"\2\2\u0553\u0551\3\2\2\2\u0554\u0555\t\23\2\2\u0555\u0099\3\2\2\2\u0556"+
		"\u0557\bN\1\2\u0557\u0634\t\24\2\2\u0558\u0559\t\25\2\2\u0559\u055a\7"+
		"\4\2\2\u055a\u055b\5\u0098M\2\u055b\u055c\7\6\2\2\u055c\u055d\5\u0096"+
		"L\2\u055d\u055e\7\6\2\2\u055e\u055f\5\u0096L\2\u055f\u0560\7\5\2\2\u0560"+
		"\u0634\3\2\2\2\u0561\u0562\t\26\2\2\u0562\u0563\7\4\2\2\u0563\u0564\5"+
		"\u0098M\2\u0564\u0565\7\6\2\2\u0565\u0566\5\u0096L\2\u0566\u0567\7\6\2"+
		"\2\u0567\u0568\5\u0096L\2\u0568\u0569\7\5\2\2\u0569\u0634\3\2\2\2\u056a"+
		"\u056c\7\37\2\2\u056b\u056d\5\u00b8]\2\u056c\u056b\3\2\2\2\u056d\u056e"+
		"\3\2\2\2\u056e\u056c\3\2\2\2\u056e\u056f\3\2\2\2\u056f\u0572\3\2\2\2\u0570"+
		"\u0571\7Q\2\2\u0571\u0573\5\u008eH\2\u0572\u0570\3\2\2\2\u0572\u0573\3"+
		"\2\2\2\u0573\u0574\3\2\2\2\u0574\u0575\7R\2\2\u0575\u0634\3\2\2\2\u0576"+
		"\u0577\7\37\2\2\u0577\u0579\5\u008eH\2\u0578\u057a\5\u00b8]\2\u0579\u0578"+
		"\3\2\2\2\u057a\u057b\3\2\2\2\u057b\u0579\3\2\2\2\u057b\u057c\3\2\2\2\u057c"+
		"\u057f\3\2\2\2\u057d\u057e\7Q\2\2\u057e\u0580\5\u008eH\2\u057f\u057d\3"+
		"\2\2\2\u057f\u0580\3\2\2\2\u0580\u0581\3\2\2\2\u0581\u0582\7R\2\2\u0582"+
		"\u0634\3\2\2\2\u0583\u0584\t\27\2\2\u0584\u0585\7\4\2\2\u0585\u0586\5"+
		"\u008eH\2\u0586\u0587\7\24\2\2\u0587\u0588\5\u00aeX\2\u0588\u0589\7\5"+
		"\2\2\u0589\u0634\3\2\2\2\u058a\u058b\7\u00ed\2\2\u058b\u0594\7\4\2\2\u058c"+
		"\u0591\5\u0082B\2\u058d\u058e\7\6\2\2\u058e\u0590\5\u0082B\2\u058f\u058d"+
		"\3\2\2\2\u0590\u0593\3\2\2\2\u0591\u058f\3\2\2\2\u0591\u0592\3\2\2\2\u0592"+
		"\u0595\3\2\2\2\u0593\u0591\3\2\2\2\u0594\u058c\3\2\2\2\u0594\u0595\3\2"+
		"\2\2\u0595\u0596\3\2\2\2\u0596\u0634\7\5\2\2\u0597\u0598\7b\2\2\u0598"+
		"\u0599\7\4\2\2\u0599\u059c\5\u008eH\2\u059a\u059b\7s\2\2\u059b\u059d\7"+
		"\u00a2\2\2\u059c\u059a\3\2\2\2\u059c\u059d\3\2\2\2\u059d\u059e\3\2\2\2"+
		"\u059e\u059f\7\5\2\2\u059f\u0634\3\2\2\2\u05a0\u05a1\7\u0083\2\2\u05a1"+
		"\u05a2\7\4\2\2\u05a2\u05a5\5\u008eH\2\u05a3\u05a4\7s\2\2\u05a4\u05a6\7"+
		"\u00a2\2\2\u05a5\u05a3\3\2\2\2\u05a5\u05a6\3\2\2\2\u05a6\u05a7\3\2\2\2"+
		"\u05a7\u05a8\7\5\2\2\u05a8\u0634\3\2\2\2\u05a9\u05aa\7\u00b9\2\2\u05aa"+
		"\u05ab\7\4\2\2\u05ab\u05ac\5\u0096L\2\u05ac\u05ad\7u\2\2\u05ad\u05ae\5"+
		"\u0096L\2\u05ae\u05af\7\5\2\2\u05af\u0634\3\2\2\2\u05b0\u0634\5\u009c"+
		"O\2\u05b1\u0634\7\u012c\2\2\u05b2\u05b3\5\u00c6d\2\u05b3\u05b4\7\7\2\2"+
		"\u05b4\u05b5\7\u012c\2\2\u05b5\u0634\3\2\2\2\u05b6\u05b7\7\4\2\2\u05b7"+
		"\u05ba\5\u0082B\2\u05b8\u05b9\7\6\2\2\u05b9\u05bb\5\u0082B\2\u05ba\u05b8"+
		"\3\2\2\2\u05bb\u05bc\3\2\2\2\u05bc\u05ba\3\2\2\2\u05bc\u05bd\3\2\2\2\u05bd"+
		"\u05be\3\2\2\2\u05be\u05bf\7\5\2\2\u05bf\u0634\3\2\2\2\u05c0\u05c1\7\4"+
		"\2\2\u05c1\u05c2\5\24\13\2\u05c2\u05c3\7\5\2\2\u05c3\u0634\3\2\2\2\u05c4"+
		"\u05c5\5\u00c4c\2\u05c5\u05d1\7\4\2\2\u05c6\u05c8\5`\61\2\u05c7\u05c6"+
		"\3\2\2\2\u05c7\u05c8\3\2\2\2\u05c8\u05c9\3\2\2\2\u05c9\u05ce\5\u008eH"+
		"\2\u05ca\u05cb\7\6\2\2\u05cb\u05cd\5\u008eH\2\u05cc\u05ca\3\2\2\2\u05cd"+
		"\u05d0\3\2\2\2\u05ce\u05cc\3\2\2\2\u05ce\u05cf\3\2\2\2\u05cf\u05d2\3\2"+
		"\2\2\u05d0\u05ce\3\2\2\2\u05d1\u05c7\3\2\2\2\u05d1\u05d2\3\2\2\2\u05d2"+
		"\u05d3\3\2\2\2\u05d3\u05da\7\5\2\2\u05d4\u05d5\7`\2\2\u05d5\u05d6\7\4"+
		"\2\2\u05d6\u05d7\7\u011b\2\2\u05d7\u05d8\5\u0092J\2\u05d8\u05d9\7\5\2"+
		"\2\u05d9\u05db\3\2\2\2\u05da\u05d4\3\2\2\2\u05da\u05db\3\2\2\2\u05db\u05de"+
		"\3\2\2\2\u05dc\u05dd\t\30\2\2\u05dd\u05df\7\u00a2\2\2\u05de\u05dc\3\2"+
		"\2\2\u05de\u05df\3\2\2\2\u05df\u05e2\3\2\2\2\u05e0\u05e1\7\u00ad\2\2\u05e1"+
		"\u05e3\5\u00be`\2\u05e2\u05e0\3\2\2\2\u05e2\u05e3\3\2\2\2\u05e3\u0634"+
		"\3\2\2\2\u05e4\u05e5\5\u00ccg\2\u05e5\u05e6\7\u0135\2\2\u05e6\u05e7\5"+
		"\u008eH\2\u05e7\u0634\3\2\2\2\u05e8\u05e9\7\4\2\2\u05e9\u05ec\5\u00cc"+
		"g\2\u05ea\u05eb\7\6\2\2\u05eb\u05ed\5\u00ccg\2\u05ec\u05ea\3\2\2\2\u05ed"+
		"\u05ee\3\2\2\2\u05ee\u05ec\3\2\2\2\u05ee\u05ef\3\2\2\2\u05ef\u05f0\3\2"+
		"\2\2\u05f0\u05f1\7\5\2\2\u05f1\u05f2\7\u0135\2\2\u05f2\u05f3\5\u008eH"+
		"\2\u05f3\u0634\3\2\2\2\u05f4\u0634\5\u00ccg\2\u05f5\u05f6\7\4\2\2\u05f6"+
		"\u05f7\5\u008eH\2\u05f7\u05f8\7\5\2\2\u05f8\u0634\3\2\2\2\u05f9\u05fa"+
		"\7\\\2\2\u05fa\u05fb\7\4\2\2\u05fb\u05fc\5\u00ccg\2\u05fc\u05fd\7h\2\2"+
		"\u05fd\u05fe\5\u0096L\2\u05fe\u05ff\7\5\2\2\u05ff\u0634\3\2\2\2\u0600"+
		"\u0601\t\31\2\2\u0601\u0602\7\4\2\2\u0602\u0603\5\u0096L\2\u0603\u0604"+
		"\t\32\2\2\u0604\u0607\5\u0096L\2\u0605\u0606\t\33\2\2\u0606\u0608\5\u0096"+
		"L\2\u0607\u0605\3\2\2\2\u0607\u0608\3\2\2\2\u0608\u0609\3\2\2\2\u0609"+
		"\u060a\7\5\2\2\u060a\u0634\3\2\2\2\u060b\u060c\7\u0104\2\2\u060c\u060e"+
		"\7\4\2\2\u060d\u060f\t\34\2\2\u060e\u060d\3\2\2\2\u060e\u060f\3\2\2\2"+
		"\u060f\u0611\3\2\2\2\u0610\u0612\5\u0096L\2\u0611\u0610\3\2\2\2\u0611"+
		"\u0612\3\2\2\2\u0612\u0613\3\2\2\2\u0613\u0614\7h\2\2\u0614\u0615\5\u0096"+
		"L\2\u0615\u0616\7\5\2\2\u0616\u0634\3\2\2\2\u0617\u0618\7\u00af\2\2\u0618"+
		"\u0619\7\4\2\2\u0619\u061a\5\u0096L\2\u061a\u061b\7\u00b8\2\2\u061b\u061c"+
		"\5\u0096L\2\u061c\u061d\7h\2\2\u061d\u0620\5\u0096L\2\u061e\u061f\7d\2"+
		"\2\u061f\u0621\5\u0096L\2\u0620\u061e\3\2\2\2\u0620\u0621\3\2\2\2\u0621"+
		"\u0622\3\2\2\2\u0622\u0623\7\5\2\2\u0623\u0634\3\2\2\2\u0624\u0625\t\35"+
		"\2\2\u0625\u0626\7\4\2\2\u0626\u0627\5\u0096L\2\u0627\u0628\7\5\2\2\u0628"+
		"\u0629\7\u011e\2\2\u0629\u062a\7n\2\2\u062a\u062b\7\4\2\2\u062b\u062c"+
		"\7\u00a9\2\2\u062c\u062d\7\34\2\2\u062d\u062e\5\66\34\2\u062e\u0631\7"+
		"\5\2\2\u062f\u0630\7\u00ad\2\2\u0630\u0632\5\u00be`\2\u0631\u062f\3\2"+
		"\2\2\u0631\u0632\3\2\2\2\u0632\u0634\3\2\2\2\u0633\u0556\3\2\2\2\u0633"+
		"\u0558\3\2\2\2\u0633\u0561\3\2\2\2\u0633\u056a\3\2\2\2\u0633\u0576\3\2"+
		"\2\2\u0633\u0583\3\2\2\2\u0633\u058a\3\2\2\2\u0633\u0597\3\2\2\2\u0633"+
		"\u05a0\3\2\2\2\u0633\u05a9\3\2\2\2\u0633\u05b0\3\2\2\2\u0633\u05b1\3\2"+
		"\2\2\u0633\u05b2\3\2\2\2\u0633\u05b6\3\2\2\2\u0633\u05c0\3\2\2\2\u0633"+
		"\u05c4\3\2\2\2\u0633\u05e4\3\2\2\2\u0633\u05e8\3\2\2\2\u0633\u05f4\3\2"+
		"\2\2\u0633\u05f5\3\2\2\2\u0633\u05f9\3\2\2\2\u0633\u0600\3\2\2\2\u0633"+
		"\u060b\3\2\2\2\u0633\u0617\3\2\2\2\u0633\u0624\3\2\2\2\u0634\u063f\3\2"+
		"\2\2\u0635\u0636\f\13\2\2\u0636\u0637\7\b\2\2\u0637\u0638\5\u0096L\2\u0638"+
		"\u0639\7\t\2\2\u0639\u063e\3\2\2\2\u063a\u063b\f\t\2\2\u063b\u063c\7\7"+
		"\2\2\u063c\u063e\5\u00ccg\2\u063d\u0635\3\2\2\2\u063d\u063a\3\2\2\2\u063e"+
		"\u0641\3\2\2\2\u063f\u063d\3\2\2\2\u063f\u0640\3\2\2\2\u0640\u009b\3\2"+
		"\2\2\u0641\u063f\3\2\2\2\u0642\u064f\7\u00a1\2\2\u0643\u064f\5\u00a2R"+
		"\2\u0644\u0645\5\u00ccg\2\u0645\u0646\7\u0138\2\2\u0646\u064f\3\2\2\2"+
		"\u0647\u064f\5\u00d2j\2\u0648\u064f\5\u00a0Q\2\u0649\u064b\7\u0138\2\2"+
		"\u064a\u0649\3\2\2\2\u064b\u064c\3\2\2\2\u064c\u064a\3\2\2\2\u064c\u064d"+
		"\3\2\2\2\u064d\u064f\3\2\2\2\u064e\u0642\3\2\2\2\u064e\u0643\3\2\2\2\u064e"+
		"\u0644\3\2\2\2\u064e\u0647\3\2\2\2\u064e\u0648\3\2\2\2\u064e\u064a\3\2"+
		"\2\2\u064f\u009d\3\2\2\2\u0650\u0651\t\36\2\2\u0651\u009f\3\2\2\2\u0652"+
		"\u0653\t\37\2\2\u0653\u00a1\3\2\2\2\u0654\u0657\7}\2\2\u0655\u0658\5\u00a4"+
		"S\2\u0656\u0658\5\u00a8U\2\u0657\u0655\3\2\2\2\u0657\u0656\3\2\2\2\u0657"+
		"\u0658\3\2\2\2\u0658\u00a3\3\2\2\2\u0659\u065b\5\u00a6T\2\u065a\u065c"+
		"\5\u00aaV\2\u065b\u065a\3\2\2\2\u065b\u065c\3\2\2\2\u065c\u00a5\3\2\2"+
		"\2\u065d\u065e\5\u00acW\2\u065e\u065f\5\u00ccg\2\u065f\u0661\3\2\2\2\u0660"+
		"\u065d\3\2\2\2\u0661\u0662\3\2\2\2\u0662\u0660\3\2\2\2\u0662\u0663\3\2"+
		"\2\2\u0663\u00a7\3\2\2\2\u0664\u0667\5\u00aaV\2\u0665\u0668\5\u00a6T\2"+
		"\u0666\u0668\5\u00aaV\2\u0667\u0665\3\2\2\2\u0667\u0666\3\2\2\2\u0667"+
		"\u0668\3\2\2\2\u0668\u00a9\3\2\2\2\u0669\u066a\5\u00acW\2\u066a\u066b"+
		"\5\u00ccg\2\u066b\u066c\7\u00fe\2\2\u066c\u066d\5\u00ccg\2\u066d\u00ab"+
		"\3\2\2\2\u066e\u0670\t \2\2\u066f\u066e\3\2\2\2\u066f\u0670\3\2\2\2\u0670"+
		"\u0671\3\2\2\2\u0671\u0672\t!\2\2\u0672\u00ad\3\2\2\2\u0673\u0674\7\23"+
		"\2\2\u0674\u0675\7\u0126\2\2\u0675\u0676\5\u00aeX\2\u0676\u0677\7\u0128"+
		"\2\2\u0677\u06a2\3\2\2\2\u0678\u0679\7\u0094\2\2\u0679\u067a\7\u0126\2"+
		"\2\u067a\u067b\5\u00aeX\2\u067b\u067c\7\6\2\2\u067c\u067d\5\u00aeX\2\u067d"+
		"\u067e\7\u0128\2\2\u067e\u06a2\3\2\2\2\u067f\u0686\7\u00ed\2\2\u0680\u0682"+
		"\7\u0126\2\2\u0681\u0683\5\u00b4[\2\u0682\u0681\3\2\2\2\u0682\u0683\3"+
		"\2\2\2\u0683\u0684\3\2\2\2\u0684\u0687\7\u0128\2\2\u0685\u0687\7\u0124"+
		"\2\2\u0686\u0680\3\2\2\2\u0686\u0685\3\2\2\2\u0687\u06a2\3\2\2\2\u0688"+
		"\u0689\7}\2\2\u0689\u068c\t\"\2\2\u068a\u068b\7\u00fe\2\2\u068b\u068d"+
		"\7\u009a\2\2\u068c\u068a\3\2\2\2\u068c\u068d\3\2\2\2\u068d\u06a2\3\2\2"+
		"\2\u068e\u068f\7}\2\2\u068f\u0692\t#\2\2\u0690\u0691\7\u00fe\2\2\u0691"+
		"\u0693\t$\2\2\u0692\u0690\3\2\2\2\u0692\u0693\3\2\2\2\u0693\u06a2\3\2"+
		"\2\2\u0694\u069f\5\u00ccg\2\u0695\u0696\7\4\2\2\u0696\u069b\7\u013c\2"+
		"\2\u0697\u0698\7\6\2\2\u0698\u069a\7\u013c\2\2\u0699\u0697\3\2\2\2\u069a"+
		"\u069d\3\2\2\2\u069b\u0699\3\2\2\2\u069b\u069c\3\2\2\2\u069c\u069e\3\2"+
		"\2\2\u069d\u069b\3\2\2\2\u069e\u06a0\7\5\2\2\u069f\u0695\3\2\2\2\u069f"+
		"\u06a0\3\2\2\2\u06a0\u06a2\3\2\2\2\u06a1\u0673\3\2\2\2\u06a1\u0678\3\2"+
		"\2\2\u06a1\u067f\3\2\2\2\u06a1\u0688\3\2\2\2\u06a1\u068e\3\2\2\2\u06a1"+
		"\u0694\3\2\2\2\u06a2\u00af\3\2\2\2\u06a3\u06a8\5\u00b2Z\2\u06a4\u06a5"+
		"\7\6\2\2\u06a5\u06a7\5\u00b2Z\2\u06a6\u06a4\3\2\2\2\u06a7\u06aa\3\2\2"+
		"\2\u06a8\u06a6\3\2\2\2\u06a8\u06a9\3\2\2\2\u06a9\u00b1\3\2\2\2\u06aa\u06a8"+
		"\3\2\2\2\u06ab\u06ac\5\u00c8e\2\u06ac\u06af\5\u00aeX\2\u06ad\u06ae\7\u00a0"+
		"\2\2\u06ae\u06b0\7\u00a1\2\2\u06af\u06ad\3\2\2\2\u06af\u06b0\3\2\2\2\u06b0"+
		"\u06b2\3\2\2\2\u06b1\u06b3\5\22\n\2\u06b2\u06b1\3\2\2\2\u06b2\u06b3\3"+
		"\2\2\2\u06b3\u00b3\3\2\2\2\u06b4\u06b9\5\u00b6\\\2\u06b5\u06b6\7\6\2\2"+
		"\u06b6\u06b8\5\u00b6\\\2\u06b7\u06b5\3\2\2\2\u06b8\u06bb\3\2\2\2\u06b9"+
		"\u06b7\3\2\2\2\u06b9\u06ba\3\2\2\2\u06ba\u00b5\3\2\2\2\u06bb\u06b9\3\2"+
		"\2\2\u06bc\u06be\5\u00ccg\2\u06bd\u06bf\7\u0134\2\2\u06be\u06bd\3\2\2"+
		"\2\u06be\u06bf\3\2\2\2\u06bf\u06c0\3\2\2\2\u06c0\u06c3\5\u00aeX\2\u06c1"+
		"\u06c2\7\u00a0\2\2\u06c2\u06c4\7\u00a1\2\2\u06c3\u06c1\3\2\2\2\u06c3\u06c4"+
		"\3\2\2\2\u06c4\u06c6\3\2\2\2\u06c5\u06c7\5\22\n\2\u06c6\u06c5\3\2\2\2"+
		"\u06c6\u06c7\3\2\2\2\u06c7\u00b7\3\2\2\2\u06c8\u06c9\7\u011a\2\2\u06c9"+
		"\u06ca\5\u008eH\2\u06ca\u06cb\7\u00f9\2\2\u06cb\u06cc\5\u008eH\2\u06cc"+
		"\u00b9\3\2\2\2\u06cd\u06ce\7\u011c\2\2\u06ce\u06d3\5\u00bc_\2\u06cf\u06d0"+
		"\7\6\2\2\u06d0\u06d2\5\u00bc_\2\u06d1\u06cf\3\2\2\2\u06d2\u06d5\3\2\2"+
		"\2\u06d3\u06d1\3\2\2\2\u06d3\u06d4\3\2\2\2\u06d4\u00bb\3\2\2\2\u06d5\u06d3"+
		"\3\2\2\2\u06d6\u06d7\5\u00c8e\2\u06d7\u06d8\7\24\2\2\u06d8\u06d9\5\u00be"+
		"`\2\u06d9\u00bd\3\2\2\2\u06da\u0709\5\u00c8e\2\u06db\u06dc\7\4\2\2\u06dc"+
		"\u06dd\5\u00c8e\2\u06dd\u06de\7\5\2\2\u06de\u0709\3\2\2\2\u06df\u0702"+
		"\7\4\2\2\u06e0\u06e1\7&\2\2\u06e1\u06e2\7\34\2\2\u06e2\u06e7\5\u008eH"+
		"\2\u06e3\u06e4\7\6\2\2\u06e4\u06e6\5\u008eH\2\u06e5\u06e3\3\2\2\2\u06e6"+
		"\u06e9\3\2\2\2\u06e7\u06e5\3\2\2\2\u06e7\u06e8\3\2\2\2\u06e8\u0703\3\2"+
		"\2\2\u06e9\u06e7\3\2\2\2\u06ea\u06eb\t%\2\2\u06eb\u06ec\7\34\2\2\u06ec"+
		"\u06f1\5\u008eH\2\u06ed\u06ee\7\6\2\2\u06ee\u06f0\5\u008eH\2\u06ef\u06ed"+
		"\3\2\2\2\u06f0\u06f3\3\2\2\2\u06f1\u06ef\3\2\2\2\u06f1\u06f2\3\2\2\2\u06f2"+
		"\u06f5\3\2\2\2\u06f3\u06f1\3\2\2\2\u06f4\u06ea\3\2\2\2\u06f4\u06f5\3\2"+
		"\2\2\u06f5\u0700\3\2\2\2\u06f6\u06f7\t&\2\2\u06f7\u06f8\7\34\2\2\u06f8"+
		"\u06fd\5\66\34\2\u06f9\u06fa\7\6\2\2\u06fa\u06fc\5\66\34\2\u06fb\u06f9"+
		"\3\2\2\2\u06fc\u06ff\3\2\2\2\u06fd\u06fb\3\2\2\2\u06fd\u06fe\3\2\2\2\u06fe"+
		"\u0701\3\2\2\2\u06ff\u06fd\3\2\2\2\u0700\u06f6\3\2\2\2\u0700\u0701\3\2"+
		"\2\2\u0701\u0703\3\2\2\2\u0702\u06e0\3\2\2\2\u0702\u06f4\3\2\2\2\u0703"+
		"\u0705\3\2\2\2\u0704\u0706\5\u00c0a\2\u0705\u0704\3\2\2\2\u0705\u0706"+
		"\3\2\2\2\u0706\u0707\3\2\2\2\u0707\u0709\7\5\2\2\u0708\u06da\3\2\2\2\u0708"+
		"\u06db\3\2\2\2\u0708\u06df\3\2\2\2\u0709\u00bf\3\2\2\2\u070a\u070b\7\u00c1"+
		"\2\2\u070b\u071b\5\u00c2b\2\u070c\u070d\7\u00d7\2\2\u070d\u071b\5\u00c2"+
		"b\2\u070e\u070f\7\u00c1\2\2\u070f\u0710\7\30\2\2\u0710\u0711\5\u00c2b"+
		"\2\u0711\u0712\7\17\2\2\u0712\u0713\5\u00c2b\2\u0713\u071b\3\2\2\2\u0714"+
		"\u0715\7\u00d7\2\2\u0715\u0716\7\30\2\2\u0716\u0717\5\u00c2b\2\u0717\u0718"+
		"\7\17\2\2\u0718\u0719\5\u00c2b\2\u0719\u071b\3\2\2\2\u071a\u070a\3\2\2"+
		"\2\u071a\u070c\3\2\2\2\u071a\u070e\3\2\2\2\u071a\u0714\3\2\2\2\u071b\u00c1"+
		"\3\2\2\2\u071c\u071d\7\u010a\2\2\u071d\u0724\t\'\2\2\u071e\u071f\78\2"+
		"\2\u071f\u0724\7\u00d6\2\2\u0720\u0721\5\u008eH\2\u0721\u0722\t\'\2\2"+
		"\u0722\u0724\3\2\2\2\u0723\u071c\3\2\2\2\u0723\u071e\3\2\2\2\u0723\u0720"+
		"\3\2\2\2\u0724\u00c3\3\2\2\2\u0725\u072a\5\u00c6d\2\u0726\u072a\7`\2\2"+
		"\u0727\u072a\7\u0087\2\2\u0728\u072a\7\u00d0\2\2\u0729\u0725\3\2\2\2\u0729"+
		"\u0726\3\2\2\2\u0729\u0727\3\2\2\2\u0729\u0728\3\2\2\2\u072a\u00c5\3\2"+
		"\2\2\u072b\u0730\5\u00ccg\2\u072c\u072d\7\7\2\2\u072d\u072f\5\u00ccg\2"+
		"\u072e\u072c\3\2\2\2\u072f\u0732\3\2\2\2\u0730\u072e\3\2\2\2\u0730\u0731"+
		"\3\2\2\2\u0731\u00c7\3\2\2\2\u0732\u0730\3\2\2\2\u0733\u0734\5\u00ccg"+
		"\2\u0734\u0735\5\u00caf\2\u0735\u00c9\3\2\2\2\u0736\u0737\7\u012b\2\2"+
		"\u0737\u0739\5\u00ccg\2\u0738\u0736\3\2\2\2\u0739\u073a\3\2\2\2\u073a"+
		"\u0738\3\2\2\2\u073a\u073b\3\2\2\2\u073b\u073e\3\2\2\2\u073c\u073e\3\2"+
		"\2\2\u073d\u0738\3\2\2\2\u073d\u073c\3\2\2\2\u073e\u00cb\3\2\2\2\u073f"+
		"\u0743\5\u00ceh\2\u0740\u0741\6g\22\2\u0741\u0743\5\u00d6l\2\u0742\u073f"+
		"\3\2\2\2\u0742\u0740\3\2\2\2\u0743\u00cd\3\2\2\2\u0744\u074b\7\u0142\2"+
		"\2\u0745\u074b\5\u00d0i\2\u0746\u0747\6h\23\2\u0747\u074b\5\u00d4k\2\u0748"+
		"\u0749\6h\24\2\u0749\u074b\5\u00d8m\2\u074a\u0744\3\2\2\2\u074a\u0745"+
		"\3\2\2\2\u074a\u0746\3\2\2\2\u074a\u0748\3\2\2\2\u074b\u00cf\3\2\2\2\u074c"+
		"\u074d\7\u0143\2\2\u074d\u00d1\3\2\2\2\u074e\u0750\6j\25\2\u074f\u0751"+
		"\7\u012b\2\2\u0750\u074f\3\2\2\2\u0750\u0751\3\2\2\2\u0751\u0752\3\2\2"+
		"\2\u0752\u077a\7\u013d\2\2\u0753\u0755\6j\26\2\u0754\u0756\7\u012b\2\2"+
		"\u0755\u0754\3\2\2\2\u0755\u0756\3\2\2\2\u0756\u0757\3\2\2\2\u0757\u077a"+
		"\7\u013e\2\2\u0758\u075a\6j\27\2\u0759\u075b\7\u012b\2\2\u075a\u0759\3"+
		"\2\2\2\u075a\u075b\3\2\2\2\u075b\u075c\3\2\2\2\u075c\u077a\t(\2\2\u075d"+
		"\u075f\7\u012b\2\2\u075e\u075d\3\2\2\2\u075e\u075f\3\2\2\2\u075f\u0760"+
		"\3\2\2\2\u0760\u077a\7\u013c\2\2\u0761\u0763\7\u012b\2\2\u0762\u0761\3"+
		"\2\2\2\u0762\u0763\3\2\2\2\u0763\u0764\3\2\2\2\u0764\u077a\7\u0139\2\2"+
		"\u0765\u0767\7\u012b\2\2\u0766\u0765\3\2\2\2\u0766\u0767\3\2\2\2\u0767"+
		"\u0768\3\2\2\2\u0768\u077a\7\u013a\2\2\u0769\u076b\7\u012b\2\2\u076a\u0769"+
		"\3\2\2\2\u076a\u076b\3\2\2\2\u076b\u076c\3\2\2\2\u076c\u077a\7\u013b\2"+
		"\2\u076d\u076f\7\u012b\2\2\u076e\u076d\3\2\2\2\u076e\u076f\3\2\2\2\u076f"+
		"\u0770\3\2\2\2\u0770\u077a\7\u0140\2\2\u0771\u0773\7\u012b\2\2\u0772\u0771"+
		"\3\2\2\2\u0772\u0773\3\2\2\2\u0773\u0774\3\2\2\2\u0774\u077a\7\u013f\2"+
		"\2\u0775\u0777\7\u012b\2\2\u0776\u0775\3\2\2\2\u0776\u0777\3\2\2\2\u0777"+
		"\u0778\3\2\2\2\u0778\u077a\7\u0141\2\2\u0779\u074e\3\2\2\2\u0779\u0753"+
		"\3\2\2\2\u0779\u0758\3\2\2\2\u0779\u075e\3\2\2\2\u0779\u0762\3\2\2\2\u0779"+
		"\u0766\3\2\2\2\u0779\u076a\3\2\2\2\u0779\u076e\3\2\2\2\u0779\u0772\3\2"+
		"\2\2\u0779\u0776\3\2\2\2\u077a\u00d3\3\2\2\2\u077b\u077c\t)\2\2\u077c"+
		"\u00d5\3\2\2\2\u077d\u077e\t*\2\2\u077e\u00d7\3\2\2\2\u077f\u0780\t+\2"+
		"\2\u0780\u00d9\3\2\2\2\u010b\u00de\u00e6\u00ea\u00ed\u00f1\u00f4\u00f8"+
		"\u00fb\u0101\u0109\u010e\u011a\u0126\u012b\u0134\u013f\u0144\u0147\u015d"+
		"\u015f\u0168\u016f\u0172\u0179\u017d\u0183\u018b\u0196\u01a1\u01a8\u01ae"+
		"\u01b7\u01ba\u01c3\u01c6\u01cf\u01d2\u01db\u01de\u01e1\u01e6\u01e8\u01f1"+
		"\u01f8\u01ff\u0202\u0204\u0210\u0214\u0218\u021e\u0222\u022a\u022e\u0231"+
		"\u0234\u0237\u023b\u023f\u0244\u0248\u024b\u024e\u0251\u0255\u025a\u025e"+
		"\u0261\u0264\u0267\u0269\u026f\u0276\u027b\u027e\u0281\u0285\u028f\u0293"+
		"\u0295\u0298\u029c\u02a2\u02a6\u02b3\u02b8\u02c5\u02ca\u02d2\u02d8\u02dc"+
		"\u02df\u02e6\u02ec\u02f5\u02ff\u030e\u0313\u0315\u0319\u0322\u032f\u0334"+
		"\u0338\u0340\u0343\u0347\u0355\u0362\u0367\u036b\u036e\u0373\u037c\u037f"+
		"\u0384\u038b\u038e\u0393\u0399\u039f\u03a3\u03a9\u03ad\u03b0\u03b5\u03b8"+
		"\u03bd\u03c1\u03c4\u03c7\u03cd\u03d2\u03d9\u03dc\u03ee\u03f0\u03f3\u03fe"+
		"\u0407\u040e\u0412\u0415\u041d\u0425\u042b\u0433\u043f\u0442\u0448\u044c"+
		"\u044e\u0457\u0463\u0465\u046c\u0473\u0479\u047f\u0481\u0488\u048d\u0491"+
		"\u0493\u049a\u04a3\u04aa\u04b4\u04b9\u04bd\u04c6\u04d3\u04d5\u04dd\u04df"+
		"\u04e3\u04eb\u04f4\u04fa\u0502\u0507\u0513\u0518\u051b\u0521\u0525\u052a"+
		"\u052f\u0534\u053a\u054f\u0551\u056e\u0572\u057b\u057f\u0591\u0594\u059c"+
		"\u05a5\u05bc\u05c7\u05ce\u05d1\u05da\u05de\u05e2\u05ee\u0607\u060e\u0611"+
		"\u0620\u0631\u0633\u063d\u063f\u064c\u064e\u0657\u065b\u0662\u0667\u066f"+
		"\u0682\u0686\u068c\u0692\u069b\u069f\u06a1\u06a8\u06af\u06b2\u06b9\u06be"+
		"\u06c3\u06c6\u06d3\u06e7\u06f1\u06f4\u06fd\u0700\u0702\u0705\u0708\u071a"+
		"\u0723\u0729\u0730\u073a\u073d\u0742\u074a\u0750\u0755\u075a\u075e\u0762"+
		"\u0766\u076a\u076e\u0772\u0776\u0779";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
