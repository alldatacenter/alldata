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

var OPENAPI_EVENT_DOC = apis.ApiSpec{
	Path:      "/api/openapi-event-doc",
	Method:    "GET",
	Scheme:    "http",
	Custom:    getEventDoc,
	Doc:       "获取 openevent 文档",
	IsOpenAPI: true,
}

var (
	eventDocLock sync.Once
	eventDoc     []byte
)

func getEventDocContent() []byte {
	eventDocLock.Do(func() {
		f, err := os.Open("./events_all.json")
		defer f.Close()
		if err != nil {
			logrus.Errorf("getEventDocContent: %v", err)
		}
		content, err := ioutil.ReadAll(f)
		if err != nil {
			logrus.Errorf("getEventDocContent: %v", err)
		}
		eventDoc = content
	})
	return eventDoc
}

func getEventDoc(rw http.ResponseWriter, req *http.Request) {
	j := getEventDocContent()
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	rw.WriteHeader(200)
	rw.Write(j)
}
