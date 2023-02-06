// Code generated from EsQueryString.g4 by ANTLR 4.9. DO NOT EDIT.

package parser // EsQueryString

import (
	"fmt"
	"reflect"
	"strconv"

	"github.com/antlr/antlr4/runtime/Go/antlr"
)

// Suppress unused import errors
var _ = fmt.Printf
var _ = reflect.Copy
var _ = strconv.Itoa

var parserATN = []uint16{
	3, 24715, 42794, 33075, 47597, 16764, 15335, 30598, 22884, 3, 14, 45, 4,
	2, 9, 2, 4, 3, 9, 3, 4, 4, 9, 4, 3, 2, 5, 2, 10, 10, 2, 3, 2, 3, 2, 3,
	3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 3, 22, 10, 3, 3, 3, 3,
	3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 7, 3, 32, 10, 3, 12, 3, 14, 3, 35,
	11, 3, 3, 4, 3, 4, 3, 4, 3, 4, 3, 4, 3, 4, 5, 4, 43, 10, 4, 3, 4, 2, 3,
	4, 5, 2, 4, 6, 2, 5, 3, 2, 3, 4, 3, 2, 5, 6, 3, 2, 7, 8, 2, 50, 2, 9, 3,
	2, 2, 2, 4, 21, 3, 2, 2, 2, 6, 42, 3, 2, 2, 2, 8, 10, 5, 4, 3, 2, 9, 8,
	3, 2, 2, 2, 9, 10, 3, 2, 2, 2, 10, 11, 3, 2, 2, 2, 11, 12, 7, 2, 2, 3,
	12, 3, 3, 2, 2, 2, 13, 14, 8, 3, 1, 2, 14, 15, 7, 9, 2, 2, 15, 16, 5, 4,
	3, 2, 16, 17, 7, 10, 2, 2, 17, 22, 3, 2, 2, 2, 18, 19, 9, 2, 2, 2, 19,
	22, 5, 4, 3, 7, 20, 22, 5, 6, 4, 2, 21, 13, 3, 2, 2, 2, 21, 18, 3, 2, 2,
	2, 21, 20, 3, 2, 2, 2, 22, 33, 3, 2, 2, 2, 23, 24, 12, 6, 2, 2, 24, 25,
	9, 3, 2, 2, 25, 32, 5, 4, 3, 7, 26, 27, 12, 5, 2, 2, 27, 28, 9, 4, 2, 2,
	28, 32, 5, 4, 3, 6, 29, 30, 12, 4, 2, 2, 30, 32, 5, 4, 3, 5, 31, 23, 3,
	2, 2, 2, 31, 26, 3, 2, 2, 2, 31, 29, 3, 2, 2, 2, 32, 35, 3, 2, 2, 2, 33,
	31, 3, 2, 2, 2, 33, 34, 3, 2, 2, 2, 34, 5, 3, 2, 2, 2, 35, 33, 3, 2, 2,
	2, 36, 37, 7, 12, 2, 2, 37, 43, 7, 14, 2, 2, 38, 39, 7, 12, 2, 2, 39, 43,
	7, 13, 2, 2, 40, 43, 7, 14, 2, 2, 41, 43, 7, 13, 2, 2, 42, 36, 3, 2, 2,
	2, 42, 38, 3, 2, 2, 2, 42, 40, 3, 2, 2, 2, 42, 41, 3, 2, 2, 2, 43, 7, 3,
	2, 2, 2, 7, 9, 21, 31, 33, 42,
}
var literalNames = []string{
	"", "'NOT'", "'not'", "'AND'", "'and'", "'OR'", "'or'", "'('", "')'",
}
var symbolicNames = []string{
	"", "", "", "", "", "", "", "GROUP_BEGIN", "GROUP_END", "WHITESPACE", "FIELD",
	"TERM", "PHRASE",
}

var ruleNames = []string{
	"query", "queryExpression", "fieldQuery",
}

type EsQueryStringParser struct {
	*antlr.BaseParser
}

