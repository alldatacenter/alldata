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

package openapi_test

//import (
//	"context"
//	"io/ioutil"
//	"testing"
//
//	"github.com/getkin/kin-openapi/openapi2"
//	"github.com/getkin/kin-openapi/openapi2conv"
//
//	"github.com/erda-project/erda/pkg/swagger/openapi"
//)
//
//const testFile = "./testdata/portal.json"
//
//// go test -v -run TestValidateOAS3
//func TestValidateOAS3(t *testing.T) {
//	data, err := ioutil.ReadFile(testFile)
//	if err != nil {
//		t.Errorf("failed to ReadFile, err: %v", err)
//	}
//
//	swagger := new(openapi2.Swagger)
//	if err = swagger.UnmarshalJSON(data); err != nil {
//		t.Errorf("failed to UnmarshalJSON, err: %v", err)
//	}
//
//	oas3, err := openapi2conv.ToV3Swagger(swagger)
//	if err != nil {
//		t.Errorf("failed to ToV3Swagger, err: %v", err)
//	}
//
//	if err = openapi.ValidateOAS3(context.Background(), *oas3); err != nil {
//		t.Errorf("failed to ValidateOAS3, err: %v", err)
//	}
//}
