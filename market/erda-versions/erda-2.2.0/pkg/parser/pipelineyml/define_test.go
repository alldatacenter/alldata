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

import "testing"

func TestCountEnabledActionNumByPipelineYml(t *testing.T) {
	type args struct {
		pipelineYmlStr string
	}
	tests := []struct {
		name    string
		args    args
		want    int64
		wantErr bool
	}{
		{
			name:    "test with empty",
			args:    args{pipelineYmlStr: ""},
			want:    0,
			wantErr: false,
		},
		{
			name:    "test with error",
			args:    args{pipelineYmlStr: "foo: bar"},
			want:    0,
			wantErr: true,
		},
		{
			name: "test with no disabled",
			args: args{
				pipelineYmlStr: `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆
          version: "1.0"
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600
  - stage:
      - custom-script:
          alias: custom-script
          version: "1.0"
          image: registry.erda.cloud/erda-actions/custom-script-action:1.0-20211216-b1d5635
          commands:
            - echo hello
`,
			},
			want:    2,
			wantErr: false,
		},
		{
			name: "test with one disabled",
			args: args{
				pipelineYmlStr: `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆
          version: "1.0"
          disable: true
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600
  - stage:
      - custom-script:
          alias: custom-script
          version: "1.0"
          image: registry.erda.cloud/erda-actions/custom-script-action:1.0-20211216-b1d5635
          commands:
            - echo hello
`,
			},
			want:    1,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := CountEnabledActionNumByPipelineYml(tt.args.pipelineYmlStr)
			if (err != nil) != tt.wantErr {
				t.Errorf("CountEnabledActionNumByPipelineYml() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("CountEnabledActionNumByPipelineYml() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetNameByPipelineYml(t *testing.T) {
	type args struct {
		pipelineYmlStr string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "test with empty",
			args: args{
				pipelineYmlStr: ``,
			},
			want:    "",
			wantErr: false,
		},
		{
			name: "test with error",
			args: args{
				pipelineYmlStr: `foo: bar`,
			},
			want:    "",
			wantErr: true,
		},
		{
			name: "test with name",
			args: args{
				pipelineYmlStr: `version: "1.1"
name: pipeline-deploy
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆
          version: "1.0"
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600
  - stage:
      - custom-script:
          alias: custom-script
          version: "1.0"
          image: registry.erda.cloud/erda-actions/custom-script-action:1.0-20211216-b1d5635
          commands:
            - echo hello
`,
			},
			want:    "pipeline-deploy",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetNameByPipelineYml(tt.args.pipelineYmlStr)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNameByPipelineYml() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetNameByPipelineYml() got = %v, want %v", got, tt.want)
			}
		})
	}
}
