// Code generated from EsQueryString.g4 by ANTLR 4.9. DO NOT EDIT.

package parser // EsQueryString

import "github.com/antlr/antlr4/runtime/Go/antlr"

// EsQueryStringListener is a complete listener for a parse tree produced by EsQueryStringParser.
type EsQueryStringListener interface {
	antlr.ParseTreeListener

	// EnterQuery is called when entering the query production.
	EnterQuery(c *QueryContext)

	// EnterDefaultOpExpression is called when entering the DefaultOpExpression production.
	EnterDefaultOpExpression(c *DefaultOpExpressionContext)

	// EnterAndExpression is called when entering the AndExpression production.
	EnterAndExpression(c *AndExpressionContext)

	// EnterNotExpression is called when entering the NotExpression production.
	EnterNotExpression(c *NotExpressionContext)

	// EnterOrExpression is called when entering the OrExpression production.
	EnterOrExpression(c *OrExpressionContext)

	// EnterGroupExpression is called when entering the GroupExpression production.
	EnterGroupExpression(c *GroupExpressionContext)

	// EnterFieldExpression is called when entering the FieldExpression production.
	EnterFieldExpression(c *FieldExpressionContext)

	// EnterNamedPhraseFieldQuery is called when entering the NamedPhraseFieldQuery production.
	EnterNamedPhraseFieldQuery(c *NamedPhraseFieldQueryContext)

	// EnterNamedTermFieldQuery is called when entering the NamedTermFieldQuery production.
	EnterNamedTermFieldQuery(c *NamedTermFieldQueryContext)

	// EnterPhraseFieldQuery is called when entering the PhraseFieldQuery production.
	EnterPhraseFieldQuery(c *PhraseFieldQueryContext)

	// EnterTermFieldQuery is called when entering the TermFieldQuery production.
	EnterTermFieldQuery(c *TermFieldQueryContext)

	// ExitQuery is called when exiting the query production.
	ExitQuery(c *QueryContext)

	// ExitDefaultOpExpression is called when exiting the DefaultOpExpression production.
	ExitDefaultOpExpression(c *DefaultOpExpressionContext)

	// ExitAndExpression is called when exiting the AndExpression production.
	ExitAndExpression(c *AndExpressionContext)

	// ExitNotExpression is called when exiting the NotExpression production.
	ExitNotExpression(c *NotExpressionContext)

	// ExitOrExpression is called when exiting the OrExpression production.
	ExitOrExpression(c *OrExpressionContext)

	// ExitGroupExpression is called when exiting the GroupExpression production.
	ExitGroupExpression(c *GroupExpressionContext)

	// ExitFieldExpression is called when exiting the FieldExpression production.
	ExitFieldExpression(c *FieldExpressionContext)

	// ExitNamedPhraseFieldQuery is called when exiting the NamedPhraseFieldQuery production.
	ExitNamedPhraseFieldQuery(c *NamedPhraseFieldQueryContext)

	// ExitNamedTermFieldQuery is called when exiting the NamedTermFieldQuery production.
	ExitNamedTermFieldQuery(c *NamedTermFieldQueryContext)

	// ExitPhraseFieldQuery is called when exiting the PhraseFieldQuery production.
	ExitPhraseFieldQuery(c *PhraseFieldQueryContext)

	// ExitTermFieldQuery is called when exiting the TermFieldQuery production.
	ExitTermFieldQuery(c *TermFieldQueryContext)
}
