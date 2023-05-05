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

package openapi_consumer

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/openapi_consumer/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_consumer"
	"github.com/erda-project/erda/pkg/common/apis"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type openapiConsumerService struct {
	p *provider
}

func (s *openapiConsumerService) GetConsumers(ctx context.Context, req *pb.GetConsumersRequest) (resp *pb.GetConsumersResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	reqDto := &dto.GetOpenConsumersDto{}
	reqDto.OrgId = apis.GetOrgID(ctx)
	reqDto.ProjectId = req.ProjectId
	reqDto.Env = req.Env
	reqDto.SortField = req.SortField
	reqDto.SortType = req.SortType
	reqDto.PageNo = req.PageNo
	reqDto.PageSize = req.PageSize
	if reqDto.PageNo == 0 {
		reqDto.PageNo = 1
	}
	if reqDto.PageSize == 0 {
		reqDto.PageSize = 20
	}
	pageQuery, err := service.GetConsumers(reqDto)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetConsumersResponse{
		Data: pageQuery.ToPbPage(),
	}
	return
}
func (s *openapiConsumerService) CreateConsumer(ctx context.Context, req *pb.CreateConsumerRequest) (resp *pb.CreateConsumerResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	id, exists, err := service.CreateConsumer(&dto.DiceArgsDto{
		OrgId:     apis.GetOrgID(ctx),
		ProjectId: req.ProjectId,
		Env:       req.Env,
	}, &dto.OpenConsumerDto{
		Name:        req.Consumer.Name,
		Description: req.Consumer.Description,
	})
	if exists {
		err = erdaErr.NewAlreadyExistsError(req.Consumer.Name)
		return
	}
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.CreateConsumerResponse{
		Data: id,
	}
	return
}
func (s *openapiConsumerService) UpdateConsumer(ctx context.Context, req *pb.UpdateConsumerRequest) (resp *pb.UpdateConsumerResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	consumerDto, err := service.UpdateConsumer(req.ConsumerId, &dto.OpenConsumerDto{
		Description: req.Consumer.Description,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.UpdateConsumerResponse{
		Data: consumerDto.ToConsumer(),
	}
	return
}
func (s *openapiConsumerService) DeleteConsumer(ctx context.Context, req *pb.DeleteConsumerRequest) (resp *pb.DeleteConsumerResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	result, err := service.DeleteConsumer(req.ConsumerId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.DeleteConsumerResponse{
		Data: result,
	}
	return
}
func (s *openapiConsumerService) GetConsumersName(ctx context.Context, req *pb.GetConsumersNameRequest) (resp *pb.GetConsumersNameResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	consumerDtos, err := service.GetConsumersName(&dto.GetOpenConsumersDto{
		DiceArgsDto: dto.DiceArgsDto{
			OrgId:     apis.GetOrgID(ctx),
			ProjectId: req.ProjectId,
			Env:       req.Env,
		},
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	consumers := []*pb.Consumer{}
	for _, consumerDto := range consumerDtos {
		consumers = append(consumers, consumerDto.ToConsumer())
	}
	resp = &pb.GetConsumersNameResponse{
		Data: consumers,
	}
	return
}
func (s *openapiConsumerService) GetConsumerAcl(ctx context.Context, req *pb.GetConsumerAclRequest) (resp *pb.GetConsumerAclResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	dtos, err := service.GetConsumerAcls(req.ConsumerId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	acls := []*pb.Acl{}
	for _, dto := range dtos {
		acls = append(acls, dto.ToAcl())
	}
	resp = &pb.GetConsumerAclResponse{
		Data: acls,
	}
	return
}
func (s *openapiConsumerService) UpdateConsumerAcl(ctx context.Context, req *pb.UpdateConsumerAclRequest) (resp *pb.UpdateConsumerAclResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	result, err := service.UpdateConsumerAcls(req.ConsumerId, &dto.ConsumerAclsDto{
		Packages: req.Packages,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.UpdateConsumerAclResponse{
		Data: result,
	}
	return
}
func (s *openapiConsumerService) GetConsumerAuth(ctx context.Context, req *pb.GetConsumerAuthRequest) (resp *pb.GetConsumerAuthResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	dto, err := service.GetConsumerCredentials(req.ConsumerId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetConsumerAuthResponse{
		Data: dto.ToCredentials(),
	}
	return
}
func (s *openapiConsumerService) UpdateConsumerAuth(ctx context.Context, req *pb.UpdateConsumerAuthRequest) (resp *pb.UpdateConsumerAuthResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	dto, exists, err := service.UpdateConsumerCredentials(req.ConsumerId, dto.FromCredentials(req.Credentials))
	if exists != "" {
		err = erdaErr.NewAlreadyExistsError(exists)
		return
	}
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.UpdateConsumerAuthResponse{
		Data: dto.ToCredentials(),
	}
	return
}
func (s *openapiConsumerService) GetEndpointAcl(ctx context.Context, req *pb.GetEndpointAclRequest) (resp *pb.GetEndpointAclResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	dtos, err := service.GetPackageAcls(req.PackageId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	acls := []*pb.Acl{}
	for _, dto := range dtos {
		acls = append(acls, dto.ToAcl())
	}
	resp = &pb.GetEndpointAclResponse{
		Data: acls,
	}
	return
}
func (s *openapiConsumerService) UpdateEndpointAcl(ctx context.Context, req *pb.UpdateEndpointAclRequest) (resp *pb.UpdateEndpointAclResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	result, err := service.UpdatePackageAcls(req.PackageId, &dto.PackageAclsDto{
		Consumers: req.Consumers,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.UpdateEndpointAclResponse{
		Data: result,
	}
	return
}
func (s *openapiConsumerService) GetEndpointApiAcl(ctx context.Context, req *pb.GetEndpointApiAclRequest) (resp *pb.GetEndpointApiAclResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	dtos, err := service.GetPackageApiAcls(req.PackageId, req.ApiId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	acls := []*pb.Acl{}
	for _, dto := range dtos {
		acls = append(acls, dto.ToAcl())
	}
	resp = &pb.GetEndpointApiAclResponse{
		Data: acls,
	}
	return
}
func (s *openapiConsumerService) UpdateEndpointApiAcl(ctx context.Context, req *pb.UpdateEndpointApiAclRequest) (resp *pb.UpdateEndpointApiAclResponse, err error) {
	service := openapi_consumer.Service.Clone(ctx)
	result, err := service.UpdatePackageApiAcls(req.PackageId, req.ApiId, &dto.PackageAclsDto{
		Consumers: req.Consumers,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.UpdateEndpointApiAclResponse{
		Data: result,
	}
	return
}
