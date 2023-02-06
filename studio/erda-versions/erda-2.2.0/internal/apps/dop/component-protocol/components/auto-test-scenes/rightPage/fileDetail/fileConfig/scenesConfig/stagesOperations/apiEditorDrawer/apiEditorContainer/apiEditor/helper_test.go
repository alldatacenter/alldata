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

package apiEditor

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
)

func TestGenEmptyAPISpecStr(t *testing.T) {
	testEmptyAPISpec, testEmptyAPISpecStr := genEmptyAPISpecStr()
	assert.Equal(t, "GET", testEmptyAPISpec.APIInfo.Method)
	assert.Equal(t, `{"apiSpec":{"id":"","name":"","url":"","method":"GET","headers":null,"params":null,"body":{"type":"","content":null},"out_params":null,"asserts":null},"loop":null}`,
		testEmptyAPISpecStr)
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

func Test_genProps(t *testing.T) {
	ae := ApiEditor{
		sdk: &cptype.SDK{
			Tran: &MockTran{},
		},
	}
	type args struct {
		input       string
		execute     string
		replaceOpts []replaceOption
	}
	tests := []struct {
		name string
		args args
		want interface{}
	}{
		{
			name: "test_replace_loop_options",
			args: args{
				input:   "",
				execute: "",
				replaceOpts: []replaceOption{
					{
						key:   LoopFormFieldDefaultExpand,
						value: "test",
					},
				},
			},
			want: cptype.ComponentProps{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ae.genProps(tt.args.input, tt.args.execute, tt.args.replaceOpts...); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("genProps() = %v, want %v", got, tt.want)
			}
		})
	}
}
