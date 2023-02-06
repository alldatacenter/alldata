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

package query

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/recallsong/go-utils/reflectx"
)

// AstField .
type AstField struct {
	Expr  string `json:"expr"`
	Alias string `json:"alias"`
	Key   string `json:"key"`
}

// AstStatement .
type AstStatement struct {
	Select  []*AstField `json:"select"`
	From    []string    `json:"from"`
	Where   []string    `json:"where"`
	GroupBy []string    `json:"groupby"`
	OrderBy []*OrderBy  `json:"orderby"`
	Limit   int64       `json:"limit"`
}

// OrderBy .
type OrderBy struct {
	Expr string `json:"expr"`
	Dir  string `json:"dir"`
}

// ConvertAstToStatement .
func ConvertAstToStatement(statement string) (string, error) {
	var ast AstStatement
	ast.Limit = -1
	err := json.Unmarshal(reflectx.StringToBytes(statement), &ast)
	if err != nil {
		return statement, err
	}
	if len(ast.From) <= 0 {
		return statement, fmt.Errorf("invalid from section")
	}
	b := &strings.Builder{}
	b.WriteString("SELECT ")
	for i, field := range ast.Select {
		b.WriteString(field.Expr)
		if len(field.Alias) > 0 {
			b.WriteString(" AS ")
			b.WriteString(field.Alias)
		} else if len(field.Key) > 0 {
			b.WriteString(" AS ")
			b.WriteString(field.Key)
		}
		if i < len(ast.Select)-1 {
			b.WriteString(",")
		}
	}
	b.WriteString(" FROM ")
	b.WriteString(strings.Join(ast.From, ","))
	if len(ast.Where) > 0 {
		b.WriteString(" WHERE ")
		for i, item := range ast.Where {
			b.WriteString(item)
			if i < len(ast.Where)-1 {
				b.WriteString(" AND ")
			}
		}
	}
	if len(ast.GroupBy) > 0 {
		b.WriteString(" GROUP BY ")
		for i, item := range ast.GroupBy {
			b.WriteString(item)
			if i < len(ast.GroupBy)-1 {
				b.WriteString(",")
			}
		}
	}
	if len(ast.OrderBy) > 0 {
		b.WriteString(" ORDER BY ")
		for i, item := range ast.OrderBy {
			b.WriteString(item.Expr)
			if len(item.Dir) > 0 {
				b.WriteString(" ")
				b.WriteString(item.Dir)
			}
			if i < len(ast.OrderBy)-1 {
				b.WriteString(",")
			}
		}
	}
	if ast.Limit >= 0 {
		b.WriteString(" LIMIT ")
		b.WriteString(fmt.Sprint(ast.Limit))
	}
	return b.String(), nil
}
