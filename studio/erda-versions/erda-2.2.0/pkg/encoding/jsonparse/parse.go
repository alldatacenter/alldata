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

package jsonparse

import (
	"bytes"
	"encoding/json"
	"strings"

	"github.com/erda-project/erda/pkg/encoding/jsonpath"
)

func JsonOneLine(o interface{}) string {
	if o == nil {
		return ""
	}
	switch o.(type) {
	case string: // remove the quotes
		return o.(string)
	case []byte: // remove the quotes
		return string(o.([]byte))
	default:
		var buffer bytes.Buffer
		enc := json.NewEncoder(&buffer)
		enc.SetEscapeHTML(false)
		if err := enc.Encode(o); err != nil {
			return ""
		}
		return strings.TrimSuffix(buffer.String(), "\n")
	}
}

// use jq jsonPath or jackson to filter json
func FilterJson(jsonValue []byte, express string, expressType string) interface{} {
	if len(jsonValue) <= 0 {
		return ""
	}

	var (
		body interface{}
		val  interface{}
	)

	d := json.NewDecoder(bytes.NewReader(jsonValue))
	d.UseNumber()
	err := d.Decode(&body)
	if err != nil {
		// resp not a json and express mean get all result, so return all resp
		if express == "." {
			return string(jsonValue)
		}
		return ""
	}

	jsonString := string(jsonValue)

	express = strings.TrimSpace(express)
	if express != "" {
		switch expressType {
		case "jsonpath":
			val, _ = jsonpath.Get(body, express)
		case "jq":
			val, _ = jsonpath.JQ(jsonString, express)
		case "jackson":
			val, _ = jsonpath.Jackson(jsonString, express)
		default:
			if strings.HasPrefix(express, jsonpath.JacksonExpressPrefix) {
				val, _ = jsonpath.Jackson(jsonString, express)
			} else {
				// jq The expression does not necessarily start with ., maybe like '{"ss": 1}' | .ss
				var err error
				val, err = jsonpath.JQ(jsonString, express)
				if err != nil {
					val, _ = jsonpath.Get(body, express)
				}
			}
		}
	} else {
		val = body
	}

	return val
}
