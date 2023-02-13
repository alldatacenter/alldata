// Code generated from EsQueryString.g4 by ANTLR 4.9. DO NOT EDIT.

package parser // EsQueryString

import "github.com/antlr/antlr4/runtime/Go/antlr"

// BaseEsQueryStringListener is a complete listener for a parse tree produced by EsQueryStringParser.
type BaseEsQueryStringListener struct{}

var _ EsQueryStringListener = &BaseEsQueryStringListener{}

// VisitTerminal is called when a terminal node is visited.
func (s *BaseEsQueryStringListener) VisitTerminal(node antlr.TerminalNode) {}

// VisitErrorNode is called when an error node is visited.
func (s *BaseEsQueryStringListener) VisitErrorNode(node antlr.ErrorNode) {}

// EnterEveryRule is called when any rule is entered.
func (s *BaseEsQueryStringListener) EnterEveryRule(ctx antlr.ParserRuleContext) {}

// ExitEveryRule is called when any rule is exited.
func (s *BaseEsQueryStringListener) ExitEveryRule(ctx antlr.ParserRuleContext) {}

// EnterQuery is called when production query is entered.
func (s *BaseEsQueryStringListener) EnterQuery(ctx *QueryContext) {}

// ExitQuery is called when production query is exited.
func (s *BaseEsQueryStringListener) ExitQuery(ctx *QueryContext) {}

// EnterDefaultOpExpression is called when production DefaultOpExpression is entered.
func (s *BaseEsQueryStringListener) EnterDefaultOpExpression(ctx *DefaultOpExpressionContext) {}

// ExitDefaultOpExpression is called when production DefaultOpExpression is exited.
func (s *BaseEsQueryStringListener) ExitDefaultOpExpression(ctx *DefaultOpExpressionContext) {}

// EnterAndExpression is called when production AndExpression is entered.
func (s *BaseEsQueryStringListener) EnterAndExpression(ctx *AndExpressionContext) {}

// ExitAndExpression is called when production AndExpression is exited.
func (s *BaseEsQueryStringListener) ExitAndExpression(ctx *AndExpressionContext) {}

// EnterNotExpression is called when production NotExpression is entered.
func (s *BaseEsQueryStringListener) EnterNotExpression(ctx *NotExpressionContext) {}

// ExitNotExpression is called when production NotExpression is exited.
func (s *BaseEsQueryStringListener) ExitNotExpression(ctx *NotExpressionContext) {}

// EnterOrExpression is called when production OrExpression is entered.
func (s *BaseEsQueryStringListener) EnterOrExpression(ctx *OrExpressionContext) {}

// ExitOrExpression is called when production OrExpression is exited.
func (s *BaseEsQueryStringListener) ExitOrExpression(ctx *OrExpressionContext) {}

// EnterGroupExpression is called when production GroupExpression is entered.
func (s *BaseEsQueryStringListener) EnterGroupExpression(ctx *GroupExpressionContext) {}

// ExitGroupExpression is called when production GroupExpression is exited.
func (s *BaseEsQueryStringListener) ExitGroupExpression(ctx *GroupExpressionContext) {}

// EnterFieldExpression is called when production FieldExpression is entered.
func (s *BaseEsQueryStringListener) EnterFieldExpression(ctx *FieldExpressionContext) {}

// ExitFieldExpression is called when production FieldExpression is exited.
func (s *BaseEsQueryStringListener) ExitFieldExpression(ctx *FieldExpressionContext) {}

// EnterNamedPhraseFieldQuery is called when production NamedPhraseFieldQuery is entered.
func (s *BaseEsQueryStringListener) EnterNamedPhraseFieldQuery(ctx *NamedPhraseFieldQueryContext) {}

// ExitNamedPhraseFieldQuery is called when production NamedPhraseFieldQuery is exited.
func (s *BaseEsQueryStringListener) ExitNamedPhraseFieldQuery(ctx *NamedPhraseFieldQueryContext) {}

// EnterNamedTermFieldQuery is called when production NamedTermFieldQuery is entered.
func (s *BaseEsQueryStringListener) EnterNamedTermFieldQuery(ctx *NamedTermFieldQueryContext) {}

// ExitNamedTermFieldQuery is called when production NamedTermFieldQuery is exited.
func (s *BaseEsQueryStringListener) ExitNamedTermFieldQuery(ctx *NamedTermFieldQueryContext) {}

// EnterPhraseFieldQuery is called when production PhraseFieldQuery is entered.
func (s *BaseEsQueryStringListener) EnterPhraseFieldQuery(ctx *PhraseFieldQueryContext) {}

// ExitPhraseFieldQuery is called when production PhraseFieldQuery is exited.
func (s *BaseEsQueryStringListener) ExitPhraseFieldQuery(ctx *PhraseFieldQueryContext) {}

// EnterTermFieldQuery is called when production TermFieldQuery is entered.
func (s *BaseEsQueryStringListener) EnterTermFieldQuery(ctx *TermFieldQueryContext) {}

// ExitTermFieldQuery is called when production TermFieldQuery is exited.
func (s *BaseEsQueryStringListener) ExitTermFieldQuery(ctx *TermFieldQueryContext) {}
