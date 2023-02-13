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

package openapiv1

import (
	"testing"
)

func Test_replaceOpenapiV1Path(t *testing.T) {
	type args struct {
		path string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "<>",
			args: args{
				path: "/api/projects/<projectID>",
			},
			want: "/api/projects/{projectID}",
		},
		{
			name: "<> path variable with space",
			args: args{
				path: "/api/cloud-mysql/<id or name>",
			},
			want: "/api/cloud-mysql/{id_or_name}",
		},
		{
			name: "normal",
			args: args{
				path: "/api/projects",
			},
			want: "/api/projects",
		},
		{
			name: "*",
			args: args{
				path: "/api/repo/<*>",
			},
			want: "/api/repo/**",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := replaceOpenapiV1Path(tt.args.path); got != tt.want {
				t.Errorf("replaceOpenapiV1Path() = %v, want %v", got, tt.want)
			}
		})
	}
}
