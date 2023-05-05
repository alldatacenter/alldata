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

package AddLabelModal

import (
	"fmt"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
)

type NopTranslator struct{}

func (t NopTranslator) Get(lang i18n.LanguageCodes, key, def string) string { return key }

func (t NopTranslator) Text(lang i18n.LanguageCodes, key string) string { return key }

func (t NopTranslator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return fmt.Sprintf(key, args...)
}

func TestAddLabelModal_getProps(t *testing.T) {
	tests := []struct {
		name string
	}{
		// TODO: Add test cases.
		{
			"1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			alm := &AddLabelModal{}
			alm.SDK = &cptype.SDK{
				Tran: NopTranslator{},
			}
			alm.GetProps()
		})
	}
}
