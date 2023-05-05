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

package operationButton

import (
	"context"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protobuf/proto-go/cp/pb"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentOperationButton_GenComponentState(t *testing.T) {
	c := &cptype.Component{
		State: map[string]interface{}{
			"clusterName": "testClusterName",
			"podId":       "testPodID",
		},
	}
	b := &ComponentOperationButton{}
	if err := b.GenComponentState(c); err != nil {
		t.Error(err)
	}

	isEqual, err := cputil.IsDeepEqual(b.State, c.State)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
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

func TestComponentOperationButton_SetComponentValue(t *testing.T) {
	b := &ComponentOperationButton{
		sdk: &cptype.SDK{
			Tran: &MockTran{},
		},
	}
	b.SetComponentValue()

	expected := &ComponentOperationButton{}
	expected.Props.Type = "primary"
	expected.Props.Menu = []Menu{
		{
			Key:  "checkYaml",
			Text: b.sdk.I18n("viewOrEditYaml"),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    "checkYaml",
					Reload: true,
				},
			},
		},
		{
			Key:  "delete",
			Text: b.sdk.I18n("delete"),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    "delete",
					Reload: true,
					Command: Command{
						Key:    "goto",
						Target: "cmpClustersPods",
						State: CommandState{
							Params: map[string]string{
								"clusterName": b.State.ClusterName,
							},
						},
					},
				},
			},
		},
	}

	isEqual, err := cputil.IsDeepEqual(expected, b)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
	}
}

type mockSteveServer struct {
	cmp.SteveServer
}

func (s *mockSteveServer) DeleteSteveResource(context.Context, *apistructs.SteveRequest) error {
	return nil
}

func TestComponentOperationButton_DeletePod(t *testing.T) {
	b := &ComponentOperationButton{
		sdk: &cptype.SDK{
			Identity: &pb.IdentityInfo{
				UserID: "testUserID",
				OrgID:  "testOrgID",
			},
		},
		server: &mockSteveServer{},
		State: State{
			PodID: "test_podID",
		},
	}
	if err := b.DeletePod(); err != nil {
		t.Error(err)
	}
}

func TestComponentOperationButton_Transfer(t *testing.T) {
	b := &ComponentOperationButton{
		Type: "",
		State: State{
			ClusterName: "testClusterName",
			PodID:       "testPodID",
		},
		Props: Props{
			Type: "testType",
			Text: "testText",
			Menu: []Menu{
				{
					Key:  "testKey",
					Text: "testText",
					Operations: map[string]interface{}{
						"test": Operation{
							Key:     "testKey",
							Reload:  true,
							Confirm: "test",
							Command: Command{
								Key:    "testKey",
								Target: "testTarget",
								State: CommandState{
									Params: map[string]string{
										"k1": "v1",
										"k2": "v2",
									},
								},
							},
						},
					},
				},
			},
		},
	}
	var c cptype.Component
	b.Transfer(&c)

	isEqual, err := cputil.IsDeepEqual(b, c)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
	}
}
