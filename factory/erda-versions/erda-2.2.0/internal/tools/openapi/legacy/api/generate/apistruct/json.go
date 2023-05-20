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

package apistruct

import (
	"encoding/json"
	"reflect"
	"strings"

	"github.com/erda-project/erda/pkg/strutil"
)

type JSON map[string]interface{}
type SliceJSON []interface{}

/*
path: URL path
method: http method
summary: 综合性描述
m: 生成的json结构
req: 请求类型结构体
resp: 应答类型结构体
*/
func ToJson(path, method, summary string, group string, m JSON, req, resp interface{}) {
	paths := m["paths"].(JSON)
	definitions := m["definitions"].(JSON)
	reqparams, _ := structToParam(context{request: true}, req)
	_, respparam := structToParam(context{}, resp)

	// convert param to JSON
	reqByte, err := json.Marshal(reqparams)
	if err != nil {
		panic(err)
	}
	respByte, err := json.Marshal(respparam)
	if err != nil {
		panic(err)
	}
	var (
		reqJSON  = make(SliceJSON, 0)
		respJSON = make(JSON)
	)
	if err := json.Unmarshal(reqByte, &reqJSON); err != nil {
		panic(err)
	}
	if err := json.Unmarshal(respByte, &respJSON); err != nil {
		panic(err)
	}
	if _, ok := paths[path]; !ok {
		paths[path] = make(JSON)
	}
	if _, ok := paths[path].(JSON)[method]; !ok {
		paths[path].(JSON)[method] = make(JSON)
	}
	paths[path].(JSON)[method].(JSON)["parameters"] = reqJSON
	paths[path].(JSON)[method].(JSON)["produces"] = []string{"application/json"}
	paths[path].(JSON)[method].(JSON)["responses"] = make(JSON)
	paths[path].(JSON)[method].(JSON)["summary"] = strings.TrimLeft(summary, "summary:")
	if group != "" {
		paths[path].(JSON)[method].(JSON)["tags"] = []string{group}
	}
	paths[path].(JSON)[method].(JSON)["responses"].(JSON)["200"] = make(JSON)
	paths[path].(JSON)[method].(JSON)["responses"].(JSON)["200"].(JSON)["description"] = "OK"
	respTp := reflect.TypeOf(resp)
	paths[path].(JSON)[method].(JSON)["responses"].(JSON)["200"].(JSON)["schema"] = JSON{
		"$ref": strutil.Concat("#/definitions/", respTp.Name()),
	}

	// definitions
	definitions[respTp.Name()] = respJSON
}

func EventToJson(event interface{}, summary string, m JSON) {
	t := reflect.TypeOf(event)
	_, eventSwagger := structToParam(context{}, event)
	eventByte, err := json.Marshal(eventSwagger)
	if err != nil {
		panic(err)
	}
	var eventJSON = make(JSON)
	if err := json.Unmarshal(eventByte, &eventJSON); err != nil {
		panic(err)
	}
	m[t.Name()] = eventJSON
	m[t.Name()].(JSON)["description"] = summary
}
