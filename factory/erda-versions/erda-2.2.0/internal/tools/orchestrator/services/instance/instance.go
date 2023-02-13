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

package instance

import (
	"math"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/strutil"
)

// Instance instance 实例对象封装
type Instance struct {
	bdl              *bundle.Bundle
	instanceinfoImpl *instanceinfo.InstanceInfoImpl
}

// Option addon 实例对象配置选项
type Option func(*Instance)

// New 新建 addon service
func New(options ...Option) *Instance {
	var instance Instance
	for _, op := range options {
		op(&instance)
	}

	return &instance
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(i *Instance) {
		i.bdl = bdl
	}
}

// WithBundle 配置 bundle
func WithInstanceInfo(instanceinfoImpl *instanceinfo.InstanceInfoImpl) Option {
	return func(i *Instance) {
		i.instanceinfoImpl = instanceinfoImpl
	}
}

// ListProjectUsageByCluster 根据 clusterName 获取集群下项目资源使用列表
func (i *Instance) ListProjectUsageByCluster(orgID, clusterName, workspace string, limited_project_ids []string) ([]apistructs.ProjectUsageFetchResponseData, error) {
	req := apistructs.InstanceInfoRequest{
		OrgID:       orgID,
		Cluster:     clusterName,
		ServiceType: "stateless-service",
		Phases:      []string{apistructs.InstanceStatusRunning, apistructs.InstanceStatusHealthy, apistructs.InstanceStatusUnHealthy},
		Limit:       10000,
	}
	if workspace != "" {
		req.Workspace = workspace
	}
	instanceList, err := i.instanceinfoImpl.GetInstanceInfo(req)
	if err != nil {
		return nil, err
	}

	// 按照 project 维度聚合
	projects := make(map[string]apistructs.ProjectUsageFetchResponseData, len(instanceList))
	for _, v := range instanceList {
		if v.RuntimeID == "" { // pipeline action 实例无 runtimeId, 须过滤掉(临时方案，后期 scheduler 提供根据 type 参数过滤)
			continue
		}

		if p, ok := projects[v.ProjectID]; ok {
			p.Instance++
			p.CPU += v.CpuRequest
			p.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				p.UnhealthyNum++
			}

			projects[v.ProjectID] = p
		} else {
			p.ID = v.ProjectID
			p.Name = v.ProjectName
			p.Workspace = v.Workspace
			p.Instance++
			p.CPU += v.CpuRequest
			p.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				p.UnhealthyNum++
			}

			projects[v.ProjectID] = p
		}
	}

	usages := make([]apistructs.ProjectUsageFetchResponseData, 0, len(projects))
	for _, v := range projects {
		if limited_project_ids != nil && !strutil.Exist(limited_project_ids, v.ID) {
			continue
		}
		v.CPU = utils.Round(v.CPU, 2)
		v.Memory = math.Ceil(v.Memory)
		usages = append(usages, v)
	}

	return usages, nil
}

// ListAppUsageByProject 根据 projectID 获取项目下各应用资源使用统计
func (i *Instance) ListAppUsageByProject(orgID, projectID, workspace string, limited_app_ids []string) ([]apistructs.ApplicationUsageFetchResponseData, error) {
	req := apistructs.InstanceInfoRequest{
		OrgID:       orgID,
		ProjectID:   projectID,
		ServiceType: "stateless-service",
		Phases:      []string{apistructs.InstanceStatusRunning, apistructs.InstanceStatusHealthy, apistructs.InstanceStatusUnHealthy},
		Limit:       10000,
	}
	if workspace != "" {
		req.Workspace = workspace
	}
	instanceList, err := i.instanceinfoImpl.GetInstanceInfo(req)
	if err != nil {
		return nil, err
	}

	// 按照 app 维度聚合
	apps := make(map[string]apistructs.ApplicationUsageFetchResponseData, len(instanceList))
	for _, v := range instanceList {
		if v.RuntimeID == "" { // pipeline action 实例无 runtimeId, 须过滤掉(临时方案，后期 scheduler 提供根据 type 参数过滤)
			continue
		}

		if a, ok := apps[v.ApplicationID]; ok {
			a.Instance++
			a.CPU += v.CpuRequest
			a.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				a.UnhealthyNum++
			}

			apps[v.ApplicationID] = a
		} else {
			a.ID = v.ApplicationID
			a.Name = v.ApplicationName
			a.Instance++
			a.CPU += v.CpuRequest
			a.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				a.UnhealthyNum++
			}

			apps[v.ApplicationID] = a
		}
	}

	usages := make([]apistructs.ApplicationUsageFetchResponseData, 0, len(apps))
	for _, v := range apps {
		if limited_app_ids != nil && !strutil.Exist(limited_app_ids, v.ID) {
			continue
		}
		v.CPU = utils.Round(v.CPU, 2)
		v.Memory = math.Ceil(v.Memory)
		usages = append(usages, v)
	}

	return usages, nil
}

