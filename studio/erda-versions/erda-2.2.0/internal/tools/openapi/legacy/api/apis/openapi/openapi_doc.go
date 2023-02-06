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

package openapi

import (
	"io/ioutil"
	"net/http"
	"os"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var OPENAPI_DOC = apis.ApiSpec{
	Path:      "/api/openapi-doc",
	Method:    "GET",
	Scheme:    "http",
	Custom:    getOpenAPIDoc,
	Doc:       "返回 openapi 文档",
	IsOpenAPI: true,
}

var (
	openApiDocLock sync.Once
	openApiDoc     []byte
)

func getOpenAPIDocContent() []byte {
	openApiDocLock.Do(func() {
		f, err := os.Open("./swagger.json")
		defer f.Close()
		if err != nil {
			logrus.Errorf("getOpenAPIDoc: %v", err)
		}
		content, err := ioutil.ReadAll(f)
		if err != nil {
			logrus.Errorf("getOpenAPIDoc: %v", err)
		}
		openApiDoc = content
	})
	return openApiDoc
}

func getOpenAPIDoc(rw http.ResponseWriter, req *http.Request) {
	j := getOpenAPIDocContent()
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	rw.WriteHeader(200)
	rw.Write(j)
}
