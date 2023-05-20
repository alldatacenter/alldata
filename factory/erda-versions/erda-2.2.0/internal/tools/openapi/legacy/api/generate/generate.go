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
	"io"
	"net/url"
	"os"
	"sort"
	"strings"
	"text/template"

	"github.com/erda-project/erda/internal/pkg/innerdomain"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/pkg/strutil"
)

//go:generate go run collect/collect.go
//go:generate go run collectEvents/collectEvents.go
//go:generate go run generate.go validate.go generate_doc.go generate_event_doc.go collectAPIs.go collectEvents.go
func main() {
	fmt.Println("generating api.go")
	fmt.Println("generating swagger.json")
	fmt.Println("generating swagger_all.json")
	fmt.Println("generating events_all.json")
	var buf strings.Builder
	trivialBegin(&buf)
	for idx, api := range APIs {
		if err := validate(&api); err != nil {
			errStr := fmt.Sprintf("validate fail[%s]: %v", api.Path, err)
			panic(errStr)
		}
		marathon, k8s, port, err := convertHost(&api)
		if err != nil {
			errStr := fmt.Errorf("failed to convert host: api: %+v, err: %v", api, err)
			panic(errStr)
		}
		if port == "" {
			port = "0"
		}

		SpecTemplate.Execute(&buf, map[string]interface{}{
			"Path":            quote(api.Path),
			"BackendPath":     quote(api.BackendPath),
			"Host":            quote(api.Host),
			"Method":          quote(strings.ToUpper(api.Method)),
			"Scheme":          strings.ToUpper(api.Scheme),
			"Custom":          APINames[idx] + ".Custom",
			"CustomResponse":  APINames[idx] + ".CustomResponse",
			"Audit":           APINames[idx] + ".Audit",
			"NeedDesensitize": api.NeedDesensitize,
			"CheckLogin":      api.CheckLogin,
			"TryCheckLogin":   api.TryCheckLogin,
			"CheckToken":      api.CheckToken,
			"ChunkAPI":        api.ChunkAPI,
			"CheckBasicAuth":  api.CheckBasicAuth,
			"MarathonHost":    quote(marathon),
			"K8SHost":         quote(k8s),
			"Port":            port,
		})
	}
	trivialEnd(&buf)
	f, err := os.OpenFile("../api.go", os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	if err != nil {
		panic(err)
	}
	f.WriteString(buf.String())
	generateDoc(true, "../swagger.json")
	generateDoc(false, "../swagger_all.json")
	generateEventDoc(false, "../events_all.json")

	fmt.Println("rm generated_desc.go")
	os.Remove("../../../../../../apistructs/generated_desc.go")
}

var SpecTemplate = template.Must(template.New("spec").Parse(`	{NewPath({{.Path}}), NewPath({{.BackendPath}}), {{.Host}}, {{.Scheme}}, {{.Method}}, {{.Custom}}, {{.CustomResponse}}, {{.Audit}}, {{.NeedDesensitize}}, {{.CheckLogin}}, {{.TryCheckLogin}}, {{.CheckToken}}, {{.CheckBasicAuth}}, {{.ChunkAPI}}, {{.MarathonHost}}, {{.K8SHost}}, {{.Port}}},
`))

func convertHost(api *apis.ApiSpec) (marathon, k8s, port string, err error) {
	if api.Custom != nil {
		return
	}
	if api.Host == "" && api.K8SHost == "" {
		err = fmt.Errorf("failed to convert host: neither Host nor K8SHost provided")
		return
	}
	if api.Host != "" {
		marathon, k8s, port, err = convertHostAux(api.Scheme, api.Host)
		if err == innerdomain.ErrNoLegacyK8SAddr {
			if api.K8SHost == "" {
				return
			}
			var u *url.URL
			u, err = url.Parse(strutil.Concat("http://", api.Host))
			if err != nil {
				return
			}
			marathon = u.Hostname()
			port = u.Port()
			_, k8s, _, err = convertHostAux(api.Scheme, api.K8SHost)
			return
		}
		return
	}
	if api.K8SHost != "" {
		marathon, k8s, port, err = convertHostAux(api.Scheme, api.K8SHost)
		return
	}
	panic("shouldn't be here")
}

// convertHost("http", "xxxx.com:8888") => ("marathonhost", "k8shost", "8888", nil)
func convertHostAux(scheme, host string) (string, string, string, error) {
	u, err := url.Parse(strutil.Concat(scheme, "://", host))
	if err != nil {
		return "", "", "", err
	}
	domain, err := innerdomain.Parse(u.Hostname())
	if err != nil {
		return "", "", "", err
	}
	marathon, err := domain.Marathon()
	if err != nil {
		return "", "", "", err
	}
	k8s, err := domain.K8S()
	if err != nil {
		return "", "", "", err
	}
	port := u.Port()
	if port == "" {
		return "", "", "", fmt.Errorf("port unassigned")
	}
	return marathon, k8s, u.Port(), nil
}
func trivialBegin(w io.Writer) {
	io.WriteString(w, "//generated file, DO NOT EDIT\n")
	io.WriteString(w, "package api\n")
	io.WriteString(w, "import (\n")

	importLines := []string{
		`. "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"`,
		`. "github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"`,
	}
	for _, pkgPath := range PkgPaths {
		importLines = append(importLines, pkgPath)
	}
	importLines = strutil.DedupSlice(importLines, true)
	sort.Strings(importLines)

	for _, line := range importLines {
		io.WriteString(w, fmt.Sprintf("    %s", line))
		io.WriteString(w, "\n")
	}
	io.WriteString(w, ")\n")

	io.WriteString(w, "var API APIs = APIs{\n")
}

func trivialEnd(w io.Writer) {
	io.WriteString(w, "}")
}

func quote(s string) string {
	return "\"" + s + "\""
}

func method(m string) string {
	if m == "" {
		return "ALL"
	}
	return m
}
