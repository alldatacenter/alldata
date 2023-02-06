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
	"io/ioutil"
	"testing"

	"github.com/erda-project/erda/pkg/swagger/oas3"
)

func TestExpandSchemaRef2(t *testing.T) {
	filename := "./testdata/dop-all.yaml"
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		t.Fatalf("failed to ReadFile: %v", err)
	}

	v3, err := oas3.LoadFromData(data)
	if err != nil {
		t.Fatalf("failed to LoadFromData: %v", err)
	}

	if err = oas3.ExpandPaths(v3); err != nil {
		t.Fatalf("failed to ExpandPaths: %v", err)
	}
}
