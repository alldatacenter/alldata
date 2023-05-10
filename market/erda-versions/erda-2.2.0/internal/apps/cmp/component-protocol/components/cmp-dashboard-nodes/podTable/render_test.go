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

package podTable

import (
	"fmt"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/table"
)

type NopTranslator struct{}

func (t NopTranslator) Get(lang i18n.LanguageCodes, key, def string) string { return key }

func (t NopTranslator) Text(lang i18n.LanguageCodes, key string) string { return key }

func (t NopTranslator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return fmt.Sprintf(key, args...)
}

func TestPodInfoTable_getProps(t *testing.T) {
	type fields struct {
		Table table.Table
		Data  []table.RowItem
	}
	tests := []struct {
		name   string
		fields fields
	}{
		{
			"case1",
			fields{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ct := &PodInfoTable{
				Table: tt.fields.Table,
				Data:  tt.fields.Data,
			}
			ct.SDK = &cptype.SDK{
				Tran: NopTranslator{},
			}
			ct.GetProps()
		})
	}
}
