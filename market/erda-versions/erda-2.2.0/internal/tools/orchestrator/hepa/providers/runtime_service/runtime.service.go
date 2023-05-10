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

package runtime_service

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/runtime_service/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/runtime_service"
	"github.com/erda-project/erda/pkg/common/apis"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type runtimeService struct {
	p *provider
}

func (s *runtimeService) ChangeRuntime(ctx context.Context, req *pb.ChangeRuntimeRequest) (resp *pb.ChangeRuntimeResponse, err error) {
	service := runtime_service.Service.Clone(ctx)
	reqDto := &dto.RuntimeServiceReqDto{
		OrgId:                 req.OrgId,
		ProjectId:             req.ProjectId,
		Env:                   req.Env,
		ClusterName:           req.ClusterName,
		RuntimeId:             req.RuntimeId,
		RuntimeName:           req.RuntimeName,
		ReleaseId:             req.ReleaseId,
		ServiceGroupNamespace: req.ServiceGroupNamespace,
		ProjectNamespace:      req.ProjectNamespace,
		ServiceGroupName:      req.ServiceGroupName,
		AppId:                 req.AppId,
		AppName:               req.AppName,
		UseApigw:              req.UseApigw,
	}
	for _, service := range req.Services {
		serviceDto := dto.ServiceDetailDto{
			ServiceName:  service.ServiceName,
			InnerAddress: service.InnerAddress,
		}
		for _, domain := range service.EndpointDomains {
			serviceDto.EndpointDomains = append(serviceDto.EndpointDomains, dto.EndpointDomainDto{Domain: domain.Domain, Type: domain.Type})
		}
		reqDto.Services = append(reqDto.Services, serviceDto)
	}
	result, err := service.TouchRuntime(reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.ChangeRuntimeResponse{
		Data: result,
	}
	return

}
func (s *runtimeService) DeleteRuntime(ctx context.Context, req *pb.DeleteRuntimeRequest) (resp *pb.DeleteRuntimeResponse, err error) {
	service := runtime_service.Service.Clone(ctx)
	err = service.DeleteRuntime(req.RuntimeId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.DeleteRuntimeResponse{
		Data: true,
	}
	return
}
func (s *runtimeService) GetApps(ctx context.Context, req *pb.GetAppsRequest) (resp *pb.GetAppsResponse, err error) {
	service := runtime_service.Service.Clone(ctx)
	result, err := service.GetRegisterAppInfo(req.ProjectId, req.Env)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	var apps []*pb.App
	for _, app := range result.Apps {
		apps = append(apps, &pb.App{
			Name:     app.Name,
			Services: app.Services,
		})
	}

	resp = &pb.GetAppsResponse{
		Apps: apps,
	}
	return
}
func (s *runtimeService) GetServiceRuntimes(ctx context.Context, req *pb.GetServiceRuntimesRequest) (resp *pb.GetServiceRuntimesResponse, err error) {
	service := runtime_service.Service.Clone(ctx)
	result, err := service.GetServiceRuntimes(req.ProjectId, req.Env, req.App, req.Service)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	serviceRuntimes := []*pb.ServiceRuntime{}
	for _, runtime := range result {
		serviceRuntimes = append(serviceRuntimes, &pb.ServiceRuntime{
			RuntimeId:   runtime.RuntimeId,
			RuntimeName: runtime.RuntimeName,
			AppId:       runtime.AppId,
			AppName:     runtime.AppName,
			ServiceName: runtime.ServiceName,
		})
	}
	resp = &pb.GetServiceRuntimesResponse{
		Data: serviceRuntimes,
	}
	return
}
func (s *runtimeService) GetServiceApiPrefix(ctx context.Context, req *pb.GetServiceApiPrefixRequest) (resp *pb.GetServiceApiPrefixResponse, err error) {
	service := runtime_service.Service.Clone(ctx)
	reqDto := &dto.ApiPrefixReqDto{
		OrgId:     apis.GetOrgID(ctx),
		ProjectId: req.ProjectId,
		Env:       req.Env,
		App:       req.App,
		Service:   req.Service,
		RuntimeId: req.RuntimeId,
	}
	result, err := service.GetServiceApiPrefix(reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetServiceApiPrefixResponse{
		Data: result,
	}
	return
}
