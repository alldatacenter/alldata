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
	"fmt"
	"testing"

	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/wrangler/pkg/data"
	appsv1 "k8s.io/api/apps/v1"
	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protobuf/proto-go/cp/pb"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

type MockSteveServer struct {
	cmp.SteveServer
}

func (s *MockSteveServer) GetSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	deploy := appsv1.Deployment{
		Spec: appsv1.DeploymentSpec{
			Template: v1.PodTemplateSpec{
				Spec: v1.PodSpec{
					Affinity: &v1.Affinity{
						NodeAffinity: &v1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &v1.NodeSelector{
								NodeSelectorTerms: []v1.NodeSelectorTerm{
									{
										MatchExpressions: []v1.NodeSelectorRequirement{
											{
												Key:      "dice/platform",
												Operator: "Exists",
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}
	daemonset := appsv1.DaemonSet{
		Spec: appsv1.DaemonSetSpec{
			Template: v1.PodTemplateSpec{
				Spec: v1.PodSpec{
					Affinity: &v1.Affinity{
						NodeAffinity: &v1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &v1.NodeSelector{
								NodeSelectorTerms: []v1.NodeSelectorTerm{
									{
										MatchExpressions: []v1.NodeSelectorRequirement{
											{
												Key:      "dice/platform",
												Operator: "DoesNotExist",
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}
	statefulSet := appsv1.StatefulSet{
		Spec: appsv1.StatefulSetSpec{
			Template: v1.PodTemplateSpec{
				Spec: v1.PodSpec{
					Affinity: &v1.Affinity{
						NodeAffinity: &v1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &v1.NodeSelector{
								NodeSelectorTerms: []v1.NodeSelectorTerm{
									{
										MatchExpressions: []v1.NodeSelectorRequirement{
											{
												Key:      "dice/platform",
												Operator: "In",
												Values: []string{
													"true",
												},
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}

	var (
		obj data.Object
		err error
	)
	apiObj := types.APIObject{}
	switch req.Type {
	case apistructs.K8SDeployment:
		obj, err = data.Convert(deploy)
	case apistructs.K8SDaemonSet:
		obj, err = data.Convert(daemonset)
	case apistructs.K8SStatefulSet:
		obj, err = data.Convert(statefulSet)
	}
	if err != nil {
		return types.APIObject{}, err
	}
	apiObj.Object = obj
	return apiObj, nil
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
			Identity: &pb.IdentityInfo{
				UserID: "testUserID",
				OrgID:  "testOrgID",
			},
		},
		server: &MockSteveServer{},
	}

	b.State.WorkloadID = fmt.Sprintf("%s_default_test", apistructs.K8SDeployment)
	b.SetComponentValue()
	menu := b.Props.Menu
	if len(menu) != 3 {
		t.Fatalf("length of menu is unexpected")
	}
	operation, ok := menu[2].Operations["click"].(Operation)
	if !ok {
		t.Fatalf("unexpect type of click operation")
	}
	if !operation.Disabled {
		t.Errorf("expected value of operation.Disabled is true, got false")
	}

	b.State.WorkloadID = fmt.Sprintf("%s_default_test", apistructs.K8SDaemonSet)
	b.SetComponentValue()
	menu = b.Props.Menu
	if len(menu) != 3 {
		t.Fatalf("length of menu is unexpected")
	}
	operation, ok = menu[2].Operations["click"].(Operation)
	if !ok {
		t.Fatalf("unexpect type of click operation")
	}
	if operation.Disabled {
		t.Errorf("expected value of operation.Disabled is false, got true")
	}

	b.State.WorkloadID = fmt.Sprintf("%s_default_test", apistructs.K8SStatefulSet)
	b.SetComponentValue()
	menu = b.Props.Menu
	if len(menu) != 3 {
		t.Fatalf("length of menu is unexpected")
	}
	operation, ok = menu[2].Operations["click"].(Operation)
	if !ok {
		t.Fatalf("unexpect type of click operation")
	}
	if !operation.Disabled {
		t.Errorf("expected value of operation.Disabled is true, got false")
	}
}

func TestComponentOperationButton_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testCluster",
		"workloadId":  "testID",
	}}
	component := &ComponentOperationButton{}
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

func TestComponentOperationButton_Transfer(t *testing.T) {
	component := &ComponentOperationButton{
		State: State{
			ClusterName: "testClusterName",
			WorkloadID:  "testWorkloadId",
		},
		Props: Props{
			Type: "testType",
			Text: "testTex",
			Menu: []Menu{
				{
					Key:  "testKey",
					Text: "testText",
					Operations: map[string]interface{}{
						"testOp": Operation{
							Key:        "testKey",
							Reload:     true,
							SuccessMsg: "testMsg",
							Confirm:    "testConfirm",
							Command: Command{
								Key:    "testKey",
								Target: "testTarget",
								State: CommandState{
									Params: map[string]string{
										"test": "test",
									},
								},
							},
							Disabled:    true,
							DisabledTip: "testTip",
						},
					},
				},
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
