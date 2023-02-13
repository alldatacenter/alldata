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

package releaseTable

import (
	"encoding/base64"
	"encoding/json"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
)

func TestComponentReleaseTable_GenComponentState(t *testing.T) {
	component := &cptype.Component{
		State: map[string]interface{}{
			"releaseTable__urlQuery": "testURLQuery",
			"pageNo":                 1,
			"pageSize":               20,
			"total":                  100,
			"selectedRowKeys": []string{
				"testKey",
			},
			"sorterData": Sorter{
				Field: "testField",
				Order: "DESC",
			},
			"isProjectRelease": true,
			"projectID":        1,
			"isFormal":         true,
			"filterValues": FilterValues{
				BranchID:          "testBranchID",
				CommitID:          "testCommitID",
				UserIDs:           []string{"testUserID"},
				CreatedAtStartEnd: []int64{1, 1},
			},
			"applicationID": 1,
		},
	}
	r := &ComponentReleaseTable{}
	if err := r.GenComponentState(component); err != nil {
		t.Fatal(err)
	}
	isEqual, err := util.IsDeepEqual(r.State, component.State)
	if err != nil {
		t.Fatal(err)
	}
	if !isEqual {
		t.Errorf("test failed, state is not expected after generate")
	}
}

func getPair() (State, string) {
	state := State{
		PageNo:   1,
		PageSize: 20,
		Sorter: Sorter{
			Field: "testField",
			Order: "DESC",
		},
	}
	m := map[string]interface{}{
		"pageNo":     state.PageNo,
		"pageSize":   state.PageSize,
		"sorterData": state.Sorter,
	}
	data, err := json.Marshal(m)
	if err != nil {
		panic(err)
	}
	encode := base64.StdEncoding.EncodeToString(data)
	return state, encode
}

func TestComponentReleaseTable_DecodeURLQuery(t *testing.T) {
	state, encode := getPair()
	r := ComponentReleaseTable{
		sdk: &cptype.SDK{InParams: map[string]interface{}{
			"releaseTable__urlQuery": encode,
		}},
	}
	if err := r.DecodeURLQuery(); err != nil {
		t.Fatal(err)
	}
	isEqual, err := util.IsDeepEqual(state, r.State)
	if err != nil {
		t.Fatal(err)
	}
	if !isEqual {
		t.Errorf("test failed, state is not expected after decode")
	}
}

func TestComponentReleaseTable_EncodeURLQuery(t *testing.T) {
	state, encode := getPair()
	r := ComponentReleaseTable{State: state}
	if err := r.EncodeURLQuery(); err != nil {
		t.Fatal(err)
	}
	if encode != r.State.ReleaseTableURLQuery {
		t.Errorf("test failed, url query is not expected after encode")
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

func TestComponentReleaseTable_SetComponentValue(t *testing.T) {
	r := ComponentReleaseTable{sdk: &cptype.SDK{Tran: &MockTran{}}}
	r.SetComponentValue()
}

func TestComponentReleaseTable_Transfer(t *testing.T) {
	tmp := true
	r := ComponentReleaseTable{
		Data: Data{
			List: []Item{
				{
					ID: "testID",
					Version: DoubleRowWithIcon{
						RenderType: "testType",
						Value:      "testValue",
						ExtraContent: ExtraContent{
							RenderType: "testType",
							ShowCount:  1,
							Value: []TagValue{
								{
									Label: "testLabel",
									Color: "blue",
								},
							},
						},
					},
					Application: "testApplication",
					Desc:        "testDesc",
					Creator: Creator{
						RenderType: "testType",
						Value:      []string{"testValue"},
					},
					CreatedAt: "testCreated",
					Operations: TableOperations{
						Operations: map[string]interface{}{
							"testOp": Operation{
								Command: Command{
									JumpOut: true,
									Key:     "testKey",
									Target:  "testTarget",
								},
								Confirm: "testConfirm",
								Key:     "testKey",
								Reload:  true,
								Text:    "testText",
								Meta: map[string]interface{}{
									"test": "test",
								},
								SuccessMsg: "testMsg",
							},
						},
						RenderType: "testType",
					},
					BatchOperations: []string{"testBatchOperation"},
				},
			},
		},
		Props: Props{
			BatchOperations: []string{"testBatchOperations"},
			Selectable:      true,
			Columns: []Column{
				{
					DataIndex: "testIndex",
					Title:     "testTitle",
					Sorter:    true,
				},
			},
			PageSizeOptions: []string{
				"10",
			},
			RowKey: "testRowKey",
		},
		State: State{
			ReleaseTableURLQuery: "testURLQuery",
			PageNo:               1,
			PageSize:             20,
			Total:                100,
			SelectedRowKeys:      []string{"testKey"},
			Sorter: Sorter{
				Field: "testField",
				Order: "DESC",
			},
			IsProjectRelease: true,
			ProjectID:        1,
			IsFormal:         &tmp,
			FilterValues: FilterValues{
				BranchID: "testBranchID",
				CommitID: "testCommitID",
				UserIDs: []string{
					"testUserID",
				},
				CreatedAtStartEnd: []int64{1, 1},
			},
			ApplicationID: 1,
		},
		Operations: map[string]interface{}{
			"testOp": Operation{
				Command: Command{
					JumpOut: true,
					Key:     "testKey",
					Target:  "testTarget",
				},
				Confirm: "testConfirm",
				Key:     "testKey",
				Reload:  true,
				Text:    "testText",
				Meta: map[string]interface{}{
					"testMeta": "test",
				},
				SuccessMsg: "testMsg",
			},
		},
	}
	component := &cptype.Component{}
	r.Transfer(component)
	isEqual, err := util.IsDeepEqual(r, component)
	if err != nil {
		t.Fatal(err)
	}
	if !isEqual {
		t.Errorf("test failed, component is not expected after transfer")
	}
}
