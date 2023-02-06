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
	"testing"

	"gotest.tools/assert"

	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse/converter"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func Test_Parse(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags": {Type: "Map(String,String)"},
			},
		}, nil),
		"content", "AND", true)

	result := parser.Parse("da-ta:\"中 文\" (not v.al:zz OR yy:dd) AND hello AND \"hi\"")
	assert.NilError(t, result.Error())
	want := "da-ta='中 文' AND ((NOT v.al='zz') OR yy='dd') AND content LIKE '%hello%' AND content LIKE '%hi%'"
	sql := result.Sql()
	assert.Equal(t, sql, want)
}

func Test_Parse_Empty_Input(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags": {Type: "Map(String,String)"},
			},
		}, nil),
		"content", "AND", true)

	result := parser.Parse("  ")
	assert.NilError(t, result.Error())
	want := ""
	sql := result.Sql()
	assert.Equal(t, sql, want)
}

func Test_Parse_NonExistTagFields(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags": {Type: "Map(String,String)"},
			},
		}, nil),
		"content", "AND", true)

	result := parser.Parse("tags.aaa:bbb")
	assert.NilError(t, result.Error())
	want := "tags['aaa']='bbb'"
	sql := result.Sql()
	assert.Equal(t, sql, want)
}

func Test_Parse_NonExistTagFields_ExistsMapper(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags":     {Type: "Map(String,String)"},
				"tags.ccc": {Type: "String"},
			},
		}, map[string]string{
			"tags.aaa": "tags.ccc",
		}),
		"content", "AND", true)

	result := parser.Parse("tags.aaa:bbb")
	assert.NilError(t, result.Error())
	want := "tags.ccc='bbb'"
	sql := result.Sql()
	assert.Equal(t, sql, want)
}

func Test_Parse_NonExistTagAndMapperFields(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags": {Type: "Map(String,String)"},
			},
		}, map[string]string{
			"tags.aaa": "tags.ccc",
		}),
		"content", "AND", true)

	result := parser.Parse("tags.aaa:bbb")
	assert.NilError(t, result.Error())
	want := "tags['aaa']='bbb'"
	sql := result.Sql()
	assert.Equal(t, sql, want)
}

func Test_Parse_ExistTagFields(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags":          {Type: "Map(String,String)"},
				"tags.trace_id": {Type: "String"},
			},
		}, nil),
		"content", "AND", true)

	result := parser.Parse("tags.trace_id:bbb")
	assert.NilError(t, result.Error())
	want := "tags.trace_id='bbb'"
	sql := result.Sql()
	assert.Equal(t, sql, want)
}

func Test_Parse_EscapeValue(t *testing.T) {
	parser := NewEsqsParser(
		converter.NewFieldNameConverter(&loader.TableMeta{
			Columns: map[string]*loader.TableColumn{
				"tags": {Type: "Map(String,String)"},
			},
		}, nil),
		"content", "AND", true)

	result := parser.Parse("'hello'")
	assert.NilError(t, result.Error())
	want := `content LIKE '%\'hello\'%'`
	sql := result.Sql()
	assert.Equal(t, sql, want)
}
