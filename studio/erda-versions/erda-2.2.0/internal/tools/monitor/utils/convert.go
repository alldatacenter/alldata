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

import (
	"reflect"
	"strconv"
	"strings"
)

const (
	StringType = "string"
	NumberType = "number"
	BoolType   = "bool"
	Unknown    = "unknown"
)

func TypeOf(obj interface{}) string {
	if obj == nil {
		return ""
	}

	typ := reflect.TypeOf(obj)
	return typeOf(typ)
}

func typeOf(typ reflect.Type) string {
	switch typ.Kind() {
	case reflect.String:
		return StringType
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64,
		reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64,
		reflect.Float32, reflect.Float64:
		return NumberType
	case reflect.Bool:
		return BoolType
	case reflect.Array, reflect.Slice:
		if typ.Elem().Kind() == reflect.Uint8 {
			return StringType
		}
		return typeOf(typ.Elem())
	}
	return Unknown
}

func ConvertDataType(obj interface{}, dataType string) (interface{}, error) {
	switch TypeOf(obj) {
	case StringType:
		value, _ := ConvertString(obj)
		switch dataType {
		case StringType:
			return value, nil
		case NumberType:
			return strconv.ParseFloat(value, 10)
		case BoolType:
			strconv.ParseBool(value)
		}
	case NumberType:
		value, _ := ConvertFloat64(obj)
		switch dataType {
		case StringType:
			return strconv.FormatFloat(value, 'f', -1, 64), nil
		case NumberType:
			return value, nil
		case BoolType:
			if value == 0 {
				return false, nil
			} else {
				return true, nil
			}
		}
	case BoolType:
		value, _ := ConvertBool(obj)
		switch dataType {
		case StringType:
			return strconv.FormatBool(value), nil
		case NumberType:
			if value {
				return 1, nil
			} else {
				return 0, nil
			}
		case BoolType:
			return value, nil
		}
	}
	return nil, nil
}

func ConvertStructToMap(obj interface{}) map[string]interface{} {
	t := reflect.TypeOf(obj)
	v := reflect.ValueOf(obj)

	m := make(map[string]interface{})
	for i := 0; i < t.NumField(); i++ {
		m[strings.ToLower(t.Field(i).Name)] = v.Field(i).Interface()
	}
	return m
}

func ConvertArrToMap(list []string) map[string]bool {
	m := make(map[string]bool)
	for _, item := range list {
		m[item] = true
	}
	return m
}

func ConvertStringArrToInterfaceArr(list []string) []interface{} {
	var arr []interface{}
	for _, item := range list {
		arr = append(arr, item)
	}
	return arr
}

func ConvertString(obj interface{}) (string, bool) {
	switch val := obj.(type) {
	case string:
		return val, true
	case []byte:
		return string(val), true
	}
	return "", false
}

// ConvertStringArr interface转字符串数组
func ConvertStringArr(obj interface{}) ([]string, bool) {
	if arr, ok := obj.([]interface{}); ok {
		var result []string
		for _, item := range arr {
			if s, ok := ConvertString(item); ok {
				result = append(result, s)
			}
		}
		return result, true
	}
	return nil, false
}

func ConvertBool(obj interface{}) (bool, bool) {
	switch val := obj.(type) {
	case bool:
		return val, true
	case int, int8, int16, int32, int64, uint, uint8, uint16, uint32, uint64, float32, float64:
		return val != 0, true
	}
	return false, false
}

func ConvertInt64(obj interface{}) (int64, bool) {
	switch val := obj.(type) {
	case int:
		return int64(val), true
	case int8:
		return int64(val), true
	case int16:
		return int64(val), true
	case int32:
		return int64(val), true
	case int64:
		return int64(val), true
	case uint:
		return int64(val), true
	case uint8:
		return int64(val), true
	case uint16:
		return int64(val), true
	case uint32:
		return int64(val), true
	case uint64:
		return int64(val), true
	case float32:
		return int64(val), true
	case float64:
		return int64(val), true
	}
	return 0, false
}

func ConvertUint64(obj interface{}) (uint64, bool) {
	switch val := obj.(type) {
	case int:
		return uint64(val), true
	case int8:
		return uint64(val), true
	case int16:
		return uint64(val), true
	case int32:
		return uint64(val), true
	case int64:
		return uint64(val), true
	case uint:
		return uint64(val), true
	case uint8:
		return uint64(val), true
	case uint16:
		return uint64(val), true
	case uint32:
		return uint64(val), true
	case uint64:
		return uint64(val), true
	case float32:
		return uint64(val), true
	case float64:
		return uint64(val), true
	}
	return 0, false
}

func ConvertFloat64(obj interface{}) (float64, bool) {
	switch val := obj.(type) {
	case int:
		return float64(val), true
	case int8:
		return float64(val), true
	case int16:
		return float64(val), true
	case int32:
		return float64(val), true
	case int64:
		return float64(val), true
	case uint:
		return float64(val), true
	case uint8:
		return float64(val), true
	case uint16:
		return float64(val), true
	case uint32:
		return float64(val), true
	case uint64:
		return float64(val), true
	case float32:
		return float64(val), true
	case float64:
		return float64(val), true
	}
	return 0, false
}