// NewEsQueryStringParser produces a new parser instance for the optional input antlr.TokenStream.
//
// The *EsQueryStringParser instance produced may be reused by calling the SetInputStream method.
// The initial parser configuration is expensive to construct, and the object is not thread-safe;
// however, if used within a Golang sync.Pool, the construction cost amortizes well and the
// objects can be used in a thread-safe manner.
func NewEsQueryStringParser(input antlr.TokenStream) *EsQueryStringParser {
	this := new(EsQueryStringParser)
	deserializer := antlr.NewATNDeserializer(nil)
	deserializedATN := deserializer.DeserializeFromUInt16(parserATN)
	decisionToDFA := make([]*antlr.DFA, len(deserializedATN.DecisionToState))
	for index, ds := range deserializedATN.DecisionToState {
		decisionToDFA[index] = antlr.NewDFA(ds, index)
	}
	this.BaseParser = antlr.NewBaseParser(input)

	this.Interpreter = antlr.NewParserATNSimulator(this, deserializedATN, decisionToDFA, antlr.NewPredictionContextCache())
	this.RuleNames = ruleNames
	this.LiteralNames = literalNames
	this.SymbolicNames = symbolicNames
	this.GrammarFileName = "EsQueryString.g4"

	return this
}

// EsQueryStringParser tokens.
const (
	EsQueryStringParserEOF         = antlr.TokenEOF
	EsQueryStringParserT__0        = 1
	EsQueryStringParserT__1        = 2
	EsQueryStringParserT__2        = 3
	EsQueryStringParserT__3        = 4
	EsQueryStringParserT__4        = 5
	EsQueryStringParserT__5        = 6
	EsQueryStringParserGROUP_BEGIN = 7
	EsQueryStringParserGROUP_END   = 8
	EsQueryStringParserWHITESPACE  = 9
	EsQueryStringParserFIELD       = 10
	EsQueryStringParserTERM        = 11
	EsQueryStringParserPHRASE      = 12
)

// EsQueryStringParser rules.
const (
	EsQueryStringParserRULE_query           = 0
	EsQueryStringParserRULE_queryExpression = 1
	EsQueryStringParserRULE_fieldQuery      = 2
)

// IQueryContext is an interface to support dynamic dispatch.
type IQueryContext interface {
	antlr.ParserRuleContext

	// GetParser returns the parser.
	GetParser() antlr.Parser

	// IsQueryContext differentiates from other interfaces.
	IsQueryContext()
}

type QueryContext struct {
	*antlr.BaseParserRuleContext
	parser antlr.Parser
}

func NewEmptyQueryContext() *QueryContext {
	var p = new(QueryContext)
	p.BaseParserRuleContext = antlr.NewBaseParserRuleContext(nil, -1)
	p.RuleIndex = EsQueryStringParserRULE_query
	return p
}

func (*QueryContext) IsQueryContext() {}

func NewQueryContext(parser antlr.Parser, parent antlr.ParserRuleContext, invokingState int) *QueryContext {
	var p = new(QueryContext)

	p.BaseParserRuleContext = antlr.NewBaseParserRuleContext(parent, invokingState)

	p.parser = parser
	p.RuleIndex = EsQueryStringParserRULE_query

	return p
}

func (s *QueryContext) GetParser() antlr.Parser { return s.parser }

func (s *QueryContext) EOF() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserEOF, 0)
}

func (s *QueryContext) QueryExpression() IQueryExpressionContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem(), 0)

	if t == nil {
		return nil
	}

	return t.(IQueryExpressionContext)
}

func (s *QueryContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *QueryContext) ToStringTree(ruleNames []string, recog antlr.Recognizer) string {
	return antlr.TreesStringTree(s, ruleNames, recog)
}

func (s *QueryContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterQuery(s)
	}
}

func (s *QueryContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitQuery(s)
	}
}

