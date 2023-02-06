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

package PodInfo

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestPodInfo_Transfer(t *testing.T) {
	component := &PodInfo{
		Data: map[string]Data{
			"test": {
				Namespace: "default",
				Age:       "1s",
				Ip:        "0.0.0.0",
				Workload:  "test",
				Node:      "test",
				Labels: []Tag{
					{
						Label: "test",
						Group: "test",
					},
				},
				Annotations: []Tag{
					{
						Label: "test",
						Group: "test",
					},
				},
			},
		},
		State: State{
			ClusterName: "testCluster",
			PodID:       "testID",
		},
		Props: Props{
			RequestIgnore: []string{"data"},
			ColumnNum:     1,
			Fields: []Field{
				{
					Label:      "test",
					ValueKey:   "test",
					RenderType: "text",
					Operations: map[string]Operation{
						"testOp": {
							Key:    "test",
							Reload: true,
							Command: Command{
								Key:    "test",
								Target: "test",
								State: CommandState{
									Params: map[string]string{
										"k1": "v1",
									},
								},
								JumpOut: true,
							},
						},
					},
					SpaceNum: 1,
				},
			},
		},
	}

	result := &cptype.Component{}
	component.Transfer(result)
	isEqual, err := cputil.IsDeepEqual(result, component)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
	}
}

func TestPodInfo_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testClusterName",
		"podId":       "testPodID",
	}}
	podInfo := &PodInfo{}
	if err := podInfo.GenComponentState(c); err != nil {
		t.Fatal(err)
	}

	ok, err := cputil.IsDeepEqual(c.State, podInfo.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}
