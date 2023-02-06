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

package customFilter

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline/common"
)

func TestCustomFilter_MakeDefaultAppSelect(t *testing.T) {
	type fields struct {
		InParams *InParams
		AppName  string
	}
	tests := []struct {
		name   string
		fields fields
		want   []string
	}{
		{
			name:   "test with no appName",
			fields: fields{AppName: ""},
			want:   []string{common.Participated},
		},
		{
			name:   "test with has appName",
			fields: fields{AppName: "erda"},
			want:   []string{"erda"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &CustomFilter{
				InParams: tt.fields.InParams,
				AppName:  tt.fields.AppName,
			}
			if got := p.MakeDefaultAppSelect(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("MakeDefaultAppSelect() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestCustomFilter_MakeDefaultBranchSelect(t *testing.T) {
	type fields struct {
		AppName string
	}
	tests := []struct {
		name   string
		fields fields
		want   []string
	}{
		{
			name: "test with app",
			fields: fields{
				AppName: "erda",
			},
			want: nil,
		},
		{
			name: "test with no app",
			fields: fields{
				AppName: "",
			},
			want: []string{"master", "develop"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &CustomFilter{
				AppName: tt.fields.AppName,
			}
			if got := p.MakeDefaultBranchSelect(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("MakeDefaultBranchSelect() = %v, want %v", got, tt.want)
			}
		})
	}
}
