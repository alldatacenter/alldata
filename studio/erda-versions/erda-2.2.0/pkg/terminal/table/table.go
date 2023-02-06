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

/* example:
1. NewTable().Header([]string{"h1", "h2"}).Data([][]string{{"d1", "d2"}, {"d3","d4"}}).Flush()
>>>
H1   H2
d1   d2
d3   d4
   NewTable(WithVertical()).Header([]string{"h1", "h2"}).Data([][]string{{"d1", "d2"}, {"d3","d4"}}).Flush()
>>>
H1   d1   d3
H2   d2   d4

2. 指定输出
   NewTable(WithWriter(os.Stderr))

// NOTE:
没有设置 WithVertical 时，标题行 全部都是大写
设置    WithVertical 时，标题列 为首字母大写
*/
package table

import (
	"errors"
	"fmt"
	"io"
	"os"
	"strings"

	"github.com/WeiZhang555/tabwriter"
)

var (
	ErrDataHeaderLength = errors.New("len(header) != len(data[i])")
)

type Table struct {
	option tableOption
	data   [][]string
	header []string

	strData   []string
	strHeader string

	err error
}

type tableOption struct {
	w        io.Writer
	vertical bool
}
type OpOption func(*tableOption)

func WithWriter(w io.Writer) OpOption {
	return func(op *tableOption) {
		op.w = w
	}
}

func WithVertical() OpOption {
	return func(op *tableOption) {
		op.vertical = true
	}
}

func initOption(ops []OpOption) tableOption {
	opt := tableOption{}
	for _, op := range ops {
		op(&opt)
	}
	if opt.w == nil {
		opt.w = os.Stdout
	}
	return opt
}

func NewTable(ops ...OpOption) *Table {
	opt := initOption(ops)
	return &Table{option: opt}
}

func (t *Table) Data(data [][]string) *Table {
	if t.err != nil {
		return t
	}
	if t.header != nil && len(data) > 0 {
		if len(t.header) != len(data[0]) {
			t.err = ErrDataHeaderLength
			return t
		}
	}
	for i, d := range data {
		data[i] = replaceEmptyStr(d)
		data[i] = trimMultiLine(data[i])

	}

	t.data = data
	return t
}

func (t *Table) Header(header []string) *Table {
	if t.err != nil {
		return t
	}
	if t.data != nil && header != nil {
		if len(header) != len(t.data[0]) {
			t.err = ErrDataHeaderLength
			return t
		}
	}
	for i, e := range header {
		header[i] = strings.Title(e)
	}
	header = replaceEmptyStr(header)
	t.header = header
	return t
}

func (t *Table) Flush() error {
	if t.err != nil {
		return t.err
	}
	w := tabwriter.NewWriter(t.option.w, 0, 0, 3, ' ', 0)
	if !t.option.vertical {
		for i := range t.header {
			t.header[i] = strings.ToUpper(t.header[i])
		}

		t.strHeader = joinTab(t.header)
		for _, d := range t.data {
			t.strData = append(t.strData, joinTab(d))
		}
		if len(t.header) != 0 {
			if _, err := fmt.Fprintln(w, t.strHeader); err != nil {
				return err
			}
		}
		for _, d := range t.strData {
			if _, err := fmt.Fprintln(w, d); err != nil {
				return err
			}
		}
		w.Flush()
		return nil
	}
	// vertical table
	allData := append([][]string{t.header}, t.data...)
	if len(t.header) == 0 {
		allData = t.data
	}

	row := len(allData)       // 2
	column := len(allData[0]) // 4
	verticalData := [][]string{}
	for i := 0; i < column; i++ {
		verticalData = append(verticalData, make([]string, row))
	}

	for i := range allData {
		for j := range allData[i] {
			verticalData[j][i] = allData[i][j]
		}
	}
	for _, l := range verticalData {
		if _, err := fmt.Fprintln(w, joinTab(l)); err != nil {
			return err
		}
	}
	w.Flush()
	return nil
}

func joinTab(data []string) string {
	return strings.Join(data, "\t") + "\t"
}

func replaceEmptyStr(data []string) []string {
	r := []string{}
	for _, d := range data {
		if d == "" {
			r = append(r, "<nil>")
		} else {
			r = append(r, d)
		}
	}
	return r
}

func trimMultiLine(data []string) []string {
	r := []string{}
	for _, d := range data {
		parts := strings.SplitN(d, "\n", 2)
		if len(parts) == 0 {
			r = append(r, "")
		}
		if len(parts) >= 1 {
			r = append(r, parts[0])
		}
	}
	return r
}
