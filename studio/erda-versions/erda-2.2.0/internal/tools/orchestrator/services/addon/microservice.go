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

package addon

import (
	"crypto/md5" // #nosec G501
	"encoding/hex"
	"encoding/json"
	"sort"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

var allEnvs = []string{apistructs.WORKSPACE_DEV, apistructs.WORKSPACE_TEST, apistructs.WORKSPACE_STAGING, apistructs.WORKSPACE_PROD}

type MicroServiceProjectData []*apistructs.MicroServiceProjectResponseData

func (d MicroServiceProjectData) Len() int {
	return len(d)
}
func (d MicroServiceProjectData) Less(i, j int) bool {
	projectI, err := strconv.ParseInt(d[i].ProjectID, 10, 64)
	if err != nil {
		return true
	}

	projectJ, err := strconv.ParseInt(d[j].ProjectID, 10, 64)
	if err != nil {
		return true
	}

	return projectI > projectJ
}
func (d MicroServiceProjectData) Swap(i, j int) {
	d[i], d[j] = d[j], d[i]
}

// ListMicroServiceProject 获取使用微服务的项目列表
func (a *Addon) ListMicroServiceProject(projectIDs []string) ([]*apistructs.MicroServiceProjectResponseData, error) {
	// 获取project信息
	var validProjectIDs []string
	projectMap := make(map[string]*apistructs.ProjectDTO)
	for _, projectID := range projectIDs {
		project, err := a.getProject(projectID)
		if err != nil {
			logrus.Warnf("failed to get project, %v", err)
			continue
		}
		validProjectIDs = append(validProjectIDs, projectID)
		projectMap[projectID] = project
	}
	if len(validProjectIDs) == 0 {
		return nil, nil
	}

	// 获取project级别对应的实例路由
	projectRoutings, err := a.db.GetInstanceRoutingsByProjectIDs(1, validProjectIDs, "", "")
	if err != nil {
		return nil, err
	}

	// 获取非project级别对应的实例路由（配置中心）
	attaches, err := a.db.GetMicroAttachesByAddonNameAndProjectIDs(apistructs.AddonConfigCenter, validProjectIDs, "")
	if err != nil {
		return nil, err
	}
	var attachRoutingIDs []string
	for _, attach := range *attaches {
		attachRoutingIDs = append(attachRoutingIDs, attach.RoutingInstanceID)
	}
	attachRoutings, err := a.db.GetInstanceRoutingsByIDs(attachRoutingIDs)
	if err != nil {
		return nil, err
	}
	attachRoutingMap := make(map[string]dbclient.AddonInstanceRouting)
	for _, attachRouting := range *attachRoutings {
		attachRoutingMap[attachRouting.ID] = attachRouting
	}

	dataMap := make(map[uint64]*apistructs.MicroServiceProjectResponseData)
	// 包装project级别的实例路由
	for _, routing := range *projectRoutings {
		project := projectMap[routing.ProjectID]
		if az, ok := project.ClusterConfig[routing.Workspace]; !ok || az != routing.Cluster {
			continue
		}
		a.appendMicroServiceProjectData(dataMap, project, routing.Workspace)
	}
	// 包装非project级别的实例路由
	for _, attach := range *attaches {
		if routing, ok := attachRoutingMap[attach.RoutingInstanceID]; !ok || routing.Status != string(apistructs.AddonAttached) {
			continue
		}
		project := projectMap[attach.ProjectID]
		a.appendMicroServiceProjectData(dataMap, project, attach.Env)
	}

	// 排序env
	var dataList []*apistructs.MicroServiceProjectResponseData
	for _, data := range dataMap {
		envMap := make(map[string]bool)
		for _, env := range data.Envs {
			envMap[env] = true
		}
		project := projectMap[data.ProjectID]
		var envs []string
		var tenantGroups []string
		for _, env := range allEnvs {
			if _, ok := envMap[env]; ok {
				tenantGroups = append(tenantGroups, md5V(data.ProjectID+"_"+env+"_"+project.ClusterConfig[env]+conf.TenantGroupKey()))
				envs = append(envs, env)
			} else {
				tenantGroups = append(tenantGroups, "")
				envs = append(envs, "")
			}
		}
		data.Envs = envs
		data.TenantGroups = tenantGroups
		dataList = append(dataList, data)
	}

	sort.Sort(MicroServiceProjectData(dataList))
	return dataList, nil
}

// md5V md5加密
func md5V(str string) string {
	h := md5.New() // #nosec G401
	h.Write([]byte(str))
	return hex.EncodeToString(h.Sum(nil))
}

// ListMicroServiceMenu 获取项目下的微服务菜单
func (a *Addon) ListMicroServiceMenu(projectID, env string) ([]*apistructs.MicroServiceMenuResponseData, error) {
	// 获取project信息
	project, err := a.getProject(projectID)
	if err != nil {
		return nil, err
	}
	projectIDs := []string{projectID}

	az := project.ClusterConfig[env]
	var instanceIDs []string
	// 获取project级别对应的实例
	projectRoutings, err := a.db.GetInstanceRoutingsByProjectIDs(1, projectIDs, az, env)
	if err != nil {
		return nil, err
	}
	for _, routing := range *projectRoutings {
		instanceIDs = append(instanceIDs, routing.RealInstance)
	}

	// 获取非project级别对应的实例
	attaches, err := a.db.GetMicroAttachesByAddonNameAndProjectIDs(apistructs.AddonConfigCenter, projectIDs, env)
	if err != nil {
		return nil, err
	}
	var attachRoutingIDs []string
	for _, attach := range *attaches {
		attachRoutingIDs = append(attachRoutingIDs, attach.RoutingInstanceID)
		instanceIDs = append(instanceIDs, attach.InstanceID)
	}
	attachRoutings, err := a.db.GetInstanceRoutingsByIDs(attachRoutingIDs)
	if err != nil {
		return nil, err
	}
	attachRoutingMap := make(map[string]dbclient.AddonInstanceRouting)
	for _, attachRouting := range *attachRoutings {
		attachRoutingMap[attachRouting.ID] = attachRouting
	}

	// 获取全部对应的实例
	instances, err := a.db.GetInstancesByIDs(instanceIDs)
	if err != nil {
		return nil, err
	}
	instanceMap := make(map[string]dbclient.AddonInstance)
	// 校验是否存在roost或zookeeper
	var hasRoost bool
	for _, instance := range *instances {
		if instance.AddonName == "terminus-roost" {
			hasRoost = true
		}
		instanceMap[instance.ID] = instance
	}

	var dataList []*apistructs.MicroServiceMenuResponseData
	// 包装project级别的实例路由
	dataListMap := make(map[string]string)
	for _, routing := range *projectRoutings {
		instance, ok := instanceMap[routing.RealInstance]
		if !ok {
			continue
		}

		if _, ok := dataListMap[routing.AddonName]; !ok {
			data := a.createMicroServiceMenuData(routing, instance, project)
			dataList = append(dataList, data)
			dataListMap[data.AddonName] = data.AddonName
		}
	}
	// 包装非project级别的实例路由
	for _, attach := range *attaches {
		routing, ok := attachRoutingMap[attach.RoutingInstanceID]
		if !ok || routing.Status != string(apistructs.AddonAttached) {
			continue
		}
		instance, ok := instanceMap[routing.RealInstance]
		if !ok {
			continue
		}
		if hasRoost && instance.AddonName == "terminus-zkproxy" {
			continue
		}

		data := a.createMicroServiceMenuData(routing, instance, project)
		dataList = append(dataList, data)
	}
	// 排序
	dataList = a.sortMicroServiceMenuData(dataList)
	return dataList, nil
}

// appendMicroServiceProjectData 添加微服务项目数据
func (a *Addon) appendMicroServiceProjectData(dataMap map[uint64]*apistructs.MicroServiceProjectResponseData, project *apistructs.ProjectDTO, env string) {
	data, ok := dataMap[project.ID]
	if !ok {
		data = new(apistructs.MicroServiceProjectResponseData)
		data.ProjectID = strconv.FormatUint(project.ID, 10)
		data.ProjectName = project.DisplayName
		data.ProjectDesc = project.Desc
		data.LogoURL = project.Logo
		data.CreateTime = project.CreatedAt
		dataMap[project.ID] = data
	}
	data.Envs = append(data.Envs, env)
}

// createMicroServiceMenuData 生成微服务菜单数据
func (a *Addon) createMicroServiceMenuData(routing dbclient.AddonInstanceRouting, instance dbclient.AddonInstance, project *apistructs.ProjectDTO) *apistructs.MicroServiceMenuResponseData {
	data := new(apistructs.MicroServiceMenuResponseData)
	data.AddonName = routing.AddonName

	if v, ok := AddonInfos.Load(routing.AddonName); !ok {
		data.AddonDisplayName = routing.AddonName
	} else {
		data.AddonDisplayName = v.(apistructs.Extension).DisplayName
	}
	data.InstanceId = routing.ID
	data.ProjectName = project.Name

	if instance.Config != "" {
		config := make(map[string]string)
		err := json.Unmarshal([]byte(instance.Config), &config)
		if err != nil {
			logrus.Errorf("fail to json unmarshal instance: %s config: %s", routing.ID, instance.Config)
			return data
		}

		if publicHost, ok := config["PUBLIC_HOST"]; ok {
			if !strings.HasPrefix(publicHost, "http") {
				publicHost = "http://" + publicHost
			}
			data.ConsoleUrl = publicHost
		}
		if terminusKey, ok := config["TERMINUS_KEY"]; ok {
			data.TerminusKey = terminusKey
		}
	}
	return data
}

// sortMicroServiceMenuData 排序微服务菜单数据
func (a *Addon) sortMicroServiceMenuData(data []*apistructs.MicroServiceMenuResponseData) []*apistructs.MicroServiceMenuResponseData {
	if len(data) == 0 {
		return data
	}

	list := make([]*apistructs.MicroServiceMenuResponseData, len(data)+1)
	index := 1
	for _, item := range data {
		if item.AddonName == "monitor" {
			list[0] = item
		} else {
			list[index] = item
			index++
		}
	}
	return list[index-len(data) : index]
}