func (p *EsQueryStringParser) Query() (localctx IQueryContext) {
	localctx = NewQueryContext(p, p.GetParserRuleContext(), p.GetState())
	p.EnterRule(localctx, 0, EsQueryStringParserRULE_query)
	var _la int

	defer func() {
		p.ExitRule()
	}()

	defer func() {
		if err := recover(); err != nil {
			if v, ok := err.(antlr.RecognitionException); ok {
				localctx.SetException(v)
				p.GetErrorHandler().ReportError(p, v)
				p.GetErrorHandler().Recover(p, v)
			} else {
				panic(err)
			}
		}
	}()

	p.EnterOuterAlt(localctx, 1)
	p.SetState(7)
	p.GetErrorHandler().Sync(p)
	_la = p.GetTokenStream().LA(1)

	if ((_la)&-(0x1f+1)) == 0 && ((1<<uint(_la))&((1<<EsQueryStringParserT__0)|(1<<EsQueryStringParserT__1)|(1<<EsQueryStringParserGROUP_BEGIN)|(1<<EsQueryStringParserFIELD)|(1<<EsQueryStringParserTERM)|(1<<EsQueryStringParserPHRASE))) != 0 {
		{
			p.SetState(6)
			p.queryExpression(0)
		}

	}
	{
		p.SetState(9)
		p.Match(EsQueryStringParserEOF)
	}

	return localctx
}

// IQueryExpressionContext is an interface to support dynamic dispatch.
type IQueryExpressionContext interface {
	antlr.ParserRuleContext

	// GetParser returns the parser.
	GetParser() antlr.Parser

	// IsQueryExpressionContext differentiates from other interfaces.
	IsQueryExpressionContext()
}

type QueryExpressionContext struct {
	*antlr.BaseParserRuleContext
	parser antlr.Parser
}

func NewEmptyQueryExpressionContext() *QueryExpressionContext {
	var p = new(QueryExpressionContext)
	p.BaseParserRuleContext = antlr.NewBaseParserRuleContext(nil, -1)
	p.RuleIndex = EsQueryStringParserRULE_queryExpression
	return p
}

func (*QueryExpressionContext) IsQueryExpressionContext() {}

func NewQueryExpressionContext(parser antlr.Parser, parent antlr.ParserRuleContext, invokingState int) *QueryExpressionContext {
	var p = new(QueryExpressionContext)

	p.BaseParserRuleContext = antlr.NewBaseParserRuleContext(parent, invokingState)

	p.parser = parser
	p.RuleIndex = EsQueryStringParserRULE_queryExpression

	return p
}

func (s *QueryExpressionContext) GetParser() antlr.Parser { return s.parser }

func (s *QueryExpressionContext) CopyFrom(ctx *QueryExpressionContext) {
	s.BaseParserRuleContext.CopyFrom(ctx.BaseParserRuleContext)
}

func (s *QueryExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *QueryExpressionContext) ToStringTree(ruleNames []string, recog antlr.Recognizer) string {
	return antlr.TreesStringTree(s, ruleNames, recog)
}

type DefaultOpExpressionContext struct {
	*QueryExpressionContext
}

func NewDefaultOpExpressionContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *DefaultOpExpressionContext {
	var p = new(DefaultOpExpressionContext)

	p.QueryExpressionContext = NewEmptyQueryExpressionContext()
	p.parser = parser
	p.CopyFrom(ctx.(*QueryExpressionContext))

	return p
}

func (s *DefaultOpExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *DefaultOpExpressionContext) AllQueryExpression() []IQueryExpressionContext {
	var ts = s.GetTypedRuleContexts(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem())
	var tst = make([]IQueryExpressionContext, len(ts))

	for i, t := range ts {
		if t != nil {
			tst[i] = t.(IQueryExpressionContext)
		}
	}

	return tst
}

func (s *DefaultOpExpressionContext) QueryExpression(i int) IQueryExpressionContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem(), i)

	if t == nil {
		return nil
	}

	return t.(IQueryExpressionContext)
}

