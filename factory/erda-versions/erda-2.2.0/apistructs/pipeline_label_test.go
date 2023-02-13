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

func TestPipelineLabelType_String(t *testing.T) {
	tests := []struct {
		name string
		t    PipelineLabelType
		want string
	}{
		{
			name: "queue",
			t:    PipelineLabelTypeQueue,
			want: "queue",
		},
		{
			name: "instance",
			t:    PipelineLabelTypeInstance,
			want: "p_i",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.t.String(); got != tt.want {
				t.Errorf("String() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestPipelineLabelType_Valid(t *testing.T) {
	tests := []struct {
		name string
		t    PipelineLabelType
		want bool
	}{
		{
			name: "queue",
			t:    PipelineLabelTypeQueue,
			want: true,
		},
		{
			name: "instance",
			t:    PipelineLabelTypeInstance,
			want: true,
		},
		{
			name: "invalid",
			t:    "invalid",
			want: false,
		},
		{
			name: "string_queue",
			t:    "queue",
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.t.Valid(); got != tt.want {
				t.Errorf("Valid() = %v, want %v", got, tt.want)
			}
		})
	}
}
