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

package ddlconv_test

import (
	"testing"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/swagger/ddlconv"
)

func TestExtractCreateName(t *testing.T) {
	sql := `create table t1 (id bigint);`
	node, err := parser.New().ParseOneStmt(sql, "", "")
	if err != nil {
		t.Fatal(err)
	}
	name := ddlconv.ExtractCreateName(node.(*ast.CreateTableStmt))
	if name != "t1" {
		t.Fatalf("failed to extract name: %s", name)
	}
}
