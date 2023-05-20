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
	"os"
	"path/filepath"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/pkg/swagger/oas3"
)

func generateDoc(onlyOpenapi bool, resultfile string) {
	var (
		apisM     = make(map[string][]*apis.ApiSpec)
		apisNames = make(map[*apis.ApiSpec]string)
	)

	for i := range APIs {
		api := &APIs[i]
		if api.Host == "" {
			continue
		}
		title := strings.Split(api.Host, ".")[0]
		apisM[title] = append(apisM[title], api)
		apisNames[api] = APINames[i]
	}

	for title, apiList := range apisM {
		var v3 = apis.NewSwagger(title)

		for _, api := range apiList {
			if api.IsValidForOperation() {
				panicError(api.AddOperationTo(v3))
			}
		}

		panicError(writeSwagger(resultfile, title, v3))
	}
}

func writeSwagger(filename, title string, v3 *openapi3.Swagger) error {
	filename = filepath.Base(filename)
	filename = strings.TrimSuffix(filename, filepath.Ext(filename)) + ".yml"
	filename = title + "-" + filename

	f, err := os.OpenFile(filename, os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	if err != nil {
		return err
	}
	defer f.Close()

	v3Data, err := oas3.MarshalYaml(v3)
	if err != nil {
		return err
	}

	_, err = f.Write(v3Data)
	return err
}

func panicError(err error) {
	if err != nil {
		panic(err)
	}
}
