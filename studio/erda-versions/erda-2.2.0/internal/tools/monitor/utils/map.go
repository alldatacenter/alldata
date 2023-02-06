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

package utils

import "strings"

func GetMapValueString(m map[string]interface{}, key ...string) (string, bool) {
	for _, item := range key {
		value, ok := m[item]
		if ok {
			return ConvertString(value)
		}
	}

	return "", false
}

func GetMapValueBool(m map[string]interface{}, key string) (bool, bool) {
	value, ok := m[key]
	if !ok {
		return false, false
	}
	return ConvertBool(value)
}

func GetMapValueInt64(m map[string]interface{}, key string) (int64, bool) {
	value, ok := m[key]
	if !ok {
		return 0, false
	}
	return ConvertInt64(value)
}

func GetMapValueUint64(m map[string]interface{}, key string) (uint64, bool) {
	value, ok := m[key]
	if !ok {
		return 0, false
	}
	return ConvertUint64(value)
}

func GetMapValueFloat64(m map[string]interface{}, key string) (float64, bool) {
	value, ok := m[key]
	if !ok {
		return 0, false
	}
	return ConvertFloat64(value)
}

func GetMapValueArr(m map[string]interface{}, key string) ([]interface{}, bool) {
	value, ok := m[key]
	if !ok {
		return nil, false
	}
	arr, ok := value.([]interface{})
	return arr, ok
}

func GetMapValueMap(m map[string]interface{}, key string) (map[string]interface{}, bool) {
	value, ok := m[key]
	if !ok {
		return nil, false
	}
	mm, ok := value.(map[string]interface{})
	return mm, ok
}

func GetMapValue(key string, data map[string]interface{}) interface{} {
	keys := strings.Split(key, ".")
	last := len(keys) - 1
	for i, k := range keys {
		if i >= last {
			return data[k]
		}
		d, ok := data[k]
		if !ok {
			return nil
		}
		data, ok = d.(map[string]interface{})
		if !ok {
			return nil
		}
	}
	return nil
}
