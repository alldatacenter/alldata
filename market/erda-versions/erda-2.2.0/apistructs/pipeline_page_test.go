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

package apistructs

import (
	"testing"
)

func TestPagePipeline_GetRunUserID(t *testing.T) {
	type fields struct {
		ID    uint64
		Extra PipelineExtra
	}
	tests := []struct {
		name   string
		fields fields
		want   string
	}{
		{
			name: "test with empty",
			fields: fields{
				ID: 10001,
				Extra: PipelineExtra{RunUser: &PipelineUser{
					ID:   nil,
					Name: "erda",
				}},
			},
			want: "",
		},
		{
			name: "test with empty2",
			fields: fields{
				ID:    10001,
				Extra: PipelineExtra{RunUser: nil},
			},
			want: "",
		},
		{
			name: "test with correct",
			fields: fields{
				ID:    10001,
				Extra: PipelineExtra{RunUser: &PipelineUser{
					ID:   1,
					Name: "erda",
				}},
			},
			want: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &PagePipeline{
				ID:    tt.fields.ID,
				Extra: tt.fields.Extra,
			}
			if got := p.GetRunUserID(); got != tt.want {
				t.Errorf("GetRunUserID() = %v, want %v", got, tt.want)
			}
		})
	}
}
