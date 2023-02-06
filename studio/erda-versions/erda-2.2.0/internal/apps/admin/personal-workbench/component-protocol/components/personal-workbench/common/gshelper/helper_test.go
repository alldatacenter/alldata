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

package gshelper

import (
	"fmt"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
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

func TestGSHelper_GetWorkbenchItemType(t *testing.T) {
	type fields struct {
		gs *cptype.GlobalStateData
	}
	tests := []struct {
		name   string
		fields fields
		want   apistructs.WorkbenchItemType
		want1  bool
	}{
		// TODO: Add test cases.
		{
			name:  "case1",
			want:  "",
			want1: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &GSHelper{
				gs: tt.fields.gs,
			}
			got, got1 := h.GetWorkbenchItemType()
			if got != tt.want {
				t.Errorf("GetWorkbenchItemType() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("GetWorkbenchItemType() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func TestGSHelper_SetFilterName(t *testing.T) {
	type fields struct {
		gs *cptype.GlobalStateData
	}
	type args struct {
		name string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		// TODO: Add test cases.
		{
			name:   "case1",
			fields: fields{gs: &cptype.GlobalStateData{}},
			args: args{
				name: "a",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &GSHelper{
				gs: tt.fields.gs,
			}
			h.SetFilterName(tt.args.name)
		})
	}
}

func TestGSHelper_GetMsgTabName(t *testing.T) {
	type fields struct {
		gs *cptype.GlobalStateData
	}
	tests := []struct {
		name   string
		fields fields
		want   apistructs.WorkbenchItemType
		want1  bool
	}{
		// TODO: Add test cases.
		{
			name:   "case1",
			fields: fields{gs: &cptype.GlobalStateData{}},
			want:   apistructs.WorkbenchItemUnreadMes,
			want1:  false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &GSHelper{
				gs: tt.fields.gs,
			}
			got, got1 := h.GetMsgTabName()
			if got != tt.want {
				t.Errorf("GetMsgTabName() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("GetMsgTabName() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func TestGSHelper_SetMsgTabName(t *testing.T) {
	type fields struct {
		gs *cptype.GlobalStateData
	}
	type args struct {
		name string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{name: "1"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &GSHelper{
				gs: tt.fields.gs,
			}
			h.SetMsgTabName(tt.args.name)
		})
	}
}

func TestGSHelper_GetFilterName(t *testing.T) {
	type fields struct {
		gs *cptype.GlobalStateData
	}
	tests := []struct {
		name   string
		fields fields
		want   string
		want1  bool
	}{
		// TODO: Add test cases.
		{
			name:   "case1",
			fields: fields{gs: &cptype.GlobalStateData{}},
			want1:  false,
			want:   "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &GSHelper{
				gs: tt.fields.gs,
			}
			got, got1 := h.GetFilterName()
			if got != tt.want {
				t.Errorf("GetFilterName() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("GetFilterName() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func TestGSHelper_SetWorkbenchItemType(t *testing.T) {
	type fields struct {
		gs *cptype.GlobalStateData
	}
	type args struct {
		wbType string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		// TODO: Add test cases.
		{
			name:   "case1",
			fields: fields{gs: &cptype.GlobalStateData{}},
			args:   args{wbType: ""},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &GSHelper{
				gs: tt.fields.gs,
			}
			h.SetWorkbenchItemType(tt.args.wbType)
		})
	}
}
