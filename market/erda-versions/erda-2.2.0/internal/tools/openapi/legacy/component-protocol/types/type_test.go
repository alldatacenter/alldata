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

package types

import (
	"testing"
)

func TestComponentProtocolConfigs_ScenarioNeedProxy(t *testing.T) {
	type fields struct {
		ScenarioProxyBinds []ProxyConfig
	}
	type args struct {
		scenario string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			name: "proxy issue-manage to dop",
			fields: fields{
				ScenarioProxyBinds: []ProxyConfig{
					{
						App:       "dop",
						Scenarios: []string{"issue-manage"},
					},
				},
			},
			args: args{
				scenario: "issue-manage",
			},
			want: true,
		},
		{
			name: "issue-manage-x not found in binds",
			fields: fields{
				ScenarioProxyBinds: []ProxyConfig{
					{
						App:       "dop",
						Scenarios: []string{"issue-manage"},
					},
				},
			},
			args: args{
				scenario: "issue-manage-x",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cfg := ComponentProtocolConfigs{
				ScenarioProxyBinds: tt.fields.ScenarioProxyBinds,
			}
			got, _ := cfg.ScenarioNeedProxy(tt.args.scenario)
			if got != tt.want {
				t.Errorf("ScenarioNeedProxy() got = %v, want %v", got, tt.want)
			}
		})
	}
}
