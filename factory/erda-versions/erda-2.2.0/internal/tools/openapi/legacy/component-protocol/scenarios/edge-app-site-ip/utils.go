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

package edgeappsiteip

import (
	"fmt"
	"reflect"
	"strconv"
)

func StructToMap(obj interface{}) map[string]interface{} {
	out := make(map[string]interface{})
	t := reflect.TypeOf(obj)
	v := reflect.ValueOf(obj)

	if v.Kind() == reflect.Ptr {
		v = v.Elem()
	}

	if v.Kind() != reflect.Struct {
		fmt.Println("it is not struct")
		return nil
	}

	for i := 0; i < t.NumField(); i++ {
		if tagVal := t.Field(i).Tag.Get("json"); tagVal != "" {
			out[tagVal] = v.Field(i).Interface()
		}
	}
	return out
}

func Force2Int(input interface{}) (int, error) {
	var res int
	if m, ok := input.(string); ok {
		res, err := strconv.Atoi(m)
		if err != nil {
			return res, err
		}
		return res, nil
	}
	if m, ok := input.(int64); ok {
		res = int(m)
		return res, nil
	}
	if m, ok := input.(float64); ok {
		res = int(m)
		return res, nil
	}

	return res, nil
}
