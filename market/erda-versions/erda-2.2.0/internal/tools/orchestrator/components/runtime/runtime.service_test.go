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

package runtime

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/orchestrator/runtime/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/components/runtime/mock"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

////go:generate mockgen -destination=./mock/mock_sg.go -package mock github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup ServiceGroup

type fakeClusterServiceServer struct {
	clusterpb.ClusterServiceServer
}

func (f *fakeClusterServiceServer) GetCluster(context.Context, *clusterpb.GetClusterRequest) (*clusterpb.GetClusterResponse, error) {
	return &clusterpb.GetClusterResponse{Data: &clusterpb.ClusterInfo{
		Name: "testCluster",
	}}, nil
}

func TestService_GetRuntime(t *testing.T) {
	assert := require.New(t)
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	bdlSvc := mock.NewMockBundleService(ctrl)
	dbSvc := mock.NewMockDBService(ctrl)
	sgiSvc := mock.NewMockServiceGroup(ctrl)
	svc := NewRuntimeService(WithBundleService(bdlSvc), WithDBService(dbSvc), WithServiceGroupImpl(sgiSvc), WithClusterSvc(&fakeClusterServiceServer{}))

	md := metadata.New(map[string]string{
		"user-id": "2",
		"org-id":  "4",
	})
	resp, err := svc.GetRuntime(context.Background(), &pb.GetRuntimeRequest{})
	// not login
	assert.NotNil(err)
	ctx := metadata.NewIncomingContext(context.Background(), md)
	// invalid param
	resp, err = svc.GetRuntime(ctx, &pb.GetRuntimeRequest{})
	assert.NotNil(err)

	runtime := &dbclient.Runtime{BaseModel: dbengine.BaseModel{
		ID: 1,
	},
		ClusterName:   "foo",
		ApplicationID: 1,
		ScheduleName: dbclient.ScheduleName{
			Namespace: "ns",
			Name:      "n",
		},
	}

	dbSvc.
		EXPECT().
		GetRuntimeAllowNil(gomock.Eq(uint64(1))).
		Return(runtime, nil).MinTimes(1)

	bdlSvc.
		EXPECT().
		CheckPermission(gomock.Any()).
		Return(&apistructs.PermissionCheckResponseData{Access: true}, nil).MinTimes(1)

	dbSvc.
		EXPECT().
		FindLastDeployment(gomock.Eq(uint64(1))).
		Return(&dbclient.Deployment{
			RuntimeId: uint64(10),
			Status:    apistructs.DeploymentStatusDeploying,
			Dice: `{
"services": {
  "sa": {
    "image": "latest",
    "expose": [1],
    "ports": [{"port": 1, "expose": true}]
  }
}
}`,
		}, nil).MinTimes(1)

	dbSvc.
		EXPECT().
		FindDomainsByRuntimeId(gomock.Eq(uint64(1))).
		Return([]dbclient.RuntimeDomain{
			{
				Domain: "foo.com",
			},
		}, nil).MinTimes(1)

	//bdlSvc.
	//	EXPECT().
	//	GetCluster(gomock.Eq("foo")).
	//	Return(&apistructs.ClusterInfo{
	//		Name: "foo",
	//	}, nil).MinTimes(1)
	//
	bdlSvc.
		EXPECT().
		GetApp(gomock.Eq(uint64(1))).
		Return(&apistructs.ApplicationDTO{
			Name: "foo",
		}, nil).MinTimes(1)

	sgiSvc.
		EXPECT().
		InspectServiceGroupWithTimeout(gomock.Eq("ns"), gomock.Eq("n")).
		Return(&apistructs.ServiceGroup{
			StatusDesc: apistructs.StatusDesc{
				Status: apistructs.StatusFailed,
			},
			Dice: apistructs.Dice{
				Services: []apistructs.Service{
					{
						Name: "sa",
						Ports: []diceyml.ServicePort{
							{
								Port:   1,
								Expose: true,
							},
						},
						Vip: "vip",
					},
				},
			},
		}, nil).
		MinTimes(1)

	resp, err = svc.GetRuntime(ctx, &pb.GetRuntimeRequest{
		NameOrID:  "1",
		AppID:     "2",
		Workspace: "dev",
	})

	assert.Nil(err)
	assert.NotNil(resp)

	dbSvc.
		EXPECT().
		FindRuntime(gomock.Any()).
		Return(runtime, nil).MinTimes(1)

	resp, err = svc.GetRuntime(ctx, &pb.GetRuntimeRequest{
		NameOrID:  "name",
		AppID:     "2",
		Workspace: "dev",
	})
	assert.Nil(err)
	assert.NotNil(resp)
}

func newMatcher(matches func(interface{}) bool) *matcher {
	return &matcher{m: matches}
}

type matcher struct {
	m func(interface{}) bool
}

func (r *matcher) Matches(i interface{}) bool {
	return r.m(i)
}

func (*matcher) String() string {
	return ""
}

func Test_DeleteRuntime(t *testing.T) {
	assert := require.New(t)
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	bdlSvc := mock.NewMockBundleService(ctrl)
	dbSvc := mock.NewMockDBService(ctrl)
	evMgr := mock.NewMockEventManagerService(ctrl)
	svc := NewRuntimeService(WithBundleService(bdlSvc), WithDBService(dbSvc), WithEventManagerService(evMgr))

	md := metadata.New(map[string]string{
		"user-id": "2",
		"org-id":  "4",
	})
	_, err := svc.DelRuntime(context.Background(), &pb.DelRuntimeRequest{Id: "20"})
	assert.NotNil(err)

	dbSvc.EXPECT().GetRuntime(gomock.Eq(uint64(20))).Return(
		&dbclient.Runtime{
			ApplicationID: 1,
		}, nil,
	).Times(1)
	bdlSvc.EXPECT().GetApp(gomock.Eq(uint64(1))).Return(
		&apistructs.ApplicationDTO{}, nil,
	).Times(1)
	bdlSvc.EXPECT().CheckPermission(gomock.Any()).Return(
		&apistructs.PermissionCheckResponseData{
			Access: true,
		}, nil,
	).Times(1)
	dbSvc.EXPECT().UpdateRuntime(
		newMatcher(func(i interface{}) bool {
			if runtime, ok := i.(*dbclient.Runtime); !ok || runtime.LegacyStatus != dbclient.LegacyStatusDeleting {
				return false
			}
			return true
		}),
	).Return(nil).Times(1)
	evMgr.EXPECT().EmitEvent(newMatcher(func(i interface{}) bool {
		if event, ok := i.(*events.RuntimeEvent); !ok || event.EventName != events.RuntimeDeleting {
			return false
		}
		return true
	})).Return().Times(1)

	ctx := metadata.NewIncomingContext(context.Background(), md)
	r, err := svc.DelRuntime(ctx, &pb.DelRuntimeRequest{Id: "20"})
	assert.Nil(err)
	assert.NotNil(r)
}
