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

package migrator

import (
	"fmt"
	"path/filepath"
	"sort"
	"strings"

	"github.com/pingcap/parser/ast"
	"gorm.io/gorm"

	"github.com/erda-project/erda/pkg/database/sqlparser/snapshot"
)

// Module is the list of Script
type Module struct {
	// Name is the name of the module
	Name string
	// Scripts contains all sql or python scripts in the module
	Scripts []*Script
	// PythonRequirementsText is the text of python requirements file in the module.
	// it is used to install dependencies of python package by "pip install -r requirements.txt -v"
	PythonRequirementsText []byte
}

// BaselineEqualCloud check baseline script schema is equal with cloud schema or not
func (m *Module) BaselineEqualCloud(tx *gorm.DB) *Equal {
	tableNames := m.BaselineTableNames()
	cloud := NewSchema()
	for _, tableName := range tableNames {
		raw := "SHOW CREATE TABLE " + tableName
		this := tx.Raw(raw)
		if err := this.Error; err != nil {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("failed to exec %s", raw),
			}
		}
		var _ig, create string
		if err := this.Row().Scan(&_ig, &create); err != nil {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("failed to Scan create table stmt, raw: %s", raw),
			}
		}
		node, err := snapshot.ParseCreateTableStmt(create)
		if err != nil {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("failed to ParseOneStmt: %s, raw: %s", create, raw),
			}
		}
		node.Accept(cloud)
	}

	return m.BaselineSchema().Equal(cloud)
}

func (m *Module) Schema() *Schema {
	schema := NewSchema()
	for _, script := range m.Scripts {
		for _, ddl := range script.DDLNodes() {
			ddl.Accept(schema)
		}
	}
	return schema
}

func (m *Module) BaselineSchema() *Schema {
	schema := NewSchema()
	for _, script := range m.Scripts {
		if !script.IsBaseline() {
			continue
		}
		for _, ddl := range script.DDLNodes() {
			ddl.Accept(schema)
		}
	}
	return schema
}

func (m *Module) TableNames() []string {
	var names []string
	for _, script := range m.Scripts {
		for _, ddl := range script.DDLNodes() {
			if create, ok := ddl.(*ast.CreateTableStmt); ok {
				if create.Table != nil {
					names = append(names, create.Table.Name.String())
				}
			}
		}
	}
	return names
}

func (m *Module) BaselineTableNames() []string {
	var names []string
	for _, script := range m.Scripts {
		if !script.IsBaseline() {
			continue
		}

		for _, ddl := range script.DDLNodes() {
			create, ok := ddl.(*ast.CreateTableStmt)
			if !ok || create.Table == nil {
				continue
			}
			names = append(names, create.Table.Name.String())
		}
	}

	return names
}

func (m *Module) Sort() {
	sort.Slice(m.Scripts, func(i, j int) bool {
		if m.Scripts[i].IsBaseline() && !m.Scripts[j].IsBaseline() {
			return true
		}
		if !m.Scripts[i].IsBaseline() && m.Scripts[j].IsBaseline() {
			return false
		}

		return strings.TrimSuffix(m.Scripts[i].GetName(), filepath.Ext(m.Scripts[i].GetName())) <
			strings.TrimSuffix(m.Scripts[j].GetName(), filepath.Ext(m.Scripts[j].GetName()))
	})
}

func (m *Module) Filenames() []string {
	var names []string
	for _, script := range m.Scripts {
		names = append(names, script.GetName())
	}
	return names
}

func (m *Module) GetScriptByFilename(filename string) (*Script, bool) {
	for _, script := range m.Scripts {
		if filename == script.GetName() {
			return script, true
		}
	}
	return nil, false
}

func (m *Module) FilterFreshBaseline(db *gorm.DB) *Module {
	var mod Module
	mod.Name = m.Name

	for _, script := range m.Scripts {
		// if the script is not baseline, skip
		if !script.IsBaseline() {
			continue
		}

		// if the script is not fresh, skip
		var cnt int64
		if db.Where(map[string]interface{}{"filename": script.GetName()}).
			First(new(HistoryModel)).Count(&cnt); db.Error == nil && cnt > 0 {
			continue
		}

		mod.Scripts = append(mod.Scripts, script)
	}

	return &mod
}
