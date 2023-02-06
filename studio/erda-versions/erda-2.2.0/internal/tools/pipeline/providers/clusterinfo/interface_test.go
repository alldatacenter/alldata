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

package clusterinfo

import (
	"fmt"
	"os"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
)

func TestGetClusterInfoByNameEdge(t *testing.T) {
	type args struct {
		name        string
		clusterName string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "get current cluster info",
			args: args{
				name: "dev",
			},
			wantErr: false,
		},
		{
			name: "get current cluster info failed",
			args: args{
				name:        "op",
				clusterName: "op",
			},
			wantErr: true,
		},
		{
			name: "get edge cluster info",
			args: args{
				name: "edge",
			},
			wantErr: false,
		},
	}
	bdl := bundle.New()
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetCluster", func(_ *bundle.Bundle, idOrName string) (*apistructs.ClusterInfo, error) {
		if idOrName == "edge" {
			return &apistructs.ClusterInfo{
				Name: "edge",
			}, nil
		}
		return nil, fmt.Errorf("not found for cluster: %s", idOrName)
	})
	defer pm1.Unpatch()
	p := provider{
		Cfg: &config{
			ClusterName: "dev",
			IsEdge:      true,
		},
		cache: NewClusterInfoCache(),
		bdl:   bdl,
	}
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(&p), "GetCurrentClusterInfoFromK8sConfigMap", func(_ *provider) (apistructs.ClusterInfo, error) {
		if p.Cfg.ClusterName == "op" {
			return apistructs.ClusterInfo{}, fmt.Errorf("not found for cluster: %s", p.Cfg.ClusterName)
		}
		return apistructs.ClusterInfo{
			Name: "dev",
		}, nil
	})
	defer pm2.Unpatch()
	for _, tt := range tests {
		if tt.args.clusterName != "" {
			p.Cfg.ClusterName = tt.args.clusterName
		}
		t.Run(tt.name, func(t *testing.T) {
			_, err := p.GetClusterInfoByName(tt.args.name)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetClusterInfoByName() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_batchUpdateClusterInfo(t *testing.T) {
	bdl := bundle.New()
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListClusters", func(_ *bundle.Bundle, clusterType string, orgID ...uint64) ([]apistructs.ClusterInfo, error) {
		return []apistructs.ClusterInfo{
			{
				Name: "dev",
			},
			{
				Name: "edge",
			},
		}, nil
	})
	defer pm1.Unpatch()
	p := provider{
		Cfg: &config{
			ClusterName: "dev",
		},
		cache: NewClusterInfoCache(),
		bdl:   bdl,
	}
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(&p), "GetCurrentClusterInfoFromK8sConfigMap", func(_ *provider) (apistructs.ClusterInfo, error) {
		return apistructs.ClusterInfo{
			Name: "dev",
		}, nil
	})
	defer pm2.Unpatch()
	t.Run("batch update cluster info", func(t *testing.T) {
		os.Setenv(string(apistructs.DICE_CLUSTER_NAME), "dev")
		p.batchUpdateClusterInfo()
	})
	devClusterInfo, err := p.GetClusterInfoByName("dev")
	assert.NoError(t, err)
	assert.Equal(t, "dev", devClusterInfo.Name)

	edgeClusterInfo, err := p.GetClusterInfoByName("edge")
	assert.NoError(t, err)
	assert.Equal(t, "edge", edgeClusterInfo.Name)
}

func Test_registerClusterHook(t *testing.T) {
	bdl := bundle.New()
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateWebhook", func(_ *bundle.Bundle, r apistructs.CreateHookRequest) error {
		return fmt.Errorf("create hook error")
	})
	defer pm1.Unpatch()
	p := provider{
		Cfg: &config{
			ClusterName: "dev",
		},
		cache: NewClusterInfoCache(),
		bdl:   bdl,
		Log:   logrusx.New(),
	}
	err := p.registerClusterHook()
	assert.Equal(t, true, err != nil)
}

type cacheImpl struct {
	cache map[string]string
}

func (c cacheImpl) GetClusterInfoByName(name string) (apistructs.ClusterInfo, bool) {
	panic("implement me")
}

func (c cacheImpl) UpdateClusterInfo(clusterInfo apistructs.ClusterInfo) {
	panic("implement me")
}

func (c cacheImpl) DeleteClusterInfo(name string) {
	panic("implement me")
}

func (c cacheImpl) GetAllClusters() []apistructs.ClusterInfo {
	var infos []apistructs.ClusterInfo
	for key := range c.cache {
		infos = append(infos, apistructs.ClusterInfo{
			Name: key,
		})
	}
	return infos
}

func Test_provider_ListAllClusterInfos(t *testing.T) {
	type args struct {
		onlyEdge bool
	}

	tests := []struct {
		name    string
		cache   map[string]string
		args    args
		want    []apistructs.ClusterInfo
		wantErr bool
	}{
		{
			name: "test all cluster",
			cache: map[string]string{
				"test":  "edge",
				"test1": "notEdge",
			},
			args: args{
				onlyEdge: false,
			},
			want: []apistructs.ClusterInfo{
				{
					Name: "test",
				},
				{
					Name: "test1",
				},
			},
			wantErr: false,
		},
		{
			name: "test edge cluster",
			cache: map[string]string{
				"test":  "edge",
				"test1": "notEdge",
			},
			args: args{
				onlyEdge: true,
			},
			want: []apistructs.ClusterInfo{
				{
					Name: "test",
				},
				{
					Name: "test1",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{}
			p.cache = cacheImpl{
				cache: tt.cache,
			}
			p.EdgeRegister = &edgepipeline_register.MockEdgeRegister{}

			got, err := p.listAllClusterInfos(tt.args.onlyEdge)
			if (err != nil) != tt.wantErr {
				t.Errorf("ListAllClusterInfos() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			for _, clu := range got {
				var find = false
				for _, wantClu := range tt.want {
					if clu.Name == wantClu.Name {
						find = true
						break
					}
				}
				assert.True(t, find)
			}
		})
	}
}
