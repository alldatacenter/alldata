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

package structparser

import (
	"reflect"
)

// getComment 获取 `t` 的 `fieldname` 对应的注释(comment)
// `t` 是 struct value
func getComment(t interface{}, fieldname string) string {
	tp := reflect.TypeOf(t)
	value := reflect.ValueOf(t)
	if tp.Kind() == reflect.Ptr {
		tp = tp.Elem()
	}
	method := value.MethodByName("Desc_" + tp.Name())

	if (method == reflect.Value{}) {
		return ""
	}
	return method.Call([]reflect.Value{reflect.ValueOf(fieldname)})[0].String()
}
