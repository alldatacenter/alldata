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

package ContainerTable

import (
	"context"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentContainerTable_Transfer(t *testing.T) {
	component := &ContainerTable{
		Data: map[string][]Data{
			"test": {
				{
					Status: Status{
						RenderType: "text",
						Value:      "testValue",
						Status:     "testStatus",
					},
					Ready: "true",
					Name:  "test",
					Images: Images{
						RenderType: "text",
						Value: Value{
							Text: "test",
						},
					},
					RestartCount: "10",
					Operate: Operate{
						Operations: map[string]Operation{
							"testOp": {
								Key:    "test",
								Text:   "test",
								Reload: true,
							},
						},
						RenderType: "text",
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
			RowKey:        "id",
			Pagination:    true,
			Scroll: Scroll{
				X: 1,
			},
			Columns: []Column{
				{
					DataIndex: "test",
					Title:     "test",
					Fixed:     "test",
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

func TestContainerTable_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testClusterName",
		"podId":       "testPodId",
	}}
	ct := &ContainerTable{}
	if err := ct.GenComponentState(c); err != nil {
		t.Fatal(err)
	}
	ok, err := cputil.IsDeepEqual(ct.State, c.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Error("test failed, json is not equal")
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

func TestParseContainerStatus(t *testing.T) {
	sdk := cptype.SDK{Tran: &MockTran{}}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &sdk)
	parseContainerStatus(ctx, "running")
	parseContainerStatus(ctx, "waiting")
	parseContainerStatus(ctx, "terminated")
}
