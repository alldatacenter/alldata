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

package podsTable

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentPodsTable_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testClusterName",
		"countValues": map[string]int{"test": 1},
		"pageNo":      1,
		"pageSize":    10,
		"sorterData": Sorter{
			Field: "testField",
			Order: "ascend",
		},
		"total": 20,
		"values": Values{
			Kind:      []string{"test"},
			Namespace: "test",
			Status:    []string{"test"},
			Node:      []string{"test"},
			Search:    "test",
		},
		"podsTable__urlQuery": "testURLQuery",
		"activeKey":           "testKey",
	}}
	component := &ComponentPodsTable{}
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

func getTestURLQuery() (State, string) {
	v := State{
		PageNo:   2,
		PageSize: 10,
		Sorter: Sorter{
			Field: "test1",
			Order: "descend",
		},
	}
	m := map[string]interface{}{
		"pageNo":     v.PageNo,
		"pageSize":   v.PageSize,
		"sorterData": v.Sorter,
	}
	data, _ := json.Marshal(m)
	encode := base64.StdEncoding.EncodeToString(data)
	return v, encode
}

func TestComponentPodsTable_DecodeURLQuery(t *testing.T) {
	state, res := getTestURLQuery()
	table := &ComponentPodsTable{
		sdk: &cptype.SDK{
			InParams: map[string]interface{}{
				"podsTable__urlQuery": res,
			},
		},
	}
	if err := table.DecodeURLQuery(); err != nil {
		t.Errorf("test failed, %v", err)
	}
	if state.PageNo != table.State.PageNo || state.PageSize != table.State.PageSize ||
		state.Sorter.Field != table.State.Sorter.Field || state.Sorter.Order != table.State.Sorter.Order {
		t.Errorf("test failed, decode result is not expected")
	}
}

func TestComponentPodsTable_EncodeURLQuery(t *testing.T) {
	state, res := getTestURLQuery()
	table := ComponentPodsTable{State: state}
	if err := table.EncodeURLQuery(); err != nil {
		t.Error(err)
	}
	if table.State.PodsTableURLQuery != res {
		t.Errorf("test failed, unexpected url query encode result")
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

func TestComponentPodsTable_SetComponentValue(t *testing.T) {
	sdk := cptype.SDK{Tran: &MockTran{}}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &sdk)
	p := &ComponentPodsTable{}
	p.SetComponentValue(ctx)
}

func TestComponentPodsTable_Transfer(t *testing.T) {
	component := &ComponentPodsTable{
		State: State{
			ActiveKey:   "testKey",
			ClusterName: "testClusterName",
			CountValues: map[string]int{
				"test": 1,
			},
			PageNo:            1,
			PageSize:          20,
			PodsTableURLQuery: "test",
			Sorter: Sorter{
				Field: "testField",
				Order: "ascend",
			},
			Total: 20,
			Values: Values{
				Kind:      []string{"test"},
				Namespace: "test",
				Status:    []string{"test"},
				Node:      []string{"test"},
				Search:    "test",
			},
		},
		Data: Data{
			List: []Item{
				{
					ID: "testID",
					Status: Status{
						RenderType: "testType",
						Value:      "testValue",
						Status:     "testStatus",
					},
					Name: Multiple{
						RenderType: "testType",
						Direction:  "testDirection",
					},
					PodName: "testName",
					IP:      "testIP",
					Age:     "1d",
					CPURequests: Multiple{
						RenderType: "testType",
						Direction:  "testDirection",
					},
					CPURequestsNum: 1000,
					CPUPercent: Percent{
						RenderType: "testType",
						Value:      "testValue",
						Tip:        "testTip",
						Status:     "testStatus",
					},
					CPULimits: Multiple{
						RenderType: "testType",
						Direction:  "testDirection",
					},
					CPULimitsNum: 1000,
					MemoryRequests: Multiple{
						RenderType: "testType",
						Direction:  "testDirection",
					},
					MemoryRequestsNum: 1 << 30,
					MemoryPercent: Percent{
						RenderType: "testType",
						Value:      "testValue",
						Tip:        "testTip",
						Status:     "testStatus",
					},
					MemoryLimits: Multiple{
						RenderType: "testType",
						Direction:  "testDirection",
					},
					MemoryLimitsNum: 1 << 30,
					Ready:           "1",
					Node: Operate{
						RenderType: "testType",
						Value:      "testValue",
					},
					Operations: Operate{
						RenderType: "testType",
						Value:      "testValue",
						Operations: map[string]interface{}{
							"testOp": Operation{
								Key:    "testKey",
								Reload: true,
							},
						},
					},
				},
			},
		},
		Props: Props{
			RequestIgnore:   []string{"test"},
			RowKey:          "testKey",
			PageSizeOptions: []string{"test"},
			Columns: []Column{
				{
					DataIndex: "test",
					Title:     "testTitle",
					Sorter:    true,
					Fixed:     "test",
				},
			},
			Operations: map[string]interface{}{
				"testOp": Operation{
					Key:    "testKey",
					Reload: true,
				},
			},
			SortDirections: []string{"ascend"},
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
