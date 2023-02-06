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

package workloadStatus

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/rancher/apiserver/pkg/types"

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

func (m *MockSteveServer) GetSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error) {
	return types.APIObject{
		Object: map[string]interface{}{
			"kind": "Deployment",
			"metadata": map[string]interface{}{
				"fields": []interface{}{
					"test",
					"1/1",
					1,
					1,
					"1d",
					"test",
					"test-image",
					"",
				},
				"labels": map[string]string{
					"key1": "value1",
				},
				"annotations": map[string]string{
					"key2": "value2",
				},
			},
		},
	}, nil
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

func TestComponentWorkloadStatus_SetComponentValue(t *testing.T) {
	i := ComponentWorkloadStatus{
		sdk: &cptype.SDK{
			Tran: &MockTran{},
			Identity: &pb.IdentityInfo{
				UserID: "1",
				OrgID:  "1",
			},
		},
		server: &MockSteveServer{},
		State: State{
			WorkloadID: "apps.deployments_default_test",
		},
	}
	if err := i.SetComponentValue(); err != nil {
		t.Errorf("test failed, %v", err)
	}
}

func TestComponentWorkloadStatus_GenComponentState(t *testing.T) {
	component := &cptype.Component{
		State: map[string]interface{}{
			"clusterName": "test",
			"workloadId":  "test",
		},
	}
	src, err := json.Marshal(component.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	f := &ComponentWorkloadStatus{}
	if err := f.GenComponentState(component); err != nil {
		t.Errorf("test failed, %v", err)
	}

	dst, err := json.Marshal(f.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	fmt.Println(string(src))
	fmt.Println(string(dst))
	if string(src) != string(dst) {
		t.Error("test failed, generate result is unexpected")
	}
}

func TestComponentWorkloadStatus_Transfer(t *testing.T) {
	component := &ComponentWorkloadStatus{
		Props: Props{
			Text:      "testText",
			Status:    "testStatus",
			Breathing: true,
		},
		State: State{
			ClusterName: "testClusterName",
			WorkloadID:  "testWorkloadID",
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
