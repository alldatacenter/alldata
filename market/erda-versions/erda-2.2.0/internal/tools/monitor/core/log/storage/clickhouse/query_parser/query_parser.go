// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package query_parser

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/antlr/antlr4/runtime/Go/antlr"

	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse/converter"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse/query_parser/parser"
)

type EsqsParser interface {
	Parse(esqsExpr string) EsqsParseResult
}

type EsqsParseResult interface {
	Error() error
	Sql() string
	HighlightItems() map[string][]string
}

func NewEsqsParser(fieldNameConverter converter.FieldNameConverter, defaultField string, defaultOp string, highlight bool) EsqsParser {
	return &esqsParser{
		defaultOp:          defaultOp,
		defaultField:       defaultField,
		highlight:          highlight,
		fieldNameConverter: fieldNameConverter,
	}
}

type esqsParser struct {
	defaultOp          string
	defaultField       string
	highlight          bool
	fieldNameConverter converter.FieldNameConverter
}

func (ep *esqsParser) Parse(esqsExpr string) EsqsParseResult {
	is := antlr.NewInputStream(esqsExpr)
	lexer := parser.NewEsQueryStringLexer(is)
	stream := antlr.NewCommonTokenStream(lexer, antlr.TokenDefaultChannel)
	p := parser.NewEsQueryStringParser(stream)

	listener := &esqsListener{
		defaultOp:          ep.defaultOp,
		defaultField:       ep.defaultField,
		highlight:          ep.highlight,
		fieldNameConverter: ep.fieldNameConverter,
	}
	p.AddErrorListener(listener)
	antlr.ParseTreeWalkerDefault.Walk(listener, p.Query())

	return listener
}

type esqsListener struct {
	*parser.BaseEsQueryStringListener
	*antlr.DefaultErrorListener

	defaultOp          string
	defaultField       string
	highlight          bool
	fieldNameConverter converter.FieldNameConverter

	stack          []string
	errs           []error
	highlightItems map[string][]string
}

func (l *esqsListener) Error() error {
	if len(l.errs) == 0 {
		return nil
	}
	return l.errs[0]
}

func (l *esqsListener) Sql() string {
	if len(l.stack) == 0 {
		return ""
	}
	return l.stack[0]
}

func (l *esqsListener) HighlightItems() map[string][]string {
	return l.highlightItems
}

func (l *esqsListener) SyntaxError(recognizer antlr.Recognizer, offendingSymbol interface{}, line, column int, msg string, ex antlr.RecognitionException) {
	l.errs = append(l.errs, fmt.Errorf("line "+strconv.Itoa(line)+":"+strconv.Itoa(column)+" "+msg))
}

func (l *esqsListener) ExitGroupExpression(c *parser.GroupExpressionContext) {
	if len(l.stack) < 1 {
		return
	}
	expr := l.pop()
	l.push(fmt.Sprintf("(%s)", expr))
}

func (l *esqsListener) ExitNotExpression(c *parser.NotExpressionContext) {
	if len(l.stack) < 1 {
		return
	}
	expr := l.pop()
	l.push(fmt.Sprintf("(NOT %s)", expr))
}

func (l *esqsListener) ExitAndExpression(c *parser.AndExpressionContext) {
	if len(l.stack) < 2 {
		return
	}
	right, left := l.pop(), l.pop()
	l.push(fmt.Sprintf("%s AND %s", left, right))
}

func (l *esqsListener) ExitOrExpression(c *parser.OrExpressionContext) {
	if len(l.stack) < 2 {
		return
	}
	right, left := l.pop(), l.pop()
	l.push(fmt.Sprintf("%s OR %s", left, right))
}

func (l *esqsListener) ExitDefaultOpExpression(c *parser.DefaultOpExpressionContext) {
	if len(l.stack) < 2 {
		return
	}
	right, left := l.pop(), l.pop()
	l.push(fmt.Sprintf("%s %s %s", left, l.defaultOp, right))
}

func (l *esqsListener) ExitNamedPhraseFieldQuery(c *parser.NamedPhraseFieldQueryContext) {
	l.push(l.formatExpression(
		strings.TrimRight(c.FIELD().GetText(), ":"),
		strings.Trim(c.PHRASE().GetText(), `"`)),
	)
}

func (l *esqsListener) ExitNamedTermFieldQuery(c *parser.NamedTermFieldQueryContext) {
	l.push(l.formatExpression(
		strings.TrimRight(c.FIELD().GetText(), ":"),
		c.TERM().GetText()),
	)
}

func (l *esqsListener) ExitPhraseFieldQuery(c *parser.PhraseFieldQueryContext) {
	l.push(l.formatExpression(
		l.defaultField,
		strings.Trim(c.PHRASE().GetText(), `"`)),
	)
}

func (l *esqsListener) ExitTermFieldQuery(c *parser.TermFieldQueryContext) {
	l.push(l.formatExpression(
		l.defaultField,
		c.TERM().GetText()),
	)
}

func (l *esqsListener) pop() string {
	if len(l.stack) < 1 {
		panic("stack is empty unable to pop")
	}
	expr := l.stack[len(l.stack)-1]
	l.stack = l.stack[:len(l.stack)-1]
	return expr
}

func (l *esqsListener) push(expr string) {
	l.stack = append(l.stack, expr)
}

func (l *esqsListener) formatExpression(field, value string) string {
	if l.highlight {
		l.buildHighlightItems(field, value)
	}

	field = l.fieldNameConverter.Convert(field)

	switch field {
	case l.defaultField:
		return fmt.Sprintf("%s LIKE '%%%s%%'", field, l.escapeStringValue(value))
	default:
		return fmt.Sprintf("%s='%s'", field, l.escapeStringValue(value))
	}
}

var replacer = strings.NewReplacer("'", "\\'")

func (l *esqsListener) escapeStringValue(value string) string {
	return replacer.Replace(value)
}

var highlightRegex, _ = regexp.Compile(`[^, '";=()+\[\]{}?@&<>/:\n\t\r]+`)

func (l *esqsListener) buildHighlightItems(field, value string) {
	if l.highlightItems == nil {
		l.highlightItems = map[string][]string{}
	}
	items := highlightRegex.FindAllString(value, -1)
	if len(items) == 0 {
		return
	}
	l.highlightItems[field] = append(l.highlightItems[field], items...)
}
