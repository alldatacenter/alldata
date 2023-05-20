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
	"os"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/generate/apistruct"
)

var eventDocJSON = make(apistruct.JSON)

func generateEventDoc(onlyOpenEvents bool, resultfile string) {
	if err := json.Unmarshal([]byte(`{"swagger": "2.0"}`), &eventDocJSON); err != nil {
		panic(err)
	}
	js := make(apistruct.JSON)
	eventDocJSON["definitions"] = js
	for _, v := range Events {
		eventStruct := v[0]
		doc := v[1].(string)
		apistruct.EventToJson(eventStruct, doc, js)
	}
	docf, err := os.OpenFile(resultfile, os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	if err != nil {
		panic(err)
	}
	eventDocTxt, err := json.Marshal(eventDocJSON)
	if err != nil {
		panic(err)
	}
	docf.Write(eventDocTxt)
}
