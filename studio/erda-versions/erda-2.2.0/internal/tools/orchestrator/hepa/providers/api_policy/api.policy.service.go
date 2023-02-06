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

package api_policy

import (
	"context"

	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/hepa/api_policy/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/api_policy"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type apiPolicyService struct {
	p *provider
}

func (s *apiPolicyService) GetPolicy(ctx context.Context, req *pb.GetPolicyRequest) (resp *pb.GetPolicyResponse, err error) {
	service := api_policy.Service.Clone(ctx)
	config, err := service.GetPolicyConfig(req.Category, req.PackageId, req.ApiId)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	value, err := structpb.NewValue(util.GetPureInterface(config))
	if err != nil {
		err = erdaErr.NewInternalServerError(errors.Cause(err))
		return
	}
	resp = &pb.GetPolicyResponse{
		Data: value,
	}
	return
}
func (s *apiPolicyService) SetPolicy(ctx context.Context, req *pb.SetPolicyRequest) (resp *pb.SetPolicyResponse, err error) {
	service := api_policy.Service.Clone(ctx)
	config, err := service.SetPolicyConfig(req.Category, req.PackageId, req.ApiId, req.Body)
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	value, err := structpb.NewValue(util.GetPureInterface(config))
	if err != nil {
		err = erdaErr.NewInternalServerError(errors.Cause(err))
		return
	}
	resp = &pb.SetPolicyResponse{
		Data: value,
	}
	return
}
