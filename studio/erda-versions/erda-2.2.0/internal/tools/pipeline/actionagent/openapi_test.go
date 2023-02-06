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

package actionagent

import (
	"os"
	"sync"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func Test_getOpenAPIInfo(t *testing.T) {
	tests := []struct {
		name              string
		envIsEdgeCluster  string
		envIsEdgePipeline string
		envOpenapiAddr    string
		envPipelineAddr   string
		wantErr           bool
	}{
		{
			name:              "center missing openapi addr",
			envIsEdgeCluster:  "false",
			envIsEdgePipeline: "false",
			envOpenapiAddr:    "",
			wantErr:           true,
		},
		{
			name:              "center have openapi addr",
			envIsEdgeCluster:  "false",
			envIsEdgePipeline: "false",
			envOpenapiAddr:    "openapi:80",
			wantErr:           false,
		},
		{
			name:              "edge pipeline missing pipeline addr",
			envIsEdgeCluster:  "false",
			envIsEdgePipeline: "true",
			envOpenapiAddr:    "openapi:80",
			envPipelineAddr:   "",
			wantErr:           true,
		},
		{
			name:              "edge pipeline have pipeline addr",
			envIsEdgeCluster:  "false",
			envIsEdgePipeline: "true",
			envOpenapiAddr:    "openapi:80",
			envPipelineAddr:   "pipeline:3081",
			wantErr:           false,
		},
	}
	for _, tt := range tests {
		agent := &Agent{
			Errs: make([]error, 0),
		}
		os.Setenv(EnvDiceIsEdge, tt.envIsEdgeCluster)
		os.Setenv(apistructs.EnvIsEdgePipeline, tt.envIsEdgePipeline)
		os.Setenv(EnvDiceOpenapiAddr, tt.envOpenapiAddr)
		os.Setenv(apistructs.EnvPipelineAddr, tt.envPipelineAddr)
		getOpenAPILock = sync.Once{}
		agent.getOpenAPIInfo()
		if (len(agent.Errs) != 0) != tt.wantErr {
			t.Errorf("%q. Agent.getOpenAPIInfo() error = %v, wantErr %v", tt.name, agent.Errs, tt.wantErr)
		}
	}
}

func TestIsEdgePipeline(t *testing.T) {
	tests := []struct {
		name string
		env  string
		want bool
	}{
		{
			name: "edge pipeline",
			env:  "true",
			want: true,
		},
		{
			name: "non-edge pipeline",
			env:  "false",
			want: false,
		},
		{
			name: "invalid env",
			env:  "xxx",
			want: false,
		},
	}
	agent := &Agent{}
	for _, tt := range tests {
		os.Setenv(apistructs.EnvIsEdgePipeline, tt.env)
		agent.isEdgePipeline()
		if agent.EasyUse.IsEdgePipeline != tt.want {
			t.Errorf("%s: isEdgePipeline = %v, want %v", tt.name, agent.EasyUse.IsEdgePipeline, tt.want)
		}
	}
}
