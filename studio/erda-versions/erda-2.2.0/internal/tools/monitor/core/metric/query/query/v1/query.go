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

package queryv1

import (
	"fmt"
	"strings"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/chartmeta"
	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
)

// Queryer .
type Queryer interface {
	QueryWithFormatV1(qlang, statement, format string, langCodes i18n.LanguageCodes) (*Response, error)
}

// Response .
type Response struct {
	Total   int64    `json:"total"`
	Metrics []string `json:"metrics"`
	Elapsed struct {
		Search time.Duration `json:"search"`
	} `json:"elapsed"`
	Data     interface{} `json:"data"`
	Interval float64     `json:"interval"`
	details  string
	req      *Request
}

// Request .
func (r *Response) Request() *Request { return r.req }

// Unmarshal .
func (r *Response) Unmarshal(out interface{}) error {
	err := Unmarshal(r.Data, out)
	if err != nil {
		return fmt.Errorf("fail to unmarshal: %s", err)
	}
	return nil
}

// Details .
func (r *Response) Details() string { return r.details }

// Request .
type Request struct {
	Name       string
	Metrics    []string
	Start, End int64 // ms
	TimeAlign  TimeAlign
	Select     []*Column
	Where      []*query.Filter
	GroupBy    []*Group
	OrderBy    []*Order
	Limit      []int
	Debug      bool
	Aggregate  *Column
	ExistKeys  map[string]struct{}
	Columns    map[string]*Column

	TimeKey          string        // Specify the time field.
	OriginalTimeUnit tsql.TimeUnit // The unit of the time field.

	EndOffset        int64
	Interval         float64
	Points           float64
	AlignEnd         bool
	ClusterNames     []string
	LegendMap        map[string]*chartmeta.DataMeta // Legend name -> Legend display name
	ChartType        string
	Trans            bool
	TransGroup       bool
	DefaultNullValue interface{}
}

// Range .
func (r *Request) Range(conv bool) (int64, int64) {
	if conv && r.OriginalTimeUnit != tsql.UnsetTimeUnit {
		return r.Start * int64(time.Millisecond) / int64(r.OriginalTimeUnit),
			r.End * int64(time.Millisecond) / int64(r.OriginalTimeUnit)
	}
	return r.Start * int64(time.Millisecond), r.End * int64(time.Millisecond)
}

// InitTimestamp .
func (r *Request) InitTimestamp(start, end, timestamp, latest string) (err error) {
	st, et, err := query.ParseTimeRange(start, end, timestamp, latest)
	if err != nil {
		return err
	}
	r.Start, r.End = st, et
	return nil
}

// TimeAlign .
type TimeAlign string

// TimeAlign .
const (
	TimeAlignUnset TimeAlign = ""
	TimeAlignNone  TimeAlign = "none"
	TimeAlignAuto  TimeAlign = "auto"
)

// Group .
type Group struct {
	ID         string
	Property   Property //
	Limit      int
	Sort       *Order
	Filters    []*GroupFilter
	ColumnAggs map[string]bool
}

// Order .
type Order struct {
	ID       string
	Property Property      //
	FuncName string        //
	Params   []interface{} //
	Sort     string        //
}

// Ascending .
func (o *Order) Ascending() bool { return o.Sort == "ASC" }

// Column .
type Column struct {
	ID       string
	Property Property
	FuncName string        //
	Params   []interface{} //
	Function Function
}

// Property .
type Property struct {
	Name   string //
	Key    string
	Script string
}

// Normalize .
func (p *Property) Normalize(typ string) {
	if !p.IsScript() {
		if len(p.Key) <= 0 {
			p.Key = NormalizeKey(p.Name, typ)
		}
		p.Name = NormalizeName(p.Name)
	} else {
		// 去除最外层的 括号
		for strings.HasPrefix(p.Script, "(") && strings.HasSuffix(p.Script, ")") {
			p.Script = p.Script[1 : len(p.Script)-1]
		}
	}
}

// GetExpression .
func (p *Property) GetExpression() string {
	if strings.HasPrefix(p.Key, "{") && strings.HasSuffix(p.Key, "}") {
		return p.Key[1 : len(p.Key)-1]
	}
	return ""
}

// IsScript .
func (p *Property) IsScript() bool { return len(p.Script) > 0 }

// GroupFilter .
type GroupFilter struct {
	Column               //
	Operator string      //
	Value    interface{} //
}

// Function .
type Function interface {
	Aggregations(ctx *Context, flags ...Flag) ([]*Aggregation, error)
	Handle(ctx *Context, aggs elastic.Aggregations) (interface{}, error)
	SupportOrderBy() bool
	SupportReduce() bool
}

// Flag .
type Flag int32

// Flag .
const (
	FlagColumnFunc = Flag(0)
	FlagOrderBy    = Flag(1)
	FlagReduce     = Flag(2)
)

// IsOrderBy .
func (f *Flag) IsOrderBy() bool {
	return *f == FlagOrderBy
}

// IsReduce .
func (f *Flag) IsReduce() bool {
	return *f == FlagReduce
}

// Flags .
type Flags []Flag

// IsOrderBy .
func (fs Flags) IsOrderBy() bool {
	for _, f := range fs {
		if f.IsOrderBy() {
			return true
		}
	}
	return false
}

// IsReduce .
func (fs Flags) IsReduce() bool {
	for _, f := range fs {
		if f.IsReduce() {
			return true
		}
	}
	return false
}

// Aggregation .
type Aggregation struct {
	ID          string
	Aggregation elastic.Aggregation
}

// Context .
type Context struct {
	Req        *Request
	Source     *elastic.SearchSource
	Resp       *elastic.SearchResult
	Attributes map[string]interface{}
	ChartMeta  *chartmeta.ChartMeta
	T          i18n.Translator
	Lang       i18n.LanguageCodes
}

// QLParser query language parser
type QLParser interface {
	Parse(statement string) (*Request, error)
}

// Parsers .
var Parsers = map[string]QLParser{}

// RegisterQueryParser .
func RegisterQueryParser(name string, parser QLParser) {
	Parsers[name] = parser
}

// Formater response formater
type Formater interface {
	Format(ctx *Context, param string) (interface{}, error)
}

// Formats .
var Formats = map[string]Formater{}

// RegisterResponseFormater .
func RegisterResponseFormater(name string, formater Formater) {
	Formats[name] = formater
}

// ValueRange .
type ValueRange struct {
	From interface{}
	To   interface{}
}
