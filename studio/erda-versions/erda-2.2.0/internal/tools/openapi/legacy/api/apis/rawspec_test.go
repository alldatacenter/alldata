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

package apis_test

import (
	"context"
	"testing"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis/dop"
	"github.com/erda-project/erda/pkg/swagger/oas3"
)

func TestApiSpec_AddOperationTo(t *testing.T) {
	var testAPIs = []*apis.ApiSpec{
		&dop.CreateAPIAssetVersion,
		&dop.GetAccess,
	}
	v3 := apis.NewSwagger("test")
	for _, api := range testAPIs {
		if err := api.AddOperationTo(v3); err != nil {
			t.Fatal(err)
		}
	}
	data, err := oas3.MarshalYaml(v3)
	if err != nil {
		t.Fatal(err)
	}
	newV3, err := oas3.LoadFromData(data)
	if err != nil {
		t.Fatal(err)
	}
	if err = oas3.ValidateOAS3(context.Background(), *newV3); err != nil {
		t.Fatal(err)
	}
	t.Log(string(data))
}
