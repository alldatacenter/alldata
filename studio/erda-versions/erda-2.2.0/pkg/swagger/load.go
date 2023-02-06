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

package swagger

import (
	"encoding/json"

	"github.com/getkin/kin-openapi/openapi3"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/swagger/oas2"
	"github.com/erda-project/erda/pkg/swagger/oas3"
	"github.com/erda-project/erda/pkg/swagger/oasconv"
)

func LoadFromData(spec []byte) (*openapi3.Swagger, error) {
	var m map[string]interface{}
	if err := yaml.Unmarshal(spec, &m); err != nil {
		return nil, err
	}
	if _, ok := m["swagger"]; ok {
		v2, err := oas2.LoadFromData(spec)
		if err != nil {
			return nil, err
		}
		return oasconv.OAS2ConvTo3(v2)
	}

	return oas3.LoadFromData(spec)
}

// 序列化为 json 格式
func MarshalJSON(v interface{}) ([]byte, error) {
	return json.MarshalIndent(v, "", "  ")
}

// 序列化为 yaml 格式
func MarshalYAML(v interface{}) ([]byte, error) {
	// 之所以要先序列化成 json, 再转换为 yaml,
	// 是因为 *openapi.Swagger 对象实现了 MarshalJSON,
	// 有特定的序列化规则
	data, err := json.Marshal(v)
	if err != nil {
		return nil, err
	}
	return oasconv.JSONToYAML(data)
}
