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

package addPodFilter

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentAddPodFilter_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testClusterName",
		"conditions": []Condition{
			{
				Fixed:      true,
				HaveFilter: true,
				Key:        "testKey",
				Label:      "testLabel",
				Required:   true,
				CustomProps: CustomProps{
					Mode: "testMode",
				},
				Options: []Option{
					{
						Label: "testLabel",
						Value: "testValue",
					},
				},
				Type: "testType",
			},
		},
		"values": Values{
			Namespace: "testNs",
		},
	}}

	component := &ComponentAddPodFilter{}
	if err := component.GenComponentState(c); err != nil {
		t.Fatal(err)
	}

	ok, err := cputil.IsDeepEqual(c, component)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}

func TestComponentAddPodFilter_Transfer(t *testing.T) {
	component := &ComponentAddPodFilter{
		State: State{
			ClusterName: "testClusterName",
			Conditions: []Condition{
				{
					Fixed:      true,
					HaveFilter: true,
					Key:        "testKey",
					Label:      "testLabel",
					Required:   true,
					CustomProps: CustomProps{
						Mode: "testMode",
					},
					Options: []Option{
						{
							Label: "testLabel",
							Value: "testValue",
						},
					},
					Type: "testType",
				},
			},
			Values: Values{
				Namespace: "testNs",
			},
		},
		Operations: map[string]interface{}{
			"testOp": Operation{
				Key:    "testKey",
				Reload: true,
			},
		},
	}
	c := &cptype.Component{}
	component.Transfer(c)
	ok, err := cputil.IsDeepEqual(c, component)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}