// ListRuntimeUsageByApp 根据 appID 获取应用下各 runtime 资源使用统计
func (i *Instance) ListRuntimeUsageByApp(orgID, appID, workspace string, limited_runtime_ids []string) ([]apistructs.RuntimeUsageFetchResponseData, error) {
	req := apistructs.InstanceInfoRequest{
		OrgID:         orgID,
		ApplicationID: appID,
		ServiceType:   "stateless-service",
		Phases:        []string{apistructs.InstanceStatusRunning, apistructs.InstanceStatusHealthy, apistructs.InstanceStatusUnHealthy},
		Limit:         10000,
	}
	if workspace != "" {
		req.Workspace = workspace
	}
	instanceList, err := i.instanceinfoImpl.GetInstanceInfo(req)
	if err != nil {
		return nil, err
	}

	// 按照 runtime 维度聚合
	runtimes := make(map[string]apistructs.RuntimeUsageFetchResponseData, len(instanceList))
	for _, v := range instanceList {
		if v.RuntimeID == "" { // pipeline action 实例无 runtimeId, 须过滤掉(临时方案，后期 scheduler 提供根据 type 参数过滤)
			continue
		}
		if r, ok := runtimes[v.RuntimeID]; ok {
			r.Instance++
			r.CPU += v.CpuRequest
			r.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				r.UnhealthyNum++
			}

			runtimes[v.RuntimeID] = r
		} else {
			r.ID = v.RuntimeID
			r.Name = v.RuntimeName
			r.Instance++
			r.CPU += v.CpuRequest
			r.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				r.UnhealthyNum++
			}

			runtimes[v.RuntimeID] = r
		}
	}
	usages := make([]apistructs.RuntimeUsageFetchResponseData, 0, len(runtimes))
	for _, v := range runtimes {
		if limited_runtime_ids != nil && !strutil.Exist(limited_runtime_ids, v.ID) {
			continue
		}
		v.CPU = utils.Round(v.CPU, 2)
		v.Memory = math.Ceil(v.Memory)
		usages = append(usages, v)
	}

	return usages, nil
}

// ListServiceUsageByRuntime 根据 runtimeID 获取 runtime 下各 service 资源使用统计
func (i *Instance) ListServiceUsageByRuntime(orgID, runtimeID, workspace string, limited_service_names []string) ([]apistructs.ServiceUsageFetchResponseData, error) {
	req := apistructs.InstanceInfoRequest{
		OrgID:       orgID,
		RuntimeID:   runtimeID,
		ServiceType: "stateless-service",
		Phases:      []string{apistructs.InstanceStatusRunning, apistructs.InstanceStatusHealthy, apistructs.InstanceStatusUnHealthy},
		Limit:       10000,
	}
	if workspace != "" {
		req.Workspace = workspace
	}

	instanceList, err := i.instanceinfoImpl.GetInstanceInfo(req)
	if err != nil {
		return nil, err
	}

	// 按照 service 维度聚合
	services := make(map[string]apistructs.ServiceUsageFetchResponseData, len(instanceList))
	for _, v := range instanceList {
		if s, ok := services[v.ServiceName]; ok {
			s.Instance++
			s.CPU += v.CpuRequest
			s.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				s.UnhealthyNum++
			}

			services[v.ServiceName] = s
		} else {
			s.Name = v.ServiceName
			s.Instance++
			s.CPU += v.CpuRequest
			s.Memory += float64(v.MemRequest)

			if v.Phase != string(apistructs.InstanceStatusHealthy) {
				s.UnhealthyNum++
			}

			services[v.ServiceName] = s
		}
	}
	usages := make([]apistructs.ServiceUsageFetchResponseData, 0, len(services))
	for _, v := range services {
		if limited_service_names != nil && !strutil.Exist(limited_service_names, v.Name) {
			continue
		}
		v.CPU = utils.Round(v.CPU, 2)
		v.Memory = math.Ceil(v.Memory)
		usages = append(usages, v)
	}

	return usages, nil
}
