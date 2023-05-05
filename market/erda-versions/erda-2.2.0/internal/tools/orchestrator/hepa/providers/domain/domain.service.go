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

package domain

import (
	"context"

	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/hepa/domain/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/domain"
	"github.com/erda-project/erda/pkg/common/apis"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type domainService struct {
	p *provider
}

func (s *domainService) GetOrgDomains(ctx context.Context, req *pb.GetOrgDomainsRequest) (resp *pb.GetOrgDomainsResponse, err error) {
	service := domain.Service.Clone(ctx)
	reqDto := dto.ManageDomainReq{
		UserID:      apis.GetUserID(ctx),
		OrgId:       apis.GetOrgID(ctx),
		Domain:      req.Domain,
		ClusterName: req.ClusterName,
		Type:        dto.DomainType(req.Type),
		ProjectID:   req.ProjectId,
		Workspace:   req.Env,
		PageSize:    req.PageSize,
		PageNo:      req.PageNo,
	}
	if reqDto.PageSize == 0 {
		reqDto.PageSize = 20
	}
	if reqDto.PageNo == 0 {
		reqDto.PageNo = 1
	}
	pageQuery, err := service.GetOrgDomainInfo(&reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetOrgDomainsResponse{
		Data: pageQuery.ToPbPage(),
	}
	return
}
func (s *domainService) GetTenantDomains(ctx context.Context, req *pb.GetTenantDomainsRequest) (resp *pb.GetTenantDomainsResponse, err error) {
	service := domain.Service.Clone(ctx)
	result, err := service.GetTenantDomains(req.ProjectId, req.Env)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetTenantDomainsResponse{
		Data: result,
	}
	return
}
func (s *domainService) ChangeInnerIngress(ctx context.Context, req *pb.ChangeInnerIngressRequest) (resp *pb.ChangeInnerIngressResponse, err error) {
	service := domain.Service.Clone(ctx)
	reqDto := apistructs.ComponentIngressUpdateRequest{
		K8SNamespace:  req.K8SNamespace,
		ComponentName: req.ComponentName,
		ComponentPort: int(req.ComponentPort),
		ClusterName:   req.ClusterName,
		IngressName:   req.IngressName,
	}
	for _, route := range req.Routes {
		reqDto.Routes = append(reqDto.Routes, apistructs.IngressRoute{
			Domain: route.Domain,
			Path:   route.Path,
		})
	}
	if req.RouteOptions != nil {
		if req.RouteOptions.RewriteHost != "" {
			reqDto.RouteOptions.RewriteHost = &req.RouteOptions.RewriteHost
		}
		if req.RouteOptions.RewritePath != "" {
			reqDto.RouteOptions.RewritePath = &req.RouteOptions.RewritePath
		}
		reqDto.RouteOptions.UseRegex = req.RouteOptions.UseRegex
		if req.RouteOptions.EnableTls != nil {
			value := req.RouteOptions.EnableTls.GetBoolValue()
			reqDto.RouteOptions.EnableTLS = &value
		}
		reqDto.RouteOptions.Annotations = req.RouteOptions.Annotations
		if req.RouteOptions.LocationSnippet != "" {
			reqDto.RouteOptions.LocationSnippet = &req.RouteOptions.LocationSnippet
		}
	}
	result, err := service.CreateOrUpdateComponentIngress(reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.ChangeInnerIngressResponse{
		Data: result,
	}
	return
}
func (s *domainService) GetRuntimeDomains(ctx context.Context, req *pb.GetRuntimeDomainsRequest) (resp *pb.GetRuntimeDomainsResponse, err error) {
	service := domain.Service.Clone(ctx)
	result, err := service.GetRuntimeDomains(req.RuntimeId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetRuntimeDomainsResponse{
		Data: map[string]*structpb.ListValue{},
	}
	for key, value := range result {
		var list []interface{}
		for _, v := range value {
			list = append(list, util.GetPureInterface(v))
		}
		var listValue *structpb.ListValue
		listValue, err = structpb.NewList(list)
		if err != nil {
			err = erdaErr.NewInternalServerError(err)
			return
		}
		resp.Data[key] = listValue
	}
	return
}
func (s *domainService) ChangeRuntimeDomains(ctx context.Context, req *pb.ChangeRuntimeDomainsRequest) (resp *pb.ChangeRuntimeDomainsResponse, err error) {
	service := domain.Service.Clone(ctx)
	reqDto := &dto.ServiceDomainReqDto{
		ReleaseId: req.ReleaseId,
		Domains:   req.Domains,
	}
	result, existDomain, err := service.UpdateRuntimeServiceDomain(apis.GetOrgID(ctx), req.RuntimeId, req.ServiceName, reqDto)
	if existDomain != "" {
		err = erdaErr.NewAlreadyExistsError("domain: " + existDomain)
		return
	}
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.ChangeRuntimeDomainsResponse{
		Data: result,
	}
	return
}
