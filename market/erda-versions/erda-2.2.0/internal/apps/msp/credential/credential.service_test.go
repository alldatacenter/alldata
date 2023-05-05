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
package credential

import (
	"context"
	"fmt"
	http1 "net/http"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"google.golang.org/protobuf/types/known/timestamppb"

	grpc1 "github.com/erda-project/erda-infra/pkg/transport/grpc"
	"github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda-proto-go/msp/credential/pb"
	tenant "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

////go:generate mockgen -destination=./credential_register_test.go -package exporter github.com/erda-project/erda-infra/pkg/transport Register
////go:generate mockgen -destination=./credential_ak_test.go -package exporter github.com/erda-project/erda-proto-go/core/services/authentication/credentials/accesskey/pb AccessKeyServiceServer
////go:generate mockgen -destination=./credential_context_test.go -package exporter github.com/erda-project/erda-infra/base/servicehub Context
////go:generate mockgen -destination=./tenant_test.go -package exporter github.com/erda-project/erda-proto-go/msp/tenant/pb TenantServiceServer

func Test_accessKeyService_QueryAccessKeys(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)
	tenantService := NewMockTenantServiceServer(ctrl)

	akService.EXPECT().CreateToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.CreateTokenResponse{
		Data: &tokenpb.Token{},
	}, nil)
	tenantService.EXPECT().GetTenantProject(gomock.Any(), gomock.Any()).AnyTimes().Return(&tenant.GetTenantProjectResponse{
		Data: &tenant.TenantProjectData{
			Workspace: "PROD",
			ProjectId: "98",
		},
	}, nil)
	defer monkey.UnpatchAll()
	monkey.Patch((*bundle.Bundle).GetProject, func(bdl *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
		return &apistructs.ProjectDTO{
			ID:                   89,
			Name:                 "ss",
			DisplayName:          "ss",
			DDHook:               "ss",
			OrgID:                21,
			Creator:              "ss",
			Logo:                 "ss",
			Desc:                 "ss",
			Owners:               nil,
			ActiveTime:           "ss",
			Joined:               false,
			CanUnblock:           nil,
			BlockStatus:          "ss",
			CanManage:            false,
			IsPublic:             false,
			Stats:                apistructs.ProjectStats{},
			ProjectResourceUsage: apistructs.ProjectResourceUsage{},
			ClusterConfig:        nil,
			ResourceConfig:       nil,
			RollbackConfig:       nil,
			CpuQuota:             0,
			MemQuota:             0,
			CreatedAt:            time.Time{},
			UpdatedAt:            time.Time{},
			Type:                 "",
		}, nil
	})

	pro := &provider{
		Cfg:                  &config{},
		Register:             NewMockRegister(ctrl),
		credentialKeyService: &accessKeyService{},
		TokenService:         akService,
		bdl:                  &bundle.Bundle{},
		audit:                nil,
		Tenant:               tenantService,
	}
	pro.credentialKeyService.p = pro
	_, err := pro.credentialKeyService.CreateAccessKey(context.Background(), &pb.CreateAccessKeyRequest{
		SubjectType: pb.SubjectTypeEnum_SYSTEM,
		Subject:     "xddd",
		Description: "cdddd",
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_accessKeyService_DeleteAccessKey(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	tenantService := NewMockTenantServiceServer(ctrl)
	akService := NewMockTokenServiceServer(ctrl)
	monkey.Patch((*bundle.Bundle).GetProject, func(bdl *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
		return &apistructs.ProjectDTO{
			ID:                   89,
			Name:                 "ss",
			DisplayName:          "ss",
			DDHook:               "ss",
			OrgID:                21,
			Creator:              "ss",
			Logo:                 "ss",
			Desc:                 "ss",
			Owners:               nil,
			ActiveTime:           "ss",
			Joined:               false,
			CanUnblock:           nil,
			BlockStatus:          "ss",
			CanManage:            false,
			IsPublic:             false,
			Stats:                apistructs.ProjectStats{},
			ProjectResourceUsage: apistructs.ProjectResourceUsage{},
			ClusterConfig:        nil,
			ResourceConfig:       nil,
			RollbackConfig:       nil,
			CpuQuota:             0,
			MemQuota:             0,
			CreatedAt:            time.Time{},
			UpdatedAt:            time.Time{},
			Type:                 "",
		}, nil
	})
	defer monkey.UnpatchAll()
	akService.EXPECT().GetToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.GetTokenResponse{
		Data: &tokenpb.Token{
			Id:          "2",
			AccessKey:   "sfdfgfg",
			SecretKey:   "sdgds",
			Description: "ss",
			CreatedAt:   nil,
			Scope:       "ss",
			ScopeId:     "dfdfd",
			CreatorId:   "2",
		},
	}, nil)
	tenantService.EXPECT().GetTenantProject(gomock.Any(), gomock.Any()).AnyTimes().Return(&tenant.GetTenantProjectResponse{
		Data: &tenant.TenantProjectData{
			Workspace: "PROD",
			ProjectId: "98",
		},
	}, nil)
	akService.EXPECT().DeleteToken(gomock.Any(), gomock.Any()).AnyTimes().Return(nil, nil)
	pro := &provider{
		Cfg:                  &config{},
		Register:             NewMockRegister(ctrl),
		credentialKeyService: &accessKeyService{},
		TokenService:         akService,
		bdl:                  &bundle.Bundle{},
		audit:                nil,
		Tenant:               tenantService,
	}
	pro.credentialKeyService.p = pro
	_, err := pro.credentialKeyService.DeleteAccessKey(context.Background(), &pb.DeleteAccessKeyRequest{
		Id: "13eef468-1d0b-42ce-aa7b-b499545bf92d",
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_accessKeyService_QueryAccessKeys1(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)
	akService.EXPECT().QueryTokens(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.QueryTokensResponse{
		Data: make([]*tokenpb.Token, 0),
	}, nil)
	pro := &provider{
		Cfg:                  &config{},
		Register:             NewMockRegister(ctrl),
		credentialKeyService: &accessKeyService{},
		TokenService:         akService,
	}
	pro.credentialKeyService.p = pro
	_, err := pro.credentialKeyService.QueryAccessKeys(context.Background(), &pb.QueryAccessKeysRequest{
		SubjectType: 3,
		Subject:     "22",
		PageSize:    1,
		PageNo:      3,
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_accessKeyService_GetAccessKey(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)
	akService.EXPECT().GetToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.GetTokenResponse{
		Data: &tokenpb.Token{},
	}, nil)
	pro := &provider{
		Cfg:                  &config{},
		Register:             NewMockRegister(ctrl),
		credentialKeyService: &accessKeyService{},
		TokenService:         akService,
	}
	pro.credentialKeyService.p = pro
	_, err := pro.credentialKeyService.GetAccessKey(context.Background(), &pb.GetAccessKeyRequest{
		Id: "13eef468-1d0b-42ce-aa7b-b499545bf92d",
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_accessKeyService_DownloadAccessKeyFile(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)
	akService.EXPECT().GetToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.GetTokenResponse{
		Data: &tokenpb.Token{
			Id:          "ssss",
			AccessKey:   "dddd",
			SecretKey:   "dddd",
			Description: "aaa",
			CreatedAt:   &timestamppb.Timestamp{},
		},
	}, nil)
	pro := &provider{
		Cfg:                  &config{},
		Register:             NewMockRegister(ctrl),
		credentialKeyService: &accessKeyService{},
		TokenService:         akService,
	}
	pro.credentialKeyService.p = pro
	_, err := pro.credentialKeyService.DownloadAccessKeyFile(context.Background(), &pb.DownloadAccessKeyFileRequest{
		Id: "ssss",
	})
	if err != nil {
		fmt.Println("should not err")
	}
}

func Test_Init(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	akService := NewMockTokenServiceServer(ctrl)
	pro := &provider{
		Cfg:                  &config{},
		Register:             NewMockRegister(ctrl),
		credentialKeyService: &accessKeyService{},
		TokenService:         akService,
		bdl:                  &bundle.Bundle{},
		audit:                nil,
		Tenant:               nil,
	}
	pro.credentialKeyService.p = pro
	defer monkey.UnpatchAll()
	monkey.Patch(encoding.EncodeResponse, func(w http1.ResponseWriter, r *http1.Request, out interface{}) error {
		return nil
	})
	monkey.Patch(pb.RegisterAccessKeyServiceHandler, func(r http.Router, srv pb.AccessKeyServiceHandler, opts ...http.HandleOption) {})
	monkey.Patch(pb.RegisterAccessKeyServiceServer, func(s grpc1.ServiceRegistrar, srv pb.AccessKeyServiceServer, opts ...grpc1.HandleOption) {})
	akService.EXPECT().GetToken(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.GetTokenResponse{
		Data: &tokenpb.Token{
			Id:          "ssss",
			AccessKey:   "dddd",
			SecretKey:   "dddd",
			Description: "aaa",
			CreatedAt:   &timestamppb.Timestamp{},
		},
	}, nil)
	pro.credentialKeyService.p = pro
	_, err := pro.credentialKeyService.DownloadAccessKeyFile(context.Background(), &pb.DownloadAccessKeyFileRequest{
		Id: "ssss",
	})
	if err != nil {
		fmt.Println("should not err2")
	}
}
