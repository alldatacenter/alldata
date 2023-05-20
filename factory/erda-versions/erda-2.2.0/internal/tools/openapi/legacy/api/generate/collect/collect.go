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
	"go/parser"
	"go/token"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strings"

	"github.com/erda-project/erda/pkg/strutil"
)

func main() {
	fmt.Println("generating collectAPIs.go")
	output, err := os.OpenFile("collectAPIs.go", os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	defer func() {
		if err := recover(); err != nil {
			fmt.Printf("generating collectAPIs.go fail: %v", err)
			os.Remove("collectAPIs.go")
		}
	}()
	if err != nil {
		panic(err)
	}
	astPkgs := parseDirRecursive("../apis/")
	specs := []string{}
	specPkgPathMap := make(map[string]string)
	for _, astPkg := range astPkgs {
		for fname, f := range astPkg.Files {
			pkgPath, spec := parseFile(fname, f)
			specs = append(specs, spec)
			specPkgPathMap[spec] = pkgPath
		}
	}
	// make specs in stable order
	sort.Strings(specs)

	io.WriteString(output, "package main\n")
	io.WriteString(output, imports(specPkgPathMap))
	writeSpecs(output, specs, specPkgPathMap)
	writeSpecNames(output, specs)
	writePkgPaths(output, specPkgPathMap)
}

func parseDirRecursive(path string) map[string]*ast.Package {
	pkgs := make(map[string]*ast.Package)
	filepath.Walk(path, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			subPkgs := parseDir(path)
			for _, subPkg := range subPkgs {
				pkgs[subPkg.Name] = subPkg
			}
		}
		return nil
	})
	return pkgs
}

func parseDir(path string) map[string]*ast.Package {
	fs := token.NewFileSet()
	pkgs, err := parser.ParseDir(fs, path, func(info os.FileInfo) bool {
		if info.Name() == "rawspec.go" {
			return false
		}
		if strings.HasSuffix(info.Name(), "_test.go") {
			return false
		}
		if strings.HasPrefix(info.Name(), "notspec_") {
			return false
		}
		return true
	}, 0)
	if err != nil {
		panic(err)
	}
	return pkgs
}

const (
	dirPrefix        = "../apis/"
	apiPkgPathPrefix = "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

// return package, var
// fname: ../apis/runner_task_collect_log.go       -> . "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
// fname: ../apis/pipeline/a/b/pipeline_cancel.go  -> pipeline_a_b "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
func parseFile(fname string, f *ast.File) (pkgPath string, name string) {
	// fnameWithoutPrefix:
	//   runner_task_collect_log.go
	//   pipeline/a/b/pipeline_cancel.go
	fnameWithoutPrefix := strings.TrimPrefix(fname, dirPrefix)
	idx := strings.LastIndex(fnameWithoutPrefix, "/")
	pkgAlias := "."
	if idx > 0 {
		// pkgDirWithoutPrefix:
		//    pipeline/a/b
		pkgDirWithoutPrefix := fnameWithoutPrefix[0:idx]
		//    pipeline/a/b -> pipeline_a_b
		pkgAlias = strings.ReplaceAll(pkgDirWithoutPrefix, "/", "_")
		//    pipeline_a_b "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis/pipeline/a/b"
		pkgPath = fmt.Sprintf(`%s "%s"`, pkgAlias, strutil.Concat(apiPkgPathPrefix, "/", pkgDirWithoutPrefix))
	} else {
		// . "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
		pkgPath = fmt.Sprintf(`%s "%s"`, pkgAlias, strutil.Concat(apiPkgPathPrefix))
	}
	defer func() {
		if pkgAlias != "." {
			name = strutil.Concat(pkgAlias, ".", name)
		}
	}()
	for _, decl := range f.Decls {
		gen, ok := decl.(*ast.GenDecl)
		if !ok {
			continue
		}
		if gen.Tok != token.VAR {
			continue
		}
		for _, spec := range gen.Specs {
			val, ok := spec.(*ast.ValueSpec)
			if !ok {
				continue
			}
			for idx, v := range val.Values {
				v_, ok := v.(*ast.CompositeLit)
				if !ok {
					continue
				}
				var name string
				switch v_.Type.(type) {
				case *ast.Ident:
					ident := v_.Type.(*ast.Ident)
					if ident.Name != "ApiSpec" {
						continue
					}
					name = val.Names[idx].Name
				case *ast.SelectorExpr:
					expr := v_.Type.(*ast.SelectorExpr)
					if expr.Sel.Name != "ApiSpec" || expr.X.(*ast.Ident).Name != "apis" {
						continue
					}
					name = val.Names[idx].Name
				}
				if name == "" {
					continue
				}
				if strings.Title(name) != name {
					errStr := fmt.Sprintf("first letter should be uppercase to output it, [%s]", name)
					panic(errStr)
				}
				return pkgPath, name
			}
		}
	}
	errStr := fmt.Sprintf("not found ApiSpec in %s", fname)
	panic(errStr)
}

func imports(specPkgPathMap map[string]string) string {
	var importLines []string
	for _, pkgPath := range specPkgPathMap {
		importLines = append(importLines, pkgPath)
	}
	importLines = strutil.DedupSlice(importLines, true)
	sort.Strings(importLines)

	results := []string{
		`import (`,
	}
	for _, line := range importLines {
		results = append(results, tab(1, line))
	}
	results = append(results, `)`)

	return strings.Join(results, "\n")
}

func writeSpecs(w io.Writer, specs []string, specPkgMap map[string]string) {
	io.WriteString(w, `
var APIs = []ApiSpec{
`)
	defer io.WriteString(w, "}")
	for _, spec := range specs {
		io.WriteString(w, tab(1, spec))
		io.WriteString(w, ",\n")
	}

}

func writeSpecNames(w io.Writer, specs []string) {
	io.WriteString(w, `
var APINames = []string{
`)
	defer io.WriteString(w, "}")
	for _, spec := range specs {
		io.WriteString(w, tab(1, "\""+spec+"\""))
		io.WriteString(w, ",\n")
	}
}

func writePkgPaths(w io.Writer, specPkgPathMap map[string]string) {
	io.WriteString(w, `
var PkgPaths = []string{
`)
	defer io.WriteString(w, "}")
	var pkgPaths []string
	for _, pkgPath := range specPkgPathMap {
		pkgPaths = append(pkgPaths, pkgPath)
	}
	pkgPaths = strutil.DedupSlice(pkgPaths, true)
	sort.Strings(pkgPaths)
	for _, pkgPath := range pkgPaths {
		io.WriteString(w, tab(1, strutil.Concat("`", pkgPath, "`")))
		io.WriteString(w, ",\n")
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
