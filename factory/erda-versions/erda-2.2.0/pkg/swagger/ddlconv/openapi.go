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

package ddlconv

import (
	"encoding/json"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	log "github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
)

type DDLConverter struct {
	Openapi    string            `json:"openapi" yaml:"openapi"`
	Info       map[string]string `json:"info" yaml:"info"`
	Paths      struct{}          `json:"paths" yaml:"paths"`
	Components Components        `json:"components" yaml:"components"`
}

func New(sql ...string) (*DDLConverter, error) {
	o := &DDLConverter{
		Openapi: "3.0.1",
		Info: map[string]string{
			"title":   "database-schemas",
			"version": "1.0",
		},
		Paths: struct{}{},
		Components: struct {
			Schemas map[string]OpenapiSchema `json:"schemas" yaml:"schemas"`
		}{Schemas: make(map[string]OpenapiSchema, 0)},
	}

	if len(sql) == 0 {
		return o, nil
	}

	for _, s := range sql {
		if _, err := o.WriteString(s); err != nil {
			return nil, err
		}
	}

	return o, nil
}

func (o *DDLConverter) Write(sql []byte) (int, error) {
	return o.WriteString(string(sql))
}

func (o *DDLConverter) WriteString(sql string) (int, error) {
	p := parser.New()
	nodes, _, err := p.Parse(sql, "", "")
	if err != nil {
		return 0, err
	}
	for _, node := range nodes {
		node.Accept(o)
	}

	return len(sql), nil
}

func (o *DDLConverter) Enter(in ast.Node) (ast.Node, bool) {
	switch in.(type) {
	case ast.DDLNode:
		return o.enterDDL(in.(ast.DDLNode))
	default:
		return in, true
	}
}

func (o *DDLConverter) enterDDL(in ast.DDLNode) (out ast.Node, skip bool) {
	out = in
	skip = true

	switch in.(type) {
	case *ast.DropTableStmt:
		stmt := in.(*ast.DropTableStmt)
		names := ExtractDropNames(stmt)
		for _, name := range names {
			delete(o.Components.Schemas, name)
		}

	case *ast.CreateTableStmt:
		stmt := in.(*ast.CreateTableStmt)
		name := ExtractCreateName(stmt)
		if _, ok := o.Components.Schemas[name]; ok {
			return
		}

		var schema OpenapiSchema
		schema.Name = name
		schema.Type = "object"
		schema.Properties = make(map[string]*openapiSchemaProperty, 0)
		for _, col := range stmt.Cols {
			var property openapiSchemaProperty
			property.XRaw = ExtractColName(col)
			property.Name = snake2LowerCamel(property.XRaw)
			format := ExtractColType(col)
			property.Type = mysqlType2OpenapiType(format)
			property.Example = genExample(property.XRaw, property.Type)
			property.Description = ExtractColComment(col)
			property.XSource = schema.Name
			schema.Properties[property.Name] = &property
		}
		o.Components.Schemas[schema.Name] = schema

	case *ast.RenameTableStmt:
		stmt := in.(*ast.RenameTableStmt)
		newName, oldName := ExtractRename(stmt)
		schema, ok := o.Components.Schemas[oldName]
		if !ok {
			return
		}
		schema.Name = newName
		delete(o.Components.Schemas, oldName)
		o.Components.Schemas[newName] = schema // 如果新名称已存在, 则会覆盖

	case *ast.AlterTableStmt:
		stmt := in.(*ast.AlterTableStmt)
		schemaName := ExtractAlterTableName(stmt)
		schema, ok := o.Components.Schemas[schemaName]
		if !ok {
			return
		}

		for _, spec := range stmt.Specs {
			switch spec.Tp {
			case ast.AlterTableAddColumns:
				var property openapiSchemaProperty
				property.XSource = schemaName
				property.XRaw = ExtractAlterTableAddColName(spec)
				property.Name = snake2LowerCamel(property.XRaw)
				format := ExtractAlterTableAddColType(spec)
				property.Type = mysqlType2OpenapiType(format)
				property.Example = genExample(property.XRaw, property.Type)
				property.Description = ExtractAlterTableAddColComment(spec)
				schema.Properties[property.Name] = &property

			case ast.AlterTableChangeColumn:
				oldRaw := ExtractAlterTableChangeColOldName(spec)
				property, ok := schema.getProperty(oldRaw)
				if !ok {
					continue
				}
				if raw := ExtractAlterTableChangeColNewName(spec); raw != "" {
					property.XRaw = raw
					property.Name = snake2LowerCamel(property.XRaw)
				}
				if format := ExtractAlterTableChangeColType(spec); format != "" {
					// property.Format = format
					property.Type = mysqlType2OpenapiType(format)
				}
				property.Example = genExample(property.XRaw, property.Type)
				if desc := ExtractAlterTableChangeColComment(spec); desc != "" {
					property.Description = desc
				}
				delete(schema.Properties, oldRaw)
				schema.Properties[property.Name] = property

			case ast.AlterTableModifyColumn:
				raw := ExtractAlterTableModifyColName(spec)
				property, ok := schema.getProperty(raw)
				if !ok {
					continue
				}
				if format := ExtractAlterTableModifyColType(spec); format != "" {
					// property.Format = format
					property.Type = mysqlType2OpenapiType(format)
				}
				property.Example = genExample(property.XRaw, property.Type)
				if desc := ExtractAlterTableModifyColComment(spec); desc != "" {
					property.Description = desc
				}

			case ast.AlterTableDropColumn:
				raw := ExtractAlterTableDropColName(spec)
				property, ok := schema.getProperty(raw)
				if !ok {
					continue
				}
				delete(schema.Properties, property.Name)

			default:
				log.Warnf("unsupport alter table spec type: %v", spec.Tp)
			}
		}
	}

	return
}

func (o *DDLConverter) Leave(in ast.Node) (ast.Node, bool) {
	return in, false
}

func (o *DDLConverter) YAML() []byte {
	if o == nil {
		return nil
	}
	data, err := yaml.Marshal(*o)
	if err != nil {
		return nil
	}
	return data
}

func (o *DDLConverter) JSON() []byte {
	if o == nil {
		return nil
	}

	data, err := json.MarshalIndent(*o, "", "  ")
	if err != nil {
		return nil
	}
	return data
}

type OpenapiSchema struct {
	Name       string                            `json:"-" yaml:"-"`
	Type       string                            `json:"type" yaml:"type"`
	Properties map[string]*openapiSchemaProperty `json:"properties" yaml:"properties"`
}

func (s OpenapiSchema) getProperty(raw string) (*openapiSchemaProperty, bool) {
	for _, v := range s.Properties {
		if v.XRaw == raw {
			return v, true
		}
	}
	return nil, false
}

type openapiSchemaProperty struct {
	Name string `json:"-" yaml:"-"`
	Type string `json:"type" yaml:"type"`
	// Format      string      `json:"format,omitempty" yaml:"format"`
	Example     interface{} `json:"example" yaml:"example"`
	Description string      `json:"description" yaml:"description"`
	XRaw        string      `json:"x-dice-raw" yaml:"x-dice-raw"`
	XSource     string      `json:"x-dice-source" yaml:"x-dice-source"`
}

type Components struct {
	Schemas map[string]OpenapiSchema `json:"schemas" yaml:"schemas"`
}
