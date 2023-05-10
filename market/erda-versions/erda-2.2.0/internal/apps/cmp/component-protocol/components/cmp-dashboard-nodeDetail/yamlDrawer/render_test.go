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
	c := &cptype.Component{
		State: map[string]interface{}{
			"visible": true,
		},
	}
	cyd := &ComponentYamlDrawer{}
	if err := cyd.GenComponentState(c); err != nil {
		t.Error(err)
	}

	isEqual, err := cputil.IsDeepEqual(cyd.State, c.State)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
	}
}

func TestComponentYamlDrawer_Transfer(t *testing.T) {
	cyd := &ComponentYamlDrawer{
		Props: Props{
			Title: "testTitle",
			Size:  "l",
		},
		State: State{
			Visible: true,
		},
	}
	var c cptype.Component
	cyd.Transfer(&c)

	isEqual, err := cputil.IsDeepEqual(cyd, c)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
	}
}
