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

/**
v1_jsonbody 兼容 apitestsv1 里的 json body 渲染逻辑：
- 反序列化，当类型是 map 或 slice 时进行处理
- value(map 的 value、slice 的 value) 必须**全量匹配** {{.+}}，不支持 {{.+}}xxx 这种
- 匹配之后，替换时根据配置的具体类型进行渲染

举例：
- 例如 map 有一组 kv 为 "key1": "{{ticketID}}"
  - 若参数 ticketID 为 integer(10)，渲染结果为 "key1": 10
  - 若参数 ticketID 为 string("10")，渲染结果为 "key1": "10"
- 假如 map 有一组 kv 为 "key1": {{ticketID}}
  - 此时反序列化失败，不进行 v1 渲染
  - 此时走 v2 渲染逻辑，即文本渲染。若参数为 ticketID=10
    - "key1": {{ticketID}} => "key1": 10
    - "key1": "{{ticketID}}" => "key1": "10"
    - "key1": "{{ticketID}}-xxx" => "key1": "10-xxx"
    - "key1": {{ticketID}}-xxx => "key1": 10-xxx
*/
package apitestsv2

import (
	"encoding/json"
	"fmt"
	"os"
	"reflect"
	"regexp"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/mock"
)

// tryV1RenderRequestBodyStr 尝试使用 apitestsv1 的严格渲染逻辑，渲染失败不报错
func tryV1RenderRequestBodyStr(bodyStr string, caseParams map[string]*apistructs.CaseParams) string {
	// 当任务不运行在流水线中时，需要打印日志
	needPrintLog := os.Getenv("PIPELINE_ID") == ""
	var val interface{}
	// 尝试解析
	if err := json.Unmarshal([]byte(bodyStr), &val); err != nil {
		// 解析失败，告警，并返回原 bodyStr 用于后续处理
		if needPrintLog {
			logrus.Warnf("tryV1RenderRequestBodyStr: failed to unmarshal bodyStr, bodyStr: %s, err: %v", bodyStr, err)
		}
		return bodyStr
	}
	v1RenderRequestBody(val, caseParams)
	renderedByte, err := json.Marshal(val)
	if err != nil {
		if needPrintLog {
			logrus.Warnf("tryV1RenderRequestBodyStr: failed to marshal after render, bodyStr: %s, err: %v", bodyStr, err)
		}
		return bodyStr
	}
	return string(renderedByte)
}

// v1RenderRequestBody 即 apitestsv1 的严格渲染
func v1RenderRequestBody(val interface{}, caseParams map[string]*apistructs.CaseParams) {
	if val == nil || (reflect.TypeOf(val).Kind() != reflect.Map &&
		reflect.TypeOf(val).Kind() != reflect.Slice &&
		reflect.TypeOf(val).Kind() != reflect.Array) {
		return
	}

	if reflect.TypeOf(val).Kind() == reflect.Map {
		for k, v := range val.(map[string]interface{}) {
			if v != nil && reflect.TypeOf(v).Kind() == reflect.String {
				valueTrim := strings.TrimSpace(fmt.Sprint(v))
				reMock, err := regexp.MatchString("^{{@.+}}$", valueTrim)
				if reMock && err == nil {
					valTemp := strings.TrimLeft(valueTrim, "{{@")
					v := strings.TrimRight(valTemp, "}}")
					mv := mock.MockValue(v)
					if mv != nil {
						val.(map[string]interface{})[k] = mv
					}
				} else {
					isMatch, err := regexp.MatchString("^{{.+}}$", valueTrim)
					if isMatch && err == nil {
						valTemp := strings.TrimLeft(valueTrim, "{{")
						valNew := strings.TrimRight(valTemp, "}}")
						if value, ok := caseParams[valNew]; ok {
							switch value.Type {
							case mock.String:
								val.(map[string]interface{})[k] = fmt.Sprint(value.Value)
							case mock.Integer:
								intVal, _ := strconv.Atoi(fmt.Sprint(value.Value))
								val.(map[string]interface{})[k] = intVal
							case mock.Float:
								fVal, _ := strconv.ParseFloat(fmt.Sprint(value.Value), 64)
								val.(map[string]interface{})[k] = fVal
							case mock.Boolean:
								var bVal bool
								if fmt.Sprint(value.Value) == "true" {
									bVal = true
								}
								val.(map[string]interface{})[k] = bVal
							case mock.List:
								var list []string
								isMatch, err := regexp.MatchString("^\\[.+]$", fmt.Sprint(value.Value))
								if isMatch && err == nil {
									valLeft := strings.TrimLeft(fmt.Sprint(value.Value), "[")
									valRight := strings.TrimRight(valLeft, "]")

									sl := strings.Split(valRight, ",")
									for _, va := range sl {
										va = strings.TrimSuffix(strings.TrimPrefix(strings.TrimSpace(va), "\""), "\"")
										list = append(list, va)
									}
									val.(map[string]interface{})[k] = list
								}
								if !isMatch {
									logrus.Warningf("list value err, key:%s, value:%s",
										valNew, fmt.Sprint(value.Value))
								}
							default:
								val.(map[string]interface{})[k] = value.Value
							}
						}
					}
				}
			}
			v1RenderRequestBody(v, caseParams)
		}
	} else if reflect.TypeOf(val).Kind() == reflect.Array || reflect.TypeOf(val).Kind() == reflect.Slice {
		for i, v := range val.([]interface{}) {
			if v != nil && reflect.TypeOf(v).Kind() == reflect.String {
				valueTrim := strings.TrimSpace(fmt.Sprint(v))
				reMock, err := regexp.MatchString("^{{@.+}}$", valueTrim)
				if reMock && err == nil {
					valTemp := strings.TrimLeft(valueTrim, "{{@")
					v := strings.TrimRight(valTemp, "}}")
					mv := mock.MockValue(v)
					if mv != nil {
						val.([]interface{})[i] = mv
					}
				} else {
					isMatch, err := regexp.MatchString("^{{.+}}$", valueTrim)
					if isMatch && err == nil {
						valTemp := strings.TrimLeft(valueTrim, "{{")
						valNew := strings.TrimRight(valTemp, "}}")
						if value, ok := caseParams[valNew]; ok {
							switch value.Type {
							case mock.String:
								val.([]interface{})[i] = fmt.Sprint(value.Value)
							case mock.Integer:
								intVal, _ := strconv.Atoi(fmt.Sprint(value.Value))
								val.([]interface{})[i] = intVal
							case mock.Float:
								fVal, _ := strconv.ParseFloat(fmt.Sprint(value.Value), 64)
								val.([]interface{})[i] = fVal
							case mock.Boolean:
								var bVal bool
								if fmt.Sprint(value.Value) == "true" {
									bVal = true
								}
								val.([]interface{})[i] = bVal
							case mock.List:
								var list []string
								isMatch, err := regexp.MatchString("^\\[.+]$", fmt.Sprint(value.Value))
								if isMatch && err == nil {
									valLeft := strings.TrimLeft(fmt.Sprint(value.Value), "[")
									valRight := strings.TrimRight(valLeft, "]")

									sl := strings.Split(valRight, ",")
									for _, va := range sl {
										list = append(list, va)
									}
									val.([]interface{})[i] = list
								}
								if !isMatch {
									logrus.Warningf("list value err, key:%s, value:%s",
										valNew, fmt.Sprint(value.Value))
								}
							default:
								val.([]interface{})[i] = value.Value
							}
						}
					}
				}
			}
			v1RenderRequestBody(v, caseParams)
		}
	}

	return
}
