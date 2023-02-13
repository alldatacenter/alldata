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

package org_client

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/org_client/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/exdto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/org_client"
	"github.com/erda-project/erda/pkg/common/apis"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type orgClientService struct {
	p *provider
}

func (s *orgClientService) CreateClient(ctx context.Context, req *pb.CreateClientRequest) (resp *pb.CreateClientResponse, err error) {
	service := org_client.Service.Clone(ctx)
	dto, exists, err := service.Create(apis.GetOrgID(ctx), req.ClientName)
	if exists {
		err = erdaErr.NewAlreadyExistsError(req.ClientName)
		return
	}
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.CreateClientResponse{
		Data: &pb.ClientInfo{
			ClientId:     dto.ClientId,
			ClientSecret: dto.ClientSecret,
		},
	}
	return
}
func (s *orgClientService) DeleteClient(ctx context.Context, req *pb.DeleteClientRequest) (resp *pb.DeleteClientResponse, err error) {
	service := org_client.Service.Clone(ctx)
	result, err := service.Delete(req.ClientId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.DeleteClientResponse{
		Data: result,
	}
	return
}
func (s *orgClientService) GetCredentials(ctx context.Context, req *pb.GetCredentialsRequest) (resp *pb.GetCredentialsResponse, err error) {
	service := org_client.Service.Clone(ctx)
	dto, err := service.GetCredentials(req.ClientId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GetCredentialsResponse{
		Data: &pb.ClientInfo{
			ClientId:     dto.ClientId,
			ClientSecret: dto.ClientSecret,
		},
	}
	return
}
func (s *orgClientService) UpdateCredentials(ctx context.Context, req *pb.UpdateCredentialsRequest) (resp *pb.UpdateCredentialsResponse, err error) {
	service := org_client.Service.Clone(ctx)
	dto, err := service.UpdateCredentials(req.ClientId, req.ClientSecret)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.UpdateCredentialsResponse{
		Data: &pb.ClientInfo{
			ClientId:     dto.ClientId,
			ClientSecret: dto.ClientSecret,
		},
	}
	return
}
func (s *orgClientService) GrantEndpoint(ctx context.Context, req *pb.GrantEndpointRequest) (resp *pb.GrantEndpointResponse, err error) {
	service := org_client.Service.Clone(ctx)
	result, err := service.GrantPackage(req.ClientId, req.PackageId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.GrantEndpointResponse{
		Data: result,
	}
	return
}
func (s *orgClientService) RevokeEndpoint(ctx context.Context, req *pb.RevokeEndpointRequest) (resp *pb.RevokeEndpointResponse, err error) {
	service := org_client.Service.Clone(ctx)
	result, err := service.RevokePackage(req.ClientId, req.PackageId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.RevokeEndpointResponse{
		Data: result,
	}
	return
}
func (s *orgClientService) ChangeClientLimit(ctx context.Context, req *pb.ChangeClientLimitRequest) (resp *pb.ChangeClientLimitResponse, err error) {
	service := org_client.Service.Clone(ctx)
	var limits []exdto.LimitType
	for _, l := range req.Limits {
		limit := exdto.LimitType{}
		if l.Qpd != 0 {
			i := (int)(l.Qpd)
			limit.Day = &i
		}
		if l.Qph != 0 {
			i := (int)(l.Qph)
			limit.Hour = &i
		}
		if l.Qpm != 0 {
			i := (int)(l.Qpm)
			limit.Minute = &i
		}
		if l.Qps != 0 {
			i := (int)(l.Qps)
			limit.Second = &i
		}
		limits = append(limits, limit)
	}
	result, err := service.CreateOrUpdateLimit(req.ClientId, req.PackageId, exdto.ChangeLimitsReq{
		Limits: limits,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.ChangeClientLimitResponse{
		Data: result,
	}
	return
}
