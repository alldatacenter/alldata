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

package convert

import "strconv"

// Converters .
var Converters = map[string]func(text string) (interface{}, error){
	"string": func(text string) (interface{}, error) {
		return text, nil
	},
	"number": func(text string) (interface{}, error) {
		return strconv.ParseFloat(text, 64)
	},
	"bool": func(text string) (interface{}, error) {
		return strconv.ParseBool(text)
	},
	"timestamp": func(text string) (interface{}, error) {
		return strconv.ParseInt(text, 10, 64)
	},
	"int": func(text string) (interface{}, error) {
		return strconv.ParseInt(text, 10, 64)
	},
	"float": func(text string) (interface{}, error) {
		return strconv.ParseFloat(text, 64)
	},
}

// Converter .
func Converter(typ string) func(text string) (interface{}, error) {
	c, ok := Converters[typ]
	if !ok {
		return Converters["string"]
	}
	return c
}
