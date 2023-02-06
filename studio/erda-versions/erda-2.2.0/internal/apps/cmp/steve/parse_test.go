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

package steve

import (
	"net/http"
	"testing"

	"github.com/rancher/apiserver/pkg/parse"
	"github.com/rancher/apiserver/pkg/types"
)

func MockUrlParser(rw http.ResponseWriter, req *http.Request, schemas *types.APISchemas) (parse.ParsedURL, error) {
	return parse.ParsedURL{
		Type:      "testType",
		Name:      "testName",
		Namespace: "testNamespace",
		Link:      "testLink",
		Method:    "testMethod",
		Action:    "testAction",
		Prefix:    "testPrefix",
	}, nil
}

func TestParse(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://test.unit", nil)
	if err != nil {
		t.Fatal(err)
	}

	apiOp := &types.APIRequest{
		Request: req,
	}
	if err := Parse(apiOp, MockUrlParser); err != nil {
		t.Error(err)
	}

	if apiOp.Type != "testType" || apiOp.Name != "testName" || apiOp.Link != "testLink" || apiOp.Action != "testAction" ||
		apiOp.Query != nil || apiOp.URLPrefix != "testPrefix" || apiOp.Namespace != "testNamespace" {
		t.Error("test failed, apiOp is not expected")
	}
}
