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

package micro_api

import (
	"context"

	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/hepa/api/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/micro_api"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type apiService struct {
	p *provider
}

func (s *apiService) GetApis(ctx context.Context, req *pb.GetApisRequest) (resp *pb.GetApisResponse, err error) {
	service := micro_api.Service.Clone(ctx)
	reqDto := &dto.GetApisDto{
		From:         req.From,
		Method:       req.Method,
		DiceApp:      req.DiceApp,
		DiceService:  req.DiceService,
		RuntimeId:    req.RuntimeId,
		ApiPath:      req.ApiPath,
		RegisterType: req.RegisterType,
		NetType:      req.NetType,
		NeedAuth:     int(req.NeedAuth),
		SortField:    req.SortField,
		SortType:     req.SortType,
		OrgId:        req.OrgId,
		ProjectId:    req.ProjectId,
		Env:          req.Env,
		Size:         req.Size,
		Page:         req.Page,
	}
	if reqDto.Size == 0 {
		reqDto.Size = 20
	}
	if reqDto.Page == 0 {
		reqDto.Page = 1
	}
	pageQuery, err := service.GetApiInfos(reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetApisResponse{
		Data: pageQuery.ToPbPage(),
	}
	return
}

func (s *apiService) CreateApi(ctx context.Context, req *pb.CreateApiRequest) (resp *pb.CreateApiResponse, err error) {
	service := micro_api.Service.Clone(ctx)
	reqDto := &dto.ApiReqDto{
		ApiDto:          dto.MakeApiDto(req.ApiRequest),
		ApiReqOptionDto: &dto.ApiReqOptionDto{},
	}
	apiId, err := service.CreateApi(reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.CreateApiResponse{
		ApiId: apiId,
	}
	return
}

func (s *apiService) UpdateApi(ctx context.Context, req *pb.UpdateApiRequest) (resp *pb.UpdateApiResponse, err error) {
	service := micro_api.Service.Clone(ctx)
	reqDto := &dto.ApiReqDto{
		ApiDto: dto.MakeApiDto(req.ApiRequest),
	}
	apiInfo, err := service.UpdateApi(req.ApiId, reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	var pathPrefix, method *structpb.Value
	if apiInfo.DisplayPathPrefix != "" {
		pathPrefix, err = structpb.NewValue(apiInfo.DisplayPathPrefix)
		if err != nil {
			err = erdaErr.NewInternalServerError(err)
			return
		}
	}
	if apiInfo.Method != "" {
		method, err = structpb.NewValue(apiInfo.Method)
		if err != nil {
			err = erdaErr.NewInternalServerError(err)
			return
		}
	}
	resp = &pb.UpdateApiResponse{
		ApiId:             req.ApiId,
		Path:              apiInfo.Path,
		DisplayPath:       apiInfo.DisplayPath,
		DisplayPathPrefix: pathPrefix,
		OuterNetEnable:    apiInfo.OuterNetEnable,
		RegisterType:      apiInfo.RegisterType,
		NeedAuth:          apiInfo.NeedAuth,
		Method:            method,
		Description:       apiInfo.Description,
		RedirectAddr:      apiInfo.RedirectAddr,
		RedirectPath:      apiInfo.RedirectPath,
		RedirectType:      apiInfo.RedirectType,
		MonitorPath:       apiInfo.MonitorPath,
		CreateAt:          apiInfo.CreateAt,
		Policies:          dto.MakePolicies(apiInfo.Policies),
	}
	return
}

func (s *apiService) DeleteApi(ctx context.Context, req *pb.DeleteApiRequest) (resp *pb.DeleteApiResponse, err error) {
	service := micro_api.Service.Clone(ctx)
	err = service.DeleteApi(req.ApiId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.DeleteApiResponse{
		Data: true,
	}
	return
}
