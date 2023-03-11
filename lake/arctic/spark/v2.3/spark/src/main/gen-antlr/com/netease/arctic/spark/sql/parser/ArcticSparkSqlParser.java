// Generated from /Users/jinsilei/arctic/arctic/spark/v2.3/spark/src/main/antlr4/com/netease/arctic/spark/sql/parser/ArcticSparkSql.g4 by ANTLR 4.7.2
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
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ArcticSparkSqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		PRIMARY=10, KEY=11, SELECT=12, FROM=13, ADD=14, AS=15, ALL=16, DISTINCT=17, 
		WHERE=18, GROUP=19, BY=20, GROUPING=21, SETS=22, CUBE=23, ROLLUP=24, ORDER=25, 
		HAVING=26, LIMIT=27, AT=28, OR=29, AND=30, IN=31, NOT=32, NO=33, EXISTS=34, 
		BETWEEN=35, LIKE=36, RLIKE=37, IS=38, NULL=39, TRUE=40, FALSE=41, NULLS=42, 
		ASC=43, DESC=44, FOR=45, INTERVAL=46, CASE=47, WHEN=48, THEN=49, ELSE=50, 
		END=51, JOIN=52, CROSS=53, OUTER=54, INNER=55, LEFT=56, SEMI=57, RIGHT=58, 
		FULL=59, NATURAL=60, ON=61, LATERAL=62, WINDOW=63, OVER=64, PARTITION=65, 
		RANGE=66, ROWS=67, UNBOUNDED=68, PRECEDING=69, FOLLOWING=70, CURRENT=71, 
		FIRST=72, AFTER=73, LAST=74, ROW=75, WITH=76, VALUES=77, CREATE=78, TABLE=79, 
		DIRECTORY=80, VIEW=81, REPLACE=82, INSERT=83, DELETE=84, INTO=85, DESCRIBE=86, 
		EXPLAIN=87, FORMAT=88, LOGICAL=89, CODEGEN=90, COST=91, CAST=92, SHOW=93, 
		TABLES=94, COLUMNS=95, COLUMN=96, USE=97, PARTITIONS=98, FUNCTIONS=99, 
		DROP=100, UNION=101, EXCEPT=102, SETMINUS=103, INTERSECT=104, TO=105, 
		TABLESAMPLE=106, STRATIFY=107, ALTER=108, RENAME=109, ARRAY=110, MAP=111, 
		STRUCT=112, COMMENT=113, SET=114, RESET=115, DATA=116, START=117, TRANSACTION=118, 
		COMMIT=119, ROLLBACK=120, MACRO=121, IGNORE=122, BOTH=123, LEADING=124, 
		TRAILING=125, IF=126, POSITION=127, EQ=128, NSEQ=129, NEQ=130, NEQJ=131, 
		LT=132, LTE=133, GT=134, GTE=135, PLUS=136, MINUS=137, ASTERISK=138, SLASH=139, 
		PERCENT=140, DIV=141, TILDE=142, AMPERSAND=143, PIPE=144, CONCAT_PIPE=145, 
		HAT=146, PERCENTLIT=147, BUCKET=148, OUT=149, OF=150, SORT=151, CLUSTER=152, 
		DISTRIBUTE=153, OVERWRITE=154, TRANSFORM=155, REDUCE=156, USING=157, SERDE=158, 
		SERDEPROPERTIES=159, RECORDREADER=160, RECORDWRITER=161, DELIMITED=162, 
		FIELDS=163, TERMINATED=164, COLLECTION=165, ITEMS=166, KEYS=167, ESCAPED=168, 
		LINES=169, SEPARATED=170, FUNCTION=171, EXTENDED=172, REFRESH=173, CLEAR=174, 
		CACHE=175, UNCACHE=176, LAZY=177, FORMATTED=178, GLOBAL=179, TEMPORARY=180, 
		OPTIONS=181, UNSET=182, TBLPROPERTIES=183, DBPROPERTIES=184, BUCKETS=185, 
		SKEWED=186, STORED=187, DIRECTORIES=188, LOCATION=189, EXCHANGE=190, ARCHIVE=191, 
		UNARCHIVE=192, FILEFORMAT=193, TOUCH=194, COMPACT=195, CONCATENATE=196, 
		CHANGE=197, CASCADE=198, RESTRICT=199, CLUSTERED=200, SORTED=201, PURGE=202, 
		INPUTFORMAT=203, OUTPUTFORMAT=204, DATABASE=205, DATABASES=206, DFS=207, 
		TRUNCATE=208, ANALYZE=209, COMPUTE=210, LIST=211, STATISTICS=212, PARTITIONED=213, 
		EXTERNAL=214, DEFINED=215, REVOKE=216, GRANT=217, LOCK=218, UNLOCK=219, 
		MSCK=220, REPAIR=221, RECOVER=222, EXPORT=223, IMPORT=224, LOAD=225, ROLE=226, 
		ROLES=227, COMPACTIONS=228, PRINCIPALS=229, TRANSACTIONS=230, INDEX=231, 
		INDEXES=232, LOCKS=233, OPTION=234, ANTI=235, LOCAL=236, INPATH=237, STRING=238, 
		BIGINT_LITERAL=239, SMALLINT_LITERAL=240, TINYINT_LITERAL=241, INTEGER_VALUE=242, 
		DECIMAL_VALUE=243, DOUBLE_LITERAL=244, BIGDECIMAL_LITERAL=245, IDENTIFIER=246, 
		BACKQUOTED_IDENTIFIER=247, SIMPLE_COMMENT=248, BRACKETED_EMPTY_COMMENT=249, 
		BRACKETED_COMMENT=250, WS=251, UNRECOGNIZED=252;
	public static final int
		RULE_singleStatement = 0, RULE_statement = 1, RULE_colListAndPk = 2, RULE_primaryKey = 3, 
		RULE_partitionFieldList = 4, RULE_partitionField = 5, RULE_singleExpression = 6, 
		RULE_singleTableIdentifier = 7, RULE_singleFunctionIdentifier = 8, RULE_singleDataType = 9, 
		RULE_singleTableSchema = 10, RULE_unsupportedHiveNativeCommands = 11, 
		RULE_createTableHeader = 12, RULE_bucketSpec = 13, RULE_skewSpec = 14, 
		RULE_locationSpec = 15, RULE_query = 16, RULE_insertInto = 17, RULE_partitionSpecLocation = 18, 
		RULE_partitionSpec = 19, RULE_partitionVal = 20, RULE_describeFuncName = 21, 
		RULE_describeColName = 22, RULE_ctes = 23, RULE_namedQuery = 24, RULE_tableProvider = 25, 
		RULE_tablePropertyList = 26, RULE_tableProperty = 27, RULE_tablePropertyKey = 28, 
		RULE_tablePropertyValue = 29, RULE_constantList = 30, RULE_nestedConstantList = 31, 
		RULE_createFileFormat = 32, RULE_fileFormat = 33, RULE_storageHandler = 34, 
		RULE_resource = 35, RULE_queryNoWith = 36, RULE_queryOrganization = 37, 
		RULE_multiInsertQueryBody = 38, RULE_queryTerm = 39, RULE_queryPrimary = 40, 
		RULE_sortItem = 41, RULE_querySpecification = 42, RULE_hint = 43, RULE_hintStatement = 44, 
		RULE_fromClause = 45, RULE_aggregation = 46, RULE_groupingSet = 47, RULE_lateralView = 48, 
		RULE_setQuantifier = 49, RULE_relation = 50, RULE_joinRelation = 51, RULE_joinType = 52, 
		RULE_joinCriteria = 53, RULE_sample = 54, RULE_sampleMethod = 55, RULE_identifierList = 56, 
		RULE_identifierSeq = 57, RULE_orderedIdentifierList = 58, RULE_orderedIdentifier = 59, 
		RULE_identifierCommentList = 60, RULE_identifierComment = 61, RULE_relationPrimary = 62, 
		RULE_inlineTable = 63, RULE_functionTable = 64, RULE_tableAlias = 65, 
		RULE_rowFormat = 66, RULE_tableIdentifier = 67, RULE_functionIdentifier = 68, 
		RULE_namedExpression = 69, RULE_namedExpressionSeq = 70, RULE_expression = 71, 
		RULE_booleanExpression = 72, RULE_predicated = 73, RULE_predicate = 74, 
		RULE_valueExpression = 75, RULE_primaryExpression = 76, RULE_constant = 77, 
		RULE_comparisonOperator = 78, RULE_arithmeticOperator = 79, RULE_predicateOperator = 80, 
		RULE_booleanValue = 81, RULE_interval = 82, RULE_intervalField = 83, RULE_intervalValue = 84, 
		RULE_colPosition = 85, RULE_dataType = 86, RULE_colTypeList = 87, RULE_colType = 88, 
		RULE_complexColTypeList = 89, RULE_complexColType = 90, RULE_whenClause = 91, 
		RULE_windows = 92, RULE_namedWindow = 93, RULE_windowSpec = 94, RULE_windowFrame = 95, 
		RULE_frameBound = 96, RULE_qualifiedName = 97, RULE_identifier = 98, RULE_strictIdentifier = 99, 
		RULE_quotedIdentifier = 100, RULE_number = 101, RULE_nonReserved = 102;
	private static String[] makeRuleNames() {
		return new String[] {
			"singleStatement", "statement", "colListAndPk", "primaryKey", "partitionFieldList", 
			"partitionField", "singleExpression", "singleTableIdentifier", "singleFunctionIdentifier", 
			"singleDataType", "singleTableSchema", "unsupportedHiveNativeCommands", 
			"createTableHeader", "bucketSpec", "skewSpec", "locationSpec", "query", 
			"insertInto", "partitionSpecLocation", "partitionSpec", "partitionVal", 
			"describeFuncName", "describeColName", "ctes", "namedQuery", "tableProvider", 
			"tablePropertyList", "tableProperty", "tablePropertyKey", "tablePropertyValue", 
			"constantList", "nestedConstantList", "createFileFormat", "fileFormat", 
			"storageHandler", "resource", "queryNoWith", "queryOrganization", "multiInsertQueryBody", 
			"queryTerm", "queryPrimary", "sortItem", "querySpecification", "hint", 
			"hintStatement", "fromClause", "aggregation", "groupingSet", "lateralView", 
			"setQuantifier", "relation", "joinRelation", "joinType", "joinCriteria", 
			"sample", "sampleMethod", "identifierList", "identifierSeq", "orderedIdentifierList", 
			"orderedIdentifier", "identifierCommentList", "identifierComment", "relationPrimary", 
			"inlineTable", "functionTable", "tableAlias", "rowFormat", "tableIdentifier", 
			"functionIdentifier", "namedExpression", "namedExpressionSeq", "expression", 
			"booleanExpression", "predicated", "predicate", "valueExpression", "primaryExpression", 
			"constant", "comparisonOperator", "arithmeticOperator", "predicateOperator", 
			"booleanValue", "interval", "intervalField", "intervalValue", "colPosition", 
			"dataType", "colTypeList", "colType", "complexColTypeList", "complexColType", 
			"whenClause", "windows", "namedWindow", "windowSpec", "windowFrame", 
			"frameBound", "qualifiedName", "identifier", "strictIdentifier", "quotedIdentifier", 
			"number", "nonReserved"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "','", "')'", "'.'", "'/*+'", "'*/'", "'['", "']'", "':'", 
			"'PRIMARY'", "'KEY'", "'SELECT'", "'FROM'", "'ADD'", "'AS'", "'ALL'", 
			"'DISTINCT'", "'WHERE'", "'GROUP'", "'BY'", "'GROUPING'", "'SETS'", "'CUBE'", 
			"'ROLLUP'", "'ORDER'", "'HAVING'", "'LIMIT'", "'AT'", "'OR'", "'AND'", 
			"'IN'", null, "'NO'", "'EXISTS'", "'BETWEEN'", "'LIKE'", null, "'IS'", 
			"'NULL'", "'TRUE'", "'FALSE'", "'NULLS'", "'ASC'", "'DESC'", "'FOR'", 
			"'INTERVAL'", "'CASE'", "'WHEN'", "'THEN'", "'ELSE'", "'END'", "'JOIN'", 
			"'CROSS'", "'OUTER'", "'INNER'", "'LEFT'", "'SEMI'", "'RIGHT'", "'FULL'", 
			"'NATURAL'", "'ON'", "'LATERAL'", "'WINDOW'", "'OVER'", "'PARTITION'", 
			"'RANGE'", "'ROWS'", "'UNBOUNDED'", "'PRECEDING'", "'FOLLOWING'", "'CURRENT'", 
			"'FIRST'", "'AFTER'", "'LAST'", "'ROW'", "'WITH'", "'VALUES'", "'CREATE'", 
			"'TABLE'", "'DIRECTORY'", "'VIEW'", "'REPLACE'", "'INSERT'", "'DELETE'", 
			"'INTO'", "'DESCRIBE'", "'EXPLAIN'", "'FORMAT'", "'LOGICAL'", "'CODEGEN'", 
			"'COST'", "'CAST'", "'SHOW'", "'TABLES'", "'COLUMNS'", "'COLUMN'", "'USE'", 
			"'PARTITIONS'", "'FUNCTIONS'", "'DROP'", "'UNION'", "'EXCEPT'", "'MINUS'", 
			"'INTERSECT'", "'TO'", "'TABLESAMPLE'", "'STRATIFY'", "'ALTER'", "'RENAME'", 
			"'ARRAY'", "'MAP'", "'STRUCT'", "'COMMENT'", "'SET'", "'RESET'", "'DATA'", 
			"'START'", "'TRANSACTION'", "'COMMIT'", "'ROLLBACK'", "'MACRO'", "'IGNORE'", 
			"'BOTH'", "'LEADING'", "'TRAILING'", "'IF'", "'POSITION'", null, "'<=>'", 
			"'<>'", "'!='", "'<'", null, "'>'", null, "'+'", "'-'", "'*'", "'/'", 
			"'%'", "'DIV'", "'~'", "'&'", "'|'", "'||'", "'^'", "'PERCENT'", "'BUCKET'", 
			"'OUT'", "'OF'", "'SORT'", "'CLUSTER'", "'DISTRIBUTE'", "'OVERWRITE'", 
			"'TRANSFORM'", "'REDUCE'", "'USING'", "'SERDE'", "'SERDEPROPERTIES'", 
			"'RECORDREADER'", "'RECORDWRITER'", "'DELIMITED'", "'FIELDS'", "'TERMINATED'", 
			"'COLLECTION'", "'ITEMS'", "'KEYS'", "'ESCAPED'", "'LINES'", "'SEPARATED'", 
			"'FUNCTION'", "'EXTENDED'", "'REFRESH'", "'CLEAR'", "'CACHE'", "'UNCACHE'", 
			"'LAZY'", "'FORMATTED'", "'GLOBAL'", null, "'OPTIONS'", "'UNSET'", "'TBLPROPERTIES'", 
			"'DBPROPERTIES'", "'BUCKETS'", "'SKEWED'", "'STORED'", "'DIRECTORIES'", 
			"'LOCATION'", "'EXCHANGE'", "'ARCHIVE'", "'UNARCHIVE'", "'FILEFORMAT'", 
			"'TOUCH'", "'COMPACT'", "'CONCATENATE'", "'CHANGE'", "'CASCADE'", "'RESTRICT'", 
			"'CLUSTERED'", "'SORTED'", "'PURGE'", "'INPUTFORMAT'", "'OUTPUTFORMAT'", 
			null, null, "'DFS'", "'TRUNCATE'", "'ANALYZE'", "'COMPUTE'", "'LIST'", 
			"'STATISTICS'", "'PARTITIONED'", "'EXTERNAL'", "'DEFINED'", "'REVOKE'", 
			"'GRANT'", "'LOCK'", "'UNLOCK'", "'MSCK'", "'REPAIR'", "'RECOVER'", "'EXPORT'", 
			"'IMPORT'", "'LOAD'", "'ROLE'", "'ROLES'", "'COMPACTIONS'", "'PRINCIPALS'", 
			"'TRANSACTIONS'", "'INDEX'", "'INDEXES'", "'LOCKS'", "'OPTION'", "'ANTI'", 
			"'LOCAL'", "'INPATH'", null, null, null, null, null, null, null, null, 
			null, null, null, "'/**/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, "PRIMARY", 
			"KEY", "SELECT", "FROM", "ADD", "AS", "ALL", "DISTINCT", "WHERE", "GROUP", 
			"BY", "GROUPING", "SETS", "CUBE", "ROLLUP", "ORDER", "HAVING", "LIMIT", 
			"AT", "OR", "AND", "IN", "NOT", "NO", "EXISTS", "BETWEEN", "LIKE", "RLIKE", 
			"IS", "NULL", "TRUE", "FALSE", "NULLS", "ASC", "DESC", "FOR", "INTERVAL", 
			"CASE", "WHEN", "THEN", "ELSE", "END", "JOIN", "CROSS", "OUTER", "INNER", 
			"LEFT", "SEMI", "RIGHT", "FULL", "NATURAL", "ON", "LATERAL", "WINDOW", 
			"OVER", "PARTITION", "RANGE", "ROWS", "UNBOUNDED", "PRECEDING", "FOLLOWING", 
			"CURRENT", "FIRST", "AFTER", "LAST", "ROW", "WITH", "VALUES", "CREATE", 
			"TABLE", "DIRECTORY", "VIEW", "REPLACE", "INSERT", "DELETE", "INTO", 
			"DESCRIBE", "EXPLAIN", "FORMAT", "LOGICAL", "CODEGEN", "COST", "CAST", 
			"SHOW", "TABLES", "COLUMNS", "COLUMN", "USE", "PARTITIONS", "FUNCTIONS", 
			"DROP", "UNION", "EXCEPT", "SETMINUS", "INTERSECT", "TO", "TABLESAMPLE", 
			"STRATIFY", "ALTER", "RENAME", "ARRAY", "MAP", "STRUCT", "COMMENT", "SET", 
			"RESET", "DATA", "START", "TRANSACTION", "COMMIT", "ROLLBACK", "MACRO", 
			"IGNORE", "BOTH", "LEADING", "TRAILING", "IF", "POSITION", "EQ", "NSEQ", 
			"NEQ", "NEQJ", "LT", "LTE", "GT", "GTE", "PLUS", "MINUS", "ASTERISK", 
			"SLASH", "PERCENT", "DIV", "TILDE", "AMPERSAND", "PIPE", "CONCAT_PIPE", 
			"HAT", "PERCENTLIT", "BUCKET", "OUT", "OF", "SORT", "CLUSTER", "DISTRIBUTE", 
			"OVERWRITE", "TRANSFORM", "REDUCE", "USING", "SERDE", "SERDEPROPERTIES", 
			"RECORDREADER", "RECORDWRITER", "DELIMITED", "FIELDS", "TERMINATED", 
			"COLLECTION", "ITEMS", "KEYS", "ESCAPED", "LINES", "SEPARATED", "FUNCTION", 
			"EXTENDED", "REFRESH", "CLEAR", "CACHE", "UNCACHE", "LAZY", "FORMATTED", 
			"GLOBAL", "TEMPORARY", "OPTIONS", "UNSET", "TBLPROPERTIES", "DBPROPERTIES", 
			"BUCKETS", "SKEWED", "STORED", "DIRECTORIES", "LOCATION", "EXCHANGE", 
			"ARCHIVE", "UNARCHIVE", "FILEFORMAT", "TOUCH", "COMPACT", "CONCATENATE", 
			"CHANGE", "CASCADE", "RESTRICT", "CLUSTERED", "SORTED", "PURGE", "INPUTFORMAT", 
			"OUTPUTFORMAT", "DATABASE", "DATABASES", "DFS", "TRUNCATE", "ANALYZE", 
			"COMPUTE", "LIST", "STATISTICS", "PARTITIONED", "EXTERNAL", "DEFINED", 
			"REVOKE", "GRANT", "LOCK", "UNLOCK", "MSCK", "REPAIR", "RECOVER", "EXPORT", 
			"IMPORT", "LOAD", "ROLE", "ROLES", "COMPACTIONS", "PRINCIPALS", "TRANSACTIONS", 
			"INDEX", "INDEXES", "LOCKS", "OPTION", "ANTI", "LOCAL", "INPATH", "STRING", 
			"BIGINT_LITERAL", "SMALLINT_LITERAL", "TINYINT_LITERAL", "INTEGER_VALUE", 
			"DECIMAL_VALUE", "DOUBLE_LITERAL", "BIGDECIMAL_LITERAL", "IDENTIFIER", 
			"BACKQUOTED_IDENTIFIER", "SIMPLE_COMMENT", "BRACKETED_EMPTY_COMMENT", 
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
	public String getGrammarFileName() { return "ArcticSparkSql.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


	  /**
	   * Verify whether current token is a valid decimal token (which contains dot).
	   * Returns true if the character that follows the token is not a digit or letter or underscore.
	   *
	   * For example:
	   * For char stream "2.3", "2." is not a valid decimal token, because it is followed by digit '3'.
	   * For char stream "2.3_", "2.3" is not a valid decimal token, because it is followed by '_'.
	   * For char stream "2.3W", "2.3" is not a valid decimal token, because it is followed by 'W'.
	   * For char stream "12.0D 34.E2+0.12 "  12.0D is a valid decimal token because it is followed
	   * by a space. 34.E2 is a valid decimal token because it is followed by symbol '+'
	   * which is not a digit or letter or underscore.
	   */
	  public boolean isValidDecimal() {
	    int nextChar = _input.LA(1);
	    if (nextChar >= 'A' && nextChar <= 'Z' || nextChar >= '0' && nextChar <= '9' ||
	      nextChar == '_') {
	      return false;
	    } else {
	      return true;
	    }
	  }

	public ArcticSparkSqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class SingleStatementContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticSparkSqlParser.EOF, 0); }
		public SingleStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleStatementContext singleStatement() throws RecognitionException {
		SingleStatementContext _localctx = new SingleStatementContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_singleStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			statement();
			setState(207);
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
	public static class PassThroughContext extends StatementContext {
		public PassThroughContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPassThrough(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPassThrough(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPassThrough(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CreateArcticTableContext extends StatementContext {
		public TablePropertyListContext options;
		public PartitionFieldListContext partitionColumnNames;
		public Token comment;
		public TablePropertyListContext tableProps;
		public CreateTableHeaderContext createTableHeader() {
			return getRuleContext(CreateTableHeaderContext.class,0);
		}
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public ColListAndPkContext colListAndPk() {
			return getRuleContext(ColListAndPkContext.class,0);
		}
		public List<BucketSpecContext> bucketSpec() {
			return getRuleContexts(BucketSpecContext.class);
		}
		public BucketSpecContext bucketSpec(int i) {
			return getRuleContext(BucketSpecContext.class,i);
		}
		public List<LocationSpecContext> locationSpec() {
			return getRuleContexts(LocationSpecContext.class);
		}
		public LocationSpecContext locationSpec(int i) {
			return getRuleContext(LocationSpecContext.class,i);
		}
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public List<TerminalNode> OPTIONS() { return getTokens(ArcticSparkSqlParser.OPTIONS); }
		public TerminalNode OPTIONS(int i) {
			return getToken(ArcticSparkSqlParser.OPTIONS, i);
		}
		public List<TerminalNode> PARTITIONED() { return getTokens(ArcticSparkSqlParser.PARTITIONED); }
		public TerminalNode PARTITIONED(int i) {
			return getToken(ArcticSparkSqlParser.PARTITIONED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSparkSqlParser.BY, i);
		}
		public List<TerminalNode> COMMENT() { return getTokens(ArcticSparkSqlParser.COMMENT); }
		public TerminalNode COMMENT(int i) {
			return getToken(ArcticSparkSqlParser.COMMENT, i);
		}
		public List<TerminalNode> TBLPROPERTIES() { return getTokens(ArcticSparkSqlParser.TBLPROPERTIES); }
		public TerminalNode TBLPROPERTIES(int i) {
			return getToken(ArcticSparkSqlParser.TBLPROPERTIES, i);
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
		public List<TerminalNode> STRING() { return getTokens(ArcticSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSparkSqlParser.STRING, i);
		}
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public CreateArcticTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterCreateArcticTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitCreateArcticTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitCreateArcticTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		int _la;
		try {
			int _alt;
			setState(242);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				_localctx = new CreateArcticTableContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(209);
				createTableHeader();
				setState(211);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0 || _la==PRIMARY) {
					{
					setState(210);
					colListAndPk();
					}
				}

				setState(213);
				tableProvider();
				setState(227);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMENT || ((((_la - 181)) & ~0x3f) == 0 && ((1L << (_la - 181)) & ((1L << (OPTIONS - 181)) | (1L << (TBLPROPERTIES - 181)) | (1L << (LOCATION - 181)) | (1L << (CLUSTERED - 181)) | (1L << (PARTITIONED - 181)))) != 0)) {
					{
					setState(225);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case OPTIONS:
						{
						{
						setState(214);
						match(OPTIONS);
						setState(215);
						((CreateArcticTableContext)_localctx).options = tablePropertyList();
						}
						}
						break;
					case PARTITIONED:
						{
						{
						setState(216);
						match(PARTITIONED);
						setState(217);
						match(BY);
						setState(218);
						((CreateArcticTableContext)_localctx).partitionColumnNames = partitionFieldList();
						}
						}
						break;
					case CLUSTERED:
						{
						setState(219);
						bucketSpec();
						}
						break;
					case LOCATION:
						{
						setState(220);
						locationSpec();
						}
						break;
					case COMMENT:
						{
						{
						setState(221);
						match(COMMENT);
						setState(222);
						((CreateArcticTableContext)_localctx).comment = match(STRING);
						}
						}
						break;
					case TBLPROPERTIES:
						{
						{
						setState(223);
						match(TBLPROPERTIES);
						setState(224);
						((CreateArcticTableContext)_localctx).tableProps = tablePropertyList();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(229);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(234);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << AS))) != 0) || ((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & ((1L << (WITH - 76)) | (1L << (VALUES - 76)) | (1L << (TABLE - 76)) | (1L << (INSERT - 76)) | (1L << (MAP - 76)))) != 0) || _la==REDUCE) {
					{
					setState(231);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(230);
						match(AS);
						}
					}

					setState(233);
					query();
					}
				}

				}
				break;
			case 2:
				_localctx = new PassThroughContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(239);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
				while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1+1 ) {
						{
						{
						setState(236);
						matchWildcard();
						}
						} 
					}
					setState(241);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
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
		public PrimaryKeyContext primaryKey() {
			return getRuleContext(PrimaryKeyContext.class,0);
		}
		public ColListOnlyPkContext(ColListAndPkContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterColListOnlyPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitColListOnlyPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitColListOnlyPk(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ColListWithPkContext extends ColListAndPkContext {
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public PrimaryKeyContext primaryKey() {
			return getRuleContext(PrimaryKeyContext.class,0);
		}
		public ColListWithPkContext(ColListAndPkContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterColListWithPk(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitColListWithPk(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitColListWithPk(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColListAndPkContext colListAndPk() throws RecognitionException {
		ColListAndPkContext _localctx = new ColListAndPkContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_colListAndPk);
		int _la;
		try {
			setState(253);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				_localctx = new ColListWithPkContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(244);
				match(T__0);
				setState(245);
				colTypeList();
				setState(248);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(246);
					match(T__1);
					setState(247);
					primaryKey();
					}
				}

				setState(250);
				match(T__2);
				}
				break;
			case PRIMARY:
				_localctx = new ColListOnlyPkContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(252);
				primaryKey();
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

	public static class PrimaryKeyContext extends ParserRuleContext {
		public TerminalNode PRIMARY() { return getToken(ArcticSparkSqlParser.PRIMARY, 0); }
		public TerminalNode KEY() { return getToken(ArcticSparkSqlParser.KEY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public PrimaryKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPrimaryKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPrimaryKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPrimaryKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryKeyContext primaryKey() throws RecognitionException {
		PrimaryKeyContext _localctx = new PrimaryKeyContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_primaryKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(PRIMARY);
			setState(256);
			match(KEY);
			setState(257);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPartitionFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPartitionFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPartitionFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldListContext partitionFieldList() throws RecognitionException {
		PartitionFieldListContext _localctx = new PartitionFieldListContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_partitionFieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259);
			match(T__0);
			setState(260);
			((PartitionFieldListContext)_localctx).partitionField = partitionField();
			((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
			setState(265);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(261);
				match(T__1);
				setState(262);
				((PartitionFieldListContext)_localctx).partitionField = partitionField();
				((PartitionFieldListContext)_localctx).fields.add(((PartitionFieldListContext)_localctx).partitionField);
				}
				}
				setState(267);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(268);
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
	public static class PartitionColumnRefContext extends PartitionFieldContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public PartitionColumnRefContext(PartitionFieldContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPartitionColumnRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPartitionColumnRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPartitionColumnRef(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PartitionColumnDefineContext extends PartitionFieldContext {
		public ColTypeContext colType() {
			return getRuleContext(ColTypeContext.class,0);
		}
		public PartitionColumnDefineContext(PartitionFieldContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPartitionColumnDefine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPartitionColumnDefine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPartitionColumnDefine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionFieldContext partitionField() throws RecognitionException {
		PartitionFieldContext _localctx = new PartitionFieldContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_partitionField);
		try {
			setState(272);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				_localctx = new PartitionColumnRefContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(270);
				identifier();
				}
				break;
			case 2:
				_localctx = new PartitionColumnDefineContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(271);
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

	public static class SingleExpressionContext extends ParserRuleContext {
		public NamedExpressionContext namedExpression() {
			return getRuleContext(NamedExpressionContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ArcticSparkSqlParser.EOF, 0); }
		public SingleExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleExpressionContext singleExpression() throws RecognitionException {
		SingleExpressionContext _localctx = new SingleExpressionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_singleExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(274);
			namedExpression();
			setState(275);
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
		public TerminalNode EOF() { return getToken(ArcticSparkSqlParser.EOF, 0); }
		public SingleTableIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleTableIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleTableIdentifierContext singleTableIdentifier() throws RecognitionException {
		SingleTableIdentifierContext _localctx = new SingleTableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_singleTableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			tableIdentifier();
			setState(278);
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
		public TerminalNode EOF() { return getToken(ArcticSparkSqlParser.EOF, 0); }
		public SingleFunctionIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleFunctionIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleFunctionIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleFunctionIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleFunctionIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleFunctionIdentifierContext singleFunctionIdentifier() throws RecognitionException {
		SingleFunctionIdentifierContext _localctx = new SingleFunctionIdentifierContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_singleFunctionIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			functionIdentifier();
			setState(281);
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
		public TerminalNode EOF() { return getToken(ArcticSparkSqlParser.EOF, 0); }
		public SingleDataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleDataType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleDataTypeContext singleDataType() throws RecognitionException {
		SingleDataTypeContext _localctx = new SingleDataTypeContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_singleDataType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(283);
			dataType();
			setState(284);
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
		public TerminalNode EOF() { return getToken(ArcticSparkSqlParser.EOF, 0); }
		public SingleTableSchemaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleTableSchema; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleTableSchema(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleTableSchema(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleTableSchema(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleTableSchemaContext singleTableSchema() throws RecognitionException {
		SingleTableSchemaContext _localctx = new SingleTableSchemaContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_singleTableSchema);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			colTypeList();
			setState(287);
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

	public static class UnsupportedHiveNativeCommandsContext extends ParserRuleContext {
		public Token kw1;
		public Token kw2;
		public Token kw3;
		public Token kw4;
		public Token kw5;
		public Token kw6;
		public TerminalNode CREATE() { return getToken(ArcticSparkSqlParser.CREATE, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSparkSqlParser.ROLE, 0); }
		public TerminalNode DROP() { return getToken(ArcticSparkSqlParser.DROP, 0); }
		public TerminalNode GRANT() { return getToken(ArcticSparkSqlParser.GRANT, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSparkSqlParser.REVOKE, 0); }
		public TerminalNode SHOW() { return getToken(ArcticSparkSqlParser.SHOW, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSparkSqlParser.PRINCIPALS, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSparkSqlParser.ROLES, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSparkSqlParser.CURRENT, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSparkSqlParser.EXPORT, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSparkSqlParser.TABLE, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSparkSqlParser.IMPORT, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSparkSqlParser.COMPACTIONS, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSparkSqlParser.TRANSACTIONS, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSparkSqlParser.INDEXES, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSparkSqlParser.LOCKS, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSparkSqlParser.INDEX, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSparkSqlParser.ALTER, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSparkSqlParser.LOCK, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSparkSqlParser.DATABASE, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSparkSqlParser.UNLOCK, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSparkSqlParser.MACRO, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSparkSqlParser.CLUSTERED, 0); }
		public TerminalNode BY() { return getToken(ArcticSparkSqlParser.BY, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSparkSqlParser.SORTED, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSparkSqlParser.SKEWED, 0); }
		public TerminalNode STORED() { return getToken(ArcticSparkSqlParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSparkSqlParser.DIRECTORIES, 0); }
		public TerminalNode SET() { return getToken(ArcticSparkSqlParser.SET, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSparkSqlParser.LOCATION, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSparkSqlParser.EXCHANGE, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSparkSqlParser.PARTITION, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSparkSqlParser.ARCHIVE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSparkSqlParser.UNARCHIVE, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSparkSqlParser.TOUCH, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSparkSqlParser.COMPACT, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode CONCATENATE() { return getToken(ArcticSparkSqlParser.CONCATENATE, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSparkSqlParser.FILEFORMAT, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSparkSqlParser.REPLACE, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSparkSqlParser.COLUMNS, 0); }
		public TerminalNode START() { return getToken(ArcticSparkSqlParser.START, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSparkSqlParser.TRANSACTION, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSparkSqlParser.COMMIT, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSparkSqlParser.ROLLBACK, 0); }
		public TerminalNode DFS() { return getToken(ArcticSparkSqlParser.DFS, 0); }
		public TerminalNode DELETE() { return getToken(ArcticSparkSqlParser.DELETE, 0); }
		public TerminalNode FROM() { return getToken(ArcticSparkSqlParser.FROM, 0); }
		public UnsupportedHiveNativeCommandsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unsupportedHiveNativeCommands; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterUnsupportedHiveNativeCommands(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitUnsupportedHiveNativeCommands(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitUnsupportedHiveNativeCommands(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnsupportedHiveNativeCommandsContext unsupportedHiveNativeCommands() throws RecognitionException {
		UnsupportedHiveNativeCommandsContext _localctx = new UnsupportedHiveNativeCommandsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_unsupportedHiveNativeCommands);
		int _la;
		try {
			setState(459);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(289);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(290);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(291);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(292);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(293);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(GRANT);
				setState(295);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ROLE) {
					{
					setState(294);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
					}
				}

				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(297);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(REVOKE);
				setState(299);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ROLE) {
					{
					setState(298);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
					}
				}

				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(301);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(302);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(GRANT);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(303);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(304);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLE);
				setState(306);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GRANT) {
					{
					setState(305);
					((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(GRANT);
					}
				}

				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(308);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(309);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(PRINCIPALS);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(310);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(311);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(ROLES);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(312);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(313);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(CURRENT);
				setState(314);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(ROLES);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(315);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(EXPORT);
				setState(316);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(317);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(IMPORT);
				setState(318);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(319);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(320);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(COMPACTIONS);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(321);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(322);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(CREATE);
				setState(323);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(TABLE);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(324);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(325);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TRANSACTIONS);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(326);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(327);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEXES);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(328);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(SHOW);
				setState(329);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(LOCKS);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(330);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(331);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(332);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(333);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(334);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(335);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(INDEX);
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(336);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(LOCK);
				setState(337);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(338);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(LOCK);
				setState(339);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(DATABASE);
				}
				break;
			case 22:
				enterOuterAlt(_localctx, 22);
				{
				setState(340);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(UNLOCK);
				setState(341);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				}
				break;
			case 23:
				enterOuterAlt(_localctx, 23);
				{
				setState(342);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(UNLOCK);
				setState(343);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(DATABASE);
				}
				break;
			case 24:
				enterOuterAlt(_localctx, 24);
				{
				setState(344);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(CREATE);
				setState(345);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TEMPORARY);
				setState(346);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(MACRO);
				}
				break;
			case 25:
				enterOuterAlt(_localctx, 25);
				{
				setState(347);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DROP);
				setState(348);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TEMPORARY);
				setState(349);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(MACRO);
				}
				break;
			case 26:
				enterOuterAlt(_localctx, 26);
				{
				setState(350);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(351);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(352);
				tableIdentifier();
				setState(353);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(354);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(CLUSTERED);
				}
				break;
			case 27:
				enterOuterAlt(_localctx, 27);
				{
				setState(356);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(357);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(358);
				tableIdentifier();
				setState(359);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(CLUSTERED);
				setState(360);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(BY);
				}
				break;
			case 28:
				enterOuterAlt(_localctx, 28);
				{
				setState(362);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(363);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(364);
				tableIdentifier();
				setState(365);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(366);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SORTED);
				}
				break;
			case 29:
				enterOuterAlt(_localctx, 29);
				{
				setState(368);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(369);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(370);
				tableIdentifier();
				setState(371);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SKEWED);
				setState(372);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(BY);
				}
				break;
			case 30:
				enterOuterAlt(_localctx, 30);
				{
				setState(374);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(375);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(376);
				tableIdentifier();
				setState(377);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(378);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SKEWED);
				}
				break;
			case 31:
				enterOuterAlt(_localctx, 31);
				{
				setState(380);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(381);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(382);
				tableIdentifier();
				setState(383);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(NOT);
				setState(384);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(STORED);
				setState(385);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw5 = match(AS);
				setState(386);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw6 = match(DIRECTORIES);
				}
				break;
			case 32:
				enterOuterAlt(_localctx, 32);
				{
				setState(388);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(389);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(390);
				tableIdentifier();
				setState(391);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SET);
				setState(392);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(SKEWED);
				setState(393);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw5 = match(LOCATION);
				}
				break;
			case 33:
				enterOuterAlt(_localctx, 33);
				{
				setState(395);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(396);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(397);
				tableIdentifier();
				setState(398);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(EXCHANGE);
				setState(399);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 34:
				enterOuterAlt(_localctx, 34);
				{
				setState(401);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(402);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(403);
				tableIdentifier();
				setState(404);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(ARCHIVE);
				setState(405);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 35:
				enterOuterAlt(_localctx, 35);
				{
				setState(407);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(408);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(409);
				tableIdentifier();
				setState(410);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(UNARCHIVE);
				setState(411);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(PARTITION);
				}
				break;
			case 36:
				enterOuterAlt(_localctx, 36);
				{
				setState(413);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(414);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(415);
				tableIdentifier();
				setState(416);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(TOUCH);
				}
				break;
			case 37:
				enterOuterAlt(_localctx, 37);
				{
				setState(418);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(419);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(420);
				tableIdentifier();
				setState(422);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(421);
					partitionSpec();
					}
				}

				setState(424);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(COMPACT);
				}
				break;
			case 38:
				enterOuterAlt(_localctx, 38);
				{
				setState(426);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(427);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(428);
				tableIdentifier();
				setState(430);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(429);
					partitionSpec();
					}
				}

				setState(432);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(CONCATENATE);
				}
				break;
			case 39:
				enterOuterAlt(_localctx, 39);
				{
				setState(434);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(435);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(436);
				tableIdentifier();
				setState(438);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(437);
					partitionSpec();
					}
				}

				setState(440);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(SET);
				setState(441);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(FILEFORMAT);
				}
				break;
			case 40:
				enterOuterAlt(_localctx, 40);
				{
				setState(443);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ALTER);
				setState(444);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TABLE);
				setState(445);
				tableIdentifier();
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
				((UnsupportedHiveNativeCommandsContext)_localctx).kw3 = match(REPLACE);
				setState(450);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw4 = match(COLUMNS);
				}
				break;
			case 41:
				enterOuterAlt(_localctx, 41);
				{
				setState(452);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(START);
				setState(453);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(TRANSACTION);
				}
				break;
			case 42:
				enterOuterAlt(_localctx, 42);
				{
				setState(454);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(COMMIT);
				}
				break;
			case 43:
				enterOuterAlt(_localctx, 43);
				{
				setState(455);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(ROLLBACK);
				}
				break;
			case 44:
				enterOuterAlt(_localctx, 44);
				{
				setState(456);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DFS);
				}
				break;
			case 45:
				enterOuterAlt(_localctx, 45);
				{
				setState(457);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw1 = match(DELETE);
				setState(458);
				((UnsupportedHiveNativeCommandsContext)_localctx).kw2 = match(FROM);
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
		public TerminalNode CREATE() { return getToken(ArcticSparkSqlParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSparkSqlParser.TABLE, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TerminalNode TEMPORARY() { return getToken(ArcticSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSparkSqlParser.EXTERNAL, 0); }
		public TerminalNode IF() { return getToken(ArcticSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSparkSqlParser.EXISTS, 0); }
		public CreateTableHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterCreateTableHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitCreateTableHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitCreateTableHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateTableHeaderContext createTableHeader() throws RecognitionException {
		CreateTableHeaderContext _localctx = new CreateTableHeaderContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_createTableHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(461);
			match(CREATE);
			setState(463);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TEMPORARY) {
				{
				setState(462);
				match(TEMPORARY);
				}
			}

			setState(466);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTERNAL) {
				{
				setState(465);
				match(EXTERNAL);
				}
			}

			setState(468);
			match(TABLE);
			setState(472);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(469);
				match(IF);
				setState(470);
				match(NOT);
				setState(471);
				match(EXISTS);
				}
				break;
			}
			setState(474);
			tableIdentifier();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode CLUSTERED() { return getToken(ArcticSparkSqlParser.CLUSTERED, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSparkSqlParser.BY, i);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode INTO() { return getToken(ArcticSparkSqlParser.INTO, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSparkSqlParser.BUCKETS, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSparkSqlParser.SORTED, 0); }
		public OrderedIdentifierListContext orderedIdentifierList() {
			return getRuleContext(OrderedIdentifierListContext.class,0);
		}
		public BucketSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bucketSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterBucketSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitBucketSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitBucketSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BucketSpecContext bucketSpec() throws RecognitionException {
		BucketSpecContext _localctx = new BucketSpecContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_bucketSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(476);
			match(CLUSTERED);
			setState(477);
			match(BY);
			setState(478);
			identifierList();
			setState(482);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SORTED) {
				{
				setState(479);
				match(SORTED);
				setState(480);
				match(BY);
				setState(481);
				orderedIdentifierList();
				}
			}

			setState(484);
			match(INTO);
			setState(485);
			match(INTEGER_VALUE);
			setState(486);
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
		public TerminalNode SKEWED() { return getToken(ArcticSparkSqlParser.SKEWED, 0); }
		public TerminalNode BY() { return getToken(ArcticSparkSqlParser.BY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode ON() { return getToken(ArcticSparkSqlParser.ON, 0); }
		public ConstantListContext constantList() {
			return getRuleContext(ConstantListContext.class,0);
		}
		public NestedConstantListContext nestedConstantList() {
			return getRuleContext(NestedConstantListContext.class,0);
		}
		public TerminalNode STORED() { return getToken(ArcticSparkSqlParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSparkSqlParser.DIRECTORIES, 0); }
		public SkewSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skewSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSkewSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSkewSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSkewSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SkewSpecContext skewSpec() throws RecognitionException {
		SkewSpecContext _localctx = new SkewSpecContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_skewSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(488);
			match(SKEWED);
			setState(489);
			match(BY);
			setState(490);
			identifierList();
			setState(491);
			match(ON);
			setState(494);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				{
				setState(492);
				constantList();
				}
				break;
			case 2:
				{
				setState(493);
				nestedConstantList();
				}
				break;
			}
			setState(499);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STORED) {
				{
				setState(496);
				match(STORED);
				setState(497);
				match(AS);
				setState(498);
				match(DIRECTORIES);
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

	public static class LocationSpecContext extends ParserRuleContext {
		public TerminalNode LOCATION() { return getToken(ArcticSparkSqlParser.LOCATION, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public LocationSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_locationSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterLocationSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitLocationSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitLocationSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocationSpecContext locationSpec() throws RecognitionException {
		LocationSpecContext _localctx = new LocationSpecContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_locationSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(501);
			match(LOCATION);
			setState(502);
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
		public QueryNoWithContext queryNoWith() {
			return getRuleContext(QueryNoWithContext.class,0);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(504);
				ctes();
				}
			}

			setState(507);
			queryNoWith();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode INSERT() { return getToken(ArcticSparkSqlParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSparkSqlParser.DIRECTORY, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSparkSqlParser.LOCAL, 0); }
		public RowFormatContext rowFormat() {
			return getRuleContext(RowFormatContext.class,0);
		}
		public CreateFileFormatContext createFileFormat() {
			return getRuleContext(CreateFileFormatContext.class,0);
		}
		public InsertOverwriteHiveDirContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInsertOverwriteHiveDir(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInsertOverwriteHiveDir(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInsertOverwriteHiveDir(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertOverwriteDirContext extends InsertIntoContext {
		public Token path;
		public TablePropertyListContext options;
		public TerminalNode INSERT() { return getToken(ArcticSparkSqlParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSparkSqlParser.DIRECTORY, 0); }
		public TableProviderContext tableProvider() {
			return getRuleContext(TableProviderContext.class,0);
		}
		public TerminalNode LOCAL() { return getToken(ArcticSparkSqlParser.LOCAL, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSparkSqlParser.OPTIONS, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public InsertOverwriteDirContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInsertOverwriteDir(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInsertOverwriteDir(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInsertOverwriteDir(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertOverwriteTableContext extends InsertIntoContext {
		public TerminalNode INSERT() { return getToken(ArcticSparkSqlParser.INSERT, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSparkSqlParser.TABLE, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public TerminalNode IF() { return getToken(ArcticSparkSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSparkSqlParser.EXISTS, 0); }
		public InsertOverwriteTableContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInsertOverwriteTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInsertOverwriteTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInsertOverwriteTable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InsertIntoTableContext extends InsertIntoContext {
		public TerminalNode INSERT() { return getToken(ArcticSparkSqlParser.INSERT, 0); }
		public TerminalNode INTO() { return getToken(ArcticSparkSqlParser.INTO, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(ArcticSparkSqlParser.TABLE, 0); }
		public PartitionSpecContext partitionSpec() {
			return getRuleContext(PartitionSpecContext.class,0);
		}
		public InsertIntoTableContext(InsertIntoContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInsertIntoTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInsertIntoTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInsertIntoTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertIntoContext insertInto() throws RecognitionException {
		InsertIntoContext _localctx = new InsertIntoContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_insertInto);
		int _la;
		try {
			setState(557);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				_localctx = new InsertOverwriteTableContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(509);
				match(INSERT);
				setState(510);
				match(OVERWRITE);
				setState(511);
				match(TABLE);
				setState(512);
				tableIdentifier();
				setState(519);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(513);
					partitionSpec();
					setState(517);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==IF) {
						{
						setState(514);
						match(IF);
						setState(515);
						match(NOT);
						setState(516);
						match(EXISTS);
						}
					}

					}
				}

				}
				break;
			case 2:
				_localctx = new InsertIntoTableContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(521);
				match(INSERT);
				setState(522);
				match(INTO);
				setState(524);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
				case 1:
					{
					setState(523);
					match(TABLE);
					}
					break;
				}
				setState(526);
				tableIdentifier();
				setState(528);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITION) {
					{
					setState(527);
					partitionSpec();
					}
				}

				}
				break;
			case 3:
				_localctx = new InsertOverwriteHiveDirContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(530);
				match(INSERT);
				setState(531);
				match(OVERWRITE);
				setState(533);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(532);
					match(LOCAL);
					}
				}

				setState(535);
				match(DIRECTORY);
				setState(536);
				((InsertOverwriteHiveDirContext)_localctx).path = match(STRING);
				setState(538);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ROW) {
					{
					setState(537);
					rowFormat();
					}
				}

				setState(541);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STORED) {
					{
					setState(540);
					createFileFormat();
					}
				}

				}
				break;
			case 4:
				_localctx = new InsertOverwriteDirContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(543);
				match(INSERT);
				setState(544);
				match(OVERWRITE);
				setState(546);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LOCAL) {
					{
					setState(545);
					match(LOCAL);
					}
				}

				setState(548);
				match(DIRECTORY);
				setState(550);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STRING) {
					{
					setState(549);
					((InsertOverwriteDirContext)_localctx).path = match(STRING);
					}
				}

				setState(552);
				tableProvider();
				setState(555);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(553);
					match(OPTIONS);
					setState(554);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPartitionSpecLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPartitionSpecLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPartitionSpecLocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionSpecLocationContext partitionSpecLocation() throws RecognitionException {
		PartitionSpecLocationContext _localctx = new PartitionSpecLocationContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_partitionSpecLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(559);
			partitionSpec();
			setState(561);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LOCATION) {
				{
				setState(560);
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
		public TerminalNode PARTITION() { return getToken(ArcticSparkSqlParser.PARTITION, 0); }
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPartitionSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPartitionSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPartitionSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionSpecContext partitionSpec() throws RecognitionException {
		PartitionSpecContext _localctx = new PartitionSpecContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_partitionSpec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(563);
			match(PARTITION);
			setState(564);
			match(T__0);
			setState(565);
			partitionVal();
			setState(570);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(566);
				match(T__1);
				setState(567);
				partitionVal();
				}
				}
				setState(572);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(573);
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
		public TerminalNode EQ() { return getToken(ArcticSparkSqlParser.EQ, 0); }
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public PartitionValContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_partitionVal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPartitionVal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPartitionVal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPartitionVal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PartitionValContext partitionVal() throws RecognitionException {
		PartitionValContext _localctx = new PartitionValContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_partitionVal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(575);
			identifier();
			setState(578);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EQ) {
				{
				setState(576);
				match(EQ);
				setState(577);
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

	public static class DescribeFuncNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterDescribeFuncName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitDescribeFuncName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitDescribeFuncName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeFuncNameContext describeFuncName() throws RecognitionException {
		DescribeFuncNameContext _localctx = new DescribeFuncNameContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_describeFuncName);
		try {
			setState(585);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(580);
				qualifiedName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(581);
				match(STRING);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(582);
				comparisonOperator();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(583);
				arithmeticOperator();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(584);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterDescribeColName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitDescribeColName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitDescribeColName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescribeColNameContext describeColName() throws RecognitionException {
		DescribeColNameContext _localctx = new DescribeColNameContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_describeColName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(587);
			((DescribeColNameContext)_localctx).identifier = identifier();
			((DescribeColNameContext)_localctx).nameParts.add(((DescribeColNameContext)_localctx).identifier);
			setState(592);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__3) {
				{
				{
				setState(588);
				match(T__3);
				setState(589);
				((DescribeColNameContext)_localctx).identifier = identifier();
				((DescribeColNameContext)_localctx).nameParts.add(((DescribeColNameContext)_localctx).identifier);
				}
				}
				setState(594);
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
		public TerminalNode WITH() { return getToken(ArcticSparkSqlParser.WITH, 0); }
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterCtes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitCtes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitCtes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtesContext ctes() throws RecognitionException {
		CtesContext _localctx = new CtesContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_ctes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(595);
			match(WITH);
			setState(596);
			namedQuery();
			setState(601);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(597);
				match(T__1);
				setState(598);
				namedQuery();
				}
				}
				setState(603);
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
		public IdentifierContext name;
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public NamedQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNamedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNamedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNamedQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedQueryContext namedQuery() throws RecognitionException {
		NamedQueryContext _localctx = new NamedQueryContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_namedQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(604);
			((NamedQueryContext)_localctx).name = identifier();
			setState(606);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(605);
				match(AS);
				}
			}

			setState(608);
			match(T__0);
			setState(609);
			query();
			setState(610);
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
		public TerminalNode USING() { return getToken(ArcticSparkSqlParser.USING, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TableProviderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProvider; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableProvider(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableProvider(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableProvider(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableProviderContext tableProvider() throws RecognitionException {
		TableProviderContext _localctx = new TableProviderContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_tableProvider);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(612);
			match(USING);
			setState(613);
			qualifiedName();
			}
		}
		catch (RecognitionException re) {
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTablePropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTablePropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTablePropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyListContext tablePropertyList() throws RecognitionException {
		TablePropertyListContext _localctx = new TablePropertyListContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_tablePropertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(615);
			match(T__0);
			setState(616);
			tableProperty();
			setState(621);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(617);
				match(T__1);
				setState(618);
				tableProperty();
				}
				}
				setState(623);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(624);
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
		public TerminalNode EQ() { return getToken(ArcticSparkSqlParser.EQ, 0); }
		public TablePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyContext tableProperty() throws RecognitionException {
		TablePropertyContext _localctx = new TablePropertyContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_tableProperty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(626);
			((TablePropertyContext)_localctx).key = tablePropertyKey();
			setState(631);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TRUE || _la==FALSE || _la==EQ || ((((_la - 238)) & ~0x3f) == 0 && ((1L << (_la - 238)) & ((1L << (STRING - 238)) | (1L << (INTEGER_VALUE - 238)) | (1L << (DECIMAL_VALUE - 238)))) != 0)) {
				{
				setState(628);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(627);
					match(EQ);
					}
				}

				setState(630);
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
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TablePropertyKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTablePropertyKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTablePropertyKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTablePropertyKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyKeyContext tablePropertyKey() throws RecognitionException {
		TablePropertyKeyContext _localctx = new TablePropertyKeyContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_tablePropertyKey);
		int _la;
		try {
			setState(642);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
			case FROM:
			case ADD:
			case AS:
			case ALL:
			case DISTINCT:
			case WHERE:
			case GROUP:
			case BY:
			case GROUPING:
			case SETS:
			case CUBE:
			case ROLLUP:
			case ORDER:
			case HAVING:
			case LIMIT:
			case AT:
			case OR:
			case AND:
			case IN:
			case NOT:
			case NO:
			case EXISTS:
			case BETWEEN:
			case LIKE:
			case RLIKE:
			case IS:
			case NULL:
			case TRUE:
			case FALSE:
			case NULLS:
			case ASC:
			case DESC:
			case FOR:
			case INTERVAL:
			case CASE:
			case WHEN:
			case THEN:
			case ELSE:
			case END:
			case JOIN:
			case CROSS:
			case OUTER:
			case INNER:
			case LEFT:
			case SEMI:
			case RIGHT:
			case FULL:
			case NATURAL:
			case ON:
			case LATERAL:
			case WINDOW:
			case OVER:
			case PARTITION:
			case RANGE:
			case ROWS:
			case UNBOUNDED:
			case PRECEDING:
			case FOLLOWING:
			case CURRENT:
			case FIRST:
			case AFTER:
			case LAST:
			case ROW:
			case WITH:
			case VALUES:
			case CREATE:
			case TABLE:
			case DIRECTORY:
			case VIEW:
			case REPLACE:
			case INSERT:
			case DELETE:
			case INTO:
			case DESCRIBE:
			case EXPLAIN:
			case FORMAT:
			case LOGICAL:
			case CODEGEN:
			case COST:
			case CAST:
			case SHOW:
			case TABLES:
			case COLUMNS:
			case COLUMN:
			case USE:
			case PARTITIONS:
			case FUNCTIONS:
			case DROP:
			case UNION:
			case EXCEPT:
			case SETMINUS:
			case INTERSECT:
			case TO:
			case TABLESAMPLE:
			case STRATIFY:
			case ALTER:
			case RENAME:
			case ARRAY:
			case MAP:
			case STRUCT:
			case COMMENT:
			case SET:
			case RESET:
			case DATA:
			case START:
			case TRANSACTION:
			case COMMIT:
			case ROLLBACK:
			case MACRO:
			case IGNORE:
			case BOTH:
			case LEADING:
			case TRAILING:
			case IF:
			case POSITION:
			case DIV:
			case PERCENTLIT:
			case BUCKET:
			case OUT:
			case OF:
			case SORT:
			case CLUSTER:
			case DISTRIBUTE:
			case OVERWRITE:
			case TRANSFORM:
			case REDUCE:
			case SERDE:
			case SERDEPROPERTIES:
			case RECORDREADER:
			case RECORDWRITER:
			case DELIMITED:
			case FIELDS:
			case TERMINATED:
			case COLLECTION:
			case ITEMS:
			case KEYS:
			case ESCAPED:
			case LINES:
			case SEPARATED:
			case FUNCTION:
			case EXTENDED:
			case REFRESH:
			case CLEAR:
			case CACHE:
			case UNCACHE:
			case LAZY:
			case FORMATTED:
			case GLOBAL:
			case TEMPORARY:
			case OPTIONS:
			case UNSET:
			case TBLPROPERTIES:
			case DBPROPERTIES:
			case BUCKETS:
			case SKEWED:
			case STORED:
			case DIRECTORIES:
			case LOCATION:
			case EXCHANGE:
			case ARCHIVE:
			case UNARCHIVE:
			case FILEFORMAT:
			case TOUCH:
			case COMPACT:
			case CONCATENATE:
			case CHANGE:
			case CASCADE:
			case RESTRICT:
			case CLUSTERED:
			case SORTED:
			case PURGE:
			case INPUTFORMAT:
			case OUTPUTFORMAT:
			case DATABASE:
			case DATABASES:
			case DFS:
			case TRUNCATE:
			case ANALYZE:
			case COMPUTE:
			case LIST:
			case STATISTICS:
			case PARTITIONED:
			case EXTERNAL:
			case DEFINED:
			case REVOKE:
			case GRANT:
			case LOCK:
			case UNLOCK:
			case MSCK:
			case REPAIR:
			case RECOVER:
			case EXPORT:
			case IMPORT:
			case LOAD:
			case ROLE:
			case ROLES:
			case COMPACTIONS:
			case PRINCIPALS:
			case TRANSACTIONS:
			case INDEX:
			case INDEXES:
			case LOCKS:
			case OPTION:
			case ANTI:
			case LOCAL:
			case INPATH:
			case IDENTIFIER:
			case BACKQUOTED_IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(633);
				identifier();
				setState(638);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__3) {
					{
					{
					setState(634);
					match(T__3);
					setState(635);
					identifier();
					}
					}
					setState(640);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(641);
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

	public static class TablePropertyValueContext extends ParserRuleContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSparkSqlParser.DECIMAL_VALUE, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TablePropertyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTablePropertyValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTablePropertyValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTablePropertyValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyValueContext tablePropertyValue() throws RecognitionException {
		TablePropertyValueContext _localctx = new TablePropertyValueContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_tablePropertyValue);
		try {
			setState(648);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(644);
				match(INTEGER_VALUE);
				}
				break;
			case DECIMAL_VALUE:
				enterOuterAlt(_localctx, 2);
				{
				setState(645);
				match(DECIMAL_VALUE);
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 3);
				{
				setState(646);
				booleanValue();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(647);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantListContext constantList() throws RecognitionException {
		ConstantListContext _localctx = new ConstantListContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_constantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(650);
			match(T__0);
			setState(651);
			constant();
			setState(656);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(652);
				match(T__1);
				setState(653);
				constant();
				}
				}
				setState(658);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(659);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNestedConstantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNestedConstantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNestedConstantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NestedConstantListContext nestedConstantList() throws RecognitionException {
		NestedConstantListContext _localctx = new NestedConstantListContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_nestedConstantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(661);
			match(T__0);
			setState(662);
			constantList();
			setState(667);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(663);
				match(T__1);
				setState(664);
				constantList();
				}
				}
				setState(669);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(670);
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
		public TerminalNode STORED() { return getToken(ArcticSparkSqlParser.STORED, 0); }
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public FileFormatContext fileFormat() {
			return getRuleContext(FileFormatContext.class,0);
		}
		public TerminalNode BY() { return getToken(ArcticSparkSqlParser.BY, 0); }
		public StorageHandlerContext storageHandler() {
			return getRuleContext(StorageHandlerContext.class,0);
		}
		public CreateFileFormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createFileFormat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterCreateFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitCreateFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitCreateFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateFileFormatContext createFileFormat() throws RecognitionException {
		CreateFileFormatContext _localctx = new CreateFileFormatContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_createFileFormat);
		try {
			setState(678);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,52,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(672);
				match(STORED);
				setState(673);
				match(AS);
				setState(674);
				fileFormat();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(675);
				match(STORED);
				setState(676);
				match(BY);
				setState(677);
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
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSparkSqlParser.INPUTFORMAT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSparkSqlParser.OUTPUTFORMAT, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSparkSqlParser.STRING, i);
		}
		public TableFileFormatContext(FileFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableFileFormat(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterGenericFileFormat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitGenericFileFormat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitGenericFileFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileFormatContext fileFormat() throws RecognitionException {
		FileFormatContext _localctx = new FileFormatContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_fileFormat);
		try {
			setState(685);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,53,_ctx) ) {
			case 1:
				_localctx = new TableFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(680);
				match(INPUTFORMAT);
				setState(681);
				((TableFileFormatContext)_localctx).inFmt = match(STRING);
				setState(682);
				match(OUTPUTFORMAT);
				setState(683);
				((TableFileFormatContext)_localctx).outFmt = match(STRING);
				}
				break;
			case 2:
				_localctx = new GenericFileFormatContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(684);
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
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticSparkSqlParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSparkSqlParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public StorageHandlerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storageHandler; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterStorageHandler(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitStorageHandler(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitStorageHandler(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StorageHandlerContext storageHandler() throws RecognitionException {
		StorageHandlerContext _localctx = new StorageHandlerContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_storageHandler);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(687);
			match(STRING);
			setState(691);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(688);
				match(WITH);
				setState(689);
				match(SERDEPROPERTIES);
				setState(690);
				tablePropertyList();
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

	public static class ResourceContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public ResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitResource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceContext resource() throws RecognitionException {
		ResourceContext _localctx = new ResourceContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_resource);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(693);
			identifier();
			setState(694);
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

	public static class QueryNoWithContext extends ParserRuleContext {
		public QueryNoWithContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryNoWith; }
	 
		public QueryNoWithContext() { }
		public void copyFrom(QueryNoWithContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SingleInsertQueryContext extends QueryNoWithContext {
		public QueryTermContext queryTerm() {
			return getRuleContext(QueryTermContext.class,0);
		}
		public QueryOrganizationContext queryOrganization() {
			return getRuleContext(QueryOrganizationContext.class,0);
		}
		public InsertIntoContext insertInto() {
			return getRuleContext(InsertIntoContext.class,0);
		}
		public SingleInsertQueryContext(QueryNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSingleInsertQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSingleInsertQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSingleInsertQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MultiInsertQueryContext extends QueryNoWithContext {
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public List<MultiInsertQueryBodyContext> multiInsertQueryBody() {
			return getRuleContexts(MultiInsertQueryBodyContext.class);
		}
		public MultiInsertQueryBodyContext multiInsertQueryBody(int i) {
			return getRuleContext(MultiInsertQueryBodyContext.class,i);
		}
		public MultiInsertQueryContext(QueryNoWithContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterMultiInsertQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitMultiInsertQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitMultiInsertQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryNoWithContext queryNoWith() throws RecognitionException {
		QueryNoWithContext _localctx = new QueryNoWithContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_queryNoWith);
		int _la;
		try {
			setState(708);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
			case 1:
				_localctx = new SingleInsertQueryContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(697);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INSERT) {
					{
					setState(696);
					insertInto();
					}
				}

				setState(699);
				queryTerm(0);
				setState(700);
				queryOrganization();
				}
				break;
			case 2:
				_localctx = new MultiInsertQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(702);
				fromClause();
				setState(704); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(703);
					multiInsertQueryBody();
					}
					}
					setState(706); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==SELECT || _la==FROM || _la==INSERT || _la==MAP || _la==REDUCE );
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

	public static class QueryOrganizationContext extends ParserRuleContext {
		public SortItemContext sortItem;
		public List<SortItemContext> order = new ArrayList<SortItemContext>();
		public ExpressionContext expression;
		public List<ExpressionContext> clusterBy = new ArrayList<ExpressionContext>();
		public List<ExpressionContext> distributeBy = new ArrayList<ExpressionContext>();
		public List<SortItemContext> sort = new ArrayList<SortItemContext>();
		public ExpressionContext limit;
		public TerminalNode ORDER() { return getToken(ArcticSparkSqlParser.ORDER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSparkSqlParser.BY, i);
		}
		public TerminalNode CLUSTER() { return getToken(ArcticSparkSqlParser.CLUSTER, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode SORT() { return getToken(ArcticSparkSqlParser.SORT, 0); }
		public WindowsContext windows() {
			return getRuleContext(WindowsContext.class,0);
		}
		public TerminalNode LIMIT() { return getToken(ArcticSparkSqlParser.LIMIT, 0); }
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
		public TerminalNode ALL() { return getToken(ArcticSparkSqlParser.ALL, 0); }
		public QueryOrganizationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryOrganization; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQueryOrganization(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQueryOrganization(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQueryOrganization(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryOrganizationContext queryOrganization() throws RecognitionException {
		QueryOrganizationContext _localctx = new QueryOrganizationContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_queryOrganization);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(720);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(710);
				match(ORDER);
				setState(711);
				match(BY);
				setState(712);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(717);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(713);
					match(T__1);
					setState(714);
					((QueryOrganizationContext)_localctx).sortItem = sortItem();
					((QueryOrganizationContext)_localctx).order.add(((QueryOrganizationContext)_localctx).sortItem);
					}
					}
					setState(719);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(732);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CLUSTER) {
				{
				setState(722);
				match(CLUSTER);
				setState(723);
				match(BY);
				setState(724);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(729);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(725);
					match(T__1);
					setState(726);
					((QueryOrganizationContext)_localctx).expression = expression();
					((QueryOrganizationContext)_localctx).clusterBy.add(((QueryOrganizationContext)_localctx).expression);
					}
					}
					setState(731);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(744);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DISTRIBUTE) {
				{
				setState(734);
				match(DISTRIBUTE);
				setState(735);
				match(BY);
				setState(736);
				((QueryOrganizationContext)_localctx).expression = expression();
				((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
				setState(741);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(737);
					match(T__1);
					setState(738);
					((QueryOrganizationContext)_localctx).expression = expression();
					((QueryOrganizationContext)_localctx).distributeBy.add(((QueryOrganizationContext)_localctx).expression);
					}
					}
					setState(743);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(756);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SORT) {
				{
				setState(746);
				match(SORT);
				setState(747);
				match(BY);
				setState(748);
				((QueryOrganizationContext)_localctx).sortItem = sortItem();
				((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
				setState(753);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(749);
					match(T__1);
					setState(750);
					((QueryOrganizationContext)_localctx).sortItem = sortItem();
					((QueryOrganizationContext)_localctx).sort.add(((QueryOrganizationContext)_localctx).sortItem);
					}
					}
					setState(755);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(759);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WINDOW) {
				{
				setState(758);
				windows();
				}
			}

			setState(766);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(761);
				match(LIMIT);
				setState(764);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
				case 1:
					{
					setState(762);
					match(ALL);
					}
					break;
				case 2:
					{
					setState(763);
					((QueryOrganizationContext)_localctx).limit = expression();
					}
					break;
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

	public static class MultiInsertQueryBodyContext extends ParserRuleContext {
		public QuerySpecificationContext querySpecification() {
			return getRuleContext(QuerySpecificationContext.class,0);
		}
		public QueryOrganizationContext queryOrganization() {
			return getRuleContext(QueryOrganizationContext.class,0);
		}
		public InsertIntoContext insertInto() {
			return getRuleContext(InsertIntoContext.class,0);
		}
		public MultiInsertQueryBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiInsertQueryBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterMultiInsertQueryBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitMultiInsertQueryBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitMultiInsertQueryBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiInsertQueryBodyContext multiInsertQueryBody() throws RecognitionException {
		MultiInsertQueryBodyContext _localctx = new MultiInsertQueryBodyContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_multiInsertQueryBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(769);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INSERT) {
				{
				setState(768);
				insertInto();
				}
			}

			setState(771);
			querySpecification();
			setState(772);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQueryTermDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQueryTermDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQueryTermDefault(this);
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
		public TerminalNode INTERSECT() { return getToken(ArcticSparkSqlParser.INTERSECT, 0); }
		public TerminalNode UNION() { return getToken(ArcticSparkSqlParser.UNION, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticSparkSqlParser.EXCEPT, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSparkSqlParser.SETMINUS, 0); }
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public SetOperationContext(QueryTermContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSetOperation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSetOperation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSetOperation(this);
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
		int _startState = 78;
		enterRecursionRule(_localctx, 78, RULE_queryTerm, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new QueryTermDefaultContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(775);
			queryPrimary();
			}
			_ctx.stop = _input.LT(-1);
			setState(785);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,71,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new SetOperationContext(new QueryTermContext(_parentctx, _parentState));
					((SetOperationContext)_localctx).left = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_queryTerm);
					setState(777);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(778);
					((SetOperationContext)_localctx).operator = _input.LT(1);
					_la = _input.LA(1);
					if ( !(((((_la - 101)) & ~0x3f) == 0 && ((1L << (_la - 101)) & ((1L << (UNION - 101)) | (1L << (EXCEPT - 101)) | (1L << (SETMINUS - 101)) | (1L << (INTERSECT - 101)))) != 0)) ) {
						((SetOperationContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(780);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ALL || _la==DISTINCT) {
						{
						setState(779);
						setQuantifier();
						}
					}

					setState(782);
					((SetOperationContext)_localctx).right = queryTerm(2);
					}
					} 
				}
				setState(787);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,71,_ctx);
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
		public QueryNoWithContext queryNoWith() {
			return getRuleContext(QueryNoWithContext.class,0);
		}
		public SubqueryContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSubquery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSubquery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSubquery(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQueryPrimaryDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQueryPrimaryDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQueryPrimaryDefault(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInlineTableDefault1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInlineTableDefault1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInlineTableDefault1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TableContext extends QueryPrimaryContext {
		public TerminalNode TABLE() { return getToken(ArcticSparkSqlParser.TABLE, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TableContext(QueryPrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryPrimaryContext queryPrimary() throws RecognitionException {
		QueryPrimaryContext _localctx = new QueryPrimaryContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_queryPrimary);
		try {
			setState(796);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
			case FROM:
			case MAP:
			case REDUCE:
				_localctx = new QueryPrimaryDefaultContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(788);
				querySpecification();
				}
				break;
			case TABLE:
				_localctx = new TableContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(789);
				match(TABLE);
				setState(790);
				tableIdentifier();
				}
				break;
			case VALUES:
				_localctx = new InlineTableDefault1Context(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(791);
				inlineTable();
				}
				break;
			case T__0:
				_localctx = new SubqueryContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(792);
				match(T__0);
				setState(793);
				queryNoWith();
				setState(794);
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
		public TerminalNode NULLS() { return getToken(ArcticSparkSqlParser.NULLS, 0); }
		public TerminalNode ASC() { return getToken(ArcticSparkSqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSparkSqlParser.DESC, 0); }
		public TerminalNode LAST() { return getToken(ArcticSparkSqlParser.LAST, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSparkSqlParser.FIRST, 0); }
		public SortItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sortItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSortItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSortItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSortItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SortItemContext sortItem() throws RecognitionException {
		SortItemContext _localctx = new SortItemContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_sortItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(798);
			expression();
			setState(800);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(799);
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
			}

			setState(804);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NULLS) {
				{
				setState(802);
				match(NULLS);
				setState(803);
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

	public static class QuerySpecificationContext extends ParserRuleContext {
		public Token kind;
		public RowFormatContext inRowFormat;
		public Token recordWriter;
		public Token script;
		public RowFormatContext outRowFormat;
		public Token recordReader;
		public BooleanExpressionContext where;
		public HintContext hint;
		public List<HintContext> hints = new ArrayList<HintContext>();
		public BooleanExpressionContext having;
		public TerminalNode USING() { return getToken(ArcticSparkSqlParser.USING, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSparkSqlParser.STRING, i);
		}
		public TerminalNode RECORDWRITER() { return getToken(ArcticSparkSqlParser.RECORDWRITER, 0); }
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSparkSqlParser.RECORDREADER, 0); }
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ArcticSparkSqlParser.WHERE, 0); }
		public TerminalNode SELECT() { return getToken(ArcticSparkSqlParser.SELECT, 0); }
		public NamedExpressionSeqContext namedExpressionSeq() {
			return getRuleContext(NamedExpressionSeqContext.class,0);
		}
		public List<RowFormatContext> rowFormat() {
			return getRuleContexts(RowFormatContext.class);
		}
		public RowFormatContext rowFormat(int i) {
			return getRuleContext(RowFormatContext.class,i);
		}
		public List<BooleanExpressionContext> booleanExpression() {
			return getRuleContexts(BooleanExpressionContext.class);
		}
		public BooleanExpressionContext booleanExpression(int i) {
			return getRuleContext(BooleanExpressionContext.class,i);
		}
		public TerminalNode TRANSFORM() { return getToken(ArcticSparkSqlParser.TRANSFORM, 0); }
		public TerminalNode MAP() { return getToken(ArcticSparkSqlParser.MAP, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSparkSqlParser.REDUCE, 0); }
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public List<LateralViewContext> lateralView() {
			return getRuleContexts(LateralViewContext.class);
		}
		public LateralViewContext lateralView(int i) {
			return getRuleContext(LateralViewContext.class,i);
		}
		public AggregationContext aggregation() {
			return getRuleContext(AggregationContext.class,0);
		}
		public TerminalNode HAVING() { return getToken(ArcticSparkSqlParser.HAVING, 0); }
		public WindowsContext windows() {
			return getRuleContext(WindowsContext.class,0);
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
		public QuerySpecificationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_querySpecification; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQuerySpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQuerySpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQuerySpecification(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuerySpecificationContext querySpecification() throws RecognitionException {
		QuerySpecificationContext _localctx = new QuerySpecificationContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_querySpecification);
		int _la;
		try {
			int _alt;
			setState(899);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,96,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				{
				setState(816);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case SELECT:
					{
					setState(806);
					match(SELECT);
					setState(807);
					((QuerySpecificationContext)_localctx).kind = match(TRANSFORM);
					setState(808);
					match(T__0);
					setState(809);
					namedExpressionSeq();
					setState(810);
					match(T__2);
					}
					break;
				case MAP:
					{
					setState(812);
					((QuerySpecificationContext)_localctx).kind = match(MAP);
					setState(813);
					namedExpressionSeq();
					}
					break;
				case REDUCE:
					{
					setState(814);
					((QuerySpecificationContext)_localctx).kind = match(REDUCE);
					setState(815);
					namedExpressionSeq();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(819);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ROW) {
					{
					setState(818);
					((QuerySpecificationContext)_localctx).inRowFormat = rowFormat();
					}
				}

				setState(823);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==RECORDWRITER) {
					{
					setState(821);
					match(RECORDWRITER);
					setState(822);
					((QuerySpecificationContext)_localctx).recordWriter = match(STRING);
					}
				}

				setState(825);
				match(USING);
				setState(826);
				((QuerySpecificationContext)_localctx).script = match(STRING);
				setState(839);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
				case 1:
					{
					setState(827);
					match(AS);
					setState(837);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
					case 1:
						{
						setState(828);
						identifierSeq();
						}
						break;
					case 2:
						{
						setState(829);
						colTypeList();
						}
						break;
					case 3:
						{
						{
						setState(830);
						match(T__0);
						setState(833);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
						case 1:
							{
							setState(831);
							identifierSeq();
							}
							break;
						case 2:
							{
							setState(832);
							colTypeList();
							}
							break;
						}
						setState(835);
						match(T__2);
						}
						}
						break;
					}
					}
					break;
				}
				setState(842);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
				case 1:
					{
					setState(841);
					((QuerySpecificationContext)_localctx).outRowFormat = rowFormat();
					}
					break;
				}
				setState(846);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
				case 1:
					{
					setState(844);
					match(RECORDREADER);
					setState(845);
					((QuerySpecificationContext)_localctx).recordReader = match(STRING);
					}
					break;
				}
				setState(849);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,83,_ctx) ) {
				case 1:
					{
					setState(848);
					fromClause();
					}
					break;
				}
				setState(853);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
				case 1:
					{
					setState(851);
					match(WHERE);
					setState(852);
					((QuerySpecificationContext)_localctx).where = booleanExpression(0);
					}
					break;
				}
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(877);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case SELECT:
					{
					setState(855);
					((QuerySpecificationContext)_localctx).kind = match(SELECT);
					setState(859);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__4) {
						{
						{
						setState(856);
						((QuerySpecificationContext)_localctx).hint = hint();
						((QuerySpecificationContext)_localctx).hints.add(((QuerySpecificationContext)_localctx).hint);
						}
						}
						setState(861);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(863);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,86,_ctx) ) {
					case 1:
						{
						setState(862);
						setQuantifier();
						}
						break;
					}
					setState(865);
					namedExpressionSeq();
					setState(867);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
					case 1:
						{
						setState(866);
						fromClause();
						}
						break;
					}
					}
					break;
				case FROM:
					{
					setState(869);
					fromClause();
					setState(875);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,89,_ctx) ) {
					case 1:
						{
						setState(870);
						((QuerySpecificationContext)_localctx).kind = match(SELECT);
						setState(872);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
						case 1:
							{
							setState(871);
							setQuantifier();
							}
							break;
						}
						setState(874);
						namedExpressionSeq();
						}
						break;
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(882);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,91,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(879);
						lateralView();
						}
						} 
					}
					setState(884);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,91,_ctx);
				}
				setState(887);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
				case 1:
					{
					setState(885);
					match(WHERE);
					setState(886);
					((QuerySpecificationContext)_localctx).where = booleanExpression(0);
					}
					break;
				}
				setState(890);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,93,_ctx) ) {
				case 1:
					{
					setState(889);
					aggregation();
					}
					break;
				}
				setState(894);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
				case 1:
					{
					setState(892);
					match(HAVING);
					setState(893);
					((QuerySpecificationContext)_localctx).having = booleanExpression(0);
					}
					break;
				}
				setState(897);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,95,_ctx) ) {
				case 1:
					{
					setState(896);
					windows();
					}
					break;
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterHint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitHint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitHint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintContext hint() throws RecognitionException {
		HintContext _localctx = new HintContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_hint);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(901);
			match(T__4);
			setState(902);
			((HintContext)_localctx).hintStatement = hintStatement();
			((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
			setState(909);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (DIV - 141)) | (1L << (PERCENTLIT - 141)) | (1L << (BUCKET - 141)) | (1L << (OUT - 141)) | (1L << (OF - 141)) | (1L << (SORT - 141)) | (1L << (CLUSTER - 141)) | (1L << (DISTRIBUTE - 141)) | (1L << (OVERWRITE - 141)) | (1L << (TRANSFORM - 141)) | (1L << (REDUCE - 141)) | (1L << (SERDE - 141)) | (1L << (SERDEPROPERTIES - 141)) | (1L << (RECORDREADER - 141)) | (1L << (RECORDWRITER - 141)) | (1L << (DELIMITED - 141)) | (1L << (FIELDS - 141)) | (1L << (TERMINATED - 141)) | (1L << (COLLECTION - 141)) | (1L << (ITEMS - 141)) | (1L << (KEYS - 141)) | (1L << (ESCAPED - 141)) | (1L << (LINES - 141)) | (1L << (SEPARATED - 141)) | (1L << (FUNCTION - 141)) | (1L << (EXTENDED - 141)) | (1L << (REFRESH - 141)) | (1L << (CLEAR - 141)) | (1L << (CACHE - 141)) | (1L << (UNCACHE - 141)) | (1L << (LAZY - 141)) | (1L << (FORMATTED - 141)) | (1L << (GLOBAL - 141)) | (1L << (TEMPORARY - 141)) | (1L << (OPTIONS - 141)) | (1L << (UNSET - 141)) | (1L << (TBLPROPERTIES - 141)) | (1L << (DBPROPERTIES - 141)) | (1L << (BUCKETS - 141)) | (1L << (SKEWED - 141)) | (1L << (STORED - 141)) | (1L << (DIRECTORIES - 141)) | (1L << (LOCATION - 141)) | (1L << (EXCHANGE - 141)) | (1L << (ARCHIVE - 141)) | (1L << (UNARCHIVE - 141)) | (1L << (FILEFORMAT - 141)) | (1L << (TOUCH - 141)) | (1L << (COMPACT - 141)) | (1L << (CONCATENATE - 141)) | (1L << (CHANGE - 141)) | (1L << (CASCADE - 141)) | (1L << (RESTRICT - 141)) | (1L << (CLUSTERED - 141)) | (1L << (SORTED - 141)) | (1L << (PURGE - 141)) | (1L << (INPUTFORMAT - 141)) | (1L << (OUTPUTFORMAT - 141)))) != 0) || ((((_la - 205)) & ~0x3f) == 0 && ((1L << (_la - 205)) & ((1L << (DATABASE - 205)) | (1L << (DATABASES - 205)) | (1L << (DFS - 205)) | (1L << (TRUNCATE - 205)) | (1L << (ANALYZE - 205)) | (1L << (COMPUTE - 205)) | (1L << (LIST - 205)) | (1L << (STATISTICS - 205)) | (1L << (PARTITIONED - 205)) | (1L << (EXTERNAL - 205)) | (1L << (DEFINED - 205)) | (1L << (REVOKE - 205)) | (1L << (GRANT - 205)) | (1L << (LOCK - 205)) | (1L << (UNLOCK - 205)) | (1L << (MSCK - 205)) | (1L << (REPAIR - 205)) | (1L << (RECOVER - 205)) | (1L << (EXPORT - 205)) | (1L << (IMPORT - 205)) | (1L << (LOAD - 205)) | (1L << (ROLE - 205)) | (1L << (ROLES - 205)) | (1L << (COMPACTIONS - 205)) | (1L << (PRINCIPALS - 205)) | (1L << (TRANSACTIONS - 205)) | (1L << (INDEX - 205)) | (1L << (INDEXES - 205)) | (1L << (LOCKS - 205)) | (1L << (OPTION - 205)) | (1L << (ANTI - 205)) | (1L << (LOCAL - 205)) | (1L << (INPATH - 205)) | (1L << (IDENTIFIER - 205)) | (1L << (BACKQUOTED_IDENTIFIER - 205)))) != 0)) {
				{
				{
				setState(904);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(903);
					match(T__1);
					}
				}

				setState(906);
				((HintContext)_localctx).hintStatement = hintStatement();
				((HintContext)_localctx).hintStatements.add(((HintContext)_localctx).hintStatement);
				}
				}
				setState(911);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(912);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterHintStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitHintStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitHintStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HintStatementContext hintStatement() throws RecognitionException {
		HintStatementContext _localctx = new HintStatementContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_hintStatement);
		int _la;
		try {
			setState(927);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(914);
				((HintStatementContext)_localctx).hintName = identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(915);
				((HintStatementContext)_localctx).hintName = identifier();
				setState(916);
				match(T__0);
				setState(917);
				((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
				((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
				setState(922);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(918);
					match(T__1);
					setState(919);
					((HintStatementContext)_localctx).primaryExpression = primaryExpression(0);
					((HintStatementContext)_localctx).parameters.add(((HintStatementContext)_localctx).primaryExpression);
					}
					}
					setState(924);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(925);
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
		public TerminalNode FROM() { return getToken(ArcticSparkSqlParser.FROM, 0); }
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
		public FromClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterFromClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitFromClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitFromClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromClauseContext fromClause() throws RecognitionException {
		FromClauseContext _localctx = new FromClauseContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_fromClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(929);
			match(FROM);
			setState(930);
			relation();
			setState(935);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,101,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(931);
					match(T__1);
					setState(932);
					relation();
					}
					} 
				}
				setState(937);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,101,_ctx);
			}
			setState(941);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(938);
					lateralView();
					}
					} 
				}
				setState(943);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
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

	public static class AggregationContext extends ParserRuleContext {
		public ExpressionContext expression;
		public List<ExpressionContext> groupingExpressions = new ArrayList<ExpressionContext>();
		public Token kind;
		public TerminalNode GROUP() { return getToken(ArcticSparkSqlParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(ArcticSparkSqlParser.BY, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode WITH() { return getToken(ArcticSparkSqlParser.WITH, 0); }
		public TerminalNode SETS() { return getToken(ArcticSparkSqlParser.SETS, 0); }
		public List<GroupingSetContext> groupingSet() {
			return getRuleContexts(GroupingSetContext.class);
		}
		public GroupingSetContext groupingSet(int i) {
			return getRuleContext(GroupingSetContext.class,i);
		}
		public TerminalNode ROLLUP() { return getToken(ArcticSparkSqlParser.ROLLUP, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSparkSqlParser.CUBE, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSparkSqlParser.GROUPING, 0); }
		public AggregationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterAggregation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitAggregation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitAggregation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregationContext aggregation() throws RecognitionException {
		AggregationContext _localctx = new AggregationContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_aggregation);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(944);
			match(GROUP);
			setState(945);
			match(BY);
			setState(946);
			((AggregationContext)_localctx).expression = expression();
			((AggregationContext)_localctx).groupingExpressions.add(((AggregationContext)_localctx).expression);
			setState(951);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,103,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(947);
					match(T__1);
					setState(948);
					((AggregationContext)_localctx).expression = expression();
					((AggregationContext)_localctx).groupingExpressions.add(((AggregationContext)_localctx).expression);
					}
					} 
				}
				setState(953);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,103,_ctx);
			}
			setState(971);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
			case 1:
				{
				setState(954);
				match(WITH);
				setState(955);
				((AggregationContext)_localctx).kind = match(ROLLUP);
				}
				break;
			case 2:
				{
				setState(956);
				match(WITH);
				setState(957);
				((AggregationContext)_localctx).kind = match(CUBE);
				}
				break;
			case 3:
				{
				setState(958);
				((AggregationContext)_localctx).kind = match(GROUPING);
				setState(959);
				match(SETS);
				setState(960);
				match(T__0);
				setState(961);
				groupingSet();
				setState(966);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(962);
					match(T__1);
					setState(963);
					groupingSet();
					}
					}
					setState(968);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(969);
				match(T__2);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterGroupingSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitGroupingSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitGroupingSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupingSetContext groupingSet() throws RecognitionException {
		GroupingSetContext _localctx = new GroupingSetContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_groupingSet);
		int _la;
		try {
			setState(986);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,108,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(973);
				match(T__0);
				setState(982);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (PERCENTLIT - 136)) | (1L << (BUCKET - 136)) | (1L << (OUT - 136)) | (1L << (OF - 136)) | (1L << (SORT - 136)) | (1L << (CLUSTER - 136)) | (1L << (DISTRIBUTE - 136)) | (1L << (OVERWRITE - 136)) | (1L << (TRANSFORM - 136)) | (1L << (REDUCE - 136)) | (1L << (SERDE - 136)) | (1L << (SERDEPROPERTIES - 136)) | (1L << (RECORDREADER - 136)) | (1L << (RECORDWRITER - 136)) | (1L << (DELIMITED - 136)) | (1L << (FIELDS - 136)) | (1L << (TERMINATED - 136)) | (1L << (COLLECTION - 136)) | (1L << (ITEMS - 136)) | (1L << (KEYS - 136)) | (1L << (ESCAPED - 136)) | (1L << (LINES - 136)) | (1L << (SEPARATED - 136)) | (1L << (FUNCTION - 136)) | (1L << (EXTENDED - 136)) | (1L << (REFRESH - 136)) | (1L << (CLEAR - 136)) | (1L << (CACHE - 136)) | (1L << (UNCACHE - 136)) | (1L << (LAZY - 136)) | (1L << (FORMATTED - 136)) | (1L << (GLOBAL - 136)) | (1L << (TEMPORARY - 136)) | (1L << (OPTIONS - 136)) | (1L << (UNSET - 136)) | (1L << (TBLPROPERTIES - 136)) | (1L << (DBPROPERTIES - 136)) | (1L << (BUCKETS - 136)) | (1L << (SKEWED - 136)) | (1L << (STORED - 136)) | (1L << (DIRECTORIES - 136)) | (1L << (LOCATION - 136)) | (1L << (EXCHANGE - 136)) | (1L << (ARCHIVE - 136)) | (1L << (UNARCHIVE - 136)) | (1L << (FILEFORMAT - 136)) | (1L << (TOUCH - 136)) | (1L << (COMPACT - 136)) | (1L << (CONCATENATE - 136)) | (1L << (CHANGE - 136)) | (1L << (CASCADE - 136)) | (1L << (RESTRICT - 136)))) != 0) || ((((_la - 200)) & ~0x3f) == 0 && ((1L << (_la - 200)) & ((1L << (CLUSTERED - 200)) | (1L << (SORTED - 200)) | (1L << (PURGE - 200)) | (1L << (INPUTFORMAT - 200)) | (1L << (OUTPUTFORMAT - 200)) | (1L << (DATABASE - 200)) | (1L << (DATABASES - 200)) | (1L << (DFS - 200)) | (1L << (TRUNCATE - 200)) | (1L << (ANALYZE - 200)) | (1L << (COMPUTE - 200)) | (1L << (LIST - 200)) | (1L << (STATISTICS - 200)) | (1L << (PARTITIONED - 200)) | (1L << (EXTERNAL - 200)) | (1L << (DEFINED - 200)) | (1L << (REVOKE - 200)) | (1L << (GRANT - 200)) | (1L << (LOCK - 200)) | (1L << (UNLOCK - 200)) | (1L << (MSCK - 200)) | (1L << (REPAIR - 200)) | (1L << (RECOVER - 200)) | (1L << (EXPORT - 200)) | (1L << (IMPORT - 200)) | (1L << (LOAD - 200)) | (1L << (ROLE - 200)) | (1L << (ROLES - 200)) | (1L << (COMPACTIONS - 200)) | (1L << (PRINCIPALS - 200)) | (1L << (TRANSACTIONS - 200)) | (1L << (INDEX - 200)) | (1L << (INDEXES - 200)) | (1L << (LOCKS - 200)) | (1L << (OPTION - 200)) | (1L << (ANTI - 200)) | (1L << (LOCAL - 200)) | (1L << (INPATH - 200)) | (1L << (STRING - 200)) | (1L << (BIGINT_LITERAL - 200)) | (1L << (SMALLINT_LITERAL - 200)) | (1L << (TINYINT_LITERAL - 200)) | (1L << (INTEGER_VALUE - 200)) | (1L << (DECIMAL_VALUE - 200)) | (1L << (DOUBLE_LITERAL - 200)) | (1L << (BIGDECIMAL_LITERAL - 200)) | (1L << (IDENTIFIER - 200)) | (1L << (BACKQUOTED_IDENTIFIER - 200)))) != 0)) {
					{
					setState(974);
					expression();
					setState(979);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(975);
						match(T__1);
						setState(976);
						expression();
						}
						}
						setState(981);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(984);
				match(T__2);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(985);
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

	public static class LateralViewContext extends ParserRuleContext {
		public IdentifierContext tblName;
		public IdentifierContext identifier;
		public List<IdentifierContext> colName = new ArrayList<IdentifierContext>();
		public TerminalNode LATERAL() { return getToken(ArcticSparkSqlParser.LATERAL, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSparkSqlParser.VIEW, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode OUTER() { return getToken(ArcticSparkSqlParser.OUTER, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public LateralViewContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lateralView; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterLateralView(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitLateralView(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitLateralView(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LateralViewContext lateralView() throws RecognitionException {
		LateralViewContext _localctx = new LateralViewContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_lateralView);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(988);
			match(LATERAL);
			setState(989);
			match(VIEW);
			setState(991);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				{
				setState(990);
				match(OUTER);
				}
				break;
			}
			setState(993);
			qualifiedName();
			setState(994);
			match(T__0);
			setState(1003);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (PERCENTLIT - 136)) | (1L << (BUCKET - 136)) | (1L << (OUT - 136)) | (1L << (OF - 136)) | (1L << (SORT - 136)) | (1L << (CLUSTER - 136)) | (1L << (DISTRIBUTE - 136)) | (1L << (OVERWRITE - 136)) | (1L << (TRANSFORM - 136)) | (1L << (REDUCE - 136)) | (1L << (SERDE - 136)) | (1L << (SERDEPROPERTIES - 136)) | (1L << (RECORDREADER - 136)) | (1L << (RECORDWRITER - 136)) | (1L << (DELIMITED - 136)) | (1L << (FIELDS - 136)) | (1L << (TERMINATED - 136)) | (1L << (COLLECTION - 136)) | (1L << (ITEMS - 136)) | (1L << (KEYS - 136)) | (1L << (ESCAPED - 136)) | (1L << (LINES - 136)) | (1L << (SEPARATED - 136)) | (1L << (FUNCTION - 136)) | (1L << (EXTENDED - 136)) | (1L << (REFRESH - 136)) | (1L << (CLEAR - 136)) | (1L << (CACHE - 136)) | (1L << (UNCACHE - 136)) | (1L << (LAZY - 136)) | (1L << (FORMATTED - 136)) | (1L << (GLOBAL - 136)) | (1L << (TEMPORARY - 136)) | (1L << (OPTIONS - 136)) | (1L << (UNSET - 136)) | (1L << (TBLPROPERTIES - 136)) | (1L << (DBPROPERTIES - 136)) | (1L << (BUCKETS - 136)) | (1L << (SKEWED - 136)) | (1L << (STORED - 136)) | (1L << (DIRECTORIES - 136)) | (1L << (LOCATION - 136)) | (1L << (EXCHANGE - 136)) | (1L << (ARCHIVE - 136)) | (1L << (UNARCHIVE - 136)) | (1L << (FILEFORMAT - 136)) | (1L << (TOUCH - 136)) | (1L << (COMPACT - 136)) | (1L << (CONCATENATE - 136)) | (1L << (CHANGE - 136)) | (1L << (CASCADE - 136)) | (1L << (RESTRICT - 136)))) != 0) || ((((_la - 200)) & ~0x3f) == 0 && ((1L << (_la - 200)) & ((1L << (CLUSTERED - 200)) | (1L << (SORTED - 200)) | (1L << (PURGE - 200)) | (1L << (INPUTFORMAT - 200)) | (1L << (OUTPUTFORMAT - 200)) | (1L << (DATABASE - 200)) | (1L << (DATABASES - 200)) | (1L << (DFS - 200)) | (1L << (TRUNCATE - 200)) | (1L << (ANALYZE - 200)) | (1L << (COMPUTE - 200)) | (1L << (LIST - 200)) | (1L << (STATISTICS - 200)) | (1L << (PARTITIONED - 200)) | (1L << (EXTERNAL - 200)) | (1L << (DEFINED - 200)) | (1L << (REVOKE - 200)) | (1L << (GRANT - 200)) | (1L << (LOCK - 200)) | (1L << (UNLOCK - 200)) | (1L << (MSCK - 200)) | (1L << (REPAIR - 200)) | (1L << (RECOVER - 200)) | (1L << (EXPORT - 200)) | (1L << (IMPORT - 200)) | (1L << (LOAD - 200)) | (1L << (ROLE - 200)) | (1L << (ROLES - 200)) | (1L << (COMPACTIONS - 200)) | (1L << (PRINCIPALS - 200)) | (1L << (TRANSACTIONS - 200)) | (1L << (INDEX - 200)) | (1L << (INDEXES - 200)) | (1L << (LOCKS - 200)) | (1L << (OPTION - 200)) | (1L << (ANTI - 200)) | (1L << (LOCAL - 200)) | (1L << (INPATH - 200)) | (1L << (STRING - 200)) | (1L << (BIGINT_LITERAL - 200)) | (1L << (SMALLINT_LITERAL - 200)) | (1L << (TINYINT_LITERAL - 200)) | (1L << (INTEGER_VALUE - 200)) | (1L << (DECIMAL_VALUE - 200)) | (1L << (DOUBLE_LITERAL - 200)) | (1L << (BIGDECIMAL_LITERAL - 200)) | (1L << (IDENTIFIER - 200)) | (1L << (BACKQUOTED_IDENTIFIER - 200)))) != 0)) {
				{
				setState(995);
				expression();
				setState(1000);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(996);
					match(T__1);
					setState(997);
					expression();
					}
					}
					setState(1002);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1005);
			match(T__2);
			setState(1006);
			((LateralViewContext)_localctx).tblName = identifier();
			setState(1018);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,114,_ctx) ) {
			case 1:
				{
				setState(1008);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
				case 1:
					{
					setState(1007);
					match(AS);
					}
					break;
				}
				setState(1010);
				((LateralViewContext)_localctx).identifier = identifier();
				((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
				setState(1015);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,113,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1011);
						match(T__1);
						setState(1012);
						((LateralViewContext)_localctx).identifier = identifier();
						((LateralViewContext)_localctx).colName.add(((LateralViewContext)_localctx).identifier);
						}
						} 
					}
					setState(1017);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,113,_ctx);
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
		public TerminalNode DISTINCT() { return getToken(ArcticSparkSqlParser.DISTINCT, 0); }
		public TerminalNode ALL() { return getToken(ArcticSparkSqlParser.ALL, 0); }
		public SetQuantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setQuantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSetQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSetQuantifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSetQuantifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetQuantifierContext setQuantifier() throws RecognitionException {
		SetQuantifierContext _localctx = new SetQuantifierContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_setQuantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1020);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationContext relation() throws RecognitionException {
		RelationContext _localctx = new RelationContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_relation);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1022);
			relationPrimary();
			setState(1026);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,115,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1023);
					joinRelation();
					}
					} 
				}
				setState(1028);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,115,_ctx);
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
		public TerminalNode JOIN() { return getToken(ArcticSparkSqlParser.JOIN, 0); }
		public RelationPrimaryContext relationPrimary() {
			return getRuleContext(RelationPrimaryContext.class,0);
		}
		public JoinTypeContext joinType() {
			return getRuleContext(JoinTypeContext.class,0);
		}
		public JoinCriteriaContext joinCriteria() {
			return getRuleContext(JoinCriteriaContext.class,0);
		}
		public TerminalNode NATURAL() { return getToken(ArcticSparkSqlParser.NATURAL, 0); }
		public JoinRelationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinRelation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterJoinRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitJoinRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitJoinRelation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinRelationContext joinRelation() throws RecognitionException {
		JoinRelationContext _localctx = new JoinRelationContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_joinRelation);
		try {
			setState(1040);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case JOIN:
			case CROSS:
			case INNER:
			case LEFT:
			case RIGHT:
			case FULL:
			case ANTI:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(1029);
				joinType();
				}
				setState(1030);
				match(JOIN);
				setState(1031);
				((JoinRelationContext)_localctx).right = relationPrimary();
				setState(1033);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,116,_ctx) ) {
				case 1:
					{
					setState(1032);
					joinCriteria();
					}
					break;
				}
				}
				break;
			case NATURAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(1035);
				match(NATURAL);
				setState(1036);
				joinType();
				setState(1037);
				match(JOIN);
				setState(1038);
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
		public TerminalNode INNER() { return getToken(ArcticSparkSqlParser.INNER, 0); }
		public TerminalNode CROSS() { return getToken(ArcticSparkSqlParser.CROSS, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSparkSqlParser.LEFT, 0); }
		public TerminalNode OUTER() { return getToken(ArcticSparkSqlParser.OUTER, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSparkSqlParser.SEMI, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSparkSqlParser.RIGHT, 0); }
		public TerminalNode FULL() { return getToken(ArcticSparkSqlParser.FULL, 0); }
		public TerminalNode ANTI() { return getToken(ArcticSparkSqlParser.ANTI, 0); }
		public JoinTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterJoinType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitJoinType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitJoinType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinTypeContext joinType() throws RecognitionException {
		JoinTypeContext _localctx = new JoinTypeContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_joinType);
		int _la;
		try {
			setState(1064);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,123,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1043);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INNER) {
					{
					setState(1042);
					match(INNER);
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1045);
				match(CROSS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1046);
				match(LEFT);
				setState(1048);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(1047);
					match(OUTER);
					}
				}

				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1050);
				match(LEFT);
				setState(1051);
				match(SEMI);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1052);
				match(RIGHT);
				setState(1054);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(1053);
					match(OUTER);
					}
				}

				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1056);
				match(FULL);
				setState(1058);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OUTER) {
					{
					setState(1057);
					match(OUTER);
					}
				}

				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1061);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LEFT) {
					{
					setState(1060);
					match(LEFT);
					}
				}

				setState(1063);
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
		public TerminalNode ON() { return getToken(ArcticSparkSqlParser.ON, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public TerminalNode USING() { return getToken(ArcticSparkSqlParser.USING, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public JoinCriteriaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinCriteria; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterJoinCriteria(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitJoinCriteria(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitJoinCriteria(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinCriteriaContext joinCriteria() throws RecognitionException {
		JoinCriteriaContext _localctx = new JoinCriteriaContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_joinCriteria);
		int _la;
		try {
			setState(1080);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ON:
				enterOuterAlt(_localctx, 1);
				{
				setState(1066);
				match(ON);
				setState(1067);
				booleanExpression(0);
				}
				break;
			case USING:
				enterOuterAlt(_localctx, 2);
				{
				setState(1068);
				match(USING);
				setState(1069);
				match(T__0);
				setState(1070);
				identifier();
				setState(1075);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(1071);
					match(T__1);
					setState(1072);
					identifier();
					}
					}
					setState(1077);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1078);
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

	public static class SampleContext extends ParserRuleContext {
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSparkSqlParser.TABLESAMPLE, 0); }
		public SampleMethodContext sampleMethod() {
			return getRuleContext(SampleMethodContext.class,0);
		}
		public SampleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sample; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSample(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSample(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSample(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleContext sample() throws RecognitionException {
		SampleContext _localctx = new SampleContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_sample);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1082);
			match(TABLESAMPLE);
			setState(1083);
			match(T__0);
			setState(1085);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (PERCENTLIT - 136)) | (1L << (BUCKET - 136)) | (1L << (OUT - 136)) | (1L << (OF - 136)) | (1L << (SORT - 136)) | (1L << (CLUSTER - 136)) | (1L << (DISTRIBUTE - 136)) | (1L << (OVERWRITE - 136)) | (1L << (TRANSFORM - 136)) | (1L << (REDUCE - 136)) | (1L << (SERDE - 136)) | (1L << (SERDEPROPERTIES - 136)) | (1L << (RECORDREADER - 136)) | (1L << (RECORDWRITER - 136)) | (1L << (DELIMITED - 136)) | (1L << (FIELDS - 136)) | (1L << (TERMINATED - 136)) | (1L << (COLLECTION - 136)) | (1L << (ITEMS - 136)) | (1L << (KEYS - 136)) | (1L << (ESCAPED - 136)) | (1L << (LINES - 136)) | (1L << (SEPARATED - 136)) | (1L << (FUNCTION - 136)) | (1L << (EXTENDED - 136)) | (1L << (REFRESH - 136)) | (1L << (CLEAR - 136)) | (1L << (CACHE - 136)) | (1L << (UNCACHE - 136)) | (1L << (LAZY - 136)) | (1L << (FORMATTED - 136)) | (1L << (GLOBAL - 136)) | (1L << (TEMPORARY - 136)) | (1L << (OPTIONS - 136)) | (1L << (UNSET - 136)) | (1L << (TBLPROPERTIES - 136)) | (1L << (DBPROPERTIES - 136)) | (1L << (BUCKETS - 136)) | (1L << (SKEWED - 136)) | (1L << (STORED - 136)) | (1L << (DIRECTORIES - 136)) | (1L << (LOCATION - 136)) | (1L << (EXCHANGE - 136)) | (1L << (ARCHIVE - 136)) | (1L << (UNARCHIVE - 136)) | (1L << (FILEFORMAT - 136)) | (1L << (TOUCH - 136)) | (1L << (COMPACT - 136)) | (1L << (CONCATENATE - 136)) | (1L << (CHANGE - 136)) | (1L << (CASCADE - 136)) | (1L << (RESTRICT - 136)))) != 0) || ((((_la - 200)) & ~0x3f) == 0 && ((1L << (_la - 200)) & ((1L << (CLUSTERED - 200)) | (1L << (SORTED - 200)) | (1L << (PURGE - 200)) | (1L << (INPUTFORMAT - 200)) | (1L << (OUTPUTFORMAT - 200)) | (1L << (DATABASE - 200)) | (1L << (DATABASES - 200)) | (1L << (DFS - 200)) | (1L << (TRUNCATE - 200)) | (1L << (ANALYZE - 200)) | (1L << (COMPUTE - 200)) | (1L << (LIST - 200)) | (1L << (STATISTICS - 200)) | (1L << (PARTITIONED - 200)) | (1L << (EXTERNAL - 200)) | (1L << (DEFINED - 200)) | (1L << (REVOKE - 200)) | (1L << (GRANT - 200)) | (1L << (LOCK - 200)) | (1L << (UNLOCK - 200)) | (1L << (MSCK - 200)) | (1L << (REPAIR - 200)) | (1L << (RECOVER - 200)) | (1L << (EXPORT - 200)) | (1L << (IMPORT - 200)) | (1L << (LOAD - 200)) | (1L << (ROLE - 200)) | (1L << (ROLES - 200)) | (1L << (COMPACTIONS - 200)) | (1L << (PRINCIPALS - 200)) | (1L << (TRANSACTIONS - 200)) | (1L << (INDEX - 200)) | (1L << (INDEXES - 200)) | (1L << (LOCKS - 200)) | (1L << (OPTION - 200)) | (1L << (ANTI - 200)) | (1L << (LOCAL - 200)) | (1L << (INPATH - 200)) | (1L << (STRING - 200)) | (1L << (BIGINT_LITERAL - 200)) | (1L << (SMALLINT_LITERAL - 200)) | (1L << (TINYINT_LITERAL - 200)) | (1L << (INTEGER_VALUE - 200)) | (1L << (DECIMAL_VALUE - 200)) | (1L << (DOUBLE_LITERAL - 200)) | (1L << (BIGDECIMAL_LITERAL - 200)) | (1L << (IDENTIFIER - 200)) | (1L << (BACKQUOTED_IDENTIFIER - 200)))) != 0)) {
				{
				setState(1084);
				sampleMethod();
				}
			}

			setState(1087);
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
		public TerminalNode ROWS() { return getToken(ArcticSparkSqlParser.ROWS, 0); }
		public SampleByRowsContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSampleByRows(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSampleByRows(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSampleByRows(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByPercentileContext extends SampleMethodContext {
		public Token negativeSign;
		public Token percentage;
		public TerminalNode PERCENTLIT() { return getToken(ArcticSparkSqlParser.PERCENTLIT, 0); }
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public SampleByPercentileContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSampleByPercentile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSampleByPercentile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSampleByPercentile(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SampleByBucketContext extends SampleMethodContext {
		public Token sampleType;
		public Token numerator;
		public Token denominator;
		public TerminalNode OUT() { return getToken(ArcticSparkSqlParser.OUT, 0); }
		public TerminalNode OF() { return getToken(ArcticSparkSqlParser.OF, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSparkSqlParser.BUCKET, 0); }
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticSparkSqlParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticSparkSqlParser.INTEGER_VALUE, i);
		}
		public TerminalNode ON() { return getToken(ArcticSparkSqlParser.ON, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public SampleByBucketContext(SampleMethodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSampleByBucket(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSampleByBucket(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSampleByBucket(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSampleByBytes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSampleByBytes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSampleByBytes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SampleMethodContext sampleMethod() throws RecognitionException {
		SampleMethodContext _localctx = new SampleMethodContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_sampleMethod);
		int _la;
		try {
			setState(1113);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,130,_ctx) ) {
			case 1:
				_localctx = new SampleByPercentileContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1090);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1089);
					((SampleByPercentileContext)_localctx).negativeSign = match(MINUS);
					}
				}

				setState(1092);
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
				setState(1093);
				match(PERCENTLIT);
				}
				break;
			case 2:
				_localctx = new SampleByRowsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1094);
				expression();
				setState(1095);
				match(ROWS);
				}
				break;
			case 3:
				_localctx = new SampleByBucketContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1097);
				((SampleByBucketContext)_localctx).sampleType = match(BUCKET);
				setState(1098);
				((SampleByBucketContext)_localctx).numerator = match(INTEGER_VALUE);
				setState(1099);
				match(OUT);
				setState(1100);
				match(OF);
				setState(1101);
				((SampleByBucketContext)_localctx).denominator = match(INTEGER_VALUE);
				setState(1110);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ON) {
					{
					setState(1102);
					match(ON);
					setState(1108);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,128,_ctx) ) {
					case 1:
						{
						setState(1103);
						identifier();
						}
						break;
					case 2:
						{
						setState(1104);
						qualifiedName();
						setState(1105);
						match(T__0);
						setState(1106);
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
				setState(1112);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierListContext identifierList() throws RecognitionException {
		IdentifierListContext _localctx = new IdentifierListContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_identifierList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1115);
			match(T__0);
			setState(1116);
			identifierSeq();
			setState(1117);
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
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public IdentifierSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIdentifierSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIdentifierSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIdentifierSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierSeqContext identifierSeq() throws RecognitionException {
		IdentifierSeqContext _localctx = new IdentifierSeqContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_identifierSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1119);
			identifier();
			setState(1124);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,131,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1120);
					match(T__1);
					setState(1121);
					identifier();
					}
					} 
				}
				setState(1126);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,131,_ctx);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterOrderedIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitOrderedIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitOrderedIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierListContext orderedIdentifierList() throws RecognitionException {
		OrderedIdentifierListContext _localctx = new OrderedIdentifierListContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_orderedIdentifierList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1127);
			match(T__0);
			setState(1128);
			orderedIdentifier();
			setState(1133);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1129);
				match(T__1);
				setState(1130);
				orderedIdentifier();
				}
				}
				setState(1135);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1136);
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
		public Token ordering;
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASC() { return getToken(ArcticSparkSqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSparkSqlParser.DESC, 0); }
		public OrderedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterOrderedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitOrderedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitOrderedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderedIdentifierContext orderedIdentifier() throws RecognitionException {
		OrderedIdentifierContext _localctx = new OrderedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_orderedIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1138);
			identifier();
			setState(1140);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(1139);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIdentifierCommentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIdentifierCommentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIdentifierCommentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierCommentListContext identifierCommentList() throws RecognitionException {
		IdentifierCommentListContext _localctx = new IdentifierCommentListContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_identifierCommentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1142);
			match(T__0);
			setState(1143);
			identifierComment();
			setState(1148);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1144);
				match(T__1);
				setState(1145);
				identifierComment();
				}
				}
				setState(1150);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1151);
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
		public TerminalNode COMMENT() { return getToken(ArcticSparkSqlParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public IdentifierCommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierComment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIdentifierComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIdentifierComment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIdentifierComment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierCommentContext identifierComment() throws RecognitionException {
		IdentifierCommentContext _localctx = new IdentifierCommentContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_identifierComment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1153);
			identifier();
			setState(1156);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(1154);
				match(COMMENT);
				setState(1155);
				match(STRING);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableValuedFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableValuedFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableValuedFunction(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInlineTableDefault2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInlineTableDefault2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInlineTableDefault2(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterAliasedRelation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitAliasedRelation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitAliasedRelation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AliasedQueryContext extends RelationPrimaryContext {
		public QueryNoWithContext queryNoWith() {
			return getRuleContext(QueryNoWithContext.class,0);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterAliasedQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitAliasedQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitAliasedQuery(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TableNameContext extends RelationPrimaryContext {
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationPrimaryContext relationPrimary() throws RecognitionException {
		RelationPrimaryContext _localctx = new RelationPrimaryContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_relationPrimary);
		try {
			setState(1182);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,139,_ctx) ) {
			case 1:
				_localctx = new TableNameContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1158);
				tableIdentifier();
				setState(1160);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,136,_ctx) ) {
				case 1:
					{
					setState(1159);
					sample();
					}
					break;
				}
				setState(1162);
				tableAlias();
				}
				break;
			case 2:
				_localctx = new AliasedQueryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1164);
				match(T__0);
				setState(1165);
				queryNoWith();
				setState(1166);
				match(T__2);
				setState(1168);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,137,_ctx) ) {
				case 1:
					{
					setState(1167);
					sample();
					}
					break;
				}
				setState(1170);
				tableAlias();
				}
				break;
			case 3:
				_localctx = new AliasedRelationContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1172);
				match(T__0);
				setState(1173);
				relation();
				setState(1174);
				match(T__2);
				setState(1176);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,138,_ctx) ) {
				case 1:
					{
					setState(1175);
					sample();
					}
					break;
				}
				setState(1178);
				tableAlias();
				}
				break;
			case 4:
				_localctx = new InlineTableDefault2Context(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1180);
				inlineTable();
				}
				break;
			case 5:
				_localctx = new TableValuedFunctionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1181);
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
		public TerminalNode VALUES() { return getToken(ArcticSparkSqlParser.VALUES, 0); }
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInlineTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInlineTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInlineTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableContext inlineTable() throws RecognitionException {
		InlineTableContext _localctx = new InlineTableContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_inlineTable);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1184);
			match(VALUES);
			setState(1185);
			expression();
			setState(1190);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,140,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1186);
					match(T__1);
					setState(1187);
					expression();
					}
					} 
				}
				setState(1192);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,140,_ctx);
			}
			setState(1193);
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
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TableAliasContext tableAlias() {
			return getRuleContext(TableAliasContext.class,0);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterFunctionTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitFunctionTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitFunctionTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionTableContext functionTable() throws RecognitionException {
		FunctionTableContext _localctx = new FunctionTableContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_functionTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1195);
			identifier();
			setState(1196);
			match(T__0);
			setState(1205);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (PERCENTLIT - 136)) | (1L << (BUCKET - 136)) | (1L << (OUT - 136)) | (1L << (OF - 136)) | (1L << (SORT - 136)) | (1L << (CLUSTER - 136)) | (1L << (DISTRIBUTE - 136)) | (1L << (OVERWRITE - 136)) | (1L << (TRANSFORM - 136)) | (1L << (REDUCE - 136)) | (1L << (SERDE - 136)) | (1L << (SERDEPROPERTIES - 136)) | (1L << (RECORDREADER - 136)) | (1L << (RECORDWRITER - 136)) | (1L << (DELIMITED - 136)) | (1L << (FIELDS - 136)) | (1L << (TERMINATED - 136)) | (1L << (COLLECTION - 136)) | (1L << (ITEMS - 136)) | (1L << (KEYS - 136)) | (1L << (ESCAPED - 136)) | (1L << (LINES - 136)) | (1L << (SEPARATED - 136)) | (1L << (FUNCTION - 136)) | (1L << (EXTENDED - 136)) | (1L << (REFRESH - 136)) | (1L << (CLEAR - 136)) | (1L << (CACHE - 136)) | (1L << (UNCACHE - 136)) | (1L << (LAZY - 136)) | (1L << (FORMATTED - 136)) | (1L << (GLOBAL - 136)) | (1L << (TEMPORARY - 136)) | (1L << (OPTIONS - 136)) | (1L << (UNSET - 136)) | (1L << (TBLPROPERTIES - 136)) | (1L << (DBPROPERTIES - 136)) | (1L << (BUCKETS - 136)) | (1L << (SKEWED - 136)) | (1L << (STORED - 136)) | (1L << (DIRECTORIES - 136)) | (1L << (LOCATION - 136)) | (1L << (EXCHANGE - 136)) | (1L << (ARCHIVE - 136)) | (1L << (UNARCHIVE - 136)) | (1L << (FILEFORMAT - 136)) | (1L << (TOUCH - 136)) | (1L << (COMPACT - 136)) | (1L << (CONCATENATE - 136)) | (1L << (CHANGE - 136)) | (1L << (CASCADE - 136)) | (1L << (RESTRICT - 136)))) != 0) || ((((_la - 200)) & ~0x3f) == 0 && ((1L << (_la - 200)) & ((1L << (CLUSTERED - 200)) | (1L << (SORTED - 200)) | (1L << (PURGE - 200)) | (1L << (INPUTFORMAT - 200)) | (1L << (OUTPUTFORMAT - 200)) | (1L << (DATABASE - 200)) | (1L << (DATABASES - 200)) | (1L << (DFS - 200)) | (1L << (TRUNCATE - 200)) | (1L << (ANALYZE - 200)) | (1L << (COMPUTE - 200)) | (1L << (LIST - 200)) | (1L << (STATISTICS - 200)) | (1L << (PARTITIONED - 200)) | (1L << (EXTERNAL - 200)) | (1L << (DEFINED - 200)) | (1L << (REVOKE - 200)) | (1L << (GRANT - 200)) | (1L << (LOCK - 200)) | (1L << (UNLOCK - 200)) | (1L << (MSCK - 200)) | (1L << (REPAIR - 200)) | (1L << (RECOVER - 200)) | (1L << (EXPORT - 200)) | (1L << (IMPORT - 200)) | (1L << (LOAD - 200)) | (1L << (ROLE - 200)) | (1L << (ROLES - 200)) | (1L << (COMPACTIONS - 200)) | (1L << (PRINCIPALS - 200)) | (1L << (TRANSACTIONS - 200)) | (1L << (INDEX - 200)) | (1L << (INDEXES - 200)) | (1L << (LOCKS - 200)) | (1L << (OPTION - 200)) | (1L << (ANTI - 200)) | (1L << (LOCAL - 200)) | (1L << (INPATH - 200)) | (1L << (STRING - 200)) | (1L << (BIGINT_LITERAL - 200)) | (1L << (SMALLINT_LITERAL - 200)) | (1L << (TINYINT_LITERAL - 200)) | (1L << (INTEGER_VALUE - 200)) | (1L << (DECIMAL_VALUE - 200)) | (1L << (DOUBLE_LITERAL - 200)) | (1L << (BIGDECIMAL_LITERAL - 200)) | (1L << (IDENTIFIER - 200)) | (1L << (BACKQUOTED_IDENTIFIER - 200)))) != 0)) {
				{
				setState(1197);
				expression();
				setState(1202);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(1198);
					match(T__1);
					setState(1199);
					expression();
					}
					}
					setState(1204);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1207);
			match(T__2);
			setState(1208);
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
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TableAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableAlias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableAlias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableAliasContext tableAlias() throws RecognitionException {
		TableAliasContext _localctx = new TableAliasContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_tableAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1217);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,145,_ctx) ) {
			case 1:
				{
				setState(1211);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
				case 1:
					{
					setState(1210);
					match(AS);
					}
					break;
				}
				setState(1213);
				strictIdentifier();
				setState(1215);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
				case 1:
					{
					setState(1214);
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
		public TerminalNode ROW() { return getToken(ArcticSparkSqlParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSparkSqlParser.FORMAT, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSparkSqlParser.SERDE, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TerminalNode WITH() { return getToken(ArcticSparkSqlParser.WITH, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSparkSqlParser.SERDEPROPERTIES, 0); }
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public RowFormatSerdeContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterRowFormatSerde(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitRowFormatSerde(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitRowFormatSerde(this);
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
		public TerminalNode ROW() { return getToken(ArcticSparkSqlParser.ROW, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSparkSqlParser.FORMAT, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSparkSqlParser.DELIMITED, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSparkSqlParser.FIELDS, 0); }
		public List<TerminalNode> TERMINATED() { return getTokens(ArcticSparkSqlParser.TERMINATED); }
		public TerminalNode TERMINATED(int i) {
			return getToken(ArcticSparkSqlParser.TERMINATED, i);
		}
		public List<TerminalNode> BY() { return getTokens(ArcticSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSparkSqlParser.BY, i);
		}
		public TerminalNode COLLECTION() { return getToken(ArcticSparkSqlParser.COLLECTION, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSparkSqlParser.ITEMS, 0); }
		public TerminalNode MAP() { return getToken(ArcticSparkSqlParser.MAP, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSparkSqlParser.KEYS, 0); }
		public TerminalNode LINES() { return getToken(ArcticSparkSqlParser.LINES, 0); }
		public TerminalNode NULL() { return getToken(ArcticSparkSqlParser.NULL, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSparkSqlParser.DEFINED, 0); }
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public List<TerminalNode> STRING() { return getTokens(ArcticSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSparkSqlParser.STRING, i);
		}
		public TerminalNode ESCAPED() { return getToken(ArcticSparkSqlParser.ESCAPED, 0); }
		public RowFormatDelimitedContext(RowFormatContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterRowFormatDelimited(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitRowFormatDelimited(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitRowFormatDelimited(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RowFormatContext rowFormat() throws RecognitionException {
		RowFormatContext _localctx = new RowFormatContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_rowFormat);
		try {
			setState(1268);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,153,_ctx) ) {
			case 1:
				_localctx = new RowFormatSerdeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1219);
				match(ROW);
				setState(1220);
				match(FORMAT);
				setState(1221);
				match(SERDE);
				setState(1222);
				((RowFormatSerdeContext)_localctx).name = match(STRING);
				setState(1226);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,146,_ctx) ) {
				case 1:
					{
					setState(1223);
					match(WITH);
					setState(1224);
					match(SERDEPROPERTIES);
					setState(1225);
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
				setState(1228);
				match(ROW);
				setState(1229);
				match(FORMAT);
				setState(1230);
				match(DELIMITED);
				setState(1240);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,148,_ctx) ) {
				case 1:
					{
					setState(1231);
					match(FIELDS);
					setState(1232);
					match(TERMINATED);
					setState(1233);
					match(BY);
					setState(1234);
					((RowFormatDelimitedContext)_localctx).fieldsTerminatedBy = match(STRING);
					setState(1238);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
					case 1:
						{
						setState(1235);
						match(ESCAPED);
						setState(1236);
						match(BY);
						setState(1237);
						((RowFormatDelimitedContext)_localctx).escapedBy = match(STRING);
						}
						break;
					}
					}
					break;
				}
				setState(1247);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,149,_ctx) ) {
				case 1:
					{
					setState(1242);
					match(COLLECTION);
					setState(1243);
					match(ITEMS);
					setState(1244);
					match(TERMINATED);
					setState(1245);
					match(BY);
					setState(1246);
					((RowFormatDelimitedContext)_localctx).collectionItemsTerminatedBy = match(STRING);
					}
					break;
				}
				setState(1254);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,150,_ctx) ) {
				case 1:
					{
					setState(1249);
					match(MAP);
					setState(1250);
					match(KEYS);
					setState(1251);
					match(TERMINATED);
					setState(1252);
					match(BY);
					setState(1253);
					((RowFormatDelimitedContext)_localctx).keysTerminatedBy = match(STRING);
					}
					break;
				}
				setState(1260);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,151,_ctx) ) {
				case 1:
					{
					setState(1256);
					match(LINES);
					setState(1257);
					match(TERMINATED);
					setState(1258);
					match(BY);
					setState(1259);
					((RowFormatDelimitedContext)_localctx).linesSeparatedBy = match(STRING);
					}
					break;
				}
				setState(1266);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,152,_ctx) ) {
				case 1:
					{
					setState(1262);
					match(NULL);
					setState(1263);
					match(DEFINED);
					setState(1264);
					match(AS);
					setState(1265);
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

	public static class TableIdentifierContext extends ParserRuleContext {
		public IdentifierContext db;
		public IdentifierContext table;
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TableIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableIdentifierContext tableIdentifier() throws RecognitionException {
		TableIdentifierContext _localctx = new TableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_tableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1273);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,154,_ctx) ) {
			case 1:
				{
				setState(1270);
				((TableIdentifierContext)_localctx).db = identifier();
				setState(1271);
				match(T__3);
				}
				break;
			}
			setState(1275);
			((TableIdentifierContext)_localctx).table = identifier();
			}
		}
		catch (RecognitionException re) {
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
		public IdentifierContext db;
		public IdentifierContext function;
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public FunctionIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterFunctionIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitFunctionIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitFunctionIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionIdentifierContext functionIdentifier() throws RecognitionException {
		FunctionIdentifierContext _localctx = new FunctionIdentifierContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_functionIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1280);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,155,_ctx) ) {
			case 1:
				{
				setState(1277);
				((FunctionIdentifierContext)_localctx).db = identifier();
				setState(1278);
				match(T__3);
				}
				break;
			}
			setState(1282);
			((FunctionIdentifierContext)_localctx).function = identifier();
			}
		}
		catch (RecognitionException re) {
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
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public NamedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNamedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNamedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNamedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionContext namedExpression() throws RecognitionException {
		NamedExpressionContext _localctx = new NamedExpressionContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_namedExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1284);
			expression();
			setState(1292);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,158,_ctx) ) {
			case 1:
				{
				setState(1286);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,156,_ctx) ) {
				case 1:
					{
					setState(1285);
					match(AS);
					}
					break;
				}
				setState(1290);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case SELECT:
				case FROM:
				case ADD:
				case AS:
				case ALL:
				case DISTINCT:
				case WHERE:
				case GROUP:
				case BY:
				case GROUPING:
				case SETS:
				case CUBE:
				case ROLLUP:
				case ORDER:
				case HAVING:
				case LIMIT:
				case AT:
				case OR:
				case AND:
				case IN:
				case NOT:
				case NO:
				case EXISTS:
				case BETWEEN:
				case LIKE:
				case RLIKE:
				case IS:
				case NULL:
				case TRUE:
				case FALSE:
				case NULLS:
				case ASC:
				case DESC:
				case FOR:
				case INTERVAL:
				case CASE:
				case WHEN:
				case THEN:
				case ELSE:
				case END:
				case JOIN:
				case CROSS:
				case OUTER:
				case INNER:
				case LEFT:
				case SEMI:
				case RIGHT:
				case FULL:
				case NATURAL:
				case ON:
				case LATERAL:
				case WINDOW:
				case OVER:
				case PARTITION:
				case RANGE:
				case ROWS:
				case UNBOUNDED:
				case PRECEDING:
				case FOLLOWING:
				case CURRENT:
				case FIRST:
				case AFTER:
				case LAST:
				case ROW:
				case WITH:
				case VALUES:
				case CREATE:
				case TABLE:
				case DIRECTORY:
				case VIEW:
				case REPLACE:
				case INSERT:
				case DELETE:
				case INTO:
				case DESCRIBE:
				case EXPLAIN:
				case FORMAT:
				case LOGICAL:
				case CODEGEN:
				case COST:
				case CAST:
				case SHOW:
				case TABLES:
				case COLUMNS:
				case COLUMN:
				case USE:
				case PARTITIONS:
				case FUNCTIONS:
				case DROP:
				case UNION:
				case EXCEPT:
				case SETMINUS:
				case INTERSECT:
				case TO:
				case TABLESAMPLE:
				case STRATIFY:
				case ALTER:
				case RENAME:
				case ARRAY:
				case MAP:
				case STRUCT:
				case COMMENT:
				case SET:
				case RESET:
				case DATA:
				case START:
				case TRANSACTION:
				case COMMIT:
				case ROLLBACK:
				case MACRO:
				case IGNORE:
				case BOTH:
				case LEADING:
				case TRAILING:
				case IF:
				case POSITION:
				case DIV:
				case PERCENTLIT:
				case BUCKET:
				case OUT:
				case OF:
				case SORT:
				case CLUSTER:
				case DISTRIBUTE:
				case OVERWRITE:
				case TRANSFORM:
				case REDUCE:
				case SERDE:
				case SERDEPROPERTIES:
				case RECORDREADER:
				case RECORDWRITER:
				case DELIMITED:
				case FIELDS:
				case TERMINATED:
				case COLLECTION:
				case ITEMS:
				case KEYS:
				case ESCAPED:
				case LINES:
				case SEPARATED:
				case FUNCTION:
				case EXTENDED:
				case REFRESH:
				case CLEAR:
				case CACHE:
				case UNCACHE:
				case LAZY:
				case FORMATTED:
				case GLOBAL:
				case TEMPORARY:
				case OPTIONS:
				case UNSET:
				case TBLPROPERTIES:
				case DBPROPERTIES:
				case BUCKETS:
				case SKEWED:
				case STORED:
				case DIRECTORIES:
				case LOCATION:
				case EXCHANGE:
				case ARCHIVE:
				case UNARCHIVE:
				case FILEFORMAT:
				case TOUCH:
				case COMPACT:
				case CONCATENATE:
				case CHANGE:
				case CASCADE:
				case RESTRICT:
				case CLUSTERED:
				case SORTED:
				case PURGE:
				case INPUTFORMAT:
				case OUTPUTFORMAT:
				case DATABASE:
				case DATABASES:
				case DFS:
				case TRUNCATE:
				case ANALYZE:
				case COMPUTE:
				case LIST:
				case STATISTICS:
				case PARTITIONED:
				case EXTERNAL:
				case DEFINED:
				case REVOKE:
				case GRANT:
				case LOCK:
				case UNLOCK:
				case MSCK:
				case REPAIR:
				case RECOVER:
				case EXPORT:
				case IMPORT:
				case LOAD:
				case ROLE:
				case ROLES:
				case COMPACTIONS:
				case PRINCIPALS:
				case TRANSACTIONS:
				case INDEX:
				case INDEXES:
				case LOCKS:
				case OPTION:
				case ANTI:
				case LOCAL:
				case INPATH:
				case IDENTIFIER:
				case BACKQUOTED_IDENTIFIER:
					{
					setState(1288);
					identifier();
					}
					break;
				case T__0:
					{
					setState(1289);
					identifierList();
					}
					break;
				default:
					throw new NoViableAltException(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNamedExpressionSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNamedExpressionSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNamedExpressionSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedExpressionSeqContext namedExpressionSeq() throws RecognitionException {
		NamedExpressionSeqContext _localctx = new NamedExpressionSeqContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_namedExpressionSeq);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1294);
			namedExpression();
			setState(1299);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,159,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1295);
					match(T__1);
					setState(1296);
					namedExpression();
					}
					} 
				}
				setState(1301);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,159,_ctx);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1302);
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
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public BooleanExpressionContext booleanExpression() {
			return getRuleContext(BooleanExpressionContext.class,0);
		}
		public LogicalNotContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterLogicalNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitLogicalNot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitLogicalNot(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BooleanDefaultContext extends BooleanExpressionContext {
		public PredicatedContext predicated() {
			return getRuleContext(PredicatedContext.class,0);
		}
		public BooleanDefaultContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterBooleanDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitBooleanDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitBooleanDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExistsContext extends BooleanExpressionContext {
		public TerminalNode EXISTS() { return getToken(ArcticSparkSqlParser.EXISTS, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public ExistsContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterExists(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitExists(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitExists(this);
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
		public TerminalNode AND() { return getToken(ArcticSparkSqlParser.AND, 0); }
		public TerminalNode OR() { return getToken(ArcticSparkSqlParser.OR, 0); }
		public LogicalBinaryContext(BooleanExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterLogicalBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitLogicalBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitLogicalBinary(this);
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
			setState(1313);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,160,_ctx) ) {
			case 1:
				{
				_localctx = new LogicalNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1305);
				match(NOT);
				setState(1306);
				booleanExpression(5);
				}
				break;
			case 2:
				{
				_localctx = new ExistsContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1307);
				match(EXISTS);
				setState(1308);
				match(T__0);
				setState(1309);
				query();
				setState(1310);
				match(T__2);
				}
				break;
			case 3:
				{
				_localctx = new BooleanDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1312);
				predicated();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1323);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,162,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1321);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,161,_ctx) ) {
					case 1:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(1315);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1316);
						((LogicalBinaryContext)_localctx).operator = match(AND);
						setState(1317);
						((LogicalBinaryContext)_localctx).right = booleanExpression(3);
						}
						break;
					case 2:
						{
						_localctx = new LogicalBinaryContext(new BooleanExpressionContext(_parentctx, _parentState));
						((LogicalBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_booleanExpression);
						setState(1318);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1319);
						((LogicalBinaryContext)_localctx).operator = match(OR);
						setState(1320);
						((LogicalBinaryContext)_localctx).right = booleanExpression(2);
						}
						break;
					}
					} 
				}
				setState(1325);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,162,_ctx);
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

	public static class PredicatedContext extends ParserRuleContext {
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public PredicateContext predicate() {
			return getRuleContext(PredicateContext.class,0);
		}
		public PredicatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPredicated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPredicated(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPredicated(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicatedContext predicated() throws RecognitionException {
		PredicatedContext _localctx = new PredicatedContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_predicated);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1326);
			valueExpression(0);
			setState(1328);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				{
				setState(1327);
				predicate();
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

	public static class PredicateContext extends ParserRuleContext {
		public Token kind;
		public ValueExpressionContext lower;
		public ValueExpressionContext upper;
		public ValueExpressionContext pattern;
		public ValueExpressionContext right;
		public TerminalNode AND() { return getToken(ArcticSparkSqlParser.AND, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSparkSqlParser.BETWEEN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode IN() { return getToken(ArcticSparkSqlParser.IN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RLIKE() { return getToken(ArcticSparkSqlParser.RLIKE, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSparkSqlParser.LIKE, 0); }
		public TerminalNode IS() { return getToken(ArcticSparkSqlParser.IS, 0); }
		public TerminalNode NULL() { return getToken(ArcticSparkSqlParser.NULL, 0); }
		public TerminalNode FROM() { return getToken(ArcticSparkSqlParser.FROM, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticSparkSqlParser.DISTINCT, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_predicate);
		int _la;
		try {
			setState(1378);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,171,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1331);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1330);
					match(NOT);
					}
				}

				setState(1333);
				((PredicateContext)_localctx).kind = match(BETWEEN);
				setState(1334);
				((PredicateContext)_localctx).lower = valueExpression(0);
				setState(1335);
				match(AND);
				setState(1336);
				((PredicateContext)_localctx).upper = valueExpression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1339);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1338);
					match(NOT);
					}
				}

				setState(1341);
				((PredicateContext)_localctx).kind = match(IN);
				setState(1342);
				match(T__0);
				setState(1343);
				expression();
				setState(1348);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(1344);
					match(T__1);
					setState(1345);
					expression();
					}
					}
					setState(1350);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1351);
				match(T__2);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1354);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1353);
					match(NOT);
					}
				}

				setState(1356);
				((PredicateContext)_localctx).kind = match(IN);
				setState(1357);
				match(T__0);
				setState(1358);
				query();
				setState(1359);
				match(T__2);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1362);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1361);
					match(NOT);
					}
				}

				setState(1364);
				((PredicateContext)_localctx).kind = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==LIKE || _la==RLIKE) ) {
					((PredicateContext)_localctx).kind = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1365);
				((PredicateContext)_localctx).pattern = valueExpression(0);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1366);
				match(IS);
				setState(1368);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1367);
					match(NOT);
					}
				}

				setState(1370);
				((PredicateContext)_localctx).kind = match(NULL);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1371);
				match(IS);
				setState(1373);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1372);
					match(NOT);
					}
				}

				setState(1375);
				((PredicateContext)_localctx).kind = match(DISTINCT);
				setState(1376);
				match(FROM);
				setState(1377);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterValueExpressionDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitValueExpressionDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitValueExpressionDefault(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitComparison(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitComparison(this);
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
		public TerminalNode ASTERISK() { return getToken(ArcticSparkSqlParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticSparkSqlParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticSparkSqlParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticSparkSqlParser.DIV, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSparkSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticSparkSqlParser.CONCAT_PIPE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticSparkSqlParser.AMPERSAND, 0); }
		public TerminalNode HAT() { return getToken(ArcticSparkSqlParser.HAT, 0); }
		public TerminalNode PIPE() { return getToken(ArcticSparkSqlParser.PIPE, 0); }
		public ArithmeticBinaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterArithmeticBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitArithmeticBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitArithmeticBinary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ArithmeticUnaryContext extends ValueExpressionContext {
		public Token operator;
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSparkSqlParser.PLUS, 0); }
		public TerminalNode TILDE() { return getToken(ArcticSparkSqlParser.TILDE, 0); }
		public ArithmeticUnaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterArithmeticUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitArithmeticUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitArithmeticUnary(this);
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
		int _startState = 150;
		enterRecursionRule(_localctx, 150, RULE_valueExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1384);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,172,_ctx) ) {
			case 1:
				{
				_localctx = new ValueExpressionDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1381);
				primaryExpression(0);
				}
				break;
			case 2:
				{
				_localctx = new ArithmeticUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1382);
				((ArithmeticUnaryContext)_localctx).operator = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (TILDE - 136)))) != 0)) ) {
					((ArithmeticUnaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1383);
				valueExpression(7);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1407);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,174,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1405);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,173,_ctx) ) {
					case 1:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1386);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1387);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 138)) & ~0x3f) == 0 && ((1L << (_la - 138)) & ((1L << (ASTERISK - 138)) | (1L << (SLASH - 138)) | (1L << (PERCENT - 138)) | (1L << (DIV - 138)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1388);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(7);
						}
						break;
					case 2:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1389);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1390);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (CONCAT_PIPE - 136)))) != 0)) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1391);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(6);
						}
						break;
					case 3:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1392);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1393);
						((ArithmeticBinaryContext)_localctx).operator = match(AMPERSAND);
						setState(1394);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(5);
						}
						break;
					case 4:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1395);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1396);
						((ArithmeticBinaryContext)_localctx).operator = match(HAT);
						setState(1397);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(4);
						}
						break;
					case 5:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1398);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1399);
						((ArithmeticBinaryContext)_localctx).operator = match(PIPE);
						setState(1400);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(3);
						}
						break;
					case 6:
						{
						_localctx = new ComparisonContext(new ValueExpressionContext(_parentctx, _parentState));
						((ComparisonContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(1401);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1402);
						comparisonOperator();
						setState(1403);
						((ComparisonContext)_localctx).right = valueExpression(2);
						}
						break;
					}
					} 
				}
				setState(1409);
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
		public TerminalNode STRUCT() { return getToken(ArcticSparkSqlParser.STRUCT, 0); }
		public List<NamedExpressionContext> namedExpression() {
			return getRuleContexts(NamedExpressionContext.class);
		}
		public NamedExpressionContext namedExpression(int i) {
			return getRuleContext(NamedExpressionContext.class,i);
		}
		public StructContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterStruct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitStruct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitStruct(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterDereference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitDereference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitDereference(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleCaseContext extends PrimaryExpressionContext {
		public ExpressionContext value;
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticSparkSqlParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticSparkSqlParser.END, 0); }
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
		public TerminalNode ELSE() { return getToken(ArcticSparkSqlParser.ELSE, 0); }
		public SimpleCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSimpleCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSimpleCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSimpleCase(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterColumnReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitColumnReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitColumnReference(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterRowConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitRowConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitRowConstructor(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LastContext extends PrimaryExpressionContext {
		public TerminalNode LAST() { return getToken(ArcticSparkSqlParser.LAST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticSparkSqlParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSparkSqlParser.NULLS, 0); }
		public LastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterLast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitLast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitLast(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StarContext extends PrimaryExpressionContext {
		public TerminalNode ASTERISK() { return getToken(ArcticSparkSqlParser.ASTERISK, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public StarContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterStar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitStar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitStar(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSubscript(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSubscript(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSubscript(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSubqueryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSubqueryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSubqueryExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CastContext extends PrimaryExpressionContext {
		public TerminalNode CAST() { return getToken(ArcticSparkSqlParser.CAST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public CastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitCast(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterConstantDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitConstantDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitConstantDefault(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterParenthesizedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitParenthesizedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitParenthesizedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FunctionCallContext extends PrimaryExpressionContext {
		public ExpressionContext expression;
		public List<ExpressionContext> argument = new ArrayList<ExpressionContext>();
		public Token trimOption;
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode OVER() { return getToken(ArcticSparkSqlParser.OVER, 0); }
		public WindowSpecContext windowSpec() {
			return getRuleContext(WindowSpecContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ArcticSparkSqlParser.FROM, 0); }
		public TerminalNode BOTH() { return getToken(ArcticSparkSqlParser.BOTH, 0); }
		public TerminalNode LEADING() { return getToken(ArcticSparkSqlParser.LEADING, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticSparkSqlParser.TRAILING, 0); }
		public FunctionCallContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SearchedCaseContext extends PrimaryExpressionContext {
		public ExpressionContext elseExpression;
		public TerminalNode CASE() { return getToken(ArcticSparkSqlParser.CASE, 0); }
		public TerminalNode END() { return getToken(ArcticSparkSqlParser.END, 0); }
		public List<WhenClauseContext> whenClause() {
			return getRuleContexts(WhenClauseContext.class);
		}
		public WhenClauseContext whenClause(int i) {
			return getRuleContext(WhenClauseContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(ArcticSparkSqlParser.ELSE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SearchedCaseContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSearchedCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSearchedCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSearchedCase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PositionContext extends PrimaryExpressionContext {
		public ValueExpressionContext substr;
		public ValueExpressionContext str;
		public TerminalNode POSITION() { return getToken(ArcticSparkSqlParser.POSITION, 0); }
		public TerminalNode IN() { return getToken(ArcticSparkSqlParser.IN, 0); }
		public List<ValueExpressionContext> valueExpression() {
			return getRuleContexts(ValueExpressionContext.class);
		}
		public ValueExpressionContext valueExpression(int i) {
			return getRuleContext(ValueExpressionContext.class,i);
		}
		public PositionContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPosition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FirstContext extends PrimaryExpressionContext {
		public TerminalNode FIRST() { return getToken(ArcticSparkSqlParser.FIRST, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode IGNORE() { return getToken(ArcticSparkSqlParser.IGNORE, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSparkSqlParser.NULLS, 0); }
		public FirstContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitFirst(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitFirst(this);
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
			setState(1534);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,188,_ctx) ) {
			case 1:
				{
				_localctx = new SearchedCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(1411);
				match(CASE);
				setState(1413); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1412);
					whenClause();
					}
					}
					setState(1415); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(1419);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(1417);
					match(ELSE);
					setState(1418);
					((SearchedCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(1421);
				match(END);
				}
				break;
			case 2:
				{
				_localctx = new SimpleCaseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1423);
				match(CASE);
				setState(1424);
				((SimpleCaseContext)_localctx).value = expression();
				setState(1426); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1425);
					whenClause();
					}
					}
					setState(1428); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WHEN );
				setState(1432);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(1430);
					match(ELSE);
					setState(1431);
					((SimpleCaseContext)_localctx).elseExpression = expression();
					}
				}

				setState(1434);
				match(END);
				}
				break;
			case 3:
				{
				_localctx = new CastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1436);
				match(CAST);
				setState(1437);
				match(T__0);
				setState(1438);
				expression();
				setState(1439);
				match(AS);
				setState(1440);
				dataType();
				setState(1441);
				match(T__2);
				}
				break;
			case 4:
				{
				_localctx = new StructContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1443);
				match(STRUCT);
				setState(1444);
				match(T__0);
				setState(1453);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (PERCENTLIT - 136)) | (1L << (BUCKET - 136)) | (1L << (OUT - 136)) | (1L << (OF - 136)) | (1L << (SORT - 136)) | (1L << (CLUSTER - 136)) | (1L << (DISTRIBUTE - 136)) | (1L << (OVERWRITE - 136)) | (1L << (TRANSFORM - 136)) | (1L << (REDUCE - 136)) | (1L << (SERDE - 136)) | (1L << (SERDEPROPERTIES - 136)) | (1L << (RECORDREADER - 136)) | (1L << (RECORDWRITER - 136)) | (1L << (DELIMITED - 136)) | (1L << (FIELDS - 136)) | (1L << (TERMINATED - 136)) | (1L << (COLLECTION - 136)) | (1L << (ITEMS - 136)) | (1L << (KEYS - 136)) | (1L << (ESCAPED - 136)) | (1L << (LINES - 136)) | (1L << (SEPARATED - 136)) | (1L << (FUNCTION - 136)) | (1L << (EXTENDED - 136)) | (1L << (REFRESH - 136)) | (1L << (CLEAR - 136)) | (1L << (CACHE - 136)) | (1L << (UNCACHE - 136)) | (1L << (LAZY - 136)) | (1L << (FORMATTED - 136)) | (1L << (GLOBAL - 136)) | (1L << (TEMPORARY - 136)) | (1L << (OPTIONS - 136)) | (1L << (UNSET - 136)) | (1L << (TBLPROPERTIES - 136)) | (1L << (DBPROPERTIES - 136)) | (1L << (BUCKETS - 136)) | (1L << (SKEWED - 136)) | (1L << (STORED - 136)) | (1L << (DIRECTORIES - 136)) | (1L << (LOCATION - 136)) | (1L << (EXCHANGE - 136)) | (1L << (ARCHIVE - 136)) | (1L << (UNARCHIVE - 136)) | (1L << (FILEFORMAT - 136)) | (1L << (TOUCH - 136)) | (1L << (COMPACT - 136)) | (1L << (CONCATENATE - 136)) | (1L << (CHANGE - 136)) | (1L << (CASCADE - 136)) | (1L << (RESTRICT - 136)))) != 0) || ((((_la - 200)) & ~0x3f) == 0 && ((1L << (_la - 200)) & ((1L << (CLUSTERED - 200)) | (1L << (SORTED - 200)) | (1L << (PURGE - 200)) | (1L << (INPUTFORMAT - 200)) | (1L << (OUTPUTFORMAT - 200)) | (1L << (DATABASE - 200)) | (1L << (DATABASES - 200)) | (1L << (DFS - 200)) | (1L << (TRUNCATE - 200)) | (1L << (ANALYZE - 200)) | (1L << (COMPUTE - 200)) | (1L << (LIST - 200)) | (1L << (STATISTICS - 200)) | (1L << (PARTITIONED - 200)) | (1L << (EXTERNAL - 200)) | (1L << (DEFINED - 200)) | (1L << (REVOKE - 200)) | (1L << (GRANT - 200)) | (1L << (LOCK - 200)) | (1L << (UNLOCK - 200)) | (1L << (MSCK - 200)) | (1L << (REPAIR - 200)) | (1L << (RECOVER - 200)) | (1L << (EXPORT - 200)) | (1L << (IMPORT - 200)) | (1L << (LOAD - 200)) | (1L << (ROLE - 200)) | (1L << (ROLES - 200)) | (1L << (COMPACTIONS - 200)) | (1L << (PRINCIPALS - 200)) | (1L << (TRANSACTIONS - 200)) | (1L << (INDEX - 200)) | (1L << (INDEXES - 200)) | (1L << (LOCKS - 200)) | (1L << (OPTION - 200)) | (1L << (ANTI - 200)) | (1L << (LOCAL - 200)) | (1L << (INPATH - 200)) | (1L << (STRING - 200)) | (1L << (BIGINT_LITERAL - 200)) | (1L << (SMALLINT_LITERAL - 200)) | (1L << (TINYINT_LITERAL - 200)) | (1L << (INTEGER_VALUE - 200)) | (1L << (DECIMAL_VALUE - 200)) | (1L << (DOUBLE_LITERAL - 200)) | (1L << (BIGDECIMAL_LITERAL - 200)) | (1L << (IDENTIFIER - 200)) | (1L << (BACKQUOTED_IDENTIFIER - 200)))) != 0)) {
					{
					setState(1445);
					((StructContext)_localctx).namedExpression = namedExpression();
					((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
					setState(1450);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(1446);
						match(T__1);
						setState(1447);
						((StructContext)_localctx).namedExpression = namedExpression();
						((StructContext)_localctx).argument.add(((StructContext)_localctx).namedExpression);
						}
						}
						setState(1452);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1455);
				match(T__2);
				}
				break;
			case 5:
				{
				_localctx = new FirstContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1456);
				match(FIRST);
				setState(1457);
				match(T__0);
				setState(1458);
				expression();
				setState(1461);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(1459);
					match(IGNORE);
					setState(1460);
					match(NULLS);
					}
				}

				setState(1463);
				match(T__2);
				}
				break;
			case 6:
				{
				_localctx = new LastContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1465);
				match(LAST);
				setState(1466);
				match(T__0);
				setState(1467);
				expression();
				setState(1470);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IGNORE) {
					{
					setState(1468);
					match(IGNORE);
					setState(1469);
					match(NULLS);
					}
				}

				setState(1472);
				match(T__2);
				}
				break;
			case 7:
				{
				_localctx = new PositionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1474);
				match(POSITION);
				setState(1475);
				match(T__0);
				setState(1476);
				((PositionContext)_localctx).substr = valueExpression(0);
				setState(1477);
				match(IN);
				setState(1478);
				((PositionContext)_localctx).str = valueExpression(0);
				setState(1479);
				match(T__2);
				}
				break;
			case 8:
				{
				_localctx = new ConstantDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1481);
				constant();
				}
				break;
			case 9:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1482);
				match(ASTERISK);
				}
				break;
			case 10:
				{
				_localctx = new StarContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1483);
				qualifiedName();
				setState(1484);
				match(T__3);
				setState(1485);
				match(ASTERISK);
				}
				break;
			case 11:
				{
				_localctx = new RowConstructorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1487);
				match(T__0);
				setState(1488);
				namedExpression();
				setState(1491); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1489);
					match(T__1);
					setState(1490);
					namedExpression();
					}
					}
					setState(1493); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__1 );
				setState(1495);
				match(T__2);
				}
				break;
			case 12:
				{
				_localctx = new SubqueryExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1497);
				match(T__0);
				setState(1498);
				query();
				setState(1499);
				match(T__2);
				}
				break;
			case 13:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1501);
				qualifiedName();
				setState(1502);
				match(T__0);
				setState(1514);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (PERCENTLIT - 136)) | (1L << (BUCKET - 136)) | (1L << (OUT - 136)) | (1L << (OF - 136)) | (1L << (SORT - 136)) | (1L << (CLUSTER - 136)) | (1L << (DISTRIBUTE - 136)) | (1L << (OVERWRITE - 136)) | (1L << (TRANSFORM - 136)) | (1L << (REDUCE - 136)) | (1L << (SERDE - 136)) | (1L << (SERDEPROPERTIES - 136)) | (1L << (RECORDREADER - 136)) | (1L << (RECORDWRITER - 136)) | (1L << (DELIMITED - 136)) | (1L << (FIELDS - 136)) | (1L << (TERMINATED - 136)) | (1L << (COLLECTION - 136)) | (1L << (ITEMS - 136)) | (1L << (KEYS - 136)) | (1L << (ESCAPED - 136)) | (1L << (LINES - 136)) | (1L << (SEPARATED - 136)) | (1L << (FUNCTION - 136)) | (1L << (EXTENDED - 136)) | (1L << (REFRESH - 136)) | (1L << (CLEAR - 136)) | (1L << (CACHE - 136)) | (1L << (UNCACHE - 136)) | (1L << (LAZY - 136)) | (1L << (FORMATTED - 136)) | (1L << (GLOBAL - 136)) | (1L << (TEMPORARY - 136)) | (1L << (OPTIONS - 136)) | (1L << (UNSET - 136)) | (1L << (TBLPROPERTIES - 136)) | (1L << (DBPROPERTIES - 136)) | (1L << (BUCKETS - 136)) | (1L << (SKEWED - 136)) | (1L << (STORED - 136)) | (1L << (DIRECTORIES - 136)) | (1L << (LOCATION - 136)) | (1L << (EXCHANGE - 136)) | (1L << (ARCHIVE - 136)) | (1L << (UNARCHIVE - 136)) | (1L << (FILEFORMAT - 136)) | (1L << (TOUCH - 136)) | (1L << (COMPACT - 136)) | (1L << (CONCATENATE - 136)) | (1L << (CHANGE - 136)) | (1L << (CASCADE - 136)) | (1L << (RESTRICT - 136)))) != 0) || ((((_la - 200)) & ~0x3f) == 0 && ((1L << (_la - 200)) & ((1L << (CLUSTERED - 200)) | (1L << (SORTED - 200)) | (1L << (PURGE - 200)) | (1L << (INPUTFORMAT - 200)) | (1L << (OUTPUTFORMAT - 200)) | (1L << (DATABASE - 200)) | (1L << (DATABASES - 200)) | (1L << (DFS - 200)) | (1L << (TRUNCATE - 200)) | (1L << (ANALYZE - 200)) | (1L << (COMPUTE - 200)) | (1L << (LIST - 200)) | (1L << (STATISTICS - 200)) | (1L << (PARTITIONED - 200)) | (1L << (EXTERNAL - 200)) | (1L << (DEFINED - 200)) | (1L << (REVOKE - 200)) | (1L << (GRANT - 200)) | (1L << (LOCK - 200)) | (1L << (UNLOCK - 200)) | (1L << (MSCK - 200)) | (1L << (REPAIR - 200)) | (1L << (RECOVER - 200)) | (1L << (EXPORT - 200)) | (1L << (IMPORT - 200)) | (1L << (LOAD - 200)) | (1L << (ROLE - 200)) | (1L << (ROLES - 200)) | (1L << (COMPACTIONS - 200)) | (1L << (PRINCIPALS - 200)) | (1L << (TRANSACTIONS - 200)) | (1L << (INDEX - 200)) | (1L << (INDEXES - 200)) | (1L << (LOCKS - 200)) | (1L << (OPTION - 200)) | (1L << (ANTI - 200)) | (1L << (LOCAL - 200)) | (1L << (INPATH - 200)) | (1L << (STRING - 200)) | (1L << (BIGINT_LITERAL - 200)) | (1L << (SMALLINT_LITERAL - 200)) | (1L << (TINYINT_LITERAL - 200)) | (1L << (INTEGER_VALUE - 200)) | (1L << (DECIMAL_VALUE - 200)) | (1L << (DOUBLE_LITERAL - 200)) | (1L << (BIGDECIMAL_LITERAL - 200)) | (1L << (IDENTIFIER - 200)) | (1L << (BACKQUOTED_IDENTIFIER - 200)))) != 0)) {
					{
					setState(1504);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,184,_ctx) ) {
					case 1:
						{
						setState(1503);
						setQuantifier();
						}
						break;
					}
					setState(1506);
					((FunctionCallContext)_localctx).expression = expression();
					((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
					setState(1511);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(1507);
						match(T__1);
						setState(1508);
						((FunctionCallContext)_localctx).expression = expression();
						((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
						}
						}
						setState(1513);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(1516);
				match(T__2);
				setState(1519);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,187,_ctx) ) {
				case 1:
					{
					setState(1517);
					match(OVER);
					setState(1518);
					windowSpec();
					}
					break;
				}
				}
				break;
			case 14:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1521);
				qualifiedName();
				setState(1522);
				match(T__0);
				setState(1523);
				((FunctionCallContext)_localctx).trimOption = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 123)) & ~0x3f) == 0 && ((1L << (_la - 123)) & ((1L << (BOTH - 123)) | (1L << (LEADING - 123)) | (1L << (TRAILING - 123)))) != 0)) ) {
					((FunctionCallContext)_localctx).trimOption = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1524);
				((FunctionCallContext)_localctx).expression = expression();
				((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
				setState(1525);
				match(FROM);
				setState(1526);
				((FunctionCallContext)_localctx).expression = expression();
				((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
				setState(1527);
				match(T__2);
				}
				break;
			case 15:
				{
				_localctx = new ColumnReferenceContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1529);
				identifier();
				}
				break;
			case 16:
				{
				_localctx = new ParenthesizedExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(1530);
				match(T__0);
				setState(1531);
				expression();
				setState(1532);
				match(T__2);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1546);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,190,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1544);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,189,_ctx) ) {
					case 1:
						{
						_localctx = new SubscriptContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((SubscriptContext)_localctx).value = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(1536);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1537);
						match(T__6);
						setState(1538);
						((SubscriptContext)_localctx).index = valueExpression(0);
						setState(1539);
						match(T__7);
						}
						break;
					case 2:
						{
						_localctx = new DereferenceContext(new PrimaryExpressionContext(_parentctx, _parentState));
						((DereferenceContext)_localctx).base = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_primaryExpression);
						setState(1541);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1542);
						match(T__3);
						setState(1543);
						((DereferenceContext)_localctx).fieldName = identifier();
						}
						break;
					}
					} 
				}
				setState(1548);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,190,_ctx);
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
		public TerminalNode NULL() { return getToken(ArcticSparkSqlParser.NULL, 0); }
		public NullLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNullLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNullLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNullLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ConstantContext {
		public List<TerminalNode> STRING() { return getTokens(ArcticSparkSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(ArcticSparkSqlParser.STRING, i);
		}
		public StringLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TypeConstructorContext extends ConstantContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public TypeConstructorContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTypeConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTypeConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTypeConstructor(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIntervalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIntervalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIntervalLiteral(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNumericLiteral(this);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_constant);
		try {
			int _alt;
			setState(1561);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,192,_ctx) ) {
			case 1:
				_localctx = new NullLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1549);
				match(NULL);
				}
				break;
			case 2:
				_localctx = new IntervalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1550);
				interval();
				}
				break;
			case 3:
				_localctx = new TypeConstructorContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1551);
				identifier();
				setState(1552);
				match(STRING);
				}
				break;
			case 4:
				_localctx = new NumericLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1554);
				number();
				}
				break;
			case 5:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1555);
				booleanValue();
				}
				break;
			case 6:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1557); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(1556);
						match(STRING);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(1559); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,191,_ctx);
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
		public TerminalNode EQ() { return getToken(ArcticSparkSqlParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(ArcticSparkSqlParser.NEQ, 0); }
		public TerminalNode NEQJ() { return getToken(ArcticSparkSqlParser.NEQJ, 0); }
		public TerminalNode LT() { return getToken(ArcticSparkSqlParser.LT, 0); }
		public TerminalNode LTE() { return getToken(ArcticSparkSqlParser.LTE, 0); }
		public TerminalNode GT() { return getToken(ArcticSparkSqlParser.GT, 0); }
		public TerminalNode GTE() { return getToken(ArcticSparkSqlParser.GTE, 0); }
		public TerminalNode NSEQ() { return getToken(ArcticSparkSqlParser.NSEQ, 0); }
		public ComparisonOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterComparisonOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitComparisonOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitComparisonOperator(this);
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
			setState(1563);
			_la = _input.LA(1);
			if ( !(((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (EQ - 128)) | (1L << (NSEQ - 128)) | (1L << (NEQ - 128)) | (1L << (NEQJ - 128)) | (1L << (LT - 128)) | (1L << (LTE - 128)) | (1L << (GT - 128)) | (1L << (GTE - 128)))) != 0)) ) {
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
		public TerminalNode PLUS() { return getToken(ArcticSparkSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public TerminalNode ASTERISK() { return getToken(ArcticSparkSqlParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(ArcticSparkSqlParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(ArcticSparkSqlParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(ArcticSparkSqlParser.DIV, 0); }
		public TerminalNode TILDE() { return getToken(ArcticSparkSqlParser.TILDE, 0); }
		public TerminalNode AMPERSAND() { return getToken(ArcticSparkSqlParser.AMPERSAND, 0); }
		public TerminalNode PIPE() { return getToken(ArcticSparkSqlParser.PIPE, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(ArcticSparkSqlParser.CONCAT_PIPE, 0); }
		public TerminalNode HAT() { return getToken(ArcticSparkSqlParser.HAT, 0); }
		public ArithmeticOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterArithmeticOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitArithmeticOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitArithmeticOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticOperatorContext arithmeticOperator() throws RecognitionException {
		ArithmeticOperatorContext _localctx = new ArithmeticOperatorContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_arithmeticOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1565);
			_la = _input.LA(1);
			if ( !(((((_la - 136)) & ~0x3f) == 0 && ((1L << (_la - 136)) & ((1L << (PLUS - 136)) | (1L << (MINUS - 136)) | (1L << (ASTERISK - 136)) | (1L << (SLASH - 136)) | (1L << (PERCENT - 136)) | (1L << (DIV - 136)) | (1L << (TILDE - 136)) | (1L << (AMPERSAND - 136)) | (1L << (PIPE - 136)) | (1L << (CONCAT_PIPE - 136)) | (1L << (HAT - 136)))) != 0)) ) {
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
		public TerminalNode OR() { return getToken(ArcticSparkSqlParser.OR, 0); }
		public TerminalNode AND() { return getToken(ArcticSparkSqlParser.AND, 0); }
		public TerminalNode IN() { return getToken(ArcticSparkSqlParser.IN, 0); }
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public PredicateOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicateOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPredicateOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPredicateOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPredicateOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateOperatorContext predicateOperator() throws RecognitionException {
		PredicateOperatorContext _localctx = new PredicateOperatorContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_predicateOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1567);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT))) != 0)) ) {
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
		public TerminalNode TRUE() { return getToken(ArcticSparkSqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSparkSqlParser.FALSE, 0); }
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1569);
			_la = _input.LA(1);
			if ( !(_la==TRUE || _la==FALSE) ) {
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
		public TerminalNode INTERVAL() { return getToken(ArcticSparkSqlParser.INTERVAL, 0); }
		public List<IntervalFieldContext> intervalField() {
			return getRuleContexts(IntervalFieldContext.class);
		}
		public IntervalFieldContext intervalField(int i) {
			return getRuleContext(IntervalFieldContext.class,i);
		}
		public IntervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitInterval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitInterval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalContext interval() throws RecognitionException {
		IntervalContext _localctx = new IntervalContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_interval);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1571);
			match(INTERVAL);
			setState(1575);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,193,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1572);
					intervalField();
					}
					} 
				}
				setState(1577);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,193,_ctx);
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

	public static class IntervalFieldContext extends ParserRuleContext {
		public IntervalValueContext value;
		public IdentifierContext unit;
		public IdentifierContext to;
		public IntervalValueContext intervalValue() {
			return getRuleContext(IntervalValueContext.class,0);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode TO() { return getToken(ArcticSparkSqlParser.TO, 0); }
		public IntervalFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intervalField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIntervalField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIntervalField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIntervalField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalFieldContext intervalField() throws RecognitionException {
		IntervalFieldContext _localctx = new IntervalFieldContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_intervalField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1578);
			((IntervalFieldContext)_localctx).value = intervalValue();
			setState(1579);
			((IntervalFieldContext)_localctx).unit = identifier();
			setState(1582);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,194,_ctx) ) {
			case 1:
				{
				setState(1580);
				match(TO);
				setState(1581);
				((IntervalFieldContext)_localctx).to = identifier();
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

	public static class IntervalValueContext extends ParserRuleContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode PLUS() { return getToken(ArcticSparkSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public IntervalValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intervalValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIntervalValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIntervalValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIntervalValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntervalValueContext intervalValue() throws RecognitionException {
		IntervalValueContext _localctx = new IntervalValueContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_intervalValue);
		int _la;
		try {
			setState(1589);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PLUS:
			case MINUS:
			case INTEGER_VALUE:
			case DECIMAL_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1585);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PLUS || _la==MINUS) {
					{
					setState(1584);
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

				setState(1587);
				_la = _input.LA(1);
				if ( !(_la==INTEGER_VALUE || _la==DECIMAL_VALUE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(1588);
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

	public static class ColPositionContext extends ParserRuleContext {
		public TerminalNode FIRST() { return getToken(ArcticSparkSqlParser.FIRST, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSparkSqlParser.AFTER, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ColPositionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colPosition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterColPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitColPosition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitColPosition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColPositionContext colPosition() throws RecognitionException {
		ColPositionContext _localctx = new ColPositionContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_colPosition);
		try {
			setState(1594);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FIRST:
				enterOuterAlt(_localctx, 1);
				{
				setState(1591);
				match(FIRST);
				}
				break;
			case AFTER:
				enterOuterAlt(_localctx, 2);
				{
				setState(1592);
				match(AFTER);
				setState(1593);
				identifier();
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
		public TerminalNode LT() { return getToken(ArcticSparkSqlParser.LT, 0); }
		public List<DataTypeContext> dataType() {
			return getRuleContexts(DataTypeContext.class);
		}
		public DataTypeContext dataType(int i) {
			return getRuleContext(DataTypeContext.class,i);
		}
		public TerminalNode GT() { return getToken(ArcticSparkSqlParser.GT, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSparkSqlParser.ARRAY, 0); }
		public TerminalNode MAP() { return getToken(ArcticSparkSqlParser.MAP, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSparkSqlParser.STRUCT, 0); }
		public TerminalNode NEQ() { return getToken(ArcticSparkSqlParser.NEQ, 0); }
		public ComplexColTypeListContext complexColTypeList() {
			return getRuleContext(ComplexColTypeListContext.class,0);
		}
		public ComplexDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterComplexDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitComplexDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitComplexDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrimitiveDataTypeContext extends DataTypeContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<TerminalNode> INTEGER_VALUE() { return getTokens(ArcticSparkSqlParser.INTEGER_VALUE); }
		public TerminalNode INTEGER_VALUE(int i) {
			return getToken(ArcticSparkSqlParser.INTEGER_VALUE, i);
		}
		public PrimitiveDataTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterPrimitiveDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitPrimitiveDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitPrimitiveDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_dataType);
		int _la;
		try {
			setState(1630);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,202,_ctx) ) {
			case 1:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1596);
				((ComplexDataTypeContext)_localctx).complex = match(ARRAY);
				setState(1597);
				match(LT);
				setState(1598);
				dataType();
				setState(1599);
				match(GT);
				}
				break;
			case 2:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1601);
				((ComplexDataTypeContext)_localctx).complex = match(MAP);
				setState(1602);
				match(LT);
				setState(1603);
				dataType();
				setState(1604);
				match(T__1);
				setState(1605);
				dataType();
				setState(1606);
				match(GT);
				}
				break;
			case 3:
				_localctx = new ComplexDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1608);
				((ComplexDataTypeContext)_localctx).complex = match(STRUCT);
				setState(1615);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LT:
					{
					setState(1609);
					match(LT);
					setState(1611);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << JOIN) | (1L << CROSS) | (1L << OUTER) | (1L << INNER) | (1L << LEFT) | (1L << SEMI) | (1L << RIGHT) | (1L << FULL) | (1L << NATURAL) | (1L << ON) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (UNION - 64)) | (1L << (EXCEPT - 64)) | (1L << (SETMINUS - 64)) | (1L << (INTERSECT - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (DIV - 141)) | (1L << (PERCENTLIT - 141)) | (1L << (BUCKET - 141)) | (1L << (OUT - 141)) | (1L << (OF - 141)) | (1L << (SORT - 141)) | (1L << (CLUSTER - 141)) | (1L << (DISTRIBUTE - 141)) | (1L << (OVERWRITE - 141)) | (1L << (TRANSFORM - 141)) | (1L << (REDUCE - 141)) | (1L << (SERDE - 141)) | (1L << (SERDEPROPERTIES - 141)) | (1L << (RECORDREADER - 141)) | (1L << (RECORDWRITER - 141)) | (1L << (DELIMITED - 141)) | (1L << (FIELDS - 141)) | (1L << (TERMINATED - 141)) | (1L << (COLLECTION - 141)) | (1L << (ITEMS - 141)) | (1L << (KEYS - 141)) | (1L << (ESCAPED - 141)) | (1L << (LINES - 141)) | (1L << (SEPARATED - 141)) | (1L << (FUNCTION - 141)) | (1L << (EXTENDED - 141)) | (1L << (REFRESH - 141)) | (1L << (CLEAR - 141)) | (1L << (CACHE - 141)) | (1L << (UNCACHE - 141)) | (1L << (LAZY - 141)) | (1L << (FORMATTED - 141)) | (1L << (GLOBAL - 141)) | (1L << (TEMPORARY - 141)) | (1L << (OPTIONS - 141)) | (1L << (UNSET - 141)) | (1L << (TBLPROPERTIES - 141)) | (1L << (DBPROPERTIES - 141)) | (1L << (BUCKETS - 141)) | (1L << (SKEWED - 141)) | (1L << (STORED - 141)) | (1L << (DIRECTORIES - 141)) | (1L << (LOCATION - 141)) | (1L << (EXCHANGE - 141)) | (1L << (ARCHIVE - 141)) | (1L << (UNARCHIVE - 141)) | (1L << (FILEFORMAT - 141)) | (1L << (TOUCH - 141)) | (1L << (COMPACT - 141)) | (1L << (CONCATENATE - 141)) | (1L << (CHANGE - 141)) | (1L << (CASCADE - 141)) | (1L << (RESTRICT - 141)) | (1L << (CLUSTERED - 141)) | (1L << (SORTED - 141)) | (1L << (PURGE - 141)) | (1L << (INPUTFORMAT - 141)) | (1L << (OUTPUTFORMAT - 141)))) != 0) || ((((_la - 205)) & ~0x3f) == 0 && ((1L << (_la - 205)) & ((1L << (DATABASE - 205)) | (1L << (DATABASES - 205)) | (1L << (DFS - 205)) | (1L << (TRUNCATE - 205)) | (1L << (ANALYZE - 205)) | (1L << (COMPUTE - 205)) | (1L << (LIST - 205)) | (1L << (STATISTICS - 205)) | (1L << (PARTITIONED - 205)) | (1L << (EXTERNAL - 205)) | (1L << (DEFINED - 205)) | (1L << (REVOKE - 205)) | (1L << (GRANT - 205)) | (1L << (LOCK - 205)) | (1L << (UNLOCK - 205)) | (1L << (MSCK - 205)) | (1L << (REPAIR - 205)) | (1L << (RECOVER - 205)) | (1L << (EXPORT - 205)) | (1L << (IMPORT - 205)) | (1L << (LOAD - 205)) | (1L << (ROLE - 205)) | (1L << (ROLES - 205)) | (1L << (COMPACTIONS - 205)) | (1L << (PRINCIPALS - 205)) | (1L << (TRANSACTIONS - 205)) | (1L << (INDEX - 205)) | (1L << (INDEXES - 205)) | (1L << (LOCKS - 205)) | (1L << (OPTION - 205)) | (1L << (ANTI - 205)) | (1L << (LOCAL - 205)) | (1L << (INPATH - 205)) | (1L << (IDENTIFIER - 205)) | (1L << (BACKQUOTED_IDENTIFIER - 205)))) != 0)) {
						{
						setState(1610);
						complexColTypeList();
						}
					}

					setState(1613);
					match(GT);
					}
					break;
				case NEQ:
					{
					setState(1614);
					match(NEQ);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 4:
				_localctx = new PrimitiveDataTypeContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1617);
				identifier();
				setState(1628);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,201,_ctx) ) {
				case 1:
					{
					setState(1618);
					match(T__0);
					setState(1619);
					match(INTEGER_VALUE);
					setState(1624);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(1620);
						match(T__1);
						setState(1621);
						match(INTEGER_VALUE);
						}
						}
						setState(1626);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(1627);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitColTypeList(this);
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
			setState(1632);
			colType();
			setState(1637);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,203,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1633);
					match(T__1);
					setState(1634);
					colType();
					}
					} 
				}
				setState(1639);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,203,_ctx);
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
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode COMMENT() { return getToken(ArcticSparkSqlParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public ColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeContext colType() throws RecognitionException {
		ColTypeContext _localctx = new ColTypeContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_colType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1640);
			identifier();
			setState(1641);
			dataType();
			setState(1644);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,204,_ctx) ) {
			case 1:
				{
				setState(1642);
				match(COMMENT);
				setState(1643);
				match(STRING);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterComplexColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitComplexColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitComplexColTypeList(this);
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
			setState(1646);
			complexColType();
			setState(1651);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1647);
				match(T__1);
				setState(1648);
				complexColType();
				}
				}
				setState(1653);
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
		public TerminalNode COMMENT() { return getToken(ArcticSparkSqlParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(ArcticSparkSqlParser.STRING, 0); }
		public ComplexColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexColType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterComplexColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitComplexColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitComplexColType(this);
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
			setState(1654);
			identifier();
			setState(1655);
			match(T__8);
			setState(1656);
			dataType();
			setState(1659);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(1657);
				match(COMMENT);
				setState(1658);
				match(STRING);
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
		public TerminalNode WHEN() { return getToken(ArcticSparkSqlParser.WHEN, 0); }
		public TerminalNode THEN() { return getToken(ArcticSparkSqlParser.THEN, 0); }
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterWhenClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitWhenClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitWhenClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhenClauseContext whenClause() throws RecognitionException {
		WhenClauseContext _localctx = new WhenClauseContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_whenClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1661);
			match(WHEN);
			setState(1662);
			((WhenClauseContext)_localctx).condition = expression();
			setState(1663);
			match(THEN);
			setState(1664);
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

	public static class WindowsContext extends ParserRuleContext {
		public TerminalNode WINDOW() { return getToken(ArcticSparkSqlParser.WINDOW, 0); }
		public List<NamedWindowContext> namedWindow() {
			return getRuleContexts(NamedWindowContext.class);
		}
		public NamedWindowContext namedWindow(int i) {
			return getRuleContext(NamedWindowContext.class,i);
		}
		public WindowsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windows; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterWindows(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitWindows(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitWindows(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowsContext windows() throws RecognitionException {
		WindowsContext _localctx = new WindowsContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_windows);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1666);
			match(WINDOW);
			setState(1667);
			namedWindow();
			setState(1672);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,207,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1668);
					match(T__1);
					setState(1669);
					namedWindow();
					}
					} 
				}
				setState(1674);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,207,_ctx);
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
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public WindowSpecContext windowSpec() {
			return getRuleContext(WindowSpecContext.class,0);
		}
		public NamedWindowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedWindow; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNamedWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNamedWindow(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNamedWindow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedWindowContext namedWindow() throws RecognitionException {
		NamedWindowContext _localctx = new NamedWindowContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_namedWindow);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1675);
			identifier();
			setState(1676);
			match(AS);
			setState(1677);
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
		public IdentifierContext name;
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public WindowRefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterWindowRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitWindowRef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitWindowRef(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WindowDefContext extends WindowSpecContext {
		public ExpressionContext expression;
		public List<ExpressionContext> partition = new ArrayList<ExpressionContext>();
		public TerminalNode CLUSTER() { return getToken(ArcticSparkSqlParser.CLUSTER, 0); }
		public List<TerminalNode> BY() { return getTokens(ArcticSparkSqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(ArcticSparkSqlParser.BY, i);
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
		public TerminalNode PARTITION() { return getToken(ArcticSparkSqlParser.PARTITION, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSparkSqlParser.ORDER, 0); }
		public TerminalNode SORT() { return getToken(ArcticSparkSqlParser.SORT, 0); }
		public WindowDefContext(WindowSpecContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterWindowDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitWindowDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitWindowDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowSpecContext windowSpec() throws RecognitionException {
		WindowSpecContext _localctx = new WindowSpecContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_windowSpec);
		int _la;
		try {
			setState(1721);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
			case FROM:
			case ADD:
			case AS:
			case ALL:
			case DISTINCT:
			case WHERE:
			case GROUP:
			case BY:
			case GROUPING:
			case SETS:
			case CUBE:
			case ROLLUP:
			case ORDER:
			case HAVING:
			case LIMIT:
			case AT:
			case OR:
			case AND:
			case IN:
			case NOT:
			case NO:
			case EXISTS:
			case BETWEEN:
			case LIKE:
			case RLIKE:
			case IS:
			case NULL:
			case TRUE:
			case FALSE:
			case NULLS:
			case ASC:
			case DESC:
			case FOR:
			case INTERVAL:
			case CASE:
			case WHEN:
			case THEN:
			case ELSE:
			case END:
			case JOIN:
			case CROSS:
			case OUTER:
			case INNER:
			case LEFT:
			case SEMI:
			case RIGHT:
			case FULL:
			case NATURAL:
			case ON:
			case LATERAL:
			case WINDOW:
			case OVER:
			case PARTITION:
			case RANGE:
			case ROWS:
			case UNBOUNDED:
			case PRECEDING:
			case FOLLOWING:
			case CURRENT:
			case FIRST:
			case AFTER:
			case LAST:
			case ROW:
			case WITH:
			case VALUES:
			case CREATE:
			case TABLE:
			case DIRECTORY:
			case VIEW:
			case REPLACE:
			case INSERT:
			case DELETE:
			case INTO:
			case DESCRIBE:
			case EXPLAIN:
			case FORMAT:
			case LOGICAL:
			case CODEGEN:
			case COST:
			case CAST:
			case SHOW:
			case TABLES:
			case COLUMNS:
			case COLUMN:
			case USE:
			case PARTITIONS:
			case FUNCTIONS:
			case DROP:
			case UNION:
			case EXCEPT:
			case SETMINUS:
			case INTERSECT:
			case TO:
			case TABLESAMPLE:
			case STRATIFY:
			case ALTER:
			case RENAME:
			case ARRAY:
			case MAP:
			case STRUCT:
			case COMMENT:
			case SET:
			case RESET:
			case DATA:
			case START:
			case TRANSACTION:
			case COMMIT:
			case ROLLBACK:
			case MACRO:
			case IGNORE:
			case BOTH:
			case LEADING:
			case TRAILING:
			case IF:
			case POSITION:
			case DIV:
			case PERCENTLIT:
			case BUCKET:
			case OUT:
			case OF:
			case SORT:
			case CLUSTER:
			case DISTRIBUTE:
			case OVERWRITE:
			case TRANSFORM:
			case REDUCE:
			case SERDE:
			case SERDEPROPERTIES:
			case RECORDREADER:
			case RECORDWRITER:
			case DELIMITED:
			case FIELDS:
			case TERMINATED:
			case COLLECTION:
			case ITEMS:
			case KEYS:
			case ESCAPED:
			case LINES:
			case SEPARATED:
			case FUNCTION:
			case EXTENDED:
			case REFRESH:
			case CLEAR:
			case CACHE:
			case UNCACHE:
			case LAZY:
			case FORMATTED:
			case GLOBAL:
			case TEMPORARY:
			case OPTIONS:
			case UNSET:
			case TBLPROPERTIES:
			case DBPROPERTIES:
			case BUCKETS:
			case SKEWED:
			case STORED:
			case DIRECTORIES:
			case LOCATION:
			case EXCHANGE:
			case ARCHIVE:
			case UNARCHIVE:
			case FILEFORMAT:
			case TOUCH:
			case COMPACT:
			case CONCATENATE:
			case CHANGE:
			case CASCADE:
			case RESTRICT:
			case CLUSTERED:
			case SORTED:
			case PURGE:
			case INPUTFORMAT:
			case OUTPUTFORMAT:
			case DATABASE:
			case DATABASES:
			case DFS:
			case TRUNCATE:
			case ANALYZE:
			case COMPUTE:
			case LIST:
			case STATISTICS:
			case PARTITIONED:
			case EXTERNAL:
			case DEFINED:
			case REVOKE:
			case GRANT:
			case LOCK:
			case UNLOCK:
			case MSCK:
			case REPAIR:
			case RECOVER:
			case EXPORT:
			case IMPORT:
			case LOAD:
			case ROLE:
			case ROLES:
			case COMPACTIONS:
			case PRINCIPALS:
			case TRANSACTIONS:
			case INDEX:
			case INDEXES:
			case LOCKS:
			case OPTION:
			case ANTI:
			case LOCAL:
			case INPATH:
			case IDENTIFIER:
			case BACKQUOTED_IDENTIFIER:
				_localctx = new WindowRefContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1679);
				((WindowRefContext)_localctx).name = identifier();
				}
				break;
			case T__0:
				_localctx = new WindowDefContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1680);
				match(T__0);
				setState(1715);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case CLUSTER:
					{
					setState(1681);
					match(CLUSTER);
					setState(1682);
					match(BY);
					setState(1683);
					((WindowDefContext)_localctx).expression = expression();
					((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
					setState(1688);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__1) {
						{
						{
						setState(1684);
						match(T__1);
						setState(1685);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						}
						}
						setState(1690);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				case T__2:
				case ORDER:
				case PARTITION:
				case RANGE:
				case ROWS:
				case SORT:
				case DISTRIBUTE:
					{
					setState(1701);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==PARTITION || _la==DISTRIBUTE) {
						{
						setState(1691);
						_la = _input.LA(1);
						if ( !(_la==PARTITION || _la==DISTRIBUTE) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1692);
						match(BY);
						setState(1693);
						((WindowDefContext)_localctx).expression = expression();
						((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
						setState(1698);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__1) {
							{
							{
							setState(1694);
							match(T__1);
							setState(1695);
							((WindowDefContext)_localctx).expression = expression();
							((WindowDefContext)_localctx).partition.add(((WindowDefContext)_localctx).expression);
							}
							}
							setState(1700);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(1713);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ORDER || _la==SORT) {
						{
						setState(1703);
						_la = _input.LA(1);
						if ( !(_la==ORDER || _la==SORT) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1704);
						match(BY);
						setState(1705);
						sortItem();
						setState(1710);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__1) {
							{
							{
							setState(1706);
							match(T__1);
							setState(1707);
							sortItem();
							}
							}
							setState(1712);
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
				setState(1718);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==RANGE || _la==ROWS) {
					{
					setState(1717);
					windowFrame();
					}
				}

				setState(1720);
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

	public static class WindowFrameContext extends ParserRuleContext {
		public Token frameType;
		public FrameBoundContext start;
		public FrameBoundContext end;
		public TerminalNode RANGE() { return getToken(ArcticSparkSqlParser.RANGE, 0); }
		public List<FrameBoundContext> frameBound() {
			return getRuleContexts(FrameBoundContext.class);
		}
		public FrameBoundContext frameBound(int i) {
			return getRuleContext(FrameBoundContext.class,i);
		}
		public TerminalNode ROWS() { return getToken(ArcticSparkSqlParser.ROWS, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSparkSqlParser.BETWEEN, 0); }
		public TerminalNode AND() { return getToken(ArcticSparkSqlParser.AND, 0); }
		public WindowFrameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_windowFrame; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterWindowFrame(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitWindowFrame(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitWindowFrame(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WindowFrameContext windowFrame() throws RecognitionException {
		WindowFrameContext _localctx = new WindowFrameContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_windowFrame);
		try {
			setState(1739);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,216,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1723);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(1724);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1725);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(1726);
				((WindowFrameContext)_localctx).start = frameBound();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1727);
				((WindowFrameContext)_localctx).frameType = match(RANGE);
				setState(1728);
				match(BETWEEN);
				setState(1729);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(1730);
				match(AND);
				setState(1731);
				((WindowFrameContext)_localctx).end = frameBound();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1733);
				((WindowFrameContext)_localctx).frameType = match(ROWS);
				setState(1734);
				match(BETWEEN);
				setState(1735);
				((WindowFrameContext)_localctx).start = frameBound();
				setState(1736);
				match(AND);
				setState(1737);
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
		public TerminalNode UNBOUNDED() { return getToken(ArcticSparkSqlParser.UNBOUNDED, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSparkSqlParser.PRECEDING, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSparkSqlParser.FOLLOWING, 0); }
		public TerminalNode ROW() { return getToken(ArcticSparkSqlParser.ROW, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSparkSqlParser.CURRENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FrameBoundContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_frameBound; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterFrameBound(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitFrameBound(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitFrameBound(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FrameBoundContext frameBound() throws RecognitionException {
		FrameBoundContext _localctx = new FrameBoundContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_frameBound);
		int _la;
		try {
			setState(1748);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,217,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1741);
				match(UNBOUNDED);
				setState(1742);
				((FrameBoundContext)_localctx).boundType = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PRECEDING || _la==FOLLOWING) ) {
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
				setState(1743);
				((FrameBoundContext)_localctx).boundType = match(CURRENT);
				setState(1744);
				match(ROW);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1745);
				expression();
				setState(1746);
				((FrameBoundContext)_localctx).boundType = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==PRECEDING || _la==FOLLOWING) ) {
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1750);
			identifier();
			setState(1755);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,218,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1751);
					match(T__3);
					setState(1752);
					identifier();
					}
					} 
				}
				setState(1757);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,218,_ctx);
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

	public static class IdentifierContext extends ParserRuleContext {
		public StrictIdentifierContext strictIdentifier() {
			return getRuleContext(StrictIdentifierContext.class,0);
		}
		public TerminalNode ANTI() { return getToken(ArcticSparkSqlParser.ANTI, 0); }
		public TerminalNode FULL() { return getToken(ArcticSparkSqlParser.FULL, 0); }
		public TerminalNode INNER() { return getToken(ArcticSparkSqlParser.INNER, 0); }
		public TerminalNode LEFT() { return getToken(ArcticSparkSqlParser.LEFT, 0); }
		public TerminalNode SEMI() { return getToken(ArcticSparkSqlParser.SEMI, 0); }
		public TerminalNode RIGHT() { return getToken(ArcticSparkSqlParser.RIGHT, 0); }
		public TerminalNode NATURAL() { return getToken(ArcticSparkSqlParser.NATURAL, 0); }
		public TerminalNode JOIN() { return getToken(ArcticSparkSqlParser.JOIN, 0); }
		public TerminalNode CROSS() { return getToken(ArcticSparkSqlParser.CROSS, 0); }
		public TerminalNode ON() { return getToken(ArcticSparkSqlParser.ON, 0); }
		public TerminalNode UNION() { return getToken(ArcticSparkSqlParser.UNION, 0); }
		public TerminalNode INTERSECT() { return getToken(ArcticSparkSqlParser.INTERSECT, 0); }
		public TerminalNode EXCEPT() { return getToken(ArcticSparkSqlParser.EXCEPT, 0); }
		public TerminalNode SETMINUS() { return getToken(ArcticSparkSqlParser.SETMINUS, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_identifier);
		try {
			setState(1773);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
			case FROM:
			case ADD:
			case AS:
			case ALL:
			case DISTINCT:
			case WHERE:
			case GROUP:
			case BY:
			case GROUPING:
			case SETS:
			case CUBE:
			case ROLLUP:
			case ORDER:
			case HAVING:
			case LIMIT:
			case AT:
			case OR:
			case AND:
			case IN:
			case NOT:
			case NO:
			case EXISTS:
			case BETWEEN:
			case LIKE:
			case RLIKE:
			case IS:
			case NULL:
			case TRUE:
			case FALSE:
			case NULLS:
			case ASC:
			case DESC:
			case FOR:
			case INTERVAL:
			case CASE:
			case WHEN:
			case THEN:
			case ELSE:
			case END:
			case OUTER:
			case LATERAL:
			case WINDOW:
			case OVER:
			case PARTITION:
			case RANGE:
			case ROWS:
			case UNBOUNDED:
			case PRECEDING:
			case FOLLOWING:
			case CURRENT:
			case FIRST:
			case AFTER:
			case LAST:
			case ROW:
			case WITH:
			case VALUES:
			case CREATE:
			case TABLE:
			case DIRECTORY:
			case VIEW:
			case REPLACE:
			case INSERT:
			case DELETE:
			case INTO:
			case DESCRIBE:
			case EXPLAIN:
			case FORMAT:
			case LOGICAL:
			case CODEGEN:
			case COST:
			case CAST:
			case SHOW:
			case TABLES:
			case COLUMNS:
			case COLUMN:
			case USE:
			case PARTITIONS:
			case FUNCTIONS:
			case DROP:
			case TO:
			case TABLESAMPLE:
			case STRATIFY:
			case ALTER:
			case RENAME:
			case ARRAY:
			case MAP:
			case STRUCT:
			case COMMENT:
			case SET:
			case RESET:
			case DATA:
			case START:
			case TRANSACTION:
			case COMMIT:
			case ROLLBACK:
			case MACRO:
			case IGNORE:
			case BOTH:
			case LEADING:
			case TRAILING:
			case IF:
			case POSITION:
			case DIV:
			case PERCENTLIT:
			case BUCKET:
			case OUT:
			case OF:
			case SORT:
			case CLUSTER:
			case DISTRIBUTE:
			case OVERWRITE:
			case TRANSFORM:
			case REDUCE:
			case SERDE:
			case SERDEPROPERTIES:
			case RECORDREADER:
			case RECORDWRITER:
			case DELIMITED:
			case FIELDS:
			case TERMINATED:
			case COLLECTION:
			case ITEMS:
			case KEYS:
			case ESCAPED:
			case LINES:
			case SEPARATED:
			case FUNCTION:
			case EXTENDED:
			case REFRESH:
			case CLEAR:
			case CACHE:
			case UNCACHE:
			case LAZY:
			case FORMATTED:
			case GLOBAL:
			case TEMPORARY:
			case OPTIONS:
			case UNSET:
			case TBLPROPERTIES:
			case DBPROPERTIES:
			case BUCKETS:
			case SKEWED:
			case STORED:
			case DIRECTORIES:
			case LOCATION:
			case EXCHANGE:
			case ARCHIVE:
			case UNARCHIVE:
			case FILEFORMAT:
			case TOUCH:
			case COMPACT:
			case CONCATENATE:
			case CHANGE:
			case CASCADE:
			case RESTRICT:
			case CLUSTERED:
			case SORTED:
			case PURGE:
			case INPUTFORMAT:
			case OUTPUTFORMAT:
			case DATABASE:
			case DATABASES:
			case DFS:
			case TRUNCATE:
			case ANALYZE:
			case COMPUTE:
			case LIST:
			case STATISTICS:
			case PARTITIONED:
			case EXTERNAL:
			case DEFINED:
			case REVOKE:
			case GRANT:
			case LOCK:
			case UNLOCK:
			case MSCK:
			case REPAIR:
			case RECOVER:
			case EXPORT:
			case IMPORT:
			case LOAD:
			case ROLE:
			case ROLES:
			case COMPACTIONS:
			case PRINCIPALS:
			case TRANSACTIONS:
			case INDEX:
			case INDEXES:
			case LOCKS:
			case OPTION:
			case LOCAL:
			case INPATH:
			case IDENTIFIER:
			case BACKQUOTED_IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1758);
				strictIdentifier();
				}
				break;
			case ANTI:
				enterOuterAlt(_localctx, 2);
				{
				setState(1759);
				match(ANTI);
				}
				break;
			case FULL:
				enterOuterAlt(_localctx, 3);
				{
				setState(1760);
				match(FULL);
				}
				break;
			case INNER:
				enterOuterAlt(_localctx, 4);
				{
				setState(1761);
				match(INNER);
				}
				break;
			case LEFT:
				enterOuterAlt(_localctx, 5);
				{
				setState(1762);
				match(LEFT);
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 6);
				{
				setState(1763);
				match(SEMI);
				}
				break;
			case RIGHT:
				enterOuterAlt(_localctx, 7);
				{
				setState(1764);
				match(RIGHT);
				}
				break;
			case NATURAL:
				enterOuterAlt(_localctx, 8);
				{
				setState(1765);
				match(NATURAL);
				}
				break;
			case JOIN:
				enterOuterAlt(_localctx, 9);
				{
				setState(1766);
				match(JOIN);
				}
				break;
			case CROSS:
				enterOuterAlt(_localctx, 10);
				{
				setState(1767);
				match(CROSS);
				}
				break;
			case ON:
				enterOuterAlt(_localctx, 11);
				{
				setState(1768);
				match(ON);
				}
				break;
			case UNION:
				enterOuterAlt(_localctx, 12);
				{
				setState(1769);
				match(UNION);
				}
				break;
			case INTERSECT:
				enterOuterAlt(_localctx, 13);
				{
				setState(1770);
				match(INTERSECT);
				}
				break;
			case EXCEPT:
				enterOuterAlt(_localctx, 14);
				{
				setState(1771);
				match(EXCEPT);
				}
				break;
			case SETMINUS:
				enterOuterAlt(_localctx, 15);
				{
				setState(1772);
				match(SETMINUS);
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
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQuotedIdentifierAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQuotedIdentifierAlternative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQuotedIdentifierAlternative(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UnquotedIdentifierContext extends StrictIdentifierContext {
		public TerminalNode IDENTIFIER() { return getToken(ArcticSparkSqlParser.IDENTIFIER, 0); }
		public NonReservedContext nonReserved() {
			return getRuleContext(NonReservedContext.class,0);
		}
		public UnquotedIdentifierContext(StrictIdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterUnquotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitUnquotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitUnquotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StrictIdentifierContext strictIdentifier() throws RecognitionException {
		StrictIdentifierContext _localctx = new StrictIdentifierContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_strictIdentifier);
		try {
			setState(1778);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENTIFIER:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1775);
				match(IDENTIFIER);
				}
				break;
			case BACKQUOTED_IDENTIFIER:
				_localctx = new QuotedIdentifierAlternativeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1776);
				quotedIdentifier();
				}
				break;
			case SELECT:
			case FROM:
			case ADD:
			case AS:
			case ALL:
			case DISTINCT:
			case WHERE:
			case GROUP:
			case BY:
			case GROUPING:
			case SETS:
			case CUBE:
			case ROLLUP:
			case ORDER:
			case HAVING:
			case LIMIT:
			case AT:
			case OR:
			case AND:
			case IN:
			case NOT:
			case NO:
			case EXISTS:
			case BETWEEN:
			case LIKE:
			case RLIKE:
			case IS:
			case NULL:
			case TRUE:
			case FALSE:
			case NULLS:
			case ASC:
			case DESC:
			case FOR:
			case INTERVAL:
			case CASE:
			case WHEN:
			case THEN:
			case ELSE:
			case END:
			case OUTER:
			case LATERAL:
			case WINDOW:
			case OVER:
			case PARTITION:
			case RANGE:
			case ROWS:
			case UNBOUNDED:
			case PRECEDING:
			case FOLLOWING:
			case CURRENT:
			case FIRST:
			case AFTER:
			case LAST:
			case ROW:
			case WITH:
			case VALUES:
			case CREATE:
			case TABLE:
			case DIRECTORY:
			case VIEW:
			case REPLACE:
			case INSERT:
			case DELETE:
			case INTO:
			case DESCRIBE:
			case EXPLAIN:
			case FORMAT:
			case LOGICAL:
			case CODEGEN:
			case COST:
			case CAST:
			case SHOW:
			case TABLES:
			case COLUMNS:
			case COLUMN:
			case USE:
			case PARTITIONS:
			case FUNCTIONS:
			case DROP:
			case TO:
			case TABLESAMPLE:
			case STRATIFY:
			case ALTER:
			case RENAME:
			case ARRAY:
			case MAP:
			case STRUCT:
			case COMMENT:
			case SET:
			case RESET:
			case DATA:
			case START:
			case TRANSACTION:
			case COMMIT:
			case ROLLBACK:
			case MACRO:
			case IGNORE:
			case BOTH:
			case LEADING:
			case TRAILING:
			case IF:
			case POSITION:
			case DIV:
			case PERCENTLIT:
			case BUCKET:
			case OUT:
			case OF:
			case SORT:
			case CLUSTER:
			case DISTRIBUTE:
			case OVERWRITE:
			case TRANSFORM:
			case REDUCE:
			case SERDE:
			case SERDEPROPERTIES:
			case RECORDREADER:
			case RECORDWRITER:
			case DELIMITED:
			case FIELDS:
			case TERMINATED:
			case COLLECTION:
			case ITEMS:
			case KEYS:
			case ESCAPED:
			case LINES:
			case SEPARATED:
			case FUNCTION:
			case EXTENDED:
			case REFRESH:
			case CLEAR:
			case CACHE:
			case UNCACHE:
			case LAZY:
			case FORMATTED:
			case GLOBAL:
			case TEMPORARY:
			case OPTIONS:
			case UNSET:
			case TBLPROPERTIES:
			case DBPROPERTIES:
			case BUCKETS:
			case SKEWED:
			case STORED:
			case DIRECTORIES:
			case LOCATION:
			case EXCHANGE:
			case ARCHIVE:
			case UNARCHIVE:
			case FILEFORMAT:
			case TOUCH:
			case COMPACT:
			case CONCATENATE:
			case CHANGE:
			case CASCADE:
			case RESTRICT:
			case CLUSTERED:
			case SORTED:
			case PURGE:
			case INPUTFORMAT:
			case OUTPUTFORMAT:
			case DATABASE:
			case DATABASES:
			case DFS:
			case TRUNCATE:
			case ANALYZE:
			case COMPUTE:
			case LIST:
			case STATISTICS:
			case PARTITIONED:
			case EXTERNAL:
			case DEFINED:
			case REVOKE:
			case GRANT:
			case LOCK:
			case UNLOCK:
			case MSCK:
			case REPAIR:
			case RECOVER:
			case EXPORT:
			case IMPORT:
			case LOAD:
			case ROLE:
			case ROLES:
			case COMPACTIONS:
			case PRINCIPALS:
			case TRANSACTIONS:
			case INDEX:
			case INDEXES:
			case LOCKS:
			case OPTION:
			case LOCAL:
			case INPATH:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1777);
				nonReserved();
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

	public static class QuotedIdentifierContext extends ParserRuleContext {
		public TerminalNode BACKQUOTED_IDENTIFIER() { return getToken(ArcticSparkSqlParser.BACKQUOTED_IDENTIFIER, 0); }
		public QuotedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quotedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterQuotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitQuotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitQuotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuotedIdentifierContext quotedIdentifier() throws RecognitionException {
		QuotedIdentifierContext _localctx = new QuotedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_quotedIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1780);
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
		public TerminalNode DECIMAL_VALUE() { return getToken(ArcticSparkSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public DecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigIntLiteralContext extends NumberContext {
		public TerminalNode BIGINT_LITERAL() { return getToken(ArcticSparkSqlParser.BIGINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public BigIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterBigIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitBigIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitBigIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TinyIntLiteralContext extends NumberContext {
		public TerminalNode TINYINT_LITERAL() { return getToken(ArcticSparkSqlParser.TINYINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public TinyIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterTinyIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitTinyIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitTinyIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BigDecimalLiteralContext extends NumberContext {
		public TerminalNode BIGDECIMAL_LITERAL() { return getToken(ArcticSparkSqlParser.BIGDECIMAL_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public BigDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterBigDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitBigDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitBigDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DoubleLiteralContext extends NumberContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(ArcticSparkSqlParser.DOUBLE_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public DoubleLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitDoubleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitDoubleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntegerLiteralContext extends NumberContext {
		public TerminalNode INTEGER_VALUE() { return getToken(ArcticSparkSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public IntegerLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitIntegerLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SmallIntLiteralContext extends NumberContext {
		public TerminalNode SMALLINT_LITERAL() { return getToken(ArcticSparkSqlParser.SMALLINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(ArcticSparkSqlParser.MINUS, 0); }
		public SmallIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterSmallIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitSmallIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitSmallIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_number);
		int _la;
		try {
			setState(1810);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,228,_ctx) ) {
			case 1:
				_localctx = new DecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1783);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1782);
					match(MINUS);
					}
				}

				setState(1785);
				match(DECIMAL_VALUE);
				}
				break;
			case 2:
				_localctx = new IntegerLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1787);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1786);
					match(MINUS);
					}
				}

				setState(1789);
				match(INTEGER_VALUE);
				}
				break;
			case 3:
				_localctx = new BigIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1791);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1790);
					match(MINUS);
					}
				}

				setState(1793);
				match(BIGINT_LITERAL);
				}
				break;
			case 4:
				_localctx = new SmallIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1795);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1794);
					match(MINUS);
					}
				}

				setState(1797);
				match(SMALLINT_LITERAL);
				}
				break;
			case 5:
				_localctx = new TinyIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1799);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1798);
					match(MINUS);
					}
				}

				setState(1801);
				match(TINYINT_LITERAL);
				}
				break;
			case 6:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1803);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1802);
					match(MINUS);
					}
				}

				setState(1805);
				match(DOUBLE_LITERAL);
				}
				break;
			case 7:
				_localctx = new BigDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(1807);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(1806);
					match(MINUS);
					}
				}

				setState(1809);
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

	public static class NonReservedContext extends ParserRuleContext {
		public TerminalNode SHOW() { return getToken(ArcticSparkSqlParser.SHOW, 0); }
		public TerminalNode TABLES() { return getToken(ArcticSparkSqlParser.TABLES, 0); }
		public TerminalNode COLUMNS() { return getToken(ArcticSparkSqlParser.COLUMNS, 0); }
		public TerminalNode COLUMN() { return getToken(ArcticSparkSqlParser.COLUMN, 0); }
		public TerminalNode PARTITIONS() { return getToken(ArcticSparkSqlParser.PARTITIONS, 0); }
		public TerminalNode FUNCTIONS() { return getToken(ArcticSparkSqlParser.FUNCTIONS, 0); }
		public TerminalNode DATABASES() { return getToken(ArcticSparkSqlParser.DATABASES, 0); }
		public TerminalNode ADD() { return getToken(ArcticSparkSqlParser.ADD, 0); }
		public TerminalNode OVER() { return getToken(ArcticSparkSqlParser.OVER, 0); }
		public TerminalNode PARTITION() { return getToken(ArcticSparkSqlParser.PARTITION, 0); }
		public TerminalNode RANGE() { return getToken(ArcticSparkSqlParser.RANGE, 0); }
		public TerminalNode ROWS() { return getToken(ArcticSparkSqlParser.ROWS, 0); }
		public TerminalNode PRECEDING() { return getToken(ArcticSparkSqlParser.PRECEDING, 0); }
		public TerminalNode FOLLOWING() { return getToken(ArcticSparkSqlParser.FOLLOWING, 0); }
		public TerminalNode CURRENT() { return getToken(ArcticSparkSqlParser.CURRENT, 0); }
		public TerminalNode ROW() { return getToken(ArcticSparkSqlParser.ROW, 0); }
		public TerminalNode LAST() { return getToken(ArcticSparkSqlParser.LAST, 0); }
		public TerminalNode FIRST() { return getToken(ArcticSparkSqlParser.FIRST, 0); }
		public TerminalNode AFTER() { return getToken(ArcticSparkSqlParser.AFTER, 0); }
		public TerminalNode MAP() { return getToken(ArcticSparkSqlParser.MAP, 0); }
		public TerminalNode ARRAY() { return getToken(ArcticSparkSqlParser.ARRAY, 0); }
		public TerminalNode STRUCT() { return getToken(ArcticSparkSqlParser.STRUCT, 0); }
		public TerminalNode LATERAL() { return getToken(ArcticSparkSqlParser.LATERAL, 0); }
		public TerminalNode WINDOW() { return getToken(ArcticSparkSqlParser.WINDOW, 0); }
		public TerminalNode REDUCE() { return getToken(ArcticSparkSqlParser.REDUCE, 0); }
		public TerminalNode TRANSFORM() { return getToken(ArcticSparkSqlParser.TRANSFORM, 0); }
		public TerminalNode SERDE() { return getToken(ArcticSparkSqlParser.SERDE, 0); }
		public TerminalNode SERDEPROPERTIES() { return getToken(ArcticSparkSqlParser.SERDEPROPERTIES, 0); }
		public TerminalNode RECORDREADER() { return getToken(ArcticSparkSqlParser.RECORDREADER, 0); }
		public TerminalNode DELIMITED() { return getToken(ArcticSparkSqlParser.DELIMITED, 0); }
		public TerminalNode FIELDS() { return getToken(ArcticSparkSqlParser.FIELDS, 0); }
		public TerminalNode TERMINATED() { return getToken(ArcticSparkSqlParser.TERMINATED, 0); }
		public TerminalNode COLLECTION() { return getToken(ArcticSparkSqlParser.COLLECTION, 0); }
		public TerminalNode ITEMS() { return getToken(ArcticSparkSqlParser.ITEMS, 0); }
		public TerminalNode KEYS() { return getToken(ArcticSparkSqlParser.KEYS, 0); }
		public TerminalNode ESCAPED() { return getToken(ArcticSparkSqlParser.ESCAPED, 0); }
		public TerminalNode LINES() { return getToken(ArcticSparkSqlParser.LINES, 0); }
		public TerminalNode SEPARATED() { return getToken(ArcticSparkSqlParser.SEPARATED, 0); }
		public TerminalNode EXTENDED() { return getToken(ArcticSparkSqlParser.EXTENDED, 0); }
		public TerminalNode REFRESH() { return getToken(ArcticSparkSqlParser.REFRESH, 0); }
		public TerminalNode CLEAR() { return getToken(ArcticSparkSqlParser.CLEAR, 0); }
		public TerminalNode CACHE() { return getToken(ArcticSparkSqlParser.CACHE, 0); }
		public TerminalNode UNCACHE() { return getToken(ArcticSparkSqlParser.UNCACHE, 0); }
		public TerminalNode LAZY() { return getToken(ArcticSparkSqlParser.LAZY, 0); }
		public TerminalNode GLOBAL() { return getToken(ArcticSparkSqlParser.GLOBAL, 0); }
		public TerminalNode TEMPORARY() { return getToken(ArcticSparkSqlParser.TEMPORARY, 0); }
		public TerminalNode OPTIONS() { return getToken(ArcticSparkSqlParser.OPTIONS, 0); }
		public TerminalNode GROUPING() { return getToken(ArcticSparkSqlParser.GROUPING, 0); }
		public TerminalNode CUBE() { return getToken(ArcticSparkSqlParser.CUBE, 0); }
		public TerminalNode ROLLUP() { return getToken(ArcticSparkSqlParser.ROLLUP, 0); }
		public TerminalNode EXPLAIN() { return getToken(ArcticSparkSqlParser.EXPLAIN, 0); }
		public TerminalNode FORMAT() { return getToken(ArcticSparkSqlParser.FORMAT, 0); }
		public TerminalNode LOGICAL() { return getToken(ArcticSparkSqlParser.LOGICAL, 0); }
		public TerminalNode FORMATTED() { return getToken(ArcticSparkSqlParser.FORMATTED, 0); }
		public TerminalNode CODEGEN() { return getToken(ArcticSparkSqlParser.CODEGEN, 0); }
		public TerminalNode COST() { return getToken(ArcticSparkSqlParser.COST, 0); }
		public TerminalNode TABLESAMPLE() { return getToken(ArcticSparkSqlParser.TABLESAMPLE, 0); }
		public TerminalNode USE() { return getToken(ArcticSparkSqlParser.USE, 0); }
		public TerminalNode TO() { return getToken(ArcticSparkSqlParser.TO, 0); }
		public TerminalNode BUCKET() { return getToken(ArcticSparkSqlParser.BUCKET, 0); }
		public TerminalNode PERCENTLIT() { return getToken(ArcticSparkSqlParser.PERCENTLIT, 0); }
		public TerminalNode OUT() { return getToken(ArcticSparkSqlParser.OUT, 0); }
		public TerminalNode OF() { return getToken(ArcticSparkSqlParser.OF, 0); }
		public TerminalNode SET() { return getToken(ArcticSparkSqlParser.SET, 0); }
		public TerminalNode RESET() { return getToken(ArcticSparkSqlParser.RESET, 0); }
		public TerminalNode VIEW() { return getToken(ArcticSparkSqlParser.VIEW, 0); }
		public TerminalNode REPLACE() { return getToken(ArcticSparkSqlParser.REPLACE, 0); }
		public TerminalNode IF() { return getToken(ArcticSparkSqlParser.IF, 0); }
		public TerminalNode POSITION() { return getToken(ArcticSparkSqlParser.POSITION, 0); }
		public TerminalNode NO() { return getToken(ArcticSparkSqlParser.NO, 0); }
		public TerminalNode DATA() { return getToken(ArcticSparkSqlParser.DATA, 0); }
		public TerminalNode START() { return getToken(ArcticSparkSqlParser.START, 0); }
		public TerminalNode TRANSACTION() { return getToken(ArcticSparkSqlParser.TRANSACTION, 0); }
		public TerminalNode COMMIT() { return getToken(ArcticSparkSqlParser.COMMIT, 0); }
		public TerminalNode ROLLBACK() { return getToken(ArcticSparkSqlParser.ROLLBACK, 0); }
		public TerminalNode IGNORE() { return getToken(ArcticSparkSqlParser.IGNORE, 0); }
		public TerminalNode SORT() { return getToken(ArcticSparkSqlParser.SORT, 0); }
		public TerminalNode CLUSTER() { return getToken(ArcticSparkSqlParser.CLUSTER, 0); }
		public TerminalNode DISTRIBUTE() { return getToken(ArcticSparkSqlParser.DISTRIBUTE, 0); }
		public TerminalNode UNSET() { return getToken(ArcticSparkSqlParser.UNSET, 0); }
		public TerminalNode TBLPROPERTIES() { return getToken(ArcticSparkSqlParser.TBLPROPERTIES, 0); }
		public TerminalNode SKEWED() { return getToken(ArcticSparkSqlParser.SKEWED, 0); }
		public TerminalNode STORED() { return getToken(ArcticSparkSqlParser.STORED, 0); }
		public TerminalNode DIRECTORIES() { return getToken(ArcticSparkSqlParser.DIRECTORIES, 0); }
		public TerminalNode LOCATION() { return getToken(ArcticSparkSqlParser.LOCATION, 0); }
		public TerminalNode EXCHANGE() { return getToken(ArcticSparkSqlParser.EXCHANGE, 0); }
		public TerminalNode ARCHIVE() { return getToken(ArcticSparkSqlParser.ARCHIVE, 0); }
		public TerminalNode UNARCHIVE() { return getToken(ArcticSparkSqlParser.UNARCHIVE, 0); }
		public TerminalNode FILEFORMAT() { return getToken(ArcticSparkSqlParser.FILEFORMAT, 0); }
		public TerminalNode TOUCH() { return getToken(ArcticSparkSqlParser.TOUCH, 0); }
		public TerminalNode COMPACT() { return getToken(ArcticSparkSqlParser.COMPACT, 0); }
		public TerminalNode CONCATENATE() { return getToken(ArcticSparkSqlParser.CONCATENATE, 0); }
		public TerminalNode CHANGE() { return getToken(ArcticSparkSqlParser.CHANGE, 0); }
		public TerminalNode CASCADE() { return getToken(ArcticSparkSqlParser.CASCADE, 0); }
		public TerminalNode RESTRICT() { return getToken(ArcticSparkSqlParser.RESTRICT, 0); }
		public TerminalNode BUCKETS() { return getToken(ArcticSparkSqlParser.BUCKETS, 0); }
		public TerminalNode CLUSTERED() { return getToken(ArcticSparkSqlParser.CLUSTERED, 0); }
		public TerminalNode SORTED() { return getToken(ArcticSparkSqlParser.SORTED, 0); }
		public TerminalNode PURGE() { return getToken(ArcticSparkSqlParser.PURGE, 0); }
		public TerminalNode INPUTFORMAT() { return getToken(ArcticSparkSqlParser.INPUTFORMAT, 0); }
		public TerminalNode OUTPUTFORMAT() { return getToken(ArcticSparkSqlParser.OUTPUTFORMAT, 0); }
		public TerminalNode DBPROPERTIES() { return getToken(ArcticSparkSqlParser.DBPROPERTIES, 0); }
		public TerminalNode DFS() { return getToken(ArcticSparkSqlParser.DFS, 0); }
		public TerminalNode TRUNCATE() { return getToken(ArcticSparkSqlParser.TRUNCATE, 0); }
		public TerminalNode COMPUTE() { return getToken(ArcticSparkSqlParser.COMPUTE, 0); }
		public TerminalNode LIST() { return getToken(ArcticSparkSqlParser.LIST, 0); }
		public TerminalNode STATISTICS() { return getToken(ArcticSparkSqlParser.STATISTICS, 0); }
		public TerminalNode ANALYZE() { return getToken(ArcticSparkSqlParser.ANALYZE, 0); }
		public TerminalNode PARTITIONED() { return getToken(ArcticSparkSqlParser.PARTITIONED, 0); }
		public TerminalNode EXTERNAL() { return getToken(ArcticSparkSqlParser.EXTERNAL, 0); }
		public TerminalNode DEFINED() { return getToken(ArcticSparkSqlParser.DEFINED, 0); }
		public TerminalNode RECORDWRITER() { return getToken(ArcticSparkSqlParser.RECORDWRITER, 0); }
		public TerminalNode REVOKE() { return getToken(ArcticSparkSqlParser.REVOKE, 0); }
		public TerminalNode GRANT() { return getToken(ArcticSparkSqlParser.GRANT, 0); }
		public TerminalNode LOCK() { return getToken(ArcticSparkSqlParser.LOCK, 0); }
		public TerminalNode UNLOCK() { return getToken(ArcticSparkSqlParser.UNLOCK, 0); }
		public TerminalNode MSCK() { return getToken(ArcticSparkSqlParser.MSCK, 0); }
		public TerminalNode REPAIR() { return getToken(ArcticSparkSqlParser.REPAIR, 0); }
		public TerminalNode RECOVER() { return getToken(ArcticSparkSqlParser.RECOVER, 0); }
		public TerminalNode EXPORT() { return getToken(ArcticSparkSqlParser.EXPORT, 0); }
		public TerminalNode IMPORT() { return getToken(ArcticSparkSqlParser.IMPORT, 0); }
		public TerminalNode LOAD() { return getToken(ArcticSparkSqlParser.LOAD, 0); }
		public TerminalNode VALUES() { return getToken(ArcticSparkSqlParser.VALUES, 0); }
		public TerminalNode COMMENT() { return getToken(ArcticSparkSqlParser.COMMENT, 0); }
		public TerminalNode ROLE() { return getToken(ArcticSparkSqlParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(ArcticSparkSqlParser.ROLES, 0); }
		public TerminalNode COMPACTIONS() { return getToken(ArcticSparkSqlParser.COMPACTIONS, 0); }
		public TerminalNode PRINCIPALS() { return getToken(ArcticSparkSqlParser.PRINCIPALS, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(ArcticSparkSqlParser.TRANSACTIONS, 0); }
		public TerminalNode INDEX() { return getToken(ArcticSparkSqlParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(ArcticSparkSqlParser.INDEXES, 0); }
		public TerminalNode LOCKS() { return getToken(ArcticSparkSqlParser.LOCKS, 0); }
		public TerminalNode OPTION() { return getToken(ArcticSparkSqlParser.OPTION, 0); }
		public TerminalNode LOCAL() { return getToken(ArcticSparkSqlParser.LOCAL, 0); }
		public TerminalNode INPATH() { return getToken(ArcticSparkSqlParser.INPATH, 0); }
		public TerminalNode ASC() { return getToken(ArcticSparkSqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(ArcticSparkSqlParser.DESC, 0); }
		public TerminalNode LIMIT() { return getToken(ArcticSparkSqlParser.LIMIT, 0); }
		public TerminalNode RENAME() { return getToken(ArcticSparkSqlParser.RENAME, 0); }
		public TerminalNode SETS() { return getToken(ArcticSparkSqlParser.SETS, 0); }
		public TerminalNode AT() { return getToken(ArcticSparkSqlParser.AT, 0); }
		public TerminalNode NULLS() { return getToken(ArcticSparkSqlParser.NULLS, 0); }
		public TerminalNode OVERWRITE() { return getToken(ArcticSparkSqlParser.OVERWRITE, 0); }
		public TerminalNode ALL() { return getToken(ArcticSparkSqlParser.ALL, 0); }
		public TerminalNode ALTER() { return getToken(ArcticSparkSqlParser.ALTER, 0); }
		public TerminalNode AS() { return getToken(ArcticSparkSqlParser.AS, 0); }
		public TerminalNode BETWEEN() { return getToken(ArcticSparkSqlParser.BETWEEN, 0); }
		public TerminalNode BY() { return getToken(ArcticSparkSqlParser.BY, 0); }
		public TerminalNode CREATE() { return getToken(ArcticSparkSqlParser.CREATE, 0); }
		public TerminalNode DELETE() { return getToken(ArcticSparkSqlParser.DELETE, 0); }
		public TerminalNode DESCRIBE() { return getToken(ArcticSparkSqlParser.DESCRIBE, 0); }
		public TerminalNode DROP() { return getToken(ArcticSparkSqlParser.DROP, 0); }
		public TerminalNode EXISTS() { return getToken(ArcticSparkSqlParser.EXISTS, 0); }
		public TerminalNode FALSE() { return getToken(ArcticSparkSqlParser.FALSE, 0); }
		public TerminalNode FOR() { return getToken(ArcticSparkSqlParser.FOR, 0); }
		public TerminalNode GROUP() { return getToken(ArcticSparkSqlParser.GROUP, 0); }
		public TerminalNode IN() { return getToken(ArcticSparkSqlParser.IN, 0); }
		public TerminalNode INSERT() { return getToken(ArcticSparkSqlParser.INSERT, 0); }
		public TerminalNode INTO() { return getToken(ArcticSparkSqlParser.INTO, 0); }
		public TerminalNode IS() { return getToken(ArcticSparkSqlParser.IS, 0); }
		public TerminalNode LIKE() { return getToken(ArcticSparkSqlParser.LIKE, 0); }
		public TerminalNode NULL() { return getToken(ArcticSparkSqlParser.NULL, 0); }
		public TerminalNode ORDER() { return getToken(ArcticSparkSqlParser.ORDER, 0); }
		public TerminalNode OUTER() { return getToken(ArcticSparkSqlParser.OUTER, 0); }
		public TerminalNode TABLE() { return getToken(ArcticSparkSqlParser.TABLE, 0); }
		public TerminalNode TRUE() { return getToken(ArcticSparkSqlParser.TRUE, 0); }
		public TerminalNode WITH() { return getToken(ArcticSparkSqlParser.WITH, 0); }
		public TerminalNode RLIKE() { return getToken(ArcticSparkSqlParser.RLIKE, 0); }
		public TerminalNode AND() { return getToken(ArcticSparkSqlParser.AND, 0); }
		public TerminalNode CASE() { return getToken(ArcticSparkSqlParser.CASE, 0); }
		public TerminalNode CAST() { return getToken(ArcticSparkSqlParser.CAST, 0); }
		public TerminalNode DISTINCT() { return getToken(ArcticSparkSqlParser.DISTINCT, 0); }
		public TerminalNode DIV() { return getToken(ArcticSparkSqlParser.DIV, 0); }
		public TerminalNode ELSE() { return getToken(ArcticSparkSqlParser.ELSE, 0); }
		public TerminalNode END() { return getToken(ArcticSparkSqlParser.END, 0); }
		public TerminalNode FUNCTION() { return getToken(ArcticSparkSqlParser.FUNCTION, 0); }
		public TerminalNode INTERVAL() { return getToken(ArcticSparkSqlParser.INTERVAL, 0); }
		public TerminalNode MACRO() { return getToken(ArcticSparkSqlParser.MACRO, 0); }
		public TerminalNode OR() { return getToken(ArcticSparkSqlParser.OR, 0); }
		public TerminalNode STRATIFY() { return getToken(ArcticSparkSqlParser.STRATIFY, 0); }
		public TerminalNode THEN() { return getToken(ArcticSparkSqlParser.THEN, 0); }
		public TerminalNode UNBOUNDED() { return getToken(ArcticSparkSqlParser.UNBOUNDED, 0); }
		public TerminalNode WHEN() { return getToken(ArcticSparkSqlParser.WHEN, 0); }
		public TerminalNode DATABASE() { return getToken(ArcticSparkSqlParser.DATABASE, 0); }
		public TerminalNode SELECT() { return getToken(ArcticSparkSqlParser.SELECT, 0); }
		public TerminalNode FROM() { return getToken(ArcticSparkSqlParser.FROM, 0); }
		public TerminalNode WHERE() { return getToken(ArcticSparkSqlParser.WHERE, 0); }
		public TerminalNode HAVING() { return getToken(ArcticSparkSqlParser.HAVING, 0); }
		public TerminalNode NOT() { return getToken(ArcticSparkSqlParser.NOT, 0); }
		public TerminalNode DIRECTORY() { return getToken(ArcticSparkSqlParser.DIRECTORY, 0); }
		public TerminalNode BOTH() { return getToken(ArcticSparkSqlParser.BOTH, 0); }
		public TerminalNode LEADING() { return getToken(ArcticSparkSqlParser.LEADING, 0); }
		public TerminalNode TRAILING() { return getToken(ArcticSparkSqlParser.TRAILING, 0); }
		public NonReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).enterNonReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ArcticSparkSqlListener ) ((ArcticSparkSqlListener)listener).exitNonReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ArcticSparkSqlVisitor ) return ((ArcticSparkSqlVisitor<? extends T>)visitor).visitNonReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonReservedContext nonReserved() throws RecognitionException {
		NonReservedContext _localctx = new NonReservedContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_nonReserved);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1812);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SELECT) | (1L << FROM) | (1L << ADD) | (1L << AS) | (1L << ALL) | (1L << DISTINCT) | (1L << WHERE) | (1L << GROUP) | (1L << BY) | (1L << GROUPING) | (1L << SETS) | (1L << CUBE) | (1L << ROLLUP) | (1L << ORDER) | (1L << HAVING) | (1L << LIMIT) | (1L << AT) | (1L << OR) | (1L << AND) | (1L << IN) | (1L << NOT) | (1L << NO) | (1L << EXISTS) | (1L << BETWEEN) | (1L << LIKE) | (1L << RLIKE) | (1L << IS) | (1L << NULL) | (1L << TRUE) | (1L << FALSE) | (1L << NULLS) | (1L << ASC) | (1L << DESC) | (1L << FOR) | (1L << INTERVAL) | (1L << CASE) | (1L << WHEN) | (1L << THEN) | (1L << ELSE) | (1L << END) | (1L << OUTER) | (1L << LATERAL) | (1L << WINDOW))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (OVER - 64)) | (1L << (PARTITION - 64)) | (1L << (RANGE - 64)) | (1L << (ROWS - 64)) | (1L << (UNBOUNDED - 64)) | (1L << (PRECEDING - 64)) | (1L << (FOLLOWING - 64)) | (1L << (CURRENT - 64)) | (1L << (FIRST - 64)) | (1L << (AFTER - 64)) | (1L << (LAST - 64)) | (1L << (ROW - 64)) | (1L << (WITH - 64)) | (1L << (VALUES - 64)) | (1L << (CREATE - 64)) | (1L << (TABLE - 64)) | (1L << (DIRECTORY - 64)) | (1L << (VIEW - 64)) | (1L << (REPLACE - 64)) | (1L << (INSERT - 64)) | (1L << (DELETE - 64)) | (1L << (INTO - 64)) | (1L << (DESCRIBE - 64)) | (1L << (EXPLAIN - 64)) | (1L << (FORMAT - 64)) | (1L << (LOGICAL - 64)) | (1L << (CODEGEN - 64)) | (1L << (COST - 64)) | (1L << (CAST - 64)) | (1L << (SHOW - 64)) | (1L << (TABLES - 64)) | (1L << (COLUMNS - 64)) | (1L << (COLUMN - 64)) | (1L << (USE - 64)) | (1L << (PARTITIONS - 64)) | (1L << (FUNCTIONS - 64)) | (1L << (DROP - 64)) | (1L << (TO - 64)) | (1L << (TABLESAMPLE - 64)) | (1L << (STRATIFY - 64)) | (1L << (ALTER - 64)) | (1L << (RENAME - 64)) | (1L << (ARRAY - 64)) | (1L << (MAP - 64)) | (1L << (STRUCT - 64)) | (1L << (COMMENT - 64)) | (1L << (SET - 64)) | (1L << (RESET - 64)) | (1L << (DATA - 64)) | (1L << (START - 64)) | (1L << (TRANSACTION - 64)) | (1L << (COMMIT - 64)) | (1L << (ROLLBACK - 64)) | (1L << (MACRO - 64)) | (1L << (IGNORE - 64)) | (1L << (BOTH - 64)) | (1L << (LEADING - 64)) | (1L << (TRAILING - 64)) | (1L << (IF - 64)) | (1L << (POSITION - 64)))) != 0) || ((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (DIV - 141)) | (1L << (PERCENTLIT - 141)) | (1L << (BUCKET - 141)) | (1L << (OUT - 141)) | (1L << (OF - 141)) | (1L << (SORT - 141)) | (1L << (CLUSTER - 141)) | (1L << (DISTRIBUTE - 141)) | (1L << (OVERWRITE - 141)) | (1L << (TRANSFORM - 141)) | (1L << (REDUCE - 141)) | (1L << (SERDE - 141)) | (1L << (SERDEPROPERTIES - 141)) | (1L << (RECORDREADER - 141)) | (1L << (RECORDWRITER - 141)) | (1L << (DELIMITED - 141)) | (1L << (FIELDS - 141)) | (1L << (TERMINATED - 141)) | (1L << (COLLECTION - 141)) | (1L << (ITEMS - 141)) | (1L << (KEYS - 141)) | (1L << (ESCAPED - 141)) | (1L << (LINES - 141)) | (1L << (SEPARATED - 141)) | (1L << (FUNCTION - 141)) | (1L << (EXTENDED - 141)) | (1L << (REFRESH - 141)) | (1L << (CLEAR - 141)) | (1L << (CACHE - 141)) | (1L << (UNCACHE - 141)) | (1L << (LAZY - 141)) | (1L << (FORMATTED - 141)) | (1L << (GLOBAL - 141)) | (1L << (TEMPORARY - 141)) | (1L << (OPTIONS - 141)) | (1L << (UNSET - 141)) | (1L << (TBLPROPERTIES - 141)) | (1L << (DBPROPERTIES - 141)) | (1L << (BUCKETS - 141)) | (1L << (SKEWED - 141)) | (1L << (STORED - 141)) | (1L << (DIRECTORIES - 141)) | (1L << (LOCATION - 141)) | (1L << (EXCHANGE - 141)) | (1L << (ARCHIVE - 141)) | (1L << (UNARCHIVE - 141)) | (1L << (FILEFORMAT - 141)) | (1L << (TOUCH - 141)) | (1L << (COMPACT - 141)) | (1L << (CONCATENATE - 141)) | (1L << (CHANGE - 141)) | (1L << (CASCADE - 141)) | (1L << (RESTRICT - 141)) | (1L << (CLUSTERED - 141)) | (1L << (SORTED - 141)) | (1L << (PURGE - 141)) | (1L << (INPUTFORMAT - 141)) | (1L << (OUTPUTFORMAT - 141)))) != 0) || ((((_la - 205)) & ~0x3f) == 0 && ((1L << (_la - 205)) & ((1L << (DATABASE - 205)) | (1L << (DATABASES - 205)) | (1L << (DFS - 205)) | (1L << (TRUNCATE - 205)) | (1L << (ANALYZE - 205)) | (1L << (COMPUTE - 205)) | (1L << (LIST - 205)) | (1L << (STATISTICS - 205)) | (1L << (PARTITIONED - 205)) | (1L << (EXTERNAL - 205)) | (1L << (DEFINED - 205)) | (1L << (REVOKE - 205)) | (1L << (GRANT - 205)) | (1L << (LOCK - 205)) | (1L << (UNLOCK - 205)) | (1L << (MSCK - 205)) | (1L << (REPAIR - 205)) | (1L << (RECOVER - 205)) | (1L << (EXPORT - 205)) | (1L << (IMPORT - 205)) | (1L << (LOAD - 205)) | (1L << (ROLE - 205)) | (1L << (ROLES - 205)) | (1L << (COMPACTIONS - 205)) | (1L << (PRINCIPALS - 205)) | (1L << (TRANSACTIONS - 205)) | (1L << (INDEX - 205)) | (1L << (INDEXES - 205)) | (1L << (LOCKS - 205)) | (1L << (OPTION - 205)) | (1L << (LOCAL - 205)) | (1L << (INPATH - 205)))) != 0)) ) {
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
		case 39:
			return queryTerm_sempred((QueryTermContext)_localctx, predIndex);
		case 72:
			return booleanExpression_sempred((BooleanExpressionContext)_localctx, predIndex);
		case 75:
			return valueExpression_sempred((ValueExpressionContext)_localctx, predIndex);
		case 76:
			return primaryExpression_sempred((PrimaryExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean queryTerm_sempred(QueryTermContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean booleanExpression_sempred(BooleanExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 2);
		case 2:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean valueExpression_sempred(ValueExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return precpred(_ctx, 6);
		case 4:
			return precpred(_ctx, 5);
		case 5:
			return precpred(_ctx, 4);
		case 6:
			return precpred(_ctx, 3);
		case 7:
			return precpred(_ctx, 2);
		case 8:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean primaryExpression_sempred(PrimaryExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 9:
			return precpred(_ctx, 4);
		case 10:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\u00fe\u0719\4\2\t"+
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
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\3\2\3\2\3\2\3\3\3"+
		"\3\5\3\u00d6\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3\u00e4"+
		"\n\3\f\3\16\3\u00e7\13\3\3\3\5\3\u00ea\n\3\3\3\5\3\u00ed\n\3\3\3\7\3\u00f0"+
		"\n\3\f\3\16\3\u00f3\13\3\5\3\u00f5\n\3\3\4\3\4\3\4\3\4\5\4\u00fb\n\4\3"+
		"\4\3\4\3\4\5\4\u0100\n\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\7\6\u010a\n\6"+
		"\f\6\16\6\u010d\13\6\3\6\3\6\3\7\3\7\5\7\u0113\n\7\3\b\3\b\3\b\3\t\3\t"+
		"\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\5"+
		"\r\u012a\n\r\3\r\3\r\5\r\u012e\n\r\3\r\3\r\3\r\3\r\3\r\5\r\u0135\n\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u01a9\n\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\5\r\u01b1\n\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u01b9\n\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\5\r\u01c2\n\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r"+
		"\u01ce\n\r\3\16\3\16\5\16\u01d2\n\16\3\16\5\16\u01d5\n\16\3\16\3\16\3"+
		"\16\3\16\5\16\u01db\n\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\5\17"+
		"\u01e5\n\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\5\20\u01f1"+
		"\n\20\3\20\3\20\3\20\5\20\u01f6\n\20\3\21\3\21\3\21\3\22\5\22\u01fc\n"+
		"\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u0208\n\23"+
		"\5\23\u020a\n\23\3\23\3\23\3\23\5\23\u020f\n\23\3\23\3\23\5\23\u0213\n"+
		"\23\3\23\3\23\3\23\5\23\u0218\n\23\3\23\3\23\3\23\5\23\u021d\n\23\3\23"+
		"\5\23\u0220\n\23\3\23\3\23\3\23\5\23\u0225\n\23\3\23\3\23\5\23\u0229\n"+
		"\23\3\23\3\23\3\23\5\23\u022e\n\23\5\23\u0230\n\23\3\24\3\24\5\24\u0234"+
		"\n\24\3\25\3\25\3\25\3\25\3\25\7\25\u023b\n\25\f\25\16\25\u023e\13\25"+
		"\3\25\3\25\3\26\3\26\3\26\5\26\u0245\n\26\3\27\3\27\3\27\3\27\3\27\5\27"+
		"\u024c\n\27\3\30\3\30\3\30\7\30\u0251\n\30\f\30\16\30\u0254\13\30\3\31"+
		"\3\31\3\31\3\31\7\31\u025a\n\31\f\31\16\31\u025d\13\31\3\32\3\32\5\32"+
		"\u0261\n\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\34\3\34\7\34"+
		"\u026e\n\34\f\34\16\34\u0271\13\34\3\34\3\34\3\35\3\35\5\35\u0277\n\35"+
		"\3\35\5\35\u027a\n\35\3\36\3\36\3\36\7\36\u027f\n\36\f\36\16\36\u0282"+
		"\13\36\3\36\5\36\u0285\n\36\3\37\3\37\3\37\3\37\5\37\u028b\n\37\3 \3 "+
		"\3 \3 \7 \u0291\n \f \16 \u0294\13 \3 \3 \3!\3!\3!\3!\7!\u029c\n!\f!\16"+
		"!\u029f\13!\3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\5\"\u02a9\n\"\3#\3#\3#\3#\3"+
		"#\5#\u02b0\n#\3$\3$\3$\3$\5$\u02b6\n$\3%\3%\3%\3&\5&\u02bc\n&\3&\3&\3"+
		"&\3&\3&\6&\u02c3\n&\r&\16&\u02c4\5&\u02c7\n&\3\'\3\'\3\'\3\'\3\'\7\'\u02ce"+
		"\n\'\f\'\16\'\u02d1\13\'\5\'\u02d3\n\'\3\'\3\'\3\'\3\'\3\'\7\'\u02da\n"+
		"\'\f\'\16\'\u02dd\13\'\5\'\u02df\n\'\3\'\3\'\3\'\3\'\3\'\7\'\u02e6\n\'"+
		"\f\'\16\'\u02e9\13\'\5\'\u02eb\n\'\3\'\3\'\3\'\3\'\3\'\7\'\u02f2\n\'\f"+
		"\'\16\'\u02f5\13\'\5\'\u02f7\n\'\3\'\5\'\u02fa\n\'\3\'\3\'\3\'\5\'\u02ff"+
		"\n\'\5\'\u0301\n\'\3(\5(\u0304\n(\3(\3(\3(\3)\3)\3)\3)\3)\3)\5)\u030f"+
		"\n)\3)\7)\u0312\n)\f)\16)\u0315\13)\3*\3*\3*\3*\3*\3*\3*\3*\5*\u031f\n"+
		"*\3+\3+\5+\u0323\n+\3+\3+\5+\u0327\n+\3,\3,\3,\3,\3,\3,\3,\3,\3,\3,\5"+
		",\u0333\n,\3,\5,\u0336\n,\3,\3,\5,\u033a\n,\3,\3,\3,\3,\3,\3,\3,\3,\5"+
		",\u0344\n,\3,\3,\5,\u0348\n,\5,\u034a\n,\3,\5,\u034d\n,\3,\3,\5,\u0351"+
		"\n,\3,\5,\u0354\n,\3,\3,\5,\u0358\n,\3,\3,\7,\u035c\n,\f,\16,\u035f\13"+
		",\3,\5,\u0362\n,\3,\3,\5,\u0366\n,\3,\3,\3,\5,\u036b\n,\3,\5,\u036e\n"+
		",\5,\u0370\n,\3,\7,\u0373\n,\f,\16,\u0376\13,\3,\3,\5,\u037a\n,\3,\5,"+
		"\u037d\n,\3,\3,\5,\u0381\n,\3,\5,\u0384\n,\5,\u0386\n,\3-\3-\3-\5-\u038b"+
		"\n-\3-\7-\u038e\n-\f-\16-\u0391\13-\3-\3-\3.\3.\3.\3.\3.\3.\7.\u039b\n"+
		".\f.\16.\u039e\13.\3.\3.\5.\u03a2\n.\3/\3/\3/\3/\7/\u03a8\n/\f/\16/\u03ab"+
		"\13/\3/\7/\u03ae\n/\f/\16/\u03b1\13/\3\60\3\60\3\60\3\60\3\60\7\60\u03b8"+
		"\n\60\f\60\16\60\u03bb\13\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3"+
		"\60\3\60\7\60\u03c7\n\60\f\60\16\60\u03ca\13\60\3\60\3\60\5\60\u03ce\n"+
		"\60\3\61\3\61\3\61\3\61\7\61\u03d4\n\61\f\61\16\61\u03d7\13\61\5\61\u03d9"+
		"\n\61\3\61\3\61\5\61\u03dd\n\61\3\62\3\62\3\62\5\62\u03e2\n\62\3\62\3"+
		"\62\3\62\3\62\3\62\7\62\u03e9\n\62\f\62\16\62\u03ec\13\62\5\62\u03ee\n"+
		"\62\3\62\3\62\3\62\5\62\u03f3\n\62\3\62\3\62\3\62\7\62\u03f8\n\62\f\62"+
		"\16\62\u03fb\13\62\5\62\u03fd\n\62\3\63\3\63\3\64\3\64\7\64\u0403\n\64"+
		"\f\64\16\64\u0406\13\64\3\65\3\65\3\65\3\65\5\65\u040c\n\65\3\65\3\65"+
		"\3\65\3\65\3\65\5\65\u0413\n\65\3\66\5\66\u0416\n\66\3\66\3\66\3\66\5"+
		"\66\u041b\n\66\3\66\3\66\3\66\3\66\5\66\u0421\n\66\3\66\3\66\5\66\u0425"+
		"\n\66\3\66\5\66\u0428\n\66\3\66\5\66\u042b\n\66\3\67\3\67\3\67\3\67\3"+
		"\67\3\67\3\67\7\67\u0434\n\67\f\67\16\67\u0437\13\67\3\67\3\67\5\67\u043b"+
		"\n\67\38\38\38\58\u0440\n8\38\38\39\59\u0445\n9\39\39\39\39\39\39\39\3"+
		"9\39\39\39\39\39\39\39\39\59\u0457\n9\59\u0459\n9\39\59\u045c\n9\3:\3"+
		":\3:\3:\3;\3;\3;\7;\u0465\n;\f;\16;\u0468\13;\3<\3<\3<\3<\7<\u046e\n<"+
		"\f<\16<\u0471\13<\3<\3<\3=\3=\5=\u0477\n=\3>\3>\3>\3>\7>\u047d\n>\f>\16"+
		">\u0480\13>\3>\3>\3?\3?\3?\5?\u0487\n?\3@\3@\5@\u048b\n@\3@\3@\3@\3@\3"+
		"@\3@\5@\u0493\n@\3@\3@\3@\3@\3@\3@\5@\u049b\n@\3@\3@\3@\3@\5@\u04a1\n"+
		"@\3A\3A\3A\3A\7A\u04a7\nA\fA\16A\u04aa\13A\3A\3A\3B\3B\3B\3B\3B\7B\u04b3"+
		"\nB\fB\16B\u04b6\13B\5B\u04b8\nB\3B\3B\3B\3C\5C\u04be\nC\3C\3C\5C\u04c2"+
		"\nC\5C\u04c4\nC\3D\3D\3D\3D\3D\3D\3D\5D\u04cd\nD\3D\3D\3D\3D\3D\3D\3D"+
		"\3D\3D\3D\5D\u04d9\nD\5D\u04db\nD\3D\3D\3D\3D\3D\5D\u04e2\nD\3D\3D\3D"+
		"\3D\3D\5D\u04e9\nD\3D\3D\3D\3D\5D\u04ef\nD\3D\3D\3D\3D\5D\u04f5\nD\5D"+
		"\u04f7\nD\3E\3E\3E\5E\u04fc\nE\3E\3E\3F\3F\3F\5F\u0503\nF\3F\3F\3G\3G"+
		"\5G\u0509\nG\3G\3G\5G\u050d\nG\5G\u050f\nG\3H\3H\3H\7H\u0514\nH\fH\16"+
		"H\u0517\13H\3I\3I\3J\3J\3J\3J\3J\3J\3J\3J\3J\5J\u0524\nJ\3J\3J\3J\3J\3"+
		"J\3J\7J\u052c\nJ\fJ\16J\u052f\13J\3K\3K\5K\u0533\nK\3L\5L\u0536\nL\3L"+
		"\3L\3L\3L\3L\3L\5L\u053e\nL\3L\3L\3L\3L\3L\7L\u0545\nL\fL\16L\u0548\13"+
		"L\3L\3L\3L\5L\u054d\nL\3L\3L\3L\3L\3L\3L\5L\u0555\nL\3L\3L\3L\3L\5L\u055b"+
		"\nL\3L\3L\3L\5L\u0560\nL\3L\3L\3L\5L\u0565\nL\3M\3M\3M\3M\5M\u056b\nM"+
		"\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\7M\u0580\nM"+
		"\fM\16M\u0583\13M\3N\3N\3N\6N\u0588\nN\rN\16N\u0589\3N\3N\5N\u058e\nN"+
		"\3N\3N\3N\3N\3N\6N\u0595\nN\rN\16N\u0596\3N\3N\5N\u059b\nN\3N\3N\3N\3"+
		"N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\7N\u05ab\nN\fN\16N\u05ae\13N\5N\u05b0"+
		"\nN\3N\3N\3N\3N\3N\3N\5N\u05b8\nN\3N\3N\3N\3N\3N\3N\3N\5N\u05c1\nN\3N"+
		"\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\6N\u05d6\nN\rN"+
		"\16N\u05d7\3N\3N\3N\3N\3N\3N\3N\3N\3N\5N\u05e3\nN\3N\3N\3N\7N\u05e8\n"+
		"N\fN\16N\u05eb\13N\5N\u05ed\nN\3N\3N\3N\5N\u05f2\nN\3N\3N\3N\3N\3N\3N"+
		"\3N\3N\3N\3N\3N\3N\3N\5N\u0601\nN\3N\3N\3N\3N\3N\3N\3N\3N\7N\u060b\nN"+
		"\fN\16N\u060e\13N\3O\3O\3O\3O\3O\3O\3O\3O\6O\u0618\nO\rO\16O\u0619\5O"+
		"\u061c\nO\3P\3P\3Q\3Q\3R\3R\3S\3S\3T\3T\7T\u0628\nT\fT\16T\u062b\13T\3"+
		"U\3U\3U\3U\5U\u0631\nU\3V\5V\u0634\nV\3V\3V\5V\u0638\nV\3W\3W\3W\5W\u063d"+
		"\nW\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\5X\u064e\nX\3X\3X\5X"+
		"\u0652\nX\3X\3X\3X\3X\3X\7X\u0659\nX\fX\16X\u065c\13X\3X\5X\u065f\nX\5"+
		"X\u0661\nX\3Y\3Y\3Y\7Y\u0666\nY\fY\16Y\u0669\13Y\3Z\3Z\3Z\3Z\5Z\u066f"+
		"\nZ\3[\3[\3[\7[\u0674\n[\f[\16[\u0677\13[\3\\\3\\\3\\\3\\\3\\\5\\\u067e"+
		"\n\\\3]\3]\3]\3]\3]\3^\3^\3^\3^\7^\u0689\n^\f^\16^\u068c\13^\3_\3_\3_"+
		"\3_\3`\3`\3`\3`\3`\3`\3`\7`\u0699\n`\f`\16`\u069c\13`\3`\3`\3`\3`\3`\7"+
		"`\u06a3\n`\f`\16`\u06a6\13`\5`\u06a8\n`\3`\3`\3`\3`\3`\7`\u06af\n`\f`"+
		"\16`\u06b2\13`\5`\u06b4\n`\5`\u06b6\n`\3`\5`\u06b9\n`\3`\5`\u06bc\n`\3"+
		"a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\5a\u06ce\na\3b\3b\3b\3"+
		"b\3b\3b\3b\5b\u06d7\nb\3c\3c\3c\7c\u06dc\nc\fc\16c\u06df\13c\3d\3d\3d"+
		"\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\5d\u06f0\nd\3e\3e\3e\5e\u06f5\ne"+
		"\3f\3f\3g\5g\u06fa\ng\3g\3g\5g\u06fe\ng\3g\3g\5g\u0702\ng\3g\3g\5g\u0706"+
		"\ng\3g\3g\5g\u070a\ng\3g\3g\5g\u070e\ng\3g\3g\5g\u0712\ng\3g\5g\u0715"+
		"\ng\3h\3h\3h\3\u00f1\6P\u0092\u0098\u009ai\2\4\6\b\n\f\16\20\22\24\26"+
		"\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|"+
		"~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096"+
		"\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae"+
		"\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6"+
		"\u00c8\u00ca\u00cc\u00ce\2\25\3\2gj\3\2-.\4\2JJLL\3\2\22\23\3\2\u00f4"+
		"\u00f5\3\2&\'\4\2\u008a\u008b\u0090\u0090\3\2\u008c\u008f\4\2\u008a\u008b"+
		"\u0093\u0093\3\2}\177\3\2\u0082\u0089\3\2\u008a\u0094\3\2\37\"\3\2*+\3"+
		"\2\u008a\u008b\4\2CC\u009b\u009b\4\2\33\33\u0099\u0099\3\2GH\n\2\16\65"+
		"88@fk\u0081\u008f\u008f\u0095\u009e\u00a0\u00ec\u00ee\u00ef\2\u080f\2"+
		"\u00d0\3\2\2\2\4\u00f4\3\2\2\2\6\u00ff\3\2\2\2\b\u0101\3\2\2\2\n\u0105"+
		"\3\2\2\2\f\u0112\3\2\2\2\16\u0114\3\2\2\2\20\u0117\3\2\2\2\22\u011a\3"+
		"\2\2\2\24\u011d\3\2\2\2\26\u0120\3\2\2\2\30\u01cd\3\2\2\2\32\u01cf\3\2"+
		"\2\2\34\u01de\3\2\2\2\36\u01ea\3\2\2\2 \u01f7\3\2\2\2\"\u01fb\3\2\2\2"+
		"$\u022f\3\2\2\2&\u0231\3\2\2\2(\u0235\3\2\2\2*\u0241\3\2\2\2,\u024b\3"+
		"\2\2\2.\u024d\3\2\2\2\60\u0255\3\2\2\2\62\u025e\3\2\2\2\64\u0266\3\2\2"+
		"\2\66\u0269\3\2\2\28\u0274\3\2\2\2:\u0284\3\2\2\2<\u028a\3\2\2\2>\u028c"+
		"\3\2\2\2@\u0297\3\2\2\2B\u02a8\3\2\2\2D\u02af\3\2\2\2F\u02b1\3\2\2\2H"+
		"\u02b7\3\2\2\2J\u02c6\3\2\2\2L\u02d2\3\2\2\2N\u0303\3\2\2\2P\u0308\3\2"+
		"\2\2R\u031e\3\2\2\2T\u0320\3\2\2\2V\u0385\3\2\2\2X\u0387\3\2\2\2Z\u03a1"+
		"\3\2\2\2\\\u03a3\3\2\2\2^\u03b2\3\2\2\2`\u03dc\3\2\2\2b\u03de\3\2\2\2"+
		"d\u03fe\3\2\2\2f\u0400\3\2\2\2h\u0412\3\2\2\2j\u042a\3\2\2\2l\u043a\3"+
		"\2\2\2n\u043c\3\2\2\2p\u045b\3\2\2\2r\u045d\3\2\2\2t\u0461\3\2\2\2v\u0469"+
		"\3\2\2\2x\u0474\3\2\2\2z\u0478\3\2\2\2|\u0483\3\2\2\2~\u04a0\3\2\2\2\u0080"+
		"\u04a2\3\2\2\2\u0082\u04ad\3\2\2\2\u0084\u04c3\3\2\2\2\u0086\u04f6\3\2"+
		"\2\2\u0088\u04fb\3\2\2\2\u008a\u0502\3\2\2\2\u008c\u0506\3\2\2\2\u008e"+
		"\u0510\3\2\2\2\u0090\u0518\3\2\2\2\u0092\u0523\3\2\2\2\u0094\u0530\3\2"+
		"\2\2\u0096\u0564\3\2\2\2\u0098\u056a\3\2\2\2\u009a\u0600\3\2\2\2\u009c"+
		"\u061b\3\2\2\2\u009e\u061d\3\2\2\2\u00a0\u061f\3\2\2\2\u00a2\u0621\3\2"+
		"\2\2\u00a4\u0623\3\2\2\2\u00a6\u0625\3\2\2\2\u00a8\u062c\3\2\2\2\u00aa"+
		"\u0637\3\2\2\2\u00ac\u063c\3\2\2\2\u00ae\u0660\3\2\2\2\u00b0\u0662\3\2"+
		"\2\2\u00b2\u066a\3\2\2\2\u00b4\u0670\3\2\2\2\u00b6\u0678\3\2\2\2\u00b8"+
		"\u067f\3\2\2\2\u00ba\u0684\3\2\2\2\u00bc\u068d\3\2\2\2\u00be\u06bb\3\2"+
		"\2\2\u00c0\u06cd\3\2\2\2\u00c2\u06d6\3\2\2\2\u00c4\u06d8\3\2\2\2\u00c6"+
		"\u06ef\3\2\2\2\u00c8\u06f4\3\2\2\2\u00ca\u06f6\3\2\2\2\u00cc\u0714\3\2"+
		"\2\2\u00ce\u0716\3\2\2\2\u00d0\u00d1\5\4\3\2\u00d1\u00d2\7\2\2\3\u00d2"+
		"\3\3\2\2\2\u00d3\u00d5\5\32\16\2\u00d4\u00d6\5\6\4\2\u00d5\u00d4\3\2\2"+
		"\2\u00d5\u00d6\3\2\2\2\u00d6\u00d7\3\2\2\2\u00d7\u00e5\5\64\33\2\u00d8"+
		"\u00d9\7\u00b7\2\2\u00d9\u00e4\5\66\34\2\u00da\u00db\7\u00d7\2\2\u00db"+
		"\u00dc\7\26\2\2\u00dc\u00e4\5\n\6\2\u00dd\u00e4\5\34\17\2\u00de\u00e4"+
		"\5 \21\2\u00df\u00e0\7s\2\2\u00e0\u00e4\7\u00f0\2\2\u00e1\u00e2\7\u00b9"+
		"\2\2\u00e2\u00e4\5\66\34\2\u00e3\u00d8\3\2\2\2\u00e3\u00da\3\2\2\2\u00e3"+
		"\u00dd\3\2\2\2\u00e3\u00de\3\2\2\2\u00e3\u00df\3\2\2\2\u00e3\u00e1\3\2"+
		"\2\2\u00e4\u00e7\3\2\2\2\u00e5\u00e3\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6"+
		"\u00ec\3\2\2\2\u00e7\u00e5\3\2\2\2\u00e8\u00ea\7\21\2\2\u00e9\u00e8\3"+
		"\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ed\5\"\22\2\u00ec"+
		"\u00e9\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00f5\3\2\2\2\u00ee\u00f0\13"+
		"\2\2\2\u00ef\u00ee\3\2\2\2\u00f0\u00f3\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f1"+
		"\u00ef\3\2\2\2\u00f2\u00f5\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f4\u00d3\3\2"+
		"\2\2\u00f4\u00f1\3\2\2\2\u00f5\5\3\2\2\2\u00f6\u00f7\7\3\2\2\u00f7\u00fa"+
		"\5\u00b0Y\2\u00f8\u00f9\7\4\2\2\u00f9\u00fb\5\b\5\2\u00fa\u00f8\3\2\2"+
		"\2\u00fa\u00fb\3\2\2\2\u00fb\u00fc\3\2\2\2\u00fc\u00fd\7\5\2\2\u00fd\u0100"+
		"\3\2\2\2\u00fe\u0100\5\b\5\2\u00ff\u00f6\3\2\2\2\u00ff\u00fe\3\2\2\2\u0100"+
		"\7\3\2\2\2\u0101\u0102\7\f\2\2\u0102\u0103\7\r\2\2\u0103\u0104\5r:\2\u0104"+
		"\t\3\2\2\2\u0105\u0106\7\3\2\2\u0106\u010b\5\f\7\2\u0107\u0108\7\4\2\2"+
		"\u0108\u010a\5\f\7\2\u0109\u0107\3\2\2\2\u010a\u010d\3\2\2\2\u010b\u0109"+
		"\3\2\2\2\u010b\u010c\3\2\2\2\u010c\u010e\3\2\2\2\u010d\u010b\3\2\2\2\u010e"+
		"\u010f\7\5\2\2\u010f\13\3\2\2\2\u0110\u0113\5\u00c6d\2\u0111\u0113\5\u00b2"+
		"Z\2\u0112\u0110\3\2\2\2\u0112\u0111\3\2\2\2\u0113\r\3\2\2\2\u0114\u0115"+
		"\5\u008cG\2\u0115\u0116\7\2\2\3\u0116\17\3\2\2\2\u0117\u0118\5\u0088E"+
		"\2\u0118\u0119\7\2\2\3\u0119\21\3\2\2\2\u011a\u011b\5\u008aF\2\u011b\u011c"+
		"\7\2\2\3\u011c\23\3\2\2\2\u011d\u011e\5\u00aeX\2\u011e\u011f\7\2\2\3\u011f"+
		"\25\3\2\2\2\u0120\u0121\5\u00b0Y\2\u0121\u0122\7\2\2\3\u0122\27\3\2\2"+
		"\2\u0123\u0124\7P\2\2\u0124\u01ce\7\u00e4\2\2\u0125\u0126\7f\2\2\u0126"+
		"\u01ce\7\u00e4\2\2\u0127\u0129\7\u00db\2\2\u0128\u012a\7\u00e4\2\2\u0129"+
		"\u0128\3\2\2\2\u0129\u012a\3\2\2\2\u012a\u01ce\3\2\2\2\u012b\u012d\7\u00da"+
		"\2\2\u012c\u012e\7\u00e4\2\2\u012d\u012c\3\2\2\2\u012d\u012e\3\2\2\2\u012e"+
		"\u01ce\3\2\2\2\u012f\u0130\7_\2\2\u0130\u01ce\7\u00db\2\2\u0131\u0132"+
		"\7_\2\2\u0132\u0134\7\u00e4\2\2\u0133\u0135\7\u00db\2\2\u0134\u0133\3"+
		"\2\2\2\u0134\u0135\3\2\2\2\u0135\u01ce\3\2\2\2\u0136\u0137\7_\2\2\u0137"+
		"\u01ce\7\u00e7\2\2\u0138\u0139\7_\2\2\u0139\u01ce\7\u00e5\2\2\u013a\u013b"+
		"\7_\2\2\u013b\u013c\7I\2\2\u013c\u01ce\7\u00e5\2\2\u013d\u013e\7\u00e1"+
		"\2\2\u013e\u01ce\7Q\2\2\u013f\u0140\7\u00e2\2\2\u0140\u01ce\7Q\2\2\u0141"+
		"\u0142\7_\2\2\u0142\u01ce\7\u00e6\2\2\u0143\u0144\7_\2\2\u0144\u0145\7"+
		"P\2\2\u0145\u01ce\7Q\2\2\u0146\u0147\7_\2\2\u0147\u01ce\7\u00e8\2\2\u0148"+
		"\u0149\7_\2\2\u0149\u01ce\7\u00ea\2\2\u014a\u014b\7_\2\2\u014b\u01ce\7"+
		"\u00eb\2\2\u014c\u014d\7P\2\2\u014d\u01ce\7\u00e9\2\2\u014e\u014f\7f\2"+
		"\2\u014f\u01ce\7\u00e9\2\2\u0150\u0151\7n\2\2\u0151\u01ce\7\u00e9\2\2"+
		"\u0152\u0153\7\u00dc\2\2\u0153\u01ce\7Q\2\2\u0154\u0155\7\u00dc\2\2\u0155"+
		"\u01ce\7\u00cf\2\2\u0156\u0157\7\u00dd\2\2\u0157\u01ce\7Q\2\2\u0158\u0159"+
		"\7\u00dd\2\2\u0159\u01ce\7\u00cf\2\2\u015a\u015b\7P\2\2\u015b\u015c\7"+
		"\u00b6\2\2\u015c\u01ce\7{\2\2\u015d\u015e\7f\2\2\u015e\u015f\7\u00b6\2"+
		"\2\u015f\u01ce\7{\2\2\u0160\u0161\7n\2\2\u0161\u0162\7Q\2\2\u0162\u0163"+
		"\5\u0088E\2\u0163\u0164\7\"\2\2\u0164\u0165\7\u00ca\2\2\u0165\u01ce\3"+
		"\2\2\2\u0166\u0167\7n\2\2\u0167\u0168\7Q\2\2\u0168\u0169\5\u0088E\2\u0169"+
		"\u016a\7\u00ca\2\2\u016a\u016b\7\26\2\2\u016b\u01ce\3\2\2\2\u016c\u016d"+
		"\7n\2\2\u016d\u016e\7Q\2\2\u016e\u016f\5\u0088E\2\u016f\u0170\7\"\2\2"+
		"\u0170\u0171\7\u00cb\2\2\u0171\u01ce\3\2\2\2\u0172\u0173\7n\2\2\u0173"+
		"\u0174\7Q\2\2\u0174\u0175\5\u0088E\2\u0175\u0176\7\u00bc\2\2\u0176\u0177"+
		"\7\26\2\2\u0177\u01ce\3\2\2\2\u0178\u0179\7n\2\2\u0179\u017a\7Q\2\2\u017a"+
		"\u017b\5\u0088E\2\u017b\u017c\7\"\2\2\u017c\u017d\7\u00bc\2\2\u017d\u01ce"+
		"\3\2\2\2\u017e\u017f\7n\2\2\u017f\u0180\7Q\2\2\u0180\u0181\5\u0088E\2"+
		"\u0181\u0182\7\"\2\2\u0182\u0183\7\u00bd\2\2\u0183\u0184\7\21\2\2\u0184"+
		"\u0185\7\u00be\2\2\u0185\u01ce\3\2\2\2\u0186\u0187\7n\2\2\u0187\u0188"+
		"\7Q\2\2\u0188\u0189\5\u0088E\2\u0189\u018a\7t\2\2\u018a\u018b\7\u00bc"+
		"\2\2\u018b\u018c\7\u00bf\2\2\u018c\u01ce\3\2\2\2\u018d\u018e\7n\2\2\u018e"+
		"\u018f\7Q\2\2\u018f\u0190\5\u0088E\2\u0190\u0191\7\u00c0\2\2\u0191\u0192"+
		"\7C\2\2\u0192\u01ce\3\2\2\2\u0193\u0194\7n\2\2\u0194\u0195\7Q\2\2\u0195"+
		"\u0196\5\u0088E\2\u0196\u0197\7\u00c1\2\2\u0197\u0198\7C\2\2\u0198\u01ce"+
		"\3\2\2\2\u0199\u019a\7n\2\2\u019a\u019b\7Q\2\2\u019b\u019c\5\u0088E\2"+
		"\u019c\u019d\7\u00c2\2\2\u019d\u019e\7C\2\2\u019e\u01ce\3\2\2\2\u019f"+
		"\u01a0\7n\2\2\u01a0\u01a1\7Q\2\2\u01a1\u01a2\5\u0088E\2\u01a2\u01a3\7"+
		"\u00c4\2\2\u01a3\u01ce\3\2\2\2\u01a4\u01a5\7n\2\2\u01a5\u01a6\7Q\2\2\u01a6"+
		"\u01a8\5\u0088E\2\u01a7\u01a9\5(\25\2\u01a8\u01a7\3\2\2\2\u01a8\u01a9"+
		"\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u01ab\7\u00c5\2\2\u01ab\u01ce\3\2\2"+
		"\2\u01ac\u01ad\7n\2\2\u01ad\u01ae\7Q\2\2\u01ae\u01b0\5\u0088E\2\u01af"+
		"\u01b1\5(\25\2\u01b0\u01af\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b1\u01b2\3\2"+
		"\2\2\u01b2\u01b3\7\u00c6\2\2\u01b3\u01ce\3\2\2\2\u01b4\u01b5\7n\2\2\u01b5"+
		"\u01b6\7Q\2\2\u01b6\u01b8\5\u0088E\2\u01b7\u01b9\5(\25\2\u01b8\u01b7\3"+
		"\2\2\2\u01b8\u01b9\3\2\2\2\u01b9\u01ba\3\2\2\2\u01ba\u01bb\7t\2\2\u01bb"+
		"\u01bc\7\u00c3\2\2\u01bc\u01ce\3\2\2\2\u01bd\u01be\7n\2\2\u01be\u01bf"+
		"\7Q\2\2\u01bf\u01c1\5\u0088E\2\u01c0\u01c2\5(\25\2\u01c1\u01c0\3\2\2\2"+
		"\u01c1\u01c2\3\2\2\2\u01c2\u01c3\3\2\2\2\u01c3\u01c4\7T\2\2\u01c4\u01c5"+
		"\7a\2\2\u01c5\u01ce\3\2\2\2\u01c6\u01c7\7w\2\2\u01c7\u01ce\7x\2\2\u01c8"+
		"\u01ce\7y\2\2\u01c9\u01ce\7z\2\2\u01ca\u01ce\7\u00d1\2\2\u01cb\u01cc\7"+
		"V\2\2\u01cc\u01ce\7\17\2\2\u01cd\u0123\3\2\2\2\u01cd\u0125\3\2\2\2\u01cd"+
		"\u0127\3\2\2\2\u01cd\u012b\3\2\2\2\u01cd\u012f\3\2\2\2\u01cd\u0131\3\2"+
		"\2\2\u01cd\u0136\3\2\2\2\u01cd\u0138\3\2\2\2\u01cd\u013a\3\2\2\2\u01cd"+
		"\u013d\3\2\2\2\u01cd\u013f\3\2\2\2\u01cd\u0141\3\2\2\2\u01cd\u0143\3\2"+
		"\2\2\u01cd\u0146\3\2\2\2\u01cd\u0148\3\2\2\2\u01cd\u014a\3\2\2\2\u01cd"+
		"\u014c\3\2\2\2\u01cd\u014e\3\2\2\2\u01cd\u0150\3\2\2\2\u01cd\u0152\3\2"+
		"\2\2\u01cd\u0154\3\2\2\2\u01cd\u0156\3\2\2\2\u01cd\u0158\3\2\2\2\u01cd"+
		"\u015a\3\2\2\2\u01cd\u015d\3\2\2\2\u01cd\u0160\3\2\2\2\u01cd\u0166\3\2"+
		"\2\2\u01cd\u016c\3\2\2\2\u01cd\u0172\3\2\2\2\u01cd\u0178\3\2\2\2\u01cd"+
		"\u017e\3\2\2\2\u01cd\u0186\3\2\2\2\u01cd\u018d\3\2\2\2\u01cd\u0193\3\2"+
		"\2\2\u01cd\u0199\3\2\2\2\u01cd\u019f\3\2\2\2\u01cd\u01a4\3\2\2\2\u01cd"+
		"\u01ac\3\2\2\2\u01cd\u01b4\3\2\2\2\u01cd\u01bd\3\2\2\2\u01cd\u01c6\3\2"+
		"\2\2\u01cd\u01c8\3\2\2\2\u01cd\u01c9\3\2\2\2\u01cd\u01ca\3\2\2\2\u01cd"+
		"\u01cb\3\2\2\2\u01ce\31\3\2\2\2\u01cf\u01d1\7P\2\2\u01d0\u01d2\7\u00b6"+
		"\2\2\u01d1\u01d0\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d2\u01d4\3\2\2\2\u01d3"+
		"\u01d5\7\u00d8\2\2\u01d4\u01d3\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d5\u01d6"+
		"\3\2\2\2\u01d6\u01da\7Q\2\2\u01d7\u01d8\7\u0080\2\2\u01d8\u01d9\7\"\2"+
		"\2\u01d9\u01db\7$\2\2\u01da\u01d7\3\2\2\2\u01da\u01db\3\2\2\2\u01db\u01dc"+
		"\3\2\2\2\u01dc\u01dd\5\u0088E\2\u01dd\33\3\2\2\2\u01de\u01df\7\u00ca\2"+
		"\2\u01df\u01e0\7\26\2\2\u01e0\u01e4\5r:\2\u01e1\u01e2\7\u00cb\2\2\u01e2"+
		"\u01e3\7\26\2\2\u01e3\u01e5\5v<\2\u01e4\u01e1\3\2\2\2\u01e4\u01e5\3\2"+
		"\2\2\u01e5\u01e6\3\2\2\2\u01e6\u01e7\7W\2\2\u01e7\u01e8\7\u00f4\2\2\u01e8"+
		"\u01e9\7\u00bb\2\2\u01e9\35\3\2\2\2\u01ea\u01eb\7\u00bc\2\2\u01eb\u01ec"+
		"\7\26\2\2\u01ec\u01ed\5r:\2\u01ed\u01f0\7?\2\2\u01ee\u01f1\5> \2\u01ef"+
		"\u01f1\5@!\2\u01f0\u01ee\3\2\2\2\u01f0\u01ef\3\2\2\2\u01f1\u01f5\3\2\2"+
		"\2\u01f2\u01f3\7\u00bd\2\2\u01f3\u01f4\7\21\2\2\u01f4\u01f6\7\u00be\2"+
		"\2\u01f5\u01f2\3\2\2\2\u01f5\u01f6\3\2\2\2\u01f6\37\3\2\2\2\u01f7\u01f8"+
		"\7\u00bf\2\2\u01f8\u01f9\7\u00f0\2\2\u01f9!\3\2\2\2\u01fa\u01fc\5\60\31"+
		"\2\u01fb\u01fa\3\2\2\2\u01fb\u01fc\3\2\2\2\u01fc\u01fd\3\2\2\2\u01fd\u01fe"+
		"\5J&\2\u01fe#\3\2\2\2\u01ff\u0200\7U\2\2\u0200\u0201\7\u009c\2\2\u0201"+
		"\u0202\7Q\2\2\u0202\u0209\5\u0088E\2\u0203\u0207\5(\25\2\u0204\u0205\7"+
		"\u0080\2\2\u0205\u0206\7\"\2\2\u0206\u0208\7$\2\2\u0207\u0204\3\2\2\2"+
		"\u0207\u0208\3\2\2\2\u0208\u020a\3\2\2\2\u0209\u0203\3\2\2\2\u0209\u020a"+
		"\3\2\2\2\u020a\u0230\3\2\2\2\u020b\u020c\7U\2\2\u020c\u020e\7W\2\2\u020d"+
		"\u020f\7Q\2\2\u020e\u020d\3\2\2\2\u020e\u020f\3\2\2\2\u020f\u0210\3\2"+
		"\2\2\u0210\u0212\5\u0088E\2\u0211\u0213\5(\25\2\u0212\u0211\3\2\2\2\u0212"+
		"\u0213\3\2\2\2\u0213\u0230\3\2\2\2\u0214\u0215\7U\2\2\u0215\u0217\7\u009c"+
		"\2\2\u0216\u0218\7\u00ee\2\2\u0217\u0216\3\2\2\2\u0217\u0218\3\2\2\2\u0218"+
		"\u0219\3\2\2\2\u0219\u021a\7R\2\2\u021a\u021c\7\u00f0\2\2\u021b\u021d"+
		"\5\u0086D\2\u021c\u021b\3\2\2\2\u021c\u021d\3\2\2\2\u021d\u021f\3\2\2"+
		"\2\u021e\u0220\5B\"\2\u021f\u021e\3\2\2\2\u021f\u0220\3\2\2\2\u0220\u0230"+
		"\3\2\2\2\u0221\u0222\7U\2\2\u0222\u0224\7\u009c\2\2\u0223\u0225\7\u00ee"+
		"\2\2\u0224\u0223\3\2\2\2\u0224\u0225\3\2\2\2\u0225\u0226\3\2\2\2\u0226"+
		"\u0228\7R\2\2\u0227\u0229\7\u00f0\2\2\u0228\u0227\3\2\2\2\u0228\u0229"+
		"\3\2\2\2\u0229\u022a\3\2\2\2\u022a\u022d\5\64\33\2\u022b\u022c\7\u00b7"+
		"\2\2\u022c\u022e\5\66\34\2\u022d\u022b\3\2\2\2\u022d\u022e\3\2\2\2\u022e"+
		"\u0230\3\2\2\2\u022f\u01ff\3\2\2\2\u022f\u020b\3\2\2\2\u022f\u0214\3\2"+
		"\2\2\u022f\u0221\3\2\2\2\u0230%\3\2\2\2\u0231\u0233\5(\25\2\u0232\u0234"+
		"\5 \21\2\u0233\u0232\3\2\2\2\u0233\u0234\3\2\2\2\u0234\'\3\2\2\2\u0235"+
		"\u0236\7C\2\2\u0236\u0237\7\3\2\2\u0237\u023c\5*\26\2\u0238\u0239\7\4"+
		"\2\2\u0239\u023b\5*\26\2\u023a\u0238\3\2\2\2\u023b\u023e\3\2\2\2\u023c"+
		"\u023a\3\2\2\2\u023c\u023d\3\2\2\2\u023d\u023f\3\2\2\2\u023e\u023c\3\2"+
		"\2\2\u023f\u0240\7\5\2\2\u0240)\3\2\2\2\u0241\u0244\5\u00c6d\2\u0242\u0243"+
		"\7\u0082\2\2\u0243\u0245\5\u009cO\2\u0244\u0242\3\2\2\2\u0244\u0245\3"+
		"\2\2\2\u0245+\3\2\2\2\u0246\u024c\5\u00c4c\2\u0247\u024c\7\u00f0\2\2\u0248"+
		"\u024c\5\u009eP\2\u0249\u024c\5\u00a0Q\2\u024a\u024c\5\u00a2R\2\u024b"+
		"\u0246\3\2\2\2\u024b\u0247\3\2\2\2\u024b\u0248\3\2\2\2\u024b\u0249\3\2"+
		"\2\2\u024b\u024a\3\2\2\2\u024c-\3\2\2\2\u024d\u0252\5\u00c6d\2\u024e\u024f"+
		"\7\6\2\2\u024f\u0251\5\u00c6d\2\u0250\u024e\3\2\2\2\u0251\u0254\3\2\2"+
		"\2\u0252\u0250\3\2\2\2\u0252\u0253\3\2\2\2\u0253/\3\2\2\2\u0254\u0252"+
		"\3\2\2\2\u0255\u0256\7N\2\2\u0256\u025b\5\62\32\2\u0257\u0258\7\4\2\2"+
		"\u0258\u025a\5\62\32\2\u0259\u0257\3\2\2\2\u025a\u025d\3\2\2\2\u025b\u0259"+
		"\3\2\2\2\u025b\u025c\3\2\2\2\u025c\61\3\2\2\2\u025d\u025b\3\2\2\2\u025e"+
		"\u0260\5\u00c6d\2\u025f\u0261\7\21\2\2\u0260\u025f\3\2\2\2\u0260\u0261"+
		"\3\2\2\2\u0261\u0262\3\2\2\2\u0262\u0263\7\3\2\2\u0263\u0264\5\"\22\2"+
		"\u0264\u0265\7\5\2\2\u0265\63\3\2\2\2\u0266\u0267\7\u009f\2\2\u0267\u0268"+
		"\5\u00c4c\2\u0268\65\3\2\2\2\u0269\u026a\7\3\2\2\u026a\u026f\58\35\2\u026b"+
		"\u026c\7\4\2\2\u026c\u026e\58\35\2\u026d\u026b\3\2\2\2\u026e\u0271\3\2"+
		"\2\2\u026f\u026d\3\2\2\2\u026f\u0270\3\2\2\2\u0270\u0272\3\2\2\2\u0271"+
		"\u026f\3\2\2\2\u0272\u0273\7\5\2\2\u0273\67\3\2\2\2\u0274\u0279\5:\36"+
		"\2\u0275\u0277\7\u0082\2\2\u0276\u0275\3\2\2\2\u0276\u0277\3\2\2\2\u0277"+
		"\u0278\3\2\2\2\u0278\u027a\5<\37\2\u0279\u0276\3\2\2\2\u0279\u027a\3\2"+
		"\2\2\u027a9\3\2\2\2\u027b\u0280\5\u00c6d\2\u027c\u027d\7\6\2\2\u027d\u027f"+
		"\5\u00c6d\2\u027e\u027c\3\2\2\2\u027f\u0282\3\2\2\2\u0280\u027e\3\2\2"+
		"\2\u0280\u0281\3\2\2\2\u0281\u0285\3\2\2\2\u0282\u0280\3\2\2\2\u0283\u0285"+
		"\7\u00f0\2\2\u0284\u027b\3\2\2\2\u0284\u0283\3\2\2\2\u0285;\3\2\2\2\u0286"+
		"\u028b\7\u00f4\2\2\u0287\u028b\7\u00f5\2\2\u0288\u028b\5\u00a4S\2\u0289"+
		"\u028b\7\u00f0\2\2\u028a\u0286\3\2\2\2\u028a\u0287\3\2\2\2\u028a\u0288"+
		"\3\2\2\2\u028a\u0289\3\2\2\2\u028b=\3\2\2\2\u028c\u028d\7\3\2\2\u028d"+
		"\u0292\5\u009cO\2\u028e\u028f\7\4\2\2\u028f\u0291\5\u009cO\2\u0290\u028e"+
		"\3\2\2\2\u0291\u0294\3\2\2\2\u0292\u0290\3\2\2\2\u0292\u0293\3\2\2\2\u0293"+
		"\u0295\3\2\2\2\u0294\u0292\3\2\2\2\u0295\u0296\7\5\2\2\u0296?\3\2\2\2"+
		"\u0297\u0298\7\3\2\2\u0298\u029d\5> \2\u0299\u029a\7\4\2\2\u029a\u029c"+
		"\5> \2\u029b\u0299\3\2\2\2\u029c\u029f\3\2\2\2\u029d\u029b\3\2\2\2\u029d"+
		"\u029e\3\2\2\2\u029e\u02a0\3\2\2\2\u029f\u029d\3\2\2\2\u02a0\u02a1\7\5"+
		"\2\2\u02a1A\3\2\2\2\u02a2\u02a3\7\u00bd\2\2\u02a3\u02a4\7\21\2\2\u02a4"+
		"\u02a9\5D#\2\u02a5\u02a6\7\u00bd\2\2\u02a6\u02a7\7\26\2\2\u02a7\u02a9"+
		"\5F$\2\u02a8\u02a2\3\2\2\2\u02a8\u02a5\3\2\2\2\u02a9C\3\2\2\2\u02aa\u02ab"+
		"\7\u00cd\2\2\u02ab\u02ac\7\u00f0\2\2\u02ac\u02ad\7\u00ce\2\2\u02ad\u02b0"+
		"\7\u00f0\2\2\u02ae\u02b0\5\u00c6d\2\u02af\u02aa\3\2\2\2\u02af\u02ae\3"+
		"\2\2\2\u02b0E\3\2\2\2\u02b1\u02b5\7\u00f0\2\2\u02b2\u02b3\7N\2\2\u02b3"+
		"\u02b4\7\u00a1\2\2\u02b4\u02b6\5\66\34\2\u02b5\u02b2\3\2\2\2\u02b5\u02b6"+
		"\3\2\2\2\u02b6G\3\2\2\2\u02b7\u02b8\5\u00c6d\2\u02b8\u02b9\7\u00f0\2\2"+
		"\u02b9I\3\2\2\2\u02ba\u02bc\5$\23\2\u02bb\u02ba\3\2\2\2\u02bb\u02bc\3"+
		"\2\2\2\u02bc\u02bd\3\2\2\2\u02bd\u02be\5P)\2\u02be\u02bf\5L\'\2\u02bf"+
		"\u02c7\3\2\2\2\u02c0\u02c2\5\\/\2\u02c1\u02c3\5N(\2\u02c2\u02c1\3\2\2"+
		"\2\u02c3\u02c4\3\2\2\2\u02c4\u02c2\3\2\2\2\u02c4\u02c5\3\2\2\2\u02c5\u02c7"+
		"\3\2\2\2\u02c6\u02bb\3\2\2\2\u02c6\u02c0\3\2\2\2\u02c7K\3\2\2\2\u02c8"+
		"\u02c9\7\33\2\2\u02c9\u02ca\7\26\2\2\u02ca\u02cf\5T+\2\u02cb\u02cc\7\4"+
		"\2\2\u02cc\u02ce\5T+\2\u02cd\u02cb\3\2\2\2\u02ce\u02d1\3\2\2\2\u02cf\u02cd"+
		"\3\2\2\2\u02cf\u02d0\3\2\2\2\u02d0\u02d3\3\2\2\2\u02d1\u02cf\3\2\2\2\u02d2"+
		"\u02c8\3\2\2\2\u02d2\u02d3\3\2\2\2\u02d3\u02de\3\2\2\2\u02d4\u02d5\7\u009a"+
		"\2\2\u02d5\u02d6\7\26\2\2\u02d6\u02db\5\u0090I\2\u02d7\u02d8\7\4\2\2\u02d8"+
		"\u02da\5\u0090I\2\u02d9\u02d7\3\2\2\2\u02da\u02dd\3\2\2\2\u02db\u02d9"+
		"\3\2\2\2\u02db\u02dc\3\2\2\2\u02dc\u02df\3\2\2\2\u02dd\u02db\3\2\2\2\u02de"+
		"\u02d4\3\2\2\2\u02de\u02df\3\2\2\2\u02df\u02ea\3\2\2\2\u02e0\u02e1\7\u009b"+
		"\2\2\u02e1\u02e2\7\26\2\2\u02e2\u02e7\5\u0090I\2\u02e3\u02e4\7\4\2\2\u02e4"+
		"\u02e6\5\u0090I\2\u02e5\u02e3\3\2\2\2\u02e6\u02e9\3\2\2\2\u02e7\u02e5"+
		"\3\2\2\2\u02e7\u02e8\3\2\2\2\u02e8\u02eb\3\2\2\2\u02e9\u02e7\3\2\2\2\u02ea"+
		"\u02e0\3\2\2\2\u02ea\u02eb\3\2\2\2\u02eb\u02f6\3\2\2\2\u02ec\u02ed\7\u0099"+
		"\2\2\u02ed\u02ee\7\26\2\2\u02ee\u02f3\5T+\2\u02ef\u02f0\7\4\2\2\u02f0"+
		"\u02f2\5T+\2\u02f1\u02ef\3\2\2\2\u02f2\u02f5\3\2\2\2\u02f3\u02f1\3\2\2"+
		"\2\u02f3\u02f4\3\2\2\2\u02f4\u02f7\3\2\2\2\u02f5\u02f3\3\2\2\2\u02f6\u02ec"+
		"\3\2\2\2\u02f6\u02f7\3\2\2\2\u02f7\u02f9\3\2\2\2\u02f8\u02fa\5\u00ba^"+
		"\2\u02f9\u02f8\3\2\2\2\u02f9\u02fa\3\2\2\2\u02fa\u0300\3\2\2\2\u02fb\u02fe"+
		"\7\35\2\2\u02fc\u02ff\7\22\2\2\u02fd\u02ff\5\u0090I\2\u02fe\u02fc\3\2"+
		"\2\2\u02fe\u02fd\3\2\2\2\u02ff\u0301\3\2\2\2\u0300\u02fb\3\2\2\2\u0300"+
		"\u0301\3\2\2\2\u0301M\3\2\2\2\u0302\u0304\5$\23\2\u0303\u0302\3\2\2\2"+
		"\u0303\u0304\3\2\2\2\u0304\u0305\3\2\2\2\u0305\u0306\5V,\2\u0306\u0307"+
		"\5L\'\2\u0307O\3\2\2\2\u0308\u0309\b)\1\2\u0309\u030a\5R*\2\u030a\u0313"+
		"\3\2\2\2\u030b\u030c\f\3\2\2\u030c\u030e\t\2\2\2\u030d\u030f\5d\63\2\u030e"+
		"\u030d\3\2\2\2\u030e\u030f\3\2\2\2\u030f\u0310\3\2\2\2\u0310\u0312\5P"+
		")\4\u0311\u030b\3\2\2\2\u0312\u0315\3\2\2\2\u0313\u0311\3\2\2\2\u0313"+
		"\u0314\3\2\2\2\u0314Q\3\2\2\2\u0315\u0313\3\2\2\2\u0316\u031f\5V,\2\u0317"+
		"\u0318\7Q\2\2\u0318\u031f\5\u0088E\2\u0319\u031f\5\u0080A\2\u031a\u031b"+
		"\7\3\2\2\u031b\u031c\5J&\2\u031c\u031d\7\5\2\2\u031d\u031f\3\2\2\2\u031e"+
		"\u0316\3\2\2\2\u031e\u0317\3\2\2\2\u031e\u0319\3\2\2\2\u031e\u031a\3\2"+
		"\2\2\u031fS\3\2\2\2\u0320\u0322\5\u0090I\2\u0321\u0323\t\3\2\2\u0322\u0321"+
		"\3\2\2\2\u0322\u0323\3\2\2\2\u0323\u0326\3\2\2\2\u0324\u0325\7,\2\2\u0325"+
		"\u0327\t\4\2\2\u0326\u0324\3\2\2\2\u0326\u0327\3\2\2\2\u0327U\3\2\2\2"+
		"\u0328\u0329\7\16\2\2\u0329\u032a\7\u009d\2\2\u032a\u032b\7\3\2\2\u032b"+
		"\u032c\5\u008eH\2\u032c\u032d\7\5\2\2\u032d\u0333\3\2\2\2\u032e\u032f"+
		"\7q\2\2\u032f\u0333\5\u008eH\2\u0330\u0331\7\u009e\2\2\u0331\u0333\5\u008e"+
		"H\2\u0332\u0328\3\2\2\2\u0332\u032e\3\2\2\2\u0332\u0330\3\2\2\2\u0333"+
		"\u0335\3\2\2\2\u0334\u0336\5\u0086D\2\u0335\u0334\3\2\2\2\u0335\u0336"+
		"\3\2\2\2\u0336\u0339\3\2\2\2\u0337\u0338\7\u00a3\2\2\u0338\u033a\7\u00f0"+
		"\2\2\u0339\u0337\3\2\2\2\u0339\u033a\3\2\2\2\u033a\u033b\3\2\2\2\u033b"+
		"\u033c\7\u009f\2\2\u033c\u0349\7\u00f0\2\2\u033d\u0347\7\21\2\2\u033e"+
		"\u0348\5t;\2\u033f\u0348\5\u00b0Y\2\u0340\u0343\7\3\2\2\u0341\u0344\5"+
		"t;\2\u0342\u0344\5\u00b0Y\2\u0343\u0341\3\2\2\2\u0343\u0342\3\2\2\2\u0344"+
		"\u0345\3\2\2\2\u0345\u0346\7\5\2\2\u0346\u0348\3\2\2\2\u0347\u033e\3\2"+
		"\2\2\u0347\u033f\3\2\2\2\u0347\u0340\3\2\2\2\u0348\u034a\3\2\2\2\u0349"+
		"\u033d\3\2\2\2\u0349\u034a\3\2\2\2\u034a\u034c\3\2\2\2\u034b\u034d\5\u0086"+
		"D\2\u034c\u034b\3\2\2\2\u034c\u034d\3\2\2\2\u034d\u0350\3\2\2\2\u034e"+
		"\u034f\7\u00a2\2\2\u034f\u0351\7\u00f0\2\2\u0350\u034e\3\2\2\2\u0350\u0351"+
		"\3\2\2\2\u0351\u0353\3\2\2\2\u0352\u0354\5\\/\2\u0353\u0352\3\2\2\2\u0353"+
		"\u0354\3\2\2\2\u0354\u0357\3\2\2\2\u0355\u0356\7\24\2\2\u0356\u0358\5"+
		"\u0092J\2\u0357\u0355\3\2\2\2\u0357\u0358\3\2\2\2\u0358\u0386\3\2\2\2"+
		"\u0359\u035d\7\16\2\2\u035a\u035c\5X-\2\u035b\u035a\3\2\2\2\u035c\u035f"+
		"\3\2\2\2\u035d\u035b\3\2\2\2\u035d\u035e\3\2\2\2\u035e\u0361\3\2\2\2\u035f"+
		"\u035d\3\2\2\2\u0360\u0362\5d\63\2\u0361\u0360\3\2\2\2\u0361\u0362\3\2"+
		"\2\2\u0362\u0363\3\2\2\2\u0363\u0365\5\u008eH\2\u0364\u0366\5\\/\2\u0365"+
		"\u0364\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u0370\3\2\2\2\u0367\u036d\5\\"+
		"/\2\u0368\u036a\7\16\2\2\u0369\u036b\5d\63\2\u036a\u0369\3\2\2\2\u036a"+
		"\u036b\3\2\2\2\u036b\u036c\3\2\2\2\u036c\u036e\5\u008eH\2\u036d\u0368"+
		"\3\2\2\2\u036d\u036e\3\2\2\2\u036e\u0370\3\2\2\2\u036f\u0359\3\2\2\2\u036f"+
		"\u0367\3\2\2\2\u0370\u0374\3\2\2\2\u0371\u0373\5b\62\2\u0372\u0371\3\2"+
		"\2\2\u0373\u0376\3\2\2\2\u0374\u0372\3\2\2\2\u0374\u0375\3\2\2\2\u0375"+
		"\u0379\3\2\2\2\u0376\u0374\3\2\2\2\u0377\u0378\7\24\2\2\u0378\u037a\5"+
		"\u0092J\2\u0379\u0377\3\2\2\2\u0379\u037a\3\2\2\2\u037a\u037c\3\2\2\2"+
		"\u037b\u037d\5^\60\2\u037c\u037b\3\2\2\2\u037c\u037d\3\2\2\2\u037d\u0380"+
		"\3\2\2\2\u037e\u037f\7\34\2\2\u037f\u0381\5\u0092J\2\u0380\u037e\3\2\2"+
		"\2\u0380\u0381\3\2\2\2\u0381\u0383\3\2\2\2\u0382\u0384\5\u00ba^\2\u0383"+
		"\u0382\3\2\2\2\u0383\u0384\3\2\2\2\u0384\u0386\3\2\2\2\u0385\u0332\3\2"+
		"\2\2\u0385\u036f\3\2\2\2\u0386W\3\2\2\2\u0387\u0388\7\7\2\2\u0388\u038f"+
		"\5Z.\2\u0389\u038b\7\4\2\2\u038a\u0389\3\2\2\2\u038a\u038b\3\2\2\2\u038b"+
		"\u038c\3\2\2\2\u038c\u038e\5Z.\2\u038d\u038a\3\2\2\2\u038e\u0391\3\2\2"+
		"\2\u038f\u038d\3\2\2\2\u038f\u0390\3\2\2\2\u0390\u0392\3\2\2\2\u0391\u038f"+
		"\3\2\2\2\u0392\u0393\7\b\2\2\u0393Y\3\2\2\2\u0394\u03a2\5\u00c6d\2\u0395"+
		"\u0396\5\u00c6d\2\u0396\u0397\7\3\2\2\u0397\u039c\5\u009aN\2\u0398\u0399"+
		"\7\4\2\2\u0399\u039b\5\u009aN\2\u039a\u0398\3\2\2\2\u039b\u039e\3\2\2"+
		"\2\u039c\u039a\3\2\2\2\u039c\u039d\3\2\2\2\u039d\u039f\3\2\2\2\u039e\u039c"+
		"\3\2\2\2\u039f\u03a0\7\5\2\2\u03a0\u03a2\3\2\2\2\u03a1\u0394\3\2\2\2\u03a1"+
		"\u0395\3\2\2\2\u03a2[\3\2\2\2\u03a3\u03a4\7\17\2\2\u03a4\u03a9\5f\64\2"+
		"\u03a5\u03a6\7\4\2\2\u03a6\u03a8\5f\64\2\u03a7\u03a5\3\2\2\2\u03a8\u03ab"+
		"\3\2\2\2\u03a9\u03a7\3\2\2\2\u03a9\u03aa\3\2\2\2\u03aa\u03af\3\2\2\2\u03ab"+
		"\u03a9\3\2\2\2\u03ac\u03ae\5b\62\2\u03ad\u03ac\3\2\2\2\u03ae\u03b1\3\2"+
		"\2\2\u03af\u03ad\3\2\2\2\u03af\u03b0\3\2\2\2\u03b0]\3\2\2\2\u03b1\u03af"+
		"\3\2\2\2\u03b2\u03b3\7\25\2\2\u03b3\u03b4\7\26\2\2\u03b4\u03b9\5\u0090"+
		"I\2\u03b5\u03b6\7\4\2\2\u03b6\u03b8\5\u0090I\2\u03b7\u03b5\3\2\2\2\u03b8"+
		"\u03bb\3\2\2\2\u03b9\u03b7\3\2\2\2\u03b9\u03ba\3\2\2\2\u03ba\u03cd\3\2"+
		"\2\2\u03bb\u03b9\3\2\2\2\u03bc\u03bd\7N\2\2\u03bd\u03ce\7\32\2\2\u03be"+
		"\u03bf\7N\2\2\u03bf\u03ce\7\31\2\2\u03c0\u03c1\7\27\2\2\u03c1\u03c2\7"+
		"\30\2\2\u03c2\u03c3\7\3\2\2\u03c3\u03c8\5`\61\2\u03c4\u03c5\7\4\2\2\u03c5"+
		"\u03c7\5`\61\2\u03c6\u03c4\3\2\2\2\u03c7\u03ca\3\2\2\2\u03c8\u03c6\3\2"+
		"\2\2\u03c8\u03c9\3\2\2\2\u03c9\u03cb\3\2\2\2\u03ca\u03c8\3\2\2\2\u03cb"+
		"\u03cc\7\5\2\2\u03cc\u03ce\3\2\2\2\u03cd\u03bc\3\2\2\2\u03cd\u03be\3\2"+
		"\2\2\u03cd\u03c0\3\2\2\2\u03cd\u03ce\3\2\2\2\u03ce_\3\2\2\2\u03cf\u03d8"+
		"\7\3\2\2\u03d0\u03d5\5\u0090I\2\u03d1\u03d2\7\4\2\2\u03d2\u03d4\5\u0090"+
		"I\2\u03d3\u03d1\3\2\2\2\u03d4\u03d7\3\2\2\2\u03d5\u03d3\3\2\2\2\u03d5"+
		"\u03d6\3\2\2\2\u03d6\u03d9\3\2\2\2\u03d7\u03d5\3\2\2\2\u03d8\u03d0\3\2"+
		"\2\2\u03d8\u03d9\3\2\2\2\u03d9\u03da\3\2\2\2\u03da\u03dd\7\5\2\2\u03db"+
		"\u03dd\5\u0090I\2\u03dc\u03cf\3\2\2\2\u03dc\u03db\3\2\2\2\u03dda\3\2\2"+
		"\2\u03de\u03df\7@\2\2\u03df\u03e1\7S\2\2\u03e0\u03e2\78\2\2\u03e1\u03e0"+
		"\3\2\2\2\u03e1\u03e2\3\2\2\2\u03e2\u03e3\3\2\2\2\u03e3\u03e4\5\u00c4c"+
		"\2\u03e4\u03ed\7\3\2\2\u03e5\u03ea\5\u0090I\2\u03e6\u03e7\7\4\2\2\u03e7"+
		"\u03e9\5\u0090I\2\u03e8\u03e6\3\2\2\2\u03e9\u03ec\3\2\2\2\u03ea\u03e8"+
		"\3\2\2\2\u03ea\u03eb\3\2\2\2\u03eb\u03ee\3\2\2\2\u03ec\u03ea\3\2\2\2\u03ed"+
		"\u03e5\3\2\2\2\u03ed\u03ee\3\2\2\2\u03ee\u03ef\3\2\2\2\u03ef\u03f0\7\5"+
		"\2\2\u03f0\u03fc\5\u00c6d\2\u03f1\u03f3\7\21\2\2\u03f2\u03f1\3\2\2\2\u03f2"+
		"\u03f3\3\2\2\2\u03f3\u03f4\3\2\2\2\u03f4\u03f9\5\u00c6d\2\u03f5\u03f6"+
		"\7\4\2\2\u03f6\u03f8\5\u00c6d\2\u03f7\u03f5\3\2\2\2\u03f8\u03fb\3\2\2"+
		"\2\u03f9\u03f7\3\2\2\2\u03f9\u03fa\3\2\2\2\u03fa\u03fd\3\2\2\2\u03fb\u03f9"+
		"\3\2\2\2\u03fc\u03f2\3\2\2\2\u03fc\u03fd\3\2\2\2\u03fdc\3\2\2\2\u03fe"+
		"\u03ff\t\5\2\2\u03ffe\3\2\2\2\u0400\u0404\5~@\2\u0401\u0403\5h\65\2\u0402"+
		"\u0401\3\2\2\2\u0403\u0406\3\2\2\2\u0404\u0402\3\2\2\2\u0404\u0405\3\2"+
		"\2\2\u0405g\3\2\2\2\u0406\u0404\3\2\2\2\u0407\u0408\5j\66\2\u0408\u0409"+
		"\7\66\2\2\u0409\u040b\5~@\2\u040a\u040c\5l\67\2\u040b\u040a\3\2\2\2\u040b"+
		"\u040c\3\2\2\2\u040c\u0413\3\2\2\2\u040d\u040e\7>\2\2\u040e\u040f\5j\66"+
		"\2\u040f\u0410\7\66\2\2\u0410\u0411\5~@\2\u0411\u0413\3\2\2\2\u0412\u0407"+
		"\3\2\2\2\u0412\u040d\3\2\2\2\u0413i\3\2\2\2\u0414\u0416\79\2\2\u0415\u0414"+
		"\3\2\2\2\u0415\u0416\3\2\2\2\u0416\u042b\3\2\2\2\u0417\u042b\7\67\2\2"+
		"\u0418\u041a\7:\2\2\u0419\u041b\78\2\2\u041a\u0419\3\2\2\2\u041a\u041b"+
		"\3\2\2\2\u041b\u042b\3\2\2\2\u041c\u041d\7:\2\2\u041d\u042b\7;\2\2\u041e"+
		"\u0420\7<\2\2\u041f\u0421\78\2\2\u0420\u041f\3\2\2\2\u0420\u0421\3\2\2"+
		"\2\u0421\u042b\3\2\2\2\u0422\u0424\7=\2\2\u0423\u0425\78\2\2\u0424\u0423"+
		"\3\2\2\2\u0424\u0425\3\2\2\2\u0425\u042b\3\2\2\2\u0426\u0428\7:\2\2\u0427"+
		"\u0426\3\2\2\2\u0427\u0428\3\2\2\2\u0428\u0429\3\2\2\2\u0429\u042b\7\u00ed"+
		"\2\2\u042a\u0415\3\2\2\2\u042a\u0417\3\2\2\2\u042a\u0418\3\2\2\2\u042a"+
		"\u041c\3\2\2\2\u042a\u041e\3\2\2\2\u042a\u0422\3\2\2\2\u042a\u0427\3\2"+
		"\2\2\u042bk\3\2\2\2\u042c\u042d\7?\2\2\u042d\u043b\5\u0092J\2\u042e\u042f"+
		"\7\u009f\2\2\u042f\u0430\7\3\2\2\u0430\u0435\5\u00c6d\2\u0431\u0432\7"+
		"\4\2\2\u0432\u0434\5\u00c6d\2\u0433\u0431\3\2\2\2\u0434\u0437\3\2\2\2"+
		"\u0435\u0433\3\2\2\2\u0435\u0436\3\2\2\2\u0436\u0438\3\2\2\2\u0437\u0435"+
		"\3\2\2\2\u0438\u0439\7\5\2\2\u0439\u043b\3\2\2\2\u043a\u042c\3\2\2\2\u043a"+
		"\u042e\3\2\2\2\u043bm\3\2\2\2\u043c\u043d\7l\2\2\u043d\u043f\7\3\2\2\u043e"+
		"\u0440\5p9\2\u043f\u043e\3\2\2\2\u043f\u0440\3\2\2\2\u0440\u0441\3\2\2"+
		"\2\u0441\u0442\7\5\2\2\u0442o\3\2\2\2\u0443\u0445\7\u008b\2\2\u0444\u0443"+
		"\3\2\2\2\u0444\u0445\3\2\2\2\u0445\u0446\3\2\2\2\u0446\u0447\t\6\2\2\u0447"+
		"\u045c\7\u0095\2\2\u0448\u0449\5\u0090I\2\u0449\u044a\7E\2\2\u044a\u045c"+
		"\3\2\2\2\u044b\u044c\7\u0096\2\2\u044c\u044d\7\u00f4\2\2\u044d\u044e\7"+
		"\u0097\2\2\u044e\u044f\7\u0098\2\2\u044f\u0458\7\u00f4\2\2\u0450\u0456"+
		"\7?\2\2\u0451\u0457\5\u00c6d\2\u0452\u0453\5\u00c4c\2\u0453\u0454\7\3"+
		"\2\2\u0454\u0455\7\5\2\2\u0455\u0457\3\2\2\2\u0456\u0451\3\2\2\2\u0456"+
		"\u0452\3\2\2\2\u0457\u0459\3\2\2\2\u0458\u0450\3\2\2\2\u0458\u0459\3\2"+
		"\2\2\u0459\u045c\3\2\2\2\u045a\u045c\5\u0090I\2\u045b\u0444\3\2\2\2\u045b"+
		"\u0448\3\2\2\2\u045b\u044b\3\2\2\2\u045b\u045a\3\2\2\2\u045cq\3\2\2\2"+
		"\u045d\u045e\7\3\2\2\u045e\u045f\5t;\2\u045f\u0460\7\5\2\2\u0460s\3\2"+
		"\2\2\u0461\u0466\5\u00c6d\2\u0462\u0463\7\4\2\2\u0463\u0465\5\u00c6d\2"+
		"\u0464\u0462\3\2\2\2\u0465\u0468\3\2\2\2\u0466\u0464\3\2\2\2\u0466\u0467"+
		"\3\2\2\2\u0467u\3\2\2\2\u0468\u0466\3\2\2\2\u0469\u046a\7\3\2\2\u046a"+
		"\u046f\5x=\2\u046b\u046c\7\4\2\2\u046c\u046e\5x=\2\u046d\u046b\3\2\2\2"+
		"\u046e\u0471\3\2\2\2\u046f\u046d\3\2\2\2\u046f\u0470\3\2\2\2\u0470\u0472"+
		"\3\2\2\2\u0471\u046f\3\2\2\2\u0472\u0473\7\5\2\2\u0473w\3\2\2\2\u0474"+
		"\u0476\5\u00c6d\2\u0475\u0477\t\3\2\2\u0476\u0475\3\2\2\2\u0476\u0477"+
		"\3\2\2\2\u0477y\3\2\2\2\u0478\u0479\7\3\2\2\u0479\u047e\5|?\2\u047a\u047b"+
		"\7\4\2\2\u047b\u047d\5|?\2\u047c\u047a\3\2\2\2\u047d\u0480\3\2\2\2\u047e"+
		"\u047c\3\2\2\2\u047e\u047f\3\2\2\2\u047f\u0481\3\2\2\2\u0480\u047e\3\2"+
		"\2\2\u0481\u0482\7\5\2\2\u0482{\3\2\2\2\u0483\u0486\5\u00c6d\2\u0484\u0485"+
		"\7s\2\2\u0485\u0487\7\u00f0\2\2\u0486\u0484\3\2\2\2\u0486\u0487\3\2\2"+
		"\2\u0487}\3\2\2\2\u0488\u048a\5\u0088E\2\u0489\u048b\5n8\2\u048a\u0489"+
		"\3\2\2\2\u048a\u048b\3\2\2\2\u048b\u048c\3\2\2\2\u048c\u048d\5\u0084C"+
		"\2\u048d\u04a1\3\2\2\2\u048e\u048f\7\3\2\2\u048f\u0490\5J&\2\u0490\u0492"+
		"\7\5\2\2\u0491\u0493\5n8\2\u0492\u0491\3\2\2\2\u0492\u0493\3\2\2\2\u0493"+
		"\u0494\3\2\2\2\u0494\u0495\5\u0084C\2\u0495\u04a1\3\2\2\2\u0496\u0497"+
		"\7\3\2\2\u0497\u0498\5f\64\2\u0498\u049a\7\5\2\2\u0499\u049b\5n8\2\u049a"+
		"\u0499\3\2\2\2\u049a\u049b\3\2\2\2\u049b\u049c\3\2\2\2\u049c\u049d\5\u0084"+
		"C\2\u049d\u04a1\3\2\2\2\u049e\u04a1\5\u0080A\2\u049f\u04a1\5\u0082B\2"+
		"\u04a0\u0488\3\2\2\2\u04a0\u048e\3\2\2\2\u04a0\u0496\3\2\2\2\u04a0\u049e"+
		"\3\2\2\2\u04a0\u049f\3\2\2\2\u04a1\177\3\2\2\2\u04a2\u04a3\7O\2\2\u04a3"+
		"\u04a8\5\u0090I\2\u04a4\u04a5\7\4\2\2\u04a5\u04a7\5\u0090I\2\u04a6\u04a4"+
		"\3\2\2\2\u04a7\u04aa\3\2\2\2\u04a8\u04a6\3\2\2\2\u04a8\u04a9\3\2\2\2\u04a9"+
		"\u04ab\3\2\2\2\u04aa\u04a8\3\2\2\2\u04ab\u04ac\5\u0084C\2\u04ac\u0081"+
		"\3\2\2\2\u04ad\u04ae\5\u00c6d\2\u04ae\u04b7\7\3\2\2\u04af\u04b4\5\u0090"+
		"I\2\u04b0\u04b1\7\4\2\2\u04b1\u04b3\5\u0090I\2\u04b2\u04b0\3\2\2\2\u04b3"+
		"\u04b6\3\2\2\2\u04b4\u04b2\3\2\2\2\u04b4\u04b5\3\2\2\2\u04b5\u04b8\3\2"+
		"\2\2\u04b6\u04b4\3\2\2\2\u04b7\u04af\3\2\2\2\u04b7\u04b8\3\2\2\2\u04b8"+
		"\u04b9\3\2\2\2\u04b9\u04ba\7\5\2\2\u04ba\u04bb\5\u0084C\2\u04bb\u0083"+
		"\3\2\2\2\u04bc\u04be\7\21\2\2\u04bd\u04bc\3\2\2\2\u04bd\u04be\3\2\2\2"+
		"\u04be\u04bf\3\2\2\2\u04bf\u04c1\5\u00c8e\2\u04c0\u04c2\5r:\2\u04c1\u04c0"+
		"\3\2\2\2\u04c1\u04c2\3\2\2\2\u04c2\u04c4\3\2\2\2\u04c3\u04bd\3\2\2\2\u04c3"+
		"\u04c4\3\2\2\2\u04c4\u0085\3\2\2\2\u04c5\u04c6\7M\2\2\u04c6\u04c7\7Z\2"+
		"\2\u04c7\u04c8\7\u00a0\2\2\u04c8\u04cc\7\u00f0\2\2\u04c9\u04ca\7N\2\2"+
		"\u04ca\u04cb\7\u00a1\2\2\u04cb\u04cd\5\66\34\2\u04cc\u04c9\3\2\2\2\u04cc"+
		"\u04cd\3\2\2\2\u04cd\u04f7\3\2\2\2\u04ce\u04cf\7M\2\2\u04cf\u04d0\7Z\2"+
		"\2\u04d0\u04da\7\u00a4\2\2\u04d1\u04d2\7\u00a5\2\2\u04d2\u04d3\7\u00a6"+
		"\2\2\u04d3\u04d4\7\26\2\2\u04d4\u04d8\7\u00f0\2\2\u04d5\u04d6\7\u00aa"+
		"\2\2\u04d6\u04d7\7\26\2\2\u04d7\u04d9\7\u00f0\2\2\u04d8\u04d5\3\2\2\2"+
		"\u04d8\u04d9\3\2\2\2\u04d9\u04db\3\2\2\2\u04da\u04d1\3\2\2\2\u04da\u04db"+
		"\3\2\2\2\u04db\u04e1\3\2\2\2\u04dc\u04dd\7\u00a7\2\2\u04dd\u04de\7\u00a8"+
		"\2\2\u04de\u04df\7\u00a6\2\2\u04df\u04e0\7\26\2\2\u04e0\u04e2\7\u00f0"+
		"\2\2\u04e1\u04dc\3\2\2\2\u04e1\u04e2\3\2\2\2\u04e2\u04e8\3\2\2\2\u04e3"+
		"\u04e4\7q\2\2\u04e4\u04e5\7\u00a9\2\2\u04e5\u04e6\7\u00a6\2\2\u04e6\u04e7"+
		"\7\26\2\2\u04e7\u04e9\7\u00f0\2\2\u04e8\u04e3\3\2\2\2\u04e8\u04e9\3\2"+
		"\2\2\u04e9\u04ee\3\2\2\2\u04ea\u04eb\7\u00ab\2\2\u04eb\u04ec\7\u00a6\2"+
		"\2\u04ec\u04ed\7\26\2\2\u04ed\u04ef\7\u00f0\2\2\u04ee\u04ea\3\2\2\2\u04ee"+
		"\u04ef\3\2\2\2\u04ef\u04f4\3\2\2\2\u04f0\u04f1\7)\2\2\u04f1\u04f2\7\u00d9"+
		"\2\2\u04f2\u04f3\7\21\2\2\u04f3\u04f5\7\u00f0\2\2\u04f4\u04f0\3\2\2\2"+
		"\u04f4\u04f5\3\2\2\2\u04f5\u04f7\3\2\2\2\u04f6\u04c5\3\2\2\2\u04f6\u04ce"+
		"\3\2\2\2\u04f7\u0087\3\2\2\2\u04f8\u04f9\5\u00c6d\2\u04f9\u04fa\7\6\2"+
		"\2\u04fa\u04fc\3\2\2\2\u04fb\u04f8\3\2\2\2\u04fb\u04fc\3\2\2\2\u04fc\u04fd"+
		"\3\2\2\2\u04fd\u04fe\5\u00c6d\2\u04fe\u0089\3\2\2\2\u04ff\u0500\5\u00c6"+
		"d\2\u0500\u0501\7\6\2\2\u0501\u0503\3\2\2\2\u0502\u04ff\3\2\2\2\u0502"+
		"\u0503\3\2\2\2\u0503\u0504\3\2\2\2\u0504\u0505\5\u00c6d\2\u0505\u008b"+
		"\3\2\2\2\u0506\u050e\5\u0090I\2\u0507\u0509\7\21\2\2\u0508\u0507\3\2\2"+
		"\2\u0508\u0509\3\2\2\2\u0509\u050c\3\2\2\2\u050a\u050d\5\u00c6d\2\u050b"+
		"\u050d\5r:\2\u050c\u050a\3\2\2\2\u050c\u050b\3\2\2\2\u050d\u050f\3\2\2"+
		"\2\u050e\u0508\3\2\2\2\u050e\u050f\3\2\2\2\u050f\u008d\3\2\2\2\u0510\u0515"+
		"\5\u008cG\2\u0511\u0512\7\4\2\2\u0512\u0514\5\u008cG\2\u0513\u0511\3\2"+
		"\2\2\u0514\u0517\3\2\2\2\u0515\u0513\3\2\2\2\u0515\u0516\3\2\2\2\u0516"+
		"\u008f\3\2\2\2\u0517\u0515\3\2\2\2\u0518\u0519\5\u0092J\2\u0519\u0091"+
		"\3\2\2\2\u051a\u051b\bJ\1\2\u051b\u051c\7\"\2\2\u051c\u0524\5\u0092J\7"+
		"\u051d\u051e\7$\2\2\u051e\u051f\7\3\2\2\u051f\u0520\5\"\22\2\u0520\u0521"+
		"\7\5\2\2\u0521\u0524\3\2\2\2\u0522\u0524\5\u0094K\2\u0523\u051a\3\2\2"+
		"\2\u0523\u051d\3\2\2\2\u0523\u0522\3\2\2\2\u0524\u052d\3\2\2\2\u0525\u0526"+
		"\f\4\2\2\u0526\u0527\7 \2\2\u0527\u052c\5\u0092J\5\u0528\u0529\f\3\2\2"+
		"\u0529\u052a\7\37\2\2\u052a\u052c\5\u0092J\4\u052b\u0525\3\2\2\2\u052b"+
		"\u0528\3\2\2\2\u052c\u052f\3\2\2\2\u052d\u052b\3\2\2\2\u052d\u052e\3\2"+
		"\2\2\u052e\u0093\3\2\2\2\u052f\u052d\3\2\2\2\u0530\u0532\5\u0098M\2\u0531"+
		"\u0533\5\u0096L\2\u0532\u0531\3\2\2\2\u0532\u0533\3\2\2\2\u0533\u0095"+
		"\3\2\2\2\u0534\u0536\7\"\2\2\u0535\u0534\3\2\2\2\u0535\u0536\3\2\2\2\u0536"+
		"\u0537\3\2\2\2\u0537\u0538\7%\2\2\u0538\u0539\5\u0098M\2\u0539\u053a\7"+
		" \2\2\u053a\u053b\5\u0098M\2\u053b\u0565\3\2\2\2\u053c\u053e\7\"\2\2\u053d"+
		"\u053c\3\2\2\2\u053d\u053e\3\2\2\2\u053e\u053f\3\2\2\2\u053f\u0540\7!"+
		"\2\2\u0540\u0541\7\3\2\2\u0541\u0546\5\u0090I\2\u0542\u0543\7\4\2\2\u0543"+
		"\u0545\5\u0090I\2\u0544\u0542\3\2\2\2\u0545\u0548\3\2\2\2\u0546\u0544"+
		"\3\2\2\2\u0546\u0547\3\2\2\2\u0547\u0549\3\2\2\2\u0548\u0546\3\2\2\2\u0549"+
		"\u054a\7\5\2\2\u054a\u0565\3\2\2\2\u054b\u054d\7\"\2\2\u054c\u054b\3\2"+
		"\2\2\u054c\u054d\3\2\2\2\u054d\u054e\3\2\2\2\u054e\u054f\7!\2\2\u054f"+
		"\u0550\7\3\2\2\u0550\u0551\5\"\22\2\u0551\u0552\7\5\2\2\u0552\u0565\3"+
		"\2\2\2\u0553\u0555\7\"\2\2\u0554\u0553\3\2\2\2\u0554\u0555\3\2\2\2\u0555"+
		"\u0556\3\2\2\2\u0556\u0557\t\7\2\2\u0557\u0565\5\u0098M\2\u0558\u055a"+
		"\7(\2\2\u0559\u055b\7\"\2\2\u055a\u0559\3\2\2\2\u055a\u055b\3\2\2\2\u055b"+
		"\u055c\3\2\2\2\u055c\u0565\7)\2\2\u055d\u055f\7(\2\2\u055e\u0560\7\"\2"+
		"\2\u055f\u055e\3\2\2\2\u055f\u0560\3\2\2\2\u0560\u0561\3\2\2\2\u0561\u0562"+
		"\7\23\2\2\u0562\u0563\7\17\2\2\u0563\u0565\5\u0098M\2\u0564\u0535\3\2"+
		"\2\2\u0564\u053d\3\2\2\2\u0564\u054c\3\2\2\2\u0564\u0554\3\2\2\2\u0564"+
		"\u0558\3\2\2\2\u0564\u055d\3\2\2\2\u0565\u0097\3\2\2\2\u0566\u0567\bM"+
		"\1\2\u0567\u056b\5\u009aN\2\u0568\u0569\t\b\2\2\u0569\u056b\5\u0098M\t"+
		"\u056a\u0566\3\2\2\2\u056a\u0568\3\2\2\2\u056b\u0581\3\2\2\2\u056c\u056d"+
		"\f\b\2\2\u056d\u056e\t\t\2\2\u056e\u0580\5\u0098M\t\u056f\u0570\f\7\2"+
		"\2\u0570\u0571\t\n\2\2\u0571\u0580\5\u0098M\b\u0572\u0573\f\6\2\2\u0573"+
		"\u0574\7\u0091\2\2\u0574\u0580\5\u0098M\7\u0575\u0576\f\5\2\2\u0576\u0577"+
		"\7\u0094\2\2\u0577\u0580\5\u0098M\6\u0578\u0579\f\4\2\2\u0579\u057a\7"+
		"\u0092\2\2\u057a\u0580\5\u0098M\5\u057b\u057c\f\3\2\2\u057c\u057d\5\u009e"+
		"P\2\u057d\u057e\5\u0098M\4\u057e\u0580\3\2\2\2\u057f\u056c\3\2\2\2\u057f"+
		"\u056f\3\2\2\2\u057f\u0572\3\2\2\2\u057f\u0575\3\2\2\2\u057f\u0578\3\2"+
		"\2\2\u057f\u057b\3\2\2\2\u0580\u0583\3\2\2\2\u0581\u057f\3\2\2\2\u0581"+
		"\u0582\3\2\2\2\u0582\u0099\3\2\2\2\u0583\u0581\3\2\2\2\u0584\u0585\bN"+
		"\1\2\u0585\u0587\7\61\2\2\u0586\u0588\5\u00b8]\2\u0587\u0586\3\2\2\2\u0588"+
		"\u0589\3\2\2\2\u0589\u0587\3\2\2\2\u0589\u058a\3\2\2\2\u058a\u058d\3\2"+
		"\2\2\u058b\u058c\7\64\2\2\u058c\u058e\5\u0090I\2\u058d\u058b\3\2\2\2\u058d"+
		"\u058e\3\2\2\2\u058e\u058f\3\2\2\2\u058f\u0590\7\65\2\2\u0590\u0601\3"+
		"\2\2\2\u0591\u0592\7\61\2\2\u0592\u0594\5\u0090I\2\u0593\u0595\5\u00b8"+
		"]\2\u0594\u0593\3\2\2\2\u0595\u0596\3\2\2\2\u0596\u0594\3\2\2\2\u0596"+
		"\u0597\3\2\2\2\u0597\u059a\3\2\2\2\u0598\u0599\7\64\2\2\u0599\u059b\5"+
		"\u0090I\2\u059a\u0598\3\2\2\2\u059a\u059b\3\2\2\2\u059b\u059c\3\2\2\2"+
		"\u059c\u059d\7\65\2\2\u059d\u0601\3\2\2\2\u059e\u059f\7^\2\2\u059f\u05a0"+
		"\7\3\2\2\u05a0\u05a1\5\u0090I\2\u05a1\u05a2\7\21\2\2\u05a2\u05a3\5\u00ae"+
		"X\2\u05a3\u05a4\7\5\2\2\u05a4\u0601\3\2\2\2\u05a5\u05a6\7r\2\2\u05a6\u05af"+
		"\7\3\2\2\u05a7\u05ac\5\u008cG\2\u05a8\u05a9\7\4\2\2\u05a9\u05ab\5\u008c"+
		"G\2\u05aa\u05a8\3\2\2\2\u05ab\u05ae\3\2\2\2\u05ac\u05aa\3\2\2\2\u05ac"+
		"\u05ad\3\2\2\2\u05ad\u05b0\3\2\2\2\u05ae\u05ac\3\2\2\2\u05af\u05a7\3\2"+
		"\2\2\u05af\u05b0\3\2\2\2\u05b0\u05b1\3\2\2\2\u05b1\u0601\7\5\2\2\u05b2"+
		"\u05b3\7J\2\2\u05b3\u05b4\7\3\2\2\u05b4\u05b7\5\u0090I\2\u05b5\u05b6\7"+
		"|\2\2\u05b6\u05b8\7,\2\2\u05b7\u05b5\3\2\2\2\u05b7\u05b8\3\2\2\2\u05b8"+
		"\u05b9\3\2\2\2\u05b9\u05ba\7\5\2\2\u05ba\u0601\3\2\2\2\u05bb\u05bc\7L"+
		"\2\2\u05bc\u05bd\7\3\2\2\u05bd\u05c0\5\u0090I\2\u05be\u05bf\7|\2\2\u05bf"+
		"\u05c1\7,\2\2\u05c0\u05be\3\2\2\2\u05c0\u05c1\3\2\2\2\u05c1\u05c2\3\2"+
		"\2\2\u05c2\u05c3\7\5\2\2\u05c3\u0601\3\2\2\2\u05c4\u05c5\7\u0081\2\2\u05c5"+
		"\u05c6\7\3\2\2\u05c6\u05c7\5\u0098M\2\u05c7\u05c8\7!\2\2\u05c8\u05c9\5"+
		"\u0098M\2\u05c9\u05ca\7\5\2\2\u05ca\u0601\3\2\2\2\u05cb\u0601\5\u009c"+
		"O\2\u05cc\u0601\7\u008c\2\2\u05cd\u05ce\5\u00c4c\2\u05ce\u05cf\7\6\2\2"+
		"\u05cf\u05d0\7\u008c\2\2\u05d0\u0601\3\2\2\2\u05d1\u05d2\7\3\2\2\u05d2"+
		"\u05d5\5\u008cG\2\u05d3\u05d4\7\4\2\2\u05d4\u05d6\5\u008cG\2\u05d5\u05d3"+
		"\3\2\2\2\u05d6\u05d7\3\2\2\2\u05d7\u05d5\3\2\2\2\u05d7\u05d8\3\2\2\2\u05d8"+
		"\u05d9\3\2\2\2\u05d9\u05da\7\5\2\2\u05da\u0601\3\2\2\2\u05db\u05dc\7\3"+
		"\2\2\u05dc\u05dd\5\"\22\2\u05dd\u05de\7\5\2\2\u05de\u0601\3\2\2\2\u05df"+
		"\u05e0\5\u00c4c\2\u05e0\u05ec\7\3\2\2\u05e1\u05e3\5d\63\2\u05e2\u05e1"+
		"\3\2\2\2\u05e2\u05e3\3\2\2\2\u05e3\u05e4\3\2\2\2\u05e4\u05e9\5\u0090I"+
		"\2\u05e5\u05e6\7\4\2\2\u05e6\u05e8\5\u0090I\2\u05e7\u05e5\3\2\2\2\u05e8"+
		"\u05eb\3\2\2\2\u05e9\u05e7\3\2\2\2\u05e9\u05ea\3\2\2\2\u05ea\u05ed\3\2"+
		"\2\2\u05eb\u05e9\3\2\2\2\u05ec\u05e2\3\2\2\2\u05ec\u05ed\3\2\2\2\u05ed"+
		"\u05ee\3\2\2\2\u05ee\u05f1\7\5\2\2\u05ef\u05f0\7B\2\2\u05f0\u05f2\5\u00be"+
		"`\2\u05f1\u05ef\3\2\2\2\u05f1\u05f2\3\2\2\2\u05f2\u0601\3\2\2\2\u05f3"+
		"\u05f4\5\u00c4c\2\u05f4\u05f5\7\3\2\2\u05f5\u05f6\t\13\2\2\u05f6\u05f7"+
		"\5\u0090I\2\u05f7\u05f8\7\17\2\2\u05f8\u05f9\5\u0090I\2\u05f9\u05fa\7"+
		"\5\2\2\u05fa\u0601\3\2\2\2\u05fb\u0601\5\u00c6d\2\u05fc\u05fd\7\3\2\2"+
		"\u05fd\u05fe\5\u0090I\2\u05fe\u05ff\7\5\2\2\u05ff\u0601\3\2\2\2\u0600"+
		"\u0584\3\2\2\2\u0600\u0591\3\2\2\2\u0600\u059e\3\2\2\2\u0600\u05a5\3\2"+
		"\2\2\u0600\u05b2\3\2\2\2\u0600\u05bb\3\2\2\2\u0600\u05c4\3\2\2\2\u0600"+
		"\u05cb\3\2\2\2\u0600\u05cc\3\2\2\2\u0600\u05cd\3\2\2\2\u0600\u05d1\3\2"+
		"\2\2\u0600\u05db\3\2\2\2\u0600\u05df\3\2\2\2\u0600\u05f3\3\2\2\2\u0600"+
		"\u05fb\3\2\2\2\u0600\u05fc\3\2\2\2\u0601\u060c\3\2\2\2\u0602\u0603\f\6"+
		"\2\2\u0603\u0604\7\t\2\2\u0604\u0605\5\u0098M\2\u0605\u0606\7\n\2\2\u0606"+
		"\u060b\3\2\2\2\u0607\u0608\f\4\2\2\u0608\u0609\7\6\2\2\u0609\u060b\5\u00c6"+
		"d\2\u060a\u0602\3\2\2\2\u060a\u0607\3\2\2\2\u060b\u060e\3\2\2\2\u060c"+
		"\u060a\3\2\2\2\u060c\u060d\3\2\2\2\u060d\u009b\3\2\2\2\u060e\u060c\3\2"+
		"\2\2\u060f\u061c\7)\2\2\u0610\u061c\5\u00a6T\2\u0611\u0612\5\u00c6d\2"+
		"\u0612\u0613\7\u00f0\2\2\u0613\u061c\3\2\2\2\u0614\u061c\5\u00ccg\2\u0615"+
		"\u061c\5\u00a4S\2\u0616\u0618\7\u00f0\2\2\u0617\u0616\3\2\2\2\u0618\u0619"+
		"\3\2\2\2\u0619\u0617\3\2\2\2\u0619\u061a\3\2\2\2\u061a\u061c\3\2\2\2\u061b"+
		"\u060f\3\2\2\2\u061b\u0610\3\2\2\2\u061b\u0611\3\2\2\2\u061b\u0614\3\2"+
		"\2\2\u061b\u0615\3\2\2\2\u061b\u0617\3\2\2\2\u061c\u009d\3\2\2\2\u061d"+
		"\u061e\t\f\2\2\u061e\u009f\3\2\2\2\u061f\u0620\t\r\2\2\u0620\u00a1\3\2"+
		"\2\2\u0621\u0622\t\16\2\2\u0622\u00a3\3\2\2\2\u0623\u0624\t\17\2\2\u0624"+
		"\u00a5\3\2\2\2\u0625\u0629\7\60\2\2\u0626\u0628\5\u00a8U\2\u0627\u0626"+
		"\3\2\2\2\u0628\u062b\3\2\2\2\u0629\u0627\3\2\2\2\u0629\u062a\3\2\2\2\u062a"+
		"\u00a7\3\2\2\2\u062b\u0629\3\2\2\2\u062c\u062d\5\u00aaV\2\u062d\u0630"+
		"\5\u00c6d\2\u062e\u062f\7k\2\2\u062f\u0631\5\u00c6d\2\u0630\u062e\3\2"+
		"\2\2\u0630\u0631\3\2\2\2\u0631\u00a9\3\2\2\2\u0632\u0634\t\20\2\2\u0633"+
		"\u0632\3\2\2\2\u0633\u0634\3\2\2\2\u0634\u0635\3\2\2\2\u0635\u0638\t\6"+
		"\2\2\u0636\u0638\7\u00f0\2\2\u0637\u0633\3\2\2\2\u0637\u0636\3\2\2\2\u0638"+
		"\u00ab\3\2\2\2\u0639\u063d\7J\2\2\u063a\u063b\7K\2\2\u063b\u063d\5\u00c6"+
		"d\2\u063c\u0639\3\2\2\2\u063c\u063a\3\2\2\2\u063d\u00ad\3\2\2\2\u063e"+
		"\u063f\7p\2\2\u063f\u0640\7\u0086\2\2\u0640\u0641\5\u00aeX\2\u0641\u0642"+
		"\7\u0088\2\2\u0642\u0661\3\2\2\2\u0643\u0644\7q\2\2\u0644\u0645\7\u0086"+
		"\2\2\u0645\u0646\5\u00aeX\2\u0646\u0647\7\4\2\2\u0647\u0648\5\u00aeX\2"+
		"\u0648\u0649\7\u0088\2\2\u0649\u0661\3\2\2\2\u064a\u0651\7r\2\2\u064b"+
		"\u064d\7\u0086\2\2\u064c\u064e\5\u00b4[\2\u064d\u064c\3\2\2\2\u064d\u064e"+
		"\3\2\2\2\u064e\u064f\3\2\2\2\u064f\u0652\7\u0088\2\2\u0650\u0652\7\u0084"+
		"\2\2\u0651\u064b\3\2\2\2\u0651\u0650\3\2\2\2\u0652\u0661\3\2\2\2\u0653"+
		"\u065e\5\u00c6d\2\u0654\u0655\7\3\2\2\u0655\u065a\7\u00f4\2\2\u0656\u0657"+
		"\7\4\2\2\u0657\u0659\7\u00f4\2\2\u0658\u0656\3\2\2\2\u0659\u065c\3\2\2"+
		"\2\u065a\u0658\3\2\2\2\u065a\u065b\3\2\2\2\u065b\u065d\3\2\2\2\u065c\u065a"+
		"\3\2\2\2\u065d\u065f\7\5\2\2\u065e\u0654\3\2\2\2\u065e\u065f\3\2\2\2\u065f"+
		"\u0661\3\2\2\2\u0660\u063e\3\2\2\2\u0660\u0643\3\2\2\2\u0660\u064a\3\2"+
		"\2\2\u0660\u0653\3\2\2\2\u0661\u00af\3\2\2\2\u0662\u0667\5\u00b2Z\2\u0663"+
		"\u0664\7\4\2\2\u0664\u0666\5\u00b2Z\2\u0665\u0663\3\2\2\2\u0666\u0669"+
		"\3\2\2\2\u0667\u0665\3\2\2\2\u0667\u0668\3\2\2\2\u0668\u00b1\3\2\2\2\u0669"+
		"\u0667\3\2\2\2\u066a\u066b\5\u00c6d\2\u066b\u066e\5\u00aeX\2\u066c\u066d"+
		"\7s\2\2\u066d\u066f\7\u00f0\2\2\u066e\u066c\3\2\2\2\u066e\u066f\3\2\2"+
		"\2\u066f\u00b3\3\2\2\2\u0670\u0675\5\u00b6\\\2\u0671\u0672\7\4\2\2\u0672"+
		"\u0674\5\u00b6\\\2\u0673\u0671\3\2\2\2\u0674\u0677\3\2\2\2\u0675\u0673"+
		"\3\2\2\2\u0675\u0676\3\2\2\2\u0676\u00b5\3\2\2\2\u0677\u0675\3\2\2\2\u0678"+
		"\u0679\5\u00c6d\2\u0679\u067a\7\13\2\2\u067a\u067d\5\u00aeX\2\u067b\u067c"+
		"\7s\2\2\u067c\u067e\7\u00f0\2\2\u067d\u067b\3\2\2\2\u067d\u067e\3\2\2"+
		"\2\u067e\u00b7\3\2\2\2\u067f\u0680\7\62\2\2\u0680\u0681\5\u0090I\2\u0681"+
		"\u0682\7\63\2\2\u0682\u0683\5\u0090I\2\u0683\u00b9\3\2\2\2\u0684\u0685"+
		"\7A\2\2\u0685\u068a\5\u00bc_\2\u0686\u0687\7\4\2\2\u0687\u0689\5\u00bc"+
		"_\2\u0688\u0686\3\2\2\2\u0689\u068c\3\2\2\2\u068a\u0688\3\2\2\2\u068a"+
		"\u068b\3\2\2\2\u068b\u00bb\3\2\2\2\u068c\u068a\3\2\2\2\u068d\u068e\5\u00c6"+
		"d\2\u068e\u068f\7\21\2\2\u068f\u0690\5\u00be`\2\u0690\u00bd\3\2\2\2\u0691"+
		"\u06bc\5\u00c6d\2\u0692\u06b5\7\3\2\2\u0693\u0694\7\u009a\2\2\u0694\u0695"+
		"\7\26\2\2\u0695\u069a\5\u0090I\2\u0696\u0697\7\4\2\2\u0697\u0699\5\u0090"+
		"I\2\u0698\u0696\3\2\2\2\u0699\u069c\3\2\2\2\u069a\u0698\3\2\2\2\u069a"+
		"\u069b\3\2\2\2\u069b\u06b6\3\2\2\2\u069c\u069a\3\2\2\2\u069d\u069e\t\21"+
		"\2\2\u069e\u069f\7\26\2\2\u069f\u06a4\5\u0090I\2\u06a0\u06a1\7\4\2\2\u06a1"+
		"\u06a3\5\u0090I\2\u06a2\u06a0\3\2\2\2\u06a3\u06a6\3\2\2\2\u06a4\u06a2"+
		"\3\2\2\2\u06a4\u06a5\3\2\2\2\u06a5\u06a8\3\2\2\2\u06a6\u06a4\3\2\2\2\u06a7"+
		"\u069d\3\2\2\2\u06a7\u06a8\3\2\2\2\u06a8\u06b3\3\2\2\2\u06a9\u06aa\t\22"+
		"\2\2\u06aa\u06ab\7\26\2\2\u06ab\u06b0\5T+\2\u06ac\u06ad\7\4\2\2\u06ad"+
		"\u06af\5T+\2\u06ae\u06ac\3\2\2\2\u06af\u06b2\3\2\2\2\u06b0\u06ae\3\2\2"+
		"\2\u06b0\u06b1\3\2\2\2\u06b1\u06b4\3\2\2\2\u06b2\u06b0\3\2\2\2\u06b3\u06a9"+
		"\3\2\2\2\u06b3\u06b4\3\2\2\2\u06b4\u06b6\3\2\2\2\u06b5\u0693\3\2\2\2\u06b5"+
		"\u06a7\3\2\2\2\u06b6\u06b8\3\2\2\2\u06b7\u06b9\5\u00c0a\2\u06b8\u06b7"+
		"\3\2\2\2\u06b8\u06b9\3\2\2\2\u06b9\u06ba\3\2\2\2\u06ba\u06bc\7\5\2\2\u06bb"+
		"\u0691\3\2\2\2\u06bb\u0692\3\2\2\2\u06bc\u00bf\3\2\2\2\u06bd\u06be\7D"+
		"\2\2\u06be\u06ce\5\u00c2b\2\u06bf\u06c0\7E\2\2\u06c0\u06ce\5\u00c2b\2"+
		"\u06c1\u06c2\7D\2\2\u06c2\u06c3\7%\2\2\u06c3\u06c4\5\u00c2b\2\u06c4\u06c5"+
		"\7 \2\2\u06c5\u06c6\5\u00c2b\2\u06c6\u06ce\3\2\2\2\u06c7\u06c8\7E\2\2"+
		"\u06c8\u06c9\7%\2\2\u06c9\u06ca\5\u00c2b\2\u06ca\u06cb\7 \2\2\u06cb\u06cc"+
		"\5\u00c2b\2\u06cc\u06ce\3\2\2\2\u06cd\u06bd\3\2\2\2\u06cd\u06bf\3\2\2"+
		"\2\u06cd\u06c1\3\2\2\2\u06cd\u06c7\3\2\2\2\u06ce\u00c1\3\2\2\2\u06cf\u06d0"+
		"\7F\2\2\u06d0\u06d7\t\23\2\2\u06d1\u06d2\7I\2\2\u06d2\u06d7\7M\2\2\u06d3"+
		"\u06d4\5\u0090I\2\u06d4\u06d5\t\23\2\2\u06d5\u06d7\3\2\2\2\u06d6\u06cf"+
		"\3\2\2\2\u06d6\u06d1\3\2\2\2\u06d6\u06d3\3\2\2\2\u06d7\u00c3\3\2\2\2\u06d8"+
		"\u06dd\5\u00c6d\2\u06d9\u06da\7\6\2\2\u06da\u06dc\5\u00c6d\2\u06db\u06d9"+
		"\3\2\2\2\u06dc\u06df\3\2\2\2\u06dd\u06db\3\2\2\2\u06dd\u06de\3\2\2\2\u06de"+
		"\u00c5\3\2\2\2\u06df\u06dd\3\2\2\2\u06e0\u06f0\5\u00c8e\2\u06e1\u06f0"+
		"\7\u00ed\2\2\u06e2\u06f0\7=\2\2\u06e3\u06f0\79\2\2\u06e4\u06f0\7:\2\2"+
		"\u06e5\u06f0\7;\2\2\u06e6\u06f0\7<\2\2\u06e7\u06f0\7>\2\2\u06e8\u06f0"+
		"\7\66\2\2\u06e9\u06f0\7\67\2\2\u06ea\u06f0\7?\2\2\u06eb\u06f0\7g\2\2\u06ec"+
		"\u06f0\7j\2\2\u06ed\u06f0\7h\2\2\u06ee\u06f0\7i\2\2\u06ef\u06e0\3\2\2"+
		"\2\u06ef\u06e1\3\2\2\2\u06ef\u06e2\3\2\2\2\u06ef\u06e3\3\2\2\2\u06ef\u06e4"+
		"\3\2\2\2\u06ef\u06e5\3\2\2\2\u06ef\u06e6\3\2\2\2\u06ef\u06e7\3\2\2\2\u06ef"+
		"\u06e8\3\2\2\2\u06ef\u06e9\3\2\2\2\u06ef\u06ea\3\2\2\2\u06ef\u06eb\3\2"+
		"\2\2\u06ef\u06ec\3\2\2\2\u06ef\u06ed\3\2\2\2\u06ef\u06ee\3\2\2\2\u06f0"+
		"\u00c7\3\2\2\2\u06f1\u06f5\7\u00f8\2\2\u06f2\u06f5\5\u00caf\2\u06f3\u06f5"+
		"\5\u00ceh\2\u06f4\u06f1\3\2\2\2\u06f4\u06f2\3\2\2\2\u06f4\u06f3\3\2\2"+
		"\2\u06f5\u00c9\3\2\2\2\u06f6\u06f7\7\u00f9\2\2\u06f7\u00cb\3\2\2\2\u06f8"+
		"\u06fa\7\u008b\2\2\u06f9\u06f8\3\2\2\2\u06f9\u06fa\3\2\2\2\u06fa\u06fb"+
		"\3\2\2\2\u06fb\u0715\7\u00f5\2\2\u06fc\u06fe\7\u008b\2\2\u06fd\u06fc\3"+
		"\2\2\2\u06fd\u06fe\3\2\2\2\u06fe\u06ff\3\2\2\2\u06ff\u0715\7\u00f4\2\2"+
		"\u0700\u0702\7\u008b\2\2\u0701\u0700\3\2\2\2\u0701\u0702\3\2\2\2\u0702"+
		"\u0703\3\2\2\2\u0703\u0715\7\u00f1\2\2\u0704\u0706\7\u008b\2\2\u0705\u0704"+
		"\3\2\2\2\u0705\u0706\3\2\2\2\u0706\u0707\3\2\2\2\u0707\u0715\7\u00f2\2"+
		"\2\u0708\u070a\7\u008b\2\2\u0709\u0708\3\2\2\2\u0709\u070a\3\2\2\2\u070a"+
		"\u070b\3\2\2\2\u070b\u0715\7\u00f3\2\2\u070c\u070e\7\u008b\2\2\u070d\u070c"+
		"\3\2\2\2\u070d\u070e\3\2\2\2\u070e\u070f\3\2\2\2\u070f\u0715\7\u00f6\2"+
		"\2\u0710\u0712\7\u008b\2\2\u0711\u0710\3\2\2\2\u0711\u0712\3\2\2\2\u0712"+
		"\u0713\3\2\2\2\u0713\u0715\7\u00f7\2\2\u0714\u06f9\3\2\2\2\u0714\u06fd"+
		"\3\2\2\2\u0714\u0701\3\2\2\2\u0714\u0705\3\2\2\2\u0714\u0709\3\2\2\2\u0714"+
		"\u070d\3\2\2\2\u0714\u0711\3\2\2\2\u0715\u00cd\3\2\2\2\u0716\u0717\t\24"+
		"\2\2\u0717\u00cf\3\2\2\2\u00e7\u00d5\u00e3\u00e5\u00e9\u00ec\u00f1\u00f4"+
		"\u00fa\u00ff\u010b\u0112\u0129\u012d\u0134\u01a8\u01b0\u01b8\u01c1\u01cd"+
		"\u01d1\u01d4\u01da\u01e4\u01f0\u01f5\u01fb\u0207\u0209\u020e\u0212\u0217"+
		"\u021c\u021f\u0224\u0228\u022d\u022f\u0233\u023c\u0244\u024b\u0252\u025b"+
		"\u0260\u026f\u0276\u0279\u0280\u0284\u028a\u0292\u029d\u02a8\u02af\u02b5"+
		"\u02bb\u02c4\u02c6\u02cf\u02d2\u02db\u02de\u02e7\u02ea\u02f3\u02f6\u02f9"+
		"\u02fe\u0300\u0303\u030e\u0313\u031e\u0322\u0326\u0332\u0335\u0339\u0343"+
		"\u0347\u0349\u034c\u0350\u0353\u0357\u035d\u0361\u0365\u036a\u036d\u036f"+
		"\u0374\u0379\u037c\u0380\u0383\u0385\u038a\u038f\u039c\u03a1\u03a9\u03af"+
		"\u03b9\u03c8\u03cd\u03d5\u03d8\u03dc\u03e1\u03ea\u03ed\u03f2\u03f9\u03fc"+
		"\u0404\u040b\u0412\u0415\u041a\u0420\u0424\u0427\u042a\u0435\u043a\u043f"+
		"\u0444\u0456\u0458\u045b\u0466\u046f\u0476\u047e\u0486\u048a\u0492\u049a"+
		"\u04a0\u04a8\u04b4\u04b7\u04bd\u04c1\u04c3\u04cc\u04d8\u04da\u04e1\u04e8"+
		"\u04ee\u04f4\u04f6\u04fb\u0502\u0508\u050c\u050e\u0515\u0523\u052b\u052d"+
		"\u0532\u0535\u053d\u0546\u054c\u0554\u055a\u055f\u0564\u056a\u057f\u0581"+
		"\u0589\u058d\u0596\u059a\u05ac\u05af\u05b7\u05c0\u05d7\u05e2\u05e9\u05ec"+
		"\u05f1\u0600\u060a\u060c\u0619\u061b\u0629\u0630\u0633\u0637\u063c\u064d"+
		"\u0651\u065a\u065e\u0660\u0667\u066e\u0675\u067d\u068a\u069a\u06a4\u06a7"+
		"\u06b0\u06b3\u06b5\u06b8\u06bb\u06cd\u06d6\u06dd\u06ef\u06f4\u06f9\u06fd"+
		"\u0701\u0705\u0709\u070d\u0711\u0714";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}