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

package secret

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func Test_addRegistryLabel(t *testing.T) {
	type args struct {
		r           map[string]string
		clusterInfo apistructs.ClusterInfoData
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		{
			args: args{
				r: map[string]string{
					"dice.openapi.addr": "12313",
				},
				clusterInfo: apistructs.ClusterInfoData{
					"REGISTRY_USERNAME": "xxxxxxxxx",
					"REGISTRY_PASSWORD": "yyyyyyyyy",
					"REGISTRY_ADDR":     "zzzzzzzzz",
				},
			},
			want: map[string]string{
				"dice.openapi.addr":                    "12313",
				"bp.docker.artifact.registry.username": "xxxxxxxxx",
				"bp.docker.artifact.registry.password": "yyyyyyyyy",
				"bp.docker.artifact.registry":          "zzzzzzzzz",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := addRegistryLabel(tt.args.r, tt.args.clusterInfo)
			flag := true
			for k, v := range got {
				wantVal, ok := tt.want[k]
				if !ok {
					flag = false
					break
				}
				if wantVal != v {

					flag = false
					break
				}
			}
			if !flag {
				t.Errorf("addRegistryLabel() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_replaceProjectApplication(t *testing.T) {
	type args struct {
		r map[string]string
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		// TODO: Add test cases.
		{
			args: args{
				r: map[string]string{
					"bp.docker.artifact.registry": "addon-registry.default.svc.cluster.local:5000",
					"dice.project.application":    "project/app",
				},
			},
			want: map[string]string{
				"bp.docker.artifact.registry": "addon-registry.default.svc.cluster.local:5000",
				"dice.project.application":    "project/app",
			},
		},
		{
			args: args{
				r: map[string]string{
					"bp.docker.artifact.registry": "addon-registry.default.svc.cluster.local:500/123/123",
					"dice.project.application":    "project/app",
				},
			},
			want: map[string]string{
				"bp.docker.artifact.registry": "addon-registry.default.svc.cluster.local:500/123/123",
				"dice.project.application":    "project-app",
			},
		},
		{
			args: args{
				r: map[string]string{
					"bp.docker.artifact.registry": "addon-registry.default.svc.cluster.local:500/registry",
					"dice.project.application":    "project/app",
				},
			},
			want: map[string]string{
				"bp.docker.artifact.registry": "addon-registry.default.svc.cluster.local:500/registry",
				"dice.project.application":    "project-app",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := replaceProjectApplication(tt.args.r); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("replaceProjectApplication() = %v, want %v", got, tt.want)
			}
		})
	}
}
