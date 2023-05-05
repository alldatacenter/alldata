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
	"encoding/json"
	"flag"
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"io"
	"os"

	"github.com/erda-project/erda/pkg/strutil"
)

// extract struct comment, and generate methods for them
var (
	pkgPath string
	pkgName string
)

func init() {
	flag.StringVar(&pkgPath, "pkg-path", ".", "package path")
	flag.StringVar(&pkgName, "pkg-name", "nil", "package name")
	flag.Parse()
}

func main() {
	//               map[structname][fieldname]comment
	structDescSet := map[string]map[string]string{}

	fset := token.NewFileSet()
	pkg, err := parser.ParseDir(fset, ".", nil, parser.ParseComments)
	if err != nil {
		panic(err)
	}
	// name=pkgname
	for name, fs := range pkg {
		if name != pkgName {
			continue
		}
		for _, f := range fs.Files {
			for _, c := range f.Decls {
				gendecl, ok := c.(*ast.GenDecl)
				if !ok {
					continue
				}
				for _, spec := range gendecl.Specs {
					typespec, ok := spec.(*ast.TypeSpec)
					if !ok {
						continue
					}
					s, ok := typespec.Type.(*ast.StructType)
					if !ok {
						continue
					}
					name := typespec.Name.Name
					if name == "" {
						continue
					}
					for _, field := range s.Fields.List {
						if len(field.Names) == 0 || field.Names[0].Name == "" {
							continue
						}
						if field.Doc.Text() != "" {
							if structDescSet[name] == nil {
								structDescSet[name] = map[string]string{}
							}

							structDescSet[name][field.Names[0].Name] = field.Doc.Text()
						}
					}
				}
			}
		}
	}
	genDescFile(structDescSet)
	fmt.Println("generated generated_desc.go")
}

var structDescMap = strutil.TrimLeft(`
var structDescMap = map[string]map[string]string%s
`)

var descfunc = strutil.TrimLeft(`
func (%s) Desc_%s(s string) string {
        if structDescMap["%s"] == nil {
		return ""
	}
	return structDescMap["%s"][s]
}

`)

func genDescFile(structDescSet map[string]map[string]string) {
	output, err := os.OpenFile("generated_desc.go", os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	r, err := json.Marshal(structDescSet)
	if err != nil {
		panic(err)
	}
	io.WriteString(output, fmt.Sprintf("package %s\n", pkgName))
	io.WriteString(output, fmt.Sprintf(structDescMap, string(r)))
	for structname := range structDescSet {
		io.WriteString(output, fmt.Sprintf(descfunc, structname, structname, structname, structname))
	}
}
