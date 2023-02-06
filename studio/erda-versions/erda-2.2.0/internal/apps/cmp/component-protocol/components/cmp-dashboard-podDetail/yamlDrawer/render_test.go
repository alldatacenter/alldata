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

package yamlDrawer

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentYamlDrawer_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"visible": true,
	}}
	cyd := &ComponentYamlDrawer{}
	if err := cyd.GenComponentState(c); err != nil {
		t.Fatal(err)
	}

	ok, err := cputil.IsDeepEqual(c.State, cyd.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}

func TestComponentYamlDrawer_Transfer(t *testing.T) {
	cyd := &ComponentYamlDrawer{
		Props: Props{
			Title: "testTitle",
			Size:  "testSize",
		},
		State: State{
			Visible: true,
		},
	}
	c := &cptype.Component{}
	cyd.Transfer(c)

	ok, err := cputil.IsDeepEqual(c, cyd)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}
