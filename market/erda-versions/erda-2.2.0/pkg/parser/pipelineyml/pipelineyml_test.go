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

package pipelineyml

import (
	"testing"
)

func TestPipelineYml_HasOnPushBranch(t *testing.T) {
	type fields struct {
		s *Spec
	}
	type args struct {
		branch string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			name: "test on push",
			fields: fields{
				s: &Spec{
					On: &TriggerConfig{
						Push: &PushTrigger{
							Branches: []string{"branch"},
						},
					},
				},
			},
			args: args{
				branch: "branch",
			},
			want: true,
		},
		{
			name: "test not on push",
			fields: fields{
				s: &Spec{
					On: &TriggerConfig{
						Push: &PushTrigger{
							Branches: []string{"notBranch"},
						},
					},
				},
			},
			args: args{
				branch: "branch",
			},
			want: false,
		},
		{
			name: "test empty",
			fields: fields{
				s: &Spec{},
			},
			args: args{},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pipelineYml := PipelineYml{
				s: tt.fields.s,
			}
			if got := pipelineYml.HasOnPushBranch(tt.args.branch); got != tt.want {
				t.Errorf("HasOnPushBranch() = %v, want %v", got, tt.want)
			}
		})
	}
}
