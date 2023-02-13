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

package legacy_upstream_lb

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/hepa/legacy_upstream_lb/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_upstream_lb"
	erdaErr "github.com/erda-project/erda/pkg/common/errors"
)

type upstreamLbService struct {
	p *provider
}

func (s *upstreamLbService) TargetOnline(ctx context.Context, req *pb.TargetOnlineRequest) (resp *pb.TargetOnlineResponse, err error) {
	service := legacy_upstream_lb.Service.Clone(ctx)
	if req.Lb == nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, "invalid request")
		return
	}
	result, err := service.UpstreamTargetOnline(&dto.UpstreamLbDto{
		Az:              req.Lb.Az,
		LbName:          req.Lb.LbName,
		OrgId:           req.Lb.OrgId,
		ProjectId:       req.Lb.ProjectId,
		Env:             req.Lb.Env,
		DeploymentId:    (int)(req.Lb.DeploymentId),
		HealthcheckPath: req.Lb.HealthcheckPath,
		Targets:         req.Lb.Targets,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.TargetOnlineResponse{
		Data: result,
	}
	return
}
func (s *upstreamLbService) TargetOffline(ctx context.Context, req *pb.TargetOfflineRequest) (resp *pb.TargetOfflineResponse, err error) {
	service := legacy_upstream_lb.Service.Clone(ctx)
	if req.Lb == nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, "invalid request")
		return
	}
	result, err := service.UpstreamTargetOffline(&dto.UpstreamLbDto{
		Az:              req.Lb.Az,
		LbName:          req.Lb.LbName,
		OrgId:           req.Lb.OrgId,
		ProjectId:       req.Lb.ProjectId,
		Env:             req.Lb.Env,
		DeploymentId:    (int)(req.Lb.DeploymentId),
		HealthcheckPath: req.Lb.HealthcheckPath,
		Targets:         req.Lb.Targets,
	})
	if err != nil {
		err = erdaErr.NewInvalidParameterError(vars.TODO_PARAM, errors.Cause(err).Error())
		return
	}
	resp = &pb.TargetOfflineResponse{
		Data: result,
	}
	return

}
