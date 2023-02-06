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

package pipeline

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/gitflowutil"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
)

func TestGetBranch(t *testing.T) {
	ss := []struct {
		ref  string
		Want string
	}{
		{"", ""},
		{"refs/heads/", ""},
		{"refs/heads/master", "master"},
		{"refs/heads/feature/test", "feature/test"},
	}
	for _, v := range ss {
		assert.Equal(t, v.Want, getBranch(v.ref))
	}
}

func TestIsPipelineYmlPath(t *testing.T) {
	ss := []struct {
		path string
		want bool
	}{
		{"pipeline.yml", true},
		{".dice/pipelines/a.yml", true},
		{"", false},
		{"dice/pipeline.yml", false},
	}
	for _, v := range ss {
		assert.Equal(t, v.want, isPipelineYmlPath(v.path))
	}

}

func Test_getWorkspaceMainBranch(t *testing.T) {
	type args struct {
		workspace string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "invalid workspace",
			args: args{
				workspace: "xxx",
			},
			want: "",
		},
		{
			name: "dev",
			args: args{
				workspace: "dev",
			},
			want: "feature",
		},
		{
			name: "Dev",
			args: args{
				workspace: "Dev",
			},
			want: "feature",
		},
		{
			name: "test",
			args: args{
				workspace: "test",
			},
			want: "develop",
		},
		{
			name: "staging",
			args: args{
				workspace: "staging",
			},
			want: "release",
		},
		{
			name: "prOD",
			args: args{
				workspace: "prOD",
			},
			want: "master",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getWorkspaceMainBranch(tt.args.workspace); got != tt.want {
				t.Errorf("getWorkspaceMainBranch() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_makeAppDefaultCmsNs(t *testing.T) {
	type args struct {
		appID uint64
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "app 1",
			args: args{
				appID: 1,
			},
			want: "app-1-default",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makeAppDefaultCmsNs(tt.args.appID); got != tt.want {
				t.Errorf("makeAppDefaultCmsNs() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_makeAppWorkspaceCmsNs(t *testing.T) {
	type args struct {
		appID     uint64
		workspace string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "app 1 dev",
			args: args{
				appID:     1,
				workspace: gitflowutil.DevWorkspace,
			},
			want: "app-1-dev",
		},
		{
			name: "app 1 prod",
			args: args{
				appID:     1,
				workspace: gitflowutil.ProdWorkspace,
			},
			want: "app-1-prod",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makeAppWorkspaceCmsNs(tt.args.appID, tt.args.workspace); got != tt.want {
				t.Errorf("makeAppWorkspaceCmsNs() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_makeBranchWorkspaceLevelCmsNs(t *testing.T) {
	type args struct {
		appID     uint64
		workspace string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "invalid workspace",
			args: args{
				appID:     1,
				workspace: "xxx",
			},
			want: []string{cms.MakeAppDefaultSecretNamespace("1")},
		},
		{
			name: "staging",
			args: args{
				appID:     1,
				workspace: "STAGING",
			},
			want: []string{cms.MakeAppDefaultSecretNamespace("1"), cms.MakeAppBranchPrefixSecretNamespaceByBranchPrefix("1", "release")},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makeBranchWorkspaceLevelCmsNs(tt.args.appID, tt.args.workspace); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("makeBranchWorkspaceLevelCmsNs() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_makeAppWorkspaceLevelCmsNs(t *testing.T) {
	type args struct {
		appID     uint64
		workspace string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "invalid workspace",
			args: args{
				appID:     1,
				workspace: "xxx",
			},
			want: []string{"app-1-default", "app-1-xxx"},
		},
		{
			name: "staging",
			args: args{
				appID:     1,
				workspace: "staging",
			},
			want: []string{"app-1-default", "app-1-staging"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makeAppWorkspaceLevelCmsNs(tt.args.appID, tt.args.workspace); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("makeAppWorkspaceLevelCmsNs() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_setClusterName(t *testing.T) {
	var bdl *bundle.Bundle
	m1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, clusterName string) (apistructs.ClusterInfoData, error) {
		if clusterName == "erda-edge" {
			return apistructs.ClusterInfoData{apistructs.JOB_CLUSTER: "erda-center", apistructs.DICE_IS_EDGE: "true"}, nil
		}
		return apistructs.ClusterInfoData{apistructs.DICE_IS_EDGE: "false"}, nil
	})
	defer m1.Unpatch()
	pipelineSvc := New(WithBundle(bdl))
	pv := &apistructs.PipelineCreateRequestV2{}
	pipelineSvc.setClusterName("erda-edge", pv)
	assert.Equal(t, "erda-center", pv.ClusterName)
	pipelineSvc.setClusterName("erda-center", pv)
	assert.Equal(t, "erda-center", pv.ClusterName)
}