func (s *DefaultOpExpressionContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterDefaultOpExpression(s)
	}
}

func (s *DefaultOpExpressionContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitDefaultOpExpression(s)
	}
}

type AndExpressionContext struct {
	*QueryExpressionContext
	op antlr.Token
}

func NewAndExpressionContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *AndExpressionContext {
	var p = new(AndExpressionContext)

	p.QueryExpressionContext = NewEmptyQueryExpressionContext()
	p.parser = parser
	p.CopyFrom(ctx.(*QueryExpressionContext))

	return p
}

func (s *AndExpressionContext) GetOp() antlr.Token { return s.op }

func (s *AndExpressionContext) SetOp(v antlr.Token) { s.op = v }

func (s *AndExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *AndExpressionContext) AllQueryExpression() []IQueryExpressionContext {
	var ts = s.GetTypedRuleContexts(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem())
	var tst = make([]IQueryExpressionContext, len(ts))

	for i, t := range ts {
		if t != nil {
			tst[i] = t.(IQueryExpressionContext)
		}
	}

	return tst
}

func (s *AndExpressionContext) QueryExpression(i int) IQueryExpressionContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem(), i)

	if t == nil {
		return nil
	}

	return t.(IQueryExpressionContext)
}

func (s *AndExpressionContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterAndExpression(s)
	}
}

func (s *AndExpressionContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitAndExpression(s)
	}
}

type NotExpressionContext struct {
	*QueryExpressionContext
	op antlr.Token
}

func NewNotExpressionContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *NotExpressionContext {
	var p = new(NotExpressionContext)

	p.QueryExpressionContext = NewEmptyQueryExpressionContext()
	p.parser = parser
	p.CopyFrom(ctx.(*QueryExpressionContext))

	return p
}

func (s *NotExpressionContext) GetOp() antlr.Token { return s.op }

func (s *NotExpressionContext) SetOp(v antlr.Token) { s.op = v }

func (s *NotExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *NotExpressionContext) QueryExpression() IQueryExpressionContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem(), 0)

	if t == nil {
		return nil
	}

	return t.(IQueryExpressionContext)
}

func (s *NotExpressionContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterNotExpression(s)
	}
}

func (s *NotExpressionContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitNotExpression(s)
	}
}

type OrExpressionContext struct {
	*QueryExpressionContext
	op antlr.Token
}

func NewOrExpressionContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *OrExpressionContext {
	var p = new(OrExpressionContext)

	p.QueryExpressionContext = NewEmptyQueryExpressionContext()
	p.parser = parser
	p.CopyFrom(ctx.(*QueryExpressionContext))

	return p
}

func (s *OrExpressionContext) GetOp() antlr.Token { return s.op }

func (s *OrExpressionContext) SetOp(v antlr.Token) { s.op = v }

func (s *OrExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *OrExpressionContext) AllQueryExpression() []IQueryExpressionContext {
	var ts = s.GetTypedRuleContexts(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem())
	var tst = make([]IQueryExpressionContext, len(ts))

	for i, t := range ts {
		if t != nil {
			tst[i] = t.(IQueryExpressionContext)
		}
	}

	return tst
}

func (s *OrExpressionContext) QueryExpression(i int) IQueryExpressionContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem(), i)

	if t == nil {
		return nil
	}

	return t.(IQueryExpressionContext)
}

func (s *OrExpressionContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterOrExpression(s)
	}
}

func (s *OrExpressionContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitOrExpression(s)
	}
}

type GroupExpressionContext struct {
	*QueryExpressionContext
}

func NewGroupExpressionContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *GroupExpressionContext {
	var p = new(GroupExpressionContext)

	p.QueryExpressionContext = NewEmptyQueryExpressionContext()
	p.parser = parser
	p.CopyFrom(ctx.(*QueryExpressionContext))

	return p
}

func (s *GroupExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *GroupExpressionContext) GROUP_BEGIN() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserGROUP_BEGIN, 0)
}

