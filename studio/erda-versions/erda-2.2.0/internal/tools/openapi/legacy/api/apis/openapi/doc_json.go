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

var DOC_JSON = apis.ApiSpec{
	Path:   "/api/openapi/swagger.json",
	Method: "GET",
	Scheme: "http",
	Custom: getDocJSON,
	Doc:    `summary: 返回 swagger.json`,
}

var (
	swaggerJSON     []byte
	swaggerJSONLock sync.Once
)

func getSwaggerJSON() []byte {
	swaggerJSONLock.Do(func() {
		f, err := os.Open("./swagger_all.json")
		defer f.Close()
		if err != nil {
			logrus.Errorf("getSwaggerJSON: %v", err)
		}
		content, err := ioutil.ReadAll(f)
		if err != nil {
			logrus.Errorf("getSwaggerJSON: %v", err)
		}
		swaggerJSON = content
	})
	return swaggerJSON
}
func getDocJSON(rw http.ResponseWriter, req *http.Request) {
	j := getSwaggerJSON()
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	rw.WriteHeader(200)
	rw.Write(j)
}
