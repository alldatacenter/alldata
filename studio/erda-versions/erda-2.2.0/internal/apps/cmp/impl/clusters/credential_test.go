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

package clusters

import (
	"context"
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"k8s.io/client-go/kubernetes"
	fakeclientset "k8s.io/client-go/kubernetes/fake"
	"k8s.io/client-go/rest"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/k8sclient"
)

var (
	bdl         *bundle.Bundle
	db          *dbclient.DBClient
	fakeCluster = "fake-cluster"
	fakeAkItem  = &tokenpb.Token{
		Id:        "5e34b95b-cd06-464c-8ee9-3aef696586c6",
		AccessKey: "Q9x5k4MJ89h327yqoc9zvvoP",
		Scope:     "cmp_cluster",
		ScopeId:   fakeCluster,
	}
)

type fakeClusterServiceServer struct {
	clusterpb.ClusterServiceServer
}

func (f *fakeClusterServiceServer) GetCluster(context.Context, *clusterpb.GetClusterRequest) (*clusterpb.GetClusterResponse, error) {
	return &clusterpb.GetClusterResponse{Data: &clusterpb.ClusterInfo{
		Name: "testCluster",
	}}, nil
}

func (f *fakeClusterServiceServer) ListCluster(context.Context, *clusterpb.ListClusterRequest) (*clusterpb.ListClusterResponse, error) {
	return &clusterpb.ListClusterResponse{Data: []*clusterpb.ClusterInfo{
		{
			Name: "testCluster",
		},
	}}, nil
}

func (f *fakeClusterServiceServer) DeleteCluster(context.Context, *clusterpb.DeleteClusterRequest) (*clusterpb.DeleteClusterResponse, error) {
	return &clusterpb.DeleteClusterResponse{}, nil
}

func (f *fakeClusterServiceServer) UpdateCluster(context.Context, *clusterpb.UpdateClusterRequest) (*clusterpb.UpdateClusterResponse, error) {
	return &clusterpb.UpdateClusterResponse{}, nil
}

func (f *fakeClusterServiceServer) CreateCluster(context.Context, *clusterpb.CreateClusterRequest) (*clusterpb.CreateClusterResponse, error) {
	return &clusterpb.CreateClusterResponse{}, nil
}

func getMockTokenServiceServer(ctrl *gomock.Controller) *MockTokenServiceServer {
	akService := NewMockTokenServiceServer(ctrl)

	akService.EXPECT().QueryTokens(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.QueryTokensResponse{
		Data: []*tokenpb.Token{},
	}, nil)

	akService.EXPECT().CreateToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.CreateTokenResponse{
		Data: fakeAkItem,
	}, nil)
	return akService
}

////go:generate mockgen -destination=./credential_ak_test.go -package clusters github.com/erda-project/erda-proto-go/core/services/authentication/credentials/accesskey/pb AccessKeyServiceServer
func Test_GetOrCreateAccessKey_Create(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	akService := getMockTokenServiceServer(ctrl)
	c := New(db, bdl, akService, &fakeClusterServiceServer{})

	monkey.PatchInstanceMethod(reflect.TypeOf(c), "CheckCluster", func(_ *Clusters, _ context.Context, _ string) error {
		return nil
	})

	defer monkey.UnpatchAll()

	akResp, err := c.GetOrCreateAccessKey(context.Background(), fakeCluster)
	assert.NoError(t, err)
	assert.Equal(t, akResp, fakeAkItem)
}

func Test_GetOrCreateAccessKey_Get(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)

	akService.EXPECT().QueryTokens(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.QueryTokensResponse{
		Data:  []*tokenpb.Token{fakeAkItem},
		Total: 1,
	}, nil)

	akService.EXPECT().CreateToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.CreateTokenResponse{
		Data: fakeAkItem,
	}, nil)

	c := New(db, bdl, akService, &fakeClusterServiceServer{})
	akResp, err := c.GetOrCreateAccessKey(context.Background(), fakeCluster)
	assert.NoError(t, err)
	assert.Equal(t, akResp, fakeAkItem)
}

func Test_DeleteAccessKey(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)

	akService.EXPECT().DeleteToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.DeleteTokenResponse{}, nil)

	akService.EXPECT().QueryTokens(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.QueryTokensResponse{
		Data:  []*tokenpb.Token{fakeAkItem},
		Total: 1,
	}, nil)

	c := New(db, bdl, akService, &fakeClusterServiceServer{})
	err := c.DeleteAccessKey(fakeCluster)
	assert.NoError(t, err)
}

func Test_ResetAccessKey_InCluster_Error(t *testing.T) {
	var (
		csErr            = errors.New("unit test, skip")
		emptyClusterName = ""
	)

	defer monkey.UnpatchAll()

	monkey.Patch(rest.InClusterConfig, func() (*rest.Config, error) {
		return nil, nil
	})

	monkey.Patch(kubernetes.NewForConfig, func(c *rest.Config) (*kubernetes.Clientset, error) {
		return nil, csErr
	})

	monkey.Patch(k8sclient.NewWithTimeOut, func(clusterName string, timeout time.Duration) (*k8sclient.K8sClient, error) {
		return nil, csErr
	})

	monkey.Patch(k8sclient.NewForInCluster, func(ops ...k8sclient.Option) (*k8sclient.K8sClient, error) {
		return nil, csErr
	})

	c := New(db, bdl, nil, &fakeClusterServiceServer{})
	_, err := c.ResetAccessKey(context.Background(), emptyClusterName)
	assert.Equal(t, err, fmt.Errorf("get inCluster kubernetes client error: %s", csErr.Error()))
}

func Test_ResetAccessKeyWithClientSet(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	akService := getMockTokenServiceServer(ctrl)
	c := New(db, bdl, akService, &fakeClusterServiceServer{})

	_, err := c.ResetAccessKeyWithClientSet(context.Background(), fakeCluster, fakeclientset.NewSimpleClientset())
	assert.NoError(t, err)
}