func (s *GroupExpressionContext) QueryExpression() IQueryExpressionContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IQueryExpressionContext)(nil)).Elem(), 0)

	if t == nil {
		return nil
	}

	return t.(IQueryExpressionContext)
}

func (s *GroupExpressionContext) GROUP_END() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserGROUP_END, 0)
}

func (s *GroupExpressionContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterGroupExpression(s)
	}
}

func (s *GroupExpressionContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitGroupExpression(s)
	}
}

type FieldExpressionContext struct {
	*QueryExpressionContext
}

func NewFieldExpressionContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *FieldExpressionContext {
	var p = new(FieldExpressionContext)

	p.QueryExpressionContext = NewEmptyQueryExpressionContext()
	p.parser = parser
	p.CopyFrom(ctx.(*QueryExpressionContext))

	return p
}

func (s *FieldExpressionContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *FieldExpressionContext) FieldQuery() IFieldQueryContext {
	var t = s.GetTypedRuleContext(reflect.TypeOf((*IFieldQueryContext)(nil)).Elem(), 0)

	if t == nil {
		return nil
	}

	return t.(IFieldQueryContext)
}

func (s *FieldExpressionContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterFieldExpression(s)
	}
}

func (s *FieldExpressionContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitFieldExpression(s)
	}
}

func (p *EsQueryStringParser) QueryExpression() (localctx IQueryExpressionContext) {
	return p.queryExpression(0)
}

