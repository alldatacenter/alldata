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

package edgepipeline_register

import (
	"net/http"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/coreos/etcd/clientv3"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func TestSourceWhiteList(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			AllowedSources: []string{"cdp-", "recommend-"},
		},
	}
	tests := []struct {
		name string
		src  apistructs.PipelineSource
		want bool
	}{
		{
			name: "cdp source",
			src:  "cdp-123",
			want: true,
		},
		{
			name: "default source",
			src:  "default",
			want: false,
		},
		{
			name: "dice source",
			src:  "dice",
			want: false,
		},
		{
			name: "valid source with prefix",
			src:  "recommend-123",
			want: true,
		},
		{
			name: "invalid source with prefix",
			src:  "invalid-123",
			want: false,
		},
	}
	patch := monkey.PatchInstanceMethod(reflect.TypeOf(p.bdl), "IsClusterManagerClientRegistered", func(_ *bundle.Bundle, _ apistructs.ClusterManagerClientType, _ string) (bool, error) {
		return true, nil
	})
	defer patch.Unpatch()
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := p.CanProxyToEdge(tt.src, "dev"); got != tt.want {
				t.Errorf("sourceWhiteList() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_parseDialerEndpoint(t *testing.T) {
	tests := []struct {
		name     string
		endpoint string
		want     string
		wantErr  bool
	}{
		{
			name:     "invalid endpoint",
			endpoint: "xxx",
			want:     "xxx",
			wantErr:  false,
		},
		{
			name:     "http endpoint",
			endpoint: "http://cluster-manager:9094",
			want:     "ws://cluster-manager:9094",
			wantErr:  false,
		},
		{
			name:     "https endpoint",
			endpoint: "https://cluster-manager:9094",
			want:     "wss://cluster-manager:9094",
			wantErr:  false,
		},
	}
	for _, tt := range tests {
		p := &provider{
			Cfg: &Config{
				IsEdge:                 true,
				ClusterManagerEndpoint: tt.endpoint,
			},
		}
		got, err := p.parseDialerEndpoint()
		if (err != nil) != tt.wantErr {
			t.Errorf("%q. provider.parseDialerEndpoint() error = %v, wantErr %v", tt.name, err, tt.wantErr)
			continue
		}
		if got != tt.want {
			t.Errorf("%q. provider.parseDialerEndpoint() = %v, want %v", tt.name, got, tt.want)
		}
	}
}

func TestGetAccessToken(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
		},
	}
	accessToken, err := p.GetAccessToken(apistructs.OAuth2TokenGetRequest{})
	assert.NoError(t, err)
	assert.Equal(t, p.Cfg.ClusterAccessKey, accessToken.AccessToken)
}

func TestGetOAuth2Token(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
		},
	}
	oauth2Token, err := p.GetOAuth2Token(apistructs.OAuth2TokenGetRequest{})
	assert.NoError(t, err)
	assert.Equal(t, p.Cfg.ClusterAccessKey, oauth2Token.AccessToken)
}

func TestCheckAccessToken(t *testing.T) {
	tests := []struct {
		name        string
		accessToken string
		wantErr     bool
	}{
		{
			name:        "valid access token",
			accessToken: "xxx",
			wantErr:     false,
		},
		{
			name:        "invalid access token",
			accessToken: "yyy",
			wantErr:     true,
		},
	}
	etcdClient := &clientv3.Client{
		KV: &MockKV{},
	}
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
		},
		EtcdClient: etcdClient,
		Log:        logrusx.New(),
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := p.CheckAccessToken(tt.accessToken)
			if (err != nil) != tt.wantErr {
				t.Errorf("provider.CheckAccessToken() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestGetEdgePipelineEnvs(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
			PipelineAddr:     "pipeline:3081",
			PipelineHost:     "pipeline.default.svc.cluster.local",
		},
	}
	envs := p.GetEdgePipelineEnvs()
	assert.Equal(t, "pipeline:3081", envs.Get(apistructs.ClusterManagerDataKeyPipelineAddr))
	assert.Equal(t, "pipeline.default.svc.cluster.local", envs.Get(apistructs.ClusterManagerDataKeyPipelineHost))
}

func TestCheckAccessTokenFromHttpRequest(t *testing.T) {
	etcdClient := &clientv3.Client{
		KV: &MockKV{},
	}
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
		},
		EtcdClient: etcdClient,
		Log:        logrusx.New(),
	}
	tests := []struct {
		name        string
		accessToken string
		wantErr     bool
	}{
		{
			name:        "valid access token",
			accessToken: "xxx",
			wantErr:     false,
		},
		{
			name:        "invalid access token",
			accessToken: "yyy",
			wantErr:     true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &http.Request{
				Header: http.Header{
					"Authorization": []string{tt.accessToken},
				},
			}
			err := p.CheckAccessTokenFromHttpRequest(r)
			if (err != nil) != tt.wantErr {
				t.Errorf("provider.CheckAccessTokenFromHttpRequest() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestIsEdge(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			IsEdge:           true,
			ClusterAccessKey: "xxx",
		},
	}
	assert.Equal(t, true, p.IsEdge())
}

func TestShouldDispatchToEdge(t *testing.T) {
	bdl := bundle.New()
	patch := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "IsClusterManagerClientRegistered", func(_ *bundle.Bundle, _ apistructs.ClusterManagerClientType, _ string) (bool, error) {
		return true, nil
	})
	defer patch.Unpatch()
	p := provider{
		bdl: bdl,
		Cfg: &Config{
			ClusterName:    "dev",
			AllowedSources: []string{"cdp-", "recommend-"},
		},
	}
	tests := []struct {
		name        string
		clusterName string
		wantEdge    bool
	}{
		{
			name:        "edge",
			clusterName: "edge",
			wantEdge:    true,
		},
		{
			name:        "center",
			clusterName: "dev",
			wantEdge:    false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := p.CanProxyToEdge("cdp-dev", tt.clusterName)
			if got != tt.wantEdge {
				t.Errorf("want edge: %v, but got: %v", tt.wantEdge, got)
			}
		})
	}
}

func Test_checkEtcdPrefixKey(t *testing.T) {
	tests := []struct {
		name    string
		key     string
		wantErr bool
	}{
		{
			name:    "empty key",
			key:     "",
			wantErr: true,
		},
		{
			name:    "end with /",
			key:     "/xxx/",
			wantErr: true,
		},
		{
			name:    "not start with /",
			key:     "xxx",
			wantErr: true,
		},
		{
			name:    "valid key",
			key:     "/devops/pipeline/cluster-key",
			wantErr: false,
		},
	}
	p := &provider{Cfg: &Config{}}
	for _, tt := range tests {
		p.Cfg.EtcdPrefixOfClusterAccessKey = tt.key
		if err := p.checkEtcdPrefixKey(p.Cfg.EtcdPrefixOfClusterAccessKey); (err != nil) != tt.wantErr {
			t.Errorf("want err: %v, but got: %v", tt.wantErr, err)
		}
	}
}
