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
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

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
	cob := &ComponentOperationButton{
		sdk: &cptype.SDK{Tran: &MockTran{}},
	}
	cob.SetComponentValue()
	if cob.Props.Type != "primary" {
		t.Errorf("test failed, .Props.Type is unexpected, expected %s, actual %s", "primary", cob.Props.Type)
	}
	if len(cob.Props.Menu) != 1 {
		t.Errorf("test failed, length of .Props.Menu is unexpected, expected %d, actual %d", 1, len(cob.Props.Menu))
	}
}

func TestComponentOperationButton_Transfer(t *testing.T) {
	cob := &ComponentOperationButton{
		Props: Props{
			Type: "testType",
			Text: "test",
			Menu: []Menu{
				{
					Key:  "testKey",
					Text: "test",
					Operations: map[string]interface{}{
						"testOp": Operation{
							Key:     "testKey",
							Reload:  true,
							Confirm: "testConfirm",
							Command: Command{
								Key:    "testKey",
								Target: "testTarget",
								State: CommandState{
									Params: map[string]string{
										"k1": "v1",
										"k2": "v2",
									},
								},
							},
						},
					},
				},
			},
		},
	}

	var c cptype.Component
	cob.Transfer(&c)

	isEqual, err := cputil.IsDeepEqual(cob, c)
	if err != nil {
		t.Error(err)
	}
	if !isEqual {
		t.Error("test failed, data is changed after transfer")
	}
}
