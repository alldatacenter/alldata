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

package global

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/global/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type globalService struct {
	p *provider
}

func (s *globalService) GetHealth(ctx context.Context, req *pb.GetHealthRequest) (resp *pb.GetHealthResponse, err error) {
	service := global.Service.Clone(ctx)
	dto := service.GetDiceHealth()
	var modules []*pb.HealthModule
	for _, moduleDto := range dto.Modules {
		module := &pb.HealthModule{
			Name:    moduleDto.Name,
			Status:  string(moduleDto.Status),
			Message: moduleDto.Message,
		}
		modules = append(modules, module)
	}
	resp = &pb.GetHealthResponse{
		Status:  string(dto.Status),
		Modules: modules,
	}
	return
}
func (s *globalService) GetTenantGroup(ctx context.Context, req *pb.GetTenantGroupRequest) (resp *pb.GetTenantGroupResponse, err error) {
	service := global.Service.Clone(ctx)
	group, err := service.GetTenantGroup(req.ProjectId, req.Env)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetTenantGroupResponse{
		Data: group,
	}
	return
}
func (s *globalService) CreateTenant(ctx context.Context, req *pb.CreateTenantRequest) (resp *pb.CreateTenantResponse, err error) {
	service := global.Service.Clone(ctx)
	result, err := service.CreateTenant(&dto.TenantDto{
		Id:              req.Id,
		TenantGroup:     req.TenantGroup,
		Az:              req.Az,
		Env:             req.Env,
		ProjectId:       req.ProjectId,
		ProjectName:     req.ProjectName,
		AdminAddr:       req.AdminAddr,
		GatewayEndpoint: req.GatewayEndpoint,
		InnerAddr:       req.InnerAddr,
		ServiceName:     req.ServiceName,
		InstanceId:      req.InstanceId,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.CreateTenantResponse{
		Data: result,
	}
	return
}
func (s *globalService) GetFeatures(ctx context.Context, req *pb.GetFeaturesRequest) (resp *pb.GetFeaturesResponse, err error) {
	service := global.Service.Clone(ctx)
	features := service.GetGatewayFeatures(req.ClusterName)
	resp = &pb.GetFeaturesResponse{
		Data: features,
	}
	return
}
