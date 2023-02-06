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

package endpoints

import "testing"

func Test_getSourcePathAndName(t *testing.T) {
	type args struct {
		name string
	}
	tests := []struct {
		name         string
		args         args
		wantPath     string
		wantFileName string
	}{
		{
			name: "test pipeline.yml",
			args: args{
				name: "pipeline.yml",
			},
			wantFileName: "pipeline.yml",
			wantPath:     "",
		},
		{
			name: "test .erda/pipelines/push.yml",
			args: args{
				name: ".erda/pipelines/push.yml",
			},
			wantFileName: "push.yml",
			wantPath:     ".erda/pipelines",
		},
		{
			name: "test .dice/pipelines/push.yml",
			args: args{
				name: ".dice/pipelines/push.yml",
			},
			wantFileName: "push.yml",
			wantPath:     ".dice/pipelines",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotPath, gotFileName := getSourcePathAndName(tt.args.name)
			if gotPath != tt.wantPath {
				t.Errorf("getSourcePathAndName() gotPath = %v, want %v", gotPath, tt.wantPath)
			}
			if gotFileName != tt.wantFileName {
				t.Errorf("getSourcePathAndName() gotFileName = %v, want %v", gotFileName, tt.wantFileName)
			}
		})
	}
}
