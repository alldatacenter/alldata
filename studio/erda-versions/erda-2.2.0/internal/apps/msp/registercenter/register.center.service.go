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

package registercenter

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/erda-project/erda-proto-go/msp/registercenter/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/registercenter/nacos"
	"github.com/erda-project/erda/internal/apps/msp/registercenter/zkproxy"
	"github.com/erda-project/erda/pkg/common/errors"
)

const engineName = "registercenter"

type registerCenterService struct {
	p                *provider
	instanceTenantDB *instancedb.InstanceTenantDB
	instanceDB       *instancedb.InstanceDB
	bdl              *bundle.Bundle
}

func (s *registerCenterService) ListInterface(ctx context.Context, req *pb.ListInterfaceRequest) (*pb.ListInterfaceResponse, error) {
	result := &pb.ListInterfaceResponse{Data: make([]*pb.Interface, 0)}
	if len(req.TenantGroup) <= 0 {
		return result, nil
	}
	clusterName, err := s.instanceTenantDB.GetClusterNameByTenantGroup(req.TenantGroup)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	ins := instance.New(s.p.DB)
	config, err := ins.GetConfigOptionByGroup(engineName, req.TenantGroup)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if addr, ok := config.Config["ZKPROXY_PUBLIC_HOST"].(string); ok {
		namespace := req.TenantID
		if len(namespace) <= 0 {
			namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
		}
		adp := zkproxy.NewAdapter(clusterName, addr)
		list, err := adp.GetAllInterfaceList(req.ProjectID, req.Env, namespace)
		if err == nil {
			result.Data = append(result.Data, list...)
		}
	}
	if addr, ok := config.Config["NACOS_ADDRESS"].(string); ok {
		namespace, _ := config.Config["NACOS_TENANT_ID"].(string)
		adp := nacos.NewAdapter(clusterName, addr)
		list, err := adp.GetDubboInterfaceList(namespace)
		if err == nil {
			result.Data = append(result.Data, list...)
		}
	}
	return result, nil
}

func (s *registerCenterService) GetHTTPServices(ctx context.Context, req *pb.GetHTTPServicesRequest) (*pb.GetHTTPServicesResponse, error) {
	result := &pb.GetHTTPServicesResponse{
		Data: &pb.HTTPServices{ServiceList: make([]*pb.HTTPService, 0)},
	}
	if len(req.TenantGroup) <= 0 {
		return result, nil
	}
	clusterName, err := s.instanceTenantDB.GetClusterNameByTenantGroup(req.TenantGroup)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	ins := instance.New(s.p.DB)
	config, err := ins.GetConfigOptionByGroup(engineName, req.TenantGroup)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if addr, ok := config.Config["NACOS_ADDRESS"].(string); ok {
		namespace, _ := config.Config["NACOS_TENANT_ID"].(string)
		adp := nacos.NewAdapter(clusterName, addr)
		list, err := adp.GetHTTPInterfaceList(namespace)
		if err != nil {
			return nil, errors.NewServiceInvokingError("nacos.GetHTTPInterfaceList", err)
		}
		result.Data.ServiceList = append(result.Data.ServiceList, list...)
	}
	return result, nil
}

func (s *registerCenterService) EnableHTTPService(ctx context.Context, req *pb.EnableHTTPServiceRequest) (*pb.EnableHTTPServiceResponse, error) {
	result := &pb.EnableHTTPServiceResponse{}
	if len(req.TenantGroup) <= 0 {
		return result, nil
	}
	clusterName, err := s.instanceTenantDB.GetClusterNameByTenantGroup(req.TenantGroup)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	ins := instance.New(s.p.DB)
	config, err := ins.GetConfigOptionByGroup(engineName, req.TenantGroup)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if addr, ok := config.Config["NACOS_ADDRESS"].(string); ok {
		namespace, _ := config.Config["NACOS_TENANT_ID"].(string)
		adp := nacos.NewAdapter(clusterName, addr)
		data, err := adp.EnableHTTPService(namespace, req.Service)
		if err != nil {
			return nil, errors.NewServiceInvokingError("nacos.EnableHTTPService", err)
		}
		result.Data = data
	}
	return result, nil
}

func (s *registerCenterService) getzkProxyHost(clusterName string) (string, error) {
	data, err := s.instanceDB.GetByFields(map[string]interface{}{
		"Engine":  engineName,
		"Version": "1.0.0",
		"Az":      clusterName,
	})
	if err != nil {
		return "", err
	}
	if data == nil {
		return "", fmt.Errorf("fail to get registercenter from tmc")
	}
	config := make(map[string]interface{})
	json.Unmarshal([]byte(data.Config), &config)
	host, ok := config["ZKPROXY_PUBLIC_HOST"].(string)
	if !ok {
		return "", fmt.Errorf("fail to get zkproxy config from tmc")
	}
	return host, nil
}

func (s *registerCenterService) GetServiceIpInfo(ctx context.Context, request *pb.ServiceIpRequest) (*pb.ServiceIpInfoResponse, error) {
	req := apistructs.InstanceInfoRequest{
		ProjectID:  request.GetProjectID(),
		Workspace:  request.GetWorkspace(),
		InstanceIP: strings.Split(request.GetIp(), ":")[0],
	}
	instanceInfo, err := s.bdl.GetInstanceInfo(req)
	if err != nil {
		return nil, errors.NewServiceInvokingError("scheduler", err)
	}
	if !instanceInfo.Success {
		return nil, errors.NewServiceInvokingError("scheduler", fmt.Errorf(instanceInfo.Error.Msg))
	}
	result := &pb.ServiceIpInfoResponse{}
	if len(instanceInfo.Data) > 0 {
		result.ServiceName = instanceInfo.Data[0].ServiceName
		result.RuntimeID = instanceInfo.Data[0].RuntimeID
		result.AppID = instanceInfo.Data[0].ApplicationID
	}
	return result, nil
}
