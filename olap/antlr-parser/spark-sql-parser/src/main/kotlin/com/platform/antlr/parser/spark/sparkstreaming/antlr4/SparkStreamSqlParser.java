// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.spark.sparkstreaming.antlr4;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class SparkStreamSqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SPACE=1, SPEC_MYSQL_COMMENT=2, COMMENT_INPUT=3, LINE_COMMENT=4, CREATE=5, 
		TABLE=6, STREAM=7, WITH=8, COMMENT=9, TRUE=10, FALSE=11, AS=12, BY=13, 
		SET=14, DELAY=15, INSERT=16, INTO=17, USING=18, PATTERN=19, MINUSMINUS=20, 
		STRING=21, BOOLEAN=22, INT=23, BIGINT=24, FLOAT=25, DOUBLE=26, DATE=27, 
		TIMESTAMP=28, STAR=29, DIVIDE=30, MODULE=31, PLUS=32, MINUS=33, EQUAL_SYMBOL=34, 
		GREATER_SYMBOL=35, LESS_SYMBOL=36, EXCLAMATION_SYMBOL=37, BIT_NOT_OP=38, 
		BIT_OR_OP=39, BIT_AND_OP=40, BIT_XOR_OP=41, DOT=42, LR_BRACKET=43, RR_BRACKET=44, 
		COMMA=45, SEMI=46, ID=47, REVERSE_QUOTE_ID=48, STRING_LITERAL=49, DECIMAL_LITERAL=50, 
		REAL_LITERAL=51, ERROR_RECONGNIGION=52;
	public static final int
		RULE_root = 0, RULE_sqlStatements = 1, RULE_sqlStatement = 2, RULE_createStreamTable = 3, 
		RULE_insertStatement = 4, RULE_setStatement = 5, RULE_emptyStatement = 6, 
		RULE_setKeyExpr = 7, RULE_valueKeyExpr = 8, RULE_selectExpr = 9, RULE_word = 10, 
		RULE_colTypeList = 11, RULE_colType = 12, RULE_dataType = 13, RULE_tablePropertyList = 14, 
		RULE_tableProperty = 15, RULE_tablePropertyKey = 16, RULE_tablePropertyValue = 17, 
		RULE_booleanValue = 18, RULE_tableIdentifier = 19, RULE_identifier = 20;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "sqlStatements", "sqlStatement", "createStreamTable", "insertStatement", 
			"setStatement", "emptyStatement", "setKeyExpr", "valueKeyExpr", "selectExpr", 
			"word", "colTypeList", "colType", "dataType", "tablePropertyList", "tableProperty", 
			"tablePropertyKey", "tablePropertyValue", "booleanValue", "tableIdentifier", 
			"identifier"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, "'CREATE'", "'TABLE'", "'STREAM'", "'WITH'", 
			"'COMMENT'", "'TRUE'", "'FALSE'", "'AS'", "'BY'", "'SET'", "'DELAY'", 
			"'INSERT'", "'INTO'", "'USING'", "'PATTERN'", "'--'", "'STRING'", "'BOOLEAN'", 
			"'INT'", "'BIGINT'", "'FLOAT'", "'DOUBLE'", "'DATE'", "'TIMESTAMP'", 
			"'*'", "'/'", "'%'", "'+'", "'-'", "'='", "'>'", "'<'", "'!'", "'~'", 
			"'|'", "'&'", "'^'", "'.'", "'('", "')'", "','", "';'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SPACE", "SPEC_MYSQL_COMMENT", "COMMENT_INPUT", "LINE_COMMENT", 
			"CREATE", "TABLE", "STREAM", "WITH", "COMMENT", "TRUE", "FALSE", "AS", 
			"BY", "SET", "DELAY", "INSERT", "INTO", "USING", "PATTERN", "MINUSMINUS", 
			"STRING", "BOOLEAN", "INT", "BIGINT", "FLOAT", "DOUBLE", "DATE", "TIMESTAMP", 
			"STAR", "DIVIDE", "MODULE", "PLUS", "MINUS", "EQUAL_SYMBOL", "GREATER_SYMBOL", 
			"LESS_SYMBOL", "EXCLAMATION_SYMBOL", "BIT_NOT_OP", "BIT_OR_OP", "BIT_AND_OP", 
			"BIT_XOR_OP", "DOT", "LR_BRACKET", "RR_BRACKET", "COMMA", "SEMI", "ID", 
			"REVERSE_QUOTE_ID", "STRING_LITERAL", "DECIMAL_LITERAL", "REAL_LITERAL", 
			"ERROR_RECONGNIGION"
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

	public SparkStreamSqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RootContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(SparkStreamSqlParser.EOF, 0); }
		public SqlStatementsContext sqlStatements() {
			return getRuleContext(SqlStatementsContext.class,0);
		}
		public TerminalNode MINUSMINUS() { return getToken(SparkStreamSqlParser.MINUSMINUS, 0); }
		public RootContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_root; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterRoot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitRoot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitRoot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RootContext root() throws RecognitionException {
		RootContext _localctx = new RootContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_root);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 70368744259616L) != 0) {
				{
				setState(42);
				sqlStatements();
				}
			}

			setState(46);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUSMINUS) {
				{
				setState(45);
				match(MINUSMINUS);
				}
			}

			setState(48);
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
	public static class SqlStatementsContext extends ParserRuleContext {
		public List<SqlStatementContext> sqlStatement() {
			return getRuleContexts(SqlStatementContext.class);
		}
		public SqlStatementContext sqlStatement(int i) {
			return getRuleContext(SqlStatementContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public List<TerminalNode> SEMI() { return getTokens(SparkStreamSqlParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(SparkStreamSqlParser.SEMI, i);
		}
		public List<TerminalNode> MINUSMINUS() { return getTokens(SparkStreamSqlParser.MINUSMINUS); }
		public TerminalNode MINUSMINUS(int i) {
			return getToken(SparkStreamSqlParser.MINUSMINUS, i);
		}
		public SqlStatementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sqlStatements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterSqlStatements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitSqlStatements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitSqlStatements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SqlStatementsContext sqlStatements() throws RecognitionException {
		SqlStatementsContext _localctx = new SqlStatementsContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_sqlStatements);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(57);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case CREATE:
					case SET:
					case INSERT:
						{
						setState(50);
						sqlStatement();
						setState(52);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==MINUSMINUS) {
							{
							setState(51);
							match(MINUSMINUS);
							}
						}

						setState(54);
						match(SEMI);
						}
						break;
					case SEMI:
						{
						setState(56);
						emptyStatement();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(61);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			}
			setState(70);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CREATE:
			case SET:
			case INSERT:
				{
				setState(62);
				sqlStatement();
				setState(67);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
				case 1:
					{
					setState(64);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==MINUSMINUS) {
						{
						setState(63);
						match(MINUSMINUS);
						}
					}

					setState(66);
					match(SEMI);
					}
					break;
				}
				}
				break;
			case SEMI:
				{
				setState(69);
				emptyStatement();
				}
				break;
			default:
				throw new NoViableAltException(this);
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
	public static class SqlStatementContext extends ParserRuleContext {
		public CreateStreamTableContext createStreamTable() {
			return getRuleContext(CreateStreamTableContext.class,0);
		}
		public InsertStatementContext insertStatement() {
			return getRuleContext(InsertStatementContext.class,0);
		}
		public SetStatementContext setStatement() {
			return getRuleContext(SetStatementContext.class,0);
		}
		public SqlStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sqlStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterSqlStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitSqlStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitSqlStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SqlStatementContext sqlStatement() throws RecognitionException {
		SqlStatementContext _localctx = new SqlStatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_sqlStatement);
		try {
			setState(75);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CREATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(72);
				createStreamTable();
				}
				break;
			case INSERT:
				enterOuterAlt(_localctx, 2);
				{
				setState(73);
				insertStatement();
				}
				break;
			case SET:
				enterOuterAlt(_localctx, 3);
				{
				setState(74);
				setStatement();
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
	public static class CreateStreamTableContext extends ParserRuleContext {
		public TableIdentifierContext tableName;
		public ColTypeListContext columns;
		public TablePropertyListContext tableProps;
		public TerminalNode CREATE() { return getToken(SparkStreamSqlParser.CREATE, 0); }
		public TerminalNode STREAM() { return getToken(SparkStreamSqlParser.STREAM, 0); }
		public TerminalNode TABLE() { return getToken(SparkStreamSqlParser.TABLE, 0); }
		public TerminalNode WITH() { return getToken(SparkStreamSqlParser.WITH, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public TablePropertyListContext tablePropertyList() {
			return getRuleContext(TablePropertyListContext.class,0);
		}
		public TerminalNode LR_BRACKET() { return getToken(SparkStreamSqlParser.LR_BRACKET, 0); }
		public TerminalNode RR_BRACKET() { return getToken(SparkStreamSqlParser.RR_BRACKET, 0); }
		public ColTypeListContext colTypeList() {
			return getRuleContext(ColTypeListContext.class,0);
		}
		public CreateStreamTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createStreamTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterCreateStreamTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitCreateStreamTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitCreateStreamTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreateStreamTableContext createStreamTable() throws RecognitionException {
		CreateStreamTableContext _localctx = new CreateStreamTableContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_createStreamTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(77);
			match(CREATE);
			setState(78);
			match(STREAM);
			setState(79);
			match(TABLE);
			setState(80);
			((CreateStreamTableContext)_localctx).tableName = tableIdentifier();
			setState(85);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LR_BRACKET) {
				{
				setState(81);
				match(LR_BRACKET);
				setState(82);
				((CreateStreamTableContext)_localctx).columns = colTypeList();
				setState(83);
				match(RR_BRACKET);
				}
			}

			setState(87);
			match(WITH);
			setState(88);
			((CreateStreamTableContext)_localctx).tableProps = tablePropertyList();
			}
		}
		catch (RecognitionException re) {
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
	public static class InsertStatementContext extends ParserRuleContext {
		public TableIdentifierContext tableName;
		public SelectExprContext select;
		public TerminalNode INSERT() { return getToken(SparkStreamSqlParser.INSERT, 0); }
		public TerminalNode INTO() { return getToken(SparkStreamSqlParser.INTO, 0); }
		public TableIdentifierContext tableIdentifier() {
			return getRuleContext(TableIdentifierContext.class,0);
		}
		public SelectExprContext selectExpr() {
			return getRuleContext(SelectExprContext.class,0);
		}
		public InsertStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterInsertStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitInsertStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitInsertStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertStatementContext insertStatement() throws RecognitionException {
		InsertStatementContext _localctx = new InsertStatementContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_insertStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			match(INSERT);
			setState(91);
			match(INTO);
			setState(92);
			((InsertStatementContext)_localctx).tableName = tableIdentifier();
			setState(93);
			((InsertStatementContext)_localctx).select = selectExpr();
			}
		}
		catch (RecognitionException re) {
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
	public static class SetStatementContext extends ParserRuleContext {
		public TerminalNode SET() { return getToken(SparkStreamSqlParser.SET, 0); }
		public SetKeyExprContext setKeyExpr() {
			return getRuleContext(SetKeyExprContext.class,0);
		}
		public TerminalNode EQUAL_SYMBOL() { return getToken(SparkStreamSqlParser.EQUAL_SYMBOL, 0); }
		public ValueKeyExprContext valueKeyExpr() {
			return getRuleContext(ValueKeyExprContext.class,0);
		}
		public SetStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterSetStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitSetStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitSetStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetStatementContext setStatement() throws RecognitionException {
		SetStatementContext _localctx = new SetStatementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_setStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
			match(SET);
			setState(96);
			setKeyExpr();
			setState(97);
			match(EQUAL_SYMBOL);
			setState(98);
			valueKeyExpr();
			}
		}
		catch (RecognitionException re) {
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
	public static class EmptyStatementContext extends ParserRuleContext {
		public TerminalNode SEMI() { return getToken(SparkStreamSqlParser.SEMI, 0); }
		public EmptyStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_emptyStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterEmptyStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitEmptyStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitEmptyStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EmptyStatementContext emptyStatement() throws RecognitionException {
		EmptyStatementContext _localctx = new EmptyStatementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_emptyStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
	public static class SetKeyExprContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(SparkStreamSqlParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SparkStreamSqlParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(SparkStreamSqlParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(SparkStreamSqlParser.DOT, i);
		}
		public SetKeyExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setKeyExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterSetKeyExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitSetKeyExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitSetKeyExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetKeyExprContext setKeyExpr() throws RecognitionException {
		SetKeyExprContext _localctx = new SetKeyExprContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_setKeyExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(ID);
			setState(107);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(103);
				match(DOT);
				setState(104);
				match(ID);
				}
				}
				setState(109);
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
	public static class ValueKeyExprContext extends ParserRuleContext {
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public ValueKeyExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueKeyExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterValueKeyExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitValueKeyExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitValueKeyExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueKeyExprContext valueKeyExpr() throws RecognitionException {
		ValueKeyExprContext _localctx = new ValueKeyExprContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_valueKeyExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
			word();
			setState(114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((_la) & ~0x3f) == 0 && ((1L << _la) & 4151755369643072L) != 0) {
				{
				{
				setState(111);
				word();
				}
				}
				setState(116);
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
	public static class SelectExprContext extends ParserRuleContext {
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public SelectExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterSelectExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitSelectExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitSelectExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectExprContext selectExpr() throws RecognitionException {
		SelectExprContext _localctx = new SelectExprContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_selectExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(117);
			word();
			setState(121);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((_la) & ~0x3f) == 0 && ((1L << _la) & 4151755369643072L) != 0) {
				{
				{
				setState(118);
				word();
				}
				}
				setState(123);
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
	public static class WordContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SparkStreamSqlParser.ID, 0); }
		public TerminalNode DOT() { return getToken(SparkStreamSqlParser.DOT, 0); }
		public TerminalNode STRING_LITERAL() { return getToken(SparkStreamSqlParser.STRING_LITERAL, 0); }
		public TerminalNode DECIMAL_LITERAL() { return getToken(SparkStreamSqlParser.DECIMAL_LITERAL, 0); }
		public TerminalNode REAL_LITERAL() { return getToken(SparkStreamSqlParser.REAL_LITERAL, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode AS() { return getToken(SparkStreamSqlParser.AS, 0); }
		public TerminalNode BY() { return getToken(SparkStreamSqlParser.BY, 0); }
		public TerminalNode SET() { return getToken(SparkStreamSqlParser.SET, 0); }
		public TerminalNode STAR() { return getToken(SparkStreamSqlParser.STAR, 0); }
		public TerminalNode DIVIDE() { return getToken(SparkStreamSqlParser.DIVIDE, 0); }
		public TerminalNode MODULE() { return getToken(SparkStreamSqlParser.MODULE, 0); }
		public TerminalNode PLUS() { return getToken(SparkStreamSqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(SparkStreamSqlParser.MINUS, 0); }
		public TerminalNode EQUAL_SYMBOL() { return getToken(SparkStreamSqlParser.EQUAL_SYMBOL, 0); }
		public TerminalNode GREATER_SYMBOL() { return getToken(SparkStreamSqlParser.GREATER_SYMBOL, 0); }
		public TerminalNode LESS_SYMBOL() { return getToken(SparkStreamSqlParser.LESS_SYMBOL, 0); }
		public TerminalNode EXCLAMATION_SYMBOL() { return getToken(SparkStreamSqlParser.EXCLAMATION_SYMBOL, 0); }
		public TerminalNode BIT_NOT_OP() { return getToken(SparkStreamSqlParser.BIT_NOT_OP, 0); }
		public TerminalNode BIT_OR_OP() { return getToken(SparkStreamSqlParser.BIT_OR_OP, 0); }
		public TerminalNode BIT_AND_OP() { return getToken(SparkStreamSqlParser.BIT_AND_OP, 0); }
		public TerminalNode BIT_XOR_OP() { return getToken(SparkStreamSqlParser.BIT_XOR_OP, 0); }
		public TerminalNode LR_BRACKET() { return getToken(SparkStreamSqlParser.LR_BRACKET, 0); }
		public TerminalNode RR_BRACKET() { return getToken(SparkStreamSqlParser.RR_BRACKET, 0); }
		public TerminalNode COMMA() { return getToken(SparkStreamSqlParser.COMMA, 0); }
		public TerminalNode TABLE() { return getToken(SparkStreamSqlParser.TABLE, 0); }
		public WordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WordContext word() throws RecognitionException {
		WordContext _localctx = new WordContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_word);
		try {
			setState(151);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(124);
				match(ID);
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(125);
				match(DOT);
				setState(126);
				match(ID);
				}
				}
				break;
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(127);
				match(STRING_LITERAL);
				}
				break;
			case DECIMAL_LITERAL:
				enterOuterAlt(_localctx, 4);
				{
				setState(128);
				match(DECIMAL_LITERAL);
				}
				break;
			case REAL_LITERAL:
				enterOuterAlt(_localctx, 5);
				{
				setState(129);
				match(REAL_LITERAL);
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 6);
				{
				setState(130);
				booleanValue();
				}
				break;
			case AS:
				enterOuterAlt(_localctx, 7);
				{
				setState(131);
				match(AS);
				}
				break;
			case BY:
				enterOuterAlt(_localctx, 8);
				{
				setState(132);
				match(BY);
				}
				break;
			case SET:
				enterOuterAlt(_localctx, 9);
				{
				setState(133);
				match(SET);
				}
				break;
			case STAR:
				enterOuterAlt(_localctx, 10);
				{
				setState(134);
				match(STAR);
				}
				break;
			case DIVIDE:
				enterOuterAlt(_localctx, 11);
				{
				setState(135);
				match(DIVIDE);
				}
				break;
			case MODULE:
				enterOuterAlt(_localctx, 12);
				{
				setState(136);
				match(MODULE);
				}
				break;
			case PLUS:
				enterOuterAlt(_localctx, 13);
				{
				setState(137);
				match(PLUS);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 14);
				{
				setState(138);
				match(MINUS);
				}
				break;
			case EQUAL_SYMBOL:
				enterOuterAlt(_localctx, 15);
				{
				setState(139);
				match(EQUAL_SYMBOL);
				}
				break;
			case GREATER_SYMBOL:
				enterOuterAlt(_localctx, 16);
				{
				setState(140);
				match(GREATER_SYMBOL);
				}
				break;
			case LESS_SYMBOL:
				enterOuterAlt(_localctx, 17);
				{
				setState(141);
				match(LESS_SYMBOL);
				}
				break;
			case EXCLAMATION_SYMBOL:
				enterOuterAlt(_localctx, 18);
				{
				setState(142);
				match(EXCLAMATION_SYMBOL);
				}
				break;
			case BIT_NOT_OP:
				enterOuterAlt(_localctx, 19);
				{
				setState(143);
				match(BIT_NOT_OP);
				}
				break;
			case BIT_OR_OP:
				enterOuterAlt(_localctx, 20);
				{
				setState(144);
				match(BIT_OR_OP);
				}
				break;
			case BIT_AND_OP:
				enterOuterAlt(_localctx, 21);
				{
				setState(145);
				match(BIT_AND_OP);
				}
				break;
			case BIT_XOR_OP:
				enterOuterAlt(_localctx, 22);
				{
				setState(146);
				match(BIT_XOR_OP);
				}
				break;
			case LR_BRACKET:
				enterOuterAlt(_localctx, 23);
				{
				setState(147);
				match(LR_BRACKET);
				}
				break;
			case RR_BRACKET:
				enterOuterAlt(_localctx, 24);
				{
				setState(148);
				match(RR_BRACKET);
				}
				break;
			case COMMA:
				enterOuterAlt(_localctx, 25);
				{
				setState(149);
				match(COMMA);
				}
				break;
			case TABLE:
				enterOuterAlt(_localctx, 26);
				{
				setState(150);
				match(TABLE);
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
	public static class ColTypeListContext extends ParserRuleContext {
		public List<ColTypeContext> colType() {
			return getRuleContexts(ColTypeContext.class);
		}
		public ColTypeContext colType(int i) {
			return getRuleContext(ColTypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(SparkStreamSqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SparkStreamSqlParser.COMMA, i);
		}
		public ColTypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colTypeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterColTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitColTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitColTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeListContext colTypeList() throws RecognitionException {
		ColTypeListContext _localctx = new ColTypeListContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_colTypeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(153);
			colType();
			setState(158);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(154);
				match(COMMA);
				setState(155);
				colType();
				}
				}
				setState(160);
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
	public static class ColTypeContext extends ParserRuleContext {
		public Token jsonPath;
		public Token pattern;
		public Token comment;
		public TerminalNode ID() { return getToken(SparkStreamSqlParser.ID, 0); }
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public TerminalNode PATTERN() { return getToken(SparkStreamSqlParser.PATTERN, 0); }
		public TerminalNode COMMENT() { return getToken(SparkStreamSqlParser.COMMENT, 0); }
		public List<TerminalNode> STRING_LITERAL() { return getTokens(SparkStreamSqlParser.STRING_LITERAL); }
		public TerminalNode STRING_LITERAL(int i) {
			return getToken(SparkStreamSqlParser.STRING_LITERAL, i);
		}
		public ColTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterColType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitColType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitColType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColTypeContext colType() throws RecognitionException {
		ColTypeContext _localctx = new ColTypeContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_colType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(161);
			match(ID);
			setState(163);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING_LITERAL) {
				{
				setState(162);
				((ColTypeContext)_localctx).jsonPath = match(STRING_LITERAL);
				}
			}

			setState(165);
			dataType();
			setState(168);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PATTERN) {
				{
				setState(166);
				match(PATTERN);
				setState(167);
				((ColTypeContext)_localctx).pattern = match(STRING_LITERAL);
				}
			}

			setState(172);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(170);
				match(COMMENT);
				setState(171);
				((ColTypeContext)_localctx).comment = match(STRING_LITERAL);
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
	public static class DataTypeContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(SparkStreamSqlParser.STRING, 0); }
		public TerminalNode BOOLEAN() { return getToken(SparkStreamSqlParser.BOOLEAN, 0); }
		public TerminalNode INT() { return getToken(SparkStreamSqlParser.INT, 0); }
		public TerminalNode BIGINT() { return getToken(SparkStreamSqlParser.BIGINT, 0); }
		public TerminalNode FLOAT() { return getToken(SparkStreamSqlParser.FLOAT, 0); }
		public TerminalNode DOUBLE() { return getToken(SparkStreamSqlParser.DOUBLE, 0); }
		public TerminalNode DATE() { return getToken(SparkStreamSqlParser.DATE, 0); }
		public TerminalNode TIMESTAMP() { return getToken(SparkStreamSqlParser.TIMESTAMP, 0); }
		public DataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitDataType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_dataType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(174);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 534773760L) != 0) ) {
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
	public static class TablePropertyListContext extends ParserRuleContext {
		public TerminalNode LR_BRACKET() { return getToken(SparkStreamSqlParser.LR_BRACKET, 0); }
		public List<TablePropertyContext> tableProperty() {
			return getRuleContexts(TablePropertyContext.class);
		}
		public TablePropertyContext tableProperty(int i) {
			return getRuleContext(TablePropertyContext.class,i);
		}
		public TerminalNode RR_BRACKET() { return getToken(SparkStreamSqlParser.RR_BRACKET, 0); }
		public List<TerminalNode> COMMA() { return getTokens(SparkStreamSqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(SparkStreamSqlParser.COMMA, i);
		}
		public TablePropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterTablePropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitTablePropertyList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitTablePropertyList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyListContext tablePropertyList() throws RecognitionException {
		TablePropertyListContext _localctx = new TablePropertyListContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_tablePropertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(176);
			match(LR_BRACKET);
			setState(177);
			tableProperty();
			setState(182);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(178);
				match(COMMA);
				setState(179);
				tableProperty();
				}
				}
				setState(184);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(185);
			match(RR_BRACKET);
			}
		}
		catch (RecognitionException re) {
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
	public static class TablePropertyContext extends ParserRuleContext {
		public TablePropertyKeyContext key;
		public TablePropertyValueContext value;
		public TerminalNode EQUAL_SYMBOL() { return getToken(SparkStreamSqlParser.EQUAL_SYMBOL, 0); }
		public TablePropertyKeyContext tablePropertyKey() {
			return getRuleContext(TablePropertyKeyContext.class,0);
		}
		public TablePropertyValueContext tablePropertyValue() {
			return getRuleContext(TablePropertyValueContext.class,0);
		}
		public TablePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterTableProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitTableProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitTableProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyContext tableProperty() throws RecognitionException {
		TablePropertyContext _localctx = new TablePropertyContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_tableProperty);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(187);
			((TablePropertyContext)_localctx).key = tablePropertyKey();
			setState(188);
			match(EQUAL_SYMBOL);
			setState(189);
			((TablePropertyContext)_localctx).value = tablePropertyValue();
			}
		}
		catch (RecognitionException re) {
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
	public static class TablePropertyKeyContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(SparkStreamSqlParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(SparkStreamSqlParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(SparkStreamSqlParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(SparkStreamSqlParser.DOT, i);
		}
		public TerminalNode STRING_LITERAL() { return getToken(SparkStreamSqlParser.STRING_LITERAL, 0); }
		public TablePropertyKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterTablePropertyKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitTablePropertyKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitTablePropertyKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyKeyContext tablePropertyKey() throws RecognitionException {
		TablePropertyKeyContext _localctx = new TablePropertyKeyContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_tablePropertyKey);
		int _la;
		try {
			setState(200);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(191);
				match(ID);
				setState(196);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(192);
					match(DOT);
					setState(193);
					match(ID);
					}
					}
					setState(198);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(199);
				match(STRING_LITERAL);
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
	public static class TablePropertyValueContext extends ParserRuleContext {
		public TerminalNode DECIMAL_LITERAL() { return getToken(SparkStreamSqlParser.DECIMAL_LITERAL, 0); }
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public TerminalNode STRING_LITERAL() { return getToken(SparkStreamSqlParser.STRING_LITERAL, 0); }
		public TablePropertyValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablePropertyValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterTablePropertyValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitTablePropertyValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitTablePropertyValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablePropertyValueContext tablePropertyValue() throws RecognitionException {
		TablePropertyValueContext _localctx = new TablePropertyValueContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_tablePropertyValue);
		try {
			setState(205);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DECIMAL_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(202);
				match(DECIMAL_LITERAL);
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(203);
				booleanValue();
				}
				break;
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(204);
				match(STRING_LITERAL);
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
	public static class BooleanValueContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(SparkStreamSqlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(SparkStreamSqlParser.FALSE, 0); }
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
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

	@SuppressWarnings("CheckReturnValue")
	public static class TableIdentifierContext extends ParserRuleContext {
		public IdentifierContext db;
		public IdentifierContext table;
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode DOT() { return getToken(SparkStreamSqlParser.DOT, 0); }
		public TableIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterTableIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitTableIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitTableIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableIdentifierContext tableIdentifier() throws RecognitionException {
		TableIdentifierContext _localctx = new TableIdentifierContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_tableIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(212);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(209);
				((TableIdentifierContext)_localctx).db = identifier();
				setState(210);
				match(DOT);
				}
				break;
			}
			setState(214);
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SparkStreamSqlParser.ID, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SparkStreamSqlParserListener ) ((SparkStreamSqlParserListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SparkStreamSqlParserVisitor ) return ((SparkStreamSqlParserVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_identifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u00014\u00db\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0001\u0000\u0003\u0000"+
		",\b\u0000\u0001\u0000\u0003\u0000/\b\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0001\u0001\u0001\u0003\u00015\b\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0005\u0001:\b\u0001\n\u0001\f\u0001=\t\u0001\u0001\u0001\u0001"+
		"\u0001\u0003\u0001A\b\u0001\u0001\u0001\u0003\u0001D\b\u0001\u0001\u0001"+
		"\u0003\u0001G\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002"+
		"L\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003V\b\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007"+
		"j\b\u0007\n\u0007\f\u0007m\t\u0007\u0001\b\u0001\b\u0005\bq\b\b\n\b\f"+
		"\bt\t\b\u0001\t\u0001\t\u0005\tx\b\t\n\t\f\t{\t\t\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u0098\b\n\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u009d\b\u000b\n\u000b\f\u000b"+
		"\u00a0\t\u000b\u0001\f\u0001\f\u0003\f\u00a4\b\f\u0001\f\u0001\f\u0001"+
		"\f\u0003\f\u00a9\b\f\u0001\f\u0001\f\u0003\f\u00ad\b\f\u0001\r\u0001\r"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e\u00b5\b\u000e"+
		"\n\u000e\f\u000e\u00b8\t\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0005"+
		"\u0010\u00c3\b\u0010\n\u0010\f\u0010\u00c6\t\u0010\u0001\u0010\u0003\u0010"+
		"\u00c9\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u00ce\b"+
		"\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0003"+
		"\u0013\u00d5\b\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0000\u0000\u0015\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012"+
		"\u0014\u0016\u0018\u001a\u001c\u001e \"$&(\u0000\u0002\u0001\u0000\u0015"+
		"\u001c\u0001\u0000\n\u000b\u00f6\u0000+\u0001\u0000\u0000\u0000\u0002"+
		";\u0001\u0000\u0000\u0000\u0004K\u0001\u0000\u0000\u0000\u0006M\u0001"+
		"\u0000\u0000\u0000\bZ\u0001\u0000\u0000\u0000\n_\u0001\u0000\u0000\u0000"+
		"\fd\u0001\u0000\u0000\u0000\u000ef\u0001\u0000\u0000\u0000\u0010n\u0001"+
		"\u0000\u0000\u0000\u0012u\u0001\u0000\u0000\u0000\u0014\u0097\u0001\u0000"+
		"\u0000\u0000\u0016\u0099\u0001\u0000\u0000\u0000\u0018\u00a1\u0001\u0000"+
		"\u0000\u0000\u001a\u00ae\u0001\u0000\u0000\u0000\u001c\u00b0\u0001\u0000"+
		"\u0000\u0000\u001e\u00bb\u0001\u0000\u0000\u0000 \u00c8\u0001\u0000\u0000"+
		"\u0000\"\u00cd\u0001\u0000\u0000\u0000$\u00cf\u0001\u0000\u0000\u0000"+
		"&\u00d4\u0001\u0000\u0000\u0000(\u00d8\u0001\u0000\u0000\u0000*,\u0003"+
		"\u0002\u0001\u0000+*\u0001\u0000\u0000\u0000+,\u0001\u0000\u0000\u0000"+
		",.\u0001\u0000\u0000\u0000-/\u0005\u0014\u0000\u0000.-\u0001\u0000\u0000"+
		"\u0000./\u0001\u0000\u0000\u0000/0\u0001\u0000\u0000\u000001\u0005\u0000"+
		"\u0000\u00011\u0001\u0001\u0000\u0000\u000024\u0003\u0004\u0002\u0000"+
		"35\u0005\u0014\u0000\u000043\u0001\u0000\u0000\u000045\u0001\u0000\u0000"+
		"\u000056\u0001\u0000\u0000\u000067\u0005.\u0000\u00007:\u0001\u0000\u0000"+
		"\u00008:\u0003\f\u0006\u000092\u0001\u0000\u0000\u000098\u0001\u0000\u0000"+
		"\u0000:=\u0001\u0000\u0000\u0000;9\u0001\u0000\u0000\u0000;<\u0001\u0000"+
		"\u0000\u0000<F\u0001\u0000\u0000\u0000=;\u0001\u0000\u0000\u0000>C\u0003"+
		"\u0004\u0002\u0000?A\u0005\u0014\u0000\u0000@?\u0001\u0000\u0000\u0000"+
		"@A\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000BD\u0005.\u0000\u0000"+
		"C@\u0001\u0000\u0000\u0000CD\u0001\u0000\u0000\u0000DG\u0001\u0000\u0000"+
		"\u0000EG\u0003\f\u0006\u0000F>\u0001\u0000\u0000\u0000FE\u0001\u0000\u0000"+
		"\u0000G\u0003\u0001\u0000\u0000\u0000HL\u0003\u0006\u0003\u0000IL\u0003"+
		"\b\u0004\u0000JL\u0003\n\u0005\u0000KH\u0001\u0000\u0000\u0000KI\u0001"+
		"\u0000\u0000\u0000KJ\u0001\u0000\u0000\u0000L\u0005\u0001\u0000\u0000"+
		"\u0000MN\u0005\u0005\u0000\u0000NO\u0005\u0007\u0000\u0000OP\u0005\u0006"+
		"\u0000\u0000PU\u0003&\u0013\u0000QR\u0005+\u0000\u0000RS\u0003\u0016\u000b"+
		"\u0000ST\u0005,\u0000\u0000TV\u0001\u0000\u0000\u0000UQ\u0001\u0000\u0000"+
		"\u0000UV\u0001\u0000\u0000\u0000VW\u0001\u0000\u0000\u0000WX\u0005\b\u0000"+
		"\u0000XY\u0003\u001c\u000e\u0000Y\u0007\u0001\u0000\u0000\u0000Z[\u0005"+
		"\u0010\u0000\u0000[\\\u0005\u0011\u0000\u0000\\]\u0003&\u0013\u0000]^"+
		"\u0003\u0012\t\u0000^\t\u0001\u0000\u0000\u0000_`\u0005\u000e\u0000\u0000"+
		"`a\u0003\u000e\u0007\u0000ab\u0005\"\u0000\u0000bc\u0003\u0010\b\u0000"+
		"c\u000b\u0001\u0000\u0000\u0000de\u0005.\u0000\u0000e\r\u0001\u0000\u0000"+
		"\u0000fk\u0005/\u0000\u0000gh\u0005*\u0000\u0000hj\u0005/\u0000\u0000"+
		"ig\u0001\u0000\u0000\u0000jm\u0001\u0000\u0000\u0000ki\u0001\u0000\u0000"+
		"\u0000kl\u0001\u0000\u0000\u0000l\u000f\u0001\u0000\u0000\u0000mk\u0001"+
		"\u0000\u0000\u0000nr\u0003\u0014\n\u0000oq\u0003\u0014\n\u0000po\u0001"+
		"\u0000\u0000\u0000qt\u0001\u0000\u0000\u0000rp\u0001\u0000\u0000\u0000"+
		"rs\u0001\u0000\u0000\u0000s\u0011\u0001\u0000\u0000\u0000tr\u0001\u0000"+
		"\u0000\u0000uy\u0003\u0014\n\u0000vx\u0003\u0014\n\u0000wv\u0001\u0000"+
		"\u0000\u0000x{\u0001\u0000\u0000\u0000yw\u0001\u0000\u0000\u0000yz\u0001"+
		"\u0000\u0000\u0000z\u0013\u0001\u0000\u0000\u0000{y\u0001\u0000\u0000"+
		"\u0000|\u0098\u0005/\u0000\u0000}~\u0005*\u0000\u0000~\u0098\u0005/\u0000"+
		"\u0000\u007f\u0098\u00051\u0000\u0000\u0080\u0098\u00052\u0000\u0000\u0081"+
		"\u0098\u00053\u0000\u0000\u0082\u0098\u0003$\u0012\u0000\u0083\u0098\u0005"+
		"\f\u0000\u0000\u0084\u0098\u0005\r\u0000\u0000\u0085\u0098\u0005\u000e"+
		"\u0000\u0000\u0086\u0098\u0005\u001d\u0000\u0000\u0087\u0098\u0005\u001e"+
		"\u0000\u0000\u0088\u0098\u0005\u001f\u0000\u0000\u0089\u0098\u0005 \u0000"+
		"\u0000\u008a\u0098\u0005!\u0000\u0000\u008b\u0098\u0005\"\u0000\u0000"+
		"\u008c\u0098\u0005#\u0000\u0000\u008d\u0098\u0005$\u0000\u0000\u008e\u0098"+
		"\u0005%\u0000\u0000\u008f\u0098\u0005&\u0000\u0000\u0090\u0098\u0005\'"+
		"\u0000\u0000\u0091\u0098\u0005(\u0000\u0000\u0092\u0098\u0005)\u0000\u0000"+
		"\u0093\u0098\u0005+\u0000\u0000\u0094\u0098\u0005,\u0000\u0000\u0095\u0098"+
		"\u0005-\u0000\u0000\u0096\u0098\u0005\u0006\u0000\u0000\u0097|\u0001\u0000"+
		"\u0000\u0000\u0097}\u0001\u0000\u0000\u0000\u0097\u007f\u0001\u0000\u0000"+
		"\u0000\u0097\u0080\u0001\u0000\u0000\u0000\u0097\u0081\u0001\u0000\u0000"+
		"\u0000\u0097\u0082\u0001\u0000\u0000\u0000\u0097\u0083\u0001\u0000\u0000"+
		"\u0000\u0097\u0084\u0001\u0000\u0000\u0000\u0097\u0085\u0001\u0000\u0000"+
		"\u0000\u0097\u0086\u0001\u0000\u0000\u0000\u0097\u0087\u0001\u0000\u0000"+
		"\u0000\u0097\u0088\u0001\u0000\u0000\u0000\u0097\u0089\u0001\u0000\u0000"+
		"\u0000\u0097\u008a\u0001\u0000\u0000\u0000\u0097\u008b\u0001\u0000\u0000"+
		"\u0000\u0097\u008c\u0001\u0000\u0000\u0000\u0097\u008d\u0001\u0000\u0000"+
		"\u0000\u0097\u008e\u0001\u0000\u0000\u0000\u0097\u008f\u0001\u0000\u0000"+
		"\u0000\u0097\u0090\u0001\u0000\u0000\u0000\u0097\u0091\u0001\u0000\u0000"+
		"\u0000\u0097\u0092\u0001\u0000\u0000\u0000\u0097\u0093\u0001\u0000\u0000"+
		"\u0000\u0097\u0094\u0001\u0000\u0000\u0000\u0097\u0095\u0001\u0000\u0000"+
		"\u0000\u0097\u0096\u0001\u0000\u0000\u0000\u0098\u0015\u0001\u0000\u0000"+
		"\u0000\u0099\u009e\u0003\u0018\f\u0000\u009a\u009b\u0005-\u0000\u0000"+
		"\u009b\u009d\u0003\u0018\f\u0000\u009c\u009a\u0001\u0000\u0000\u0000\u009d"+
		"\u00a0\u0001\u0000\u0000\u0000\u009e\u009c\u0001\u0000\u0000\u0000\u009e"+
		"\u009f\u0001\u0000\u0000\u0000\u009f\u0017\u0001\u0000\u0000\u0000\u00a0"+
		"\u009e\u0001\u0000\u0000\u0000\u00a1\u00a3\u0005/\u0000\u0000\u00a2\u00a4"+
		"\u00051\u0000\u0000\u00a3\u00a2\u0001\u0000\u0000\u0000\u00a3\u00a4\u0001"+
		"\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a8\u0003"+
		"\u001a\r\u0000\u00a6\u00a7\u0005\u0013\u0000\u0000\u00a7\u00a9\u00051"+
		"\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a8\u00a9\u0001\u0000"+
		"\u0000\u0000\u00a9\u00ac\u0001\u0000\u0000\u0000\u00aa\u00ab\u0005\t\u0000"+
		"\u0000\u00ab\u00ad\u00051\u0000\u0000\u00ac\u00aa\u0001\u0000\u0000\u0000"+
		"\u00ac\u00ad\u0001\u0000\u0000\u0000\u00ad\u0019\u0001\u0000\u0000\u0000"+
		"\u00ae\u00af\u0007\u0000\u0000\u0000\u00af\u001b\u0001\u0000\u0000\u0000"+
		"\u00b0\u00b1\u0005+\u0000\u0000\u00b1\u00b6\u0003\u001e\u000f\u0000\u00b2"+
		"\u00b3\u0005-\u0000\u0000\u00b3\u00b5\u0003\u001e\u000f\u0000\u00b4\u00b2"+
		"\u0001\u0000\u0000\u0000\u00b5\u00b8\u0001\u0000\u0000\u0000\u00b6\u00b4"+
		"\u0001\u0000\u0000\u0000\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7\u00b9"+
		"\u0001\u0000\u0000\u0000\u00b8\u00b6\u0001\u0000\u0000\u0000\u00b9\u00ba"+
		"\u0005,\u0000\u0000\u00ba\u001d\u0001\u0000\u0000\u0000\u00bb\u00bc\u0003"+
		" \u0010\u0000\u00bc\u00bd\u0005\"\u0000\u0000\u00bd\u00be\u0003\"\u0011"+
		"\u0000\u00be\u001f\u0001\u0000\u0000\u0000\u00bf\u00c4\u0005/\u0000\u0000"+
		"\u00c0\u00c1\u0005*\u0000\u0000\u00c1\u00c3\u0005/\u0000\u0000\u00c2\u00c0"+
		"\u0001\u0000\u0000\u0000\u00c3\u00c6\u0001\u0000\u0000\u0000\u00c4\u00c2"+
		"\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001\u0000\u0000\u0000\u00c5\u00c9"+
		"\u0001\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000\u0000\u0000\u00c7\u00c9"+
		"\u00051\u0000\u0000\u00c8\u00bf\u0001\u0000\u0000\u0000\u00c8\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c9!\u0001\u0000\u0000\u0000\u00ca\u00ce\u00052\u0000"+
		"\u0000\u00cb\u00ce\u0003$\u0012\u0000\u00cc\u00ce\u00051\u0000\u0000\u00cd"+
		"\u00ca\u0001\u0000\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00cd"+
		"\u00cc\u0001\u0000\u0000\u0000\u00ce#\u0001\u0000\u0000\u0000\u00cf\u00d0"+
		"\u0007\u0001\u0000\u0000\u00d0%\u0001\u0000\u0000\u0000\u00d1\u00d2\u0003"+
		"(\u0014\u0000\u00d2\u00d3\u0005*\u0000\u0000\u00d3\u00d5\u0001\u0000\u0000"+
		"\u0000\u00d4\u00d1\u0001\u0000\u0000\u0000\u00d4\u00d5\u0001\u0000\u0000"+
		"\u0000\u00d5\u00d6\u0001\u0000\u0000\u0000\u00d6\u00d7\u0003(\u0014\u0000"+
		"\u00d7\'\u0001\u0000\u0000\u0000\u00d8\u00d9\u0005/\u0000\u0000\u00d9"+
		")\u0001\u0000\u0000\u0000\u0017+.49;@CFKUkry\u0097\u009e\u00a3\u00a8\u00ac"+
		"\u00b6\u00c4\u00c8\u00cd\u00d4";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}