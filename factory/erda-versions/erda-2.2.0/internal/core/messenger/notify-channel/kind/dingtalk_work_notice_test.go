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

package kind

import "testing"

func TestDingDingWorkNotice_Validate(t *testing.T) {
	type fields struct {
		AgentId   int64
		AppKey    string
		AppSecret string
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		{
			name: "case1",
			fields: fields{
				AgentId:   0,
				AppKey:    "ss",
				AppSecret: "ss",
			},
			wantErr: true,
		},
		{
			name: "case2",
			fields: fields{
				AgentId:   2,
				AppKey:    "",
				AppSecret: "ss",
			},
			wantErr: true,
		},
		{
			name: "case3",
			fields: fields{
				AgentId:   3,
				AppKey:    "",
				AppSecret: "",
			},
			wantErr: true,
		},
		{
			name: "case4",
			fields: fields{
				AgentId:   0,
				AppKey:    "",
				AppSecret: "",
			},
			wantErr: true,
		},
		{
			name: "2",
			fields: fields{
				AgentId:   3,
				AppKey:    "ss",
				AppSecret: "ss",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ding := &DingDingWorkNotice{
				AgentId:   tt.fields.AgentId,
				AppKey:    tt.fields.AppKey,
				AppSecret: tt.fields.AppSecret,
			}
			if err := ding.Validate(); (err != nil) != tt.wantErr {
				t.Errorf("Validate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
