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
	"fmt"
	"testing"

	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
)

func TestComponentPodsTable_GenComponentState(t *testing.T) {
	component := &cptype.Component{
		State: map[string]interface{}{
			"clusterName": "test",
			"pageNo":      1,
			"pageSize":    20,
			"total":       100,
			"sorterData": Sorter{
				Field: "test",
				Order: "test",
			},
		},
	}
	src, err := json.Marshal(component.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	p := &ComponentPodsTable{}
	if err := p.GenComponentState(component); err != nil {
		t.Errorf("test failed, %v", err)
	}

	dst, err := json.Marshal(p.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	fmt.Println(string(src))
	fmt.Println(string(dst))
	if string(src) != string(dst) {
		t.Error("test failed, generate result is unexpected")
	}
}

func getTestURLQuery() (State, string) {
	v := State{
		PageNo:   1,
		PageSize: 20,
		Sorter: Sorter{
			Field: "test",
			Order: "ascend",
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
				"workloadTable__urlQuery": res,
			},
		},
	}
	if err := table.DecodeURLQuery(); err != nil {
		t.Errorf("test failed, %v", err)
	}
	if state.PageNo != table.State.PageNo || state.PageSize != table.State.PageSize || state.Sorter.Field != table.State.Sorter.Field ||
		state.Sorter.Order != table.State.Sorter.Order {
		t.Errorf("test failed, edcode result is not expected")
	}
}

func TestComponentPodsTable_EncodeURLQuery(t *testing.T) {
	state, res := getTestURLQuery()
	table := &ComponentPodsTable{State: state}
	if err := table.EncodeURLQuery(); err != nil {
		t.Errorf("test failed, %v", err)
	}
	if res != table.State.PodsTableURLQuery {
		t.Error("test failed, encode url query result is unexpected")
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
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{Tran: &MockTran{}})
	p := &ComponentPodsTable{}
	p.SetComponentValue(ctx)
	if len(p.Props.PageSizeOptions) != 4 || len(p.Props.Columns) != 13 || len(p.Operations) != 1 {
		t.Errorf("test failed, length of pods table fileds is unexpected")
	}
}

func TestMatchSelector(t *testing.T) {
	selector := map[string]interface{}{
		"v1": "k1",
		"v2": "k2",
	}
	labels := map[string]interface{}{
		"v1": "k1",
		"v2": "k2",
		"v3": "k3",
	}
	if !matchSelector(selector, labels) {
		t.Errorf("test failed, expected to match selector, actual not")
	}
	labels["v1"] = "k4"
	if matchSelector(selector, labels) {
		t.Errorf("test failed, expected to mismatch selecto, actual match")
	}
}

func TestParseResource(t *testing.T) {
	zero := resource.NewQuantity(0, resource.BinarySI)
	res := parseResource("", resource.BinarySI)
	if zero.Cmp(*res) != 0 {
		t.Errorf("test failed, expected parse result: %s, actual %s", zero.String(), res.String())
	}

	gi := resource.NewQuantity(1<<30, resource.BinarySI)
	res = parseResource("1Gi", resource.BinarySI)
	if gi.Cmp(*res) != 0 {
		t.Errorf("test failed, expected parse result: %s, actual %s", gi.String(), res.String())
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
