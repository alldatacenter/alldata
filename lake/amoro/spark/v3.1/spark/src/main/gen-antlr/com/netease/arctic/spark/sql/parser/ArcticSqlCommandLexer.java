package com.netease.arctic.spark.sql.parser;// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlCommand.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ArcticSqlCommandLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, MIGRATE=2, ARCTIC=3, TO=4, EQ=5, NSEQ=6, NEQ=7, NEQJ=8, LT=9, 
		LTE=10, GT=11, GTE=12, PLUS=13, MINUS=14, ASTERISK=15, SLASH=16, PERCENT=17, 
		TILDE=18, AMPERSAND=19, PIPE=20, CONCAT_PIPE=21, HAT=22, STRING=23, IDENTIFIER=24, 
		BACKQUOTED_IDENTIFIER=25, UNRECOGNIZED=26;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "MIGRATE", "ARCTIC", "TO", "EQ", "NSEQ", "NEQ", "NEQJ", "LT", 
			"LTE", "GT", "GTE", "PLUS", "MINUS", "ASTERISK", "SLASH", "PERCENT", 
			"TILDE", "AMPERSAND", "PIPE", "CONCAT_PIPE", "HAT", "STRING", "IDENTIFIER", 
			"BACKQUOTED_IDENTIFIER", "DECIMAL_DIGITS", "EXPONENT", "DIGIT", "LETTER", 
			"UNRECOGNIZED"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'.'", "'MIGRATE'", "'ARCTIC'", "'TO'", null, "'<=>'", "'<>'", 
			"'!='", "'<'", null, "'>'", null, "'+'", "'-'", "'*'", "'/'", "'%'", 
			"'~'", "'&'", "'|'", "'||'", "'^'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, "MIGRATE", "ARCTIC", "TO", "EQ", "NSEQ", "NEQ", "NEQJ", "LT", 
			"LTE", "GT", "GTE", "PLUS", "MINUS", "ASTERISK", "SLASH", "PERCENT", 
			"TILDE", "AMPERSAND", "PIPE", "CONCAT_PIPE", "HAT", "STRING", "IDENTIFIER", 
			"BACKQUOTED_IDENTIFIER", "UNRECOGNIZED"
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


	public ArcticSqlCommandLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ArcticSqlCommand.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\34\u00d2\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\3\2"+
		"\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3"+
		"\5\3\5\3\6\3\6\3\6\5\6W\n\6\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3"+
		"\n\3\n\3\13\3\13\3\13\3\13\5\13i\n\13\3\f\3\f\3\r\3\r\3\r\3\r\5\rq\n\r"+
		"\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24"+
		"\3\25\3\25\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3\30\3\30\7\30\u008c\n\30"+
		"\f\30\16\30\u008f\13\30\3\30\3\30\3\30\3\30\3\30\7\30\u0096\n\30\f\30"+
		"\16\30\u0099\13\30\3\30\5\30\u009c\n\30\3\31\3\31\3\31\6\31\u00a1\n\31"+
		"\r\31\16\31\u00a2\3\32\3\32\3\32\3\32\7\32\u00a9\n\32\f\32\16\32\u00ac"+
		"\13\32\3\32\3\32\3\33\6\33\u00b1\n\33\r\33\16\33\u00b2\3\33\3\33\7\33"+
		"\u00b7\n\33\f\33\16\33\u00ba\13\33\3\33\3\33\6\33\u00be\n\33\r\33\16\33"+
		"\u00bf\5\33\u00c2\n\33\3\34\3\34\5\34\u00c6\n\34\3\34\6\34\u00c9\n\34"+
		"\r\34\16\34\u00ca\3\35\3\35\3\36\3\36\3\37\3\37\2\2 \3\3\5\4\7\5\t\6\13"+
		"\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'"+
		"\25)\26+\27-\30/\31\61\32\63\33\65\2\67\29\2;\2=\34\3\2\b\4\2))^^\4\2"+
		"$$^^\3\2bb\4\2--//\3\2\62;\3\2C\\\2\u00e0\2\3\3\2\2\2\2\5\3\2\2\2\2\7"+
		"\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2"+
		"\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2"+
		"\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2"+
		"\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2"+
		"\2=\3\2\2\2\3?\3\2\2\2\5A\3\2\2\2\7I\3\2\2\2\tP\3\2\2\2\13V\3\2\2\2\r"+
		"X\3\2\2\2\17\\\3\2\2\2\21_\3\2\2\2\23b\3\2\2\2\25h\3\2\2\2\27j\3\2\2\2"+
		"\31p\3\2\2\2\33r\3\2\2\2\35t\3\2\2\2\37v\3\2\2\2!x\3\2\2\2#z\3\2\2\2%"+
		"|\3\2\2\2\'~\3\2\2\2)\u0080\3\2\2\2+\u0082\3\2\2\2-\u0085\3\2\2\2/\u009b"+
		"\3\2\2\2\61\u00a0\3\2\2\2\63\u00a4\3\2\2\2\65\u00c1\3\2\2\2\67\u00c3\3"+
		"\2\2\29\u00cc\3\2\2\2;\u00ce\3\2\2\2=\u00d0\3\2\2\2?@\7\60\2\2@\4\3\2"+
		"\2\2AB\7O\2\2BC\7K\2\2CD\7I\2\2DE\7T\2\2EF\7C\2\2FG\7V\2\2GH\7G\2\2H\6"+
		"\3\2\2\2IJ\7C\2\2JK\7T\2\2KL\7E\2\2LM\7V\2\2MN\7K\2\2NO\7E\2\2O\b\3\2"+
		"\2\2PQ\7V\2\2QR\7Q\2\2R\n\3\2\2\2SW\7?\2\2TU\7?\2\2UW\7?\2\2VS\3\2\2\2"+
		"VT\3\2\2\2W\f\3\2\2\2XY\7>\2\2YZ\7?\2\2Z[\7@\2\2[\16\3\2\2\2\\]\7>\2\2"+
		"]^\7@\2\2^\20\3\2\2\2_`\7#\2\2`a\7?\2\2a\22\3\2\2\2bc\7>\2\2c\24\3\2\2"+
		"\2de\7>\2\2ei\7?\2\2fg\7#\2\2gi\7@\2\2hd\3\2\2\2hf\3\2\2\2i\26\3\2\2\2"+
		"jk\7@\2\2k\30\3\2\2\2lm\7@\2\2mq\7?\2\2no\7#\2\2oq\7>\2\2pl\3\2\2\2pn"+
		"\3\2\2\2q\32\3\2\2\2rs\7-\2\2s\34\3\2\2\2tu\7/\2\2u\36\3\2\2\2vw\7,\2"+
		"\2w \3\2\2\2xy\7\61\2\2y\"\3\2\2\2z{\7\'\2\2{$\3\2\2\2|}\7\u0080\2\2}"+
		"&\3\2\2\2~\177\7(\2\2\177(\3\2\2\2\u0080\u0081\7~\2\2\u0081*\3\2\2\2\u0082"+
		"\u0083\7~\2\2\u0083\u0084\7~\2\2\u0084,\3\2\2\2\u0085\u0086\7`\2\2\u0086"+
		".\3\2\2\2\u0087\u008d\7)\2\2\u0088\u008c\n\2\2\2\u0089\u008a\7^\2\2\u008a"+
		"\u008c\13\2\2\2\u008b\u0088\3\2\2\2\u008b\u0089\3\2\2\2\u008c\u008f\3"+
		"\2\2\2\u008d\u008b\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u0090\3\2\2\2\u008f"+
		"\u008d\3\2\2\2\u0090\u009c\7)\2\2\u0091\u0097\7$\2\2\u0092\u0096\n\3\2"+
		"\2\u0093\u0094\7^\2\2\u0094\u0096\13\2\2\2\u0095\u0092\3\2\2\2\u0095\u0093"+
		"\3\2\2\2\u0096\u0099\3\2\2\2\u0097\u0095\3\2\2\2\u0097\u0098\3\2\2\2\u0098"+
		"\u009a\3\2\2\2\u0099\u0097\3\2\2\2\u009a\u009c\7$\2\2\u009b\u0087\3\2"+
		"\2\2\u009b\u0091\3\2\2\2\u009c\60\3\2\2\2\u009d\u00a1\5;\36\2\u009e\u00a1"+
		"\59\35\2\u009f\u00a1\7a\2\2\u00a0\u009d\3\2\2\2\u00a0\u009e\3\2\2\2\u00a0"+
		"\u009f\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a0\3\2\2\2\u00a2\u00a3\3\2"+
		"\2\2\u00a3\62\3\2\2\2\u00a4\u00aa\7b\2\2\u00a5\u00a9\n\4\2\2\u00a6\u00a7"+
		"\7b\2\2\u00a7\u00a9\7b\2\2\u00a8\u00a5\3\2\2\2\u00a8\u00a6\3\2\2\2\u00a9"+
		"\u00ac\3\2\2\2\u00aa\u00a8\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\u00ad\3\2"+
		"\2\2\u00ac\u00aa\3\2\2\2\u00ad\u00ae\7b\2\2\u00ae\64\3\2\2\2\u00af\u00b1"+
		"\59\35\2\u00b0\u00af\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b0\3\2\2\2\u00b2"+
		"\u00b3\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4\u00b8\7\60\2\2\u00b5\u00b7\5"+
		"9\35\2\u00b6\u00b5\3\2\2\2\u00b7\u00ba\3\2\2\2\u00b8\u00b6\3\2\2\2\u00b8"+
		"\u00b9\3\2\2\2\u00b9\u00c2\3\2\2\2\u00ba\u00b8\3\2\2\2\u00bb\u00bd\7\60"+
		"\2\2\u00bc\u00be\59\35\2\u00bd\u00bc\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf"+
		"\u00bd\3\2\2\2\u00bf\u00c0\3\2\2\2\u00c0\u00c2\3\2\2\2\u00c1\u00b0\3\2"+
		"\2\2\u00c1\u00bb\3\2\2\2\u00c2\66\3\2\2\2\u00c3\u00c5\7G\2\2\u00c4\u00c6"+
		"\t\5\2\2\u00c5\u00c4\3\2\2\2\u00c5\u00c6\3\2\2\2\u00c6\u00c8\3\2\2\2\u00c7"+
		"\u00c9\59\35\2\u00c8\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00c8\3\2"+
		"\2\2\u00ca\u00cb\3\2\2\2\u00cb8\3\2\2\2\u00cc\u00cd\t\6\2\2\u00cd:\3\2"+
		"\2\2\u00ce\u00cf\t\7\2\2\u00cf<\3\2\2\2\u00d0\u00d1\13\2\2\2\u00d1>\3"+
		"\2\2\2\25\2Vhp\u008b\u008d\u0095\u0097\u009b\u00a0\u00a2\u00a8\u00aa\u00b2"+
		"\u00b8\u00bf\u00c1\u00c5\u00ca\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
