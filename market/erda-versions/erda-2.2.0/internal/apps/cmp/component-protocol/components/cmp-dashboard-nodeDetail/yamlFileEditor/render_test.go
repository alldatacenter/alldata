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

package yamlFileEditor

import (
	"context"
	"testing"

	"github.com/rancher/apiserver/pkg/types"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protobuf/proto-go/cp/pb"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentYamlFileEditor_GenComponentState(t *testing.T) {
	c := &cptype.Component{
		State: map[string]interface{}{
			"clusterName": "testCluster",
			"nodeId":      "testNodeId",
			"value":       "testValue",
		},
	}

	f := &ComponentYamlFileEditor{}
	if err := f.GenComponentState(c); err != nil {
		t.Error(err)
	}

	isEqual, err := cputil.IsDeepEqual(f.State, c.State)
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

func (s *mockSteveServer) UpdateSteveResource(context.Context, *apistructs.SteveRequest) (types.APIObject, error) {
	return types.APIObject{}, nil
}

func TestComponentYamlFileEditor_UpdateNode(t *testing.T) {
	body := `{"test": "true"}`
	f := &ComponentYamlFileEditor{
		sdk: &cptype.SDK{
			Identity: &pb.IdentityInfo{
				UserID: "testUserID",
				OrgID:  "testOrgID",
			},
		},
		server: &mockSteveServer{},
		State: State{
			Value: body,
		},
	}
	if err := f.UpdateNode(); err != nil {
		t.Error(err)
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

func TestComponentYamlFileEditor_SetComponentValue(t *testing.T) {
	f := &ComponentYamlFileEditor{
		sdk: &cptype.SDK{
			Tran: &MockTran{},
		},
	}
	f.SetComponentValue()
	if !f.Props.Bordered {
		t.Errorf("test failed, .Props.Bordered is unexpected, expected true, got false")
	}
	if len(f.Props.FileValidate) != 2 {
		t.Errorf("test failed, len of .Props.FileValidata is unexpected, expected %d, got %d", 2, len(f.Props.FileValidate))
	}
	if f.Props.MinLines != 22 {
		t.Errorf("test failed, .Props.Minlines is unexpected, expected %d, got %d", 22, f.Props.MinLines)
	}
	if _, ok := f.Operations["submit"]; !ok {
		t.Errorf("test failed, submit operation is not existed in operations")
	}
}