func (p *EsQueryStringParser) queryExpression(_p int) (localctx IQueryExpressionContext) {
	var _parentctx antlr.ParserRuleContext = p.GetParserRuleContext()
	_parentState := p.GetState()
	localctx = NewQueryExpressionContext(p, p.GetParserRuleContext(), _parentState)
	var _prevctx IQueryExpressionContext = localctx
	var _ antlr.ParserRuleContext = _prevctx // TODO: To prevent unused variable warning.
	_startState := 2
	p.EnterRecursionRule(localctx, 2, EsQueryStringParserRULE_queryExpression, _p)
	var _la int

	defer func() {
		p.UnrollRecursionContexts(_parentctx)
	}()

	defer func() {
		if err := recover(); err != nil {
			if v, ok := err.(antlr.RecognitionException); ok {
				localctx.SetException(v)
				p.GetErrorHandler().ReportError(p, v)
				p.GetErrorHandler().Recover(p, v)
			} else {
				panic(err)
			}
		}
	}()

	var _alt int

	p.EnterOuterAlt(localctx, 1)
	p.SetState(19)
	p.GetErrorHandler().Sync(p)

	switch p.GetTokenStream().LA(1) {
	case EsQueryStringParserGROUP_BEGIN:
		localctx = NewGroupExpressionContext(p, localctx)
		p.SetParserRuleContext(localctx)
		_prevctx = localctx

		{
			p.SetState(12)
			p.Match(EsQueryStringParserGROUP_BEGIN)
		}
		{
			p.SetState(13)
			p.queryExpression(0)
		}
		{
			p.SetState(14)
			p.Match(EsQueryStringParserGROUP_END)
		}

	case EsQueryStringParserT__0, EsQueryStringParserT__1:
		localctx = NewNotExpressionContext(p, localctx)
		p.SetParserRuleContext(localctx)
		_prevctx = localctx
		{
			p.SetState(16)

			var _lt = p.GetTokenStream().LT(1)

			localctx.(*NotExpressionContext).op = _lt

			_la = p.GetTokenStream().LA(1)

			if !(_la == EsQueryStringParserT__0 || _la == EsQueryStringParserT__1) {
				var _ri = p.GetErrorHandler().RecoverInline(p)

				localctx.(*NotExpressionContext).op = _ri
			} else {
				p.GetErrorHandler().ReportMatch(p)
				p.Consume()
			}
		}
		{
			p.SetState(17)
			p.queryExpression(5)
		}

	case EsQueryStringParserFIELD, EsQueryStringParserTERM, EsQueryStringParserPHRASE:
		localctx = NewFieldExpressionContext(p, localctx)
		p.SetParserRuleContext(localctx)
		_prevctx = localctx
		{
			p.SetState(18)
			p.FieldQuery()
		}

	default:
		panic(antlr.NewNoViableAltException(p, nil, nil, nil, nil, nil))
	}
	p.GetParserRuleContext().SetStop(p.GetTokenStream().LT(-1))
	p.SetState(31)
	p.GetErrorHandler().Sync(p)
	_alt = p.GetInterpreter().AdaptivePredict(p.GetTokenStream(), 3, p.GetParserRuleContext())

	for _alt != 2 && _alt != antlr.ATNInvalidAltNumber {
		if _alt == 1 {
			if p.GetParseListeners() != nil {
				p.TriggerExitRuleEvent()
			}
			_prevctx = localctx
			p.SetState(29)
			p.GetErrorHandler().Sync(p)
			switch p.GetInterpreter().AdaptivePredict(p.GetTokenStream(), 2, p.GetParserRuleContext()) {
			case 1:
				localctx = NewAndExpressionContext(p, NewQueryExpressionContext(p, _parentctx, _parentState))
				p.PushNewRecursionContext(localctx, _startState, EsQueryStringParserRULE_queryExpression)
				p.SetState(21)

				if !(p.Precpred(p.GetParserRuleContext(), 4)) {
					panic(antlr.NewFailedPredicateException(p, "p.Precpred(p.GetParserRuleContext(), 4)", ""))
				}
				{
					p.SetState(22)

					var _lt = p.GetTokenStream().LT(1)

					localctx.(*AndExpressionContext).op = _lt

					_la = p.GetTokenStream().LA(1)

					if !(_la == EsQueryStringParserT__2 || _la == EsQueryStringParserT__3) {
						var _ri = p.GetErrorHandler().RecoverInline(p)

						localctx.(*AndExpressionContext).op = _ri
					} else {
						p.GetErrorHandler().ReportMatch(p)
						p.Consume()
					}
				}
				{
					p.SetState(23)
					p.queryExpression(5)
				}

			case 2:
				localctx = NewOrExpressionContext(p, NewQueryExpressionContext(p, _parentctx, _parentState))
				p.PushNewRecursionContext(localctx, _startState, EsQueryStringParserRULE_queryExpression)
				p.SetState(24)

				if !(p.Precpred(p.GetParserRuleContext(), 3)) {
					panic(antlr.NewFailedPredicateException(p, "p.Precpred(p.GetParserRuleContext(), 3)", ""))
				}
				{
					p.SetState(25)

					var _lt = p.GetTokenStream().LT(1)

					localctx.(*OrExpressionContext).op = _lt

					_la = p.GetTokenStream().LA(1)

					if !(_la == EsQueryStringParserT__4 || _la == EsQueryStringParserT__5) {
						var _ri = p.GetErrorHandler().RecoverInline(p)

						localctx.(*OrExpressionContext).op = _ri
					} else {
						p.GetErrorHandler().ReportMatch(p)
						p.Consume()
					}
				}
				{
					p.SetState(26)
					p.queryExpression(4)
				}

			case 3:
				localctx = NewDefaultOpExpressionContext(p, NewQueryExpressionContext(p, _parentctx, _parentState))
				p.PushNewRecursionContext(localctx, _startState, EsQueryStringParserRULE_queryExpression)
				p.SetState(27)

				if !(p.Precpred(p.GetParserRuleContext(), 2)) {
					panic(antlr.NewFailedPredicateException(p, "p.Precpred(p.GetParserRuleContext(), 2)", ""))
				}
				{
					p.SetState(28)
					p.queryExpression(3)
				}

			}

		}
		p.SetState(33)
		p.GetErrorHandler().Sync(p)
		_alt = p.GetInterpreter().AdaptivePredict(p.GetTokenStream(), 3, p.GetParserRuleContext())
	}

	return localctx
}

