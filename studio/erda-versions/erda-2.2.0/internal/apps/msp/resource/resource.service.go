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

package resource

import (
	"context"
	"fmt"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda-proto-go/msp/resource/pb"
	monitordb "github.com/erda-project/erda/internal/apps/msp/instance/db/monitor"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/coordinator"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
)

type resourceService struct {
	p           *provider
	coordinator coordinator.Interface
	es          *elastic.Client
	monitorDb   *monitordb.MonitorDB
}

func (s *resourceService) GetMonitorInstance(ctx context.Context, request *pb.GetMonitorInstanceRequest) (*pb.GetMonitorInstanceResponse, error) {
	instance, err := s.monitorDb.GetByTerminusKey(request.TerminusKey)
	if err != nil {
		return nil, err
	}

	if instance == nil {
		return nil, fmt.Errorf("terminusKey not exists")
	}

	return &pb.GetMonitorInstanceResponse{
		Data: &pb.MonitorInstance{
			MonitorId:   instance.MonitorId,
			MonitorName: instance.ProjectName + "-" + instance.Workspace,
			TerminusKey: instance.TerminusKey,
			Workspace:   instance.Workspace,
			ProjectId:   instance.ProjectId,
			ProjectName: instance.ProjectName,
			CreateTime:  instance.Created.Format("2006-01-02 15:04:05"),
			UpdateTime:  instance.Updated.Format("2006-01-02 15:04:05"),
		},
	}, nil
}

func (s *resourceService) GetMonitorRuntime(ctx context.Context, req *pb.GetMonitorRuntimeRequest) (*pb.GetMonitorRuntimeResponse, error) {
	result, err := s.QueryRuntime(RuntimeQuery{
		RuntimeName:   req.RuntimeName,
		RuntimeId:     "", // todo why here omit the runtimeId from request
		ApplicationId: req.ApplicationId,
		TerminusKey:   req.TerminusKey,
	})

	if result == nil || err != nil {
		return nil, err
	}

	return &pb.GetMonitorRuntimeResponse{
		Data: &pb.MonitorRuntime{
			RuntimeId:       result.RuntimeId,
			RuntimeName:     result.RuntimeName,
			TerminusKey:     result.TerminusKey,
			Workspace:       result.Workspace,
			ProjectId:       result.ProjectId,
			ProjectName:     result.ProjectName,
			ApplicationId:   result.ApplicationId,
			ApplicationName: result.ApplicationName,
		},
	}, nil
}

func (s *resourceService) CreateResource(ctx context.Context, req *pb.CreateResourceRequest) (*pb.CreateResourceResponse, error) {
	deployReq := handlers.ResourceDeployRequest{
		Az:          req.Az,
		Uuid:        req.Uuid,
		Plan:        req.Plan,
		Engine:      req.Engine,
		Callback:    req.Callback,
		Options:     req.Options,
		TenantGroup: req.Options["tenantGroup"],
	}

	needDeployInstance, err := s.coordinator.CheckIfNeedRealDeploy(deployReq)
	if err != nil {
		return nil, err
	}

	// if need real deployment action, return with INIT status and deploy async
	if needDeployInstance {
		// use goroutine to async deploy
		go s.coordinator.Deploy(deployReq)

		return &pb.CreateResourceResponse{
			Data: &pb.ResourceCreateResult{
				Id:     req.Uuid,
				Status: handlers.TmcInstanceStatusInit,
			},
		}, nil
	}

	result, err := s.coordinator.Deploy(deployReq)

	if err != nil {
		return nil, err
	}

	return &pb.CreateResourceResponse{
		Data: &pb.ResourceCreateResult{
			Id:        result.ID,
			Config:    result.Config,
			Status:    result.Status,
			Label:     map[string]string{},
			UpdateAt:  result.UpdatedTime.UTC().Format("2006-01-02T15:04:05Z"),
			CreatedAt: result.CreatedTime.UTC().Format("2006-01-02T15:04:05Z"),
		},
	}, nil
}

func (s *resourceService) DeleteResource(ctx context.Context, req *pb.DeleteResourceRequest) (*pb.DeleteResourceResponse, error) {
	err := s.coordinator.UnDeploy(req.Id)

	if err != nil {
		return nil, err
	}

	return &pb.DeleteResourceResponse{Data: true}, nil
}
