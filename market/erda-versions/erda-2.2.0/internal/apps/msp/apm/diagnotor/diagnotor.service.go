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

package diagnotor

import (
	"context"
	"sort"

	basepb "github.com/erda-project/erda-proto-go/core/monitor/diagnotor/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/diagnotor/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/errors"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type diagnotorService struct {
	p *provider
}

func (s *diagnotorService) ListServices(ctx context.Context, req *pb.ListServicesRequest) (*pb.ListServicesResponse, error) {
	pdata, ok := perm.GetPermissionDataFromContext(ctx, scopeKey)
	if !ok || pdata == nil {
		return nil, errors.NewNotFoundError("services")
	}
	info := pdata.(*scopeInfo)

	resp, err := s.p.bdl.GetInstanceInfo(apistructs.InstanceInfoRequest{
		Cluster:   info.ClusterName,
		ProjectID: info.ProjectID,
		Workspace: info.Workspace,
	})
	if err != nil {
		return nil, errors.NewServiceInvokingError("instances", err)
	}

	services := make(map[string][]*apistructs.InstanceInfoData)
	var keys []string
	for _, item := range resp.Data {
		if len(item.ServiceName) <= 0 {
			continue
		}
		instance := item
		key := item.ApplicationID + "/" + item.ServiceName
		services[key] = append(services[key], &instance)
		keys = append(keys, key)
	}
	sort.Strings(keys)

	result := &pb.ListServicesResponse{}
	for _, key := range keys {
		info := services[key]
		if len(info) <= 0 {
			continue
		}
		first := info[0]
		service := &pb.ServiceInfo{
			OrgName:         first.OrgName,
			OrgID:           first.OrgID,
			ClusterName:     first.Cluster,
			ProjectName:     first.ProjectName,
			ProjectID:       first.ProjectID,
			ApplicationName: first.ApplicationName,
			ApplicationID:   first.ApplicationID,
			Service:         first.ServiceName,
		}
		for _, item := range info {
			metadata := parseInstanceMetadata(item.Meta)
			service.Instances = append(service.Instances, &pb.InstanceInfo{
				PodName:     metadata["k8spodname"],
				Namespace:   metadata["k8snamespace"],
				HostIP:      item.HostIP,
				Ip:          item.ContainerIP,
				RuntimeName: item.RuntimeName,
				RuntimeID:   item.RuntimeID,
			})
		}
		result.Data = append(result.Data, service)
	}
	return result, nil
}

func (s *diagnotorService) StartDiagnosis(ctx context.Context, req *pb.StartDiagnosisRequest) (*pb.StartDiagnosisResponse, error) {
	pdata, ok := perm.GetPermissionDataFromContext(ctx, instanceKey)
	if !ok || pdata == nil {
		return nil, errors.NewNotFoundError("instance")
	}
	info := pdata.(*instanceInfo)

	resp, err := s.p.BaseDiagnotorService.StartDiagnosis(ctx, &basepb.StartDiagnosisRequest{
		ClusterName: info.ClusterName,
		Namespace:   info.Namespace,
		PodName:     info.PodName,
	})
	if err != nil {
		return nil, unwrapRpcError(err)
	}
	return &pb.StartDiagnosisResponse{
		Data: resp.Data,
	}, nil
}

func (s *diagnotorService) QueryDiagnosisStatus(ctx context.Context, req *pb.QueryDiagnosisStatusRequest) (*pb.QueryDiagnosisStatusResponse, error) {
	pdata, ok := perm.GetPermissionDataFromContext(ctx, instanceKey)
	if !ok || pdata == nil {
		return nil, errors.NewNotFoundError("instance")
	}
	info := pdata.(*instanceInfo)

	resp, err := s.p.BaseDiagnotorService.QueryDiagnosisStatus(ctx, &basepb.QueryDiagnosisStatusRequest{
		ClusterName: info.ClusterName,
		Namespace:   info.Namespace,
		PodName:     info.PodName,
	})
	if err != nil {
		return nil, unwrapRpcError(err)
	}
	return &pb.QueryDiagnosisStatusResponse{
		Data: resp.Data,
	}, nil
}

func (s *diagnotorService) StopDiagnosis(ctx context.Context, req *pb.StopDiagnosisRequest) (*pb.StopDiagnosisResponse, error) {
	pdata, ok := perm.GetPermissionDataFromContext(ctx, instanceKey)
	if !ok || pdata == nil {
		return nil, errors.NewNotFoundError("instance")
	}
	info := pdata.(*instanceInfo)

	resp, err := s.p.BaseDiagnotorService.StopDiagnosis(ctx, &basepb.StopDiagnosisRequest{
		ClusterName: info.ClusterName,
		Namespace:   info.Namespace,
		PodName:     info.PodName,
	})
	if err != nil {
		return nil, unwrapRpcError(err)
	}
	return &pb.StopDiagnosisResponse{
		Data: resp.Data,
	}, nil
}

func (s *diagnotorService) ListProcesses(ctx context.Context, req *pb.ListProcessesRequest) (*pb.ListProcessesResponse, error) {
	pdata, ok := perm.GetPermissionDataFromContext(ctx, instanceKey)
	if !ok || pdata == nil {
		return nil, errors.NewNotFoundError("instance")
	}
	info := pdata.(*instanceInfo)

	resp, err := s.p.BaseDiagnotorService.ListProcesses(ctx, &basepb.ListProcessesRequest{
		ClusterName: info.ClusterName,
		Namespace:   info.Namespace,
		PodName:     info.PodName,
		PodIP:       info.PodIP,
	})
	if err != nil {
		return nil, unwrapRpcError(err)
	}
	return &pb.ListProcessesResponse{
		Data: resp.Data,
	}, nil
}
