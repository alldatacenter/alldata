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
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"io/ioutil"
	"path/filepath"
	"strings"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	_ "github.com/pingcap/tidb/types/parser_driver"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

const (
	ScriptTypeSQL    ScriptType = ".sql"
	ScriptTypePython ScriptType = ".py"
)

type ScriptType string

const (
	baseScriptLabel  = "# MIGRATION_BASE"
	baseScriptLabel2 = "-- MIGRATION_BASE"
	baseScriptLabel3 = "/* MIGRATION_BASE */"
)

// Script is the object from a SQL script file, it contains many statements.
type Script struct {
	// path from repo root, filepath.Base(script.Name) for base filename
	Name      string
	Rawtext   []byte
	Reversing []string
	Nodes     []ast.StmtNode
	Blocks    []Block
	Pending   bool
	Record    *HistoryModel
	Workdir   string
	Type      ScriptType
	isBase    bool
}

// NewScript read the local file, parse data as SQL AST nodes, and mark script IsBase or not
func NewScript(workdir, pathFromRepoRoot string) (*Script, error) {
	data, err := ioutil.ReadFile(filepath.Join(workdir, pathFromRepoRoot))
	if err != nil {
		return nil, errors.Wrap(err, "failed to ReadFile")
	}
	return NewScriptFromData(workdir, pathFromRepoRoot, data)
}

func NewScriptFromData(workdir, pathFromRepoRoot string, data []byte) (s *Script, err error) {
	data = bytes.TrimLeftFunc(data, func(r rune) bool {
		return r == ' ' || r == '\n' || r == '\t' || r == '\r'
	})
	s = &Script{
		Name:      pathFromRepoRoot,
		Rawtext:   data,
		Reversing: nil,
		Nodes:     nil,
		Pending:   true,
		Record:    nil,
		Workdir:   workdir,
		Type:      "",
		isBase: bytes.HasPrefix(data, []byte(baseScriptLabel)) ||
			bytes.HasPrefix(data, []byte(baseScriptLabel2)) ||
			bytes.HasPrefix(data, []byte(baseScriptLabel3)),
	}
	var (
		warns []error
		nodes []ast.StmtNode
	)
	switch ext := filepath.Ext(s.GetName()); {
	case strings.EqualFold(ext, string(ScriptTypeSQL)):
		s.Type = ScriptTypeSQL
		nodes, warns, err = parser.New().Parse(string(data), "", "")
		if err != nil {
			return nil, errors.Wrap(err, "failed to Parse file text data")
		}
		for _, warn := range warns {
			logrus.Fatalln(warn)
		}
	case strings.EqualFold(ext, string(ScriptTypePython)):
		s.Type = ScriptTypePython
	default:
		return nil, errors.Errorf("invalid script type: only support .sql or .py file: %s", ext)
	}

	for _, node := range nodes {
		// ignore C-Style comments
		text := strings.TrimLeftFunc(node.Text(), func(r rune) bool {
			return r == ' ' || r == '\n' || r == '\t' || r == '\r'
		})
		if strings.Contains(text, "/*!") && strings.Contains(text, "*/") {
			continue
		}
		if strings.HasPrefix(text, "/*!") {
			continue
		}
		node.SetText(text)

		switch n := node.(type) {
		case ast.DDLNode:
			s.Nodes = append(s.Nodes, node)
			s.Blocks = AppendBlock(s.Blocks, n, DDL)
		case ast.DMLNode, *ast.SetStmt:
			s.Nodes = append(s.Nodes, node)
			s.Blocks = AppendBlock(s.Blocks, n, DML)
		default:
			if s.IsBaseline() {
				continue
			}
			return nil, errors.Errorf("only support DDL and DML, filename: %s, SQL: %s", s.GetName(), node.Text())
		}
	}

	return s, nil
}

func (s *Script) DDLNodes() []ast.DDLNode {
	var results []ast.DDLNode
	for _, node := range s.Nodes {
		if strings.HasPrefix(strings.TrimPrefix(node.Text(), " "), "/*!") {
			continue
		}

		switch node.(type) {
		// ignore LockTablesStmt or UnlockTablesStmt
		case *ast.LockTablesStmt, *ast.UnlockTablesStmt:
		case ast.DDLNode:
			results = append(results, node.(ast.DDLNode))
		}
	}
	return results
}

func (s *Script) DMLNodes() []ast.StmtNode {
	var results []ast.StmtNode
	for _, node := range s.Nodes {
		if strings.HasPrefix(strings.TrimPrefix(node.Text(), " "), "/*!") {
			continue
		}

		switch node.(type) {
		// note: process SetStmt as DML
		case *ast.SetStmt, ast.DMLNode:
			results = append(results, node)
		}
	}
	return results
}

func (s *Script) Checksum() string {
	hash := sha256.New()
	hash.Write(s.Rawtext)
	return hex.EncodeToString(hash.Sum(nil))
}

func (s *Script) IsBaseline() bool {
	return s.isBase
}

func (s *Script) GetName() string {
	return filepath.Base(s.Name)
}

func (s *Script) GetData() []byte {
	return s.Rawtext
}

func (s *Script) IsEmpty() bool {
	return len(s.GetData()) == 0
}