// IFieldQueryContext is an interface to support dynamic dispatch.
type IFieldQueryContext interface {
	antlr.ParserRuleContext

	// GetParser returns the parser.
	GetParser() antlr.Parser

	// IsFieldQueryContext differentiates from other interfaces.
	IsFieldQueryContext()
}

type FieldQueryContext struct {
	*antlr.BaseParserRuleContext
	parser antlr.Parser
}

func NewEmptyFieldQueryContext() *FieldQueryContext {
	var p = new(FieldQueryContext)
	p.BaseParserRuleContext = antlr.NewBaseParserRuleContext(nil, -1)
	p.RuleIndex = EsQueryStringParserRULE_fieldQuery
	return p
}

func (*FieldQueryContext) IsFieldQueryContext() {}

func NewFieldQueryContext(parser antlr.Parser, parent antlr.ParserRuleContext, invokingState int) *FieldQueryContext {
	var p = new(FieldQueryContext)

	p.BaseParserRuleContext = antlr.NewBaseParserRuleContext(parent, invokingState)

	p.parser = parser
	p.RuleIndex = EsQueryStringParserRULE_fieldQuery

	return p
}

func (s *FieldQueryContext) GetParser() antlr.Parser { return s.parser }

func (s *FieldQueryContext) CopyFrom(ctx *FieldQueryContext) {
	s.BaseParserRuleContext.CopyFrom(ctx.BaseParserRuleContext)
}

func (s *FieldQueryContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *FieldQueryContext) ToStringTree(ruleNames []string, recog antlr.Recognizer) string {
	return antlr.TreesStringTree(s, ruleNames, recog)
}

type TermFieldQueryContext struct {
	*FieldQueryContext
}

func NewTermFieldQueryContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *TermFieldQueryContext {
	var p = new(TermFieldQueryContext)

	p.FieldQueryContext = NewEmptyFieldQueryContext()
	p.parser = parser
	p.CopyFrom(ctx.(*FieldQueryContext))

	return p
}

func (s *TermFieldQueryContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *TermFieldQueryContext) TERM() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserTERM, 0)
}

func (s *TermFieldQueryContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterTermFieldQuery(s)
	}
}

func (s *TermFieldQueryContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitTermFieldQuery(s)
	}
}

type NamedPhraseFieldQueryContext struct {
	*FieldQueryContext
}

func NewNamedPhraseFieldQueryContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *NamedPhraseFieldQueryContext {
	var p = new(NamedPhraseFieldQueryContext)

	p.FieldQueryContext = NewEmptyFieldQueryContext()
	p.parser = parser
	p.CopyFrom(ctx.(*FieldQueryContext))

	return p
}

func (s *NamedPhraseFieldQueryContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *NamedPhraseFieldQueryContext) FIELD() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserFIELD, 0)
}

func (s *NamedPhraseFieldQueryContext) PHRASE() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserPHRASE, 0)
}

func (s *NamedPhraseFieldQueryContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterNamedPhraseFieldQuery(s)
	}
}

func (s *NamedPhraseFieldQueryContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitNamedPhraseFieldQuery(s)
	}
}

type NamedTermFieldQueryContext struct {
	*FieldQueryContext
}

func NewNamedTermFieldQueryContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *NamedTermFieldQueryContext {
	var p = new(NamedTermFieldQueryContext)

	p.FieldQueryContext = NewEmptyFieldQueryContext()
	p.parser = parser
	p.CopyFrom(ctx.(*FieldQueryContext))

	return p
}

func (s *NamedTermFieldQueryContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *NamedTermFieldQueryContext) FIELD() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserFIELD, 0)
}

func (s *NamedTermFieldQueryContext) TERM() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserTERM, 0)
}

func (s *NamedTermFieldQueryContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterNamedTermFieldQuery(s)
	}
}

func (s *NamedTermFieldQueryContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitNamedTermFieldQuery(s)
	}
}

