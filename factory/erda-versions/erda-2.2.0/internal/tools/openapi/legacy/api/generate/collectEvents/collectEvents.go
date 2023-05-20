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

package main

import (
	"fmt"
	"go/ast"
	"go/doc"
	"go/parser"
	"go/token"
	"io"
	"os"
	"strings"
)

func main() {
	fmt.Println("generating collectEvents.go")

	output, err := os.OpenFile("collectEvents.go", os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	defer func() {
		if err := recover(); err != nil {
			fmt.Printf("generating collectEvents.go fail: %v\n", err)
			os.Remove("collectEvents.go")
		}
	}()
	if err != nil {
		panic(err)
	}
	events := parseComments("../../../../../../apistructs")
	io.WriteString(output, "package main\n")
	io.WriteString(output, imports())
	writeEvents(output, events)
}

func parseFile(path string) []string {
	events := []string{}

	fs := token.NewFileSet()
	f, err := parser.ParseFile(fs, path, nil, 0)
	if err != nil {
		panic(err)
	}
	for _, decl := range f.Decls {
		genDecl, ok := decl.(*ast.GenDecl)
		if !ok {
			continue
		}
		if genDecl.Tok != token.TYPE {
			continue
		}
		for _, spec := range genDecl.Specs {
			ts, ok := spec.(*ast.TypeSpec)
			if !ok {
				continue
			}
			if strings.HasSuffix(ts.Name.Name, "Event") {
				events = append(events, ts.Name.Name)
			}
		}
	}
	return events
}

func parseComments(path string) map[string]string {
	fs := token.NewFileSet()
	pkgs, err := parser.ParseDir(fs, path, nil, parser.ParseComments)
	if err != nil {
		panic(err)
	}
	var docPkg *doc.Package

	for _, pkg := range pkgs {
		p := doc.New(pkg, path, 0)
		if p.Name == "apistructs" {
			docPkg = p
			break
		}
	}

	if docPkg == nil {
		panic("not found apistructs package")
	}

	r := map[string]string{}

	for _, t := range docPkg.Types {
		if !strings.HasSuffix(t.Name, "Event") {
			continue
		}
		r[t.Name] = t.Doc

	}
	return r
}

func imports() string {
	return `
import (
        . "github.com/erda-project/erda/apistructs"
)
`

}

func writeEvents(w io.Writer, events map[string]string) {
	io.WriteString(w, `
var Events = [][2]interface{} {
`)
	defer io.WriteString(w, "}")
	for name, doc := range events {
		io.WriteString(w, tab(1, "[2]interface{}{"))
		io.WriteString(w, tab(1, name))
		io.WriteString(w, "{}, ")
		io.WriteString(w, "`"+doc+"`},\n")
	}
}

func tab(n int, content string) string {
	var buf strings.Builder
	for i := 0; i < n; i++ {
		buf.WriteString("	")
	}
	buf.WriteString(content)
	return buf.String()
}
