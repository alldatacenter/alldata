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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

// Resource 应用实例对象封装
type Resource struct {
	db  *dbclient.DBClient
	bdl *bundle.Bundle
}

// Option 应用实例对象配置选项
type Option func(*Resource)

// New 新建应用实例 service
func New(options ...Option) *Resource {
	r := &Resource{}
	for _, op := range options {
		op(r)
	}
	return r
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(r *Resource) {
		r.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(r *Resource) {
		r.bdl = bdl
	}
}

// GetProjectServiceResource 获取project下，service的使用资源
func (r *Resource) GetProjectServiceResource(projectIDs []uint64) (*map[uint64]apistructs.ProjectResourceItem, error) {
	if len(projectIDs) == 0 {
		return nil, nil
	}

	runtimes, err := r.db.GetRuntimeByProjectIDs(projectIDs)
	if err != nil {
		return nil, err
	}
	projectResourceMap := map[uint64]apistructs.ProjectResourceItem{}
	for _, v := range projectIDs {
		projectResourceMap[v] = apistructs.ProjectResourceItem{
			CpuServiceUsed: 0.0,
			MemServiceUsed: 0,
		}
	}
	if len(*runtimes) == 0 {
		return &projectResourceMap, nil
	}

	for _, v := range *runtimes {
		if _, ok := projectResourceMap[v.ProjectID]; ok {
			oldMem := projectResourceMap[v.ProjectID].MemServiceUsed
			oldCPU := projectResourceMap[v.ProjectID].CpuServiceUsed
			projectResourceMap[v.ProjectID] = apistructs.ProjectResourceItem{
				CpuServiceUsed: oldCPU + v.CPU,
				MemServiceUsed: oldMem + float64(v.Mem)/1024,
			}
		} else {
			newValue := apistructs.ProjectResourceItem{
				CpuServiceUsed: v.CPU,
				MemServiceUsed: float64(v.Mem) / 1024,
			}
			projectResourceMap[v.ProjectID] = newValue
		}
	}
	for _, v := range projectIDs {
		if _, ok := projectResourceMap[v]; !ok {
			projectResourceMap[v] = apistructs.ProjectResourceItem{
				CpuServiceUsed: 0.0,
				MemServiceUsed: 0,
			}
		}
	}

	return &projectResourceMap, nil
}

// GetProjectAddonResource 获取project下，addon的使用资源
func (a *Resource) GetProjectAddonResource(projectIDs []uint64) (*map[uint64]apistructs.ProjectResourceItem, error) {
	if len(projectIDs) == 0 {
		return nil, nil
	}
	insList, err := a.db.ListAddonInstancesByProjectIDs(projectIDs, "custom", "discovery")
	if err != nil {
		return nil, err
	}
	// 先初始化一些数据
	projectResourceMap := map[uint64]apistructs.ProjectResourceItem{}
	for _, v := range projectIDs {
		projectResourceMap[v] = apistructs.ProjectResourceItem{
			CpuServiceUsed: 0.0,
			MemServiceUsed: 0,
		}
	}
	if len(*insList) == 0 {
		return &projectResourceMap, nil
	}
	insIds := make([]string, len(*insList))
	instanceProjectMap := map[string]string{}
	for _, v := range *insList {
		insIds = append(insIds, v.ID)
		instanceProjectMap[v.ID] = v.ProjectID
	}
	//查询node表数据，获取cpu、mem信息进行累加
	nodeList, err := a.db.GetAddonNodesByInstanceIDs(insIds)
	if err != nil {
		return nil, err
	}
	if len(*nodeList) == 0 {
		return nil, nil
	}

	for _, v := range *nodeList {
		projectID, err := strconv.ParseUint(instanceProjectMap[v.InstanceID], 10, 64)
		if err != nil {
			return nil, err
		}
		if _, ok := projectResourceMap[projectID]; ok {
			oldMem := projectResourceMap[projectID].MemAddonUsed
			oldCPU := projectResourceMap[projectID].CpuAddonUsed
			projectResourceMap[projectID] = apistructs.ProjectResourceItem{
				CpuAddonUsed: oldCPU + v.CPU,
				MemAddonUsed: oldMem + float64(v.Mem)/1024,
			}
		} else {
			newValue := apistructs.ProjectResourceItem{
				CpuAddonUsed: v.CPU,
				MemAddonUsed: float64(v.Mem) / 1024,
			}
			projectResourceMap[projectID] = newValue
		}
	}
	for _, v := range projectIDs {
		if _, ok := projectResourceMap[v]; !ok {
			projectResourceMap[v] = apistructs.ProjectResourceItem{
				CpuAddonUsed: 0.0,
				MemAddonUsed: 0,
			}
		}
	}
	return &projectResourceMap, nil
}
