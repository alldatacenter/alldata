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

package oas3_test

import (
	"testing"

	"github.com/getkin/kin-openapi/openapi3"
	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/swagger/oas3"
)

const schemaText = `description: ""
properties:
  name:
    type: string
  age:
    type: integer
  registered:
    type: boolean
  info:
    type: object
    properties:
      school:
        type: string
      grade:
        type: integer
type: object
`

const applicationJsonExample = `{"age":0,"info":{"grade":0,"school":""},"name":"","registered":true}`
const urlEncodedExample = `age=0&info=%7B%22grade%22%3A0%2C%22school%22%3A%22%22%7D&name=&registered=true`

func TestGenExampleFromExpandedSchema(t *testing.T) {
	t.Run(string(httputil.ApplicationJson), testGenExampleFromExpandedSchema_applicationJson)
	t.Run(string(httputil.URLEncodedFormMime), testGenExampleFromExpandedSchema_urlEncodedForm)
}

func testGenExampleFromExpandedSchema_applicationJson(t *testing.T) {
	s := openapi3.NewSchema()
	if err := yaml.Unmarshal([]byte(schemaText), s); err != nil {
		t.Fatalf("failed to yaml.Unmarshal schemaText: %v", err)
	}
	oas3.GenExampleFromExpandedSchema(httputil.ApplicationJson, s)
	t.Log("GenExampleFromExpandedSchema application/json:\n", s.Example)
	example, ok := s.Example.(string)
	if !ok {
		t.Fatal("s.Example was not converted to string")
	}
	if example != applicationJsonExample {
		t.Fatal("s.Example is wrong")
	}
}

func testGenExampleFromExpandedSchema_urlEncodedForm(t *testing.T) {
	s := openapi3.NewSchema()
	if err := yaml.Unmarshal([]byte(schemaText), s); err != nil {
		t.Fatalf("failed to yaml.Unmarshal schemaText: %v", err)
	}
	oas3.GenExampleFromExpandedSchema(httputil.URLEncodedFormMime, s)
	t.Log("GenExampleFromExpandedSchema urlencoded form:\n", s.Example)
	example, ok := s.Example.(string)
	if !ok {
		t.Fatalf("s.Example was not convered to string: %v", example)
	}
	if example != urlEncodedExample {
		t.Fatalf("s.Example is wrong: \n%s\n%s", urlEncodedExample, example)
	}
}
