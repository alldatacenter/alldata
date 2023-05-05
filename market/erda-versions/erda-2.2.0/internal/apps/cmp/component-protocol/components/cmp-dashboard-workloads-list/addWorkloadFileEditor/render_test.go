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

package addWorkloadFileEditor

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentAddWorkloadFileEditor_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName":  "testClusterName",
		"workloadKind": "apps.deployments",
		"values": Values{
			WorkloadKind: "apps.deployments",
			Namespace:    "default",
		},
		"value": "test",
	}}
	component := &ComponentAddWorkloadFileEditor{}
	if err := component.GenComponentState(c); err != nil {
		t.Fatal(err)
	}
	ok, err := cputil.IsDeepEqual(c.State, component.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}

func TestComponentAddWorkloadFileEditor_SetComponentValue(t *testing.T) {
	component := &ComponentAddWorkloadFileEditor{
		sdk: &cptype.SDK{
			Tran: &MockTran{},
		},
	}
	component.SetComponentValue()
}

func TestComponentAddWorkloadFileEditor_Transfer(t *testing.T) {
	component := &ComponentAddWorkloadFileEditor{
		State: State{
			ClusterName: "testClusterName",
			Value:       "testValue",
			Values: Values{
				WorkloadKind: "apps.deployments",
				Namespace:    "default",
			},
			WorkloadKind: "apps.deployments",
		},
		Props: Props{
			Bordered:     true,
			FileValidate: []string{"test"},
			MinLines:     22,
		},
		Operations: map[string]interface{}{
			"testOp": Operation{
				Key:        "testKey",
				Reload:     true,
				SuccessMsg: "testMsg",
			},
		},
	}
	c := &cptype.Component{}
	component.Transfer(c)
	ok, err := cputil.IsDeepEqual(c.State, component.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}
