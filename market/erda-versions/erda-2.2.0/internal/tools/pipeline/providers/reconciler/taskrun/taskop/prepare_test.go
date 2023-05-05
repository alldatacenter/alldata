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

package taskop

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func Test_prepare_generateOpenapiTokenForPullBootstrapInfo(t *testing.T) {
	type args struct {
		task *spec.PipelineTask
	}
	tests := []struct {
		name    string
		pre     prepare
		args    args
		wantErr bool
	}{
		{
			name: "test_api-test",
			pre:  prepare{},
			args: args{
				task: &spec.PipelineTask{
					Type: apistructs.ActionTypeAPITest,
				},
			},
			wantErr: false,
		},
		{
			name: "test_wait",
			pre:  prepare{},
			args: args{
				task: &spec.PipelineTask{
					Type: apistructs.ActionTypeWait,
				},
			},
			wantErr: false,
		},
		{
			name: "test_snippet",
			pre:  prepare{},
			args: args{
				task: &spec.PipelineTask{
					Type: apistructs.ActionTypeSnippet,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := tt.pre.generateOpenapiTokenForPullBootstrapInfo(tt.args.task); (err != nil) != tt.wantErr {
				t.Errorf("generateOpenapiTokenForPullBootstrapInfo() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_condition(t *testing.T) {
	task := &spec.PipelineTask{Extra: spec.PipelineTaskExtra{Action: pipelineyml.Action{If: "${{   1 == 1   }}"}}}
	b := condition(task)
	assert.Equal(t, false, b)
}

func Test_existContinuePrivateEnv(t *testing.T) {
	tests := []struct {
		name      string
		key       string
		envs      map[string]string
		wantExist bool
	}{
		{
			name: "application name",
			key:  apistructs.DiceApplicationName,
			envs: map[string]string{
				apistructs.DiceApplicationName: "test",
			},
			wantExist: true,
		},
		{
			name: "application id",
			key:  apistructs.DiceApplicationId,
			envs: map[string]string{
				apistructs.DiceApplicationId: "1",
			},
			wantExist: true,
		},
		{
			name: "gittar branch",
			key:  apistructs.GittarBranchEnv,
			envs: map[string]string{
				apistructs.GittarBranchEnv: "develop",
			},
			wantExist: true,
		},
		{
			name: "no exist",
			key:  apistructs.SourceDeployPipeline,
			envs: map[string]string{
				apistructs.SourceDeployPipeline: "cdp",
			},
			wantExist: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotExist := existContinuePrivateEnv(tt.envs, tt.key); gotExist != tt.wantExist {
				t.Fatalf("existContinuePrivateEnv() = %v, want %v", gotExist, tt.wantExist)
			}
		})
	}
}

func Test_handleAccessTokenExpiredIn(t *testing.T) {
	tests := []struct {
		name    string
		timeout time.Duration
		expect  string
	}{
		{
			name:    "no timeout",
			timeout: -1,
			expect:  "0",
		},
		{
			name:    "defualt timeout",
			timeout: 0,
			expect:  "30s",
		},
		{
			name:    "half hour",
			timeout: time.Minute * 30,
			expect:  "1830s",
		},
	}
	task := &spec.PipelineTask{
		Extra: spec.PipelineTaskExtra{},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			task.Extra.Timeout = tt.timeout
			got := handleAccessTokenExpiredIn(task)
			if got != tt.expect {
				t.Fatalf("expect timeout: %s, but got: %s", tt.expect, got)
			}
		})
	}
}

func Test_handleInternalClient(t *testing.T) {
	p := &spec.Pipeline{
		PipelineExtra: spec.PipelineExtra{
			Extra: spec.PipelineExtraInfo{
				InternalClient: "bundle",
			},
		},
	}
	client := handleInternalClient(p)
	assert.Equal(t, "bundle", client)
	p.Extra.InternalClient = ""
	client = handleInternalClient(p)
	assert.Equal(t, "pipeline-signed-openapi-token", client)
}

func Test_generateTaskCMDs(t *testing.T) {
	cmd, args, err := generateTaskCMDs(&pipelineyml.Action{}, spec.PipelineTaskContext{}, 1, 1)
	assert.NoError(t, err)
	assert.Equal(t, "agent", cmd)
	assert.Equal(t, "eyJwdWxsQm9vdHN0cmFwSW5mbyI6dHJ1ZSwiY29udGV4dCI6e30sInBpcGVsaW5lSUQiOjEsInBpcGVsaW5lVGFza0lEIjoxLCJlbmNyeXB0U2VjcmV0S2V5cyI6bnVsbH0=", args[0])
}

func Test_getActionAgentTypeVersion(t *testing.T) {
	version := getActionAgentTypeVersion()
	assert.Equal(t, "agent@1.0", version)
}

func Test_contextVolumes(t *testing.T) {
	taskContext := spec.PipelineTaskContext{
		InStorages:  metadata.Metadata{{Name: "in1"}},
		OutStorages: metadata.Metadata{{Name: "out1"}},
	}
	fields := contextVolumes(taskContext)
	assert.Equal(t, 2, len(fields))
}
