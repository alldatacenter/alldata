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

package diceyml

import (
	"reflect"
	"regexp"

	"sigs.k8s.io/yaml"
)

func assignWithoutEmpty(p interface{}, src interface{}) {
	value := reflect.ValueOf(p).Elem()
	if !value.CanSet() {
		return
	}

	if v, ok := src.(string); ok && v != "" {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.([]string); ok && v != nil && len(v) != 0 {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.(int); ok && v != 0 {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.([]int); ok && v != nil && len(v) != 0 {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.(int64); ok && v != 0 {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.([]int64); ok && v != nil && len(v) != 0 {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.(float64); ok && v != 0 {
		value.Set(reflect.ValueOf(v))
	}
	if v, ok := src.([]float64); ok && v != nil && len(v) != 0 {
		value.Set(reflect.ValueOf(v))
	}
}

func isZero(v interface{}) bool {
	switch reflect.TypeOf(v).Kind() {
	case reflect.Int:
		return v.(int) == 0
	case reflect.Int64:
		return v.(int64) == 0
	case reflect.Float64:
		return v.(float64) == 0
	case reflect.String:
		return v.(string) == ""
	case reflect.Map:
		return reflect.ValueOf(v).Len() == 0
	case reflect.Slice:
		return reflect.ValueOf(v).Len() == 0
	default:
		panic("not support kind: " + reflect.TypeOf(v).Kind().String())
	}
}

func overrideIfNotZero(src, dst interface{}) {
	if isZero(src) {
		return
	}
	override(&src, dst)
}

func override(src, dst interface{}) {
	r, err := yaml.Marshal(src)
	if err != nil {
		panic("deepcopy marshal")
	}
	if err := yaml.Unmarshal(r, dst); err != nil {
		panic("deepcopy unmarshal")
	}
}

func CopyObj(src *Object) *Object {
	dst := new(Object)
	override(src, dst)
	return dst
}

func yamlHeaderRegex(head string) *regexp.Regexp {
	return regexp.MustCompile("(?m)^[[:blank:]]*" + head + "[[:blank:]]*:")
}

func yamlHeaderRegexWithUpperHeader(upper []string, head string) *regexp.Regexp {
	r := "(?sm)"
	for _, uphead := range upper {
		r += "(?:^[[:blank:]]*?" + uphead + "[[:blank:]]*?:.*?)"
	}
	r += "(^[[:blank:]]*?" + head + "[[:blank:]]*?:)"
	return regexp.MustCompile(r)
}

func ComposeIntPortsFromServicePorts(servicePorts []ServicePort) []int {
	var ports []int
	for _, port := range servicePorts {
		ports = append(ports, port.Port)
	}
	return ports
}
