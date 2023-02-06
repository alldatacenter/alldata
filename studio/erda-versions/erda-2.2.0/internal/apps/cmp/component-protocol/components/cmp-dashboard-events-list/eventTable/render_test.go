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

package eventTable

import (
	"context"
	"encoding/base64"
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

func TestComponentEventTable_DecodeURLQuery(t *testing.T) {
	state, res := getTestURLQuery()
	table := &ComponentEventTable{
		sdk: &cptype.SDK{
			InParams: map[string]interface{}{
				"eventTable__urlQuery": res,
			},
		},
	}
	if err := table.DecodeURLQuery(); err != nil {
		t.Errorf("test failed, %v", err)
	}
	if state.PageNo != table.State.PageNo || state.PageSize != table.State.PageSize ||
		state.Sorter.Field != table.State.Sorter.Field || state.Sorter.Order != table.State.Sorter.Order {
		t.Errorf("test failed, edcode result is not expected")
	}
}

func TestComponentEventTable_EncodeURLQuery(t *testing.T) {
	state, res := getTestURLQuery()
	table := ComponentEventTable{State: state}
	if err := table.EncodeURLQuery(); err != nil {
		t.Error(err)
	}
	if table.State.EventTableUQLQuery != res {
		t.Errorf("test failed, expected url query encode result")
	}
}

func TestComponentEventTable_GenComponentState(t *testing.T) {
	component := &cptype.Component{
		State: map[string]interface{}{
			"clusterName": "test1",
			"pageNo":      2,
			"pageSize":    10,
			"sorterData": Sorter{
				Field: "test1",
				Order: "descend",
			},
			"total": 100,
			"filterValues": FilterValues{
				Namespace: "test1",
				Type:      []string{"test1"},
			},
		},
	}
	src, err := json.Marshal(component.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	f := &ComponentEventTable{}
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

type MockSteveServer struct {
	cmp.SteveServer
}

func (m *MockSteveServer) ListSteveResource(context.Context, *apistructs.SteveRequest) ([]types.APIObject, error) {
	return []types.APIObject{
		{
			Type: "testType",
			ID:   "test",
			Object: map[string]interface{}{
				"metadata": map[string]interface{}{
					"fields": []interface{}{
						"1m",
						"Normal",
						"Scheduled",
						"pod/test-0",
						"",
						"default",
						"Success",
						"1m",
						1,
						"test",
					},
				},
			},
		},
	}, nil
}

func TestComponentEventTable_RenderList(t *testing.T) {
	cet := ComponentEventTable{
		sdk: &cptype.SDK{
			Tran: &MockTran{},
			Identity: &pb.IdentityInfo{
				UserID: "1",
				OrgID:  "1",
			},
		},
		server: &MockSteveServer{},
		State: State{
			Sorter: Sorter{
				Field: "testField",
				Order: "ascend",
			},
		},
	}
	if err := cet.RenderList(); err != nil {
		t.Errorf("test failed, %v", err)
	}
}

func TestContain(t *testing.T) {
	arr := []string{
		"a", "b", "c", "d",
	}
	if contain(arr, "e") {
		t.Errorf("test failed, expected not contain \"e\", actual do")
	}
	if !contain(arr, "a") || !contain(arr, "b") || !contain(arr, "c") || !contain(arr, "d") {
		t.Errorf("test failed, expected contain \"a\" , \"b\", \"c\" and \"d\", actual not")
	}
}

func TestGetRange(t *testing.T) {
	length := 0
	pageNo := 1
	pageSize := 20
	l, r := getRange(length, pageNo, pageSize)
	if l != 0 {
		t.Errorf("test failed, l is unexpected, expected 0, actual %d", l)
	}
	if r != 0 {
		t.Errorf("test failed, r is unexpected, expected 0, actual %d", r)
	}

	length = 21
	pageNo = 2
	pageSize = 20
	l, r = getRange(length, pageNo, pageSize)
	if l != 20 {
		t.Errorf("test failed, l is unexpected, expected 20, actual %d", l)
	}
	if r != 21 {
		t.Errorf("test failed, r is unexpected, expected 21, actual %d", r)
	}

	length = 20
	pageNo = 2
	pageSize = 50
	l, r = getRange(length, pageNo, pageSize)
	if l != 0 {
		t.Errorf("test failed, l is unexpected, expected 0, actual %d", l)
	}
	if r != 20 {
		t.Errorf("test failed, r is unexpected, expected 20, actual %d", r)
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

func TestComponentEventTable_SetComponentValue(t *testing.T) {
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{Tran: &MockTran{}})
	cet := &ComponentEventTable{}
	cet.SetComponentValue(ctx)
	if len(cet.Props.PageSizeOptions) != 4 {
		t.Errorf("test failed, len of .Props.PageSizeOptions is unexpected, expected 4, actual %d", len(cet.Props.PageSizeOptions))
	}
	if len(cet.Props.Columns) != 9 {
		t.Errorf("test failed, len of .Props.Columns is unexpected, expected 9, actual %d", len(cet.Props.Columns))
	}
	if cet.Operations == nil {
		t.Errorf("test failed, .Operations is unexpected, expected not null, actual null")
	}
	if _, ok := cet.Operations[apistructs.OnChangeSortOperation.String()]; !ok {
		t.Errorf("test failed, .Operations is unexpected, %s is not existed", apistructs.OnChangeSortOperation.String())
	}
}

func TestComponentEventTable_Transfer(t *testing.T) {
	component := ComponentEventTable{
		Type: "",
		State: State{
			ClusterName: "testCluster",
			FilterValues: FilterValues{
				Namespace: "default",
				Type:      []string{"Normal"},
				Search:    "test",
			},
			PageNo:   1,
			PageSize: 20,
			Sorter: Sorter{
				Field: "test",
				Order: "ascend",
			},
			Total:              10,
			EventTableUQLQuery: "testQuery",
		},
		Props: Props{
			RequestIgnore:   []string{"data"},
			PageSizeOptions: []string{"10"},
			Columns: []Column{
				{
					DataIndex: "test",
					Title:     "test",
					Sorter:    true,
				},
			},
		},
		Data: Data{
			List: []Item{
				{
					LastSeen:          "1s",
					LastSeenTimestamp: 1,
					Type:              "Normal",
					Reason:            "test",
					Object:            "test",
					Source:            "test",
					Message:           "test",
					Count:             "10",
					CountNum:          10,
					Name:              "test",
					Namespace:         "default",
				},
			},
		},
		Operations: map[string]interface{}{
			"testOp": Operation{
				Key:    "testOp",
				Reload: true,
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