type PhraseFieldQueryContext struct {
	*FieldQueryContext
}

func NewPhraseFieldQueryContext(parser antlr.Parser, ctx antlr.ParserRuleContext) *PhraseFieldQueryContext {
	var p = new(PhraseFieldQueryContext)

	p.FieldQueryContext = NewEmptyFieldQueryContext()
	p.parser = parser
	p.CopyFrom(ctx.(*FieldQueryContext))

	return p
}

func (s *PhraseFieldQueryContext) GetRuleContext() antlr.RuleContext {
	return s
}

func (s *PhraseFieldQueryContext) PHRASE() antlr.TerminalNode {
	return s.GetToken(EsQueryStringParserPHRASE, 0)
}

func (s *PhraseFieldQueryContext) EnterRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.EnterPhraseFieldQuery(s)
	}
}

func (s *PhraseFieldQueryContext) ExitRule(listener antlr.ParseTreeListener) {
	if listenerT, ok := listener.(EsQueryStringListener); ok {
		listenerT.ExitPhraseFieldQuery(s)
	}
}

func (p *EsQueryStringParser) FieldQuery() (localctx IFieldQueryContext) {
	localctx = NewFieldQueryContext(p, p.GetParserRuleContext(), p.GetState())
	p.EnterRule(localctx, 4, EsQueryStringParserRULE_fieldQuery)

	defer func() {
		p.ExitRule()
	}()

	defer func() {
		if err := recover(); err != nil {
			if v, ok := err.(antlr.RecognitionException); ok {
				localctx.SetException(v)
				p.GetErrorHandler().ReportError(p, v)
				p.GetErrorHandler().Recover(p, v)
			} else {
				panic(err)
			}
		}
	}()

	p.SetState(40)
	p.GetErrorHandler().Sync(p)
	switch p.GetInterpreter().AdaptivePredict(p.GetTokenStream(), 4, p.GetParserRuleContext()) {
	case 1:
		localctx = NewNamedPhraseFieldQueryContext(p, localctx)
		p.EnterOuterAlt(localctx, 1)
		{
			p.SetState(34)
			p.Match(EsQueryStringParserFIELD)
		}
		{
			p.SetState(35)
			p.Match(EsQueryStringParserPHRASE)
		}

	case 2:
		localctx = NewNamedTermFieldQueryContext(p, localctx)
		p.EnterOuterAlt(localctx, 2)
		{
			p.SetState(36)
			p.Match(EsQueryStringParserFIELD)
		}
		{
			p.SetState(37)
			p.Match(EsQueryStringParserTERM)
		}

	case 3:
		localctx = NewPhraseFieldQueryContext(p, localctx)
		p.EnterOuterAlt(localctx, 3)
		{
			p.SetState(38)
			p.Match(EsQueryStringParserPHRASE)
		}

	case 4:
		localctx = NewTermFieldQueryContext(p, localctx)
		p.EnterOuterAlt(localctx, 4)
		{
			p.SetState(39)
			p.Match(EsQueryStringParserTERM)
		}

	}

	return localctx
}

func (p *EsQueryStringParser) Sempred(localctx antlr.RuleContext, ruleIndex, predIndex int) bool {
	switch ruleIndex {
	case 1:
		var t *QueryExpressionContext = nil
		if localctx != nil {
			t = localctx.(*QueryExpressionContext)
		}
		return p.QueryExpression_Sempred(t, predIndex)

	default:
		panic("No predicate with index: " + fmt.Sprint(ruleIndex))
	}
}

func (p *EsQueryStringParser) QueryExpression_Sempred(localctx antlr.RuleContext, predIndex int) bool {
	switch predIndex {
	case 0:
		return p.Precpred(p.GetParserRuleContext(), 4)

	case 1:
		return p.Precpred(p.GetParserRuleContext(), 3)

	case 2:
		return p.Precpred(p.GetParserRuleContext(), 2)

	default:
		panic("No predicate with index: " + fmt.Sprint(predIndex))
	}
}
