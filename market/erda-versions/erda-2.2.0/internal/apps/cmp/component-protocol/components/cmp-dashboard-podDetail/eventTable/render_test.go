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
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

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

	if len(cet.Props.Columns) != 4 {
		t.Errorf("test failed, len of .Props.Columns is unexpected, expected 9, actual %d", len(cet.Props.Columns))
	}
	if cet.Operations == nil {
		t.Errorf("test failed, .Operations is unexpected, expected not null, actual null")
	}
	if _, ok := cet.Operations[apistructs.OnChangeSortOperation.String()]; !ok {
		t.Errorf("test failed, .Operations is unexpected, %s is not existed", apistructs.OnChangeSortOperation.String())
	}
	if _, ok := cet.Operations[apistructs.OnChangePageNoOperation.String()]; !ok {
		t.Errorf("test failed, .Operations is unexpected, %s is not existed", apistructs.OnChangePageNoOperation.String())
	}
	if _, ok := cet.Operations[apistructs.OnChangePageSizeOperation.String()]; !ok {
		t.Errorf("test failed, .Operations is unexpected, %s is not existed", apistructs.OnChangePageSizeOperation.String())
	}
}

func TestComponentEventTable_Transfer(t *testing.T) {
	component := &ComponentEventTable{
		Data: Data{List: []Item{
			{
				ID:                "1",
				LastSeen:          "1s",
				LastSeenTimestamp: 1,
				Type:              "test",
				Reason:            "test",
				Message:           "test",
			},
		}},
		State: State{
			ClusterName: "testCluster",
			PodID:       "testID",
		},
		Props: Props{
			RequestIgnore: []string{"data"},
			RowKey:        "id",
			Pagination:    true,
			Columns: []Column{
				{
					DataIndex: "test",
					Title:     "test",
					Width:     120,
				},
			},
		},
		Operations: map[string]interface{}{
			"testOp": Operation{
				Key:    "test",
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

func TestComponentEventTable_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testClusterName",
		"podId":       "testPodID",
	}}
	ct := &ComponentEventTable{}
	if err := ct.GenComponentState(c); err != nil {
		t.Fatal(err)
	}

	ok, err := cputil.IsDeepEqual(c.State, ct.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}
