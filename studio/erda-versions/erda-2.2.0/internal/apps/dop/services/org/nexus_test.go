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

package org

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func Test_needEnableNexusOrgGroupRepos(t *testing.T) {
	type args struct {
		org       *apistructs.OrgDTO
		nexusAddr string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "no nexus addr",
			args: args{
				nexusAddr: "",
			},
			want: false,
		},
		{
			name: "crossCluster but not publisher",
			args: args{
				org:       &apistructs.OrgDTO{EnableReleaseCrossCluster: true, PublisherID: 0},
				nexusAddr: "mock",
			},
			want: true,
		},
		{
			name: "is publisher but not crossCluster",
			args: args{
				org:       &apistructs.OrgDTO{EnableReleaseCrossCluster: false, PublisherID: 1},
				nexusAddr: "mock",
			},
			want: true,
		},
		{
			name: "crossCluster and is publisher",
			args: args{
				org:       &apistructs.OrgDTO{EnableReleaseCrossCluster: true, PublisherID: 1},
				nexusAddr: "mock",
			},
			want: true,
		},
		{
			name: "neither crossCluster nor publisher",
			args: args{
				org:       &apistructs.OrgDTO{EnableReleaseCrossCluster: false, PublisherID: 0},
				nexusAddr: "mock",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := needEnableNexusOrgGroupRepos(tt.args.nexusAddr, tt.args.org); got != tt.want {
				t.Errorf("needEnableNexusOrgGroupRepos() = %v, want %v", got, tt.want)
			}
		})
	}
}
