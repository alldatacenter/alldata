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
	"context"
	"fmt"
	"net/http"

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type MockEdgeRegister struct{}

func (m *MockEdgeRegister) ClusterAccessKey() string { return "mock-access-key" }
func (m *MockEdgeRegister) GetAccessToken(req apistructs.OAuth2TokenGetRequest) (*apistructs.OAuth2Token, error) {
	return nil, nil
}
func (m *MockEdgeRegister) GetOAuth2Token(req apistructs.OAuth2TokenGetRequest) (*apistructs.OAuth2Token, error) {
	return nil, nil
}
func (m *MockEdgeRegister) GetEdgePipelineEnvs() apistructs.ClusterManagerClientDetail {
	return apistructs.ClusterManagerClientDetail{}
}
func (m *MockEdgeRegister) CheckAccessToken(token string) error                     { return nil }
func (m *MockEdgeRegister) CheckAccessTokenFromHttpRequest(req *http.Request) error { return nil }
func (m *MockEdgeRegister) IsEdge() bool                                            { return true }
func (m *MockEdgeRegister) IsCenter() bool                                          { return !m.IsEdge() }
func (m *MockEdgeRegister) CanProxyToEdge(source apistructs.PipelineSource, clusterName string) bool {
	return true
}
func (m *MockEdgeRegister) GetEdgeBundleByClusterName(clusterName string) (*bundle.Bundle, error) {
	return nil, nil
}
func (m *MockEdgeRegister) ClusterIsEdge(clusterName string) (bool, error) {
	return true, nil
}
func (m *MockEdgeRegister) OnEdge(f func(context.Context))                                {}
func (m *MockEdgeRegister) OnCenter(f func(context.Context))                              {}
func (m *MockEdgeRegister) CreateMessageEvent(event *apistructs.EventCreateRequest) error { return nil }
func (m *MockEdgeRegister) RegisterEventHandler(handler EventHandler)                     {}
func (m *MockEdgeRegister) ListAllClients() []apistructs.ClusterManagerClientDetail       { return nil }

type MockKV struct{}

func (o MockKV) Put(ctx context.Context, key, val string, opts ...clientv3.OpOption) (*clientv3.PutResponse, error) {
	return nil, nil
}
func (o MockKV) Get(ctx context.Context, key string, opts ...clientv3.OpOption) (*clientv3.GetResponse, error) {
	if key == "/xxx" {
		return nil, nil
	}
	return nil, fmt.Errorf("not found")
}
func (o MockKV) Delete(ctx context.Context, key string, opts ...clientv3.OpOption) (*clientv3.DeleteResponse, error) {
	panic("implement me")
}
func (o MockKV) Compact(ctx context.Context, rev int64, opts ...clientv3.CompactOption) (*clientv3.CompactResponse, error) {
	panic("implement me")
}
func (o MockKV) Do(ctx context.Context, op clientv3.Op) (clientv3.OpResponse, error) {
	panic("implement me")
}
func (o MockKV) Txn(ctx context.Context) clientv3.Txn {
	panic("implement me")
}
