// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.flink.antlr4;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class FlinkCdcSqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEMICOLON=1, LEFT_PAREN=2, RIGHT_PAREN=3, COMMA=4, DOT=5, LEFT_BRACKET=6, 
		RIGHT_BRACKET=7, ADD=8, AFTER=9, ALL=10, AS=11, BEGIN=12, BY=13, CAST=14, 
		COLUMN=15, COMMENT=16, CREATE=17, DATABASE=18, DISTINCT=19, DIV=20, END=21, 
		EXCLUDING=22, EXISTS=23, FALSE=24, FIRST=25, IF=26, INCLUDING=27, KAFKA=28, 
		NOT=29, NULL=30, OPTIONS=31, PARTITIONED=32, SET=33, STATEMENT=34, TABLE=35, 
		TABLES=36, TRUE=37, WITH=38, EQ=39, NSEQ=40, NEQ=41, NEQJ=42, LT=43, LTE=44, 
		GT=45, GTE=46, PLUS=47, MINUS=48, ASTERISK=49, SLASH=50, PERCENT=51, TILDE=52, 
		AMPERSAND=53, PIPE=54, CONCAT_PIPE=55, HAT=56, COLON=57, ARROW=58, HENT_START=59, 
		HENT_END=60, STRING=61, BIGINT_LITERAL=62, SMALLINT_LITERAL=63, TINYINT_LITERAL=64, 
		INTEGER_VALUE=65, EXPONENT_VALUE=66, DECIMAL_VALUE=67, FLOAT_LITERAL=68, 
		DOUBLE_LITERAL=69, BIGDECIMAL_LITERAL=70, IDENTIFIER=71, BACKQUOTED_IDENTIFIER=72, 
		SIMPLE_COMMENT=73, BRACKETED_COMMENT=74, WS=75, UNRECOGNIZED=76;
	public static final int
		RULE_singleStatement = 0, RULE_statement = 1, RULE_multipartIdentifier = 2, 
		RULE_identifierList = 3, RULE_identifierSeq = 4, RULE_stringLit = 5, RULE_commentSpec = 6, 
		RULE_propertyList = 7, RULE_property = 8, RULE_propertyKey = 9, RULE_propertyValue = 10, 
		RULE_computeColList = 11, RULE_computeColDef = 12, RULE_expression = 13, 
		RULE_valueExpression = 14, RULE_primaryExpression = 15, RULE_functionName = 16, 
		RULE_setQuantifier = 17, RULE_identifier = 18, RULE_quotedIdentifier = 19, 
		RULE_constant = 20, RULE_number = 21, RULE_comparisonOperator = 22, RULE_arithmeticOperator = 23, 
		RULE_booleanValue = 24;
	private static String[] makeRuleNames() {
		return new String[] {
			"singleStatement", "statement", "multipartIdentifier", "identifierList", 
			"identifierSeq", "stringLit", "commentSpec", "propertyList", "property", 
			"propertyKey", "propertyValue", "computeColList", "computeColDef", "expression", 
			"valueExpression", "primaryExpression", "functionName", "setQuantifier", 
			"identifier", "quotedIdentifier", "constant", "number", "comparisonOperator", 
			"arithmeticOperator", "booleanValue"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'('", "')'", "','", "'.'", "'['", "']'", "'ADD'", "'AFTER'", 
			"'ALL'", "'AS'", "'BEGIN'", "'BY'", "'CAST'", "'COLUMN'", "'COMMENT'", 
			"'CREATE'", "'DATABASE'", "'DISTINCT'", "'DIV'", "'END'", "'EXCLUDING'", 
			"'EXISTS'", "'FALSE'", "'FIRST'", "'IF'", "'INCLUDING'", "'KAFKA'", "'NOT'", 
			"'NULL'", "'OPTIONS'", "'PARTITIONED'", "'SET'", "'STATEMENT'", "'TABLE'", 
			"'TABLES'", "'TRUE'", "'WITH'", null, "'<=>'", "'<>'", "'!='", "'<'", 
			null, "'>'", null, "'+'", "'-'", "'*'", "'/'", "'%'", "'~'", "'&'", "'|'", 
			"'||'", "'^'", "':'", "'->'", "'/*+'", "'*/'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEMICOLON", "LEFT_PAREN", "RIGHT_PAREN", "COMMA", "DOT", "LEFT_BRACKET", 
			"RIGHT_BRACKET", "ADD", "AFTER", "ALL", "AS", "BEGIN", "BY", "CAST", 
			"COLUMN", "COMMENT", "CREATE", "DATABASE", "DISTINCT", "DIV", "END", 
			"EXCLUDING", "EXISTS", "FALSE", "FIRST", "IF", "INCLUDING", "KAFKA", 
			"NOT", "NULL", "OPTIONS", "PARTITIONED", "SET", "STATEMENT", "TABLE", 
			"TABLES", "TRUE", "WITH", "EQ", "NSEQ", "NEQ", "NEQJ", "LT", "LTE", "GT", 
			"GTE", "PLUS", "MINUS", "ASTERISK", "SLASH", "PERCENT", "TILDE", "AMPERSAND", 
			"PIPE", "CONCAT_PIPE", "HAT", "COLON", "ARROW", "HENT_START", "HENT_END", 
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
	public String getGrammarFileName() { return "java-escape"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public FlinkCdcSqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingleStatementContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode EOF() { return getToken(FlinkCdcSqlParser.EOF, 0); }
		public List<TerminalNode> SEMICOLON() { return getTokens(FlinkCdcSqlParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(FlinkCdcSqlParser.SEMICOLON, i);
		}
		public SingleStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterSingleStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitSingleStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitSingleStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleStatementContext singleStatement() throws RecognitionException {
		SingleStatementContext _localctx = new SingleStatementContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_singleStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(50);
			statement();
			setState(54);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMICOLON) {
				{
				{
				setState(51);
				match(SEMICOLON);
				}
				}
				setState(56);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(57);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class EndStatementContext extends StatementContext {
		public TerminalNode END() { return getToken(FlinkCdcSqlParser.END, 0); }
		public EndStatementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterEndStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitEndStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitEndStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BeginStatementContext extends StatementContext {
		public TerminalNode BEGIN() { return getToken(FlinkCdcSqlParser.BEGIN, 0); }
		public TerminalNode STATEMENT() { return getToken(FlinkCdcSqlParser.STATEMENT, 0); }
		public TerminalNode SET() { return getToken(FlinkCdcSqlParser.SET, 0); }
		public BeginStatementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterBeginStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitBeginStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitBeginStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CreateDatabaseContext extends StatementContext {
		public MultipartIdentifierContext sink;
		public PropertyListContext sinkOptions;
		public MultipartIdentifierContext source;
		public Token includeTable;
		public Token excludeTable;
		public PropertyListContext sourceOptions;
		public TerminalNode CREATE() { return getToken(FlinkCdcSqlParser.CREATE, 0); }
		public List<TerminalNode> DATABASE() { return getTokens(FlinkCdcSqlParser.DATABASE); }
		public TerminalNode DATABASE(int i) {
			return getToken(FlinkCdcSqlParser.DATABASE, i);
		}
		public TerminalNode IF() { return getToken(FlinkCdcSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(FlinkCdcSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(FlinkCdcSqlParser.EXISTS, 0); }
		public TerminalNode AS() { return getToken(FlinkCdcSqlParser.AS, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public TerminalNode KAFKA() { return getToken(FlinkCdcSqlParser.KAFKA, 0); }
		public TerminalNode PARTITIONED() { return getToken(FlinkCdcSqlParser.PARTITIONED, 0); }
		public TerminalNode BY() { return getToken(FlinkCdcSqlParser.BY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public TerminalNode WITH() { return getToken(FlinkCdcSqlParser.WITH, 0); }
		public TerminalNode INCLUDING() { return getToken(FlinkCdcSqlParser.INCLUDING, 0); }
		public TerminalNode EXCLUDING() { return getToken(FlinkCdcSqlParser.EXCLUDING, 0); }
		public List<TerminalNode> TABLE() { return getTokens(FlinkCdcSqlParser.TABLE); }
		public TerminalNode TABLE(int i) {
			return getToken(FlinkCdcSqlParser.TABLE, i);
		}
		public TerminalNode OPTIONS() { return getToken(FlinkCdcSqlParser.OPTIONS, 0); }
		public List<PropertyListContext> propertyList() {
			return getRuleContexts(PropertyListContext.class);
		}
		public PropertyListContext propertyList(int i) {
			return getRuleContext(PropertyListContext.class,i);
		}
		public List<TerminalNode> STRING() { return getTokens(FlinkCdcSqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(FlinkCdcSqlParser.STRING, i);
		}
		public TerminalNode ALL() { return getToken(FlinkCdcSqlParser.ALL, 0); }
		public TerminalNode TABLES() { return getToken(FlinkCdcSqlParser.TABLES, 0); }
		public CreateDatabaseContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterCreateDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitCreateDatabase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitCreateDatabase(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CreateTableContext extends StatementContext {
		public MultipartIdentifierContext sink;
		public PropertyListContext sinkOptions;
		public MultipartIdentifierContext source;
		public PropertyListContext sourceOptions;
		public TerminalNode CREATE() { return getToken(FlinkCdcSqlParser.CREATE, 0); }
		public List<TerminalNode> TABLE() { return getTokens(FlinkCdcSqlParser.TABLE); }
		public TerminalNode TABLE(int i) {
			return getToken(FlinkCdcSqlParser.TABLE, i);
		}
		public TerminalNode IF() { return getToken(FlinkCdcSqlParser.IF, 0); }
		public TerminalNode NOT() { return getToken(FlinkCdcSqlParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(FlinkCdcSqlParser.EXISTS, 0); }
		public TerminalNode AS() { return getToken(FlinkCdcSqlParser.AS, 0); }
		public List<MultipartIdentifierContext> multipartIdentifier() {
			return getRuleContexts(MultipartIdentifierContext.class);
		}
		public MultipartIdentifierContext multipartIdentifier(int i) {
			return getRuleContext(MultipartIdentifierContext.class,i);
		}
		public TerminalNode PARTITIONED() { return getToken(FlinkCdcSqlParser.PARTITIONED, 0); }
		public TerminalNode BY() { return getToken(FlinkCdcSqlParser.BY, 0); }
		public IdentifierListContext identifierList() {
			return getRuleContext(IdentifierListContext.class,0);
		}
		public CommentSpecContext commentSpec() {
			return getRuleContext(CommentSpecContext.class,0);
		}
		public TerminalNode WITH() { return getToken(FlinkCdcSqlParser.WITH, 0); }
		public TerminalNode OPTIONS() { return getToken(FlinkCdcSqlParser.OPTIONS, 0); }
		public TerminalNode ADD() { return getToken(FlinkCdcSqlParser.ADD, 0); }
		public TerminalNode COLUMN() { return getToken(FlinkCdcSqlParser.COLUMN, 0); }
		public TerminalNode LEFT_PAREN() { return getToken(FlinkCdcSqlParser.LEFT_PAREN, 0); }
		public ComputeColListContext computeColList() {
			return getRuleContext(ComputeColListContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(FlinkCdcSqlParser.RIGHT_PAREN, 0); }
		public List<PropertyListContext> propertyList() {
			return getRuleContexts(PropertyListContext.class);
		}
		public PropertyListContext propertyList(int i) {
			return getRuleContext(PropertyListContext.class,i);
		}
		public CreateTableContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterCreateTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitCreateTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitCreateTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		int _la;
		try {
			setState(135);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				_localctx = new BeginStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(59);
				match(BEGIN);
				setState(60);
				match(STATEMENT);
				setState(61);
				match(SET);
				}
				break;
			case 2:
				_localctx = new EndStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(62);
				match(END);
				}
				break;
			case 3:
				_localctx = new CreateTableContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(63);
				match(CREATE);
				setState(64);
				match(TABLE);
				setState(65);
				match(IF);
				setState(66);
				match(NOT);
				setState(67);
				match(EXISTS);
				setState(68);
				((CreateTableContext)_localctx).sink = multipartIdentifier();
				setState(72);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITIONED) {
					{
					setState(69);
					match(PARTITIONED);
					setState(70);
					match(BY);
					setState(71);
					identifierList();
					}
				}

				setState(75);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMENT) {
					{
					setState(74);
					commentSpec();
					}
				}

				setState(79);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WITH) {
					{
					setState(77);
					match(WITH);
					setState(78);
					((CreateTableContext)_localctx).sinkOptions = propertyList();
					}
				}

				setState(81);
				match(AS);
				setState(82);
				match(TABLE);
				setState(83);
				((CreateTableContext)_localctx).source = multipartIdentifier();
				setState(86);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(84);
					match(OPTIONS);
					setState(85);
					((CreateTableContext)_localctx).sourceOptions = propertyList();
					}
				}

				setState(94);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ADD) {
					{
					setState(88);
					match(ADD);
					setState(89);
					match(COLUMN);
					setState(90);
					match(LEFT_PAREN);
					setState(91);
					computeColList();
					setState(92);
					match(RIGHT_PAREN);
					}
				}

				}
				break;
			case 4:
				_localctx = new CreateDatabaseContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(96);
				match(CREATE);
				setState(97);
				match(DATABASE);
				setState(98);
				match(IF);
				setState(99);
				match(NOT);
				setState(100);
				match(EXISTS);
				setState(101);
				((CreateDatabaseContext)_localctx).sink = multipartIdentifier();
				setState(105);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PARTITIONED) {
					{
					setState(102);
					match(PARTITIONED);
					setState(103);
					match(BY);
					setState(104);
					identifierList();
					}
				}

				setState(108);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMENT) {
					{
					setState(107);
					commentSpec();
					}
				}

				setState(112);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WITH) {
					{
					setState(110);
					match(WITH);
					setState(111);
					((CreateDatabaseContext)_localctx).sinkOptions = propertyList();
					}
				}

				setState(114);
				match(AS);
				setState(115);
				_la = _input.LA(1);
				if ( !(_la==DATABASE || _la==KAFKA) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(116);
				((CreateDatabaseContext)_localctx).source = multipartIdentifier();
				setState(124);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INCLUDING) {
					{
					setState(117);
					match(INCLUDING);
					setState(122);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case ALL:
						{
						setState(118);
						match(ALL);
						setState(119);
						match(TABLES);
						}
						break;
					case TABLE:
						{
						setState(120);
						match(TABLE);
						setState(121);
						((CreateDatabaseContext)_localctx).includeTable = match(STRING);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
				}

				setState(129);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXCLUDING) {
					{
					setState(126);
					match(EXCLUDING);
					setState(127);
					match(TABLE);
					setState(128);
					((CreateDatabaseContext)_localctx).excludeTable = match(STRING);
					}
				}

				setState(133);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPTIONS) {
					{
					setState(131);
					match(OPTIONS);
					setState(132);
					((CreateDatabaseContext)_localctx).sourceOptions = propertyList();
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

	@SuppressWarnings("CheckReturnValue")
	public static class MultipartIdentifierContext extends ParserRuleContext {
		public IdentifierContext identifier;
		public List<IdentifierContext> parts = new ArrayList<IdentifierContext>();
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(FlinkCdcSqlParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(FlinkCdcSqlParser.DOT, i);
		}
		public MultipartIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multipartIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterMultipartIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitMultipartIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitMultipartIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultipartIdentifierContext multipartIdentifier() throws RecognitionException {
		MultipartIdentifierContext _localctx = new MultipartIdentifierContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_multipartIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(137);
			((MultipartIdentifierContext)_localctx).identifier = identifier();
			((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).identifier);
			setState(142);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(138);
				match(DOT);
				setState(139);
				((MultipartIdentifierContext)_localctx).identifier = identifier();
				((MultipartIdentifierContext)_localctx).parts.add(((MultipartIdentifierContext)_localctx).identifier);
				}
				}
				setState(144);
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierListContext extends ParserRuleContext {
		public TerminalNode LEFT_PAREN() { return getToken(FlinkCdcSqlParser.LEFT_PAREN, 0); }
		public IdentifierSeqContext identifierSeq() {
			return getRuleContext(IdentifierSeqContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(FlinkCdcSqlParser.RIGHT_PAREN, 0); }
		public IdentifierListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterIdentifierList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitIdentifierList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitIdentifierList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierListContext identifierList() throws RecognitionException {
		IdentifierListContext _localctx = new IdentifierListContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_identifierList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			match(LEFT_PAREN);
			setState(146);
			identifierSeq();
			setState(147);
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierSeqContext extends ParserRuleContext {
		public IdentifierContext identifier;
		public List<IdentifierContext> ident = new ArrayList<IdentifierContext>();
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(FlinkCdcSqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(FlinkCdcSqlParser.COMMA, i);
		}
		public IdentifierSeqContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierSeq; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterIdentifierSeq(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitIdentifierSeq(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitIdentifierSeq(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierSeqContext identifierSeq() throws RecognitionException {
		IdentifierSeqContext _localctx = new IdentifierSeqContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_identifierSeq);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			((IdentifierSeqContext)_localctx).identifier = identifier();
			((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).identifier);
			setState(154);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(150);
				match(COMMA);
				setState(151);
				((IdentifierSeqContext)_localctx).identifier = identifier();
				((IdentifierSeqContext)_localctx).ident.add(((IdentifierSeqContext)_localctx).identifier);
				}
				}
				setState(156);
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

	@SuppressWarnings("CheckReturnValue")
	public static class StringLitContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(FlinkCdcSqlParser.STRING, 0); }
		public StringLitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterStringLit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitStringLit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitStringLit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringLitContext stringLit() throws RecognitionException {
		StringLitContext _localctx = new StringLitContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_stringLit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(157);
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

	@SuppressWarnings("CheckReturnValue")
	public static class CommentSpecContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(FlinkCdcSqlParser.COMMENT, 0); }
		public TerminalNode STRING() { return getToken(FlinkCdcSqlParser.STRING, 0); }
		public CommentSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commentSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterCommentSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitCommentSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitCommentSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommentSpecContext commentSpec() throws RecognitionException {
		CommentSpecContext _localctx = new CommentSpecContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_commentSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(159);
			match(COMMENT);
			setState(160);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyListContext extends ParserRuleContext {
		public TerminalNode LEFT_PAREN() { return getToken(FlinkCdcSqlParser.LEFT_PAREN, 0); }
		public List<PropertyContext> property() {
			return getRuleContexts(PropertyContext.class);
		}
		public PropertyContext property(int i) {
			return getRuleContext(PropertyContext.class,i);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(FlinkCdcSqlParser.RIGHT_PAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(FlinkCdcSqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(FlinkCdcSqlParser.COMMA, i);
		}
		public PropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterPropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitPropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitPropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyListContext propertyList() throws RecognitionException {
		PropertyListContext _localctx = new PropertyListContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_propertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(LEFT_PAREN);
			setState(163);
			property();
			setState(168);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(164);
				match(COMMA);
				setState(165);
				property();
				}
				}
				setState(170);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(171);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyContext extends ParserRuleContext {
		public PropertyKeyContext key;
		public PropertyValueContext value;
		public PropertyKeyContext propertyKey() {
			return getRuleContext(PropertyKeyContext.class,0);
		}
		public PropertyValueContext propertyValue() {
			return getRuleContext(PropertyValueContext.class,0);
		}
		public TerminalNode EQ() { return getToken(FlinkCdcSqlParser.EQ, 0); }
		public PropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_property; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyContext property() throws RecognitionException {
		PropertyContext _localctx = new PropertyContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_property);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			((PropertyContext)_localctx).key = propertyKey();
			setState(178);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & 11132555272193L) != 0) {
				{
				setState(175);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(174);
					match(EQ);
					}
				}

				setState(177);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyKeyContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(FlinkCdcSqlParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(FlinkCdcSqlParser.DOT, i);
		}
		public TerminalNode STRING() { return getToken(FlinkCdcSqlParser.STRING, 0); }
		public PropertyKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterPropertyKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitPropertyKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitPropertyKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyKeyContext propertyKey() throws RecognitionException {
		PropertyKeyContext _localctx = new PropertyKeyContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_propertyKey);
		int _la;
		try {
			setState(189);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENTIFIER:
			case BACKQUOTED_IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(180);
				identifier();
				setState(185);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(181);
					match(DOT);
					setState(182);
					identifier();
					}
					}
					setState(187);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(188);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyValueContext extends ParserRuleContext {
		public TerminalNode INTEGER_VALUE() { return getToken(FlinkCdcSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(FlinkCdcSqlParser.DECIMAL_VALUE, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode STRING() { return getToken(FlinkCdcSqlParser.STRING, 0); }
		public PropertyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterPropertyValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitPropertyValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitPropertyValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyValueContext propertyValue() throws RecognitionException {
		PropertyValueContext _localctx = new PropertyValueContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_propertyValue);
		try {
			setState(195);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(191);
				match(INTEGER_VALUE);
				}
				break;
			case DECIMAL_VALUE:
				enterOuterAlt(_localctx, 2);
				{
				setState(192);
				match(DECIMAL_VALUE);
				}
				break;
			case FALSE:
			case TRUE:
				enterOuterAlt(_localctx, 3);
				{
				setState(193);
				booleanValue();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(194);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComputeColListContext extends ParserRuleContext {
		public List<ComputeColDefContext> computeColDef() {
			return getRuleContexts(ComputeColDefContext.class);
		}
		public ComputeColDefContext computeColDef(int i) {
			return getRuleContext(ComputeColDefContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(FlinkCdcSqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(FlinkCdcSqlParser.COMMA, i);
		}
		public ComputeColListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_computeColList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterComputeColList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitComputeColList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitComputeColList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComputeColListContext computeColList() throws RecognitionException {
		ComputeColListContext _localctx = new ComputeColListContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_computeColList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(197);
			computeColDef();
			setState(202);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(198);
				match(COMMA);
				setState(199);
				computeColDef();
				}
				}
				setState(204);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComputeColDefContext extends ParserRuleContext {
		public IdentifierContext colName;
		public IdentifierContext name;
		public TerminalNode AS() { return getToken(FlinkCdcSqlParser.AS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode FIRST() { return getToken(FlinkCdcSqlParser.FIRST, 0); }
		public TerminalNode AFTER() { return getToken(FlinkCdcSqlParser.AFTER, 0); }
		public ComputeColDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_computeColDef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterComputeColDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitComputeColDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitComputeColDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComputeColDefContext computeColDef() throws RecognitionException {
		ComputeColDefContext _localctx = new ComputeColDefContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_computeColDef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(205);
			((ComputeColDefContext)_localctx).colName = identifier();
			setState(206);
			match(AS);
			setState(207);
			expression();
			setState(211);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FIRST:
				{
				setState(208);
				match(FIRST);
				}
				break;
			case AFTER:
				{
				setState(209);
				match(AFTER);
				setState(210);
				((ComputeColDefContext)_localctx).name = identifier();
				}
				break;
			case RIGHT_PAREN:
			case COMMA:
				break;
			default:
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(213);
			valueExpression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class ValueExpressionDefaultContext extends ValueExpressionContext {
		public PrimaryExpressionContext primaryExpression() {
			return getRuleContext(PrimaryExpressionContext.class,0);
		}
		public ValueExpressionDefaultContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterValueExpressionDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitValueExpressionDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitValueExpressionDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
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
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitComparison(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitComparison(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
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
		public TerminalNode ASTERISK() { return getToken(FlinkCdcSqlParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(FlinkCdcSqlParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(FlinkCdcSqlParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(FlinkCdcSqlParser.DIV, 0); }
		public TerminalNode PLUS() { return getToken(FlinkCdcSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(FlinkCdcSqlParser.CONCAT_PIPE, 0); }
		public TerminalNode AMPERSAND() { return getToken(FlinkCdcSqlParser.AMPERSAND, 0); }
		public TerminalNode HAT() { return getToken(FlinkCdcSqlParser.HAT, 0); }
		public TerminalNode PIPE() { return getToken(FlinkCdcSqlParser.PIPE, 0); }
		public ArithmeticBinaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterArithmeticBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitArithmeticBinary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitArithmeticBinary(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ArithmeticUnaryContext extends ValueExpressionContext {
		public Token operator;
		public ValueExpressionContext valueExpression() {
			return getRuleContext(ValueExpressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(FlinkCdcSqlParser.PLUS, 0); }
		public TerminalNode TILDE() { return getToken(FlinkCdcSqlParser.TILDE, 0); }
		public ArithmeticUnaryContext(ValueExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterArithmeticUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitArithmeticUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitArithmeticUnary(this);
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
		int _startState = 28;
		enterRecursionRule(_localctx, 28, RULE_valueExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				_localctx = new ValueExpressionDefaultContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(216);
				primaryExpression();
				}
				break;
			case 2:
				{
				_localctx = new ArithmeticUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(217);
				((ArithmeticUnaryContext)_localctx).operator = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 4925812092436480L) != 0) ) {
					((ArithmeticUnaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(218);
				valueExpression(7);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(242);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(240);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
					case 1:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(221);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(222);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 3940649674997760L) != 0) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(223);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(7);
						}
						break;
					case 2:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(224);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(225);
						((ArithmeticBinaryContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 36451009484029952L) != 0) ) {
							((ArithmeticBinaryContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(226);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(6);
						}
						break;
					case 3:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(227);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(228);
						((ArithmeticBinaryContext)_localctx).operator = match(AMPERSAND);
						setState(229);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(5);
						}
						break;
					case 4:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(230);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(231);
						((ArithmeticBinaryContext)_localctx).operator = match(HAT);
						setState(232);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(4);
						}
						break;
					case 5:
						{
						_localctx = new ArithmeticBinaryContext(new ValueExpressionContext(_parentctx, _parentState));
						((ArithmeticBinaryContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(233);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(234);
						((ArithmeticBinaryContext)_localctx).operator = match(PIPE);
						setState(235);
						((ArithmeticBinaryContext)_localctx).right = valueExpression(3);
						}
						break;
					case 6:
						{
						_localctx = new ComparisonContext(new ValueExpressionContext(_parentctx, _parentState));
						((ComparisonContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_valueExpression);
						setState(236);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(237);
						comparisonOperator();
						setState(238);
						((ComparisonContext)_localctx).right = valueExpression(2);
						}
						break;
					}
					} 
				}
				setState(244);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class CastContext extends PrimaryExpressionContext {
		public Token name;
		public TerminalNode LEFT_PAREN() { return getToken(FlinkCdcSqlParser.LEFT_PAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(FlinkCdcSqlParser.AS, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode RIGHT_PAREN() { return getToken(FlinkCdcSqlParser.RIGHT_PAREN, 0); }
		public TerminalNode CAST() { return getToken(FlinkCdcSqlParser.CAST, 0); }
		public CastContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitCast(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ColumnReferenceContext extends PrimaryExpressionContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ColumnReferenceContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterColumnReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitColumnReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitColumnReference(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstantDefaultContext extends PrimaryExpressionContext {
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public ConstantDefaultContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterConstantDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitConstantDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitConstantDefault(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionCallContext extends PrimaryExpressionContext {
		public ExpressionContext expression;
		public List<ExpressionContext> argument = new ArrayList<ExpressionContext>();
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public TerminalNode LEFT_PAREN() { return getToken(FlinkCdcSqlParser.LEFT_PAREN, 0); }
		public TerminalNode RIGHT_PAREN() { return getToken(FlinkCdcSqlParser.RIGHT_PAREN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public SetQuantifierContext setQuantifier() {
			return getRuleContext(SetQuantifierContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(FlinkCdcSqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(FlinkCdcSqlParser.COMMA, i);
		}
		public FunctionCallContext(PrimaryExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExpressionContext primaryExpression() throws RecognitionException {
		PrimaryExpressionContext _localctx = new PrimaryExpressionContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_primaryExpression);
		int _la;
		try {
			setState(271);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				_localctx = new ColumnReferenceContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(245);
				identifier();
				}
				break;
			case 2:
				_localctx = new ConstantDefaultContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(246);
				constant();
				}
				break;
			case 3:
				_localctx = new CastContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(247);
				((CastContext)_localctx).name = match(CAST);
				setState(248);
				match(LEFT_PAREN);
				setState(249);
				expression();
				setState(250);
				match(AS);
				setState(251);
				identifier();
				setState(252);
				match(RIGHT_PAREN);
				}
				break;
			case 4:
				_localctx = new FunctionCallContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(254);
				functionName();
				setState(255);
				match(LEFT_PAREN);
				setState(267);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 9221265785028100625L) != 0) {
					{
					setState(257);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ALL || _la==DISTINCT) {
						{
						setState(256);
						setQuantifier();
						}
					}

					setState(259);
					((FunctionCallContext)_localctx).expression = expression();
					((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
					setState(264);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(260);
						match(COMMA);
						setState(261);
						((FunctionCallContext)_localctx).expression = expression();
						((FunctionCallContext)_localctx).argument.add(((FunctionCallContext)_localctx).expression);
						}
						}
						setState(266);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(269);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionNameContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(FlinkCdcSqlParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(FlinkCdcSqlParser.DOT, i);
		}
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitFunctionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitFunctionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_functionName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			identifier();
			setState(278);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(274);
				match(DOT);
				setState(275);
				identifier();
				}
				}
				setState(280);
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

	@SuppressWarnings("CheckReturnValue")
	public static class SetQuantifierContext extends ParserRuleContext {
		public TerminalNode DISTINCT() { return getToken(FlinkCdcSqlParser.DISTINCT, 0); }
		public TerminalNode ALL() { return getToken(FlinkCdcSqlParser.ALL, 0); }
		public SetQuantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setQuantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterSetQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitSetQuantifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitSetQuantifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetQuantifierContext setQuantifier() throws RecognitionException {
		SetQuantifierContext _localctx = new SetQuantifierContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_setQuantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierContext extends ParserRuleContext {
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
	 
		public IdentifierContext() { }
		public void copyFrom(IdentifierContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class QuotedIdentifierAlternativeContext extends IdentifierContext {
		public QuotedIdentifierContext quotedIdentifier() {
			return getRuleContext(QuotedIdentifierContext.class,0);
		}
		public QuotedIdentifierAlternativeContext(IdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterQuotedIdentifierAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitQuotedIdentifierAlternative(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitQuotedIdentifierAlternative(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UnquotedIdentifierContext extends IdentifierContext {
		public TerminalNode IDENTIFIER() { return getToken(FlinkCdcSqlParser.IDENTIFIER, 0); }
		public UnquotedIdentifierContext(IdentifierContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterUnquotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitUnquotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitUnquotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_identifier);
		try {
			setState(285);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENTIFIER:
				_localctx = new UnquotedIdentifierContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(283);
				match(IDENTIFIER);
				}
				break;
			case BACKQUOTED_IDENTIFIER:
				_localctx = new QuotedIdentifierAlternativeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(284);
				quotedIdentifier();
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

	@SuppressWarnings("CheckReturnValue")
	public static class QuotedIdentifierContext extends ParserRuleContext {
		public TerminalNode BACKQUOTED_IDENTIFIER() { return getToken(FlinkCdcSqlParser.BACKQUOTED_IDENTIFIER, 0); }
		public QuotedIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quotedIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterQuotedIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitQuotedIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitQuotedIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuotedIdentifierContext quotedIdentifier() throws RecognitionException {
		QuotedIdentifierContext _localctx = new QuotedIdentifierContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_quotedIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(287);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class NullLiteralContext extends ConstantContext {
		public TerminalNode NULL() { return getToken(FlinkCdcSqlParser.NULL, 0); }
		public NullLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterNullLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitNullLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitNullLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StringLiteralContext extends ConstantContext {
		public List<StringLitContext> stringLit() {
			return getRuleContexts(StringLitContext.class);
		}
		public StringLitContext stringLit(int i) {
			return getRuleContext(StringLitContext.class,i);
		}
		public StringLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeConstructorContext extends ConstantContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public StringLitContext stringLit() {
			return getRuleContext(StringLitContext.class,0);
		}
		public TypeConstructorContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterTypeConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitTypeConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitTypeConstructor(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParameterLiteralContext extends ConstantContext {
		public TerminalNode COLON() { return getToken(FlinkCdcSqlParser.COLON, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ParameterLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterParameterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitParameterLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitParameterLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NumericLiteralContext extends ConstantContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public NumericLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitNumericLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BooleanLiteralContext extends ConstantContext {
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public BooleanLiteralContext(ConstantContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_constant);
		try {
			int _alt;
			setState(302);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NULL:
				_localctx = new NullLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(289);
				match(NULL);
				}
				break;
			case COLON:
				_localctx = new ParameterLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(290);
				match(COLON);
				setState(291);
				identifier();
				}
				break;
			case IDENTIFIER:
			case BACKQUOTED_IDENTIFIER:
				_localctx = new TypeConstructorContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(292);
				identifier();
				setState(293);
				stringLit();
				}
				break;
			case MINUS:
			case BIGINT_LITERAL:
			case SMALLINT_LITERAL:
			case TINYINT_LITERAL:
			case INTEGER_VALUE:
			case EXPONENT_VALUE:
			case DECIMAL_VALUE:
			case FLOAT_LITERAL:
			case DOUBLE_LITERAL:
			case BIGDECIMAL_LITERAL:
				_localctx = new NumericLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(295);
				number();
				}
				break;
			case FALSE:
			case TRUE:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(296);
				booleanValue();
				}
				break;
			case STRING:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(298); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(297);
						stringLit();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(300); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class BigIntLiteralContext extends NumberContext {
		public TerminalNode BIGINT_LITERAL() { return getToken(FlinkCdcSqlParser.BIGINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public BigIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterBigIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitBigIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitBigIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TinyIntLiteralContext extends NumberContext {
		public TerminalNode TINYINT_LITERAL() { return getToken(FlinkCdcSqlParser.TINYINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public TinyIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterTinyIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitTinyIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitTinyIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LegacyDecimalLiteralContext extends NumberContext {
		public TerminalNode EXPONENT_VALUE() { return getToken(FlinkCdcSqlParser.EXPONENT_VALUE, 0); }
		public TerminalNode DECIMAL_VALUE() { return getToken(FlinkCdcSqlParser.DECIMAL_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public LegacyDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterLegacyDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitLegacyDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitLegacyDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BigDecimalLiteralContext extends NumberContext {
		public TerminalNode BIGDECIMAL_LITERAL() { return getToken(FlinkCdcSqlParser.BIGDECIMAL_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public BigDecimalLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterBigDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitBigDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitBigDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DoubleLiteralContext extends NumberContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(FlinkCdcSqlParser.DOUBLE_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public DoubleLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitDoubleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitDoubleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IntegerLiteralContext extends NumberContext {
		public TerminalNode INTEGER_VALUE() { return getToken(FlinkCdcSqlParser.INTEGER_VALUE, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public IntegerLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitIntegerLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitIntegerLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FloatLiteralContext extends NumberContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(FlinkCdcSqlParser.FLOAT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public FloatLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterFloatLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitFloatLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitFloatLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SmallIntLiteralContext extends NumberContext {
		public TerminalNode SMALLINT_LITERAL() { return getToken(FlinkCdcSqlParser.SMALLINT_LITERAL, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public SmallIntLiteralContext(NumberContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterSmallIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitSmallIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitSmallIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_number);
		int _la;
		try {
			setState(336);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,43,_ctx) ) {
			case 1:
				_localctx = new LegacyDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(305);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(304);
					match(MINUS);
					}
				}

				setState(307);
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
			case 2:
				_localctx = new IntegerLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(309);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(308);
					match(MINUS);
					}
				}

				setState(311);
				match(INTEGER_VALUE);
				}
				break;
			case 3:
				_localctx = new BigIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(313);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(312);
					match(MINUS);
					}
				}

				setState(315);
				match(BIGINT_LITERAL);
				}
				break;
			case 4:
				_localctx = new SmallIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(317);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(316);
					match(MINUS);
					}
				}

				setState(319);
				match(SMALLINT_LITERAL);
				}
				break;
			case 5:
				_localctx = new TinyIntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(321);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(320);
					match(MINUS);
					}
				}

				setState(323);
				match(TINYINT_LITERAL);
				}
				break;
			case 6:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(325);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(324);
					match(MINUS);
					}
				}

				setState(327);
				match(DOUBLE_LITERAL);
				}
				break;
			case 7:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(329);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(328);
					match(MINUS);
					}
				}

				setState(331);
				match(FLOAT_LITERAL);
				}
				break;
			case 8:
				_localctx = new BigDecimalLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(333);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(332);
					match(MINUS);
					}
				}

				setState(335);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonOperatorContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(FlinkCdcSqlParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(FlinkCdcSqlParser.NEQ, 0); }
		public TerminalNode NEQJ() { return getToken(FlinkCdcSqlParser.NEQJ, 0); }
		public TerminalNode LT() { return getToken(FlinkCdcSqlParser.LT, 0); }
		public TerminalNode LTE() { return getToken(FlinkCdcSqlParser.LTE, 0); }
		public TerminalNode GT() { return getToken(FlinkCdcSqlParser.GT, 0); }
		public TerminalNode GTE() { return getToken(FlinkCdcSqlParser.GTE, 0); }
		public TerminalNode NSEQ() { return getToken(FlinkCdcSqlParser.NSEQ, 0); }
		public ComparisonOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterComparisonOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitComparisonOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitComparisonOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOperatorContext comparisonOperator() throws RecognitionException {
		ComparisonOperatorContext _localctx = new ComparisonOperatorContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_comparisonOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(338);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 140187732541440L) != 0) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class ArithmeticOperatorContext extends ParserRuleContext {
		public TerminalNode PLUS() { return getToken(FlinkCdcSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(FlinkCdcSqlParser.MINUS, 0); }
		public TerminalNode ASTERISK() { return getToken(FlinkCdcSqlParser.ASTERISK, 0); }
		public TerminalNode SLASH() { return getToken(FlinkCdcSqlParser.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(FlinkCdcSqlParser.PERCENT, 0); }
		public TerminalNode DIV() { return getToken(FlinkCdcSqlParser.DIV, 0); }
		public TerminalNode TILDE() { return getToken(FlinkCdcSqlParser.TILDE, 0); }
		public TerminalNode AMPERSAND() { return getToken(FlinkCdcSqlParser.AMPERSAND, 0); }
		public TerminalNode PIPE() { return getToken(FlinkCdcSqlParser.PIPE, 0); }
		public TerminalNode CONCAT_PIPE() { return getToken(FlinkCdcSqlParser.CONCAT_PIPE, 0); }
		public TerminalNode HAT() { return getToken(FlinkCdcSqlParser.HAT, 0); }
		public ArithmeticOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterArithmeticOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitArithmeticOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitArithmeticOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticOperatorContext arithmeticOperator() throws RecognitionException {
		ArithmeticOperatorContext _localctx = new ArithmeticOperatorContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_arithmeticOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 143974450588549120L) != 0) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class BooleanValueContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(FlinkCdcSqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(FlinkCdcSqlParser.FALSE, 0); }
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FlinkCdcSqlParserListener ) ((FlinkCdcSqlParserListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof FlinkCdcSqlParserVisitor ) return ((FlinkCdcSqlParserVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(342);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 14:
			return valueExpression_sempred((ValueExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean valueExpression_sempred(ValueExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 6);
		case 1:
			return precpred(_ctx, 5);
		case 2:
			return precpred(_ctx, 4);
		case 3:
			return precpred(_ctx, 3);
		case 4:
			return precpred(_ctx, 2);
		case 5:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001L\u0159\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0001\u0000\u0001\u0000\u0005\u00005\b\u0000\n\u0000\f\u00008\t\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001I\b\u0001\u0001\u0001"+
		"\u0003\u0001L\b\u0001\u0001\u0001\u0001\u0001\u0003\u0001P\b\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001W\b"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0003\u0001_\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u0001j\b\u0001\u0001\u0001\u0003\u0001m\b\u0001\u0001\u0001\u0001\u0001"+
		"\u0003\u0001q\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001{\b\u0001"+
		"\u0003\u0001}\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001"+
		"\u0082\b\u0001\u0001\u0001\u0001\u0001\u0003\u0001\u0086\b\u0001\u0003"+
		"\u0001\u0088\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u008d"+
		"\b\u0002\n\u0002\f\u0002\u0090\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004\u0099\b\u0004"+
		"\n\u0004\f\u0004\u009c\t\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005"+
		"\u0007\u00a7\b\u0007\n\u0007\f\u0007\u00aa\t\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0003\b\u00b0\b\b\u0001\b\u0003\b\u00b3\b\b\u0001\t\u0001"+
		"\t\u0001\t\u0005\t\u00b8\b\t\n\t\f\t\u00bb\t\t\u0001\t\u0003\t\u00be\b"+
		"\t\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u00c4\b\n\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0005\u000b\u00c9\b\u000b\n\u000b\f\u000b\u00cc\t\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u00d4\b\f\u0001"+
		"\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e"+
		"\u00dc\b\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0005\u000e\u00f1\b\u000e\n\u000e\f\u000e\u00f4"+
		"\t\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0003\u000f\u0102\b\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0005"+
		"\u000f\u0107\b\u000f\n\u000f\f\u000f\u010a\t\u000f\u0003\u000f\u010c\b"+
		"\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u0110\b\u000f\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0005\u0010\u0115\b\u0010\n\u0010\f\u0010\u0118\t\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0003\u0012\u011e\b\u0012"+
		"\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0004\u0014"+
		"\u012b\b\u0014\u000b\u0014\f\u0014\u012c\u0003\u0014\u012f\b\u0014\u0001"+
		"\u0015\u0003\u0015\u0132\b\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u0136"+
		"\b\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u013a\b\u0015\u0001\u0015"+
		"\u0001\u0015\u0003\u0015\u013e\b\u0015\u0001\u0015\u0001\u0015\u0003\u0015"+
		"\u0142\b\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u0146\b\u0015\u0001"+
		"\u0015\u0001\u0015\u0003\u0015\u014a\b\u0015\u0001\u0015\u0001\u0015\u0003"+
		"\u0015\u014e\b\u0015\u0001\u0015\u0003\u0015\u0151\b\u0015\u0001\u0016"+
		"\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0000\u0001\u001c\u0019\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012"+
		"\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.0\u0000\t\u0002\u0000\u0012"+
		"\u0012\u001c\u001c\u0002\u0000/044\u0002\u0000\u0014\u001413\u0002\u0000"+
		"/077\u0002\u0000\n\n\u0013\u0013\u0001\u0000BC\u0001\u0000\'.\u0002\u0000"+
		"\u0014\u0014/8\u0002\u0000\u0018\u0018%%\u0180\u00002\u0001\u0000\u0000"+
		"\u0000\u0002\u0087\u0001\u0000\u0000\u0000\u0004\u0089\u0001\u0000\u0000"+
		"\u0000\u0006\u0091\u0001\u0000\u0000\u0000\b\u0095\u0001\u0000\u0000\u0000"+
		"\n\u009d\u0001\u0000\u0000\u0000\f\u009f\u0001\u0000\u0000\u0000\u000e"+
		"\u00a2\u0001\u0000\u0000\u0000\u0010\u00ad\u0001\u0000\u0000\u0000\u0012"+
		"\u00bd\u0001\u0000\u0000\u0000\u0014\u00c3\u0001\u0000\u0000\u0000\u0016"+
		"\u00c5\u0001\u0000\u0000\u0000\u0018\u00cd\u0001\u0000\u0000\u0000\u001a"+
		"\u00d5\u0001\u0000\u0000\u0000\u001c\u00db\u0001\u0000\u0000\u0000\u001e"+
		"\u010f\u0001\u0000\u0000\u0000 \u0111\u0001\u0000\u0000\u0000\"\u0119"+
		"\u0001\u0000\u0000\u0000$\u011d\u0001\u0000\u0000\u0000&\u011f\u0001\u0000"+
		"\u0000\u0000(\u012e\u0001\u0000\u0000\u0000*\u0150\u0001\u0000\u0000\u0000"+
		",\u0152\u0001\u0000\u0000\u0000.\u0154\u0001\u0000\u0000\u00000\u0156"+
		"\u0001\u0000\u0000\u000026\u0003\u0002\u0001\u000035\u0005\u0001\u0000"+
		"\u000043\u0001\u0000\u0000\u000058\u0001\u0000\u0000\u000064\u0001\u0000"+
		"\u0000\u000067\u0001\u0000\u0000\u000079\u0001\u0000\u0000\u000086\u0001"+
		"\u0000\u0000\u00009:\u0005\u0000\u0000\u0001:\u0001\u0001\u0000\u0000"+
		"\u0000;<\u0005\f\u0000\u0000<=\u0005\"\u0000\u0000=\u0088\u0005!\u0000"+
		"\u0000>\u0088\u0005\u0015\u0000\u0000?@\u0005\u0011\u0000\u0000@A\u0005"+
		"#\u0000\u0000AB\u0005\u001a\u0000\u0000BC\u0005\u001d\u0000\u0000CD\u0005"+
		"\u0017\u0000\u0000DH\u0003\u0004\u0002\u0000EF\u0005 \u0000\u0000FG\u0005"+
		"\r\u0000\u0000GI\u0003\u0006\u0003\u0000HE\u0001\u0000\u0000\u0000HI\u0001"+
		"\u0000\u0000\u0000IK\u0001\u0000\u0000\u0000JL\u0003\f\u0006\u0000KJ\u0001"+
		"\u0000\u0000\u0000KL\u0001\u0000\u0000\u0000LO\u0001\u0000\u0000\u0000"+
		"MN\u0005&\u0000\u0000NP\u0003\u000e\u0007\u0000OM\u0001\u0000\u0000\u0000"+
		"OP\u0001\u0000\u0000\u0000PQ\u0001\u0000\u0000\u0000QR\u0005\u000b\u0000"+
		"\u0000RS\u0005#\u0000\u0000SV\u0003\u0004\u0002\u0000TU\u0005\u001f\u0000"+
		"\u0000UW\u0003\u000e\u0007\u0000VT\u0001\u0000\u0000\u0000VW\u0001\u0000"+
		"\u0000\u0000W^\u0001\u0000\u0000\u0000XY\u0005\b\u0000\u0000YZ\u0005\u000f"+
		"\u0000\u0000Z[\u0005\u0002\u0000\u0000[\\\u0003\u0016\u000b\u0000\\]\u0005"+
		"\u0003\u0000\u0000]_\u0001\u0000\u0000\u0000^X\u0001\u0000\u0000\u0000"+
		"^_\u0001\u0000\u0000\u0000_\u0088\u0001\u0000\u0000\u0000`a\u0005\u0011"+
		"\u0000\u0000ab\u0005\u0012\u0000\u0000bc\u0005\u001a\u0000\u0000cd\u0005"+
		"\u001d\u0000\u0000de\u0005\u0017\u0000\u0000ei\u0003\u0004\u0002\u0000"+
		"fg\u0005 \u0000\u0000gh\u0005\r\u0000\u0000hj\u0003\u0006\u0003\u0000"+
		"if\u0001\u0000\u0000\u0000ij\u0001\u0000\u0000\u0000jl\u0001\u0000\u0000"+
		"\u0000km\u0003\f\u0006\u0000lk\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000"+
		"\u0000mp\u0001\u0000\u0000\u0000no\u0005&\u0000\u0000oq\u0003\u000e\u0007"+
		"\u0000pn\u0001\u0000\u0000\u0000pq\u0001\u0000\u0000\u0000qr\u0001\u0000"+
		"\u0000\u0000rs\u0005\u000b\u0000\u0000st\u0007\u0000\u0000\u0000t|\u0003"+
		"\u0004\u0002\u0000uz\u0005\u001b\u0000\u0000vw\u0005\n\u0000\u0000w{\u0005"+
		"$\u0000\u0000xy\u0005#\u0000\u0000y{\u0005=\u0000\u0000zv\u0001\u0000"+
		"\u0000\u0000zx\u0001\u0000\u0000\u0000{}\u0001\u0000\u0000\u0000|u\u0001"+
		"\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}\u0081\u0001\u0000\u0000"+
		"\u0000~\u007f\u0005\u0016\u0000\u0000\u007f\u0080\u0005#\u0000\u0000\u0080"+
		"\u0082\u0005=\u0000\u0000\u0081~\u0001\u0000\u0000\u0000\u0081\u0082\u0001"+
		"\u0000\u0000\u0000\u0082\u0085\u0001\u0000\u0000\u0000\u0083\u0084\u0005"+
		"\u001f\u0000\u0000\u0084\u0086\u0003\u000e\u0007\u0000\u0085\u0083\u0001"+
		"\u0000\u0000\u0000\u0085\u0086\u0001\u0000\u0000\u0000\u0086\u0088\u0001"+
		"\u0000\u0000\u0000\u0087;\u0001\u0000\u0000\u0000\u0087>\u0001\u0000\u0000"+
		"\u0000\u0087?\u0001\u0000\u0000\u0000\u0087`\u0001\u0000\u0000\u0000\u0088"+
		"\u0003\u0001\u0000\u0000\u0000\u0089\u008e\u0003$\u0012\u0000\u008a\u008b"+
		"\u0005\u0005\u0000\u0000\u008b\u008d\u0003$\u0012\u0000\u008c\u008a\u0001"+
		"\u0000\u0000\u0000\u008d\u0090\u0001\u0000\u0000\u0000\u008e\u008c\u0001"+
		"\u0000\u0000\u0000\u008e\u008f\u0001\u0000\u0000\u0000\u008f\u0005\u0001"+
		"\u0000\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0091\u0092\u0005"+
		"\u0002\u0000\u0000\u0092\u0093\u0003\b\u0004\u0000\u0093\u0094\u0005\u0003"+
		"\u0000\u0000\u0094\u0007\u0001\u0000\u0000\u0000\u0095\u009a\u0003$\u0012"+
		"\u0000\u0096\u0097\u0005\u0004\u0000\u0000\u0097\u0099\u0003$\u0012\u0000"+
		"\u0098\u0096\u0001\u0000\u0000\u0000\u0099\u009c\u0001\u0000\u0000\u0000"+
		"\u009a\u0098\u0001\u0000\u0000\u0000\u009a\u009b\u0001\u0000\u0000\u0000"+
		"\u009b\t\u0001\u0000\u0000\u0000\u009c\u009a\u0001\u0000\u0000\u0000\u009d"+
		"\u009e\u0005=\u0000\u0000\u009e\u000b\u0001\u0000\u0000\u0000\u009f\u00a0"+
		"\u0005\u0010\u0000\u0000\u00a0\u00a1\u0005=\u0000\u0000\u00a1\r\u0001"+
		"\u0000\u0000\u0000\u00a2\u00a3\u0005\u0002\u0000\u0000\u00a3\u00a8\u0003"+
		"\u0010\b\u0000\u00a4\u00a5\u0005\u0004\u0000\u0000\u00a5\u00a7\u0003\u0010"+
		"\b\u0000\u00a6\u00a4\u0001\u0000\u0000\u0000\u00a7\u00aa\u0001\u0000\u0000"+
		"\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a8\u00a9\u0001\u0000\u0000"+
		"\u0000\u00a9\u00ab\u0001\u0000\u0000\u0000\u00aa\u00a8\u0001\u0000\u0000"+
		"\u0000\u00ab\u00ac\u0005\u0003\u0000\u0000\u00ac\u000f\u0001\u0000\u0000"+
		"\u0000\u00ad\u00b2\u0003\u0012\t\u0000\u00ae\u00b0\u0005\'\u0000\u0000"+
		"\u00af\u00ae\u0001\u0000\u0000\u0000\u00af\u00b0\u0001\u0000\u0000\u0000"+
		"\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00b3\u0003\u0014\n\u0000\u00b2"+
		"\u00af\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000\u00b3"+
		"\u0011\u0001\u0000\u0000\u0000\u00b4\u00b9\u0003$\u0012\u0000\u00b5\u00b6"+
		"\u0005\u0005\u0000\u0000\u00b6\u00b8\u0003$\u0012\u0000\u00b7\u00b5\u0001"+
		"\u0000\u0000\u0000\u00b8\u00bb\u0001\u0000\u0000\u0000\u00b9\u00b7\u0001"+
		"\u0000\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00be\u0001"+
		"\u0000\u0000\u0000\u00bb\u00b9\u0001\u0000\u0000\u0000\u00bc\u00be\u0005"+
		"=\u0000\u0000\u00bd\u00b4\u0001\u0000\u0000\u0000\u00bd\u00bc\u0001\u0000"+
		"\u0000\u0000\u00be\u0013\u0001\u0000\u0000\u0000\u00bf\u00c4\u0005A\u0000"+
		"\u0000\u00c0\u00c4\u0005C\u0000\u0000\u00c1\u00c4\u00030\u0018\u0000\u00c2"+
		"\u00c4\u0005=\u0000\u0000\u00c3\u00bf\u0001\u0000\u0000\u0000\u00c3\u00c0"+
		"\u0001\u0000\u0000\u0000\u00c3\u00c1\u0001\u0000\u0000\u0000\u00c3\u00c2"+
		"\u0001\u0000\u0000\u0000\u00c4\u0015\u0001\u0000\u0000\u0000\u00c5\u00ca"+
		"\u0003\u0018\f\u0000\u00c6\u00c7\u0005\u0004\u0000\u0000\u00c7\u00c9\u0003"+
		"\u0018\f\u0000\u00c8\u00c6\u0001\u0000\u0000\u0000\u00c9\u00cc\u0001\u0000"+
		"\u0000\u0000\u00ca\u00c8\u0001\u0000\u0000\u0000\u00ca\u00cb\u0001\u0000"+
		"\u0000\u0000\u00cb\u0017\u0001\u0000\u0000\u0000\u00cc\u00ca\u0001\u0000"+
		"\u0000\u0000\u00cd\u00ce\u0003$\u0012\u0000\u00ce\u00cf\u0005\u000b\u0000"+
		"\u0000\u00cf\u00d3\u0003\u001a\r\u0000\u00d0\u00d4\u0005\u0019\u0000\u0000"+
		"\u00d1\u00d2\u0005\t\u0000\u0000\u00d2\u00d4\u0003$\u0012\u0000\u00d3"+
		"\u00d0\u0001\u0000\u0000\u0000\u00d3\u00d1\u0001\u0000\u0000\u0000\u00d3"+
		"\u00d4\u0001\u0000\u0000\u0000\u00d4\u0019\u0001\u0000\u0000\u0000\u00d5"+
		"\u00d6\u0003\u001c\u000e\u0000\u00d6\u001b\u0001\u0000\u0000\u0000\u00d7"+
		"\u00d8\u0006\u000e\uffff\uffff\u0000\u00d8\u00dc\u0003\u001e\u000f\u0000"+
		"\u00d9\u00da\u0007\u0001\u0000\u0000\u00da\u00dc\u0003\u001c\u000e\u0007"+
		"\u00db\u00d7\u0001\u0000\u0000\u0000\u00db\u00d9\u0001\u0000\u0000\u0000"+
		"\u00dc\u00f2\u0001\u0000\u0000\u0000\u00dd\u00de\n\u0006\u0000\u0000\u00de"+
		"\u00df\u0007\u0002\u0000\u0000\u00df\u00f1\u0003\u001c\u000e\u0007\u00e0"+
		"\u00e1\n\u0005\u0000\u0000\u00e1\u00e2\u0007\u0003\u0000\u0000\u00e2\u00f1"+
		"\u0003\u001c\u000e\u0006\u00e3\u00e4\n\u0004\u0000\u0000\u00e4\u00e5\u0005"+
		"5\u0000\u0000\u00e5\u00f1\u0003\u001c\u000e\u0005\u00e6\u00e7\n\u0003"+
		"\u0000\u0000\u00e7\u00e8\u00058\u0000\u0000\u00e8\u00f1\u0003\u001c\u000e"+
		"\u0004\u00e9\u00ea\n\u0002\u0000\u0000\u00ea\u00eb\u00056\u0000\u0000"+
		"\u00eb\u00f1\u0003\u001c\u000e\u0003\u00ec\u00ed\n\u0001\u0000\u0000\u00ed"+
		"\u00ee\u0003,\u0016\u0000\u00ee\u00ef\u0003\u001c\u000e\u0002\u00ef\u00f1"+
		"\u0001\u0000\u0000\u0000\u00f0\u00dd\u0001\u0000\u0000\u0000\u00f0\u00e0"+
		"\u0001\u0000\u0000\u0000\u00f0\u00e3\u0001\u0000\u0000\u0000\u00f0\u00e6"+
		"\u0001\u0000\u0000\u0000\u00f0\u00e9\u0001\u0000\u0000\u0000\u00f0\u00ec"+
		"\u0001\u0000\u0000\u0000\u00f1\u00f4\u0001\u0000\u0000\u0000\u00f2\u00f0"+
		"\u0001\u0000\u0000\u0000\u00f2\u00f3\u0001\u0000\u0000\u0000\u00f3\u001d"+
		"\u0001\u0000\u0000\u0000\u00f4\u00f2\u0001\u0000\u0000\u0000\u00f5\u0110"+
		"\u0003$\u0012\u0000\u00f6\u0110\u0003(\u0014\u0000\u00f7\u00f8\u0005\u000e"+
		"\u0000\u0000\u00f8\u00f9\u0005\u0002\u0000\u0000\u00f9\u00fa\u0003\u001a"+
		"\r\u0000\u00fa\u00fb\u0005\u000b\u0000\u0000\u00fb\u00fc\u0003$\u0012"+
		"\u0000\u00fc\u00fd\u0005\u0003\u0000\u0000\u00fd\u0110\u0001\u0000\u0000"+
		"\u0000\u00fe\u00ff\u0003 \u0010\u0000\u00ff\u010b\u0005\u0002\u0000\u0000"+
		"\u0100\u0102\u0003\"\u0011\u0000\u0101\u0100\u0001\u0000\u0000\u0000\u0101"+
		"\u0102\u0001\u0000\u0000\u0000\u0102\u0103\u0001\u0000\u0000\u0000\u0103"+
		"\u0108\u0003\u001a\r\u0000\u0104\u0105\u0005\u0004\u0000\u0000\u0105\u0107"+
		"\u0003\u001a\r\u0000\u0106\u0104\u0001\u0000\u0000\u0000\u0107\u010a\u0001"+
		"\u0000\u0000\u0000\u0108\u0106\u0001\u0000\u0000\u0000\u0108\u0109\u0001"+
		"\u0000\u0000\u0000\u0109\u010c\u0001\u0000\u0000\u0000\u010a\u0108\u0001"+
		"\u0000\u0000\u0000\u010b\u0101\u0001\u0000\u0000\u0000\u010b\u010c\u0001"+
		"\u0000\u0000\u0000\u010c\u010d\u0001\u0000\u0000\u0000\u010d\u010e\u0005"+
		"\u0003\u0000\u0000\u010e\u0110\u0001\u0000\u0000\u0000\u010f\u00f5\u0001"+
		"\u0000\u0000\u0000\u010f\u00f6\u0001\u0000\u0000\u0000\u010f\u00f7\u0001"+
		"\u0000\u0000\u0000\u010f\u00fe\u0001\u0000\u0000\u0000\u0110\u001f\u0001"+
		"\u0000\u0000\u0000\u0111\u0116\u0003$\u0012\u0000\u0112\u0113\u0005\u0005"+
		"\u0000\u0000\u0113\u0115\u0003$\u0012\u0000\u0114\u0112\u0001\u0000\u0000"+
		"\u0000\u0115\u0118\u0001\u0000\u0000\u0000\u0116\u0114\u0001\u0000\u0000"+
		"\u0000\u0116\u0117\u0001\u0000\u0000\u0000\u0117!\u0001\u0000\u0000\u0000"+
		"\u0118\u0116\u0001\u0000\u0000\u0000\u0119\u011a\u0007\u0004\u0000\u0000"+
		"\u011a#\u0001\u0000\u0000\u0000\u011b\u011e\u0005G\u0000\u0000\u011c\u011e"+
		"\u0003&\u0013\u0000\u011d\u011b\u0001\u0000\u0000\u0000\u011d\u011c\u0001"+
		"\u0000\u0000\u0000\u011e%\u0001\u0000\u0000\u0000\u011f\u0120\u0005H\u0000"+
		"\u0000\u0120\'\u0001\u0000\u0000\u0000\u0121\u012f\u0005\u001e\u0000\u0000"+
		"\u0122\u0123\u00059\u0000\u0000\u0123\u012f\u0003$\u0012\u0000\u0124\u0125"+
		"\u0003$\u0012\u0000\u0125\u0126\u0003\n\u0005\u0000\u0126\u012f\u0001"+
		"\u0000\u0000\u0000\u0127\u012f\u0003*\u0015\u0000\u0128\u012f\u00030\u0018"+
		"\u0000\u0129\u012b\u0003\n\u0005\u0000\u012a\u0129\u0001\u0000\u0000\u0000"+
		"\u012b\u012c\u0001\u0000\u0000\u0000\u012c\u012a\u0001\u0000\u0000\u0000"+
		"\u012c\u012d\u0001\u0000\u0000\u0000\u012d\u012f\u0001\u0000\u0000\u0000"+
		"\u012e\u0121\u0001\u0000\u0000\u0000\u012e\u0122\u0001\u0000\u0000\u0000"+
		"\u012e\u0124\u0001\u0000\u0000\u0000\u012e\u0127\u0001\u0000\u0000\u0000"+
		"\u012e\u0128\u0001\u0000\u0000\u0000\u012e\u012a\u0001\u0000\u0000\u0000"+
		"\u012f)\u0001\u0000\u0000\u0000\u0130\u0132\u00050\u0000\u0000\u0131\u0130"+
		"\u0001\u0000\u0000\u0000\u0131\u0132\u0001\u0000\u0000\u0000\u0132\u0133"+
		"\u0001\u0000\u0000\u0000\u0133\u0151\u0007\u0005\u0000\u0000\u0134\u0136"+
		"\u00050\u0000\u0000\u0135\u0134\u0001\u0000\u0000\u0000\u0135\u0136\u0001"+
		"\u0000\u0000\u0000\u0136\u0137\u0001\u0000\u0000\u0000\u0137\u0151\u0005"+
		"A\u0000\u0000\u0138\u013a\u00050\u0000\u0000\u0139\u0138\u0001\u0000\u0000"+
		"\u0000\u0139\u013a\u0001\u0000\u0000\u0000\u013a\u013b\u0001\u0000\u0000"+
		"\u0000\u013b\u0151\u0005>\u0000\u0000\u013c\u013e\u00050\u0000\u0000\u013d"+
		"\u013c\u0001\u0000\u0000\u0000\u013d\u013e\u0001\u0000\u0000\u0000\u013e"+
		"\u013f\u0001\u0000\u0000\u0000\u013f\u0151\u0005?\u0000\u0000\u0140\u0142"+
		"\u00050\u0000\u0000\u0141\u0140\u0001\u0000\u0000\u0000\u0141\u0142\u0001"+
		"\u0000\u0000\u0000\u0142\u0143\u0001\u0000\u0000\u0000\u0143\u0151\u0005"+
		"@\u0000\u0000\u0144\u0146\u00050\u0000\u0000\u0145\u0144\u0001\u0000\u0000"+
		"\u0000\u0145\u0146\u0001\u0000\u0000\u0000\u0146\u0147\u0001\u0000\u0000"+
		"\u0000\u0147\u0151\u0005E\u0000\u0000\u0148\u014a\u00050\u0000\u0000\u0149"+
		"\u0148\u0001\u0000\u0000\u0000\u0149\u014a\u0001\u0000\u0000\u0000\u014a"+
		"\u014b\u0001\u0000\u0000\u0000\u014b\u0151\u0005D\u0000\u0000\u014c\u014e"+
		"\u00050\u0000\u0000\u014d\u014c\u0001\u0000\u0000\u0000\u014d\u014e\u0001"+
		"\u0000\u0000\u0000\u014e\u014f\u0001\u0000\u0000\u0000\u014f\u0151\u0005"+
		"F\u0000\u0000\u0150\u0131\u0001\u0000\u0000\u0000\u0150\u0135\u0001\u0000"+
		"\u0000\u0000\u0150\u0139\u0001\u0000\u0000\u0000\u0150\u013d\u0001\u0000"+
		"\u0000\u0000\u0150\u0141\u0001\u0000\u0000\u0000\u0150\u0145\u0001\u0000"+
		"\u0000\u0000\u0150\u0149\u0001\u0000\u0000\u0000\u0150\u014d\u0001\u0000"+
		"\u0000\u0000\u0151+\u0001\u0000\u0000\u0000\u0152\u0153\u0007\u0006\u0000"+
		"\u0000\u0153-\u0001\u0000\u0000\u0000\u0154\u0155\u0007\u0007\u0000\u0000"+
		"\u0155/\u0001\u0000\u0000\u0000\u0156\u0157\u0007\b\u0000\u0000\u0157"+
		"1\u0001\u0000\u0000\u0000,6HKOV^ilpz|\u0081\u0085\u0087\u008e\u009a\u00a8"+
		"\u00af\u00b2\u00b9\u00bd\u00c3\u00ca\u00d3\u00db\u00f0\u00f2\u0101\u0108"+
		"\u010b\u010f\u0116\u011d\u012c\u012e\u0131\u0135\u0139\u013d\u0141\u0145"+
		"\u0149\u014d\u0150";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}