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
//
// Data format in elasticsearch
// {
// 	"name":"table_name",
//     "tags":{
//         "tag1":"val1",
//         "tag2":"val2"
//     },
//     "fields":{
//         "field1":1,
//         "field2":2
//     },
//     "timestamp":1599551100000000000
// }

package tsql

import (
	"bytes"
	"fmt"
	"strings"
	"time"

	"github.com/olivere/elastic"
)

// keys define
const (
	TagsKey         = "tags."
	FieldsKey       = "fields."
	TimestampKey    = "timestamp"
	TimeKey         = "time"
	NameKey         = "name"
	DefaultLimtSize = 100
)

// TimeUnit .
type TimeUnit time.Duration

// TimeUnit values
const (
	UnsetTimeUnit = TimeUnit(0)
	Nanosecond    = TimeUnit(time.Nanosecond)
	Microsecond   = TimeUnit(time.Microsecond)
	Millisecond   = TimeUnit(time.Millisecond)
	Second        = TimeUnit(time.Second)
	Minute        = TimeUnit(time.Minute)
	Hour          = TimeUnit(time.Hour)
	Day           = TimeUnit(24 * time.Hour)
)

// ParseTimeUnit .
func ParseTimeUnit(u string) (TimeUnit, error) {
	switch u {
	case "ns":
		return Nanosecond, nil
	case "us":
		return Microsecond, nil
	case "µs":
		return Microsecond, nil
	case "μs":
		return Microsecond, nil
	case "ms":
		return Millisecond, nil
	case "s":
		return Second, nil
	case "m":
		return Minute, nil
	case "h":
		return Hour, nil
	case "day":
		return Day, nil
	}
	return UnsetTimeUnit, fmt.Errorf("invaild time unit '%s'", u)
}

// ConvertTimestamp .
func ConvertTimestamp(t int64, from, to TimeUnit) int64 {
	if to == UnsetTimeUnit {
		return t
	}
	if from == UnsetTimeUnit {
		from = Nanosecond
	}
	return t * int64(from) / int64(to)
}

// ResultSet .
type ResultSet struct {
	Total    int64
	Interval int64
	Columns  []*Column
	Rows     [][]interface{}
}

// Column .
type Column struct {
	Type  string
	Name  string
	Key   string
	Flag  ColumnFlag
	Extra interface{}
}

// ColumnFlag .
type ColumnFlag int32

// ColumnFlag values
const (
	ColumnFlagNone = ColumnFlag(0)
	ColumnFlagHide = ColumnFlag(1 << (iota - 1))
	ColumnFlagName
	ColumnFlagTimestamp
	ColumnFlagTag
	ColumnFlagField
	ColumnFlagLiteral
	ColumnFlagFunc
	ColumnFlagAgg
	ColumnFlagGroupBy
	ColumnFlagOrderBy

	ColumnFlagGroupByInterval
	ColumnFlagGroupByRange
)

func (f ColumnFlag) String() string {
	if f == ColumnFlagNone {
		return "node"
	}
	buf := &bytes.Buffer{}
	if f&ColumnFlagHide != 0 {
		buf.WriteString("hide|")
	}
	if f&ColumnFlagName != 0 {
		buf.WriteString("name|")
	}
	if f&ColumnFlagTimestamp != 0 {
		buf.WriteString("timestamp|")
	}
	if f&ColumnFlagTag != 0 {
		buf.WriteString("tag|")
	}
	if f&ColumnFlagField != 0 {
		buf.WriteString("field|")
	}
	if f&ColumnFlagLiteral != 0 {
		buf.WriteString("literal|")
	}
	if f&ColumnFlagFunc != 0 {
		buf.WriteString("func|")
	}
	if f&ColumnFlagAgg != 0 {
		buf.WriteString("agg|")
	}
	if f&ColumnFlagGroupBy != 0 {
		buf.WriteString("groupby")
		if f&(ColumnFlagGroupByInterval|ColumnFlagGroupByRange) != 0 {
			buf.WriteString(":")
			var group []string
			if f&ColumnFlagGroupByInterval != 0 {
				group = append(group, "interval")
			}
			if f&ColumnFlagGroupByRange != 0 {
				group = append(group, "range")
			}
			buf.WriteString(strings.Join(group, ","))
		}
		buf.WriteString("|")
	}
	if f&ColumnFlagOrderBy != 0 {
		buf.WriteString("orderby|")
	}
	return string(buf.Bytes()[0 : buf.Len()-1])
}

// Source .
type Source struct {
	Database string
	Name     string
}

// Query .
type Query interface {
	Sources() []*Source
	SearchSource() *elastic.SearchSource
	BoolQuery() *elastic.BoolQuery
	SetAllColumnsCallback(fn func(start, end int64, sources []*Source) ([]*Column, error))
	ParseResult(resp *elastic.SearchResult) (*ResultSet, error)
	Context() Context
}

// ErrNotSupportNonQueryStatement .
var ErrNotSupportNonQueryStatement = fmt.Errorf("not support non query statement")

// Parser .
type Parser interface {
	SetParams(params map[string]interface{}) Parser
	SetFilter(filter *elastic.BoolQuery) Parser
	SetOriginalTimeUnit(unit TimeUnit) Parser
	SetTargetTimeUnit(unit TimeUnit) Parser
	SetTimeKey(key string) Parser
	SetMaxTimePoints(points int64) Parser
	ParseQuery() ([]Query, error)
	ParseRawQuery() ([]*Source, *elastic.BoolQuery, *elastic.SearchSource, error)
}

// Creator .
type Creator func(start, end int64, stmt string) Parser

// Parsers .
var Parsers = map[string]Creator{}

// RegisterParser .
func RegisterParser(name string, c Creator) {
	Parsers[name] = c
}

// New .
func New(start, end int64, ql, stmt string) Parser {
	creator := Parsers[ql]
	if creator != nil {
		return creator(start, end, stmt)
	}
	return nil
}
