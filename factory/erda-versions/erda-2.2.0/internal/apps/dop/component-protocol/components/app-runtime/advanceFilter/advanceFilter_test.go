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

package page

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

var defaultSDK = &cptype.SDK{

	GlobalState: &cptype.GlobalStateData{},
	Tran:        &NopTranslator{},
}

func Test_getSelectCondition(t *testing.T) {
	type args struct {
		sdk  *cptype.SDK
		keys map[string]bool
		key  string
	}
	tests := []struct {
		name string
		args args
		want Condition
	}{
		{
			name: "1",
			args: args{
				sdk: defaultSDK,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			getSelectCondition(tt.args.sdk, tt.args.keys, tt.args.key)
		})
	}
}

//func Test_getRangeCondition(t *testing.T) {
//	type args struct {
//		sdk *cptype.SDK
//		key string
//	}
//	tests := []struct {
//		name string
//		args args
//		want Condition
//	}{
//		// TODO: Add test cases.
//		{
//			name: "1",
//			args: args{
//				sdk: defaultSDK,
//				key: "",
//			},
//		},
//	}
//	for _, tt := range tests {
//		t.Run(tt.name, func(t *testing.T) {
//			getRangeCondition(tt.args.sdk, tt.args.key)
//		})
//	}
//}

func TestAdvanceFilter_generateUrlQueryParams(t *testing.T) {
	type args struct {
		Values cptype.ExtraMap
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "1",
			args: args{Values: map[string]interface{}{}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			af := &AdvanceFilter{}
			af.generateUrlQueryParams(tt.args.Values)
		})
	}
}

func TestAdvanceFilter_flushOptsByFilter(t *testing.T) {
	type args struct {
		filterEntity string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{filterEntity: "×"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			af := &AdvanceFilter{}
			af.flushOptsByFilter(tt.args.filterEntity)
		})
	}
}

func TestAdvanceFilter_generateUrlQueryParams1(t *testing.T) {
	type fields struct{}
	type args struct {
		Values cptype.ExtraMap
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{map[string]interface{}{"1": 1}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			af := &AdvanceFilter{}
			af.generateUrlQueryParams(tt.args.Values)
		})
	}
}
