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

package podsCharts

import (
	"context"
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

func TestPodsCharts_ParsePodStatus(t *testing.T) {
	sdk := cptype.SDK{Tran: &MockTran{}}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &sdk)
	pd := &PodsCharts{}
	pd.ParsePodStatus(ctx, "Running", 1, 1)
}

func TestPodsCharts_Transfer(t *testing.T) {
	component := &PodsCharts{
		Data: Data{
			Group: [][][]Pie{
				{{{
					Name:  "testName",
					Value: 1,
					Color: "testColor",
					Infos: []Info{
						{
							Main: "testMain",
							Sub:  "testSub",
							Desc: "testDesc",
						},
					},
				}}},
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
