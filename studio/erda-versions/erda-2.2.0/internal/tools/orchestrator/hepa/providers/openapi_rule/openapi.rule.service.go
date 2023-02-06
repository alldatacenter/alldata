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

package openapi_rule

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/openapi_rule/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type openapiRuleService struct {
	p *provider
}

func (s *openapiRuleService) GetLimits(ctx context.Context, req *pb.GetLimitsRequest) (resp *pb.GetLimitsResponse, err error) {
	service := openapi_rule.Service.Clone(ctx)
	dto := &dto.GetOpenLimitRulesDto{
		ConsumerId: req.ConsumerId,
		PackageId:  req.PackageId,
	}
	dto.PageNo = req.PageNo
	dto.PageSize = req.PageSize
	if dto.PageNo == 0 {
		dto.PageNo = 1
	}
	if dto.PageSize == 20 {
		dto.PageSize = 20
	}
	pageQuery, err := service.GetLimitRules(dto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetLimitsResponse{
		Data: pageQuery.ToPbPage(),
	}
	return
}
func (s *openapiRuleService) CreateLimit(ctx context.Context, req *pb.CreateLimitRequest) (resp *pb.CreateLimitResponse, err error) {
	service := openapi_rule.Service.Clone(ctx)
	result, existed, err := service.CreateLimitRule(&dto.DiceArgsDto{
		ProjectId: req.ProjectId,
		Env:       req.Env,
	}, dto.FromLimitRequest(req.LimitRequest))
	if existed {
		err = erdaErr.NewAlreadyExistsError("rule")
		return
	}
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.CreateLimitResponse{
		Data: result,
	}
	return
}
func (s *openapiRuleService) UpdateLimit(ctx context.Context, req *pb.UpdateLimitRequest) (resp *pb.UpdateLimitResponse, err error) {
	service := openapi_rule.Service.Clone(ctx)
	result, err := service.UpdateLimitRule(req.RuleId, dto.FromLimitRequest(req.LimitRequest))
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	limitRequest := result.OpenLimitRuleDto.ToLimitRequest()
	resp = &pb.UpdateLimitResponse{
		Data: &pb.LimitRuleInfo{
			ConsumerId:   limitRequest.ConsumerId,
			PackageId:    limitRequest.PackageId,
			Method:       limitRequest.Method,
			ApiPath:      limitRequest.ApiPath,
			Limit:        limitRequest.Limit,
			Id:           result.Id,
			CreateAt:     result.CreateAt,
			ConsumerName: result.ConsumerName,
			PackageName:  result.PackageName,
		},
	}
	return
}
func (s *openapiRuleService) DeleteLimit(ctx context.Context, req *pb.DeleteLimitRequest) (resp *pb.DeleteLimitResponse, err error) {
	service := openapi_rule.Service.Clone(ctx)
	result, err := service.DeleteLimitRule(req.RuleId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.DeleteLimitResponse{
		Data: result,
	}
	return
}
