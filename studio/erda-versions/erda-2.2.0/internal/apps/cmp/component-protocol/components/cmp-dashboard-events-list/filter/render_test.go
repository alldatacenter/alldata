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

package filter

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestComponentFilter_GenComponentState(t *testing.T) {
	component := &cptype.Component{
		State: map[string]interface{}{
			"clusterName": "test",
			"values": Values{
				Type: []string{
					"Normal",
				},
				Namespace: "default",
				Search:    "test",
			},
		},
	}
	src, err := json.Marshal(component.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	p := &ComponentFilter{}
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

func getTestURLQuery() (Values, string) {
	v := Values{
		Type:      []string{"Normal"},
		Namespace: "default",
		Search:    "test",
	}
	m := map[string]interface{}{
		"type":      v.Type,
		"namespace": v.Namespace,
		"search":    v.Search,
	}
	data, _ := json.Marshal(m)
	encode := base64.StdEncoding.EncodeToString(data)
	return v, encode
}

func isEqual(arr1, arr2 []string) bool {
	if len(arr1) != len(arr2) {
		return false
	}
	for i := range arr1 {
		if arr1[i] != arr2[i] {
			return false
		}
	}
	return true
}

func TestComponentFilter_DecodeURLQuery(t *testing.T) {
	values, res := getTestURLQuery()
	table := &ComponentFilter{
		sdk: &cptype.SDK{
			InParams: map[string]interface{}{
				"filter__urlQuery": res,
			},
		},
	}
	if err := table.DecodeURLQuery(); err != nil {
		t.Errorf("test failed, %v", err)
	}
	if !isEqual(values.Type, table.State.Values.Type) || values.Namespace != table.State.Values.Namespace ||
		values.Search != table.State.Values.Search {
		t.Errorf("test failed, edcode result is not expected")
	}
}

func TestComponentFilter_EncodeURLQuery(t *testing.T) {
	values, res := getTestURLQuery()
	table := &ComponentFilter{State: State{
		Values: values,
	}}
	if err := table.EncodeURLQuery(); err != nil {
		t.Errorf("test failed, %v", err)
	}
	if res != table.State.FilterURLQuery {
		t.Error("test failed, encode url query result is unexpected")
	}
}

func TestHasSuffix(t *testing.T) {
	if _, ok := hasSuffix("project-1-dev"); !ok {
		t.Errorf("test failed, \"project-1-dev\" has project suffix, actual not")
	}
	if _, ok := hasSuffix("project-2-staging"); !ok {
		t.Errorf("test failed, \"project-2-staging\" has project suffix, actual not")
	}
	if _, ok := hasSuffix("project-3-test"); !ok {
		t.Errorf("test failed, \"project-3-test\" has project suffix, actual not")
	}
	if _, ok := hasSuffix("project-4-prod"); !ok {
		t.Errorf("test failed, \"project-4-prod\" has project suffix, actual not")
	}
	if _, ok := hasSuffix("project-5-custom"); ok {
		t.Errorf("test failed, \"project-5-custom\" does not have project suffix, actul do")
	}
}

func TestComponentFilter_Transfer(t *testing.T) {
	component := ComponentFilter{
		Type: "",
		State: State{
			ClusterName: "testCluster",
			Conditions: []Condition{
				{
					HaveFilter:  true,
					Key:         "test",
					Placeholder: "test",
					Label:       "test",
					Type:        "test",
					Fixed:       true,
					Options: []Option{
						{
							Label: "test",
							Value: "test",
						},
					},
				},
			},
			Values: Values{
				Namespace: "default",
				Search:    "test",
				Type:      []string{"Normal"},
			},
			FilterURLQuery: "test",
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
